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

host_session_version=ominal-display-host-v4
host_session_marker="$DISPLAY_DIR/session-version"
reset_host_session=0
if [ "$(cat "$host_session_marker" 2>/dev/null || true)" != "$host_session_version" ]; then
  reset_host_session=1
elif "$PREFIX/bin/pgrep" -x fluxbox >/dev/null 2>&1; then
  reset_host_session=1
elif ! "$PREFIX/bin/pgrep" -x jwm >/dev/null 2>&1; then
  reset_host_session=1
elif ! "$PREFIX/bin/pgrep" -x Xvfb >/dev/null 2>&1; then
  reset_host_session=1
elif ! "$PREFIX/bin/pgrep" -x x11vnc >/dev/null 2>&1; then
  reset_host_session=1
elif ! "$PREFIX/bin/pgrep" -f "[w]ebsockify.*6080" >/dev/null 2>&1; then
  reset_host_session=1
fi

if [ "$reset_host_session" -eq 1 ]; then
  display_tracer_pids="$("$PREFIX/bin/pgrep" -f "[p]root.*ominal-display-guest.sh" 2>/dev/null || true)"
  for pid in $display_tracer_pids; do
    "$PREFIX/bin/kill" -9 "$pid" 2>/dev/null || true
  done
  sleep 0.1
  "$PREFIX/bin/pkill" -f "[w]ebsockify.*6080" 2>/dev/null || true
  "$PREFIX/bin/pkill" -x x11vnc 2>/dev/null || true
  "$PREFIX/bin/pkill" -x Xvfb 2>/dev/null || true
  "$PREFIX/bin/pkill" -x fluxbox 2>/dev/null || true
  "$PREFIX/bin/pkill" -x tint2 2>/dev/null || true
  "$PREFIX/bin/pkill" -x pcmanfm 2>/dev/null || true
  "$PREFIX/bin/pkill" -x jwm 2>/dev/null || true
  "$PREFIX/bin/pkill" -x xterm 2>/dev/null || true
  "$PREFIX/bin/pkill" -x xfe 2>/dev/null || true
  "$PREFIX/bin/pkill" -x xfwrite 2>/dev/null || true
  for attempt in 1 2 3 4 5 6 7 8 9 10; do
    if ! "$PREFIX/bin/pgrep" -x Xvfb >/dev/null 2>&1 \
        && ! "$PREFIX/bin/pgrep" -x x11vnc >/dev/null 2>&1 \
        && ! "$PREFIX/bin/pgrep" -x fluxbox >/dev/null 2>&1 \
        && ! "$PREFIX/bin/pgrep" -f "[w]ebsockify.*6080" >/dev/null 2>&1; then
      break
    fi
    sleep 0.2
  done
  "$PREFIX/bin/pkill" -9 -f "[w]ebsockify.*6080" 2>/dev/null || true
  "$PREFIX/bin/pkill" -9 -x x11vnc 2>/dev/null || true
  "$PREFIX/bin/pkill" -9 -x Xvfb 2>/dev/null || true
  "$PREFIX/bin/pkill" -9 -x fluxbox 2>/dev/null || true
  printf "%s" "$host_session_version" >"$host_session_marker"
  sleep 0.2
fi

GUEST_SCRIPT="$DISPLAY_WORKDIR/.ominal-display-guest.sh"
mkdir -p "$DISPLAY_WORKDIR"
cat > "$GUEST_SCRIPT" <<'OMINAL_GUEST_DISPLAY'
#!/bin/bash
set -eu
display_dir=/root/.ominal/display
desktop_version=ominal-mobile-v3
mkdir -p "$display_dir" /root/.local/bin

for command_name in Xvfb x11vnc websockify jwm xterm xfe xfwrite xdotool wmctrl scrot ominal-screen; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    printf "Missing screen command: %s\n" "$command_name" >&2
    exit 69
  fi
done

