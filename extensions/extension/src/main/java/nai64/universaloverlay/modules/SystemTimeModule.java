package nai64.universaloverlay.modules;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Displays the phone's local date and time in both common clock formats. */
public final class SystemTimeModule extends UniversalOverlayStatisticModule {
    private final String format;
    public SystemTimeModule(String format) {
        super("systemTime", "System time", "Phone date and time in the selected clock format. The menu also shows the current timezone. Monitor short name: ST.");
        this.format = "24".equals(format) ? "24" : "12";
    }
    @Override protected String value() {
        String pattern = "24".equals(format) ? "dd MMM yyyy | HH:mm:ss" : "dd MMM yyyy | hh:mm:ss a";
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date()) + " | " + timezone();
    }
    @Override protected String monitorValue() {
        String pattern = "24".equals(format) ? "HH:mm" : "hh:mm a";
        return "ST: " + new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date());
    }
    private static String timezone() {
        int offset = TimeZone.getDefault().getOffset(System.currentTimeMillis());
        if (offset == 0) return "UTC";
        int minutes = Math.abs(offset) / 60000;
        int hours = minutes / 60;
        int remainder = minutes % 60;
        return String.format(Locale.US, "GMT%s%d%s", offset < 0 ? "-" : "+", hours,
                remainder == 0 ? "" : String.format(Locale.US, ":%02d", remainder));
    }
}
