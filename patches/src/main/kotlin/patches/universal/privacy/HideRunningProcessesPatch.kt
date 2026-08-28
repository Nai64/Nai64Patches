package patches.universal.privacy

import app.morphe.patcher.patch.bytecodePatch
import patches.universal.misc.replaceGetterWithStaticCall
import java.util.logging.Logger

@Suppress("unused")
val hideRunningProcessesPatch = bytecodePatch(
    name = "Hide Running Processes",
    description = "Makes the app see no running processes so it cannot fingerprint other apps.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = replaceGetterWithStaticCall(
            "Landroid/app/ActivityManager;",
            setOf("getRunningAppProcesses"),
            "Ljava/util/List;",
            "invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;",
        )
        if (patched > 0) logger.info("Hid running processes at $patched call site(s)")
        else logger.warning("No getRunningAppProcesses call sites found. No changes applied.")
    }
}
