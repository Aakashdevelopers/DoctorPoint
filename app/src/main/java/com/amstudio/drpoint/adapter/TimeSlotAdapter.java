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
import com.amstudio.drpoint.databinding.ItemTimeSlotBinding;

public class TimeSlotAdapter extends ListAdapter<String, TimeSlotAdapter.ViewHolder> {

    public interface OnTimeSlotSelectedListener {
        void onTimeSlotSelected(String slot, int position);
    }

    private static final DiffUtil.ItemCallback<String> DIFF_CALLBACK = new DiffUtil.ItemCallback<String>() {
        @Override
        public boolean areItemsTheSame(@NonNull String oldItem, @NonNull String newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areContentsTheSame(@NonNull String oldItem, @NonNull String newItem) {
            return oldItem.equals(newItem);
        }
    };

    private int selectedPosition = 0;
    private final OnTimeSlotSelectedListener listener;

    public TimeSlotAdapter(OnTimeSlotSelectedListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void setSelectedPosition(int position) {
        int oldPos = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(oldPos);
        notifyItemChanged(selectedPosition);
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public String getSelectedSlot() {
        if (selectedPosition >= 0 && selectedPosition < getItemCount()) {
            return getItem(selectedPosition);
        }
        return null;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTimeSlotBinding binding = ItemTimeSlotBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        boolean isSelected = (position == selectedPosition);
        holder.bind(getItem(position), isSelected, (slot, adapterPos) -> {
            setSelectedPosition(adapterPos);
            if (listener != null) {
                listener.onTimeSlotSelected(slot, adapterPos);
            }
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemTimeSlotBinding binding;

        ViewHolder(@NonNull ItemTimeSlotBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(String slot, boolean isSelected, OnInternalSlotClickListener listener) {
            Context context = itemView.getContext();
            binding.getRoot().setText(slot);

            if (isSelected) {
                binding.getRoot().setBackgroundResource(R.drawable.bg_date_chip_selected);
                binding.getRoot().setTextColor(ContextCompat.getColor(context, R.color.white));
            } else {
                binding.getRoot().setBackgroundResource(R.drawable.bg_date_chip_unselected);
                binding.getRoot().setTextColor(ContextCompat.getColor(context, R.color.text_primary));
            }

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onSlotClick(slot, pos);
                }
            });
        }

        interface OnInternalSlotClickListener {
            void onSlotClick(String slot, int position);
        }
    }
}
