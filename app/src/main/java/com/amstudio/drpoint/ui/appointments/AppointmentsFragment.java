package com.amstudio.drpoint.ui.appointments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.amstudio.drpoint.adapter.AppointmentListAdapter;
import com.amstudio.drpoint.databinding.FragmentAppointmentsBinding;
import com.amstudio.drpoint.model.Appointment;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.ui.booking.BookAppointmentActivity;
import com.amstudio.drpoint.util.DummyDataProvider;
import com.amstudio.drpoint.util.PreferenceManager;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class AppointmentsFragment extends Fragment {

    private FragmentAppointmentsBinding binding;
    private AppointmentListAdapter adapter;
    private int currentTabPosition = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAppointmentsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.rvAppointments.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AppointmentListAdapter(new AppointmentListAdapter.OnAppointmentActionListener() {
            @Override
            public void onRescheduleClick(Appointment appointment) {
                Doctor doc = DummyDataProvider.getDoctors().get(0);
                Intent intent = new Intent(requireContext(), BookAppointmentActivity.class);
                intent.putExtra("doctor", doc);
                startActivity(intent);
            }

            @Override
            public void onCancelClick(Appointment appointment) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Cancel Appointment")
                        .setMessage("Are you sure you want to cancel your appointment with " + appointment.getDoctorName() + "?")
                        .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                            DummyDataProvider.cancelAppointment(appointment.getId());
                            Toast.makeText(requireContext(), "Appointment cancelled", Toast.LENGTH_SHORT).show();
                            filterAppointments(currentTabPosition);
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });
        binding.rvAppointments.setAdapter(adapter);

        PreferenceManager prefManager = PreferenceManager.getInstance(requireContext());
        binding.swReminders.setChecked(prefManager.getRemindersEnabled());
        binding.swReminders.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefManager.setRemindersEnabled(isChecked);
            String msg = isChecked ? "Reminders enabled" : "Reminders disabled";
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        });

        binding.tabLayoutAppointments.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTabPosition = tab.getPosition();
                filterAppointments(currentTabPosition);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        filterAppointments(0);
    }

    private void filterAppointments(int tabPosition) {
        List<Appointment> allAppointments = DummyDataProvider.getAppointments();
        List<Appointment> displayed = new ArrayList<>();

        for (Appointment appt : allAppointments) {
            if (tabPosition == 0) {
                if ("✓ Confirmed".equalsIgnoreCase(appt.getStatus()) || "Confirmed".equalsIgnoreCase(appt.getStatus())) {
                    displayed.add(appt);
                }
            } else if (tabPosition == 1) {
                if ("Completed".equalsIgnoreCase(appt.getStatus())) {
                    displayed.add(appt);
                }
            } else if (tabPosition == 2) {
                if ("Cancelled".equalsIgnoreCase(appt.getStatus())) {
                    displayed.add(appt);
                }
            }
        }

        if (displayed.isEmpty()) {
            binding.llEmptyState.setVisibility(View.VISIBLE);
            binding.rvAppointments.setVisibility(View.GONE);
        } else {
            binding.llEmptyState.setVisibility(View.GONE);
            binding.rvAppointments.setVisibility(View.VISIBLE);
        }

        adapter.submitList(displayed);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
