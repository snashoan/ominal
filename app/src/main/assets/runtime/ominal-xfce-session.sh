#!/bin/bash
set -eu

display_dir="${OMINAL_DISPLAY_DIR:-/root/.ominal/display}"
session_mode="${OMINAL_DESKTOP_SESSION:-xfce}"
launch_mode="${1:-foreground}"
top_bar_height="${OMINAL_TOP_BAR_HEIGHT:-112}"
bottom_bar_height="${OMINAL_BOTTOM_BAR_HEIGHT:-144}"
wm_font_size="${OMINAL_WM_FONT_SIZE:-17}"
menu_item_height="${OMINAL_MENU_ITEM_HEIGHT:-120}"
geometry_width="${OMINAL_GEOMETRY_WIDTH:-1080}"
geometry_height="${OMINAL_GEOMETRY_HEIGHT:-1920}"
ui_scale="${OMINAL_UI_SCALE:-2}"
files_icon="${OMINAL_FILES_ICON:-system-file-manager}"
terminal_icon="${OMINAL_TERMINAL_ICON:-utilities-terminal}"
editor_icon="${OMINAL_EDITOR_ICON:-accessories-text-editor}"
browser_icon="${OMINAL_BROWSER_ICON:-firefox}"
settings_icon="${OMINAL_SETTINGS_ICON:-preferences-system}"
screen_icon="${OMINAL_SCREEN_ICON:-video-display}"
chat_icon="${OMINAL_CHAT_ICON:-go-previous}"
jwm_top_spacer=$((geometry_width - top_bar_height))
[ "$jwm_top_spacer" -ge 0 ] || jwm_top_spacer=0

mkdir -p "$display_dir"

stop_xfce() {
    pkill -x xfce4-session 2>/dev/null || true
    pkill -x xfwm4 2>/dev/null || true
    pkill -x xfce4-panel 2>/dev/null || true
    pkill -x xfdesktop 2>/dev/null || true
    pkill -x xfconfd 2>/dev/null || true
    pkill -x devilspie2 2>/dev/null || true
    pkill -f "[u]nclutter-xfixes" 2>/dev/null || true
    pkill -f "[o]minal-geometry-keeper" 2>/dev/null || true
    pkill -f "[o]minal-wallpaper-keeper" 2>/dev/null || true
}

write_desktop_entry() {
    target="$1"
    name="$2"
    command="$3"
    icon="$4"
    category="$5"
    cat >"$target" <<EOF
[Desktop Entry]
Type=Application
Version=1.0
Name=$name
Exec=$command
Icon=$icon
Terminal=false
StartupNotify=true
Categories=$category;
EOF
    chmod 755 "$target"
}

