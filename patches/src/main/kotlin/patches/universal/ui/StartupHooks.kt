package patches.universal.ui

import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element
import patches.universal.manifest.NS_ANDROID
import patches.universal.manifest.applicationOrNull

/**
 * Shared plumbing for the startup toast/dialog patches.
 *
 * Injections are written into cloned methods that received extra registers
 * ([patches.universal.ads.util.cloneMutable]), so every temporary register is
 * guaranteed valid regardless of the original register layout. The context
 * register (`p0`) keeps its original absolute number and stays untouched.
 */
internal object StartupHooks {
    /** Name of the static boolean guard field added to the Application class. */
    const val GUARD_FIELD = "nai64StartupShown"

    /**
     * Real Application class descriptor resolved from the manifest's
     * `android:name` (e.g. `Lcom/peacock/flashlight/App;`), or `null` when the
     * manifest could not be read. `BytecodePatchContext` has no manifest access
     * of its own, so the [resolveRealApplicationPatch] resource patch discovers
     * it and publishes it here for the bytecode patches to consume.
     */
    var resolvedApplicationDescriptor: String? = null

    /**
     * Launcher activity descriptor resolved from the manifest's MAIN/LAUNCHER
     * intent filter (e.g. `Lcom/peacock/flashlight/pages/splash/SplashActivity;`).
     * When set and the app has more than one activity, the dialog patch skips
     * injecting into the launcher so the dialog does not flash on a transient
     * splash screen that immediately transitions to the real main activity.
     */
    var resolvedLauncherActivityDescriptor: String? = null

    /** Reads the Application and launcher classes declared in the manifest. */
    val resolveRealApplicationPatch = resourcePatch(
        name = "Resolve Real Application (internal)",
        default = false,
    ) {
        execute {
            try {
                document("AndroidManifest.xml").use { doc ->
                    val application = doc.documentElement.applicationOrNull()
                    val appName = application?.getAttribute("android:name")
                        ?: application?.getAttributeNS(NS_ANDROID, "name")
                    resolvedApplicationDescriptor = if (!appName.isNullOrEmpty()) {
                        "L" + appName.replace('.', '/') + ";"
                    } else {
                        null
                    }

                    resolvedLauncherActivityDescriptor = findLauncherActivity(doc.documentElement)
                }
            } catch (_: Exception) {
                resolvedApplicationDescriptor = null
                resolvedLauncherActivityDescriptor = null
            }
        }
    }

    /** Returns the descriptor of the activity with a MAIN/LAUNCHER filter, or null. */
    private fun findLauncherActivity(root: Element): String? {
        val packageName = root.getAttribute("package")
        val activities = root.getElementsByTagName("activity")
        for (i in 0 until activities.length) {
            val activity = activities.item(i) as? Element ?: continue
            var hasMain = false
            var hasLauncher = false
            val filters = activity.getElementsByTagName("intent-filter")
            for (j in 0 until filters.length) {
                val filter = filters.item(j) as? Element ?: continue
                val actions = filter.getElementsByTagName("action")
                for (k in 0 until actions.length) {
                    val action = actions.item(k) as? Element ?: continue
                    if (action.getAttribute("android:name") == "android.intent.action.MAIN") {
                        hasMain = true
                    }
                }
                val categories = filter.getElementsByTagName("category")
                for (k in 0 until categories.length) {
                    val category = categories.item(k) as? Element ?: continue
                    if (category.getAttribute("android:name") == "android.intent.category.LAUNCHER") {
                        hasLauncher = true
                    }
                }
                if (hasMain && hasLauncher) break
            }
            if (hasMain && hasLauncher) {
                val name = activity.getAttribute("android:name")
                if (!name.isNullOrEmpty()) return componentDescriptor(name, packageName)
            }
        }
        val aliases = root.getElementsByTagName("activity-alias")
        for (i in 0 until aliases.length) {
            val alias = aliases.item(i) as? Element ?: continue
            var hasMain = false
            var hasLauncher = false
            val filters = alias.getElementsByTagName("intent-filter")
            for (j in 0 until filters.length) {
                val filter = filters.item(j) as? Element ?: continue
                val actions = filter.getElementsByTagName("action")
                for (k in 0 until actions.length) {
                    val action = actions.item(k) as? Element ?: continue
                    if (action.getAttribute("android:name") == "android.intent.action.MAIN") hasMain = true
                }
                val categories = filter.getElementsByTagName("category")
                for (k in 0 until categories.length) {
                    val category = categories.item(k) as? Element ?: continue
                    if (category.getAttribute("android:name") == "android.intent.category.LAUNCHER") hasLauncher = true
                }
            }
            if (hasMain && hasLauncher) {
                val target = alias.getAttribute("android:targetActivity")
                if (!target.isNullOrEmpty()) return componentDescriptor(target, packageName)
            }
        }
        return null
    }

    private fun componentDescriptor(name: String, packageName: String): String {
        val qualifiedName = when {
            name.startsWith(".") -> packageName + name
            '.' !in name -> "$packageName.$name"
            else -> name
        }
        return "L" + qualifiedName.replace('.', '/') + ";"
    }

    fun escapeSmali(value: String): String =
        value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r\n", "\\n")
            .replace("\n", "\\n")
            .replace("\r", "\\n")
            .replace("\t", "\\t")
}