if [ ! -f "$display_dir/geometry" ] \
    || [ "$(cat "$display_dir/geometry" 2>/dev/null || true)" != "$OMINAL_DISPLAY_GEOMETRY" ] \
    || [ "$(cat "$display_dir/version" 2>/dev/null || true)" != "$desktop_version" ]; then
  pkill -f "[w]ebsockify.*6080" 2>/dev/null || true
  pkill -f "[x]11vnc.*$OMINAL_DISPLAY" 2>/dev/null || true
  pkill -f "[X]vfb $OMINAL_DISPLAY" 2>/dev/null || true
  pkill -x jwm 2>/dev/null || true
  pkill -x xterm 2>/dev/null || true
  pkill -x xfe 2>/dev/null || true
  pkill -x xfwrite 2>/dev/null || true
  pkill -x fluxbox 2>/dev/null || true
  pkill -x tint2 2>/dev/null || true
  pkill -x pcmanfm 2>/dev/null || true
  printf "%s" "$OMINAL_DISPLAY_GEOMETRY" >"$display_dir/geometry"
  printf "%s" "$desktop_version" >"$display_dir/version"
fi

if ! pgrep -f "[X]vfb $OMINAL_DISPLAY" >/dev/null 2>&1; then
  rm -f "/tmp/.X11-unix/X${OMINAL_DISPLAY#:}" "/tmp/.X${OMINAL_DISPLAY#:}-lock"
  nohup Xvfb "$OMINAL_DISPLAY" -screen 0 "$OMINAL_DISPLAY_GEOMETRY" -nolisten tcp -nolock -dpi 160 \
    >"$display_dir/xvfb.log" 2>&1 &
fi

export DISPLAY="$OMINAL_DISPLAY"
for attempt in 1 2 3 4 5; do
  [ -S "/tmp/.X11-unix/X${OMINAL_DISPLAY#:}" ] && break
  sleep 0.2
done

cat > /root/.local/bin/ominal-terminal <<"EOF"
#!/bin/sh
cd "${OMINAL_WORKDIR:-/root/workspace}" 2>/dev/null || cd /root
exec xterm -fa Monospace -fs 13 -bg '#000000' -fg '#f4f5f7' -cr '#ffffff' \
  -sb -rightbar -T Terminal -e bash --noprofile --norc -i
EOF

cat > /root/.local/bin/ominal-files <<"EOF"
#!/bin/sh
workspace="${OMINAL_WORKDIR:-/root/workspace}"
mkdir -p /tmp/ominal-user
chown nobody:nogroup /tmp/ominal-user 2>/dev/null || true
exec su -s /bin/sh nobody -c 'HOME=/tmp/ominal-user DISPLAY="$DISPLAY" exec xfe -m -p 1 "$1"' sh "$workspace"
EOF

cat > /root/.local/bin/ominal-editor <<"EOF"
#!/bin/sh
cd "${OMINAL_WORKDIR:-/root/workspace}" 2>/dev/null || cd /root
exec xfwrite
EOF
chmod 755 /root/.local/bin/ominal-terminal /root/.local/bin/ominal-files /root/.local/bin/ominal-editor

fallback_icon=/usr/share/pixmaps/xterm-color_48x48.xpm
files_icon="$(find /usr/share/icons /usr/share/pixmaps -type f \( -iname 'xfe*.png' -o -iname 'folder*.png' \) 2>/dev/null | head -n 1)"
terminal_icon="$(find /usr/share/icons /usr/share/pixmaps -type f \( -iname 'xterm-color.png' -o -iname 'utilities-terminal*.png' \) 2>/dev/null | head -n 1)"
editor_icon="$(find /usr/share/icons /usr/share/pixmaps -type f \( -iname 'xfwrite*.png' -o -iname 'accessories-text-editor*.png' \) 2>/dev/null | head -n 1)"
[ -n "$files_icon" ] || files_icon="$fallback_icon"
[ -n "$terminal_icon" ] || terminal_icon="$fallback_icon"
[ -n "$editor_icon" ] || editor_icon="$fallback_icon"

