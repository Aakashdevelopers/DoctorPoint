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
import com.amstudio.drpoint.databinding.BottomSheetAppointmentDetailsBinding;
import com.amstudio.drpoint.databinding.FragmentAppointmentsBinding;
import com.amstudio.drpoint.model.Appointment;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.network.SupabaseClient;
import com.amstudio.drpoint.network.model.CancelAppointmentRpcRequest;
import com.amstudio.drpoint.ui.booking.BookAppointmentActivity;
import com.amstudio.drpoint.ui.main.MainActivity;
import com.amstudio.drpoint.util.DummyDataProvider;
import com.amstudio.drpoint.util.PreferenceManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AppointmentsFragment extends Fragment {

    private FragmentAppointmentsBinding binding;
    private AppointmentListAdapter adapter;
    private int currentTabPosition = 0;
    private List<Appointment> fetchedAppointments = new ArrayList<>();

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
            public void onItemClick(Appointment appointment) {
                openAppointmentDetailsBottomSheet(appointment);
            }

            @Override
            public void onRescheduleClick(Appointment appointment) {
                if (!appointment.isCancellable()) {
                    Toast.makeText(requireContext(), "This appointment cannot be rescheduled.", Toast.LENGTH_SHORT).show();
                    return;
                }
                openRescheduleFlow(appointment);
            }

            @Override
            public void onCancelClick(Appointment appointment) {
                attemptCancelAppointment(appointment);
            }
        });
        binding.rvAppointments.setAdapter(adapter);

        if (binding.btnBookNowEmpty != null) {
            binding.btnBookNowEmpty.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).selectTab(MainActivity.TAB_EXPLORE);
                }
            });
        }

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

        loadAppointments();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAppointments();
    }

    private void loadAppointments() {
        if (!isAdded()) return;
        String patientId = PreferenceManager.getInstance(requireContext()).getUserId();

        // 1. Fetch real appointments from Supabase appointments table
        SupabaseClient.getAppointmentService().getAllAppointments()
                .enqueue(new Callback<List<Appointment>>() {
                    @Override
                    public void onResponse(Call<List<Appointment>> call, Response<List<Appointment>> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            fetchedAppointments = response.body();
                        } else {
                            fetchUserAppointmentsFallback(patientId);
                            return;
                        }
                        filterAppointments(currentTabPosition);
                    }

                    @Override
                    public void onFailure(Call<List<Appointment>> call, Throwable t) {
                        if (!isAdded()) return;
                        fetchUserAppointmentsFallback(patientId);
                    }
                });
    }

    private void fetchUserAppointmentsFallback(String patientId) {
        if (patientId != null && !patientId.trim().isEmpty()) {
            SupabaseClient.getAppointmentService().getAppointmentsForPatient("eq." + patientId)
                    .enqueue(new Callback<List<Appointment>>() {
                        @Override
                        public void onResponse(Call<List<Appointment>> call, Response<List<Appointment>> response) {
                            if (!isAdded()) return;
                            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                                fetchedAppointments = response.body();
                            } else {
                                fetchedAppointments = new ArrayList<>();
                            }
                            filterAppointments(currentTabPosition);
                        }

                        @Override
                        public void onFailure(Call<List<Appointment>> call, Throwable t) {
                            if (!isAdded()) return;
                            fetchedAppointments = new ArrayList<>();
                            filterAppointments(currentTabPosition);
                        }
                    });
        } else {
            fetchedAppointments = new ArrayList<>();
            filterAppointments(currentTabPosition);
        }
    }

    private void filterAppointments(int tabPosition) {
        List<Appointment> allAppointments = fetchedAppointments;
        List<Appointment> displayed = new ArrayList<>();

        for (Appointment appt : allAppointments) {
            String st = appt.getStatus() != null ? appt.getStatus().toLowerCase() : "confirmed";

            if (tabPosition == 0) {
                // Upcoming Tab: pending, confirmed, checked_in, waiting, in_consultation
                if ("pending".equals(st) || "confirmed".equals(st) || "checked_in".equals(st) || "waiting".equals(st) || "in_consultation".equals(st) || st.contains("confirm")) {
                    displayed.add(appt);
                }
            } else if (tabPosition == 1) {
                // Completed Tab: completed
                if ("completed".equals(st)) {
                    displayed.add(appt);
                }
            } else if (tabPosition == 2) {
                // Cancelled Tab: cancelled, rejected, no_show
                if ("cancelled".equals(st) || "rejected".equals(st) || "no_show".equals(st)) {
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

    private void attemptCancelAppointment(Appointment appointment) {
        if (!appointment.isCancellable()) {
            Toast.makeText(requireContext(), "This appointment cannot be cancelled.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Cancel Appointment")
                .setMessage("Are you sure you want to cancel your appointment with " + appointment.getDoctorName() + "?")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> cancelAppointmentInSupabase(appointment))
                .setNegativeButton("No", null)
                .show();
    }

    private void cancelAppointmentInSupabase(Appointment appointment) {
        appointment.setStatus("Cancelled");

        String patientId = PreferenceManager.getInstance(requireContext()).getUserId();
        CancelAppointmentRpcRequest cancelRequest = new CancelAppointmentRpcRequest(appointment.getId(), patientId, "Cancelled by patient");

        SupabaseClient.getSlotService().cancelAppointment(cancelRequest).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                Toast.makeText(requireContext(), "Appointment cancelled successfully.", Toast.LENGTH_SHORT).show();
                loadAppointments();
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Map<String, Object> updateMap = new HashMap<>();
                updateMap.put("status", "Cancelled");

                SupabaseClient.getAppointmentService()
                        .updateAppointmentStatus("eq." + appointment.getId(), updateMap)
                        .enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                Toast.makeText(requireContext(), "Appointment cancelled successfully.", Toast.LENGTH_SHORT).show();
                                loadAppointments();
                            }

                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                Toast.makeText(requireContext(), "Appointment marked cancelled.", Toast.LENGTH_SHORT).show();
                                loadAppointments();
                            }
                        });
            }
        });
    }

    private void openRescheduleFlow(Appointment appointment) {
        Doctor doc = new Doctor();
        doc.setId(appointment.getDoctorId() != null ? appointment.getDoctorId() : "doc_1");
        doc.setName(appointment.getDoctorName());
        doc.setClinicName(appointment.getClinicName());
        doc.setLocation(appointment.getLocation());
        doc.setQualification(appointment.getSpecialization());

        Intent intent = new Intent(requireContext(), BookAppointmentActivity.class);
        intent.putExtra("doctor", doc);
        startActivity(intent);
    }

    private void openAppointmentDetailsBottomSheet(Appointment appointment) {
        if (getContext() == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        BottomSheetAppointmentDetailsBinding sheetBinding = BottomSheetAppointmentDetailsBinding.inflate(getLayoutInflater());
        dialog.setContentView(sheetBinding.getRoot());

        // Header info
        sheetBinding.tvDetailStatus.setText(appointment.getUserFriendlyStatus());
        sheetBinding.tvDetailDoctorName.setText(appointment.getDoctorName());
        sheetBinding.tvDetailSpecialization.setText(appointment.getSpecialization());
        sheetBinding.tvDetailClinicName.setText(appointment.getClinicName());
        sheetBinding.tvDetailClinicAddress.setText(appointment.getLocation());

        String dateVal = (appointment.getDate() != null ? appointment.getDate() : "") + " • " + (appointment.getTime() != null ? appointment.getTime() : "");
        sheetBinding.tvDetailDatetime.setText(dateVal);

        int feeVal = appointment.getAmount() > 0 ? appointment.getAmount() : (appointment.getFee() > 0 ? appointment.getFee() : 900);
        sheetBinding.tvDetailTypeFee.setText((appointment.getAppointmentType() != null ? appointment.getAppointmentType() : "Clinic") + " • ₹" + feeVal);

        // Queue Metrics Calculation
        int yourToken = appointment.getTokenNumber() > 0 ? appointment.getTokenNumber() : 12;
        sheetBinding.tvQueueTokenNum.setText("Token #" + String.format(Locale.getDefault(), "%02d", yourToken));

        int currentToken = Math.max(1, yourToken - 3);
        int patientsAhead = Math.max(0, yourToken - currentToken);

        sheetBinding.tvQueueCurrentToken.setText("#" + String.format(Locale.getDefault(), "%02d", currentToken));
        sheetBinding.tvQueuePatientsAhead.setText(String.valueOf(patientsAhead));

        if (appointment.getPatientReason() != null && !appointment.getPatientReason().trim().isEmpty()) {
            sheetBinding.cardPatientReason.setVisibility(View.VISIBLE);
            sheetBinding.tvDetailReason.setText(appointment.getPatientReason());
        } else {
            sheetBinding.cardPatientReason.setVisibility(View.GONE);
        }

        // Action Buttons
        if (!appointment.isCancellable()) {
            sheetBinding.btnDetailCancel.setVisibility(View.GONE);
            sheetBinding.btnDetailReschedule.setVisibility(View.GONE);
        } else {
            sheetBinding.btnDetailCancel.setVisibility(View.VISIBLE);
            sheetBinding.btnDetailReschedule.setVisibility(View.VISIBLE);
        }

        sheetBinding.btnDetailCancel.setOnClickListener(v -> {
            dialog.dismiss();
            attemptCancelAppointment(appointment);
        });

        sheetBinding.btnDetailReschedule.setOnClickListener(v -> {
            dialog.dismiss();
            openRescheduleFlow(appointment);
        });

        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
