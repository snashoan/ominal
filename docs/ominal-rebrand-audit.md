# Ominal Rebrand Audit

This file separates release-blocking package identity from inherited upstream names.

## Completed Package Identity Work

- Android package/application identity is configured as `com.ominal`.
- Runtime prefix is expected at `/data/data/com.ominal/files/usr`.
- Java/Kotlin source package declarations were moved from `com.termux` to `com.ominal`.
- Manifest authorities and permissions are parameterized through the Ominal package entity.
- Gradle validation rejects bootstrap archives containing `/data/data/com.termux` or `/data/user/0/com.termux`.
- A current source-built ARM64 bootstrap is required before release. Build status
  and verification evidence are recorded by `docs/ominal-bootstrap.md`; do not
  treat an older archive as release evidence after the package source changes.
- `README.md` and `docs/en/index.md` now describe Ominal instead of the upstream app.

## Preserved Upstream/Legal References

- `LICENSE.md` and `ominal-shared/LICENSE.md` must retain upstream license attribution.
- Some upstream package names remain in the bootstrap ecosystem, including packages such as `termux-core`, `termux-exec`, `termux-tools`, and `termux-am`.
- The build system still consumes the upstream `termux-packages` source layout to produce Ominal-targeted bootstraps.
- The published `com.termux:termux-am-library` dependency namespace remains external unless that library is forked and republished.
- The native X server currently exports inherited JNI ABI symbols. Renaming
  those symbols requires rebuilding that native dependency and is not a string
  replacement task.

## Remaining Rebrand Debt

- The explicit settings-import migration retains the source product name so
  existing users can identify compatible archives; it is not the application
  identity or runtime package name.
- Package-build compatibility environment variables and native library names
  remain where they are contracts with the current upstream package ecosystem.
- Historical handoff/checkpoint files contain old `com.termux` commands and should not be treated as current install instructions.
- A full independent ecosystem release would require a package-repo fork with renamed package metadata, package URLs, repository metadata, and update channels.

## Current Release Rule

For the current Ominal prototype, the hard release gate is runtime identity:

- app installs as `com.ominal`;
- bootstraps are source-built for `/data/data/com.ominal/files/usr`;
- app build validation passes;
- device or emulator smoke test passes.

Current status:

- Runtime bootstrap/build validation passes.
- Device install is not yet proven because Android rejected the ADB install with `INSTALL_FAILED_USER_RESTRICTED: Install canceled by user`.

Do not claim the package ecosystem is fully rebranded until the upstream package-repo names and update channels are forked deliberately.
