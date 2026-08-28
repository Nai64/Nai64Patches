package patches.universal.telemetry

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import java.util.logging.Logger

/** Class prefixes of the supported crash-reporting SDKs. */
private val crashSdkPrefixes = mapOf(
    "Lcom/crashlytics/android" to "Crashlytics",
    "Lio/fabric/sdk" to "Fabric",
    "Lio/sentry" to "Sentry",
    "Lcom/bugsnag" to "Bugsnag",
    "Lorg/acra" to "ACRA",
    "Lnet/hockeyapp" to "HockeyApp",
    "Lcom/microsoft/appcenter/crashes" to "AppCenter Crashes",
    "Lcom/instabug" to "Instabug",
    "Lly/count/android" to "Countly",
)

/** Static entry points used by crash SDKs to bootstrap themselves. */
private val entryPointNames = setOf(
    "init", "start", "register", "initialize", "initializeSdk", "setup",
)

@Suppress("unused")
val disableCrashReportersPatch = bytecodePatch(
    name = "Disable Crash Reporters",
    description = "Blocks crash reporting so the app doesn't send crash logs.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val detected = mutableMapOf<String, Int>()
        var patched = 0
        classDefForEach { classDef ->
            val sdkName = crashSdkPrefixes.entries.firstOrNull { classDef.type.startsWith(it.key) }?.value
                ?: return@classDefForEach

            val mutableClass = mutableClassDefBy(classDef)
            for (method in mutableClass.methods) {
                // Only static void bootstrap methods are touched: constructors and
                // value-returning methods are never modified.
                if (method.returnType != "V") continue
                if (!AccessFlags.STATIC.isSet(method.accessFlags)) continue
                if (method.name !in entryPointNames) continue

                method.addInstruction(0, "return-void")
                detected[sdkName] = (detected[sdkName] ?: 0) + 1
                patched++
            }
        }

        if (patched == 0) {
            logger.warning("No supported crash-reporting SDKs found. No changes applied.")
            return@execute
        }

        detected.forEach { (sdk, count) -> logger.info("Disabled $sdk ($count method(s))") }
        logger.info("Disable Crash Reporters patch succeeded (${detected.size} SDK(s) affected)")
    }
}
