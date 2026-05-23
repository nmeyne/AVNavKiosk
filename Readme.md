# AVNavKiosk

A lightweight fullscreen Android kiosk wrapper for AVNav, designed for older Android tablets and dedicated marine navigation displays. 'Kiosk' means 'switch on and go', with predefined configuration and network checks for users who are less familiar with Android and expect an appliance-style navigation display.

<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/5614c1bf-37c3-4d80-9042-af11e5778c58" />

<img width="1494" height="920" alt="image" src="https://github.com/user-attachments/assets/cd4ddaaf-2c7b-4c2f-b038-ed59c57e0680" />


AVNavKiosk provides:

- stable, more recent Chromium/WebView behaviour on older Android devices
- immersive fullscreen kiosk mode
- configurable startup splash screen and branding
- SSID-aware startup workflow
- simplified appliance-style UX for onboard navigation systems

The project was developed and tested primarily on:

- Sony Xperia Tablet Z (SGP311) - which is normally end of life on stock Android 5
- LineageOS 15.1
- Android 8.1
- AVNav server installations running on Raspberry Pi systems connected by WiFi

## AVNav Compatibility

AVNavKiosk implements a minimal subset of the historical BonjourBrowser / AVNav Android JavaScript bridge APIs required by AVNav Viewer.

Currently supported bridge functions include:

- screen dimming
- fullscreen control
- application lifecycle compatibility

Upload and download bridge functions are intentionally not implemented in kiosk mode.

---

# Design Goals

AVNavKiosk intentionally avoids:

- complex Android UI frameworks
- custom browser engines
- viewport manipulation hacks
- heavy kiosk-management frameworks

Instead it focuses on:

- reliability
- simplicity
- stable WebView behaviour - Chromium-based
- predictable fullscreen operation
- appliance-style startup UX
- economy and sustainability

The implementation keeps WebView configuration intentionally close to:

