package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofBuildExtrasPatch = bytecodePatch(
    name = null,
    description =
        "Spoofs the remaining android.os.Build identity fields not covered by Bypass " +
            "Emulator Detection (BOOTLOADER, RADIO, USER, HOST, getRadioVersion, and " +
            "Build.VERSION RELEASE/INCREMENTAL/SECURITY_PATCH/CODENAME) with values from a " +
            "real Pixel 6, so apps cannot fingerprint the device through these fields.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val buildFields = mapOf(
            "BOOTLOADER" to "redfin-1.3-89001234",
            "RADIO" to "g5123b-107525-220303",
            "USER" to "android-build",
            "HOST" to "abfarm-b.epsi.corp.google.com",
        )
        val versionFields = mapOf(
            "RELEASE" to "13",
            "INCREMENTAL" to "TQ3A.230805.001",
            "SECURITY_PATCH" to "2023-08-05",
            "CODENAME" to "REL",
            "BASE_OS" to "",
        )

        val patchedBuild = foldStaticStringField("Landroid/os/Build;", buildFields)
        val patchedVersion = foldStaticStringField("Landroid/os/Build\$VERSION;", versionFields)
        val patchedRadio = foldNoArgStringGetter(
            definingClass = "Landroid/os/Build;",
            methodNames = setOf("getRadioVersion"),
            value = buildFields.getValue("RADIO"),
        )

        val total = patchedBuild + patchedVersion + patchedRadio
        if (total > 0) {
            logger.info("Spoofed $total Build-extra read(s)")
        } else {
            logger.warning("No Build-extra reads found. No changes applied.")
        }
    }
}
