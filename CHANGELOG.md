## [1.9.1](https://github.com/Nai64/Nai64Patches/compare/1.9.0...1.9.1) (2026-07-04)

### Bug Fixes

* **SpoofPlayStore:** tighten fallback installer check fingerprints to require PRIVATE access ([5ef4d55](https://github.com/Nai64/Nai64Patches/commit/5ef4d554aac5c382cd74d2cbcd5d86acb05828d2))

## [1.9.0](https://github.com/Nai64/Nai64Patches/compare/1.8.1...1.9.0) (2026-07-03)

### Bug Fixes

* **AdsFreeRewards:** fix ironSource Unity bridge listener lifecycle for Pickcrafter ([6eb3543](https://github.com/Nai64/Nai64Patches/commit/6eb3543c3b1faa2f6850936bb48f6c9f0aab799c))
* disable auto-release on push, require manual dispatch only ([ade94be](https://github.com/Nai64/Nai64Patches/commit/ade94bebb024d1f30e8bf7b23fbfbf85a533e3f2))
* **SpoofPlayStore:** avoid Pairip VM dispatcher bypass ([fb9e823](https://github.com/Nai64/Nai64Patches/commit/fb9e823d4f7bd7c63bfedbd18d12fc981f8b9840))

### New Features

* **AdsFreeRewards:** add ironSource Unity bridge support ([41f3b72](https://github.com/Nai64/Nai64Patches/commit/41f3b72c6a2b77802a73b93c7d43de56fcf50d0a))
* **SpoofPlayStore:** add Pairip SignatureCheck bypass strategies ([2d05a52](https://github.com/Nai64/Nai64Patches/commit/2d05a5205209c843bfd35eb81cc8b0dd48216d85))

## [1.8.1](https://github.com/Nai64/Nai64Patches/compare/1.8.0...1.8.1) (2026-07-03)

### Bug Fixes

* **SpoofPlayStore:** prioritize Pairip VM skip over generic string matches to avoid false positives ([651333c](https://github.com/Nai64/Nai64Patches/commit/651333c7e5d89b08edbc067b246b76a11e70855b))

## [1.8.0](https://github.com/Nai64/Nai64Patches/compare/1.7.0...1.8.0) (2026-07-03)

### New Features

* **SpoofPlayStore:** add Pairip VM skip strategy for native-VM based apps ([d7f8525](https://github.com/Nai64/Nai64Patches/commit/d7f852537ee90bd9ea37a480cd1a0a2b1e775953))

## [1.7.0](https://github.com/Nai64/Nai64Patches/compare/1.6.0...1.7.0) (2026-07-03)

### Bug Fixes

* **AdsFreeRewards:** dont patch Unity Ads load(), only show() to avoid error 628 [skip ci] ([83dc4e0](https://github.com/Nai64/Nai64Patches/commit/83dc4e093cf0d14cd2eef8737b15bfa9cf03cb24))

### New Features

* **AdsFreeRewards:** add LevelPlay strategy with Unity Ads fallthrough ([4b02a66](https://github.com/Nai64/Nai64Patches/commit/4b02a666d13e9d99d8469589cb2631dcaee5553e))
* **SpoofPlayStore:** add fallback strategies for non-Pairip apps ([fd2a63d](https://github.com/Nai64/Nai64Patches/commit/fd2a63db5c5914bfc7bf969502fe3b2783462277))

## [1.6.0](https://github.com/Nai64/Nai64Patches/compare/1.5.9...1.6.0) (2026-07-02)

### New Features

* **AdsFreeRewards:** add Unity Ads RewardedAd support ([c026d48](https://github.com/Nai64/Nai64Patches/commit/c026d48fb87ee1c9c5e15dd057fa50cf5a983bda))
* **AdsFreeRewards:** add Unity Ads RewardedAd support [skip ci] ([dcf32a6](https://github.com/Nai64/Nai64Patches/commit/dcf32a628c2a5499719bfce1d2385e80e7381b3b))

## [1.5.9](https://github.com/Nai64/Nai64Patches/compare/1.5.8...1.5.9) (2026-07-02)

### Bug Fixes

* NoAds now blocks rewarded ads, overriding AdsFreeRewards when both enabled ([dc96979](https://github.com/Nai64/Nai64Patches/commit/dc9697918f670fd11639cbdb00ffda0389be066b))
* **NoAds:** also disable rewarded ads when No Ads is enabled [skip ci] ([cd822b2](https://github.com/Nai64/Nai64Patches/commit/cd822b287dc483742819984fac55068d6f7eca9c))

## [1.5.8](https://github.com/Nai64/Nai64Patches/compare/1.5.7...1.5.8) (2026-07-02)

### Bug Fixes

* **release:** clean build artifacts before release to avoid duplicating previous MPP versions ([0de1115](https://github.com/Nai64/Nai64Patches/commit/0de111595a8b9533525b7db50c86dadee3aba5d3))

## [1.5.7](https://github.com/Nai64/Nai64Patches/compare/1.5.6...1.5.7) (2026-07-01)

### Bug Fixes

* **AdsFreeRewards:** fix register corruption, add OnRewardedAdDisplayedEvent ([2fbdd05](https://github.com/Nai64/Nai64Patches/commit/2fbdd056013578849074c77dd3e2f795a0fe7430))

## [1.5.6](https://github.com/Nai64/Nai64Patches/compare/1.5.5...1.5.6) (2026-07-01)

### Bug Fixes

* update patch descriptions and documentation ([604233f](https://github.com/Nai64/Nai64Patches/commit/604233f97074fb24274f5a9bb372a07a46446c08))

## [1.5.5](https://github.com/Nai64/Nai64Patches/compare/1.5.4...1.5.5) (2026-07-01)

### Bug Fixes

* remove double semicolon in fireHiddenCallbacks smali template ([2f03299](https://github.com/Nai64/Nai64Patches/commit/2f03299f6e6b22d3dcc91d5cc82801e2222cc2d1))

## [1.5.4](https://github.com/Nai64/Nai64Patches/compare/1.5.3...1.5.4) (2026-07-01)

### Bug Fixes

* add fingerprint checks with early return and warning logs for universal patches ([1b605a7](https://github.com/Nai64/Nai64Patches/commit/1b605a7afe501f4b26b42556bed1e1bf9f6c3f3c))

## [1.5.3](https://github.com/Nai64/Nai64Patches/compare/1.5.2...1.5.3) (2026-07-01)
