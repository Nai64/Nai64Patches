package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val ignoreDataSaverPatch = bytecodePatch(
    name = "Ignore Data Saver",
    description = "Makes Data Saver checks report disabled so apps do not throttle downloads or quality on metered networks.",
    default = false,
) {
    category("Ignore")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldNoArgIntGetter(
            "Landroid/net/ConnectivityManager;",
            setOf("getRestrictBackgroundStatus"),
            1,
        )
        if (patched > 0) logger.info("Ignored $patched Data Saver check(s)")
        else logger.warning("No Data Saver checks found. No changes applied.")
    }
}
