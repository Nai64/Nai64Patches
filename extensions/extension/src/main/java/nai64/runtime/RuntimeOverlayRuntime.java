package nai64.runtime;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Runtime implementation for the Nai64 overlay.
 *
 * This class is compiled into the extension DEX. The patch generator only injects a bridge call;
 * changes to this file therefore do not alter host Activity fields, listener interfaces, or large
 * generated Smali methods. The implementation deliberately uses platform Views only so it can run
 * in ordinary Android apps, Unity/Godot hosts, and game Activities without AppCompat coupling.
 */
public final class RuntimeOverlayRuntime {
    private static final Map<Activity, Controller> CONTROLLERS = new WeakHashMap<>();
    private static boolean callbacksRegistered;
    private static RuntimeOverlayConfig configuration;

    private RuntimeOverlayRuntime() { }

    /** Primary entry point, called once from Application.onCreate(). */
    public static synchronized void install(Application application, String encodedConfig) {
        if (application == null) return;
        configuration = RuntimeOverlayConfig.decode(encodedConfig);
        if (!callbacksRegistered) {
            application.registerActivityLifecycleCallbacks(new RuntimeOverlayLifecycle());
            callbacksRegistered = true;
        }
    }

    /** Compatibility fallback for APKs where Application.onCreate cannot be resolved. */
    public static synchronized void installActivity(Activity activity, String encodedConfig) {
        if (activity == null) return;
        configuration = RuntimeOverlayConfig.decode(encodedConfig);
        showActivity(activity);
    }

    static synchronized void showActivity(Activity activity) {
        if (activity.isFinishing() || (android.os.Build.VERSION.SDK_INT >= 17 && activity.isDestroyed())) return;
        if (CONTROLLERS.containsKey(activity)) return;
        try {
            Controller controller = new Controller(activity, configuration);
            CONTROLLERS.put(activity, controller);
            controller.attach();
        } catch (RuntimeException ignored) {
            // TODO(runtime-overlay): replace this broad runtime guard with narrow diagnostics after
            // the first APK compatibility matrix is available. Never let overlay failure crash host apps.
            CONTROLLERS.remove(activity);
        }
    }

    static synchronized void removeActivity(Activity activity) {
        Controller controller = CONTROLLERS.remove(activity);
        if (controller != null) controller.detach();
    }

    /** Owns all views and state for exactly one Activity. */
    private static final class Controller {
        private final Activity activity;
        private final RuntimeOverlayConfig config;
        private final FrameLayout root;
        private final TextView floatingButton;
        private final FrameLayout menuLayer;
        private final View menuScrim;
        private final LinearLayout panel;
        private final FrameLayout confirmationLayer;
        private final List<RuntimeOverlayFeature> features = new ArrayList<>();
        private final int originalWindowFlags;
        private final int originalSystemUi;
        private boolean menuVisible;
        private boolean fullyClosed;
        private float downX;
        private float downY;
        private float startX;
        private float startY;
        private boolean dragged;

        Controller(Activity activity, RuntimeOverlayConfig config) {
            this.activity = activity;
            this.config = config;
            Window window = activity.getWindow();
            originalWindowFlags = window.getAttributes().flags;
            originalSystemUi = window.getDecorView().getSystemUiVisibility();
            root = new FrameLayout(activity);
            root.setClipChildren(false);
            floatingButton = createFloatingButton();
            menuLayer = new FrameLayout(activity);
            menuScrim = createMenuScrim();
            panel = createMenuPanel();
            confirmationLayer = createConfirmationLayer();
        }

