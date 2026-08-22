package patches.universal.ui

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import patches.universal.ads.util.cloneMutable
import patches.universal.ads.util.p0Register
import java.util.logging.Logger

/**
 * Finds the first Application.onCreate()V method, or null. Used as a fallback
 * when the manifest-declared Application class cannot be resolved.
 * The returned pair is (mutable class, method).
 */
internal fun BytecodePatchContext.findApplicationOnCreate(): Pair<MutableClass, MutableMethod>? {
    var result: Pair<MutableClass, MutableMethod>? = null
    classDefForEach { classDef ->
        if (classDef.superclass != "Landroid/app/Application;") return@classDefForEach
        val mutableClass = mutableClassDefBy(classDef)
        val onCreate = mutableClass.methods.firstOrNull {
            it.name == "onCreate" && it.returnType == "V" && it.parameterTypes.isEmpty()
        } ?: return@classDefForEach
        // Keep the first hit; later Application subclasses are ignored.
        if (result == null) result = mutableClass to onCreate
    }
    return result
}

@Suppress("unused")
val customStartupToastPatch = bytecodePatch(
    name = "Custom Startup Toast",
    description = "Shows a customizable toast message every time the app starts",
    default = false,
) {
    dependsOn(StartupHooks.resolveRealApplicationPatch)

    val message by stringOption(
        title = "Toast message",
        default = "Patched by Nai's Patches",
        key = "startupToastMessage",
        description = "Text shown in the toast on every app start.",
    )
    val duration by stringOption(
        title = "Duration",
        default = "long",
        values = linkedMapOf("Short" to "short", "Long" to "long"),
        key = "startupToastDuration",
        description = "How long the toast stays visible.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val text = message.orEmpty().trim()
        if (text.isEmpty()) {
            logger.warning("No message configured. No changes applied.")
            return@execute
        }
        val durationFlag = when (duration.orEmpty().trim().lowercase()) {
            "short" -> "0x0"
            "long" -> "0x1"
            else -> {
                logger.warning("Invalid duration \"$duration\". No changes applied.")
                return@execute
            }
        }

        val (mutableClass, onCreate) = run {
            val descriptor = StartupHooks.resolvedApplicationDescriptor
            if (descriptor != null) {
                val cls = mutableClassDefByOrNull(descriptor)
                val om = cls?.methods?.firstOrNull {
                    it.name == "onCreate" && it.returnType == "V" && it.parameterTypes.isEmpty()
                }
                if (cls != null && om != null) {
                    return@run cls to om
                }
            }
            findApplicationOnCreate()
        } ?: run {
            logger.warning("No Application.onCreate found. No changes applied.")
            return@execute
        }

        // Three fresh registers: context copy (later reused for the toast
        // reference), text, duration. The context slot is overwritten with the
        // makeText result because its value is dead once makeText consumed it;
        // writing a fourth register would collide with p0, which cloneMutable
        // parks at the very top of the new register window.
        // All multi-register invokes use the /range variant because plain
        // invoke-* cannot address registers above v15, and Application.onCreate
        // may well place these temporaries beyond that. The context is copied
        // first to keep every operand list contiguous.
        val tempBase = onCreate.implementation!!.registerCount

        // cloneMutable shifts parameters to the top of the new register
        // window; p0 must be resolved against the CLONED method.
        val cloned = onCreate.cloneMutable(additionalRegisters = 4)
        val contextReg = cloned.p0Register

        cloned.addInstructions(
            0,
            """
            move-object/from16 v$tempBase, v$contextReg
            const-string v${tempBase + 1}, "${StartupHooks.escapeSmali(text)}"
            const/16 v${tempBase + 2}, $durationFlag
            invoke-static/range {v$tempBase .. v${tempBase + 2}}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
            move-result-object v$tempBase
            invoke-virtual/range {v$tempBase}, Landroid/widget/Toast;->show()V
            """.trimIndent(),
        )

        mutableClass.methods.remove(onCreate)
        mutableClass.methods.add(cloned)

        logger.info("Startup toast injected into ${mutableClass.type}->onCreate")
    }
}
