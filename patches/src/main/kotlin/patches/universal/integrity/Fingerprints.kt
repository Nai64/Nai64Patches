package patches.universal.integrity

import app.morphe.patcher.Fingerprint

const val IntegrityTokenResponseDescriptor = "Lcom/google/android/play/core/integrity/IntegrityTokenResponse;"

const val StandardIntegrityTokenResponseDescriptor = "Lcom/google/android/play/core/integrity/StandardIntegrityTokenResponse;"

internal object IntegrityTokenResponseFingerprint : Fingerprint(
    name = "integrityToken",
    returnType = "Ljava/lang/String;",
    custom = { method, classDef ->
        classDef.interfaces.contains(IntegrityTokenResponseDescriptor)
    },
)

internal object StandardIntegrityTokenResponseFingerprint : Fingerprint(
    name = "token",
    returnType = "Ljava/lang/String;",
    custom = { method, classDef ->
        classDef.interfaces.contains(StandardIntegrityTokenResponseDescriptor)
    },
)