# Android updates through direct APK releases

The Android app checks the latest published, non-draft GitHub Release once every 24 hours. A release is recognized when it contains both an APK and an `update.json` asset. The app compares `versionCode`, downloads the APK only after the user chooses to update, verifies its SHA-256, and opens Android's package installer.

Android requires the user to allow TaskPie to install packages from this source and confirm the installation. The updater does not install an APK silently.

## Publishing an update

1. Increase `versionCode` and update `versionName` in `app/build.gradle.kts`. Every published APK must have a greater `versionCode` than the previous one.
2. Build a release APK signed with the same persistent release key used for every earlier release. Supply these Gradle properties through a private `~/.gradle/gradle.properties` file or CI secrets: `TASKPIE_RELEASE_STORE_FILE`, `TASKPIE_RELEASE_STORE_PASSWORD`, `TASKPIE_RELEASE_KEY_ALIAS`, and `TASKPIE_RELEASE_KEY_PASSWORD`. Keep the key and passwords in a secure backup; a different signing key cannot update an existing installation.
3. Name the APK, for example, `TaskPie-1.0.1.apk`.
4. Calculate the APK's SHA-256 checksum.
5. Copy `release/update.template.json` to `update.json`, then fill in the new version code, version name, APK asset filename, SHA-256, and release notes.
6. Create a GitHub Release in `AkkarinRothen/TaskPie` (not a draft or prerelease) and attach both the APK and `update.json` assets. Use a tag such as `v1.0.1`.

The JSON asset format is:

```json
{
  "versionCode": 2,
  "versionName": "1.0.1",
  "apkAsset": "TaskPie-1.0.1.apk",
  "sha256": "lowercase-or-uppercase-64-character-sha256",
  "releaseNotes": "What changed in this release."
}
```

The `apkAsset` value must exactly match the APK filename attached to that same GitHub Release. The updater retrieves the release metadata from GitHub's public Releases API.
