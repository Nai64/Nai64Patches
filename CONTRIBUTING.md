# Contributing to Nai64Patches

Thanks for wanting to contribute — keep it small and reviewable and it will merge fast.

## One patch per PR

* **One logical change per PR** — one new patch *or* one enhancement to an existing patch. Do **not** bundle 5 unrelated features (e.g. Amazon spoof + ClearSplit + PairIP + NoAds) into one PR — it blocks the whole PR if one strategy has a false positive.
* **One logical change per commit** (`feat: add Foo patch`, `fix: handle X edge`, not `move`/`update`/`hi`). Use [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `chore:`, `revert:`).

## Branch

* Fork → create a **feature branch** (`feat/amazon-spoof`, `fix/pairip-attachbasecontext`) — **never PR from `main`**. `main` must stay clean or the other agent on `main` will clash and pollute `patches-bundle.json` versions.

## What not to edit

* **Generated:** `patches-list.json`, `patches-bundle.json`, `CHANGELOG.md` — produced by `:patches:generatePatchesList` + `semantic-release` on `main`. Keep the committed list **stale** during dev (`git checkout -- patches-list.json` after `generatePatchesList`).
* **Build/infra:** `gradle.properties` `version`, `.releaserc`, `gradlew` — only maintainer bumps version via `Release` workflow.

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

* Universal patches: `patches/universal/<category>/YourPatch.kt` with `bytecodePatch` or `resourcePatch`, `compatibleWith` omitted (universal) or scoped, `default = false`, `booleanOption`/`stringOption` for configurability (see `CustomStartupDialogPatch.kt:24`).
* Reuse helpers in `patches/universal/misc/InvokeHelpers.kt` (`noOpVoidInvoke`, `foldBooleanReturns`) and `GetterSpoofer.kt` — don’t copy-paste `forceBooleanSetter` per-file variants.

## PR checklist

- [ ] Single feature, single `feat:` commit per patch (or `fix:` per bug)
- [ ] Feature branch, not `main`
- [ ] No generated/infra files in diff (`git diff --name-only main...HEAD` should only show `patches/src/main/kotlin/...`)
- [ ] `BUILD SUCCESSFUL` + `generatePatchesList` literal grep shows name
- [ ] Description lists what was added/enhanced and why

PRs that bundle `chore: Release` commits or `gradle.properties` bumps will be asked to rebase and drop them — releases are done only by maintainer via `Release` workflow dispatch.
