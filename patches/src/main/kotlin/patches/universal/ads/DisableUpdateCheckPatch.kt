package patches.universal.ads

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableUpdateCheckPatch = bytecodePatch(
    name = "Disable Update Checks",
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
        }
    }
}
