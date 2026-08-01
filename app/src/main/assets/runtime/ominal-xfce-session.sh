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
jwm_dock_spacer=$(((geometry_width - bottom_bar_height * 5) / 2))
[ "$jwm_top_spacer" -ge 0 ] || jwm_top_spacer=0
[ "$jwm_dock_spacer" -ge 0 ] || jwm_dock_spacer=0

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
    icon_size=$((bottom_bar_height * 58 / 100))
    [ "$icon_size" -ge 40 ] || icon_size=40
    [ "$icon_size" -le 52 ] || icon_size=52

    rm -rf "$panel_root"
    mkdir -p "$channel_root" "$panel_root" "$desktop_root" "$autostart_root" "$rules_root"
    rm -rf /root/.cache/sessions

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
    <property name="titleless_maximize" type="bool" value="false"/>
    <property name="borderless_maximize" type="bool" value="false"/>
    <property name="easy_click" type="string" value="None"/>
    <property name="workspace_count" type="int" value="1"/>
    <property name="wrap_windows" type="bool" value="false"/>
    <property name="wrap_workspaces" type="bool" value="false"/>
  </property>
</channel>
EOF

    if [ ! -f "$channel_root/xfce4-desktop.xml" ]; then
        cat >"$channel_root/xfce4-desktop.xml" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<channel name="xfce4-desktop" version="1.0">
  <property name="desktop-icons" type="empty">
    <property name="style" type="int" value="2"/>
    <property name="icon-size" type="uint" value="$icon_size"/>
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

    cat >"$channel_root/xfce4-panel.xml" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<channel name="xfce4-panel" version="1.0">
  <property name="configver" type="int" value="2"/>
  <property name="panels" type="array">
    <value type="int" value="2"/>
    <property name="panel-2" type="empty">
      <property name="position" type="string" value="p=10;x=0;y=0"/>
      <property name="length" type="uint" value="100"/>
      <property name="position-locked" type="bool" value="true"/>
      <property name="size" type="uint" value="$bottom_bar_height"/>
      <property name="nrows" type="uint" value="1"/>
      <property name="autohide-behavior" type="uint" value="0"/>
      <property name="disable-struts" type="bool" value="false"/>
      <property name="background-style" type="uint" value="1"/>
      <property name="background-rgba" type="array">
        <value type="double" value="0.031"/>
        <value type="double" value="0.031"/>
        <value type="double" value="0.039"/>
        <value type="double" value="1"/>
      </property>
      <property name="enter-opacity" type="uint" value="100"/>
      <property name="leave-opacity" type="uint" value="100"/>
      <property name="icon-size" type="uint" value="$icon_size"/>
      <property name="plugin-ids" type="array">
        <value type="int" value="20"/>
        <value type="int" value="26"/>
        <value type="int" value="27"/>
        <value type="int" value="28"/>
        <value type="int" value="21"/>
        <value type="int" value="22"/>
        <value type="int" value="23"/>
        <value type="int" value="24"/>
        <value type="int" value="25"/>
      </property>
    </property>
  </property>
  <property name="plugins" type="empty">
    <property name="plugin-20" type="string" value="separator">
      <property name="expand" type="bool" value="true"/>
      <property name="style" type="uint" value="0"/>
    </property>
    <property name="plugin-26" type="string" value="launcher">
      <property name="items" type="array">
        <value type="string" value="ominal-chat.desktop"/>
      </property>
    </property>
    <property name="plugin-27" type="string" value="launcher">
      <property name="items" type="array">
        <value type="string" value="ominal-home.desktop"/>
      </property>
    </property>
    <property name="plugin-28" type="string" value="tasklist">
      <property name="show-labels" type="bool" value="false"/>
      <property name="flat-buttons" type="bool" value="true"/>
      <property name="show-handle" type="bool" value="false"/>
      <property name="grouping" type="uint" value="1"/>
      <property name="sort-order" type="uint" value="1"/>
    </property>
    <property name="plugin-21" type="string" value="launcher">
      <property name="items" type="array">
        <value type="string" value="ominal-files.desktop"/>
      </property>
    </property>
    <property name="plugin-22" type="string" value="launcher">
      <property name="items" type="array">
        <value type="string" value="ominal-browser.desktop"/>
      </property>
    </property>
    <property name="plugin-23" type="string" value="launcher">
      <property name="items" type="array">
        <value type="string" value="ominal-terminal.desktop"/>
      </property>
    </property>
    <property name="plugin-24" type="string" value="launcher">
      <property name="items" type="array">
        <value type="string" value="ominal-settings.desktop"/>
      </property>
    </property>
    <property name="plugin-25" type="string" value="separator">
      <property name="expand" type="bool" value="true"/>
      <property name="style" type="uint" value="0"/>
    </property>
  </property>
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

    for launcher_id in 21 22 23 24 26 27; do
        mkdir -p "$panel_root/launcher-$launcher_id"
    done
    cp "$desktop_root/Files.desktop" "$panel_root/launcher-21/ominal-files.desktop"
    cp "$desktop_root/Firefox.desktop" "$panel_root/launcher-22/ominal-browser.desktop"
    cp "$desktop_root/Terminal.desktop" "$panel_root/launcher-23/ominal-terminal.desktop"
    cp "$desktop_root/Settings.desktop" "$panel_root/launcher-24/ominal-settings.desktop"
    write_desktop_entry "$panel_root/launcher-26/ominal-chat.desktop" "Chat" \
        /root/.local/bin/ominal-chat "$chat_icon" Utility
    write_desktop_entry "$panel_root/launcher-27/ominal-home.desktop" "Home" \
        /root/.local/bin/ominal-home user-home Utility

    cat >"$rules_root/ominal-mobile.lua" <<'EOF'
