package nai64.universaloverlay.modules.statistic;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import java.util.Locale;
import nai64.universaloverlay.modules.UniversalOverlayStatisticModule;

/** Displays battery-reported temperature in Celsius and Fahrenheit. */
public final class DeviceTemperatureModule extends UniversalOverlayStatisticModule {
    private final Activity activity;
    private final String format;
    public DeviceTemperatureModule(Activity activity, String format) {
        super("deviceTemperature", "Device temperature", "Battery-reported temperature in the selected C, F, or K format. Monitor short name: TMP.");
        this.activity = activity;
        this.format = "fahrenheit".equals(format) || "kelvin".equals(format) ? format : "celsius";
    }
    @Override protected String monitorValue() { return "TMP: " + value(); }
    @Override protected String value() {
        Intent battery = activity.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery == null) return "Unavailable";
        int tenthsC = battery.getIntExtra("temperature", Integer.MIN_VALUE);
        if (tenthsC == Integer.MIN_VALUE) return "Unavailable";
        float celsius = tenthsC / 10f;
        if ("fahrenheit".equals(format)) return String.format(Locale.US, "%.1f F", celsius * 9f / 5f + 32f);
        if ("kelvin".equals(format)) return String.format(Locale.US, "%.1f K", celsius + 273.15f);
        return String.format(Locale.US, "%.1f C", celsius);
    }
}
