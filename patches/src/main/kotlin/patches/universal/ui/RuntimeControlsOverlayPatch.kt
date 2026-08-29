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
import patches.universal.ads.util.cloneMutable
import patches.universal.ads.util.p0Register

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
private const val DEFAULT_DESCRIPTION =
    "Welcome to Nai64Patches Runtime Controls Overlay. This experimental in-app overlay " +
        "contains controls that may change parts of the app or game at runtime. More may be " +
        "added in future updates."

private fun parseColor(value: String, fallback: Int): Int {
    val normalized = value.trim().removePrefix("#")
    val hex = when (normalized.length) {
        6 -> "FF$normalized"
        8 -> normalized
        else -> return fallback
    }
    return hex.toLongOrNull(16)?.toInt() ?: fallback
}

@Suppress("unused")
val runtimeControlsOverlayPatch = bytecodePatch(
    name = "Runtime Controls Overlay (Experimental)",
    description =
        "Experimental in-app floating runtime controls for Nai64Patches users. Select which " +
            "controls to include in Morphe Manager. Each selected control adds APK hooks and a " +
            "runtime switch to the overlay. Switches start with the original app or game behavior; " +
            "changes apply to the current Activity immediately. The overlay also provides a " +
            "repository link, hide/remove actions, and customizable colors, text, and URL.",
    default = false,
) {
    dependsOn(StartupHooks.resolveRealApplicationPatch)

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
    val buttonSizeDp by intOption(
        title = "Overlay button size (dp)",
        default = 56,
        key = "runtimeOverlayButtonSizeDp",
        description = "Button width and height in density-independent pixels. Recommended: 48-72.",
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
        val logger = Logger.getLogger(this::class.java.name)
        val titleText = title.orEmpty().ifBlank { "Nai64Patches Runtime Controls Overlay" }
        val descriptionValue = descriptionText.orEmpty().ifBlank { DEFAULT_DESCRIPTION }
        val repositoryLabel = repositoryText.orEmpty().ifBlank { "Nai64 repository" }
        val repository = repositoryUrl.orEmpty().ifBlank { "https://github.com/Nai64/Nai64Patches" }
        val background = parseColor(backgroundColor.orEmpty(), 0xCC101820.toInt())
        val outline = parseColor(outlineColor.orEmpty(), 0xFF55D6BE.toInt())
        val buttonSize = (buttonSizeDp ?: 56).coerceIn(32, 128)
        val buttonGravity = parseButtonGravity(buttonPosition.orEmpty())
        val selectedControls = listOfNotNull(
            "keep screen awake".takeIf { includeKeepScreenAwake == true },
            "fullscreen".takeIf { includeFullscreen == true },
            "allow screenshots".takeIf { includeScreenshots == true },
        )
        var patched = 0
        val superMap = mutableMapOf<String, String>()
        classDefForEach { classDef -> classDef.superclass?.let { superMap[classDef.type] = it } }
        fun isActivity(type: String, seen: MutableSet<String> = mutableSetOf()): Boolean {
            if (type == "Landroid/app/Activity;") return true
            if (type == "Ljava/lang/Object;" || type in seen) return false
            seen.add(type)
            return superMap[type]?.let { isActivity(it, seen) } == true
        }

        val candidates = mutableListOf<MutableClass>()
        classDefForEach { classDef ->
            if (!isActivity(classDef.type)) return@classDefForEach
            candidates.add(mutableClassDefBy(classDef))
        }

        val launcher = StartupHooks.resolvedLauncherActivityDescriptor
        val activity = candidates.firstOrNull { it.type == launcher } ?: candidates.firstOrNull()
        if (activity != null) {
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
                logger.warning("Overlay target already has a conflicting onClick method. No changes applied.")
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
                includeKeepScreenAwake == true,
                includeFullscreen == true,
                includeScreenshots == true,
            )
            injectOverlay(
                onWindowFocusChanged,
                activity,
                outline,
                buttonSize,
                buttonGravity,
                includeKeepScreenAwake == true,
                includeFullscreen == true,
                includeScreenshots == true,
            )
            patched = 1
        }

        if (patched > 0) {
            logger.info(
                "Injected experimental runtime controls overlay into $patched activit(ies); " +
                    "selected controls: ${selectedControls.joinToString().ifEmpty { "none" }}",
            )
        } else {
            logger.warning("No compatible Activity window focus methods found. No changes applied.")
        }
    }
}

