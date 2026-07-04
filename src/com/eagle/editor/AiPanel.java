package com.eagle.editor;

import com.eagle.controller.TabManager;
import com.eagle.icons.IconManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

public class AiPanel extends BorderPane {

    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.webide";
    private static final String CONFIG_FILE = CONFIG_DIR + "/ai.properties";

    private static final String GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String OLLAMA_DEFAULT = "http://localhost:11434/api/chat";

    private final VBox messagesContainer = new VBox(8);
    private final TextField inputField = new TextField();
    private final Button sendBtn = new Button("Send");
    private final Button stopBtn = new Button("Stop");
    private final Button fixBtn = new Button("Fix Code");
    private final Button explainBtn = new Button("Explain");
    private final Label statusLabel = new Label();
    private ScrollPane chatScrollPane;
    private ToggleButton autoCommitBtn;

    private TabManager tabManager;
    private File projectRoot;
    private Runnable onFileCreated;
    private final List<ChatMessage> chatHistory = new ArrayList<>();
    private final Gson gson = new Gson();

    private String providerType = "gemini";
    private String geminiKey = "";
    private String geminiModel = "gemini-2.0-flash";
    private String openaiEndpoint = "https://api.groq.com/openai/v1/chat/completions";
    private String openaiKey = "";
    private String openaiModel = "llama-3.3-70b-versatile";
    private String ollamaEndpoint = OLLAMA_DEFAULT;
    private String ollamaModel = "llama3.2";
    private String localGgufPath = "";
    
    // GGUF Fields
    private String llamaCliPath = "llama-cli";
    private Process activeProcess;
    
    private volatile boolean stopRequested = false;
    private Thread workerThread;
    private HttpURLConnection activeConn;
    private Timeline thinkingAnim;
    private int thinkingDots = 0;
    
    // Track streaming state to avoid re-creating files on every flush
    private volatile boolean streamingActive = false;
    private final java.util.Set<String> autoCreatedFiles = new java.util.HashSet<>();
    private VBox streamingBubble = null; // persistent bubble during streaming
    
    // Project Mode
    private volatile boolean projectMode = false;
    private ToggleButton projectModeBtn;
    private String projectContextCache = "";
    private long projectContextLastScan = 0;
    private static final long PROJECT_CONTEXT_TTL = 3000;

    // AiFileManager — tracks all file ops, undo, diff, tree
    private AiFileManager fileManager;
    private VBox sidePanel;
    private ScrollPane sideScroll;
    private SplitPane splitPane;
    private CodeOutlinePanel codeOutline;
    private boolean sidePanelVisible = true;
    private ToggleButton sidePanelToggle;

    // Terminal support
    private static final String SESSION_DIR = CONFIG_DIR + "/sessions";
    private final java.util.Set<String> executedCommands = new java.util.HashSet<>();

    // Chat session persistence
    private String currentSessionId = "";
    private static final Gson SESSION_GSON = new GsonBuilder().setPrettyPrinting().create();

    public AiPanel() {
        getStyleClass().add("ai-panel");
        loadConfig();
        buildUI();
    }

    public void setTabManager(TabManager tm) {
        this.tabManager = tm;
        if (fileManager != null) fileManager.setTabManager(tm);
    }

    private javafx.animation.PauseTransition outlineDebounce;

    public void setActiveEditor(CodeEditor editor) {
        if (codeOutline != null) {
            codeOutline.setEditor(editor);
            if (editor != null) {
                codeOutline.setLspIntegration(editor.getLspIntegration());
                codeOutline.refresh(editor.getText(), editor.getLanguage());
                if (outlineDebounce == null) {
                    outlineDebounce = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
                }
                outlineDebounce.setOnFinished(ev -> {
                    String t = editor.getText();
                    if (t != null) codeOutline.refresh(t, editor.getLanguage());
                });
                editor.textProperty().addListener((obs, old, nv) -> {
                    if (nv != null && !nv.equals(old)) {
                        outlineDebounce.playFromStart();
                    }
                });
            }
        }
    }

    public void setProjectRoot(File projectRoot) {
        this.projectRoot = projectRoot;
        if (projectRoot != null) {
            if (fileManager == null) {
                fileManager = new AiFileManager(projectRoot);
                fileManager.setTabManager(tabManager);
                fileManager.setProblemCallback((file, problems) -> {
                    StringBuilder sb = new StringBuilder("Problems found in " + file.getName() + ":\n");
                    for (String p : problems) sb.append("- ").append(p).append("\n");
                    Platform.runLater(() -> addMessage("System", sb.toString(), false));
                });
            }
            // Auto-enable project mode
            projectMode = true;
            projectModeBtn.setSelected(true);
            projectContextCache = "";
            projectContextLastScan = 0;
            statusLabel.setText("Project Mode ON — " + projectRoot.getName());
            // Refresh side panel with file tree
            Platform.runLater(() -> {
                refreshSidePanel();
            });
            // Load session for this project
            loadSession(projectRoot.getName());
        }
    }

    public void setOnFileCreated(Runnable r) {
        this.onFileCreated = r;
    }

    private void loadConfig() {
        try {
            File f = new File(CONFIG_FILE);
            if (f.exists()) {
                Properties p = new Properties();
                try (FileInputStream in = new FileInputStream(f)) {
                    p.load(in);
                }
                providerType = p.getProperty("provider", "gemini");
                geminiKey = p.getProperty("gemini.key", "");
                geminiModel = p.getProperty("gemini.model", "gemini-2.0-flash");
                String oep = p.getProperty("openai.endpoint", "");
                if (!oep.isEmpty()) openaiEndpoint = oep;
                openaiKey = p.getProperty("openai.key", "");
                openaiModel = p.getProperty("openai.model", "llama-3.3-70b-versatile");
                String olep = p.getProperty("ollama.endpoint", "");
                if (!olep.isEmpty()) ollamaEndpoint = olep;
                ollamaModel = p.getProperty("ollama.model", "llama3.2");
                localGgufPath = p.getProperty("localgguf.path", "");
                llamaCliPath = p.getProperty("llama.cli.path", "llama-cli");
                
            }
        } catch (Exception ignored) {}
    }

