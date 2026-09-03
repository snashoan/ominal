#!/data/data/com.ominal/files/usr/bin/sh
set -eu

PREFIX="${OMINAL_PREFIX:-/data/data/com.ominal/files/usr}"
HOME="${OMINAL_HOME:-/data/data/com.ominal/files/home}"
RUNTIME_ROOT="${OMINAL_RUNTIME_ROOT:-$HOME/.ominal/runtime}"
PROOT_BIN="$PREFIX/bin/proot"
PROOT_LOADER="${PROOT_LOADER:-$PREFIX/libexec/proot/loader}"
ROOTFS="$RUNTIME_ROOT/linux/rootfs"
ROOTFS_READY="$ROOTFS/.ominal-rootfs-ready"
WORKSPACE="${OMINAL_WORKDIR:-$HOME/workspace}"
CODEX_HOME="${OMINAL_CODEX_HOME:-$HOME/.ominal/codex}"
CLAUDE_HOME="${OMINAL_CLAUDE_HOME:-$HOME/.ominal/harnesses/claude}"
ANTIGRAVITY_HOME="${OMINAL_ANTIGRAVITY_HOME:-$HOME/.ominal/harnesses/antigravity}"
CAPABILITIES_HOME="${OMINAL_CAPABILITIES_HOME:-$HOME/.ominal/harness-capabilities}"
HARNESS_REGISTRY_HOME="${GIR_HARNESS_REGISTRY_HOME:-$HOME/.ominal/harness-registry}"
UI_THEME_HOME="${OMINAL_UI_THEME_HOME:-$HOME/.ominal/themes}"
USER_PROFILE_FILE="${OMINAL_USER_PROFILE_FILE:-$HOME/.ominal/profile.json}"
PROOT_ID="${OMINAL_PROOT_ID:-0:0}"
XDG_OPEN_BRIDGE="$PREFIX/bin/ominal-xdg-open-guest"
HARNESS_DISCOVER_BRIDGE="$PREFIX/bin/ominal-harness-discover"
BRAND_WALLPAPER="$PREFIX/share/gir/gir-final-wallpaper.png"
SHM_DIR="$RUNTIME_ROOT/shm"
BRIDGE_DIR="$HOME/.ominal/bridge"
HOST_HOME="$HOME"

if [ ! -x "$PROOT_BIN" ] || [ ! -x "$PROOT_LOADER" ] || [ ! -f "$ROOTFS_READY" ]; then
    printf '%s\n' 'Ominal Linux runtime is not installed yet.' >&2
    exit 69
fi
export PROOT_LOADER

case "$PROOT_ID" in
    *[!0-9:]*|*:*:*|:|:*|*:)
        printf 'Invalid GIR Linux identity: %s\n' "$PROOT_ID" >&2
        exit 64
        ;;
    *:*) ;;
    *) PROOT_ID="$PROOT_ID:$PROOT_ID" ;;
esac

