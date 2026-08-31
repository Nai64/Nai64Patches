package patches.universal.ui

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.intOption
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import java.util.logging.Logger

/**
 * Runtime overlay architecture
 *
 * This file is a bytecode generator, not an Android UI class. The Kotlin code runs in Morphe
 * while patching; the strings built below become Smali instructions inside the target APK.
 * That distinction explains the deliberately explicit registers, labels, method signatures, and
 * returns throughout the file. The generated overlay itself uses only platform Android Views so
 * it can be attached to Activities from different app and game engines.
 */

// Generated field names are kept in one namespace to avoid collisions with host-app members.
private const val OVERLAY_BUTTON = "nai64RuntimeOverlayButton"
private const val OVERLAY_BUTTON_FIELD = "Landroid/view/View;"
private const val ORIGINAL_WINDOW_FLAGS = "nai64OriginalWindowFlags"
private const val ORIGINAL_SYSTEM_UI = "nai64OriginalSystemUi"
private const val KEEP_SCREEN_AWAKE_STATE = "nai64KeepScreenAwakeState"
private const val FULLSCREEN_STATE = "nai64FullscreenState"
private const val ALLOW_SCREENSHOTS_STATE = "nai64AllowScreenshotsState"
private const val TOUCH_START_X = "nai64OverlayTouchStartX"
private const val TOUCH_START_Y = "nai64OverlayTouchStartY"
private const val BUTTON_START_X = "nai64OverlayButtonStartX"
private const val BUTTON_START_Y = "nai64OverlayButtonStartY"
private const val CLOSE_CONFIRMATION = "nai64OverlayCloseConfirmation"
private const val TOUCH_DRAGGED = "nai64OverlayTouchDragged"
private const val DEFAULT_DESCRIPTION =
    "Welcome to Nai64Patches Runtime Controls Overlay. This experimental in-app overlay " +
        "contains controls that may change parts of the app or game at runtime. More may be " +
        "added in future updates."

// Configuration parsing happens at patch time. Invalid user input falls back before any Smali is
// generated, which keeps malformed strings, colors, sizes, and gravity values out of the APK.
private fun parseColor(value: String, fallback: Int): Int {
    val normalized = value.trim().removePrefix("#")
    val hex = when (normalized.length) {
        6 -> "FF$normalized"
        8 -> normalized
        else -> return fallback
    }
    return hex.toLongOrNull(16)?.toInt() ?: fallback
}

private fun floatLiteral(value: Float): String = "0x${value.toBits().toUInt().toString(16)}"

private fun activityDescriptor(value: String): String {
    val trimmed = value.trim()
    return if (trimmed.startsWith("L") && trimmed.endsWith(";")) {
        trimmed
    } else {
        "L${trimmed.replace('.', '/')};"
    }
}

private fun validateOverlayTemplateInputs(
    title: String,
    description: String,
    repositoryLabel: String,
    repositoryUrl: String,
    buttonSizeDp: Int,
    buttonGravity: Int,
) {
    check(title.isNotBlank())
    check(description.isNotBlank())
    check(repositoryLabel.isNotBlank())
    check(repositoryUrl.startsWith("http://") || repositoryUrl.startsWith("https://"))
    check(buttonSizeDp in 32..128)
    check(buttonGravity in setOf(0x33, 0x31, 0x35, 0x13, 0x15, 0x53, 0x51, 0x55))
    check(listOf(title, description, repositoryLabel, repositoryUrl).none { it.contains('\n') })
}

