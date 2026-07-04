package com.eagle.editor;

import com.eagle.icons.IconManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TodoPanel extends BorderPane {

    public static class TodoItem {
        public final File file;
        public final int line;
        public final int column;
        public final String type;
        public final String message;
        public final String priority; // CRITICAL, HIGH, MEDIUM, LOW

        public TodoItem(File file, int line, int column, String type, String message) {
            this.file = file;
            this.line = line;
            this.column = column;
            this.type = type.toUpperCase();
            this.message = message;
            String t = this.type;
            if (t.matches("FIXME|BUG|FIX_THIS|ERROR")) this.priority = "CRITICAL";
            else if (t.matches("HACK|WORKAROUND|TEMP|XXX|OPTIMIZE")) this.priority = "HIGH";
            else if (t.matches("TODO|ISSUE|DEPRECATED|WARNING")) this.priority = "MEDIUM";
            else this.priority = "LOW";
        }

        public String typeStyle() {
            switch (type) {
                case "FIXME": case "BUG": case "FIX_THIS": return "-fx-text-fill: #ff5555; -fx-font-weight: bold;";
                case "HACK": case "WORKAROUND": case "TEMP": return "-fx-text-fill: #ffb86c; -fx-font-weight: bold;";
                case "TODO": return "-fx-text-fill: #f1fa8c; -fx-font-weight: bold;";
                case "OPTIMIZE": case "PERF": return "-fx-text-fill: #8be9fd; -fx-font-weight: bold;";
                case "NOTE": case "REVIEW": case "XXX": return "-fx-text-fill: #6272a4; -fx-font-weight: bold;";
                case "DEPRECATED": case "WARNING": return "-fx-text-fill: #ff5555;";
                default: return "-fx-text-fill: -text-primary; -fx-font-weight: bold;";
            }
        }

        public String priorityBadge() {
            switch (priority) {
                case "CRITICAL": return "⬤ CRITICAL";
                case "HIGH": return "◉ HIGH";
                case "MEDIUM": return "○ MEDIUM";
                default: return "○ LOW";
            }
        }

        public String priorityStyle() {
            switch (priority) {
                case "CRITICAL": return "-fx-text-fill: #ff5555; -fx-font-size: 10px;";
                case "HIGH": return "-fx-text-fill: #ffb86c; -fx-font-size: 10px;";
                case "MEDIUM": return "-fx-text-fill: #f1fa8c; -fx-font-size: 10px;";
                default: return "-fx-text-fill: #6272a4; -fx-font-size: 10px;";
            }
        }
    }

    private static final Pattern TODO_PATTERN = Pattern.compile(
        "(TODO|FIXME|FIX_THIS|BUG|HACK|WORKAROUND|TEMP|XXX|OPTIMIZE|PERF|NOTE|REVIEW|ISSUE|DEPRECATED|WARNING|ERROR)" +
        "\\s*[:\\-]?\\s*(.*?)$",
        Pattern.MULTILINE | Pattern.CASE_INSENSITIVE
    );

    private static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList(
        "java", "js", "ts", "jsx", "tsx", "html", "htm", "css", "scss", "less", "sass",
        "py", "php", "xml", "fxml", "json", "yml", "yaml", "sql", "sh", "bash", "md",
        "vue", "svelte", "properties", "cfg", "ini", "gradle", "kt", "ktx", "swift"
    );

    private final ListView<TodoItem> todoList = new ListView<>();
    private final Label statusLabel = new Label("No project scanned");
    private final TextField filterField = new TextField();
    private final HBox filterBar = new HBox(4);
    private File projectRoot;
    private List<TodoItem> allResults = new ArrayList<>();
    private Consumer<TodoItem> onOpenFile;
    private String activeTypeFilter = "ALL";

    public TodoPanel() {
        setStyle("-fx-background-color: -bg-secondary;");

        // Header
        Label header = new Label("☰ TODO / FIXME");
        header.setStyle("-fx-font-weight: bold; -fx-padding: 8; -fx-font-size: 13px;");

        Button refreshBtn = new Button("", IconManager.refreshIcon());
        refreshBtn.setTooltip(new Tooltip("Scan project for TODOs"));
        refreshBtn.setOnAction(e -> scanProject());

        HBox topBar = new HBox(8, header, refreshBtn);
        topBar.setPadding(new Insets(4, 8, 4, 8));
        topBar.setStyle("-fx-border-color: -border-color; -fx-border-width: 0 0 1 0;");

        // Type filter buttons
        String[][] typeFilters = {
            {"ALL", "#f8f8f2"}, {"FIXME", "#ff5555"}, {"TODO", "#f1fa8c"},
            {"HACK", "#ffb86c"}, {"BUG", "#ff5555"}, {"NOTE", "#6272a4"},
            {"OPTIMIZE", "#8be9fd"}, {"DEPRECATED", "#ff5555"}
        };
        for (String[] tf : typeFilters) {
            Button btn = new Button(tf[0]);
            btn.setStyle("-fx-font-size: 10px; -fx-padding: 2 6; -fx-background: transparent; -fx-text-fill: " + tf[1] + "; -fx-cursor: hand;");
            btn.setOnAction(e -> { activeTypeFilter = tf[0]; applyFilter(); });
            filterBar.getChildren().add(btn);
        }
        filterBar.setPadding(new Insets(2, 8, 2, 8));
        filterBar.setAlignment(Pos.CENTER_LEFT);

        // Search field
        filterField.setPromptText("Filter items...");
        filterField.textProperty().addListener((obs, o, n) -> applyFilter());
        filterField.setStyle("-fx-font-size: 11px; -fx-padding: 3 6;");

        VBox topSection = new VBox(0, topBar, filterBar, filterField);

        // ListView with custom cell
        todoList.setCellFactory(lv -> new ListCell<TodoItem>() {
            private final HBox graph = new HBox(6);
            private final Label typeLabel = new Label();
            private final Label msgLabel = new Label();
            private final Label fileLabel = new Label();
            private final Label lineLabel = new Label();
            {
                graph.setAlignment(Pos.CENTER_LEFT);
                typeLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
                msgLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-primary;");
                fileLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-muted;");
                lineLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #6272a4;");
            }
            @Override
            protected void updateItem(TodoItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null); setText(null);
                } else {
                    typeLabel.setText(item.type);
                    typeLabel.setStyle(item.typeStyle() + "-fx-font-size: 11px; -fx-font-weight: bold;");
                    msgLabel.setText(item.message);
                    fileLabel.setText(item.file.getName());
                    lineLabel.setText(":" + item.line);
                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    graph.getChildren().setAll(typeLabel, new Label(" "), msgLabel, spacer, fileLabel, lineLabel);
                    setGraphic(graph);
                    setText(null);
                    setPrefHeight(24);
                    setStyle("-fx-padding: 1 8; -fx-cursor: hand;");
                    setOnMouseClicked(e -> {
                        if (e.getClickCount() == 2 && onOpenFile != null) {
                            onOpenFile.accept(item);
                        }
                    });
                }
            }
        });

        statusLabel.setStyle("-fx-padding: 6 8; -fx-font-size: 11px; -fx-text-fill: -text-muted;");
        setTop(topSection);
        setCenter(todoList);
        setBottom(statusLabel);
    }

    public void setOnOpenFile(Consumer<TodoItem> callback) { this.onOpenFile = callback; }

    public void setProjectRoot(File root) {
        this.projectRoot = root;
        scanProject();
    }

    public void scanProject() {
        todoList.getItems().clear();
        allResults.clear();
        if (projectRoot == null || !projectRoot.exists()) {
            statusLabel.setText("No project open");
            return;
        }
        statusLabel.setText("Scanning...");
        new Thread(() -> {
            List<TodoItem> results = new ArrayList<>();
            scanDir(projectRoot, results);
            Platform.runLater(() -> {
                allResults = results;
                applyFilter();
            });
        }).start();
    }

    private void scanDir(File dir, List<TodoItem> results) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                if (!f.getName().startsWith(".") && !f.getName().equals("node_modules")
                    && !f.getName().equals("dist") && !f.getName().equals("build")
                    && !f.getName().equals(".git")) {
                    scanDir(f, results);
                }
            } else {
                String name = f.getName().toLowerCase();
                String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";
                if (SUPPORTED_EXTENSIONS.contains(ext)) {
                    try {
                        String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                        String[] lines = content.split("\n", -1);
                        for (int i = 0; i < lines.length; i++) {
                            Matcher m = TODO_PATTERN.matcher(lines[i]);
                            while (m.find()) {
                                int col = m.start();
                                String msg = m.group(2).trim();
                                results.add(new TodoItem(f, i + 1, col, m.group(1), msg.isEmpty() ? "(no description)" : msg));
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    private void applyFilter() {
        String filter = filterField.getText().toLowerCase().trim();
        List<TodoItem> filtered = new ArrayList<>();
        for (TodoItem item : allResults) {
            boolean typeMatch = activeTypeFilter.equals("ALL") || item.type.equals(activeTypeFilter);
            boolean textMatch = filter.isEmpty()
                || item.type.toLowerCase().contains(filter)
                || item.message.toLowerCase().contains(filter)
                || item.file.getName().toLowerCase().contains(filter);
            if (typeMatch && textMatch) filtered.add(item);
        }
        todoList.getItems().setAll(filtered);
        updateCounts();
    }

    private void updateCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (TodoItem item : allResults) {
            counts.put(item.type, counts.getOrDefault(item.type, 0) + 1);
        }
        StringBuilder sb = new StringBuilder(allResults.size() + " total");
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            sb.append(" | ").append(e.getKey()).append(": ").append(e.getValue());
        }
        int shown = todoList.getItems().size();
        if (shown != allResults.size()) {
            sb.insert(0, "Showing " + shown + " of ");
        }
        statusLabel.setText(sb.toString());
    }
}
