package patches.universal.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x
import java.util.logging.Logger

@Suppress("unused")
val pairipBypassPatch = bytecodePatch(
    name = "Pairip Bypass (Experimental)",
    description = "Bypass app protection so the patched app can start.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        // -- Strategy 1: Local installer check --
        // Report a successful local installer check.
        PerformLocalInstallerCheckFingerprint.methodOrNull?.let {
            it.addInstructions(0, listOf(
                BuilderInstruction11n(Opcode.CONST_4, 0, 1),
                BuilderInstruction11x(Opcode.RETURN, 0),
            ))
            logger.info("Applied Pairip performLocalInstallerCheck spoof")
        }

        // -- Strategy 2: Generic boolean installer check --
        // Integrated from Spoof Play Store Install Source: report Play Store
        // installation. Only the first matching spoof strategy applies.
        var spoofApplied = false
        var spoofName: String? = null
        GenericBooleanInstallerCheckFingerprint.methodOrNull?.let {
            it.addInstructions(0, listOf(
                BuilderInstruction11n(Opcode.CONST_4, 0, 1),
                BuilderInstruction11x(Opcode.RETURN, 0),
            ))
            logger.info("Applied generic boolean Play Store spoof")
            spoofApplied = true
            spoofName = "generic boolean spoof"
        }

        // -- Strategy 3: Generic installer-source string --
        // Return the Play Store package name when PairIP checks the installer source.
        if (!spoofApplied) GenericStringInstallerCheckFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                const-string v0, "com.android.vending"
                return-object v0
            """.trimIndent())
            logger.info("Applied Play Store installer source spoof")
            spoofApplied = true
            spoofName = "installer source"
        }

        // -- Strategy 4: Fallback boolean installer check --
        // Duplicate safety net returning Play Store installation.
        if (!spoofApplied) FallbackBooleanInstallerCheckFingerprint.methodOrNull?.let {
            it.addInstructions(0, listOf(
                BuilderInstruction11n(Opcode.CONST_4, 0, 1),
                BuilderInstruction11x(Opcode.RETURN, 0),
            ))
            logger.info("Applied fallback boolean Play Store spoof")
            spoofApplied = true
            spoofName = "fallback boolean spoof"
        }

        // -- Strategy 5: Fallback installer-source string --
        // Duplicate safety net returning the Play Store package name.
        if (!spoofApplied) FallbackStringInstallerCheckFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                const-string v0, "com.android.vending"
                return-object v0
            """.trimIndent())
            logger.info("Applied fallback String Play Store spoof")
            spoofApplied = true
            spoofName = "fallback installer source"
        }

        // -- Strategy 6: StartupLauncher.launch --
        // Disable the PairIP startup dispatcher.
        PairipStartupLauncherLaunchFingerprint.methodOrNull?.let {
            it.addInstructions(0, "return-void")
            logger.info("Applied PairIP StartupLauncher.launch bypass")
        }

        // -- Strategy 7: StartupLauncher.pairip --
        // Disable the PairIP dispatcher entry point.
        PairipStartupLauncherPairipFingerprint.methodOrNull?.let {
            it.addInstructions(0, "return-void")
            logger.info("Applied PairIP StartupLauncher.pairip bypass")
        }

        val applied = buildList {
            if (PerformLocalInstallerCheckFingerprint.methodOrNull != null) add("performLocalInstallerCheck")
            if (spoofName != null) add(spoofName!!)
            if (PairipStartupLauncherLaunchFingerprint.methodOrNull != null) add("StartupLauncher.launch")
            if (PairipStartupLauncherPairipFingerprint.methodOrNull != null) add("StartupLauncher.pairip")
        }
        if (applied.isEmpty()) {
            logger.warning("No Pairip license methods found. No changes applied.")
        } else {
            logger.info("Pairip Bypass (Experimental) patch succeeded (${applied.size} strategy(s) applied)")
            logger.warning("IF THE APP CRASHES OR STILL BRINGS UP PLAY STORE, DON'T ASK ME TO FIX IT. IT IS NOT POSSIBLE")
            logger.warning("IF THE APP CRASHES OR STILL BRINGS UP PLAY STORE, DON'T ASK ME TO FIX IT. IT IS NOT POSSIBLE")
            logger.warning("IF THE APP CRASHES OR STILL BRINGS UP PLAY STORE, DON'T ASK ME TO FIX IT. IT IS NOT POSSIBLE")
        }
    }
}
