package patches.universal.manifest

import org.w3c.dom.Element

internal const val NS_ANDROID = "http://schemas.android.com/apk/res/android"

/** Returns the <application> element of the given document root, or null. */
internal fun Element?.applicationOrNull(): Element? =
    this?.getElementsByTagName("application")?.item(0) as? Element
