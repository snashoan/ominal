#!/data/data/com.ominal/files/usr/bin/sh
set -eu

PREFIX="${OMINAL_PREFIX:-/data/data/com.ominal/files/usr}"
RUNNER="${OMINAL_PROOT_RUNNER:-$PREFIX/bin/ominal-proot-run}"

if [ ! -x "$RUNNER" ]; then
    printf 'Ominal PRoot launcher is missing: %s\n' "$RUNNER" >&2
    exit 69
fi
if "$RUNNER" /bin/bash -lc 'for command_name in Xvfb x11vnc websockify fluxbox xterm; do command -v "$command_name" >/dev/null || exit 1; done; test -d /usr/share/novnc || test -d /usr/share/noVNC'; then
    exit 0
fi

exec "$RUNNER" /bin/bash -lc '
set -eu
export DEBIAN_FRONTEND=noninteractive
export TZ=Etc/UTC
if [ ! -x /usr/sbin/policy-rc.d ]; then
    printf "#!/bin/sh\nexit 101\n" > /usr/sbin/policy-rc.d
    chmod 755 /usr/sbin/policy-rc.d
fi
apt-get -o Acquire::Retries=5 -o Acquire::ForceIPv4=true -o Dpkg::Use-Pty=0 update
apt-get -o Acquire::Retries=5 -o Acquire::ForceIPv4=true -o Dpkg::Use-Pty=0 \
    install -y --no-install-recommends --no-install-suggests \
    ca-certificates xvfb x11vnc novnc websockify fluxbox xterm python3
for command_name in Xvfb x11vnc websockify fluxbox xterm; do command -v "$command_name" >/dev/null; done
test -d /usr/share/novnc || test -d /usr/share/noVNC
apt-get clean
rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/*
'
