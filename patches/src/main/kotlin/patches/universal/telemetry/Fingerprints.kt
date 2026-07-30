package patches.universal.telemetry

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object FirebaseInitializeFingerprint : Fingerprint(
    definingClass = "Lcom/google/firebase/FirebaseApp;",
    name = "initializeApp",
    returnType = "Lcom/google/firebase/FirebaseApp;",
    parameters = listOf("Landroid/content/Context;"),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
)

internal object FirebaseAnalyticsLogEventFingerprint : Fingerprint(
    definingClass = "Lcom/google/firebase/analytics/FirebaseAnalytics;",
    name = "logEvent",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Landroid/os/Bundle;"),
)

internal object FirebaseCrashlyticsInitFingerprint : Fingerprint(
    definingClass = "Lcom/google/firebase/crashlytics/FirebaseCrashlytics;",
    name = "init",
    returnType = "V",
    parameters = emptyList(),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
)

internal object AppsFlyerStartFingerprint : Fingerprint(
    definingClass = "Lcom/appsflyer/AppsFlyerLib;",
    name = "start",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
)

internal object AppsFlyerLogEventFingerprint : Fingerprint(
    definingClass = "Lcom/appsflyer/AppsFlyerLib;",
    name = "logEvent",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;", "Ljava/util/Map;"),
)

internal object AdjustOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/adjust/sdk/Adjust;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Lcom/adjust/sdk/AdjustConfig;"),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
)

internal object AdjustTrackEventFingerprint : Fingerprint(
    definingClass = "Lcom/adjust/sdk/Adjust;",
    name = "trackEvent",
    returnType = "V",
    parameters = listOf("Lcom/adjust/sdk/AdjustEvent;"),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
)

internal object BranchInitFingerprint : Fingerprint(
    definingClass = "Lio/branch/referral/Branch;",
    name = "init",
    returnType = "Lio/branch/referral/Branch;",
    parameters = listOf("Landroid/content/Context;"),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
)

internal object AmplitudeLogEventFingerprint : Fingerprint(
    definingClass = "Lcom/amplitude/api/Amplitude;",
    name = "logEvent",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
)

internal object MixpanelTrackFingerprint : Fingerprint(
    definingClass = "Lcom/mixpanel/android/mpmetrics/MixpanelAPI;",
    name = "track",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Lorg/json/JSONObject;"),
)

internal object CleverTapPushEventFingerprint : Fingerprint(
    definingClass = "Lcom/clevertap/android/sdk/CleverTapAPI;",
    name = "pushEvent",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
)

internal object SegmentTrackFingerprint : Fingerprint(
    definingClass = "Lcom/segment/analytics/Analytics;",
    name = "track",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Lcom/segment/analytics/Properties;"),
)

internal object FacebookLogEventFingerprint : Fingerprint(
    definingClass = "Lcom/facebook/appevents/AppEventsLogger;",
    name = "logEvent",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Landroid/os/Bundle;"),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
)

internal object UnityAnalyticsTransactionFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/analytics/UnityAnalytics;",
    name = "Transaction",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Ljava/math/BigDecimal;", "Ljava/util/Currency;", "Ljava/lang/String;", "Ljava/lang/Boolean;"),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
)

internal object FlurryLogEventFingerprint : Fingerprint(
    definingClass = "Lcom/flurry/android/Flurry;",
    name = "logEvent",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Landroid/os/Bundle;"),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
)
