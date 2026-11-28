#!/usr/bin/env bash
# Fails if any native library in the APK has an ELF LOAD segment aligned below 16 KB.
# Android 15+ devices with 16 KB pages refuse to map such libraries (System.load throws
# UnsatisfiedLinkError), which would leave the module without any hooks.
set -euo pipefail
apk="${1:?usage: check-native-alignment.sh <apk>}"
tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' EXIT
unzip -q -o "$apk" 'lib/*' -d "$tmp" || { echo "no native libs in $apk"; exit 0; }
fail=0
while IFS= read -r so; do
  # 64-bit ABIs only: 32-bit devices don't use 16 KB pages.
  case "$so" in *arm64-v8a/*|*x86_64/*) ;; *) continue ;; esac
  bad=0
  for a in $(readelf -lW "$so" | awk '$1=="LOAD"{print $NF}'); do
    if (( a < 0x4000 )); then bad=1; fi
  done
  if [ "$bad" = 1 ]; then echo "::error::${so#$tmp/}: a LOAD segment is aligned below 0x4000 (16 KB)"; fail=1
  else echo "ok  ${so#$tmp/}"; fi
done < <(find "$tmp/lib" -name '*.so')
exit $fail
