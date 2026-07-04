package com.eagle.util;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;

public class EditorSettings {

    private static final Preferences prefs = Preferences.userNodeForPackage(EditorSettings.class);

    private static final String[] FALLBACK_FONTS = {
        "Consolas", "Courier New", "Monospace"
    };

    private static final String[] SYNTAX_THEMES = {
        "Default", "Monokai", "Dracula", "Solarized", "GitHub Light", "One Dark",
        "Nord", "GitHub Dark", "Atom One Light", "Tokyo Night", "Catppuccin",
        "Ayu Dark", "SynthWave '84", "Noctis Lux",
        "Gruvbox Dark", "Gruvbox Light", "Material Darker", "Material Lighter",
        "Material Ocean", "Rose Pine", "Rose Pine Moon", "Everforest Dark",
        "Everforest Light", "Night Owl", "Light Owl", "Palenight",
        "Horizon", "Panda", "Shades of Purple", "Monokai Pro",
        "Ayu Light", "Ayu Mirage", "VSCode Dark+", "VSCode Light+"
    };

    // ── Font ─────────────────────────────────────────────────────────────────
    public static String getFontFamily()          { return prefs.get("fontFamily", "Consolas"); }
    public static void   setFontFamily(String v)  { prefs.put("fontFamily", v != null ? v : "Consolas"); }

    public static double getFontSize()            { return prefs.getDouble("fontSize", 14.0); }
    public static void   setFontSize(double v)    { prefs.putDouble("fontSize", Math.max(8, Math.min(72, v))); }

    // ── Editor ───────────────────────────────────────────────────────────────
    public static boolean isWordWrap()            { return prefs.getBoolean("wordWrap", false); }
    public static void    setWordWrap(boolean v)  { prefs.putBoolean("wordWrap", v); }

    public static int  getTabSize()               { return prefs.getInt("tabSize", 4); }
    public static void setTabSize(int v)          { prefs.putInt("tabSize", Math.max(1, Math.min(8, v))); }
    /** overload — Spinner<Double> في SettingsDialog */
    public static void setTabSize(double v)       { setTabSize((int) v); }

    public static double getLineHeight()          { return prefs.getDouble("lineHeight", 1.5); }
    public static void   setLineHeight(double v)  { prefs.putDouble("lineHeight", Math.max(1.0, Math.min(3.0, v))); }

    public static boolean isAutoCloseBrackets()   { return prefs.getBoolean("autoCloseBrackets", true); }
    public static void    setAutoCloseBrackets(boolean v) { prefs.putBoolean("autoCloseBrackets", v); }

    public static boolean isAutoSave()            { return prefs.getBoolean("autoSave", true); }
    public static void    setAutoSave(boolean v)  { prefs.putBoolean("autoSave", v); }

    // ── Display ──────────────────────────────────────────────────────────────
    public static boolean isShowLineNumbers()     { return prefs.getBoolean("showLineNumbers", true); }
    public static void    setShowLineNumbers(boolean v) { prefs.putBoolean("showLineNumbers", v); }

    public static boolean isHighlightCurrentLine()     { return prefs.getBoolean("highlightCurrentLine", true); }
    public static void    setHighlightCurrentLine(boolean v) { prefs.putBoolean("highlightCurrentLine", v); }

    public static boolean isHighlightMatchingBrackets()     { return prefs.getBoolean("highlightMatchingBrackets", true); }
    public static void    setHighlightMatchingBrackets(boolean v) { prefs.putBoolean("highlightMatchingBrackets", v); }

    public static boolean isShowWhitespace()      { return prefs.getBoolean("showWhitespace", false); }
    public static void    setShowWhitespace(boolean v) { prefs.putBoolean("showWhitespace", v); }

    public static boolean isShowIndentGuide()     { return prefs.getBoolean("showIndentGuide", true); }
    public static void    setShowIndentGuide(boolean v) { prefs.putBoolean("showIndentGuide", v); }

    // ── Behavior ─────────────────────────────────────────────────────────────
    public static boolean isFormatOnSave()        { return prefs.getBoolean("formatOnSave", false); }
    public static void    setFormatOnSave(boolean v) { prefs.putBoolean("formatOnSave", v); }

    public static boolean isTrimWhitespace()      { return prefs.getBoolean("trimWhitespace", true); }
    public static void    setTrimWhitespace(boolean v) { prefs.putBoolean("trimWhitespace", v); }

    public static boolean isAutoIndent()          { return prefs.getBoolean("autoIndent", true); }
    public static void    setAutoIndent(boolean v){ prefs.putBoolean("autoIndent", v); }

    // ── Syntax Theme ─────────────────────────────────────────────────────────
    public static String getSyntaxTheme()         { return prefs.get("syntaxTheme", "Default"); }
    public static void   setSyntaxTheme(String v) { prefs.put("syntaxTheme", v != null ? v : "Default"); }

    // ── Session ──────────────────────────────────────────────────────────────
    public static boolean isRestoreSession()      { return prefs.getBoolean("restoreSession", false); }
    public static void    setRestoreSession(boolean v) { prefs.putBoolean("restoreSession", v); }