@Suppress("unused")
val runtimeControlsOverlayPatch = bytecodePatch(
    name = "Runtime Controls Overlay (Experimental)",
    description =
        "Experimental in-app floating runtime controls overlay. Morphe Manager can enable the " +
            "keep screen awake, fullscreen, and allow screenshots controls; each is disabled by " +
            "default and initially preserves the app's original behavior. The overlay also " +
            "provides a configurable repository link, drag-and-drop positioning, a hide menu " +
            "action, and a fully close action. Customize the overlay title, description, " +
            "repository label and URL, background and outline colors, button text and colors, " +
            "button shape, size, initial position, and idle opacity. The button fades fully " +
            "transparent while being dragged or after it is pressed. Leave the Activity name override empty for " +
            "automatic selection, or specify an Activity from AndroidManifest.xml as a fallback " +
            "when automatic injection targets the wrong screen.",
    default = false,
) {
    // The resource dependency resolves manifest information before this bytecode patch runs.
    // StartupHooks supplies the launcher, noHistory activities, and application metadata used by
    // the universal Activity selector below.
    dependsOn(StartupHooks.resolveRealApplicationPatch)

    // These options are resolved once while building the patch. Their values are embedded as
    // constants in generated Smali; they are not runtime preferences inside the patched app.
    val title by stringOption(
        title = "Overlay title",
        default = "Nai64Patches Runtime Controls Overlay",
        key = "runtimeOverlayTitle",
        description = "Title shown in the overlay menu.",
    )
    val descriptionText by stringOption(
        title = "Overlay description",
        default = DEFAULT_DESCRIPTION,
        key = "runtimeOverlayDescription",
        description = "Welcome and information shown below the overlay title.",
    )
    val repositoryText by stringOption(
        title = "Repository button text",
        default = "Nai64 repository",
        key = "runtimeOverlayRepositoryText",
        description = "Text of the button that opens the configured website.",
    )
    val repositoryUrl by stringOption(
        title = "Repository button URL",
        default = "https://github.com/Nai64/Nai64Patches",
        key = "runtimeOverlayRepositoryUrl",
        description = "Website opened by the repository button.",
    )
    val backgroundColor by stringOption(
        title = "Overlay background color",
        default = "#CC101820",
        key = "runtimeOverlayBackgroundColor",
        description = "Overlay background color as #RRGGBB or #AARRGGBB.",
    )
    val outlineColor by stringOption(
        title = "Overlay outline color",
        default = "#FF55D6BE",
        key = "runtimeOverlayOutlineColor",
        description = "Overlay outline color as #RRGGBB or #AARRGGBB.",
    )
    val buttonText by stringOption(
        title = "Overlay button text",
        default = "N",
        key = "runtimeOverlayButtonText",
        description = "Text shown inside the overlay button. Maximum three characters.",
    )
    val buttonTextColor by stringOption(
        title = "Overlay button text color",
        default = "#FF000000",
        key = "runtimeOverlayButtonTextColor",
        description = "Button text color as #RRGGBB or #AARRGGBB.",
    )
    val buttonBackgroundColor by stringOption(
        title = "Overlay button background color",
        default = "#FFFFFFFF",
        key = "runtimeOverlayButtonBackgroundColor",
        description = "Button background color as #RRGGBB or #AARRGGBB.",
    )
    val buttonShape by stringOption(
        title = "Overlay button shape",
        default = "circle",
        key = "runtimeOverlayButtonShape",
        description = "Shape of the overlay button.",
        values = linkedMapOf(
            "Circle" to "circle",
            "Squircle" to "squircle",
            "Square" to "square",
        ),
    )
    val buttonSizeDp by intOption(
        title = "Overlay button size (dp)",
        default = 56,
        key = "runtimeOverlayButtonSizeDp",
        description = "Button width and height in density-independent pixels. Recommended: 48-72.",
    )
    val buttonIdleOpacityPercent by intOption(
        title = "Overlay button idle opacity (%)",
        default = 35,
        key = "runtimeOverlayButtonIdleOpacityPercent",
        description =
            "Opacity of the overlay button when idle, from 10 to 100 percent. Default: 35 " +
                "percent. The button becomes fully transparent while being dragged and fades " +
                "to transparent when pressed to open the menu.",
    )
    val buttonPosition by stringOption(
        title = "Overlay button position",
        default = "topRight",
        key = "runtimeOverlayButtonPosition",
        description = "Initial position relative to the phone display.",
        values = linkedMapOf(
            "Top left" to "topLeft",
            "Top middle" to "topMiddle",
            "Top right" to "topRight",
            "Center left" to "centerLeft",
            "Center right" to "centerRight",
            "Bottom left" to "bottomLeft",
            "Bottom middle" to "bottomMiddle",
            "Bottom right" to "bottomRight",
        ),
    )
    val activityNameOverride by stringOption(
        title = "Overlay Activity name override",
        default = "",
        key = "runtimeOverlayActivityNameOverride",
        description =
            "Optional fallback target for the overlay. Leave empty for automatic Activity " +
                "selection. If the overlay is injected into the wrong screen, enter the target " +
                "Activity's fully qualified name from AndroidManifest.xml, for example " +
                "com.example.MainActivity. Smali descriptors such as " +
                "Lcom/example/MainActivity; are also accepted. Do not enter a method name. " +
                "The Activity must be present in classes.dex and extend android.app.Activity.",
    )
    val includeKeepScreenAwake by booleanOption(
        title = "Include keep screen awake control",
        default = false,
        key = "runtimeOverlayIncludeKeepScreenAwakeV2",
        description =
            "Include the APK hook and overlay switch for keeping the screen awake. Default " +
                "runtime state: Off, matching original app behavior.",
    )
    val includeFullscreen by booleanOption(
        title = "Include fullscreen control",
        default = false,
        key = "runtimeOverlayIncludeFullscreenV2",
        description =
            "Include the APK hook and overlay switch for fullscreen mode. Default runtime state: " +
                "Off, matching original app behavior.",
    )
    val includeScreenshots by booleanOption(
        title = "Include allow screenshots control",
        default = false,
        key = "runtimeOverlayIncludeScreenshotsV2",
        description =
            "Include the APK hook and overlay switch for allowing screenshots. Default runtime " +
                "state: Off, preserving the app's original screenshot behavior.",
    )
    execute {
        // Normalize all options once so the generator below can assume valid, non-null values.
        val logger = Logger.getLogger(this::class.java.name)
        val titleText = title.orEmpty().ifBlank { "Nai64Patches Runtime Controls Overlay" }
        val descriptionValue = descriptionText.orEmpty().ifBlank { DEFAULT_DESCRIPTION }
        val repositoryLabel = repositoryText.orEmpty().ifBlank { "Nai64 repository" }
        val repository = repositoryUrl.orEmpty().ifBlank { "https://github.com/Nai64/Nai64Patches" }
        val background = parseColor(backgroundColor.orEmpty(), 0xCC101820.toInt())
        val outline = parseColor(outlineColor.orEmpty(), 0xFF55D6BE.toInt())
        val label = buttonText.orEmpty().trim().take(3).ifBlank { "N" }
        val labelColor = parseColor(buttonTextColor.orEmpty(), 0xFF000000.toInt())
        val buttonBackground = parseColor(buttonBackgroundColor.orEmpty(), 0xFFFFFFFF.toInt())
        val shape = parseButtonShape(buttonShape.orEmpty())
        val buttonSize = (buttonSizeDp ?: 56).coerceIn(32, 128)
        val buttonIdleOpacity = ((buttonIdleOpacityPercent ?: 35).coerceIn(10, 100)) / 100f
        val buttonGravity = parseButtonGravity(buttonPosition.orEmpty())
        val activityOverride = activityNameOverride.orEmpty().trim()
        val selectedControls = listOfNotNull(
            "keep screen awake".takeIf { includeKeepScreenAwake == true },
            "fullscreen".takeIf { includeFullscreen == true },
            "allow screenshots".takeIf { includeScreenshots == true },
        )
        validateOverlayTemplateInputs(
            titleText,
            descriptionValue,
            repositoryLabel,
            repository,
            buttonSize,
            buttonGravity,
        )
        check(label.length <= 3)
        var patched = 0
        val superMap = mutableMapOf<String, String>()
        classDefForEach { classDef -> classDef.superclass?.let { superMap[classDef.type] = it } }
        fun isActivity(type: String, seen: MutableSet<String> = mutableSetOf()): Boolean {
            if (type == "Landroid/app/Activity;") return true
            if (type == "Ljava/lang/Object;" || type in seen) return false
            seen.add(type)
            return superMap[type]?.let { isActivity(it, seen) } == true
        }

        // Build the Activity candidate set from class inheritance rather than package names or
        // framework-specific classes. This is the part that gives the patch cross-engine reach.
        val candidates = mutableListOf<MutableClass>()
        classDefForEach { classDef ->
            if (!isActivity(classDef.type)) return@classDefForEach
            candidates.add(mutableClassDefBy(classDef))
        }

        val launcher = StartupHooks.resolvedLauncherActivityDescriptor
        val noHistoryActivities = StartupHooks.resolvedNoHistoryActivityDescriptors
        fun transientLaunchActivity(activity: MutableClass): Boolean {
            val onCreate = activity.methods.firstOrNull {
                it.name == "onCreate" && it.parameterTypes == listOf("Landroid/os/Bundle;")
            } ?: return false
            val instructions = onCreate.implementation?.instructions ?: return false
            var startsActivity = false
            var finishes = false
            for (instruction in instructions) {
                val text = instruction.toString()
                startsActivity = startsActivity || "startActivity" in text
                finishes = finishes || "->finish(" in text
            }
            return startsActivity && finishes
        }
        fun activityScore(activity: MutableClass): Int {
            var score = 0
            if (activity.type == launcher) score += 1000
            if (activity.type in noHistoryActivities) score -= 1500
            if (activity.type == launcher && transientLaunchActivity(activity)) score -= 1500
            if (activity.methods.any { AccessFlags.NATIVE.isSet(it.accessFlags) }) score += 500
            val onCreateSize = activity.methods.firstOrNull {
                it.name == "onCreate" && it.parameterTypes == listOf("Landroid/os/Bundle;")
            }?.implementation?.instructions?.count() ?: 0
            score += onCreateSize.coerceAtMost(200)
            if (activity.methods.any {
                    it.name == "onWindowFocusChanged" && it.returnType == "V" &&
                        it.parameterTypes == listOf("Z")
                }) score += 10
            return score
        }
        val requestedActivity = activityOverride.takeIf { it.isNotEmpty() }?.let(::activityDescriptor)
        // Prefer an explicit override when supplied; otherwise score likely visible Activities.
        // A diagnostic list is logged because automatic Activity selection is otherwise opaque
        // when an app has splash, deep-link, proxy, and gameplay Activities.
        val activity = requestedActivity?.let { descriptor ->
            candidates.firstOrNull { it.type == descriptor }
        } ?: candidates.maxByOrNull(::activityScore)
        logger.info(
            "Runtime overlay Activity candidates: " +
                candidates.joinToString { "${it.type}(score=${activityScore(it)})" },
        )
        if (requestedActivity != null && activity?.type != requestedActivity) {
            logger.warning(
                "Overlay Activity override $activityOverride was not found or is not an Activity. " +
                    "Using automatic Activity selection.",
            )
        }
        if (activity != null) {
            // Never add a second generated helper or reuse a field with an incompatible type.
            // A duplicate method/field can make dex merging fail or leave the verifier with an
            // ambiguous definition, so this target is skipped instead.
            if (activity.methods.any { it.name == "nai64CreateRuntimeOverlay" }) {
                logger.warning("${activity.type} already contains the overlay helper. No changes applied.")
                return@execute
            }
            val existingButtonField = activity.fields.firstOrNull { it.name == OVERLAY_BUTTON }
            if (existingButtonField != null && existingButtonField.type != OVERLAY_BUTTON_FIELD) {
                logger.warning("${activity.type} has an incompatible overlay button field. No changes applied.")
                return@execute
            }
            val focusMethod = activity.methods.firstOrNull {
                it.name == "onWindowFocusChanged" && it.returnType == "V" &&
                    it.parameterTypes == listOf("Z")
            } ?: newMethod(
                activity = activity,
                name = "onWindowFocusChanged",
                parameterTypes = listOf("Z"),
                returnType = "V",
                registers = 2,
            ).also { method ->
                val superclass = activity.superclass
                    ?: error("Activity ${activity.type} has no superclass")
                method.addInstructions(
                    0,
                    compactSmali("""
                        invoke-super {p0, p1}, $superclass->onWindowFocusChanged(Z)V
                        return-void
                    """),
                )
                activity.methods.add(method)
            }

            val onWindowFocusChanged = focusMethod
            if (activity.methods.any {
                    it.name == "onClick" && it.parameterTypes == listOf("Landroid/view/View;")
                } || activity.methods.any {
                    it.name == "onClick" && it.parameterTypes == listOf(
                        "Landroid/content/DialogInterface;",
                        "I",
                    )
                } || activity.methods.any {
                    it.name == "onTouch" && it.returnType == "Z" && it.parameterTypes == listOf(
                        "Landroid/view/View;",
                        "Landroid/view/MotionEvent;",
                    )
                }) {
                logger.warning("Overlay target already uses a required listener callback. No changes applied.")
                return@execute
            }
            addOverlayField(activity)
            addOverlayListeners(
                activity,
                titleText,
                descriptionValue,
                repositoryLabel,
                repository,
                background,
                outline,
                label,
                labelColor,
                buttonBackground,
                shape,
                includeKeepScreenAwake == true,
                includeFullscreen == true,
                includeScreenshots == true,
                buttonIdleOpacity,
            )
            injectOverlay(
                onWindowFocusChanged,
                activity,
                outline,
                label,
                labelColor,
                buttonBackground,
                shape,
                buttonSize,
                buttonGravity,
                includeKeepScreenAwake == true,
                includeFullscreen == true,
                includeScreenshots == true,
                buttonIdleOpacity,
            )
            patched = 1
        }

        if (patched > 0) {
            logger.info(
                "Injected experimental runtime controls overlay into $patched activit(ies); " +
                    "selected controls: ${selectedControls.joinToString().ifEmpty { "none" }}",
            )
            // TODO(device-test): exercise the generated APK on Android API levels and host
            // frameworks that cannot be represented by a JVM-only patch build.
        } else {
            logger.warning("No compatible Activity window focus methods found. No changes applied.")
        }
    }
}

