# Compatibility

This project does not maintain a hand-curated list of supported apps. The maintainer does not have time to test hundreds of apps and games — the compatibility database is **built by the community**.

## How to use

1. Search existing reports before testing: [issues with `compatibility` label](https://github.com/Nai64/Nai64Patches/issues?q=label%3Acompatibility).
2. Test with a minimal set of patches first (see `TROUBLESHOOTING.md`), then add more.
3. Share your result via the **Compatibility Report** issue template.

## How to report

Use **New Issue → Compatibility Report**:

- **App / Game name, Package (`com.*`), Version** — exact build you tested
- **Patches tested** — list names exactly as shown in Morphe (e.g. `No Ads`, `AMOLED Theme`)
- **Result** — `Working` / `Partial` / `Not working` / `Needs options tweak`
- **Android / Device / ROM** — helps others on the same setup
- **Notes** — Unity vs native, online vs offline, whether `Preserve App Data` kept your save, log snippet if it crashed

One report per app version. If an app update changes behavior, open a new report.

## What to expect

- **~1 in 5 games working is normal.** Most mobile games are Unity Il2Cpp (`libil2cpp.so` native). Morphe `bytecodePatch`/`resourcePatch` only touches dex/resources, so it cannot reach Unity game logic. Only Java/Kotlin parts (ads SDKs, manifest, some UI) are patchable.
- **Server-checked features** (cooldowns, IAP, time skips) are validated server-side — no client patch can bypass them reliably and you risk a ban.
- **Results vary by build.** A patch that works today may need an option tweak after an app update.

## Finding reports

- Filter by package: `label:compatibility com.example.app` in the issue search.
- Subscribe to an issue for updates on that app.

> Keep reports factual — `app X 1.2.3 + No Ads = working` is more useful than `please support app X`.
