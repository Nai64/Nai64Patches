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
 * Finds the Application.onCreate()V method, or null.
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

        val (mutableClass, onCreate) = findApplicationOnCreate()
            ?: run {
                logger.warning("No Application.onCreate found. No changes applied.")
                return@execute
            }

        // Four fresh registers: text, duration, toast reference, spare.
        val tempBase = onCreate.implementation!!.registerCount
        val contextReg = onCreate.p0Register
        val cloned = onCreate.cloneMutable(additionalRegisters = 4)

        cloned.addInstructions(
            0,
            """
            const-string v${tempBase + 1}, "${StartupHooks.escapeSmali(text)}"
            const/4 v${tempBase + 2}, $durationFlag
            invoke-static {v$contextReg, v${tempBase + 1}, v${tempBase + 2}}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
            move-result-object v${tempBase + 3}
            invoke-virtual {v${tempBase + 3}}, Landroid/widget/Toast;->show()V
            """.trimIndent(),
        )

        mutableClass.methods.remove(onCreate)
        mutableClass.methods.add(cloned)

        logger.info("Startup toast injected into ${mutableClass.type}->onCreate")
    }
}
