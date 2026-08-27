package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val unlockNotificationChannelsPatch = bytecodePatch(
    name = "Unlock Notification Channels",
    description = "Makes all notification channels blockable and their importance unlocked so greyed-out toggles become interactable.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        // Channel is blockable -> toggle not greyed
        var patched = foldBooleanGetterConst(
            "Landroid/app/NotificationChannel;",
            setOf("isBlockable"),
            true,
        )
        // Also handle vendor/system lock flags that grey out importance
        patched += foldBooleanGetterConst(
            "Landroid/app/NotificationChannel;",
            setOf(
                "isImportanceLockedByOEM",
                "isImportanceLockedByCriticalDeviceFunction",
            ),
            false,
        )
        // Some OEMs use isBlockableSystem on Android 14+
        patched += foldBooleanReturns(
            mapOf(
                "Landroid/app/NotificationChannel;" to mapOf(
                    "isBlockableSystem" to "0x0",
                    "isImportanceLockedByOEM" to "0x0",
                    "isImportanceLockedByCriticalDeviceFunction" to "0x0",
                ),
            ),
            returnType = "Z",
        )

        if (patched > 0) {
            logger.info("Unlocked $patched notification channel check(s)")
        } else {
            logger.warning("No NotificationChannel blockable/importance checks found. No changes applied.")
        }
    }
}
