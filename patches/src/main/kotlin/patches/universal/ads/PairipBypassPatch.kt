package patches.universal.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x
import java.util.logging.Logger
import org.w3c.dom.Element
import patches.universal.manifest.NS_ANDROID
import patches.universal.manifest.applicationOrNull

@Suppress("unused")
val pairipBypassPatch = bytecodePatch(
    name = "Pairip Bypass",
    description = "Bypass app protection so the patched app can start.",
    default = false,
) {
    val localInstallerChecks by booleanOption(
        key = "localInstallerChecks",
        default = true,
        title = "Local installer checks",
        description = "Spoof checks that verify which installer installed the app.",
    )
    val storeSpoofChecks by booleanOption(
        key = "storeSpoofChecks",
        default = true,
        title = "Play Store install spoof",
        description = "Report Play Store installation (integrated Spoof Play Store Install Source).",
    )
    val startupLauncherChecks by booleanOption(
        key = "startupLauncherChecks",
        default = true,
        title = "StartupLauncher checks",
        description = "Bypass both PairIP StartupLauncher.launch and StartupLauncher.pairip entry points.",
    )
    val disableFirebase by booleanOption(
        key = "disableFirebase",
        default = true,
        title = "Disable Firebase",
        description = "Add Firebase manifest switches that stop Analytics, Messaging, Crashlytics and Performance from auto-initializing, and remove the measurement init provider, receiver and services. For apps that crash at startup in ScionFrontendApi (broken Dynamite loading).",
    )

    // -- Strategy 8: Firebase disable + measurement removal (internal resource patch) --
    // Merged from Disable Firebase Auto-Init: manifest switches first, then
    // removal of the init provider (boots FirebaseApp, analytics connector
    // and the Scion thread) and the measurement receiver/services.
    val firebaseCleanupPatch = resourcePatch(
        name = "Pairip Firebase Cleanup (internal)",
        default = false,
    ) {
        execute {
            val logger = Logger.getLogger(this::class.java.name)
            if (disableFirebase != true) {
                logger.info("Firebase cleanup disabled by strategy selection")
                return@execute
            }

            val switches = mapOf(
                "firebase_analytics_collection_enabled" to "false",
                "firebase_messaging_auto_init_enabled" to "false",
                "firebase_crashlytics_collection_enabled" to "false",
                "firebase_performance_collection_enabled" to "false",
            )
            var added = 0
            var updated = 0
            document("AndroidManifest.xml").use { manifest ->
                val application = manifest.documentElement.applicationOrNull()
                    ?: return@use

                for ((name, value) in switches) {
                    val metadata = application.getElementsByTagName("meta-data")
                    var target: Element? = null
                    for (i in 0 until metadata.length) {
                        val meta = metadata.item(i) as? Element ?: continue
                        if (meta.getAttributeNS(NS_ANDROID, "name") == name) {
                            target = meta
                            break
                        }
                    }

                    if (target != null) {
                        if (target.getAttributeNS(NS_ANDROID, "value") != value) {
                            target.setAttributeNS(NS_ANDROID, "android:value", value)
                            updated++
                        }
                    } else {
                        val meta = manifest.createElement("meta-data")
                        meta.setAttributeNS(NS_ANDROID, "android:name", name)
                        meta.setAttributeNS(NS_ANDROID, "android:value", value)
                        application.appendChild(meta)
                        added++
                    }
                }
            }
            if (added > 0 || updated > 0) {
                logger.info("Firebase auto-init: $added switch(es) added, $updated overridden")
            }

            val measurementComponents = setOf(
                "io.invertase.firebase.app.ReactNativeFirebaseAppInitProvider",
                "com.google.firebase.provider.FirebaseInitProvider",
                "com.google.android.gms.measurement.AppMeasurementReceiver",
                "com.google.android.gms.measurement.AppMeasurementService",
                "com.google.android.gms.measurement.AppMeasurementJobService",
                "com.google.android.gms.measurement.AppMeasurementContentProvider",
            )
            var removed = 0
            document("AndroidManifest.xml").use { manifest ->
                for (tag in listOf("provider", "receiver", "service")) {
                    val nodes = manifest.getElementsByTagName(tag)
                    for (index in nodes.length - 1 downTo 0) {
                        val component = nodes.item(index) as? Element ?: continue
                        val name = component.getAttributeNS(NS_ANDROID, "name")
                            .takeIf { it.isNotEmpty() }
                            ?: component.getAttribute("android:name")
                        if (name in measurementComponents) {
                            component.parentNode?.removeChild(component)
                            removed++
                        }
                    }
                }
            }
            if (removed > 0) {
                logger.info("Removed $removed measurement component(s)")
            } else {
                logger.warning("No measurement components found. No changes applied.")
            }
        }
    }

    dependsOn(firebaseCleanupPatch)

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        // -- Strategy 1: Local installer check --
        // Report a successful local installer check.
        if (localInstallerChecks == true) PerformLocalInstallerCheckFingerprint.methodOrNull?.let {
            it.addInstructions(0, listOf(
                BuilderInstruction11n(Opcode.CONST_4, 0, 1),
                BuilderInstruction11x(Opcode.RETURN, 0),
            ))
            logger.info("Applied Pairip performLocalInstallerCheck spoof")
        }

        // -- Strategies 2-5: Spoof Play Store install source --
        // Integrated from Spoof Play Store Install Source: report Play Store
        // installation. Only the first matching spoof strategy applies.
        var spoofName: String? = null
        if (storeSpoofChecks == true) {
            GenericBooleanInstallerCheckFingerprint.methodOrNull?.let {
                it.addInstructions(0, listOf(
                    BuilderInstruction11n(Opcode.CONST_4, 0, 1),
                    BuilderInstruction11x(Opcode.RETURN, 0),
                ))
                logger.info("Applied generic boolean Play Store spoof")
                spoofName = "generic boolean spoof"
            }

            if (spoofName == null) GenericStringInstallerCheckFingerprint.methodOrNull?.let {
                it.addInstructions(0, """
                    const-string v0, "com.android.vending"
                    return-object v0
                """.trimIndent())
                logger.info("Applied Play Store installer source spoof")
                spoofName = "installer source"
            }

            if (spoofName == null) FallbackBooleanInstallerCheckFingerprint.methodOrNull?.let {
                it.addInstructions(0, listOf(
                    BuilderInstruction11n(Opcode.CONST_4, 0, 1),
                    BuilderInstruction11x(Opcode.RETURN, 0),
                ))
                logger.info("Applied fallback boolean Play Store spoof")
                spoofName = "fallback boolean spoof"
            }

            if (spoofName == null) FallbackStringInstallerCheckFingerprint.methodOrNull?.let {
                it.addInstructions(0, """
                    const-string v0, "com.android.vending"
                    return-object v0
                """.trimIndent())
                logger.info("Applied fallback String Play Store spoof")
                spoofName = "fallback installer source"
            }
        }

        // -- Strategy 6: StartupLauncher.launch --
        // Disable the PairIP startup dispatcher.
        if (startupLauncherChecks == true) PairipStartupLauncherLaunchFingerprint.methodOrNull?.let {
            it.addInstructions(0, "return-void")
            logger.info("Applied PairIP StartupLauncher.launch bypass")
        }

        // -- Strategy 7: StartupLauncher.pairip --
        // Disable the PairIP dispatcher entry point.
        if (startupLauncherChecks == true) PairipStartupLauncherPairipFingerprint.methodOrNull?.let {
            it.addInstructions(0, "return-void")
            logger.info("Applied PairIP StartupLauncher.pairip bypass")
        }

        val applied = buildList {
            if (localInstallerChecks == true && PerformLocalInstallerCheckFingerprint.methodOrNull != null) add("performLocalInstallerCheck")
            if (spoofName != null) add(spoofName!!)
            if (startupLauncherChecks == true && PairipStartupLauncherLaunchFingerprint.methodOrNull != null) add("StartupLauncher.launch")
            if (startupLauncherChecks == true && PairipStartupLauncherPairipFingerprint.methodOrNull != null) add("StartupLauncher.pairip")
        }
        if (applied.isEmpty()) {
            logger.warning("No Pairip license methods found. No changes applied.")
        } else {
            logger.info("Pairip Bypass (Experimental) patch succeeded (${applied.size} strategy(s) applied)")
            logger.warning("IF THE APP CRASHES OR STILL BRINGS UP PLAY STORE, DON'T ASK ME TO FIX IT. IT IS NOT POSSIBLE")
            logger.warning("IF THE APP CRASHES OR STILL BRINGS UP PLAY STORE, DON'T ASK ME TO FIX IT. IT IS NOT POSSIBLE")
            logger.warning("IF THE APP CRASHES OR STILL BRINGS UP PLAY STORE, DON'T ASK ME TO FIX IT. IT IS NOT POSSIBLE")
        }
    }
}