configure_xfce() {
    config_root=/root/.config/xfce4
    channel_root="$config_root/xfconf/xfce-perchannel-xml"
    panel_root="$config_root/panel"
    desktop_root=/root/Desktop
    autostart_root=/root/.config/autostart
    rules_root=/root/.config/devilspie2
    desktop_icon_size=68
    wallpaper_root=/root/.local/share/backgrounds
    wallpaper_state="$wallpaper_root/current-wallpaper.path"
    bundled_wallpaper=/usr/local/share/gir/gir-final-wallpaper.png
    default_wallpaper="$wallpaper_root/gir-final-wallpaper.png"
    legacy_wallpaper="$wallpaper_root/gir-fabric.svg"

    rm -rf "$panel_root"
    mkdir -p "$channel_root" "$panel_root" "$desktop_root" "$autostart_root" \
        "$rules_root" "$config_root/desktop" "$wallpaper_root"
    rm -rf /root/.cache/sessions

    if [ -f "$bundled_wallpaper" ]; then
        cp -f "$bundled_wallpaper" "$default_wallpaper"
    else
        default_wallpaper="$legacy_wallpaper"
        cat >"$default_wallpaper" <<'EOF'
<svg xmlns="http://www.w3.org/2000/svg" width="1080" height="2131">
  <rect width="1080" height="2131" fill="#050506"/>
</svg>
EOF
    fi
    chmod 644 "$default_wallpaper"

    desktop_layout_version=balanced-home-v3
    desktop_layout_marker="$display_dir/desktop-layout-version"
    desktop_layout_changed=false
    if [ "$(cat "$desktop_layout_marker" 2>/dev/null || true)" != "$desktop_layout_version" ]; then
        rm -f "$config_root/desktop"/icons.screen*.rc
        printf '%s' "$desktop_layout_version" >"$desktop_layout_marker"
        desktop_layout_changed=true
    fi

    cat >"$channel_root/xsettings.xml" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<channel name="xsettings" version="1.0">
  <property name="Net" type="empty">
    <property name="ThemeName" type="string" value="Adwaita-dark"/>
    <property name="IconThemeName" type="string" value="Adwaita"/>
    <property name="DoubleClickTime" type="int" value="400"/>
  </property>
  <property name="Gtk" type="empty">
    <property name="FontName" type="string" value="Sans $wm_font_size"/>
    <property name="CursorThemeSize" type="int" value="1"/>
    <property name="ToolbarStyle" type="string" value="icons"/>
    <property name="MenuImages" type="bool" value="true"/>
    <property name="ButtonImages" type="bool" value="true"/>
  </property>
  <property name="Gdk" type="empty">
    <property name="WindowScalingFactor" type="int" value="$ui_scale"/>
  </property>
</channel>
EOF

    cat >"$channel_root/xfwm4.xml" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<channel name="xfwm4" version="1.0">
  <property name="general" type="empty">
    <property name="theme" type="string" value="OminalMobile"/>
    <property name="button_layout" type="string" value="|MC"/>
    <property name="title_font" type="string" value="Sans Bold $wm_font_size"/>
    <property name="title_alignment" type="string" value="center"/>
    <property name="click_to_focus" type="bool" value="true"/>
    <property name="focus_delay" type="int" value="0"/>
    <property name="raise_delay" type="int" value="0"/>
    <property name="use_compositing" type="bool" value="false"/>
    <property name="titleless_maximize" type="bool" value="true"/>
    <property name="borderless_maximize" type="bool" value="true"/>
    <property name="easy_click" type="string" value="None"/>
    <property name="workspace_count" type="int" value="1"/>
    <property name="wrap_windows" type="bool" value="false"/>
    <property name="wrap_workspaces" type="bool" value="false"/>
  </property>
</channel>
EOF

    shortcuts_file="$channel_root/xfce4-keyboard-shortcuts.xml"
    if [ ! -f "$shortcuts_file" ]; then
        cat >"$shortcuts_file" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<channel name="xfce4-keyboard-shortcuts" version="1.0"/>
EOF
    fi
    python3 - "$shortcuts_file" <<'PY'
import sys
import xml.etree.ElementTree as ET

path = sys.argv[1]
tree = ET.parse(path)
root = tree.getroot()

def child(parent, name, kind="empty", value=None):
    node = next((item for item in parent.findall("property")
                 if item.get("name") == name), None)
    if node is None:
        node = ET.SubElement(parent, "property", {"name": name})
    node.set("type", kind)
    if value is None:
        node.attrib.pop("value", None)
    else:
        node.set("value", str(value))
    return node

xfwm = child(root, "xfwm4")
custom = child(xfwm, "custom")
child(custom, "<Primary><Alt>d", "string", "show_desktop_key")
child(custom, "<Alt>Tab", "string", "cycle_windows_key")
tree.write(path, encoding="UTF-8", xml_declaration=True)
PY

    if [ ! -f "$channel_root/xfce4-desktop.xml" ]; then
        cat >"$channel_root/xfce4-desktop.xml" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<channel name="xfce4-desktop" version="1.0">
  <property name="desktop-icons" type="empty">
    <property name="style" type="int" value="2"/>
    <property name="icon-size" type="uint" value="$desktop_icon_size"/>
    <property name="gravity" type="int" value="5"/>
    <property name="use-custom-font-size" type="bool" value="true"/>
    <property name="font-size" type="double" value="16"/>
    <property name="center-text" type="bool" value="true"/>
    <property name="single-click" type="bool" value="true"/>
    <property name="show-tooltips" type="bool" value="false"/>
    <property name="file-icons" type="empty">
      <property name="show-home" type="bool" value="false"/>
      <property name="show-filesystem" type="bool" value="false"/>
      <property name="show-trash" type="bool" value="false"/>
      <property name="show-removable" type="bool" value="false"/>
    </property>
  </property>
  <property name="backdrop" type="empty">
    <property name="screen0" type="empty">
      <property name="monitor0" type="empty">
        <property name="workspace0" type="empty">
          <property name="color-style" type="int" value="0"/>
          <property name="image-style" type="int" value="0"/>
          <property name="last-image" type="string" value=""/>
          <property name="rgba1" type="array">
            <value type="double" value="0"/>
            <value type="double" value="0"/>
            <value type="double" value="0"/>
            <value type="double" value="1"/>
          </property>
        </property>
      </property>
      <property name="monitorbuiltin" type="empty">
        <property name="workspace0" type="empty">
          <property name="color-style" type="int" value="0"/>
          <property name="image-style" type="int" value="0"/>
          <property name="last-image" type="string" value=""/>
          <property name="rgba1" type="array">
            <value type="double" value="0"/>
            <value type="double" value="0"/>
            <value type="double" value="0"/>
            <value type="double" value="1"/>
          </property>
        </property>
      </property>
    </property>
  </property>
</channel>
EOF
    fi

    # Preserve user-selected backdrops while retiring the oversized bundled home mark.
    python3 - "$channel_root/xfce4-desktop.xml" "$desktop_icon_size" \
        "$default_wallpaper" "$legacy_wallpaper" "$wallpaper_state" <<'PY'
import os
import sys
import xml.etree.ElementTree as ET

path, icon_size, default_wallpaper, legacy_wallpaper, wallpaper_state = sys.argv[1:6]
tree = ET.parse(path)
root = tree.getroot()

def child(parent, name, kind="empty", value=None):
    node = next((item for item in parent.findall("property")
                 if item.get("name") == name), None)
    if node is None:
        node = ET.SubElement(parent, "property", {"name": name})
    node.set("type", kind)
    if value is None:
        node.attrib.pop("value", None)
    else:
        node.set("value", str(value))
    return node

icons = child(root, "desktop-icons")
child(icons, "style", "int", 2)
child(icons, "icon-size", "uint", icon_size)
child(icons, "gravity", "int", 5)
child(icons, "use-custom-font-size", "bool", "true")
child(icons, "font-size", "double", 16)
child(icons, "center-text", "bool", "true")
child(icons, "single-click", "bool", "true")
child(icons, "show-tooltips", "bool", "false")

backdrop = child(root, "backdrop")
screen = child(backdrop, "screen0")
for monitor_name in ("monitor0", "monitorbuiltin"):
    child(child(screen, monitor_name), "workspace0")

retired_wallpapers = {
    legacy_wallpaper,
    "/usr/share/backgrounds/xfce/xfce-shapes.svg",
}
persisted_wallpaper = ""
try:
    with open(wallpaper_state, encoding="utf-8") as state:
        candidate = state.read().strip()
    if os.path.isfile(candidate):
        persisted_wallpaper = candidate
except OSError:
    pass
for monitor in screen.findall("property"):
    if not monitor.get("name", "").startswith("monitor"):
        continue
    workspaces = [node for node in monitor.findall("property")
                  if node.get("name", "").startswith("workspace")]
    if not workspaces:
        workspaces = [child(monitor, "workspace0")]
    for workspace in workspaces:
        image = child(workspace, "last-image", "string", "")
        current_image = image.get("value", "")
        if current_image and current_image not in retired_wallpapers \
                and os.path.isfile(current_image):
            continue
        if persisted_wallpaper:
            image.set("value", persisted_wallpaper)
            child(workspace, "image-style", "int", 5)
        else:
            image.set("value", "")
            child(workspace, "image-style", "int", 0)
tree.write(path, encoding="UTF-8", xml_declaration=True)
PY

    # GIR owns visible navigation; XFCE only needs a hidden panel process for session health.
    cat >"$channel_root/xfce4-panel.xml" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<channel name="xfce4-panel" version="1.0">
  <property name="configver" type="int" value="2"/>
  <property name="panels" type="array">
    <value type="int" value="2"/>
    <property name="panel-2" type="empty">
      <property name="position" type="string" value="p=10;x=0;y=0"/>
      <property name="length" type="uint" value="1"/>
      <property name="position-locked" type="bool" value="true"/>
      <property name="size" type="uint" value="1"/>
      <property name="nrows" type="uint" value="1"/>
      <property name="autohide-behavior" type="uint" value="1"/>
      <property name="disable-struts" type="bool" value="true"/>
      <property name="background-style" type="uint" value="1"/>
      <property name="background-rgba" type="array">
        <value type="double" value="0"/>
        <value type="double" value="0"/>
        <value type="double" value="0"/>
        <value type="double" value="0"/>
      </property>
      <property name="enter-opacity" type="uint" value="0"/>
      <property name="leave-opacity" type="uint" value="0"/>
      <property name="plugin-ids" type="array"/>
    </property>
  </property>
  <property name="plugins" type="empty"/>
</channel>
EOF

    write_desktop_entry "$desktop_root/Files.desktop" "Files" \
        /root/.local/bin/ominal-files "$files_icon" System
    write_desktop_entry "$desktop_root/Firefox.desktop" "Firefox" \
        /root/.local/bin/ominal-browser "$browser_icon" Network
    write_desktop_entry "$desktop_root/Terminal.desktop" "Terminal" \
        /root/.local/bin/ominal-terminal "$terminal_icon" System
    write_desktop_entry "$desktop_root/Settings.desktop" "Settings" \
        /root/.local/bin/ominal-settings "$settings_icon" Settings
    write_desktop_entry "$desktop_root/Editor.desktop" "Editor" \
        /root/.local/bin/ominal-editor "$editor_icon" Utility

    cat >/root/.local/bin/ominal-wallpaper-keeper <<'EOF'
#!/bin/sh
wallpaper_root=/root/.local/share/backgrounds
state_file="$wallpaper_root/current-wallpaper.path"
mkdir -p "$wallpaper_root"
while sleep 0.8; do
    properties="$(xfconf-query -c xfce4-desktop -l 2>/dev/null \
        | grep '/last-image$' || true)"
    [ -n "$properties" ] || continue
    saved="$(cat "$state_file" 2>/dev/null || true)"
    selected=
    while IFS= read -r property; do
        [ -n "$property" ] || continue
        candidate="$(xfconf-query -c xfce4-desktop -p "$property" 2>/dev/null || true)"
        [ -f "$candidate" ] || continue
        [ "$candidate" != "$saved" ] || continue
        case "$candidate" in
          "$wallpaper_root"/current-wallpaper.*)
            selected="$candidate"
            break
            ;;
          /usr/local/share/gir/gir-final-wallpaper.png)
            selected="$wallpaper_root/gir-final-wallpaper.png"
            break
            ;;
          "$wallpaper_root"/*)
            selected="$candidate"
            break
            ;;
          *)
            extension="${candidate##*.}"
            case "$extension" in
              png|PNG|jpg|JPG|jpeg|JPEG|webp|WEBP|svg|SVG) ;;
              *) extension=img ;;
            esac
            target="$wallpaper_root/current-wallpaper.$extension"
            if cp -f "$candidate" "$target.tmp" 2>/dev/null; then
                mv -f "$target.tmp" "$target"
                selected="$target"
            fi
            break
            ;;
        esac
    done <<PROPERTIES
$properties
PROPERTIES
    if [ -z "$selected" ] && [ -f "$saved" ]; then
        selected="$saved"
    fi
    [ -n "$selected" ] || continue
    if [ "$saved" != "$selected" ]; then
        printf '%s' "$selected" >"$state_file"
    fi
    changed=false
    while IFS= read -r property; do
        [ -n "$property" ] || continue
        current="$(xfconf-query -c xfce4-desktop -p "$property" 2>/dev/null || true)"
        if [ "$current" != "$selected" ]; then
            xfconf-query -c xfce4-desktop -p "$property" -s "$selected" 2>/dev/null || true
            style_property="${property%/last-image}/image-style"
            xfconf-query -c xfce4-desktop -p "$style_property" -s 5 2>/dev/null || true
            changed=true
        fi
    done <<PROPERTIES
$properties
PROPERTIES
    if [ "$changed" = true ]; then
        xfdesktop --reload >/dev/null 2>&1 || true
    fi
done
EOF
    chmod 755 /root/.local/bin/ominal-wallpaper-keeper

    if [ "$desktop_layout_changed" = true ]; then
        icon_cell=$((desktop_icon_size + 70))
        desktop_columns=$((geometry_width / icon_cell))
        [ "$desktop_columns" -ge 3 ] || desktop_columns=3
        desktop_column_offset=$(((desktop_columns - 3) / 2))
        desktop_bottom_row=$((geometry_height / icon_cell - 1))
        [ "$desktop_bottom_row" -ge 1 ] || desktop_bottom_row=1
        desktop_top_row=$((desktop_bottom_row - 1))
        cat >"$config_root/desktop/icons.screen0-${geometry_width}x${geometry_height}.rc" <<EOF
[xfdesktop-version-4.10.3+-rcfile_format]
4.10.3+=true

[$desktop_root/Files.desktop]
row=$desktop_top_row
col=$desktop_column_offset

[$desktop_root/Settings.desktop]
row=$desktop_top_row
col=$((desktop_column_offset + 2))

[$desktop_root/Editor.desktop]
row=$desktop_bottom_row
col=$desktop_column_offset

[$desktop_root/Terminal.desktop]
row=$desktop_bottom_row
col=$((desktop_column_offset + 1))

[$desktop_root/Firefox.desktop]
row=$desktop_bottom_row
col=$((desktop_column_offset + 2))
EOF
    fi

    cat >"$rules_root/ominal-mobile.lua" <<'EOF'
-- Window placement is owned by ominal-geometry-keeper so dialogs are
-- classified before any maximize action is applied.
EOF
cat >/root/.local/bin/ominal-home <<'EOF'
#!/bin/sh
DISPLAY="${DISPLAY:-:20}" wmctrl -k on
sleep 0.05
set -- $(DISPLAY="${DISPLAY:-:20}" xdotool getdisplaygeometry 2>/dev/null || true)
if [ "$#" -eq 2 ]; then
    DISPLAY="${DISPLAY:-:20}" xdotool mousemove "$(($1 / 2))" "$(($2 / 2))"
fi
EOF
    chmod 755 /root/.local/bin/ominal-home
    cat >/root/.local/bin/ominal-geometry-keeper <<'EOF'
#!/bin/sh
last_geometry=
dock_tick=0
is_integer() {
    case "${1:-}" in
      ''|'-'|*[!0-9-]*) return 1 ;;
      *) return 0 ;;
    esac
}
read_workarea() {
    work_x=0
    work_y=0
    work_width="$screen_width"
    work_height="$screen_height"
    net_workarea="$(xprop -root _NET_WORKAREA 2>/dev/null \
        | sed -n 's/^[^=]*=[[:space:]]*//p' | head -n 1 | tr ',' ' ')"
    set -- $net_workarea
    if [ "$#" -ge 4 ] && is_integer "$1" && is_integer "$2" \
        && is_integer "$3" && is_integer "$4" \
        && [ "$3" -gt 0 ] && [ "$4" -gt 0 ] \
        && [ "$3" -le "$screen_width" ] && [ "$4" -le "$screen_height" ]; then
        work_x="$1"
        work_y="$2"
        work_width="$3"
        work_height="$4"
    fi
}
constrain_window() {
    window_id="$1"
    window_bounds="$(read_window_bounds "$window_id")"
    set -- $window_bounds
    [ "$#" -eq 4 ] || return
    is_integer "$1" && is_integer "$2" && is_integer "$3" && is_integer "$4" || return
    window_right=$(($1 + $3))
    window_bottom=$(($2 + $4))
    work_right=$((work_x + work_width))
    work_bottom=$((work_y + work_height))
    tolerance=$((16 * ${OMINAL_UI_SCALE:-1}))
    if [ "$1" -lt $((work_x - tolerance)) ] \
        || [ "$2" -lt $((work_y - tolerance)) ] \
        || [ "$window_right" -gt $((work_right + tolerance)) ] \
        || [ "$window_bottom" -gt $((work_bottom + tolerance)) ]; then
        wmctrl -i -r "$window_id" -b remove,maximized_vert,maximized_horz \
            2>/dev/null || true
        wmctrl -i -r "$window_id" -e "0,$work_x,$work_y,$work_width,$work_height" \
            2>/dev/null || true
        wmctrl -i -r "$window_id" -b add,maximized_vert,maximized_horz \
            2>/dev/null || true
    fi
}
read_window_bounds() {
    xwininfo -id "$1" 2>/dev/null | awk '
        /Absolute upper-left X:/ { x = $4 }
        /Absolute upper-left Y:/ { y = $4 }
        /^[[:space:]]*Width:/ { width = $2 }
        /^[[:space:]]*Height:/ { height = $2 }
        END {
            if (width != "" && height != "") print x, y, width, height
        }'
}
normalize_window_hints() {
    window_id="$1"
    min_size="$(xprop -id "$window_id" WM_NORMAL_HINTS 2>/dev/null \
        | awk '/minimum size:/ {print $(NF - 2), $NF; exit}')"
    set -- $min_size
    min_width="${1:-0}"
    min_height="${2:-0}"
    case "$min_width:$min_height" in
      *[!0-9:]*|'':*) return ;;
    esac
    if [ "$min_width" -gt "$work_width" ] || [ "$min_height" -gt "$work_height" ]; then
        xprop -id "$window_id" -remove WM_NORMAL_HINTS 2>/dev/null || true
    fi
}
place_dialog() {
    window_id="$1"
    window_bounds="$(read_window_bounds "$window_id")"
    set -- $window_bounds
    [ "$#" -eq 4 ] || return
    is_integer "$1" && is_integer "$2" && is_integer "$3" && is_integer "$4" || return
    current_x="$1"
    current_y="$2"
    current_width="$3"
    current_height="$4"
    tolerance=$((16 * ${OMINAL_UI_SCALE:-1}))
    margin=$((12 * ${OMINAL_UI_SCALE:-1}))
    available_width=$((work_width - margin * 2))
    available_height=$((work_height - margin * 2))
    [ "$available_width" -gt 0 ] || available_width="$work_width"
    [ "$available_height" -gt 0 ] || available_height="$work_height"
    target_outer_width="$current_width"
    target_outer_height="$current_height"
    [ "$target_outer_width" -le "$available_width" ] || target_outer_width="$available_width"
    [ "$target_outer_height" -le "$available_height" ] || target_outer_height="$available_height"

    center_x=$((work_x + work_width / 2))
    center_y=$((work_y + work_height / 2))
    parent_id="$(xprop -id "$window_id" WM_TRANSIENT_FOR 2>/dev/null \
        | sed -n 's/.*window id #[[:space:]]*\(0x[0-9a-fA-F]*\).*/\1/p')"
    if [ -n "$parent_id" ]; then
        parent_bounds="$(read_window_bounds "$parent_id")"
        set -- $parent_bounds
        if [ "$#" -eq 4 ] && is_integer "$1" && is_integer "$2" \
            && is_integer "$3" && is_integer "$4"; then
            center_x=$(($1 + $3 / 2))
            center_y=$(($2 + $4 / 2))
        fi
    fi
    target_x=$((center_x - target_outer_width / 2))
    target_y=$((center_y - target_outer_height / 2))
    min_x=$((work_x + margin))
    min_y=$((work_y + margin))
    max_x=$((work_x + work_width - margin - target_outer_width))
    max_y=$((work_y + work_height - margin - target_outer_height))
    [ "$target_x" -ge "$min_x" ] || target_x="$min_x"
    [ "$target_y" -ge "$min_y" ] || target_y="$min_y"
    [ "$target_x" -le "$max_x" ] || target_x="$max_x"
    [ "$target_y" -le "$max_y" ] || target_y="$max_y"

    if [ "$current_x" -ge $((target_x - tolerance)) ] \
        && [ "$current_x" -le $((target_x + tolerance)) ] \
        && [ "$current_y" -ge $((target_y - tolerance)) ] \
        && [ "$current_y" -le $((target_y + tolerance)) ] \
        && [ "$current_width" -ge $((target_outer_width - tolerance)) ] \
        && [ "$current_width" -le $((target_outer_width + tolerance)) ] \
        && [ "$current_height" -ge $((target_outer_height - tolerance)) ] \
        && [ "$current_height" -le $((target_outer_height + tolerance)) ]; then
        return
    fi

    target_client_width="$target_outer_width"
    target_client_height="$target_outer_height"
    frame_extents="$(xprop -id "$window_id" _NET_FRAME_EXTENTS 2>/dev/null \
        | sed -n 's/^[^=]*=[[:space:]]*//p' | tr ',' ' ')"
    set -- $frame_extents
    if [ "$#" -ge 4 ] && is_integer "$1" && is_integer "$2" \
        && is_integer "$3" && is_integer "$4"; then
        frame_horizontal=$(($1 + $2))
        frame_vertical=$(($3 + $4))
        if [ "$frame_horizontal" -lt "$target_client_width" ]; then
            target_client_width=$((target_client_width - frame_horizontal))
        fi
        if [ "$frame_vertical" -lt "$target_client_height" ]; then
            target_client_height=$((target_client_height - frame_vertical))
        fi
    fi
    [ "$target_client_width" -gt 0 ] || target_client_width=1
    [ "$target_client_height" -gt 0 ] || target_client_height=1

    wmctrl -i -r "$window_id" -b remove,fullscreen,maximized_vert,maximized_horz \
        2>/dev/null || true
    wmctrl -i -r "$window_id" -e "0,$target_x,$target_y,$target_client_width,$target_client_height" \
        2>/dev/null || true
    wmctrl -i -r "$window_id" -b add,above 2>/dev/null || true
}
raise_docks() {
    for dock_id in $(wmctrl -l 2>/dev/null | awk '{print $1}'); do
        dock_type="$(xprop -id "$dock_id" _NET_WM_WINDOW_TYPE 2>/dev/null || true)"
        case "$dock_type" in
          *_NET_WM_WINDOW_TYPE_DOCK*) xdotool windowraise "$dock_id" 2>/dev/null || true ;;
        esac
    done
}
while sleep 0.15; do
    geometry="$(xdpyinfo 2>/dev/null | awk '/dimensions:/ {print $2; exit}')"
    [ -n "$geometry" ] || continue
    geometry_changed=false
    if [ "$geometry" != "$last_geometry" ]; then
        last_geometry="$geometry"
        dock_tick=7
        geometry_changed=true
    fi
    dock_tick=$((dock_tick + 1))
    raise_dock_now=false
    if [ "$dock_tick" -ge 7 ]; then
        dock_tick=0
        raise_dock_now=true
    fi
    screen_width="${geometry%x*}"
    screen_height="${geometry#*x}"
    read_workarea
    showing_desktop="$(xprop -root _NET_SHOWING_DESKTOP 2>/dev/null || true)"
    case "$showing_desktop" in
      *"= 1"*)
        if [ "$raise_dock_now" = true ]; then
            raise_docks
        fi
        continue
        ;;
    esac
    for window_id in $(wmctrl -l 2>/dev/null | awk '{print $1}'); do
        window_type="$(xprop -id "$window_id" _NET_WM_WINDOW_TYPE 2>/dev/null || true)"
        window_state="$(xprop -id "$window_id" _NET_WM_STATE 2>/dev/null || true)"
        transient_for="$(xprop -id "$window_id" WM_TRANSIENT_FOR 2>/dev/null || true)"
        window_class="$(xprop -id "$window_id" WM_CLASS 2>/dev/null || true)"
        window_role="$(xprop -id "$window_id" WM_WINDOW_ROLE 2>/dev/null \
            | tr '[:upper:]' '[:lower:]' || true)"
        dialog_window=false
        case "$window_type" in
          *_NET_WM_WINDOW_TYPE_DIALOG*|*_NET_WM_WINDOW_TYPE_UTILITY*|*_NET_WM_WINDOW_TYPE_SPLASH*)
            dialog_window=true ;;
        esac
        case "$window_state" in *_NET_WM_STATE_MODAL*) dialog_window=true ;; esac
        case "$transient_for" in *"window id #"*) dialog_window=true ;; esac
        case "$window_class" in
          *'"yad", "Yad"'*|*'"zenity", "Zenity"'*) dialog_window=true ;;
        esac
        case "$window_role" in
          *dialog*|*popup*|*prompt*|*chooser*) dialog_window=true ;;
        esac
        case "$window_type" in
          *_NET_WM_WINDOW_TYPE_NORMAL*|*_NET_WM_WINDOW_TYPE_DIALOG*|*_NET_WM_WINDOW_TYPE_UTILITY*|*_NET_WM_WINDOW_TYPE_SPLASH*)
            normalize_window_hints "$window_id"
            if [ "$dialog_window" = true ]; then
                place_dialog "$window_id"
                continue
            fi
            case "$window_state" in
              *MAXIMIZED_HORZ*MAXIMIZED_VERT*|*MAXIMIZED_VERT*MAXIMIZED_HORZ*)
                if [ "$geometry_changed" = true ]; then
                    wmctrl -i -r "$window_id" -b remove,maximized_vert,maximized_horz \
                        2>/dev/null || true
                    wmctrl -i -r "$window_id" -b add,maximized_vert,maximized_horz \
                        2>/dev/null || true
                fi
                ;;
              *)
                wmctrl -i -r "$window_id" -b add,maximized_vert,maximized_horz \
                    2>/dev/null || true
                raise_docks
                ;;
            esac
            constrain_window "$window_id"
            ;;
          *_NET_WM_WINDOW_TYPE_DOCK*)
            if [ "$raise_dock_now" = true ]; then
                wmctrl -i -r "$window_id" -b add,above,sticky 2>/dev/null || true
                xdotool windowraise "$window_id" 2>/dev/null || true
            fi
            ;;
        esac
    done
