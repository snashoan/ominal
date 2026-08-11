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

bridge_dir="${OMINAL_BRIDGE_DIR:-/run/ominal}"
if [ ! -d "$bridge_dir" ] || [ ! -w "$bridge_dir" ]; then
    printf '%s\n' 'The Monolith browser bridge is unavailable.' >&2
    exit 69
fi

request="$bridge_dir/url-$(date +%s)-$$.request"
partial="$request.part"
umask 077
printf '%s\n' "$url" > "$partial"
mv "$partial" "$request"
printf '%s\n' 'Choose where to open the sign-in link in Monolith.'
