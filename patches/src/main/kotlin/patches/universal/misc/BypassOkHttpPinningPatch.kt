package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

/** OkHttp2/OkHttp3 CertificatePinner variants. */
private val pinnerClasses = listOf(
    "Lokhttp3/CertificatePinner;",
    "Lcom/squareup/okhttp/CertificatePinner;",
)

@Suppress("unused")
val bypassOkHttpPinningPatch = bytecodePatch(
    name = "Bypass OkHttp Pinning",
    description =
        "No-ops the certificate verification entry points of bundled OkHttp clients so " +
            "certificate pinning never rejects connections",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        // The $ must be escaped: without it Kotlin treats "$okhttp" as a template.
        val checkMethods = setOf("check", "check\$okhttp")

        var patched = 0
        classDefForEach { classDef ->
            if (pinnerClasses.none { classDef.type.startsWith(it) }) return@classDefForEach

            val mutableClass = mutableClassDefBy(classDef)
            for (method in mutableClass.methods) {
                // Only void check methods are touched; they throw instead of returning values,
                // so an immediate return disables rejection entirely.
                if (method.returnType != "V") continue
                if (method.name !in checkMethods) continue

                method.addInstruction(0, "return-void")
                patched++
            }
        }

        if (patched > 0) {
            logger.info("Disabled $patched OkHttp pin check(s)")
        } else {
            logger.warning("No OkHttp CertificatePinner found. No changes applied.")
        }
    }
}
