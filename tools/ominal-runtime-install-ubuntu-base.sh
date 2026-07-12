#!/data/data/com.ominal/files/usr/bin/sh
set -eu

PREFIX="${OMINAL_PREFIX:-/data/data/com.ominal/files/usr}"
HOME="${OMINAL_HOME:-/data/data/com.ominal/files/home}"
RUNTIME_ROOT="${OMINAL_RUNTIME_ROOT:-$HOME/.ominal/runtime}"
ARCHIVE_PATH="${1:?missing normalized Ubuntu base archive path}"
RUNTIME_VERSION="${OMINAL_RUNTIME_VERSION:-ubuntu-base-24.04.4-arm64-nohardlinks-v1}"
DNS_SERVERS="${OMINAL_DNS_SERVERS:-1.1.1.1 8.8.8.8}"
PROOT_BIN="$RUNTIME_ROOT/proot/root/bin/proot"
LINUX_ROOT="$RUNTIME_ROOT/linux"
ROOTFS="$LINUX_ROOT/rootfs"
ROOTFS_READY="$ROOTFS/.ominal-rootfs-ready"

if [ ! -x "$PROOT_BIN" ]; then
    printf '%s\n' 'Ominal PRoot runtime is missing.' >&2
    exit 64
fi

if [ ! -f "$ARCHIVE_PATH" ]; then
    printf 'Normalized Ubuntu base archive is missing: %s\n' "$ARCHIVE_PATH" >&2
    exit 66
fi

mkdir -p "$LINUX_ROOT" "$RUNTIME_ROOT/tmp"
export PREFIX HOME
export PROOT_TMP_DIR="$RUNTIME_ROOT/tmp"

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
    PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin "$PROOT_BIN" -0 -r "$target_rootfs" \
        -b /dev -b /proc -b /sys -b "$RUNTIME_ROOT/tmp:/tmp" -w /root \
        /bin/bash -lc 'test -x /usr/bin/tar && test -x /usr/bin/perl && test -f /etc/os-release && /usr/bin/perl -e "print qq(Ominal Perl ready\\n)" && . /etc/os-release && printf "Ominal Linux runtime ready: %s %s\\n" "$NAME" "$VERSION_ID"'
}

current_version=""
if [ -f "$ROOTFS_READY" ]; then
    current_version="$(cat "$ROOTFS_READY" 2>/dev/null || true)"
fi

if [ "$current_version" != "$RUNTIME_VERSION" ]; then
    STAGING_ROOTFS="$LINUX_ROOT/rootfs.stage.$$"
    mkdir -p "$STAGING_ROOTFS/tmp"

    /system/bin/tar -xzf "$ARCHIVE_PATH" -C "$STAGING_ROOTFS"
    write_resolver "$STAGING_ROOTFS"
    validate_rootfs "$STAGING_ROOTFS"
    printf '%s\n' "$RUNTIME_VERSION" > "$STAGING_ROOTFS/.ominal-rootfs-ready"

    if [ -e "$ROOTFS" ]; then
        BACKUP_ROOTFS="$LINUX_ROOT/rootfs.previous.$(/system/bin/date +%s)"
        mv "$ROOTFS" "$BACKUP_ROOTFS"
    fi
    mv "$STAGING_ROOTFS" "$ROOTFS"
fi

write_resolver "$ROOTFS"
validate_rootfs "$ROOTFS"
