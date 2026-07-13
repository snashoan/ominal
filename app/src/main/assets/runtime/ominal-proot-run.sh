#!/data/data/com.ominal/files/usr/bin/sh
set -eu

PREFIX="${OMINAL_PREFIX:-/data/data/com.ominal/files/usr}"
HOME="${OMINAL_HOME:-/data/data/com.ominal/files/home}"
RUNTIME_ROOT="${OMINAL_RUNTIME_ROOT:-$HOME/.ominal/runtime}"
PROOT_BIN="$RUNTIME_ROOT/proot/root/bin/proot"
ROOTFS="$RUNTIME_ROOT/linux/rootfs"
ROOTFS_READY="$ROOTFS/.ominal-rootfs-ready"
WORKSPACE="${OMINAL_WORKDIR:-$HOME/workspace}"
CODEX_HOME="${OMINAL_CODEX_HOME:-$HOME/.ominal/codex}"

if [ ! -x "$PROOT_BIN" ] || [ ! -f "$ROOTFS_READY" ]; then
    printf '%s\n' 'Ominal Linux runtime is not installed yet.' >&2
    exit 69
fi

case "$WORKSPACE" in
    /data/user/0/com.ominal/*)
        WORKSPACE="/data/data/com.ominal${WORKSPACE#/data/user/0/com.ominal}"
        ;;
esac

mkdir -p "$RUNTIME_ROOT/tmp" "$ROOTFS/.l2s" "$WORKSPACE" "$ROOTFS/root/workspace" \
    "$ROOTFS/root/.codex" "$CODEX_HOME"

if [ ! -e "$CODEX_HOME/auth.json" ] && [ ! -e "$CODEX_HOME/config.toml" ]; then
    for legacy_home in \
        "$RUNTIME_ROOT/linux/rootfs.previous/root/.codex" \
        "$ROOTFS/root/.codex"; do
        if [ -d "$legacy_home" ]; then
            /system/bin/cp -R "$legacy_home/." "$CODEX_HOME/" 2>/dev/null || true
        fi
        if [ -e "$CODEX_HOME/auth.json" ] || [ -e "$CODEX_HOME/config.toml" ]; then
            break
        fi
    done
fi

export PREFIX HOME
export PROOT_TMP_DIR="$RUNTIME_ROOT/tmp"
export PROOT_L2S_DIR="$ROOTFS/.l2s"
export PATH=/root/.ominal/npm/bin:/root/.ominal/node/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
export HOME=/root USER=root LOGNAME=root

if [ "$#" -eq 0 ]; then
    set -- /bin/bash
fi

exec "$PROOT_BIN" --link2symlink --tcsetsf2tcsets -0 -r "$ROOTFS" \
    -b /dev -b /proc -b /sys -b "$RUNTIME_ROOT/tmp:/tmp" \
    -b "$CODEX_HOME:/root/.codex" -b "$WORKSPACE:/root/workspace" \
    -w /root/workspace "$@"
