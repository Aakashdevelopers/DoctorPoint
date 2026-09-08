package com.amstudio.drpoint.ui.doctor;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.amstudio.drpoint.R;
import com.amstudio.drpoint.adapter.ClinicPhotoAdapter;
import com.amstudio.drpoint.databinding.ActivityDoctorDetailBinding;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.ui.booking.BookAppointmentActivity;
import com.amstudio.drpoint.util.DummyDataProvider;
import com.amstudio.drpoint.util.PreferenceManager;
import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;

public class DoctorDetailActivity extends AppCompatActivity {

    private ActivityDoctorDetailBinding binding;
    private Doctor doctor;

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

        binding.btnBookAppointment.setOnClickListener(v -> {
            Intent intent = new Intent(DoctorDetailActivity.this, BookAppointmentActivity.class);
            intent.putExtra("doctor", doctor);
            startActivity(intent);
        });
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

        Glide.with(this)
                .load(doctor.getImageRes() != 0 ? doctor.getImageRes() : R.drawable.ic_user)
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .into(binding.ivDoctorImage);
    }

    private void setupActionButtons() {
        binding.btnActionCall.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:9876543210"));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Calling clinic...", Toast.LENGTH_SHORT).show();
            }
        });

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
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Overview"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Patient Stories"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Photos"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Clinic"));

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Toast.makeText(DoctorDetailActivity.this, tab.getText() + " tab selected", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupClinicPhotos() {
        binding.rvClinicPhotos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        ClinicPhotoAdapter photoAdapter = new ClinicPhotoAdapter();
        binding.rvClinicPhotos.setAdapter(photoAdapter);
        photoAdapter.submitList(DummyDataProvider.getClinicPhotoResIds());
    }

    private void shareDoctorInfo() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out " + doctor.getName() + " (" + doctor.getSpecializationString() + ") on Doctor Point app!");
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }
}
