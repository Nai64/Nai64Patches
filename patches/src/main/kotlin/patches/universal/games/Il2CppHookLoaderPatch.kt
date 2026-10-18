package patches.universal.games

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import patches.universal.ads.util.cloneParameters
import java.util.logging.Logger

/**
 * Loads the IL2CPP hook library at startup.
 *
 * A null name is deliberate: the patch list generator drops every unnamed
 * patch, so this ships inside the bundle without showing up as a selectable
 * entry in Morphe Manager. Game patches reach it through `dependsOn`, which
 * references the object directly and does not care about the name. On its own
 * it does nothing - it ships no library and no rules.
 */
@Suppress("unused")
val il2cppHookLoaderPatch = bytecodePatch(
    name = null,
    description = "Internal loader for the IL2CPP hook library. Enabled by the game patches that use it.",
    default = false,
) {
    category("Scripts")
    val loadOnEveryActivity by booleanOption(
        key = "loadOnEveryActivity",
        default = false,
        title = "Load on every activity",
        description = "Also load from activities other than the launcher, for games that start a second activity first.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val matches: List<app.morphe.patcher.Match> = try {
            with(this@execute) { activityOnCreateFingerprint.matchAll() }
        } catch (_: Exception) {
            emptyList()
        }
        if (matches.isEmpty()) {
            logger.warning("IL2CPP Hook Loader: no Activity.onCreate(Bundle) found")
            return@execute
        }

        // The launcher is patched first and, by default, alone: the loader
        // guards itself with a one-shot flag, so extra call sites cost nothing
        // but are only useful if the game opens a different activity first.
        val ordered = matches.sortedByDescending { isLauncherActivity(it.method.definingClass) }
        var patched = 0
        for ((index, match) in ordered.withIndex()) {
            if (index > 0 && loadOnEveryActivity != true) continue
            val method = match.method
            if (method.implementation == null) continue
            val applied = try {
                // Grows the frame and preserves the incoming parameters into
                // low v-registers, so the injected block below can use a
                // scratch register without running past the frame.
                method.cloneParameters().addInstructions(0, LOAD_BLOCK)
                true
            } catch (e: Exception) {
                logger.warning("IL2CPP Hook Loader: failed on ${method.definingClass} - ${e.message}")
                false
            }
            if (applied) {
                patched++
                logger.info("IL2CPP Hook Loader: loading hook from ${method.definingClass}")
            }
        }
        if (patched == 0) logger.warning("IL2CPP Hook Loader: no activity could be patched")
    }
}

private val activityOnCreateFingerprint = Fingerprint(
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    custom = { _, type ->
        // Framework and UI-framework activities are never the game's launcher
        // here, and patching them risks a very early call into a library that
        // may not be loaded yet.
        type.startsWith("L") &&
            !type.startsWith("Landroid/") &&
            !type.startsWith("Landroidx/") &&
            !type.startsWith("Lcom/google/android/gms/ads/")
    },
)

/**
 * Prefers the Unity player activity.
 *
 * The manifest can name the launcher anything at all, and this patch type
 * cannot read the manifest, so recognition is by class name. Unity ships
 * `unityplayer.UnityActivity` in some titles and the stock
 * `com.unity3d.player.UnityPlayerActivity` in others; both are checked before
 * falling back to declaration order, which the log makes visible.
 */
private fun isLauncherActivity(type: String): Boolean = when (type) {
    "Lunityplayer/UnityActivity;",
    "Lcom/unity3d/player/UnityPlayerActivity;",
    -> true

    else -> false
}

/**
 * Injected block.
 *
 * Copies `this` into a scratch register with the from16 form before using it:
 * morphe's inline smali cannot place a p-register in a non-first operand
 * position in a large frame, and drops the line silently. The label keeps the
 * block self-contained in a method that already owns one.
 */
private const val LOAD_BLOCK = """
    :morphe_il2cpp_load
    move-object/from16 v0, p0
    invoke-static {v0}, Lnai64/universaloverlay/Il2CppHookLoader;->install(Landroid/content/Context;)V
    :morphe_il2cpp_load_done
"""