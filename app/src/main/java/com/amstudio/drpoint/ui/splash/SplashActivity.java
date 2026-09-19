package com.amstudio.drpoint.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.WindowCompat;

import com.amstudio.drpoint.R;
import com.amstudio.drpoint.databinding.ActivitySplashBinding;
import com.amstudio.drpoint.network.SupabaseClient;
import com.amstudio.drpoint.network.model.User;
import com.amstudio.drpoint.ui.auth.LoginActivity;
import com.amstudio.drpoint.ui.main.MainActivity;
import com.amstudio.drpoint.util.PreferenceManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashActivity extends AppCompatActivity {

    private ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.primary));
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false);

        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        new Handler(Looper.getMainLooper()).postDelayed(this::checkSessionAndProceed, 1200);
    }

    private void checkSessionAndProceed() {
        PreferenceManager prefManager = PreferenceManager.getInstance(SplashActivity.this);
        boolean isLoggedIn = prefManager.isLoggedIn();
        String accessToken = prefManager.getAccessToken();

        if (isLoggedIn) {
            if (accessToken != null && !accessToken.trim().isEmpty()) {
                // Background refresh of user details if access token is available
                SupabaseClient.getAuthService().getUser("Bearer " + accessToken).enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(Call<User> call, Response<User> response) {
                        if (isFinishing()) return;
                        if (response.isSuccessful() && response.body() != null) {
                            User user = response.body();
                            if (user.getId() != null) prefManager.setUserId(user.getId());
                            if (user.getEmail() != null) prefManager.setUserEmail(user.getEmail());
                            if (user.getFullName() != null && !user.getFullName().isEmpty()) {
                                prefManager.setUserName(user.getFullName());
                            }
                        }
                        // Do not clear session on 401/403 (token expiration); keep user logged in locally
                        navigateToMain();
                    }

                    @Override
                    public void onFailure(Call<User> call, Throwable t) {
                        if (isFinishing()) return;
                        // Network failure or offline mode, proceed with stored session
                        navigateToMain();
                    }
                });
            } else {
                navigateToMain();
            }
        } else {
            navigateToLogin();
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
