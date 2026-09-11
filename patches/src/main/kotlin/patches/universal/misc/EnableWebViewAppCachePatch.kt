package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import patches.universal.ads.util.findMutableMethodOf
import patches.universal.ads.util.registersUsed
import java.util.logging.Logger

private fun BytecodePatchContext.forceBooleanSetter(
    targetClass: String,
    setters: Set<String>,
    value: Boolean,
): Int {
    val literal = if (value) "0x1" else "0x0"
    var patched = 0
    classDefForEach { classDef ->
        val mutableClass by lazy { mutableClassDefBy(classDef) }
        for (method in classDef.methods) {
            val mutableMethod by lazy { mutableClass.findMutableMethodOf(method) }
            val impl = method.implementation ?: continue
            val instructions = impl.instructions.toList()
            for ((index, insn) in instructions.withIndex()) {
                if (insn !is ReferenceInstruction) continue
                val ref = insn.reference as? MethodReference ?: continue
                if (ref.definingClass != targetClass) continue
                if (ref.name !in setters) continue
                if (ref.returnType != "V") continue
                if (ref.parameterTypes != listOf("Z")) continue

                val reg = insn.registersUsed[1]
                for (j in index - 1 downTo 0) {
                    val prev = instructions[j]
                    if (prev.opcode == Opcode.NOP) continue
                    if (prev is NarrowLiteralInstruction &&
                        prev is OneRegisterInstruction &&
                        prev.registerA == reg
                    ) {
                        mutableMethod.replaceInstruction(j, "const/4 v$reg, $literal")
                        patched++
                        break
                    }
                    break
                }
            }
        }
    }
    return patched
}

@Suppress("unused")
val enableWebViewAppCachePatch = bytecodePatch(
    name = "Enable WebView App Cache",
    description = "Enables the WebView HTML5 application cache so offline-capable web apps work.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Enable") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = forceBooleanSetter(
            "Landroid/webkit/WebSettings;",
            setOf("setAppCacheEnabled"),
            true,
        )
        if (patched > 0) {
            logger.info("Forced $patched setAppCacheEnabled() call(s) to true")
        } else {
            logger.warning("No setAppCacheEnabled() calls found. No changes applied.")
        }
    }
}
