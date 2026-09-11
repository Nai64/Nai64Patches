package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val hideInstalledAppsPatch = bytecodePatch(
    name = "Hide Installed Apps",
    description = "Hides other installed apps.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Hide") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = replaceGetterWithStaticCall(
            "Landroid/content/pm/PackageManager;",
            setOf("getInstalledPackages", "getInstalledApplications"),
            "Ljava/util/List;",
            "invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;",
        )
        if (patched > 0) logger.info("Redirected $patched package-list call(s) to emptyList()")
        else logger.warning("No package-list calls found. No changes applied.")
    }
}
