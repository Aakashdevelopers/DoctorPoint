package com.amstudio.drpoint.ui.home;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.amstudio.drpoint.R;
import com.amstudio.drpoint.adapter.DoctorListAdapter;
import com.amstudio.drpoint.adapter.QuickAccessAdapter;
import com.amstudio.drpoint.databinding.FragmentHomeBinding;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.ui.doctor.DoctorDetailActivity;
import com.amstudio.drpoint.ui.explore.FindDoctorsActivity;
import com.amstudio.drpoint.util.DummyDataProvider;
import com.amstudio.drpoint.util.PreferenceManager;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.interfaces.ItemClickListener;
import com.denzcoskun.imageslider.models.SlideModel;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String userName = PreferenceManager.getInstance(requireContext()).getUserName();
        if (userName != null && !userName.isEmpty()) {
            binding.tvUsername.setText(userName + " 👋");
        }

        // Clicks
        binding.cardCarePlan.setOnClickListener(v -> openFindDoctors());
        binding.layoutSearch.setOnClickListener(v -> openFindDoctors());
        binding.flFilter.setOnClickListener(v -> openFindDoctors());
        binding.flBell.setOnClickListener(v -> Toast.makeText(requireContext(), "No new notifications", Toast.LENGTH_SHORT).show());
        binding.tvSeeAllDoctors.setOnClickListener(v -> openFindDoctors());

        // Image Slideshow Setup (denzcoskun/ImageSlideshow)
        List<SlideModel> slideList = new ArrayList<>();
        slideList.add(new SlideModel(R.drawable.banner_1, ScaleTypes.FIT));
        binding.imageSlider.setImageList(slideList, ScaleTypes.FIT);
        binding.imageSlider.setItemClickListener(new ItemClickListener() {
            @Override
            public void onItemSelected(int position) {
                openFindDoctors();
            }

            @Override
            public void doubleClick(int position) {
                openFindDoctors();
            }
        });

        // Quick Access Grid (4 Columns)
        binding.rvQuickAccess.setLayoutManager(new GridLayoutManager(requireContext(), 4));
        QuickAccessAdapter quickAccessAdapter = new QuickAccessAdapter(item -> {
            Toast.makeText(requireContext(), "Selected: " + item.getTitle(), Toast.LENGTH_SHORT).show();
            openFindDoctors();
        });
        binding.rvQuickAccess.setAdapter(quickAccessAdapter);
        quickAccessAdapter.submitList(DummyDataProvider.getQuickAccessItems());

        // Top Doctors Section (2-Column Grid)
        binding.rvTopDoctors.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        DoctorListAdapter doctorAdapter = new DoctorListAdapter(true, new DoctorListAdapter.OnDoctorClickListener() {
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
        binding.rvTopDoctors.setAdapter(doctorAdapter);

        DummyDataProvider.fetchDoctorsFromSupabase(doctorAdapter::submitList);
    }

    private void openFindDoctors() {
        Intent intent = new Intent(requireContext(), FindDoctorsActivity.class);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
