package patches.universal.privacy

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger
import patches.universal.misc.foldBooleanReturns

@Suppress("unused")
val bypassPermissionCheckPatch = bytecodePatch(
    name = "Bypass Permission Check",
    description = "Forces permission checks to report granted so permission-gated features skip dialogs",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        patched += foldBooleanReturns(
            mapOf(
                "Landroid/content/Context;" to mapOf(
                    "checkSelfPermission" to "0x0",
                    "checkCallingOrSelfPermission" to "0x0",
                    "checkCallingPermission" to "0x0",
                ),
                "Landroidx/core/content/ContextCompat;" to mapOf("checkSelfPermission" to "0x0"),
            ),
            returnType = "I",
        )
        if (patched > 0) logger.info("Bypassed permission check at $patched call site(s)")
        else logger.warning("No permission check calls found. No changes applied.")
    }
}
