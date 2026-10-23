package com.amstudio.drpoint.ui.profile;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.amstudio.drpoint.databinding.ActivityJoinAsDoctorBinding;
import com.amstudio.drpoint.util.PreferenceManager;
import com.amstudio.drpoint.util.WindowInsetsUtil;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class JoinAsDoctorActivity extends AppCompatActivity {

    private static final String TAG = "JoinAsDoctorActivity";
    private static final String PREF_JOIN_DOCTOR_URL = "join_doctor_cached_url";
    private static final String DEFAULT_FALLBACK_URL = "https://doctors.doctorpoint.com/register";

    private ActivityJoinAsDoctorBinding binding;
    private String currentTargetUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityJoinAsDoctorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        WindowInsetsUtil.applySystemBarInsets(binding.getRoot());

        initViews();
        setupWebView();
        setupBackNavigation();
        fetchUrlFromFirebase();
    }

    private void initViews() {
        // Disable SwipeRefreshLayout to prevent accidental refreshes when scrolling the WebView form
        binding.swipeRefreshLayout.setEnabled(false);

        binding.btnRetry.setOnClickListener(v -> fetchUrlFromFirebase());
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.webView != null && binding.webView.canGoBack()) {
                    binding.webView.goBack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebView webView = binding.webView;
        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setGeolocationEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setDisplayZoomControls(false);
        settings.setBuiltInZoomControls(false);
        settings.setSupportZoom(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        // Security: Disable long press / context menus to prevent selecting URL or text
        webView.setOnLongClickListener(v -> true);
        webView.setLongClickable(false);
        webView.setHapticFeedbackEnabled(false);

        registerForContextMenu(webView);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.llErrorState.setVisibility(View.GONE);
                binding.webView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefreshLayout.setRefreshing(false);

                // Security: Disable text selection & touch callouts without breaking form input fields
                view.loadUrl("javascript:(function() { " +
                        "var style = document.createElement('style'); " +
                        "style.type = 'text/css'; " +
                        "style.innerHTML = 'body { -webkit-user-select: none !important; -webkit-touch-callout: none !important; user-select: none !important; } input, select, textarea { -webkit-user-select: text !important; user-select: text !important; }';" +
                        "if (document.head) document.head.appendChild(style); " +
                        "document.oncontextmenu = function() { return false; }; " +
                        "})()");
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request == null || request.getUrl() == null) return false;
                String urlString = request.getUrl().toString();

                if (urlString.startsWith("http://") || urlString.startsWith("https://")) {
                    // Keep navigation within the webview; do NOT open external browser
                    return false;
                }

                // Handle custom schemes safely (tel, mailto, whatsapp)
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, request.getUrl());
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Error handling external URL scheme: " + urlString, e);
                }
                return true;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request != null && request.isForMainFrame()) {
                    showErrorState("Failed to load registration page.");
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    binding.progressBar.setVisibility(View.VISIBLE);
                } else {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.swipeRefreshLayout.setRefreshing(false);
                }
            }
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        // Security: clear native WebView context menu options (like "Copy link", "Select text", etc.)
        menu.clear();
    }

    private void fetchUrlFromFirebase() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.llErrorState.setVisibility(View.GONE);

        String cachedUrl = PreferenceManager.getInstance(this).getString(PREF_JOIN_DOCTOR_URL, DEFAULT_FALLBACK_URL);

        try {
            DatabaseReference databaseRef = FirebaseDatabase.getInstance("https://presets-store-default-rtdb.firebaseio.com")
                    .getReference("settings/join_doctor_url");

            databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String firebaseUrl = snapshot.getValue(String.class);
                    if (!TextUtils.isEmpty(firebaseUrl)) {
                        currentTargetUrl = firebaseUrl.trim();
                        PreferenceManager.getInstance(JoinAsDoctorActivity.this).putString(PREF_JOIN_DOCTOR_URL, currentTargetUrl);
                    } else {
                        currentTargetUrl = cachedUrl;
                    }
                    loadWebPage(currentTargetUrl);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.w(TAG, "Firebase fetch cancelled: " + error.getMessage());
                    currentTargetUrl = cachedUrl;
                    loadWebPage(currentTargetUrl);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase Database fetch", e);
            currentTargetUrl = cachedUrl;
            loadWebPage(currentTargetUrl);
        }
    }

    private void loadWebPage(String url) {
        if (TextUtils.isEmpty(url)) {
            showErrorState("Registration URL is not available.");
            return;
        }
        binding.llErrorState.setVisibility(View.GONE);
        binding.webView.setVisibility(View.VISIBLE);
        binding.webView.loadUrl(url);
    }

    private void showErrorState(String message) {
        binding.progressBar.setVisibility(View.GONE);
        binding.swipeRefreshLayout.setRefreshing(false);
        binding.webView.setVisibility(View.GONE);
        binding.llErrorState.setVisibility(View.VISIBLE);
        if (!TextUtils.isEmpty(message)) {
            binding.tvErrorSubtitle.setText(message);
        }
    }
}
