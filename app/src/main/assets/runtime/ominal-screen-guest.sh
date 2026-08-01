#!/bin/sh
set -eu

export DISPLAY="${DISPLAY:-${OMINAL_DISPLAY:-:20}}"
action="${1:-help}"
[ "$#" -eq 0 ] || shift

display_ready() {
    timeout 1 xdpyinfo >/dev/null 2>&1
}

require_display() {
    if ! display_ready; then
        printf '{"state":"unavailable","display":"%s"}\n' "$DISPLAY" >&2
        exit 69
    fi
}

emit_agent_activity() {
    session="${OMINAL_AGENT_SESSION:-}"
    case "$session" in
        ''|*[!A-Za-z0-9._-]*) return ;;
    esac
    activity_file="${OMINAL_DISPLAY_ACTIVITY_LOG:-/tmp/ominal-display-activity.json}"
    temporary="${activity_file}.tmp.$$"
    printf '{"schemaVersion":1,"sessionId":"%s","action":"%s","timestamp":%s}\n' \
        "$session" "$action" "$(date +%s)" > "$temporary"
    mv -f "$temporary" "$activity_file"
}

case "$action" in
    help|--help|-h) ;;
    status)
        if display_ready; then
            dimensions="$(timeout 1 xdpyinfo | awk '/dimensions:/{print $2; exit}')"
            printf '{"state":"ready_idle","display":"%s","dimensions":"%s"}\n' \
                "$DISPLAY" "$dimensions"
            exit 0
        fi
        printf '{"state":"unavailable","display":"%s"}\n' "$DISPLAY" >&2
        exit 69
        ;;
    wait)
        timeout="${1:-20}"
        case "$timeout" in *[!0-9]*|'') timeout=20 ;; esac
        attempt=0
        while ! display_ready; do
            if [ "$attempt" -ge "$timeout" ]; then
                printf '{"state":"unavailable","display":"%s"}\n' "$DISPLAY" >&2
                exit 69
            fi
            sleep 1
            attempt=$((attempt + 1))
        done
        emit_agent_activity
        printf '{"state":"ready_idle","display":"%s"}\n' "$DISPLAY"
        exit 0
        ;;
    *)
        require_display
        emit_agent_activity
        ;;
esac

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
            'ominal-screen status' \
            'ominal-screen wait [SECONDS]' \
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
