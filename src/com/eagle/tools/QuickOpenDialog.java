package com.eagle.tools;

import com.eagle.util.ThemeManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.*;
import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class QuickOpenDialog {

    public static void show(File projectRoot, Consumer<File> onOpen, Window owner) {
        if (projectRoot == null) return;
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Quick Open (Ctrl+P)");

        TextField searchField = new TextField();
        searchField.setPromptText("Type file name...");
        searchField.setStyle("-fx-font-size: 14px; -fx-padding: 8;");

        ListView<String> list = new ListView<>();
        list.setPrefHeight(350);
        list.setPrefWidth(500);

        List<File> allFiles = new ArrayList<>();
        collectFiles(projectRoot, allFiles);

        List<String> filePaths = allFiles.stream()
            .map(f -> projectRoot.toURI().relativize(f.toURI()).getPath())
            .collect(Collectors.toList());
        list.setItems(FXCollections.observableArrayList(filePaths));

        searchField.textProperty().addListener((o, ov, nv) -> {
            String q = nv.toLowerCase().trim();
            if (q.isEmpty()) {
                list.setItems(FXCollections.observableArrayList(filePaths));
            } else {
                List<String> filtered = filePaths.stream()
                    .filter(p -> p.toLowerCase().contains(q))
                    .collect(Collectors.toList());
                list.setItems(FXCollections.observableArrayList(filtered));
            }
        });

        list.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String sel = list.getSelectionModel().getSelectedItem();
                if (sel != null) selectFile(allFiles, filePaths, sel, onOpen, stage);
            }
        });
        list.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                String sel = list.getSelectionModel().getSelectedItem();
                if (sel != null) selectFile(allFiles, filePaths, sel, onOpen, stage);
            }
        });

        BorderPane root = new BorderPane();
        root.setTop(searchField);
        root.setCenter(list);
        BorderPane.setMargin(searchField, new Insets(8));
        root.setPrefSize(520, 400);

        Scene scene = new Scene(root);
        ThemeManager.getInstance().applyTheme(scene);
        stage.setScene(scene);
        stage.show();
        Platform.runLater(searchField::requestFocus);
    }

    private static void collectFiles(File dir, List<File> results) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                if (!f.getName().startsWith(".") && !f.getName().equals("node_modules")
                    && !f.getName().equals("build") && !f.getName().equals("dist")
                    && !f.getName().equals("__pycache__") && !f.getName().equals(".git")
                    && !f.getName().equals("target")) {
                    collectFiles(f, results);
                }
            } else {
                results.add(f);
            }
        }
    }

    private static void selectFile(List<File> allFiles, List<String> paths, String sel, Consumer<File> onOpen, Stage stage) {
        int idx = paths.indexOf(sel);
        if (idx >= 0 && idx < allFiles.size()) {
            onOpen.accept(allFiles.get(idx));
            stage.close();
        }
    }
}
