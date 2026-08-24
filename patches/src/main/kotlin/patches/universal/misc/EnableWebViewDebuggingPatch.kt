package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import patches.universal.ads.util.cloneMutable
import patches.universal.ui.findApplicationOnCreate
import java.util.logging.Logger

private const val SET_DEBUGGING = "setWebContentsDebuggingEnabled"

@Suppress("unused")
val enableWebViewDebuggingPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description =
        "Turns on WebView remote debugging: existing setWebContentsDebuggingEnabled calls are " +
            "forced to true and it is actively enabled at startup, so WebViews can be inspected " +
            "via chrome://inspect",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        // 1) Flip every explicit setWebContentsDebuggingEnabled(false) to true by
        //    overwriting the boolean argument register before the invoke.
        var forced = 0
        classDefForEach { classDef ->
            val mutableClass = mutableClassDefBy(classDef)
            for (method in mutableClass.methods) {
                val implementation = method.implementation ?: continue
                val instructions = implementation.instructions.toList()

                // Collect first, apply in descending order so insertions never
                // invalidate the indices of pending edits.
                val insertions = mutableListOf<Pair<Int, String>>()
                for ((index, instruction) in instructions.withIndex()) {
                    val reference =
                        (instruction as? ReferenceInstruction)?.reference as? MethodReference
                            ?: continue
                    if (reference.definingClass != "Landroid/webkit/WebView;" ||
                        reference.name != SET_DEBUGGING ||
                        reference.returnType != "V"
                    ) {
                        continue
                    }

                    val argRegister = when (instruction) {
                        is FiveRegisterInstruction -> instruction.registerC
                        is RegisterRangeInstruction -> instruction.startRegister
                        else -> null
                    } ?: continue

                    insertions += index to "const/4 v$argRegister, 0x1"
                }

                if (insertions.isNotEmpty()) {
                    insertions.sortedByDescending { it.first }.forEach { (at, smali) ->
                        method.addInstructions(at, smali)
                    }
                    forced += insertions.size
                }
            }
        }

        // 2) Actively enable debugging once at startup, even when the app never
        //    calls the setter itself (the release default is false).
        var injected = false
        findApplicationOnCreate()?.let { (mutableClass, onCreate) ->
            val tempBase = onCreate.implementation!!.registerCount
            val cloned = onCreate.cloneMutable(additionalRegisters = 2)
            cloned.addInstructions(
                0,
                """
                const/4 v${tempBase + 1}, 0x1
                invoke-static {v${tempBase + 1}}, Landroid/webkit/WebView;->setWebContentsDebuggingEnabled(Z)V
                """.trimIndent(),
            )
            mutableClass.methods.remove(onCreate)
            mutableClass.methods.add(cloned)
            injected = true
        }

        when {
            forced > 0 && injected ->
                logger.info("Forced $forced debug call(s) true and enabled WebView debugging at startup")

            injected -> logger.info("Enabled WebView debugging at startup")
            forced > 0 -> logger.info("Forced $forced debug call(s) to true")
            else -> logger.warning("No Application.onCreate or WebView debug calls found. No changes applied.")
        }
    }
}
