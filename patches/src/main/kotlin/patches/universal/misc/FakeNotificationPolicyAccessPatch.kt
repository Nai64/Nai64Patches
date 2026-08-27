package patches.universal.misc

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val fakeNotificationPolicyAccessPatch = bytecodePatch(
    name = "Fake Notification Policy Access",
    description = "Makes NotificationManager.isNotificationPolicyAccessGranted() report a chosen state so apps that gate features behind DND policy access stop doing so.",
    default = false,
) {
    val granted by booleanOption(
        title = "Grant access",
        default = true,
        key = "notificationPolicyAccess",
        description = "Report DND policy access as granted (true) or denied (false).",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanGetterConst(
            "Landroid/app/NotificationManager;",
            setOf("isNotificationPolicyAccessGranted"),
            granted == true,
        )
        if (patched > 0) {
            logger.info("Faked notification policy access at $patched call site(s)")
        } else {
            logger.warning("No isNotificationPolicyAccessGranted call sites found. No changes applied.")
        }
    }
}
