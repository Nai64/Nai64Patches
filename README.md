# Nai's Patches

Universal Morphe patches for Android games using AppLovin MAX SDK.

## Add to Morphe

| Method | URL |
|--------|-----|
| Deep link | [morphe.software/add-source?github=Nai64/Nai64Patches](https://morphe.software/add-source?github=Nai64/Nai64Patches) |
| Manual | `https://github.com/Nai64/Nai64Patches` |

## Patches

| Patch | Description |
|-------|-------------|
| **No Ads** | Blocks all non-rewarded ads: interstitial, banner, app open, MREC. WARNING: Enabling Ads Free Rewards alongside this patch will cause it to NOT work (No Ads takes priority). |
| **Ads Free Rewards** | Auto-claim rewarded ad rewards without watching ads. WARNING: Enabling No Ads alongside this patch will prevent rewards from being claimed. |
| **Spoof Play Store Install Source** | Makes the app think it was installed from Google Play Store. Also neuters Pairip's Application entry point methods to prevent Pairip from loading its native library. Recommended if the app has a Play Store license check. |
| **Pairip Application Redirect** | Stronger Pairip bypass — changes AndroidManifest.xml to redirect Pairip's Application entry point to the real app's Application class, preventing Pairip from ever being instantiated. Auto-discovers the real app class from Pairip's DEX. |
