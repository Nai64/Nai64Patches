package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.booleanOption
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
    "USER",
    "HOST",
    "RADIO",
    "BOOTLOADER",
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
 * Folds `System.getProperty(key)` call sites into the mapped constant string,
 * so emulators that probe telltale properties (`ro.kernel.qemu`, `ro.hardware`,
 * `ro.product.model`, etc.) cannot tell.
 *
 * The key is matched via the `const-string` immediately preceding the invoke
 * (the standard single-argument layout), which avoids register parsing that
 * the dexlib2 35c interface does not expose for single-arg invokes.
 *
 * @return number of patched call sites.
 */
internal fun BytecodePatchContext.foldSystemPropertyMap(properties: Map<String, String>): Int {
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
                val value = if (keyValue != null) properties[keyValue] else null
                if (value == null) continue

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

/** Backwards-compatible wrapper: fold the given properties all to [value]. */
internal fun BytecodePatchContext.foldQemuProperties(properties: Set<String>, value: String): Int =
    foldSystemPropertyMap(properties.associateWith { value })

/**
 * Folds a no-argument `Build.<method>()` call that returns a String (e.g.
 * `getSerial()`, `getRadioVersion()`) into a constant string. Emulators often
 * report a null/empty radio version, which some apps use to detect them.
 *
 * @return number of patched call sites.
 */
internal fun BytecodePatchContext.foldBuildMethodResult(methodName: String, value: String): Int {
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
                if (reference.name != methodName || reference.returnType != "Ljava/lang/String;") continue
                if (reference.parameterTypes.isNotEmpty()) continue

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

/** Convenience wrapper for `Build.getRadioVersion()`. */
internal fun BytecodePatchContext.foldBuildGetRadioVersion(value: String): Int =
    foldBuildMethodResult("getRadioVersion", value)

/**
 * Folds no-argument `TelephonyManager.getPhoneType()` into a constant
 * (default GSM), so emulators that report no phone type (NONE) cannot tell.
 *
 * @return number of patched call sites.
 */
internal fun BytecodePatchContext.foldPhoneType(value: Int): Int {
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
                if (reference.definingClass != "Landroid/telephony/TelephonyManager;") continue
                if (reference.name != "getPhoneType" || reference.returnType != "I") continue
                if (reference.parameterTypes.isNotEmpty()) continue

                val next = instructions.getOrNull(index + 1)
                val register = (next as? OneRegisterInstruction)?.registerA
                if (next != null &&
                    (next.opcode == Opcode.MOVE_RESULT ||
                        next.opcode == Opcode.MOVE_RESULT_OBJECT)
                ) {
                    method.replaceInstruction(index, "const/4 v$register, ${value.and(0xf)}")
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
        "USER" to "android",
        "HOST" to "aosp-build",
        "BOOTLOADER" to "redfin-1.0",
        "RADIO" to "g5123b-220720-2307180001",
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
        "USER" to "android",
        "HOST" to "aosp-build",
        "BOOTLOADER" to "r0s-1.0",
        "RADIO" to "g0sxx-220720-2307180001",
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
        "USER" to "android",
        "HOST" to "aosp-build",
        "BOOTLOADER" to "fuxi-1.0",
        "RADIO" to "vk3a-220720-2307180001",
    ),
)

@Suppress("unused")
val bypassEmulatorDetectionPatch = bytecodePatch(
    name = "Bypass Emulator Detection",
    description =
        "Spoofs android.os.Build identity (model, device, manufacturer, hardware, " +
            "fingerprint, serial, user, host, radio, bootloader) and Build.getRadioVersion(), " +
            "TelephonyManager.getPhoneType(), and telltale system properties " +
            "(ro.kernel.qemu, ro.hardware, ro.product.model/device, ro.bootloader, ro.radio) so " +
            "apps and games that refuse to run, crash or match you with emulator lobbies cannot " +
            "tell they are on an emulator. Does not hide root or a debugger connection.",
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
    val hideEmulatorRadio by booleanOption(
        title = "Hide Emulator Radio",
        default = false,
        key = "hideEmulatorRadio",
        description = "Make TelephonyManager report a normal GSM radio type.",
    )
    val spoofBuildExtras by booleanOption(
        title = "Spoof Build Extras",
        default = false,
        key = "spoofBuildExtras",
        description = "Spoof additional Build and Build.VERSION fields used by emulator checks.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val preset = DEVICE_PRESETS[profile.orEmpty().trim()] ?: DEVICE_PRESETS.getValue("pixel6")

        val patchedBuild = foldBuildStringFields(preset)
        val patchedSerial = foldBuildGetSerial(preset.getValue("SERIAL"))
        val patchedRadio = foldBuildGetRadioVersion(preset.getValue("RADIO"))
        val patchedPhone = foldPhoneType(1)

        // Spoof emulator-revealing system properties to the chosen device's
        // identity (or "0" for pure QEMU flags).
        val emulatorProps = mapOf(
            "ro.kernel.qemu" to "0",
            "ro.kernel.qemu.device" to "0",
            "ro.hardware" to preset.getValue("HARDWARE"),
            "ro.kernel.androidboot.hardware" to preset.getValue("HARDWARE"),
            "ro.product.model" to preset.getValue("MODEL"),
            "ro.product.device" to preset.getValue("DEVICE"),
            "ro.bootloader" to preset.getValue("BOOTLOADER"),
            "ro.radio" to preset.getValue("RADIO"),
            "qemu.hw.mainkeys" to "0",
        )
        val patchedProps = foldSystemPropertyMap(emulatorProps)

        if (hideEmulatorRadio == true) {
            hideEmulatorRadioPatch.execute(this)
        }
        if (spoofBuildExtras == true) {
            spoofBuildExtrasPatch.execute(this)
        }

        val total = patchedBuild + patchedSerial + patchedRadio + patchedPhone + patchedProps
        if (total > 0) {
            logger.info("Spoofed $total emulator-detection check(s) [profile=${profile.orEmpty()}]")
        } else {
            logger.warning("No emulator-detection checks found. No changes applied.")
        }
    }
}
