package com.amstudio.drpoint;

import android.app.Application;
import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

public class DoctorPointApp extends Application {

    private static DoctorPointApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        // Force Light Mode globally across the app
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }

    public static DoctorPointApp getInstance() {
        return instance;
    }

    public static Context getAppContext() {
        return instance != null ? instance.getApplicationContext() : null;
    }
}
