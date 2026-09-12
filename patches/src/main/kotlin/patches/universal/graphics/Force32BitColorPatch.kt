package patches.universal.graphics

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import patches.universal.ads.util.cloneParameters
import java.util.logging.Logger

@Suppress("unused")
val force32BitColorPatch = bytecodePatch(
    name = "Force 32-Bit Color",
    description = "Requests RGBA_8888 window format to reduce color banding.",
    default = false,
) {
    category("Graphics")
    execute {
        val logger = Logger.getLogger(this::class.java.name)

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
            for (matchedMethod in classDef.methods.toList()) {
                if (matchedMethod.name != "onCreate" ||
                    matchedMethod.returnType != "V" ||
                    matchedMethod.parameterTypes != listOf("Landroid/os/Bundle;") ||
                    matchedMethod.implementation == null
                ) {
                    continue
                }

                matchedMethod.cloneParameters().addInstructions(
                    0,
                    """
                    invoke-virtual {p0}, Landroid/app/Activity;->getWindow()Landroid/view/Window;
                    move-result-object v0
                    const/4 v1, 0x1
                    invoke-virtual {v0, v1}, Landroid/view/Window;->setFormat(I)V
                    """.trimIndent(),
                )
                patched++
            }
        }

        if (patched > 0) logger.info("Requested 32-bit color for $patched Activity window(s)")
        else logger.warning("No Activity onCreate(Bundle) methods found. No changes applied.")
    }
}
