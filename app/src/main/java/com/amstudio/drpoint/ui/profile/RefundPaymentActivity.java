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
import com.amstudio.drpoint.databinding.ActivityRefundPaymentBinding;
import com.amstudio.drpoint.databinding.ItemRefundPaymentCardBinding;
import com.amstudio.drpoint.model.Appointment;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.model.RefundRequest;
import com.amstudio.drpoint.network.SupabaseClient;
import com.amstudio.drpoint.util.AvailabilityHelper;
import com.amstudio.drpoint.util.PreferenceManager;
import com.amstudio.drpoint.util.ToastUtils;
import com.amstudio.drpoint.util.WindowInsetsUtil;
import com.bumptech.glide.Glide;

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

public class RefundPaymentActivity extends AppCompatActivity {

    private ActivityRefundPaymentBinding binding;
    private RefundPaymentAdapter adapter;
    private final List<RefundPaymentItem> displayItems = new ArrayList<>();
    private final Map<String, Doctor> doctorCache = new HashMap<>();

    public static class RefundPaymentItem {
        public Appointment appointment;
        public RefundRequest refundRequest;

        public RefundPaymentItem(Appointment appointment, RefundRequest refundRequest) {
            this.appointment = appointment;
            this.refundRequest = refundRequest;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRefundPaymentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowInsetsUtil.applySystemBarInsets(binding.getRoot());

        binding.flBack.setOnClickListener(v -> finish());

        binding.rvRefundPayments.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RefundPaymentAdapter();
        binding.rvRefundPayments.setAdapter(adapter);

        binding.swipeRefreshLayout.setOnRefreshListener(this::loadRefundAndPaymentData);
        binding.btnRetry.setOnClickListener(v -> loadRefundAndPaymentData());

        loadRefundAndPaymentData();
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

    private void loadRefundAndPaymentData() {
        PreferenceManager prefManager = PreferenceManager.getInstance(this);

        if (!prefManager.isLoggedIn()) {
            showEmptyState("No Refund Records Found", "Please sign in to view your refund and payment history.", false);
            return;
        }

        String patientId = prefManager.getUserId();
        if (patientId == null || patientId.trim().isEmpty()) {
            showEmptyState("No Refund Records Found", "No active account session found. Please sign in again.", false);
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

        String cancelledStatusFilter = "in.(Cancelled,cancelled,Rejected,rejected,no_show)";

        if (binding.shimmerRefundPayments != null && displayItems.isEmpty()) {
            binding.shimmerRefundPayments.startShimmer();
            binding.shimmerRefundPayments.setVisibility(View.VISIBLE);
            binding.rvRefundPayments.setVisibility(View.GONE);
            binding.llEmptyState.setVisibility(View.GONE);
        }

        final List<Appointment>[] userAppointments = new List[]{new ArrayList<>()};
        final List<RefundRequest>[] userRefunds = new List[]{new ArrayList<>()};
        final boolean[] apptsDone = {false};
        final boolean[] refundsDone = {false};

        Runnable checkAndProcess = () -> {
            if (apptsDone[0] && refundsDone[0]) {
                processAndDisplayData(userAppointments[0], userRefunds[0]);
            }
        };

        // Query 1: Filter cancelled/refund-eligible appointments at backend level
        SupabaseClient.getAppointmentService().getAppointmentsForPatientFiltered(queryParam, cancelledStatusFilter)
                .enqueue(new Callback<List<Appointment>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Appointment>> call, @NonNull Response<List<Appointment>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            userAppointments[0] = response.body();
                            apptsDone[0] = true;
                            checkAndProcess.run();
                        } else {
                            // Fallback to general query
                            SupabaseClient.getAppointmentService().getAppointmentsForPatient(queryParam)
                                    .enqueue(new Callback<List<Appointment>>() {
                                        @Override
                                        public void onResponse(@NonNull Call<List<Appointment>> call2, @NonNull Response<List<Appointment>> response2) {
                                            if (response2.isSuccessful() && response2.body() != null) {
                                                userAppointments[0] = response2.body();
                                            }
                                            apptsDone[0] = true;
                                            checkAndProcess.run();
                                        }

                                        @Override
                                        public void onFailure(@NonNull Call<List<Appointment>> call2, @NonNull Throwable t2) {
                                            apptsDone[0] = true;
                                            checkAndProcess.run();
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<Appointment>> call, @NonNull Throwable t) {
                        SupabaseClient.getAppointmentService().getAppointmentsForPatient(queryParam)
                                .enqueue(new Callback<List<Appointment>>() {
                                    @Override
                                    public void onResponse(@NonNull Call<List<Appointment>> call2, @NonNull Response<List<Appointment>> response2) {
                                        if (response2.isSuccessful() && response2.body() != null) {
                                            userAppointments[0] = response2.body();
                                        }
                                        apptsDone[0] = true;
                                        checkAndProcess.run();
                                    }

                                    @Override
                                    public void onFailure(@NonNull Call<List<Appointment>> call2, @NonNull Throwable t2) {
                                        apptsDone[0] = true;
                                        checkAndProcess.run();
                                    }
                                });
                    }
                });

        // Query 2: Get refund requests for patient
        SupabaseClient.getAppointmentService().getRefundRequestsForPatient(queryParam)
                .enqueue(new Callback<List<RefundRequest>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<RefundRequest>> call, @NonNull Response<List<RefundRequest>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            userRefunds[0] = response.body();
                        }
                        refundsDone[0] = true;
                        checkAndProcess.run();
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<RefundRequest>> call, @NonNull Throwable t) {
                        refundsDone[0] = true;
                        checkAndProcess.run();
                    }
                });
    }

