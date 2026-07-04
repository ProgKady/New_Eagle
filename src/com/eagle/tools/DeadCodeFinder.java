package com.eagle.tools;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeadCodeFinder {

    public static class DeadSymbol {
        public final File file; public final int line;
        public final String name; public final String kind;
        public DeadSymbol(File f, int l, String n, String k) { file = f; line = l; name = n; kind = k; }
    }

    public static void show(File projectRoot) {
        if (projectRoot == null || !projectRoot.exists()) {
            DialogUtil.showError("No project", "Open a project first"); return;
        }

        Dialog<Void> dlg = DialogUtil.progressDialog("Dead Code Finder", "Scanning for unused code...");
        ProgressIndicator pi = new ProgressIndicator();
        dlg.getDialogPane().setContent(pi);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dlg.show();

        new Thread(() -> {
            try {
                List<File> files = new ArrayList<>();
                collectFiles(projectRoot, files);
                String allText = readAllFiles(files);
                List<DeadSymbol> symbols = extractSymbols(files);
                List<DeadSymbol> dead = findUnused(symbols, allText);
                Platform.runLater(() -> {
                    dlg.setHeaderText("Found " + dead.size() + " potentially unused symbols");
                    showResults(dead);
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
                    || name.endsWith(".py") || name.endsWith(".php")) files.add(f);
            }
        }
    }

    private static String readAllFiles(List<File> files) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (File f : files)
            sb.append(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8)).append("\n");
        return sb.toString();
    }

    private static List<DeadSymbol> extractSymbols(List<File> files) throws Exception {
        List<DeadSymbol> symbols = new ArrayList<>();
        Pattern javaClass = Pattern.compile("(?:public|private|protected)?\\s*(?:static\\s+)?(?:class|interface|enum)\\s+(\\w+)");
        Pattern javaMethod = Pattern.compile("(?:public|private|protected)?\\s*(?:static\\s+)?(?:\\w+\\s+)+(\\w+)\\s*\\([^)]*\\)\\s*\\{");
        Pattern javaField = Pattern.compile("(?:public|private|protected)?\\s*(?:static\\s+)?(?:final\\s+)?(\\w+(?:<[^>]*>)?)\\s+(\\w+)\\s*(?:;|=|,)");
        Pattern jsFunc = Pattern.compile("(?:function\\s+(\\w+)|(?:const|let|var)\\s+(\\w+)\\s*=\\s*(?:function|\\(|=>)|export\\s+(?:default\\s+)?(?:function|class)\\s+(\\w+))");
        Pattern pyDef = Pattern.compile("^\\s*def\\s+(\\w+)\\s*\\(|^\\s*class\\s+(\\w+)\\s*[:(\\(]");

        for (File f : files) {
            String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            String[] lines = content.split("\n");
            String ext = f.getName().toLowerCase();
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (ext.endsWith(".java")) {
                    Matcher cm = javaClass.matcher(line);
                    if (cm.find()) symbols.add(new DeadSymbol(f, i + 1, cm.group(1), "class"));
                    Matcher mm = javaMethod.matcher(line);
                    if (mm.find()) {
                        String name = mm.group(1);
                        if (!name.equals("if") && !name.equals("for") && !name.equals("while")
                            && !name.equals("switch") && !name.equals("catch") && !name.equals("try")
                            && !name.equals("synchronized") && name.length() > 1)
                            symbols.add(new DeadSymbol(f, i + 1, name, "method"));
                    }
                    Matcher fm = javaField.matcher(line);
                    if (fm.find() && !fm.group(1).matches("(if|for|while|return|new|this|super|import|package)"))
                        symbols.add(new DeadSymbol(f, i + 1, fm.group(2), "field"));
                } else if (ext.endsWith(".js") || ext.endsWith(".ts")) {
                    Matcher jm = jsFunc.matcher(line);
                    while (jm.find()) {
                        for (int g = 1; g <= jm.groupCount(); g++) {
                            if (jm.group(g) != null) symbols.add(new DeadSymbol(f, i + 1, jm.group(g), "function"));
                        }
                    }
                } else if (ext.endsWith(".py")) {
                    Matcher pm = pyDef.matcher(line);
                    if (pm.find()) {
                        if (pm.group(1) != null) symbols.add(new DeadSymbol(f, i + 1, pm.group(1), "def"));
                        if (pm.group(2) != null) symbols.add(new DeadSymbol(f, i + 1, pm.group(2), "class"));
                    }
                }
            }
        }
        return symbols;
    }

    private static List<DeadSymbol> findUnused(List<DeadSymbol> symbols, String allText) {
        List<DeadSymbol> dead = new ArrayList<>();
        for (DeadSymbol s : symbols) {
            if (s.name.equals("main") || s.name.equals("init") || s.name.equals("toString")
                || s.name.equals("equals") || s.name.equals("hashCode") || s.name.length() <= 1) continue;
            int count = 0, idx = 0;
            while ((idx = allText.indexOf(s.name, idx)) != -1) { count++; idx += s.name.length(); }
            if (count <= 1) dead.add(s);
        }
        return dead;
    }

    private static void showResults(List<DeadSymbol> dead) {
        Dialog<Void> dlg = DialogUtil.progressDialog("Dead Code Finder Results", "");
        dlg.setResizable(true);
        dlg.getDialogPane().setPrefSize(700, 500);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        ListView<DeadSymbol> listView = new ListView<>();
        listView.setCellFactory(lv -> new ListCell<DeadSymbol>() {
            @Override
            protected void updateItem(DeadSymbol item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(String.format("%-10s | %s  | %s:%d", item.kind, item.name, item.file.getName(), item.line));
            }
        });
        listView.getItems().addAll(dead);

        TextArea detailArea = new TextArea();
        detailArea.setEditable(false);
        detailArea.setStyle("-fx-font-family: monospace; -fx-font-size: 11px;");
        listView.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) detailArea.setText("Type: " + n.kind + "\nName: " + n.name + "\nFile: " + n.file.getAbsolutePath() + "\nLine: " + n.line);
        });

        SplitPane split = new SplitPane(listView, detailArea);
        split.setDividerPositions(0.5);
        dlg.getDialogPane().setContent(split);
        dlg.showAndWait();
    }
}