private fun addOverlayField(activity: MutableClass) {
    // State is stored on the selected Activity so listeners can access it through p0 without a
    // separate singleton or application-global object. The nullable button field is also the
    // creation guard used by the focus callback.
    val fields = listOf(
        OVERLAY_BUTTON to OVERLAY_BUTTON_FIELD,
        ORIGINAL_WINDOW_FLAGS to "I",
        ORIGINAL_SYSTEM_UI to "I",
        KEEP_SCREEN_AWAKE_STATE to "Z",
        FULLSCREEN_STATE to "Z",
        ALLOW_SCREENSHOTS_STATE to "Z",
        TOUCH_START_X to "F",
        TOUCH_START_Y to "F",
        BUTTON_START_X to "F",
        BUTTON_START_Y to "F",
        CLOSE_CONFIRMATION to "Z",
        TOUCH_DRAGGED to "Z",
    )
    for ((name, type) in fields) {
        if (activity.fields.any { it.name == name }) continue
        activity.fields.add(
            ImmutableField(
                activity.type,
                name,
                type,
                AccessFlags.PRIVATE.value,
                null,
                emptySet(),
                emptySet(),
            ).toMutable(),
        )
    }
}

private fun addOverlayListeners(
    activity: MutableClass,
    title: String,
    description: String,
    repositoryLabel: String,
    repositoryUrl: String,
    backgroundColor: Int,
    outlineColor: Int,
    buttonText: String,
    buttonTextColor: Int,
    buttonBackgroundColor: Int,
    buttonShape: Int,
    includeKeepScreenAwake: Boolean,
    includeFullscreen: Boolean,
    includeScreenshots: Boolean,
    buttonIdleOpacity: Float,
) {
    // The three callbacks implement two Android listener interfaces: View.OnClickListener handles
    // both the floating button and menu CheckBoxes, while DialogInterface.OnClickListener handles
    // the dialog buttons. A target that already owns one of these signatures is skipped earlier.
    // These methods are generated from scratch, so their register layouts are intentionally
    // fixed below. Keep p0/p1/p2 for the method parameters and use v0-v9 only for temporaries.
    if (!activity.interfaces.contains("Landroid/view/View\$OnClickListener;")) {
        activity.interfaces.add("Landroid/view/View\$OnClickListener;")
    }
    if (!activity.interfaces.contains("Landroid/view/View\$OnTouchListener;")) {
        activity.interfaces.add("Landroid/view/View\$OnTouchListener;")
    }
    if (!activity.interfaces.contains("Landroid/content/DialogInterface\$OnClickListener;")) {
        activity.interfaces.add("Landroid/content/DialogInterface\$OnClickListener;")
    }

    // onClick(View): v0 is the CheckBox test/result, v1 is the menu item id, and v2-v9 are UI
    // construction temporaries. The terminal label is shared by both checkbox and menu paths.
    val viewClick = newMethod(activity, "onClick", listOf("Landroid/view/View;"), "V", registers = 10)
    val menuItems = listOfNotNull(
        "Keep screen awake".takeIf { includeKeepScreenAwake },
        "Fullscreen".takeIf { includeFullscreen },
        "Allow screenshots".takeIf { includeScreenshots },
    )
    // Explicit width and height limits keep the ScrollView usable across host themes and OEM
    // dialog implementations instead of allowing it to consume the entire display.
    viewClick.addValidatedInstructionsWithLabels("onClick(View)", compactSmali("""
        instance-of v0, p1, Landroid/widget/CheckBox;
        if-eqz v0, :nai64_overlay_open_menu
        invoke-virtual {p1}, Landroid/view/View;->getId()I
        move-result v1
        add-int/lit8 v1, v1, -0x1
        invoke-virtual {p1}, Landroid/widget/CheckBox;->isChecked()Z
        move-result v0
        ${buildControlHandler(
            activity.type,
            includeKeepScreenAwake,
            includeFullscreen,
            includeScreenshots,
            ":nai64_overlay_click_done",
        )}
        goto :nai64_overlay_click_done
        :nai64_overlay_open_menu
        iget-object v0, p0, ${activity.type}->${OVERLAY_BUTTON}:$OVERLAY_BUTTON_FIELD
        invoke-virtual {v0}, Landroid/view/View;->animate()Landroid/view/ViewPropertyAnimator;
        move-result-object v0
        const/4 v1, 0x0
        invoke-virtual {v0, v1}, Landroid/view/ViewPropertyAnimator;->alpha(F)Landroid/view/ViewPropertyAnimator;
        const-wide/16 v1, 0xb4
        invoke-virtual {v0, v1, v2}, Landroid/view/ViewPropertyAnimator;->setDuration(J)Landroid/view/ViewPropertyAnimator;
        invoke-virtual {v0}, Landroid/view/ViewPropertyAnimator;->start()V
        new-instance v3, Landroid/view/ContextThemeWrapper;
        const v4, 0x01030226
        invoke-direct {v3, p0, v4}, Landroid/view/ContextThemeWrapper;-><init>(Landroid/content/Context;I)V
        new-instance v2, Landroid/app/AlertDialog${'$'}Builder;
        invoke-direct {v2, v3}, Landroid/app/AlertDialog${'$'}Builder;-><init>(Landroid/content/Context;)V
        const-string v3, "${StartupHooks.escapeSmali(title)}"
        invoke-virtual {v2, v3}, Landroid/app/AlertDialog${'$'}Builder;->setTitle(Ljava/lang/CharSequence;)Landroid/app/AlertDialog${'$'}Builder;
        ${buildCustomMenuLayout(activity.type, description, menuItems, outlineColor)}
        const/4 v4, 0x0
        const/4 v5, 0x0
        const/4 v6, 0x0
        const/4 v7, 0x0
        invoke-virtual/range {v2 .. v7}, Landroid/app/AlertDialog${'$'}Builder;->setView(Landroid/view/View;IIII)Landroid/app/AlertDialog${'$'}Builder;
        const-string v3, "${StartupHooks.escapeSmali(repositoryLabel)}"
        invoke-virtual {v2, v3, p0}, Landroid/app/AlertDialog${'$'}Builder;->setNeutralButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface${'$'}OnClickListener;)Landroid/app/AlertDialog${'$'}Builder;
        const-string v3, "Close menu"
        invoke-virtual {v2, v3, p0}, Landroid/app/AlertDialog${'$'}Builder;->setNegativeButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface${'$'}OnClickListener;)Landroid/app/AlertDialog${'$'}Builder;
        const-string v3, "Fully close"
        invoke-virtual {v2, v3, p0}, Landroid/app/AlertDialog${'$'}Builder;->setPositiveButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface${'$'}OnClickListener;)Landroid/app/AlertDialog${'$'}Builder;
        invoke-virtual {v2}, Landroid/app/AlertDialog${'$'}Builder;->show()Landroid/app/AlertDialog;
        move-result-object v2
        invoke-virtual {v2}, Landroid/app/AlertDialog;->getWindow()Landroid/view/Window;
        move-result-object v4
        if-eqz v4, :nai64_overlay_menu_done
        invoke-virtual {p0}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;
        move-result-object v8
        invoke-virtual {v8}, Landroid/content/res/Resources;->getDisplayMetrics()Landroid/util/DisplayMetrics;
        move-result-object v8
        iget v9, v8, Landroid/util/DisplayMetrics;->density:F
        const/high16 v5, 0x41400000
        mul-float/2addr v5, v9
        float-to-int v5, v5
        iget v6, v8, Landroid/util/DisplayMetrics;->heightPixels:I
        int-to-float v6, v6
        const/high16 v7, 0x3f333333
        mul-float/2addr v6, v7
        float-to-int v6, v6
        iget v8, v8, Landroid/util/DisplayMetrics;->widthPixels:I
        sub-int/2addr v8, v5
        sub-int/2addr v8, v5
        const v7, 0x1a4
        int-to-float v7, v7
        mul-float/2addr v7, v9
        float-to-int v7, v7
        invoke-static {v8, v7}, Ljava/lang/Math;->min(II)I
        move-result v8
        invoke-virtual {v4, v8, v6}, Landroid/view/Window;->setLayout(II)V
        new-instance v5, Landroid/graphics/drawable/GradientDrawable;
        invoke-direct {v5}, Landroid/graphics/drawable/GradientDrawable;-><init>()V
        const v6, 0x${Integer.toHexString(backgroundColor)}
        invoke-virtual {v5, v6}, Landroid/graphics/drawable/GradientDrawable;->setColor(I)V
        const/4 v6, 0x1
        const v7, 0x${Integer.toHexString(outlineColor)}
        invoke-virtual/range {v5 .. v7}, Landroid/graphics/drawable/GradientDrawable;->setStroke(II)V
        const/high16 v6, 0x41000000
        invoke-virtual {v5, v6}, Landroid/graphics/drawable/GradientDrawable;->setCornerRadius(F)V
        invoke-virtual {v4, v5}, Landroid/view/Window;->setBackgroundDrawable(Landroid/graphics/drawable/Drawable;)V
        const/high16 v5, 0x3e800000
        invoke-virtual {v4, v5}, Landroid/view/Window;->setDimAmount(F)V
        const/4 v5, -0x1
        invoke-virtual {v2, v5}, Landroid/app/AlertDialog;->getButton(I)Landroid/widget/Button;
        move-result-object v5
        const v6, 0x${Integer.toHexString(outlineColor)}
        invoke-virtual {v5, v6}, Landroid/widget/TextView;->setTextColor(I)V
        const/4 v6, 0x0
        invoke-virtual {v5, v6}, Landroid/widget/TextView;->setAllCaps(Z)V
        invoke-virtual {v5, v6}, Landroid/view/View;->setBackgroundColor(I)V
        invoke-virtual {v5}, Landroid/view/View;->getLayoutParams()Landroid/view/ViewGroup${'$'}LayoutParams;
        move-result-object v6
        check-cast v6, Landroid/widget/LinearLayout${'$'}LayoutParams;
        const/4 v7, 0x0
        iput v7, v6, Landroid/view/ViewGroup${'$'}LayoutParams;->width:I
        const/high16 v7, 0x3f800000
        iput v7, v6, Landroid/widget/LinearLayout${'$'}LayoutParams;->weight:F
        const/16 v7, 0x11
        invoke-virtual {v5, v7}, Landroid/widget/TextView;->setGravity(I)V
        invoke-virtual {v5}, Landroid/view/View;->requestLayout()V
        const/16 v5, -0x2
        invoke-virtual {v2, v5}, Landroid/app/AlertDialog;->getButton(I)Landroid/widget/Button;
        move-result-object v5
        const v6, 0x${Integer.toHexString(outlineColor)}
        invoke-virtual {v5, v6}, Landroid/widget/TextView;->setTextColor(I)V
        const/4 v6, 0x0
        invoke-virtual {v5, v6}, Landroid/widget/TextView;->setAllCaps(Z)V
        invoke-virtual {v5, v6}, Landroid/view/View;->setBackgroundColor(I)V
        invoke-virtual {v5}, Landroid/view/View;->getLayoutParams()Landroid/view/ViewGroup${'$'}LayoutParams;
        move-result-object v6
        check-cast v6, Landroid/widget/LinearLayout${'$'}LayoutParams;
        const/4 v7, 0x0
        iput v7, v6, Landroid/view/ViewGroup${'$'}LayoutParams;->width:I
        const/high16 v7, 0x3f800000
        iput v7, v6, Landroid/widget/LinearLayout${'$'}LayoutParams;->weight:F
        const/16 v7, 0x11
        invoke-virtual {v5, v7}, Landroid/widget/TextView;->setGravity(I)V
        invoke-virtual {v5}, Landroid/view/View;->requestLayout()V
        const/16 v5, -0x3
        invoke-virtual {v2, v5}, Landroid/app/AlertDialog;->getButton(I)Landroid/widget/Button;
        move-result-object v5
        const v6, 0x${Integer.toHexString(outlineColor)}
        invoke-virtual {v5, v6}, Landroid/widget/TextView;->setTextColor(I)V
        const/4 v6, 0x0
        invoke-virtual {v5, v6}, Landroid/widget/TextView;->setAllCaps(Z)V
        invoke-virtual {v5, v6}, Landroid/view/View;->setBackgroundColor(I)V
        invoke-virtual {v5}, Landroid/view/View;->getLayoutParams()Landroid/view/ViewGroup${'$'}LayoutParams;
        move-result-object v6
        check-cast v6, Landroid/widget/LinearLayout${'$'}LayoutParams;
        const/4 v7, 0x0
        iput v7, v6, Landroid/view/ViewGroup${'$'}LayoutParams;->width:I
        const/high16 v7, 0x3f800000
        iput v7, v6, Landroid/widget/LinearLayout${'$'}LayoutParams;->weight:F
        const/16 v7, 0x11
        invoke-virtual {v5, v7}, Landroid/widget/TextView;->setGravity(I)V
        invoke-virtual {v5}, Landroid/view/View;->requestLayout()V
        invoke-virtual {v5}, Landroid/view/View;->getParent()Landroid/view/ViewParent;
        move-result-object v6
        instance-of v7, v6, Landroid/widget/LinearLayout;
        if-eqz v7, :nai64_overlay_menu_done
        check-cast v6, Landroid/widget/LinearLayout;
        const/4 v7, 0x1
        invoke-virtual {v6, v7}, Landroid/widget/LinearLayout;->setGravity(I)V
        :nai64_overlay_menu_done
        :nai64_overlay_click_done
        return-void
    """))
        activity.methods.add(viewClick)

    // onTouch(View,MotionEvent) consumes all touch events so a drag cannot also open the menu.
    val touch = newMethod(activity, "onTouch", listOf(
        "Landroid/view/View;",
        "Landroid/view/MotionEvent;",
    ), "Z", registers = 8)
    // Touch handling uses v0-v3 for coordinates and v4-v7 for parent/layout temporaries.
    touch.addValidatedInstructionsWithLabels("onTouch(View,MotionEvent)", compactSmali("""
        invoke-virtual {p2}, Landroid/view/MotionEvent;->getActionMasked()I
        move-result v0
        const/4 v1, 0x0
        if-eq v0, v1, :nai64_overlay_touch_down
        const/4 v1, 0x2
        if-eq v0, v1, :nai64_overlay_touch_move
        const/4 v1, 0x1
        if-eq v0, v1, :nai64_overlay_touch_up
        const/4 v0, 0x0
        return v0
        :nai64_overlay_touch_down
        invoke-virtual {p2}, Landroid/view/MotionEvent;->getRawX()F
        move-result v0
        iput v0, p0, ${activity.type}->${TOUCH_START_X}:F
        invoke-virtual {p2}, Landroid/view/MotionEvent;->getRawY()F
        move-result v1
        iput v1, p0, ${activity.type}->${TOUCH_START_Y}:F
        invoke-virtual {p1}, Landroid/view/View;->getX()F
        move-result v2
        iput v2, p0, ${activity.type}->${BUTTON_START_X}:F
        invoke-virtual {p1}, Landroid/view/View;->getY()F
        move-result v0
        iput v0, p0, ${activity.type}->${BUTTON_START_Y}:F
        const/4 v0, 0x0
        iput-boolean v0, p0, ${activity.type}->${TOUCH_DRAGGED}:Z
        const/4 v0, 0x1
        return v0
        :nai64_overlay_touch_move
        invoke-virtual {p2}, Landroid/view/MotionEvent;->getRawX()F
        move-result v0
        iget v1, p0, ${activity.type}->${TOUCH_START_X}:F
        sub-float/2addr v0, v1
        invoke-static {v0}, Ljava/lang/Math;->abs(F)F
        move-result v0
        const/high16 v1, 0x41000000
        cmpl-float v1, v0, v1
        if-lez v1, :nai64_overlay_move_x_skip
        const/4 v1, 0x1
        iput-boolean v1, p0, ${activity.type}->${TOUCH_DRAGGED}:Z
        invoke-virtual {p1}, Landroid/view/View;->animate()Landroid/view/ViewPropertyAnimator;
        move-result-object v0
        const/4 v1, 0x0
        invoke-virtual {v0, v1}, Landroid/view/ViewPropertyAnimator;->alpha(F)Landroid/view/ViewPropertyAnimator;
        const-wide/16 v1, 0xb4
        invoke-virtual {v0, v1, v2}, Landroid/view/ViewPropertyAnimator;->setDuration(J)Landroid/view/ViewPropertyAnimator;
        invoke-virtual {v0}, Landroid/view/ViewPropertyAnimator;->start()V
        invoke-virtual {p2}, Landroid/view/MotionEvent;->getRawX()F
        move-result v0
        iget v1, p0, ${activity.type}->${TOUCH_START_X}:F
        sub-float/2addr v0, v1
        iget v1, p0, ${activity.type}->${BUTTON_START_X}:F
        add-float/2addr v0, v1
        invoke-virtual {p1}, Landroid/view/View;->getParent()Landroid/view/ViewParent;
        move-result-object v1
        instance-of v2, v1, Landroid/view/ViewGroup;
        if-eqz v2, :nai64_overlay_move_x_done
        check-cast v1, Landroid/view/ViewGroup;
        invoke-virtual {v1}, Landroid/view/ViewGroup;->getWidth()I
        move-result v2
        invoke-virtual {p1}, Landroid/view/View;->getWidth()I
        move-result v3
        sub-int/2addr v2, v3
        int-to-float v2, v2
        invoke-static {v0, v2}, Ljava/lang/Math;->min(FF)F
        move-result v0
        const/4 v2, 0x0
        invoke-static {v0, v2}, Ljava/lang/Math;->max(FF)F
        move-result v0
        invoke-virtual {p1, v0}, Landroid/view/View;->setX(F)V
        :nai64_overlay_move_x_done
        :nai64_overlay_move_x_skip
        invoke-virtual {p2}, Landroid/view/MotionEvent;->getRawY()F
        move-result v0
        iget v1, p0, ${activity.type}->${TOUCH_START_Y}:F
        sub-float/2addr v0, v1
        invoke-static {v0}, Ljava/lang/Math;->abs(F)F
        move-result v0
        const/high16 v1, 0x41000000
        cmpl-float v1, v0, v1
        if-lez v1, :nai64_overlay_move_y_skip
        const/4 v1, 0x1
        iput-boolean v1, p0, ${activity.type}->${TOUCH_DRAGGED}:Z
        invoke-virtual {p2}, Landroid/view/MotionEvent;->getRawY()F
        move-result v0
        iget v1, p0, ${activity.type}->${TOUCH_START_Y}:F
        sub-float/2addr v0, v1
        iget v1, p0, ${activity.type}->${BUTTON_START_Y}:F
        add-float/2addr v0, v1
        invoke-virtual {p1}, Landroid/view/View;->getParent()Landroid/view/ViewParent;
        move-result-object v1
        instance-of v2, v1, Landroid/view/ViewGroup;
        if-eqz v2, :nai64_overlay_move_y_done
        check-cast v1, Landroid/view/ViewGroup;
        invoke-virtual {v1}, Landroid/view/ViewGroup;->getHeight()I
        move-result v2
        invoke-virtual {p1}, Landroid/view/View;->getHeight()I
        move-result v3
        sub-int/2addr v2, v3
        int-to-float v2, v2
        invoke-static {v0, v2}, Ljava/lang/Math;->min(FF)F
        move-result v0
        const/4 v2, 0x0
        invoke-static {v0, v2}, Ljava/lang/Math;->max(FF)F
        move-result v0
        invoke-virtual {p1, v0}, Landroid/view/View;->setY(F)V
        :nai64_overlay_move_y_done
        :nai64_overlay_move_y_skip
        const/4 v0, 0x1
        return v0
        :nai64_overlay_touch_up
        iget-boolean v0, p0, ${activity.type}->${TOUCH_DRAGGED}:Z
        if-nez v0, :nai64_overlay_drag_finished
        invoke-virtual {p1}, Landroid/view/View;->performClick()Z
        goto :nai64_overlay_touch_consumed
        :nai64_overlay_drag_finished
        invoke-virtual {p1}, Landroid/view/View;->animate()Landroid/view/ViewPropertyAnimator;
        move-result-object v0
        const v1, ${floatLiteral(buttonIdleOpacity)}
        invoke-virtual {v0, v1}, Landroid/view/ViewPropertyAnimator;->alpha(F)Landroid/view/ViewPropertyAnimator;
        const-wide/16 v1, 0xb4
        invoke-virtual {v0, v1, v2}, Landroid/view/ViewPropertyAnimator;->setDuration(J)Landroid/view/ViewPropertyAnimator;
        invoke-virtual {v0}, Landroid/view/ViewPropertyAnimator;->start()V
        :nai64_overlay_touch_consumed
        const/4 v0, 0x1
        return v0
    """))
    activity.methods.add(touch)

    // Dialog buttons are identified by Android's standard -1/-2/-3 constants: positive,
    // negative, and neutral. The close-confirmation flag distinguishes the second dialog.
    val dialogClick = newMethod(activity, "onClick", listOf(
        "Landroid/content/DialogInterface;",
        "I",
    ), "V")
    // Resolve the repository Intent before launching it; some Android profiles have no browser
    // handler, and that case must remain a normal in-app Toast rather than a crash.
    dialogClick.addValidatedInstructionsWithLabels("onClick(DialogInterface,int)", compactSmali("""
        iget-boolean v3, p0, ${activity.type}->${CLOSE_CONFIRMATION}:Z
        if-eqz v3, :nai64_overlay_main_dialog
        const/4 v3, 0x0
        iput-boolean v3, p0, ${activity.type}->${CLOSE_CONFIRMATION}:Z
        const/16 v2, -0x1
        if-eq p2, v2, :nai64_overlay_remove
        goto :nai64_overlay_done
        :nai64_overlay_main_dialog
        const/16 v2, -0x3
        if-eq p2, v2, :nai64_overlay_repository
        iget-object v0, p0, ${activity.type}->${OVERLAY_BUTTON}:$OVERLAY_BUTTON_FIELD
        if-eqz v0, :nai64_overlay_done
        const/16 v2, -0x1
        if-ne p2, v2, :nai64_overlay_toast
        new-instance v0, Landroid/app/AlertDialog${'$'}Builder;
        invoke-direct {v0, p0}, Landroid/app/AlertDialog${'$'}Builder;-><init>(Landroid/content/Context;)V
        const-string v1, "Fully close overlay?"
        invoke-virtual {v0, v1}, Landroid/app/AlertDialog${'$'}Builder;->setTitle(Ljava/lang/CharSequence;)Landroid/app/AlertDialog${'$'}Builder;
        const-string v1, "The overlay will no longer be available until you reopen the app. Continue?"
        invoke-virtual {v0, v1}, Landroid/app/AlertDialog${'$'}Builder;->setMessage(Ljava/lang/CharSequence;)Landroid/app/AlertDialog${'$'}Builder;
        const/4 v1, 0x0
        invoke-virtual {v0, v1}, Landroid/app/AlertDialog${'$'}Builder;->setCancelable(Z)Landroid/app/AlertDialog${'$'}Builder;
        const-string v1, "No"
        invoke-virtual {v0, v1, p0}, Landroid/app/AlertDialog${'$'}Builder;->setNegativeButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface${'$'}OnClickListener;)Landroid/app/AlertDialog${'$'}Builder;
        const-string v1, "Yes"
        invoke-virtual {v0, v1, p0}, Landroid/app/AlertDialog${'$'}Builder;->setPositiveButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface${'$'}OnClickListener;)Landroid/app/AlertDialog${'$'}Builder;
        const/4 v1, 0x1
        iput-boolean v1, p0, ${activity.type}->${CLOSE_CONFIRMATION}:Z
        invoke-virtual {v0}, Landroid/app/AlertDialog${'$'}Builder;->show()Landroid/app/AlertDialog;
        goto :nai64_overlay_done
        :nai64_overlay_remove
        iget-object v0, p0, ${activity.type}->${OVERLAY_BUTTON}:$OVERLAY_BUTTON_FIELD
        if-eqz v0, :nai64_overlay_done
        invoke-virtual {v0}, Landroid/view/View;->getParent()Landroid/view/ViewParent;
        move-result-object v1
        instance-of v2, v1, Landroid/view/ViewGroup;
        if-eqz v2, :nai64_overlay_done
        check-cast v1, Landroid/view/ViewGroup;
        invoke-virtual {v1, v0}, Landroid/view/ViewGroup;->removeView(Landroid/view/View;)V
        goto :nai64_overlay_done
        :nai64_overlay_toast
        const-string v1, "Overlay menu hidden. Press the overlay button to open it again."
        const/4 v2, 0x0
        invoke-static {p0, v1, v2}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
        move-result-object v1
        invoke-virtual {v1}, Landroid/widget/Toast;->show()V
        goto :nai64_overlay_done
        :nai64_overlay_repository
        new-instance v0, Landroid/content/Intent;
        const-string v1, "android.intent.action.VIEW"
        invoke-direct {v0, v1}, Landroid/content/Intent;-><init>(Ljava/lang/String;)V
        const-string v1, "${StartupHooks.escapeSmali(repositoryUrl)}"
        invoke-static {v1}, Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;
        move-result-object v1
        invoke-virtual {v0, v1}, Landroid/content/Intent;->setData(Landroid/net/Uri;)Landroid/content/Intent;
        invoke-virtual {p0}, Landroid/content/Context;->getPackageManager()Landroid/content/pm/PackageManager;
        move-result-object v3
        const/4 v4, 0x0
        invoke-virtual {v3, v0, v4}, Landroid/content/pm/PackageManager;->resolveActivity(Landroid/content/Intent;I)Landroid/content/pm/ResolveInfo;
        move-result-object v3
        if-eqz v3, :nai64_overlay_repository_unavailable
        :try_start_nai64_overlay_repository
        invoke-virtual {p0, v0}, Landroid/app/Activity;->startActivity(Landroid/content/Intent;)V
        goto :nai64_overlay_done
        :try_end_nai64_overlay_repository
        .catch Landroid/content/ActivityNotFoundException; {:try_start_nai64_overlay_repository .. :try_end_nai64_overlay_repository} :nai64_overlay_repository_unavailable
        :nai64_overlay_repository_unavailable
        const-string v1, "No app is available to open the repository link."
        const/4 v2, 0x0
        invoke-static {p0, v1, v2}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
        move-result-object v1
        invoke-virtual {v1}, Landroid/widget/Toast;->show()V
        goto :nai64_overlay_done
        :nai64_overlay_done
        return-void
    """))
    activity.methods.add(dialogClick)

}

