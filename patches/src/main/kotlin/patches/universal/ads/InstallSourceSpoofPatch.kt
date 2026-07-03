package patches.universal.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x
import java.util.logging.Logger

@Suppress("unused")
val installSourceSpoofPatch = bytecodePatch(
    name = "Spoof Play Store Install Source",
    description = "Makes the app think it was installed from Google Play Store. " +
            "Supports Pairip and generic installer check methods. " +
            "Recommended if the app has a Play Store license check.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        // Strategy 1: Pairip performLocalInstallerCheck
        val pairipMethod = PerformLocalInstallerCheckFingerprint.methodOrNull
        if (pairipMethod != null) {
            pairipMethod.addInstructions(0, listOf(
                BuilderInstruction11n(Opcode.CONST_4, 0, 1),
                BuilderInstruction11x(Opcode.RETURN, 0),
            ))
            logger.info("Applied Pairip Play Store spoof")
            return@execute
        }

        // Strategy 2: Pairip VMRunner.invoke() — native VM entry point.
        // Skips the native Pairip VM that performs license/installer checks.
        // Checked early (before generic string matches) to avoid false positives
        // from billing methods that also reference "com.android.vending".
        val vmRunner = PairipVMRunnerInvokeFingerprint.methodOrNull
        if (vmRunner != null) {
            vmRunner.addInstructions(0, """
                const/4 v0, 0x0
                return-object v0
            """.trimIndent())
            logger.info("Applied Pairip VM skip spoof")
            return@execute
        }

        // ── Generic string-based strategies ──
        // These search for methods containing "com.android.vending" by return type.
        // Lower priority: can false-match billing/purchase methods.

        // Strategy 3: Private boolean method referencing "com.android.vending"
        val boolCheck = GenericBooleanInstallerCheckFingerprint.methodOrNull
        if (boolCheck != null) {
            boolCheck.addInstructions(0, listOf(
                BuilderInstruction11n(Opcode.CONST_4, 0, 1),
                BuilderInstruction11x(Opcode.RETURN, 0),
            ))
            logger.info("Applied generic boolean Play Store spoof")
            return@execute
        }

        // Strategy 4: Private String method referencing "com.android.vending"
        val strCheck = GenericStringInstallerCheckFingerprint.methodOrNull
        if (strCheck != null) {
            strCheck.addInstructions(0, """
                const-string v0, "com.android.vending"
                return-object v0
            """.trimIndent())
            logger.info("Applied generic String Play Store spoof")
            return@execute
        }

        // Strategy 5: Any boolean method (any access) with "com.android.vending"
        val fallbackBool = FallbackBooleanInstallerCheckFingerprint.methodOrNull
        if (fallbackBool != null) {
            fallbackBool.addInstructions(0, listOf(
                BuilderInstruction11n(Opcode.CONST_4, 0, 1),
                BuilderInstruction11x(Opcode.RETURN, 0),
            ))
            logger.info("Applied fallback boolean Play Store spoof")
            return@execute
        }

        // Strategy 6: Any String method (any access) with "com.android.vending"
        val fallbackStr = FallbackStringInstallerCheckFingerprint.methodOrNull
        if (fallbackStr != null) {
            fallbackStr.addInstructions(0, """
                const-string v0, "com.android.vending"
                return-object v0
            """.trimIndent())
            logger.info("Applied fallback String Play Store spoof")
            return@execute
        }

        logger.warning("Could not find any install source check method. No changes applied.")
    }
}
