# Troubleshooting

> Start minimal, then add patches. Smaller changes are easier to debug. Keep the patcher log open — patches that find nothing warn and skip.

## Install errors

| You see | Why | Try |
|---|---|---|
| `App not installed` / `INSTALL_FAILED_UPDATE_INCOMPATIBLE` / Morphe asks to uninstall | Patched APK is re-signed with a different key than the installed app. Android only allows an upgrade when the key matches. | Enable `Preserve App Data` (and/or `Keep Data on Uninstall`) **before** patching, then uninstall the original and tick **Keep data** (Android 10+ dialog), then install the patched APK. Keep the same package name — don't use a rename patch. |
| `INSTALL_FAILED_VERSION_DOWNGRADE` | Patched build has lower `versionCode` than installed | Enable `Change Version Code` with a higher value, or uninstall first (with Keep Data). |
| `There was a problem parsing the package` | Corrupt APK / partial XAPK | Re-download the original APK, keep a copy before patching (see `README.md#how-to-use`). |
| Install succeeds but data lost | Uninstalled without keeping data, or side-by-side install with renamed package | Don't use `Rename App` if you need the save. Use `Preserve App Data` flow. If the game supports Google Play Games login, sync to cloud first — most reliable. |

## Patched app misbehaves

| Symptom | Why | Try |
|---|---|---|
| No effect at all | App is Unity Il2Cpp (`libil2cpp.so`) or server-checked. `bytecodePatch` only touches dex/resources, not native or server logic. | Expected for ~80% of games. Try only manifest/resource patches (`Preserve App Data`, `AMOLED Theme` on `values-night` dark themes, `Force Landscape`) or test a different app. Check `COMPATIBILITY.md`. |
| Ads still show | Ad SDK not matched by fingerprints, or native/webview ad flow | Try `No Ads` with all toggles + `Disable Telemetry` + `Remove Ad Services Entries`. Some ad paths require `Ads Free Rewards` with a specific option. |
| Black screen / missing banner / layout broken | Too many patches, or `AMOLED Theme` / `Transparent System Bars` conflict, or `Custom App Resolution` | Disable patches one by one. `AMOLED Theme` only touches dark themes (`values-night` or `Dark` themes) and leaves light themes alone; test without it first. |
| App detects root / won't start | Integrity / Play Integrity check | Try `Spoof Play Integrity` + `Disable Root Checks` + `Spoof Signature Match`. Strong anti-cheat may still block. |
| Crash on launch — check logcat | Patch inserted into missing method, or resource `ensureItem` hit unexpected theme | Paste the crash + patcher log in a Bug Report (`bug-report.yml`) and note which single patch triggers it. |
| `No dark themes found` from AMOLED Theme | App has no `values-night` and no `Dark` theme | Nothing to patch — app doesn't expose a dark theme via resources. No change is correct. |
| Translations gone / wrong language | `Strip Translations` removes `values-xx` folders | Disable that patch or keep only `Strip Translations` off for daily use. |

## General tips

- Test **unpatched first** — confirm the original APK works on the device.
- Enable **one patch at a time**. When it works, add the next.
- Use `Preserve App Data` whenever you need to replace an installed app and keep saves (see `COMPATIBILITY.md` for community reports).
- Keep the original APK. If a patched build misbehaves, fall back and try a different combination.
- Report what worked: open a **Compatibility Report** so others skip the trial/error.
