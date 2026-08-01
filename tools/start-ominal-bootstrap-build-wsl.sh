#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${1:-/mnt/c/Users/saura/skynet/termux-app}"
TP_DIR="${2:-/root/ominal/termux-packages-ominal}"
ARCHES="${3:-aarch64}"
LOG="${4:-/root/ominal/bootstrap-build.log}"
NOHUP_LOG="${5:-/root/ominal/bootstrap-build.nohup.log}"
PID_FILE="${6:-/root/ominal/bootstrap-build.pid}"
STATUS="$APP_DIR/build-logs/ominal-bootstrap-status.txt"

mkdir -p "$(dirname "$PID_FILE")" "$(dirname "$STATUS")"
if [[ -s "$PID_FILE" ]]; then
    existing_pid="$(tr -d '[:space:]' < "$PID_FILE")"
    if [[ "$existing_pid" =~ ^[0-9]+$ ]] && kill -0 "$existing_pid" 2>/dev/null; then
        echo "ALREADY_RUNNING PID=$existing_pid"
        echo "LOG=$LOG"
        echo "STATUS=$STATUS"
        exit 0
    fi
    if [[ -f "$STATUS" ]] && grep -Fqx running "$STATUS"; then
        printf '%s\n' failed:orphaned > "$STATUS"
    fi
fi

printf '%s\n' starting > "$STATUS"

# Keep Docker and its build children out of the invoking terminal's session.
# WSL can tear down a direct child when the Windows console closes, so the
# runner must own a new session and ignore a parent hangup.
nohup setsid "$APP_DIR/tools/run-ominal-bootstrap-build-wsl.sh" "$APP_DIR" "$TP_DIR" "$ARCHES" "$LOG" \
    </dev/null > "$NOHUP_LOG" 2>&1 &
pid=$!
printf '%s\n' "$pid" > "$PID_FILE"
printf '%s\n' running > "$STATUS"

echo "PID=$pid"
echo "LOG=$LOG"
echo "NOHUP=$NOHUP_LOG"
echo "STATUS=$STATUS"
