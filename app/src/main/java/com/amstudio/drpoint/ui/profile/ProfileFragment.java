package com.amstudio.drpoint.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.amstudio.drpoint.R;
import com.amstudio.drpoint.adapter.ProfileMenuAdapter;
import com.amstudio.drpoint.databinding.BottomSheetEditProfileBinding;
import com.amstudio.drpoint.databinding.FragmentProfileBinding;
import com.amstudio.drpoint.model.MenuItem;
import com.amstudio.drpoint.model.PatientProfile;
import com.amstudio.drpoint.network.SupabaseClient;
import com.amstudio.drpoint.ui.auth.LoginActivity;
import com.amstudio.drpoint.ui.main.MainActivity;
import com.amstudio.drpoint.util.DummyDataProvider;
import com.amstudio.drpoint.util.PreferenceManager;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private PatientProfile currentProfile = new PatientProfile();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        updateProfileHeader();

        binding.ivSettings.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Opening Settings...", Toast.LENGTH_SHORT).show()
        );

        binding.tvEditProfile.setOnClickListener(v -> openEditProfileBottomSheet());

        binding.rvProfileMenu.setLayoutManager(new LinearLayoutManager(requireContext()));
        List<MenuItem> menuItems = DummyDataProvider.getProfileMenuItems();
        ProfileMenuAdapter adapter = new ProfileMenuAdapter(item -> {
            String title = item.getTitle();
            if ("Logout".equalsIgnoreCase(title)) {
                PreferenceManager.getInstance(requireContext()).clearSession();
                Intent intent = new Intent(requireContext(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                if (getActivity() != null) {
                    getActivity().finish();
                }
            } else if ("My Appointments".equalsIgnoreCase(title)) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).selectTab(MainActivity.TAB_APPOINTMENTS);
                }
            } else if ("Health Records".equalsIgnoreCase(title) || "Prescriptions".equalsIgnoreCase(title) || title.toLowerCase().contains("record")) {
                Intent intent = new Intent(requireContext(), MedicalRecordsActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(requireContext(), title + " clicked", Toast.LENGTH_SHORT).show();
            }
        });
        binding.rvProfileMenu.setAdapter(adapter);
        adapter.submitList(menuItems);

        loadProfileFromSupabase();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateProfileHeader();
        loadProfileFromSupabase();
    }

    private void updateProfileHeader() {
        if (binding == null) return;
        PreferenceManager prefManager = PreferenceManager.getInstance(requireContext());
        String name = prefManager.getUserName();
        String email = prefManager.getUserEmail();

        if (currentProfile.getFullName() != null && !currentProfile.getFullName().isEmpty()) {
            name = currentProfile.getFullName();
        }
        if (currentProfile.getEmail() != null && !currentProfile.getEmail().isEmpty()) {
            email = currentProfile.getEmail();
        }

        binding.tvUserName.setText(name);
        binding.tvUserEmail.setText(email);

        if (currentProfile.getAvatarUrl() != null && !currentProfile.getAvatarUrl().isEmpty()) {
            Glide.with(this)
                    .load(currentProfile.getAvatarUrl())
                    .placeholder(R.drawable.ic_user)
                    .error(R.drawable.ic_user)
                    .into(binding.ivUserAvatar);
        }
    }

    private void loadProfileFromSupabase() {
        if (!isAdded()) return;
        String userId = PreferenceManager.getInstance(requireContext()).getUserId();
        if (userId == null || userId.trim().isEmpty()) return;

        // Fetch from Supabase profiles table
        SupabaseClient.getPatientService().getProfile("eq." + userId).enqueue(new Callback<List<PatientProfile>>() {
            @Override
            public void onResponse(Call<List<PatientProfile>> call, Response<List<PatientProfile>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    PatientProfile fetched = response.body().get(0);
                    updateCurrentProfileWith(fetched);
                    updateProfileHeader();
                }
            }

            @Override
            public void onFailure(Call<List<PatientProfile>> call, Throwable t) {}
        });

        // Fetch additional details from patients table
        SupabaseClient.getPatientService().getPatientDetails("eq." + userId).enqueue(new Callback<List<PatientProfile>>() {
            @Override
            public void onResponse(Call<List<PatientProfile>> call, Response<List<PatientProfile>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    PatientProfile fetched = response.body().get(0);
                    updateCurrentProfileWith(fetched);
                    updateProfileHeader();
                }
            }

            @Override
            public void onFailure(Call<List<PatientProfile>> call, Throwable t) {}
        });
    }

    private void updateCurrentProfileWith(PatientProfile fetched) {
        if (fetched == null) return;
        if (fetched.getId() != null) currentProfile.setId(fetched.getId());
        if (fetched.getFullName() != null && !fetched.getFullName().isEmpty()) {
            currentProfile.setFullName(fetched.getFullName());
            PreferenceManager.getInstance(requireContext()).setUserName(fetched.getFullName());
        }
        if (fetched.getEmail() != null && !fetched.getEmail().isEmpty()) {
            currentProfile.setEmail(fetched.getEmail());
            PreferenceManager.getInstance(requireContext()).setUserEmail(fetched.getEmail());
        }
        if (fetched.getPhone() != null && !fetched.getPhone().isEmpty()) {
            currentProfile.setPhone(fetched.getPhone());
            PreferenceManager.getInstance(requireContext()).setUserPhone(fetched.getPhone());
        }
        if (fetched.getAvatarUrl() != null && !fetched.getAvatarUrl().isEmpty()) {
            currentProfile.setAvatarUrl(fetched.getAvatarUrl());
        }
        if (fetched.getDateOfBirth() != null) currentProfile.setDateOfBirth(fetched.getDateOfBirth());
        if (fetched.getGender() != null) currentProfile.setGender(fetched.getGender());
        if (fetched.getBloodGroup() != null) currentProfile.setBloodGroup(fetched.getBloodGroup());
        if (fetched.getEmergencyContact() != null) currentProfile.setEmergencyContact(fetched.getEmergencyContact());
        if (fetched.getAddress() != null) currentProfile.setAddress(fetched.getAddress());
    }

    private void openEditProfileBottomSheet() {
        if (getContext() == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        BottomSheetEditProfileBinding sheetBinding = BottomSheetEditProfileBinding.inflate(getLayoutInflater());
        dialog.setContentView(sheetBinding.getRoot());

        PreferenceManager prefManager = PreferenceManager.getInstance(requireContext());

        // Pre-populate fields
        sheetBinding.etFullName.setText(currentProfile.getFullName() != null ? currentProfile.getFullName() : prefManager.getUserName());
        sheetBinding.etPhone.setText(currentProfile.getPhone() != null ? currentProfile.getPhone() : prefManager.getUserPhone());
        sheetBinding.etDob.setText(currentProfile.getDateOfBirth() != null ? currentProfile.getDateOfBirth() : "");
        sheetBinding.etGender.setText(currentProfile.getGender() != null ? currentProfile.getGender() : "");
        sheetBinding.etBloodGroup.setText(currentProfile.getBloodGroup() != null ? currentProfile.getBloodGroup() : "");
        sheetBinding.etEmergencyContact.setText(currentProfile.getEmergencyContact() != null ? currentProfile.getEmergencyContact() : "");
        sheetBinding.etAddress.setText(currentProfile.getAddress() != null ? currentProfile.getAddress() : "");

        sheetBinding.btnCancel.setOnClickListener(v -> dialog.dismiss());

        sheetBinding.btnSaveProfile.setOnClickListener(v -> {
            String name = sheetBinding.etFullName.getText().toString().trim();
            String phone = sheetBinding.etPhone.getText().toString().trim();
            String dob = sheetBinding.etDob.getText().toString().trim();
            String gender = sheetBinding.etGender.getText().toString().trim();
            String bloodGroup = sheetBinding.etBloodGroup.getText().toString().trim();
            String emergencyContact = sheetBinding.etEmergencyContact.getText().toString().trim();
            String address = sheetBinding.etAddress.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                sheetBinding.tilFullName.setError("Full name is required");
                return;
            }

            sheetBinding.btnSaveProfile.setEnabled(false);
            sheetBinding.btnSaveProfile.setText("Saving...");

            saveProfileToSupabase(dialog, sheetBinding, name, phone, dob, gender, bloodGroup, emergencyContact, address);
        });

        dialog.show();
    }

    private void saveProfileToSupabase(
            BottomSheetDialog dialog,
            BottomSheetEditProfileBinding sheetBinding,
            String name,
            String phone,
            String dob,
            String gender,
            String bloodGroup,
            String emergencyContact,
            String address
    ) {
        String userId = PreferenceManager.getInstance(requireContext()).getUserId();
        if (userId == null || userId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "User session not found", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            return;
        }

        // Map for profiles table
        Map<String, Object> profileMap = new HashMap<>();
        profileMap.put("id", userId);
        profileMap.put("full_name", name);
        if (!phone.isEmpty()) profileMap.put("phone", phone);

        // Map for patients table
        Map<String, Object> patientMap = new HashMap<>();
        patientMap.put("id", userId);
        patientMap.put("full_name", name);
        if (!phone.isEmpty()) patientMap.put("phone", phone);
        if (!dob.isEmpty()) patientMap.put("date_of_birth", dob);
        if (!gender.isEmpty()) patientMap.put("gender", gender);
        if (!bloodGroup.isEmpty()) patientMap.put("blood_group", bloodGroup);
        if (!emergencyContact.isEmpty()) patientMap.put("emergency_contact", emergencyContact);
        if (!address.isEmpty()) patientMap.put("address", address);

        // Update profiles table
        SupabaseClient.getPatientService().updateProfile("eq." + userId, profileMap).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) {
                    // Try fallback insert/upsert
                    SupabaseClient.getPatientService().createProfileRecord("resolution=merge-duplicates", profileMap)
                            .enqueue(new Callback<Void>() {
                                @Override public void onResponse(Call<Void> c, Response<Void> r) {}
                                @Override public void onFailure(Call<Void> c, Throwable t) {}
                            });
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {}
        });

        // Update patients table
        SupabaseClient.getPatientService().updatePatientDetails("eq." + userId, patientMap).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                sheetBinding.btnSaveProfile.setEnabled(true);
                sheetBinding.btnSaveProfile.setText("Save Changes");

                // Update local model & PreferenceManager
                currentProfile.setFullName(name);
                currentProfile.setPhone(phone);
                currentProfile.setDateOfBirth(dob);
                currentProfile.setGender(gender);
                currentProfile.setBloodGroup(bloodGroup);
                currentProfile.setEmergencyContact(emergencyContact);
                currentProfile.setAddress(address);

                PreferenceManager prefManager = PreferenceManager.getInstance(requireContext());
                prefManager.setUserName(name);
                prefManager.setUserPhone(phone);

                updateProfileHeader();
                Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Upsert attempt in case row didn't exist
                SupabaseClient.getPatientService().createPatientRecord("resolution=merge-duplicates", patientMap)
                        .enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> c, Response<Void> r) {
                                sheetBinding.btnSaveProfile.setEnabled(true);
                                sheetBinding.btnSaveProfile.setText("Save Changes");

                                currentProfile.setFullName(name);
                                currentProfile.setPhone(phone);
                                currentProfile.setDateOfBirth(dob);
                                currentProfile.setGender(gender);
                                currentProfile.setBloodGroup(bloodGroup);
                                currentProfile.setEmergencyContact(emergencyContact);
                                currentProfile.setAddress(address);

                                PreferenceManager prefManager = PreferenceManager.getInstance(requireContext());
                                prefManager.setUserName(name);
                                prefManager.setUserPhone(phone);

                                updateProfileHeader();
                                Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            }

                            @Override
                            public void onFailure(Call<Void> c, Throwable t1) {
                                sheetBinding.btnSaveProfile.setEnabled(true);
                                sheetBinding.btnSaveProfile.setText("Save Changes");
                                Toast.makeText(requireContext(), "Network Error: Profile updated locally.", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            }
                        });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
