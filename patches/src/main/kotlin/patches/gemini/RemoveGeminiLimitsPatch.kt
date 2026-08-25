package patches.gemini

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger

private val LIMIT_NAMES = setOf(
    "isLimitReached", "isQuotaExceeded", "hasQuota", "isRateLimited",
    "isCharacterLimitExceeded", "isFileSizeLimited", "isInputTooLong",
    "isOverQuota", "hasReachedLimit", "isThrottled",
)

private fun BytecodePatchContext.foldLimitChecks(): Int {
    var patched = 0
    classDefForEach { classDef ->
        val mutableClass = mutableClassDefBy(classDef)
        for (method in mutableClass.methods) {
            val impl = method.implementation ?: continue
            val instructions = impl.instructions.toList()
            for ((index, insn) in instructions.withIndex()) {
                val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                if (ref.name !in LIMIT_NAMES) continue
                if (ref.returnType != "Z") continue
                val next = instructions.getOrNull(index + 1) as? OneRegisterInstruction
                if (next != null && next.opcode == Opcode.MOVE_RESULT) {
                    method.replaceInstruction(index, "const/4 v${next.registerA}, 0x0")
                    method.replaceInstruction(index + 1, "nop")
                    patched++
                } else if (next == null || next.opcode != Opcode.MOVE_RESULT) {
                    method.replaceInstruction(index, "nop")
                    patched++
                }
            }
        }
    }
    return patched
}

@Suppress("unused")
val removeGeminiLimitsPatch = bytecodePatch(
    name = "Remove Gemini Limits",
    description = "Gemini: forces quota/rate-limit checks to false to hide input/file-size and rate-limit dialogs.",
    default = false,
) {
    compatibleWith("com.google.android.apps.bard")

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldLimitChecks()
        if (patched > 0) logger.info("Disabled $patched limit check(s)")
        else logger.warning("No limit checks found. No changes applied.")
    }
}
