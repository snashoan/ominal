#!/data/data/com.ominal/files/usr/bin/sh
set -eu

PREFIX="${OMINAL_PREFIX:-/data/data/com.ominal/files/usr}"
HOME="${OMINAL_HOME:-/data/data/com.ominal/files/home}"
RUNTIME_ROOT="${OMINAL_RUNTIME_ROOT:-$HOME/.ominal/runtime}"
ARCHIVE_PATH="${1:?missing normalized Ubuntu base archive path}"
RUNTIME_ARCH="${OMINAL_RUNTIME_ARCH:-arm64}"
RUNTIME_VERSION="${OMINAL_ROOTFS_VERSION:-ubuntu-base-24.04.4-${RUNTIME_ARCH}-nohardlinks-v3}"
DNS_SERVERS="${OMINAL_DNS_SERVERS:-1.1.1.1 8.8.8.8}"
PROOT_BIN="$PREFIX/bin/proot"
PROOT_LOADER="${PROOT_LOADER:-$PREFIX/libexec/proot/loader}"
LINUX_ROOT="$RUNTIME_ROOT/linux"
ROOTFS="$LINUX_ROOT/rootfs"
ROOTFS_READY="$ROOTFS/.ominal-rootfs-ready"

if [ ! -x "$PROOT_BIN" ] || [ ! -x "$PROOT_LOADER" ]; then
    printf '%s\n' 'Ominal PRoot runtime is missing.' >&2
    exit 64
fi
if [ ! -f "$ARCHIVE_PATH" ]; then
    printf 'Normalized Ubuntu base archive is missing: %s\n' "$ARCHIVE_PATH" >&2
    exit 66
fi

mkdir -p "$LINUX_ROOT" "$RUNTIME_ROOT/tmp"
export PREFIX HOME PROOT_LOADER PROOT_TMP_DIR="$RUNTIME_ROOT/tmp"

write_resolver() {
    target_rootfs="$1"
    if [ -d "$target_rootfs/etc" ]; then
        : > "$target_rootfs/etc/resolv.conf"
        for dns_server in $DNS_SERVERS; do
            printf 'nameserver %s\n' "$dns_server" >> "$target_rootfs/etc/resolv.conf"
        done
    fi
}

validate_rootfs() {
    target_rootfs="$1"
    (
        # Keep the Android exec bridge for bootstrap tools, but never expose it
        # inside the Linux guest.
        unset LD_PRELOAD
        PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin \
            exec "$PROOT_BIN" -0 -r "$target_rootfs" \
            -b /dev -b /proc -b /sys -b "$RUNTIME_ROOT/tmp:/tmp" -w /root \
            /bin/bash -lc 'test -x /usr/bin/tar && test -x /usr/bin/perl && test -f /etc/os-release'
    )
}

current_version=""
if [ -f "$ROOTFS_READY" ]; then
    current_version="$(cat "$ROOTFS_READY" 2>/dev/null || true)"
fi
if [ "$current_version" != "$RUNTIME_VERSION" ]; then
    STAGING_ROOTFS="$LINUX_ROOT/rootfs.stage.$$"
    mkdir -p "$STAGING_ROOTFS/tmp"
    trap 'rm -rf "$STAGING_ROOTFS"' EXIT HUP INT TERM
    "$PREFIX/bin/tar" -xzf "$ARCHIVE_PATH" -C "$STAGING_ROOTFS"
    write_resolver "$STAGING_ROOTFS"
    validate_rootfs "$STAGING_ROOTFS"
    printf '%s\n' "$RUNTIME_VERSION" > "$STAGING_ROOTFS/.ominal-rootfs-ready"
    if [ -e "$ROOTFS" ]; then
        BACKUP_ROOTFS="$LINUX_ROOT/rootfs.previous"
        rm -rf "$BACKUP_ROOTFS"
        mv "$ROOTFS" "$BACKUP_ROOTFS"
    fi
    mv "$STAGING_ROOTFS" "$ROOTFS"
    trap - EXIT HUP INT TERM
fi
write_resolver "$ROOTFS"
validate_rootfs "$ROOTFS"
