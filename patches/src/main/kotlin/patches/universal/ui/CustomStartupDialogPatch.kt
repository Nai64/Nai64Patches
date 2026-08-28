package patches.universal.ui

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import app.morphe.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import patches.universal.ads.util.cloneMutable
import patches.universal.ads.util.p0Register
import java.util.logging.Logger

@Suppress("unused")
val customStartupDialogPatch = bytecodePatch(
    name = "Custom Startup Dialog",
    description = "Shows a customizable dialog once when the app is opened",
    default = false,
) {
    dependsOn(StartupHooks.resolveRealApplicationPatch)

    val title by stringOption(
        title = "Dialog title",
        default = "Patched App",
        key = "startupDialogTitle",
        description = "Title of the startup dialog.",
    )
    val message by stringOption(
        title = "Dialog message",
        default = "This app has been modified by Nai's Patches.",
        key = "startupDialogMessage",
        description = "Body text of the startup dialog.",
    )
    val cancelable by booleanOption(
        title = "Dismissable",
        default = true,
        key = "startupDialogCancelable",
        description = "Allow closing the dialog with the back button or outside taps.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val text = message.orEmpty().trim()
        if (text.isEmpty()) {
            logger.warning("No message configured. No changes applied.")
            return@execute
        }

        // Activity detection must walk the superclass chain: many apps extend
        // androidx.appcompat.app.AppCompatActivity (or some app-specific base),
        // whose literal superclass name does NOT end in "Activity;".
        val superMap = mutableMapOf<String, String>()
        classDefForEach { classDef -> classDef.superclass?.let { superMap[classDef.type] = it } }
        fun isActivity(type: String, seen: MutableSet<String> = mutableSetOf()): Boolean {
            if (type == "Landroid/app/Activity;") return true
            if (type == "Ljava/lang/Object;" || type in seen) return false
            seen.add(type)
            val superType = superMap[type] ?: return false
            return isActivity(superType, seen)
        }

        // The guard flag lives on the real Application class so every activity
        // shares it. Prefer the manifest-declared class; fall back to the first
        // direct Application subclass when the manifest is unavailable.
        val guardOwner = StartupHooks.resolvedApplicationDescriptor ?: run {
            var found: String? = null
            classDefForEach { classDef ->
                if (found != null) return@classDefForEach
                if (classDef.superclass == "Landroid/app/Application;") found = classDef.type
            }
            found
        } ?: run {
            logger.warning("No Application class found. No changes applied.")
            return@execute
        }

        mutableClassDefByOrNull(guardOwner)?.let { appClass ->
            if (appClass.fields.none { it.name == StartupHooks.GUARD_FIELD }) {
                appClass.fields.add(
                    ImmutableField(
                        guardOwner,
                        StartupHooks.GUARD_FIELD,
                        "Z",
                        AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
                        null,
                        emptySet(),
                        emptySet(),
                    ).toMutable(),
                )
            }
        }
        val guard = "$guardOwner->${StartupHooks.GUARD_FIELD}:Z"

        val cancelableFlag = cancelable ?: true

        // Collect every real activity that has an onCreate(Bundle).
        val candidates = mutableListOf<Pair<MutableClass, MutableMethod>>()
        classDefForEach { classDef ->
            if (!isActivity(classDef.type)) return@classDefForEach
            val mutableClass = mutableClassDefBy(classDef)
            val onCreate = mutableClass.methods.firstOrNull {
                it.name == "onCreate" && it.returnType == "V" &&
                    it.parameterTypes == listOf("Landroid/os/Bundle;")
            } ?: return@classDefForEach
            candidates.add(mutableClass to onCreate)
        }

        // Skip the launcher activity only when the app has more than one
        // activity, so a transient splash screen (which immediately transitions
        // to the real main activity) does not flash and dismiss the dialog.
        // Single-activity apps keep the launcher as the injection target.
        val launcher = StartupHooks.resolvedLauncherActivityDescriptor
        val skipLauncher = launcher != null && candidates.size > 1

        var patchedActivities = 0
        for ((mutableClass, onCreate) in candidates) {
            if (skipLauncher && mutableClass.type == launcher) continue
            injectStartupDialog(onCreate, mutableClass, guard, title.orEmpty(), text, cancelableFlag)
            patchedActivities++
        }

        if (patchedActivities > 0) {
            logger.info(
                "Startup dialog injected into $patchedActivities activit(ies), guarded by $guard",
            )
        } else {
            logger.warning("No activity onCreate methods found. No changes applied.")
        }
    }
}

