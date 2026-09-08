package com.amstudio.drpoint.ui.explore;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.amstudio.drpoint.adapter.DoctorListAdapter;
import com.amstudio.drpoint.adapter.SpecialitiesAdapter;
import com.amstudio.drpoint.databinding.ActivityFindDoctorsBinding;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.model.Speciality;
import com.amstudio.drpoint.ui.doctor.DoctorDetailActivity;
import com.amstudio.drpoint.ui.doctor.DoctorListActivity;
import com.amstudio.drpoint.util.DummyDataProvider;

import java.util.List;

public class FindDoctorsActivity extends AppCompatActivity {

    private ActivityFindDoctorsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFindDoctorsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.ivBack.setOnClickListener(v -> finish());

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 3) {
                    Intent intent = new Intent(FindDoctorsActivity.this, DoctorListActivity.class);
                    intent.putExtra("search_query", s.toString());
                    startActivity(intent);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Specialities Grid
        binding.rvSpecialities.setLayoutManager(new GridLayoutManager(this, 4));
        List<Speciality> specialities = DummyDataProvider.getSpecialities();
        SpecialitiesAdapter specialitiesAdapter = new SpecialitiesAdapter(speciality -> {
            Intent intent = new Intent(FindDoctorsActivity.this, DoctorListActivity.class);
            intent.putExtra("category_name", speciality.getName());
            startActivity(intent);
        });
        binding.rvSpecialities.setAdapter(specialitiesAdapter);
        specialitiesAdapter.submitList(specialities);

        // Top Doctors Preview (2-Column Grid)
        binding.rvTopDoctors.setLayoutManager(new GridLayoutManager(this, 2));
        DoctorListAdapter doctorListAdapter = new DoctorListAdapter(true, new DoctorListAdapter.OnDoctorClickListener() {
            @Override
            public void onDoctorClick(Doctor doctor) {
                Intent intent = new Intent(FindDoctorsActivity.this, DoctorDetailActivity.class);
                intent.putExtra("doctor", doctor);
                startActivity(intent);
            }

            @Override
            public void onBookClick(Doctor doctor) {
                Intent intent = new Intent(FindDoctorsActivity.this, DoctorDetailActivity.class);
                intent.putExtra("doctor", doctor);
                startActivity(intent);
            }

            @Override
            public void onCallClick(Doctor doctor) {
                try {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:9876543210"));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(FindDoctorsActivity.this, "Calling Dr. " + doctor.getName(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFavoriteClick(Doctor doctor) {}
        });
        binding.rvTopDoctors.setAdapter(doctorListAdapter);

        DummyDataProvider.fetchDoctorsFromSupabase(doctorListAdapter::submitList);

        binding.tvSeeAllDoctors.setOnClickListener(v -> {
            Intent intent = new Intent(FindDoctorsActivity.this, DoctorListActivity.class);
            intent.putExtra("category_name", "All Doctors");
            startActivity(intent);
        });

        binding.btnTryAi.setOnClickListener(v -> {
            Intent intent = new Intent(FindDoctorsActivity.this, DoctorListActivity.class);
            intent.putExtra("category_name", "AI Recommended Doctors");
            startActivity(intent);
        });
    }
}
