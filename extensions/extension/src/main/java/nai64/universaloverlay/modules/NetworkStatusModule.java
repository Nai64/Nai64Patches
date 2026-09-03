package nai64.universaloverlay.modules;

import android.app.Activity;
import android.net.TrafficStats;
import java.util.Locale;

/** Displays process network traffic deltas as separate incoming and outgoing monitors. */
public final class NetworkStatusModule extends UniversalOverlayStatisticModule {
    private final int uid;
    private long lastRx = TrafficStats.UNSUPPORTED;
    private long lastTx = TrafficStats.UNSUPPORTED;
    private long rxRate;
    private long txRate;

    public NetworkStatusModule(Activity activity) {
        super("networkStatus", "Network status", "Approximate app network traffic between samples. Monitor short names: ↓IT incoming, ↑OT outgoing.");
        uid = activity.getApplicationInfo().uid;
    }
    @Override public int monitorCount() { return 2; }
    @Override protected String value() {
        sampleTraffic();
        return "↓IT: " + format(rxRate) + " | ↑OT: " + format(txRate);
    }
    @Override protected String monitorValue() {
        return "↓IT: " + format(rxRate) + " | ↑OT: " + format(txRate);
    }
    @Override protected String monitorValue(int index) {
        return (index == 0 ? "↓IT: " : "↑OT: ") + format(index == 0 ? rxRate : txRate);
    }
    private void sampleTraffic() {
        long rx = TrafficStats.getUidRxBytes(uid);
        long tx = TrafficStats.getUidTxBytes(uid);
        if (rx == TrafficStats.UNSUPPORTED || tx == TrafficStats.UNSUPPORTED) {
            rxRate = txRate = 0;
        } else {
            if (lastRx != TrafficStats.UNSUPPORTED) rxRate = Math.max(0, rx - lastRx);
            if (lastTx != TrafficStats.UNSUPPORTED) txRate = Math.max(0, tx - lastTx);
            lastRx = rx;
            lastTx = tx;
        }
    }
    private static String format(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.US, "%.1f KB", bytes / 1024f);
        return String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f));
    }
}
