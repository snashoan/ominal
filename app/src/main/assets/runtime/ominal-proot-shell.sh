#!/data/data/com.ominal/files/usr/bin/sh
set -eu

PREFIX="${OMINAL_PREFIX:-/data/data/com.ominal/files/usr}"
HOME="${OMINAL_HOME:-/data/data/com.ominal/files/home}"
RUNNER="${OMINAL_PROOT_RUNNER:-$PREFIX/bin/ominal-proot-run}"
WORKSPACE="${1:-$HOME/workspace}"

case "$WORKSPACE" in
    /data/user/0/com.ominal/*)
        WORKSPACE="/data/data/com.ominal${WORKSPACE#/data/user/0/com.ominal}"
        ;;
    /root/workspace|/root/workspace/*|'')
        WORKSPACE="$HOME/workspace"
        ;;
esac

case "$WORKSPACE" in
    "$HOME"|"$HOME"/*) ;;
    *)
        printf 'Refusing terminal workspace outside Ominal home: %s\n' "$WORKSPACE" >&2
        exit 64
        ;;
esac

if [ ! -x "$RUNNER" ]; then
    printf 'Ominal PRoot launcher is missing: %s\n' "$RUNNER" >&2
    exit 69
fi

mkdir -p "$WORKSPACE"
export OMINAL_WORKDIR="$WORKSPACE"
exec "$RUNNER" /bin/bash -lc '
    if command -v git >/dev/null 2>&1 && [ ! -d .git ]; then
        git init --quiet
    fi
    exec /bin/bash --login
'
