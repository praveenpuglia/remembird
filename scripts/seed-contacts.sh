#!/usr/bin/env bash
# Wipes the EMULATOR's contacts and seeds test people (see app/src/androidTest/.../SeedContacts.kt for the cases).
# Refuses to run against a real phone.
set -euo pipefail
cd "$(dirname "$0")/.."

serial=${ANDROID_SERIAL:-$(adb devices | awk '$1 ~ /^emulator-/ && $2 == "device" {print $1; exit}')}
[ -n "$serial" ] || { echo "No authorised emulator found (check adb devices)." >&2; exit 1; }
[ "$(adb -s "$serial" shell getprop ro.boot.qemu | tr -d '\r')" = 1 ] || { echo "$serial is not an emulator; refusing to wipe its contacts." >&2; exit 1; }

ANDROID_SERIAL=$serial ./gradlew -q installDebug installDebugAndroidTest
adb -s "$serial" shell am instrument -w -e class com.praveenpuglia.remembird.SeedContacts \
  com.praveenpuglia.remembird.test/androidx.test.runner.AndroidJUnitRunner
