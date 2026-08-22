package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import patches.universal.ui.StartupHooks.escapeSmali
import java.util.logging.Logger

/** Build identity fields spoofed to look like a real device. */
private val BUILD_STRING_FIELDS = setOf(
    "MODEL",
    "DEVICE",
    "MANUFACTURER",
    "BRAND",
    "PRODUCT",
    "BOARD",
    "HARDWARE",
    "FINGERPRINT",
    "DISPLAY",
    "ID",
    "TYPE",
    "TAGS",
    "SERIAL",
)

/** System properties that reveal an emulator/QEMU environment. */
private val QEMU_PROPERTIES = setOf(
    "ro.kernel.qemu",
    "ro.kernel.qemu.device",
)

/**
 * Replaces every `sget-object vX, Landroid/os/Build;-><FIELD>:Ljava/lang/String;`
 * with `const-string vX, "<value>"`, so the app sees a real device's identity.
 *
 * @return number of patched field reads.
 */
internal fun BytecodePatchContext.foldBuildStringFields(values: Map<String, String>): Int {
    var patched = 0
    classDefForEach { classDef ->
        val mutableClass = mutableClassDefBy(classDef)
        for (method in mutableClass.methods) {
            val implementation = method.implementation ?: continue
            val instructions: List<Instruction> = implementation.instructions.toList()
            for ((index, instruction) in instructions.withIndex()) {
                if (instruction.opcode != Opcode.SGET_OBJECT) continue
                val reference =
                    (instruction as? ReferenceInstruction)?.reference as? FieldReference
                        ?: continue
                if (reference.definingClass != "Landroid/os/Build;") continue
                if (reference.type != "Ljava/lang/String;") continue
                val value = values[reference.name] ?: continue

                val register = (instruction as? OneRegisterInstruction)?.registerA ?: continue
                method.replaceInstruction(
                    index,
                    "const-string v$register, \"${escapeSmali(value)}\"",
                )
                patched++
            }
        }
    }
    return patched
}

/**
 * Folds `Build.getSerial()` call sites into a constant serial string.
 *
 * @return number of patched call sites.
 */
internal fun BytecodePatchContext.foldBuildGetSerial(value: String): Int {
    var patched = 0
    classDefForEach { classDef ->
        val mutableClass = mutableClassDefBy(classDef)
        for (method in mutableClass.methods) {
            val implementation = method.implementation ?: continue
            val instructions: List<Instruction> = implementation.instructions.toList()
            for ((index, instruction) in instructions.withIndex()) {
                val reference =
                    (instruction as? ReferenceInstruction)?.reference as? MethodReference
                        ?: continue
                if (reference.definingClass != "Landroid/os/Build;") continue
                if (reference.name != "getSerial" || reference.returnType != "Ljava/lang/String;") continue

                val next = instructions.getOrNull(index + 1)
                val register = (next as? OneRegisterInstruction)?.registerA
                if (next != null &&
                    (next.opcode == Opcode.MOVE_RESULT ||
                        next.opcode == Opcode.MOVE_RESULT_OBJECT)
                ) {
                    method.replaceInstruction(index, "const-string v$register, \"${escapeSmali(value)}\"")
                    method.replaceInstruction(index + 1, "nop")
                } else {
                    method.replaceInstruction(index, "nop")
                }
                patched++
            }
        }
    }
    return patched
}

/**
 * Folds `System.getProperty(key)` call sites for [properties] into a constant
 * `0` (no QEMU), so emulators that probe `ro.kernel.qemu` cannot tell.
 *
 * The key is matched via the `const-string` immediately preceding the invoke
 * (the standard single-argument layout), which avoids register parsing that
 * the dexlib2 35c interface does not expose for single-arg invokes.
 *
 * @return number of patched call sites.
 */
