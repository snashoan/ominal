#!/bin/sh
set -eu

if [ "$#" -ne 1 ]; then
    printf '%s\n' 'usage: xdg-open URL' >&2
    exit 64
fi

url="$1"
case "$url" in
    http://*|https://*) ;;
    *)
        printf '%s\n' 'Only HTTP and HTTPS links can be opened.' >&2
        exit 64
        ;;
esac

if ! command -v firefox >/dev/null 2>&1; then
    printf '%s\n' 'Firefox is not installed in the Monolith display.' >&2
    exit 69
fi

export DISPLAY="${OMINAL_DISPLAY:-${DISPLAY:-:20}}"
export XDG_RUNTIME_DIR="${XDG_RUNTIME_DIR:-/tmp/ominal-runtime-root}"
mkdir -p "$XDG_RUNTIME_DIR"
chmod 700 "$XDG_RUNTIME_DIR"

if command -v ominal-event >/dev/null 2>&1; then
    ominal-event request-user-input "Complete sign-in in Firefox" >/dev/null 2>&1 || true
fi

nohup firefox --new-tab "$url" </dev/null >/dev/null 2>&1 &
