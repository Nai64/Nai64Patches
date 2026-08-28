package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger

@Suppress("unused")
val spoofDeviceSoftwareVersionPatch = bytecodePatch(
    name = "Spoof Device Software Version",
    description = "Reports a chosen value from TelephonyManager.getDeviceSoftwareVersion() so apps cannot read the real radio/software version.",
    default = false,
) {
    val version by stringOption(
        title = "Software version",
        default = "0",
        key = "deviceSoftwareVersion",
        description = "Version to report.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldStringGetterConst(
            "Landroid/telephony/TelephonyManager;",
            setOf("getDeviceSoftwareVersion"),
            version ?: "0",
        )
        if (patched > 0) logger.info("Spoofed device software version at $patched call site(s)")
        else logger.warning("No getDeviceSoftwareVersion call sites found. No changes applied.")
    }
}
