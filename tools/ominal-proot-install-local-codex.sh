#!/data/data/com.ominal/files/usr/bin/sh
set -eu

PREFIX="${OMINAL_PREFIX:-/data/data/com.ominal/files/usr}"
HOME="${OMINAL_HOME:-/data/data/com.ominal/files/home}"
RUNTIME_ROOT="${OMINAL_RUNTIME_ROOT:-$HOME/.ominal/runtime}"
NODE_ARCHIVE="${1:?missing Node arm64 archive}"
CODEX_CORE_ARCHIVE="${2:?missing Codex core npm archive}"
CODEX_PLATFORM_ARCHIVE="${3:?missing Codex Linux arm64 npm archive}"
PROOT_BIN="$PREFIX/bin/proot"
PROOT_LOADER="${PROOT_LOADER:-$PREFIX/libexec/proot/loader}"
ROOTFS="$RUNTIME_ROOT/linux/rootfs"
ROOTFS_READY="$ROOTFS/.ominal-rootfs-ready"
NODE_ROOT="$ROOTFS/root/.ominal/node"

for input in "$NODE_ARCHIVE" "$CODEX_CORE_ARCHIVE" "$CODEX_PLATFORM_ARCHIVE"; do
    if [ ! -f "$input" ]; then
        printf 'Provider artifact is missing: %s\n' "$input" >&2
        exit 66
    fi
done

if [ ! -x "$PROOT_BIN" ] || [ ! -x "$PROOT_LOADER" ] || [ ! -f "$ROOTFS_READY" ]; then
    printf '%s\n' 'Ominal Linux runtime is not installed yet.' >&2
    exit 69
fi

mkdir -p "$ROOTFS/root/.ominal" "$RUNTIME_ROOT/tmp"
export PREFIX HOME PROOT_LOADER
export PROOT_TMP_DIR="$RUNTIME_ROOT/tmp"
export PROOT_L2S_DIR="$ROOTFS/.l2s"

if [ ! -x "$NODE_ROOT/bin/node" ]; then
    if [ -e "$NODE_ROOT" ]; then
        rm -rf "$NODE_ROOT"
    fi

    NODE_STAGE="$ROOTFS/root/.ominal/node.stage.$$"
    mkdir -p "$NODE_STAGE"
    "$PREFIX/bin/tar" -xzf "$NODE_ARCHIVE" -C "$NODE_STAGE"

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

INSTALL_ROOT="$ROOTFS/root/.ominal/npm"
INSTALL_STAGE="$ROOTFS/root/.ominal/npm.stage.$$"
INSTALL_PREVIOUS="$ROOTFS/root/.ominal/npm.previous"
CORE_PACKAGE="$INSTALL_STAGE/lib/node_modules/@openai/codex"
PLATFORM_PACKAGE="$INSTALL_STAGE/lib/node_modules/@openai/codex-linux-arm64"

rm -rf "$INSTALL_STAGE" "$INSTALL_PREVIOUS"
mkdir -p "$CORE_PACKAGE" "$PLATFORM_PACKAGE" "$INSTALL_STAGE/bin"
"$PREFIX/bin/tar" -xzf "$CODEX_CORE_ARCHIVE" --strip-components 1 -C "$CORE_PACKAGE"
"$PREFIX/bin/tar" -xzf "$CODEX_PLATFORM_ARCHIVE" --strip-components 1 -C "$PLATFORM_PACKAGE"
test -f "$CORE_PACKAGE/bin/codex.js"
test -x "$PLATFORM_PACKAGE/vendor/aarch64-unknown-linux-musl/bin/codex"
ln -s ../lib/node_modules/@openai/codex/bin/codex.js "$INSTALL_STAGE/bin/codex"

if [ -e "$INSTALL_ROOT" ]; then
    /system/bin/mv "$INSTALL_ROOT" "$INSTALL_PREVIOUS"
fi
/system/bin/mv "$INSTALL_STAGE" "$INSTALL_ROOT"
rm -rf "$INSTALL_PREVIOUS" "$ROOTFS/root/.ominal/npm-cache"

unset LD_PRELOAD
PATH=/root/.ominal/npm/bin:/root/.ominal/node/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin \
    exec "$PROOT_BIN" --link2symlink -0 -r "$ROOTFS" \
    -b /dev -b /proc -b /sys -b "$RUNTIME_ROOT/tmp:/tmp" \
    -w /root /bin/bash -lc '
set -eu
export PATH=/root/.ominal/npm/bin:/root/.ominal/node/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
node --version
command -v codex
codex --version
'
