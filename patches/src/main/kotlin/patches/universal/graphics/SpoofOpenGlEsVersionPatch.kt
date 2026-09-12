package patches.universal.graphics

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import patches.universal.misc.foldStringGetterConst
import java.util.logging.Logger

@Suppress("unused")
val spoofOpenGlEsVersionPatch = bytecodePatch(
    name = "Spoof OpenGL ES Version",
    description = "Reports a chosen GLES version so games unlock higher graphics options.",
    default = false,
) {
    category("Graphics")
    val glesVersion by stringOption(
        key = "glesVersion",
        title = "GLES version",
        description = "Reported ConfigurationInfo.getGlEsVersion value.",
        default = "3.2",
        values = linkedMapOf(
            "3.2" to "3.2",
            "3.1" to "3.1",
            "3.0" to "3.0",
            "2.0" to "2.0",
        ),
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val version = glesVersion.orEmpty().trim()
        if (version.isEmpty()) {
            logger.warning("No GLES version configured. No changes applied.")
            return@execute
        }
        val patched = foldStringGetterConst(
            definingClass = "Landroid/content/pm/ConfigurationInfo;",
            methodNames = setOf("getGlEsVersion"),
            value = version,
        )
        if (patched > 0) logger.info("Spoofed $patched GLES version check(s) to $version")
        else logger.warning("No GLES version checks found. No changes applied.")
    }
}
