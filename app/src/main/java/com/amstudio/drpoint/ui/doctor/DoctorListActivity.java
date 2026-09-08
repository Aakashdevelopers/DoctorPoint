package com.amstudio.drpoint.ui.doctor;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.amstudio.drpoint.R;
import com.amstudio.drpoint.adapter.DoctorListAdapter;
import com.amstudio.drpoint.databinding.ActivityDoctorListBinding;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.ui.booking.BookAppointmentActivity;
import com.amstudio.drpoint.util.DummyDataProvider;
import com.amstudio.drpoint.util.PreferenceManager;

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
                    Intent callIntent = new Intent(Intent.ACTION_DIAL);
                    callIntent.setData(Uri.parse("tel:9876543210"));
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
        TextView[] chips = new TextView[]{binding.chipAll, binding.chipFemale, binding.chipAvailable, binding.chipNearby};
        String[] chipKeys = new String[]{"ALL", "FEMALE", "AVAILABLE", "NEARBY"};

        for (int i = 0; i < chips.length; i++) {
            final int index = i;
            TextView chip = chips[i];
            if (chip == null) continue;

            chip.setOnClickListener(v -> {
                selectedChip = chipKeys[index];
                for (int j = 0; j < chips.length; j++) {
                    if (chips[j] == null) continue;
                    if (j == index) {
                        chips[j].setBackgroundResource(R.drawable.bg_chip_selected);
                        chips[j].setTextColor(ContextCompat.getColor(DoctorListActivity.this, R.color.white));
                    } else {
                        chips[j].setBackgroundResource(R.drawable.bg_chip_unselected);
                        chips[j].setTextColor(ContextCompat.getColor(DoctorListActivity.this, R.color.text_primary));
                    }
                }
                filterAndDisplayDoctors();
            });
        }
    }

    private void loadDoctorsAndFilter() {
        binding.swipeRefreshLayout.setRefreshing(true);
        DummyDataProvider.fetchDoctorsFromSupabase(doctors -> {
            binding.swipeRefreshLayout.setRefreshing(false);
            filterAndDisplayDoctors();
        });
    }

    private void filterAndDisplayDoctors() {
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
