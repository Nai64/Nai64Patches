package patches.universal.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableUpdateCheckPatch = bytecodePatch(
    name = "Disable Google Update Check",
    description = "Disables Play Core in-app update flow. Don't use with Bypass Forced Updates which already includes this.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Updates") } catch (_: NoSuchMethodError) {}
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
