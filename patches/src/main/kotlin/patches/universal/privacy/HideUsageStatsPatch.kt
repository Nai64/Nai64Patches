package patches.universal.privacy

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger
import patches.universal.misc.replaceGetterWithStaticCall

@Suppress("unused")
val hideUsageStatsPatch = bytecodePatch(
    name = "Hide Usage Stats",
    description = "Makes UsageStatsManager return empty lists so app usage fingerprinting sees nothing",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Privacy") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        patched += replaceGetterWithStaticCall(
            definingClass = "Landroid/app/usage/UsageStatsManager;",
            methodNames = setOf("queryUsageStats", "queryAndAggregateUsageStats"),
            returnType = "Ljava/util/List;",
            replacementInvoke = "invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;",
        )
        patched += replaceGetterWithStaticCall(
            definingClass = "Landroid/app/usage/UsageStatsManager;",
            methodNames = setOf("queryUsageStats", "queryAndAggregateUsageStats"),
            returnType = "Ljava/util/Map;",
            replacementInvoke = "invoke-static {}, Ljava/util/Collections;->emptyMap()Ljava/util/Map;",
        )
        if (patched > 0) logger.info("Hid usage stats at $patched call site(s)")
        else logger.warning("No UsageStatsManager query calls found. No changes applied.")
    }
}
