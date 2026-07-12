#!/data/data/com.ominal/files/usr/bin/sh
set -eu

PREFIX="${OMINAL_PREFIX:-/data/data/com.ominal/files/usr}"
RUNNER="${OMINAL_PROOT_RUNNER:-$PREFIX/bin/ominal-proot-run}"

if [ ! -x "$RUNNER" ]; then
    printf 'Ominal PRoot launcher is missing: %s\n' "$RUNNER" >&2
    exit 69
fi

exec "$RUNNER" /bin/bash -lc '
set -eu
export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y xvfb x11vnc novnc websockify fluxbox xterm x11-apps python3 python3-tk pcmanfm
'
