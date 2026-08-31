package patches.universal.update

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import java.util.Locale
import java.util.logging.Logger
import patches.universal.ads.AppUpdateManagerImplStartUpdateFlowFingerprint

private val updateTerms = listOf(
    "update required",
    "please update",
    "new version",
    "latest version",
    "minimum version",
    "min version",
    "outdated version",
    "unsupported version",
    "must update",
    "download the latest",
    "upgrade required",
    "update now",
)

private val updateNameTerms = setOf(
    "update",
    "updates",
    "upgrade",
    "version",
    "outdated",
    "compatibility",
)

private val versionApiNames = setOf(
    "getversioncode",
    "getlongversioncode",
    "getversionname",
    "getpackageinfo",
)

private val updateUrlTerms = listOf(
    "market://",
    "play.google.com/store/apps",
    "apps.apple.com",
    "applicationdetails",
)

private fun String.normalized() = lowercase(Locale.ROOT)

private fun MethodReference.isVoidCall(name: String): Boolean =
    this.name == name && returnType == "V"

private fun methodEvidence(method: app.morphe.patcher.util.proxy.mutableTypes.MutableMethod): Evidence {
    val implementation = method.implementation ?: return Evidence()
    val strings = implementation.instructions.mapNotNull { instruction ->
        ((instruction as? ReferenceInstruction)?.reference as? StringReference)?.string
    }
    val lowerStrings = strings.map(String::normalized)
    val lowerName = method.name.normalized()
    val hasUpdateString = lowerStrings.any { string -> updateTerms.any(string::contains) }
    val hasUpdateUrl = lowerStrings.any { string -> updateUrlTerms.any(string::contains) }
    val hasVersionApi = implementation.instructions.any { instruction ->
        val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            ?: return@any false
        reference.name.normalized() in versionApiNames ||
            reference.definingClass.contains("appupdate", ignoreCase = true) ||
            reference.definingClass.contains("update", ignoreCase = true)
    }
    val hasUpdateName = updateNameTerms.any(lowerName::contains)
    return Evidence(
        score = (if (hasUpdateString) 3 else 0) +
            (if (hasUpdateUrl) 3 else 0) +
            (if (hasVersionApi) 2 else 0) +
            (if (hasUpdateName) 2 else 0),
        hasUpdateString = hasUpdateString,
        hasUpdateUrl = hasUpdateUrl,
    )
}

private data class Evidence(
    val score: Int = 0,
    val hasUpdateString: Boolean = false,
    val hasUpdateUrl: Boolean = false,
)

private fun falseBooleanGate(
    method: app.morphe.patcher.util.proxy.mutableTypes.MutableMethod,
    evidence: Evidence,
): Boolean {
    if (method.returnType != "Z" || evidence.score < 4) return false
    if (method.implementation?.registerCount ?: 0 < 1) return false

    method.addInstructions(
        0,
        """
        const/4 v0, 0x0
        return v0
        """.trimIndent(),
    )
    return true
}

private fun booleanArgumentRegister(instruction: Any): Int? = when (instruction) {
    is BuilderInstruction35c -> if (instruction.registerCount >= 2) instruction.registerD else null
    is BuilderInstruction3rc -> if (instruction.registerCount >= 2) instruction.startRegister + 1 else null
    else -> null
}

private fun isFalseConstant(instruction: Any?, register: Int): Boolean =
    instruction is OneRegisterInstruction &&
        instruction.registerA == register &&
        instruction.opcode in setOf(Opcode.CONST_4, Opcode.CONST_16, Opcode.CONST) &&
        (instruction as? NarrowLiteralInstruction)?.narrowLiteral == 0

@Suppress("unused")
val bypassForcedUpdatesPatch = bytecodePatch(
    name = "Bypass Forced Updates (Experimental)",
    description = "Skip forced update screens and keep using the app.",
    default = false,
) {
    val bypassUpdateGate by booleanOption(
        key = "bypassUpdateGate",
        default = true,
        title = "Bypass update gate",
        description = "Ignore high-confidence client-side checks that report an update is required",
    )
    val makeDialogsDismissible by booleanOption(
        key = "makeDialogsDismissible",
        default = true,
        title = "Make dialogs dismissible",
        description = "Allow detected update dialogs to be canceled instead of forcing the user to update",
    )
    val blockUpdateRedirects by booleanOption(
        key = "blockUpdateRedirects",
        default = true,
        title = "Block update redirects",
        description = "Block detected redirects to the Play Store, browser, or updater activity",
    )
    val preventForcedExit by booleanOption(
        key = "preventForcedExit",
        default = true,
        title = "Prevent forced exit",
        description = "Remove detected finish or exit calls from update-related methods",
    )
    val patchPlayCore by booleanOption(
        key = "patchPlayCore",
        default = true,
        title = "Patch Play Core updates",
        description = "Disable Google Play's in-app update flow when it is present",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var gates = 0
        var dialogs = 0
        var redirects = 0
        var exits = 0
        var playCore = 0

        if (patchPlayCore == true) {
            val method = AppUpdateManagerImplStartUpdateFlowFingerprint.methodOrNull
            if (method != null && (method.implementation?.registerCount ?: 0) >= 1) {
                method.addInstructions(0, "const/4 v0, 0x0\nreturn-object v0")
                playCore++
            }
        }

        classDefForEach { classDef ->
            val mutableClass = mutableClassDefBy(classDef)
            for (method in mutableClass.methods) {
                val implementation = method.implementation ?: continue
                val evidence = methodEvidence(method)
                if (evidence.score < 4) continue

                if (bypassUpdateGate == true && falseBooleanGate(method, evidence)) {
                    gates++
                    continue
                }

                val instructions = implementation.instructions.toList()
                for ((index, instruction) in instructions.withIndex()) {
                    val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
                        ?: continue

                    if (makeDialogsDismissible == true &&
                        reference.definingClass == "Landroid/app/AlertDialog\$Builder;" &&
                        reference.name == "setCancelable" &&
                        reference.parameterTypes == listOf("Z")
                    ) {
                        val register = booleanArgumentRegister(instruction)
                        val previous = instructions.getOrNull(index - 1)
                        if (register != null && isFalseConstant(previous, register)) {
                            method.replaceInstruction(index - 1, "const/4 v$register, 0x1")
                            dialogs++
                        }
                    }

                    if (preventForcedExit == true &&
                        ((reference.definingClass == "Landroid/app/Activity;" &&
                            (reference.isVoidCall("finish") ||
                                reference.isVoidCall("finishAffinity") ||
                                reference.isVoidCall("finishAndRemoveTask"))) ||
                            (reference.definingClass == "Ljava/lang/System;" && reference.isVoidCall("exit")))
                    ) {
                        method.replaceInstruction(index, "nop")
                        exits++
                    }

                    if (blockUpdateRedirects == true &&
                        evidence.hasUpdateUrl &&
                        reference.name in setOf("startActivity", "startActivityForResult") &&
                        reference.returnType == "V"
                    ) {
                        method.replaceInstruction(index, "nop")
                        redirects++
                    }
                }
            }
        }

        val total = gates + dialogs + redirects + exits + playCore
        if (total == 0) {
            logger.warning(
                "No high-confidence forced-update patterns found. This experimental patch " +
                    "cannot guarantee compatibility with custom server-side update enforcement.",
            )
        } else {
            logger.info(
                "Bypass Forced Updates: $gates gate(s), $dialogs dialog(s), " +
                    "$redirects redirect(s), $exits exit(s), $playCore Play Core flow(s) patched",
            )
        }
    }
}
