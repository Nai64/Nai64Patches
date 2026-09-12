package patches.universal.graphics

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import patches.universal.ads.util.findMutableMethodOf
import java.util.logging.Logger

private val glesClasses = setOf(
    "Landroid/opengl/GLES10;",
    "Landroid/opengl/GLES20;",
    "Landroid/opengl/GLES30;",
    "Landroid/opengl/GLES31;",
    "Landroid/opengl/GLES32;",
)

private const val GL_VENDOR = 0x1F00
private const val GL_RENDERER = 0x1F01

private fun escapeSmali(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")

@Suppress("unused")
val spoofGpuInfoPatch = bytecodePatch(
    name = "Spoof GPU Info",
    description = "Replaces OpenGL renderer and vendor strings so apps see a chosen GPU.",
    default = false,
) {
    category("Graphics")
    val gpuRenderer by stringOption(
        key = "gpuRenderer",
        title = "GPU renderer",
        description = "Reported GL_RENDERER, e.g. Adreno (TM) 750. Empty keeps the original.",
        default = "Adreno (TM) 750",
        values = linkedMapOf(
            "Adreno 750" to "Adreno (TM) 750",
            "Mali-G720" to "Mali-G720",
            "Apple M2 (compat)" to "Apple M2",
            "Keep original" to "",
        ),
    )
    val gpuVendor by stringOption(
        key = "gpuVendor",
        title = "GPU vendor",
        description = "Reported GL_VENDOR, e.g. Qualcomm. Empty keeps the original.",
        default = "Qualcomm",
        values = linkedMapOf(
            "Qualcomm" to "Qualcomm",
            "ARM" to "ARM",
            "Keep original" to "",
        ),
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val renderer = gpuRenderer.orEmpty().trim()
        val vendor = gpuVendor.orEmpty().trim()
        if (renderer.isEmpty() && vendor.isEmpty()) {
            logger.warning("No GPU strings configured. No changes applied.")
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
                    if (reference.definingClass !in glesClasses) continue
                    if (reference.name != "glGetString") continue
                    if (reference.parameterTypes != listOf("I")) continue
                    if (reference.returnType != "Ljava/lang/String;") continue

                    val paramRegister = when (instruction) {
                        is BuilderInstruction35c -> instruction.registerC
                        is BuilderInstruction3rc -> instruction.startRegister
                        else -> continue
                    }
                    var literal: Int? = null
                    for (j in index - 1 downTo 0) {
                        val prev = instructions[j]
                        if (prev !is OneRegisterInstruction || prev.registerA != paramRegister) continue
                        if (prev is NarrowLiteralInstruction) {
                            literal = prev.narrowLiteral
                            break
                        }
                    }
                    val replacement = when (literal) {
                        GL_RENDERER -> renderer.ifEmpty { continue }
                        GL_VENDOR -> vendor.ifEmpty { continue }
                        else -> continue
                    }

                    val next = instructions.getOrNull(index + 1)
                    if (next != null && next.opcode == Opcode.MOVE_RESULT_OBJECT) {
                        val resultRegister = (next as OneRegisterInstruction).registerA
                        val constInsn = if (resultRegister <= 0xff) {
                            "const-string v$resultRegister, \"${escapeSmali(replacement)}\""
                        } else {
                            "const-string/jumbo v$resultRegister, \"${escapeSmali(replacement)}\""
                        }
                        mutableMethod.replaceInstruction(index, constInsn)
                        mutableMethod.replaceInstruction(index + 1, "nop")
                        patched++
                    }
                }
            }
        }

        if (patched > 0) logger.info("Spoofed $patched GPU string(s)")
        else logger.warning("No GLES glGetString(RENDERER/VENDOR) calls found. No changes applied.")
    }
}
