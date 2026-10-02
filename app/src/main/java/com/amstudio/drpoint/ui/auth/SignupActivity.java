package com.amstudio.drpoint.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.amstudio.drpoint.databinding.ActivitySignupBinding;
import com.amstudio.drpoint.network.SupabaseClient;
import com.amstudio.drpoint.network.model.AuthResponse;
import com.amstudio.drpoint.network.model.ErrorResponse;
import com.amstudio.drpoint.network.model.SignUpRequest;
import com.amstudio.drpoint.network.model.User;
import com.amstudio.drpoint.ui.main.MainActivity;
import com.amstudio.drpoint.util.PreferenceManager;
import com.amstudio.drpoint.util.ToastUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivity extends AppCompatActivity {

    private ActivitySignupBinding binding;

    private final String[] indianStates = new String[]{
            "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh",
            "Delhi", "Goa", "Gujarat", "Haryana", "Himachal Pradesh",
            "Jammu and Kashmir", "Jharkhand", "Karnataka", "Kerala", "Madhya Pradesh",
            "Maharashtra", "Manipur", "Meghalaya", "Mizoram", "Nagaland",
            "Odisha", "Punjab", "Rajasthan", "Sikkim", "Tamil Nadu",
            "Telangana", "Tripura", "Uttar Pradesh", "Uttarakhand", "West Bengal"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupTextWatchers();
        setupStatePicker();

        binding.ivBack.setOnClickListener(v -> finish());
        binding.tvLogin.setOnClickListener(v -> finish());

        binding.btnSignup.setOnClickListener(v -> {
            if (validateInputs()) {
                if (!binding.cbTerms.isChecked()) {
                    ToastUtils.showWarning(SignupActivity.this, "Please agree to the Terms & Conditions");
                    return;
                }

                String name = binding.etName.getText().toString().trim();
                String email = binding.etEmail.getText().toString().trim();
                String phone = binding.etPhone.getText().toString().trim();
                String state = binding.etState.getText().toString().trim();
                String password = binding.etPassword.getText().toString();

                performSupabaseSignup(name, email, phone, state, password);
            }
        });
    }

    private void setupStatePicker() {
        View.OnClickListener stateClickListener = v -> new MaterialAlertDialogBuilder(this)
                .setTitle("Select State")
                .setItems(indianStates, (dialog, which) -> {
                    binding.etState.setText(indianStates[which]);
                    binding.tilState.setError(null);
                })
                .show();

        binding.etState.setOnClickListener(stateClickListener);
        binding.tilState.setOnClickListener(stateClickListener);
        binding.tilState.setEndIconOnClickListener(stateClickListener);
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

    private boolean validateState(String state) {
        if (TextUtils.isEmpty(state)) {
            binding.tilState.setError("State is required");
            return false;
        } else {
            binding.tilState.setError(null);
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
        String state = binding.etState.getText().toString().trim();
        String password = binding.etPassword.getText().toString();
        String confirmPassword = binding.etConfirmPassword.getText().toString();

        boolean isNameValid = validateName(name);
        boolean isEmailValid = validateEmail(email);
        boolean isPhoneValid = validatePhone(phone);
        boolean isStateValid = validateState(state);
        boolean isPasswordValid = validatePassword(password);
        boolean isConfirmValid = validateConfirmPassword(confirmPassword, password);

        return isNameValid && isEmailValid && isPhoneValid && isStateValid && isPasswordValid && isConfirmValid;
    }

    private void performSupabaseSignup(String name, String email, String phone, String state, String password) {
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
                    prefManager.setUserAddress(state);
                    prefManager.setLoggedIn(true);

                    if (authResponse.getAccessToken() != null && !authResponse.getAccessToken().isEmpty()) {
                        prefManager.setAccessToken(authResponse.getAccessToken());
                        prefManager.setRefreshToken(authResponse.getRefreshToken());
                    }

                    ensurePatientProfileInSupabase(userId, name, userEmail, phone, state);

                    ToastUtils.showSuccess(SignupActivity.this, "Account Created Successfully!");
                    Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    String errorMessage = "Signup failed";
                    if (response.errorBody() != null) {
                        try {
                            ErrorResponse errorObj = new Gson().fromJson(response.errorBody().string(), ErrorResponse.class);
                            if (errorObj != null && errorObj.getErrorMessage() != null) {
                                errorMessage = errorObj.getErrorMessage();
                            }
                        } catch (Exception e) {
                            errorMessage = response.message();
                        }
                    }
                    ToastUtils.showError(SignupActivity.this, errorMessage);
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setLoading(false);
                ToastUtils.showError(SignupActivity.this, "Network error. Please check your connection.");
            }
        });
    }

    private void ensurePatientProfileInSupabase(String userId, String name, String email, String phone, String state) {
        Map<String, Object> profileMap = new HashMap<>();
        profileMap.put("id", userId);
        profileMap.put("full_name", name);
        profileMap.put("email", email);
        if (phone != null && !phone.isEmpty()) profileMap.put("phone", phone);
        if (state != null && !state.isEmpty()) profileMap.put("address", state);
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
        if (state != null && !state.isEmpty()) patientMap.put("address", state);

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
        binding.etState.setEnabled(!isLoading);
        binding.etPassword.setEnabled(!isLoading);
        binding.etConfirmPassword.setEnabled(!isLoading);
    }
}
