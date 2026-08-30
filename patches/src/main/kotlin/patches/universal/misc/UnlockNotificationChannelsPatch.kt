package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger

@Suppress("unused")
val unlockNotificationChannelsPatch = bytecodePatch(
    name = "Unlock Notification Channels",
    description = "Makes all notification channels blockable and their importance unlocked so greyed-out toggles become interactable.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        // Single-pass: was 3 separate classDefForEach scans (foldBooleanGetterConst x2 + foldBooleanReturns) -> 750MB transient on large apps.
        // Merged to 1 scan to fix #59 Truecaller 1024 MB OOM.
        var patched = 0
        classDefForEach { classDef ->
            val mutableClass = mutableClassDefBy(classDef)
            for (method in mutableClass.methods) {
                val impl = method.implementation ?: continue
                val instructions = impl.instructions.toList()
                for ((index, insn) in instructions.withIndex()) {
                    val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                    if (ref.definingClass != "Landroid/app/NotificationChannel;") continue
                    if (ref.returnType != "Z") continue
                    val value = when (ref.name) {
                        "isBlockable" -> "0x1"
                        "isImportanceLockedByOEM", "isImportanceLockedByCriticalDeviceFunction", "isBlockableSystem" -> "0x0"
                        else -> continue
                    }
                    val next = instructions.getOrNull(index + 1) as? OneRegisterInstruction
                    if (next != null && next.opcode == Opcode.MOVE_RESULT) {
                        method.replaceInstruction(index, "const/4 v${next.registerA}, $value")
                        method.replaceInstruction(index + 1, "nop")
                    } else {
                        method.replaceInstruction(index, "nop")
                    }
                    patched++
                }
            }
        }

        if (patched > 0) {
            logger.info("Unlocked $patched notification channel check(s)")
        } else {
            logger.warning("No NotificationChannel blockable/importance checks found. No changes applied.")
        }
    }
}
