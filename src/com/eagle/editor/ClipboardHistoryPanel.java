package com.eagle.editor;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class ClipboardHistoryPanel extends VBox {

    private final ListView<String> historyList = new ListView<>();
    private final LinkedHashSet<String> items = new LinkedHashSet<>();
    private static final int MAX_ITEMS = 100;
    private String lastClipboard = "";
    private TextField searchField;

    public ClipboardHistoryPanel() {
        setStyle("-fx-background-color: -bg-secondary;");
        Label header = new Label("Clipboard History");
        header.setStyle("-fx-font-weight: bold; -fx-padding: 8; -fx-font-size: 13px;");

        Button clearBtn = new Button("Clear");
        clearBtn.setTooltip(new Tooltip("Clear all history"));
        clearBtn.setOnAction(e -> { items.clear(); refresh(); });

        Button clearSelectedBtn = new Button("Remove");
        clearSelectedBtn.setTooltip(new Tooltip("Remove selected item"));
        clearSelectedBtn.setOnAction(e -> {
            String sel = historyList.getSelectionModel().getSelectedItem();
            if (sel != null) { items.remove(sel); refresh(); }
        });

        Button pinBtn = new Button("Pin");
        pinBtn.setTooltip(new Tooltip("Pin selected to top"));
        pinBtn.setOnAction(e -> {
            String sel = historyList.getSelectionModel().getSelectedItem();
            if (sel != null) {
                items.remove(sel);
                List<String> list = new ArrayList<>(items);
                list.add(0, sel);
                items.clear();
                items.addAll(list);
                refresh();
            }
        });

        HBox topBar = new HBox(8, header, clearBtn, clearSelectedBtn, pinBtn);
        topBar.setPadding(new Insets(4, 8, 4, 8));
        topBar.setStyle("-fx-border-color: -border-color; -fx-border-width: 0 0 1 0;");

        searchField = new TextField();
        searchField.setPromptText("Search clipboard history...");
        searchField.setStyle("-fx-font-size: 11px;");
        searchField.textProperty().addListener((obs, old, val) -> refresh());
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) searchField.clear();
        });

        historyList.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String display = item.length() > 80 ? item.substring(0, 80) + "..." : item;
                    setText(display.replace("\n", " ").replace("\r", ""));
                    setTooltip(new Tooltip(item));
                    // Pin icon for first item
                    if (getIndex() == 0) {
                        Label pinIcon = new Label("📌");
                        pinIcon.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-muted;");
                        setGraphic(pinIcon);
                    }
                }
            }
        });

        historyList.setOnMouseClicked(e -> {
            String sel = historyList.getSelectionModel().getSelectedItem();
            if (sel != null) {
                ClipboardContent cc = new ClipboardContent();
                cc.putString(sel);
                Clipboard.getSystemClipboard().setContent(cc);
                // Move to top on use
                items.remove(sel);
                List<String> list = new ArrayList<>(items);
                list.add(0, sel);
                items.clear();
                items.addAll(list);
                refresh();
                historyList.getSelectionModel().select(0);
            }
        });

        ContextMenu ctxMenu = new ContextMenu();
        MenuItem copyItem = new MenuItem("Copy");
        copyItem.setOnAction(e -> copySelected());
        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> {
            String sel = historyList.getSelectionModel().getSelectedItem();
            if (sel != null) { items.remove(sel); refresh(); }
        });
        MenuItem pinItem = new MenuItem("Pin to Top");
        pinItem.setOnAction(e -> pinSelected());
        ctxMenu.getItems().addAll(copyItem, pinItem, deleteItem);
        historyList.setContextMenu(ctxMenu);

        getChildren().addAll(topBar, searchField, historyList);
        VBox.setVgrow(historyList, Priority.ALWAYS);

        javafx.animation.PauseTransition pollTimer = new javafx.animation.PauseTransition(javafx.util.Duration.millis(1500));
        pollTimer.setOnFinished(e -> {
            if (getScene() != null && isVisible()) pollClipboard();
            pollTimer.playFromStart();
        });
        pollTimer.play();
    }

    public void addItem(String text) {
        if (text == null || text.isEmpty()) return;
        items.remove(text);
        items.add(text);
        if (items.size() > MAX_ITEMS) {
            List<String> list = new ArrayList<>(items);
            items.clear();
            items.addAll(list.subList(list.size() - MAX_ITEMS, list.size()));
        }
        refresh();
    }

    private void pollClipboard() {
        try {
            Clipboard cb = Clipboard.getSystemClipboard();
            if (cb.hasString()) {
                String content = cb.getString();
                if (content != null && !content.isEmpty() && !content.equals(lastClipboard)) {
                    lastClipboard = content;
                    addItem(content);
                }
            }
        } catch (Exception ignored) {}
    }

    private void refresh() {
        String filter = searchField.getText().toLowerCase().trim();
        List<String> all = new ArrayList<>(items);
        if (filter.isEmpty()) {
            historyList.getItems().setAll(all);
        } else {
            List<String> filtered = new ArrayList<>();
            for (String s : all) {
                if (s.toLowerCase().contains(filter)) filtered.add(s);
            }
            historyList.getItems().setAll(filtered);
        }
        ((Label) ((HBox) getChildren().get(0)).getChildren().get(0)).setText("Clipboard History (" + items.size() + ")");
    }

    private void copySelected() {
        String sel = historyList.getSelectionModel().getSelectedItem();
        if (sel != null) {
            ClipboardContent cc = new ClipboardContent();
            cc.putString(sel);
            Clipboard.getSystemClipboard().setContent(cc);
        }
    }

    private void pinSelected() {
        String sel = historyList.getSelectionModel().getSelectedItem();
        if (sel != null) {
            items.remove(sel);
            List<String> list = new ArrayList<>(items);
            list.add(0, sel);
            items.clear();
            items.addAll(list);
            refresh();
            historyList.getSelectionModel().select(0);
        }
    }
}
