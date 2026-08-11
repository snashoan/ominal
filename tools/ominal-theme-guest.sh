#!/bin/sh
set -eu

THEMES_DIR="${OMINAL_THEMES_DIR:-/root/.ominal/themes}"
ACTIVE_FILE="$THEMES_DIR/active"

usage() {
    printf '%s\n' \
        'usage: ominal-theme list|current|show [THEME]|create THEME [LABEL]|set THEME KEY VALUE|use THEME|reset|delete THEME' >&2
    exit 64
}

valid_theme_id() {
    case "$1" in
        ''|*[!a-z0-9_-]*|[-_]*) return 1 ;;
        *) [ "${#1}" -le 32 ] ;;
    esac
}

theme_path() {
    valid_theme_id "$1" || usage
    printf '%s/%s.properties\n' "$THEMES_DIR" "$1"
}

current_theme() {
    if [ -s "$ACTIVE_FILE" ]; then
        IFS= read -r selected < "$ACTIVE_FILE"
        if [ "$selected" = default ] || valid_theme_id "$selected"; then
            printf '%s\n' "$selected"
            return
        fi
    fi
    printf '%s\n' default
}

reload_ui() {
    if command -v ominal-event >/dev/null 2>&1; then
        ominal-event reload-ui "Theme changed" >/dev/null
    fi
}

mkdir -p "$THEMES_DIR"
command="${1:-}"
[ "$#" -eq 0 ] || shift

case "$command" in
    list)
        active="$(current_theme)"
        [ "$active" = default ] && printf '%s\n' '* default'
        for file in "$THEMES_DIR"/*.properties; do
            [ -f "$file" ] || continue
            name="${file##*/}"
            name="${name%.properties}"
            [ "$name" = "$active" ] && prefix='*' || prefix=' '
            printf '%s %s\n' "$prefix" "$name"
        done
        ;;
    current)
        current_theme
        ;;
    show)
        theme="${1:-$(current_theme)}"
        [ "$theme" != default ] || {
            printf '%s\n' 'default is built into the app and is immutable'
            exit 0
        }
        file="$(theme_path "$theme")"
        [ -f "$file" ] || { printf 'Unknown theme: %s\n' "$theme" >&2; exit 66; }
        cat "$file"
        ;;
    create)
        theme="${1:-}"
        label="${2:-$theme}"
        file="$(theme_path "$theme")"
        [ ! -e "$file" ] || { printf 'Theme already exists: %s\n' "$theme" >&2; exit 73; }
        template="$THEMES_DIR/custom.properties"
        if [ -f "$template" ]; then
            cp "$template" "$file"
        else
            printf '%s\n' 'ui.version=monolith-custom-v1' > "$file"
        fi
        printf '\ntheme.id=%s\ntheme.name=%s\ntheme.enabled=true\n' \
            "$theme" "$label" >> "$file"
        printf '%s\n' "$file"
        ;;
    set)
        [ "$#" -ge 3 ] || usage
        theme="$1"
        key="$2"
        shift 2
        value="$*"
        case "$key" in ''|*[!A-Za-z0-9._-]*) usage ;; esac
        case "$value" in *'\n'*|*'\r'*) usage ;; esac
        file="$(theme_path "$theme")"
        [ -f "$file" ] || { printf 'Unknown theme: %s\n' "$theme" >&2; exit 66; }
        printf '%s=%s\n' "$key" "$value" >> "$file"
        if [ "$(current_theme)" = "$theme" ]; then
            reload_ui
        fi
        ;;
    use)
        theme="${1:-}"
        valid_theme_id "$theme" || usage
        file="$(theme_path "$theme")"
        [ -f "$file" ] || { printf 'Unknown theme: %s\n' "$theme" >&2; exit 66; }
        temporary="$ACTIVE_FILE.tmp.$$"
        printf '%s\n' "$theme" > "$temporary"
        mv "$temporary" "$ACTIVE_FILE"
        reload_ui
        ;;
    reset)
        temporary="$ACTIVE_FILE.tmp.$$"
        printf '%s\n' default > "$temporary"
        mv "$temporary" "$ACTIVE_FILE"
        reload_ui
        ;;
    delete)
        theme="${1:-}"
        valid_theme_id "$theme" || usage
        [ "$theme" != custom ] || { printf '%s\n' 'The custom template is immutable.' >&2; exit 77; }
        [ "$(current_theme)" != "$theme" ] || {
            printf '%s\n' 'Activate another theme before deleting this one.' >&2
            exit 77
        }
        file="$(theme_path "$theme")"
        [ -f "$file" ] || { printf 'Unknown theme: %s\n' "$theme" >&2; exit 66; }
        rm -f "$file"
        ;;
    *) usage ;;
esac
