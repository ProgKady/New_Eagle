package com.eagle.tools;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class DuplicateCodeFinder {

    public static class DuplicateResult {
        public final File file1; public final int line1;
        public final File file2; public final int line2;
        public final int lineCount; public final double similarity;
        public final String preview;

        public DuplicateResult(File f1, int l1, File f2, int l2, int lc, double sim, String prev) {
            file1 = f1; line1 = l1; file2 = f2; line2 = l2;
            lineCount = lc; similarity = sim; preview = prev;
        }
    }

    public static void show(File projectRoot) {
        if (projectRoot == null || !projectRoot.exists()) {
            DialogUtil.showError("No project", "Open a project first"); return;
        }

        Dialog<Void> dlg = DialogUtil.progressDialog("Duplicate Code Finder", "Scanning for duplicate code blocks...");
        ProgressIndicator pi = new ProgressIndicator();
        dlg.getDialogPane().setContent(pi);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dlg.show();

        new Thread(() -> {
            try {
                List<File> files = new ArrayList<>();
                collectFiles(projectRoot, files);
                List<DuplicateResult> results = findDuplicates(files);
                Platform.runLater(() -> {
                    dlg.setHeaderText("Found " + results.size() + " potential duplicates");
                    showResults(results);
                    dlg.close();
                });
            } catch (Exception e) {
                Platform.runLater(() -> { DialogUtil.showError("Error", e.getMessage()); dlg.close(); });
            }
        }).start();
    }

    private static void collectFiles(File dir, List<File> files) {
        File[] list = dir.listFiles();
        if (list == null) return;
        for (File f : list) {
            if (f.isDirectory()) {
                if (!f.getName().startsWith(".") && !f.getName().equals("node_modules")
                    && !f.getName().equals("dist") && !f.getName().equals("build")
                    && !f.getName().equals(".git")) collectFiles(f, files);
            } else {
                String name = f.getName().toLowerCase();
                if (name.endsWith(".java") || name.endsWith(".js") || name.endsWith(".ts")
                    || name.endsWith(".py") || name.endsWith(".php") || name.endsWith(".html")
                    || name.endsWith(".css") || name.endsWith(".xml") || name.endsWith(".json")
                    || name.endsWith(".sql")) files.add(f);
            }
        }
    }

    private static List<DuplicateResult> findDuplicates(List<File> files) {
        List<DuplicateResult> results = new ArrayList<>();
        int total = files.size();
        for (int i = 0; i < total; i++) {
            for (int j = i + 1; j < total; j++) {
                try { compareFiles(files.get(i), files.get(j), results); } catch (Exception ignored) {}
            }
        }
        results.sort((a, b) -> Double.compare(b.similarity, a.similarity));
        return results.size() > 50 ? results.subList(0, 50) : results;
    }

    private static void compareFiles(File f1, File f2, List<DuplicateResult> results) throws Exception {
        List<String> lines1 = Files.readAllLines(f1.toPath(), StandardCharsets.UTF_8);
        List<String> lines2 = Files.readAllLines(f2.toPath(), StandardCharsets.UTF_8);
        if (lines1.size() < 5 || lines2.size() < 5) return;

        for (int i = 0; i <= lines1.size() - 5; i++) {
            for (int j = 0; j <= lines2.size() - 5; j++) {
                int matchLen = 0;
                while (i + matchLen < lines1.size() && j + matchLen < lines2.size()
                    && lines1.get(i + matchLen).trim().equals(lines2.get(j + matchLen).trim())) matchLen++;
                if (matchLen >= 5) {
                    StringBuilder preview = new StringBuilder();
                    for (int k = 0; k < Math.min(matchLen, 8); k++) preview.append(lines1.get(i + k).trim()).append("\n");
                    if (matchLen > 8) preview.append("... (+").append(matchLen - 8).append(" more lines)");
                    double sim = matchLen / (double) Math.min(lines1.size(), lines2.size()) * 100;
                    results.add(new DuplicateResult(f1, i + 1, f2, j + 1, matchLen, sim, preview.toString().trim()));
                    i += matchLen - 1; break;
                }
            }
        }
    }

    private static void showResults(List<DuplicateResult> results) {
        Dialog<Void> dlg = DialogUtil.progressDialog("Duplicate Code Finder Results", "");
        dlg.setResizable(true);
        dlg.getDialogPane().setPrefSize(800, 500);

        ListView<DuplicateResult> listView = new ListView<>();
        listView.setCellFactory(lv -> new ListCell<DuplicateResult>() {
            @Override
            protected void updateItem(DuplicateResult item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else {
                    setText(String.format("%.0f%% | %s:%d \u2194 %s:%d (%d lines)",
                        item.similarity, item.file1.getName(), item.line1,
                        item.file2.getName(), item.line2, item.lineCount));
                    setTooltip(new Tooltip(item.preview));
                }
            }
        });
        listView.getItems().addAll(results);

        TextArea previewArea = new TextArea();
        previewArea.setEditable(false);
        previewArea.setStyle("-fx-font-family: monospace; -fx-font-size: 11px;");
        listView.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) previewArea.setText(n.preview);
        });

        SplitPane split = new SplitPane(listView, previewArea);
        split.setDividerPositions(0.5);
        dlg.getDialogPane().setContent(split);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dlg.showAndWait();
    }
}
