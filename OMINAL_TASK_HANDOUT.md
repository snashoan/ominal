# Ominal Task Handout

## Current Checkpoint - 2026-07-06 18:20 IST

The source-built bootstrap integration is complete and validated.

Verified state:

- `build-logs\ominal-bootstrap-status.txt`: `complete`
- WSL build log ended with `DONE 2026-07-06T12:40:15+00:00`.
- Generated bootstraps were copied into `app\src\main\cpp`.
- `app\src\main\cpp\bootstrap-ominal.sha256` was written and matches the copied zips.
- Independent zip scan found no `/data/data/com.termux` strings in the copied bootstrap archives.
- Gradle validation passed:

```powershell
.\gradlew.bat :app:validateOminalBootstraps
```

- Debug build passed:

```powershell
.\gradlew.bat assembleDebug
```

Current APKs:

```text
app\build\outputs\apk\debug\ominal-app_apt-android-7-debug_arm64-v8a.apk
app\build\outputs\apk\debug\ominal-app_apt-android-7-debug_universal.apk
```

Use the `arm64-v8a` APK for the connected Redmi K20 Pro. The universal APK is about `546 MB` and previously timed out during install.

ADB status:

- Device seen: `10.172.121.55:42801`, `Redmi_K20_Pro`.
- `arm64-v8a` install failed at the device with:

```text
INSTALL_FAILED_USER_RESTRICTED: Install canceled by user
```

