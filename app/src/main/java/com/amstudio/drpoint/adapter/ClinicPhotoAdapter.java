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

public class ClinicPhotoAdapter extends ListAdapter<Object, ClinicPhotoAdapter.ViewHolder> {

    private static final DiffUtil.ItemCallback<Object> DIFF_CALLBACK = new DiffUtil.ItemCallback<Object>() {
        @Override
        public boolean areItemsTheSame(@NonNull Object oldItem, @NonNull Object newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Object oldItem, @NonNull Object newItem) {
            return oldItem.toString().equals(newItem.toString());
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

        void bind(Object item) {
            Glide.with(itemView.getContext())
                    .load(item)
                    .placeholder(R.drawable.ic_stethoscope)
                    .error(R.drawable.ic_stethoscope)
                    .into(binding.ivClinicPhoto);
        }
    }
}
