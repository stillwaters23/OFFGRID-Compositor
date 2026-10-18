#!/usr/bin/env bash
# Runs on the booted API 35 emulator. The walk is this branch's adb script.
# The instrument suite is the fixer's RoyaltyPhoneFixTest, run once after the walk.
set -u
root="${GITHUB_WORKSPACE:-$(pwd)}"
ev="$root/docs/qa/independent_qa/evidence"
mkdir -p "$ev/cold" "$ev/steps" "$ev/dumps" "$ev/video" "$ev/instrument" "$ev/anr"
rm -f "$ev/recording.stop"

echo "NOTE (non-blocking, traced on the unfit VM): startup main-thread work before first frame included AppCompatTextView class verification (art::verifier::MethodVerifier::VerifyMethod inside AppCompatTextView.<init>) and ResourcesCompat.loadFont / TypefaceCompat.createFromResourcesFamilyXml inside AppCompatActivity.setContentView."

adb wait-for-device
adb shell 'while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 1; done'
adb shell settings delete secure immersive_mode_confirmations >/dev/null 2>&1 || true
adb shell settings put global window_animation_scale 1
adb shell settings put global transition_animation_scale 1
adb shell settings put global animator_duration_scale 1
adb shell wm size 1080x2400 || true
adb shell wm density 420 || true

{
  echo "=== device ==="
  adb shell getprop ro.build.version.sdk
  adb shell getprop ro.build.version.release
  adb shell getprop ro.product.model
  adb shell getprop ro.kernel.qemu
  adb shell wm size
  adb shell wm density
  echo "window_animation=$(adb shell settings get global window_animation_scale)"
  echo "transition_animation=$(adb shell settings get global transition_animation_scale)"
  echo "animator_duration=$(adb shell settings get global animator_duration_scale)"
  echo "immersive_mode_confirmations=$(adb shell settings get secure immersive_mode_confirmations)"
  echo "nproc=$(adb shell nproc)"
  adb shell cat /proc/meminfo | head -n 3
  adb shell dumpsys SurfaceFlinger | grep -E 'GLES|OpenGL|renderer' | head -n 20 || true
} | tee "$ev/device.txt"

app_apk="$root/offgrid-head/04 Build/app/build/outputs/apk/debug/app-debug.apk"
empty_apk="$root/docs/qa/independent_qa/empty/empty-debug.apk"
test -s "$app_apk"
test -s "$empty_apk"
adb install -r -d "$empty_apk"
adb install -r -d "$app_apk"
adb shell pm grant com.offgrid.compositor android.permission.CAMERA >/dev/null 2>&1 || true

(
  i=1
  while [ ! -f "$ev/recording.stop" ] && [ "$i" -le 24 ]; do
    adb shell screenrecord --time-limit 180 --bit-rate 4000000 "/sdcard/qa_r${i}.mp4" || true
    adb pull "/sdcard/qa_r${i}.mp4" "$ev/video/qa_r${i}.mp4" || true
    adb shell rm -f "/sdcard/qa_r${i}.mp4" || true
    i=$((i + 1))
  done
) &
rec_pid=$!

set +e
python3 "$root/docs/qa/independent_qa/walk.py"
walk_status=$?

(
  cd "$root/offgrid-head/04 Build"
  ./gradlew :app:connectedDebugAndroidTest --stacktrace \
    -Pandroid.testInstrumentationRunnerArguments.class=com.offgrid.compositor.RoyaltyPhoneFixTest
)
instrument_status=$?
set -e

adb logcat -d -v threadtime >"$ev/logcat.txt" || true
adb shell ls /data/anr >"$ev/anr/list.txt" 2>/dev/null || true
adb pull /data/anr "$ev/anr" >/dev/null 2>&1 || true
mkdir -p "$ev/instrument"
if [ -d "$root/offgrid-head/04 Build/app/build/reports/androidTests" ]; then
  cp -a "$root/offgrid-head/04 Build/app/build/reports/androidTests/." "$ev/instrument/reports/" || true
fi
if [ -d "$root/offgrid-head/04 Build/app/build/outputs/androidTest-results" ]; then
  cp -a "$root/offgrid-head/04 Build/app/build/outputs/androidTest-results/." "$ev/instrument/results/" || true
fi
printf 'walk_exit=%s\ninstrument_exit=%s\n' "$walk_status" "$instrument_status" | tee "$ev/exit_codes.txt"

touch "$ev/recording.stop"
adb shell 'kill -2 $(pidof screenrecord) 2>/dev/null || true'
wait "$rec_pid" || true
# The loop pulls each finished file. Pull a last clip if kill left one behind.
adb pull /sdcard/qa_r1.mp4 "$ev/video/qa_r_tail.mp4" >/dev/null 2>&1 || true

if [ "$walk_status" -ne 0 ] || [ "$instrument_status" -ne 0 ]; then
  echo "Independent QA finished with walk=$walk_status instrument=$instrument_status"
  exit 1
fi
echo "Independent QA walk and instrument suite both exited 0"
