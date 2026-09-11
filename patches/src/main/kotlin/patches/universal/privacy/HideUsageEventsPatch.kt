package patches.universal.privacy

import app.morphe.patcher.patch.bytecodePatch
import patches.universal.misc.foldObjectGetterToNull
import java.util.logging.Logger

@Suppress("unused")
val hideUsageEventsPatch = bytecodePatch(
    name = "Hide Usage Events",
    description = "Makes usage events appear empty so apps cannot read your app launch history.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Privacy") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = foldObjectGetterToNull(
            "Landroid/app/usage/UsageStatsManager;",
            setOf("queryEvents"),
            "Landroid/app/usage/UsageEvents;",
        )
        patched += foldObjectGetterToNull(
            "Landroid/app/usage/UsageStatsManager;",
            setOf("queryEventStats"),
            "Ljava/util/List;",
        )
        patched += foldObjectGetterToNull(
            "Landroid/app/usage/UsageStatsManager;",
            setOf("queryConfigurations"),
            "Ljava/util/List;",
        )
        if (patched > 0) logger.info("Hid usage events at $patched call site(s)")
        else logger.warning("No usage event call sites found. No changes applied.")
    }
}
