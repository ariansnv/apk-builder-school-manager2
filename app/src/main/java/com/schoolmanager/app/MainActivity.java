package com.schoolmanager.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private WebView webView;
    private boolean updateBlocked = false;
    private boolean webLoaded = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor("#2563eb"));
        }

        webView = new WebView(this);
        setContentView(webView);
        configureWebView();
        checkForUpdate(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!updateBlocked) {
            checkForUpdate(false);
        }
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        String ua = settings.getUserAgentString();
        settings.setUserAgentString(ua + " SchoolManagerApp/1.0");

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
    }

    private void loadAppUrl() {
        if (webLoaded || updateBlocked) {
            return;
        }
        webLoaded = true;
        webView.loadUrl(getString(R.string.app_start_url));
    }

    private void checkForUpdate(boolean initial) {
        new Thread(() -> {
            try {
                String startUrl = getString(R.string.app_start_url);
                String base = startUrl.endsWith("/") ? startUrl : startUrl + "/";
                int versionCode = BuildConfig.VERSION_CODE;
                String brandingHash = getString(R.string.app_branding_hash);
                String checkUrl = base + "api/pwa/app-update?version_code=" + versionCode
                        + "&branding_hash=" + Uri.encode(brandingHash);

                HttpURLConnection conn = (HttpURLConnection) new URL(checkUrl).openConnection();
                conn.setConnectTimeout(12000);
                conn.setReadTimeout(12000);
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("User-Agent", "SchoolManagerApp/1.0");

                if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    mainHandler.post(() -> {
                        if (initial) {
                            loadAppUrl();
                        }
                    });
                    return;
                }

                StringBuilder body = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        body.append(line);
                    }
                }

                JSONObject json = new JSONObject(body.toString());
                boolean required = json.optBoolean("update_required", false);
                if (!required) {
                    mainHandler.post(() -> {
                        if (initial) {
                            loadAppUrl();
                        }
                    });
                    return;
                }

                String message = json.optString("message",
                        "نسخه جدید منتشر شده است. لطفاً آخرین نسخه را نصب کنید.");
                String downloadUrl = json.optString("download_url", base + "app/apk");

                mainHandler.post(() -> showUpdateDialog(message, downloadUrl));
            } catch (Exception ignored) {
                mainHandler.post(() -> {
                    if (initial) {
                        loadAppUrl();
                    }
                });
            }
        }).start();
    }

    private void showUpdateDialog(String message, String downloadUrl) {
        updateBlocked = true;
        webView.loadUrl("about:blank");

        new AlertDialog.Builder(this)
                .setTitle("به‌روزرسانی اپلیکیشن")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("دانلود نسخه جدید", (d, w) -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)));
                    } catch (Exception ignored) {
                        /* ignore */
                    }
                })
                .setNegativeButton("بررسی مجدد", (d, w) -> {
                    updateBlocked = false;
                    webLoaded = false;
                    checkForUpdate(true);
                })
                .show();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (updateBlocked) {
            return;
        }
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.destroy();
        }
        super.onDestroy();
    }
}
