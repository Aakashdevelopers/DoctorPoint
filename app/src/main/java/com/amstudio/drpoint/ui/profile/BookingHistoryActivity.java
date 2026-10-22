package com.amstudio.drpoint.ui.profile;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.amstudio.drpoint.R;
import com.amstudio.drpoint.adapter.AppointmentListAdapter;
import com.amstudio.drpoint.databinding.ActivityBookingHistoryBinding;
import com.amstudio.drpoint.databinding.BottomSheetAppointmentDetailsBinding;
import com.amstudio.drpoint.databinding.DialogRateDoctorBinding;
import com.amstudio.drpoint.model.Appointment;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.model.DoctorReview;
import com.amstudio.drpoint.network.SupabaseClient;
import com.amstudio.drpoint.util.DummyDataProvider;
import com.amstudio.drpoint.util.PreferenceManager;
import com.amstudio.drpoint.util.ToastUtils;
import com.amstudio.drpoint.util.WindowInsetsUtil;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

public class BookingHistoryActivity extends AppCompatActivity {

    private static final String TAG = "BookingHistoryActivity";

    private ActivityBookingHistoryBinding binding;
    private AppointmentListAdapter adapter;
    private final List<Appointment> completedAppointmentsList = new ArrayList<>();
    private final Map<String, Doctor> doctorCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookingHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowInsetsUtil.applySystemBarInsets(binding.getRoot());

        binding.flBack.setOnClickListener(v -> finish());

        binding.rvBookingHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppointmentListAdapter(new AppointmentListAdapter.OnAppointmentActionListener() {
            @Override
            public void onItemClick(Appointment appointment) {
                if (appointment != null) {
                    openAppointmentDetailsBottomSheet(appointment);
                }
            }

            @Override
            public void onRescheduleClick(Appointment appointment) {}

            @Override
            public void onCancelClick(Appointment appointment) {}

            @Override
            public void onRateDoctorClick(Appointment appointment) {
                if (appointment != null) {
                    openRateDoctorBottomSheet(appointment);
                }
            }

            @Override
            public void onRefundClick(Appointment appointment) {}
        });
        binding.rvBookingHistory.setAdapter(adapter);

        binding.swipeRefreshLayout.setOnRefreshListener(this::loadBookingHistory);
        binding.btnRetry.setOnClickListener(v -> loadBookingHistory());

        loadBookingHistory();
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

    private boolean belongsToCurrentUser(String apptPatientId) {
        if (apptPatientId == null || apptPatientId.trim().isEmpty()) return false;
        PreferenceManager prefManager = PreferenceManager.getInstance(this);
        String currentUserId = prefManager.getUserId();
        if (currentUserId == null || currentUserId.trim().isEmpty()) return false;

        String cleanCurrent = currentUserId.trim();
        String cleanAppt = apptPatientId.trim();

        if (cleanCurrent.equalsIgnoreCase(cleanAppt)) return true;

        String uuidCurrent = formatUuidOrNull(cleanCurrent);
        String uuidAppt = formatUuidOrNull(cleanAppt);

        if (uuidCurrent != null && uuidCurrent.equalsIgnoreCase(cleanAppt)) return true;
        if (uuidAppt != null && uuidAppt.equalsIgnoreCase(cleanCurrent)) return true;
        if (uuidCurrent != null && uuidCurrent.equalsIgnoreCase(uuidAppt)) return true;

        return false;
    }

