package com.amstudio.drpoint.ui.doctor;

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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.amstudio.drpoint.R;
import com.amstudio.drpoint.adapter.DoctorListAdapter;
import com.amstudio.drpoint.databinding.ActivityDoctorListBinding;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.model.Speciality;
import com.amstudio.drpoint.ui.booking.BookAppointmentActivity;
import com.amstudio.drpoint.util.DummyDataProvider;
import com.amstudio.drpoint.util.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class DoctorListActivity extends AppCompatActivity {

    private ActivityDoctorListBinding binding;
    private DoctorListAdapter adapter;
    private String categoryName = "All Doctors";
    private String searchQuery = "";
    private String selectedChip = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDoctorListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getIntent().hasExtra("category_name")) {
            categoryName = getIntent().getStringExtra("category_name");
        } else if (getIntent().hasExtra("category")) {
            categoryName = getIntent().getStringExtra("category");
        }
        if (getIntent().hasExtra("search_query")) {
            searchQuery = getIntent().getStringExtra("search_query");
            binding.etSearch.setText(searchQuery);
            binding.llSearchContainer.setVisibility(View.VISIBLE);
        }

        binding.tvCategoryTitle.setText(categoryName);
        binding.ivBack.setOnClickListener(v -> finish());

        binding.ivSearch.setOnClickListener(v -> {
            if (binding.llSearchContainer.getVisibility() == View.VISIBLE) {
                binding.llSearchContainer.setVisibility(View.GONE);
                searchQuery = "";
                binding.etSearch.setText("");
                filterAndDisplayDoctors();
            } else {
                binding.llSearchContainer.setVisibility(View.VISIBLE);
                binding.etSearch.requestFocus();
            }
        });

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString();
                filterAndDisplayDoctors();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        setupFilterChips();
        setupRecyclerView();

        binding.swipeRefreshLayout.setOnRefreshListener(this::loadDoctorsAndFilter);

        loadDoctorsAndFilter();
    }

    private void setupRecyclerView() {
        binding.rvDoctorList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DoctorListAdapter(false, new DoctorListAdapter.OnDoctorClickListener() {
            @Override
            public void onDoctorClick(Doctor doctor) {
                Intent intent = new Intent(DoctorListActivity.this, DoctorDetailActivity.class);
                intent.putExtra("doctor", doctor);
                startActivity(intent);
            }

            @Override
            public void onBookClick(Doctor doctor) {
                Intent intent = new Intent(DoctorListActivity.this, BookAppointmentActivity.class);
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
                    Intent callIntent = new Intent(Intent.ACTION_DIAL);
                    callIntent.setData(Uri.parse("tel:" + phone));
                    startActivity(callIntent);
                } catch (Exception e) {
                    Toast.makeText(DoctorListActivity.this, "Calling Dr. " + doctor.getName(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFavoriteClick(Doctor doctor) {
                boolean isFav = PreferenceManager.getInstance(DoctorListActivity.this).isFavoriteDoctor(doctor.getId());
                String msg = isFav ? "Added to favorites" : "Removed from favorites";
                Toast.makeText(DoctorListActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
        binding.rvDoctorList.setAdapter(adapter);
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

            boolean isSelected = isCategoryMatching(categoryName, catName);
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
                categoryName = catName;
                binding.tvCategoryTitle.setText("All Doctors".equalsIgnoreCase(catName) ? "All Doctors" : catName);

                for (TextView cv : chipViews) {
                    cv.setBackgroundResource(R.drawable.bg_chip_unselected);
                    cv.setTextColor(ContextCompat.getColor(DoctorListActivity.this, R.color.text_primary));
                    cv.setTypeface(null, Typeface.NORMAL);
                }
                chip.setBackgroundResource(R.drawable.bg_chip_selected);
                chip.setTextColor(ContextCompat.getColor(DoctorListActivity.this, R.color.white));
                chip.setTypeface(null, Typeface.BOLD);

                filterAndDisplayDoctors();
            });

            binding.llChipContainer.addView(chip);
            chipViews.add(chip);
        }
    }

    private boolean isCategoryMatching(String cat1, String cat2) {
        if (cat1 == null || cat2 == null) return false;
        if (cat1.equalsIgnoreCase(cat2)) return true;
        if ("All Doctors".equalsIgnoreCase(cat1) && ("All".equalsIgnoreCase(cat2) || "All Doctors".equalsIgnoreCase(cat2))) return true;
        if ("All".equalsIgnoreCase(cat1) && ("All".equalsIgnoreCase(cat2) || "All Doctors".equalsIgnoreCase(cat2))) return true;

        Doctor dummyDoc = new Doctor();
        dummyDoc.setSpecialization(cat1);
        return DummyDataProvider.isDoctorMatchingCategory(dummyDoc, cat2);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void loadDoctorsAndFilter() {
        binding.swipeRefreshLayout.setRefreshing(true);
        DummyDataProvider.fetchDoctorsFromSupabase(doctors -> {
            binding.swipeRefreshLayout.setRefreshing(false);
            filterAndDisplayDoctors();
        });
    }

    private void filterAndDisplayDoctors() {
        if (binding.shimmerDoctorList != null) {
            binding.shimmerDoctorList.stopShimmer();
            binding.shimmerDoctorList.setVisibility(View.GONE);
        }

        List<Doctor> filtered = DummyDataProvider.getFilteredDoctors(searchQuery, categoryName, selectedChip);

        if (filtered.isEmpty()) {
            binding.llEmptyState.setVisibility(View.VISIBLE);
            binding.rvDoctorList.setVisibility(View.GONE);
            binding.tvResultCount.setText("0 Doctors found");
        } else {
            binding.llEmptyState.setVisibility(View.GONE);
            binding.rvDoctorList.setVisibility(View.VISIBLE);
            binding.tvResultCount.setText(filtered.size() + " Doctors found");
        }

        adapter.submitList(filtered);
    }
}