- [BonjourBrowser](https://github.com/wellenvogel/BonjourBrowser)
- [AVNav Android](https://github.com/wellenvogel/avnav)

with thanks to Andreas Vogel (@wellenvogel).


# Economy and Sustainability

With top of the market 10" display marine sailing chartplotters costing around £2500, an open-source system that costs a fraction of this makes sense, and gets a few more years use out of perfectly good hardware that would otherwise be in landfill.

---

# Features

## Fullscreen immersive mode

- hides Android status/navigation bars
- sticky immersive kiosk operation
- optional configuration control

## Stable WebView behaviour on older tablets

The project specifically addresses Chromium viewport problems found on some older Android tablets.

Important implementation details include:

- `targetSdkVersion=29`
- `setUseWideViewPort(true)`
- `setLoadWithOverviewMode(true)`

These avoid legacy mobile viewport scaling problems that can otherwise render AVNav incorrectly on older Sony Android devices.

## Splash screen and branding

Configurable:

- startup banner image
- startup text
- splash text size
- fullscreen splash dialog

The splash screen supports:

- tap-to-dismiss once AVNav is ready
- automatic dismissal after successful AVNav load
- startup status messaging

## WiFi-aware startup workflow

The kiosk can:

- detect WiFi enabled/disabled state
- verify connection to a configured SSID
- guide users to reconnect correctly
- prevent confusing blank-screen startup states

Typical startup flow:

```text
Tablet WiFi OFF
→ prompt user to enable WiFi

Wrong SSID
→ prompt user to connect to vessel network

Correct SSID
→ connect to AVNav server

AVNav loaded
→ dismiss splash
```

## Appliance-style operation

- uploads disabled
- downloads disabled
- optional keep-screen-on mode
- simplified navigation
- minimal Android exposure

---

# Configuration

Configuration file:

```text
/sdcard/avnavkiosk.conf
```

Example:

```ini
# AVNav server URL
serverUrl=http://192.168.30.10:8080

# Expected onboard WiFi SSID
wifiSSID=SeaScamp1

# Startup splash/banner image
bannerFileName=avnav-banner.png

# Splash text shown on startup
splashText=Welcome to Sea Scamp AVNav\n\nPlease check that the server is ready\n(red light on the instrument panel)

# Splash text size
splashTextSize=20

# Keep screen awake
keepScreenOn=true

# Hide Android UI
hideStatus=true
hideNavigation=true

# Lock orientation to portrait if true, landscape if false.
portraitLock=false

```

---

# Banner Images

Banner images are searched in:

```text
/sdcard/
```

Example:

```text
/sdcard/avnav-banner.png
```

Recommended:

- PNG format
- landscape or portrait aspect ratio if portraitLock=true has been set
- high contrast for daylight readability

---

# Android Permissions

The project requires:

```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
```

Note:

Android requires location permission in order to read connected WiFi SSIDs.

---

# Preparing the Tablet

More recent tablets may be able to sideload this APK directly even though it is built for Android 8.  Try to install, and set the kiosk app as the home app instead of the default app launcher on startup. (Set, and use developer options).

However, if this won't work for your device, and your OS is older, and you are happy to reconfigure and give them more life, Lineage OS seems a pretty good choice. See https://wiki.lineageos.org/devices/

There are ready-made builds still available for many older tablets, and the more recent chromium / webkits are more reliable for more recent releases of the AvNav viewer.  It is also  possible to make a Lineage OS build from scratch if you can't find one for your device.  See the wiki. There are step by step guides see https://wiki.lineageos.org/devices/pollux_windy/ for example.

## Compatibility

AVNavKiosk has been tested primarily on:

- Sony Xperia Tablet Z (SGP311)
- LineageOS 15.1
- Android 8.1

and also on a Samsung Galaxy 8 phone (G950F) (Android 9) - for dashboard display

The application should also work on many later Android phones and tablets using modern Chromium-based Android WebView implementations, including later Sony Xperia tablet models.

Important compatibility requirements:

- Android 8.0 or later recommended
- Chromium-based Android System WebView
- Runtime support for:
  - ACCESS_FINE_LOCATION
  - ACCESS_WIFI_STATE
  - READ_EXTERNAL_STORAGE

The application relies on modern Chromium WebView behaviour together with:

- targetSdkVersion=29
- wide viewport support
- immersive fullscreen APIs

Older Android WebView implementations may exhibit incorrect viewport scaling behaviour.

Android 10+ devices may require additional storage permission changes due to scoped storage restrictions.

## Installing

AVNavKiosk is currently distributed as a sideloaded APK and is not available on Google Play.

Developer Settings and USB debugging enabled - recommended

### Option 1 — Install via ADB (recommended)

Enable Developer Options:
```
Settings → About Tablet → tap Build Number 7 times
```
Enable USB debugging:
```
Settings → Developer Options → USB Debugging
```
Connect tablet via USB.

Verify device connection:
```
adb devices
```
Install APK:
```
adb install -r AVNavKiosk.apk
```

### Option 2 — Direct APK sideload

Copy APK to tablet storage.

Open the APK using a file manager.

Android may prompt to allow installation from unknown sources.

Enable:
```
Allow from this source
```
then continue installation.

### First Run Permissions

On first launch the application will request:

Location permission
(required for Wi-Fi SSID detection)
Storage permission
(required for splash banner and configuration file access)

Both permissions should be granted for normal operation.

### Configuration File

Create:
```
/sdcard/avnavkiosk.conf
```
Example:
```
serverUrl=http://192.168.30.10:8080
wifiSSID=SeaScamp1
bannerFileName=avnav-banner.png
```
Optional banner image:
```
/sdcard/avnav-banner.png
```
### Recommended Setup

For dedicated onboard kiosk installations:

disable screen timeout
disable automatic app updates
disable battery optimisation for AVNavKiosk
keep Android System WebView updated
optionally remove/disable unnecessary Android apps

LineageOS-based installations generally provide the cleanest kiosk experience.

### Using AVNavKiosk as the Home / Launcher App

For dedicated navigation tablets, AVNavKiosk can be configured as the default Android Home application (launcher).

This allows the tablet to boot directly into the AVNav kiosk interface.

**Set AVNavKiosk as Home App**

Press the Android Home button after installation.

Android should prompt:
```
Complete action using
```
Select:
```
AVNavKiosk
```
then choose:
```
Always
```

**If Android Does Not Prompt**

Open:
```
Settings → Apps → Default Apps → Home App
```
or on some LineageOS versions:
```
Settings → Apps → Gear Icon → Home App
```
then select:
```
AVNavKiosk
```
**Returning to the Standard Launcher**

If required:
```
Settings → Apps → Default Apps → Home App
```
and reselect the normal launcher.

---

# Building

Typical Android Studio / Gradle build:

```bash
./gradlew assembleDebug
```

or:

```bash
./build.sh
```

depending on local project setup.  I used a bash script (provided here as an example).  However, this project was more complicated than expected, and in retrospect a proper build system would have saved time and effort.

---

# Installing

Best to enable developer settings on the tablet and allow debug access over USB.  I used a linux desktop machine with adb

Example:

```bash
adb install app-debug.apk
```

Verify target SDK:

```bash
adb shell dumpsys package com.kiosk.avnav | grep targetSdk
```

Expected:

```text
targetSdk=29
```

---

# Recommended Hardware

Tested successfully on:

- Sony Xperia Tablet Z (SGP311)
- LineageOS 15.1
- Raspberry Pi AVNav servers

The project is particularly useful for:

- repurposed older Android tablets
- dedicated helm displays (an old phone?)
- marine chartplotter replacements (build the screen into your nav station instrument panel)

---

# Known Limitations

- Upload/import operations are intentionally disabled
- Download operations are intentionally disabled
- Orientation is intentionally locked from start up - choose appropriate
  text size and splash banners for the orientation you select
- AVNav HTTP error pages may still require additional handling

## Security Model

AVNavKiosk is designed primarily for dedicated onboard navigation displays operating on trusted local vessel networks.

The application assumes that the connected AVNav server and AVNav Viewer web application are trusted components. As a result, the Android JavaScript bridge implementation intentionally prioritises simplicity, stability, and low overhead over heavy sandboxing or defensive validation layers.

AVNavKiosk is therefore not intended to function as a hardened general-purpose Android browser or internet-facing kiosk platform.

---

# Future Ideas

Possible future improvements:

- launcher replacement mode - other nav apps or tide tables?
- dedicated settings activity

---

# Credits

This project was heavily inspired by:

- [BonjourBrowser](https://github.com/wellenvogel/BonjourBrowser)
- [AVNav Android](https://github.com/wellenvogel/avnav)

Many thanks to Andreas Vogel (@wellenvogel) for the original projects, code and design ideas that informed this implementation and also credit to ChatGPT which (mostly) made this easier for me!
