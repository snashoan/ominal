#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${1:-/mnt/c/Users/saura/skynet/termux-app}"
TP_DIR="${2:-/root/ominal/termux-packages}"
ARCHES="${3:-aarch64,arm,i686,x86_64}"
LOG="${4:-/root/ominal/bootstrap-build.log}"
STATUS="$APP_DIR/build-logs/ominal-bootstrap-status.txt"

mkdir -p "$(dirname "$STATUS")"
echo "running" > "$STATUS"
trap 'rc=$?; if [ "$rc" -ne 0 ]; then echo "failed:$rc" > "$STATUS"; fi' EXIT

require_build_disk_space() {
  local min_mb="${OMINAL_MIN_C_DRIVE_FREE_MB:-8192}"
  local free_mb
  free_mb="$(df -Pm "$APP_DIR" | awk 'NR == 2 { print $4 }')"
  if [[ -z "$free_mb" || "$free_mb" -lt "$min_mb" ]]; then
    echo "Need at least ${min_mb}MB free on the app checkout drive before building bootstraps; found ${free_mb:-unknown}MB." >&2
    return 1
  fi
}

ominal_package_debs_clean() {
  local package_name="$1"
  local arch="$2"
  local tmpdir

  shopt -s nullglob
  local debs=(output/"${package_name}"_*_"${arch}".deb)
  shopt -u nullglob

  if (( ${#debs[@]} == 0 )); then
    return 1
  fi

  tmpdir="$(mktemp -d)"
  for deb in "${debs[@]}"; do
    dpkg-deb -x "$deb" "$tmpdir"
    mkdir -p "$tmpdir/DEBIAN-$(basename "$deb")"
    dpkg-deb -e "$deb" "$tmpdir/DEBIAN-$(basename "$deb")"
  done

  if grep -aR --binary-files=text -q '/data/data/com.termux' "$tmpdir"; then
    rm -rf "$tmpdir"
    return 1
  fi

  rm -rf "$tmpdir"
  return 0
}

{
  echo "START $(date -Is)"
  echo "Goal: source-build Ominal bootstraps for com.ominal"
  require_build_disk_space

  cd "$TP_DIR"
  sed -i -E 's/^TERMUX__NAME=.*/TERMUX__NAME="Ominal"/' scripts/properties.sh
  sed -i -E 's/^TERMUX_APP__PACKAGE_NAME=.*/TERMUX_APP__PACKAGE_NAME="com.ominal"/' scripts/properties.sh

  if grep -q 'TERMUX_BUILT_PACKAGES_DIRECTORY_FOR_ARCH' scripts/build-bootstraps.sh; then
    echo "Applying source fix: build-bootstraps force cleanup uses TERMUX_BUILT_PACKAGES_DIRECTORY"
    sed -i 's/"$TERMUX_BUILT_PACKAGES_DIRECTORY_FOR_ARCH"\/\*/"$TERMUX_BUILT_PACKAGES_DIRECTORY"\/\*/' scripts/build-bootstraps.sh
  fi

  if ! grep -q 'rm -f "$TERMUX_BUILT_PACKAGES_DIRECTORY"/\*' scripts/build-bootstraps.sh; then
    echo "Expected bootstrap cleanup source fix was not applied." >&2
    exit 1
  fi

  if ! grep -q 'Unsafe TERMUX_BUILT_PACKAGES_DIRECTORY for force cleanup' scripts/build-bootstraps.sh; then
    sed -i '/rm -f "\$TERMUX_BUILT_PACKAGES_DIRECTORY"\/\*/i\            [[ -n "$TERMUX_BUILT_PACKAGES_DIRECTORY" && "$TERMUX_BUILT_PACKAGES_DIRECTORY" != "/" ]] || { echo "Unsafe TERMUX_BUILT_PACKAGES_DIRECTORY for force cleanup" >&2; return 1; }' scripts/build-bootstraps.sh
  fi

  if grep -q 'add_termux_bootstrap_second_stage_files "$package_arch"' scripts/build-bootstraps.sh; then
    echo "Applying source fix: bootstrap second stage uses TERMUX_ARCH"
    sed -i 's/add_termux_bootstrap_second_stage_files "\$package_arch"/add_termux_bootstrap_second_stage_files "\$TERMUX_ARCH"/' scripts/build-bootstraps.sh
  fi

  if ! grep -q 'add_termux_bootstrap_second_stage_files "$TERMUX_ARCH"' scripts/build-bootstraps.sh; then
    echo "Expected second-stage architecture source fix was not applied." >&2
    exit 1
  fi

  if grep -q 'PACKAGES+=("bzip2")' scripts/build-bootstraps.sh && [[ -d packages/libbz2 && ! -d packages/bzip2 ]]; then
    echo "Applying source fix: bootstrap package list builds libbz2 for bzip2 subpackage"
    sed -i 's/PACKAGES+=("bzip2")/PACKAGES+=("libbz2")/' scripts/build-bootstraps.sh
  fi

  if grep -q 'https://fossies.org/linux/misc/bzip2-${TERMUX_PKG_VERSION}.tar.xz' packages/libbz2/build.sh; then
    echo "Applying source fix: libbz2 source URL uses sourceware tar.gz"
    sed -i 's|https://fossies.org/linux/misc/bzip2-${TERMUX_PKG_VERSION}.tar.xz|https://sourceware.org/pub/bzip2/bzip2-${TERMUX_PKG_VERSION}.tar.gz|' packages/libbz2/build.sh
    sed -i 's|47fd74b2ff83effad0ddf62074e6fad1f6b4a77a96e121ab421c20a216371a1f|ab5a03176ee106d3f0fa90e381da478ddae405918153cca248e682cd0c4a2269|' packages/libbz2/build.sh
  fi

  if grep -q 'https://fossies.org/linux/misc/db-${TERMUX_PKG_VERSION}.tar.gz' packages/libdb/build.sh; then
    echo "Applying source fix: libdb source URL uses Oracle upstream tar.gz"
    sed -i 's|https://fossies.org/linux/misc/db-${TERMUX_PKG_VERSION}.tar.gz|https://download.oracle.com/berkeley-db/db-${TERMUX_PKG_VERSION}.tar.gz|' packages/libdb/build.sh
  fi

  if grep -q 'https://fossies.org/linux/misc/psmisc-$TERMUX_PKG_VERSION.tar.xz' packages/psmisc/build.sh; then
    echo "Applying source fix: psmisc source URL uses SourceForge release tar.xz"
    sed -i 's|https://fossies.org/linux/misc/psmisc-$TERMUX_PKG_VERSION.tar.xz|https://downloads.sourceforge.net/project/psmisc/psmisc/psmisc-$TERMUX_PKG_VERSION.tar.xz|' packages/psmisc/build.sh
  fi

  if grep -q 'https://fossies.org/linux/misc/psmisc-\$TERMUX_PKG_VERSION.tar.xz' packages/psmisc/build.sh; then
    echo "Applying source fix: psmisc source URL uses SourceForge"
    sed -i 's|https://fossies.org/linux/misc/psmisc-\$TERMUX_PKG_VERSION.tar.xz|https://downloads.sourceforge.net/project/psmisc/psmisc/psmisc-\$TERMUX_PKG_VERSION.tar.xz|' packages/psmisc/build.sh
  fi

  if ! grep -q 'Ominal path rewrite for forked app package' packages/termux-tools/build.sh; then
    echo "Applying source fix: termux-tools rewrites hardcoded upstream data paths during package build"
    sed -i '/TERMUX_PKG_CONFFILES=/a\
\	# Ominal path rewrite for forked app package.\
\	find "$TERMUX_PREFIX" -type f -print0 | xargs -0 -r grep -lZ --binary-files=without-match "/data/data/com.termux" | xargs -0 -r sed -i "s|/data/data/com.termux|$TERMUX_APP__DATA_DIR|g"' packages/termux-tools/build.sh
  fi

  if ! grep -q 'Ominal package metadata path rewrite' packages/termux-tools/build.sh; then
    echo "Applying source fix: termux-tools rewrites hardcoded upstream data paths in package metadata"
    cat >> packages/termux-tools/build.sh <<'EOF'

termux_step_post_massage() {
	# Ominal package metadata path rewrite.
	find "$TERMUX_PKG_MASSAGEDIR" -type f -print0 | xargs -0 -r grep -lZ --binary-files=without-match "/data/data/com.termux" | xargs -0 -r sed -i "s|/data/data/com.termux|$TERMUX_APP__DATA_DIR|g"
}
EOF
  fi

  if ! grep -q 'Ominal package metadata safe path rewrite' packages/termux-tools/build.sh; then
    echo "Applying source fix: termux-tools metadata rewrite tolerates clean trees"
    cat >> packages/termux-tools/build.sh <<'EOF'

termux_step_post_massage() {
	# Ominal package metadata safe path rewrite.
	while IFS= read -r -d '' file; do
		if grep -a -q "/data/data/com.termux" "$file"; then
			sed -i "s|/data/data/com.termux|$TERMUX_APP__DATA_DIR|g" "$file"
		fi
	done < <(find "$TERMUX_PKG_MASSAGEDIR" -type f -print0)
}
EOF
  fi

  if ! grep -q 'Ominal package metadata loop path rewrite' packages/termux-tools/build.sh; then
    echo "Applying source fix: termux-tools metadata rewrite avoids pipefail"
    cat >> packages/termux-tools/build.sh <<'EOF'

termux_step_post_massage() {
	# Ominal package metadata loop path rewrite.
	while IFS= read -r -d '' file; do
		if grep -a -q "/data/data/com.termux" "$file"; then
			sed -i "s|/data/data/com.termux|$TERMUX_APP__DATA_DIR|g" "$file"
		fi
	done < <(find "$TERMUX_PKG_MASSAGEDIR" -type f -print0)
}
EOF
  fi

  if ! grep -q 'Ominal debscripts path rewrite' packages/termux-tools/build.sh; then
    echo "Applying source fix: termux-tools debscripts rewrite generated preinst"
    cat >> packages/termux-tools/build.sh <<'EOF'

termux_step_create_debscripts() {
	# Ominal debscripts path rewrite.
	sed "s|/data/data/com.termux|$TERMUX_APP__DATA_DIR|g" "$TERMUX_PKG_BUILDDIR/preinst" > ./preinst
}
EOF
  fi

  if ! grep -q 'Ominal path rewrite for forked app package' packages/termux-exec/build.sh; then
    echo "Applying source fix: termux-exec rewrites hardcoded upstream data paths during package build"
    awk '
      /^termux_step_post_massage\(\) \{$/ { in_massage = 1 }
      in_massage && /^\}$/ {
        print "\t# Ominal path rewrite for forked app package."
        print "\tgrep -rlZ --binary-files=without-match \"/data/data/com.termux\" \"$TERMUX_PKG_MASSAGEDIR/$TERMUX_PREFIX\" | xargs -0 -r sed -i \"s|/data/data/com.termux|$TERMUX_APP__DATA_DIR|g\""
        in_massage = 0
      }
      { print }
    ' packages/termux-exec/build.sh > packages/termux-exec/build.sh.tmp
    mv packages/termux-exec/build.sh.tmp packages/termux-exec/build.sh
  fi

  if grep -q 'zip -r9 "${BOOTSTRAP_TMPDIR}/bootstrap-${1}.zip" ./\*' scripts/build-bootstraps.sh; then
    echo "Applying source fix: bootstrap archive creation uses quiet moderate compression"
    sed -i 's|zip -r9 "${BOOTSTRAP_TMPDIR}/bootstrap-${1}.zip" ./\*|zip -q -r -6 "${BOOTSTRAP_TMPDIR}/bootstrap-${1}.zip" ./*|' scripts/build-bootstraps.sh
  fi

  if ! grep -q 'zip -q -r -6 "${BOOTSTRAP_TMPDIR}/bootstrap-${1}.zip" ./\*' scripts/build-bootstraps.sh; then
    echo "Expected bootstrap archive source fix was not applied." >&2
    exit 1
  fi

  if ! grep -q 'Bootstrap archive was not created' scripts/build-bootstraps.sh; then
    sed -i '/zip -q -r -6 "\${BOOTSTRAP_TMPDIR}\/bootstrap-\${1}.zip" \.\/\*/a\		[ -s "${BOOTSTRAP_TMPDIR}/bootstrap-${1}.zip" ] || { echo "Bootstrap archive was not created: ${BOOTSTRAP_TMPDIR}/bootstrap-${1}.zip" >&2; exit 1; }' scripts/build-bootstraps.sh
  fi

  chown -R 1001:1001 "$TP_DIR"
  if [[ "${OMINAL_RESET_BUILDER:-0}" == "1" ]]; then
    docker rm -f termux-package-builder >/dev/null 2>&1 || true
  fi

  if [[ "${OMINAL_FORCE_REBRAND_PACKAGES:-1}" == "1" ]]; then
    echo "Forcing rebuild of package payloads with forked app paths"
    if docker container inspect termux-package-builder >/dev/null 2>&1; then
      docker start termux-package-builder >/dev/null 2>&1 || true
      docker exec termux-package-builder bash -lc 'rm -f /data/data/.built-packages/termux-tools /data/data/.built-packages/termux-exec'
      if docker exec termux-package-builder bash -lc 'test ! -e /data/data/.built-packages/termux-tools && test ! -e /data/data/.built-packages/termux-exec'; then
        echo "Cleared existing termux-tools and termux-exec build markers"
      else
        echo "Failed to clear termux-tools and termux-exec build markers" >&2
        exit 1
      fi
    fi
  fi

  echo "Properties:"
  grep -n 'TERMUX__NAME=\|TERMUX_APP__PACKAGE_NAME=\|TERMUX_APP__DATA_DIR=' scripts/properties.sh | head -20

  echo "Builder self-report:"
  ./scripts/build-bootstraps.sh --help | grep -E 'TERMUX_APP_PACKAGE|TERMUX_PREFIX|TERMUX_ARCHITECTURES' || true

  if [[ "${OMINAL_FORCE_REBRAND_PACKAGES:-1}" == "1" ]]; then
    echo "Prebuilding path-sensitive packages with forced source rebuild"
    IFS=',' read -r -a ominal_arches <<< "$ARCHES"
    for arch in "${ominal_arches[@]}"; do
      if ominal_package_debs_clean termux-exec "$arch"; then
        echo "Reusing clean forced rebuild output for termux-exec $arch"
      else
        ./scripts/run-docker.sh ./build-package.sh -f -a "$arch" termux-exec
      fi

      if ominal_package_debs_clean termux-tools "$arch"; then
        echo "Reusing clean forced rebuild output for termux-tools $arch"
      else
        ./scripts/run-docker.sh ./build-package.sh -f -a "$arch" termux-tools
      fi
    done

    shopt -s nullglob
    for arch in "${ominal_arches[@]}"; do
      termux_exec_debs=(output/termux-exec_*_"$arch".deb)
      termux_tools_debs=(output/termux-tools_*_"$arch".deb)
      if (( ${#termux_exec_debs[@]} == 0 || ${#termux_tools_debs[@]} == 0 )); then
        echo "Missing forced rebuild debs for $arch: termux-exec=${#termux_exec_debs[@]} termux-tools=${#termux_tools_debs[@]}" >&2
        exit 1
      fi
    done
    shopt -u nullglob
  fi

  echo "Running build for $ARCHES"
  require_build_disk_space
  build_args=(./scripts/build-bootstraps.sh --architectures "$ARCHES")
  if [[ "${OMINAL_FORCE_BOOTSTRAPS:-0}" == "1" ]]; then
    build_args=(./scripts/build-bootstraps.sh -f --architectures "$ARCHES")
  fi
  ./scripts/run-docker.sh "${build_args[@]}"

  echo "Copying bootstrap archives into repo"
  require_build_disk_space
  cp -f bootstrap-aarch64.zip "$APP_DIR/app/src/main/cpp/bootstrap-aarch64.zip"
  cp -f bootstrap-arm.zip "$APP_DIR/app/src/main/cpp/bootstrap-arm.zip"
  cp -f bootstrap-i686.zip "$APP_DIR/app/src/main/cpp/bootstrap-i686.zip"
  cp -f bootstrap-x86_64.zip "$APP_DIR/app/src/main/cpp/bootstrap-x86_64.zip"

  cd "$APP_DIR/app/src/main/cpp"
  sha256sum bootstrap-aarch64.zip bootstrap-arm.zip bootstrap-i686.zip bootstrap-x86_64.zip > bootstrap-ominal.sha256
  echo "Wrote $APP_DIR/app/src/main/cpp/bootstrap-ominal.sha256"

  echo "Running Gradle bootstrap validation"
  cd "$APP_DIR"
  if command -v powershell.exe >/dev/null 2>&1; then
    powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "Set-Location -LiteralPath 'C:\Users\saura\skynet\termux-app'; .\gradlew.bat :app:validateOminalBootstraps"
  else
    ./gradlew.bat :app:validateOminalBootstraps
  fi

  echo "DONE $(date -Is)"
  echo "complete" > "$STATUS"
} > "$LOG" 2>&1
