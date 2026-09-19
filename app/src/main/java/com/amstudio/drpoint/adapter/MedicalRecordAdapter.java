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
import com.amstudio.drpoint.databinding.ItemMedicalRecordBinding;
import com.amstudio.drpoint.model.MedicalRecord;

import java.util.Objects;

public class MedicalRecordAdapter extends ListAdapter<MedicalRecord, MedicalRecordAdapter.ViewHolder> {

    public interface OnRecordActionListener {
        void onItemClick(MedicalRecord record);
        void onDeleteClick(MedicalRecord record);
    }

    private static final DiffUtil.ItemCallback<MedicalRecord> DIFF_CALLBACK = new DiffUtil.ItemCallback<MedicalRecord>() {
        @Override
        public boolean areItemsTheSame(@NonNull MedicalRecord oldItem, @NonNull MedicalRecord newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull MedicalRecord oldItem, @NonNull MedicalRecord newItem) {
            return Objects.equals(oldItem.getTitle(), newItem.getTitle()) &&
                   Objects.equals(oldItem.getRecordType(), newItem.getRecordType()) &&
                   Objects.equals(oldItem.getFilePath(), newItem.getFilePath());
        }
    };

    private final OnRecordActionListener listener;

    public MedicalRecordAdapter(OnRecordActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMedicalRecordBinding binding = ItemMedicalRecordBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemMedicalRecordBinding binding;

        ViewHolder(@NonNull ItemMedicalRecordBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(MedicalRecord record, OnRecordActionListener listener) {
            Context context = itemView.getContext();

            binding.tvRecordTitle.setText(record.getTitle());
            binding.tvRecordTypeBadge.setText(record.getUserFriendlyType());
            binding.tvRecordDoctorClinic.setText(record.getDoctorName() + " • " + record.getClinicName());
            binding.tvRecordDate.setText(record.getRecordDate() != null ? record.getRecordDate() : "Recent");

            if (record.isDoctorGenerated()) {
                binding.llRecordBadge.setBackgroundResource(R.drawable.bg_membership_gradient);
                binding.tvRecordTypeBadge.setTextColor(ContextCompat.getColor(context, R.color.white));
                binding.ivDeleteRecord.setVisibility(View.GONE); // Doctor prescriptions cannot be deleted by patient
            } else {
                binding.llRecordBadge.setBackgroundResource(R.drawable.bg_badge_verified);
                binding.tvRecordTypeBadge.setTextColor(ContextCompat.getColor(context, R.color.primary));
                binding.ivDeleteRecord.setVisibility(View.VISIBLE);
            }

            if (record.isPdf()) {
                binding.ivFileType.setImageResource(R.drawable.ic_file);
            } else {
                binding.ivFileType.setImageResource(R.drawable.ic_file);
            }

            if (record.getNotes() != null && !record.getNotes().trim().isEmpty()) {
                binding.tvRecordNotes.setVisibility(View.VISIBLE);
                binding.tvRecordNotes.setText(record.getNotes());
            } else {
                binding.tvRecordNotes.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(record);
            });

            binding.ivDeleteRecord.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(record);
            });
        }
    }
}
