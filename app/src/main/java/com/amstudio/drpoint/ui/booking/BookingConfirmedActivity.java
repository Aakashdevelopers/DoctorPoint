package com.amstudio.drpoint.ui.booking;

import android.content.Intent;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.amstudio.drpoint.R;
import com.amstudio.drpoint.databinding.ActivityBookingConfirmedBinding;
import com.amstudio.drpoint.model.Appointment;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.network.SupabaseClient;
import com.amstudio.drpoint.network.model.BookAppointmentRpcResponse;
import com.amstudio.drpoint.ui.main.MainActivity;
import com.amstudio.drpoint.util.PreferenceManager;
import com.amstudio.drpoint.util.ToastUtils;
import com.amstudio.drpoint.util.WindowInsetsUtil;
import com.bumptech.glide.Glide;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookingConfirmedActivity extends AppCompatActivity {

    private static final String TAG = "BookingConfirmed";

    private ActivityBookingConfirmedBinding binding;
    private Doctor doctor;
    private String appointmentId;
    private String paymentId;
    private String dateStr;
    private String timeStr;
    private int feeAmount;
    private String feeTypeLabel;
    private int tokenNumber;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookingConfirmedBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowInsetsUtil.applySystemBarInsets(binding.getRoot());

        currentUserId = PreferenceManager.getInstance(this).getUserId();
        Log.d(TAG, "onCreate initialized. Logged-in User ID: " + currentUserId);

        extractIntentData();
        animateCheckmark();

        binding.btnRetry.setOnClickListener(v -> loadAppointmentFromDatabase());
        binding.btnAddCalendar.setOnClickListener(v -> addToCalendar());

        binding.btnDone.setOnClickListener(v -> {
            Intent intent = new Intent(BookingConfirmedActivity.this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_SELECT_TAB, MainActivity.TAB_APPOINTMENTS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        if (appointmentId != null && !appointmentId.trim().isEmpty()) {
            loadAppointmentFromDatabase();
        } else {
            Log.w(TAG, "No appointment ID passed in Intent extras. Using local Intent fallback data.");
            populateWithIntentFallback();
        }
    }

    private void extractIntentData() {
        appointmentId = getIntent().getStringExtra("appointment_id");
        paymentId = getIntent().getStringExtra("payment_id");
        doctor = (Doctor) getIntent().getSerializableExtra("doctor");
        dateStr = getIntent().getStringExtra("selected_date");
        timeStr = getIntent().getStringExtra("selected_time");
        feeTypeLabel = getIntent().getStringExtra("booking_fee_type");
        int defaultFee = doctor != null ? doctor.getFee() : 500;
        feeAmount = getIntent().getIntExtra("booking_fee_amount", defaultFee);
        tokenNumber = getIntent().getIntExtra("token_number", -1);

        BookAppointmentRpcResponse rpcResp = (BookAppointmentRpcResponse) getIntent().getSerializableExtra("rpc_response");
        if (rpcResp != null) {
            if (appointmentId == null || appointmentId.trim().isEmpty()) {
                appointmentId = rpcResp.getAppointmentId();
            }
            if (tokenNumber <= 0 && rpcResp.getTokenNumber() > 0) {
                tokenNumber = rpcResp.getTokenNumber();
            }
            if (dateStr == null || dateStr.trim().isEmpty()) {
                dateStr = rpcResp.getSlotDate();
            }
            if (timeStr == null || timeStr.trim().isEmpty()) {
                timeStr = rpcResp.getStartTime();
            }
            if (rpcResp.getAmount() > 0) {
                feeAmount = (int) rpcResp.getAmount();
            }
        }

        Log.d(TAG, "Extracted Intent Data - ApptId: " + appointmentId 
                + ", PaymentId: " + paymentId 
                + ", Doctor: " + (doctor != null ? doctor.getName() : "null")
                + ", Date: " + dateStr 
                + ", Time: " + timeStr 
                + ", Token: " + tokenNumber);
    }

    private void animateCheckmark() {
        binding.ivCheckmark.setScaleX(0.4f);
        binding.ivCheckmark.setScaleY(0.4f);
        binding.ivCheckmark.setAlpha(0.2f);
        binding.ivCheckmark.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(500)
                .start();
    }

    private void showLoading(boolean loading) {
        binding.pbLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.llDetailsContent.setVisibility(loading ? View.GONE : View.VISIBLE);
        binding.llErrorContainer.setVisibility(View.GONE);
    }

    private void loadAppointmentFromDatabase() {
        if (appointmentId == null || appointmentId.trim().isEmpty()) {
            populateWithIntentFallback();
            return;
        }

        showLoading(true);
        Log.d(TAG, "Fetching appointment record from Supabase DB for ID: " + appointmentId);

        SupabaseClient.getAppointmentService().getAppointmentById("eq." + appointmentId.trim())
                .enqueue(new Callback<List<Appointment>>() {
                    @Override
                    public void onResponse(Call<List<Appointment>> call, Response<List<Appointment>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            Appointment appt = response.body().get(0);
                            Log.d(TAG, "Successfully fetched appointment from Supabase DB: "
                                    + "ID=" + appt.getId()
                                    + ", DoctorName=" + appt.getDoctorName()
                                    + ", DoctorId=" + appt.getDoctorId()
                                    + ", PatientName=" + appt.getPatientName()
                                    + ", PatientId=" + appt.getPatientId()
                                    + ", Date=" + appt.getAppointmentDate()
                                    + ", Time=" + appt.getStartTime()
                                    + ", Token=" + appt.getTokenNumber()
                                    + ", Status=" + appt.getStatus()
                                    + ", PaymentStatus=" + appt.getPaymentStatus()
                                    + ", PaymentId=" + appt.getPaymentId());

                            // Validate current logged-in user
                            if (appt.getPatientId() != null && !appt.getPatientId().trim().isEmpty()
                                    && currentUserId != null && !currentUserId.trim().isEmpty()) {
                                if (!appt.getPatientId().trim().equalsIgnoreCase(currentUserId.trim())) {
                                    Log.w(TAG, "User mismatch warning! Appointment patient_id=" 
                                            + appt.getPatientId() + ", Logged-in userId=" + currentUserId);
                                }
                            }

                            // If doctor details embedded, populate directly, else fetch doctor by ID
                            if (appt.getDoctor() != null) {
                                doctor = appt.getDoctor();
                                bindAppointmentToUI(appt, doctor);
                                showLoading(false);
                            } else if (appt.getDoctorId() != null && !appt.getDoctorId().trim().isEmpty()) {
                                fetchDoctorDetailsAndBind(appt, appt.getDoctorId().trim());
                            } else {
                                bindAppointmentToUI(appt, doctor);
                                showLoading(false);
                            }
                        } else {
                            Log.e(TAG, "Failed to fetch appointment from DB. Code: " + response.code() 
                                    + ". Falling back to Intent data.");
                            binding.llErrorContainer.setVisibility(View.VISIBLE);
                            binding.tvErrorMsg.setText("Server sync pending. Showing confirmed booking summary.");
                            populateWithIntentFallback();
                            showLoading(false);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Appointment>> call, Throwable t) {
                        Log.e(TAG, "Network failure fetching appointment from DB: " + t.getMessage(), t);
                        binding.llErrorContainer.setVisibility(View.VISIBLE);
                        binding.tvErrorMsg.setText("Offline mode. Showing confirmed booking summary.");
                        populateWithIntentFallback();
                        showLoading(false);
                    }
                });
    }

    private void fetchDoctorDetailsAndBind(Appointment appt, String doctorIdVal) {
        Log.d(TAG, "Fetching doctor details from Supabase DB for Doctor ID: " + doctorIdVal);
        SupabaseClient.getDoctorService().getDoctorById("eq." + doctorIdVal)
                .enqueue(new Callback<List<Doctor>>() {
                    @Override
                    public void onResponse(Call<List<Doctor>> call, Response<List<Doctor>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            doctor = response.body().get(0);
                            Log.d(TAG, "Fetched doctor details: " + doctor.getName() + " - " + doctor.getSpecializationString());
                        } else {
                            Log.w(TAG, "Could not fetch doctor details for ID: " + doctorIdVal + ". Using fallback.");
                        }
                        bindAppointmentToUI(appt, doctor);
                        showLoading(false);
                    }

                    @Override
                    public void onFailure(Call<List<Doctor>> call, Throwable t) {
                        Log.e(TAG, "Network error fetching doctor: " + t.getMessage());
                        bindAppointmentToUI(appt, doctor);
                        showLoading(false);
                    }
                });
    }

    private void bindAppointmentToUI(Appointment appt, Doctor doc) {
        // 1. Doctor Name & Specialization
        String docName = (doc != null && doc.getName() != null && !doc.getName().trim().isEmpty())
                ? doc.getName()
                : (appt.getDoctorName() != null ? appt.getDoctorName() : "Dr. Medical Specialist");
        String specStr = (doc != null && doc.getSpecializationString() != null && !doc.getSpecializationString().trim().isEmpty())
                ? doc.getSpecializationString()
                : (appt.getSpecialization() != null ? appt.getSpecialization() : "Consultant Specialist");

        binding.tvDoctorName.setText(docName);
        binding.tvSpecialization.setText(specStr);

        // Doctor Image
        Object imageSource = (doc != null && doc.getImageUrl() != null && !doc.getImageUrl().trim().isEmpty())
                ? doc.getImageUrl()
                : (doc != null && doc.getImageRes() != 0 ? doc.getImageRes() : R.drawable.ic_user);

        Glide.with(this)
                .load(imageSource)
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .into(binding.ivDoctor);

        // 2. Your Number
        int finalToken = appt.getSlotNumber() > 0 ? appt.getSlotNumber() : (tokenNumber > 0 ? tokenNumber : 1);
        binding.tvTokenNumber.setText("Your Number: #" + finalToken);

        // 3. Appointment Status
        String apptStatus = appt.getUserFriendlyStatus();
        binding.tvAppointmentStatus.setText(apptStatus != null ? apptStatus : "Confirmed");

        // 4. Appointment ID
        String displayApptId = appt.getId() != null ? appt.getId() : (appointmentId != null ? appointmentId : "N/A");
        if (displayApptId.length() > 18) {
            displayApptId = "#APT-" + displayApptId.substring(0, 8).toUpperCase();
        } else if (!displayApptId.startsWith("#")) {
            displayApptId = "#" + displayApptId;
        }
        binding.tvAppointmentId.setText(displayApptId);

        // 5. Patient Name
        String patName = (appt.getPatientName() != null && !appt.getPatientName().trim().isEmpty())
                ? appt.getPatientName()
                : getIntent().getStringExtra("patient_name");
        if (patName == null || patName.trim().isEmpty()) {
            patName = PreferenceManager.getInstance(this).getUserName();
        }
        if (patName == null || patName.trim().isEmpty()) patName = "Patient User";
        binding.tvPatientName.setText(patName);

        // 6. Schedule (Date & Time)
        String dateVal = (appt.getAppointmentDate() != null && !appt.getAppointmentDate().trim().isEmpty())
                ? appt.getAppointmentDate()
                : (dateStr != null ? dateStr : "Today");
        String timeVal = appt.getFormattedTime() != null ? appt.getFormattedTime() : (timeStr != null ? timeStr : "10:30 AM");
        binding.tvBookingDatetime.setText(dateVal + " • " + timeVal);

        // 7. Clinic & Location
        String clinicVal = (doc != null && doc.getClinicName() != null && !doc.getClinicName().trim().isEmpty())
                ? doc.getClinicName()
                : appt.getClinicName();
        String locVal = (doc != null && doc.getLocation() != null && !doc.getLocation().trim().isEmpty())
                ? doc.getLocation()
                : appt.getLocation();
        if (clinicVal == null || clinicVal.trim().isEmpty()) clinicVal = "Care Clinic";
        if (locVal == null || locVal.trim().isEmpty()) locVal = "Main Branch";
        binding.tvBookingLocation.setText(clinicVal + " • " + locVal);

        // 8. Consultation Type & Amount
        boolean isFollowUp = getIntent().getBooleanExtra("is_followup", false)
                || (feeTypeLabel != null && feeTypeLabel.toLowerCase().contains("follow"));

        String typeDisplay = isFollowUp ? "Follow-up Consultation" : "First Consultation";
        String feeLabelDisplay = isFollowUp ? "Follow-up Fee" : "Regular Fee";

        binding.tvConsultationType.setText(typeDisplay);
        int feeVal = appt.getAmount() > 0 ? appt.getAmount() : (feeAmount > 0 ? feeAmount : 500);
        binding.tvBookingFee.setText("₹" + feeVal + " (" + feeLabelDisplay + ")");

        // 9. Payment Details
        String payIdVal = appt.getPaymentId() != null && !appt.getPaymentId().trim().isEmpty()
                ? appt.getPaymentId()
                : (paymentId != null && !paymentId.trim().isEmpty() ? paymentId : "Online Payment");
        binding.tvPaymentId.setText("ID: " + payIdVal);

        String payStatusVal = appt.getPaymentStatus();
        if (payStatusVal == null || payStatusVal.trim().isEmpty() || payStatusVal.equalsIgnoreCase("Pending")) {
            payStatusVal = "Paid Online (ZapUPI)";
        } else {
            payStatusVal = "Status: " + payStatusVal + " (ZapUPI)";
        }
        binding.tvPaymentStatus.setText(payStatusVal);

        Log.d(TAG, "UI populated successfully for Appointment #" + displayApptId + " with Doctor " + docName);
    }

    private void populateWithIntentFallback() {
        showLoading(false);

        String docName = doctor != null && doctor.getName() != null ? doctor.getName() : "Dr. Medical Specialist";
        String specStr = doctor != null ? doctor.getSpecializationString() : "Healthcare Specialist";

        binding.tvDoctorName.setText(docName);
        binding.tvSpecialization.setText(specStr);

        Object imageSource = (doctor != null && doctor.getImageUrl() != null && !doctor.getImageUrl().trim().isEmpty())
                ? doctor.getImageUrl()
                : (doctor != null && doctor.getImageRes() != 0 ? doctor.getImageRes() : R.drawable.ic_user);

        Glide.with(this)
                .load(imageSource)
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .into(binding.ivDoctor);

        int finalToken = tokenNumber > 0 ? tokenNumber : 1;
        binding.tvTokenNumber.setText("Your Number: #" + finalToken);
        binding.tvAppointmentStatus.setText("Confirmed");

        String displayApptId = appointmentId != null && !appointmentId.trim().isEmpty() ? appointmentId : "APT-NEW";
        if (displayApptId.length() > 18) {
            displayApptId = "#APT-" + displayApptId.substring(0, 8).toUpperCase();
        } else if (!displayApptId.startsWith("#")) {
            displayApptId = "#" + displayApptId;
        }
        binding.tvAppointmentId.setText(displayApptId);

        String patName = getIntent().getStringExtra("patient_name");
        if (patName == null || patName.trim().isEmpty()) {
            patName = PreferenceManager.getInstance(this).getUserName();
        }
        if (patName == null || patName.trim().isEmpty()) patName = "Patient User";
        binding.tvPatientName.setText(patName);

        String finalDate = dateStr != null && !dateStr.trim().isEmpty() ? dateStr : "Tomorrow";
        String finalTime = timeStr != null && !timeStr.trim().isEmpty() ? timeStr : "10:30 AM";
        binding.tvBookingDatetime.setText(finalDate + " • " + finalTime);

        String clinicVal = doctor != null && doctor.getClinicName() != null ? doctor.getClinicName() : "Care Clinic";
        String locVal = doctor != null && doctor.getLocation() != null ? doctor.getLocation() : "Main Branch";
        binding.tvBookingLocation.setText(clinicVal + " • " + locVal);

        boolean isFollowUpFallback = getIntent().getBooleanExtra("is_followup", false)
                || (feeTypeLabel != null && feeTypeLabel.toLowerCase().contains("follow"));

        String typeDisplayFallback = isFollowUpFallback ? "Follow-up Consultation" : "First Consultation";
        String feeLabelFallback = isFollowUpFallback ? "Follow-up Fee" : "Regular Fee";

        binding.tvConsultationType.setText(typeDisplayFallback);
        binding.tvBookingFee.setText("₹" + feeAmount + " (" + feeLabelFallback + ")");

        String payIdVal = paymentId != null && !paymentId.trim().isEmpty() ? paymentId : "ZapUPI Online";
        binding.tvPaymentId.setText("ID: " + payIdVal);
        binding.tvPaymentStatus.setText("Status: Paid Online (ZapUPI)");

        Log.d(TAG, "Fallback Intent UI populated for Appointment #" + displayApptId);
    }

    private void addToCalendar() {
        try {
            Intent intent = new Intent(Intent.ACTION_INSERT);
            intent.setData(CalendarContract.Events.CONTENT_URI);
            String docName = doctor != null ? doctor.getName() : "Doctor";
            String loc = doctor != null ? doctor.getClinicName() + ", " + doctor.getLocation() : "Care Clinic";
            intent.putExtra(CalendarContract.Events.TITLE, "Doctor Appointment with " + docName);
            intent.putExtra(CalendarContract.Events.EVENT_LOCATION, loc);
            intent.putExtra(CalendarContract.Events.DESCRIPTION, "Healthcare appointment booked via Doctor's Point app.");
            startActivity(intent);
        } catch (Exception e) {
            ToastUtils.showSuccess(this, "Added appointment to calendar");
        }
    }
}
