# Next Release

## v150-dev display checkpoint

Verified on Raphael (`Redmi K20 Pro`, Android) on 2026-07-28:

- Native display service reconnects after a forced process crash.
- XFCE health recovery restarts the desktop and restores a live portrait frame.
- Returning from Home preserves the active display during a warm resume.
- Terminal windows retain a visible title, maximize control, close control, and bottom dock.
- Long-press opens the Android IME and resizes the Linux viewport above it.
- Text input and Enter reach the guest terminal.
- Hiding the IME restores the full `1080x2340` viewport.
- Debug unit tests and `assembleDebug` pass.

Device evidence is under `art/monolith-v150-*.png`. The signed v149 internal
AAB remains frozen; these changes exist only in the v150-dev APK.

Remaining display work:

- Reduce the roughly 16 second cold desktop start and 16-23 second forced-crash recovery.
- Apply rounded-corner and cutout-safe placement across devices.
- Replace the functional XFWM chrome with the final compact dark visual system.
- Repeat the matrix on a sharp-corner device and at least one different resolution/DPI.

## v150-dev harness checkpoint

- First run chooses an intelligence harness, not a Monolith-owned authentication method.
- Codex opens `codex login --device-auth` in its real terminal and enters the Codex TUI
  after successful sign-in.
- Claude Code installs from Anthropic's official native installer on first use and then
  owns its normal onboarding, authentication, settings, and TUI.
- Antigravity installs from Google's official CLI installer on first use and then owns
  Google authentication, model selection, settings, and its TUI.
- Harness IDs are allowlisted before reaching the shell; workspace paths remain confined
  to Monolith's private home.
- Harness configuration is persisted in private per-harness mounts, but Monolith does
  not parse or broker provider credentials.
- Users can continue into the computer without launching a harness.
- Codex remains the only adapter currently wired to the native chat surface. Claude Code
  and Antigravity are usable through their official terminal interfaces until dedicated
  chat transports are implemented.
- The previous custom browser/device-code/API-key first-run form is retired.

## Pending verification

- Chat composer caret disappears while typing.
  - Reproduce on the Play-installed v149 build before changing implementation.
  - Check single-line and multiline input.
  - Check keyboard open, close, and resize transitions.
  - Check switching between chat and DUI.
  - Check Home, Recents, and activity resume.
  - Confirm whether text focus is lost or only the caret becomes invisible.

## Launch readiness

- Choose a permanent public product name and description after the core experience is stable.
- Keep Ominal, Oringutan, and Monolith as internal/testing names rather than public branding.
- Do not create company-first branding; use an individual publisher identity unless that becomes necessary later.
- Decide whether `com.ominal` remains the permanent application ID before publication.
- Review every first-run and recovery surface against the agent-front computer principles.
- Finalize the launcher icon, wordmark, typography, and light/dark visual system.
- Capture polished phone screenshots from a release build with realistic content.
- Produce the Play Store app icon, feature graphic, phone screenshots, and promotional copy.
- Verify that store assets accurately represent chat, terminal, and DUI behavior.
