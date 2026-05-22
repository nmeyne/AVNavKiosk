#!/usr/bin/env bash
set -euo pipefail

APP_NAME="AVNavKiosk"
PKG="com.kiosk.avnav"
SDK="${ANDROID_HOME:-$HOME/Android}"
BT="$SDK/build-tools/35.0.0"
PLATFORM="$SDK/platforms/android-26/android.jar"
KEYSTORE="kiosk.keystore"
ALIAS="kiosk"

export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$SDK/platform-tools:$BT:$SDK/cmdline-tools/latest/bin:$PATH"

echo "== Clean =="
rm -rf build
mkdir -p build/obj build/dex

echo "== Generate R.java =="
mkdir -p build/gen

aapt package -f -m \
  -J build/gen \
  -M app/src/main/AndroidManifest.xml \
  -S app/src/main/res \
  -I "$PLATFORM"

echo "== Compile Java =="

javac -source 1.8 -target 1.8 \
  -bootclasspath "$PLATFORM" \
  -classpath "$PLATFORM" \
  -d build/obj \
  $(find app/src/main/java build/gen -name "*.java")
echo "== DEX =="

d8 \
  --lib "$PLATFORM" \
  --output build/dex \
  $(find build/obj -name "*.class")

echo "== Package APK =="
aapt package -f \
  -M app/src/main/AndroidManifest.xml \
  -S app/src/main/res \
  -I "$PLATFORM" \
  -F build/${APP_NAME}-unsigned.apk

cp build/dex/classes.dex build/classes.dex

cd build
zip -q -u ${APP_NAME}-unsigned.apk classes.dex
cd ..

if [ ! -f "$KEYSTORE" ]; then
  echo "== Create signing key =="
  keytool -genkeypair \
    -v \
    -keystore "$KEYSTORE" \
    -alias "$ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000
fi

echo "== Sign APK =="
apksigner sign \
  --ks "$KEYSTORE" \
  --out build/${APP_NAME}.apk \
  build/${APP_NAME}-unsigned.apk

echo "== Verify APK =="
apksigner verify --verbose build/${APP_NAME}.apk

echo "Built: build/${APP_NAME}.apk"
