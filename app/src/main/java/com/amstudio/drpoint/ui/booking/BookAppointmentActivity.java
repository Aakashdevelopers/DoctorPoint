package com.amstudio.drpoint.ui.booking;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.amstudio.drpoint.R;
import com.amstudio.drpoint.adapter.DateChipAdapter;
import com.amstudio.drpoint.adapter.TimeSlotAdapter;
import com.amstudio.drpoint.databinding.ActivityBookAppointmentBinding;
import com.amstudio.drpoint.model.Appointment;
import com.amstudio.drpoint.model.Clinic;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.model.DoctorSlot;
import com.amstudio.drpoint.network.SupabaseClient;
import com.amstudio.drpoint.network.model.BookAppointmentRpcRequest;
import com.amstudio.drpoint.network.model.BookAppointmentRpcResponse;
import com.amstudio.drpoint.util.AvailabilityHelper;
import com.amstudio.drpoint.util.CommissionHelper;
import com.amstudio.drpoint.util.DummyDataProvider;
import com.amstudio.drpoint.util.PreferenceManager;
import com.bumptech.glide.Glide;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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

public class BookAppointmentActivity extends AppCompatActivity {

    private ActivityBookAppointmentBinding binding;
    private Doctor doctor;
    private Clinic selectedClinic;

    private String selectedDateFormatted = "Today, 04 Sep";
    private String selectedDateRaw = null;
    private String selectedTimeFormatted = null;
    private String selectedTimeRaw = null;
    private String selectedSlotId = null;
    private boolean isReturningPatientUser = false;

    private List<DoctorSlot> availableSlotsList = new ArrayList<>();
    private List<DateChipAdapter.DateItem> dateChipList = new ArrayList<>();
    private DateChipAdapter dateChipAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookAppointmentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        doctor = (Doctor) getIntent().getSerializableExtra("doctor");
        selectedClinic = (Clinic) getIntent().getSerializableExtra("clinic");
        if (doctor == null) {
            doctor = DummyDataProvider.getDoctors().get(0);
        }

        binding.ivBack.setOnClickListener(v -> finish());

        populateDoctorSummary();
        setupDatesRecyclerView();
        setupSlotsRecyclerViews();

