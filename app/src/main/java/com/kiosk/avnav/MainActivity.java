package com.kiosk.avnav;

/*
 * AVNavKiosk
 *
 * Simplified kiosk WebView wrapper for AVNav.
 * Nick Meyne, @nmeyne 2026
 *
 * WebView configuration and fullscreen handling are based closely on:
 *
 * BonjourBrowser
 * https://github.com/wellenvogel/BonjourBrowser
 *
 * and:
 *
 * AVNav Android app
 * https://github.com/wellenvogel/avnav
 *
 * Copyright Andreas Vogel (@wellenvogel)
 *
 * Compatibility note:
 *
 * targetSdkVersion metadata is essential for correct Chromium viewport
 * behaviour on older Android tablets such as the Sony SGP311.
 */

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebChromeClient.FileChooserParams;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends Activity {

    private static final String TAG = "AVNavKiosk";

    private static final String MSG_UPLOADS_DISABLED =
            "File uploads/imports are disabled in kiosk mode";

    private static final String MSG_DOWNLOADS_DISABLED =
            "Downloads are disabled in kiosk mode";

    private WebView webView;

    private Config config;

    private Dialog splashDialog;

    private TextView splashStatusView;

    private boolean avnavReady = false;

    private boolean pageLoadFailed = false;

    private boolean splashDismissed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        requestRuntimePermissions();

        config = Config.load(this);

        applyOrientationLock();

        setContentView(R.layout.activity_main);

        webView = (WebView)findViewById(R.id.webview);

        configureWebView();

        applyWindowConfig();
        showSplashDialog();
        loadWebApp();
    }

    private void requestRuntimePermissions() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        requestPermissions(
                new String[] {
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION
                },
                1
        );
    }

    /*
     * Keep this configuration close to BonjourBrowser / AVNav Android. The
     * wide viewport settings are especially important for old tablet WebViews.
     */
    @SuppressWarnings("deprecation")
    private void configureWebView() {

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setTextZoom(100);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        try {
            settings.setAllowUniversalAccessFromFileURLs(true);
        }
        catch (Exception e) {
            Log.w(TAG, "Universal file access unavailable");
        }

        // Intentional AVNav compatibility API name.
        webView.addJavascriptInterface(
                new JavaScriptApi(this),
                "bonjourBrowser"
        );

        webView.setWebChromeClient(
                new WebChromeClient() {

                    @Override
                    public boolean onShowFileChooser(
                            WebView webView,
                            android.webkit.ValueCallback<Uri[]> filePathCallback,
                            FileChooserParams fileChooserParams
                    ) {

                        Toast.makeText(
                                MainActivity.this,
                                MSG_UPLOADS_DISABLED,
                                Toast.LENGTH_LONG
                        ).show();

                        filePathCallback.onReceiveValue(null);

                        return true;
                    }
                }
        );

        webView.setWebViewClient(
                new WebViewClient() {

                    @Override
                    public void onPageFinished(
                            WebView view,
                            String url
                    ) {

                        super.onPageFinished(view, url);

                        if (!pageLoadFailed) {
                            avnavReady = true;
                            dismissSplashIfReady();
                        }
                    }

                    @Override
                    public void onReceivedError(
                            WebView view,
                            int errorCode,
                            String description,
                            String failingUrl
                    ) {

                        super.onReceivedError(
                                view,
                                errorCode,
                                description,
                                failingUrl
                        );

                        Log.w(TAG, "AVNav load failed: " + description);

                        pageLoadFailed = true;

                        updateSplashStatus("Connecting to AVNav server...");

                        retryLoadLater();
                    }
                }
        );

        webView.setDownloadListener(
                new android.webkit.DownloadListener() {

                    @Override
                    public void onDownloadStart(
                            String url,
                            String userAgent,
                            String contentDisposition,
                            String mimetype,
                            long contentLength
                    ) {

                        Toast.makeText(
                                MainActivity.this,
                                MSG_DOWNLOADS_DISABLED,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    /*
     * Splash is deliberately implemented as a Dialog rather than as part of the
     * WebView layout tree. This keeps old WebView viewport behaviour stable.
     */
    private void showSplashDialog() {

        splashDialog = new Dialog(this);
        splashDialog.setContentView(R.layout.dialog_splash);
        splashDialog.setCancelable(false);

        ImageView bannerView =
                (ImageView)splashDialog.findViewById(R.id.splashBanner);

        TextView splashText =
                (TextView)splashDialog.findViewById(R.id.splashText);

        splashStatusView =
                (TextView)splashDialog.findViewById(R.id.splashStatus);

        splashText.setText(config.getSplashText());
        splashText.setTextSize(config.getSplashTextSize());

        Bitmap banner = loadBannerImage();

        if (banner != null) {
            bannerView.setImageBitmap(banner);
            bannerView.setVisibility(View.VISIBLE);
        }

        View root = splashDialog.findViewById(android.R.id.content);

        if (root != null) {
            root.setOnClickListener(
                    new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            dismissSplashIfReady();
                        }
                    }
            );
        }

        splashDialog.show();

        if (splashDialog.getWindow() != null) {
            splashDialog.getWindow().setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
            );
        }
    }

    private void updateSplashStatus(String text) {

        if (splashStatusView != null) {
            splashStatusView.setText(text);
        }
    }

    private void dismissSplashIfReady() {

        if (!avnavReady) {
            return;
        }

        if (splashDismissed) {
            return;
        }

        splashDismissed = true;

        dismissSplashDialog();
    }

    private void dismissSplashDialog() {

        if (
                splashDialog != null
                && splashDialog.isShowing()
        ) {
            splashDialog.dismiss();
        }
    }

    private Bitmap loadBannerImage() {

        File external =
                new File(
                        "/sdcard",
                        config.getBannerFileName()
                );

        if (external.exists()) {
            return BitmapFactory.decodeFile(external.getAbsolutePath());
        }

        File appFile =
                new File(
                        getExternalFilesDir(null),
                        config.getBannerFileName()
                );

        if (appFile.exists()) {
            return BitmapFactory.decodeFile(appFile.getAbsolutePath());
        }

        return null;
    }

    private boolean isWifiEnabled() {

        try {
            WifiManager wm =
                    (WifiManager)
                            getApplicationContext()
                                    .getSystemService(WIFI_SERVICE);

            return wm != null && wm.isWifiEnabled();
        }
        catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    private String getConnectedSSID() {

        try {
            WifiManager wm =
                    (WifiManager)
                            getApplicationContext()
                                    .getSystemService(WIFI_SERVICE);

            if (wm == null) {
                return null;
            }

            WifiInfo info = wm.getConnectionInfo();

            if (info == null) {
                return null;
            }

            String ssid = info.getSSID();

            if (ssid == null) {
                return null;
            }

            ssid = ssid.replace("\"", "");

            if (
                    ssid.equals("<unknown ssid>")
                    || ssid.isEmpty()
            ) {
                return null;
            }

            return ssid;
        }
        catch (Exception e) {
            return null;
        }
    }

    private void retryLoadLater() {

        webView.postDelayed(
                new Runnable() {

                    @Override
                    public void run() {
                        loadWebApp();
                    }
                },
                5000
        );
    }

    private void loadWebApp() {

        String expectedSSID = config.getWifiSSID();
        String currentSSID = getConnectedSSID();

        if (!isWifiEnabled()) {

            updateSplashStatus(
                    "Tablet Wi-Fi is OFF.\n"
                            + "Please enable Wi-Fi in Android Settings."
            );

            retryLoadLater();

            return;
        }

        if (currentSSID == null) {

            updateSplashStatus(
                    "Waiting for "
                            + expectedSSID
                            + " Wi-Fi..."
            );

            retryLoadLater();

            return;
        }

        if (
                !expectedSSID.isEmpty()
                && !expectedSSID.equals(currentSSID)
        ) {

            updateSplashStatus(
                    "Please connect only to:\n"
                            + expectedSSID
                            + "\n\nCurrent Wi-Fi:\n"
                            + currentSSID
            );

            retryLoadLater();

            return;
        }

        updateSplashStatus("Connecting to AVNav server...");

        String url = config.getServerUrl();

        pageLoadFailed = false;

        webView.loadUrl(url);
    }

    private void applyWindowConfig() {

        if (config.isKeepScreenOn()) {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            );
        }

        hideSystemUi();
    }

    private void applyOrientationLock() {

        if (config.isPortraitLock()) {
            setRequestedOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            );
        }
        else {
            setRequestedOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            );
        }
    }

    @Override
    protected void onResume() {

        super.onResume();

        if (webView != null) {
            webView.onResume();
        }

        hideSystemUi();
    }

    @Override
    protected void onPause() {

        super.onPause();

        if (webView != null) {
            webView.onPause();
        }
    }

    /*
     * Reapply immersive mode after focus changes.
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {

        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            hideSystemUi();
        }
    }

    public void hideSystemUi() {

        int flags =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        if (config.isHideNavigation()) {
            flags |=
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }

        if (config.isHideStatus()) {
            flags |=
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_FULLSCREEN;
        }

        getWindow()
                .getDecorView()
                .setSystemUiVisibility(flags);
    }

    public void showSystemUi() {

        getWindow()
                .getDecorView()
                .setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_VISIBLE
                );
    }

    /*
     * JavaScript bridge brightness control.
     */
    public void setBrightness(int percent) {

        float brightness;

        if (percent >= 100) {
            brightness =
                    WindowManager.LayoutParams
                            .BRIGHTNESS_OVERRIDE_NONE;
        }
        else {
            brightness =
                    Math.max(
                            0.01f,
                            Math.min(
                                    1f,
                                    percent / 100f
                            )
                    );
        }

        WindowManager.LayoutParams lp = getWindow().getAttributes();

        lp.screenBrightness = brightness;

        getWindow().setAttributes(lp);
    }

    @Override
    public void onBackPressed() {

        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {

        if (webView != null) {
            webView.clearCache(false);
            webView.destroy();
        }

        super.onDestroy();
    }
}
