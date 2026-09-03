package nai64.universaloverlay.modules;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import java.util.Locale;

/** Displays battery-reported temperature in Celsius and Fahrenheit. */
public final class DeviceTemperatureModule extends UniversalOverlayStatisticModule {
    private final Activity activity;
    public DeviceTemperatureModule(Activity activity) {
        super("deviceTemperature", "Device temperature", "Battery-reported temperature in Celsius and Fahrenheit. Monitor short name: TMP.");
        this.activity = activity;
    }
    @Override protected String monitorValue() { return "TMP: " + value(); }
    @Override protected String value() {
        Intent battery = activity.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery == null) return "Unavailable";
        int tenthsC = battery.getIntExtra("temperature", Integer.MIN_VALUE);
        if (tenthsC == Integer.MIN_VALUE) return "Unavailable";
        float celsius = tenthsC / 10f;
        return String.format(Locale.US, "%.1f C | %.1f F", celsius, celsius * 9f / 5f + 32f);
    }
}