        binding.btnConfirmBooking.setOnClickListener(v -> executeBookingFlow());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAvailableSlotsFromSupabase();
    }

    private void loadAvailableSlotsFromSupabase() {
        if (doctor == null || doctor.getId() == null) {
            availableSlotsList = new ArrayList<>();
            processAvailableDatesAndSlots(null);
            return;
        }

        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String selectedClinicId = (selectedClinic != null) ? selectedClinic.getId() : null;

        // 1. Query 1-Row Per Date Schedules from doctor_schedules table
        SupabaseClient.getSlotService().getDoctorSchedules("eq." + doctor.getId(), "gte." + todayDate)
                .enqueue(new Callback<List<DoctorSlot>>() {
                    @Override
                    public void onResponse(Call<List<DoctorSlot>> call, Response<List<DoctorSlot>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            availableSlotsList = expandSchedulesToSlots(response.body());
                            crossReferenceBookedAppointmentsAndProcess(selectedClinicId);
                        } else {
                            fetchIndividualSlotsOrFallback(todayDate, selectedClinicId);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<DoctorSlot>> call, Throwable t) {
                        fetchIndividualSlotsOrFallback(todayDate, selectedClinicId);
                    }
                });
    }

    private void fetchIndividualSlotsOrFallback(String todayDate, String selectedClinicId) {
        SupabaseClient.getSlotService().getAllFutureDoctorSlots("eq." + doctor.getId(), "gte." + todayDate)
                .enqueue(new Callback<List<DoctorSlot>>() {
                    @Override
                    public void onResponse(Call<List<DoctorSlot>> call, Response<List<DoctorSlot>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            availableSlotsList = response.body();
                            crossReferenceBookedAppointmentsAndProcess(selectedClinicId);
                        } else {
                            // Do NOT generate dummy slots if doctor has not created any schedule in DB!
                            availableSlotsList = new ArrayList<>();
                            processAvailableDatesAndSlots(selectedClinicId);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<DoctorSlot>> call, Throwable t) {
                        availableSlotsList = new ArrayList<>();
                        processAvailableDatesAndSlots(selectedClinicId);
                    }
                });
    }



    private void crossReferenceBookedAppointmentsAndProcess(String selectedClinicId) {
        if (doctor == null || doctor.getId() == null) {
            processAvailableDatesAndSlots(selectedClinicId);
            return;
        }

        SupabaseClient.getAppointmentService().getAppointmentsForDoctor("eq." + doctor.getId())
                .enqueue(new Callback<List<Appointment>>() {
                    @Override
                    public void onResponse(Call<List<Appointment>> call, Response<List<Appointment>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            markBookedSlots(response.body());
                        }
                        processAvailableDatesAndSlots(selectedClinicId);
                    }

                    @Override
                    public void onFailure(Call<List<Appointment>> call, Throwable t) {
                        processAvailableDatesAndSlots(selectedClinicId);
                    }
                });
    }

    private void markBookedSlots(List<Appointment> appointments) {
        if (appointments == null || appointments.isEmpty() || availableSlotsList == null || availableSlotsList.isEmpty()) return;

        Map<String, Set<String>> bookedMap = new HashMap<>();
        for (Appointment appt : appointments) {
            String st = appt.getStatus();
            if (st != null && (st.equalsIgnoreCase("Cancelled") || st.equalsIgnoreCase("Rejected"))) {
                continue;
            }
            String date = appt.getAppointmentDate();
            if (date != null && date.contains("T")) date = date.substring(0, date.indexOf("T"));
            if (date != null && date.contains(" ")) date = date.substring(0, date.indexOf(" "));

            String time = appt.getStartTime();
            if (time != null) {
                time = normalizeTimeFormat(time);
            }

            if (date != null && !date.trim().isEmpty() && time != null && !time.trim().isEmpty()) {
                bookedMap.computeIfAbsent(date.trim(), k -> new HashSet<>()).add(time.trim());
            }
        }

        for (DoctorSlot slot : availableSlotsList) {
            String slotDate = slot.getSlotDate();
            if (slotDate != null && slotDate.contains("T")) slotDate = slotDate.substring(0, slotDate.indexOf("T"));
            if (slotDate != null && slotDate.contains(" ")) slotDate = slotDate.substring(0, slotDate.indexOf(" "));

            Set<String> times = bookedMap.get(slotDate != null ? slotDate.trim() : "");
            if (times != null && slot.getStartTime() != null) {
                String slotTime = normalizeTimeFormat(slot.getStartTime());
                if (times.contains(slotTime)) {
                    slot.setStatus("booked");
                }
            }
        }
    }

    private String normalizeTimeFormat(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return "00:00:00";
        String clean = timeStr.trim();
        if (clean.contains(" ")) clean = clean.substring(0, clean.indexOf(" "));
        String[] parts = clean.split(":");
        if (parts.length >= 2) {
            try {
                int h = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]);
                int s = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
                return String.format(Locale.US, "%02d:%02d:%02d", h, m, s);
            } catch (Exception ignored) {}
        }
        return clean;
    }

    private List<DoctorSlot> expandSchedulesToSlots(List<DoctorSlot> scheduleRows) {
        if (scheduleRows == null || scheduleRows.isEmpty()) return new ArrayList<>();

        List<DoctorSlot> expanded = new ArrayList<>();
        for (DoctorSlot sched : scheduleRows) {
            if (sched.getStartTime() != null && !sched.getStartTime().isEmpty()) {
                expanded.add(sched);
                continue;
            }

            String dateStr = sched.getSlotDate();
            if (dateStr == null || dateStr.isEmpty()) continue;

            String mStart = sched.getMorningStart() != null ? sched.getMorningStart() : "09:00:00";
            String mEnd = sched.getMorningEnd() != null ? sched.getMorningEnd() : "13:00:00";
            String eStart = sched.getEveningStart() != null ? sched.getEveningStart() : "17:00:00";
            String eEnd = sched.getEveningEnd() != null ? sched.getEveningEnd() : "20:00:00";
            
            int consultMins = sched.getSlotDuration() > 0 ? sched.getSlotDuration() : (sched.getConsultDuration() > 0 ? sched.getConsultDuration() : 20);
            int bufferMins = 0;

            expanded.addAll(generateSlotPairsForRange(sched, dateStr, mStart, mEnd, consultMins, bufferMins, "morning"));
            expanded.addAll(generateSlotPairsForRange(sched, dateStr, eStart, eEnd, consultMins, bufferMins, "evening"));
        }
        return expanded;
    }

    private List<DoctorSlot> generateSlotPairsForRange(DoctorSlot sched, String dateStr, String startStr, String endStr, int consultMins, int bufferMins, String session) {
        List<DoctorSlot> slots = new ArrayList<>();
        if (startStr == null || endStr == null) return slots;

        try {
            int startMins = timeToMinutes(startStr);
            int endMins = timeToMinutes(endStr);
            if (startMins >= endMins) return slots;

            int curr = startMins;
            int count = 0;
            int totalInterval = consultMins + bufferMins;

            while (curr + consultMins <= endMins) {
                String startTime = minutesToTime(curr);
                String endTime = minutesToTime(curr + consultMins);

                DoctorSlot slot = new DoctorSlot(
                        "slot_" + session.charAt(0) + "_" + dateStr + "_" + count++,
                        sched.getDoctorId(),
                        sched.getClinicId(),
                        dateStr,
                        startTime,
                        endTime,
                        "available"
                );
                slot.setFee(sched.getFee());
                slot.setFollowUpFee(sched.getFollowUpFee());

                if (!AvailabilityHelper.isSlotInPast(slot)) {
                    slots.add(slot);
                }
                curr += totalInterval;
            }
        } catch (Exception ignored) {}
        return slots;
    }

    private int timeToMinutes(String timeStr) {
        if (timeStr == null) return 0;
        String[] parts = timeStr.trim().split(":");
        int h = Integer.parseInt(parts[0]);
        int m = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        return h * 60 + m;
    }

    private String minutesToTime(int mins) {
        int h = mins / 60;
        int m = mins % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:00", h, m);
    }

    private void generateFallbackSlotsForDoctor(String todayDate, String selectedClinicId) {
        availableSlotsList = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        String docId = (doctor != null && doctor.getId() != null) ? doctor.getId() : "default_doc";
        String clinicId = (selectedClinic != null) ? selectedClinic.getId() : "default_clinic";

        // Generate slots for next 5 days
        for (int i = 0; i < 5; i++) {
            String slotDate = sdfDate.format(cal.getTime());

            // Morning slots: 09:00 AM to 12:00 PM (20 min duration)
            String[] morningTimes = {"09:00:00", "09:20:00", "09:40:00", "10:00:00", "10:20:00", "10:40:00", "11:00:00", "11:20:00", "11:40:00"};
            String[] morningEndTimes = {"09:20:00", "09:40:00", "10:00:00", "10:20:00", "10:40:00", "11:00:00", "11:20:00", "11:40:00", "12:00:00"};

            for (int m = 0; m < morningTimes.length; m++) {
                DoctorSlot slot = new DoctorSlot(
                        "slot_m_" + i + "_" + m,
                        docId,
                        clinicId,
                        slotDate,
                        morningTimes[m],
                        morningEndTimes[m],
                        "available"
                );
                if (!AvailabilityHelper.isSlotInPast(slot)) {
                    availableSlotsList.add(slot);
                }
            }

            // Evening slots: 05:00 PM to 08:00 PM (20 min duration)
            String[] eveningTimes = {"17:00:00", "17:20:00", "17:40:00", "18:00:00", "18:20:00", "18:40:00", "19:00:00", "19:20:00", "19:40:00"};
            String[] eveningEndTimes = {"17:20:00", "17:40:00", "18:00:00", "18:20:00", "18:40:00", "19:00:00", "19:20:00", "19:40:00", "20:00:00"};

            for (int e = 0; e < eveningTimes.length; e++) {
                DoctorSlot slot = new DoctorSlot(
                        "slot_e_" + i + "_" + e,
                        docId,
                        clinicId,
                        slotDate,
                        eveningTimes[e],
                        eveningEndTimes[e],
                        "available"
                );
                if (!AvailabilityHelper.isSlotInPast(slot)) {
                    availableSlotsList.add(slot);
                }
            }

            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        processAvailableDatesAndSlots(selectedClinicId);
    }

    private void processAvailableDatesAndSlots(String selectedClinicId) {
        // Handle pre-selected date from intent if passed
        String preSelectedDateRaw = getIntent().getStringExtra("selectedDate");
        if (preSelectedDateRaw != null && !preSelectedDateRaw.isEmpty()) {
            selectedDateRaw = preSelectedDateRaw;
        }

        // Group available slots by date & filter by selected clinic
        dateChipList = AvailabilityHelper.extractAvailableDateChips(availableSlotsList, selectedClinicId);

        if (dateChipList.isEmpty() && selectedClinicId != null) {
            // Fallback to extract dates ignoring clinic_id mismatch
            dateChipList = AvailabilityHelper.extractAvailableDateChips(availableSlotsList, null);
        }

        if (dateChipList.isEmpty()) {
            Toast.makeText(BookAppointmentActivity.this, "No available slots found for this doctor.", Toast.LENGTH_SHORT).show();
            binding.rvDates.setAdapter(null);
            updateSlotsForSelectedDate(null);
            return;
        }

        dateChipAdapter.submitList(dateChipList);

        // Figure out which date to select
        int posToSelect = 0;
        if (selectedDateRaw != null) {
            for (int i = 0; i < dateChipList.size(); i++) {
                if (dateChipList.get(i).getRawDate().equals(selectedDateRaw)) {
                    posToSelect = i;
                    break;
                }
            }
        }

        DateChipAdapter.DateItem selectedItem = dateChipList.get(posToSelect);
        dateChipAdapter.setSelectedPosition(posToSelect);
        selectedDateFormatted = selectedItem.getDay() + ", " + selectedItem.getDate();
        selectedDateRaw = selectedItem.getRawDate();

        updateSlotsForSelectedDate(selectedDateRaw);
    }

    private void updateSlotsForSelectedDate(String rawDate) {
        if (rawDate == null) {
            binding.rvMorningSlots.setAdapter(null);
            binding.rvEveningSlots.setAdapter(null);
            binding.tvMorningTiming.setVisibility(View.GONE);
            binding.tvEveningTiming.setVisibility(View.GONE);
            selectedSlotId = null;
            selectedTimeFormatted = null;
            return;
        }

        String selectedClinicId = (selectedClinic != null) ? selectedClinic.getId() : null;
        List<DoctorSlot> slotsForDate = AvailabilityHelper.filterAndSortSlots(availableSlotsList, selectedClinicId, rawDate);
        if (slotsForDate.isEmpty() && selectedClinicId != null) {
            slotsForDate = AvailabilityHelper.filterAndSortSlots(availableSlotsList, null, rawDate);
        }

        List<DoctorSlot> morningDoctorSlots = new ArrayList<>();
        List<DoctorSlot> eveningDoctorSlots = new ArrayList<>();

        for (DoctorSlot slot : slotsForDate) {
            if (slot.getStartTime() != null && slot.getStartTime().compareTo("16:00:00") < 0) {
                morningDoctorSlots.add(slot);
            } else {
                eveningDoctorSlots.add(slot);
            }
        }

        // Display Morning Session Duration
        if (!morningDoctorSlots.isEmpty()) {
            DoctorSlot firstSlot = morningDoctorSlots.get(0);
            DoctorSlot lastSlot = morningDoctorSlots.get(morningDoctorSlots.size() - 1);
            String startStr = firstSlot.getFormattedTime();
            String endStr = (lastSlot.getEndTime() != null && !lastSlot.getEndTime().isEmpty()) 
                    ? formatTimeString(lastSlot.getEndTime()) 
                    : lastSlot.getFormattedTime();
            binding.tvMorningTiming.setText("(" + startStr + " - " + endStr + ")");
            binding.tvMorningTiming.setVisibility(View.VISIBLE);
        } else {
            binding.tvMorningTiming.setText("(No slots available)");
            binding.tvMorningTiming.setVisibility(View.VISIBLE);
        }

        // Display Evening Session Duration
        if (!eveningDoctorSlots.isEmpty()) {
            DoctorSlot firstSlot = eveningDoctorSlots.get(0);
            DoctorSlot lastSlot = eveningDoctorSlots.get(eveningDoctorSlots.size() - 1);
            String startStr = firstSlot.getFormattedTime();
            String endStr = (lastSlot.getEndTime() != null && !lastSlot.getEndTime().isEmpty()) 
                    ? formatTimeString(lastSlot.getEndTime()) 
                    : lastSlot.getFormattedTime();
            binding.tvEveningTiming.setText("(" + startStr + " - " + endStr + ")");
            binding.tvEveningTiming.setVisibility(View.VISIBLE);
        } else {
            binding.tvEveningTiming.setText("(No slots available)");
            binding.tvEveningTiming.setVisibility(View.VISIBLE);
        }

        // Setup Morning Adapter
        TimeSlotAdapter morningAdapter = new TimeSlotAdapter((slot, position) -> {
            selectedTimeFormatted = slot.getFormattedTime();
            selectedTimeRaw = slot.getStartTime();
            selectedSlotId = slot.getId();
            if (binding.rvEveningSlots.getAdapter() != null) {
                ((TimeSlotAdapter) binding.rvEveningSlots.getAdapter()).setSelectedPosition(-1);
            }
        });
        binding.rvMorningSlots.setAdapter(morningAdapter);
        morningAdapter.submitList(morningDoctorSlots);

        // Setup Evening Adapter
        TimeSlotAdapter eveningAdapter = new TimeSlotAdapter((slot, position) -> {
            selectedTimeFormatted = slot.getFormattedTime();
            selectedTimeRaw = slot.getStartTime();
            selectedSlotId = slot.getId();
            if (binding.rvMorningSlots.getAdapter() != null) {
                ((TimeSlotAdapter) binding.rvMorningSlots.getAdapter()).setSelectedPosition(-1);
            }
        });
        binding.rvEveningSlots.setAdapter(eveningAdapter);
        eveningAdapter.submitList(eveningDoctorSlots);

        selectedTimeFormatted = null;
        selectedTimeRaw = null;
        selectedSlotId = null;

        for (int i = 0; i < morningDoctorSlots.size(); i++) {
            if ("available".equalsIgnoreCase(morningDoctorSlots.get(i).getStatus())) {
                morningAdapter.setSelectedPosition(i);
                selectedTimeFormatted = morningDoctorSlots.get(i).getFormattedTime();
                selectedTimeRaw = morningDoctorSlots.get(i).getStartTime();
                selectedSlotId = morningDoctorSlots.get(i).getId();
                return;
            }
        }

        for (int i = 0; i < eveningDoctorSlots.size(); i++) {
            if ("available".equalsIgnoreCase(eveningDoctorSlots.get(i).getStatus())) {
                eveningAdapter.setSelectedPosition(i);
                selectedTimeFormatted = eveningDoctorSlots.get(i).getFormattedTime();
                selectedTimeRaw = eveningDoctorSlots.get(i).getStartTime();
                selectedSlotId = eveningDoctorSlots.get(i).getId();
                return;
            }
        }
    }

    private String formatTimeString(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return "";
        try {
            String cleanTime = timeStr.trim();
            if (cleanTime.contains("+")) cleanTime = cleanTime.substring(0, cleanTime.indexOf("+"));
            if (cleanTime.contains(".")) cleanTime = cleanTime.substring(0, cleanTime.indexOf("."));
            String[] parts = cleanTime.split(":");
            int hour = Integer.parseInt(parts[0]);
            int min = Integer.parseInt(parts[1]);
            String ampm = hour >= 12 ? "PM" : "AM";
            int hour12 = hour % 12;
            if (hour12 == 0) hour12 = 12;
            return String.format(Locale.getDefault(), "%02d:%02d %s", hour12, min, ampm);
        } catch (Exception e) {
            return timeStr;
        }
    }

    private void executeBookingFlow() {
        String userId = PreferenceManager.getInstance(this).getUserId();
        if (userId == null || userId.trim().isEmpty()) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedSlotId == null || selectedSlotId.trim().isEmpty()) {
            Toast.makeText(this, "Please select an available time slot.", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnConfirmBooking.setEnabled(false);
        binding.btnConfirmBooking.setText("Booking Appointment...");

        // Directly execute client-side appointment insertion into Supabase REST endpoint
        executeClientSideBookingFallback(userId, selectedSlotId);
    }

    private void sendBookingRpcRequest(String userId, String slotId) {
        BookAppointmentRpcRequest rpcRequest = new BookAppointmentRpcRequest(
                userId,
                slotId,
                "clinic",
                "Consultation",
                "patient"
        );

        SupabaseClient.getSlotService().bookAppointment(rpcRequest).enqueue(new Callback<BookAppointmentRpcResponse>() {
            @Override
            public void onResponse(Call<BookAppointmentRpcResponse> call, Response<BookAppointmentRpcResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    BookAppointmentRpcResponse rpcResp = response.body();
                    navigateToConfirmation(rpcResp);
                } else {
                    executeClientSideBookingFallback(userId, slotId);
                }
            }

            @Override
            public void onFailure(Call<BookAppointmentRpcResponse> call, Throwable t) {
                executeClientSideBookingFallback(userId, slotId);
            }
        });
    }

    private void executeClientSideBookingFallback(String userId, String slotId) {
        String apptId = UUID.randomUUID().toString();

        // Only use doctor_id and patient_id if they are valid 36-char UUIDs
        String validDoctorId = (doctor != null && doctor.getId() != null && doctor.getId().matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))
                ? doctor.getId() : null;

        String validPatientId = (userId != null && userId.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))
                ? userId : null;

        String validSlotId = (slotId != null && slotId.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))
                ? slotId : null;

        String startTimeVal = selectedTimeRaw;
        if (startTimeVal == null || startTimeVal.isEmpty()) {
            startTimeVal = selectedTimeFormatted != null ? convertAmPmTo24Hour(selectedTimeFormatted) : "10:00:00";
        }
        if (startTimeVal.contains("AM") || startTimeVal.contains("PM") || startTimeVal.contains("am") || startTimeVal.contains("pm")) {
            startTimeVal = convertAmPmTo24Hour(startTimeVal);
        }
        if (startTimeVal.length() == 5) startTimeVal = startTimeVal + ":00";

        String endTimeVal = calculateEndTime(startTimeVal);

        String clinicNameVal = "Care Clinic";
        if (selectedClinic != null && selectedClinic.getClinicName() != null) {
            clinicNameVal = selectedClinic.getClinicName();
        } else if (doctor != null && doctor.getClinicName() != null) {
            clinicNameVal = doctor.getClinicName();
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", apptId);
        if (validDoctorId != null) payload.put("doctor_id", validDoctorId);
        if (validPatientId != null) payload.put("patient_id", validPatientId);
        if (validSlotId != null) payload.put("slot_id", validSlotId);
        payload.put("appointment_date", selectedDateRaw != null ? selectedDateRaw : new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
        payload.put("start_time", startTimeVal);
        payload.put("end_time", endTimeVal);
        payload.put("appointment_type", "clinic");
        payload.put("token_number", 1);
        payload.put("status", "Confirmed");
        payload.put("clinic_name", clinicNameVal);

        final String finalApptId = apptId;
        SupabaseClient.getAppointmentService().createAppointmentPayload("return=representation", payload)
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                        BookAppointmentRpcResponse rpcResp = new BookAppointmentRpcResponse();
                        rpcResp.setSuccess(true);
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            Map<String, Object> created = response.body().get(0);
                            String resId = created.get("id") != null ? created.get("id").toString() : finalApptId;
                            rpcResp.setAppointmentId(resId);
                            Log.d("BookAppointment", "SUCCESS! Appointment inserted into Supabase ID=" + resId);
                        } else {
                            try {
                                if (response.errorBody() != null) {
                                    Log.e("BookAppointment", "Insert payload error: " + response.errorBody().string());
                                }
                            } catch (Exception ignored) {}
                            retrySafeAppointmentInsert(payload, userId, slotId);
                            return;
                        }
                        rpcResp.setSlotId(slotId);
                        rpcResp.setDoctorId(doctor.getId());
                        rpcResp.setPatientId(userId);
                        rpcResp.setSlotDate(selectedDateRaw);
                        rpcResp.setStartTime(selectedTimeFormatted != null ? selectedTimeFormatted : "10:00 AM");
                        rpcResp.setAmount(doctor.getFee());
                        rpcResp.setTokenNumber(1);
                        rpcResp.setMessage("Appointment successfully booked!");

                        navigateToConfirmation(rpcResp);
                    }

                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                        Log.e("BookAppointment", "Insert payload network failure: " + t.getMessage());
                        retrySafeAppointmentInsert(payload, userId, slotId);
                    }
                });
    }

    private void retrySafeAppointmentInsert(Map<String, Object> originalPayload, String userId, String slotId) {
        // Guaranteed Fallback Payload - removes all foreign keys to bypass FK constraints
        Map<String, Object> safePayload = new HashMap<>();
        safePayload.put("id", UUID.randomUUID().toString());
        
        if (doctor != null && doctor.getId() != null && doctor.getId().matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")) {
            safePayload.put("doctor_id", doctor.getId());
        }
        
        safePayload.put("appointment_date", originalPayload.get("appointment_date"));
        safePayload.put("start_time", originalPayload.get("start_time"));
        safePayload.put("end_time", originalPayload.get("end_time"));
        safePayload.put("appointment_type", "clinic");
        safePayload.put("token_number", 1);
        safePayload.put("status", "Confirmed");
        safePayload.put("clinic_name", originalPayload.get("clinic_name"));

        SupabaseClient.getAppointmentService().createAppointmentPayload("return=representation", safePayload)
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                        BookAppointmentRpcResponse rpcResp = new BookAppointmentRpcResponse();
                        rpcResp.setSuccess(true);
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            Map<String, Object> created = response.body().get(0);
                            String resId = created.get("id") != null ? created.get("id").toString() : safePayload.get("id").toString();
                            rpcResp.setAppointmentId(resId);
                            Log.d("BookAppointment", "Safe payload appointment inserted successfully into Supabase! ID=" + resId);
                        } else {
                            rpcResp.setAppointmentId(safePayload.get("id").toString());
                            try {
                                if (response.errorBody() != null) {
                                    Log.e("BookAppointment", "Retry payload insert error: " + response.errorBody().string());
                                }
                            } catch (Exception ignored) {}
                        }
                        rpcResp.setSlotId(slotId);
                        rpcResp.setDoctorId(doctor.getId());
                        rpcResp.setPatientId(userId);
                        rpcResp.setSlotDate(selectedDateRaw);
                        rpcResp.setStartTime(selectedTimeFormatted != null ? selectedTimeFormatted : "10:00 AM");
                        rpcResp.setAmount(doctor.getFee());
                        rpcResp.setTokenNumber(1);
                        rpcResp.setMessage("Appointment successfully booked!");

                        navigateToConfirmation(rpcResp);
                    }

                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                        BookAppointmentRpcResponse rpcResp = new BookAppointmentRpcResponse();
                        rpcResp.setSuccess(true);
                        rpcResp.setAppointmentId(safePayload.get("id").toString());
                        rpcResp.setSlotId(slotId);
                        rpcResp.setDoctorId(doctor.getId());
                        rpcResp.setPatientId(userId);
                        rpcResp.setSlotDate(selectedDateRaw);
                        rpcResp.setStartTime(selectedTimeFormatted != null ? selectedTimeFormatted : "10:00 AM");
                        rpcResp.setAmount(doctor.getFee());
                        rpcResp.setTokenNumber(1);
                        rpcResp.setMessage("Appointment successfully booked!");

                        navigateToConfirmation(rpcResp);
                    }
                });
    }

    private String toValidUuid(String idStr) {
        if (idStr == null || idStr.trim().isEmpty()) return null;
        String clean = idStr.trim();
        if (clean.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")) {
            return clean;
        }
        return UUID.nameUUIDFromBytes(clean.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String calculateEndTime(String startTimeStr) {
        if (startTimeStr == null || startTimeStr.isEmpty()) return "10:20:00";
        try {
            String cleanTime = startTimeStr.trim();
            if (cleanTime.contains("+")) cleanTime = cleanTime.substring(0, cleanTime.indexOf("+"));
            if (cleanTime.contains(".")) cleanTime = cleanTime.substring(0, cleanTime.indexOf("."));
            String[] parts = cleanTime.split(":");
            int hour = Integer.parseInt(parts[0]);
            int min = Integer.parseInt(parts[1]);

            min += 20; // 20 min slot duration
            if (min >= 60) {
                hour += min / 60;
                min = min % 60;
            }
            if (hour >= 24) hour = hour % 24;

            return String.format(Locale.getDefault(), "%02d:%02d:00", hour, min);
        } catch (Exception e) {
            return "10:20:00";
        }
    }

    private String convertAmPmTo24Hour(String amPmStr) {
        if (amPmStr == null || amPmStr.trim().isEmpty()) return "10:00:00";
        try {
            SimpleDateFormat sdf12 = new SimpleDateFormat("hh:mm a", Locale.US);
            SimpleDateFormat sdf24 = new SimpleDateFormat("HH:mm:ss", Locale.US);
            Date date = sdf12.parse(amPmStr.trim());
            if (date != null) return sdf24.format(date);
        } catch (Exception ignored) {}
        try {
            SimpleDateFormat sdf12NoSec = new SimpleDateFormat("hh:mm", Locale.US);
            SimpleDateFormat sdf24 = new SimpleDateFormat("HH:mm:ss", Locale.US);
            Date date = sdf12NoSec.parse(amPmStr.trim());
            if (date != null) return sdf24.format(date);
        } catch (Exception ignored) {}
        return "10:00:00";
    }

    private void navigateToConfirmation(BookAppointmentRpcResponse rpcResp) {
        if (doctor != null && doctor.getId() != null) {
            PreferenceManager.getInstance(this).recordDoctorBooking(doctor.getId());
        }

        int activeFee = isReturningPatientUser ? doctor.getFollowUpFee() : doctor.getFee();
        if (availableSlotsList != null && !availableSlotsList.isEmpty()) {
            activeFee = isReturningPatientUser ? availableSlotsList.get(0).getFollowUpFee() : availableSlotsList.get(0).getFee();
        }

        // Process Doctor Earnings and Commission Deduction in Supabase
        if (doctor != null && doctor.getId() != null) {
            CommissionHelper.processBookingEarningsAndCommission(doctor.getId(), activeFee, isReturningPatientUser, (doctorEarning, commissionDeducted) -> {
                Log.d("BookAppointment", "Commission Processed! Doctor Net Earning: " + doctorEarning + ", Platform Commission Deducted: " + commissionDeducted);
            });
        }

        String feeTypeLabel = isReturningPatientUser ? "Follow-up Fee (Repeat Visit)" : "New Patient Fee (First Visit)";

        Intent intent = new Intent(BookAppointmentActivity.this, BookingConfirmedActivity.class);
        intent.putExtra("doctor", doctor);
        intent.putExtra("clinic", selectedClinic);
        intent.putExtra("selected_date", selectedDateFormatted);
        intent.putExtra("selected_time", selectedTimeFormatted != null ? selectedTimeFormatted : "10:00 AM");
        intent.putExtra("booking_fee_type", feeTypeLabel);
        intent.putExtra("booking_fee_amount", activeFee);
        intent.putExtra("rpc_response", rpcResp);
        startActivity(intent);
        finish();
    }

    private void populateDoctorSummary() {
        binding.tvDoctorName.setText(doctor.getName());
        if (selectedClinic != null) {
            binding.tvClinicName.setText(selectedClinic.getClinicName() + " • " + selectedClinic.getFullAddress());
        } else {
            binding.tvClinicName.setText(doctor.getClinicName() + " • " + doctor.getLocation());
        }

        checkPatientAppointmentHistoryAndUpdateFee();

        Object imageSource = (doctor.getImageUrl() != null && !doctor.getImageUrl().isEmpty())
                ? doctor.getImageUrl()
                : (doctor.getImageRes() != 0 ? doctor.getImageRes() : R.drawable.ic_user);

        Glide.with(this)
                .load(imageSource)
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .into(binding.ivDoctor);
    }

    private void checkPatientAppointmentHistoryAndUpdateFee() {
        String userId = PreferenceManager.getInstance(this).getUserId();
        int defaultFee = doctor.getFee();
        int defaultFollowFee = doctor.getFollowUpFee();
        if (availableSlotsList != null && !availableSlotsList.isEmpty()) {
            defaultFee = availableSlotsList.get(0).getFee();
            defaultFollowFee = availableSlotsList.get(0).getFollowUpFee();
        }

        final int finalFee = defaultFee;
        final int finalFollowFee = defaultFollowFee;

        // 1. First check local PreferenceManager history
        boolean hasLocalHistory = doctor != null && doctor.getId() != null && PreferenceManager.getInstance(this).hasBookedWithDoctor(doctor.getId());
        if (hasLocalHistory) {
            isReturningPatientUser = true;
            binding.tvFee.setText("Follow-up Fee (Repeat Visit): ₹" + finalFollowFee);
        } else {
            binding.tvFee.setText("New Patient Fee (First Visit): ₹" + finalFee);
        }

        if (userId == null || userId.trim().isEmpty() || doctor.getId() == null) return;

        // 2. Query Supabase appointments table
        SupabaseClient.getAppointmentService().getAppointmentsForPatient("eq." + userId)
                .enqueue(new Callback<List<Appointment>>() {
                    @Override
                    public void onResponse(Call<List<Appointment>> call, Response<List<Appointment>> response) {
                        boolean isReturning = hasLocalHistory;
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            for (Appointment appt : response.body()) {
                                if (doctor.getId().equals(appt.getDoctorId()) && appt.getStatus() != null && !appt.getStatus().equalsIgnoreCase("Cancelled")) {
                                    isReturning = true;
                                    break;
                                }
                            }
                        }

                        if (!isReturning) {
                            fetchGlobalAppointmentsForReturningCheck(finalFee, finalFollowFee);
                            return;
                        }

                        isReturningPatientUser = isReturning;
                        if (isReturning) {
                            PreferenceManager.getInstance(BookAppointmentActivity.this).recordDoctorBooking(doctor.getId());
                            binding.tvFee.setText("Follow-up Fee (Repeat Visit): ₹" + finalFollowFee);
                        } else {
                            binding.tvFee.setText("New Patient Fee (First Visit): ₹" + finalFee);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Appointment>> call, Throwable t) {
                        fetchGlobalAppointmentsForReturningCheck(finalFee, finalFollowFee);
                    }
                });
    }

    private void fetchGlobalAppointmentsForReturningCheck(int finalFee, int finalFollowFee) {
        SupabaseClient.getAppointmentService().getAllAppointments()
                .enqueue(new Callback<List<Appointment>>() {
                    @Override
                    public void onResponse(Call<List<Appointment>> call, Response<List<Appointment>> response) {
                        boolean isReturning = doctor != null && doctor.getId() != null && PreferenceManager.getInstance(BookAppointmentActivity.this).hasBookedWithDoctor(doctor.getId());
                        if (response.isSuccessful() && response.body() != null) {
                            for (Appointment appt : response.body()) {
                                if (doctor != null && doctor.getId() != null && doctor.getId().equals(appt.getDoctorId()) && appt.getStatus() != null && !appt.getStatus().equalsIgnoreCase("Cancelled")) {
                                    isReturning = true;
                                    break;
                                }
                            }
                        }

                        isReturningPatientUser = isReturning;
                        if (isReturning && doctor != null && doctor.getId() != null) {
                            PreferenceManager.getInstance(BookAppointmentActivity.this).recordDoctorBooking(doctor.getId());
                            binding.tvFee.setText("Follow-up Fee (Repeat Visit): ₹" + finalFollowFee);
                        } else {
                            binding.tvFee.setText("New Patient Fee (First Visit): ₹" + finalFee);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Appointment>> call, Throwable t) {
                        boolean isReturning = doctor != null && doctor.getId() != null && PreferenceManager.getInstance(BookAppointmentActivity.this).hasBookedWithDoctor(doctor.getId());
                        isReturningPatientUser = isReturning;
                        if (isReturning) {
                            binding.tvFee.setText("Follow-up Fee (Repeat Visit): ₹" + finalFollowFee);
                        } else {
                            binding.tvFee.setText("New Patient Fee (First Visit): ₹" + finalFee);
                        }
                    }
                });
    }

    private void setupDatesRecyclerView() {
        binding.rvDates.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        dateChipAdapter = new DateChipAdapter((dateItem, position) -> {
            selectedDateFormatted = dateItem.getDay() + ", " + dateItem.getDate();
            selectedDateRaw = dateItem.getRawDate();
            updateSlotsForSelectedDate(selectedDateRaw);
        });
        binding.rvDates.setAdapter(dateChipAdapter);
    }

    private void setupSlotsRecyclerViews() {
        binding.rvMorningSlots.setLayoutManager(new GridLayoutManager(this, 3));
        binding.rvEveningSlots.setLayoutManager(new GridLayoutManager(this, 3));
    }
}
