#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${1:-/mnt/c/Users/saura/skynet/termux-app}"
TP_DIR="${2:-/root/ominal/termux-packages}"
ARCHES="${3:-aarch64,arm,i686,x86_64}"
LOG="${4:-/root/ominal/bootstrap-build.log}"
NOHUP_LOG="${5:-/root/ominal/bootstrap-build.nohup.log}"
PID_FILE="${6:-/root/ominal/bootstrap-build.pid}"
STATUS="$APP_DIR/build-logs/ominal-bootstrap-status.txt"

mkdir -p "$(dirname "$PID_FILE")" "$(dirname "$STATUS")"
echo "running" > "$STATUS"

nohup "$APP_DIR/tools/run-ominal-bootstrap-build-wsl.sh" "$APP_DIR" "$TP_DIR" "$ARCHES" "$LOG" > "$NOHUP_LOG" 2>&1 &
pid=$!
printf '%s\n' "$pid" > "$PID_FILE"

echo "PID=$pid"
echo "LOG=$LOG"
echo "NOHUP=$NOHUP_LOG"
echo "STATUS=$STATUS"
