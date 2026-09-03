#!/data/data/com.ominal/files/usr/bin/sh
set -eu

PREFIX="${OMINAL_PREFIX:-/data/data/com.ominal/files/usr}"
HOME="${OMINAL_HOME:-/data/data/com.ominal/files/home}"
RUNTIME_ROOT="${OMINAL_RUNTIME_ROOT:-$HOME/.ominal/runtime}"
RUNTIME_ARCH="${OMINAL_RUNTIME_ARCH:-arm64}"
RUNTIME_VERSION="ominal-ubuntu-24.04.4-${RUNTIME_ARCH}-node-24.18.0-codex-0.144.6-desktop-xfce-v4"
READY_FILE="$RUNTIME_ROOT/.ominal-runtime-ready"
PREPARED_ROOTFS_VERSION="ominal-ubuntu-24.04.4-arm64-prepared-v3"

verify_codex_runtime() {
    "$PREFIX/bin/ominal-proot-run" /bin/bash -lc '
set -eu
test "$(node --version)" = "v24.18.0"
test "$(command -v codex)" = "/root/.ominal/npm/bin/codex"
codex --version | grep -E "^codex-cli [0-9]+\.[0-9]+\.[0-9]+" >/dev/null
'
}

verify_runtime() {
    "$PREFIX/bin/ominal-proot-run" /bin/bash -lc '
set -eu
test "$(dpkg --print-architecture)" = "${OMINAL_RUNTIME_ARCH:-arm64}"
test -z "$(dpkg --audit)"
test "$(node --version)" = "v24.18.0"
test "$(command -v codex)" = "/root/.ominal/npm/bin/codex"
codex --version | grep -E "^codex-cli [0-9]+\.[0-9]+\.[0-9]+" >/dev/null
for command_name in Xvfb x11vnc websockify jwm xterm pcmanfm xfwrite firefox xfce4-settings-manager xfce4-session xfwm4 xfce4-panel xfdesktop thunar xfce4-terminal mousepad devilspie2 unclutter-xfixes dbus-run-session xdotool wmctrl scrot xdpyinfo xrdb xclip file yad xdg-mime update-desktop-database ominal-screen ominal-event ominal-theme ominal-device ominal-install gir-harness gir-chats ominal-open-executable; do command -v "$command_name" >/dev/null; done
test -d /usr/share/novnc || test -d /usr/share/noVNC
test -f /var/lib/ominal/base-upgrade-noble-v8
for package_name in desktop-file-utils file libnss3 libnspr4 librsvg2-common shared-mime-info xdg-utils yad; do
    test "$(dpkg-query -W -f=\${Status} "$package_name" 2>/dev/null)" = "install ok installed"
done
apt-get -o Debug::NoLocking=1 check >/dev/null
'
}

mark_ready() {
    mkdir -p "$RUNTIME_ROOT"
    printf '%s\n' "$RUNTIME_VERSION" > "$READY_FILE.tmp"
    mv "$READY_FILE.tmp" "$READY_FILE"
}

if [ "${1:-}" = "--verify" ]; then
    verify_runtime
    mark_ready
    exit 0
fi

if [ "${1:-}" = "--prepare" ]; then
    if ! verify_runtime; then
        verify_codex_runtime
        "$PREFIX/bin/ominal-install-display-packages"
        verify_runtime
    fi
    mark_ready
    exit 0
fi

if [ "${1:-}" = "--install-prepared" ]; then
    PREPARED_ROOTFS_ARCHIVE="${2:?missing prepared rootfs archive}"
    rm -f "$READY_FILE" "$READY_FILE.tmp"
    "$PREFIX/bin/ominal-runtime-install-proot"
    OMINAL_ROOTFS_VERSION="$PREPARED_ROOTFS_VERSION" \
        "$PREFIX/bin/ominal-runtime-install-ubuntu-base" "$PREPARED_ROOTFS_ARCHIVE"
    verify_codex_runtime
    "$PREFIX/bin/ominal-install-display-packages"
    verify_runtime
    mark_ready
    rm -rf "$RUNTIME_ROOT/linux/rootfs.previous"
    printf '%s\n' 'Ominal prepared runtime ready'
    exit 0
fi

ROOTFS_ARCHIVE="${1:?missing Ubuntu rootfs archive}"
NODE_ARCHIVE="${2:?missing Node archive}"
CODEX_CORE_ARCHIVE="${3:?missing Codex core archive}"
CODEX_PLATFORM_ARCHIVE="${4:?missing Codex arm64 archive}"

rm -f "$READY_FILE" "$READY_FILE.tmp"
"$PREFIX/bin/ominal-runtime-install-proot"
"$PREFIX/bin/ominal-runtime-install-ubuntu-base" "$ROOTFS_ARCHIVE"
if ! verify_codex_runtime; then
    "$PREFIX/bin/ominal-proot-install-local-codex" \
        "$NODE_ARCHIVE" "$CODEX_CORE_ARCHIVE" "$CODEX_PLATFORM_ARCHIVE"
fi
"$PREFIX/bin/ominal-install-display-packages"
verify_runtime
mark_ready
rm -rf "$RUNTIME_ROOT/linux/rootfs.previous"
printf '%s\n' 'Ominal runtime ready'
