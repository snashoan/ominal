#!/data/data/com.ominal/files/usr/bin/sh
set -eu

PREFIX="${OMINAL_PREFIX:-/data/data/com.ominal/files/usr}"
HOME="${OMINAL_HOME:-/data/data/com.ominal/files/home}"
RUNNER="${OMINAL_PROOT_RUNNER:-$PREFIX/bin/ominal-proot-run}"
STATE_DIR="$HOME/.ominal/harness-maintenance"
STAMP="$STATE_DIR/last-check"
LOCK="$STATE_DIR/update.lock"
LOG_DIR="$HOME/.ominal/logs"
LOG="$LOG_DIR/harness-update.log"
INTERVAL_SECONDS="${OMINAL_HARNESS_UPDATE_INTERVAL_SECONDS:-21600}"

if [ ! -x "$RUNNER" ]; then
    exit 0
fi

mkdir -p "$STATE_DIR" "$LOG_DIR"
now="$(date +%s 2>/dev/null || printf '0')"
last="$(cat "$STAMP" 2>/dev/null || printf '0')"
case "$now:$last:$INTERVAL_SECONDS" in
    *[!0-9:]*|0:*) ;;
    *)
        if [ $((now - last)) -lt "$INTERVAL_SECONDS" ]; then
            exit 0
        fi
        ;;
esac

if ! mkdir "$LOCK" 2>/dev/null; then
    exit 0
fi
trap 'rmdir "$LOCK" 2>/dev/null || true' EXIT HUP INT TERM

{
    printf '\n[%s] checking installed harnesses\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    OMINAL_PROOT_ID=1000:1000 "$RUNNER" /bin/bash -lc '
        set +e
        export PATH=/root/.ominal/npm/bin:/root/.ominal/node/bin:/root/.local/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

        run_limited() {
            if command -v timeout >/dev/null 2>&1; then
                timeout 240 "$@"
            else
                "$@"
            fi
        }

        report() {
            printf "%s: %s -> %s (%s)\n" "$1" "$2" "$3" "$4"
        }

        if command -v codex >/dev/null 2>&1 && command -v npm >/dev/null 2>&1; then
            before="$(codex --version 2>/dev/null | head -n 1)"
            if run_limited npm install --global --prefix /root/.ominal/npm @openai/codex@latest </dev/null; then
                hash -r
                after="$(codex --version 2>/dev/null | head -n 1)"
                report codex "${before:-unknown}" "${after:-unknown}" updated
            else
                report codex "${before:-unknown}" "${before:-unknown}" deferred
            fi
        fi

        if command -v claude >/dev/null 2>&1; then
            before="$(claude --version 2>/dev/null | head -n 1)"
            if run_limited claude update </dev/null; then
                hash -r
                after="$(claude --version 2>/dev/null | head -n 1)"
                report claude-code "${before:-unknown}" "${after:-unknown}" updated
            else
                report claude-code "${before:-unknown}" "${before:-unknown}" deferred
            fi
        fi

        if command -v agy >/dev/null 2>&1 \
            && agy --help 2>&1 | grep -Eq "(^|[[:space:]])update([[:space:]]|$)"; then
            before="$(agy --version 2>/dev/null | head -n 1)"
            if run_limited agy update </dev/null; then
                hash -r
                after="$(agy --version 2>/dev/null | head -n 1)"
                report antigravity "${before:-unknown}" "${after:-unknown}" updated
            else
                report antigravity "${before:-unknown}" "${before:-unknown}" deferred
            fi
        fi

        exit 0
    '
} >> "$LOG" 2>&1 || true

date +%s > "$STAMP"
tail -n 6 "$LOG" 2>/dev/null || true
