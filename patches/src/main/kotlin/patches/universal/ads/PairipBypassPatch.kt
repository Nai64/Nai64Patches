package patches.universal.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.booleanOption
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

private val pairipLicenseManifestCleanupPatch = resourcePatch(
    name = "Pairip License Manifest Cleanup (internal)",
    default = false,
) {
    dependsOn(applicationRedirectPatch)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val androidNamespace = "http://schemas.android.com/apk/res/android"
        val pairipComponents = setOf(
            "com.pairip.licensecheck.LicenseActivity",
            "com.pairip.licensecheck.LicenseContentProvider",
        )
        var removed = 0

        document("AndroidManifest.xml").use { manifest ->
            for (tag in listOf("activity", "provider")) {
                val nodes = manifest.getElementsByTagName(tag)
                for (index in nodes.length - 1 downTo 0) {
                    val component = nodes.item(index) as? Element ?: continue
                    val name = component.getAttributeNS(androidNamespace, "name")
                    if (name in pairipComponents) {
                        component.parentNode?.removeChild(component)
                        removed++
                    }
                }
            }

            val permissions = manifest.getElementsByTagName("uses-permission")
            for (index in permissions.length - 1 downTo 0) {
                val permission = permissions.item(index) as? Element ?: continue
                if (permission.getAttributeNS(androidNamespace, "name") == "com.android.vending.CHECK_LICENSE") {
                    permission.parentNode?.removeChild(permission)
                    removed++
                }
            }
        }

        if (removed > 0) {
            logger.info("Removed $removed Pairip license manifest entr${if (removed == 1) "y" else "ies"}")
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
    description = "Pairip is anti-tamper / license protection used by some games. This bypasses its checks so patched or modified builds run instead of being blocked.",
    default = false,
) {
    dependsOn(pairipLicenseManifestCleanupPatch)

    val automaticStrategySelection by booleanOption(
        key = "automaticStrategySelection",
        default = true,
        title = "Automatic strategy selection",
        description = "Apply every compatible PairIP strategy whose target is found. Turn this off to select only the strategy groups needed for testing.",
    )
    val localInstallerChecks by booleanOption(
        key = "localInstallerChecks",
        default = false,
        title = "Local installer checks",
        description = "Spoof checks that verify which installer installed the app.",
    )
    val signatureChecks by booleanOption(
        key = "signatureChecks",
        default = false,
        title = "Signature checks",
        description = "Bypass APK integrity and signature-match checks.",
    )
    val licenseUiSuppression by booleanOption(
        key = "licenseUiSuppression",
        default = false,
        title = "License error and paywall UI",
        description = "Suppress PairIP error dialogs, paywalls, and close-app screens.",
    )
    val applicationStartupHooks by booleanOption(
        key = "applicationStartupHooks",
        default = false,
        title = "Application startup hooks",
        description = "Bypass PairIP Application attachBaseContext and onCreate hooks. These run early and may be less compatible with some apps.",
    )
    val licenseClientChecks by booleanOption(
        key = "licenseClientChecks",
        default = false,
        title = "License client checks",
        description = "Disable the legacy LicenseClient license-check and root-termination path.",
    )
    val contentProviderChecks by booleanOption(
        key = "contentProviderChecks",
        default = false,
        title = "Content provider checks",
        description = "Bypass PairIP content-provider initialization and query checks.",
    )
    val responseValidationChecks by booleanOption(
        key = "responseValidationChecks",
        default = false,
        title = "Response validation checks",
        description = "Bypass license-response validation, metadata, and signature checks across legacy validator variants.",
    )
    val pairipV2Checks by booleanOption(
        key = "pairipV2Checks",
        default = false,
        title = "PairIP V2 checks",
        description = "Bypass PairIP V2 license flow, response signature verification, and repeated background checks.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val applyLocalInstallerChecks = automaticStrategySelection == true || localInstallerChecks == true
        val applySignatureChecks = automaticStrategySelection == true || signatureChecks == true
        val applyLicenseUiSuppression = automaticStrategySelection == true || licenseUiSuppression == true
        val applyApplicationStartupHooks = automaticStrategySelection == true || applicationStartupHooks == true
        val applyLicenseClientChecks = automaticStrategySelection == true || licenseClientChecks == true
        val applyContentProviderChecks = automaticStrategySelection == true || contentProviderChecks == true
        val applyResponseValidationChecks = automaticStrategySelection == true || responseValidationChecks == true
        val applyPairipV2Checks = automaticStrategySelection == true || pairipV2Checks == true

        if (applyLocalInstallerChecks) {
        // -- Strategy 1: Local installer check --
        PerformLocalInstallerCheckFingerprint.methodOrNull?.let {
            it.addInstructions(0, listOf(
                BuilderInstruction11n(Opcode.CONST_4, 0, 1),
                BuilderInstruction11x(Opcode.RETURN, 0),
            ))
            logger.info("Applied Pairip performLocalInstallerCheck spoof")
        }

        GenericStringInstallerCheckFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                const-string v0, "com.android.vending"
                return-object v0
            """.trimIndent())
            logger.info("Applied Play Store installer source spoof")
        }

        }

        if (applySignatureChecks) {
        // -- Strategy 2: APK signature integrity check --
        PairipSignatureCheckVerifyIntegrityFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                return-void
            """.trimIndent())
            logger.info("Applied Pairip SignatureCheck.verifyIntegrity bypass")
        }

        // -- Strategy 3: Signature match check (belt-and-suspenders) --
        PairipSignatureCheckVerifySignatureMatchesFingerprint.methodOrNull?.let {
            it.addInstructions(0, listOf(
                BuilderInstruction11n(Opcode.CONST_4, 0, 1),
                BuilderInstruction11x(Opcode.RETURN, 0),
            ))
            logger.info("Applied Pairip SignatureCheck.verifySignatureMatches bypass")
        }

        }

        if (applyLicenseUiSuppression) {
        // -- Strategy 4: LicenseClient error dialog --
        PairipLicenseClientStartErrorDialogFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                return-void
            """.trimIndent())
            logger.info("Applied Pairip LicenseClient error dialog suppress")
        }

        // -- Strategy 5: LicenseClient paywall --
        PairipLicenseClientStartPaywallFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                return-void
            """.trimIndent())
            logger.info("Applied Pairip LicenseClient paywall suppress")
        }

        // -- Strategy 6: LicenseActivity showPaywallAndCloseApp --
        PairipLicenseActivityShowPaywallFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                return-void
            """.trimIndent())
            logger.info("Applied Pairip LicenseActivity paywall suppress")
        }

        PairipLicenseActivityNnStartFingerprint.methodOrNull?.let {
            it.addInstructions(0, "return-void")
            logger.info("Applied Pairip LicenseActivity.nnStart suppress")
        }

        PairipLicenseActivityCloseAppFingerprint.methodOrNull?.let {
            it.addInstructions(0, "return-void")
            logger.info("Applied Pairip LicenseActivity.closeApp suppress")
        }

        PairipLicenseActivityExitAppFingerprint.methodOrNull?.let {
            it.addInstructions(0, "return-void")
            logger.info("Applied Pairip LicenseActivity.exitApp suppress")
        }

        PairipLicenseActivityCloseappFingerprint.methodOrNull?.let {
            it.addInstructions(0, "return-void")
            logger.info("Applied Pairip LicenseActivity.closeapp suppress")
        }

        PairipLicenseActivityExitappFingerprint.methodOrNull?.let {
            it.addInstructions(0, "return-void")
            logger.info("Applied Pairip LicenseActivity.exitapp suppress")
        }

        }

        if (applyApplicationStartupHooks) {
        // -- Strategy 7a: Application.attachBaseContext - main entry point --
        PairipApplicationAttachBaseContextFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                invoke-super {p0, p1}, Landroid/app/Application;->attachBaseContext(Landroid/content/Context;)V
                return-void
            """.trimIndent())
            logger.info("Applied Pairip Application.attachBaseContext bypass")
        }

        // -- Strategy 7b: Application.onCreate - backup entry point --
        PairipApplicationOnCreateFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                invoke-super {p0}, Landroid/app/Application;->onCreate()V
                return-void
            """.trimIndent())
            logger.info("Applied Pairip Application.onCreate bypass")
        }

        }

        if (applyLicenseClientChecks) {
        // -- Strategy 8: LicenseClient.checkLicense - root kill --
        PairipLicenseClientCheckLicenseFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                return-void
            """.trimIndent())
            logger.info("Applied Pairip LicenseClient.checkLicense root kill")
        }

        PairipLicenseClientInitializeLicenseCheckFingerprint.methodOrNull?.let {
            it.addInstructions(0, "return-void")
            logger.info("Applied Pairip LicenseClient.initializeLicenseCheck suppress")
        }

        }

        if (applyContentProviderChecks) {
        // -- Strategy 9: LicenseContentProvider.onCreate (report success) --
        PairipLicenseContentProviderOnCreateFingerprint.methodOrNull?.let {
            it.addInstructions(0, listOf(
                BuilderInstruction11n(Opcode.CONST_4, 0, 1),
                BuilderInstruction11x(Opcode.RETURN, 0),
            ))
            logger.info("Applied Pairip LicenseContentProvider.onCreate bypass")
        }

        // -- Strategy 10: LicenseContentProvider.query --
        PairipLicenseContentProviderQueryFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                const/4 v0, 0x0
                return-object v0
            """.trimIndent())
            logger.info("Applied Pairip LicenseContentProvider.query bypass")
        }

        // -- Strategy 11: InitContextProvider.getContext --
        PairipInitContextProviderGetContextFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                const/4 v0, 0x0
                return-object v0
            """.trimIndent())
            logger.info("Applied Pairip InitContextProvider.getContext bypass")
        }

        }

        if (applyResponseValidationChecks) {
        // -- Strategy 12: LicenseResponseHelper.validateResponse --
        PairipLicenseResponseHelperValidateResponseFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                return-void
            """.trimIndent())
            logger.info("Applied Pairip LicenseResponseHelper.validateResponse bypass")
        }

        // -- Strategy 13: LicenseResponseHelper.getRepeatedCheckMetadata --
        PairipLicenseResponseHelperGetRepeatedCheckMetadataFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                const/4 v0, 0x0
                return-object v0
            """.trimIndent())
            logger.info("Applied Pairip LicenseResponseHelper.getRepeatedCheckMetadata bypass")
        }

        // -- Strategy 14: LicenseResponseHelper.verifySignature --
        PairipLicenseResponseHelperVerifySignatureFingerprint.methodOrNull?.let {
            it.addInstructions(0, listOf(
                BuilderInstruction11n(Opcode.CONST_4, 0, 1),
                BuilderInstruction11x(Opcode.RETURN, 0),
            ))
            logger.info("Applied Pairip LicenseResponseHelper.verifySignature bypass")
        }

        // -- Strategy 15: ResponseValidator.validateResponse --
        PairipResponseValidatorValidateResponseFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                return-void
            """.trimIndent())
            logger.info("Applied Pairip ResponseValidator.validateResponse bypass")
        }

        // -- Strategy 16: ResponseValidator.verifySignature --
        PairipResponseValidatorVerifySignatureFingerprint.methodOrNull?.let {
            it.addInstructions(0, listOf(
                BuilderInstruction11n(Opcode.CONST_4, 0, 1),
                BuilderInstruction11x(Opcode.RETURN, 0),
            ))
            logger.info("Applied Pairip ResponseValidator.verifySignature bypass")
        }

        // -- Strategy 17: licensecheck3 ResponseValidator.validateResponse --
        PairipResponseValidatorV3ValidateResponseFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                return-void
            """.trimIndent())
            logger.info("Applied Pairip licensecheck3 ResponseValidator.validateResponse bypass")
        }

        }

        if (applyPairipV2Checks) {
        // -- Strategy 18: Pairip V2 checkLicenseInternal -> force license success --
        // V2 routes the verification result back to the app through the IBinder
        // listener supplied to checkLicenseInternal. Short-circuit it to call the
        // success path directly so the app unlocks regardless of the (now
        // neutralized) signature / response checks.
        PairipV2CheckLicenseInternalFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                invoke-virtual {p0, p1}, Lcom/pairip/licensecheck/LicenseClient;->reportSuccessfulLicenseCheck(Landroid/os/IBinder;)V
                return-void
            """.trimIndent())
            logger.info("Applied Pairip V2 checkLicenseInternal force-success")
        }

        // -- Strategy 19: Pairip V2 LicenseResponseHelper.verifySignature (void) --
        // V2's verifySignature returns void (V1 returned Z). Neutralize it so the
        // JWS signature of the license response is never rejected.
        PairipV2LicenseResponseHelperVerifySignatureFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                return-void
            """.trimIndent())
            logger.info("Applied Pairip V2 LicenseResponseHelper.verifySignature bypass")
        }

        // -- Strategy 20: Pairip V2 scheduleRepeatedLicenseCheck suppress --
        // Stops Pairip from re-verifying (and potentially re-locking) the app in
        // the background after the initial unlock.
        PairipV2ScheduleRepeatedLicenseCheckFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                return-void
            """.trimIndent())
            logger.info("Applied Pairip V2 scheduleRepeatedLicenseCheck suppress")
        }

        }

        val applied = buildList {
            fun addIfMatched(enabled: Boolean, name: String, matched: Boolean) {
                if (enabled && matched) add(name)
            }

            addIfMatched(applyLocalInstallerChecks, "performLocalInstallerCheck", PerformLocalInstallerCheckFingerprint.methodOrNull != null)
            addIfMatched(applyLocalInstallerChecks, "installer source", GenericStringInstallerCheckFingerprint.methodOrNull != null)
            addIfMatched(applySignatureChecks, "verifyIntegrity", PairipSignatureCheckVerifyIntegrityFingerprint.methodOrNull != null)
            addIfMatched(applySignatureChecks, "verifySignatureMatches", PairipSignatureCheckVerifySignatureMatchesFingerprint.methodOrNull != null)
            addIfMatched(applyLicenseUiSuppression, "errorDialog", PairipLicenseClientStartErrorDialogFingerprint.methodOrNull != null)
            addIfMatched(applyLicenseUiSuppression, "paywall", PairipLicenseClientStartPaywallFingerprint.methodOrNull != null)
            addIfMatched(applyLicenseUiSuppression, "showPaywallAndCloseApp", PairipLicenseActivityShowPaywallFingerprint.methodOrNull != null)
            addIfMatched(applyLicenseUiSuppression, "nnStart", PairipLicenseActivityNnStartFingerprint.methodOrNull != null)
            addIfMatched(applyLicenseUiSuppression, "closeApp", PairipLicenseActivityCloseAppFingerprint.methodOrNull != null)
            addIfMatched(applyLicenseUiSuppression, "exitApp", PairipLicenseActivityExitAppFingerprint.methodOrNull != null)
            addIfMatched(applyLicenseUiSuppression, "closeapp", PairipLicenseActivityCloseappFingerprint.methodOrNull != null)
            addIfMatched(applyLicenseUiSuppression, "exitapp", PairipLicenseActivityExitappFingerprint.methodOrNull != null)
            addIfMatched(applyApplicationStartupHooks, "attachBaseContext", PairipApplicationAttachBaseContextFingerprint.methodOrNull != null)
            addIfMatched(applyApplicationStartupHooks, "onCreate", PairipApplicationOnCreateFingerprint.methodOrNull != null)
            addIfMatched(applyLicenseClientChecks, "checkLicense", PairipLicenseClientCheckLicenseFingerprint.methodOrNull != null)
            addIfMatched(applyLicenseClientChecks, "initializeLicenseCheck", PairipLicenseClientInitializeLicenseCheckFingerprint.methodOrNull != null)
            addIfMatched(applyContentProviderChecks, "onCreate (ContentProvider)", PairipLicenseContentProviderOnCreateFingerprint.methodOrNull != null)
            addIfMatched(applyContentProviderChecks, "query", PairipLicenseContentProviderQueryFingerprint.methodOrNull != null)
            addIfMatched(applyContentProviderChecks, "getContext", PairipInitContextProviderGetContextFingerprint.methodOrNull != null)
            addIfMatched(applyResponseValidationChecks, "validateResponse", PairipLicenseResponseHelperValidateResponseFingerprint.methodOrNull != null)
            addIfMatched(applyResponseValidationChecks, "getRepeatedCheckMetadata", PairipLicenseResponseHelperGetRepeatedCheckMetadataFingerprint.methodOrNull != null)
            addIfMatched(applyResponseValidationChecks, "verifySignature (ResponseHelper)", PairipLicenseResponseHelperVerifySignatureFingerprint.methodOrNull != null)
            addIfMatched(applyResponseValidationChecks, "validateResponse (ResponseValidator)", PairipResponseValidatorValidateResponseFingerprint.methodOrNull != null)
            addIfMatched(applyResponseValidationChecks, "verifySignature (ResponseValidator)", PairipResponseValidatorVerifySignatureFingerprint.methodOrNull != null)
            addIfMatched(applyResponseValidationChecks, "validateResponse (V3)", PairipResponseValidatorV3ValidateResponseFingerprint.methodOrNull != null)
            addIfMatched(applyPairipV2Checks, "checkLicenseInternal (V2)", PairipV2CheckLicenseInternalFingerprint.methodOrNull != null)
            addIfMatched(applyPairipV2Checks, "verifySignature (V2)", PairipV2LicenseResponseHelperVerifySignatureFingerprint.methodOrNull != null)
            addIfMatched(applyPairipV2Checks, "scheduleRepeatedLicenseCheck (V2)", PairipV2ScheduleRepeatedLicenseCheckFingerprint.methodOrNull != null)
        }
        if (applied.isEmpty()) {
            val reason = if (automaticStrategySelection == true) {
                "No Pairip license methods found. No changes applied."
            } else {
                "No selected Pairip strategies matched. No changes applied."
            }
            logger.warning(reason)
        } else {
            logger.info("Pairip Bypass (Experimental) patch succeeded (${applied.size} strategy(s) applied)")
        }
    }
}