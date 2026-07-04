package com.eagle.editor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;

/**
 * Full-featured code editor control: RichTextFX CodeArea with syntax
 * highlighting, line numbers (built-in gutter), auto-indent, bracket/tag
 * auto-closing, and a first-keystroke autocomplete popup.
 */
public class CodeEditor extends StackPane {

    private final CodeArea codeArea = new CodeArea();
    private final LanguageType language;
    private final Popup completionPopup = new Popup();
    private final ListView<CompletionProvider.Suggestion> completionList = new ListView<>();
    private final VBox completionDetailsPanel = new VBox();
    private final Label completionDetailLabel = new Label();
    private final Label completionDetailDesc = new Label();
    private final Label completionDetailCode = new Label();
    private final Label completionDetailParams = new Label();
    private final Label completionDetailReturn = new Label();
    private final SignatureHelpPopup signatureHelp = new SignatureHelpPopup();
    private GhostTextOverlay ghostText;
    private IndentGuideOverlay indentGuides;
    private InlayHintOverlay inlayHints;
    private Tooltip hoverTooltip;
    private MinimapPanel minimap;
    private final FindReplacePanel findReplacePanel;

    private Runnable onContentChanged;
    private Consumer<Integer> onCaretMoved;
    private Consumer<Integer> onBreakpointToggled;
    private Consumer<List<CodeLinter.Problem>> onProblemsChanged;
    private Consumer<String> onRuntimeError;
    private Runnable onGoToDefinition;
    private Runnable onFindReferences;
    private Runnable onRenameSymbol;

    public void setOnGoToDefinition(Runnable r) { this.onGoToDefinition = r; }
    public void setOnFindReferences(Runnable r) { this.onFindReferences = r; }
    public void setOnRenameSymbol(Runnable r) { this.onRenameSymbol = r; }

    private final Set<Integer> breakpointLines = new java.util.HashSet<>();
    private final Set<Integer> runtimeErrorLines = new java.util.HashSet<>();
    private final java.util.Map<Integer, String> errorMessages = new java.util.HashMap<>();
    private List<CodeLinter.Problem> lastProblems = new ArrayList<>();
    private java.util.List<CodeLinter.Problem> lspDiagnostics = new ArrayList<>();

    private com.eagle.lsp.LspIntegration lspIntegration;
    private AiInlineProvider aiInlineProvider;

    public void setLspIntegration(com.eagle.lsp.LspIntegration integration) {
        this.lspIntegration = integration;
    }
    public com.eagle.lsp.LspIntegration getLspIntegration() { return lspIntegration; }

    public void setAiInlineProvider(AiInlineProvider p) { this.aiInlineProvider = p; }
    public AiInlineProvider getAiInlineProvider() { return aiInlineProvider; }

    /**
     * Merge LSP diagnostics into the editor's problem set and re-render.
     */
    public void mergeLspDiagnostics(java.util.List<CodeLinter.Problem> problems) {
        lspDiagnostics = problems;
        // Re-run lint to update the gutter with combined problems
        applyLspDiagnosticStyles();
    }

    private void applyLspDiagnosticStyles() {
        for (CodeLinter.Problem p : lspDiagnostics) {
            if (p.start >= 0 && p.end > p.start && p.start < codeArea.getLength()) {
                int end = Math.min(p.end, codeArea.getLength());
                String severityStyle;
                switch (p.severity) {
                    case ERROR: severityStyle = "syn-error"; break;
                    case WARNING: severityStyle = "syn-warning"; break;
                    default: severityStyle = "syn-info"; break;
                }
                try {
                    java.util.Collection<String> existing = new java.util.ArrayList<>(codeArea.getStyleOfChar(p.start));
                    existing.add(severityStyle);
                    codeArea.setStyle(p.start, end, existing);
                } catch (Exception ignored) { }
            }
        }
        codeArea.setParagraphGraphicFactory(buildLineNumberAndBreakpointFactory());
    }

    // Word highlight (all occurrences of selected word)
    private final java.util.List<int[]> wordHighlightRanges = new java.util.ArrayList<>();
    private String lastSelectedWord = "";

    // Search highlight (Find in Project)
    private final java.util.List<int[]> searchHighlightRanges = new java.util.ArrayList<>();
    private String searchHighlightQuery = null;

    private static final Pattern WORD_BOUNDARY = Pattern.compile("[A-Za-z_][A-Za-z0-9_-]*$");
    private static final Pattern HTML_TAG_PREFIX = Pattern.compile("<(/?[A-Za-z]*)$");
    private static final Pattern CSS_ATTR_PREFIX = Pattern.compile("@[A-Za-z-]*$");
    private static final Pattern CSS_PROP_PREFIX = Pattern.compile("([a-zA-Z-]+\\s*:\\s*)([^;]*)$");
    private static final Pattern CSS_SEL_PREFIX = Pattern.compile("[.#][A-Za-z-]*$");
    private double currentFontSize = 13.5;
    private String dynamicCssUrl;
    private String currentText = "";
    private boolean internalTextChange = false;
    private boolean pendingGhostUpdate = false;

    /** Dual-editor mode toggle (RichTextFX ↔ Monaco) */
    public enum EditorMode { RICHTEXT, MONACO }
    private EditorMode editorMode = EditorMode.RICHTEXT;
    private MonacoEditor monacoEditor;
    private javafx.scene.layout.BorderPane wrapper;
    private StackPane editorContainer;

