package com.amstudio.drpoint.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.drpoint.R;
import com.amstudio.drpoint.databinding.ItemCalendarDayBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarDayAdapter extends RecyclerView.Adapter<CalendarDayAdapter.ViewHolder> {

    public interface OnCalendarDayClickListener {
        void onDayClick(CalendarDay day);
    }

    public static class CalendarDay {
        private final int dayNumber;
        private final String fullDate;
        private final boolean isCurrentMonth;
        private final boolean isAvailable;
        private final boolean isPast;
        private boolean isSelected;

        public CalendarDay(int dayNumber, String fullDate, boolean isCurrentMonth, boolean isAvailable, boolean isPast) {
            this.dayNumber = dayNumber;
            this.fullDate = fullDate;
            this.isCurrentMonth = isCurrentMonth;
            this.isAvailable = isAvailable;
            this.isPast = isPast;
            this.isSelected = false;
        }

        public int getDayNumber() { return dayNumber; }
        public String getFullDate() { return fullDate; }
        public boolean isCurrentMonth() { return isCurrentMonth; }
        public boolean isAvailable() { return isAvailable; }
        public boolean isPast() { return isPast; }
        public boolean isSelected() { return isSelected; }
        public void setSelected(boolean selected) { isSelected = selected; }
    }

    private final List<CalendarDay> dayList = new ArrayList<>();
    private final OnCalendarDayClickListener listener;
    private int selectedPosition = -1;

    public CalendarDayAdapter(OnCalendarDayClickListener listener) {
        this.listener = listener;
    }

    public void setDays(List<CalendarDay> days) {
        this.dayList.clear();
        if (days != null) {
            this.dayList.addAll(days);
        }
        this.selectedPosition = -1;
        notifyDataSetChanged();
    }

    public void selectDate(String fullDate) {
        for (int i = 0; i < dayList.size(); i++) {
            CalendarDay day = dayList.get(i);
            if (day.getFullDate() != null && day.getFullDate().equals(fullDate)) {
                setSelectedPosition(i);
                break;
            }
        }
    }

    public void setSelectedPosition(int pos) {
        int prev = selectedPosition;
        selectedPosition = pos;

        for (int i = 0; i < dayList.size(); i++) {
            dayList.get(i).setSelected(i == selectedPosition);
        }

        if (prev != -1) notifyItemChanged(prev);
        if (selectedPosition != -1) notifyItemChanged(selectedPosition);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCalendarDayBinding binding = ItemCalendarDayBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(dayList.get(position), position == selectedPosition, listener, this);
    }

    @Override
    public int getItemCount() {
        return dayList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemCalendarDayBinding binding;

        ViewHolder(@NonNull ItemCalendarDayBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(CalendarDay day, boolean isSelected, OnCalendarDayClickListener listener, CalendarDayAdapter adapter) {
            Context context = itemView.getContext();

            if (day.getDayNumber() <= 0 || !day.isCurrentMonth()) {
                binding.llDayContainer.setVisibility(View.GONE);
                return;
            }

            binding.llDayContainer.setVisibility(View.VISIBLE);

            if (day.getFullDate() != null && !day.getFullDate().isEmpty()) {
                try {
                    SimpleDateFormat inputSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    SimpleDateFormat dateMonthSdf = new SimpleDateFormat("d MMM", Locale.getDefault());
                    SimpleDateFormat dayNameSdf = new SimpleDateFormat("EEE", Locale.getDefault());
                    String todayStr = inputSdf.format(new Date());

                    Date d = inputSdf.parse(day.getFullDate());
                    if (d != null) {
                        binding.tvDayNumber.setText(dateMonthSdf.format(d));
                        if (day.getFullDate().equals(todayStr)) {
                            binding.tvDayName.setText("Today");
                        } else {
                            binding.tvDayName.setText(dayNameSdf.format(d));
                        }
                    } else {
                        binding.tvDayNumber.setText(String.valueOf(day.getDayNumber()));
                        binding.tvDayName.setText("");
                    }
                } catch (Exception e) {
                    binding.tvDayNumber.setText(String.valueOf(day.getDayNumber()));
                    binding.tvDayName.setText("");
                }
            } else {
                binding.tvDayNumber.setText(String.valueOf(day.getDayNumber()));
                binding.tvDayName.setText("");
            }

            if (isSelected) {
                binding.llDayContainer.setBackgroundResource(R.drawable.bg_calendar_day_selected);
                binding.tvDayNumber.setTextColor(ContextCompat.getColor(context, R.color.white));
                binding.tvDayName.setTextColor(ContextCompat.getColor(context, R.color.white));
                binding.viewStatusDot.setVisibility(View.VISIBLE);
                binding.viewStatusDot.setBackgroundResource(R.drawable.bg_dot_green);
            } else if (day.isPast()) {
                binding.llDayContainer.setBackgroundResource(R.drawable.bg_calendar_day_unavailable);
                binding.tvDayNumber.setTextColor(ContextCompat.getColor(context, R.color.text_muted));
                binding.tvDayName.setTextColor(ContextCompat.getColor(context, R.color.text_muted));
                binding.viewStatusDot.setVisibility(View.GONE);
            } else if (day.isAvailable()) {
                // GREEN - DOCTOR AVAILABLE
                binding.llDayContainer.setBackgroundResource(R.drawable.bg_calendar_day_available);
                binding.tvDayNumber.setTextColor(ContextCompat.getColor(context, R.color.success_green));
                binding.tvDayName.setTextColor(ContextCompat.getColor(context, R.color.success_green));
                binding.viewStatusDot.setVisibility(View.VISIBLE);
                binding.viewStatusDot.setBackgroundResource(R.drawable.bg_dot_green);
            } else {
                // RED - DOCTOR UNAVAILABLE / OFF
                binding.llDayContainer.setBackgroundResource(R.drawable.bg_calendar_day_unavailable);
                binding.tvDayNumber.setTextColor(ContextCompat.getColor(context, R.color.error_red));
                binding.tvDayName.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
                binding.viewStatusDot.setVisibility(View.VISIBLE);
                binding.viewStatusDot.setBackgroundResource(R.drawable.bg_dot_red);
            }

            itemView.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && !day.isPast() && day.isCurrentMonth() && day.getDayNumber() > 0) {
                    adapter.setSelectedPosition(pos);
                    if (listener != null) {
                        listener.onDayClick(day);
                    }
                }
            });
        }
    }
}
