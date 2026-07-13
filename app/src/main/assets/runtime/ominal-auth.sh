#!/data/data/com.ominal/files/usr/bin/sh
set -eu

PREFIX="${PREFIX:-/data/data/com.ominal/files/usr}"
HOME="${HOME:-/data/data/com.ominal/files/home}"
PATH="$PREFIX/bin:/system/bin"
export PREFIX HOME PATH

auth_dir="$HOME/.ominal/auth"
log_file="$auth_dir/device-login.log"
result_file="$auth_dir/device-login.result"
pid_file="$auth_dir/device-login.pid"
mkdir -p "$auth_dir"

is_running() {
    [ -s "$pid_file" ] || return 1
    pid="$(cat "$pid_file" 2>/dev/null || true)"
    [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null
}

case "${1:-status}" in
    start)
        if is_running; then
            printf 'running\n'
            exit 0
        fi
        rm -f "$log_file" "$result_file" "$pid_file"
        export OMINAL_AUTH_LOG="$log_file"
        export OMINAL_AUTH_RESULT="$result_file"
        nohup "$PREFIX/bin/sh" -lc '
            result=0
            "$PREFIX/bin/codex" login --device-auth >"$OMINAL_AUTH_LOG" 2>&1 || result=$?
            printf "%s\n" "$result" >"$OMINAL_AUTH_RESULT"
        ' </dev/null >/dev/null 2>&1 &
        printf '%s\n' "$!" >"$pid_file"
        printf 'started\n'
        ;;
    status)
        [ -f "$log_file" ] && cat "$log_file"
        if [ -f "$result_file" ]; then
            printf '\nresult=%s\n' "$(cat "$result_file")"
        elif is_running; then
            printf '\nstate=running\n'
        else
            printf '\nstate=idle\n'
        fi
        ;;
    cancel)
        if is_running; then
            pid="$(cat "$pid_file")"
            kill "$pid" 2>/dev/null || true
        fi
        printf '130\n' >"$result_file"
        rm -f "$pid_file"
        printf 'cancelled\n'
        ;;
    *)
        printf 'Usage: ominal-auth {start|status|cancel}\n' >&2
        exit 64
        ;;
esac