        void attach() {
            // TODO(runtime-overlay): verify this attachment strategy against SurfaceView and
            // immersive-mode hosts before adding more rendering-specific behavior.
            FrameLayout.LayoutParams rootParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            activity.addContentView(root, rootParams);
            root.addView(menuLayer, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            menuLayer.addView(menuScrim, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            menuLayer.addView(panel, panel.getLayoutParams());
            root.addView(floatingButton, buttonParams());
            root.addView(confirmationLayer, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            menuLayer.setVisibility(View.GONE);
            confirmationLayer.setVisibility(View.GONE);
        }

        void detach() {
            restoreFeatures();
            if (root.getParent() instanceof ViewGroup) ((ViewGroup) root.getParent()).removeView(root);
        }

        private void restoreFeatures() {
            for (RuntimeOverlayFeature feature : features) {
                try {
                    feature.restore(activity, originalWindowFlags, originalSystemUi);
                } catch (RuntimeException ignored) {
                    // A single incompatible window must not prevent other features or host cleanup.
                }
            }
            features.clear();
        }

        private FrameLayout.LayoutParams buttonParams() {
            int size = dp(config.buttonSize);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
            params.gravity = config.gravity;
            int margin = dp(16);
            params.setMargins(margin, margin, margin, margin);
            return params;
        }

        private TextView createFloatingButton() {
            TextView button = new TextView(activity);
            button.setText(config.buttonText);
            button.setTextColor(config.buttonTextColor);
            button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            button.setGravity(Gravity.CENTER);
            button.setAlpha(config.opacity);
            button.setContentDescription(config.buttonText);
            button.setBackground(RuntimeOverlayViews.background(config.buttonBackground, config.outline, config.shape == 1));
            button.setOnClickListener(v -> toggleMenu());
            button.setOnTouchListener(this::onButtonTouch);
            return button;
        }

        private FrameLayout createMenuLayer() {
            FrameLayout layer = new FrameLayout(activity);
            // This full-screen container remains touchable while the menu is open. Its children
            // consume all background touches so Unity/host content cannot receive game input.
            layer.setClickable(true);
            layer.setFocusable(true);
            return layer;
        }

        private View createMenuScrim() {
            View scrim = new View(activity);
            scrim.setBackgroundColor(0x99000000);
            scrim.setClickable(true);
            scrim.setFocusable(true);
            scrim.setOnClickListener(v -> closeMenu());
            // The clickable View consumes the gesture and still delivers its click callback;
            // returning true here would bypass View.onTouchEvent and prevent dismissal.
            scrim.setOnTouchListener((v, event) -> false);
            return scrim;
        }

        private LinearLayout createMenuPanel() {
            LinearLayout menu = new LinearLayout(activity);
            menu.setOrientation(LinearLayout.VERTICAL);
            menu.setClickable(true);
            menu.setFocusable(true);
            // Consume unused panel area without preventing its child controls from receiving taps.
            menu.setOnTouchListener((v, event) -> true);
            menu.setPadding(dp(20), dp(18), dp(20), dp(12));
            menu.setBackground(RuntimeOverlayViews.background(config.background, config.outline, false));
            FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER);
            panelParams.setMargins(dp(20), dp(20), dp(20), dp(20));
            menu.setLayoutParams(panelParams);

            TextView title = text(config.title, 20, config.outline);
            title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            menu.addView(title, new LinearLayout.LayoutParams(-1, -2));

            TextView description = text(config.description, 14, config.outline);
            LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(-1, -2);
            descriptionParams.topMargin = dp(8);
            menu.addView(description, descriptionParams);

            ScrollView scroll = new ScrollView(activity);
            scroll.setFillViewport(true);
            LinearLayout controls = new LinearLayout(activity);
            controls.setOrientation(LinearLayout.VERTICAL);
            addControls(controls);
            scroll.addView(controls, new ScrollView.LayoutParams(-1, -2));
            LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, 0, 1f);
            scrollParams.topMargin = dp(12);
            menu.addView(scroll, scrollParams);

            LinearLayout actions = new LinearLayout(activity);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setGravity(Gravity.CENTER);
            menu.addView(actions, new LinearLayout.LayoutParams(-1, -2));
            addAction(actions, config.repositoryText, v -> openRepository());
            addAction(actions, "Close menu", v -> closeMenu());
            addAction(actions, "Fully close", v -> showCloseConfirmation());
            return menu;
        }

        private FrameLayout createConfirmationLayer() {
            FrameLayout layer = new FrameLayout(activity);
            layer.setBackgroundColor(0xB3000000);
            layer.setClickable(true);
            layer.setFocusable(true);
            layer.setOnClickListener(v -> hideCloseConfirmation());

            LinearLayout card = new LinearLayout(activity);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(20), dp(18), dp(20), dp(12));
            card.setBackground(RuntimeOverlayViews.background(config.background, config.outline, false));
            card.setClickable(true);
            card.setOnClickListener(v -> { });

            TextView title = text("Close overlay?", 20, config.outline);
            title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            card.addView(title, new LinearLayout.LayoutParams(-1, -2));

            TextView message = text("The overlay will be removed for this Activity.", 14, config.outline);
            LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(-1, -2);
            messageParams.topMargin = dp(8);
            card.addView(message, messageParams);

            LinearLayout actions = new LinearLayout(activity);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(-1, -2);
            actionsParams.topMargin = dp(8);
            card.addView(actions, actionsParams);
            addAction(actions, "Cancel", v -> hideCloseConfirmation());
            addAction(actions, "Fully close", v -> fullyClose());

            FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER);
            cardParams.setMargins(dp(20), dp(20), dp(20), dp(20));
            layer.addView(card, cardParams);
            return layer;
        }

