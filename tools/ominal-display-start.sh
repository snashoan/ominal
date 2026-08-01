#!/data/data/com.ominal/files/usr/bin/sh
set -eu

PREFIX="${OMINAL_PREFIX:-/data/data/com.ominal/files/usr}"
HOME="${OMINAL_HOME:-/data/data/com.ominal/files/home}"
PATH="$PREFIX/bin:/system/bin"
export PREFIX HOME PATH

DISPLAY_NUM="${OMINAL_DISPLAY:-:20}"
DISPLAY_GEOMETRY="${OMINAL_DISPLAY_GEOMETRY:-540x1096x24}"
DISPLAY_DPI="${OMINAL_DISPLAY_DPI:-320}"
DISPLAY_BACKEND="${OMINAL_DISPLAY_BACKEND:-novnc}"
DESKTOP_SESSION="${OMINAL_DESKTOP_SESSION:-xfce}"
DISPLAY_DIR="$HOME/.ominal/display"
DISPLAY_WORKDIR="${OMINAL_WORKDIR:-$HOME/workspace}"
DISPLAY_READY_MARKER="$DISPLAY_DIR/ready"
mkdir -p "$DISPLAY_DIR"

case "$DESKTOP_SESSION" in
  xfce|jwm) ;;
  *)
    printf 'Unsupported Ominal desktop session: %s\n' "$DESKTOP_SESSION" >&2
    exit 64
    ;;
esac

host_session_version="ominal-display-host-v40-$DISPLAY_BACKEND-$DESKTOP_SESSION-$DISPLAY_GEOMETRY-$DISPLAY_DPI"
host_session_marker="$DISPLAY_DIR/session-version"
reset_host_session=0
if [ "$(cat "$host_session_marker" 2>/dev/null || true)" != "$host_session_version" ]; then
  reset_host_session=1
elif "$PREFIX/bin/pgrep" -x fluxbox >/dev/null 2>&1; then
  reset_host_session=1
elif [ "$DESKTOP_SESSION" = xfce ]; then
  for required_process in xfce4-session xfwm4 xfce4-panel xfdesktop; do
    if ! "$PREFIX/bin/pgrep" -x "$required_process" >/dev/null 2>&1; then
      reset_host_session=1
      break
    fi
  done
  if [ "$reset_host_session" -eq 0 ] \
      && ! "$PREFIX/bin/pgrep" -f "[o]minal-geometry-keeper" >/dev/null 2>&1; then
    reset_host_session=1
  fi
elif [ "$DESKTOP_SESSION" = jwm ] && ! "$PREFIX/bin/pgrep" -x jwm >/dev/null 2>&1; then
  reset_host_session=1
fi
if [ "$reset_host_session" -eq 0 ] && [ "$DISPLAY_BACKEND" != native ]; then
  for required_process in Xvfb x11vnc; do
    if ! "$PREFIX/bin/pgrep" -x "$required_process" >/dev/null 2>&1; then
      reset_host_session=1
      break
    fi
  done
  if [ "$reset_host_session" -eq 0 ] \
      && ! "$PREFIX/bin/pgrep" -f "[w]ebsockify.*6080" >/dev/null 2>&1; then
    reset_host_session=1
  fi
fi

if [ "$reset_host_session" -eq 1 ]; then
  "$PREFIX/bin/rm" -f "$DISPLAY_READY_MARKER"
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
  "$PREFIX/bin/pkill" -x xfce4-session 2>/dev/null || true
  "$PREFIX/bin/pkill" -x xfwm4 2>/dev/null || true
  "$PREFIX/bin/pkill" -x xfce4-panel 2>/dev/null || true
  "$PREFIX/bin/pkill" -x xfdesktop 2>/dev/null || true
  "$PREFIX/bin/pkill" -x xfconfd 2>/dev/null || true
  "$PREFIX/bin/pkill" -x devilspie2 2>/dev/null || true
  "$PREFIX/bin/pkill" -f "[u]nclutter-xfixes" 2>/dev/null || true
  "$PREFIX/bin/pkill" -x xterm 2>/dev/null || true
  "$PREFIX/bin/pkill" -x xfe 2>/dev/null || true
  "$PREFIX/bin/pkill" -x xfwrite 2>/dev/null || true
  "$PREFIX/bin/pkill" -x thunar 2>/dev/null || true
  "$PREFIX/bin/pkill" -x xfce4-terminal 2>/dev/null || true
  "$PREFIX/bin/pkill" -x mousepad 2>/dev/null || true
  "$PREFIX/bin/pkill" -x xfdesktop-settings 2>/dev/null || true
  "$PREFIX/bin/pkill" -f "[x]fce4-settings-manager" 2>/dev/null || true
  "$PREFIX/bin/pkill" -f "[f]irefox" 2>/dev/null || true
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

