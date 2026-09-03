# Nai64Patches Universal Overlay

## Purpose

Universal Overlay Patch is a runtime overlay for supported Android APKs. It is designed to work
without knowing the target app's package, engine, Activity names, or internal game logic. The same
architecture can run in ordinary Android apps, Unity games, Godot games, and apps that move between
multiple Activities.

The patch adds an in-app floating button and an optional menu. The menu is configured before
patching, while selected modules can be enabled, disabled, and customized at runtime. The overlay
belongs to the patched app's Activity content; it is not an Android system-level window.

## File map

### Patch and injection

patches/src/main/kotlin/patches/universal/universaloverlay/UniversalOverlayPatch.kt

This is the Morphe patch entry point. It:

- declares the patch name, description, and Morphe settings;
- validates user configuration;
- serializes configuration into a Base64-delimited payload;
- includes the extension DEX through extensions/extension.mpe;
- finds the real Application onCreate method when possible;
- injects a small bridge call using safe temporary registers;
- falls back to a suitable Activity onCreate method when an Application entry point is unavailable.

It should contain patch-time discovery and configuration only. Runtime UI and feature behavior belong
in the extension Java code.

patches/src/main/kotlin/patches/universal/ui/StartupHooks.kt

The overlay reuses StartupHooks for Application discovery, launcher Activity fallback information,
and safe Smali string escaping. This is a shared patch helper, not an overlay-specific runtime.

The overlay does not require target-specific Fingerprints. It discovers framework relationships and
uses the platform Application or Activity lifecycle so it remains universal.

## Structure at a glance

The patch is split into a small patch-time layer and a larger runtime extension layer:

```text
Nai64Patches
|
|-- patches/src/main/kotlin/patches/universal/universaloverlay/
|   `-- UniversalOverlayPatch.kt       Morphe settings and safe injection bridge
|
|-- patches/src/main/kotlin/patches/universal/ui/
|   `-- StartupHooks.kt                Application and Activity discovery helpers
|
`-- extensions/extension/src/main/java/nai64/universaloverlay/
    |-- UniversalOverlayRuntime.java   Runtime coordinator and Activity controllers
    |-- UniversalOverlayLifecycle.java Lifecycle callback adapter
    |-- UniversalOverlayConfig.java    Configuration decoder and fallbacks
    |-- UniversalOverlayViews.java     Shared view and style construction
    `-- modules/
        |-- UniversalOverlayModule.java          common contract
        |-- UniversalOverlayActivityModule.java  activity base class
        |-- UniversalOverlayStatisticModule.java statistic base class
        |-- UniversalOverlayHookModule.java      hook base class
        |-- activity/                            Activity implementations
        |-- statistic/                           statistic implementations
        `-- hook/                                hook implementations
```

Module inheritance is intentionally separated by responsibility:

```text
UniversalOverlayModule
|                         common identity and metadata
|-- UniversalOverlayActivityModule
|   `-- Activity modules    Activity state, apply, restore, isolation
|
|-- UniversalOverlayStatisticModule
|   `-- Statistic modules  values, monitors, scheduling, isolation
|
`-- UniversalOverlayHookModule
    `-- Hook modules       best-effort hooks and hook isolation
