#!/data/data/com.ominal/files/usr/bin/sh
set -eu

PREFIX="${OMINAL_PREFIX:-/data/data/com.ominal/files/usr}"
RUNNER="${OMINAL_PROOT_RUNNER:-$PREFIX/bin/ominal-proot-run}"
HARNESS="${1:-}"
ACTION="${2:-}"

case "$HARNESS" in
    ''|-*|*-|*[!a-z0-9-]*)
        printf 'Invalid chat harness: %s\n' "$HARNESS" >&2
        exit 64
        ;;
    *) ;;
esac

if [ ! -x "$RUNNER" ]; then
    printf '%s\n' 'GIR Linux launcher is not ready.' >&2
    exit 69
fi

case "$ACTION" in
    discover)
        OMINAL_PROOT_ID=1000:1000 exec "$RUNNER" /bin/bash -lc '
            set -eu
            harness="$1"
            manifest="/root/.ominal/harness-registry/${harness}/manifest.json"
            [ -s "$manifest" ] || manifest="/root/.ominal/harness-capabilities/${harness}.json"
            if [ "$harness" != "claude-code" ] && [ "$harness" != "antigravity" ]; then
                python3 -c '\''
import json, re, sys
data = json.load(open(sys.argv[1], encoding="utf-8"))
transport = data.get("transport") or {}
command = transport.get("adapterCommand", "")
if data.get("harness") != sys.argv[2] or transport.get("outputFormat") != "monopot-jsonl":
    raise SystemExit(65)
if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]{0,127}", command):
    raise SystemExit(65)
'\'' "$manifest" "$harness"
                exit 0
            fi
            ominal-harness-discover "$harness" "$manifest"
        ' ominal "$HARNESS"
        ;;
    run)
        GUEST_CWD="${3:-}"
        THREAD_ID="${4:-}"
        PROMPT_FILE="${5:-}"
        INSTRUCTIONS_FILE="${6:-}"
        MODEL_ID="${7:-}"
        EFFORT_ID="${8:-}"
        if [ -z "$GUEST_CWD" ] || [ -z "$PROMPT_FILE" ] || [ -z "$INSTRUCTIONS_FILE" ]; then
            printf '%s\n' 'Incomplete harness chat request.' >&2
            exit 64
        fi
        OMINAL_PROOT_ID=1000:1000 exec "$RUNNER" /bin/bash -lc '
            set -eu
            harness="$1"
            cwd="$2"
            thread_id="$3"
            prompt_file="$4"
            instructions_file="$5"
            model_id="$6"
            effort_id="$7"
            cd "$cwd"
            if command -v git >/dev/null 2>&1 && [ ! -d .git ]; then
                git init --quiet
            fi
            prompt="$(cat "$prompt_file")"
            manifest="/root/.ominal/harness-registry/${harness}/manifest.json"
            [ -s "$manifest" ] || manifest="/root/.ominal/harness-capabilities/${harness}.json"

            if command -v ominal-harness-discover >/dev/null 2>&1; then
                if [ ! -s "$manifest" ]; then
                    printf "%s\n" \
                        "{\"type\":\"ominal_setup\",\"status\":\"Setting up ${harness}\"}"
                fi
                ominal-harness-discover "$harness" "$manifest" >/dev/null 2>&1 || true
            fi

            case "$harness" in
                claude-code)
                    if ! command -v claude >/dev/null 2>&1; then
                        printf "%s\n" "Claude Code is not installed. Run /login first." >&2
                        exit 69
                    fi
                    set -- claude -p --output-format stream-json --verbose \
                        --dangerously-skip-permissions
                    if [ -n "$thread_id" ]; then
                        set -- "$@" --resume "$thread_id"
                    fi
                    if [ -n "$model_id" ]; then
                        set -- "$@" --model "$model_id"
                    fi
                    if [ -n "$effort_id" ]; then
                        set -- "$@" --effort "$effort_id"
                    fi
                    if [ -s "$instructions_file" ] && [ "${prompt#/}" = "$prompt" ]; then
                        set -- "$@" --append-system-prompt "$(cat "$instructions_file")"
                    fi
                    exec "$@" "$prompt" < /dev/null
                    ;;
                antigravity)
                    if ! command -v agy >/dev/null 2>&1; then
                        printf "%s\n" "Antigravity is not installed. Run /login first." >&2
                        exit 69
                    fi
                    if [ "${prompt#/}" != "$prompt" ] && [ "${prompt#* }" = "$prompt" ] \
                        && [ -s "$manifest" ]; then
                        native_command="$(ominal-harness-discover \
                            --resolve-command "$manifest" "$prompt")"
                        if [ -n "$native_command" ]; then
                            exec agy "$native_command" < /dev/null
                        fi
                    fi
                    set -- agy --dangerously-skip-permissions
                    if agy --help 2>&1 | grep -q -- "--output-format"; then
                        set -- "$@" --output-format stream-json
                    fi
                    if [ -n "$thread_id" ]; then
                        set -- "$@" --conversation "$thread_id"
                    fi
                    if [ -n "$model_id" ]; then
                        set -- "$@" --model "$model_id"
                    fi
                    if [ -n "$effort_id" ]; then
                        set -- "$@" --effort "$effort_id"
                    fi
                    set -- "$@" -p
                    if [ "${prompt#/}" != "$prompt" ]; then
                        exec "$@" "$prompt" < /dev/null
                    fi
                    payload="$(cat "$instructions_file")

${prompt}"
                    exec "$@" "$payload" < /dev/null
                    ;;
                *)
                    if [ ! -s "$manifest" ]; then
                        printf "%s\n" "No runtime adapter is registered for ${harness}." >&2
                        exit 69
                    fi
                    adapter="$(python3 -c '\''
import json, re, sys
data = json.load(open(sys.argv[1], encoding="utf-8"))
transport = data.get("transport") or {}
command = transport.get("adapterCommand", "")
if data.get("harness") != sys.argv[2] or transport.get("outputFormat") != "monopot-jsonl":
    raise SystemExit(65)
if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]{0,127}", command):
    raise SystemExit(65)
print(command)
'\'' "$manifest" "$harness")"
                    if ! command -v "$adapter" >/dev/null 2>&1; then
                        printf "Runtime adapter not found: %s\n" "$adapter" >&2
                        exit 69
                    fi
                    exec "$adapter" turn \
                        --protocol monopot/1 \
                        --harness "$harness" \
                        --workspace "$cwd" \
                        --thread "$thread_id" \
                        --prompt-file "$prompt_file" \
                        --instructions-file "$instructions_file" \
                        --model "$model_id" \
                        --effort "$effort_id" < /dev/null
                    ;;
            esac
        ' ominal "$HARNESS" "$GUEST_CWD" "$THREAD_ID" \
            "$PROMPT_FILE" "$INSTRUCTIONS_FILE" "$MODEL_ID" "$EFFORT_ID"
        ;;
    *)
        printf 'Unsupported harness chat action: %s\n' "$ACTION" >&2
        exit 64
        ;;
esac