done
EOF
    chmod 755 /root/.local/bin/ominal-geometry-keeper
    cat >"$autostart_root/ominal-mobile-windows.desktop" <<EOF
[Desktop Entry]
Type=Application
Name=Mobile window policy
Exec=devilspie2 --folder $rules_root
OnlyShowIn=XFCE;
NoDisplay=true
X-GNOME-Autostart-enabled=true
EOF
    cat >"$autostart_root/ominal-geometry.desktop" <<'EOF'
[Desktop Entry]
Type=Application
Name=Responsive display geometry
Exec=/root/.local/bin/ominal-geometry-keeper
OnlyShowIn=XFCE;
NoDisplay=true
X-GNOME-Autostart-enabled=true
EOF
    cat >"$autostart_root/ominal-hide-pointer.desktop" <<'EOF'
[Desktop Entry]
Type=Application
Name=Touch pointer policy
Exec=unclutter-xfixes --timeout 0.1 --jitter 1 --hide-on-touch
OnlyShowIn=XFCE;
NoDisplay=true
X-GNOME-Autostart-enabled=true
EOF
    cat >"$autostart_root/ominal-wallpaper.desktop" <<'EOF'
[Desktop Entry]
Type=Application
Name=Wallpaper persistence
Exec=/root/.local/bin/ominal-wallpaper-keeper
OnlyShowIn=XFCE;
NoDisplay=true
X-GNOME-Autostart-enabled=true
EOF
}

