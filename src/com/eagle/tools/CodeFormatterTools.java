package com.eagle.tools;

import com.eagle.util.ThemeManager;

public class CodeFormatterTools {

    public static void formatXml(String text) {
        try {
            StringBuilder sb = new StringBuilder();
            int indent = 0;
            boolean inTag = false;
            StringBuilder tag = new StringBuilder();
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '<') { inTag = true; tag.setLength(0); tag.append(c); }
                else if (c == '>') { tag.append(c); inTag = false; processTag(sb, tag.toString(), indent); }
                else if (inTag) { tag.append(c); }
                else { sb.append(c); }
            }
            DialogUtil.showResult("XML Formatted", sb.toString().trim());
        } catch (Exception e) {
            DialogUtil.showError("Error", "Failed to format XML: " + e.getMessage());
        }
    }

    private static void processTag(StringBuilder sb, String tag, int indent) {
        String trimmed = tag.trim();
        if (trimmed.startsWith("<?") || trimmed.startsWith("<!") || trimmed.startsWith("<![CDATA[")) {
            sb.append(trimmed).append("\n"); return;
        }
        if (trimmed.startsWith("</")) { repeat(sb, "  ", indent - 1); sb.append(trimmed).append("\n"); }
        else if (trimmed.endsWith("/>")) { repeat(sb, "  ", indent); sb.append(trimmed).append("\n"); }
        else if (trimmed.startsWith("<")) { repeat(sb, "  ", indent); sb.append(trimmed).append("\n"); }
    }

    private static int currentIndent;
    private static boolean inBlock;

    public static void formatCss(String text) {
        try {
            StringBuilder sb = new StringBuilder();
            currentIndent = 0;
            inBlock = false;
            int i = 0;
            while (i < text.length()) {
                char c = text.charAt(i);
                if (c == '{') { sb.append(" {\n"); currentIndent++; inBlock = true; i++; }
                else if (c == '}') { currentIndent--; if (currentIndent < 0) currentIndent = 0; sb.append("}\n\n"); inBlock = false; i++; }
                else if (c == ';' && inBlock) { sb.append(";\n"); repeat(sb, "  ", currentIndent); i++; }
                else if (c == '\n' || c == '\r') { i++; }
                else if (c == '/' && i + 1 < text.length() && text.charAt(i + 1) == '*') {
                    int end = text.indexOf("*/", i + 2);
                    if (end == -1) end = text.length() - 2;
                    sb.append("/* ").append(text.substring(i + 2, end).trim()).append(" */\n");
                    i = end + 2;
                } else {
                    if (!inBlock) sb.append(c); else sb.append(c);
                    i++;
                }
            }
            DialogUtil.showResult("CSS Formatted", sb.toString().trim());
        } catch (Exception e) {
            DialogUtil.showError("Error", "Failed to format CSS: " + e.getMessage());
        }
    }

    public static void formatSql(String text) {
        try {
            String[] keywords = {"SELECT", "FROM", "WHERE", "AND", "OR", "JOIN", "LEFT JOIN",
                "RIGHT JOIN", "INNER JOIN", "OUTER JOIN", "ON", "ORDER BY", "GROUP BY",
                "HAVING", "LIMIT", "OFFSET", "INSERT INTO", "VALUES", "UPDATE", "SET",
                "DELETE FROM", "CREATE TABLE", "ALTER TABLE", "DROP TABLE", "CREATE INDEX",
                "UNION", "UNION ALL", "INTO", "AS", "DISTINCT", "COUNT", "SUM", "AVG",
                "MAX", "MIN", "BETWEEN", "IN", "LIKE", "IS NULL", "IS NOT NULL",
                "NOT IN", "EXISTS", "CASE", "WHEN", "THEN", "ELSE", "END", "BEGIN",
                "COMMIT", "ROLLBACK", "PRIMARY KEY", "FOREIGN KEY", "REFERENCES",
                "INDEX", "UNIQUE", "NOT NULL", "DEFAULT", "CHECK"};

            String[] parts = text.split("(?i)\\b(" + String.join("|", keywords) + ")\\b");
            int keywordStart = 0;
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                int kwIdx = text.indexOf(part, keywordStart);
                if (kwIdx > keywordStart) {
                    String kw = text.substring(keywordStart, kwIdx).trim();
                    if (!kw.isEmpty()) sb.append(kw.toUpperCase()).append("\n    ");
                }
                sb.append(part);
                keywordStart = kwIdx + part.length();
            }

            String result = sb.toString()
                .replaceAll("\\s*\n\\s*", "\n")
                .replaceAll("\\n\\s*,\\s*", ",\n    ")
                .trim();

            DialogUtil.showResult("SQL Formatted", result);
        } catch (Exception e) {
            DialogUtil.showError("Error", "Failed to format SQL: " + e.getMessage());
        }
    }

    public static void formatYaml(String text) {
        try {
            StringBuilder sb = new StringBuilder();
            int indent = 0;
            boolean inList = false;
            for (String line : text.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) { sb.append("\n"); continue; }
                if (trimmed.startsWith("- ")) { if (!inList) indent++; inList = true; }
                else { inList = false; if (trimmed.endsWith(":")) indent++; }
                repeat(sb, "  ", Math.max(0, indent - (inList ? 1 : 0)));
                sb.append(trimmed).append("\n");
            }
            DialogUtil.showResult("YAML Formatted", sb.toString().trim());
        } catch (Exception e) {
            DialogUtil.showError("Error", "Failed to format YAML: " + e.getMessage());
        }
    }

    public static void formatJson(String text) {
        try {
            text = text.trim();
            StringBuilder sb = new StringBuilder();
            int indent = 0;
            boolean inString = false;
            boolean escaped = false;
            int jsonDepth = 0;

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (escaped) { sb.append(c); escaped = false; continue; }
                if (c == '\\' && inString) { sb.append(c); escaped = true; continue; }
                if (c == '"') { inString = !inString; sb.append(c); continue; }
                if (inString) { sb.append(c); continue; }

                if (c == '{' || c == '[') {
                    sb.append(c).append("\n");
                    indent++;
                    repeat(sb, "  ", indent);
                } else if (c == '}' || c == ']') {
                    sb.append("\n");
                    indent--;
                    if (indent < 0) indent = 0;
                    repeat(sb, "  ", indent);
                    sb.append(c);
                } else if (c == ',') {
                    sb.append(c).append("\n");
                    repeat(sb, "  ", indent);
                } else if (c == ':') {
                    sb.append(" : ");
                } else if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                    // skip whitespace
                } else {
                    sb.append(c);
                }
            }

            DialogUtil.showResult("JSON Formatted", sb.toString().trim());
        } catch (Exception e) {
            DialogUtil.showError("Error", "Failed to format JSON: " + e.getMessage());
        }
    }

    public static String formatJava(String text) {
        if (text == null || text.isEmpty()) return text;
        StringBuilder sb = new StringBuilder();
        int indent = 0;
        boolean inBlockComment = false;
        boolean inLineComment = false;
        boolean inString = false;
        boolean inChar = false;
        boolean prevNewline = true;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            char next = i + 1 < text.length() ? text.charAt(i + 1) : 0;

            // Strings and chars
            if (c == '"' && !inBlockComment && !inLineComment && !inChar && (i == 0 || text.charAt(i - 1) != '\\')) {
                inString = !inString;
                sb.append(c); continue;
            }
            if (c == '\'' && !inBlockComment && !inLineComment && !inString && (i == 0 || text.charAt(i - 1) != '\\')) {
                inChar = !inChar;
                sb.append(c); continue;
            }
            if (inString || inChar) { sb.append(c); continue; }

            // Comments
            if (c == '/' && next == '*' && !inLineComment) { inBlockComment = true; sb.append("/*"); i++; continue; }
            if (c == '*' && next == '/' && inBlockComment) { inBlockComment = false; sb.append("*/"); i++; continue; }
            if (inBlockComment) { sb.append(c); continue; }

            if (c == '/' && next == '/' && !inBlockComment) { inLineComment = true; sb.append("//"); i++; continue; }
            if (inLineComment) {
                if (c == '\n') { inLineComment = false; sb.append("\n"); prevNewline = true; continue; }
                sb.append(c); continue;
            }

            // Newlines
            if (c == '\n' || c == '\r') {
                if (c == '\r' && next == '\n') { i++; }
                sb.append("\n");
                prevNewline = true;
                continue;
            }

            // Opening braces
            if (c == '{') {
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ' && sb.charAt(sb.length() - 1) != '\n') {
                    sb.append(" ");
                }
                sb.append("{").append("\n");
                indent++;
                prevNewline = true;
                continue;
            }

            // Closing braces
            if (c == '}') {
                sb.append("\n");
                indent--; if (indent < 0) indent = 0;
                repeat(sb, "  ", indent);
                sb.append("}");
                prevNewline = false;
                continue;
            }

            // Semicolons
            if (c == ';') {
                sb.append(";\n");
                prevNewline = true;
                continue;
            }

            // Commas
            if (c == ',') {
                sb.append(", ");
                continue;
            }

            // Clean up extra spaces
            if (prevNewline) {
                if (c == ' ') continue;
                if (c == '\t') continue;
                repeat(sb, "  ", indent);
                prevNewline = false;
            }

            sb.append(c);
        }

        // Post-process: fix common patterns
        String result = sb.toString()
            .replaceAll("\\n{3,}", "\n\n")
            .replaceAll("\\s+\\n", "\n")
            .replaceAll("\\n\\s*\\.", "\n.")
            .replaceAll("enum\\s*\\{", "enum {")
            .trim();

        return result;
    }

    public static void formatJavaDialog(String text) {
        try {
            String result = formatJava(text);
            DialogUtil.showResult("Java Formatted", result);
        } catch (Exception e) {
            DialogUtil.showError("Error", "Failed to format Java: " + e.getMessage());
        }
    }

    public static String formatJavaScript(String text) {
        if (text == null || text.isEmpty()) return text;
        // JS follows same rules as Java mostly
        return formatJava(text);
    }

    public static void formatJavaScriptDialog(String text) {
        try {
            String result = formatJavaScript(text);
            DialogUtil.showResult("JavaScript Formatted", result);
        } catch (Exception e) {
            DialogUtil.showError("Error", "Failed to format JavaScript: " + e.getMessage());
        }
    }

    public static void showDialog(javafx.stage.Window owner) {
        javafx.stage.Stage stage = new javafx.stage.Stage();
        stage.initOwner(owner);
        stage.setTitle("Code Formatter");

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(8);
        root.setPadding(new javafx.geometry.Insets(12));

        javafx.scene.control.Label instr = new javafx.scene.control.Label("Paste code below, select language, and click Format.");
        instr.setWrapText(true);

        javafx.scene.control.ComboBox<String> langBox = new javafx.scene.control.ComboBox<>();
        langBox.getItems().addAll("Java", "JavaScript", "JSON", "XML", "CSS", "SQL", "YAML");
        langBox.setValue("Java");

        javafx.scene.control.TextArea input = new javafx.scene.control.TextArea();
        input.setPromptText("Paste your code here...");
        input.setPrefRowCount(10);
        input.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px;");

        javafx.scene.control.TextArea output = new javafx.scene.control.TextArea();
        output.setPromptText("Formatted output...");
        output.setPrefRowCount(10);
        output.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px;");
        output.setEditable(false);

        javafx.scene.control.Button formatBtn = new javafx.scene.control.Button("Format");
        formatBtn.setStyle("-fx-background-color: -accent; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 6 14; -fx-background-radius: 4;");
        formatBtn.setOnAction(ev -> {
            String text = input.getText();
            if (text.isEmpty()) { output.setText("// No input"); return; }
            String lang = langBox.getValue();
            try {
                switch (lang) {
                    case "Java": output.setText(formatJava(text)); break;
                    case "JavaScript": output.setText(formatJavaScript(text)); break;
                    case "JSON": formatJson(text); break;
                    case "XML": formatXml(text); break;
                    case "CSS": formatCss(text); break;
                    case "SQL": formatSql(text); break;
                    case "YAML": formatYaml(text); break;
                }
                if (!lang.equals("JSON") && !lang.equals("XML") && !lang.equals("CSS") && !lang.equals("SQL") && !lang.equals("YAML")) {
                    // output already set
                }
            } catch (Exception e) {
                output.setText("Error: " + e.getMessage());
            }
        });

        javafx.scene.control.Button copyBtn = new javafx.scene.control.Button("Copy Output");
        copyBtn.setStyle("-fx-cursor: hand; -fx-font-size: 11px;");
        copyBtn.setOnAction(ev -> {
            String out = output.getText();
            if (!out.isEmpty()) {
                javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
                cc.putString(out);
                javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
            }
        });

        javafx.scene.layout.HBox bottomBar = new javafx.scene.layout.HBox(6, formatBtn, copyBtn);
        bottomBar.setAlignment(javafx.geometry.Pos.CENTER);

        root.getChildren().addAll(instr, langBox, input, output, bottomBar);

        javafx.scene.Scene scene = new javafx.scene.Scene(root, 560, 520);
        ThemeManager.getInstance().applyTheme(scene);
        stage.setScene(scene);
        stage.show();
    }

    private static void repeat(StringBuilder sb, String s, int n) {
        for (int i = 0; i < n; i++) sb.append(s);
    }
}
