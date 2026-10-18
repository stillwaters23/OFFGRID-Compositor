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

dest="$root/docs/bugs/screenshots/api-${api}"
mkdir -p "$dest"

anr_dump() {
  local file="$1"
  adb shell cat "$file" 2>/dev/null | grep -E "isn't responding|isn.t responding|Close app" >/dev/null
}

dump_ui() {
  local remote="$1"
  local n
  for n in 1 2 3; do
    if adb shell uiautomator dump "$remote" >/dev/null 2>&1; then
      return 0
    fi
    sleep 0.4
  done
  return 1
}

prove_ten_cold_starts() {
  local log="$dest/cold-starts.log"
  : > "$log"
  adb logcat -c >/dev/null 2>&1 || true
  adb shell pm grant com.offgrid.compositor android.permission.CAMERA >/dev/null 2>&1 || true
  local i started remote
  for i in $(seq 1 10); do
    adb shell am force-stop com.offgrid.compositor >/dev/null 2>&1 || true
    sleep 0.5
    started=$(adb shell am start -W -n com.offgrid.compositor/.MainActivity) || true
    {
      echo "cold_$i"
      echo "$started"
    } | tee -a "$log"
    sleep 1.2
    remote="/data/local/tmp/offgrid-cold-$i.xml"
    dump_ui "$remote" || true
    adb pull "$remote" "$dest/cold_$i.xml" >/dev/null 2>&1 || true
    if anr_dump "$remote"; then
      echo "ANR dialog on force-stop cold start $i/10" | tee -a "$log"
      adb logcat -d > "$dest/cold_$i.logcat.txt" || true
      return 1
    fi
    if adb logcat -d | grep -E "ANR in com.offgrid.compositor|Input dispatching timed out: .*com.offgrid.compositor"; then
      echo "ANR log on force-stop cold start $i/10" | tee -a "$log"
      adb logcat -d > "$dest/cold_$i.logcat.txt" || true
      return 1
    fi
    adb logcat -c >/dev/null 2>&1 || true
    if ! echo "$started" | grep -q "Status: ok"; then
      echo "force-stop cold start $i/10 did not report Status: ok" | tee -a "$log"
      return 1
    fi
    if echo "$started" | grep -q "LaunchState:" && ! echo "$started" | grep -q "LaunchState: COLD"; then
      echo "force-stop cold start $i/10 was not COLD" | tee -a "$log"
      echo "$started" | tee -a "$log"
      return 1
    fi
  done
  echo "cold_ok=10" | tee -a "$log"
  adb push "$log" /data/local/tmp/offgrid-cold-starts.log >/dev/null
  adb shell am force-stop com.offgrid.compositor >/dev/null 2>&1 || true
  return 0
}

set +e
(
  cd "04 Build"
  ./gradlew :app:installDebug --stacktrace
)
install_status=$?
set -e
if [ "$install_status" -ne 0 ]; then
  echo "Could not install the debug app for cold starts"
  exit "$install_status"
fi

set +e
prove_ten_cold_starts
cold_status=$?
set -e
if [ "$cold_status" -ne 0 ]; then
  echo "Ten force-stop cold starts showed an ANR or failed to start"
  adb shell mkdir -p /data/local/tmp/royalty-fix-evidence || true
  adb pull /data/local/tmp/royalty-fix-evidence/. "$dest" || true
  exit "$cold_status"
fi

adb shell am force-stop com.offgrid.compositor >/dev/null 2>&1 || true

set +e
(
  cd "04 Build"
  ./gradlew connectedDebugAndroidTest --stacktrace \
    -Pandroid.testInstrumentationRunnerArguments.class=com.offgrid.compositor.RoyaltyPhoneFixTest
)
status=$?
set -e

adb shell mkdir -p /data/local/tmp/royalty-fix-evidence || true
adb pull /data/local/tmp/royalty-fix-evidence/. "$dest" || true
echo "Pulled emulator screenshots into $dest"
ls -la "$dest" || true
exit "$status"
