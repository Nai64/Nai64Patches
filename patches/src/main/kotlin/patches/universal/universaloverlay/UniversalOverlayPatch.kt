package patches.universal.universaloverlay

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.intOption
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import patches.universal.ads.util.cloneMutable
import patches.universal.ads.util.numberOfParameterRegisters
import patches.universal.ads.util.p0Register
import patches.universal.ui.StartupHooks
import java.util.Base64
import java.util.logging.Logger

private const val RUNTIME_CLASS = "Lnai64/universaloverlay/UniversalOverlayRuntime;"
private const val CONFIG_VERSION = "3"
private const val MAX_TITLE_CHARACTERS = 80
private const val MAX_DESCRIPTION_CHARACTERS = 500
private const val DEFAULT_DESCRIPTION =
    "Welcome! This is Nai64Patches Universal Overlay Patch Menu. This experimental overlay patch" +
        "contains optional statistic, activity, and hook modules. More of them may be added in " +
        "future updates. You will find modules below the description if you enabled some modules " +
        "in this patch settings before patching this APK." +
        "The idea and initial works of this Universal Overlay Patch are from Zanuaimi"

private fun encode(value: String): String =
    Base64.getEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))

private fun descriptor(value: String): String {
    val trimmed = value.trim()
    return if (trimmed.startsWith("L") && trimmed.endsWith(";")) trimmed
    else "L${trimmed.replace('.', '/')};"
}

private fun validate(
    title: String,
    description: String,
    label: String,
    url: String,
    background: String,
    outline: String,
    buttonTextColor: String,
    buttonBackground: String,
    shape: String,
    position: String,
    size: Int,
    opacity: Int,
) {
    check(title.isNotBlank() && title.length <= MAX_TITLE_CHARACTERS)
    check(description.isNotBlank() && description.length <= MAX_DESCRIPTION_CHARACTERS)
    check(label.isNotBlank() && label.length <= MAX_TITLE_CHARACTERS)
    check(url.startsWith("http://") || url.startsWith("https://"))
    fun validColor(value: String) = value.matches(Regex("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?"))
    check(validColor(background) && validColor(outline))
    check(validColor(buttonTextColor) && validColor(buttonBackground))
    check(shape in setOf("circle", "squircle", "square"))
    check(position in setOf("topLeft", "topMiddle", "topRight", "centerLeft", "centerRight", "bottomLeft", "bottomMiddle", "bottomRight"))
    check(size in 32..128)
    check(opacity in 10..100)
}

private fun injectMethod(owner: MutableClass, method: MutableMethod, config: String, application: Boolean) {
    /*
     * Keep the injected bridge independent of the target method's register layout. cloneMutable
     * moves the original parameters into the expanded register frame, leaving the registers at
     * the old registerCount free for our temporaries. Copy the receiver into the first temporary
     * and place the configuration immediately after it so the bridge can use invoke-static/range.
     *
     * An ordinary invoke-static has a five-register, four-bit register-list encoding. It can
     * therefore fail when a Unity or other game Activity has a large register frame and the new
     * temporary register is above v15. The range form is valid in the supported Morphe inline
     * compiler and addresses the high registers safely.
     */
    val temporaryBase = method.implementation?.registerCount
        ?: error("Cannot inject into ${owner.type}->${method.name} without an implementation")
    // cloneMutable shifts parameters upward by the number of added registers. Reserve those
    // parameter slots first, then reserve two registers for the receiver and configuration so
    // neither temporary can alias p0/p1 after cloning.
    val cloned = method.cloneMutable(additionalRegisters = method.numberOfParameterRegisters + 2)
    val originalReceiver = cloned.p0Register
    // The runtime API accepts the platform base type. The injected receiver may be any concrete
    // Activity subclass; using owner.type here would generate a method descriptor that does not
    // exist in UniversalOverlayRuntime and fail with NoSuchMethodError at launch.
    val type = if (application) "Landroid/app/Application;" else "Landroid/app/Activity;"
    val injectionIndex = if (application) {
        0
    } else {
        // Activity views cannot be attached reliably until the framework superclass has completed
        // onCreate. Place the fallback bridge after invoke-super so it works with AppCompat,
        // Unity, Godot, and ordinary platform Activity subclasses.
        val instructions = cloned.implementation?.instructions
        val superIndex = instructions?.indexOfFirst {
            val text = it.toString()
            text.contains("invoke-super") && text.contains("->onCreate(")
        } ?: -1
        if (superIndex >= 0) {
            superIndex + 1
        } else {
            // A non-standard Activity may omit invoke-super. Run at the end of onCreate so the
            // host still has a chance to initialize its content before overlay attachment.
            // Use the final existing instruction rather than relying on return-void text: some
            // dex instruction proxy implementations do not expose that text consistently.
            maxOf(0, (instructions?.size ?: 0) - 1)
        }
    }
    // Use the label-aware compiler entry point. Morphe Manager versions in the wild have
    // rejected range instructions through addInstructions even though the same Smali is valid
    // when compiled through addInstructionsWithLabels.
    cloned.addInstructionsWithLabels(
        injectionIndex,
        """
        move-object/from16 v$temporaryBase, v$originalReceiver
        const-string v${temporaryBase + 1}, "${StartupHooks.escapeSmali(config)}"
        invoke-static/range {v$temporaryBase .. v${temporaryBase + 1}}, $RUNTIME_CLASS->${if (application) "install" else "installActivity"}(${type}Ljava/lang/String;)V
        """.trimIndent(),
    )
    owner.methods.remove(method)
    owner.methods.add(cloned)
}