write_jwm_config() {
    cat >/root/.jwmrc <<EOF
<?xml version="1.0"?>
<JWM>
  <RootMenu onroot="1" height="$menu_item_height" labeled="false">
    <Program icon="$files_icon" label="Files">/root/.local/bin/ominal-files</Program>
    <Program icon="$terminal_icon" label="Terminal">/root/.local/bin/ominal-terminal</Program>
    <Program icon="$editor_icon" label="Editor">/root/.local/bin/ominal-editor</Program>
    <Program icon="$browser_icon" label="Firefox">/root/.local/bin/ominal-browser</Program>
    <Program icon="$settings_icon" label="Settings">/root/.local/bin/ominal-settings</Program>
  </RootMenu>
  <Group>
    <Option>maximized</Option>
    <Option>notitle</Option>
    <Option>noborder</Option>
  </Group>
  <WindowStyle decorations="flat">
    <Font>Sans-$wm_font_size</Font>
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
    <Font>Sans-$wm_font_size</Font>
    <Foreground>#F4F5F7</Foreground>
    <Background>#000000</Background>
    <Outline>#000000</Outline>
  </TrayStyle>
  <TrayButtonStyle>
    <Font>Sans-$wm_font_size</Font>
    <Foreground>#F4F5F7</Foreground>
    <Background>#000000</Background>
    <Active>
      <Foreground>#FFFFFF</Foreground>
      <Background>#121214</Background>
    </Active>
  </TrayButtonStyle>
  <MenuStyle>
    <Font>Sans-$wm_font_size</Font>
    <Foreground>#F4F5F7</Foreground>
    <Background>#000000</Background>
    <Active>
      <Foreground>#FFFFFF</Foreground>
      <Background>#1F1F21</Background>
    </Active>
    <Outline>#2C2C2E</Outline>
  </MenuStyle>
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
}

