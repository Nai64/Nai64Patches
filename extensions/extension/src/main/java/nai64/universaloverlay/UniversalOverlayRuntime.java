package nai64.universaloverlay;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import nai64.universaloverlay.modules.FullscreenModule;
import nai64.universaloverlay.modules.KeepAwakeModule;
import nai64.universaloverlay.modules.ScreenshotsModule;
import nai64.universaloverlay.modules.UniversalOverlayActivityModule;
import nai64.universaloverlay.modules.UniversalOverlayStatisticModule;
import nai64.universaloverlay.modules.FpsModule;
import nai64.universaloverlay.modules.SessionTimeModule;
import nai64.universaloverlay.modules.SystemTimeModule;

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
public final class UniversalOverlayRuntime {
    private static final Map<Activity, Controller> CONTROLLERS = new WeakHashMap<>();
    private static boolean callbacksRegistered;
    private static UniversalOverlayConfig configuration;
    private static Boolean keepAwakeState;
    private static Boolean fullscreenState;
    private static Boolean screenshotsState;
    private static final Map<String, Boolean> MODULE_STATES = new java.util.HashMap<>();
    private static long sessionStartElapsed;
    private static boolean sharedButtonPositionInitialized;
    private static int sharedButtonX;
    private static int sharedButtonY;

    private UniversalOverlayRuntime() { }

    /** Primary entry point, called once from Application.onCreate(). */
    public static synchronized void install(Application application, String encodedConfig) {
        if (application == null) return;
        configuration = UniversalOverlayConfig.decode(encodedConfig);
        if (sessionStartElapsed == 0) sessionStartElapsed = SystemClock.elapsedRealtime();
        if (!callbacksRegistered) {
            application.registerActivityLifecycleCallbacks(new UniversalOverlayLifecycle());
            callbacksRegistered = true;
        }
    }

    /** Compatibility fallback for APKs where Application.onCreate cannot be resolved. */
    public static synchronized void installActivity(Activity activity, String encodedConfig) {
        if (activity == null) return;
        if (sessionStartElapsed == 0) sessionStartElapsed = SystemClock.elapsedRealtime();
        try {
            Application application = activity.getApplication();
            if (application != null) {
                install(application, encodedConfig);
            } else {
                configuration = UniversalOverlayConfig.decode(encodedConfig);
            }
        } catch (RuntimeException ignored) {
            configuration = UniversalOverlayConfig.decode(encodedConfig);
        }
        showActivity(activity);
    }

    static synchronized void showActivity(Activity activity) {
        if (configuration == null) return;
        if (activity.isFinishing() || (android.os.Build.VERSION.SDK_INT >= 17 && activity.isDestroyed())) return;
        Controller existing = CONTROLLERS.get(activity);
        if (existing != null) {
            existing.applyRememberedStates();
            return;
        }
        Controller controller = null;
        try {
            controller = new Controller(activity, configuration);
            CONTROLLERS.put(activity, controller);
            controller.attach();
        } catch (RuntimeException ignored) {
            if (controller != null) controller.detach();
            // Never let overlay setup failure crash the host application.
            CONTROLLERS.remove(activity);
        }
    }

    static synchronized void removeActivity(Activity activity) {
        Controller controller = CONTROLLERS.remove(activity);
        if (controller != null) controller.detach();
    }

    private static Boolean rememberedState(String key) {
        if ("keepAwake".equals(key)) return keepAwakeState;
        if ("fullscreen".equals(key)) return fullscreenState;
        if ("screenshots".equals(key)) return screenshotsState;
        return null;
    }

    private static void rememberState(String key, boolean enabled) {
        if ("keepAwake".equals(key)) keepAwakeState = enabled;
        else if ("fullscreen".equals(key)) fullscreenState = enabled;
        else if ("screenshots".equals(key)) screenshotsState = enabled;
    }

    private static Boolean rememberedModuleState(String key) { return MODULE_STATES.get(key); }
    private static void rememberModuleState(String key, boolean enabled) { MODULE_STATES.put(key, enabled); }

