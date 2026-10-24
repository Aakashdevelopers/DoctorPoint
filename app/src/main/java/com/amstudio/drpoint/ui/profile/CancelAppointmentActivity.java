package com.amstudio.drpoint.ui.profile;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.drpoint.R;
import com.amstudio.drpoint.databinding.ActivityCancelAppointmentBinding;
import com.amstudio.drpoint.databinding.ItemCancellableAppointmentBinding;
import com.amstudio.drpoint.model.Appointment;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.network.SupabaseClient;
import com.amstudio.drpoint.network.model.CancelAppointmentRpcRequest;
import com.amstudio.drpoint.util.AvailabilityHelper;
import com.amstudio.drpoint.util.PreferenceManager;
import com.amstudio.drpoint.util.ToastUtils;
import com.amstudio.drpoint.util.WindowInsetsUtil;
import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CancelAppointmentActivity extends AppCompatActivity {

    private ActivityCancelAppointmentBinding binding;
    private CancellableAdapter adapter;
    private final List<Appointment> cancellableList = new ArrayList<>();
    private final Map<String, Doctor> doctorCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCancelAppointmentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowInsetsUtil.applySystemBarInsets(binding.getRoot());
        setContentView(binding.getRoot());

        binding.flBack.setOnClickListener(v -> finish());

        binding.rvCancellableAppointments.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CancellableAdapter();
        binding.rvCancellableAppointments.setAdapter(adapter);

        binding.swipeRefreshLayout.setOnRefreshListener(this::loadCancellableAppointments);
        binding.btnRetry.setOnClickListener(v -> loadCancellableAppointments());

        loadCancellableAppointments();
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

    private void loadCancellableAppointments() {
        PreferenceManager prefManager = PreferenceManager.getInstance(this);

        if (!prefManager.isLoggedIn()) {
            showEmptyState("No Cancellable Appointments", "Please sign in to view and manage your appointments.", false);
            return;
        }

        String patientId = prefManager.getUserId();
        if (patientId == null || patientId.trim().isEmpty()) {
            showEmptyState("No Cancellable Appointments", "No active account session found. Please log in again.", false);
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

        // Backend level filter parameter for non-cancelled and cancellable statuses
        String statusFilter = "not.in.(Cancelled,cancelled,Completed,completed,Refunded,refunded,Rejected,rejected,no_show)";

        if (binding.shimmerCancelAppointments != null && cancellableList.isEmpty()) {
            binding.shimmerCancelAppointments.startShimmer();
            binding.shimmerCancelAppointments.setVisibility(View.VISIBLE);
            binding.rvCancellableAppointments.setVisibility(View.GONE);
            binding.llEmptyState.setVisibility(View.GONE);
        }

        SupabaseClient.getAppointmentService().getAppointmentsForPatientFiltered(queryParam, statusFilter)
                .enqueue(new Callback<List<Appointment>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Appointment>> call, @NonNull Response<List<Appointment>> response) {
                        binding.swipeRefreshLayout.setRefreshing(false);
                        if (binding.shimmerCancelAppointments != null) {
                            binding.shimmerCancelAppointments.stopShimmer();
                            binding.shimmerCancelAppointments.setVisibility(View.GONE);
                        }

                        if (response.isSuccessful() && response.body() != null) {
                            enrichAppointmentsAndFilter(response.body());
                        } else {
                            fetchAppointmentsFallback(queryParam);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<Appointment>> call, @NonNull Throwable t) {
                        fetchAppointmentsFallback(queryParam);
                    }
                });
    }

    private void fetchAppointmentsFallback(String queryParam) {
        SupabaseClient.getAppointmentService().getAppointmentsForPatient(queryParam)
                .enqueue(new Callback<List<Appointment>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Appointment>> call, @NonNull Response<List<Appointment>> response) {
                        binding.swipeRefreshLayout.setRefreshing(false);
                        if (binding.shimmerCancelAppointments != null) {
                            binding.shimmerCancelAppointments.stopShimmer();
                            binding.shimmerCancelAppointments.setVisibility(View.GONE);
                        }

                        List<Appointment> allAppts = new ArrayList<>();
                        if (response.isSuccessful() && response.body() != null) {
                            allAppts = response.body();
                            enrichAppointmentsAndFilter(allAppts);
                        } else {
                            showErrorState("Unable to load appointments. Try again.");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<Appointment>> call, @NonNull Throwable t) {
                        binding.swipeRefreshLayout.setRefreshing(false);
                        if (binding.shimmerCancelAppointments != null) {
                            binding.shimmerCancelAppointments.stopShimmer();
                            binding.shimmerCancelAppointments.setVisibility(View.GONE);
                        }
                        showErrorState("Unable to load appointments. Check your network connection and try again.");
                    }
                });
    }

    private void enrichAppointmentsAndFilter(List<Appointment> allAppts) {
        cancellableList.clear();

        for (Appointment a : allAppts) {
            if (a == null) continue;

            // Strict 1: Check patient ID belongs to current logged in user ONLY
            String apptPatientId = a.getPatientId() != null ? a.getPatientId() : a.getUserId();
            if (!belongsToCurrentUser(apptPatientId)) {
                continue;
            }

            // Strict 2: Check status - CANCELLED, COMPLETED, REFUNDED, REJECTED, NO_SHOW must NEVER be shown
            String st = a.getStatus() != null ? a.getStatus().trim().toLowerCase(Locale.US) : "";
            if ("cancelled".equals(st) || "completed".equals(st) || "refunded".equals(st) || "rejected".equals(st) || "no_show".equals(st)) {
                continue;
            }

            // Strict 3: Must be cancellable
            if (a.isCancellable()) {
                cancellableList.add(a);
            }
        }

        if (cancellableList.isEmpty()) {
            showEmptyState("No Cancellable Appointments", "You don't have any upcoming active appointments eligible for cancellation.", false);
            return;
        }

        List<String> missingDocIds = new ArrayList<>();
        for (Appointment a : cancellableList) {
            String docId = a.getDoctorId();
            if (docId != null && !docId.trim().isEmpty() && !doctorCache.containsKey(docId) && a.getDoctor() == null) {
                if (!missingDocIds.contains(docId)) missingDocIds.add(docId);
            } else if (a.getDoctor() != null && docId != null) {
                doctorCache.put(docId, a.getDoctor());
            }
        }

        if (missingDocIds.isEmpty()) {
            displayList();
            return;
        }

        final int[] fetchedCount = {0};
        final int totalToFetch = missingDocIds.size();

        for (String dId : missingDocIds) {
            SupabaseClient.getDoctorService().getDoctorById("eq." + dId).enqueue(new Callback<List<Doctor>>() {
                @Override
                public void onResponse(@NonNull Call<List<Doctor>> call, @NonNull Response<List<Doctor>> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        Doctor doc = response.body().get(0);
                        if (doc != null && doc.getId() != null) {
                            doctorCache.put(doc.getId(), doc);
                        }
                    }
                    fetchedCount[0]++;
                    if (fetchedCount[0] >= totalToFetch) {
                        displayList();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<List<Doctor>> call, @NonNull Throwable t) {
                    fetchedCount[0]++;
                    if (fetchedCount[0] >= totalToFetch) {
                        displayList();
                    }
                }
            });
        }
    }

    private void displayList() {
        for (Appointment a : cancellableList) {
            if (a.getDoctor() == null && a.getDoctorId() != null && doctorCache.containsKey(a.getDoctorId())) {
                a.setDoctor(doctorCache.get(a.getDoctorId()));
            }
        }
        if (cancellableList.isEmpty()) {
            showEmptyState("No Cancellable Appointments", "You don't have any upcoming active appointments eligible for cancellation.", false);
        } else {
            binding.rvCancellableAppointments.setVisibility(View.VISIBLE);
            binding.llEmptyState.setVisibility(View.GONE);
            adapter.notifyDataSetChanged();
        }
    }

    private void showEmptyState(String title, String subtitle, boolean showRetry) {
        binding.rvCancellableAppointments.setVisibility(View.GONE);
        binding.llEmptyState.setVisibility(View.VISIBLE);
        binding.tvEmptyTitle.setText(title);
        binding.tvEmptySubtitle.setText(subtitle);
        binding.btnRetry.setVisibility(showRetry ? View.VISIBLE : View.GONE);
    }

    private void showErrorState(String message) {
        if (!cancellableList.isEmpty()) {
            ToastUtils.showError(this, message);
        } else {
            showEmptyState("Unable to load appointments", message, true);
        }
    }

    private void confirmAndCancelAppointment(Appointment appointment, int position) {
        if (appointment == null) return;

        String docName = appointment.getDoctorName();
        String dateStr = appointment.getDate() != null ? appointment.getDate() : "scheduled date";
        String timeStr = appointment.getFormattedTime();

        String message = "Are you sure you want to cancel your appointment with " + docName + " on " + dateStr + " at " + timeStr + "?";

        new MaterialAlertDialogBuilder(this)
                .setTitle("Cancel Appointment?")
                .setMessage(message)
                .setPositiveButton("Cancel Appointment", (dialog, which) -> executeCancellation(appointment, position))
                .setNegativeButton("Keep Appointment", null)
                .show();
    }

    private void executeCancellation(Appointment appointment, int position) {
        if (appointment == null) return;

        PreferenceManager prefManager = PreferenceManager.getInstance(this);
        String patientId = prefManager.getUserId();
        if (patientId == null) patientId = "user_default";

        ToastUtils.showInfo(this, "Cancelling appointment...");

        boolean isWithinOneHour = AvailabilityHelper.isLessThanOneHourBeforeStart(appointment);
        int pct = AvailabilityHelper.calculateRefundPercentage(appointment);
        int totalPaid = appointment.getAmount() > 0 ? appointment.getAmount() : 500;
        int refundAmt = AvailabilityHelper.calculateRefundAmount(totalPaid, isWithinOneHour);

        String cancelReason = isWithinOneHour
                ? "Cancelled by patient (Within 1 hour of schedule - 50% Refund Approved)"
                : "Cancelled by patient (1 hour before schedule - 100% Refund Approved)";
        String cancelledAt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(new Date());

        appointment.setStatus("Cancelled");
        appointment.setPatientReason(cancelReason);
        appointment.setCancelledAt(cancelledAt);

        // Step 1: Send RPC cancellation request
        CancelAppointmentRpcRequest rpcRequest = new CancelAppointmentRpcRequest(appointment.getId(), patientId, cancelReason, cancelledAt);
        final String finalPatientId = patientId;

        SupabaseClient.getSlotService().cancelAppointment(rpcRequest).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                onCancellationSuccess(appointment, finalPatientId, isWithinOneHour, pct, refundAmt, cancelReason);
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                // Fallback: Direct status update
                Map<String, Object> updateMap = new HashMap<>();
                updateMap.put("status", "Cancelled");
                updateMap.put("patient_reason", cancelReason);
                updateMap.put("notes", cancelReason);
                updateMap.put("cancelled_at", cancelledAt);

                SupabaseClient.getAppointmentService().updateAppointmentStatus("eq." + appointment.getId(), updateMap)
                        .enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                                onCancellationSuccess(appointment, finalPatientId, isWithinOneHour, pct, refundAmt, cancelReason);
                            }

                            @Override
                            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                                onCancellationSuccess(appointment, finalPatientId, isWithinOneHour, pct, refundAmt, cancelReason);
                            }
                        });
            }
        });
    }

    private void onCancellationSuccess(Appointment appointment, String patientId, boolean isWithinOneHour, int pct, int refundAmt, String cancelReason) {
        ToastUtils.showSuccess(this, "Appointment cancelled successfully. (" + pct + "% Refund Approved: ₹" + refundAmt + ")");

        // Post refund request if payment was made
        int paidAmt = appointment.getAmount() > 0 ? appointment.getAmount() : 500;
        String payStatus = appointment.getPaymentStatus() != null ? appointment.getPaymentStatus() : "";
        if (paidAmt > 0 || "Paid".equalsIgnoreCase(payStatus) || "Paid Online".equalsIgnoreCase(payStatus)) {
            PreferenceManager prefManager = PreferenceManager.getInstance(this);
            String patientName = prefManager.getUserName();
            String patientPhone = prefManager.getUserPhone();

            Map<String, Object> refundMap = new HashMap<>();
            refundMap.put("appointment_id", appointment.getId());
            refundMap.put("patient_id", patientId);
            refundMap.put("patient_name", patientName != null ? patientName : "Patient");
            refundMap.put("patient_phone", patientPhone != null ? patientPhone : "");
            refundMap.put("doctor_id", appointment.getDoctorId() != null ? appointment.getDoctorId() : "doc_1");
            refundMap.put("doctor_name", appointment.getDoctorName());
            refundMap.put("doctor_specialization", appointment.getSpecialization());
            refundMap.put("clinic_name", appointment.getClinicName());
            refundMap.put("appointment_date", appointment.getDate());
            refundMap.put("amount", refundAmt);
            refundMap.put("refund_percentage", pct);
            refundMap.put("reason", cancelReason);
            refundMap.put("status", "pending");

            SupabaseClient.getAppointmentService()
                    .postRefundRequestPayload("resolution=merge-duplicates", "return=minimal", refundMap)
                    .enqueue(new Callback<List<Map<String, Object>>>() {
                        @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {}
                        @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
                    });
        }

        loadCancellableAppointments();
    }

    private class CancellableAdapter extends RecyclerView.Adapter<CancellableAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemCancellableAppointmentBinding b = ItemCancellableAppointmentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(b);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(cancellableList.get(position), position);
        }

        @Override
        public int getItemCount() {
            return cancellableList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private final ItemCancellableAppointmentBinding b;

            ViewHolder(@NonNull ItemCancellableAppointmentBinding binding) {
                super(binding.getRoot());
                this.b = binding;
            }

            void bind(Appointment appointment, int position) {
                Context context = itemView.getContext();

                b.tvDoctorName.setText(appointment.getDoctorName());
                b.tvSpecialization.setText(appointment.getSpecialization());
                b.tvClinicName.setText(appointment.getClinicName() + " • " + appointment.getLocation());

                String dateStr = appointment.getDate() != null ? appointment.getDate() : "";
                String timeStr = appointment.getFormattedTime();
                String st = appointment.getStatus() != null ? appointment.getStatus().toLowerCase(Locale.US) : "";
                boolean isCancelled = st.equals("cancelled") || st.equals("rejected") || st.equals("no_show") || st.contains("cancel");

                if (isCancelled) {
                    b.tvDateTime.setText(dateStr + " • " + timeStr + " (canceled)");
                    b.tvDateTime.setTextColor(ContextCompat.getColor(context, R.color.error_red));
                } else {
                    b.tvDateTime.setText(dateStr + " • " + timeStr);
                    b.tvDateTime.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
                }

                String origDate = appointment.getOriginalAppointmentDate();
                String origTime = appointment.getOriginalFormattedTime();
                if (origDate != null && !origDate.isEmpty() && (!origDate.equals(dateStr) || !origTime.equals(timeStr))) {
                    b.tvOriginalDatetime.setVisibility(View.VISIBLE);
                    b.tvOriginalDatetime.setText("Orig: " + origDate + " • " + origTime);
                } else {
                    b.tvOriginalDatetime.setVisibility(View.GONE);
                }

                int token = appointment.getSlotNumber();
                b.tvTokenNumber.setText("Your Number: #" + (token > 0 ? token : 1));

                int amt = appointment.getAmount();
                if (amt <= 0) amt = 500;
                b.tvPaidAmount.setText("Paid: ₹" + amt);

                String statusText = appointment.getUserFriendlyStatus();
                b.tvStatusBadge.setText(statusText);

                if ("Confirmed".equalsIgnoreCase(statusText)) {
                    b.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.success_green));
                    b.llStatusBadge.setBackgroundResource(R.drawable.bg_badge_verified);
                } else {
                    b.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.warning_yellow));
                    b.llStatusBadge.setBackgroundResource(R.drawable.bg_warning_chip);
                }

                String avatar = appointment.getDoctor() != null ? appointment.getDoctor().getImageUrl() : null;
                if (avatar != null && !avatar.trim().isEmpty()) {
                    try {
                        Glide.with(context)
                                .load(avatar)
                                .placeholder(R.drawable.ic_user)
                                .error(R.drawable.ic_user)
                                .into(b.ivDoctorAvatar);
                    } catch (Exception e) {
                        b.ivDoctorAvatar.setImageResource(R.drawable.ic_user);
                    }
                } else {
                    b.ivDoctorAvatar.setImageResource(R.drawable.ic_user);
                }

                b.btnCancelAppointment.setOnClickListener(v -> confirmAndCancelAppointment(appointment, position));
            }
        }
    }
}