if get_window_type() == "WINDOW_TYPE_NORMAL" then
    maximize()
end
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
    work_height=$((screen_height - ${OMINAL_BOTTOM_BAR_HEIGHT:-0}))
    [ "$work_height" -gt 0 ] || work_height="$screen_height"
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
    window_bounds="$(xwininfo -id "$window_id" 2>/dev/null | awk '
        /Absolute upper-left X:/ { x = $4 }
        /Absolute upper-left Y:/ { y = $4 }
        /^[[:space:]]*Width:/ { width = $2 }
        /^[[:space:]]*Height:/ { height = $2 }
        END {
            if (width != "" && height != "") print x, y, width, height
        }')"
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
        case "$window_type" in
          *_NET_WM_WINDOW_TYPE_NORMAL*)
            min_size="$(xprop -id "$window_id" WM_NORMAL_HINTS 2>/dev/null \
                | awk '/minimum size:/ {print $(NF - 2), $NF; exit}')"
            set -- $min_size
            min_width="${1:-0}"
            min_height="${2:-0}"
            case "$min_width:$min_height" in
              *[!0-9:]*|'':*) ;;
              *)
                if [ "$min_width" -gt "$work_width" ] \
                    || [ "$min_height" -gt "$work_height" ]; then
                    xprop -id "$window_id" -remove WM_NORMAL_HINTS 2>/dev/null || true
                fi
                ;;
            esac
            window_state="$(xprop -id "$window_id" _NET_WM_STATE 2>/dev/null || true)"
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
  <Tray x="0" y="-1" width="0" height="$bottom_bar_height" autohide="off">
    <Spacer width="$jwm_dock_spacer"/>
    <TrayButton label="" icon="$chat_icon">exec:/root/.local/bin/ominal-chat</TrayButton>
    <TrayButton label="" icon="$files_icon">exec:/root/.local/bin/ominal-files</TrayButton>
    <TrayButton label="" icon="$browser_icon">exec:/root/.local/bin/ominal-browser</TrayButton>
    <TrayButton label="" icon="$terminal_icon">exec:/root/.local/bin/ominal-terminal</TrayButton>
    <TrayButton label="" icon="$settings_icon">exec:/root/.local/bin/ominal-settings</TrayButton>
  </Tray>
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
