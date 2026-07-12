# Ominal Handoff

## Checkpoint - 2026-07-06 18:20 IST

Source-built Ominal bootstraps are now integrated and validated.

Verified evidence:

- WSL runner status: `build-logs\ominal-bootstrap-status.txt` says `complete`.
- WSL source tree: `/root/ominal/termux-packages`.
- WSL log ended with `DONE 2026-07-06T12:40:15+00:00`.
- The runner copied all four generated archives into `app\src\main\cpp` and wrote `bootstrap-ominal.sha256`.
- Independent zip scan found zero `/data/data/com.termux` hits in:
  - `bootstrap-aarch64.zip`
  - `bootstrap-arm.zip`
  - `bootstrap-i686.zip`
  - `bootstrap-x86_64.zip`
- SHA-256 manifest matches the copied archives:
  - `de76c99189e4b6d5791f6012465f860064424cbf398541f38de339d329fa063b  bootstrap-aarch64.zip`
  - `f1b2ddecf90cfa306e519589a009b7ba1178024f0cba0e7a390d749e774481a8  bootstrap-arm.zip`
  - `c425f279c67487ad47dde91e4dbeaa38e79d22915c7d920d02097a7a02ed91b1  bootstrap-i686.zip`
  - `96c54c25d21f4d3c31c2a46a792860104ac757b0b0dca726c2ec9556cefb5701  bootstrap-x86_64.zip`
- Gradle bootstrap validation passed:

```powershell
.\gradlew.bat :app:validateOminalBootstraps
```

- Android debug build passed:

```powershell
.\gradlew.bat assembleDebug
```

- Fresh APKs:
  - `app\build\outputs\apk\debug\ominal-app_apt-android-7-debug_universal.apk`, about `546 MB`
  - `app\build\outputs\apk\debug\ominal-app_apt-android-7-debug_arm64-v8a.apk`, about `145 MB`
  - plus `armeabi-v7a`, `x86`, and `x86_64` debug APKs.

ADB install status:

- Wireless device was connected:
  - `10.172.121.55:42801`
  - `Redmi_K20_Pro`
- Universal APK install timed out because the file is large.
- ABI-specific `arm64-v8a` install reached the device but failed with:

```text
INSTALL_FAILED_USER_RESTRICTED: Install canceled by user
```

Next install attempt should use:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$apk = "C:\Users\saura\skynet\termux-app\app\build\outputs\apk\debug\ominal-app_apt-android-7-debug_arm64-v8a.apk"
& $adb -s 10.172.121.55:42801 install -r $apk
```

If it fails again, approve the install prompt/device setting on the phone. The build itself is already valid.

Remaining Termux/legal/upstream references:

- License and attribution files must keep upstream Termux notices.
- Bootstrap package names such as `termux-tools`, `termux-exec`, `termux-core`, and `termux-am` still exist in the package ecosystem.
- Gradle modules, Java class names, style names, resource keys, art/Fastlane metadata, docs, and historical handoff commands still contain inherited `Termux`/`termux` names.
- The package repo/update channel is still upstream-shaped. A fully independent release needs a deliberate fork of package metadata, repository URLs, update channels, and release artifact storage.
- Current git remote is still upstream Termux only; do not push until an Ominal-owned remote is configured.
- Bootstrap zips are too large for normal GitHub blobs, so use Git LFS or release assets.

## Checkpoint - 2026-07-04 20:16 IST

The resumed source-built bootstrap pass is running again after C: disk recovery.

Current evidence:

- Windows host wrapper PID: `36480`
- Status file: `build-logs\ominal-bootstrap-status.txt`
- Current status: `running`
- WSL source tree: `/root/ominal/termux-packages`
- WSL log: `/root/ominal/bootstrap-build.log`
- Completed source-built archives:
  - `/root/ominal/termux-packages/bootstrap-aarch64.zip`, about `135M`
  - `/root/ominal/termux-packages/bootstrap-arm.zip`, about `127M`
- Current architecture:
  - `i686`
- Recent `i686` package progress:
  - package output count observed at `430`
  - active package observed around GNU `coreutils`
  - log paths are using `/data/data/com.ominal/files/usr`
- Repo `app\src\main\cpp` still intentionally contains the old June bootstraps until all four generated archives are available and copied together.

Disk/build recovery:

- C: was nearly full and caused the previous arm zip failure.
- Generated Gradle/build outputs, temp files, and rebuildable user caches were cleared.
- C: free space recovered to roughly `19G` while `i686` was building.
- `tools\run-ominal-bootstrap-build-wsl.sh` now has an app-checkout-drive free-space guard before build/copy steps.

Repo-facing metadata:

- `README.md` and `docs\en\index.md` now describe Ominal instead of presenting this repo as the upstream Termux app.
- License notices were intentionally preserved; this fork still inherits GPLv3-only upstream app licensing plus documented subcomponent exceptions.

Git/push guard remains unchanged:

- `termux-app` remote is still only `origin https://github.com/termux/termux-app.git`.
- No Ominal-owned remote is configured.
- Do not push to upstream Termux.
- Pushing is blocked until a valid Ominal-owned remote/branch is configured.
- The generated source-built bootstrap zips exceed normal GitHub single-blob limits, so use Git LFS or release assets unless the final remote explicitly supports large normal git blobs.

