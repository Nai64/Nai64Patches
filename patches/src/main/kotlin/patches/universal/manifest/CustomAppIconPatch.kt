package patches.universal.manifest

import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.intOption
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.Base64
import java.util.logging.Logger
import org.w3c.dom.Element
import patches.universal.manifest.NS_ANDROID
import patches.universal.manifest.applicationOrNull
import patches.universal.manifest.ensureThemeItem

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
    description = "Replaces the launcher icon with a custom image you supply (image URL, file path, or base64). Optionally apply hue shift, negative, sepia, or flip to the icon (works on original icon even without custom source).",
    default = false,
) {
    val iconSource by stringOption(
        title = "Icon source",
        default = "",
        key = "customAppIconSource",
        description = "Image source: an http(s) URL, a local file path, or base64 " +
            "(optionally prefixed with 'data:image/png;base64,'). Leave empty to use original icon with effects.",
    )
    val hue by intOption(
        title = "Hue shift (0-360)",
        default = 0,
        key = "iconHue",
        description = "Hue rotation in degrees (0 = none, 180 = complementary). Applied after other effects.",
    )
    val negative by booleanOption(
        title = "Negative (Invert)",
        default = false,
        key = "iconNegative",
        description = "Invert colors (negative).",
    )
    val sepia by booleanOption(
        title = "Sepia",
        default = false,
        key = "iconSepia",
        description = "Apply sepia tone.",
    )
    val flipHorizontal by booleanOption(
        title = "Flip Horizontal",
        default = false,
        key = "iconFlipH",
        description = "Mirror icon horizontally.",
    )
    val flipVertical by booleanOption(
        title = "Flip Vertical",
        default = false,
        key = "iconFlipV",
        description = "Mirror icon vertically.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val src = (iconSource ?: "").trim()
        val hueShift = (hue ?: 0).coerceIn(0, 360)
        val doNegative = negative == true
        val doSepia = sepia == true
        val doFlipH = flipHorizontal == true
        val doFlipV = flipVertical == true
        val hasEffect = hueShift != 0 || doNegative || doSepia || doFlipH || doFlipV

        if (src.isEmpty() && !hasEffect) {
            logger.warning("No icon source or effect provided. Skipping.")
            return@execute
        }

        val srcBytes: ByteArray? = when {
            src.isNotEmpty() -> resolveIconBytes(src, logger)
            else -> findOriginalIconBytes(logger)
        }
        if (srcBytes == null) {
            logger.warning("Could not load image (custom source or original icon). Skipping.")
            return@execute
        }

        // Decode once, apply effects if needed, then scale per density
        val decoded = decodeImage(srcBytes, logger)
        if (decoded == null) {
            logger.warning("Source is not a decodable image. Skipping.")
            return@execute
        }

        val processed = if (hasEffect) {
            applyEffects(decoded, hueShift, doNegative, doSepia, doFlipH, doFlipV, logger) ?: decoded
        } else decoded

        var written = 0
        for ((dir, size) in ICON_DENSITIES) {
            val outDir = get("res/$dir", true)
            outDir.mkdirs()
            val outFile = File(outDir, "$ICON_NAME.png")
            val scaled = try {
                scaleDecodedImage(processed, size, logger)
            } catch (e: Exception) {
                logger.warning("Failed to scale image for $dir (${e.message})")
                null
            }
            if (scaled != null) {
                FileOutputStream(outFile).use { it.write(scaled) }
                written++
            } else {
                // Fallback: encode processed as-is without scaling
                val fallback = encodeImage(processed, logger) ?: srcBytes
                FileOutputStream(outFile).use { it.write(fallback) }
                written++
            }
        }

        // Recycle decode objects if Android bitmaps
        tryRecycle(decoded)
        if (processed !== decoded) tryRecycle(processed)

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

        logger.info("Wrote $written icon density bucket(s) and replaced $replaced icon reference(s). Hue=$hueShift neg=$doNegative sepia=$doSepia flipH=$doFlipH flipV=$doFlipV")
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

private fun ResourcePatchContext.findOriginalIconBytes(logger: Logger): ByteArray? {
    return try {
        // Try to read icon name from manifest
        var iconRef: String? = null
        document("AndroidManifest.xml").use { manifest ->
            val app = manifest.documentElement.applicationOrNull()
            iconRef = app?.getAttributeNS(NS_ANDROID, "icon")?.trim()?.takeIf { it.isNotEmpty() }
            if (iconRef.isNullOrEmpty()) {
                // Fallback search activity
                for (tag in listOf("activity", "activity-alias")) {
                    val nodes = manifest.getElementsByTagName(tag)
                    for (i in 0 until nodes.length) {
                        val el = nodes.item(i) as? Element ?: continue
                        val ref = el.getAttributeNS(NS_ANDROID, "icon")?.trim()
                        if (!ref.isNullOrEmpty()) { iconRef = ref; break }
                    }
                }
            }
        }
        val iconName = iconRef?.substringAfterLast('/')?.substringAfterLast(':') ?: "ic_launcher"
        logger.info("Looking for original icon '$iconName' (ref=$iconRef)")
        val resDir = get("res", false)
        if (!resDir.isDirectory) return null
        // Collect candidates: mipmap-* / drawable-* with iconName
        val candidates = resDir.walkTopDown().filter { it.isFile && it.nameWithoutExtension == iconName }.toList()
        val chosen = candidates.maxByOrNull { it.length() } ?: candidates.firstOrNull()
        // Fallback: any ic_launcher*
        val fallback = if (chosen == null) {
            resDir.walkTopDown().filter { it.isFile && it.nameWithoutExtension.startsWith("ic_launcher", ignoreCase = true) }.maxByOrNull { it.length() }
        } else null
        val file = chosen ?: fallback
        if (file != null) {
            logger.info("Using original icon file: ${file.relativeTo(resDir)}")
            file.readBytes()
        } else {
            logger.warning("Original icon file not found for '$iconName'")
            null
        }
    } catch (e: Exception) {
        logger.warning("Failed to find original icon: ${e.message}")
        null
    }
}

private fun decodeImage(bytes: ByteArray, logger: Logger): Any? {
    // Try Android first
    try {
        val bitmapFactoryClass = Class.forName("android.graphics.BitmapFactory")
        val decodeMethod = bitmapFactoryClass.getMethod("decodeByteArray", ByteArray::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
        val bmp = decodeMethod.invoke(null, bytes, 0, bytes.size)
        if (bmp != null) return bmp
    } catch (_: ClassNotFoundException) {
    } catch (e: Exception) {
        logger.info("Android decode failed: ${e.message}")
    }
    // Fallback ImageIO
    return try {
        val imageIoClass = Class.forName("javax.imageio.ImageIO")
        val readMethod = imageIoClass.getMethod("read", java.io.InputStream::class.java)
        readMethod.invoke(null, ByteArrayInputStream(bytes))
    } catch (e: Exception) {
        logger.warning("ImageIO decode failed: ${e.message}")
        null
    }
}

private fun encodeImage(obj: Any, logger: Logger): ByteArray? {
    val isAndroid = isAndroidBitmap(obj)
    return if (isAndroid) {
        try {
            val bitmapClass = Class.forName("android.graphics.Bitmap")
            val streamClass = Class.forName("java.io.ByteArrayOutputStream")
            val stream = streamClass.getConstructor().newInstance()
            val compress = bitmapClass.getMethod("compress", Class.forName("android.graphics.Bitmap\$CompressFormat"), Int::class.javaPrimitiveType, Class.forName("java.io.OutputStream"))
            val formatPng = Class.forName("android.graphics.Bitmap\$CompressFormat").getField("PNG").get(null)
            compress.invoke(obj, formatPng, 100, stream)
            val toByteArray = streamClass.getMethod("toByteArray")
            toByteArray.invoke(stream) as ByteArray
        } catch (e: Exception) {
            logger.warning("Android encode failed: ${e.message}")
            null
        }
    } else {
        try {
            val imageIoClass = Class.forName("javax.imageio.ImageIO")
            val baosClass = Class.forName("java.io.ByteArrayOutputStream")
            val baos = baosClass.getConstructor().newInstance()
            val writeMethod = imageIoClass.getMethod("write", Class.forName("java.awt.image.RenderedImage"), String::class.java, Class.forName("java.io.OutputStream"))
            writeMethod.invoke(null, obj, "png", baos)
            val toByteArray = baosClass.getMethod("toByteArray")
            toByteArray.invoke(baos) as ByteArray
        } catch (e: Exception) {
            logger.warning("ImageIO encode failed: ${e.message}")
            null
        }
    }
}

private fun isAndroidBitmap(obj: Any): Boolean {
    return try { obj.javaClass.name == "android.graphics.Bitmap" } catch (_: Exception) { false }
}

private fun tryRecycle(obj: Any) {
    if (!isAndroidBitmap(obj)) return
    try { Class.forName("android.graphics.Bitmap").getMethod("recycle").invoke(obj) } catch (_: Exception) {}
}

private fun applyEffects(src: Any, hue: Int, negative: Boolean, sepia: Boolean, flipH: Boolean, flipV: Boolean, logger: Logger): Any? {
    return try {
        if (isAndroidBitmap(src)) applyEffectsAndroid(src, hue, negative, sepia, flipH, flipV, logger)
        else applyEffectsAwt(src, hue, negative, sepia, flipH, flipV, logger)
    } catch (e: Exception) {
        logger.warning("Effect failed: ${e.message}")
        null
    }
}

private fun applyEffectsAndroid(src: Any, hue: Int, negative: Boolean, sepia: Boolean, flipH: Boolean, flipV: Boolean, logger: Logger): Any? {
    val bitmapClass = Class.forName("android.graphics.Bitmap")
    val configClass = Class.forName("android.graphics.Bitmap\$Config")
    val getWidth = bitmapClass.getMethod("getWidth")
    val getHeight = bitmapClass.getMethod("getHeight")
    val w = getWidth.invoke(src) as Int
    val h = getHeight.invoke(src) as Int
    val pixels = IntArray(w * h)
    val getPixels = bitmapClass.getMethod("getPixels", IntArray::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
    getPixels.invoke(src, pixels, 0, w, 0, 0, w, h)
    val outPixels = processPixels(pixels, w, h, hue, negative, sepia, flipH, flipV)
    val createBitmap = bitmapClass.getMethod("createBitmap", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, configClass)
    val configArgb8888 = configClass.getField("ARGB_8888").get(null)
    val out = createBitmap.invoke(null, w, h, configArgb8888)
    val setPixels = bitmapClass.getMethod("setPixels", IntArray::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
    setPixels.invoke(out, outPixels, 0, w, 0, 0, w, h)
    return out
}

private fun applyEffectsAwt(src: Any, hue: Int, negative: Boolean, sepia: Boolean, flipH: Boolean, flipV: Boolean, logger: Logger): Any? {
    val bufferedImageClass = src.javaClass
    val getWidth = bufferedImageClass.getMethod("getWidth")
    val getHeight = bufferedImageClass.getMethod("getHeight")
    val w = getWidth.invoke(src) as Int
    val h = getHeight.invoke(src) as Int
    val pixels = IntArray(w * h)
    val getRGB = bufferedImageClass.getMethod("getRGB", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, IntArray::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
    getRGB.invoke(src, 0, 0, w, h, pixels, 0, w)
    val outPixels = processPixels(pixels, w, h, hue, negative, sepia, flipH, flipV)
    val outClass = Class.forName("java.awt.image.BufferedImage")
    val outCtor = outClass.getConstructor(Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
    val out = outCtor.newInstance(w, h, 2) // TYPE_INT_ARGB = 2
    val setRGB = outClass.getMethod("setRGB", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, IntArray::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
    setRGB.invoke(out, 0, 0, w, h, outPixels, 0, w)
    return out
}

private fun processPixels(pixels: IntArray, w: Int, h: Int, hue: Int, negative: Boolean, sepia: Boolean, flipH: Boolean, flipV: Boolean): IntArray {
    var work = pixels
    // Flip first (geometry)
    if (flipH || flipV) {
        val flipped = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val srcX = if (flipH) w - 1 - x else x
                val srcY = if (flipV) h - 1 - y else y
                flipped[y * w + x] = work[srcY * w + srcX]
            }
        }
        work = flipped
    }
    // Color effects per pixel
    if (negative || sepia || hue != 0) {
        for (i in work.indices) {
            val argb = work[i]
            val a = (argb ushr 24) and 0xFF
            var r = (argb ushr 16) and 0xFF
            var g = (argb ushr 8) and 0xFF
            var b = argb and 0xFF
            if (negative) {
                r = 255 - r; g = 255 - g; b = 255 - b
            }
            if (sepia) {
                val tr = (0.393 * r + 0.769 * g + 0.189 * b).toInt().coerceIn(0, 255)
                val tg = (0.349 * r + 0.686 * g + 0.168 * b).toInt().coerceIn(0, 255)
                val tb = (0.272 * r + 0.534 * g + 0.131 * b).toInt().coerceIn(0, 255)
                r = tr; g = tg; b = tb
            }
            if (hue != 0) {
                val hsv = rgbToHsv(r, g, b)
                var hh = (hsv[0] + hue) % 360
                if (hh < 0) hh += 360
                val rgb = hsvToRgb(hh, hsv[1], hsv[2])
                r = (rgb ushr 16) and 0xFF
                g = (rgb ushr 8) and 0xFF
                b = rgb and 0xFF
            }
            work[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
    }
    return work
}

private fun rgbToHsv(r: Int, g: Int, b: Int): FloatArray {
    val rf = r / 255f; val gf = g / 255f; val bf = b / 255f
    val max = maxOf(rf, gf, bf); val min = minOf(rf, gf, bf)
    val delta = max - min
    var h = 0f
    val s = if (max == 0f) 0f else delta / max
    val v = max
    if (delta != 0f) {
        h = when (max) {
            rf -> 60 * (((gf - bf) / delta) % 6)
            gf -> 60 * (((bf - rf) / delta) + 2)
            else -> 60 * (((rf - gf) / delta) + 4)
        }
        if (h < 0) h += 360
    }
    return floatArrayOf(h, s, v)
}

private fun hsvToRgb(h: Float, s: Float, v: Float): Int {
    val c = v * s
    val hh = h / 60f
    val x = c * (1 - kotlin.math.abs(hh % 2 - 1))
    var r1 = 0f; var g1 = 0f; var b1 = 0f
    when (hh.toInt()) {
        0 -> { r1 = c; g1 = x }
        1 -> { r1 = x; g1 = c }
        2 -> { g1 = c; b1 = x }
        3 -> { g1 = x; b1 = c }
        4 -> { r1 = x; b1 = c }
        5 -> { r1 = c; b1 = x }
    }
    val m = v - c
    val r = ((r1 + m) * 255).toInt().coerceIn(0, 255)
    val g = ((g1 + m) * 255).toInt().coerceIn(0, 255)
    val b = ((b1 + m) * 255).toInt().coerceIn(0, 255)
    return (r shl 16) or (g shl 8) or b
}

private fun scaleDecodedImage(src: Any, size: Int, logger: Logger): ByteArray? {
    return if (isAndroidBitmap(src)) {
        try {
            val bitmapClass = Class.forName("android.graphics.Bitmap")
            val canvasClass = Class.forName("android.graphics.Canvas")
            val paintClass = Class.forName("android.graphics.Paint")
            val getWidth = bitmapClass.getMethod("getWidth")
            val getHeight = bitmapClass.getMethod("getHeight")
            val w0 = getWidth.invoke(src) as Int
            val h0 = getHeight.invoke(src) as Int
            val scale = size.toDouble() / maxOf(w0, h0)
            val w = (w0 * scale).toInt().coerceAtLeast(1)
            val h = (h0 * scale).toInt().coerceAtLeast(1)
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
            val scaledSrc = createScaled.invoke(null, src, w, h, true)
            val drawBitmap = canvasClass.getMethod("drawBitmap", bitmapClass, Float::class.javaPrimitiveType, Float::class.javaPrimitiveType, paintClass)
            drawBitmap.invoke(canvas, scaledSrc, ((size - w) / 2).toFloat(), ((size - h) / 2).toFloat(), paint)
            try { bitmapClass.getMethod("recycle").invoke(scaledSrc) } catch (_: Exception) {}
            val streamClass = Class.forName("java.io.ByteArrayOutputStream")
            val stream = streamClass.getConstructor().newInstance()
            val compress = bitmapClass.getMethod("compress", Class.forName("android.graphics.Bitmap\$CompressFormat"), Int::class.javaPrimitiveType, Class.forName("java.io.OutputStream"))
            val formatPng = Class.forName("android.graphics.Bitmap\$CompressFormat").getField("PNG").get(null)
            compress.invoke(out, formatPng, 100, stream)
            val toByteArray = streamClass.getMethod("toByteArray")
            val result = toByteArray.invoke(stream) as ByteArray
            try { bitmapClass.getMethod("recycle").invoke(out) } catch (_: Exception) {}
            result
        } catch (e: Exception) {
            logger.info("Android scale failed: ${e.message}")
            null
        }
    } else {
        try {
            val bufferedImageClass = src.javaClass
            val getWidth = bufferedImageClass.getMethod("getWidth")
            val getHeight = bufferedImageClass.getMethod("getHeight")
            val w0 = getWidth.invoke(src) as Int
            val h0 = getHeight.invoke(src) as Int
            val scale = size.toDouble() / maxOf(w0, h0)
            val w = (w0 * scale).toInt().coerceAtLeast(1)
            val h = (h0 * scale).toInt().coerceAtLeast(1)
            val outClass = Class.forName("java.awt.image.BufferedImage")
            val outCtor = outClass.getConstructor(Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
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
            drawImage.invoke(g, src, (size - w) / 2, (size - h) / 2, w, h, null)
            g2dClass.getMethod("dispose").invoke(g)
            val imageIoClass = Class.forName("javax.imageio.ImageIO")
            val baosClass = Class.forName("java.io.ByteArrayOutputStream")
            val baos = baosClass.getConstructor().newInstance()
            val writeMethod = imageIoClass.getMethod("write", Class.forName("java.awt.image.RenderedImage"), String::class.java, Class.forName("java.io.OutputStream"))
            writeMethod.invoke(null, out, "png", baos)
            val toByteArray = baosClass.getMethod("toByteArray")
            toByteArray.invoke(baos) as ByteArray
        } catch (e: Exception) {
            logger.warning("ImageIO scale failed: ${e.message}")
            null
        }
    }
}
