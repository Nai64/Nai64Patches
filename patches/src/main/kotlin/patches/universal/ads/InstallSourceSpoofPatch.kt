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

        var pairipApplied = false

        // Strategy 1: Pairip performLocalInstallerCheck
        PerformLocalInstallerCheckFingerprint.methodOrNull?.let {
            it.addInstructions(0, listOf(
                BuilderInstruction11n(Opcode.CONST_4, 0, 1),
                BuilderInstruction11x(Opcode.RETURN, 0),
            ))
            logger.info("Applied Pairip performLocalInstallerCheck spoof")
            pairipApplied = true
        }

        // Strategy 2: Pairip SignatureCheck.verifyIntegrity() — runs in Application.attachBaseContext
        // before any app code, checks APK signature. Must be patched first to avoid crash.
        PairipSignatureCheckVerifyIntegrityFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                return-void
            """.trimIndent())
            logger.info("Applied Pairip SignatureCheck.verifyIntegrity bypass")
            pairipApplied = true
        }

        // Strategy 3: Pairip SignatureCheck.verifySignatureMatches() — belt-and-suspenders
        PairipSignatureCheckVerifySignatureMatchesFingerprint.methodOrNull?.let {
            it.addInstructions(0, listOf(
                BuilderInstruction11n(Opcode.CONST_4, 0, 1),
                BuilderInstruction11x(Opcode.RETURN, 0),
            ))
            logger.info("Applied Pairip SignatureCheck.verifySignatureMatches bypass")
            pairipApplied = true
        }

        // Strategy 4: Pairip StartupLauncher.launch() — early native VM entry point.
        // Skip only the startup VM program; VMRunner.invoke() is a shared dispatcher
        // used by protected app/library code and must keep returning real values.
        PairipStartupLauncherLaunchFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                return-void
            """.trimIndent())
            logger.info("Applied Pairip StartupLauncher.launch bypass")
            pairipApplied = true
        }

        // If any Pairip strategy was applied, generic fallbacks are unnecessary
        if (pairipApplied) return@execute

        // ── Generic string-based strategies ──
        // These search for methods containing "com.android.vending" by return type.
        // Only reached if no Pairip-specific methods were found.

        // Strategy 5: Private boolean method referencing "com.android.vending"
        val boolCheck = GenericBooleanInstallerCheckFingerprint.methodOrNull
        if (boolCheck != null) {
            boolCheck.addInstructions(0, listOf(
                BuilderInstruction11n(Opcode.CONST_4, 0, 1),
                BuilderInstruction11x(Opcode.RETURN, 0),
            ))
            logger.info("Applied generic boolean Play Store spoof")
            return@execute
        }

        // Strategy 6: Private String method referencing "com.android.vending"
        val strCheck = GenericStringInstallerCheckFingerprint.methodOrNull
        if (strCheck != null) {
            strCheck.addInstructions(0, """
                const-string v0, "com.android.vending"
                return-object v0
            """.trimIndent())
            logger.info("Applied generic String Play Store spoof")
            return@execute
        }

        // Strategy 7: Any boolean method (any access) with "com.android.vending"
        val fallbackBool = FallbackBooleanInstallerCheckFingerprint.methodOrNull
        if (fallbackBool != null) {
            fallbackBool.addInstructions(0, listOf(
                BuilderInstruction11n(Opcode.CONST_4, 0, 1),
                BuilderInstruction11x(Opcode.RETURN, 0),
            ))
            logger.info("Applied fallback boolean Play Store spoof")
            return@execute
        }

        // Strategy 8: Any String method (any access) with "com.android.vending"
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
