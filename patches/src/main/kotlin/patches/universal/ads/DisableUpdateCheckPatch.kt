package patches.universal.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableUpdateCheckPatch = bytecodePatch(
    name = "Disable Google Update Check",
    description =
        "Disables the Google Play in-app update flow used by newer apps through the Play Core " +
            "update API. This does not remove the app from Google Play's update list or disable " +
            "custom update checks implemented by the app itself. Do not select this patch when " +
            "using Bypass Forced Updates, because that patch already includes this Play Core " +
            "functionality.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val impl = AppUpdateManagerImplStartUpdateFlowFingerprint.methodOrNull
        if (impl != null) {
            impl.addInstructions(0, """
                const/4 v0, 0x0
                return-object v0
            """.trimIndent())
            logger.info("Disabled Play In-app Updates")
            logger.info("Disable Update Checks patch succeeded")
        }
    }
}