cat > /root/.jwmrc <<EOF
<?xml version="1.0"?>
<JWM>
  <RootMenu onroot="1" height="52" labeled="false">
    <Program label="Files">/root/.local/bin/ominal-files</Program>
    <Program label="Terminal">/root/.local/bin/ominal-terminal</Program>
    <Program label="Editor">/root/.local/bin/ominal-editor</Program>
  </RootMenu>

  <Group>
    <Option>maximized</Option>
    <Option>notitle</Option>
    <Option>noborder</Option>
  </Group>
  <Group>
    <Class>XTerm</Class>
    <Option>maximized</Option>
  </Group>
  <Group>
    <Class>Xfe</Class>
    <Option>maximized</Option>
  </Group>
  <Group>
    <Class>Xfwrite</Class>
    <Option>maximized</Option>
  </Group>

  <Tray x="0" y="0" width="0" height="44" autohide="off">
    <TrayButton label="Ominal">root:1</TrayButton>
    <TaskList maxwidth="46" labeled="false" height="38"/>
    <Clock format="%H:%M">showdesktop</Clock>
  </Tray>

  <Tray x="0" y="-1" width="0" height="76" autohide="off">
    <TrayButton label="Files" icon="$files_icon">exec:/root/.local/bin/ominal-files</TrayButton>
    <TrayButton label="Terminal" icon="$terminal_icon">exec:/root/.local/bin/ominal-terminal</TrayButton>
    <TrayButton label="Editor" icon="$editor_icon">exec:/root/.local/bin/ominal-editor</TrayButton>
  </Tray>

  <WindowStyle decorations="flat">
    <Font>Sans-13</Font>
    <Width>0</Width>
    <Height>0</Height>
    <Corner>0</Corner>
    <Foreground>#F4F5F7</Foreground>
    <Background>#000000</Background>
    <Outline>#000000</Outline>
    <Active>
      <Foreground>#F4F5F7</Foreground>
      <Background>#000000</Background>
      <Outline>#000000</Outline>
    </Active>
  </WindowStyle>

  <TrayStyle>
    <Font>Sans-12</Font>
    <Foreground>#F4F5F7</Foreground>
    <Background>#000000</Background>
    <Outline>#2C2C2E</Outline>
  </TrayStyle>
  <TaskListStyle>
    <Font>Sans-12</Font>
    <Foreground>#F4F5F7</Foreground>
    <Background>#000000</Background>
    <Active>
      <Foreground>#FFFFFF</Foreground>
      <Background>#1F1F21</Background>
    </Active>
  </TaskListStyle>
  <TrayButtonStyle>
    <Font>Sans-11</Font>
    <Foreground>#F4F5F7</Foreground>
    <Background>#000000</Background>
    <Active>
      <Foreground>#FFFFFF</Foreground>
      <Background>#1F1F21</Background>
    </Active>
  </TrayButtonStyle>
  <MenuStyle>
    <Font>Sans-12</Font>
    <Foreground>#F4F5F7</Foreground>
    <Background>#000000</Background>
    <Active>
      <Foreground>#FFFFFF</Foreground>
      <Background>#1F1F21</Background>
    </Active>
    <Outline>#2C2C2E</Outline>
  </MenuStyle>
  <PopupStyle>
    <Font>Sans-11</Font>
    <Foreground>#F4F5F7</Foreground>
    <Background>#000000</Background>
    <Outline>#2C2C2E</Outline>
  </PopupStyle>

  <Desktops width="1" height="1">
    <Background type="solid">#000000</Background>
  </Desktops>
  <FocusModel>click</FocusModel>
  <MoveMode coordinates="off">opaque</MoveMode>
  <ResizeMode coordinates="off">opaque</ResizeMode>
  <SnapMode distance="8">border</SnapMode>
  <DoubleClickSpeed>400</DoubleClickSpeed>
  <Key mask="A" key="F4">close</Key>
