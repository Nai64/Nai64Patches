package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger

@Suppress("unused")
val spoofLocalePatch = bytecodePatch(
    name = "Spoof Locale",
    description =
        "Forces Locale.getDefault() to return a chosen BCP-47 language tag (e.g. en-US) " +
            "so region- or language-locked apps see the locale you pick.",
    default = false,
) {
    val localeTag by stringOption(
        title = "Locale tag",
        default = "en-US",
        key = "spoofLocaleTag",
        description = "BCP-47 language tag to report (e.g. en-US, ja-JP, en-GB).",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val tag = (localeTag ?: "en-US").trim()
        val patched = foldLocaleGetDefault(tag)

        if (patched > 0) {
            logger.info("Spoofed $patched Locale.getDefault() call(s)")
        } else {
            logger.warning("No Locale.getDefault() calls found. No changes applied.")
        }
    }
}
