package com.amstudio.drpoint.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.drpoint.R;
import com.amstudio.drpoint.databinding.ItemBannerGradientBinding;
import com.amstudio.drpoint.databinding.ItemBannerImageBinding;

public class BannerSliderAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_GRADIENT = 0;
    private static final int TYPE_IMAGE = 1;

    private final OnBannerClickListener listener;

    public interface OnBannerClickListener {
        void onBannerClick(int position);
    }

    public BannerSliderAdapter(OnBannerClickListener listener) {
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_GRADIENT : TYPE_IMAGE;
    }

    @Override
    public int getItemCount() {
        return 2; // Gradient banner + banner_1.jpeg
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_GRADIENT) {
            ItemBannerGradientBinding binding = ItemBannerGradientBinding.inflate(inflater, parent, false);
            return new GradientViewHolder(binding);
        } else {
            ItemBannerImageBinding binding = ItemBannerImageBinding.inflate(inflater, parent, false);
            return new ImageViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof GradientViewHolder) {
            ((GradientViewHolder) holder).bind(listener);
        } else if (holder instanceof ImageViewHolder) {
            ((ImageViewHolder) holder).bind(listener);
        }
    }

    static class GradientViewHolder extends RecyclerView.ViewHolder {
        private final ItemBannerGradientBinding binding;

        GradientViewHolder(@NonNull ItemBannerGradientBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(OnBannerClickListener listener) {
            binding.btnBannerBook.setOnClickListener(v -> {
                if (listener != null) listener.onBannerClick(0);
            });
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onBannerClick(0);
            });
        }
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        private final ItemBannerImageBinding binding;

        ImageViewHolder(@NonNull ItemBannerImageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(OnBannerClickListener listener) {
            binding.ivBannerImage.setImageResource(R.drawable.banner_1);
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onBannerClick(1);
            });
        }
    }
}
