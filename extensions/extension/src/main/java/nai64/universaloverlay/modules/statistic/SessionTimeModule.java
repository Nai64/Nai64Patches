package nai64.universaloverlay.modules.statistic;

import android.os.SystemClock;
import java.util.Locale;
import nai64.universaloverlay.modules.UniversalOverlayStatisticModule;

/** Displays elapsed time from the first initialization of the overlay process. */
public final class SessionTimeModule extends UniversalOverlayStatisticModule {
    private final long startElapsed;
    public SessionTimeModule(long startElapsed) {
        super("sessionTime", "App session time", "Time since this overlay process first started. Monitor short name: AST.");
        this.startElapsed = startElapsed;
    }
    @Override protected String value() {
        long seconds = Math.max(0, (SystemClock.elapsedRealtime() - startElapsed) / 1000);
        return String.format(Locale.US, "%02d:%02d:%02d", seconds / 3600, (seconds / 60) % 60, seconds % 60);
    }
    @Override protected String monitorValue() { return "AST: " + value(); }
}
