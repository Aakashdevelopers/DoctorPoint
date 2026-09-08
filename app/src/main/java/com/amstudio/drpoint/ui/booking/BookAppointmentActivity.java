package com.amstudio.drpoint.ui.booking;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.amstudio.drpoint.adapter.DateChipAdapter;
import com.amstudio.drpoint.adapter.TimeSlotAdapter;
import com.amstudio.drpoint.databinding.ActivityBookAppointmentBinding;
import com.amstudio.drpoint.model.Appointment;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.util.DummyDataProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BookAppointmentActivity extends AppCompatActivity {

    private ActivityBookAppointmentBinding binding;
    private Doctor doctor;
    private String selectedDate = "Today, 04 Sep";
    private String selectedTime = "10:00 AM";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookAppointmentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        doctor = (Doctor) getIntent().getSerializableExtra("doctor");
        if (doctor == null) {
            doctor = DummyDataProvider.getDoctors().get(0);
        }

        binding.ivBack.setOnClickListener(v -> finish());

        populateDoctorSummary();
        setupDatesRecyclerView();
        setupSlotsRecyclerViews();

        binding.btnConfirmBooking.setOnClickListener(v -> {
            String apptId = "appt_" + System.currentTimeMillis();
            Appointment newAppt = new Appointment(
                    apptId,
                    doctor.getId(),
                    doctor.getName(),
                    doctor.getSpecializationString(),
                    selectedDate,
                    selectedTime,
                    doctor.getClinicName(),
                    doctor.getLocation(),
                    "✓ Confirmed",
                    doctor.getFee(),
                    doctor.getImageRes()
            );

            DummyDataProvider.addAppointment(newAppt);

            Intent intent = new Intent(BookAppointmentActivity.this, BookingConfirmedActivity.class);
            intent.putExtra("doctor", doctor);
            intent.putExtra("selected_date", selectedDate);
            intent.putExtra("selected_time", selectedTime);
            startActivity(intent);
        });
    }

    private void populateDoctorSummary() {
        binding.tvDoctorName.setText(doctor.getName());
        binding.tvClinicName.setText(doctor.getClinicName() + " • " + doctor.getLocation());
        binding.tvFee.setText("Consultation Fee: ₹" + doctor.getFee());
        if (doctor.getImageRes() != 0) {
            binding.ivDoctor.setImageResource(doctor.getImageRes());
        }
    }

    private void setupDatesRecyclerView() {
        binding.rvDates.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        List<DateChipAdapter.DateItem> dateList = new ArrayList<>();
        dateList.add(new DateChipAdapter.DateItem("Today", "04 Sep"));
        dateList.add(new DateChipAdapter.DateItem("Tomorrow", "05 Sep"));
        dateList.add(new DateChipAdapter.DateItem("Fri", "06 Sep"));
        dateList.add(new DateChipAdapter.DateItem("Sat", "07 Sep"));
        dateList.add(new DateChipAdapter.DateItem("Sun", "08 Sep"));

        DateChipAdapter dateAdapter = new DateChipAdapter((dateItem, position) ->
                selectedDate = dateItem.getDay() + ", " + dateItem.getDate()
        );
        binding.rvDates.setAdapter(dateAdapter);
        dateAdapter.submitList(dateList);
    }

    private void setupSlotsRecyclerViews() {
        binding.rvMorningSlots.setLayoutManager(new GridLayoutManager(this, 3));
        List<String> morningSlots = Arrays.asList("09:00 AM", "09:30 AM", "10:00 AM", "10:30 AM", "11:00 AM", "11:30 AM");
        TimeSlotAdapter morningAdapter = new TimeSlotAdapter((slot, position) -> selectedTime = slot);
        binding.rvMorningSlots.setAdapter(morningAdapter);
        morningAdapter.submitList(morningSlots);

        binding.rvEveningSlots.setLayoutManager(new GridLayoutManager(this, 3));
        List<String> eveningSlots = Arrays.asList("04:00 PM", "04:30 PM", "05:00 PM", "06:00 PM", "07:00 PM", "08:00 PM");
        TimeSlotAdapter eveningAdapter = new TimeSlotAdapter((slot, position) -> selectedTime = slot);
        binding.rvEveningSlots.setAdapter(eveningAdapter);
        eveningAdapter.submitList(eveningSlots);
    }
}
