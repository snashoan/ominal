#!/data/data/com.ominal/files/usr/bin/sh
set -eu

PREFIX="${OMINAL_PREFIX:-/data/data/com.ominal/files/usr}"
HOME="${OMINAL_HOME:-/data/data/com.ominal/files/home}"
RUNTIME_ROOT="${OMINAL_RUNTIME_ROOT:-$HOME/.ominal/runtime}"
PROOT_BIN="$RUNTIME_ROOT/proot/root/bin/proot"
ROOTFS="$RUNTIME_ROOT/linux/rootfs"
ROOTFS_READY="$ROOTFS/.ominal-rootfs-ready"
WORKSPACE="${OMINAL_WORKDIR:-$HOME/workspace}"

if [ ! -x "$PROOT_BIN" ] || [ ! -f "$ROOTFS_READY" ]; then
    printf '%s\n' 'Ominal Linux runtime is not installed yet.' >&2
    exit 69
fi

case "$WORKSPACE" in
    /data/user/0/com.ominal/*)
        WORKSPACE="/data/data/com.ominal${WORKSPACE#/data/user/0/com.ominal}"
        ;;
esac

mkdir -p "$RUNTIME_ROOT/tmp" "$ROOTFS/.l2s" "$WORKSPACE" "$ROOTFS/root/workspace"

export PREFIX HOME
export PROOT_TMP_DIR="$RUNTIME_ROOT/tmp"
export PROOT_L2S_DIR="$ROOTFS/.l2s"
export PATH=/root/.ominal/npm/bin:/root/.ominal/node/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

if [ "$#" -eq 0 ]; then
    set -- /bin/bash
fi

exec "$PROOT_BIN" --link2symlink --tcsetsf2tcsets -0 -r "$ROOTFS" \
    -b /dev -b /proc -b /sys -b "$RUNTIME_ROOT/tmp:/tmp" \
    -b "$WORKSPACE:/root/workspace" -w /root/workspace "$@"
