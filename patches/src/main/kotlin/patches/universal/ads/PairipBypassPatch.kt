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
    val automaticStrategySelection by booleanOption(
        key = "automaticStrategySelection",
        default = true,
        title = "Automatic strategy selection",
        description = "Apply every compatible PairIP strategy whose target is found. Turn this off to select only the strategy groups needed for testing.",
    )
    val manifestCleanupStrategy by booleanOption(
        key = "manifestCleanupStrategy",
        default = false,
        title = "XML manifest cleanup",
        description = "Redirect the PairIP Application wrapper when possible, then remove PairIP license activities, provider, and CHECK_LICENSE permission from AndroidManifest.xml.",
    )

    // -- Resource Strategy 1: PairIP Application redirect --
    // Replace the PairIP wrapper with a discovered real Application superclass.
    val applicationRedirectPatch = resourcePatch(
        name = "Pairip Application Redirect (internal)",
        default = false,
    ) {
        execute {
            val logger = Logger.getLogger(this::class.java.name)
            val applyManifestChanges = automaticStrategySelection == true || manifestCleanupStrategy == true
            if (!applyManifestChanges) {
                logger.info("Pairip Application redirect disabled by strategy selection")
                return@execute
            }

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
                    logger.info("Application class is '$cur' - not PairIP, skipping")
                    return@execute
                }
                app.setAttributeNS(ns, "android:name", real)
                logger.info("Redirected PairIP -> $real - PairIP Application Redirect (internal) patch succeeded")
            }
        }
    }

    // -- Resource Strategy 2: PairIP license manifest cleanup --
    // Remove only the exact PairIP license components and CHECK_LICENSE permission.
    val pairipLicenseManifestCleanupPatch = resourcePatch(
        name = "Pairip License Manifest Cleanup (internal)",
        default = false,
    ) {
        dependsOn(applicationRedirectPatch)

        execute {
            val logger = Logger.getLogger(this::class.java.name)
            val applyManifestCleanup = automaticStrategySelection == true || manifestCleanupStrategy == true
            if (!applyManifestCleanup) {
                logger.info("Pairip XML manifest cleanup disabled by strategy selection")
                return@execute
            }

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

    dependsOn(pairipLicenseManifestCleanupPatch)

    val pairipVmRunnerChecks by booleanOption(
        key = "pairipVmRunnerChecks",
        default = false,
        title = "PairIP VMRunner checks",
        description = "Bypass Application.<clinit> and VMRunner.invoke startup paths that can execute the PairIP VM.",
    )
    val startupLauncherChecks by booleanOption(
        key = "startupLauncherChecks",
        default = false,
        title = "StartupLauncher checks",
        description = "Bypass both PairIP StartupLauncher.launch and StartupLauncher.pairip entry points.",
    )
    val licenseClientV3Activity by booleanOption(
        key = "licenseClientV3Activity",
        default = false,
        title = "LicenseClient V3 activity",
        description = "Disable the PairIP V3 LicenseClient.onActivityCreate path.",
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

        fun isSelected(option: Boolean?) = automaticStrategySelection == true || option == true

        val applyLocalInstallerChecks = isSelected(localInstallerChecks)
        val applySignatureChecks = isSelected(signatureChecks)
        val applyLicenseUiSuppression = isSelected(licenseUiSuppression)
        val applyApplicationStartupHooks = isSelected(applicationStartupHooks)
        val applyLicenseClientChecks = isSelected(licenseClientChecks)
        val applyContentProviderChecks = isSelected(contentProviderChecks)
        val applyResponseValidationChecks = isSelected(responseValidationChecks)
        val applyPairipV2Checks = isSelected(pairipV2Checks)
        val applyPairipVmRunnerChecks = isSelected(pairipVmRunnerChecks)
        val applyStartupLauncherChecks = isSelected(startupLauncherChecks)
        val applyLicenseClientV3Activity = isSelected(licenseClientV3Activity)

        if (applyLocalInstallerChecks) {
        // -- Strategy 1: Local installer check --
        // Report a successful local installer check.
        PerformLocalInstallerCheckFingerprint.methodOrNull?.let {
            it.addInstructions(0, listOf(
                BuilderInstruction11n(Opcode.CONST_4, 0, 1),
                BuilderInstruction11x(Opcode.RETURN, 0),
            ))
            logger.info("Applied Pairip performLocalInstallerCheck spoof")
        }

        // -- Strategy 2: Generic installer-source string --
        // Return the Play Store package name when PairIP checks the installer source.
        GenericStringInstallerCheckFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                const-string v0, "com.android.vending"
                return-object v0
            """.trimIndent())
            logger.info("Applied Play Store installer source spoof")
        }

        }

        if (applySignatureChecks) {
        // -- Strategy 3: APK signature integrity check --
        // Skip PairIP's APK integrity verification routine.
        PairipSignatureCheckVerifyIntegrityFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                return-void
            """.trimIndent())
            logger.info("Applied Pairip SignatureCheck.verifyIntegrity bypass")
        }

        // -- Strategy 4: Signature match check --
        // Report a successful signature match to PairIP.
        PairipSignatureCheckVerifySignatureMatchesFingerprint.methodOrNull?.let {
            it.addInstructions(0, listOf(
                BuilderInstruction11n(Opcode.CONST_4, 0, 1),
                BuilderInstruction11x(Opcode.RETURN, 0),
            ))
            logger.info("Applied Pairip SignatureCheck.verifySignatureMatches bypass")
        }

        }

        if (applyLicenseUiSuppression) {
        // -- Strategy 5: LicenseClient error dialog --
        // Prevent the PairIP license failure dialog from opening.
        PairipLicenseClientStartErrorDialogFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                return-void
            """.trimIndent())
            logger.info("Applied Pairip LicenseClient error dialog suppress")
        }

        // -- Strategy 6: LicenseClient paywall --
        // Prevent the PairIP Play Store paywall activity from opening.
        PairipLicenseClientStartPaywallFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                return-void
            """.trimIndent())
            logger.info("Applied Pairip LicenseClient paywall suppress")
        }

        // -- Strategy 7: LicenseActivity.showPaywallAndCloseApp --
        // Suppress the PairIP activity's paywall-and-exit flow.
        PairipLicenseActivityShowPaywallFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                return-void
            """.trimIndent())
            logger.info("Applied Pairip LicenseActivity paywall suppress")
        }

        // -- Strategy 8: LicenseActivity.nnStart --
        // Suppress the PairIP license activity startup routine.
        PairipLicenseActivityNnStartFingerprint.methodOrNull?.let {
            it.addInstructions(0, "return-void")
            logger.info("Applied Pairip LicenseActivity.nnStart suppress")
        }

        // -- Strategy 9: LicenseActivity.closeApp --
        // Prevent the PairIP license activity from closing the app.
        PairipLicenseActivityCloseAppFingerprint.methodOrNull?.let {
            it.addInstructions(0, "return-void")
            logger.info("Applied Pairip LicenseActivity.closeApp suppress")
        }

        // -- Strategy 10: LicenseActivity.exitApp --
        // Prevent the PairIP license activity from exiting the app.
        PairipLicenseActivityExitAppFingerprint.methodOrNull?.let {
            it.addInstructions(0, "return-void")
            logger.info("Applied Pairip LicenseActivity.exitApp suppress")
        }

        // -- Strategy 11: LicenseActivity.closeapp --
        // Suppress the case-sensitive lowercase close-app variant.
        PairipLicenseActivityCloseappFingerprint.methodOrNull?.let {
            it.addInstructions(0, "return-void")
            logger.info("Applied Pairip LicenseActivity.closeapp suppress")
        }

        // -- Strategy 12: LicenseActivity.exitapp --
        // Suppress the case-sensitive lowercase exit-app variant.
        PairipLicenseActivityExitappFingerprint.methodOrNull?.let {
            it.addInstructions(0, "return-void")
            logger.info("Applied Pairip LicenseActivity.exitapp suppress")
        }

        }

        if (applyApplicationStartupHooks) {
        // -- Strategy 13: Application.attachBaseContext --
        // Skip PairIP's early Application startup code while preserving framework initialization.
        PairipApplicationAttachBaseContextFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                invoke-super {p0, p1}, Landroid/app/Application;->attachBaseContext(Landroid/content/Context;)V
                return-void
            """.trimIndent())
            logger.info("Applied Pairip Application.attachBaseContext bypass")
        }

        // -- Strategy 14: Application.onCreate --
        // Skip PairIP's Application onCreate startup hook.
        PairipApplicationOnCreateFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                invoke-super {p0}, Landroid/app/Application;->onCreate()V
                return-void
            """.trimIndent())
            logger.info("Applied Pairip Application.onCreate bypass")
        }

        }

        if (applyPairipVmRunnerChecks) {
        // -- Strategy 15: Application.<clinit> --
        // Prevent static PairIP initialization from starting the VM or native core.
        PairipApplicationClinitFingerprint.methodOrNull?.let {
            it.addInstructions(0, "return-void")
            logger.info("Applied Pairip Application.<clinit> bypass")
        }

        // -- Strategy 16: VMRunner.invoke --
        // Return null instead of executing PairIP VM instructions.
        PairipVMRunnerInvokeFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                const/4 v0, 0x0
                return-object v0
            """.trimIndent())
            logger.info("Applied PairIP VMRunner.invoke bypass")
        }
        }

        if (applyStartupLauncherChecks) {
        // -- Strategy 17: StartupLauncher.launch --
        // Disable the PairIP startup dispatcher.
        PairipStartupLauncherLaunchFingerprint.methodOrNull?.let {
            it.addInstructions(0, "return-void")
            logger.info("Applied PairIP StartupLauncher.launch bypass")
        }

        // -- Strategy 18: StartupLauncher.pairip --
        // Disable the PairIP dispatcher entry point.
        PairipStartupLauncherPairipFingerprint.methodOrNull?.let {
            it.addInstructions(0, "return-void")
            logger.info("Applied PairIP StartupLauncher.pairip bypass")
        }
        }

        if (applyLicenseClientV3Activity) {
        // -- Strategy 19: LicenseClientV3.onActivityCreate --
        // Disable the newer PairIP V3 license activity path.
        PairipLicenseClientV3OnActivityCreateFingerprint.methodOrNull?.let {
            it.addInstructions(0, "return-void")
            logger.info("Applied Pairip LicenseClientV3.onActivityCreate bypass")
        }
        }

        if (applyLicenseClientChecks) {
        // -- Strategy 20: LicenseClient.checkLicense --
        // Prevent the legacy license check from taking the root-termination path.
        PairipLicenseClientCheckLicenseFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                return-void
            """.trimIndent())
            logger.info("Applied Pairip LicenseClient.checkLicense root kill")
        }

        // -- Strategy 21: LicenseClient.initializeLicenseCheck --
        // Prevent the legacy client from scheduling or starting its license check.
        PairipLicenseClientInitializeLicenseCheckFingerprint.methodOrNull?.let {
            it.addInstructions(0, "return-void")
            logger.info("Applied Pairip LicenseClient.initializeLicenseCheck suppress")
        }

        }

        if (applyContentProviderChecks) {
        // -- Strategy 22: LicenseContentProvider.onCreate --
        // Report successful provider initialization to PairIP.
        PairipLicenseContentProviderOnCreateFingerprint.methodOrNull?.let {
            it.addInstructions(0, listOf(
                BuilderInstruction11n(Opcode.CONST_4, 0, 1),
                BuilderInstruction11x(Opcode.RETURN, 0),
            ))
            logger.info("Applied Pairip LicenseContentProvider.onCreate bypass")
        }

        // -- Strategy 23: LicenseContentProvider.query --
        // Return no result from the PairIP license provider query.
        PairipLicenseContentProviderQueryFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                const/4 v0, 0x0
                return-object v0
            """.trimIndent())
            logger.info("Applied Pairip LicenseContentProvider.query bypass")
        }

        // -- Strategy 24: InitContextProvider.getContext --
        // Prevent PairIP from retrieving its initialization context.
        PairipInitContextProviderGetContextFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                const/4 v0, 0x0
                return-object v0
            """.trimIndent())
            logger.info("Applied Pairip InitContextProvider.getContext bypass")
        }

        }

        if (applyResponseValidationChecks) {
        // -- Strategy 25: LicenseResponseHelper.validateResponse --
        // Skip legacy license-response validation.
        PairipLicenseResponseHelperValidateResponseFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                return-void
            """.trimIndent())
            logger.info("Applied Pairip LicenseResponseHelper.validateResponse bypass")
        }

        // -- Strategy 26: LicenseResponseHelper.getRepeatedCheckMetadata --
        // Remove metadata used to schedule repeated license checks.
        PairipLicenseResponseHelperGetRepeatedCheckMetadataFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                const/4 v0, 0x0
                return-object v0
            """.trimIndent())
            logger.info("Applied Pairip LicenseResponseHelper.getRepeatedCheckMetadata bypass")
        }

        // -- Strategy 27: LicenseResponseHelper.verifySignature --
        // Report a successful response signature check.
        PairipLicenseResponseHelperVerifySignatureFingerprint.methodOrNull?.let {
            it.addInstructions(0, listOf(
                BuilderInstruction11n(Opcode.CONST_4, 0, 1),
                BuilderInstruction11x(Opcode.RETURN, 0),
            ))
            logger.info("Applied Pairip LicenseResponseHelper.verifySignature bypass")
        }

        // -- Strategy 28: ResponseValidator.validateResponse --
        // Skip the legacy ResponseValidator response check.
        PairipResponseValidatorValidateResponseFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                return-void
            """.trimIndent())
            logger.info("Applied Pairip ResponseValidator.validateResponse bypass")
        }

        // -- Strategy 29: ResponseValidator.verifySignature --
        // Report a successful ResponseValidator signature check.
        PairipResponseValidatorVerifySignatureFingerprint.methodOrNull?.let {
            it.addInstructions(0, listOf(
                BuilderInstruction11n(Opcode.CONST_4, 0, 1),
                BuilderInstruction11x(Opcode.RETURN, 0),
            ))
            logger.info("Applied Pairip ResponseValidator.verifySignature bypass")
        }

        // -- Strategy 30: licensecheck3 ResponseValidator.validateResponse --
        // Skip the observed V3 response validation path.
        PairipResponseValidatorV3ValidateResponseFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                return-void
            """.trimIndent())
            logger.info("Applied Pairip licensecheck3 ResponseValidator.validateResponse bypass")
        }

        }

        if (applyPairipV2Checks) {
        // -- Strategy 31: PairIP V2 checkLicenseInternal --
        // Report a successful V2 license result through the supplied binder.
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

        // -- Strategy 32: PairIP V2 LicenseResponseHelper.verifySignature --
        // Skip the V2 response signature verification routine.
        // V2's verifySignature returns void (V1 returned Z); neutralize it so the
        // JWS signature of the license response is never rejected.
        PairipV2LicenseResponseHelperVerifySignatureFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                return-void
            """.trimIndent())
            logger.info("Applied Pairip V2 LicenseResponseHelper.verifySignature bypass")
        }

        // -- Strategy 33: PairIP V2 scheduleRepeatedLicenseCheck --
        // Stop PairIP from scheduling a later background recheck.
        // Stops PairIP from re-verifying (and potentially re-locking) the app in
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
            addIfMatched(applyPairipVmRunnerChecks, "Application.<clinit>", PairipApplicationClinitFingerprint.methodOrNull != null)
            addIfMatched(applyPairipVmRunnerChecks, "VMRunner.invoke", PairipVMRunnerInvokeFingerprint.methodOrNull != null)
            addIfMatched(applyStartupLauncherChecks, "StartupLauncher.launch", PairipStartupLauncherLaunchFingerprint.methodOrNull != null)
            addIfMatched(applyStartupLauncherChecks, "StartupLauncher.pairip", PairipStartupLauncherPairipFingerprint.methodOrNull != null)
            addIfMatched(applyLicenseClientV3Activity, "LicenseClientV3.onActivityCreate", PairipLicenseClientV3OnActivityCreateFingerprint.methodOrNull != null)
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
