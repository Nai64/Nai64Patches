package nai64.universaloverlay.modules;

import android.view.Choreographer;

/** Samples approximate display frame cadence only while the FPS row is visible. */
public final class FpsModule extends UniversalOverlayStatisticModule {
    private int frames;
    private Choreographer.FrameCallback frameCallback;
    public FpsModule() {
        super("fps", "FPS", "Approximate display frame cadence. Monitor short name: FPS.");
        frameCallback = frameTimeNanos -> {
            try {
                frames++;
                if (running) Choreographer.getInstance().postFrameCallback(frameCallback);
            } catch (RuntimeException ignored) {
                disableAfterFailure();
            }
        };
    }
    @Override protected String value() { return "Measuring…"; }
    @Override public void start() {
        if (running || valueView == null) return;
        running = true;
        frames = 0;
        refresh();
        Choreographer.getInstance().postFrameCallback(frameCallback);
        handler.postDelayed(this::sample, 1000);
    }
    private void sample() {
        if (!running) return;
        try {
            int sample = frames;
            frames = 0;
            String display = "FPS: ~" + sample;
            if (isMenuVisible()) {
                if (valueView != null) valueView.setText(display);
            } else if (monitorView != null) {
                monitorView.setText(display);
            }
            handler.postDelayed(this::sample, 1000);
        } catch (RuntimeException ignored) {
            disableAfterFailure();
        }
    }
    @Override public void stop() {
        try {
            running = false;
            handler.removeCallbacksAndMessages(null);
            Choreographer.getInstance().removeFrameCallback(frameCallback);
            if (valueView != null) valueView.setText("Disabled");
        } catch (RuntimeException ignored) {
            disableAfterFailure();
        }
    }
}
