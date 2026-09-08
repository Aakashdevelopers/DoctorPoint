package com.amstudio.drpoint.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.Toast;

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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivity extends AppCompatActivity {

    private ActivitySignupBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
                    if (user != null) {
                        prefManager.setUserId(user.getId());
                        prefManager.setUserEmail(user.getEmail() != null ? user.getEmail() : email);
                        prefManager.setUserName(name);
                        prefManager.setUserPhone(phone);
                    }

                    if (authResponse.getAccessToken() != null && !authResponse.getAccessToken().isEmpty()) {
                        prefManager.setAccessToken(authResponse.getAccessToken());
                        prefManager.setRefreshToken(authResponse.getRefreshToken());
                        prefManager.setLoggedIn(true);

                        Toast.makeText(SignupActivity.this, "Account Created Successfully!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(SignupActivity.this, "Registration Successful! Please check your email to confirm registration.", Toast.LENGTH_LONG).show();
                        finish();
                    }
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
