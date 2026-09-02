package nai64.universaloverlay;

import android.graphics.Color;
import android.view.Gravity;

/**
 * Decodes and validates overlay configuration inside the extension runtime.
 * The patch-building Kotlin code supplies version 2; legacy 14-field payloads remain supported.
 */
final class UniversalOverlayConfig {
    private static final String DEFAULT_DESCRIPTION =
            "Welcome to the Nai64Patches Universal Overlay Patch. This experimental in-app overlay " +
            "contains optional statistic, activity, and future hook modules. More may be added in " +
            "future updates. The idea and initial works of this Universal Overlay Patch are from " +
            "Zanuaimi.";
    String title, description, repositoryText, repositoryUrl, buttonText;
    int background, outline, buttonTextColor, buttonBackground, buttonSize, gravity;
    float opacity;
    int shape;
    boolean keepAwake, fullscreen, screenshots;
    boolean systemTime, fps, sessionTime;

    static UniversalOverlayConfig decode(String encoded) {
        UniversalOverlayConfig c = new UniversalOverlayConfig();
        String[] values = encoded == null ? new String[0] : encoded.split("\\|", -1);
        // Version 2 prepends a version field. Keep accepting the original 14-field format so an
        // older generated patch remains safe when paired with this newer extension.
        String[] v = new String[15];
        for (int i = 0; i < v.length; i++) v[i] = i < values.length ? decodePart(values[i]) : "";
        int offset = "2".equals(v[0]) ? 1 : 0;
        c.title = limit(field(v, offset, 0), 80, "Nai64Patches Universal Overlay Patch");
        c.description = limit(field(v, offset, 1), 500, DEFAULT_DESCRIPTION);
        c.repositoryText = empty(field(v, offset, 2), "Nai64 repository");
        c.repositoryUrl = validUrl(field(v, offset, 3));
        c.background = color(field(v, offset, 4), 0xCC101820);
        c.outline = color(field(v, offset, 5), 0xFF55D6BE);
        c.buttonText = limit(empty(field(v, offset, 6), "N"), 3, "N");
        c.buttonTextColor = color(field(v, offset, 7), Color.BLACK);
        c.buttonBackground = color(field(v, offset, 8), Color.WHITE);
        String shape = field(v, offset, 9);
        c.shape = "square".equals(shape) ? 0 : ("squircle".equals(shape) ? 2 : 1);
        c.buttonSize = integer(field(v, offset, 10), 56, 32, 128);
        c.opacity = integer(field(v, offset, 11), 35, 10, 100) / 100f;
        c.gravity = gravity(field(v, offset, 12));
        String controls = field(v, offset, 13);
        c.keepAwake = hasToken(controls, "keep");
        c.fullscreen = hasToken(controls, "fullscreen");
        c.screenshots = hasToken(controls, "screenshots");
        c.systemTime = hasToken(controls, "systemTime");
        c.fps = hasToken(controls, "fps");
        c.sessionTime = hasToken(controls, "sessionTime");
        return c;
    }

    private static boolean hasToken(String values, String token) {
        for (String value : values.split(",")) if (token.equals(value)) return true;
        return false;
    }

    private static String field(String[] values, int offset, int index) {
        int position = offset + index;
        return position < values.length ? values[position] : "";
    }

    private static String decodePart(String value) {
        try { return new String(android.util.Base64.decode(value, android.util.Base64.DEFAULT), java.nio.charset.Charset.forName("UTF-8")); }
        catch (RuntimeException ignored) { return ""; }
    }

    private static String empty(String value, String fallback) { return value == null || value.isEmpty() ? fallback : value; }
    private static String limit(String value, int max, String fallback) { String result = empty(value, fallback); return result.substring(0, Math.min(max, result.length())); }
    private static String validUrl(String value) { return value.startsWith("http://") || value.startsWith("https://") ? value : "https://github.com/Nai64/Nai64Patches"; }
    private static int integer(String value, int fallback, int min, int max) { try { return Math.max(min, Math.min(max, Integer.parseInt(value))); } catch (RuntimeException ignored) { return fallback; } }
    private static int color(String value, int fallback) {
        try {
            String v = value.startsWith("#") ? value.substring(1) : value;
            if (v.length() == 6) v = "FF" + v;
            if (v.length() != 8) return fallback;
            return (int) Long.parseLong(v, 16);
        } catch (RuntimeException ignored) { return fallback; }
    }
    private static int gravity(String value) {
        if ("topLeft".equals(value)) return Gravity.TOP | Gravity.LEFT;
        if ("topMiddle".equals(value)) return Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        if ("centerLeft".equals(value)) return Gravity.CENTER_VERTICAL | Gravity.LEFT;
        if ("centerRight".equals(value)) return Gravity.CENTER_VERTICAL | Gravity.RIGHT;
        if ("bottomLeft".equals(value)) return Gravity.BOTTOM | Gravity.LEFT;
        if ("bottomMiddle".equals(value)) return Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        if ("bottomRight".equals(value)) return Gravity.BOTTOM | Gravity.RIGHT;
        return Gravity.TOP | Gravity.RIGHT;
    }
}
