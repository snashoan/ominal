#!/data/data/com.ominal/files/usr/bin/sh
set -eu

# Keep the development mirror byte-for-byte aligned with the packaged runtime asset.
SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
SOURCE="$SCRIPT_DIR/../app/src/main/assets/runtime/ominal-harness-chat.sh"

if [ ! -f "$SOURCE" ]; then
    printf '%s\n' 'Packaged harness chat bridge was not found.' >&2
    exit 69
fi

exec /bin/sh "$SOURCE" "$@"
