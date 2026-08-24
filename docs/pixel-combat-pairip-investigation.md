# Pixel Combat / PairIP investigation notes

**Status:** Investigation only. No patch source was changed as part of this note.

This records the observed behavior while testing the existing **Pairip Bypass (Experimental)** patch against Pixel Combat package `com.gsgames.pixel.combat`, version `5.12.23`. Use only with apps and APKs you are authorized to inspect or modify.

## Existing patch behavior

Relevant implementation:

- `patches/src/main/kotlin/patches/universal/ads/PairipBypassPatch.kt`
- `patches/src/main/kotlin/patches/universal/ads/Fingerprints.kt`

The patch has two phases:

1. An internal resource patch attempts to discover an app-specific `Application` superclass and rewrite the manifest away from `com.pairip.application.Application`.
2. The public bytecode patch applies independent PairIP fingerprint strategies. `dependsOn(...)` guarantees the resource phase runs first; it does **not** require that discovery succeeds before bytecode strategies run.

## Morphe patching result

Morphe Manager `1.27.0` / patcher `1.10.0` processed Pixel Combat `5.12.23` with Nai's Patches `1.26.0`.

The patcher input was reported as a **single APK**:

```text
Patching started ... pkg=com.gsgames.pixel.combat version=5.12.23
input=.../com.gsgames.pixel.combat_5.12.23_original.apk
split=false
```

### Manifest redirect result

The application-class discovery did not succeed:

```text
Could not discover real app class. Skipping manifest redirect.
```

Therefore, no manifest application redirect was applied for this APK.

### Bytecode strategies that matched

Despite the redirect failure, 14 PairIP strategies matched and were applied:

```text
Applied Pairip performLocalInstallerCheck spoof
Applied Pairip SignatureCheck.verifyIntegrity bypass
Applied Pairip SignatureCheck.verifySignatureMatches bypass
Applied Pairip LicenseClient error dialog suppress
Applied Pairip LicenseClient paywall suppress
Applied Pairip LicenseActivity paywall suppress
Applied Pairip Application.attachBaseContext bypass
Applied Pairip LicenseClient.checkLicense root kill
Applied Pairip LicenseContentProvider.onCreate bypass
Applied Pairip LicenseContentProvider.query bypass
Applied Pairip LicenseResponseHelper.validateResponse bypass
Applied Pairip V2 checkLicenseInternal force-success
Applied Pairip V2 LicenseResponseHelper.verifySignature bypass
Applied Pairip V2 scheduleRepeatedLicenseCheck suppress
Pairip Bypass (Experimental) patch succeeded (14 strategy(s) applied)
```

Morphe then wrote modified Dex content successfully:

```text
Writing 6 new classes to new DEX files
Stripping 6 modified classes from original DEX files
Patching succeeded
```

This verifies that the bytecode changes were incorporated into the rebuilt APK. It does **not** establish that all runtime enforcement paths were covered.

## Runtime result for the patched APK

The patched APK starts Pixel Combat's `com.unity3d.player.UnityPlayerActivity`, then loads PairIP's native core very early:

```text
Load .../base.apk!/lib/arm64-v8a/libpairipcore.so ... (caller=.../base.apk!classes2.dex): ok
```

About 70 ms later, Android launches the Play Store using this URL:

```text
http://play.google.com/store/license/paywall?id=com.gsgames.pixel.combat
```

The Pixel Combat process then exits.

### Evidence-based conclusion

- The patch build completed without a patcher/Dex compilation failure.
- The manifest redirect did not run because class discovery failed.
- Existing Java/Dex strategies did match and modify six Dex classes.
- The observed Play Store handoff occurs immediately after PairIP's native core loads.
- This existing patch configuration does not prevent the enforcement path used by the tested app version.

The logs do not prove the precise decision point inside the native component or any service it contacts.

## Split APK finding

The Aurora Store download was identified by Universal Installer as a **split APK install**. Installing that complete set through Universal Installer launched successfully without the Play Store paywall.

By contrast, the "original APK" installed from Morphe's storage manager appeared as a single APK and returned to the launcher immediately. Its log still showed `libpairipcore.so` loading, but it did **not** open the Play Store URL before the process died.

This is not a valid successful-original control: a standalone/base-only installation can differ materially from a complete split package and may be missing configuration, feature, resource, or ABI APKs expected at startup.

### Verify each installed variant

After installing a variant, run:

```sh
adb shell pm path com.gsgames.pixel.combat
```

A complete split installation reports `base.apk` plus one or more `split_*.apk` paths. A standalone/base-only installation reports only `base.apk`.

Do not compare behavior across these layouts as though they were identical artifacts.

## Reproducible log capture

### Patch-time log

Start this before initiating the patch operation in Morphe:

```sh
adb logcat -c
adb logcat -v threadtime -b main -b system -b crash | grep --line-buffered -iE 'morphe|patch(er|ing)?|pairip|com\.gsgames\.pixel\.combat|Application Redirect|strategy|AndroidRuntime|FATAL EXCEPTION|warning|error|failed|integrity|installer|signature|license'
```

### Launch-time log

Start this before launching the installed app:

```sh
adb logcat -c
adb logcat -v threadtime -b main -b system -b crash | grep --line-buffered -iE 'com\.gsgames\.pixel\.combat|AndroidRuntime|FATAL EXCEPTION|pairip|license|integrity|installer|signature'
```

Avoid sharing account tokens, Android IDs, personally identifying file paths, or unrelated device data from logs.

## Current limitations

- The current `discoverPairipAppClass` superclass heuristic is best-effort; a manifest wrapper does not guarantee an app-specific superclass can be found that way.
- The application class declared in the manifest is normally canonical. When it is a framework/protection wrapper, an inferred "real" application class may not exist as a simple manifest-replaceable class.
- Native startup activity means Java/Dex patch match logs alone cannot establish full runtime coverage.

## Files relevant to future authorized maintenance

- `patches/src/main/kotlin/patches/universal/ads/PairipBypassPatch.kt`
- `patches/src/main/kotlin/patches/universal/ads/Fingerprints.kt`
- `docs/pixel-combat-pairip-investigation.md`
