package patches.universal.ui

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.intOption
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import app.morphe.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import patches.universal.ads.util.cloneMutable
import patches.universal.ads.util.p0Register
import java.io.File
import java.net.URL
import java.util.Base64
import java.util.logging.Logger

private const val ASSET_NAME = "nai64_startup.mp3"
private const val FIELD_PLAYER = "nai64StartupPlayer"

private fun resolveAudioBytes(src: String, logger: Logger): ByteArray? = runCatching {
    when {
        src.startsWith("data:", ignoreCase = true) -> {
            val comma = src.indexOf(',')
            if (comma < 0) return null
            val meta = src.substring(0, comma)
            if (!meta.contains("base64", ignoreCase = true)) return null
            Base64.getDecoder().decode(src.substring(comma + 1))
        }
        src.startsWith("http://", ignoreCase = true) || src.startsWith("https://", ignoreCase = true) ->
            URL(src).readBytes()
        File(src).exists() ->
            File(src).readBytes()
        else -> {
            logger.info("Treating source as raw base64.")
            Base64.getDecoder().decode(src)
        }
    }
}.getOrNull()

private val startupSoundAssetPatch = resourcePatch(
    name = "Startup Sound Asset (internal)",
    description = "Internal helper to write startup sound asset",
    default = false,
) {
    execute {
        // Placeholder - actual write is done in bytecode patch via File API using patch temp dir
        // This resource patch exists only to ensure assets directory is created
    }
}