private fun buildCustomMenuLayout(
    activityType: String,
    description: String,
    menuItems: List<String>,
    outlineColor: Int,
): String = buildString {
    // Keep the content in a ScrollView because descriptions and future controls may exceed the
    // available height. The caller separately constrains the dialog window dimensions.
    appendLine("new-instance v3, Landroid/widget/ScrollView;")
    appendLine("invoke-direct {v3, p0}, Landroid/widget/ScrollView;-><init>(Landroid/content/Context;)V")
    appendLine("new-instance v4, Landroid/widget/LinearLayout;")
    appendLine("invoke-direct {v4, p0}, Landroid/widget/LinearLayout;-><init>(Landroid/content/Context;)V")
    appendLine("const/4 v5, 0x1")
    appendLine("invoke-virtual {v4, v5}, Landroid/widget/LinearLayout;->setOrientation(I)V")
    appendLine("invoke-virtual {p0}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;")
    appendLine("move-result-object v5")
    appendLine("invoke-virtual {v5}, Landroid/content/res/Resources;->getDisplayMetrics()Landroid/util/DisplayMetrics;")
    appendLine("move-result-object v5")
    appendLine("iget v5, v5, Landroid/util/DisplayMetrics;->density:F")
    appendLine("const/high16 v6, 0x41c00000")
    appendLine("mul-float/2addr v5, v6")
    appendLine("float-to-int v5, v5")
    appendLine("invoke-virtual {v4, v5, v5, v5, v5}, Landroid/view/View;->setPadding(IIII)V")
    appendLine("new-instance v5, Landroid/widget/TextView;")
    appendLine("invoke-direct {v5, p0}, Landroid/widget/TextView;-><init>(Landroid/content/Context;)V")
    appendLine("const-string v6, \"${StartupHooks.escapeSmali(description)}\"")
    appendLine("invoke-virtual {v5, v6}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V")
    appendLine("const v6, 0x${Integer.toHexString(outlineColor)}")
    appendLine("invoke-virtual {v5, v6}, Landroid/widget/TextView;->setTextColor(I)V")
    appendLine("const/high16 v6, 0x41600000")
    appendLine("invoke-virtual {v5, v6}, Landroid/widget/TextView;->setTextSize(F)V")
    appendLine("invoke-virtual {v4, v5}, Landroid/view/ViewGroup;->addView(Landroid/view/View;)V")
    menuItems.forEachIndexed { index, item ->
        // IDs are local to this generated menu and intentionally start at one because the click
        // handler subtracts one before matching the selected control index.
        appendLine("new-instance v5, Landroid/widget/CheckBox;")
        appendLine("invoke-direct {v5, p0}, Landroid/widget/CheckBox;-><init>(Landroid/content/Context;)V")
        appendLine("const-string v6, \"${StartupHooks.escapeSmali(item)}\"")
        appendLine("invoke-virtual {v5, v6}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V")
        appendLine("const/4 v6, ${index + 1}")
        appendLine("invoke-virtual {v5, v6}, Landroid/view/View;->setId(I)V")
        val field = when (item) {
            "Keep screen awake" -> KEEP_SCREEN_AWAKE_STATE
            "Fullscreen" -> FULLSCREEN_STATE
            else -> ALLOW_SCREENSHOTS_STATE
        }
        appendLine("iget-boolean v6, p0, $activityType->$field:Z")
        appendLine("invoke-virtual {v5, v6}, Landroid/widget/CompoundButton;->setChecked(Z)V")
        appendLine("invoke-virtual {v5, p0}, Landroid/view/View;->setOnClickListener(Landroid/view/View\$OnClickListener;)V")
        appendLine("invoke-virtual {v4, v5}, Landroid/view/ViewGroup;->addView(Landroid/view/View;)V")
    }
    appendLine("invoke-virtual {v3, v4}, Landroid/widget/ScrollView;->addView(Landroid/view/View;)V")
}

