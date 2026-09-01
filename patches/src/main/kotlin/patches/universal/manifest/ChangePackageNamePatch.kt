package patches.universal.manifest

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger

@Suppress("unused")
val changePackageNamePatch = resourcePatch(
    name = "Change Package Name",
    description = "Clone the app with a new package name.",
    default = false,
) {
    val customPackageName by stringOption(
        key = "customPackageName",
        default = "",
        title = "Custom package name",
        description = "Fully custom package name (e.g. com.example.clone). Leave empty to use suffix mode below.",
    )
    val packageSuffix by stringOption(
        key = "packageSuffix",
        default = ".morphe",
        title = "Package suffix",
        description = "Suffix appended to the original package when Custom package name is empty (e.g. .morphe, .clone). Must start with a dot.",
    )
    val updateProviders by booleanOption(
        key = "updateProviders",
        default = true,
        title = "Update providers",
        description = "Rewrite provider android:authorities that start with the old package to the new package to avoid install conflicts.",
    )
    val updatePermissions by booleanOption(
        key = "updatePermissions",
        default = true,
        title = "Update permissions",
        description = "Rewrite custom permission android:name that starts with the old package (e.g. C2D_MESSAGE) to the new package.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val suffix = packageSuffix.orEmpty().trim()
        val custom = customPackageName.orEmpty().trim()

        document("AndroidManifest.xml").use { manifest ->
            val manifestEl = manifest.documentElement
            val original = manifestEl.getAttribute("package")?.trim().orEmpty()
            if (original.isEmpty()) {
                logger.warning("No manifest package found. No changes applied.")
                return@use
            }

            val newPackage = when {
                custom.isNotEmpty() -> custom
                suffix.isNotEmpty() -> {
                    val s = if (suffix.startsWith(".")) suffix else ".$suffix"
                    "$original$s"
                }
                else -> {
                    logger.warning("No custom package name or suffix configured. No changes applied.")
                    return@use
                }
            }

            // Basic validation  -  package must be lowercase, dot-separated
            if (!newPackage.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$"))) {
                logger.warning("New package name \"$newPackage\" looks invalid  -  proceeding anyway")
            }

            if (newPackage == original) {
                logger.warning("New package equals original ($original). No changes applied.")
                return@use
            }

            manifestEl.setAttribute("package", newPackage)
            logger.info("Package renamed: $original -> $newPackage")

            var providersPatched = 0
            if (updateProviders == true) {
                val providers = manifest.getElementsByTagName("provider")
                for (i in 0 until providers.length) {
                    val el = providers.item(i) as? org.w3c.dom.Element ?: continue
                    val auth = el.getAttributeNS(NS_ANDROID, "authorities").takeIf { it.isNotEmpty() }
                        ?: el.getAttribute("android:authorities").takeIf { it.isNotEmpty() } ?: continue
                    // Authorities may be list separated by ';'
                    val updated = auth.split(";").joinToString(";") { part ->
                        val p = part.trim()
                        when {
                            p == original -> newPackage
                            p.startsWith("$original.") -> newPackage + p.removePrefix(original)
                            p.startsWith(original) -> p.replaceFirst(original, newPackage)
                            else -> p
                        }
                    }
                    if (updated != auth) {
                        el.setAttributeNS(NS_ANDROID, "android:authorities", updated)
                        providersPatched++
                    }
                }
                if (providersPatched > 0) logger.info("Updated $providersPatched provider authorities")
            }

            var permsPatched = 0
            if (updatePermissions == true) {
                // <permission android:name="com.example.permission.C2D_MESSAGE">
                val perms = manifest.getElementsByTagName("permission")
                for (i in 0 until perms.length) {
                    val el = perms.item(i) as? org.w3c.dom.Element ?: continue
                    val name = el.getAttributeNS(NS_ANDROID, "name").takeIf { it.isNotEmpty() }
                        ?: el.getAttribute("android:name").takeIf { it.isNotEmpty() } ?: continue
                    if (name == original || name.startsWith("$original.")) {
                        val updated = if (name == original) newPackage else newPackage + name.removePrefix(original)
                        el.setAttributeNS(NS_ANDROID, "android:name", updated)
                        permsPatched++
                    } else if (name.startsWith(original)) {
                        // Fallback for odd cases without dot
                        val updated = name.replaceFirst(original, newPackage)
                        el.setAttributeNS(NS_ANDROID, "android:name", updated)
                        permsPatched++
                    }
                }
                // Also fix <uses-permission> that references the same custom permission
                val usesPerms = manifest.getElementsByTagName("uses-permission")
                for (i in 0 until usesPerms.length) {
                    val el = usesPerms.item(i) as? org.w3c.dom.Element ?: continue
                    val name = el.getAttributeNS(NS_ANDROID, "name").takeIf { it.isNotEmpty() }
                        ?: el.getAttribute("android:name").takeIf { it.isNotEmpty() } ?: continue
                    if (name == original || name.startsWith("$original.")) {
                        val updated = if (name == original) newPackage else newPackage + name.removePrefix(original)
                        el.setAttributeNS(NS_ANDROID, "android:name", updated)
                        permsPatched++
                    }
                }
                // <uses-permission-sdk-23> etc.
                val usesPermSdk = manifest.getElementsByTagName("uses-permission-sdk-23")
                for (i in 0 until usesPermSdk.length) {
                    val el = usesPermSdk.item(i) as? org.w3c.dom.Element ?: continue
                    val name = el.getAttributeNS(NS_ANDROID, "name").takeIf { it.isNotEmpty() } ?: continue
                    if (name.startsWith(original)) {
                        val updated = name.replaceFirst(original, newPackage)
                        el.setAttributeNS(NS_ANDROID, "android:name", updated)
                        permsPatched++
                    }
                }
                if (permsPatched > 0) logger.info("Updated $permsPatched permission entries")
            }

            logger.info("Change Package Name finished: $original -> $newPackage (providers=$providersPatched, perms=$permsPatched)")
        }
    }
}
