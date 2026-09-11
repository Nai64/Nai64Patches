package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import patches.universal.ads.util.findMutableMethodOf
import java.util.logging.Logger

@Suppress("unused")
val spoofAppSignaturePatch = bytecodePatch(
    name = "Spoof App Signature",
    description = "Bypasses signature checks.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Spoof") } catch (_: NoSuchMethodError) {}
    val spoofSignature by booleanOption(
        key = "spoofSignature",
        default = true,
        title = "Spoof app signature",
        description = "Make PackageManager return the original signing certificate so tamper checks see the app as unmodified",
    )
    val packageNameOverride by stringOption(
        key = "packageNameOverride",
        default = "",
        title = "Package name override",
        description = "Manually define the package name to report (e.g. com.example.app). Leave empty to use the manifest package. Useful when Change Package Name was used.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        if (spoofSignature != true && packageNameOverride.orEmpty().trim().isEmpty()) {
            logger.warning("No spoof options enabled. No changes applied.")
            return@execute
        }

        var patched = 0
        val override = packageNameOverride.orEmpty().trim()

        classDefForEach { classDef ->
            val mutableClass by lazy { mutableClassDefBy(classDef) }
            for (method in classDef.methods) {
                val mutableMethod by lazy { mutableClass.findMutableMethodOf(method) }
                val impl = method.implementation ?: continue
                val instructions = impl.instructions.toList()
                for ((index, insn) in instructions.withIndex()) {
                    val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue

                    // Hook PackageManager.getPackageInfo(String,int) and getPackageArchiveInfo
                    val isGetPackageInfo = ref.definingClass == "Landroid/content/pm/PackageManager;" &&
                        (ref.name == "getPackageInfo" || ref.name == "getPackageArchiveInfo") &&
                        ref.returnType.startsWith("Landroid/content/pm/PackageInfo;")

                    // Hook Context.getPackageName() for package name override
                    val isGetPackageName = ref.definingClass == "Landroid/content/Context;" &&
                        ref.name == "getPackageName" && ref.returnType == "Ljava/lang/String;" &&
                        ref.parameterTypes.isEmpty()

                    if (!isGetPackageInfo && !isGetPackageName) continue

                    // Package name override: force getPackageName() to return custom name
                    if (isGetPackageName && override.isNotEmpty()) {
                        val next = instructions.getOrNull(index + 1) as? OneRegisterInstruction
                        if (next != null && next.opcode == Opcode.MOVE_RESULT_OBJECT) {
                            val reg = next.registerA
                            val escaped = override.replace("\\", "\\\\").replace("\"", "\\\"")
                            mutableMethod.replaceInstruction(index, "const-string v$reg, \"$escaped\"")
                            mutableMethod.replaceInstruction(index + 1, "nop")
                            patched++
                        }
                        continue
                    }
                }
            }
        }

        // Also handle PackageManager.checkSignatures as fallback for signature spoof
        if (spoofSignature == true) {
            var sigPatched = 0
            classDefForEach { classDef ->
                val mutableClass by lazy { mutableClassDefBy(classDef) }
                for (method in classDef.methods) {
                    val mutableMethod by lazy { mutableClass.findMutableMethodOf(method) }
                    val impl = method.implementation ?: continue
                    val instructions = impl.instructions.toList()
                    for ((index, insn) in instructions.withIndex()) {
                        val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                        if (ref.definingClass != "Landroid/content/pm/PackageManager;" || ref.name != "checkSignatures" || ref.returnType != "I") continue
                        val next = instructions.getOrNull(index + 1) as? OneRegisterInstruction
                        if (next != null && next.opcode == Opcode.MOVE_RESULT) {
                            mutableMethod.replaceInstruction(index, "const/4 v${next.registerA}, 0x0")
                            mutableMethod.replaceInstruction(index + 1, "nop")
                            sigPatched++
                        } else {
                            mutableMethod.replaceInstruction(index, "nop")
                            sigPatched++
                        }
                    }
                }
            }
            if (sigPatched > 0) {
                logger.info("Spoofed $sigPatched PackageManager.checkSignatures to SIGNATURE_MATCH")
                patched += sigPatched
            }
        }

        if (patched > 0) {
            logger.info("Spoof App Signature: patched $patched call(s) (override=${override.ifEmpty { "<none>" }})")
        } else {
            logger.warning("No PackageManager calls found for spoof. No changes applied (app may use direct Signature check).")
        }
    }
}
