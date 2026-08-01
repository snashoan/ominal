#!/usr/bin/env bash
set -euo pipefail

if [[ ${EUID:-$(id -u)} -ne 0 ]]; then
    exec sudo -E "$0" "$@"
fi

UBUNTU_VERSION="${UBUNTU_VERSION:-24.04.4}"
NODE_VERSION="${NODE_VERSION:-24.18.0}"
CODEX_VERSION="${CODEX_VERSION:-0.144.6}"
CODEX_CORE_SHA256="${CODEX_CORE_SHA256:-779eab25aa8473583b3d1d6f9316a0ab8d0643fdfd0bfef80ce76cc8cf85e401}"
CODEX_ARM64_SHA256="${CODEX_ARM64_SHA256:-19f0b01b33f273df94191670b2e0e5d0f624b0354e765bfdea5763920b713800}"
MOZILLA_REPO_KEY_SHA256="${MOZILLA_REPO_KEY_SHA256:-3ecc63922b7795eb23fdc449ff9396f9114cb3cf186d6f5b53ad4cc3ebfbb11f}"
OUTPUT="$(realpath -m "${1:-build/runtime/ominal-ubuntu-24.04.4-arm64-prepared-v3.tgz}")"
WORK="$(mktemp -d)"
ROOTFS="$WORK/rootfs"
MOUNTS=()

