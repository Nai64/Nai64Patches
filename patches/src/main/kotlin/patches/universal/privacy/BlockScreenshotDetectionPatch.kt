package patches.universal.privacy

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import patches.universal.ads.util.cloneMutable
import patches.universal.ads.util.p0Register
import java.util.logging.Logger

private const val FLAG_SECURE = "0x2000"

@Suppress("unused")
val blockScreenshotDetectionPatch = bytecodePatch(
    name = "Block Screenshot Detection",
    description =
        "Adds FLAG_SECURE to every activity window so the system blocks screenshots, screen " +
            "recording and recents previews of the app",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        var patchedActivities = 0
        classDefForEach { classDef ->
            if (classDef.superclass?.endsWith("Activity;") != true) return@classDefForEach

            val mutableClass = mutableClassDefBy(classDef)
            val onCreate = mutableClass.methods.firstOrNull {
                it.name == "onCreate" && it.returnType == "V" &&
                    it.parameterTypes == listOf("Landroid/os/Bundle;")
            } ?: return@classDefForEach

            val contextReg = onCreate.p0Register
            val b = onCreate.implementation!!.registerCount
            val cloned = onCreate.cloneMutable(additionalRegisters = 3)

            cloned.addInstructions(
                0,
                """
                invoke-virtual {v$contextReg}, Landroid/app/Activity;->getWindow()Landroid/view/Window;
                move-result-object v$b
                const/4 v${b + 1}, $FLAG_SECURE
                invoke-virtual {v$b, v${b + 1}}, Landroid/view/Window;->addFlags(I)V
                """.trimIndent(),
            )

            mutableClass.methods.remove(onCreate)
            mutableClass.methods.add(cloned)
            patchedActivities++
        }

        if (patchedActivities > 0) {
            logger.info("Added FLAG_SECURE to $patchedActivities activit(ies)")
        } else {
            logger.warning("No activity onCreate methods found. No changes applied.")
        }
    }
}
