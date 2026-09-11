package patches.universal.resolution

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.intOption
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import patches.universal.ads.util.findMutableMethodOf
import java.util.logging.Logger

@Suppress("unused")
val tabletModePatch = bytecodePatch(
    name = "Tablet Mode",
    description = "Spoof a tablet smallest width so apps render their tablet UI.",
    default = false,
) {
    category("Resolution")
    val smallestWidth by intOption(
        title = "Smallest width (dp)",
        default = 600,
        key = "tabletSmallestWidthDp",
        description = "Reported smallestScreenWidthDp. 600 or higher unlocks most tablet layouts.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val width = (smallestWidth ?: 600).coerceIn(320, 1200)
        var patched = 0
        classDefForEach { classDef ->
            var hasRef = false
            for (m in classDef.methods) {
                val impl = m.implementation ?: continue
                for (insn in impl.instructions) {
                    if (insn.opcode != Opcode.IGET) continue
                    val ref = (insn as? ReferenceInstruction)?.reference as? FieldReference ?: continue
                    if (ref.definingClass == "Landroid/content/res/Configuration;" &&
                        ref.name == "smallestScreenWidthDp" &&
                        ref.type == "I"
                    ) { hasRef = true; break }
                }
                if (hasRef) break
            }
            if (!hasRef) return@classDefForEach
            val mutableClass by lazy { mutableClassDefBy(classDef) }
            for (method in classDef.methods) {
                val mutableMethod by lazy { mutableClass.findMutableMethodOf(method) }
                val implementation = method.implementation ?: continue
                val instructions: List<Instruction> = implementation.instructions.toList()
                for ((index, instruction) in instructions.withIndex()) {
                    if (instruction.opcode != Opcode.IGET) continue
                    val reference =
                        (instruction as? ReferenceInstruction)?.reference as? FieldReference
                            ?: continue
                    if (reference.definingClass != "Landroid/content/res/Configuration;") continue
                    if (reference.name != "smallestScreenWidthDp") continue
                    if (reference.type != "I") continue

                    val register = (instruction as? OneRegisterInstruction)?.registerA ?: continue
                    mutableMethod.replaceInstruction(index, "const/16 v$register, 0x${width.toString(16)}")
                    patched++
                }
            }
        }
        if (patched > 0) {
            logger.info("Spoofed smallest width to $width dp at $patched call site(s)")
        } else {
            logger.warning("No smallestScreenWidthDp reads found. No changes applied.")
        }
    }
}
