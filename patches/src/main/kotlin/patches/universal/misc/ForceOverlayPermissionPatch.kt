package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val forceOverlayPermissionPatch = bytecodePatch(
    name = "Force Overlay Permission",
    description = "Makes overlay permission checks report granted so floating features work without the system prompt.",
    default = false,
) {
    category("Force")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/provider/Settings;" to mapOf("canDrawOverlays" to "0x1")),
        )
        if (patched > 0) logger.info("Granted $patched overlay-permission check(s)")
        else logger.warning("No overlay-permission checks found. No changes applied.")
    }
}
