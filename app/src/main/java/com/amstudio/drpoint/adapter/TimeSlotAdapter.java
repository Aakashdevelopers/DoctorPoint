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
import com.amstudio.drpoint.model.DoctorSlot;
import com.amstudio.drpoint.util.ToastUtils;

import java.util.Objects;

public class TimeSlotAdapter extends ListAdapter<DoctorSlot, TimeSlotAdapter.ViewHolder> {

    public interface OnTimeSlotSelectedListener {
        void onTimeSlotSelected(DoctorSlot slot, int position);
    }

    private static final DiffUtil.ItemCallback<DoctorSlot> DIFF_CALLBACK = new DiffUtil.ItemCallback<DoctorSlot>() {
        @Override
        public boolean areItemsTheSame(@NonNull DoctorSlot oldItem, @NonNull DoctorSlot newItem) {
            if (oldItem.getId() != null && newItem.getId() != null) {
                return oldItem.getId().equals(newItem.getId());
            }
            return Objects.equals(oldItem.getStartTime(), newItem.getStartTime());
        }

        @Override
        public boolean areContentsTheSame(@NonNull DoctorSlot oldItem, @NonNull DoctorSlot newItem) {
            return Objects.equals(oldItem.getStatus(), newItem.getStatus())
                    && Objects.equals(oldItem.getStartTime(), newItem.getStartTime());
        }
    };

    private int selectedPosition = -1;
    private final OnTimeSlotSelectedListener listener;

    public TimeSlotAdapter(OnTimeSlotSelectedListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void setSelectedPosition(int position) {
        if (selectedPosition == position) return;
        int oldPos = selectedPosition;
        selectedPosition = position;
        if (oldPos >= 0 && oldPos < getItemCount()) {
            notifyItemChanged(oldPos);
        }
        if (selectedPosition >= 0 && selectedPosition < getItemCount()) {
            notifyItemChanged(selectedPosition);
        }
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public DoctorSlot getSelectedSlot() {
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

        void bind(DoctorSlot slot, boolean isSelected, OnInternalSlotClickListener listener) {
            Context context = itemView.getContext();
            String timeStr = slot != null ? slot.getFormattedTime() : "";
            binding.getRoot().setText(timeStr);

            boolean isAvailable = slot != null && "available".equalsIgnoreCase(slot.getStatus());

            if (!isAvailable) {
                // Booked / Blocked -> Grayed out background with red text
                binding.getRoot().setBackgroundResource(R.drawable.bg_calendar_day_unavailable);
                binding.getRoot().setTextColor(ContextCompat.getColor(context, R.color.error_red));
                binding.getRoot().setAlpha(0.5f);
            } else if (isSelected) {
                // Available & Selected -> Dark Green Fill
                binding.getRoot().setBackgroundResource(R.drawable.bg_calendar_day_selected);
                binding.getRoot().setTextColor(ContextCompat.getColor(context, R.color.white));
                binding.getRoot().setAlpha(1.0f);
            } else {
                // Available & Not Selected -> Light Green (Available) outline
                binding.getRoot().setBackgroundResource(R.drawable.bg_calendar_day_available);
                binding.getRoot().setTextColor(ContextCompat.getColor(context, R.color.primary));
                binding.getRoot().setAlpha(1.0f);
            }

            itemView.setOnClickListener(v -> {
                if (!isAvailable) {
                    ToastUtils.showWarning(context, "Slot is already booked or unavailable.");
                    return;
                }
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null && slot != null) {
                    listener.onSlotClick(slot, pos);
                }
            });
        }

        interface OnInternalSlotClickListener {
            void onSlotClick(DoctorSlot slot, int position);
        }
    }
}
