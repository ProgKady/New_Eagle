package com.eagle.tools;

import com.eagle.util.ThemeManager;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

public class GeneratorLogViewer {

    private final Stage stage;
    private final ListView<String> listView;
    private final List<String> logSource;
    private final Timeline refresher;
    private final Button exportBtn;
    private final Button savePromptBtn;
    private final Button savedPromptsBtn;
    private int lastKnownSize = 0;

    public GeneratorLogViewer(List<String> logSource, String projectPath) {
        this(logSource, projectPath, null);
    }

    public GeneratorLogViewer(List<String> logSource, String projectPath, javafx.stage.Window owner) {
        this.logSource = logSource;

        stage = new Stage();
        if (owner != null) stage.initOwner(owner);
        stage.initModality(Modality.NONE);
        stage.setTitle("Generator Log — Live");

        Label header = new Label("Live Generator Log");
        header.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 0 0 5 0;");

        Label pathLabel = new Label(projectPath != null && !projectPath.isEmpty() ? projectPath : "");
        pathLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-muted; -fx-padding: 0 0 10 0;");

        listView = new ListView<>();
        listView.setPrefSize(800, 450);
        listView.setCellFactory(lv -> new ColoredLogCell());

        refresher = new Timeline(new KeyFrame(Duration.millis(500), e -> refresh()));
        refresher.setCycleCount(Timeline.INDEFINITE);

        HBox btnBar = new HBox(8);
        btnBar.setAlignment(Pos.CENTER_LEFT);

        exportBtn = new Button("Export Log");
        exportBtn.setOnAction(e -> exportLog());

        savePromptBtn = new Button("Save Prompt");
        savePromptBtn.setOnAction(e -> savePromptDialog());

        savedPromptsBtn = new Button("Saved Prompts");
        savedPromptsBtn.setOnAction(e -> showSavedPrompts());

        Button closeBtn = new Button("Close");
        closeBtn.setOnAction(e -> {
            refresher.stop();
            stage.close();
        });

        btnBar.getChildren().addAll(exportBtn, savePromptBtn, savedPromptsBtn, closeBtn);

        VBox root = new VBox(6, header, pathLabel, listView, btnBar);
        root.setPadding(new Insets(12));
        VBox.setVgrow(listView, Priority.ALWAYS);

        Scene scene = new Scene(root);
        ThemeManager.getInstance().applyTheme(scene);
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> refresher.stop());

