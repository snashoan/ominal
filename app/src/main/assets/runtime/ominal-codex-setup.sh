#!/data/data/com.ominal/files/usr/bin/sh
set -eu

PREFIX="${OMINAL_PREFIX:-/data/data/com.ominal/files/usr}"
RUNNER="${OMINAL_PROOT_RUNNER:-$PREFIX/bin/ominal-proot-run}"

if [ ! -x "$RUNNER" ]; then
    printf 'Ominal PRoot launcher is missing: %s\n' "$RUNNER" >&2
    exit 69
fi

exec "$RUNNER" /bin/bash -lc '
set -eu
export DEBIAN_FRONTEND=noninteractive
if [ ! -x /usr/sbin/policy-rc.d ]; then
    printf "#!/bin/sh\\nexit 101\\n" > /usr/sbin/policy-rc.d
    chmod 755 /usr/sbin/policy-rc.d
fi
sed -i "s|http://ports.ubuntu.com/ubuntu-ports/|https://ports.ubuntu.com/ubuntu-ports/|g" /etc/apt/sources.list.d/ubuntu.sources
apt_bootstrap() {
    apt-get -o Acquire::Retries=5 -o Acquire::ForceIPv4=true -o Acquire::https::Timeout=30 \
        -o Acquire::https::Verify-Peer=false -o Acquire::https::Verify-Host=false "$@"
}
apt_strict() {
    apt-get -o Acquire::Retries=5 -o Acquire::ForceIPv4=true -o Acquire::https::Timeout=30 "$@"
}
apt_bootstrap update
apt_bootstrap install -y debconf
apt_bootstrap install -y ca-certificates
dpkg --configure -a
apt_strict update
apt_strict install -y curl
node --version
npm --version
npm install --global --prefix /root/.ominal/npm "@openai/codex@${OMINAL_CODEX_VERSION:-0.144.1}"
command -v codex
codex --version
'
