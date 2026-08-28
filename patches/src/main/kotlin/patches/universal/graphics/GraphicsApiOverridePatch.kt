package patches.universal.graphics

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger
import patches.universal.ads.util.cloneMutableAndPreserveParameters

@Suppress("unused")
val graphicsApiOverridePatch = bytecodePatch(
    name = "Graphics API Override (Experimental)",
    description = "Forces a Unity game to use Vulkan or OpenGL via launch argument. Only for supported Unity games.",
    default = false,
) {
    val graphicsApi by stringOption(
        key = "graphicsApi",
        title = "Graphics API",
        description = "Renderer to request from supported Unity Android games",
        default = "opengl",
        values = linkedMapOf(
            "Vulkan" to "vulkan",
            "OpenGL ES" to "opengl",
        ),
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val argument = when (graphicsApi) {
            "vulkan" -> "-force-vulkan"
            "opengl" -> "-force-gles20"
            else -> {
                logger.warning("Invalid graphics API \"$graphicsApi\". No changes applied.")
                return@execute
            }
        }

        val matchedMethod = UnityPlayerActivityOnCreateFingerprint.methodOrNull
        val mutableClass = UnityPlayerActivityOnCreateFingerprint.classDefOrNull?.let(::mutableClassDefBy)
        if (matchedMethod == null || mutableClass == null) {
            logger.warning("No supported Unity activity found. No changes applied.")
            return@execute
        }

        // v0..v2 may overlap p0/p1 in a small onCreate method. Add registers and
        // preserve the original parameters before using them as temporaries.
        val method = matchedMethod.cloneMutableAndPreserveParameters(mutableClass)
        if ((method.implementation?.registerCount ?: 0) < 3) {
            logger.warning("Unity activity could not provide 3 temporary registers. No changes applied.")
            return@execute
        }

        // Unity Android reads command-line arguments from the "unity" intent extra.
        // Keep the original intent and append a renderer argument before Unity starts.
        method.addInstructions(
            0,
            """
            invoke-virtual {p0}, Landroid/app/Activity;->getIntent()Landroid/content/Intent;
            move-result-object v0
            const-string v1, "unity"
            const-string v2, "$argument"
            invoke-virtual {v0, v1, v2}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;
            move-result-object v0
            """.trimIndent(),
        )
        logger.info("Requested $graphicsApi graphics API through Unity launch arguments")
    }
}
