# Ominal / Oringutan Checkpoint

Date: 2026-07-13 20:55 IST

## Product Contract

- Chat is the default and primary interface.
- The Linux screen is preloaded but hidden until the screen icon is tapped or
  Codex requests direct user input.
- Screen mode is immersive fullscreen. The close button returns to chat.
- The selected chat is represented by the highlighted drawer row; the header
  remains `Ominal`.
- The screen is touch-first, phone-shaped, pointer-free, and portrait-only.

## Repository State

- Workspace: `C:\Users\saura\skynet\termux-app`
- Branch: `ominal/main`
- Previous remote commit: `dc86006b`
- APK: `app/build/outputs/apk/debug/ominal-app_apt-android-7-debug_arm64-v8a.apk`
- Runtime marker:
  `ominal-ubuntu-24.04.4-node-24.15.0-codex-0.144.1-desktop-v4`
- Rootfs marker: `ubuntu-base-24.04.4-arm64-nohardlinks-v3`

Implemented in this checkpoint:

- Pure-black, chat-first native UI with direct chat, new-chat, screen, and
  account actions.
- Removed the inherited three-dot feature menu and old ChatGPT-app bridge.
- Native Codex account sheet, ChatGPT device-auth flow, API-key login, auth
  status refresh, cancellation, and sanitized sign-in errors.
- Chat history, per-chat workspaces, attachments, terminal view, and automatic
  screen handoff markers.
- Portrait lock in the activity manifest and activity lifecycle.
- Ominal-owned noVNC page with touch input, fixed portrait scaling, no cursor,
  no session resize, reconnect behavior, and native connection-state reporting.
- Deterministic display teardown that terminates the old PRoot tracer before
  replacing Xvfb, JWM, x11vnc, and websockify.
- JWM phone-sized shell with Files, Terminal, and Editor launchers.
- `ominal-screen` controls for screenshots, taps, typing, keys, windows, and
  geometry.

## Device Validation

- Device: Raphael / Redmi K20 Pro (`raphaelin`)
- Wireless ADB: `10.91.157.55:41755`
- Android package: `com.ominal`
- ABI: `arm64-v8a`
- Installed update time: 2026-07-13 20:42 IST

Validated:

- `:app:testDebugUnitTest` and `:app:assembleDebug` pass.
- Runtime bootstrap `--verify` passes, including Node `v24.15.0`, Codex CLI
  `0.144.1`, clean `dpkg --audit`, and all display commands.
- Runtime/tool script mirrors are byte-identical and `git diff --check` passes.
- Packaged manifest contains `android:screenOrientation="portrait"`.
- Android reports portrait, fullscreen bounds `1080x2340`, and
  `ROTATION_0`.
- Display endpoint `/ominal.html` returns HTTP 200.
- Live WebView URL is a valid `ominal.html?_ominal=...` URL.
- Live WebView DOM reports `ready=complete`, title `Ominal screen`, and a
  `540x1170` framebuffer canvas.
- x11vnc reports an active WebView client, tight encoding, quality 6,
  compression 6, and 4 ms measured latency.
- The WebView canvas was extracted and visually checked; it renders the full
  portrait shell instead of the previous blank error page.
- Exactly one current Xvfb/JWM/x11vnc/websockify display stack remains.
- Temporary `deviceidle` whitelist entry for `com.ominal` was removed.

## Remaining Manual Check

The phone was locked behind AOD during final validation. After unlocking, tap
the screen icon once and confirm the visible full-screen transition and close
button. The rendered WebView canvas and VNC connection are already validated;
this is the remaining physical interaction check.

No package installer, Gradle build, or display migration job is pending.
