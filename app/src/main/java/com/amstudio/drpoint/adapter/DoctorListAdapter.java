package com.amstudio.drpoint.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
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
                   oldItem.getRating() == newItem.getRating();
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
        }

        void bind(Doctor doctor, OnDoctorClickListener listener) {
            Context context = itemView.getContext();

            // Adjust width dynamically for horizontal scroll vs 2-column grid
            ViewGroup.LayoutParams lp = itemView.getLayoutParams();
            if (lp != null) {
                if (isParentHorizontalLinear(itemView)) {
                    lp.width = dpToPx(context, 160);
                } else {
                    lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                }
                itemView.setLayoutParams(lp);
            }

            binding.tvDoctorNamePreview.setText(doctor.getName() != null ? doctor.getName() : "Dr. Medical Specialist");
            
            String spec = doctor.getSpecializationString();
            binding.tvSpecializationPreview.setText((spec != null && !spec.isEmpty()) ? spec : "General Physician");

            double rating = doctor.getRating() > 0 ? doctor.getRating() : 4.8;
            binding.tvRatingPreview.setText(String.format(Locale.getDefault(), "%.1f", rating));

            if (binding.viewAvailableDot != null) {
                binding.viewAvailableDot.setVisibility(doctor.isAvailableToday() ? View.VISIBLE : View.GONE);
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

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onDoctorClick(doctor);
            });
            if (binding.btnBookPreview != null) {
                binding.btnBookPreview.setOnClickListener(v -> {
                    if (listener != null) listener.onBookClick(doctor);
                });
            }
        }

        private static boolean isParentHorizontalLinear(View view) {
            if (view.getParent() instanceof RecyclerView) {
                RecyclerView rv = (RecyclerView) view.getParent();
                RecyclerView.LayoutManager lm = rv.getLayoutManager();
                if (lm instanceof LinearLayoutManager && !(lm instanceof GridLayoutManager)) {
                    return ((LinearLayoutManager) lm).getOrientation() == LinearLayoutManager.HORIZONTAL;
                }
            }
            return false;
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

            binding.tvDoctorName.setText(doctor.getName() != null ? doctor.getName() : "Dr. Medical Specialist");

            String spec = doctor.getSpecializationString();
            String qual = doctor.getQualification();
            if (spec != null && !spec.isEmpty() && qual != null && !qual.isEmpty() && !qual.equalsIgnoreCase(spec)) {
                binding.tvQualification.setText(spec + " • " + qual);
            } else if (spec != null && !spec.isEmpty()) {
                binding.tvQualification.setText(spec);
            } else if (qual != null && !qual.isEmpty()) {
                binding.tvQualification.setText(qual);
            } else {
                binding.tvQualification.setText("General Physician");
            }

            String exp = doctor.getExperience();
            if (exp != null && !exp.isEmpty()) {
                binding.tvExperience.setText(exp.toLowerCase().contains("exp") ? exp : exp + " Exp");
                binding.tvExperience.setVisibility(View.VISIBLE);
            } else {
                binding.tvExperience.setVisibility(View.GONE);
            }

            double rating = doctor.getRating() > 0 ? doctor.getRating() : 4.8;
            int reviews = doctor.getReviewCount() > 0 ? doctor.getReviewCount() : 120;
            binding.tvRating.setText(String.format(Locale.getDefault(), "%.1f (%d reviews)", rating, reviews));

            String clinic = doctor.getClinicName() != null ? doctor.getClinicName() : "Care Clinic";
            String location = doctor.getLocation() != null ? doctor.getLocation() : "Main Branch";
            binding.tvClinicLocation.setText(clinic + " • " + location);

            int fee = doctor.getFee() > 0 ? doctor.getFee() : 500;
            binding.tvFee.setText("₹" + fee + " Fee");

            if (binding.llVerified != null) {
                binding.llVerified.setVisibility(doctor.isVerified() ? View.VISIBLE : View.GONE);
            }

            Object imageSource = (doctor.getImageUrl() != null && !doctor.getImageUrl().trim().isEmpty())
                    ? doctor.getImageUrl().trim()
                    : (doctor.getImageRes() != 0 ? doctor.getImageRes() : R.drawable.banner_1);

            Glide.with(context)
                    .load(imageSource)
                    .centerCrop()
                    .placeholder(R.drawable.banner_1)
                    .error(R.drawable.banner_1)
                    .into(binding.ivDoctor);

            // Favorite state
            boolean isFav = PreferenceManager.getInstance(context).isFavoriteDoctor(doctor.getId());
            binding.ivFavorite.setImageResource(isFav ? R.drawable.ic_heart_filled : R.drawable.ic_heart);

            binding.ivFavorite.setOnClickListener(v -> {
                boolean newFav = PreferenceManager.getInstance(context).toggleFavoriteDoctor(doctor.getId());
                binding.ivFavorite.setImageResource(newFav ? R.drawable.ic_heart_filled : R.drawable.ic_heart);
                if (listener != null) listener.onFavoriteClick(doctor);
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onDoctorClick(doctor);
            });
            binding.btnBookNow.setOnClickListener(v -> {
                if (listener != null) listener.onBookClick(doctor);
            });
        }
    }
}
