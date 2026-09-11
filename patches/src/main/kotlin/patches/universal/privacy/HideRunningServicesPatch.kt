package patches.universal.privacy

import app.morphe.patcher.patch.bytecodePatch
import patches.universal.misc.replaceGetterWithStaticCall
import java.util.logging.Logger

@Suppress("unused")
val hideRunningServicesPatch = bytecodePatch(
    name = "Hide Running Services",
    description = "Makes the app see no running services so it cannot detect other services.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Privacy") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = replaceGetterWithStaticCall(
            "Landroid/app/ActivityManager;",
            setOf("getRunningServices"),
            "Ljava/util/List;",
            "invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;",
        )
        if (patched > 0) logger.info("Hid running services at $patched call site(s)")
        else logger.warning("No getRunningServices call sites found. No changes applied.")
    }
}
