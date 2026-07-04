package com.eagle.controller;

import com.eagle.util.EditorSettings;
import com.eagle.util.ThemeManager;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.fxmisc.richtext.CodeArea;

public class EditorSettingsController {

    // Sidebar
    @FXML private ListView<String> categoryList;

    // Content
    @FXML private StackPane contentArea;
    @FXML private VBox editorPane;
    @FXML private VBox appearancePane;
    @FXML private VBox behaviorPane;
    @FXML private VBox keyboardPane;

    // Preview
    @FXML private StackPane previewContainer;

    // Editor controls
    @FXML private ComboBox<String> fontFamilyCombo;
    @FXML private Slider fontSizeSlider;
    @FXML private Label fontSizeLabel;
    @FXML private Slider lineHeightSlider;
    @FXML private Label lineHeightLabel;
    @FXML private Spinner<Integer> tabSizeSpinner;
    @FXML private ComboBox<String> cursorStyleCombo;
    @FXML private CheckBox wordWrapCheck;
    @FXML private CheckBox autoCloseBracketsCheck;
    @FXML private CheckBox showLineNumbersCheck;
    @FXML private CheckBox highlightCurrentLineCheck;
    @FXML private CheckBox highlightMatchingBracketsCheck;
    @FXML private CheckBox showWhitespaceCheck;
    @FXML private CheckBox showIndentGuideCheck;

    // Appearance controls
    @FXML private ComboBox<String> appThemeCombo;
    @FXML private ComboBox<String> syntaxThemeCombo;
    @FXML private ComboBox<String> monacoThemeCombo;

    // Behavior controls
    @FXML private CheckBox autoSaveCheck;
    @FXML private CheckBox autoIndentCheck;
    @FXML private CheckBox formatOnSaveCheck;
    @FXML private CheckBox trimWhitespaceCheck;
    @FXML private CheckBox restoreSessionCheck;

    // Buttons
    @FXML private Button okBtn;
    @FXML private Button cancelBtn;

    private CodeArea previewCode;
    private Stage stage;
    private boolean saved = false;

    private static final String PREVIEW_CODE =
        "function fibonacci(n) {\n" +
        "  if (n <= 1) return n;\n" +
        "  return fibonacci(n - 1) + fibonacci(n - 2);\n" +
        "}\n" +
        "\n" +
        "const result = fibonacci(10);\n" +
        "console.log(\"Result:\", result);\n" +
        "\n" +
        "// Fibonacci sequence\n" +
        "for (let i = 0; i < 10; i++) {\n" +
        "  console.log(fibonacci(i));\n" +
        "}";

    // ================================================================
    //  INIT
    // ================================================================

    @FXML
    void initialize() {
        setupCategoryList();
        setupPreview();
        loadSettings();
        setupLiveListeners();
        switchCategory("Editor");
    }

    void setStage(Stage stage) { this.stage = stage; }

    // ================================================================
    //  SIDEBAR
    // ================================================================

