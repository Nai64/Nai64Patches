<a name="top"></a>
<p align="center">
  <img src="https://github.com/Nai64.png" alt="Nai64" width="180" height="180">
</p>

<div align="center">

![Morphe](https://img.shields.io/badge/Morphe-Patcher-9b59b6?style=for-the-badge)
![Patches](https://img.shields.io/badge/6%20Patches-2ecc71?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)
![Platform](https://img.shields.io/badge/Android-Apps%20%26%20Games-ff7f50?style=for-the-badge)

# Nai's Patches

A curated collection of Morphe patches that tune, unlock and declutter Android apps and games. This source ships the 6 Featured patches: Unlock Premium, Free In-app Purchases, No Ads, Ads Free Rewards, Unlimited Currencies and Pairip Bypass.

> [!NOTE]
> Looking for the other 430 patches (spoofing, manifest tweaks, telemetry blocks, UI tweaks and more)? Add the companion source **[Nai's Extra Patches](https://github.com/Nai64/Nai64ExtraPatches)** alongside this one.

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

Nai's Patches is a set of ready made patches built for the [Morphe](https://github.com/MorpheApp) patcher. Each patch targets a common annoyance found in modern Android apps and games: rewarded ad walls, forced splash screens, license popups, root and integrity detection, and locked in app purchases.

> [!NOTE]
> Every patch is optional. Enable only what you need for a given app or game. Most patches work independently, and several can be combined for a cleaner experience.

The project is open source and community driven. New patches and fixes land through standard pull requests, and releases are produced automatically.

> [!IMPORTANT]
> **Want to contribute?** Please read [`CONTRIBUTING.md`](CONTRIBUTING.md) first. It shows how to open a proper PR with one patch at a time, the right commit style, and how to avoid common mistakes that block merges.

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

This source ships the 6 Featured patches only.

| Patch | What it does |
| :--- | :--- |
| Unlock Premium | Unlock premium features and remove paywalls |
| Free In-app Purchases | Get paid items for free, best for offline games |
| No Ads | Blocks ads by type, pick what to block |
| Ads Free Rewards | Get rewards without watching ads |
| Unlimited Currencies | Unlimited coins, gems and other currencies in most games |
| Pairip Bypass | Bypass app protection so the patched app can start |

> [!TIP]
> Combine **No Ads** with **Ads Free Rewards** (keep No Ads' rewarded block off) for the quietest session in apps that gate progress behind rewarded ads. For spoofing, manifest tweaks, telemetry blocks and UI tweaks, add **[Nai's Extra Patches](https://github.com/Nai64/Nai64ExtraPatches)**.

---

<a name="how-to-use"></a>
![How to Use](images/header-how-to-use.png)

1. Open your app or game APK in the Morphe patcher.
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
| Patch version | Dropdown | 1.19.0 (Current) | Pick a historical implementation, useful when an app only works with an older approach |
| Reward Strategy | Dropdown | Auto (all networks) | AppLovin MAX, Unity Ads, ironSource or every supported network |
| Instant reward | Toggle | On | Claim the reward immediately without showing an ad, applies to the current version |

</details>

<details>
<summary><b>No Ads</b></summary>

| Option | Type | Default | Notes |
| :--- | :--- | :--- | :--- |
| Block Interstitials | Toggle | On | Full screen ads between content |
| Block Banners | Toggle | On | Top or bottom banner ads |
| Block App Open | Toggle | On | Ads shown on app start |
| Block MREC | Toggle | On | Medium rectangle banner ads |
| Block Rewarded | Toggle | Off | Rewarded video — disable if you use Ads Free Rewards, otherwise progress gates may break. Enable only to fully remove rewarded ads |
| Block Native | Toggle | On | Native ads blended into feeds/lists |

</details>

---

<a name="tips"></a>
![Tips](images/header-tips.png)

- Start with a minimal set of patches, then add more only if needed. Smaller changes are easier to debug.
- If an app crashes after patching, disable the most recent patch you enabled and test again.
- Use the Patch version option in Ads Free Rewards to roll back to an older implementation when a newer app build stops working.
- Pairip Bypass helps when a patched app refuses to start on launch.
- For orientation locks, spoofs and manifest tweaks, add Nai's Extra Patches.

> [!TIP]
> Keep the patcher log open while testing. Patches that find nothing simply report a warning and move on, so a clean log with no errors usually means the app just did not contain that code path.

---

<a name="warnings"></a>
![Warnings](images/header-warnings.png)

> [!WARNING]
> Patches marked Experimental, such as Pairip Bypass, hook deeper into app internals. They may not work on every app and can cause crashes or visual glitches.

> [!CAUTION]
> Modifying applications can violate the terms of service of the apps you patch. Use these patches only on apps you own, for personal and educational purposes. The authors are not responsible for bans, data loss or other consequences.

- Do not enable every patch at once. Over patching raises the chance of conflicts.
- Some anti cheat systems or strongly protected apps detect tampering regardless of these patches.
- Always back up your save data before installing a patched build.

---

<a name="compatibility"></a>
![Compatibility](images/header-compatibility.png)

- Targets Android apps and games packaged as APK or XAPK.
- Requires the Morphe patcher and a Java runtime.
- Works best on standard Unity, native and ad SDK based apps.
- Patch behavior depends on the exact app build. A patch that works today may need an option tweak after an app update.

> [!NOTE]
> Because each app is different, no single configuration fits all titles. Treat the patch list as a toolkit and tune it per app.

- **Compatibility reports (community):** the maintainer does not test every app — search and contribute via [`COMPATIBILITY.md`](COMPATIBILITY.md) and the [Compatibility Report](.github/ISSUE_TEMPLATE/compatibility-report.yml) issue template ([label:compatibility](https://github.com/Nai64/Nai64Patches/issues?q=label%3Acompatibility) issues).
- **Troubleshooting:** see [`TROUBLESHOOTING.md`](TROUBLESHOOTING.md) — `Got this error? Try enabling x patch.` Common install errors, save-data preservation (`Preserve App Data` / `Keep Data on Uninstall`), Unity/Il2Cpp limits.

---

<a name="disclaimer"></a>
![Disclaimer](images/header-disclaimer.png)

Nai's Patches is provided as is, without warranty of any kind. It is intended for learning, accessibility and personal customization of software you legally own. It is not a piracy tool and should not be used to bypass paid content you have not licensed.

---

<p align="center">
Made with the Morphe patcher. Contributions welcome.
</p>

<p align="center"><a href="#top">Back to top</a></p>
