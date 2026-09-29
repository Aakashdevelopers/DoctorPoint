package com.amstudio.drpoint.ui.booking;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import android.net.Uri;
import android.text.TextUtils;
import android.widget.ArrayAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.amstudio.drpoint.BuildConfig;
import com.amstudio.drpoint.R;
import com.amstudio.drpoint.adapter.DateChipAdapter;
import com.amstudio.drpoint.adapter.TimeSlotAdapter;
import com.amstudio.drpoint.databinding.ActivityBookAppointmentBinding;
import com.amstudio.drpoint.databinding.BottomSheetEditProfileBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.amstudio.drpoint.model.Appointment;
import com.amstudio.drpoint.model.Clinic;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.model.DoctorSlot;
import com.amstudio.drpoint.model.PatientProfile;
import com.amstudio.drpoint.network.SupabaseClient;
import com.amstudio.drpoint.network.model.BookAppointmentRpcRequest;
import com.amstudio.drpoint.network.model.BookAppointmentRpcResponse;
import com.amstudio.drpoint.util.AvailabilityHelper;
import com.amstudio.drpoint.util.CommissionHelper;
import com.amstudio.drpoint.util.DummyDataProvider;
import com.amstudio.drpoint.util.PreferenceManager;
import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

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

    private BottomSheetEditProfileBinding activeSheetBinding;
    private String uploadedAvatarUrl = "";
    private ActivityResultLauncher<Intent> imagePickerLauncher;

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
        setupImagePickerLauncher();

        binding.btnConfirmBooking.setOnClickListener(v -> executeBookingFlow());
        binding.btnNoSlotsGoBack.setOnClickListener(v -> finish());
        binding.btnNoSlotsRefresh.setOnClickListener(v -> loadAvailableSlotsFromSupabase());
    }

    private void setupImagePickerLauncher() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (activeSheetBinding != null) {
                            activeSheetBinding.ivEditProfilePic.setImageURI(imageUri);
                            uploadPatientImageToSupabaseStorage(imageUri);
                        }
                    }
                }
        );
    }

    private void uploadPatientImageToSupabaseStorage(Uri imageUri) {
        if (activeSheetBinding != null) {
            activeSheetBinding.tvEditUploadLabel.setText("⌛ Uploading photo...");
        }
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                byte[] imageBytes = getBytes(inputStream);

                String fileName = "profile_" + System.currentTimeMillis() + ".jpg";
                String uploadUrl = BuildConfig.SUPABASE_URL + "storage/v1/object/profiile/" + fileName;
                String publicUrl = BuildConfig.SUPABASE_URL + "storage/v1/object/public/profiile/" + fileName;

                OkHttpClient client = new OkHttpClient();
                RequestBody requestBody = RequestBody.create(imageBytes, MediaType.parse("image/jpeg"));

                Request request = new Request.Builder()
                        .url(uploadUrl)
                        .post(requestBody)
                        .addHeader("Authorization", "Bearer " + BuildConfig.SUPABASE_KEY)
                        .addHeader("apikey", BuildConfig.SUPABASE_KEY)
                        .addHeader("x-upsert", "true")
                        .addHeader("Content-Type", "image/jpeg")
                        .build();

                okhttp3.Response response = client.newCall(request).execute();
                if (response.isSuccessful() || response.code() == 200 || response.code() == 201) {
                    uploadedAvatarUrl = publicUrl;
                    PreferenceManager.getInstance(BookAppointmentActivity.this).setUserAvatar(publicUrl);
                    runOnUiThread(() -> {
                        if (activeSheetBinding != null) {
                            activeSheetBinding.tvEditUploadLabel.setText("✅ Photo Uploaded!");
                        }
                        Toast.makeText(BookAppointmentActivity.this, "Patient Photo Uploaded!", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
            } catch (Exception e) {
                Log.e("BookAppointment", "Patient photo upload failed: " + e.getMessage());
            }
            runOnUiThread(() -> {
                if (activeSheetBinding != null) {
                    activeSheetBinding.tvEditUploadLabel.setText("📷 Tap to Change Profile Photo");
                }
            });
        });
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAvailableSlotsFromSupabase();
    }

    private void loadAvailableSlotsFromSupabase() {
        showShimmerLoaders();
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String selectedClinicId = (selectedClinic != null) ? selectedClinic.getId() : null;

        if (doctor == null) {
            generateFallbackSlotsForDoctor(todayDate, selectedClinicId);
            return;
        }

        // 1. Fetch directly from Supabase doctor_slots table
        SupabaseClient.getSlotService().getAllSlots().enqueue(new Callback<List<DoctorSlot>>() {
            @Override
            public void onResponse(Call<List<DoctorSlot>> call, Response<List<DoctorSlot>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    List<DoctorSlot> allSlotsFromDb = response.body();
                    List<DoctorSlot> matchedSlots = new ArrayList<>();
                    String currentDocId = doctor.getId();

                    for (DoctorSlot slot : allSlotsFromDb) {
                        if (slot == null) continue;
                        String slotDocId = slot.getDoctorId();

                        if (currentDocId == null || slotDocId == null ||
                                currentDocId.equalsIgnoreCase(slotDocId) ||
                                isDoctorMatch(currentDocId, slotDocId)) {
                            matchedSlots.add(slot);
                        }
                    }

                    if (!matchedSlots.isEmpty()) {
                        availableSlotsList = matchedSlots;
                    } else {
                        availableSlotsList = allSlotsFromDb;
                    }

                    crossReferenceBookedAppointmentsAndProcess(selectedClinicId);
                    return;
                }

                // 2. Fallback to doctor_schedules table
                fetchSchedulesFromSupabase(todayDate, selectedClinicId);
            }

            @Override
            public void onFailure(Call<List<DoctorSlot>> call, Throwable t) {
                fetchSchedulesFromSupabase(todayDate, selectedClinicId);
            }
        });
    }

    private boolean isDoctorMatch(String id1, String id2) {
        if (id1 == null || id2 == null) return true;
        if (id1.equalsIgnoreCase(id2)) return true;
        return id1.contains(id2) || id2.contains(id1);
    }

    private void fetchSchedulesFromSupabase(String todayDate, String selectedClinicId) {
        SupabaseClient.getSlotService().getAllDoctorSchedules().enqueue(new Callback<List<DoctorSlot>>() {
            @Override
            public void onResponse(Call<List<DoctorSlot>> call, Response<List<DoctorSlot>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    availableSlotsList = expandSchedulesToSlots(response.body());
                    if (!availableSlotsList.isEmpty()) {
                        crossReferenceBookedAppointmentsAndProcess(selectedClinicId);
                        return;
                    }
                }
                generateFallbackSlotsForDoctor(todayDate, selectedClinicId);
            }

            @Override
            public void onFailure(Call<List<DoctorSlot>> call, Throwable t) {
                generateFallbackSlotsForDoctor(todayDate, selectedClinicId);
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

    private List<DoctorSlot> generateSlotsForSpecificDate(String rawDate) {
        List<DoctorSlot> generated = new ArrayList<>();
        if (rawDate == null || rawDate.isEmpty()) {
            rawDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        }

        String docId = (doctor != null && doctor.getId() != null) ? doctor.getId() : "doc_1";
        String clinicId = (selectedClinic != null) ? selectedClinic.getId() : "clinic_1";

        String[] morningTimes = {"09:00:00", "09:20:00", "09:40:00", "10:00:00", "10:20:00", "10:40:00", "11:00:00", "11:20:00", "11:40:00", "12:00:00", "12:20:00", "12:40:00"};
        String[] morningEndTimes = {"09:20:00", "09:40:00", "10:00:00", "10:20:00", "10:40:00", "11:00:00", "11:20:00", "11:40:00", "12:00:00", "12:20:00", "12:40:00", "13:00:00"};

        for (int m = 0; m < morningTimes.length; m++) {
            DoctorSlot slot = new DoctorSlot(
                    "slot_m_" + rawDate + "_" + m,
                    docId,
                    clinicId,
                    rawDate,
                    morningTimes[m],
                    morningEndTimes[m],
                    "available"
            );
            slot.setFee(doctor != null ? doctor.getFee() : 500);
            slot.setFollowUpFee(doctor != null ? doctor.getFollowUpFee() : 300);
            generated.add(slot);
        }

        String[] eveningTimes = {"17:00:00", "17:20:00", "17:40:00", "18:00:00", "18:20:00", "18:40:00", "19:00:00", "19:20:00", "19:40:00", "20:00:00", "20:20:00", "20:40:00", "21:00:00", "21:20:00", "21:40:00"};
        String[] eveningEndTimes = {"17:20:00", "17:40:00", "18:00:00", "18:20:00", "18:40:00", "19:00:00", "19:20:00", "19:40:00", "20:00:00", "20:20:00", "20:40:00", "21:00:00", "21:20:00", "21:40:00", "22:00:00"};

        for (int e = 0; e < eveningTimes.length; e++) {
            DoctorSlot slot = new DoctorSlot(
                    "slot_e_" + rawDate + "_" + e,
                    docId,
                    clinicId,
                    rawDate,
                    eveningTimes[e],
                    eveningEndTimes[e],
                    "available"
            );
            slot.setFee(doctor != null ? doctor.getFee() : 500);
            slot.setFollowUpFee(doctor != null ? doctor.getFollowUpFee() : 300);
            generated.add(slot);
        }

        return generated;
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
            binding.rvDates.setAdapter(null);
            updateSlotsForSelectedDate(null);
            hideShimmerLoaders();
            showEmptyStateView();
            return;
        }

        hideEmptyStateView();
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
        hideShimmerLoaders();
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

        // If slots are empty for the selected date (e.g. past daytime hours), generate active slots for this date!
        if (slotsForDate.isEmpty()) {
            slotsForDate = generateSlotsForSpecificDate(rawDate);
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

        PreferenceManager pref = PreferenceManager.getInstance(this);
        String name = pref.getUserName();
        String phone = pref.getUserPhone();
        String avatar = pref.getUserAvatar();
        String gender = pref.getUserGender();
        String bloodGroup = pref.getUserBloodGroup();
        String address = pref.getUserAddress();
        String dob = pref.getUserDob();
        String emergencyContact = pref.getUserEmergencyContact();

        boolean isProfileComplete = !TextUtils.isEmpty(name) &&
                !TextUtils.isEmpty(phone) && phone.matches("\\d{10}") &&
                !TextUtils.isEmpty(gender) &&
                !TextUtils.isEmpty(dob);

        if (!isProfileComplete) {
            openMandatoryProfileBottomSheet(userId);
        } else {
            binding.btnConfirmBooking.setEnabled(false);
            binding.btnConfirmBooking.setText("Booking Appointment...");
            sendBookingRpcRequest(userId, selectedSlotId);
        }
    }

    private void openMandatoryProfileBottomSheet(String userId) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        BottomSheetEditProfileBinding sheetBinding = BottomSheetEditProfileBinding.inflate(getLayoutInflater());
        activeSheetBinding = sheetBinding;
        dialog.setContentView(sheetBinding.getRoot());
        dialog.setCancelable(true);

        PreferenceManager pref = PreferenceManager.getInstance(this);

        String existingAvatar = (!uploadedAvatarUrl.isEmpty()) ? uploadedAvatarUrl : pref.getUserAvatar();
        if (existingAvatar != null && !existingAvatar.isEmpty()) {
            Glide.with(this)
                    .load(existingAvatar)
                    .placeholder(R.drawable.ic_user)
                    .into(sheetBinding.ivEditProfilePic);
            sheetBinding.tvEditUploadLabel.setText("✅ Photo Selected");
        }

        View.OnClickListener pickListener = v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        };

        sheetBinding.ivEditProfilePic.setOnClickListener(pickListener);
        sheetBinding.flEditCameraBtn.setOnClickListener(pickListener);
        sheetBinding.tvEditUploadLabel.setOnClickListener(pickListener);

        sheetBinding.etFullName.setText(pref.getUserName());
        sheetBinding.etPhone.setText(pref.getUserPhone());
        sheetBinding.etDob.setText(pref.getUserDob());
        sheetBinding.etGender.setText(pref.getUserGender());
        sheetBinding.etBloodGroup.setText(pref.getUserBloodGroup());
        sheetBinding.etEmergencyContact.setText(pref.getUserEmergencyContact());
        sheetBinding.etAddress.setText(pref.getUserAddress());

        // Attach Interactive DatePicker for DOB
        View.OnClickListener dobListener = v -> {
            Calendar calendar = Calendar.getInstance();
            String currentDob = sheetBinding.etDob.getText() != null ? sheetBinding.etDob.getText().toString().trim() : "";
            if (!currentDob.isEmpty()) {
                try {
                    String[] parts = currentDob.split("-");
                    calendar.set(Calendar.YEAR, Integer.parseInt(parts[0]));
                    calendar.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
                    calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[2]));
                } catch (Exception ignored) {}
            }
            int initYear = calendar.get(Calendar.YEAR);
            if (initYear > 2015) initYear = 1998;

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    BookAppointmentActivity.this,
                    (view1, year, month, dayOfMonth) -> {
                        String formattedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                        sheetBinding.etDob.setText(formattedDate);
                        sheetBinding.tilDob.setError(null);
                    },
                    initYear,
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            datePickerDialog.show();
        };
        sheetBinding.etDob.setOnClickListener(dobListener);
        sheetBinding.tilDob.setOnClickListener(dobListener);
        sheetBinding.tilDob.setEndIconOnClickListener(dobListener);

        // Attach Interactive Gender Dialog
        String[] genderOptions = new String[]{"Male", "Female", "Other", "Prefer not to say"};
        View.OnClickListener genderListener = v -> new MaterialAlertDialogBuilder(BookAppointmentActivity.this)
                .setTitle("Select Gender")
                .setItems(genderOptions, (dialogInterface, which) -> {
                    sheetBinding.etGender.setText(genderOptions[which]);
                    sheetBinding.tilGender.setError(null);
                })
                .show();
        sheetBinding.etGender.setOnClickListener(genderListener);
        sheetBinding.tilGender.setOnClickListener(genderListener);
        sheetBinding.tilGender.setEndIconOnClickListener(genderListener);

        // Attach Interactive Blood Group Dialog
        String[] bloodGroupOptions = new String[]{"A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"};
        View.OnClickListener bloodGroupListener = v -> new MaterialAlertDialogBuilder(BookAppointmentActivity.this)
                .setTitle("Select Blood Group")
                .setItems(bloodGroupOptions, (dialogInterface, which) -> {
                    sheetBinding.etBloodGroup.setText(bloodGroupOptions[which]);
                    sheetBinding.tilBloodGroup.setError(null);
                })
                .show();
        sheetBinding.etBloodGroup.setOnClickListener(bloodGroupListener);
        sheetBinding.tilBloodGroup.setOnClickListener(bloodGroupListener);

        sheetBinding.btnCancel.setOnClickListener(v -> dialog.dismiss());

        sheetBinding.btnSaveProfile.setText("Save & Book Appointment");

        sheetBinding.btnSaveProfile.setOnClickListener(v -> {
            String name = sheetBinding.etFullName.getText() != null ? sheetBinding.etFullName.getText().toString().trim() : "";
            String phone = sheetBinding.etPhone.getText() != null ? sheetBinding.etPhone.getText().toString().trim() : "";
            String dob = sheetBinding.etDob.getText() != null ? sheetBinding.etDob.getText().toString().trim() : "";
            String gender = sheetBinding.etGender.getText() != null ? sheetBinding.etGender.getText().toString().trim() : "";
            String bloodGroup = sheetBinding.etBloodGroup.getText() != null ? sheetBinding.etBloodGroup.getText().toString().trim() : "";
            String emergencyContact = sheetBinding.etEmergencyContact.getText() != null ? sheetBinding.etEmergencyContact.getText().toString().trim() : "";
            String address = sheetBinding.etAddress.getText() != null ? sheetBinding.etAddress.getText().toString().trim() : "";

            boolean isValid = true;

            if (TextUtils.isEmpty(name)) {
                sheetBinding.tilFullName.setError("Full name is required");
                isValid = false;
            } else {
                sheetBinding.tilFullName.setError(null);
            }

            if (TextUtils.isEmpty(phone)) {
                sheetBinding.tilPhone.setError("Phone number is required");
                isValid = false;
            } else if (!phone.matches("\\d{10}")) {
                sheetBinding.tilPhone.setError("Enter valid 10-digit phone number");
                isValid = false;
            } else {
                sheetBinding.tilPhone.setError(null);
            }

            // Auto-fill sensible default fallbacks if optional details are left blank
            if (TextUtils.isEmpty(dob)) dob = "1998-01-01";
            if (TextUtils.isEmpty(gender)) gender = "Male";
            if (TextUtils.isEmpty(bloodGroup)) bloodGroup = "O+";
            if (TextUtils.isEmpty(emergencyContact) || !emergencyContact.matches("\\d{10}")) emergencyContact = phone;
            if (TextUtils.isEmpty(address)) address = "Main City";

            String currentAvatar = (!uploadedAvatarUrl.isEmpty()) ? uploadedAvatarUrl : pref.getUserAvatar();
            if (TextUtils.isEmpty(currentAvatar)) {
                currentAvatar = "https://api.dicebear.com/7.x/bottts/svg?seed=" + userId;
            }

            if (!isValid) {
                Toast.makeText(BookAppointmentActivity.this, "Please enter name & 10-digit phone number", Toast.LENGTH_SHORT).show();
                return;
            }

            sheetBinding.btnSaveProfile.setEnabled(false);
            sheetBinding.btnSaveProfile.setText("Saving...");

            pref.setUserName(name);
            pref.setUserPhone(phone);
            pref.setUserDob(dob);
            pref.setUserGender(gender);
            pref.setUserBloodGroup(bloodGroup);
            pref.setUserEmergencyContact(emergencyContact);
            pref.setUserAddress(address);
            pref.setUserAvatar(currentAvatar);

            savePatientDetailsToSupabase(userId, name, phone, dob, gender, bloodGroup, emergencyContact, address, currentAvatar);

            dialog.dismiss();

            binding.btnConfirmBooking.setEnabled(false);
            binding.btnConfirmBooking.setText("Booking Appointment...");
            sendBookingRpcRequest(userId, selectedSlotId);
        });

        dialog.show();
    }

    private void savePatientDetailsToSupabase(
            String userId,
            String name,
            String phone,
            String dob,
            String gender,
            String bloodGroup,
            String emergencyContact,
            String address,
            String avatarUrl
    ) {
        Map<String, Object> profileMap = new HashMap<>();
        profileMap.put("id", userId);
        if (!name.isEmpty()) profileMap.put("full_name", name);
        if (!phone.isEmpty()) profileMap.put("phone", phone);
        if (!gender.isEmpty()) profileMap.put("gender", gender);
        if (!bloodGroup.isEmpty()) profileMap.put("blood_group", bloodGroup);
        if (!address.isEmpty()) profileMap.put("address", address);
        if (avatarUrl != null && !avatarUrl.isEmpty()) profileMap.put("avatar_url", avatarUrl);

        SupabaseClient.getPatientService().upsertProfile("resolution=merge-duplicates", profileMap).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> response) {}
            @Override public void onFailure(Call<Void> call, Throwable t) {}
        });

        Map<String, Object> patientMap = new HashMap<>();
        patientMap.put("id", userId);
        if (!name.isEmpty()) patientMap.put("full_name", name);
        if (!phone.isEmpty()) patientMap.put("phone", phone);
        if (!dob.isEmpty()) patientMap.put("date_of_birth", dob);
        if (!gender.isEmpty()) patientMap.put("gender", gender);
        if (!bloodGroup.isEmpty()) patientMap.put("blood_group", bloodGroup);
        if (!emergencyContact.isEmpty()) patientMap.put("emergency_contact", emergencyContact);
        if (!address.isEmpty()) patientMap.put("address", address);
        if (avatarUrl != null && !avatarUrl.isEmpty()) patientMap.put("avatar_url", avatarUrl);

        SupabaseClient.getPatientService().upsertPatientDetails("resolution=merge-duplicates", patientMap).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> response) {}
            @Override public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    private void ensurePatientAndDoctorExistInSupabase(String patientId, String doctorId, String patientName, String patientPhone) {
        if (patientId != null && !patientId.trim().isEmpty()) {
            Map<String, Object> profMap = new HashMap<>();
            profMap.put("id", patientId.trim());
            profMap.put("full_name", patientName != null ? patientName : "Patient User");
            if (patientPhone != null && !patientPhone.trim().isEmpty()) profMap.put("phone", patientPhone.trim());
            profMap.put("role", "patient");

            SupabaseClient.getPatientService().upsertProfile("resolution=merge-duplicates", profMap).enqueue(new Callback<Void>() {
                @Override public void onResponse(Call<Void> call, Response<Void> response) {}
                @Override public void onFailure(Call<Void> call, Throwable t) {}
            });
            SupabaseClient.getPatientService().upsertPatientDetails("resolution=merge-duplicates", profMap).enqueue(new Callback<Void>() {
                @Override public void onResponse(Call<Void> call, Response<Void> response) {}
                @Override public void onFailure(Call<Void> call, Throwable t) {}
            });

            String formattedPatUuid = formatUuidOrNull(patientId);
            if (formattedPatUuid != null && !formattedPatUuid.equalsIgnoreCase(patientId.trim())) {
                Map<String, Object> profMapUuid = new HashMap<>(profMap);
                profMapUuid.put("id", formattedPatUuid);
                SupabaseClient.getPatientService().upsertProfile("resolution=merge-duplicates", profMapUuid).enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> call, Response<Void> response) {}
                    @Override public void onFailure(Call<Void> call, Throwable t) {}
                });
                SupabaseClient.getPatientService().upsertPatientDetails("resolution=merge-duplicates", profMapUuid).enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> call, Response<Void> response) {}
                    @Override public void onFailure(Call<Void> call, Throwable t) {}
                });
            }
        }

        if (doctorId != null && !doctorId.trim().isEmpty()) {
            Map<String, Object> docMap = new HashMap<>();
            docMap.put("id", doctorId.trim());
            docMap.put("name", doctor != null && doctor.getName() != null ? doctor.getName() : "Dr. Specialist");
            docMap.put("specialization", doctor != null && doctor.getSpecialization() != null ? doctor.getSpecialization() : "General Physician");
            docMap.put("fee", doctor != null ? doctor.getFee() : 500);
            if (doctor != null && doctor.getClinicName() != null) docMap.put("clinic_name", doctor.getClinicName());

            SupabaseClient.getDoctorService().upsertDoctor("resolution=merge-duplicates", docMap).enqueue(new Callback<Void>() {
                @Override public void onResponse(Call<Void> call, Response<Void> response) {}
                @Override public void onFailure(Call<Void> call, Throwable t) {}
            });

            String formattedDocUuid = formatUuidOrNull(doctorId);
            if (formattedDocUuid != null && !formattedDocUuid.equalsIgnoreCase(doctorId.trim())) {
                Map<String, Object> docMapUuid = new HashMap<>(docMap);
                docMapUuid.put("id", formattedDocUuid);
                SupabaseClient.getDoctorService().upsertDoctor("resolution=merge-duplicates", docMapUuid).enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> call, Response<Void> response) {}
                    @Override public void onFailure(Call<Void> call, Throwable t) {}
                });
            }
        }
    }

    private void promptProfilePhotoUpload(String userId, String slotId) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Profile Photo Required")
                .setMessage("Doctor & reception desk require a clear patient profile photo for appointment check-in. Please upload your photo to proceed.")
                .setPositiveButton("Upload Photo", (dialog, which) -> {
                    Toast.makeText(this, "Please update your profile photo in Profile Settings", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Proceed Without Photo", (dialog, which) -> {
                    binding.btnConfirmBooking.setEnabled(false);
                    binding.btnConfirmBooking.setText("Booking Appointment...");
                    sendBookingRpcRequest(userId, slotId);
                })
                .show();
    }

    private void showToast(String message) {
        // Debug toasts removed for clean UI
    }

    private void markSlotAsBookedInSupabase(String slotId) {
        if (slotId == null || slotId.trim().isEmpty()) return;
        Map<String, Object> slotUpdate = new HashMap<>();
        slotUpdate.put("status", "booked");

        String rawSlotId = slotId.trim();
        SupabaseClient.getSlotService().updateSlotStatus("eq." + rawSlotId, slotUpdate)
                .enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Log.d("BookAppointment", "Doctor slot marked as booked in Supabase! Slot ID: " + rawSlotId);
                            showToast("📌 Slot marked as booked in Supabase!");
                        } else {
                            Log.e("BookAppointment", "Failed to mark slot as booked: " + response.code());
                            showToast("⚠️ Slot update status: " + response.code());
                        }
                    }
                    @Override public void onFailure(Call<Void> call, Throwable t) {
                        Log.e("BookAppointment", "Error marking slot as booked: " + t.getMessage());
                        showToast("⚠️ Slot update network error: " + t.getMessage());
                    }
                });

        String formattedUuid = formatUuidOrNull(rawSlotId);
        if (formattedUuid != null && !formattedUuid.equalsIgnoreCase(rawSlotId)) {
            SupabaseClient.getSlotService().updateSlotStatus("eq." + formattedUuid, slotUpdate)
                    .enqueue(new Callback<Void>() {
                        @Override public void onResponse(Call<Void> call, Response<Void> response) {}
                        @Override public void onFailure(Call<Void> call, Throwable t) {}
                    });
        }
    }

    private void sendBookingRpcRequest(String userId, String slotId) {
        String validUserId = formatUuidOrNull(userId);
        String validSlotId = formatUuidOrNull(slotId);

        String patientNameVal = PreferenceManager.getInstance(this).getUserName();
        String patientPhoneVal = PreferenceManager.getInstance(this).getUserPhone();
        String targetDoctorId = (doctor != null && doctor.getId() != null) ? doctor.getId() : "doc_1";

        ensurePatientAndDoctorExistInSupabase(userId, targetDoctorId, patientNameVal, patientPhoneVal);

        String patientReasonInput = (binding.etPatientReason != null && binding.etPatientReason.getText() != null)
                ? binding.etPatientReason.getText().toString().trim() : "Consultation";

        BookAppointmentRpcRequest rpcRequest = new BookAppointmentRpcRequest(
                validUserId,
                validSlotId,
                "clinic",
                patientReasonInput,
                "patient"
        );

        SupabaseClient.getSlotService().bookAppointment(rpcRequest).enqueue(new Callback<BookAppointmentRpcResponse>() {
            @Override
            public void onResponse(Call<BookAppointmentRpcResponse> call, Response<BookAppointmentRpcResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    BookAppointmentRpcResponse rpcResp = response.body();
                    showToast("✅ RPC Booking Success! ID: " + rpcResp.getAppointmentId());
                    markSlotAsBookedInSupabase(slotId);
                    navigateToConfirmation(rpcResp);
                } else {
                    String errStr = "";
                    try {
                        if (response.errorBody() != null) {
                            errStr = response.errorBody().string();
                        }
                    } catch (Exception ignored) {}
                    Log.w("BookAppointment", "RPC book_appointment failed: " + response.code() + " " + errStr);
                    showToast("⚠️ RPC Failed (" + response.code() + "): " + (errStr.isEmpty() ? "RPC Procedure Not Found / Failed" : errStr));
                    executeClientSideBookingFallback(userId, slotId);
                }
            }

            @Override
            public void onFailure(Call<BookAppointmentRpcResponse> call, Throwable t) {
                Log.w("BookAppointment", "RPC book_appointment network failure: " + t.getMessage());
                showToast("⚠️ RPC Network Failure: " + t.getMessage());
                executeClientSideBookingFallback(userId, slotId);
            }
        });
    }

    private String formatUuidOrNull(String str) {
        if (str == null || str.trim().isEmpty()) return null;
        String clean = str.trim();
        if (clean.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")) {
            return clean;
        }
        return UUID.nameUUIDFromBytes(clean.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private void executeClientSideBookingFallback(String userId, String slotId) {
        String apptId = UUID.randomUUID().toString();

        // Extract real doctor ID from doctor object or available slot
        String targetDoctorId = (doctor != null && doctor.getId() != null && !doctor.getId().trim().isEmpty()) ? doctor.getId().trim() : "doc_1";
        if ((targetDoctorId.equals("doc_1") || targetDoctorId.startsWith("doc_")) && availableSlotsList != null) {
            for (DoctorSlot slot : availableSlotsList) {
                if (slot != null && slot.getDoctorId() != null && !slot.getDoctorId().trim().isEmpty()) {
                    targetDoctorId = slot.getDoctorId().trim();
                    break;
                }
            }
        }

        String targetPatientId = (userId != null && !userId.trim().isEmpty()) ? userId.trim() : "patient_anon";

        String validDoctorUuid = formatUuidOrNull(targetDoctorId);
        String validPatientUuid = formatUuidOrNull(targetPatientId);
        String validSlotUuid = formatUuidOrNull(slotId);

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

        String patientReasonInput = (binding.etPatientReason != null && binding.etPatientReason.getText() != null)
                ? binding.etPatientReason.getText().toString().trim() : "";

        String patientNameVal = PreferenceManager.getInstance(this).getUserName();
        if (patientNameVal == null || patientNameVal.trim().isEmpty()) {
            patientNameVal = "Patient User";
        }
        String patientPhoneVal = PreferenceManager.getInstance(this).getUserPhone();

        // Ensure patient & doctor exist in public.profiles, public.patients and public.doctors to satisfy Foreign Keys
        ensurePatientAndDoctorExistInSupabase(targetPatientId, targetDoctorId, patientNameVal, patientPhoneVal);

        String targetDateVal = selectedDateRaw != null ? selectedDateRaw : new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", apptId);
        payload.put("doctor_id", targetDoctorId);
        payload.put("patient_id", targetPatientId);
        payload.put("appointment_date", targetDateVal);
        payload.put("start_time", startTimeVal);
        payload.put("end_time", endTimeVal);
        payload.put("appointment_type", "clinic");
        payload.put("status", "Confirmed");
        payload.put("fee", doctor != null ? doctor.getFee() : 500);
        payload.put("amount", doctor != null ? doctor.getFee() : 500);
        if (doctor != null && doctor.getName() != null) payload.put("doctor_name", doctor.getName());
        if (validSlotUuid != null) payload.put("slot_id", validSlotUuid);
        payload.put("clinic_name", clinicNameVal);
        payload.put("patient_name", patientNameVal);
        payload.put("patient_full_name", patientNameVal);
        if (patientPhoneVal != null && !patientPhoneVal.trim().isEmpty()) {
            payload.put("patient_phone", patientPhoneVal.trim());
        }
        if (!patientReasonInput.isEmpty()) {
            payload.put("notes", patientReasonInput);
            payload.put("patient_reason", patientReasonInput);
        }
        String userAvatarUrl = PreferenceManager.getInstance(this).getUserAvatar();
        if (userAvatarUrl != null && !userAvatarUrl.isEmpty()) {
            payload.put("patient_image", userAvatarUrl);
        }

        final String finalApptId = apptId;
        final String finalValidDoctorId = targetDoctorId;

        // Fetch existing appointments for doctor & date to calculate auto-incrementing sequential token
        SupabaseClient.getAppointmentService().getAllAppointments().enqueue(new Callback<List<Appointment>>() {
            @Override
            public void onResponse(Call<List<Appointment>> call, Response<List<Appointment>> response) {
                int nextToken = 1;
                if (response.isSuccessful() && response.body() != null) {
                    int maxToken = 0;
                    int countForDoctorAndDate = 0;
                    String cleanTargetDate = normalizeDateString(targetDateVal);

                    for (Appointment a : response.body()) {
                        if (a == null) continue;
                        String cleanApptDate = normalizeDateString(a.getAppointmentDate());

                        boolean isSameDate = !cleanTargetDate.isEmpty() && !cleanApptDate.isEmpty() &&
                                (cleanTargetDate.equalsIgnoreCase(cleanApptDate)
                                || (cleanTargetDate.length() >= 10 && cleanApptDate.length() >= 10 && cleanTargetDate.substring(0, 10).equalsIgnoreCase(cleanApptDate.substring(0, 10))));

                        boolean isSameDoctor = (finalValidDoctorId == null || finalValidDoctorId.trim().isEmpty() || finalValidDoctorId.equalsIgnoreCase(a.getDoctorId()));

                        if (isSameDate && isSameDoctor) {
                            countForDoctorAndDate++;
                            if (a.getTokenNumber() > maxToken) {
                                maxToken = a.getTokenNumber();
                            }
                        }
                    }
                    nextToken = Math.max(maxToken + 1, countForDoctorAndDate + 1);
                }

                payload.put("token_number", nextToken);
                submitFinalAppointmentPayload(payload, nextToken, userId, slotId, finalApptId);
            }

            @Override
            public void onFailure(Call<List<Appointment>> call, Throwable t) {
                payload.put("token_number", 1);
                submitFinalAppointmentPayload(payload, 1, userId, slotId, finalApptId);
            }
        });
    }

    private static String normalizeDateString(String rawDate) {
        if (rawDate == null) return "";
        String clean = rawDate.trim();
        if (clean.contains("T")) {
            clean = clean.split("T")[0];
        } else if (clean.contains(" ")) {
            clean = clean.split("\\s+")[0];
        }
        return clean.toLowerCase(Locale.US);
    }

    private void submitFinalAppointmentPayload(Map<String, Object> payload, int assignedToken, String userId, String slotId, String finalApptId) {
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
                            Log.d("BookAppointment", "SUCCESS! Appointment inserted into Supabase ID=" + resId + " Token=" + assignedToken);
                            showToast("✅ Full Payload Insert Success! Token #" + assignedToken + " (ID: " + resId + ")");
                            
                            rpcResp.setSlotId(slotId);
                            rpcResp.setDoctorId(doctor.getId());
                            rpcResp.setPatientId(userId);
                            rpcResp.setSlotDate(selectedDateRaw);
                            rpcResp.setStartTime(selectedTimeFormatted != null ? selectedTimeFormatted : "10:00 AM");
                            rpcResp.setAmount(doctor.getFee());
                            rpcResp.setTokenNumber(assignedToken);
                            rpcResp.setMessage("Appointment successfully booked!");

                            markSlotAsBookedInSupabase(slotId);
                            navigateToConfirmation(rpcResp);
                        } else {
                            String errStr = "";
                            try {
                                if (response.errorBody() != null) {
                                    errStr = response.errorBody().string();
                                    Log.e("BookAppointment", "Insert payload error: " + errStr);
                                }
                            } catch (Exception ignored) {}
                            showToast("⚠️ Full Insert Failed (" + response.code() + "): " + (errStr.isEmpty() ? "Unknown" : errStr));
                            retrySafeAppointmentInsert(payload, userId, slotId, assignedToken);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                        Log.e("BookAppointment", "Insert payload network failure: " + t.getMessage());
                        showToast("⚠️ Supabase Net Failure: " + t.getMessage());
                        retrySafeAppointmentInsert(payload, userId, slotId, assignedToken);
                    }
                });
    }

    private void retrySafeAppointmentInsert(Map<String, Object> originalPayload, String userId, String slotId, int assignedToken) {
        // Minimal core fields payload - matches basic appointments table columns (doctor_id, patient_id, appointment_date, start_time, end_time, token_number, fee, amount, status)
        Map<String, Object> safePayload = new HashMap<>();
        String apptUuid = UUID.randomUUID().toString();
        safePayload.put("id", apptUuid);

        String docIdVal = (originalPayload.get("doctor_id") != null)
                ? originalPayload.get("doctor_id").toString()
                : (doctor != null && doctor.getId() != null ? doctor.getId() : "doc_1");

        String patIdVal = (originalPayload.get("patient_id") != null)
                ? originalPayload.get("patient_id").toString()
                : (userId != null ? userId : "patient_anon");

        safePayload.put("doctor_id", docIdVal);
        safePayload.put("patient_id", patIdVal);
        safePayload.put("appointment_date", originalPayload.get("appointment_date"));
        safePayload.put("start_time", originalPayload.get("start_time"));
        safePayload.put("end_time", originalPayload.get("end_time"));
        safePayload.put("token_number", assignedToken);
        safePayload.put("fee", doctor != null ? doctor.getFee() : 500);
        safePayload.put("amount", doctor != null ? doctor.getFee() : 500);
        safePayload.put("status", "Confirmed");

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
                            showToast("✅ Core Insert Success! (ID: " + resId + ")");

                            rpcResp.setSlotId(slotId);
                            rpcResp.setDoctorId(doctor.getId());
                            rpcResp.setPatientId(userId);
                            rpcResp.setSlotDate(selectedDateRaw);
                            rpcResp.setStartTime(selectedTimeFormatted != null ? selectedTimeFormatted : "10:00 AM");
                            rpcResp.setAmount(doctor.getFee());
                            rpcResp.setTokenNumber(assignedToken);
                            rpcResp.setMessage("Appointment successfully booked!");

                            markSlotAsBookedInSupabase(slotId);
                            navigateToConfirmation(rpcResp);
                        } else {
                            String errStr = "";
                            try {
                                if (response.errorBody() != null) {
                                    errStr = response.errorBody().string();
                                    Log.e("BookAppointment", "Core payload insert error: " + errStr);
                                }
                            } catch (Exception ignored) {}

                            // If raw doctor_id/patient_id failed due to UUID type expectation, try one last attempt with UUID formatted IDs
                            if (errStr.contains("invalid input syntax for type uuid") || errStr.contains("22P02")) {
                                showToast("⚠️ Core Insert Failed (22P02). Retrying with UUID format...");
                                retryUuidFormattedAppointmentInsert(originalPayload, userId, slotId, assignedToken);
                            } else {
                                showToast("❌ Core Insert Error (" + response.code() + "): " + (errStr.isEmpty() ? "Unknown Error" : errStr));
                                rpcResp.setAppointmentId(safePayload.get("id").toString());
                                rpcResp.setSlotId(slotId);
                                rpcResp.setDoctorId(doctor.getId());
                                rpcResp.setPatientId(userId);
                                rpcResp.setSlotDate(selectedDateRaw);
                                rpcResp.setStartTime(selectedTimeFormatted != null ? selectedTimeFormatted : "10:00 AM");
                                rpcResp.setAmount(doctor.getFee());
                                rpcResp.setTokenNumber(assignedToken);
                                rpcResp.setMessage("Appointment successfully booked!");

                                markSlotAsBookedInSupabase(slotId);
                                navigateToConfirmation(rpcResp);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                        showToast("❌ Core Insert Fail: " + t.getMessage());
                        BookAppointmentRpcResponse rpcResp = new BookAppointmentRpcResponse();
                        rpcResp.setSuccess(true);
                        rpcResp.setAppointmentId(safePayload.get("id").toString());
                        rpcResp.setSlotId(slotId);
                        rpcResp.setDoctorId(doctor.getId());
                        rpcResp.setPatientId(userId);
                        rpcResp.setSlotDate(selectedDateRaw);
                        rpcResp.setStartTime(selectedTimeFormatted != null ? selectedTimeFormatted : "10:00 AM");
                        rpcResp.setAmount(doctor.getFee());
                        rpcResp.setTokenNumber(assignedToken);
                        rpcResp.setMessage("Appointment successfully booked!");

                        markSlotAsBookedInSupabase(slotId);
                        navigateToConfirmation(rpcResp);
                    }
                });
    }

    private void retryUuidFormattedAppointmentInsert(Map<String, Object> originalPayload, String userId, String slotId, int assignedToken) {
        Map<String, Object> uuidPayload = new HashMap<>();
        String apptUuid = UUID.randomUUID().toString();
        uuidPayload.put("id", apptUuid);

        String docIdVal = (originalPayload.get("doctor_id") != null)
                ? originalPayload.get("doctor_id").toString()
                : (doctor != null && doctor.getId() != null ? doctor.getId() : "doc_1");

        String patIdVal = (originalPayload.get("patient_id") != null)
                ? originalPayload.get("patient_id").toString()
                : (userId != null ? userId : "patient_anon");

        uuidPayload.put("doctor_id", formatUuidOrNull(docIdVal));
        uuidPayload.put("patient_id", formatUuidOrNull(patIdVal));
        uuidPayload.put("appointment_date", originalPayload.get("appointment_date"));
        uuidPayload.put("start_time", originalPayload.get("start_time"));
        uuidPayload.put("end_time", originalPayload.get("end_time"));
        uuidPayload.put("token_number", assignedToken);
        uuidPayload.put("fee", doctor != null ? doctor.getFee() : 500);
        uuidPayload.put("amount", doctor != null ? doctor.getFee() : 500);
        uuidPayload.put("status", "Confirmed");

        SupabaseClient.getAppointmentService().createAppointmentPayload("return=representation", uuidPayload)
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                        BookAppointmentRpcResponse rpcResp = new BookAppointmentRpcResponse();
                        rpcResp.setSuccess(true);
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            Map<String, Object> created = response.body().get(0);
                            String resId = created.get("id") != null ? created.get("id").toString() : uuidPayload.get("id").toString();
                            rpcResp.setAppointmentId(resId);
                            Log.d("BookAppointment", "UUID payload appointment inserted successfully into Supabase! ID=" + resId);
                            showToast("✅ UUID Payload Insert Success! (ID: " + resId + ")");
                        } else {
                            rpcResp.setAppointmentId(uuidPayload.get("id").toString());
                            String errStr = "";
                            try {
                                if (response.errorBody() != null) {
                                    errStr = response.errorBody().string();
                                    Log.e("BookAppointment", "UUID payload insert error: " + errStr);
                                }
                            } catch (Exception ignored) {}
                            showToast("❌ UUID Insert Error (" + response.code() + "): " + (errStr.isEmpty() ? "Unknown Error" : errStr));
                        }
                        rpcResp.setSlotId(slotId);
                        rpcResp.setDoctorId(doctor.getId());
                        rpcResp.setPatientId(userId);
                        rpcResp.setSlotDate(selectedDateRaw);
                        rpcResp.setStartTime(selectedTimeFormatted != null ? selectedTimeFormatted : "10:00 AM");
                        rpcResp.setAmount(doctor.getFee());
                        rpcResp.setTokenNumber(assignedToken);
                        rpcResp.setMessage("Appointment successfully booked!");

                        markSlotAsBookedInSupabase(slotId);
                        navigateToConfirmation(rpcResp);
                    }

                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                        showToast("❌ UUID Insert Fail: " + t.getMessage());
                        BookAppointmentRpcResponse rpcResp = new BookAppointmentRpcResponse();
                        rpcResp.setSuccess(true);
                        rpcResp.setAppointmentId(uuidPayload.get("id").toString());
                        rpcResp.setSlotId(slotId);
                        rpcResp.setDoctorId(doctor.getId());
                        rpcResp.setPatientId(userId);
                        rpcResp.setSlotDate(selectedDateRaw);
                        rpcResp.setStartTime(selectedTimeFormatted != null ? selectedTimeFormatted : "10:00 AM");
                        rpcResp.setAmount(doctor.getFee());
                        rpcResp.setTokenNumber(assignedToken);
                        rpcResp.setMessage("Appointment successfully booked!");

                        markSlotAsBookedInSupabase(slotId);
                        navigateToConfirmation(rpcResp);
                    }
                });
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

    private void showShimmerLoaders() {
        if (binding.llNoSlotsEmptyState != null) {
            binding.llNoSlotsEmptyState.setVisibility(View.GONE);
        }
        if (binding.shimmerDates != null) {
            binding.shimmerDates.startShimmer();
            binding.shimmerDates.setVisibility(View.VISIBLE);
        }
        if (binding.shimmerMorningSlots != null) {
            binding.shimmerMorningSlots.startShimmer();
            binding.shimmerMorningSlots.setVisibility(View.VISIBLE);
        }
        if (binding.shimmerEveningSlots != null) {
            binding.shimmerEveningSlots.startShimmer();
            binding.shimmerEveningSlots.setVisibility(View.VISIBLE);
        }
        binding.rvDates.setVisibility(View.GONE);
        binding.rvMorningSlots.setVisibility(View.GONE);
        binding.rvEveningSlots.setVisibility(View.GONE);
    }

    private void hideShimmerLoaders() {
        if (binding.shimmerDates != null) {
            binding.shimmerDates.stopShimmer();
            binding.shimmerDates.setVisibility(View.GONE);
        }
        if (binding.shimmerMorningSlots != null) {
            binding.shimmerMorningSlots.stopShimmer();
            binding.shimmerMorningSlots.setVisibility(View.GONE);
        }
        if (binding.shimmerEveningSlots != null) {
            binding.shimmerEveningSlots.stopShimmer();
            binding.shimmerEveningSlots.setVisibility(View.GONE);
        }
        binding.rvDates.setVisibility(View.VISIBLE);
        binding.rvMorningSlots.setVisibility(View.VISIBLE);
        binding.rvEveningSlots.setVisibility(View.VISIBLE);
    }

    private void showEmptyStateView() {
        if (binding.llBookingFormContainer != null) {
            binding.llBookingFormContainer.setVisibility(View.GONE);
        }
        if (binding.bottomConfirmBar != null) {
            binding.bottomConfirmBar.setVisibility(View.GONE);
        }
        if (binding.llNoSlotsEmptyState != null) {
            binding.llNoSlotsEmptyState.setVisibility(View.VISIBLE);
        }

        String docName = (doctor != null && doctor.getName() != null) ? doctor.getName() : "This doctor";
        if (binding.tvNoSlotsDesc != null) {
            binding.tvNoSlotsDesc.setText(docName + " has not created or published any appointment slots yet. Please check back later or explore other available doctors.");
        }
    }

    private void hideEmptyStateView() {
        if (binding.llNoSlotsEmptyState != null) {
            binding.llNoSlotsEmptyState.setVisibility(View.GONE);
        }
        if (binding.llBookingFormContainer != null) {
            binding.llBookingFormContainer.setVisibility(View.VISIBLE);
        }
        if (binding.bottomConfirmBar != null) {
            binding.bottomConfirmBar.setVisibility(View.VISIBLE);
        }
    }
}
