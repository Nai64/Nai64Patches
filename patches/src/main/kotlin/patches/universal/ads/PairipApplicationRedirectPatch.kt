package patches.universal.ads

import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.patcher.patch.option
import app.morphe.patcher.patch.resourcePatch
import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import java.util.logging.Logger
import org.w3c.dom.Element

@Suppress("unused")
val pairipApplicationRedirectPatch = resourcePatch(
    name = "Pairip Application Redirect",
    description = "Changes AndroidManifest.xml to redirect Pairip's Application entry point " +
            "to the real app's Application class, preventing Pairip from ever starting. " +
            "Auto-discovers the real app class from Pairip's DEX if not specified manually.",
    default = false,
) {
    val realAppClass by option(
        title = "Real Application Class",
        default = "",
        key = "realAppClass",
        description = "The real app's Application class name (e.g., com.example.app.App). " +
                "Leave empty to auto-discover from Pairip's DEX.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val targetClass = realAppClass?.takeIf { it.isNotEmpty() } ?: discoverPairipAppClass(logger)

        document("AndroidManifest.xml").use { doc ->
            val appElement = doc.getElementsByTagName("application").item(0)
            if (appElement !is Element) {
                logger.warning("No <application> element found in AndroidManifest.xml")
                return@execute
            }

            val ns = "http://schemas.android.com/apk/res/android"
            val currentName = appElement.getAttributeNS(ns, "name").let { nsName ->
                if (!nsName.isNullOrEmpty()) nsName else appElement.getAttribute("android:name")
            }

            if (currentName != "com.pairip.application.Application") {
                logger.info("Application class is '$currentName', not Pairip — skipping redirect")
                return@execute
            }

            if (targetClass == null) {
                logger.warning("Could not auto-discover real app class. Set the 'Real Application Class' option. " +
                        "To find it: check smali for .super directive in com/pairip/application/Application.smali")
                return@execute
            }

            appElement.setAttributeNS(ns, "android:name", targetClass)
            logger.info("Redirected Pairip Application entry point -> $targetClass")
        }
    }
}

private fun ResourcePatchContext.discoverPairipAppClass(logger: Logger): String? {
    for (i in 0..99) {
        val name = if (i == 0) "classes.dex" else "classes${i + 1}.dex"
        val file = try {
            get(name, false)
        } catch (_: Exception) {
            null
        } ?: break

        try {
            val dex = DexFileFactory.loadDexFile(file, Opcodes.getDefault())
            for (classDef in dex.classes) {
                if (classDef.type != "Lcom/pairip/application/Application;") continue
                val superclass = classDef.superclass ?: continue
                if (superclass == "Ljava/lang/Object;" || superclass == "Landroid/app/Application;") continue
                val realClass = superclass.substringAfter("L").substringBefore(";").replace('/', '.')
                logger.info("Auto-discovered real app class from $name: $realClass")
                return realClass
            }
        } catch (e: Exception) {
            logger.warning("Failed to parse $name: ${e.message}")
        }
    }
    return null
}
