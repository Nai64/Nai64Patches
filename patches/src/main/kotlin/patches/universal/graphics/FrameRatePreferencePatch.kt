package patches.universal.graphics

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger
import patches.universal.ads.util.cloneMutableAndPreserveParameters

private val frameRateBits = mapOf(
    "24" to "0x41c00000",
    "30" to "0x41f00000",
    "40" to "0x42200000",
    "45" to "0x42340000",
    "48" to "0x42400000",
    "50" to "0x42480000",
    "60" to "0x42700000",
    "72" to "0x42900000",
    "90" to "0x42b40000",
    "96" to "0x42c00000",
    "120" to "0x42f00000",
    "144" to "0x43100000",
    "165" to "0x43250000",
    "180" to "0x43340000",
    "185" to "0x43390000",
)

@Suppress("unused")
val frameRatePreferencePatch = bytecodePatch(
    name = "Frame Rate Preference (Experimental)",
    description = "Requests a preferred refresh rate like 60 or 90 Hz for the app window. The system may ignore it.",
    default = false,
) {
    val frameRate by stringOption(
        key = "frameRate",
        title = "Frame rate",
        description = "Preferred display refresh rate for supported app windows",
        default = "60",
        values = linkedMapOf(
            "24 FPS" to "24",
            "30 FPS" to "30",
            "40 FPS" to "40",
            "45 FPS" to "45",
            "48 FPS" to "48",
            "50 FPS" to "50",
            "60 FPS" to "60",
            "72 FPS" to "72",
            "90 FPS" to "90",
            "96 FPS" to "96",
            "120 FPS" to "120",
            "144 FPS" to "144",
            "165 FPS" to "165",
            "180 FPS" to "180",
            "185 FPS" to "185",
        ),
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val selected = frameRate.orEmpty()
        val bits = frameRateBits[selected]
        if (bits == null) {
            logger.warning("Invalid frame rate \"$selected\". No changes applied.")
            return@execute
        }

        val superMap = mutableMapOf<String, String>()
        classDefForEach { classDef -> classDef.superclass?.let { superMap[classDef.type] = it } }
        fun isActivity(type: String, seen: MutableSet<String> = mutableSetOf()): Boolean {
            if (type == "Landroid/app/Activity;") return true
            if (type == "Ljava/lang/Object;" || type in seen) return false
            seen.add(type)
            return superMap[type]?.let { isActivity(it, seen) } == true
        }

        var patched = 0
        classDefForEach { classDef ->
            if (!isActivity(classDef.type)) return@classDefForEach
            val mutableClass = mutableClassDefBy(classDef)
            val methods = mutableClass.methods.toList()
            for (matchedMethod in methods) {
                if (matchedMethod.name != "onCreate" ||
                    matchedMethod.returnType != "V" ||
                    matchedMethod.parameterTypes != listOf("Landroid/os/Bundle;") ||
                    matchedMethod.implementation == null
                ) {
                    continue
                }

                // Add temporary registers while preserving p0 (the Activity) and p1 (Bundle).
                val method = matchedMethod.cloneMutableAndPreserveParameters(mutableClass)
                method.addInstructions(
                    0,
                    """
                    sget v0, Landroid/os/Build${'$'}VERSION;->SDK_INT:I
                    const/16 v1, 0x17
                    if-lt v0, v1, :morphe_frame_rate_done
                    invoke-virtual {p0}, Landroid/app/Activity;->getWindow()Landroid/view/Window;
                    move-result-object v0
                    invoke-virtual {v0}, Landroid/view/Window;->getAttributes()Landroid/view/WindowManager${'$'}LayoutParams;
                    move-result-object v1
                    const/high16 v2, $bits
                    iput v2, v1, Landroid/view/WindowManager${'$'}LayoutParams;->preferredRefreshRate:F
                    invoke-virtual {v0, v1}, Landroid/view/Window;->setAttributes(Landroid/view/WindowManager${'$'}LayoutParams;)V
                    :morphe_frame_rate_done
                    """.trimIndent(),
                )
                patched++
            }
        }

        if (patched > 0) {
            logger.info("Requested $selected FPS for $patched Activity window(s)")
        } else {
            logger.warning("No Activity onCreate(Bundle) methods found. No changes applied.")
        }
    }
}
