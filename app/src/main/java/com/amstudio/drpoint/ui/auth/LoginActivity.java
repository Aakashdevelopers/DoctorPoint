package com.amstudio.drpoint.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.amstudio.drpoint.databinding.ActivityLoginBinding;
import com.amstudio.drpoint.network.SupabaseClient;
import com.amstudio.drpoint.network.model.AuthResponse;
import com.amstudio.drpoint.network.model.ErrorResponse;
import com.amstudio.drpoint.network.model.LoginRequest;
import com.amstudio.drpoint.network.model.User;
import com.amstudio.drpoint.ui.main.MainActivity;
import com.amstudio.drpoint.util.PreferenceManager;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupTextWatchers();

        binding.btnLogin.setOnClickListener(v -> {
            if (validateInputs()) {
                String email = binding.etEmail.getText().toString().trim();
                String password = binding.etPassword.getText().toString();
                performSupabaseLogin(email, password);
            }
        });

        binding.btnGoogleLogin.setOnClickListener(v -> {
            PreferenceManager prefManager = PreferenceManager.getInstance(LoginActivity.this);
            prefManager.setLoggedIn(true);
            prefManager.setUserName("Aakash Mishra");
            prefManager.setUserEmail("aakash.mishra@example.com");

            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        binding.tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });

        binding.tvForgotPassword.setOnClickListener(v ->
                Toast.makeText(LoginActivity.this, "Password reset link sent to your email.", Toast.LENGTH_SHORT).show()
        );
    }

    private void setupTextWatchers() {
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

        binding.etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePassword(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private boolean validateEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError("Email or Phone Number is required");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches() && !email.matches("\\d{10}")) {
            binding.tilEmail.setError("Enter a valid email address or 10-digit phone number");
            return false;
        } else {
            binding.tilEmail.setError(null);
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

    private boolean validateInputs() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString();

        boolean isEmailValid = validateEmail(email);
        boolean isPasswordValid = validatePassword(password);

        return isEmailValid && isPasswordValid;
    }

    private void performSupabaseLogin(String email, String password) {
        setLoading(true);
        LoginRequest request = new LoginRequest(email, password);

        SupabaseClient.getAuthService().loginWithPassword(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    PreferenceManager prefManager = PreferenceManager.getInstance(LoginActivity.this);
                    prefManager.setLoggedIn(true);
                    prefManager.setAccessToken(authResponse.getAccessToken());
                    prefManager.setRefreshToken(authResponse.getRefreshToken());

                    User user = authResponse.getUser();
                    if (user != null) {
                        prefManager.setUserId(user.getId());
                        prefManager.setUserEmail(user.getEmail());
                        if (user.getFullName() != null && !user.getFullName().isEmpty()) {
                            prefManager.setUserName(user.getFullName());
                        }
                        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                            prefManager.setUserPhone(user.getPhone());
                        }
                    }

                    Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    String errorMessage = "Login failed";
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
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, "Network Error: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        binding.btnLogin.setEnabled(!isLoading);
        binding.btnLogin.setText(isLoading ? "Logging in..." : "Login");
        binding.etEmail.setEnabled(!isLoading);
        binding.etPassword.setEnabled(!isLoading);
    }
}
