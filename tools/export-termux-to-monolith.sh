#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

readonly SOURCE_HOME="${HOME:?HOME is not set}"
readonly SOURCE_PREFIX="${PREFIX:-/data/data/com.termux/files/usr}"
readonly STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
readonly OUTPUT_DIR="${1:-/sdcard/Download/monolith-termux-migration-${STAMP}}"
readonly SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
readonly STAGE_DIR="${SOURCE_PREFIX}/tmp/monolith-termux-export-$$"

cleanup() {
    rm -rf -- "$STAGE_DIR"
}
trap cleanup EXIT INT TERM

case "$SOURCE_HOME" in
    /data/data/com.termux/files/home) ;;
    *)
        printf 'Refusing to export unexpected HOME: %s\n' "$SOURCE_HOME" >&2
        exit 2
        ;;
esac

umask 077
mkdir -p -- "$OUTPUT_DIR" "$STAGE_DIR/settings"

printf 'Creating Termux migration inventory...\n'
{
    printf 'format=monolith-termux-storage-v1\n'
    printf 'created_utc=%s\n' "$STAMP"
    printf 'source_home=%s\n' "$SOURCE_HOME"
    printf 'source_prefix=%s\n' "$SOURCE_PREFIX"
    printf 'source_user=%s\n' "$(id -un)"
    printf 'source_uid=%s\n' "$(id -u)"
    printf 'device=%s\n' "$(getprop ro.product.device 2>/dev/null || true)"
    printf 'android_release=%s\n' "$(getprop ro.build.version.release 2>/dev/null || true)"
} > "$OUTPUT_DIR/manifest.txt"

find "$SOURCE_HOME" -xdev -type d -name .git -prune -print 2>/dev/null \
    | while IFS= read -r repository; do
        printf '%s\n' "${repository%/.git}"
    done \
    | LC_ALL=C sort > "$OUTPUT_DIR/repositories.txt"

copy_setting() {
    local relative_path="$1"
    local source="$SOURCE_HOME/$relative_path"
    local destination="$STAGE_DIR/settings/$relative_path"

    [ -e "$source" ] || return 0
    mkdir -p -- "$(dirname -- "$destination")"
    cp -aL -- "$source" "$destination"
}

for setting in \
    .termux .bashrc .bash_profile .profile .zshrc .zprofile \
    .gitconfig .ssh; do
    copy_setting "$setting"
done

printf 'Creating settings archive...\n'
tar -C "$STAGE_DIR/settings" -czf "$OUTPUT_DIR/termux-settings.tgz" .

printf 'Creating persistent storage archive...\n'
tar \
    --exclude='./storage' \
    --exclude='./.cache' \
    --exclude='./.tmp' \
    --exclude='./.cargo' \
    --exclude='./.gradle' \
    --exclude='./.jdks' \
    --exclude='./.npm' \
    --exclude='./.ollama' \
    --exclude='./.rustup' \
    --exclude='*/.venv' \
    --exclude='*/venv' \
    --exclude='*/node_modules' \
    --exclude='*/__pycache__' \
    --exclude='*/target' \
    --exclude='*/build' \
    --exclude='*/dist' \
    --exclude='*.pyc' \
    --exclude='*.sock' \
    -C "$SOURCE_HOME" \
    -czf "$OUTPUT_DIR/termux-storage.tgz" .

if [ -f "$SCRIPT_DIR/restore-termux-in-monolith.sh" ]; then
    cp -- "$SCRIPT_DIR/restore-termux-in-monolith.sh" "$OUTPUT_DIR/restore-in-monolith.sh"
    chmod 700 "$OUTPUT_DIR/restore-in-monolith.sh"
fi

(
    cd "$OUTPUT_DIR"
    sha256sum manifest.txt repositories.txt \
        termux-settings.tgz termux-storage.tgz > SHA256SUMS
)

printf '\nMigration export complete:\n%s\n' "$OUTPUT_DIR"
du -h "$OUTPUT_DIR"/* | LC_ALL=C sort -h
printf '\nThis export contains private credentials. Delete it after Monolith is verified.\n'
