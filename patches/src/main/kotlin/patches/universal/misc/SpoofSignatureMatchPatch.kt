package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import patches.universal.ads.util.findMutableMethodOf
import java.util.logging.Logger

@Suppress("unused")
val spoofSignatureMatchPatch = bytecodePatch(
    name = "Spoof Signature Match",
    description =
        "Makes PackageManager.checkSignatures always return SIGNATURE_MATCH so apps that " +
            "verify their own or another package's signature accept any build",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Spoof") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        var patched = 0
        classDefForEach { classDef ->
            val mutableClass by lazy { mutableClassDefBy(classDef) }
            for (method in classDef.methods) {
                val mutableMethod by lazy { mutableClass.findMutableMethodOf(method) }
                val implementation = method.implementation ?: continue
                // Snapshot; one-for-one replacements keep indices valid.
                val instructions = implementation.instructions.toList()
                for ((index, instruction) in instructions.withIndex()) {
                    val reference =
                        (instruction as? ReferenceInstruction)?.reference as? MethodReference
                            ?: continue

                    if (reference.definingClass != "Landroid/content/pm/PackageManager;" ||
                        reference.name != "checkSignatures" ||
                        reference.returnType != "I"
                    ) {
                        continue
                    }

                    // SIGNATURE_MATCH == 0.
                    val next = instructions.getOrNull(index + 1) as? OneRegisterInstruction
                    if (next != null && next.opcode == Opcode.MOVE_RESULT) {
                        mutableMethod.replaceInstruction(index, "const/4 v${next.registerA}, 0x0")
                        mutableMethod.replaceInstruction(index + 1, "nop")
                    } else {
                        mutableMethod.replaceInstruction(index, "nop")
                    }
                    patched++
                }
            }
        }

        if (patched > 0) {
            logger.info("Forced $patched signature check(s) to SIGNATURE_MATCH")
        } else {
            logger.warning("No PackageManager.checkSignatures calls found. No changes applied.")
        }
    }
}
