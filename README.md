<a name="top"></a>
<p align="center">
  <img src="https://placehold.co/1200x320/0d1117/58a6ff?text=Nai's+Patches&font=montserrat" alt="Nai's Patches banner">
</p>

<div align="center">

![Morphe](https://img.shields.io/badge/Morphe-Patcher-9b59b6?style=for-the-badge)
![Patches](https://img.shields.io/badge/22%20Patches-2ecc71?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)
![Platform](https://img.shields.io/badge/Android-Games-ff7f50?style=for-the-badge)

# Nai's Patches

A curated collection of Morphe patches that tune, unlock and declutter Android games. Skip ads, bypass license checks, hide root, force orientations and more, all from a single patcher.

</div>

---

## 📑 Table of Contents

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
## 📖 Overview

Nai's Patches is a set of ready made patches built for the [Morphe](https://github.com/MorpheApp) patcher. Each patch targets a common annoyance found in modern Android games: rewarded ad walls, forced splash screens, license popups, root and integrity detection, and locked in app purchases.

> [!NOTE]
> Every patch is optional. Enable only what you need for a given game. Most patches work independently, and several can be combined for a cleaner experience.

The project is open source and community driven. New patches and fixes land through standard pull requests, and releases are produced automatically.

---

<a name="install"></a>
## 📦 Install Source

Add Nai's Patches as a source inside the Morphe patcher.

| Method | Link |
| :--- | :--- |
| Deep link | [morphe.software/add-source?github=Nai64/Nai64Patches](https://morphe.software/add-source?github=Nai64/Nai64Patches) |
| Manual | `https://github.com/Nai64/Nai64Patches` |

> [!TIP]
> Tap the deep link on a device that already has Morphe installed to add the source in one step.

<a name="features"></a>
## ✨ Features

Patches are grouped by what they affect. The table below lists every available patch and a short description.

### Ads, Tracking and Consent

| Patch | What it does |
| :--- | :--- |
| Ads Free Rewards | Skip rewarded ads and claim rewards instantly |
| No Ads | Remove interstitial, banner, app open, MREC and rewarded ads |
| Skip Consent Popup | Skip the Google consent, GDPR, popup |
| Skip Rate Us Prompt | Dismiss the annoying rate this app prompt |
| Spoof Play Store Install Source | Make the app believe it was installed from the Play Store |
| Limit Ad Tracking | Reduce ad tracking identifiers |
| Disable Telemetry | Block analytics and tracking SDKs |

### Unlocks and Licensing

| Patch | What it does |
| :--- | :--- |
| Bypass License Verification | Skip Google Play license checks |
| Bypass Google Play Services Check | Neutralize Play Services availability checks |
| Pairip Bypass (Experimental) | Bypass Pairip license protection |
| Unlock RevenueCat Entitlements | Unlock RevenueCat entitlements |
| Unlock All IAPs (Unity IAP) | Unlock all in app purchases in Unity IAP games |

### Display and Device

| Patch | What it does |
| :--- | :--- |
| Remove Permissions | Remove permissions from the app manifest |
| Remove Hardware Requirements | Remove uses feature entries so the app installs on more devices |
| Unlock Rotation | Allow the app to rotate freely |
| Force Landscape / Portrait | Force a fixed screen orientation |
| Custom App Resolution (Experimental) | Set a custom resolution for the game |
| Allow Screenshots | Remove the screenshot blocking window flag |
| Skip Splash Screen (Experimental) | Skip or shorten splash screen delays |

### Privacy and Integrity

| Patch | What it does |
| :--- | :--- |
| Spoof Play Integrity | Spoof Play Integrity verdicts |
| Disable Root Checks | Disable root detection |
| Disable Update Checks | Stop forced update prompts |

> [!TIP]
> Combine **No Ads** with **Disable Telemetry** for the quietest possible session, and add **Ads Free Rewards** only when a game gates progress behind rewarded ads.

---

<a name="how-to-use"></a>
## 🛠️ How to Use

1. Open your game APK in the Morphe patcher.
2. Browse the patch list and toggle the patches you want.
3. Expand a patch to review its options and adjust them.
4. Run the patch and install the rebuilt APK.

> [!IMPORTANT]
> Always keep a copy of the original APK. If a patched build misbehaves, you can fall back to the unmodified version and try a different combination of patches.

Most patches expose friendly dropdowns or toggles instead of raw text fields, so you rarely need to type anything by hand.

---

<a name="patch-options"></a>
## ⚙️ Patch Options

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
<summary><b>Manifest cleanup patches</b></summary>

| Patch | Option | Default | Notes |
| :--- | :--- | :--- | :--- |
| Remove Permissions | Permissions to remove | SMS, RECORD_AUDIO, CAMERA and similar | Comma separated permission names |
| Remove Hardware Requirements | Features to remove | Empty, removes all | Comma separated feature names, leave empty to remove every uses feature entry |

</details>

---

<a name="tips"></a>
## 💡 Tips

- Start with a minimal set of patches, then add more only if needed. Smaller changes are easier to debug.
- If a game crashes after patching, disable the most recent patch you enabled and test again.
- Use the Patch version option in Ads Free Rewards to roll back to an older implementation when a newer game build stops working.
- Spoof Play Store Install Source can help with apps that restrict features to Play Store installs.
- Force Landscape or Portrait is great for games that ignore device rotation or feel wrong in one orientation.

> [!TIP]
> Keep the patcher log open while testing. Patches that find nothing simply report a warning and move on, so a clean log with no errors usually means the game just did not contain that code path.

---

<a name="warnings"></a>
## ⚠️ Warnings

> [!WARNING]
> Patches marked Experimental, such as Pairip Bypass, Custom App Resolution and Skip Splash Screen, hook deeper into app internals. They may not work on every game and can cause crashes or visual glitches.

> [!CAUTION]
> Modifying applications can violate the terms of service of the games you patch. Use these patches only on apps you own, for personal and educational purposes. The authors are not responsible for bans, data loss or other consequences.

- Do not enable every patch at once. Over patching raises the chance of conflicts.
- Some anti cheat or strongly protected games detect tampering regardless of these patches.
- Always back up your save data before installing a patched build.

---

<a name="compatibility"></a>
## 📋 Compatibility

- Targets Android games packaged as APK or XAPK.
- Requires the Morphe patcher and a Java runtime.
- Works best on standard Unity, native and ad SDK based games.
- Patch behavior depends on the exact game build. A patch that works today may need an option tweak after a game update.

> [!NOTE]
> Because each game is different, no single configuration fits all titles. Treat the patch list as a toolkit and tune it per app.

---

<a name="disclaimer"></a>
## 📜 Disclaimer

Nai's Patches is provided as is, without warranty of any kind. It is intended for learning, accessibility and personal customization of software you legally own. It is not a piracy tool and should not be used to bypass paid content you have not licensed.

---

<p align="center">
Made with the Morphe patcher. Contributions welcome.
</p>

<p align="center"><a href="#top">Back to top</a></p>
