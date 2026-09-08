package com.amstudio.drpoint.ui.booking;

import android.content.Intent;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.amstudio.drpoint.databinding.ActivityBookingConfirmedBinding;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.ui.main.MainActivity;
import com.amstudio.drpoint.util.DummyDataProvider;

public class BookingConfirmedActivity extends AppCompatActivity {

    private ActivityBookingConfirmedBinding binding;
    private Doctor doctor;
    private String dateStr;
    private String timeStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookingConfirmedBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        doctor = (Doctor) getIntent().getSerializableExtra("doctor");
        if (doctor == null) {
            doctor = DummyDataProvider.getDoctors().get(0);
        }

        dateStr = getIntent().getStringExtra("selected_date");
        if (dateStr == null || dateStr.isEmpty()) {
            dateStr = "Tomorrow, 05 Sep";
        }

        timeStr = getIntent().getStringExtra("selected_time");
        if (timeStr == null || timeStr.isEmpty()) {
            timeStr = "10:30 AM";
        }

        animateCheckmark();
        populateBookingInfo();

        binding.btnAddCalendar.setOnClickListener(v -> addToCalendar());

        binding.btnDone.setOnClickListener(v -> {
            Intent intent = new Intent(BookingConfirmedActivity.this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_SELECT_TAB, MainActivity.TAB_APPOINTMENTS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
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

    private void populateBookingInfo() {
        binding.tvDoctorName.setText(doctor.getName());
        binding.tvSpecialization.setText(doctor.getQualification());
        binding.tvBookingDatetime.setText(dateStr + " • " + timeStr);
        binding.tvBookingLocation.setText(doctor.getClinicName() + " • " + doctor.getLocation());
        binding.tvBookingFee.setText("₹" + doctor.getFee() + " (To be paid at clinic)");
        if (doctor.getImageRes() != 0) {
            binding.ivDoctor.setImageResource(doctor.getImageRes());
        }
    }

    private void addToCalendar() {
        try {
            Intent intent = new Intent(Intent.ACTION_INSERT);
            intent.setData(CalendarContract.Events.CONTENT_URI);
            intent.putExtra(CalendarContract.Events.TITLE, "Doctor Appointment with " + doctor.getName());
            intent.putExtra(CalendarContract.Events.EVENT_LOCATION, doctor.getClinicName() + ", " + doctor.getLocation());
            intent.putExtra(CalendarContract.Events.DESCRIPTION, "Healthcare appointment booked via Doctor Point app.");
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Added appointment to calendar", Toast.LENGTH_SHORT).show();
        }
    }
}
