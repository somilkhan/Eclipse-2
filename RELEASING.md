# Releasing InstaEclipse

Releases are built and published by GitHub Actions
([`.github/workflows/release.yml`](.github/workflows/release.yml)) when a version tag is pushed.
Nobody builds or uploads a release APK by hand.

| Tag | Result |
|---|---|
| `v0.8.4` | Signed APK, GitHub Release marked **Latest** |
| `v0.8.5-beta.1`, `v0.8.5-rc.1`, `v0.9.0-alpha.2` | Signed APK, GitHub Release marked **Pre-release** |

The debug workflow ([`main.yml`](.github/workflows/main.yml)) keeps running unit tests and building
a debug APK on pushes to `main` and `maintainer/**`, and on every pull request.

## One-time setup: signing key and secrets

The release workflow signs with a keystore stored in 4 repository secrets. If any secret is
missing, the workflow stops at its first step with an error listing the missing ones and publishes
nothing.

### 1. Create the keystore (once, on your own machine)

```sh
keytool -genkeypair -v \
  -keystore instaeclipse-release.jks \
  -storetype PKCS12 \
  -alias instaeclipse \
  -keyalg RSA -keysize 4096 \
  -validity 10000 \
  -dname "CN=InstaEclipse, O=somilkhan"
```

`keytool` ships with any JDK. It asks for a keystore password; with PKCS12 the key password is the
same as the keystore password.

**Back up `instaeclipse-release.jks` and its password somewhere safe and offline** (password
manager + encrypted backup). If the key is lost, future updates cannot be installed over existing
ones and every user has to uninstall and reinstall again. Never commit the keystore; `*.jks` /
`*.keystore` must stay out of git.

### 2. Add the 4 secrets

GitHub -> repository **Settings -> Secrets and variables -> Actions -> New repository secret**:

| Secret | Value |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | The keystore file, base64-encoded on one line (see below) |
| `RELEASE_KEYSTORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | `instaeclipse` (the `-alias` used above) |
| `RELEASE_KEY_PASSWORD` | Key password (same as the keystore password for PKCS12) |

Encode the keystore:

```sh
# Linux
base64 -w0 instaeclipse-release.jks > keystore.b64
# macOS
base64 -i instaeclipse-release.jks -o keystore.b64
# Windows PowerShell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("instaeclipse-release.jks")) | Out-File -NoNewline keystore.b64
```

Paste the contents of `keystore.b64` as `RELEASE_KEYSTORE_BASE64`, then delete `keystore.b64`.

Or with the GitHub CLI:

```sh
gh secret set RELEASE_KEYSTORE_BASE64 --repo somilkhan/InstaEclipse-2 < keystore.b64
gh secret set RELEASE_KEYSTORE_PASSWORD --repo somilkhan/InstaEclipse-2
gh secret set RELEASE_KEY_ALIAS --repo somilkhan/InstaEclipse-2 --body instaeclipse
gh secret set RELEASE_KEY_PASSWORD --repo somilkhan/InstaEclipse-2
```

Also make sure **Settings -> Actions -> General -> Workflow permissions** allows the workflow token
to create releases (the workflow requests `contents: write`).

## Signing key change: existing users must reinstall once

Releases from this repository are signed with the maintainer's new key, not the key used for the
original ReSo7200 builds. Android refuses to update an app signed with a different key, so anyone
coming from an original build (0.8.3 and earlier from ReSo7200/InstaEclipse) has to:

1. Back up settings: companion app -> Tools -> Backup settings (or in Instagram: InstaEclipse menu ->
   Backup & restore).
2. Uninstall the old InstaEclipse.
3. Install the new APK, enable it again in LSPosed for Instagram (or re-patch with LSPatch), then
   force-stop and reopen Instagram.
4. Restore the backup.

This is needed only once. Put this note in the release notes of the first release signed with the
new key (0.8.4). Later releases update in place.

## Cutting a release

1. Make sure `main` is green in CI.
2. Bump the version in [`app/build.gradle`](app/build.gradle):
   - `versionName` = the tag without `v` (for a pre-release, either `0.8.5-beta.1` or the base
     `0.8.5` is accepted; the workflow fails if neither matches).
   - `versionCode` = previous + 1 (must always increase, including for betas).
3. If the tested Instagram build changed, update `TESTED_INSTAGRAM` in
   [`ExportBrand.java`](app/src/main/java/io/github/somilkhan/instaeclipse2/utils/core/ExportBrand.java) and the
   README Compatibility section.
4. Add a section to [`CHANGELOG.md`](CHANGELOG.md). The heading must be one of `## 0.8.4`,
   `## v0.8.4` or `## [0.8.4]` (text after the version, like ` - 2026-11-27`, is fine). For
   `v0.8.5-beta.1` the workflow uses `## 0.8.5-beta.1` if present, otherwise `## 0.8.5`. The section
   body becomes the release notes; the workflow fails if it is missing or empty. Check locally:

   ```sh
   .github/scripts/changelog-section.sh 0.8.4
   ```

5. Commit, then tag that commit and push the tag:

   ```sh
   git commit -am "Release 0.8.4"
   git push origin main
   git tag -a v0.8.4 -m "InstaEclipse 0.8.4"
   git push origin v0.8.4
   ```

6. Watch **Actions -> Release**. It checks the secrets, the tag/versionName match and the
   changelog, runs unit tests, builds `assembleRelease` signed with the secrets, verifies the
   signature with `apksigner`, and publishes `InstaEclipse-v0.8.4.apk` (+ `.sha256`) as a GitHub
   Release.
7. For a stable release only: update [`version.json`](version.json) on `main` (`latest_version`)
   so the in-app update checker offers it. Leave it alone for pre-releases.

### If something fails

- Missing secrets / bad tag / version mismatch / no changelog section: nothing was published. Fix
  it, delete and re-push the tag:

  ```sh
  git tag -d v0.8.4 && git push origin :refs/tags/v0.8.4
  git tag -a v0.8.4 -m "InstaEclipse 0.8.4" && git push origin v0.8.4
  ```

- The release exists but is wrong: delete it on GitHub (Releases -> Delete) and re-push the tag as
  above. Never reuse a `versionCode` that users may already have installed.

## Building a signed APK locally (optional)

```sh
export IE_KEYSTORE_PATH=/path/to/instaeclipse-release.jks
export IE_KEYSTORE_PASSWORD=... IE_KEY_ALIAS=instaeclipse IE_KEY_PASSWORD=...
./gradlew assembleRelease
```

Without these variables `assembleRelease` produces an unsigned APK. Set `IE_REQUIRE_SIGNING=true`
to make Gradle fail instead.