if [ "$reset_host_session" -eq 0 ]; then
  printf "%s" "$host_session_version" >"$DISPLAY_READY_MARKER"
  printf 'Ominal screen ready\n'
  exit 0
fi

GUEST_SCRIPT="$DISPLAY_WORKDIR/.ominal-display-guest.sh"
GUEST_SESSION_SOURCE="$PREFIX/bin/ominal-xfce-session"
GUEST_SESSION_SCRIPT="$DISPLAY_WORKDIR/.ominal-xfce-session.sh"
mkdir -p "$DISPLAY_WORKDIR"
if [ ! -x "$GUEST_SESSION_SOURCE" ]; then
  printf 'Ominal desktop session module is missing: %s\n' "$GUEST_SESSION_SOURCE" >&2
  exit 69
fi
"$PREFIX/bin/cp" "$GUEST_SESSION_SOURCE" "$GUEST_SESSION_SCRIPT"
"$PREFIX/bin/chmod" 700 "$GUEST_SESSION_SCRIPT"
cat > "$GUEST_SCRIPT" <<'OMINAL_GUEST_DISPLAY'
#!/bin/bash
set -eu
display_dir=/root/.ominal/display
desktop_version="ominal-mobile-v41-$OMINAL_DISPLAY_BACKEND-$OMINAL_DESKTOP_SESSION"
mkdir -p "$display_dir" /root/.local/bin
export OMINAL_WORKDIR=/root/workspace

required_commands="jwm xterm pcmanfm xfwrite firefox xfce4-settings-manager xdotool wmctrl scrot xrdb ominal-screen"
if [ "$OMINAL_DESKTOP_SESSION" = xfce ]; then
  required_commands="xfce4-session xfwm4 xfce4-panel xfdesktop thunar xfce4-terminal mousepad devilspie2 unclutter-xfixes $required_commands"
fi
if [ "$OMINAL_DISPLAY_BACKEND" != native ]; then
  required_commands="Xvfb x11vnc websockify $required_commands"
fi
for command_name in $required_commands; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    printf "Missing screen command: %s\n" "$command_name" >&2
    exit 69
  fi
done

if [ ! -f "$display_dir/geometry" ] \
    || [ "$(cat "$display_dir/geometry" 2>/dev/null || true)" != "$OMINAL_DISPLAY_GEOMETRY" ] \
    || [ "$(cat "$display_dir/dpi" 2>/dev/null || true)" != "$OMINAL_DISPLAY_DPI" ] \
    || [ "$(cat "$display_dir/version" 2>/dev/null || true)" != "$desktop_version" ]; then
  pkill -f "[w]ebsockify.*6080" 2>/dev/null || true
  pkill -f "[x]11vnc.*$OMINAL_DISPLAY" 2>/dev/null || true
  pkill -f "[X]vfb $OMINAL_DISPLAY" 2>/dev/null || true
  pkill -x jwm 2>/dev/null || true
  pkill -x xfce4-session 2>/dev/null || true
  pkill -x xfwm4 2>/dev/null || true
  pkill -x xfce4-panel 2>/dev/null || true
  pkill -x xfdesktop 2>/dev/null || true
  pkill -x xfconfd 2>/dev/null || true
  pkill -x devilspie2 2>/dev/null || true
  pkill -f "[u]nclutter-xfixes" 2>/dev/null || true
  pkill -x xterm 2>/dev/null || true
  pkill -x xfe 2>/dev/null || true
  pkill -x xfwrite 2>/dev/null || true
  pkill -x thunar 2>/dev/null || true
  pkill -x xfce4-terminal 2>/dev/null || true
  pkill -x mousepad 2>/dev/null || true
  pkill -x xfdesktop-settings 2>/dev/null || true
  pkill -f "[x]fce4-settings-manager" 2>/dev/null || true
  pkill -f "[f]irefox" 2>/dev/null || true
  pkill -x fluxbox 2>/dev/null || true
  pkill -x tint2 2>/dev/null || true
  pkill -x pcmanfm 2>/dev/null || true
  printf "%s" "$OMINAL_DISPLAY_GEOMETRY" >"$display_dir/geometry"
  printf "%s" "$OMINAL_DISPLAY_DPI" >"$display_dir/dpi"
  printf "%s" "$desktop_version" >"$display_dir/version"
fi

if [ "$OMINAL_DISPLAY_BACKEND" != native ] && ! pgrep -f "[X]vfb $OMINAL_DISPLAY" >/dev/null 2>&1; then
  rm -f "/tmp/.X11-unix/X${OMINAL_DISPLAY#:}" "/tmp/.X${OMINAL_DISPLAY#:}-lock"
  nohup Xvfb "$OMINAL_DISPLAY" -screen 0 "$OMINAL_DISPLAY_GEOMETRY" -nolisten tcp -nolock -dpi "$OMINAL_DISPLAY_DPI" \
    >"$display_dir/xvfb.log" 2>&1 &
