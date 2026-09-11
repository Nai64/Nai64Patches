package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import patches.universal.ads.util.findMutableMethodOf
import java.util.logging.Logger

@Suppress("unused")
val fakeOnlineStatePatch = bytecodePatch(
    name = "Fake Online State",
    description =
        "Makes connectivity checks report an active connection: WifiManager.isWifiEnabled, " +
            "NetworkInfo.isConnected/isAvailable/isConnectedOrConnecting all return true",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Fake") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        fun isForcedTrue(reference: MethodReference): Boolean {
            if (reference.returnType != "Z") return false
            return when (reference.definingClass) {
                "Landroid/net/wifi/WifiManager;" -> reference.name == "isWifiEnabled"
                "Landroid/net/NetworkInfo;" ->
                    reference.name in setOf("isConnected", "isAvailable", "isConnectedOrConnecting")

                else -> false
            }
        }

        var patched = 0
        classDefForEach { classDef ->
            val mutableClass by lazy { mutableClassDefBy(classDef) }
            for (method in classDef.methods) {
                val mutableMethod by lazy { mutableClass.findMutableMethodOf(method) }
                val implementation = method.implementation ?: continue
                // Snapshot; one-for-one replacements keep indices valid.
                val instructions = implementation.instructions.toList()
                for ((index, instruction) in instructions.withIndex()) {
                    val reference =
                        (instruction as? ReferenceInstruction)?.reference as? MethodReference
                            ?: continue
                    if (!isForcedTrue(reference)) continue

                    val next = instructions.getOrNull(index + 1) as? OneRegisterInstruction
                    if (next != null && next.opcode == Opcode.MOVE_RESULT) {
                        // Fold invoke + move-result into a single constant.
                        mutableMethod.replaceInstruction(index, "const/4 v${next.registerA}, 0x1")
                        mutableMethod.replaceInstruction(index + 1, "nop")
                    } else {
                        // Result unused: dropping the call entirely is safe.
                        mutableMethod.replaceInstruction(index, "nop")
                    }
                    patched++
                }
            }
        }

        if (patched > 0) {
            logger.info("Forced $patched connectivity check(s) to true")
        } else {
            logger.warning("No connectivity checks found. No changes applied.")
        }
    }
}
