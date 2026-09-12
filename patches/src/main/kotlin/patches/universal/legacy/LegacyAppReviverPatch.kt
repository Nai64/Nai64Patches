package patches.universal.legacy

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger
import org.w3c.dom.Element
import patches.universal.manifest.NS_ANDROID
import patches.universal.manifest.applicationOrNull

private const val APACHE_LEGACY_LIB = "org.apache.http.legacy"

private const val FG_SERVICE = "android.permission.FOREGROUND_SERVICE"
private const val SCHEDULE_EXACT_ALARM = "android.permission.SCHEDULE_EXACT_ALARM"
private const val USE_EXACT_ALARM = "android.permission.USE_EXACT_ALARM"
private const val BT_CONNECT = "android.permission.BLUETOOTH_CONNECT"
private const val BT_SCAN = "android.permission.BLUETOOTH_SCAN"

private fun hasPermission(manifest: org.w3c.dom.Document, name: String): Boolean {
    val permissions = manifest.getElementsByTagName("uses-permission")
    for (i in 0 until permissions.length) {
        val item = permissions.item(i)?.attributes?.getNamedItem("android:name")?.nodeValue
        if (item == name) return true
    }
    return false
}

private fun addPermission(manifest: org.w3c.dom.Document, name: String): Boolean {
    if (hasPermission(manifest, name)) return false
    val root = manifest.documentElement ?: return false
    val permission = manifest.createElement("uses-permission")
    permission.setAttributeNS(NS_ANDROID, "android:name", name)
    root.appendChild(permission)
    return true
}

@Suppress("unused")
val legacyAppReviverPatch = resourcePatch(
    name = "Legacy App Reviver",
    description = "Revives Android 1.0-5.0 era apps on modern Android: restores the removed Apache HTTP stack, and declares the foreground-service, exact-alarm and Bluetooth permissions that old apps assume. Combine with SpoofTargetSdk (28), AllowCleartextTraffic, LegacyExternalStorage, SupportAllScreens, UnlockMaxAspectRatio, RelaxSharedLibraries, SpoofImei, Bypass Play License Check, Bypass Google Play Services Check, DisableHeapPointerTagging and VmSafeMode. Cannot fix armeabi-only native code on 64-bit-only devices or shut-down game servers.",
    default = false,
) {
    category("Enable")
    val apacheLegacy by booleanOption(
        title = "Apache HTTP legacy library",
        default = true,
        key = "apacheLegacy",
        description = "Adds org.apache.http.legacy as an optional shared library. Fixes instant crashes in pre-2015 apps using HttpClient or AndroidHttpClient.",
    )
    val foregroundService by booleanOption(
        title = "Foreground service permission",
        default = true,
        key = "foregroundService",
        description = "Declares FOREGROUND_SERVICE, required since Android 9 for apps playing music or tracking in the background.",
    )
    val exactAlarms by booleanOption(
        title = "Exact alarm permissions",
        default = true,
        key = "exactAlarms",
        description = "Declares SCHEDULE_EXACT_ALARM and USE_EXACT_ALARM for old reminder and alarm apps.",
    )
    val bluetooth by booleanOption(
        title = "Bluetooth permissions",
        default = true,
        key = "bluetooth",
        description = "Declares BLUETOOTH_CONNECT and BLUETOOTH_SCAN for old Bluetooth apps and companion-device flows.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var added = 0

        if (apacheLegacy == true) {
            document("AndroidManifest.xml").use { manifest ->
                val application = manifest.documentElement.applicationOrNull() ?: return@use
                val libs = application.getElementsByTagName("uses-library")
                var present = false
                for (i in 0 until libs.length) {
                    val el = libs.item(i) as? Element ?: continue
                    if (el.getAttributeNS(NS_ANDROID, "name") == APACHE_LEGACY_LIB) {
                        present = true
                        break
                    }
                }
                if (!present) {
                    val lib = manifest.createElement("uses-library")
                    lib.setAttributeNS(NS_ANDROID, "android:name", APACHE_LEGACY_LIB)
                    lib.setAttributeNS(NS_ANDROID, "android:required", "false")
                    application.appendChild(lib)
                    added++
                    logger.info("Legacy App Reviver: added $APACHE_LEGACY_LIB shared library")
                }
            }
        }

        document("AndroidManifest.xml").use { manifest ->
            if (foregroundService == true && addPermission(manifest, FG_SERVICE)) {
                added++
                logger.info("Legacy App Reviver: added $FG_SERVICE")
            }
            if (exactAlarms == true) {
                if (addPermission(manifest, SCHEDULE_EXACT_ALARM)) {
                    added++
                    logger.info("Legacy App Reviver: added $SCHEDULE_EXACT_ALARM")
                }
                if (addPermission(manifest, USE_EXACT_ALARM)) {
                    added++
                    logger.info("Legacy App Reviver: added $USE_EXACT_ALARM")
                }
            }
            if (bluetooth == true) {
                if (addPermission(manifest, BT_CONNECT)) {
                    added++
                    logger.info("Legacy App Reviver: added $BT_CONNECT")
                }
                if (addPermission(manifest, BT_SCAN)) {
                    added++
                    logger.info("Legacy App Reviver: added $BT_SCAN")
                }
            }
        }

        if (added == 0) logger.warning("Legacy App Reviver: everything already declared. No changes applied.")
        else logger.info("Legacy App Reviver: $added declaration(s) added in total")
    }
}
