package patches.universal.misc

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val fakeWriteSettingsAccessPatch = bytecodePatch(
    name = "Fake Write Settings Access",
    description = "Report system-settings write access as granted.",
    default = false,
) {
    val granted by booleanOption(
        title = "Grant access",
        default = true,
        key = "writeSettingsAccess",
        description = "Report write-settings access as granted (true) or denied (false).",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanGetterConst(
            "Landroid/provider/Settings\$System;",
            setOf("canWrite"),
            granted == true,
        )
        if (patched > 0) {
            logger.info("Faked write-settings access at $patched call site(s)")
        } else {
            logger.warning("No Settings.System.canWrite call sites found. No changes applied.")
        }
    }
}