## Checkpoint - 2026-07-04 17:55 IST

The source-built bootstrap pass is still running in WSL/Docker.

Current evidence:

- Windows host wrapper PID: `34404`
- Status file: `build-logs\ominal-bootstrap-status.txt`
- Current status: `running`
- WSL source tree: `/root/ominal/termux-packages`
- WSL log: `/root/ominal/bootstrap-build.log`
- Completed source-built archive:
  - `/root/ominal/termux-packages/bootstrap-aarch64.zip`, about `134M`
- Current architecture:
  - `arm`
- Recent `arm` package progress:
  - completed `libicu`, `ncurses`, `readline`, `gdbm`, `libbz2`, `libffi`, `liblzma`, `tcl`, `libsqlite`, `brotli`
  - active package observed around `libpng`
- Repo `app\src\main\cpp` still intentionally contains the old June bootstraps until all four generated archives are available and the runner copies them together.

Git/push guard:

- `termux-app` remote is still only `origin https://github.com/termux/termux-app.git`.
- No Ominal-owned remote is configured.
- Do not push to upstream Termux.
- Pushing is blocked until a valid Ominal-owned remote/branch is configured.
- The generated bootstrap zip size is already above GitHub's normal single-file limit, so the proper publish path likely needs Git LFS or release assets for bootstraps instead of a normal git blob commit.

Device/emulator state:

- `adb devices` currently shows no attached device.
- Emulator binary and Android 36/36.1 x86_64 Google Play system images exist locally.
- No AVD is currently listed, and C: has limited free space, so avoid downloading emulator components unless strictly required.

## Checkpoint - 2026-07-04 15:10 IST

The source-built bootstrap pass is still running. Current Windows host wrapper PID is `5156`, and `build-logs\ominal-bootstrap-status.txt` says `running`.

Important source fixes are now part of `tools\run-ominal-bootstrap-build-wsl.sh`:

- patch `scripts/build-bootstraps.sh` so force cleanup uses `TERMUX_BUILT_PACKAGES_DIRECTORY`, with a guard against empty/root targets;
- patch second-stage bootstrap generation to pass `"$TERMUX_ARCH"` instead of undefined `"$package_arch"`;
- chown `/root/ominal/termux-packages` to `1001:1001` so the Docker `builder` user can write outputs;
- keep `TERMUX_APP__PACKAGE_NAME=com.ominal` and `TERMUX__NAME=Ominal` in `scripts/properties.sh`.

The corrected run had reached real `aarch64` package builds with `/data/data/com.ominal/files/usr` paths before WSL became slow to accept new status commands. No corrected bootstrap zips were present at that checkpoint.

Later observed progress: status still `running`; `aarch64` had built through `doxygen` and `libandroid-glob`, and was building `libicu`. No corrected `bootstrap-*.zip` existed yet.

Latest progress:

- Patched flaky Fossies recipe URLs in the runner for `libbz2`, `libdb`, and `psmisc`.
- Runner now defaults to resume mode instead of `-f`, preserving existing debs and the Docker builder container across retries.
- `bootstrap-aarch64.zip` was built successfully in `/root/ominal/termux-packages` at roughly `134M`.
- Build is continuing into the `arm` architecture.
- Repo `app\src\main\cpp` still has old bootstraps until all four source-built zips are available and copied.

## Checkpoint - 2026-07-04 14:45 IST

