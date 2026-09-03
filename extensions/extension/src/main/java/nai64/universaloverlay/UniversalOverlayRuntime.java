package nai64.universaloverlay;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Paint;
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
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;

import nai64.universaloverlay.modules.FullscreenModule;
import nai64.universaloverlay.modules.KeepAwakeModule;
import nai64.universaloverlay.modules.ScreenshotsModule;
import nai64.universaloverlay.modules.UniversalOverlayActivityModule;
import nai64.universaloverlay.modules.UniversalOverlayHookModule;
import nai64.universaloverlay.modules.UniversalOverlayModule;
import nai64.universaloverlay.modules.UniversalOverlayStatisticModule;
import nai64.universaloverlay.modules.FpsModule;
import nai64.universaloverlay.modules.SessionTimeModule;
import nai64.universaloverlay.modules.SystemTimeModule;
import nai64.universaloverlay.modules.BatteryStatusModule;
import nai64.universaloverlay.modules.AppMemoryModule;
import nai64.universaloverlay.modules.NetworkStatusModule;
import nai64.universaloverlay.modules.DeviceInformationModule;
import nai64.universaloverlay.modules.DeviceTemperatureModule;
import nai64.universaloverlay.modules.AppBrightnessModule;
import nai64.universaloverlay.modules.RotationModeModule;
import nai64.universaloverlay.modules.AppAudioMuteModule;
import nai64.universaloverlay.modules.DisableHapticsModule;
import nai64.universaloverlay.modules.DisableAnimationsModule;

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
    private static boolean globallyClosed;
    private static Application installedApplication;
    private static UniversalOverlayLifecycle lifecycleCallbacks;
    private static UniversalOverlayConfig configuration;
    private static Boolean keepAwakeState;
    private static Boolean fullscreenState;
    private static Boolean screenshotsState;
    private static final Map<String, Boolean> MODULE_STATES = new java.util.HashMap<>();
    private static final Map<String, Boolean> MONITOR_STATES = new java.util.HashMap<>();
    private static final Map<String, Boolean> HOOK_STATES = new java.util.HashMap<>();
    private static long sessionStartElapsed;
    private static boolean sharedButtonPositionInitialized;
    private static int sharedButtonX;
    private static int sharedButtonY;
    private static Float appBrightnessState;
    private static Integer rotationModeState;

    private UniversalOverlayRuntime() { }

    /** Primary entry point, called once from Application.onCreate(). */
    public static synchronized void install(Application application, String encodedConfig) {
        if (application == null || globallyClosed) return;
        configuration = UniversalOverlayConfig.decode(encodedConfig);
        if (sessionStartElapsed == 0) sessionStartElapsed = SystemClock.elapsedRealtime();
        if (!callbacksRegistered) {
            installedApplication = application;
            lifecycleCallbacks = new UniversalOverlayLifecycle();
            application.registerActivityLifecycleCallbacks(lifecycleCallbacks);
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
        if (configuration == null || globallyClosed) return;
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

    static synchronized void pauseActivity(Activity activity) {
        Controller controller = CONTROLLERS.get(activity);
        if (controller != null) controller.pause();
    }

    private static synchronized void closeGlobally() {
        if (globallyClosed) return;
        globallyClosed = true;
        for (Controller controller : new ArrayList<>(CONTROLLERS.values())) {
            if (controller != null) controller.detach();
        }
        CONTROLLERS.clear();
        MODULE_STATES.clear();
        MONITOR_STATES.clear();
        HOOK_STATES.clear();
        if (installedApplication != null && lifecycleCallbacks != null) {
            try { installedApplication.unregisterActivityLifecycleCallbacks(lifecycleCallbacks); }
            catch (RuntimeException ignored) { }
        }
        installedApplication = null;
        lifecycleCallbacks = null;
        callbacksRegistered = false;
        configuration = null;
        sessionStartElapsed = 0;
        sharedButtonPositionInitialized = false;
        appBrightnessState = null;
        rotationModeState = null;
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
        private final View brightnessDimLayer;
        private final List<UniversalOverlayActivityModule> activityModules = new ArrayList<>();
        private final List<UniversalOverlayHookModule> hookModules = new ArrayList<>();
        private final List<UniversalOverlayStatisticModule> statistics = new ArrayList<>();
        private final Map<String, CheckBox> featureControls = new java.util.HashMap<>();
        private final Map<String, List<TextView>> statisticMonitors = new java.util.HashMap<>();
        private final int monitorWidth;
        private final int monitorHeight;
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
            root.setFocusableInTouchMode(true);
            root.setOnKeyListener((view, keyCode, event) -> {
                if (keyCode == android.view.KeyEvent.KEYCODE_BACK
                        && event.getAction() == android.view.KeyEvent.ACTION_UP && menuVisible) {
                    closeMenu();
                    return true;
                }
                return false;
            });
            brightnessDimLayer = new View(activity);
            brightnessDimLayer.setBackgroundColor(Color.BLACK);
            brightnessDimLayer.setClickable(false);
            brightnessDimLayer.setFocusable(false);
            brightnessDimLayer.setAlpha(0f);
            brightnessDimLayer.setOnTouchListener((v, event) -> false);
            root.addView(brightnessDimLayer, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            Paint monitorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            monitorPaint.setTextSize(dp(12));
            Paint.FontMetrics metrics = monitorPaint.getFontMetrics();
            String timeExample = "24".equals(config.timeFormat) ? "ST: 00:00" : "ST: 00:00 AM";
            String temperatureSuffix = "fahrenheit".equals(config.temperatureFormat) ? "F"
                    : ("kelvin".equals(config.temperatureFormat) ? "K" : "C");
            String[] monitorExamples = {
                    timeExample, "FPS: ~999", "AST: 00:00:00", "BAT: 100%", "MEM: 9999 MB",
                    "↓IT: 999.9 MB", "↑OT: 999.9 MB", "TMP: 99.9 " + temperatureSuffix,
            };
            float widestMonitor = 0f;
            for (String example : monitorExamples) widestMonitor = Math.max(widestMonitor, monitorPaint.measureText(example));
            monitorWidth = Math.round(((int) Math.ceil(widestMonitor) + dp(24)) * config.monitorScale);
            monitorHeight = Math.round(((int) Math.ceil(metrics.bottom - metrics.top) + dp(12)) * config.monitorScale);
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
                root.post(this::updateMonitorLayout);
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

        void pause() {
            if (detached) return;
            menuVisible = false;
            menuLayer.setVisibility(View.GONE);
            confirmationLayer.setVisibility(View.GONE);
            floatingButton.setAlpha(config.opacity);
            for (UniversalOverlayStatisticModule module : statistics) {
                module.setMenuVisible(false);
                module.setEnabled(module.isEnabled(), false);
            }
            setMonitorAlphaImmediate(config.opacity);
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
            for (UniversalOverlayHookModule hook : hookModules) {
                try {
                    hook.restore(activity, originalWindowFlags, originalSystemUi);
                } catch (RuntimeException ignored) {
                    // Hook cleanup is independent from Activity module cleanup.
                }
            }
            activityModules.clear();
            hookModules.clear();
            statistics.clear();
            featureControls.clear();
            statisticMonitors.clear();
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

        private void createStatisticMonitors(UniversalOverlayStatisticModule module) {
            List<TextView> monitors = new ArrayList<>();
            for (int i = 0; i < module.monitorCount(); i++) {
                TextView monitor = text("", 12, config.outline);
                monitor.setGravity(Gravity.CENTER);
                monitor.setPadding(dp(3), 0, dp(3), 0);
                monitor.setBackground(UniversalOverlayViews.background(config.background, config.outline, false));
                monitor.setClickable(false);
                monitor.setFocusable(false);
                monitor.setFocusableInTouchMode(false);
                monitor.setContentDescription(module.label());
                monitor.setVisibility(View.GONE);
                monitor.setOnTouchListener((v, event) -> false);
                root.addView(monitor, new FrameLayout.LayoutParams(monitorWidth, monitorHeight));
                monitors.add(monitor);
            }
            statisticMonitors.put(module.key(), monitors);
            module.bindMonitors(monitors);
        }

        private boolean shouldStatisticsRun(UniversalOverlayStatisticModule module) {
            // Menu-only statistics sample only while visible. Monitor statistics continue only
            // when their monitor is enabled, avoiding background work for hidden modules.
            return menuVisible || (module.isMonitorEnabled() && module.monitorCount() > 0);
        }

        private void syncStatisticExecution() {
            for (UniversalOverlayStatisticModule module : statistics) {
                module.setEnabled(module.isEnabled(), shouldStatisticsRun(module));
            }
        }

        private void updateStatisticMonitor(UniversalOverlayStatisticModule module) {
            List<TextView> monitors = statisticMonitors.get(module.key());
            if (monitors == null) return;
            for (TextView monitor : monitors) {
                monitor.setVisibility(config.statisticMonitorPosition != 0
                        && module.isMonitorEnabled() && module.isEnabled()
                        ? View.VISIBLE : View.GONE);
                monitor.setAlpha(menuVisible ? 0f : config.opacity);
            }
        }

        private void updateMonitorLayout() {
            int buttonX = Math.round(floatingButton.getX());
            int buttonY = Math.round(floatingButton.getY());
            int count = 0;
            for (UniversalOverlayStatisticModule module : statistics) {
                List<TextView> monitors = statisticMonitors.get(module.key());
                if (monitors != null) for (TextView monitor : monitors) {
                    if (monitor.getVisibility() == View.VISIBLE) count++;
                }
            }
            int columns = Math.max(1, config.monitorColumns);
            int spacing = dp(4);
            int rows = (count + columns - 1) / columns;
            int stackHeight = rows * monitorHeight + Math.max(0, rows - 1) * spacing;
            int monitorStartY = config.statisticMonitorPosition == 1
                    ? buttonY - spacing - stackHeight
                    : buttonY + floatingButton.getHeight() + spacing;
            monitorStartY = Math.max(0, Math.min(monitorStartY, Math.max(0, root.getHeight() - stackHeight)));
            int gridWidth = columns * monitorWidth + Math.max(0, columns - 1) * spacing;
            int gridStartX = buttonX + (floatingButton.getWidth() - gridWidth) / 2;
            gridStartX = Math.max(0, Math.min(gridStartX, Math.max(0, root.getWidth() - gridWidth)));
            // Follow the same stable order used by addModules.
            int slot = 0;
            for (UniversalOverlayStatisticModule module : statistics) {
                List<TextView> monitors = statisticMonitors.get(module.key());
                if (monitors == null) continue;
                for (TextView monitor : monitors) {
                    if (monitor.getVisibility() != View.VISIBLE) continue;
                    int row = slot / columns;
                    int column = slot % columns;
                    int itemsInRow = Math.min(columns, count - row * columns);
                    int rowWidth = itemsInRow * monitorWidth + Math.max(0, itemsInRow - 1) * spacing;
                    int rowStartX = buttonX + (floatingButton.getWidth() - rowWidth) / 2;
                    rowStartX = Math.max(0, Math.min(rowStartX, Math.max(0, root.getWidth() - rowWidth)));
                    monitor.setX(rowStartX + column * (monitorWidth + spacing));
                    monitor.setY(monitorStartY + row * (monitorHeight + spacing));
                    slot++;
                }
            }
        }

        private void setMonitorAlpha(float alpha) {
            for (List<TextView> monitors : statisticMonitors.values()) {
                for (TextView monitor : monitors) if (monitor.getVisibility() == View.VISIBLE) {
                    monitor.animate().alpha(alpha).setDuration(180).start();
                }
            }
        }

        private void setMonitorAlphaImmediate(float alpha) {
            for (List<TextView> monitors : statisticMonitors.values()) {
                for (TextView monitor : monitors) if (monitor.getVisibility() == View.VISIBLE) {
                    monitor.animate().cancel();
                    monitor.setAlpha(alpha);
                }
            }
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
                    Math.max(dp(1), Math.min(dp(560), activity.getResources().getDisplayMetrics().widthPixels - dp(40))),
                    ViewGroup.LayoutParams.WRAP_CONTENT,
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

            int maxControlHeight = Math.max(dp(120), Math.min(dp(280),
                    (int) (activity.getResources().getDisplayMetrics().heightPixels * .45f)) - dp(8));
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
            boolean hasStatistics = config.systemTime || config.fps || config.sessionTime
                    || config.batteryStatus || config.appMemory || config.networkStatus
                    || config.deviceInformation || config.deviceTemperature;
            boolean hasActivity = config.keepAwake || config.fullscreen || config.screenshots
                    || config.appBrightness || config.rotationMode || config.appAudioMute;
            boolean hasHooks = config.disableHaptics || config.disableAnimations;
            if (hasStatistics) {
                addSectionLabel(modules, "Statistic modules");
                if (config.deviceInformation) addStatisticSafely(modules, () -> new DeviceInformationModule(activity));
                if (config.fps) addStatisticSafely(modules, FpsModule::new);
                if (config.deviceTemperature) addStatisticSafely(modules, () -> new DeviceTemperatureModule(activity, config.temperatureFormat));
                if (config.systemTime) addStatisticSafely(modules, () -> new SystemTimeModule(config.timeFormat));
                if (config.sessionTime) addStatisticSafely(modules, () -> new SessionTimeModule(sessionStartElapsed));
                if (config.batteryStatus) addStatisticSafely(modules, () -> new BatteryStatusModule(activity));
                if (config.appMemory) addStatisticSafely(modules, () -> new AppMemoryModule(activity));
                if (config.networkStatus) addStatisticSafely(modules, () -> new NetworkStatusModule(activity));
            }
            if (hasActivity) {
                addSectionLabel(modules, "Activity modules");
                if (config.keepAwake) addActivityModuleSafely(modules, KeepAwakeModule::new);
                if (config.fullscreen) addActivityModuleSafely(modules, FullscreenModule::new);
                if (config.screenshots) addActivityModuleSafely(modules, ScreenshotsModule::new);
                if (config.appBrightness) addActivityModuleSafely(modules, AppBrightnessModule::new);
                if (config.rotationMode) addActivityModuleSafely(modules, RotationModeModule::new);
                if (config.appAudioMute) addActivityModuleSafely(modules, AppAudioMuteModule::new);
            }
            if (hasHooks) {
                addSectionLabel(modules, "Hook modules");
                if (config.disableHaptics) addHookModuleSafely(modules, DisableHapticsModule::new);
                if (config.disableAnimations) addHookModuleSafely(modules, DisableAnimationsModule::new);
            }
        }

        private void addActivityModuleSafely(LinearLayout parent, ActivityModuleFactory factory) {
            try {
                addActivityModule(parent, factory.create());
            } catch (RuntimeException ignored) {
                // A module constructor or UI setup failure must not hide other modules.
            }
        }

        private void addStatisticSafely(LinearLayout parent, StatisticModuleFactory factory) {
            try {
                addStatistic(parent, factory.create());
            } catch (RuntimeException ignored) {
                // A module constructor or UI setup failure must not hide other modules.
            }
        }

        private void addHookModuleSafely(LinearLayout parent, HookModuleFactory factory) {
            try {
                addHookModule(parent, factory.create());
            } catch (RuntimeException ignored) {
                // A hook constructor or UI setup failure must not hide other modules.
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
            if (feature instanceof AppBrightnessModule) {
                addBrightnessModule(controls, (AppBrightnessModule) feature);
                return;
            }
            if (feature instanceof RotationModeModule) {
                addRotationModule(controls, (RotationModeModule) feature);
                return;
            }
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

        private void addHookModule(LinearLayout controls, UniversalOverlayHookModule hook) {
            final boolean initial;
            try {
                Boolean remembered = HOOK_STATES.get(hook.key());
                initial = remembered != null ? remembered
                        : hook.initiallyEnabled(activity, originalWindowFlags, originalSystemUi);
                if (remembered != null && !hook.setEnabled(activity, remembered, originalWindowFlags, originalSystemUi)) {
                    HOOK_STATES.put(hook.key(), false);
                    return;
                }
            } catch (RuntimeException ignored) {
                return;
            }
            hookModules.add(hook);
            addControlRow(controls, hook, initial, checked -> {
                try {
                    boolean applied = hook.setEnabled(activity, checked, originalWindowFlags, originalSystemUi);
                    HOOK_STATES.put(hook.key(), applied && checked);
                    return applied;
                } catch (RuntimeException ignored) {
                    HOOK_STATES.put(hook.key(), false);
                    return false;
                }
            });
        }

        private void addBrightnessModule(LinearLayout parent, AppBrightnessModule module) {
            activityModules.add(module);
            module.initiallyEnabled(activity, originalWindowFlags, originalSystemUi);
            module.bindDimLayer(brightnessDimLayer);
            Float remembered = appBrightnessState;
            if (remembered != null) module.apply(activity, remembered);
            LinearLayout row = moduleRow(module.label(), module.description());
            SeekBar slider = new SeekBar(activity);
            slider.setMax(100);
            slider.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            slider.setProgress(Math.round((remembered == null ? module.current(activity) : remembered) * 100f));
            slider.setContentDescription(module.label());
            slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                    try {
                        float brightness = progress / 100f;
                        appBrightnessState = brightness;
                        module.apply(activity, brightness);
                    }
                    catch (RuntimeException ignored) { }
                }
                @Override public void onStartTrackingTouch(SeekBar bar) { }
                @Override public void onStopTrackingTouch(SeekBar bar) { }
            });
            row.addView(slider, new LinearLayout.LayoutParams(-1, -2));
            parent.addView(row, new LinearLayout.LayoutParams(-1, -2));
        }

        private void addRotationModule(LinearLayout parent, RotationModeModule module) {
            activityModules.add(module);
            module.initiallyEnabled(activity, originalWindowFlags, originalSystemUi);
            Integer remembered = rotationModeState;
            if (remembered != null) module.apply(activity, remembered);
            LinearLayout row = moduleRow(module.label(), module.description());
            Spinner spinner = new Spinner(activity);
            String[] labels = {"System", "Portrait", "Landscape"};
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item, labels) {
                @Override public View getView(int position, View convertView, android.view.ViewGroup parentView) {
                    TextView view = (TextView) super.getView(position, convertView, parentView);
                    view.setTextColor(config.outline);
                    return view;
                }
                @Override public View getDropDownView(int position, View convertView, android.view.ViewGroup parentView) {
                    TextView view = (TextView) super.getDropDownView(position, convertView, parentView);
                    view.setTextColor(config.outline);
                    view.setBackgroundColor(config.background);
                    return view;
                }
            };
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            spinner.setBackground(UniversalOverlayViews.background(config.background, config.outline, false));
            if (android.os.Build.VERSION.SDK_INT >= 16) {
                spinner.setPopupBackgroundDrawable(UniversalOverlayViews.background(config.background, config.outline, false));
            }
            int current = remembered == null ? module.current(activity) : remembered;
            spinner.setSelection(current == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ? 1
                    : (current == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ? 2 : 0));
            spinner.setContentDescription(module.label());
            spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(android.widget.AdapterView<?> parentView, View view, int position, long id) {
                    int mode = position == 1 ? android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            : (position == 2 ? android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            : android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                    try {
                        module.apply(activity, mode);
                        rotationModeState = mode;
                    } catch (RuntimeException ignored) { }
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> parentView) { }
            });
            row.addView(spinner, new LinearLayout.LayoutParams(-1, -2));
            parent.addView(row, new LinearLayout.LayoutParams(-1, -2));
        }

        private LinearLayout moduleRow(String label, String details) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, dp(6), 0, dp(6));
            TextView title = text(label, 16, config.outline);
            title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            row.addView(title, new LinearLayout.LayoutParams(-1, -2));
            TextView description = text(details, 13, config.outline);
            description.setAlpha(.82f);
            row.addView(description, new LinearLayout.LayoutParams(-1, -2));
            return row;
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

            CheckBox monitorControl = null;
            if (module.monitorCount() > 0) {
                monitorControl = new CheckBox(activity);
                Boolean rememberedMonitor = MONITOR_STATES.get(key);
                monitorControl.setChecked(rememberedMonitor != null ? rememberedMonitor : config.enableMonitorsOnLaunch);
                monitorControl.setText("Monitor");
                styleCheckBox(monitorControl);
                monitorControl.setContentDescription(label + " monitor");
                module.setMonitorEnabled(monitorControl.isChecked());
                monitorControl.setOnCheckedChangeListener((button, checked) -> {
                    MONITOR_STATES.put(key, checked);
                    module.setMonitorEnabled(checked);
                    updateStatisticMonitor(module);
                    root.post(this::updateMonitorLayout);
                    Toast.makeText(activity, label + " monitor is " + (checked ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
                });
            } else {
                module.setMonitorEnabled(false);
            }

            CheckBox control = new CheckBox(activity);
            Boolean remembered = rememberedModuleState(key);
            control.setChecked(remembered != null ? remembered : config.activateStatisticsOnLaunch);
            control.setText("Active");
            styleCheckBox(control);
            control.setContentDescription(label + " active");
            module.bind(valueView, control);
            createStatisticMonitors(module);
            boolean requested = control.isChecked();
            module.setMenuVisible(menuVisible);
            boolean applied = module.setEnabled(requested, shouldStatisticsRun(module));
            if (requested && !applied) {
                control.setChecked(false);
                rememberModuleState(key, false);
                module.setChecked(false);
            }
            updateStatisticMonitor(module);
            control.setOnCheckedChangeListener((button, checked) -> {
                rememberModuleState(key, checked);
                boolean toggleApplied = module.setEnabled(checked, shouldStatisticsRun(module));
                if (!toggleApplied && checked) {
                    rememberModuleState(key, false);
                    module.setChecked(false);
                    Toast.makeText(activity, label + " could not be enabled", Toast.LENGTH_SHORT).show();
                } else {
                    updateStatisticMonitor(module);
                    root.post(this::updateMonitorLayout);
                    Toast.makeText(activity, label + " is " + (checked ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
                }
            });
            if (monitorControl != null) row.addView(monitorControl, new LinearLayout.LayoutParams(-2, -2));
            row.addView(control, new LinearLayout.LayoutParams(-2, -2));
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
                Boolean rememberedMonitor = MONITOR_STATES.get(module.key());
                if (rememberedMonitor != null) module.setMonitorEnabled(rememberedMonitor);
                Boolean remembered = rememberedModuleState(module.key());
                if (remembered == null) continue;
                module.setChecked(remembered);
                if (!module.setEnabled(remembered, shouldStatisticsRun(module))) {
                    rememberModuleState(module.key(), false);
                    module.setChecked(false);
                }
                updateStatisticMonitor(module);
            }
            for (UniversalOverlayHookModule hook : hookModules) {
                Boolean remembered = HOOK_STATES.get(hook.key());
                if (remembered == null) continue;
                try {
                    if (!hook.setEnabled(activity, remembered, originalWindowFlags, originalSystemUi)) {
                        HOOK_STATES.put(hook.key(), false);
                        continue;
                    }
                    CheckBox control = featureControls.get(hook.key());
                    if (control != null && control.isChecked() != remembered) control.setChecked(remembered);
                } catch (RuntimeException ignored) {
                    HOOK_STATES.put(hook.key(), false);
                }
            }
        }

        private void addControlRow(LinearLayout parent, UniversalOverlayModule feature, boolean initial, final Toggle toggle) {
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
            styleCheckBox(control);
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

        private void styleCheckBox(CheckBox control) {
            control.setTextColor(config.outline);
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                control.setButtonTintList(ColorStateList.valueOf(config.outline));
            }
        }

        private void addAction(LinearLayout row, String label, View.OnClickListener listener) {
            TextView action = text(label, 14, config.outline);
            action.setGravity(Gravity.CENTER);
            action.setContentDescription(label);
            action.setOnClickListener(listener);
            action.setBackground(UniversalOverlayViews.selectableBackground(activity));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(56), 1f);
            row.addView(action, params);
        }

        private void toggleMenu() {
            if (fullyClosed) return;
            menuVisible = !menuVisible;
            for (UniversalOverlayStatisticModule module : statistics) module.setMenuVisible(menuVisible);
            if (menuVisible) {
                root.requestFocus();
                menuLayer.setVisibility(View.VISIBLE);
                for (UniversalOverlayStatisticModule module : statistics) {
                    if (module.isEnabled() && !module.startSafely()) {
                        rememberModuleState(module.key(), false);
                        module.setChecked(false);
                        Toast.makeText(activity, module.label() + " is unavailable", Toast.LENGTH_SHORT).show();
                    }
                    updateStatisticMonitor(module);
                }
                menuScrim.animate().cancel();
                menuScrim.setAlpha(0f);
                menuScrim.animate().alpha(1f).setDuration(180).start();
            } else {
                root.clearFocus();
                hideMenuLayer();
            }
            syncStatisticExecution();
            floatingButton.animate().alpha(menuVisible ? 0f : config.opacity).setDuration(180).start();
            setMonitorAlpha(menuVisible ? 0f : config.opacity);
            root.post(this::updateMonitorLayout);
        }

        private void closeMenu() {
            menuVisible = false;
            root.clearFocus();
            for (UniversalOverlayStatisticModule module : statistics) module.setMenuVisible(false);
            hideMenuLayer();
            syncStatisticExecution();
            floatingButton.animate().alpha(config.opacity).setDuration(180).start();
            setMonitorAlpha(config.opacity);
            root.post(this::updateMonitorLayout);
        }

        private void hideMenuLayer() {
            menuScrim.animate().cancel();
            menuScrim.animate().alpha(0f).setDuration(180).withEndAction(() -> {
                if (!menuVisible) {
                    menuLayer.setVisibility(View.GONE);
                    for (UniversalOverlayStatisticModule module : statistics) updateStatisticMonitor(module);
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
            closeGlobally();
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
                        setMonitorAlphaImmediate(1f);
                        updateMonitorLayout();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!dragged) view.performClick();
                    else {
                        sharedButtonPositionInitialized = true;
                        sharedButtonX = Math.max(0, (int) view.getX());
                        sharedButtonY = Math.max(0, (int) view.getY());
                        view.animate().alpha(config.opacity).setDuration(180).start();
                        setMonitorAlpha(config.opacity);
                        updateMonitorLayout();
                    }
                    return true;
                default: return true;
            }
        }

        private int dp(int value) { return (int) (value * activity.getResources().getDisplayMetrics().density + .5f); }

    }

    private interface Toggle { boolean changed(boolean checked); }

    private interface ActivityModuleFactory {
        UniversalOverlayActivityModule create();
    }

    private interface StatisticModuleFactory {
        UniversalOverlayStatisticModule create();
    }

    private interface HookModuleFactory {
        UniversalOverlayHookModule create();
    }

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