fi

export DISPLAY="$OMINAL_DISPLAY"
export XCURSOR_SIZE=1
export LANG=C.UTF-8 LC_ALL=C.UTF-8
geometry_width="${OMINAL_DISPLAY_GEOMETRY%%x*}"
geometry_rest="${OMINAL_DISPLAY_GEOMETRY#*x}"
geometry_height="${geometry_rest%%x*}"
case "$geometry_width" in *[!0-9]*|'') geometry_width=1080 ;; esac
case "$geometry_height" in *[!0-9]*|'') geometry_height=1920 ;; esac
short_side="$geometry_width"
[ "$geometry_height" -ge "$short_side" ] || short_side="$geometry_height"
ui_dpi="${OMINAL_DISPLAY_DPI:-320}"
case "$ui_dpi" in *[!0-9]*|'') ui_dpi=320 ;; esac
# X receives physical pixels. Derive logical scale from the portrait canvas so
# 720p, 1080p, and 1440p phones retain equivalent touch geometry.
ui_scale=$(((short_side + 539) / 540))
[ "$ui_scale" -ge 1 ] || ui_scale=1
[ "$ui_scale" -le 3 ] || ui_scale=3
export OMINAL_UI_SCALE="$ui_scale"
export GDK_SCALE="$ui_scale" GDK_DPI_SCALE=1 GTK_OVERLAY_SCROLLING=0
export QT_SCALE_FACTOR="$ui_scale" QT_FONT_DPI=96 QT_AUTO_SCREEN_SCALE_FACTOR=0
export QT_ENABLE_HIGHDPI_SCALING=1 QT_SCALE_FACTOR_ROUNDING_POLICY=PassThrough
export ELM_SCALE="$ui_scale" FLTK_SCALING_FACTOR="$ui_scale" WINIT_X11_SCALE_FACTOR="$ui_scale"
export SAL_FORCEDPI=96
export MOZ_ENABLE_WAYLAND=0 MOZ_USE_XINPUT2=1
export MOZ_DISABLE_CONTENT_SANDBOX=1 MOZ_DISABLE_RDD_SANDBOX=1
export MOZ_DISABLE_GPU_SANDBOX=1 MOZ_DISABLE_GMP_SANDBOX=1
export MOZ_WEBRENDER=0 LIBGL_ALWAYS_SOFTWARE=1
font_dpi=96
top_bar_height=$(((short_side * 12 / 100 + ui_scale - 1) / ui_scale))
bottom_bar_height=$(((short_side * 14 / 100 + ui_scale - 1) / ui_scale))
[ "$top_bar_height" -ge 52 ] || top_bar_height=52
[ "$top_bar_height" -le 72 ] || top_bar_height=72
[ "$bottom_bar_height" -ge 64 ] || bottom_bar_height=64
[ "$bottom_bar_height" -le 84 ] || bottom_bar_height=84
task_height=$((top_bar_height * 3 / 4))
task_width=$((short_side * 28 / 100))
menu_item_height=$(((short_side * 13 / 100 + ui_scale - 1) / ui_scale))
[ "$menu_item_height" -ge 56 ] || menu_item_height=56
[ "$menu_item_height" -le 84 ] || menu_item_height=84
wm_font_size=13
dock_button_width=$bottom_bar_height
dock_spacer=$(((geometry_width - dock_button_width * 4) / 2))
[ "$dock_spacer" -ge 0 ] || dock_spacer=0
top_spacer=$((geometry_width - top_bar_height))
[ "$top_spacer" -ge 0 ] || top_spacer=0
for attempt in 1 2 3 4 5; do
  [ -S "/tmp/.X11-unix/X${OMINAL_DISPLAY#:}" ] && break
  sleep 0.2
done

mkdir -p /root/.config/gtk-3.0 /root/.config/gtk-4.0 /root/.config/xfe \
  /root/.config/xfce4/terminal /etc/profile.d
cat > /root/.Xresources <<EOF
Xft.dpi: $font_dpi
Xft.antialias: 1
Xft.hinting: 1
Xft.hintstyle: hintslight
XTerm*faceName: Monospace
XTerm*faceSize: 13
XTerm*scrollBar*width: 18
EOF
xrdb -merge /root/.Xresources >/dev/null 2>&1 || true

