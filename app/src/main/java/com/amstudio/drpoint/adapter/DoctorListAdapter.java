package com.amstudio.drpoint.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.drpoint.R;
import com.amstudio.drpoint.databinding.ItemDoctorCardBinding;
import com.amstudio.drpoint.databinding.ItemDoctorPreviewBinding;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.util.PreferenceManager;
import com.bumptech.glide.Glide;

import java.util.Locale;

public class DoctorListAdapter extends ListAdapter<Doctor, RecyclerView.ViewHolder> {

    public interface OnDoctorClickListener {
        void onDoctorClick(Doctor doctor);
        void onBookClick(Doctor doctor);
        void onCallClick(Doctor doctor);
        void onFavoriteClick(Doctor doctor);
    }

    private static final int TYPE_VERTICAL = 1;
    private static final int TYPE_HORIZONTAL = 2;

    private static final DiffUtil.ItemCallback<Doctor> DIFF_CALLBACK = new DiffUtil.ItemCallback<Doctor>() {
        @Override
        public boolean areItemsTheSame(@NonNull Doctor oldItem, @NonNull Doctor newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Doctor oldItem, @NonNull Doctor newItem) {
            return oldItem.getName().equals(newItem.getName()) &&
                   oldItem.getFee() == newItem.getFee() &&
                   oldItem.getRating() == newItem.getRating() &&
                   oldItem.isAvailableToday() == newItem.isAvailableToday();
        }
    };

    private final boolean isHorizontalPreview;
    private final OnDoctorClickListener listener;

    public DoctorListAdapter(boolean isHorizontalPreview, OnDoctorClickListener listener) {
        super(DIFF_CALLBACK);
        this.isHorizontalPreview = isHorizontalPreview;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return isHorizontalPreview ? TYPE_HORIZONTAL : TYPE_VERTICAL;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HORIZONTAL) {
            ItemDoctorPreviewBinding binding = ItemDoctorPreviewBinding.inflate(inflater, parent, false);
            return new HorizontalViewHolder(binding);
        } else {
            ItemDoctorCardBinding binding = ItemDoctorCardBinding.inflate(inflater, parent, false);
            return new VerticalViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Doctor doctor = getItem(position);
        if (holder instanceof HorizontalViewHolder) {
            ((HorizontalViewHolder) holder).bind(doctor, listener);
        } else if (holder instanceof VerticalViewHolder) {
            ((VerticalViewHolder) holder).bind(doctor, listener);
        }
    }

    static class HorizontalViewHolder extends RecyclerView.ViewHolder {
        private final ItemDoctorPreviewBinding binding;

        HorizontalViewHolder(@NonNull ItemDoctorPreviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            ViewGroup.LayoutParams lp = binding.getRoot().getLayoutParams();
            if (lp != null) {
                lp.width = dpToPx(binding.getRoot().getContext(), 130);
                binding.getRoot().setLayoutParams(lp);
            }
        }

