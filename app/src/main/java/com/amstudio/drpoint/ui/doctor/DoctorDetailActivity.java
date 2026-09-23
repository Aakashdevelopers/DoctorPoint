package com.amstudio.drpoint.ui.doctor;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.amstudio.drpoint.R;
import com.amstudio.drpoint.adapter.CalendarDayAdapter;
import com.amstudio.drpoint.adapter.ClinicPhotoAdapter;
import com.amstudio.drpoint.adapter.TimeSlotAdapter;
import com.amstudio.drpoint.databinding.ActivityDoctorDetailBinding;
import com.amstudio.drpoint.model.Clinic;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.model.DoctorSlot;
import com.amstudio.drpoint.network.SupabaseClient;
import com.amstudio.drpoint.ui.booking.BookAppointmentActivity;
import com.amstudio.drpoint.util.AvailabilityHelper;
import com.amstudio.drpoint.util.DummyDataProvider;
import com.amstudio.drpoint.util.PreferenceManager;
import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DoctorDetailActivity extends AppCompatActivity {

    private ActivityDoctorDetailBinding binding;
    private Doctor doctor;
    private List<Clinic> doctorClinics = new ArrayList<>();
    private List<DoctorSlot> futureSlots = new ArrayList<>();

    private Calendar currentCalendar = Calendar.getInstance();
    private CalendarDayAdapter calendarDayAdapter;
    private TimeSlotAdapter timeSlotAdapter;
    private String selectedDateRaw = null;
    private DoctorSlot selectedDoctorSlot = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDoctorDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        doctor = (Doctor) getIntent().getSerializableExtra("doctor");
        if (doctor == null) {
            doctor = DummyDataProvider.getDoctors().get(0);
        }

        binding.ivBack.setOnClickListener(v -> finish());
        binding.ivShare.setOnClickListener(v -> shareDoctorInfo());

        updateFavoriteIcon();
        binding.ivHeart.setOnClickListener(v -> {
            boolean isFav = PreferenceManager.getInstance(DoctorDetailActivity.this).toggleFavoriteDoctor(doctor.getId());
            updateFavoriteIcon();
            String msg = isFav ? "Added to favorites" : "Removed from favorites";
            Toast.makeText(DoctorDetailActivity.this, msg, Toast.LENGTH_SHORT).show();
        });

        populateDoctorDetails();
        setupActionButtons();
        setupTabLayout();
        setupClinicPhotos();

        setupMonthCalendar();
        loadClinicsAndSlotsFromSupabase();

        binding.btnBookAppointment.setOnClickListener(v -> launchBookingActivity());
    }

    private void launchBookingActivity() {
        Intent intent = new Intent(DoctorDetailActivity.this, BookAppointmentActivity.class);
        intent.putExtra("doctor", doctor);
        if (doctorClinics != null && !doctorClinics.isEmpty()) {
            intent.putExtra("clinic", doctorClinics.get(0));
        }
        if (selectedDateRaw != null) {
            intent.putExtra("selectedDate", selectedDateRaw);
        }
        startActivity(intent);
    }

    private void updateFavoriteIcon() {
        boolean isFav = PreferenceManager.getInstance(this).isFavoriteDoctor(doctor.getId());
        binding.ivHeart.setImageResource(isFav ? R.drawable.ic_heart_filled : R.drawable.ic_heart);
    }

    private void populateDoctorDetails() {
        binding.tvDoctorName.setText(doctor.getName());
        binding.tvSpecialization.setText(doctor.getSpecializationString());
        binding.tvQualification.setText(doctor.getQualification());
        binding.tvExperience.setText(doctor.getExperience() + " Overall Experience");
        binding.tvRatingNum.setText(doctor.getRating() + " ★");
        binding.tvClinicName.setText(doctor.getClinicName());
        binding.tvClinicAddress.setText(doctor.getLocation());
        binding.tvBottomFee.setText("₹" + doctor.getFee());

        if (doctor.getAbout() != null && !doctor.getAbout().isEmpty()) {
            binding.tvAboutDesc.setText(doctor.getAbout());
        }

        // Contact Information (Doctor Phone & Reception Phone)
        if (binding.cardContactInfo != null) {
            String docPhone = doctor.getDoctorPhone();
            String recPhone = doctor.getReceptionPhone();

            boolean hasDocPhone = docPhone != null && !docPhone.trim().isEmpty();
            boolean hasRecPhone = recPhone != null && !recPhone.trim().isEmpty();

            if (!hasDocPhone && !hasRecPhone) {
                binding.cardContactInfo.setVisibility(View.GONE);
            } else {
                binding.cardContactInfo.setVisibility(View.VISIBLE);

                if (hasDocPhone) {
                    binding.llDoctorPhone.setVisibility(View.VISIBLE);
                    binding.tvDoctorPhone.setText(docPhone);
                    binding.btnCallDoctor.setOnClickListener(v -> makePhoneCall(docPhone));
                } else {
                    binding.llDoctorPhone.setVisibility(View.GONE);
                }

                if (hasDocPhone && hasRecPhone) {
                    binding.dividerPhone.setVisibility(View.VISIBLE);
                } else {
                    binding.dividerPhone.setVisibility(View.GONE);
                }

                if (hasRecPhone) {
                    binding.llReceptionPhone.setVisibility(View.VISIBLE);
                    binding.tvReceptionPhone.setText(recPhone);
                    binding.btnCallReception.setOnClickListener(v -> makePhoneCall(recPhone));
                } else {
                    binding.llReceptionPhone.setVisibility(View.GONE);
                }
            }
        }

        Object imageSource = (doctor.getImageUrl() != null && !doctor.getImageUrl().isEmpty())
                ? doctor.getImageUrl()
                : (doctor.getImageRes() != 0 ? doctor.getImageRes() : R.drawable.ic_user);

        Glide.with(this)
                .load(imageSource)
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .into(binding.ivDoctorImage);
    }

    private void loadClinicsAndSlotsFromSupabase() {
        if (doctor == null || doctor.getId() == null) return;

        // Load clinics from Supabase
        SupabaseClient.getDoctorService().getDoctorClinics("eq." + doctor.getId())
                .enqueue(new Callback<List<Clinic>>() {
                    @Override
                    public void onResponse(Call<List<Clinic>> call, Response<List<Clinic>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            doctorClinics = response.body();
                            Clinic primary = doctorClinics.get(0);
                            binding.tvClinicName.setText(primary.getClinicName());
                            binding.tvClinicAddress.setText(primary.getFullAddress());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Clinic>> call, Throwable t) {}
                });

        // Load schedules & slots from Supabase
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        SupabaseClient.getSlotService().getDoctorSchedules("eq." + doctor.getId(), "gte." + todayDate)
                .enqueue(new Callback<List<DoctorSlot>>() {
                    @Override
                    public void onResponse(Call<List<DoctorSlot>> call, Response<List<DoctorSlot>> response) {
                        List<DoctorSlot> scheduleSlots = new ArrayList<>();
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            scheduleSlots = expandSchedulesToSlots(response.body());
                        }

                        List<DoctorSlot> finalScheduleSlots = scheduleSlots;

                        // Also fetch doctor_slots table
                        SupabaseClient.getSlotService().getAllFutureDoctorSlots("eq." + doctor.getId(), "gte." + todayDate)
                                .enqueue(new Callback<List<DoctorSlot>>() {
                                    @Override
                                    public void onResponse(Call<List<DoctorSlot>> call2, Response<List<DoctorSlot>> response2) {
                                        futureSlots = new ArrayList<>(finalScheduleSlots);
                                        if (response2.isSuccessful() && response2.body() != null) {
                                            for (DoctorSlot slot : response2.body()) {
                                                if (!futureSlots.contains(slot)) {
                                                    futureSlots.add(slot);
                                                }
                                            }
                                        }
                                        renderCurrentMonthCalendar();
                                    }

                                    @Override
                                    public void onFailure(Call<List<DoctorSlot>> call2, Throwable t2) {
                                        futureSlots = finalScheduleSlots;
                                        renderCurrentMonthCalendar();
                                    }
                                });
                    }

                    @Override
                    public void onFailure(Call<List<DoctorSlot>> call, Throwable t) {
                        // Fallback to doctor_slots table
                        SupabaseClient.getSlotService().getAllFutureDoctorSlots("eq." + doctor.getId(), "gte." + todayDate)
                                .enqueue(new Callback<List<DoctorSlot>>() {
                                    @Override
                                    public void onResponse(Call<List<DoctorSlot>> call2, Response<List<DoctorSlot>> response2) {
                                        if (response2.isSuccessful() && response2.body() != null) {
                                            futureSlots = response2.body();
                                        } else {
                                            futureSlots = new ArrayList<>();
                                        }
                                        renderCurrentMonthCalendar();
                                    }

                                    @Override
                                    public void onFailure(Call<List<DoctorSlot>> call2, Throwable t2) {
                                        futureSlots = new ArrayList<>();
                                        renderCurrentMonthCalendar();
                                    }
                                });
                    }
                });
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

            expanded.addAll(generateSlotPairsForRange(sched, dateStr, mStart, mEnd, consultMins, 0, "morning"));
            expanded.addAll(generateSlotPairsForRange(sched, dateStr, eStart, eEnd, consultMins, 0, "evening"));
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

                curr += consultMins + bufferMins;
            }
        } catch (Exception ignored) {}
        return slots;
    }

    private int timeToMinutes(String timeStr) {
        if (timeStr == null) return 0;
        String[] parts = timeStr.trim().split(":");
        int h = Integer.parseInt(parts[0]);
        int m = Integer.parseInt(parts[1]);
        return h * 60 + m;
    }

    private String minutesToTime(int mins) {
        int h = mins / 60;
        int m = mins % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:00", h, m);
    }

    private void makePhoneCall(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phoneNumber.trim()));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to make call", Toast.LENGTH_SHORT).show();
        }
    }

    private void showCallSelectionDialog() {
        String docPhone = doctor != null ? doctor.getDoctorPhone() : null;
        String recPhone = doctor != null ? doctor.getReceptionPhone() : null;

        boolean hasDocPhone = docPhone != null && !docPhone.trim().isEmpty();
        boolean hasRecPhone = recPhone != null && !recPhone.trim().isEmpty();

        if (hasDocPhone && hasRecPhone) {
            String[] options = new String[]{"Call Doctor Direct (" + docPhone + ")", "Call Clinic Reception (" + recPhone + ")"};
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Select Contact Number")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            makePhoneCall(docPhone);
                        } else {
                            makePhoneCall(recPhone);
                        }
                    })
                    .show();
        } else if (hasDocPhone) {
            makePhoneCall(docPhone);
        } else if (hasRecPhone) {
            makePhoneCall(recPhone);
        } else {
            makePhoneCall("9876543210");
        }
    }

    private void setupActionButtons() {
        binding.btnActionCall.setOnClickListener(v -> showCallSelectionDialog());

        binding.btnActionDirections.setOnClickListener(v ->
                Toast.makeText(this, "Opening directions to " + doctor.getClinicName(), Toast.LENGTH_SHORT).show()
        );

        binding.btnActionShare.setOnClickListener(v -> shareDoctorInfo());

        binding.btnActionSave.setOnClickListener(v -> {
            boolean isFav = PreferenceManager.getInstance(DoctorDetailActivity.this).toggleFavoriteDoctor(doctor.getId());
            updateFavoriteIcon();
            String msg = isFav ? "Doctor profile saved" : "Doctor profile removed";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupTabLayout() {
        binding.tabLayout.removeAllTabs();
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Appointment"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Emergency"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("About Doctor"));

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showTabContent(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        showTabContent(0);

        if (binding.btnQuickBookSlot != null) {
            binding.btnQuickBookSlot.setOnClickListener(v -> launchBookingActivity());
        }

        if (binding.btnEmergencyCall108 != null) {
            binding.btnEmergencyCall108.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:108"));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Calling 108 Emergency...", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (binding.btnEmergencyCallClinic != null) {
            binding.btnEmergencyCallClinic.setOnClickListener(v -> {
                String recPhone = doctor != null ? doctor.getReceptionPhone() : null;
                String docPhone = doctor != null ? doctor.getDoctorPhone() : null;
                if (recPhone != null && !recPhone.trim().isEmpty()) {
                    makePhoneCall(recPhone);
                } else if (docPhone != null && !docPhone.trim().isEmpty()) {
                    makePhoneCall(docPhone);
                } else {
                    makePhoneCall("9876543210");
                }
            });
        }
    }

    private void showTabContent(int position) {
        if (binding == null) return;
        if (binding.layoutTabAppointment != null) {
            binding.layoutTabAppointment.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        }
        if (binding.layoutTabEmergency != null) {
            binding.layoutTabEmergency.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
        }
        if (binding.layoutTabAbout != null) {
            binding.layoutTabAbout.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
        }
    }

    private void setupMonthCalendar() {
        if (binding.rvMonthCalendar == null) return;

        binding.rvMonthCalendar.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        calendarDayAdapter = new CalendarDayAdapter(this::onCalendarDaySelected);
        binding.rvMonthCalendar.setAdapter(calendarDayAdapter);

        // Time slots horizontal list
        binding.rvTimeSlots.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        timeSlotAdapter = new TimeSlotAdapter((slot, pos) -> selectedDoctorSlot = slot);
        binding.rvTimeSlots.setAdapter(timeSlotAdapter);

        if (binding.btnPrevMonth != null) {
            binding.btnPrevMonth.setOnClickListener(v -> {
                currentCalendar.add(Calendar.MONTH, -1);
                renderCurrentMonthCalendar();
            });
        }

        if (binding.btnNextMonth != null) {
            binding.btnNextMonth.setOnClickListener(v -> {
                currentCalendar.add(Calendar.MONTH, 1);
                renderCurrentMonthCalendar();
            });
        }

        renderCurrentMonthCalendar();
    }

    private void renderCurrentMonthCalendar() {
        if (binding == null || calendarDayAdapter == null) return;

        if (binding.shimmerDetail != null) {
            binding.shimmerDetail.stopShimmer();
            binding.shimmerDetail.setVisibility(View.GONE);
        }

        SimpleDateFormat monthSdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        if (binding.tvMonthYear != null) {
            binding.tvMonthYear.setText(monthSdf.format(currentCalendar.getTime()));
        }

        List<CalendarDayAdapter.CalendarDay> dayList = new ArrayList<>();

        Calendar nowCal = Calendar.getInstance();
        SimpleDateFormat sdfFull = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayStr = sdfFull.format(nowCal.getTime());

        boolean isCurrentMonth = (currentCalendar.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)) &&
                (currentCalendar.get(Calendar.MONTH) == nowCal.get(Calendar.MONTH));

        int startDay = 1;
        if (isCurrentMonth) {
            startDay = nowCal.get(Calendar.DAY_OF_MONTH); // Start from today
        } else if (currentCalendar.before(nowCal)) {
            // Past month -> empty list
            calendarDayAdapter.setDays(new ArrayList<>());
            return;
        }

        Calendar cal = (Calendar) currentCalendar.clone();
        int maxDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Extract available dates from futureSlots with date normalization
        Set<String> availableDates = new HashSet<>();
        if (futureSlots != null) {
            for (DoctorSlot slot : futureSlots) {
                if (slot != null && slot.getSlotDate() != null) {
                    String cleanDate = slot.getSlotDate().trim();
                    if (cleanDate.contains("T")) cleanDate = cleanDate.substring(0, cleanDate.indexOf("T")).trim();
                    if (cleanDate.contains(" ")) cleanDate = cleanDate.substring(0, cleanDate.indexOf(" ")).trim();

                    try {
                        Date d = sdfFull.parse(cleanDate);
                        if (d != null) {
                            cleanDate = sdfFull.format(d);
                        }
                    } catch (Exception ignored) {}

                    boolean isBookable = slot.getStatus() == null ||
                            slot.getStatus().isEmpty() ||
                            "available".equalsIgnoreCase(slot.getStatus()) ||
                            !"booked".equalsIgnoreCase(slot.getStatus());

                    if (isBookable && !AvailabilityHelper.isDateInPast(cleanDate)) {
                        availableDates.add(cleanDate);
                    }
                }
            }
        }

        int firstAvailablePos = -1;
        String firstAvailableDateStr = null;

        for (int day = startDay; day <= maxDaysInMonth; day++) {
            cal.set(Calendar.DAY_OF_MONTH, day);
            String fullDate = sdfFull.format(cal.getTime());

            boolean isPast = fullDate.compareTo(todayStr) < 0;
            boolean isAvailable = !isPast && availableDates.contains(fullDate);

            CalendarDayAdapter.CalendarDay cd = new CalendarDayAdapter.CalendarDay(
                    day,
                    fullDate,
                    true,
                    isAvailable,
                    isPast
            );

            dayList.add(cd);

            if (isAvailable && firstAvailablePos == -1) {
                firstAvailablePos = dayList.size() - 1;
                firstAvailableDateStr = fullDate;
            }
        }

        calendarDayAdapter.setDays(dayList);

        if (firstAvailableDateStr != null) {
            calendarDayAdapter.setSelectedPosition(firstAvailablePos);
            onCalendarDaySelected(dayList.get(firstAvailablePos));
        } else if (!dayList.isEmpty()) {
            calendarDayAdapter.setSelectedPosition(0);
            onCalendarDaySelected(dayList.get(0));
        } else {
            showSlotsForDate(todayStr);
        }
    }

    private void onCalendarDaySelected(CalendarDayAdapter.CalendarDay day) {
        if (day == null || day.getFullDate() == null || day.getFullDate().isEmpty()) return;
        selectedDateRaw = day.getFullDate();

        try {
            SimpleDateFormat inputSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputSdf = new SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault());
            Date d = inputSdf.parse(selectedDateRaw);
            if (d != null && binding.tvSelectedDateTitle != null) {
                binding.tvSelectedDateTitle.setText("Available Slots for " + outputSdf.format(d));
            }
        } catch (Exception ignored) {}

        showSlotsForDate(selectedDateRaw);
    }

    private void showSlotsForDate(String dateRaw) {
        if (futureSlots == null || dateRaw == null) return;

        List<DoctorSlot> filteredSlots = AvailabilityHelper.filterAndSortSlots(futureSlots, null, dateRaw);

        if (timeSlotAdapter != null) {
            timeSlotAdapter.submitList(filteredSlots);
            timeSlotAdapter.setSelectedPosition(-1);
            selectedDoctorSlot = null;

            for (int i = 0; i < filteredSlots.size(); i++) {
                if ("available".equalsIgnoreCase(filteredSlots.get(i).getStatus())) {
                    timeSlotAdapter.setSelectedPosition(i);
                    selectedDoctorSlot = filteredSlots.get(i);
                    break;
                }
            }
        }
    }



    private void setupClinicPhotos() {
        binding.rvClinicPhotos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        ClinicPhotoAdapter photoAdapter = new ClinicPhotoAdapter();
        binding.rvClinicPhotos.setAdapter(photoAdapter);

        List<Object> photos = (doctor != null) ? doctor.getClinicPhotosList() : new ArrayList<>();
        if (photos.isEmpty()) {
            binding.cardClinicPhotos.setVisibility(View.GONE);
        } else {
            binding.cardClinicPhotos.setVisibility(View.VISIBLE);
            photoAdapter.submitList(photos);
        }
    }

    private void shareDoctorInfo() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out " + doctor.getName() + " (" + doctor.getSpecializationString() + ") on Doctor Point app!");
        startActivity(shareIntent);
    }
}
