#!/data/data/com.ominal/files/usr/bin/sh
set -eu

PREFIX="${OMINAL_PREFIX:-/data/data/com.ominal/files/usr}"
RUNNER="${OMINAL_PROOT_RUNNER:-$PREFIX/bin/ominal-proot-run}"

if [ ! -x "$RUNNER" ]; then
    printf 'Ominal PRoot launcher is missing: %s\n' "$RUNNER" >&2
    exit 69
fi

exec "$RUNNER" /bin/bash -c 'exec codex "$@"' ominal-codex "$@"