/**
 * Clones [onCreate] with five extra registers and prepends a once-per-process
 * guarded AlertDialog show. The first activity created wins; later ones skip.
 */
private fun injectStartupDialog(
    onCreate: MutableMethod,
    owner: MutableClass,
    guard: String,
    title: String,
    message: String,
    cancelable: Boolean,
) {
    val impl = onCreate.implementation ?: return
    val tempBase = impl.registerCount

    // NOTE: cloneMutable shifts parameters to the top of the new register
    // window (it prepends moves that refresh the OLD parameter slots for the
    // sake of the original body). Any reference to p0 must therefore be
    // resolved against the CLONED method, not the original.
    val cloned = onCreate.cloneMutable(additionalRegisters = 5)
    val contextReg = cloned.p0Register

    val b = tempBase
    // Every multi-register invoke uses the /range variant with an exact
    // operand span (plain invoke-* cannot address registers above v15).
    // v(b+2) doubles as the context copy slot and then as the string scratch,
    // keeping each range at exactly receiver + argument.
    val fullSmali = StringBuilder().apply {
        appendLine("sget-boolean v$b, $guard")
        appendLine("if-nez v$b, :nai64_skip")
        appendLine("const/16 v$b, 0x1")
        appendLine("sput-boolean v$b, $guard")
        appendLine("move-object/from16 v${b + 2}, v$contextReg")
        appendLine("new-instance v${b + 1}, Landroid/app/AlertDialog\$Builder;")
        appendLine("invoke-direct/range {v${b + 1} .. v${b + 2}}, Landroid/app/AlertDialog\$Builder;-><init>(Landroid/content/Context;)V")
        appendLine("const-string v${b + 2}, \"${StartupHooks.escapeSmali(title)}\"")
        appendLine("invoke-virtual/range {v${b + 1} .. v${b + 2}}, Landroid/app/AlertDialog\$Builder;->setTitle(Ljava/lang/CharSequence;)Landroid/app/AlertDialog\$Builder;")
        appendLine("const-string v${b + 2}, \"${StartupHooks.escapeSmali(message)}\"")
        appendLine("invoke-virtual/range {v${b + 1} .. v${b + 2}}, Landroid/app/AlertDialog\$Builder;->setMessage(Ljava/lang/CharSequence;)Landroid/app/AlertDialog\$Builder;")
        if (!cancelable) {
            appendLine("const/16 v${b + 2}, 0x0")
            appendLine("invoke-virtual/range {v${b + 1} .. v${b + 2}}, Landroid/app/AlertDialog\$Builder;->setCancelable(Z)Landroid/app/AlertDialog\$Builder;")
        }
        appendLine("const-string v${b + 2}, \"OK\"")
        appendLine("const/16 v${b + 3}, 0x0")
        appendLine("invoke-virtual/range {v${b + 1} .. v${b + 3}}, Landroid/app/AlertDialog\$Builder;->setPositiveButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface\$OnClickListener;)Landroid/app/AlertDialog\$Builder;")
        appendLine("invoke-virtual/range {v${b + 1}}, Landroid/app/AlertDialog\$Builder;->show()Landroid/app/AlertDialog;")
        appendLine(":nai64_skip")
        appendLine("nop")
    }.toString()

    // addInstructionsWithLabels (not addInstructions) re-points internal branch
    // labels to real labels inside the target method; plain addInstructions
    // leaves them dangling in the compiler's template context, which crashes
    // DEX writing with "label that hasn't been placed".
    cloned.addInstructionsWithLabels(0, fullSmali)

    owner.methods.remove(onCreate)
    owner.methods.add(cloned)
}
