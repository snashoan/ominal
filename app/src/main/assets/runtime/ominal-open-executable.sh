#!/bin/bash
set -eu

show_error() {
    message="$1"
    if [ -n "${DISPLAY:-}" ] && command -v yad >/dev/null 2>&1; then
        yad --error --title="Unable to run" --window-icon=dialog-error \
            --image=dialog-error --text="$message" --button="Close":0 \
            --width=420 --fixed --center --borders=20 >/dev/null 2>&1 || true
    elif [ -n "${DISPLAY:-}" ] && command -v xmessage >/dev/null 2>&1; then
        xmessage -center -title "Unable to run" -buttons "Close:0" \
            -default "Close" "$message" >/dev/null 2>&1 || true
    else
        printf '%s\n' "$message" >&2
    fi
}

requested_path="${1:-}"
if [ -z "$requested_path" ]; then
    show_error "No executable was selected."
    exit 64
fi

case "$requested_path" in
    file://*)
        show_error "This file location cannot be opened safely. Open it from Files instead."
        exit 64
        ;;
esac
path="$(realpath -e -- "$requested_path" 2>/dev/null || true)"
if [ -z "$path" ] || [ ! -f "$path" ]; then
    show_error "The selected file no longer exists."
    exit 66
fi

mime_type="$(file --brief --mime-type -- "$path" 2>/dev/null || true)"
case "$mime_type" in
    application/x-executable|application/x-pie-executable|application/x-shellscript|text/x-shellscript|application/vnd.appimage)
        ;;
    *)
        signature="$(LC_ALL=C head -c 2 -- "$path" 2>/dev/null || true)"
        if [ "$signature" != '#!' ]; then
            show_error "This file is not a supported Linux executable."
            exit 65
        fi
        ;;
esac

display_name="$(basename -- "$path")"
case "$display_name" in
    ????????????????????????????????????????????????????????????????????????????????*)
        display_name="$(printf '%.77s...' "$display_name")"
        ;;
esac
message="Run \"$display_name\"?

GIR will grant executable permission only to this file. Continue only if you trust its source."

approved=0
if [ -n "${DISPLAY:-}" ] && command -v yad >/dev/null 2>&1; then
    if yad --question --title="Allow executable?" --window-icon=dialog-warning \
        --image=dialog-warning --text="$message" --button="Cancel":1 \
        --button="Allow and run":0 --width=420 --fixed --center --on-top \
        --borders=20 >/dev/null 2>&1; then
        approved=1
    fi
elif [ -n "${DISPLAY:-}" ] && command -v xmessage >/dev/null 2>&1; then
    if xmessage -center -title "Allow executable?" \
        -buttons "Cancel:1,Allow and run:0" -default "Cancel" \
        "$message" >/dev/null 2>&1; then
        approved=1
    fi
else
    show_error "Executable approval requires the GIR display."
    exit 77
fi

[ "$approved" -eq 1 ] || exit 0

if ! chmod u+x -- "$path" || [ ! -x "$path" ]; then
    show_error "GIR could not grant executable permission to this file. Move it into the Linux workspace and try again."
    exit 73
fi

working_directory="$(dirname -- "$path")"
state_directory=/root/.local/state/ominal/executables
mkdir -p "$state_directory"
log_file="$state_directory/last-run.log"

case "$mime_type" in
    application/x-shellscript|text/x-shellscript)
        if command -v xfce4-terminal >/dev/null 2>&1; then
            nohup xfce4-terminal --disable-server --title="$display_name" \
                --working-directory="$working_directory" --execute "$path" \
                </dev/null >"$log_file" 2>&1 &
        elif command -v xterm >/dev/null 2>&1; then
            nohup xterm -T "$display_name" -e "$path" \
                </dev/null >"$log_file" 2>&1 &
        else
            (cd "$working_directory" && nohup "$path" </dev/null >"$log_file" 2>&1 &)
        fi
        ;;
    *)
        (cd "$working_directory" && nohup "$path" </dev/null >"$log_file" 2>&1 &)
        ;;
esac
