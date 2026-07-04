package com.eagle.tools;

import com.eagle.editor.CodeEditor;
import com.eagle.editor.LanguageType;
import com.eagle.util.ThemeManager;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class ImportOptimizer {

    private static final Pattern JAVA_IMPORT = Pattern.compile("^import\\s+(?:static\\s+)?([\\w.]+\\*?)\\s*;");
    private static final Pattern PY_IMPORT = Pattern.compile("^(?:from\\s+([\\w.]+)\\s+)?import\\s+(.+)$");
    private static final Pattern JS_IMPORT = Pattern.compile(
        "^(?:import\\s+(?:\\{[^}]*\\}|\\*\\s+as\\s+\\w+|\\w+)(?:\\s*,\\s*\\{[^}]*\\})?\\s+from\\s+['\"]([^'\"]+)['\"]"
        + "|const\\s+(\\w+)\\s*=\\s*require\\s*\\(['\"]([^'\"]+)['\"]\\))");
    private static final Pattern CSS_IMPORT = Pattern.compile("@import\\s+['\"]([^'\"]+)['\"]");

    private final CodeEditor editor;
    private final String text;
    private final LanguageType language;
    private final List<ImportEntry> imports = new ArrayList<>();

    public ImportOptimizer(CodeEditor editor) {
        this.editor = editor;
        this.text = editor.getText();
        this.language = editor.getLanguage();
    }

    public List<ImportEntry> analyze() {
        imports.clear();
        if (text == null || text.isEmpty()) return imports;
        String[] lines = text.split("\n", -1);

        if (language == LanguageType.JAVA) {
            for (int i = 0; i < lines.length; i++) {
                Matcher m = JAVA_IMPORT.matcher(lines[i].trim());
                if (m.find()) {
                    String imp = m.group(1);
                    String simpleName = imp.contains(".") ? imp.substring(imp.lastIndexOf('.') + 1) : imp;
                    if (simpleName.endsWith("*")) {
                        imports.add(new ImportEntry(imp, lines[i].trim(), i, true));
                    } else {
                        boolean used = isUsedInBody(lines, i, simpleName);
                        imports.add(new ImportEntry(imp, lines[i].trim(), i, used, false));
                    }
                }
            }
        } else if (language == LanguageType.PYTHON) {
            for (int i = 0; i < lines.length; i++) {
                Matcher m = PY_IMPORT.matcher(lines[i].trim());
                if (m.find()) {
                    String fromPart = m.group(1);
                    String whatPart = m.group(2);
                    if (whatPart != null) {
                        String[] parts = whatPart.split(",");
                        for (String p : parts) {
                            p = p.trim().split("\\s+as\\s+")[0].trim();
                            String symbol = fromPart != null ? p : p.split("\\.")[0];
                            if (!symbol.isEmpty() && !symbol.equals("*")) {
                                boolean used = isUsedInBody(lines, i, symbol);
                                imports.add(new ImportEntry(symbol, lines[i].trim(), i, used, false));
                            }
                        }
                    }
                }
            }
        } else if (language == LanguageType.JAVASCRIPT || language == LanguageType.TYPESCRIPT
            || language == LanguageType.JSX || language == LanguageType.TSX) {
            for (int i = 0; i < lines.length; i++) {
                Matcher m = JS_IMPORT.matcher(lines[i].trim());
                if (m.find()) {
                    String path = m.group(1) != null ? m.group(1) : (m.group(3) != null ? m.group(3) : "");
                    String symbol = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
                    symbol = symbol.replaceAll("\\.\\w+$", "");
                    String importText = lines[i].trim();
                    String namedImports = extractNamedImports(importText);
                    if (namedImports != null && !namedImports.isEmpty()) {
                        String[] names = namedImports.split(",\\s*");
                        for (String name : names) {
                            name = name.trim().replaceAll("\\s+as\\s+\\w+", "").trim();
                            if (!name.isEmpty() && !name.equals("*")) {
                                boolean used = isUsedInBody(lines, i, name);
                                imports.add(new ImportEntry(name, importText, i, used, false));
                            }
                        }
                    } else if (importText.contains("import ")) {
                        String defaultName = importText.replaceAll("import\\s+(\\w+).*", "$1");
                        if (!defaultName.equals(importText)) {
                            boolean used = isUsedInBody(lines, i, defaultName);
                            imports.add(new ImportEntry(defaultName, importText, i, used, false));
                        }
                    }
                }
            }
        } else if (language == LanguageType.CSS || language == LanguageType.SCSS
            || language == LanguageType.LESS) {
            for (int i = 0; i < lines.length; i++) {
                Matcher m = CSS_IMPORT.matcher(lines[i].trim());
                if (m.find()) {
                    if (!isUsedInCss(lines, i, m.group(1))) {
                        imports.add(new ImportEntry(m.group(1), lines[i].trim(), i, false, false));
                    }
                }
            }
        }

        return imports;
    }

    private String extractNamedImports(String importText) {
        Pattern p = Pattern.compile("\\{\\s*([^}]+)\\s*}");
        Matcher m = p.matcher(importText);
        return m.find() ? m.group(1) : null;
    }

    public boolean isUsedInBody(String[] lines, int skipLine, String symbol) {
        for (int i = 0; i < lines.length; i++) {
            if (i == skipLine) continue;
            String line = lines[i];
            if (line.contains(symbol)) {
                return true;
            }
        }
        return false;
    }

    private boolean isUsedInCss(String[] lines, int skipLine, String path) {
        String name = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
        name = name.replaceAll("\\.\\w+$", "");
        if (name.isEmpty()) return true;
        for (int i = 0; i < lines.length; i++) {
            if (i == skipLine) continue;
            if (lines[i].contains(name)) return true;
        }
        return false;
    }

    public void showDialog() {
        analyze();

        Stage stage = new Stage();
        stage.setTitle("Import Optimizer - " + language);
        VBox root = new VBox(8);
        root.setPadding(new Insets(12));

        Label title = new Label("Import Analysis");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        int total = imports.size();
        long unused = imports.stream().filter(i -> !i.used).count();
        Label summary = new Label("Found " + total + " import(s), " + unused + " unused");
        summary.setStyle("-fx-font-size: 11px;");

        ListView<ImportEntry> listView = new ListView<>();
        listView.getItems().addAll(imports);
        listView.setCellFactory(lv -> new ListCell<ImportEntry>() {
            @Override
            protected void updateItem(ImportEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String icon = item.used ? "\u2713 " : "\u2717 ";
                    setText(icon + item.symbol + "  (line " + (item.line + 1) + ")");
                    if (!item.used) {
                        setStyle("-fx-text-fill: #e06c75;");
                    } else {
                        setStyle("-fx-text-fill: #98c379;");
                    }
                }
            }
        });
        VBox.setVgrow(listView, Priority.ALWAYS);

        HBox buttons = new HBox(8);
        Button removeUnused = new Button("Remove " + unused + " Unused Import(s)");
        removeUnused.setDisable(unused == 0);
        removeUnused.setOnAction(e -> {
            removeUnusedImports();
            stage.close();
        });
        Button closeBtn = new Button("Close");
        closeBtn.setOnAction(e -> stage.close());
        buttons.getChildren().addAll(removeUnused, closeBtn);

        root.getChildren().addAll(title, summary, listView, buttons);

        Scene scene = new Scene(root, 480, 360);
        ThemeManager.getInstance().applyTheme(scene);
        stage.setScene(scene);
        stage.show();
    }

    private void removeUnusedImports() {
        if (editor == null) return;
        String[] lines = text.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        List<Integer> removeLines = new ArrayList<>();
        for (ImportEntry ie : imports) {
            if (!ie.used) {
                removeLines.add(ie.line);
            }
        }
        for (int i = 0; i < lines.length; i++) {
            if (!removeLines.contains(i)) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(lines[i]);
            }
        }
        editor.setText(sb.toString());
    }

    public static class ImportEntry {
        public final String symbol;
        public final String lineText;
        public final int line;
        public final boolean used;
        public final boolean wildcard;

        public ImportEntry(String symbol, String lineText, int line, boolean used, boolean wildcard) {
            this.symbol = symbol;
            this.lineText = lineText;
            this.line = line;
            this.used = used;
            this.wildcard = wildcard;
        }

        public ImportEntry(String symbol, String lineText, int line, boolean wildcard) {
            this(symbol, lineText, line, true, wildcard);
        }
    }
}
