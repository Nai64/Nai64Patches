package patches.universal.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x
import java.util.logging.Logger

@Suppress("unused")
val pairipBypassPatch = bytecodePatch(
    name = "Pairip Bypass",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        // ── Strategy 1: Local installer check ──
        PerformLocalInstallerCheckFingerprint.methodOrNull?.let {
            it.addInstructions(0, listOf(
                BuilderInstruction11n(Opcode.CONST_4, 0, 1),
                BuilderInstruction11x(Opcode.RETURN, 0),
            ))
            logger.info("Applied Pairip performLocalInstallerCheck spoof")
        }

        // ── Strategy 2: APK signature integrity check ──
        PairipSignatureCheckVerifyIntegrityFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                return-void
            """.trimIndent())
            logger.info("Applied Pairip SignatureCheck.verifyIntegrity bypass")
        }

        // ── Strategy 3: Signature match check (belt-and-suspenders) ──
        PairipSignatureCheckVerifySignatureMatchesFingerprint.methodOrNull?.let {
            it.addInstructions(0, listOf(
                BuilderInstruction11n(Opcode.CONST_4, 0, 1),
                BuilderInstruction11x(Opcode.RETURN, 0),
            ))
            logger.info("Applied Pairip SignatureCheck.verifySignatureMatches bypass")
        }

        // ── Strategy 4: LicenseClient error dialog ──
        PairipLicenseClientStartErrorDialogFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                return-void
            """.trimIndent())
            logger.info("Applied Pairip LicenseClient error dialog suppress")
        }

        // ── Strategy 5: LicenseClient paywall ──
        PairipLicenseClientStartPaywallFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                return-void
            """.trimIndent())
            logger.info("Applied Pairip LicenseClient paywall suppress")
        }

        // ── Strategy 6: LicenseActivity showPaywallAndCloseApp ──
        PairipLicenseActivityShowPaywallFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                return-void
            """.trimIndent())
            logger.info("Applied Pairip LicenseActivity paywall suppress")
        }

        // ── Strategy 7a: Application.attachBaseContext — main entry point ──
        PairipApplicationAttachBaseContextFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                invoke-static {p1}, Lcom/pairip/VMRunner;->setContext(Landroid/content/Context;)V
                invoke-super {p0, p1}, Lcom/pairip/application/Application;->attachBaseContext(Landroid/content/Context;)V
                return-void
            """.trimIndent())
            logger.info("Applied Pairip Application.attachBaseContext bypass")
        }

        // ── Strategy 7b: Application.onCreate — backup entry point ──
        PairipApplicationOnCreateFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                invoke-super {p0}, Lcom/pairip/application/Application;->onCreate()V
                return-void
            """.trimIndent())
            logger.info("Applied Pairip Application.onCreate bypass")
        }

        // ── Strategy 8: LicenseClient.checkLicense — root kill ──
        PairipLicenseClientCheckLicenseFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                return-void
            """.trimIndent())
            logger.info("Applied Pairip LicenseClient.checkLicense root kill")
        }

        // ── Strategy 9: LicenseContentProvider.onCreate ──
        PairipLicenseContentProviderOnCreateFingerprint.methodOrNull?.let {
            it.addInstructions(0, listOf(
                BuilderInstruction11n(Opcode.CONST_4, 0, 0),
                BuilderInstruction11x(Opcode.RETURN, 0),
            ))
            logger.info("Applied Pairip LicenseContentProvider.onCreate bypass")
        }

        // ── Strategy 10: LicenseContentProvider.query ──
        PairipLicenseContentProviderQueryFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                const/4 v0, 0x0
                return-object v0
            """.trimIndent())
            logger.info("Applied Pairip LicenseContentProvider.query bypass")
        }

        // ── Strategy 11: InitContextProvider.getContext ──
        PairipInitContextProviderGetContextFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                const/4 v0, 0x0
                return-object v0
            """.trimIndent())
            logger.info("Applied Pairip InitContextProvider.getContext bypass")
        }
    }
}
