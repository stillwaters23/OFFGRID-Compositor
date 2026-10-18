#!/usr/bin/env bash
# Compile a one-activity APK used only as a cold-start baseline. Not the app.
set -euo pipefail
root="$(cd "$(dirname "$0")" && pwd)"
sdk="${ANDROID_HOME:?ANDROID_HOME is required}"
jar="$sdk/platforms/android-36/android.jar"
bt="$sdk/build-tools/36.0.0"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

mkdir -p "$work/classes" "$work/dex"
javac --release 17 -classpath "$jar" -d "$work/classes" \
  "$root/empty/src/com/qa/empty/EmptyActivity.java"
"$bt/d8" --lib "$jar" --output "$work/dex" "$work/classes/com/qa/empty/EmptyActivity.class"
"$bt/aapt2" link -o "$work/unsigned.apk" \
  -I "$jar" \
  --manifest "$root/empty/AndroidManifest.xml" \
  --min-sdk-version 33 \
  --target-sdk-version 35 \
  --version-code 1 \
  --version-name 1
(cd "$work/dex" && zip -q -j "$work/unsigned.apk" classes.dex)
"$bt/zipalign" -f 4 "$work/unsigned.apk" "$work/aligned.apk"
keytool -genkeypair -keystore "$work/debug.keystore" -storepass android \
  -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=Independent QA"
"$bt/apksigner" sign --ks "$work/debug.keystore" --ks-pass pass:android \
  --key-pass pass:android --out "$root/empty/empty-debug.apk" "$work/aligned.apk"
test -s "$root/empty/empty-debug.apk"
echo "empty apk $(wc -c <"$root/empty/empty-debug.apk") bytes"
