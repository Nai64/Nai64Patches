package patches.gemini

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger

private val EXT_NAMES = setOf(
    "isExtensionEnabled", "isExtensionsEnabled", "hasExtensionAccess",
    "isWorkspaceEnabled", "isWorkspaceExtensionEnabled", "isGmailExtensionEnabled",
    "isDriveExtensionEnabled", "isYoutubeExtensionEnabled", "isMapsExtensionEnabled",
    "isExtensionSupported", "hasWorkspaceAccess",
)

private fun BytecodePatchContext.foldExtensionChecks(): Int {
    var patched = 0
    classDefForEach { classDef ->
        val mutableClass = mutableClassDefBy(classDef)
        for (method in mutableClass.methods) {
            val impl = method.implementation ?: continue
            val instructions = impl.instructions.toList()
            for ((index, insn) in instructions.withIndex()) {
                val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                if (ref.name !in EXT_NAMES) continue
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
val unlockGeminiExtensionsPatch = bytecodePatch(
    name = "Unlock Gemini Extensions",
    description = "Gemini: enables Workspace/Gmail/Drive/YouTube/Maps extensions by spoofing their gated checks.",
    default = false,
) {
    compatibleWith("com.google.android.apps.bard")

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldExtensionChecks()
        if (patched > 0) logger.info("Enabled $patched Extensions check(s)")
        else logger.warning("No Extensions checks found. No changes applied.")
    }
}