Current priority is the proper source-built Ominal bootstrap and release-readiness pass. Older UI-only tasks are historical context unless they directly block validation.

What is running:

- WSL/Docker bootstrap build is active through:

```powershell
.\tools\start-ominal-bootstrap-build-windows.ps1
```

- WSL-side runner:
  - `/mnt/c/Users/saura/skynet/termux-app/tools/run-ominal-bootstrap-build-wsl.sh`
  - source tree: `/root/ominal/termux-packages`
  - log: `/root/ominal/bootstrap-build.log`
  - status: `build-logs\ominal-bootstrap-status.txt`
- Log evidence already confirmed:
  - `TERMUX_APP_PACKAGE: "com.ominal"`
  - `TERMUX_PREFIX: "/data/data/com.ominal/files/usr"`
  - `TERMUX_ARCHITECTURES: "aarch64 arm i686 x86_64"`

Background Codex:

- Repo worker can be reattached with:

```powershell
.\contun.ps1
```

- Tail only:

```powershell
.\tools\ominal-bg-codex-tail.ps1 -Workspace . -Lines 120
```

- Stale UI-only queued prompts were skipped. A fresh 2026-07-04 long-running job was queued for source-built bootstraps, validation, device/emulator smoke testing, and safe push.

Push guard:

- Current `git remote -v` shows only `origin https://github.com/termux/termux-app.git`.
- Do not push this work to upstream Termux.
- Push only after checks pass and a valid Ominal-owned remote/branch is configured through existing credentials.

Required gates:

```powershell
git status --short
git remote -v
git diff --check
.\gradlew.bat :app:validateOminalBootstraps
.\gradlew.bat assembleDebug
```

Then install/smoke test on an attached ADB device. If no USB device is connected, use an emulator validation path instead.

Current device state on 2026-07-04:

- `adb devices -l` showed no attached devices.
- Emulator tooling exists at `C:\Users\saura\AppData\Local\Android\Sdk\emulator\emulator.exe`.
- Local Android 36/36.1 Google Play x86_64 system images are present.

## Checkpoint - 2026-07-02 14:57 IST

Direct work resumed after the detached Codex worker was found inactive.

Completed source changes:

- `app/src/main/java/com/termux/app/OringutanActivity.java`
  - Added drawer search state and a real `EditText` search field.
  - Replaced the dominant drawer `New chat` button with a compact `+` action.
  - Added chat filtering by title and latest visible non-system message.
  - Removed visible `ominal-*` session ids from drawer metadata.
  - Removed internal terminal name from the header subtitle.
  - Changed terminal pane visible label to `Per-chat terminal`.
  - Display WebView now clears stale loads and reloads noVNC with a timestamp query param.
- `tools/ominal-display-start.sh`
  - Bumped `DISPLAY_VERSION` to `2026-07-02-drawer-display-restart-1`.
- `continue-ominal.ps1`
  - Ensures `build-logs` exists.
  - Starts the display helper with `540x1076x24`.
  - Captures install screenshot to `build-logs\ominal-current-installed.png`.

Build verification:

```powershell
.\gradlew.bat assembleDebug
```

Result:

- `BUILD SUCCESSFUL`
- Fresh APK:
  - `app\build\outputs\apk\debug\termux-app_apt-android-7-debug_arm64-v8a.apk`
  - LastWriteTime: `2026-07-02 14:54:53 IST`
  - Size: `39,548,243` bytes

Install status:

- A detached watcher was started:

```powershell
.\continue-ominal.ps1
```

- It rebuilt successfully and is waiting for ADB device `O7ON59OZEY7LOVQG`.
- Logs:
  - `build-logs\ominal-install-watch.log`
  - `build-logs\ominal-install-watch.err.log`
- At checkpoint time, `adb devices` showed no attached device, so app install, launch, screenshots, and logcat checks are still pending.

Next step when the phone is attached/authorized:

```powershell
cd C:\Users\saura\skynet\termux-app
.\continue-ominal.ps1
```

This will rebuild if needed, wait for the phone, install the fresh arm64 APK, push `ominal-display-start`, launch Ominal, and capture `build-logs\ominal-current-installed.png`.

## Latest Direction - 2026-06-30

## Task Handout - Drawer Search / Display Surface

Latest user correction:

