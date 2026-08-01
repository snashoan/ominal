#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

destination="${1:-$HOME/storage/downloads/ominal-termux-configs.tgz}"
if [[ ! -d "$HOME/storage/downloads" ]]; then
    printf '%s\n' 'Run termux-setup-storage once, then run this script again.' >&2
    exit 64
fi

entries=()
for path in .termux .bashrc .bash_profile .profile .zshrc .zprofile .gitconfig .ssh .config/git; do
    [[ -e "$HOME/$path" ]] && entries+=("$path")
done
if [[ ${#entries[@]} -eq 0 ]]; then
    printf '%s\n' 'No supported Termux configuration files were found.' >&2
    exit 66
fi

mkdir -p "$(dirname "$destination")"
temporary="$destination.tmp.$$"
rm -f "$temporary"
tar -C "$HOME" -czf "$temporary" -- "${entries[@]}"
mv "$temporary" "$destination"
printf 'Exported %d configuration entries to %s\n' "${#entries[@]}" "$destination"