    /** Owns all views and state for exactly one Activity. */
    private static final class Controller {
        private final Activity activity;
        private final UniversalOverlayConfig config;
        private final FrameLayout root;
        private final TextView floatingButton;
        private final FrameLayout menuLayer;
        private final View menuScrim;
        private final LinearLayout panel;
        private final FrameLayout confirmationLayer;
        private final List<UniversalOverlayActivityModule> activityModules = new ArrayList<>();
        private final List<UniversalOverlayStatisticModule> statistics = new ArrayList<>();
        private final Map<String, CheckBox> featureControls = new java.util.HashMap<>();
        private final int originalWindowFlags;
        private final int originalSystemUi;
        private boolean menuVisible;
        private boolean fullyClosed;
        private boolean attached;
        private boolean detached;
        private float downX;
        private float downY;
        private float startX;
        private float startY;
        private boolean dragged;

        Controller(Activity activity, UniversalOverlayConfig config) {
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
            if (attached || detached) return;
            try {
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
                activity.addContentView(root, contentLayoutParams());
                attached = true;
            } catch (RuntimeException failure) {
                removeRoot();
                throw failure;
            }
        }

        void detach() {
            if (detached) return;
            detached = true;
            for (UniversalOverlayStatisticModule module : statistics) module.stopSafely();
            restoreActivityModules();
            removeRoot();
        }

        private void removeRoot() {
            try {
                if (root.getParent() instanceof ViewGroup) {
                    ((ViewGroup) root.getParent()).removeView(root);
                }
            } catch (RuntimeException ignored) {
                // Cleanup must not propagate a host-specific view hierarchy failure.
            }
            attached = false;
        }

        private FrameLayout.LayoutParams contentLayoutParams() {
            return new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        private void restoreActivityModules() {
            for (UniversalOverlayActivityModule feature : activityModules) {
                try {
                    feature.restore(activity, originalWindowFlags, originalSystemUi);
                } catch (RuntimeException ignored) {
                    // A single incompatible Activity module must not prevent other modules or host cleanup.
                }
            }
            activityModules.clear();
            statistics.clear();
            featureControls.clear();
        }

        private FrameLayout.LayoutParams buttonParams() {
            int size = dp(config.buttonSize);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
            int margin = dp(16);
            if (sharedButtonPositionInitialized) {
                params.gravity = Gravity.TOP | Gravity.LEFT;
                params.setMargins(sharedButtonX, sharedButtonY, 0, 0);
            } else {
                params.gravity = config.gravity;
                params.setMargins(margin, margin, margin, margin);
            }
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
            button.setBackground(UniversalOverlayViews.background(config.buttonBackground, config.outline, config.shape == 1));
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
            scrim.setBackgroundColor(0x55000000);
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
            menu.setBackground(UniversalOverlayViews.background(config.background, config.outline, false));
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

            int maxControlHeight = Math.min(dp(280), (int) (activity.getResources().getDisplayMetrics().heightPixels * .45f));
            ScrollView scroll = new BoundedScrollView(activity, maxControlHeight);
            scroll.setFillViewport(true);
            LinearLayout modules = new LinearLayout(activity);
            modules.setOrientation(LinearLayout.VERTICAL);
            addModules(modules);
            scroll.addView(modules, new ScrollView.LayoutParams(-1, -2));
            LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, -2);
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
            card.setBackground(UniversalOverlayViews.background(config.background, config.outline, false));
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

        private void addModules(LinearLayout modules) {
            boolean hasStatistics = config.systemTime || config.fps || config.sessionTime;
            boolean hasActivity = config.keepAwake || config.fullscreen || config.screenshots;
            if (hasStatistics) {
                addSectionLabel(modules, "Statistic modules");
                if (config.systemTime) addStatistic(modules, new SystemTimeModule());
                if (config.fps) addStatistic(modules, new FpsModule());
                if (config.sessionTime) addStatistic(modules, new SessionTimeModule(sessionStartElapsed));
            }
            if (hasActivity) {
                addSectionLabel(modules, "Activity modules");
                if (config.fullscreen) addActivityModule(modules, new FullscreenModule());
                if (config.keepAwake) addActivityModule(modules, new KeepAwakeModule());
                if (config.screenshots) addActivityModule(modules, new ScreenshotsModule());
            }
        }

        private void addSectionLabel(LinearLayout parent, String label) {
            TextView separator = text("—  " + label + "  —", 13, config.outline);
            separator.setAlpha(.65f);
            separator.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(32));
            params.topMargin = dp(4);
            parent.addView(separator, params);
        }

