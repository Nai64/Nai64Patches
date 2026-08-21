package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val ignoreScreenLockPatch = bytecodePatch(
    name = "Ignore Screen Lock",
    description =
        "Makes KeyguardManager lock-state checks report an unlocked device so apps that gate " +
            "features behind a screen lock behave as if none is set",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val patched = foldBooleanReturns(
            mapOf(
                "Landroid/app/KeyguardManager;" to mapOf(
                    "isKeyguardLocked" to "0x0",
                    "isKeyguardRestrictedInputMode" to "0x0",
                    "isDeviceLocked" to "0x0",
                    "isDeviceSecure" to "0x0",
                ),
            ),
        )

        if (patched > 0) logger.info("Forced $patched screen-lock check(s) to unlocked")
        else logger.warning("No screen-lock checks found. No changes applied.")
    }
}
