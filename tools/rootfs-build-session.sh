#!/usr/bin/env bash
set -euo pipefail

SESSION="${OMINAL_ROOTFS_SESSION:-monolith-rootfs-v3}"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT="${OMINAL_ROOTFS_OUTPUT:-$REPO_ROOT/build/runtime/ominal-ubuntu-24.04.4-arm64-prepared-v3.tgz}"
LOG="${OMINAL_ROOTFS_LOG:-$REPO_ROOT/build-logs/runtime-v3-build.log}"
STATUS="${OMINAL_ROOTFS_STATUS:-$REPO_ROOT/build-logs/runtime-v3-build.status}"

mkdir -p "$(dirname "$OUTPUT")" "$(dirname "$LOG")" "$(dirname "$STATUS")"

case "${1:-start}" in
    start)
        if tmux has-session -t "$SESSION" 2>/dev/null; then
            printf 'Rootfs build session already running: %s\n' "$SESSION"
            exit 0
        fi

        rm -f "$STATUS"
        command_line="$(
            printf 'cd %q; set -o pipefail; bash tools/build-prepared-rootfs.sh %q 2>&1 | tee %q; result=${PIPESTATUS[0]}; printf "%%s\\n" "$result" > %q; exit "$result"' \
                "$REPO_ROOT" "$OUTPUT" "$LOG" "$STATUS"
        )"
        tmux new-session -d -s "$SESSION" bash -lc "$command_line"
        printf 'Started rootfs build session: %s\n' "$SESSION"
        ;;
    status)
        if tmux has-session -t "$SESSION" 2>/dev/null; then
            printf 'running\n'
            tmux capture-pane -p -t "$SESSION":0.0 -S -30
        elif [[ -f "$STATUS" ]]; then
            result="$(cat "$STATUS")"
            if [[ "$result" == 0 ]]; then
                printf 'completed\n'
            else
                printf 'failed (%s)\n' "$result"
            fi
            tail -n 30 "$LOG"
        else
            printf 'not running; no completion status recorded\n'
            [[ -f "$LOG" ]] && tail -n 30 "$LOG"
        fi
        ;;
    attach)
        exec tmux attach-session -t "$SESSION"
        ;;
    *)
        printf 'Usage: %s {start|status|attach}\n' "$0" >&2
        exit 64
        ;;
esac