- Replace the large `New chat` control at the top of the chat drawer with a search field.
- Do not show internal `ominal-*` ids in the drawer. Those are implementation details.
- Chat drawer rows should be based on meaningful chat titles and recent usage context.
- Keep a way to create a new chat, but it should not be the dominant first control in the drawer. Prefer a small icon/action near search or in the main header.
- Display mode interpretation is now corrected:
  - keep the close `X`,
  - remove the full permanent `Display ... X` pill/header,
  - display should be the surface itself with only a small floating `X`.
- Pointer/cursor requirement remains:
  - no visible cursor,
  - touch-first surface,
  - keep `cursor:none` in the WebView/noVNC CSS and `x11vnc -nocursor`.

Implementation targets:

1. In `OringutanActivity.java`, update `createChatDrawer()`:
   - remove the full-width `New chat` button,
   - add a compact search input at the top,
   - filter drawer rows by chat title/visible metadata,
   - keep new-chat as a small secondary action.
2. In `createChatDrawerRow()`, remove `session.terminalName()` / `ominal-*` from visible row text.
   - Suggested metadata: message count, last activity, workspace label like `Workspace ready`.
3. Keep the installed display overlay behavior from the current build:
   - only floating `X`,
   - no `Display` title pill.
4. Main remaining blocker after drawer cleanup:
   - display mode still opens to a black WebView/noVNC surface,
   - backend can run, but app rendering/reconnect is not deterministic.

Testing requirement:

- Do a full installed-device test pass after changes, not just `assembleDebug`.
- Check launch, main chat, composer, drawer search, chat switching, new chat, prompt execution, attachments, terminal, display, close/back behavior, persistence, and logcat.
- Verify sizing on the actual phone screen: no clipped text, oversized chrome, awkward landscape display scaling, or visible pointer.
- Display-specific verification must prove:
  - only the floating `X` is visible,
  - no cursor appears,
  - no `xmessage`/Ubuntu-logo fallback appears,
  - stale display processes are killed/restarted deterministically,
  - noVNC does not remain a black surface.
- Capture screenshots for main chat, drawer search, terminal, and display, plus logs for any remaining failure.

## Checkpoint - 2026-06-30 20:58 IST

Verified by direct work after the background runner stalled:

- Fresh Android build succeeded:
  - `.\gradlew.bat assembleDebug`
  - `BUILD SUCCESSFUL in 28s`
- Installed successfully to device `O7ON59OZEY7LOVQG`.
- Launched `com.termux/.app.OringutanActivity` successfully.
- Updated display helper was pushed to:
  - `/data/data/com.termux/files/usr/bin/ominal-display-start`
- Verified installed screenshots:
  - `build-logs\ominal-check-installed-main.png`
  - `build-logs\ominal-check-installed-drawer-open.png`
  - `build-logs\ominal-check-installed-display-final.png`
- Chat UI is visibly changed from the stale old build:
  - visible title/subtitle,
  - standard dark chatbot layout,
  - left sliding chat drawer is working,
  - bottom metallic tray is gone.
- `MetalSurfaceDrawable` is bypassed in source by returning a normal rounded `GradientDrawable`.
- Display helper version is now `2026-06-30-product-display-2`.
- Display backend process table was corrected once manually:
  - `Xvfb :20 -screen 0 540x1076x24`
  - `x11vnc -display :20 ... -nocursor`
  - `websockify --web /usr/share/novnc 127.0.0.1:6080 127.0.0.1:5900`

Remaining problem:

- Display mode still visually renders as a black surface inside the app, even though x11vnc receives a noVNC client connection.
- This is now a WebView/noVNC reconnect/rendering/display-content issue, not the old APK issue.
- Scanner correction from user: keep the close `X`. Remove only the surrounding permanent `Display` pill/header. Display mode should be a full display surface with a small floating `X`, not a full-width `Display ... X` bar.
- Additional scanner notes from enlarged crops:
  - The large `New chat` button in the drawer appears to be marked as not needed.
  - The random `ominal-...` chat ids in the drawer are circled/marked; likely replace with useful names/usage context rather than exposing internal ids.
  - The drawer should be more minimal and based on actual chat names/usage, not chunky cards plus technical ids.
- Installed checkpoint after scanner correction:
  - Built and installed current APK to `O7ON59OZEY7LOVQG`.
  - Verified screenshot: `build-logs\ominal-current-installed-after-scanner-display.png`.
  - Display overlay now shows only the small floating `X`; the full `Display ... X` pill/header is removed.