    private boolean isEligibleForRefundActivity(Appointment a, RefundRequest r) {
        // Must belong to current logged in user
        if (a != null) {
            String apptPatientId = a.getPatientId() != null ? a.getPatientId() : a.getUserId();
            if (!belongsToCurrentUser(apptPatientId)) return false;
        }
        if (r != null) {
            if (!belongsToCurrentUser(r.getPatientId())) return false;
        }

        // 1. If a RefundRequest record exists for this user, it belongs in Refund Activity!
        if (r != null) {
            return true;
        }

        // 2. If no RefundRequest record exists, show ONLY if appointment is Cancelled/Rejected/No_Show AND payment was Paid AND refund eligible
        if (a != null) {
            if (!AvailabilityHelper.isRefundEligible(a)) {
                return false;
            }

            String st = a.getStatus() != null ? a.getStatus().trim().toLowerCase(Locale.US) : "";
            boolean isCancelled = "cancelled".equals(st) || "rejected".equals(st) || "no_show".equals(st);

            if (!isCancelled) {
                // CONFIRMED, PENDING, COMPLETED without refund -> DO NOT SHOW
                return false;
            }

            int amt = a.getAmount();
            String payStatus = a.getPaymentStatus() != null ? a.getPaymentStatus().trim().toLowerCase(Locale.US) : "";
            boolean isPaid = "paid".equals(payStatus) || "paid online".equals(payStatus) || payStatus.contains("paid") || amt > 0;

            return isPaid;
        }

        return false;
    }

