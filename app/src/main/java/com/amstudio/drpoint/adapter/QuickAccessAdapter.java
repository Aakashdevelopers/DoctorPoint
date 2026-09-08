package com.amstudio.drpoint.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.drpoint.databinding.ItemQuickAccessBinding;
import com.amstudio.drpoint.model.QuickAccessItem;

public class QuickAccessAdapter extends ListAdapter<QuickAccessItem, QuickAccessAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(QuickAccessItem item);
    }

    private static final DiffUtil.ItemCallback<QuickAccessItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<QuickAccessItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull QuickAccessItem oldItem, @NonNull QuickAccessItem newItem) {
            return oldItem.getTitle().equals(newItem.getTitle());
        }

        @Override
        public boolean areContentsTheSame(@NonNull QuickAccessItem oldItem, @NonNull QuickAccessItem newItem) {
            return oldItem.getIconRes() == newItem.getIconRes() && oldItem.getBgColorRes() == newItem.getBgColorRes();
        }
    };

    private final OnItemClickListener listener;

    public QuickAccessAdapter(OnItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemQuickAccessBinding binding = ItemQuickAccessBinding.inflate(android.view.LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemQuickAccessBinding binding;

        ViewHolder(@NonNull ItemQuickAccessBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(QuickAccessItem item, OnItemClickListener listener) {
            Context context = itemView.getContext();
            binding.tvTitle.setText(item.getTitle());
            if (item.getSubtitle() != null && !item.getSubtitle().isEmpty()) {
                binding.tvSubtitle.setText(item.getSubtitle());
                binding.tvSubtitle.setVisibility(View.VISIBLE);
            } else {
                binding.tvSubtitle.setVisibility(View.GONE);
            }
            binding.ivIcon.setImageResource(item.getIconRes());
            binding.flIconBg.setBackgroundTintList(ContextCompat.getColorStateList(context, item.getBgColorRes()));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });
        }
    }
}
