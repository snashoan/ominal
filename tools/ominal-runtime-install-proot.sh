#!/data/data/com.ominal/files/usr/bin/sh
set -eu

PREFIX="${OMINAL_PREFIX:-/data/data/com.ominal/files/usr}"
HOME="${OMINAL_HOME:-/data/data/com.ominal/files/home}"
RUNTIME_ROOT="${OMINAL_RUNTIME_ROOT:-$HOME/.ominal/runtime}"
PROOT_BIN="$PREFIX/bin/proot"
PROOT_LOADER="${PROOT_LOADER:-$PREFIX/libexec/proot/loader}"

if [ "$#" -ne 0 ]; then
    printf '%s\n' 'Ominal PRoot is supplied by the installed bootstrap; no archive is accepted.' >&2
    exit 64
fi

if [ ! -x "$PROOT_BIN" ] || [ ! -x "$PROOT_LOADER" ]; then
    printf '%s\n' 'Ominal bootstrap is missing its Android-compatible PRoot runtime.' >&2
    exit 69
fi

mkdir -p "$RUNTIME_ROOT/tmp"
export PREFIX HOME PROOT_LOADER PROOT_TMP_DIR="$RUNTIME_ROOT/tmp"

# The shell already has the Android exec bridge mapped. Keep it out of PRoot's
# child environment so the Linux guest never inherits an Android preload.
unset LD_PRELOAD
exec "$PROOT_BIN" --version
