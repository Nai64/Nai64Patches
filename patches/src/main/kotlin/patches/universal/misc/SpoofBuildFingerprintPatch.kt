package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import patches.universal.ads.util.findMutableMethodOf
import java.util.logging.Logger

private const val BUILD_CLASS = "Landroid/os/Build;"

/** Escapes a value for use inside a smali const-string literal. */
private fun escapeSmali(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")

@Suppress("unused")
val spoofBuildFingerprintPatch = bytecodePatch(
    name = "Spoof Build Fingerprint",
    description =
        "Replaces every read of Build.FINGERPRINT with a custom value, useful for integrity " +
            "and device checks. Leave empty to keep the original fingerprint",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Spoof") } catch (_: NoSuchMethodError) {}
    val fingerprint by stringOption(
        title = "Fingerprint",
        default = "",
        key = "spoofFingerprint",
        description =
            "A full Android build fingerprint, e.g. " +
                "google/mustang/mustang:17/CP2A.260605.012/15430684:user/release-keys",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val value = fingerprint.orEmpty().trim()
        if (value.isEmpty()) {
            logger.warning("No fingerprint configured. No changes applied.")
            return@execute
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
                    val field =
                        (instruction as? ReferenceInstruction)?.reference as? FieldReference
                            ?: continue
                    if (field.definingClass != BUILD_CLASS || field.name != "FINGERPRINT") continue

                    val next = instructions.getOrNull(index + 1) as? OneRegisterInstruction
                    if (next != null && next.opcode == Opcode.MOVE_RESULT_OBJECT) {
                        mutableMethod.replaceInstruction(
                            index,
                            "const-string v${next.registerA}, \"${escapeSmali(value)}\"",
                        )
                        mutableMethod.replaceInstruction(index + 1, "nop")
                        patched++
                    }
                }
            }
        }

        if (patched > 0) {
            logger.info("Spoofed $patched FINGERPRINT read(s)")
        } else {
            logger.warning("No Build.FINGERPRINT reads found. No changes applied.")
        }
    }
}