        void bind(Doctor doctor, OnDoctorClickListener listener) {
            Context context = itemView.getContext();

            ViewGroup.LayoutParams lp = itemView.getLayoutParams();
            if (lp != null) {
                lp.width = dpToPx(context, 130);
                itemView.setLayoutParams(lp);
            }

            binding.tvDoctorNamePreview.setText(doctor.getName() != null ? doctor.getName() : "Dr. Medical Specialist");
            
            String spec = doctor.getSpecializationString();
            binding.tvSpecializationPreview.setText((spec != null && !spec.isEmpty()) ? spec : "General Physician");

            double rating = doctor.getRating();
            int reviews = doctor.getReviewCount();
            if (reviews > 0 && rating > 0) {
                binding.tvRatingPreview.setText(String.format(Locale.getDefault(), "%.1f", rating));
            } else {
                binding.tvRatingPreview.setText("0");
            }

            boolean isAvailable = doctor.isAvailableToday();
            if (binding.viewAvailableDot != null) {
                binding.viewAvailableDot.setVisibility(isAvailable ? View.VISIBLE : View.GONE);
            }



            Object imageSource = (doctor.getImageUrl() != null && !doctor.getImageUrl().trim().isEmpty())
                    ? doctor.getImageUrl().trim()
                    : (doctor.getImageRes() != 0 ? doctor.getImageRes() : R.drawable.banner_1);

            Glide.with(itemView.getContext())
                    .load(imageSource)
                    .centerCrop()
                    .placeholder(R.drawable.banner_1)
                    .error(R.drawable.banner_1)
                    .into(binding.ivDoctorPreview);

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onDoctorClick(doctor);
            });
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onDoctorClick(doctor);
            });
            if (binding.btnBookPreview != null) {
                binding.btnBookPreview.setOnClickListener(v -> {
                    if (listener != null) listener.onBookClick(doctor);
                });
            }
        }

        private static int dpToPx(Context context, float dp) {
            return Math.round(dp * context.getResources().getDisplayMetrics().density);
        }
    }

    static class VerticalViewHolder extends RecyclerView.ViewHolder {
        private final ItemDoctorCardBinding binding;

        VerticalViewHolder(@NonNull ItemDoctorCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Doctor doctor, OnDoctorClickListener listener) {
            Context context = itemView.getContext();

            String name = doctor.getName();
            binding.tvDoctorName.setText(name != null && !name.trim().isEmpty() ? name.trim() : "Dr. Medical Specialist");

            String spec = doctor.getSpecializationString();
            if (spec == null || spec.trim().isEmpty()) {
                spec = doctor.getQualification() != null ? doctor.getQualification() : "";
            }

            String clinic = doctor.getClinicName();
            String location = doctor.getLocation();

            StringBuilder subtitle = new StringBuilder();
            if (spec != null && !spec.trim().isEmpty()) {
                subtitle.append(spec.trim());
            }

            if (clinic != null && !clinic.trim().isEmpty()) {
                if (subtitle.length() > 0) {
                    subtitle.append("  |  ");
                }
                subtitle.append(clinic.trim());
                if (location != null && !location.trim().isEmpty() && !location.equalsIgnoreCase(clinic)) {
                    subtitle.append(", ").append(location.trim());
                }
            } else if (location != null && !location.trim().isEmpty()) {
                if (subtitle.length() > 0) {
                    subtitle.append("  |  ");
                }
                subtitle.append(location.trim());
            }

            if (subtitle.length() == 0) {
                subtitle.append("General Physician");
            }

            binding.tvQualification.setText(subtitle.toString());

            double rating = doctor.getRating();
            int reviews = doctor.getReviewCount();

            if (binding.tvRating != null) {
                if (rating > 0) {
                    String rStr = (rating % 1 == 0) ? String.valueOf((int) rating) : String.format(Locale.getDefault(), "%.1f", rating);
                    binding.tvRating.setText(rStr);
                } else {
                    binding.tvRating.setText("0");
                }
            }

            if (binding.tvReviewCount != null) {
                binding.tvReviewCount.setText(String.format(Locale.getDefault(), "(%d reviews)", reviews));
            }

            Object imageSource = (doctor.getImageUrl() != null && !doctor.getImageUrl().trim().isEmpty())
                    ? doctor.getImageUrl().trim()
                    : (doctor.getImageRes() != 0 ? doctor.getImageRes() : R.drawable.ic_user);

            Glide.with(context)
                    .load(imageSource)
                    .centerCrop()
                    .placeholder(R.drawable.ic_user)
                    .error(R.drawable.ic_user)
                    .into(binding.ivDoctor);

            // Favorite heart icon handling
            boolean isFav = PreferenceManager.getInstance(context).isFavoriteDoctor(doctor.getId());


            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onDoctorClick(doctor);
            });
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onDoctorClick(doctor);
            });
        }



        private static int dpToPx(Context context, float dp) {
            return Math.round(dp * context.getResources().getDisplayMetrics().density);
        }
    }
}