```

The runtime coordinates all three branches. Activity, statistic, and hook base classes do not
inherit from one another, so adding a module to one category does not couple it to another
category's lifecycle or failure behavior.

### Runtime entry and configuration

extensions/extension/src/main/java/nai64/universaloverlay/UniversalOverlayRuntime.java

This is the runtime coordinator. It:

- starts from the injected Application or Activity bridge;
- registers ActivityLifecycleCallbacks;
- creates one controller for each resumed Activity;
- attaches the overlay to Activity content;
- keeps the button and menu above the Activity content;
- installs and removes statistic monitors;
- coordinates module state, lifecycle state, and restoration;
- isolates failures so an unsupported feature does not crash the host app;
- fully closes and unregisters the overlay across all Activities.

The controller is Activity-specific. Shared state is used only for intentional cross-Activity
settings such as module toggles, monitor toggles, button position, and temporary Activity feature
state.

extensions/extension/src/main/java/nai64/universaloverlay/UniversalOverlayLifecycle.java

This is the lifecycle adapter. It forwards Activity resume, pause, and destroy events to the runtime.
The runtime creates a controller on resume, pauses work when an Activity is not visible, and removes
the controller when the Activity is destroyed.

extensions/extension/src/main/java/nai64/universaloverlay/UniversalOverlayConfig.java

This decodes and sanitizes the payload produced by the Kotlin patch. Configuration fields are
positional, so new fields are appended and version-aware defaults preserve older payloads.
Malformed colors, enums, dimensions, and missing values receive safe fallbacks.

### Shared view construction

extensions/extension/src/main/java/nai64/universaloverlay/UniversalOverlayViews.java

This contains reusable overlay backgrounds, gradient rendering, outline rendering, and selectable
button backgrounds. It keeps visual construction separate from lifecycle and module behavior.

Overlay views use an isolated Android theme context and then apply configured colors explicitly. This
prevents a host app's legacy or custom theme from changing checkbox, spinner, slider, or overlay
button appearance.

## Module categories

The module base classes are placed in:

extensions/extension/src/main/java/nai64/universaloverlay/modules/

Concrete implementations are placed in the matching activity/, statistic/, or hook/ subdirectory.

UniversalOverlayModule.java is the common module contract. It provides the stable identity and
user-facing metadata shared by all categories.

### Activity modules

UniversalOverlayActivityModule.java is the base class for temporary behavior tied to the current
Activity or its Window. Examples include fullscreen, screenshots, brightness, rotation, and audio
state.

The base class owns Activity-module isolation and restoration boundaries. Each module captures the
state it changes, applies only its own behavior, and restores independently. A failure in one
Activity module must not stop other modules from being toggled or restored.

### Statistic modules

UniversalOverlayStatisticModule.java is the base class for values displayed in the menu and/or
floating monitors. It manages enabled state, Monitor state, menu visibility, monitor binding, and
safe update scheduling.

Statistic modules should avoid unnecessary work:

- menu values update only while the menu is open;
- floating monitors update only while the menu is closed;
- monitor sampling runs only when both Monitor and Active are enabled;
- static or rarely changing values should be cached;
- timers and polling must stop when the module or overlay is inactive.

The runtime owns monitor placement, columns, dimensions, alpha, and click-through behavior.

### Hook modules

UniversalOverlayHookModule.java is the base class for best-effort runtime hooks that affect views or
host behavior without changing a specific app's internal business logic.

The base class owns hook isolation. Hooks must tolerate unsupported APIs, unexpected view types, and
dynamic host behavior. Hook failures are contained so they do not propagate to the app or prevent
other module categories from operating.

## Runtime flow

1. Morphe builds the patch with the selected settings.
2. The patch injects a bridge into Application.onCreate, or uses an Activity fallback.
3. UniversalOverlayRuntime decodes the configuration and registers lifecycle callbacks.
4. Each resumed Activity receives its own controller and overlay content.
5. The controller creates only the selected module categories.
6. Users control module state from the menu; state can be remembered across Activities for the
   current process.
7. Menu and monitor updates follow their separate visibility rules.
8. Pause stops visible-work updates, destruction removes the controller, and full close removes all
   controllers and unregisters the runtime.

If content attachment is unavailable for an unusual Activity, setup failure is caught and the host
app continues without the overlay. The implementation does not create a system-level overlay.

## Adding future modules

For the practical module checklist, read UNIVERSAL_OVERLAY_MODULES.md. In short, a new module needs:

1. A class extending the correct category base class.
2. A disabled-by-default Morphe option.
3. A configuration token serialized by UniversalOverlayPatch.kt.
4. A decoder field in UniversalOverlayConfig.java.
5. Registration in UniversalOverlayRuntime.java in the intended category/order.
6. Independent failure handling and restoration where state is changed.
7. A description update only when the user-facing patch overview needs a new representative example.

Do not add package-specific assumptions, do not retain destroyed Activities in static fields, and do
not move runtime feature logic into generated Smali.

## Maintenance and release notes

The extension DEX is part of the patch architecture, so changes commonly involve both the Kotlin
patch and Java runtime files. The release workflow builds the patch; local patch building is not
required for the normal development workflow used by this project.

Before submitting changes:

- inspect all related files for configuration and lifecycle rewiring;
- preserve older configuration decoding when adding fields;
- run git diff --check;
- test at least one ordinary app, one game, and one multi-Activity app when practical;
- use a semantic feat:, fix:, or chore: commit so semantic release can recognize it.

The idea and initial works of Universal Overlay Patch are from Zanuaimi / Noobite.