@Suppress("unused")
val customStartupSoundPatch = bytecodePatch(
    name = "Custom Startup Sound",
    description = "Plays a custom sound once when the app starts.",
    default = false,
) {
    dependsOn(StartupHooks.resolveRealApplicationPatch)
    dependsOn(startupSoundAssetPatch)

    val soundSource by stringOption(
        title = "Sound source",
        default = "",
        key = "startupSoundSource",
        description = "Audio source: an http(s) URL, a local file path, or base64 (optionally prefixed with 'data:audio/mp3;base64,'). Keep short (<1 MB).",
    )
    val volume by intOption(
        title = "Volume (%)",
        default = 100,
        key = "startupSoundVolume",
        description = "Playback volume 0-100.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val src = (soundSource ?: "").trim()
        if (src.isEmpty()) {
            logger.warning("No sound source configured. No changes applied.")
            return@execute
        }
        val vol = (volume ?: 100).coerceIn(0, 100)
        val volF = vol / 100f

        val bytes = resolveAudioBytes(src, logger)
        if (bytes == null) {
            logger.warning("Could not load audio (custom source). No changes applied.")
            return@execute
        }
        if (bytes.size > 1_048_576) {
            logger.warning("Audio is large (${bytes.size} bytes) - may bloat APK, consider a shorter clip.")
        }

        // Write to assets via File API (works in both resource and bytecode context)
        try {
            val assetsDir = File("assets")
            // In patch context, get() is not available for bytecode, so use File with temp dir via reflection on patch context
            // Fallback: try to write via resource helper if available
            val assetFile = File(assetsDir, ASSET_NAME)
            // Assets dir may not exist in temp, create via get() if possible
            try {
                val getMethod = this::class.java.methods.find { it.name == "get" }
                if (getMethod != null) {
                    @Suppress("UNCHECKED_CAST")
                    val dir = getMethod.invoke(this, "assets", true) as File
                    dir.mkdirs()
                    File(dir, ASSET_NAME).writeBytes(bytes)
                    logger.info("Wrote startup sound ${bytes.size} bytes to assets/$ASSET_NAME via get()")
                } else {
                    throw Exception("get() not found")
                }
            } catch (_: Exception) {
                // Fallback to direct File in current working dir (patch temp)
                assetsDir.mkdirs()
                File(assetsDir, ASSET_NAME).writeBytes(bytes)
                logger.info("Wrote startup sound ${bytes.size} bytes to assets/$ASSET_NAME (fallback)")
            }
        } catch (e: Exception) {
            logger.warning("Failed to write asset: ${e.message}")
        }

        // Resolve Application class and onCreate
        val descriptor = StartupHooks.resolvedApplicationDescriptor
        val appAndMethod: Pair<MutableClass, MutableMethod>? = if (descriptor != null) {
            val cls = mutableClassDefByOrNull(descriptor)
            val m = cls?.methods?.firstOrNull { it.name == "onCreate" && it.returnType == "V" && it.parameterTypes.isEmpty() }
            if (cls != null && m != null) cls to m else null
        } else null

        val (mutableClass, onCreate) = appAndMethod ?: run {
            var result: Pair<MutableClass, MutableMethod>? = null
            classDefForEach { classDef ->
                if (classDef.superclass != "Landroid/app/Application;") return@classDefForEach
                val mc = mutableClassDefBy(classDef)
                val m = mc.methods.firstOrNull { it.name == "onCreate" && it.returnType == "V" && it.parameterTypes.isEmpty() }
                if (m != null && result == null) result = mc to m
            }
            result
        } ?: run {
            logger.warning("No Application.onCreate found. No changes applied.")
            return@execute
        }

        if (mutableClass.fields.none { it.name == StartupHooks.GUARD_FIELD }) {
            mutableClass.fields.add(
                ImmutableField(
                    mutableClass.type,
                    StartupHooks.GUARD_FIELD,
                    "Z",
                    AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
                    null, emptySet(), emptySet(),
                ).toMutable()
            )
        }
        if (mutableClass.fields.none { it.name == FIELD_PLAYER }) {
            mutableClass.fields.add(
                ImmutableField(
                    mutableClass.type,
                    FIELD_PLAYER,
                    "Landroid/media/MediaPlayer;",
                    AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
                    null, emptySet(), emptySet(),
                ).toMutable()
            )
        }
        val guard = "${mutableClass.type}->${StartupHooks.GUARD_FIELD}:Z"
        val fieldPlayer = "${mutableClass.type}->$FIELD_PLAYER:Landroid/media/MediaPlayer;"

        val tempBase = onCreate.implementation!!.registerCount
        val cloned = onCreate.cloneMutable(additionalRegisters = 6)
        val contextReg = cloned.p0Register
        val b = tempBase
        val volStr = volF.toString()

        val smali = StringBuilder().apply {
            appendLine("sget-boolean v$b, $guard")
            appendLine("if-nez v$b, :nai64_sound_skip")
            appendLine("const/16 v$b, 0x1")
            appendLine("sput-boolean v$b, $guard")
            appendLine("move-object/from16 v${b + 2}, v$contextReg")
            appendLine("invoke-virtual/range {v${b + 2}}, Landroid/content/Context;->getAssets()Landroid/content/res/AssetManager;")
            appendLine("move-result-object v${b + 2}")
            appendLine("const-string v${b + 3}, \"$ASSET_NAME\"")
            appendLine("invoke-virtual/range {v${b + 2} .. v${b + 3}}, Landroid/content/res/AssetManager;->openFd(Ljava/lang/String;)Landroid/content/res/AssetFileDescriptor;")
            appendLine("move-result-object v${b + 2}")
            appendLine("new-instance v${b + 3}, Landroid/media/MediaPlayer;")
            appendLine("invoke-direct/range {v${b + 3}}, Landroid/media/MediaPlayer;-><init>()V")
            appendLine("sput-object v${b + 3}, $fieldPlayer")
            appendLine("invoke-virtual/range {v${b + 2}}, Landroid/content/res/AssetFileDescriptor;->getFileDescriptor()Ljava/io/FileDescriptor;")
            appendLine("move-result-object v${b + 4}")
            appendLine("invoke-virtual/range {v${b + 2}}, Landroid/content/res/AssetFileDescriptor;->getStartOffset()J")
            appendLine("move-result-wide v${b + 4}")
            appendLine("invoke-virtual/range {v${b + 2}}, Landroid/content/res/AssetFileDescriptor;->getLength()J")
            appendLine("move-result-wide v${b + 2}")
            appendLine("invoke-virtual/range {v${b + 3} .. v${b + 5}}, Landroid/media/MediaPlayer;->setDataSource(Ljava/io/FileDescriptor;JJ)V")
            appendLine("const v${b + 4}, $volStr")
            appendLine("invoke-virtual/range {v${b + 3} .. v${b + 4}}, Landroid/media/MediaPlayer;->setVolume(FF)V")
            appendLine("invoke-virtual/range {v${b + 3}}, Landroid/media/MediaPlayer;->prepare()V")
            appendLine("invoke-virtual/range {v${b + 3}}, Landroid/media/MediaPlayer;->start()V")
            appendLine("const/4 v${b + 4}, 0x0")
            appendLine("invoke-virtual/range {v${b + 3} .. v${b + 4}}, Landroid/media/MediaPlayer;->setOnCompletionListener(Landroid/media/MediaPlayer\$OnCompletionListener;)V")
            appendLine(":nai64_sound_skip")
            appendLine("nop")
        }.toString()

        cloned.addInstructionsWithLabels(0, smali)
        mutableClass.methods.remove(onCreate)
        mutableClass.methods.add(cloned)

        logger.info("Startup sound injected into ${mutableClass.type}->onCreate, guarded by $guard, volume $vol%")
    }
}
