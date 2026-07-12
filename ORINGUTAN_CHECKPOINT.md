# Ominal / Oringutan Checkpoint

Date: 2026-06-24

## Current Winning Runtime

- Device: `O7ON59OZEY7LOVQG`
- Android package: `com.termux`
- Termux app uid after reinstall: `u0_a662`
- Codename still visible in app: `Oringutan`
- Working runtime path: Termux wrapper -> `proot-distro` Ubuntu -> npm-installed Codex.

Termux command now works:

```sh
/data/user/0/com.termux/files/usr/bin/codex --version
# codex-cli 0.142.0
```

The old native Android Codex binary was kept as backup:

```sh
/data/user/0/com.termux/files/usr/bin/codex-native-android
```

The active wrapper is:

```sh
/data/user/0/com.termux/files/usr/bin/codex
```

Source copy:

```text
C:\Users\saura\skynet\termux-app\tools\ominal-proot-codex-wrapper.sh
```

## Auth / Config

Local Codex auth/config from `C:\Users\saura\.codex` was copied to:

```sh
/data/user/0/com.termux/files/home/.codex/auth.json
/data/user/0/com.termux/files/home/.codex/config.toml
/root/.codex/auth.json          # inside proot Ubuntu
/root/.codex/config.toml        # inside proot Ubuntu
```

Temporary ADB staging files were removed after copy.

## Verified Direct CLI

This command succeeded through Termux and proot:

```sh
codex exec --skip-git-repo-check -- "reply with exactly: ominal-ok" </dev/null
```

Observed output:

```text
ominal-ok
```

## Verified UI Path

Rebuilt and installed APK:

```text
C:\Users\saura\skynet\termux-app\app\build\outputs\apk\debug\termux-app_apt-android-7-debug_arm64-v8a.apk
```

The app UI invoked Codex successfully. Visible completed turn:

```text
user: hiii
codex: Hi. What would you like to work on?
```

The UI bubble showed Codex running as:

```text
OpenAI Codex v0.142.0
workdir: /root
provider: openai
sandbox: danger-full-access
```

At checkpoint time, a second UI prompt was active:

```text
user: make me game
status: Running through Oringutan agent adapter...
```

Process table showed:

```text
com.termux -> proot -> node -> codex
```

## Source Changes In Play

- `app/src/main/java/com/termux/app/OringutanActivity.java`
  - Chat UI frontend.
  - Bubbles and input are selectable/copyable.
  - Adapter calls `codex exec --skip-git-repo-check -- "$prompt" </dev/null`.
- `app/src/main/AndroidManifest.xml`
  - Launcher points to `OringutanActivity`.
- `app/src/main/res/values/strings.xml`
  - Oringutan UI strings.
- `app/build.gradle`
  - Debug versionCode bumped for reinstall.
- `tools/ominal-proot-codex-bootstrap.sh`
  - Installs proot Ubuntu, Node 22, npm Codex.
- `tools/ominal-proot-codex-wrapper.sh`
  - Termux `codex` wrapper into proot Ubuntu.

## Native Android Binary Track

Native Android Codex binary previously ran `--version`, but `codex exec` failed on Android file locking. A source patch was added:

```text
C:\Users\saura\skynet\codex\codex-rs\core\src\installation_id.rs
```

The local native rebuild is not the current winning path because vendored OpenSSL cross-build from Windows needs more Perl/build-system cleanup. Keep this as later optimization only.

## Resume Commands

Check device:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb devices -l
```

Check Codex wrapper:

```powershell
& $adb -s O7ON59OZEY7LOVQG shell "run-as com.termux sh -lc 'export PREFIX=/data/user/0/com.termux/files/usr; export HOME=/data/user/0/com.termux/files/home; export PATH=$PREFIX/bin:/system/bin; codex --version'"
```

Launch UI:

```powershell
& $adb -s O7ON59OZEY7LOVQG shell am start -W -n com.termux/.app.OringutanActivity
```
