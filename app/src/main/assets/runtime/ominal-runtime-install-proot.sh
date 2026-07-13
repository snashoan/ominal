#!/data/data/com.ominal/files/usr/bin/sh
set -eu

PREFIX="${OMINAL_PREFIX:-/data/data/com.ominal/files/usr}"
HOME="${OMINAL_HOME:-/data/data/com.ominal/files/home}"
RUNTIME_ROOT="${OMINAL_RUNTIME_ROOT:-$HOME/.ominal/runtime}"
ARCHIVE_PATH="${1:?missing PRoot archive path}"
PROOT_ROOT="$RUNTIME_ROOT/proot/root"

mkdir -p "$RUNTIME_ROOT/proot" "$RUNTIME_ROOT/tmp"
if [ ! -x "$PROOT_ROOT/bin/proot" ]; then
    /system/bin/tar -xzf "$ARCHIVE_PATH" -C "$RUNTIME_ROOT/proot"
fi
chmod 700 "$PROOT_ROOT/bin/proot" "$PROOT_ROOT/bin/proot-userland"
chmod 700 "$PROOT_ROOT/libexec/proot/loader" "$PROOT_ROOT/libexec/proot/loader32"
export PREFIX HOME PROOT_TMP_DIR="$RUNTIME_ROOT/tmp"
exec "$PROOT_ROOT/bin/proot" --version
