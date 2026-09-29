package com.amstudio.drpoint.ui.doctor;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.amstudio.drpoint.adapter.DoctorListAdapter;
import com.amstudio.drpoint.databinding.ActivitySavedDoctorsBinding;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.network.SupabaseClient;
import com.amstudio.drpoint.ui.booking.BookAppointmentActivity;
import com.amstudio.drpoint.ui.explore.FindDoctorsActivity;
import com.amstudio.drpoint.util.DummyDataProvider;
import com.amstudio.drpoint.util.PreferenceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SavedDoctorsActivity extends AppCompatActivity {

    private ActivitySavedDoctorsBinding binding;
    private DoctorListAdapter adapter;
    private List<Doctor> allDoctorsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySavedDoctorsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.ivBack.setOnClickListener(v -> finish());
        binding.flBackBg.setOnClickListener(v -> finish());

        binding.btnExploreDoctors.setOnClickListener(v -> {
            Intent intent = new Intent(SavedDoctorsActivity.this, FindDoctorsActivity.class);
            startActivity(intent);
            finish();
        });

        setupRecyclerView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSavedDoctors();
    }

    private void setupRecyclerView() {
        binding.rvSavedDoctors.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DoctorListAdapter(false, new DoctorListAdapter.OnDoctorClickListener() {
            @Override
            public void onDoctorClick(Doctor doctor) {
                Intent intent = new Intent(SavedDoctorsActivity.this, DoctorDetailActivity.class);
                intent.putExtra("doctor", doctor);
                startActivity(intent);
            }

            @Override
            public void onBookClick(Doctor doctor) {
                Intent intent = new Intent(SavedDoctorsActivity.this, BookAppointmentActivity.class);
                intent.putExtra("doctor", doctor);
                startActivity(intent);
            }

            @Override
            public void onCallClick(Doctor doctor) {
                Intent intent = new Intent(SavedDoctorsActivity.this, DoctorDetailActivity.class);
                intent.putExtra("doctor", doctor);
                startActivity(intent);
            }

            @Override
            public void onFavoriteClick(Doctor doctor) {
                filterAndDisplaySavedDoctors();
            }
        });
        binding.rvSavedDoctors.setAdapter(adapter);
    }

    private void loadSavedDoctors() {
        binding.pbLoading.setVisibility(View.VISIBLE);

        SupabaseClient.getDoctorService().getDoctors().enqueue(new Callback<List<Doctor>>() {
            @Override
            public void onResponse(Call<List<Doctor>> call, Response<List<Doctor>> response) {
                if (isFinishing() || binding == null) return;
                binding.pbLoading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    allDoctorsList = response.body();
                } else {
                    allDoctorsList = DummyDataProvider.getDoctors();
                }
                filterAndDisplaySavedDoctors();
            }

            @Override
            public void onFailure(Call<List<Doctor>> call, Throwable t) {
                if (isFinishing() || binding == null) return;
                binding.pbLoading.setVisibility(View.GONE);
                allDoctorsList = DummyDataProvider.getDoctors();
                filterAndDisplaySavedDoctors();
            }
        });
    }

    private void filterAndDisplaySavedDoctors() {
        Set<String> savedIds = PreferenceManager.getInstance(this).getFavoriteDoctorIds();

        List<Doctor> savedDoctors = new ArrayList<>();
        if (allDoctorsList != null && savedIds != null && !savedIds.isEmpty()) {
            for (Doctor doc : allDoctorsList) {
                if (doc != null && doc.getId() != null && savedIds.contains(doc.getId())) {
                    savedDoctors.add(doc);
                }
            }
        }

        int count = savedDoctors.size();
        if (count == 0) {
            binding.tvSavedCount.setText("No saved doctors");
            binding.layoutEmptySaved.setVisibility(View.VISIBLE);
            binding.rvSavedDoctors.setVisibility(View.GONE);
        } else {
            binding.tvSavedCount.setText(count + (count == 1 ? " Doctor saved" : " Doctors saved"));
            binding.layoutEmptySaved.setVisibility(View.GONE);
            binding.rvSavedDoctors.setVisibility(View.VISIBLE);
            adapter.submitList(savedDoctors);
        }
    }
}
