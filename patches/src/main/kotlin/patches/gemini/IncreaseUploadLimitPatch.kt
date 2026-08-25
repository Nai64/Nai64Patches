package patches.gemini

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger

private val UPLOAD_NAMES = setOf(
    "getMaxFiles", "getMaxUploads", "getFileLimit", "getMaxFileCount",
    "getMaxAttachments", "getAttachmentLimit", "getMaxImages",
)

private fun BytecodePatchContext.foldUploadLimits(): Int {
    var patched = 0
    val value = 100
    classDefForEach { classDef ->
        val mutableClass = mutableClassDefBy(classDef)
        for (method in mutableClass.methods) {
            val impl = method.implementation ?: continue
            val instructions = impl.instructions.toList()
            for ((index, insn) in instructions.withIndex()) {
                val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                if (ref.name !in UPLOAD_NAMES) continue
                if (ref.returnType != "I") continue
                val next = instructions.getOrNull(index + 1) as? OneRegisterInstruction
                if (next != null && next.opcode == Opcode.MOVE_RESULT) {
                    val reg = next.registerA
                    // 100 fits in const/16
                    method.replaceInstruction(index, "const/16 v$reg, 0x64")
                    method.replaceInstruction(index + 1, "nop")
                    patched++
                }
            }
        }
    }
    return patched
}

@Suppress("unused")
val increaseUploadLimitPatch = bytecodePatch(
    name = "Increase Upload Limit",
    description = "Gemini: raises hidden file/attachment count limits (getMaxFiles/getMaxUploads) to 100.",
    default = false,
) {
    compatibleWith("com.google.android.apps.bard")

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldUploadLimits()
        if (patched > 0) logger.info("Increased $patched upload limit(s) to 100")
        else logger.warning("No upload limit checks found. No changes applied.")
    }
}