    private void loadBookingHistory() {
        PreferenceManager prefManager = PreferenceManager.getInstance(this);

        if (!prefManager.isLoggedIn()) {
            binding.swipeRefreshLayout.setRefreshing(false);
            showEmptyState("Not Signed In", "Please sign in to view your completed booking history.");
            return;
        }

        String userId = prefManager.getUserId();
        if (userId == null || userId.trim().isEmpty()) {
            binding.swipeRefreshLayout.setRefreshing(false);
            showEmptyState("User Not Found", "Unable to load booking history for this user.");
            return;
        }

        String cleanUserId = userId.trim();
        String uuidUserId = formatUuidOrNull(cleanUserId);

        String queryParam;
        if (uuidUserId != null && !uuidUserId.equalsIgnoreCase(cleanUserId)) {
            queryParam = "in.(" + cleanUserId + "," + uuidUserId + ")";
        } else {
            queryParam = "eq." + cleanUserId;
        }

        showLoadingState();

        SupabaseClient.getAppointmentService().getAppointmentsForPatient(queryParam)
                .enqueue(new Callback<List<Appointment>>() {
                    @Override
                    public void onResponse(Call<List<Appointment>> call, Response<List<Appointment>> response) {
                        if (isFinishing() || isDestroyed()) return;

                        List<Appointment> rawList = (response.isSuccessful() && response.body() != null)
                                ? response.body() : new ArrayList<>();

                        enrichAppointmentsWithDoctorData(rawList, () -> {
                            if (isFinishing() || isDestroyed()) return;

                            completedAppointmentsList.clear();
                            for (Appointment appt : rawList) {
                                if (appt == null) continue;
                                if (!belongsToCurrentUser(appt.getPatientId())) continue;

                                String st = appt.getStatus() != null ? appt.getStatus().toLowerCase(Locale.US) : "";
                                if ("completed".equals(st)) {
                                    completedAppointmentsList.add(appt);
                                }
                            }

                            hideLoadingState();

                            if (completedAppointmentsList.isEmpty()) {
                                showEmptyState("No Completed Appointments", "You haven't completed any doctor consultations yet.");
                            } else {
                                showDataState(completedAppointmentsList);
                            }
                        });
                    }

                    @Override
                    public void onFailure(Call<List<Appointment>> call, Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        hideLoadingState();
                        showEmptyState("Network Error", "Failed to load booking history. " + t.getMessage());
                    }
                });
    }

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

