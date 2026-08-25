package patches.universal.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x
import java.util.logging.Logger

@Suppress("unused")
val amazonAppstoreSpoofPatch = bytecodePatch(
    name = "Spoof Amazon Appstore Availability",
    description = "Makes app-specific Amazon Appstore availability checks succeed without changing the system package manager.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0

        fun patchBoolean(fingerprint: app.morphe.patcher.Fingerprint, label: String) {
            val method = fingerprint.methodOrNull ?: return
            if (method.implementation == null || method.implementation!!.registerCount < 1) {
                logger.warning("Skipped Amazon Appstore boolean spoof ($label): no writable register")
                return
            }
            method.addInstructions(0, listOf(
                BuilderInstruction11n(Opcode.CONST_4, 0, 1),
                BuilderInstruction11x(Opcode.RETURN, 0),
            ))
            logger.info("Applied Amazon Appstore boolean availability spoof ($label)")
            patched++
        }

        fun patchString(fingerprint: app.morphe.patcher.Fingerprint, packageName: String) {
            val method = fingerprint.methodOrNull ?: return
            if (method.implementation == null || method.implementation!!.registerCount < 1) {
                logger.warning("Skipped Amazon Appstore string spoof ($packageName): no writable register")
                return
            }
            method.addInstructions(0, """
                const-string v0, "$packageName"
                return-object v0
            """.trimIndent())
            logger.info("Applied Amazon Appstore string availability spoof ($packageName)")
            patched++
        }

        patchBoolean(AmazonVeneziaBooleanAvailabilityFingerprint, "com.amazon.venezia")
        patchString(AmazonVeneziaStringAvailabilityFingerprint, "com.amazon.venezia")
        patchBoolean(AmazonMarketplaceBooleanAvailabilityFingerprint, "com.amazon.device.marketplace")
        patchString(AmazonMarketplaceStringAvailabilityFingerprint, "com.amazon.device.marketplace")

        if (patched == 0) {
            logger.warning("No Amazon Appstore availability check was found. No changes applied.")
        } else {
            logger.info("Amazon Appstore availability spoof succeeded ($patched check(s) patched)")
        }
    }
}
