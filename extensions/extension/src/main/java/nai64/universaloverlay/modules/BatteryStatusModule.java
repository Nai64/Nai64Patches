package nai64.universaloverlay.modules;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;

/** Shows the current device battery percentage without polling more often than the base sampler. */
public final class BatteryStatusModule extends UniversalOverlayStatisticModule {
    private final Activity activity;
    public BatteryStatusModule(Activity activity) {
        super("batteryStatus", "Battery status", "Current battery percentage and charging state.");
        this.activity = activity;
    }
    @Override protected String value() {
        Intent battery = activity.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery == null) return "Unavailable";
        int level = battery.getIntExtra("level", -1);
        int scale = battery.getIntExtra("scale", -1);
        if (level < 0 || scale <= 0) return "Unavailable";
        int percent = Math.max(0, Math.min(100, Math.round(level * 100f / scale)));
        int status = battery.getIntExtra("status", -1);
        boolean charging = status == 2 || status == 5;
        return percent + "%" + (charging ? " charging" : "");
    }
}
