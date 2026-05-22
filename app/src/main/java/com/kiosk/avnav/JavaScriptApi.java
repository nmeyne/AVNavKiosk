package com.kiosk.avnav;

import android.webkit.JavascriptInterface;

/*
 * Minimal AVNav / BonjourBrowser compatibility bridge.
 *
 * Exposes the subset of JavaScript APIs required
 * by AVNav Viewer for kiosk integration features
 * such as screen dimming.
 *
 * Only the minimal subset required for
 * kiosk functionality is implemented.
 *
 */
public class JavaScriptApi {

    private final MainActivity activity;

    public JavaScriptApi(MainActivity activity) {

        this.activity = activity;
    }

    @JavascriptInterface
    public boolean dimScreen(int percent) {

        if (percent < 0 || percent > 100) {
            return false;
        }

        activity.setBrightness(percent);

        return true;
    }

    @JavascriptInterface
    public String getVersion() {

        return "AVNavKiosk";
    }

    @JavascriptInterface
    public void applicationStarted() {

        // compatibility stub
    }

    @JavascriptInterface
    public void showSystemUi() {

        activity.showSystemUi();
    }

    @JavascriptInterface
    public void hideSystemUi() {

        activity.hideSystemUi();
    }
}
