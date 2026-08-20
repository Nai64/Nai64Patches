package patches.universal.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.patcher.patch.resourcePatch
import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x
import java.util.logging.Logger
import org.w3c.dom.Element

private val applicationRedirectPatch = resourcePatch(
    name = "Pairip Application Redirect (internal)",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val real = discoverPairipAppClass(logger) ?: run {
            logger.warning("Could not discover real app class. Skipping manifest redirect.")
            return@execute
        }

        document("AndroidManifest.xml").use { doc ->
            val app = doc.getElementsByTagName("application").item(0) as? Element ?: run {
                logger.warning("No <application> element found")
                return@execute
            }
            val ns = "http://schemas.android.com/apk/res/android"
            val cur = app.getAttributeNS(ns, "name").let { if (!it.isNullOrEmpty()) it else app.getAttribute("android:name") }
            if (cur != "com.pairip.application.Application") {
                logger.info("Application class is '$cur' - not Pairip, skipping")
                return@execute
            }
            app.setAttributeNS(ns, "android:name", real)
            logger.info("Redirected Pairip -> $real - Pairip Application Redirect (internal) patch succeeded")
        }
    }
}

private fun ResourcePatchContext.discoverPairipAppClass(logger: Logger): String? {
    val dir = try { get("AndroidManifest.xml", false)?.parentFile } catch (_: Exception) { null }
        ?: return null.also { logger.warning("Cannot determine APK directory") }
    for (i in 0..99) {
        val f = java.io.File(dir, if (i == 0) "classes.dex" else "classes${i + 1}.dex")
        if (!f.exists()) break
        try {
            for (cls in DexFileFactory.loadDexFile(f, Opcodes.getDefault()).classes) {
                if (cls.type != "Lcom/pairip/application/Application;") continue
                val sup = cls.superclass ?: continue
                if (sup == "Ljava/lang/Object;" || sup == "Landroid/app/Application;") continue
                return sup.substringAfter("L").substringBefore(";").replace('/', '.').also {
                    logger.info("Discovered real app class from ${f.name}: $it")
                }
            }
        } catch (e: Exception) {
            logger.warning("Failed to parse ${f.name}: ${e.message}")
        }
    }
    return null
}

@Suppress("unused")
val pairipBypassPatch = bytecodePatch(
    name = "Pairip Bypass (Experimental)",
    description = "Bypass Pairip license protection",
    default = false,
) {
    dependsOn(applicationRedirectPatch)

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

        // ── Strategy 7a: Application.attachBaseContext - main entry point ──
        PairipApplicationAttachBaseContextFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                invoke-static {p1}, Lcom/pairip/VMRunner;->setContext(Landroid/content/Context;)V
                invoke-super {p0, p1}, Landroid/app/Application;->attachBaseContext(Landroid/content/Context;)V
                return-void
            """.trimIndent())
            logger.info("Applied Pairip Application.attachBaseContext bypass")
        }

        // ── Strategy 7b: Application.onCreate - backup entry point ──
        PairipApplicationOnCreateFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                invoke-super {p0}, Landroid/app/Application;->onCreate()V
                return-void
            """.trimIndent())
            logger.info("Applied Pairip Application.onCreate bypass")
        }

        // ── Strategy 8: LicenseClient.checkLicense - root kill ──
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

        // ── Strategy 12: LicenseResponseHelper.validateResponse ──
        PairipLicenseResponseHelperValidateResponseFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                return-void
            """.trimIndent())
            logger.info("Applied Pairip LicenseResponseHelper.validateResponse bypass")
        }

        // ── Strategy 13: LicenseResponseHelper.getRepeatedCheckMetadata ──
        PairipLicenseResponseHelperGetRepeatedCheckMetadataFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                const/4 v0, 0x0
                return-object v0
            """.trimIndent())
            logger.info("Applied Pairip LicenseResponseHelper.getRepeatedCheckMetadata bypass")
        }

        // ── Strategy 14: LicenseResponseHelper.verifySignature ──
        PairipLicenseResponseHelperVerifySignatureFingerprint.methodOrNull?.let {
            it.addInstructions(0, listOf(
                BuilderInstruction11n(Opcode.CONST_4, 0, 1),
                BuilderInstruction11x(Opcode.RETURN, 0),
            ))
            logger.info("Applied Pairip LicenseResponseHelper.verifySignature bypass")
        }

        // ── Strategy 15: ResponseValidator.validateResponse ──
        PairipResponseValidatorValidateResponseFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                return-void
            """.trimIndent())
            logger.info("Applied Pairip ResponseValidator.validateResponse bypass")
        }

        // ── Strategy 16: ResponseValidator.verifySignature ──
        PairipResponseValidatorVerifySignatureFingerprint.methodOrNull?.let {
            it.addInstructions(0, listOf(
                BuilderInstruction11n(Opcode.CONST_4, 0, 1),
                BuilderInstruction11x(Opcode.RETURN, 0),
            ))
            logger.info("Applied Pairip ResponseValidator.verifySignature bypass")
        }

        // ── Strategy 17: licensecheck3 ResponseValidator.validateResponse ──
        PairipResponseValidatorV3ValidateResponseFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                return-void
            """.trimIndent())
            logger.info("Applied Pairip licensecheck3 ResponseValidator.validateResponse bypass")
        }

        val applied = listOfNotNull(
            PerformLocalInstallerCheckFingerprint.methodOrNull?.let { "performLocalInstallerCheck" },
            PairipSignatureCheckVerifyIntegrityFingerprint.methodOrNull?.let { "verifyIntegrity" },
            PairipSignatureCheckVerifySignatureMatchesFingerprint.methodOrNull?.let { "verifySignatureMatches" },
            PairipLicenseClientStartErrorDialogFingerprint.methodOrNull?.let { "errorDialog" },
            PairipLicenseClientStartPaywallFingerprint.methodOrNull?.let { "paywall" },
            PairipLicenseActivityShowPaywallFingerprint.methodOrNull?.let { "showPaywallAndCloseApp" },
            PairipApplicationAttachBaseContextFingerprint.methodOrNull?.let { "attachBaseContext" },
            PairipApplicationOnCreateFingerprint.methodOrNull?.let { "onCreate" },
            PairipLicenseClientCheckLicenseFingerprint.methodOrNull?.let { "checkLicense" },
            PairipLicenseContentProviderOnCreateFingerprint.methodOrNull?.let { "onCreate (ContentProvider)" },
            PairipLicenseContentProviderQueryFingerprint.methodOrNull?.let { "query" },
            PairipInitContextProviderGetContextFingerprint.methodOrNull?.let { "getContext" },
            PairipLicenseResponseHelperValidateResponseFingerprint.methodOrNull?.let { "validateResponse" },
            PairipLicenseResponseHelperGetRepeatedCheckMetadataFingerprint.methodOrNull?.let { "getRepeatedCheckMetadata" },
            PairipLicenseResponseHelperVerifySignatureFingerprint.methodOrNull?.let { "verifySignature (ResponseHelper)" },
            PairipResponseValidatorValidateResponseFingerprint.methodOrNull?.let { "validateResponse (ResponseValidator)" },
            PairipResponseValidatorVerifySignatureFingerprint.methodOrNull?.let { "verifySignature (ResponseValidator)" },
            PairipResponseValidatorV3ValidateResponseFingerprint.methodOrNull?.let { "validateResponse (V3)" },
        )
        logger.info("Pairip Bypass (Experimental) patch succeeded (${applied.size} strategy(s) applied)")
    }
}