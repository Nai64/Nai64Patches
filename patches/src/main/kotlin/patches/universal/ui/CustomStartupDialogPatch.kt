package patches.universal.ui

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
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

        // The guard flag lives on the Application class so every activity can see it.
        var appType: String? = null
        classDefForEach { classDef ->
            if (appType != null || classDef.superclass != "Landroid/app/Application;") {
                return@classDefForEach
            }
            val mutableClass = mutableClassDefBy(classDef)
            if (mutableClass.fields.none { it.name == StartupHooks.GUARD_FIELD }) {
                mutableClass.fields.add(
                    ImmutableField(
                        classDef.type,
                        StartupHooks.GUARD_FIELD,
                        "Z",
                        AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
                        null,
                        emptySet(),
                        emptySet(),
                    ).toMutable(),
                )
            }
            appType = classDef.type
        }
        val guardOwner = appType ?: run {
            logger.warning("No Application class found. No changes applied.")
            return@execute
        }
        val guard = "$guardOwner->${StartupHooks.GUARD_FIELD}:Z"

        val cancelableFlag = cancelable ?: true

        var patchedActivities = 0
        classDefForEach { classDef ->
            // Heuristic: activities end in ...Activity; (covers framework and support classes).
            if (classDef.superclass?.endsWith("Activity;") != true) return@classDefForEach

            val mutableClass = mutableClassDefBy(classDef)
            val onCreate = mutableClass.methods.firstOrNull {
                it.name == "onCreate" && it.returnType == "V" &&
                    it.parameterTypes == listOf("Landroid/os/Bundle;")
            } ?: return@classDefForEach

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
    val tempBase = onCreate.implementation!!.registerCount
    val contextReg = onCreate.p0Register
    val cloned = onCreate.cloneMutable(additionalRegisters = 5)

    val b = tempBase
    val fullSmali = StringBuilder().apply {
        appendLine("sget-boolean v$b, $guard")
        appendLine("if-nez v$b, :nai64_skip")
        appendLine("const/4 v$b, 0x1")
        appendLine("sput-boolean v$b, $guard")
        appendLine("new-instance v${b + 1}, Landroid/app/AlertDialog\$Builder;")
        appendLine("invoke-direct {v${b + 1}, v$contextReg}, Landroid/app/AlertDialog\$Builder;-><init>(Landroid/content/Context;)V")
        appendLine("const-string v${b + 2}, \"${StartupHooks.escapeSmali(title)}\"")
        appendLine("invoke-virtual {v${b + 1}, v${b + 2}}, Landroid/app/AlertDialog\$Builder;->setTitle(Ljava/lang/CharSequence;)Landroid/app/AlertDialog\$Builder;")
        appendLine("const-string v${b + 2}, \"${StartupHooks.escapeSmali(message)}\"")
        appendLine("invoke-virtual {v${b + 1}, v${b + 2}}, Landroid/app/AlertDialog\$Builder;->setMessage(Ljava/lang/CharSequence;)Landroid/app/AlertDialog\$Builder;")
        if (!cancelable) {
            appendLine("const/4 v${b + 3}, 0x0")
            appendLine("invoke-virtual {v${b + 1}, v${b + 3}}, Landroid/app/AlertDialog\$Builder;->setCancelable(Z)Landroid/app/AlertDialog\$Builder;")
        }
        appendLine("invoke-virtual {v${b + 1}}, Landroid/app/AlertDialog\$Builder;->show()Landroid/app/AlertDialog;")
        append(":nai64_skip")
    }.toString()

    cloned.addInstructions(0, fullSmali)

    owner.methods.remove(onCreate)
    owner.methods.add(cloned)
}
