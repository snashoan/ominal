#!/data/data/com.ominal/files/usr/bin/bash
set -euo pipefail

readonly TARGET_HOME="${HOME:?HOME is not set}"
readonly EXPORT_DIR="${1:?Usage: restore-in-monolith.sh /sdcard/Download/monolith-termux-migration-TIMESTAMP}"
readonly STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
readonly OMINAL_STATE="$TARGET_HOME/.ominal"
readonly STAGE_DIR="$OMINAL_STATE/imports/termux-$STAMP"
readonly BACKUP_DIR="$OMINAL_STATE/termux-home-backups/$STAMP"

case "$TARGET_HOME" in
    /data/data/com.ominal/files/home) ;;
    *)
        printf 'Refusing to restore into unexpected HOME: %s\n' "$TARGET_HOME" >&2
        exit 2
        ;;
esac

for required in SHA256SUMS termux-storage.tgz repositories.txt; do
    if [ ! -f "$EXPORT_DIR/$required" ]; then
        printf 'Missing migration file: %s\n' "$required" >&2
        exit 3
    fi
done

umask 077
mkdir -p -- "$STAGE_DIR" "$BACKUP_DIR"

printf 'Verifying migration checksums...\n'
(
    cd "$EXPORT_DIR"
    sha256sum -c SHA256SUMS
)

printf 'Extracting migration into staging...\n'
tar -C "$STAGE_DIR" -xzf "$EXPORT_DIR/termux-storage.tgz"

printf 'Backing up conflicting Monolith home entries...\n'
find "$STAGE_DIR" -mindepth 1 -maxdepth 1 -print0 \
    | while IFS= read -r -d '' source; do
        name="${source##*/}"
        destination="$TARGET_HOME/$name"
        if [ -e "$destination" ] || [ -L "$destination" ]; then
            cp -a -- "$destination" "$BACKUP_DIR/$name"
        fi
    done

printf 'Restoring Termux home data...\n'
tar -C "$STAGE_DIR" -cf - . | tar -C "$TARGET_HOME" -xf -

if [ -f "$TARGET_HOME/.codex/auth.json" ]; then
    mkdir -p -- "$OMINAL_STATE/codex"
    cp -- "$TARGET_HOME/.codex/auth.json" "$OMINAL_STATE/codex/auth.json"
    chmod 600 "$OMINAL_STATE/codex/auth.json"
fi

cp -- "$EXPORT_DIR/repositories.txt" "$OMINAL_STATE/termux-repositories.txt"
chmod 600 "$OMINAL_STATE/termux-repositories.txt"
rm -rf -- "$STAGE_DIR"

printf '\nRestore complete.\n'
printf 'Backup of replaced Monolith files: %s\n' "$BACKUP_DIR"
