package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofGroupIdLevel1Patch = bytecodePatch(
    name = "Spoof Group ID Level 1",
    description = "Reports a constant value from TelephonyManager.getGroupIdLevel1() so apps cannot read the real SIM GID1.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldStringGetterConst(
            "Landroid/telephony/TelephonyManager;",
            setOf("getGroupIdLevel1"),
            "",
        )
        if (patched > 0) logger.info("Spoofed GID1 at $patched call site(s)")
        else logger.warning("No getGroupIdLevel1 call sites found. No changes applied.")
    }
}
