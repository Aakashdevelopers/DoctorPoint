package com.amstudio.drpoint.ui.profile;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
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
import com.amstudio.drpoint.ui.doctor.SavedDoctorsActivity;
import com.amstudio.drpoint.ui.explore.FindDoctorsActivity;
import com.amstudio.drpoint.ui.home.NotificationsActivity;
import com.amstudio.drpoint.ui.main.MainActivity;
import com.amstudio.drpoint.util.DummyDataProvider;
import com.amstudio.drpoint.util.PreferenceManager;
import com.amstudio.drpoint.util.ToastUtils;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.yalantis.ucrop.UCrop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

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
    private ActivityResultLauncher<Intent> cropLauncher;
    private BottomSheetEditProfileBinding activeSheetBinding;
    private static long profileCacheTimestamp = 0;
    private static final long PROFILE_CACHE_TTL_MS = 5 * 60 * 1000L; // 5 minutes cache

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupEditImagePicker();
    }

    private void setupEditImagePicker() {
        cropLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri croppedUri = UCrop.getOutput(result.getData());
                        if (croppedUri != null && activeSheetBinding != null) {
                            try {
                                Glide.with(this)
                                        .load(croppedUri)
                                        .placeholder(R.drawable.ic_user)
                                        .into(activeSheetBinding.ivEditProfilePic);
                            } catch (Exception e) {
                                activeSheetBinding.ivEditProfilePic.setImageURI(croppedUri);
                            }
                            uploadEditImageToSupabaseStorage(croppedUri);
                        }
                    } else if (result.getResultCode() == UCrop.RESULT_ERROR && result.getData() != null) {
                        Throwable cropError = UCrop.getError(result.getData());
                        Context ctx = getContext();
                        if (ctx != null) {
                            ToastUtils.showError(ctx, "Crop failed: " + (cropError != null ? cropError.getMessage() : "Unknown error"));
                        }
                    }
                }
        );

        editImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        Uri sourceUri = result.getData().getData();
                        launchImageCrop(sourceUri);
                    }
                }
        );
    }

    private void launchImageCrop(Uri sourceUri) {
        Context context = getContext();
        if (context == null || sourceUri == null) return;

        try {
            File cacheDir = context.getCacheDir();
            File cropFile = new File(cacheDir, "crop_profile_" + System.currentTimeMillis() + ".jpg");
            Uri destinationUri = Uri.fromFile(cropFile);

            UCrop.Options options = new UCrop.Options();
            options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
            options.setCompressionQuality(90);
            options.setCircleDimmedLayer(true);
            options.setShowCropGrid(false);
            options.setToolbarColor(ContextCompat.getColor(context, R.color.primary));
            options.setStatusBarColor(ContextCompat.getColor(context, R.color.primary));
            options.setToolbarWidgetColor(ContextCompat.getColor(context, R.color.white));
            options.setToolbarTitle("Crop Profile Photo");

            Intent cropIntent = UCrop.of(sourceUri, destinationUri)
                    .withAspectRatio(1, 1)
                    .withMaxResultSize(800, 800)
                    .withOptions(options)
                    .getIntent(context);

            cropLauncher.launch(cropIntent);
        } catch (Exception e) {
            Log.e("ProfileFragment", "Error launching crop: " + e.getMessage());
            ToastUtils.showError(context, "Could not open crop tool. Please try again.");
        }
    }

    private void uploadEditImageToSupabaseStorage(Uri imageUri) {
        Context context = getContext();
        if (context == null || activeSheetBinding == null) return;

        String userId = PreferenceManager.getInstance(context).getUserId();
        if (userId == null || userId.trim().isEmpty()) {
            userId = "user_" + System.currentTimeMillis();
        }

        activeSheetBinding.tvEditUploadLabel.setText("Uploading photo to Supabase...");
        final String currentUserId = userId;
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
                byte[] imageBytes = getBytes(inputStream);

                String fileName = "profile_" + currentUserId + "_" + System.currentTimeMillis() + ".jpg";
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
                    Activity activity = getActivity();
                    if (isAdded() && activity != null && !activity.isFinishing()) {
                        activity.runOnUiThread(() -> {
                            Context ctx = getContext();
                            if (activeSheetBinding != null && ctx != null) {
                                activeSheetBinding.tvEditUploadLabel.setText("Photo Uploaded!");
                                ToastUtils.showSuccess(ctx, "Profile photo uploaded successfully!");
                            }
                        });
                    }
                    return;
                }
            } catch (Exception e) {
                Log.e("ProfileFragment", "Supabase image upload failed: " + e.getMessage());
            }
            Activity activity = getActivity();
            if (isAdded() && activity != null && !activity.isFinishing()) {
                activity.runOnUiThread(() -> {
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

        if (binding == null) return;

        updateProfileHeader();

        binding.ivEditProfileTop.setOnClickListener(v -> openEditProfileBottomSheet());
        binding.flEditProfileTop.setOnClickListener(v -> openEditProfileBottomSheet());
        binding.tvEditProfile.setOnClickListener(v -> openEditProfileBottomSheet());

        binding.cardJoinAsDoctor.setOnClickListener(v -> openJoinAsDoctorBottomSheet());
        binding.btnJoinDoctorBanner.setOnClickListener(v -> openJoinAsDoctorBottomSheet());

        View.OnClickListener customerCareListener = v -> openCustomerCareBottomSheet();
        if (binding.cardCustomerCareSupport != null) {
            binding.cardCustomerCareSupport.setOnClickListener(customerCareListener);
        }


        setupProfileSections();

        loadProfileFromSupabase();
    }

    private void setupProfileSections() {
        if (binding == null) return;

        if (binding.itemJoinDoctor != null) binding.itemJoinDoctor.setOnClickListener(v -> handleMenuItemClick(new MenuItem("Join as a Doctor")));
        if (binding.itemMyAppointments != null) binding.itemMyAppointments.setOnClickListener(v -> handleMenuItemClick(new MenuItem("My Appointments")));
        if (binding.itemBookingHistory != null) binding.itemBookingHistory.setOnClickListener(v -> handleMenuItemClick(new MenuItem("Booking History")));
        if (binding.itemMedicalRecords != null) binding.itemMedicalRecords.setOnClickListener(v -> handleMenuItemClick(new MenuItem("Medical Records & Reports")));
        if (binding.itemCancelAppointment != null) binding.itemCancelAppointment.setOnClickListener(v -> handleMenuItemClick(new MenuItem("Cancel Appointment")));
        if (binding.itemRefundPayment != null) binding.itemRefundPayment.setOnClickListener(v -> handleMenuItemClick(new MenuItem("Refund & Payment")));
        if (binding.itemSavedDoctors != null) binding.itemSavedDoctors.setOnClickListener(v -> handleMenuItemClick(new MenuItem("Saved Doctors")));
        if (binding.itemFindDoctors != null) binding.itemFindDoctors.setOnClickListener(v -> handleMenuItemClick(new MenuItem("Find Doctors & Specialists")));
        if (binding.itemCustomerCare != null) binding.itemCustomerCare.setOnClickListener(v -> handleMenuItemClick(new MenuItem("Customer Care")));

        if (binding.itemPrivacyPolicy != null) binding.itemPrivacyPolicy.setOnClickListener(v -> handleMenuItemClick(new MenuItem("Privacy Policy")));
        if (binding.itemTermsConditions != null) binding.itemTermsConditions.setOnClickListener(v -> handleMenuItemClick(new MenuItem("Terms & Conditions")));
        if (binding.itemRateUs != null) binding.itemRateUs.setOnClickListener(v -> handleMenuItemClick(new MenuItem("Like us? Give us 5 stars")));
        if (binding.itemLogout != null) binding.itemLogout.setOnClickListener(v -> handleMenuItemClick(new MenuItem("Logout")));
    }

    private void handleMenuItemClick(MenuItem item) {
        if (item == null || item.getTitle() == null) return;
        Context context = getContext();
        if (context == null) return;

        String title = item.getTitle().trim();

        if ("Logout".equalsIgnoreCase(title)) {
            PreferenceManager.getInstance(context).clearSession();
            Intent intent = new Intent(context, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        } else if (title.toLowerCase().contains("history")) {
            Intent intent = new Intent(context, BookingHistoryActivity.class);
            startActivity(intent);
        } else if (title.toLowerCase().contains("record") || title.toLowerCase().contains("medical")) {
            Intent intent = new Intent(context, MedicalRecordsActivity.class);
            startActivity(intent);
        } else if (title.toLowerCase().contains("cancel")) {
            Intent intent = new Intent(context, CancelAppointmentActivity.class);
            startActivity(intent);
        } else if (title.toLowerCase().contains("refund") || title.toLowerCase().contains("payment")) {
            Intent intent = new Intent(context, RefundPaymentActivity.class);
            startActivity(intent);
        } else if (title.toLowerCase().contains("appointment")) {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).selectTab(MainActivity.TAB_APPOINTMENTS);
            } else {
                ToastUtils.showInfo(context, "Opening My Appointments");
            }
        } else if (title.toLowerCase().contains("saved") || title.toLowerCase().contains("favorite")) {
            Intent intent = new Intent(context, SavedDoctorsActivity.class);
            startActivity(intent);
        } else if (title.toLowerCase().contains("find") || title.toLowerCase().contains("specialist")) {
            Intent intent = new Intent(context, FindDoctorsActivity.class);
            startActivity(intent);
        } else if (title.toLowerCase().contains("notification")) {
            Intent intent = new Intent(context, NotificationsActivity.class);
            startActivity(intent);
        } else if (title.toLowerCase().contains("doctor") || title.toLowerCase().contains("join") || title.toLowerCase().contains("apply")) {
            openJoinAsDoctorBottomSheet();
        } else if (title.toLowerCase().contains("customer") || title.toLowerCase().contains("care") || title.toLowerCase().contains("help") || title.toLowerCase().contains("support")) {
            openCustomerCareBottomSheet();
        } else if (title.toLowerCase().contains("privacy")) {
            openPrivacyPolicy();
        } else if (title.toLowerCase().contains("term")) {
            ToastUtils.showInfo(context, "Opening Terms & Conditions...");
        } else if (title.toLowerCase().contains("star") || title.toLowerCase().contains("rate") || title.toLowerCase().contains("like")) {
            ToastUtils.showSuccess(context, "Thank you for giving DoctorPoint 5 stars!");
        } else {
            ToastUtils.showInfo(context, title + " clicked");
        }
    }

    private void openJoinAsDoctorBottomSheet() {
        Context context = getContext();
        if (context == null) return;
        ToastUtils.showInfo(context, "Join as a Doctor feature is coming soon!");
    }

    private void openCustomerCareBottomSheet() {
        if (!isAdded()) return;
        CustomerCareBottomSheetFragment fragment = CustomerCareBottomSheetFragment.newInstance();
        fragment.show(getChildFragmentManager(), CustomerCareBottomSheetFragment.TAG);
    }

    private void openPrivacyPolicy() {
        Context context = getContext();
        if (context == null) return;
        try {
            WebView webView = new WebView(context);
            webView.loadUrl("file:///android_asset/privacy_policy.html");
            new AlertDialog.Builder(context)
                    .setTitle("Privacy Policy")
                    .setView(webView)
                    .setPositiveButton("Close", null)
                    .show();
        } catch (Exception e) {
            ToastUtils.showInfo(context, "Opening Privacy Policy...");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateProfileHeader();
        loadProfileFromSupabase();
    }

    private void updateProfileHeader() {
        if (binding == null || !isAdded()) return;
        Context context = getContext();
        if (context == null) return;

        PreferenceManager prefManager = PreferenceManager.getInstance(context);
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

        if (binding.tvUserName != null) {
            binding.tvUserName.setText(name);
        }
        if (binding.tvUserEmail != null) {
            binding.tvUserEmail.setText(email);
        }

        if (avatar != null && !avatar.trim().isEmpty() && binding.ivUserAvatar != null) {
            try {
                Glide.with(context)
                        .load(avatar)
                        .placeholder(R.drawable.ic_user)
                        .error(R.drawable.ic_user)
                        .into(binding.ivUserAvatar);
            } catch (Exception ignored) {}
        }
    }

    private void loadProfileFromSupabase() {
        if (!isAdded() || binding == null) return;
        Context context = getContext();
        if (context == null) return;

        String userId = PreferenceManager.getInstance(context).getUserId();
        if (userId == null || userId.trim().isEmpty()) return;

        // Use cache if profile was loaded recently (< 5 mins) to avoid redundant Supabase network calls
        if (profileCacheTimestamp > 0 && (System.currentTimeMillis() - profileCacheTimestamp < PROFILE_CACHE_TTL_MS)) {
            return;
        }

        SupabaseClient.getPatientService().getProfile("eq." + userId).enqueue(new Callback<List<PatientProfile>>() {
            @Override
            public void onResponse(Call<List<PatientProfile>> call, Response<List<PatientProfile>> response) {
                if (!isAdded() || getContext() == null || binding == null) return;
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    PatientProfile fetched = response.body().get(0);
                    updateCurrentProfileWith(fetched);
                    updateProfileHeader();
                    profileCacheTimestamp = System.currentTimeMillis();
                }
            }

            @Override
            public void onFailure(Call<List<PatientProfile>> call, Throwable t) {}
        });

        SupabaseClient.getPatientService().getPatientDetails("eq." + userId).enqueue(new Callback<List<PatientProfile>>() {
            @Override
            public void onResponse(Call<List<PatientProfile>> call, Response<List<PatientProfile>> response) {
                if (!isAdded() || getContext() == null || binding == null) return;
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    PatientProfile fetched = response.body().get(0);
                    updateCurrentProfileWith(fetched);
                    updateProfileHeader();
                    profileCacheTimestamp = System.currentTimeMillis();
                }
            }

            @Override
            public void onFailure(Call<List<PatientProfile>> call, Throwable t) {}
        });
    }

    private void updateCurrentProfileWith(PatientProfile fetched) {
        if (fetched == null) return;
        Context context = getContext();
        if (context == null) return;

        if (fetched.getId() != null) currentProfile.setId(fetched.getId());
        if (fetched.getFullName() != null && !fetched.getFullName().isEmpty()) {
            currentProfile.setFullName(fetched.getFullName());
            PreferenceManager.getInstance(context).setUserName(fetched.getFullName());
        }
        if (fetched.getEmail() != null && !fetched.getEmail().isEmpty()) {
            currentProfile.setEmail(fetched.getEmail());
            PreferenceManager.getInstance(context).setUserEmail(fetched.getEmail());
        }
        if (fetched.getPhone() != null && !fetched.getPhone().isEmpty()) {
            currentProfile.setPhone(fetched.getPhone());
            PreferenceManager.getInstance(context).setUserPhone(fetched.getPhone());
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
        Context context = getContext();
        if (!isAdded() || context == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(context);
        BottomSheetEditProfileBinding sheetBinding = BottomSheetEditProfileBinding.inflate(getLayoutInflater());
        activeSheetBinding = sheetBinding;
        pendingAvatarUrl = "";
        dialog.setContentView(sheetBinding.getRoot());

        PreferenceManager prefManager = PreferenceManager.getInstance(context);
        String userId = prefManager.getUserId();

        String existingAvatar = (currentProfile.getAvatarUrl() != null && !currentProfile.getAvatarUrl().isEmpty())
                ? currentProfile.getAvatarUrl() : prefManager.getUserAvatar();

        if (existingAvatar != null && !existingAvatar.isEmpty()) {
            try {
                Glide.with(context)
                        .load(existingAvatar)
                        .placeholder(R.drawable.ic_user)
                        .into(sheetBinding.ivEditProfilePic);
            } catch (Exception ignored) {}
        }

        View.OnClickListener pickListener = v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            editImagePickerLauncher.launch(intent);
        };

        sheetBinding.ivEditProfilePic.setOnClickListener(pickListener);
        sheetBinding.flEditCameraBtn.setOnClickListener(pickListener);
        sheetBinding.tvEditUploadLabel.setOnClickListener(pickListener);

        // Pre-fill fields from current profile / local session immediately
        sheetBinding.etFullName.setText(currentProfile.getFullName() != null && !currentProfile.getFullName().isEmpty() ? currentProfile.getFullName() : prefManager.getUserName());
        sheetBinding.etPhone.setText(currentProfile.getPhone() != null && !currentProfile.getPhone().isEmpty() ? currentProfile.getPhone() : prefManager.getUserPhone());
        sheetBinding.etDob.setText(currentProfile.getDateOfBirth() != null && !currentProfile.getDateOfBirth().isEmpty() ? currentProfile.getDateOfBirth() : prefManager.getUserDob());
        sheetBinding.etGender.setText(currentProfile.getGender() != null && !currentProfile.getGender().isEmpty() ? currentProfile.getGender() : prefManager.getUserGender());
        sheetBinding.etBloodGroup.setText(currentProfile.getBloodGroup() != null && !currentProfile.getBloodGroup().isEmpty() ? currentProfile.getBloodGroup() : prefManager.getUserBloodGroup());
        sheetBinding.etEmergencyContact.setText(currentProfile.getEmergencyContact() != null && !currentProfile.getEmergencyContact().isEmpty() ? currentProfile.getEmergencyContact() : prefManager.getUserEmergencyContact());

        // Setup State Dropdown reusing exact same list from DummyDataProvider
        String[] indianStates = DummyDataProvider.getCleanIndianStatesArray();
        ArrayAdapter<String> stateAdapter = new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, indianStates);
        sheetBinding.actvState.setAdapter(stateAdapter);

        String savedState = (currentProfile.getAddress() != null && !currentProfile.getAddress().trim().isEmpty())
                ? currentProfile.getAddress().trim() : prefManager.getSelectedState();
        if (savedState != null && !savedState.trim().isEmpty()) {
            sheetBinding.actvState.setText(savedState, false);
        }

        View.OnClickListener stateListener = v -> {
            Context ctx = getContext();
            if (ctx == null) return;
            sheetBinding.actvState.showDropDown();
            new MaterialAlertDialogBuilder(ctx)
                    .setTitle("Select State")
                    .setItems(indianStates, (dialogInterface, which) -> {
                        String selected = indianStates[which];
                        sheetBinding.actvState.setText(selected, false);
                        sheetBinding.tilState.setError(null);
                    })
                    .show();
        };

        sheetBinding.actvState.setOnClickListener(stateListener);
        sheetBinding.tilState.setOnClickListener(stateListener);
        sheetBinding.tilState.setEndIconOnClickListener(stateListener);

        // Fetch fresh Supabase profile details specifically for currently logged in user
        if (userId != null && !userId.trim().isEmpty()) {
            SupabaseClient.getPatientService().getProfile("eq." + userId).enqueue(new Callback<List<PatientProfile>>() {
                @Override
                public void onResponse(Call<List<PatientProfile>> call, Response<List<PatientProfile>> response) {
                    if (activeSheetBinding != sheetBinding || !isAdded()) return;
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        PatientProfile p = response.body().get(0);
                        updateCurrentProfileWith(p);
                        if (p.getFullName() != null) sheetBinding.etFullName.setText(p.getFullName());
                        if (p.getPhone() != null) sheetBinding.etPhone.setText(p.getPhone());
                        if (p.getAvatarUrl() != null && !p.getAvatarUrl().isEmpty()) {
                            try {
                                Glide.with(context).load(p.getAvatarUrl()).placeholder(R.drawable.ic_user).into(sheetBinding.ivEditProfilePic);
                            } catch (Exception ignored) {}
                        }
                    }
                }

                @Override
                public void onFailure(Call<List<PatientProfile>> call, Throwable t) {}
            });

            SupabaseClient.getPatientService().getPatientDetails("eq." + userId).enqueue(new Callback<List<PatientProfile>>() {
                @Override
                public void onResponse(Call<List<PatientProfile>> call, Response<List<PatientProfile>> response) {
                    if (activeSheetBinding != sheetBinding || !isAdded()) return;
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        PatientProfile p = response.body().get(0);
                        updateCurrentProfileWith(p);
                        if (p.getDateOfBirth() != null) sheetBinding.etDob.setText(p.getDateOfBirth());
                        if (p.getGender() != null) sheetBinding.etGender.setText(p.getGender());
                        if (p.getBloodGroup() != null) sheetBinding.etBloodGroup.setText(p.getBloodGroup());
                        if (p.getEmergencyContact() != null) sheetBinding.etEmergencyContact.setText(p.getEmergencyContact());
                        if (p.getAddress() != null && !p.getAddress().trim().isEmpty()) {
                            sheetBinding.actvState.setText(p.getAddress().trim(), false);
                        }
                    }
                }

                @Override
                public void onFailure(Call<List<PatientProfile>> call, Throwable t) {}
            });
        }

        View.OnClickListener dobListener = v -> {
            Context ctx = getContext();
            if (ctx == null) return;
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
                    ctx,
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

        String[] genderOptions = new String[]{"Male", "Female", "Prefer not to say"};
        View.OnClickListener genderListener = v -> {
            Context ctx = getContext();
            if (ctx == null) return;
            new MaterialAlertDialogBuilder(ctx)
                    .setTitle("Select Gender")
                    .setItems(genderOptions, (dialogInterface, which) -> {
                        sheetBinding.etGender.setText(genderOptions[which]);
                        sheetBinding.tilGender.setError(null);
                    })
                    .show();
        };

        sheetBinding.etGender.setOnClickListener(genderListener);
        sheetBinding.tilGender.setOnClickListener(genderListener);
        sheetBinding.tilGender.setEndIconOnClickListener(genderListener);

        sheetBinding.btnCancel.setOnClickListener(v -> dialog.dismiss());

        sheetBinding.btnSaveProfile.setOnClickListener(v -> {
            String name = sheetBinding.etFullName.getText() != null ? sheetBinding.etFullName.getText().toString().trim() : "";
            String phone = sheetBinding.etPhone.getText() != null ? sheetBinding.etPhone.getText().toString().trim() : "";
            String dob = sheetBinding.etDob.getText() != null ? sheetBinding.etDob.getText().toString().trim() : "";
            String gender = sheetBinding.etGender.getText() != null ? sheetBinding.etGender.getText().toString().trim() : "";
            String bloodGroup = sheetBinding.etBloodGroup.getText() != null ? sheetBinding.etBloodGroup.getText().toString().trim() : "";
            String emergencyContact = sheetBinding.etEmergencyContact.getText() != null ? sheetBinding.etEmergencyContact.getText().toString().trim() : "";
            String selectedStateChoice = sheetBinding.actvState.getText() != null ? sheetBinding.actvState.getText().toString().trim() : "";

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

            if (TextUtils.isEmpty(selectedStateChoice)) {
                sheetBinding.tilState.setError("State is required");
                isValid = false;
            } else {
                sheetBinding.tilState.setError(null);
            }

            Context ctx = getContext();
            if (!isValid) {
                if (ctx != null) {
                    ToastUtils.showWarning(ctx, "Please fill all mandatory profile details");
                }
                return;
            }

            sheetBinding.btnSaveProfile.setEnabled(false);
            sheetBinding.btnSaveProfile.setText("Saving...");

            saveProfileToSupabase(dialog, sheetBinding, name, phone, dob, gender, bloodGroup, emergencyContact, selectedStateChoice);
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
            String selectedStateChoice
    ) {
        Context context = getContext();
        if (!isAdded() || context == null) return;

        String userId = PreferenceManager.getInstance(context).getUserId();
        if (userId == null || userId.trim().isEmpty()) {
            ToastUtils.showError(context, "User session not found");
            sheetBinding.btnSaveProfile.setEnabled(true);
            sheetBinding.btnSaveProfile.setText("Save Changes");
            dialog.dismiss();
            return;
        }

        Map<String, Object> profileMap = new HashMap<>();
        profileMap.put("id", userId);
        profileMap.put("full_name", name);
        if (!phone.isEmpty()) profileMap.put("phone", phone);
        if (!pendingAvatarUrl.isEmpty()) profileMap.put("avatar_url", pendingAvatarUrl);

        Map<String, Object> patientMap = new HashMap<>();
        patientMap.put("id", userId);
        patientMap.put("full_name", name);
        if (!phone.isEmpty()) patientMap.put("phone", phone);
        if (!pendingAvatarUrl.isEmpty()) patientMap.put("avatar_url", pendingAvatarUrl);
        if (!dob.isEmpty()) patientMap.put("date_of_birth", dob);
        if (!gender.isEmpty()) patientMap.put("gender", gender);
        if (!bloodGroup.isEmpty()) patientMap.put("blood_group", bloodGroup);
        if (!emergencyContact.isEmpty()) patientMap.put("emergency_contact", emergencyContact);
        if (!selectedStateChoice.isEmpty()) patientMap.put("address", selectedStateChoice);

        SupabaseClient.getPatientService().updateProfile("eq." + userId, profileMap).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) {
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

        SupabaseClient.getPatientService().updatePatientDetails("eq." + userId, patientMap).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Context ctx = getContext();
                if (!isAdded() || ctx == null) return;

                sheetBinding.btnSaveProfile.setEnabled(true);
                sheetBinding.btnSaveProfile.setText("Save Changes");

                currentProfile.setFullName(name);
                currentProfile.setPhone(phone);
                currentProfile.setDateOfBirth(dob);
                currentProfile.setGender(gender);
                currentProfile.setBloodGroup(bloodGroup);
                currentProfile.setEmergencyContact(emergencyContact);
                currentProfile.setAddress(selectedStateChoice);
                if (!pendingAvatarUrl.isEmpty()) {
                    currentProfile.setAvatarUrl(pendingAvatarUrl);
                }

                PreferenceManager prefManager = PreferenceManager.getInstance(ctx);
                prefManager.setUserName(name);
                prefManager.setUserPhone(phone);
                prefManager.setUserGender(gender);
                prefManager.setUserBloodGroup(bloodGroup);
                prefManager.setUserAddress(selectedStateChoice);
                prefManager.setSelectedState(selectedStateChoice);
                prefManager.setUserDob(dob);
                prefManager.setUserEmergencyContact(emergencyContact);
                if (!pendingAvatarUrl.isEmpty()) {
                    prefManager.setUserAvatar(pendingAvatarUrl);
                }

                updateProfileHeader();
                ToastUtils.showSuccess(ctx, "Profile updated successfully!");
                dialog.dismiss();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                SupabaseClient.getPatientService().createPatientRecord("resolution=merge-duplicates", patientMap)
                        .enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> c, Response<Void> r) {
                                Context ctx = getContext();
                                if (!isAdded() || ctx == null) return;

                                sheetBinding.btnSaveProfile.setEnabled(true);
                                sheetBinding.btnSaveProfile.setText("Save Changes");

                                currentProfile.setFullName(name);
                                currentProfile.setPhone(phone);
                                currentProfile.setDateOfBirth(dob);
                                currentProfile.setGender(gender);
                                currentProfile.setBloodGroup(bloodGroup);
                                currentProfile.setEmergencyContact(emergencyContact);
                                currentProfile.setAddress(selectedStateChoice);
                                if (!pendingAvatarUrl.isEmpty()) {
                                    currentProfile.setAvatarUrl(pendingAvatarUrl);
                                }

                                PreferenceManager prefManager = PreferenceManager.getInstance(ctx);
                                prefManager.setUserName(name);
                                prefManager.setUserPhone(phone);
                                prefManager.setUserGender(gender);
                                prefManager.setUserBloodGroup(bloodGroup);
                                prefManager.setUserAddress(selectedStateChoice);
                                prefManager.setSelectedState(selectedStateChoice);
                                prefManager.setUserDob(dob);
                                prefManager.setUserEmergencyContact(emergencyContact);
                                if (!pendingAvatarUrl.isEmpty()) {
                                    prefManager.setUserAvatar(pendingAvatarUrl);
                                }

                                updateProfileHeader();
                                ToastUtils.showSuccess(ctx, "Profile updated successfully!");
                                dialog.dismiss();
                            }

                            @Override
                            public void onFailure(Call<Void> c, Throwable t1) {
                                Context ctx = getContext();
                                if (!isAdded() || ctx == null) return;

                                sheetBinding.btnSaveProfile.setEnabled(true);
                                sheetBinding.btnSaveProfile.setText("Save Changes");
                                ToastUtils.showError(ctx, "Unable to update profile. Please try again.");
                            }
                        });
            }
        });
    }

    @Override
    public void onDestroyView() {
        activeSheetBinding = null;
        super.onDestroyView();
        binding = null;
    }
}
