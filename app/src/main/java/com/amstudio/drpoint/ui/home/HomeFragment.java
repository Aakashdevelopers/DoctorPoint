package com.amstudio.drpoint.ui.home;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import com.amstudio.drpoint.R;
import com.amstudio.drpoint.adapter.DoctorListAdapter;
import com.amstudio.drpoint.adapter.QuickAccessAdapter;
import com.amstudio.drpoint.adapter.SpecialitiesAdapter;
import com.amstudio.drpoint.databinding.FragmentHomeBinding;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.model.NotificationItem;
import com.amstudio.drpoint.model.PatientProfile;
import com.amstudio.drpoint.network.SupabaseClient;
import com.amstudio.drpoint.ui.doctor.DoctorDetailActivity;
import com.amstudio.drpoint.ui.doctor.DoctorListActivity;
import com.amstudio.drpoint.ui.explore.FindDoctorsActivity;
import com.amstudio.drpoint.util.DummyDataProvider;
import com.amstudio.drpoint.util.PreferenceManager;
import com.amstudio.drpoint.util.ToastUtils;
import com.bumptech.glide.Glide;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.interfaces.ItemClickListener;
import com.denzcoskun.imageslider.models.SlideModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private DoctorListAdapter availableAdapter;
    private DoctorListAdapter doctorAdapter;

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

        if (binding.llLocationContainer != null) {
            binding.llLocationContainer.setOnClickListener(v -> {
                Context context = getContext();
                if (context != null) {
                    DummyDataProvider.showStatePickerDialog(context, state -> updateLocationChipAndDoctors());
                }
            });
        }
        updateLocationChipText();

        // Clicks
        binding.cardCarePlan.setOnClickListener(v -> openFindDoctors());
        binding.layoutSearch.setOnClickListener(v -> openFindDoctors());
        binding.flFilter.setOnClickListener(v -> openFindDoctors());

        // Quick Services Grid Setup


        binding.flBell.setOnClickListener(v -> {
            Context context = getContext();
            if (context != null) {
                Intent intent = new Intent(context, NotificationsActivity.class);
                startActivity(intent);
            }
        });
        binding.tvSeeAllDoctors.setOnClickListener(v -> openDoctorList("Top Rated Doctors"));

        if (binding.tvSeeAllSpecialities != null) {
            binding.tvSeeAllSpecialities.setOnClickListener(v -> showSpecialitiesBottomSheet());
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
            Context context = getContext();
            if (context != null) {
                binding.rvSpecialities.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
                SpecialitiesAdapter specialitiesAdapter = new SpecialitiesAdapter(speciality -> {
                    openDoctorList(speciality.getName());
                });
                binding.rvSpecialities.setAdapter(specialitiesAdapter);
                DummyDataProvider.fetchSpecialitiesFromSupabase(list -> {
                    if (binding == null || !isAdded()) return;
                    specialitiesAdapter.submitList(list);
                    if (binding.shimmerSpecialities != null) {
                        binding.shimmerSpecialities.stopShimmer();
                        binding.shimmerSpecialities.setVisibility(View.GONE);
                    }
                    binding.rvSpecialities.setVisibility(View.VISIBLE);
                });
            }
        }

        // Available Today Section (Horizontal Carousel)
        if (binding.tvSeeAllAvailable != null) {
            binding.tvSeeAllAvailable.setOnClickListener(v -> openDoctorList("Available Today"));
        }

        Context context = getContext();
        if (context != null) {
            binding.rvAvailableToday.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
            availableAdapter = new DoctorListAdapter(true, new DoctorListAdapter.OnDoctorClickListener() {
                @Override
                public void onDoctorClick(Doctor doctor) {
                    Context ctx = getContext();
                    if (ctx != null) {
                        Intent intent = new Intent(ctx, DoctorDetailActivity.class);
                        intent.putExtra("doctor", doctor);
                        startActivity(intent);
                    }
                }

                @Override
                public void onBookClick(Doctor doctor) {
                    Context ctx = getContext();
                    if (ctx != null) {
                        Intent intent = new Intent(ctx, DoctorDetailActivity.class);
                        intent.putExtra("doctor", doctor);
                        startActivity(intent);
                    }
                }

                @Override
                public void onCallClick(Doctor doctor) {
                    Context ctx = getContext();
                    if (ctx == null) return;
                    try {
                        String phone = doctor.getDoctorPhone() != null && !doctor.getDoctorPhone().trim().isEmpty()
                                ? doctor.getDoctorPhone().trim()
                                : (doctor.getReceptionPhone() != null && !doctor.getReceptionPhone().trim().isEmpty()
                                ? doctor.getReceptionPhone().trim() : "9876543210");
                        Intent intent = new Intent(Intent.ACTION_DIAL);
                        intent.setData(Uri.parse("tel:" + phone));
                        startActivity(intent);
                    } catch (Exception e) {
                        ToastUtils.showInfo(ctx, "Calling Dr. " + doctor.getName());
                    }
                }

                @Override
                public void onFavoriteClick(Doctor doctor) {}
            });
            binding.rvAvailableToday.setAdapter(availableAdapter);

            // Top Doctors Section (Horizontal Carousel)
            binding.rvTopDoctors.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
            doctorAdapter = new DoctorListAdapter(true, new DoctorListAdapter.OnDoctorClickListener() {
                @Override
                public void onDoctorClick(Doctor doctor) {
                    Context ctx = getContext();
                    if (ctx != null) {
                        Intent intent = new Intent(ctx, DoctorDetailActivity.class);
                        intent.putExtra("doctor", doctor);
                        startActivity(intent);
                    }
                }

                @Override
                public void onBookClick(Doctor doctor) {
                    Context ctx = getContext();
                    if (ctx != null) {
                        Intent intent = new Intent(ctx, DoctorDetailActivity.class);
                        intent.putExtra("doctor", doctor);
                        startActivity(intent);
                    }
                }

                @Override
                public void onCallClick(Doctor doctor) {
                    Context ctx = getContext();
                    if (ctx == null) return;
                    try {
                        String phone = doctor.getDoctorPhone() != null && !doctor.getDoctorPhone().trim().isEmpty()
                                ? doctor.getDoctorPhone().trim()
                                : (doctor.getReceptionPhone() != null && !doctor.getReceptionPhone().trim().isEmpty()
                                ? doctor.getReceptionPhone().trim() : "9876543210");
                        Intent intent = new Intent(Intent.ACTION_DIAL);
                        intent.setData(Uri.parse("tel:" + phone));
                        startActivity(intent);
                    } catch (Exception e) {
                        ToastUtils.showInfo(ctx, "Calling Dr. " + doctor.getName());
                    }
                }

                @Override
                public void onFavoriteClick(Doctor doctor) {}
            });
            binding.rvTopDoctors.setAdapter(doctorAdapter);
        }

        loadDoctors();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateGreeting();
        updateNotificationBadge();
        updateLocationChipAndDoctors();
    }

    private void updateLocationChipText() {
        if (binding == null || !isAdded()) return;
        Context context = getContext();
        if (context == null) return;
        String selectedState = PreferenceManager.getInstance(context).getSelectedState();
        if (binding.tvLocationChip != null) {
            binding.tvLocationChip.setText("📍 " + selectedState + " ▾");
        }
    }

    private void updateLocationChipAndDoctors() {
        updateLocationChipText();
        loadDoctors();
    }

    private void loadDoctors() {
        if (binding == null || !isAdded()) return;
        Context context = getContext();
        if (context == null) return;
        String currentState = PreferenceManager.getInstance(context).getSelectedState();

        DummyDataProvider.fetchDoctorsFromSupabase(doctors -> {
            if (binding == null || !isAdded()) return;

            List<Doctor> stateFilteredDoctors = DummyDataProvider.filterDoctorsByState(doctors, currentState);

            List<Doctor> availableTodayList = new ArrayList<>();
            for (Doctor d : stateFilteredDoctors) {
                if (d.isAvailableToday()) {
                    availableTodayList.add(d);
                }
            }
            if (availableTodayList.isEmpty()) {
                availableTodayList.addAll(stateFilteredDoctors);
            }

            List<Doctor> topRatedList = new ArrayList<>();
            for (Doctor d : stateFilteredDoctors) {
                if (d.getRating() > 2.0) {
                    topRatedList.add(d);
                }
            }

            Collections.sort(topRatedList, (d1, d2) -> {
                int reviewCompare = Integer.compare(d2.getReviewCount(), d1.getReviewCount());
                if (reviewCompare != 0) {
                    return reviewCompare;
                }
                return Double.compare(d2.getRating(), d1.getRating());
            });

            if (topRatedList.isEmpty()) {
                topRatedList.addAll(stateFilteredDoctors);
                Collections.sort(topRatedList, (d1, d2) -> {
                    int reviewCompare = Integer.compare(d2.getReviewCount(), d1.getReviewCount());
                    if (reviewCompare != 0) {
                        return reviewCompare;
                    }
                    return Double.compare(d2.getRating(), d1.getRating());
                });
            }

            if (availableAdapter != null) {
                availableAdapter.submitList(availableTodayList);
            }
            if (doctorAdapter != null) {
                doctorAdapter.submitList(topRatedList);
            }

            if (binding.shimmerAvailableToday != null) {
                binding.shimmerAvailableToday.stopShimmer();
                binding.shimmerAvailableToday.setVisibility(View.GONE);
            }
            if (binding.shimmerTopDoctors != null) {
                binding.shimmerTopDoctors.stopShimmer();
                binding.shimmerTopDoctors.setVisibility(View.GONE);
            }
            if (binding.rvAvailableToday != null) {
                binding.rvAvailableToday.setVisibility(View.VISIBLE);
            }
            if (binding.rvTopDoctors != null) {
                binding.rvTopDoctors.setVisibility(View.VISIBLE);
            }
        });
    }

    private void updateNotificationBadge() {
        if (binding == null || !isAdded()) return;
        Context context = getContext();
        if (context == null) return;

        String userId = PreferenceManager.getInstance(context).getUserId();
        if (userId == null || userId.trim().isEmpty()) return;

        SupabaseClient.getNotificationService().getNotificationsForPatient("eq." + userId)
                .enqueue(new Callback<List<NotificationItem>>() {
                    @Override
                    public void onResponse(Call<List<NotificationItem>> call, Response<List<NotificationItem>> response) {
                        if (binding == null || !isAdded()) return;
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
        if (binding == null || !isAdded()) return;
        Context context = getContext();
        if (context == null) return;

        PreferenceManager prefManager = PreferenceManager.getInstance(context);
        String userName = prefManager.getUserName();
        if (userName != null && !userName.isEmpty()) {
            binding.tvUsername.setText(userName);
        }

        String userAvatar = prefManager.getUserAvatar();
        if (userAvatar != null && !userAvatar.trim().isEmpty()
                && !userAvatar.toLowerCase().contains("banner")
                && !userAvatar.toLowerCase().contains("offer")) {
            Glide.with(context)
                    .load(userAvatar)
                    .circleCrop()
                    .placeholder(R.drawable.ic_user)
                    .error(R.drawable.ic_user)
                    .into(binding.ivAvatar);
        } else {
            binding.ivAvatar.setImageResource(R.drawable.ic_user);
        }

        String userId = prefManager.getUserId();
        if (userId != null && !userId.trim().isEmpty()) {
            SupabaseClient.getPatientService().getProfile("eq." + userId)
                    .enqueue(new Callback<List<PatientProfile>>() {
                        @Override
                        public void onResponse(Call<List<PatientProfile>> call, Response<List<PatientProfile>> response) {
                            if (binding == null || !isAdded()) return;
                            Context ctx = getContext();
                            if (ctx == null) return;
                            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                                PatientProfile p = response.body().get(0);
                                if (p.getAvatarUrl() != null && !p.getAvatarUrl().trim().isEmpty()
                                        && !p.getAvatarUrl().toLowerCase().contains("banner")
                                        && !p.getAvatarUrl().toLowerCase().contains("offer")) {
                                    prefManager.setUserAvatar(p.getAvatarUrl());
                                    Glide.with(ctx)
                                            .load(p.getAvatarUrl())
                                            .circleCrop()
                                            .placeholder(R.drawable.ic_user)
                                            .error(R.drawable.ic_user)
                                            .into(binding.ivAvatar);
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<List<PatientProfile>> call, Throwable t) {}
                    });
        }
    }

    private void showSpecialitiesBottomSheet() {
        Context context = getContext();
        if (!isAdded() || context == null) return;
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_specialities, null);
        dialog.setContentView(view);

        RecyclerView rv = view.findViewById(R.id.rv_specialities_grid);
        ImageView ivClose = view.findViewById(R.id.iv_close);

        if (ivClose != null) {
            ivClose.setOnClickListener(v -> dialog.dismiss());
        }

        if (rv != null) {
            rv.setLayoutManager(new GridLayoutManager(context, 3));
            SpecialitiesAdapter specialitiesAdapter = new SpecialitiesAdapter(speciality -> {
                dialog.dismiss();
                openDoctorList(speciality.getName());
            });
            rv.setAdapter(specialitiesAdapter);

            DummyDataProvider.fetchSpecialitiesFromSupabase(list -> {
                if (isAdded() && specialitiesAdapter != null) {
                    specialitiesAdapter.submitList(list);
                }
            });
        }

        dialog.show();
    }

    private void openFindDoctors() {
        Context context = getContext();
        if (context == null || !isAdded()) return;
        Intent intent = new Intent(context, FindDoctorsActivity.class);
        startActivity(intent);
    }

    private void openDoctorList(String categoryName) {
        Context context = getContext();
        if (context == null || !isAdded()) return;
        Intent intent = new Intent(context, DoctorListActivity.class);
        intent.putExtra("category_name", categoryName);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
