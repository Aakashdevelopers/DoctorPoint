package com.amstudio.drpoint.ui.explore;

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

import com.amstudio.drpoint.R;
import com.amstudio.drpoint.adapter.DoctorListAdapter;
import com.amstudio.drpoint.adapter.SpecialitiesAdapter;
import com.amstudio.drpoint.databinding.FragmentExploreBinding;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.ui.doctor.DoctorDetailActivity;
import com.amstudio.drpoint.ui.doctor.DoctorListActivity;
import com.amstudio.drpoint.util.DummyDataProvider;
import com.amstudio.drpoint.util.PreferenceManager;
import com.amstudio.drpoint.util.ToastUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExploreFragment extends Fragment {

    private FragmentExploreBinding binding;
    private DoctorListAdapter doctorListAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentExploreBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (binding == null) return;

        if (binding.tvLocationChip != null) {
            binding.tvLocationChip.setOnClickListener(v -> {
                Context context = getContext();
                if (context != null) {
                    DummyDataProvider.showStatePickerDialog(context, selectedState -> updateLocationChipAndDoctors());
                }
            });
        }
        updateLocationChipText();

        Context context = getContext();
        if (context != null && binding != null) {
            // Specialities Horizontal Grid (2 Rows)
            if (binding.rvSpecialities != null) {
                binding.rvSpecialities.setLayoutManager(new GridLayoutManager(context, 2, GridLayoutManager.HORIZONTAL, false));
                SpecialitiesAdapter specialitiesAdapter = new SpecialitiesAdapter(speciality -> {
                    Context ctx = getContext();
                    if (ctx != null && speciality != null) {
                        Intent intent = new Intent(ctx, DoctorListActivity.class);
                        intent.putExtra("category_name", speciality.getName());
                        startActivity(intent);
                    }
                });
                binding.rvSpecialities.setAdapter(specialitiesAdapter);
                DummyDataProvider.fetchSpecialitiesFromSupabase(list -> {
                    if (binding == null || !isAdded() || getContext() == null) return;
                    specialitiesAdapter.submitList(list);
                    if (binding.shimmerSpecialities != null) {
                        binding.shimmerSpecialities.stopShimmer();
                        binding.shimmerSpecialities.setVisibility(View.GONE);
                    }
                    if (binding.rvSpecialities != null) {
                        binding.rvSpecialities.setVisibility(View.VISIBLE);
                    }
                });
            }

            // Top Doctors Horizontal List (1 Row)
            if (binding.rvTopDoctors != null) {
                binding.rvTopDoctors.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
                doctorListAdapter = new DoctorListAdapter(true, new DoctorListAdapter.OnDoctorClickListener() {
                    @Override
                    public void onDoctorClick(Doctor doctor) {
                        Context ctx = getContext();
                        if (ctx != null && doctor != null) {
                            Intent intent = new Intent(ctx, DoctorDetailActivity.class);
                            intent.putExtra("doctor", doctor);
                            startActivity(intent);
                        }
                    }

                    @Override
                    public void onBookClick(Doctor doctor) {
                        Context ctx = getContext();
                        if (ctx != null && doctor != null) {
                            Intent intent = new Intent(ctx, DoctorDetailActivity.class);
                            intent.putExtra("doctor", doctor);
                            startActivity(intent);
                        }
                    }

                    @Override
                    public void onCallClick(Doctor doctor) {
                        Context ctx = getContext();
                        if (ctx == null || doctor == null) return;
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
                binding.rvTopDoctors.setAdapter(doctorListAdapter);
            }
        }

        loadTopDoctors();

        if (binding.tvSeeAllSpecialities != null) {
            binding.tvSeeAllSpecialities.setOnClickListener(v -> showSpecialitiesBottomSheet());
        }

        if (binding.tvSeeAllDoctors != null) {
            binding.tvSeeAllDoctors.setOnClickListener(v -> {
                Context ctx = getContext();
                if (ctx != null) {
                    Intent intent = new Intent(ctx, DoctorListActivity.class);
                    intent.putExtra("category_name", "Top Doctors Near You");
                    startActivity(intent);
                }
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
                Context ctx = getContext();
                if (ctx != null && speciality != null) {
                    Intent intent = new Intent(ctx, DoctorListActivity.class);
                    intent.putExtra("category_name", speciality.getName());
                    startActivity(intent);
                }
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

    @Override
    public void onResume() {
        super.onResume();
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
        loadTopDoctors();
    }

    private void loadTopDoctors() {
        if (binding == null || !isAdded()) return;
        Context context = getContext();
        if (context == null) return;
        String currentState = PreferenceManager.getInstance(context).getSelectedState();
        DummyDataProvider.fetchDoctorsFromSupabase(list -> {
            if (binding == null || !isAdded() || getContext() == null) return;
            List<Doctor> filtered = DummyDataProvider.filterDoctorsByState(list, currentState);

            List<Doctor> topRatedList = new ArrayList<>();
            for (Doctor d : filtered) {
                if (d != null && d.getRating() > 2.0) {
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
                topRatedList.addAll(filtered);
                Collections.sort(topRatedList, (d1, d2) -> {
                    int reviewCompare = Integer.compare(d2.getReviewCount(), d1.getReviewCount());
                    if (reviewCompare != 0) {
                        return reviewCompare;
                    }
                    return Double.compare(d2.getRating(), d1.getRating());
                });
            }

            if (doctorListAdapter != null) {
                doctorListAdapter.submitList(topRatedList);
            }
            if (binding.shimmerTopDoctors != null) {
                binding.shimmerTopDoctors.stopShimmer();
                binding.shimmerTopDoctors.setVisibility(View.GONE);
            }
            if (binding.rvTopDoctors != null) {
                binding.rvTopDoctors.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        if (binding != null) {
            if (binding.shimmerSpecialities != null) {
                try {
                    binding.shimmerSpecialities.stopShimmer();
                } catch (Exception ignored) {}
            }
            if (binding.shimmerTopDoctors != null) {
                try {
                    binding.shimmerTopDoctors.stopShimmer();
                } catch (Exception ignored) {}
            }
        }
        super.onDestroyView();
        binding = null;
    }
}
