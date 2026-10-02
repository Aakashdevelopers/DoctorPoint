package com.amstudio.drpoint.adapter;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
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
                    .placeholder(R.drawable.bg_clinic_placeholder)
                    .error(R.drawable.bg_clinic_placeholder)
                    .into(binding.ivClinicPhoto);

            itemView.setOnClickListener(v -> showFullImageDialog(item));
        }

        private void showFullImageDialog(Object photoItem) {
            try {
                Dialog dialog = new Dialog(itemView.getContext());
                dialog.setContentView(R.layout.dialog_image_preview);
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                }

                AppCompatImageView ivFull = dialog.findViewById(R.id.iv_preview_full);
                if (ivFull != null) {
                    Glide.with(itemView.getContext())
                            .load(photoItem)
                            .placeholder(R.drawable.bg_clinic_placeholder)
                            .error(R.drawable.bg_clinic_placeholder)
                            .into(ivFull);
                }

                View btnClose = dialog.findViewById(R.id.btn_close_preview);
                if (btnClose != null) {
                    btnClose.setOnClickListener(v -> dialog.dismiss());
                }

                dialog.show();
            } catch (Exception ignored) {}
        }
    }
}
