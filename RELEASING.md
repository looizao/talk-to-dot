# Releasing Talk to Zip

## Current procedure

1. Update `android:versionName` and increment `android:versionCode` in `app/AndroidManifest.xml`. Version names use `MAJOR.MINOR.PATCH`.
2. Add `docs/releases/vMAJOR.MINOR.PATCH.md` describing changes, tested device flows, and compatibility limits.
3. Build and test on a real device, including **Tasks → Home → tap shortcut → Message zip**. Also test with an ordinary chat beneath Tasks; returning to an already-open Zip conversation does not cover the full navigation path.
4. Push the changes to `main` and wait for `Build APK` to pass.
5. Create and push the matching annotated tag:

```bash
git tag -a v1.2.0 -m 'Talk to Zip v1.2.0'
git push origin v1.2.0
gh run list --workflow build.yml --limit 5
gh run watch RUN_ID --exit-status
gh release view v1.2.0
```

Use the version you actually prepared. The workflow rejects a tag that does not match the manifest or has no release notes. Tags should point to the tested commit. Do not move published tags; fix a failed or incorrect published release with a new version.

## Signing secrets

This repository's release workflow requires:

- `ANDROID_KEYSTORE_BASE64`: base64-encoded persistent release keystore.
- `ANDROID_KEYSTORE_PASSWORD`: password for the keystore and private key.

The existing key alias is `local`. The initial published release uses the same key as the previously installed prototype, preserving Android update compatibility. Keystores and passwords are not committed. Each job restores the key only into the runner's temporary directory and removes it before uploading artifacts.

Example setup for a maintainer who already has the correct private keystore:

```bash
base64 -w 0 /private/path/release.keystore | gh secret set ANDROID_KEYSTORE_BASE64
gh secret set ANDROID_KEYSTORE_PASSWORD
```

Back up the private keystore securely. A replacement key cannot transparently update existing sideloaded installations. A fork needs its own secrets, signing key, and repository badge URLs.

## Limits of this first process

CI compiles the APK, verifies its signature, and publishes checksums. UI navigation remains a manual real-device check. There is no staged rollout or required release approval environment yet. Dot names are configurable starting in v1.2.0. Before publishing, verify the default, saving and reopening a custom name, blank-name rejection, reset to zip, and navigation from Tasks. A configured name that does not exist must time out instead of opening a different Dot. Future work includes language support, device/UI regression coverage, and a fuller release review process.
