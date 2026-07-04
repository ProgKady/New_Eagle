package com.eagle.editor;

import com.eagle.lsp.LspIntegration;
import com.eagle.lsp.LspIntegration.SymbolResult;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class CodeOutlinePanel extends VBox {

    private final TreeView<String> treeView = new TreeView<>();
    private final TreeItem<String> rootItem = new TreeItem<>("Outline");
    private CodeEditor editor;
    private LspIntegration lspIntegration;

    private static final Pattern JAVA_CLASS = Pattern.compile("(?:public|private|protected)?\\s*(?:static\\s+)?(?:abstract\\s+)?(?:class|interface|enum)\\s+(\\w+)");
    private static final Pattern JAVA_METHOD = Pattern.compile("(?:public|private|protected)?\\s*(?:static\\s+)?(?:synchronized\\s+)?(?:\\w+(?:<[^>]+>)?\\s+)?(\\w+)\\s*\\(([^)]*)\\)\\s*\\{?");
    private static final Pattern JAVA_FIELD = Pattern.compile("(?:public|private|protected)?\\s*(?:static\\s+)?(?:final\\s+)?(?:\\w+(?:<[^>]+>)?\\s+)(\\w+)\\s*(?:=|;)");
    private static final Pattern JS_FUNC = Pattern.compile("(?:function\\s+(\\w+)|(?:const|let|var)\\s+(\\w+)\\s*=\\s*(?:async\\s*)?\\(|class\\s+(\\w+))");
    private static final Pattern PY_CLASS = Pattern.compile("class\\s+(\\w+)\\s*[:({]");
    private static final Pattern PY_FUNC = Pattern.compile("(?:async\\s+)?def\\s+(\\w+)\\s*\\(");
    private static final Pattern C_FUNC = Pattern.compile("(?:\\w+\\s+)+(\\w+)\\s*\\([^)]*\\)\\s*\\{");

    public CodeOutlinePanel() {
        setSpacing(2);
        setPrefWidth(200);
        setMinWidth(150);
        getStyleClass().add("code-outline");

        Label title = new Label("Outline");
        title.setStyle("-fx-font-weight: bold; -fx-padding: 4 8; -fx-text-fill: #abb2bf; -fx-font-size: 11px;");
        getChildren().add(title);

        rootItem.setExpanded(true);
        treeView.setRoot(rootItem);
        treeView.setShowRoot(false);
        treeView.setPrefHeight(Double.MAX_VALUE);
        treeView.setStyle("-fx-background-color: transparent; -fx-font-size: 11px;");

        treeView.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if (val != null && val != rootItem) {
                Object lineObj = val.getValue();
                if (val.getParent() != null && val.getParent() != rootItem) {
                    Object parentLine = val.getParent().getValue();
                }
            }
        });

        treeView.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 1) {
                TreeItem<String> item = treeView.getSelectionModel().getSelectedItem();
                if (item != null && item != rootItem && editor != null) {
                    Integer line = (Integer) item.getGraphic().getUserData();
                    if (line == null && item.getParent() != null && !item.getParent().getValue().equals("Outline")) {
                        line = (Integer) item.getParent().getGraphic().getUserData();
                    }
                    if (line != null) {
                        editor.moveTo(line, 0);
                        editor.requestFocus();
                    }
                }
            }
        });

        ScrollPane scroll = new ScrollPane(treeView);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(scroll);
    }

    public void setEditor(CodeEditor ed) {
        this.editor = ed;
    }

    public void setLspIntegration(LspIntegration lsp) {
        this.lspIntegration = lsp;
    }

    public void refresh(String text, LanguageType language) {
        // Try LSP first
        if (lspIntegration != null && lspIntegration.isActive()) {
            List<SymbolResult> symbols = lspIntegration.getDocumentSymbols();
            if (symbols != null && !symbols.isEmpty()) {
                Platform.runLater(() -> updateFromLspSymbols(symbols));
                return;
            }
        }
        // Fallback to regex parser
        Platform.runLater(() -> updateFromParser(text, language));
    }

    private void updateFromLspSymbols(List<SymbolResult> symbols) {
        rootItem.getChildren().clear();
        for (SymbolResult s : symbols) {
            TreeItem<String> node = new TreeItem<>(iconForKind(s.kind) + " " + s.name);
            Label iconLabel = new Label();
            iconLabel.setUserData(s.startLine);
            node.setGraphic(iconLabel);
            rootItem.getChildren().add(node);
            if (s.children != null) {
                for (SymbolResult child : s.children) {
                    TreeItem<String> childNode = new TreeItem<>(iconForKind(child.kind) + " " + child.name);
                    Label childLabel = new Label();
                    childLabel.setUserData(child.startLine);
                    childNode.setGraphic(childLabel);
                    node.getChildren().add(childNode);
                }
                node.setExpanded(true);
            }
        }
    }

    private void updateFromParser(String text, LanguageType language) {
        rootItem.getChildren().clear();
        if (text == null || text.isEmpty()) return;
        String[] lines = text.split("\n", -1);

        if (language == LanguageType.JAVA) {
            parseJavaLines(lines);
        } else if (language == LanguageType.JAVASCRIPT || language == LanguageType.TYPESCRIPT
            || language == LanguageType.JSX || language == LanguageType.TSX) {
            parseJsLines(lines);
        } else if (language == LanguageType.PYTHON) {
            parsePythonLines(lines);
        } else if (language == LanguageType.C || language == LanguageType.CPP) {
            parseCLines(lines);
        } else if (language == LanguageType.KOTLIN) {
            parseJavaLines(lines);
        } else if (language == LanguageType.GO) {
            parseGoLines(lines);
        } else if (language == LanguageType.RUST) {
            parseRustLines(lines);
        } else if (language == LanguageType.PHP) {
            parsePhpLines(lines);
        } else if (false) {
            // placeholder for future language
        }
    }

    private TreeItem<String> addItem(String name, int line, TreeItem<String> parent) {
        TreeItem<String> item = new TreeItem<>(name);
        Label lbl = new Label();
        lbl.setUserData(line);
        item.setGraphic(lbl);
        if (parent != null) {
            parent.getChildren().add(item);
        } else {
            rootItem.getChildren().add(item);
        }
        return item;
    }

    private void parseJavaLines(String[] lines) {
        TreeItem<String> currentClass = null;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            Matcher cm = JAVA_CLASS.matcher(line);
            if (cm.find()) {
                currentClass = addItem(cm.group(1), i, null);
                continue;
            }
            Matcher mm = JAVA_METHOD.matcher(line);
            if (mm.find() && currentClass != null) {
                String sig = mm.group(1) + "(" + mm.group(2) + ")";
                addItem(sig, i, currentClass);
                continue;
            }
            Matcher fm = JAVA_FIELD.matcher(line);
            if (fm.find() && currentClass != null) {
                addItem(fm.group(1), i, currentClass);
            }
        }
        // Top-level methods/fields
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.contains("public static void main")) {
                addItem("main(String[])", i, null);
            }
        }
    }

    private void parseJsLines(String[] lines) {
        TreeItem<String> currentClass = null;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("class ")) {
                currentClass = addItem(line.replaceAll("class\\s+(\\w+).*", "$1"), i, null);
                continue;
            }
            if (line.contains("function ") && line.contains("(")) {
                String name = line.replaceAll(".*function\\s+(\\w+).*", "$1");
                if (name.contains("(")) name = name.substring(0, name.indexOf("("));
                if (currentClass != null) {
                    addItem(name + "()", i, currentClass);
                } else {
                    addItem(name + "()", i, null);
                }
                continue;
            }
            if ((line.startsWith("const ") || line.startsWith("let ") || line.startsWith("var "))
                && line.contains("=") && line.contains("(") && line.contains("=>")) {
                String name = line.replaceAll("(?:const|let|var)\\s+(\\w+).*", "$1");
                addItem(name + "()", i, currentClass);
            }
        }
    }

    private void parsePythonLines(String[] lines) {
        TreeItem<String> currentClass = null;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            Matcher cm = PY_CLASS.matcher(line);
            if (cm.find()) {
                currentClass = addItem(cm.group(1), i, null);
                continue;
            }
            Matcher fm = PY_FUNC.matcher(line);
            if (fm.find()) {
                if (currentClass != null) {
                    addItem(fm.group(1) + "()", i, currentClass);
                } else {
                    addItem(fm.group(1) + "()", i, null);
                }
            }
        }
    }

    private void parseCLines(String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("//") || line.startsWith("/*") || line.startsWith("*")) continue;
            if (line.contains("(") && line.contains(")") && line.contains("{")) {
                String name = line.replaceAll(".*?(\\w+)\\s*\\(.*", "$1");
                if (!name.contains(" ") && !name.equals(line)) {
                    addItem(name + "()", i, null);
                }
            }
        }
    }

    private void parseGoLines(String[] lines) {
        TreeItem<String> currentStruct = null;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("func ")) {
                String name = line.replaceAll("func\\s+(\\w+).*", "$1");
                addItem(name + "()", i, null);
            }
            if (line.startsWith("type ") && line.contains("struct")) {
                String name = line.replaceAll("type\\s+(\\w+).*", "$1");
                currentStruct = addItem(name, i, null);
            }
        }
    }

    private void parseRustLines(String[] lines) {
        TreeItem<String> currentImpl = null;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("fn ")) {
                String name = line.replaceAll("fn\\s+(\\w+).*", "$1");
                if (currentImpl != null) {
                    addItem(name + "()", i, currentImpl);
                } else {
                    addItem(name + "()", i, null);
                }
            }
            if (line.startsWith("struct ") || line.startsWith("enum ") || line.startsWith("trait ")) {
                String name = line.replaceAll("(?:struct|enum|trait)\\s+(\\w+).*", "$1");
                currentImpl = addItem(name, i, null);
            }
            if (line.startsWith("impl ")) {
                currentImpl = addItem(line.replaceAll("impl\\s+(\\w+).*", "$1"), i, null);
            }
        }
    }

    private void parsePhpLines(String[] lines) {
        TreeItem<String> currentClass = null;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("class ")) {
                currentClass = addItem(line.replaceAll("class\\s+(\\w+).*", "$1"), i, null);
            }
            if (line.contains("function ")) {
                String name = line.replaceAll(".*function\\s+(\\w+).*", "$1");
                if (currentClass != null) {
                    addItem(name + "()", i, currentClass);
                } else {
                    addItem(name + "()", i, null);
                }
            }
        }
    }

    private void parseSwiftLines(String[] lines) {
        TreeItem<String> currentClass = null;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("class ") || line.startsWith("struct ") || line.startsWith("enum ")) {
                currentClass = addItem(line.replaceAll("(?:class|struct|enum)\\s+(\\w+).*", "$1"), i, null);
            }
            if (line.startsWith("func ")) {
                String name = line.replaceAll("func\\s+(\\w+).*", "$1");
                if (currentClass != null) {
                    addItem(name + "()", i, currentClass);
                } else {
                    addItem(name + "()", i, null);
                }
            }
        }
    }

    private String iconForKind(int kind) {
        switch (kind) {
            case 1: return "\u2699"; // File / Module
            case 2: return "\u25A0"; // Namespace
            case 3: case 4: return "\u25A0"; // Package / Class
            case 5: return "\u25A0"; // Method
            case 6: return "\u25A0"; // Property
            case 7: return "\u25A0"; // Field
            case 8: return "\u25A0"; // Constructor
            case 9: return "\u25A0"; // Enum
            case 10: return "\u25A0"; // Interface
            case 11: return "\u25A0"; // Function
            case 12: return "\u25A0"; // Variable
            case 13: return "\u25A0"; // Constant
            case 14: return "\u25A0"; // String
            case 15: return "\u25A0"; // Number
            case 16: return "\u25A0"; // Boolean
            case 17: return "\u25A0"; // Array
            default: return "\u25CF";
        }
    }
}
