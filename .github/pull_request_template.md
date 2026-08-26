<!-- One logical change per PR — one new patch OR one enhancement. See CONTRIBUTING.md -->
## What does this PR do?

<!-- feat: add Foo patch / fix: handle Bar edge — one line per commit -->

## Checklist

- [ ] Single feature (one new patch *or* one enhancement, not 5 bundled)
- [ ] Conventional commits (`feat:`, `fix:`, `chore:`) — no `move`/`update`/`hi`
- [ ] Feature branch, not `main` (`git checkout -b feat/foo`)
- [ ] No generated files in diff: `patches-list.json`, `patches-bundle.json`, `CHANGELOG.md`, `gradle.properties` version, `.releaserc`, `gradlew`
- [ ] `.\gradlew.bat :patches:build` → `BUILD SUCCESSFUL`
- [ ] `.\gradlew.bat :patches:generatePatchesList` → literal `Select-String -SimpleMatch '"Your Patch Name"'` found, then `git checkout -- patches-list.json`

## Testing

<!-- APK package + patch options tested, Morphe log excerpt if relevant -->