start_jwm() {
    stop_xfce
    write_jwm_config
    if ! jwm -p >"$display_dir/jwm-parse.log" 2>&1; then
        cat "$display_dir/jwm-parse.log" >&2
        return 1
    fi
    printf '%s\n' jwm >"$display_dir/active-session"
    if [ "$launch_mode" = background ]; then
        nohup jwm >"$display_dir/jwm.log" 2>&1 &
        return 0
    fi
    exec jwm >"$display_dir/jwm.log" 2>&1
}

start_xfce() {
    for command_name in xfce4-session xfwm4 xfce4-panel xfdesktop devilspie2 unclutter-xfixes; do
        command -v "$command_name" >/dev/null 2>&1 || return 1
    done
    configure_xfce
    pkill -x jwm 2>/dev/null || true
    if [ "$launch_mode" != background ]; then
        printf '%s\n' xfce >"$display_dir/active-session"
        if dbus-run-session -- xfce4-session >"$display_dir/xfce.log" 2>&1; then
            return 0
        fi
        return 1
    fi

    nohup dbus-run-session -- xfce4-session >"$display_dir/xfce.log" 2>&1 &
    session_pid=$!
    for attempt in $(seq 1 50); do
        if pgrep -x xfwm4 >/dev/null 2>&1 && pgrep -x xfce4-panel >/dev/null 2>&1; then
            printf '%s\n' xfce >"$display_dir/active-session"
            return 0
        fi
        if ! kill -0 "$session_pid" 2>/dev/null; then
            break
        fi
        sleep 0.1
    done
    kill "$session_pid" 2>/dev/null || true
    stop_xfce
    return 1
}

case "$session_mode" in
    xfce)
        if start_xfce; then
            exit 0
        fi
        printf '%s\n' "XFCE failed to start; using recovery desktop." >&2
        start_jwm
        ;;
    jwm)
        start_jwm
        ;;
    *)
        printf 'Unsupported desktop session: %s\n' "$session_mode" >&2
        exit 64
        ;;
esac
