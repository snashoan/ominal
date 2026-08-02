# Google Play declarations

These answers describe the `com.ominal` Google Play build. Keep them aligned
with the merged release manifest and the installed build shown in review media.

## Foreground service: special use

### App functionality

Monolith runs user-started local Linux shell and coding-agent sessions. A user
can start a request in chat and leave the app while the requested command or
agent turn continues. During that work Monolith displays an ongoing Android
notification that returns the user to the active session.

The command API can also run a command explicitly requested by an authorized
companion integration. That work uses the same visible notification and local
runtime boundary.

### Impact if the task is deferred

The requested command or agent turn would not begin promptly. The user would
see a stalled conversation and dependent local display or file operations would
not occur.

### Impact if the task is interrupted

The active shell or agent process may be terminated before producing its
response, and unsaved process state may be lost. The user would need to reopen
the conversation and retry or recover the task.

### Review video script

1. Launch Monolith and open a configured conversation.
2. Send a request that takes at least 15 seconds and creates a workspace file.
3. Show the active working state in chat.
4. Press Home.
5. Show Monolith's ongoing agent-session notification.
6. Wait for completion, tap the notification, and show the completed response
   and created file in the same conversation.
7. Open the notification again during a second task and demonstrate its stop or
   return control if exposed by the current build.

Upload an unlisted YouTube video or a publicly viewable cloud-hosted MP4 and
paste that URL into the foreground-service declaration.

## Restricted permissions

The release manifest does not declare `MANAGE_EXTERNAL_STORAGE` or
`REQUEST_INSTALL_PACKAGES`. No declaration form should be submitted for either
permission after version code 170 replaces every active artifact that contains
them.

## Data safety baseline

- Monolith has no developer-operated account or model backend.
- Conversations and workspaces are stored locally.
- User prompts, context, and files may be sent directly to the external agent
  provider selected by the user for app functionality.
- No ads, analytics SDK, advertising ID, location, contacts, microphone, or
  camera collection is present in the release build.
- Data is encrypted in transit when the selected provider and package sources
  use HTTPS. Do not claim end-to-end encryption.
- Users can delete app-private data by clearing storage or uninstalling.
- The privacy policy URL is `https://snashoan.github.io/ominal/privacy/`.

Confirm every answer against the exact Play Console wording before submission.