private fun addOverlayField(activity: MutableClass) {
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
    includeKeepScreenAwake: Boolean,
    includeFullscreen: Boolean,
    includeScreenshots: Boolean,
) {
    activity.interfaces.add("Landroid/view/View\$OnClickListener;")
    activity.interfaces.add("Landroid/view/View\$OnTouchListener;")
    activity.interfaces.add("Landroid/content/DialogInterface\$OnClickListener;")
    activity.interfaces.add("Landroid/content/DialogInterface\$OnMultiChoiceClickListener;")

    val viewClick = newMethod(activity, "onClick", listOf("Landroid/view/View;"), "V", registers = 10)
    val menuItems = listOfNotNull(
        "Keep screen awake".takeIf { includeKeepScreenAwake },
        "Fullscreen".takeIf { includeFullscreen },
        "Allow screenshots".takeIf { includeScreenshots },
    )
    val menuSetup = buildMenuSetup(
        activity.type,
        menuItems,
        includeKeepScreenAwake,
        includeFullscreen,
        includeScreenshots,
    )
    viewClick.addInstructionsWithLabels(0, compactSmali("""
        new-instance v2, Landroid/app/AlertDialog${'$'}Builder;
        invoke-direct {v2, p0}, Landroid/app/AlertDialog${'$'}Builder;-><init>(Landroid/content/Context;)V
        const-string v3, "${StartupHooks.escapeSmali(title)}"
        invoke-virtual {v2, v3}, Landroid/app/AlertDialog${'$'}Builder;->setTitle(Ljava/lang/CharSequence;)Landroid/app/AlertDialog${'$'}Builder;
        const-string v3, "${StartupHooks.escapeSmali(description)}"
        invoke-virtual {v2, v3}, Landroid/app/AlertDialog${'$'}Builder;->setMessage(Ljava/lang/CharSequence;)Landroid/app/AlertDialog${'$'}Builder;
        const-string v3, "${StartupHooks.escapeSmali(repositoryLabel)}"
        invoke-virtual {v2, v3, p0}, Landroid/app/AlertDialog${'$'}Builder;->setNeutralButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface${'$'}OnClickListener;)Landroid/app/AlertDialog${'$'}Builder;
        $menuSetup
        const-string v3, "Close menu"
        invoke-virtual {v2, v3, p0}, Landroid/app/AlertDialog${'$'}Builder;->setNegativeButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface${'$'}OnClickListener;)Landroid/app/AlertDialog${'$'}Builder;
        const-string v3, "Fully close"
        invoke-virtual {v2, v3, p0}, Landroid/app/AlertDialog${'$'}Builder;->setPositiveButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface${'$'}OnClickListener;)Landroid/app/AlertDialog${'$'}Builder;
        invoke-virtual {v2}, Landroid/app/AlertDialog${'$'}Builder;->show()Landroid/app/AlertDialog;
        move-result-object v2
        invoke-virtual {v2}, Landroid/app/AlertDialog;->getWindow()Landroid/view/Window;
        move-result-object v4
        if-eqz v4, :nai64_overlay_menu_done
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
        const/16 v5, -0x2
        invoke-virtual {v2, v5}, Landroid/app/AlertDialog;->getButton(I)Landroid/widget/Button;
        move-result-object v5
        const v6, 0x${Integer.toHexString(outlineColor)}
        invoke-virtual {v5, v6}, Landroid/widget/TextView;->setTextColor(I)V
        const/4 v6, 0x0
        invoke-virtual {v5, v6}, Landroid/widget/TextView;->setAllCaps(Z)V
        invoke-virtual {v5, v6}, Landroid/view/View;->setBackgroundColor(I)V
        const/16 v5, -0x3
        invoke-virtual {v2, v5}, Landroid/app/AlertDialog;->getButton(I)Landroid/widget/Button;
        move-result-object v5
        const v6, 0x${Integer.toHexString(outlineColor)}
        invoke-virtual {v5, v6}, Landroid/widget/TextView;->setTextColor(I)V
        const/4 v6, 0x0
        invoke-virtual {v5, v6}, Landroid/widget/TextView;->setAllCaps(Z)V
        invoke-virtual {v5, v6}, Landroid/view/View;->setBackgroundColor(I)V
        :nai64_overlay_menu_done
        return-void
    """))
    activity.methods.add(viewClick)

    val touch = newMethod(activity, "onTouch", listOf(
        "Landroid/view/View;",
        "Landroid/view/MotionEvent;",
    ), "Z", registers = 6)
    touch.addInstructionsWithLabels(0, compactSmali("""
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
        move-result v2
        iput v2, p0, ${activity.type}->${TOUCH_START_X}:F
        invoke-virtual {p2}, Landroid/view/MotionEvent;->getRawY()F
        move-result v3
        iput v3, p0, ${activity.type}->${TOUCH_START_Y}:F
        invoke-virtual {p2}, Landroid/view/MotionEvent;->getX()F
        move-result v4
        invoke-virtual {p2}, Landroid/view/MotionEvent;->getY()F
        move-result v5
        invoke-virtual {p2}, Landroid/view/MotionEvent;->getRawX()F
        move-result v2
        sub-float/2addr v2, v4
        iput v2, p0, ${activity.type}->${BUTTON_START_X}:F
        invoke-virtual {p2}, Landroid/view/MotionEvent;->getRawY()F
        move-result v3
        sub-float/2addr v3, v5
        iput v3, p0, ${activity.type}->${BUTTON_START_Y}:F
        const/4 v0, 0x1
        return v0
        :nai64_overlay_touch_move
        invoke-virtual {p2}, Landroid/view/MotionEvent;->getRawX()F
        move-result v2
        iget v3, p0, ${activity.type}->${TOUCH_START_X}:F
        sub-float/2addr v2, v3
        iget v3, p0, ${activity.type}->${BUTTON_START_X}:F
        add-float/2addr v2, v3
        invoke-virtual {p2}, Landroid/view/MotionEvent;->getRawY()F
        move-result v3
        iget v4, p0, ${activity.type}->${TOUCH_START_Y}:F
        sub-float/2addr v3, v4
        iget v4, p0, ${activity.type}->${BUTTON_START_Y}:F
        add-float/2addr v3, v4
        invoke-virtual {p1, v2}, Landroid/view/View;->setX(F)V
        invoke-virtual {p1, v3}, Landroid/view/View;->setY(F)V
        const/4 v0, 0x1
        return v0
        :nai64_overlay_touch_up
        invoke-virtual {p1}, Landroid/view/View;->performClick()Z
        const/4 v0, 0x1
        return v0
    """))
    activity.methods.add(touch)

    val dialogClick = newMethod(activity, "onClick", listOf(
        "Landroid/content/DialogInterface;",
        "I",
    ), "V")
    dialogClick.addInstructionsWithLabels(0, compactSmali("""
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
        const-string v1, "Overlay menu hidden. Tap the N button to open it again."
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
        invoke-virtual {p0, v0}, Landroid/app/Activity;->startActivity(Landroid/content/Intent;)V
        :nai64_overlay_done
        return-void
    """))
    activity.methods.add(dialogClick)

    val multiChoiceClick = newMethod(activity, "onClick", listOf(
        "Landroid/content/DialogInterface;",
        "I",
        "Z",
    ), "V", registers = 12)
    multiChoiceClick.addInstructionsWithLabels(0, compactSmali(buildControlHandler(
        activity.type,
        includeKeepScreenAwake,
        includeFullscreen,
        includeScreenshots,
    )))
    activity.methods.add(multiChoiceClick)
}

