#!/data/data/com.ominal/files/usr/bin/sh
set -eu

PREFIX="${OMINAL_PREFIX:-/data/data/com.ominal/files/usr}"
HOME="${OMINAL_HOME:-/data/data/com.ominal/files/home}"
PATH="$PREFIX/bin:/system/bin"
export PREFIX HOME PATH

DISPLAY_NUM="${OMINAL_DISPLAY:-:20}"
DISPLAY_GEOMETRY="${OMINAL_DISPLAY_GEOMETRY:-540x1096x24}"
DISPLAY_DIR="$HOME/.ominal/display"
DISPLAY_WORKDIR="${OMINAL_WORKDIR:-$HOME/workspace}"
mkdir -p "$DISPLAY_DIR"

GUEST_SCRIPT="$DISPLAY_WORKDIR/.ominal-display-guest.sh"
mkdir -p "$DISPLAY_WORKDIR"
cat > "$GUEST_SCRIPT" <<'OMINAL_GUEST_DISPLAY'
#!/bin/bash
set -eu
display_dir=/root/.ominal/display
mkdir -p "$display_dir" /root/.fluxbox

for command_name in Xvfb x11vnc websockify fluxbox xterm; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    printf "Missing display command: %s\n" "$command_name" >&2
    exit 69
  fi
done

if [ ! -f "$display_dir/geometry" ] || [ "$(cat "$display_dir/geometry" 2>/dev/null || true)" != "$OMINAL_DISPLAY_GEOMETRY" ]; then
  pkill -f "[w]ebsockify.*6080" 2>/dev/null || true
  pkill -f "[x]11vnc.*$OMINAL_DISPLAY" 2>/dev/null || true
  pkill -f "[X]vfb $OMINAL_DISPLAY" 2>/dev/null || true
  pkill -f "[f]luxbox" 2>/dev/null || true
  printf "%s" "$OMINAL_DISPLAY_GEOMETRY" >"$display_dir/geometry"
fi

if ! pgrep -f "[X]vfb $OMINAL_DISPLAY" >/dev/null 2>&1; then
  nohup Xvfb "$OMINAL_DISPLAY" -screen 0 "$OMINAL_DISPLAY_GEOMETRY" -nolisten tcp -nolock -dpi 160 \
    >"$display_dir/xvfb.log" 2>&1 &
fi

export DISPLAY="$OMINAL_DISPLAY"
for attempt in 1 2 3 4 5; do
  [ -S "/tmp/.X11-unix/X${OMINAL_DISPLAY#:}" ] && break
  sleep 0.2
done

cat > /root/.fluxbox/init <<"EOF"
session.screen0.toolbar.visible: false
session.screen0.workspaces: 1
session.screen0.windowPlacement: CenterPlacement
session.screen0.focusModel: ClickFocus
session.screen0.fullMaximization: true
session.screen0.defaultDeco: NONE
EOF

if ! pgrep -f "[f]luxbox" >/dev/null 2>&1; then
  nohup fluxbox >"$display_dir/fluxbox.log" 2>&1 &
fi

if command -v xsetroot >/dev/null 2>&1; then
  xsetroot -solid "#080809" -cursor_name none >/dev/null 2>&1 || true
fi

if ! pgrep -f "[x]11vnc.*$OMINAL_DISPLAY" >/dev/null 2>&1; then
  nohup x11vnc -display "$OMINAL_DISPLAY" -localhost -forever -shared -nopw \
    -noshm -noxdamage -nocursor -rfbport 5900 >"$display_dir/x11vnc.log" 2>&1 &
fi

novnc_web=/usr/share/novnc
[ -d "$novnc_web" ] || novnc_web=/usr/share/noVNC
if [ ! -d "$novnc_web" ]; then
  printf "noVNC web root is missing\n" >&2
  exit 70
fi

if pgrep -f "[w]ebsockify.*6080" >/dev/null 2>&1 && command -v curl >/dev/null 2>&1; then
  if ! curl --fail --silent --max-time 2 http://127.0.0.1:6080/vnc_lite.html >/dev/null 2>&1; then
    pkill -f "[w]ebsockify.*6080" 2>/dev/null || true
  fi
fi

if ! pgrep -f "[w]ebsockify.*6080" >/dev/null 2>&1; then
  nohup websockify --web "$novnc_web" 127.0.0.1:6080 127.0.0.1:5900 \
    >"$display_dir/websockify.log" 2>&1 &
fi

printf "Agent Display ready\n"
OMINAL_GUEST_DISPLAY
chmod 700 "$GUEST_SCRIPT"
export OMINAL_WORKDIR="$DISPLAY_WORKDIR"

nohup "$PREFIX/bin/ominal-proot-run" /usr/bin/env \
  OMINAL_DISPLAY="$DISPLAY_NUM" \
  OMINAL_DISPLAY_GEOMETRY="$DISPLAY_GEOMETRY" \
  OMINAL_WORKDIR="$DISPLAY_WORKDIR" \
  /bin/bash /root/workspace/.ominal-display-guest.sh \
  >"$DISPLAY_DIR/launcher.log" 2>&1 &
printf 'Agent Display starting\n'
