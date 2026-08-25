package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val hideDeviceAdminsPatch = bytecodePatch(
    name = "Hide Device Admins",
    description = "Redirects DevicePolicyManager.getActiveAdmins() to Collections.emptyList() so the app sees no active device admins.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = replaceGetterWithStaticCall(
            "Landroid/app/admin/DevicePolicyManager;",
            setOf("getActiveAdmins"),
            "Ljava/util/List;",
            "invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;",
        )
        if (patched > 0) logger.info("Redirected $patched getActiveAdmins() call(s) to emptyList()")
        else logger.warning("No getActiveAdmins() calls found. No changes applied.")
    }
}