@Suppress("unused")
val universalOverlayPatch = bytecodePatch(
    name = "Universal Overlay Patch (SPECIAL PATCH, Experimental)",
    description =
        "A universal overlay for any supported APK, like ordinary Android apps and games from engines " +
            "such as Unity and Godot. This patch injects an overlay to patched APK. Modules provide runtime functionality inside the patched app: " +
            "Statistic modules can show phone System Time, approximate FPS, and App Session Time; " +
            "Activity modules can keep the screen awake, toggle fullscreen, or allow screenshots. " +
            "These actions are performed while the app is running, from the overlay menu, rather " +
            "than changing one specific APK's game or app logic. Hook modules, such as future " +
            "runtime ads controls, are reserved for future releases. Modules are not included by " +
            "default; select the modules you want before patching, then activate them in the overlay. " +
            "The title, description, colors, button text, shape, size, opacity, position, statistic " +
            "monitor placement, and repository action are customizable in morphe patch settings " + 
            " " + 
            "The idea and initial works of this Universal Overlay Patch are from Zanuaimi",
    default = false,
) {
    // TODO(universal-overlay): this extension DEX is the architectural boundary. Do not move UI or
    // feature implementation back into generated Smali.
    extendWith("extensions/extension.mpe")
    dependsOn(StartupHooks.resolveRealApplicationPatch)

    val title by stringOption(
        title = "General - Overlay title",
        default = "Nai64Patches Universal Overlay Patch",
        key = "runtimeOverlayTitle",
        description = "Title shown in the overlay menu. Limited to 80 characters.",
    )
    val descriptionText by stringOption(
        title = "General - Overlay description",
        default = DEFAULT_DESCRIPTION,
        key = "runtimeOverlayDescription",
        description = "Description below the title. Limited to 500 characters.",
    )
    val repositoryText by stringOption(
        title = "General - Repository button text",
        default = "Nai64 repository",
        key = "runtimeOverlayRepositoryText",
        description = "Text of the always-present repository button.",
    )
    val repositoryUrl by stringOption(
        title = "General - Repository button URL",
        default = "https://github.com/Nai64/Nai64Patches",
        key = "runtimeOverlayRepositoryUrl",
        description = "URL opened by the repository button.",
    )
    val backgroundColor by stringOption(
        title = "General - Overlay background color",
        default = "#CC101820",
        key = "runtimeOverlayBackgroundColor",
        description = "Overlay background as #RRGGBB or #AARRGGBB.",
    )
    val outlineColor by stringOption(
        title = "General - Overlay outline color",
        default = "#FF55D6BE",
        key = "runtimeOverlayOutlineColor",
        description = "Overlay outline as #RRGGBB or #AARRGGBB.",
    )
    val buttonText by stringOption(
        title = "General - Overlay button text",
        default = "N",
        key = "runtimeOverlayButtonText",
        description = "Button text. Maximum three characters.",
    )
    val buttonTextColor by stringOption(
        title = "General - Overlay button text color",
        default = "#FF000000",
        key = "runtimeOverlayButtonTextColor",
        description = "Button text color.",
    )
    val buttonBackgroundColor by stringOption(
        title = "General - Overlay button background color",
        default = "#FFFFFFFF",
        key = "runtimeOverlayButtonBackgroundColor",
        description = "Button background color.",
    )
    val buttonShape by stringOption(
        title = "General - Overlay button shape",
        default = "circle",
        key = "runtimeOverlayButtonShape",
        description = "Button shape.",
        values = linkedMapOf("Circle" to "circle", "Squircle" to "squircle", "Square" to "square"),
    )
    val buttonSizeDp by intOption(
        title = "General - Overlay button size (dp)",
        default = 56,
        key = "runtimeOverlayButtonSizeDp",
        description = "Button size in density-independent pixels.",
    )
    val buttonOpacity by intOption(
        title = "General - Overlay button idle opacity (%)",
        default = 35,
        key = "runtimeOverlayButtonIdleOpacityPercent",
        description = "Idle opacity from 10 to 100 percent.",
    )
    val buttonPosition by stringOption(
        title = "General - Overlay button position",
        default = "topRight",
        key = "runtimeOverlayButtonPosition",
        description = "Initial floating button position.",
        values = linkedMapOf(
            "Top left" to "topLeft", "Top middle" to "topMiddle", "Top right" to "topRight",
            "Center left" to "centerLeft", "Center right" to "centerRight",
            "Bottom left" to "bottomLeft", "Bottom middle" to "bottomMiddle", "Bottom right" to "bottomRight",
        ),
    )
    val activityOverride by stringOption(
        title = "Advanced - Overlay Activity name override",
        default = "",
        key = "runtimeOverlayActivityNameOverride",
        description = "Optional Activity fallback target.",
    )
    val activateStatisticsOnLaunch by booleanOption(
        title = "Settings to Modules - Activate statistics on launch",
        default = false,
        key = "runtimeOverlayActivateStatisticsOnLaunch",
        description = "Start selected statistic modules as soon as the app launches. Disabled by default.",
    )
    val statisticMonitorPosition by stringOption(
        title = "Settings to Modules - Statistic monitor position",
        default = "bottom",
        key = "runtimeOverlayStatisticMonitorPosition",
        description = "Show enabled statistic monitors from statistic modules above or below the overlay button.",
        values = linkedMapOf("No stat monitors" to "none", "Above overlay button" to "top", "Below overlay button" to "bottom"),
    )
    val includeSystemTime by booleanOption(
        title = "Statistic modules - System Time",
        default = false,
        key = "runtimeOverlayIncludeSystemTime",
        description = "Include the phone system time statistic module.",
    )
    val includeFps by booleanOption(
        title = "Statistic modules - FPS",
        default = false,
        key = "runtimeOverlayIncludeFps",
        description = "Include the approximate display frame-rate statistic module.",
    )
    val includeSessionTime by booleanOption(
        title = "Statistic modules - App Session Time",
        default = false,
        key = "runtimeOverlayIncludeSessionTime",
        description = "Include the in-process overlay session timer statistic module.",
    )
    val includeKeepAwake by booleanOption(
        title = "Activity modules - Keep screen awake",
        default = false,
        key = "runtimeOverlayIncludeKeepScreenAwake",
        description = "Include the keep-screen-awake activity module.",
    )
    val includeFullscreen by booleanOption(
        title = "Activity modules - Fullscreen",
        default = false,
        key = "runtimeOverlayIncludeFullscreen",
        description = "Include the fullscreen activity module.",
    )
    val includeScreenshots by booleanOption(
        title = "Activity modules - Allow screenshots",
        default = false,
        key = "runtimeOverlayIncludeScreenshots",
        description = "Include the allow-screenshots activity module.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val titleValue = title.orEmpty().ifBlank { "Nai64Patches Universal Overlay Patch" }
            .take(MAX_TITLE_CHARACTERS)
        val descriptionValue = descriptionText.orEmpty().ifBlank { DEFAULT_DESCRIPTION }
            .take(MAX_DESCRIPTION_CHARACTERS)
        val labelValue = repositoryText.orEmpty().ifBlank { "Nai64 repository" }
        val urlValue = repositoryUrl.orEmpty().ifBlank { "https://github.com/Nai64/Nai64Patches" }
        val sizeValue = (buttonSizeDp ?: 56).coerceIn(32, 128)
        val opacityValue = (buttonOpacity ?: 35).coerceIn(10, 100)
        val shapeValue = buttonShape.orEmpty().ifBlank { "circle" }
        val positionValue = buttonPosition.orEmpty().ifBlank { "topRight" }
        val backgroundValue = backgroundColor.orEmpty().ifBlank { "#CC101820" }
        val outlineValue = outlineColor.orEmpty().ifBlank { "#FF55D6BE" }
        val buttonTextColorValue = buttonTextColor.orEmpty().ifBlank { "#FF000000" }
        val buttonBackgroundValue = buttonBackgroundColor.orEmpty().ifBlank { "#FFFFFFFF" }
        val monitorPositionValue = statisticMonitorPosition.orEmpty().ifBlank { "bottom" }
        validate(
            titleValue, descriptionValue, labelValue, urlValue,
            backgroundValue, outlineValue, buttonTextColorValue, buttonBackgroundValue,
            shapeValue, positionValue, sizeValue, opacityValue,
        )
        check(monitorPositionValue in setOf("none", "top", "bottom"))
        check(buttonText.orEmpty().trim().length <= 3)

        val config = listOf(
            CONFIG_VERSION, titleValue, descriptionValue, labelValue, urlValue,
            backgroundValue, outlineValue, buttonText.orEmpty().trim().take(3).ifBlank { "N" },
            buttonTextColorValue, buttonBackgroundValue, shapeValue,
            sizeValue.toString(), opacityValue.toString(), positionValue,
            listOf(
                if (includeSystemTime == true) "systemTime" else null,
                if (includeFps == true) "fps" else null,
                if (includeSessionTime == true) "sessionTime" else null,
                if (includeKeepAwake == true) "keep" else null,
                if (includeFullscreen == true) "fullscreen" else null,
                if (includeScreenshots == true) "screenshots" else null,
            ).filterNotNull().joinToString(","),
            if (activateStatisticsOnLaunch == true) "1" else "0",
            monitorPositionValue,
        ).joinToString("|") { encode(it) }

        // TODO(universal-overlay): Application.onCreate is the primary hook; the Activity path is
        // only a compatibility fallback for APKs without a resolvable Application method.
        val appDescriptor = StartupHooks.resolvedApplicationDescriptor
        val appClass = appDescriptor?.let { mutableClassDefByOrNull(it) }
        val appMethod = appClass?.let { findInheritedApplicationOnCreate(it) }
        if (appMethod != null) {
            val (appOwner, appOnCreate) = appMethod
            if (appOnCreate.implementation?.instructions?.any { it.toString().contains(RUNTIME_CLASS) } == true) {
                logger.info("Runtime overlay bridge already exists in ${appOwner.type}->onCreate")
                return@execute
            }
            injectMethod(appOwner, appOnCreate, config, application = true)
            logger.info("Runtime overlay bridge injected into ${appOwner.type}->onCreate")
            return@execute
        }

        val fallback = activityOverride.orEmpty().trim().takeIf { it.isNotEmpty() }?.let(::descriptor)
            ?.let { target -> mutableClassDefByOrNull(target) }
            ?: findFallbackActivity()
        val onCreate = fallback?.methods?.firstOrNull {
            it.name == "onCreate" && it.returnType == "V" && it.parameterTypes == listOf("Landroid/os/Bundle;")
        }
        if (fallback != null && onCreate != null) {
            if (onCreate.implementation?.instructions?.any { it.toString().contains(RUNTIME_CLASS) } == true) {
                logger.info("Runtime overlay bridge already exists in ${fallback.type}->onCreate")
                return@execute
            }
            injectMethod(fallback, onCreate, config, application = false)
            logger.warning("Runtime overlay used Activity fallback: ${fallback.type}->onCreate")
        } else {
            logger.warning("No suitable Application or Activity entry point found. No changes applied.")
        }
    }
}

