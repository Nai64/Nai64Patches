package patches.universal.splash

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger

internal object SplashOnCreateFingerprint : Fingerprint(
    name = "Skip Splash Screen (Experimental)",
    returnType = "V",
    custom = { method, classDef ->
        classDef.type.contains("splash", ignoreCase = true) &&
            method.parameterTypes.size == 1 &&
            method.parameterTypes[0] == "Landroid/os/Bundle;"
    },
)

private fun postDelayedDelayRegister(invoke: com.android.tools.smali.dexlib2.iface.instruction.Instruction): Int? {
    val reference = (invoke as? ReferenceInstruction)?.reference as? MethodReference ?: return null
    if (reference.definingClass != "Landroid/os/Handler;") return null
    if (reference.name != "postDelayed") return null

    return when (invoke) {
        is RegisterRangeInstruction -> {
            if (invoke.registerCount == 4) invoke.startRegister + 2 else null
        }

        is FiveRegisterInstruction -> {
            // invoke-virtual {handler, runnable, wideLow, wideHigh}
            if (invoke.registerCount == 4) invoke.registerE else null
        }

        else -> null
    }
}

@Suppress("unused")
val skipSplashScreenPatch = bytecodePatch(
    name = "Skip Splash Screen (Experimental)",
    description = "Skip or shorten splash screen delays",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val method = SplashOnCreateFingerprint.methodOrNull
        if (method == null || method.implementation == null) {
            logger.warning("No splash activity found. No changes applied.")
            return@execute
        }

        val instructions = method.implementation!!.instructions.toList()
        var patched = 0
        for ((index, insn) in instructions.withIndex()) {
            val delayRegister = postDelayedDelayRegister(insn) ?: continue
            // Insert a zero delay right before the postDelayed call so the
            // splash handler fires immediately.
            method.addInstructions(index, "const-wide/16 v$delayRegister, 0x0")
            patched++
        }

        if (patched == 0) {
            logger.warning("Splash activity found, but no delayed transitions to skip.")
        } else {
            logger.info("Skipped splash delays in ${method.definingClass} ($patched call(s))")
        }
    }
}
