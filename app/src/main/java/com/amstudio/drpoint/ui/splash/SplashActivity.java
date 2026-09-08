package com.amstudio.drpoint.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.amstudio.drpoint.databinding.ActivitySplashBinding;
import com.amstudio.drpoint.ui.auth.LoginActivity;
import com.amstudio.drpoint.ui.main.MainActivity;
import com.amstudio.drpoint.util.PreferenceManager;

public class SplashActivity extends AppCompatActivity {

    private ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            boolean isLoggedIn = PreferenceManager.getInstance(SplashActivity.this).isLoggedIn();
            Intent intent;
            if (isLoggedIn) {
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }
            startActivity(intent);
            finish();
        }, 1200);
    }
}