cat > /root/.gtkrc-2.0 <<EOF
gtk-font-name="Sans 13"
gtk-icon-sizes="gtk-menu=20,20:gtk-small-toolbar=22,22:gtk-large-toolbar=28,28:gtk-button=24,24:gtk-dialog=32,32"
EOF
for gtk_version in gtk-3.0 gtk-4.0; do
  cat > "/root/.config/$gtk_version/settings.ini" <<EOF
[Settings]
gtk-font-name=Sans 13
gtk-icon-theme-name=Adwaita
gtk-cursor-theme-size=1
gtk-enable-animations=true
EOF
done
cat > /root/.config/gtk-3.0/gtk.css <<'EOF'
#XfcePanelWindow,
.xfce4-panel.background {
  background-color: #08080a;
  color: #f4f5f7;
  border: 0;
  box-shadow: none;
}

#XfcePanelWindow button,
.xfce4-panel button {
  background: transparent;
  color: #f4f5f7;
  border: 0;
  border-radius: 12px;
  box-shadow: none;
  padding: 6px;
}

#XfcePanelWindow button:hover,
#XfcePanelWindow button:checked,
.xfce4-panel button:hover,
.xfce4-panel button:checked {
  background-color: #202024;
}
EOF

xfwm_theme_dir=/root/.themes/OminalMobile/xfwm4
rm -rf "$xfwm_theme_dir"
mkdir -p "$xfwm_theme_dir"
cp -a /usr/share/themes/Default-xhdpi/xfwm4/. "$xfwm_theme_dir/"
rm -f "$xfwm_theme_dir"/*.png

generate_xfwm_title() {
  piece="$1"
  state="$2"
  target="$3"
  awk -v piece="$piece" -v state="$state" '
BEGIN {
  height = 58
  width = piece == "title" ? 8 : 24
  radius = 18
  fill = state == "active" ? "." : "+"
  print "/* XPM */"
  print "static char * ominal_title[] = {"
  print "\"" width " " height " 3 1\","
  print "\"  c None\","
  print "\". c #121214\","
  print "\"+ c #0E0E10\","
  for (y = 0; y < height; y++) {
    row = ""
    for (x = 0; x < width; x++) {
      pixel = fill
      if (piece == "left" && x < radius && y < radius) {
        dx = radius - x
        dy = radius - y
        if (dx * dx + dy * dy > radius * radius) pixel = " "
      } else if (piece == "right" && x >= width - radius && y < radius) {
        dx = x - (width - radius - 1)
        dy = radius - y
        if (dx * dx + dy * dy > radius * radius) pixel = " "
      }
      row = row pixel
    }
    printf "\"%s\"%s\n", row, y == height - 1 ? "" : ","
  }
  print "};"
}' >"$target"
}

generate_xfwm_edge() {
  edge="$1"
  state="$2"
  target="$3"
  awk -v edge="$edge" -v state="$state" '
BEGIN {
  if (edge == "left" || edge == "right") { width = 12; height = 48 }
  else if (edge == "bottom") { width = 48; height = 12 }
  else { width = 32; height = 32 }
  line = state == "active" ? "." : "+"
  print "/* XPM */"
  print "static char * ominal_edge[] = {"
  print "\"" width " " height " 4 1\","
  print "\"  c None\","
  print "\". c #29292E\","
  print "\"+ c #202024\","
  print "\"@ c #000000\","
  for (y = 0; y < height; y++) {
    row = ""
    for (x = 0; x < width; x++) {
      pixel = "@"
      if (edge == "left" && x < 2) pixel = line
      else if (edge == "right" && x >= width - 2) pixel = line
      else if (edge == "bottom" && y >= height - 2) pixel = line
      else if (edge == "bottom-left") {
        dx = 15 - x
        dy = y - 15
        if (x < 16 && y >= 16 && dx * dx + dy * dy > 15 * 15) pixel = " "
        else if (x < 2 || y >= height - 2) pixel = line
      } else if (edge == "bottom-right") {
        dx = x - 16
        dy = y - 15
        if (x >= 16 && y >= 16 && dx * dx + dy * dy > 15 * 15) pixel = " "
        else if (x >= width - 2 || y >= height - 2) pixel = line
      }
      row = row pixel
    }
    printf "\"%s\"%s\n", row, y == height - 1 ? "" : ","
  }
  print "};"
}' >"$target"
}

