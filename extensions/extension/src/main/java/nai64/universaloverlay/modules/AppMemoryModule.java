package nai64.universaloverlay.modules;

import android.app.ActivityManager;
import android.app.Application;

/** Shows the host process' approximate private memory in MB. */
public final class AppMemoryModule extends UniversalOverlayStatisticModule {
    private final ActivityManager manager;
    private final int pid;
    public AppMemoryModule(android.content.Context context) {
        super("appMemory", "App memory usage", "Approximate memory used by the current app process in MB.");
        manager = (ActivityManager) context.getSystemService(android.content.Context.ACTIVITY_SERVICE);
        pid = android.os.Process.myPid();
    }
    @Override protected String value() {
        if (manager == null) return "Unavailable";
        android.os.Debug.MemoryInfo[] infos = manager.getProcessMemoryInfo(new int[]{pid});
        if (infos.length == 0) return "Unavailable";
        return (infos[0].getTotalPss() / 1024) + " MB";
    }
}
