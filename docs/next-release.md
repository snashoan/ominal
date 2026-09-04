# Next Release

## v193 release candidate

The v193 target tightens the engineering workflow without turning the chat into
an IDE:

- Codex app-server processes start in the selected chat's exact workspace and
  resume the saved thread when handed to the interactive terminal.
- Monopot Activity converts verified command actions and paths into readable
  engineering updates while retaining raw command output for inspection.
- Reasoning-only events stay out of the execution log.
- Markdown code blocks use a dedicated monospace surface with syntax highlighting,
  and existing math and Unicode normalization remain enabled.
- Registered harnesses may declare a validated interactive terminal command,
  without adding provider-specific Android code.
- Codex model discovery falls back to Codex's own model cache when live discovery
  is unavailable.
- Healthy runtimes skip installation work during warm restarts.
- Adaptive and legacy launcher resources share the same high-contrast GIR mark.

Release gates completed on 2026-09-04: 104 unit tests, release lint with zero
errors, shell syntax checks, runtime contract checks, Play permission
validation, target API 36 validation, signed APK and bundled AAB generation,
bundletool validation, signature verification, and English release notes.

Publication artifact: `release/gir-0.121.24-v193-bundled.aab`

SHA-256: `e38161c1758c9f8824df36f3c10515acd0e7ad3220ae3a621053f3f3c8c0d674`

The local upload-key APK cannot replace an installation signed by Google Play.
Device verification for this release must use the Play-distributed artifact or
a separately installed development application ID; existing app data must not
be removed to bypass that signing boundary.

## v192 release candidate

The v192 target consolidates the developer-alpha work since v150 into one
verified release candidate:

- History, Chat, and Computer form a spatial three-surface flow. Dragging tracks
  the finger directly and settles with distance-aware 120-220 ms easing.
- Each conversation keeps its own persistent harness process, workspace, event
  log, model choice, queued turns, media, and execution receipts.
- Codex and Antigravity remain first-class native harness integrations.
- Validated runtime manifests can add provider-neutral Monopot stdio adapters
  without changing Android source or replacing harness-native behavior.
- Harness packages can register themselves live through `gir-harness`, including
  bounded local artwork, without an app restart or a hosted registry.
- Every runtime identifies the public app as GIR while existing Ominal package
  names, private paths, and manifests remain compatible.
- Harnesses can query protected snapshots of prior non-incognito conversations
  through `gir-chats`; source conversations remain read-only and the active chat
  is never duplicated into its own context.
- A canonical provider-neutral profile stays on device and is projected into
  every selected Linux runtime as shared context.
- Chat renders Markdown, LaTeX, repaired Unicode, and syntax-highlighted fenced
  code while keeping raw event and execution records verifiable.
- Agent work survives activity backgrounding and surfaces completion or input
  requests through notifications.
- Silent harness waits emit an honest elapsed-time heartbeat, and GIR-owned text
  entry and confirmation flows share one native interaction sheet.
- The runtime installer detects the device architecture and refuses an
  incompatible bundled runtime instead of attempting a corrupt setup.

Verified so far on Raphael (`Redmi K20 Pro`): unit tests, debug assembly,
installation, warm relaunch, History-to-Chat navigation, Chat-to-Computer
navigation, Settings/Profile visibility, and the private profile projection.

Release gates completed on 2026-09-03: unit tests, debug and release lint,
runtime contract checks, Play permission validation, target API 36 validation,
signed bundled AAB generation, bundletool validation, signature verification,
and English release notes.

Publication artifact: `release/gir-0.121.23-v192-bundled.aab`

SHA-256: `c22f94a8989599eb60fd858656a33fa9d6d57e06fe7dcb93c0fd04af97caf7a0`

Device delivery and Play Console submission are operational steps outside the
release build gate.

## Archived v150-dev display checkpoint

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
