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
    codex|claude-code|antigravity) ;;
    *)
        printf 'Unsupported intelligence harness: %s\n' "$HARNESS" >&2
        exit 64
        ;;
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
        printf 'Refusing harness workspace outside Monolith home: %s\n' "$WORKSPACE" >&2
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
    printf 'Monolith Linux launcher is missing: %s\n' "$RUNNER" >&2
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
            exec codex
        '
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
esac
