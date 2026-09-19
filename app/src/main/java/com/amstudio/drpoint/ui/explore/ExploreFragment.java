package com.amstudio.drpoint.ui.explore;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.amstudio.drpoint.adapter.DoctorListAdapter;
import com.amstudio.drpoint.adapter.SpecialitiesAdapter;
import com.amstudio.drpoint.databinding.FragmentExploreBinding;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.ui.doctor.DoctorDetailActivity;
import com.amstudio.drpoint.ui.doctor.DoctorListActivity;
import com.amstudio.drpoint.util.DummyDataProvider;

public class ExploreFragment extends Fragment {

    private FragmentExploreBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentExploreBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // AI Symptom Checker Click
        View.OnClickListener openAiSymptomListener = v -> {
            Intent intent = new Intent(requireContext(), DoctorListActivity.class);
            intent.putExtra("category_name", "General Physician");
            startActivity(intent);
            Toast.makeText(requireContext(), "Connecting to AI Doctor Assistant...", Toast.LENGTH_SHORT).show();
        };

        if (binding.cardAiSymptom != null) {
            binding.cardAiSymptom.setOnClickListener(openAiSymptomListener);
        }

        if (binding.tvLocationChip != null) {
            binding.tvLocationChip.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Selected Location: Bangalore", Toast.LENGTH_SHORT).show()
            );
        }

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 3) {
                    Intent intent = new Intent(requireContext(), DoctorListActivity.class);
                    intent.putExtra("search_query", s.toString());
                    startActivity(intent);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Specialities Grid (4 Columns)
        binding.rvSpecialities.setLayoutManager(new GridLayoutManager(requireContext(), 4));
        SpecialitiesAdapter specialitiesAdapter = new SpecialitiesAdapter(speciality -> {
            Intent intent = new Intent(requireContext(), DoctorListActivity.class);
            intent.putExtra("category_name", speciality.getName());
            startActivity(intent);
        });
        binding.rvSpecialities.setAdapter(specialitiesAdapter);
        DummyDataProvider.fetchSpecialitiesFromSupabase(specialitiesAdapter::submitList);

        // Top Doctors Grid (2 Columns)
        binding.rvTopDoctors.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        DoctorListAdapter doctorListAdapter = new DoctorListAdapter(true, new DoctorListAdapter.OnDoctorClickListener() {
            @Override
            public void onDoctorClick(Doctor doctor) {
                Intent intent = new Intent(requireContext(), DoctorDetailActivity.class);
                intent.putExtra("doctor", doctor);
                startActivity(intent);
            }

            @Override
            public void onBookClick(Doctor doctor) {
                Intent intent = new Intent(requireContext(), DoctorDetailActivity.class);
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
                    Toast.makeText(requireContext(), "Calling Dr. " + doctor.getName(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFavoriteClick(Doctor doctor) {}
        });
        binding.rvTopDoctors.setAdapter(doctorListAdapter);

        DummyDataProvider.fetchDoctorsFromSupabase(doctorListAdapter::submitList);

        binding.tvSeeAllDoctors.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), DoctorListActivity.class);
            intent.putExtra("category_name", "All Doctors");
            startActivity(intent);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
