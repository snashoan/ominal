# GIR Privacy Policy

Effective: August 27, 2026

GIR (`com.ominal`) is independently developed and published by snashoan. It
provides a conversation interface for user-selected intelligence runtimes, an
app-private Linux workspace, and an in-app computer display.

## Data stored on the device

GIR may store the following inside Android app-private storage:

- conversations, agent status, and execution receipts;
- files imported into a conversation workspace through Android's system document picker;
- Linux runtime files, installed tools, and user-created workspace files;
- interface preferences, including theme configuration;
- authentication material created by a selected runtime during its own sign-in flow.

Android backup is disabled for GIR. Files deliberately exported through
Android's system document interface are stored at the destination selected by
the user and must be removed separately.

## Connected intelligence providers

GIR does not operate an account system or model backend. A runtime selected by
the user communicates directly with its provider. Prompts, conversation
context, command output, and files the user asks the runtime to inspect may be
transmitted to that provider to produce a response. The provider's privacy
policy and account terms govern its processing.

GIR does not collect provider passwords. Authentication occurs through the
provider's browser or command-line sign-in flow. Resulting credentials remain
inside the app-private Linux environment unless the user or provider software
exports them.

## Network and websites

GIR and its Linux environment may access the internet to:

- communicate with the provider selected by the user;
- download tools and package updates requested by the user;
- open websites requested by the user or active runtime.

Websites and external services process data under their own policies.

## Android permissions

The Google Play build uses internet and network-state access, notifications,
wake lock, vibration, and a foreground service for visible, user-started agent
and shell sessions.

It does not request all-files access, package-installation access,
display-over-other-apps access, location, contacts, microphone, camera, or
advertising identifiers. File import and export use Android's system document
interfaces, where the user chooses the file or destination.

## Analytics and advertising

GIR contains no advertising SDK, analytics SDK, or automatic developer
telemetry. It does not automatically upload application logs or conversations
to a server operated by snashoan.

## Retention and deletion

GIR does not create a GIR account. Clearing GIR's app storage or uninstalling
the app removes its app-private data. Files exported to shared storage must be
removed separately.

Removing GIR data does not delete information already processed by an external
provider. Provider-account and provider-data deletion requests must be made
through that provider.

## Security

GIR relies on Android application isolation and app-private storage. Selected
runtimes can execute commands and access workspace files. Users should review
sensitive or irreversible actions and only provide data they are comfortable
sharing with the selected provider.

## Children

GIR is a general-purpose software tool and is not directed to children under 13.

## Changes

This policy may be updated when GIR's data handling changes. The effective date
identifies the current version.

## Contact

Privacy questions can be sent to `hsjc1600@gmail.com` or filed at
https://github.com/snashoan/ominal/issues.
