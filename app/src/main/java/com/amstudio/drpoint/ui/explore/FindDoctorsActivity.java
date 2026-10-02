package com.amstudio.drpoint.ui.explore;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.amstudio.drpoint.adapter.DoctorListAdapter;
import com.amstudio.drpoint.databinding.ActivityFindDoctorsBinding;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.ui.booking.BookAppointmentActivity;
import com.amstudio.drpoint.ui.doctor.DoctorDetailActivity;
import com.amstudio.drpoint.ui.doctor.DoctorListActivity;
import com.amstudio.drpoint.util.DummyDataProvider;
import com.amstudio.drpoint.util.PreferenceManager;
import com.amstudio.drpoint.util.ToastUtils;

import java.util.List;

public class FindDoctorsActivity extends AppCompatActivity {

    private ActivityFindDoctorsBinding binding;
    private DoctorListAdapter doctorListAdapter;
    private String searchQuery = "";
    private String selectedCategory = "All Doctors";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFindDoctorsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.ivBack.setOnClickListener(v -> finish());

        // Clear Search Button Listener
        binding.ivClearSearch.setOnClickListener(v -> {
            binding.etSearch.setText("");
            searchQuery = "";
            filterAndDisplayDoctors();
        });

        // Search Input TextWatcher for Real-time Doctor Filtering
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim();
                binding.ivClearSearch.setVisibility(searchQuery.length() > 0 ? View.VISIBLE : View.GONE);
                filterAndDisplayDoctors();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Top Doctors Grid (Live Supabase Data)
        binding.rvTopDoctors.setLayoutManager(new GridLayoutManager(this, 2));
        doctorListAdapter = new DoctorListAdapter(true, new DoctorListAdapter.OnDoctorClickListener() {
            @Override
            public void onDoctorClick(Doctor doctor) {
                Intent intent = new Intent(FindDoctorsActivity.this, DoctorDetailActivity.class);
                intent.putExtra("doctor", doctor);
                startActivity(intent);
            }

            @Override
            public void onBookClick(Doctor doctor) {
                Intent intent = new Intent(FindDoctorsActivity.this, BookAppointmentActivity.class);
                intent.putExtra("doctor", doctor);
                startActivity(intent);
            }

            @Override
            public void onCallClick(Doctor doctor) {
                try {
                    String phone = doctor.getDoctorPhone() != null && !doctor.getDoctorPhone().trim().isEmpty()
                            ? doctor.getDoctorPhone().trim()
                            : (doctor.getReceptionPhone() != null && !doctor.getReceptionPhone().trim().isEmpty()
                            ? doctor.getReceptionPhone().trim() : "9876543210");
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + phone));
                    startActivity(intent);
                } catch (Exception e) {
                    ToastUtils.showInfo(FindDoctorsActivity.this, "Calling Dr. " + doctor.getName());
                }
            }

            @Override
            public void onFavoriteClick(Doctor doctor) {
                boolean isFav = PreferenceManager.getInstance(FindDoctorsActivity.this).toggleFavoriteDoctor(doctor.getId());
                ToastUtils.showSuccess(FindDoctorsActivity.this, isFav ? "Added to Favorites" : "Removed from Favorites");
            }
        });
        binding.rvTopDoctors.setAdapter(doctorListAdapter);

        binding.tvSeeAllDoctors.setOnClickListener(v -> {
            Intent intent = new Intent(FindDoctorsActivity.this, DoctorListActivity.class);
            intent.putExtra("category_name", selectedCategory);
            intent.putExtra("search_query", searchQuery);
            startActivity(intent);
        });

        if (binding.tvLocationChip != null) {
            binding.tvLocationChip.setOnClickListener(v ->
                    DummyDataProvider.showStatePickerDialog(FindDoctorsActivity.this, state -> updateLocationChipAndFilter())
            );
        }

        updateLocationChipAndFilter();
        loadDoctorsFromSupabase();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLocationChipAndFilter();
    }

    private void updateLocationChipAndFilter() {
        if (binding == null) return;
        String selectedState = PreferenceManager.getInstance(this).getSelectedState();
        if (binding.tvLocationChip != null) {
            binding.tvLocationChip.setText("📍 " + selectedState + " ▾");
        }
        filterAndDisplayDoctors();
    }

    private void loadDoctorsFromSupabase() {
        DummyDataProvider.fetchDoctorsFromSupabase(doctors -> filterAndDisplayDoctors());
    }

    private void filterAndDisplayDoctors() {
        if (binding == null) return;

        String selectedState = PreferenceManager.getInstance(this).getSelectedState();
        List<Doctor> filteredDoctors = DummyDataProvider.getFilteredDoctors(searchQuery, selectedCategory, "ALL", selectedState);

        binding.tvResultCount.setVisibility(View.VISIBLE);
        binding.tvResultCount.setText(filteredDoctors.size() + " Doctors found");

        if (filteredDoctors.isEmpty()) {
            binding.rvTopDoctors.setVisibility(View.GONE);
            binding.llEmptyState.setVisibility(View.VISIBLE);
        } else {
            binding.rvTopDoctors.setVisibility(View.VISIBLE);
            binding.llEmptyState.setVisibility(View.GONE);
        }

        if (doctorListAdapter != null) {
            doctorListAdapter.submitList(filteredDoctors);
        }
    }
}