private fun buildControlHandler(
    activityType: String,
    includeKeepScreenAwake: Boolean,
    includeFullscreen: Boolean,
    includeScreenshots: Boolean,
    terminalLabel: String,
): String {
    // Control branches are emitted only for enabled options. Disabled options are absent from
    // both the menu and the handler, preserving the target app's original behavior by default.
    val blocks = mutableListOf<String>()
    var index = 0
    if (includeKeepScreenAwake) {
        blocks += controlBranch(activityType, index, "0x80", "keep")
        index++
    }
    if (includeFullscreen) {
        blocks += controlBranch(activityType, index, "0x4", "fullscreen")
        index++
    }
    if (includeScreenshots) blocks += controlBranch(activityType, index, "0x2000", "screenshots")
    if (blocks.isEmpty()) return "goto $terminalLabel"
    // Every checkbox path exits through the caller-owned terminal label. This avoids relying on
    // fall-through from generated control blocks into whatever instructions follow them.
    return blocks.joinToString("\n").replace(":nai64_control_done", terminalLabel)
}

private fun controlBranch(activityType: String, index: Int, mask: String, kind: String): String = when (kind) {
    // Window flags are restored from the captured original values when a switch is turned off;
    // this avoids assuming that the host app started with a particular flag configuration.
    "keep", "screenshots" -> """
        const/16 v3, $index
        if-ne v1, v3, :nai64_next_control_$index
        iput-boolean v0, p0, $activityType->${if (kind == "keep") KEEP_SCREEN_AWAKE_STATE else ALLOW_SCREENSHOTS_STATE}:Z
        invoke-virtual {p0}, Landroid/app/Activity;->getWindow()Landroid/view/Window;
        move-result-object v4
        if-eqz v0, :nai64_restore_$index
        ${if (kind == "screenshots") "const v5, $mask\n        invoke-virtual {v4, v5}, Landroid/view/Window;->clearFlags(I)V" else "const v5, $mask\n        invoke-virtual {v4, v5}, Landroid/view/Window;->addFlags(I)V"}
        goto :nai64_control_done
        :nai64_restore_$index
        iget v5, p0, $activityType->${ORIGINAL_WINDOW_FLAGS}:I
        const v6, $mask
        and-int/2addr v5, v6
        if-eqz v5, :nai64_clear_$index
        invoke-virtual {v4, v6}, Landroid/view/Window;->addFlags(I)V
        goto :nai64_control_done
        :nai64_clear_$index
        invoke-virtual {v4, v6}, Landroid/view/Window;->clearFlags(I)V
        goto :nai64_control_done
        :nai64_next_control_$index
    """.trimIndent()
    "fullscreen" -> """
        const/16 v3, $index
        if-ne v1, v3, :nai64_next_control_$index
        iput-boolean v0, p0, $activityType->$FULLSCREEN_STATE:Z
        invoke-virtual {p0}, Landroid/app/Activity;->getWindow()Landroid/view/Window;
        move-result-object v4
        invoke-virtual {v4}, Landroid/view/Window;->getDecorView()Landroid/view/View;
        move-result-object v5
        if-eqz v0, :nai64_restore_$index
        const v6, 0x1706
        invoke-virtual {v5, v6}, Landroid/view/View;->setSystemUiVisibility(I)V
        goto :nai64_control_done
        :nai64_restore_$index
        iget v6, p0, $activityType->${ORIGINAL_SYSTEM_UI}:I
        invoke-virtual {v5, v6}, Landroid/view/View;->setSystemUiVisibility(I)V
        goto :nai64_control_done
        :nai64_next_control_$index
    """.trimIndent()
    else -> ""
}

