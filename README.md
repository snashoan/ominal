<p align="center">
  <img src="art/gir-mark.svg" width="128" height="128" alt="GIR light-cone mark">
</p>

<h1 align="center">GIR</h1>

<p align="center">
  A chat-first Android computer where an intelligence harness and its user share
  one persistent Linux workspace and touch display.
</p>

GIR keeps conversation at the front. Each chat owns its working files and agent
session; terminal output and the graphical desktop stay available without
turning the product into a terminal-first interface.

## What works

- Persistent agent sessions scoped to individual chats and workspaces.
- A native Android chat surface with Markdown, attachments, and inline media.
- A Linux userspace with package management, terminal tools, and an XFCE desktop.
- A shared portrait display controlled through touch, the Android keyboard, or an agent.
- Harness-driven authentication, model discovery, and commands.
- Monopot events for structured chat, progress, media, and computer-use state.
- Immutable Light and Dark themes plus separately stored custom themes.

## Architecture

| Layer | Responsibility |
| --- | --- |
| Android shell | Chat, navigation, lifecycle, permissions, files, and native display input |
| Agent runtime | Persistent per-chat harness processes and structured event transport |
| Linux workspace | ARM64 Ubuntu userspace, packages, repositories, and graphical applications |
| Monopot | Provider-neutral events between harnesses, chat, media, and computer use |
| Display | Native X11 surface with phone geometry, touch input, and mobile window policy |

The Android package is `com.ominal`. Runtime files live under
`/data/data/com.ominal/files`, while the guest sees its own `/root` workspace.
Bootstraps are source-built for this package identity; the build rejects archives
that still target upstream application paths.

## Build

Requirements:

- JDK 17
- Android SDK with API 35 and NDK support
- PowerShell 7 on Windows, or a compatible shell on Linux

Build and test the developer APK:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug --no-daemon
```

Validate only the packaged runtime archives:

```powershell
.\gradlew.bat :app:validateOminalBootstraps
```

When bootstrap contents change, rebuild them from their package sources. Do not
binary-patch upstream archives. The Windows entry point is:

```powershell
.\tools\start-ominal-bootstrap-build-windows.ps1
```

Generated archives, APKs, AABs, device captures, signing material, auth files,
and build logs are intentionally excluded from Git.

## Interface contracts

- [Monopot protocol](docs/MONOPOT.md)
- [Custom themes](docs/UI_THEMES.md)
- [GIR mark](docs/GIR_MARK.md)
- [Quick start](docs/GIR_QUICKSTART.md)

## Licensing

GIR remains a GPLv3 derivative of `termux/termux-app`. The renamed package,
runtime, and public identity do not remove upstream attribution or source
availability requirements. See [LICENSE.md](LICENSE.md) and the component license
files for inherited Apache, MIT, and GPL exceptions.
