package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import patches.universal.ads.util.findMutableMethodOf
import java.util.logging.Logger

@Suppress("unused")
val spoofWidevineL1Patch = bytecodePatch(
    name = "Spoof Widevine L1",
    description = "Reports a chosen Widevine security level so streaming apps unlock HD playback.",
    default = false,
) {
    category("Spoof")
    val securityLevel by stringOption(
        key = "widevineLevel",
        title = "Security level",
        description = "Reported MediaDrm securityLevel property.",
        default = "L1",
        values = linkedMapOf("L1 (HD)" to "L1", "L3 (SD)" to "L3"),
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val level = securityLevel.orEmpty().trim()
        if (level != "L1" && level != "L3") {
            logger.warning("Invalid security level \"$level\". No changes applied.")
            return@execute
        }

        var patched = 0
        classDefForEach { classDef ->
            val mutableClass by lazy { mutableClassDefBy(classDef) }
            for (method in classDef.methods) {
                val mutableMethod by lazy { mutableClass.findMutableMethodOf(method) }
                val implementation = method.implementation ?: continue
                val instructions = implementation.instructions.toList()
                for ((index, instruction) in instructions.withIndex()) {
                    val reference =
                        (instruction as? ReferenceInstruction)?.reference as? MethodReference
                            ?: continue
                    if (reference.definingClass != "Landroid/media/MediaDrm;") continue
                    if (reference.name != "getPropertyString") continue
                    if (reference.parameterTypes != listOf("Ljava/lang/String;")) continue
                    if (reference.returnType != "Ljava/lang/String;") continue

                    // Property name is the second invoke register (after `this`).
                    val keyRegister = when (instruction) {
                        is BuilderInstruction35c -> when (instruction.registerCount) {
                            1 -> instruction.registerC
                            else -> instruction.registerD
                        }
                        is BuilderInstruction3rc -> instruction.startRegister + 1
                        else -> continue
                    }
                    var keyValue: String? = null
                    for (j in index - 1 downTo 0) {
                        val prev = instructions[j]
                        if (prev.opcode != Opcode.CONST_STRING) continue
                        val reg = (prev as? OneRegisterInstruction)?.registerA ?: continue
                        if (reg != keyRegister) continue
                        keyValue = ((prev as? ReferenceInstruction)?.reference as? StringReference)?.string
                        break
                    }
                    if (keyValue != "securityLevel") continue

                    val next = instructions.getOrNull(index + 1)
                    if (next != null && next.opcode == Opcode.MOVE_RESULT_OBJECT) {
                        val resultRegister = (next as OneRegisterInstruction).registerA
                        mutableMethod.replaceInstruction(
                            index,
                            "const-string v$resultRegister, \"$level\"",
                        )
                        mutableMethod.replaceInstruction(index + 1, "nop")
                        patched++
                    }
                }
            }
        }

        if (patched > 0) logger.info("Spoofed $patched Widevine level check(s) to $level")
        else logger.warning("No Widevine securityLevel checks found. No changes applied.")
    }
}