        refresher.play();
    }

    public void show() {
        refresh();
        stage.show();
    }

    public void close() {
        refresher.stop();
        stage.close();
    }

    public boolean isShowing() {
        return stage.isShowing();
    }

    public void toFront() {
        stage.toFront();
    }

    private void refresh() {
        synchronized (logSource) {
            int size = logSource.size();
            if (size > lastKnownSize) {
                listView.getItems().clear();
                listView.getItems().addAll(logSource);
                lastKnownSize = size;
            }
        }
        if (!listView.getItems().isEmpty()) {
            listView.scrollTo(listView.getItems().size() - 1);
        }
    }

    private void exportLog() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Generator Log");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        chooser.setInitialFileName("generator_log_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".txt");
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;
        try {
            StringBuilder sb = new StringBuilder();
            for (String line : logSource) {
                sb.append(line).append("\n");
            }
            Files.write(file.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
            showToast("Log exported to: " + file.getName());
        } catch (IOException ex) {
            showToast("Export failed: " + ex.getMessage());
        }
    }

    private void savePromptDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Save Prompt");
        dialog.setHeaderText("Enter a label for this prompt:");
        dialog.setContentText("Label:");
        dialog.getEditor().setPromptText("e.g., React project with dark mode");
        dialog.initOwner(stage);
        dialog.getDialogPane().sceneProperty().addListener((obs, oldS, newS) -> {
            if (newS != null) ThemeManager.getInstance().applyTheme(newS);
        });
        dialog.showAndWait().ifPresent(label -> {
            if (label.trim().isEmpty()) return;
            savePrompt(label.trim());
        });
    }

    private void savePrompt(String label) {
        try {
            File dir = new File(System.getProperty("user.home") + "/.webide");
            dir.mkdirs();
            File promptsFile = new File(dir, "saved_prompts.json");

            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"label\": ").append(JSON.stringify(label)).append(",\n");
            json.append("  \"timestamp\": \"").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\",\n");
            json.append("  \"projectPath\": ").append(JSON.stringify(stage.getTitle())).append(",\n");
            json.append("  \"lines\": [\n");
            for (int i = 0; i < logSource.size(); i++) {
                json.append("    ").append(JSON.stringify(logSource.get(i)));
                if (i < logSource.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("  ]\n}");

            String existing = "";
            if (promptsFile.exists()) {
                existing = new String(Files.readAllBytes(promptsFile.toPath()), StandardCharsets.UTF_8).trim();
            }

            String output;
            if (existing.isEmpty() || existing.equals("[]")) {
                output = "[\n" + json.toString() + "\n]";
            } else if (existing.startsWith("[")) {
                output = existing.substring(0, 1) + "\n" + json.toString() + ",\n" + existing.substring(1);
            } else {
                output = "[\n" + json.toString() + "\n]";
            }

            Files.write(promptsFile.toPath(), output.getBytes(StandardCharsets.UTF_8));
            showToast("Prompt saved as: " + label);
        } catch (IOException ex) {
            showToast("Failed to save prompt: " + ex.getMessage());
        }
    }

    private void showToast(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Generator Log Viewer");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.initOwner(stage);
        alert.getDialogPane().sceneProperty().addListener((obs, oldS, newS) -> {
            if (newS != null) ThemeManager.getInstance().applyTheme(newS);
        });
        alert.show();
    }

    private void showSavedPrompts() {
        try {
            File dir = new File(System.getProperty("user.home") + "/.webide");
            File promptsFile = new File(dir, "saved_prompts.json");
            if (!promptsFile.exists()) {
                showToast("No saved prompts yet. Generate a project or use AI Development to create prompts.");
                return;
            }
            String content = new String(Files.readAllBytes(promptsFile.toPath()), StandardCharsets.UTF_8);

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Saved Prompts");
            dialog.setHeaderText("Previously saved prompts (from ~/.webide/saved_prompts.json)");
            dialog.initOwner(stage);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            TextArea area = new TextArea(content);
            area.setEditable(false);
            area.setPrefSize(600, 450);
            area.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 10px;");

            Button copyBtn = new Button("Copy All to Clipboard");
            copyBtn.setOnAction(e -> {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent cc = new ClipboardContent();
                cc.putString(content);
                clipboard.setContent(cc);
                showToast("Copied to clipboard!");
            });

            HBox btnBar = new HBox(8, copyBtn);
            VBox root = new VBox(8, area, btnBar);
            root.setPadding(new Insets(12));
            dialog.getDialogPane().setContent(root);
            dialog.getDialogPane().sceneProperty().addListener((obs, oldS, newS) -> {
                if (newS != null) ThemeManager.getInstance().applyTheme(newS);
            });
            dialog.show();
        } catch (IOException ex) {
            showToast("Failed to load saved prompts: " + ex.getMessage());
        }
    }

    // ── Colored List Cell ──

    private static class ColoredLogCell extends ListCell<String> {
        private final Text text = new Text();
        {
            text.setFont(Font.font("Consolas", 11));
            setGraphic(text);
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                text.setText(null);
                return;
            }
            text.setText(item);
            String upper = item.toUpperCase();
            if (upper.contains("ERROR") || upper.contains("FATAL") || upper.contains("FAILED")
                || item.contains("❌") || item.contains("✗") || upper.contains("ROLLBACK")) {
                text.setFill(Color.web("#F44336"));
            } else if (upper.contains("COMPLETE") || upper.contains("SUCCESS") || upper.contains("CREATED")
                || upper.contains("WRITTEN") || item.contains("✅") || upper.contains("DONE")) {
                text.setFill(Color.web("#4CAF50"));
            } else if (upper.contains("PHASE") || upper.contains("PLANNING") || upper.contains("START")
                || upper.contains("GENERATION")) {
                text.setFill(Color.web("#2196F3"));
            } else if (upper.contains("WARN") || upper.contains("RETRY") || upper.contains("SKIP")) {
                text.setFill(Color.web("#FF9800"));
            } else if (item.contains("🔄") || upper.contains("MODIFIED") || upper.contains("UPDATED")) {
                text.setFill(Color.web("#9C27B0"));
            } else {
                text.setFill(Color.web("#E0E0E0"));
            }
        }
    }

    // Minimal JSON stringifier (no external lib needed)
    private static class JSON {
        static String stringify(String s) {
            if (s == null) return "null";
            StringBuilder sb = new StringBuilder();
            sb.append('"');
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '"': sb.append("\\\""); break;
                    case '\\': sb.append("\\\\"); break;
                    case '\n': sb.append("\\n"); break;
                    case '\r': sb.append("\\r"); break;
                    case '\t': sb.append("\\t"); break;
                    default: sb.append(c);
                }
            }
            sb.append('"');
            return sb.toString();
        }
    }
}
