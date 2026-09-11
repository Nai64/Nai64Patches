package patches.universal.misc

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val fakeDeviceAdminPatch = bytecodePatch(
    name = "Fake Device Admin",
    description = "Report device admin as active.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Fake") } catch (_: NoSuchMethodError) {}
    val active by booleanOption(
        title = "Report active",
        default = true,
        key = "deviceAdminActive",
        description = "Report device admin as active (true) or inactive (false).",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanGetterConst(
            "Landroid/app/admin/DevicePolicyManager;",
            setOf("isAdminActive"),
            active == true,
        )
        if (patched > 0) {
            logger.info("Faked device admin state at $patched call site(s)")
        } else {
            logger.warning("No DevicePolicyManager.isAdminActive call sites found. No changes applied.")
        }
    }
}
