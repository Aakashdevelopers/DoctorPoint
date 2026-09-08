package com.amstudio.drpoint.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.drpoint.databinding.ItemSpecialityBinding;
import com.amstudio.drpoint.model.Speciality;

import java.util.Objects;

public class SpecialitiesAdapter extends ListAdapter<Speciality, SpecialitiesAdapter.ViewHolder> {

    public interface OnSpecialityClickListener {
        void onSpecialityClick(Speciality speciality);
    }

    private static final DiffUtil.ItemCallback<Speciality> DIFF_CALLBACK = new DiffUtil.ItemCallback<Speciality>() {
        @Override
        public boolean areItemsTheSame(@NonNull Speciality oldItem, @NonNull Speciality newItem) {
            return oldItem.getName().equals(newItem.getName());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Speciality oldItem, @NonNull Speciality newItem) {
            return oldItem.getIconRes() == newItem.getIconRes() && Objects.equals(oldItem.getIconUrl(), newItem.getIconUrl());
        }
    };

    private final OnSpecialityClickListener listener;

    public SpecialitiesAdapter(OnSpecialityClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSpecialityBinding binding = ItemSpecialityBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemSpecialityBinding binding;

        ViewHolder(@NonNull ItemSpecialityBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Speciality speciality, OnSpecialityClickListener listener) {
            Context context = itemView.getContext();
            binding.tvSpecialityName.setText(speciality.getName());
            if (speciality.getIconUrl() != null && !speciality.getIconUrl().isEmpty()) {
                com.bumptech.glide.Glide.with(context)
                        .load(speciality.getIconUrl())
                        .into(binding.ivSpecialityIcon);
            } else {
                binding.ivSpecialityIcon.setImageResource(speciality.getIconRes());
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSpecialityClick(speciality);
                }
            });
        }
    }
}
