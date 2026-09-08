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
import com.amstudio.drpoint.databinding.ItemDateChipBinding;

import java.util.Objects;

public class DateChipAdapter extends ListAdapter<DateChipAdapter.DateItem, DateChipAdapter.ViewHolder> {

    public interface OnDateSelectedListener {
        void onDateSelected(DateItem dateItem, int position);
    }

    public static class DateItem {
        private final String day;
        private final String date;

        public DateItem(String day, String date) {
            this.day = day;
            this.date = date;
        }

        public String getDay() { return day; }
        public String getDate() { return date; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DateItem dateItem = (DateItem) o;
            return Objects.equals(day, dateItem.day) && Objects.equals(date, dateItem.date);
        }

        @Override
        public int hashCode() {
            return Objects.hash(day, date);
        }
    }

    private static final DiffUtil.ItemCallback<DateItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<DateItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull DateItem oldItem, @NonNull DateItem newItem) {
            return oldItem.getDay().equals(newItem.getDay()) && oldItem.getDate().equals(newItem.getDate());
        }

        @Override
        public boolean areContentsTheSame(@NonNull DateItem oldItem, @NonNull DateItem newItem) {
            return oldItem.equals(newItem);
        }
    };

    private int selectedPosition = 0;
    private final OnDateSelectedListener listener;

    public DateChipAdapter(OnDateSelectedListener listener) {
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

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDateChipBinding binding = ItemDateChipBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        boolean isSelected = (position == selectedPosition);
        holder.bind(getItem(position), isSelected, (item, adapterPos) -> {
            setSelectedPosition(adapterPos);
            if (listener != null) {
                listener.onDateSelected(item, adapterPos);
            }
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemDateChipBinding binding;

        ViewHolder(@NonNull ItemDateChipBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(DateItem dateItem, boolean isSelected, OnInternalClickListener internalClickListener) {
            Context context = itemView.getContext();
            binding.tvDayName.setText(dateItem.getDay());
            binding.tvDateVal.setText(dateItem.getDate());

            if (isSelected) {
                binding.llDateContainer.setBackgroundResource(R.drawable.bg_date_chip_selected);
                binding.tvDayName.setTextColor(ContextCompat.getColor(context, R.color.primary_light));
                binding.tvDateVal.setTextColor(ContextCompat.getColor(context, R.color.white));
            } else {
                binding.llDateContainer.setBackgroundResource(R.drawable.bg_date_chip_unselected);
                binding.tvDayName.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
                binding.tvDateVal.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
            }

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && internalClickListener != null) {
                    internalClickListener.onDateClick(dateItem, pos);
                }
            });
        }

        interface OnInternalClickListener {
            void onDateClick(DateItem dateItem, int position);
        }
    }
}
