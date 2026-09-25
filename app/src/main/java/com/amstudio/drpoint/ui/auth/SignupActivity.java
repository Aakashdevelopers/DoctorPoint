package com.amstudio.drpoint.ui.auth;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.amstudio.drpoint.databinding.ActivitySignupBinding;
import com.amstudio.drpoint.network.SupabaseClient;
import com.amstudio.drpoint.network.model.AuthResponse;
import com.amstudio.drpoint.network.model.ErrorResponse;
import com.amstudio.drpoint.network.model.SignUpRequest;
import com.amstudio.drpoint.network.model.User;
import com.amstudio.drpoint.ui.main.MainActivity;
import com.amstudio.drpoint.util.PreferenceManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivity extends AppCompatActivity {

    private ActivitySignupBinding binding;
    private String uploadedAvatarUrl = "";
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupDropdowns();
        setupImagePicker();
        setupTextWatchers();

        binding.ivBack.setOnClickListener(v -> finish());
        binding.tvLogin.setOnClickListener(v -> finish());

        binding.btnSignup.setOnClickListener(v -> {
            if (validateInputs()) {
                if (!binding.cbTerms.isChecked()) {
                    Toast.makeText(SignupActivity.this, "Please agree to the Terms & Conditions", Toast.LENGTH_SHORT).show();
                    return;
                }

                String name = binding.etName.getText().toString().trim();
                String email = binding.etEmail.getText().toString().trim();
                String phone = binding.etPhone.getText().toString().trim();
                String password = binding.etPassword.getText().toString();

                performSupabaseSignup(name, email, phone, password);
            }
        });
    }

    private void setupDropdowns() {
        String[] genders = new String[]{"Male", "Female", "Other"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, genders);
        binding.actvGender.setAdapter(genderAdapter);

        String[] bloodGroups = new String[]{"O+", "A+", "B+", "AB+", "O-", "A-", "B-", "AB-"};
        ArrayAdapter<String> bloodAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, bloodGroups);
        binding.actvBloodGroup.setAdapter(bloodAdapter);
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        Uri imageUri = result.getData().getData();
                        binding.ivProfilePic.setImageURI(imageUri);
                        uploadImageToImgBB(imageUri);
                    }
                }
        );

        binding.flCameraBtn.setOnClickListener(v -> pickImageFromGallery());
        binding.ivProfilePic.setOnClickListener(v -> pickImageFromGallery());
        binding.tvUploadLabel.setOnClickListener(v -> pickImageFromGallery());
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void uploadImageToImgBB(Uri imageUri) {
        binding.tvUploadLabel.setText("⌛ Uploading photo...");
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                byte[] imageBytes = getBytes(inputStream);
                String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

                OkHttpClient client = new OkHttpClient();
                RequestBody formBody = new FormBody.Builder()
                        .add("key", "98b5b00cee79645e2ea44a187446c2e3")
                        .add("image", base64Image)
                        .build();

                Request request = new Request.Builder()
                        .url("https://api.imgbb.com/1/upload")
                        .post(formBody)
                        .build();

                okhttp3.Response response = client.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    String respStr = response.body().string();
                    JsonObject json = new Gson().fromJson(respStr, JsonObject.class);
                    if (json.has("data") && json.getAsJsonObject("data").has("url")) {
                        uploadedAvatarUrl = json.getAsJsonObject("data").get("url").getAsString();
                        runOnUiThread(() -> {
                            binding.tvUploadLabel.setText("✅ Photo Uploaded!");
                            Toast.makeText(SignupActivity.this, "Profile Photo Uploaded!", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }
                }
            } catch (Exception e) {
                Log.e("SignUpActivity", "Image upload failed: " + e.getMessage());
            }
            runOnUiThread(() -> binding.tvUploadLabel.setText("📷 Tap to Upload Profile Photo"));
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

    private void setupTextWatchers() {
        binding.etName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateName(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.etEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateEmail(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.etPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePhone(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePassword(s.toString());
                validateConfirmPassword(binding.etConfirmPassword.getText().toString(), s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.etConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateConfirmPassword(s.toString(), binding.etPassword.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private boolean validateName(String name) {
        if (TextUtils.isEmpty(name)) {
            binding.tilName.setError("Full name is required");
            return false;
        } else {
            binding.tilName.setError(null);
            return true;
        }
    }

    private boolean validateEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError("Email address is required");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("Enter a valid email address");
            return false;
        } else {
            binding.tilEmail.setError(null);
            return true;
        }
    }

    private boolean validatePhone(String phone) {
        if (TextUtils.isEmpty(phone)) {
            binding.tilPhone.setError("Phone number is required");
            return false;
        } else if (!phone.matches("\\d{10}")) {
            binding.tilPhone.setError("Phone number must be exactly 10 digits");
            return false;
        } else {
            binding.tilPhone.setError(null);
            return true;
        }
    }

    private boolean validatePassword(String password) {
        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError("Password is required");
            return false;
        } else if (password.length() < 8) {
            binding.tilPassword.setError("Password must be at least 8 characters");
            return false;
        } else {
            binding.tilPassword.setError(null);
            return true;
        }
    }

    private boolean validateConfirmPassword(String confirmPassword, String password) {
        if (TextUtils.isEmpty(confirmPassword)) {
            binding.tilConfirmPassword.setError("Confirm password is required");
            return false;
        } else if (!confirmPassword.equals(password)) {
            binding.tilConfirmPassword.setError("Passwords do not match");
            return false;
        } else {
            binding.tilConfirmPassword.setError(null);
            return true;
        }
    }

    private boolean validateInputs() {
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String password = binding.etPassword.getText().toString();
        String confirmPassword = binding.etConfirmPassword.getText().toString();

        boolean isNameValid = validateName(name);
        boolean isEmailValid = validateEmail(email);
        boolean isPhoneValid = validatePhone(phone);
        boolean isPasswordValid = validatePassword(password);
        boolean isConfirmValid = validateConfirmPassword(confirmPassword, password);

        return isNameValid && isEmailValid && isPhoneValid && isPasswordValid && isConfirmValid;
    }

    private void performSupabaseSignup(String name, String email, String phone, String password) {
        setLoading(true);
        SignUpRequest request = new SignUpRequest(email, password, name, phone);

        SupabaseClient.getAuthService().signUp(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    PreferenceManager prefManager = PreferenceManager.getInstance(SignupActivity.this);

                    User user = authResponse.getUser();
                    String userId = (user != null && user.getId() != null) ? user.getId() : "user_" + System.currentTimeMillis();
                    String userEmail = (user != null && user.getEmail() != null && !user.getEmail().isEmpty()) ? user.getEmail() : email;

                    prefManager.setUserId(userId);
                    prefManager.setUserEmail(userEmail);
                    prefManager.setUserName(name);
                    prefManager.setUserPhone(phone);
                    prefManager.setLoggedIn(true);

                    if (authResponse.getAccessToken() != null && !authResponse.getAccessToken().isEmpty()) {
                        prefManager.setAccessToken(authResponse.getAccessToken());
                        prefManager.setRefreshToken(authResponse.getRefreshToken());
                    }

                    ensurePatientProfileInSupabase(userId, name, userEmail, phone);

                    Toast.makeText(SignupActivity.this, "Account Created Successfully!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    String errorMessage = "Signup failed";
                    if (response.errorBody() != null) {
                        try {
                            ErrorResponse errorObj = new Gson().fromJson(response.errorBody().string(), ErrorResponse.class);
                            if (errorObj != null) {
                                errorMessage = errorObj.getErrorMessage();
                            }
                        } catch (Exception e) {
                            errorMessage = response.message();
                        }
                    }
                    Toast.makeText(SignupActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(SignupActivity.this, "Network Error: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void ensurePatientProfileInSupabase(String userId, String name, String email, String phone) {
        String gender = binding.actvGender.getText().toString().trim();
        String ageStr = binding.etAge.getText().toString().trim();
        String bloodGroup = binding.actvBloodGroup.getText().toString().trim();
        String address = binding.etAddress.getText().toString().trim();

        PreferenceManager pref = PreferenceManager.getInstance(this);
        if (!uploadedAvatarUrl.isEmpty()) {
            pref.setUserAvatar(uploadedAvatarUrl);
        }

        Map<String, Object> profileMap = new HashMap<>();
        profileMap.put("id", userId);
        profileMap.put("full_name", name);
        profileMap.put("email", email);
        if (phone != null && !phone.isEmpty()) profileMap.put("phone", phone);
        if (!uploadedAvatarUrl.isEmpty()) profileMap.put("avatar_url", uploadedAvatarUrl);
        if (!gender.isEmpty()) profileMap.put("gender", gender);
        if (!ageStr.isEmpty()) {
            try { profileMap.put("age", Integer.parseInt(ageStr)); } catch (Exception ignored) {}
        }
        if (!bloodGroup.isEmpty()) profileMap.put("blood_group", bloodGroup);
        if (!address.isEmpty()) profileMap.put("address", address);
        profileMap.put("role", "patient");

        SupabaseClient.getPatientService().createProfileRecord("resolution=merge-duplicates", profileMap)
                .enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> call, Response<Void> response) {}
                    @Override public void onFailure(Call<Void> call, Throwable t) {}
                });

        Map<String, Object> patientMap = new HashMap<>();
        patientMap.put("id", userId);
        patientMap.put("full_name", name);
        patientMap.put("email", email);
        if (phone != null && !phone.isEmpty()) patientMap.put("phone", phone);
        if (!uploadedAvatarUrl.isEmpty()) patientMap.put("avatar_url", uploadedAvatarUrl);

        SupabaseClient.getPatientService().createPatientRecord("resolution=merge-duplicates", patientMap)
                .enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> call, Response<Void> response) {}
                    @Override public void onFailure(Call<Void> call, Throwable t) {}
                });
    }

    private void setLoading(boolean isLoading) {
        binding.btnSignup.setEnabled(!isLoading);
        binding.btnSignup.setText(isLoading ? "Creating Account..." : "Sign Up");
        binding.etName.setEnabled(!isLoading);
        binding.etEmail.setEnabled(!isLoading);
        binding.etPhone.setEnabled(!isLoading);
        binding.etPassword.setEnabled(!isLoading);
        binding.etConfirmPassword.setEnabled(!isLoading);
    }
}