private fun newMethod(
    activity: MutableClass,
    name: String,
    parameterTypes: List<String>,
    returnType: String,
    registers: Int = 8,
    accessFlags: Int = AccessFlags.PUBLIC.value,
): MutableMethod = ImmutableMethod(
    // The implementation starts empty; addInstructionsWithLabels parses and attaches the
    // generated instruction list afterward. `registers` is the total DEX register frame size.
    activity.type,
    name,
    parameterTypes.map { com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter(it, emptySet(), null) },
    returnType,
    accessFlags,
    emptySet(),
    emptySet(),
    ImmutableMethodImplementation(registers, emptyList(), emptyList(), emptyList()),
).toMutable()

private fun parseButtonShape(shape: String): Int = when (shape) {
    "circle" -> 1
    "squircle" -> 2
    "square" -> 0
    else -> 1
}

private fun injectOverlay(
    onWindowFocusChanged: MutableMethod,
    activity: MutableClass,
    outlineColor: Int,
    buttonText: String,
    buttonTextColor: Int,
    buttonBackgroundColor: Int,
    buttonShape: Int,
    buttonSizeDp: Int,
    buttonGravity: Int,
    includeKeepScreenAwake: Boolean,
    includeFullscreen: Boolean,
    includeScreenshots: Boolean,
    buttonIdleOpacity: Float,
) {
    // The helper is called from onWindowFocusChanged because that callback runs after the
    // Activity has a usable window. The field-null guard makes repeated focus callbacks harmless.
    val helperName = "nai64CreateRuntimeOverlay"
    val helper = newMethod(
        activity = activity,
        name = helperName,
        parameterTypes = listOf(activity.type),
        returnType = "V",
        registers = 15,
        accessFlags = AccessFlags.PRIVATE.value or AccessFlags.STATIC.value,
    )
    // The helper deliberately owns a larger fixed register frame because it constructs the
    // complete view hierarchy and uses /range invokes for multi-argument framework calls.
    val initialState = buildInitialState(0, activity.type, includeKeepScreenAwake, includeFullscreen, includeScreenshots)
    val buttonCornerRadius = if (buttonShape == 2) {
        "const/high16 v4, 0x41400000\n        invoke-virtual {v3, v4}, Landroid/graphics/drawable/GradientDrawable;->setCornerRadius(F)V"
    } else {
        ""
    }
    helper.addValidatedInstructionsWithLabels("nai64CreateRuntimeOverlay", compactSmali("""
        invoke-virtual {p0}, Landroid/app/Activity;->hasWindowFocus()Z
        move-result v1
        if-eqz v1, :nai64_overlay_done
        iget-object v0, p0, ${activity.type}->$OVERLAY_BUTTON:$OVERLAY_BUTTON_FIELD
        if-nez v0, :nai64_overlay_done
        invoke-virtual {p0}, Landroid/app/Activity;->getWindow()Landroid/view/Window;
        move-result-object v6
        invoke-virtual {v6}, Landroid/view/Window;->getAttributes()Landroid/view/WindowManager${'$'}LayoutParams;
        move-result-object v7
        iget v8, v7, Landroid/view/WindowManager${'$'}LayoutParams;->flags:I
        iput v8, p0, ${activity.type}->${ORIGINAL_WINDOW_FLAGS}:I
        invoke-virtual {v6}, Landroid/view/Window;->getDecorView()Landroid/view/View;
        move-result-object v9
        invoke-virtual {v9}, Landroid/view/View;->getSystemUiVisibility()I
        move-result v10
        iput v10, p0, ${activity.type}->${ORIGINAL_SYSTEM_UI}:I
        $initialState
        new-instance v0, Landroid/widget/TextView;
        invoke-direct {v0, p0}, Landroid/widget/TextView;-><init>(Landroid/content/Context;)V
        const-string v1, "${StartupHooks.escapeSmali(buttonText)}"
        invoke-virtual {v0, v1}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V
        const v1, 0x${Integer.toHexString(buttonTextColor)}
        invoke-virtual {v0, v1}, Landroid/widget/TextView;->setTextColor(I)V
        sget-object v1, Landroid/graphics/Typeface;->DEFAULT:Landroid/graphics/Typeface;
        const/4 v2, 0x1
        invoke-virtual/range {v0 .. v2}, Landroid/widget/TextView;->setTypeface(Landroid/graphics/Typeface;I)V
        const/high16 v1, 0x40000000
        const/high16 v2, 0x3f800000
        const/high16 v3, 0x3f800000
        const v4, -0x1000000
        invoke-virtual/range {v0 .. v4}, Landroid/widget/TextView;->setShadowLayer(FFFI)V
        const v1, ${floatLiteral(buttonIdleOpacity)}
        invoke-virtual {v0, v1}, Landroid/view/View;->setAlpha(F)V
        invoke-virtual {p0}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;
        move-result-object v11
        invoke-virtual {v11}, Landroid/content/res/Resources;->getDisplayMetrics()Landroid/util/DisplayMetrics;
        move-result-object v11
        iget v1, v11, Landroid/util/DisplayMetrics;->density:F
        const v2, $buttonSizeDp
        int-to-float v2, v2
        mul-float/2addr v2, v1
        float-to-int v2, v2
        invoke-virtual {v0, v2}, Landroid/widget/TextView;->setWidth(I)V
        invoke-virtual {v0, v2}, Landroid/widget/TextView;->setHeight(I)V
        const/16 v1, 0x11
        invoke-virtual {v0, v1}, Landroid/widget/TextView;->setGravity(I)V
        new-instance v3, Landroid/graphics/drawable/GradientDrawable;
        invoke-direct {v3}, Landroid/graphics/drawable/GradientDrawable;-><init>()V
        const v4, ${if (buttonShape == 1) 1 else 0}
        invoke-virtual {v3, v4}, Landroid/graphics/drawable/GradientDrawable;->setShape(I)V
        const v4, 0x${Integer.toHexString(buttonBackgroundColor)}
        invoke-virtual {v3, v4}, Landroid/graphics/drawable/GradientDrawable;->setColor(I)V
        const/4 v4, 0x1
        const v5, $outlineColor
        invoke-virtual/range {v3 .. v5}, Landroid/graphics/drawable/GradientDrawable;->setStroke(II)V
        $buttonCornerRadius
        invoke-virtual {v0, v3}, Landroid/view/View;->setBackground(Landroid/graphics/drawable/Drawable;)V
        invoke-virtual {v0, p0}, Landroid/view/View;->setOnClickListener(Landroid/view/View${'$'}OnClickListener;)V
        invoke-virtual {v0, p0}, Landroid/view/View;->setOnTouchListener(Landroid/view/View${'$'}OnTouchListener;)V
        iput-object v0, p0, ${activity.type}->${OVERLAY_BUTTON}:$OVERLAY_BUTTON_FIELD
        new-instance v10, Landroid/widget/FrameLayout${'$'}LayoutParams;
        invoke-direct {v10, v2, v2}, Landroid/widget/FrameLayout${'$'}LayoutParams;-><init>(II)V
        const/16 v4, 0x10
        int-to-float v4, v4
        mul-float/2addr v4, v1
        float-to-int v4, v4
        move v5, v4
        move v6, v4
        move v7, v4
        invoke-virtual {v10, v4, v5, v6, v7}, Landroid/view/ViewGroup${'$'}MarginLayoutParams;->setMargins(IIII)V
        const v1, $buttonGravity
        iput v1, v10, Landroid/widget/FrameLayout${'$'}LayoutParams;->gravity:I
        invoke-virtual {p0, v0, v10}, Landroid/app/Activity;->addContentView(Landroid/view/View;Landroid/view/ViewGroup${'$'}LayoutParams;)V
        :nai64_overlay_done
        return-void
    """))
    activity.methods.add(helper)
    val insertionIndex = onWindowFocusChanged.implementation!!.instructions
        .indexOfLast { it.opcode.name.startsWith("RETURN") }
        .coerceAtLeast(0)
    onWindowFocusChanged.addInstructions(
        insertionIndex,
        "invoke-static {p0}, ${activity.type}->$helperName(${activity.type})V",
    )
}

