package patches.universal.ui

/**
 * Shared plumbing for the startup toast/dialog patches.
 *
 * Injections are written into cloned methods that received extra registers
 * ([patches.universal.ads.util.cloneMutable]), so every temporary register is
 * guaranteed valid regardless of the original register layout. The context
 * register (`p0`) keeps its original absolute number and stays untouched.
 */
internal object StartupHooks {
    /** Name of the static boolean guard field added to the Application class. */
    const val GUARD_FIELD = "nai64StartupShown"

    fun escapeSmali(value: String): String =
        value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r\n", "\\n")
            .replace("\n", "\\n")
            .replace("\r", "\\n")
            .replace("\t", "\\t")
}
