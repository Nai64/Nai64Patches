package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableAnalyticsEventsPatch = bytecodePatch(
    name = "Disable Analytics Events",
    description = "Blocks app tracking and analytics.",
    default = false,
) {
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
