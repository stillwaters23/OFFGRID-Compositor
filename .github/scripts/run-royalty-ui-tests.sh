#!/usr/bin/env bash
# One command for android-emulator-runner. That action runs /usr/bin/sh -c on
# each script line in a new process, so "cd" and "set -o pipefail" cannot live
# in the workflow YAML.
set -u
api="${1:?api level}"
root="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$root"
chmod +x "04 Build/gradlew"
adb shell mkdir -p /data/local/tmp/royalty-fix-evidence
adb shell chmod 777 /data/local/tmp/royalty-fix-evidence
# Match independent QA: 1080x2400 phone, animations on, immersive hint armed, cold start.
adb shell settings delete secure immersive_mode_confirmations >/dev/null 2>&1 || true
adb shell settings put global window_animation_scale 1
adb shell settings put global transition_animation_scale 1
adb shell settings put global animator_duration_scale 1
adb shell wm size 1080x2400 || true
adb shell wm density 420 || true
adb shell am force-stop com.offgrid.compositor >/dev/null 2>&1 || true

set +e
(
  cd "04 Build"
  ./gradlew connectedDebugAndroidTest --stacktrace \
    -Pandroid.testInstrumentationRunnerArguments.class=com.offgrid.compositor.RoyaltyPhoneFixTest
)
status=$?
set -e

dest="$root/docs/bugs/screenshots/api-${api}"
mkdir -p "$dest"
adb shell mkdir -p /data/local/tmp/royalty-fix-evidence || true
adb pull /data/local/tmp/royalty-fix-evidence/. "$dest" || true
echo "Pulled emulator screenshots into $dest"
ls -la "$dest" || true
exit "$status"
