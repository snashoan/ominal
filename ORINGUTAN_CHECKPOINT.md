# Ominal / Oringutan Checkpoint

Date: 2026-07-13 13:45 IST

## Objective

Replace the retired Android-native Codex provider with a verified arm64 PRoot
Ubuntu runtime using the normal npm Codex package, then wire login and display
to that runtime on Raphael.

## Repository State

- Workspace: `C:\Users\saura\skynet\termux-app`
- Branch: `ominal/main`
- Last pushed commit before this change: `08cd9919`
- Final APK: `app/build/outputs/apk/debug/ominal-app_apt-android-7-debug_arm64-v8a.apk`
- Build, shell syntax checks, and `git diff --check` pass.
- The current runtime replacement is ready to commit and push.

Implemented:

- Bundled, checksum-verified arm64 PRoot and normalized Ubuntu Base seed assets.
- Resumable, checksum-verified Node `24.15.0` and Codex `0.144.1` npm downloads.
- Offline npm installation of Codex and its Linux arm64 package inside Ubuntu.
- Atomic rootfs replacement, readiness markers, retry-safe provisioning, and a
  2 GB preflight free-space requirement.
- Persistent app-private Codex home bound to `/root/.codex`, preserving login
  state across rootfs upgrades.
- Minimal display packages only; apt and npm caches are removed after setup.
- Runtime gating for chat, login, and display.
- Arm64-only APK packaging.
- Removal of the old native Codex build/install tools and on-device cleanup of
  legacy `codex.real` and `codex-aarch64` files.
- Display health recovery, phone-shaped X11 geometry, hidden pointer, and longer
  cold-start polling.

## Device Validation

- Device: Raphael / Redmi K20 Pro (`raphaelin`)
- Last verified wireless ADB endpoint: `10.91.157.55:42701`
- Android package: `com.ominal`
- ABI: `arm64-v8a`
- App UID: `u0_a258` / numeric UID `10258`

Validated on the final installed APK:

```text
Runtime marker: ominal-ubuntu-24.04.4-node-24.15.0-codex-0.144.1-display-v2
Node: v24.15.0
npm: 11.12.1
Codex: codex-cli 0.144.1
Codex login state: Not logged in (expected before user sign-in)
dpkg --audit: clean
Guest HOME: /root
Display HTTP health: 200
Auth endpoint connectivity: 403 (reachable; unauthenticated root response)
Runtime rootfs after cache cleanup: approximately 1.2 GB
```

The persistent `/root/.codex` bind was verified by creating a guest-side probe,
observing it at `files/home/.ominal/codex/`, and removing it. Display recovery
was verified by killing websockify, restarting the display, and receiving HTTP
200 after the cold-start window. The X11 shell rendered at phone geometry with
no mouse pointer.

The temporary device-idle whitelist was removed and both display and auth-host
network access were revalidated afterward.

The previously working runtime remains preserved at:

```text
/data/data/com.ominal/files/home/.ominal/runtime.pre-clean-test
```

It can be removed in a later storage-cleanup pass after the user has completed
sign-in and normal interactive use testing.

## Remaining Manual Step

Complete `Sign in with ChatGPT` on the phone. The app launches
`codex login --device-auth` through the PRoot-backed terminal and stores the
result in the persistent app-private Codex home. No device code was captured in
logs during automated validation.
