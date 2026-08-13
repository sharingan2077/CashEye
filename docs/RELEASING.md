# Publishing a CashEye release

GitHub Releases are created from annotated tags in the `vX.Y.Z` format. The release workflow builds a
production-signed APK and attaches both the APK and its SHA-256 checksum.

## One-time repository setup

1. Generate a production keystore outside this repository and back it up securely.
2. In **Settings → Secrets and variables → Actions**, add these repository secrets:
   - `ANDROID_KEYSTORE_BASE64` — Base64-encoded content of the keystore file;
   - `ANDROID_KEYSTORE_PASSWORD`;
   - `ANDROID_KEY_ALIAS`;
   - `ANDROID_KEY_PASSWORD`.
3. In **Settings → Security → Code security and analysis**, enable private vulnerability reporting.
4. Protect `main`: require pull requests and the `CI` status check before merging.

On Windows PowerShell, create the first secret value without printing the keystore:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes('C:\secure\casheye-release.keystore')) |
    Set-Clipboard
```

Paste the clipboard content into `ANDROID_KEYSTORE_BASE64`; do not save it in a file or commit it.

## Local signed build

Keep the production keystore outside the repository and set these environment variables for the
current shell:

```powershell
$env:RELEASE_STORE_FILE = 'C:\secure\casheye-release.keystore'
$env:RELEASE_STORE_PASSWORD = '...'
$env:RELEASE_KEY_ALIAS = '...'
$env:RELEASE_KEY_PASSWORD = '...'
.\gradlew assembleRelease
```

The same values can be supplied as Gradle properties: `releaseStoreFile`, `releaseStorePassword`,
`releaseKeyAlias`, and `releaseKeyPassword`. Do not commit those values. A release build without a
complete signing configuration fails intentionally.

## Create a release

1. Update `versionCode` and `versionName` in `app/build.gradle.kts` according to SemVer.
2. Merge the version change into protected `main` after CI succeeds.
3. Create and push an annotated tag, for example:

```powershell
git tag -a v1.0.0 -m "Release v1.0.0"
git push github v1.0.0
```

GitHub Actions creates the release notes automatically. Verify the APK checksum before publishing a
link to the release.