- The next pass should focus on making display startup deterministic:
  - kill/restart stale display processes from the helper reliably,
  - reload noVNC after server startup instead of showing a dead black view,
  - provide a visible touch-first launcher or clean shell surface,
  - keep `x11vnc -nocursor`,
  - remove all xmessage/Ubuntu-logo fallback paths.

Previous warning: the installed APK used to be the old/rejected UI. As of the checkpoint above, a newer APK has been built and installed. Do not reinstall older stale APKs.

The next worker must make source changes directly, then run a fresh build and install only the newly generated APK. Verify the APK timestamp changed after the source edit before installing.

User intent:

- Treat this like a product-grade build, not a quick visual skin.
- Take as much time as needed. Do not rush toward another stale or half-visible APK.
- Standard serious chatbot interface: clean dark mainstream chat UI, normal rounded input bar, readable title/subtitle, polished but boring and defensible.
- No literal metallic UI, no shiny material gimmicks, no weird bottom tray.
- Chat history should be a standard sliding left drawer/sheet/list.
- Per-chat workspace/terminal/display remains the architectural model.
- Display is a product feature, not a debug side effect.
- End goal: Codex should use the computer/display smoothly, and the user should be prompted only when human input is needed.
- The experience should feel like a continuous back-and-forth, not a jagged mode switch.
- Example use case, not the only use case: Codex is signing into a form, reaches a point where user credentials/confirmation are needed, opens the display automatically, the user touches/enters the needed info, and Codex continues.
- Build toward full computer-use, not only login forms.
- Premium quality here means smooth, fast when possible, predictable, and recoverable: no surprise dead screens, no hidden waiting, and no rough transitions.

Display requirements:

- The Linux/X display must be phone-geometry correct.
- Current display is rejected because geometry is messed up and the cursor/window scale feels huge/bloated.
- There should not be a cursor in the first place. The display must be touch-first, because the user and Codex both interact with it as a phone surface.
- Hide/remove the pointer from the visible display. Do not design around desktop mouse interaction.
- Fix the display as if this was paid product work:
  - choose a stable portrait geometry that matches the Android WebView aspect,
  - do not stretch landscape content into portrait,
  - do not show a cursor,
  - do not show huge desktop chrome,
  - do not show xmessage as the default experience,
  - use a quiet empty/launcher desktop with normal-scale terminal/app windows,
  - make noVNC canvas sizing preserve the intended phone frame cleanly.
- The display should feel like a clean phone-adjacent Linux surface available to both agent and user.

Immediate required implementation:

1. Edit `app/src/main/java/com/termux/app/OringutanActivity.java`.
2. Remove or bypass `MetalSurfaceDrawable` and `DiffusedScrimView` for normal UI surfaces.
3. Replace `showChatPicker()` bottom horizontal tray with a left sliding chat drawer.
4. Restore visible header title/subtitle. It currently hides the title by using tiny text / empty text.
5. Edit display code/CSS and `tools/ominal-display-start.sh` so the display is portrait, correctly scaled, touch-first, and has no visible cursor.
6. Build with `.\gradlew.bat assembleDebug`.
7. Confirm APK LastWriteTime is newer than this handoff.
8. Install to `O7ON59OZEY7LOVQG` if connected.
9. Launch app and capture screenshots.
10. Update this handoff with exact result.

## Current User Direction

Date: 2026-06-29

The last UI pass went in the wrong direction.

Do **not** continue the metallic/physical-object look. The user did not mean literal metal. They meant the UI should feel native and high-quality, but still like a normal modern chatbot app.

Correct direction:

- Use a standard polished chatbot interface as the base.
- Keep the screen black/dark and clean.
- Use conventional chat layout patterns from ChatGPT/Claude/Gemini style apps.
- Do not over-style controls with heavy shiny/metal gradients.
- Do not make chat history a weird bottom metallic tray.
- Chat history should be a normal chatbot UX: drawer/sidebar/sheet/list of conversations that slides in cleanly and can be tapped.
- Display should be clean and phone-shaped like the earlier working display, not a huge desktop/cursor/window environment.
- Display must still be an actual Linux/X display, but curated for phone geometry:
  - no giant Ubuntu logo,
  - no huge cursor,
  - no ugly `xmessage` fallback as the main look,
  - no oversized desktop chrome,
  - no shell-only default if a clean lightweight desktop/launcher is available.

