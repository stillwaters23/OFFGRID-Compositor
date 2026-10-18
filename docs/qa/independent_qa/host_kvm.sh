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
