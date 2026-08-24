package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val hideInstalledAppsPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Redirects PackageManager.getInstalledPackages/getInstalledApplications to Collections.emptyList() so the app sees no other installed apps.",
    default = false,
) {
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
