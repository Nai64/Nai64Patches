package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.Base64
import java.util.logging.Logger
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

        // Try Android graphics first (when patch runs on Android), fallback to ImageIO (desktop)
        val originalBytesWithFallback = bytes
        var written = 0
        for ((dir, size) in ICON_DENSITIES) {
            val outDir = get("res/$dir", true)
            outDir.mkdirs()
            val outFile = File(outDir, "$ICON_NAME.png")
            val scaled = try {
                scaleImageWithAndroidOrAwt(originalBytesWithFallback, size, logger)
            } catch (e: Exception) {
                logger.warning("Failed to scale image for $dir (${e.message}), writing original bytes")
                null
            }
            if (scaled != null) {
                FileOutputStream(outFile).use { it.write(scaled) }
                written++
            } else {
                // Fallback: just write original bytes without scaling
                FileOutputStream(outFile).use { it.write(originalBytesWithFallback) }
                written++
            }
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

private fun scaleImageWithAndroidOrAwt(bytes: ByteArray, size: Int, logger: Logger): ByteArray? {
    // Try Android BitmapFactory/Canvas first
    try {
        val bitmapFactoryClass = Class.forName("android.graphics.BitmapFactory")
        val bitmapClass = Class.forName("android.graphics.Bitmap")
        val canvasClass = Class.forName("android.graphics.Canvas")
        val paintClass = Class.forName("android.graphics.Paint")

        val decodeMethod = bitmapFactoryClass.getMethod("decodeByteArray", ByteArray::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
        val original = decodeMethod.invoke(null, bytes, 0, bytes.size) ?: return null
        val getWidth = bitmapClass.getMethod("getWidth")
        val getHeight = bitmapClass.getMethod("getHeight")
        val width = getWidth.invoke(original) as Int
        val height = getHeight.invoke(original) as Int

        val scale = size.toDouble() / maxOf(width, height)
        val w = (width * scale).toInt().coerceAtLeast(1)
        val h = (height * scale).toInt().coerceAtLeast(1)

        val createBitmap = bitmapClass.getMethod("createBitmap", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Class.forName("android.graphics.Bitmap\$Config"))
        val configArgb8888 = Class.forName("android.graphics.Bitmap\$Config").getField("ARGB_8888").get(null)
        val out = createBitmap.invoke(null, size, size, configArgb8888)

        val canvasCtor = canvasClass.getConstructor(bitmapClass)
        val canvas = canvasCtor.newInstance(out)
        val paintCtor = paintClass.getConstructor()
        val paint = paintCtor.newInstance()
        val setFilter = paintClass.getMethod("setFilterBitmap", Boolean::class.javaPrimitiveType)
        setFilter.invoke(paint, true)

        val createScaled = bitmapClass.getMethod("createScaledBitmap", bitmapClass, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType)
        val scaledSrc = createScaled.invoke(null, original, w, h, true)

        val drawBitmap = canvasClass.getMethod("drawBitmap", bitmapClass, Float::class.javaPrimitiveType, Float::class.javaPrimitiveType, paintClass)
        val left = ((size - w) / 2).toFloat()
        val top = ((size - h) / 2).toFloat()
        drawBitmap.invoke(canvas, scaledSrc, left, top, paint)

        // Recycle
        try { bitmapClass.getMethod("recycle").invoke(scaledSrc) } catch (_: Exception) {}
        try { bitmapClass.getMethod("recycle").invoke(original) } catch (_: Exception) {}

        val streamClass = Class.forName("java.io.ByteArrayOutputStream")
        val stream = streamClass.getConstructor().newInstance()
        val compress = bitmapClass.getMethod("compress", Class.forName("android.graphics.Bitmap\$CompressFormat"), Int::class.javaPrimitiveType, Class.forName("java.io.OutputStream"))
        val formatPng = Class.forName("android.graphics.Bitmap\$CompressFormat").getField("PNG").get(null)
        compress.invoke(out, formatPng, 100, stream)
        val toByteArray = streamClass.getMethod("toByteArray")
        val result = toByteArray.invoke(stream) as ByteArray
        try { bitmapClass.getMethod("recycle").invoke(out) } catch (_: Exception) {}
        return result
    } catch (e: ClassNotFoundException) {
        logger.info("Android graphics not available (${e.message}), falling back to ImageIO")
    } catch (e: Exception) {
        logger.info("Android scaling failed (${e.message}), falling back to ImageIO")
    }

    // Fallback to ImageIO (desktop JVM)
    return try {
        val imageIoClass = Class.forName("javax.imageio.ImageIO")
        val readMethod = imageIoClass.getMethod("read", java.io.InputStream::class.java)
        val original = readMethod.invoke(null, ByteArrayInputStream(bytes)) ?: return null
        val bufferedImageClass = original.javaClass
        val getWidth = bufferedImageClass.getMethod("getWidth")
        val getHeight = bufferedImageClass.getMethod("getHeight")
        val width = getWidth.invoke(original) as Int
        val height = getHeight.invoke(original) as Int

        val scale = size.toDouble() / maxOf(width, height)
        val w = (width * scale).toInt().coerceAtLeast(1)
        val h = (height * scale).toInt().coerceAtLeast(1)

        val outClass = Class.forName("java.awt.image.BufferedImage")
        val outCtor = outClass.getConstructor(Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
        // BufferedImage.TYPE_INT_ARGB = 2
        val out = outCtor.newInstance(size, size, 2)
        val createGraphics = outClass.getMethod("createGraphics")
        val g = createGraphics.invoke(out)
        val g2dClass = g.javaClass
        try {
            val hintsClass = Class.forName("java.awt.RenderingHints")
            val keyInterpolation = hintsClass.getField("KEY_INTERPOLATION").get(null)
            val valueBilinear = hintsClass.getField("VALUE_INTERPOLATION_BILINEAR").get(null)
            val keyRendering = hintsClass.getField("KEY_RENDERING").get(null)
            val valueQuality = hintsClass.getField("VALUE_RENDER_QUALITY").get(null)
            val setHint = g2dClass.getMethod("setRenderingHint", Class.forName("java.awt.RenderingHints\$Key"), Any::class.java)
            setHint.invoke(g, keyInterpolation, valueBilinear)
            setHint.invoke(g, keyRendering, valueQuality)
        } catch (_: Exception) {}

        val drawImage = g2dClass.getMethod("drawImage", Class.forName("java.awt.Image"), Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Any::class.java)
        drawImage.invoke(g, original, (size - w) / 2, (size - h) / 2, w, h, null)
        g2dClass.getMethod("dispose").invoke(g)

        val baosClass = Class.forName("java.io.ByteArrayOutputStream")
        val baos = baosClass.getConstructor().newInstance()
        val writeMethod = imageIoClass.getMethod("write", Class.forName("java.awt.image.RenderedImage"), String::class.java, Class.forName("java.io.OutputStream"))
        writeMethod.invoke(null, out, "png", baos)
        val toByteArray = baosClass.getMethod("toByteArray")
        toByteArray.invoke(baos) as ByteArray
    } catch (e: Exception) {
        logger.warning("ImageIO fallback also failed: ${e.message}")
        null
    }
}
