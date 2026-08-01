#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${1:-/mnt/c/Users/saura/skynet/termux-app}"
TP_DIR="${2:-/root/ominal/termux-packages-ominal}"
ARCHES="${3:-aarch64}"
LOG="${4:-/root/ominal/bootstrap-build.log}"
STATUS="$APP_DIR/build-logs/ominal-bootstrap-status.txt"
SOURCE_LOCK="$APP_DIR/tools/ominal-bootstrap-source.lock"
BUILD_JOBS="${OMINAL_BOOTSTRAP_BUILD_JOBS:-4}"
FORCE_REBUILD="${OMINAL_BOOTSTRAP_FORCE_REBUILD:-1}"

mkdir -p "$(dirname "$STATUS")" "$(dirname "$LOG")"
printf '%s\n' running > "$STATUS"
trap 'rc=$?; if [ "$rc" -ne 0 ]; then printf "failed:%s\n" "$rc" > "$STATUS"; fi' EXIT

if ! [[ "$BUILD_JOBS" =~ ^[1-9][0-9]*$ ]]; then
    printf 'OMINAL_BOOTSTRAP_BUILD_JOBS must be a positive integer; found %s.\n' "$BUILD_JOBS" >&2
    exit 2
fi
if [[ "$FORCE_REBUILD" != "0" && "$FORCE_REBUILD" != "1" ]]; then
    printf 'OMINAL_BOOTSTRAP_FORCE_REBUILD must be 0 or 1; found %s.\n' "$FORCE_REBUILD" >&2
    exit 2
fi

require_build_disk_space() {
    local min_mb="${OMINAL_MIN_C_DRIVE_FREE_MB:-8192}"
    local free_mb
    free_mb="$(df -Pm "$APP_DIR" | awk 'NR == 2 { print $4 }')"
    if [[ -z "$free_mb" || "$free_mb" -lt "$min_mb" ]]; then
        printf 'Need at least %sMB free on the app checkout drive; found %sMB.\n' \
            "$min_mb" "${free_mb:-unknown}" >&2
        return 1
    fi
}

source_git() {
    git -c safe.directory="$TP_DIR" -C "$TP_DIR" "$@"
}

require_ominal_source() {
    test -f "$SOURCE_LOCK"
    # shellcheck source=/dev/null
    source "$SOURCE_LOCK"
    : "${OMINAL_PACKAGES_UPSTREAM_REVISION:?missing source revision lock}"
    : "${OMINAL_PACKAGES_TREE:?missing source tree lock}"

    source_git rev-parse --is-inside-work-tree >/dev/null
    test "$(source_git rev-parse 'HEAD^')" = "$OMINAL_PACKAGES_UPSTREAM_REVISION"
    test "$(source_git rev-parse 'HEAD^{tree}')" = "$OMINAL_PACKAGES_TREE"
    source_git diff --quiet
    source_git diff --cached --quiet
    test -d "$TP_DIR/root-packages"
    test -w "$TP_DIR"

    grep -Fqx 'TERMUX__NAME="Ominal"' "$TP_DIR/scripts/properties.sh"
    grep -Fqx 'TERMUX__INTERNAL_NAME="ominal"' "$TP_DIR/scripts/properties.sh"
    grep -Fqx 'TERMUX_APP__PACKAGE_NAME="com.ominal"' "$TP_DIR/scripts/properties.sh"
    grep -Fqx 'TERMUX_APP__DATA_DIR="/data/data/$TERMUX_APP__PACKAGE_NAME"' \
        "$TP_DIR/scripts/properties.sh"
}

