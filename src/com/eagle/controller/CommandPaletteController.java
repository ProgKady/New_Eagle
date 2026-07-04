package com.eagle.controller;

import com.eagle.editor.CompletionProvider;
import com.eagle.editor.LanguageType;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import com.eagle.icons.IconManager;
import java.util.*;
import java.util.function.Consumer;

public class CommandPaletteController {

    @FXML private VBox rootPane;
    @FXML private TextField searchField;
    @FXML private ListView<CommandItem> commandList;

    private final List<CommandItem> allItems = new ArrayList<>();
    private Consumer<CommandItem> onChosen;

    /** A command can be a snippet suggestion or an editor action. */
    public static class CommandItem {
        final String label;
        final String category;
        final String detail;
        final Runnable action;          // null for snippet items
        final String insertText;        // null for action items
        final String codePreview;       // optional preview text

        CommandItem(String label, String category, String detail, Runnable action) {
            this.label = label;
            this.category = category;
            this.detail = detail;
            this.action = action;
            this.insertText = null;
            this.codePreview = null;
        }

        CommandItem(String label, String category, String detail, String insertText, String codePreview) {
            this.label = label;
            this.category = category;
            this.detail = detail;
            this.action = null;
            this.insertText = insertText;
            this.codePreview = codePreview;
        }

        CompletionProvider.Suggestion toSuggestion() {
            return insertText != null
                ? new CompletionProvider.Suggestion(label, insertText, category, codePreview)
                : null;
        }
    }

    @FXML
    public void initialize() {
        commandList.setCellFactory(lv -> new ListCell<CommandItem>() {
            @Override
            protected void updateItem(CommandItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label icon = new Label(iconFor(item.category));
                    Label name = new Label(item.label);
                    name.setStyle("-fx-font-weight: bold;");
                    Label detail = new Label(item.detail != null ? "  " + item.detail : "");
                    detail.setStyle("-fx-text-fill: -text-secondary; -fx-font-size: 11px;");
                    setGraphic(new javafx.scene.layout.HBox(4, icon, name, detail));
                }
            }
        });

        searchField.textProperty().addListener((obs, old, val) -> filter(val));
        commandList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) chooseSelected();
        });
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DOWN) {
                commandList.getSelectionModel().selectNext();
                commandList.requestFocus();
            } else if (e.getCode() == KeyCode.ENTER) {
                chooseSelected();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                closeDialog();
            }
        });
        commandList.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) chooseSelected();
            if (e.getCode() == KeyCode.ESCAPE) closeDialog();
        });
    }

    public void loadCommands(LanguageType primaryLanguage, List<CommandItem> extraActions) {
        allItems.clear();

        // 1. Add editor actions at top
        if (extraActions != null) {
            for (CommandItem a : extraActions) {
                allItems.add(a);
            }
            allItems.add(null); // separator
        }

        // 2. Load snippet commands from ALL known languages
        LanguageType[] allLangs = LanguageType.values();
        Set<String> addedLabels = new HashSet<>();

        for (LanguageType lang : allLangs) {
            List<CompletionProvider.Suggestion> suggestions = CompletionProvider.getAllSuggestions(lang);
            for (CompletionProvider.Suggestion s : suggestions) {
                // Deduplicate by label across languages
                String key = s.category + "|" + s.label;
                if (addedLabels.contains(key)) continue;
                addedLabels.add(key);

                allItems.add(new CommandItem(
                    s.label, s.category, lang.name(),
                    s.insertText, s.codePreview
                ));
            }
        }

        commandList.getItems().setAll(allItems);
        if (!allItems.isEmpty()) commandList.getSelectionModel().selectFirst();
    }

    /** Convenience overload when no extra actions are needed. */
    public void loadCommands(LanguageType primaryLanguage) {
        loadCommands(primaryLanguage, null);
    }

    private void filter(String query) {
        if (query == null || query.trim().isEmpty()) {
            commandList.getItems().setAll(allItems);
            if (!allItems.isEmpty()) commandList.getSelectionModel().selectFirst();
            return;
        }
        String q = query.toLowerCase();
        List<CommandItem> filtered = new ArrayList<>();
        for (CommandItem item : allItems) {
            if (item == null) continue;
            if (item.label.toLowerCase().contains(q)
                || (item.detail != null && item.detail.toLowerCase().contains(q))
                || (item.category != null && item.category.toLowerCase().contains(q))) {
                filtered.add(item);
            }
        }
        commandList.getItems().setAll(filtered);
        if (!filtered.isEmpty()) commandList.getSelectionModel().selectFirst();
    }

    private String iconFor(String category) {
        if (category == null) return "\u2192";
        switch (category.toLowerCase()) {
            case "action":        return "\u2699";
            case "tag":           return "#";
            case "keyword":       return "kw";
            case "property":      return "pr";
            case "snippet":       return "sn";
            case "function":      return "fn";
            case "method":        return "md";
            case "attribute":     return "at";
            case "selector":      return "sl";
            case "directive":     return "di";
            case "variable":      return "vr";
            case "type":          return "tp";
            case "operator":      return "op";
            case "builtin":       return "bl";
            case "value":         return "vl";
            case "statement":     return "st";
            default:              return "\u2192";
        }
    }

    private void chooseSelected() {
        CommandItem selected = commandList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        if (selected.action != null) {
            selected.action.run();
        } else if (onChosen != null) {
            onChosen.accept(selected);
        }
        closeDialog();
    }

    public void setOnChosen(Consumer<CommandItem> callback) {
        this.onChosen = callback;
    }

    private void closeDialog() {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.close();
    }
}
