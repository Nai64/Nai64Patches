package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val unmeteredNetworkPatch = bytecodePatch(
    name = "Treat Network as Unmetered",
    description =
        "Makes ConnectivityManager.isActiveNetworkMetered always return false so apps lift " +
            "download restrictions and quality caps on mobile data",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Misc") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val patched = foldBooleanReturns(
            mapOf(
                "Landroid/net/ConnectivityManager;" to mapOf("isActiveNetworkMetered" to "0x0"),
            ),
        )

        if (patched > 0) logger.info("Forced $patched metered-network check(s) to false")
        else logger.warning("No metered-network checks found. No changes applied.")
    }
}
