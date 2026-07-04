package com.eagle.controller;

import com.eagle.icons.IconManager;
import com.eagle.model.ProjectMeta;
import com.eagle.model.ProjectType;
import com.eagle.model.RecentProject;
import com.eagle.plugin.PluginContext;
import com.eagle.plugin.PluginManager;
import com.eagle.util.ProjectsStore;
import com.eagle.util.RecentProjectsStore;
import com.eagle.util.ThemeManager;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

public class WelcomeController {

    @FXML private BorderPane rootPane;
    @FXML private StackPane themeTogglePane;
    @FXML private Circle toggleKnob;
    @FXML private ListView<RecentProject> recentProjectsList;
    @FXML private GridPane apkToolsGrid;

    private ContextMenu contextMenu;

    private void openApkTool(String title, String fxml) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/eagle/fxml/" + fxml));
        Parent root = loader.load();
        Stage dialogStage = new Stage();
        dialogStage.setTitle(title);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(rootPane.getScene().getWindow());
        Scene scene = new Scene(root);
        ThemeManager.getInstance().applyTheme(scene);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    @FXML
    void buildhtmlapkaction(MouseEvent event) throws IOException {
        openApkTool("HTML to Android", "CreateHtmlApk.fxml");
    }

    @FXML
    void buildjsapkaction(MouseEvent event) throws IOException {
        openApkTool("JS to Android", "JsToApk.fxml");
    }

    @FXML
    void buildpdfapkaction(MouseEvent event) throws IOException {
        openApkTool("PDF to Android", "PdfToApk.fxml");
    }

    @FXML
    void buildmusicapkaction(MouseEvent event) throws IOException {
        openApkTool("Music to Android", "MusicApk.fxml");
    }

    @FXML
    void buildwebsiteapkaction(MouseEvent event) throws IOException {
        openApkTool("Website to Android", "WebsiteToApk.fxml");
    }

    @FXML
    void buildquizapkaction(MouseEvent event) throws IOException {
        openApkTool("Quiz to Android", "QuizApk.fxml");
    }

    @FXML
    void buildwebappapkaction(MouseEvent event) throws IOException {
        openApkTool("Web App to Android", "WebAppToApk.fxml");
    }

    @FXML
    void buildjavafxapkaction(MouseEvent event) throws IOException {
        openApkTool("JavaFX to Android", "JavaFxToApk.fxml");
    }

    private WebView dashboardWebView;

    @FXML private VBox dashboardPanel;

    @FXML
    public void initialize() {
        loadRecentProjects();
        positionKnob(false);
        offerSessionRestore();
        setupContextMenu();
        loadPluginApkTools();
        initDashboard();
    }

    private void initDashboard() {
        dashboardWebView = new javafx.scene.web.WebView();
        dashboardWebView.setPrefWidth(200);
        VBox.setVgrow(dashboardWebView, Priority.ALWAYS);
        dashboardPanel.getChildren().add(dashboardWebView);
        updateDashboard();
    }

    private void updateDashboard() {
        boolean dark = ThemeManager.getInstance().isDark();
        String file = dark ? "dashboard-dark.html" : "dashboard-light.html";
        dashboardWebView.getEngine().load(getClass().getResource("/com/eagle/web/" + file).toExternalForm());
    }