        private void addActivityModule(LinearLayout controls, UniversalOverlayActivityModule feature) {
            final boolean initial;
            try {
                Boolean remembered = rememberedState(feature.key());
                initial = remembered != null ? remembered
                        : feature.initiallyEnabled(activity, originalWindowFlags, originalSystemUi);
                if (remembered != null) {
                    if (!feature.setEnabled(activity, remembered, originalWindowFlags, originalSystemUi)) return;
                }
            } catch (RuntimeException ignored) {
                return;
            }
            activityModules.add(feature);
            addControlRow(controls, feature, initial, checked -> {
                try {
                    boolean applied = feature.setEnabled(activity, checked, originalWindowFlags, originalSystemUi);
                    rememberState(feature.key(), applied && checked);
                    return applied;
                } catch (RuntimeException ignored) {
                    // Feature controls are independent; a failure here must not crash the host.
                    rememberState(feature.key(), false);
                    return false;
                }
            });
        }

        private void addStatistic(LinearLayout parent, UniversalOverlayStatisticModule module) {
            String key = module.key();
            String label = module.label();
            String description = module.description();
            statistics.add(module);
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(6), 0, dp(6));
            row.setMinimumHeight(dp(64));

            LinearLayout copy = new LinearLayout(activity);
            copy.setOrientation(LinearLayout.VERTICAL);
            TextView title = text(label, 16, config.outline);
            title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            copy.addView(title, new LinearLayout.LayoutParams(-1, -2));
            TextView details = text(description, 13, config.outline);
            details.setAlpha(.82f);
            copy.addView(details, new LinearLayout.LayoutParams(-1, -2));
            TextView valueView = text("Disabled", 12, config.outline);
            valueView.setAlpha(.72f);
            copy.addView(valueView, new LinearLayout.LayoutParams(-1, -2));
            row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));

            CheckBox control = new CheckBox(activity);
            control.setChecked(Boolean.TRUE.equals(rememberedModuleState(key)));
            control.setContentDescription(label);
            module.bind(valueView, control);
            module.setEnabled(control.isChecked(), false);
            control.setOnCheckedChangeListener((button, checked) -> {
                rememberModuleState(key, checked);
                boolean applied = module.setEnabled(checked, menuVisible);
                if (!applied && checked) {
                    rememberModuleState(key, false);
                    module.setChecked(false);
                    Toast.makeText(activity, label + " could not be enabled", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(activity, label + " is " + (checked ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
                }
            });
            row.addView(control, new LinearLayout.LayoutParams(-2, -2));
            row.setClickable(true);
            row.setOnClickListener(v -> control.setChecked(!control.isChecked()));
            parent.addView(row, new LinearLayout.LayoutParams(-1, -2));
        }

        private void applyRememberedStates() {
            if (detached) return;
            for (UniversalOverlayActivityModule feature : activityModules) {
                Boolean remembered = rememberedState(feature.key());
                if (remembered == null) continue;
                try {
                    CheckBox control = featureControls.get(feature.key());
                    if (!feature.setEnabled(activity, remembered, originalWindowFlags, originalSystemUi)) {
                        rememberState(feature.key(), false);
                        if (control != null) control.setChecked(false);
                        continue;
                    }
                    if (control != null && control.isChecked() != remembered) {
                        control.setChecked(remembered);
                    }
                } catch (RuntimeException ignored) {
                    // A failed feature must not prevent the remaining controls from syncing.
                }
            }
            for (UniversalOverlayStatisticModule module : statistics) {
                Boolean remembered = rememberedModuleState(module.key());
                if (remembered == null) continue;
                module.setChecked(remembered);
                if (!module.setEnabled(remembered, menuVisible)) {
                    rememberModuleState(module.key(), false);
                    module.setChecked(false);
                }
            }
        }

        private void addControlRow(LinearLayout parent, UniversalOverlayActivityModule feature, boolean initial, final Toggle toggle) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(6), 0, dp(6));
            row.setMinimumHeight(dp(64));

            LinearLayout copy = new LinearLayout(activity);
            copy.setOrientation(LinearLayout.VERTICAL);
            TextView title = text(feature.label(), 16, config.outline);
            title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            copy.addView(title, new LinearLayout.LayoutParams(-1, -2));
            TextView description = text(feature.description(), 13, config.outline);
            description.setAlpha(.82f);
            LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(-1, -2);
            descriptionParams.topMargin = dp(2);
            copy.addView(description, descriptionParams);
            row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));

            CheckBox control = new CheckBox(activity);
            control.setChecked(initial);
            control.setContentDescription(feature.label());
            final android.widget.CompoundButton.OnCheckedChangeListener[] listener = new android.widget.CompoundButton.OnCheckedChangeListener[1];
            listener[0] = (button, checked) -> {
                boolean applied = toggle.changed(checked);
                if (!applied) {
                    control.setOnCheckedChangeListener(null);
                    control.setChecked(!checked);
                    control.setOnCheckedChangeListener(listener[0]);
                    Toast.makeText(activity, feature.label() + " could not be " + (checked ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(activity, feature.label() + " is " + (checked ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
                }
            };
            control.setOnCheckedChangeListener(listener[0]);
            featureControls.put(feature.key(), control);
            row.addView(control, new LinearLayout.LayoutParams(-2, -2));
            row.setClickable(true);
            row.setOnClickListener(v -> control.setChecked(!control.isChecked()));
            parent.addView(row, new LinearLayout.LayoutParams(-1, -2));
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
            action.setBackground(UniversalOverlayViews.selectableBackground(activity));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(48), 1f);
            row.addView(action, params);
        }

        private void toggleMenu() {
            if (fullyClosed) return;
            menuVisible = !menuVisible;
            if (menuVisible) {
                menuLayer.setVisibility(View.VISIBLE);
                for (UniversalOverlayStatisticModule module : statistics) {
                    if (module.isEnabled() && !module.startSafely()) {
                        rememberModuleState(module.key(), false);
                        module.setChecked(false);
                        Toast.makeText(activity, module.label() + " is unavailable", Toast.LENGTH_SHORT).show();
                    }
                }
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
                if (!menuVisible) {
                    menuLayer.setVisibility(View.GONE);
                    for (UniversalOverlayStatisticModule module : statistics) module.stopSafely();
                }
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
                    else {
                        sharedButtonPositionInitialized = true;
                        sharedButtonX = Math.max(0, (int) view.getX());
                        sharedButtonY = Math.max(0, (int) view.getY());
                        view.animate().alpha(config.opacity).setDuration(180).start();
                    }
                    return true;
                default: return true;
            }
        }

        private int dp(int value) { return (int) (value * activity.getResources().getDisplayMetrics().density + .5f); }

    }

    private interface Toggle { boolean changed(boolean checked); }

    private static final class BoundedScrollView extends ScrollView {
        private final int maxHeight;

        BoundedScrollView(android.content.Context context, int maxHeight) {
            super(context);
            this.maxHeight = maxHeight;
        }

        @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            setMeasuredDimension(getMeasuredWidth(), Math.min(getMeasuredHeight(), maxHeight));
        }
    }

    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(value, max)); }

}
