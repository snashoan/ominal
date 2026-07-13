#!/data/data/com.ominal/files/usr/bin/sh
set -eu

PREFIX="${OMINAL_PREFIX:-/data/data/com.ominal/files/usr}"
HOME="${OMINAL_HOME:-/data/data/com.ominal/files/home}"
RUNTIME_ROOT="${OMINAL_RUNTIME_ROOT:-$HOME/.ominal/runtime}"
RUNTIME_VERSION="ominal-ubuntu-24.04.4-node-24.15.0-codex-0.144.1-display-v2"
READY_FILE="$RUNTIME_ROOT/.ominal-runtime-ready"

verify_runtime() {
    "$PREFIX/bin/ominal-proot-run" /bin/bash -lc '
set -eu
test "$(dpkg --print-architecture)" = "arm64"
test -z "$(dpkg --audit)"
test "$(node --version)" = "v24.15.0"
test "$(command -v codex)" = "/root/.ominal/npm/bin/codex"
codex --version | grep -F "codex-cli 0.144.1" >/dev/null
for command_name in Xvfb x11vnc websockify fluxbox xterm; do command -v "$command_name" >/dev/null; done
test -d /usr/share/novnc || test -d /usr/share/noVNC
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

PROOT_ARCHIVE="${1:?missing PRoot archive}"
ROOTFS_ARCHIVE="${2:?missing Ubuntu rootfs archive}"
NODE_ARCHIVE="${3:?missing Node archive}"
CODEX_CORE_ARCHIVE="${4:?missing Codex core archive}"
CODEX_PLATFORM_ARCHIVE="${5:?missing Codex arm64 archive}"

rm -f "$READY_FILE" "$READY_FILE.tmp"
"$PREFIX/bin/ominal-runtime-install-proot" "$PROOT_ARCHIVE"
"$PREFIX/bin/ominal-runtime-install-ubuntu-base" "$ROOTFS_ARCHIVE"
"$PREFIX/bin/ominal-proot-install-local-codex" \
    "$NODE_ARCHIVE" "$CODEX_CORE_ARCHIVE" "$CODEX_PLATFORM_ARCHIVE"
"$PREFIX/bin/ominal-install-display-packages"
verify_runtime
mark_ready
rm -rf "$RUNTIME_ROOT/linux/rootfs.previous"
printf '%s\n' 'Ominal runtime ready'
