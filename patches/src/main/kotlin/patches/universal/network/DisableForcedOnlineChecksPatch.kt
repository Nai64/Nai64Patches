package patches.universal.network

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import java.util.Locale
import java.util.logging.Logger
import patches.universal.misc.foldBooleanReturns

private val onlineTerms = listOf(
    "checking connection",
    "check your connection",
    "connection required",
    "no internet",
    "offline",
    "server maintenance",
    "server unavailable",
    "unable to connect",
    "connection failed",
    "network unavailable",
    "online required",
)

private val positiveGateNames = setOf(
    "isonline",
    "isconnected",
    "hasconnection",
    "canconnect",
    "serveravailable",
    "networkavailable",
)

private val negativeGateNames = setOf(
    "isoffline",
    "requiresinternet",
    "requiresonline",
    "serverunavailable",
    "connectionfailed",
    "networkunavailable",
    "ismaintenance",
    "maintenanceactive",
    "forcedoffline",
)

private fun String.normalized() = lowercase(Locale.ROOT).replace("_", "").replace("-", "")

private data class EngineFlags(
    val unity: Boolean = false,
    val unreal: Boolean = false,
    val godot: Boolean = false,
)

private fun engineFlags(types: List<String>): EngineFlags = EngineFlags(
    unity = types.any { it.contains("com/unity3d/player", ignoreCase = true) },
    unreal = types.any {
        it.contains("com/epicgames/ue4", ignoreCase = true) ||
            it.contains("com/epicgames/unreal", ignoreCase = true)
    },
    godot = types.any {
        it.contains("org/godotengine/godot", ignoreCase = true) ||
            it.contains("org/godotengine", ignoreCase = true)
    },
)

@Suppress("unused")
val disableForcedOnlineChecksPatch = bytecodePatch(
    name = "Disable Forced Online Checks (Experimental)",
    description = "Tries to skip online checks that prevent the app from starting. Auto mode handles most apps; turn it off to pick strategies manually. Experimental and may break online features.",
    default = false,
) {
    val autoMode by booleanOption(
        key = "autoMode",
        title = "Auto mode",
        description = "Automatically run all strategies supported by the detected APK",
        default = true,
    )
    val commonAndroidNetwork by booleanOption(
        key = "commonAndroidNetwork",
        title = "Common Android/network API",
        description = "Make common Android connectivity checks report an active connection",
        default = true,
    )
    val unityStrategy by booleanOption(
        key = "unityStrategy",
        title = "Unity strategy",
        description = "Apply generic high-confidence online-gate detection when Unity is detected",
        default = true,
    )
    val unrealStrategy by booleanOption(
        key = "unrealStrategy",
        title = "Unreal strategy",
        description = "Apply generic high-confidence online-gate detection when Unreal is detected",
        default = true,
    )
    val godotStrategy by booleanOption(
        key = "godotStrategy",
        title = "Godot strategy",
        description = "Apply generic high-confidence online-gate detection when Godot is detected",
        default = true,
    )
    val genericBytecodeStrategy by booleanOption(
        key = "genericBytecodeStrategy",
        title = "Generic bytecode strategy",
        description = "Scan app bytecode for high-confidence online gate methods without engine detection",
        default = true,
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val types = mutableListOf<String>()
        classDefForEach { classDef -> types += classDef.type }
        val engines = engineFlags(types)
        val auto = autoMode == true
        val useCommon = auto || commonAndroidNetwork == true
        val useUnity = auto || (unityStrategy == true && engines.unity)
        val useUnreal = auto || (unrealStrategy == true && engines.unreal)
        val useGodot = auto || (godotStrategy == true && engines.godot)
        val useGeneric = auto || genericBytecodeStrategy == true

        var patched = 0
        if (useCommon) {
            patched += foldBooleanReturns(
                mapOf(
                    "Landroid/net/wifi/WifiManager;" to mapOf("isWifiEnabled" to "0x1"),
                    "Landroid/net/NetworkInfo;" to mapOf(
                        "isConnected" to "0x1",
                        "isAvailable" to "0x1",
                        "isConnectedOrConnecting" to "0x1",
                    ),
                ),
            )
        }

        val enabledEngineStrategies = buildSet {
            if (useUnity) add("unity")
            if (useUnreal) add("unreal")
            if (useGodot) add("godot")
        }
        val detectedSelectedEngine =
            (engines.unity && "unity" in enabledEngineStrategies) ||
                (engines.unreal && "unreal" in enabledEngineStrategies) ||
                (engines.godot && "godot" in enabledEngineStrategies)

        if (useGeneric || enabledEngineStrategies.isNotEmpty()) {
            classDefForEach { classDef ->
                if (!useGeneric && !detectedSelectedEngine) return@classDefForEach

                val mutableClass = mutableClassDefBy(classDef)
                for (method in mutableClass.methods) {
                    if (method.returnType != "Z") continue
                    val implementation = method.implementation ?: continue
                    val methodName = method.name.normalized()
                    val strings = implementation.instructions.mapNotNull { instruction ->
                        ((instruction as? ReferenceInstruction)?.reference as? StringReference)?.string
                    }.map(String::normalized)
                    val hasOnlineText = strings.any { value -> onlineTerms.any(value::contains) }
                    val positive = methodName in positiveGateNames
                    val negative = methodName in negativeGateNames
                    if (!positive && !negative) continue
                    if (!hasOnlineText && !methodName.contains("online") && !methodName.contains("connect")) continue
                    if (implementation.registerCount < 1) continue

                    val value = if (positive) "0x1" else "0x0"
                    method.addInstructions(0, "const/4 v0, $value\nreturn v0")
                    patched++
                }
            }
        }

        if (patched > 0) {
            logger.info(
                "Disable Forced Online Checks: patched $patched check(s); " +
                    "detected engines=" + listOfNotNull(
                        "Unity".takeIf { engines.unity },
                        "Unreal".takeIf { engines.unreal },
                        "Godot".takeIf { engines.godot },
                    ).ifEmpty { listOf("none") }.joinToString(", "),
            )
        } else {
            logger.warning(
                "No high-confidence forced-online checks found. Server-side enforcement and " +
                    "native-only engine logic were not modified.",
            )
        }
    }
}
