package patches.steam

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import java.util.logging.Logger

private fun BytecodePatchContext.forceDebugFlag(): Int {
    var patched = 0
    classDefForEach { classDef ->
        val mutableClass = mutableClassDefBy(classDef)
        for (method in mutableClass.methods) {
            val impl = method.implementation ?: continue
            val instructions = impl.instructions.toList()
            for ((index, insn) in instructions.withIndex()) {
                if (insn.opcode != Opcode.SGET_BOOLEAN && insn.opcode != Opcode.SGET && insn.opcode != Opcode.SGET_OBJECT) continue
                val ref = (insn as? ReferenceInstruction)?.reference as? FieldReference ?: continue
                if (ref.definingClass != "Lcom/valvesoftware/android/steam/community/BuildConfig;") continue
                if (ref.name != "DEBUG") continue
                if (ref.type != "Z") continue
                val reg = (insn as? OneRegisterInstruction)?.registerA ?: continue
                method.replaceInstruction(index, "const/4 v$reg, 0x1")
                patched++
            }
        }
    }
    return patched
}

@Suppress("unused")
val forceDebugBuildPatch = bytecodePatch(
    name = "Force Debug Build",
    description = "Steam: forces BuildConfig.DEBUG to true to enable debug logs and toasts.",
    default = false,
) {
    compatibleWith("com.valvesoftware.android.steam.community")

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = forceDebugFlag()
        if (patched > 0) logger.info("Forced $patched DEBUG field read(s) to true")
        else logger.warning("No DEBUG field reads found. No changes applied.")
    }
}
