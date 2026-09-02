package nai64.universaloverlay.modules;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.view.WindowManager;

/** Read-only device facts that are inexpensive to collect when the menu is refreshed. */
public final class DeviceInformationModule extends UniversalOverlayStatisticModule {
    private final ActivityManager memory;
    private final WindowManager windows;
    public DeviceInformationModule(android.content.Context context) {
        super("deviceInformation", "Device information", "Phone model, refresh rate, CPU, RAM, and Android version.");
        memory = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        windows = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }
    @Override protected String value() {
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
        if (memory != null) memory.getMemoryInfo(info);
        float refreshRate = windows == null ? 0f : windows.getDefaultDisplay().getRefreshRate();
        return Build.MANUFACTURER + " " + Build.MODEL + " | " + String.format(java.util.Locale.US, "%.0f Hz", refreshRate)
                + " | " + Build.HARDWARE + " | " + Runtime.getRuntime().availableProcessors() + " CPU | "
                + (info.totalMem / (1024L * 1024L)) + " MB RAM | Android " + Build.VERSION.RELEASE;
    }
}
