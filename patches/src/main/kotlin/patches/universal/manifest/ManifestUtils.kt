package patches.universal.manifest

import org.w3c.dom.Document
import org.w3c.dom.Element

internal const val NS_ANDROID = "http://schemas.android.com/apk/res/android"

/** Returns the <application> element of the given document root, or null. */
internal fun Element?.applicationOrNull(): Element? =
    this?.getElementsByTagName("application")?.item(0) as? Element

/** Sets an existing theme item or appends a new one. Shared by UI/manifest theme patches. */
internal fun ensureThemeItem(doc: Document, style: Element, attrName: String, value: String) {
    val items = style.getElementsByTagName("item")
    for (i in 0 until items.length) {
        val item = items.item(i) as? Element ?: continue
        if (item.getAttribute("name") == attrName) {
            item.textContent = value
            return
        }
    }
    val item = doc.createElement("item")
    item.setAttribute("name", attrName)
    item.textContent = value
    style.appendChild(item)
}
