package com.amstudio.drpoint.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;

import com.amstudio.drpoint.BuildConfig;
import com.amstudio.drpoint.DoctorPointApp;
import com.amstudio.drpoint.R;

public class ToastUtils {

    private static final String TAG = "ToastUtils";
    private static Toast sCurrentToast = null;
    private static final Handler sMainHandler = new Handler(Looper.getMainLooper());

    public enum ToastType {
        SUCCESS,
        ERROR,
        WARNING,
        INFO,
        DEFAULT
    }

    public static void showShort(Context context, CharSequence message) {
        show(context, message, Toast.LENGTH_SHORT, ToastType.DEFAULT);
    }

    public static void showLong(Context context, CharSequence message) {
        show(context, message, Toast.LENGTH_LONG, ToastType.DEFAULT);
    }

    public static void showSuccess(Context context, CharSequence message) {
        show(context, message, Toast.LENGTH_SHORT, ToastType.SUCCESS);
    }

    public static void showError(Context context, CharSequence message) {
        show(context, message, Toast.LENGTH_SHORT, ToastType.ERROR);
    }

    public static void showWarning(Context context, CharSequence message) {
        show(context, message, Toast.LENGTH_SHORT, ToastType.WARNING);
    }

    public static void showInfo(Context context, CharSequence message) {
        show(context, message, Toast.LENGTH_SHORT, ToastType.INFO);
    }

    /**
     * Debug toasts are logged to Logcat and shown ONLY during development/debug builds.
     * In production release builds, debug messages are never shown to end users.
     */
    public static void showDebug(Context context, CharSequence message) {
        if (message == null) return;
        Log.d(TAG, "Debug Message: " + message);
        if (BuildConfig.DEBUG) {
            show(context, "[DEBUG] " + message, Toast.LENGTH_SHORT, ToastType.INFO);
        }
    }

    @SuppressWarnings("deprecation")
    public static void show(Context context, CharSequence message, int duration, ToastType type) {
        if (message == null || message.toString().trim().isEmpty()) return;

        Context appContext = (context != null) ? context.getApplicationContext() : DoctorPointApp.getAppContext();
        if (appContext == null) {
            Log.w(TAG, "Cannot show toast, Application context is null");
            return;
        }

        sMainHandler.post(() -> {
            try {
                // Cancel previous Toast immediately to prevent Toast hanging/queueing!
                if (sCurrentToast != null) {
                    sCurrentToast.cancel();
                    sCurrentToast = null;
                }

                Toast toast;
                try {
                    LayoutInflater inflater = LayoutInflater.from(appContext);
                    View customView = inflater.inflate(R.layout.layout_custom_toast, null);
                    TextView tvText = customView.findViewById(R.id.tvToastText);
                    ImageView ivIcon = customView.findViewById(R.id.ivToastIcon);

                    tvText.setText(message);

                    @DrawableRes int iconRes;
                    switch (type) {
                        case SUCCESS:
                            iconRes = R.drawable.ic_toast_success;
                            break;
                        case ERROR:
                            iconRes = R.drawable.ic_toast_error;
                            break;
                        case WARNING:
                            iconRes = R.drawable.ic_toast_warning;
                            break;
                        case INFO:
                        case DEFAULT:
                        default:
                            iconRes = R.drawable.ic_toast_info;
                            break;
                    }
                    ivIcon.setImageResource(iconRes);

                    toast = new Toast(appContext);
                    toast.setView(customView);
                    toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 150);
                } catch (Exception e) {
                    Log.e(TAG, "Custom toast inflate failed, falling back to standard toast", e);
                    toast = Toast.makeText(appContext, message, duration);
                }

                toast.setDuration(duration);
                sCurrentToast = toast;
                toast.show();
            } catch (Exception e) {
                Log.e(TAG, "Failed to display toast", e);
            }
        });
    }

    /**
     * Cancels any currently displaying toast immediately.
     */
    public static void cancel() {
        sMainHandler.post(() -> {
            if (sCurrentToast != null) {
                sCurrentToast.cancel();
                sCurrentToast = null;
            }
        });
    }
}
