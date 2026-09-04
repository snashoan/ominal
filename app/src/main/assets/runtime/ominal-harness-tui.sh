#!/data/data/com.ominal/files/usr/bin/sh
set -eu

PREFIX="${OMINAL_PREFIX:-/data/data/com.ominal/files/usr}"
HOME="${OMINAL_HOME:-/data/data/com.ominal/files/home}"
RUNNER="${OMINAL_PROOT_RUNNER:-$PREFIX/bin/ominal-proot-run}"
HARNESS="${1:-}"
WORKSPACE="${2:-$HOME/workspace}"
if [ "$#" -ge 2 ]; then
    shift 2
else
    set --
fi

case "$HARNESS" in
    ''|-*|*-|*[!a-z0-9-]*)
        printf 'Unsupported intelligence harness: %s\n' "$HARNESS" >&2
        exit 64
        ;;
    *) ;;
esac

case "$WORKSPACE" in
    /data/user/0/com.ominal/*)
        WORKSPACE="/data/data/com.ominal${WORKSPACE#/data/user/0/com.ominal}"
        ;;
    /root/workspace|/root/workspace/*|'')
        WORKSPACE="$HOME/workspace"
        ;;
esac

case "$WORKSPACE" in
    "$HOME"|"$HOME"/*) ;;
    *)
        printf 'Refusing harness workspace outside GIR home: %s\n' "$WORKSPACE" >&2
        exit 64
        ;;
esac

case "$WORKSPACE" in
    "$HOME/.ominal/chats/"*/workspace)
        chat_parent="${WORKSPACE%/workspace}"
        export OMINAL_AGENT_SESSION="${chat_parent##*/}"
        ;;
esac

if [ ! -x "$RUNNER" ]; then
    printf 'GIR Linux launcher is missing: %s\n' "$RUNNER" >&2
    exit 69
fi

mkdir -p "$WORKSPACE"
export OMINAL_WORKDIR="$WORKSPACE"

