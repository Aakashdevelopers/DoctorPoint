package com.amstudio.drpoint.ui.profile;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.amstudio.drpoint.BuildConfig;
import com.amstudio.drpoint.R;
import com.amstudio.drpoint.adapter.ProfileMenuAdapter;
import com.amstudio.drpoint.databinding.BottomSheetEditProfileBinding;
import com.amstudio.drpoint.databinding.FragmentProfileBinding;
import com.amstudio.drpoint.model.MenuItem;
import com.amstudio.drpoint.model.PatientProfile;
import com.amstudio.drpoint.network.SupabaseClient;
import com.amstudio.drpoint.ui.auth.LoginActivity;
import com.amstudio.drpoint.ui.doctor.DoctorListActivity;
import com.amstudio.drpoint.ui.explore.FindDoctorsActivity;
import com.amstudio.drpoint.ui.home.NotificationsActivity;
import com.amstudio.drpoint.ui.main.MainActivity;
import com.amstudio.drpoint.util.DummyDataProvider;
import com.amstudio.drpoint.util.PreferenceManager;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private PatientProfile currentProfile = new PatientProfile();
    private String pendingAvatarUrl = "";
    private ActivityResultLauncher<Intent> editImagePickerLauncher;
    private BottomSheetEditProfileBinding activeSheetBinding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupEditImagePicker();
    }

    private void setupEditImagePicker() {
        editImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (activeSheetBinding != null) {
                            activeSheetBinding.ivEditProfilePic.setImageURI(imageUri);
                            uploadEditImageToSupabaseStorage(imageUri);
                        }
                    }
                }
        );
    }

    private void uploadEditImageToSupabaseStorage(Uri imageUri) {
        if (activeSheetBinding == null) return;
        activeSheetBinding.tvEditUploadLabel.setText("Uploading photo to Supabase...");
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
                byte[] imageBytes = getBytes(inputStream);

                String fileName = "profile_" + System.currentTimeMillis() + ".jpg";
                String uploadUrl = BuildConfig.SUPABASE_URL + "storage/v1/object/profiile/" + fileName;
                String publicUrl = BuildConfig.SUPABASE_URL + "storage/v1/object/public/profiile/" + fileName;

                OkHttpClient client = new OkHttpClient();
                RequestBody requestBody = RequestBody.create(imageBytes, MediaType.parse("image/jpeg"));

                Request request = new Request.Builder()
                        .url(uploadUrl)
                        .post(requestBody)
                        .addHeader("Authorization", "Bearer " + BuildConfig.SUPABASE_KEY)
                        .addHeader("apikey", BuildConfig.SUPABASE_KEY)
                        .addHeader("x-upsert", "true")
                        .addHeader("Content-Type", "image/jpeg")
                        .build();

                okhttp3.Response response = client.newCall(request).execute();
                if (response.isSuccessful() || response.code() == 200 || response.code() == 201) {
                    pendingAvatarUrl = publicUrl;
                    if (isAdded() && getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (activeSheetBinding != null) {
                                activeSheetBinding.tvEditUploadLabel.setText("Photo Uploaded!");
                                Toast.makeText(requireContext(), "Profile Photo Uploaded to Supabase!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    return;
                }
            } catch (Exception e) {
                Log.e("ProfileFragment", "Supabase image upload failed: " + e.getMessage());
            }
            if (isAdded() && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (activeSheetBinding != null) {
                        activeSheetBinding.tvEditUploadLabel.setText("Tap to Change Profile Photo");
                    }
                });
            }
        });
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

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

        binding.ivEditProfileTop.setOnClickListener(v -> openEditProfileBottomSheet());
        binding.flEditProfileTop.setOnClickListener(v -> openEditProfileBottomSheet());
        binding.tvEditProfile.setOnClickListener(v -> openEditProfileBottomSheet());

        setupProfileSections();

        loadProfileFromSupabase();
    }

    private void setupProfileSections() {
        ProfileMenuAdapter.OnMenuItemClickListener clickListener = this::handleMenuItemClick;

        // Section 1: My History
        ProfileMenuAdapter historyAdapter = new ProfileMenuAdapter(clickListener);
        binding.rvHistoryMenu.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHistoryMenu.setAdapter(historyAdapter);
        historyAdapter.submitList(DummyDataProvider.getHistoryMenuItems());

        // Section 2: Help & Support
        ProfileMenuAdapter helpAdapter = new ProfileMenuAdapter(clickListener);
        binding.rvHelpMenu.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHelpMenu.setAdapter(helpAdapter);
        helpAdapter.submitList(DummyDataProvider.getHelpSupportMenuItems());

        // Section 3: More
        ProfileMenuAdapter moreAdapter = new ProfileMenuAdapter(clickListener);
        binding.rvMoreMenu.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMoreMenu.setAdapter(moreAdapter);
        moreAdapter.submitList(DummyDataProvider.getMoreMenuItems());
    }

    private void handleMenuItemClick(MenuItem item) {
        if (item == null || item.getTitle() == null) return;
        String title = item.getTitle().trim();

        if ("Logout".equalsIgnoreCase(title)) {
            PreferenceManager.getInstance(requireContext()).clearSession();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        } else if (title.toLowerCase().contains("appointment")) {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).selectTab(MainActivity.TAB_APPOINTMENTS);
            } else {
                Toast.makeText(requireContext(), "Opening My Appointments", Toast.LENGTH_SHORT).show();
            }
        } else if (title.toLowerCase().contains("saved") || title.toLowerCase().contains("favorite")) {
            Intent intent = new Intent(requireContext(), DoctorListActivity.class);
            intent.putExtra("category_name", "Saved Doctors");
            startActivity(intent);
        } else if (title.toLowerCase().contains("find") || title.toLowerCase().contains("specialist")) {
            Intent intent = new Intent(requireContext(), FindDoctorsActivity.class);
            startActivity(intent);
        } else if (title.toLowerCase().contains("notification")) {
            Intent intent = new Intent(requireContext(), NotificationsActivity.class);
            startActivity(intent);
        } else if (title.toLowerCase().contains("help") || title.toLowerCase().contains("support")) {
            Toast.makeText(requireContext(), "Help Center & Support active", Toast.LENGTH_SHORT).show();
        } else if (title.toLowerCase().contains("are you a doctor") || title.toLowerCase().contains("apply")) {
            Toast.makeText(requireContext(), "Thank you for your interest! Doctor onboarding form will open shortly.", Toast.LENGTH_LONG).show();
        } else if (title.toLowerCase().contains("privacy")) {
            Toast.makeText(requireContext(), "Opening Privacy Policy...", Toast.LENGTH_SHORT).show();
        } else if (title.toLowerCase().contains("term")) {
            Toast.makeText(requireContext(), "Opening Terms & Conditions...", Toast.LENGTH_SHORT).show();
        } else if (title.toLowerCase().contains("star") || title.toLowerCase().contains("rate") || title.toLowerCase().contains("like")) {
            Toast.makeText(requireContext(), "Thank you for giving DoctorPoint 5 stars!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(requireContext(), title + " clicked", Toast.LENGTH_SHORT).show();
        }
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
        String avatar = (currentProfile.getAvatarUrl() != null && !currentProfile.getAvatarUrl().isEmpty())
                ? currentProfile.getAvatarUrl() : prefManager.getUserAvatar();

        if (currentProfile.getFullName() != null && !currentProfile.getFullName().isEmpty()) {
            name = currentProfile.getFullName();
        }
        if (currentProfile.getEmail() != null && !currentProfile.getEmail().isEmpty()) {
            email = currentProfile.getEmail();
        }

        binding.tvUserName.setText(name);
        binding.tvUserEmail.setText(email);

        if (avatar != null && !avatar.trim().isEmpty()) {
            Glide.with(this)
                    .load(avatar)
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
        activeSheetBinding = sheetBinding;
        pendingAvatarUrl = "";
        dialog.setContentView(sheetBinding.getRoot());

        PreferenceManager prefManager = PreferenceManager.getInstance(requireContext());

        String existingAvatar = (currentProfile.getAvatarUrl() != null && !currentProfile.getAvatarUrl().isEmpty())
                ? currentProfile.getAvatarUrl() : prefManager.getUserAvatar();

        if (existingAvatar != null && !existingAvatar.isEmpty()) {
            Glide.with(this)
                    .load(existingAvatar)
                    .placeholder(R.drawable.ic_user)
                    .into(sheetBinding.ivEditProfilePic);
        }

        View.OnClickListener pickListener = v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            editImagePickerLauncher.launch(intent);
        };

        sheetBinding.ivEditProfilePic.setOnClickListener(pickListener);
        sheetBinding.flEditCameraBtn.setOnClickListener(pickListener);
        sheetBinding.tvEditUploadLabel.setOnClickListener(pickListener);

        // Pre-populate fields
        sheetBinding.etFullName.setText(currentProfile.getFullName() != null ? currentProfile.getFullName() : prefManager.getUserName());
        sheetBinding.etPhone.setText(currentProfile.getPhone() != null ? currentProfile.getPhone() : prefManager.getUserPhone());
        sheetBinding.etDob.setText(currentProfile.getDateOfBirth() != null ? currentProfile.getDateOfBirth() : "");
        sheetBinding.etGender.setText(currentProfile.getGender() != null ? currentProfile.getGender() : "");
        sheetBinding.etBloodGroup.setText(currentProfile.getBloodGroup() != null ? currentProfile.getBloodGroup() : "");
        sheetBinding.etEmergencyContact.setText(currentProfile.getEmergencyContact() != null ? currentProfile.getEmergencyContact() : "");
        sheetBinding.etAddress.setText(currentProfile.getAddress() != null ? currentProfile.getAddress() : "");

        // Setup Date of Birth Calendar Picker
        View.OnClickListener dobListener = v -> {
            Calendar calendar = Calendar.getInstance();
            String currentDob = sheetBinding.etDob.getText() != null ? sheetBinding.etDob.getText().toString().trim() : "";
            if (!currentDob.isEmpty() && currentDob.matches("\\d{4}-\\d{2}-\\d{2}")) {
                try {
                    String[] parts = currentDob.split("-");
                    calendar.set(Calendar.YEAR, Integer.parseInt(parts[0]));
                    calendar.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
                    calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[2]));
                } catch (Exception ignored) {}
            }

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view1, year, month, dayOfMonth) -> {
                        String formattedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                        sheetBinding.etDob.setText(formattedDate);
                        sheetBinding.tilDob.setError(null);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            datePickerDialog.show();
        };

        sheetBinding.etDob.setOnClickListener(dobListener);
        sheetBinding.tilDob.setOnClickListener(dobListener);
        sheetBinding.tilDob.setEndIconOnClickListener(dobListener);

        // Setup Gender Selection Dialog (Male, Female, Prefer not to say)
        String[] genderOptions = new String[]{"Male", "Female", "Prefer not to say"};
        View.OnClickListener genderListener = v -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Gender")
                .setItems(genderOptions, (dialogInterface, which) -> {
                    sheetBinding.etGender.setText(genderOptions[which]);
                    sheetBinding.tilGender.setError(null);
                })
                .show();

        sheetBinding.etGender.setOnClickListener(genderListener);
        sheetBinding.tilGender.setOnClickListener(genderListener);
        sheetBinding.tilGender.setEndIconOnClickListener(genderListener);

        // Setup State Selection Dialog
        String[] indianStates = new String[]{
                "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh",
                "Delhi", "Goa", "Gujarat", "Haryana", "Himachal Pradesh",
                "Jammu and Kashmir", "Jharkhand", "Karnataka", "Kerala", "Madhya Pradesh",
                "Maharashtra", "Manipur", "Meghalaya", "Mizoram", "Nagaland",
                "Odisha", "Punjab", "Rajasthan", "Sikkim", "Tamil Nadu",
                "Telangana", "Tripura", "Uttar Pradesh", "Uttarakhand", "West Bengal"
        };
        View.OnClickListener stateListener = v -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select State")
                .setItems(indianStates, (dialogInterface, which) -> {
                    sheetBinding.etAddress.setText(indianStates[which]);
                    sheetBinding.tilAddress.setError(null);
                })
                .show();

        sheetBinding.etAddress.setOnClickListener(stateListener);
        sheetBinding.tilAddress.setOnClickListener(stateListener);
        sheetBinding.tilAddress.setEndIconOnClickListener(stateListener);

        sheetBinding.btnCancel.setOnClickListener(v -> dialog.dismiss());

        sheetBinding.btnSaveProfile.setOnClickListener(v -> {
            String name = sheetBinding.etFullName.getText() != null ? sheetBinding.etFullName.getText().toString().trim() : "";
            String phone = sheetBinding.etPhone.getText() != null ? sheetBinding.etPhone.getText().toString().trim() : "";
            String dob = sheetBinding.etDob.getText() != null ? sheetBinding.etDob.getText().toString().trim() : "";
            String gender = sheetBinding.etGender.getText() != null ? sheetBinding.etGender.getText().toString().trim() : "";
            String bloodGroup = sheetBinding.etBloodGroup.getText() != null ? sheetBinding.etBloodGroup.getText().toString().trim() : "";
            String emergencyContact = sheetBinding.etEmergencyContact.getText() != null ? sheetBinding.etEmergencyContact.getText().toString().trim() : "";
            String address = sheetBinding.etAddress.getText() != null ? sheetBinding.etAddress.getText().toString().trim() : "";

            boolean isValid = true;

            if (TextUtils.isEmpty(name)) {
                sheetBinding.tilFullName.setError("Full name is required");
                isValid = false;
            } else {
                sheetBinding.tilFullName.setError(null);
            }

            if (TextUtils.isEmpty(phone)) {
                sheetBinding.tilPhone.setError("Phone number is required");
                isValid = false;
            } else if (!phone.matches("\\d{10}")) {
                sheetBinding.tilPhone.setError("Enter valid 10-digit phone number");
                isValid = false;
            } else {
                sheetBinding.tilPhone.setError(null);
            }

            if (TextUtils.isEmpty(dob)) {
                sheetBinding.tilDob.setError("Date of birth is required");
                isValid = false;
            } else {
                sheetBinding.tilDob.setError(null);
            }

            if (TextUtils.isEmpty(gender)) {
                sheetBinding.tilGender.setError("Gender is required");
                isValid = false;
            } else {
                sheetBinding.tilGender.setError(null);
            }

            if (TextUtils.isEmpty(bloodGroup)) {
                sheetBinding.tilBloodGroup.setError("Blood group is required");
                isValid = false;
            } else {
                sheetBinding.tilBloodGroup.setError(null);
            }

            if (TextUtils.isEmpty(emergencyContact)) {
                sheetBinding.tilEmergencyContact.setError("Emergency contact is required");
                isValid = false;
            } else if (!emergencyContact.matches("\\d{10}")) {
                sheetBinding.tilEmergencyContact.setError("Enter valid 10-digit emergency contact");
                isValid = false;
            } else {
                sheetBinding.tilEmergencyContact.setError(null);
            }

            if (TextUtils.isEmpty(address)) {
                sheetBinding.tilAddress.setError("Address is required");
                isValid = false;
            } else {
                sheetBinding.tilAddress.setError(null);
            }

            if (!isValid) {
                Toast.makeText(requireContext(), "Please fill all mandatory profile details", Toast.LENGTH_SHORT).show();
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
        if (!pendingAvatarUrl.isEmpty()) profileMap.put("avatar_url", pendingAvatarUrl);

        // Map for patients table
        Map<String, Object> patientMap = new HashMap<>();
        patientMap.put("id", userId);
        patientMap.put("full_name", name);
        if (!phone.isEmpty()) patientMap.put("phone", phone);
        if (!pendingAvatarUrl.isEmpty()) patientMap.put("avatar_url", pendingAvatarUrl);
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
                prefManager.setUserGender(gender);
                prefManager.setUserBloodGroup(bloodGroup);
                prefManager.setUserAddress(address);
                prefManager.setUserDob(dob);
                prefManager.setUserEmergencyContact(emergencyContact);
                if (!pendingAvatarUrl.isEmpty()) {
                    prefManager.setUserAvatar(pendingAvatarUrl);
                }

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