        int[] remaining = {missingDocIds.size()};
        for (String dId : missingDocIds) {
            SupabaseClient.getDoctorService().getDoctorById("eq." + dId)
                    .enqueue(new Callback<List<Doctor>>() {
                        @Override
                        public void onResponse(Call<List<Doctor>> call, Response<List<Doctor>> response) {
                            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                                Doctor d = response.body().get(0);
                                if (d != null && d.getId() != null) {
                                    doctorCache.put(d.getId().trim(), d);
                                    for (Appointment a : list) {
                                        if (a != null && dId.equalsIgnoreCase(a.getDoctorId())) {
                                            a.setDoctor(d);
                                        }
                                    }
                                }
                            }
                            remaining[0]--;
                            if (remaining[0] <= 0 && onComplete != null) {
                                onComplete.run();
                            }
                        }

                        @Override
                        public void onFailure(Call<List<Doctor>> call, Throwable t) {
                            remaining[0]--;
                            if (remaining[0] <= 0 && onComplete != null) {
                                onComplete.run();
                            }
                        }
                    });
        }
    }

    private void showLoadingState() {
        binding.shimmerBookingHistory.startShimmer();
        binding.shimmerBookingHistory.setVisibility(View.VISIBLE);
        binding.rvBookingHistory.setVisibility(View.GONE);
        binding.llEmptyState.setVisibility(View.GONE);
    }

    private void hideLoadingState() {
        binding.shimmerBookingHistory.stopShimmer();
        binding.shimmerBookingHistory.setVisibility(View.GONE);
        binding.swipeRefreshLayout.setRefreshing(false);
    }

    private void showDataState(List<Appointment> list) {
        binding.llEmptyState.setVisibility(View.GONE);
        binding.rvBookingHistory.setVisibility(View.VISIBLE);
        adapter.submitList(new ArrayList<>(list));
    }

    private void showEmptyState(String title, String subtitle) {
        binding.rvBookingHistory.setVisibility(View.GONE);
        binding.llEmptyState.setVisibility(View.VISIBLE);
        binding.tvEmptyTitle.setText(title);
        binding.tvEmptySubtitle.setText(subtitle);
        adapter.submitList(new ArrayList<>());
    }

    private void openAppointmentDetailsBottomSheet(Appointment appointment) {
        if (appointment == null || isFinishing() || isDestroyed()) return;

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        BottomSheetAppointmentDetailsBinding sheetBinding = BottomSheetAppointmentDetailsBinding.inflate(getLayoutInflater());
        dialog.setContentView(sheetBinding.getRoot());

        sheetBinding.tvDetailStatus.setText(appointment.getUserFriendlyStatus());
        sheetBinding.tvDetailDoctorName.setText(appointment.getDoctorName());
        sheetBinding.tvDetailSpecialization.setText(appointment.getSpecialization());
        sheetBinding.tvDetailClinicName.setText(appointment.getClinicName());
        sheetBinding.tvDetailClinicAddress.setText(appointment.getLocation());

        String patName = appointment.getPatientName();
        if (patName == null || patName.trim().isEmpty()) {
            patName = PreferenceManager.getInstance(this).getUserName();
        }
        if (patName == null || patName.trim().isEmpty()) {
            patName = "Patient User";
        }
        sheetBinding.tvDetailPatientName.setText(patName);

        if (appointment.getDoctor() != null && appointment.getDoctor().getImageUrl() != null && !appointment.getDoctor().getImageUrl().isEmpty()) {
            try {
                Glide.with(this)
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

        String dtStr = (appointment.getDate() != null ? appointment.getDate() : "N/A") + " • " + appointment.getFormattedTime();
        String st = appointment.getStatus() != null ? appointment.getStatus().toLowerCase(Locale.US) : "";
        boolean isCancelled = st.equals("cancelled") || st.equals("rejected") || st.equals("no_show") || st.contains("cancel");

        if (isCancelled) {
            sheetBinding.tvDetailDatetime.setText(dtStr + " (canceled)");
            sheetBinding.tvDetailDatetime.setTextColor(ContextCompat.getColor(this, R.color.error_red));
        } else {
            sheetBinding.tvDetailDatetime.setText(dtStr);
            sheetBinding.tvDetailDatetime.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        }

        dialog.show();
    }

    private void openRateDoctorBottomSheet(Appointment appointment) {
        if (appointment == null || isFinishing() || isDestroyed()) return;

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        DialogRateDoctorBinding rateBinding = DialogRateDoctorBinding.inflate(getLayoutInflater());
        dialog.setContentView(rateBinding.getRoot());

        rateBinding.tvRateDoctorName.setText(appointment.getDoctorName() != null ? appointment.getDoctorName() : "Doctor");
        rateBinding.tvRateSpecialization.setText(appointment.getSpecialization() != null ? appointment.getSpecialization() : "Consultant");

        String dateStr = appointment.getDate() != null ? appointment.getDate() : "Completed Consultation";
        rateBinding.tvRateApptDate.setText("Completed • " + dateStr);

        if (appointment.getDoctor() != null && appointment.getDoctor().getImageUrl() != null && !appointment.getDoctor().getImageUrl().isEmpty()) {
            try {
                Glide.with(this)
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
                            if (isFinishing() || isDestroyed()) return;
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

            PreferenceManager prefManager = PreferenceManager.getInstance(BookingHistoryActivity.this);

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

            SupabaseClient.getDoctorService().postDoctorReviewPayload("appointment_id", "resolution=merge-duplicates,return=representation", reviewMap)
                    .enqueue(new Callback<List<Map<String, Object>>>() {
                        @Override
                        public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                            if (isFinishing() || isDestroyed()) return;
                            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                                dialog.dismiss();
                                ToastUtils.showSuccess(BookingHistoryActivity.this, "Thank you! Your review has been submitted.");
                                if (appointment.getDoctorId() != null) {
                                    DummyDataProvider.recalculateAndUpdateDoctorRating(appointment.getDoctorId());
                                }
                            } else {
                                rateBinding.btnSubmitReview.setEnabled(true);
                                rateBinding.btnSubmitReview.setText(hasExistingReview[0] ? "Update Review" : "Submit Review");
                                ToastUtils.showError(BookingHistoryActivity.this, "Failed to submit review.");
                            }
                        }

                        @Override
                        public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                            if (isFinishing() || isDestroyed()) return;
                            rateBinding.btnSubmitReview.setEnabled(true);
                            rateBinding.btnSubmitReview.setText(hasExistingReview[0] ? "Update Review" : "Submit Review");
                            ToastUtils.showError(BookingHistoryActivity.this, "Network error: " + t.getMessage());
                        }
                    });
        });

        dialog.show();
    }
}
