# Talk to Zip

[![Build APK](https://github.com/looizao/talk-to-zip/actions/workflows/build.yml/badge.svg)](https://github.com/looizao/talk-to-zip/actions/workflows/build.yml)
[![Latest release](https://img.shields.io/github/v/release/looizao/talk-to-zip)](https://github.com/looizao/talk-to-zip/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/looizao/talk-to-zip/total)](https://github.com/looizao/talk-to-zip/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Android 8+](https://img.shields.io/badge/Android-8%2B-green.svg)](#requirements)

An Android shortcut that opens ChatGPT and navigates directly to the conversation with your Dot named **zip**. You can put its icon on your home screen or assign it to a phone button that supports launching an app.

This is an independent, experimental utility, unaffiliated with OpenAI. It uses UI automation because a working public Dot-specific deep link was not identified in the tested ChatGPT build.

## Install and use

1. Download `talk-to-zip.apk` from the [latest release](https://github.com/looizao/talk-to-zip/releases/latest) and install it on your Android phone.
2. Open **Talk to Zip**, tap **Open Accessibility settings**, and enable its service.
3. If Android blocks enabling a sideloaded service, review the app and then use **Settings → Apps → Talk to Zip → ⋮ → Allow restricted settings**, if offered by your device. Return to Accessibility and enable **Use Talk to Zip**. Menu names vary by phone.
4. Open **Talk to Zip** again. Allow a few seconds for it to navigate to **Message zip**.
5. Add its icon to your home screen or select it in your phone's **Launch app** button setting.

To stop automation, disable the Talk to Zip Accessibility service. To remove it completely, uninstall Talk to Zip.

## Requirements

- Android 8/API 26 or newer for this utility; ChatGPT itself may require a newer Android version.
- ChatGPT installed and signed in, with Dot access already set up.
- A Dot named exactly **zip** and the interface labels described below.

The APK currently targets `zip`; it has no name-selection settings yet. For a different Dot, fork the repository, update the `zip`, `Message zip`, and explanatory text literals in `app/src/local/zipshortcut/`, and build your own APK. Broad Dot-name and language support is future work.

## How it works

`MainActivity` starts ChatGPT's launcher activity, using `CLEAR_TOP` and `SINGLE_TOP` so an old screen above it does not prevent navigation. It arms `ZipService` for at most 15 seconds.

The Accessibility service inspects ChatGPT's visible accessibility nodes and:

1. Stops when the composer shows **Message zip** and the sidebar is closed.
2. Selects **zip** if the sidebar is open, identified by **Scheduled**.
3. Opens **Menu** from a regular ChatGPT screen.
4. Uses **Navigate up** or **Navegar para cima** when a nested screen such as **Tasks** has a back arrow instead of Menu, then continues toward the sidebar.

It tries a node's click action, then a clickable parent, and finally a gesture at the node's bounds. Navigation attempts are spaced out; the shortcut times out if it cannot reach Zip. It does not send a message or start a call.

## Accessibility and privacy

Android grants Accessibility services broad screen-reading and interaction capabilities. This service is configured for `com.openai.chatgpt` and checks the active package before interacting. It acts only during the 15-second window after you launch the shortcut.

The app requests no `INTERNET` permission, has no analytics or remote service, and does not persist conversation content. Its preference storage contains only the temporary automation deadline. The source and manifest are available for inspection.

## What was tested

Version 1.1 was tested on a Motorola Edge 60 Pro with ChatGPT **1.2026.272**:

- Ordinary ChatGPT screen → shortcut → **Message zip**.
- ChatGPT restart → shortcut → **Message zip**.
- **Tasks → Home → tap Talk to Zip in Niagara Launcher → Message zip**.
- The same Tasks flow with an ordinary new chat beneath Tasks, verifying that automation returns, opens the sidebar, and selects Zip.

These were manual device checks, not automated UI tests. GitHub Actions verifies APK compilation and signing, but does not prove navigation works on every ChatGPT version. App updates, translated labels, a renamed Dot, or unexpected dialogs can break the shortcut.

## Build from the command line

Install JDK 17+ and the Android SDK command-line tools, then:

```bash
export ANDROID_HOME="/path/to/Android/Sdk"
# Set JAVA_HOME if java is not already on PATH.
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" \
  'platforms;android-36' 'build-tools;36.1.0'
./scripts/build.sh
```

Outputs: `dist/talk-to-zip.apk` and `dist/SHA256SUMS.txt`. Without release credentials the script creates a local development signing key in ignored `build/`. A development-signed APK cannot update an official release without uninstalling it first.

For a release build, supply `RELEASE_BUILD=true`, `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, optional `KEY_PASSWORD`, and `KEY_ALIAS`. Keep the keystore private. SDK versions can be overridden with `SDK_PLATFORM` and `BUILD_TOOLS_VERSION`.

## Build using GitHub CLI

From a clone of this repository, with `gh` authenticated:

```bash
gh workflow run build.yml --ref main
gh run list --workflow build.yml --limit 5
# Replace RUN_ID with the run you just dispatched.
gh run watch RUN_ID --exit-status
gh run download RUN_ID --dir downloaded-apk
```

Manual, main-branch, and pull-request builds produce **development APKs**. Download official signed APKs from Releases:

```bash
gh release download --pattern 'talk-to-zip.apk' --pattern 'SHA256SUMS.txt'
sha256sum -c SHA256SUMS.txt
```

## Releases

The `Build APK` workflow runs on pushes to `main`, pull requests, manual dispatch, and `v*` tags. Tag builds use the persistent signing key in Actions secrets. A successful tag build publishes the APK and SHA-256 checksum through `gh release create`.

For the current release procedure and signing setup, see [RELEASING.md](RELEASING.md). A stricter release process and broader compatibility testing are planned for later releases.

## License

[MIT](LICENSE).
