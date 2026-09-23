package com.amstudio.drpoint.util;

import android.util.Log;

import com.amstudio.drpoint.model.CommissionSettings;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.network.SupabaseClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommissionHelper {

    private static final String TAG = "CommissionHelper";

    public interface CommissionCallback {
        void onCompleted(double doctorEarning, double commissionDeducted);
    }

    /**
     * Calculates doctor earnings after deducting platform commission.
     * Hierarchy:
     * 1. Uses Doctor-specific commission if defined on Doctor object in `doctors` table.
     * 2. Otherwise falls back to Global Commission Settings in `commission_settings` table.
     */
    public static void processBookingEarningsAndCommission(String doctorId, double totalFee, boolean isFollowUp, CommissionCallback callback) {
        if (doctorId == null || doctorId.trim().isEmpty()) {
            if (callback != null) callback.onCompleted(totalFee, 0);
            return;
        }

        String docIdQuery = doctorId.startsWith("eq.") ? doctorId : "eq." + doctorId;

        // Step 1: Fetch Doctor info to check for custom commission override
        SupabaseClient.getDoctorService().getDoctorById(docIdQuery).enqueue(new Callback<List<Doctor>>() {
            @Override
            public void onResponse(Call<List<Doctor>> call, Response<List<Doctor>> response) {
                Doctor doctor = null;
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    doctor = response.body().get(0);
                }

                if (doctor != null && doctor.hasCustomCommission()) {
                    // Use Doctor-specific Commission
                    double commVal = isFollowUp
                            ? (doctor.getFollowupCommission() != null ? doctor.getFollowupCommission() : 0)
                            : (doctor.getNewPatientCommission() != null ? doctor.getNewPatientCommission() : 0);

                    String commType = doctor.getCommissionType() != null ? doctor.getCommissionType().toLowerCase() : "percentage";

                    double commissionDeducted;
                    if (commType.contains("percent") || commType.contains("%")) {
                        commissionDeducted = (totalFee * commVal) / 100.0;
                    } else {
                        commissionDeducted = commVal;
                    }

                    if (commissionDeducted < 0) commissionDeducted = 0;
                    if (commissionDeducted > totalFee) commissionDeducted = totalFee;

                    double doctorNetEarning = Math.max(0, totalFee - commissionDeducted);

                    Log.d(TAG, "Doctor-Specific Commission Applied! Doctor: " + doctor.getName() + ", Fee: " + totalFee + ", Comm: " + commissionDeducted + ", Net: " + doctorNetEarning);

                    updateDoctorEarningsInSupabase(docIdQuery, doctor, doctorNetEarning, commissionDeducted, callback);
                } else {
                    // Fall back to Global Commission Settings
                    final Doctor docObj = doctor;
                    fetchGlobalCommissionAndUpdate(docIdQuery, docObj, totalFee, isFollowUp, callback);
                }
            }

            @Override
            public void onFailure(Call<List<Doctor>> call, Throwable t) {
                Log.e(TAG, "Failed to fetch doctor by ID, falling back to global commission: " + t.getMessage());
                fetchGlobalCommissionAndUpdate(docIdQuery, null, totalFee, isFollowUp, callback);
            }
        });
    }

    private static void fetchGlobalCommissionAndUpdate(String docIdQuery, Doctor doctorObj, double totalFee, boolean isFollowUp, CommissionCallback callback) {
        SupabaseClient.getDoctorService().getCommissionSettings().enqueue(new Callback<List<CommissionSettings>>() {
            @Override
            public void onResponse(Call<List<CommissionSettings>> call, Response<List<CommissionSettings>> response) {
                CommissionSettings settings = new CommissionSettings();
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    settings = response.body().get(0);
                }

                double commissionDeducted = settings.calculateCommission(totalFee, isFollowUp);
                double doctorNetEarning = settings.calculateDoctorEarning(totalFee, isFollowUp);

                Log.d(TAG, "Global Commission Applied! Fee: " + totalFee + ", Comm: " + commissionDeducted + ", Net: " + doctorNetEarning);

                updateDoctorEarningsInSupabase(docIdQuery, doctorObj, doctorNetEarning, commissionDeducted, callback);
            }

            @Override
            public void onFailure(Call<List<CommissionSettings>> call, Throwable t) {
                Log.e(TAG, "Failed to fetch global commission settings, using fallback defaults: " + t.getMessage());
                CommissionSettings defaultSettings = new CommissionSettings();
                double commissionDeducted = defaultSettings.calculateCommission(totalFee, isFollowUp);
                double doctorNetEarning = defaultSettings.calculateDoctorEarning(totalFee, isFollowUp);

                updateDoctorEarningsInSupabase(docIdQuery, doctorObj, doctorNetEarning, commissionDeducted, callback);
            }
        });
    }

    private static void updateDoctorEarningsInSupabase(String docIdQuery, Doctor doctor, double netEarningToAdd, double commissionDeducted, CommissionCallback callback) {
        double currentToday = doctor != null ? doctor.getTodayEarning() : 0;
        double currentTotal = doctor != null ? doctor.getTotalEarning() : 0;

        double newToday = currentToday + netEarningToAdd;
        double newTotal = currentTotal + netEarningToAdd;

        Map<String, Object> updatePayload = new HashMap<>();
        updatePayload.put("today_earning", newToday);
        updatePayload.put("total_earning", newTotal);

        SupabaseClient.getDoctorService().updateDoctorEarnings(docIdQuery, "return=minimal", updatePayload)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        Log.d(TAG, "Doctor earnings updated in Supabase! New Today: " + newToday + ", New Total: " + newTotal);
                        if (callback != null) callback.onCompleted(netEarningToAdd, commissionDeducted);
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Log.e(TAG, "Failed to update doctor earnings in Supabase: " + t.getMessage());
                        if (callback != null) callback.onCompleted(netEarningToAdd, commissionDeducted);
                    }
                });
    }
}
