#!/data/data/com.termux/files/usr/bin/bash
set -e

ROOT="$(cd "$(dirname "$0")" && pwd)"
LIBDIR="$ROOT/third_party/libXray"
OUT="$ROOT/android/app/libs"

: "${ANDROID_HOME:=/data/data/com.termux/files/home/android-sdk}"
: "${ANDROID_SDK_ROOT:=$ANDROID_HOME}"
export ANDROID_HOME ANDROID_SDK_ROOT
export PATH="$HOME/go/bin:$PATH"

mkdir -p "$LIBDIR" "$OUT"

if [ ! -d "$LIBDIR/.git" ]; then
  git clone https://github.com/XTLS/libXray.git "$LIBDIR"
fi
cd "$LIBDIR"
git fetch --tags --force origin
# Exact upstream release commit requested for this app.
git checkout --detach 50b9597

echo "== libXray =="
git rev-parse HEAD

echo "== Go =="
go version

echo "== Android SDK =="
ls -d "$ANDROID_HOME/platforms/android-36" "$ANDROID_HOME/build-tools/35.0.0" >/dev/null

echo "== gomobile =="
if ! command -v gomobile >/dev/null 2>&1 || ! gomobile version >/dev/null 2>&1; then
  go install golang.org/x/mobile/cmd/gomobile@latest
  go install golang.org/x/mobile/cmd/gobind@latest
fi
export PATH="$HOME/go/bin:$PATH"
gomobile version || true
gomobile init

python3 build/main.py android

cp -f libXray.aar "$OUT/libXray.aar"
cp -f libXray-sources.jar "$OUT/libXray-sources.jar"
sha256sum "$OUT/libXray.aar" "$OUT/libXray-sources.jar"
