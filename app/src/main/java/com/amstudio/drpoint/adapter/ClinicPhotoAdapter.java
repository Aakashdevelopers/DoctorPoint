package com.amstudio.drpoint.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.drpoint.R;
import com.amstudio.drpoint.databinding.ItemClinicPhotoBinding;
import com.bumptech.glide.Glide;

public class ClinicPhotoAdapter extends ListAdapter<Integer, ClinicPhotoAdapter.ViewHolder> {

    private static final DiffUtil.ItemCallback<Integer> DIFF_CALLBACK = new DiffUtil.ItemCallback<Integer>() {
        @Override
        public boolean areItemsTheSame(@NonNull Integer oldItem, @NonNull Integer newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Integer oldItem, @NonNull Integer newItem) {
            return oldItem.equals(newItem);
        }
    };

    public ClinicPhotoAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemClinicPhotoBinding binding = ItemClinicPhotoBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemClinicPhotoBinding binding;

        ViewHolder(@NonNull ItemClinicPhotoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Integer resId) {
            Glide.with(itemView.getContext())
                    .load(resId)
                    .placeholder(R.drawable.ic_stethoscope)
                    .error(R.drawable.ic_stethoscope)
                    .into(binding.ivClinicPhoto);
        }
    }
}
