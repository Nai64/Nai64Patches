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
private const val CONFIG_VERSION = "8"
private const val MAX_TITLE_CHARACTERS = 80
private const val MAX_DESCRIPTION_CHARACTERS = 500
private val DEFAULT_DESCRIPTION =
    """
    Welcome! This is Nai64Patches Universal Overlay Patch Menu.
    You will find modules below the description if you enabled some modules
    in this patch settings before patching this APK.
    The idea and initial works of Universal Overlay Patch are from Zanuaimi / Noobite.
    """.trimIndent()

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
    outlineWidth: Int,
    iconOutlineColor: String,
    iconType: String,
    iconBackground2: String,
    iconGradientAngle: Int,
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
    check(outlineWidth in 1..8)
    check(validColor(iconOutlineColor) && validColor(iconBackground2))
    check(iconType in setOf("legacy", "image"))
    check(iconGradientAngle in 0..360)
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
    description = """
        Universal in-app overlay for Android apps and games. Optional modules include System Time, FPS,
        fullscreen, app brightness, and haptic controls. Modules are excluded and disabled by default;
        select them in Morphe settings before patching. Statistic modules show information, Activity modules
        control the current Activity, and Hook modules control internal app behavior, such as disabling
        animations, through best-effort runtime changes. Experimental : May not work on all apps!

        The idea and initial works of this Universal Overlay Patch are from Zanuaimi / Noobite.
    """.trimIndent(),
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
    val outlineWidth by intOption(
        title = "UI - Menu outline width (dp)",
        default = 2,
        key = "runtimeOverlayOutlineWidthDp",
        description = "Width of the menu, monitor, and confirmation outlines, from 1 to 8dp.",
    )
    val buttonText by stringOption(
        title = "UI - Legacy icon text",
        default = "N64",
        key = "runtimeOverlayButtonText",
        description = "Text shown by the legacy icon. Maximum three characters.",
    )
    val iconBold by booleanOption(
        title = "UI - Legacy icon bold text",
        default = true,
        key = "runtimeOverlayIconBold",
        description = "Use bold text in the legacy icon. Enabled by default.",
    )
    val buttonTextColor by stringOption(
        title = "UI - Legacy icon text color",
        default = "#FFFFFFFF",
        key = "runtimeOverlayButtonTextColor",
        description = "Text color used by the legacy icon.",
    )
    val buttonBackgroundColor by stringOption(
        title = "UI - Legacy icon background 1",
        default = "#000083",
        key = "runtimeOverlayButtonBackgroundColor",
        description = "First color of the legacy icon gradient.",
    )
    val iconBackground2 by stringOption(
        title = "UI - Legacy icon background 2",
        default = "#00AF7C",
        key = "runtimeOverlayIconBackgroundColor2",
        description = "Second color of the legacy icon gradient. Background 1 is the existing overlay button background color.",
    )
    val iconGradientAngle by intOption(
        title = "UI - Legacy icon gradient angle (degrees)",
        default = 30,
        key = "runtimeOverlayIconGradientAngle",
        description = "Gradient direction: 0 degrees runs top to bottom, 90 runs left to right, and 30 runs diagonally down and right. Values wrap through 360 degrees.",
    )
    val iconOutline by booleanOption(
        title = "UI - Icon outline",
        default = false,
        key = "runtimeOverlayIconOutline",
        description = "Add a separate outline around the legacy text icon. Disabled by default.",
    )
    val iconOutlineColor by stringOption(
        title = "UI - Icon outline color",
        default = "#FFFFFFFF",
        key = "runtimeOverlayIconOutlineColor",
        description = "Color used only when the icon outline is enabled.",
    )
    val iconType by stringOption(
        title = "UI - Icon type",
        default = "legacy",
        key = "runtimeOverlayIconType",
        description = "Legacy icon uses up to three bold text characters and a gradient background. Custom image replaces it and supports transparent images. Custom image requires a valid Base64 PNG or JPEG below; otherwise the legacy icon is used.",
        values = linkedMapOf("Legacy text icon" to "legacy", "Custom image icon" to "image"),
    )
    val customIconImage by stringOption(
        title = "UI - Custom image icon (Base64)",
        default = "",
        key = "runtimeOverlayCustomIconImage",
        description = "Select Custom image icon in UI - Icon type for this image input to be used. Use a trusted Base64 image encoder, such as https://base64.guru/converter/encode/image: choose a square PNG or JPEG, ideally 128x128 or 256x256 pixels, copy its encoded result, then paste it here instead of a file path. Example format: data:image/png;base64,<your encoded image data>. Transparent images are supported and other sizes are scaled proportionally. Leave blank to use the legacy icon; invalid or missing input falls back with a one-time launch notice.",
    )
    val buttonShape by stringOption(
        title = "UI - Overlay button shape",
        default = "circle",
        key = "runtimeOverlayButtonShape",
        description = "Shape of the legacy text icon background.",
        values = linkedMapOf("Circle" to "circle", "Squircle" to "squircle", "Square" to "square"),
    )
    val buttonSizeDp by intOption(
        title = "UI - Overlay button size (dp)",
        default = 56,
        key = "runtimeOverlayButtonSizeDp",
        description = "Button size in density-independent pixels.",
    )
    val buttonOpacity by intOption(
        title = "UI - Overlay button idle opacity (%)",
        default = 50,
        key = "runtimeOverlayButtonIdleOpacityPercent",
        description = "Idle opacity from 10 to 100 percent. Higher values make the button less transparent.",
    )
    val buttonDragVisibilityDurationSeconds by intOption(
        title = "UI - Overlay button fully visible duration (seconds)",
        default = 2,
        key = "runtimeOverlayButtonDragVisibilityDurationSeconds",
        description = "How long the overlay button stays fully visible after dragging before fading to its idle opacity. The timer resets while dragging and starts again when the finger is released. Use a value from 1 to 10 seconds.",
    )
    val buttonPosition by stringOption(
        title = "UI - Overlay button position",
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
        description = "Optional fallback Activity class used only when Application startup cannot be found. Leave blank for universal automatic discovery. Example: com.example.MainActivity or Lcom/example/MainActivity;.",
    )
    val activateStatisticsOnLaunch by booleanOption(
        title = "Settings to Modules - Activate statistic modules on launch",
        default = false,
        key = "runtimeOverlayActivateStatisticsOnLaunch",
        description = "Start selected statistic modules as soon as the app launches. Active is disabled by default.",
    )
    val enableMonitorsOnLaunch by booleanOption(
        title = "Settings to Modules - Enable monitors for statistic modules on launch",
        default = false,
        key = "runtimeOverlayEnableMonitorsOnLaunch",
        description = "Show selected statistic monitors as soon as the app launches. Monitor is disabled by default.",
    )
    val statisticMonitorPosition by stringOption(
        title = "Settings to Modules - Statistic monitor position",
        default = "bottom",
        key = "runtimeOverlayStatisticMonitorPosition",
        description = "Show enabled statistic monitors from statistic modules above or below the overlay button.",
        values = linkedMapOf("No stat monitors" to "none", "Above overlay button" to "top", "Below overlay button" to "bottom"),
    )
    val monitorScale by stringOption(
        title = "Settings to Modules - Monitor panel size",
        default = "1",
        key = "runtimeOverlayMonitorScale",
        description = "Size multiplier for statistic monitor panels.",
        values = linkedMapOf("0.75x" to "0.75", "1x" to "1", "1.25x" to "1.25", "1.5x" to "1.5", "2x" to "2"),
    )
    val monitorColumns by stringOption(
        title = "Settings to Modules - Monitor columns",
        default = "2",
        key = "runtimeOverlayMonitorColumns",
        description = "Number of statistic monitor columns.",
        values = linkedMapOf("1 column" to "1", "2 columns" to "2", "3 columns" to "3"),
    )
    val temperatureFormat by stringOption(
        title = "Settings to Modules - Temperature stat format",
        default = "celsius",
        key = "runtimeOverlayTemperatureFormat",
        description = "Temperature unit used by the Device Temperature menu value and monitor.",
        values = linkedMapOf("Celsius" to "celsius", "Fahrenheit" to "fahrenheit", "Kelvin" to "kelvin"),
    )
    val timeFormat by stringOption(
        title = "Settings to Modules - System time format",
        default = "12",
        key = "runtimeOverlayTimeFormat",
        description = "Clock format used by the System Time menu value and monitor. Timezone is shown in the menu value.",
        values = linkedMapOf("12-hour clock" to "12", "24-hour clock" to "24"),
    )
    val includeDeviceInformation by booleanOption(
        title = "Statistic modules - Device Information",
        default = false,
        key = "runtimeOverlayIncludeDeviceInformation",
        description = "Include read-only phone and Android device information.",
    )
    val includeFps by booleanOption(
        title = "Statistic modules - FPS",
        default = false,
        key = "runtimeOverlayIncludeFps",
        description = "Include the approximate display frame-rate statistic module.",
    )
    val includeDeviceTemperature by booleanOption(
        title = "Statistic modules - Device Temperature",
        default = false,
        key = "runtimeOverlayIncludeDeviceTemperature",
        description = "Include battery-reported temperature in the selected temperature format.",
    )
    val includeSystemTime by booleanOption(
        title = "Statistic modules - System Time",
        default = false,
        key = "runtimeOverlayIncludeSystemTime",
        description = "Include the phone system time statistic module.",
    )
    val includeSessionTime by booleanOption(
        title = "Statistic modules - App Session Time",
        default = false,
        key = "runtimeOverlayIncludeSessionTime",
        description = "Include the in-process overlay session timer statistic module.",
    )
    val includeBatteryStatus by booleanOption(
        title = "Statistic modules - Battery Status",
        default = false,
        key = "runtimeOverlayIncludeBatteryStatus",
        description = "Include the current battery percentage statistic module.",
    )
    val includeAppMemory by booleanOption(
        title = "Statistic modules - App Memory Usage",
        default = false,
        key = "runtimeOverlayIncludeAppMemory",
        description = "Include approximate memory used by the current app process.",
    )
    val includeNetworkStatus by booleanOption(
        title = "Statistic modules - Network Status",
        default = false,
        key = "runtimeOverlayIncludeNetworkStatus",
        description = "Include incoming and outgoing app network traffic monitors.",
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
    val includeAppBrightness by booleanOption(
        title = "Activity modules - App brightness",
        default = false,
        key = "runtimeOverlayIncludeAppBrightness",
        description = "Include a per-Activity brightness slider.",
    )
    val includeRotationMode by booleanOption(
        title = "Activity modules - Rotation mode",
        default = false,
        key = "runtimeOverlayIncludeRotationMode",
        description = "Include a per-Activity rotation mode selector.",
    )
    val includeAppAudioMute by booleanOption(
        title = "Activity modules - App audio mute",
        default = false,
        key = "runtimeOverlayIncludeAppAudioMute",
        description = "Include a best-effort app audio mute toggle.",
    )
    val includeDisableHaptics by booleanOption(
        title = "Hook modules - Disable haptic feedback / vibrations",
        default = false,
        key = "runtimeOverlayIncludeDisableHaptics",
        description = "Include a best-effort runtime haptic and vibration suppression module.",
    )
    val includeDisableAnimations by booleanOption(
        title = "Hook modules - Disable app animations",
        default = false,
        key = "runtimeOverlayIncludeDisableAnimations",
        description = "Include a best-effort runtime animation suppression module.",
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
        val opacityValue = (buttonOpacity ?: 50).coerceIn(10, 100)
        val dragVisibilityDurationValue = (buttonDragVisibilityDurationSeconds ?: 2).coerceIn(1, 10)
        val shapeValue = buttonShape.orEmpty().ifBlank { "circle" }
        val positionValue = buttonPosition.orEmpty().ifBlank { "topRight" }
        val backgroundValue = backgroundColor.orEmpty().ifBlank { "#CC101820" }
        val outlineValue = outlineColor.orEmpty().ifBlank { "#FF55D6BE" }
        val buttonTextColorValue = buttonTextColor.orEmpty().ifBlank { "#FFFFFFFF" }
        val buttonBackgroundValue = buttonBackgroundColor.orEmpty().ifBlank { "#000083" }
        val outlineWidthValue = (outlineWidth ?: 1).coerceIn(1, 8)
        val iconOutlineColorValue = iconOutlineColor.orEmpty().ifBlank { "#FFFFFFFF" }
        val iconTypeValue = iconType.orEmpty().ifBlank { "legacy" }
        val iconBackground2Value = iconBackground2.orEmpty().ifBlank { "#00AF7C" }
        val iconGradientAngleValue = ((iconGradientAngle ?: 30) % 361 + 361) % 361
        val customIconImageValue = customIconImage.orEmpty().trim()
        val monitorPositionValue = statisticMonitorPosition.orEmpty().ifBlank { "bottom" }
        val monitorScaleValue = monitorScale.orEmpty().ifBlank { "1" }
        val monitorColumnsValue = monitorColumns.orEmpty().ifBlank { "2" }
        val temperatureFormatValue = temperatureFormat.orEmpty().ifBlank { "celsius" }
        val timeFormatValue = timeFormat.orEmpty().ifBlank { "12" }
        validate(
            titleValue, descriptionValue, labelValue, urlValue,
            backgroundValue, outlineValue, buttonTextColorValue, buttonBackgroundValue,
            outlineWidthValue, iconOutlineColorValue, iconTypeValue, iconBackground2Value,
            iconGradientAngleValue,
            shapeValue, positionValue, sizeValue, opacityValue,
        )
        check(monitorPositionValue in setOf("none", "top", "bottom"))
        check(monitorScaleValue.toFloatOrNull() in listOf(.75f, 1f, 1.25f, 1.5f, 2f))
        check(monitorColumnsValue in setOf("1", "2", "3"))
        check(temperatureFormatValue in setOf("celsius", "fahrenheit", "kelvin"))
        check(timeFormatValue in setOf("12", "24"))
        check(buttonText.orEmpty().trim().length <= 3)

        val config = listOf(
            CONFIG_VERSION, titleValue, descriptionValue, labelValue, urlValue,
            backgroundValue, outlineValue, buttonText.orEmpty().trim().take(3).ifBlank { "N64" },
            buttonTextColorValue, buttonBackgroundValue, shapeValue,
            sizeValue.toString(), opacityValue.toString(), positionValue,
            listOf(
                if (includeSystemTime == true) "systemTime" else null,
                if (includeFps == true) "fps" else null,
                if (includeSessionTime == true) "sessionTime" else null,
                if (includeBatteryStatus == true) "batteryStatus" else null,
                if (includeAppMemory == true) "appMemory" else null,
                if (includeNetworkStatus == true) "networkStatus" else null,
                if (includeDeviceInformation == true) "deviceInformation" else null,
                if (includeDeviceTemperature == true) "deviceTemperature" else null,
                if (includeKeepAwake == true) "keep" else null,
                if (includeFullscreen == true) "fullscreen" else null,
                if (includeScreenshots == true) "screenshots" else null,
                if (includeAppBrightness == true) "appBrightness" else null,
                if (includeRotationMode == true) "rotationMode" else null,
                if (includeAppAudioMute == true) "appAudioMute" else null,
                if (includeDisableHaptics == true) "disableHaptics" else null,
                if (includeDisableAnimations == true) "disableAnimations" else null,
            ).filterNotNull().joinToString(","),
            if (activateStatisticsOnLaunch == true) "1" else "0",
            if (enableMonitorsOnLaunch == true) "1" else "0",
            monitorPositionValue,
            monitorScaleValue,
            monitorColumnsValue,
            temperatureFormatValue,
            timeFormatValue,
            outlineWidthValue.toString(),
            if (iconOutline == true) "1" else "0",
            iconOutlineColorValue,
            iconTypeValue,
            if (iconBold != false) "1" else "0",
            iconBackground2Value,
            iconGradientAngleValue.toString(),
            customIconImageValue,
            dragVisibilityDurationValue.toString(),
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