private fun compactSmali(smali: String): String =
    // Remove indentation-only blank lines while preserving instruction order and label names.
    smali.lines().filter(String::isNotBlank).joinToString("\n")

/**
 * Performs cheap, generator-side validation before dexlib parses injected instructions.
 *
 * This is not a replacement for Android's verifier, but it catches the failures that previously
 * produced an installable APK with a truncated or unterminated listener method.
 */
private fun MutableMethod.addValidatedInstructionsWithLabels(methodName: String, smali: String) {
    // This lightweight preflight runs before dexlib2 parses the text. It is intentionally small,
    // but catches unresolved branches, missing terminal instructions, and short invoke overflow.
    val labels = smali.lineSequence()
        .map(String::trim)
        .filter { it.startsWith(":") }
        .map { it.substring(1) }
        .toSet()
    val branchTargets = Regex("\\b(?:goto|if-[^, ]+)\\s+(?::([A-Za-z0-9_]+))")
        .findAll(smali)
        .map { it.groupValues[1] }
        .toSet()
    check(branchTargets.all { it in labels }) {
        "Generated $methodName contains an unresolved branch label."
    }
    // Smali uses `return-void` for void methods but `return v0` for value-returning methods such
    // as onTouch(Z). Accept both forms so validation does not reject a structurally valid listener.
    check(Regex("(?m)^\\s*(?:return(?:-[^\\s]+)?|throw)\\b").containsMatchIn(smali)) {
        "Generated $methodName has no terminal return or throw instruction."
    }
    // Escape the closing brace explicitly: Android's ICU regex engine rejects an unescaped }
    // even though some desktop Java regex implementations accept it as a literal.
    Regex("(?m)^\\s*invoke-(?![^ ]*/range)\\S+\\s+\\{([^}]*)\\}")
        .findAll(smali)
        .forEach { match ->
            val registerCount = match.groupValues[1].split(',').count { it.isNotBlank() }
            check(registerCount <= 5) {
                "Generated $methodName uses $registerCount registers in a short invoke; use /range."
            }
        }
    addInstructionsWithLabels(0, smali)
}

