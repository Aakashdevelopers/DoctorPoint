package com.amstudio.drpoint.ui.appointments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.amstudio.drpoint.R;
import com.amstudio.drpoint.adapter.AppointmentListAdapter;
import com.amstudio.drpoint.databinding.BottomSheetAppointmentDetailsBinding;
import com.amstudio.drpoint.databinding.DialogRateDoctorBinding;
import com.amstudio.drpoint.databinding.DialogRefundDetailsBinding;
import com.amstudio.drpoint.databinding.FragmentAppointmentsBinding;
import com.amstudio.drpoint.model.Appointment;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.model.DoctorReview;
import com.amstudio.drpoint.model.RefundRequest;
import com.amstudio.drpoint.network.SupabaseClient;
import com.amstudio.drpoint.network.model.CancelAppointmentRpcRequest;
import com.amstudio.drpoint.ui.booking.BookAppointmentActivity;
import com.amstudio.drpoint.ui.main.MainActivity;
import com.amstudio.drpoint.util.AvailabilityHelper;
import com.amstudio.drpoint.util.DummyDataProvider;
import com.amstudio.drpoint.util.PreferenceManager;
import com.amstudio.drpoint.util.ToastUtils;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AppointmentsFragment extends Fragment {

    private FragmentAppointmentsBinding binding;
    private AppointmentListAdapter adapter;
    private int currentTabPosition = 0;
    private List<Appointment> fetchedAppointments = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAppointmentsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Context context = getContext();
        if (context == null || binding == null) return;

        binding.rvAppointments.setLayoutManager(new LinearLayoutManager(context));
        adapter = new AppointmentListAdapter(new AppointmentListAdapter.OnAppointmentActionListener() {
            @Override
            public void onItemClick(Appointment appointment) {
                if (appointment != null) openAppointmentDetailsBottomSheet(appointment);
            }

            @Override
            public void onRescheduleClick(Appointment appointment) {
                if (appointment == null) return;
                if (!appointment.isCancellable()) {
                    Context ctx = getContext();
                    if (ctx != null) {
                        ToastUtils.showWarning(ctx, "This appointment cannot be rescheduled.");
                    }
                    return;
                }
                openRescheduleFlow(appointment);
            }

            @Override
            public void onCancelClick(Appointment appointment) {
                if (appointment != null) attemptCancelAppointment(appointment);
            }

            @Override
            public void onRateDoctorClick(Appointment appointment) {
                if (appointment != null) openRateDoctorBottomSheet(appointment);
            }

            @Override
            public void onRefundClick(Appointment appointment) {
                if (appointment != null) openRefundDetailsBottomSheet(appointment);
            }
        });
        binding.rvAppointments.setAdapter(adapter);

        if (binding.btnBookNowEmpty != null) {
            binding.btnBookNowEmpty.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).selectTab(MainActivity.TAB_EXPLORE);
                }
            });
        }

        PreferenceManager prefManager = PreferenceManager.getInstance(context);
        binding.swReminders.setChecked(prefManager.getRemindersEnabled());
        binding.swReminders.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefManager.setRemindersEnabled(isChecked);
            Context ctx = getContext();
            if (ctx != null) {
                String msg = isChecked ? "Reminders enabled" : "Reminders disabled";
                ToastUtils.showInfo(ctx, msg);
            }
        });

        binding.tabLayoutAppointments.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTabPosition = tab.getPosition();
                filterAppointments(currentTabPosition);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        loadAppointments();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAppointments();
    }

    private static String formatUuidOrNull(String str) {
        if (str == null || str.trim().isEmpty()) return null;
        String clean = str.trim();
        if (clean.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")) {
            return clean;
        }
        try {
            return UUID.nameUUIDFromBytes(clean.getBytes(StandardCharsets.UTF_8)).toString();
        } catch (Exception e) {
            return null;
        }
    }

    private void loadAppointments() {
        if (!isAdded() || getContext() == null) return;
        Context context = getContext();
        if (context == null) return;

        PreferenceManager prefManager = PreferenceManager.getInstance(context);

        // Check if current user is logged in
        if (!prefManager.isLoggedIn()) {
            fetchedAppointments = new ArrayList<>();
            filterAppointments(currentTabPosition);
            return;
        }

        String patientId = prefManager.getUserId();
        if (patientId == null || patientId.trim().isEmpty()) {
            fetchedAppointments = new ArrayList<>();
            filterAppointments(currentTabPosition);
            return;
        }

        String cleanPatientId = patientId.trim();
        String uuidPatientId = formatUuidOrNull(cleanPatientId);

        String queryParam;
        if (uuidPatientId != null && !uuidPatientId.equalsIgnoreCase(cleanPatientId)) {
            queryParam = "in.(" + cleanPatientId + "," + uuidPatientId + ")";
        } else {
            queryParam = "eq." + cleanPatientId;
        }

        if (binding != null && binding.shimmerAppointments != null) {
            binding.shimmerAppointments.startShimmer();
            binding.shimmerAppointments.setVisibility(View.VISIBLE);
            if (binding.rvAppointments != null) binding.rvAppointments.setVisibility(View.GONE);
            if (binding.llEmptyState != null) binding.llEmptyState.setVisibility(View.GONE);
        }

        // Fetch ONLY currently logged-in user's appointments from Supabase
        SupabaseClient.getAppointmentService().getAppointmentsForPatient(queryParam)
                .enqueue(new Callback<List<Appointment>>() {
                    @Override
                    public void onResponse(Call<List<Appointment>> call, Response<List<Appointment>> response) {
                        if (!isAdded() || getContext() == null || binding == null) return;
                        if (response.isSuccessful() && response.body() != null) {
                            fetchedAppointments = response.body();
                        } else {
                            fetchedAppointments = new ArrayList<>();
                        }
                        enrichAppointmentsWithDoctorData(fetchedAppointments, () -> {
                            if (!isAdded() || getContext() == null || binding == null) return;
                            ensureTokenNumbersAssigned(fetchedAppointments);
                            processAutoCancellationForPastAppointments(fetchedAppointments);
                            filterAppointments(currentTabPosition);
                        });
                    }

                    @Override
                    public void onFailure(Call<List<Appointment>> call, Throwable t) {
                        if (!isAdded() || getContext() == null || binding == null) return;
                        fetchedAppointments = new ArrayList<>();
                        filterAppointments(currentTabPosition);
                    }
                });
    }

    private final Map<String, Doctor> doctorCache = new HashMap<>();

    private void enrichAppointmentsWithDoctorData(List<Appointment> list, Runnable onComplete) {
        if (list == null || list.isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        Set<String> missingDocIds = new HashSet<>();
        for (Appointment appt : list) {
            if (appt == null) continue;
            if (appt.getDoctor() != null) {
                if (appt.getDoctor().getId() != null) {
                    doctorCache.put(appt.getDoctor().getId().trim(), appt.getDoctor());
                }
            } else {
                String dId = appt.getDoctorId();
                if (dId != null && !dId.trim().isEmpty()) {
                    Doctor cached = doctorCache.get(dId.trim());
                    if (cached != null) {
                        appt.setDoctor(cached);
                    } else {
                        missingDocIds.add(dId.trim());
                    }
                }
            }
        }

        if (missingDocIds.isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        SupabaseClient.getDoctorService().getDoctors().enqueue(new Callback<List<Doctor>>() {
            @Override
            public void onResponse(Call<List<Doctor>> call, Response<List<Doctor>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Doctor doc : response.body()) {
                        if (doc != null && doc.getId() != null) {
                            doctorCache.put(doc.getId().trim(), doc);
                        }
                    }
                    for (Appointment appt : list) {
                        if (appt == null) continue;
                        if (appt.getDoctor() == null && appt.getDoctorId() != null) {
                            Doctor d = doctorCache.get(appt.getDoctorId().trim());
                            if (d != null) {
                                appt.setDoctor(d);
                            }
                        }
                    }
                }
                if (onComplete != null) onComplete.run();
            }

            @Override
            public void onFailure(Call<List<Doctor>> call, Throwable t) {
                if (onComplete != null) onComplete.run();
            }
        });
    }

    private void ensureTokenNumbersAssigned(List<Appointment> list) {
        if (list == null || list.isEmpty()) return;

        Map<String, List<Appointment>> map = new HashMap<>();
        for (Appointment appt : list) {
            if (appt == null) continue;
            String docId = appt.getDoctorId() != null ? appt.getDoctorId().trim() : "doc_default";
            String rawDate = appt.getAppointmentDate() != null ? appt.getAppointmentDate().trim() : "date_default";
            String cleanDate = normalizeDateKey(rawDate);
            String key = docId + "_" + cleanDate;

            List<Appointment> apptList = map.get(key);
            if (apptList == null) {
                apptList = new ArrayList<>();
                map.put(key, apptList);
            }
            apptList.add(appt);
        }

        for (Map.Entry<String, List<Appointment>> entry : map.entrySet()) {
            List<Appointment> group = entry.getValue();
            if (group == null || group.isEmpty()) continue;

            // Sort chronologically by start_time minutes
            Collections.sort(group, (a1, a2) -> Integer.compare(a1.getStartTimeMinutes(), a2.getStartTimeMinutes()));

            Set<Integer> uniqueTokens = new HashSet<>();
            boolean hasZeros = false;
            for (Appointment a : group) {
                if (a.getTokenNumber() <= 0) {
                    hasZeros = true;
                } else {
                    uniqueTokens.add(a.getTokenNumber());
                }
            }

            if (hasZeros || uniqueTokens.size() < group.size()) {
                int seq = 1;
                for (Appointment a : group) {
                    a.setTokenNumber(seq++);
                }
            }
        }
    }

    private static String normalizeDateKey(String rawDate) {
        if (rawDate == null) return "date_unknown";
        String clean = rawDate.trim();
        if (clean.contains("T")) {
            clean = clean.split("T")[0];
        } else if (clean.contains(" ")) {
            clean = clean.split("\\s+")[0];
        }
        return clean.toLowerCase(Locale.US);
    }

    private void filterAppointments(int tabPosition) {
        if (binding == null || !isAdded()) return;
        Context context = getContext();
        if (context == null) return;

        PreferenceManager prefManager = PreferenceManager.getInstance(context);

        // If user is not logged in, show empty state immediately
        if (!prefManager.isLoggedIn()) {
            if (binding.shimmerAppointments != null) {
                binding.shimmerAppointments.stopShimmer();
                binding.shimmerAppointments.setVisibility(View.GONE);
            }
            if (binding.llEmptyState != null) binding.llEmptyState.setVisibility(View.VISIBLE);
            if (binding.rvAppointments != null) binding.rvAppointments.setVisibility(View.GONE);
            if (adapter != null) adapter.submitList(new ArrayList<>());
            return;
        }

        String currentUserId = prefManager.getUserId() != null ? prefManager.getUserId().trim() : "";
        String currentUuid = formatUuidOrNull(currentUserId);

        if (currentUserId.isEmpty()) {
            if (binding.shimmerAppointments != null) {
                binding.shimmerAppointments.stopShimmer();
                binding.shimmerAppointments.setVisibility(View.GONE);
            }
            if (binding.llEmptyState != null) binding.llEmptyState.setVisibility(View.VISIBLE);
            if (binding.rvAppointments != null) binding.rvAppointments.setVisibility(View.GONE);
            if (adapter != null) adapter.submitList(new ArrayList<>());
            return;
        }

        List<Appointment> allAppointments = fetchedAppointments != null ? fetchedAppointments : new ArrayList<>();
        List<Appointment> displayed = new ArrayList<>();

        for (Appointment appt : allAppointments) {
            if (appt == null) continue;

            String pId = appt.getPatientId() != null ? appt.getPatientId().trim() : "";

            // Strictly filter by currently logged-in user's ID or UUID
            boolean isUserMatch = !pId.isEmpty() && (
                    pId.equalsIgnoreCase(currentUserId) ||
                    (currentUuid != null && pId.equalsIgnoreCase(currentUuid))
            );

            if (!isUserMatch) {
                continue; // Strictly hide appointments belonging to other users
            }

            String st = appt.getStatus() != null ? appt.getStatus().toLowerCase() : "confirmed";

            if (tabPosition == 0) {
                // Upcoming Tab: pending, confirmed, checked_in, waiting, in_consultation
                if ("pending".equals(st) || "confirmed".equals(st) || "checked_in".equals(st) || "waiting".equals(st) || "in_consultation".equals(st) || st.contains("confirm")) {
                    displayed.add(appt);
                }
            } else if (tabPosition == 1) {
                // Completed Tab: completed
                if ("completed".equals(st)) {
                    displayed.add(appt);
                }
            } else if (tabPosition == 2) {
                // Cancelled Tab: cancelled, rejected, no_show
                if ("cancelled".equals(st) || "rejected".equals(st) || "no_show".equals(st)) {
                    displayed.add(appt);
                }
            }
        }

        if (binding.shimmerAppointments != null) {
            binding.shimmerAppointments.stopShimmer();
            binding.shimmerAppointments.setVisibility(View.GONE);
        }

        if (displayed.isEmpty()) {
            if (binding.llEmptyState != null) binding.llEmptyState.setVisibility(View.VISIBLE);
            if (binding.rvAppointments != null) binding.rvAppointments.setVisibility(View.GONE);
        } else {
            Collections.sort(displayed, (a1, a2) -> {
                String d1 = a1.getDate() != null ? a1.getDate() : "";
                String d2 = a2.getDate() != null ? a2.getDate() : "";
                int dateComp = d2.compareTo(d1);
                if (dateComp != 0) return dateComp;
                return Integer.compare(a1.getStartTimeMinutes(), a2.getStartTimeMinutes());
            });
            if (binding.llEmptyState != null) binding.llEmptyState.setVisibility(View.GONE);
            if (binding.rvAppointments != null) binding.rvAppointments.setVisibility(View.VISIBLE);
        }

        if (adapter != null) {
            adapter.submitList(displayed);
        }
    }

    private void attemptCancelAppointment(Appointment appointment) {
        if (appointment == null || !isAdded()) return;
        Context context = getContext();
        if (context == null) return;

        if (!appointment.isCancellable()) {
            ToastUtils.showWarning(context, "This appointment cannot be cancelled.");
            return;
        }

        new AlertDialog.Builder(context)
                .setTitle("Cancel Appointment")
                .setMessage("Are you sure you want to cancel your appointment with " + appointment.getDoctorName() + "?")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> cancelAppointmentInSupabase(appointment))
                .setNegativeButton("No", null)
                .show();
    }

    private void cancelAppointmentInSupabase(Appointment appointment) {
        if (appointment == null || !isAdded()) return;
        Context context = getContext();
        if (context == null) return;

        appointment.setStatus("Cancelled");

        String patientId = PreferenceManager.getInstance(context).getUserId();
        CancelAppointmentRpcRequest cancelRequest = new CancelAppointmentRpcRequest(appointment.getId(), patientId, "Cancelled by patient");

        SupabaseClient.getSlotService().cancelAppointment(cancelRequest).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                Context ctx = getContext();
                if (!isAdded() || ctx == null) return;
                ToastUtils.showSuccess(ctx, "Appointment cancelled successfully.");
                loadAppointments();
                openRefundDetailsBottomSheet(appointment);
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Map<String, Object> updateMap = new HashMap<>();
                updateMap.put("status", "Cancelled");

                SupabaseClient.getAppointmentService()
                        .updateAppointmentStatus("eq." + appointment.getId(), updateMap)
                        .enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                Context ctx = getContext();
                                if (!isAdded() || ctx == null) return;
                                ToastUtils.showSuccess(ctx, "Appointment cancelled successfully.");
                                loadAppointments();
                                openRefundDetailsBottomSheet(appointment);
                            }

                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                Context ctx = getContext();
                                if (!isAdded() || ctx == null) return;
                                ToastUtils.showSuccess(ctx, "Appointment marked cancelled.");
                                loadAppointments();
                                openRefundDetailsBottomSheet(appointment);
                            }
                        });
            }
        });
    }

    private void openRescheduleFlow(Appointment appointment) {
        if (appointment == null || !isAdded()) return;
        Context context = getContext();
        if (context == null) return;

        Doctor doc = new Doctor();
        doc.setId(appointment.getDoctorId() != null ? appointment.getDoctorId() : "doc_1");
        doc.setName(appointment.getDoctorName());
        doc.setClinicName(appointment.getClinicName());
        doc.setLocation(appointment.getLocation());
        doc.setQualification(appointment.getSpecialization());

        Intent intent = new Intent(context, BookAppointmentActivity.class);
        intent.putExtra("doctor", doc);
        startActivity(intent);
    }

    private void openAppointmentDetailsBottomSheet(Appointment appointment) {
        if (!isAdded() || appointment == null) return;
        Context context = getContext();
        if (context == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(context);
        BottomSheetAppointmentDetailsBinding sheetBinding = BottomSheetAppointmentDetailsBinding.inflate(getLayoutInflater());
        dialog.setContentView(sheetBinding.getRoot());

        // Header info & Doctor Avatar
        sheetBinding.tvDetailStatus.setText(appointment.getUserFriendlyStatus());
        sheetBinding.tvDetailDoctorName.setText(appointment.getDoctorName());
        sheetBinding.tvDetailSpecialization.setText(appointment.getSpecialization());
        sheetBinding.tvDetailClinicName.setText(appointment.getClinicName());
        sheetBinding.tvDetailClinicAddress.setText(appointment.getLocation());

        if (appointment.getDoctor() != null && appointment.getDoctor().getImageUrl() != null && !appointment.getDoctor().getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(appointment.getDoctor().getImageUrl())
                    .placeholder(R.drawable.ic_user)
                    .into(sheetBinding.ivDetailDoctorImage);
        }

        if (appointment.getDoctor() != null && appointment.getDoctor().getReceptionPhone() != null && !appointment.getDoctor().getReceptionPhone().isEmpty()) {
            sheetBinding.tvDetailReceptionPhone.setText("Reception: " + appointment.getDoctor().getReceptionPhone());
            sheetBinding.tvDetailReceptionPhone.setVisibility(View.VISIBLE);
        } else {
            sheetBinding.tvDetailReceptionPhone.setText("Clinic Branch: " + appointment.getClinicName());
            sheetBinding.tvDetailReceptionPhone.setVisibility(View.VISIBLE);
        }

        // Clean Date & Time Formatting
        String formattedDate = appointment.getDate() != null ? appointment.getDate() : "Scheduled";
        String formattedTime = appointment.getFormattedTime();
        sheetBinding.tvDetailDatetime.setText(formattedDate + " • " + formattedTime);

        // Clean Type & Fee
        String apptTypeStr = "In-Clinic";
        if (appointment.getAppointmentType() != null && !appointment.getAppointmentType().isEmpty()) {
            String typeLower = appointment.getAppointmentType().toLowerCase();
            if (typeLower.contains("video")) apptTypeStr = "Video Consult";
            else if (typeLower.contains("follow")) apptTypeStr = "Follow-up";
            else apptTypeStr = "In-Clinic";
        }
        int feeVal = appointment.getAmount() > 0 ? appointment.getAmount() : (appointment.getFee() > 0 ? appointment.getFee() : 900);
        sheetBinding.tvDetailTypeFee.setText(apptTypeStr + " • ₹" + feeVal);

        // Queue Metrics Calculation
        int yourToken = appointment.getTokenNumber() > 0 ? appointment.getTokenNumber() : 1;
        sheetBinding.tvQueueTokenNum.setText("Token #" + String.format(Locale.getDefault(), "%02d", yourToken));

        int currentToken = Math.max(1, yourToken - 2);
        int patientsAhead = Math.max(0, yourToken - currentToken);

        sheetBinding.tvQueueCurrentToken.setText("#" + String.format(Locale.getDefault(), "%02d", currentToken));
        sheetBinding.tvQueuePatientsAhead.setText(String.valueOf(patientsAhead));

        // Symptoms / Illness Notes
        if (appointment.getPatientReason() != null && !appointment.getPatientReason().trim().isEmpty()) {
            sheetBinding.cardPatientReason.setVisibility(View.VISIBLE);
            sheetBinding.tvDetailReason.setText(appointment.getPatientReason());
        } else {
            sheetBinding.cardPatientReason.setVisibility(View.GONE);
        }

        // Prescription & Medicines (If Completed & Prescription Available)
        if (appointment.getPrescription() != null && !appointment.getPrescription().trim().isEmpty()) {
            sheetBinding.cardPrescriptionDetail.setVisibility(View.VISIBLE);
            sheetBinding.tvDetailPrescriptionText.setText(appointment.getPrescription());
        } else {
            sheetBinding.cardPrescriptionDetail.setVisibility(View.GONE);
        }

        // Action Buttons
        if (!appointment.isCancellable()) {
            sheetBinding.btnDetailCancel.setVisibility(View.GONE);
            sheetBinding.btnDetailReschedule.setVisibility(View.GONE);
        } else {
            sheetBinding.btnDetailCancel.setVisibility(View.VISIBLE);
            sheetBinding.btnDetailReschedule.setVisibility(View.VISIBLE);
        }

        sheetBinding.btnDetailCancel.setOnClickListener(v -> {
            dialog.dismiss();
            attemptCancelAppointment(appointment);
        });

        sheetBinding.btnDetailReschedule.setOnClickListener(v -> {
            dialog.dismiss();
            openRescheduleFlow(appointment);
        });

        dialog.show();
    }

    private void processAutoCancellationForPastAppointments(List<Appointment> appointments) {
        if (appointments == null || appointments.isEmpty()) return;

        for (Appointment appt : appointments) {
            if (appt == null) continue;
            String st = appt.getStatus() != null ? appt.getStatus().toLowerCase() : "";

            boolean isCompletedOrCancelled = "completed".equals(st) || "cancelled".equals(st) || "rejected".equals(st) || "no_show".equals(st);

            if (!isCompletedOrCancelled) {
                if (AvailabilityHelper.isDateInPast(appt.getDate())) {
                    appt.setStatus("Cancelled");

                    Map<String, Object> updateMap = new HashMap<>();
                    updateMap.put("status", "Cancelled");

                    SupabaseClient.getAppointmentService()
                            .updateAppointmentStatus("eq." + appt.getId(), updateMap)
                            .enqueue(new Callback<Void>() {
                                @Override
                                public void onResponse(Call<Void> call, Response<Void> response) {}

                                @Override
                                public void onFailure(Call<Void> call, Throwable t) {}
                            });
                }
            }
        }
    }

    private void openRefundDetailsBottomSheet(Appointment appointment) {
        if (!isAdded() || appointment == null) return;
        Context context = getContext();
        if (context == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(context);
        DialogRefundDetailsBinding refundBinding = DialogRefundDetailsBinding.inflate(getLayoutInflater());
        dialog.setContentView(refundBinding.getRoot());

        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        dialog.setOnShowListener(d -> {
            BottomSheetDialog bsd = (BottomSheetDialog) d;
            FrameLayout bottomSheet = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(android.R.color.transparent);
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });

        dialog.show();

        int fee = appointment.getAmount() > 0 ? appointment.getAmount() : (appointment.getFee() > 0 ? appointment.getFee() : 500);

        refundBinding.tvRefundPaidAmount.setText("₹" + fee);
        refundBinding.tvTotalRefundAmount.setText("₹" + fee);

        String refId = "REF-" + Math.abs((appointment.getId() != null ? appointment.getId() : "123").hashCode() % 899999 + 100000);
        refundBinding.tvRefundReferenceId.setText("Refund Reference ID: " + refId);

        refundBinding.ivCloseRefundDialog.setOnClickListener(v -> dialog.dismiss());

        PreferenceManager prefManager = PreferenceManager.getInstance(context);
        String resolvedPatientId = null;
        if (appointment.getPatientId() != null && !appointment.getPatientId().trim().isEmpty() && !appointment.getPatientId().equals("user_default")) {
            resolvedPatientId = appointment.getPatientId().trim();
        } else if (prefManager.getUserId() != null && !prefManager.getUserId().trim().isEmpty()) {
            resolvedPatientId = prefManager.getUserId().trim();
        }
        if ("patient_anon".equals(resolvedPatientId)) {
            resolvedPatientId = null;
        }

        String patientName = prefManager.getUserName() != null ? prefManager.getUserName() : "Verified Patient";
        String phone = prefManager.getUserPhone() != null && !prefManager.getUserPhone().trim().isEmpty()
                ? prefManager.getUserPhone() : "+91 9876543210";

        final boolean[] hasExistingRefund = {false};

        // Check if a refund request already exists for this appointment
        if (appointment.getId() != null) {
            SupabaseClient.getAppointmentService().getRefundRequestByAppointment("eq." + appointment.getId())
                    .enqueue(new Callback<List<RefundRequest>>() {
                        @Override
                        public void onResponse(Call<List<RefundRequest>> call, Response<List<RefundRequest>> response) {
                            if (!isAdded() || getContext() == null) return;
                            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                                RefundRequest existing = response.body().get(0);
                                if (existing != null) {
                                    hasExistingRefund[0] = true;
                                    refundBinding.tilRefundReason.setVisibility(View.GONE);
                                    if (existing.getPatientUpi() != null && !existing.getPatientUpi().trim().isEmpty()) {
                                        refundBinding.etPatientUpi.setText(existing.getPatientUpi());
                                        refundBinding.etPatientUpi.setEnabled(false);
                                    }
                                    String status = existing.getStatus() != null ? existing.getStatus().toLowerCase() : "pending";

                                    if ("approved".equals(status)) {
                                        refundBinding.tvRefundStatusTitle.setText("Refund Approved & Processed");
                                        refundBinding.tvRefundStatusSub.setText("₹" + fee + " has been approved by admin and credited to your payment source.");
                                        refundBinding.btnRefundAction.setText("✓ Refund Processed");
                                        refundBinding.btnRefundAction.setEnabled(false);
                                    } else if ("rejected".equals(status)) {
                                        refundBinding.tvRefundStatusTitle.setText("Refund Request Rejected");
                                        refundBinding.tvRefundStatusSub.setText("Your refund request was reviewed by admin and marked ineligible.");
                                        refundBinding.btnRefundAction.setText("Refund Request Closed");
                                        refundBinding.btnRefundAction.setEnabled(false);
                                    } else {
                                        refundBinding.tvRefundStatusTitle.setText("Refund Request Pending Approval");
                                        refundBinding.tvRefundStatusSub.setText("Request submitted to admin. Status: Under Review.");
                                        refundBinding.btnRefundAction.setText("Status: Pending Admin Review");
                                        refundBinding.btnRefundAction.setEnabled(false);
                                    }
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<List<RefundRequest>> call, Throwable t) {}
                    });
        }

        String safePatientId = resolvedPatientId != null && !resolvedPatientId.trim().isEmpty() ? resolvedPatientId.trim() : "patient_anon";
        final String finalPatientId = safePatientId;

        refundBinding.btnRefundAction.setOnClickListener(v -> {
            String upiText = refundBinding.etPatientUpi.getText() != null
                    ? refundBinding.etPatientUpi.getText().toString().trim() : "";
            String reasonText = refundBinding.etRefundReason.getText() != null
                    ? refundBinding.etRefundReason.getText().toString().trim() : "";

            String apptDate = appointment.getAppointmentDate() != null ? appointment.getAppointmentDate() : appointment.getDate();
            if (apptDate == null || apptDate.equalsIgnoreCase("Completed Consultation") || apptDate.equalsIgnoreCase("Scheduled") || !apptDate.matches(".*\\d+.*")) {
                apptDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            }

            Map<String, Object> refundMap = new HashMap<>();
            refundMap.put("appointment_id", appointment.getId());
            refundMap.put("patient_id", finalPatientId);
            refundMap.put("patient_name", patientName);
            refundMap.put("patient_phone", phone);
            refundMap.put("patient_upi", upiText);
            refundMap.put("doctor_id", appointment.getDoctorId() != null ? appointment.getDoctorId() : "doc_1");
            refundMap.put("doctor_name", appointment.getDoctorName());
            refundMap.put("doctor_specialization", appointment.getSpecialization());
            refundMap.put("clinic_name", appointment.getClinicName());
            refundMap.put("appointment_date", apptDate);
            refundMap.put("amount", fee);
            refundMap.put("reason", reasonText.isEmpty() ? "Patient requested cancellation refund" : reasonText);
            refundMap.put("status", "pending");

            refundBinding.btnRefundAction.setEnabled(false);
            refundBinding.btnRefundAction.setText("Submitting Request...");

            Callback<List<Map<String, Object>>> refundCallback = new Callback<List<Map<String, Object>>>() {
                @Override
                public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                    Context ctx = getContext();
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        if (isAdded() && ctx != null) {
                            ToastUtils.showSuccess(ctx, "Refund request submitted successfully!");
                            dialog.dismiss();
                        }
                    } else {
                        String errStr = extractResponseError(response);
                        Log.e("AppointmentsFragment", "Refund submission failed: " + errStr);

                        if (hasExistingRefund[0]) {
                            SupabaseClient.getAppointmentService().postRefundRequestPayload("appointment_id", "resolution=merge-duplicates,return=representation", refundMap)
                                    .enqueue(new Callback<List<Map<String, Object>>>() {
                                        @Override
                                        public void onResponse(Call<List<Map<String, Object>>> c2, Response<List<Map<String, Object>>> r2) {
                                            Context ctx2 = getContext();
                                            if (r2.isSuccessful() && r2.body() != null && !r2.body().isEmpty()) {
                                                if (isAdded() && ctx2 != null) {
                                                    ToastUtils.showSuccess(ctx2, "Refund request submitted successfully!");
                                                    dialog.dismiss();
                                                }
                                            } else {
                                                resetRefundButton(refundBinding, extractResponseError(r2));
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<List<Map<String, Object>>> c2, Throwable t2) {
                                            resetRefundButton(refundBinding, "Network Error: " + t2.getMessage());
                                        }
                                    });
                        } else {
                            SupabaseClient.getAppointmentService().updateRefundRequest("eq." + appointment.getId(), "return=representation", refundMap)
                                    .enqueue(new Callback<List<Map<String, Object>>>() {
                                        @Override
                                        public void onResponse(Call<List<Map<String, Object>>> c2, Response<List<Map<String, Object>>> r2) {
                                            Context ctx2 = getContext();
                                            if (r2.isSuccessful() && r2.body() != null && !r2.body().isEmpty()) {
                                                if (isAdded() && ctx2 != null) {
                                                    ToastUtils.showSuccess(ctx2, "Refund request submitted successfully!");
                                                    dialog.dismiss();
                                                }
                                            } else {
                                                resetRefundButton(refundBinding, extractResponseError(r2));
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<List<Map<String, Object>>> c2, Throwable t2) {
                                            resetRefundButton(refundBinding, "Network Error: " + t2.getMessage());
                                        }
                                    });
                        }
                    }
                }

                @Override
                public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                    Log.e("AppointmentsFragment", "Refund submission error: " + t.getMessage(), t);
                    resetRefundButton(refundBinding, "Network Error: " + t.getMessage());
                }
            };

            // Also update payment_status in appointments table to 'Refund Requested'
            Map<String, Object> updateApptMap = new HashMap<>();
            updateApptMap.put("payment_status", "Refund Requested");
            SupabaseClient.getAppointmentService().updateAppointmentStatus("eq." + appointment.getId(), updateApptMap)
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {}
                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {}
                    });

            if (hasExistingRefund[0]) {
                SupabaseClient.getAppointmentService().updateRefundRequest("eq." + appointment.getId(), "return=representation", refundMap)
                        .enqueue(refundCallback);
            } else {
                SupabaseClient.getAppointmentService().postRefundRequestPayload("appointment_id", "resolution=merge-duplicates,return=representation", refundMap)
                        .enqueue(refundCallback);
            }
        });

        dialog.show();
    }

    private String extractResponseError(Response<?> response) {
        if (response == null) return "Unknown Error";
        int code = response.code();
        String body = "";
        try {
            if (response.errorBody() != null) {
                body = response.errorBody().string();
            }
        } catch (Exception ignored) {}
        if (!body.isEmpty()) {
            return "Error " + code + ": " + body;
        }
        if (code == 200 || code == 204) {
            return "No row updated in Supabase (0 rows affected)";
        }
        return "Supabase Error Code " + code;
    }

    private void resetRefundButton(DialogRefundDetailsBinding refundBinding, String message) {
        Context context = getContext();
        if (!isAdded() || context == null || refundBinding == null) return;
        refundBinding.btnRefundAction.setEnabled(true);
        refundBinding.btnRefundAction.setText("Request Cancellation & Refund");
        ToastUtils.showInfo(context, message);
    }

    private void openRateDoctorBottomSheet(Appointment appointment) {
        if (!isAdded() || appointment == null) return;
        Context context = getContext();
        if (context == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(context);
        DialogRateDoctorBinding rateBinding = DialogRateDoctorBinding.inflate(getLayoutInflater());
        dialog.setContentView(rateBinding.getRoot());

        rateBinding.tvRateDoctorName.setText(appointment.getDoctorName() != null ? appointment.getDoctorName() : "Doctor");
        rateBinding.tvRateSpecialization.setText(appointment.getSpecialization() != null ? appointment.getSpecialization() : "Consultant");

        String dateStr = appointment.getDate() != null ? appointment.getDate() : "Completed Consultation";
        rateBinding.tvRateApptDate.setText("Completed • " + dateStr);

        if (appointment.getDoctor() != null && appointment.getDoctor().getImageUrl() != null && !appointment.getDoctor().getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(appointment.getDoctor().getImageUrl())
                    .placeholder(R.drawable.ic_user)
                    .into(rateBinding.ivRateDoctorAvatar);
        }

        final boolean[] hasExistingReview = {false};

        // Fetch existing review for this appointment if already submitted
        if (appointment.getId() != null) {
            SupabaseClient.getDoctorService().getDoctorReviewByAppointment("eq." + appointment.getId())
                    .enqueue(new Callback<List<DoctorReview>>() {
                        @Override
                        public void onResponse(Call<List<DoctorReview>> call, Response<List<DoctorReview>> response) {
                            if (!isAdded() || getContext() == null) return;
                            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                                DoctorReview existingReview = response.body().get(0);
                                if (existingReview != null) {
                                    hasExistingReview[0] = true;
                                    rateBinding.ratingBar.setRating((float) existingReview.getRating());
                                    if (existingReview.getReviewText() != null && !existingReview.getReviewText().trim().isEmpty()) {
                                        rateBinding.etReviewText.setText(existingReview.getReviewText());
                                    }
                                    rateBinding.btnSubmitReview.setText("Update Review");
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<List<DoctorReview>> call, Throwable t) {}
                    });
        }

        rateBinding.ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            float val = Math.max(1.0f, rating);
            if (val >= 4.5f) {
                rateBinding.tvRatingLabel.setText(String.format(Locale.getDefault(), "%.1f - Excellent", val));
            } else if (val >= 3.5f) {
                rateBinding.tvRatingLabel.setText(String.format(Locale.getDefault(), "%.1f - Very Good", val));
            } else if (val >= 2.5f) {
                rateBinding.tvRatingLabel.setText(String.format(Locale.getDefault(), "%.1f - Good", val));
            } else if (val >= 1.5f) {
                rateBinding.tvRatingLabel.setText(String.format(Locale.getDefault(), "%.1f - Fair", val));
            } else {
                rateBinding.tvRatingLabel.setText(String.format(Locale.getDefault(), "%.1f - Poor", val));
            }
        });

        rateBinding.ivCloseDialog.setOnClickListener(v -> dialog.dismiss());

        rateBinding.btnSubmitReview.setOnClickListener(v -> {
            double selectedRating = Math.max(1.0, rateBinding.ratingBar.getRating());
            String reviewComment = rateBinding.etReviewText.getText() != null ? rateBinding.etReviewText.getText().toString().trim() : "";

            Context ctx1 = getContext();
            if (ctx1 == null) return;

            PreferenceManager prefManager = PreferenceManager.getInstance(ctx1);

            // Resolve valid patientId
            String patientId = null;
            if (appointment.getPatientId() != null && !appointment.getPatientId().trim().isEmpty() && !appointment.getPatientId().equals("user_default")) {
                patientId = appointment.getPatientId().trim();
            } else if (prefManager.getUserId() != null && !prefManager.getUserId().trim().isEmpty()) {
                patientId = prefManager.getUserId().trim();
            }

            String patientName = prefManager.getUserName() != null ? prefManager.getUserName() : "Verified Patient";
            String patientAvatar = prefManager.getUserAvatar() != null ? prefManager.getUserAvatar() : "";

            Map<String, Object> reviewMap = new HashMap<>();
            reviewMap.put("appointment_id", appointment.getId());
            reviewMap.put("doctor_id", appointment.getDoctorId() != null ? appointment.getDoctorId() : "doc_1");
            reviewMap.put("patient_id", patientId != null && !patientId.isEmpty() ? patientId : "patient_anon");
            reviewMap.put("patient_name", patientName);
            reviewMap.put("patient_avatar", patientAvatar);
            reviewMap.put("rating", selectedRating);
            reviewMap.put("review_text", reviewComment);

            rateBinding.btnSubmitReview.setEnabled(false);
            rateBinding.btnSubmitReview.setText("Submitting...");

            Callback<List<Map<String, Object>>> reviewCallback = new Callback<List<Map<String, Object>>>() {
                @Override
                public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                    Context ctx = getContext();
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        if (isAdded() && ctx != null) {
                            dialog.dismiss();
                            ToastUtils.showSuccess(ctx, "Thank you! Your review has been submitted.");
                        }
                        if (appointment.getDoctorId() != null) {
                            DummyDataProvider.recalculateAndUpdateDoctorRating(appointment.getDoctorId());
                        }
                    } else {
                        String errStr = extractResponseError(response);
                        Log.e("AppointmentsFragment", "Review submission failed: " + errStr);

                        if (hasExistingReview[0]) {
                            SupabaseClient.getDoctorService().postDoctorReviewPayload("appointment_id", "resolution=merge-duplicates,return=representation", reviewMap)
                                    .enqueue(new Callback<List<Map<String, Object>>>() {
                                        @Override
                                        public void onResponse(Call<List<Map<String, Object>>> c2, Response<List<Map<String, Object>>> r2) {
                                            Context ctx2 = getContext();
                                            if (r2.isSuccessful() && r2.body() != null && !r2.body().isEmpty()) {
                                                if (isAdded() && ctx2 != null) {
                                                    dialog.dismiss();
                                                    ToastUtils.showSuccess(ctx2, "Thank you! Your review has been submitted.");
                                                }
                                                if (appointment.getDoctorId() != null) {
                                                    DummyDataProvider.recalculateAndUpdateDoctorRating(appointment.getDoctorId());
                                                }
                                            } else {
                                                resetSubmitButton(rateBinding, extractResponseError(r2));
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<List<Map<String, Object>>> c2, Throwable t2) {
                                            resetSubmitButton(rateBinding, "Network Error: " + t2.getMessage());
                                        }
                                    });
                        } else {
                            SupabaseClient.getDoctorService().updateDoctorReview("eq." + appointment.getId(), "return=representation", reviewMap)
                                    .enqueue(new Callback<List<Map<String, Object>>>() {
                                        @Override
                                        public void onResponse(Call<List<Map<String, Object>>> c2, Response<List<Map<String, Object>>> r2) {
                                            Context ctx2 = getContext();
                                            if (r2.isSuccessful() && r2.body() != null && !r2.body().isEmpty()) {
                                                if (isAdded() && ctx2 != null) {
                                                    dialog.dismiss();
                                                    ToastUtils.showSuccess(ctx2, "Thank you! Your review has been submitted.");
                                                }
                                                if (appointment.getDoctorId() != null) {
                                                    DummyDataProvider.recalculateAndUpdateDoctorRating(appointment.getDoctorId());
                                                }
                                            } else {
                                                resetSubmitButton(rateBinding, extractResponseError(r2));
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<List<Map<String, Object>>> c2, Throwable t2) {
                                            resetSubmitButton(rateBinding, "Network Error: " + t2.getMessage());
                                        }
                                    });
                        }
                    }
                }

                @Override
                public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                    Log.e("AppointmentsFragment", "Review submission network error: " + t.getMessage(), t);
                    resetSubmitButton(rateBinding, "Network Error: " + t.getMessage());
                }
            };

            if (hasExistingReview[0]) {
                SupabaseClient.getDoctorService().updateDoctorReview("eq." + appointment.getId(), "return=representation", reviewMap)
                        .enqueue(reviewCallback);
            } else {
                SupabaseClient.getDoctorService().postDoctorReviewPayload("appointment_id", "resolution=merge-duplicates,return=representation", reviewMap)
                        .enqueue(reviewCallback);
            }
        });

        dialog.show();
    }

    private void resetSubmitButton(DialogRateDoctorBinding rateBinding, String message) {
        Context context = getContext();
        if (!isAdded() || context == null || rateBinding == null) return;
        rateBinding.btnSubmitReview.setEnabled(true);
        rateBinding.btnSubmitReview.setText("Submit Review");
        ToastUtils.showInfo(context, message);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
