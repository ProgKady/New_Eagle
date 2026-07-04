package com.eagle.editor;

import com.eagle.controller.TabManager;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

public class AiFileManager {

    public static class FileOp {
        public final File file;
        public final String type; // create, modify, delete, rename
        public final File backupFile;
        public final String oldContent;
        public final String newContent;
        public final long timestamp;
        public String diffText;

        public FileOp(File file, String type, File backupFile, String oldContent, String newContent) {
            this.file = file;
            this.type = type;
            this.backupFile = backupFile;
            this.oldContent = oldContent;
            this.newContent = newContent;
            this.timestamp = System.currentTimeMillis();
            this.diffText = buildDiff(oldContent, newContent);
        }

        private String buildDiff(String oldC, String newC) {
            if (oldC == null) return "[NEW FILE]\n" + (newC != null ? newC : "");
            if (newC == null) return "[DELETED]\n" + oldC;
            String[] oldLines = oldC.split("\n", -1);
            String[] newLines = newC.split("\n", -1);
            StringBuilder sb = new StringBuilder();
            int maxLen = Math.max(oldLines.length, newLines.length);
            // Simple line-based diff
            boolean hasDiff = false;
            for (int i = 0; i < maxLen; i++) {
                String ol = i < oldLines.length ? oldLines[i] : "";
                String nl = i < newLines.length ? newLines[i] : "";
                if (!ol.equals(nl)) {
                    hasDiff = true;
                    if (!ol.isEmpty()) sb.append("- ").append(ol).append("\n");
                    if (!nl.isEmpty()) sb.append("+ ").append(nl).append("\n");
                } else {
                    sb.append("  ").append(ol).append("\n");
                }
            }
            if (!hasDiff) return "[No changes]\n" + (newC.length() > 500 ? newC.substring(0, 500) + "..." : newC);
            return sb.toString();
        }
    }

    private final File projectRoot;
    private final File backupRoot;
    private final List<FileOp> history = new ArrayList<>();
    private int undoPointer = -1;
    private TabManager tabManager;
    private final VBox fileTreePanel = new VBox(2);
    private int totalTokens = 0;
    private int totalCostCents = 0;

    public AiFileManager(File projectRoot) {
        this.projectRoot = projectRoot;
        this.backupRoot = new File(projectRoot, ".webide_backups");
        backupRoot.mkdirs();
        buildFileTreePanel();
    }

    public void setTabManager(TabManager tm) { this.tabManager = tm; }
    public VBox getFileTreePanel() { return fileTreePanel; }
    public List<FileOp> getHistory() { return history; }
    public int getTotalTokens() { return totalTokens; }
    public int getTotalCostCents() { return totalCostCents; }

    public void addTokens(int tokens, int costCents) {
        totalTokens += tokens;
        totalCostCents += costCents;
    }

    // Backup a file before modifying
    public File backupFile(File f) {
        if (!f.exists()) return null;
        try {
            File bak = new File(backupRoot, f.getName() + "." + System.currentTimeMillis() + ".bak");
            bak.getParentFile().mkdirs();
            Files.copy(f.toPath(), bak.toPath());
            return bak;
        } catch (IOException e) {
            return null;
        }
    }

    // Create or update a file
    public FileOp applyFile(File file, String newContent) {
        boolean exists = file.exists();
        String oldContent = null;
        File backup = null;
        if (exists) {
            try { oldContent = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8); } catch (IOException e) {}
            backup = backupFile(file);
        }
        try {
            file.getParentFile().mkdirs();
            Files.write(file.toPath(), newContent.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) { return null; }

        FileOp op = new FileOp(file, exists ? "modify" : "create", backup, oldContent, newContent);
        history.add(op);
        undoPointer = history.size() - 1;
        buildFileTreePanel();
        return op;
    }