    public CodeEditor(LanguageType language) {
        this.language = language;
        getStyleClass().add("code-editor-root");

        codeArea.setParagraphGraphicFactory(buildLineNumberAndBreakpointFactory());
        codeArea.getStyleClass().add("code-area");
        codeArea.setWrapText(false);
        try { codeArea.getClass().getMethod("setMultiSelectionEnabled", boolean.class).invoke(codeArea, true); } catch (Exception ignored) {}

        findReplacePanel = new FindReplacePanel(codeArea);
        minimap = new MinimapPanel();
        minimap.setEditor(this);

        monacoEditor = new MonacoEditor(language);
        monacoEditor.setVisible(false);

        wrapper = new javafx.scene.layout.BorderPane();
        wrapper.setTop(findReplacePanel);
        wrapper.setCenter(codeArea);
        wrapper.setRight(minimap);

        editorContainer = new StackPane(wrapper, monacoEditor);
        getChildren().add(editorContainer);

        monacoEditor.setOnContentChanged(text -> {
            // Only update currentText from Monaco if we're in Monaco mode
            if (editorMode == EditorMode.MONACO) {
                currentText = text != null ? text : "";
                textProp.set(currentText);
            }
            if (onContentChanged != null) {
                Platform.runLater(onContentChanged);
            }
        });
        monacoEditor.setOnCaretMoved(line -> {
            if (editorMode == EditorMode.MONACO && onCaretMoved != null) {
                onCaretMoved.accept(line);
            }
        });
        monacoEditor.setOnBreakpointToggled(lineIndex -> {
            if (editorMode == EditorMode.MONACO) {
                if (breakpointLines.contains(lineIndex)) {
                    breakpointLines.remove(lineIndex);
                } else {
                    breakpointLines.add(lineIndex);
                }
                if (onBreakpointToggled != null) {
                    onBreakpointToggled.accept(lineIndex);
                }
            }
        });

        codeArea.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.F) {
                findReplacePanel.showPanel();
                e.consume();
            }
        });

        setupHighlighting();
        setupAutoIndentAndBrackets();
        setupCompletionPopup();
        setupSignatureHelp();
        setupHover();
        setupCaretListener();
        setupContextMenu();
        setupGhostText();
        setupIndentGuides();
        setupInlayHints();
    }

    /** Wraps RichTextFX's line number factory, adding a clickable breakpoint dot before each line number. */
    private IntFunction<Node> buildLineNumberAndBreakpointFactory() {
        IntFunction<Node> numberFactory = LineNumberFactory.get(codeArea);
        return lineIndex -> {
            Node numberNode = numberFactory.apply(lineIndex);

            StackPane dot = new StackPane();
            dot.setPrefSize(10, 10);
            dot.getStyleClass().add("breakpoint-dot");
            updateDotVisual(dot, lineIndex);
            dot.setOnMouseClicked((MouseEvent e) -> {
                toggleBreakpoint(lineIndex);
                updateDotVisual(dot, lineIndex);
            });

            Label foldArrow = new Label();
            foldArrow.setPrefWidth(12);
            foldArrow.getStyleClass().add("fold-arrow");
            updateFoldArrowVisual(foldArrow, lineIndex);
            foldArrow.setOnMouseClicked((MouseEvent e) -> toggleFold(lineIndex, foldArrow));

            javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(4, dot, foldArrow, numberNode);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setStyle("-fx-padding: 0 4 0 4;");
            return row;
        };
    }

    // ---------------------------------------------------------------- CODE FOLDING

    private final java.util.Map<Integer, String> foldedContentMap = new java.util.HashMap<>();

    private boolean lineOpensFoldableBlock(int lineIndex) {
        if (lineIndex < 0 || lineIndex >= codeArea.getParagraphs().size()) return false;
        String line = codeArea.getParagraph(lineIndex).getText();
        return line.trim().endsWith("{");
    }

    private int findFoldEndLine(int startLine) {
        int depth = 0;
        int totalLines = codeArea.getParagraphs().size();
        for (int i = startLine; i < totalLines; i++) {
            String raw = codeArea.getParagraph(i).getText();
            String line = raw.replaceAll("\"(?:\\\\.|[^\"\\\\])*\"", "\"\"")
                             .replaceAll("'(?:\\\\.|[^'\\\\])*'", "''")
                             .replaceAll("`(?:\\\\.|[^`\\\\])*`", "``")
                             .replaceAll("//[^\n]*", "")
                             .replaceAll("/\\*.*?\\*/", "");
            for (char c : line.toCharArray()) {
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0 && i > startLine) return i;
                }
            }
        }
        return -1;
    }

    private void toggleFold(int lineIndex, Label arrow) {
        if (!lineOpensFoldableBlock(lineIndex)) return;

        if (foldedContentMap.containsKey(lineIndex)) {
            unfoldCodeBlock(lineIndex);
        } else {
            int endLine = findFoldEndLine(lineIndex);
            if (endLine > lineIndex) {
                foldCodeBlock(lineIndex, endLine);
            }
        }
        updateFoldArrowVisual(arrow, lineIndex);
        codeArea.setParagraphGraphicFactory(buildLineNumberAndBreakpointFactory());
    }

    private void foldCodeBlock(int startLine, int endLine) {
        int totalLines = codeArea.getParagraphs().size();
        if (startLine + 1 >= totalLines) return;

        int fromPos = codeArea.getAbsolutePosition(startLine + 1, 0);
        int toPos;
        if (endLine + 1 < totalLines) {
            toPos = codeArea.getAbsolutePosition(endLine + 1, 0);
        } else {
            toPos = codeArea.getLength();
        }
        if (fromPos >= toPos) return;

        String foldedText = codeArea.getText(fromPos, toPos);
        foldedContentMap.put(startLine, foldedText);
        codeArea.replaceText(fromPos, toPos, "    // ...");
    }

    private void unfoldCodeBlock(int startLine) {
        String storedText = foldedContentMap.get(startLine);
        if (storedText == null) return;

        int totalLines = codeArea.getParagraphs().size();
        if (startLine + 1 >= totalLines) return;

        int fromPos = codeArea.getAbsolutePosition(startLine + 1, 0);
        int toPos;
        if (startLine + 2 < totalLines) {
            toPos = codeArea.getAbsolutePosition(startLine + 2, 0);
        } else {
            toPos = codeArea.getLength();
        }
        if (fromPos >= toPos) return;

        codeArea.replaceText(fromPos, toPos, storedText);
        foldedContentMap.remove(startLine);
    }

    private void updateFoldArrowVisual(Label arrow, int lineIndex) {
        if (!lineOpensFoldableBlock(lineIndex)) {
            arrow.setText("");
            arrow.setStyle("-fx-cursor: default;");
            foldedContentMap.remove(lineIndex);
            return;
        }
        boolean folded = foldedContentMap.containsKey(lineIndex);
        arrow.setText(folded ? "▸" : "▾");
        arrow.setStyle("-fx-cursor: hand; -fx-font-size: 9px; -fx-text-fill: -text-muted;");
    }

    private void updateDotVisual(StackPane dot, int lineIndex) {
        if (breakpointLines.contains(lineIndex)) {
            dot.setStyle("-fx-background-color: #e74c3c; -fx-background-radius: 5; -fx-cursor: hand;");
            dot.getProperties().put("tooltip", "Breakpoint");
        } else if (runtimeErrorLines.contains(lineIndex)) {
            dot.setStyle("-fx-background-color: #ff0000; -fx-background-radius: 5; -fx-cursor: help;");
            String msg = errorMessages.getOrDefault(lineIndex, "Runtime error");
            Tooltip t = new Tooltip("[RUNTIME ERROR] " + msg);
            Tooltip.install(dot, t);
        } else {
            CodeLinter.Problem problem = problemAtLine(lineIndex);
            if (problem != null) {
                String color = problem.severity == CodeLinter.Problem.Severity.ERROR ? "#e74c3c"
                        : problem.severity == CodeLinter.Problem.Severity.WARNING ? "#f1c40f" : "#3498db";
                dot.setStyle("-fx-background-color: " + color + "80; -fx-background-radius: 5; -fx-cursor: help;");
                Tooltip t = new Tooltip("[" + problem.severity + "] " + problem.message);
                Tooltip.install(dot, t);
            } else {
                dot.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                dot.getProperties().remove("tooltip");
            }
        }
    }

    private CodeLinter.Problem problemAtLine(int lineIndex) {
        int line = lineIndex + 1;
        for (CodeLinter.Problem p : lastProblems) {
            if (p.line == line) return p;
        }
        return null;
    }


    private void toggleBreakpoint(int lineIndex) {
        if (breakpointLines.contains(lineIndex)) {
            breakpointLines.remove(lineIndex);
        } else {
            breakpointLines.add(lineIndex);
        }
        if (onBreakpointToggled != null) {
            onBreakpointToggled.accept(lineIndex);
        }
    }

    public Set<Integer> getBreakpointLines() {
        return breakpointLines;
    }

    public void setOnBreakpointToggled(Consumer<Integer> callback) {
        this.onBreakpointToggled = callback;
    }

    /** Reports a runtime error (from debugger) to show inline in the editor gutter. */
    public void reportRuntimeError(String errorMessage) {
        runtimeErrorLines.clear();
        errorMessages.clear();
        if (errorMessage == null || errorMessage.isEmpty()) return;
        // Try to parse line number from error message: "line X" or "line X, col Y" or "(line X)"
        java.util.regex.Matcher m = Pattern.compile("line\\s+(\\d+)").matcher(errorMessage);
        if (m.find()) {
            int line = Integer.parseInt(m.group(1));
            int line0 = line - 1; // convert to 0-based
            if (line0 >= 0 && line0 < codeArea.getParagraphs().size()) {
                runtimeErrorLines.add(line0);
                errorMessages.put(line0, errorMessage);
            }
        }
        codeArea.setParagraphGraphicFactory(buildLineNumberAndBreakpointFactory());
    }

    public void clearRuntimeErrors() {
        runtimeErrorLines.clear();
        errorMessages.clear();
        codeArea.setParagraphGraphicFactory(buildLineNumberAndBreakpointFactory());
    }

    public void setOnRuntimeError(Consumer<String> callback) {
        this.onRuntimeError = callback;
    }

    public void setOnProblemsChanged(Consumer<List<CodeLinter.Problem>> callback) {
        this.onProblemsChanged = callback;
    }

    public List<CodeLinter.Problem> getProblems() {
        return lastProblems;
    }

    // ---------------------------------------------------------------- GHOST TEXT

    private void setupGhostText() {
        ghostText = new GhostTextOverlay(codeArea, this);
        ghostText.setOnAccept(this::onGhostTextAccepted);

        // Auto-show ghost text on typing (debounced)
        codeArea.textProperty().addListener((obs, old, newText) -> {
            if (internalTextChange || ghostText == null) return;
            if (pendingGhostUpdate) return;
            pendingGhostUpdate = true;
            Platform.runLater(() -> { pendingGhostUpdate = false; autoShowGhostSuggestion(); });
        });

        codeArea.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.SPACE) {
                if (!ghostText.isShowing()) {
                    autoShowGhostSuggestion();
                } else {
                    ghostText.accept();
                }
                e.consume();
            }
        });
    }

    private void autoShowGhostSuggestion() {
        if (ghostText == null || ghostText.isShowing()) return;
        if (completionPopup.isShowing()) return;
        int caret = codeArea.getCaretPosition();
        if (caret < 0) return;
        String text = codeArea.getText();
        if (text.isEmpty()) return;
        int start = caret;
        while (start > 0 && Character.isLetterOrDigit(text.charAt(start - 1))) start--;
        if (start >= caret) return;
        String prefix = text.substring(start, caret);
        if (prefix.isEmpty()) return;

        // Try AI inline completion first
        if (aiInlineProvider != null && aiInlineProvider.isAvailable()) {
            String codeBefore = text.substring(0, caret);
            String codeAfter = text.substring(caret);
            int line = codeArea.getCurrentParagraph();
            aiInlineProvider.getInlineCompletion(codeBefore, codeAfter,
                language != null ? language.name().toLowerCase() : "unknown", line)
                .thenAccept(completion -> {
                    if (completion != null && !completion.isEmpty()
                        && !ghostText.isShowing() && !completionPopup.isShowing()) {
                        Platform.runLater(() -> {
                            ghostText.showCustom(completion, caret);
                        });
                    }
                });
            return;
        }

        // Fallback to static CompletionProvider
        List<CompletionProvider.Suggestion> suggestions = CompletionProvider.getSuggestions(language, prefix);
        if (!suggestions.isEmpty()) {
            CompletionProvider.Suggestion best = suggestions.get(0);
            String completion = best.insertText != null ? best.insertText : best.label;
            if (completion.length() > prefix.length()) {
                ghostText.showCustom(completion.substring(prefix.length()), caret);
            }
        }
    }

    private void showGhostSuggestion() {
        autoShowGhostSuggestion();
    }

    private void onGhostTextAccepted() {
        int caret = codeArea.getCaretPosition();
        completionList.getSelectionModel().select(0);
    }

    // ---------------------------------------------------------------- INDENT GUIDES

    private void setupIndentGuides() {
        indentGuides = new IndentGuideOverlay(codeArea, this);
        indentGuides.setVisible(com.eagle.util.EditorSettings.isShowIndentGuide());
    }

    // ---------------------------------------------------------------- INLAY HINTS

    private void setupInlayHints() {
        inlayHints = new InlayHintOverlay(codeArea, this);
        codeArea.caretPositionProperty().addListener((o, a, b) -> {
            if (inlayHints != null) inlayHints.update();
        });
        codeArea.textProperty().addListener((o, a, b) -> {
            if (inlayHints != null) inlayHints.update();
        });
        try {
            codeArea.estimatedScrollYProperty().addListener((o, old, val) -> {
                if (inlayHints != null) inlayHints.update();
            });
        } catch (Exception ignored) {}
    }

    // ---------------------------------------------------------------- HIGHLIGHTING

    private void setupHighlighting() {
        codeArea.multiPlainChanges()
                .successionEnds(Duration.ofMillis(80))
                .subscribe(ignore -> {
                    // Only update currentText from codeArea if we're in RichText mode
                    if (!internalTextChange && editorMode == EditorMode.RICHTEXT) {
                        currentText = codeArea.getText();
                        textProp.set(currentText);
                    }
                    applyHighlighting();
                    if (!wordHighlightRanges.isEmpty()) {
                        applyWordHighlights();
                    }
                    if (!searchHighlightRanges.isEmpty()) {
                        applySearchHighlights();
                    }
                    runLint();
                    if (language == LanguageType.CSS) {
                        refreshColorSwatches();
                    }
                });
    }

    private void runLint() {
        lastProblems = CodeLinter.lint(codeArea.getText(), language);
        if (onProblemsChanged != null) {
            onProblemsChanged.accept(lastProblems);
        }
        String severityStyle;
        for (CodeLinter.Problem p : lastProblems) {
            if (p.start >= 0 && p.end > p.start && p.start < codeArea.getLength()) {
                int end = Math.min(p.end, codeArea.getLength());
                switch (p.severity) {
                    case ERROR: severityStyle = "syn-error"; break;
                    case WARNING: severityStyle = "syn-warning"; break;
                    default: severityStyle = "syn-info"; break;
                }
                try {
                    Collection<String> existing = new ArrayList<>(codeArea.getStyleOfChar(p.start));
                    existing.add(severityStyle);
                    codeArea.setStyle(p.start, end, existing);
                } catch (Exception ignored) { }
            }
        }
        codeArea.setParagraphGraphicFactory(buildLineNumberAndBreakpointFactory());
    }

    // ---------------------------------------------------------------- INLINE COLOR SWATCHES

    private final java.util.List<javafx.scene.shape.Rectangle> activeSwatches = new java.util.ArrayList<>();
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("#[0-9a-fA-F]{3,8}\\b");
    private boolean colorSwatchesActive = true;

    private void refreshColorSwatches() {
        if (!colorSwatchesActive) { activeSwatches.clear(); return; }
        // Remove old swatch popups before re-adding for the new text
        for (javafx.scene.shape.Rectangle r : activeSwatches) {
            Popup p = (Popup) r.getProperties().get("popup");
            if (p != null) p.hide();
        }
        activeSwatches.clear();

        String text = codeArea.getText();
        Matcher m = HEX_COLOR_PATTERN.matcher(text);
        while (m.find()) {
            String hex = m.group();
            int matchStart = m.start();
            try {
                javafx.scene.paint.Color color = javafx.scene.paint.Color.web(hex);
                javafx.scene.shape.Rectangle swatch = new javafx.scene.shape.Rectangle(10, 10, color);
                swatch.setStroke(javafx.scene.paint.Color.GRAY);
                swatch.setStrokeWidth(0.5);
                swatch.setCursor(javafx.scene.Cursor.HAND);

                Popup swatchPopup = new Popup();
                swatchPopup.getContent().add(swatch);
                swatchPopup.setAutoFix(true);
                swatchPopup.setAutoHide(true);
                swatch.getProperties().put("popup", swatchPopup);

                swatch.setOnMouseClicked(evt -> openColorPickerFor(matchStart, hex.length()));

                Platform.runLater(() -> positionSwatch(swatchPopup, swatch, matchStart));

                activeSwatches.add(swatch);
            } catch (Exception ignored) { /* not a valid color */ }
        }
    }

    private void positionSwatch(Popup popup, javafx.scene.shape.Rectangle swatch, int charPos) {
        if (!colorSwatchesActive) return;
        if (charPos > codeArea.getLength()) return;
        Scene s = codeArea.getScene();
        if (s == null || s.getWindow() == null || !s.getWindow().isShowing()) return;
        try {
            java.util.Optional<Bounds> boundsOpt = codeArea.getCharacterBoundsOnScreen(charPos, charPos + 1);
            boundsOpt.ifPresent(b -> {
                if (!popup.isShowing()) {
                    popup.show(codeArea, b.getMinX() - 14, b.getMinY() + 2);
                } else {
                    popup.setX(b.getMinX() - 14);
                    popup.setY(b.getMinY() + 2);
                }
            });
        } catch (Exception ignored) { }
    }

    private void openColorPickerFor(int start, int length) {
        String currentHex = codeArea.getText(start, start + length);
        javafx.scene.control.ColorPicker picker = new javafx.scene.control.ColorPicker();
        try {
            picker.setValue(javafx.scene.paint.Color.web(currentHex));
        } catch (Exception ignored) { }

        Popup pickerPopup = new Popup();
        pickerPopup.getContent().add(picker);
        pickerPopup.setAutoHide(true);
        codeArea.getCharacterBoundsOnScreen(start, start + length).ifPresent(b ->
                pickerPopup.show(codeArea, b.getMinX(), b.getMaxY() + 4));

        picker.setOnAction(e -> {
            String newHex = toHex(picker.getValue());
            codeArea.replaceText(start, start + length, newHex);
            pickerPopup.hide();
        });
    }

    private String toHex(javafx.scene.paint.Color c) {
        return String.format("#%02X%02X%02X",
                (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255));
    }

    private void applyHighlighting() {
        String text = codeArea.getText();
        StyleSpans<Collection<String>> spans = SyntaxHighlighter.computeHighlighting(text, language);
        codeArea.setStyleSpans(0, spans);
        if (lastBracketHighlight != null) {
            for (int pos : lastBracketHighlight) {
                applyBracketStyleRaw(pos);
            }
        }
    }

    // ---------------------------------------------------------------- AUTO-INDENT / AUTO-CLOSE

    private void setupAutoIndentAndBrackets() {
        codeArea.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
        codeArea.addEventFilter(KeyEvent.KEY_TYPED, this::handleKeyTyped);
    }

    private void handleKeyPressed(KeyEvent e) {
        // Backspace inside empty pair → delete both
        if (e.getCode() == KeyCode.BACK_SPACE && !completionPopup.isShowing()) {
            int caret = codeArea.getCaretPosition();
            if (caret > 0 && caret < codeArea.getLength()) {
                String text = codeArea.getText();
                char before = text.charAt(caret - 1);
                char after = text.charAt(caret);
                for (java.util.Map.Entry<Character, Character> entry : AUTO_CLOSE_PAIRS.entrySet()) {
                    if (before == entry.getKey() && after == entry.getValue()) {
                        codeArea.deleteText(caret - 1, caret + 1);
                        e.consume();
                        return;
                    }
                }
            }
        }
        if (e.getCode() == KeyCode.ENTER && !completionPopup.isShowing()) {
            int caret = codeArea.getCaretPosition();
            String text = codeArea.getText();
            int lineStart = text.lastIndexOf('\n', caret - 1) + 1;
            String currentLine = text.substring(lineStart, caret);

            StringBuilder indent = new StringBuilder();
            for (char c : currentLine.toCharArray()) {
                if (c == ' ' || c == '\t') indent.append(c);
                else break;
            }

            boolean opensBlock = currentLine.trim().endsWith("{") || currentLine.trim().endsWith(":");
            String extra = opensBlock ? "    " : "";

            e.consume();
            codeArea.insertText(caret, "\n" + indent.toString() + extra);
        } else if (e.getCode() == KeyCode.TAB && !completionPopup.isShowing()) {
            e.consume();
            IndexRange sel = codeArea.getSelection();
            if (sel.getLength() == 0) {
                codeArea.insertText(codeArea.getCaretPosition(), "    ");
            } else {
                codeArea.replaceSelection("    ");
            }
        } else if (e.getCode() == KeyCode.DOWN && completionPopup.isShowing()) {
            moveCompletionSelection(1);
            e.consume();
        } else if (e.getCode() == KeyCode.UP && completionPopup.isShowing()) {
            moveCompletionSelection(-1);
            e.consume();
        } else if ((e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.TAB) && completionPopup.isShowing()) {
            applySelectedCompletion();
            e.consume();
        } else if (e.getCode() == KeyCode.ESCAPE && completionPopup.isShowing()) {
            hideCompletion();
            e.consume();
        }
    }

    private static final java.util.Map<Character, Character> AUTO_CLOSE_PAIRS = new java.util.HashMap<>();
    private static final java.util.Set<Character> CLOSING_CHARS = new java.util.HashSet<>();
    static {
        AUTO_CLOSE_PAIRS.put('(', ')');
        AUTO_CLOSE_PAIRS.put('[', ']');
        AUTO_CLOSE_PAIRS.put('{', '}');
        AUTO_CLOSE_PAIRS.put('"', '"');
        AUTO_CLOSE_PAIRS.put('\'', '\'');
        AUTO_CLOSE_PAIRS.put('`', '`');
        CLOSING_CHARS.add(')');
        CLOSING_CHARS.add(']');
        CLOSING_CHARS.add('}');
        CLOSING_CHARS.add('"');
        CLOSING_CHARS.add('\'');
        CLOSING_CHARS.add('`');
        CLOSING_CHARS.add('>');
    }

    /** Smart auto-close brackets/quotes + smart skip + selection wrapping. */
    private void handleKeyTyped(KeyEvent e) {
        String typed = e.getCharacter();
        if (typed.length() != 1) {
            Platform.runLater(this::updateCompletionPopup);
            return;
        }
        char c = typed.charAt(0);

        // Enter while completion is showing → accept
        if ((c == '\n' || c == '\r') && completionPopup.isShowing()) {
            applySelectedCompletion();
            e.consume();
            return;
        }

        // --- Smart closing: if next char matches typed closing char, just skip over ---
        if (CLOSING_CHARS.contains(c) && codeArea.getCaretPosition() < codeArea.getLength()) {
            char next = codeArea.getText().charAt(codeArea.getCaretPosition());
            if (next == c) {
                // Check if it's a paired case: for quotes, only skip if we're inside a pair
                if (c == '"' || c == '\'' || c == '`') {
                    // Skip if next char is same quote (overtype)
                    codeArea.moveTo(codeArea.getCaretPosition() + 1);
                    e.consume();
                    return;
                }
                codeArea.moveTo(codeArea.getCaretPosition() + 1);
                e.consume();
                return;
            }
        }

        // --- Auto-close brackets/quotes ---
        if (AUTO_CLOSE_PAIRS.containsKey(c)) {
            char closing = AUTO_CLOSE_PAIRS.get(c);
            IndexRange sel = codeArea.getSelection();

            if (sel.getLength() > 0) {
                // Wrap selection: (selected)
                String selected = codeArea.getSelectedText();
                codeArea.replaceSelection(c + selected + closing);
                codeArea.moveTo(sel.getStart() + selected.length() + 1);
            } else {
                // Insert pair and place cursor between: (|)
                int caret = codeArea.getCaretPosition();
                codeArea.insertText(caret, c + String.valueOf(closing));
                codeArea.moveTo(caret + 1);
            }
            e.consume();
            Platform.runLater(this::updateCompletionPopup);
            if (onContentChanged != null) Platform.runLater(() -> { onContentChanged.run(); scanDocumentForWords(); if (minimap != null) minimap.update(); });
            return;
        }

        Platform.runLater(this::updateCompletionPopup);
        if (onContentChanged != null) {
            Platform.runLater(() -> { onContentChanged.run(); scanDocumentForWords(); if (minimap != null) minimap.update(); });
        }
    }

    // ---------------------------------------------------------------- AUTOCOMPLETE POPUP

    private void setupCompletionPopup() {
        // Setup completion list
        completionList.setPrefWidth(350);
        completionList.setPrefHeight(250);
        completionList.getStyleClass().add("completion-list");
        completionList.setCellFactory(lv -> new javafx.scene.control.ListCell<CompletionProvider.Suggestion>() {
            private final Label tagLabel = new Label();
            private final Label nameLabel = new Label();
            private final javafx.scene.layout.HBox content = new javafx.scene.layout.HBox(6);

            {
                content.setPadding(new javafx.geometry.Insets(4, 8, 4, 8));
                tagLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #c678dd; -fx-font-weight: bold;");
                nameLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
                content.getChildren().addAll(tagLabel, nameLabel);
            }

            @Override
            protected void updateItem(CompletionProvider.Suggestion item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    tagLabel.setText(tagFor(item.category));
                    tagLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: " + tagColor(item.category) + "; -fx-font-weight: bold;");
                    nameLabel.setText(item.label);
                    setGraphic(content);
                    setText(null);
                }
            }
        });
        
        // Setup details panel
        completionDetailsPanel.setPrefWidth(300);
        completionDetailsPanel.setPrefHeight(250);
        completionDetailsPanel.setStyle("-fx-background-color: #1e1e1e; -fx-border-color: #444; -fx-border-width: 0 0 0 1; -fx-padding: 10;");
        completionDetailsPanel.setSpacing(4);
        
        completionDetailLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #61afef;");
        completionDetailDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #abb2bf; -fx-wrap-text: true; -fx-padding: 0 0 6 0;");
        completionDetailCode.setStyle("-fx-font-size: 11px; -fx-font-family: 'Consolas','Courier New',monospace; -fx-text-fill: #98c379; -fx-wrap-text: true; -fx-background-color: #2d2d2d; -fx-padding: 6; -fx-background-radius: 3; -fx-border-color: #3a3a3a; -fx-border-radius: 3;");
        completionDetailParams.setStyle("-fx-font-size: 11px; -fx-text-fill: #e5c07b; -fx-wrap-text: true;");
        completionDetailReturn.setStyle("-fx-font-size: 11px; -fx-text-fill: #98c379; -fx-wrap-text: true;");
        
        completionDetailsPanel.getChildren().addAll(
            completionDetailLabel,
            completionDetailDesc,
            completionDetailCode,
            completionDetailParams,
            completionDetailReturn
        );
        
        // Hide details initially
        completionDetailCode.setVisible(false);
        completionDetailParams.setVisible(false);
        completionDetailReturn.setVisible(false);
        
        // Layout: list on left, details on right
        javafx.scene.layout.HBox popupContent = new javafx.scene.layout.HBox(completionList, completionDetailsPanel);
        popupContent.setStyle("-fx-background-color: #1e1e1e; -fx-border-color: #444; -fx-border-width: 1; -fx-border-radius: 4;");
        
        completionList.setOnMouseClicked(e -> applySelectedCompletion());
        completionList.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                updateCompletionDetails(newVal);
            }
        });

        completionPopup.getContent().add(popupContent);
        completionPopup.setAutoHide(true);
    }
    
    private void updateCompletionDetails(CompletionProvider.Suggestion item) {
        completionDetailLabel.setText(item.label);
        String desc = item.description;
        if (desc == null || desc.isEmpty()) {
            if ("snippet".equals(item.category)) desc = "Code snippet";
            else if ("tag".equals(item.category)) desc = "HTML element";
            else if ("keyword".equals(item.category)) desc = "Language keyword";
            else if ("property".equals(item.category)) desc = "CSS property";
            else if ("value".equals(item.category)) desc = "CSS value";
            else desc = "Code completion";
        }
        completionDetailDesc.setText(desc);
        
        // Show insert text as code preview
        if (item.insertText != null && !item.insertText.isEmpty() && !item.insertText.equals(item.label)) {
            String preview = item.insertText.length() > 80 ? item.insertText.substring(0, 77) + "..." : item.insertText;
            preview = preview.replace("\n", "\\n").replace("\r", "").replace("\t", "  ");
            completionDetailCode.setText("  " + preview);
            completionDetailCode.setVisible(true);
        } else {
            completionDetailCode.setVisible(false);
        }
        
        if (item.params != null && !item.params.isEmpty()) {
            completionDetailParams.setText("Params: " + String.join(", ", item.params));
            completionDetailParams.setVisible(true);
        } else {
            completionDetailParams.setVisible(false);
        }
        
        if (item.returnType != null && !item.returnType.isEmpty()) {
            completionDetailReturn.setText("Returns: " + item.returnType);
            completionDetailReturn.setVisible(true);
        } else {
            completionDetailReturn.setVisible(false);
        }
    }

    private String tagFor(String category) {
        if ("tag".equals(category)) return "[tag]";
        if ("keyword".equals(category)) return "[kw]";
        if ("property".equals(category)) return "[prop]";
        if ("snippet".equals(category)) return "[snip]";
        return "[?]";
    }

    private String tagColor(String category) {
        if ("tag".equals(category)) return "#e06c75";
        if ("keyword".equals(category)) return "#c678dd";
        if ("property".equals(category)) return "#e5c07b";
        if ("snippet".equals(category)) return "#61afef";
        return "#abb2bf";
    }

    private void updateCompletionPopup() {
        // Check for context-based triggers first
        int caret = codeArea.getCaretPosition();
        String textBefore = codeArea.getText().substring(0, Math.min(caret, codeArea.getLength()));

        // 1. HTML tag completion: after < or </
        if (language == LanguageType.HTML || language == LanguageType.JSX || language == LanguageType.TSX
                || language == LanguageType.VUE || language == LanguageType.SVELTE) {
            Matcher tagMatcher = HTML_TAG_PREFIX.matcher(textBefore);
            if (tagMatcher.find()) {
                String tagPrefix = tagMatcher.group(1);
                boolean isClosing = tagPrefix.startsWith("/");
                String search = isClosing ? tagPrefix.substring(1) : tagPrefix;
                List<CompletionProvider.Suggestion> tags = CompletionProvider.getSuggestionsByExactPrefix(language, search, "tag");
                if (!tags.isEmpty()) {
                    completionList.getItems().setAll(tags);
                    completionList.getSelectionModel().selectFirst();
                    showCompletionPopupAtCaret();
                    return;
                }
            }
        }

        // 2. CSS @-rule completion
        if (language == LanguageType.CSS || language == LanguageType.SCSS || language == LanguageType.LESS) {
            Matcher atMatcher = CSS_ATTR_PREFIX.matcher(textBefore);
            if (atMatcher.find()) {
                String search = atMatcher.group(1);
                List<CompletionProvider.Suggestion> rules = CompletionProvider.getSuggestionsByExactPrefix(language, search, "snippet");
                if (!rules.isEmpty()) {
                    completionList.getItems().setAll(rules);
                    completionList.getSelectionModel().selectFirst();
                    showCompletionPopupAtCaret();
                    return;
                }
            }
        }

        // 3. Standard word prefix completion
        String prefix = currentWordPrefix();
        if (prefix.isEmpty()) {
            hideCompletion();
            return;
        }
        List<CompletionProvider.Suggestion> suggestions = new java.util.ArrayList<>(
            CompletionProvider.getSuggestions(language, prefix));

        // 4. Merge LSP completions on top
        if (lspIntegration != null && lspIntegration.isActive()) {
            int line = 0, col = 0;
            try {
                String txtB4 = codeArea.getText().substring(0, caret);
                line = txtB4.isEmpty() ? 0 : txtB4.split("\n").length - 1;
                String lastLine = txtB4.isEmpty() ? "" :
                    txtB4.substring(txtB4.lastIndexOf('\n') + 1);
                col = lastLine.length();
            } catch (Exception ignored) {}
            List<CompletionProvider.Suggestion> lspSugs = lspIntegration.getCompletions(line, col, prefix);
            if (!lspSugs.isEmpty()) {
                // Add LSP suggestions first (they're more accurate)
                suggestions.addAll(0, lspSugs);
            }
        }

        if (suggestions.isEmpty()) {
            hideCompletion();
            return;
        }
        completionList.getItems().setAll(suggestions);
        completionList.getSelectionModel().selectFirst();
        showCompletionPopupAtCaret();
    }

    private String currentWordPrefix() {
        int caret = codeArea.getCaretPosition();
        String text = codeArea.getText().substring(0, caret);
        Matcher m = WORD_BOUNDARY.matcher(text);
        if (m.find()) {
            return m.group();
        }
        return "";
    }

    private void showCompletionPopupAtCaret() {
        codeArea.getCaretBounds().ifPresent(bounds -> {
            Point2D screenPos = new Point2D(bounds.getMinX(), bounds.getMaxY());
            if (!completionPopup.isShowing()) {
                completionPopup.show(codeArea, screenPos.getX(), screenPos.getY() + 4);
            } else {
                completionPopup.setX(screenPos.getX());
                completionPopup.setY(screenPos.getY() + 4);
            }
        });
    }

    private void moveCompletionSelection(int delta) {
        int idx = completionList.getSelectionModel().getSelectedIndex();
        int newIdx = Math.max(0, Math.min(completionList.getItems().size() - 1, idx + delta));
        completionList.getSelectionModel().select(newIdx);
        completionList.scrollTo(newIdx);
    }

    private void applySelectedCompletion() {
        CompletionProvider.Suggestion selected = completionList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            hideCompletion();
            return;
        }
        String prefix = currentWordPrefix();
        int caret = codeArea.getCaretPosition();
        int start = caret - prefix.length();

        String insertText = selected.insertText;
        int cursorOffset = insertText.indexOf("{CURSOR}");
        String finalText = insertText.replace("{CURSOR}", "");

        codeArea.replaceText(start, caret, finalText);

        if (cursorOffset >= 0) {
            codeArea.moveTo(start + cursorOffset);
        } else {
            codeArea.moveTo(start + finalText.length());
        }
        hideCompletion();
    }

    private void hideCompletion() {
        if (completionPopup.isShowing()) {
            completionPopup.hide();
        }
    }

    // ---------------------------------------------------------------- SIGNATURE HELP

    private void setupSignatureHelp() {
        codeArea.addEventFilter(KeyEvent.KEY_TYPED, e -> {
            if (signatureHelp.isShowing() && e.getCharacter().equals("\t")) {
                signatureHelp.nextSignature();
                e.consume();
                return;
            }
            if (e.getCharacter().equals("(")) {
                showSignatureHelp();
            } else if (e.getCharacter().equals(")") || e.getCharacter().equals(";") || e.getCharacter().equals("\n")) {
                signatureHelp.hide();
            }
        });
        codeArea.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (signatureHelp.isShowing()) {
                if (e.getCode() == KeyCode.ENTER) {
                    signatureHelp.hide();
                } else if (e.getCode() == KeyCode.ESCAPE) {
                    signatureHelp.hide();
                }
            }
        });
    }

    private void showSignatureHelp() {
        int caret = codeArea.getCaretPosition();
        if (caret < 2) return;
        String text = codeArea.getText();
        int start = Math.max(0, caret - 80);
        String before = text.substring(start, Math.min(caret, text.length()));

        Matcher m = Pattern.compile("([\\w.]+)\\s*\\(\\s*$").matcher(before);
        if (m.find()) {
            String funcName = m.group(1);
            CompletionProvider.Signature[] sigs = CompletionProvider.getSignatures(language, funcName);
            if (sigs != null && sigs.length > 0) {
                codeArea.getCaretBounds().ifPresent(bounds -> {
                    javafx.geometry.Point2D pos = new javafx.geometry.Point2D(bounds.getMinX(), bounds.getMaxY());
                    signatureHelp.show(sigs, pos);
                });
            }
        } else {
            // Check for method calls like "obj.method("
            Matcher m2 = Pattern.compile("\\.([\\w]+)\\s*\\(\\s*$").matcher(before);
            if (m2.find()) {
                String fullName = m2.group(0).replace("(", "").trim();
                CompletionProvider.Signature[] sigs = CompletionProvider.getSignatures(language, fullName);
                if (sigs != null && sigs.length > 0) {
                    codeArea.getCaretBounds().ifPresent(bounds -> {
                        javafx.geometry.Point2D pos = new javafx.geometry.Point2D(bounds.getMinX(), bounds.getMaxY());
                        signatureHelp.show(sigs, pos);
                    });
                }
            }
        }
    }

    public SignatureHelpPopup getSignatureHelp() {
        return signatureHelp;
    }

    // ---------------------------------------------------------------- HOVER TOOLTIP

    private void setupHover() {
        hoverTooltip = new Tooltip();
        hoverTooltip.setWrapText(true);
        hoverTooltip.setMaxWidth(450);
        hoverTooltip.setStyle("-fx-font-size: 11px; -fx-background-color: #1e1e1e; -fx-text-fill: #abb2bf; -fx-border-color: #444;");

        codeArea.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED, e -> {
            if (signatureHelp.isShowing()) return;
            try {
                String word = wordAtScreenPos(e.getScreenX(), e.getScreenY());
                if (word != null && !word.isEmpty()) {
                    String hoverText = CompletionProvider.getHoverText(language, word);
                    // Try LSP hover if static hover is empty
                    if (hoverText == null && lspIntegration != null && lspIntegration.isActive()) {
                        try {
                            javafx.geometry.Point2D local = codeArea.screenToLocal(e.getScreenX(), e.getScreenY());
                            if (local != null) {
                                int lineIdx = (int) (local.getY() / 18.0);
                                String lineText = lineIdx >= 0 && lineIdx < codeArea.getParagraphs().size()
                                    ? codeArea.getParagraph(lineIdx).getText() : "";
                                int col = lineText.isEmpty() ? 0 : Math.max(0, (int) ((local.getX() - 10) / 7.5));
                                col = Math.min(col, lineText.length());
                                hoverText = lspIntegration.getHoverText(lineIdx, col);
                            }
                        } catch (Exception ignored) {}
                    }
                    if (hoverText != null) {
                        // Format hover text with better styling
                        String formattedHover = formatHoverText(hoverText);
                        hoverTooltip.setText(formattedHover);
                        if (!hoverTooltip.isShowing()) {
                            hoverTooltip.show(codeArea, e.getScreenX() + 15, e.getScreenY() + 15);
                        }
                        return;
                    }
                }
                hoverTooltip.hide();
            } catch (Exception ex) {
                hoverTooltip.hide();
            }
        });

        codeArea.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_EXITED, e -> hoverTooltip.hide());
    }
    
    private String formatHoverText(String hoverText) {
        // Add formatting to hover text for better readability
        if (hoverText.contains("→")) {
            // It's a function signature
            int arrowIndex = hoverText.indexOf("→");
            String signature = hoverText.substring(0, arrowIndex).trim();
            String returnType = hoverText.substring(arrowIndex + 1).trim();
            return "Function:\n" + signature + "\n\nReturns: " + returnType;
        }
        return hoverText;
    }

    private String wordAtScreenPos(double screenX, double screenY) {
        try {
            javafx.geometry.Point2D local = codeArea.screenToLocal(screenX, screenY);
            if (local == null) return null;
            double lineHeight = 18.0;
            int lineIndex = (int) (local.getY() / lineHeight);
            if (lineIndex >= 0 && lineIndex < codeArea.getParagraphs().size()) {
                String lineText = codeArea.getParagraph(lineIndex).getText();
                if (lineText != null && !lineText.isEmpty()) {
                    int charOffset = Math.max(0, (int) ((local.getX() - 10) / 7.5));
                    if (charOffset >= 0 && charOffset < lineText.length()) {
                        return wordAtIndex(lineText, charOffset);
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String wordAtIndex(String text, int index) {
        if (text == null || index < 0 || index >= text.length()) return null;
        int start = index;
        int end = index;
        while (start > 0 && Character.isLetterOrDigit(text.charAt(start - 1))) start--;
        while (end < text.length() && Character.isLetterOrDigit(text.charAt(end))) end++;
        if (start < end) return text.substring(start, end);
        return null;
    }

    // ---------------------------------------------------------------- WORD-BASED COMPLETIONS

    private final java.util.Map<String, Integer> wordFrequency = new java.util.HashMap<>();

    public void scanDocumentForWords() {
        wordFrequency.clear();
        String text = codeArea.getText();
        String[] words = text.split("[^A-Za-z_$][^A-Za-z0-9_$]*");
        for (String w : words) {
            if (w.length() >= 3 && !isKeyword(w)) {
                wordFrequency.put(w, wordFrequency.getOrDefault(w, 0) + 1);
            }
        }
    }

    private boolean isKeyword(String w) {
        return "var|let|const|function|return|if|else|for|while|do|switch|case|break|continue|class|extends|import|export|default|null|undefined|true|false|this|typeof|instanceof|new|delete|void|try|catch|finally|throw|async|await|yield|in|of|public|private|protected|static|final|abstract|synchronized|native|int|long|double|float|boolean|char|byte|short|String|void".contains(w);
    }

    // ---------------------------------------------------------------- CONTEXT MENU

    private void setupContextMenu() {
        ContextMenu cm = new ContextMenu();
        cm.getStyleClass().add("editor-context-menu");

        MenuItem cutItem = new MenuItem("Cut");
        cutItem.setAccelerator(javafx.scene.input.KeyCombination.valueOf("Shortcut+X"));
        cutItem.setOnAction(e -> this.cut());

        MenuItem copyItem = new MenuItem("Copy");
        copyItem.setAccelerator(javafx.scene.input.KeyCombination.valueOf("Shortcut+C"));
        copyItem.setOnAction(e -> this.copy());

        MenuItem pasteItem = new MenuItem("Paste");
        pasteItem.setAccelerator(javafx.scene.input.KeyCombination.valueOf("Shortcut+V"));
        pasteItem.setOnAction(e -> this.paste());

        MenuItem findItem = new MenuItem("Find / Replace");
        findItem.setAccelerator(javafx.scene.input.KeyCombination.valueOf("Shortcut+F"));
        findItem.setOnAction(e -> showFindReplace());

        MenuItem selectAllItem = new MenuItem("Select All");
        selectAllItem.setOnAction(e -> this.selectAll());

        cm.getItems().addAll(cutItem, copyItem, pasteItem, new SeparatorMenuItem(), findItem, selectAllItem);

        cm.getItems().add(new SeparatorMenuItem());

        MenuItem formatItem = new MenuItem("Format Code");
        formatItem.setOnAction(e -> formatCurrentCode());
        MenuItem duplicateLineItem = new MenuItem("Duplicate Line");
        duplicateLineItem.setAccelerator(KeyCombination.valueOf("Shortcut+D"));
        duplicateLineItem.setOnAction(e -> duplicateCurrentLine());
        MenuItem deleteLineItem = new MenuItem("Delete Line");
        deleteLineItem.setAccelerator(KeyCombination.valueOf("Shortcut+Shift+K"));
        deleteLineItem.setOnAction(e -> deleteCurrentLine());
        MenuItem toggleCommentItem = new MenuItem("Toggle Comment");
        toggleCommentItem.setAccelerator(KeyCombination.valueOf("Shortcut+/"));
        toggleCommentItem.setOnAction(e -> toggleCommentSelection());

        cm.getItems().addAll(formatItem, duplicateLineItem, deleteLineItem, toggleCommentItem);

        cm.getItems().add(new SeparatorMenuItem());
        MenuItem goToDefItem = new MenuItem("Go to Definition");
        goToDefItem.setAccelerator(KeyCombination.valueOf("Shortcut+Shift+D"));
        goToDefItem.setOnAction(e -> {
            if (onGoToDefinition != null) onGoToDefinition.run();
        });
        MenuItem findRefsItem = new MenuItem("Find References");
        findRefsItem.setAccelerator(KeyCombination.valueOf("Shortcut+Shift+F12"));
        findRefsItem.setOnAction(e -> {
            if (onFindReferences != null) onFindReferences.run();
        });
        MenuItem renameItem = new MenuItem("Rename Symbol");
        renameItem.setAccelerator(KeyCombination.valueOf("Shortcut+F2"));
        renameItem.setOnAction(e -> {
            if (onRenameSymbol != null) onRenameSymbol.run();
        });
        cm.getItems().addAll(goToDefItem, findRefsItem, renameItem);

        codeArea.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (cm.isShowing()) cm.hide();
        });

        codeArea.setOnContextMenuRequested(e -> cm.show(codeArea, e.getScreenX(), e.getScreenY()));
        monacoEditor.getWebView().setOnContextMenuRequested(e -> cm.show(monacoEditor.getWebView(), e.getScreenX(), e.getScreenY()));
    }

    /** Formats the current document using basic indentation rules per language. */
    public void formatCurrentCode() {
        if (editorMode == EditorMode.MONACO) {
            monacoEditor.formatCode();
            return;
        }
        String text = codeArea.getText();
        if (text.isEmpty()) return;
        String formatted = formatText(text, language);
        if (formatted != null && !formatted.equals(text)) {
            int caret = codeArea.getCaretPosition();
            codeArea.replaceText(formatted);
            codeArea.moveTo(Math.min(caret, formatted.length()));
        }
    }

    private static String formatText(String text, LanguageType lang) {
        switch (lang) {
            case HTML:
            case JAVASCRIPT:
            case TYPESCRIPT:
            case JSX:
            case TSX:
            case VUE:
            case SVELTE:
            case JAVA:
            case PHP:
            case CSS:
            case SCSS:
            case LESS:
            case SASS:
            case C:
            case CPP:
            case KOTLIN:
            case GO:
            case RUST:
                return basicIndentFormat(text);
            case PYTHON:
                return pythonIndentFormat(text);
            default:
                return text;
        }
    }

    private static String pythonIndentFormat(String text) {
        StringBuilder sb = new StringBuilder();
        int indent = 0;
        String[] lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            if (trimmed.isEmpty()) { sb.append("\n"); continue; }
            // Dedent for dedent-trigger keywords at start (after leading whitespace)
            String[] dedentWords = {"return", "pass", "break", "continue", "raise"};
            if (indent > 0) {
                boolean dedent = false;
                for (String w : dedentWords) {
                    if (trimmed.equals(w) || trimmed.startsWith(w + " ")) {
                        dedent = true; break;
                    }
                }
                if (trimmed.startsWith("elif ") || trimmed.startsWith("else:")
                        || trimmed.startsWith("except") || trimmed.startsWith("finally:")
                        || trimmed.startsWith("except:")) {
                    indent = Math.max(0, indent - 1);
                }
                if (dedent && i + 1 < lines.length && !lines[i + 1].trim().isEmpty()) {
                    String nextTrim = lines[i + 1].trim();
                    String[] nextStarts = {"def ", "class ", "if ", "for ", "while ", "try:", "with "};
                    boolean nextIsBlock = false;
                    for (String s : nextStarts) {
                        if (nextTrim.startsWith(s)) { nextIsBlock = true; break; }
                    }
                    if (!nextIsBlock) {
                        indent = Math.max(0, indent - 1);
                    }
                }
            }
            for (int j = 0; j < indent; j++) sb.append("    ");
            sb.append(trimmed).append("\n");
            // Indent after colon (block starters)
            if (trimmed.endsWith(":")
                    && !trimmed.startsWith("#")
                    && !trimmed.endsWith("\"\"\"")
                    && !trimmed.endsWith("'''")) {
                indent++;
            }
        }
        return sb.toString().trim();
    }

    private static String basicIndentFormat(String text) {
        StringBuilder sb = new StringBuilder();
        int indent = 0;
        String[] lines = text.split("\n", -1);
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) { sb.append("\n"); continue; }
            // Dedent for closing braces
            if (trimmed.startsWith("}") || trimmed.startsWith("]") || trimmed.startsWith(")")) {
                indent = Math.max(0, indent - 1);
            }
            if (indent > 0) {
                for (int i = 0; i < indent; i++) sb.append("    ");
            }
            sb.append(trimmed).append("\n");
            // Indent after opening braces
            if (trimmed.endsWith("{") || trimmed.endsWith("[") || trimmed.endsWith("(")) {
                indent++;
            }
        }
        return sb.toString().trim();
    }

    private void duplicateCurrentLine() {
        int caret = getCaretPosition();
        String text = getText();
        int lineStart = text.lastIndexOf('\n', caret - 1) + 1;
        int lineEnd = text.indexOf('\n', caret);
        String line = (lineEnd < 0) ? text.substring(lineStart) : text.substring(lineStart, lineEnd);
        insertText(lineEnd < 0 ? getLength() : lineEnd, "\n" + line);
    }

    private void deleteCurrentLine() {
        int caret = getCaretPosition();
        String text = getText();
        int lineStart = text.lastIndexOf('\n', caret - 1) + 1;
        int lineEnd = text.indexOf('\n', caret);
        if (lineEnd < 0) lineEnd = text.length();
        else lineEnd++;
        deleteText(lineStart, lineEnd);
        moveTo(Math.min(lineStart, getLength()));
    }

    private void toggleCommentSelection() {
        if (editorMode == EditorMode.MONACO) {
            monacoEditor.toggleComment();
            return;
        }
        String commentPrefix = "//";
        if (language == LanguageType.HTML || language == LanguageType.XML
                || language == LanguageType.JSX || language == LanguageType.TSX) {
            commentPrefix = "<!-- ";
        }
        IndexRange sel = codeArea.getSelection();
        if (sel.getLength() > 0) {
            String selected = codeArea.getSelectedText();
            if (selected.trim().startsWith(commentPrefix.trim())) {
                String uncommented = selected.replaceAll("(?m)^\\s*" + commentPrefix.replace("?", "\\?").replace("*", "\\*") + " ?", "");
                codeArea.replaceSelection(uncommented);
            } else {
                codeArea.replaceSelection(commentPrefix + " " + selected);
            }
        } else {
            int caret = codeArea.getCaretPosition();
            String text = codeArea.getText();
            int lineStart = text.lastIndexOf('\n', caret - 1) + 1;
            int lineEnd = text.indexOf('\n', caret);
            if (lineEnd < 0) lineEnd = text.length();
            String line = text.substring(lineStart, lineEnd);
            String trimmed = line.trim();
            if (trimmed.startsWith(commentPrefix.trim())) {
                codeArea.replaceText(lineStart, lineEnd, line.replaceFirst("\\s*" + commentPrefix.replace("?", "\\?").replace("*", "\\*") + " ?", ""));
            } else {
                codeArea.insertText(lineStart, commentPrefix + " ");
            }
        }
    }

    // ---------------------------------------------------------------- CARET TRACKING / BRACKET MATCHING

    private static final String OPENERS = "([{";
    private static final String CLOSERS = ")]}";
    private int[] lastBracketHighlight; // [pos1, pos2] of last highlighted pair, or null

    private void setupCaretListener() {
        codeArea.caretPositionProperty().addListener((obs, old, val) -> {
            if (onCaretMoved != null) {
                onCaretMoved.accept(val);
            }
            highlightMatchingBracket(val);
        });
        codeArea.selectionProperty().addListener((obs, oldVal, newVal) -> {
            String selected = codeArea.getSelectedText();
            if (selected == null || selected.isEmpty() || selected.length() < 2 || !selected.matches("[\\w_]+")) {
                if (!wordHighlightRanges.isEmpty()) {
                    wordHighlightRanges.clear();
                    lastSelectedWord = "";
                    // Force re-apply to clear highlights, but only if we had any
                    if (oldVal != null && oldVal.getLength() > 0) {
                        Platform.runLater(this::applyHighlighting);
                    }
                }
            } else {
                highlightWord(selected);
            }
        });
    }

    private void highlightMatchingBracket(int caret) {
        clearBracketHighlight();
        String text = codeArea.getText();
        if (text.isEmpty()) return;

        int checkPos = -1;
        char c = '\0';
        if (caret < text.length() && (OPENERS.indexOf(text.charAt(caret)) >= 0 || CLOSERS.indexOf(text.charAt(caret)) >= 0)) {
            checkPos = caret;
            c = text.charAt(caret);
        } else if (caret > 0 && (OPENERS.indexOf(text.charAt(caret - 1)) >= 0 || CLOSERS.indexOf(text.charAt(caret - 1)) >= 0)) {
            checkPos = caret - 1;
            c = text.charAt(caret - 1);
        }
        if (checkPos < 0) return;

        int matchPos = findMatchingBracket(text, checkPos, c);
        if (matchPos >= 0) {
            lastBracketHighlight = new int[]{checkPos, matchPos};
            applyBracketStyleRaw(checkPos);
            applyBracketStyleRaw(matchPos);
        }
    }

    private int findMatchingBracket(String text, int pos, char bracket) {
        boolean forward = OPENERS.indexOf(bracket) >= 0;
        char target = forward ? CLOSERS.charAt(OPENERS.indexOf(bracket)) : OPENERS.charAt(CLOSERS.indexOf(bracket));
        int depth = 0;
        if (forward) {
            for (int i = pos; i < text.length(); i++) {
                char ch = text.charAt(i);
                if (ch == bracket) depth++;
                else if (ch == target) {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        } else {
            for (int i = pos; i >= 0; i--) {
                char ch = text.charAt(i);
                if (ch == bracket) depth++;
                else if (ch == target) {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        }
        return -1;
    }

    private void applyBracketStyleRaw(int pos) {
        if (pos < 0 || pos >= codeArea.getLength()) return;
        try {
            java.util.Collection<String> existing = new java.util.ArrayList<>(codeArea.getStyleOfChar(pos));
            if (!existing.contains("bracket-match")) existing.add("bracket-match");
            codeArea.setStyle(pos, pos + 1, existing);
        } catch (Exception ignored) { }
    }

    // ---------------------------------------------------------------- WORD HIGHLIGHT

    private void highlightWord(String word) {
        if (word.equals(lastSelectedWord)) return;
        wordHighlightRanges.clear();
        lastSelectedWord = word;

        String text = codeArea.getText();
        javafx.scene.control.IndexRange sel = codeArea.getSelection();
        int idx = 0;
        while ((idx = text.indexOf(word, idx)) >= 0) {
            if (idx < sel.getStart() || idx >= sel.getEnd()) {
                wordHighlightRanges.add(new int[]{idx, idx + word.length()});
            }
            idx += word.length();
        }
        if (!wordHighlightRanges.isEmpty()) {
            applyWordHighlights();
        }
    }

    private void applyWordHighlights() {
        for (int[] range : wordHighlightRanges) {
            try {
                java.util.Collection<String> existing = new java.util.ArrayList<>(codeArea.getStyleOfChar(range[0]));
                if (!existing.contains("word-highlight")) existing.add("word-highlight");
                codeArea.setStyle(range[0], range[1], existing);
            } catch (Exception ignored) { }
        }
    }

    // ---------------------------------------------------------------- SEARCH HIGHLIGHT

    public void setSearchHighlight(String query) {
        searchHighlightRanges.clear();
        searchHighlightQuery = query;
        if (query == null || query.isEmpty()) return;

        String text = codeArea.getText();
        int idx = 0;
        while ((idx = text.indexOf(query, idx)) >= 0) {
            searchHighlightRanges.add(new int[]{idx, idx + query.length()});
            idx += query.length();
        }
        if (!searchHighlightRanges.isEmpty()) {
            applySearchHighlights();
        }
    }

    public void clearSearchHighlight() {
        searchHighlightRanges.clear();
        searchHighlightQuery = null;
        applyHighlighting();
        if (!wordHighlightRanges.isEmpty()) {
            applyWordHighlights();
        }
    }

    private void applySearchHighlights() {
        for (int[] range : searchHighlightRanges) {
            try {
                java.util.Collection<String> existing = new java.util.ArrayList<>(codeArea.getStyleOfChar(range[0]));
                if (!existing.contains("search-highlight")) existing.add("search-highlight");
                codeArea.setStyle(range[0], range[1], existing);
            } catch (Exception ignored) { }
        }
    }

    private void clearBracketHighlight() {
        if (lastBracketHighlight == null) return;
        for (int pos : lastBracketHighlight) {
            try {
                java.util.Collection<String> existing = new java.util.ArrayList<>(codeArea.getStyleOfChar(pos));
                existing.remove("bracket-match");
                codeArea.setStyle(pos, pos + 1, existing);
            } catch (Exception ignored) { }
        }
        lastBracketHighlight = null;
    }

    public void setOnContentChanged(Runnable callback) {
        this.onContentChanged = callback;
    }

    public void setOnCaretMoved(Consumer<Integer> callback) {
        this.onCaretMoved = callback;
    }

    // ---------------------------------------------------------------- EDITOR MODE TOGGLE

    public void toggleEditorMode() {
        setEditorMode(editorMode == EditorMode.RICHTEXT ? EditorMode.MONACO : EditorMode.RICHTEXT);
    }

    public void setEditorMode(EditorMode mode) {
        if (mode == editorMode) return;
        
        if (mode == EditorMode.MONACO) {
            currentText = codeArea.getText();
        } else {
            if (editorMode == EditorMode.MONACO && monacoEditor.isReady()) {
                currentText = monacoEditor.getText();
            }
        }
        
        editorMode = mode;
        if (mode == EditorMode.MONACO) {
            wrapper.setVisible(false);
            monacoEditor.setVisible(true);

            monacoEditor.setText(currentText);
            monacoEditor.setFontSize(currentFontSize);
            monacoEditor.applyAllSettings();
            monacoEditor.setBreakpoints(new ArrayList<>(breakpointLines));

            if (onContentChanged != null) {
                Platform.runLater(onContentChanged);
            }
            Platform.runLater(() -> monacoEditor.focus());
        } else {
            wrapper.setVisible(true);
            monacoEditor.setVisible(false);
            
            Platform.runLater(() -> {
                internalTextChange = true;
                codeArea.clear();
                codeArea.appendText(currentText);
                internalTextChange = false;
                codeArea.setStyle("-fx-font-size: " + currentFontSize + "px;");
                if (!currentText.isEmpty()) {
                    codeArea.setStyleSpans(0, SyntaxHighlighter.computeHighlighting(currentText, language));
                }
            });
            
            if (onContentChanged != null) {
                Platform.runLater(onContentChanged);
            }
            
            Platform.runLater(() -> codeArea.requestFocus());
        }
    }

    public EditorMode getEditorMode() { return editorMode; }
    public MonacoEditor getMonacoEditor() { return monacoEditor; }

    // ---------------------------------------------------------------- MODE-AWARE DELEGATE METHODS

    @Override
    public void requestFocus() {
        if (editorMode == EditorMode.MONACO) {
            monacoEditor.focus();
        } else {
            codeArea.requestFocus();
        }
    }

    public void undo() {
        if (editorMode == EditorMode.MONACO) { monacoEditor.undo(); }
        else { codeArea.undo(); }
    }

    public void redo() {
        if (editorMode == EditorMode.MONACO) { monacoEditor.redo(); }
        else { codeArea.redo(); }
    }

    public void cut() {
        if (editorMode == EditorMode.MONACO) { monacoEditor.cut(); }
        else { codeArea.cut(); }
    }

    public void copy() {
        if (editorMode == EditorMode.MONACO) { monacoEditor.copy(); }
        else { codeArea.copy(); }
    }

    public void paste() {
        if (editorMode == EditorMode.MONACO) { monacoEditor.paste(); }
        else { codeArea.paste(); }
    }

    public void selectAll() {
        if (editorMode == EditorMode.MONACO) { monacoEditor.selectAll(); }
        else { codeArea.selectAll(); }
    }

    /** Returns absolute caret position (0-based). Mode-aware. */
    public int getCaretPosition() {
        if (editorMode == EditorMode.MONACO) {
            int[] pos = monacoEditor.getCursorPosition();
            if (pos == null) return 0;
            return lineColToOffset(currentText, pos[0], pos[1]);
        }
        return codeArea.getCaretPosition();
    }

    public void moveTo(int pos) {
        if (editorMode == EditorMode.MONACO) {
            int[] lc = offsetToLineCol(currentText, pos);
            if (lc != null) {
                monacoEditor.goToLine(lc[0]);
            }
        } else {
            codeArea.moveTo(pos);
        }
    }

    public void moveTo(int line, int col) {
        if (editorMode == EditorMode.MONACO) {
            monacoEditor.goToLine(line + 1);
        } else {
            codeArea.moveTo(line, col);
        }
    }

    public void insertText(int pos, String text) {
        if (editorMode == EditorMode.MONACO) {
            int safePos = Math.min(pos, currentText.length());
            currentText = currentText.substring(0, safePos) + text + currentText.substring(safePos);
            textProp.set(currentText);
            monacoEditor.setText(currentText);
        } else {
            codeArea.insertText(pos, text);
        }
    }

    public void replaceText(int start, int end, String text) {
        if (editorMode == EditorMode.MONACO) {
            int safeStart = Math.min(start, currentText.length());
            int safeEnd = Math.min(end, currentText.length());
            currentText = currentText.substring(0, safeStart) + text + currentText.substring(safeEnd);
            textProp.set(currentText);
            monacoEditor.setText(currentText);
        } else {
            codeArea.replaceText(start, end, text);
        }
    }

    public void replaceText(String text) {
        currentText = text == null ? "" : text;
        textProp.set(currentText);
        if (editorMode == EditorMode.MONACO) {
            monacoEditor.setText(currentText);
        } else {
            codeArea.replaceText(currentText);
        }
    }

    public void deleteText(int start, int end) {
        if (editorMode == EditorMode.MONACO) {
            int safeStart = Math.min(start, currentText.length());
            int safeEnd = Math.min(end, currentText.length());
            currentText = currentText.substring(0, safeStart) + currentText.substring(safeEnd);
            textProp.set(currentText);
            monacoEditor.setText(currentText);
        } else {
            codeArea.deleteText(start, end);
        }
    }

    public void replaceSelection(String text) {
        if (editorMode == EditorMode.MONACO) {
            int[] pos = monacoEditor.getCursorPosition();
            int offset = lineColToOffset(currentText, pos[0], pos[1]);
            currentText = currentText.substring(0, offset) + text + currentText.substring(offset);
            textProp.set(currentText);
            monacoEditor.setText(currentText);
        } else {
            IndexRange sel = codeArea.getSelection();
            if (sel != null && sel.getLength() > 0) {
                currentText = currentText.substring(0, sel.getStart()) + text + currentText.substring(sel.getEnd());
            } else {
                int caret = codeArea.getCaretPosition();
                currentText = currentText.substring(0, caret) + text + currentText.substring(caret);
            }
            codeArea.replaceSelection(text);
        }
    }

    public void setWrapText(boolean wrap) {
        if (editorMode == EditorMode.MONACO) {
            monacoEditor.setWrapText(wrap);
        } else {
            codeArea.setWrapText(wrap);
        }
    }

    public void selectWord() {
        if (editorMode == EditorMode.MONACO) {
            if (monacoEditor.isReady()) monacoEditor.getEngine().executeScript("editor.trigger('keyboard','editor.action.selectWord');");
        } else {
            codeArea.selectWord();
        }
    }

    public void requestFollowCaret() {
        if (editorMode == EditorMode.MONACO) {
            // Monaco follows caret by default
        } else {
            codeArea.requestFollowCaret();
        }
    }

    public int getLength() {
        return currentText.length();
    }

    public String getText(int start, int end) {
        return currentText.substring(Math.min(start, currentText.length()), Math.min(end, currentText.length()));
    }

    /** Returns 0-based line/col for absolute offset */
    private int[] offsetToLineCol(String text, int offset) {
        if (text == null || text.isEmpty() || offset <= 0) return new int[]{1, 1};
        int line = 1;
        int col = 1;
        int limit = Math.min(offset, text.length());
        for (int i = 0; i < limit; i++) {
            if (text.charAt(i) == '\n') { line++; col = 1; }
            else { col++; }
        }
        return new int[]{line, col};
    }

    /** Returns absolute offset for 1-based line/col */
    private int lineColToOffset(String text, int line, int col) {
        if (text == null || text.isEmpty()) return 0;
        int currentLine = 1;
        int currentCol = 1;
        for (int i = 0; i < text.length(); i++) {
            if (currentLine == line && currentCol == col) return i;
            if (text.charAt(i) == '\n') { currentLine++; currentCol = 1; }
            else { currentCol++; }
        }
        return text.length();
    }

    // ---------------------------------------------------------------- PUBLIC API

    public String getText() {
        return currentText;
    }

    public String getSelectedText() {
        if (editorMode == EditorMode.MONACO) {
            return monacoEditor.getSelectedText();
        }
        IndexRange sel = codeArea.getSelection();
        if (sel == null || sel.getLength() == 0) return "";
        return currentText.substring(sel.getStart(), sel.getEnd());
    }

    public void setText(String text) {
        currentText = text == null ? "" : text;
        textProp.set(currentText);
        if (editorMode == EditorMode.MONACO) {
            monacoEditor.setText(currentText);
        } else {
            internalTextChange = true;
            codeArea.replaceText(currentText);
            internalTextChange = false;
            applyHighlighting();
        }
    }

    public CodeArea getCodeArea() {
        return codeArea;
    }

    private final javafx.beans.property.SimpleStringProperty textProp = new javafx.beans.property.SimpleStringProperty();

    public javafx.beans.value.ObservableValue<String> textProperty() {
        return textProp;
    }

    /** Computes 1-based line/column for status bar display. */
    public int[] getLineAndColumn() {
        if (editorMode == EditorMode.MONACO) {
            int[] pos = monacoEditor.getCursorPosition();
            return pos != null ? pos : new int[]{1, 1};
        }
        int caret = codeArea.getCaretPosition();
        String text = codeArea.getText();
        int line = 1, col = 1;
        for (int i = 0; i < caret && i < text.length(); i++) {
            if (text.charAt(i) == '\n') { line++; col = 1; } else { col++; }
        }
        return new int[]{line, col};
    }

    public void setFontSize(double size) {
        currentFontSize = size;
        if (editorMode == EditorMode.MONACO) {
            monacoEditor.setFontSize(size);
        } else {
            codeArea.setStyle("-fx-font-size: " + size + "px;");
        }
    }

    /**
     * يطبق كل الإعدادات المحفوظة في EditorSettings على المحرر ده —
     * سواء كان RichTextFX أو Monaco.
     * يُستدعى من EditorController.applySettingsToOpenEditors()
     * الآن يطبق الإعدادات على كلا المحررين بغض النظر عن الوضع الحالي
     */
    public void applySettings() {
        double  fontSize   = com.eagle.util.EditorSettings.getFontSize();
        String  fontFamily = com.eagle.util.EditorSettings.getFontFamily();
        boolean wordWrap   = com.eagle.util.EditorSettings.isWordWrap();

        currentFontSize = fontSize;

        // Apply to RichTextFX editor always
        applyRichTextTypography(fontFamily, fontSize);
        applySyntaxTheme();
        codeArea.setWrapText(wordWrap);
        codeArea.setParagraphGraphicFactory(
            com.eagle.util.EditorSettings.isShowLineNumbers()
                ? buildLineNumberAndBreakpointFactory()
                : buildBreakpointOnlyFactory()
        );

        // Apply to Monaco editor always (even if not currently visible)
        if (monacoEditor != null) {
            monacoEditor.applyAllSettings();
        }
    }

    /** Loads the syntax theme CSS into the CodeArea stylesheets */
    private void applySyntaxTheme() {
        String theme = com.eagle.util.EditorSettings.getSyntaxTheme();
        if (theme == null || "Default".equals(theme)) {
            codeArea.getStylesheets().removeIf(s -> s.contains("syntax-"));
            return;
        }
        String path = getSyntaxCssPath(theme);
        if (path == null) {
            codeArea.getStylesheets().removeIf(s -> s.contains("syntax-"));
            return;
        }
        java.net.URL url = getClass().getResource(path);
        if (url == null) return;
        String ext = url.toExternalForm();
        // Remove old syntax stylesheets (different from dynamic font CSS)
        codeArea.getStylesheets().removeIf(s -> s.contains("syntax-"));
        codeArea.getStylesheets().add(ext);
    }

    private String getSyntaxCssPath(String theme) {
        switch (theme) {
            case "Monokai":          return "/com/eagle/css/syntax-monokai.css";
            case "Dracula":          return "/com/eagle/css/syntax-dracula.css";
            case "Solarized":        return "/com/eagle/css/syntax-solarized-dark.css";
            case "GitHub Light":     return "/com/eagle/css/syntax-github-light.css";
            case "One Dark":         return "/com/eagle/css/syntax-one-dark.css";
            case "Nord":             return "/com/eagle/css/syntax-nord.css";
            case "GitHub Dark":      return "/com/eagle/css/syntax-github-dark.css";
            case "Atom One Light":   return "/com/eagle/css/syntax-atom-one-light.css";
            case "Tokyo Night":      return "/com/eagle/css/syntax-tokyo-night.css";
            case "Catppuccin":       return "/com/eagle/css/syntax-catppuccin.css";
            case "Ayu Dark":         return "/com/eagle/css/syntax-ayu-dark.css";
            case "SynthWave '84":    return "/com/eagle/css/syntax-synthwave.css";
            case "Noctis Lux":       return "/com/eagle/css/syntax-noctis-lux.css";
            case "Gruvbox Dark":     return "/com/eagle/css/syntax-gruvbox-dark.css";
            case "Gruvbox Light":    return "/com/eagle/css/syntax-gruvbox-light.css";
            case "Material Darker":  return "/com/eagle/css/syntax-material-darker.css";
            case "Material Lighter": return "/com/eagle/css/syntax-material-lighter.css";
            case "Material Ocean":   return "/com/eagle/css/syntax-material-ocean.css";
            case "Rose Pine":        return "/com/eagle/css/syntax-rose-pine.css";
            case "Rose Pine Moon":   return "/com/eagle/css/syntax-rose-pine-moon.css";
            case "Everforest Dark":  return "/com/eagle/css/syntax-everforest-dark.css";
            case "Everforest Light": return "/com/eagle/css/syntax-everforest-light.css";
            case "Night Owl":        return "/com/eagle/css/syntax-night-owl.css";
            case "Light Owl":        return "/com/eagle/css/syntax-light-owl.css";
            case "Palenight":        return "/com/eagle/css/syntax-palenight.css";
            case "Horizon":          return "/com/eagle/css/syntax-horizon.css";
            case "Panda":            return "/com/eagle/css/syntax-panda.css";
            case "Shades of Purple": return "/com/eagle/css/syntax-shades-purple.css";
            case "Monokai Pro":      return "/com/eagle/css/syntax-monokai-pro.css";
            case "Ayu Light":        return "/com/eagle/css/syntax-ayu-light.css";
            case "Ayu Mirage":       return "/com/eagle/css/syntax-ayu-mirage.css";
            case "VSCode Dark+":     return "/com/eagle/css/syntax-vscode-dark.css";
            case "VSCode Light+":    return "/com/eagle/css/syntax-vscode-light.css";
            default:                 return null;
        }
    }

    /** يكتب ملف CSS ويضيفه لـ CodeArea stylesheets (node-level > scene-level) */
    private void applyRichTextTypography(String fontFamily, double fontSize) {
        if (fontFamily == null || fontFamily.isEmpty()) fontFamily = "Consolas";
        double lineHeight = com.eagle.util.EditorSettings.getLineHeight();
        // هرب double quotes في اسم الخط
        String family = fontFamily.replace("\"", "\\\"");
        double linenoSize = Math.max(10, fontSize - 1.5);
        String css =
            ".code-area { -fx-font-family: \"" + family + "\"; -fx-font-size: " + fontSize + "px; }\n" +
            ".code-area .text { -fx-font-family: \"" + family + "\"; -fx-font-size: " + fontSize + "px; }\n" +
            ".code-area .lineno { -fx-font-family: \"" + family + "\"; -fx-font-size: " + linenoSize + "px; }\n" +
            ".code-area .paragraph-box { -fx-line-spacing: " + ((lineHeight - 1.0) * fontSize) + "px; }\n";
        try {
            java.io.File f = new java.io.File(
                System.getProperty("java.io.tmpdir"),
                "webide-editor-" + System.identityHashCode(this) + ".css"
            );
            java.nio.file.Files.write(f.toPath(), css.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String newUrl = f.toURI().toURL().toExternalForm();
            if (dynamicCssUrl != null) {
                codeArea.getStylesheets().remove(dynamicCssUrl);
            }
            codeArea.getStylesheets().add(newUrl);
            dynamicCssUrl = newUrl;
        } catch (Exception ignored) {}
    }

    /** Gutter بدون أرقام أسطر — لما Show Line Numbers = off */
    private IntFunction<Node> buildBreakpointOnlyFactory() {
        return lineIndex -> {
            StackPane dot = new StackPane();
            dot.setPrefSize(10, 10);
            dot.getStyleClass().add("breakpoint-dot");
            updateDotVisual(dot, lineIndex);
            dot.setOnMouseClicked((MouseEvent e) -> {
                toggleBreakpoint(lineIndex);
                updateDotVisual(dot, lineIndex);
            });
            javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(4, dot);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setStyle("-fx-padding: 0 4 0 4;");
            return row;
        };
    }

    public double getFontSize() {
        return currentFontSize;
    }

    public LanguageType getLanguage() {
        return language;
    }

    public void showFindReplace() {
        findReplacePanel.showPanel();
    }

    public void hideColorSwatches() {
        colorSwatchesActive = false;
        for (javafx.scene.shape.Rectangle r : activeSwatches) {
            Popup p = (Popup) r.getProperties().get("popup");
            if (p != null) p.hide();
        }
        activeSwatches.clear();
    }

    public void activateColorSwatches() {
        colorSwatchesActive = true;
        refreshColorSwatches();
    }
}