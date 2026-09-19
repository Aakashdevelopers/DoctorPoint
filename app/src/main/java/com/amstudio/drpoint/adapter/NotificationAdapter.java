package com.amstudio.drpoint.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.drpoint.R;
import com.amstudio.drpoint.databinding.ItemNotificationBinding;
import com.amstudio.drpoint.model.NotificationItem;

import java.util.Objects;

public class NotificationAdapter extends ListAdapter<NotificationItem, NotificationAdapter.ViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationItem notification);
    }

    private static final DiffUtil.ItemCallback<NotificationItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<NotificationItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull NotificationItem oldItem, @NonNull NotificationItem newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull NotificationItem oldItem, @NonNull NotificationItem newItem) {
            return oldItem.isRead() == newItem.isRead() &&
                   Objects.equals(oldItem.getTitle(), newItem.getTitle()) &&
                   Objects.equals(oldItem.getMessage(), newItem.getMessage());
        }
    };

    private final OnNotificationClickListener listener;

    public NotificationAdapter(OnNotificationClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNotificationBinding binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemNotificationBinding binding;

        ViewHolder(@NonNull ItemNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(NotificationItem notification, OnNotificationClickListener listener) {
            Context context = itemView.getContext();

            binding.tvNotifTitle.setText(notification.getTitle());
            binding.tvNotifMessage.setText(notification.getMessage());
            binding.tvNotifTime.setText(notification.getCreatedAt() != null ? notification.getCreatedAt() : "Recent");

            if (notification.isRead()) {
                binding.vUnreadDot.setVisibility(View.GONE);
                binding.cardNotification.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface_white));
            } else {
                binding.vUnreadDot.setVisibility(View.VISIBLE);
                binding.cardNotification.setCardBackgroundColor(ContextCompat.getColor(context, R.color.bg_blue_light));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onNotificationClick(notification);
            });
        }
    }
}
