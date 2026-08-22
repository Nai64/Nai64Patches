package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger

@Suppress("unused")
val spoofAndroidIdPatch = bytecodePatch(
    name = "Spoof Android ID",
    description =
        "Replaces Settings.Secure.ANDROID_ID reads with a fixed value so the patched " +
            "app cannot be tracked across reinstalls or matched to a specific device.",
    default = false,
) {
    val androidId by stringOption(
        title = "Android ID",
        default = "0123456789abcdef",
        key = "spoofAndroidIdValue",
        description = "Hex string to report as ANDROID_ID (typically 16 characters).",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val value = (androidId ?: "0123456789abcdef").trim()
        val patched = foldSettingsGetterConst(
            classes = setOf("Landroid/provider/Settings\$Secure;"),
            keys = setOf("android_id"),
            stringValue = value,
        )

        if (patched > 0) {
            logger.info("Spoofed $patched Android ID read(s)")
        } else {
            logger.warning("No Android ID reads found. No changes applied.")
        }
    }
}