private fun buildMenuSetup(
    activityType: String,
    menuItems: List<String>,
    includeKeepScreenAwake: Boolean,
    includeFullscreen: Boolean,
    includeScreenshots: Boolean,
): String {
    if (menuItems.isEmpty()) return ""
    val lines = mutableListOf<String>()
    lines += "const/16 v3, ${menuItems.size}"
    lines += "new-array v3, v3, [Ljava/lang/CharSequence;"
    menuItems.forEachIndexed { index, item ->
        lines += "const-string v5, \"$item\""
        lines += "aput-object v5, v3, $index"
    }
    lines += "const/16 v4, ${menuItems.size}"
    lines += "new-array v4, v4, [Z"
    var index = 0
    if (includeKeepScreenAwake) {
        lines += readState(activityType, KEEP_SCREEN_AWAKE_STATE, index)
        index++
    }
    if (includeFullscreen) {
        lines += readState(activityType, FULLSCREEN_STATE, index)
        index++
    }
    if (includeScreenshots) {
        lines += readState(activityType, ALLOW_SCREENSHOTS_STATE, index)
    }
    lines += "move-object/from16 v5, p0"
    lines += "invoke-virtual/range {v2 .. v5}, Landroid/app/AlertDialog\$Builder;->setMultiChoiceItems([Ljava/lang/CharSequence;[ZLandroid/content/DialogInterface\$OnMultiChoiceClickListener;)Landroid/app/AlertDialog\$Builder;"
    return lines.joinToString("\n")
}

