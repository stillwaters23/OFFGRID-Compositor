#!/usr/bin/env bash
# Runs on the booted API 35 emulator. The walk is this branch's adb script.
# The instrument suite is the fixer's RoyaltyPhoneFixTest, run once after the walk.
# Every adb command has its own timeout. Progress lines name the command in flight.
set -u
root="${GITHUB_WORKSPACE:-$(pwd)}"
ev="$root/docs/qa/independent_qa/evidence"
mkdir -p "$ev/cold" "$ev/steps" "$ev/dumps" "$ev/video" "$ev/instrument" "$ev/anr"
rm -f "$ev/recording.stop"
: >"$ev/progress.log"

progress() {
  local line="PROGRESS $(date -u +%H:%M:%S) $*"
  echo "$line" | tee -a "$ev/progress.log" >&2
}

# seconds, then adb args. Exit 124/137 is a timeout.
adb_t() {
  local seconds="$1"
  shift
  progress "adb(${seconds}s) $*"
  timeout --signal=KILL "$seconds" adb "$@"
  local code=$?
  if [ "$code" -eq 124 ] || [ "$code" -eq 137 ]; then
    progress "TIMEOUT(${seconds}s) adb $*"
  else
    progress "adb-exit ${code} $*"
  fi
  return "$code"
}

progress "script start"
echo "NOTE (non-blocking, traced on the unfit VM): startup main-thread work before first frame included AppCompatTextView class verification (art::verifier::MethodVerifier::VerifyMethod inside AppCompatTextView.<init>) and ResourcesCompat.loadFont / TypefaceCompat.createFromResourcesFamilyXml inside AppCompatActivity.setContentView."

progress "wait-for-device cap 120s"
if ! adb_t 120 wait-for-device; then
  progress "device did not appear"
  exit 1
fi

progress "boot-complete wait cap 180s"
boot_deadline=$((SECONDS + 180))
booted=0
while [ "$SECONDS" -lt "$boot_deadline" ]; do
  prop="$(adb_t 20 shell getprop sys.boot_completed | tr -d '\r')"
  progress "sys.boot_completed=${prop:-empty}"
  if [ "$prop" = "1" ]; then
    booted=1
    break
  fi
  sleep 3
done
if [ "$booted" -ne 1 ]; then
  progress "boot_completed did not become 1 within 180s"
  exit 1
fi
progress "boot_completed=1"

adb_t 30 shell settings delete secure immersive_mode_confirmations || true
adb_t 30 shell settings put global window_animation_scale 1 || true
adb_t 30 shell settings put global transition_animation_scale 1 || true
adb_t 30 shell settings put global animator_duration_scale 1 || true
adb_t 30 shell wm size 1080x2400 || true
adb_t 30 shell wm density 420 || true

{
  echo "=== device ==="
  adb_t 20 shell getprop ro.build.version.sdk
  adb_t 20 shell getprop ro.build.version.release
  adb_t 20 shell getprop ro.product.model
  adb_t 20 shell getprop ro.kernel.qemu
  adb_t 20 shell wm size
  adb_t 20 shell wm density
  echo "window_animation=$(adb_t 20 shell settings get global window_animation_scale | tr -d '\r')"
  echo "transition_animation=$(adb_t 20 shell settings get global transition_animation_scale | tr -d '\r')"
  echo "animator_duration=$(adb_t 20 shell settings get global animator_duration_scale | tr -d '\r')"
  echo "immersive_mode_confirmations=$(adb_t 20 shell settings get secure immersive_mode_confirmations | tr -d '\r')"
  echo "nproc=$(adb_t 20 shell nproc | tr -d '\r')"
  adb_t 20 shell cat /proc/meminfo | head -n 3
  adb_t 30 shell dumpsys SurfaceFlinger | grep -E 'GLES|OpenGL|renderer' | head -n 20 || true
} | tee "$ev/device.txt"

app_apk="$root/offgrid-head/04 Build/app/build/outputs/apk/debug/app-debug.apk"
empty_apk="$root/docs/qa/independent_qa/empty/empty-debug.apk"
test -s "$app_apk"
test -s "$empty_apk"
progress "install empty apk"
adb_t 120 install -r -d "$empty_apk"
progress "install offgrid apk"
if ! adb_t 120 install -r -d "$app_apk"; then
  progress "offgrid install failed"
  exit 1
fi
adb_t 30 shell pm grant com.offgrid.compositor android.permission.CAMERA || true
progress "installs done"

(
  i=1
  while [ ! -f "$ev/recording.stop" ] && [ "$i" -le 16 ]; do
    progress "screenrecord clip ${i} begin"
    timeout --signal=KILL 15 adb shell pkill -2 screenrecord >/dev/null 2>&1 || true
    timeout --signal=KILL 200 adb shell screenrecord --time-limit 170 --bit-rate 4000000 "/sdcard/qa_r${i}.mp4" || true
    timeout --signal=KILL 60 adb pull "/sdcard/qa_r${i}.mp4" "$ev/video/qa_r${i}.mp4" || true
    timeout --signal=KILL 20 adb shell rm -f "/sdcard/qa_r${i}.mp4" || true
    progress "screenrecord clip ${i} pulled"
    i=$((i + 1))
  done
  progress "screenrecord loop end"
) >>"$ev/progress.log" 2>&1 &
rec_pid=$!

set +e
progress "walk.py begin"
python3 "$root/docs/qa/independent_qa/walk.py"
walk_status=$?
progress "walk.py exit ${walk_status}"

progress "instrument suite begin cap 1200s"
(
  cd "$root/offgrid-head/04 Build"
  timeout --signal=KILL 1200 ./gradlew :app:connectedDebugAndroidTest --stacktrace \
    -Pandroid.testInstrumentationRunnerArguments.class=com.offgrid.compositor.RoyaltyPhoneFixTest
)
instrument_status=$?
progress "instrument suite exit ${instrument_status}"
set -e

progress "pull logcat and anr"
adb_t 120 logcat -d -v threadtime >"$ev/logcat.txt" || true
adb_t 30 shell ls /data/anr >"$ev/anr/list.txt" 2>/dev/null || true
timeout --signal=KILL 60 adb pull /data/anr "$ev/anr" >/dev/null 2>&1 || true
mkdir -p "$ev/instrument"
if [ -d "$root/offgrid-head/04 Build/app/build/reports/androidTests" ]; then
  cp -a "$root/offgrid-head/04 Build/app/build/reports/androidTests/." "$ev/instrument/reports/" || true
fi
if [ -d "$root/offgrid-head/04 Build/app/build/outputs/androidTest-results" ]; then
  cp -a "$root/offgrid-head/04 Build/app/build/outputs/androidTest-results/." "$ev/instrument/results/" || true
fi
printf 'walk_exit=%s\ninstrument_exit=%s\n' "$walk_status" "$instrument_status" | tee "$ev/exit_codes.txt"

progress "stop screenrecord"
touch "$ev/recording.stop"
adb_t 20 shell 'kill -2 $(pidof screenrecord) 2>/dev/null || true' || true
# wait is a shell builtin, so GNU timeout cannot run it. The recorder loop exits on the flag.
kill "$rec_pid" 2>/dev/null || true
progress "script end walk=${walk_status} instrument=${instrument_status}"

if [ "$walk_status" -ne 0 ] || [ "$instrument_status" -ne 0 ]; then
  echo "Independent QA finished with walk=$walk_status instrument=$instrument_status"
  exit 1
fi
echo "Independent QA walk and instrument suite both exited 0"
