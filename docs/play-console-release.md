# Google Play internal release

The release workflow builds both delivery profiles but only the bundled-runtime AAB is eligible for automatic Play upload. Publishing is opt-in on every workflow run.

## Repository secrets

- `OMINAL_UPLOAD_KEYSTORE_BASE64`: base64-encoded upload keystore.
- `OMINAL_UPLOAD_STORE_PASSWORD`: upload keystore password.
- `OMINAL_UPLOAD_KEY_ALIAS`: upload key alias.
- `OMINAL_UPLOAD_KEY_PASSWORD`: upload key password.
- `OMINAL_PLAY_SERVICE_ACCOUNT_JSON_BASE64`: base64-encoded Google Play service-account JSON.

Grant the service account access to this app in Play Console with permission to manage testing-track releases. Keep Play App Signing enabled; the repository stores only the upload key through encrypted Actions secrets.

## Publish

Run **Release Android App Bundles** from GitHub Actions. Supply the pinned bootstrap release tag and SHA-256, leave `upload_to_play` off for artifact-only builds, or enable it to upload the bundled AAB to Internal testing. Use `validate_play_upload` for a no-publish credential and bundle check.

For a local upload, set `GOOGLE_PLAY_JSON_KEY` to the service-account JSON and run:

```powershell
.\tools\publish-internal.ps1 -Aab .\release\monolith-0.121.1-v170-bundled.aab -ValidateOnly
```
