<a name="top"></a>
<p align="center">
  <img src="https://github.com/Nai64.png" alt="Nai64" width="180" height="180">
</p>

<div align="center">

[![Discord](https://img.shields.io/badge/Discord-Join-7289DA?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/an9uXxMNyc)

# Nai's Patches

A curated collection of Morphe patches for Android apps and games. This source ships 5 Featured patches plus per-title patches for supported games and apps.

> [!NOTE]
> Need spoofing, manifest tweaks, telemetry blocks or UI tweaks? Add the companion source **[Nai's Extra Patches](https://github.com/Nai64/Nai64ExtraPatches)** alongside this one. It ships 432 patches.

</div>

---

![Table of Contents](images/header-table-of-contents.png)

- [Overview](#overview)
- [Featured Patches](#features)
- [Game and App Patches](#games)
- [How to Use](#how-to-use)
- [Patch Options](#patch-options)
- [Tips](#tips)
- [Warnings](#warnings)
- [Compatibility](#compatibility)
- [Disclaimer](#disclaimer)

---

<a name="overview"></a>
![Overview](images/header-overview.png)

Nai's Patches is a set of ready made patches built for the [Morphe](https://github.com/MorpheApp) patcher. It has two halves:

- **5 Featured patches** that work across most apps: premium unlocks, free purchases, ad blocking, ad-free rewards and Pairip bypass.
- **Per-title patches** for specific games and apps, split into small independent toggles so you pick exactly what you want.

> [!NOTE]
> Every patch is optional and off by default. Enable only what you need. Most patches work independently, and several can be combined.

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

## Featured Patches

Five patches that work on most apps and games.

| Patch | What it does |
| :--- | :--- |
| Unlock Premium | Unlocks premium features and removes paywalls |
| Free In-app Purchases | Grants paid items without charging. Best for offline games |
| No Ads | Blocks interstitials, banners, app open, MREC and native ads. Rewarded ads stay on by default |
| Ads Free Rewards | Grants the reward from a rewarded ad without playing it |
| Pairip Bypass | Defeats Pairip startup protection so a patched app can launch |

> [!TIP]
> Combine **No Ads** with **Ads Free Rewards** and leave `Block Rewarded` off. That removes the ad walls while keeping reward gates working. For spoofing, manifest tweaks, telemetry blocks and UI tweaks, add **[Nai's Extra Patches](https://github.com/Nai64/Nai64ExtraPatches)**.

---

<a name="games"></a>

## Game and App Patches

These patches are tied to one package and only show up for that app. All are off by default.

### Spirit Detective

Package `com.ghost.detective.rpg`, tested on 1.01.27.

Eleven independent toggles. Mix and match freely. Each one checks the game's config files by hash before writing, so an unknown game version refuses to patch instead of producing a broken build.

| Patch | What it changes |
| :--- | :--- |
| Unlimited Diamonds | Diamond rewards and drops |
| Unlimited Gold | Gold rewards and drops |
| Unlimited Armor Shards | Shard rewards and gear sale values, plus 0 shard part upgrades |
| Unlimited Skill Points | Skill point rewards, plus 0 cost skills |
| Unlimited Keys | Armory, golden and developer key rewards |
| Unlimited VIP Points | VIP point rewards |
| Unlimited Ad-Free Tickets | Ad-free ticket rewards |
| Unlimited Active Points | Daily activity, active and passive point rewards |
| Unlimited Materials | Crafting and upgrade material rewards |
| Free Purchases | Shop and upgrade prices |
| Free Equipment Upgrades | Equipment, star and collar upgrade materials |

> [!NOTE]
> The reward patches raise the matching reward to 1,000,000,000. These are client side numbers, not true infinity, and some screens may display them in short form.

### Vivo TWS

Package `com.vivo.vivotws`.

| Patch | What it does |
| :--- | :--- |
| Accept any earphones | Shows any paired Bluetooth audio device as a Vivo TWS earphone so it can be selected and connected |

---

<a name="how-to-use"></a>
![How to Use](images/header-how-to-use.png)

1. Open your app or game APK in the Morphe patcher.
2. Browse the patch list and toggle the patches you want.
3. Expand a patch to review its options and adjust them.
4. Run the patch and install the rebuilt APK.

> [!IMPORTANT]
> Always keep a copy of the original APK. If a patched build misbehaves, you can fall back to the unmodified version and try a different combination of patches.

Most patches expose dropdowns and toggles instead of raw text fields, so you rarely need to type anything by hand.

---

<a name="patch-options"></a>
![Patch Options](images/header-patch-options.png)

Some patches are configurable. Expand a section to see its options.

<details>
<summary><b>Unlock Premium</b></summary>

| Option | Type | Default | Notes |
| :--- | :--- | :--- | :--- |
| Extra keys | Text | Empty | Comma separated SharedPreferences or DataStore keys to treat as premium. Leave empty for the built in list |

</details>

<details>
<summary><b>Free In-app Purchases</b></summary>

| Option | Type | Default | Notes |
| :--- | :--- | :--- | :--- |
| Native byte rules | Text | Empty | One rule per line as `FROM => TO` in hex. Use `??` for bytes that vary. Lines starting with `#` are ignored |
| Native libraries | Text | Empty | Comma separated `.so` paths. Leave empty to scan every library under `lib/` |

</details>

<details>
<summary><b>No Ads</b></summary>

| Option | Type | Default | Notes |
| :--- | :--- | :--- | :--- |
| Block Interstitials | Toggle | On | Full screen ads between content |
| Block Banners | Toggle | On | Top or bottom banner ads |
| Block App Open | Toggle | On | Ads shown on app start |
| Block MREC | Toggle | On | Medium rectangle banner ads |
| Block Rewarded | Toggle | Off | Leave off when using Ads Free Rewards, otherwise reward gates can break |
| Block Native | Toggle | On | Native ads blended into feeds and lists |

</details>

<details>
<summary><b>Ads Free Rewards</b></summary>

| Option | Type | Default | Notes |
| :--- | :--- | :--- | :--- |
| Reward multiplier | Text | 1 | How many times the reward callback runs per ad |

</details>

<details>
<summary><b>Pairip Bypass</b></summary>

Pairip Bypass has no options. It applies every strategy it can find and reports which ones matched in the patcher log.

</details>

---

<a name="tips"></a>
![Tips](images/header-tips.png)

- Start with a minimal set of patches, then add more only if needed. Smaller changes are easier to debug.
- If an app crashes after patching, disable the most recent patch you enabled and test again.
- Use `Reward multiplier` in Ads Free Rewards to speed up reward farming.
- Pairip Bypass helps when a patched app refuses to start on launch.
- For orientation locks, spoofs and manifest tweaks, add Nai's Extra Patches.

> [!TIP]
> Keep the patcher log open while testing. Patches that find nothing simply report a warning and move on, so a clean log with no errors usually means the app just did not contain that code path.

---

<a name="warnings"></a>
![Warnings](images/header-warnings.png)

> [!WARNING]
> Patches that hook app internals, such as Pairip Bypass, may not work on every app and can cause crashes or visual glitches.

> [!CAUTION]
> Modifying applications can violate the terms of service of the apps you patch. Use these patches only on apps you own, for personal and educational purposes. The authors are not responsible for bans, data loss or other consequences.

- Do not enable every patch at once. Over patching raises the chance of conflicts.
- Some anti cheat systems or strongly protected apps detect tampering regardless of these patches.
- Always back up your save data before installing a patched build.

---

<a name="compatibility"></a>
![Compatibility](images/header-compatibility.png)

- Targets Android apps and games packaged as APK or XAPK.
- Requires the Morphe patcher. The desktop build also needs a Java runtime.
- Works best on Unity, native, ad SDK and Cocos2d-js based apps.
- Patch behavior depends on the exact app build. A patch that works today may need an option tweak after an app update.
- Per-title patches are locked to the game or app version they were built for. A new version needs an updated patch.

> [!NOTE]
> Because each app is different, no single configuration fits all titles. Treat the patch list as a toolkit and tune it per app.

- **Compatibility reports (community):** the maintainer does not test every app. Search and contribute via [`COMPATIBILITY.md`](COMPATIBILITY.md) and the [Compatibility Report](.github/ISSUE_TEMPLATE/compatibility-report.yml) issue template ([label:compatibility](https://github.com/Nai64/Nai64Patches/issues?q=label%3Acompatibility) issues).
- **Troubleshooting:** see [`TROUBLESHOOTING.md`](TROUBLESHOOTING.md) for common install errors, save data preservation and Unity or Il2Cpp limits.

---

<a name="disclaimer"></a>
![Disclaimer](images/header-disclaimer.png)

Nai's Patches is provided as is, without warranty of any kind, for **educational and personal use only**.

- **You must legally own every app you patch.** These patches are for studying, accessibility tweaks, and personal customization of software you already own or that is free. They are not a piracy tool: do not use them to obtain paid apps, subscriptions, or content you have not licensed, and do not redistribute patched copies of anyone's apps.
- **No cracked apps are distributed here.** This repository contains only patch definitions (small bytecode recipes applied on your own device by the Morphe patcher). No APKs, no paid content, no activation keys, and no circumvention services are hosted or sold.
- **Non-commercial project.** There are no paid tiers, no ads, and nothing for sale. Do not sell these patches or patched apps, and be wary of anyone who does.
- **Use at your own risk.** Modifying apps can violate their terms of service and may lead to account restrictions, bans, data loss, or apps that no longer work. The authors accept no liability for any consequences of using these patches, and provide no guarantee they work on any particular app or version.
- **No affiliation.** This project is not affiliated with, endorsed by, or connected to Google, Morphe, or any app developer. All trademarks belong to their respective owners.
- **Abuse reports / takedown requests:** if you are a rights holder and believe specific content here infringes your rights, open an issue with the exact file or patch name and a description of the concern, and it will be reviewed promptly and in good faith.

---

<p align="center">
Made with the Morphe patcher. Contributions welcome.
</p>

<p align="center"><a href="#top">Back to top</a></p>
