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

On Windows from this repo:

```powershell
.\tools\build-ominal-bootstraps.ps1 -AllArchitectures -ForceRebuild
```

For a faster first pass:

```powershell
.\tools\build-ominal-bootstraps.ps1 -Architectures aarch64 -ForceRebuild
```

The script clones or updates `external/termux-packages`, edits the package build properties to `com.ominal`, runs the Docker-backed bootstrap build, copies the produced `bootstrap-*.zip` files into `app/src/main/cpp`, and writes `app/src/main/cpp/bootstrap-ominal.sha256`.

## Build Gate

`app/build.gradle` validates bootstrap archives before Java compilation:

- rejects `/data/data/com.termux` and `/data/user/0/com.termux`
- requires `/data/data/com.ominal/files/usr`
- requires `bootstrap-ominal.sha256`
- checks every packaged `bootstrap-*.zip` against that manifest

This intentionally fails the app build until source-built Ominal bootstraps are present.

## Upstream References

The relevant upstream flow is Termux's `build-bootstraps.sh`, which builds bootstrap archives from local package sources for forked apps. `generate-bootstraps.sh` is only suitable after a custom package repo exists for the custom package name.
