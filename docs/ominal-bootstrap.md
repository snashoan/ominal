# Ominal Bootstrap Build

Ominal uses `com.ominal` as the Android package and `/data/data/com.ominal/files/usr` as `$PREFIX`.
The bootstrap archives in `app/src/main/cpp` must be built for that prefix from package sources.

Do not patch `bootstrap-*.zip` binaries after the fact. The archive contents, native binaries, package metadata, and maintainer scripts must come from a source build configured for `com.ominal`.

## Source Build Flow

The upstream Termux package build system defines the app package in `scripts/properties.sh` as `TERMUX_APP__PACKAGE_NAME`. For Ominal this must be:

```sh
TERMUX_APP__PACKAGE_NAME="com.ominal"
```

Build bootstraps with the package build system, not the public Termux package repo. Public Termux repos are built for `com.termux`, so generated bootstraps from those repos are not valid for Ominal.

The reviewed package-source mirror is kept at `external/termux-packages`. The
actual build checkout is the complete native Linux tree at
`/root/ominal/termux-packages-ominal`; a Windows sparse checkout is not a valid
package build input because required package groups such as `root-packages`
may be absent. Both trees carry the same committed Ominal identity in
`scripts/properties.sh`; build tooling must never rewrite it dynamically.
`tools/ominal-bootstrap-source.lock` pins both the reviewed upstream parent and
the complete Ominal source-tree hash. The runner also rejects tracked or staged
changes, an absent `root-packages` tree, and a checkout the package builder
cannot write to.

For a foreground ARM64 build on Windows:

```powershell
.\tools\build-ominal-bootstraps.ps1 -Architectures aarch64
```

For a build that survives closing the invoking terminal:

```powershell
.\tools\start-ominal-bootstrap-build-windows.ps1 -Architectures aarch64
```

To continue packages already built from the same verified source revision and
Ominal prefix after an interrupted WSL VM, add `-Resume`. The normal command
cleans the dedicated builder before compiling; resume mode preserves its
source-built package outputs and still runs the archive and Gradle validation
gates before accepting the result.

The Windows launcher registers a user-level scheduled task so a Windows-owned
`wsl.exe` process remains attached for the complete build. This prevents WSL
from shutting down its VM when the invoking terminal exits. The runner records
progress in `build-logs/ominal-bootstrap-status.txt` and writes build output to
`/root/ominal/bootstrap-build.log` in WSL. Both
build paths run the Docker-backed source build, copy the produced
`bootstrap-*.zip` files into `app/src/main/cpp`, write
`app/src/main/cpp/bootstrap-ominal.sha256`, and run the Gradle archive gate.
The runner limits package builds and the builder container's CPU affinity to
four workers by default. The affinity also constrains upstream validation
steps that call `nproc` directly. Set `OMINAL_BOOTSTRAP_BUILD_JOBS` only when
the host has capacity for a higher limit.

Ominal uses the dedicated Docker container name `ominal-package-builder`.
Reusing the upstream builder name is prohibited because Docker retains the
source bind mount from container creation and can silently build a different
checkout. The runner verifies that mount and replaces only the disposable
Ominal builder container when the configured source path changes.

## Updating Package Sources

Update the maintained Ominal package-source branch first. Merge or rebase a
reviewed upstream revision, resolve the Ominal identity source changes there,
and push that branch to the Ominal package-source fork. Hydrate the complete
WSL checkout from that branch, then update both values in
`tools/ominal-bootstrap-source.lock` from `HEAD^` and `HEAD^{tree}`. A clean
ARM64 bootstrap build and the Gradle archive gate are required before changing
the bootstrap release referenced by the app.

The current local branch has no published Ominal package-source remote yet.
Creating that fork and protecting its bootstrap branch is therefore a release
prerequisite; the local checkout alone is not a reproducible release source.

Publish an accepted `bootstrap-aarch64.zip` as a GitHub release asset together
with its checksum and source revision. The AAB workflow requires that release
tag and SHA-256 as manual inputs, downloads the archive into the ignored local
build directory, recreates `bootstrap-ominal.sha256`, and verifies it before
Gradle runs. Bootstrap binaries are never committed to the application source
repository.

## Build Gate

`app/build.gradle` validates bootstrap archives before Java compilation:

- rejects `/data/data/com.termux` and `/data/user/0/com.termux`
- requires `/data/data/com.ominal/files/usr`
- requires `bootstrap-ominal.sha256`
- checks every packaged `bootstrap-*.zip` against that manifest

This intentionally fails the app build until source-built Ominal bootstraps are present.

## Upstream References

The relevant upstream flow is Termux's `build-bootstraps.sh`, which builds bootstrap archives from local package sources for forked apps. `generate-bootstraps.sh` is only suitable after a custom package repo exists for the custom package name.
