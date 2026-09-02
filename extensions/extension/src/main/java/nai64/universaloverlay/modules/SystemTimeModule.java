package nai64.universaloverlay.modules;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Displays the phone's local date and time in both common clock formats. */
public final class SystemTimeModule extends UniversalOverlayStatisticModule {
    public SystemTimeModule() { super("systemTime", "System time", "Phone date and time in 24-hour and 12-hour formats."); }
    @Override protected String value() {
        return new SimpleDateFormat("dd MMM yyyy | HH:mm:ss | hh:mm:ss a", Locale.getDefault()).format(new Date());
    }
    @Override protected String monitorValue() {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
    }
}
