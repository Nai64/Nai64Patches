package patches.universal.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x
import java.util.logging.Logger

@Suppress("unused")
val installSourceSpoofPatch = bytecodePatch(
    name = "Spoof Play Store Install Source",
    description = "Makes the app think it was installed from Google Play Store. " +
            "Recommended if the app has a Play Store license check.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        // Strategy 1: Pairip performLocalInstallerCheck
        PerformLocalInstallerCheckFingerprint.methodOrNull?.let {
            it.addInstructions(0, listOf(
                BuilderInstruction11n(Opcode.CONST_4, 0, 1),
                BuilderInstruction11x(Opcode.RETURN, 0),
            ))
            logger.info("Applied Pairip performLocalInstallerCheck spoof")
        }

        // Strategy 2: Pairip SignatureCheck.verifyIntegrity() — runs in Application.attachBaseContext
        // before any app code, checks APK signature. Must be patched first to avoid crash.
        PairipSignatureCheckVerifyIntegrityFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                return-void
            """.trimIndent())
            logger.info("Applied Pairip SignatureCheck.verifyIntegrity bypass")
        }

        // Strategy 3: Pairip SignatureCheck.verifySignatureMatches() — belt-and-suspenders
        PairipSignatureCheckVerifySignatureMatchesFingerprint.methodOrNull?.let {
            it.addInstructions(0, listOf(
                BuilderInstruction11n(Opcode.CONST_4, 0, 1),
                BuilderInstruction11x(Opcode.RETURN, 0),
            ))
            logger.info("Applied Pairip SignatureCheck.verifySignatureMatches bypass")
        }

        // Strategy 4: Pairip LicenseClient error dialog — suppresses the "Get from Play Store" redirect
        PairipLicenseClientStartErrorDialogFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                return-void
            """.trimIndent())
            logger.info("Applied Pairip LicenseClient error dialog suppress")
        }

        // Strategy 5: Pairip LicenseClient paywall — suppresses the LVL paywall PendingIntent (opens Play Store)
        PairipLicenseClientStartPaywallFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                return-void
            """.trimIndent())
            logger.info("Applied Pairip LicenseClient paywall suppress")
        }

        // Strategy 6: Pairip LicenseActivity.showPaywallAndCloseApp — last link before PendingIntent.send()
        // Catches paywall even if LicenseActivity is started from native code (libpairipcore.so).
        PairipLicenseActivityShowPaywallFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                return-void
            """.trimIndent())
            logger.info("Applied Pairip LicenseActivity paywall suppress")
        }

        // ── Pairip Application entry point bypass ──
        // Prevents Pairip from loading libpairipcore.so and starting the VM.
        // Works by neutering the Application class methods that Pairip overrides.
        // If Pairip's Application extends the real app's Application, super calls
        // handle real app initialization normally.
        //
        // For apps where Pairip wraps android.app.Application directly (not extending
        // the real app), this may skip the real app's custom Application init.
        // In that case, use the manifest-level bypass instead (ResourcePatch).

        // Strategy 7a: Pairip Application.attachBaseContext — main Pairip entry point
        PairipApplicationAttachBaseContextFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                invoke-super {p0, p1}, Lcom/pairip/application/Application;->attachBaseContext(Landroid/content/Context;)V
                return-void
            """.trimIndent())
            logger.info("Applied Pairip Application.attachBaseContext bypass")
        }

        // Strategy 7b: Pairip Application.onCreate — backup in case attachBaseContext isn't overridden
        PairipApplicationOnCreateFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                invoke-super {p0}, Lcom/pairip/application/Application;->onCreate()V
                return-void
            """.trimIndent())
            logger.info("Applied Pairip Application.onCreate bypass")
        }

        // ── Generic string-based strategies ──
        // These search for methods containing "com.android.vending" by return type.
        // These run even when Pairip is found — catches non-Pairip methods.

        // Strategy 7: Private boolean method referencing "com.android.vending"
        val boolCheck = GenericBooleanInstallerCheckFingerprint.methodOrNull
        if (boolCheck != null) {
            boolCheck.addInstructions(0, listOf(
                BuilderInstruction11n(Opcode.CONST_4, 0, 1),
                BuilderInstruction11x(Opcode.RETURN, 0),
            ))
            logger.info("Applied generic boolean Play Store spoof")
            return@execute
        }

        // Strategy 8: Private String method referencing "com.android.vending"
        val strCheck = GenericStringInstallerCheckFingerprint.methodOrNull
        if (strCheck != null) {
            strCheck.addInstructions(0, """
                const-string v0, "com.android.vending"
                return-object v0
            """.trimIndent())
            logger.info("Applied generic String Play Store spoof")
            return@execute
        }

        // Strategy 9: Private boolean method with "com.android.vending" (duplicate safety net)
        val fallbackBool = FallbackBooleanInstallerCheckFingerprint.methodOrNull
        if (fallbackBool != null) {
            fallbackBool.addInstructions(0, listOf(
                BuilderInstruction11n(Opcode.CONST_4, 0, 1),
                BuilderInstruction11x(Opcode.RETURN, 0),
            ))
            logger.info("Applied fallback boolean Play Store spoof")
            return@execute
        }

        // Strategy 10: Private String method with "com.android.vending" (duplicate safety net)
        val fallbackStr = FallbackStringInstallerCheckFingerprint.methodOrNull
        if (fallbackStr != null) {
            fallbackStr.addInstructions(0, """
                const-string v0, "com.android.vending"
                return-object v0
            """.trimIndent())
            logger.info("Applied fallback String Play Store spoof")
            return@execute
        }

        logger.warning("Could not find any install source check method. No changes applied.")
    }
}
