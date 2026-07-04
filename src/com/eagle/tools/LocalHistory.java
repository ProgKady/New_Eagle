package com.eagle.tools;

import com.eagle.util.ThemeManager;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

public class LocalHistory {

    private static final String HISTORY_DIR = System.getProperty("user.home") + "/.webide/local-history";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final int MAX_VERSIONS = 20;

    public static void saveSnapshot(File file) {
        if (file == null || !file.exists()) return;
        try {
            String rel = file.getAbsolutePath().replace(":", "").replace("\\", "/");
            File dir = new File(HISTORY_DIR, rel);
            dir.mkdirs();
            String ts = LocalDateTime.now().format(FMT);
            Files.copy(file.toPath(), new File(dir, ts + "_" + file.getName()).toPath(),
                StandardCopyOption.REPLACE_EXISTING);
            // Clean old
            File[] files = dir.listFiles((d, n) -> n.endsWith(file.getName()));
            if (files != null && files.length > MAX_VERSIONS) {
                Arrays.sort(files);
                for (int i = 0; i < files.length - MAX_VERSIONS; i++) files[i].delete();
            }
        } catch (Exception ignored) {}
    }

    public static void showHistory(File file, javafx.stage.Window owner) {
        if (file == null || !file.exists()) {
            showAlert(owner, "No file selected", "Open a file first to view its history.");
            return;
        }
        String rel = file.getAbsolutePath().replace(":", "").replace("\\", "/");
        File histDir = new File(HISTORY_DIR, rel);
        if (!histDir.exists() || histDir.list().length == 0) {
            showAlert(owner, "No History", "No saved versions for: " + file.getName());
            return;
        }
        File[] versions = histDir.listFiles((d, n) -> n.endsWith(file.getName()));
        if (versions == null || versions.length == 0) {
            showAlert(owner, "No History", "No saved versions for: " + file.getName());
            return;
        }
        Arrays.sort(versions, Collections.reverseOrder());
        ListView<String> list = new ListView<>();
        for (File v : versions) {
            String name = v.getName();
            String ts = name.length() > 15 ? name.substring(0, name.indexOf('_')) : name;
            try {
                LocalDateTime dt = LocalDateTime.parse(ts, FMT);
                list.getItems().add(dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "  (" + v.length() + " bytes)");
            } catch (Exception e) {
                list.getItems().add(v.getName());
            }
        }
        list.setPrefHeight(250);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Local History: " + file.getName());
        dialog.initOwner(owner);
        VBox root = new VBox(8, new Label("Select a version to restore:"), list);
        root.setPadding(new javafx.geometry.Insets(15));
        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().getButtonTypes().addAll(
            new ButtonType("Restore", ButtonBar.ButtonData.OK_DONE),
            ButtonType.CLOSE);
        dialog.setResultConverter(btn -> btn.getButtonData() == ButtonBar.ButtonData.OK_DONE ? ButtonType.YES : ButtonType.NO);
        dialog.getDialogPane().sceneProperty().addListener((obs, oldS, newS) -> {
            if (newS != null) ThemeManager.getInstance().applyTheme(newS);
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            int idx = list.getSelectionModel().getSelectedIndex();
            if (idx < 0) { showAlert(owner, "No Selection", "Select a version to restore."); return; }
            File src = versions[idx];
            try {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Restore version from " + list.getItems().get(idx) + "?", ButtonType.YES, ButtonType.NO);
                confirm.initOwner(owner);
                confirm.getDialogPane().sceneProperty().addListener((obs, oldS, newS) -> {
                    if (newS != null) ThemeManager.getInstance().applyTheme(newS);
                });
                if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
                saveSnapshot(file); // backup current
                Files.copy(src.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                showAlert(owner, "Restored", "Version restored successfully.");
            } catch (Exception e) {
                showAlert(owner, "Error", "Failed to restore: " + e.getMessage());
            }
        }
    }

    private static void showAlert(Window owner, String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.setTitle(title); a.setHeaderText(null);
        a.initOwner(owner);
        a.getDialogPane().sceneProperty().addListener((obs, oldS, newS) -> {
            if (newS != null) ThemeManager.getInstance().applyTheme(newS);
        });
        a.showAndWait();
    }
}
