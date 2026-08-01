#!/data/data/com.ominal/files/usr/bin/sh
set -eu

PREFIX="${OMINAL_PREFIX:-/data/data/com.ominal/files/usr}"
HOME="${OMINAL_HOME:-/data/data/com.ominal/files/home}"
RUNNER="${OMINAL_PROOT_RUNNER:-$PREFIX/bin/ominal-proot-run}"
ROOTFS="${OMINAL_RUNTIME_ROOT:-$HOME/.ominal/runtime}/linux/rootfs"
SCREEN_HELPER="$PREFIX/bin/ominal-screen-guest"
EVENT_HELPER="$PREFIX/bin/ominal-event-guest"
DEVICE_HELPER="$PREFIX/bin/ominal-device-guest"
PACKAGE_HELPER="$PREFIX/bin/ominal-package-guest"
HARNESS_HOOK_HELPER="$PREFIX/bin/ominal-harness-hook"
UPGRADE_MARKER="$ROOTFS/var/lib/ominal/base-upgrade-noble-v6"

if [ ! -x "$RUNNER" ]; then
    printf 'Ominal PRoot launcher is missing: %s\n' "$RUNNER" >&2
    exit 69
fi
if [ ! -f "$UPGRADE_MARKER" ] || ! "$RUNNER" /bin/bash -lc 'for command_name in Xvfb x11vnc websockify jwm xterm pcmanfm xfwrite firefox xfce4-settings-manager xfce4-session xfwm4 xfce4-panel xfdesktop thunar xfce4-terminal mousepad devilspie2 unclutter-xfixes dbus-run-session xdotool wmctrl scrot xdpyinfo xrdb; do command -v "$command_name" >/dev/null || exit 1; done; test -d /usr/share/novnc || test -d /usr/share/noVNC'; then
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
if ! dpkg --configure -a; then
    apt-get -o Acquire::Retries=5 -o Dpkg::Use-Pty=0 -f install -y || true
    dpkg --configure -a
fi
apt-get -o Acquire::Retries=5 -o Dpkg::Use-Pty=0 -f install -y
if [ ! -f /var/lib/ominal/base-upgrade-noble-v1 ]; then
    apt-get -o Acquire::Retries=5 -o Dpkg::Use-Pty=0 full-upgrade -y
fi
apt-get -o Acquire::Retries=5 -o Acquire::ForceIPv4=true -o Dpkg::Use-Pty=0 \
    install -y --no-install-recommends ca-certificates wget gnupg
install -d -m 0755 /etc/apt/keyrings
wget -q https://packages.mozilla.org/apt/repo-signing-key.gpg \
    -O /etc/apt/keyrings/packages.mozilla.org.asc
mozilla_fingerprint="$(gpg -n -q --import-options import-show --import \
    /etc/apt/keyrings/packages.mozilla.org.asc 2>/dev/null |
    awk "/^pub/ {getline; gsub(/[[:space:]]/, \"\"); print; exit}")"
test "$mozilla_fingerprint" = "35BAA0B33E9EB396F59CA838C0BA5CE6DC6315A3"
printf "%s\n" \
    "deb [signed-by=/etc/apt/keyrings/packages.mozilla.org.asc] https://packages.mozilla.org/apt mozilla main" \
    > /etc/apt/sources.list.d/mozilla.list
printf "%s\n" "Package: *" "Pin: origin packages.mozilla.org" "Pin-Priority: 1000" \
    > /etc/apt/preferences.d/mozilla
apt-get -o Acquire::Retries=5 -o Acquire::ForceIPv4=true -o Dpkg::Use-Pty=0 update
apt-get -o Acquire::Retries=5 -o Acquire::ForceIPv4=true -o Dpkg::Use-Pty=0 \
    install -y --no-install-recommends --no-install-suggests \
    ca-certificates git xvfb x11vnc novnc websockify jwm xterm python3 xfe pcmanfm firefox \
    xfce4-settings xfce4-session xfwm4 xfce4-panel xfdesktop4 thunar xfce4-terminal \
    mousepad xfce4-appfinder xfce4-notifyd tumbler gvfs devilspie2 unclutter-xfixes \
    dbus-x11 adwaita-icon-theme librsvg2-common shared-mime-info \
    fontconfig fonts-dejavu-core fonts-noto-core \
    libnss3 libnspr4 xdotool wmctrl scrot \
    x11-utils x11-xserver-utils
  for command_name in Xvfb x11vnc websockify jwm xterm pcmanfm xfwrite firefox xfce4-settings-manager xfce4-session xfwm4 xfce4-panel xfdesktop thunar xfce4-terminal mousepad devilspie2 unclutter-xfixes dbus-run-session xdotool wmctrl scrot xdpyinfo xrdb; do command -v "$command_name" >/dev/null; done
test -d /usr/share/novnc || test -d /usr/share/noVNC
mkdir -p /var/lib/ominal
  touch /var/lib/ominal/base-upgrade-noble-v1 /var/lib/ominal/base-upgrade-noble-v2 \
      /var/lib/ominal/base-upgrade-noble-v3 /var/lib/ominal/base-upgrade-noble-v4 \
      /var/lib/ominal/base-upgrade-noble-v5 /var/lib/ominal/base-upgrade-noble-v6 \
      /var/lib/ominal/apt-refreshed
apt-get clean
rm -rf /var/cache/apt/archives/*
'
fi

if [ ! -f "$SCREEN_HELPER" ]; then
    printf '%s\n' 'Ominal screen controls are missing.' >&2
    exit 69
fi
if [ ! -f "$EVENT_HELPER" ]; then
    printf '%s\n' 'Ominal agent event control is missing.' >&2
    exit 69
fi
if [ ! -f "$DEVICE_HELPER" ]; then
    printf '%s\n' 'Ominal Android controls are missing.' >&2
    exit 69
fi
if [ ! -f "$PACKAGE_HELPER" ]; then
    printf '%s\n' 'Ominal package controls are missing.' >&2
    exit 69
fi
mkdir -p "$ROOTFS/usr/local/bin"
/system/bin/cp "$SCREEN_HELPER" "$ROOTFS/usr/local/bin/ominal-screen"
/system/bin/cp "$EVENT_HELPER" "$ROOTFS/usr/local/bin/ominal-event"
/system/bin/cp "$DEVICE_HELPER" "$ROOTFS/usr/local/bin/ominal-device"
/system/bin/cp "$PACKAGE_HELPER" "$ROOTFS/usr/local/bin/ominal-install"
/system/bin/cp "$HARNESS_HOOK_HELPER" "$ROOTFS/usr/local/bin/ominal-harness-hook"
/system/bin/chmod 755 "$ROOTFS/usr/local/bin/ominal-screen"
/system/bin/chmod 755 "$ROOTFS/usr/local/bin/ominal-event"
/system/bin/chmod 755 "$ROOTFS/usr/local/bin/ominal-device"
/system/bin/chmod 755 "$ROOTFS/usr/local/bin/ominal-install"
/system/bin/chmod 755 "$ROOTFS/usr/local/bin/ominal-harness-hook"
"$RUNNER" /bin/bash -lc 'command -v ominal-screen >/dev/null; command -v ominal-event >/dev/null; command -v ominal-device >/dev/null; command -v ominal-install >/dev/null; command -v ominal-harness-hook >/dev/null; ominal-screen --help >/dev/null'
