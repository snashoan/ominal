# Ominal

Ominal is an Android coding-agent workspace built on a Linux userspace bootstrap, terminal execution, and a chat-first front end.

The current package identity is `com.ominal`. Runtime files are expected under:

```text
/data/data/com.ominal/files/usr
```

The app build intentionally rejects bootstraps that still target `com.termux` data paths.

## Current Scope

- Chat-first Android shell UI for a coding agent.
- Local executable area under the app sandbox.
- Source-built bootstrap archives for `com.ominal`.
- Validation before Java compilation to prevent accidentally shipping upstream-package bootstraps.

This repo is not ready to publish until the generated bootstraps, Android build, and device or emulator smoke tests all pass.

## Bootstrap Build

Do not binary-patch bootstrap zips. Build them from package sources configured for `com.ominal`.

The WSL/Docker runner is:

```powershell
.\tools\start-ominal-bootstrap-build-windows.ps1
```

The runner writes status to:

```text
build-logs/ominal-bootstrap-status.txt
```

The generated archives must be copied into:

```text
app/src/main/cpp/bootstrap-aarch64.zip
app/src/main/cpp/bootstrap-arm.zip
app/src/main/cpp/bootstrap-i686.zip
app/src/main/cpp/bootstrap-x86_64.zip
app/src/main/cpp/bootstrap-ominal.sha256
```

Then run:

```powershell
.\gradlew.bat :app:validateOminalBootstraps
.\gradlew.bat assembleDebug
```

## Git Hygiene

Local agent sources, auth files, external clones, build logs, screenshots, and downloaded APKs are ignored.

Generated bootstrap zips may exceed normal GitHub blob limits. Use an Ominal-owned remote plus Git LFS or release assets for bootstraps unless the target remote explicitly supports large files.

Do not push this fork to the upstream Termux remote.

## Licensing

This fork inherits licensing from upstream Termux app sources. The main app is GPLv3-only, with documented Apache, MIT, and GPL exceptions in subcomponents. Keep `LICENSE.md`, `termux-shared/LICENSE.md`, source notices, and source availability intact.

Ominal branding and `com.ominal` package identity do not remove upstream attribution requirements.

See `docs/ominal-rebrand-audit.md` for the current boundary between completed Ominal package identity work and remaining inherited upstream naming.
