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

import java.util.Locale;
import java.util.Objects;

public class AppointmentListAdapter extends ListAdapter<Appointment, AppointmentListAdapter.ViewHolder> {

    public interface OnAppointmentActionListener {
        void onItemClick(Appointment appointment);
        void onRescheduleClick(Appointment appointment);
        void onCancelClick(Appointment appointment);
        void onRateDoctorClick(Appointment appointment);
        void onRefundClick(Appointment appointment);
    }

    private static final DiffUtil.ItemCallback<Appointment> DIFF_CALLBACK = new DiffUtil.ItemCallback<Appointment>() {
        @Override
        public boolean areItemsTheSame(@NonNull Appointment oldItem, @NonNull Appointment newItem) {
            if (oldItem == newItem) return true;
            if (oldItem.getId() == null || newItem.getId() == null) return false;
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Appointment oldItem, @NonNull Appointment newItem) {
            return Objects.equals(oldItem.getStatus(), newItem.getStatus()) &&
                   Objects.equals(oldItem.getDate(), newItem.getDate()) &&
                   Objects.equals(oldItem.getTime(), newItem.getTime()) &&
                   Objects.equals(oldItem.getSpecialization(), newItem.getSpecialization()) &&
                   Objects.equals(oldItem.getDoctorName(), newItem.getDoctorName()) &&
                   oldItem.getTokenNumber() == newItem.getTokenNumber();
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
            binding.tvApptTime.setText(appointment.getFormattedTime());
            binding.tvApptLocation.setText(appointment.getClinicName() + " • " + appointment.getLocation());
            binding.tvStatusText.setText(appointment.getUserFriendlyStatus());

            String dateStr = appointment.getDate();
            if (dateStr != null && dateStr.contains(" ")) {
                String[] parts = dateStr.trim().split("\\s+");
                if (parts.length >= 2) {
                    binding.tvApptDayNum.setText(parts[0]);
                    binding.tvApptMonth.setText(parts[1].toUpperCase());
                } else if (parts.length == 1) {
                    binding.tvApptDayNum.setText(parts[0]);
                    binding.tvApptMonth.setText("SEP");
                } else {
                    binding.tvApptDayNum.setText("18");
                    binding.tvApptMonth.setText("SEP");
                }
            } else if (dateStr != null && dateStr.contains("-")) {
                String[] parts = dateStr.trim().split("-");
                if (parts.length >= 3) {
                    binding.tvApptDayNum.setText(parts[2]);
                    binding.tvApptMonth.setText(getMonthName(parts[1]));
                } else if (parts.length == 2) {
                    binding.tvApptDayNum.setText(parts[1]);
                    binding.tvApptMonth.setText(getMonthName(parts[0]));
                } else {
                    binding.tvApptDayNum.setText("18");
                    binding.tvApptMonth.setText("SEP");
                }
            } else {
                binding.tvApptDayNum.setText("18");
                binding.tvApptMonth.setText("SEP");
            }

            String st = appointment.getStatus() != null ? appointment.getStatus().toLowerCase() : "";
            int token = appointment.getTokenNumber();
            String tokenPrefix = token > 0 ? "Token #" + String.format(Locale.US, "%02d", token) + " • " : "";

            if (st.contains("consultation") || st.contains("in_consultation")) {
                binding.tvStatusText.setText(tokenPrefix + "IN CONSULTATION (LIVE)");
                binding.tvStatusText.setTextColor(ContextCompat.getColor(context, R.color.success_green));
                binding.llStatusBadge.setBackgroundResource(R.drawable.bg_badge_verified);
                binding.llActionButtons.setVisibility(View.GONE);
            } else if ("completed".equals(st)) {
                binding.tvStatusText.setText(tokenPrefix + "Completed");
                binding.tvStatusText.setTextColor(ContextCompat.getColor(context, R.color.primary));
                binding.llStatusBadge.setBackgroundResource(R.drawable.bg_badge_verified);

                binding.llActionButtons.setVisibility(View.VISIBLE);
                binding.btnReschedule.setVisibility(View.GONE);
                binding.btnCancel.setVisibility(View.GONE);
                binding.btnRateDoctor.setVisibility(View.VISIBLE);
                binding.btnRequestRefund.setVisibility(View.GONE);
            } else if ("cancelled".equals(st) || "rejected".equals(st) || "no_show".equals(st)) {
                binding.tvStatusText.setText(tokenPrefix + appointment.getUserFriendlyStatus());
                binding.tvStatusText.setTextColor(ContextCompat.getColor(context, R.color.error_red));
                binding.llStatusBadge.setBackgroundResource(R.drawable.bg_date_chip_unselected);

                binding.llActionButtons.setVisibility(View.VISIBLE);
                binding.btnReschedule.setVisibility(View.GONE);
                binding.btnCancel.setVisibility(View.GONE);
                binding.btnRateDoctor.setVisibility(View.GONE);
                binding.btnRequestRefund.setVisibility(View.VISIBLE);
            } else {
                binding.llActionButtons.setVisibility(View.VISIBLE);
                binding.btnReschedule.setVisibility(View.VISIBLE);
                binding.btnCancel.setVisibility(View.VISIBLE);
                binding.btnRateDoctor.setVisibility(View.GONE);
                binding.btnRequestRefund.setVisibility(View.GONE);

                binding.tvStatusText.setText(tokenPrefix + appointment.getUserFriendlyStatus());
                binding.tvStatusText.setTextColor(ContextCompat.getColor(context, R.color.success_green));
                binding.llStatusBadge.setBackgroundResource(R.drawable.bg_badge_verified);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(appointment);
            });

            binding.btnReschedule.setOnClickListener(v -> {
                if (listener != null) listener.onRescheduleClick(appointment);
            });

            binding.btnCancel.setOnClickListener(v -> {
                if (listener != null) listener.onCancelClick(appointment);
            });

            binding.btnRateDoctor.setOnClickListener(v -> {
                if (listener != null) listener.onRateDoctorClick(appointment);
            });

            binding.btnRequestRefund.setOnClickListener(v -> {
                if (listener != null) listener.onRefundClick(appointment);
            });
        }

        private static String getMonthName(String monthNumStr) {
            if (monthNumStr == null) return "SEP";
            switch (monthNumStr.trim()) {
                case "01": case "1": return "JAN";
                case "02": case "2": return "FEB";
                case "03": case "3": return "MAR";
                case "04": case "4": return "APR";
                case "05": case "5": return "MAY";
                case "06": case "6": return "JUN";
                case "07": case "7": return "JUL";
                case "08": case "8": return "AUG";
                case "09": case "9": return "SEP";
                case "10": return "OCT";
                case "11": return "NOV";
                case "12": return "DEC";
                default: return monthNumStr.toUpperCase();
            }
        }
    }
}
