package com.amstudio.drpoint.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.drpoint.R;
import com.amstudio.drpoint.databinding.ItemProfileMenuBinding;
import com.amstudio.drpoint.model.MenuItem;

public class ProfileMenuAdapter extends ListAdapter<MenuItem, ProfileMenuAdapter.ViewHolder> {

    public interface OnMenuItemClickListener {
        void onMenuItemClick(MenuItem item);
    }

    private static final DiffUtil.ItemCallback<MenuItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<MenuItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull MenuItem oldItem, @NonNull MenuItem newItem) {
            return oldItem.getTitle().equals(newItem.getTitle());
        }

        @Override
        public boolean areContentsTheSame(@NonNull MenuItem oldItem, @NonNull MenuItem newItem) {
            return oldItem.getIconRes() == newItem.getIconRes() && oldItem.getTextColorRes() == newItem.getTextColorRes();
        }
    };

    private final OnMenuItemClickListener listener;

    public ProfileMenuAdapter(OnMenuItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProfileMenuBinding binding = ItemProfileMenuBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemProfileMenuBinding binding;

        ViewHolder(@NonNull ItemProfileMenuBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(MenuItem item, OnMenuItemClickListener listener) {
            Context context = itemView.getContext();
            binding.tvMenuTitle.setText(item.getTitle());
            binding.ivMenuIcon.setImageResource(item.getIconRes());

            if (item.getTextColorRes() != 0) {
                int textColor = ContextCompat.getColor(context, item.getTextColorRes());
                binding.tvMenuTitle.setTextColor(textColor);
                binding.ivMenuIcon.setImageTintList(ContextCompat.getColorStateList(context, item.getTextColorRes()));
            } else {
                binding.tvMenuTitle.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
                binding.ivMenuIcon.setImageTintList(ContextCompat.getColorStateList(context, R.color.primary));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onMenuItemClick(item);
            });
        }
    }
}
