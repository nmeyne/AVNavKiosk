package com.kiosk.avnav;

/*
 * Lightweight external configuration loader
 * for AVNavKiosk.
 *
 * AVNavKiosk intentionally keeps configuration minimal to preserve stable
 * WebView behaviour on older Android tablets.
 */

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class Config {

    private static final String TAG = "AVNavKiosk.Config";

    private static final String CONFIG_FILE_NAME = "avnavkiosk.conf";

    private final Map<String,String> values;

    private Config(Map<String,String> values) {
        this.values = values;
    }

    public String getServerUrl() {

        String url =
                get(
                        "serverUrl",
                        "http://192.168.30.10:8080"
                );

        if (!url.startsWith("http")) {
            url = "http://" + url;
        }

        return url;
    }

    public String getWifiSSID() {

        return get(
                "wifiSSID",
                ""
        );
    }

    public String getBannerFileName() {

        return get(
                "bannerFileName",
                "avnav-banner.png"
        );
    }

    public String getSplashText() {

        return get(
                "splashText",
                "AVNav Kiosk"
        );
    }

    public int getSplashTextSize() {

        return getInt(
                "splashTextSize",
                18
        );
    }

    public boolean isKeepScreenOn() {

        return getBool(
                "keepScreenOn",
                true
        );
    }

    public boolean isHideStatus() {

        return getBool(
                "hideStatus",
                true
        );
    }

    public boolean isHideNavigation() {

        return getBool(
                "hideNavigation",
                true
        );
    }

    public boolean isPortraitLock() {

        return getBool(
                "portraitLock",
                false
        );
    }

    public static Config load(Context context) {

        Map<String,String> map = new HashMap<>();
        File file = findConfigFile(context);

        if (file != null) {
            loadFile(file, map);
        }

        return new Config(map);
    }

    private static void loadFile(
            File file,
            Map<String,String> map
    ) {

        try (
                BufferedReader r =
                        new BufferedReader(
                                new InputStreamReader(
                                        new FileInputStream(file)
                                )
                        )
        ) {

            String line;

            while ((line = r.readLine()) != null) {
                readConfigLine(line, map);
            }
        }
        catch (Exception e) {
            Log.w(TAG, "Config load failed", e);
        }
    }

    private static void readConfigLine(
            String line,
            Map<String,String> map
    ) {

        line = line.trim();

        if (
                line.isEmpty()
                || line.startsWith("#")
        ) {
            return;
        }

        int i = line.indexOf('=');

        if (i < 0) {
            return;
        }

        map.put(
                line.substring(0, i).trim(),
                line.substring(i + 1).trim()
        );
    }

    private String get(
            String key,
            String def
    ) {

        String v = values.get(key);

        if (
                v == null
                || v.isEmpty()
        ) {
            return def;
        }

        return v.replace("\\n", "\n");
    }

    private int getInt(
            String key,
            int def
    ) {

        try {
            return Integer.parseInt(
                    get(
                            key,
                            String.valueOf(def)
                    )
            );
        }
        catch (Exception e) {
            return def;
        }
    }

    private boolean getBool(
            String key,
            boolean def
    ) {

        String v =
                get(
                        key,
                        String.valueOf(def)
                );

        return
                v.equalsIgnoreCase("true")
                        || v.equals("1")
                        || v.equalsIgnoreCase("yes");
    }

    /*
     * Config search locations:
     *
     * /sdcard/avnavkiosk.conf
     * app external files dir
     */
    private static File findConfigFile(Context context) {

        File external =
                new File(
                        Environment.getExternalStorageDirectory(),
                        CONFIG_FILE_NAME
                );

        if (external.exists()) {
            return external;
        }

        File appFile =
                new File(
                        context.getExternalFilesDir(null),
                        CONFIG_FILE_NAME
                );

        if (appFile.exists()) {
            return appFile;
        }

        return null;
    }
}
