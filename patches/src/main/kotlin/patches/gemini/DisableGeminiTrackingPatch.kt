package patches.gemini

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger
import patches.universal.misc.noOpVoidInvoke

@Suppress("unused")
val disableGeminiTrackingPatch = bytecodePatch(
    name = "Disable Gemini Tracking",
    description = "Gemini: disables Clearcut/Primes and Firebase analytics pings.",
    default = false,
) {
    compatibleWith("com.google.android.apps.bard")

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        patched += noOpVoidInvoke("Lcom/google/firebase/analytics/FirebaseAnalytics;", setOf("logEvent", "setAnalyticsCollectionEnabled"))
        patched += noOpVoidInvoke("Lcom/google/android/gms/clearcut/ClearcutLogger;", setOf("log", "logAsync"))
        patched += noOpVoidInvoke("Lcom/google/android/libraries/performance/primes/metrics/storage/PackageStatsCapture;", setOf("log", "record"))
        if (patched > 0) logger.info("Disabled $patched tracking call(s)")
        else logger.warning("No tracking calls found. No changes applied.")
    }
}
