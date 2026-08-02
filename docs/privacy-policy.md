# Monolith Privacy Policy

Effective: August 2, 2026

Monolith (`com.ominal`) is a local-first Android application maintained by
snashoan. It provides a conversation interface for user-selected coding-agent
harnesses and a Linux workspace that runs on the user's device.

## Data stored on the device

Monolith may store the following inside its Android app-private storage:

- conversations, agent status, and local execution receipts;
- files copied into a conversation workspace through Android's system document picker;
- Linux runtime files, installed command-line tools, and user-created workspace files;
- interface preferences, including the local theme configuration;
- authentication material created by a selected agent harness during that harness's sign-in flow.

Monolith does not enable Android backup for this app-private data. Other apps
cannot normally read it through Android's standard application sandbox.

## Connected agent providers

Monolith does not operate an account system or a model backend. When the user
selects and signs in to an external agent harness, that harness communicates
directly with its provider. Prompts, conversation context, command output, and
files the user asks the agent to inspect may be transmitted to that selected
provider to produce a response. The provider's privacy policy and account terms
govern its processing of that data.

Provider passwords are not collected by Monolith. Authentication takes place
through the provider's own browser or command-line sign-in flow. Resulting local
credentials remain in the app-private Linux environment unless the user or the
provider's software exports them.

## Network and websites

The app and its local Linux environment may connect to the internet to:

- communicate with the agent provider selected by the user;
- download or update user-selected command-line tools and Linux packages;
- open websites requested by the user or agent in the local browser.

Websites and external services can collect data under their own policies.
Monolith does not control those services.

## Permissions

The Google Play build uses network access, network-state access, notifications,
wake lock, vibration, and a foreground service for visible, user-started agent
and shell sessions. It does not request all-files access, package-installation
access, display-over-other-apps access, location, contacts, microphone, camera,
or advertising identifiers.

Files are imported or exported through Android's system document interfaces,
where the user chooses the file or destination.

## Analytics, advertising, and developer servers

Monolith contains no advertising SDK, analytics SDK, or automatic developer
telemetry. It does not automatically upload application logs or conversations
to a server operated by snashoan.

## Retention and deletion

Monolith does not create a Monolith account. Clearing the app's storage or
uninstalling the app removes its app-private data. Files deliberately exported
to shared storage must be removed separately by the user.

Deleting local Monolith data does not delete data already processed by an
external agent provider. Provider-account and provider-data deletion requests
must be made through that provider.

## Security

Monolith relies on Android application isolation and stores its working data in
app-private storage. Agent harnesses can execute commands and access files in
the Monolith workspace, so users should only provide data they are comfortable
sharing with the selected provider and should review sensitive or irreversible
actions before approving them.

## Children

Monolith is a developer tool and is not directed to children under 13.

## Changes

This policy may be updated when the application's data handling changes. The
effective date above identifies the current version.

## Contact

Privacy questions can be sent to `hsjc1600@gmail.com` or filed at
https://github.com/snashoan/ominal/issues.
