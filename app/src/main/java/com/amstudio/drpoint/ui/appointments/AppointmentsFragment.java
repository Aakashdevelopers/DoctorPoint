package com.amstudio.drpoint.ui.appointments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

        binding.rvAppointments.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AppointmentListAdapter(new AppointmentListAdapter.OnAppointmentActionListener() {
            @Override
            public void onItemClick(Appointment appointment) {
                openAppointmentDetailsBottomSheet(appointment);
            }

            @Override
            public void onRescheduleClick(Appointment appointment) {
                if (!appointment.isCancellable()) {
                    Toast.makeText(requireContext(), "This appointment cannot be rescheduled.", Toast.LENGTH_SHORT).show();
                    return;
                }
                openRescheduleFlow(appointment);
            }

            @Override
            public void onCancelClick(Appointment appointment) {
                attemptCancelAppointment(appointment);
            }

            @Override
            public void onRateDoctorClick(Appointment appointment) {
                openRateDoctorBottomSheet(appointment);
            }

            @Override
            public void onRefundClick(Appointment appointment) {
                openRefundDetailsBottomSheet(appointment);
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

        PreferenceManager prefManager = PreferenceManager.getInstance(requireContext());
        binding.swReminders.setChecked(prefManager.getRemindersEnabled());
        binding.swReminders.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefManager.setRemindersEnabled(isChecked);
            String msg = isChecked ? "Reminders enabled" : "Reminders disabled";
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
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

    private void loadAppointments() {
        if (!isAdded()) return;
        String patientId = PreferenceManager.getInstance(requireContext()).getUserId();

        // 1. Fetch real appointments from Supabase appointments table
        SupabaseClient.getAppointmentService().getAllAppointments()
                .enqueue(new Callback<List<Appointment>>() {
                    @Override
                    public void onResponse(Call<List<Appointment>> call, Response<List<Appointment>> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            fetchedAppointments = response.body();
                        } else {
                            fetchUserAppointmentsFallback(patientId);
                            return;
                        }
                        processAutoCancellationForPastAppointments(fetchedAppointments);
                        filterAppointments(currentTabPosition);
                    }

                    @Override
                    public void onFailure(Call<List<Appointment>> call, Throwable t) {
                        if (!isAdded()) return;
                        fetchUserAppointmentsFallback(patientId);
                    }
                });
    }

    private void fetchUserAppointmentsFallback(String patientId) {
        if (patientId != null && !patientId.trim().isEmpty()) {
            SupabaseClient.getAppointmentService().getAppointmentsForPatient("eq." + patientId)
                    .enqueue(new Callback<List<Appointment>>() {
                        @Override
                        public void onResponse(Call<List<Appointment>> call, Response<List<Appointment>> response) {
                            if (!isAdded()) return;
                            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                                fetchedAppointments = response.body();
                            } else {
                                fetchedAppointments = new ArrayList<>();
                            }
                            processAutoCancellationForPastAppointments(fetchedAppointments);
                            filterAppointments(currentTabPosition);
                        }

                        @Override
                        public void onFailure(Call<List<Appointment>> call, Throwable t) {
                            if (!isAdded()) return;
                            fetchedAppointments = new ArrayList<>();
                            filterAppointments(currentTabPosition);
                        }
                    });
        } else {
            fetchedAppointments = new ArrayList<>();
            filterAppointments(currentTabPosition);
        }
    }

    private void filterAppointments(int tabPosition) {
        List<Appointment> allAppointments = fetchedAppointments;
        List<Appointment> displayed = new ArrayList<>();

        PreferenceManager prefManager = PreferenceManager.getInstance(requireContext());
        String currentUserId = prefManager.getUserId();
        String currentUserName = prefManager.getUserName();

        for (Appointment appt : allAppointments) {
            if (appt == null) continue;

            String pId = appt.getPatientId() != null ? appt.getPatientId().trim() : "";
            String pName = appt.getPatientName() != null ? appt.getPatientName().trim() : "";

            if (currentUserId != null && !currentUserId.trim().isEmpty() && !pId.isEmpty()) {
                if (!pId.equalsIgnoreCase(currentUserId.trim()) && (currentUserName == null || currentUserName.trim().isEmpty() || !pName.equalsIgnoreCase(currentUserName.trim()))) {
                    continue;
                }
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

        if (binding != null && binding.shimmerAppointments != null) {
            binding.shimmerAppointments.stopShimmer();
            binding.shimmerAppointments.setVisibility(View.GONE);
        }

        if (displayed.isEmpty()) {
            binding.llEmptyState.setVisibility(View.VISIBLE);
            binding.rvAppointments.setVisibility(View.GONE);
        } else {
            binding.llEmptyState.setVisibility(View.GONE);
            binding.rvAppointments.setVisibility(View.VISIBLE);
        }

        adapter.submitList(displayed);
    }

    private void attemptCancelAppointment(Appointment appointment) {
        if (!appointment.isCancellable()) {
            Toast.makeText(requireContext(), "This appointment cannot be cancelled.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Cancel Appointment")
                .setMessage("Are you sure you want to cancel your appointment with " + appointment.getDoctorName() + "?")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> cancelAppointmentInSupabase(appointment))
                .setNegativeButton("No", null)
                .show();
    }

    private void cancelAppointmentInSupabase(Appointment appointment) {
        appointment.setStatus("Cancelled");

        String patientId = PreferenceManager.getInstance(requireContext()).getUserId();
        CancelAppointmentRpcRequest cancelRequest = new CancelAppointmentRpcRequest(appointment.getId(), patientId, "Cancelled by patient");

        SupabaseClient.getSlotService().cancelAppointment(cancelRequest).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                Toast.makeText(requireContext(), "Appointment cancelled successfully.", Toast.LENGTH_SHORT).show();
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
                                Toast.makeText(requireContext(), "Appointment cancelled successfully.", Toast.LENGTH_SHORT).show();
                                loadAppointments();
                                openRefundDetailsBottomSheet(appointment);
                            }

                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                Toast.makeText(requireContext(), "Appointment marked cancelled.", Toast.LENGTH_SHORT).show();
                                loadAppointments();
                                openRefundDetailsBottomSheet(appointment);
                            }
                        });
            }
        });
    }

    private void openRescheduleFlow(Appointment appointment) {
        Doctor doc = new Doctor();
        doc.setId(appointment.getDoctorId() != null ? appointment.getDoctorId() : "doc_1");
        doc.setName(appointment.getDoctorName());
        doc.setClinicName(appointment.getClinicName());
        doc.setLocation(appointment.getLocation());
        doc.setQualification(appointment.getSpecialization());

        Intent intent = new Intent(requireContext(), BookAppointmentActivity.class);
        intent.putExtra("doctor", doc);
        startActivity(intent);
    }

    private void openAppointmentDetailsBottomSheet(Appointment appointment) {
        if (getContext() == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        BottomSheetAppointmentDetailsBinding sheetBinding = BottomSheetAppointmentDetailsBinding.inflate(getLayoutInflater());
        dialog.setContentView(sheetBinding.getRoot());

        // Header info & Doctor Avatar
        sheetBinding.tvDetailStatus.setText(appointment.getUserFriendlyStatus());
        sheetBinding.tvDetailDoctorName.setText(appointment.getDoctorName());
        sheetBinding.tvDetailSpecialization.setText(appointment.getSpecialization());
        sheetBinding.tvDetailClinicName.setText(appointment.getClinicName());
        sheetBinding.tvDetailClinicAddress.setText(appointment.getLocation());

        if (appointment.getDoctor() != null && appointment.getDoctor().getImageUrl() != null && !appointment.getDoctor().getImageUrl().isEmpty()) {
            Glide.with(this)
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
        if (getContext() == null || appointment == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        DialogRefundDetailsBinding refundBinding = DialogRefundDetailsBinding.inflate(getLayoutInflater());
        dialog.setContentView(refundBinding.getRoot());

        int fee = appointment.getAmount() > 0 ? appointment.getAmount() : (appointment.getFee() > 0 ? appointment.getFee() : 500);

        refundBinding.tvRefundPaidAmount.setText("₹" + fee);
        refundBinding.tvTotalRefundAmount.setText("₹" + fee);

        String refId = "REF-" + Math.abs((appointment.getId() != null ? appointment.getId() : "123").hashCode() % 899999 + 100000);
        refundBinding.tvRefundReferenceId.setText("Refund Reference ID: " + refId);

        refundBinding.ivCloseRefundDialog.setOnClickListener(v -> dialog.dismiss());

        PreferenceManager prefManager = PreferenceManager.getInstance(requireContext());
        String patientId = prefManager.getUserId() != null ? prefManager.getUserId() : "patient_anon";
        String patientName = prefManager.getUserName() != null ? prefManager.getUserName() : "Verified Patient";

        // Check if a refund request already exists for this appointment
        if (appointment.getId() != null) {
            SupabaseClient.getAppointmentService().getRefundRequestByAppointment("eq." + appointment.getId())
                    .enqueue(new Callback<List<RefundRequest>>() {
                        @Override
                        public void onResponse(Call<List<RefundRequest>> call, Response<List<RefundRequest>> response) {
                            if (!isAdded()) return;
                            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                                RefundRequest existing = response.body().get(0);
                                if (existing != null) {
                                    refundBinding.tilRefundReason.setVisibility(View.GONE);
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

        refundBinding.btnRefundAction.setOnClickListener(v -> {
            String reasonText = refundBinding.etRefundReason.getText() != null
                    ? refundBinding.etRefundReason.getText().toString().trim() : "";

            RefundRequest refundReq = new RefundRequest(
                    appointment.getId(),
                    patientId,
                    patientName,
                    "+91 9876543210",
                    appointment.getDoctorId(),
                    appointment.getDoctorName(),
                    appointment.getSpecialization(),
                    appointment.getClinicName(),
                    appointment.getDate(),
                    fee,
                    reasonText.isEmpty() ? "Patient requested cancellation refund" : reasonText
            );

            refundBinding.btnRefundAction.setEnabled(false);
            refundBinding.btnRefundAction.setText("Submitting Request...");

            SupabaseClient.getAppointmentService().postRefundRequest("return=minimal", refundReq)
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            Toast.makeText(requireContext(), "Refund request submitted! Admin will process it within 24-48 hours.", Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(requireContext(), "Refund request submitted successfully!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        }
                    });
        });

        dialog.show();
    }

    private void openRateDoctorBottomSheet(Appointment appointment) {
        if (getContext() == null || appointment == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        DialogRateDoctorBinding rateBinding = DialogRateDoctorBinding.inflate(getLayoutInflater());
        dialog.setContentView(rateBinding.getRoot());

        rateBinding.tvRateDoctorName.setText(appointment.getDoctorName() != null ? appointment.getDoctorName() : "Doctor");
        rateBinding.tvRateSpecialization.setText(appointment.getSpecialization() != null ? appointment.getSpecialization() : "Consultant");

        String dateStr = appointment.getDate() != null ? appointment.getDate() : "Completed Consultation";
        rateBinding.tvRateApptDate.setText("Completed • " + dateStr);

        if (appointment.getDoctor() != null && appointment.getDoctor().getImageUrl() != null && !appointment.getDoctor().getImageUrl().isEmpty()) {
            Glide.with(this)
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
                            if (!isAdded()) return;
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

            PreferenceManager prefManager = PreferenceManager.getInstance(requireContext());
            String patientId = prefManager.getUserId() != null ? prefManager.getUserId() : "patient_anon";
            String patientName = prefManager.getUserName() != null ? prefManager.getUserName() : "Verified Patient";
            String patientAvatar = prefManager.getUserAvatar() != null ? prefManager.getUserAvatar() : "";

            Map<String, Object> reviewMap = new HashMap<>();
            reviewMap.put("appointment_id", appointment.getId());
            reviewMap.put("doctor_id", appointment.getDoctorId());
            reviewMap.put("patient_id", patientId);
            reviewMap.put("patient_name", patientName);
            reviewMap.put("patient_avatar", patientAvatar);
            reviewMap.put("rating", selectedRating);
            reviewMap.put("review_text", reviewComment);

            rateBinding.btnSubmitReview.setEnabled(false);
            rateBinding.btnSubmitReview.setText("Submitting...");

            Callback<Void> reviewCallback = new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    dialog.dismiss();
                    Toast.makeText(requireContext(), "Thank you for reviewing Dr. " + appointment.getDoctorName() + "!", Toast.LENGTH_LONG).show();

                    if (appointment.getDoctorId() != null) {
                        DummyDataProvider.recalculateAndUpdateDoctorRating(appointment.getDoctorId());
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    dialog.dismiss();
                    Toast.makeText(requireContext(), "Thank you for reviewing Dr. " + appointment.getDoctorName() + "!", Toast.LENGTH_SHORT).show();

                    if (appointment.getDoctorId() != null) {
                        DummyDataProvider.recalculateAndUpdateDoctorRating(appointment.getDoctorId());
                    }
                }
            };

            if (hasExistingReview[0]) {
                SupabaseClient.getDoctorService().updateDoctorReview("eq." + appointment.getId(), "return=minimal", reviewMap)
                        .enqueue(reviewCallback);
            } else {
                SupabaseClient.getDoctorService().postDoctorReviewPayload("return=minimal", reviewMap)
                        .enqueue(reviewCallback);
            }
        });

        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
