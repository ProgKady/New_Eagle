package com.eagle.tools;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import java.io.File;
import java.text.DecimalFormat;
import java.util.*;

public class FileAnalyzerTools {

    private static final DecimalFormat SIZE_FMT = new DecimalFormat("#,##0.#");
    private static final String[] SIZE_UNITS = {"B", "KB", "MB", "GB"};

    private static String formatSize(long bytes) {
        if (bytes == 0) return "0 B";
        int unit = (int) (Math.log10(bytes) / Math.log10(1024));
        double size = bytes / Math.pow(1024, unit);
        return SIZE_FMT.format(size) + " " + SIZE_UNITS[Math.min(unit, SIZE_UNITS.length - 1)];
    }

    public static class FileEntry {
        File file; long size; String ext;
        FileEntry(File f, long s, String e) { file = f; size = s; ext = e; }
    }

    public static void showFileSizeAnalyzer(File projectRoot) {
        if (projectRoot == null || !projectRoot.exists()) {
            DialogUtil.showError("No project", "Open a project first"); return;
        }
        Dialog<Void> dlg = DialogUtil.progressDialog("File Size Analyzer", "Scanning files...");
        dlg.setResizable(true);
        dlg.getDialogPane().setPrefSize(800, 550);
        ProgressIndicator pi = new ProgressIndicator();
        dlg.getDialogPane().setContent(pi);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dlg.show();

        new Thread(() -> {
            List<FileEntry> entries = new ArrayList<>();
            scanFiles(projectRoot, entries);
            entries.sort((a, b) -> Long.compare(b.size, a.size));
            Platform.runLater(() -> {
                ListView<FileEntry> listView = new ListView<>();
                listView.setCellFactory(lv -> new ListCell<FileEntry>() {
                    @Override
                    protected void updateItem(FileEntry item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) setText(null);
                        else setText(String.format("%-10s  %s  %s",
                            formatSize(item.size), item.ext,
                            item.file.getAbsolutePath().substring(projectRoot.getAbsolutePath().length() + 1)));
                    }
                });
                listView.getItems().addAll(entries);
                long total = entries.stream().mapToLong(e -> e.size).sum();
                Label stats = new Label(entries.size() + " files, total " + formatSize(total));
                stats.setPadding(new Insets(6));
                VBox vb = new VBox(stats, listView);
                VBox.setVgrow(listView, Priority.ALWAYS);
                dlg.getDialogPane().setContent(vb);
                dlg.setHeaderText("File Size Analyzer");
                dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            });
        }).start();
    }

    private static void scanFiles(File dir, List<FileEntry> entries) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                if (!f.getName().startsWith(".") && !f.getName().equals("node_modules")
                    && !f.getName().equals("dist") && !f.getName().equals("build")
                    && !f.getName().equals(".git")) scanFiles(f, entries);
            } else {
                String name = f.getName().toLowerCase();
                String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "?";
                entries.add(new FileEntry(f, f.length(), ext));
            }
        }
    }

    public static void showLargeFileViewer(File projectRoot) {
        if (projectRoot == null || !projectRoot.exists()) {
            DialogUtil.showError("No project", "Open a project first"); return;
        }
        Dialog<Void> dlg = DialogUtil.progressDialog("Large File Viewer", "Scanning for files > 1 MB...");
        dlg.setResizable(true);
        dlg.getDialogPane().setPrefSize(800, 550);
        ProgressIndicator pi = new ProgressIndicator();
        dlg.getDialogPane().setContent(pi);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dlg.show();

        new Thread(() -> {
            List<FileEntry> entries = new ArrayList<>();
            scanFiles(projectRoot, entries);
            List<FileEntry> large = new ArrayList<>();
            for (FileEntry e : entries) { if (e.size > 1_000_000) large.add(e); }
            large.sort((a, b) -> Long.compare(b.size, a.size));
            Platform.runLater(() -> {
                ListView<FileEntry> listView = new ListView<>();
                listView.setCellFactory(lv -> new ListCell<FileEntry>() {
                    @Override
                    protected void updateItem(FileEntry item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) setText(null);
                        else setText(String.format("%-10s  %s", formatSize(item.size), item.file.getAbsolutePath()));
                    }
                });
                listView.getItems().addAll(large);
                VBox vb = new VBox(new Label(large.size() + " files found > 1 MB"), listView);
                VBox.setVgrow(listView, Priority.ALWAYS);
                dlg.getDialogPane().setContent(vb);
                dlg.setHeaderText("Large File Viewer");
                dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            });
        }).start();
    }

    public static void showFolderCompare() {
        DirectoryChooser dc1 = new DirectoryChooser(); dc1.setTitle("Select first folder");
        File dir1 = dc1.showDialog(DialogUtil.getOwnerWindow());
        if (dir1 == null) return;
        DirectoryChooser dc2 = new DirectoryChooser(); dc2.setTitle("Select second folder");
        File dir2 = dc2.showDialog(DialogUtil.getOwnerWindow());
        if (dir2 == null) return;

        Dialog<Void> dlg = DialogUtil.progressDialog("Folder Compare", "Comparing...");
        dlg.setResizable(true);
        dlg.getDialogPane().setPrefSize(800, 550);
        ProgressIndicator pi = new ProgressIndicator();
        dlg.getDialogPane().setContent(pi);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dlg.show();

        new Thread(() -> {
            Set<String> set1 = new HashSet<>(); Set<String> set2 = new HashSet<>();
            listFiles(dir1, dir1, set1); listFiles(dir2, dir2, set2);
            List<String> onlyIn1 = new ArrayList<>(), onlyIn2 = new ArrayList<>(), different = new ArrayList<>();
            for (String p : set1) { if (!set2.contains(p)) onlyIn1.add(p); }
            for (String p : set2) { if (!set1.contains(p)) onlyIn2.add(p); }
            for (String p : set1) {
                if (set2.contains(p)) {
                    File f1 = new File(dir1, p); File f2 = new File(dir2, p);
                    if (f1.length() != f2.length())
                        different.add(p + " (" + formatSize(f1.length()) + " vs " + formatSize(f2.length()) + ")");
                }
            }
            Platform.runLater(() -> {
                TabPane tabs = new TabPane();
                tabs.getTabs().add(makeTab("Only in Folder 1 (" + onlyIn1.size() + ")", onlyIn1));
                tabs.getTabs().add(makeTab("Only in Folder 2 (" + onlyIn2.size() + ")", onlyIn2));
                tabs.getTabs().add(makeTab("Different Sizes (" + different.size() + ")", different));
                dlg.getDialogPane().setContent(tabs);
                dlg.setHeaderText("Folder Compare Complete");
                dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            });
        }).start();
    }

    private static void listFiles(File base, File dir, Set<String> set) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            String rel = f.getAbsolutePath().substring(base.getAbsolutePath().length() + 1);
            if (f.isDirectory()) listFiles(base, f, set);
            else set.add(rel);
        }
    }

    private static Tab makeTab(String title, List<String> items) {
        ListView<String> lv = new ListView<>(); lv.getItems().addAll(items);
        return new Tab(title, lv);
    }

    public static void showProjectSizeAnalyzer(File projectRoot) {
        if (projectRoot == null || !projectRoot.exists()) {
            DialogUtil.showError("No project", "Open a project first"); return;
        }
        Dialog<Void> dlg = DialogUtil.progressDialog("Project Size Analyzer", "Analyzing...");
        dlg.setResizable(true);
        dlg.getDialogPane().setPrefSize(800, 550);
        ProgressIndicator pi = new ProgressIndicator();
        dlg.getDialogPane().setContent(pi);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dlg.show();

        new Thread(() -> {
            Map<String, long[]> folderStats = new LinkedHashMap<>();
            analyzeProject(projectRoot, projectRoot, folderStats);
            Platform.runLater(() -> {
                ListView<String> listView = new ListView<>();
                long totalFiles = 0, totalSize = 0;
                for (Map.Entry<String, long[]> e : folderStats.entrySet()) {
                    listView.getItems().add(String.format("%-30s  %,6d files  %s", e.getKey(), e.getValue()[0], formatSize(e.getValue()[1])));
                    totalFiles += e.getValue()[0]; totalSize += e.getValue()[1];
                }
                Label header = new Label("Total: " + totalFiles + " files, " + formatSize(totalSize));
                header.setPadding(new Insets(6));
                VBox vb = new VBox(header, listView);
                VBox.setVgrow(listView, Priority.ALWAYS);
                dlg.getDialogPane().setContent(vb);
                dlg.setHeaderText("Project Size Analyzer");
                dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            });
        }).start();
    }

    private static void analyzeProject(File base, File dir, Map<String, long[]> stats) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                if (!f.getName().startsWith(".") && !f.getName().equals("node_modules")
                    && !f.getName().equals("dist") && !f.getName().equals("build")
                    && !f.getName().equals(".git")) analyzeProject(base, f, stats);
            } else {
                String folder = dir.equals(base) ? "(root)" : dir.getAbsolutePath().substring(base.getAbsolutePath().length() + 1);
                long[] arr = stats.computeIfAbsent(folder, k -> new long[2]);
                arr[0]++; arr[1] += f.length();
            }
        }
    }

    public static void showDuplicateFileFinder(File projectRoot) {
        if (projectRoot == null || !projectRoot.exists()) {
            DialogUtil.showError("No project", "Open a project first"); return;
        }
        Dialog<Void> dlg = DialogUtil.progressDialog("Duplicate File Finder", "Scanning...");
        dlg.setResizable(true);
        dlg.getDialogPane().setPrefSize(800, 550);
        ProgressIndicator pi = new ProgressIndicator();
        dlg.getDialogPane().setContent(pi);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dlg.show();

        new Thread(() -> {
            Map<String, List<File>> nameMap = new HashMap<>();
            collectByName(projectRoot, nameMap);
            List<String> dupes = new ArrayList<>();
            int count = 0;
            for (Map.Entry<String, List<File>> e : nameMap.entrySet()) {
                if (e.getValue().size() > 1) {
                    count++;
                    StringBuilder sb = new StringBuilder(e.getKey()).append(":\n");
                    for (File f : e.getValue())
                        sb.append("  - ").append(f.getAbsolutePath()).append(" (").append(formatSize(f.length())).append(")\n");
                    dupes.add(sb.toString());
                }
            }
            int finalCount = count;
            Platform.runLater(() -> {
                TextArea area = new TextArea(String.join("\n", dupes));
                area.setEditable(false);
                area.setStyle("-fx-font-family: monospace; -fx-font-size: 11px;");
                VBox vb = new VBox(new Label(finalCount + " duplicate file names found"), area);
                VBox.setVgrow(area, Priority.ALWAYS);
                dlg.getDialogPane().setContent(vb);
                dlg.setHeaderText("Duplicate File Finder");
                dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            });
        }).start();
    }

    private static void collectByName(File dir, Map<String, List<File>> nameMap) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                if (!f.getName().startsWith(".") && !f.getName().equals("node_modules")
                    && !f.getName().equals("dist") && !f.getName().equals("build")
                    && !f.getName().equals(".git")) collectByName(f, nameMap);
            } else {
                List<File> list = nameMap.computeIfAbsent(f.getName(), k -> new ArrayList<>());
                list.add(f);
            }
        }
    }
}
