<a name="top"></a>
<p align="center">
  <img src="https://github.com/Nai64.png" alt="Nai64" width="180" height="180">
</p>

<div align="center">

![Morphe](https://img.shields.io/badge/Morphe-Patcher-9b59b6?style=for-the-badge)
![Patches](https://img.shields.io/badge/100%20Patches-2ecc71?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)
![Platform](https://img.shields.io/badge/Android-Games-ff7f50?style=for-the-badge)

# Nai's Patches

A curated collection of Morphe patches that tune, unlock and declutter Android games and apps. Skip ads, bypass license checks, hide root, force orientations, tweak manifest behavior, strip translations and more, all from a single patcher.

</div>

---

![Table of Contents](images/header-table-of-contents.png)

- [Overview](#overview)
- [Features](#features)
- [How to Use](#how-to-use)
- [Patch Options](#patch-options)
- [Tips](#tips)
- [Warnings](#warnings)
- [Compatibility](#compatibility)
- [Disclaimer](#disclaimer)

---

<a name="overview"></a>
![Overview](images/header-overview.png)

Nai's Patches is a set of ready made patches built for the [Morphe](https://github.com/MorpheApp) patcher. Each patch targets a common annoyance found in modern Android games: rewarded ad walls, forced splash screens, license popups, root and integrity detection, and locked in app purchases.

> [!NOTE]
> Every patch is optional. Enable only what you need for a given game. Most patches work independently, and several can be combined for a cleaner experience.

The project is open source and community driven. New patches and fixes land through standard pull requests, and releases are produced automatically.

---

<a name="install"></a>
![Install Source](images/header-install-source.png)

Add Nai's Patches as a source inside the Morphe patcher.

| Method | Link |
| :--- | :--- |
| Deep link | [morphe.software/add-source?github=Nai64/Nai64Patches](https://morphe.software/add-source?github=Nai64/Nai64Patches) |
| Manual | `https://github.com/Nai64/Nai64Patches` |

> [!TIP]
> Tap the deep link on a device that already has Morphe installed to add the source in one step.

<a name="features"></a>
![Features](images/header-features.png)

Patches are grouped by what they affect. The table below lists every available patch and a short description.

<details>
<summary><b>Ads, Tracking and Consent</b></summary>


| Patch | What it does |
| :--- | :--- |
| Ads Free Rewards | Skip rewarded ads and claim rewards instantly |
| No Ads | Remove interstitial, banner, app open, MREC and rewarded ads |
| Block Push Ads | Silence notification ads |
| Skip Consent Popup | Skip the Google consent, GDPR, popup |
| Skip Rate-Us Prompt | Dismiss the annoying rate this app prompt |
| Spoof Play Store Install Source | Make the app believe it was installed from the Play Store |
| Limit Ad Tracking | Reduce ad tracking identifiers |
| Disable Telemetry | Block analytics and tracking SDKs |
| Disable Firebase Auto-Init | Stop Firebase components initializing themselves |
| Disable Facebook SDK Auto-Init | Stop Facebook auto-init, auto-logging and ad ID collection |

</details>

<details>
<summary><b>Unlocks and Licensing</b></summary>


| Patch | What it does |
| :--- | :--- |
| Bypass License Verification | Skip Google Play license checks |
| Bypass Google Play Services Check | Neutralize Play Services availability checks |
| Pairip Bypass (Experimental) | Bypass Pairip license protection |
| Unlock RevenueCat Entitlements | Unlock RevenueCat entitlements |
| Unlock All IAPs (Unity IAP) | Unlock all in app purchases in Unity IAP games |
| Spoof Play Integrity | Spoof Play Integrity verdicts |
| Spoof Signature Match | Make signature checks report a match |

</details>

<details>
<summary><b>Display and Interface</b></summary>


| Patch | What it does |
| :--- | :--- |
| Unlock Rotation | Allow the app to rotate freely |
| Force Landscape / Portrait | Force a fixed screen orientation |
| Custom App Resolution (Experimental) | Set a custom resolution for the game |
| Allow Screenshots | Remove the screenshot blocking window flag |
| Block Screenshot Detection | Block screenshots, recording and recents previews |
| Skip Splash Screen (Experimental) | Skip or shorten splash screen delays |
| Force Resizable Activity | Enable split-screen, free-form windows, DeX and Chromebooks |
| Immersive Fullscreen | Hide the status bar by forcing fullscreen themes |
| Keep Screen On | Keep the display on while any activity of the app is visible |
| Disable Vibration | Stop apps from making the device buzz |
| Status Bar Icons | Force white or dark system bar icon color in every theme |
| Transparent System Bars | Render edge-to-edge with transparent system bars |
| Unlock Max Aspect Ratio | Fill tall screens instead of letterboxing |
| Custom Startup Toast | Show a customizable toast on every launch |
| Custom Startup Dialog | Show a customizable dialog once per launch |
| Force Left-to-Right Layout | Fix layouts that break under right-to-left languages |

</details>

<details>
<summary><b>Privacy and Security</b></summary>


| Patch | What it does |
| :--- | :--- |
| Disable Crash Reporters | Neutralize Crashlytics, Sentry, Bugsnag, ACRA and similar SDKs |
| Disable Root Checks | Disable root detection |
| Disable Update Checks | Stop forced update prompts |
| Hide Debugger Connection | Let apps run even while a debugger is attached |
| Empty Clipboard Report | Stop apps harvesting clipboard content |
| Trust User Certificates | Accept any certificate in bundled trust managers |
| Bypass OkHttp Pinning | Disable certificate pinning in OkHttp clients |
| Remove Network Security Config | Drop pinning configs for HTTPS inspection |
| Allow Cleartext Traffic | Permit plain HTTP requests for local proxy debugging |

</details>

<details>
<summary><b>Device State Spoofs</b></summary>


| Patch | What it does |
| :--- | :--- |
| Fake Online State | Connectivity checks always report an active connection |
| Treat Network as Unmetered | Lift download restrictions on mobile data |
| Bypass VPN Detection | Apps cannot detect an active VPN |
| Fake Battery Whitelist | Battery optimization exemption checks return true |
| Ignore Power Save Mode | Ignore battery saver and Doze throttling |
| Treat Screen as On | Keep apps running while the screen is off |
| Ignore Low RAM Device | Stop degrading quality on entry-level devices |
| Allow Background Activity | Ignore background activity restrictions |
| Ignore Screen Lock | Report an unlocked device to lock-state checks |
| Always Allow Biometrics | Biometric capability checks report success |
| Fake Fingerprint Hardware | Report fingerprint hardware present and enrolled |
| Fake Bluetooth Enabled | Bluetooth state checks report enabled |
| Fake NFC Enabled | NFC state checks report enabled |
| Force Notifications Enabled | Notification-state checks report enabled |
| Hide Mock Location | Hide spoofed GPS from mock-location detection |
| Ignore GPS Disabled | Location service checks report enabled |
| Ignore Location Services Off | The master location switch reports on |
| Hide Roaming Status | Roaming checks report home network |
| Hide Emulator Radio | Radio type reports GSM instead of none |
| Ignore Touch Exploration | Ignore TalkBack exploration state |

</details>

<details>
<summary><b>Hardware and System Spoofs</b></summary>


| Patch | What it does |
| :--- | :--- |
| Spoof Device Model | Replace Build model, manufacturer and brand values |
| Spoof Build Fingerprint | Replace every read of Build.FINGERPRINT |
| Spoof CPU Architecture | Replace legacy CPU_ABI field reads |
| Spoof SDK Level | Override reads of Build.VERSION.SDK_INT (advanced) |
| Enable WebView Debugging | Inspect WebViews via chrome://inspect |

</details>

<details>
<summary><b>Storage and Backups</b></summary>


| Patch | What it does |
| :--- | :--- |
| Legacy External Storage | Use classic shared storage instead of scoped storage |
| Grant All-Files Access | All-files-access checks report granted |
| Report Legacy Storage | Storage-model checks report classic access |
| Remove Backup Restrictions | Let Android back up and transfer all app data |
| Keep Data on Uninstall | Offer to keep app data when uninstalling |

</details>

<details>
<summary><b>Manifest and App Tweaks</b></summary>


| Patch | What it does |
| :--- | :--- |
| Make App Debuggable | Let debuggers and profiling tools attach |
| Change Version Code | Override the version code reported to the system |
| Change Version Name | Override the version name shown in settings |
| Rename App | Change the launcher name of the app |
| Remove App Icon | Replace the launcher icon with a transparent drawable |
| Enable Large Heap | Give the app a bigger Java heap where supported |
| Force Hardware Acceleration | GPU render every activity, even if the app opts out |
| Set Install Location | Prefer internal storage, external storage or let the system decide |
| Set Profileable | Allow profilers to attach to release builds |
| Optimize as Game | Apply Android game mode optimizations |
| VM Safe Mode | Run without AOT/JIT optimizations for stability |
| Unmark Test Only | Install test-only marked builds like normal apps |
| Disable Heap Pointer Tagging | Fix crashes from Android 12+ tagged pointers |
| Export All Activities | Make every activity launchable by other apps and ADB |
| Disable Battery Optimization Prompt | Remove the ignore-battery-optimizations dialog |
| Disable App Links Verification | Stop domain verification prompts and link hijacking |
| Support All Screens | Install on devices the manifest would reject |
| Remove Ad Services Entries | Strip Privacy Sandbox permissions and metadata |
| Disable Permission Auto-Revoke | Stop Android resetting unused permissions |
| Ensure Internet Permission | Add the INTERNET permission when missing |
| Relax Shared Libraries | Make required shared libraries optional |
| Allow Clearing App Data | Keep the Clear Data button available |
| Classic Back Gesture | Restore classic back behavior on Android 13+ |
| Force Extract Native Libs | Extract native libraries for mod loaders and tools |
| Strip Translations | Delete translated resources to shrink the APK |
| Remove Permissions | Remove permissions from the app manifest |
| Remove Hardware Requirements | Remove uses feature entries so the app installs on more devices |
| Disable StrictMode | No-op StrictMode policy installs made by the app |

</details>

> [!TIP]
> Combine **No Ads** with **Disable Telemetry** for the quietest possible session, and add **Ads Free Rewards** only when a game gates progress behind rewarded ads.

---

<a name="how-to-use"></a>
![How to Use](images/header-how-to-use.png)

1. Open your game APK in the Morphe patcher.
2. Browse the patch list and toggle the patches you want.
3. Expand a patch to review its options and adjust them.
4. Run the patch and install the rebuilt APK.

> [!IMPORTANT]
> Always keep a copy of the original APK. If a patched build misbehaves, you can fall back to the unmodified version and try a different combination of patches.

Most patches expose friendly dropdowns or toggles instead of raw text fields, so you rarely need to type anything by hand.

---

<a name="patch-options"></a>
![Patch Options](images/header-patch-options.png)

Some patches are highly configurable. Expand a section to see its options.

<details>
<summary><b>Ads Free Rewards</b></summary>

| Option | Type | Default | Notes |
| :--- | :--- | :--- | :--- |
| Patch version | Dropdown | 1.19.0 (Current) | Pick a historical implementation, useful when a game only works with an older approach |
| Reward Strategy | Dropdown | Auto (all networks) | AppLovin MAX, Unity Ads, ironSource or every supported network |
| Instant reward | Toggle | On | Claim the reward immediately without showing an ad, applies to the current version |

</details>

<details>
<summary><b>Force Landscape / Portrait</b></summary>

| Option | Type | Default | Notes |
| :--- | :--- | :--- | :--- |
| Orientation | Dropdown | Landscape | Landscape, Portrait, Sensor Landscape, Sensor Portrait, User Landscape, User Portrait |

</details>

<details>
<summary><b>No Ads</b></summary>

| Option | Type | Default | Notes |
| :--- | :--- | :--- | :--- |
| Block Interstitials | Toggle | On | Full screen ads between content |
| Block Banners | Toggle | On | Top or bottom banner ads |
| Block App Open | Toggle | On | Ads shown on app start |
| Block MREC | Toggle | On | Medium rectangle banner ads |
| Block Rewarded | Toggle | On | Rewarded ads, may disable features that require watching them |

</details>

<details>
<summary><b>Custom App Resolution (Experimental)</b></summary>

| Option | Type | Default | Notes |
| :--- | :--- | :--- | :--- |
| Enable Custom Resolution | Toggle | Off | Override the game window size with the values below |
| Resolution width | Number | 1920 | Horizontal resolution in pixels |
| Resolution height | Number | 1080 | Vertical resolution in pixels |

</details>

<details>
<summary><b>Disable Telemetry</b></summary>

Twelve independent toggles let you block each analytics SDK on its own. All default to On.

| Option | Blocks |
| :--- | :--- |
| Block Firebase Analytics | Google analytics and crash reporting |
| Block AppsFlyer | Mobile attribution and marketing analytics |
| Block Adjust | Mobile attribution analytics |
| Block Amplitude | Product analytics |
| Block Mixpanel | Product analytics |
| Block CleverTap | User engagement and analytics |
| Block Segment | Customer data pipeline |
| Block Facebook Analytics | Meta analytics and event logging |
| Block Branch.io | Deep linking and attribution |
| Block Unity Analytics | Unity engine analytics |
| Block Flurry | Yahoo analytics |
| Block GameAnalytics | Game focused analytics |

</details>

<details>
<summary><b>Version and identity patches</b></summary>

| Patch | Option | Default | Notes |
| :--- | :--- | :--- | :--- |
| Change Version Code | Version code | -1, keeps original | Any positive number |
| Change Version Name | Version name | Empty, keeps original | e.g. 2.5.1 |
| Rename App | App name | Empty, keeps original | New launcher label |
| Set Install Location | Install location | Auto | Auto, Prefer external storage or Internal storage only |
| Spoof Device Model | Model / Manufacturer / Brand | Empty, keeps original | Replaces Build field reads |
| Spoof Build Fingerprint | Fingerprint | Empty, keeps original | Full build fingerprint string |
| Spoof CPU Architecture | CPU ABI dropdown | Keep original | arm64-v8a, armeabi-v7a, x86_64, x86 |
| Spoof SDK Level | SDK level | -1, keeps original | Advanced: wrong values crash apps calling newer APIs |

</details>

<details>
<summary><b>Interface patches with options</b></summary>

| Patch | Option | Default | Notes |
| :--- | :--- | :--- | :--- |
| Custom Startup Toast | Toast message | Patched by Nai's Patches | Shown on every launch |
| Custom Startup Toast | Duration | Long | Short or long toast |
| Custom Startup Dialog | Dialog title | Patched App | Title of the startup dialog |
| Custom Startup Dialog | Dialog message | This app has been modified... | Body of the dialog |
| Custom Startup Dialog | Dismissable | On | Allow closing via back button or outside tap |
| Status Bar Icons | Icon color | White | White icons on dark backgrounds or dark icons on light ones |

</details>

<details>
<summary><b>Manifest cleanup patches</b></summary>

| Patch | Option | Default | Notes |
| :--- | :--- | :--- | :--- |
| Remove Permissions | Permissions to remove | SMS, RECORD_AUDIO, CAMERA and similar | Comma separated permission names |
| Remove Hardware Requirements | Features to remove | Empty, removes all | Comma separated feature names, leave empty to remove every uses feature entry |

</details>

---

<a name="tips"></a>
![Tips](images/header-tips.png)

- Start with a minimal set of patches, then add more only if needed. Smaller changes are easier to debug.
- If a game crashes after patching, disable the most recent patch you enabled and test again.
- Use the Patch version option in Ads Free Rewards to roll back to an older implementation when a newer game build stops working.
- Spoof Play Store Install Source can help with apps that restrict features to Play Store installs.
- Force Landscape or Portrait is great for games that ignore device rotation or feel wrong in one orientation.

> [!TIP]
> Keep the patcher log open while testing. Patches that find nothing simply report a warning and move on, so a clean log with no errors usually means the game just did not contain that code path.

---

<a name="warnings"></a>
![Warnings](images/header-warnings.png)

> [!WARNING]
> Patches marked Experimental, such as Pairip Bypass, Custom App Resolution and Skip Splash Screen, hook deeper into app internals. They may not work on every game and can cause crashes or visual glitches.

> [!CAUTION]
> Modifying applications can violate the terms of service of the games you patch. Use these patches only on apps you own, for personal and educational purposes. The authors are not responsible for bans, data loss or other consequences.

- Do not enable every patch at once. Over patching raises the chance of conflicts.
- Some anti cheat or strongly protected games detect tampering regardless of these patches.
- Always back up your save data before installing a patched build.

---

<a name="compatibility"></a>
![Compatibility](images/header-compatibility.png)

- Targets Android games packaged as APK or XAPK.
- Requires the Morphe patcher and a Java runtime.
- Works best on standard Unity, native and ad SDK based games.
- Patch behavior depends on the exact game build. A patch that works today may need an option tweak after a game update.

> [!NOTE]
> Because each game is different, no single configuration fits all titles. Treat the patch list as a toolkit and tune it per app.

---

<a name="disclaimer"></a>
![Disclaimer](images/header-disclaimer.png)

Nai's Patches is provided as is, without warranty of any kind. It is intended for learning, accessibility and personal customization of software you legally own. It is not a piracy tool and should not be used to bypass paid content you have not licensed.

---

<p align="center">
Made with the Morphe patcher. Contributions welcome.
</p>

<p align="center"><a href="#top">Back to top</a></p>