    private void saveConfig() {
        try {
            new File(CONFIG_DIR).mkdirs();
            Properties p = new Properties();
            p.setProperty("provider", providerType);
            p.setProperty("gemini.key", geminiKey == null ? "" : geminiKey);
            p.setProperty("gemini.model", geminiModel);
            p.setProperty("openai.endpoint", openaiEndpoint);
            p.setProperty("openai.key", openaiKey == null ? "" : openaiKey);
            p.setProperty("openai.model", openaiModel);
            p.setProperty("ollama.endpoint", ollamaEndpoint);
            p.setProperty("ollama.model", ollamaModel);
            p.setProperty("localgguf.path", localGgufPath);
            p.setProperty("llama.cli.path", llamaCliPath);
            
            try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
                p.store(out, "Webide AI Config");
            }
        } catch (Exception ignored) {}
    }

    private void buildUI() {
        HBox header = new HBox(8);
        header.setPadding(new Insets(8, 10, 4, 10));
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("AI Assistant");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        sidePanelToggle = new ToggleButton("Files");
        sidePanelToggle.setSelected(true);
        sidePanelToggle.setStyle("-fx-background-color: -bg-tertiary; -fx-text-fill: -text-primary; -fx-font-size: 11px; -fx-background-radius: 6; -fx-padding: 4 10; -fx-cursor: hand;");
        sidePanelToggle.setTooltip(new Tooltip("Toggle file changes panel"));
        sidePanelToggle.setOnAction(e -> {
            sidePanelVisible = sidePanelToggle.isSelected();
            if (sideScroll != null && splitPane != null) {
                if (sidePanelVisible) {
                    if (!splitPane.getItems().contains(sideScroll)) {
                        splitPane.getItems().add(0, sideScroll);
                        splitPane.setDividerPositions(0.22);
                    }
                    sideScroll.setVisible(true);
                    sideScroll.setManaged(true);
                } else {
                    sideScroll.setVisible(false);
                    sideScroll.setManaged(false);
                }
            }
        });

        Button settingsBtn = new Button();
        settingsBtn.setGraphic(IconManager.imageView(IconManager.SETTINGS, 14));
        settingsBtn.setStyle("-fx-background: transparent; -fx-text-fill: -text-muted; -fx-cursor: hand; -fx-font-size: 14px;");
        settingsBtn.setTooltip(new Tooltip("AI Settings"));
        settingsBtn.setOnAction(e -> {
            SettingsDialog dialog = new SettingsDialog(null);
            dialog.show();
            loadConfig();
            statusLabel.setText("Settings reloaded.");
        });
        projectModeBtn = new ToggleButton("Project");
        projectModeBtn.setStyle("-fx-background-color: -bg-tertiary; -fx-text-fill: -text-primary; -fx-font-size: 11px; -fx-background-radius: 6; -fx-padding: 4 10; -fx-cursor: hand;");
        projectModeBtn.setTooltip(new Tooltip("Toggle Project Mode (includes file context in prompts)"));
        projectModeBtn.setOnAction(e -> {
            projectMode = projectModeBtn.isSelected();
            projectContextCache = "";
            projectContextLastScan = 0;
            statusLabel.setText(projectMode ? "Project Mode ON" : "Project Mode OFF");
        });
        Button closeBtn = new Button();
        closeBtn.setGraphic(IconManager.imageView(IconManager.CLOSE, 14));
        closeBtn.setStyle("-fx-background: transparent; -fx-text-fill: -text-muted; -fx-cursor: hand; -fx-font-size: 14px;");
        closeBtn.setOnAction(e -> setVisible(false));
        header.getChildren().addAll(title, projectModeBtn, sidePanelToggle, spacer, settingsBtn, closeBtn);

        setTop(header);

        // Side panel with file tree
        sidePanel = new VBox(6);
        sidePanel.setPadding(new Insets(8));
        sidePanel.setStyle("-fx-background-color: -bg-secondary; -fx-border-color: -border-color; -fx-border-width: 0 1 0 0;");
        sidePanel.setPrefWidth(220);
        sidePanel.setMinWidth(180);

        Label sideTitle = new Label("AI File Changes");
        sideTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: -text-primary;");
        sidePanel.getChildren().add(sideTitle);

        // Placeholder — file tree added when fileManager is available
        VBox fileTreePlaceholder = new VBox();
        fileTreePlaceholder.setId("file-tree-container");
        sidePanel.getChildren().add(fileTreePlaceholder);

        // Chat controls
        Button clearChatBtn = new Button("Clear Chat");
        clearChatBtn.setStyle("-fx-background-color: -bg-tertiary; -fx-text-fill: -text-primary; -fx-font-size: 10px; -fx-background-radius: 4; -fx-padding: 3 10; -fx-cursor: hand;");
        clearChatBtn.setMaxWidth(Double.MAX_VALUE);
        clearChatBtn.setOnAction(e -> clearChat());

        Button saveSessionBtn = new Button("Save Session");
        saveSessionBtn.setStyle("-fx-background-color: -bg-tertiary; -fx-text-fill: -text-primary; -fx-font-size: 10px; -fx-background-radius: 4; -fx-padding: 3 10; -fx-cursor: hand;");
        saveSessionBtn.setMaxWidth(Double.MAX_VALUE);
        saveSessionBtn.setOnAction(e -> saveSession());

        autoCommitBtn = new ToggleButton("Auto-Commit");
        autoCommitBtn.setStyle("-fx-background-color: -bg-tertiary; -fx-text-fill: -text-primary; -fx-font-size: 10px; -fx-background-radius: 4; -fx-padding: 3 10; -fx-cursor: hand;");
        autoCommitBtn.setTooltip(new Tooltip("Auto-commit AI changes to Git"));
        autoCommitBtn.setMaxWidth(Double.MAX_VALUE);
        sidePanel.getChildren().addAll(autoCommitBtn, clearChatBtn, saveSessionBtn);

        // Code Outline section
        Label outlineTitle = new Label("Outline");
        outlineTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: -text-primary; -fx-padding: 8 0 2 0;");
        codeOutline = new CodeOutlinePanel();
        codeOutline.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(codeOutline, Priority.ALWAYS);
        sidePanel.getChildren().addAll(outlineTitle, codeOutline);

        sideScroll = new ScrollPane(sidePanel);
        sideScroll.setFitToWidth(true);
        sideScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        sideScroll.setPrefWidth(220);
        sideScroll.setMinWidth(180);

        // Chat area
        chatScrollPane = new ScrollPane(messagesContainer);
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        // Auto-scroll to bottom when content changes
        messagesContainer.heightProperty().addListener((obs, old, nv) -> chatScrollPane.setVvalue(1.0));

        splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.getItems().addAll(sideScroll, chatScrollPane);
        splitPane.setDividerPositions(0.22);
        setCenter(splitPane);

        statusLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-muted; -fx-padding: 2 12;");

        HBox actions = new HBox(6);
        actions.setPadding(new Insets(4, 10, 4, 10));
        fixBtn.setStyle("-fx-background-color: -accent; -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 6; -fx-padding: 4 12; -fx-cursor: hand;");
        explainBtn.setStyle("-fx-background-color: -bg-tertiary; -fx-text-fill: -text-primary; -fx-font-size: 11px; -fx-background-radius: 6; -fx-padding: 4 12; -fx-cursor: hand;");
        fixBtn.setOnAction(e -> quickAction("Fix this code:\n\n{CODE}\n\nIdentify bugs and provide the corrected version."));
        explainBtn.setOnAction(e -> quickAction("Explain this code:\n\n{CODE}\n\nExplain what it does in detail."));
        actions.getChildren().addAll(fixBtn, explainBtn);

        HBox inputBar = new HBox(6);
        inputBar.setPadding(new Insets(6, 10, 10, 10));
        inputField.setPromptText("Ask AI to write, fix, or explain code... (!npm, !pip, !git)");
        HBox.setHgrow(inputField, Priority.ALWAYS);
        sendBtn.setStyle("-fx-background-color: -accent; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 18; -fx-cursor: hand;");
        sendBtn.setOnAction(e -> sendMessage());
        inputField.setOnAction(e -> sendMessage());
        stopBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 18; -fx-cursor: hand;");
        stopBtn.setVisible(false);
        stopBtn.setManaged(false);
        stopBtn.setOnAction(e -> stopGeneration());
        inputBar.getChildren().addAll(inputField, sendBtn, stopBtn);

        VBox bottom = new VBox(statusLabel, actions, inputBar);
        setBottom(bottom);

        if (providerType.equals("gemini") && (geminiKey == null || geminiKey.trim().isEmpty())) {
            sendBtn.setDisable(true);
            statusLabel.setText("Set your API key in Settings (gear icon).");
        }
    }

    private void stopGeneration() {
        
        
        stopRequested = true;
        
        if (activeProcess != null && activeProcess.isAlive()) {
            activeProcess.destroyForcibly();
        }
        
        if (activeConn != null) {
            try { activeConn.disconnect(); } catch (Exception ignored) {}
        }
        if (workerThread != null) {
            workerThread.interrupt();
        }
    }

    private void quickAction(String template) {
        if (tabManager == null) { statusLabel.setText("No editor open"); return; }
        CodeEditor editor = tabManager.getActiveEditor();
        if (editor == null) { statusLabel.setText("No file open"); return; }
        String code = editor.getSelectedText();
        String fullCode = editor.getText();
        if (code == null || code.isEmpty()) code = fullCode;
        inputField.setText(template.replace("{CODE}", code));
        sendMessage();
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        inputField.clear();

        // Intercept local commands
        if (text.startsWith("!npm ") || text.startsWith("!pip ") || text.startsWith("!git ")) {
            executeTerminalCommand(text);
            return;
        }
        if (text.startsWith("!web ")) {
            webSearch(text.substring(5).trim());
            return;
        }

        String context = "";
        if (tabManager != null) {
            File f = tabManager.getActiveFile();
            CodeEditor ed = tabManager.getActiveEditor();
            if (f != null && ed != null) {
                String lang = f.getName().replaceAll(".*\\.", "");
                context = "File: " + f.getName() + " (" + lang + ")\n```" + lang + "\n" + ed.getText() + "\n```\n";
            }
        }

        // Inject project context in Project Mode
        String projectCtx = "";
        if (projectMode) {
            projectCtx = buildProjectContext();
        }

        addMessage("You", text, false);
        addMessage("AI", "...", false);
        statusLabel.setText("Thinking...");

        String chatPrompt = context.isEmpty() ? text : context + "\nUser question/request:\n" + text;

        // Build full prompt with project context and file-operation instructions
        String fullPrompt = chatPrompt;
        if (!projectCtx.isEmpty()) {
            String fileOpsInstr = "\n\nYou have FULL access to the project. You can read, create, modify, delete, rename any file."
                + "\nTo CREATE or MODIFY a file, use a fenced code block with the filename as tag:"
                + "\n```relative/path/to/file.ext\n// full file content\n```"
                + "\nTo DELETE a file, use: !delete relative/path/to/file.ext"
                + "\nTo RENAME a file, use: !rename old/path new/path"
                + "\nTo CREATE a folder, use: !mkdir relative/path/to/folder"
                + "\nApply changes immediately as you write them."
                + "\nAlways show the complete file content in code blocks."
                + "\nBe thorough, write complete working code, and explain your changes.\n\n"
                + "=== PROJECT CONTEXT ===\n" + projectCtx + "\n=== END PROJECT CONTEXT ===\n";
            fullPrompt = fileOpsInstr + "\n\nUser request:\n" + chatPrompt;
        }

        chatHistory.add(new ChatMessage("user", text));

        // Quality instruction for non-project mode
        if (projectCtx.isEmpty()) {
            fullPrompt = fullPrompt + "\n\nBe thorough and provide complete code examples.";
        } else {
            fullPrompt = fullPrompt + "\n\nBe very thorough, detailed, and write complete code.";
        }

        String fullPromptt = fullPrompt;

        stopRequested = false;
        sendBtn.setVisible(false);
        sendBtn.setManaged(false);
        stopBtn.setVisible(true);
        stopBtn.setManaged(true);
        startThinkingAnimation();
        // Reset streaming state
        executedCommands.clear();
        autoCreatedFiles.clear();
        streamingActive = true;

        workerThread = new Thread(() -> {
            try {
                switch (providerType) {
                    case "ollama":
                        callOllamaStreaming(chatHistory, fullPromptt);
                        return;
                    case "localgguf":
                        callLocalGgufStreaming(chatHistory, fullPromptt);
                        return;
                }
                String response;
                switch (providerType) {
                    case "openai":
                        response = callOpenAI(chatHistory, fullPromptt);
                        break;
                    default:
                        response = callGemini(chatHistory, fullPromptt);
                }
                chatHistory.add(new ChatMessage("assistant", response));
                String resp = response;
                Platform.runLater(() -> {
                    removeLastAIMessage();
                    addMessage("AI", resp, true);
                    if (projectMode) {
                        applyFileChanges(resp);
                    }
                    finishSend();
                });
            } catch (Exception ex) {
                if (stopRequested) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Stopped.");
                        finishSend();
                    });
                    return;
                }
                Platform.runLater(() -> {
                    removeLastAIMessage();
                    String msg = ex.getMessage();
                    if (msg != null && msg.contains("403"))
                        msg = "API key is invalid or expired.\nCheck your key in Settings (gear icon).";
                    else if (msg != null && msg.contains("Connection refused"))
                        msg = "Cannot reach the server.\nCheck the endpoint URL in Settings.";
                    else if (msg != null && msg.contains("401"))
                        msg = "API key is missing or invalid.\nAdd your key in Settings.";
                    else if (msg != null && msg.contains("404"))
                        msg = "Endpoint not found.\nCheck the URL and model name in Settings.";
                    addMessage("AI", "Error: " + msg, false);
                    finishSend();
                });
            }
        });
        workerThread.setDaemon(true);
        workerThread.start();
    }
    
    
    private void callLocalGgufStreaming(List<ChatMessage> history, String currentPrompt) throws Exception {
        if (localGgufPath.isEmpty() || !new File(localGgufPath).exists()) {
            throw new IOException("GGUF model path is empty or file not found!");
        }

        streamingActive = true;
        autoCreatedFiles.clear();
        stopRequested = false;
        StringBuilder fullResponse = new StringBuilder();

        // Build chat-formatted prompt
        StringBuilder promptText = new StringBuilder();
        for (ChatMessage msg : history) {
            String role = msg.role.equals("user") ? "User" : "Assistant";
            promptText.append(role).append(": ").append(msg.content).append("\n");
        }
        promptText.append("User: ").append(currentPrompt).append("\nAssistant: ");

        // Write prompt to a temp file for reliable handling of special chars
        File tempPrompt = File.createTempFile("gguf_prompt_", ".txt");
        try (OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(tempPrompt), StandardCharsets.UTF_8)) {
            fw.write(promptText.toString());
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(llamaCliPath);
        cmd.add("--model"); cmd.add(localGgufPath);
        cmd.add("--ctx-size"); cmd.add("4096");
        cmd.add("--temp"); cmd.add("0.7");
        cmd.add("--predict"); cmd.add("-1");
        cmd.add("--file"); cmd.add(tempPrompt.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Platform.runLater(() -> statusLabel.setText("Running GGUF..."));
        activeProcess = pb.start();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(activeProcess.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            long lastFlush = System.currentTimeMillis();

            while ((line = br.readLine()) != null && !stopRequested) {
                fullResponse.append(line).append("\n");
                long now = System.currentTimeMillis();
                if (now - lastFlush > 25) {
                    lastFlush = now;
                    String snap = fullResponse.toString();
                    Platform.runLater(() -> {
                        if (streamingBubble == null) {
                            addMessage("AI", snap, false);
                            HBox w = (HBox) messagesContainer.getChildren().get(messagesContainer.getChildren().size() - 1);
                            streamingBubble = (VBox) w.getChildren().get(0);
                        } else {
                            updateStreamingContent(snap);
                        }
                    });
                }
            }
        }

        int exitCode = activeProcess.waitFor();
        activeProcess = null;

        tempPrompt.delete();

        // If no output and exit code != 0, show the error
        String rawText = fullResponse.toString().trim();
        if (rawText.isEmpty() && exitCode != 0) {
            String cmdStr = llamaCliPath + " --model " + localGgufPath + " --ctx-size 4096 --temp 0.7 --predict -1 --file ...";
            rawText = "GGUF process exited with code " + exitCode + "\nCommand: " + cmdStr + "\nCheck the CLI path and model in Settings.";
        }
        streamingActive = false;
        final String finalText = rawText;
        if (!finalText.isEmpty()) {
            chatHistory.add(new ChatMessage("assistant", finalText));
            Platform.runLater(() -> {
                if (streamingBubble != null) {
                    Node header = streamingBubble.getChildren().get(0);
                    streamingBubble.getChildren().clear();
                    streamingBubble.getChildren().add(header);
                    String[] ref = new String[1];
                    renderContent(streamingBubble, "AI", finalText, true, ref);
                    streamingBubble = null;
                } else {
                    addMessage("AI", finalText, true);
                }
                if (projectMode) applyFileChanges(finalText);
                finishSend();
            });
        } else {
            Platform.runLater(() -> finishSend());
        }
    }
    

    private void startThinkingAnimation() {
        stopThinkingAnimation();
        thinkingDots = 0;
        thinkingAnim = new Timeline(new KeyFrame(Duration.millis(500), e -> {
            thinkingDots = (thinkingDots + 1) % 4;
            StringBuilder sb = new StringBuilder("Thinking");
            for (int i = 0; i < thinkingDots; i++) sb.append(".");
            for (int i = thinkingDots; i < 3; i++) sb.append(" ");
            statusLabel.setText(sb.toString());
        }));
        thinkingAnim.setCycleCount(Timeline.INDEFINITE);
        thinkingAnim.play();
    }

    private void stopThinkingAnimation() {
        if (thinkingAnim != null) {
            thinkingAnim.stop();
            thinkingAnim = null;
        }
    }

    private void finishSend() {
        stopThinkingAnimation();
        sendBtn.setVisible(true);
        sendBtn.setManaged(true);
        stopBtn.setVisible(false);
        stopBtn.setManaged(false);
        stopRequested = false;
        streamingActive = false;
        activeConn = null;
    }

    private String callGemini(List<ChatMessage> history, String currentPrompt) throws IOException {
        JsonObject body = new JsonObject();
        JsonArray contents = new JsonArray();
        // Truncate history: last 4 messages
        List<ChatMessage> sendHistory = history;
        if (history.size() > 4) {
            sendHistory = history.subList(history.size() - 4, history.size());
        }
        for (ChatMessage msg : sendHistory) {
            JsonObject part = new JsonObject();
            part.addProperty("text", msg.content);
            JsonArray parts = new JsonArray();
            parts.add(part);
            JsonObject content = new JsonObject();
            content.add("parts", parts);
            content.addProperty("role", msg.role.equals("assistant") ? "model" : "user");
            contents.add(content);
        }
        // Add current prompt with context
        JsonObject part = new JsonObject();
        part.addProperty("text", currentPrompt);
        JsonArray parts = new JsonArray();
        parts.add(part);
        JsonObject content = new JsonObject();
        content.add("parts", parts);
        content.addProperty("role", "user");
        contents.add(content);
        body.add("contents", contents);
        JsonObject safety = new JsonObject();
        safety.addProperty("category", "HARM_CATEGORY_UNSPECIFIED");
        safety.addProperty("threshold", "BLOCK_NONE");
        JsonArray safetySettings = new JsonArray();
        safetySettings.add(safety);
        body.add("safetySettings", safetySettings);

        String resp = doRequest(GEMINI_ENDPOINT + geminiModel + ":generateContent?key=" + geminiKey, body);
        JsonObject json = gson.fromJson(resp, JsonObject.class);
        if (json.has("candidates")) {
            JsonArray candidates = json.getAsJsonArray("candidates");
            if (candidates.size() > 0) {
                JsonObject candidate = candidates.get(0).getAsJsonObject();
                if (candidate.has("content")) {
                    JsonArray partss = candidate.getAsJsonObject("content").getAsJsonArray("parts");
                    if (partss.size() > 0)
                        return partss.get(0).getAsJsonObject().get("text").getAsString();
                }
                if (candidate.has("finishReason")) {
                    String reason = candidate.get("finishReason").getAsString();
                    if (!reason.equals("STOP"))
                        throw new IOException("Blocked: " + reason);
                }
            }
        }
        if (json.has("error")) {
            JsonObject err = json.getAsJsonObject("error");
            throw new IOException(err.has("message") ? err.get("message").getAsString() : json.toString());
        }
        throw new IOException("Unexpected Gemini response format");
    }

    private String callOpenAI(List<ChatMessage> history, String currentPrompt) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("model", openaiModel);
        body.addProperty("stream", false);
        JsonArray msgs = new JsonArray();
        // Truncate history: last 4 messages
        List<ChatMessage> sendHistory = history;
        if (history.size() > 4) {
            sendHistory = history.subList(history.size() - 4, history.size());
        }
        for (ChatMessage msg : sendHistory) {
            JsonObject m = new JsonObject();
            m.addProperty("role", msg.role.equals("assistant") ? "assistant" : "user");
            m.addProperty("content", msg.content);
            msgs.add(m);
        }
        // Add current prompt with context
        JsonObject m = new JsonObject();
        m.addProperty("role", "user");
        m.addProperty("content", currentPrompt);
        msgs.add(m);
        body.add("messages", msgs);

        String resp = doRequest(openaiEndpoint, body, openaiKey);
        JsonObject json = gson.fromJson(resp, JsonObject.class);
        if (json.has("choices")) {
            JsonArray choices = json.getAsJsonArray("choices");
            if (choices.size() > 0) {
                JsonObject choice = choices.get(0).getAsJsonObject();
                if (choice.has("message"))
                    return choice.getAsJsonObject("message").get("content").getAsString();
            }
        }
        if (json.has("error")) {
            JsonObject err = json.getAsJsonObject("error");
            throw new IOException(err.has("message") ? err.get("message").getAsString() : json.toString());
        }
        throw new IOException("Unexpected API response format");
    }

    private void callOllamaStreaming(List<ChatMessage> history, String currentPrompt) throws IOException {
        // Truncate history: last 4 messages max (2 exchanges) for speed
        // Plus current prompt with context
        List<ChatMessage> sendHistory = new ArrayList<>();
        if (history.size() > 4) {
            sendHistory.addAll(history.subList(history.size() - 4, history.size()));
        } else {
            sendHistory.addAll(history);
        }

        JsonObject body = new JsonObject();
        body.addProperty("model", ollamaModel);
        body.addProperty("stream", true);
        body.addProperty("keep_alive", "5m");
        JsonObject options = new JsonObject();
        options.addProperty("num_predict", 8192);
        body.add("options", options);
        JsonArray msgs = new JsonArray();
        for (ChatMessage msg : sendHistory) {
            JsonObject m = new JsonObject();
            m.addProperty("role", msg.role);
            m.addProperty("content", msg.content);
            msgs.add(m);
        }
        // Add the current prompt (with context) as the user message
        JsonObject m = new JsonObject();
        m.addProperty("role", "user");
        m.addProperty("content", currentPrompt);
        msgs.add(m);
        body.add("messages", msgs);

        String urlStr = ollamaEndpoint;
        byte[] postData = gson.toJson(body).getBytes(StandardCharsets.UTF_8);
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        activeConn = conn;
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(120000);
        try (OutputStream os = conn.getOutputStream()) { os.write(postData); }

        int code = conn.getResponseCode();
        if (code != 200) {
            try (InputStream es = conn.getErrorStream()) {
                String err = es == null ? "" : new String(readAllBytes(es), StandardCharsets.UTF_8);
                throw new IOException("API returned " + code + ": " + err);
            }
        }

        streamingActive = true;
        autoCreatedFiles.clear();
        StringBuilder full = new StringBuilder();
        final long[] lastFlush = {System.currentTimeMillis()};
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null && !stopRequested) {
                if (line.isEmpty()) continue;
                JsonObject json = gson.fromJson(line, JsonObject.class);
                if (json == null) continue;
                String content = null;
                if (json.has("message")) {
                    content = json.getAsJsonObject("message").get("content").getAsString();
                } else if (json.has("response")) {
                    content = json.get("response").getAsString();
                }
                if (content != null && !content.isEmpty()) {
                    full.append(content);
                    long now = System.currentTimeMillis();
                    if (now - lastFlush[0] > 25) {
                        lastFlush[0] = now;
                        final String snapshot = full.toString();
                        Platform.runLater(() -> {
                            if (streamingBubble == null) {
                                addMessage("AI", snapshot, false);
                                HBox w = (HBox) messagesContainer.getChildren().get(messagesContainer.getChildren().size() - 1);
                                streamingBubble = (VBox) w.getChildren().get(0);
                            } else {
                                updateStreamingContent(snapshot);
                            }
                        });
                    }
                }
                if (json.has("done") && json.get("done").getAsBoolean()) {
                    break;
                }
            }
        } catch (IOException e) {
            if (stopRequested) {
                streamingActive = false;
                final String partial = full.toString();
                if (!partial.isEmpty()) {
                    history.add(new ChatMessage("assistant", partial));
                    Platform.runLater(() -> {
                        if (streamingBubble != null) {
                            Node header = streamingBubble.getChildren().get(0);
                            streamingBubble.getChildren().clear();
                            streamingBubble.getChildren().add(header);
                            String[] ref = new String[1];
                            renderContent(streamingBubble, "AI", partial, false, ref);
                            streamingBubble = null;
                        } else {
                            addMessage("AI", partial, false);
                        }
                        statusLabel.setText("Stopped");
                        finishSend();
                    });
                } else {
                    Platform.runLater(() -> { statusLabel.setText("Stopped"); finishSend(); });
                }
                return;
            }
            if (e.getMessage() != null && e.getMessage().contains("404") && urlStr.contains("/api/chat")) {
                streamingActive = false;
                callOllamaGenerateStreaming(sendHistory, currentPrompt);
                return;
            }
            throw e;
        } finally {
            activeConn = null;
        }

        // Flush remaining buffer
        streamingActive = false;
        final String finalFull = full.toString();
        if (!finalFull.isEmpty()) {
            history.add(new ChatMessage("assistant", finalFull));
            Platform.runLater(() -> {
                if (streamingBubble != null) {
                    Node header = streamingBubble.getChildren().get(0);
                    streamingBubble.getChildren().clear();
                    streamingBubble.getChildren().add(header);
                    String[] ref = new String[1];
                    renderContent(streamingBubble, "AI", finalFull, true, ref);
                    streamingBubble = null;
                } else {
                    addMessage("AI", finalFull, true);
                }
                if (projectMode) applyFileChanges(finalFull);
                statusLabel.setText("Ready");
                finishSend();
            });
        } else {
            Platform.runLater(() -> { statusLabel.setText("Ready"); finishSend(); });
        }
    }

    private void callOllamaGenerateStreaming(List<ChatMessage> history, String currentPrompt) throws IOException {
        // Truncate history: last 4 messages max
        List<ChatMessage> sendHistory = new ArrayList<>();
        if (history.size() > 4) {
            sendHistory.addAll(history.subList(history.size() - 4, history.size()));
        } else {
            sendHistory.addAll(history);
        }

        JsonObject body = new JsonObject();
        body.addProperty("model", ollamaModel);
        body.addProperty("stream", true);
        body.addProperty("keep_alive", "5m");
        JsonObject options = new JsonObject();
        options.addProperty("num_predict", 8192);
        body.add("options", options);
        StringBuilder prompt = new StringBuilder();
        for (ChatMessage msg : sendHistory) {
            prompt.append(msg.role.equals("user") ? "User: " : "Assistant: ");
            prompt.append(msg.content).append("\n");
        }
        prompt.append("User: ").append(currentPrompt).append("\n");
        prompt.append("Assistant: ");
        body.addProperty("prompt", prompt.toString());

        String urlStr = ollamaEndpoint.replace("/api/chat", "/api/generate");
        byte[] postData = gson.toJson(body).getBytes(StandardCharsets.UTF_8);
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        activeConn = conn;
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(120000);
        try (OutputStream os = conn.getOutputStream()) { os.write(postData); }

        int code = conn.getResponseCode();
        if (code != 200) {
            try (InputStream es = conn.getErrorStream()) {
                String err = es == null ? "" : new String(readAllBytes(es), StandardCharsets.UTF_8);
                throw new IOException("API returned " + code + ": " + err);
            }
        }

        streamingActive = true;
        autoCreatedFiles.clear();
        StringBuilder full = new StringBuilder();
        final long[] lastFlush = {System.currentTimeMillis()};
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null && !stopRequested) {
                if (line.isEmpty()) continue;
                JsonObject json = gson.fromJson(line, JsonObject.class);
                if (json == null) continue;
                if (json.has("response")) {
                    String content = json.get("response").getAsString();
                    if (content != null && !content.isEmpty()) {
                        full.append(content);
                        long now = System.currentTimeMillis();
                        if (now - lastFlush[0] > 25) {
                            lastFlush[0] = now;
                            final String snapshot = full.toString();
                            Platform.runLater(() -> {
                                if (streamingBubble == null) {
                                    addMessage("AI", snapshot, false);
                                    HBox w = (HBox) messagesContainer.getChildren().get(messagesContainer.getChildren().size() - 1);
                                    streamingBubble = (VBox) w.getChildren().get(0);
                                } else {
                                    updateStreamingContent(snapshot);
                                }
                            });
                        }
                    }
                }
                if (json.has("done") && json.get("done").getAsBoolean()) {
                    break;
                }
            }
        } finally {
            activeConn = null;
        }

        streamingActive = false;
        final String finalFull = full.toString();
        if (!finalFull.isEmpty()) {
            history.add(new ChatMessage("assistant", finalFull));
            Platform.runLater(() -> {
                if (streamingBubble != null) {
                    Node header = streamingBubble.getChildren().get(0);
                    streamingBubble.getChildren().clear();
                    streamingBubble.getChildren().add(header);
                    String[] ref = new String[1];
                    renderContent(streamingBubble, "AI", finalFull, true, ref);
                    streamingBubble = null;
                } else {
                    addMessage("AI", finalFull, true);
                }
                if (projectMode) applyFileChanges(finalFull);
                statusLabel.setText(stopRequested ? "Stopped" : "Ready");
                finishSend();
            });
        } else {
            Platform.runLater(() -> { statusLabel.setText(stopRequested ? "Stopped" : "Ready"); finishSend(); });
        }
    }

    private String doRequest(String urlStr, JsonObject body) throws IOException {
        return doRequest(urlStr, body, null);
    }

    private String doRequest(String urlStr, JsonObject body, String bearerToken) throws IOException {
        byte[] postData = gson.toJson(body).getBytes(StandardCharsets.UTF_8);
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        if (bearerToken != null && !bearerToken.isEmpty())
            conn.setRequestProperty("Authorization", "Bearer " + bearerToken);
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(120000);
        try (OutputStream os = conn.getOutputStream()) { os.write(postData); }

        int code = conn.getResponseCode();
        if (code != 200) {
            try (InputStream es = conn.getErrorStream()) {
                String err = es == null ? "" : new String(readAllBytes(es), StandardCharsets.UTF_8);
                throw new IOException("API returned " + code + ": " + err);
            }
        }
        try (InputStream is = conn.getInputStream()) {
            return new String(readAllBytes(is), StandardCharsets.UTF_8);
        }
    }

    private byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] tmp = new byte[8192];
        int n;
        while ((n = in.read(tmp)) != -1) buf.write(tmp, 0, n);
        return buf.toByteArray();
    }

    // ── Project Mode: build context from project files ──
    private String buildProjectContext() {
        if (projectRoot == null || !projectRoot.isDirectory()) {
            projectModeBtn.setSelected(false);
            projectMode = false;
            Platform.runLater(() -> statusLabel.setText("Project Mode: no project open"));
            return "";
        }
        long now = System.currentTimeMillis();
        if (now - projectContextLastScan < PROJECT_CONTEXT_TTL && !projectContextCache.isEmpty()) {
            return projectContextCache;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Project root: ").append(projectRoot.getAbsolutePath()).append("\n");
        sb.append("File tree:\n");
        List<File> sources = new ArrayList<>();
        collectSourceFiles(projectRoot, sources, 0, sb);
        sb.append("\nFile contents:\n");
        int filesIncluded = 0;
        for (File file : sources) {
            if (filesIncluded >= 8) {
                sb.append("... and ").append(sources.size() - filesIncluded).append(" more files (truncated)\n");
                break;
            }
            long len = file.length();
            if (len > 10000) {
                sb.append("--- ").append(relativePath(file, projectRoot)).append(" (").append(len).append(" bytes, skipped) ---\n");
                continue;
            }
            sb.append("--- ").append(relativePath(file, projectRoot)).append(" ---\n");
            String lang = file.getName().replaceAll(".*\\.", "");
            sb.append("```").append(lang).append("\n");
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                String line;
                int lineCount = 0;
                while ((line = br.readLine()) != null) {
                    if (lineCount++ >= 80) {
                        sb.append("... (file truncated at 80 lines)\n");
                        break;
                    }
                    sb.append(line).append("\n");
                }
            } catch (IOException ignored) {}
            sb.append("```\n\n");
            filesIncluded++;
            // Stop if total context exceeds ~5000 chars to avoid token limit errors
            if (sb.length() > 5000) {
                sb.append("... (context truncated, ").append(sources.size() - filesIncluded).append(" more files omitted)\n");
                break;
            }
        }
        projectContextCache = sb.toString();
        projectContextLastScan = now;
        return projectContextCache;
    }

    private void collectSourceFiles(File dir, List<File> result, int depth, StringBuilder tree) {
        File[] files = dir.listFiles();
        if (files == null) return;
        // indent
        for (int i = 0; i < depth; i++) tree.append("  ");
        tree.append("|-- ").append(dir.getName()).append("/\n");
        for (File f : files) {
            if (f.isDirectory()) {
                String n = f.getName();
                if (n.startsWith(".") || n.equals("node_modules") || n.equals("__pycache__")
                    || n.equals("target") || n.equals("build") || n.equals("dist")
                    || n.equals(".git") || n.equals(".svn")) continue;
                collectSourceFiles(f, result, depth + 1, tree);
            } else {
                String fn = f.getName().toLowerCase();
                if (fn.endsWith(".java") || fn.endsWith(".kt") || fn.endsWith(".swift")
                    || fn.endsWith(".py") || fn.endsWith(".js") || fn.endsWith(".ts")
                    || fn.endsWith(".jsx") || fn.endsWith(".tsx") || fn.endsWith(".html")
                    || fn.endsWith(".css") || fn.endsWith(".scss") || fn.endsWith(".less")
                    || fn.endsWith(".xml") || fn.endsWith(".json") || fn.endsWith(".yaml")
                    || fn.endsWith(".yml") || fn.endsWith(".toml") || fn.endsWith(".ini")
                    || fn.endsWith(".cfg") || fn.endsWith(".conf") || fn.endsWith(".properties")
                    || fn.endsWith(".md") || fn.endsWith(".txt") || fn.endsWith(".gradle")
                    || fn.endsWith(".mvn") || fn.endsWith(".c") || fn.endsWith(".cpp")
                    || fn.endsWith(".h") || fn.endsWith(".hpp") || fn.endsWith(".rs")
                    || fn.endsWith(".go") || fn.endsWith(".rb") || fn.endsWith(".php")
                    || fn.endsWith(".sql") || fn.endsWith(".sh") || fn.endsWith(".bat")
                    || fn.endsWith(".ps1") || fn.endsWith(".dart") || fn.endsWith(".vue")
                    || fn.endsWith(".svelte") || fn.endsWith(".astro")) {
                    result.add(f);
                }
            }
        }
    }

    private String relativePath(File file, File base) {
        try {
            return base.toURI().relativize(file.toURI()).getPath();
        } catch (Exception e) {
            return file.getName();
        }
    }

    // ── Project Mode: apply file changes from AI response ──
    private void applyFileChanges(String responseText) {
        if (projectRoot == null || !projectRoot.isDirectory()) return;
        if (fileManager == null) {
            fileManager = new AiFileManager(projectRoot);
            fileManager.setTabManager(tabManager);
        }
        Pattern fencedBlock = Pattern.compile("```([^\\n]*?)\\n([\\s\\S]*?)```", Pattern.MULTILINE);
        Matcher m = fencedBlock.matcher(responseText);
        int created = 0, updated = 0, skipped = 0;
        int streamingCreated = 0, streamingUpdated = 0;
        StringBuilder report = new StringBuilder("## File Changes Report\n\n");
        StringBuilder details = new StringBuilder();
        while (m.find()) {
            String header = m.group(1).trim();
            String content = m.group(2);
            String filename = parseFilename(header, content);
            if (filename == null) continue;
            File target = new File(projectRoot, filename);
            boolean exists = target.exists();
            // If already auto-created during streaming, don't rewrite, just report
            if (!autoCreatedFiles.add(filename)) {
                details.append("⚡ ").append(exists ? "Updated" : "Created").append(": `").append(filename).append("` (streaming)\n");
                if (exists) streamingUpdated++; else streamingCreated++;
                continue;
            }
            // New file — write via fileManager
            AiFileManager.FileOp op = fileManager.applyFile(target, content);
            if (op != null) {
                details.append("✅ ").append("create".equals(op.type) ? "Created" : "Updated").append(": `").append(filename).append("`\n");
                if ("create".equals(op.type)) created++;
                else updated++;
                fileManager.scanForProblems(target);
            } else {
                skipped++;
                details.append("❌ Failed: `").append(filename).append("`\n");
            }
        }
        // Check for deletes/renames from recently added operations
        for (int i = fileManager.getHistory().size() - 1; i >= Math.max(0, fileManager.getHistory().size() - 20); i--) {
            AiFileManager.FileOp op = fileManager.getHistory().get(i);
            if ("delete".equals(op.type)) {
                details.append("🗑️ Deleted: `").append(op.file.getName()).append("`\n");
            } else if ("rename".equals(op.type)) {
                details.append("✏️ Renamed: `").append(op.file.getName()).append("` → `").append(op.newContent).append("`\n");
            }
        }
        int total = created + updated + streamingCreated + streamingUpdated;
        if (total > 0 || skipped > 0) {
            report.append(details);
            report.append("\n### Summary\n");
            if (created > 0) report.append("- ").append(created).append(" files created\n");
            if (updated > 0) report.append("- ").append(updated).append(" files updated\n");
            if (streamingCreated > 0) report.append("- ").append(streamingCreated).append(" files created (during streaming)\n");
            if (streamingUpdated > 0) report.append("- ").append(streamingUpdated).append(" files updated (during streaming)\n");
            if (skipped > 0) report.append("- ").append(skipped).append(" files skipped\n");
            report.append("- **Total**: ").append(total).append(" file operation(s)");
            final String reportStr = report.toString();
            Platform.runLater(() -> {
                refreshSidePanel();
                addMessage("System", reportStr, false);
                statusLabel.setText(total + " file(s) affected");
                if (onFileCreated != null) onFileCreated.run();
            });
            // Auto-save session after changes
            saveSession();
            // Git auto-commit
            if (autoCommitBtn != null && autoCommitBtn.isSelected()) {
                gitAutoCommit("AI: " + total + " file(s) changed");
            }
        }
    }

    private String parseFilename(String header, String content) {
        // Handle "language:path/to/file.ext" format
        if (header.contains(":")) {
            String after = header.substring(header.indexOf(':') + 1).trim();
            if (after.contains(".") && !after.contains(" ") && !after.startsWith("http") && !isLanguageTag(after))
                return after.replace("\\", "/");
        } else if (header.contains(" ")) {
            header = header.substring(0, header.indexOf(' ')).trim();
        }
        // Handle "path/to/file.ext" or "file.ext"
        if (header.contains(".") && !header.startsWith(".")) {
            if (!header.contains(" ") && !isLanguageTag(header)) {
                return header.replace("\\", "/");
            }
            // Could be like "java MyClass.java" — take last part
            String[] parts = header.split("\\s+");
            for (int i = parts.length - 1; i >= 0; i--) {
                if (parts[i].contains(".") && !isLanguageTag(parts[i])) {
                    return parts[i].replace("\\", "/");
                }
            }
        }
        // Try to detect from content first line
        String firstLine = content.split("\n", 2)[0].trim();
        String clean = firstLine.replaceAll("^(//|#|/\\*|<!--|%%|```)\\s*", "").replaceAll("\\*/$", "").trim();
        // Match paths like src/com/example/Main.java or Main.java
        if (clean.matches("^[A-Za-z0-9_./\\\\-]+\\.[a-zA-Z0-9]+$") && !clean.contains(" ") && clean.length() < 200) {
            return clean.replace("\\", "/");
        }
        // Java class detection
        if (content.contains("class ") || content.contains("public class")) {
            Matcher cm = Pattern.compile("class\\s+([A-Za-z0-9_]+)").matcher(content);
            if (cm.find()) return cm.group(1) + ".java";
        }
        // Python class detection
        if (content.contains("class ") && !content.contains("public class")) {
            Matcher cm = Pattern.compile("class\\s+([A-Za-z0-9_]+)").matcher(content);
            if (cm.find()) return cm.group(1) + ".py";
        }
        return null;
    }

    private boolean isLanguageTag(String s) {
        s = s.trim().toLowerCase();
        // If it contains a path separator, it's a file path not a language tag
        if (s.contains("/") || s.contains("\\")) return false;
        // If it looks like a file name with extension, it's not a language tag
        if (s.contains(".") && s.indexOf('.') < s.length() - 1) return false;
        String[] tags = {"json","java","python","javascript","typescript","html","css","xml",
            "yaml","yml","md","text","sql","bash","sh","shell","php","ruby","go","rust",
            "c","cpp","dart","kotlin","swift","vue","svelte","astro","gradle","toml",
            "ini","cfg","conf","properties","bat","ps1","csv","tsv","scala","groovy",
            "makefile","dockerfile","gitignore","env","svg","txt","jsx","tsx","less",
            "scss","sass","handlebars","hbs","ejs","jade","pug","js","ts"};
        for (String t : tags) if (s.equals(t)) return true;
        return false;
    }

    private void refreshSidePanel() {
        if (sidePanel == null || fileManager == null) return;
        Node container = sidePanel.lookup("#file-tree-container");
        if (container instanceof Pane) {
            Pane pane = (Pane) container;
            pane.getChildren().clear();
            pane.getChildren().add(fileManager.getFileTreePanel());
        }
    }

    // Execute file commands during streaming: !delete, !rename, !mkdir, !npm, !pip, !git
    private void executeFileCommands(String content) {
        if (projectRoot == null) return;
        if (fileManager == null) {
            fileManager = new AiFileManager(projectRoot);
            fileManager.setTabManager(tabManager);
        }
        // !delete path/to/file
        Pattern del = Pattern.compile("!delete\\s+(\\S+)", Pattern.MULTILINE);
        Matcher dm = del.matcher(content);
        while (dm.find()) {
            File target = new File(projectRoot, dm.group(1));
            fileManager.deleteFile(target);
            Platform.runLater(() -> statusLabel.setText("Deleted: " + dm.group(1)));
        }
        // !rename old new
        Pattern ren = Pattern.compile("!rename\\s+(\\S+)\\s+(\\S+)", Pattern.MULTILINE);
        Matcher rm = ren.matcher(content);
        while (rm.find()) {
            File src = new File(projectRoot, rm.group(1));
            File dst = new File(projectRoot, rm.group(2));
            fileManager.renameFile(src, dst);
            Platform.runLater(() -> statusLabel.setText("Renamed: " + rm.group(1) + " -> " + rm.group(2)));
        }
        // !mkdir path/to/folder
        Pattern mk = Pattern.compile("!mkdir\\s+(\\S+)", Pattern.MULTILINE);
        Matcher mm = mk.matcher(content);
        while (mm.find()) {
            File dir = new File(projectRoot, mm.group(1));
            if (dir.mkdirs()) {
                Platform.runLater(() -> statusLabel.setText("Created folder: " + mm.group(1)));
            }
        }
        // !npm, !pip, !git commands (deduplicated)
        Pattern terminalPkg = Pattern.compile("!(npm|pip|git)\\s+(.+)", Pattern.MULTILINE);
        Matcher tm = terminalPkg.matcher(content);
        while (tm.find()) {
            String fullCmd = tm.group(1) + " " + tm.group(2).trim();
            if (!executedCommands.add(fullCmd)) continue; // skip duplicates
            final String capturedCmd = fullCmd;
            Platform.runLater(() -> {
                addMessage("System", "Running: " + capturedCmd, false);
                statusLabel.setText("Running: " + capturedCmd);
            });
            executeShellCommand(capturedCmd);
        }
        // Apply completed code blocks to files during streaming (via fileManager)
        if (projectMode && projectRoot != null) {
            Pattern blocks = Pattern.compile("```([^\\n]*?)\\n([\\s\\S]*?)```", Pattern.MULTILINE);
            Matcher bm = blocks.matcher(content);
            while (bm.find()) {
                String header = bm.group(1).trim();
                String fileContent = bm.group(2);
                String filename = parseFilename(header, fileContent);
                if (filename == null || !autoCreatedFiles.add(filename)) continue;
                // Skip plain language tags
                if (isLanguageTag(filename)) {
                    autoCreatedFiles.remove(filename);
                    continue;
                }
                final String fname = filename;
                File target = new File(projectRoot, filename);
                AiFileManager.FileOp op = fileManager.applyFile(target, fileContent);
                if (op != null) {
                    fileManager.scanForProblems(target);
                    Platform.runLater(() -> statusLabel.setText("Written: " + fname));
                }
            }
        }
    }

    private void backupFile(File f) {
        if (!f.exists()) return;
        try {
            File backupDir = new File(projectRoot, ".webide_backups");
            backupDir.mkdirs();
            File bak = new File(backupDir, f.getName() + "." + System.currentTimeMillis() + ".bak");
            copyFile(f, bak);
        } catch (IOException ignored) {}
    }

    private void copyFile(File src, File dst) throws IOException {
        try (InputStream in = new BufferedInputStream(new FileInputStream(src));
             OutputStream out = new BufferedOutputStream(new FileOutputStream(dst))) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        }
    }

    private void addMessage(String role, String content, boolean allowApply) {
        VBox bubble = new VBox(4);
        bubble.setMaxWidth(480);
        bubble.setPadding(new Insets(8, 10, 8, 10));
        if (role.equals("AI")) {
            bubble.setStyle("-fx-background-color: -bg-tertiary; -fx-background-radius: 10;");
        } else {
            bubble.setStyle("-fx-background-color: -accent-soft; -fx-background-radius: 10;");
            bubble.setAlignment(Pos.CENTER_RIGHT);
        }
        Label roleLabel = new Label(role);
        roleLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: -accent;");
        final String[] latestContent = {content};
        Button msgCopyBtn = new Button();
        msgCopyBtn.setGraphic(IconManager.imageView(IconManager.COPY, 10));
        msgCopyBtn.setStyle("-fx-background: transparent; -fx-text-fill: -text-muted; -fx-cursor: hand; -fx-padding: 0;");
        msgCopyBtn.setTooltip(new Tooltip("Copy message"));
        msgCopyBtn.setOnAction(ev -> {
            ClipboardContent cc = new ClipboardContent();
            cc.putString(latestContent[0]);
            Clipboard.getSystemClipboard().setContent(cc);
            statusLabel.setText("Copied");
        });
        HBox topRow = new HBox(4, roleLabel, msgCopyBtn);
        if (role.equals("You")) topRow.setAlignment(Pos.CENTER_RIGHT);
        bubble.getChildren().add(topRow);

        renderContent(bubble, role, content, allowApply, latestContent);

        HBox wrapper = new HBox(bubble);
        if (role.equals("You")) wrapper.setAlignment(Pos.CENTER_RIGHT);
        else wrapper.setAlignment(Pos.CENTER_LEFT);
        messagesContainer.getChildren().add(wrapper);
    }

    // Update streaming bubble in-place (no flicker)
    private void updateStreamingContent(String content) {
        if (streamingBubble == null) return;
        // Keep header row (role + copy), re-render everything below it
        Node header = streamingBubble.getChildren().get(0);
        streamingBubble.getChildren().clear();
        streamingBubble.getChildren().add(header);
        String[] dummy = new String[1];
        renderContent(streamingBubble, "AI", content, false, dummy);
        // Execute file operations during streaming
        if (projectMode) {
            executeFileCommands(content);
        }
    }

    private void renderContent(VBox bubble, String role, String content, boolean allowApply, String[] latestContentRef) {
        if (!role.equals("AI")) {
            TextFlow tf = new TextFlow(makeCopyableText(content, "-fx-fill: -text-primary;"));
            tf.setMaxWidth(460);
            bubble.getChildren().add(tf);
            return;
        }

        Pattern codePattern = Pattern.compile("```([^\\n]*)\\n([\\s\\S]*?)```");
        Matcher m = codePattern.matcher(content);
        int lastEnd = 0;
        List<String> codeBlocks = new ArrayList<>();
        List<File> filesToOpen = new ArrayList<>();

        while (m.find()) {
            if (m.start() > lastEnd) {
                TextFlow tf = new TextFlow(makeCopyableText(content.substring(lastEnd, m.start()), "-fx-fill: -text-primary;"));
                tf.setMaxWidth(460);
                bubble.getChildren().add(tf);
            }

            String lang = m.group(1).trim();
            String code = m.group(2).trim();
            codeBlocks.add(code);

            String detectedFile = detectFilename(lang, code);

            VBox codeBlock = new VBox(4);
            codeBlock.setStyle("-fx-background-color: -bg-primary; -fx-background-radius: 6; -fx-padding: 8; -fx-border-color: -border-color; -fx-border-radius: 6;");
            codeBlock.setMaxWidth(460);

            HBox codeHeader = new HBox(6);
            String headerLabel = detectedFile != null ? detectedFile : (lang.isEmpty() ? "code" : lang);
            Label langLabel = new Label(headerLabel);
            langLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: -text-muted; -fx-font-weight: bold;");
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);

            Text codeText = makeCopyableText(code, "-fx-font-family: 'Consolas'; -fx-font-size: 11px; -fx-fill: -text-primary;");

            if (allowApply) {
                Button applyBtn = new Button("Apply");
                applyBtn.setStyle("-fx-background-color: -accent; -fx-text-fill: white; -fx-font-size: 9px; -fx-background-radius: 4; -fx-padding: 2 8; -fx-cursor: hand;");
                final String codeCapture = code;
                applyBtn.setOnAction(ev -> applyCode(codeCapture));

                Button copyBtn = new Button("Copy");
                copyBtn.setStyle("-fx-background-color: -bg-tertiary; -fx-text-fill: -text-primary; -fx-font-size: 9px; -fx-background-radius: 4; -fx-padding: 2 8; -fx-cursor: hand;");
                copyBtn.setOnAction(ev -> {
                    ClipboardContent cc = new ClipboardContent();
                    cc.putString(codeCapture);
                    Clipboard.getSystemClipboard().setContent(cc);
                    statusLabel.setText("Copied to clipboard");
                });

                if (detectedFile != null && projectRoot != null && !streamingActive && autoCreatedFiles.add(detectedFile)) {
                    File file = new File(projectRoot, detectedFile);
                    if (fileManager == null) {
                        fileManager = new AiFileManager(projectRoot);
                        fileManager.setTabManager(tabManager);
                    }
                    AiFileManager.FileOp op = fileManager.applyFile(file, codeCapture);
                    if (op != null) {
                        statusLabel.setText("Created: " + detectedFile);
                        filesToOpen.add(file);
                        if (tabManager != null) tabManager.openCodeFile(file);
                        if (onFileCreated != null) onFileCreated.run();
                        refreshSidePanel();
                    } else {
                        statusLabel.setText("Failed to create " + detectedFile);
                    }
                }

                if (detectedFile != null && projectRoot != null) {
                    Button createFileBtn = new Button("Create");
                    createFileBtn.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-font-size: 9px; -fx-background-radius: 4; -fx-padding: 2 8; -fx-cursor: hand;");
                    final String fileCapture = detectedFile;
                    createFileBtn.setOnAction(ev -> createFile(fileCapture, codeCapture));
                    codeHeader.getChildren().addAll(langLabel, sp, createFileBtn, applyBtn, copyBtn);
                } else {
                    codeHeader.getChildren().addAll(langLabel, sp, applyBtn, copyBtn);
                }
            } else {
                codeHeader.getChildren().addAll(langLabel, sp);
            }

            codeBlock.getChildren().addAll(codeHeader, codeText);
            bubble.getChildren().add(codeBlock);
            lastEnd = m.end();
        }

        if (lastEnd < content.length()) {
            TextFlow tf = new TextFlow(makeCopyableText(content.substring(lastEnd), "-fx-fill: -text-primary;"));
            tf.setMaxWidth(460);
            bubble.getChildren().add(tf);
        }

        if (!codeBlocks.isEmpty() && allowApply) {
            Separator sep = new Separator();
            sep.setMaxWidth(400);
            sep.setStyle("-fx-padding: 6 0 2 0;");
            bubble.getChildren().add(sep);
            HBox actionBar = new HBox(6);
            actionBar.setAlignment(Pos.CENTER);
            actionBar.setPadding(new Insets(2, 0, 0, 0));
            Button applyAllBtn = new Button("Apply " + codeBlocks.size() + " Block(s)");
            applyAllBtn.setStyle("-fx-background-color: -accent; -fx-text-fill: white; -fx-font-size: 10px; -fx-background-radius: 6; -fx-padding: 4 14; -fx-cursor: hand; -fx-font-weight: bold;");
            applyAllBtn.setOnAction(ev -> applyWithConfirm(codeBlocks));
            Button skipBtn = new Button("Skip");
            skipBtn.setStyle("-fx-background-color: -bg-tertiary; -fx-text-fill: -text-primary; -fx-font-size: 10px; -fx-background-radius: 6; -fx-padding: 4 12; -fx-border-color: -border-color; -fx-border-radius: 6; -fx-cursor: hand;");
            skipBtn.setOnAction(ev -> actionBar.setVisible(false));
            Button editBtn = new Button("Edit");
            editBtn.setStyle("-fx-background-color: -bg-primary; -fx-text-fill: -text-primary; -fx-font-size: 10px; -fx-background-radius: 6; -fx-padding: 4 12; -fx-border-color: -border-color; -fx-border-radius: 6; -fx-cursor: hand;");
            editBtn.setOnAction(ev -> editAndApply(codeBlocks));
            actionBar.getChildren().addAll(applyAllBtn, editBtn, skipBtn);
            bubble.getChildren().add(actionBar);
        }

        if (!filesToOpen.isEmpty() && tabManager != null) {
            for (File f : filesToOpen) {
                tabManager.openCodeFile(f);
            }
            if (onFileCreated != null) onFileCreated.run();
        }

        // Update copy button content reference
        if (latestContentRef != null && latestContentRef.length > 0) {
            latestContentRef[0] = content;
        }
    }

    // Add right-click "Copy" to any Text node for text selection support
    private Text makeCopyableText(String content, String style) {
        Text t = new Text(content);
        if (style != null) t.setStyle(style);
        ContextMenu ctx = new ContextMenu();
        MenuItem copyItem = new MenuItem("Copy");
        copyItem.setOnAction(e -> {
            ClipboardContent cc = new ClipboardContent();
            cc.putString(content);
            Clipboard.getSystemClipboard().setContent(cc);
        });
        ctx.getItems().add(copyItem);
        t.setOnContextMenuRequested(e -> ctx.show(t, e.getScreenX(), e.getScreenY()));
        return t;
    }

    private void removeLastAIMessage() {
        int size = messagesContainer.getChildren().size();
        if (size > 0) messagesContainer.getChildren().remove(size - 1);
    }
    
    
        private String detectFilename(String lang, String code) {
        lang = lang.trim();

        // 1. لو فيه اسم ملف في الـ language tag (مثل ```java:MyClass.java)
        if (lang.contains(":")) {
            String after = lang.substring(lang.indexOf(":") + 1).trim();
            if (after.contains(".") && !after.contains(" ")) {
                return after;
            }
        }

        // 2. من أول سطر في الكود (أكثر ذكاء)
        String firstLine = code.split("\n", 2)[0].trim();
        String clean = firstLine.replaceAll("^(//|#|/\\*|<!--|%%|```).*?\\s+", "").trim();

        if (clean.matches("^[A-Za-z0-9_.-]+\\.[a-zA-Z0-9]+$") && 
            !clean.contains(" ") && clean.length() < 100) {
            return clean;
        }

        // 3. محاولة استخراج من الكود نفسه (لو فيه class أو filename)
        if (code.contains("class ") || code.contains("public class")) {
            Matcher m = Pattern.compile("class\\s+([A-Za-z0-9_]+)").matcher(code);
            if (m.find()) {
                return m.group(1) + ".java";
            }
        }

        if (code.contains("{") && code.contains("}")) {
            // JSON or config files
            if (code.contains("\"name\"") || code.contains("version")) {
                return "config.json";
            }
        }

        // 4. لو الـ lang فيه امتداد
        if (lang.matches("^[A-Za-z0-9_.-]+\\.[a-zA-Z0-9]+$")) {
            return lang;
        }

        return null;
    }

