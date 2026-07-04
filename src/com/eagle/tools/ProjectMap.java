package com.eagle.tools;

import com.eagle.util.ThemeManager;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import java.io.File;

public class ProjectMap {

    public static void show(File projectRoot, Window owner) {
        if (projectRoot == null) return;
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.setTitle("Project Map: " + projectRoot.getName());

        TreeItem<String> root = new TreeItem<>(projectRoot.getName());
        root.setExpanded(true);
        buildTree(projectRoot, root);

        TreeView<String> tree = new TreeView<>(root);
        tree.setShowRoot(true);

        TextArea info = new TextArea();
        info.setEditable(false);
        info.setPrefRowCount(3);
        info.setText("Files: " + countFiles(projectRoot) + "  |  Folders: " + countDirs(projectRoot));

        VBox vbox = new VBox(6, new Label("Project Structure:"), tree, info);
        vbox.setPadding(new Insets(10));
        VBox.setVgrow(tree, Priority.ALWAYS);

        Scene scene = new Scene(vbox, 500, 600);
        ThemeManager.getInstance().applyTheme(scene);
        stage.setScene(scene);
        stage.show();
    }

    private static void buildTree(File dir, TreeItem<String> parent) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            String name = f.getName();
            if (name.startsWith(".") || "node_modules".equals(name) || "build".equals(name)
                || "dist".equals(name) || "__pycache__".equals(name) || ".git".equals(name)
                || "target".equals(name)) continue;
            TreeItem<String> item = new TreeItem<>(name);
            parent.getChildren().add(item);
            if (f.isDirectory()) {
                item.setExpanded(false);
                buildTree(f, item);
            }
        }
    }

    private static int countFiles(File dir) {
        int c = 0;
        File[] f = dir.listFiles();
        if (f == null) return 0;
        for (File x : f) if (x.isFile()) c++; else c += countFiles(x);
        return c;
    }

    private static int countDirs(File dir) {
        int c = 0;
        File[] f = dir.listFiles();
        if (f == null) return 0;
        for (File x : f) if (x.isDirectory()) { c++; c += countDirs(x); }
        return c;
    }
}