    private void processAndDisplayData(List<Appointment> appointments, List<RefundRequest> refunds) {
        binding.swipeRefreshLayout.setRefreshing(false);
        if (binding.shimmerRefundPayments != null) {
            binding.shimmerRefundPayments.stopShimmer();
            binding.shimmerRefundPayments.setVisibility(View.GONE);
        }

        displayItems.clear();

        Map<String, RefundRequest> refundByApptId = new HashMap<>();
        if (refunds != null) {
            for (RefundRequest r : refunds) {
                if (r != null && r.getAppointmentId() != null) {
                    refundByApptId.put(r.getAppointmentId(), r);
                }
            }
        }

        Set<String> processedApptIds = new HashSet<>();

        if (appointments != null) {
            for (Appointment a : appointments) {
                if (a == null || a.getId() == null) continue;
                RefundRequest matchedRefund = refundByApptId.get(a.getId());

                if (isEligibleForRefundActivity(a, matchedRefund)) {
                    displayItems.add(new RefundPaymentItem(a, matchedRefund));
                    processedApptIds.add(a.getId());
                }
            }
        }

        // Add any remaining refund requests whose appointment object was not in appointments list
        if (refunds != null) {
            for (RefundRequest r : refunds) {
                if (r == null || r.getAppointmentId() == null) continue;
                if (!processedApptIds.contains(r.getAppointmentId()) && belongsToCurrentUser(r.getPatientId())) {
                    Appointment dummyAppt = new Appointment();
                    dummyAppt.setId(r.getAppointmentId());
                    dummyAppt.setPatientId(r.getPatientId());
                    dummyAppt.setDoctorId(r.getDoctorId());
                    dummyAppt.setDoctorName(r.getDoctorName());
                    dummyAppt.setSpecialization(r.getDoctorSpecialization());
                    dummyAppt.setClinicName(r.getClinicName());
                    dummyAppt.setAppointmentDate(r.getAppointmentDate());
                    dummyAppt.setStatus("Cancelled");
                    dummyAppt.setPaymentStatus("Paid");
                    dummyAppt.setAmount(r.getAmount());

                    displayItems.add(new RefundPaymentItem(dummyAppt, r));
                    processedApptIds.add(r.getAppointmentId());
                }
            }
        }

        if (displayItems.isEmpty()) {
            showEmptyState("No Refund Records Found", "You don't have any refund requests or cancelled transaction records.", false);
            return;
        }

        List<String> missingDocIds = new ArrayList<>();
        for (RefundPaymentItem item : displayItems) {
            Appointment a = item.appointment;
            if (a != null) {
                String docId = a.getDoctorId();
                if (docId != null && !docId.trim().isEmpty() && !doctorCache.containsKey(docId) && a.getDoctor() == null) {
                    if (!missingDocIds.contains(docId)) missingDocIds.add(docId);
                } else if (a.getDoctor() != null && docId != null) {
                    doctorCache.put(docId, a.getDoctor());
                }
            }
        }

        if (missingDocIds.isEmpty()) {
            renderList();
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
                        renderList();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<List<Doctor>> call, @NonNull Throwable t) {
                    fetchedCount[0]++;
                    if (fetchedCount[0] >= totalToFetch) {
                        renderList();
                    }
                }
            });
        }
    }

    private void renderList() {
        for (RefundPaymentItem item : displayItems) {
            if (item.appointment != null && item.appointment.getDoctor() == null && item.appointment.getDoctorId() != null) {
                if (doctorCache.containsKey(item.appointment.getDoctorId())) {
                    item.appointment.setDoctor(doctorCache.get(item.appointment.getDoctorId()));
                }
            }
        }
        if (displayItems.isEmpty()) {
            showEmptyState("No Refund Records Found", "You don't have any refund requests or cancelled transaction records.", false);
        } else {
            binding.rvRefundPayments.setVisibility(View.VISIBLE);
            binding.llEmptyState.setVisibility(View.GONE);
            adapter.notifyDataSetChanged();
        }
    }

    private void showEmptyState(String title, String subtitle, boolean showRetry) {
        binding.rvRefundPayments.setVisibility(View.GONE);
        binding.llEmptyState.setVisibility(View.VISIBLE);
        binding.tvEmptyTitle.setText(title);
        binding.tvEmptySubtitle.setText(subtitle);
        binding.btnRetry.setVisibility(showRetry ? View.VISIBLE : View.GONE);
    }

    private void submitRefundRequest(Appointment a) {
        if (a == null || a.getId() == null) return;

        // Verify if a refund request already exists
        SupabaseClient.getAppointmentService().getRefundRequestByAppointment("eq." + a.getId())
                .enqueue(new Callback<List<RefundRequest>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<RefundRequest>> call, @NonNull Response<List<RefundRequest>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            ToastUtils.showWarning(RefundPaymentActivity.this, "Refund request has already been submitted for this appointment.");
                            loadRefundAndPaymentData();
                            return;
                        }
                        performSubmitRefundRequest(a);
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<RefundRequest>> call, @NonNull Throwable t) {
                        performSubmitRefundRequest(a);
                    }
                });
    }

    private void performSubmitRefundRequest(Appointment a) {
        PreferenceManager prefManager = PreferenceManager.getInstance(this);
        String patientId = prefManager.getUserId();
        String patientName = prefManager.getUserName();
        String patientPhone = prefManager.getUserPhone();

        int totalFee = a.getAmount() > 0 ? a.getAmount() : 500;
        boolean isWithinOneHour = AvailabilityHelper.isLessThanOneHourBeforeStart(a);
        int pct = AvailabilityHelper.calculateRefundPercentage(a);
        int refundAmt = AvailabilityHelper.calculateRefundAmount(totalFee, isWithinOneHour);
        String reason = isWithinOneHour ? "Cancelled within 1 hour of schedule - 50% Refund Approved" : "Cancelled 1 hour before schedule - 100% Refund Approved";

        Map<String, Object> refundMap = new HashMap<>();
        refundMap.put("appointment_id", a.getId());
        refundMap.put("patient_id", patientId != null ? patientId : "user_default");
        refundMap.put("patient_name", patientName != null ? patientName : "Patient");
        refundMap.put("patient_phone", patientPhone != null ? patientPhone : "");
        refundMap.put("doctor_id", a.getDoctorId() != null ? a.getDoctorId() : "doc_1");
        refundMap.put("doctor_name", a.getDoctorName());
        refundMap.put("doctor_specialization", a.getSpecialization());
        refundMap.put("clinic_name", a.getClinicName());
        refundMap.put("appointment_date", a.getDate() != null ? a.getDate() : "");
        refundMap.put("amount", refundAmt);
        refundMap.put("refund_percentage", pct);
        refundMap.put("reason", reason);
        refundMap.put("status", "pending");

        ToastUtils.showInfo(this, "Submitting refund request...");

        SupabaseClient.getAppointmentService()
                .postRefundRequestPayload("resolution=merge-duplicates", "return=representation", refundMap)
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                        ToastUtils.showSuccess(RefundPaymentActivity.this, "Refund request submitted successfully!");
                        loadRefundAndPaymentData();
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {
                        ToastUtils.showError(RefundPaymentActivity.this, "Failed to submit refund request. Try again.");
                    }
                });
    }

    private class RefundPaymentAdapter extends RecyclerView.Adapter<RefundPaymentAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemRefundPaymentCardBinding b = ItemRefundPaymentCardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(b);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(displayItems.get(position));
        }

        @Override
        public int getItemCount() {
            return displayItems.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private final ItemRefundPaymentCardBinding b;

            ViewHolder(@NonNull ItemRefundPaymentCardBinding binding) {
                super(binding.getRoot());
                this.b = binding;
            }

            void bind(RefundPaymentItem item) {
                Context context = itemView.getContext();
                Appointment a = item.appointment;
                RefundRequest r = item.refundRequest;

                if (a == null) return;

                b.tvDoctorName.setText(a.getDoctorName());
                b.tvSpecialization.setText(a.getSpecialization() + " • " + a.getClinicName());

                String dateStr = a.getDate() != null ? a.getDate() : "";
                String timeStr = a.getFormattedTime();
                String st = a.getStatus() != null ? a.getStatus().toLowerCase(Locale.US) : "";
                boolean isCancelled = st.equals("cancelled") || st.equals("rejected") || st.equals("no_show") || st.contains("cancel");

                if (isCancelled) {
                    b.tvAppointmentDateTime.setText(dateStr + " • " + timeStr + " (canceled)");
                    b.tvAppointmentDateTime.setTextColor(ContextCompat.getColor(context, R.color.error_red));
                } else {
                    b.tvAppointmentDateTime.setText(dateStr + " • " + timeStr);
                    b.tvAppointmentDateTime.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
                }

                String apptStatus = a.getUserFriendlyStatus();
                b.tvAppointmentStatus.setText(apptStatus);

                int amt = a.getAmount();
                if (amt <= 0) amt = 500;
                b.tvOriginalPayment.setText("₹" + amt + " (Paid)");

                boolean isWithinOneHour = AvailabilityHelper.isLessThanOneHourBeforeStart(a);
                int pct = (r != null && r.getRefundPercentage() > 0) ? r.getRefundPercentage() : AvailabilityHelper.calculateRefundPercentage(a);
                int calculatedRefundAmt = AvailabilityHelper.calculateRefundAmount(amt, isWithinOneHour);
                int refundAmt = (r != null && r.getAmount() > 0) ? r.getAmount() : calculatedRefundAmt;

                b.tvRefundAmount.setText("₹" + refundAmt + " (" + pct + "%)");

                String paymentId = a.getPaymentId();
                if (paymentId == null || paymentId.trim().isEmpty()) {
                    paymentId = "pay_rzp_" + (a.getId() != null ? a.getId().substring(0, Math.min(8, a.getId().length())) : "online");
                }
                b.tvPaymentId.setText("Payment ID: " + paymentId);

                String refundRefId = (r != null && r.getId() != null) ? "ref_" + r.getId().substring(0, Math.min(8, r.getId().length())) : "ref_pending_" + (a.getId() != null ? a.getId().substring(0, Math.min(6, a.getId().length())) : "1");
                b.tvRefundId.setText("Refund Reference ID: " + refundRefId);

                b.tvCancellationDate.setText("Appointment Date: " + dateStr);

                String rStatus = (r != null && r.getStatus() != null) ? r.getStatus().trim().toLowerCase(Locale.US) : "";

                b.tvAppointmentStatus.setTextColor(ContextCompat.getColor(context, R.color.error_red));

                if ("approved".equals(rStatus) || "refunded".equals(rStatus)) {
                    b.tvRefundStatusBadge.setText("Refunded");
                    b.tvRefundStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.success_green));
                    b.llRefundStatusBadge.setBackgroundResource(R.drawable.bg_badge_verified);
                    b.tvRefundGuidance.setText("Your appointment was cancelled. Refund Amount: ₹" + refundAmt + " (" + pct + "% Approved) has been processed and credited to your original payment method.");
                    b.llGuidanceBox.setVisibility(View.VISIBLE);

                    b.btnRefundAction.setVisibility(View.VISIBLE);
                    b.btnRefundAction.setText("Refund Completed");
                    b.btnRefundAction.setEnabled(false);
                    b.btnRefundAction.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.success_green));
                } else if ("pending".equals(rStatus) || "processing".equals(rStatus)) {
                    b.tvRefundStatusBadge.setText("Refund Processing");
                    b.tvRefundStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.warning_yellow));
                    b.llRefundStatusBadge.setBackgroundResource(R.drawable.bg_warning_chip);
                    b.tvRefundGuidance.setText("Your appointment was cancelled. Refund Request is under review. Amount: ₹" + refundAmt + " (" + pct + "% Approved). It will be credited according to payment provider timelines.");
                    b.llGuidanceBox.setVisibility(View.VISIBLE);

                    b.btnRefundAction.setVisibility(View.VISIBLE);
                    b.btnRefundAction.setText("Refund Processing");
                    b.btnRefundAction.setEnabled(false);
                    b.btnRefundAction.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.warning_yellow));
                } else if ("rejected".equals(rStatus) || "failed".equals(rStatus)) {
                    b.tvRefundStatusBadge.setText("Refund Failed / Rejected");
                    b.tvRefundStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.error_red));
                    b.llRefundStatusBadge.setBackgroundResource(R.drawable.bg_date_chip_unselected);
                    b.tvRefundGuidance.setText("Your refund request was reviewed and marked ineligible according to policy. Contact support if you need assistance.");
                    b.llGuidanceBox.setVisibility(View.VISIBLE);

                    b.btnRefundAction.setVisibility(View.VISIBLE);
                    b.btnRefundAction.setText("Refund Request Closed");
                    b.btnRefundAction.setEnabled(false);
                    b.btnRefundAction.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.text_muted));
                } else {
                    // Appointment is Cancelled + Paid, but no RefundRequest record exists yet
                    b.tvRefundStatusBadge.setText("Refund Eligible (" + pct + "%)");
                    b.tvRefundStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.primary));
                    b.llRefundStatusBadge.setBackgroundResource(R.drawable.bg_badge_verified);
                    b.tvRefundGuidance.setText("Your appointment was cancelled. Calculated Refund (" + pct + "% Approved): ₹" + refundAmt + ". Click below to submit your refund request.");
                    b.llGuidanceBox.setVisibility(View.VISIBLE);

                    b.btnRefundAction.setVisibility(View.VISIBLE);
                    b.btnRefundAction.setText("Request Refund");
                    b.btnRefundAction.setEnabled(true);
                    b.btnRefundAction.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.primary));
                    b.btnRefundAction.setOnClickListener(v -> submitRefundRequest(a));
                }

                String avatar = a.getDoctor() != null ? a.getDoctor().getImageUrl() : null;
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
            }
        }
    }
}
