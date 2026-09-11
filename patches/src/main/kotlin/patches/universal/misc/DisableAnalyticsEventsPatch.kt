package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableAnalyticsEventsPatch = bytecodePatch(
    name = "Disable Analytics Events",
    description = "Blocks app tracking and analytics.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Disable") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = noOpVoidInvoke(
            "Lcom/google/firebase/analytics/FirebaseAnalytics;",
            setOf("logEvent"),
        ) + noOpVoidInvoke(
            "Lcom/facebook/appevents/AppEventsLogger;",
            setOf("logEvent"),
        )
        if (patched > 0) logger.info("Neutralized $patched analytics logEvent() call(s)")
        else logger.warning("No analytics logEvent() calls found. No changes applied.")
    }
}