internal fun BytecodePatchContext.foldQemuProperties(properties: Set<String>, value: String): Int {
    var patched = 0
    classDefForEach { classDef ->
        val mutableClass = mutableClassDefBy(classDef)
        for (method in mutableClass.methods) {
            val implementation = method.implementation ?: continue
            val instructions: List<Instruction> = implementation.instructions.toList()
            for ((index, instruction) in instructions.withIndex()) {
                val reference =
                    (instruction as? ReferenceInstruction)?.reference as? MethodReference
                        ?: continue
                if (reference.definingClass != "Ljava/lang/System;") continue
                if (reference.name != "getProperty" || reference.returnType != "Ljava/lang/String;") continue
                if (reference.parameterTypes != listOf("Ljava/lang/String;")) continue

                // The key must be the const-string right before the invoke.
                val prev = instructions.getOrNull(index - 1)
                val keyValue = if (prev != null && prev.opcode == Opcode.CONST_STRING) {
                    ((prev as? ReferenceInstruction)?.reference as? StringReference)?.string
                } else {
                    null
                }
                if (keyValue !in properties) continue

                val next = instructions.getOrNull(index + 1)
                val register = (next as? OneRegisterInstruction)?.registerA
                if (next != null &&
                    (next.opcode == Opcode.MOVE_RESULT ||
                        next.opcode == Opcode.MOVE_RESULT_OBJECT)
                ) {
                    method.replaceInstruction(index, "const-string v$register, \"${escapeSmali(value)}\"")
                    method.replaceInstruction(index + 1, "nop")
                } else {
                    method.replaceInstruction(index, "nop")
                }
                patched++
            }
        }
    }
    return patched
}

/** Device identity presets keyed by their `BytecodePatchContext` option value. */
private val DEVICE_PRESETS = mapOf(
    "pixel6" to mapOf(
        "MODEL" to "Pixel 6",
        "DEVICE" to "redfin",
        "MANUFACTURER" to "Google",
        "BRAND" to "google",
        "PRODUCT" to "redfin",
        "BOARD" to "redfin",
        "HARDWARE" to "redfin",
        "FINGERPRINT" to "google/redfin/redfin:13/TQ3A.230805.001/10161055:user/release-keys",
        "DISPLAY" to "TQ3A.230805.001",
        "ID" to "TQ3A.230805.001",
        "TYPE" to "user",
        "TAGS" to "release-keys",
        "SERIAL" to "1A2B3C4D5E6F",
    ),
    "samsungS23" to mapOf(
        "MODEL" to "SM-S918B",
        "DEVICE" to "r0s",
        "MANUFACTURER" to "samsung",
        "BRAND" to "samsung",
        "PRODUCT" to "r0sxxx",
        "BOARD" to "s5e9925",
        "HARDWARE" to "s5e9925",
        "FINGERPRINT" to "samsung/r0sxxx/r0s:13/TP1A.220624.014/S918BXXU1AWAH:user/release-keys",
        "DISPLAY" to "TP1A.220624.014",
        "ID" to "TP1A.220624.014",
        "TYPE" to "user",
        "TAGS" to "release-keys",
        "SERIAL" to "RZ8N70ABCD12EF",
    ),
    "xiaomi13" to mapOf(
        "MODEL" to "Xiaomi 13",
        "DEVICE" to "fuxi",
        "MANUFACTURER" to "Xiaomi",
        "BRAND" to "Xiaomi",
        "PRODUCT" to "fuxi",
        "BOARD" to "fuxi",
        "HARDWARE" to "fuxi",
        "FINGERPRINT" to "Xiaomi/fuxi/fuxi:13/TKQ1.220829.002/V14.0.4.0.TMCCNXM:user/release-keys",
        "DISPLAY" to "TKQ1.220829.002",
        "ID" to "TKQ1.220829.002",
        "TYPE" to "user",
        "TAGS" to "release-keys",
        "SERIAL" to "abcdef0123456789",
    ),
)

@Suppress("unused")
val bypassEmulatorDetectionPatch = bytecodePatch(
    name = "Bypass Emulator Detection",
    description =
        "Spoofs android.os.Build identity (model, device, manufacturer, hardware, " +
            "fingerprint, serial) and the ro.kernel.qemu property so apps and games that " +
            "refuse to run, crash or match you with emulator lobbies cannot tell they are " +
            "on an emulator. Does not hide root or a debugger connection.",
    default = false,
) {
    val profile by stringOption(
        title = "Device profile",
        default = "pixel6",
        values = linkedMapOf(
            "Pixel 6" to "pixel6",
            "Samsung Galaxy S23" to "samsungS23",
            "Xiaomi 13" to "xiaomi13",
        ),
        key = "emulatorProfile",
        description = "Which real device identity to imitate.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val preset = DEVICE_PRESETS[profile.orEmpty().trim()] ?: DEVICE_PRESETS.getValue("pixel6")

        val patchedBuild = foldBuildStringFields(preset)
        val patchedSerial = foldBuildGetSerial(preset.getValue("SERIAL"))
        val patchedQemu = foldQemuProperties(QEMU_PROPERTIES, "0")

        val total = patchedBuild + patchedSerial + patchedQemu
        if (total > 0) {
            logger.info("Spoofed $total emulator-detection check(s) [profile=${profile.orEmpty()}]")
        } else {
            logger.warning("No emulator-detection checks found. No changes applied.")
        }
    }
}
