package patches.universal.category

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import patches.universal.misc.*
import patches.universal.notifications.*
import patches.universal.privacy.*
import patches.universal.review.*
import patches.universal.splash.*
import patches.universal.ui.*
import java.util.logging.Logger

@Suppress("unused")
val systemAndDeviceTweaksPatch = bytecodePatch(
    name = "System & Device Tweaks",
    description = "Grouped settings for small standalone patches. Every option is disabled by default.",
    default = false,
) {
    val allowBackgroundActivityEnabled by booleanOption(
        key = "allowBackgroundActivityEnabled",
        default = false,
        title = "Allow Background Activity",
        description = "Apply the Allow Background Activity patch.",
    )
    val allowMixedContentEnabled by booleanOption(
        key = "allowMixedContentEnabled",
        default = false,
        title = "Allow Mixed Content",
        description = "Apply the Allow Mixed Content patch.",
    )
    val allowTextSelectionEnabled by booleanOption(
        key = "allowTextSelectionEnabled",
        default = false,
        title = "Allow Text Selection",
        description = "Apply the Allow Text Selection patch.",
    )
    val allowWebviewAutoplayEnabled by booleanOption(
        key = "allowWebviewAutoplayEnabled",
        default = false,
        title = "Allow WebView Autoplay",
        description = "Apply the Allow WebView Autoplay patch.",
    )
    val allowWebviewFileAccessEnabled by booleanOption(
        key = "allowWebviewFileAccessEnabled",
        default = false,
        title = "Allow WebView File Access",
        description = "Apply the Allow WebView File Access patch.",
    )
    val alwaysAllowBiometricsEnabled by booleanOption(
        key = "alwaysAllowBiometricsEnabled",
        default = false,
        title = "Always Allow Biometrics",
        description = "Apply the Always Allow Biometrics patch.",
    )
    val bypassDemoUserEnabled by booleanOption(
        key = "bypassDemoUserEnabled",
        default = false,
        title = "Bypass Demo User",
        description = "Apply the Bypass Demo User patch.",
    )
    val bypassDndPolicyAccessEnabled by booleanOption(
        key = "bypassDndPolicyAccessEnabled",
        default = false,
        title = "Bypass DND Policy Access",
        description = "Apply the Bypass DND Policy Access patch.",
    )
    val bypassGuestUserEnabled by booleanOption(
        key = "bypassGuestUserEnabled",
        default = false,
        title = "Bypass Guest User",
        description = "Apply the Bypass Guest User patch.",
    )
    val bypassHostnameVerificationEnabled by booleanOption(
        key = "bypassHostnameVerificationEnabled",
        default = false,
        title = "Bypass Hostname Verification",
        description = "Apply the Bypass Hostname Verification patch.",
    )
    val bypassInstantAppEnabled by booleanOption(
        key = "bypassInstantAppEnabled",
        default = false,
        title = "Bypass Instant App",
        description = "Apply the Bypass Instant App patch.",
    )
    val bypassKeyguardSecureEnabled by booleanOption(
        key = "bypassKeyguardSecureEnabled",
        default = false,
        title = "Bypass Keyguard Secure",
        description = "Apply the Bypass Keyguard Secure patch.",
    )
    val bypassLinkedUserEnabled by booleanOption(
        key = "bypassLinkedUserEnabled",
        default = false,
        title = "Bypass Linked User",
        description = "Apply the Bypass Linked User patch.",
    )
    val bypassLockTaskModeEnabled by booleanOption(
        key = "bypassLockTaskModeEnabled",
        default = false,
        title = "Bypass Lock Task Mode",
        description = "Apply the Bypass Lock Task Mode patch.",
    )
    val bypassLowEndDeviceEnabled by booleanOption(
        key = "bypassLowEndDeviceEnabled",
        default = false,
        title = "Bypass Low-End Device",
        description = "Apply the Bypass Low-End Device patch.",
    )
    val bypassManagedProfileEnabled by booleanOption(
        key = "bypassManagedProfileEnabled",
        default = false,
        title = "Bypass Managed Profile",
        description = "Apply the Bypass Managed Profile patch.",
    )
    val bypassOkhttpPinningEnabled by booleanOption(
        key = "bypassOkhttpPinningEnabled",
        default = false,
        title = "Bypass OkHttp Pinning",
        description = "Apply the Bypass OkHttp Pinning patch.",
    )
    val bypassOverlayDetectionEnabled by booleanOption(
        key = "bypassOverlayDetectionEnabled",
        default = false,
        title = "Bypass Overlay Detection",
        description = "Apply the Bypass Overlay Detection patch.",
    )
    val bypassPackageSuspendedEnabled by booleanOption(
        key = "bypassPackageSuspendedEnabled",
        default = false,
        title = "Bypass Package Suspended",
        description = "Apply the Bypass Package Suspended patch.",
    )
    val bypassPictureInPictureModeEnabled by booleanOption(
        key = "bypassPictureInPictureModeEnabled",
        default = false,
        title = "Bypass Picture-in-Picture Mode",
        description = "Apply the Bypass Picture-in-Picture Mode patch.",
    )
    val bypassSafeModeEnabled by booleanOption(
        key = "bypassSafeModeEnabled",
        default = false,
        title = "Bypass Safe Mode",
        description = "Apply the Bypass Safe Mode patch.",
    )
    val bypassSystemUserEnabled by booleanOption(
        key = "bypassSystemUserEnabled",
        default = false,
        title = "Bypass System User",
        description = "Apply the Bypass System User patch.",
    )
    val bypassTestEnvironmentEnabled by booleanOption(
        key = "bypassTestEnvironmentEnabled",
        default = false,
        title = "Bypass Test Environment",
        description = "Apply the Bypass Test Environment patch.",
    )
    val bypassUserRestrictedEnabled by booleanOption(
        key = "bypassUserRestrictedEnabled",
        default = false,
        title = "Bypass User Restricted",
        description = "Apply the Bypass User Restricted patch.",
    )
    val bypassVpnDetectionEnabled by booleanOption(
        key = "bypassVpnDetectionEnabled",
        default = false,
        title = "Bypass VPN Detection",
        description = "Apply the Bypass VPN Detection patch.",
    )
    val bypassWebviewSafeBrowsingEnabled by booleanOption(
        key = "bypassWebviewSafeBrowsingEnabled",
        default = false,
        title = "Bypass WebView Safe Browsing",
        description = "Apply the Bypass WebView Safe Browsing patch.",
    )
    val bypassWebviewSslErrorsEnabled by booleanOption(
        key = "bypassWebviewSslErrorsEnabled",
        default = false,
        title = "Bypass WebView SSL Errors",
        description = "Apply the Bypass WebView SSL Errors patch.",
    )
    val disableActivityTransitionsEnabled by booleanOption(
        key = "disableActivityTransitionsEnabled",
        default = false,
        title = "Disable Activity Transitions",
        description = "Apply the Disable Activity Transitions patch.",
    )
    val disableAnalyticsEventsEnabled by booleanOption(
        key = "disableAnalyticsEventsEnabled",
        default = false,
        title = "Disable Analytics Events",
        description = "Apply the Disable Analytics Events patch.",
    )
    val disableAnimationsEnabled by booleanOption(
        key = "disableAnimationsEnabled",
        default = false,
        title = "Disable Animations",
        description = "Apply the Disable Animations patch.",
    )
    val disableBackgroundSyncEnabled by booleanOption(
        key = "disableBackgroundSyncEnabled",
        default = false,
        title = "Disable Background Sync",
        description = "Apply the Disable Background Sync patch.",
    )
    val disableBluetoothA2dpEnabled by booleanOption(
        key = "disableBluetoothA2dpEnabled",
        default = false,
        title = "Disable Bluetooth A2DP",
        description = "Apply the Disable Bluetooth A2DP patch.",
    )
    val disableBluetoothDiscoveringEnabled by booleanOption(
        key = "disableBluetoothDiscoveringEnabled",
        default = false,
        title = "Disable Bluetooth Discovering",
        description = "Apply the Disable Bluetooth Discovering patch.",
    )
    val disableBluetoothScoEnabled by booleanOption(
        key = "disableBluetoothScoEnabled",
        default = false,
        title = "Disable Bluetooth SCO",
        description = "Apply the Disable Bluetooth SCO patch.",
    )
    val disableCameraShutterSoundEnabled by booleanOption(
        key = "disableCameraShutterSoundEnabled",
        default = false,
        title = "Disable Camera Shutter Sound",
        description = "Apply the Disable Camera Shutter Sound patch.",
    )
    val disableClipboardWriteEnabled by booleanOption(
        key = "disableClipboardWriteEnabled",
        default = false,
        title = "Disable Clipboard Write",
        description = "Apply the Disable Clipboard Write patch.",
    )
    val disableFixedVolumeEnabled by booleanOption(
        key = "disableFixedVolumeEnabled",
        default = false,
        title = "Disable Fixed Volume",
        description = "Apply the Disable Fixed Volume patch.",
    )
    val disableHapticFeedbackEnabled by booleanOption(
        key = "disableHapticFeedbackEnabled",
        default = false,
        title = "Disable Haptic Feedback",
        description = "Apply the Disable Haptic Feedback patch.",
    )
    val disableHeadsUpNotificationsEnabled by booleanOption(
        key = "disableHeadsUpNotificationsEnabled",
        default = false,
        title = "Disable Heads-up Notifications",
        description = "Apply the Disable Heads-up Notifications patch.",
    )
    val disableHighTextContrastEnabled by booleanOption(
        key = "disableHighTextContrastEnabled",
        default = false,
        title = "Disable High Text Contrast",
        description = "Apply the Disable High Text Contrast patch.",
    )
    val disableKeyboardSoundEnabled by booleanOption(
        key = "disableKeyboardSoundEnabled",
        default = false,
        title = "Disable Keyboard Sound",
        description = "Apply the Disable Keyboard Sound patch.",
    )
    val disableLocationRequestsEnabled by booleanOption(
        key = "disableLocationRequestsEnabled",
        default = false,
        title = "Disable Location Requests",
        description = "Apply the Disable Location Requests patch.",
    )
    val disableMusicDetectionEnabled by booleanOption(
        key = "disableMusicDetectionEnabled",
        default = false,
        title = "Disable Music Detection",
        description = "Apply the Disable Music Detection patch.",
    )
    val disableNotificationSoundEnabled by booleanOption(
        key = "disableNotificationSoundEnabled",
        default = false,
        title = "Disable Notification Sound",
        description = "Apply the Disable Notification Sound patch.",
    )
    val disableNotificationVibrationEnabled by booleanOption(
        key = "disableNotificationVibrationEnabled",
        default = false,
        title = "Disable Notification Vibration",
        description = "Apply the Disable Notification Vibration patch.",
    )
    val disableNotificationsEnabled by booleanOption(
        key = "disableNotificationsEnabled",
        default = false,
        title = "Disable Notifications",
        description = "Apply the Disable Notifications patch.",
    )
    val disableOrientationLockEnabled by booleanOption(
        key = "disableOrientationLockEnabled",
        default = false,
        title = "Disable Orientation Lock",
        description = "Apply the Disable Orientation Lock patch.",
    )
    val disableOverscrollEffectEnabled by booleanOption(
        key = "disableOverscrollEffectEnabled",
        default = false,
        title = "Disable Overscroll Effect",
        description = "Apply the Disable Overscroll Effect patch.",
    )
    val disableQuietModeEnabled by booleanOption(
        key = "disableQuietModeEnabled",
        default = false,
        title = "Disable Quiet Mode",
        description = "Apply the Disable Quiet Mode patch.",
    )
    val disableRttEnabled by booleanOption(
        key = "disableRttEnabled",
        default = false,
        title = "Disable RTT",
        description = "Apply the Disable RTT patch.",
    )
    val disableScrollbarsEnabled by booleanOption(
        key = "disableScrollbarsEnabled",
        default = false,
        title = "Disable Scrollbars",
        description = "Apply the Disable Scrollbars patch.",
    )
    val disableSecureSurfacesEnabled by booleanOption(
        key = "disableSecureSurfacesEnabled",
        default = false,
        title = "Disable Secure Surfaces",
        description = "Apply the Disable Secure Surfaces patch.",
    )
    val disableSensorsEnabled by booleanOption(
        key = "disableSensorsEnabled",
        default = false,
        title = "Disable Sensors",
        description = "Apply the Disable Sensors patch.",
    )
    val disableSnackbarsEnabled by booleanOption(
        key = "disableSnackbarsEnabled",
        default = false,
        title = "Disable Snackbars",
        description = "Apply the Disable Snackbars patch.",
    )
    val disableSoundEffectsEnabled by booleanOption(
        key = "disableSoundEffectsEnabled",
        default = false,
        title = "Disable Sound Effects",
        description = "Apply the Disable Sound Effects patch.",
    )
    val disableStrictmodeEnabled by booleanOption(
        key = "disableStrictmodeEnabled",
        default = false,
        title = "Disable StrictMode",
        description = "Apply the Disable StrictMode patch.",
    )
    val disableToastsEnabled by booleanOption(
        key = "disableToastsEnabled",
        default = false,
        title = "Disable Toasts",
        description = "Apply the Disable Toasts patch.",
    )
    val disableVibrationEnabled by booleanOption(
        key = "disableVibrationEnabled",
        default = false,
        title = "Disable Vibration",
        description = "Apply the Disable Vibration patch.",
    )
    val disableWakeLocksEnabled by booleanOption(
        key = "disableWakeLocksEnabled",
        default = false,
        title = "Disable Wake Locks",
        description = "Apply the Disable Wake Locks patch.",
    )
    val disableWebviewSafeBrowsingEnabled by booleanOption(
        key = "disableWebviewSafeBrowsingEnabled",
        default = false,
        title = "Disable WebView Safe Browsing",
        description = "Apply the Disable WebView Safe Browsing patch.",
    )
    val emptyClipboardReportEnabled by booleanOption(
        key = "emptyClipboardReportEnabled",
        default = false,
        title = "Empty Clipboard Report",
        description = "Apply the Empty Clipboard Report patch.",
    )
    val enableWebviewAppCacheEnabled by booleanOption(
        key = "enableWebviewAppCacheEnabled",
        default = false,
        title = "Enable WebView App Cache",
        description = "Apply the Enable WebView App Cache patch.",
    )
    val enableWebviewCacheEnabled by booleanOption(
        key = "enableWebviewCacheEnabled",
        default = false,
        title = "Enable WebView Cache",
        description = "Apply the Enable WebView Cache patch.",
    )
    val enableWebviewContentAccessEnabled by booleanOption(
        key = "enableWebviewContentAccessEnabled",
        default = false,
        title = "Enable WebView Content Access",
        description = "Apply the Enable WebView Content Access patch.",
    )
    val enableWebviewDebuggingEnabled by booleanOption(
        key = "enableWebviewDebuggingEnabled",
        default = false,
        title = "Enable WebView Debugging",
        description = "Apply the Enable WebView Debugging patch.",
    )
    val enableWebviewDomStorageEnabled by booleanOption(
        key = "enableWebviewDomStorageEnabled",
        default = false,
        title = "Enable WebView DOM Storage",
        description = "Apply the Enable WebView DOM Storage patch.",
    )
    val enableWebviewGeolocationEnabled by booleanOption(
        key = "enableWebviewGeolocationEnabled",
        default = false,
        title = "Enable WebView Geolocation",
        description = "Apply the Enable WebView Geolocation patch.",
    )
    val enableWebviewImageLoadingEnabled by booleanOption(
        key = "enableWebviewImageLoadingEnabled",
        default = false,
        title = "Enable WebView Image Loading",
        description = "Apply the Enable WebView Image Loading patch.",
    )
    val enableWebviewInitialFocusEnabled by booleanOption(
        key = "enableWebviewInitialFocusEnabled",
        default = false,
        title = "Enable WebView Initial Focus",
        description = "Apply the Enable WebView Initial Focus patch.",
    )
    val enableWebviewJavascriptEnabled by booleanOption(
        key = "enableWebviewJavascriptEnabled",
        default = false,
        title = "Enable WebView JavaScript",
        description = "Apply the Enable WebView JavaScript patch.",
    )
    val enableWebviewOffscreenPreRasterEnabled by booleanOption(
        key = "enableWebviewOffscreenPreRasterEnabled",
        default = false,
        title = "Enable WebView Offscreen Pre-Raster",
        description = "Apply the Enable WebView Offscreen Pre-Raster patch.",
    )
    val enableWebviewPopupsEnabled by booleanOption(
        key = "enableWebviewPopupsEnabled",
        default = false,
        title = "Enable WebView Popups",
        description = "Apply the Enable WebView Popups patch.",
    )
    val enableWebviewSaveFormDataEnabled by booleanOption(
        key = "enableWebviewSaveFormDataEnabled",
        default = false,
        title = "Enable WebView Save Form Data",
        description = "Apply the Enable WebView Save Form Data patch.",
    )
    val enableWebviewSavePasswordEnabled by booleanOption(
        key = "enableWebviewSavePasswordEnabled",
        default = false,
        title = "Enable WebView Save Password",
        description = "Apply the Enable WebView Save Password patch.",
    )
    val enableWebviewWideViewportEnabled by booleanOption(
        key = "enableWebviewWideViewportEnabled",
        default = false,
        title = "Enable WebView Wide Viewport",
        description = "Apply the Enable WebView Wide Viewport patch.",
    )
    val enableWebviewZoomEnabled by booleanOption(
        key = "enableWebviewZoomEnabled",
        default = false,
        title = "Enable WebView Zoom",
        description = "Apply the Enable WebView Zoom patch.",
    )
    val enableWebviewZoomSupportEnabled by booleanOption(
        key = "enableWebviewZoomSupportEnabled",
        default = false,
        title = "Enable WebView Zoom Support",
        description = "Apply the Enable WebView Zoom Support patch.",
    )
    val fakeBatteryWhitelistEnabled by booleanOption(
        key = "fakeBatteryWhitelistEnabled",
        default = false,
        title = "Fake Battery Whitelist",
        description = "Apply the Fake Battery Whitelist patch.",
    )
    val fakeBluetoothEnabledEnabled by booleanOption(
        key = "fakeBluetoothEnabledEnabled",
        default = false,
        title = "Fake Bluetooth Enabled",
        description = "Apply the Fake Bluetooth Enabled patch.",
    )
    val fakeFingerprintHardwareEnabled by booleanOption(
        key = "fakeFingerprintHardwareEnabled",
        default = false,
        title = "Fake Fingerprint Hardware",
        description = "Apply the Fake Fingerprint Hardware patch.",
    )
    val fakeNfcEnabledEnabled by booleanOption(
        key = "fakeNfcEnabledEnabled",
        default = false,
        title = "Fake NFC Enabled",
        description = "Apply the Fake NFC Enabled patch.",
    )
    val fakeOnlineStateEnabled by booleanOption(
        key = "fakeOnlineStateEnabled",
        default = false,
        title = "Fake Online State",
        description = "Apply the Fake Online State patch.",
    )
    val force5ghzBandSupportedEnabled by booleanOption(
        key = "force5ghzBandSupportedEnabled",
        default = false,
        title = "Force 5GHz Band Supported",
        description = "Apply the Force 5GHz Band Supported patch.",
    )
    val forceAndroidBeamEnabled by booleanOption(
        key = "forceAndroidBeamEnabled",
        default = false,
        title = "Force Android Beam",
        description = "Apply the Force Android Beam patch.",
    )
    val forceAppActiveEnabled by booleanOption(
        key = "forceAppActiveEnabled",
        default = false,
        title = "Force App Active",
        description = "Apply the Force App Active patch.",
    )
    val forceBatteryPresentEnabled by booleanOption(
        key = "forceBatteryPresentEnabled",
        default = false,
        title = "Force Battery Present",
        description = "Apply the Force Battery Present patch.",
    )
    val forceCanInstallPackagesEnabled by booleanOption(
        key = "forceCanInstallPackagesEnabled",
        default = false,
        title = "Force Can Install Packages",
        description = "Apply the Force Can Install Packages patch.",
    )
    val forceDataCapableEnabled by booleanOption(
        key = "forceDataCapableEnabled",
        default = false,
        title = "Force Data Capable",
        description = "Apply the Force Data Capable patch.",
    )
    val forceEmulatedStorageEnabled by booleanOption(
        key = "forceEmulatedStorageEnabled",
        default = false,
        title = "Force Emulated Storage",
        description = "Apply the Force Emulated Storage patch.",
    )
    val forceHapticsAvailableEnabled by booleanOption(
        key = "forceHapticsAvailableEnabled",
        default = false,
        title = "Force Haptics Available",
        description = "Apply the Force Haptics Available patch.",
    )
    val forceIdleCallStateEnabled by booleanOption(
        key = "forceIdleCallStateEnabled",
        default = false,
        title = "Force Idle Call State",
        description = "Apply the Force Idle Call State patch.",
    )
    val forceMaxBrightnessEnabled by booleanOption(
        key = "forceMaxBrightnessEnabled",
        default = false,
        title = "Force Max Brightness",
        description = "Apply the Force Max Brightness patch.",
    )
    val forceMicrophoneUnmutedEnabled by booleanOption(
        key = "forceMicrophoneUnmutedEnabled",
        default = false,
        title = "Force Microphone Unmuted",
        description = "Apply the Force Microphone Unmuted patch.",
    )
    val forceMultiSimEnabled by booleanOption(
        key = "forceMultiSimEnabled",
        default = false,
        title = "Force Multi-SIM",
        description = "Apply the Force Multi-SIM patch.",
    )
    val forceNormalAudioModeEnabled by booleanOption(
        key = "forceNormalAudioModeEnabled",
        default = false,
        title = "Force Normal Audio Mode",
        description = "Apply the Force Normal Audio Mode patch.",
    )
    val forceNotificationsEnabledEnabled by booleanOption(
        key = "forceNotificationsEnabledEnabled",
        default = false,
        title = "Force Notifications Enabled",
        description = "Apply the Force Notifications Enabled patch.",
    )
    val forceScreenInteractiveEnabled by booleanOption(
        key = "forceScreenInteractiveEnabled",
        default = false,
        title = "Force Screen Interactive",
        description = "Apply the Force Screen Interactive patch.",
    )
    val forceSimReadyEnabled by booleanOption(
        key = "forceSimReadyEnabled",
        default = false,
        title = "Force SIM Ready",
        description = "Apply the Force SIM Ready patch.",
    )
    val forceSinglePhoneEnabled by booleanOption(
        key = "forceSinglePhoneEnabled",
        default = false,
        title = "Force Single Phone",
        description = "Apply the Force Single Phone patch.",
    )
    val forceSingleSimEnabled by booleanOption(
        key = "forceSingleSimEnabled",
        default = false,
        title = "Force Single SIM",
        description = "Apply the Force Single SIM patch.",
    )
    val forceSmsCapableEnabled by booleanOption(
        key = "forceSmsCapableEnabled",
        default = false,
        title = "Force SMS Capable",
        description = "Apply the Force SMS Capable patch.",
    )
    val forceSpeakerphoneOffEnabled by booleanOption(
        key = "forceSpeakerphoneOffEnabled",
        default = false,
        title = "Force Speakerphone Off",
        description = "Apply the Force Speakerphone Off patch.",
    )
    val forceStorageNonRemovableEnabled by booleanOption(
        key = "forceStorageNonRemovableEnabled",
        default = false,
        title = "Force Storage Non-Removable",
        description = "Apply the Force Storage Non-Removable patch.",
    )
    val forceUserUnlockedEnabled by booleanOption(
        key = "forceUserUnlockedEnabled",
        default = false,
        title = "Force User Unlocked",
        description = "Apply the Force User Unlocked patch.",
    )
    val forceVoiceCapableEnabled by booleanOption(
        key = "forceVoiceCapableEnabled",
        default = false,
        title = "Force Voice Capable",
        description = "Apply the Force Voice Capable patch.",
    )
    val forceWifiP2pSupportedEnabled by booleanOption(
        key = "forceWifiP2pSupportedEnabled",
        default = false,
        title = "Force WiFi P2P Supported",
        description = "Apply the Force WiFi P2P Supported patch.",
    )
    val forceWifiScanAlwaysAvailableEnabled by booleanOption(
        key = "forceWifiScanAlwaysAvailableEnabled",
        default = false,
        title = "Force WiFi Scan Always Available",
        description = "Apply the Force WiFi Scan Always Available patch.",
    )
    val forceWorldPhoneEnabled by booleanOption(
        key = "forceWorldPhoneEnabled",
        default = false,
        title = "Force World Phone",
        description = "Apply the Force World Phone patch.",
    )
    val grantAllFilesAccessEnabled by booleanOption(
        key = "grantAllFilesAccessEnabled",
        default = false,
        title = "Grant All-Files Access",
        description = "Apply the Grant All-Files Access patch.",
    )
    val grantWebviewGeolocationEnabled by booleanOption(
        key = "grantWebviewGeolocationEnabled",
        default = false,
        title = "Grant WebView Geolocation",
        description = "Apply the Grant WebView Geolocation patch.",
    )
    val hideAccessibilityUsageEnabled by booleanOption(
        key = "hideAccessibilityUsageEnabled",
        default = false,
        title = "Hide Accessibility Usage",
        description = "Apply the Hide Accessibility Usage patch.",
    )
    val hideAccountsEnabled by booleanOption(
        key = "hideAccountsEnabled",
        default = false,
        title = "Hide Accounts",
        description = "Apply the Hide Accounts patch.",
    )
    val hideClipboardEnabled by booleanOption(
        key = "hideClipboardEnabled",
        default = false,
        title = "Hide Clipboard",
        description = "Apply the Hide Clipboard patch.",
    )
    val hideDebuggerConnectionEnabled by booleanOption(
        key = "hideDebuggerConnectionEnabled",
        default = false,
        title = "Hide Debugger Connection",
        description = "Apply the Hide Debugger Connection patch.",
    )
    val hideDeviceAdminsEnabled by booleanOption(
        key = "hideDeviceAdminsEnabled",
        default = false,
        title = "Hide Device Admins",
        description = "Apply the Hide Device Admins patch.",
    )
    val hideInstalledAppsEnabled by booleanOption(
        key = "hideInstalledAppsEnabled",
        default = false,
        title = "Hide Installed Apps",
        description = "Apply the Hide Installed Apps patch.",
    )
    val hideMockLocationEnabled by booleanOption(
        key = "hideMockLocationEnabled",
        default = false,
        title = "Hide Mock Location",
        description = "Apply the Hide Mock Location patch.",
    )
    val hideRoamingStatusEnabled by booleanOption(
        key = "hideRoamingStatusEnabled",
        default = false,
        title = "Hide Roaming Status",
        description = "Apply the Hide Roaming Status patch.",
    )
    val ignoreGpsDisabledEnabled by booleanOption(
        key = "ignoreGpsDisabledEnabled",
        default = false,
        title = "Ignore GPS Disabled",
        description = "Apply the Ignore GPS Disabled patch.",
    )
    val ignoreLocationServicesOffEnabled by booleanOption(
        key = "ignoreLocationServicesOffEnabled",
        default = false,
        title = "Ignore Location Services Off",
        description = "Apply the Ignore Location Services Off patch.",
    )
    val ignoreLowRamDeviceEnabled by booleanOption(
        key = "ignoreLowRamDeviceEnabled",
        default = false,
        title = "Ignore Low RAM Device",
        description = "Apply the Ignore Low RAM Device patch.",
    )
    val ignorePowerSaveModeEnabled by booleanOption(
        key = "ignorePowerSaveModeEnabled",
        default = false,
        title = "Ignore Power Save Mode",
        description = "Apply the Ignore Power Save Mode patch.",
    )
    val ignoreScreenLockEnabled by booleanOption(
        key = "ignoreScreenLockEnabled",
        default = false,
        title = "Ignore Screen Lock",
        description = "Apply the Ignore Screen Lock patch.",
    )
    val ignoreTouchExplorationEnabled by booleanOption(
        key = "ignoreTouchExplorationEnabled",
        default = false,
        title = "Ignore Touch Exploration",
        description = "Apply the Ignore Touch Exploration patch.",
    )
    val reportLegacyStorageEnabled by booleanOption(
        key = "reportLegacyStorageEnabled",
        default = false,
        title = "Report Legacy Storage",
        description = "Apply the Report Legacy Storage patch.",
    )
    val spoofAirplaneModeEnabled by booleanOption(
        key = "spoofAirplaneModeEnabled",
        default = false,
        title = "Spoof Airplane Mode",
        description = "Apply the Spoof Airplane Mode patch.",
    )
    val spoofBatteryChargingStateEnabled by booleanOption(
        key = "spoofBatteryChargingStateEnabled",
        default = false,
        title = "Spoof Battery Charging State",
        description = "Apply the Spoof Battery Charging State patch.",
    )
    val spoofBatteryLevelEnabled by booleanOption(
        key = "spoofBatteryLevelEnabled",
        default = false,
        title = "Spoof Battery Level",
        description = "Apply the Spoof Battery Level patch.",
    )
    val spoofBluetoothEnabledEnabled by booleanOption(
        key = "spoofBluetoothEnabledEnabled",
        default = false,
        title = "Spoof Bluetooth Enabled",
        description = "Apply the Spoof Bluetooth Enabled patch.",
    )
    val spoofBluetoothNameEnabled by booleanOption(
        key = "spoofBluetoothNameEnabled",
        default = false,
        title = "Spoof Bluetooth Name",
        description = "Apply the Spoof Bluetooth Name patch.",
    )
    val spoofBssidEnabled by booleanOption(
        key = "spoofBssidEnabled",
        default = false,
        title = "Spoof BSSID",
        description = "Apply the Spoof BSSID patch.",
    )
    val spoofBuildSerialEnabled by booleanOption(
        key = "spoofBuildSerialEnabled",
        default = false,
        title = "Spoof Build Serial",
        description = "Apply the Spoof Build Serial patch.",
    )
    val spoofDataStateEnabled by booleanOption(
        key = "spoofDataStateEnabled",
        default = false,
        title = "Spoof Data State",
        description = "Apply the Spoof Data State patch.",
    )
    val spoofDeveloperOptionsEnabled by booleanOption(
        key = "spoofDeveloperOptionsEnabled",
        default = false,
        title = "Spoof Developer Options",
        description = "Apply the Spoof Developer Options patch.",
    )
    val spoofDeviceSoftwareVersionEnabled by booleanOption(
        key = "spoofDeviceSoftwareVersionEnabled",
        default = false,
        title = "Spoof Device Software Version",
        description = "Apply the Spoof Device Software Version patch.",
    )
    val spoofGroupIdLevel1Enabled by booleanOption(
        key = "spoofGroupIdLevel1Enabled",
        default = false,
        title = "Spoof Group ID Level 1",
        description = "Apply the Spoof Group ID Level 1 patch.",
    )
    val spoofImeiEnabled by booleanOption(
        key = "spoofImeiEnabled",
        default = false,
        title = "Spoof IMEI",
        description = "Apply the Spoof IMEI patch.",
    )
    val spoofIsimImpiEnabled by booleanOption(
        key = "spoofIsimImpiEnabled",
        default = false,
        title = "Spoof ISIM IMPI",
        description = "Apply the Spoof ISIM IMPI patch.",
    )
    val spoofLastKnownLocationEnabled by booleanOption(
        key = "spoofLastKnownLocationEnabled",
        default = false,
        title = "Spoof Last Known Location",
        description = "Apply the Spoof Last Known Location patch.",
    )
    val spoofLocationProviderEnabled by booleanOption(
        key = "spoofLocationProviderEnabled",
        default = false,
        title = "Spoof Location Provider",
        description = "Apply the Spoof Location Provider patch.",
    )
    val spoofMacAddressEnabled by booleanOption(
        key = "spoofMacAddressEnabled",
        default = false,
        title = "Spoof MAC Address",
        description = "Apply the Spoof MAC Address patch.",
    )
    val spoofNaiEnabled by booleanOption(
        key = "spoofNaiEnabled",
        default = false,
        title = "Spoof NAI",
        description = "Apply the Spoof NAI patch.",
    )
    val spoofNetworkTypeLteEnabled by booleanOption(
        key = "spoofNetworkTypeLteEnabled",
        default = false,
        title = "Spoof Network Type LTE",
        description = "Apply the Spoof Network Type LTE patch.",
    )
    val spoofPhoneNumberEnabled by booleanOption(
        key = "spoofPhoneNumberEnabled",
        default = false,
        title = "Spoof Phone Number",
        description = "Apply the Spoof Phone Number patch.",
    )
    val spoofRingerModeEnabled by booleanOption(
        key = "spoofRingerModeEnabled",
        default = false,
        title = "Spoof Ringer Mode",
        description = "Apply the Spoof Ringer Mode patch.",
    )
    val spoofSensorListEnabled by booleanOption(
        key = "spoofSensorListEnabled",
        default = false,
        title = "Spoof Sensor List",
        description = "Apply the Spoof Sensor List patch.",
    )
    val spoofSignatureMatchEnabled by booleanOption(
        key = "spoofSignatureMatchEnabled",
        default = false,
        title = "Spoof Signature Match",
        description = "Apply the Spoof Signature Match patch.",
    )
    val spoofSimSerialNumberEnabled by booleanOption(
        key = "spoofSimSerialNumberEnabled",
        default = false,
        title = "Spoof SIM Serial Number",
        description = "Apply the Spoof SIM Serial Number patch.",
    )
    val spoofStorageStateEnabled by booleanOption(
        key = "spoofStorageStateEnabled",
        default = false,
        title = "Spoof Storage State",
        description = "Apply the Spoof Storage State patch.",
    )
    val spoofSubscriberIdEnabled by booleanOption(
        key = "spoofSubscriberIdEnabled",
        default = false,
        title = "Spoof Subscriber ID",
        description = "Apply the Spoof Subscriber ID patch.",
    )
    val spoofTimeZoneEnabled by booleanOption(
        key = "spoofTimeZoneEnabled",
        default = false,
        title = "Spoof Time Zone",
        description = "Apply the Spoof Time Zone patch.",
    )
    val spoofVoiceMailEnabled by booleanOption(
        key = "spoofVoiceMailEnabled",
        default = false,
        title = "Spoof Voice Mail",
        description = "Apply the Spoof Voice Mail patch.",
    )
    val spoofWebviewUserAgentEnabled by booleanOption(
        key = "spoofWebviewUserAgentEnabled",
        default = false,
        title = "Spoof WebView User Agent",
        description = "Apply the Spoof WebView User Agent patch.",
    )
    val spoofWifiEnabledEnabled by booleanOption(
        key = "spoofWifiEnabledEnabled",
        default = false,
        title = "Spoof WiFi Enabled",
        description = "Apply the Spoof WiFi Enabled patch.",
    )
    val spoofWifiRssiEnabled by booleanOption(
        key = "spoofWifiRssiEnabled",
        default = false,
        title = "Spoof WiFi RSSI",
        description = "Apply the Spoof WiFi RSSI patch.",
    )
    val spoofWifiSsidEnabled by booleanOption(
        key = "spoofWifiSsidEnabled",
        default = false,
        title = "Spoof WiFi SSID",
        description = "Apply the Spoof WiFi SSID patch.",
    )
    val spoofWiredHeadsetEnabled by booleanOption(
        key = "spoofWiredHeadsetEnabled",
        default = false,
        title = "Spoof Wired Headset",
        description = "Apply the Spoof Wired Headset patch.",
    )
    val treatScreenAsOnEnabled by booleanOption(
        key = "treatScreenAsOnEnabled",
        default = false,
        title = "Treat Screen as On",
        description = "Apply the Treat Screen as On patch.",
    )
    val trustUserCertificatesEnabled by booleanOption(
        key = "trustUserCertificatesEnabled",
        default = false,
        title = "Trust User Certificates",
        description = "Apply the Trust User Certificates patch.",
    )
    val treatNetworkAsUnmeteredEnabled by booleanOption(
        key = "treatNetworkAsUnmeteredEnabled",
        default = false,
        title = "Treat Network as Unmetered",
        description = "Apply the Treat Network as Unmetered patch.",
    )
    val onmessagereceivedEnabled by booleanOption(
        key = "onmessagereceivedEnabled",
        default = false,
        title = "onMessageReceived",
        description = "Apply the onMessageReceived patch.",
    )
    val blockScreenshotDetectionEnabled by booleanOption(
        key = "blockScreenshotDetectionEnabled",
        default = false,
        title = "Block Screenshot Detection",
        description = "Apply the Block Screenshot Detection patch.",
    )
    val getidEnabled by booleanOption(
        key = "getidEnabled",
        default = false,
        title = "getId",
        description = "Apply the getId patch.",
    )
    val requestreviewflowEnabled by booleanOption(
        key = "requestreviewflowEnabled",
        default = false,
        title = "requestReviewFlow",
        description = "Apply the requestReviewFlow patch.",
    )
    val oncreateEnabled by booleanOption(
        key = "oncreateEnabled",
        default = false,
        title = "onCreate",
        description = "Apply the onCreate patch.",
    )
    val allowScreenshotsEnabled by booleanOption(
        key = "allowScreenshotsEnabled",
        default = false,
        title = "Allow Screenshots",
        description = "Apply the Allow Screenshots patch.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var selected = 0
        if (allowBackgroundActivityEnabled == true) {
            allowBackgroundActivityPatch.execute(this)
            selected++
        }
        if (allowMixedContentEnabled == true) {
            allowMixedContentPatch.execute(this)
            selected++
        }
        if (allowTextSelectionEnabled == true) {
            allowTextSelectionPatch.execute(this)
            selected++
        }
        if (allowWebviewAutoplayEnabled == true) {
            allowWebViewAutoplayPatch.execute(this)
            selected++
        }
        if (allowWebviewFileAccessEnabled == true) {
            allowWebViewFileAccessPatch.execute(this)
            selected++
        }
        if (alwaysAllowBiometricsEnabled == true) {
            alwaysAllowBiometricsPatch.execute(this)
            selected++
        }
        if (bypassDemoUserEnabled == true) {
            bypassDemoUserPatch.execute(this)
            selected++
        }
        if (bypassDndPolicyAccessEnabled == true) {
            bypassDndPolicyAccessPatch.execute(this)
            selected++
        }
        if (bypassGuestUserEnabled == true) {
            bypassGuestUserPatch.execute(this)
            selected++
        }
        if (bypassHostnameVerificationEnabled == true) {
            bypassHostnameVerificationPatch.execute(this)
            selected++
        }
        if (bypassInstantAppEnabled == true) {
            bypassInstantAppPatch.execute(this)
            selected++
        }
        if (bypassKeyguardSecureEnabled == true) {
            bypassKeyguardSecurePatch.execute(this)
            selected++
        }
        if (bypassLinkedUserEnabled == true) {
            bypassLinkedUserPatch.execute(this)
            selected++
        }
        if (bypassLockTaskModeEnabled == true) {
            bypassLockTaskModePatch.execute(this)
            selected++
        }
        if (bypassLowEndDeviceEnabled == true) {
            bypassLowEndDevicePatch.execute(this)
            selected++
        }
        if (bypassManagedProfileEnabled == true) {
            bypassManagedProfilePatch.execute(this)
            selected++
        }
        if (bypassOkhttpPinningEnabled == true) {
            bypassOkHttpPinningPatch.execute(this)
            selected++
        }
        if (bypassOverlayDetectionEnabled == true) {
            bypassOverlayDetectionPatch.execute(this)
            selected++
        }
        if (bypassPackageSuspendedEnabled == true) {
            bypassPackageSuspendedPatch.execute(this)
            selected++
        }
        if (bypassPictureInPictureModeEnabled == true) {
            bypassPipModePatch.execute(this)
            selected++
        }
        if (bypassSafeModeEnabled == true) {
            bypassSafeModePatch.execute(this)
            selected++
        }
        if (bypassSystemUserEnabled == true) {
            bypassSystemUserPatch.execute(this)
            selected++
        }
        if (bypassTestEnvironmentEnabled == true) {
            bypassTestEnvironmentPatch.execute(this)
            selected++
        }
        if (bypassUserRestrictedEnabled == true) {
            bypassUserRestrictedPatch.execute(this)
            selected++
        }
        if (bypassVpnDetectionEnabled == true) {
            bypassVpnDetectionPatch.execute(this)
            selected++
        }
        if (bypassWebviewSafeBrowsingEnabled == true) {
            bypassWebViewSafeBrowsingPatch.execute(this)
            selected++
        }
        if (bypassWebviewSslErrorsEnabled == true) {
            bypassWebViewSslErrorsPatch.execute(this)
            selected++
        }
        if (disableActivityTransitionsEnabled == true) {
            disableActivityTransitionsPatch.execute(this)
            selected++
        }
        if (disableAnalyticsEventsEnabled == true) {
            disableAnalyticsEventsPatch.execute(this)
            selected++
        }
        if (disableAnimationsEnabled == true) {
            disableAnimationsPatch.execute(this)
            selected++
        }
        if (disableBackgroundSyncEnabled == true) {
            disableBackgroundSyncPatch.execute(this)
            selected++
        }
        if (disableBluetoothA2dpEnabled == true) {
            disableBluetoothA2dpPatch.execute(this)
            selected++
        }
        if (disableBluetoothDiscoveringEnabled == true) {
            disableBluetoothDiscoveringPatch.execute(this)
            selected++
        }
        if (disableBluetoothScoEnabled == true) {
            disableBluetoothScoPatch.execute(this)
            selected++
        }
        if (disableCameraShutterSoundEnabled == true) {
            disableCameraShutterSoundPatch.execute(this)
            selected++
        }
        if (disableClipboardWriteEnabled == true) {
            disableClipboardWritePatch.execute(this)
            selected++
        }
        if (disableFixedVolumeEnabled == true) {
            disableFixedVolumePatch.execute(this)
            selected++
        }
        if (disableHapticFeedbackEnabled == true) {
            disableHapticFeedbackPatch.execute(this)
            selected++
        }
        if (disableHeadsUpNotificationsEnabled == true) {
            disableHeadsUpNotificationsPatch.execute(this)
            selected++
        }
        if (disableHighTextContrastEnabled == true) {
            disableHighTextContrastPatch.execute(this)
            selected++
        }
        if (disableKeyboardSoundEnabled == true) {
            disableKeyboardSoundPatch.execute(this)
            selected++
        }
        if (disableLocationRequestsEnabled == true) {
            disableLocationRequestsPatch.execute(this)
            selected++
        }
        if (disableMusicDetectionEnabled == true) {
            disableMusicDetectionPatch.execute(this)
            selected++
        }
        if (disableNotificationSoundEnabled == true) {
            disableNotificationSoundPatch.execute(this)
            selected++
        }
        if (disableNotificationVibrationEnabled == true) {
            disableNotificationVibrationPatch.execute(this)
            selected++
        }
        if (disableNotificationsEnabled == true) {
            disableNotificationsPatch.execute(this)
            selected++
        }
        if (disableOrientationLockEnabled == true) {
            disableOrientationLockPatch.execute(this)
            selected++
        }
        if (disableOverscrollEffectEnabled == true) {
            disableOverscrollEffectPatch.execute(this)
            selected++
        }
        if (disableQuietModeEnabled == true) {
            disableQuietModePatch.execute(this)
            selected++
        }
        if (disableRttEnabled == true) {
            disableRttPatch.execute(this)
            selected++
        }
        if (disableScrollbarsEnabled == true) {
            disableScrollbarsPatch.execute(this)
            selected++
        }
        if (disableSecureSurfacesEnabled == true) {
            disableSecureSurfacesPatch.execute(this)
            selected++
        }
        if (disableSensorsEnabled == true) {
            disableSensorsPatch.execute(this)
            selected++
        }
        if (disableSnackbarsEnabled == true) {
            disableSnackbarsPatch.execute(this)
            selected++
        }
        if (disableSoundEffectsEnabled == true) {
            disableSoundEffectsPatch.execute(this)
            selected++
        }
        if (disableStrictmodeEnabled == true) {
            disableStrictModePatch.execute(this)
            selected++
        }
        if (disableToastsEnabled == true) {
            disableToastsPatch.execute(this)
            selected++
        }
        if (disableVibrationEnabled == true) {
            disableVibrationPatch.execute(this)
            selected++
        }
        if (disableWakeLocksEnabled == true) {
            disableWakeLocksPatch.execute(this)
            selected++
        }
        if (disableWebviewSafeBrowsingEnabled == true) {
            disableWebViewSafeBrowsingPatch.execute(this)
            selected++
        }
        if (emptyClipboardReportEnabled == true) {
            emptyClipboardReportPatch.execute(this)
            selected++
        }
        if (enableWebviewAppCacheEnabled == true) {
            enableWebViewAppCachePatch.execute(this)
            selected++
        }
        if (enableWebviewCacheEnabled == true) {
            enableWebViewCachePatch.execute(this)
            selected++
        }
        if (enableWebviewContentAccessEnabled == true) {
            enableWebViewContentAccessPatch.execute(this)
            selected++
        }
        if (enableWebviewDebuggingEnabled == true) {
            enableWebViewDebuggingPatch.execute(this)
            selected++
        }
        if (enableWebviewDomStorageEnabled == true) {
            enableWebViewDomStoragePatch.execute(this)
            selected++
        }
        if (enableWebviewGeolocationEnabled == true) {
            enableWebViewGeolocationPatch.execute(this)
            selected++
        }
        if (enableWebviewImageLoadingEnabled == true) {
            enableWebViewImageLoadingPatch.execute(this)
            selected++
        }
        if (enableWebviewInitialFocusEnabled == true) {
            enableWebViewInitialFocusPatch.execute(this)
            selected++
        }
        if (enableWebviewJavascriptEnabled == true) {
            enableWebViewJavaScriptPatch.execute(this)
            selected++
        }
        if (enableWebviewOffscreenPreRasterEnabled == true) {
            enableWebViewOffscreenPreRasterPatch.execute(this)
            selected++
        }
        if (enableWebviewPopupsEnabled == true) {
            enableWebViewPopupsPatch.execute(this)
            selected++
        }
        if (enableWebviewSaveFormDataEnabled == true) {
            enableWebViewSaveFormDataPatch.execute(this)
            selected++
        }
        if (enableWebviewSavePasswordEnabled == true) {
            enableWebViewSavePasswordPatch.execute(this)
            selected++
        }
        if (enableWebviewWideViewportEnabled == true) {
            enableWebViewWideViewportPatch.execute(this)
            selected++
        }
        if (enableWebviewZoomEnabled == true) {
            enableWebViewZoomPatch.execute(this)
            selected++
        }
        if (enableWebviewZoomSupportEnabled == true) {
            enableWebViewZoomSupportPatch.execute(this)
            selected++
        }
        if (fakeBatteryWhitelistEnabled == true) {
            fakeBatteryWhitelistPatch.execute(this)
            selected++
        }
        if (fakeBluetoothEnabledEnabled == true) {
            fakeBluetoothEnabledPatch.execute(this)
            selected++
        }
        if (fakeFingerprintHardwareEnabled == true) {
            fakeFingerprintHardwarePatch.execute(this)
            selected++
        }
        if (fakeNfcEnabledEnabled == true) {
            fakeNfcEnabledPatch.execute(this)
            selected++
        }
        if (fakeOnlineStateEnabled == true) {
            fakeOnlineStatePatch.execute(this)
            selected++
        }
        if (force5ghzBandSupportedEnabled == true) {
            force5GhzBandSupportedPatch.execute(this)
            selected++
        }
        if (forceAndroidBeamEnabled == true) {
            forceAndroidBeamPatch.execute(this)
            selected++
        }
        if (forceAppActiveEnabled == true) {
            forceAppActivePatch.execute(this)
            selected++
        }
        if (forceBatteryPresentEnabled == true) {
            forceBatteryPresentPatch.execute(this)
            selected++
        }
        if (forceCanInstallPackagesEnabled == true) {
            forceCanInstallPackagesPatch.execute(this)
            selected++
        }
        if (forceDataCapableEnabled == true) {
            forceDataCapablePatch.execute(this)
            selected++
        }
        if (forceEmulatedStorageEnabled == true) {
            forceEmulatedStoragePatch.execute(this)
            selected++
        }
        if (forceHapticsAvailableEnabled == true) {
            forceHapticsAvailablePatch.execute(this)
            selected++
        }
        if (forceIdleCallStateEnabled == true) {
            forceIdleCallStatePatch.execute(this)
            selected++
        }
        if (forceMaxBrightnessEnabled == true) {
            forceMaxBrightnessPatch.execute(this)
            selected++
        }
        if (forceMicrophoneUnmutedEnabled == true) {
            forceMicrophoneUnmutedPatch.execute(this)
            selected++
        }
        if (forceMultiSimEnabled == true) {
            forceMultiSimPatch.execute(this)
            selected++
        }
        if (forceNormalAudioModeEnabled == true) {
            forceNormalAudioModePatch.execute(this)
            selected++
        }
        if (forceNotificationsEnabledEnabled == true) {
            forceNotificationsEnabledPatch.execute(this)
            selected++
        }
        if (forceScreenInteractiveEnabled == true) {
            forceScreenInteractivePatch.execute(this)
            selected++
        }
        if (forceSimReadyEnabled == true) {
            forceSimReadyPatch.execute(this)
            selected++
        }
        if (forceSinglePhoneEnabled == true) {
            forceSinglePhonePatch.execute(this)
            selected++
        }
        if (forceSingleSimEnabled == true) {
            forceSingleSimPatch.execute(this)
            selected++
        }
        if (forceSmsCapableEnabled == true) {
            forceSmsCapablePatch.execute(this)
            selected++
        }
        if (forceSpeakerphoneOffEnabled == true) {
            forceSpeakerphoneOffPatch.execute(this)
            selected++
        }
        if (forceStorageNonRemovableEnabled == true) {
            forceStorageNonRemovablePatch.execute(this)
            selected++
        }
        if (forceUserUnlockedEnabled == true) {
            forceUserUnlockedPatch.execute(this)
            selected++
        }
        if (forceVoiceCapableEnabled == true) {
            forceVoiceCapablePatch.execute(this)
            selected++
        }
        if (forceWifiP2pSupportedEnabled == true) {
            forceWifiP2pSupportedPatch.execute(this)
            selected++
        }
        if (forceWifiScanAlwaysAvailableEnabled == true) {
            forceWifiScanAlwaysAvailablePatch.execute(this)
            selected++
        }
        if (forceWorldPhoneEnabled == true) {
            forceWorldPhonePatch.execute(this)
            selected++
        }
        if (grantAllFilesAccessEnabled == true) {
            grantAllFilesAccessPatch.execute(this)
            selected++
        }
        if (grantWebviewGeolocationEnabled == true) {
            grantWebViewGeolocationPatch.execute(this)
            selected++
        }
        if (hideAccessibilityUsageEnabled == true) {
            hideAccessibilityUsagePatch.execute(this)
            selected++
        }
        if (hideAccountsEnabled == true) {
            hideAccountsPatch.execute(this)
            selected++
        }
        if (hideClipboardEnabled == true) {
            hideClipboardPatch.execute(this)
            selected++
        }
        if (hideDebuggerConnectionEnabled == true) {
            hideDebuggerConnectionPatch.execute(this)
            selected++
        }
        if (hideDeviceAdminsEnabled == true) {
            hideDeviceAdminsPatch.execute(this)
            selected++
        }
        if (hideInstalledAppsEnabled == true) {
            hideInstalledAppsPatch.execute(this)
            selected++
        }
        if (hideMockLocationEnabled == true) {
            hideMockLocationPatch.execute(this)
            selected++
        }
        if (hideRoamingStatusEnabled == true) {
            hideRoamingStatusPatch.execute(this)
            selected++
        }
        if (ignoreGpsDisabledEnabled == true) {
            ignoreGpsDisabledPatch.execute(this)
            selected++
        }
        if (ignoreLocationServicesOffEnabled == true) {
            ignoreLocationServicesOffPatch.execute(this)
            selected++
        }
        if (ignoreLowRamDeviceEnabled == true) {
            ignoreLowRamDevicePatch.execute(this)
            selected++
        }
        if (ignorePowerSaveModeEnabled == true) {
            ignorePowerSaveModePatch.execute(this)
            selected++
        }
        if (ignoreScreenLockEnabled == true) {
            ignoreScreenLockPatch.execute(this)
            selected++
        }
        if (ignoreTouchExplorationEnabled == true) {
            ignoreTouchExplorationPatch.execute(this)
            selected++
        }
        if (reportLegacyStorageEnabled == true) {
            reportLegacyStoragePatch.execute(this)
            selected++
        }
        if (spoofAirplaneModeEnabled == true) {
            spoofAirplaneModePatch.execute(this)
            selected++
        }
        if (spoofBatteryChargingStateEnabled == true) {
            spoofBatteryChargingPatch.execute(this)
            selected++
        }
        if (spoofBatteryLevelEnabled == true) {
            spoofBatteryLevelPatch.execute(this)
            selected++
        }
        if (spoofBluetoothEnabledEnabled == true) {
            spoofBluetoothEnabledPatch.execute(this)
            selected++
        }
        if (spoofBluetoothNameEnabled == true) {
            spoofBluetoothNamePatch.execute(this)
            selected++
        }
        if (spoofBssidEnabled == true) {
            spoofBssidPatch.execute(this)
            selected++
        }
        if (spoofBuildSerialEnabled == true) {
            spoofBuildSerialPatch.execute(this)
            selected++
        }
        if (spoofDataStateEnabled == true) {
            spoofDataStatePatch.execute(this)
            selected++
        }
        if (spoofDeveloperOptionsEnabled == true) {
            spoofDeveloperOptionsPatch.execute(this)
            selected++
        }
        if (spoofDeviceSoftwareVersionEnabled == true) {
            spoofDeviceSoftwareVersionPatch.execute(this)
            selected++
        }
        if (spoofGroupIdLevel1Enabled == true) {
            spoofGroupIdLevel1Patch.execute(this)
            selected++
        }
        if (spoofImeiEnabled == true) {
            spoofImeiPatch.execute(this)
            selected++
        }
        if (spoofIsimImpiEnabled == true) {
            spoofIsimImpiPatch.execute(this)
            selected++
        }
        if (spoofLastKnownLocationEnabled == true) {
            spoofLastKnownLocationPatch.execute(this)
            selected++
        }
        if (spoofLocationProviderEnabled == true) {
            spoofLocationProviderPatch.execute(this)
            selected++
        }
        if (spoofMacAddressEnabled == true) {
            spoofMacAddressPatch.execute(this)
            selected++
        }
        if (spoofNaiEnabled == true) {
            spoofNaiPatch.execute(this)
            selected++
        }
        if (spoofNetworkTypeLteEnabled == true) {
            spoofNetworkTypeLtePatch.execute(this)
            selected++
        }
        if (spoofPhoneNumberEnabled == true) {
            spoofPhoneNumberPatch.execute(this)
            selected++
        }
        if (spoofRingerModeEnabled == true) {
            spoofRingerModePatch.execute(this)
            selected++
        }
        if (spoofSensorListEnabled == true) {
            spoofSensorListPatch.execute(this)
            selected++
        }
        if (spoofSignatureMatchEnabled == true) {
            spoofSignatureMatchPatch.execute(this)
            selected++
        }
        if (spoofSimSerialNumberEnabled == true) {
            spoofSimSerialPatch.execute(this)
            selected++
        }
        if (spoofStorageStateEnabled == true) {
            spoofStorageStatePatch.execute(this)
            selected++
        }
        if (spoofSubscriberIdEnabled == true) {
            spoofSubscriberIdPatch.execute(this)
            selected++
        }
        if (spoofTimeZoneEnabled == true) {
            spoofTimeZonePatch.execute(this)
            selected++
        }
        if (spoofVoiceMailEnabled == true) {
            spoofVoiceMailPatch.execute(this)
            selected++
        }
        if (spoofWebviewUserAgentEnabled == true) {
            spoofWebViewUserAgentPatch.execute(this)
            selected++
        }
        if (spoofWifiEnabledEnabled == true) {
            spoofWifiEnabledPatch.execute(this)
            selected++
        }
        if (spoofWifiRssiEnabled == true) {
            spoofWifiRssiPatch.execute(this)
            selected++
        }
        if (spoofWifiSsidEnabled == true) {
            spoofWifiSsidPatch.execute(this)
            selected++
        }
        if (spoofWiredHeadsetEnabled == true) {
            spoofWiredHeadsetPatch.execute(this)
            selected++
        }
        if (treatScreenAsOnEnabled == true) {
            treatScreenAsOnPatch.execute(this)
            selected++
        }
        if (trustUserCertificatesEnabled == true) {
            trustUserCertificatesPatch.execute(this)
            selected++
        }
        if (treatNetworkAsUnmeteredEnabled == true) {
            unmeteredNetworkPatch.execute(this)
            selected++
        }
        if (onmessagereceivedEnabled == true) {
            blockPushAdsPatch.execute(this)
            selected++
        }
        if (blockScreenshotDetectionEnabled == true) {
            blockScreenshotDetectionPatch.execute(this)
            selected++
        }
        if (getidEnabled == true) {
            limitAdTrackingPatch.execute(this)
            selected++
        }
        if (requestreviewflowEnabled == true) {
            skipRateUsPromptPatch.execute(this)
            selected++
        }
        if (oncreateEnabled == true) {
            skipSplashScreenPatch.execute(this)
            selected++
        }
        if (allowScreenshotsEnabled == true) {
            allowScreenshotsPatch.execute(this)
            selected++
        }
        if (selected == 0) {
            logger.info("System & Device Tweaks: no options enabled; no changes applied")
        }
    }
}