    // ── Cursor ───────────────────────────────────────────────────────────────
    public static String getCursorStyle()         { return prefs.get("cursorStyle", "block"); }
    public static void   setCursorStyle(String v) { prefs.put("cursorStyle", v != null ? v : "block"); }

    public static double getCursorWidth()         { return prefs.getDouble("cursorWidth", 2.0); }
    public static void   setCursorWidth(double v) { prefs.putDouble("cursorWidth", Math.max(1, Math.min(8, v))); }

    // ── Monaco-specific (مش موجودة في الأصل — مضافة للـ SettingsDialog) ────
    public static boolean isMinimapEnabled()      { return prefs.getBoolean("minimap", true); }
    public static void    setMinimapEnabled(boolean v) { prefs.putBoolean("minimap", v); }

    public static boolean isBracketColors()       { return prefs.getBoolean("bracketColors", true); }
    public static void    setBracketColors(boolean v)  { prefs.putBoolean("bracketColors", v); }

    public static boolean isSmoothScroll()        { return prefs.getBoolean("smoothScroll", true); }
    public static void    setSmoothScroll(boolean v)   { prefs.putBoolean("smoothScroll", v); }

    public static boolean isParameterHints()      { return prefs.getBoolean("parameterHints", true); }
    public static void    setParameterHints(boolean v) { prefs.putBoolean("parameterHints", v); }

    public static boolean isInlineSuggest()       { return prefs.getBoolean("inlineSuggest", true); }
    public static void    setInlineSuggest(boolean v)  { prefs.putBoolean("inlineSuggest", v); }

    public static boolean isCodeLens()            { return prefs.getBoolean("codeLens", true); }
    public static void    setCodeLens(boolean v)  { prefs.putBoolean("codeLens", v); }

    public static boolean isQuickSuggest()        { return prefs.getBoolean("quickSuggest", true); }
    public static void    setQuickSuggest(boolean v)   { prefs.putBoolean("quickSuggest", v); }

    public static boolean isSuggestTrigger()      { return prefs.getBoolean("suggestTrigger", true); }
    public static void    setSuggestTrigger(boolean v) { prefs.putBoolean("suggestTrigger", v); }

    public static boolean isWordBasedSuggest()    { return prefs.getBoolean("wordBasedSuggest", true); }
    public static void    setWordBasedSuggest(boolean v) { prefs.putBoolean("wordBasedSuggest", v); }

    public static String getCursorBlink()         { return prefs.get("cursorBlink", "smooth"); }
    public static void   setCursorBlink(String v) { prefs.put("cursorBlink", v != null ? v : "smooth"); }

    public static String getRenderLineHighlight()      { return prefs.get("renderLineHighlight", "all"); }
    public static void   setRenderLineHighlight(String v)  { prefs.put("renderLineHighlight", v != null ? v : "all"); }

    public static String getMatchBracketsMode()   { return prefs.get("matchBracketsMode", "always"); }
    public static void   setMatchBracketsMode(String v)    { prefs.put("matchBracketsMode", v != null ? v : "always"); }

    public static String getTabCompletion()       { return prefs.get("tabCompletion", "on"); }
    public static void   setTabCompletion(String v)        { prefs.put("tabCompletion", v != null ? v : "on"); }

    public static String getMonacoTheme()         { return prefs.get("monacoTheme", "darcula"); }
    public static void   setMonacoTheme(String v) { prefs.put("monacoTheme", v != null ? v : "darcula"); }

    // ── Debugger ─────────────────────────────────────────────────────────────
    public static boolean isAutoBreak()           { return prefs.getBoolean("autoBreak", true); }
    public static void    setAutoBreak(boolean v) { prefs.putBoolean("autoBreak", v); }

    public static int  getStepDelay()             { return prefs.getInt("stepDelay", 500); }
    public static void setStepDelay(int v)        { prefs.putInt("stepDelay", v); }

    public static String getBpIcon()              { return prefs.get("bpIcon", "dot"); }
    public static void   setBpIcon(String v)      { prefs.put("bpIcon", v != null ? v : "dot"); }

    // ── Lists ────────────────────────────────────────────────────────────────
    /** يرجع كل Fonts النظام عشان الكومبو بوكس — يضمن إن الخط موجود فعلًا */
    public static List<String> getAvailableFonts() {
        try {
            List<String> families = new ArrayList<>(javafx.scene.text.Font.getFamilies());
            Collections.sort(families);
            return families;
        } catch (Exception e) {
            return Arrays.asList(FALLBACK_FONTS);
        }
    }
    public static List<String> getSyntaxThemes()    { return Arrays.asList(SYNTAX_THEMES); }

    /** array version لو محتاجها في مكان تاني */
    public static String[] getAvailableFontsArray() {
        try {
            List<String> families = javafx.scene.text.Font.getFamilies();
            return families.toArray(new String[0]);
        } catch (Exception e) {
            return FALLBACK_FONTS;
        }
    }
    public static String[] getSyntaxThemesArray()   { return SYNTAX_THEMES; }

    /** يكتب الإعدادات على القرص فوراً (Windows Registry / ملف prefs) */
    public static void flush() {
        try {
            prefs.flush();
        } catch (Exception ignored) {}
    }
}