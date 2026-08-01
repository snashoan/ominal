#!/bin/bash
set -euo pipefail

export DEBIAN_FRONTEND=noninteractive
state_dir=/var/lib/ominal
refresh_stamp="$state_dir/apt-refreshed"
mkdir -p "$state_dir"

refresh_indexes() {
    if [ ! -f "$refresh_stamp" ] || ! find "$refresh_stamp" -mmin -1440 -print -quit | grep -q .; then
        apt-get -o Acquire::Retries=5 -o Acquire::ForceIPv4=true -o Dpkg::Use-Pty=0 update
        touch "$refresh_stamp"
    fi
}

repair_packages() {
    dpkg --configure -a
    apt-get -o Acquire::Retries=5 -o Dpkg::Use-Pty=0 -f install -y
}

if [ "${1:-}" = "--upgrade" ]; then
    shift
    refresh_indexes
    repair_packages
    apt-get -o Acquire::Retries=5 -o Dpkg::Use-Pty=0 full-upgrade -y
    apt-get clean
    exit 0
fi

if [ "$#" -eq 0 ]; then
    printf '%s\n' \
        'usage: ominal-install PACKAGE...' \
        '       ominal-install /path/to/package.deb' \
        '       ominal-install --upgrade' >&2
    exit 64
fi

host_arch="$(dpkg --print-architecture)"
for candidate in "$@"; do
    case "$candidate" in
        *.deb)
            [ -f "$candidate" ] || { printf 'Package file not found: %s\n' "$candidate" >&2; exit 66; }
            package_arch="$(dpkg-deb -f "$candidate" Architecture)"
            if [ "$package_arch" != all ] && [ "$package_arch" != "$host_arch" ]; then
                printf 'Package architecture %s is incompatible with this %s runtime: %s\n' \
                    "$package_arch" "$host_arch" "$candidate" >&2
                exit 65
            fi
            ;;
    esac
done

refresh_indexes
repair_packages
apt-get -o Acquire::Retries=5 -o Dpkg::Use-Pty=0 install -y -- "$@"
apt-get clean
