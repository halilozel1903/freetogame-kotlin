#!/usr/bin/env bash
# Shared helpers for scripts/screenshots.sh. Expects PKG to be set by the caller's environment
# or falls back to the sample's application id from sample/build.gradle.kts.
PKG="${PKG:-$(grep -oE 'applicationId = "[^"]+"' sample/build.gradle.kts | cut -d'"' -f2)}"
OUT="docs/screenshots"
mkdir -p "$OUT"

install_sample() {
  adb install -r sample/build/outputs/apk/debug/sample-debug.apk
  # A freshly booted TV emulator often shows "System UI isn't responding" or a launcher ANR.
  # Hide error/ANR dialogs and give the system time to settle.
  adb shell settings put global hide_error_dialogs 1
  adb shell settings put global anr_show_background 0
  sleep 20
  dismiss_system_dialogs
}

# Scenes log "SCENE_READY <scene>" under this tag once the live data and every image on screen have loaded
# (focus in place), or "SCENE_FAILED <scene>: <reason>" when a request or an image fails.
SCENE_TAG="${SCENE_TAG:-FreeToGameScene}"

fresh_launch() {
  adb shell pm clear "$PKG" > /dev/null
  adb logcat -c || true
  adb shell am start -W -n "$PKG/.MainActivity" "$@" > /dev/null
}

# Waits until the scene reports that it is complete. Fails on SCENE_FAILED or after a timeout, so a screen
# that did not settle never replaces a good screenshot.
wait_for_scene() {
  local scene="$1" timeout="${2:-120}" log=""
  for _ in $(seq 1 "$timeout"); do
    log="$(adb logcat -d -s "$SCENE_TAG:*" 2> /dev/null || true)"
    if printf '%s' "$log" | grep -q "SCENE_FAILED"; then
      echo "The $scene scene failed:" >&2
      printf '%s\n' "$log" | grep "SCENE_" >&2
      exit 1
    fi
    if printf '%s' "$log" | grep -q "SCENE_READY $scene"; then
      echo "Scene $scene is ready"
      sleep 1
      return 0
    fi
    sleep 1
  done
  echo "The $scene scene did not get ready within ${timeout}s." >&2
  adb logcat -d -t 200 >&2 || true
  exit 1
}

dismiss_system_dialogs() {
  for _ in 1 2 3; do
    if adb shell dumpsys window | grep -qiE "Application Not Responding|isn't responding"; then
      adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS > /dev/null
      adb shell input keyevent KEYCODE_ENTER
      sleep 2
    else
      return 0
    fi
  done
  if adb shell dumpsys window | grep -qiE "Application Not Responding|isn't responding"; then
    echo "A system dialog is still on screen; refusing to capture a broken screenshot." >&2
    exit 1
  fi
}

# Fails when the sample is not the resumed activity or the expected text is missing from the
# screen. A failing UI dump (it happens on busy emulators) only warns; the image checks still run.
expect_on_screen() {
  local text="$1"
  local resumed
  resumed="$(adb shell dumpsys activity activities | grep -E "ResumedActivity" || true)"
  if [ -n "$resumed" ] && ! printf '%s' "$resumed" | grep -q "$PKG"; then
    echo "The sample app is not in front; refusing to capture." >&2
    printf '%s\n' "$resumed" >&2
    exit 1
  fi
  local xml=""
  for _ in 1 2 3; do
    if adb shell uiautomator dump /sdcard/window.xml > /dev/null 2>&1; then
      xml="$(adb exec-out cat /sdcard/window.xml)"
      break
    fi
    sleep 2
  done
  if [ -z "$xml" ]; then
    echo "Warning: could not dump the UI to look for \"$text\"." >&2
  elif ! printf '%s' "$xml" | grep -qF "$text"; then
    echo "\"$text\" is not on screen; the scene did not appear." >&2
    exit 1
  fi
}

# Fails on a missing, tiny, blank or single colored PNG, and on a TV sized capture that is
# almost all black (a video surface that never drew).
check_png() {
  python3 - "$1" <<'PY'
import struct, sys, zlib

path = sys.argv[1]
with open(path, "rb") as f:
    data = f.read()
if len(data) < 30_000 or not data.startswith(b"\x89PNG\r\n\x1a\n"):
    sys.exit(f"{path}: not a PNG or too small ({len(data)} bytes)")
width, height = struct.unpack(">II", data[16:24])
if width < 1280 or height < 720:
    sys.exit(f"{path}: {width}x{height} is not a TV sized screenshot")
idat, pos = b"", 8
while pos < len(data):
    length, kind = struct.unpack(">I4s", data[pos:pos + 8])
    if kind == b"IDAT":
        idat += data[pos + 8:pos + 8 + length]
    pos += 12 + length
raw = zlib.decompress(idat)
# Filtered scanlines of a blank screen are almost all zeros; real content has many different bytes.
distinct = len(set(raw[::97]))
nonzero = sum(1 for b in raw[::211] if b) / max(1, len(raw[::211]))
if distinct < 24 or (nonzero < 0.01 and distinct < 120):
    sys.exit(f"{path}: looks blank ({width}x{height}, {distinct} distinct samples, {nonzero:.3f} non-zero)")
print(f"{path}: {width}x{height}, {len(data) // 1024} KB, {distinct} distinct samples")
PY
}

capture() {
  local name="$1" expected="$2"
  dismiss_system_dialogs
  expect_on_screen "$expected"
  adb exec-out screencap -p > "$OUT/$name.png"
  if ! check_png "$OUT/$name.png"; then
    rm -f "$OUT/$name.png"
    exit 1
  fi
  echo "Captured $name"
}
