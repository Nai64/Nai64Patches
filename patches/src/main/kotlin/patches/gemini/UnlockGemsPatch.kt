package patches.gemini

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger

private val GEMS_NAMES = setOf(
    "isGemsEnabled", "isGemsAvailable", "hasGemsAccess", "isGemsSupported",
    "isCanvasEnabled", "isCanvasAvailable", "hasCanvasAccess",
    "isDeepResearchEnabled", "isDeepResearchAvailable", "hasDeepResearchAccess",
    "isGemsCreationEnabled", "isCustomGemsEnabled",
)

private fun BytecodePatchContext.foldGemsChecks(): Int {
    var patched = 0
    classDefForEach { classDef ->
        val mutableClass = mutableClassDefBy(classDef)
        for (method in mutableClass.methods) {
            val impl = method.implementation ?: continue
            val instructions = impl.instructions.toList()
            for ((index, insn) in instructions.withIndex()) {
                val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                if (ref.name !in GEMS_NAMES) continue
                if (ref.returnType != "Z") continue
                val next = instructions.getOrNull(index + 1) as? OneRegisterInstruction
                if (next != null && next.opcode == Opcode.MOVE_RESULT) {
                    method.replaceInstruction(index, "const/4 v${next.registerA}, 0x1")
                    method.replaceInstruction(index + 1, "nop")
                    patched++
                }
            }
        }
    }
    return patched
}

@Suppress("unused")
val unlockGemsPatch = bytecodePatch(
    name = "Unlock Gems",
    description = "Gemini: enables Gems, Canvas and Deep Research features by spoofing their availability checks.",
    default = false,
) {
    compatibleWith("com.google.android.apps.bard")

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldGemsChecks()
        if (patched > 0) logger.info("Enabled $patched Gems/Canvas/DeepResearch check(s)")
        else logger.warning("No Gems checks found. No changes applied.")
    }
}
