package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofNetworkTypeLtePatch = bytecodePatch(
    name = "Spoof Network Type LTE",
    description = "Reports NETWORK_TYPE_LTE (13) from TelephonyManager.getNetworkType() so apps that require an LTE connection run on any network.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Spoof") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldNoArgIntGetter(
            "Landroid/telephony/TelephonyManager;",
            setOf("getNetworkType"),
            13,
        )
        if (patched > 0) logger.info("Spoofed network type at $patched call site(s)")
        else logger.warning("No getNetworkType call sites found. No changes applied.")
    }
}
