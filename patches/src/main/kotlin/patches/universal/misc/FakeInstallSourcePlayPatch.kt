package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger

@Suppress("unused")
val fakeInstallSourcePlayPatch = bytecodePatch(
    name = "Fake Play Store Install Source",
    description = "Fakes the install source.",
    default = false,
) {
    val installer by stringOption(
        title = "Installer package",
        default = "com.android.vending",
        key = "installerPackage",
        description = "Package name to report as installer.",
        values = linkedMapOf(
            "Play Store" to "com.android.vending",
            "Galaxy Store" to "com.sec.android.app.samsungapps",
            "Amazon Appstore" to "com.amazon.venezia",
            "Huawei AppGallery" to "com.huawei.appmarket",
            "Custom (use text)" to "com.android.vending",
        ),
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldStringGetterConst(
            "Landroid/content/pm/PackageManager;",
            setOf("getInstallerPackageName"),
            installer ?: "com.android.vending",
        )
        if (patched > 0) {
            logger.info("Spoofed Play Store install source at $patched call site(s)")
        } else {
            logger.warning("No getInstallerPackageName call sites found. No changes applied.")
        }
    }
}
