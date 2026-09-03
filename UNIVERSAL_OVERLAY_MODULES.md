# Universal Overlay module guide

This guide explains how to add a module to Nai64Patches Universal Overlay.

## Architecture

The patch file at patches/src/main/kotlin/patches/universal/universaloverlay/UniversalOverlayPatch.kt
exposes Morphe settings and serializes them into the runtime configuration. It should not contain
app-specific logic.

The extension runtime starts from UniversalOverlayRuntime and owns lifecycle registration,
Activity controllers, view attachment, state restoration, update scheduling, and failure isolation.
It is shared by ordinary Android apps, Unity games, Godot games, and apps with multiple Activities.

Use the matching base class:

- UniversalOverlayActivityModule for temporary Activity/window behavior.
- UniversalOverlayStatisticModule for menu values and floating monitors.
- UniversalOverlayHookModule for best-effort view or runtime behavior changes.
- UniversalOverlayModule for common labels, keys, descriptions, and contracts.

Do not make one module inherit from another module category. Each category owns its own lifecycle and
isolation behavior.

## Adding a module

1. Add a Java class under the matching category directory:
   extensions/extension/src/main/java/nai64/universaloverlay/modules/activity/,
   .../statistic/, or .../hook/.
2. Extend the correct base class and provide a stable, unique key(), user-facing label(), and
   short description().
3. Add a disabled-by-default booleanOption in the Kotlin patch settings, grouped under
   Statistic modules, Activity modules, or Hook modules.
4. Add the option token to the serialized controls list in the Kotlin patch.
5. Decode the token in UniversalOverlayConfig.
6. Add the module in UniversalOverlayRuntime.addModules() in the intended display order.
7. Use the category-specific runtime helper: statistic modules use
   addStatisticSafely() and createStatisticMonitors(); activity modules use
   addActivitySafely(); hook modules use addHookSafely().
8. Restore mutable state independently and tolerate incompatible Activities.
9. Update the Morphe patch description when the user-visible module list changes.

## Statistic modules

Menu values update only while the menu is visible. Floating monitors update only while the menu is
closed and only when both Monitor and Active are enabled. A monitor must be click-through and
must not expand the overlay touch area.

Cache values that do not change during a session, such as device and CPU names. Avoid creating
timers or polling loops when a module is disabled, its monitor is hidden, the menu is closed, or the
overlay is fully closed.

Modules with multiple monitors must return a stable count and bind each view in a consistent order.
The runtime owns placement, columns, panel sizing, alpha, and visibility.

## Activity and hook modules

Activity modules capture original state before changing it and restore only their own state. Hook
modules handle unsupported APIs and unexpected view types without throwing into the host application.
A failure in one module must not prevent other modules from being created, toggled, or restored.

Never retain an Activity in a static field. The runtime stores controllers in a weak map and removes
them when Activities are destroyed. Do not attach system-level windows; the overlay belongs to the
current app content and uses Activity-content fallbacks.

## Configuration compatibility

Configuration fields are positional and Base64 encoded. When adding fields:

- increment CONFIG_VERSION in the Kotlin patch;
- append new fields rather than inserting them in the middle;
- add version-aware defaults in UniversalOverlayConfig;
- preserve decoding for older versions;
- validate bounded numeric values and enum strings before serialization.

Keep the runtime fallback safe when a setting is missing or malformed.

## Review checklist

- No target-package, class-name, engine, or APK-specific assumptions.
- No local patch build is required for normal development; the release workflow builds the patch.
- git diff --check passes.
- New UI uses the overlay context and configured colors, not the host Activity theme.
- Lifecycle, recreation, fully-close, and multi-Activity behavior remain isolated.
- Use a semantic commit such as feat: add ..., fix: handle ..., or chore: ....

The idea and initial works of Universal Overlay Patch are from Zanuaimi / Noobite.
