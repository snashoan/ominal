#!/bin/sh
set -eu

event_type="${1:-}"
[ "$#" -eq 0 ] || shift
case "$event_type" in
    open-display) event_type=open_display ;;
    request-user-input) event_type=request_user_input ;;
    open_display|request_user_input|status) ;;
    *)
        printf '%s\n' 'usage: ominal-event open-display|request-user-input|status [MESSAGE]' >&2
        exit 64
        ;;
esac

event_log="${OMINAL_EVENT_LOG:-$PWD/.ominal/events.jsonl}"
mkdir -p "$(dirname "$event_log")"
message="$(printf '%s' "$*" | sed ':a;N;$!ba;s/\\/\\\\/g;s/"/\\"/g;s/\r//g;s/\n/\\n/g')"
printf '{"schemaVersion":1,"type":"%s","message":"%s","timestamp":%s}\n' \
    "$event_type" "$message" "$(date +%s)" >> "$event_log"