generate_xfwm_button() {
  button_kind="$1"
  button_state="$2"
  button_target="$3"
  awk -v kind="$button_kind" -v state="$button_state" '
function abs(value) { return value < 0 ? -value : value }
BEGIN {
  width = 88
  height = 88
  start = 25
  finish = 62
  stroke = 3
  print "/* XPM */"
  print "static char * ominal_button[] = {"
  print "\"" width " " height " 5 1\","
  print "\"  c None\","
  print "\". c #F1F1F3\","
  print "\"+ c #29292E\","
  print "\"* c #36363C\","
  print "\"@ c #A8A8AE\","
  for (y = 0; y < height; y++) {
    row = ""
    for (x = 0; x < width; x++) {
      pixel = " "
      if (state == "prelight" || state == "pressed") {
        dx = x < 20 ? 20 - x : (x > 67 ? x - 67 : 0)
        dy = y < 12 ? 12 - y : (y > 75 ? y - 75 : 0)
        if (dx * dx + dy * dy <= 12 * 12)
          pixel = state == "pressed" ? "*" : "+"
      }
      if (x >= start && x <= finish && y >= start && y <= finish) {
        if (kind == "close" && (abs(x - y) <= stroke || abs((x + y) - (width - 1)) <= stroke))
          pixel = state == "inactive" ? "@" : "."
        if (kind == "maximize" && (x - start <= stroke || finish - x <= stroke || y - start <= stroke || finish - y <= stroke))
          pixel = state == "inactive" ? "@" : "."
      }
      row = row pixel
    }
    printf "\"%s\"%s\n", row, y == height - 1 ? "" : ","
  }
  print "};"
}' >"$button_target"
}
for title_state in active inactive; do
  generate_xfwm_title left "$title_state" "$xfwm_theme_dir/top-left-$title_state.xpm"
  generate_xfwm_title right "$title_state" "$xfwm_theme_dir/top-right-$title_state.xpm"
  for title_piece in 1 2 3 4 5; do
    generate_xfwm_title title "$title_state" \
      "$xfwm_theme_dir/title-$title_piece-$title_state.xpm"
  done
  generate_xfwm_edge left "$title_state" "$xfwm_theme_dir/left-$title_state.xpm"
  generate_xfwm_edge right "$title_state" "$xfwm_theme_dir/right-$title_state.xpm"
  generate_xfwm_edge bottom "$title_state" "$xfwm_theme_dir/bottom-$title_state.xpm"
  generate_xfwm_edge bottom-left "$title_state" \
    "$xfwm_theme_dir/bottom-left-$title_state.xpm"
  generate_xfwm_edge bottom-right "$title_state" \
    "$xfwm_theme_dir/bottom-right-$title_state.xpm"
done
for button_state in active inactive prelight pressed; do
  generate_xfwm_button close "$button_state" \
    "$xfwm_theme_dir/close-$button_state.xpm"
  generate_xfwm_button maximize "$button_state" \
    "$xfwm_theme_dir/maximize-$button_state.xpm"
  generate_xfwm_button maximize "$button_state" \
    "$xfwm_theme_dir/maximize-toggled-$button_state.xpm"
done
cat >"$xfwm_theme_dir/themerc" <<'EOF'
active_text_color=#F1F1F3
inactive_text_color=#A8A8AE
button_offset=0
button_spacing=4
frame_border_top=4
full_width_title=true
maximized_offset=0
show_app_icon=false
shadow_delta_height=0
shadow_delta_width=0
shadow_delta_x=0
shadow_delta_y=0
shadow_opacity=0
title_horizontal_offset=0
title_shadow_active=false
title_shadow_inactive=false
title_vertical_offset_active=2
title_vertical_offset_inactive=2
EOF

cat > /root/.config/xfe/xferc <<EOF
[SETTINGS]
screenres=320
normalfont=Sans,120,normal,regular
textfont=Monospace,120,normal,regular
scrollbarsize=$((16 * ui_scale))
wheellines=3
iconpath=/usr/share/xfe/icons/default-theme
EOF

terminal_config=/root/.config/xfce4/terminal/terminalrc
if [ ! -f "$terminal_config" ]; then
  cat >"$terminal_config" <<EOF
[Configuration]
FontName=Monospace 13
FontUseSystem=FALSE
MiscDefaultGeometry=80x24
MiscMenubarDefault=FALSE
MiscToolbarDefault=FALSE
MiscBordersDefault=TRUE
ScrollingBar=TERMINAL_SCROLLBAR_NONE
ScrollingLines=10000
ScrollingUnlimited=TRUE
ScrollingOnOutput=FALSE
ScrollingOnKeystroke=TRUE
BackgroundMode=TERMINAL_BACKGROUND_SOLID
ColorForeground=#f4f5f7
ColorBackground=#000000
ColorCursor=#ffffff
EOF
fi
if grep -q '^MiscBordersDefault=' "$terminal_config"; then
  sed -i 's/^MiscBordersDefault=.*/MiscBordersDefault=TRUE/' "$terminal_config"
else
  printf '%s\n' 'MiscBordersDefault=TRUE' >>"$terminal_config"
fi

