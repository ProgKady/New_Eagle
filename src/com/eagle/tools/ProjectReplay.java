package com.eagle.tools;

import com.eagle.util.ThemeManager;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

public class ProjectReplay {

    private static final String REPLAY_DIR = System.getProperty("user.home") + "/.webide/project-replay";

    public static void show(File projectRoot, Window owner) {
        if (projectRoot == null) return;
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.setTitle("Project Replay: " + projectRoot.getName());

        ListView<String> snapshotList = new ListView<>();
        snapshotList.setPrefHeight(300);

        TextArea diffArea = new TextArea();
        diffArea.setEditable(false);
        diffArea.setPrefRowCount(10);
        diffArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px;");

        Button recordBtn = new Button("Take Snapshot Now");
        recordBtn.setOnAction(e -> {
            takeSnapshot(projectRoot);
            refreshList(projectRoot, snapshotList);
        });

        Button compareBtn = new Button("Compare Selected");
        compareBtn.setOnAction(e -> {
            String sel = snapshotList.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            String ts = sel.substring(0, sel.indexOf(' '));
            showDiff(projectRoot, ts, diffArea);
        });

        refreshList(projectRoot, snapshotList);

        HBox controls = new HBox(8, recordBtn, compareBtn);
        SplitPane split = new SplitPane(snapshotList, diffArea);
        split.setDividerPositions(0.4);
        VBox.setVgrow(split, Priority.ALWAYS);

        VBox root = new VBox(6, new Label("Snapshots:"), controls, split);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 700, 500);
        ThemeManager.getInstance().applyTheme(scene);
        stage.setScene(scene);
        stage.show();
    }

    private static void takeSnapshot(File projectRoot) {
        try {
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            File snapDir = new File(REPLAY_DIR, projectRoot.getName() + "/" + ts);
            snapDir.mkdirs();
            copyRecursive(projectRoot, snapDir);
        } catch (Exception ignored) {}
    }

    private static void copyRecursive(File src, File dst) {
        File[] files = src.listFiles();
        if (files == null) return;
        for (File f : files) {
            String n = f.getName();
            if (n.startsWith(".") || "node_modules".equals(n) || "build".equals(n)
                || "dist".equals(n) || "__pycache__".equals(n) || ".git".equals(n)) continue;
            if (f.isDirectory()) {
                File sub = new File(dst, n);
                sub.mkdirs();
                copyRecursive(f, sub);
            } else if (f.length() < 100000) {
                try {
                    java.nio.file.Files.copy(f.toPath(), new File(dst, n).toPath());
                } catch (Exception ignored) {}
            }
        }
    }

    private static void refreshList(File projectRoot, ListView<String> list) {
        list.getItems().clear();
        File dir = new File(REPLAY_DIR, projectRoot.getName());
        if (!dir.exists()) return;
        File[] snaps = dir.listFiles();
        if (snaps == null) return;
        Arrays.sort(snaps, (a, b) -> b.getName().compareTo(a.getName()));
        for (File s : snaps) {
            String ts = s.getName();
            try {
                LocalDateTime dt = LocalDateTime.parse(ts, DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
                list.getItems().add(ts + "  " + dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            } catch (Exception e) {
                list.getItems().add(ts);
            }
        }
    }

    private static void showDiff(File projectRoot, String timestamp, TextArea diffArea) {
        File snapDir = new File(REPLAY_DIR, projectRoot.getName() + "/" + timestamp);
        if (!snapDir.exists()) { diffArea.setText("Snapshot not found."); return; }
        StringBuilder sb = new StringBuilder();
        sb.append("=== Snapshot: ").append(timestamp).append(" ===\n\n");
        File[] files = snapDir.listFiles();
        if (files != null) {
            for (File f : files) {
                File current = new File(projectRoot, f.getName());
                sb.append(f.getName()).append("  ");
                if (current.exists()) {
                    sb.append("(exists in project)  size: ").append(f.length()).append(" bytes");
                } else {
                    sb.append("(deleted/renamed)");
                }
                sb.append("\n");
            }
        }
        diffArea.setText(sb.toString());
    }
}