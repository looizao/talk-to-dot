# Talk to Dot

[![Build APK](https://github.com/looizao/talk-to-dot/actions/workflows/build.yml/badge.svg)](https://github.com/looizao/talk-to-dot/actions/workflows/build.yml)
[![Latest release](https://img.shields.io/github/v/release/looizao/talk-to-dot)](https://github.com/looizao/talk-to-dot/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/looizao/talk-to-dot/total)](https://github.com/looizao/talk-to-dot/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Android 8+](https://img.shields.io/badge/Android-8%2B-green.svg)](#requirements)

An Android shortcut that opens ChatGPT and navigates directly to the conversation with your chosen Dot. The default name is **zip**. You can put its icon on your home screen or assign it to a phone button that supports launching an app.

This is an independent, experimental utility, unaffiliated with OpenAI. It uses UI automation because a working public Dot-specific deep link was not identified in the tested ChatGPT build.

## Install and use

1. Download `talk-to-dot.apk` from the [latest release](https://github.com/looizao/talk-to-dot/releases/latest) and install it on your Android phone.
2. Open **Talk to Dot**, tap **Open Accessibility settings**, and enable its service.
3. If Android blocks enabling a sideloaded service, review the app and then use **Settings → Apps → Talk to Dot → ⋮ → Allow restricted settings**, if offered by your device. Return to Accessibility and enable **Use Talk to Dot**. Menu names vary by phone.
4. To choose a different Dot, long-press the **Talk to Dot** icon and select **Choose Dot**. Enter its name exactly as shown in ChatGPT’s sidebar and tap **Save name** or **Save and open Dot**. You can also open this screen from the Accessibility service’s settings.
5. Open **Talk to Dot** again. Allow a few seconds for it to navigate to **Message <your Dot name>**.
6. Add its icon to your home screen or select it in your phone's **Launch app** button setting.

To stop automation, disable the Talk to Dot Accessibility service. To remove it completely, uninstall Talk to Dot.

## Requirements

- Android 8/API 26 or newer for this utility; ChatGPT itself may require a newer Android version.
- ChatGPT installed and signed in, with Dot access already set up.
- An existing Dot whose sidebar name exactly matches the saved name, and the interface labels described below.

## Choose a Dot

**v1.2.1** introduces the **Talk to Dot** name. Configurable Dot names and faster screen-driven navigation remain available. Download the signed APK from [Releases](https://github.com/looizao/talk-to-dot/releases/latest); it updates existing installations without changing the saved name.

Version 1.2.0 and newer support a saved Dot name. Long-press the app icon → **Choose Dot**, or open **Talk to Dot** under Android Accessibility and use its service settings. The setup screen also has a **Choose Dot** button when the service is disabled.

Enter the exact sidebar name, including capitalization, and tap **Save name**. Leading and trailing whitespace is trimmed; blank names are rejected. **Save and open Dot** saves the name and launches the shortcut. **Reset to zip** immediately restores the default. The setting survives app restarts and updates. Existing installations without a saved name continue using `zip`.

The app retains its **Talk to Dot** name and icon regardless of the chosen Dot. Changing the setting does not rename or create a Dot in ChatGPT. If your launcher does not expose app shortcuts, use the Accessibility service’s settings instead.

## Updating from an earlier version

Install the latest signed APK over your existing installation. Version 1.2.1 renames the launcher, setup screen, and Accessibility service to **Talk to Dot** and uses a D icon. Your saved Dot name and existing Accessibility authorization remain compatible; zip is still the default.

The Android package `local.zipshortcut` and service component `ZipService` retain their original identifiers for update compatibility. The repository is now [looizao/talk-to-dot](https://github.com/looizao/talk-to-dot), and new builds produce `talk-to-dot.apk`. Versions 1.1.0 and 1.2.0 were published before the rename; their existing APK assets remain named `talk-to-zip.apk` and display the former app name.

## How it works

`MainActivity` starts ChatGPT's launcher activity, using `CLEAR_TOP` and `SINGLE_TOP` so an old screen above it does not prevent navigation. It arms `ZipService` for at most 30 seconds. The service snapshots the locally saved name at launch, defaulting to `zip`.

The Accessibility service inspects ChatGPT's visible accessibility nodes and:

1. Stops after the composer exactly matches **Message <saved name>** with the sidebar closed for a short confirmation period.
2. Selects the entry whose name exactly matches the saved Dot name if the sidebar is open, identified by **Scheduled**.
3. Opens **Menu** from a regular ChatGPT screen.
4. Uses **Navigate up** or **Navegar para cima** when a nested screen such as **Tasks** has a back arrow instead of Menu, then continues toward the sidebar.

It tries a node's click action, then a clickable parent, and finally a gesture at the node's bounds. Navigation advances as soon as Accessibility reports a usable next screen. A 120 ms watchdog handles missed events, and a 700 ms retry delay applies only when the same action remains visible, avoiding duplicate menu toggles. After selecting a Dot, it watches for the composer instead of reopening the menu while the old chat controls remain visible. A 4-second recovery window handles a stalled selection; it does not postpone completion when the composer is ready. The destination is confirmed for 150 ms to reject transient screen state. Nodes are refreshed and the active ChatGPT window is checked again before clicking or dispatching a gesture. Switching apps pauses interaction; returning within the 30-second deadline resumes it. Accessibility feedback interruption does not cancel navigation. The shortcut times out if it cannot reach the selected Dot. It does not send a message or start a call.

## Accessibility and privacy

Android grants Accessibility services broad screen-reading and interaction capabilities. This service is configured for `com.openai.chatgpt` and checks the active package before interacting. It acts only during the 30-second window after you launch the shortcut.

The app requests no `INTERNET` permission, has no analytics or remote service, and does not persist conversation content. Debug-level Android logs record generic navigation actions and duration, without Dot names or chat content. Its preference storage contains the selected Dot name and the temporary automation deadline and start time. The source and manifest are available for inspection.

## What was tested

Version 1.1 was tested on a Motorola Edge 60 Pro with ChatGPT **1.2026.272**:

- Ordinary ChatGPT screen → shortcut → **Message zip**.
- ChatGPT restart → shortcut → **Message zip**.
- **Tasks → Home → tap Talk to Dot in Niagara Launcher → Message zip**.
- The same Tasks flow with an ordinary new chat beneath Tasks, verifying that automation returns, opens the sidebar, and selects Zip.

Configuration was also checked on-device: the default zip, saving and reopening a custom name, rejecting a blank save, timing out for a nonexistent name without selecting a different Dot, resetting to zip, and Save and open Dot.

Version 1.2.0 also has ADB-driven device checks for background music, launching from YouTube Music, restarting ChatGPT from Chrome under bounded CPU load, and switching to Music during cold startup before returning to ChatGPT. Tests check the exact destination composer and that Music remains foreground during interruption.

For repeatable checks on an unlocked phone with Accessibility enabled and the chosen name already saved:

```bash
export ANDROID_SERIAL="your-device-serial" # Required when multiple devices are connected.
python scripts/device-check.py music --dot-name zip
python scripts/device-check.py cold --dot-name zip
python scripts/device-check.py interrupted --dot-name zip
```

These checks open an ordinary new chat, visit Tasks, launch YouTube Music or Chrome, and may force-stop ChatGPT. Start music yourself before testing; the script does not start playback. They never send messages. UI snapshots are stored in ignored `build/device-checks/` and may contain private screen content; do not publish them. The script requires the tested interface labels and those two apps. UiAutomator snapshots can temporarily affect Accessibility services, so navigation timing should be treated as approximate; the interrupted check uses activity state while the request is armed.

The v1.1 checks were manual. GitHub Actions verifies APK compilation and signing, but does not prove navigation works on every ChatGPT version. App updates, translated labels, a Dot renamed without updating the setting, or unexpected dialogs can break the shortcut.

## Navigation speed

Version 1.2.0 removes the previous 500 ms startup wait, 400 ms polling interval, and 1.2-second delay between different navigation actions. Accessibility events drive progress immediately; retries of the same unchanged action remain throttled.

A device comparison from Tasks with ChatGPT already running measured 2,266 ms before this change and 926 ms afterwards. A launcher run with an ordinary chat beneath Tasks also reached Zip successfully. Timing measures service start to accessibility confirmation of the target composer, rather than the moment pixels finish animating. These are individual checks, not guaranteed performance numbers.

Final reliability checks on the same phone recorded 1,061 ms when launching from Music with playback active, 6,711 ms for a cold start from Chrome with music and two bounded CPU load workers, and 4,294 ms for a cold-start interruption including a two-second stay in Music before returning. All reached the exact Zip composer; the interruption check also verified Music stayed foreground. Timing includes the launch request and survives service reconnections. ADB UI inspection affects Accessibility delivery, so these are approximate diagnostic timings, not a benchmark.

ChatGPT startup remains outside the shortcut’s control: a check after force-stopping ChatGPT took 8,032 ms. This is still menu automation, not a native direct Dot link. The tested ChatGPT app registers a direct Remote route but no equivalent Dot route was found.

## Build from the command line

Install JDK 17+ and the Android SDK command-line tools, then:

```bash
export ANDROID_HOME="/path/to/Android/Sdk"
# Set JAVA_HOME if java is not already on PATH.
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" \
  'platforms;android-36' 'build-tools;36.1.0'
./scripts/build.sh
```

Outputs: `dist/talk-to-dot.apk` and `dist/SHA256SUMS.txt`. Without release credentials the script creates a local development signing key in ignored `build/`. A development-signed APK cannot update an official release without uninstalling it first.

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
gh release download --pattern 'talk-to-dot.apk' --pattern 'SHA256SUMS.txt'
sha256sum -c SHA256SUMS.txt
```

## Releases

The `Build APK` workflow runs on pushes to `main`, pull requests, manual dispatch, and `v*` tags. Tag builds use the persistent signing key in Actions secrets. A successful tag build publishes the APK and SHA-256 checksum through `gh release create`.

For the current release procedure and signing setup, see [RELEASING.md](RELEASING.md). Device checks include settings persistence, blank-name rejection, reset to the default, and the Tasks navigation regression. Broader device and language compatibility testing remains future work.

## License

[MIT](LICENSE).
