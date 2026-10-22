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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.amstudio.drpoint.R;
import com.amstudio.drpoint.adapter.AppointmentListAdapter;
import com.amstudio.drpoint.adapter.DateChipAdapter;
import com.amstudio.drpoint.adapter.TimeSlotAdapter;
import com.amstudio.drpoint.databinding.BottomSheetAppointmentDetailsBinding;
import com.amstudio.drpoint.databinding.BottomSheetRescheduleAppointmentBinding;
import com.amstudio.drpoint.databinding.DialogRateDoctorBinding;
import com.amstudio.drpoint.databinding.DialogRefundDetailsBinding;
import com.amstudio.drpoint.databinding.FragmentAppointmentsBinding;
import com.amstudio.drpoint.model.Appointment;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.model.DoctorReview;
import com.amstudio.drpoint.model.DoctorSlot;
import com.amstudio.drpoint.model.RefundRequest;
import com.amstudio.drpoint.network.SupabaseClient;
import com.amstudio.drpoint.network.model.CancelAppointmentRpcRequest;
import com.amstudio.drpoint.network.model.RescheduleAppointmentRpcRequest;
import com.amstudio.drpoint.network.model.RescheduleAppointmentRpcResponse;
import com.amstudio.drpoint.ui.booking.BookAppointmentActivity;
import com.amstudio.drpoint.ui.main.MainActivity;
import com.amstudio.drpoint.util.AvailabilityHelper;
import com.amstudio.drpoint.util.DummyDataProvider;
import com.amstudio.drpoint.util.PreferenceManager;
import com.amstudio.drpoint.util.ToastUtils;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

    // In-memory caching for appointments to prevent unnecessary Supabase calls on tab switches
    private static List<Appointment> cachedAppointments;
    private static long appointmentsCacheTimestamp = 0;
    private static final long APPOINTMENTS_CACHE_TTL_MS = 60 * 1000L; // 1 minute TTL

    public static synchronized void invalidateAppointmentsCache() {
        appointmentsCacheTimestamp = 0;
        cachedAppointments = null;
    }

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

        if (binding.rvAppointments != null) {
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
        }

        if (binding.btnBookNowEmpty != null) {
            binding.btnBookNowEmpty.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).selectTab(MainActivity.TAB_EXPLORE);
                }
            });
        }



        if (binding.tabLayoutAppointments != null) {
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
        }

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
        if (!isAdded() || getContext() == null || binding == null) return;
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

        // Serve from memory cache if fresh (< 1 min) to avoid repeated Supabase calls on tab switch
        if (cachedAppointments != null && (System.currentTimeMillis() - appointmentsCacheTimestamp < APPOINTMENTS_CACHE_TTL_MS)) {
            fetchedAppointments = new ArrayList<>(cachedAppointments);
            filterAppointments(currentTabPosition);
            return;
        }

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
                            synchronized (AppointmentsFragment.class) {
                                cachedAppointments = new ArrayList<>(fetchedAppointments);
                                appointmentsCacheTimestamp = System.currentTimeMillis();
                            }
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
        if (!isAdded() || getContext() == null) {
            if (onComplete != null) onComplete.run();
            return;
        }
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

            Collections.sort(group, (a1, a2) -> Integer.compare(a1.getStartTimeMinutes(), a2.getStartTimeMinutes()));

            Set<Integer> uniqueTokens = new HashSet<>();
            boolean hasZeros = false;
            for (Appointment a : group) {
                if (a != null) {
                    if (a.getTokenNumber() <= 0) {
                        hasZeros = true;
                    } else {
                        uniqueTokens.add(a.getTokenNumber());
                    }
                }
            }

            if (hasZeros || uniqueTokens.size() < group.size()) {
                int seq = 1;
                for (Appointment a : group) {
                    if (a != null) {
                        a.setTokenNumber(seq++);
                    }
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

            boolean isUserMatch = !pId.isEmpty() && (
                    pId.equalsIgnoreCase(currentUserId) ||
                    (currentUuid != null && pId.equalsIgnoreCase(currentUuid))
            );

            if (!isUserMatch) {
                continue;
            }

            String st = appt.getStatus() != null ? appt.getStatus().toLowerCase(Locale.US) : "confirmed";

            if (tabPosition == 0) {
                boolean isExcludedStatus = st.contains("cancel") || st.contains("reject") ||
                        st.contains("no_show") || st.contains("completed") || st.contains("refund");

                boolean isSchedulePassed = AvailabilityHelper.isAppointmentSchedulePassed(appt, 20) ||
                        AvailabilityHelper.isSlotEndPassed(appt.getAppointmentDate(), appt.getStartTime(), appt.getEndTime(), 20);

                if (!isExcludedStatus && !isSchedulePassed) {
                    if ("pending".equals(st) || "confirmed".equals(st) || "checked_in".equals(st) ||
                            "waiting".equals(st) || "in_consultation".equals(st) || st.contains("confirm")) {
                        displayed.add(appt);
                    }
                }
            } else if (tabPosition == 1) {
                if ("completed".equals(st)) {
                    displayed.add(appt);
                }
            } else if (tabPosition == 2) {
                if ("cancelled".equals(st) || "rejected".equals(st) || "no_show".equals(st) || st.contains("cancel") || st.contains("reject")) {
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

        boolean isLateCancel = AvailabilityHelper.isLessThanOneHourBeforeStart(appointment);
        String cancelReason = isLateCancel ? "Cancelled by patient (Late cancellation - Less than 1 hour remaining)" : "Cancelled by patient";

        appointment.setStatus("Cancelled");
        appointment.setPatientReason(cancelReason);
        invalidateAppointmentsCache();
        DummyDataProvider.invalidateDoctorsCache();

        String patientId = PreferenceManager.getInstance(context).getUserId();
        CancelAppointmentRpcRequest cancelRequest = new CancelAppointmentRpcRequest(appointment.getId(), patientId, cancelReason);

        SupabaseClient.getSlotService().cancelAppointment(cancelRequest).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                Context ctx = getContext();
                if (!isAdded() || ctx == null) return;
                if (isLateCancel) {
                    ToastUtils.showSuccess(ctx, "Appointment cancelled successfully. (No refund applicable for cancellations within 1 hour of schedule)");
                } else {
                    ToastUtils.showSuccess(ctx, "Appointment cancelled successfully.");
                }
                loadAppointments();
                if (!isLateCancel) {
                    openRefundDetailsBottomSheet(appointment);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Map<String, Object> updateMap = new HashMap<>();
                updateMap.put("status", "Cancelled");
                updateMap.put("patient_reason", cancelReason);
                updateMap.put("notes", cancelReason);

                SupabaseClient.getAppointmentService()
                        .updateAppointmentStatus("eq." + appointment.getId(), updateMap)
                        .enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                Context ctx = getContext();
                                if (!isAdded() || ctx == null) return;
                                if (isLateCancel) {
                                    ToastUtils.showSuccess(ctx, "Appointment cancelled successfully. (No refund applicable for cancellations within 1 hour of schedule)");
                                } else {
                                    ToastUtils.showSuccess(ctx, "Appointment cancelled successfully.");
                                }
                                loadAppointments();
                                if (!isLateCancel) {
                                    openRefundDetailsBottomSheet(appointment);
                                }
                            }

                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                Context ctx = getContext();
                                if (!isAdded() || ctx == null) return;
                                ToastUtils.showSuccess(ctx, "Appointment marked cancelled.");
                                loadAppointments();
                                if (!isLateCancel) {
                                    openRefundDetailsBottomSheet(appointment);
                                }
                            }
                        });
            }
        });
    }

    private DoctorSlot selectedRescheduleSlot = null;
    private boolean isRescheduleSubmitting = false;

    private void openRescheduleFlow(Appointment appointment) {
        if (appointment == null || !isAdded()) return;
        Context context = getContext();
        if (context == null) return;

        PreferenceManager prefManager = PreferenceManager.getInstance(context);
        if (!prefManager.isLoggedIn()) {
            ToastUtils.showWarning(context, "Please log in to reschedule appointments.");
            return;
        }

        String currentUserId = prefManager.getUserId();
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            ToastUtils.showWarning(context, "User ID not found. Please log in again.");
            return;
        }

        // Security check: Ensure patient owns this appointment
        String apptPatientId = appointment.getPatientId();
        if (apptPatientId != null && !apptPatientId.trim().isEmpty()) {
            String cleanCurrent = currentUserId.trim();
            String cleanApptPatient = apptPatientId.trim();
            String uuidCurrent = formatUuidOrNull(cleanCurrent);
            boolean isOwner = cleanCurrent.equalsIgnoreCase(cleanApptPatient) ||
                    (uuidCurrent != null && uuidCurrent.equalsIgnoreCase(cleanApptPatient));
            if (!isOwner) {
                ToastUtils.showError(context, "You can only reschedule your own appointments.");
                return;
            }
        }

        // Status check: Block completed/cancelled/in-consultation appointments
        if (!appointment.isCancellable()) {
            ToastUtils.showWarning(context, "This appointment cannot be rescheduled.");
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(context);
        BottomSheetRescheduleAppointmentBinding sheetBinding = BottomSheetRescheduleAppointmentBinding.inflate(getLayoutInflater());
        dialog.setContentView(sheetBinding.getRoot());

        // Configure BottomSheet Behavior
        FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        }

        // Reset state variables
        selectedRescheduleSlot = null;
        isRescheduleSubmitting = false;

        // Populate Doctor Info
        sheetBinding.tvRescheduleDoctorName.setText(appointment.getDoctorName());
        sheetBinding.tvRescheduleSpecialization.setText(appointment.getSpecialization());
        sheetBinding.tvRescheduleClinicName.setText(appointment.getClinicName());

        if (appointment.getDoctor() != null && appointment.getDoctor().getImageUrl() != null && !appointment.getDoctor().getImageUrl().isEmpty()) {
            try {
                Glide.with(context)
                        .load(appointment.getDoctor().getImageUrl())
                        .placeholder(R.drawable.ic_user)
                        .into(sheetBinding.ivRescheduleDoctorImage);
            } catch (Exception ignored) {}
        }

        // Populate Current Schedule Text
        String currentDateStr = appointment.getDate() != null ? appointment.getDate() : "18 Sep 2026";
        String currentTimeStr = appointment.getFormattedTime();
        sheetBinding.tvCurrentScheduleText.setText(currentDateStr + " • " + currentTimeStr);

        // Keep Appointment / Dismiss Listener
        sheetBinding.btnKeepAppointment.setOnClickListener(v -> dialog.dismiss());

        // Fetch Doctor Slots from Supabase
        String doctorId = appointment.getDoctorId();
        if (doctorId == null || doctorId.trim().isEmpty()) {
            doctorId = "doc_1";
        }
        final String finalDoctorId = doctorId.trim();

        SimpleDateFormat sdfToday = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        sdfToday.setTimeZone(AvailabilityHelper.getAppTimeZone());
        String todayDate = sdfToday.format(new Date(AvailabilityHelper.getCalibratedCurrentTimeMillis()));

        sheetBinding.pbRescheduleSlotsLoading.setVisibility(View.VISIBLE);
        sheetBinding.llRescheduleSlotsContainer.setVisibility(View.GONE);
        sheetBinding.tvRescheduleNoSlots.setVisibility(View.GONE);

        // Query slots strictly for this doctor
        SupabaseClient.getSlotService().getDoctorAllSlots("eq." + finalDoctorId, "gte." + todayDate)
                .enqueue(new Callback<List<DoctorSlot>>() {
                    @Override
                    public void onResponse(Call<List<DoctorSlot>> call, Response<List<DoctorSlot>> response) {
                        if (!dialog.isShowing() || !isAdded()) return;
                        List<DoctorSlot> slotsFromDb = (response.isSuccessful() && response.body() != null) ? response.body() : new ArrayList<>();
                        fetchSchedulesAndCombineRescheduleUI(dialog, sheetBinding, appointment, finalDoctorId, todayDate, slotsFromDb);
                    }

                    @Override
                    public void onFailure(Call<List<DoctorSlot>> call, Throwable t) {
                        if (!dialog.isShowing() || !isAdded()) return;
                        fetchSchedulesAndCombineRescheduleUI(dialog, sheetBinding, appointment, finalDoctorId, todayDate, new ArrayList<>());
                    }
                });

        dialog.show();
    }

    private void fetchSchedulesAndCombineRescheduleUI(BottomSheetDialog dialog, BottomSheetRescheduleAppointmentBinding sheetBinding, Appointment appointment, String doctorId, String todayDate, List<DoctorSlot> slotsFromDb) {
        SupabaseClient.getSlotService().getDoctorSchedules("eq." + doctorId, "gte." + todayDate)
                .enqueue(new Callback<List<DoctorSlot>>() {
                    @Override
                    public void onResponse(Call<List<DoctorSlot>> call, Response<List<DoctorSlot>> response) {
                        if (!dialog.isShowing() || !isAdded()) return;
                        List<DoctorSlot> schedulesFromDb = (response.isSuccessful() && response.body() != null) ? response.body() : new ArrayList<>();
                        List<DoctorSlot> combined = combineSlotsAndSchedules(slotsFromDb, schedulesFromDb);
                        crossReferenceBookedAndSetupRescheduleUI(dialog, sheetBinding, appointment, doctorId, combined);
                    }

                    @Override
                    public void onFailure(Call<List<DoctorSlot>> call, Throwable t) {
                        if (!dialog.isShowing() || !isAdded()) return;
                        List<DoctorSlot> combined = combineSlotsAndSchedules(slotsFromDb, new ArrayList<>());
                        crossReferenceBookedAndSetupRescheduleUI(dialog, sheetBinding, appointment, doctorId, combined);
                    }
                });
    }

    private List<DoctorSlot> combineSlotsAndSchedules(List<DoctorSlot> slotsFromDb, List<DoctorSlot> schedulesFromDb) {
        List<DoctorSlot> combined = new ArrayList<>();
        Set<String> keys = new HashSet<>();

        if (slotsFromDb != null) {
            for (DoctorSlot s : slotsFromDb) {
                if (s != null && s.getSlotDate() != null && s.getStartTime() != null) {
                    String cleanDate = AvailabilityHelper.cleanDateString(s.getSlotDate());
                    String normTime = AvailabilityHelper.normalizeTimeFormat(s.getStartTime());
                    keys.add(cleanDate + "_" + normTime);
                    combined.add(s);
                }
            }
        }

        if (schedulesFromDb != null && !schedulesFromDb.isEmpty()) {
            List<DoctorSlot> expanded = expandSchedulesToSlotsForReschedule(schedulesFromDb);
            for (DoctorSlot schedSlot : expanded) {
                if (schedSlot != null && schedSlot.getSlotDate() != null && schedSlot.getStartTime() != null) {
                    String cleanDate = AvailabilityHelper.cleanDateString(schedSlot.getSlotDate());
                    String normTime = AvailabilityHelper.normalizeTimeFormat(schedSlot.getStartTime());
                    String key = cleanDate + "_" + normTime;
                    if (!keys.contains(key)) {
                        keys.add(key);
                        combined.add(schedSlot);
                    }
                }
            }
        }

        return combined;
    }

    private List<DoctorSlot> expandSchedulesToSlotsForReschedule(List<DoctorSlot> schedules) {
        List<DoctorSlot> expanded = new ArrayList<>();
        if (schedules == null) return expanded;
        for (DoctorSlot sched : schedules) {
            if (sched == null) continue;
            String dateStr = sched.getSlotDate();
            if (dateStr == null || dateStr.trim().isEmpty()) continue;
            String cleanDate = AvailabilityHelper.cleanDateString(dateStr);

            if (sched.getMorningStart() != null && sched.getMorningEnd() != null) {
                expanded.addAll(generateSlotsForTimeRange(sched, cleanDate, sched.getMorningStart(), sched.getMorningEnd()));
            }
            if (sched.getEveningStart() != null && sched.getEveningEnd() != null) {
                expanded.addAll(generateSlotsForTimeRange(sched, cleanDate, sched.getEveningStart(), sched.getEveningEnd()));
            }
        }
        return expanded;
    }

    private List<DoctorSlot> generateSlotsForTimeRange(DoctorSlot sched, String dateStr, String startStr, String endStr) {
        List<DoctorSlot> slots = new ArrayList<>();
        try {
            String normStart = AvailabilityHelper.normalizeTimeFormat(startStr);
            String normEnd = AvailabilityHelper.normalizeTimeFormat(endStr);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            sdf.setTimeZone(AvailabilityHelper.getAppTimeZone());

            Date current = sdf.parse(dateStr + " " + normStart);
            Date end = sdf.parse(dateStr + " " + normEnd);

            if (current == null || end == null) return slots;

            int durationMins = sched.getSlotDuration() > 0 ? sched.getSlotDuration() : 20;
            SimpleDateFormat timeOutSdf = new SimpleDateFormat("HH:mm:ss", Locale.US);
            timeOutSdf.setTimeZone(AvailabilityHelper.getAppTimeZone());

            while (current.before(end)) {
                String startTimeFormatted = timeOutSdf.format(current);
                long nextMillis = current.getTime() + (durationMins * 60L * 1000L);
                Date nextDate = new Date(nextMillis);
                String endTimeFormatted = timeOutSdf.format(nextDate);

                String slotId = "sched_slot_" + sched.getDoctorId() + "_" + dateStr + "_" + startTimeFormatted.replace(":", "");

                DoctorSlot slot = new DoctorSlot(
                        slotId,
                        sched.getDoctorId(),
                        sched.getClinicId(),
                        dateStr,
                        startTimeFormatted,
                        endTimeFormatted,
                        "available"
                );
                slot.setFee(sched.getFee());
                slots.add(slot);

                current = nextDate;
            }
        } catch (Exception ignored) {}
        return slots;
    }

    private void crossReferenceBookedAndSetupRescheduleUI(BottomSheetDialog dialog, BottomSheetRescheduleAppointmentBinding sheetBinding, Appointment appointment, String doctorId, List<DoctorSlot> availableSlots) {
        SupabaseClient.getAppointmentService().getAppointmentsForDoctor("eq." + doctorId)
                .enqueue(new Callback<List<Appointment>>() {
                    @Override
                    public void onResponse(Call<List<Appointment>> call, Response<List<Appointment>> response) {
                        if (!dialog.isShowing() || !isAdded()) return;
                        List<Appointment> doctorAppts = (response.isSuccessful() && response.body() != null) ? response.body() : new ArrayList<>();
                        markBookedSlotsForReschedule(availableSlots, doctorAppts, appointment.getId());
                        setupRescheduleDateAndSlotUI(dialog, sheetBinding, appointment, availableSlots);
                    }

                    @Override
                    public void onFailure(Call<List<Appointment>> call, Throwable t) {
                        if (!dialog.isShowing() || !isAdded()) return;
                        setupRescheduleDateAndSlotUI(dialog, sheetBinding, appointment, availableSlots);
                    }
                });
    }

    private void markBookedSlotsForReschedule(List<DoctorSlot> slots, List<Appointment> doctorAppts, String currentAppointmentId) {
        if (slots == null || slots.isEmpty()) return;

        Map<String, Set<String>> bookedTimesMap = new HashMap<>();
        if (doctorAppts != null) {
            for (Appointment appt : doctorAppts) {
                if (appt == null) continue;
                // Exclude cancelled/rejected appointments AND the current appointment being rescheduled
                if (appt.getId() != null && appt.getId().equalsIgnoreCase(currentAppointmentId)) {
                    continue;
                }
                String st = appt.getStatus();
                if (st != null && (st.equalsIgnoreCase("Cancelled") || st.equalsIgnoreCase("Rejected") || st.equalsIgnoreCase("Canceled"))) {
                    continue;
                }
                String date = AvailabilityHelper.cleanDateString(appt.getAppointmentDate());
                String time = AvailabilityHelper.normalizeTimeFormat(appt.getStartTime());

                if (!date.isEmpty() && !time.isEmpty()) {
                    bookedTimesMap.computeIfAbsent(date, k -> new HashSet<>()).add(time);
                }
            }
        }

        for (DoctorSlot slot : slots) {
            if (slot == null) continue;
            String slotDate = AvailabilityHelper.cleanDateString(slot.getSlotDate());
            String slotTime = AvailabilityHelper.normalizeTimeFormat(slot.getStartTime());

            Set<String> bookedSet = bookedTimesMap.get(slotDate);
            if (bookedSet != null && bookedSet.contains(slotTime)) {
                slot.setStatus("booked");
            }
        }
    }

    private void setupRescheduleDateAndSlotUI(BottomSheetDialog dialog, BottomSheetRescheduleAppointmentBinding sheetBinding, Appointment appointment, List<DoctorSlot> slots) {
        if (!dialog.isShowing() || !isAdded()) return;

        sheetBinding.pbRescheduleSlotsLoading.setVisibility(View.GONE);

        String clinicId = appointment.getClinicId();
        List<DateChipAdapter.DateItem> dateChips = AvailabilityHelper.extractAvailableDateChips(slots, clinicId);

        if (dateChips == null || dateChips.isEmpty()) {
            sheetBinding.tvRescheduleNoSlots.setVisibility(View.VISIBLE);
            sheetBinding.llRescheduleSlotsContainer.setVisibility(View.GONE);
            return;
        }

        sheetBinding.tvRescheduleNoSlots.setVisibility(View.GONE);
        sheetBinding.llRescheduleSlotsContainer.setVisibility(View.VISIBLE);

        DateChipAdapter dateAdapter = new DateChipAdapter((dateItem, position) -> {
            loadRescheduleTimeSlotsForDate(dialog, sheetBinding, appointment, slots, clinicId, dateItem);
        });

        sheetBinding.rvRescheduleDates.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        sheetBinding.rvRescheduleDates.setAdapter(dateAdapter);
        dateAdapter.submitList(dateChips);

        // Select first date chip by default
        if (!dateChips.isEmpty()) {
            dateAdapter.setSelectedPosition(0);
            loadRescheduleTimeSlotsForDate(dialog, sheetBinding, appointment, slots, clinicId, dateChips.get(0));
        }
    }

    private void loadRescheduleTimeSlotsForDate(BottomSheetDialog dialog, BottomSheetRescheduleAppointmentBinding sheetBinding, Appointment appointment, List<DoctorSlot> allSlots, String clinicId, DateChipAdapter.DateItem dateItem) {
        if (dateItem == null) return;
        String selectedDateRaw = dateItem.getRawDate();

        List<DoctorSlot> slotsForDate = AvailabilityHelper.filterAndSortSlots(allSlots, clinicId, selectedDateRaw);

        // Filter ONLY valid available and future time slots
        List<DoctorSlot> availableOnlySlots = new ArrayList<>();
        for (DoctorSlot slot : slotsForDate) {
            if (slot == null) continue;
            if (AvailabilityHelper.isSlotValidAndBookable(slot) || ("available".equalsIgnoreCase(slot.getStatus()) && !AvailabilityHelper.isSlotTimeReachedOrPassed(slot))) {
                availableOnlySlots.add(slot);
            }
        }

        List<DoctorSlot> morningSlots = new ArrayList<>();
        List<DoctorSlot> eveningSlots = new ArrayList<>();

        for (DoctorSlot slot : availableOnlySlots) {
            String normTime = AvailabilityHelper.normalizeTimeFormat(slot.getStartTime());
            try {
                int hour = Integer.parseInt(normTime.split(":")[0]);
                if (hour < 12) {
                    morningSlots.add(slot);
                } else {
                    eveningSlots.add(slot);
                }
            } catch (Exception e) {
                morningSlots.add(slot);
            }
        }

        selectedRescheduleSlot = null;

        sheetBinding.rvRescheduleMorningSlots.setLayoutManager(new GridLayoutManager(getContext(), 3));
        sheetBinding.rvRescheduleEveningSlots.setLayoutManager(new GridLayoutManager(getContext(), 3));

        TimeSlotAdapter morningAdapter = new TimeSlotAdapter((slot, position) -> {
            selectedRescheduleSlot = slot;
            if (sheetBinding.rvRescheduleEveningSlots.getAdapter() instanceof TimeSlotAdapter) {
                ((TimeSlotAdapter) sheetBinding.rvRescheduleEveningSlots.getAdapter()).setSelectedPosition(-1);
            }
            onRescheduleSlotSelected(dialog, sheetBinding, appointment, dateItem, slot);
        });

        TimeSlotAdapter eveningAdapter = new TimeSlotAdapter((slot, position) -> {
            selectedRescheduleSlot = slot;
            if (sheetBinding.rvRescheduleMorningSlots.getAdapter() instanceof TimeSlotAdapter) {
                ((TimeSlotAdapter) sheetBinding.rvRescheduleMorningSlots.getAdapter()).setSelectedPosition(-1);
            }
            onRescheduleSlotSelected(dialog, sheetBinding, appointment, dateItem, slot);
        });

        sheetBinding.rvRescheduleMorningSlots.setAdapter(morningAdapter);
        sheetBinding.rvRescheduleEveningSlots.setAdapter(eveningAdapter);

        if (morningSlots.isEmpty()) {
            sheetBinding.tvRescheduleMorningLabel.setVisibility(View.GONE);
            sheetBinding.rvRescheduleMorningSlots.setVisibility(View.GONE);
        } else {
            sheetBinding.tvRescheduleMorningLabel.setVisibility(View.VISIBLE);
            sheetBinding.rvRescheduleMorningSlots.setVisibility(View.VISIBLE);
            morningAdapter.submitList(morningSlots);
        }

        if (eveningSlots.isEmpty()) {
            sheetBinding.tvRescheduleEveningLabel.setVisibility(View.GONE);
            sheetBinding.rvRescheduleEveningSlots.setVisibility(View.GONE);
        } else {
            sheetBinding.tvRescheduleEveningLabel.setVisibility(View.VISIBLE);
            sheetBinding.rvRescheduleEveningSlots.setVisibility(View.VISIBLE);
            eveningAdapter.submitList(eveningSlots);
        }

        if (morningSlots.isEmpty() && eveningSlots.isEmpty()) {
            sheetBinding.tvRescheduleNoSlots.setVisibility(View.VISIBLE);
            sheetBinding.llRescheduleSlotsContainer.setVisibility(View.GONE);
            sheetBinding.btnConfirmReschedule.setEnabled(false);
            sheetBinding.btnConfirmReschedule.setAlpha(0.6f);
            sheetBinding.tvNewSchedulePreview.setText("No available time slots for this date");
        } else {
            sheetBinding.tvRescheduleNoSlots.setVisibility(View.GONE);
            sheetBinding.llRescheduleSlotsContainer.setVisibility(View.VISIBLE);

            // Auto-select the first available time slot by default
            if (!morningSlots.isEmpty()) {
                morningAdapter.setSelectedPosition(0);
                selectedRescheduleSlot = morningSlots.get(0);
                onRescheduleSlotSelected(dialog, sheetBinding, appointment, dateItem, morningSlots.get(0));
            } else if (!eveningSlots.isEmpty()) {
                eveningAdapter.setSelectedPosition(0);
                selectedRescheduleSlot = eveningSlots.get(0);
                onRescheduleSlotSelected(dialog, sheetBinding, appointment, dateItem, eveningSlots.get(0));
            }
        }
    }

    private void onRescheduleSlotSelected(BottomSheetDialog dialog, BottomSheetRescheduleAppointmentBinding sheetBinding, Appointment appointment, DateChipAdapter.DateItem dateItem, DoctorSlot selectedSlot) {
        if (selectedSlot == null || dateItem == null) {
            sheetBinding.btnConfirmReschedule.setEnabled(false);
            sheetBinding.btnConfirmReschedule.setAlpha(0.6f);
            sheetBinding.tvNewSchedulePreview.setText("Please select an available time slot");
            return;
        }

        String formattedDate = dateItem.getDate() != null ? dateItem.getDate() : dateItem.getRawDate();
        String formattedTime = selectedSlot.getFormattedTime();

        sheetBinding.tvNewSchedulePreview.setText(formattedDate + " • " + formattedTime);
        sheetBinding.btnConfirmReschedule.setEnabled(true);
        sheetBinding.btnConfirmReschedule.setAlpha(1.0f);

        sheetBinding.btnConfirmReschedule.setOnClickListener(v -> {
            if (isRescheduleSubmitting) return;
            if (selectedRescheduleSlot == null) {
                ToastUtils.showWarning(getContext(), "Please select an available time slot first.");
                return;
            }
            showConfirmRescheduleDialog(dialog, sheetBinding, appointment, dateItem, selectedSlot);
        });
    }

    private void showConfirmRescheduleDialog(BottomSheetDialog dialog, BottomSheetRescheduleAppointmentBinding sheetBinding, Appointment appointment, DateChipAdapter.DateItem dateItem, DoctorSlot newSlot) {
        Context context = getContext();
        if (context == null || appointment == null || newSlot == null) return;

        String oldDate = appointment.getDate() != null ? appointment.getDate() : "18 Sep";
        String oldTime = appointment.getFormattedTime();
        String newDate = dateItem.getDate() != null ? dateItem.getDate() : dateItem.getRawDate();
        String newTime = newSlot.getFormattedTime();

        String message = "Reschedule Appointment?\n\n" +
                "Doctor: " + appointment.getDoctorName() + "\n\n" +
                "Current: " + oldDate + " • " + oldTime + "\n" +
                "New: " + newDate + " • " + newTime;

        new MaterialAlertDialogBuilder(context)
                .setTitle("Confirm Reschedule")
                .setMessage(message)
                .setNegativeButton("Keep Appointment", (d, which) -> d.dismiss())
                .setPositiveButton("Confirm Reschedule", (d, which) -> {
                    d.dismiss();
                    executeRescheduleProcess(dialog, sheetBinding, appointment, newSlot, newDate, newTime);
                })
                .show();
    }

    private void executeRescheduleProcess(BottomSheetDialog dialog, BottomSheetRescheduleAppointmentBinding sheetBinding, Appointment appointment, DoctorSlot newSlot, String newDateFormatted, String newTimeFormatted) {
        Context context = getContext();
        if (context == null || appointment == null || newSlot == null) return;

        PreferenceManager prefManager = PreferenceManager.getInstance(context);
        String patientId = prefManager.getUserId();
        if (patientId == null || patientId.trim().isEmpty()) {
            ToastUtils.showError(context, "User session expired. Please log in again.");
            return;
        }

        isRescheduleSubmitting = true;
        sheetBinding.btnConfirmReschedule.setEnabled(false);
        sheetBinding.btnKeepAppointment.setEnabled(false);
        sheetBinding.pbConfirmReschedule.setVisibility(View.VISIBLE);

        String cleanPatientId = patientId.trim();
        String validPatientUuid = formatUuidOrNull(cleanPatientId);
        if (validPatientUuid == null) validPatientUuid = cleanPatientId;

        String rawApptId = appointment.getId();
        String validApptUuid = formatUuidOrNull(rawApptId);
        if (validApptUuid == null) validApptUuid = rawApptId;

        String rawSlotId = newSlot.getId();
        String validSlotUuid = formatUuidOrNull(rawSlotId);
        if (validSlotUuid == null) validSlotUuid = rawSlotId;

        final String finalPatientUuid = validPatientUuid;
        final String finalApptUuid = validApptUuid;
        final String finalSlotUuid = validSlotUuid;

        RescheduleAppointmentRpcRequest request = new RescheduleAppointmentRpcRequest(finalApptUuid, finalPatientUuid, finalSlotUuid);

        // Try RPC Stored Procedure first
        SupabaseClient.getSlotService().rescheduleAppointment(request)
                .enqueue(new Callback<RescheduleAppointmentRpcResponse>() {
                    @Override
                    public void onResponse(Call<RescheduleAppointmentRpcResponse> call, Response<RescheduleAppointmentRpcResponse> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful() && response.body() != null) {
                            RescheduleAppointmentRpcResponse rpcResp = response.body();
                            if (rpcResp.isSuccess()) {
                                onRescheduleSuccess(dialog, context, sheetBinding);
                                return;
                            } else {
                                String msg = rpcResp.getMessage();
                                Log.w("AppointmentsFragment", "Reschedule RPC returned failure: " + msg);
                                if (msg != null && (msg.contains("no longer available") || msg.contains("Unauthorized"))) {
                                    onRescheduleFailed(context, sheetBinding, msg);
                                    return;
                                }
                            }
                        } else {
                            Log.w("AppointmentsFragment", "Reschedule RPC HTTP error code: " + response.code());
                        }
                        // Fallback to client-side atomic update
                        executeClientRescheduleFallback(dialog, context, sheetBinding, appointment, newSlot, finalPatientUuid, finalApptUuid, finalSlotUuid);
                    }

                    @Override
                    public void onFailure(Call<RescheduleAppointmentRpcResponse> call, Throwable t) {
                        if (!isAdded()) return;
                        Log.w("AppointmentsFragment", "Reschedule RPC network failure: " + t.getMessage());
                        executeClientRescheduleFallback(dialog, context, sheetBinding, appointment, newSlot, finalPatientUuid, finalApptUuid, finalSlotUuid);
                    }
                });
    }

    private void executeClientRescheduleFallback(BottomSheetDialog dialog, Context context, BottomSheetRescheduleAppointmentBinding sheetBinding, Appointment appointment, DoctorSlot newSlot, String patientUuid, String apptUuid, String slotUuid) {
        String doctorId = appointment.getDoctorId();
        String newSlotDate = newSlot.getSlotDate();
        String newStartTime = newSlot.getStartTime();

        // 1. Double-check slot availability in DB
        SupabaseClient.getAppointmentService().getAppointmentsForDoctor("eq." + doctorId)
                .enqueue(new Callback<List<Appointment>>() {
                    @Override
                    public void onResponse(Call<List<Appointment>> call, Response<List<Appointment>> response) {
                        if (!isAdded()) return;
                        List<Appointment> doctorAppts = (response.isSuccessful() && response.body() != null) ? response.body() : new ArrayList<>();

                        // Check if another appointment already booked this slot
                        String normNewTime = AvailabilityHelper.normalizeTimeFormat(newStartTime);
                        String cleanNewDate = AvailabilityHelper.cleanDateString(newSlotDate);

                        for (Appointment appt : doctorAppts) {
                            if (appt == null) continue;
                            if (appt.getId() != null && appt.getId().equalsIgnoreCase(appointment.getId())) continue;
                            String st = appt.getStatus();
                            if (st != null && (st.equalsIgnoreCase("Cancelled") || st.equalsIgnoreCase("Rejected") || st.equalsIgnoreCase("Canceled"))) continue;

                            String apptDate = AvailabilityHelper.cleanDateString(appt.getAppointmentDate());
                            String apptTime = AvailabilityHelper.normalizeTimeFormat(appt.getStartTime());

                            if (cleanNewDate.equalsIgnoreCase(apptDate) && normNewTime.equalsIgnoreCase(apptTime)) {
                                onRescheduleFailed(context, sheetBinding, "This slot is no longer available. Please select another slot.");
                                return;
                            }
                        }

                        // 2. Calculate new token number chronologically for doctor on that date
                        int tokenCount = 1;
                        for (Appointment appt : doctorAppts) {
                            if (appt == null) continue;
                            String st = appt.getStatus();
                            if (st != null && (st.equalsIgnoreCase("Cancelled") || st.equalsIgnoreCase("Rejected") || st.equalsIgnoreCase("Canceled"))) continue;

                            String apptDate = AvailabilityHelper.cleanDateString(appt.getAppointmentDate());
                            String apptTime = AvailabilityHelper.normalizeTimeFormat(appt.getStartTime());

                            if (cleanNewDate.equalsIgnoreCase(apptDate) && apptTime.compareTo(normNewTime) <= 0) {
                                tokenCount++;
                            }
                        }

                        final int newTokenNumber = tokenCount;

                        // Ensure doctor_slots table has this slot row created if generated from schedule
                        if (newSlot.getId() != null && newSlot.getId().startsWith("sched_slot_")) {
                            DoctorSlot dbSlot = new DoctorSlot(
                                    slotUuid,
                                    doctorId,
                                    appointment.getClinicId(),
                                    cleanNewDate,
                                    newSlot.getStartTime(),
                                    newSlot.getEndTime(),
                                    "booked"
                            );
                            dbSlot.setAppointmentId(apptUuid);
                            SupabaseClient.getSlotService().createSlot("return=representation", dbSlot).enqueue(new Callback<List<DoctorSlot>>() {
                                @Override public void onResponse(Call<List<DoctorSlot>> c, Response<List<DoctorSlot>> r) {}
                                @Override public void onFailure(Call<List<DoctorSlot>> c, Throwable t) {}
                            });
                        }

                        // 3. Update appointment record in Supabase
                        Map<String, Object> updateMap = new HashMap<>();
                        updateMap.put("slot_id", slotUuid);
                        updateMap.put("appointment_date", cleanNewDate);
                        updateMap.put("start_time", newSlot.getStartTime());
                        if (newSlot.getEndTime() != null) {
                            updateMap.put("end_time", newSlot.getEndTime());
                        }
                        updateMap.put("token_number", newTokenNumber);

                        String queryParam = "eq." + appointment.getId();
                        String altQueryParam = "eq." + apptUuid;

                        SupabaseClient.getAppointmentService().updateAppointmentStatus(queryParam, updateMap)
                                .enqueue(new Callback<Void>() {
                                    @Override
                                    public void onResponse(Call<Void> call, Response<Void> response) {
                                        if (!isAdded()) return;
                                        if (response.isSuccessful()) {
                                            finishFallbackReschedule(dialog, context, sheetBinding, appointment, newSlot, slotUuid);
                                        } else {
                                            if (!queryParam.equalsIgnoreCase(altQueryParam)) {
                                                SupabaseClient.getAppointmentService().updateAppointmentStatus(altQueryParam, updateMap)
                                                        .enqueue(new Callback<Void>() {
                                                            @Override
                                                            public void onResponse(Call<Void> c2, Response<Void> r2) {
                                                                if (!isAdded()) return;
                                                                if (r2.isSuccessful()) {
                                                                    finishFallbackReschedule(dialog, context, sheetBinding, appointment, newSlot, slotUuid);
                                                                } else {
                                                                    retryWithoutSlotId(dialog, context, sheetBinding, appointment, newSlot, cleanNewDate, newTokenNumber, queryParam, altQueryParam);
                                                                }
                                                            }

                                                            @Override
                                                            public void onFailure(Call<Void> c2, Throwable t2) {
                                                                retryWithoutSlotId(dialog, context, sheetBinding, appointment, newSlot, cleanNewDate, newTokenNumber, queryParam, altQueryParam);
                                                            }
                                                        });
                                            } else {
                                                retryWithoutSlotId(dialog, context, sheetBinding, appointment, newSlot, cleanNewDate, newTokenNumber, queryParam, altQueryParam);
                                            }
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<Void> call, Throwable t) {
                                        if (!isAdded()) return;
                                        retryWithoutSlotId(dialog, context, sheetBinding, appointment, newSlot, cleanNewDate, newTokenNumber, queryParam, altQueryParam);
                                    }
                                });
                    }

                    @Override
                    public void onFailure(Call<List<Appointment>> call, Throwable t) {
                        if (!isAdded()) return;
                        onRescheduleFailed(context, sheetBinding, "Unable to reschedule appointment. Please check network connection and try again.");
                    }
                });
    }

    private void retryWithoutSlotId(BottomSheetDialog dialog, Context context, BottomSheetRescheduleAppointmentBinding sheetBinding, Appointment appointment, DoctorSlot newSlot, String cleanNewDate, int newTokenNumber, String queryParam, String altQueryParam) {
        Map<String, Object> fallbackMap = new HashMap<>();
        fallbackMap.put("appointment_date", cleanNewDate);
        fallbackMap.put("start_time", newSlot.getStartTime());
        if (newSlot.getEndTime() != null) {
            fallbackMap.put("end_time", newSlot.getEndTime());
        }
        fallbackMap.put("token_number", newTokenNumber);

        SupabaseClient.getAppointmentService().updateAppointmentStatus(queryParam, fallbackMap)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful()) {
                            onRescheduleSuccess(dialog, context, sheetBinding);
                        } else {
                            SupabaseClient.getAppointmentService().updateAppointmentStatus(altQueryParam, fallbackMap)
                                    .enqueue(new Callback<Void>() {
                                        @Override
                                        public void onResponse(Call<Void> c2, Response<Void> r2) {
                                            if (!isAdded()) return;
                                            if (r2.isSuccessful()) {
                                                onRescheduleSuccess(dialog, context, sheetBinding);
                                            } else {
                                                Log.e("AppointmentsFragment", "Reschedule update error code: " + r2.code());
                                                onRescheduleFailed(context, sheetBinding, extractErrorMessage(r2, null, "Unable to reschedule appointment. Please try again."));
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<Void> c2, Throwable t2) {
                                            if (!isAdded()) return;
                                            onRescheduleFailed(context, sheetBinding, extractErrorMessage(null, t2, "Unable to reschedule appointment. Please try again."));
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        if (!isAdded()) return;
                        onRescheduleFailed(context, sheetBinding, extractErrorMessage(null, t, "Unable to reschedule appointment. Please try again."));
                    }
                });
    }

    private String extractErrorMessage(Response<?> response, Throwable t, String fallback) {
        if (t != null && t.getMessage() != null && !t.getMessage().trim().isEmpty()) {
            Log.e("AppointmentsFragment", "Reschedule error exception: " + t.getMessage());
        }
        if (response != null) {
            try {
                if (response.errorBody() != null) {
                    String errStr = response.errorBody().string().trim();
                    Log.e("AppointmentsFragment", "Reschedule error response (" + response.code() + "): " + errStr);
                    if (errStr.contains("no longer available") || errStr.contains("booked")) {
                        return "This time slot is no longer available. Please select another slot.";
                    }
                }
            } catch (Exception ignored) {}
        }
        return fallback != null ? fallback : "Unable to reschedule appointment. Please try again.";
    }

    private void finishFallbackReschedule(BottomSheetDialog dialog, Context context, BottomSheetRescheduleAppointmentBinding sheetBinding, Appointment appointment, DoctorSlot newSlot, String slotUuid) {
        // Mark new slot as booked in doctor_slots if applicable
        if (newSlot.getId() != null) {
            Map<String, Object> newSlotMap = new HashMap<>();
            newSlotMap.put("status", "booked");
            newSlotMap.put("appointment_id", appointment.getId());
            SupabaseClient.getSlotService().updateSlotStatus("eq." + slotUuid, newSlotMap).enqueue(new Callback<Void>() {
                @Override public void onResponse(Call<Void> c, Response<Void> r) {}
                @Override public void onFailure(Call<Void> c, Throwable t) {}
            });
        }

        // Release old slot if exists
        String oldSlotId = appointment.getSlotId();
        if (oldSlotId != null && !oldSlotId.trim().isEmpty() && !oldSlotId.equalsIgnoreCase(newSlot.getId())) {
            String oldSlotUuid = formatUuidOrNull(oldSlotId);
            if (oldSlotUuid != null) {
                Map<String, Object> oldSlotMap = new HashMap<>();
                oldSlotMap.put("status", "available");
                oldSlotMap.put("appointment_id", null);
                SupabaseClient.getSlotService().updateSlotStatus("eq." + oldSlotUuid, oldSlotMap).enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> c, Response<Void> r) {}
                    @Override public void onFailure(Call<Void> c, Throwable t) {}
                });
            }
        }

        onRescheduleSuccess(dialog, context, sheetBinding);
    }

    private void onRescheduleSuccess(BottomSheetDialog dialog, Context context, BottomSheetRescheduleAppointmentBinding sheetBinding) {
        isRescheduleSubmitting = false;
        sheetBinding.pbConfirmReschedule.setVisibility(View.GONE);
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        ToastUtils.showSuccess(context, "Appointment rescheduled successfully.");
        invalidateAppointmentsCache();
        DummyDataProvider.invalidateDoctorsCache();
        loadAppointments();
    }

    private void onRescheduleFailed(Context context, BottomSheetRescheduleAppointmentBinding sheetBinding, String message) {
        isRescheduleSubmitting = false;
        sheetBinding.btnConfirmReschedule.setEnabled(true);
        sheetBinding.btnKeepAppointment.setEnabled(true);
        sheetBinding.pbConfirmReschedule.setVisibility(View.GONE);
        ToastUtils.showError(context, message != null ? message : "Unable to reschedule appointment. Please try again.");
    }

    private void openAppointmentDetailsBottomSheet(Appointment appointment) {
        if (!isAdded() || appointment == null) return;
        Context context = getContext();
        if (context == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(context);
        BottomSheetAppointmentDetailsBinding sheetBinding = BottomSheetAppointmentDetailsBinding.inflate(getLayoutInflater());
        dialog.setContentView(sheetBinding.getRoot());

        sheetBinding.tvDetailStatus.setText(appointment.getUserFriendlyStatus());
        sheetBinding.tvDetailDoctorName.setText(appointment.getDoctorName());
        sheetBinding.tvDetailSpecialization.setText(appointment.getSpecialization());
        sheetBinding.tvDetailClinicName.setText(appointment.getClinicName());
        sheetBinding.tvDetailClinicAddress.setText(appointment.getLocation());

        String patName = appointment.getPatientName();
        if (patName == null || patName.trim().isEmpty()) {
            patName = PreferenceManager.getInstance(context).getUserName();
        }
        if (patName == null || patName.trim().isEmpty()) {
            patName = "Patient User";
        }
        sheetBinding.tvDetailPatientName.setText(patName);

        if (appointment.getDoctor() != null && appointment.getDoctor().getImageUrl() != null && !appointment.getDoctor().getImageUrl().isEmpty()) {
            try {
                Glide.with(context)
                        .load(appointment.getDoctor().getImageUrl())
                        .placeholder(R.drawable.ic_user)
                        .into(sheetBinding.ivDetailDoctorImage);
            } catch (Exception ignored) {}
        }

        if (appointment.getDoctor() != null && appointment.getDoctor().getReceptionPhone() != null && !appointment.getDoctor().getReceptionPhone().isEmpty()) {
            sheetBinding.tvDetailReceptionPhone.setText("Reception: " + appointment.getDoctor().getReceptionPhone());
            sheetBinding.tvDetailReceptionPhone.setVisibility(View.VISIBLE);
        } else {
            sheetBinding.tvDetailReceptionPhone.setText("Clinic Branch: " + appointment.getClinicName());
            sheetBinding.tvDetailReceptionPhone.setVisibility(View.VISIBLE);
        }

        String formattedDate = appointment.getDate() != null ? appointment.getDate() : "Scheduled";
        String formattedTime = appointment.getFormattedTime();
        String lowerSt = appointment.getStatus() != null ? appointment.getStatus().toLowerCase(Locale.US) : "";
        boolean isCancelled = lowerSt.equals("cancelled") || lowerSt.equals("rejected") || lowerSt.equals("no_show") || lowerSt.contains("cancel");

        if (isCancelled) {
            sheetBinding.tvDetailDatetime.setText(formattedDate + " • " + formattedTime + " (canceled)");
            sheetBinding.tvDetailDatetime.setTextColor(ContextCompat.getColor(context, R.color.error_red));
        } else {
            sheetBinding.tvDetailDatetime.setText(formattedDate + " • " + formattedTime);
            sheetBinding.tvDetailDatetime.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
        }

        String apptTypeStr = "In-Clinic";
        if (appointment.getAppointmentType() != null && !appointment.getAppointmentType().isEmpty()) {
            String typeLower = appointment.getAppointmentType().toLowerCase();
            if (typeLower.contains("video")) apptTypeStr = "Video Consult";
            else if (typeLower.contains("follow")) apptTypeStr = "Follow-up";
            else apptTypeStr = "In-Clinic";
        }
        int feeVal = appointment.getAmount() > 0 ? appointment.getAmount() : (appointment.getFee() > 0 ? appointment.getFee() : 900);
        sheetBinding.tvDetailTypeFee.setText(apptTypeStr + " • ₹" + feeVal);

        int yourNum = appointment.getSlotNumber() > 0 ? appointment.getSlotNumber() : 1;
        sheetBinding.tvQueueTokenNum.setText("Your Number: #" + String.format(Locale.getDefault(), "%02d", yourNum));

        if (appointment.getPatientReason() != null && !appointment.getPatientReason().trim().isEmpty()) {
            sheetBinding.cardPatientReason.setVisibility(View.VISIBLE);
            sheetBinding.tvDetailReason.setText(appointment.getPatientReason());
        } else {
            sheetBinding.cardPatientReason.setVisibility(View.GONE);
        }

        if (appointment.getPrescription() != null && !appointment.getPrescription().trim().isEmpty()) {
            sheetBinding.cardPrescriptionDetail.setVisibility(View.VISIBLE);
            sheetBinding.tvDetailPrescriptionText.setText(appointment.getPrescription());
        } else {
            sheetBinding.cardPrescriptionDetail.setVisibility(View.GONE);
        }

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
            String st = appt.getStatus() != null ? appt.getStatus().trim() : "";
            String lowerSt = st.toLowerCase(Locale.US);

            if (!"completed".equals(lowerSt) && !"cancelled".equals(lowerSt) &&
                !"rejected".equals(lowerSt) && !"no_show".equals(lowerSt)) {

                if (AvailabilityHelper.isAppointmentSchedulePassed(appt, 20)) {
                    String cancelReason = "Appointment cancelled automatically because doctor consultation was not completed before the appointment schedule ended.";
                    appt.setStatus("Cancelled");
                    appt.setPatientReason(cancelReason);

                    Map<String, Object> updateMap = new HashMap<>();
                    updateMap.put("status", "Cancelled");
                    updateMap.put("patient_reason", cancelReason);
                    updateMap.put("notes", cancelReason);

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

        if (!AvailabilityHelper.isRefundEligible(appointment)) {
            ToastUtils.showWarning(context, "Refund is not available for this appointment because it was cancelled less than 1 hour before schedule.");
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(context);
        DialogRefundDetailsBinding refundBinding = DialogRefundDetailsBinding.inflate(getLayoutInflater());
        dialog.setContentView(refundBinding.getRoot());

        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
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

        // Disable button & inputs while checking existing refund request status
        refundBinding.btnRefundAction.setEnabled(false);
        refundBinding.btnRefundAction.setText("Checking refund status...");
        refundBinding.etPatientUpi.setEnabled(false);
        refundBinding.etRefundReason.setEnabled(false);

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
                                    }
                                    refundBinding.etPatientUpi.setEnabled(false);
                                    refundBinding.etRefundReason.setEnabled(false);

                                    String status = existing.getStatus() != null ? existing.getStatus().toLowerCase(Locale.US) : "pending";

                                    if ("approved".equals(status) || "refunded".equals(status)) {
                                        refundBinding.tvRefundStatusTitle.setText("Refund Approved & Processed");
                                        refundBinding.tvRefundStatusSub.setText("₹" + fee + " has been approved by admin and credited to your payment source.");
                                        refundBinding.btnRefundAction.setText("✓ Refund Processed");
                                        refundBinding.btnRefundAction.setEnabled(false);
                                    } else if ("rejected".equals(status) || "failed".equals(status)) {
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
                                    return;
                                }
                            }
                            // No existing refund request found
                            hasExistingRefund[0] = false;
                            refundBinding.btnRefundAction.setEnabled(true);
                            refundBinding.btnRefundAction.setText("Request Cancellation & Refund");
                            refundBinding.etPatientUpi.setEnabled(true);
                            refundBinding.etRefundReason.setEnabled(true);
                        }

                        @Override
                        public void onFailure(Call<List<RefundRequest>> call, Throwable t) {
                            if (!isAdded() || getContext() == null) return;
                            hasExistingRefund[0] = false;
                            refundBinding.btnRefundAction.setEnabled(true);
                            refundBinding.btnRefundAction.setText("Request Cancellation & Refund");
                            refundBinding.etPatientUpi.setEnabled(true);
                            refundBinding.etRefundReason.setEnabled(true);
                        }
                    });
        } else {
            refundBinding.btnRefundAction.setEnabled(true);
            refundBinding.btnRefundAction.setText("Request Cancellation & Refund");
            refundBinding.etPatientUpi.setEnabled(true);
            refundBinding.etRefundReason.setEnabled(true);
        }

        String safePatientId = resolvedPatientId != null && !resolvedPatientId.trim().isEmpty() ? resolvedPatientId.trim() : "patient_anon";
        final String finalPatientId = safePatientId;

        refundBinding.btnRefundAction.setOnClickListener(v -> {
            if (hasExistingRefund[0]) {
                ToastUtils.showWarning(context, "Refund request has already been submitted for this appointment.");
                refundBinding.btnRefundAction.setEnabled(false);
                return;
            }

            String upiText = refundBinding.etPatientUpi.getText() != null
                    ? refundBinding.etPatientUpi.getText().toString().trim() : "";
            String reasonText = refundBinding.etRefundReason.getText() != null
                    ? refundBinding.etRefundReason.getText().toString().trim() : "";

            if (upiText.isEmpty()) {
                ToastUtils.showWarning(context, "Please enter valid UPI ID for refund");
                return;
            }

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
            refundBinding.etPatientUpi.setEnabled(false);
            refundBinding.etRefundReason.setEnabled(false);

            Callback<List<Map<String, Object>>> refundCallback = new Callback<List<Map<String, Object>>>() {
                @Override
                public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                    Context ctx = getContext();
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        hasExistingRefund[0] = true;
                        if (isAdded() && ctx != null) {
                            ToastUtils.showSuccess(ctx, "Refund request submitted successfully!");
                            refundBinding.tvRefundStatusTitle.setText("Refund Request Pending Approval");
                            refundBinding.tvRefundStatusSub.setText("Request submitted to admin. Status: Under Review.");
                            refundBinding.btnRefundAction.setText("Status: Pending Admin Review");
                            refundBinding.btnRefundAction.setEnabled(false);
                            dialog.dismiss();
                        }
                    } else {
                        String errStr = extractResponseError(response);
                        Log.e("AppointmentsFragment", "Refund submission failed: " + errStr);
                        resetRefundButton(refundBinding, hasExistingRefund[0], errStr);
                    }
                }

                @Override
                public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                    Log.e("AppointmentsFragment", "Refund submission error: " + t.getMessage(), t);
                    resetRefundButton(refundBinding, hasExistingRefund[0], "Network Error: " + t.getMessage());
                }
            };

            Map<String, Object> updateApptMap = new HashMap<>();
            updateApptMap.put("payment_status", "Refund Requested");
            SupabaseClient.getAppointmentService().updateAppointmentStatus("eq." + appointment.getId(), updateApptMap)
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {}
                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {}
                    });

            SupabaseClient.getAppointmentService().postRefundRequestPayload("appointment_id", "resolution=merge-duplicates,return=representation", refundMap)
                    .enqueue(refundCallback);
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

    private void resetRefundButton(DialogRefundDetailsBinding refundBinding, boolean hasExistingRefund, String message) {
        Context context = getContext();
        if (!isAdded() || context == null || refundBinding == null) return;
        if (hasExistingRefund) {
            refundBinding.btnRefundAction.setEnabled(false);
            refundBinding.btnRefundAction.setText("Refund Request Closed");
            refundBinding.etPatientUpi.setEnabled(false);
            refundBinding.etRefundReason.setEnabled(false);
        } else {
            refundBinding.btnRefundAction.setEnabled(true);
            refundBinding.btnRefundAction.setText("Request Cancellation & Refund");
            refundBinding.etPatientUpi.setEnabled(true);
            refundBinding.etRefundReason.setEnabled(true);
        }
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
            try {
                Glide.with(context)
                        .load(appointment.getDoctor().getImageUrl())
                        .placeholder(R.drawable.ic_user)
                        .into(rateBinding.ivRateDoctorAvatar);
            } catch (Exception ignored) {}
        }

        final boolean[] hasExistingReview = {false};

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
        if (binding != null) {
            if (binding.shimmerAppointments != null) {
                try {
                    binding.shimmerAppointments.stopShimmer();
                } catch (Exception ignored) {}
            }
        }
        super.onDestroyView();
        binding = null;
    }
}