    private void loadPluginApkTools() {
        List<PluginContext.ApkToolRegistration> tools = new java.util.ArrayList<>();
        for (PluginContext ctx : PluginManager.getInstance().getActiveContexts()) {
            tools.addAll(ctx.getApkTools());
        }
        if (tools.isEmpty()) return;

        // Build set of occupied cells so we can fill empty slots
        java.util.Set<String> occupied = new java.util.HashSet<>();
        for (javafx.scene.Node child : apkToolsGrid.getChildren()) {
            Integer r = GridPane.getRowIndex(child);
            Integer c = GridPane.getColumnIndex(child);
            if (r != null && c != null) occupied.add(r + "," + c);
        }

        // Find first free cell (row,col)
        int row = 0, col = 0;
        findFree:
        for (int r = 0; ; r++) {
            for (int c = 0; c < 2; c++) {
                if (!occupied.contains(r + "," + c)) {
                    row = r; col = c; break findFree;
                }
            }
        }

        while (apkToolsGrid.getRowConstraints().size() <= row) {
            apkToolsGrid.getRowConstraints().add(new javafx.scene.layout.RowConstraints());
        }

        for (PluginContext.ApkToolRegistration tool : tools) {
            VBox card = new VBox();
            card.setPadding(new Insets(14, 14, 12, 14));
            card.getStyleClass().add("tool-card");
            card.setSpacing(6);
            card.setAlignment(Pos.CENTER);

            Label iconLabel = new Label();
            iconLabel.setStyle("-fx-font-size: 28px;");
            iconLabel.setGraphic(IconManager.imageView(IconManager.PACKAGE, 28));

            Label nameLabel = new Label(tool.name);
            nameLabel.getStyleClass().add("tool-card-label");

            Label descLabel = new Label(tool.description);
            descLabel.getStyleClass().add("tool-card-desc");

            card.getChildren().addAll(iconLabel, nameLabel, descLabel);
            card.setOnMouseClicked(e -> tool.action.run());

            GridPane.setConstraints(card, col, row);
            apkToolsGrid.getChildren().add(card);

            col++;
            if (col >= 2) { col = 0; row++;
                while (apkToolsGrid.getRowConstraints().size() <= row) {
                    apkToolsGrid.getRowConstraints().add(new javafx.scene.layout.RowConstraints());
                }
            }
        }
    }

    private void loadRecentProjects() {
        List<RecentProject> recent = RecentProjectsStore.load();
        recentProjectsList.setItems(javafx.collections.FXCollections.observableArrayList(recent));
        recentProjectsList.setCellFactory(param -> new RecentProjectCell());
    }
    
    @FXML
    void rofo(MouseEvent event) {

        refreshProjectList();
        
    }

    private void refreshProjectList() {
        List<File> dirs = ProjectsStore.scanAllDirectories();
        for (File d : dirs) {
            RecentProjectsStore.addRecent(d);
        }
        loadRecentProjects();
        showInfo("Refreshed: found " + dirs.size() + " project(s) in " + ProjectsStore.getProjectsRoot().getAbsolutePath());
    }

    // ---- Custom cell with rich display ----
    static class RecentProjectCell extends ListCell<RecentProject> {
        private final HBox root;
        private final Label iconLabel;
        private final Label nameLabel;
        private final Label pathLabel;
        private final Label dateLabel;

        RecentProjectCell() {
            root = new HBox(12);
            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(8, 14, 8, 14));

            iconLabel = new Label();
            iconLabel.setMinWidth(32);
            iconLabel.setAlignment(Pos.CENTER);
            iconLabel.setStyle("-fx-font-size: 20px;");

            VBox info = new VBox(2);
            info.setAlignment(Pos.CENTER_LEFT);

            nameLabel = new Label();
            nameLabel.getStyleClass().add("cell-name");

            pathLabel = new Label();
            pathLabel.getStyleClass().add("cell-path");

            dateLabel = new Label();
            dateLabel.getStyleClass().add("cell-date");

            info.getChildren().addAll(nameLabel, pathLabel, dateLabel);
            root.getChildren().addAll(iconLabel, info);
            setGraphic(root);
        }

