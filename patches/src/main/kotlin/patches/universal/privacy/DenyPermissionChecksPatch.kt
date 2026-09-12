package patches.universal.privacy

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger
import patches.universal.misc.foldBooleanReturns

// 0xf encodes -1 in const/4, which is PackageManager.PERMISSION_DENIED.
// This mirrors BypassPermissionCheckPatch but reports denied instead of granted.
@Suppress("unused")
val denyPermissionChecksPatch = bytecodePatch(
    name = "Deny Permission Checks",
    description = "Forces permission checks to report denied so the app cannot silently access permission-gated data",
    default = false,
) {
    category("Privacy")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        patched += foldBooleanReturns(
            mapOf(
                "Landroid/content/Context;" to mapOf(
                    "checkSelfPermission" to "0xf",
                    "checkCallingOrSelfPermission" to "0xf",
                    "checkCallingPermission" to "0xf",
                ),
                "Landroidx/core/content/ContextCompat;" to mapOf("checkSelfPermission" to "0xf"),
            ),
            returnType = "I",
        )
        if (patched > 0) logger.info("Denied permission check at $patched call site(s)")
        else logger.warning("No permission check calls found. No changes applied.")
    }
}
