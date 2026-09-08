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
import com.amstudio.drpoint.databinding.ItemAppointmentCardBinding;
import com.amstudio.drpoint.model.Appointment;

public class AppointmentListAdapter extends ListAdapter<Appointment, AppointmentListAdapter.ViewHolder> {

    public interface OnAppointmentActionListener {
        void onRescheduleClick(Appointment appointment);
        void onCancelClick(Appointment appointment);
    }

    private static final DiffUtil.ItemCallback<Appointment> DIFF_CALLBACK = new DiffUtil.ItemCallback<Appointment>() {
        @Override
        public boolean areItemsTheSame(@NonNull Appointment oldItem, @NonNull Appointment newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Appointment oldItem, @NonNull Appointment newItem) {
            return oldItem.getStatus().equals(newItem.getStatus()) &&
                   oldItem.getDate().equals(newItem.getDate()) &&
                   oldItem.getTime().equals(newItem.getTime());
        }
    };

    private final OnAppointmentActionListener listener;

    public AppointmentListAdapter(OnAppointmentActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAppointmentCardBinding binding = ItemAppointmentCardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemAppointmentCardBinding binding;

        ViewHolder(@NonNull ItemAppointmentCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Appointment appointment, OnAppointmentActionListener listener) {
            Context context = itemView.getContext();

            binding.tvApptDoctorName.setText(appointment.getDoctorName());
            binding.tvApptSpecialization.setText(appointment.getSpecialization());
            binding.tvApptTime.setText(appointment.getTime());
            binding.tvApptLocation.setText(appointment.getClinicName() + " • " + appointment.getLocation());
            binding.tvStatusText.setText(appointment.getStatus());

            String dateStr = appointment.getDate();
            if (dateStr != null && dateStr.contains(" ")) {
                String[] parts = dateStr.split(" ");
                binding.tvApptDayNum.setText(parts[0]);
                binding.tvApptMonth.setText(parts[1].toUpperCase());
            } else {
                binding.tvApptDayNum.setText("05");
                binding.tvApptMonth.setText("SEP");
            }

            if ("Completed".equalsIgnoreCase(appointment.getStatus()) || "Cancelled".equalsIgnoreCase(appointment.getStatus())) {
                binding.llActionButtons.setVisibility(View.GONE);
                if ("Cancelled".equalsIgnoreCase(appointment.getStatus())) {
                    binding.tvStatusText.setTextColor(ContextCompat.getColor(context, R.color.error_red));
                    binding.llStatusBadge.setBackgroundResource(R.drawable.bg_date_chip_unselected);
                } else {
                    binding.tvStatusText.setTextColor(ContextCompat.getColor(context, R.color.primary));
                    binding.llStatusBadge.setBackgroundResource(R.drawable.bg_badge_verified);
                }
            } else {
                binding.llActionButtons.setVisibility(View.VISIBLE);
                binding.tvStatusText.setTextColor(ContextCompat.getColor(context, R.color.success_green));
                binding.llStatusBadge.setBackgroundResource(R.drawable.bg_badge_verified);
            }

            binding.btnReschedule.setOnClickListener(v -> {
                if (listener != null) listener.onRescheduleClick(appointment);
            });

            binding.btnCancel.setOnClickListener(v -> {
                if (listener != null) listener.onCancelClick(appointment);
            });
        }
    }
}
