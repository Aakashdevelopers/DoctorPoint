package com.amstudio.drpoint.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
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
            return oldItem.equals(newItem);
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
        boolean isLastItem = position == getItemCount() - 1;
        holder.bind(getItem(position), listener, isLastItem);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemProfileMenuBinding binding;

        ViewHolder(@NonNull ItemProfileMenuBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(MenuItem item, OnMenuItemClickListener listener, boolean isLastItem) {
            Context context = itemView.getContext();
            binding.tvMenuTitle.setText(item.getTitle());
            binding.ivMenuIcon.setImageResource(item.getIconRes());

            if (item.getSubtitle() != null && !item.getSubtitle().trim().isEmpty()) {
                binding.tvMenuSubtitle.setText(item.getSubtitle());
                binding.tvMenuSubtitle.setVisibility(View.VISIBLE);
            } else {
                binding.tvMenuSubtitle.setVisibility(View.GONE);
            }

            if (item.getIconTintRes() != 0) {
                int iconTint = ContextCompat.getColor(context, item.getIconTintRes());
                binding.ivMenuIcon.setImageTintList(ColorStateList.valueOf(iconTint));
            } else if (item.getTextColorRes() != 0) {
                int textColor = ContextCompat.getColor(context, item.getTextColorRes());
                binding.ivMenuIcon.setImageTintList(ColorStateList.valueOf(textColor));
            } else {
                binding.ivMenuIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.text_primary)));
            }

            if (item.getTextColorRes() != 0) {
                int textColor = ContextCompat.getColor(context, item.getTextColorRes());
                binding.tvMenuTitle.setTextColor(textColor);
            } else {
                binding.tvMenuTitle.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
            }

            if (item.getBadgeText() != null && !item.getBadgeText().trim().isEmpty()) {
                binding.tvMenuBadge.setText(item.getBadgeText());
                binding.tvMenuBadge.setVisibility(View.VISIBLE);
            } else {
                binding.tvMenuBadge.setVisibility(View.GONE);
            }

            binding.vDivider.setVisibility(isLastItem ? View.GONE : View.VISIBLE);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onMenuItemClick(item);
            });
        }
    }
}
