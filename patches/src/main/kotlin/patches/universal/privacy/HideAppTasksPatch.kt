package patches.universal.privacy

import app.morphe.patcher.patch.bytecodePatch
import patches.universal.misc.replaceGetterWithStaticCall
import java.util.logging.Logger

@Suppress("unused")
val hideAppTasksPatch = bytecodePatch(
    name = "Hide App Tasks",
    description = "Makes the app see no recent tasks so it cannot read your recent apps.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = replaceGetterWithStaticCall(
            "Landroid/app/ActivityManager;",
            setOf("getAppTasks"),
            "Ljava/util/List;",
            "invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;",
        )
        patched += replaceGetterWithStaticCall(
            "Landroid/app/ActivityManager;",
            setOf("getRecentTasks", "getRunningTasks"),
            "Ljava/util/List;",
            "invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;",
        )
        if (patched > 0) logger.info("Hid app tasks at $patched call site(s)")
        else logger.warning("No app task call sites found. No changes applied.")
    }
}
