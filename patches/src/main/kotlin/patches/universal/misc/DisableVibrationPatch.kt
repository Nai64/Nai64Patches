package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger

/** Framework classes whose void `vibrate(...)` methods are neutralised. */
private val vibratorClasses = setOf(
    "Landroid/os/Vibrator;",
    "Landroid/os/SystemVibrator;",
    "Landroid/os/VibratorManager;",
)

@Suppress("unused")
val disableVibrationPatch = bytecodePatch(
    name = "Disable Vibration",
    description =
        "Replaces framework Vibrator.vibrate calls with no-ops so apps cannot make the " +
            "device buzz",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        var patched = 0
        classDefForEach { classDef ->
            val mutableClass = mutableClassDefBy(classDef)
            for (method in mutableClass.methods) {
                // Snapshot of the instruction list; replacing one-for-one keeps indices valid.
                val implementation = method.implementation ?: continue
                val instructions = implementation.instructions.toList()
                for ((index, instruction) in instructions.withIndex()) {
                    val reference =
                        (instruction as? ReferenceInstruction)?.reference as? MethodReference
                            ?: continue

                    // Only void vibrate(...) overloads are replaced; the invoke has no
                    // move-result successor so a plain nop cannot break the caller.
                    if (reference.definingClass in vibratorClasses &&
                        reference.name == "vibrate" &&
                        reference.returnType == "V"
                    ) {
                        method.replaceInstruction(index, "nop")
                        patched++
                    }
                }
            }
        }

        if (patched > 0) {
            logger.info("Disabled $patched vibration call(s)")
        } else {
            logger.warning("No Vibrator.vibrate calls found. No changes applied.")
        }
    }
}
