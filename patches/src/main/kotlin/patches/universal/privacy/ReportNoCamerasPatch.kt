package patches.universal.privacy

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import patches.universal.ads.util.findMutableMethodOf
import java.util.logging.Logger

@Suppress("unused")
val reportNoCamerasPatch = bytecodePatch(
    name = "Report No Cameras",
    description = "Report an empty camera list to apps.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Privacy") } catch (_: NoSuchMethodError) {}
    val enabled by booleanOption(
        title = "Hide cameras",
        default = true,
        key = "hideCameras",
        description = "Report zero cameras (true) or leave the camera list alone (false).",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        if (enabled != true) {
            logger.warning("Disabled by option. No changes applied.")
            return@execute
        }
        var patched = 0
        classDefForEach { classDef ->
            var hasRef = false
            for (m in classDef.methods) {
                val impl = m.implementation ?: continue
                for (insn in impl.instructions) {
                    val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                    if (ref.definingClass == "Landroid/hardware/camera2/CameraManager;" &&
                        ref.name == "getCameraIdList" &&
                        ref.returnType == "[Ljava/lang/String;"
                    ) { hasRef = true; break }
                }
                if (hasRef) break
            }
            if (!hasRef) return@classDefForEach
            val mutableClass by lazy { mutableClassDefBy(classDef) }
            for (method in classDef.methods) {
                val mutableMethod by lazy { mutableClass.findMutableMethodOf(method) }
                val implementation = method.implementation ?: continue
                val instructions: List<Instruction> = implementation.instructions.toList()
                for ((index, instruction) in instructions.withIndex()) {
                    val reference =
                        (instruction as? ReferenceInstruction)?.reference as? MethodReference
                            ?: continue
                    if (reference.definingClass != "Landroid/hardware/camera2/CameraManager;") continue
                    if (reference.name != "getCameraIdList") continue
                    if (reference.returnType != "[Ljava/lang/String;") continue

                    val next = instructions.getOrNull(index + 1)
                    if (next != null && next.opcode == Opcode.MOVE_RESULT_OBJECT) {
                        val resultRegister = (next as OneRegisterInstruction).registerA
                        mutableMethod.replaceInstruction(index, "const/4 v$resultRegister, 0x0")
                        mutableMethod.replaceInstruction(index + 1, "new-array v$resultRegister, v$resultRegister, [Ljava/lang/String;")
                        patched++
                    }
                }
            }
        }
        if (patched > 0) {
            logger.info("Emptied camera list at $patched call site(s)")
        } else {
            logger.warning("No CameraManager.getCameraIdList call sites found. No changes applied.")
        }
    }
}
