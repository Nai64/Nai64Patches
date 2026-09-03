package nai64.universaloverlay.modules;

import android.os.Handler;
import android.os.Looper;
import android.widget.CheckBox;
import android.widget.TextView;
import java.util.List;

/** Base for low-frequency statistic modules that update only while enabled. */
public abstract class UniversalOverlayStatisticModule implements UniversalOverlayModule {
    private final String key;
    private final String label;
    private final String description;
    protected final Handler handler = new Handler(Looper.getMainLooper());
    protected TextView valueView;
    protected TextView monitorView;
    protected List<TextView> monitorViews;
    private CheckBox control;
    protected boolean running;
    private boolean enabled;
    private boolean monitorEnabled;
    private final Runnable sampler = this::sample;

    private void sample() {
        if (!running) return;
        try {
            refresh();
            handler.postDelayed(sampler, 1000);
        } catch (RuntimeException ignored) {
            disableAfterFailure();
        }
    }

    protected UniversalOverlayStatisticModule(String key, String label, String description) {
        this.key = key;
        this.label = label;
        this.description = description;
    }

    @Override public final String key() { return key; }
    @Override public final String label() { return label; }
    @Override public final String description() { return description; }
    protected abstract String value();

    public final void bind(TextView valueView, CheckBox control) {
        this.valueView = valueView;
        this.control = control;
    }

    public final void bindMonitor(TextView monitorView) {
        this.monitorView = monitorView;
    }

    public final void bindMonitors(List<TextView> monitorViews) {
        this.monitorViews = monitorViews;
        this.monitorView = monitorViews == null || monitorViews.isEmpty() ? null : monitorViews.get(0);
    }

    public int monitorCount() { return 1; }

    public final boolean isEnabled() { return enabled; }
    public final boolean isMonitorEnabled() { return monitorEnabled; }

    public final void setMonitorEnabled(boolean enabled) { monitorEnabled = enabled; }

    public final boolean setEnabled(boolean enabled, boolean menuVisible) {
        this.enabled = enabled;
        return menuVisible && enabled ? startSafely() : stopSafely();
    }

    public final boolean startSafely() {
        try {
            start();
            return true;
        } catch (RuntimeException ignored) {
            disableAfterFailure();
            return false;
        }
    }

    public final boolean stopSafely() {
        try {
            stop();
            return true;
        } catch (RuntimeException ignored) {
            disableAfterFailure();
            return false;
        }
    }

    public void start() {
        if (running || valueView == null) return;
        running = true;
        refresh();
        handler.postDelayed(sampler, 1000);
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(sampler);
        if (valueView != null) valueView.setText("Disabled");
        if (monitorViews != null) {
            for (TextView monitor : monitorViews) monitor.setText("Disabled");
        } else {
            setMonitorText("Disabled");
        }
    }

    public final void setChecked(boolean checked) {
        if (control != null && control.isChecked() != checked) control.setChecked(checked);
    }

    protected final void refresh() {
        String current = value();
        if (valueView != null) valueView.setText(current);
        setMonitorText(monitorValue());
        refreshMonitors();
    }

    /** Compact value used by the optional floating monitor. */
    protected String monitorValue() { return value(); }

    protected String monitorValue(int index) { return monitorValue(); }

    protected final void setMonitorText(String value) { setMonitorText(0, value); }

    protected final void setMonitorText(int index, String value) {
        if (monitorViews != null && index >= 0 && index < monitorViews.size()) {
            monitorViews.get(index).setText(value);
        } else if (index == 0 && monitorView != null) {
            monitorView.setText(value);
        }
    }

    private void refreshMonitors() {
        if (monitorViews == null) return;
        for (int i = 0; i < monitorViews.size(); i++) monitorViews.get(i).setText(monitorValue(i));
    }

    protected final void disableAfterFailure() {
        running = false;
        enabled = false;
        handler.removeCallbacksAndMessages(null);
        if (valueView != null) valueView.setText("Unavailable");
        if (monitorViews != null) for (TextView monitor : monitorViews) monitor.setText("Unavailable");
        else if (monitorView != null) monitorView.setText("Unavailable");
    }
}