//    private String detectFilename(String lang, String code) {
//        if (lang.contains(":")) {
//            String after = lang.substring(lang.indexOf(":") + 1).trim();
//            if (after.contains(".") && !after.contains(" ")) return after;
//        }
//        String lower = lang.trim().toLowerCase();
//        if (lower.contains(".") && !lower.contains(" ") && !lower.matches("\\d+") && !lower.startsWith("http")) return lang.trim();
//        String firstLine = code.split("\n", 2)[0].trim();
//        String clean = firstLine.replaceAll("^(//|#|/\\*|<!--|%)\\s*", "").replaceAll("\\*/$", "").trim();
//        if (clean.contains(".") && !clean.contains(" ") && !clean.contains("http")) return clean;
//        return null;
//    }

    private void createFile(String filename, String code) {
        if (projectRoot == null || filename == null || filename.isEmpty()) {
            statusLabel.setText("No project root set. Open a project first.");
            return;
        }
        File file = new File(projectRoot, filename);
        if (fileManager == null) {
            fileManager = new AiFileManager(projectRoot);
            fileManager.setTabManager(tabManager);
        }
        AiFileManager.FileOp op = fileManager.applyFile(file, code);
        if (op != null) {
            if (tabManager != null) tabManager.openCodeFile(file);
            if (onFileCreated != null) onFileCreated.run();
            refreshSidePanel();
            statusLabel.setText("Created: " + filename);
        } else {
            statusLabel.setText("Failed to create file: " + filename);
        }
    }

    private void applyCode(String code) {
        if (tabManager == null) return;
        CodeEditor editor = tabManager.getActiveEditor();
        if (editor == null) return;
        String sel = editor.getSelectedText();
        if (!sel.isEmpty()) editor.replaceSelection(code);
        else editor.setText(code);
        statusLabel.setText("Code applied to editor");
    }

    private void applyWithConfirm(List<String> codeBlocks) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Apply Code");
        alert.setHeaderText("Apply " + codeBlocks.size() + " code block(s) to editor?");
        TextArea preview = new TextArea(codeBlocks.get(0));
        preview.setEditable(false);
        preview.setPrefHeight(150);
        preview.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px;");
        if (codeBlocks.size() > 1)
            alert.getDialogPane().setContent(new VBox(4, preview, new Label("... and " + (codeBlocks.size() - 1) + " more block(s)")));
        else
            alert.getDialogPane().setContent(preview);
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                for (String code : codeBlocks) applyCode(code);
            }
        });
    }

    private void editAndApply(List<String> codeBlocks) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Edit Code Before Applying");
        dialog.setHeaderText("Edit the code below, then click Apply:");
        TextArea editor = new TextArea(codeBlocks.get(0));
        editor.setPrefHeight(300);
        editor.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12px;");
        dialog.getDialogPane().setContent(editor);
        ButtonType applyBtn = new ButtonType("Apply", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(applyBtn, ButtonType.CANCEL);
        dialog.setResultConverter(b -> b == applyBtn ? editor.getText() : null);
        dialog.showAndWait().ifPresent(text -> {
            if (text != null && !text.trim().isEmpty()) applyCode(text.trim());
        });
    }

    public void sendPrompt(String text) {
        inputField.setText(text);
        sendMessage();
    }

    // ── Git Auto-Commit ──
    private void gitAutoCommit(final String message) {
        if (projectRoot == null) return;
        Thread t = new Thread(() -> {
            try {
                // Check if git repo
                ProcessBuilder check = new ProcessBuilder("git", "rev-parse", "--git-dir");
                check.directory(projectRoot);
                check.redirectErrorStream(true);
                Process cp = check.start();
                int ec = cp.waitFor();
                if (ec != 0) {
                    Platform.runLater(() -> statusLabel.setText("Not a git repo, skipping auto-commit"));
                    return;
                }
                // git add -A
                ProcessBuilder add = new ProcessBuilder("git", "add", "-A");
                add.directory(projectRoot);
                add.redirectErrorStream(true);
                Process ap = add.start();
                ap.waitFor();
                // git commit
                ProcessBuilder commit = new ProcessBuilder("git", "commit", "-m", message);
                commit.directory(projectRoot);
                commit.redirectErrorStream(true);
                Process cmp = commit.start();
                StringBuilder output = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(cmp.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) output.append(line).append("\n");
                }
                int exitCode = cmp.waitFor();
                final String result = output.toString().trim();
                Platform.runLater(() -> {
                    if (exitCode == 0) {
                        statusLabel.setText("Git: committed AI changes");
                    } else {
                        String err = result.isEmpty() ? "nothing to commit" : result;
                        statusLabel.setText("Git: " + (err.length() > 80 ? err.substring(0, 80) + "..." : err));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Git commit failed: " + e.getMessage()));
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public void clearChat() {
        messagesContainer.getChildren().clear();
        chatHistory.clear();
        statusLabel.setText("");
    }

    // ── Terminal Command Execution ──
    private void executeTerminalCommand(String text) {
        addMessage("You", text, false);
        String cmd = text.substring(1).trim(); // remove !
        addMessage("System", "Running: " + cmd, false);
        statusLabel.setText("Running: " + cmd);
        executeShellCommand(cmd);
    }

    private void executeShellCommand(final String command) {
        Thread t = new Thread(() -> {
            try {
                ProcessBuilder pb;
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    pb = new ProcessBuilder("cmd.exe", "/c", command);
                } else {
                    pb = new ProcessBuilder("sh", "-c", command);
                }
                pb.directory(projectRoot != null ? projectRoot : new File("."));
                pb.redirectErrorStream(true);
                Process p = pb.start();
                StringBuilder output = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }
                int exitCode = p.waitFor();
                final String result = output.toString().trim();
                Platform.runLater(() -> {
                    String msg = "Command: " + command + "\nExit code: " + exitCode;
                    if (!result.isEmpty()) {
                        msg += "\n" + (result.length() > 2000 ? result.substring(0, 2000) + "\n..." : result);
                    }
                    addMessage("System", msg, false);
                    statusLabel.setText("Command finished (exit " + exitCode + ")");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    addMessage("System", "Command failed: " + e.getMessage(), false);
                    statusLabel.setText("Command failed");
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // ── Web Search ──
    private void webSearch(String query) {
        addMessage("You", "!web " + query, false);
        addMessage("AI", "Searching web for: " + query + "...", false);
        statusLabel.setText("Searching web...");
        Thread t = new Thread(() -> {
            try {
                String encoded = URLEncoder.encode(query, "UTF-8");
                URL url = new URL("https://api.duckduckgo.com/?q=" + encoded + "&format=json&no_html=1");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                StringBuilder resp = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) resp.append(line);
                }
                String json = resp.toString();
                // Extract AbstractText and RelatedTopics
                JsonObject obj = gson.fromJson(json, JsonObject.class);
                StringBuilder result = new StringBuilder();
                if (obj.has("AbstractText") && !obj.get("AbstractText").getAsString().isEmpty()) {
                    result.append(obj.get("AbstractText").getAsString()).append("\n\n");
                }
                if (obj.has("AbstractURL") && !obj.get("AbstractURL").getAsString().isEmpty()) {
                    result.append("Source: ").append(obj.get("AbstractURL").getAsString()).append("\n\n");
                }
                if (obj.has("RelatedTopics")) {
                    JsonArray topics = obj.getAsJsonArray("RelatedTopics");
                    int count = 0;
                    for (int i = 0; i < topics.size() && count < 5; i++) {
                        JsonObject tpc = topics.get(i).getAsJsonObject();
                        if (tpc.has("Text")) {
                            result.append("- ").append(tpc.get("Text").getAsString()).append("\n");
                            count++;
                        }
                    }
                }
                if (result.length() == 0) result.append("No results found for: ").append(query);
                final String finalResult = result.toString();
                Platform.runLater(() -> {
                    removeLastAIMessage();
                    addMessage("AI", finalResult.length() > 3000 ? finalResult.substring(0, 3000) + "\n..." : finalResult, false);
                    statusLabel.setText("Web search complete");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    removeLastAIMessage();
                    addMessage("AI", "Web search failed: " + e.getMessage() + "\nTry again later.", false);
                    statusLabel.setText("Web search failed");
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // ── Session Persistence ──
    private void saveSession() {
        if (projectRoot == null) { statusLabel.setText("No project open"); return; }
        try {
            new File(SESSION_DIR).mkdirs();
            String sessionName = projectRoot.getName() + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            File f = new File(SESSION_DIR, sessionName + ".json");
            JsonObject obj = new JsonObject();
            obj.addProperty("project", projectRoot.getAbsolutePath());
            obj.addProperty("timestamp", LocalDateTime.now().toString());
            obj.addProperty("provider", providerType);
            obj.addProperty("model", getModelName());
            JsonArray msgs = new JsonArray();
            for (ChatMessage msg : chatHistory) {
                JsonObject m = new JsonObject();
                m.addProperty("role", msg.role);
                m.addProperty("content", msg.content);
                msgs.add(m);
            }
            obj.add("messages", msgs);
            // Add file ops
            if (fileManager != null) {
                JsonArray ops = new JsonArray();
                for (AiFileManager.FileOp op : fileManager.getHistory()) {
                    JsonObject o = new JsonObject();
                    o.addProperty("file", op.file.getAbsolutePath());
                    o.addProperty("type", op.type);
                    o.addProperty("diff", op.diffText.length() > 500 ? op.diffText.substring(0, 500) : op.diffText);
                    ops.add(o);
                }
                obj.add("fileOperations", ops);
                obj.addProperty("totalTokens", fileManager.getTotalTokens());
                obj.addProperty("totalCost", fileManager.getTotalCostCents());
            }
            try (FileWriter w = new FileWriter(f)) {
                SESSION_GSON.toJson(obj, w);
            }
            addMessage("System", "Session saved: " + sessionName, false);
            statusLabel.setText("Session saved: " + sessionName);
            currentSessionId = sessionName;
        } catch (IOException e) {
            statusLabel.setText("Failed to save session: " + e.getMessage());
        }
    }

    private void loadSession(String projectName) {
        try {
            new File(SESSION_DIR).mkdirs();
            File[] sessions = new File(SESSION_DIR).listFiles((dir, name) -> name.startsWith(projectName) && name.endsWith(".json"));
            if (sessions == null || sessions.length == 0) return;
            // Load the most recent
            File latest = sessions[0];
            for (File s : sessions) if (s.lastModified() > latest.lastModified()) latest = s;
            JsonObject obj = gson.fromJson(new String(Files.readAllBytes(latest.toPath()), StandardCharsets.UTF_8), JsonObject.class);
            if (obj.has("messages")) {
                JsonArray msgs = obj.getAsJsonArray("messages");
                for (int i = 0; i < msgs.size(); i++) {
                    JsonObject m = msgs.get(i).getAsJsonObject();
                    String role = m.get("role").getAsString();
                    String content = m.get("content").getAsString();
                    chatHistory.add(new ChatMessage(role.equals("user") ? "user" : "assistant", content));
                    // Add to UI (skip bulk restore to avoid UI lag for large sessions)
                    if (i < 20) {
                        addMessage(role.equals("user") ? "You" : "AI", content.length() > 200 ? content.substring(0, 200) + "..." : content, false);
                    }
                }
                if (msgs.size() > 20) {
                    addMessage("System", "... and " + (msgs.size() - 20) + " more messages from session: " + latest.getName(), false);
                }
                statusLabel.setText("Loaded session: " + latest.getName() + " (" + msgs.size() + " messages)");
            }
            currentSessionId = latest.getName();
        } catch (Exception ignored) {}
    }

    private String getModelName() {
        switch (providerType) {
            case "gemini": return geminiModel;
            case "openai": return openaiModel;
            case "ollama": return ollamaModel;
            case "localgguf": return new File(localGgufPath).getName();
            default: return providerType;
        }
    }

    private static class ChatMessage {
        final String role;
        final String content;
        ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