private fun app.morphe.patcher.patch.BytecodePatchContext.findFallbackActivity(): MutableClass? {
    val superMap = mutableMapOf<String, String>()
    classDefForEach { it.superclass?.let { parent -> superMap[it.type] = parent } }
    fun isActivity(type: String, seen: MutableSet<String> = mutableSetOf()): Boolean {
        if (type == "Landroid/app/Activity;") return true
        if (type == "Ljava/lang/Object;" || !seen.add(type)) return false
        return superMap[type]?.let { isActivity(it, seen) } == true
    }
    val override = StartupHooks.resolvedLauncherActivityDescriptor
    val candidates = mutableListOf<MutableClass>()
    classDefForEach { classDef ->
        if (!isActivity(classDef.type)) return@classDefForEach
        val candidate = mutableClassDefBy(classDef)
        if (candidate.methods.any { it.name == "onCreate" && it.returnType == "V" && it.parameterTypes == listOf("Landroid/os/Bundle;") }) {
            candidates += candidate
        }
    }
    return candidates.firstOrNull { it.type == override } ?: candidates.firstOrNull()
}

/**
 * Finds the implementation of Application.onCreate, including an implementation inherited by
 * the manifest-declared Application class. Mutating a bundled application superclass is safe here:
 * it is still the process Application entry point, whereas selecting an arbitrary Activity or SDK
 * class can leave the actual game screen without an overlay.
 */
private fun app.morphe.patcher.patch.BytecodePatchContext.findInheritedApplicationOnCreate(
    start: MutableClass,
): Pair<MutableClass, MutableMethod>? {
    val seen = mutableSetOf<String>()
    var current: MutableClass? = start
    while (current != null && seen.add(current.type)) {
        val method = current.methods.firstOrNull {
            it.name == "onCreate" && it.returnType == "V" && it.parameterTypes.isEmpty()
        }
        if (method != null) return current to method

        val superclass = current.superclass ?: return null
        if (superclass == "Landroid/app/Application;" || superclass == "Ljava/lang/Object;") return null
        current = mutableClassDefByOrNull(superclass)
    }
    return null
}
