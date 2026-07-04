package com.eagle.editor;

import com.eagle.controller.EditorController;
import com.eagle.icons.IconManager;
import com.eagle.util.EditorSettings;
import com.eagle.util.ThemeManager;
import com.eagle.util.ToolsConfig;
import java.io.*;
import java.util.*;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class SettingsDialog {

    private static final String AI_CONFIG_DIR = System.getProperty("user.home") + "/.webide";
    private static final String AI_CONFIG_FILE = AI_CONFIG_DIR + "/ai.properties";
    private static final String REGISTRY_FILE = AI_CONFIG_DIR + "/registry.properties";

    private final Stage stage = new Stage();
    private final TabPane tabPane = new TabPane();

    private MonacoEditor monacoEditor;
    private Runnable onApplied;

    // -- General --
    private ComboBox<String> themeCombo;
    private ComboBox<String> fontCombo;
    private Spinner<Double> fontSizeSpinner;
    private ComboBox<String> cursorStyleCombo;
    private Spinner<Double> cursorWidthSpinner;
    private CheckBox restoreSessionCheck;

    // -- Editor --
    private CheckBox wordWrapCheck, lineNumbersCheck, highlightLineCheck, matchBracketsCheck;
    private CheckBox showWhitespaceCheck, indentGuideCheck, autoCloseBracketsCheck;
    private CheckBox autoSaveCheck, formatOnSaveCheck, autoIndentCheck, trimWhitespaceCheck;
    private Spinner<Integer> tabSizeSpinner;
    private Spinner<Double> lineHeightSpinner;

    // -- AI --
    private ComboBox<String> aiProviderCombo;
    private TextField geminiKeyField, geminiModelField;
    private TextField openaiEndpointField, openaiKeyField;
    private ComboBox<String> openaiModelCombo, openaiPresetCombo;
    private TextField ollamaEndpointField, ollamaModelField;
    private TextField localGgufPathField;
    private Label aiStatusLabel;
    private Button aiTestBtn;
    
    private TextField llamaCliField;     // ← أضف ده

    // -- Monaco --
    private CheckBox minimapCheck, bracketColorsCheck, parameterHintsCheck;
    private CheckBox smoothScrollCheck, inlineSuggestCheck, codeLensCheck;
    private CheckBox quickSuggestCheck, suggestTriggerCheck, wordBasedSuggestCheck;
    private ComboBox<String> cursorBlinkCombo, renderLineHighlightCombo;
    private ComboBox<String> matchBracketsCombo, tabCompletionCombo;
    private ComboBox<String> monacoThemeCombo;

    // -- Editor --
    private ComboBox<String> syntaxThemeCombo;

    // -- Registry --
    private final ObservableList<RegistryEntry> registryItems = FXCollections.observableArrayList();
    private TableView<RegistryEntry> registryTable;
    private TextField registryKeyField, registryValueField, registryFilterField;

    // -- Debugger --
    private CheckBox autoBreakCheck;
    private Spinner<Integer> stepDelaySpinner;
    private RadioButton bpDotRadio, bpCrossRadio, bpCircleRadio;

    // -- Environment --
    private final Map<String, TextField> toolFields = new LinkedHashMap<>();

    public SettingsDialog() {
        this(null, EditorController::applySettingsToOpenEditors);
    }

    public SettingsDialog(MonacoEditor editor) {
        this(editor, EditorController::applySettingsToOpenEditors);
    }

    public SettingsDialog(MonacoEditor editor, Runnable onApplied) {
        this.monacoEditor = editor;
        this.onApplied = onApplied;
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Settings");
        stage.setMinWidth(720);
        stage.setMinHeight(550);

        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().addAll(
            buildGeneralTab(),
            buildEditorTab(),
            buildAiTab(),
            buildEnvironmentTab(),
            buildMonacoTab(),
            buildRegistryTab(),
            buildDebuggerTab()
        );

        VBox root = new VBox(10);
        root.setPadding(new Insets(12));
        root.getChildren().addAll(tabPane, buildButtonBar());
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        Scene scene = new Scene(root);
        ThemeManager.getInstance().applyTheme(scene);
        stage.setScene(scene);
        loadAllSettings();
    }

    public void show() {
        stage.showAndWait();
    }

    private javafx.stage.Window getWindow() { return stage; }

    // ================================================================
    //  Tabs
    // ================================================================

    private Tab buildGeneralTab() {
        Tab tab = new Tab("General");
        tab.setGraphic(IconManager.imageView(IconManager.SETTINGS, 14));

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(8);
        grid.setPadding(new Insets(12));

        themeCombo = new ComboBox<>();
        themeCombo.getItems().addAll("Dark (Default)", "Light (Default)", "Midnight Violet", "Sandstone Light");
        grid.addRow(0, new Label("Theme:"), themeCombo);

        fontCombo = new ComboBox<>();
        fontCombo.getItems().addAll(EditorSettings.getAvailableFonts());
        fontCombo.setEditable(true);
        fontCombo.setCellFactory(lv -> new javafx.scene.control.ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(null);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item + " \u2014 Eagle By Kadysoft");
                setStyle("-fx-font-family: '" + item + "'; -fx-font-size: 12px;");
            }
        });
        fontCombo.setButtonCell(new javafx.scene.control.ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("-fx-font-family: '" + item + "'; -fx-font-size: 14px;");
            }
        });
        grid.addRow(1, new Label("Font Family:"), fontCombo);

        fontSizeSpinner = new Spinner<>(8.0, 72.0, 14.0, 1.0);
        fontSizeSpinner.setEditable(true);
        grid.addRow(2, new Label("Font Size:"), fontSizeSpinner);

        cursorStyleCombo = new ComboBox<>();
        cursorStyleCombo.getItems().addAll("block", "line", "underline", "block-outline", "underline-thin");
        cursorStyleCombo.setEditable(true);
        grid.addRow(3, new Label("Cursor Style:"), cursorStyleCombo);

        cursorWidthSpinner = new Spinner<>(1.0, 8.0, 2.0, 0.5);
        cursorWidthSpinner.setEditable(true);
        grid.addRow(4, new Label("Cursor Width:"), cursorWidthSpinner);

        restoreSessionCheck = new CheckBox("Restore previous session on startup");
        grid.add(restoreSessionCheck, 0, 5, 2, 1);

        setAllGridColSpan(grid, 1);
        Label note = new Label("Changes take effect after clicking Save.");
        note.setStyle("-fx-text-fill: -text-muted; -fx-font-size: 11px;");
        grid.add(note, 0, 6, 2, 1);

        tab.setContent(new VBox(grid));
        return tab;
    }

    private Tab buildEditorTab() {
        Tab tab = new Tab("Editor");
        tab.setGraphic(IconManager.imageView(IconManager.FILE_CODE, 14));

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(8);
        grid.setPadding(new Insets(12));

        wordWrapCheck = new CheckBox("Word Wrap");
        grid.addRow(0, wordWrapCheck);

        lineNumbersCheck = new CheckBox("Show Line Numbers");
        grid.addRow(1, lineNumbersCheck);

        highlightLineCheck = new CheckBox("Highlight Current Line");
        grid.addRow(2, highlightLineCheck);

        matchBracketsCheck = new CheckBox("Highlight Matching Brackets");
        grid.addRow(3, matchBracketsCheck);

        showWhitespaceCheck = new CheckBox("Show Whitespace Characters");
        grid.addRow(4, showWhitespaceCheck);

        indentGuideCheck = new CheckBox("Show Indent Guide");
        grid.addRow(5, indentGuideCheck);

        autoCloseBracketsCheck = new CheckBox("Auto-Close Brackets & Quotes");
        grid.addRow(6, autoCloseBracketsCheck);

        autoSaveCheck = new CheckBox("Auto-Save on Focus Loss");
        grid.addRow(7, autoSaveCheck);

        formatOnSaveCheck = new CheckBox("Format Code on Save");
        grid.addRow(8, formatOnSaveCheck);

        autoIndentCheck = new CheckBox("Auto-Indent on Paste");
        grid.addRow(9, autoIndentCheck);

        trimWhitespaceCheck = new CheckBox("Trim Trailing Whitespace on Save");
        grid.addRow(10, trimWhitespaceCheck);

        tabSizeSpinner = new Spinner<>(1, 8, 4);
        tabSizeSpinner.setEditable(true);
        HBox tabBox = new HBox(6, new Label("Tab Size:"), tabSizeSpinner);
        grid.addRow(11, tabBox);

        lineHeightSpinner = new Spinner<>(1.0, 3.0, 1.5, 0.1);
        lineHeightSpinner.setEditable(true);
        HBox lhBox = new HBox(6, new Label("Line Height:"), lineHeightSpinner);
        grid.addRow(12, lhBox);

        syntaxThemeCombo = new ComboBox<>();
        syntaxThemeCombo.getItems().addAll(EditorSettings.getSyntaxThemes());
        grid.addRow(13, new Label("Syntax Highlight Theme:"), syntaxThemeCombo);

        tab.setContent(new ScrollPane(new VBox(grid)) {{
            setFitToWidth(true); setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        }});
        return tab;
    }

    private Tab buildAiTab() {
        Tab tab = new Tab("AI");
        tab.setGraphic(IconManager.imageView(IconManager.ROBOT, 14));

        VBox root = new VBox(8);
        root.setPadding(new Insets(12));

        aiProviderCombo = new ComboBox<>();
        aiProviderCombo.getItems().addAll("Google Gemini (Online)", "OpenAI Compatible (Online)", "Ollama (Local)", "Local GGUF (llama.cpp)");

        VBox geminiBox = new VBox(4);
        geminiKeyField = new TextField(); geminiKeyField.setPromptText("Gemini API Key");
        Hyperlink geminiLink = new Hyperlink("Get Free Key");
        geminiLink.setOnAction(e -> openUrl("https://aistudio.google.com/app/apikey"));
        HBox geminiKeyRow = new HBox(6, geminiKeyField, geminiLink);
        HBox.setHgrow(geminiKeyField, Priority.ALWAYS);
        geminiModelField = new TextField(); geminiModelField.setPromptText("Model (e.g. gemini-2.0-flash)");
        Label geminiHelp = new Label("Setup: Go to aistudio.google.com -> Create API Key -> paste above -> model: gemini-2.0-flash -> Test Connection");
        geminiHelp.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-muted; -fx-wrap-text: true;");
        geminiBox.getChildren().addAll(geminiKeyRow, geminiModelField, geminiHelp);

        VBox openaiBox = new VBox(4);
        openaiPresetCombo = new ComboBox<>();
        openaiPresetCombo.getItems().addAll("Groq (Free)", "GitHub Models (Free)", "OpenRouter (Free)", "DeepSeek (Free)", "Custom");
        openaiEndpointField = new TextField(); openaiEndpointField.setPromptText("Endpoint URL");
        openaiKeyField = new TextField(); openaiKeyField.setPromptText("API Key");
        HBox oaiKeyRow = new HBox(6, openaiKeyField, new Hyperlink("Get Key") {{
            setOnAction(e -> {
                String p = openaiPresetCombo.getValue();
                if ("Groq (Free)".equals(p)) openUrl("https://console.groq.com/keys");
                else if ("GitHub Models (Free)".equals(p)) openUrl("https://github.com/settings/tokens");
                else if ("OpenRouter (Free)".equals(p)) openUrl("https://openrouter.ai/keys");
                else if ("DeepSeek (Free)".equals(p)) openUrl("https://platform.deepseek.com/api_keys");
            });
        }});
        HBox.setHgrow(openaiKeyField, Priority.ALWAYS);
        openaiModelCombo = new ComboBox<>(); openaiModelCombo.setEditable(true);
            openaiPresetCombo.setOnAction(e -> {
            String p = openaiPresetCombo.getValue();
            if ("Groq (Free)".equals(p)) {
                openaiEndpointField.setText("https://api.groq.com/openai/v1/chat/completions");
                openaiModelCombo.getItems().setAll("llama-3.3-70b-versatile", "llama-3.3-70b-instruct", "deepseek-r1-distill-llama-70b", "gemma-3-27b-it", "mixtral-8x7b-32768");
                openaiModelCombo.setValue("llama-3.3-70b-versatile");
            } else if ("GitHub Models (Free)".equals(p)) {
                openaiEndpointField.setText("https://models.inference.ai.azure.com/chat/completions");
                openaiModelCombo.getItems().setAll("gpt-4o", "gpt-4o-mini", "Phi-3.5-mini-instruct", "Mistral-small", "AI21-Jamba-1.5-Mini");
                openaiModelCombo.setValue("gpt-4o-mini");
            } else if ("OpenRouter (Free)".equals(p)) {
                openaiEndpointField.setText("https://openrouter.ai/api/v1/chat/completions");
                openaiModelCombo.getItems().setAll("deepseek/deepseek-r1", "deepseek/deepseek-v3", "qwen/qwen-3-coder", "google/gemma-3-27b-it");
                openaiModelCombo.setValue("deepseek/deepseek-r1");
            } else if ("DeepSeek (Free)".equals(p)) {
                openaiEndpointField.setText("https://api.deepseek.com/v1/chat/completions");
                openaiModelCombo.getItems().setAll("deepseek-chat", "deepseek-reasoner");
                openaiModelCombo.setValue("deepseek-chat");
            }
        });
        Label openaiHelp = new Label(
            "<Groq> Sign up at console.groq.com -> API Keys -> paste key (gsk_...). Model: llama-3.3-70b-versatile\n" +
            "<GitHub> github.com/settings/tokens -> classic token with read:user -> paste. Model: gpt-4o-mini\n" +
            "<OpenRouter> openrouter.ai/keys -> create key -> paste. Model: deepseek/deepseek-r1\n" +
            "<DeepSeek> platform.deepseek.com/api_keys -> create -> paste. Model: deepseek-chat");
        openaiHelp.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-muted; -fx-wrap-text: true;");
        openaiBox.getChildren().addAll(openaiPresetCombo, openaiEndpointField, oaiKeyRow, openaiModelCombo, openaiHelp);

        VBox ollamaBox = new VBox(4);
        ollamaEndpointField = new TextField("http://localhost:11434/api/chat");
        ollamaModelField = new TextField("llama3.2:1b");
        Label ollamaHelp = new Label(
            "Install Ollama from ollama.com -> run: ollama pull llama3.2:1b (or qwen2.5:0.5b for less RAM)\n" +
            "Recommended light models for 8GB RAM: llama3.2:1b (1.3GB, fast), qwen2.5-coder:1.5b (good at code),\nphi3:mini (2.2GB), gemma2:2b (1.6GB), qwen2.5:0.5b (500MB, fastest).\n" +
            "To change model later, just pull it with Ollama and type the name above.\n" +
            "For longer responses, the system now uses num_predict=8192 internally.");
        ollamaHelp.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-muted; -fx-wrap-text: true;");
        ollamaBox.getChildren().addAll(ollamaEndpointField, ollamaModelField, ollamaHelp);

        
        
        
        
        VBox localGgufBox = new VBox(8);
        localGgufBox.setStyle("-fx-padding: 10; -fx-border-color: -border-color; -fx-border-radius: 6;");

        Label title = new Label("Local GGUF (llama.cpp)");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        localGgufPathField = new TextField();
        localGgufPathField.setPromptText("مسار ملف الموديل (.gguf)");

        Button browseModelBtn = new Button("Browse GGUF");
        browseModelBtn.setOnAction(e -> browseFile(localGgufPathField, "*.gguf"));

        HBox modelRow = new HBox(8, localGgufPathField, browseModelBtn);
        HBox.setHgrow(localGgufPathField, Priority.ALWAYS);

        // llama-cli
        llamaCliField = new TextField("llama-cli");
        llamaCliField.setPromptText("مسار llama-cli.exe (مثال: C:\\llama\\llama-cli.exe)");

        Button browseCliBtn = new Button("Browse llama-cli");
        browseCliBtn.setOnAction(e -> browseFile(llamaCliField, "*.exe"));

        HBox cliRow = new HBox(8, new Label("llama-cli:"), llamaCliField, browseCliBtn);
        HBox.setHgrow(llamaCliField, Priority.ALWAYS);

        Label ggufHelp = new Label(
            "Download a .gguf model from HuggingFace (e.g. TheBloke/Llama-2-7B-Chat-GGUF, Q4_K_M).\n" +
            "Get llama-cli from github.com/ggml-org/llama.cpp/releases.\n" +
            "Browse to both files above. Requires 8GB+ RAM for 7B models.\n" +
            "Light models: use Q3_K_M or Q4_K_S quantizations for less memory.");
        ggufHelp.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-muted; -fx-wrap-text: true;");
        localGgufBox.getChildren().addAll(title, modelRow, cliRow, ggufHelp);
        
//        VBox localGgufBox = new VBox(4);
//        localGgufPathField = new TextField();
//        localGgufPathField.setPromptText("Path to .gguf model file");
//        Button browseBtn = new Button("Browse");
//        browseBtn.setOnAction(e -> {
//            FileChooser fc = new FileChooser();
//            fc.setTitle("Select GGUF Model File");
//            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("GGUF Models", "*.gguf"));
//            File sel = fc.showOpenDialog(getWindow());
//            if (sel != null) localGgufPathField.setText(sel.getAbsolutePath());
//        });
//        HBox pathRow = new HBox(6, localGgufPathField, browseBtn);
//        HBox.setHgrow(localGgufPathField, Priority.ALWAYS);
//        localGgufBox.getChildren().add(pathRow);

        
        
        
        
        
        aiProviderCombo.setOnAction(e -> {
            String v = aiProviderCombo.getValue();
            geminiBox.setVisible(v != null && v.startsWith("Google Gemini"));
            geminiBox.setManaged(v != null && v.startsWith("Google Gemini"));
            openaiBox.setVisible(v != null && v.startsWith("OpenAI Compatible"));
            openaiBox.setManaged(v != null && v.startsWith("OpenAI Compatible"));
            ollamaBox.setVisible(v != null && v.startsWith("Ollama"));
            ollamaBox.setManaged(v != null && v.startsWith("Ollama"));
            localGgufBox.setVisible(v != null && v.startsWith("Local GGUF"));
            localGgufBox.setManaged(v != null && v.startsWith("Local GGUF"));
        });

        aiStatusLabel = new Label();
        aiStatusLabel.setStyle("-fx-text-fill: -text-muted; -fx-font-size: 11px;");

        aiTestBtn = new Button("Test Connection");
        aiTestBtn.setOnAction(e -> testAiConnection());

        root.getChildren().addAll(
            new Label("AI Provider:"), aiProviderCombo,
            geminiBox, openaiBox, ollamaBox, localGgufBox,
            new HBox(6, aiTestBtn),
            aiStatusLabel
        );
        tab.setContent(new ScrollPane(root) {{
            setFitToWidth(true); setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        }});
        
        
        
               // تحميل مسار GGUF عند اختيار الـ Provider
        aiProviderCombo.setOnAction(e -> {
            String v = aiProviderCombo.getValue();
            geminiBox.setVisible(v != null && v.startsWith("Google Gemini"));
            geminiBox.setManaged(v != null && v.startsWith("Google Gemini"));
            openaiBox.setVisible(v != null && v.startsWith("OpenAI Compatible"));
            openaiBox.setManaged(v != null && v.startsWith("OpenAI Compatible"));
            ollamaBox.setVisible(v != null && v.startsWith("Ollama"));
            ollamaBox.setManaged(v != null && v.startsWith("Ollama"));
            localGgufBox.setVisible(v != null && v.startsWith("Local GGUF"));
            localGgufBox.setManaged(v != null && v.startsWith("Local GGUF"));

            if (v != null && v.startsWith("Local GGUF")) {
                localGgufPathField.setText(loadGgufPathFromConfig());
            }
        });
        
        
        return tab;
    }
    
    
    
        private void browseFile(TextField field, String filter) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Files", filter));
        File file = fc.showOpenDialog(getWindow());
        if (file != null) {
            field.setText(file.getAbsolutePath());
        }
    }

    private void browseDirectory(TextField field) {
        DirectoryChooser dc = new DirectoryChooser();
        File dir = dc.showDialog(getWindow());
        if (dir != null) {
            field.setText(dir.getAbsolutePath());
        }
    }

    private Tab buildEnvironmentTab() {
        Tab tab = new Tab("Environment");
        tab.setGraphic(IconManager.imageView(IconManager.FILE_CODE, 14));

        VBox root = new VBox(8);
        root.setPadding(new Insets(12));

        Label info = new Label("Configure custom paths for development tools. "
            + "If a path is set here, it will be used instead of searching PATH.");
        info.setWrapText(true);
        info.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted;");
        root.getChildren().add(info);

        root.getChildren().add(buildToolCategory("Node.js Ecosystem", ToolsConfig.NODE_KEYS));
        root.getChildren().add(buildToolCategory("Mobile Development", ToolsConfig.MOBILE_KEYS));
        root.getChildren().add(buildToolCategory("Java Ecosystem", ToolsConfig.JAVA_KEYS));
        root.getChildren().add(buildToolCategory("Python", ToolsConfig.PYTHON_KEYS));
        root.getChildren().add(buildToolCategory("Rust", ToolsConfig.RUST_KEYS));
        root.getChildren().add(buildToolCategory("Go", ToolsConfig.GO_KEYS));
        root.getChildren().add(buildToolCategory("PHP", ToolsConfig.PHP_KEYS));
        root.getChildren().add(buildToolCategory("Flutter / Dart", ToolsConfig.FLUTTER_KEYS));
        root.getChildren().add(buildToolCategory(".NET", ToolsConfig.DOTNET_KEYS));
        root.getChildren().add(buildToolCategory("Other", ToolsConfig.OTHER_KEYS));

        tab.setContent(new ScrollPane(root) {{
            setFitToWidth(true); setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        }});
        return tab;
    }

    private TitledPane buildToolCategory(String title, String[] keys) {
        VBox content = new VBox(6);
        content.setPadding(new Insets(4, 0, 4, 0));
        for (String key : keys) {
            TextField tf = new TextField();
            tf.setPromptText(key + " path (leave empty to use PATH)");
            toolFields.put(key, tf);

            Button browseBtn = new Button("Browse");
            browseBtn.setOnAction(e -> browseDirectory(tf));

            Hyperlink dlLink = new Hyperlink("Get " + key);
            String docUrl = ToolsConfig.getDocUrl(key);
            dlLink.setOnAction(e -> openUrl(docUrl));

            HBox row = new HBox(6, new Label(key + ":"), tf, browseBtn, dlLink);
            HBox.setHgrow(tf, Priority.ALWAYS);
            row.setAlignment(Pos.CENTER_LEFT);
            content.getChildren().add(row);
        }
        TitledPane pane = new TitledPane(title, content);
        pane.setExpanded(false);
        return pane;
    }

    private Tab buildMonacoTab() {
        Tab tab = new Tab("Monaco Editor");
        tab.setGraphic(IconManager.imageView(IconManager.FILE_CODE, 14));

        VBox root = new VBox(6);
        root.setPadding(new Insets(12));

        TitledPane displayPane = new TitledPane("Display", new VBox(6) {{
            minimapCheck = new CheckBox("Show Minimap"); getChildren().add(minimapCheck);
            bracketColorsCheck = new CheckBox("Bracket Pair Colorization"); getChildren().add(bracketColorsCheck);
            smoothScrollCheck = new CheckBox("Smooth Scrolling"); getChildren().add(smoothScrollCheck);
            cursorBlinkCombo = new ComboBox<>();
            cursorBlinkCombo.getItems().addAll("smooth", "blink", "phase", "expand", "solid");
            cursorBlinkCombo.setValue("smooth");
            getChildren().add(new HBox(6, new Label("Cursor Blinking:"), cursorBlinkCombo));
            renderLineHighlightCombo = new ComboBox<>();
            renderLineHighlightCombo.getItems().addAll("all", "line", "gutter", "none");
            getChildren().add(new HBox(6, new Label("Render Line Highlight:"), renderLineHighlightCombo));
            monacoThemeCombo = new ComboBox<>();
            monacoThemeCombo.getItems().addAll(MonacoEditor.AVAILABLE_THEMES);
            monacoThemeCombo.setValue("darcula");
            getChildren().add(new HBox(6, new Label("Theme:"), monacoThemeCombo));
        }});
        displayPane.setExpanded(true);

        TitledPane suggestPane = new TitledPane("Suggestions & Completion", new VBox(6) {{
            parameterHintsCheck = new CheckBox("Parameter Hints"); getChildren().add(parameterHintsCheck);
            inlineSuggestCheck = new CheckBox("Inline Suggestions"); getChildren().add(inlineSuggestCheck);
            codeLensCheck = new CheckBox("Code Lens"); getChildren().add(codeLensCheck);
            quickSuggestCheck = new CheckBox("Quick Suggestions While Typing"); getChildren().add(quickSuggestCheck);
            suggestTriggerCheck = new CheckBox("Suggest on Trigger Characters"); getChildren().add(suggestTriggerCheck);
            wordBasedSuggestCheck = new CheckBox("Word Based Suggestions"); getChildren().add(wordBasedSuggestCheck);
            matchBracketsCombo = new ComboBox<>();
            matchBracketsCombo.getItems().addAll("always", "near", "never");
            getChildren().add(new HBox(6, new Label("Match Brackets:"), matchBracketsCombo));
            tabCompletionCombo = new ComboBox<>();
            tabCompletionCombo.getItems().addAll("on", "off", "onlySnippets");
            getChildren().add(new HBox(6, new Label("Tab Completion:"), tabCompletionCombo));
        }});
        suggestPane.setExpanded(false);

        root.getChildren().addAll(displayPane, suggestPane);
        tab.setContent(new ScrollPane(root) {{
            setFitToWidth(true); setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        }});
        return tab;
    }

    private Tab buildRegistryTab() {
        Tab tab = new Tab("Registry");
        tab.setGraphic(IconManager.imageView(IconManager.SETTINGS, 14));

        VBox root = new VBox(6);
        root.setPadding(new Insets(12));

        registryFilterField = new TextField();
        registryFilterField.setPromptText("Filter by key\u2026");
        registryFilterField.textProperty().addListener((o,ov,nv) -> filterRegistry());

        registryTable = new TableView<>(registryItems);
        registryTable.setPrefHeight(300);

        Label srcInfo = new Label("Legend: EditorPrefs / ThemePrefs = Java Preferences  |  AI = ~/.webide/ai.properties  |  Registry = ~/.webide/registry.properties");
        srcInfo.setStyle("-fx-text-fill: #888; -fx-font-size: 10;");

        TableColumn<RegistryEntry, String> keyCol = new TableColumn<>("Key");
        keyCol.setPrefWidth(180);
        keyCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().key));

        TableColumn<RegistryEntry, String> valCol = new TableColumn<>("Value");
        valCol.setPrefWidth(250);
        valCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().value));

        TableColumn<RegistryEntry, String> srcCol = new TableColumn<>("Source");
        srcCol.setPrefWidth(100);
        srcCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().source));

        registryTable.getColumns().addAll(keyCol, valCol, srcCol);
        registryTable.getSelectionModel().selectedItemProperty().addListener((o,ov,nv) -> {
            if (nv != null) { registryKeyField.setText(nv.key); registryValueField.setText(nv.value); }
        });

        registryKeyField = new TextField();
        registryKeyField.setPromptText("Key");
        registryValueField = new TextField();
        registryValueField.setPromptText("Value");

        Button addRegBtn = new Button("Add / Update");
        addRegBtn.setOnAction(e -> addRegistryEntry());

        Button deleteRegBtn = new Button("Delete Selected");
        deleteRegBtn.setOnAction(e -> deleteRegistryEntry());

        Button clearFilterBtn = new Button("Clear Filter");
        clearFilterBtn.setOnAction(e -> { registryFilterField.clear(); loadRegistry(); });

        HBox btnRow = new HBox(6, addRegBtn, deleteRegBtn, clearFilterBtn);
        HBox editRow = new HBox(6, new Label("Key:"), registryKeyField, new Label("Value:"), registryValueField);
        HBox.setHgrow(registryKeyField, Priority.ALWAYS);
        HBox.setHgrow(registryValueField, Priority.ALWAYS);

        root.getChildren().addAll(registryFilterField, registryTable, srcInfo, editRow, btnRow);
        tab.setContent(root);
        return tab;
    }

    private Tab buildDebuggerTab() {
        Tab tab = new Tab("Debugger");
        tab.setGraphic(IconManager.imageView(IconManager.BUG, 14));

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(8);
        grid.setPadding(new Insets(12));

        autoBreakCheck = new CheckBox("Auto-break on unhandled error");
        grid.addRow(0, autoBreakCheck);

        stepDelaySpinner = new Spinner<>(100, 5000, 500, 100);
        stepDelaySpinner.setEditable(true);
        HBox stepBox = new HBox(6, new Label("Step delay (ms):"), stepDelaySpinner);
        grid.addRow(1, stepBox);

        Label bpLabel = new Label("Breakpoint gutter icon:");
        bpDotRadio = new RadioButton("Dot"); bpDotRadio.setSelected(true);
        bpCrossRadio = new RadioButton("Cross");
        bpCircleRadio = new RadioButton("Circle");
        ToggleGroup bpGroup = new ToggleGroup();
        bpDotRadio.setToggleGroup(bpGroup);
        bpCrossRadio.setToggleGroup(bpGroup);
        bpCircleRadio.setToggleGroup(bpGroup);
        HBox bpBox = new HBox(6, bpDotRadio, bpCrossRadio, bpCircleRadio);
        grid.addRow(2, bpLabel);
        grid.addRow(3, bpBox);

        tab.setContent(new ScrollPane(new VBox(grid)) {{
            setFitToWidth(true); setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        }});
        return tab;
    }

    // ================================================================
    //  Button Bar
    // ================================================================

    private HBox buildButtonBar() {
        Button saveBtn = new Button("Save");
        saveBtn.setStyle("-fx-background-color: -accent; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 24; -fx-cursor: hand;");
        saveBtn.setOnAction(e -> {
            saveAllSettings();   // بيحفظ + بيطبق لايف في خطوة واحدة
            stage.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-radius: 6; -fx-padding: 8 24; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> stage.close());

        Button applyBtn = new Button("Apply");
        applyBtn.setStyle("-fx-background-radius: 6; -fx-padding: 8 24; -fx-cursor: hand;");
        applyBtn.setOnAction(e -> saveAllSettings());   // حفظ + تطبيق بدون إغلاق

        HBox bar = new HBox(8, cancelBtn, applyBtn, saveBtn);
        bar.setAlignment(Pos.CENTER_RIGHT);
        return bar;
    }

    // ================================================================
    //  Load / Save
    // ================================================================

    private void loadAllSettings() {
        // General
        themeCombo.setValue(ThemeManager.getInstance().getCurrentThemeName());
        fontCombo.setValue(EditorSettings.getFontFamily());
        fontSizeSpinner.getValueFactory().setValue(EditorSettings.getFontSize());
        cursorStyleCombo.setValue(EditorSettings.getCursorStyle());
        cursorWidthSpinner.getValueFactory().setValue(EditorSettings.getCursorWidth());
        restoreSessionCheck.setSelected(EditorSettings.isRestoreSession());

        // Editor
        wordWrapCheck.setSelected(EditorSettings.isWordWrap());
        lineNumbersCheck.setSelected(EditorSettings.isShowLineNumbers());
        highlightLineCheck.setSelected(EditorSettings.isHighlightCurrentLine());
        matchBracketsCheck.setSelected(EditorSettings.isHighlightMatchingBrackets());
        showWhitespaceCheck.setSelected(EditorSettings.isShowWhitespace());
        indentGuideCheck.setSelected(EditorSettings.isShowIndentGuide());
        autoCloseBracketsCheck.setSelected(EditorSettings.isAutoCloseBrackets());
        autoSaveCheck.setSelected(EditorSettings.isAutoSave());
        formatOnSaveCheck.setSelected(EditorSettings.isFormatOnSave());
        autoIndentCheck.setSelected(EditorSettings.isAutoIndent());
        trimWhitespaceCheck.setSelected(EditorSettings.isTrimWhitespace());
        tabSizeSpinner.getValueFactory().setValue(EditorSettings.getTabSize());
        lineHeightSpinner.getValueFactory().setValue(EditorSettings.getLineHeight());
        syntaxThemeCombo.setValue(EditorSettings.getSyntaxTheme());

        // AI
        loadAiSettings();

        // Monaco
        loadMonacoSettings();

        // Registry
        loadRegistry();

        // Debugger
        loadDebuggerSettings();

        // Environment
        loadEnvironmentSettings();
    }
    
    
    
            private void saveAiSettings() {
        try {
            new File(AI_CONFIG_DIR).mkdirs();
            Properties p = new Properties();
            String v = aiProviderCombo.getValue();

            if (v != null && v.startsWith("Google Gemini")) {
                p.setProperty("provider", "gemini");
                p.setProperty("gemini.key", geminiKeyField.getText().trim());
                p.setProperty("gemini.model", geminiModelField.getText().trim());
            } 
            else if (v != null && v.startsWith("OpenAI Compatible")) {
                p.setProperty("provider", "openai");
                p.setProperty("openai.endpoint", openaiEndpointField.getText().trim());
                p.setProperty("openai.key", openaiKeyField.getText().trim());
                String model = openaiModelCombo.getValue() != null ? 
                               openaiModelCombo.getValue().toString() : "";
                p.setProperty("openai.model", model.isEmpty() ? "llama-3.3-70b-versatile" : model.trim());
            } 
            else if (v != null && v.startsWith("Ollama")) {
                p.setProperty("provider", "ollama");
                p.setProperty("ollama.endpoint", ollamaEndpointField.getText().trim());
                p.setProperty("ollama.model", ollamaModelField.getText().trim());
            } 
//            else if (v != null && v.startsWith("Local GGUF")) {
//                p.setProperty("provider", "localgguf");
//                p.setProperty("localgguf.path", localGgufPathField.getText().trim());
//                p.setProperty("llama.cli.path", "llama-cli");
//            }
            
             else if (v != null && v.startsWith("Local GGUF")) {
                p.setProperty("provider", "localgguf");
                p.setProperty("localgguf.path", localGgufPathField.getText().trim());
                p.setProperty("llama.cli.path", llamaCliField.getText().trim()); // مهم
            }

            try (FileOutputStream out = new FileOutputStream(AI_CONFIG_FILE)) {
                p.store(out, "Webide AI Config");
            }
            aiStatusLabel.setText("✓ AI Settings saved");
        } catch (Exception ex) {
            aiStatusLabel.setText("✗ Failed to save: " + ex.getMessage());
        }
    }
    

    private void saveAllSettings() {
        // General
        ThemeManager.getInstance().setThemeByName(themeCombo.getValue());
        EditorSettings.setFontFamily(comboText(fontCombo));
        EditorSettings.setFontSize(fontSizeSpinner.getValue());
        EditorSettings.setCursorStyle(comboText(cursorStyleCombo));
        EditorSettings.setCursorWidth(cursorWidthSpinner.getValue());
        EditorSettings.setRestoreSession(restoreSessionCheck.isSelected());

        // Editor
        EditorSettings.setWordWrap(wordWrapCheck.isSelected());
        EditorSettings.setShowLineNumbers(lineNumbersCheck.isSelected());
        EditorSettings.setHighlightCurrentLine(highlightLineCheck.isSelected());
        EditorSettings.setHighlightMatchingBrackets(matchBracketsCheck.isSelected());
        EditorSettings.setShowWhitespace(showWhitespaceCheck.isSelected());
        EditorSettings.setShowIndentGuide(indentGuideCheck.isSelected());
        EditorSettings.setAutoCloseBrackets(autoCloseBracketsCheck.isSelected());
        EditorSettings.setAutoSave(autoSaveCheck.isSelected());
        EditorSettings.setFormatOnSave(formatOnSaveCheck.isSelected());
        EditorSettings.setAutoIndent(autoIndentCheck.isSelected());
        EditorSettings.setTrimWhitespace(trimWhitespaceCheck.isSelected());
        EditorSettings.setTabSize(tabSizeSpinner.getValue());
        EditorSettings.setLineHeight(lineHeightSpinner.getValue());
        EditorSettings.setSyntaxTheme(comboText(syntaxThemeCombo));

        // AI
        saveAiSettings();

        // Monaco
        saveMonacoSettings();

        
        saveAiSettings();
        
        // Debugger
        saveDebuggerSettings();

        // Environment
        saveEnvironmentSettings();

        // اكتب Preferences على القرص — بدون saveRegistry() اللي كانت بتمسح الإعدادات
        EditorSettings.flush();

        if (onApplied != null) {
            onApplied.run();
        }
        
        
      
        
        
    }

    /** ComboBox قابل للتحرير — getValue() ممكن يرجع null لو المستخدم كتب نص */
    private static String comboText(ComboBox<String> combo) {
        if (combo == null) return null;
        String v = combo.getValue();
        if (v != null && !v.trim().isEmpty()) return v.trim();
        if (combo.getEditor() != null) {
            String t = combo.getEditor().getText();
            if (t != null && !t.trim().isEmpty()) return t.trim();
        }
        return v;
    }

    // ================================================================
    //  AI Settings
    // ================================================================

    private void loadAiSettings() {
        try {
            File f = new File(AI_CONFIG_FILE);
            if (f.exists()) {
                Properties p = new Properties();
                try (FileInputStream in = new FileInputStream(f)) { p.load(in); }
                String providerType = p.getProperty("provider", "gemini");
                String comboVal = "Google Gemini (Online)";
                if ("openai".equals(providerType)) comboVal = "OpenAI Compatible (Online)";
                else if ("ollama".equals(providerType)) comboVal = "Ollama (Local)";
                else if ("localgguf".equals(providerType)) comboVal = "Local GGUF (llama.cpp)";
                aiProviderCombo.setValue(comboVal);

                geminiKeyField.setText(p.getProperty("gemini.key", ""));
                geminiModelField.setText(p.getProperty("gemini.model", "gemini-2.0-flash"));

                String oep = p.getProperty("openai.endpoint", "https://api.groq.com/openai/v1/chat/completions");
                openaiEndpointField.setText(oep);
                openaiKeyField.setText(p.getProperty("openai.key", ""));
                openaiModelCombo.setValue(p.getProperty("openai.model", "llama-3.3-70b-versatile"));
                openaiPresetCombo.setValue(detectPreset(oep));

                ollamaEndpointField.setText(p.getProperty("ollama.endpoint", "http://localhost:11434/api/chat"));
                ollamaModelField.setText(p.getProperty("ollama.model", "llama3.2"));
                localGgufPathField.setText(p.getProperty("localgguf.path", ""));
                llamaCliField.setText(p.getProperty("llama.cli.path", "llama-cli"));
            } else {
                aiProviderCombo.setValue("Google Gemini (Online)");
                openaiPresetCombo.setValue("Groq (Free)");
            }
        } catch (Exception ignored) {}
        // trigger visibility
        String v = aiProviderCombo.getValue();
        if (v != null) aiProviderCombo.getOnAction().handle(null);
    }

    
    
        private String loadGgufPathFromConfig() {
        try {
            File f = new File(AI_CONFIG_FILE);
            if (f.exists()) {
                Properties p = new Properties();
                try (FileInputStream in = new FileInputStream(f)) {
                    p.load(in);
                }
                return p.getProperty("localgguf.path", "");
            }
        } catch (Exception ignored) {}
        return "";
    }
    
    

    // ================================================================
    //  Monaco Settings
    // ================================================================

    private void loadMonacoSettings() {
        minimapCheck.setSelected(EditorSettings.isMinimapEnabled());
        bracketColorsCheck.setSelected(EditorSettings.isBracketColors());
        parameterHintsCheck.setSelected(EditorSettings.isParameterHints());
        smoothScrollCheck.setSelected(EditorSettings.isSmoothScroll());
        inlineSuggestCheck.setSelected(EditorSettings.isInlineSuggest());
        codeLensCheck.setSelected(EditorSettings.isCodeLens());
        quickSuggestCheck.setSelected(EditorSettings.isQuickSuggest());
        suggestTriggerCheck.setSelected(EditorSettings.isSuggestTrigger());
        wordBasedSuggestCheck.setSelected(EditorSettings.isWordBasedSuggest());
        cursorBlinkCombo.setValue(EditorSettings.getCursorBlink());
        renderLineHighlightCombo.setValue(EditorSettings.getRenderLineHighlight());
        matchBracketsCombo.setValue(EditorSettings.getMatchBracketsMode());
        tabCompletionCombo.setValue(EditorSettings.getTabCompletion());
        monacoThemeCombo.setValue(EditorSettings.getMonacoTheme());
    }

    private void saveMonacoSettings() {
        // ① حفظ كل إعدادات Monaco في EditorSettings (Preferences)
        EditorSettings.setMinimapEnabled(minimapCheck.isSelected());
        EditorSettings.setBracketColors(bracketColorsCheck.isSelected());
        EditorSettings.setParameterHints(parameterHintsCheck.isSelected());
        EditorSettings.setSmoothScroll(smoothScrollCheck.isSelected());
        EditorSettings.setInlineSuggest(inlineSuggestCheck.isSelected());
        EditorSettings.setCodeLens(codeLensCheck.isSelected());
        EditorSettings.setQuickSuggest(quickSuggestCheck.isSelected());
        EditorSettings.setSuggestTrigger(suggestTriggerCheck.isSelected());
        EditorSettings.setWordBasedSuggest(wordBasedSuggestCheck.isSelected());
        EditorSettings.setCursorBlink(comboText(cursorBlinkCombo));
        EditorSettings.setRenderLineHighlight(comboText(renderLineHighlightCombo));
        EditorSettings.setMatchBracketsMode(comboText(matchBracketsCombo));
        EditorSettings.setTabCompletion(comboText(tabCompletionCombo));
        EditorSettings.setMonacoTheme(comboText(monacoThemeCombo));
    }

    // ================================================================
    //  Registry
    // ================================================================

    private void loadRegistry() {
        registryItems.clear();
        try {
            // 1. EditorSettings Preferences
            Preferences editorPrefs = Preferences.userRoot().node("/com/eagle/util/EditorSettings");
            for (String k : editorPrefs.keys()) {
                registryItems.add(new RegistryEntry(k, editorPrefs.get(k, ""), "EditorPrefs"));
            }
        } catch (Exception ignored) {}
        try {
            // 2. ThemeManager Preferences
            Preferences themePrefs = Preferences.userRoot().node("/com/eagle/util/ThemeManager");
            for (String k : themePrefs.keys()) {
                registryItems.add(new RegistryEntry(k, themePrefs.get(k, ""), "ThemePrefs"));
            }
        } catch (Exception ignored) {}
        try {
            // 3. AI properties file
            File af = new File(AI_CONFIG_FILE);
            if (af.exists()) {
                Properties ap = new Properties();
                try (FileInputStream in = new FileInputStream(af)) { ap.load(in); }
                for (String k : ap.stringPropertyNames()) {
                    registryItems.add(new RegistryEntry(k, ap.getProperty(k), "AI"));
                }
            }
        } catch (Exception ignored) {}
        try {
            // 4. Custom Registry properties file
            File rf = new File(REGISTRY_FILE);
            if (rf.exists()) {
                Properties rp = new Properties();
                try (FileInputStream in = new FileInputStream(rf)) { rp.load(in); }
                for (String k : rp.stringPropertyNames()) {
                    registryItems.add(new RegistryEntry(k, rp.getProperty(k), "Registry"));
                }
            }
        } catch (Exception ignored) {}
        registryTable.setItems(FXCollections.observableArrayList(registryItems));
    }

    private void saveRegistry() {
        Properties regProps = new Properties();
        for (RegistryEntry e : registryItems) {
            if ("Registry".equals(e.source)) {
                regProps.setProperty(e.key, e.value);
            }
        }
        new File(AI_CONFIG_DIR).mkdirs();
        try (FileOutputStream out = new FileOutputStream(REGISTRY_FILE)) {
            regProps.store(out, "Webide Registry");
        } catch (Exception ignored) {}
    }

    private void addRegistryEntry() {
        String key = registryKeyField.getText().trim();
        String val = registryValueField.getText().trim();
        if (key.isEmpty()) return;
        for (RegistryEntry e : registryItems) {
            if (e.key.equals(key) && "Registry".equals(e.source)) {
                e.value = val; registryTable.refresh(); registryKeyField.clear(); registryValueField.clear();
                saveRegistry();
                return;
            }
        }
        registryItems.add(new RegistryEntry(key, val, "Registry"));
        registryKeyField.clear(); registryValueField.clear();
        registryTable.setItems(FXCollections.observableArrayList(registryItems));
        saveRegistry();
    }

    private void deleteRegistryEntry() {
        RegistryEntry sel = registryTable.getSelectionModel().getSelectedItem();
        if (sel != null) {
            registryItems.remove(sel);
            registryKeyField.clear();
            registryValueField.clear();
            saveRegistry();
        }
    }

    private void filterRegistry() {
        String q = registryFilterField.getText().toLowerCase();
        if (q.isEmpty()) { registryTable.setItems(FXCollections.observableArrayList(registryItems)); return; }
        ObservableList<RegistryEntry> filtered = FXCollections.observableArrayList();
        for (RegistryEntry e : registryItems) {
            if (e.key.toLowerCase().contains(q) || e.value.toLowerCase().contains(q) || e.source.toLowerCase().contains(q))
                filtered.add(e);
        }
        registryTable.setItems(filtered);
    }

    // ================================================================
    //  Debugger
    // ================================================================

    private void loadDebuggerSettings() {
        autoBreakCheck.setSelected(EditorSettings.isAutoBreak());
        stepDelaySpinner.getValueFactory().setValue(EditorSettings.getStepDelay());
        String icon = EditorSettings.getBpIcon();
        if ("cross".equals(icon))  { bpCrossRadio.setSelected(true); }
        else if ("circle".equals(icon)) { bpCircleRadio.setSelected(true); }
        else { bpDotRadio.setSelected(true); }
    }

    private void saveDebuggerSettings() {
        EditorSettings.setAutoBreak(autoBreakCheck.isSelected());
        EditorSettings.setStepDelay(stepDelaySpinner.getValue());
        if (bpCrossRadio.isSelected())        EditorSettings.setBpIcon("cross");
        else if (bpCircleRadio.isSelected())  EditorSettings.setBpIcon("circle");
        else                                  EditorSettings.setBpIcon("dot");
    }

    // ================================================================
    //  Environment
    // ================================================================

    private void loadEnvironmentSettings() {
        Map<String, String> paths = ToolsConfig.getAllPaths();
        for (Map.Entry<String, TextField> e : toolFields.entrySet()) {
            String saved = paths.get(e.getKey());
            if (saved != null && !saved.isEmpty()) {
                e.getValue().setText(saved);
            }
        }
    }

    private void saveEnvironmentSettings() {
        Properties props = new Properties();
        for (Map.Entry<String, TextField> e : toolFields.entrySet()) {
            String val = e.getValue().getText().trim();
            if (!val.isEmpty()) {
                props.setProperty(e.getKey() + ".path", val);
            }
        }
        ToolsConfig.save(props);
    }

    // ================================================================
    //  Helpers
    // ================================================================

    private static void setAllGridColSpan(GridPane grid, int col) {
        for (javafx.scene.Node n : grid.getChildren()) {
            if (GridPane.getColumnIndex(n) == null) GridPane.setColumnIndex(n, 0);
            if (GridPane.getRowIndex(n) == null) GridPane.setRowIndex(n, grid.getChildren().indexOf(n));
        }
    }

    private void testAiConnection() {
        aiStatusLabel.setText("Testing\u2026");
        aiTestBtn.setDisable(true);
        new Thread(() -> {
            String result;
            try {
                String provider = aiProviderCombo.getValue();
                if (provider != null && provider.startsWith("Local GGUF")) {
                    String modelPath = localGgufPathField.getText().trim();
                    String cliPath = llamaCliField.getText().trim();
                    java.io.File modelFile = new java.io.File(modelPath);
                    if (!modelFile.exists()) {
                        result = "❌ GGUF file not found: " + modelPath;
                    } else {
                        result = "✅ GGUF file: " + modelFile.length() / (1024*1024) + " MB\n";
                        // Check CLI
                        try {
                            ProcessBuilder pb = new ProcessBuilder(cliPath, "--version");
                            pb.redirectErrorStream(true);
                            Process p = pb.start();
                            p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
                            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                            String ver = br.readLine();
                            result += "✅ CLI: " + (ver != null ? ver : cliPath);
                        } catch (Exception ex) {
                            result += "❌ CLI not found: " + cliPath;
                        }
                    }
                } else if (provider != null && provider.startsWith("Ollama")) {
                    // Ollama: ping /api/tags
                    String base = ollamaEndpointField.getText().trim()
                        .replace("/api/chat", "").replace("/api/generate", "");
                    java.net.URL url = new java.net.URL(base + "/api/tags");
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(5000); conn.setReadTimeout(5000);
                    int code = conn.getResponseCode();
                    result = code == 200 ? "✅ Ollama reachable (HTTP " + code + ")" : "⚠ HTTP " + code;
                } else if (provider != null && provider.startsWith("OpenAI Compatible")) {
                    String endpoint = openaiEndpointField.getText().trim();
                    String key = openaiKeyField.getText().trim();
                    java.net.URL url = new java.net.URL(endpoint
                        .replace("/chat/completions", "/models"));
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("Authorization", "Bearer " + key);
                    conn.setConnectTimeout(6000); conn.setReadTimeout(6000);
                    int code = conn.getResponseCode();
                    if (code == 200 || code == 307) {
                        result = "✅ Endpoint reachable (HTTP " + code + ")";
                    } else {
                        String body = "";
                        try {
                            java.io.InputStream es = conn.getErrorStream();
                            if (es != null) body = new java.util.Scanner(es, "UTF-8").useDelimiter("\\A").next();
                        } catch (Exception ignored) {}
                        result = "⚠ HTTP " + code + (body.isEmpty() ? "" : " — " + body.substring(0, Math.min(120, body.length())));
                        if (code == 403) result += "\n💡 تأكد من صحة API key أو جرب توليد واحدة جديدة من موقع المزود";
                    }
                } else {
                    // Gemini: simple check
                    String key = geminiKeyField.getText().trim();
                    if (key.isEmpty()) { result = "❌ No API key entered"; }
                    else {
                        java.net.URL url = new java.net.URL(
                            "https://generativelanguage.googleapis.com/v1/models?key=" + key);
                        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                        conn.setConnectTimeout(6000); conn.setReadTimeout(6000);
                        int code = conn.getResponseCode();
                        result = code == 200 ? "✅ Gemini key valid" : "❌ Gemini rejected key (HTTP " + code + ")";
                    }
                }
            } catch (Exception ex) {
                result = "❌ " + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
            }
            String finalResult = result;
            Platform.runLater(() -> { aiStatusLabel.setText(finalResult); aiTestBtn.setDisable(false); });
        }).start();
    }

    private void openUrl(String url) {
        try {
            if (java.awt.Desktop.isDesktopSupported())
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception ignored) {}
    }

    private String detectPreset(String endpoint) {
        if (endpoint.contains("groq.com")) return "Groq (Free)";
        if (endpoint.contains("models.inference.ai.azure.com")) return "GitHub Models (Free)";
        if (endpoint.contains("openrouter.ai")) return "OpenRouter (Free)";
        if (endpoint.contains("deepseek.com")) return "DeepSeek (Free)";
        return "Custom";
    }

    // ================================================================
    //  Model
    // ================================================================

    public static class RegistryEntry {
        public String key;
        public String value;
        public String source;
        public RegistryEntry(String key, String value, String source) {
            this.key = key; this.value = value; this.source = source;
        }
    }
}