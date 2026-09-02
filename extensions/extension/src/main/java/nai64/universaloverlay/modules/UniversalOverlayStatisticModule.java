package nai64.universaloverlay.modules;

import android.os.Handler;
import android.os.Looper;
import android.widget.CheckBox;
import android.widget.TextView;

/** Base for low-frequency statistic modules that only update while visible and enabled. */
public abstract class UniversalOverlayStatisticModule implements UniversalOverlayModule {
    private final String key;
    private final String label;
    private final String description;
    protected final Handler handler = new Handler(Looper.getMainLooper());
    protected TextView valueView;
    private CheckBox control;
    protected boolean running;
    private boolean enabled;
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

    public final boolean isEnabled() { return enabled; }

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
    }

    public final void setChecked(boolean checked) {
        if (control != null && control.isChecked() != checked) control.setChecked(checked);
    }

    protected final void refresh() {
        if (valueView != null) valueView.setText(value());
    }

    protected final void disableAfterFailure() {
        running = false;
        enabled = false;
        handler.removeCallbacksAndMessages(null);
        if (valueView != null) valueView.setText("Unavailable");
    }
}