cat > /etc/profile.d/ominal-mobile-ui.sh <<EOF
export OMINAL_DISPLAY_DPI=$ui_dpi OMINAL_UI_SCALE=$ui_scale
export GDK_SCALE=$ui_scale GDK_DPI_SCALE=1 GTK_OVERLAY_SCROLLING=0
export QT_SCALE_FACTOR=$ui_scale QT_FONT_DPI=96 QT_AUTO_SCREEN_SCALE_FACTOR=0
export QT_ENABLE_HIGHDPI_SCALING=1 QT_SCALE_FACTOR_ROUNDING_POLICY=PassThrough
export ELM_SCALE=$ui_scale FLTK_SCALING_FACTOR=$ui_scale WINIT_X11_SCALE_FACTOR=$ui_scale
export SAL_FORCEDPI=96
export MOZ_ENABLE_WAYLAND=0 MOZ_USE_XINPUT2=1
export MOZ_DISABLE_CONTENT_SANDBOX=1 MOZ_DISABLE_RDD_SANDBOX=1
export MOZ_DISABLE_GPU_SANDBOX=1 MOZ_DISABLE_GMP_SANDBOX=1
export MOZ_WEBRENDER=0 LIBGL_ALWAYS_SOFTWARE=1
EOF

cat > /root/.local/bin/ominal-terminal <<EOF
#!/bin/sh
export LANG=C.UTF-8 LC_ALL=C.UTF-8
if wmctrl -xa xfce4-terminal.Xfce4-terminal >/dev/null 2>&1 \
    || wmctrl -xa xterm.XTerm >/dev/null 2>&1; then exit 0; fi
cd "${OMINAL_WORKDIR:-/root/workspace}" 2>/dev/null || cd /root
if command -v xfce4-terminal >/dev/null 2>&1; then
  exec xfce4-terminal --show-borders --hide-menubar --hide-toolbar \
    --title=Terminal --working-directory="${OMINAL_WORKDIR:-/root/workspace}"
fi
exec xterm -fa Monospace -fs 13 -bg '#000000' -fg '#f4f5f7' -cr '#ffffff' \
  -xrm 'XTerm*scrollBar*width: 18' -xrm 'XTerm*utf8: 2' \
  -sb -rightbar -T Terminal -e bash --noprofile --norc -i
EOF

cat > /root/.local/bin/ominal-files <<"EOF"
#!/bin/sh
if wmctrl -xa thunar.Thunar >/dev/null 2>&1 \
    || wmctrl -xa pcmanfm.Pcmanfm >/dev/null 2>&1; then exit 0; fi
workspace=/root/workspace
if command -v thunar >/dev/null 2>&1; then
  exec thunar "$workspace"
fi
exec pcmanfm "$workspace"
EOF

cat > /root/.local/bin/ominal-editor <<"EOF"
#!/bin/sh
if wmctrl -xa mousepad.Mousepad >/dev/null 2>&1 || wmctrl -xa xfwrite.Xfwrite >/dev/null 2>&1; then exit 0; fi
cd "${OMINAL_WORKDIR:-/root/workspace}" 2>/dev/null || cd /root
if command -v mousepad >/dev/null 2>&1; then
  exec mousepad
fi
exec xfwrite
EOF

cat > /root/.local/bin/ominal-browser <<"EOF"
#!/bin/sh
if wmctrl -xa Navigator.firefox >/dev/null 2>&1; then exit 0; fi
profile_root=/var/lib/ominal/browser-user
mkdir -p "$profile_root/Downloads" "$profile_root/.mozilla"
chown -R nobody:nogroup "$profile_root" 2>/dev/null || true
exec su -m -s /bin/sh nobody -c \
  'HOME=/var/lib/ominal/browser-user XDG_DOWNLOAD_DIR=/var/lib/ominal/browser-user/Downloads DISPLAY="$DISPLAY" MOZ_ENABLE_WAYLAND=0 MOZ_USE_XINPUT2=1 MOZ_DISABLE_CONTENT_SANDBOX=1 MOZ_DISABLE_RDD_SANDBOX=1 MOZ_DISABLE_GPU_SANDBOX=1 MOZ_DISABLE_GMP_SANDBOX=1 MOZ_WEBRENDER=0 LIBGL_ALWAYS_SOFTWARE=1 exec firefox --new-window about:blank'
EOF

cat > /root/.local/bin/ominal-settings <<"EOF"
#!/bin/sh
if wmctrl -xa xfce4-settings-manager.Xfce4-settings-manager >/dev/null 2>&1; then exit 0; fi
exec xfce4-settings-manager
EOF
cat > /root/.local/bin/ominal-chat <<"EOF"
#!/bin/sh
request_dir=/tmp
request_file="$request_dir/ominal-display-close.request"
mkdir -p "$request_dir"
printf '%s\n' "$(date +%s)" >"$request_file.tmp"
mv "$request_file.tmp" "$request_file"
EOF
chmod 755 /root/.local/bin/ominal-terminal /root/.local/bin/ominal-files \
  /root/.local/bin/ominal-editor /root/.local/bin/ominal-browser \
  /root/.local/bin/ominal-settings /root/.local/bin/ominal-chat

