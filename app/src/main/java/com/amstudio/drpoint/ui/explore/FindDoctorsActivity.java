package com.amstudio.drpoint.ui.explore;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import com.amstudio.drpoint.R;
import com.amstudio.drpoint.adapter.DoctorListAdapter;
import com.amstudio.drpoint.adapter.SpecialitiesAdapter;
import com.amstudio.drpoint.databinding.ActivityFindDoctorsBinding;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.model.Speciality;
import com.amstudio.drpoint.ui.booking.BookAppointmentActivity;
import com.amstudio.drpoint.ui.doctor.DoctorDetailActivity;
import com.amstudio.drpoint.ui.doctor.DoctorListActivity;
import com.amstudio.drpoint.util.DummyDataProvider;
import com.amstudio.drpoint.util.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class FindDoctorsActivity extends AppCompatActivity {

    private ActivityFindDoctorsBinding binding;
    private DoctorListAdapter doctorListAdapter;
    private SpecialitiesAdapter specialitiesAdapter;
    private String searchQuery = "";
    private String selectedCategory = "All Doctors";
    private String searchMode = "ALL"; // ALL, DOCTORS, SPECIALISATIONS
    private List<Speciality> fetchedSpecialities = new ArrayList<>();

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

        // Search Input TextWatcher for Real-time Doctor & Speciality Filtering
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

        setupSearchModeChips();
        setupFilterChips();

        // Specialities Grid (Live Supabase Data)
        binding.rvSpecialities.setLayoutManager(new GridLayoutManager(this, 4));
        specialitiesAdapter = new SpecialitiesAdapter(speciality -> {
            selectedCategory = speciality.getName();
            searchMode = "DOCTORS";
            updateSearchModeChipStyles();
            filterAndDisplayDoctors();
        });
        binding.rvSpecialities.setAdapter(specialitiesAdapter);
        DummyDataProvider.fetchSpecialitiesFromSupabase(list -> {
            fetchedSpecialities = list != null ? list : new ArrayList<>();
            filterAndDisplayDoctors();
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
                    Toast.makeText(FindDoctorsActivity.this, "Calling Dr. " + doctor.getName(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFavoriteClick(Doctor doctor) {
                boolean isFav = PreferenceManager.getInstance(FindDoctorsActivity.this).toggleFavoriteDoctor(doctor.getId());
                Toast.makeText(FindDoctorsActivity.this, isFav ? "Added to Favorites" : "Removed from Favorites", Toast.LENGTH_SHORT).show();
            }
        });
        binding.rvTopDoctors.setAdapter(doctorListAdapter);

        binding.tvSeeAllDoctors.setOnClickListener(v -> {
            Intent intent = new Intent(FindDoctorsActivity.this, DoctorListActivity.class);
            intent.putExtra("category_name", selectedCategory);
            intent.putExtra("search_query", searchQuery);
            startActivity(intent);
        });

        loadDoctorsFromSupabase();
    }

    private void setupSearchModeChips() {
        binding.chipSearchAll.setOnClickListener(v -> setSearchMode("ALL"));
        binding.chipSearchDoctors.setOnClickListener(v -> setSearchMode("DOCTORS"));
        binding.chipSearchSpecs.setOnClickListener(v -> setSearchMode("SPECIALISATIONS"));
    }

    private void setSearchMode(String mode) {
        searchMode = mode;
        updateSearchModeChipStyles();
        filterAndDisplayDoctors();
    }

    private void updateSearchModeChipStyles() {
        updateChipStyle(binding.chipSearchAll, "ALL".equals(searchMode));
        updateChipStyle(binding.chipSearchDoctors, "DOCTORS".equals(searchMode));
        updateChipStyle(binding.chipSearchSpecs, "SPECIALISATIONS".equals(searchMode));
    }

    private void updateChipStyle(TextView chip, boolean isSelected) {
        if (chip == null) return;
        if (isSelected) {
            chip.setBackgroundResource(R.drawable.bg_chip_selected);
            chip.setTextColor(ContextCompat.getColor(this, R.color.white));
            chip.setTypeface(null, Typeface.BOLD);
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_unselected);
            chip.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            chip.setTypeface(null, Typeface.NORMAL);
        }
    }

    private void setupFilterChips() {
        if (binding.llChipContainer == null) return;

        DummyDataProvider.fetchSpecialitiesFromSupabase(specialities -> {
            if (!isFinishing() && binding != null && binding.llChipContainer != null) {
                populateSpecialityChips(specialities);
            }
        });
    }

    private void populateSpecialityChips(List<Speciality> specialities) {
        binding.llChipContainer.removeAllViews();

        List<String> categories = new ArrayList<>();
        categories.add("All Doctors");
        categories.add("Available Today");
        if (specialities != null) {
            for (Speciality s : specialities) {
                if (s != null && s.getName() != null && !s.getName().trim().isEmpty()) {
                    if (!categories.contains(s.getName())) {
                        categories.add(s.getName());
                    }
                }
            }
        }

        List<TextView> chipViews = new ArrayList<>();

        for (String catName : categories) {
            TextView chip = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(0, 0, dpToPx(8), 0);
            chip.setLayoutParams(lp);

            chip.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
            chip.setTextSize(13);
            chip.setClickable(true);
            chip.setFocusable(true);

            String displayName = "All Doctors".equalsIgnoreCase(catName) ? "All" : catName;
            chip.setText(displayName);

            boolean isSelected = selectedCategory.equalsIgnoreCase(catName);
            if (isSelected) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected);
                chip.setTextColor(ContextCompat.getColor(this, R.color.white));
                chip.setTypeface(null, Typeface.BOLD);
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_unselected);
                chip.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                chip.setTypeface(null, Typeface.NORMAL);
            }

            chip.setOnClickListener(v -> {
                selectedCategory = catName;

                for (TextView cv : chipViews) {
                    cv.setBackgroundResource(R.drawable.bg_chip_unselected);
                    cv.setTextColor(ContextCompat.getColor(FindDoctorsActivity.this, R.color.text_primary));
                    cv.setTypeface(null, Typeface.NORMAL);
                }
                chip.setBackgroundResource(R.drawable.bg_chip_selected);
                chip.setTextColor(ContextCompat.getColor(FindDoctorsActivity.this, R.color.white));
                chip.setTypeface(null, Typeface.BOLD);

                filterAndDisplayDoctors();
            });

            binding.llChipContainer.addView(chip);
            chipViews.add(chip);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void loadDoctorsFromSupabase() {
        DummyDataProvider.fetchDoctorsFromSupabase(doctors -> filterAndDisplayDoctors());
    }

    private void filterAndDisplayDoctors() {
        if (binding == null) return;

        // 1. Filter Specialities based on search query
        List<Speciality> filteredSpecs = new ArrayList<>();
        if (fetchedSpecialities != null) {
            for (Speciality s : fetchedSpecialities) {
                if (searchQuery.isEmpty() || (s.getName() != null && s.getName().toLowerCase().contains(searchQuery.toLowerCase()))) {
                    filteredSpecs.add(s);
                }
            }
        }

        if (specialitiesAdapter != null) {
            specialitiesAdapter.submitList(filteredSpecs);
        }

        // 2. Filter Doctors based on search query & selected category
        String chipFilter = "ALL";
        if ("Available Today".equalsIgnoreCase(selectedCategory)) {
            chipFilter = "TODAY";
        }

        List<Doctor> filteredDoctors = DummyDataProvider.getFilteredDoctors(searchQuery, selectedCategory, chipFilter);

        // 3. Toggle Visibility based on Search Mode
        if ("DOCTORS".equals(searchMode)) {
            binding.tvSpecialityHeader.setVisibility(View.GONE);
            binding.rvSpecialities.setVisibility(View.GONE);

            binding.tvResultCount.setVisibility(View.VISIBLE);
            binding.rvTopDoctors.setVisibility(filteredDoctors.isEmpty() ? View.GONE : View.VISIBLE);
            binding.llEmptyState.setVisibility(filteredDoctors.isEmpty() ? View.VISIBLE : View.GONE);
            binding.tvResultCount.setText(filteredDoctors.size() + " Doctors found");

        } else if ("SPECIALISATIONS".equals(searchMode)) {
            binding.tvResultCount.setVisibility(View.GONE);
            binding.rvTopDoctors.setVisibility(View.GONE);

            binding.tvSpecialityHeader.setVisibility(View.VISIBLE);
            binding.rvSpecialities.setVisibility(filteredSpecs.isEmpty() ? View.GONE : View.VISIBLE);
            binding.llEmptyState.setVisibility(filteredSpecs.isEmpty() ? View.GONE : View.VISIBLE);

        } else {
            // ALL Results Mode
            binding.tvSpecialityHeader.setVisibility(filteredSpecs.isEmpty() ? View.GONE : View.VISIBLE);
            binding.rvSpecialities.setVisibility(filteredSpecs.isEmpty() ? View.GONE : View.VISIBLE);

            binding.tvResultCount.setVisibility(filteredDoctors.isEmpty() ? View.GONE : View.VISIBLE);
            binding.rvTopDoctors.setVisibility(filteredDoctors.isEmpty() ? View.GONE : View.VISIBLE);

            boolean bothEmpty = filteredSpecs.isEmpty() && filteredDoctors.isEmpty();
            binding.llEmptyState.setVisibility(bothEmpty ? View.VISIBLE : View.GONE);
            binding.tvResultCount.setText(filteredDoctors.size() + " Doctors found");
        }

        if (doctorListAdapter != null) {
            doctorListAdapter.submitList(filteredDoctors);
        }
    }
}
