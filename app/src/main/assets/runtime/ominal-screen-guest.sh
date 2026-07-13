#!/bin/sh
set -eu

export DISPLAY="${DISPLAY:-${OMINAL_DISPLAY:-:20}}"
action="${1:-help}"
[ "$#" -eq 0 ] || shift

case "$action" in
    screenshot|shot)
        output="${1:-/root/workspace/.ominal-screen.png}"
        mkdir -p "$(dirname "$output")"
        rm -f "$output"
        scrot "$output"
        printf '%s\n' "$output"
        ;;
    tap)
        [ "$#" -eq 2 ] || { printf '%s\n' 'usage: ominal-screen tap X Y' >&2; exit 64; }
        xdotool mousemove --sync "$1" "$2" click 1
        ;;
    double-tap)
        [ "$#" -eq 2 ] || { printf '%s\n' 'usage: ominal-screen double-tap X Y' >&2; exit 64; }
        xdotool mousemove --sync "$1" "$2" click --repeat 2 --delay 120 1
        ;;
    type)
        [ "$#" -gt 0 ] || { printf '%s\n' 'usage: ominal-screen type TEXT' >&2; exit 64; }
        xdotool type --clearmodifiers --delay 20 -- "$*"
        ;;
    key)
        [ "$#" -gt 0 ] || { printf '%s\n' 'usage: ominal-screen key KEY...' >&2; exit 64; }
        xdotool key --clearmodifiers "$@"
        ;;
    windows)
        wmctrl -lx
        ;;
    focus)
        [ "$#" -eq 1 ] || { printf '%s\n' 'usage: ominal-screen focus WINDOW_ID' >&2; exit 64; }
        wmctrl -ia "$1"
        ;;
    close)
        [ "$#" -eq 1 ] || { printf '%s\n' 'usage: ominal-screen close WINDOW_ID' >&2; exit 64; }
        wmctrl -ic "$1"
        ;;
    size)
        xdpyinfo | awk '/dimensions:/{print $2; exit}'
        ;;
    help|--help|-h)
        printf '%s\n' \
            'ominal-screen screenshot [FILE]' \
            'ominal-screen tap X Y' \
            'ominal-screen double-tap X Y' \
            'ominal-screen type TEXT' \
            'ominal-screen key KEY...' \
            'ominal-screen windows' \
            'ominal-screen focus WINDOW_ID' \
            'ominal-screen close WINDOW_ID' \
            'ominal-screen size'
        ;;
    *)
        printf 'Unknown screen action: %s\n' "$action" >&2
        exit 64
        ;;
esac
