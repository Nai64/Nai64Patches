package patches.universal.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x

@Suppress("unused")
val installSourceSpoofPatch = bytecodePatch(
    name = "Spoof Play Store Install Source",
    description = "Makes the app think it was installed from Google Play Store. " +
            "Prevents Pairip license check from failing.",
    default = false,
) {
    execute {
        val method = PerformLocalInstallerCheckFingerprint.methodOrNull ?: return@execute
        method.addInstructions(0, listOf(
            BuilderInstruction11n(Opcode.CONST_4, 0, 1),
            BuilderInstruction11x(Opcode.RETURN, 0),
        ))
    }
}
