# Contributing to Nai64Patches

Thanks for wanting to contribute — keep it small and reviewable and it will merge fast.

## One patch per PR

* **One logical change per PR.** Add one new patch or one enhancement to an existing patch. Please do not bundle five unrelated features (like Amazon spoof plus ClearSplit plus PairIP plus NoAds) into one PR. If one strategy has a false positive, the whole PR gets blocked.
* **One logical change per commit.** Use `feat: add Foo patch` or `fix: handle X edge`, not `move` or `update` or `hi`. Follow [Conventional Commits](https://www.conventionalcommits.org/) with `feat:`, `fix:`, `chore:`, `revert:`.

## Branch

* Fork the repo and create a **feature branch** like `feat/amazon-spoof` or `fix/pairip-attachbasecontext`. Never open a PR from `main`. Keeping `main` clean avoids clashes with the other agent and prevents `patches-bundle.json` version pollution.

## What not to edit

* **Generated files:** `patches-list.json`, `patches-bundle.json`, and `CHANGELOG.md`. These are produced by `:patches:generatePatchesList` and `semantic-release` on `main`. Keep the committed list stale while you work. After you run `generatePatchesList`, restore it with `git checkout -- patches-list.json`.
* **Build and infra:** `gradle.properties` `version`, `.releaserc`, and `gradlew`. Only the maintainer bumps the version through the `Release` workflow.

## Build & verify

```bat
.\gradlew.bat :patches:build
.\gradlew.bat :patches:generatePatchesList --console=plain -q
:: literal grep, no re-check with bash
Select-String -Path "patches-list.json" -SimpleMatch '"Your New Patch Name"'
git checkout -- patches-list.json
```

* `BUILD SUCCESSFUL` required. `No ... checks found. No changes applied.` warning is okay if the APK simply lacks that code path.

## Patch style

* Universal patches live in `patches/universal/<category>/YourPatch.kt`. Use `bytecodePatch` or `resourcePatch`, leave `compatibleWith` out for universal patches or scope it when needed, set `default = false`, and add `booleanOption` or `stringOption` when you need configurability. See `CustomStartupDialogPatch.kt:24` for a good example.
* Reuse the shared helpers in `patches/universal/misc/InvokeHelpers.kt` (`noOpVoidInvoke`, `foldBooleanReturns`) and `GetterSpoofer.kt`. Please do not copy and paste per-file `forceBooleanSetter` variants.
* For Universal Overlay modules, read [UNIVERSAL_OVERLAY_MODULES.md](UNIVERSAL_OVERLAY_MODULES.md) before adding or changing a module. It documents runtime boundaries, configuration wiring, lifecycle rules, and isolation requirements.
* For the complete Universal Overlay architecture and file map, read [UNIVERSAL_OVERLAY.md](UNIVERSAL_OVERLAY.md).

## PR checklist

- [ ] Single feature, single `feat:` commit per patch (or `fix:` per bug)
- [ ] Feature branch, not `main`
- [ ] No generated/infra files in diff (`git diff --name-only main...HEAD` should only show `patches/src/main/kotlin/...`)
- [ ] `BUILD SUCCESSFUL` + `generatePatchesList` literal grep shows name
- [ ] Description lists what was added/enhanced and why

If your PR bundles `chore: Release` commits or `gradle.properties` bumps, we will ask you to rebase and drop them. Releases are handled only by the maintainer through the `Release` workflow dispatch.
