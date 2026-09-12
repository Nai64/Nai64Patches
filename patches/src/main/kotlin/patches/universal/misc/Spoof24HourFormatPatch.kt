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
val spoof24HourFormatPatch = bytecodePatch(
    name = "Spoof 24-Hour Format",
    description = "Reports a chosen clock format so apps render times the way you prefer.",
    default = false,
) {
    category("Spoof")
    val timeFormat by stringOption(
        key = "timeFormat",
        title = "Clock format",
        description = "Reported Settings.System time_12_24 value.",
        default = "24",
        values = linkedMapOf("24-hour" to "24", "12-hour" to "12"),
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val format = timeFormat.orEmpty().trim()
        if (format != "12" && format != "24") {
            logger.warning("Invalid clock format \"$format\". No changes applied.")
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
                    if (reference.definingClass != "Landroid/provider/Settings\$System;") continue
                    if (reference.name != "getString") continue
                    if (reference.returnType != "Ljava/lang/String;") continue
                    val params = reference.parameterTypes
                    if (params.size < 2) continue
                    if (params[0] != "Landroid/content/ContentResolver;") continue
                    if (params[1] != "Ljava/lang/String;") continue

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
                    if (keyValue != "time_12_24") continue

                    val next = instructions.getOrNull(index + 1)
                    if (next != null && next.opcode == Opcode.MOVE_RESULT_OBJECT) {
                        val resultRegister = (next as OneRegisterInstruction).registerA
                        mutableMethod.replaceInstruction(
                            index,
                            "const-string v$resultRegister, \"$format\"",
                        )
                        mutableMethod.replaceInstruction(index + 1, "nop")
                        patched++
                    }
                }
            }
        }

        if (patched > 0) logger.info("Spoofed $patched clock-format check(s) to $format")
        else logger.warning("No clock-format checks found. No changes applied.")
    }
}
