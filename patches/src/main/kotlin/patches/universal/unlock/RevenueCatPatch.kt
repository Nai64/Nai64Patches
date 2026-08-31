package patches.universal.unlock

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import patches.universal.ads.EntitlementInfoIsActiveFingerprint
import java.util.logging.Logger

@Suppress("unused")
val unlockEntitlementsRevenueCatPatch = bytecodePatch(
    name = "Unlock RevenueCat Entitlements",
    description = "Unlock premium features in apps that use RevenueCat.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        EntitlementInfoIsActiveFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                const/4 v0, 0x1
                return v0
            """.trimIndent())
            logger.info("Patched EntitlementInfo.isActive() to always return true")
            logger.info("Unlock RevenueCat Entitlements patch succeeded")
        } ?: run {
            logger.warning("Could not find RevenueCat EntitlementInfo.isActive() method. Skipping.")
        }
    }
}