{
    printf 'START %s\n' "$(date -Is)"
    printf '%s\n' 'Goal: build current Android 10+ Ominal bootstrap from a clean source fork'
    require_build_disk_space
    require_ominal_source

    cd "$TP_DIR"
    printf 'SOURCE_REVISION=%s\n' "$(source_git rev-parse HEAD)"
    printf 'SOURCE_BRANCH=%s\n' "$(source_git branch --show-current)"
    printf 'ARCHITECTURES=%s\n' "$ARCHES"
    printf 'BUILD_JOBS=%s\n' "$BUILD_JOBS"
    printf 'FORCE_REBUILD=%s\n' "$FORCE_REBUILD"

    docker version --format 'DOCKER={{.Server.Version}}' >/dev/null
    available_cpus="$(nproc)"
    cpu_limit="$BUILD_JOBS"
    if (( cpu_limit > available_cpus )); then
        cpu_limit="$available_cpus"
    fi
    if (( cpu_limit == 1 )); then
        builder_cpuset="0"
    else
        builder_cpuset="0-$((cpu_limit - 1))"
    fi
    printf 'BUILDER_CPUSET=%s\n' "$builder_cpuset"

    # A Docker container preserves its original source bind mount. Never reuse
    # the generic upstream builder, which may still point at another checkout.
    builder_container="${OMINAL_BUILDER_CONTAINER:-ominal-package-builder}"
    export CONTAINER_NAME="$builder_container"
    printf 'BUILDER_CONTAINER=%s\n' "$builder_container"
    builder_container_existed=0
    if docker container inspect "$builder_container" >/dev/null 2>&1; then
        expected_source="$(realpath "$TP_DIR")"
        mounted_source="$(docker inspect --format \
            '{{range .Mounts}}{{if eq .Destination "/home/builder/termux-packages"}}{{.Source}}{{end}}{{end}}' \
            "$builder_container")"
        if [[ "$mounted_source" != "$expected_source" ]]; then
            printf 'Replacing %s: source mount changed from %s to %s\n' \
                "$builder_container" "${mounted_source:-missing}" "$expected_source"
            docker rm -f "$builder_container" >/dev/null
            export TERMUX_DOCKER_RUN_EXTRA_ARGS="${TERMUX_DOCKER_RUN_EXTRA_ARGS:-} --cpuset-cpus=$builder_cpuset"
        else
            builder_container_existed=1
            docker update --cpuset-cpus "$builder_cpuset" "$builder_container" >/dev/null
        fi
    else
        export TERMUX_DOCKER_RUN_EXTRA_ARGS="${TERMUX_DOCKER_RUN_EXTRA_ARGS:-} --cpuset-cpus=$builder_cpuset"
    fi

    bootstrap_args=(--android10 --architectures "$ARCHES")
    if [[ "$FORCE_REBUILD" == "1" && "$builder_container_existed" == "1" ]]; then
        printf '%s\n' 'Cleaning the dedicated Ominal builder before the forced source build'
        ./scripts/run-docker.sh ./clean.sh
    fi
    TERMUX_DOCKER_EXEC_EXTRA_ARGS="${TERMUX_DOCKER_EXEC_EXTRA_ARGS:-} --env TERMUX_PKG_MAKE_PROCESSES=$BUILD_JOBS" \
        ./scripts/run-docker.sh ./scripts/build-bootstraps.sh "${bootstrap_args[@]}"

    IFS=',' read -r -a ominal_arches <<< "$ARCHES"
    for arch in "${ominal_arches[@]}"; do
        archive="bootstrap-${arch}.zip"
        test -s "$archive"
        cp -f "$archive" "$APP_DIR/app/src/main/cpp/$archive"
    done

    cd "$APP_DIR/app/src/main/cpp"
    sha256sum bootstrap-aarch64.zip > bootstrap-ominal.sha256

    cd "$APP_DIR"
    if command -v powershell.exe >/dev/null 2>&1; then
        powershell.exe -NoProfile -ExecutionPolicy Bypass -Command \
            "Set-Location -LiteralPath 'C:\Users\saura\skynet\termux-app'; .\gradlew.bat :app:validateOminalBootstraps"
    else
        ./gradlew.bat :app:validateOminalBootstraps
    fi

    printf 'DONE %s\n' "$(date -Is)"
    printf '%s\n' complete > "$STATUS"
} > "$LOG" 2>&1
