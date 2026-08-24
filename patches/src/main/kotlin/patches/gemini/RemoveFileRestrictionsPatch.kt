package patches.gemini

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger

private val FILE_NAMES = setOf(
    "isFileSupported", "isFileSizeAllowed", "isImageSupported",
    "isFileUploadEnabled", "hasFileAccess", "isFileTypeSupported",
    "isImageGenerationSupported", "isImageUploadSupported", "isFileTooLarge",
    "isOverFileLimit", "isFileLimitExceeded",
)

private fun BytecodePatchContext.foldFileChecks(): Int {
    var patched = 0
    classDefForEach { classDef ->
        val mutableClass = mutableClassDefBy(classDef)
        for (method in mutableClass.methods) {
            val impl = method.implementation ?: continue
            val instructions = impl.instructions.toList()
            for ((index, insn) in instructions.withIndex()) {
                val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                if (ref.name !in FILE_NAMES) continue
                if (ref.returnType != "Z") continue
                val next = instructions.getOrNull(index + 1) as? OneRegisterInstruction
                // isFileTooLarge etc. should be forced false (0x0), others true (0x1)
                val forceTrue = ref.name !in setOf("isFileTooLarge", "isOverFileLimit", "isFileLimitExceeded")
                val value = if (forceTrue) "0x1" else "0x0"
                if (next != null && next.opcode == Opcode.MOVE_RESULT) {
                    method.replaceInstruction(index, "const/4 v${next.registerA}, $value")
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
val removeFileRestrictionsPatch = bytecodePatch(
    name = "Remove File Restrictions",
    description = "Gemini: removes file/image upload type and size restrictions by spoofing file support checks.",
    default = false,
) {
    compatibleWith("com.google.android.apps.bard")

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldFileChecks()
        if (patched > 0) logger.info("Patched $patched file restriction check(s)")
        else logger.warning("No file restriction checks found. No changes applied.")
    }
}
