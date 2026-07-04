package com.eagle.editor;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Enhanced JS debugger panel: captures console.log/warn/error and runtime
 * exceptions from a WebView's WebEngine via a Java bridge object, supports
 * breakpoints, step controls, watch expressions, call stack, and variable
 * inspection. Reports errors with line numbers for inline editor display.
 */
public class DebuggerPanel extends BorderPane {

    private final ListView<String> consoleList = new ListView<>();
    private final TextField evalField = new TextField();
    private final Set<Integer> breakpoints = new LinkedHashSet<>();
    private WebEngine engine;
    private Consumer<String> onErrorReported;

    private final Button continueBtn = new Button("Continue");
    private final Button stepOverBtn = new Button("Step Over");
    private final Button stepIntoBtn = new Button("Step Into");
    private final Button stepOutBtn = new Button("Step Out");
    private final Button pauseBtn = new Button("Pause");
    private final Button stopBtn = new Button("Stop");

    private final ListView<String> watchList = new ListView<>();
    private final TextField watchField = new TextField();
    private final java.util.List<String> watchExpressions = new java.util.ArrayList<>();

    private final ListView<String> callStackList = new ListView<>();

    private final TreeView<String> variablesTree = new TreeView<>();

    public DebuggerPanel() {
        getStyleClass().add("debugger-panel");

        HBox stepBar = new HBox(4);
        stepBar.setPadding(new Insets(4, 8, 4, 8));
        stepBar.setStyle("-fx-background-color: -bg-secondary; -fx-border-color: -border-color; -fx-border-width: 0 0 1 0;");

        continueBtn.setTooltip(new Tooltip("Continue execution (F5)"));
        stepOverBtn.setTooltip(new Tooltip("Step over (F10)"));
        stepIntoBtn.setTooltip(new Tooltip("Step into (F11)"));
        stepOutBtn.setTooltip(new Tooltip("Step out (Shift+F11)"));
        pauseBtn.setTooltip(new Tooltip("Pause execution"));
        stopBtn.setTooltip(new Tooltip("Stop execution"));

        for (Button b : new Button[]{continueBtn, stepOverBtn, stepIntoBtn, stepOutBtn, pauseBtn, stopBtn}) {
            b.setStyle("-fx-font-size: 10px;");
            b.setDisable(true);
        }

        stepBar.getChildren().addAll(continueBtn, stepOverBtn, stepIntoBtn, stepOutBtn, pauseBtn, stopBtn);
        Label bpLabel = new Label("BP: 0");
        bpLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-muted;");
        stepBar.getChildren().add(bpLabel);

        VBox watchPanel = new VBox(4);
        watchPanel.setPadding(new Insets(4, 8, 4, 8));
        HBox watchBar = new HBox(4);
        watchField.setPromptText("Add watch expression...");
        watchField.setPrefWidth(200);
        Button addWatchBtn = new Button("+");
        addWatchBtn.setStyle("-fx-font-size: 10px;");
        addWatchBtn.setOnAction(e -> addWatch());
        watchField.setOnAction(e -> addWatch());
        Button clearWatchesBtn = new Button("Clear");
        clearWatchesBtn.setStyle("-fx-font-size: 10px;");
        clearWatchesBtn.setOnAction(e -> { watchExpressions.clear(); refreshWatches(); });
        watchBar.getChildren().addAll(watchField, addWatchBtn, clearWatchesBtn);
        HBox.setHgrow(watchField, Priority.ALWAYS);
        watchList.setPrefHeight(100);
        watchList.setOnMouseClicked(e -> {
            int idx = watchList.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < watchExpressions.size()) {
                watchExpressions.remove(idx);
                refreshWatches();
            }
        });
        Label watchHeader = new Label("WATCH");
        watchHeader.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: -text-muted;");
        watchPanel.getChildren().addAll(watchHeader, watchBar, watchList);

        VBox callStackPanel = new VBox(4);
        callStackPanel.setPadding(new Insets(4, 8, 4, 8));
        Label csHeader = new Label("CALL STACK");
        csHeader.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: -text-muted;");
        callStackList.setPrefHeight(80);
        callStackPanel.getChildren().addAll(csHeader, callStackList);

        VBox varsPanel = new VBox(4);
        varsPanel.setPadding(new Insets(4, 8, 4, 8));
        Label varsHeader = new Label("VARIABLES");
        varsHeader.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: -text-muted;");
        variablesTree.setPrefHeight(120);
        varsPanel.getChildren().addAll(varsHeader, variablesTree);