case "$WORKSPACE" in
    /data/user/0/com.ominal/*)
        WORKSPACE="/data/data/com.ominal${WORKSPACE#/data/user/0/com.ominal}"
        ;;
esac

mkdir -p "$RUNTIME_ROOT/tmp" "$SHM_DIR" "$BRIDGE_DIR" "$ROOTFS/.l2s" "$WORKSPACE" \
    "$ROOTFS/root/workspace" "$ROOTFS/root/.codex" "$ROOTFS/root/.claude" \
    "$ROOTFS/root/.gemini" "$ROOTFS/root/.ominal/harness-capabilities" \
    "$ROOTFS/root/.ominal/harness-registry" \
    "$ROOTFS/root/.ominal/themes" "$ROOTFS/root/.ominal" \
    "$CODEX_HOME" "$CLAUDE_HOME" "$ANTIGRAVITY_HOME" "$CAPABILITIES_HOME" \
    "$HARNESS_REGISTRY_HOME" \
    "$UI_THEME_HOME"
chmod 1777 "$SHM_DIR"
chmod 700 "$BRIDGE_DIR"
if [ ! -f "$USER_PROFILE_FILE" ]; then
    printf '%s\n' '{"schemaVersion":1,"canonicalStorage":"device","scope":"shared-across-runtimes","available":false,"fields":{}}' \
        > "$USER_PROFILE_FILE"
    chmod 600 "$USER_PROFILE_FILE"
fi
if [ ! -e "$ROOTFS/root/.ominal/profile.json" ]; then
    : > "$ROOTFS/root/.ominal/profile.json"
fi

hosts_file="$ROOTFS/etc/hosts"
hosts_has_localhost=false
if [ -f "$hosts_file" ]; then
    while IFS= read -r hosts_line; do
        case "$hosts_line" in
            127.0.0.1*localhost*)
                hosts_has_localhost=true
                break
                ;;
        esac
    done < "$hosts_file"
fi
if [ "$hosts_has_localhost" = false ]; then
    printf '%s\n' '127.0.0.1 localhost' >> "$hosts_file"
fi

if [ ! -e "$CODEX_HOME/auth.json" ] && [ ! -e "$CODEX_HOME/config.toml" ]; then
    for legacy_home in \
        "$RUNTIME_ROOT/linux/rootfs.previous/root/.codex" \
        "$ROOTFS/root/.codex"; do
        if [ -d "$legacy_home" ]; then
            /system/bin/cp -R "$legacy_home/." "$CODEX_HOME/" 2>/dev/null || true
        fi
        if [ -e "$CODEX_HOME/auth.json" ] || [ -e "$CODEX_HOME/config.toml" ]; then
            break
        fi
    done
fi

export PREFIX HOME
export PROOT_TMP_DIR="$RUNTIME_ROOT/tmp"
export PROOT_L2S_DIR="$ROOTFS/.l2s"
export PATH=/root/.local/bin:/root/.ominal/npm/bin:/root/.ominal/node/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
if [ "$PROOT_ID" = "0:0" ]; then
    GUEST_USER=root
else
    GUEST_USER=ominal
fi
export HOME=/root USER="$GUEST_USER" LOGNAME="$GUEST_USER" TMPDIR=/tmp

ui_dpi="${OMINAL_DISPLAY_DPI:-320}"
case "$ui_dpi" in *[!0-9]*|'') ui_dpi=320 ;; esac
ui_scale=$(((ui_dpi + 159) / 160))
[ "$ui_scale" -ge 1 ] || ui_scale=1
[ "$ui_scale" -le 3 ] || ui_scale=3
export OMINAL_DISPLAY_DPI="$ui_dpi" OMINAL_UI_SCALE="$ui_scale"
export GDK_SCALE="$ui_scale" GDK_DPI_SCALE=0.56 GTK_OVERLAY_SCROLLING=0
export QT_SCALE_FACTOR="$ui_scale" QT_FONT_DPI=106 QT_AUTO_SCREEN_SCALE_FACTOR=0
export QT_ENABLE_HIGHDPI_SCALING=1 QT_SCALE_FACTOR_ROUNDING_POLICY=PassThrough
export ELM_SCALE="$ui_scale" FLTK_SCALING_FACTOR="$ui_scale" WINIT_X11_SCALE_FACTOR="$ui_scale"
export SAL_FORCEDPI="$ui_dpi"
export MOZ_ENABLE_WAYLAND=0 MOZ_USE_XINPUT2=1

if [ "$#" -eq 0 ]; then
    set -- /bin/bash
fi

for host_config in .bashrc .bash_profile .profile .zshrc .zprofile .gitconfig; do
    if [ -f "$HOST_HOME/$host_config" ]; then
        set -- -b "$HOST_HOME/$host_config:/root/$host_config" "$@"
    fi
done
if [ -d "$HOST_HOME/.ssh" ]; then
    set -- -b "$HOST_HOME/.ssh:/root/.ssh" "$@"
fi
if [ -d "$HOST_HOME/.config/git" ]; then
    set -- -b "$HOST_HOME/.config/git:/root/.config/git" "$@"
fi
if [ -x "$XDG_OPEN_BRIDGE" ]; then
    if [ ! -e "$ROOTFS/usr/local/bin/xdg-open" ]; then
        mkdir -p "$ROOTFS/usr/local/bin"
        : > "$ROOTFS/usr/local/bin/xdg-open"
    fi
    set -- -b "$XDG_OPEN_BRIDGE:/usr/local/bin/xdg-open" "$@"
fi
if [ ! -d "$ROOTFS/run/ominal" ]; then
    mkdir -p "$ROOTFS/run/ominal"
fi
set -- -b "$BRIDGE_DIR:/run/ominal" "$@"
if [ -x "$HARNESS_DISCOVER_BRIDGE" ]; then
    if [ ! -e "$ROOTFS/usr/local/bin/ominal-harness-discover" ]; then
        mkdir -p "$ROOTFS/usr/local/bin"
        : > "$ROOTFS/usr/local/bin/ominal-harness-discover"
    fi
    set -- -b "$HARNESS_DISCOVER_BRIDGE:/usr/local/bin/ominal-harness-discover" "$@"
fi
if [ -f "$BRAND_WALLPAPER" ]; then
    mkdir -p "$ROOTFS/usr/local/share/gir"
    [ -e "$ROOTFS/usr/local/share/gir/gir-final-wallpaper.png" ] \
        || : > "$ROOTFS/usr/local/share/gir/gir-final-wallpaper.png"
    set -- -b "$BRAND_WALLPAPER:/usr/local/share/gir/gir-final-wallpaper.png" "$@"
fi

# The Android-side exec bridge is required to start PRoot from app storage,
# but it must never enter the Linux guest where it would intercept guest execve.
unset LD_PRELOAD

exec "$PROOT_BIN" --link2symlink --sysvipc -i "$PROOT_ID" -r "$ROOTFS" \
    -b /dev -b "$SHM_DIR:/dev/shm" -b /proc -b /sys -b "$RUNTIME_ROOT/tmp:/tmp" \
    -b "$CODEX_HOME:/root/.codex" -b "$CLAUDE_HOME:/root/.claude" \
    -b "$ANTIGRAVITY_HOME:/root/.gemini" \
    -b "$CAPABILITIES_HOME:/root/.ominal/harness-capabilities" \
    -b "$HARNESS_REGISTRY_HOME:/root/.ominal/harness-registry" \
    -b "$UI_THEME_HOME:/root/.ominal/themes" \
    -b "$USER_PROFILE_FILE:/root/.ominal/profile.json" \
    -b "$WORKSPACE:/root/workspace" \
    -w /root/workspace "$@"
