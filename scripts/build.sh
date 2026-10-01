#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "$SDK_ROOT" ]]; then
  echo 'Set ANDROID_SDK_ROOT or ANDROID_HOME to your Android SDK directory.' >&2
  exit 1
fi
SDK_PLATFORM="${SDK_PLATFORM:-36}"
BUILD_TOOLS_VERSION="${BUILD_TOOLS_VERSION:-36.1.0}"
TOOLS="$SDK_ROOT/build-tools/$BUILD_TOOLS_VERSION"
ANDROID_JAR="$SDK_ROOT/platforms/android-$SDK_PLATFORM/android.jar"
if [[ -n "${JAVA_HOME:-}" ]]; then export PATH="$JAVA_HOME/bin:$PATH"; fi
for tool in java javac jar keytool sha256sum; do command -v "$tool" >/dev/null; done
for tool in aapt d8 zipalign apksigner; do
  [[ -x "$TOOLS/$tool" ]] || { echo "Missing $TOOLS/$tool; install build-tools;$BUILD_TOOLS_VERSION" >&2; exit 1; }
done
[[ -f "$ANDROID_JAR" ]] || { echo "Missing $ANDROID_JAR; install platforms;android-$SDK_PLATFORM" >&2; exit 1; }

mkdir -p build/classes build/dex dist
"$TOOLS/aapt" package -f -m -J build -M app/AndroidManifest.xml -S app/res -I "$ANDROID_JAR" -F build/unsigned.apk
"$TOOLS/apksigner" version >/dev/null
javac -source 8 -target 8 -classpath "$ANDROID_JAR" -d build/classes app/src/local/zipshortcut/*.java
jar cf build/classes.jar -C build/classes .
"$TOOLS/d8" --lib "$ANDROID_JAR" --min-api 26 --output build/dex build/classes.jar
cp build/dex/classes.dex build/classes.dex
(cd build && "$TOOLS/aapt" add unsigned.apk classes.dex)
"$TOOLS/zipalign" -f 4 build/unsigned.apk build/aligned.apk

if [[ -z "${KEYSTORE_PATH:-}" ]]; then
  if [[ "${RELEASE_BUILD:-false}" == true ]]; then
    echo 'Release builds require KEYSTORE_PATH and signing passwords.' >&2
    exit 1
  fi
  KEYSTORE_PATH="$PWD/build/debug.keystore"
  KEYSTORE_PASSWORD=android
  KEY_PASSWORD=android
  KEY_ALIAS=androiddebugkey
  if [[ ! -f "$KEYSTORE_PATH" ]]; then
    keytool -genkeypair -keystore "$KEYSTORE_PATH" -storepass "$KEYSTORE_PASSWORD" -keypass "$KEY_PASSWORD" \
      -alias "$KEY_ALIAS" -dname 'CN=Android Debug,O=Android,C=US' -keyalg RSA -validity 3650 >/dev/null 2>&1
  fi
  echo 'Building a development APK. Official releases use a different signing key.'
else
  : "${KEYSTORE_PASSWORD:?Set KEYSTORE_PASSWORD}"
  KEY_PASSWORD="${KEY_PASSWORD:-$KEYSTORE_PASSWORD}"
  KEY_ALIAS="${KEY_ALIAS:-local}"
fi
export KEYSTORE_PASSWORD KEY_PASSWORD
"$TOOLS/apksigner" sign --ks "$KEYSTORE_PATH" --ks-pass env:KEYSTORE_PASSWORD --key-pass env:KEY_PASSWORD \
  --ks-key-alias "$KEY_ALIAS" --out dist/talk-to-dot.apk build/aligned.apk
"$TOOLS/apksigner" verify --verbose dist/talk-to-dot.apk
(cd dist && sha256sum talk-to-dot.apk > SHA256SUMS.txt)
echo 'Built dist/talk-to-dot.apk and dist/SHA256SUMS.txt'
