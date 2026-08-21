package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import java.util.logging.Logger

private const val BUILD_CLASS = "Landroid/os/Build;"

/** Escapes a value for use inside a smali const-string literal. */
private fun escapeSmali(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")

@Suppress("unused")
val spoofDeviceModelPatch = bytecodePatch(
    name = "Spoof Device Model",
    description =
        "Replaces common android.os.Build fields (model, manufacturer, brand) with custom " +
            "values. Leave an option empty to keep the original value",
    default = false,
) {
    val model by stringOption(
        title = "Model",
        default = "",
        key = "spoofModel",
        description = "Build.MODEL, e.g. Pixel 10 Pro XL. Empty keeps the original.",
    )
    val manufacturer by stringOption(
        title = "Manufacturer",
        default = "",
        key = "spoofManufacturer",
        description = "Build.MANUFACTURER, e.g. Google. Empty keeps the original.",
    )
    val brand by stringOption(
        title = "Brand",
        default = "",
        key = "spoofBrand",
        description = "Build.BRAND, e.g. google. Empty keeps the original.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val replacements = mapOf(
            "MODEL" to model.orEmpty(),
            "MANUFACTURER" to manufacturer.orEmpty(),
            "BRAND" to brand.orEmpty(),
        ).filterValues { it.isNotEmpty() }

        if (replacements.isEmpty()) {
            logger.warning("No values configured. No changes applied.")
            return@execute
        }

        var patched = 0
        classDefForEach { classDef ->
            val mutableClass = mutableClassDefBy(classDef)
            for (method in mutableClass.methods) {
                val implementation = method.implementation ?: continue
                // Snapshot; one-for-one replacements keep indices valid.
                val instructions = implementation.instructions.toList()
                for ((index, instruction) in instructions.withIndex()) {
                    val field =
                        (instruction as? ReferenceInstruction)?.reference as? FieldReference
                            ?: continue
                    if (field.definingClass != BUILD_CLASS || field.name !in replacements) continue

                    val next = instructions.getOrNull(index + 1) as? OneRegisterInstruction
                    if (next != null && next.opcode == Opcode.MOVE_RESULT_OBJECT) {
                        method.replaceInstruction(
                            index,
                            "const-string v${next.registerA}, \"${escapeSmali(replacements[field.name]!!)}\"",
                        )
                        method.replaceInstruction(index + 1, "nop")
                        patched++
                    }
                }
            }
        }

        if (patched > 0) {
            logger.info("Spoofed $patched Build field read(s)")
        } else {
            logger.warning("No matching Build field reads found. No changes applied.")
        }
    }
}