        VBox sidePanel = new VBox(4);
        sidePanel.getChildren().addAll(watchPanel, callStackPanel, varsPanel);
        VBox.setVgrow(callStackList, Priority.ALWAYS);

        SplitPane centerSplit = new SplitPane();
        centerSplit.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
        centerSplit.getItems().addAll(consoleList, sidePanel);
        centerSplit.setDividerPositions(0.6);

        Label header = new Label("CONSOLE");
        header.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        header.setPadding(new Insets(8, 12, 4, 12));

        consoleList.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.startsWith("[error]")) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else if (item.startsWith("[warn]")) {
                        setStyle("-fx-text-fill: #f39c12;");
                    } else if (item.startsWith("[result]")) {
                        setStyle("-fx-text-fill: #2ecc71;");
                    } else if (item.startsWith("[info]")) {
                        setStyle("-fx-text-fill: #3498db;");
                    } else if (item.startsWith("> ")) {
                        setStyle("-fx-text-fill: #9b59b6; -fx-font-style: italic;");
                    } else if (item.startsWith("[stack]")) {
                        setStyle("-fx-text-fill: #8be9fd; -fx-font-size: 10px;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        HBox evalBar = new HBox(6);
        evalBar.setPadding(new Insets(4, 8, 6, 8));
        Button clearBtn = new Button("Clear");
        clearBtn.setOnAction(e -> consoleList.getItems().clear());
        Button runBtn = new Button("Run");
        runBtn.setOnAction(e -> evaluateCurrentInput());
        evalField.setPromptText("Type a JS expression and press Enter to evaluate...");
        Label bpCount = new Label("BP: 0");
        bpCount.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-muted;");
        evalBar.getChildren().addAll(evalField, runBtn, clearBtn, bpCount);
        HBox.setHgrow(evalField, Priority.ALWAYS);
        evalField.setOnAction(e -> evaluateCurrentInput());

        VBox top = new VBox(0, header, stepBar);
        setTop(top);
        setCenter(centerSplit);
        setBottom(evalBar);

        continueBtn.setOnAction(e -> {
            if (engine != null) try { engine.executeScript("if(typeof __resume__ === 'function') __resume__();"); } catch (Exception ex) { /* ignore */ }
        });
        stopBtn.setOnAction(e -> {
            if (engine != null) try { engine.executeScript("if(typeof __stop__ === 'function') __stop__();"); } catch (Exception ex) { /* ignore */ }
        });
    }

    public void setOnErrorReported(Consumer<String> callback) {
        this.onErrorReported = callback;
    }

    public void attach(WebEngine engine) {
        this.engine = engine;
        engine.setOnError(event -> {
            String msg = event.getMessage();
            appendLine("[error] " + msg);
            if (onErrorReported != null) onErrorReported.accept(msg);
        });

        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == javafx.concurrent.Worker.State.SUCCEEDED) {
                injectConsoleBridge();
                enableStepControls(true);
            }
        });
    }

    public void enableStepControls(boolean enabled) {
        for (Button b : new Button[]{continueBtn, stepOverBtn, stepIntoBtn, stepOutBtn, pauseBtn, stopBtn}) {
            b.setDisable(!enabled);
        }
    }

    private void injectConsoleBridge() {
        try {
            JSObject window = (JSObject) engine.executeScript("window");
            window.setMember("javaConsoleBridge", new ConsoleBridge());
            engine.executeScript(
                    "(function() {" +
                    "  var origLog = console.log, origWarn = console.warn, origErr = console.error;" +
                    "  console.log = function() { javaConsoleBridge.log(Array.prototype.slice.call(arguments).join(' ')); origLog.apply(console, arguments); };" +
                    "  console.warn = function() { javaConsoleBridge.warn(Array.prototype.slice.call(arguments).join(' ')); origWarn.apply(console, arguments); };" +
                    "  console.error = function() { javaConsoleBridge.error(Array.prototype.slice.call(arguments).join(' ')); origErr.apply(console, arguments); };" +
                    "  window.onerror = function(msg, url, line, col, err) { javaConsoleBridge.error(msg + ' (line ' + line + ', col ' + col + ')'); return false; };" +
                    "  window.__debugStack = [];" +
                    "  var origFn = window.Function;" +
                    "  window.Function = function() { __debugStack.push('Function() called'); javaConsoleBridge.stackTrace('Function() called'); return origFn.apply(this, arguments); };" +
                    "})();"
            );
        } catch (Exception e) {
            appendLine("[warn] Debugger bridge unavailable: " + e.getMessage());
        }
    }

    private void evaluateCurrentInput() {
        String expr = evalField.getText();
        if (expr == null || expr.trim().isEmpty() || engine == null) return;
        try {
            Object result = engine.executeScript(expr);
            appendLine("> " + expr);
            String resultStr = String.valueOf(result);
            appendLine("[result] " + resultStr);
            addToWatchHistory(expr, resultStr);
        } catch (Exception e) {
            appendLine("> " + expr);
            appendLine("[error] " + e.getMessage());
            if (onErrorReported != null) onErrorReported.accept(e.getMessage());
        }
        evalField.clear();
    }

    private void addToWatchHistory(String expr, String result) {
        String entry = expr + " = " + result;
        watchExpressions.add(0, entry);
        if (watchExpressions.size() > 50) watchExpressions.remove(watchExpressions.size() - 1);
        refreshWatches();
    }

    private void addWatch() {
        String expr = watchField.getText().trim();
        if (expr.isEmpty()) return;
        watchField.clear();
        if (engine != null) {
            try {
                Object result = engine.executeScript(expr);
                watchExpressions.add(expr + " = " + String.valueOf(result));
            } catch (Exception e) {
                watchExpressions.add(expr + " = <error: " + e.getMessage() + ">");
            }
        } else {
            watchExpressions.add(expr + " = (no engine)");
        }
        refreshWatches();
    }

    private void refreshWatches() {
        watchList.getItems().setAll(watchExpressions);
    }

    public void executeScript(String script) {
        if (engine == null) return;
        try {
            engine.executeScript(script);
        } catch (Exception e) {
            appendLine("[error] " + e.getMessage());
            if (onErrorReported != null) onErrorReported.accept(e.getMessage());
        }
    }

    public void appendLine(String line) {
        Platform.runLater(() -> {
            consoleList.getItems().add(line);
            consoleList.scrollTo(consoleList.getItems().size() - 1);
        });
    }

    public void showError(String message) {
        appendLine("[error] " + message);
    }

    public void toggleBreakpoint(int lineNumber) {
        if (breakpoints.contains(lineNumber)) {
            breakpoints.remove(lineNumber);
            appendLine("[info] Breakpoint removed at line " + (lineNumber + 1));
        } else {
            breakpoints.add(lineNumber);
            appendLine("[info] Breakpoint set at line " + (lineNumber + 1));
        }
    }

    public Set<Integer> getBreakpoints() {
        return breakpoints;
    }

    public String injectBreakpoints(String jsSource) {
        if (breakpoints.isEmpty()) return jsSource;
        String[] lines = jsSource.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (breakpoints.contains(i)) {
                sb.append("debugger; ");
            }
            sb.append(lines[i]);
            if (i < lines.length - 1) sb.append("\n");
        }
        return sb.toString();
    }

    public void clearConsole() {
        consoleList.getItems().clear();
    }

    public void updateCallStack(String[] frames) {
        Platform.runLater(() -> {
            callStackList.getItems().setAll(frames);
        });
    }

    public void updateVariables(java.util.Map<String, String> vars) {
        TreeItem<String> root = new TreeItem<>("Scope");
        root.setExpanded(true);
        if (vars != null) {
            for (java.util.Map.Entry<String, String> e : vars.entrySet()) {
                TreeItem<String> item = new TreeItem<>(e.getKey() + ": " + e.getValue());
                root.getChildren().add(item);
            }
        }
        Platform.runLater(() -> variablesTree.setRoot(root));
    }

    public void refreshAllWatches() {
        if (engine == null) return;
        java.util.List<String> updated = new java.util.ArrayList<>();
        for (String expr : watchExpressions) {
            String varName = expr.split("\\s*=\\s*")[0].trim();
            try {
                Object result = engine.executeScript(varName);
                updated.add(varName + " = " + String.valueOf(result));
            } catch (Exception e) {
                updated.add(varName + " = <error>");
            }
        }
        watchExpressions.clear();
        watchExpressions.addAll(updated);
        refreshWatches();
    }

    public class ConsoleBridge {
        public void log(String msg) { appendLine("[log] " + msg); }
        public void warn(String msg) { appendLine("[warn] " + msg); }
        public void error(String msg) {
            appendLine("[error] " + msg);
            if (onErrorReported != null) onErrorReported.accept(msg);
        }
        public void stackTrace(String trace) {
            appendLine("[stack] " + trace);
        }
    }
}
