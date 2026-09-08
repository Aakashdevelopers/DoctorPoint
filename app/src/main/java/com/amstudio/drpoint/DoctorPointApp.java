package com.amstudio.drpoint;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

public class DoctorPointApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Force Light Mode globally across the app
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}
