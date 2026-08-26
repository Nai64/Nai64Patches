package util

import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.loadPatchesFromJar
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import java.io.File
import java.net.URLClassLoader
import java.util.jar.Manifest

fun main() {
    val patchFiles = setOf(
        File("build/libs/").listFiles { file ->
            val fileName = file.name
            !fileName.contains("javadoc") &&
                    !fileName.contains("sources") &&
                    fileName.endsWith(".mpp")
        }?.maxByOrNull { it.lastModified() }
            ?: error("No .mpp artifact found in build/libs/")
    )
    val loadedPatches = loadPatchesFromJar(patchFiles)
    val patchClassLoader = URLClassLoader(patchFiles.map { it.toURI().toURL() }.toTypedArray())
    val manifest = patchClassLoader.getResources("META-INF/MANIFEST.MF")

    while (manifest.hasMoreElements()) {
        Manifest(manifest.nextElement().openStream())
            .mainAttributes
            .getValue("Version")
            ?.let {
                generatePatchList(it, loadedPatches)
            }
    }
}

@Suppress("DEPRECATION")
private fun generatePatchList(version: String, patches: Set<Patch<*>>) {
    val listJson = File("../patches-list.json")

    val patchOrder = listOf(
        "Ads Free Rewards (Experimental)",
        "No Ads (Experimental)",
        "Skip Consent Popup",
        "Skip Rate-Us Prompt",
        "Spoof Play Store Install Source",
        "Spoof Amazon Appstore Availability",
        "Limit Ad Tracking",
        "Remove Permissions",
        "Remove Hardware Requirements",
        "Unlock Rotation",
        "Force Landscape / Portrait",
        "Make App Debuggable",
        "Change Version Code",
        "Change Version Name",
        "Rename App",
        "Remove App Icon",
        "Allow Cleartext Traffic",
        "Keep Screen On",
        "Remove Backup Restrictions",
        "Export All Activities",
        "Repair Missing Component Export Flags",
        "Disable Battery Optimization Prompt",
        "Remove Network Security Config",
        "Enable Large Heap",
        "Force Hardware Acceleration",
        "Prefer ANGLE Graphics Driver",
        "Graphics API Override (Experimental)",
        "Frame Rate Preference (Experimental)",
        "Legacy External Storage",
        "Ensure Storage Permissions",
        "Ensure Modern Media Permissions",
        "Restore Package Visibility",
        "Keep Data on Uninstall",
        "Set Install Location",
        "Force Resizable Activity",
        "Immersive Fullscreen",
        "Strip Translations",
        "Disable Vibration",
        "Disable Crash Reporters",
        "Disable App Links Verification",
        "Support All Screens",
        "Remove Ad Services Entries",
        "Disable Permission Auto-Revoke",
        "Ensure Internet Permission",
        "Ensure Notification Permission",
        "Status Bar Icons",
        "Fake Online State",
        "Disable Forced Online Checks (Experimental)",
        "Set Profileable",
        "Force Extract Native Libs",
        "Unlock Max Aspect Ratio",
        "Bypass VPN Detection",
        "Spoof Device Model",
        "Bypass OkHttp Pinning",
        "Trust User Certificates",
        "Spoof Build Fingerprint",
        "Optimize as Game",
        "Relax Shared Libraries",
        "Transparent System Bars",
        "Custom Startup Toast",
        "Custom Startup Dialog",
        "Enable WebView Debugging",
        "Fake Battery Whitelist",
        "Spoof Signature Match",
        "Treat Network as Unmetered",
        "Grant All-Files Access",
        "Ensure Exact Alarm Availability",
        "Hide Roaming Status",
        "Ignore Power Save Mode",
        "Spoof Notifications as Enabled",
        "Ignore Screen Lock",
        "Ignore GPS Disabled",
        "Hide Mock Location",
        "Spoof Play Integrity",
        "Bypass License Verification",
        "Bypass Google Play Services Check",
        "Pairip Bypass (Experimental)",
        "Unlock RevenueCat Entitlements",
        "Unlock All IAPs (Unity IAP)",
        "Custom App Resolution (Experimental)",
        "Allow Screenshots",
        "Skip Splash Screen (Experimental)",
        "Disable Root Checks",
        "Disable Google Update Check",
        "Bypass Forced Updates (Experimental)",
        "Disable Telemetry",
    )
    val orderMap = patchOrder.withIndex().associate { (i, name) -> name to i }

    // Null-named patches are implementation helpers used by a visible patch.
    // Keep them available to Kotlin dependencies without exposing them as
    // standalone entries in Morphe Manager.
    val patchesMap = patches
        .filter { it.name != null }
        .sortedWith(compareBy({ orderMap[it.name] ?: Int.MAX_VALUE }, { it.name ?: "" }))
        .map { patch ->
        JsonPatch(
            name = patch.name!!,
            description = patch.description,
            default = patch.default,
            dependencies = patch.dependencies.map { it.javaClass.simpleName },
            compatiblePackages = patch.compatibility?.map { compat ->
                JsonCompatibility(
                    packageName = compat.packageName!!,
                    name = compat.name,
                    description = compat.description,
                    apkFileType = compat.apkFileType?.name,
                    appIconColor = compat.appIconColor?.let { "#%06X".format(it) },
                    signatures = compat.signatures,
                    targets = compat.targets.map { target ->
                        JsonCompatibility.Target(
                            version = target.version,
                            versionCodes = target.versionCodes?.mapKeys { it.key.name },
                            isExperimental = target.isExperimental,
                            minSdk = target.minSdk,
                            description = target.description,
                        )
                    },
                )
            },
            options = patch.options.values.map { option ->
                JsonPatch.Option(
                    key = option.key,
                    title = option.title,
                    description = option.description,
                    required = option.required,
                    type = option.type.toString(),
                    default = option.default,
                    values = option.values,
                )
            }
        )
    }

    val gson = GsonBuilder()
        .serializeNulls()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create()

    val jsonObject = JsonObject()
    jsonObject.addProperty(
        "NOTE",
        "Do NOT manually edit this file. This file is automatically updated when " +
                "semantic release (release.yml) runs. Manually editing this file can break " +
                "your releases and break third party tools that use this file."
    )
    jsonObject.addProperty("version", version)
    jsonObject.add("patches", gson.toJsonTree(patchesMap))

    listJson.writeText(gson.toJson(jsonObject))
}

@Suppress("unused")
private class JsonPatch(
    val name: String? = null,
    val description: String? = null,
    val default: Boolean = true,
    val dependencies: List<String>,
    val compatiblePackages: List<JsonCompatibility>? = null,
    val options: List<Option>,
) {
    class Option(
        val key: String,
        val title: String?,
        val description: String?,
        val required: Boolean,
        val type: String,
        val default: Any?,
        val values: Map<String, Any?>?,
    )
}

@Suppress("unused")
private class JsonCompatibility(
    val packageName: String,
    val name: String?,
    val description: String?,
    val apkFileType: String?,
    val appIconColor: String?,
    val signatures: Set<String>?,
    val targets: List<Target>,
) {
    class Target(
        val version: String?,
        val versionCodes: Map<String, Int>?,
        val isExperimental: Boolean,
        val minSdk: Int?,
        val description: String?,
    )
}