    // Delete a file
    public FileOp deleteFile(File file) {
        if (!file.exists()) return null;
        String oldContent = null;
        try { oldContent = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8); } catch (IOException e) {}
        backupFile(file);
        file.delete();
        FileOp op = new FileOp(file, "delete", null, oldContent, null);
        history.add(op);
        undoPointer = history.size() - 1;
        buildFileTreePanel();
        return op;
    }

    // Rename a file
    public FileOp renameFile(File src, File dst) {
        if (!src.exists()) return null;
        String oldContent = null;
        try { oldContent = new String(Files.readAllBytes(src.toPath()), StandardCharsets.UTF_8); } catch (IOException e) {}
        backupFile(src);
        dst.getParentFile().mkdirs();
        src.renameTo(dst);
        FileOp op = new FileOp(src, "rename", null, oldContent, dst.getAbsolutePath());
        history.add(op);
        undoPointer = history.size() - 1;
        buildFileTreePanel();
        return op;
    }

    // Undo last operation
    public FileOp undo() {
        if (history.isEmpty() || undoPointer < 0) return null;
        FileOp op = history.get(undoPointer);
        undoPointer--;
        if (op.backupFile != null && op.backupFile.exists()) {
            try {
                Files.copy(op.backupFile.toPath(), op.file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {}
        } else if ("create".equals(op.type)) {
            op.file.delete();
        } else if ("delete".equals(op.type) && op.oldContent != null) {
            try {
                op.file.getParentFile().mkdirs();
                Files.write(op.file.toPath(), op.oldContent.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {}
        } else if ("rename".equals(op.type) && op.oldContent != null) {
            File dst = new File(op.newContent);
            try {
                dst.getParentFile().mkdirs();
                Files.write(dst.toPath(), op.oldContent.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {}
        }
        buildFileTreePanel();
        return op;
    }

    // Show diff dialog
    public void showDiff(FileOp op) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Diff: " + op.file.getName());
        dialog.setHeaderText(op.type.toUpperCase() + " — " + relativePath(op.file));
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE));
        dialog.getDialogPane().setPrefSize(650, 450);

        TextArea ta = new TextArea(op.diffText);
        ta.setEditable(false);
        ta.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px;");
        dialog.getDialogPane().setContent(ta);
        dialog.show();
    }

    // Show full history dialog
    public void showHistoryDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("AI File Operations History");
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE));
        dialog.getDialogPane().setPrefSize(600, 400);

        VBox list = new VBox(4);
        list.setPadding(new Insets(8));
        for (int i = history.size() - 1; i >= 0; i--) {
            FileOp op = history.get(i);
            String label = op.type.toUpperCase() + " " + relativePath(op.file) + " (" + new java.text.SimpleDateFormat("HH:mm:ss").format(new Date(op.timestamp)) + ")";
            Button row = new Button(label);
            row.setStyle("-fx-background-color: transparent; -fx-text-fill: -text-primary; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 4 8; -fx-alignment: CENTER_LEFT;");
            row.setMaxWidth(Double.MAX_VALUE);
            final FileOp captured = op;
            row.setOnAction(e -> showDiff(captured));
            list.getChildren().add(row);
        }
        ScrollPane sp = new ScrollPane(list);
        sp.setFitToWidth(true);
        dialog.getDialogPane().setContent(sp);
        dialog.show();
    }

    // Build file tree panel showing AI changes
    private void buildFileTreePanel() {
        fileTreePanel.getChildren().clear();
        Label title = new Label("AI File Changes");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: -text-primary; -fx-padding: 4 0;");
        fileTreePanel.getChildren().add(title);

        if (history.isEmpty()) {
            Label empty = new Label("No changes yet");
            empty.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-muted;");
            fileTreePanel.getChildren().add(empty);
            return;
        }

        // Group by status
        Set<String> added = new LinkedHashSet<>();
        Set<String> modified = new LinkedHashSet<>();
        Set<String> deleted = new LinkedHashSet<>();
        for (FileOp op : history) {
            String path = relativePath(op.file);
            switch (op.type) {
                case "create": added.add(path); break;
                case "modify": modified.add(path); break;
                case "delete": deleted.add(path); break;
                case "rename": added.add(op.newContent); deleted.add(path); break;
            }
        }

        if (!added.isEmpty()) addFileGroup(fileTreePanel, "Created", added, "#22c55e");
        if (!modified.isEmpty()) addFileGroup(fileTreePanel, "Modified", modified, "#f59e0b");
        if (!deleted.isEmpty()) addFileGroup(fileTreePanel, "Deleted", deleted, "#ef4444");

        Button undoBtn = new Button("Undo Last (" + (undoPointer + 1) + "/" + history.size() + ")");
        undoBtn.setStyle("-fx-background-color: -accent; -fx-text-fill: white; -fx-font-size: 10px; -fx-background-radius: 4; -fx-padding: 3 10; -fx-cursor: hand;");
        undoBtn.setOnAction(e -> {
            FileOp undone = undo();
            if (undone != null && tabManager != null) {
                tabManager.openCodeFile(undone.file);
            }
            buildFileTreePanel();
        });
        undoBtn.setMaxWidth(Double.MAX_VALUE);
        fileTreePanel.getChildren().add(undoBtn);

        Button historyBtn = new Button("View History");
        historyBtn.setStyle("-fx-background-color: -bg-tertiary; -fx-text-fill: -text-primary; -fx-font-size: 10px; -fx-background-radius: 4; -fx-padding: 3 10; -fx-cursor: hand;");
        historyBtn.setOnAction(e -> showHistoryDialog());
        historyBtn.setMaxWidth(Double.MAX_VALUE);
        fileTreePanel.getChildren().add(historyBtn);

        // Token/cost stats
        if (totalTokens > 0) {
            Label stats = new Label("Tokens: " + totalTokens + " | Cost: $" + String.format("%.2f", totalCostCents / 100.0));
            stats.setStyle("-fx-font-size: 9px; -fx-text-fill: -text-muted; -fx-padding: 4 0 0 0;");
            fileTreePanel.getChildren().add(stats);
        }
    }

    private void addFileGroup(VBox parent, String groupName, Set<String> files, String color) {
        Label group = new Label(groupName + " (" + files.size() + ")");
        group.setStyle("-fx-font-size: 10px; -fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-padding: 2 0;");
        parent.getChildren().add(group);
        for (String f : files) {
            Label fileLabel = new Label("  " + f);
            fileLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: -text-muted; -fx-cursor: hand;");
            fileLabel.setOnMouseClicked(e -> {
                File file = new File(projectRoot, f);
                if (file.exists() && tabManager != null) tabManager.openCodeFile(file);
            });
            parent.getChildren().add(fileLabel);
        }
    }

    private String relativePath(File f) {
        try { return projectRoot.toURI().relativize(f.toURI()).getPath(); } catch (Exception e) { return f.getName(); }
    }

    // Problem scanner — run basic lint checks after changes
    public void scanForProblems(File file) {
        if (!file.exists()) return;
        String content;
        try { content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8); } catch (IOException e) { return; }

        List<String> problems = new ArrayList<>();
        String name = file.getName();

        if (name.endsWith(".java")) {
            // Check for common Java issues
            if (!content.contains("class ") && !content.contains("interface ") && !content.contains("enum "))
                problems.add("No class/interface/enum found");
            if (content.contains("class ") && !content.contains("{"))
                problems.add("Missing class body braces");
            if (content.contains("public static void main"))
                problems.add("Has main() method");
        }
        if (name.endsWith(".py")) {
            if (!content.contains("def ") && !content.contains("class "))
                problems.add("No function or class found");
        }
        if (name.endsWith(".html") && !content.contains("<!DOCTYPE html") && !content.contains("<html"))
            problems.add("Missing DOCTYPE or html tag");
        if (name.endsWith(".json")) {
            try { new com.google.gson.Gson().fromJson(content, Object.class); }
            catch (Exception e) { problems.add("Invalid JSON: " + e.getMessage()); }
        }

        if (!problems.isEmpty()) {
            Platform.runLater(() -> {
                // Show in chat as system message
                // The AiPanel will handle this via callback
                if (problemCallback != null) problemCallback.accept(file, problems);
            });
        }
    }

    private java.util.function.BiConsumer<File, List<String>> problemCallback;
    public void setProblemCallback(java.util.function.BiConsumer<File, List<String>> cb) { this.problemCallback = cb; }
}
