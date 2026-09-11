package patches.universal.misc

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val fakeUsageAccessPatch = bytecodePatch(
    name = "Fake Usage Access",
    description = "Report usage-stats access as granted.",
    default = false,
) {
    category("Fake")
    val granted by booleanOption(
        title = "Grant access",
        default = true,
        key = "usageStatsAccess",
        description = "Report usage-stats access as granted (true) or denied (false).",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        // MODE_ALLOWED = 0, MODE_IGNORED = 1: fold to 0 when granting.
        val patched = if (granted == true) {
            foldIntGetterConst(
                "Landroid/app/AppOpsManager;",
                setOf("checkOpNoThrow", "unsafeCheckOpNoThrow"),
                0,
            )
        } else {
            foldIntGetterConst(
                "Landroid/app/AppOpsManager;",
                setOf("checkOpNoThrow", "unsafeCheckOpNoThrow"),
                1,
            )
        }
        if (patched > 0) {
            logger.info("Faked usage-stats access at $patched call site(s)")
        } else {
            logger.warning("No AppOpsManager usage-stats call sites found. No changes applied.")
        }
    }
}
