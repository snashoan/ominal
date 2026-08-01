# Product Principles

## Agent-front computer

The primary interface is conversation, not a terminal, launcher, or desktop.
Users state intent and the agent operates the computer without requiring them to
learn the runtime, package manager, filesystem, or window manager.

## Visible when visual

The computer display appears naturally when the task is inherently visual:

- playing games
- watching video
- browsing
- editing visual documents
- reviewing generated work
- completing sign-in, payment, consent, or other user-only steps

The user and agent operate the same persistent display. Control can pass between
them without opening a separate workflow or losing task state.

## Progressive disclosure

Chat is the default surface. Display, files, terminal, logs, models, and runtime
controls remain available, but stay out of the way until the user or task needs
them. Technical implementation names are not part of the normal experience.

## Replaceable intelligence

The intelligence provider and its local harness are separate concepts. OpenAI
is a provider; Codex is a harness. The same boundary applies to other supported
providers and harnesses.

- A harness adapter owns installation, version verification, authentication,
  auth status, launch, resume, cancellation, model discovery, and capabilities.
- The app invokes the harness's official sign-in flow. It does not collect or
  imitate provider passwords.
- A button is selectable only after its adapter and recovery path work.
- Google Play builds deliver executable harness components through Play
  delivery mechanisms. Direct builds may use a signed, checksum-pinned catalog.
- Chat, files, DUI, consent, and session persistence remain stable when the
  selected harness changes.

## No manual required

Setup, dependency management, application installation, workspace selection,
session restoration, and routine recovery should be handled by the agent.
Documentation can exist for inspection and advanced use, but normal operation
must not depend on reading it.

## Explicit boundaries

The product may remove operational jargon, but it must not hide meaningful
security boundaries. Credentials, destructive actions, purchases, permissions,
privacy choices, and irreversible operations require clear user-facing consent.

## Future scope: financial vault

Financial data must be isolated from chat history, model context, logs, the Linux
workspace, DUI applications, and agent-accessible files.

- Present payment and banking authorization as a native Android surface above
  chat or DUI; pause agent interaction until that surface returns a result.
- Route transactions through typed native gateway APIs, verified bank-app
  intents, or provider SDKs rather than computer-use automation.
- Prefer bank OAuth, passkeys, Google Pay, and payment-provider tokens over
  collecting reusable credentials.
- Never persist banking passwords, transaction PINs, card PINs, CVV/CVC values,
  or full magnetic-stripe data.
- Protect vault encryption keys with Android Keystore hardware security and
  require biometric or device-credential authorization for sensitive use.
- Give the agent a transaction-scoped capability, not access to the underlying
  secret.
- Return a structured success, cancellation, or failure result and a receipt
  reference to the agent; never return payment credentials.
- Before authorization, show the exact recipient, account, amount, currency,
  fees, and action being approved.
- Prevent PRoot, root-like experimental modes, terminal sessions, screenshots,
  clipboard history, telemetry, and crash reports from reading vault plaintext.
- Maintain a user-visible audit trail with revocation and emergency lockout.

Financial storage is not release-ready until it has a documented threat model,
regional legal review, payment-industry compliance review, and independent
security assessment.
