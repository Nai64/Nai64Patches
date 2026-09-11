package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val bypassDemoUserPatch = bytecodePatch(
    name = "Bypass Demo User",
    description = "Makes UserManager.isDemoUser() report false so apps that gate functionality in retail/demo mode run normally.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Bypass") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/os/UserManager;" to mapOf("isDemoUser" to "0x0")),
        )
        if (patched > 0) logger.info("Forced $patched demo-user check(s) to false")
        else logger.warning("No demo-user checks found. No changes applied.")
    }
}
