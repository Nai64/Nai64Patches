package patches.universal.ui

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.booleanOption
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
    val includeKeepScreenAwake by booleanOption(
        title = "Include keep screen awake control",
        default = false,
        key = "runtimeOverlayIncludeKeepScreenAwake",
        description =
            "Include the APK hook and overlay switch for keeping the screen awake. Default " +
                "runtime state: Off, matching original app behavior.",
    )
    val includeFullscreen by booleanOption(
        title = "Include fullscreen control",
        default = false,
        key = "runtimeOverlayIncludeFullscreen",
        description =
            "Include the APK hook and overlay switch for fullscreen mode. Default runtime state: " +
                "Off, matching original app behavior.",
    )
    val includeScreenshots by booleanOption(
        title = "Include allow screenshots control",
        default = false,
        key = "runtimeOverlayIncludeScreenshots",
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

        classDefForEach { classDef ->
            if (!isActivity(classDef.type)) return@classDefForEach
            val activity = mutableClassDefBy(classDef)
            val onCreate = activity.methods.firstOrNull {
                it.name == "onCreate" && it.returnType == "V" &&
                    it.parameterTypes == listOf("Landroid/os/Bundle;")
            } ?: return@classDefForEach

            if (activity.methods.any {
                    it.name == "onClick" && it.parameterTypes == listOf("Landroid/view/View;")
                } || activity.methods.any {
                    it.name == "onClick" && it.parameterTypes == listOf(
                        "Landroid/content/DialogInterface;",
                        "I",
                    )
                }) {
                return@classDefForEach
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
                onCreate,
                activity,
                outline,
                includeKeepScreenAwake == true,
                includeFullscreen == true,
                includeScreenshots == true,
            )
            patched++
        }

        if (patched > 0) {
            logger.info(
                "Injected experimental runtime controls overlay into $patched activit(ies); " +
                    "selected controls: ${selectedControls.joinToString().ifEmpty { "none" }}",
            )
        } else {
            logger.warning("No compatible Activity onCreate methods found. No changes applied.")
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
    viewClick.addInstructionsWithLabels(0, """
        new-instance v2, Landroid/app/AlertDialog${'$'}Builder;
        invoke-direct {v2, p0}, Landroid/app/AlertDialog${'$'}Builder;-><init>(Landroid/content/Context;)V
        const-string v3, "${StartupHooks.escapeSmali(title)}"
        invoke-virtual {v2, v3}, Landroid/app/AlertDialog${'$'}Builder;->setTitle(Ljava/lang/CharSequence;)Landroid/app/AlertDialog${'$'}Builder;
        const-string v3, "${StartupHooks.escapeSmali(description)}"
        invoke-virtual {v2, v3}, Landroid/app/AlertDialog${'$'}Builder;->setMessage(Ljava/lang/CharSequence;)Landroid/app/AlertDialog${'$'}Builder;
        const-string v3, "${StartupHooks.escapeSmali(repositoryLabel)}"
        invoke-virtual {v2, v3, p0}, Landroid/app/AlertDialog${'$'}Builder;->setNeutralButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface${'$'}OnClickListener;)Landroid/app/AlertDialog${'$'}Builder;
        $menuSetup
        const-string v3, "Hide overlay"
        invoke-virtual {v2, v3, p0}, Landroid/app/AlertDialog${'$'}Builder;->setNegativeButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface${'$'}OnClickListener;)Landroid/app/AlertDialog${'$'}Builder;
        const-string v3, "Close overlay"
        invoke-virtual {v2, v3, p0}, Landroid/app/AlertDialog${'$'}Builder;->setPositiveButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface${'$'}OnClickListener;)Landroid/app/AlertDialog${'$'}Builder;
        invoke-virtual {v2}, Landroid/app/AlertDialog${'$'}Builder;->show()Landroid/app/AlertDialog;
        move-result-object v2
        invoke-virtual {v2}, Landroid/app/AlertDialog;->getWindow()Landroid/view/Window;
        move-result-object v4
        if-eqz v4, :nai64_overlay_menu_done
        new-instance v5, Landroid/graphics/drawable/GradientDrawable;
        invoke-direct {v5}, Landroid/graphics/drawable/GradientDrawable;-><init>()V
        const v6, $backgroundColor
        invoke-virtual {v5, v6}, Landroid/graphics/drawable/GradientDrawable;->setColor(I)V
        const/4 v6, 0x1
        const v7, $outlineColor
        invoke-virtual/range {v5 .. v7}, Landroid/graphics/drawable/GradientDrawable;->setStroke(II)V
        invoke-virtual {v4, v5}, Landroid/view/Window;->setBackgroundDrawable(Landroid/graphics/drawable/Drawable;)V
        :nai64_overlay_menu_done
        return-void
    """.trimIndent())
    activity.methods.add(viewClick)

    val dialogClick = newMethod(activity, "onClick", listOf(
        "Landroid/content/DialogInterface;",
        "I",
    ), "V")
    dialogClick.addInstructionsWithLabels(0, """
        const/16 v2, -0x3
        if-eq p1, v2, :nai64_overlay_repository
        iget-object v0, p0, ${activity.type}->${OVERLAY_BUTTON}:$OVERLAY_BUTTON_FIELD
        if-eqz v0, :nai64_overlay_done
        const/16 v1, 0x4
        invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
        const/16 v2, -0x1
        if-ne p1, v2, :nai64_overlay_toast
        invoke-virtual {v0}, Landroid/view/View;->getParent()Landroid/view/ViewParent;
        move-result-object v1
        instance-of v2, v1, Landroid/view/ViewGroup;
        if-eqz v2, :nai64_overlay_toast
        check-cast v1, Landroid/view/ViewGroup;
        invoke-virtual {v1, v0}, Landroid/view/ViewGroup;->removeView(Landroid/view/View;)V
        goto :nai64_overlay_done
        :nai64_overlay_toast
        const-string v1, "Overlay hidden. Remember its position before hiding it."
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
    """.trimIndent())
    activity.methods.add(dialogClick)

    val multiChoiceClick = newMethod(activity, "onClick", listOf(
        "Landroid/content/DialogInterface;",
        "I",
        "Z",
    ), "V", registers = 12)
    multiChoiceClick.addInstructionsWithLabels(0, buildControlHandler(
        activity.type,
        includeKeepScreenAwake,
        includeFullscreen,
        includeScreenshots,
    ))
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
        if-ne p1, v3, :nai64_next_control_$index
        iput-boolean p2, p0, $activityType->${if (kind == "keep") KEEP_SCREEN_AWAKE_STATE else ALLOW_SCREENSHOTS_STATE}:Z
        invoke-virtual {p0}, Landroid/app/Activity;->getWindow()Landroid/view/Window;
        move-result-object v4
        if-eqz p2, :nai64_restore_$index
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
        if-ne p1, v3, :nai64_next_control_$index
        iput-boolean p2, p0, $activityType->${FULLSCREEN_STATE}:Z
        invoke-virtual {p0}, Landroid/app/Activity;->getWindow()Landroid/view/Window;
        move-result-object v4
        invoke-virtual {v4}, Landroid/view/Window;->getDecorView()Landroid/view/View;
        move-result-object v5
        if-eqz p2, :nai64_restore_$index
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
): MutableMethod = ImmutableMethod(
    activity.type,
    name,
    parameterTypes.map { com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter(it, emptySet(), null) },
    returnType,
    AccessFlags.PUBLIC.value,
    emptySet(),
    emptySet(),
    ImmutableMethodImplementation(registers, emptyList(), emptyList(), emptyList()),
).toMutable()

private fun injectOverlay(
    onCreate: MutableMethod,
    activity: MutableClass,
    outlineColor: Int,
    includeKeepScreenAwake: Boolean,
    includeFullscreen: Boolean,
    includeScreenshots: Boolean,
) {
    val base = onCreate.implementation!!.registerCount
    val cloned = onCreate.cloneMutable(additionalRegisters = 11)
    val context = cloned.p0Register
    val initialState = buildInitialState(base, activity.type, includeKeepScreenAwake, includeFullscreen, includeScreenshots)
    cloned.addInstructionsWithLabels(0, """
        invoke-virtual {p0}, Landroid/app/Activity;->getWindow()Landroid/view/Window;
        move-result-object v${base + 6}
        invoke-virtual {v${base + 6}}, Landroid/view/Window;->getAttributes()Landroid/view/WindowManager${'$'}LayoutParams;
        move-result-object v${base + 7}
        iget v${base + 8}, v${base + 7}, Landroid/view/WindowManager${'$'}LayoutParams;->flags:I
        iput v${base + 8}, p0, ${activity.type}->${ORIGINAL_WINDOW_FLAGS}:I
        invoke-virtual {v${base + 6}}, Landroid/view/Window;->getDecorView()Landroid/view/View;
        move-result-object v${base + 9}
        invoke-virtual {v${base + 9}}, Landroid/view/View;->getSystemUiVisibility()I
        move-result v${base + 10}
        iput v${base + 10}, p0, ${activity.type}->${ORIGINAL_SYSTEM_UI}:I
        $initialState
        new-instance v$base, Landroid/widget/TextView;
        invoke-direct {v$base, v$context}, Landroid/widget/TextView;-><init>(Landroid/content/Context;)V
        const-string v${base + 1}, "N"
        invoke-virtual {v$base, v${base + 1}}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V
        const v${base + 1}, -0x1000000
        invoke-virtual {v$base, v${base + 1}}, Landroid/widget/TextView;->setTextColor(I)V
        sget-object v${base + 1}, Landroid/graphics/Typeface;->DEFAULT:Landroid/graphics/Typeface;
        const/4 v${base + 2}, 0x1
        invoke-virtual/range {v$base .. v${base + 2}}, Landroid/widget/TextView;->setTypeface(Landroid/graphics/Typeface;I)V
        const/high16 v${base + 1}, 0x40000000
        const/high16 v${base + 2}, 0x3f800000
        const/high16 v${base + 3}, 0x3f800000
        const v${base + 4}, -0x1000000
        invoke-virtual/range {v$base .. v${base + 4}}, Landroid/widget/TextView;->setShadowLayer(FFFI)V
        const/high16 v${base + 1}, 0x3e800000
        invoke-virtual {v$base, v${base + 1}}, Landroid/view/View;->setAlpha(F)V
        const/16 v${base + 1}, 0x38
        invoke-virtual {v$base, v${base + 1}}, Landroid/widget/TextView;->setWidth(I)V
        const/16 v${base + 1}, 0x38
        invoke-virtual {v$base, v${base + 1}}, Landroid/widget/TextView;->setHeight(I)V
        const/16 v${base + 1}, 0x11
        invoke-virtual {v$base, v${base + 1}}, Landroid/widget/TextView;->setGravity(I)V
        new-instance v${base + 3}, Landroid/graphics/drawable/GradientDrawable;
        invoke-direct {v${base + 3}}, Landroid/graphics/drawable/GradientDrawable;-><init>()V
        const/4 v${base + 4}, 0x1
        invoke-virtual {v${base + 3}, v${base + 4}}, Landroid/graphics/drawable/GradientDrawable;->setShape(I)V
        const v${base + 4}, -0x1
        invoke-virtual {v${base + 3}, v${base + 4}}, Landroid/graphics/drawable/GradientDrawable;->setColor(I)V
        const/4 v${base + 4}, 0x1
        const v${base + 5}, $outlineColor
        invoke-virtual/range {v${base + 3} .. v${base + 5}}, Landroid/graphics/drawable/GradientDrawable;->setStroke(II)V
        invoke-virtual {v$base, v${base + 3}}, Landroid/view/View;->setBackground(Landroid/graphics/drawable/Drawable;)V
        invoke-virtual {v$base, p0}, Landroid/view/View;->setOnClickListener(Landroid/view/View${'$'}OnClickListener;)V
        iput-object v$base, p0, ${activity.type}->${OVERLAY_BUTTON}:$OVERLAY_BUTTON_FIELD
        const/16 v${base + 1}, 0x38
        new-instance v${base + 2}, Landroid/view/ViewGroup${'$'}LayoutParams;
        invoke-direct {v${base + 2}, v${base + 1}, v${base + 1}}, Landroid/view/ViewGroup${'$'}LayoutParams;-><init>(II)V
        invoke-virtual {p0, v$base, v${base + 2}}, Landroid/app/Activity;->addContentView(Landroid/view/View;Landroid/view/ViewGroup${'$'}LayoutParams;)V
    """.trimIndent())
    activity.methods.remove(onCreate)
    activity.methods.add(cloned)
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