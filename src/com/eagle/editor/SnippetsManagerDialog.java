package com.eagle.editor;

import com.eagle.util.ThemeManager;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class SnippetsManagerDialog {

    private static CompletionProvider.Suggestion editingSuggestion;

    public static void show() {
        editingSuggestion = null;
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Snippets Manager");

        ComboBox<LanguageType> langCombo = new ComboBox<>();
        langCombo.getItems().addAll(CompletionProvider.getSupportedLanguages());
        langCombo.setValue(LanguageType.HTML);

        Label snippetCountLabel = new Label();
        snippetCountLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted; -fx-padding: 0 0 0 8;");

        ListView<CompletionProvider.Suggestion> listView = new ListView<>();
        listView.setPrefHeight(300);

        TextField searchField = new TextField();
        searchField.setPromptText("Search snippets...");

        Runnable updateCount = () -> {
            LanguageType lang = langCombo.getValue();
            if (lang == null) { snippetCountLabel.setText(""); return; }
            int total = CompletionProvider.getAllSuggestions(lang).size();
            snippetCountLabel.setText(total + " snippet" + (total == 1 ? "" : "s"));
        };

        langCombo.setOnAction(e -> {
            updateCount.run();
            refreshList(listView, langCombo.getValue(), searchField.getText().trim().toLowerCase());
        });
        searchField.textProperty().addListener((obs, o, n) -> refreshList(listView, langCombo.getValue(), n.trim().toLowerCase()));

        // Editor fields
        TextField labelField = new TextField();
        labelField.setPromptText("Label (shown in autocomplete)");
        TextField categoryField = new TextField();
        categoryField.setPromptText("Category (e.g. HTML Tag, JavaScript)");
        TextArea codeArea = new TextArea();
        codeArea.setPromptText("Snippet code to insert...\n\nUse {CURSOR} to set final cursor position.\nUse ${1:name}, ${2:value} for tab-placeholders.");
        codeArea.setPrefRowCount(6);
        TextField descField = new TextField();
        descField.setPromptText("Description (optional)");

        // Code preview (monospace read-only)
        TextArea previewArea = new TextArea();
        previewArea.setEditable(false);
        previewArea.setPrefRowCount(4);
        previewArea.setStyle("-fx-font-family: monospace; -fx-font-size: 11px; -fx-text-fill: -text-primary; -fx-control-inner-background: -bg-tertiary;");

        // Update preview when code changes
        codeArea.textProperty().addListener((obs, o, n) -> previewArea.setText(n));

        // Action buttons
        Button addBtn = new Button("Add");
        Button updateBtn = new Button("Update");
        Button deleteBtn = new Button("Delete Selected");
        Button clearBtn = new Button("Clear Form");

        addBtn.setOnAction(e -> {
            String label = labelField.getText().trim();
            String cat = categoryField.getText().trim();
            String code = codeArea.getText();
            LanguageType lang = langCombo.getValue();
            if (label.isEmpty() || cat.isEmpty() || code.isEmpty() || lang == null) return;
            String desc = descField.getText().trim();
            CompletionProvider.Suggestion s = new CompletionProvider.Suggestion(
                    label, code, cat, null, desc.isEmpty() ? null : desc, null, null);
            CompletionProvider.addSuggestion(lang, s);
            refreshList(listView, lang, searchField.getText().trim().toLowerCase());
            updateCount.run();
            clearForm(labelField, categoryField, codeArea, descField, previewArea);
        });

        updateBtn.setVisible(false);
        updateBtn.setOnAction(e -> {
            if (editingSuggestion == null) return;
            String label = labelField.getText().trim();
            String cat = categoryField.getText().trim();
            String code = codeArea.getText();
            LanguageType lang = langCombo.getValue();
            if (label.isEmpty() || cat.isEmpty() || code.isEmpty() || lang == null) return;
            String desc = descField.getText().trim();
            CompletionProvider.Suggestion updated = new CompletionProvider.Suggestion(
                    label, code, cat, null, desc.isEmpty() ? null : desc, null, null);
            CompletionProvider.removeSuggestion(lang, editingSuggestion);
            CompletionProvider.addSuggestion(lang, updated);
            editingSuggestion = null;
            updateBtn.setVisible(false);
            addBtn.setVisible(true);
            refreshList(listView, lang, searchField.getText().trim().toLowerCase());
            clearForm(labelField, categoryField, codeArea, descField, previewArea);
        });

        deleteBtn.setOnAction(e -> {
            CompletionProvider.Suggestion sel = listView.getSelectionModel().getSelectedItem();
            LanguageType lang = langCombo.getValue();
            if (sel != null && lang != null) {
                CompletionProvider.removeSuggestion(lang, sel);
                refreshList(listView, lang, searchField.getText().trim().toLowerCase());
                updateCount.run();
            }
        });

        clearBtn.setOnAction(e -> {
            editingSuggestion = null;
            updateBtn.setVisible(false);
            addBtn.setVisible(true);
            clearForm(labelField, categoryField, codeArea, descField, previewArea);
        });

        listView.setOnMouseClicked(e -> {
            CompletionProvider.Suggestion sel = listView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                labelField.setText(sel.label);
                categoryField.setText(sel.category);
                codeArea.setText(sel.insertText);
                previewArea.setText(sel.insertText);
                descField.setText(sel.description != null ? sel.description : "");
                editingSuggestion = sel;
                addBtn.setVisible(false);
                updateBtn.setVisible(true);
            }
        });

        // Import / Export buttons
        Button exportBtn = new Button("Export All");
        Button importBtn = new Button("Import");
        exportBtn.setOnAction(e -> exportSnippets(stage, langCombo.getValue()));
        importBtn.setOnAction(e -> {
            importSnippets(stage, langCombo.getValue(), listView, searchField);
            updateCount.run();
        });

        // Layout
        HBox topBar = new HBox(8, new Label("Language:"), langCombo, searchField, snippetCountLabel);
        topBar.setPadding(new Insets(8));

        VBox editor = new VBox(4,
                new Label("Snippet Editor:"),
                labelField, categoryField, descField, new Label("Code (use {CURSOR} for cursor position):"),
                codeArea,
                new Label("Preview:"),
                previewArea,
                new HBox(8, addBtn, updateBtn, deleteBtn, clearBtn));
        editor.setPadding(new Insets(8));

        SplitPane split = new SplitPane(listView, editor);
        split.setDividerPositions(0.4);

        // Placeholder help
        Label helpLabel = new Label(
            "Tip: Use {CURSOR} in snippet code to place cursor after insertion. "
            + "Use ${1:name}, ${2:value} for multi-cursor placeholders (Tab to jump)."
        );
        helpLabel.setWrapText(true);
        helpLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted; -fx-padding: 4 8;");

        Button closeBtn = new Button("Close");
        closeBtn.setOnAction(e -> stage.close());
        HBox bottom = new HBox(8, exportBtn, importBtn, new Region(), closeBtn);
        HBox.setHgrow(bottom.getChildren().get(2), Priority.ALWAYS);
        bottom.setPadding(new Insets(8));
        bottom.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(topBar, split, helpLabel, bottom);
        root.setPrefSize(800, 600);

        Scene scene = new Scene(root);
        ThemeManager.getInstance().applyTheme(scene);
        stage.setScene(scene);
        updateCount.run();
        refreshList(listView, langCombo.getValue(), "");
        stage.showAndWait();
    }

    private static void clearForm(TextField labelField, TextField categoryField, TextArea codeArea,
                                  TextField descField, TextArea previewArea) {
        labelField.clear(); categoryField.clear(); codeArea.clear();
        descField.clear(); previewArea.clear();
    }

    private static void refreshList(ListView<CompletionProvider.Suggestion> list, LanguageType lang, String filter) {
        list.getItems().clear();
        if (lang == null) return;
        List<CompletionProvider.Suggestion> all = CompletionProvider.getAllSuggestions(lang);
        if (filter == null || filter.isEmpty()) {
            list.getItems().addAll(all);
        } else {
            for (CompletionProvider.Suggestion s : all) {
                if (s.label.toLowerCase().contains(filter)
                    || s.category.toLowerCase().contains(filter)
                    || (s.insertText != null && s.insertText.toLowerCase().contains(filter))
                    || (s.description != null && s.description.toLowerCase().contains(filter))) {
                    list.getItems().add(s);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void exportSnippets(Stage owner, LanguageType lang) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Snippets");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
        fc.setInitialFileName("snippets.json");
        File file = fc.showSaveDialog(owner);
        if (file == null) return;
        try {
            JSONArray arr = new JSONArray();
            for (CompletionProvider.Suggestion s : CompletionProvider.getAllSuggestions(lang)) {
                JSONObject obj = new JSONObject();
                obj.put("label", s.label);
                obj.put("insertText", s.insertText);
                obj.put("category", s.category);
                if (s.description != null) obj.put("description", s.description);
                arr.add(obj);
            }
            sb.setLength(0);
            buildPrettyJson(arr, sb, 0);
            Files.write(file.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            showAlert("Export Error", "Failed to export: " + ex.getMessage());
        }
    }

    private static final StringBuilder sb = new StringBuilder();

    @SuppressWarnings("unchecked")
    private static void buildPrettyJson(Object value, StringBuilder out, int indent) {
        if (value instanceof JSONArray) {
            JSONArray arr = (JSONArray) value;
            if (arr.isEmpty()) { out.append("[]"); return; }
            out.append("[\n");
            for (int i = 0; i < arr.size(); i++) {
                repeat(out, "  ", indent + 1);
                buildPrettyJson(arr.get(i), out, indent + 1);
                if (i < arr.size() - 1) out.append(",");
                out.append("\n");
            }
            repeat(out, "  ", indent);
            out.append("]");
        } else if (value instanceof JSONObject) {
            JSONObject obj = (JSONObject) value;
            if (obj.isEmpty()) { out.append("{}"); return; }
            out.append("{\n");
            int i = 0;
            for (Object key : obj.keySet()) {
                repeat(out, "  ", indent + 1);
                out.append("\"").append(key).append("\": ");
                buildPrettyJson(obj.get(key), out, indent + 1);
                if (i++ < obj.size() - 1) out.append(",");
                out.append("\n");
            }
            repeat(out, "  ", indent);
            out.append("}");
        } else if (value instanceof String) {
            out.append("\"").append(escapeJson((String) value)).append("\"");
        } else {
            out.append(value);
        }
    }

    private static void repeat(StringBuilder out, String s, int n) {
        for (int i = 0; i < n; i++) out.append(s);
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    @SuppressWarnings("unchecked")
    private static void importSnippets(Stage owner, LanguageType lang,
                                       ListView<CompletionProvider.Suggestion> listView,
                                       TextField searchField) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Import Snippets");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
        File file = fc.showOpenDialog(owner);
        if (file == null) return;
        try {
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            JSONParser parser = new JSONParser();
            JSONArray arr = (JSONArray) parser.parse(content);
            if (arr.isEmpty()) { showAlert("Import", "No snippets found in file."); return; }

            // Ask user for import mode
            ChoiceDialog<String> modeDialog = new ChoiceDialog<>("Merge (update by label)",
                "Replace All", "Merge (update by label)", "Append (allow duplicates)");
            modeDialog.setTitle("Import Mode");
            modeDialog.setHeaderText("How to handle existing snippets? (" + arr.size() + " snippets in file)");
            modeDialog.setContentText("Choose import mode:");
            modeDialog.setOnShown(ev -> {
                Scene s = modeDialog.getDialogPane().getScene();
                if (s != null) ThemeManager.getInstance().applyTheme(s);
            });
            String mode = modeDialog.showAndWait().orElse(null);
            if (mode == null) return;

            int count = 0;
            if (mode.startsWith("Replace")) {
                // Clear all existing snippets for this language
                List<CompletionProvider.Suggestion> existing = new ArrayList<>(CompletionProvider.getAllSuggestions(lang));
                for (CompletionProvider.Suggestion s : existing) {
                    CompletionProvider.removeSuggestion(lang, s);
                }
            }

            for (int i = 0; i < arr.size(); i++) {
                JSONObject obj = (JSONObject) arr.get(i);
                String label = stringOr(obj, "label", "");
                String insertText = stringOr(obj, "insertText", "");
                String category = stringOr(obj, "category", "");
                String description = stringOr(obj, "description", null);
                if (label.isEmpty() || insertText.isEmpty() || category.isEmpty()) continue;
                CompletionProvider.Suggestion s = new CompletionProvider.Suggestion(
                        label, insertText, category, null, description, null, null);

                if (mode.startsWith("Merge")) {
                    // Remove existing snippet with same label
                    CompletionProvider.removeSuggestionByLabel(lang, label);
                }

                CompletionProvider.addSuggestion(lang, s);
                count++;
            }
            refreshList(listView, lang, searchField.getText().trim().toLowerCase());
            showAlert("Import Complete", count + " snippets imported (" + mode + ").");
        } catch (Exception ex) {
            showAlert("Import Error", "Failed to import: " + ex.getMessage());
        }
    }

    private static String stringOr(JSONObject obj, String key, String fallback) {
        Object v = obj.get(key);
        return v instanceof String ? (String) v : fallback;
    }

    private static void showAlert(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }
}
