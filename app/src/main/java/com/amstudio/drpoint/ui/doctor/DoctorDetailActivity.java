package com.amstudio.drpoint.ui.doctor;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.amstudio.drpoint.R;
import com.amstudio.drpoint.adapter.CalendarDayAdapter;
import com.amstudio.drpoint.adapter.ClinicPhotoAdapter;
import com.amstudio.drpoint.adapter.DoctorReviewsAdapter;
import com.amstudio.drpoint.adapter.TimeSlotAdapter;
import com.amstudio.drpoint.databinding.ActivityDoctorDetailBinding;
import com.amstudio.drpoint.model.Appointment;
import com.amstudio.drpoint.model.Clinic;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.model.DoctorReview;
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
    private DoctorReviewsAdapter tabReviewsAdapter;

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
        binding.flBackBg.setOnClickListener(v -> finish());
        binding.ivShare.setOnClickListener(v -> shareDoctorInfo());
        binding.flShareBg.setOnClickListener(v -> shareDoctorInfo());

        updateFavoriteIcon();
        binding.ivHeart.setOnClickListener(v -> toggleSaveDoctor());
        binding.flHeartBg.setOnClickListener(v -> toggleSaveDoctor());

        setupReviewsTabRecyclerView();
        populateDoctorDetails();
        setupActionButtons();
        setupTabLayout();
        setupClinicPhotos();

        setupMonthCalendar();
        loadClinicsAndSlotsFromSupabase();

        binding.btnBookAppointment.setOnClickListener(v -> launchBookingActivity());
    }

    private void setupReviewsTabRecyclerView() {
        if (binding.rvTabReviewsList != null) {
            binding.rvTabReviewsList.setLayoutManager(new LinearLayoutManager(this));
            tabReviewsAdapter = new DoctorReviewsAdapter();
            binding.rvTabReviewsList.setAdapter(tabReviewsAdapter);
        }
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
        if (doctor == null || doctor.getId() == null) return;
        boolean isFav = PreferenceManager.getInstance(this).isFavoriteDoctor(doctor.getId());
        if (isFav) {
            binding.ivHeart.setImageResource(R.drawable.ic_heart_filled);
            ImageViewCompat.setImageTintList(binding.ivHeart, ColorStateList.valueOf(ContextCompat.getColor(this, R.color.error_red)));

            if (binding.ivActionSave != null) {
                binding.ivActionSave.setImageResource(R.drawable.ic_heart_filled);
                ImageViewCompat.setImageTintList(binding.ivActionSave, ColorStateList.valueOf(ContextCompat.getColor(this, R.color.error_red)));
            }
            if (binding.tvActionSave != null) {
                binding.tvActionSave.setText("Saved");
                binding.tvActionSave.setTextColor(ContextCompat.getColor(this, R.color.error_red));
            }
        } else {
            binding.ivHeart.setImageResource(R.drawable.ic_heart);
            ImageViewCompat.setImageTintList(binding.ivHeart, ColorStateList.valueOf(ContextCompat.getColor(this, R.color.text_primary)));

            if (binding.ivActionSave != null) {
                binding.ivActionSave.setImageResource(R.drawable.ic_save);
                ImageViewCompat.setImageTintList(binding.ivActionSave, ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
            }
            if (binding.tvActionSave != null) {
                binding.tvActionSave.setText("Save");
                binding.tvActionSave.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            }
        }
    }

    private void populateDoctorDetails() {
        binding.tvDoctorName.setText(doctor.getName());
        binding.tvSpecialization.setText(doctor.getSpecializationString());
        binding.tvQualification.setText(doctor.getQualification());
        binding.tvExperience.setText(doctor.getExperience() + " Overall Experience");
        double initialRating = doctor.getRating();
        int initialReviews = doctor.getReviewCount();
        binding.tvRatingNum.setText(String.format(Locale.getDefault(), "%.1f ★", initialRating));
        if (binding.tvReviewsCount != null) {
            binding.tvReviewsCount.setText(initialReviews + " Reviews");
        }
        if (binding.tvPatientCount != null) {
            int patients = doctor.getPatientCount();
            if (patients >= 1000) {
                binding.tvPatientCount.setText(String.format(Locale.getDefault(), "%,d+", patients));
            } else {
                binding.tvPatientCount.setText(String.valueOf(patients));
            }
        }
        if (binding.tvSatisfactionNum != null) {
            int satisfaction = 0;
            if (initialReviews > 0 && initialRating >= 3.0) {
                satisfaction = (int) Math.min(100, Math.round((initialRating / 5.0) * 100));
            }
            binding.tvSatisfactionNum.setText(satisfaction + "%");
        }
        binding.tvClinicName.setText(doctor.getClinicName());
        binding.tvClinicAddress.setText(doctor.getLocation());
        binding.tvBottomFee.setText("₹" + doctor.getFee());

        loadLiveDoctorRatingAndReviews();
        loadLivePatientCount();

        // Switch to Review Tab when rating or reviews count is clicked
        View.OnClickListener goToReviewTab = v -> {
            if (binding.tabLayout != null && binding.tabLayout.getTabCount() > 1) {
                TabLayout.Tab tab = binding.tabLayout.getTabAt(1);
                if (tab != null) {
                    tab.select();
                }
            }
        };

        if (binding.tvReviewsCount != null) {
            binding.tvReviewsCount.setOnClickListener(goToReviewTab);
        }
        if (binding.tvRatingNum != null) {
            binding.tvRatingNum.setOnClickListener(goToReviewTab);
        }

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
        binding.btnActionDirections.setOnClickListener(v -> openClinicInGoogleMaps());
        binding.btnActionShare.setOnClickListener(v -> shareDoctorInfo());
        binding.btnActionSave.setOnClickListener(v -> toggleSaveDoctor());
    }

    private void toggleSaveDoctor() {
        if (doctor == null || doctor.getId() == null) return;
        boolean isFav = PreferenceManager.getInstance(this).toggleFavoriteDoctor(doctor.getId());
        updateFavoriteIcon();
        String msg = isFav ? doctor.getName() + " saved to your list!" : doctor.getName() + " removed from saved list";
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void openClinicInGoogleMaps() {
        if (doctor == null) return;

        Uri mapUri;
        if (doctor.hasGpsLocation()) {
            double lat = doctor.getClinicLatitude();
            double lng = doctor.getClinicLongitude();
            String label = (doctor.getClinicName() != null && !doctor.getClinicName().isEmpty())
                    ? doctor.getClinicName() : "Doctor Clinic";
            mapUri = Uri.parse("geo:" + lat + "," + lng + "?q=" + lat + "," + lng + "(" + Uri.encode(label) + ")");
        } else {
            String query = (doctor.getClinicName() != null ? doctor.getClinicName() + " " : "") +
                    (doctor.getLocation() != null ? doctor.getLocation() : "");
            mapUri = Uri.parse("geo:0,0?q=" + Uri.encode(query.trim()));
        }

        try {
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, mapUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
                return;
            }
        } catch (Exception ignored) {}

        try {
            String url;
            if (doctor.hasGpsLocation()) {
                url = "https://www.google.com/maps/search/?api=1&query=" + doctor.getClinicLatitude() + "," + doctor.getClinicLongitude();
            } else {
                String query = (doctor.getClinicName() != null ? doctor.getClinicName() + " " : "") +
                        (doctor.getLocation() != null ? doctor.getLocation() : "");
                url = "https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query.trim());
            }
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open map for " + doctor.getClinicName(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupTabLayout() {
        binding.tabLayout.removeAllTabs();
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Appointment"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Review"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("About"));

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
    }

    private void showTabContent(int position) {
        if (binding == null) return;
        if (binding.layoutTabAppointment != null) {
            binding.layoutTabAppointment.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        }
        if (binding.layoutTabReview != null) {
            binding.layoutTabReview.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
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

    private void loadLiveDoctorRatingAndReviews() {
        if (doctor == null || doctor.getId() == null) return;

        if (binding.pbTabReviewsLoading != null) {
            binding.pbTabReviewsLoading.setVisibility(View.VISIBLE);
        }

        SupabaseClient.getDoctorService().getDoctorReviews("eq." + doctor.getId())
                .enqueue(new Callback<List<DoctorReview>>() {
                    @Override
                    public void onResponse(Call<List<DoctorReview>> call, Response<List<DoctorReview>> response) {
                        if (isFinishing() || binding == null) return;

                        if (binding.pbTabReviewsLoading != null) {
                            binding.pbTabReviewsLoading.setVisibility(View.GONE);
                        }

                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            List<DoctorReview> reviews = response.body();
                            double sum = 0;
                            int positiveCount = 0;
                            for (DoctorReview r : reviews) {
                                sum += r.getRating();
                                if (r.getRating() >= 3.0) {
                                    positiveCount++;
                                }
                            }
                            double avgRating = sum / reviews.size();
                            avgRating = Math.round(avgRating * 10.0) / 10.0;
                            int totalReviews = reviews.size();

                            doctor.setRating(avgRating);
                            doctor.setReviewCount(totalReviews);

                            String avgStr = String.format(Locale.getDefault(), "%.1f ★", avgRating);
                            binding.tvRatingNum.setText(avgStr);
                            if (binding.tvReviewsCount != null) {
                                binding.tvReviewsCount.setText(totalReviews + " Reviews");
                            }

                            int satisfactionPercent = (int) Math.round((positiveCount * 100.0) / totalReviews);
                            if (binding.tvSatisfactionNum != null) {
                                binding.tvSatisfactionNum.setText(satisfactionPercent + "%");
                            }

                            if (binding.tvTabReviewAvgRating != null) {
                                binding.tvTabReviewAvgRating.setText(avgStr);
                            }
                            if (binding.tvTabReviewTotalCount != null) {
                                binding.tvTabReviewTotalCount.setText("Based on " + totalReviews + " review" + (totalReviews > 1 ? "s" : ""));
                            }

                            if (binding.tvTabNoReviews != null) {
                                binding.tvTabNoReviews.setVisibility(View.GONE);
                            }
                            if (binding.rvTabReviewsList != null) {
                                binding.rvTabReviewsList.setVisibility(View.VISIBLE);
                            }
                            if (tabReviewsAdapter != null) {
                                tabReviewsAdapter.submitList(reviews);
                            }
                        } else {
                            doctor.setRating(0.0);
                            doctor.setReviewCount(0);
                            binding.tvRatingNum.setText("0.0 ★");
                            if (binding.tvReviewsCount != null) {
                                binding.tvReviewsCount.setText("0 Reviews");
                            }
                            if (binding.tvSatisfactionNum != null) {
                                binding.tvSatisfactionNum.setText("0%");
                            }
                            if (binding.tvTabReviewAvgRating != null) {
                                binding.tvTabReviewAvgRating.setText("0.0 ★");
                            }
                            if (binding.tvTabReviewTotalCount != null) {
                                binding.tvTabReviewTotalCount.setText("Based on 0 reviews");
                            }
                            if (binding.tvTabNoReviews != null) {
                                binding.tvTabNoReviews.setVisibility(View.VISIBLE);
                            }
                            if (binding.rvTabReviewsList != null) {
                                binding.rvTabReviewsList.setVisibility(View.GONE);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<List<DoctorReview>> call, Throwable t) {
                        if (isFinishing() || binding == null) return;

                        if (binding.pbTabReviewsLoading != null) {
                            binding.pbTabReviewsLoading.setVisibility(View.GONE);
                        }
                        if (binding.tvTabNoReviews != null) {
                            binding.tvTabNoReviews.setVisibility(View.VISIBLE);
                        }
                        if (binding.rvTabReviewsList != null) {
                            binding.rvTabReviewsList.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void loadLivePatientCount() {
        if (doctor == null || doctor.getId() == null) return;

        SupabaseClient.getAppointmentService().getAppointmentsForDoctor("eq." + doctor.getId())
                .enqueue(new Callback<List<Appointment>>() {
                    @Override
                    public void onResponse(Call<List<Appointment>> call, Response<List<Appointment>> response) {
                        if (isFinishing() || binding == null) return;

                        int patientCount = 0;
                        if (response.isSuccessful() && response.body() != null) {
                            List<Appointment> appointments = response.body();
                            Set<String> uniquePatients = new HashSet<>();
                            for (Appointment appt : appointments) {
                                if (appt != null && appt.getPatientId() != null && !appt.getPatientId().trim().isEmpty()) {
                                    uniquePatients.add(appt.getPatientId());
                                }
                            }
                            patientCount = !uniquePatients.isEmpty() ? uniquePatients.size() : appointments.size();
                        }

                        if (doctor.getPatientCount() > patientCount) {
                            patientCount = doctor.getPatientCount();
                        } else {
                            doctor.setPatientCount(patientCount);
                        }

                        if (binding.tvPatientCount != null) {
                            if (patientCount >= 1000) {
                                binding.tvPatientCount.setText(String.format(Locale.getDefault(), "%,d+", patientCount));
                            } else {
                                binding.tvPatientCount.setText(String.valueOf(patientCount));
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Appointment>> call, Throwable t) {
                        // Keep current count
                    }
                });
    }

    private void shareDoctorInfo() {
        if (doctor == null) return;
        String name = doctor.getName() != null ? doctor.getName() : "Doctor";
        String spec = doctor.getSpecializationString() != null ? doctor.getSpecializationString() : "";
        String clinic = doctor.getClinicName() != null ? doctor.getClinicName() : "";
        String location = doctor.getLocation() != null ? doctor.getLocation() : "";

        StringBuilder sb = new StringBuilder();
        sb.append("👨‍⚕️ ").append(name);
        if (!spec.isEmpty()) {
            sb.append(" (").append(spec).append(")");
        }
        sb.append("\n\n📍 Location: ");
        if (!clinic.isEmpty()) {
            sb.append(clinic);
            if (!location.isEmpty()) {
                sb.append(", ");
            }
        }
        if (!location.isEmpty()) {
            sb.append(location);
        } else if (clinic.isEmpty()) {
            sb.append("Main Clinic");
        }
        sb.append("\n\n📱 Book consultation on DoctorPoint app!");

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Doctor Profile: " + name);
        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(shareIntent, "Share Doctor Profile"));
    }
}
