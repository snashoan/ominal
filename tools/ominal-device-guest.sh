#!/bin/sh
set -eu

if [ "${OMINAL_LOLO_MODE:-0}" != "1" ]; then
    printf '%s\n' 'Lolo mode is off. Enable it explicitly in Ominal before using Android controls.' >&2
    exit 77
fi

event_log="${OMINAL_EVENT_LOG:-$PWD/.ominal/events.jsonl}"

write_request() {
    event_type="$1"
    event_value="$2"
    mkdir -p "$(dirname "$event_log")"
    message="$(printf '%s' "$event_value" | sed ':a;N;$!ba;s/\\/\\\\/g;s/"/\\"/g;s/\r//g;s/\n/\\n/g')"
    printf '{"schemaVersion":1,"type":"%s","message":"%s","timestamp":%s}\n' \
        "$event_type" "$message" "$(date +%s)" >> "$event_log"
}

command_name="${1:-}"
[ "$#" -eq 0 ] || shift
case "$command_name" in
    status)
        printf '%s\n' 'Lolo mode enabled; Android bridge available under the Ominal app UID.'
        ;;
    settings)
        write_request android_settings ""
        printf '%s\n' 'Requested Android Settings.'
        ;;
    open)
        [ "$#" -eq 1 ] || { printf '%s\n' 'usage: ominal-device open URI' >&2; exit 64; }
        case "$1" in
            http://*|https://*|mailto:*|geo:*|market:*) ;;
            *) printf '%s\n' 'Unsupported URI scheme.' >&2; exit 64 ;;
        esac
        write_request android_open "$1"
        printf '%s\n' 'Requested Android link.'
        ;;
    app)
        [ "$#" -eq 1 ] || { printf '%s\n' 'usage: ominal-device app PACKAGE' >&2; exit 64; }
        case "$1" in
            *[!A-Za-z0-9._]*) printf '%s\n' 'Invalid Android package name.' >&2; exit 64 ;;
        esac
        write_request android_app "$1"
        printf '%s\n' 'Requested Android app.'
        ;;
    help|--help|-h|'')
        printf '%s\n' \
            'ominal-device status' \
            'ominal-device settings' \
            'ominal-device open URI' \
            'ominal-device app PACKAGE'
        ;;
    *)
        printf '%s\n' 'usage: ominal-device status|settings|open|app' >&2
        exit 64
        ;;
esac
