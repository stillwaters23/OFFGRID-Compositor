#!/usr/bin/env bash
# Host-side KVM check, run after the emulator package is installed and before boot.
set -u
root="${GITHUB_WORKSPACE:-$(pwd)}"
ev="$root/docs/qa/independent_qa/evidence"
mkdir -p "$ev"
{
  echo "date=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "nproc=$(nproc)"
  free -h || true
  ls -l /dev/kvm || true
  if [ -x "${ANDROID_HOME:-}/emulator/emulator" ]; then
    "${ANDROID_HOME}/emulator/emulator" -accel-check || true
  else
    emulator -accel-check || true
  fi
} >"$ev/kvm.txt" 2>&1
cat "$ev/kvm.txt"
if [ ! -r /dev/kvm ]; then
  echo "KVM device is not readable. This runner is unfit." >&2
  exit 1
fi

# The emulator-runner launches $ANDROID_HOME/emulator/emulator and reads its
# pipes. A wrapper tees those pipes to files without dropping them.
emu_dir="${ANDROID_HOME:-$ANDROID_SDK_ROOT}/emulator"
emu="$emu_dir/emulator"
if [ -x "$emu" ] && [ ! -e "$emu_dir/emulator.real" ]; then
  mv "$emu" "$emu_dir/emulator.real"
  cat >"$emu" << 'EOF'
#!/bin/bash
here="$(cd "$(dirname "$0")" && pwd)"
log="${GITHUB_WORKSPACE:-/tmp}/docs/qa/independent_qa/evidence"
mkdir -p "$log"
exec "$here/emulator.real" "$@" \
  > >(tee -a "$log/emulator-process.stdout") \
  2> >(tee -a "$log/emulator-process.stderr" >&2)
EOF
  chmod +x "$emu"
  echo "wrapped emulator binary for stdout/stderr capture" >>"$ev/kvm.txt"
fi

# Host memory of qemu, and kernel log, for the life of this job.
nohup bash -c '
  ev="$1"
  while [ ! -f "$ev/recording.stop" ]; do
    {
      echo "===== $(date -u +%H:%M:%S) ====="
      ps -eo pid,rss,vsz,pcpu,comm | awk "BEGIN{IGNORECASE=1} /qemu|emulator/ && !/awk/"
      free -m | head -n 2
    } >>"$ev/emulator-memory.log" 2>>"$ev/emulator-memory.err"
    sleep 10
  done
' bash "$ev" >/dev/null 2>&1 &
echo "memory_sampler=$!" >>"$ev/kvm.txt"

nohup sudo dmesg -wT >>"$ev/dmesg-follow.txt" 2>>"$ev/dmesg-follow.err" &
echo "dmesg_follow=$!" >>"$ev/kvm.txt"