case "$HARNESS" in
    codex)
        exec "$RUNNER" /bin/bash --login -c '
            export PATH="/root/.local/bin:$PATH"
            if ! command -v codex >/dev/null 2>&1; then
                printf "\nCodex is not installed in this Linux workspace.\n"
                exec /bin/bash --login
            fi
            if ! codex login status >/dev/null 2>&1; then
                printf "\nCodex will handle sign-in in this terminal.\n\n"
                codex login --device-auth || exec /bin/bash --login
            fi
            if [ "${1:-}" = "--resume" ] && [ -n "${2:-}" ]; then
                exec codex resume "$2"
            fi
            exec codex
        ' ominal "$@"
        ;;
    claude-code)
        exec "$RUNNER" /bin/bash --login -c '
            export PATH="/root/.local/bin:$PATH"
            if ! command -v claude >/dev/null 2>&1; then
                printf "\nInstalling Claude Code from claude.ai...\n\n"
                if ! command -v curl >/dev/null 2>&1 \
                    && (! apt-get update \
                        || ! apt-get install -y --no-install-recommends ca-certificates curl); then
                    printf "\nThe network prerequisite could not be installed.\n"
                    exec /bin/bash --login
                fi
                installer="$(mktemp)"
                if ! curl -fsSL https://claude.ai/install.sh -o "$installer" \
                    || ! /bin/bash "$installer"; then
                    rm -f "$installer"
                    printf "\nClaude Code installation did not complete.\n"
                    exec /bin/bash --login
                fi
                rm -f "$installer"
                hash -r
            fi
            if ! command -v claude >/dev/null 2>&1; then
                printf "\nClaude Code was installed outside the current PATH.\n"
                exec /bin/bash --login
            fi
            exec claude
        '
        ;;
    antigravity)
        exec "$RUNNER" /bin/bash --login -c '
            export PATH="/root/.local/bin:$PATH"
            if command -v git >/dev/null 2>&1 && [ ! -d .git ]; then
                git init --quiet
            fi
            if ! command -v agy >/dev/null 2>&1; then
                printf "\nInstalling Antigravity CLI from antigravity.google...\n\n"
                if ! command -v curl >/dev/null 2>&1 \
                    && (! apt-get update \
                        || ! apt-get install -y --no-install-recommends ca-certificates curl); then
                    printf "\nThe network prerequisite could not be installed.\n"
                    exec /bin/bash --login
                fi
                installer="$(mktemp)"
                if ! curl -fsSL https://antigravity.google/cli/install.sh -o "$installer" \
                    || ! /bin/bash "$installer"; then
                    rm -f "$installer"
                    printf "\nAntigravity CLI installation did not complete.\n"
                    exec /bin/bash --login
                fi
                rm -f "$installer"
                hash -r
            fi
            if ! command -v agy >/dev/null 2>&1; then
                printf "\nAntigravity CLI was installed outside the current PATH.\n"
                exec /bin/bash --login
            fi
            mkdir -p .ominal
            export OMINAL_HARNESS_EVENT_FILE="$PWD/.ominal/antigravity-events.jsonl"
            if command -v ominal-harness-hook >/dev/null 2>&1; then
                ominal-harness-hook --install
            fi
            exec agy --dangerously-skip-permissions "$@"
        ' ominal "$@"
        ;;
    *)
        RESUME_ID=""
        MODEL_ID=""
        EFFORT_ID=""
        while [ "$#" -gt 0 ]; do
            if [ "$#" -lt 2 ]; then
                printf 'Missing value for harness terminal option: %s\n' "$1" >&2
                exit 64
            fi
            case "$1" in
                --resume) RESUME_ID="$2" ;;
                --model) MODEL_ID="$2" ;;
                --effort) EFFORT_ID="$2" ;;
                *)
                    printf 'Unsupported harness terminal option: %s\n' "$1" >&2
                    exit 64
                    ;;
            esac
            shift 2
        done
        exec "$RUNNER" /bin/bash --login -c '
            set -eu
            harness="$1"
            resume_id="$2"
            model_id="$3"
            effort_id="$4"
            manifest="/root/.ominal/harness-registry/${harness}/manifest.json"
            [ -s "$manifest" ] || manifest="/root/.ominal/harness-capabilities/${harness}.json"
            if [ ! -s "$manifest" ]; then
                printf "No registered terminal contract for %s.\n" "$harness" >&2
                exec /bin/bash --login
            fi
            mapfile -t spec < <(python3 -c '\''
import json, re, sys
data = json.load(open(sys.argv[1], encoding="utf-8"))
transport = data.get("transport") or {}
autonomy = data.get("autonomy") or {}
executable = re.compile(r"[A-Za-z0-9][A-Za-z0-9._-]{0,127}\Z")
flag = re.compile(r"--[A-Za-z0-9][A-Za-z0-9-]{0,63}\Z")
if data.get("harness") != sys.argv[2]:
    raise SystemExit(65)
terminal = str(transport.get("terminalCommand", ""))
values = [
    terminal,
    str(transport.get("resumeFlag", "")),
    str(transport.get("modelFlag", "")),
    str(transport.get("effortFlag", "")),
    str(autonomy.get("flag", "")),
    "1" if autonomy.get("enabledByDefault") else "0",
]
if not executable.fullmatch(terminal):
    raise SystemExit(65)
if any(value and not flag.fullmatch(value) for value in values[1:5]):
    raise SystemExit(65)
print("\n".join(values))
'\'' "$manifest" "$harness") || exit 65
            terminal="${spec[0]:-}"
            resume_flag="${spec[1]:-}"
            model_flag="${spec[2]:-}"
            effort_flag="${spec[3]:-}"
            autonomy_flag="${spec[4]:-}"
            autonomy_default="${spec[5]:-0}"
            if ! command -v "$terminal" >/dev/null 2>&1; then
                printf "Harness terminal not found: %s\n" "$terminal" >&2
                exec /bin/bash --login
            fi
            cd /root/workspace
            set -- "$terminal"
            if [ -n "$resume_id" ] && [ -n "$resume_flag" ]; then
                set -- "$@" "$resume_flag" "$resume_id"
            fi
            if [ -n "$model_id" ] && [ -n "$model_flag" ]; then
                set -- "$@" "$model_flag" "$model_id"
            fi
            if [ -n "$effort_id" ] && [ -n "$effort_flag" ]; then
                set -- "$@" "$effort_flag" "$effort_id"
            fi
            if [ "$autonomy_default" = 1 ] && [ -n "$autonomy_flag" ]; then
                set -- "$@" "$autonomy_flag"
            fi
            exec "$@"
        ' ominal "$HARNESS" "$RESUME_ID" "$MODEL_ID" "$EFFORT_ID"
        ;;
esac
