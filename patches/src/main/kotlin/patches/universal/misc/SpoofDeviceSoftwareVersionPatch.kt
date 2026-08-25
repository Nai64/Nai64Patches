package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofDeviceSoftwareVersionPatch = bytecodePatch(
    name = "Spoof Device Software Version",
    description = "Reports a constant value from TelephonyManager.getDeviceSoftwareVersion() so apps cannot read the real radio/software version.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldStringGetterConst(
            "Landroid/telephony/TelephonyManager;",
            setOf("getDeviceSoftwareVersion"),
            "0",
        )
        if (patched > 0) logger.info("Spoofed device software version at $patched call site(s)")
        else logger.warning("No getDeviceSoftwareVersion call sites found. No changes applied.")
    }
}
