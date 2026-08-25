package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger

@Suppress("unused")
val disableStrictModePatch = bytecodePatch(
    name = "Disable StrictMode",
    description =
        "No-ops StrictMode policy installs made by the app so debug builds that enable strict " +
            "disk or network checks stop crashing on release-like usage",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val policies = setOf("setThreadPolicy", "setVmPolicy")

        var patched = 0
        classDefForEach { classDef ->
            val mutableClass = mutableClassDefBy(classDef)
            for (method in mutableClass.methods) {
                val implementation = method.implementation ?: continue
                val instructions = implementation.instructions.toList()
                for ((index, instruction) in instructions.withIndex()) {
                    val reference =
                        (instruction as? ReferenceInstruction)?.reference as? MethodReference
                            ?: continue

                    // setThreadPolicy / setVmPolicy are void, so dropping the invoke is safe.
                    if (reference.definingClass != "Landroid/os/StrictMode;" ||
                        reference.name !in policies ||
                        reference.returnType != "V"
                    ) {
                        continue
                    }

                    method.replaceInstruction(index, "nop")
                    patched++
                }
            }
        }

        if (patched > 0) {
            logger.info("Disabled $patched StrictMode policy install(s)")
        } else {
            logger.warning("No StrictMode installs found. No changes applied.")
        }
    }
}
