package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger

private fun BytecodePatchContext.replaceVoidCallsWithProceed(
    targetClass: String,
    targetMethod: String,
    targetParams: List<String>,
): Int {
    var patched = 0
    classDefForEach { classDef ->
        val mutableClass = mutableClassDefBy(classDef)
        for (method in mutableClass.methods) {
            val impl = method.implementation ?: continue
            val instructions = impl.instructions.toList()
            for ((index, insn) in instructions.withIndex()) {
                if (insn !is ReferenceInstruction) continue
                val ref = insn.reference as? MethodReference ?: continue
                if (ref.definingClass != targetClass) continue
                if (ref.name != targetMethod) continue
                if (ref.returnType != "V") continue
                if (ref.parameterTypes != targetParams) continue
                val objReg = when (insn) {
                    is FiveRegisterInstruction -> if (insn.registerCount == 1) insn.registerC else insn.registerD
                    is RegisterRangeInstruction -> insn.startRegister
                    else -> continue
                }
                method.replaceInstruction(index, "invoke-virtual {v$objReg}, $targetClass->proceed()V")
                patched++
            }
        }
    }
    return patched
}

@Suppress("unused")
val bypassWebViewSslErrorsPatch = bytecodePatch(
    name = "Bypass WebView SSL Errors",
    description = "Makes WebViewClient.onReceivedSslError call proceed() instead of cancel() so WebView cert errors no longer block the page.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = replaceVoidCallsWithProceed(
            "Landroid/webkit/SslErrorHandler;",
            "cancel",
            emptyList(),
        )
        if (patched > 0) {
            logger.info("Redirected $patched SslErrorHandler.cancel() call(s) to proceed()")
        } else {
            logger.warning("No SslErrorHandler.cancel() calls found. No changes applied.")
        }
    }
}
