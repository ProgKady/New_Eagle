package com.eagle.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.prefs.Preferences;
import javafx.scene.Scene;

/**
 * Manages application-wide theme and persists the user's choice.
 *
 * The toggle knob (Welcome/Editor/Builder screens) only ever flips between
 * the two base modes (DARK / LIGHT) for that simple on/off control, but the
 * Settings dialog exposes the full named theme list below so power users get
 * real color variety without breaking the simple toggle elsewhere.
 */
public class ThemeManager {

    public enum Theme { DARK, LIGHT }

    /** A named theme: which base mode it inherits from (for the toggle-knob position) and its stylesheet. */
    public static class NamedTheme {
        public final String name;
        public final Theme baseMode;
        public final String cssPath;

        NamedTheme(String name, Theme baseMode, String cssPath) {
            this.name = name;
            this.baseMode = baseMode;
            this.cssPath = cssPath;
        }
    }

    private static ThemeManager instance;
    private Theme currentTheme;
    private String currentThemeName;
    private final Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);

    private static final String BASE_CSS = "/com/eagle/css/base.css";
    private static final String RICHTEXTFX_OVERRIDES_CSS = "/com/eagle/css/richtextfx-overrides.css";

    private final Map<String, NamedTheme> namedThemes = new LinkedHashMap<>();

    private final java.util.List<Scene> registeredScenes = new java.util.ArrayList<>();

    private ThemeManager() {
        registerBuiltInThemes();

        String savedMode = prefs.get("theme", "DARK");
        currentTheme = Theme.valueOf(savedMode);

        String savedName = prefs.get("themeName", currentTheme == Theme.DARK ? "Dark (Default)" : "Light (Default)");
        currentThemeName = namedThemes.containsKey(savedName) ? savedName
                : (currentTheme == Theme.DARK ? "Dark (Default)" : "Light (Default)");
    }

    private void registerBuiltInThemes() {
        namedThemes.put("Dark (Default)", new NamedTheme("Dark (Default)", Theme.DARK, "/com/eagle/css/dark-theme.css"));
        namedThemes.put("Light (Default)", new NamedTheme("Light (Default)", Theme.LIGHT, "/com/eagle/css/light-theme.css"));
        namedThemes.put("Midnight Violet", new NamedTheme("Midnight Violet", Theme.DARK, "/com/eagle/css/theme-midnight-violet.css"));
        namedThemes.put("Sandstone Light", new NamedTheme("Sandstone Light", Theme.LIGHT, "/com/eagle/css/theme-sandstone-light.css"));
    }

    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    public void applyTheme(Scene scene) {
        if (!registeredScenes.contains(scene)) {
            registeredScenes.add(scene);
        }
        scene.getStylesheets().clear();

        // RichTextFX ships its own base CSS inside its jar; load it first so
        // our overrides (colors/fonts) layer on top correctly.
        java.net.URL richTextFxCss = org.fxmisc.richtext.CodeArea.class.getResource("/org/fxmisc/richtext/rich-text.css");
        if (richTextFxCss != null) {
            scene.getStylesheets().add(richTextFxCss.toExternalForm());
        }
        scene.getStylesheets().add(getClass().getResource(RICHTEXTFX_OVERRIDES_CSS).toExternalForm());
        scene.getStylesheets().add(getClass().getResource(BASE_CSS).toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/icons/icons.css").toExternalForm());

        NamedTheme theme = namedThemes.get(currentThemeName);
        if (theme == null) {
            theme = namedThemes.get(currentTheme == Theme.DARK ? "Dark (Default)" : "Light (Default)");
        }
        java.net.URL themeCss = getClass().getResource(theme.cssPath);
        if (themeCss != null) {
            scene.getStylesheets().add(themeCss.toExternalForm());
        }
    }

    /** Flips between the two base modes using each mode's "Default" theme — used by the simple toggle knob. */
    public void toggleTheme() {
        currentTheme = (currentTheme == Theme.DARK) ? Theme.LIGHT : Theme.DARK;
        currentThemeName = currentTheme == Theme.DARK ? "Dark (Default)" : "Light (Default)";
        persistAndReapply();
    }

    /** Switches to a specific named theme (from the Settings dialog's theme picker). */
    public void setThemeByName(String name) {
        NamedTheme theme = namedThemes.get(name);
        if (theme == null) return;
        currentThemeName = name;
        currentTheme = theme.baseMode;
        persistAndReapply();
    }

    private void persistAndReapply() {
        prefs.put("theme", currentTheme.name());
        prefs.put("themeName", currentThemeName);
        registeredScenes.removeIf(s -> s == null);
        for (Scene scene : registeredScenes) {
            applyTheme(scene);
        }
    }

    public java.util.List<String> getAvailableThemeNames() {
        return new java.util.ArrayList<>(namedThemes.keySet());
    }

    public String getCurrentThemeName() {
        return currentThemeName;
    }

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    public boolean isDark() {
        return currentTheme == Theme.DARK;
    }
}
