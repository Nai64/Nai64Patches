package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URL
import java.util.Base64
import java.util.logging.Logger
import javax.imageio.ImageIO
import org.w3c.dom.Element

private const val ICON_NAME = "ic_nai64_custom"

private val ICON_DENSITIES = listOf(
    "drawable-mdpi" to 48,
    "drawable-hdpi" to 72,
    "drawable-xhdpi" to 96,
    "drawable-xxhdpi" to 144,
    "drawable-xxxhdpi" to 192,
)

@Suppress("unused")
val customAppIconPatch = resourcePatch(
    name = "Custom App Icon",
    description = "Replaces the launcher icon with a custom image you supply (image URL, file path, or base64).",
    default = false,
) {
    val iconSource by stringOption(
        title = "Icon source",
        default = "",
        key = "customAppIconSource",
        description = "Image source: an http(s) URL, a local file path, or base64 " +
            "(optionally prefixed with 'data:image/png;base64,'). Leave empty to skip.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val src = (iconSource ?: "").trim()
        if (src.isEmpty()) {
            logger.warning("No icon source provided. Skipping.")
            return@execute
        }

        val bytes = resolveIconBytes(src, logger)
        if (bytes == null) {
            logger.warning("Could not load an image from the provided source. Skipping.")
            return@execute
        }

        val original = ImageIO.read(ByteArrayInputStream(bytes))
        if (original == null) {
            logger.warning("The source is not a decodable image. Skipping.")
            return@execute
        }

        var written = 0
        for ((dir, size) in ICON_DENSITIES) {
            val scaled = scaleImage(original, size)
            val outDir = get("res/$dir", true)
            outDir.mkdirs()
            ImageIO.write(scaled, "png", File(outDir, "$ICON_NAME.png"))
            written++
        }

        var replaced = 0
        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.documentElement.applicationOrNull()

            fun Element.replaceIcons() {
                setAttributeNS(NS_ANDROID, "android:icon", "@drawable/$ICON_NAME")
                replaced++
                if (hasAttributeNS(NS_ANDROID, "roundIcon")) {
                    setAttributeNS(NS_ANDROID, "android:roundIcon", "@drawable/$ICON_NAME")
                    replaced++
                }
            }

            application?.replaceIcons()
            for (tag in listOf("activity", "activity-alias")) {
                val nodes = manifest.getElementsByTagName(tag)
                for (i in 0 until nodes.length) {
                    (nodes.item(i) as? Element)?.replaceIcons()
                }
            }
        }

        logger.info("Wrote $written icon density bucket(s) and replaced $replaced icon reference(s).")
    }
}

private fun resolveIconBytes(src: String, logger: Logger): ByteArray? = runCatching {
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

private fun scaleImage(src: BufferedImage, size: Int): BufferedImage {
    val scale = size.toDouble() / maxOf(src.width, src.height)
    val w = (src.width * scale).toInt().coerceAtLeast(1)
    val h = (src.height * scale).toInt().coerceAtLeast(1)
    val out = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g = out.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g.drawImage(src, (size - w) / 2, (size - h) / 2, w, h, null)
    g.dispose()
    return out
}