    private void setupCategoryList() {
        categoryList.setItems(FXCollections.observableArrayList(
            "Editor", "Appearance", "Behavior", "Keyboards"
        ));
        categoryList.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
            if (nv != null) switchCategory(nv);
        });
        categoryList.getSelectionModel().select(0);
    }

    private void switchCategory(String category) {
        editorPane.setVisible(false); editorPane.setManaged(false);
        appearancePane.setVisible(false); appearancePane.setManaged(false);
        behaviorPane.setVisible(false); behaviorPane.setManaged(false);
        keyboardPane.setVisible(false); keyboardPane.setManaged(false);

        switch (category) {
            case "Editor": editorPane.setVisible(true); editorPane.setManaged(true); break;
            case "Appearance": appearancePane.setVisible(true); appearancePane.setManaged(true); break;
            case "Behavior": behaviorPane.setVisible(true); behaviorPane.setManaged(true); break;
            case "Keyboards": keyboardPane.setVisible(true); keyboardPane.setManaged(true); break;
        }
    }

    // ================================================================
    //  PREVIEW
    // ================================================================

    private void setupPreview() {
        previewCode = new CodeArea(PREVIEW_CODE);
        previewCode.setEditable(false);
        previewCode.setPrefHeight(140);
        previewCode.setMaxHeight(140);
        previewContainer.getChildren().add(previewCode);
    }

    private void updatePreview() {
        String font = fontFamilyCombo.getValue();
        if (font == null || font.isEmpty()) font = "Consolas";
        double size = fontSizeSlider.getValue();
        String previewStyle = "-fx-font-family: '" + font + "'; -fx-font-size: " + size + "px;";
        previewCode.setStyle(previewStyle);

        // Reapply syntax theme stylesheet
        previewCode.getStylesheets().clear();
        previewCode.getStylesheets().addAll(
            previewCode.getClass().getResource("/com/eagle/css/base.css").toExternalForm(),
            getSyntaxThemePath(syntaxThemeCombo.getValue())
        );
    }

    // ================================================================
    //  LOAD / SAVE
    // ================================================================

    private void loadSettings() {
        // Font
        fontFamilyCombo.setItems(FXCollections.observableArrayList(EditorSettings.getAvailableFonts()));
        fontFamilyCombo.setValue(EditorSettings.getFontFamily());
        fontFamilyCombo.setCellFactory(lv -> new javafx.scene.control.ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(null);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item + " \u2014 Eagle By Kadysoft");
                setStyle("-fx-font-family: '" + item + "'; -fx-font-size: 12px;");
            }
        });
        fontFamilyCombo.setButtonCell(new javafx.scene.control.ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("-fx-font-family: '" + item + "'; -fx-font-size: 14px;");
            }
        });
        fontSizeSlider.setValue(EditorSettings.getFontSize());
        fontSizeLabel.setText(String.valueOf((int) EditorSettings.getFontSize()));
        lineHeightSlider.setValue(EditorSettings.getLineHeight());
        lineHeightLabel.setText(String.format("%.1f", EditorSettings.getLineHeight()));
        tabSizeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 8, EditorSettings.getTabSize()));

        cursorStyleCombo.setItems(FXCollections.observableArrayList("block", "line", "underline"));
        cursorStyleCombo.setValue(EditorSettings.getCursorStyle());

        // Editor options
        wordWrapCheck.setSelected(EditorSettings.isWordWrap());
        autoCloseBracketsCheck.setSelected(EditorSettings.isAutoCloseBrackets());
        showLineNumbersCheck.setSelected(EditorSettings.isShowLineNumbers());
        highlightCurrentLineCheck.setSelected(EditorSettings.isHighlightCurrentLine());
        highlightMatchingBracketsCheck.setSelected(EditorSettings.isHighlightMatchingBrackets());
        showWhitespaceCheck.setSelected(EditorSettings.isShowWhitespace());
        showIndentGuideCheck.setSelected(EditorSettings.isShowIndentGuide());

        // Appearance
        appThemeCombo.setItems(FXCollections.observableArrayList(ThemeManager.getInstance().getAvailableThemeNames()));
        appThemeCombo.setValue(ThemeManager.getInstance().getCurrentThemeName());

        syntaxThemeCombo.setItems(FXCollections.observableArrayList(EditorSettings.getSyntaxThemes()));
        syntaxThemeCombo.setValue(EditorSettings.getSyntaxTheme());

        monacoThemeCombo.setItems(FXCollections.observableArrayList(com.eagle.editor.MonacoEditor.AVAILABLE_THEMES));
        monacoThemeCombo.setValue(EditorSettings.getMonacoTheme());

        // Behavior
        autoSaveCheck.setSelected(EditorSettings.isAutoSave());
        autoIndentCheck.setSelected(EditorSettings.isAutoIndent());
        formatOnSaveCheck.setSelected(EditorSettings.isFormatOnSave());
        trimWhitespaceCheck.setSelected(EditorSettings.isTrimWhitespace());
        restoreSessionCheck.setSelected(EditorSettings.isRestoreSession());
    }

    private void saveSettings() {
        // ── General ──────────────────────────────────────────────────
        EditorSettings.setFontFamily(fontFamilyCombo.getValue());
        EditorSettings.setFontSize(fontSizeSlider.getValue());
        EditorSettings.setLineHeight(lineHeightSlider.getValue());
        EditorSettings.setTabSize(tabSizeSpinner.getValue());
        EditorSettings.setCursorStyle(cursorStyleCombo.getValue());

        // ── Editor ───────────────────────────────────────────────────
        EditorSettings.setWordWrap(wordWrapCheck.isSelected());
        EditorSettings.setAutoCloseBrackets(autoCloseBracketsCheck.isSelected());
        EditorSettings.setShowLineNumbers(showLineNumbersCheck.isSelected());
        EditorSettings.setHighlightCurrentLine(highlightCurrentLineCheck.isSelected());
        EditorSettings.setHighlightMatchingBrackets(highlightMatchingBracketsCheck.isSelected());
        EditorSettings.setShowWhitespace(showWhitespaceCheck.isSelected());
        EditorSettings.setShowIndentGuide(showIndentGuideCheck.isSelected());

        // ── Behavior ─────────────────────────────────────────────────
        EditorSettings.setAutoSave(autoSaveCheck.isSelected());
        EditorSettings.setAutoIndent(autoIndentCheck.isSelected());
        EditorSettings.setFormatOnSave(formatOnSaveCheck.isSelected());
        EditorSettings.setTrimWhitespace(trimWhitespaceCheck.isSelected());
        EditorSettings.setRestoreSession(restoreSessionCheck.isSelected());

        // ── Appearance ───────────────────────────────────────────────
        EditorSettings.setSyntaxTheme(syntaxThemeCombo.getValue());
        EditorSettings.setMonacoTheme(monacoThemeCombo.getValue());

        // App theme — يحفظ داخل ThemeManager اللي بيكتب في Preferences بتاعته
        String themeName = appThemeCombo.getValue();
        if (themeName != null && !themeName.equals(ThemeManager.getInstance().getCurrentThemeName())) {
            ThemeManager.getInstance().setThemeByName(themeName);
        }

        EditorSettings.flush();
    }

    // ================================================================
    //  LIVE LISTENERS
    // ================================================================

    private void setupLiveListeners() {
        ChangeListener<Object> previewUpdater = (o, ov, nv) -> updatePreview();

        fontFamilyCombo.valueProperty().addListener((o, ov, nv) -> {
            if (nv != null) {
                EditorSettings.setFontFamily((String) nv);
            }
            updatePreview();
            applySettingsToOpenEditors();
        });
        fontSizeSlider.valueProperty().addListener((o, ov, nv) -> {
            fontSizeLabel.setText(String.valueOf(nv.intValue()));
            EditorSettings.setFontSize(((Number) nv).doubleValue());
            updatePreview();
            applySettingsToOpenEditors();
        });
        lineHeightSlider.valueProperty().addListener((o, ov, nv) -> {
            lineHeightLabel.setText(String.format("%.1f", nv));
            EditorSettings.setLineHeight(((Number) nv).doubleValue());
            applySettingsToOpenEditors();
        });
        syntaxThemeCombo.valueProperty().addListener(previewUpdater);
        monacoThemeCombo.valueProperty().addListener((o, ov, nv) -> {
            applySettingsToOpenEditors();
        });
    }

    // ================================================================
    //  ACTIONS
    // ================================================================

    @FXML
    private void onOk() {
        applyAndClose();
    }

    @FXML
    private void onCancel() {
        if (stage != null) stage.close();
    }

    private void applyAndClose() {
        saveSettings();              // يحفظ كل شيء في Preferences + يطبق الثيم
        applySettingsToOpenEditors(); // يطبق على كل CodeArea مفتوح
        saved = true;
        if (stage != null) stage.close();
    }

    // ================================================================
    //  APPLY TO EDITORS
    // ================================================================

    private void applySettingsToOpenEditors() {
        EditorController.applySettingsToOpenEditors();
    }

    // ================================================================
    //  HELPERS
    // ================================================================

    private String getSyntaxThemePath(String theme) {
        if (theme == null || "Default".equals(theme)) return "";
        String path;
        switch (theme) {
            case "Monokai":          path = "/com/eagle/css/syntax-monokai.css";          break;
            case "Solarized":
            case "Solarized Dark":   path = "/com/eagle/css/syntax-solarized-dark.css";  break;
            case "GitHub Light":     path = "/com/eagle/css/syntax-github-light.css";     break;
            case "Dracula":          path = "/com/eagle/css/syntax-dracula.css";          break;
            case "One Dark":         path = "/com/eagle/css/syntax-one-dark.css";         break;
            case "Nord":             path = "/com/eagle/css/syntax-nord.css";             break;
            case "GitHub Dark":      path = "/com/eagle/css/syntax-github-dark.css";      break;
            case "Atom One Light":   path = "/com/eagle/css/syntax-atom-one-light.css";   break;
            case "Tokyo Night":      path = "/com/eagle/css/syntax-tokyo-night.css";      break;
            case "Catppuccin":       path = "/com/eagle/css/syntax-catppuccin.css";       break;
            case "Ayu Dark":         path = "/com/eagle/css/syntax-ayu-dark.css";         break;
            case "SynthWave '84":    path = "/com/eagle/css/syntax-synthwave.css";        break;
            case "Noctis Lux":       path = "/com/eagle/css/syntax-noctis-lux.css";       break;
            case "Gruvbox Dark":     path = "/com/eagle/css/syntax-gruvbox-dark.css";     break;
            case "Gruvbox Light":    path = "/com/eagle/css/syntax-gruvbox-light.css";    break;
            case "Material Darker":  path = "/com/eagle/css/syntax-material-darker.css";  break;
            case "Material Lighter": path = "/com/eagle/css/syntax-material-lighter.css"; break;
            case "Material Ocean":   path = "/com/eagle/css/syntax-material-ocean.css";   break;
            case "Rose Pine":        path = "/com/eagle/css/syntax-rose-pine.css";        break;
            case "Rose Pine Moon":   path = "/com/eagle/css/syntax-rose-pine-moon.css";   break;
            case "Everforest Dark":  path = "/com/eagle/css/syntax-everforest-dark.css";  break;
            case "Everforest Light": path = "/com/eagle/css/syntax-everforest-light.css"; break;
            case "Night Owl":        path = "/com/eagle/css/syntax-night-owl.css";        break;
            case "Light Owl":        path = "/com/eagle/css/syntax-light-owl.css";        break;
            case "Palenight":        path = "/com/eagle/css/syntax-palenight.css";        break;
            case "Horizon":          path = "/com/eagle/css/syntax-horizon.css";          break;
            case "Panda":            path = "/com/eagle/css/syntax-panda.css";            break;
            case "Shades of Purple": path = "/com/eagle/css/syntax-shades-purple.css";    break;
            case "Monokai Pro":      path = "/com/eagle/css/syntax-monokai-pro.css";      break;
            case "Ayu Light":        path = "/com/eagle/css/syntax-ayu-light.css";        break;
            case "Ayu Mirage":       path = "/com/eagle/css/syntax-ayu-mirage.css";       break;
            case "VSCode Dark+":     path = "/com/eagle/css/syntax-vscode-dark.css";      break;
            case "VSCode Light+":    path = "/com/eagle/css/syntax-vscode-light.css";     break;
            default: return "";
        }
        java.net.URL url = getClass().getResource(path);
        return url != null ? url.toExternalForm() : "";
    }

    public boolean isSaved() { return saved; }
}