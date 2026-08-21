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
        "Ads Free Rewards",
        "No Ads",
        "Skip Consent Popup",
        "Skip Rate-Us Prompt",
        "Spoof Play Store Install Source",
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
        "Disable Battery Optimization Prompt",
        "Remove Network Security Config",
        "Enable Large Heap",
        "Force Hardware Acceleration",
        "Legacy External Storage",
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
        "Status Bar Icons",
        "Fake Online State",
        "Set Profileable",
        "Force Extract Native Libs",
        "Unlock Max Aspect Ratio",
        "Bypass VPN Detection",
        "Spoof Device Model",
        "Bypass OkHttp Pinning",
        "Trust User Certificates",
        "Spoof Build Fingerprint",
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
        "Disable Update Checks",
        "Disable Telemetry",
    )
    val orderMap = patchOrder.withIndex().associate { (i, name) -> name to i }

    val patchesMap = patches.sortedWith(compareBy({ orderMap[it.name] ?: Int.MAX_VALUE }, { it.name ?: "" })).map { patch ->
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
