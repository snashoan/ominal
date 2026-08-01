# Ominal Runtime Delivery

Ominal produces two release profiles from the same source tree:

- `bundled` is the only Google Play release profile. It carries the prepared ARM64 Linux runtime as an install-time Play Asset Delivery pack, so the runtime is present at first launch.
- `bootstrap` is a direct-test profile. It downloads a checksum-pinned prepared runtime during first setup. It is useful for internal app sharing and controlled sideload testing.

The local `legacy` profile exists only for APK development and device setup
testing. Its Ubuntu base archive lives in `app/src/legacy/assets`; Gradle adds
that source directory only for legacy builds. Release profiles reject legacy
delivery and the AAB workflow verifies that no legacy runtime archive appears
under the base module's assets.

Do not upload both profiles as alternatives in the same Google Play track. They support the same devices and have the same package name, so Play selects the one with the higher version code. A public, user-facing Play release must use the `bundled` profile.

Each Play release needs a new, greater Android version code. Pass it through `ominalAppVersionCode` locally or the `version_code` workflow input. The workflow requires an upload keystore through the four `OMINAL_UPLOAD_*` GitHub secrets; Play App Signing then manages the user-facing signing key.

Prepared-runtime GitHub release tags are immutable. A bootstrap bundle embeds both the runtime URL and its SHA-256, so replacing an existing release asset would make previously shipped bundles fail verification. Use a new runtime tag whenever the prepared runtime changes. The bootstrap downloader requires the release asset to be publicly reachable, or the URL must be replaced with a public CDN URL.

The Android bootstrap is also a checksum-pinned release input. Publish the
source-built `bootstrap-aarch64.zip` under an immutable tag, then provide that
tag and SHA-256 to the AAB workflow. The application repository does not track
the generated bootstrap binary.

## Play-track gates

Treat a signed AAB and an internal-track upload as release candidates, not as
public-release approval.

- Internal testing is the current delivery track. It has no tester-duration
  requirement and is used for rapid device and regression testing.
- Closed testing becomes available after the Play Console app setup is
  complete, including required store listing, policy, app access, content,
  privacy, and data-safety declarations.
- For a personal developer account created after November 13, 2023, production
  access requires a closed test with at least 12 testers continuously opted in
  for 14 days. Internal testers do not satisfy this gate.
- After the closed-test requirement is met, apply for production access from
  the Play Console dashboard and provide the requested testing, feedback, and
  production-readiness evidence.
- Open testing is available only after production access is granted.

Keep the production track untouched until Play Console confirms production
access. Current requirements are documented by Google at:

- https://support.google.com/googleplay/android-developer/answer/14151465
- https://support.google.com/googleplay/android-developer/answer/9845334

## Internal-track deployment

Use the repository Fastlane lane after Play Console has granted a service
account permission to manage internal releases. Keep its JSON key outside the
repository:

```powershell
bundle install
$env:GOOGLE_PLAY_JSON_KEY = "C:\secure\play-service-account.json"
.\tools\publish-internal.ps1 `
  -Aab .\release\monolith-0.119.0-internal-v149.aab `
  -ValidateOnly
.\tools\publish-internal.ps1 `
  -Aab .\release\monolith-0.119.0-internal-v149.aab
```

The lane is fixed to package `com.ominal` and track `internal`. It uploads only
the supplied AAB and the matching version-code changelog under
`fastlane/metadata/android/en-US/changelogs/`; store listing text, graphics,
screenshots, and production tracks are not changed.
