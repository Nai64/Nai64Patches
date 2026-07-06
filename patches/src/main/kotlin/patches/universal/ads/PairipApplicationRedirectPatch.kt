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
    description = "Redirects Pairip's Application entry point to the real app class. Auto-discovers from DEX.",
    default = false,
) {
    val realAppClass by option(
        title = "Real Application Class",
        default = "",
        key = "realAppClass",
        description = "The real app's Application class. Leave empty to auto-discover.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val real = realAppClass?.takeIf { it.isNotEmpty() } ?: discoverPairipAppClass(logger) ?: run {
            logger.warning("Could not discover real app class. Set 'Real Application Class' option manually.")
            return@execute
        }

        document("AndroidManifest.xml").use { doc ->
            val app = doc.getElementsByTagName("application").item(0) as? Element ?: run {
                logger.warning("No <application> element found")
                return@execute
            }
            val ns = "http://schemas.android.com/apk/res/android"
            val cur = app.getAttributeNS(ns, "name").let { if (!it.isNullOrEmpty()) it else app.getAttribute("android:name") }
            if (cur != "com.pairip.application.Application") {
                logger.info("Application class is '$cur' — not Pairip, skipping")
                return@execute
            }
            app.setAttributeNS(ns, "android:name", real)
            logger.info("Redirected Pairip -> $real")
        }
    }
}

private fun ResourcePatchContext.discoverPairipAppClass(logger: Logger): String? {
    val dir = try { get("AndroidManifest.xml", false)?.parentFile } catch (_: Exception) { null }
        ?: return null.also { logger.warning("Cannot determine APK directory") }
    for (i in 0..99) {
        val f = java.io.File(dir, if (i == 0) "classes.dex" else "classes${i + 1}.dex")
        if (!f.exists()) break
        try {
            for (cls in DexFileFactory.loadDexFile(f, Opcodes.getDefault()).classes) {
                if (cls.type != "Lcom/pairip/application/Application;") continue
                val sup = cls.superclass ?: continue
                if (sup == "Ljava/lang/Object;" || sup == "Landroid/app/Application;") continue
                return sup.substringAfter("L").substringBefore(";").replace('/', '.').also {
                    logger.info("Discovered real app class from ${f.name}: $it")
                }
            }
        } catch (e: Exception) {
            logger.warning("Failed to parse ${f.name}: ${e.message}")
        }
    }
    return null
}