## Current Files To Fix

- `app/src/main/java/com/termux/app/OringutanActivity.java`
  - Contains the bad metal/native surface pass.
  - `MetalSurfaceDrawable` and `DiffusedScrimView` were added.
  - `showChatPicker()` was changed to a bottom tray. User rejected this interaction model.
  - Next pass should simplify back to clean chatbot UI:
    - remove/reduce metallic `MetalSurfaceDrawable` usage,
    - use subtle dark rounded native drawables,
    - replace bottom history tray with a standard sliding drawer/sheet conversation list.

- `tools/ominal-display-start.sh`
  - Version currently around `2026-06-29-phone-desktop-3`.
  - It starts fluxbox and `xmessage` fallback.
  - User rejected the current display look as too huge/awkward.
  - Next pass should restore a clean phone display:
    - keep Xvfb/noVNC phone geometry,
    - avoid giant desktop splash/logo,
    - avoid `xmessage` as the default,
    - provide a small clean launcher or empty desktop with apps available.

## Build / Install State

Workspace:

```powershell
cd C:\Users\saura\skynet\termux-app
```

Device:

```text
O7ON59OZEY7LOVQG
package: com.termux
activity: com.termux/.app.OringutanActivity
```

Last Android build succeeded:

```text
.\gradlew.bat assembleDebug
BUILD SUCCESSFUL
```

Installed APK:

```text
app\build\outputs\apk\debug\termux-app_apt-android-7-debug_universal.apk
```

Current APK sizes:

```text
universal debug APK: 120.26 MB
arm64-v8a APK:       37.72 MB
```

On-device runtime footprint:

```text
/data/data/com.termux/files: about 3.7 GB
proot-distro subtree: about 2.8 GB
```

Optional richer GUI package install failed due device DNS errors resolving `ports.ubuntu.com`.

## Verified Screenshots

Use these only as evidence of the rejected/current state:

```text
build-logs\ominal-native-surface-chat.png
build-logs\ominal-native-surface-history.png
build-logs\ominal-phone-desktop-display2.png
```

Print files:

```text
build-logs\ominal-whole-ui-experience-print-lite.jpg
build-logs\ominal-whole-ui-experience-fullpage.jpg
```

The fullpage print job was submitted after a paper jam was cleared, but printing reliability depends on Epson state.

## Resume Commands

Check device:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb devices -l
```

Build and install:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
.\gradlew.bat assembleDebug
$apk = (Get-ChildItem -Recurse -File -Path app\build\outputs\apk\debug -Filter '*universal*.apk' | Sort-Object LastWriteTime -Descending | Select-Object -First 1).FullName
& $adb -s O7ON59OZEY7LOVQG install -r $apk
& $adb -s O7ON59OZEY7LOVQG shell am force-stop com.termux
& $adb -s O7ON59OZEY7LOVQG shell am start -W -n com.termux/.app.OringutanActivity -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
```

Push/restart display helper:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb -s O7ON59OZEY7LOVQG push tools\ominal-display-start.sh /data/local/tmp/ominal-display-start
& $adb -s O7ON59OZEY7LOVQG shell run-as com.termux cp /data/local/tmp/ominal-display-start /data/data/com.termux/files/usr/bin/ominal-display-start
& $adb -s O7ON59OZEY7LOVQG shell run-as com.termux chmod 700 /data/data/com.termux/files/usr/bin/ominal-display-start
& $adb -s O7ON59OZEY7LOVQG shell run-as com.termux /data/data/com.termux/files/usr/bin/env PREFIX=/data/data/com.termux/files/usr HOME=/data/data/com.termux/files/home PATH=/data/data/com.termux/files/usr/bin:/system/bin OMINAL_DISPLAY=:20 OMINAL_DISPLAY_GEOMETRY=540x1076x24 /data/data/com.termux/files/usr/bin/ominal-display-start
```

## Next Implementation Task

1. Rework `OringutanActivity.java` back to a standard clean chatbot UI.
2. Remove or heavily soften `MetalSurfaceDrawable` usage.
3. Replace the bottom history tray with a normal sliding conversation drawer/sheet.
4. Restore the display helper to a clean, minimal, phone-sized Linux display:
   - no xmessage default,
   - no giant logo,
   - no huge cursor/window chrome,
   - display remains available to the agent and user.
5. Build, install, and verify with screenshots before printing again.