        private void addControls(LinearLayout controls) {
            if (config.keepAwake) addFeature(controls, new KeepAwakeFeature());
            if (config.fullscreen) addFeature(controls, new FullscreenFeature());
            if (config.screenshots) addFeature(controls, new ScreenshotsFeature());
        }

        private void addFeature(LinearLayout controls, RuntimeOverlayFeature feature) {
            final boolean initial;
            try {
                initial = feature.initiallyEnabled(activity, originalWindowFlags, originalSystemUi);
            } catch (RuntimeException ignored) {
                return;
            }
            features.add(feature);
            addSwitch(controls, feature.label(), initial, checked -> {
                try {
                    feature.setEnabled(activity, checked, originalWindowFlags, originalSystemUi);
                } catch (RuntimeException ignored) {
                    // Feature controls are independent; a failure here must not crash the host.
                }
            });
        }

        private void addSwitch(LinearLayout parent, String label, boolean initial, final Toggle toggle) {
            Switch control = new Switch(activity);
            control.setText(label);
            control.setTextSize(16);
            control.setTextColor(config.outline);
            control.setChecked(initial);
            control.setPadding(0, dp(6), 0, dp(6));
            control.setOnCheckedChangeListener((button, checked) -> toggle.changed(checked));
            parent.addView(control, new LinearLayout.LayoutParams(-1, -2));
        }

        private TextView text(String value, float size, int color) {
            TextView view = new TextView(activity);
            view.setText(value);
            view.setTextSize(size);
            view.setTextColor(color);
            return view;
        }

        private void addAction(LinearLayout row, String label, View.OnClickListener listener) {
            TextView action = text(label, 14, config.outline);
            action.setGravity(Gravity.CENTER);
            action.setContentDescription(label);
            action.setOnClickListener(listener);
            action.setBackground(RuntimeOverlayViews.selectableBackground(activity));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(48), 1f);
            row.addView(action, params);
        }

        private void toggleMenu() {
            if (fullyClosed) return;
            menuVisible = !menuVisible;
            if (menuVisible) {
                menuLayer.setVisibility(View.VISIBLE);
                menuScrim.animate().cancel();
                menuScrim.setAlpha(0f);
                menuScrim.animate().alpha(1f).setDuration(180).start();
            } else {
                hideMenuLayer();
            }
            floatingButton.animate().alpha(menuVisible ? 0f : config.opacity).setDuration(180).start();
        }

        private void closeMenu() {
            menuVisible = false;
            hideMenuLayer();
            floatingButton.animate().alpha(config.opacity).setDuration(180).start();
        }

        private void hideMenuLayer() {
            menuScrim.animate().cancel();
            menuScrim.animate().alpha(0f).setDuration(180).withEndAction(() -> {
                if (!menuVisible) menuLayer.setVisibility(View.GONE);
            }).start();
        }

        private void showCloseConfirmation() {
            confirmationLayer.setAlpha(0f);
            confirmationLayer.setVisibility(View.VISIBLE);
            confirmationLayer.animate().alpha(1f).setDuration(180).start();
        }

        private void hideCloseConfirmation() {
            confirmationLayer.animate().alpha(0f).setDuration(160).withEndAction(() ->
                    confirmationLayer.setVisibility(View.GONE)).start();
        }

        private void fullyClose() {
            fullyClosed = true;
            detach();
        }

        private void openRepository() {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(config.repositoryUrl));
                activity.startActivity(intent);
            } catch (RuntimeException ignored) {
                Toast.makeText(activity, "No app is available to open the repository link.", Toast.LENGTH_SHORT).show();
            }
        }

        private boolean onButtonTouch(View view, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getRawX(); downY = event.getRawY();
                    startX = view.getX(); startY = view.getY(); dragged = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - downX;
                    float dy = event.getRawY() - downY;
                    if (Math.abs(dx) > dp(5) || Math.abs(dy) > dp(5)) dragged = true;
                    if (dragged) {
                        view.setX(clamp(startX + dx, 0, root.getWidth() - view.getWidth()));
                        view.setY(clamp(startY + dy, 0, root.getHeight() - view.getHeight()));
                        // The idle state is intentionally translucent, but dragging must make
                        // the control fully visible so its position remains easy to track.
                        view.setAlpha(1f);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!dragged) view.performClick();
                    else view.animate().alpha(config.opacity).setDuration(180).start();
                    return true;
                default: return true;
            }
        }

        private int dp(int value) { return (int) (value * activity.getResources().getDisplayMetrics().density + .5f); }
    }

    private interface Toggle { void changed(boolean checked); }

    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(value, max)); }

}
