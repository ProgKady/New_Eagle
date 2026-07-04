package com.eagle.controller;

import com.eagle.editor.CodeEditor;
import com.eagle.editor.LanguageType;
import com.eagle.plugin.PluginInfo;
import com.eagle.plugin.PluginManager;
import com.eagle.icons.IconManager;
import com.eagle.util.ThemeManager;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class PluginManagerController {

    @FXML private ListView<PluginInfo> pluginList;
    @FXML private VBox detailPane;
    @FXML private Button enableBtn;
    @FXML private Button disableBtn;
    @FXML private Button editBtn;
    @FXML private Button deleteBtn;

    @FXML
    public void initialize() {
        pluginList.setCellFactory(lv -> new ListCell<PluginInfo>() {
            @Override
            protected void updateItem(PluginInfo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    boolean builtin = PluginManager.getInstance().isBuiltin(item.getId());
                    String tag = builtin ? " [B]" : "";
                    String icon = item.isEnabled() ? "[+]" : "[-]";
                    setText(icon + item.getName() + "  v" + item.getVersion() + tag);
                }
            }
        });

        java.util.List<PluginInfo> all = PluginManager.getInstance().getAllPlugins();
        pluginList.setItems(javafx.collections.FXCollections.observableArrayList(all));

        pluginList.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) showDetails(sel);
        });

        if (!all.isEmpty()) {
            pluginList.getSelectionModel().select(0);
        }
    }

    private void showDetails(PluginInfo info) {
        detailPane.getChildren().clear();

        Label name = new Label(info.getName());
        name.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        Label id = new Label("ID: " + info.getId());
        id.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted;");

        Label version = new Label("Version: " + info.getVersion());
        Label author = new Label("Author: " + info.getAuthor());

        boolean builtin = PluginManager.getInstance().isBuiltin(info.getId());
        Label type = new Label(builtin ? "Type: Built-in" : "Type: External JAR");
        type.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted;");

        Label desc = new Label(info.getDescription());
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 12px;");

        Label status = new Label(info.isEnabled() ? "Enabled" : "Disabled");
        status.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (info.isEnabled() ? "-success" : "-warning") + ";");

        detailPane.getChildren().addAll(name, id, version, author, type,
            new javafx.scene.control.Separator(), desc, status);

        enableBtn.setDisable(info.isEnabled());
        disableBtn.setDisable(!info.isEnabled());

        File src = PluginManager.getInstance().getBuiltinSourceFile(info.getId());
        editBtn.setDisable(src == null);
        editBtn.setText(builtin ? "Edit Source" : "View Source");

        deleteBtn.setDisable(false);
    }

    @FXML
    private void onEnable() {
        PluginInfo sel = pluginList.getSelectionModel().getSelectedItem();
        if (sel != null) {
            PluginManager.getInstance().enablePlugin(sel.getId());
            pluginList.refresh();
            showDetails(sel);
        }
    }

    @FXML
    private void onDisable() {
        PluginInfo sel = pluginList.getSelectionModel().getSelectedItem();
        if (sel != null) {
            PluginManager.getInstance().disablePlugin(sel.getId());
            pluginList.refresh();
            showDetails(sel);
        }
    }

    @FXML
    private void onEdit() {
        PluginInfo sel = pluginList.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        File srcFile = PluginManager.getInstance().getBuiltinSourceFile(sel.getId());
        if (srcFile == null) {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("Source Not Available");
            a.setHeaderText(null);
            a.setContentText("Source code is not available for this plugin.\n\n"
                + "External JAR plugins only contain compiled classes.\n"
                + "Built-in plugins whose .java file was not found also cannot be edited.");
            a.showAndWait();
            return;
        }

        List<File> relatedFiles = PluginManager.getInstance().getBuiltinPluginFiles(sel.getId());
        showPluginSourceEditor(sel.getName(), srcFile, relatedFiles);
    }

    private void showPluginSourceEditor(String pluginName, File primaryFile, List<File> allFiles) {
        Stage stage = new Stage();
        stage.setTitle("Plugin Source: " + pluginName);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(pluginList.getScene().getWindow());

        // --- File Tree (left) ---
        TreeItem<String> rootItem = new TreeItem<>(pluginName);
        rootItem.setExpanded(true);
        Map<TreeItem<String>, File> treeFileMap = new HashMap<>();

        for (File f : allFiles) {
            TreeItem<String> item = new TreeItem<>(f.getName());
            treeFileMap.put(item, f);
            rootItem.getChildren().add(item);
        }

        TreeView<String> fileTree = new TreeView<>(rootItem);
        fileTree.setPrefWidth(240);
        fileTree.setMinWidth(180);
        fileTree.setShowRoot(true);

        // --- Editor Tabs (right) ---
        TabPane editorTabs = new TabPane();
        Map<File, CodeEditor> openEditors = new HashMap<>();

        // Open primary file by default
        openFileInTab(primaryFile, editorTabs, openEditors);

        // When tree item is clicked, open the file in a tab
        fileTree.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null && treeFileMap.containsKey(sel)) {
                File f = treeFileMap.get(sel);
                openFileInTab(f, editorTabs, openEditors);
            }
        });

        // --- Status bar ---
        Label statusBar = new Label("File: " + primaryFile.getAbsolutePath());
        statusBar.setStyle("-fx-font-size: 11px; -fx-padding: 4 8; -fx-background-color: -bg-secondary; -fx-text-fill: -text-muted;");

        Button saveAllBtn = new Button("Save All");
        saveAllBtn.setOnAction(e -> {
            int saved = 0;
            for (Map.Entry<File, CodeEditor> entry : openEditors.entrySet()) {
                try {
                    Files.write(entry.getKey().toPath(), entry.getValue().getText().getBytes(StandardCharsets.UTF_8));
                    saved++;
                } catch (IOException ex) {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Save failed: " + entry.getKey().getName() + "\n" + ex.getMessage());
                    a.setHeaderText(null);
                    a.showAndWait();
                }
            }
            statusBar.setText("Saved " + saved + " file(s). Restart app for changes to take effect.");
        });

        Button closeBtn = new Button("Close");
        closeBtn.setOnAction(e -> stage.close());

        HBox toolbar = new HBox(8, saveAllBtn, closeBtn);
        toolbar.setStyle("-fx-padding: 8; -fx-background-color: -bg-secondary; -fx-border-color: -border-primary; -fx-border-width: 1 0 0 0;");
        toolbar.getChildren().add(0, statusBar);
        HBox.setHgrow(statusBar, javafx.scene.layout.Priority.ALWAYS);

        // --- Split Layout ---
        SplitPane split = new SplitPane();
        split.getItems().addAll(fileTree, editorTabs);
        split.setDividerPositions(0.22);

        BorderPane root = new BorderPane();
        root.setCenter(split);
        root.setBottom(toolbar);

        Scene scene = new Scene(root, 960, 640);
        ThemeManager.getInstance().applyTheme(scene);
        stage.setScene(scene);
        stage.showAndWait();
    }

    private void openFileInTab(File file, TabPane editorTabs, Map<File, CodeEditor> openEditors) {
        // If already open, select it
        for (Tab tab : editorTabs.getTabs()) {
            if (tab.getText() != null && tab.getText().contains(file.getName())) {
                editorTabs.getSelectionModel().select(tab);
                return;
            }
        }

        if (!com.eagle.util.FileIconUtil.isEditable(file)) {
            // Open images, binaries, etc. with system default app
            try {
                java.awt.Desktop.getDesktop().open(file);
            } catch (Exception ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Cannot open " + file.getName() + ": " + ex.getMessage());
                a.setHeaderText(null);
                a.showAndWait();
            }
            return;
        }

        try {
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            LanguageType lang = LanguageType.fromFile(file);
            CodeEditor editor = new CodeEditor(lang);
            editor.setText(content);

            Tab tab = new Tab(file.getName());
            tab.setContent(editor);
            tab.setOnCloseRequest(e -> {
                CodeEditor ed = openEditors.remove(file);
                if (ed != null) {
                    editorTabs.getTabs().remove(tab);
                }
            });

            editorTabs.getTabs().add(tab);
            editorTabs.getSelectionModel().select(tab);
            openEditors.put(file, editor);
        } catch (IOException e) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Failed to open " + file.getName() + ": " + e.getMessage());
            a.setHeaderText(null);
            a.showAndWait();
        }
    }

    @FXML
    private void onDelete() {
        PluginInfo sel = pluginList.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        boolean builtin = PluginManager.getInstance().isBuiltin(sel.getId());

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Plugin");
        confirm.setHeaderText(null);
        if (builtin) {
            confirm.setContentText("\"" + sel.getName() + "\" is a built-in plugin.\n"
                + "It cannot be deleted. It will be disabled instead.\n\nContinue?");
        } else {
            confirm.setContentText("Delete \"" + sel.getName() + "\" permanently?\n"
                + "The JAR file will be removed from the plugins folder.");
        }
        confirm.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                PluginManager.getInstance().deletePlugin(sel.getId());
                if (!builtin) {
                    PluginManager.getInstance().removePluginFromList(sel.getId());
                }
                refreshList();
            }
        });
    }

    private void refreshList() {
        java.util.List<PluginInfo> all = PluginManager.getInstance().getAllPlugins();
        pluginList.setItems(javafx.collections.FXCollections.observableArrayList(all));
        if (!all.isEmpty()) {
            pluginList.getSelectionModel().select(0);
        } else {
            detailPane.getChildren().clear();
            detailPane.getChildren().add(new Label("No plugins installed."));
        }
    }

    @FXML
    private void onInstall() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Install Plugin");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Plugin JAR", "*.jar", "*.plugin", "*.zip"));
        File jar = chooser.showOpenDialog(pluginList.getScene().getWindow());
        if (jar == null) return;

        File target = new File(PluginManager.getInstance().getPluginsDir(), jar.getName());
        try {
            java.nio.file.Files.copy(jar.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Plugin copied to plugins folder.\nRestart the app to load it.");
            a.setHeaderText(null);
            ThemeManager.getInstance().applyTheme(a.getDialogPane().getScene());
            a.showAndWait();
        } catch (IOException e) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Install failed: " + e.getMessage());
            a.setHeaderText(null);
            a.showAndWait();
        }
    }

    @FXML
    private void onOpenFolder() {
        try {
            java.awt.Desktop.getDesktop().open(PluginManager.getInstance().getPluginsDir());
        } catch (Exception e) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Could not open folder: " + e.getMessage());
            a.setHeaderText(null);
            a.showAndWait();
        }
    }

    @FXML
    private void onClose() {
        ((Stage) pluginList.getScene().getWindow()).close();
    }
}
