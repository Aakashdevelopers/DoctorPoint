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
            return new HorizontalViewHolder(binding, parent);
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

        HorizontalViewHolder(@NonNull ItemDoctorPreviewBinding binding, ViewGroup parent) {
            super(binding.getRoot());
            this.binding = binding;

            if (parent instanceof RecyclerView) {
                RecyclerView rv = (RecyclerView) parent;
                RecyclerView.LayoutManager lm = rv.getLayoutManager();
                if (lm instanceof LinearLayoutManager) {
                    if (((LinearLayoutManager) lm).getOrientation() == LinearLayoutManager.HORIZONTAL) {
                        ViewGroup.LayoutParams lp = binding.getRoot().getLayoutParams();
                        if (lp != null) {
                            lp.width = dpToPx(parent.getContext(), 160);
                            binding.getRoot().setLayoutParams(lp);
                        }
                    }
                }
            }
        }

        void bind(Doctor doctor, OnDoctorClickListener listener) {
            Context context = itemView.getContext();

            ViewGroup.LayoutParams lp = itemView.getLayoutParams();
            if (lp != null) {
                if (isParentHorizontalLinear(itemView)) {
                    lp.width = dpToPx(context, 160);
                    itemView.setLayoutParams(lp);
                }
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

            if (isAvailable) {
                binding.ivDoctorPreview.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.success_green)));
                binding.ivDoctorPreview.setStrokeWidth(dpToPx(context, 3));
            } else {
                binding.ivDoctorPreview.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.card_stroke)));
                binding.ivDoctorPreview.setStrokeWidth(dpToPx(context, 1));
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

        private static boolean isParentHorizontalLinear(View view) {
            if (view.getParent() instanceof RecyclerView) {
                RecyclerView rv = (RecyclerView) view.getParent();
                RecyclerView.LayoutManager lm = rv.getLayoutManager();
                if (lm instanceof LinearLayoutManager) {
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

            boolean isAvailable = doctor.isAvailableToday();
            if (binding.viewOnlineStatus != null) {
                binding.viewOnlineStatus.setVisibility(isAvailable ? View.VISIBLE : View.GONE);
            }
            if (binding.tvAvailableToday != null) {
                binding.tvAvailableToday.setVisibility(isAvailable ? View.VISIBLE : View.GONE);
            }

            if (isAvailable) {
                binding.ivDoctor.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.success_green)));
                binding.ivDoctor.setStrokeWidth(dpToPx(context, 3));
            } else {
                binding.ivDoctor.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.card_stroke)));
                binding.ivDoctor.setStrokeWidth(dpToPx(context, 1));
            }

            double rating = doctor.getRating();
            int reviews = doctor.getReviewCount();
            if (reviews > 0 && rating > 0) {
                binding.tvRating.setText(String.format(Locale.getDefault(), "%.1f (%d reviews)", rating, reviews));
            } else {
                binding.tvRating.setText("0 (0 reviews)");
            }

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
            updateFavoriteState(binding, context, isFav);

            binding.ivFavorite.setOnClickListener(v -> {
                boolean newFav = PreferenceManager.getInstance(context).toggleFavoriteDoctor(doctor.getId());
                updateFavoriteState(binding, context, newFav);
                if (listener != null) listener.onFavoriteClick(doctor);
            });

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onDoctorClick(doctor);
            });
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onDoctorClick(doctor);
            });
            binding.btnBookNow.setOnClickListener(v -> {
                if (listener != null) listener.onBookClick(doctor);
            });
        }

        private static void updateFavoriteState(ItemDoctorCardBinding binding, Context context, boolean isFav) {
            if (isFav) {
                binding.ivFavorite.setImageResource(R.drawable.ic_heart_filled);
                ImageViewCompat.setImageTintList(binding.ivFavorite, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.error_red)));
            } else {
                binding.ivFavorite.setImageResource(R.drawable.ic_heart);
                ImageViewCompat.setImageTintList(binding.ivFavorite, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.text_secondary)));
            }
        }

        private static int dpToPx(Context context, float dp) {
            return Math.round(dp * context.getResources().getDisplayMetrics().density);
        }
    }
}
