#!/usr/bin/env bash
# One command for android-emulator-runner. That action runs /usr/bin/sh -c on
# each script line in a new process, so "cd" and "set -o pipefail" cannot live
# in the workflow YAML.
set -u
api="${1:?api level}"
root="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$root"
chmod +x "04 Build/gradlew"

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
for path in \
  /sdcard/Android/data/com.offgrid.compositor/files/Pictures/royalty-fix-evidence \
  /storage/emulated/0/Android/data/com.offgrid.compositor/files/Pictures/royalty-fix-evidence
do
  adb pull "$path/." "$dest" && break
done || true
echo "Pulled emulator screenshots into $dest"
ls -la "$dest" || true
exit "$status"