icon_dir="$display_dir/icons"
mkdir -p "$icon_dir"
rm -f "$icon_dir"/files.* "$icon_dir"/terminal.* "$icon_dir"/editor.* \
  "$icon_dir"/browser.* "$icon_dir"/settings.* "$icon_dir"/screen.* \
  "$icon_dir"/chat.* "$icon_dir"/apps.*
prepare_icon() {
  source_icon="$1"
  target_icon="$2"
  if [ -f "$source_icon" ]; then
    sed 's/#2e3436/#f4f5f7/g; s/#241f31/#f4f5f7/g' "$source_icon" > "$target_icon"
  fi
}
prepare_icon /usr/share/icons/Adwaita/symbolic/places/folder-symbolic.svg "$icon_dir/files.svg"
prepare_icon /usr/share/icons/Adwaita/symbolic/legacy/utilities-terminal-symbolic.svg "$icon_dir/terminal.svg"
prepare_icon /usr/share/icons/Adwaita/symbolic/legacy/accessories-text-editor-symbolic.svg "$icon_dir/editor.svg"
prepare_icon /usr/share/icons/Adwaita/symbolic/categories/preferences-system-symbolic.svg "$icon_dir/settings.svg"
cat > "$icon_dir/screen.svg" <<'EOF'
<svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 48 48">
  <g fill="none" stroke="#F4F5F7" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
    <rect x="11" y="13" width="26" height="19" rx="2.5"/>
    <path d="M19 37h10M24 32v5"/>
  </g>
</svg>
EOF
cat > "$icon_dir/chat.svg" <<'EOF'
<svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 48 48">
  <path d="M6.24 38.4H16.8L12 31.2C6.72 23.52 8.64 9.12 24 8.64C39.36 9.12 41.28 23.52 36 31.2L31.2 38.4H41.76"
    fill="none" stroke="#F4F5F7" stroke-width="4.2" stroke-linecap="round" stroke-linejoin="round"/>
