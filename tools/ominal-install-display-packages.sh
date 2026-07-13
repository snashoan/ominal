#!/data/data/com.ominal/files/usr/bin/sh
set -eu

PREFIX="${OMINAL_PREFIX:-/data/data/com.ominal/files/usr}"
HOME="${OMINAL_HOME:-/data/data/com.ominal/files/home}"
RUNNER="${OMINAL_PROOT_RUNNER:-$PREFIX/bin/ominal-proot-run}"
ROOTFS="${OMINAL_RUNTIME_ROOT:-$HOME/.ominal/runtime}/linux/rootfs"
SCREEN_HELPER="$PREFIX/bin/ominal-screen-guest"

if [ ! -x "$RUNNER" ]; then
    printf 'Ominal PRoot launcher is missing: %s\n' "$RUNNER" >&2
    exit 69
fi
if ! "$RUNNER" /bin/bash -lc 'for command_name in Xvfb x11vnc websockify jwm xterm xfe xfwrite xdotool wmctrl scrot xdpyinfo; do command -v "$command_name" >/dev/null || exit 1; done; test -d /usr/share/novnc || test -d /usr/share/noVNC'; then
"$RUNNER" /bin/bash -lc '
set -eu
export DEBIAN_FRONTEND=noninteractive
export TZ=Etc/UTC
if [ -n "${OMINAL_APT_HOSTS:-}" ]; then
    hosts_tmp=/tmp/ominal-hosts.$$
    if [ -f /etc/hosts ]; then
        grep -v "ports\\.ubuntu\\.com" /etc/hosts > "$hosts_tmp" || true
    else
        : > "$hosts_tmp"
    fi
    for apt_address in $OMINAL_APT_HOSTS; do
        printf "%s ports.ubuntu.com\\n" "$apt_address" >> "$hosts_tmp"
    done
    cat "$hosts_tmp" > /etc/hosts
    rm -f "$hosts_tmp"
fi
if [ ! -x /usr/sbin/policy-rc.d ]; then
    printf "#!/bin/sh\nexit 101\n" > /usr/sbin/policy-rc.d
    chmod 755 /usr/sbin/policy-rc.d
fi
apt-get -o Acquire::Retries=5 -o Acquire::ForceIPv4=true -o Dpkg::Use-Pty=0 update
apt-get -o Acquire::Retries=5 -o Acquire::ForceIPv4=true -o Dpkg::Use-Pty=0 \
    install -y --no-install-recommends --no-install-suggests \
    ca-certificates xvfb x11vnc novnc websockify jwm xterm python3 xfe \
    xdotool wmctrl scrot x11-utils x11-xserver-utils
for command_name in Xvfb x11vnc websockify jwm xterm xfe xfwrite xdotool wmctrl scrot xdpyinfo; do command -v "$command_name" >/dev/null; done
test -d /usr/share/novnc || test -d /usr/share/noVNC
apt-get clean
rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/*
'
fi

if [ ! -f "$SCREEN_HELPER" ]; then
    printf '%s\n' 'Ominal screen controls are missing.' >&2
    exit 69
fi
mkdir -p "$ROOTFS/usr/local/bin"
/system/bin/cp "$SCREEN_HELPER" "$ROOTFS/usr/local/bin/ominal-screen"
/system/bin/chmod 755 "$ROOTFS/usr/local/bin/ominal-screen"
"$RUNNER" /bin/bash -lc 'command -v ominal-screen >/dev/null; ominal-screen --help >/dev/null'