cleanup() {
    for ((index=${#MOUNTS[@]} - 1; index >= 0; index--)); do
        umount -l "${MOUNTS[$index]}" 2>/dev/null || true
    done
    rm -rf "$WORK"
}
trap cleanup EXIT HUP INT TERM

download() {
    local url="$1"
    local target="$2"
    curl --fail --location --retry 5 --retry-all-errors --output "$target" "$url"
}

verify_sha256() {
    local expected="$1"
    local file="$2"
    printf '%s  %s\n' "$expected" "$file" | sha256sum --check --status
}

ubuntu_archive="ubuntu-base-${UBUNTU_VERSION}-base-arm64.tar.gz"
ubuntu_base_url="https://cdimage.ubuntu.com/ubuntu-base/releases/${UBUNTU_VERSION}/release"
download "$ubuntu_base_url/$ubuntu_archive" "$WORK/$ubuntu_archive"
download "$ubuntu_base_url/SHA256SUMS" "$WORK/ubuntu-sha256sums"
ubuntu_sha256="$(awk -v name="$ubuntu_archive" '$2 == name || $2 == "*" name { print $1; exit }' "$WORK/ubuntu-sha256sums")"
[[ "$ubuntu_sha256" =~ ^[0-9a-f]{64}$ ]]
verify_sha256 "$ubuntu_sha256" "$WORK/$ubuntu_archive"

node_archive="node-v${NODE_VERSION}-linux-arm64.tar.gz"
node_base_url="https://nodejs.org/dist/v${NODE_VERSION}"
download "$node_base_url/$node_archive" "$WORK/$node_archive"
download "$node_base_url/SHASUMS256.txt" "$WORK/node-sha256sums"
node_sha256="$(awk -v name="$node_archive" '$2 == name { print $1; exit }' "$WORK/node-sha256sums")"
[[ "$node_sha256" =~ ^[0-9a-f]{64}$ ]]
verify_sha256 "$node_sha256" "$WORK/$node_archive"

codex_core="codex-${CODEX_VERSION}.tgz"
codex_arm64="codex-${CODEX_VERSION}-linux-arm64.tgz"
download "https://registry.npmjs.org/@openai/codex/-/$codex_core" "$WORK/$codex_core"
download "https://registry.npmjs.org/@openai/codex/-/$codex_arm64" "$WORK/$codex_arm64"
verify_sha256 "$CODEX_CORE_SHA256" "$WORK/$codex_core"
verify_sha256 "$CODEX_ARM64_SHA256" "$WORK/$codex_arm64"
download "https://packages.mozilla.org/apt/repo-signing-key.gpg" "$WORK/mozilla-repo-signing-key.gpg"
verify_sha256 "$MOZILLA_REPO_KEY_SHA256" "$WORK/mozilla-repo-signing-key.gpg"

mkdir -p "$ROOTFS"
tar -xpf "$WORK/$ubuntu_archive" -C "$ROOTFS"
cp /usr/bin/qemu-aarch64-static "$ROOTFS/usr/bin/qemu-aarch64-static"
printf '#!/bin/sh\nexit 101\n' > "$ROOTFS/usr/sbin/policy-rc.d"
chmod 755 "$ROOTFS/usr/sbin/policy-rc.d"
cp /etc/resolv.conf "$ROOTFS/etc/resolv.conf"
install -d -m 0755 "$ROOTFS/etc/apt/keyrings"
install -m 0644 "$WORK/mozilla-repo-signing-key.gpg" \
    "$ROOTFS/etc/apt/keyrings/packages.mozilla.org.asc"

mount --bind /dev "$ROOTFS/dev"
MOUNTS+=("$ROOTFS/dev")
mount -t proc proc "$ROOTFS/proc"
MOUNTS+=("$ROOTFS/proc")
mount -t sysfs sysfs "$ROOTFS/sys"
MOUNTS+=("$ROOTFS/sys")

chroot "$ROOTFS" /usr/bin/qemu-aarch64-static /bin/bash -lc '
set -euo pipefail
export DEBIAN_FRONTEND=noninteractive TZ=Etc/UTC
apt-get -o Acquire::Retries=5 -o Dpkg::Use-Pty=0 update
dpkg --configure -a
apt-get -o Acquire::Retries=5 -o Dpkg::Use-Pty=0 -f install -y
apt-get -o Acquire::Retries=5 -o Dpkg::Use-Pty=0 full-upgrade -y
apt-get -o Acquire::Retries=5 -o Dpkg::Use-Pty=0 install -y \
    --no-install-recommends ca-certificates wget gnupg
printf "%s\n" \
    "deb [signed-by=/etc/apt/keyrings/packages.mozilla.org.asc] https://packages.mozilla.org/apt mozilla main" \
    > /etc/apt/sources.list.d/mozilla.list
printf "%s\n" "Package: *" "Pin: origin packages.mozilla.org" "Pin-Priority: 1000" \
    > /etc/apt/preferences.d/mozilla
apt-get -o Acquire::Retries=5 -o Dpkg::Use-Pty=0 update
apt-get -o Acquire::Retries=5 -o Dpkg::Use-Pty=0 install -y \
    --no-install-recommends --no-install-suggests \
    ca-certificates git xvfb x11vnc novnc websockify jwm xterm python3 xfe pcmanfm firefox \
    xfce4-settings xfce4-session xfwm4 xfce4-panel xfdesktop4 thunar xfce4-terminal \
    mousepad xfce4-appfinder xfce4-notifyd tumbler gvfs devilspie2 unclutter-xfixes \
    dbus-x11 adwaita-icon-theme fontconfig fonts-dejavu-core fonts-noto-core \
    libnss3 libnspr4 xdotool wmctrl scrot \
    x11-utils x11-xserver-utils
  for command_name in Xvfb x11vnc websockify jwm xterm pcmanfm xfwrite firefox xfce4-settings-manager xfce4-session xfwm4 xfce4-panel xfdesktop thunar xfce4-terminal mousepad devilspie2 unclutter-xfixes dbus-run-session xdotool wmctrl scrot xdpyinfo xrdb; do
    command -v "$command_name" >/dev/null
done
test -d /usr/share/novnc || test -d /usr/share/noVNC
test -z "$(dpkg --audit)"
apt-get check
'

node_root="$ROOTFS/root/.ominal/node"
codex_root="$ROOTFS/root/.ominal/npm"
mkdir -p "$node_root" \
    "$codex_root/bin" \
    "$codex_root/lib/node_modules/@openai/codex" \
    "$codex_root/lib/node_modules/@openai/codex-linux-arm64"
tar -xzf "$WORK/$node_archive" --strip-components 1 -C "$node_root"
tar -xzf "$WORK/$codex_core" --strip-components 1 \
    -C "$codex_root/lib/node_modules/@openai/codex"
tar -xzf "$WORK/$codex_arm64" --strip-components 1 \
    -C "$codex_root/lib/node_modules/@openai/codex-linux-arm64"
ln -s ../lib/node_modules/@openai/codex/bin/codex.js "$codex_root/bin/codex"
test -x "$node_root/bin/node"
test -x "$codex_root/lib/node_modules/@openai/codex-linux-arm64/vendor/aarch64-unknown-linux-musl/bin/codex"

mkdir -p "$ROOTFS/var/lib/ominal"
touch "$ROOTFS/var/lib/ominal/base-upgrade-noble-v1" \
    "$ROOTFS/var/lib/ominal/base-upgrade-noble-v2" \
    "$ROOTFS/var/lib/ominal/base-upgrade-noble-v3" \
    "$ROOTFS/var/lib/ominal/base-upgrade-noble-v4" \
    "$ROOTFS/var/lib/ominal/base-upgrade-noble-v5"

chroot "$ROOTFS" /usr/bin/qemu-aarch64-static /bin/bash -lc '
set -euo pipefail
export PATH=/root/.ominal/npm/bin:/root/.ominal/node/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
test "$(node --version)" = "v24.18.0"
codex --version | grep -F "codex-cli 0.144.6" >/dev/null
test -z "$(dpkg --audit)"
apt-get check >/dev/null
'

for ((index=${#MOUNTS[@]} - 1; index >= 0; index--)); do
    umount "${MOUNTS[$index]}"
done
MOUNTS=()

rm -f "$ROOTFS/usr/bin/qemu-aarch64-static" \
    "$ROOTFS/etc/machine-id" \
    "$ROOTFS/root/.bash_history"
rm -rf "$ROOTFS/root/.codex" \
    "$ROOTFS/root/workspace" \
    "$ROOTFS/root/.cache" \
    "$ROOTFS/root/.npm" \
    "$ROOTFS/tmp"/* \
    "$ROOTFS/var/tmp"/* \
    "$ROOTFS/var/cache/apt/archives"/* \
    "$ROOTFS/var/lib/apt/lists"/* \
    "$ROOTFS/var/log"/*
mkdir -p "$ROOTFS/root/workspace" "$ROOTFS/tmp" "$ROOTFS/var/tmp"
chmod 1777 "$ROOTFS/tmp" "$ROOTFS/var/tmp"

if find "$ROOTFS" -type f -name auth.json -print -quit | grep -q .; then
    printf '%s\n' 'Refusing to package a rootfs containing auth.json' >&2
    exit 70
fi

mkdir -p "$(dirname "$OUTPUT")"
tar --hard-dereference --numeric-owner --owner=0 --group=0 \
    -C "$ROOTFS" -I 'gzip -9' -cf "$OUTPUT" .
sha256sum "$OUTPUT" | tee "$OUTPUT.sha256"