        @Override
        protected void updateItem(RecentProject item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
            } else {
                File f = new File(item.getPath());
                boolean exists = f.exists();
                iconLabel.setText("");
                iconLabel.setGraphic(exists ? IconManager.imageView(IconManager.FOLDER, 20) : IconManager.imageView(IconManager.CLOSE, 20));
                nameLabel.setText(item.getName());
                pathLabel.setText(f.getParent());
                dateLabel.setText(formatDate(f.lastModified()));
                setGraphic(root);

                if (!exists) {
                    nameLabel.getStyleClass().add("cell-missing");
                } else {
                    nameLabel.getStyleClass().remove("cell-missing");
                }
            }
        }

        private String formatDate(long millis) {
            if (millis <= 0) return "";
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
            return sdf.format(new Date(millis));
        }
    }

    // ---- Fixed context menu using onContextMenuRequested ----
    private void setupContextMenu() {
        contextMenu = new ContextMenu();

        // Select item on right-click before showing context menu
        recentProjectsList.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                javafx.scene.Node n = e.getPickResult().getIntersectedNode();
                while (n != null && !(n instanceof ListCell)) {
                    n = n.getParent();
                }
                if (n instanceof ListCell) {
                    int idx = ((ListCell<?>) n).getIndex();
                    if (idx >= 0 && idx < recentProjectsList.getItems().size()) {
                        recentProjectsList.getSelectionModel().select(idx);
                    }
                }
            }
            // Dismiss context menu on any click in the list
            contextMenu.hide();
        });

        recentProjectsList.setOnContextMenuRequested(event -> {
            RecentProject selected = recentProjectsList.getSelectionModel().getSelectedItem();
            if (selected == null) {
                event.consume();
                return;
            }
            buildContextMenu(selected);
            contextMenu.show(recentProjectsList, event.getScreenX(), event.getScreenY());
            event.consume();
        });

        // Dismiss on key press
        recentProjectsList.setOnKeyPressed(e -> contextMenu.hide());

        // Double-click to open
        recentProjectsList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                RecentProject selected = recentProjectsList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openSelectedProject(selected);
                }
            }
        });
    }

    private void buildContextMenu(RecentProject selected) {
        contextMenu.getItems().clear();
        File dir = new File(selected.getPath());
        boolean exists = dir.exists();
        
        // ---- Refresh ----
        MenuItem refreshItem = new MenuItem("Refresh Projects");
        refreshItem.setGraphic(IconManager.imageView(IconManager.REFRESH, 14));
        refreshItem.setOnAction(e -> refreshProjectList());
        contextMenu.getItems().add(refreshItem);

        // ---- Open ----
        MenuItem openItem = new MenuItem("Open Project");
        openItem.setGraphic(IconManager.imageView(IconManager.PLAY, 14));
        openItem.setOnAction(e -> openSelectedProject(selected));
        contextMenu.getItems().add(openItem);

        // ---- Open in New Window ----
        MenuItem newWindowItem = new MenuItem("Open in New Window");
        newWindowItem.setGraphic(IconManager.imageView(IconManager.NEW_WINDOW, 14));
        newWindowItem.setOnAction(e -> openInNewWindow(selected));
        //contextMenu.getItems().add(newWindowItem);

        contextMenu.getItems().add(new SeparatorMenuItem());

        // ---- Show in Explorer ----
        MenuItem explorerItem = new MenuItem("Show in Explorer");
        explorerItem.setGraphic(IconManager.imageView(IconManager.FOLDER, 14));
        explorerItem.setOnAction(e -> showInExplorer(selected));
        explorerItem.setDisable(!exists);
        contextMenu.getItems().add(explorerItem);

        // ---- Copy Path ----
        MenuItem copyPathItem = new MenuItem("Copy Path");
        copyPathItem.setGraphic(IconManager.imageView(IconManager.COPY, 14));
        copyPathItem.setOnAction(e -> copyPath(selected));
        contextMenu.getItems().add(copyPathItem);

        // ---- Duplicate ----
        MenuItem duplicateItem = new MenuItem("Duplicate");
        duplicateItem.setGraphic(IconManager.imageView(IconManager.REFRESH, 14));
        duplicateItem.setOnAction(e -> duplicateProject(selected));
        duplicateItem.setDisable(!exists);
        contextMenu.getItems().add(duplicateItem);

        // ---- Rename ----
        MenuItem renameItem = new MenuItem("Rename");
        renameItem.setGraphic(IconManager.imageView(IconManager.EDIT, 14));
        renameItem.setOnAction(e -> renameProject(selected));
        renameItem.setDisable(!exists);
        contextMenu.getItems().add(renameItem);

        // ---- Archive (ZIP) Only ----
        MenuItem archiveItem = new MenuItem("Archive (Only) as ZIP");
        archiveItem.setGraphic(IconManager.imageView(IconManager.PACKAGE, 14));
        archiveItem.setOnAction(e -> archiveProject(selected));
        archiveItem.setDisable(!exists);
        contextMenu.getItems().add(archiveItem);

        //contextMenu.getItems().add(new SeparatorMenuItem());
        
        // ---- Archive (ZIP) & Delete ----
        MenuItem archiveItemm = new MenuItem("Archive And Delete");
        archiveItemm.setGraphic(IconManager.imageView(IconManager.PACKAGE, 14));
        archiveItemm.setOnAction(e -> archiveProjectt(selected));
        archiveItemm.setDisable(!exists);
        contextMenu.getItems().add(archiveItemm);

        contextMenu.getItems().add(new SeparatorMenuItem());

        // ---- Remove from Recent ----
        MenuItem removeRecentItem = new MenuItem("Remove from Recent");
        removeRecentItem.setGraphic(IconManager.imageView(IconManager.DELETE, 14));
        removeRecentItem.setOnAction(e -> removeFromRecent(selected));
        contextMenu.getItems().add(removeRecentItem);

        // ---- Delete ----
        MenuItem deleteItem = new MenuItem("Delete Project");
        deleteItem.setGraphic(IconManager.imageView(IconManager.DELETE, 14));
        deleteItem.setOnAction(e -> deleteProject(selected));
        deleteItem.setDisable(!exists);
        contextMenu.getItems().add(deleteItem);

        contextMenu.getItems().add(new SeparatorMenuItem());

        // ---- Project Info ----
        MenuItem infoItem = new MenuItem("Project Info");
        infoItem.setGraphic(IconManager.imageView(IconManager.SEVERITY_INFO, 14));
        infoItem.setOnAction(e -> showProjectInfo(selected));
        infoItem.setDisable(!exists);
        contextMenu.getItems().add(infoItem);
    }

    // ================================================================
    //   CONTEXT MENU ACTIONS
    // ================================================================

    private void openSelectedProject(RecentProject selected) {
        if (selected == null) return;
        File dir = new File(selected.getPath());
        if (dir.exists()) {
            openProject(dir);
        } else {
            showError("This project folder no longer exists:\n" + selected.getPath());
            RecentProjectsStore.removeRecent(selected.getPath());
            loadRecentProjects();
        }
    }

    private void openInNewWindow(RecentProject selected) {
        if (selected == null) return;
        File dir = new File(selected.getPath());
        if (!dir.exists()) {
            showError("Project folder no longer exists.");
            return;
        }
        try {
            String classpath = System.getProperty("java.class.path");
            String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            String mainClass = "com.eagle.Main";

            // Use a temp .bat file to avoid Windows command-line length limits
            File batFile = new File(System.getProperty("java.io.tmpdir"), "eagle_open_" + System.nanoTime() + ".bat");
            PrintWriter pw = new PrintWriter(batFile, "UTF-8");
            pw.println("@echo off");
            pw.println("start \"Eagle IDE\" \"" + javaBin + "\" -cp \"" + classpath + "\" " + mainClass + " \"" + dir.getAbsolutePath() + "\"");
            pw.close();

            new ProcessBuilder(batFile.getAbsolutePath()).start();
        } catch (Exception e) {
            showError("Could not open in new window: " + e.getMessage());
        }
    }

    private void showInExplorer(RecentProject selected) {
        if (selected == null) return;
        File dir = new File(selected.getPath());
        try {
            java.awt.Desktop.getDesktop().open(dir);
        } catch (Exception e) {
            showError("Could not open explorer: " + e.getMessage());
        }
    }

    private void copyPath(RecentProject selected) {
        if (selected == null) return;
        ClipboardContent content = new ClipboardContent();
        content.putString(selected.getPath());
        Clipboard.getSystemClipboard().setContent(content);
    }

    private void duplicateProject(RecentProject selected) {
        if (selected == null) return;
        File dir = new File(selected.getPath());
        if (!dir.exists()) {
            showError("Project folder no longer exists.");
            return;
        }
        TextInputDialog dialog = new TextInputDialog(dir.getName() + " - Copy");
        dialog.setTitle("Duplicate Project");
        dialog.setHeaderText("Enter name for duplicate");
        ThemeManager.getInstance().applyTheme(dialog.getDialogPane().getScene());

        dialog.showAndWait().ifPresent(newName -> {
            if (newName.trim().isEmpty()) return;
            File parent = dir.getParentFile();
            File dest = new File(parent, newName.trim());
            try {
                copyFolder(dir, dest);
                RecentProjectsStore.addRecent(dest);
                loadRecentProjects();
            } catch (IOException e) {
                showError("Duplicate failed: " + e.getMessage());
            }
        });
    }

    private void copyFolder(File src, File dest) throws IOException {
        if (!dest.exists()) dest.mkdirs();
        for (File f : src.listFiles()) {
            File target = new File(dest, f.getName());
            if (f.isDirectory()) {
                copyFolder(f, target);
            } else {
                Files.copy(f.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void renameProject(RecentProject selected) {
        if (selected == null) return;
        File dir = new File(selected.getPath());
        if (!dir.exists()) {
            showError("Project folder no longer exists.");
            return;
        }
        TextInputDialog dialog = new TextInputDialog(dir.getName());
        dialog.setTitle("Rename Project");
        dialog.setHeaderText("Rename " + dir.getName());
        ThemeManager.getInstance().applyTheme(dialog.getDialogPane().getScene());

        dialog.showAndWait().ifPresent(newName -> {
            if (newName.trim().isEmpty()) return;
            File parent = dir.getParentFile();
            File renamed = new File(parent, newName.trim());
            if (dir.renameTo(renamed)) {
                RecentProjectsStore.addRecent(renamed);
                RecentProjectsStore.removeRecent(selected.getPath());
                loadRecentProjects();
            } else {
                showError("Rename failed.");
            }
        });
    }

    private void archiveProject(RecentProject selected) {
        if (selected == null) return;
        File dir = new File(selected.getPath());
        if (!dir.exists()) {
            showError("Project folder no longer exists.");
            return;
        }
        try {
            File zipFile = new File(dir.getParent(), dir.getName() + ".zip");
            java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                new java.io.FileOutputStream(zipFile));
            zipFolder(dir, dir, zos);
            zos.close();
            showInfo("Project archived to:\n" + zipFile.getAbsolutePath());
        } catch (IOException e) {
            showError("Archive failed: " + e.getMessage());
        }
    }
    
    //Archive And Delete
    
    private void archiveProjectt(RecentProject selected) {
        if (selected == null) return;
        File dir = new File(selected.getPath());
        if (!dir.exists()) {
            showError("Project folder no longer exists.");
            return;
        }
        try {
            File zipFile = new File(dir.getParent(), dir.getName() + ".zip");
            java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                new java.io.FileOutputStream(zipFile));
            zipFolder(dir, dir, zos);
            
            if (ProjectsStore.deleteProject(dir)) {
                    RecentProjectsStore.removeRecent(selected.getPath());
                    loadRecentProjects();
                } else {
                    showError("Could not delete project.");
                }
            
            zos.close();
            showInfo("Project archived and deleted to:\n" + zipFile.getAbsolutePath());
        } catch (IOException e) {
            showError("Archive failed: " + e.getMessage());
        }
    }
    

    private void zipFolder(File base, File dir, java.util.zip.ZipOutputStream zos) throws IOException {
        byte[] buf = new byte[8192];
        for (File f : dir.listFiles()) {
            if (f.getName().startsWith(".")) continue;
            String entryName = base.toPath().relativize(f.toPath()).toString().replace("\\", "/");
            if (f.isDirectory()) {
                zos.putNextEntry(new java.util.zip.ZipEntry(entryName + "/"));
                zos.closeEntry();
                zipFolder(base, f, zos);
            } else {
                zos.putNextEntry(new java.util.zip.ZipEntry(entryName));
                java.io.FileInputStream in = new java.io.FileInputStream(f);
                int n;
                while ((n = in.read(buf)) != -1) zos.write(buf, 0, n);
                in.close();
                zos.closeEntry();
            }
        }
    }

    private void removeFromRecent(RecentProject selected) {
        if (selected == null) return;
        RecentProjectsStore.removeRecent(selected.getPath());
        loadRecentProjects();
    }

    private void deleteProject(RecentProject selected) {
        if (selected == null) return;
        File dir = new File(selected.getPath());
        if (!dir.exists()) {
            RecentProjectsStore.removeRecent(selected.getPath());
            loadRecentProjects();
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete project \"" + selected.getName() + "\" and all its files?\nThis cannot be undone.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Delete Project");
        confirm.setHeaderText(null);
        ThemeManager.getInstance().applyTheme(confirm.getDialogPane().getScene());

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                if (ProjectsStore.deleteProject(dir)) {
                    RecentProjectsStore.removeRecent(selected.getPath());
                    loadRecentProjects();
                } else {
                    showError("Could not delete project.");
                }
            }
        });
    }

    private void showProjectInfo(RecentProject selected) {
        if (selected == null) return;
        File dir = new File(selected.getPath());
        if (!dir.exists()) {
            showError("Project folder no longer exists.");
            return;
        }
        long size = folderSize(dir);
        int fileCount = countFiles(dir);
        String sizeStr = size < 1024 ? size + " B" :
            size < 1024 * 1024 ? (size / 1024) + " KB" :
            (size / (1024 * 1024)) + " MB";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String info = "Name: " + selected.getName() +
            "\nPath: " + selected.getPath() +
            "\nSize: " + sizeStr +
            "\nFiles: " + fileCount +
            "\nCreated: " + sdf.format(new Date(dir.lastModified())) +
            "\nModified: " + sdf.format(new Date(dir.lastModified()));
        showInfo(info);
    }

    private long folderSize(File dir) {
        long size = 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;
        for (File f : files) {
            if (f.isDirectory()) size += folderSize(f);
            else size += f.length();
        }
        return size;
    }

    private int countFiles(File dir) {
        int count = 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;
        for (File f : files) {
            if (f.isDirectory()) count += countFiles(f);
            else count++;
        }
        return count;
    }

    @FXML
    private void onRecentItemClicked(MouseEvent event) {
        if (event.getClickCount() == 2) {
            RecentProject selected = recentProjectsList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                File dir = new File(selected.getPath());
                if (dir.exists()) {
                    openProject(dir);
                } else {
                    showError("This project folder no longer exists:\n" + selected.getPath());
                    loadRecentProjects();
                }
            }
        }
    }

    // ================================================================
    //   MAIN ACTIONS
    // ================================================================

    @FXML
    private void onNewProject() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/eagle/fxml/NewProjectDialog.fxml"));
            Parent root = loader.load();
            NewProjectController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Create New Project");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(rootPane.getScene().getWindow());
            Scene scene = new Scene(root);
            ThemeManager.getInstance().applyTheme(scene);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();

            if (controller.isCancelled()) return;
            File projectDir = controller.getCreatedProjectDir();
            if (projectDir == null) return;

            RecentProjectsStore.addRecent(projectDir);
            openProject(projectDir);
        } catch (IOException e) {
            showError("Failed to create project: " + e.getMessage());
        }
    }

    @FXML
    private void onOpenProject() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Open Project Folder");
        chooser.setInitialDirectory(ProjectsStore.getProjectsRoot());
        File dir = chooser.showDialog(rootPane.getScene().getWindow());
        if (dir != null && dir.isDirectory()) {
            if (!new File(dir, ".eagle-project").exists()) {
                if (dir.getParentFile().equals(ProjectsStore.getProjectsRoot())) {
                    ProjectMeta.write(dir, ProjectType.CODE);
                }
            }
            RecentProjectsStore.addRecent(dir);
            openProject(dir);
        }
    }

    @FXML
    private void onToggleTheme() {
        ThemeManager.getInstance().toggleTheme();
        positionKnob(true);
        updateDashboard();
    }

    // ================================================================
    //   HELPERS
    // ================================================================

    private void positionKnob(boolean animate) {
        boolean dark = ThemeManager.getInstance().isDark();
        double targetX = dark ? -12 : 12;
        if (animate) {
            TranslateTransition tt = new TranslateTransition(Duration.millis(150), toggleKnob);
            tt.setToX(targetX);
            tt.play();
        } else {
            toggleKnob.setTranslateX(targetX);
        }
    }

    private void offerSessionRestore() {
        try {
            Class<?> sm = Class.forName("com.eagle.util.SessionManager");
            java.lang.reflect.Method getLast = sm.getMethod("getLastSessionDir");
            File lastDir = (File) getLast.invoke(null);
            if (lastDir != null && lastDir.isDirectory()) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Session Restore");
                alert.setHeaderText("Restore previous session?");
                alert.setContentText("Would you like to open the last project: " + lastDir.getName() + "?");
                ThemeManager.getInstance().applyTheme(alert.getDialogPane().getScene());
                alert.showAndWait().ifPresent(btn -> {
                    if (btn == ButtonType.OK && lastDir.exists()) {
                        openProject(lastDir);
                    }
                });
            }
        } catch (Exception ignored) { }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(null);
        ThemeManager.getInstance().applyTheme(alert.getDialogPane().getScene());
        alert.showAndWait();
    }

    @FXML
    private void onBuildDesktop() {
        try {
            openApkTool("Web to Desktop", "DesktopBuildDialog.fxml");
        } catch (IOException e) {
            showError("Failed to open Desktop build dialog: " + e.getMessage());
        }
    }

    @FXML
    private void onBuildAndroid() {
        try {
            openApkTool("Web to Android", "AndroidBuildDialog.fxml");
        } catch (IOException e) {
            showError("Failed to open Android build dialog: " + e.getMessage());
        }
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setHeaderText(null);
        ThemeManager.getInstance().applyTheme(alert.getDialogPane().getScene());
        alert.showAndWait();
    }

    public static void openProject(File projectDir) {
        try {
            com.eagle.Main.openProjectDir(projectDir);
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to open project:\n" + e.getMessage());
            alert.setHeaderText(null);
            alert.showAndWait();
        }
    }
}
