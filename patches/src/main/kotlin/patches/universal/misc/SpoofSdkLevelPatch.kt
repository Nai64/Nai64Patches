package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.intOption
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import java.util.logging.Logger

private const val VERSION_CLASS = "Landroid/os/Build\$VERSION;"

@Suppress("unused")
val spoofSdkLevelPatch = bytecodePatch(
    name = "Spoof SDK Level",
    description =
        "Replaces every read of Build.VERSION.SDK_INT with a custom value. WARNING: setting " +
            "a level lower than the device's real API level makes apps call missing APIs and " +
            "crash; only use when you know the app checks it",
    default = false,
) {
    val sdkInt by intOption(
        title = "SDK level",
        default = -1,
        key = "spoofSdkLevel",
        description = "Reported API level (e.g. 29). Use -1 to keep the original.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val value = sdkInt ?: -1
        if (value <= 0) {
            logger.warning("No valid SDK level configured. No changes applied.")
            return@execute
        }
        if (value > 127) {
            logger.warning("SDK levels above 127 cannot be encoded as const/4. No changes applied.")
            return@execute
        }

        var patched = 0
        classDefForEach { classDef ->
            val mutableClass = mutableClassDefBy(classDef)
            for (method in mutableClass.methods) {
                val implementation = method.implementation ?: continue
                val instructions = implementation.instructions.toList()
                for ((index, instruction) in instructions.withIndex()) {
                    val field =
                        (instruction as? ReferenceInstruction)?.reference as? FieldReference
                            ?: continue
                    if (field.definingClass != VERSION_CLASS || field.name != "SDK_INT") continue

                    val next = instructions.getOrNull(index + 1) as? OneRegisterInstruction
                    if (next != null && next.opcode == Opcode.MOVE_RESULT) {
                        method.replaceInstruction(
                            index,
                            "const/4 v${next.registerA}, 0x${value.toString(16)}",
                        )
                        method.replaceInstruction(index + 1, "nop")
                        patched++
                    }
                }
            }
        }

        if (patched > 0) {
            logger.info("Spoofed $patched SDK_INT read(s) to $value")
        } else {
            logger.warning("No Build.VERSION.SDK_INT reads found. No changes applied.")
        }
    }
}
