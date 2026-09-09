package com.schoolmanager.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
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
    private static final int NOTIFICATION_PERMISSION_CODE = 4101;
    private static final String CHANNEL_ID = "school_manager_alerts";

    private WebView webView;
    private boolean updateBlocked = false;
    private boolean webLoaded = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int notificationSeq = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor("#2563eb"));
        }

        createNotificationChannel();
        requestNotificationPermission();

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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE && webView != null) {
            webView.reload();
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE
                );
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("اعلان‌ها و پیام‌های سامانه");
        channel.enableVibration(true);
        channel.enableLights(true);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private void showNativeNotification(String title, String body, String url) {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }

        String safeTitle = title == null || title.trim().isEmpty() ? getString(R.string.app_name) : title.trim();
        String safeBody = body == null ? "" : body.trim();
        String targetUrl = url == null || url.trim().isEmpty()
                ? normalizeStartUrl(getString(R.string.app_start_url))
                : url.trim();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(targetUrl));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int id = ++notificationSeq;
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, id, intent, flags);

        android.app.Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new android.app.Notification.Builder(this, CHANNEL_ID)
                : new android.app.Notification.Builder(this);

        builder.setContentTitle(safeTitle)
                .setContentText(safeBody)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(android.app.Notification.PRIORITY_HIGH)
                .setCategory(android.app.Notification.CATEGORY_MESSAGE);

        if (safeBody.isEmpty()) {
            builder.setContentText("پیام جدید");
        }

        manager.notify(id, builder.build());
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

        webView.addJavascriptInterface(new NativeBridge(), "SchoolManagerNative");

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    return;
                }
                runOnUiThread(() -> {
                    try {
                        request.grant(request.getResources());
                    } catch (Exception ignored) {
                        request.deny();
                    }
                });
            }
        });
    }

    private final class NativeBridge {
        @JavascriptInterface
        public void showNotification(String title, String body, String url) {
            mainHandler.post(() -> showNativeNotification(title, body, url));
        }

        @JavascriptInterface
        public boolean notificationsAllowed() {
            if (Build.VERSION.SDK_INT >= 33) {
                return checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED;
            }
            return true;
        }
    }

    private void loadAppUrl() {
        if (webLoaded || updateBlocked) {
            return;
        }
        webLoaded = true;
        webView.loadUrl(normalizeStartUrl(getString(R.string.app_start_url)));
    }

    private String normalizeStartUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "about:blank";
        }
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed : trimmed + "/";
    }

    private String joinApiPath(String path) {
        String base = normalizeStartUrl(getString(R.string.app_start_url));
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return base + path;
    }

    private void checkForUpdate(boolean initial) {
        new Thread(() -> {
            try {
                int versionCode = BuildConfig.VERSION_CODE;
                String brandingHash = getString(R.string.app_branding_hash);
                String checkUrl = joinApiPath("api/pwa/app-update")
                        + "?version_code=" + versionCode
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
                String downloadUrl = json.optString("download_url", joinApiPath("app/apk"));

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
