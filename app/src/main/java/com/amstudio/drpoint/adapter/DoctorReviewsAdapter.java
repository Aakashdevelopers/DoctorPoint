package com.amstudio.drpoint.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.drpoint.R;
import com.amstudio.drpoint.databinding.ItemDoctorReviewBinding;
import com.amstudio.drpoint.model.DoctorReview;
import com.bumptech.glide.Glide;

import java.util.Locale;

public class DoctorReviewsAdapter extends ListAdapter<DoctorReview, DoctorReviewsAdapter.ViewHolder> {

    private static final DiffUtil.ItemCallback<DoctorReview> DIFF_CALLBACK = new DiffUtil.ItemCallback<DoctorReview>() {
        @Override
        public boolean areItemsTheSame(@NonNull DoctorReview oldItem, @NonNull DoctorReview newItem) {
            return oldItem.getId() != null && oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull DoctorReview oldItem, @NonNull DoctorReview newItem) {
            return oldItem.getRating() == newItem.getRating() &&
                    (oldItem.getReviewText() == null || oldItem.getReviewText().equals(newItem.getReviewText()));
        }
    };

    public DoctorReviewsAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDoctorReviewBinding binding = ItemDoctorReviewBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemDoctorReviewBinding binding;

        ViewHolder(@NonNull ItemDoctorReviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(DoctorReview review) {
            String name = review.getPatientName() != null && !review.getPatientName().trim().isEmpty()
                    ? review.getPatientName().trim() : "Verified Patient";
            binding.tvPatientName.setText(name);

            String text = review.getReviewText() != null && !review.getReviewText().trim().isEmpty()
                    ? review.getReviewText().trim() : "Great consultation experience!";
            binding.tvReviewText.setText(text);

            binding.tvReviewRating.setText(String.format(Locale.getDefault(), "%.1f", review.getRating()));

            String dateStr = review.getCreatedAt();
            if (dateStr != null && dateStr.contains("T")) {
                dateStr = dateStr.split("T")[0];
            }
            binding.tvReviewDate.setText(dateStr != null && !dateStr.isEmpty() ? dateStr : "Verified Consultation");

            if (review.getPatientAvatar() != null && !review.getPatientAvatar().trim().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(review.getPatientAvatar().trim())
                        .placeholder(R.drawable.ic_user)
                        .error(R.drawable.ic_user)
                        .into(binding.ivPatientAvatar);
            } else {
                binding.ivPatientAvatar.setImageResource(R.drawable.ic_user);
            }
        }
    }
}