</svg>
EOF
firefox_icon=/usr/lib/firefox/browser/chrome/icons/default/default128.png
[ -f "$firefox_icon" ] || firefox_icon="$(find -L /usr/share/icons/hicolor -type f -path '*/apps/firefox.png' | sort -V | tail -n 1)"
[ -n "$firefox_icon" ] && cp -L "$firefox_icon" "$icon_dir/browser.png"
fallback_icon=/usr/share/icons/hicolor/48x48/apps/xterm.png
[ -s "$icon_dir/files.svg" ] || cp "$fallback_icon" "$icon_dir/files.png"
[ -s "$icon_dir/terminal.svg" ] || cp "$fallback_icon" "$icon_dir/terminal.png"
[ -s "$icon_dir/editor.svg" ] || cp "$fallback_icon" "$icon_dir/editor.png"
[ -s "$icon_dir/settings.svg" ] || cp "$fallback_icon" "$icon_dir/settings.png"
[ -s "$icon_dir/browser.png" ] || cp "$fallback_icon" "$icon_dir/browser.png"
[ -s "$icon_dir/screen.svg" ] || cp "$fallback_icon" "$icon_dir/screen.png"
[ -s "$icon_dir/chat.svg" ] || cp "$fallback_icon" "$icon_dir/chat.png"
chmod 644 "$icon_dir"/*
files_icon="$(find "$icon_dir" -maxdepth 1 -type f -name 'files.*' | head -n 1)"
terminal_icon="$(find "$icon_dir" -maxdepth 1 -type f -name 'terminal.*' | head -n 1)"
editor_icon="$(find "$icon_dir" -maxdepth 1 -type f -name 'editor.*' | head -n 1)"
browser_icon="$(find "$icon_dir" -maxdepth 1 -type f -name 'browser.*' | head -n 1)"
settings_icon="$(find "$icon_dir" -maxdepth 1 -type f -name 'settings.*' | head -n 1)"
screen_icon="$(find "$icon_dir" -maxdepth 1 -type f -name 'screen.*' | head -n 1)"
chat_icon="$(find "$icon_dir" -maxdepth 1 -type f -name 'chat.*' | head -n 1)"

if command -v xsetroot >/dev/null 2>&1; then
  timeout 2s xsetroot -solid "#000000" -cursor_name none >/dev/null 2>&1 || true
fi

session_script=/root/workspace/.ominal-xfce-session.sh
if [ ! -x "$session_script" ]; then
  printf 'Ominal desktop session module is missing.\n' >&2
  exit 69
fi
export OMINAL_DISPLAY_DIR="$display_dir"
export OMINAL_TOP_BAR_HEIGHT="$top_bar_height"
export OMINAL_BOTTOM_BAR_HEIGHT="$bottom_bar_height"
export OMINAL_WM_FONT_SIZE="$wm_font_size"
export OMINAL_MENU_ITEM_HEIGHT="$menu_item_height"
export OMINAL_GEOMETRY_WIDTH="$geometry_width"
export OMINAL_GEOMETRY_HEIGHT="$geometry_height"
export OMINAL_FILES_ICON="$files_icon"
export OMINAL_TERMINAL_ICON="$terminal_icon"
export OMINAL_EDITOR_ICON="$editor_icon"
export OMINAL_BROWSER_ICON="$browser_icon"
export OMINAL_SETTINGS_ICON="$settings_icon"
export OMINAL_SCREEN_ICON="$screen_icon"
export OMINAL_CHAT_ICON="$chat_icon"
if [ "$OMINAL_DISPLAY_BACKEND" = native ]; then
  exec "$session_script"
fi
"$session_script" background

if [ "$OMINAL_DISPLAY_BACKEND" != native ] && ! pgrep -f "[x]11vnc.*$OMINAL_DISPLAY" >/dev/null 2>&1; then
  nohup x11vnc -display "$OMINAL_DISPLAY" -localhost -forever -shared -nopw \
    -noshm -noxdamage -nocursor -rfbport 5900 >"$display_dir/x11vnc.log" 2>&1 &
fi

if [ "$OMINAL_DISPLAY_BACKEND" != native ]; then
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
fi

printf "Ominal screen ready\n"
OMINAL_GUEST_DISPLAY
chmod 700 "$GUEST_SCRIPT"
export OMINAL_WORKDIR="$DISPLAY_WORKDIR"

nohup "$PREFIX/bin/ominal-proot-run" /usr/bin/env \
  OMINAL_DISPLAY="$DISPLAY_NUM" \
  OMINAL_DISPLAY_GEOMETRY="$DISPLAY_GEOMETRY" \
  OMINAL_DISPLAY_DPI="$DISPLAY_DPI" \
  OMINAL_DISPLAY_BACKEND="$DISPLAY_BACKEND" \
  OMINAL_DESKTOP_SESSION="$DESKTOP_SESSION" \
  OMINAL_WORKDIR="$DISPLAY_WORKDIR" \
  /bin/bash /root/workspace/.ominal-display-guest.sh \
  >"$DISPLAY_DIR/launcher.log" 2>&1 &
(
  for attempt in $(seq 1 100); do
    desktop_ready=1
    if [ "$DESKTOP_SESSION" = xfce ]; then
      for required_process in xfce4-session xfwm4 xfce4-panel xfdesktop; do
        if ! "$PREFIX/bin/pgrep" -x "$required_process" >/dev/null 2>&1; then
          desktop_ready=0
          break
        fi
      done
      if [ "$desktop_ready" -eq 1 ] \
          && ! "$PREFIX/bin/pgrep" -f "[o]minal-geometry-keeper" >/dev/null 2>&1; then
        desktop_ready=0
      fi
    elif ! "$PREFIX/bin/pgrep" -x jwm >/dev/null 2>&1; then
      desktop_ready=0
    fi

    if [ "$desktop_ready" -eq 1 ]; then
      printf "%s" "$host_session_version" >"$DISPLAY_READY_MARKER"
      while sleep 1; do
        desktop_ready=1
        if [ "$DESKTOP_SESSION" = xfce ]; then
          for required_process in xfce4-session xfwm4 xfce4-panel xfdesktop; do
            if ! "$PREFIX/bin/pgrep" -x "$required_process" >/dev/null 2>&1; then
              desktop_ready=0
              break
            fi
          done
          if [ "$desktop_ready" -eq 1 ] \
              && ! "$PREFIX/bin/pgrep" -f "[o]minal-geometry-keeper" >/dev/null 2>&1; then
            desktop_ready=0
          fi
        elif ! "$PREFIX/bin/pgrep" -x jwm >/dev/null 2>&1; then
          desktop_ready=0
        fi
        if [ "$desktop_ready" -eq 0 ]; then
          "$PREFIX/bin/rm" -f "$DISPLAY_READY_MARKER"
          exit 1
        fi
      done
    fi
    sleep 0.1
  done
  "$PREFIX/bin/rm" -f "$DISPLAY_READY_MARKER"
) >"$DISPLAY_DIR/health.log" 2>&1 &
printf 'Ominal screen starting\n'
