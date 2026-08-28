package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import java.util.logging.Logger

@Suppress("unused")
val stripBuildConfigDebugPatch = bytecodePatch(
    name = "Strip BuildConfig DEBUG",
    description = "Forces BuildConfig.DEBUG to false so apps hide debug banners and verbose checks.",
    default = false,
) {
    val forceDebug by booleanOption(
        title = "Force debug",
        default = false,
        key = "forceBuildConfigDebug",
        description = "If true, forces DEBUG to true instead of false.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val target = if (forceDebug == true) "0x1" else "0x0"
        var patched = 0
        classDefForEach { classDef ->
            val mutableClass = mutableClassDefBy(classDef)
            for (method in mutableClass.methods) {
                val impl = method.implementation ?: continue
                val instructions = impl.instructions.toList()
                for ((index, insn) in instructions.withIndex()) {
                    if (insn.opcode != Opcode.SGET && insn.opcode != Opcode.SGET_BOOLEAN && insn.opcode != Opcode.SGET_OBJECT) continue
                    val ref = (insn as? ReferenceInstruction)?.reference as? FieldReference ?: continue
                    if (ref.name != "DEBUG") continue
                    if (ref.type != "Z") continue
                    if (!ref.definingClass.contains("BuildConfig")) continue
                    val reg = (insn as? OneRegisterInstruction)?.registerA ?: continue
                    method.replaceInstruction(index, "const/4 v$reg, $target")
                    patched++
                }
            }
        }
        // Also handle sget-boolean variant where opcode is SGET_BOOLEAN (DEX uses SGET for all)
        // Fallback: scan for any field named DEBUG regardless of opcode already handled above covers it.
        if (patched > 0) logger.info("Stripped BuildConfig.DEBUG at $patched site(s) -> $target")
        else logger.warning("No BuildConfig.DEBUG reads found. No changes applied.")
    }
}
