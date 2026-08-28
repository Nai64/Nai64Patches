package patches.universal.privacy

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger

@Suppress("unused")
val hideCurrentLocationPatch = bytecodePatch(
    name = "Hide Current Location",
    description = "Prevents the app from requesting your current location.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        classDefForEach { classDef ->
            val mutableClass = mutableClassDefBy(classDef)
            for (method in mutableClass.methods) {
                val impl = method.implementation ?: continue
                val instructions = impl.instructions.toList()
                for ((index, insn) in instructions.withIndex()) {
                    val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                    if (ref.definingClass != "Landroid/location/LocationManager;" && ref.definingClass != "Lcom/google/android/gms/location/FusedLocationProviderClient;") continue
                    if (ref.name != "getCurrentLocation") continue
                    method.replaceInstruction(index, "nop")
                    patched++
                }
            }
        }
        if (patched > 0) logger.info("Hid current location at $patched call site(s)")
        else logger.warning("No getCurrentLocation call sites found. No changes applied.")
    }
}
