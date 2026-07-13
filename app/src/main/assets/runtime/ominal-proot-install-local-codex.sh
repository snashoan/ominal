#!/data/data/com.ominal/files/usr/bin/sh
set -eu

PREFIX="${OMINAL_PREFIX:-/data/data/com.ominal/files/usr}"
HOME="${OMINAL_HOME:-/data/data/com.ominal/files/home}"
RUNTIME_ROOT="${OMINAL_RUNTIME_ROOT:-$HOME/.ominal/runtime}"
NODE_ARCHIVE="${1:?missing Node arm64 archive}"
CODEX_CORE_ARCHIVE="${2:?missing Codex core npm archive}"
CODEX_PLATFORM_ARCHIVE="${3:?missing Codex Linux arm64 npm archive}"
PROOT_BIN="$RUNTIME_ROOT/proot/root/bin/proot"
ROOTFS="$RUNTIME_ROOT/linux/rootfs"
ROOTFS_READY="$ROOTFS/.ominal-rootfs-ready"
NODE_ROOT="$ROOTFS/root/.ominal/node"

for input in "$NODE_ARCHIVE" "$CODEX_CORE_ARCHIVE" "$CODEX_PLATFORM_ARCHIVE"; do
    if [ ! -f "$input" ]; then
        printf 'Provider artifact is missing: %s\n' "$input" >&2
        exit 66
    fi
done

if [ ! -x "$PROOT_BIN" ] || [ ! -f "$ROOTFS_READY" ]; then
    printf '%s\n' 'Ominal Linux runtime is not installed yet.' >&2
    exit 69
fi

mkdir -p "$ROOTFS/root/.ominal" "$RUNTIME_ROOT/tmp"
export PREFIX HOME
export PROOT_TMP_DIR="$RUNTIME_ROOT/tmp"
export PROOT_L2S_DIR="$ROOTFS/.l2s"

if [ ! -x "$NODE_ROOT/bin/node" ]; then
    if [ -e "$NODE_ROOT" ]; then
        rm -rf "$NODE_ROOT"
    fi

    NODE_STAGE="$ROOTFS/root/.ominal/node.stage.$$"
    mkdir -p "$NODE_STAGE"
    /system/bin/tar -xzf "$NODE_ARCHIVE" -C "$NODE_STAGE"

    NODE_SOURCE=""
    for candidate in "$NODE_STAGE"/*; do
        if [ -d "$candidate" ] && [ -x "$candidate/bin/node" ]; then
            NODE_SOURCE="$candidate"
            break
        fi
    done
    if [ -z "$NODE_SOURCE" ]; then
        printf '%s\n' 'Node archive did not contain an executable Linux Node runtime.' >&2
        exit 65
    fi
    /system/bin/mv "$NODE_SOURCE" "$NODE_ROOT"
    /system/bin/rmdir "$NODE_STAGE" 2>/dev/null || true
fi

PATH=/root/.ominal/npm/bin:/root/.ominal/node/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin \
    "$PROOT_BIN" --link2symlink --tcsetsf2tcsets -0 -r "$ROOTFS" \
        -b /dev -b /proc -b /sys -b "$RUNTIME_ROOT/tmp:/tmp" \
        -b "$CODEX_CORE_ARCHIVE:/mnt/codex-core.tgz" \
        -b "$CODEX_PLATFORM_ARCHIVE:/mnt/codex-platform.tgz" \
        -w /root /bin/bash -lc '
set -eu
export PATH=/root/.ominal/npm/bin:/root/.ominal/node/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
export npm_config_cache=/root/.ominal/npm-cache
node --version
npm --version
npm cache add /mnt/codex-platform.tgz
npm cache add /mnt/codex-core.tgz
npm install --global --prefix /root/.ominal/npm --offline --no-audit --no-fund --install-links=false /mnt/codex-core.tgz
npm install --global --prefix /root/.ominal/npm --offline --no-audit --no-fund --install-links=false --force "@openai/codex-linux-arm64@file:/mnt/codex-platform.tgz"
export PATH=/root/.ominal/npm/bin:/root/.ominal/node/bin:$PATH
command -v codex
codex --version
npm cache clean --force >/dev/null 2>&1 || true
rm -rf /root/.ominal/npm-cache
'