Retry after allowing installs on the device:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$apk = "C:\Users\saura\skynet\termux-app\app\build\outputs\apk\debug\ominal-app_apt-android-7-debug_arm64-v8a.apk"
& $adb -s 10.172.121.55:42801 install -r $apk
```

Remaining release debt:

- Do not push to the current upstream Termux remote.
- Configure an Ominal-owned remote before any push.
- Store bootstrap zips through Git LFS or release assets.
- Keep upstream license/attribution.
- Termux ecosystem names remain in packages, modules, classes, resources, art/Fastlane metadata, and historical docs until a separate deep rebrand/package-repo fork pass.

## Current Checkpoint - 2026-07-04 20:16 IST

The source-built bootstrap pass is active again after disk recovery.

Live state:

- Windows host wrapper PID: `36480`
- Current status: `running`
- WSL source tree: `/root/ominal/termux-packages`
- WSL log: `/root/ominal/bootstrap-build.log`
- Completed source-built zips:
  - `bootstrap-aarch64.zip`, about `135M`
  - `bootstrap-arm.zip`, about `127M`
- Current architecture:
  - `i686`
- Recent `i686` progress:
  - package output count observed at `430`
  - active package observed around GNU `coreutils`
  - build/install paths in log use `/data/data/com.ominal/files/usr`
- Repo bootstraps are not replaced yet. The runner copies all four zips into `app\src\main\cpp` only after `aarch64`, `arm`, `i686`, and `x86_64` all finish.

Disk/build recovery:

- C: disk exhaustion caused the earlier bootstrap zip failure.
- Generated build directories, temp files, and rebuildable caches were cleared.
- C: free space recovered to roughly `19G` while `i686` was building.
- `tools\run-ominal-bootstrap-build-wsl.sh` now checks checkout-drive free space before build/copy steps.

Repo-facing metadata:

- `README.md` and `docs\en\index.md` now describe Ominal.
- License files were preserved. This fork still inherits upstream GPLv3-only app licensing and documented subcomponent exceptions.

Git/push status:

- Current remote is still upstream Termux only:

```text
origin  https://github.com/termux/termux-app.git
```

- Do not push to that remote.
- No Ominal remote is configured, so push remains blocked after checks until the correct remote/branch is added.
- The generated source-built bootstrap zips exceed normal GitHub blob limits. Use Git LFS or release assets unless the final remote explicitly supports large files.

## Current Checkpoint - 2026-07-04 17:55 IST

The corrected source-built bootstrap build is still active.

Live state:

- Windows host wrapper PID: `34404`
- Current status: `running`
- WSL source tree: `/root/ominal/termux-packages`
- WSL log: `/root/ominal/bootstrap-build.log`
- Completed source-built zip:
  - `bootstrap-aarch64.zip`, about `134M`
- Current architecture:
  - `arm`
- Recent `arm` progress:
  - completed `libicu`, `ncurses`, `readline`, `gdbm`, `libbz2`, `libffi`, `liblzma`, `tcl`, `libsqlite`, `brotli`
  - active package observed around `libpng`
- Repo bootstraps are not replaced yet. The runner copies all four zips into `app\src\main\cpp` only after `aarch64`, `arm`, `i686`, and `x86_64` all finish.

Git/push status:

- Current remote is still upstream Termux only:

```text
origin  https://github.com/termux/termux-app.git
```

- Do not push to that remote.
- No Ominal remote is configured, so push remains blocked after checks until the correct remote/branch is added.
- The generated source-built bootstrap zip is larger than GitHub's normal single-file limit. Treat bootstraps as release assets or Git LFS objects unless the final remote explicitly supports large normal git blobs.

Device/emulator status:

- `adb devices` currently shows no attached device.
- Emulator/system images exist locally, but there is no AVD listed.
- C: is tight on free space, so prefer a USB device if it appears; create a minimal local AVD only if needed.

## Current Checkpoint - 2026-07-04 15:10 IST

The corrected source-built bootstrap run is active.

Live state:

- Windows host wrapper PID: `5156`
- Status file: `build-logs\ominal-bootstrap-status.txt`
- Current status: `running`
- WSL source tree: `/root/ominal/termux-packages`
- WSL log: `/root/ominal/bootstrap-build.log`

Fixes applied by `tools\run-ominal-bootstrap-build-wsl.sh` before launching Docker:

- `scripts/properties.sh`
  - `TERMUX__NAME="Ominal"`
  - `TERMUX_APP__PACKAGE_NAME="com.ominal"`
- `scripts/build-bootstraps.sh`
  - force cleanup now uses the defined `TERMUX_BUILT_PACKAGES_DIRECTORY`, not undefined `TERMUX_BUILT_PACKAGES_DIRECTORY_FOR_ARCH`;
  - cleanup now has an explicit guard against empty `/`-style targets;
  - second-stage bootstrap generation now calls `add_termux_bootstrap_second_stage_files "$TERMUX_ARCH"` instead of the undefined `"$package_arch"`.
- The runner also `chown`s the WSL `termux-packages` clone to UID/GID `1001:1001` so the container `builder` user can write `output/`.

Failure history from this pass:

- First Docker run failed after image pull because undefined `TERMUX_BUILT_PACKAGES_DIRECTORY_FOR_ARCH` expanded the force-clean target incorrectly inside the container.
- Second run reached package build but failed because the root-owned WSL clone blocked the container `builder` user from creating `output/`.
- Third run was intentionally stopped before producing zips after the second-stage architecture bug was found.
- Current run was restarted with all three source/tooling fixes above.

Last healthy evidence before WSL became slow to open new sessions:

- `build-bootstraps.sh` was running under Docker.
- `aarch64` package builds were active.
- Build/install paths in the log used `/data/data/com.ominal/files/usr`.
- No `bootstrap-*.zip` had been produced yet by the corrected run.

Latest observed progress after WSL status recovered:

- Current status remained `running`.
- `aarch64` package chain had built through:
  - `pcre2`
  - `libandroid-selinux`
  - `libandroid-support`
  - `libc++`
  - `libgmp`
  - `libiconv`
  - `ca-certificates`
  - `zlib`
  - `openssl`
  - `coreutils`
  - `doxygen`
  - `libandroid-glob`
- Current active package was `libicu`.
- No corrected `bootstrap-*.zip` had been produced yet.

Later source-build progress:

- Fossies download failures were fixed in the WSL runner by patching package recipes before build:
  - `libbz2` now downloads from Sourceware with SHA-256 `ab5a03176ee106d3f0fa90e381da478ddae405918153cca248e682cd0c4a2269`.
  - `libdb` now downloads from Oracle using the existing recipe SHA-256.
  - `psmisc` now downloads from SourceForge using the existing recipe SHA-256.
- The runner now resumes without `-f` by default so built debs and the Docker builder container are preserved between retries.
- `bootstrap-aarch64.zip` was created successfully in `/root/ominal/termux-packages` at about `134M`.
- The build then continued into the next architecture, `arm`.
- The repo bootstraps have not been replaced yet; the runner copies archives into `app\src\main\cpp` only after all four architecture zips are built.

Next required steps when the source build finishes:

```powershell
cd C:\Users\saura\skynet\termux-app
.\gradlew.bat :app:validateOminalBootstraps
.\gradlew.bat assembleDebug
```

Do not push until those pass and an Ominal-owned remote is configured. Current visible remote is still upstream Termux only.

## Current Checkpoint - 2026-07-04 14:45 IST

Primary objective is now the proper Ominal source-built bootstrap and release-readiness pass. Do not do binary patchwork on bootstrap archives.

Live background work:

- WSL/Docker bootstrap build is running.
- Bootstrap runner:
  - `C:\Users\saura\skynet\termux-app\tools\run-ominal-bootstrap-build-wsl.sh`
  - source tree: `/root/ominal/termux-packages`
  - log: `/root/ominal/bootstrap-build.log`
  - status file: `build-logs\ominal-bootstrap-status.txt`
- Current log evidence:
  - `TERMUX_APP_PACKAGE: "com.ominal"`
  - `TERMUX_PREFIX: "/data/data/com.ominal/files/usr"`
  - `TERMUX_ARCHITECTURES: "aarch64 arm i686 x86_64"`
- Windows-side restart command:

```powershell
cd C:\Users\saura\skynet\termux-app
.\tools\start-ominal-bootstrap-build-windows.ps1
```

- WSL status command:

```powershell
wsl -u root bash -lc 'cat /mnt/c/Users/saura/skynet/termux-app/build-logs/ominal-bootstrap-status.txt; tail -120 /root/ominal/bootstrap-build.log; ps -ef | grep -E "run-ominal-bootstrap|build-bootstraps|run-docker|docker" | grep -v grep || true'
```

Background Codex handoff:

- Active runner PID is recorded at `build-logs\codex-bg\runner.pid`.
- Current queue cursor has been advanced past stale UI-only prompts.
- Fresh long-running handoff job was queued on 2026-07-04 with these constraints:
  - source-built `com.ominal` bootstraps only,
  - Gradle validation before build,
  - device check via ADB, emulator fallback only if no USB device is available,
  - no Codex source/auth/local config/build logs/external clones in git,
  - push only to a valid Ominal-owned remote.
- Reattach/status command:

```powershell
cd C:\Users\saura\skynet\termux-app
.\contun.ps1
```

- Tail without starting a new worker:

```powershell
.\tools\ominal-bg-codex-tail.ps1 -Workspace . -Lines 120
```

Git and push state:

- Current visible remote is only upstream Termux:

```text
origin  https://github.com/termux/termux-app.git
```

- Do not push Ominal fork work to that remote.
- Pushing is blocked until a valid Ominal remote/branch is configured with existing credentials.

Validation gates before any push:

```powershell
git status --short
git remote -v
git diff --check
.\gradlew.bat :app:validateOminalBootstraps
.\gradlew.bat assembleDebug
```

Expected current validation state:

- `:app:validateOminalBootstraps` should fail until the source-built bootstrap archives finish and `app\src\main\cpp\bootstrap-ominal.sha256` is written.
- Old upstream bootstrap zips must not be committed as the final Ominal bootstraps.

Device/emulator test gate:

- First try attached/authorized ADB device.
- If no USB device is connected, use an Android emulator validation path instead.
- Current ADB check on 2026-07-04 showed no attached devices.
- Emulator tooling is present under `C:\Users\saura\AppData\Local\Android\Sdk\emulator\emulator.exe`.
- Available local system images include Android 36 and 36.1 Google Play x86_64 images.
- Install and smoke test the built APK before push:
  - launch,
  - main chat,
  - drawer/search,
  - per-chat terminal,
  - display entry/exit,
  - logcat crash/display errors.

Licensing and attribution:

- Keep GPLv3 and upstream attribution intact.
- Rebrand/package identity can be changed, but license notices and source availability obligations remain.
- Do not claim all Termux references are removed while legal/upstream/internal class names still remain.

## Current Checkpoint - 2026-07-02 14:57 IST

- Direct source pass completed in `app/src/main/java/com/termux/app/OringutanActivity.java`.
- Drawer now uses a top search field instead of a dominant full-width `New chat` button.
- New chat remains available as a compact `+` action.
- Drawer rows no longer expose `ominal-*` terminal/session ids.
- Drawer rows now filter by chat title and latest visible message context.
- Header subtitle no longer exposes the internal terminal name.
- Terminal pane label changed to user-facing `Per-chat terminal`; internal shell names are still used only for Termux session routing.
- Display WebView load path now clears stale loads, reloads with a cache-busting timestamp, and retries through `ensureDisplayServerStarted(true)`.
- Display helper version bumped to `2026-07-02-drawer-display-restart-1`.
- `continue-ominal.ps1` now starts the display helper with `540x1076x24` to match the app.

Verification so far:

```powershell
.\gradlew.bat assembleDebug
```

Result:

- `BUILD SUCCESSFUL`
- Fresh arm64 APK:
  - `app\build\outputs\apk\debug\termux-app_apt-android-7-debug_arm64-v8a.apk`
  - LastWriteTime: `2026-07-02 14:54:53 IST`
  - Size: `39,548,243` bytes

Install state:

- A detached watcher is running through `continue-ominal.ps1`.
- It rebuilt successfully and is waiting for ADB device `O7ON59OZEY7LOVQG`.
- Logs:
  - `build-logs\ominal-install-watch.log`
  - `build-logs\ominal-install-watch.err.log`
- At checkpoint time, `adb devices` showed no attached device, so installed-device screenshots are still pending.

## Current Installed State

- Current APK was built and installed to `O7ON59OZEY7LOVQG`.
- Display overlay correction is installed:
  - the full `Display ... X` pill/header was removed,
  - only a small floating `X` remains.
- Pointer hiding is wired:
  - WebView/noVNC CSS uses `cursor:none`,
  - display backend uses `x11vnc -nocursor`.

## Next UI Task

Replace the large drawer `New chat` control with search.

Requirements:

- The chat drawer top control should be a search field, not a dominant `New chat` button.
- Hide internal `ominal-*` ids from drawer rows.
- Drawer rows should show meaningful chat title plus useful context, such as message count or recent activity.
- Keep new-chat available as a small secondary icon/action, not the main drawer feature.
- Keep the drawer visually quiet and mainstream-chat-app-like.

Primary file:

```text
app/src/main/java/com/termux/app/OringutanActivity.java
```

Likely methods:

```text
createChatDrawer()
renderChatDrawer()
createChatDrawerRow()
```

## Display Blocker

Do not regress the overlay. Keep only the floating `X`.

Remaining display bug:

- Display mode still often shows a black WebView/noVNC surface.
- Backend processes can run with `Xvfb :20`, `x11vnc -nocursor`, and `websockify`.
- Next display pass should make startup/reconnect deterministic and show a visible touch-first surface.

## Verification

After source edits:

```powershell
.\gradlew.bat assembleDebug
```

Then install only the freshly built APK and capture screenshots for:

- main chat,
- drawer with search,
- display mode with only floating `X`.

## Thorough Test Pass Required

Do not treat a successful build as enough. After installing, test the app as a complete phone product.

Required checks:

- Launch/cold start: app opens without crash and lands on the expected chat surface.
- Main chat: header, messages, composer, attach button, send button, and keyboard behavior fit the phone screen.
- Drawer: search is the top control, filtering works, internal `ominal-*` ids are not visible, rows are readable, and new-chat is still available as a small secondary action.
- Chat persistence: switching chats preserves history and selected chat state.
- New chat: creates a usable chat workspace without visual clutter.
- Agent run: sending a prompt creates user/assistant bubbles and handles Codex/network errors without breaking layout.
- Attachments: file picker opens and selected files are copied into that chat workspace.
- Terminal: opens the correct per-chat workspace and copy/open actions work.
- Display: opens with only the floating `X`, no permanent `Display` header, no visible cursor, and no old xmessage/Ubuntu-logo fallback.
- Display recovery: stale display processes are restarted cleanly and noVNC does not stay on a dead black surface.
- Touch behavior: display is touch-first; no mouse/pointer UX should be visible.
- Back/close behavior: Android back, drawer close, and display `X` return to sensible states.
- Sizing: verify on the actual phone resolution; no text overlaps, giant controls, clipped buttons, or awkward landscape display scaling.
- Logcat: check for app crashes, WebView errors, display-start failures, and permission errors.

Capture evidence:

- screenshot of main chat,
- screenshot of drawer search,
- screenshot of terminal screen,
- screenshot of display screen,
- relevant logcat/display logs if display is still black.
