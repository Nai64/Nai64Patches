package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.io.File
import java.util.logging.Logger

private const val NSC_NAME = "nai64_trust_user_cas"

private val NSC_XML = """
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config>
        <trust-anchors>
            <certificates src="system" />
            <certificates src="user" />
        </trust-anchors>
    </base-config>
</network-security-config>
""".trimIndent() + "\n"

@Suppress("unused")
val trustUserCasConfigPatch = resourcePatch(
    name = "Trust User CAs (Config)",
    description = "Injects a network security config that trusts user-installed CA certificates, enabling MITM/proxy interception for the app.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val xmlDir = get("res/xml", true)
        xmlDir.mkdirs()
        File(xmlDir, "$NSC_NAME.xml").writeText(NSC_XML)

        var applied = false
        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.documentElement.applicationOrNull()
            if (application != null) {
                application.setAttributeNS(NS_ANDROID, "android:networkSecurityConfig", "@xml/$NSC_NAME")
                applied = true
            }
        }

        if (applied) {
            logger.info("Pointed app at user-CA-trusting network security config")
        } else {
            logger.warning("No <application> element found. No changes applied.")
        }
    }
}
