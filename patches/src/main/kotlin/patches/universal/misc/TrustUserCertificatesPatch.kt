package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

private const val TRUST_MANAGER = "Ljavax/net/ssl/X509TrustManager;"

@Suppress("unused")
val trustUserCertificatesPatch = bytecodePatch(
    name = "Trust User Certificates",
    description =
        "No-ops the check methods of app-bundled X509TrustManager implementations so custom " +
            "certificate validation (including pinning reimplemented by hand) accepts anything",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val checkMethods = setOf("checkClientTrusted", "checkServerTrusted")

        var patchedClasses = 0
        var patchedMethods = 0
        classDefForEach { classDef ->
            // Only classes that directly implement the trust manager interface.
            if (classDef.interfaces.none { it == TRUST_MANAGER }) return@classDefForEach

            val mutableClass = mutableClassDefBy(classDef)
            var changed = false
            for (method in mutableClass.methods) {
                // Both check methods are void; an immediate return accepts every chain.
                if (method.returnType != "V") continue
                if (method.name !in checkMethods) continue

                method.addInstruction(0, "return-void")
                changed = true
                patchedMethods++
            }
            if (changed) patchedClasses++
        }

        if (patchedMethods > 0) {
            logger.info(
                "Disabled $patchedMethods trust check(s) in $patchedClasses trust manager(s)",
            )
        } else {
            logger.warning("No bundled X509TrustManager implementations found. No changes applied.")
        }
    }
}
