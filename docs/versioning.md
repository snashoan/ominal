# Versioning

`version.properties` is the only source of Android `versionName` and
`versionCode`. Gradle rejects environment or project-property overrides that
do not match it.

Every version code requires a non-empty Play changelog at
`fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`. The newest
changelog code must match `version.properties`, so reverting only the version
file cannot create an accidental downgrade.

Advance both records together:

```powershell
.\tools\set-version.ps1 `
  -VersionName 0.119.0 `
  -VersionCode 165 `
  -Changelog "Describe the user-visible changes."
```

Before building or publishing:

```powershell
.\gradlew.bat :app:verifyVersioning
```

Release workflows read the canonical version directly. `publish-internal.ps1`
also inspects the AAB manifest with Bundletool and refuses mismatched bundles.
