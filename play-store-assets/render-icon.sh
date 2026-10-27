#!/usr/bin/env bash
# Renders the 512x512 Play Store icon from icon.svg with headless Chrome (macOS path).
set -euo pipefail
cd "$(dirname "$0")"
"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" --headless=new --disable-gpu --hide-scrollbars \
  --window-size=512,512 --screenshot="$PWD/../app/src/main/ic_launcher-playstore.png" "file://$PWD/icon.svg" 2>/dev/null
