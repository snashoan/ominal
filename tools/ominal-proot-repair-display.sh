#!/bin/bash
set -eu

export DEBIAN_FRONTEND=noninteractive
printf '#!/bin/sh\nexit 101\n' >/usr/sbin/policy-rc.d
chmod 755 /usr/sbin/policy-rc.d

dpkg --configure -a
apt-get -f install -y
dpkg --audit

for command_name in Xvfb x11vnc websockify fluxbox xterm; do
    command -v "$command_name"
done
