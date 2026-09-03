package nai64.universaloverlay.modules.statistic;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.view.WindowManager;
import nai64.universaloverlay.modules.UniversalOverlayStatisticModule;

/** Read-only device facts that are inexpensive to collect when the menu is refreshed. */
public final class DeviceInformationModule extends UniversalOverlayStatisticModule {
    private final ActivityManager memory;
    private final WindowManager windows;
    private String cachedValue;
    public DeviceInformationModule(android.content.Context context) {
        super("deviceInformation", "Device information", "Phone model, refresh rate, CPU, RAM, and Android version. No monitor is used.");
        memory = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        windows = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }
    @Override public int monitorCount() { return 0; }
    @Override protected String value() {
        if (cachedValue != null) return cachedValue;
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
        if (memory != null) memory.getMemoryInfo(info);
        float refreshRate = windows == null ? 0f : windows.getDefaultDisplay().getRefreshRate();
        cachedValue = "Phone: " + phoneName() + " | " + String.format(java.util.Locale.US, "%.0f Hz", refreshRate)
                + " | CPU: " + cpuName() + " | " + Runtime.getRuntime().availableProcessors() + " cores | "
                + (info.totalMem / (1024L * 1024L)) + " MB RAM | Android " + Build.VERSION.RELEASE;
        return cachedValue;
    }

    private static String phoneName() {
        String marketName = firstNonBlank(
                systemProperty("ro.product.marketname"),
                systemProperty("ro.product.vendor.marketname"),
                systemProperty("ro.product.odm.marketname"),
                systemProperty("ro.product.product.marketname"));
        if (!marketName.isEmpty()) return marketName;
        String model = firstNonBlank(Build.MODEL, Build.PRODUCT, Build.DEVICE);
        String manufacturer = firstNonBlank(Build.MANUFACTURER, Build.BRAND);
        String fallback = (manufacturer + " " + model).trim();
        return fallback.isEmpty() ? "Unknown device" : fallback;
    }

    private static String cpuName() {
        if (Build.VERSION.SDK_INT >= 31) {
            String manufacturer = Build.SOC_MANUFACTURER == null ? "" : Build.SOC_MANUFACTURER.trim();
            String model = Build.SOC_MODEL == null ? "" : Build.SOC_MODEL.trim();
            String soc = (manufacturer + " " + model).trim();
            if (!soc.isEmpty()) return soc;
        }
        String socModel = firstNonBlank(
                systemProperty("ro.soc.model"),
                systemProperty("ro.vendor.soc.model"),
                systemProperty("ro.board.platform"));
        if (!socModel.isEmpty()) return socModel;
        String hardware = firstNonBlank(Build.HARDWARE, Build.BOARD);
        return hardware.isEmpty() ? "Unknown CPU" : hardware + " (model unavailable)";
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.trim().isEmpty()) return value.trim();
        return "";
    }

    private static String systemProperty(String name) {
        try {
            Class<?> properties = Class.forName("android.os.SystemProperties");
            Object value = properties.getMethod("get", String.class).invoke(null, name);
            return value == null ? "" : value.toString().trim();
        } catch (Exception ignored) {
            return "";
        }
    }
}