private fun parseButtonGravity(position: String): Int = when (position) {
    "topLeft" -> 0x33
    "topMiddle" -> 0x31
    "topRight" -> 0x35
    "centerLeft" -> 0x13
    "centerRight" -> 0x15
    "bottomLeft" -> 0x53
    "bottomMiddle" -> 0x51
    "bottomRight" -> 0x55
    else -> 0x35
}

private fun buildInitialState(
    base: Int,
    activityType: String,
    includeKeepScreenAwake: Boolean,
    includeFullscreen: Boolean,
    includeScreenshots: Boolean,
): String = buildList {
    // Capture the host window's original state before the overlay changes anything. Turning a
    // control off later can then restore only the bits that were originally enabled.
    if (includeKeepScreenAwake) add(
        """
        iget v$base, p0, $activityType->${ORIGINAL_WINDOW_FLAGS}:I
        const v${base + 1}, 0x80
        and-int/2addr v$base, v${base + 1}
        if-eqz v$base, :nai64_initial_keep_off
        const/4 v$base, 0x1
        goto :nai64_initial_keep_done
        :nai64_initial_keep_off
        const/4 v$base, 0x0
        :nai64_initial_keep_done
        iput-boolean v$base, p0, $activityType->${KEEP_SCREEN_AWAKE_STATE}:Z
        """.trimIndent(),
    )
    if (includeFullscreen) add(
        """
        iget v${base + 2}, p0, $activityType->${ORIGINAL_SYSTEM_UI}:I
        const v${base + 3}, 0x4
        and-int/2addr v${base + 2}, v${base + 3}
        if-eqz v${base + 2}, :nai64_initial_fullscreen_off
        const/4 v${base + 2}, 0x1
        goto :nai64_initial_fullscreen_done
        :nai64_initial_fullscreen_off
        const/4 v${base + 2}, 0x0
        :nai64_initial_fullscreen_done
        iput-boolean v${base + 2}, p0, $activityType->${FULLSCREEN_STATE}:Z
        """.trimIndent(),
    )
    if (includeScreenshots) add(
        """
        iget v${base + 4}, p0, $activityType->${ORIGINAL_WINDOW_FLAGS}:I
        const v${base + 5}, 0x2000
        and-int/2addr v${base + 4}, v${base + 5}
        if-eqz v${base + 4}, :nai64_initial_screenshots_on
        const/4 v${base + 4}, 0x0
        goto :nai64_initial_screenshots_done
        :nai64_initial_screenshots_on
        const/4 v${base + 4}, 0x1
        :nai64_initial_screenshots_done
        iput-boolean v${base + 4}, p0, $activityType->${ALLOW_SCREENSHOTS_STATE}:Z
        """.trimIndent(),
    )
}.joinToString("\n")