</JWM>
EOF

if command -v xsetroot >/dev/null 2>&1; then
  xsetroot -solid "#000000" -cursor_name none >/dev/null 2>&1 || true
fi

if ! pgrep -x jwm >/dev/null 2>&1; then
  if ! jwm -p >"$display_dir/jwm-parse.log" 2>&1; then
    cat "$display_dir/jwm-parse.log" >&2
    exit 71
  fi
  nohup jwm >"$display_dir/jwm.log" 2>&1 &
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

cat >"$novnc_web/ominal.html" <<'OMINAL_NOVNC'
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
  <title>Ominal screen</title>
  <style>
    html, body, #screen {
      width: 100%;
      height: 100%;
      margin: 0;
      overflow: hidden;
      background: #000;
      cursor: none;
      touch-action: none;
    }
    #screen { position: fixed; inset: 0; }
    canvas {
      display: block !important;
      width: 100% !important;
      height: 100% !important;
      max-width: none !important;
      max-height: none !important;
      object-fit: fill !important;
      cursor: none !important;
    }
  </style>
  <script type="module">
    import RFB from './core/rfb.js';

    const report = (state, detail = '') => {
      if (window.OminalDisplay && window.OminalDisplay.state) {
        window.OminalDisplay.state(state, detail);
      }
    };

    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
    const url = `${protocol}://${window.location.host}/websockify`;
    let reconnectTimer;

    const connect = () => {
      report('connecting', 'Connecting');
      try {
        const rfb = new RFB(document.getElementById('screen'), url);
        rfb.viewOnly = false;
        rfb.scaleViewport = true;
        rfb.resizeSession = false;
        rfb.showDotCursor = false;
        rfb.qualityLevel = 6;
        rfb.compressionLevel = 6;
        rfb.addEventListener('connect', () => {
          window.clearTimeout(reconnectTimer);
          report('connected');
        });
        rfb.addEventListener('securityfailure', () => {
          report('error', 'Screen connection failed');
        });
        rfb.addEventListener('disconnect', (event) => {
          if (!event.detail.clean) report('error', 'Reconnecting screen...');
          reconnectTimer = window.setTimeout(() => window.location.reload(), 800);
        });
      } catch (error) {
        report('error', 'Screen connection failed');
        reconnectTimer = window.setTimeout(() => window.location.reload(), 800);
      }
    };

    connect();
  </script>
</head>
<body><div id="screen" aria-label="Linux screen"></div></body>
</html>
OMINAL_NOVNC

if pgrep -f "[w]ebsockify.*6080" >/dev/null 2>&1 && command -v curl >/dev/null 2>&1; then
  if ! curl --fail --silent --max-time 2 http://127.0.0.1:6080/ominal.html >/dev/null 2>&1; then
    pkill -f "[w]ebsockify.*6080" 2>/dev/null || true
  fi
fi

if ! pgrep -f "[w]ebsockify.*6080" >/dev/null 2>&1; then
  nohup websockify --web "$novnc_web" 127.0.0.1:6080 127.0.0.1:5900 \
    >"$display_dir/websockify.log" 2>&1 &
fi

printf "Ominal screen ready\n"
OMINAL_GUEST_DISPLAY
chmod 700 "$GUEST_SCRIPT"
export OMINAL_WORKDIR="$DISPLAY_WORKDIR"

nohup "$PREFIX/bin/ominal-proot-run" /usr/bin/env \
  OMINAL_DISPLAY="$DISPLAY_NUM" \
  OMINAL_DISPLAY_GEOMETRY="$DISPLAY_GEOMETRY" \
  OMINAL_WORKDIR="$DISPLAY_WORKDIR" \
  /bin/bash /root/workspace/.ominal-display-guest.sh \
  >"$DISPLAY_DIR/launcher.log" 2>&1 &
printf 'Ominal screen starting\n'