private fun readState(
    activityType: String,
    stateFieldName: String,
    index: Int,
): String = """
    iget-boolean v7, p0, $activityType->$stateFieldName:Z
    aput-boolean v7, v4, $index
""".trimIndent()

private fun buildControlHandler(
    activityType: String,
    includeKeepScreenAwake: Boolean,
    includeFullscreen: Boolean,
    includeScreenshots: Boolean,
): String {
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
    return (blocks + "return-void").joinToString("\n")
}

private fun controlBranch(activityType: String, index: Int, mask: String, kind: String): String = when (kind) {
    "keep", "screenshots" -> """
        const/16 v3, $index
        if-ne p2, v3, :nai64_next_control_$index
        iput-boolean p3, p0, $activityType->${if (kind == "keep") KEEP_SCREEN_AWAKE_STATE else ALLOW_SCREENSHOTS_STATE}:Z
        invoke-virtual {p0}, Landroid/app/Activity;->getWindow()Landroid/view/Window;
        move-result-object v4
        if-eqz p3, :nai64_restore_$index
        ${if (kind == "screenshots") "const v5, $mask\n        invoke-virtual {v4, v5}, Landroid/view/Window;->clearFlags(I)V" else "const v5, $mask\n        invoke-virtual {v4, v5}, Landroid/view/Window;->addFlags(I)V"}
        goto :nai64_control_done_$index
        :nai64_restore_$index
        iget v5, p0, $activityType->${ORIGINAL_WINDOW_FLAGS}:I
        const v6, $mask
        and-int/2addr v5, v6
        if-eqz v5, :nai64_clear_$index
        invoke-virtual {v4, v6}, Landroid/view/Window;->addFlags(I)V
        goto :nai64_control_done_$index
        :nai64_clear_$index
        invoke-virtual {v4, v6}, Landroid/view/Window;->clearFlags(I)V
        goto :nai64_control_done_$index
        :nai64_next_control_$index
    """.trimIndent()
    "fullscreen" -> """
        const/16 v3, $index
        if-ne p2, v3, :nai64_next_control_$index
        iput-boolean p3, p0, $activityType->${FULLSCREEN_STATE}:Z
        invoke-virtual {p0}, Landroid/app/Activity;->getWindow()Landroid/view/Window;
        move-result-object v4
        invoke-virtual {v4}, Landroid/view/Window;->getDecorView()Landroid/view/View;
        move-result-object v5
        if-eqz p3, :nai64_restore_$index
        const v6, 0x1706
        invoke-virtual {v5, v6}, Landroid/view/View;->setSystemUiVisibility(I)V
        goto :nai64_control_done_$index
        :nai64_restore_$index
        iget v6, p0, $activityType->${ORIGINAL_SYSTEM_UI}:I
        invoke-virtual {v5, v6}, Landroid/view/View;->setSystemUiVisibility(I)V
        goto :nai64_control_done_$index
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
    activity.type,
    name,
    parameterTypes.map { com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter(it, emptySet(), null) },
    returnType,
    accessFlags,
    emptySet(),
    emptySet(),
    ImmutableMethodImplementation(registers, emptyList(), emptyList(), emptyList()),
).toMutable()

private fun injectOverlay(
    onWindowFocusChanged: MutableMethod,
    activity: MutableClass,
    outlineColor: Int,
    buttonSizeDp: Int,
    buttonGravity: Int,
    includeKeepScreenAwake: Boolean,
    includeFullscreen: Boolean,
    includeScreenshots: Boolean,
) {
    val helperName = "nai64CreateRuntimeOverlay"
    val helper = newMethod(
        activity = activity,
        name = helperName,
        parameterTypes = listOf(activity.type),
        returnType = "V",
        registers = 15,
        accessFlags = AccessFlags.PRIVATE.value or AccessFlags.STATIC.value,
    )
    val initialState = buildInitialState(0, activity.type, includeKeepScreenAwake, includeFullscreen, includeScreenshots)
    helper.addInstructionsWithLabels(0, compactSmali("""
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
        const-string v1, "N"
        invoke-virtual {v0, v1}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V
        const v1, -0x1000000
        invoke-virtual {v0, v1}, Landroid/widget/TextView;->setTextColor(I)V
        sget-object v1, Landroid/graphics/Typeface;->DEFAULT:Landroid/graphics/Typeface;
        const/4 v2, 0x1
        invoke-virtual/range {v0 .. v2}, Landroid/widget/TextView;->setTypeface(Landroid/graphics/Typeface;I)V
        const/high16 v1, 0x40000000
        const/high16 v2, 0x3f800000
        const/high16 v3, 0x3f800000
        const v4, -0x1000000
        invoke-virtual/range {v0 .. v4}, Landroid/widget/TextView;->setShadowLayer(FFFI)V
        const/high16 v1, 0x3e800000
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
        const/4 v4, 0x1
        invoke-virtual {v3, v4}, Landroid/graphics/drawable/GradientDrawable;->setShape(I)V
        const v4, -0x1
        invoke-virtual {v3, v4}, Landroid/graphics/drawable/GradientDrawable;->setColor(I)V
        const/4 v4, 0x1
        const v5, $outlineColor
        invoke-virtual/range {v3 .. v5}, Landroid/graphics/drawable/GradientDrawable;->setStroke(II)V
        invoke-virtual {v0, v3}, Landroid/view/View;->setBackground(Landroid/graphics/drawable/Drawable;)V
        invoke-virtual {v0, p0}, Landroid/view/View;->setOnClickListener(Landroid/view/View${'$'}OnClickListener;)V
        invoke-virtual {v0, p0}, Landroid/view/View;->setOnTouchListener(Landroid/view/View${'$'}OnTouchListener;)V
        iput-object v0, p0, ${activity.type}->${OVERLAY_BUTTON}:$OVERLAY_BUTTON_FIELD
        new-instance v3, Landroid/widget/FrameLayout${'$'}LayoutParams;
        invoke-direct {v3, v2, v2}, Landroid/widget/FrameLayout${'$'}LayoutParams;-><init>(II)V
        const/16 v4, 0xc
        int-to-float v4, v4
        mul-float/2addr v4, v1
        float-to-int v4, v4
        move v5, v4
        move v6, v4
        move v7, v4
        invoke-virtual/range {v3 .. v7}, Landroid/view/ViewGroup${'$'}MarginLayoutParams;->setMargins(IIII)V
        const v1, $buttonGravity
        iput v1, v3, Landroid/widget/FrameLayout${'$'}LayoutParams;->gravity:I
        invoke-virtual {p0, v0, v3}, Landroid/app/Activity;->addContentView(Landroid/view/View;Landroid/view/ViewGroup${'$'}LayoutParams;)V
        :nai64_overlay_done
        return-void
    """))
    activity.methods.add(helper)
    val insertionIndex = onWindowFocusChanged.implementation!!.instructions
        .indexOfLast { it.opcode.name.startsWith("RETURN") }
        .coerceAtLeast(0)
    onWindowFocusChanged.addInstructions(
        insertionIndex,
        compactSmali("""
            invoke-static {p0}, ${activity.type}->$helperName(${activity.type})V
        """),
    )
}

private fun compactSmali(smali: String): String =
    smali.lines().filter(String::isNotBlank).joinToString("\n")

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