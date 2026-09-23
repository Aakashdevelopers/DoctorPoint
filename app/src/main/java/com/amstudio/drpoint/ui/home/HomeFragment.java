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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.amstudio.drpoint.R;
import com.amstudio.drpoint.adapter.DoctorListAdapter;
import com.amstudio.drpoint.adapter.SpecialitiesAdapter;
import com.amstudio.drpoint.databinding.FragmentHomeBinding;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.model.NotificationItem;
import com.amstudio.drpoint.network.SupabaseClient;
import com.amstudio.drpoint.ui.doctor.DoctorDetailActivity;
import com.amstudio.drpoint.ui.doctor.DoctorListActivity;
import com.amstudio.drpoint.ui.explore.FindDoctorsActivity;
import com.amstudio.drpoint.util.DummyDataProvider;
import com.amstudio.drpoint.util.PreferenceManager;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.interfaces.ItemClickListener;
import com.denzcoskun.imageslider.models.SlideModel;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

        updateGreeting();

        // Clicks
        binding.cardCarePlan.setOnClickListener(v -> openFindDoctors());
        binding.layoutSearch.setOnClickListener(v -> openFindDoctors());
        binding.flFilter.setOnClickListener(v -> openFindDoctors());
        binding.flBell.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), NotificationsActivity.class);
            startActivity(intent);
        });
        binding.tvSeeAllDoctors.setOnClickListener(v -> openFindDoctors());

        if (binding.tvSeeAllSpecialities != null) {
            binding.tvSeeAllSpecialities.setOnClickListener(v -> openFindDoctors());
        }



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

        // Explore Specialities Horizontal Carousel
        if (binding.rvSpecialities != null) {
            binding.rvSpecialities.setLayoutManager(new GridLayoutManager(requireContext(), 4));
            SpecialitiesAdapter specialitiesAdapter = new SpecialitiesAdapter(speciality -> {
                Intent intent = new Intent(requireContext(), DoctorListActivity.class);
                intent.putExtra("category_name", speciality.getName());
                startActivity(intent);
            });
            binding.rvSpecialities.setAdapter(specialitiesAdapter);
            DummyDataProvider.fetchSpecialitiesFromSupabase(list -> {
                if (binding == null) return;
                specialitiesAdapter.submitList(list);
                if (binding.shimmerSpecialities != null) {
                    binding.shimmerSpecialities.stopShimmer();
                    binding.shimmerSpecialities.setVisibility(View.GONE);
                }
                binding.rvSpecialities.setVisibility(View.VISIBLE);
            });
        }

        // Available Today Section (Horizontal Carousel)
        if (binding.tvSeeAllAvailable != null) {
            binding.tvSeeAllAvailable.setOnClickListener(v -> openFindDoctors());
        }

        binding.rvAvailableToday.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        DoctorListAdapter availableAdapter = new DoctorListAdapter(true, new DoctorListAdapter.OnDoctorClickListener() {
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
                    String phone = doctor.getDoctorPhone() != null && !doctor.getDoctorPhone().trim().isEmpty()
                            ? doctor.getDoctorPhone().trim()
                            : (doctor.getReceptionPhone() != null && !doctor.getReceptionPhone().trim().isEmpty()
                            ? doctor.getReceptionPhone().trim() : "9876543210");
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + phone));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Calling Dr. " + doctor.getName(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFavoriteClick(Doctor doctor) {}
        });
        binding.rvAvailableToday.setAdapter(availableAdapter);

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
                    String phone = doctor.getDoctorPhone() != null && !doctor.getDoctorPhone().trim().isEmpty()
                            ? doctor.getDoctorPhone().trim()
                            : (doctor.getReceptionPhone() != null && !doctor.getReceptionPhone().trim().isEmpty()
                            ? doctor.getReceptionPhone().trim() : "9876543210");
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + phone));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Calling Dr. " + doctor.getName(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFavoriteClick(Doctor doctor) {}
        });
        binding.rvTopDoctors.setAdapter(doctorAdapter);

        DummyDataProvider.fetchDoctorsFromSupabase(doctors -> {
            if (binding == null) return;
            List<Doctor> availableTodayList = new ArrayList<>();
            for (Doctor d : doctors) {
                if (d.isAvailableToday()) {
                    availableTodayList.add(d);
                }
            }
            if (availableTodayList.isEmpty()) {
                availableTodayList.addAll(doctors);
            }
            availableAdapter.submitList(availableTodayList);
            doctorAdapter.submitList(doctors);

            if (binding.shimmerAvailableToday != null) {
                binding.shimmerAvailableToday.stopShimmer();
                binding.shimmerAvailableToday.setVisibility(View.GONE);
            }
            if (binding.shimmerTopDoctors != null) {
                binding.shimmerTopDoctors.stopShimmer();
                binding.shimmerTopDoctors.setVisibility(View.GONE);
            }
            binding.rvAvailableToday.setVisibility(View.VISIBLE);
            binding.rvTopDoctors.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updateGreeting();
        updateNotificationBadge();
    }

    private void updateNotificationBadge() {
        if (binding == null) return;
        String userId = PreferenceManager.getInstance(requireContext()).getUserId();
        if (userId == null || userId.trim().isEmpty()) return;

        SupabaseClient.getNotificationService().getNotificationsForPatient("eq." + userId)
                .enqueue(new Callback<List<NotificationItem>>() {
                    @Override
                    public void onResponse(Call<List<NotificationItem>> call, Response<List<NotificationItem>> response) {
                        if (binding == null) return;
                        boolean hasUnread = false;
                        if (response.isSuccessful() && response.body() != null) {
                            for (NotificationItem item : response.body()) {
                                if (!item.isRead()) {
                                    hasUnread = true;
                                    break;
                                }
                            }
                        }
                        if (binding.vUnreadBadge != null) {
                            binding.vUnreadBadge.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<NotificationItem>> call, Throwable t) {}
                });
    }

    private void updateGreeting() {
        if (binding == null) return;
        String userName = PreferenceManager.getInstance(requireContext()).getUserName();
        if (userName != null && !userName.isEmpty()) {
            binding.tvUsername.setText(userName + " 👋");
        }
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
