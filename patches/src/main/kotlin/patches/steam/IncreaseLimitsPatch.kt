package patches.steam

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger

private val LIMIT_BOOL = setOf(
    "isLimitReached", "isQuotaExceeded", "isRateLimited", "isLimitedAccount",
    "hasReachedLimit", "isOverQuota", "isThrottled", "isFileTooLarge",
)
private val LIMIT_INT = setOf(
    "getMaxFiles", "getMaxUploads", "getFileLimit", "getMaxFileCount",
    "getMaxImages", "getMaxAttachments", "getAttachmentLimit",
    "getMaxTokens", "getContextLimit", "getMaxContextLength",
)

private fun BytecodePatchContext.foldSteamLimits(): Int {
    var patched = 0
    classDefForEach { classDef ->
        val mutableClass = mutableClassDefBy(classDef)
        for (method in mutableClass.methods) {
            val impl = method.implementation ?: continue
            val instructions = impl.instructions.toList()
            for ((index, insn) in instructions.withIndex()) {
                val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                if (ref.returnType == "Z" && ref.name in LIMIT_BOOL) {
                    val next = instructions.getOrNull(index + 1) as? OneRegisterInstruction
                    if (next != null && next.opcode == Opcode.MOVE_RESULT) {
                        method.replaceInstruction(index, "const/4 v${next.registerA}, 0x0")
                        method.replaceInstruction(index + 1, "nop")
                        patched++
                    }
                } else if (ref.returnType == "I" && ref.name in LIMIT_INT) {
                    val next = instructions.getOrNull(index + 1) as? OneRegisterInstruction
                    if (next != null && next.opcode == Opcode.MOVE_RESULT) {
                        val reg = next.registerA
                        val constInstr = if (ref.name == "getMaxTokens" || ref.name == "getContextLimit" || ref.name == "getMaxContextLength") {
                            // 1M
                            if (reg <= 0xff) "const v$reg, 0xF4240" else "const v$reg, 0xF4240"
                        } else {
                            "const/16 v$reg, 0x64" // 100
                        }
                        // Use const for large, const/16 for 100
                        val instr = if (constInstr.contains("0xF4240")) {
                            // 1000000 needs const (32-bit)
                            "const v$reg, 0xF4240"
                        } else {
                            "const/16 v$reg, 0x64"
                        }
                        method.replaceInstruction(index, instr)
                        method.replaceInstruction(index + 1, "nop")
                        patched++
                    }
                }
            }
        }
    }
    return patched
}

@Suppress("unused")
val increaseLimitsPatch = bytecodePatch(
    name = "Increase Limits",
    description = "Steam: removes quota/rate limits and raises file/context caps (getMaxFiles→100, getMaxTokens→1M).",
    default = false,
) {
    compatibleWith("com.valvesoftware.android.steam.community")

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldSteamLimits()
        if (patched > 0) logger.info("Patched $patched limit check(s)")
        else logger.warning("No limit checks found. No changes applied.")
    }
}
