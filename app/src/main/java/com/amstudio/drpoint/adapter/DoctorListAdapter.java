package com.amstudio.drpoint.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.drpoint.R;
import com.amstudio.drpoint.databinding.ItemDoctorCardBinding;
import com.amstudio.drpoint.databinding.ItemDoctorPreviewBinding;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.util.PreferenceManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;

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
                   oldItem.getQualification().equals(newItem.getQualification()) &&
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
            binding.tvDoctorNamePreview.setText(doctor.getName());
            binding.tvSpecializationPreview.setText(doctor.getQualification());
            binding.tvRatingPreview.setText(String.valueOf(doctor.getRating()));

            Object imageSource = (doctor.getImageUrl() != null && !doctor.getImageUrl().isEmpty())
                    ? doctor.getImageUrl()
                    : (doctor.getImageRes() != 0 ? doctor.getImageRes() : R.drawable.ic_user);

            Glide.with(itemView.getContext())
                    .load(imageSource)
                    .transform(new CircleCrop())
                    .placeholder(R.drawable.ic_user)
                    .error(R.drawable.ic_user)
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
    }

    static class VerticalViewHolder extends RecyclerView.ViewHolder {
        private final ItemDoctorCardBinding binding;

        VerticalViewHolder(@NonNull ItemDoctorCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Doctor doctor, OnDoctorClickListener listener) {
            Context context = itemView.getContext();
            binding.tvDoctorName.setText(doctor.getName());
            binding.tvQualification.setText(doctor.getQualification() + " • " + doctor.getExperience());
            binding.tvRating.setText(doctor.getRating() + " (" + doctor.getReviewCount() + " reviews)");
            binding.tvClinicLocation.setText(doctor.getClinicName() + " • " + doctor.getLocation());
            binding.tvFee.setText("₹" + doctor.getFee() + " Consultation Fee");

            if (doctor.isVerified()) {
                binding.llVerified.setVisibility(View.VISIBLE);
            } else {
                binding.llVerified.setVisibility(View.GONE);
            }

            Object imageSource = (doctor.getImageUrl() != null && !doctor.getImageUrl().isEmpty())
                    ? doctor.getImageUrl()
                    : (doctor.getImageRes() != 0 ? doctor.getImageRes() : R.drawable.ic_user);

            Glide.with(context)
                    .load(imageSource)
                    .transform(new CircleCrop())
                    .placeholder(R.drawable.ic_user)
                    .error(R.drawable.ic_user)
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
            if (binding.btnCall != null) {
                binding.btnCall.setOnClickListener(v -> {
                    if (listener != null) listener.onCallClick(doctor);
                });
            }
        }
    }
}
