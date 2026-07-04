package com.eagle.controller;

import com.eagle.editor.AiPanel;
import com.eagle.editor.BreadcrumbBar;
import com.eagle.editor.ClipboardHistoryPanel;
import com.eagle.editor.CodeEditor;
import com.eagle.editor.CompletionProvider;
import com.eagle.editor.DatabaseViewerDialog;
import com.eagle.editor.DebuggerPanel;
import com.eagle.editor.ExtensionsMarketplaceDialog;
import com.eagle.editor.GitPanel;
import com.eagle.editor.LanguageType;
import com.eagle.editor.MediaViewer;
import com.eagle.editor.MonacoEditor;
import com.eagle.editor.ScratchPadPanel;
import com.eagle.editor.SettingsDialog;
import com.eagle.editor.SnippetsManagerDialog;
import com.eagle.editor.TerminalPanel;
import com.eagle.editor.TodoPanel;
import com.eagle.generator.*;
import com.eagle.icons.IconManager;
import com.eagle.model.FileTreeItem;
import com.eagle.model.ProjectMeta;
import com.eagle.model.ProjectType;
import com.eagle.plugin.PluginContext;
import com.eagle.plugin.PluginManager;
import com.eagle.tools.*;
import com.eagle.util.EditorSettings;
import com.eagle.util.FileIconUtil;
import com.eagle.util.ProjectsStore;
import com.eagle.util.RecentProjectsStore;
import com.eagle.util.SessionManager;
import com.eagle.util.SimpleApkBuilder;
import com.eagle.util.ThemeManager;
import com.eagle.util.ToolsConfig;
import com.eagle.util.ZipExporter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

public class EditorController {

    @FXML private BorderPane rootPane;
    @FXML private TreeView<FileTreeItem> fileTree;
    @FXML private TextField treeFilter;
    @FXML private TabPane editorTabs;
    @FXML private VBox editorContainer;
    @FXML private BreadcrumbBar breadcrumbBar;
    @FXML private VBox previewContainer;
    @FXML private WebView webPreview;
    @FXML private Label projectNameLabel;
    @FXML private Label projectsRootLabel;
    @FXML private Label projectsCountLabel;
    @FXML private Label projectsCountLabel2;
    @FXML private Label statusLabel;
    @FXML private Label cursorPosLabel;
    @FXML private Label fileTypeLabel;
    @FXML private Label statusLabelSecondary;
    @FXML private Button previewToggleBtn;
    @FXML private CheckMenuItem viewPreviewItem;
    @FXML private CheckMenuItem aiLangArabicItem;
    @FXML private CheckMenuItem aiLangEnglishItem;
    @FXML private CheckMenuItem viewSidebarItem;
    @FXML private CheckMenuItem viewStatusBarItem;
    @FXML private CheckMenuItem viewWordWrapItem;
    @FXML private CheckMenuItem viewAiItem;
    @FXML private CheckMenuItem viewTerminalItem;
    @FXML private CheckMenuItem viewGitItem;
    @FXML private CheckMenuItem viewTodoItem;
    @FXML private CheckMenuItem viewClipboardItem;
    @FXML private CheckMenuItem viewScratchPadItem;
    @FXML private CheckMenuItem runPreviewItem;
    @FXML private StackPane themeTogglePane;
    @FXML private Circle toggleKnob;
    @FXML private SplitPane mainSplit;
    @FXML private Button debugBtn;
    @FXML private Button aiBtn;
    @FXML private Button terminalBtn;
    @FXML private Button gitBtn;
    @FXML private Button refreshSideBtn;
    @FXML private Button newFileSideBtn;
    @FXML private Button newFolderSideBtn;
    @FXML private ProgressBar generationProgress;
    @FXML private Button viewLogBtn;

    private File projectRoot;
    private boolean previewVisible = true;
    private double currentCodeFont = 13.5;

    private Map<Integer, Node> splitItems;

    private TabManager tabManager;
    private ContextMenu treeContextMenu;
    private DebuggerPanel debuggerPanel;
    private AiPanel aiPanel;
    private TerminalPanel terminalPanel;
    private com.eagle.editor.AiInlineProvider aiInlineProvider;
    private GitPanel gitPanel;
    private TodoPanel todoPanel;
    private ClipboardHistoryPanel clipboardHistoryPanel;
    private ScratchPadPanel scratchPadPanel;
    private PauseTransition autoSaveDebounce;

    // ---- File Clipboard (Copy/Cut/Paste) ----
    private java.util.List<File> clipboardFiles;
    private boolean clipboardIsCut;
    private File clipboardSourceDir;

    public static EditorController getInstance() {
        Stage stage = com.eagle.Main.getPrimaryStage();
        if (stage != null && stage.getScene() != null) {
            Object data = stage.getScene().getUserData();
            if (data instanceof EditorController) return (EditorController) data;
        }
        return null;
    }

    public CodeEditor getActiveEditor() {
        return tabManager.getActiveEditor();
    }

    public static EditorController openProject(File projectDir) throws IOException {
        FXMLLoader loader = new FXMLLoader(EditorController.class.getResource("/com/eagle/fxml/Editor.fxml"));
        Parent root = loader.load();
        EditorController controller = loader.getController();
        controller.initProject(projectDir);

        Stage stage = com.eagle.Main.getPrimaryStage();
        Scene scene = new Scene(root, 1280, 800);
        scene.setUserData(controller);
        ThemeManager.getInstance().applyTheme(scene);
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.setTitle("Eagle IDE - " + projectDir.getName());
        stage.setMinWidth(1280);
        stage.setMinHeight(800);

        stage.setOnCloseRequest(e -> SessionManager.clearLastSession());
        return controller;
    }

    public void openFileFromExternal(File file) {
        if (file != null && file.exists() && file.isFile()) {
            openFile(file);
            statusLabel.setText("Opened from external: " + file.getName());
        }
    }

    // ================================================================
    //   MENU ACTIONS
    // ================================================================

    @FXML private void onUndo() {
        CodeEditor ed = tabManager.getActiveEditor();
        if (ed != null) ed.undo();
    }

    @FXML private void onRedo() {
        CodeEditor ed = tabManager.getActiveEditor();
        if (ed != null) ed.redo();
    }

    @FXML private void onCut() {
        CodeEditor ed = tabManager.getActiveEditor();
        if (ed != null) ed.cut();
    }

    @FXML private void onCopy() {
        CodeEditor ed = tabManager.getActiveEditor();
        if (ed != null) ed.copy();
    }

    @FXML private void onPaste() {
        CodeEditor ed = tabManager.getActiveEditor();
        if (ed != null) ed.paste();
    }

    @FXML private void onFindReplace() {
        CodeEditor ed = tabManager.getActiveEditor();
        if (ed != null) {
            ed.showFindReplace();
        } else {
            statusLabel.setText("Open a file first");
        }
    }

    @FXML private void onSelectAll() {
        CodeEditor ed = tabManager.getActiveEditor();
        if (ed != null) ed.selectAll();
    }

    @FXML private void onToggleSidebar() {
        toggleSplitItem(0, viewSidebarItem.isSelected());
    }

    @FXML private void onZoomIn() {
        double z = Math.min(32, currentCodeFont + 2);
        currentCodeFont = z;
        for (CodeEditor ed : tabManager.allOpenEditors()) ed.setFontSize(z);
        statusLabel.setText("Zoom: " + (int)(z / 13.5 * 100) + "%");
    }

    @FXML private void onZoomOut() {
        double z = Math.max(9, currentCodeFont - 2);
        currentCodeFont = z;
        for (CodeEditor ed : tabManager.allOpenEditors()) ed.setFontSize(z);
        statusLabel.setText("Zoom: " + (int)(z / 13.5 * 100) + "%");
    }

    @FXML private void onZoomReset() {
        currentCodeFont = 13.5;
        for (CodeEditor ed : tabManager.allOpenEditors()) ed.setFontSize(13.5);
        statusLabel.setText("Zoom: 100%");
    }

    @FXML private void onShowProjectInExplorer() {
        if (projectRoot != null) {
            try {
                java.awt.Desktop.getDesktop().open(projectRoot);
            } catch (Exception e) {
                showError("Could not open explorer: " + e.getMessage());
            }
        }
    }

    @FXML private void onCloseProject() {
        try {
            tabManager.closeAll();
            webPreview.getEngine().loadContent("<html><body></body></html>");
            fileTree.setRoot(null);
            projectRoot = null;
            if (aiPanel != null) aiPanel.setProjectRoot(null);
            com.eagle.Main.showWelcomeScreen();
        } catch (Exception e) {
            showError("Could not return to welcome screen.");
        }
    }

    @FXML private void onExit() {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.close();
    }

    @FXML private void onAbout() {
        
        
Alert about = new Alert(Alert.AlertType.NONE);
about.setTitle("About Eagle IDE");
about.setHeaderText(null); // سنستخدم header مخصص

// تحميل صورة اللوجو (غير المسار حسب مشروعك)
Image logoImage = new Image(getClass().getResourceAsStream("icons/png/kadysoft.png"));
ImageView logoView = new ImageView(logoImage);
logoView.setFitWidth(110);
logoView.setFitHeight(110);
logoView.setPreserveRatio(true);

// صندوق المحتوى الرئيسي
VBox contentBox = new VBox(18);
contentBox.setAlignment(Pos.CENTER);
contentBox.setPadding(new Insets(25));

// العنوان الرئيسي
Label titleLabel = new Label("Eagle IDE");
titleLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

// الإصدار
Label versionLabel = new Label("الإصدار 1.0");
versionLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #3498db; -fx-font-weight: 600;");

// الوصف
TextFlow description = new TextFlow();
description.setTextAlignment(TextAlignment.CENTER);
description.setLineSpacing(6);

Text desc1 = new Text("بيئة تطوير متكاملة لتطوير الويب\n\n");
desc1.setStyle("-fx-font-size: 15px;");

Text desc2 = new Text("• Visual Builder\n");
Text desc3 = new Text("• محرر كود احترافي\n");
Text desc4 = new Text("• Live Preview فوري\n");
Text desc5 = new Text("• إدارة المشاريع بكفاءة");

desc1.setStyle("-fx-fill: #2c3e50;");
desc2.setStyle("-fx-fill: #34495e;");
desc3.setStyle("-fx-fill: #34495e;");
desc4.setStyle("-fx-fill: #34495e;");
desc5.setStyle("-fx-fill: #34495e;");

description.getChildren().addAll(desc1, desc2, desc3, desc4, desc5);

// معلومات إضافية
Label techLabel = new Label("Built with JavaFX & RichTextFX");
techLabel.setStyle("-fx-font-size: 13.5px; -fx-text-fill: #7f8c8d;");

// تجميع كل العناصر
contentBox.getChildren().addAll(logoView, titleLabel, versionLabel, description, techLabel);

// إعداد DialogPane
DialogPane dialogPane = about.getDialogPane();
dialogPane.setContent(contentBox);
dialogPane.setGraphic(logoView); // لوجو في الـ Header
dialogPane.getButtonTypes().add(ButtonType.CLOSE);

// تصميم احترافي
dialogPane.setStyle(
    "-fx-background-radius: 16px; " +
    "-fx-border-radius: 16px; " +
    "-fx-border-color: #3498db; " +
    "-fx-border-width: 2.5px; " +
    "-fx-padding: 10px;"
);

// تطبيق الثيم بنفس الطريقة الأصلية
ThemeManager.getInstance().applyTheme(dialogPane.getScene());

about.showAndWait();
        
    }

    @FXML
    public void initialize() {
        positionKnob(false);
        setupFileTree();
        setupPreview();
        setupTabManager();
        if (aiPanel != null) aiPanel.setTabManager(tabManager);
        setupPlugins();
        Platform.runLater(() -> {
            applyIconsToUI();
            applySettingsToOpenEditors();
        });
        startFileWatcher();
    }

    private void applyIconsToUI() {
        // Find MenuBar
        javafx.scene.control.MenuBar menuBar = (javafx.scene.control.MenuBar) rootPane.lookup(".menu-bar");
        if (menuBar != null) {
            for (javafx.scene.control.Menu menu : menuBar.getMenus()) {
                setMenuIcon(menu);
                for (javafx.scene.control.MenuItem item : menu.getItems()) {
                    if (item instanceof javafx.scene.control.Menu) {
                        setMenuIcon((javafx.scene.control.Menu) item);
                        for (javafx.scene.control.MenuItem sub : ((javafx.scene.control.Menu) item).getItems()) {
                            setMenuItemIcon(sub);
                        }
                    } else {
                        setMenuItemIcon(item);
                    }
                }
            }
        }
        // Toolbar buttons
        javafx.scene.Node toolbar = rootPane.lookup(".toolbar-container");
        if (toolbar instanceof javafx.scene.layout.HBox) {
            for (javafx.scene.Node node : ((javafx.scene.layout.HBox) toolbar).getChildren()) {
                if (node instanceof javafx.scene.control.Button) {
                    setToolbarIcon((javafx.scene.control.Button) node);
                }
            }
        }
        // Sidebar header buttons
        setSidebarIcon(refreshSideBtn);
        setSidebarIcon(newFileSideBtn);
        setSidebarIcon(newFolderSideBtn);
    }

    /** Set icon on a sidebar button by matching its text, then clear text */
    private void setSidebarIcon(javafx.scene.control.Button btn) {
        if (btn == null) return;
        String text = btn.getText();
        if (text == null || text.isEmpty()) return;
        javafx.scene.Node icon = iconForText(text);
        if (icon != null) {
            btn.setGraphic(icon);
            btn.setText("");
        }
    }

    private void setMenuIcon(javafx.scene.control.Menu menu) {
        String text = menu.getText();
        javafx.scene.Node icon = iconForText(text);
        if (icon != null) menu.setGraphic(icon);
    }

    private void setMenuItemIcon(javafx.scene.control.MenuItem item) {
        if (item instanceof javafx.scene.control.SeparatorMenuItem) return;
        String text = item.getText();
        if (text == null || text.isEmpty()) return;
        javafx.scene.Node icon = iconForText(text);
        if (icon != null) item.setGraphic(icon);
    }

    private void setToolbarIcon(javafx.scene.control.Button btn) {
        String text = btn.getText();
        if (text == null || text.isEmpty()) return;
        // Remove existing emoji from button text
        btn.setText(text.replaceAll("[\\u2600-\\u27BF\\uD83C-\\uDBFF\\uDC00-\\uDFFF]", "").trim());
        javafx.scene.Node icon = iconForText(text);
        if (icon != null) btn.setGraphic(icon);
    }

    private javafx.scene.Node iconForText(String text) {
        if (text == null) return null;
        String t = text.toLowerCase();
        t = t.replaceAll("[\\u2600-\\u27BF\\uD83C-\\uDBFF\\uDC00-\\uDFFF\\s]", "").trim();
        if (t.isEmpty()) return null;
        if (t.contains("quickopen")) return IconManager.searchIcon();
        if (t.contains("localhistory") || t.contains("history")) return IconManager.icon("history");
        if (t.contains("newproject")) return IconManager.newFileIcon();
        if (t.contains("newfile") || t.contains("newfile")) return IconManager.newFileIcon();
        if (t.contains("newfolder")) return IconManager.newFolderIcon();
        if (t.contains("saveas") || t.contains("saveas")) return IconManager.icon(IconManager.SAVE_AS);
        if (t.contains("saveall") || t.contains("saveall")) return IconManager.icon(IconManager.SAVE);
        if (t.contains("closefile") || t.contains("closeall") || t.contains("exit") || t.contains("close"))
            return IconManager.icon(IconManager.CLOSE, 16);
        if (t.contains("export") || t.contains("zip")) return IconManager.icon(IconManager.ZIP);
        if (t.contains("print")) return IconManager.icon(IconManager.DOWNLOAD);
        if (t.contains("save")) return IconManager.saveIcon();
        if (t.contains("undo")) return IconManager.undoIcon();
        if (t.contains("redo")) return IconManager.redoIcon();
        if (t.contains("cut")) return IconManager.cutIcon();
        if (t.contains("copy")) return IconManager.copyIcon();
        if (t.contains("paste")) return IconManager.pasteIcon();
        if (t.contains("find") && t.contains("replace")) return IconManager.searchIcon();
        if (t.contains("find")) return IconManager.searchIcon();
        if (t.contains("select")) return IconManager.icon(IconManager.COPY);
        if (t.contains("sidebar") || t.contains("statusbar")) return IconManager.menuIcon();
        if (t.contains("imagepreview")) return IconManager.searchIcon();
        if (t.contains("zoomin") || t.contains("zoomout")) return IconManager.searchIcon();
        if (t.contains("resetzoom")) return IconManager.refreshIcon();
        if (t.contains("wordwrap")) return IconManager.icon(IconManager.FILE_CODE);
        if (t.contains("expandselection") || t.contains("shrinkselection")) return IconManager.searchIcon();
        if (t.contains("splitintolines") || t.contains("joinlines")) return IconManager.icon(IconManager.EDIT);
        if (t.contains("switchtab")) return IconManager.menuIcon();
        if (t.contains("showinexplorer") || t.contains("showin")) return IconManager.folderIcon();
        if (t.contains("commandpalette")) return IconManager.searchIcon();
        if (t.contains("boilerplate") || t.contains("cssreset") || t.contains("consolelog")
            || t.contains("function") || t.contains("component")
            || t.contains("forloop") || t.contains("ifstatement"))
            return IconManager.icon(IconManager.FILE_CODE);
        if (t.contains("colorpicker")) return IconManager.icon("history");
        if (t.contains("apitester") || t.contains("api")) return IconManager.cloudUploadIcon();
        if (t.contains("sqlrunner") || t.contains("sql")) return IconManager.databaseIcon();
        if (t.contains("erdiagram") || t.contains("diagram")) return IconManager.databaseIcon();
        if (t.contains("dependency")) return IconManager.databaseIcon();
        if (t.contains("figma") || t.contains("htmlto")) return IconManager.icon(IconManager.FILE_CODE);
        if (t.contains("voice")) return IconManager.robotIcon();
        if (t.contains("comment") || t.contains("toggle")) return IconManager.icon(IconManager.FILE_CODE);
        if (t.contains("indent")) return IconManager.icon(IconManager.MENU);
        if (t.contains("preview") || t.contains("live")) return IconManager.playIcon();
        if (t.contains("browser") || t.contains("run")) return IconManager.playIcon();
        if (t.contains("debug") || t.contains("bug")) return IconManager.bugIcon();
        if (t.contains("terminal") || t.contains("console")) return IconManager.terminalIcon();
        if (t.contains("git") || t.contains("branch") || (t.contains("init") && (t.contains("repo") || t.contains("repository"))))
            return IconManager.gitIcon();
        if (t.contains("ai") || t.contains("robot") || t.contains("assistant") || t.contains("generate"))
            return IconManager.robotIcon();
        if (t.contains("quick") && t.contains("apk")) return IconManager.androidIcon();
        if (t.contains("build") || t.contains("apk") || t.contains("android")) return IconManager.androidIcon();
        if (t.contains("deploy") || t.contains("cloud")) return IconManager.cloudUploadIcon();
        if (t.contains("settings") || t.contains("preference")) return IconManager.settingsIcon();
        if (t.contains("folder") || t.contains("open") || t.contains("browse")) return IconManager.folderIcon();
        if (t.contains("project") || t.contains("properties")) return IconManager.icon(IconManager.FOLDER, 16);
        if (t.contains("home") || t.contains("welcome") || t.contains("screen")) return IconManager.homeIcon();
        if (t.contains("refresh") || t.contains("reload")) return IconManager.refreshIcon();
        if (t.contains("database") || t.contains("db") || t.contains("viewer")) return IconManager.databaseIcon();
        if (t.contains("plugin") || t.contains("extensions") || t.contains("marketplace") || t.contains("install"))
            return IconManager.packageIcon();
        if (t.contains("duplicate") || t.contains("delete")) return IconManager.icon(IconManager.CLOSE, 16);
        if (t.contains("outdent")) return IconManager.icon(IconManager.MENU);
        if (t.contains("insertsnippet")) return IconManager.icon(IconManager.FILE_CODE);
        if (t.contains("minify") || t.contains("beautify") || t.contains("format")) return IconManager.icon(IconManager.FILE_CODE);
        if (t.contains("snippet")) return IconManager.icon(IconManager.FILE_CODE);
        if (t.contains("todo") || t.contains("fixme")) return IconManager.icon(IconManager.FILE_CODE);
        if (t.contains("develop")) return IconManager.robotIcon();
        if (t.contains("help") || t.contains("about") || t.contains("docs") || t.contains("keyboard") || t.contains("update") || t.contains("issue"))
            return IconManager.icon(IconManager.SEARCH, 16);
        if (t.contains("window") || t.contains("minimize") || t.contains("minimize")) return IconManager.icon(IconManager.CLOSE, 16);
        if (t.contains("menu") || t.contains("view")) return IconManager.menuIcon();
        if (t.contains("edit")) return IconManager.icon(IconManager.EDIT);
        if (t.contains("navigate") || t.contains("go")) return IconManager.icon(IconManager.UNDO);
        if (t.contains("tool")) return IconManager.settingsIcon();
        if (t.contains("projectmap") || t.contains("projectreplay")) return IconManager.icon(IconManager.FOLDER, 16);
        if (t.equals("file")) return IconManager.fileIcon();
        if (t.equals("sourcecontrol")) return IconManager.gitIcon();
        if (t.equals("selection")) return IconManager.icon(IconManager.COPY);
        if (t.equals("navigate")) return IconManager.icon(IconManager.UNDO);
        if (t.equals("plugins")) return IconManager.packageIcon();
        if (t.equals("run")) return IconManager.playIcon();
        if (t.equals("window")) return IconManager.icon(IconManager.CLOSE, 16);
        return null;
    }

    // ================================================================
    //   BREADCRUMB
    // ================================================================

    private void updateBreadcrumb(File file) {
        if (breadcrumbBar != null) {
            breadcrumbBar.setProjectRoot(projectRoot);
            breadcrumbBar.setCurrentFile(file);
            breadcrumbBar.update();
        }
    }

    // ================================================================
    //   UNIFIED FILE TREE (projects as roots, files nested inside)
    // ================================================================

    private void setupFileTree() {
        fileTree.setShowRoot(false);
        fileTree.setCellFactory(tv -> new AdvancedTreeCell());

        // Double-click to open files/directories (no single-click open)
        fileTree.setOnMousePressed(e -> {
            if (e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY) {
                TreeItem<FileTreeItem> sel = fileTree.getSelectionModel().getSelectedItem();
                if (sel != null && sel.getValue() != null) {
                    File f = sel.getValue().getFile();
                    if (f.isDirectory()) {
                        if (isProjectRoot(f)) {
                            ProjectType type = ProjectsStore.getProjectType(f);
                            if (type == ProjectType.VISUAL) {
                                openVisualProject(f);
                            } else {
                                switchToProject(f);
                            }
                        }
                    } else {
                        openFile(f);
                    }
                }
            }
        });

        fileTree.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                TreeCell<FileTreeItem> cell = findTreeCell(e.getPickResult().getIntersectedNode());
                if (cell != null) {
                    if (cell.getTreeItem() != null) fileTree.getSelectionModel().select(cell.getTreeItem());
                } else {
                    fileTree.getSelectionModel().clearSelection();
                    if (treeContextMenu != null) { treeContextMenu.hide(); treeContextMenu = null; }
                }
            } else if (e.getButton() == MouseButton.PRIMARY) {
                TreeCell<FileTreeItem> cell = findTreeCell(e.getPickResult().getIntersectedNode());
                if (cell == null || cell.getTreeItem() == null) {
                    fileTree.getSelectionModel().clearSelection();
                }
                if (treeContextMenu != null) { treeContextMenu.hide(); treeContextMenu = null; }
            }
        });

        fileTree.setOnContextMenuRequested(this::onTreeContextMenu);

        fileTree.setOnKeyPressed(e -> {
            TreeItem<FileTreeItem> sel = fileTree.getSelectionModel().getSelectedItem();
            if (e.getCode() == KeyCode.DELETE && sel != null) {
                deleteItem(sel);
            } else if (e.getCode() == KeyCode.F2 && sel != null) {
                renameItem(sel);
            } else if (e.isShortcutDown() && e.getCode() == KeyCode.C && sel != null) {
                copyFileToClipboard(sel);
            } else if (e.isShortcutDown() && e.getCode() == KeyCode.X && sel != null) {
                cutFileToClipboard(sel);
            } else if (e.isShortcutDown() && e.getCode() == KeyCode.V) {
                pasteFilesFromClipboard(sel);
            }
        });

        // Filter text field: rebuild tree on keystroke with debounce-like rebuild
        treeFilter.textProperty().addListener((obs, old, txt) -> {
            refreshTree();
        });

        // Close context menu on any click outside the tree
        rootPane.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (treeContextMenu != null) {
                Node target = (Node) e.getTarget();
                while (target != null) {
                    if (target == fileTree) return;
                    target = target.getParent();
                }
                treeContextMenu.hide();
                treeContextMenu = null;
            }
        });
    }

    private boolean isProjectRoot(File dir) {
        return dir.getParentFile() != null
            && dir.getParentFile().equals(ProjectsStore.getProjectsRoot());
    }

    private void refreshTree() {
        // Save selection
        File selectedFile = null;
        TreeItem<FileTreeItem> selectedItem = fileTree.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem.getValue() != null) {
            selectedFile = selectedItem.getValue().getFile();
        }

        // Save expanded state
        java.util.Set<String> expandedPaths = new java.util.HashSet<>();
        if (fileTree.getRoot() != null) {
            saveExpandedState(fileTree.getRoot(), expandedPaths);
        }

        java.util.List<File> projects = ProjectsStore.scanProjects();
        String txt = projects.size() + " projects";
        projectsCountLabel.setText(txt);
        projectsCountLabel2.setText(projectsCountLabel2 != null ? txt : "");

        String filter = treeFilter != null ? treeFilter.getText().trim() : "";
        boolean filtering = !filter.isEmpty();

        TreeItem<FileTreeItem> root = new TreeItem<>(new FileTreeItem(ProjectsStore.getProjectsRoot()));
        root.setExpanded(true);

        for (File projectDir : projects) {
            TreeItem<FileTreeItem> projectItem = buildProjectTree(projectDir, filter);
            if (filtering || projectDir.equals(this.projectRoot)) {
                projectItem.setExpanded(true);
            }
            root.getChildren().add(projectItem);
        }

        fileTree.setRoot(root);

        // Restore expanded state
        restoreExpandedState(fileTree.getRoot(), expandedPaths);

        // Restore selection
        if (selectedFile != null) {
            restoreSelection(fileTree.getRoot(), selectedFile);
        }
    }

    private void saveExpandedState(TreeItem<FileTreeItem> item, java.util.Set<String> expandedPaths) {
        if (item == null) return;
        if (item.isExpanded() && item.getValue() != null) {
            expandedPaths.add(item.getValue().getFile().getAbsolutePath());
        }
        for (TreeItem<FileTreeItem> child : item.getChildren()) {
            saveExpandedState(child, expandedPaths);
        }
    }

    private void restoreExpandedState(TreeItem<FileTreeItem> item, java.util.Set<String> expandedPaths) {
        if (item == null) return;
        if (item.getValue() != null && expandedPaths.contains(item.getValue().getFile().getAbsolutePath())) {
            item.setExpanded(true);
        }
        for (TreeItem<FileTreeItem> child : item.getChildren()) {
            restoreExpandedState(child, expandedPaths);
        }
    }

    private void restoreSelection(TreeItem<FileTreeItem> item, File selectedFile) {
        if (item == null || selectedFile == null) return;
        if (item.getValue() != null && item.getValue().getFile().equals(selectedFile)) {
            fileTree.getSelectionModel().select(item);
            return;
        }
        for (TreeItem<FileTreeItem> child : item.getChildren()) {
            restoreSelection(child, selectedFile);
        }
    }

    private TreeItem<FileTreeItem> buildProjectTree(File dir, String filter) {
        TreeItem<FileTreeItem> item = new TreeItem<>(new FileTreeItem(dir));
        boolean filterActive = filter != null && !filter.isEmpty();
        try {
            File[] children = dir.listFiles();
            if (children != null) {
                java.util.Arrays.sort(children, (a, b) -> {
                    if (a.isDirectory() != b.isDirectory()) return a.isDirectory() ? -1 : 1;
                    return a.getName().compareToIgnoreCase(b.getName());
                });
                for (File child : children) {
                    if (child.isHidden()) continue;
                    if (ProjectMeta.isMarkerFile(child)) continue;
                    boolean matches = !filterActive || child.getName().toLowerCase().contains(filter.toLowerCase());
                    if (child.isDirectory()) {
                        TreeItem<FileTreeItem> childTree = buildProjectTree(child, filter);
                        if (matches || !childTree.getChildren().isEmpty()) {
                            if (filterActive) childTree.setExpanded(true);
                            item.getChildren().add(childTree);
                        }
                    } else if (matches) {
                        item.getChildren().add(new TreeItem<>(new FileTreeItem(child)));
                    }
                }
            }
        } catch (Exception ignored) { }
        return item;
    }

    private void setupPreview() {
        debuggerPanel = new DebuggerPanel();
        debuggerPanel.attach(webPreview.getEngine());

        aiPanel = new AiPanel();
        terminalPanel = new TerminalPanel();
        gitPanel = new GitPanel();
        todoPanel = new TodoPanel();
        if (projectRoot != null) todoPanel.setProjectRoot(projectRoot);
        todoPanel.setOnOpenFile(item -> {
            if (tabManager == null) return;
            CodeEditor editor = tabManager.getEditorForFile(item.file);
            if (editor == null) {
                editor = tabManager.openCodeFile(item.file);
            } else {
                tabManager.selectIfOpen(item.file);
            }
            if (editor != null) {
                editor.moveTo(item.line, item.column >= 0 ? item.column : 0);
            }
        });

        clipboardHistoryPanel = new ClipboardHistoryPanel();
        scratchPadPanel = new ScratchPadPanel();

        if (breadcrumbBar != null) {
            breadcrumbBar.setOnSegmentClick(() -> {});
        }

        if (mainSplit != null) {
            mainSplit.getItems().addAll(debuggerPanel, aiPanel, terminalPanel, gitPanel, clipboardHistoryPanel, scratchPadPanel);
            splitItems = new HashMap<>();
            splitItems.put(0, mainSplit.getItems().get(0)); // sidebar
            splitItems.put(1, mainSplit.getItems().get(1)); // editorContainer
            splitItems.put(2, mainSplit.getItems().get(2)); // previewContainer
            splitItems.put(3, debuggerPanel);
            splitItems.put(4, aiPanel);
            splitItems.put(5, terminalPanel);
            splitItems.put(6, gitPanel);
            splitItems.put(7, todoPanel);
            splitItems.put(8, clipboardHistoryPanel);
            splitItems.put(9, scratchPadPanel);

            mainSplit.getItems().removeAll(debuggerPanel, aiPanel, terminalPanel, gitPanel, clipboardHistoryPanel, scratchPadPanel);
        }
    }

    private void toggleSplitItem(int index, boolean show) {
        if (splitItems == null) return;
        Node node = splitItems.get(index);
        if (node == null) return;
        if (show && !mainSplit.getItems().contains(node)) {
            int insertAt = 0;
            for (int i = 0; i < index; i++) {
                Node n = splitItems.get(i);
                if (n != null && mainSplit.getItems().contains(n)) insertAt++;
            }
            mainSplit.getItems().add(Math.min(insertAt, mainSplit.getItems().size()), node);
        } else if (!show) {
            mainSplit.getItems().remove(node);
        }
    }

    private void setupTabManager() {
        try {
            editorTabs.getClass().getMethod("setTabDragPolicy", Enum.class)
                .invoke(editorTabs, Enum.valueOf(
                    (Class<Enum>) Class.forName("javafx.scene.control.TabPane$TabDragPolicy"), "REORDER"));
        } catch (Exception ignored) {}
        tabManager = new TabManager(editorTabs);
        tabManager.setErrorHandler(this::showError);
        tabManager.setOnFileOpened(file -> {
            fileTypeLabel.setText(extensionOf(file));
            statusLabel.setText("Opened " + file.getName());
            if (isHtmlFamily(file)) refreshPreview();
            SessionManager.recordOpenFiles(projectRoot, tabManager.allOpenFiles());
            updateBreadcrumb(file);
            // Notify LSP server about the opened document
            CodeEditor ed = tabManager.getEditorForFile(file);
            if (ed != null && ed.getLspIntegration() != null) {
                ed.getLspIntegration().openDocument(file);
            }
        });
        tabManager.setOnFileSaved(file -> {
            statusLabel.setText("Saved " + file.getName());
            refreshPreview();
        });
        tabManager.setOnContentChanged(file -> {
            if (previewVisible) {
                refreshPreviewDebounced();
            }
            scheduleAutoSave();
        });
        tabManager.setOnCaretMoved(editor -> {
            updateCursorPos(editor);
            File f = tabManager.getActiveFile();
            if (f != null) updateBreadcrumb(f);
        });
        tabManager.setOnBreakpointToggled((file, lineIndex) -> {
            if (debuggerPanel != null) debuggerPanel.toggleBreakpoint(lineIndex);
        });
        tabManager.setOnEditorCreated(editor -> {
            if (debuggerPanel != null) {
                debuggerPanel.setOnErrorReported(error -> {
                    editor.reportRuntimeError(error);
                });
            }
            // Wire LSP integration when editor is created
            com.eagle.lsp.LspLanguageServer lspServer = com.eagle.lsp.LspLanguageServer.getInstance();
            lspServer.setWorkspaceRoot(projectRoot != null ? projectRoot.getAbsolutePath() : null);
            com.eagle.lsp.LspIntegration lsp = new com.eagle.lsp.LspIntegration(editor, lspServer);
            editor.setLspIntegration(lsp);
            // Wire AI inline completion provider
            editor.setAiInlineProvider(getAiInlineProvider());
            // Wire navigation callbacks to EditorController methods
            editor.setOnGoToDefinition(() -> Platform.runLater(this::onGoToDefinition));
            editor.setOnFindReferences(() -> Platform.runLater(this::onFindReferences));
            editor.setOnRenameSymbol(() -> Platform.runLater(this::onRenameSymbol));
        });

        // Update code outline when active tab changes
        editorTabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null && aiPanel != null) {
                CodeEditor ed = tabManager.getEditorForTab(newTab);
                if (ed != null) {
                    aiPanel.setActiveEditor(ed);
                }
            }
        });
    }

    // ================================================================
    //   PROJECT INITIALIZATION
    // ================================================================

    private void initProject(File projectDir) {
        this.projectRoot = projectDir;
        projectNameLabel.setText(projectDir.getName());
        projectsRootLabel.setText(ProjectsStore.getProjectsRoot().getAbsolutePath());
        statusLabel.setText("Project: " + projectDir.getAbsolutePath());
        refreshTree();
        SessionManager.saveLastSession(projectDir);
        if (terminalPanel != null) terminalPanel.setWorkingDir(projectDir);
        if (gitPanel != null) gitPanel.setProjectDir(projectDir);
        if (breadcrumbBar != null) breadcrumbBar.setProjectRoot(projectDir);
        if (aiPanel != null) {
            aiPanel.setProjectRoot(projectDir);
            aiPanel.setOnFileCreated(this::refreshTree);
        }

        java.util.List<String> lastFiles = SessionManager.getLastOpenFiles(projectDir);
        if (!lastFiles.isEmpty()) {
            for (String relPath : lastFiles) {
                File f = new File(projectDir, relPath);
                if (f.exists() && f.isFile()) openFile(f);
            }
        } else {
            ProjectType type = ProjectsStore.getProjectType(projectDir);
            String defaultFile = (type == ProjectType.ANDROID_JS) ? "index.js" : "index.html";
            File index = new File(projectDir, defaultFile);
            if (index.exists()) openFile(index);
        }

        // Auto-detect project technologies and configure editor
        detectProjectTechnologies();
    }

    private EditorController switchToProject(File projectDir) {
        if (projectDir == null || !projectDir.isDirectory()) return null;
        if (projectDir.equals(this.projectRoot)) return this;
        try {
            return openProject(projectDir);
        } catch (IOException e) {
            showError("Failed to switch project: " + e.getMessage());
            return null;
        }
    }

    private void openVisualProject(File projectDir) {
        Alert a = new Alert(Alert.AlertType.INFORMATION,
            "Visual Builder is currently under development.\nStay tuned for future updates!");
        a.setTitle("Under Development");
        a.setHeaderText(null);
        a.showAndWait();
    }

    // ================================================================
    //   PROJECT ROOT & NEW PROJECT
    // ================================================================

    @FXML
    private void onBrowseProjectsRoot() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Projects Root Folder");
        chooser.setInitialDirectory(ProjectsStore.getProjectsRoot());
        File dir = chooser.showDialog(rootPane.getScene().getWindow());
        if (dir != null) {
            ProjectsStore.setProjectsRoot(dir);
            projectsRootLabel.setText(dir.getAbsolutePath());
            refreshTree();
            statusLabel.setText("Projects root changed to: " + dir.getAbsolutePath());
        }
    }

    @FXML
    private void onNewProjectFromEditor() {
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
            refreshTree();

            ProjectType type = ProjectsStore.getProjectType(projectDir);
            if (type == ProjectType.VISUAL) {
                openVisualProject(projectDir);
            } else {
                switchToProject(projectDir);
            }
            statusLabel.setText("Created project: " + projectDir.getName());
        } catch (IOException e) {
            showError("Failed to create project: " + e.getMessage());
        }
    }
    // ================================================================
    //   FILE OPENING

    @FXML
    private void onRefreshProject() {
        refreshTree();
        statusLabel.setText("Refreshed project files");
    }
    // ================================================================

    private void openFile(File file) {
        if (file == null || !file.exists()) return;
        if (tabManager.isOpen(file)) {
            tabManager.selectIfOpen(file);
            return;
        }
        if (MediaViewer.isMediaFile(file)) {
            tabManager.openMediaFile(file);
            fileTypeLabel.setText(extensionOf(file));
            statusLabel.setText("Viewing " + file.getName());
            return;
        }
        if (!FileIconUtil.isEditable(file)) {
            try {
                java.awt.Desktop.getDesktop().open(file);
            } catch (Exception ex) {
                statusLabel.setText("Cannot open file type: " + file.getName());
            }
            return;
        }
        tabManager.openCodeFile(file);
        SessionManager.recordOpenFiles(projectRoot, tabManager.allOpenFiles());
    }

    // ================================================================
    //   UNIFIED TREE CONTEXT MENU
    // ================================================================

    private javafx.scene.control.TreeCell<FileTreeItem> findTreeCell(javafx.scene.Node node) {
        while (node != null && !(node instanceof javafx.scene.control.TreeCell)) {
            node = node.getParent();
        }
        return (javafx.scene.control.TreeCell<FileTreeItem>) node;
    }

    private void onTreeContextMenu(javafx.scene.input.ContextMenuEvent event) {
        Node picked = event.getPickResult().getIntersectedNode();
        TreeCell<FileTreeItem> cell = (picked != null) ? findTreeCell(picked) : null;
        TreeItem<FileTreeItem> cellItem = (cell != null) ? cell.getTreeItem() : null;
        if (cellItem != null) fileTree.getSelectionModel().select(cellItem);
        final TreeItem<FileTreeItem> sel = cellItem != null ? cellItem : fileTree.getSelectionModel().getSelectedItem();
        if (sel == null || sel.getValue() == null) {
            event.consume();
            return;
        }
        File file = sel.getValue().getFile();
        boolean isDirectory = file.isDirectory();
        boolean isProjectRootNode = isDirectory && isProjectRoot(file);

        ContextMenu menu = new ContextMenu();

        if (isProjectRootNode) {
            // --- Project-level context menu ---
            ProjectType type = ProjectsStore.getProjectType(file);
            MenuItem openItem = new MenuItem("Open Project");
            openItem.setOnAction(e -> switchToProject(file));
            menu.getItems().add(openItem);

            if (type == ProjectType.CODE) {
                MenuItem setVisual = new MenuItem("Set as VISUAL Project");
                setVisual.setOnAction(e -> toggleProjectType(file));
                menu.getItems().add(setVisual);
                MenuItem setAndroid = new MenuItem("Set as ANDROID JS Project");
                setAndroid.setOnAction(e -> {
                    ProjectMeta.write(file, ProjectType.ANDROID_JS);
                    refreshTree();
                    statusLabel.setText("Switched to Android JS Project");
                });
                menu.getItems().add(setAndroid);
            } else if (type == ProjectType.VISUAL) {
                MenuItem setCode = new MenuItem("Set as CODE Project");
                setCode.setOnAction(e -> toggleProjectType(file));
                menu.getItems().add(setCode);
                MenuItem setAndroid = new MenuItem("Set as ANDROID JS Project");
                setAndroid.setOnAction(e -> {
                    ProjectMeta.write(file, ProjectType.ANDROID_JS);
                    refreshTree();
                    statusLabel.setText("Switched to Android JS Project");
                });
                menu.getItems().add(setAndroid);
            } else {
                MenuItem setCode = new MenuItem("Set as CODE Project");
                setCode.setOnAction(e -> { ProjectMeta.write(file, ProjectType.CODE); refreshTree(); statusLabel.setText("Switched to Code Project"); });
                menu.getItems().add(setCode);
                MenuItem setVisual = new MenuItem("Set as VISUAL Project");
                setVisual.setOnAction(e -> { ProjectMeta.write(file, ProjectType.VISUAL); refreshTree(); statusLabel.setText("Switched to Visual Project"); });
                menu.getItems().add(setVisual);
            }

            menu.getItems().add(new SeparatorMenuItem());

            MenuItem aiDevelopItem = new MenuItem("Develop Project with AI...");
            aiDevelopItem.setOnAction(e -> onDevelopProjectWithAi());
            MenuItem aiChatItem = new MenuItem("AI Chat About Project...");
            aiChatItem.setOnAction(e -> onAiChatProject());
            menu.getItems().addAll(aiDevelopItem, aiChatItem);
            menu.getItems().add(new SeparatorMenuItem());

            MenuItem newFileItem = new MenuItem("New File");
            newFileItem.setOnAction(e -> createNewFileIn(file));
            MenuItem newFolderItem = new MenuItem("New Folder");
            newFolderItem.setOnAction(e -> createNewFolderIn(file));
            menu.getItems().addAll(newFileItem, newFolderItem);
            menu.getItems().add(new SeparatorMenuItem());

            MenuItem renameItem = new MenuItem("Rename  (F2)");
            renameItem.setOnAction(e -> renameProject(file));
            MenuItem duplicateItem = new MenuItem("Duplicate Project");
            duplicateItem.setOnAction(e -> duplicateProject(file));
            MenuItem deleteItem = new MenuItem("Delete Project  (Del)");
            deleteItem.setOnAction(e -> deleteProject(file));
            menu.getItems().addAll(renameItem, duplicateItem, deleteItem);
            menu.getItems().add(new SeparatorMenuItem());

            MenuItem copyItem = new MenuItem("Copy  (Ctrl+C)");
            copyItem.setOnAction(e -> copyFileToClipboard(sel));
            MenuItem cutItem = new MenuItem("Cut  (Ctrl+X)");
            cutItem.setOnAction(e -> cutFileToClipboard(sel));
            MenuItem pasteItem = new MenuItem("Paste  (Ctrl+V)");
            pasteItem.setOnAction(e -> pasteFilesFromClipboard(sel));
            menu.getItems().addAll(copyItem, cutItem, pasteItem);
            menu.getItems().add(new SeparatorMenuItem());

            MenuItem exportZipItem = new MenuItem("Export as ZIP");
            exportZipItem.setOnAction(e -> exportProjectAsZip(file));
            MenuItem showExplorerItem = new MenuItem("Show in Explorer");
            showExplorerItem.setOnAction(e -> showInExplorer(file));
            MenuItem searchItem = new MenuItem("Search in Project  (Ctrl+Shift+F)");
            searchItem.setOnAction(e -> searchInProject(file));
            menu.getItems().add(searchItem);
            menu.getItems().add(new SeparatorMenuItem());

            MenuItem copyPathItem = new MenuItem("Copy Path");
            copyPathItem.setOnAction(e -> copyToClipboard(file.getAbsolutePath()));
            menu.getItems().addAll(exportZipItem, showExplorerItem, copyPathItem);

        } else if (isDirectory) {
            // --- Sub-directory context menu ---
            MenuItem newFileItem = new MenuItem("New File");
            newFileItem.setOnAction(e -> createNewFileIn(file));
            MenuItem newFolderItem = new MenuItem("New Folder");
            newFolderItem.setOnAction(e -> createNewFolderIn(file));
            menu.getItems().addAll(newFileItem, newFolderItem);
            menu.getItems().add(new SeparatorMenuItem());

            MenuItem renameItem = new MenuItem("Rename  (F2)");
            renameItem.setOnAction(e -> renameItem(sel));
            MenuItem deleteItem = new MenuItem("Delete  (Del)");
            deleteItem.setOnAction(e -> deleteItem(sel));
            menu.getItems().addAll(renameItem, deleteItem);
            menu.getItems().add(new SeparatorMenuItem());

            MenuItem copyItem = new MenuItem("Copy  (Ctrl+C)");
            copyItem.setOnAction(e -> copyFileToClipboard(sel));
            MenuItem cutItem = new MenuItem("Cut  (Ctrl+X)");
            cutItem.setOnAction(e -> cutFileToClipboard(sel));
            MenuItem pasteItem = new MenuItem("Paste  (Ctrl+V)");
            pasteItem.setOnAction(e -> pasteFilesFromClipboard(sel));
            menu.getItems().addAll(copyItem, cutItem, pasteItem);
            menu.getItems().add(new SeparatorMenuItem());

            MenuItem copyPathItem = new MenuItem("Copy Path");
            copyPathItem.setOnAction(e -> copyToClipboard(file.getAbsolutePath()));
            MenuItem showExplorerItem = new MenuItem("Show in Explorer");
            showExplorerItem.setOnAction(e -> showInExplorer(file));
            menu.getItems().addAll(copyPathItem, showExplorerItem);
            menu.getItems().add(new SeparatorMenuItem());
            MenuItem searchDirItem = new MenuItem("Search in Directory");
            searchDirItem.setOnAction(e -> searchInProject(file));
            menu.getItems().add(searchDirItem);

        } else {
            // --- File context menu ---
            MenuItem openItem = new MenuItem("Open");
            openItem.setOnAction(e -> openFile(file));
            menu.getItems().add(openItem);

            if (isHtmlFamily(file)) {
                MenuItem openBrowser = new MenuItem("Open in Browser");
                openBrowser.setOnAction(e -> openInBrowser(file));
                menu.getItems().add(openBrowser);
            }

            MenuItem openSystem = new MenuItem("Open in System");
            openSystem.setOnAction(e -> openInSystem(file));
            menu.getItems().add(openSystem);
            menu.getItems().add(new SeparatorMenuItem());

            MenuItem renameItem = new MenuItem("Rename  (F2)");
            renameItem.setOnAction(e -> renameItem(sel));
            MenuItem deleteItem = new MenuItem("Delete  (Del)");
            deleteItem.setOnAction(e -> deleteItem(sel));
            MenuItem duplicateItem = new MenuItem("Duplicate");
            duplicateItem.setOnAction(e -> duplicateFile(sel));
            menu.getItems().addAll(renameItem, deleteItem, duplicateItem);
            menu.getItems().add(new SeparatorMenuItem());

            MenuItem copyItem = new MenuItem("Copy  (Ctrl+C)");
            copyItem.setOnAction(e -> copyFileToClipboard(sel));
            MenuItem cutItem = new MenuItem("Cut  (Ctrl+X)");
            cutItem.setOnAction(e -> cutFileToClipboard(sel));
            menu.getItems().addAll(copyItem, cutItem);
            menu.getItems().add(new SeparatorMenuItem());

            MenuItem copyPathItem = new MenuItem("Copy Path");
            copyPathItem.setOnAction(e -> copyToClipboard(file.getAbsolutePath()));
            MenuItem showExplorerItem = new MenuItem("Show in Explorer");
            showExplorerItem.setOnAction(e -> showInExplorer(file));
            menu.getItems().addAll(copyPathItem, showExplorerItem);
        }

        if (treeContextMenu != null) treeContextMenu.hide();
        treeContextMenu = menu;
        menu.show(fileTree, event.getScreenX(), event.getScreenY());
    }

    // ================================================================
    //   CONTEXT MENU ACTIONS
    // ================================================================

    private void renameItem(TreeItem<FileTreeItem> item) {
        File file = item.getValue().getFile();
        TextInputDialog dialog = new TextInputDialog(file.getName());
        dialog.setTitle("Rename");
        dialog.setHeaderText("Rename " + file.getName());
        ThemeManager.getInstance().applyTheme(dialog.getDialogPane().getScene());

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            File renamed = new File(file.getParentFile(), result.get().trim());
            if (file.renameTo(renamed)) {
                tabManager.onFileRenamed(file, renamed);
                refreshTree();
            } else {
                showError("Rename failed.");
            }
        }
    }

    private void deleteItem(TreeItem<FileTreeItem> item) {
        File file = item.getValue().getFile();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + file.getName() + "\"? This cannot be undone.");
        confirm.setTitle("Delete");
        confirm.setHeaderText(null);
        ThemeManager.getInstance().applyTheme(confirm.getDialogPane().getScene());

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (deleteRecursive(file)) {
                tabManager.forceCloseForDeletedFile(file);
                refreshTree();
            } else {
                showError("Could not delete " + file.getName());
            }
        }
    }

    // ---------------------------------------------------------------- SEARCH IN PROJECT

    private void searchInProject(File dir) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Search in Project");
        dialog.setHeaderText("Search for text in " + dir.getName());
        dialog.getEditor().setPromptText("Enter search text...");
        ThemeManager.getInstance().applyTheme(dialog.getDialogPane().getScene());

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String query = result.get().trim();
            performSearch(dir, query);
        }
    }

    private void performSearch(File dir, String query) {
        statusLabel.setText("Searching for \"" + query + "\"...");
        java.util.List<File> matches = new java.util.ArrayList<>();
        searchRecursive(dir, query, matches);

        if (matches.isEmpty()) {
            statusLabel.setText("No matches found for \"" + query + "\"");
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "No files containing \"" + query + "\" were found.");
            alert.setTitle("Search Results");
            alert.setHeaderText(null);
            ThemeManager.getInstance().applyTheme(alert.getDialogPane().getScene());
            alert.show();
            return;
        }

        // Clear previous search highlights from all editors
        for (CodeEditor ed : tabManager.allOpenEditors()) {
            ed.clearSearchHighlight();
        }

        final int MAX_OPEN = 30;
        int opened = 0;
        for (File file : matches) {
            if (opened >= MAX_OPEN) break;
            openFile(file);
            CodeEditor editor = tabManager.getEditorForFile(file);
            if (editor != null) {
                editor.setSearchHighlight(query);
            }
            opened++;
        }

        int total = matches.size();
        String msg = "Found " + total + " file(s) containing \"" + query + "\"";
        if (total > MAX_OPEN) {
            msg += " (opened " + MAX_OPEN + " of " + total + ")";
        }
        statusLabel.setText(msg);

        Alert done = new Alert(Alert.AlertType.INFORMATION, msg);
        done.setTitle("Search Complete");
        done.setHeaderText(null);
        ThemeManager.getInstance().applyTheme(done.getDialogPane().getScene());
        done.show();
    }

    private void searchRecursive(File dir, String query, java.util.List<File> results) {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isHidden()) continue;
            if (!child.isDirectory() && !FileIconUtil.isEditable(child)) continue;
            if (ProjectMeta.isMarkerFile(child)) continue;
            if (child.isDirectory()) {
                searchRecursive(child, query, results);
            } else {
                try {
                    byte[] bytes = java.nio.file.Files.readAllBytes(child.toPath());
                    String content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                    if (content.contains(query)) {
                        results.add(child);
                    }
                } catch (Exception ignored) { }
            }
        }
    }

    private void duplicateFile(TreeItem<FileTreeItem> item) {
        File file = item.getValue().getFile();
        String name = file.getName();
        String baseName = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')) : "";
        String newName = baseName + " (copy)" + ext;
        File copy = new File(file.getParentFile(), newName);

        try {
            if (file.isDirectory()) {
                ProjectsStore.duplicateProject(file, newName);
            } else {
                java.nio.file.Files.copy(file.toPath(), copy.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            refreshTree();
            statusLabel.setText("Duplicated as: " + newName);
        } catch (Exception e) {
            showError("Could not duplicate: " + e.getMessage());
        }
    }

    private void createNewFileIn(File dir) {
        // Preset file types grouped by category
        String[][] presets = {
    {"HTML File", "index.html", "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n    <meta charset=\"UTF-8\">\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n    <title>Document</title>\n    <link rel=\"stylesheet\" href=\"styles.css\">\n</head>\n<body>\n    \n</body>\n</html>"},

    {"CSS File", "styles.css", "/* CSS */\n* {\n    margin: 0;\n    padding: 0;\n    box-sizing: border-box;\n}\n\nbody {\n    font-family: system-ui, -apple-system, sans-serif;\n}\n"},

    {"JavaScript File", "script.js", "// JavaScript\n'use strict';\n\ndocument.addEventListener('DOMContentLoaded', () => {\n    console.log('Script loaded successfully');\n});\n"},

    {"TypeScript File", "app.ts", "interface Config {\n    apiUrl: string;\n    debug: boolean;\n}\n\nconst config: Config = {\n    apiUrl: 'https://api.example.com',\n    debug: true\n};\n\nconsole.log('TypeScript ready');\n"},

    {"JSX File", "component.jsx", "import React from 'react';\n\nexport default function App() {\n    return (\n        <div className=\"app\">\n            <h1>Hello World</h1>\n        </div>\n    );\n}\n"},

    {"TSX File", "component.tsx", "import React from 'react';\n\ninterface Props {\n    title?: string;\n}\n\nexport default function App({ title = 'Hello World' }: Props) {\n    return (\n        <div className=\"app\">\n            <h1>{title}</h1>\n        </div>\n    );\n}\n"},

    // === Java & JVM Languages ===
    {"Java File", "Main.java", "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello, Java! 🚀\");\n    }\n}\n"},

    {"Java Class", "User.java", "public class User {\n    private String name;\n    private int age;\n\n    public User(String name, int age) {\n        this.name = name;\n        this.age = age;\n    }\n\n    public String getName() { return name; }\n\n    @Override\n    public String toString() {\n        return \"User{name='\" + name + \"', age=\" + age + \"}\";\n    }\n}\n"},

    {"Kotlin File", "Main.kt", "fun main() {\n    println(\"Hello from Kotlin! 🔥\")\n}\n"},

    {"Kotlin Class", "User.kt", "data class User(\n    val name: String,\n    val age: Int\n)\n"},

    // === Backend & Others ===
    {"Python File", "main.py", "#!/usr/bin/env python3\n\ndef main():\n    print(\"Hello from Python! 🐍\")\n\nif __name__ == '__main__':\n    main()\n"},

    {"Go File", "main.go", "package main\n\nimport \"fmt\"\n\nfunc main() {\n    fmt.Println(\"Hello from Go! 🐹\")\n}\n"},

    {"Rust File", "main.rs", "fn main() {\n    println!(\"Hello from Rust! 🦀\");\n}\n"},

    {"PHP File", "index.php", "<?php\n\ndeclare(strict_types=1);\n\necho \"Hello from PHP!\";\n"},

    {"Laravel Controller", "UserController.php", "<?php\n\nnamespace App\\Http\\Controllers;\n\nuse Illuminate\\Http\\Request;\n\nclass UserController extends Controller\n{\n    public function index()\n    {\n        return response()->json(['message' => 'Hello from Laravel!']);\n    }\n}\n"},

    // === Frameworks ===
    {"Next.js Page", "page.tsx", "export default function Home() {\n    return (\n        <div>\n            <h1>Welcome to Next.js 15</h1>\n        </div>\n    );\n}\n"},

    {"Vue File", "App.vue", "<template>\n  <div class=\"app\">\n    <h1>Hello Vue 3! ✨</h1>\n  </div>\n</template>\n\n<script setup>\n// Composition API\n</script>\n\n<style scoped>\n.app { text-align: center; padding: 2rem; }\n</style>\n"},

    {"Svelte File", "App.svelte", "<script>\n\tlet name = 'world';\n</script>\n\n<h1>Hello {name}!</h1>\n\n<style>\n\th1 { color: purple; text-align: center; }\n</style>\n"},

    // === Config & Others ===
    {"JSON File", "data.json", "{\n    \"name\": \"Project\",\n    \"version\": \"1.0.0\",\n    \"settings\": {\n        \"theme\": \"dark\"\n    }\n}\n"},

    {"SQL File", "query.sql", "-- SQL\nSELECT * FROM users WHERE active = true;\n"},

    {"Dockerfile", "Dockerfile", "FROM openjdk:21-slim\nWORKDIR /app\nCOPY . .\nRUN ./mvnw clean package -DskipTests\nEXPOSE 8080\nENTRYPOINT [\"java\", \"-jar\", \"target/app.jar\"]\n"},

    {"Gitignore File", ".gitignore", "# Dependencies\nnode_modules/\ntarget/\n*.jar\n\n# Environment\n.env\n.env.local\n\n# IDE\n.vscode/\n.idea/\n\n# Logs\n*.log\n"},

    {"Environment File", ".env", "# Environment Variables\nPORT=3000\nNODE_ENV=development\nDATABASE_URL=your_database_url\nJWT_SECRET=your_jwt_secret\n"},
};

        ChoiceDialog<String> dialog = new ChoiceDialog<>("HTML File",
                java.util.Arrays.stream(presets).map(p -> p[0]).collect(java.util.stream.Collectors.toList()));
        dialog.setTitle("New File");
        dialog.setHeaderText("Create new file in " + dir.getName());
        dialog.setContentText("File type:");
        ThemeManager.getInstance().applyTheme(dialog.getDialogPane().getScene());

        Optional<String> result = dialog.showAndWait();
        if (!result.isPresent()) return;

        String chosenType = result.get();
        String defaultName = "";
        String template = "";
        for (String[] p : presets) {
            if (p[0].equals(chosenType)) {
                defaultName = p[1];
                template = p[2];
                break;
            }
        }

        TextInputDialog nameDialog = new TextInputDialog(defaultName);
        nameDialog.setTitle("New File Name");
        nameDialog.setHeaderText("Enter file name for " + chosenType);
        nameDialog.setContentText("File name:");
        ThemeManager.getInstance().applyTheme(nameDialog.getDialogPane().getScene());

        Optional<String> nameResult = nameDialog.showAndWait();
        if (nameResult.isPresent() && !nameResult.get().trim().isEmpty()) {
            File newFile = new File(dir, nameResult.get().trim());
            try {
                if (newFile.createNewFile()) {
                    if (!template.isEmpty()) {
                        java.nio.file.Files.write(newFile.toPath(), template.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    }
                    refreshTree();
                    openFile(newFile);
                } else {
                    showError("File already exists.");
                }
            } catch (IOException e) {
                showError("Could not create file: " + e.getMessage());
            }
        }
    }

    private void createNewFolderIn(File dir) {
        TextInputDialog dialog = new TextInputDialog("new-folder");
        dialog.setTitle("New Folder");
        dialog.setHeaderText("Create new folder in " + dir.getName());
        dialog.setContentText("Folder name:");
        ThemeManager.getInstance().applyTheme(dialog.getDialogPane().getScene());

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            File newDir = new File(dir, result.get().trim());
            if (newDir.mkdirs()) {
                refreshTree();
            } else {
                showError("Could not create folder.");
            }
        }
    }

    private void renameProject(File projectDir) {
        TextInputDialog dialog = new TextInputDialog(projectDir.getName());
        dialog.setTitle("Rename Project");
        dialog.setHeaderText("Rename project");
        ThemeManager.getInstance().applyTheme(dialog.getDialogPane().getScene());

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String newName = result.get().trim();
            if (ProjectsStore.renameProject(projectDir, newName)) {
                refreshTree();
                if (projectDir.equals(this.projectRoot)) {
                    this.projectRoot = new File(projectDir.getParentFile(), newName);
                    projectNameLabel.setText(newName);
                }
                statusLabel.setText("Renamed to: " + newName);
            } else {
                showError("Rename failed.");
            }
        }
    }

    private void deleteProject(File projectDir) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete project \"" + projectDir.getName() + "\" and all its files?");
        confirm.setTitle("Delete Project");
        confirm.setHeaderText(null);
        ThemeManager.getInstance().applyTheme(confirm.getDialogPane().getScene());

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (ProjectsStore.deleteProject(projectDir)) {
                refreshTree();
                if (projectDir.equals(this.projectRoot)) {
                    try {
                        com.eagle.Main.showWelcomeScreen();
                    } catch (IOException e) {
                        showError("Could not return to welcome screen.");
                    }
                }
                statusLabel.setText("Deleted project: " + projectDir.getName());
                
            } else {
                showError("Could not delete project.");
            }
        }
    }

    private void duplicateProject(File projectDir) {
        TextInputDialog dialog = new TextInputDialog(projectDir.getName() + " (copy)");
        dialog.setTitle("Duplicate Project");
        dialog.setHeaderText("Duplicate as:");
        ThemeManager.getInstance().applyTheme(dialog.getDialogPane().getScene());

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            File copy = ProjectsStore.duplicateProject(projectDir, result.get().trim());
            if (copy != null) {
                refreshTree();
                statusLabel.setText("Duplicated as: " + copy.getName());
            } else {
                showError("Could not duplicate project.");
            }
        }
    }

    private void toggleProjectType(File projectDir) {
        ProjectType current = ProjectsStore.getProjectType(projectDir);
        ProjectType newType;
        switch (current) {
            case CODE:    newType = ProjectType.VISUAL; break;
            case VISUAL:  newType = ProjectType.ANDROID_JS; break;
            default:      newType = ProjectType.CODE; break;
        }

        ProjectMeta.write(projectDir, newType);

        refreshTree();

        statusLabel.setText("Switched to " + newType.getDisplayName());
    }

    private void deleteFileInTree(File file) {
        if (deleteRecursive(file)) {
            refreshTree();
            statusLabel.setText("Deleted: " + file.getName());
        } else {
            showError("Could not delete " + file.getName());
        }
    }

    private void exportProjectAsZip(File projectDir) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Project as ZIP");
        chooser.setInitialFileName(projectDir.getName() + ".zip");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP Archive", "*.zip"));
        File target = chooser.showSaveDialog(rootPane.getScene().getWindow());
        if (target == null) return;
        try {
            ZipExporter.exportDirectory(projectDir, target);
            statusLabel.setText("Exported to " + target.getAbsolutePath());
        } catch (IOException e) {
            showError("Export failed: " + e.getMessage());
        }
    }

    // ================================================================
    //   FILE ACTIONS
    // ================================================================

    private void openInBrowser(File file) {
        try {
            java.awt.Desktop.getDesktop().browse(file.toURI());
        } catch (Exception e) {
            showError("Could not open in browser: " + e.getMessage());
        }
    }

    private void openInSystem(File file) {
        try {
            java.awt.Desktop.getDesktop().open(file);
        } catch (Exception e) {
            statusLabel.setText("Cannot open file: " + e.getMessage());
        }
    }

    private void showInExplorer(File file) {
        try {
            if (file.isDirectory()) {
                java.awt.Desktop.getDesktop().open(file);
            } else {
                java.awt.Desktop.getDesktop().open(file.getParentFile());
            }
        } catch (Exception e) {
            statusLabel.setText("Could not open explorer: " + e.getMessage());
        }
    }

    private void copyToClipboard(String text) {
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
        statusLabel.setText("Copied to clipboard");
    }

    // ================================================================
    //   FILE COPY / CUT / PASTE (Internal + System Clipboard)
    // ================================================================

    private void copyFileToClipboard(TreeItem<FileTreeItem> item) {
        if (item == null) return;
        clipboardFiles = new java.util.ArrayList<>();
        clipboardFiles.add(item.getValue().getFile());
        clipboardIsCut = false;
        clipboardSourceDir = item.getValue().getFile().getParentFile();
        putFilesOnSystemClipboard(clipboardFiles);
        statusLabel.setText("Copied: " + item.getValue().getFile().getName());
    }

    private void cutFileToClipboard(TreeItem<FileTreeItem> item) {
        if (item == null) return;
        clipboardFiles = new java.util.ArrayList<>();
        clipboardFiles.add(item.getValue().getFile());
        clipboardIsCut = true;
        clipboardSourceDir = item.getValue().getFile().getParentFile();
        putFilesOnSystemClipboard(clipboardFiles);
        statusLabel.setText("Cut: " + item.getValue().getFile().getName());
    }

    private void pasteFilesFromClipboard(TreeItem<FileTreeItem> targetItem) {
        File targetDir;
        if (targetItem != null && targetItem.getValue().getFile().isDirectory()) {
            targetDir = targetItem.getValue().getFile();
        } else if (targetItem != null && targetItem.getValue().getFile().isFile()) {
            targetDir = targetItem.getValue().getFile().getParentFile();
        } else {
            targetDir = projectRoot;
        }

        // Try external (system) clipboard first
        javafx.scene.input.Clipboard sysClip = javafx.scene.input.Clipboard.getSystemClipboard();
        List<File> sysFiles = sysClip.getFiles();
        if (sysFiles != null && !sysFiles.isEmpty()) {
            // External paste: copy files from system clipboard
            int count = 0;
            for (File src : sysFiles) {
                try {
                    copyFileToDir(src, targetDir);
                    count++;
                } catch (IOException ex) {
                    statusLabel.setText("Error pasting: " + src.getName());
                    return;
                }
            }
            statusLabel.setText("Pasted " + count + " item(s) from system clipboard");
            refreshTree();
            return;
        }

        // Internal paste
        if (clipboardFiles == null || clipboardFiles.isEmpty()) {
            statusLabel.setText("Nothing to paste");
            return;
        }

        // Don't paste into the same directory for cut
        if (clipboardIsCut && targetDir.equals(clipboardSourceDir)) {
            statusLabel.setText("Same directory – nothing to do");
            return;
        }

        int count = 0;
        for (File src : clipboardFiles) {
            try {
                if (clipboardIsCut) {
                    moveFileToDir(src, targetDir);
                } else {
                    copyFileToDir(src, targetDir);
                }
                count++;
            } catch (IOException ex) {
                statusLabel.setText("Error: " + ex.getMessage());
                refreshTree();
                return;
            }
        }

        if (clipboardIsCut) {
            clipboardFiles = null;
            clipboardIsCut = false;
        }

        statusLabel.setText("Pasted " + count + " item(s)");
        refreshTree();
    }

    private void putFilesOnSystemClipboard(List<File> files) {
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putFiles(files);
        clipboard.setContent(content);
    }

    private void copyFileToDir(File src, File destDir) throws IOException {
        File dest = new File(destDir, src.getName());
        if (src.isDirectory()) {
            copyDirectory(src, dest);
        } else {
            java.nio.file.Files.copy(src.toPath(), dest.toPath(),
                java.nio.file.StandardCopyOption.COPY_ATTRIBUTES,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void moveFileToDir(File src, File destDir) throws IOException {
        File dest = new File(destDir, src.getName());
        if (src.isDirectory()) {
            copyDirectory(src, dest);
            deleteDirectory(src);
        } else {
            java.nio.file.Files.move(src.toPath(), dest.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void copyDirectory(File src, File dest) throws IOException {
        if (!dest.exists()) dest.mkdirs();
        for (File f : src.listFiles()) {
            if (f.isDirectory()) {
                copyDirectory(f, new File(dest, f.getName()));
            } else {
                java.nio.file.Files.copy(f.toPath(), new File(dest, f.getName()).toPath(),
                    java.nio.file.StandardCopyOption.COPY_ATTRIBUTES,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void deleteDirectory(File dir) throws IOException {
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) deleteDirectory(f);
            else f.delete();
        }
        dir.delete();
    }

    // ================================================================
    //   TOOLBAR ACTIONS (existing)
    // ================================================================

    @FXML
    private void onExportZip() {
        exportProjectAsZip(projectRoot);
    }

    @FXML
    private void onSave() {
        File active = tabManager.getActiveFile();
        tabManager.saveActive();
        if (active != null) LocalHistory.saveSnapshot(active);
    }

    @FXML
    private void onSaveAll() {
        tabManager.saveAll();
        // Snapshot the active file (most recently saved)
        File active = tabManager.getActiveFile();
        if (active != null) LocalHistory.saveSnapshot(active);
    }

    private void scheduleAutoSave() {
        if (!EditorSettings.isAutoSave()) return;
        if (autoSaveDebounce != null) autoSaveDebounce.stop();
        autoSaveDebounce = new PauseTransition(Duration.seconds(3));
        autoSaveDebounce.setOnFinished(e -> tabManager.saveAll());
        autoSaveDebounce.play();
    }

    @FXML
    private void onNewFile() {
        File targetDir = getSelectedDirectoryOrRoot();
        if (targetDir != null) createNewFileIn(targetDir);
    }

    @FXML
    private void onNewFolder() {
        File targetDir = getSelectedDirectoryOrRoot();
        if (targetDir != null) createNewFolderIn(targetDir);
    }

    private File getSelectedDirectoryOrRoot() {
        TreeItem<FileTreeItem> sel = fileTree.getSelectionModel().getSelectedItem();
        if (sel != null) {
            File f = sel.getValue().getFile();
            return f.isDirectory() ? f : f.getParentFile();
        }
        return projectRoot;
    }

    @FXML
    private void onOpenFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Open Project Folder");
        File dir = chooser.showDialog(rootPane.getScene().getWindow());
        if (dir != null) {
            try {
                RecentProjectsStore.addRecent(dir);
                if (!new File(dir, ".eagle-project").exists()) {
                    ProjectMeta.write(dir, ProjectType.CODE);
                }
                openProject(dir);
            } catch (IOException e) {
                showError("Failed to open project: " + e.getMessage());
            }
        }
    }

    // ================================================================
    //   COMMAND PALETTE
    // ================================================================

    @FXML
    private void onFormatDocument() {
        CodeEditor activeEditor = tabManager.getActiveEditor();
        if (activeEditor == null) return;
        String text = activeEditor.getText();
        if (text == null || text.isEmpty()) return;
        LanguageType lang = LanguageType.fromFile(tabManager.getActiveFile());
        String formatted = formatCode(text, lang);
        if (formatted != null && !formatted.equals(text)) {
            activeEditor.setText(formatted);
            statusLabel.setText("Document formatted");
        } else {
            statusLabel.setText("No formatting changes needed");
        }
    }

    private String formatCode(String text, LanguageType lang) {
        if (lang == LanguageType.HTML) {
            return formatHtml(text);
        }
        if (lang == LanguageType.JAVASCRIPT || lang == LanguageType.TYPESCRIPT) {
            return formatJs(text);
        }
        if (lang == LanguageType.CSS) {
            return formatCss(text);
        }
        return text;
    }

    private String formatHtml(String html) {
        StringBuilder result = new StringBuilder();
        String[] lines = html.split("\n", -1);
        int indent = 0;
        String indentStr = "    ";
        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            if (trimmed.isEmpty()) { result.append("\n"); continue; }
            int dec = 0;
            String test = trimmed.replaceAll("<!--.*?-->", "");
            if (test.matches("\\s*</[^>]+>.*")) dec = 1;
            if (test.matches("\\s*(\\}>|\\]>|\\s*>\\s*</).*")) dec = 1;
            indent = Math.max(0, indent - dec);
            for (int j = 0; j < indent; j++) result.append(indentStr);
            result.append(trimmed).append("\n");
            if (test.matches("<[^/][^>]*>[^<]*</[^>]+>")) continue;
            if (test.matches("<[^/!?][^>]*>.*") && !test.matches(".*</[^>]+>\\s*$") && !test.endsWith("/>")) indent++;
            if (test.matches(".*<[^/][^>]*>[^<]*$") && !test.matches(".*/>\\s*$")) indent++;
        }
        return result.toString().replaceAll("\\n{2,}", "\n");
    }

    private String formatJs(String js) {
        StringBuilder result = new StringBuilder();
        int indent = 0;
        String indentStr = "    ";
        String[] lines = js.split("\n", -1);
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) { result.append('\n'); continue; }
            int openBraces = 0, closeBraces = 0;
            boolean inStr = false;
            char strChar = 0;
            for (int i = 0; i < trimmed.length(); i++) {
                char c = trimmed.charAt(i);
                if (inStr) {
                    if (c == '\\' && i + 1 < trimmed.length()) { i++; continue; }
                    if (c == strChar) inStr = false;
                } else {
                    if (c == '"' || c == '\'' || c == '`') { inStr = true; strChar = c; }
                    else if (c == '{') openBraces++;
                    else if (c == '}') closeBraces++;
                }
            }
            indent = Math.max(0, indent - closeBraces);
            for (int j = 0; j < indent; j++) result.append(indentStr);
            result.append(trimmed).append('\n');
            indent += openBraces;
        }
        return result.toString().trim();
    }

    private String formatCss(String css) {
        return css.replaceAll("\\s*\\{\\s*", " {\n    ").replaceAll(";\\s*", ";\n    ").replaceAll("\\}\\s*", "\n}\n").trim();
    }

    @FXML
    private void onOpenCommandPalette() {
        CodeEditor activeEditor = tabManager.getActiveEditor();
        if (activeEditor == null) {
            statusLabel.setText("Open a code file first to insert a snippet.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/eagle/fxml/CommandPaletteDialog.fxml"));
            Parent root = loader.load();
            CommandPaletteController controller = loader.getController();

            // Build editor action commands
            List<CommandPaletteController.CommandItem> actions = new java.util.ArrayList<>();
            actions.addAll(Arrays.asList(
                new CommandPaletteController.CommandItem("Save File", "action", "Ctrl+S", () -> onSave()),
                new CommandPaletteController.CommandItem("Save All Files", "action", "Ctrl+Shift+S", () -> onSaveAll()),
                new CommandPaletteController.CommandItem("Close Tab", "action", "", () -> {
                    Tab tab = editorTabs.getSelectionModel().getSelectedItem();
                    if (tab != null) tab.getOnCloseRequest().handle(null);
                }),
                new CommandPaletteController.CommandItem("Format Document", "action", "Ctrl+Shift+F", () -> onFormatDocument()),
                new CommandPaletteController.CommandItem("Toggle Preview", "action", "Ctrl+P", () -> onTogglePreview()),
                new CommandPaletteController.CommandItem("Toggle Sidebar", "action", "Ctrl+B", () -> onToggleSidebar()),
                new CommandPaletteController.CommandItem("Open Settings", "action", "Ctrl+,", () -> onOpenSettings()),
                new CommandPaletteController.CommandItem("Open Folder", "action", "Ctrl+O", () -> onOpenFolder()),
                new CommandPaletteController.CommandItem("Show Find/Replace", "action", "Ctrl+F", () -> onFindReplace()),
                new CommandPaletteController.CommandItem("New File", "action", "Ctrl+N", () -> onNewFile()),
                new CommandPaletteController.CommandItem("New Folder", "action", "Ctrl+Shift+N", () -> onNewFolder()),
                new CommandPaletteController.CommandItem("Refresh Project", "action", "Ctrl+R", () -> onRefreshProject()),
                new CommandPaletteController.CommandItem("Undo", "action", "Ctrl+Z", () -> onUndo()),
                new CommandPaletteController.CommandItem("Redo", "action", "Ctrl+Y", () -> onRedo()),
                new CommandPaletteController.CommandItem("Zoom In", "action", "Ctrl+Plus", () -> onZoomIn()),
                new CommandPaletteController.CommandItem("Zoom Out", "action", "Ctrl+Minus", () -> onZoomOut())
            ));
            actions.addAll(commandsFromPlugins);

            File file = tabManager.getActiveFile();
            LanguageType lang = file != null ? LanguageType.fromFile(file) : LanguageType.PLAIN;
            controller.loadCommands(lang, actions);
            controller.setOnChosen(item -> {
                if (item.insertText != null) {
                    CompletionProvider.Suggestion sug = new CompletionProvider.Suggestion(
                        item.label, item.insertText, item.category, item.codePreview
                    );
                    insertSnippetIntoEditor(activeEditor, sug);
                }
            });

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Commands & Snippets");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(rootPane.getScene().getWindow());
            Scene scene = new Scene(root);
            ThemeManager.getInstance().applyTheme(scene);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
        } catch (IOException e) {
            showError("Failed to open command palette: " + e.getMessage());
        }
    }

    private void insertSnippetIntoEditor(CodeEditor editor, CompletionProvider.Suggestion suggestion) {
        int caret = editor.getCaretPosition();
        String insertText = suggestion.insertText;
        int cursorOffset = insertText.indexOf("{CURSOR}");
        String finalText = insertText.replace("{CURSOR}", "");
        editor.insertText(caret, finalText);
        if (cursorOffset >= 0) {
            editor.moveTo(caret + cursorOffset);
        }
    }

    // ================================================================
    //   ADVANCED SETTINGS
    // ================================================================

    @FXML
    private void onSnippetsManager() {
        SnippetsManagerDialog.show();
    }

    @FXML
    private void onOpenSettings() {
        openSettings();
    }

    public void openSettings() {
        try {
            MonacoEditor editor = tabManager != null && tabManager.getActiveEditor() != null
                ? tabManager.getActiveEditor().getMonacoEditor() : null;
            new SettingsDialog(editor, this::applySettingsToAllEditors).show();
            applySettingsToAllEditors();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Could not open settings: " + e.getMessage());
            alert.setHeaderText(null);
            ThemeManager.getInstance().applyTheme(alert.getDialogPane().getScene());
            alert.showAndWait();
        }
    }

    /** يطبق إعدادات المحرر على كل التبويبات المفتوحة + syntax theme على الواجهة */
    public void applySettingsToAllEditors() {
        if (tabManager != null) {
            for (CodeEditor ed : tabManager.allOpenEditors()) {
                ed.applySettings();
            }
        }
        applySyntaxThemeClass();
    }

    public static void applySettingsToOpenEditors() {
        EditorController instance = getInstance();
        if (instance != null) {
            instance.applySettingsToAllEditors();
        }
    }

    private void applySyntaxThemeClass() {
        String syntaxTheme = EditorSettings.getSyntaxTheme();
        String themeClass;
        switch (syntaxTheme) {
            case "Monokai":          themeClass = "theme-monokai";          break;
            case "Dracula":          themeClass = "theme-dracula";          break;
            case "Solarized":        themeClass = "theme-solarized";        break;
            case "GitHub Light":     themeClass = "theme-github-light";     break;
            case "One Dark":         themeClass = "theme-one-dark";         break;
            case "Nord":             themeClass = "theme-nord";             break;
            case "GitHub Dark":      themeClass = "theme-github-dark";      break;
            case "Atom One Light":   themeClass = "theme-atom-one-light";   break;
            case "Tokyo Night":      themeClass = "theme-tokyo-night";      break;
            case "Catppuccin":       themeClass = "theme-catppuccin";       break;
            case "Ayu Dark":         themeClass = "theme-ayu-dark";         break;
            case "SynthWave '84":    themeClass = "theme-synthwave";        break;
            case "Noctis Lux":       themeClass = "theme-noctis-lux";       break;
            case "Gruvbox Dark":     themeClass = "theme-gruvbox-dark";     break;
            case "Gruvbox Light":    themeClass = "theme-gruvbox-light";    break;
            case "Material Darker":  themeClass = "theme-material-darker";  break;
            case "Material Lighter": themeClass = "theme-material-lighter"; break;
            case "Material Ocean":   themeClass = "theme-material-ocean";   break;
            case "Rose Pine":        themeClass = "theme-rose-pine";        break;
            case "Rose Pine Moon":   themeClass = "theme-rose-pine-moon";   break;
            case "Everforest Dark":  themeClass = "theme-everforest-dark";  break;
            case "Everforest Light": themeClass = "theme-everforest-light"; break;
            case "Night Owl":        themeClass = "theme-night-owl";        break;
            case "Light Owl":        themeClass = "theme-light-owl";        break;
            case "Palenight":        themeClass = "theme-palenight";        break;
            case "Horizon":          themeClass = "theme-horizon";          break;
            case "Panda":            themeClass = "theme-panda";            break;
            case "Shades of Purple": themeClass = "theme-shades-purple";    break;
            case "Monokai Pro":      themeClass = "theme-monokai-pro";      break;
            case "Ayu Light":        themeClass = "theme-ayu-light";        break;
            case "Ayu Mirage":       themeClass = "theme-ayu-mirage";       break;
            case "VSCode Dark+":     themeClass = "theme-vscode-dark";      break;
            case "VSCode Light+":    themeClass = "theme-vscode-light";     break;
            default:                 themeClass = null;
        }
        if (rootPane != null) {
            rootPane.getStyleClass().removeIf(c -> c.startsWith("theme-"));
            if (themeClass != null) {
                rootPane.getStyleClass().add(themeClass);
            }
        }
    }

    // ================================================================
    //   LIVE PREVIEW
    // ================================================================

    @FXML
    private void onTogglePreview() {
        previewVisible = !previewVisible;
        toggleSplitItem(2, previewVisible);
        previewToggleBtn.setText(previewVisible ? "Preview On" : "Preview Off");
        if (previewVisible) refreshPreview();
    }

    private PauseTransition debounce;

    private void refreshPreviewDebounced() {
        if (debounce != null) debounce.stop();
        debounce = new PauseTransition(Duration.millis(400));
        debounce.setOnFinished(e -> refreshPreview());
        debounce.play();
    }

    private void refreshPreview() {
        if (!previewVisible) return;
        File htmlFile = findHtmlContext();
        if (htmlFile == null) return;

        try {
            String html = tabManager.getEditableContentOrDisk(htmlFile);
            html = inlineLocalAssets(html, htmlFile.getParentFile());
            html = injectBaseTag(html, htmlFile.getParentFile());
            webPreview.getEngine().loadContent(html, "text/html");
        } catch (Exception e) {
            statusLabel.setText("Preview error: " + e.getMessage());
        }
    }

    private String injectBaseTag(String html, File baseDir) {
        String baseHref = baseDir.toURI().toString();
        if (!baseHref.endsWith("/")) baseHref += "/";
        String baseTag = "<base href=\"" + baseHref + "\">";
        int headEnd = html.indexOf("</head>");
        if (headEnd >= 0) {
            return html.substring(0, headEnd) + baseTag + html.substring(headEnd);
        }
        int headStart = html.indexOf("<head>");
        if (headStart >= 0) {
            return html.substring(0, headStart + 7) + baseTag + html.substring(headStart + 7);
        }
        int htmlEnd = html.indexOf(">");
        if (htmlEnd >= 0 && html.toLowerCase().startsWith("<html")) {
            return html.substring(0, htmlEnd + 1) + "<head>" + baseTag + "</head>" + html.substring(htmlEnd + 1);
        }
        return "<!DOCTYPE html><html><head>" + baseTag + "</head><body>" + html + "</body></html>";
    }

    private File findHtmlContext() {
        File active = tabManager.getActiveFile();
        if (active != null && isHtmlFamily(active)) return active;
        for (File f : tabManager.allOpenFiles()) {
            if (isHtmlFamily(f)) return f;
        }
        File index = new File(projectRoot, "index.html");
        return index.exists() ? index : null;
    }

    private String inlineLocalAssets(String html, File baseDir) {
        try {
            html = replaceLinkedCss(html, baseDir);
            html = replaceLinkedJs(html, baseDir);
            html = replaceLinkedImages(html, baseDir);
        } catch (Exception ignored) { }
        return html;
    }

    private String replaceLinkedCss(String html, File baseDir) throws IOException {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "<link[^>]+rel=\"stylesheet\"[^>]+href=\"([^\"]+)\"[^>]*>");
        java.util.regex.Matcher m = p.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String href = m.group(1);
            File cssFile = new File(baseDir, href);
            String replacement;
            if (cssFile.exists() && !href.startsWith("http")) {
                String cssContent = tabManager.getEditableContentOrDisk(cssFile);
                replacement = "<style>" + java.util.regex.Matcher.quoteReplacement(cssContent) + "</style>";
            } else {
                replacement = m.group(0);
            }
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String replaceLinkedJs(String html, File baseDir) throws IOException {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "<script[^>]+src=\"([^\"]+)\"[^>]*></script>");
        java.util.regex.Matcher m = p.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String src = m.group(1);
            File jsFile = new File(baseDir, src);
            String replacement;
            if (jsFile.exists() && !src.startsWith("http")) {
                String jsContent = tabManager.getEditableContentOrDisk(jsFile);
                if (debuggerPanel != null) {
                    jsContent = debuggerPanel.injectBreakpoints(jsContent);
                }
                replacement = "<script>" + java.util.regex.Matcher.quoteReplacement(jsContent) + "</script>";
            } else {
                replacement = m.group(0);
            }
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String replaceLinkedImages(String html, File baseDir) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
            "<img[^>]+src=\"([^\"]+)\"");
        java.util.regex.Matcher m = p.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String src = m.group(1);
            if (src.startsWith("http") || src.startsWith("data:")) {
                m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(m.group(0)));
                continue;
            }
            File imgFile = new File(baseDir, src);
            if (imgFile.exists()) {
                try {
                    byte[] bytes = java.nio.file.Files.readAllBytes(imgFile.toPath());
                    String mimeType = guessMimeType(imgFile.getName());
                    String b64 = Base64.getEncoder().encodeToString(bytes);
                    String dataUri = "data:" + mimeType + ";base64," + b64;
                    String replacement = m.group(0).replace("src=\"" + src + "\"", "src=\"" + dataUri + "\"");
                    m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
                } catch (Exception e) {
                    m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(m.group(0)));
                }
            } else {
                m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(m.group(0)));
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String guessMimeType(String name) {
        String n = name.toLowerCase();
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".gif")) return "image/gif";
        if (n.endsWith(".svg")) return "image/svg+xml";
        if (n.endsWith(".webp")) return "image/webp";
        if (n.endsWith(".ico")) return "image/x-icon";
        if (n.endsWith(".bmp")) return "image/bmp";
        return "image/png";
    }

    private boolean isHtmlFamily(File file) {
        String n = file.getName().toLowerCase();
        return n.endsWith(".html") || n.endsWith(".htm");
    }

    private String extensionOf(File file) {
        String n = file.getName();
        int idx = n.lastIndexOf('.');
        return idx >= 0 ? n.substring(idx + 1).toUpperCase() : "";
    }

    private void updateCursorPos(CodeEditor editor) {
        int[] lc = editor.getLineAndColumn();
        cursorPosLabel.setText("Ln " + lc[0] + ", Col " + lc[1]);
    }

    // ================================================================
    //   THEME
    // ================================================================

    @FXML
    private void onToggleTheme() {
        ThemeManager.getInstance().toggleTheme();
        positionKnob(true);
    }

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

    // ================================================================
    //   EXPANDED MENU HANDLERS
    // ================================================================

    // ---- File ----
    @FXML
    private void onSaveAs() {
        CodeEditor ed = tabManager.getActiveEditor();
        if (ed == null) { statusLabel.setText("No file open"); return; }
        File current = tabManager.getActiveFile();
        if (current == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName(current.getName());
        File target = chooser.showSaveDialog(rootPane.getScene().getWindow());
        if (target == null) return;
        try (FileWriter w = new FileWriter(target)) {
            w.write(ed.getText());
            statusLabel.setText("Saved as: " + target.getName());
        } catch (IOException ex) {
            showError("Save failed: " + ex.getMessage());
        }
    }

    @FXML
    private void onQuickOpen() {
        if (projectRoot == null) {
            showError("Open a project first.");
            return;
        }
        QuickOpenDialog.show(projectRoot, file -> {
            if (file != null) {
                Platform.runLater(() -> {
                    openFile(file);
                    statusLabel.setText("Opened: " + file.getName());
                });
            }
        }, rootPane.getScene().getWindow());
    }

    @FXML
    private void onLocalHistory() {
        File active = tabManager.getActiveFile();
        LocalHistory.showHistory(active, rootPane.getScene().getWindow());
    }

    // ---- View ----
    @FXML
    private void onMarkdownPreview() {
        File active = tabManager.getActiveFile();
        if (active == null || !active.getName().toLowerCase().endsWith(".md")) {
            showError("Open a .md file first to preview markdown.");
            return;
        }
        MarkdownPreview.show(active, rootPane.getScene().getWindow());
    }

    @FXML
    private void onImagePreview() {
        File active = tabManager.getActiveFile();
        if (active == null) return;
        String name = active.getName().toLowerCase();
        if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")
            || name.endsWith(".gif") || name.endsWith(".svg") || name.endsWith(".bmp")) {
            ImagePreview.show(active, rootPane.getScene().getWindow());
        } else {
            showError("Open an image file first (.png, .jpg, .gif, .svg)");
        }
    }

    // ---- Tools ----
    @FXML
    private void onColorPicker() {
        String color = ColorPickerTool.show(rootPane.getScene().getWindow());
        if (color != null) {
            CodeEditor ed = tabManager.getActiveEditor();
            if (ed != null) ed.insertText(ed.getCaretPosition(), color);
        }
    }

    @FXML
    private void onApiTester() {
        ApiTester.show(rootPane.getScene().getWindow());
    }

    @FXML
    private void onSqlRunner() {
        SqlRunner.show(rootPane.getScene().getWindow());
    }

    @FXML
    private void onErDiagram() {
        ErDiagram.show(rootPane.getScene().getWindow());
    }

    @FXML
    private void onErDesigner() {
        ErDesigner.show(rootPane.getScene().getWindow());
    }

    @FXML
    private void onProjectMap() {
        if (projectRoot == null) { showError("Open a project first."); return; }
        ProjectMap.show(projectRoot, rootPane.getScene().getWindow());
    }

    @FXML
    private void onLiveShare() {
        LiveShare.show(rootPane.getScene().getWindow());
    }

    @FXML
    private void onFigmaToHtml() {
        FigmaToHtml.show(rootPane.getScene().getWindow());
    }

    @FXML
    private void onVoiceToCode() {
        VoiceToCode.show(rootPane.getScene().getWindow());
    }

    @FXML
    private void onExportFlutter() {
        ExportFlutter.show(rootPane.getScene().getWindow());
    }

    @FXML
    private void onDepVisualizer() {
        if (projectRoot == null) { showError("Open a project first."); return; }
        DepVisualizer.show(projectRoot, rootPane.getScene().getWindow());
    }

    @FXML
    private void onProjectReplay() {
        if (projectRoot == null) { showError("Open a project first."); return; }
        ProjectReplay.show(projectRoot, rootPane.getScene().getWindow());
    }

    // ---- Project ----
    @FXML
    private void onAiChatProject() {
        if (projectRoot == null) {
            showError("Open a project first.");
            return;
        }
        onToggleAiAssistant();
        ProjectAiChat.show(projectRoot, aiPanel, rootPane.getScene().getWindow());
    }

    // ---- Source Control ----
    @FXML
    private void onGitGraph() {
        GitGraph.show(projectRoot, rootPane.getScene().getWindow());
    }

    private void runGitCommand(String... args) {
        if (projectRoot == null) { showError("No project open"); return; }
        try {
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.directory(projectRoot);
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            StringBuilder out = new StringBuilder();
            try (java.io.BufferedReader r = new java.io.BufferedReader(
                    new java.io.InputStreamReader(proc.getInputStream()))) {
                String l;
                while ((l = r.readLine()) != null) out.append(l).append("\n");
            }
            proc.waitFor();
            TextArea ta = new TextArea(out.toString());
            ta.setEditable(false);
            ta.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px;");
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle(String.join(" ", args));
            d.initOwner(rootPane.getScene().getWindow());
            d.getDialogPane().setContent(ta);
            d.getDialogPane().setPrefSize(500, 400);
            d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            d.showAndWait();
            refreshTree();
        } catch (Exception ex) {
            showError("Git error: " + ex.getMessage());
        }
    }

    @FXML
    private void onGitCommit() {
        TextInputDialog d = new TextInputDialog();
        d.setTitle("Git Commit");
        d.setHeaderText("Enter commit message:");
        d.initOwner(rootPane.getScene().getWindow());
        d.showAndWait().ifPresent(msg -> {
            if (!msg.trim().isEmpty()) {
                runGitCommand("git", "add", "-A");
                runGitCommand("git", "commit", "-m", msg);
            }
        });
    }

    @FXML
    private void onGitPush() { runGitCommand("git", "push"); }

    @FXML
    private void onGitPull() { runGitCommand("git", "pull"); }

    @FXML
    private void onGitInit() {
        if (projectRoot == null) return;
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
            "Initialize a Git repository in: " + projectRoot.getName() + "?",
            ButtonType.YES, ButtonType.NO);
        a.initOwner(rootPane.getScene().getWindow());
        if (a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            runGitCommand("git", "init");
        }
    }

    @FXML
    private void onGitStash() {
        TextInputDialog d = new TextInputDialog("WIP");
        d.setTitle("Git Stash");
        d.setHeaderText("Enter stash message:");
        d.initOwner(rootPane.getScene().getWindow());
        d.showAndWait().ifPresent(msg -> runGitCommand("git", "stash", "push", "-m", msg.isEmpty() ? "WIP" : msg));
    }

    @FXML
    private void onGitFetch() { runGitCommand("git", "fetch"); }

    @FXML
    private void onGitReset() {
        if (projectRoot == null) return;
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
            "Reset all uncommitted changes?", ButtonType.YES, ButtonType.NO);
        a.setHeaderText("Git Reset");
        a.initOwner(rootPane.getScene().getWindow());
        if (a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            runGitCommand("git", "reset", "--hard");
        }
    }

    @FXML
    private void onJsonValidator() {
        CodeEditor ed = tabManager.getActiveEditor();
        if (ed == null) { statusLabel.setText("Open a file first"); return; }
        String text = ed.getText();
        try {
            Object parsed = new com.google.gson.Gson().fromJson(text, Object.class);
            String pretty = new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(parsed);
            showInfo("Valid JSON\n\n" + pretty);
        } catch (Exception e) {
            showError("Invalid JSON: " + e.getMessage());
        }
    }

    @FXML
    private void onRegexTester() {
        CodeEditor ed = tabManager.getActiveEditor();
        if (ed == null) { statusLabel.setText("Open a file first"); return; }
        String text = ed.getText();
        TextInputDialog d = new TextInputDialog();
        d.setTitle("Regex Tester");
        d.setHeaderText("Enter regex pattern:");
        d.initOwner(rootPane.getScene().getWindow());
        d.showAndWait().ifPresent(pattern -> {
            try {
                java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
                java.util.regex.Matcher m = p.matcher(text);
                int count = 0;
                while (m.find()) count++;
                showInfo("Pattern found " + count + " match(es)");
            } catch (Exception e) {
                showError("Invalid regex: " + e.getMessage());
            }
        });
    }

    @FXML
    private void onCodeMetrics() {
        CodeEditor ed = tabManager.getActiveEditor();
        if (ed == null) { statusLabel.setText("Open a file first"); return; }
        String text = ed.getText();
        String[] lines = text.split("\n", -1);
        int nonEmpty = 0, codeLines = 0, commentLines = 0;
        for (String line : lines) {
            String t = line.trim();
            if (t.isEmpty()) continue;
            nonEmpty++;
            if (t.startsWith("//") || t.startsWith("#") || t.startsWith("/*") || t.startsWith("*")) {
                commentLines++;
            } else {
                codeLines++;
            }
        }
        showInfo("Lines: " + lines.length + " | Code: " + codeLines + " | Comments: " + commentLines + " | Blank: " + (lines.length - nonEmpty));
    }

    @FXML
    private void onImportOptimizer() {
        CodeEditor ed = tabManager.getActiveEditor();
        if (ed == null) { statusLabel.setText("Open a file first"); return; }
        com.eagle.tools.ImportOptimizer optimizer = new com.eagle.tools.ImportOptimizer(ed);
        optimizer.showDialog();
    }

    // ── Encoder / Generator Tools ─────────────────────────────────────

    @FXML private void onBase64Encoder() { TextUtilityTools.showBase64Encoder(); }
    @FXML private void onBase64Decoder() { TextUtilityTools.showBase64Decoder(); }
    @FXML private void onUrlEncoder() { TextUtilityTools.showUrlEncoder(); }
    @FXML private void onUrlDecoder() { TextUtilityTools.showUrlDecoder(); }
    @FXML private void onHashGenerator() { TextUtilityTools.showHashGenerator(); }
    @FXML private void onUuidGenerator() { TextUtilityTools.showUuidGenerator(); }
    @FXML private void onPasswordGenerator() { TextUtilityTools.showPasswordGenerator(); }
    @FXML private void onLoremIpsumGenerator() { TextUtilityTools.showLoremIpsumGenerator(); }
    @FXML private void onTimestampConverter() { TextUtilityTools.showTimestampConverter(); }

    // ── Code Analysis Tools ───────────────────────────────────────────

    @FXML
    private void onDuplicateCodeFinder() {
        DuplicateCodeFinder.show(projectRoot);
    }

    @FXML
    private void onDeadCodeFinder() {
        DeadCodeFinder.show(projectRoot);
    }

    // ── JWT & Color Palette ─────────────────────────────────────────

    @FXML private void onJwtDecoder() { TextUtilityTools.showJwtDecoder(); }
    @FXML private void onColorPaletteGenerator() { TextUtilityTools.showColorPaletteGenerator(); }

    // ── Code Formatters ─────────────────────────────────────────────

    @FXML
    private void onFormatXml() {
        String text = getActiveText();
        if (text != null) CodeFormatterTools.formatXml(text);
    }

    @FXML
    private void onFormatCss() {
        String text = getActiveText();
        if (text != null) CodeFormatterTools.formatCss(text);
    }

    @FXML
    private void onFormatSql() {
        String text = getActiveText();
        if (text != null) CodeFormatterTools.formatSql(text);
    }

    @FXML
    private void onFormatYaml() {
        String text = getActiveText();
        if (text != null) CodeFormatterTools.formatYaml(text);
    }

    // ── File Analyzers ──────────────────────────────────────────────

    @FXML
    private void onFileSizeAnalyzer() { FileAnalyzerTools.showFileSizeAnalyzer(projectRoot); }

    @FXML
    private void onProjectSizeAnalyzer() { FileAnalyzerTools.showProjectSizeAnalyzer(projectRoot); }

    @FXML
    private void onLargeFileViewer() { FileAnalyzerTools.showLargeFileViewer(projectRoot); }

    @FXML
    private void onFolderCompare() { FileAnalyzerTools.showFolderCompare(); }

    @FXML
    private void onDuplicateFileFinder() { FileAnalyzerTools.showDuplicateFileFinder(projectRoot); }

    // ── AI Tools ────────────────────────────────────────────────────

    @FXML
    private void onAiLangChange() {
        if (aiLangArabicItem.isSelected()) {
            aiLangEnglishItem.setSelected(false);
            AiTools.responseLanguage = "Arabic";
        } else {
            aiLangEnglishItem.setSelected(true);
            AiTools.responseLanguage = "English";
        }
    }

    @FXML
    private void onAiExplainCode() {
        String text = getActiveText(); String lang = getActiveLang();
        if (text != null) AiTools.explainCode(text, lang);
    }

    @FXML
    private void onAiFixCode() {
        String text = getActiveText(); String lang = getActiveLang();
        if (text != null) AiTools.fixCode(text, lang);
    }

    @FXML
    private void onAiGenerateCode() {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Generate Code");
        dlg.setHeaderText("Describe the code you want to generate:");
        dlg.initOwner(rootPane.getScene().getWindow());
        dlg.showAndWait().ifPresent(desc -> {
            String lang = getActiveLang();
            AiTools.generateCode(desc, lang != null ? lang : "java");
        });
    }

    @FXML
    private void onAiRefactorCode() {
        String text = getActiveText(); String lang = getActiveLang();
        if (text != null) AiTools.refactorCode(text, lang);
    }

    @FXML
    private void onAiGenerateUnitTests() {
        String text = getActiveText(); String lang = getActiveLang();
        if (text != null) AiTools.generateUnitTests(text, lang);
    }

    @FXML
    private void onAiGenerateDocumentation() {
        String text = getActiveText(); String lang = getActiveLang();
        if (text != null) AiTools.generateDocumentation(text, lang);
    }

    @FXML
    private void onAiExplainError() {
        CodeEditor ed = tabManager != null ? tabManager.getActiveEditor() : null;
        String code = ed != null ? ed.getText() : "";
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Explain Error");
        dlg.setHeaderText("Paste the error message:");
        dlg.initOwner(rootPane.getScene().getWindow());
        dlg.showAndWait().ifPresent(err -> AiTools.explainError(err, code));
    }

    @FXML
    private void onAiCodeReview() {
        String text = getActiveText(); String lang = getActiveLang();
        if (text != null) AiTools.aiCodeReview(text, lang);
    }

    @FXML
    private void onAiRenameSymbol() {
        CodeEditor ed = tabManager != null ? tabManager.getActiveEditor() : null;
        if (ed == null) { statusLabel.setText("Open a file first"); return; }
        String code = ed.getText(); String lang = getActiveLang();
        TextInputDialog oldDlg = new TextInputDialog();
        oldDlg.setTitle("Rename Symbol");
        oldDlg.setHeaderText("Enter the symbol name to rename:");
        oldDlg.initOwner(rootPane.getScene().getWindow());
        oldDlg.showAndWait().ifPresent(oldName -> {
            TextInputDialog newDlg = new TextInputDialog();
            newDlg.setTitle("Rename Symbol");
            newDlg.setHeaderText("Enter the new name for '" + oldName + "':");
            newDlg.initOwner(rootPane.getScene().getWindow());
            newDlg.showAndWait().ifPresent(newName -> {
                AiTools.aiRenameSymbol(code, oldName, newName, lang);
            });
        });
    }

    @FXML
    private void onAiCommitMessage() {
        // Get git diff from project root
        if (projectRoot == null) { statusLabel.setText("No project open"); return; }
        new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder("git", "diff", "--cached");
                pb.directory(projectRoot);
                Process p = pb.start();
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new java.io.InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    String line; while ((line = br.readLine()) != null) sb.append(line).append("\n");
                }
                String diffText = sb.toString().trim();
                if (diffText.isEmpty()) {
                    Platform.runLater(() -> statusLabel.setText("No staged changes to commit"));
                    return;
                }
                Platform.runLater(() -> AiTools.aiCommitMessage(diffText));
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Git diff failed: " + e.getMessage()));
            }
        }).start();
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private String getActiveText() {
        CodeEditor ed = tabManager != null ? tabManager.getActiveEditor() : null;
        if (ed == null) { statusLabel.setText("Open a file first"); return null; }
        return ed.getText();
    }

    private String getActiveLang() {
        if (tabManager == null) return "java";
        File f = tabManager.getActiveFile();
        if (f == null) return "java";
        String name = f.getName().toLowerCase();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";
        switch (ext) {
            case "java": return "Java";
            case "js": return "JavaScript";
            case "ts": return "TypeScript";
            case "py": return "Python";
            case "php": return "PHP";
            case "html": case "htm": return "HTML";
            case "css": case "scss": case "less": return "CSS";
            case "xml": case "fxml": return "XML";
            case "json": return "JSON";
            case "sql": return "SQL";
            case "sh": case "bash": return "Bash";
            case "md": return "Markdown";
            default: return ext;
        }
    }

    @FXML
    private void onLanguageManager() { LanguageSupportDialog.showDialog(); }

    @FXML
    private void onInstallPython() { onLanguageInstall("Python"); }

    @FXML
    private void onInstallNodeJs() { onLanguageInstall("Node.js"); }

    @FXML
    private void onInstallJava() { onLanguageInstall("Java"); }

    @FXML
    private void onInstallCpp() { onLanguageInstall("C/C++"); }

    @FXML
    private void onInstallGo() { onLanguageInstall("Go"); }

    @FXML
    private void onInstallRust() { onLanguageInstall("Rust"); }

    @FXML
    private void onInstallDart() { onLanguageInstall("Dart/Flutter"); }

    @FXML
    private void onInstallPhp() { onLanguageInstall("PHP"); }

    @FXML
    private void onInstallRuby() { onLanguageInstall("Ruby"); }

    @FXML
    private void onInstallTypeScript() { onLanguageInstall("TypeScript"); }

    @FXML
    private void onInstallKotlin() { onLanguageInstall("Kotlin"); }

    @FXML
    private void onInstallSwift() { onLanguageInstall("Swift"); }

    @FXML
    private void onInstallSql() { onLanguageInstall("SQL"); }

    @FXML
    private void onInstallGit() { onLanguageInstall("Node.js"); }

    private void onLanguageInstall(String langName) {
        LanguageSupportManager.LangConfig c = LanguageSupportManager.get(langName);
        if (c == null) { showInfo("Unknown language: " + langName); return; }
        LanguageSupportManager.detectRuntime(c);
        LanguageSupportManager.detectLinter(c);
        LanguageSupportManager.detectFormatter(c);
        if (c.runtimeFound) {
            StringBuilder msg = new StringBuilder();
            msg.append("✓ ").append(c.name).append(" runtime detected\n");
            if (c.linterFound) msg.append("✓ Linter: ").append(c.linterPath).append("\n");
            else if (!c.linterCheck.isEmpty()) msg.append("✗ Linter not found. ").append(c.linterInstall).append("\n");
            if (c.formatterFound) msg.append("✓ Formatter: ").append(c.formatterPath).append("\n");
            else if (!c.formatterCheck.isEmpty()) msg.append("✗ Formatter not found. ").append(c.formatterInstall).append("\n");
            showInfo(msg.toString());
        } else {
            if (!c.runtimeUrl.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Runtime Not Found");
                alert.setHeaderText(c.name + " runtime not detected on your system.");
                alert.setContentText("Open download page?");
                ThemeManager.getInstance().applyTheme(alert.getDialogPane().getScene());
                if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    LanguageSupportManager.installLanguage(langName);
                }
            } else {
                showInfo(c.name + ": no runtime check configured. Tools can be configured via Language Manager.");
            }
        }
    }

    @FXML
    private void onCloseFile() {
        Tab tab = editorTabs.getSelectionModel().getSelectedItem();
        if (tab != null) {
            tab.getOnCloseRequest().handle(null);
            if (editorTabs.getTabs().contains(tab)) {
                editorTabs.getTabs().remove(tab);
            }
            if (editorTabs.getTabs().isEmpty()) {
                webPreview.getEngine().loadContent("<html><body></body></html>");
            }
            statusLabel.setText("File closed");
        }
    }

    @FXML
    private void onCloseAllFiles() {
        tabManager.closeAll();
        if (editorTabs.getTabs().isEmpty()) {
            webPreview.getEngine().loadContent("<html><body></body></html>");
        }
        statusLabel.setText("All files closed");
    }

    @FXML
    private void onPrint() {
        CodeEditor ed = tabManager.getActiveEditor();
        if (ed == null) { statusLabel.setText("Open a file to print"); return; }
        try {
            File tmp = File.createTempFile("eagle-print-", ".html");
            String content = ed.getText();
            String html = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\">"
                + "<title>Print - " + (tabManager.getActiveFile() != null ? tabManager.getActiveFile().getName() : "untitled") + "</title>"
                + "<style>body{font-family:Consolas,monospace;font-size:12px;white-space:pre-wrap;padding:20px;background:#fff;color:#000}"
                + "pre{margin:0;border:none;white-space:pre-wrap;word-wrap:break-word}</style></head><body>"
                + "<pre>" + escapeHtml(content) + "</pre></body></html>";
            java.nio.file.Files.write(tmp.toPath(), html.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            java.awt.Desktop.getDesktop().browse(tmp.toURI());
            statusLabel.setText("Print preview opened in browser");
        } catch (Exception ex) {
            showError("Print failed: " + ex.getMessage());
        }
    }

    private String escapeHtml(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    // ---- Edit ----
    @FXML
    private void onDuplicateLine() {
        CodeEditor ed = tabManager.getActiveEditor();
        if (ed == null) return;
        int[] lc = ed.getLineAndColumn();
        String text = ed.getText();
        String[] lines = text.split("\n", -1);
        int idx = Math.min(lc[0] - 1, lines.length - 1);
        String line = lines[idx];
        int pos = ed.getCaretPosition();
        ed.insertText(pos, "\n" + line);
        statusLabel.setText("Line duplicated");
    }

    @FXML
    private void onDeleteLine() {
        CodeEditor ed = tabManager.getActiveEditor();
        if (ed == null) return;
        int[] lc = ed.getLineAndColumn();
        String text = ed.getText();
        String[] lines = text.split("\n", -1);
        if (lc[0] - 1 < lines.length) {
            int startOff = 0;
            for (int i = 0; i < lc[0] - 1; i++) startOff += lines[i].length() + 1;
            int endOff = startOff + lines[lc[0] - 1].length() + 1;
            ed.deleteText(startOff, Math.min(endOff, text.length()));
            statusLabel.setText("Line deleted");
        }
    }

    @FXML
    private void onMoveLineUp() {
        CodeEditor ed = tabManager.getActiveEditor();
        if (ed == null) return;
        int[] lc = ed.getLineAndColumn();
        if (lc[0] <= 1) return;
        String text = ed.getText();
        String[] lines = text.split("\n", -1);
        int idx = Math.min(lc[0] - 1, lines.length - 1);
        String tmp = lines[idx];
        lines[idx] = lines[idx - 1];
        lines[idx - 1] = tmp;
        ed.setText(String.join("\n", lines));
        statusLabel.setText("Line moved up");
    }

    @FXML
    private void onMoveLineDown() {
        CodeEditor ed = tabManager.getActiveEditor();
        if (ed == null) return;
        int[] lc = ed.getLineAndColumn();
        String text = ed.getText();
        String[] lines = text.split("\n", -1);
        int idx = Math.min(lc[0] - 1, lines.length - 1);
        if (idx >= lines.length - 1) return;
        String tmp = lines[idx];
        lines[idx] = lines[idx + 1];
        lines[idx + 1] = tmp;
        ed.setText(String.join("\n", lines));
        statusLabel.setText("Line moved down");
    }

    @FXML
    private void onToggleComment() {
        CodeEditor ed = tabManager.getActiveEditor();
        if (ed == null) return;
        int[] lc = ed.getLineAndColumn();
        String text = ed.getText();
        String[] lines = text.split("\n", -1);
        int idx = Math.min(lc[0] - 1, lines.length - 1);
        String line = lines[idx];
        if (line.trim().startsWith("//")) {
            lines[idx] = line.replaceFirst("\\s*//\\s?", "");
        } else if (line.trim().startsWith("<!--")) {
            lines[idx] = line.replaceFirst("\\s*<!--\\s*", "").replaceFirst("\\s*-->\\s*$", "");
        } else if (line.trim().startsWith("/*")) {
            lines[idx] = line.replaceFirst("\\s*/\\*\\s*", "").replaceFirst("\\s*\\*/\\s*$", "");
        } else {
            lines[idx] = "// " + line;
        }
        ed.setText(String.join("\n", lines));
    }

    @FXML
    private void onIndent() {
        CodeEditor ed = tabManager.getActiveEditor();
        if (ed == null) return;
        int pos = ed.getCaretPosition();
        ed.insertText(pos, "    ");
    }

    @FXML
    private void onOutdent() {
        CodeEditor ed = tabManager.getActiveEditor();
        if (ed == null) return;
        int pos = ed.getCaretPosition();
        if (pos >= 4) {
            String before = ed.getText().substring(Math.max(0, pos - 4), pos);
            if (before.equals("    ")) {
                ed.deleteText(pos - 4, pos);
            }
        }
    }

    @FXML
    private void onFindInFiles() {
        if (projectRoot == null) {
            showError("Open a project first.");
            return;
        }
        TextInputDialog queryDialog = new TextInputDialog();
        queryDialog.setTitle("Find in Files");
        queryDialog.setHeaderText("Search across all files in: " + projectRoot.getName());
        queryDialog.setContentText("Search for:");
        queryDialog.getEditor().setPromptText("e.g. function, class, TODO...");
        queryDialog.initOwner(rootPane.getScene().getWindow());
        ThemeManager.getInstance().applyTheme(queryDialog.getDialogPane().getScene());

        Optional<String> queryResult = queryDialog.showAndWait();
        if (!queryResult.isPresent() || queryResult.get().trim().isEmpty()) return;
        String query = queryResult.get().trim().toLowerCase();

        // Search in background
        statusLabel.setText("Searching for \"" + query + "\"...");
        new Thread(() -> {
            List<File> sourceFiles = new java.util.ArrayList<>();
            collectSourceFiles(projectRoot, sourceFiles);

            // Store results as [file, lineNumber, matchedLine]
            List<String[]> results = new java.util.ArrayList<>();
            for (File f : sourceFiles) {
                try {
                    List<String> lines = java.nio.file.Files.readAllLines(f.toPath(), java.nio.charset.StandardCharsets.UTF_8);
                    String relPath = projectRoot.toURI().relativize(f.toURI()).getPath();
                    for (int i = 0; i < lines.size(); i++) {
                        if (lines.get(i).toLowerCase().contains(query)) {
                            results.add(new String[]{relPath, String.valueOf(i + 1), lines.get(i).trim(), f.getAbsolutePath()});
                        }
                    }
                } catch (Exception ignored) {}
            }

            Platform.runLater(() -> {
                if (results.isEmpty()) {
                    statusLabel.setText("No results for \"" + query + "\"");
                    return;
                }
                showFindResultsDialog(query, results);
                statusLabel.setText("Found " + results.size() + " matches for \"" + query + "\"");
            });
        }).start();
    }

    private void showFindResultsDialog(String query, List<String[]> results) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Find in Files - \"" + query + "\"");
        dialog.setHeaderText(results.size() + " matches found");
        dialog.initOwner(rootPane.getScene().getWindow());
        dialog.initModality(Modality.NONE);

        ListView<String> listView = new ListView<>();
        for (String[] r : results) {
            listView.getItems().add(r[0] + ":" + r[1] + "  " + r[2]);
        }
        listView.setPrefSize(700, 400);
        listView.setCellFactory(lv -> {
            ListCell<String> cell = new ListCell<String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item);
                        setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px; -fx-padding: 2 6;");
                    }
                }
            };
            return cell;
        });
        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                int idx = listView.getSelectionModel().getSelectedIndex();
                if (idx >= 0 && idx < results.size()) {
                    String[] r = results.get(idx);
                    File f = new File(r[3]);
                    openFile(f);
                    CodeEditor ed = tabManager.getActiveEditor();
                    if (ed != null) {
                        int line = Integer.parseInt(r[1]) - 1;
                        ed.moveTo(line, 0);
                        ed.requestFollowCaret();
                    }
                }
            }
        });

        dialog.getDialogPane().setContent(listView);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        ThemeManager.getInstance().applyTheme(dialog.getDialogPane().getScene());
        dialog.show();
    }

    // ---- Selection ----
    @FXML
    private void onExpandSelection() {
        CodeEditor ed = tabManager.getActiveEditor();
        if (ed == null) return;
        ed.selectWord();
        statusLabel.setText("Selection expanded to word");
    }

    @FXML
    private void onShrinkSelection() {
        statusLabel.setText("Shrink selection - feature coming soon");
    }

    @FXML
    private void onSplitLines() {
        CodeEditor ed = tabManager.getActiveEditor();
        if (ed == null) return;
        String sel = ed.getSelectedText();
        if (sel != null && !sel.isEmpty()) {
            ed.replaceSelection(sel.replace(" ", "\n"));
            statusLabel.setText("Lines split");
        }
    }

    @FXML
    private void onJoinLines() {
        CodeEditor ed = tabManager.getActiveEditor();
        if (ed == null) return;
        String sel = ed.getSelectedText();
        if (sel != null && !sel.isEmpty()) {
            ed.replaceSelection(sel.replace("\n", " "));
            statusLabel.setText("Lines joined");
        }
    }

    // ---- View ----
    @FXML
    private void onToggleStatusBar() {
        boolean show = viewStatusBarItem.isSelected();
        statusLabel.setVisible(show);
        statusLabel.setManaged(show);
    }

    @FXML
    private void onToggleWordWrap() {
        boolean wrap = viewWordWrapItem.isSelected();
        for (CodeEditor ed : tabManager.allOpenEditors()) {
            ed.setWrapText(wrap);
        }
        EditorSettings.setWordWrap(wrap);
        statusLabel.setText(wrap ? "Word wrap on" : "Word wrap off");
    }

    @FXML
    private void onToggleFullScreen() {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.setFullScreen(!stage.isFullScreen());
    }

    @FXML
    private void onToggleTabVisibility() {
        boolean vis = mainSplit.getItems().contains(editorContainer);
        toggleSplitItem(1, !vis);
        statusLabel.setText(vis ? "Tabs hidden" : "Tabs visible");
    }

    // ---- Navigate ----
    @FXML
    private void onGoToLine() {
        CodeEditor ed = tabManager.getActiveEditor();
        if (ed == null) { statusLabel.setText("Open a file first"); return; }
        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Go to Line");
        dialog.setHeaderText("Enter line number:");
        ThemeManager.getInstance().applyTheme(dialog.getDialogPane().getScene());
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                int line = Integer.parseInt(result.get().trim());
                ed.moveTo(line - 1, 0);
                statusLabel.setText("Jumped to line " + line);
            } catch (NumberFormatException e) {
                statusLabel.setText("Invalid line number");
            }
        }
    }

    @FXML
    private void onGoToFile() {
        statusLabel.setText("Go to file - feature coming soon");
    }

    @FXML
    private void onGoToDefinition() {
        CodeEditor ed = tabManager.getActiveEditor();
        if (ed == null || projectRoot == null) {
            statusLabel.setText("Open a file and project first");
            return;
        }
        // Try LSP first
        com.eagle.lsp.LspIntegration lsp = ed.getLspIntegration();
        if (lsp != null && lsp.isActive()) {
            int[] lineCol = getCursorLineCol(ed);
            com.eagle.lsp.LspIntegration.DefinitionResult def = lsp.goToDefinition(lineCol[0], lineCol[1]);
            if (def != null && def.uri != null) {
                try {
                    File targetFile = new File(new java.net.URI(def.uri));
                    if (targetFile.exists()) {
                        openFile(targetFile);
                        statusLabel.setText("Go to Definition: " + targetFile.getName());
                        return;
                    }
                } catch (Exception ignored) {}
            }
        }
        // Fallback: search in project files
        String word = getSymbolAtCursor(ed);
        if (word == null || word.isEmpty()) { statusLabel.setText("No symbol selected"); return; }
        final String searchWord = word;
        File activeFile = tabManager.getActiveFile();
        new Thread(() -> {
            java.util.List<File> matches = new java.util.ArrayList<>();
            File[] files = projectRoot.listFiles();
            if (files != null) searchInFiles(files, searchWord, activeFile, matches);
            if (!matches.isEmpty()) {
                File found = matches.get(0);
                Platform.runLater(() -> {
                    openFile(found);
                    statusLabel.setText("Found in: " + found.getName());
                });
            } else {
                Platform.runLater(() -> statusLabel.setText("Definition not found for: " + searchWord));
            }
        }).start();
    }

    @FXML
    private void onFindReferences() {
        CodeEditor ed = tabManager.getActiveEditor();
        if (ed == null) { statusLabel.setText("Open a file first"); return; }
        // Try LSP first
        com.eagle.lsp.LspIntegration lsp = ed.getLspIntegration();
        if (lsp != null && lsp.isActive()) {
            int[] lineCol = getCursorLineCol(ed);
            java.util.List<com.eagle.lsp.LspIntegration.LocationResult> refs = lsp.findReferences(lineCol[0], lineCol[1]);
            if (!refs.isEmpty()) {
                showReferencesResults(refs);
                return;
            }
        }
        // Fallback: search in project files
        String word = getSymbolAtCursor(ed);
        if (word == null || word.isEmpty()) { statusLabel.setText("No symbol selected"); return; }
        final String searchWord = word;
        File activeFile = tabManager.getActiveFile();
        new Thread(() -> {
            java.util.List<File> matches = new java.util.ArrayList<>();
            File[] files = projectRoot.listFiles();
            if (files != null) searchInFiles(files, searchWord, activeFile, matches);
            if (!matches.isEmpty()) {
                Platform.runLater(() -> showSimpleReferenceResults(searchWord, matches));
            } else {
                Platform.runLater(() -> statusLabel.setText("No references found for: " + searchWord));
            }
        }).start();
    }

    @FXML
    private void onRenameSymbol() {
        CodeEditor ed = tabManager.getActiveEditor();
        if (ed == null || projectRoot == null) {
            statusLabel.setText("Open a file and project first");
            return;
        }
        com.eagle.lsp.LspIntegration lsp = ed.getLspIntegration();
        String word = getSymbolAtCursor(ed);
        if (word == null || word.isEmpty()) { statusLabel.setText("No symbol selected"); return; }
        TextInputDialog dialog = new TextInputDialog(word);
        dialog.setTitle("Rename Symbol");
        dialog.setHeaderText("Rename '" + word + "' to:");
        dialog.setContentText("New name:");
        ThemeManager.getInstance().applyTheme(dialog.getDialogPane().getScene());
        java.util.Optional<String> result = dialog.showAndWait();
        if (!result.isPresent() || result.get().trim().isEmpty()) return;
        String newName = result.get().trim();
        if (lsp != null && lsp.isActive()) {
            int[] lineCol = getCursorLineCol(ed);
            com.eagle.lsp.LspIntegration.RenameResult rename = lsp.renameSymbol(lineCol[0], lineCol[1], newName);
            if (rename != null && !rename.changes.isEmpty()) {
                applyLspRenameChanges(rename);
                statusLabel.setText("Renamed " + word + " → " + newName + " (" + rename.changes.size() + " changes)");
                return;
            }
        }
        // Fallback: simple text replace in current file
        String text = ed.getText();
        String replaced = text.replace(word, newName);
        ed.setText(replaced);
        statusLabel.setText("Renamed " + word + " → " + newName + " (current file only)");
    }

    private String getSymbolAtCursor(CodeEditor ed) {
        String word = ed.getSelectedText();
        if (word == null || word.isEmpty()) {
            String text = ed.getText();
            if (text == null || text.isEmpty()) return null;
            int pos = ed.getCaretPosition();
            int start = pos, end = pos;
            while (start > 0 && Character.isLetterOrDigit(text.charAt(start - 1))) start--;
            while (end < text.length() && Character.isLetterOrDigit(text.charAt(end))) end++;
            if (start < end) word = text.substring(start, end);
        }
        return (word == null || word.isEmpty()) ? null : word;
    }

    private int[] getCursorLineCol(CodeEditor ed) {
        int pos = ed.getCaretPosition();
        String text = ed.getText();
        if (text == null || text.isEmpty()) return new int[]{0, 0};
        int line = text.substring(0, Math.min(pos, text.length())).split("\n").length - 1;
        int lastNl = text.lastIndexOf('\n', Math.min(pos, text.length()) - 1);
        int col = pos - (lastNl < 0 ? 0 : lastNl + 1);
        return new int[]{Math.max(0, line), Math.max(0, col)};
    }

    private void showReferencesResults(java.util.List<com.eagle.lsp.LspIntegration.LocationResult> refs) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("References Found");
        alert.setHeaderText(refs.size() + " reference(s) found");
        StringBuilder sb = new StringBuilder();
        for (com.eagle.lsp.LspIntegration.LocationResult r : refs) {
            String path = r.uri;
            try { path = new File(new java.net.URI(r.uri)).getAbsolutePath(); } catch (Exception ignored) {}
            sb.append(path).append(":").append(r.startLine + 1).append(":").append(r.startChar + 1).append("\n");
        }
        TextArea ta = new TextArea(sb.toString());
        ta.setEditable(false);
        ta.setPrefRowCount(12);
        ta.setPrefColumnCount(60);
        alert.getDialogPane().setContent(ta);
        ThemeManager.getInstance().applyTheme(alert.getDialogPane().getScene());
        alert.showAndWait();
    }

    private void showSimpleReferenceResults(String word, java.util.List<File> matches) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("References Found");
        alert.setHeaderText("Found '" + word + "' in " + matches.size() + " file(s)");
        StringBuilder sb = new StringBuilder();
        for (File f : matches) sb.append(f.getAbsolutePath()).append("\n");
        TextArea ta = new TextArea(sb.toString());
        ta.setEditable(false);
        ta.setPrefRowCount(12);
        ta.setPrefColumnCount(60);
        alert.getDialogPane().setContent(ta);
        ThemeManager.getInstance().applyTheme(alert.getDialogPane().getScene());
        alert.showAndWait();
    }

    private void applyLspRenameChanges(com.eagle.lsp.LspIntegration.RenameResult rename) {
        for (com.eagle.lsp.LspIntegration.RenameChange change : rename.changes) {
            try {
                File file = new File(new java.net.URI(change.uri));
                if (file.exists()) {
                    // If file is open in editor, update it
                    CodeEditor editor = tabManager.getEditorForFile(file);
                    if (editor != null) {
                        int startOff = lineColToOffset(editor.getText(), change.startLine, change.startChar);
                        int endOff = lineColToOffset(editor.getText(), change.endLine, change.endChar);
                        editor.replaceText(startOff, endOff, change.newText);
                    } else {
                        // Read file, replace, write back
                        String content = readFileContent(file);
                        int startOff = lineColToOffset(content, change.startLine, change.startChar);
                        int endOff = lineColToOffset(content, change.endLine, change.endChar);
                        String newContent = content.substring(0, startOff) + change.newText
                            + content.substring(endOff);
                        writeFileContent(file, newContent);
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private int lineColToOffset(String text, int line, int col) {
        if (text == null) return 0;
        int cur = 0;
        for (int i = 0; i < line; i++) {
            int nl = text.indexOf('\n', cur);
            if (nl < 0) return text.length();
            cur = nl + 1;
        }
        return Math.min(cur + col, text.length());
    }

    @FXML
    private void onDeploy() {
        if (projectRoot == null) { showError("No project open"); return; }

        ChoiceDialog<String> dialog = new ChoiceDialog<>("GitHub Pages", 
            "GitHub Pages", "Netlify", "Vercel", "Surge.sh", "Firebase Hosting",
            "Cloudflare Pages", "Render", "Railway", "Kinsta", "DigitalOcean App Platform", "Heroku");
        dialog.setTitle("Deploy Project");
        dialog.setHeaderText("Choose deployment target:");
        ThemeManager.getInstance().applyTheme(dialog.getDialogPane().getScene());
        Optional<String> choice = dialog.showAndWait();
        if (!choice.isPresent()) return;

        String target = choice.get();
        switch (target) {
            case "GitHub Pages": deployToGithubPages(); break;
            case "Netlify": deployNpx("netlify-cli", "deploy", "--prod", "--dir=."); break;
            case "Vercel": deployNpx("vercel", "--prod", "--yes"); break;
            case "Surge.sh": deployNpx("surge", ".", projectRoot.getName() + ".surge.sh"); break;
            case "Firebase Hosting": deployNpx("firebase-tools", "deploy", "--only", "hosting"); break;
            case "Cloudflare Pages": deployNpx("wrangler", "pages", "deploy", "."); break;
            case "Render": openBrowser("https://dashboard.render.com/select-repo"); break;
            case "Railway": deployNpx("railway", "up"); break;
            case "Kinsta": openBrowser("https://app.kinsta.com/login"); break;
            case "DigitalOcean App Platform": deployNpx("doctl", "apps", "create", "--spec", ".do/app.yaml"); break;
            case "Heroku": deployNpx("heroku", "create", projectRoot.getName()); break;
        }
    }

    /** Run a deployment via npx (wraps in cmd.exe for PATH resolution) */
    private void deployNpx(String... args) {
        statusLabel.setText("Deploying...");
        new Thread(() -> {
            try {
                String[] cmd = new String[args.length + 3];
                cmd[0] = "cmd.exe";
                cmd[1] = "/c";
                cmd[2] = "npx";
                System.arraycopy(args, 0, cmd, 3, args.length);
                Process p = new ProcessBuilder(cmd)
                        .directory(projectRoot)
                        .inheritIO()
                        .start();
                p.waitFor();
                Platform.runLater(() -> statusLabel.setText("Deploy command sent. Check the terminal output."));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    String msg = e.getMessage();
                    if (msg != null && msg.contains("CreateProcess")) {
                        showError("Command not found. Make sure Node.js/npx is installed and in your PATH.\n\n"
                            + "Try: npm install -g " + args[0]);
                    } else {
                        showError("Deploy failed: " + msg);
                    }
                });
            }
        }).start();
    }

    private void deployToGithubPages() {
        statusLabel.setText("Deploying to GitHub Pages...");
        new Thread(() -> {
            try {
                String[] cmds = {
                    "git init", "git add -A", "git commit -m \"Deploy\"",
                    "git branch -M main",
                    "git remote add origin https://github.com/USER/REPO.git",
                    "git push -u origin main"
                };
                for (String cmd : cmds) {
                    ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", cmd);
                    pb.directory(projectRoot).inheritIO();
                    Process p = pb.start();
                    p.waitFor();
                }
                Platform.runLater(() -> statusLabel.setText("GitHub Pages: pushed to main. Set up Pages in repo settings."));
            } catch (Exception e) {
                Platform.runLater(() -> showError("GitHub Pages deploy failed: " + e.getMessage()));
            }
        }).start();
    }

    /** Open a URL in the default browser */
    private void openBrowser(String url) {
        try {
            new ProcessBuilder("cmd.exe", "/c", "start", url).start();
            statusLabel.setText("Opened browser: " + url);
        } catch (Exception e) {
            showError("Could not open browser: " + e.getMessage());
        }
    }

    private void searchInFiles(File[] files, String word, File skipFile, java.util.List<File> results) {
        for (File f : files) {
            if (results.size() >= 5) return;
            if (f.equals(skipFile)) continue;
            if (f.isDirectory()) {
                File[] children = f.listFiles();
                if (children != null) searchInFiles(children, word, skipFile, results);
            } else if (f.isFile()) {
                String name = f.getName();
                if (name.endsWith(".java") || name.endsWith(".js")
                        || name.endsWith(".ts") || name.endsWith(".html")
                        || name.endsWith(".css") || name.endsWith(".json")
                        || name.endsWith(".xml") || name.endsWith(".php")
                        || name.endsWith(".py") || name.endsWith(".txt")) {
                try {
                    String content = new String(java.nio.file.Files.readAllBytes(f.toPath()), java.nio.charset.StandardCharsets.UTF_8);
                    if (content.contains(word)) {
                        synchronized (results) { results.add(f); }
                    }
                } catch (Exception ignored) {}
            }
        }
    }
}
 
    @FXML
    private void onGoBack() {
        statusLabel.setText("Navigate back - feature coming soon");
    }

    @FXML
    private void onGoForward() {
        statusLabel.setText("Navigate forward - feature coming soon");
    }

    @FXML
    private void onSwitchTabRight() {
        int idx = editorTabs.getSelectionModel().getSelectedIndex();
        if (idx >= 0 && idx < editorTabs.getTabs().size() - 1) {
            editorTabs.getSelectionModel().select(idx + 1);
        }
    }

    @FXML
    private void onSwitchTabLeft() {
        int idx = editorTabs.getSelectionModel().getSelectedIndex();
        if (idx > 0) {
            editorTabs.getSelectionModel().select(idx - 1);
        }
    }

    // ---- Project ----
    @FXML
    private void onOpenTerminal() {
        if (projectRoot == null) {
            showError("Open a project first.");
            return;
        }
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                new ProcessBuilder("cmd.exe", "/c", "start", "cmd.exe").directory(projectRoot).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", "-a", "Terminal", projectRoot.getAbsolutePath()).start();
            } else {
                new ProcessBuilder("x-terminal-emulator").directory(projectRoot).start();
            }
            statusLabel.setText("Terminal opened for " + projectRoot.getName());
        } catch (IOException e) {
            showError("Could not open terminal: " + e.getMessage());
        }
    }

    @FXML
    private void onCopyProjectPath() {
        if (projectRoot == null) return;
        javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
        cc.putString(projectRoot.getAbsolutePath());
        javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
        statusLabel.setText("Project path copied");
    }

    @FXML
    private void onProjectProperties() {
        if (projectRoot == null) return;
        ProjectType type = ProjectsStore.getProjectType(projectRoot);
        long size = folderSize(projectRoot);
        int files = countFiles(projectRoot);
        String sizeStr = size < 1024 ? size + " B" : size < 1048576 ? (size / 1024) + " KB" : (size / 1048576) + " MB";
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Project Properties");
        alert.setHeaderText(projectRoot.getName());
        alert.setContentText(
            "Type: " + type.getDisplayName() + "\n" +
            "Path: " + projectRoot.getAbsolutePath() + "\n" +
            "Size: " + sizeStr + "\n" +
            "Files: " + files
        );
        ThemeManager.getInstance().applyTheme(alert.getDialogPane().getScene());
        alert.showAndWait();
    }

    private long folderSize(File dir) {
        long size = 0;
        File[] ch = dir.listFiles();
        if (ch == null) return 0;
        for (File f : ch) {
            if (f.isDirectory()) size += folderSize(f);
            else size += f.length();
        }
        return size;
    }

    private int countFiles(File dir) {
        int c = 0;
        File[] ch = dir.listFiles();
        if (ch == null) return 0;
        for (File f : ch) {
            if (f.isDirectory()) c += countFiles(f);
            else c++;
        }
        return c;
    }

    // ---- Run ----
    @FXML
    private void onPreviewInBrowser() {
        File active = tabManager.getActiveFile();
        if (active == null) active = new File(projectRoot, "index.html");
        if (active.exists()) {
            try {
                java.awt.Desktop.getDesktop().browse(active.toURI());
            } catch (Exception e) {
                showError("Could not open browser: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onRunProject() {
        if (projectRoot == null) {
            showError("Open a project first to run it.");
            return;
        }

        // Detect project type and determine run command + required tool
        String[] result = detectRunCommand();
        String cmd = result[0];
        final String toolName = result[1];
        final String toolUrl = result[2];
        final String projectType = result[3];

        if (cmd == null) {
            statusLabel.setText("Run: Could not determine how to run this project.");
            showError("No run command could be determined for this project.\n"
                + "Make sure the project has a recognized structure (package.json, pom.xml, etc.).");
            return;
        }

        // For HTML/web projects — open in browser directly
        if ("__preview__".equals(cmd)) {
            File indexHtml = new File(projectRoot, "index.html");
            if (indexHtml.exists()) {
                try {
                    java.awt.Desktop.getDesktop().browse(indexHtml.toURI());
                } catch (Exception e) {
                    // Fallback to WebView preview
                    if (!previewVisible) onTogglePreview();
                    refreshPreview();
                }
                statusLabel.setText("Run: Opened index.html in browser");
            } else {
                if (!previewVisible) onTogglePreview();
                refreshPreview();
                statusLabel.setText("Run: Showing WebView preview");
            }
            return;
        }

        // Check if the required tool is available
        if (toolName != null && !toolName.isEmpty() && !isToolAvailable(toolName)) {
            showToolMissingDialog(toolName, toolUrl, cmd);
            return;
        }

        // Multi-tool check: Tauri needs both node and cargo
        if ("Tauri".equals(projectType)) {
            if (!isToolAvailable("cargo")) {
                showToolMissingDialog("cargo", "https://rustup.rs/",
                    "cargo is required to build the Rust backend of a Tauri app");
                return;
            }
        }

        // Auto-install dependencies if node_modules is missing
        if (projectRoot != null && new File(projectRoot, "package.json").exists()
                && !new File(projectRoot, "node_modules").exists()
                && (cmd.startsWith("npm ") || cmd.startsWith("npx "))) {
            cmd = "npm install && " + cmd;
        }

        // Show terminal panel
        if (terminalPanel != null) {
            boolean vis = mainSplit.getItems().contains(terminalPanel);
            if (!vis) {
                toggleSplitItem(5, true);
                if (terminalBtn != null) {
                    terminalBtn.setText("Terminal (shown)");
                }
            }
            terminalPanel.setWorkingDir(projectRoot);
            terminalPanel.newTerminal();

            String resolvedCmd = resolveCommandPath(cmd, toolName);
            statusLabel.setText("Run: " + resolvedCmd + " (" + projectType + ")");

            String extraPath = null;
            if (toolName != null) {
                String resolvedToolPath = ToolsConfig.getToolPath(toolName);
                if (resolvedToolPath != null) {
                    File p = new File(resolvedToolPath).getParentFile();
                    if (p != null) extraPath = p.getAbsolutePath();
                }
            }
            if (extraPath == null) {
                String npxPath = ToolsConfig.getToolPath("npx");
                if (npxPath != null) {
                    File p = new File(npxPath).getParentFile();
                    if (p != null) extraPath = p.getAbsolutePath();
                }
            }
            final String ep = extraPath;

            // Small delay to ensure terminal tab is ready, then run command
            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(
                javafx.util.Duration.millis(300));
            delay.setOnFinished(e -> terminalPanel.runCommand(resolvedCmd, ep));
            delay.play();

            // Watch terminal for server URL and show dialog when detected
            if (isWebDevServer(projectType) && !"Neutralino.js".equals(projectType)) {
                watchTerminalForServerUrl(projectType);
            }
        }
    }

    /** Watches terminal output for a server URL, then shows a dialog with open/copy buttons. */
    private void watchTerminalForServerUrl(final String projectType) {
        new Thread(() -> {
            int attempts = 0;
            while (attempts < 35) {
                try { Thread.sleep(2000); } catch (InterruptedException e) { return; }
                final String url = extractServerUrl(terminalPanel);
                if (url != null) {
                    javafx.application.Platform.runLater(() -> showServerUrlDialog(url, projectType));
                    return;
                }
                attempts++;
            }
        }).start();
    }

    /** Extracts an http://localhost:PORT URL from the active terminal output, stripping ANSI codes. */
    private String extractServerUrl(TerminalPanel tp) {
        if (tp == null) return null;
        String out = tp.getActiveTerminalOutput();
        if (out == null || out.isEmpty()) return null;
        // Strip all ANSI escape variants
        String clean = out.replaceAll("\u001B\\[[\\d;]*[a-zA-Z]", "");
        clean = clean.replaceAll("\\[\\d+m", "").replaceAll("\\[\\d;\\d+m", "").replaceAll("\\[\\d;\\d;\\d+m", "");
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("http://localhost:\\d+").matcher(clean);
        if (m.find()) return m.group();
        // Fallback: extract port from lines containing "Local"
        java.util.regex.Matcher pm = java.util.regex.Pattern.compile("Local.*?(\\d{4,5})").matcher(clean);
        if (pm.find()) return "http://localhost:" + pm.group(1);
        return null;
    }

    /** Shows a small dialog with the server URL, Open in Browser, and Copy Link buttons. */
    private void showServerUrlDialog(String url, String projectType) {
        javafx.scene.control.Dialog<String> dlg = new javafx.scene.control.Dialog<>();
        dlg.setTitle("Server Ready — " + projectType);
        dlg.setHeaderText("Development server is running!");
        dlg.initOwner(rootPane.getScene().getWindow());
        dlg.initModality(javafx.stage.Modality.NONE);

        javafx.scene.control.TextField urlField = new javafx.scene.control.TextField(url);
        urlField.setEditable(false);
        urlField.setStyle("-fx-font-family: monospace; -fx-font-size: 14px; -fx-text-fill: -accent;");

        javafx.scene.control.Button openBtn = new javafx.scene.control.Button("Open in Browser");
        openBtn.setStyle("-fx-background-color: -accent; -fx-text-fill: white; -fx-font-weight: bold;");
        openBtn.setOnAction(e -> {
            try { java.awt.Desktop.getDesktop().browse(java.net.URI.create(url)); }
            catch (Exception ex) { showError("Could not open browser: " + ex.getMessage()); }
        });

        javafx.scene.control.Button copyBtn = new javafx.scene.control.Button("Copy Link");
        copyBtn.setOnAction(e -> {
            javafx.scene.input.Clipboard cb = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(url);
            cb.setContent(content);
            statusLabel.setText("Copied: " + url);
        });

        javafx.scene.control.Button closeBtn = new javafx.scene.control.Button("Close");
        closeBtn.setOnAction(e -> dlg.close());

        javafx.scene.layout.HBox btnRow = new javafx.scene.layout.HBox(10, openBtn, copyBtn, closeBtn);
        btnRow.setAlignment(javafx.geometry.Pos.CENTER);

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(12, urlField, btnRow);
        content.setPadding(new javafx.geometry.Insets(14));

        javafx.scene.control.ButtonType dummy = new javafx.scene.control.ButtonType("Done", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        dlg.getDialogPane().getButtonTypes().add(dummy);
        dlg.getDialogPane().setContent(content);
        dlg.getDialogPane().setPrefWidth(480);

        javafx.scene.Node dummyBtn = dlg.getDialogPane().lookupButton(dummy);
        if (dummyBtn != null) dummyBtn.setVisible(false);

        com.eagle.util.ThemeManager.getInstance().applyTheme(dlg.getDialogPane().getScene());
        dlg.show();

        // Auto-close after 60 seconds
        javafx.animation.PauseTransition autoClose = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(60));
        autoClose.setOnFinished(e -> { if (dlg.isShowing()) dlg.close(); });
        autoClose.play();
    }

    private boolean isWebDevServer(String type) {
        if (type == null) return false;
        return type.equals("Vite") || type.equals("Vue") || type.equals("React")
            || type.equals("Next.js") || type.equals("Nuxt") || type.equals("Angular")
            || type.equals("SvelteKit") || type.equals("Solid.js") || type.equals("Astro")
            || type.equals("Quasar") || type.equals("Static") || type.equals("HTML")
            || type.equals("Neutralino.js") || type.equals("NW.js");
    }

    private String getDevServerUrl(String type) {
        if (type == null) return "http://localhost:5173";
        switch (type) {
            case "Vite": case "Vue": case "React": case "Solid.js": case "Quasar":
            case "SvelteKit": case "Astro":
                return "http://localhost:5173";
            case "Next.js": case "Nuxt": case "Static":
                return "http://localhost:3000";
            case "Angular":
                return "http://localhost:4200";
            case "Neutralino.js":
                return "http://localhost:0"; // Neutralino picks a random port
            case "NW.js":
                return "http://localhost:8080";
            case "HTML":
                return new File(projectRoot, "index.html").toURI().toString();
            default:
                return "http://localhost:5173";
        }
    }

    // ================================================================
    //   CONVERT PROJECT
    // ================================================================

    // Detected technology labels for the current project (e.g., "Rust", "React", "Tauri")
    private final java.util.List<String> detectedTechnologies = new java.util.ArrayList<>();

    private void detectProjectTechnologies() {
        detectedTechnologies.clear();
        if (projectRoot == null) return;
        if (statusLabelSecondary != null) statusLabelSecondary.setText("");
        String[] run = detectRunCommand();
        String projectType = run[3];
        if (projectType != null) {
            detectedTechnologies.add(projectType);
        }
        // Scan for specific files that indicate technologies
        File root = projectRoot;
        if (new File(root, "Cargo.toml").exists()) { detectedTechnologies.add("Rust"); }
        if (new File(root, "src-tauri").isDirectory()) { detectedTechnologies.add("Tauri"); }
        if (new File(root, "neutralino.config.json").exists()) { detectedTechnologies.add("Neutralino.js"); }
        if (new File(root, "go.mod").exists()) { detectedTechnologies.add("Go"); }
        if (new File(root, "pom.xml").exists()) { detectedTechnologies.add("Maven"); }
        if (new File(root, "build.gradle").exists()) { detectedTechnologies.add("Gradle"); }
        if (new File(root, "angular.json").exists()) { detectedTechnologies.add("Angular"); }
        if (new File(root, "next.config.js").exists() || new File(root, "next.config.mjs").exists()) { detectedTechnologies.add("Next.js"); }
        if (new File(root, "nuxt.config.ts").exists() || new File(root, "nuxt.config.js").exists()) { detectedTechnologies.add("Nuxt"); }
        if (new File(root, "svelte.config.js").exists()) { detectedTechnologies.add("SvelteKit"); }
        if (new File(root, "vite.config.ts").exists() || new File(root, "vite.config.js").exists()) { detectedTechnologies.add("Vite"); }
        if (new File(root, "pubspec.yaml").exists()) { detectedTechnologies.add("Flutter"); }
        if (new File(root, "ionic.config.json").exists()) { detectedTechnologies.add("Ionic"); }
        if (new File(root, "cordova").exists() || new File(root, "config.xml").exists()) { detectedTechnologies.add("Cordova"); }
        if (new File(root, "capacitor.config.json").exists()) { detectedTechnologies.add("Capacitor"); }
        if (new File(root, "astro.config.mjs").exists()) { detectedTechnologies.add("Astro"); }

        File pkgJson = new File(root, "package.json");
        if (pkgJson.exists()) {
            String pc = readFileFirstLines(pkgJson, 300);
            if (pc != null) {
                if (pc.contains("\"electron\"")) detectedTechnologies.add("Electron");
                if (pc.contains("\"@tauri-apps/cli\"")) detectedTechnologies.add("Tauri");
                if (pc.contains("\"@neutralinojs/neu\"")) detectedTechnologies.add("Neutralino.js");
                if (pc.contains("\"nw\"") || pc.contains("\"nwjs\"")) detectedTechnologies.add("NW.js");
                if (pc.contains("\"react\"") || pc.contains("\"react-dom\"")) detectedTechnologies.add("React");
                if (pc.contains("\"vue\"") || pc.contains("\"vue3\"")) detectedTechnologies.add("Vue");
                if (pc.contains("\"svelte\"")) detectedTechnologies.add("Svelte");
                if (pc.contains("\"@sveltejs/kit\"")) detectedTechnologies.add("SvelteKit");
                if (pc.contains("\"@angular/core\"")) detectedTechnologies.add("Angular");
                if (pc.contains("\"next\"")) detectedTechnologies.add("Next.js");
                if (pc.contains("\"nuxt\"")) detectedTechnologies.add("Nuxt");
                if (pc.contains("\"@nestjs/core\"")) detectedTechnologies.add("NestJS");
                if (pc.contains("\"mongoose\"")) detectedTechnologies.add("MongoDB");
                if (pc.contains("\"express\"")) detectedTechnologies.add("Express");
                if (pc.contains("\"cordova\"")) detectedTechnologies.add("Cordova");
                if (pc.contains("\"@capacitor/core\"")) detectedTechnologies.add("Capacitor");
                if (pc.contains("\"react-native\"")) detectedTechnologies.add("React Native");
                if (pc.contains("\"@nativescript/core\"")) detectedTechnologies.add("NativeScript");
                if (pc.contains("\"astro\"")) detectedTechnologies.add("Astro");
                if (pc.contains("\"ionic\"")) detectedTechnologies.add("Ionic");
                if (pc.contains("\"gatsby\"")) detectedTechnologies.add("Gatsby");
                if (pc.contains("\"quasar\"")) detectedTechnologies.add("Quasar");
                if (pc.contains("\"docusaurus\"")) detectedTechnologies.add("Docusaurus");
                if (pc.contains("\"typescript\"")) detectedTechnologies.add("TypeScript");
            }
        }
        // Remove duplicates
        java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>(detectedTechnologies);
        detectedTechnologies.clear();
        detectedTechnologies.addAll(set);
        if (!detectedTechnologies.isEmpty() && statusLabelSecondary != null) {
            statusLabelSecondary.setText("Tech: " + String.join(", ", detectedTechnologies));
        }
    }

    @FXML
    private String[] detectRunCommand() {
        File root = projectRoot;
        if (root == null) return new String[]{null, null, null, null};

        // ── Electron Desktop App ──
        File mainJs = new File(root, "main.js");
        File pkgJson = new File(root, "package.json");
        if (pkgJson.exists()) {
            String pkgContent = readFileFirstLines(pkgJson, 200);
            if (pkgContent != null && pkgContent.contains("\"electron\"")) {
                return new String[]{"npx electron .", "node", "https://nodejs.org", "Electron"};
            }
            // ── Cordova Android App ──
            if (pkgContent != null && pkgContent.contains("\"cordova\"")) {
                return new String[]{"npx cordova run android", "node", "https://nodejs.org", "Cordova"};
            }
        }

        // ── Ionic ──
        if (new File(root, "ionic.config.json").exists()) {
            return new String[]{"npx ionic serve", "node", "https://nodejs.org", "Ionic"};
        }

        // ── Neutralino.js ──
        if (new File(root, "neutralino.config.json").exists()) {
            return new String[]{"npm start", "node", "https://nodejs.org", "Neutralino.js"};
        }

        // ── NW.js ──
        if (mainJs.exists() && pkgJson.exists()) {
            String pkgContent2 = readFileFirstLines(pkgJson, 200);
            if (pkgContent2 != null && (pkgContent2.contains("\"nw\"") || pkgContent2.contains("\"nwjs\""))) {
                return new String[]{"npm start", "node", "https://nodejs.org", "NW.js"};
            }
        }

        // ── Tauri ──
        if (new File(root, "src-tauri").isDirectory()) {
            return new String[]{"npx tauri dev", "node", "https://nodejs.org", "Tauri"};
        }

        // ── Flutter Web ──
        if (new File(root, "pubspec.yaml").exists()) {
            return new String[]{"flutter run -d chrome", "flutter", "https://flutter.dev", "Flutter"};
        }

        // ── Angular ──
        if (new File(root, "angular.json").exists()) {
            return new String[]{"npx ng serve", "node", "https://nodejs.org", "Angular"};
        }

        // ── Next.js ──
        if (new File(root, "next.config.js").exists() || new File(root, "next.config.mjs").exists()) {
            return new String[]{"npx next dev", "node", "https://nodejs.org", "Next.js"};
        }

        // ── SvelteKit ──
        if (new File(root, "svelte.config.js").exists()) {
            return new String[]{"npx vite dev", "node", "https://nodejs.org", "SvelteKit"};
        }

        // ── Nuxt ──
        if (new File(root, "nuxt.config.ts").exists() || new File(root, "nuxt.config.js").exists()) {
            return new String[]{"npx nuxt dev", "node", "https://nodejs.org", "Nuxt"};
        }

        // ── Remix ──
        if (new File(root, "remix.config.js").exists()) {
            return new String[]{"npx remix dev", "node", "https://nodejs.org", "Remix"};
        }

        // ── Astro ──
        if (new File(root, "astro.config.mjs").exists()) {
            return new String[]{"npx astro dev", "node", "https://nodejs.org", "Astro"};
        }

        // ── Gatsby ──
        if (new File(root, "gatsby-config.js").exists()) {
            return new String[]{"npx gatsby develop", "node", "https://nodejs.org", "Gatsby"};
        }

        // ── Docusaurus ──
        if (new File(root, "docusaurus.config.js").exists()) {
            return new String[]{"npx docusaurus start", "node", "https://nodejs.org", "Docusaurus"};
        }

        // ── NestJS ──
        if (new File(root, "nest-cli.json").exists() || new File(root, "nest.config.json").exists()) {
            return new String[]{"npx nest start", "node", "https://nodejs.org", "NestJS"};
        }
        if (pkgJson.exists()) {
            String pkgContent = readFileFirstLines(pkgJson, 200);
            if (pkgContent != null && pkgContent.contains("\"@nestjs/core\"")) {
                return new String[]{"npx nest start", "node", "https://nodejs.org", "NestJS"};
            }
        }

        // ── AdonisJS ──
        if (pkgJson.exists()) {
            String pkgContent = readFileFirstLines(pkgJson, 200);
            if (pkgContent != null && pkgContent.contains("\"@adonisjs/core\"")) {
                return new String[]{"npm start", "node", "https://nodejs.org", "AdonisJS"};
            }
        }

        // ── Quasar Vue App ──
        if (new File(root, "quasar.config.js").exists() || new File(root, "quasar.conf.js").exists()) {
            return new String[]{"npx quasar dev", "node", "https://nodejs.org", "Quasar"};
        }

        // ── Framework7 ──
        if (pkgJson.exists()) {
            String pkgContent = readFileFirstLines(pkgJson, 200);
            if (pkgContent != null && (pkgContent.contains("\"framework7\"") || pkgContent.contains("\"framework7-react\""))) {
                return new String[]{"npx vite", "node", "https://nodejs.org", "Framework7"};
            }
        }

        // ── NativeScript ──
        if (new File(root, "nativescript.config.ts").exists() || new File(root, "nativescript.config.js").exists()) {
            return new String[]{"ns run android", "node", "https://nodejs.org", "NativeScript"};
        }
        if (pkgJson.exists()) {
            String pkgContent = readFileFirstLines(pkgJson, 200);
            if (pkgContent != null && pkgContent.contains("\"@nativescript/core\"")) {
                return new String[]{"ns run android", "node", "https://nodejs.org", "NativeScript"};
            }
        }

        // ── Onsen UI ──
        if (pkgJson.exists()) {
            String pkgContent = readFileFirstLines(pkgJson, 200);
            if (pkgContent != null && (pkgContent.contains("\"onsenui\"") || pkgContent.contains("\"react-onsenui\""))) {
                return new String[]{"npx vite", "node", "https://nodejs.org", "Onsen UI"};
            }
        }

        // ── Stencil.js ──
        if (new File(root, "stencil.config.ts").exists()) {
            return new String[]{"npx stencil build --dev --watch --serve", "node", "https://nodejs.org", "Stencil"};
        }

        // ── Deno ──
        if (new File(root, "deno.json").exists() || new File(root, "deno.jsonc").exists()) {
            return new String[]{"deno run --allow-net --allow-read main.ts", "deno", "https://deno.land", "Deno"};
        }

        // ── Bun ──
        if (new File(root, "bun.lockb").exists() || new File(root, "bunfig.toml").exists()) {
            return new String[]{"bun run index.ts", "bun", "https://bun.sh", "Bun"};
        }

        // ── PWA (has manifest.json + sw.js) ──
        if (new File(root, "manifest.json").exists() && new File(root, "sw.js").exists()) {
            return new String[]{"__preview__", null, null, "PWA"};
        }

        // ── React Native Web ──
        if (mainJs.exists() && mainJs.getName().equals("main.js")
            && new File(root, "webpack.config.js").exists()) {
            return new String[]{"npx webpack serve", "node", "https://nodejs.org", "React Native Web"};
        }

        // ── Solid.js (vite-plugin-solid in devDependencies) ──
        if (new File(root, "vite.config.js").exists() || new File(root, "vite.config.ts").exists()) {
            if (pkgJson.exists()) {
                String pc = readFileFirstLines(pkgJson, 200);
                if (pc != null && pc.contains("\"vite-plugin-solid\"")) {
                    return new String[]{"npx vite", "node", "https://nodejs.org", "Solid.js"};
                }
            }
        }

        // ── Vite-based projects ──
        if (new File(root, "vite.config.js").exists() || new File(root, "vite.config.ts").exists()) {
            return new String[]{"npx vite", "node", "https://nodejs.org", "Vite"};
        }

        // ── Detect Node.js project (generic) ──
        if (pkgJson.exists()) {
            String pkgContent = readFileFirstLines(pkgJson, 200);

            // Express + MongoDB (mongoose dependency)
            if (pkgContent != null && pkgContent.contains("\"mongoose\"")) {
                return new String[]{"npm start", "node", "https://nodejs.org", "Express+MongoDB"};
            }

            // HTMX Express - detected by custom script name
            if (pkgContent != null && pkgContent.contains("\"nodemon\"")) {
                File svrJs = new File(root, "server.js");
                if (svrJs.exists() && pkgContent.contains("\"express\"")) {
                    return new String[]{"npm start", "node", "https://nodejs.org", "Express"};
                }
            }

            String startScript = hasNpmStartScript(pkgJson) ? "npm start" : null;
            if (startScript != null) {
                return new String[]{startScript, "node", "https://nodejs.org", "Node.js"};
            }
            File svrJs = new File(root, "server.js");
            if (svrJs.exists()) return new String[]{"node server.js", "node", "https://nodejs.org", "Node.js"};
            File idxJs = new File(root, "index.js");
            if (idxJs.exists()) return new String[]{"node index.js", "node", "https://nodejs.org", "Node.js"};
            // No start script, no server file — treat as static if index.html exists
            File idxHtml2 = new File(root, "index.html");
            if (idxHtml2.exists()) return new String[]{"npx serve .", "node", "https://nodejs.org", "Static"};
            return new String[]{"__preview__", null, null, "HTML"};
        }

        // Detect Python/Flask/Django/FastAPI
        File managePy = new File(root, "manage.py");
        if (managePy.exists()) return new String[]{"python manage.py runserver", "python", "https://www.python.org/downloads/", "Python/Django"};
        File appPy = new File(root, "app.py");
        if (appPy.exists()) return new String[]{"python app.py", "python", "https://www.python.org/downloads/", "Python/Flask"};
        File mainPy = new File(root, "main.py");
        if (mainPy.exists()) {
            String mainContent = readFileFirstLines(mainPy, 50);
            if (mainContent != null && mainContent.contains("fastapi")) {
                return new String[]{"uvicorn main:app --reload", "python", "https://www.python.org/downloads/", "Python/FastAPI"};
            }
            return new String[]{"python main.py", "python", "https://www.python.org/downloads/", "Python"};
        }

        // Detect Java/Maven
        File pomXml = new File(root, "pom.xml");
        if (pomXml.exists()) {
            String pomContent = readFileFirstLines(pomXml, 50);
            if (pomContent != null && pomContent.contains("spring-boot")) {
                return new String[]{"mvn spring-boot:run", "mvn", "https://maven.apache.org/download.cgi", "Java/Spring Boot"};
            }
            return new String[]{"mvn compile exec:java", "mvn", "https://maven.apache.org/download.cgi", "Java/Maven"};
        }

        // Detect Java/Gradle
        File gradleFile = new File(root, "build.gradle");
        if (gradleFile.exists()) {
            String gradleContent = readFileFirstLines(gradleFile, 50);
            if (gradleContent != null && gradleContent.contains("spring-boot")) {
                return new String[]{"gradle bootRun", "gradle", "https://gradle.org/install/", "Java/Gradle"};
            }
            return new String[]{"gradle run", "gradle", "https://gradle.org/install/", "Java/Gradle"};
        }

        // Detect Rust
        File cargoToml = new File(root, "Cargo.toml");
        if (cargoToml.exists()) return new String[]{"cargo run", "cargo", "https://rustup.rs/", "Rust"};

        // Detect Go
        File goMod = new File(root, "go.mod");
        if (goMod.exists()) return new String[]{"go run .", "go", "https://go.dev/dl/", "Go"};

        // Detect Ruby on Rails
        File gemfile = new File(root, "Gemfile");
        if (gemfile.exists()) {
            String gemContent = readFileFirstLines(gemfile, 50);
            if (gemContent != null && gemContent.contains("rails")) {
                return new String[]{"rails server", "rails", "https://rubyonrails.org/", "Ruby/Rails"};
            }
        }

        // Detect PHP / Laravel / Symfony
        if (new File(root, "artisan").exists()) {
            return new String[]{"php artisan serve", "php", "https://windows.php.net/download/", "PHP/Laravel"};
        }
        File indexPhp = new File(root, "index.php");
        if (indexPhp.exists()) return new String[]{"php -S localhost:8080", "php", "https://windows.php.net/download/", "PHP"};

        // Detect C# / .NET / ASP.NET Core
        File[] csprojFiles = root.listFiles((d, n) -> n.endsWith(".csproj"));
        if (csprojFiles != null && csprojFiles.length > 0) {
            String csprojContent = readFileFirstLines(csprojFiles[0], 50);
            if (csprojContent != null && csprojContent.contains("Microsoft.NET.Sdk.Web")) {
                return new String[]{"dotnet run", "dotnet", "https://dotnet.microsoft.com/download", "ASP.NET Core"};
            }
            return new String[]{"dotnet run", "dotnet", "https://dotnet.microsoft.com/download", "C#/.NET"};
        }

        // ── Static Site Generators ──
        if (new File(root, "hugo.toml").exists() || new File(root, "hugo.yaml").exists() || new File(root, "hugo.json").exists()) {
            return new String[]{"hugo server", "hugo", "https://gohugo.io/", "Hugo"};
        }
        if (new File(root, ".eleventy.js").exists()) {
            return new String[]{"npx @11ty/eleventy --serve", "node", "https://nodejs.org", "Eleventy"};
        }
        if (new File(root, "_config.yml").exists()) {
            String configContent = readFileFirstLines(new File(root, "_config.yml"), 30);
            if (configContent != null && configContent.contains("jekyll")) {
                return new String[]{"jekyll serve", "jekyll", "https://jekyllrb.com/", "Jekyll"};
            }
        }

        // ── Nx Monorepo ──
        if (new File(root, "nx.json").exists()) {
            return new String[]{"npx nx serve", "node", "https://nodejs.org", "Nx"};
        }

        // ── Turborepo ──
        if (new File(root, "turbo.json").exists()) {
            return new String[]{"npx turbo dev", "node", "https://nodejs.org", "Turborepo"};
        }

        // ── VuePress ──
        if (new File(root, ".vuepress/config.js").exists() || new File(root, ".vuepress/config.ts").exists()) {
            return new String[]{"npx vuepress dev", "node", "https://nodejs.org", "VuePress"};
        }

        // ── VitePress ──
        if (new File(root, ".vitepress/config.js").exists() || new File(root, ".vitepress/config.ts").exists()) {
            return new String[]{"npx vitepress dev", "node", "https://nodejs.org", "VitePress"};
        }

        // ── Meteor ──
        if (new File(root, ".meteor").isDirectory()) {
            return new String[]{"meteor run", "meteor", "https://www.meteor.com/", "Meteor"};
        }

        // ── RedwoodJS ──
        if (new File(root, "redwood.toml").exists()) {
            return new String[]{"npx redwood dev", "node", "https://nodejs.org", "RedwoodJS"};
        }

        // ── Blitz.js ──
        if (new File(root, "blitz.config.js").exists() || new File(root, "blitz.config.ts").exists()) {
            return new String[]{"npx blitz dev", "node", "https://nodejs.org", "Blitz.js"};
        }

        // ── Strapi CMS ──
        if (pkgJson.exists()) {
            String pkgContent = readFileFirstLines(pkgJson, 200);
            if (pkgContent != null && pkgContent.contains("\"strapi\"")) {
                return new String[]{"npm run develop", "node", "https://nodejs.org", "Strapi"};
            }
        }

        // ── KeystoneJS ──
        if (new File(root, "keystone.config.js").exists() || new File(root, "keystone.config.ts").exists()) {
            return new String[]{"npx keystone dev", "node", "https://nodejs.org", "KeystoneJS"};
        }

        // Detect HTML (use browser preview)
        File indexHtml = new File(root, "index.html");
        if (indexHtml.exists()) return new String[]{"__preview__", null, null, "HTML"};

        // Try active file
        File active = tabManager != null ? tabManager.getActiveFile() : null;
        if (active != null) {
            String name = active.getName().toLowerCase();
            if (name.endsWith(".js")) {
                return new String[]{"node \"" + active.getAbsolutePath() + "\"", "node", "https://nodejs.org", "Node.js (file)"};
            }
            if (name.endsWith(".py")) {
                return new String[]{"python \"" + active.getAbsolutePath() + "\"", "python", "https://www.python.org/downloads/", "Python (file)"};
            }
            if (name.endsWith(".html") || name.endsWith(".htm")) return new String[]{"__preview__", null, null, "HTML"};
        }

        return new String[]{null, null, null, null};
    }

    private String resolveCommandPath(String cmd, String toolName) {
        if (cmd.contains(" && ")) {
            String[] parts = cmd.split(" && ", 2);
            return resolveCommandPath(parts[0], toolName) + " && " + resolveCommandPath(parts[1], toolName);
        }
        // Resolve npx via its own configured path
        if (cmd.startsWith("npx ") && (toolName == null || toolName.equals("node"))) {
            String npxPath = ToolsConfig.getToolPath("npx");
            if (npxPath != null) return "\"" + npxPath + "\"" + cmd.substring(3);
        }
        if (toolName == null || toolName.isEmpty()) return cmd;
        String configuredPath = ToolsConfig.getToolPath(toolName);
        if (configuredPath != null) {
            if (cmd.startsWith(toolName + " ")) {
                return "\"" + configuredPath + "\"" + cmd.substring(toolName.length());
            }
            // Try finding sibling executables in the same dir (npm, npx beside node, etc.)
            File toolDir = new File(configuredPath).getParentFile();
            if (toolDir != null && toolDir.isDirectory()) {
                String firstWord = cmd.split(" ")[0];
                if (!firstWord.equals(toolName)) {
                    String[] exts = {".exe", ".cmd", ".bat", ""};
                    for (String ext : exts) {
                        File sibling = new File(toolDir, firstWord + ext);
                        if (sibling.exists() && sibling.isFile()) {
                            return "\"" + sibling.getAbsolutePath() + "\"" + cmd.substring(firstWord.length());
                        }
                    }
                }
            }
        }
        if (configuredPath == null && toolName.equals("node")) {
            String npxPath = ToolsConfig.getToolPath("npx");
            if (npxPath != null) {
                File nodeFile = new File(new File(npxPath).getParentFile(), "node.exe");
                if (nodeFile.exists()) {
                    if (cmd.startsWith("npm ")) {
                        File npmFile = new File(nodeFile.getParentFile(), "npm.cmd");
                        if (npmFile.exists()) return "\"" + npmFile.getAbsolutePath() + "\"" + cmd.substring(3);
                    }
                }
            }
        }
        return cmd;
    }

    private boolean isToolAvailable(String tool) {
        try {
            String configuredPath = ToolsConfig.getToolPath(tool);
            if (configuredPath != null) {
                File f = new File(configuredPath);
                return f.exists() && f.isFile();
            }
            if (tool.equals("node")) {
                String npxPath = ToolsConfig.getToolPath("npx");
                if (npxPath != null) {
                    File npxFile = new File(npxPath);
                    File nodeFile = new File(npxFile.getParentFile(), "node.exe");
                    if (nodeFile.exists() && nodeFile.isFile()) return true;
                }
            }
            String checkCmd = tool;
            if (tool.equals("npm")) checkCmd = "npm --version";
            else if (tool.equals("node")) checkCmd = "node --version";
            else if (tool.equals("python")) checkCmd = "python --version";
            else if (tool.equals("mvn")) checkCmd = "mvn --version";
            else if (tool.equals("gradle")) checkCmd = "gradle --version";
            else if (tool.equals("cargo")) checkCmd = "cargo --version";
            else if (tool.equals("go")) checkCmd = "go version";
            else if (tool.equals("php")) checkCmd = "php --version";
            else if (tool.equals("dotnet")) checkCmd = "dotnet --version";
            else if (tool.equals("deno")) checkCmd = "deno --version";
            else if (tool.equals("bun")) checkCmd = "bun --version";
            else if (tool.equals("hugo")) checkCmd = "hugo version";
            else if (tool.equals("jekyll")) checkCmd = "jekyll --version";
            else if (tool.equals("rails")) checkCmd = "rails --version";
            else if (tool.equals("ns")) checkCmd = "ns --version";
            else if (tool.equals("quasar")) checkCmd = "quasar --version";
            else if (tool.equals("flutter")) checkCmd = "flutter --version";
            else checkCmd = tool + " --version";

            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", checkCmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            int exitCode = p.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void showToolMissingDialog(String toolName, String downloadUrl, String command) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
        alert.setTitle("Missing Tool: " + toolName);
        alert.setHeaderText(toolName + " is not installed or not in PATH");
        alert.setContentText("To run this project, you need " + toolName + " installed.\n\n"
            + "Command: " + command + "\n\n"
            + "You can set a custom path for " + toolName + " in Settings > Environment.");
        ButtonType downloadBtn = new ButtonType("Download " + toolName, ButtonBar.ButtonData.OK_DONE);
        ButtonType settingsBtn = new ButtonType("Configure Path...", ButtonBar.ButtonData.OTHER);
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(downloadBtn, settingsBtn, cancelBtn);
        alert.initOwner(rootPane.getScene().getWindow());
        ThemeManager.getInstance().applyTheme(alert.getDialogPane().getScene());

        alert.showAndWait().ifPresent(btn -> {
            if (btn == downloadBtn && downloadUrl != null && !downloadUrl.isEmpty()) {
                try {
                    java.awt.Desktop.getDesktop().browse(java.net.URI.create(downloadUrl));
                } catch (Exception ex) {
                    showError("Could not open browser. Download from:\n" + downloadUrl);
                }
            } else if (btn == settingsBtn) {
                openSettings();
            }
        });
    }

    private String readFileFirstLines(File file, int maxLines) {
        try {
            java.util.List<String> lines = java.nio.file.Files.readAllLines(file.toPath(),
                java.nio.charset.StandardCharsets.UTF_8);
            StringBuilder sb = new StringBuilder();
            int limit = Math.min(maxLines, lines.size());
            for (int i = 0; i < limit; i++) {
                sb.append(lines.get(i)).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean hasNpmStartScript(File packageJson) {
        try {
            String content = new String(java.nio.file.Files.readAllBytes(packageJson.toPath()),
                java.nio.charset.StandardCharsets.UTF_8);
            return content.contains("\"start\"");
        } catch (Exception e) {
            return false;
        }
    }

    @FXML
    private void onRunJs() {
        File active = tabManager.getActiveFile();
        if (active == null) {
            statusLabel.setText("Open a file first");
            return;
        }
        String ext = active.getName().substring(active.getName().lastIndexOf('.') + 1).toLowerCase();
        if (!"js".equals(ext)) {
            statusLabel.setText("Open a .js file to run");
            return;
        }

        CodeEditor editor = tabManager.getActiveEditor();
        String code = editor != null ? editor.getText() : "";
        if (code.isEmpty()) {
            statusLabel.setText("File is empty");
            return;
        }

        // Try Nashorn first (Java 8 built-in)
        try {
            javax.script.ScriptEngineManager manager = new javax.script.ScriptEngineManager();
            javax.script.ScriptEngine engine = manager.getEngineByName("nashorn");
            if (engine != null) {
                if (debuggerPanel != null) {
                    debuggerPanel.appendLine("[info] Running with Nashorn: " + active.getName());
                    java.io.StringWriter writer = new java.io.StringWriter();
                    java.io.StringWriter errorWriter = new java.io.StringWriter();
                    engine.getContext().setWriter(writer);
                    engine.getContext().setErrorWriter(errorWriter);
                    engine.eval(code);
                    String out = writer.toString();
                    if (!out.isEmpty()) debuggerPanel.appendLine(out);
                    String err = errorWriter.toString();
                    if (!err.isEmpty()) debuggerPanel.appendLine("[error] " + err);
                }
                statusLabel.setText("Executed with Nashorn: " + active.getName());
                return;
            }
        } catch (Exception ignored) { }

        // If WebView is available, run in the browser engine (live debug)
        if (debuggerPanel != null) {
            try {
                debuggerPanel.appendLine("[info] Running JS in WebView: " + active.getName());
                debuggerPanel.executeScript(code);
                statusLabel.setText("Executed JS in preview: " + active.getName());
                return;
            } catch (Exception ignored) { }
        }

        // Fallback: try Node.js
        try {
            String node = "node";
            new ProcessBuilder(node, active.getAbsolutePath()).inheritIO().start();
            statusLabel.setText("Running with Node: " + active.getName());
        } catch (IOException e) {
            showError("Run failed. Is Node.js installed?\n" + e.getMessage());
        }
    }

    @FXML
    private void onToggleDebugger() {
        if (debuggerPanel != null) {
            boolean vis = mainSplit.getItems().contains(debuggerPanel);
            toggleSplitItem(3, !vis);
            if (debugBtn != null) {
                debugBtn.setText(vis ? "Debug" : "Debug (shown)");
                debugBtn.setGraphic(IconManager.imageView(IconManager.BUG, 16));
            }
            statusLabel.setText(vis ? "Debugger hidden" : "Debugger shown");
        }
    }

    @FXML
    private void onToggleAiAssistant() {
        if (aiPanel != null) {
            boolean vis = mainSplit.getItems().contains(aiPanel);
            toggleSplitItem(4, !vis);
            if (viewAiItem != null) viewAiItem.setSelected(!vis);
            if (aiBtn != null) {
                aiBtn.setText(vis ? "AI" : "AI (shown)");
                aiBtn.setGraphic(IconManager.imageView(IconManager.ROBOT, 16));
            }
            statusLabel.setText(vis ? "AI Assistant hidden" : "AI Assistant shown");
        }
    }

    @FXML
    private void onToggleTerminal() {
        if (terminalPanel != null) {
            boolean vis = mainSplit.getItems().contains(terminalPanel);
            toggleSplitItem(5, !vis);
            if (viewTerminalItem != null) viewTerminalItem.setSelected(!vis);
            if (terminalBtn != null) {
                terminalBtn.setText(vis ? "Terminal" : "Terminal (shown)");
                terminalBtn.setGraphic(IconManager.imageView(IconManager.TERMINAL, 16));
            }
            statusLabel.setText(vis ? "Terminal hidden" : "Terminal shown");
            if (!vis && terminalPanel != null) terminalPanel.newTerminal();
        }
    }

    @FXML
    private void onToggleGit() {
        if (gitPanel != null) {
            boolean vis = mainSplit.getItems().contains(gitPanel);
            toggleSplitItem(6, !vis);
            if (viewGitItem != null) viewGitItem.setSelected(!vis);
            if (gitBtn != null) {
                gitBtn.setText(vis ? "Git" : "Git (shown)");
                gitBtn.setGraphic(IconManager.imageView(IconManager.GIT_BRANCH, 16));
            }
            statusLabel.setText(vis ? "Git hidden" : "Git shown");
            if (!vis && gitPanel != null) gitPanel.refreshBranch();
        }
    }

    @FXML
    private void onToggleTodo() {
        if (todoPanel != null) {
            boolean vis = mainSplit.getItems().contains(todoPanel);
            toggleSplitItem(7, !vis);
            if (viewTodoItem != null) viewTodoItem.setSelected(!vis);
            statusLabel.setText(vis ? "TODO panel hidden" : "TODO panel shown");
            if (!vis) todoPanel.setProjectRoot(projectRoot);
        }
    }

    @FXML
    private void onToggleClipboardHistory() {
        if (clipboardHistoryPanel != null) {
            boolean vis = mainSplit.getItems().contains(clipboardHistoryPanel);
            toggleSplitItem(8, !vis);
            if (viewClipboardItem != null) viewClipboardItem.setSelected(!vis);
            statusLabel.setText(vis ? "Clipboard History hidden" : "Clipboard History shown");
        }
    }

    @FXML
    private void onToggleScratchPad() {
        if (scratchPadPanel != null) {
            boolean vis = mainSplit.getItems().contains(scratchPadPanel);
            toggleSplitItem(9, !vis);
            if (viewScratchPadItem != null) viewScratchPadItem.setSelected(!vis);
            statusLabel.setText(vis ? "Scratch Pad hidden" : "Scratch Pad shown");
        }
    }

    @FXML
    private void onQuickBuildApk() {
        if (projectRoot == null) {
            showError("No project is open. Open a project first.");
            return;
        }
        String projName = projectRoot.getName();
        File sourceFile = new File(projectRoot, "index.html");
        boolean hasIndexHtml = sourceFile.exists();
        if (!hasIndexHtml) {
            sourceFile = new File(projectRoot, "index.js");
            if (!sourceFile.exists()) {
                statusLabel.setText("No index.html or index.js found — open the detailed builder");
                onBuildToApk();
                return;
            }
        }
        final File src = sourceFile;

        Stage progressStage = new Stage();
        progressStage.initOwner(rootPane.getScene().getWindow());
        progressStage.initModality(Modality.APPLICATION_MODAL);
        progressStage.setTitle("Quick APK Build — " + projName);

        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefSize(500, 300);

        Label label = new Label("Building " + projName + " → APK...");
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10;");

        Button closeBtn = new Button("Close");
        closeBtn.setDisable(true);
        closeBtn.setOnAction(e -> progressStage.close());

        VBox vbox = new VBox(10, label, logArea, closeBtn);
        vbox.setPadding(new Insets(10));
        Scene progressScene = new Scene(vbox);
        ThemeManager.getInstance().applyTheme(progressScene);
        progressStage.setScene(progressScene);
        progressStage.setOnCloseRequest(e -> {
            if (!closeBtn.isDisable()) progressStage.close();
            else e.consume();
        });
        progressStage.show();

        new Thread(() -> {
            try {
                SimpleApkBuilder.ApkBuildConfig config = new SimpleApkBuilder.ApkBuildConfig();
                config.sourceFile = src;
                config.apkName = projName.replaceAll("[^a-zA-Z0-9]", "");
                config.packageName = projName.toLowerCase().replaceAll("[^a-z0-9]", "");
                config.workDir = new File(System.getProperty("java.io.tmpdir") + "\\apk_quick_" + System.currentTimeMillis());
                config.workDir.mkdirs();
                SimpleApkBuilder.buildApk(config, logArea);
                Platform.runLater(() -> {
                    label.setText("Build complete. APK on Desktop.");
                    closeBtn.setDisable(false);
                    statusLabel.setText("APK built: " + config.apkName + ".apk");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    label.setText("Build failed");
                    logArea.appendText("\nError: " + ex.getMessage() + "\n");
                    closeBtn.setDisable(false);
                });
            }
        }).start();
    }

    @FXML
    private void onGenerateProject() {
        Dialog<Map<String, Object>> dialog = new Dialog<>();
        dialog.setTitle("AI Project Generator");
        dialog.setHeaderText("Create a full project with AI");
        dialog.initOwner(rootPane.getScene().getWindow());
        dialog.initModality(Modality.APPLICATION_MODAL);

        TextField nameField = new TextField();
        nameField.setPromptText("e.g. MyTodoApp");

        TextArea descField = new TextArea();
        descField.setPromptText("Describe the project in full detail — features, pages, components, UI, data, etc.");
        descField.setPrefRowCount(5);
        descField.setWrapText(true);

        // ── Category groups ──
        java.util.LinkedHashMap<String, String[]> categories = new java.util.LinkedHashMap<>();
        categories.put("Classic Web", new String[]{
            "HTML/CSS/JS Website", "Static Landing Page", "PWA Project",
            "Alpine.js SPA", "Web Components (Vanilla)", "Stencil.js PWA",
            "Docusaurus Site"
        });
        categories.put("SPA Frameworks", new String[]{
            "React SPA (JavaScript)", "React SPA (TypeScript)", "Vue.js SPA",
            "Angular SPA", "Svelte SPA", "SolidJS SPA", "Preact SPA",
            "Lit Element Project", "Qwik Project"
        });
        categories.put("Vite Modern", new String[]{
            "Vite + Vue 3", "Vite + React", "Vite + TypeScript"
        });
        categories.put("Fullstack / Meta", new String[]{
            "Next.js Fullstack", "Next.js + TypeScript", "Nuxt 3 Project",
            "SvelteKit Project", "Remix Project", "Astro Project",
            "Gatsby Project", "Full-Stack MERN App"
        });
        categories.put("Backend / API", new String[]{
            "Node.js REST API", "Express + EJS Web App", "Express + MongoDB API",
            "NestJS API", "AdonisJS API", "HTMX + Express",
            "Python Flask Web App", "Python Django Web App", "FastAPI Python",
            "Python Script", "Java Spring Boot API", "Go Web Server",
            "PHP Web App", "Laravel PHP", "Symfony PHP",
            "Ruby on Rails", "ASP.NET Core Web API"
        });
        categories.put("Mobile Hybrid", new String[]{
            "Cordova Android App", "Ionic Angular App", "Capacitor JS App",
            "React Native Web", "Quasar Vue App", "Framework7 App",
            "NativeScript Vue App", "Onsen UI + React"
        });
        categories.put("Desktop", new String[]{
            "Electron Desktop App", "Tauri + React App"
        });
        categories.put("Console / CLI", new String[]{
            "Java Console Application", "JavaScript CLI Tool",
            "C++ Console Application", "C# .NET Console App",
            "Rust CLI Tool", "Kotlin Console App", "Java Desktop (Swing)"
        });
        categories.put("Runtimes / SSG", new String[]{
            "Deno Project", "Bun Project", "Hugo Site",
            "Eleventy (11ty) Site", "Jekyll Site"
        });
        categories.put("Special", new String[]{
            "Flutter Web Project", "Docker + Compose Project",
            "Multi-Service Project", "Empty Project"
        });

        ListView<String> categoryList = new ListView<>();
        categoryList.setItems(FXCollections.observableArrayList(categories.keySet()));
        categoryList.setPrefWidth(130);
        categoryList.getSelectionModel().select(0);

        ListView<String> typeList = new ListView<>();
        typeList.setPrefWidth(320);

        categoryList.getSelectionModel().selectedItemProperty().addListener((obs, old, cat) -> {
            if (cat != null) {
                String[] types = categories.get(cat);
                typeList.setItems(FXCollections.observableArrayList(types));
                typeList.getSelectionModel().select(0);
            }
        });
        // Trigger initial population
        categoryList.getSelectionModel().select(0);

        SplitPane split = new SplitPane(categoryList, typeList);
        split.setDividerPositions(0.3);
        split.setPrefHeight(220);

        CheckBox templateOnly = new CheckBox("Use templates only (skip AI)");
        templateOnly.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted;");

        VBox dialogContent = new VBox(8);
        dialogContent.setPadding(new Insets(20));
        dialogContent.getChildren().addAll(
            new Label("Project Name:"), nameField,
            new Label("Category / Type:"), split,
            new Label("Description:"), descField, templateOnly
        );
        dialog.getDialogPane().setContent(dialogContent);
        dialog.getDialogPane().setPrefWidth(560);

        ButtonType createBtn = new ButtonType("Generate Project", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createBtn, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> {
            if (btn == createBtn) {
                Map<String, Object> m = new HashMap<>();
                m.put("name", nameField.getText().trim());
                m.put("desc", descField.getText().trim());
                String selectedType = typeList.getSelectionModel().getSelectedItem();
                m.put("type", selectedType != null ? selectedType : "HTML/CSS/JS Website");
                m.put("templateOnly", templateOnly.isSelected());
                return m;
            }
            return null;
        });
        dialog.getDialogPane().lookupButton(createBtn).disableProperty().bind(
            nameField.textProperty().isEmpty().or(descField.textProperty().isEmpty()));

        Scene scene = dialog.getDialogPane().getScene();
        if (scene != null) ThemeManager.getInstance().applyTheme(scene);

        Optional<Map<String, Object>> result = dialog.showAndWait();
        if (!result.isPresent()) return;
        String projName = (String) result.get().get("name");
        String description = (String) result.get().get("desc");
        String projType = (String) result.get().get("type");
        boolean useTemplatesOnly = (boolean) result.get().get("templateOnly");

        File projectsDir = ProjectsStore.getProjectsRoot();
        File projectDir = new File(projectsDir, projName.replaceAll("[^a-zA-Z0-9._-]", ""));
        if (projectDir.exists()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Project \"" + projName + "\" already exists. Overwrite?", ButtonType.YES, ButtonType.NO);
            alert.initOwner(rootPane.getScene().getWindow());
            ThemeManager.getInstance().applyTheme(alert.getDialogPane().getScene());
            if (alert.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
            try { deleteDirectory(projectDir); } catch (Exception ignored) {}
        }
        projectDir.mkdirs();
        try { new File(projectDir, ".eagle-project").createNewFile(); } catch (Exception ignored) {}

        if ("Empty Project".equals(projType)) {
            RecentProjectsStore.addRecent(projectDir);
            refreshTree();
            switchToProject(projectDir);
            statusLabel.setText("Created empty project: " + projName);
            return;
        }

        String genPromptText = "Project: " + projName + " | Type: " + projType + " | Desc: " + description;
        saveAiPrompt("generate_" + projName, genPromptText);

        if (useTemplatesOnly) {
            setGeneratorProgress(-1, "[Generator] Using templates for \"" + projName + "\"...");
            generateFallbackProject(projectDir, projName, description, projType);
        } else {
            setGeneratorProgress(0.02, "[Generator] AI — Planning...");
            generateWithEngine(projectDir, projName, description, projType);
        }
    }

    // ----------------------------------------------------------------
    //  GENERATOR LOG
    // ----------------------------------------------------------------
    private final List<String> generatorLog = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
    private GeneratorLogViewer generatorLogViewer;
    private String lastGeneratedProjectPath = "";

    private void appendGeneratorLog(String msg) {
        String ts = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        String line = "[" + ts + "] " + msg;
        generatorLog.add(line);
        // Append to log file
        try {
            File logDir = new File(System.getProperty("user.home") + "/.webide");
            logDir.mkdirs();
            File logFile = new File(logDir, "generator.log");
            java.nio.file.Files.write(logFile.toPath(),
                (line + "\n").getBytes(StandardCharsets.UTF_8),
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
    }

    private void setGeneratorProgress(double value, String msg) {
        Platform.runLater(() -> {
            if (msg != null) {
                String ts = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
                statusLabel.setText("[" + ts + "] " + msg);
            }
            if (value >= 0) {
                generationProgress.setVisible(true);
                generationProgress.setProgress(Math.min(1.0, Math.max(0, value)));
                viewLogBtn.setVisible(true);
            }
        });
    }

    private void hideGeneratorProgress() {
        Platform.runLater(() -> {
            generationProgress.setVisible(false);
            generationProgress.setProgress(0);
        });
    }

    // ----------------------------------------------------------------
    //  AI GENERATION — Delegates to modular AiProjectEngine
    // ----------------------------------------------------------------
    private void generateWithEngine(File projectDir, String projName, String description, String projType) {
        String[] prov = {"gemini"};
        String[] gKey = {""};
        String[] gMod = {"gemini-2.0-flash"};
        String[] oEp = {"https://api.groq.com/openai/v1/chat/completions"};
        String[] oKey = {""};
        String[] oMod = {"llama-3.3-70b-versatile"};
        String[] ollEp = {"http://localhost:11434/api/chat"};
        String[] ollMod = {"llama3.2"};
        File cfgFile = new File(System.getProperty("user.home") + "/.webide/ai.properties");
        if (cfgFile.exists()) {
            java.util.Properties p = new java.util.Properties();
            try (java.io.FileInputStream fis = new java.io.FileInputStream(cfgFile)) {
                p.load(fis);
                prov[0] = p.getProperty("provider", "gemini");
                gKey[0] = p.getProperty("gemini.key", "");
                gMod[0] = p.getProperty("gemini.model", "gemini-2.0-flash");
                String tmp = p.getProperty("openai.endpoint", "");
                if (!tmp.isEmpty()) oEp[0] = tmp;
                oKey[0] = p.getProperty("openai.key", "");
                oMod[0] = p.getProperty("openai.model", "llama-3.3-70b-versatile");
                tmp = p.getProperty("ollama.endpoint", "");
                if (!tmp.isEmpty()) ollEp[0] = tmp;
                ollMod[0] = p.getProperty("ollama.model", "llama3.2");
            } catch (Exception ignored) {}
        }
        boolean hasKey = (prov[0].equals("gemini") && !gKey[0].isEmpty())
            || (prov[0].equals("openai") && !oKey[0].isEmpty())
            || prov[0].equals("ollama");
        if (!hasKey) {
            appendGeneratorLog("No AI key configured — using templates");
            setGeneratorProgress(-1, "[Generator] No AI key — using templates");
            generateFallbackProject(projectDir, projName, description, projType);
            return;
        }

        final String fProv = prov[0];
        final String fGKey = gKey[0];
        final String fGMod = gMod[0];
        final String fOEp = oEp[0];
        final String fOKey = oKey[0];
        final String fOMod = oMod[0];
        final String fOllEp = ollEp[0];
        final String fOllMod = ollMod[0];

        lastGeneratedProjectPath = projectDir.getAbsolutePath();

        final AiProvider aiProvider = new EngineAiProvider(fProv, fGKey, fGMod, fOEp, fOKey, fOMod, fOllEp, fOllMod);
        final ProgressMonitor monitor = new EngineProgressMonitor();

        new Thread(() -> {
            try {
                appendGeneratorLog("=== GENERATION STARTED ===");
                appendGeneratorLog("Project: " + projName + " | Type: " + projType);
                appendGeneratorLog("Dir: " + projectDir.getAbsolutePath());
                appendGeneratorLog("Provider: " + fProv);

                AiProjectEngine engine = new AiProjectEngine(aiProvider, monitor);
                AiProjectEngine.GenerationResult result = engine.generate(
                    projName, projType, description, projectDir);

                Platform.runLater(() -> {
                    if (result.isSuccess() && !result.getFilePaths().isEmpty()) {
                        RecentProjectsStore.addRecent(projectDir);
                        refreshTree();
                        ProjectType type;
                        try { type = ProjectsStore.getProjectType(projectDir); } catch (Exception ex) { type = ProjectType.CODE; }
                        EditorController targetCtrl = this;
                        if (type == ProjectType.VISUAL) openVisualProject(projectDir);
                        else targetCtrl = switchToProject(projectDir);
                        if (targetCtrl == null) targetCtrl = this;
                        int opened = 0;
                        for (String fPath : result.getFilePaths()) {
                            File f = new File(projectDir, fPath);
                            if (f.exists() && opened < 8) {
                                targetCtrl.openFile(f);
                                opened++;
                            }
                        }
                        try { writeReadmeFile(projectDir, projName, description, projType); } catch (Exception ignored) {}
                        String status = "[Generator] Done — " + result.getFilesGenerated() + " files created for \"" + projName + "\"";
                        setGeneratorProgress(1.0, status);
                        appendGeneratorLog("=== GENERATION COMPLETE: " + result.getFilesGenerated()
                            + " files | Issues: " + result.getIssuesFound()
                            + " fixed: " + result.getIssuesFixed() + " ===");
                    } else {
                        String reason = result.isSuccess() ? "0 files" : result.getErrorMessage();
                        setGeneratorProgress(-1, "[Generator] AI " + reason + " — using templates");
                        appendGeneratorLog("AI failed (" + reason + "), using templates as fallback");
                        generateFallbackProject(projectDir, projName, description, projType);
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                final String errMsg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                appendGeneratorLog("FATAL: " + errMsg);
                Platform.runLater(() -> {
                    setGeneratorProgress(-1, "[Generator] AI error: " + errMsg + " — using templates");
                    generateFallbackProject(projectDir, projName, description, projType);
                });
            }
        }).start();
    }

    // ----------------------------------------------------------------
    //  INNER CLASSES for AiProjectEngine integration
    // ----------------------------------------------------------------
    private class EngineAiProvider implements AiProvider {
        private final String prov, gKey, gMod, oEp, oKey, oMod, ollEp, ollMod;
        EngineAiProvider(String p, String gk, String gm, String oe, String ok, String om, String ole, String olm) {
            this.prov = p; this.gKey = gk; this.gMod = gm; this.oEp = oe;
            this.oKey = ok; this.oMod = om; this.ollEp = ole; this.ollMod = olm;
        }
        @Override
        public String call(String systemPrompt, String userPrompt) throws Exception {
            return callAiApi(prov, gKey, gMod, oEp, oKey, oMod, ollEp, ollMod,
                systemPrompt + "\n\n" + userPrompt);
        }
        @Override public String getModelName() {
            return prov.equals("gemini") ? gMod : prov.equals("openai") ? oMod : ollMod;
        }
        @Override public String getProviderName() { return prov; }
    }

    private class EngineProgressMonitor implements ProgressMonitor {
        @Override public void onPhase(String phase, double percent) {
            double pct = Math.min(1.0, Math.max(0, percent / 100.0));
            setGeneratorProgress(pct, "[Generator] " + phase + "...");
        }
        @Override public void onLog(String message) { appendGeneratorLog(message); }
        @Override public void onError(String error) { appendGeneratorLog("ERROR: " + error); }
        @Override public void onComplete(int totalFiles, java.util.List<String> filePaths) { }
        @Override public boolean isCancelled() { return false; }
    }

    @FXML
    private void onViewGenerationLog() {
        if (generatorLog.isEmpty()) {
            showError("No generator log available. Generate a project first.");
            return;
        }
        if (generatorLogViewer == null || !generatorLogViewer.isShowing()) {
            generatorLogViewer = new GeneratorLogViewer(generatorLog, lastGeneratedProjectPath,
                rootPane.getScene().getWindow());
            generatorLogViewer.show();
        } else {
            generatorLogViewer.toFront();
        }
    }

    // ----------------------------------------------------------------
    //  TEMPLATE FALLBACK — 22 project types
    // ----------------------------------------------------------------
    private void generateFallbackProject(File projectDir, String projName, String description, String projType) {
        try {
            generateProjectFiles(projectDir, projName, description, projType);
            writeReadmeFile(projectDir, projName, description, projType);
        } catch (Exception ex) {
            showError("Failed to create project: " + ex.getMessage());
            return;
        }
        RecentProjectsStore.addRecent(projectDir);
        refreshTree();
        ProjectType type;
        try { type = ProjectsStore.getProjectType(projectDir); } catch (Exception e) { type = ProjectType.CODE; }
        EditorController targetCtrl = this;
        if (type == ProjectType.VISUAL) openVisualProject(projectDir);
        else targetCtrl = switchToProject(projectDir);
        if (targetCtrl == null) targetCtrl = this;
        File[] children = projectDir.listFiles();
        if (children != null) {
            int opened = 0;
            for (File f : children) {
                if (f.isFile() && opened < 5) { targetCtrl.openFile(f); opened++; }
            }
        }
        setGeneratorProgress(1.0, "[Generator] Template project created: \"" + projName + "\" (" + projType + ")");
        // Detect technologies and configure editor for this project
        detectProjectTechnologies();
    }

    private void writeReadmeFile(File projectDir, String name, String desc, String projType) throws IOException {
        String type = projType.toLowerCase();
        String setupCmd = "", runCmd = "", buildCmd = "", tech = "";
        boolean hasPkgJson = false, hasPip = false, hasMaven = false, hasDocker = false;
        if (type.contains("react") || type.contains("vue") || type.contains("angular") || type.contains("svelte")
            || type.contains("next") || type.contains("nuxt") || type.contains("remix") || type.contains("astro")
            || type.contains("gatsby") || type.contains("solid") || type.contains("preact") || type.contains("lit")
            || type.contains("qwik") || type.contains("stencil") || type.contains("quasar") || type.contains("framework7")
            || type.contains("nativescript") || type.contains("onsen") || type.contains("ionic") || type.contains("electron")
            || type.contains("capacitor") || type.contains("cordova") || type.contains("react native")
            || type.contains("node") || type.contains("express") || type.contains("nest") || type.contains("adonis")
            || type.contains("htmx") || type.contains("docusaurus") || type.contains("eleventy") || type.contains("11ty")
            || type.contains("vuepress") || type.contains("vitepress") || type.contains("alpine")
            || type.contains("sveltekit") || type.contains("redwood") || type.contains("blitz")
            || type.contains("web component") || type.contains("mer") || type.contains("deno") || type.contains("bun")
            || type.contains("preact") || type.contains("solid") || type.contains("gatsby") || type.contains("vite")
            || type.contains("javascript") || type.contains("pwa")) {
            hasPkgJson = true;
            setupCmd = "npm install";
            runCmd = type.contains("next") || type.contains("nuxt") || type.contains("remix") || type.contains("astro")
                || type.contains("sveltekit") || type.contains("gatsby") || type.contains("docusaurus")
                || type.contains("vuepress") || type.contains("vitepress") || type.contains("redwood") || type.contains("blitz")
                || type.contains("adonis") || type.contains("stencil") ? "npm run dev"
                : type.contains("html") || type.contains("static landing") || type.contains("alpine") || type.contains("web component")
                || type.contains("eleventy") || type.contains("11ty") ? "open index.html (or use Live Server)"
                : type.contains("electron") || type.contains("cordova") || type.contains("capacitor") || type.contains("ionic") ? "npm start"
                : type.contains("nest") || type.contains("htmx") || type.contains("express") || type.contains("node")
                || type.contains("adonis") || type.contains("mer") || type.contains("nativescript") || type.contains("onsen") ? "npm start"
                : "npm start";
            buildCmd = "npm run build";
            tech = "Node.js, npm";
        } else if (type.contains("python") || type.contains("flask") || type.contains("django") || type.contains("fastapi")) {
            hasPip = true;
            setupCmd = "pip install -r requirements.txt";
            runCmd = type.contains("django") ? "python manage.py runserver"
                : type.contains("fastapi") ? "uvicorn main:app --reload"
                : "python app.py";
            tech = "Python";
        } else if (type.contains("spring") || type.contains("java console") || type.contains("swing")
            || type.contains("java desktop") || (type.contains("java") && !type.contains("javascript"))) {
            hasMaven = true;
            setupCmd = type.contains("maven") || type.contains("spring") ? "mvn clean install" : "mkdir -p build && javac -d build src/**/*.java";
            runCmd = type.contains("spring") ? "mvn spring-boot:run"
                : type.contains("maven") ? "java -jar target/" + name.toLowerCase().replaceAll("[^a-z]", "") + "-1.0.jar"
                : type.contains("swing") || type.contains("java desktop") || type.contains("java console")
                ? "java -cp build Main" : "java -cp build Main";
            tech = "Java, Maven";
        } else if (type.contains("go")) {
            setupCmd = "go mod init " + name.toLowerCase().replaceAll("[^a-z0-9]", "") + " && go mod tidy";
            runCmd = "go run main.go";
            buildCmd = "go build -o " + name.toLowerCase().replaceAll("[^a-z0-9]", "") + " .";
            tech = "Go";
        } else if (type.contains("rust")) {
            setupCmd = "cargo build";
            runCmd = "cargo run";
            buildCmd = "cargo build --release";
            tech = "Rust, Cargo";
        } else if (type.contains("kotlin")) {
            setupCmd = "kotlinc src/*.kt -include-runtime -d " + name.toLowerCase().replaceAll("[^a-z0-9]", "") + ".jar";
            runCmd = "java -jar " + name.toLowerCase().replaceAll("[^a-z0-9]", "") + ".jar";
            tech = "Kotlin";
        } else if (type.contains("c#") || type.contains("csharp") || type.contains(".net") || type.contains("asp.net")) {
            setupCmd = "dotnet restore";
            runCmd = "dotnet run";
            buildCmd = "dotnet build";
            tech = ".NET";
        } else if (type.contains("php") || type.contains("laravel") || type.contains("symfony")) {
            hasPkgJson = true;
            setupCmd = type.contains("laravel") || type.contains("symfony") ? "composer install" : "php -S localhost:8000";
            runCmd = type.contains("laravel") ? "php artisan serve"
                : type.contains("symfony") ? "symfony serve"
                : "php -S localhost:8000";
            tech = type.contains("laravel") ? "PHP, Laravel" : type.contains("symfony") ? "PHP, Symfony" : "PHP";
        } else if (type.contains("ruby") || type.contains("rails")) {
            setupCmd = "bundle install";
            runCmd = "rails server";
            tech = "Ruby on Rails";
        } else if (type.contains("flutter") || type.contains("dart")) {
            setupCmd = "flutter pub get";
            runCmd = "flutter run";
            buildCmd = "flutter build web";
            tech = "Flutter, Dart";
        } else if (type.contains("hugo")) {
            setupCmd = "hugo new site .";
            runCmd = "hugo server";
            buildCmd = "hugo";
            tech = "Hugo";
        } else if (type.contains("jekyll")) {
            setupCmd = "gem install jekyll bundler";
            runCmd = "jekyll serve";
            tech = "Jekyll";
        } else if (type.contains("docker") || type.contains("multi-service")) {
            setupCmd = "docker-compose build";
            runCmd = "docker-compose up";
            tech = "Docker Compose";
        } else if (type.contains("tauri")) {
            hasPkgJson = true;
            setupCmd = "npm install && cargo install tauri-cli";
            runCmd = "npx tauri dev";
            buildCmd = "npx tauri build";
            tech = "Tauri, Rust";
        } else {
            setupCmd = "No special setup required";
            runCmd = "Open index.html in a browser";
            tech = "HTML, CSS, JavaScript";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(name).append("\n\n");
        sb.append(desc).append("\n\n");
        sb.append("## Tech Stack\n\n");
        sb.append("- **Language/Framework**: ").append(tech).append("\n");
        if (!projType.isEmpty()) sb.append("- **Project Type**: ").append(projType).append("\n");
        sb.append("\n");
        sb.append("## Getting Started\n\n");
        sb.append("### Prerequisites\n\n");
        if (hasPkgJson) {
            sb.append("- [Node.js](https://nodejs.org/) (v16 or later)\n");
            sb.append("- npm (comes with Node.js)\n");
        }
        if (hasPip) sb.append("- [Python](https://python.org/) (3.8 or later)\n");
        if (hasMaven) sb.append("- [Java JDK](https://adoptium.net/) (11 or later)\n");
        if (hasDocker) sb.append("- [Docker](https://docker.com/) and Docker Compose\n");
        if (tech.contains("Go")) sb.append("- [Go](https://go.dev/) (1.20 or later)\n");
        if (tech.contains("Rust")) sb.append("- [Rust](https://rustup.rs/) (stable)\n");
        if (tech.contains("Kotlin")) sb.append("- [Kotlin](https://kotlinlang.org/) compiler\n");
        if (tech.contains(".NET")) sb.append("- [.NET SDK](https://dotnet.microsoft.com/download) (6.0 or later)\n");
        if (tech.contains("PHP")) sb.append("- [PHP](https://php.net/) (8.0 or later)\n");
        if (tech.contains("Flutter") || tech.contains("Dart")) sb.append("- [Flutter SDK](https://flutter.dev/) (3.0 or later)\n");
        if (tech.contains("Ruby")) sb.append("- [Ruby](https://ruby-lang.org/) (3.0 or later)\n");
        if (tech.contains("Hugo")) sb.append("- [Hugo](https://gohugo.io/) (extended edition)\n");
        if (tech.contains("Jekyll")) sb.append("- [Ruby](https://ruby-lang.org/) + Bundler\n");
        sb.append("\n");
        sb.append("### Installation\n\n");
        sb.append("```bash\n").append(setupCmd).append("\n```\n\n");
        sb.append("### Running\n\n");
        sb.append("```bash\n").append(runCmd).append("\n```\n\n");
        if (!buildCmd.isEmpty()) {
            sb.append("### Build\n\n");
            sb.append("```bash\n").append(buildCmd).append("\n```\n\n");
        }
        sb.append("## Project Structure\n\n");
        sb.append("```\n").append(name).append("/\n");
        File[] children = projectDir.listFiles();
        if (children != null) {
            java.util.Arrays.sort(children);
            int count = 0;
            for (File f : children) {
                if (count++ >= 20) { sb.append("  ... (more files)\n"); break; }
                if (f.isDirectory()) sb.append("  ").append(f.getName()).append("/\n");
                else sb.append("  ").append(f.getName()).append("\n");
            }
        }
        sb.append("```\n");
        writeFile(new File(projectDir, "README.md"), sb.toString());
    }

    private void generateProjectFiles(File projectDir, String projName, String description, String projType)
            throws IOException {
        projectDir.mkdirs();
        new File(projectDir, ".eagle-project").createNewFile();
        switch (projType) {
            // ── Core Web ──
            case "HTML/CSS/JS Website":
            case "Static Landing Page":
                generateHtmlProject(projectDir, projName, description); break;
            case "PWA Project":
                generatePwaProject(projectDir, projName, description); break;
            // ── SPA Frameworks ──
            case "React SPA (JavaScript)":
                generateReactProject(projectDir, projName, description, false); break;
            case "React SPA (TypeScript)":
                generateReactProject(projectDir, projName, description, true); break;
            case "Vue.js SPA":
                generateVueProject(projectDir, projName, description); break;
            case "Angular SPA":
                generateAngularProject(projectDir, projName, description); break;
            case "Svelte SPA":
                generateSvelteProject(projectDir, projName, description); break;
            case "SolidJS SPA":
                generateSolidJsProject(projectDir, projName, description); break;
            case "Preact SPA":
                generatePreactProject(projectDir, projName, description); break;
            case "Lit Element Project":
                generateLitProject(projectDir, projName, description); break;
            case "Qwik Project":
                generateQwikProject(projectDir, projName, description); break;
            // ── Fullstack / Meta-frameworks ──
            case "Next.js Fullstack":
            case "Next.js + TypeScript":
                generateNextProject(projectDir, projName, description); break;
            case "Nuxt 3 Project":
                generateNuxtProject(projectDir, projName, description); break;
            case "SvelteKit Project":
                generateSvelteKitProject(projectDir, projName, description); break;
            case "Remix Project":
                generateRemixProject(projectDir, projName, description); break;
            case "Astro Project":
                generateAstroProject(projectDir, projName, description); break;
            case "Gatsby Project":
                generateGatsbyProject(projectDir, projName, description); break;
            case "Full-Stack MERN App":
                generateMernProject(projectDir, projName, description); break;
            case "Docusaurus Site":
                generateDocusaurusProject(projectDir, projName, description); break;
            // ── Backend / API ──
            case "Node.js REST API":
            case "Express + EJS Web App":
                generateNodeApiProject(projectDir, projName, description); break;
            case "Python Flask Web App":
                generateFlaskProject(projectDir, projName, description); break;
            case "Python Django Web App":
                generateDjangoProject(projectDir, projName, description); break;
            case "Python Script":
                generatePythonProject(projectDir, projName, description); break;
            case "Java Spring Boot API":
                generateSpringBootProject(projectDir, projName, description); break;
            case "PHP Web App":
                generatePhpProject(projectDir, projName, description); break;
            case "Go Web Server":
                generateGoProject(projectDir, projName, description); break;
            // ── Console / CLI ──
            case "Java Console Application":
                generateJavaProject(projectDir, projName, description); break;
            case "JavaScript CLI Tool":
                generateJsCliProject(projectDir, projName, description); break;
            case "C++ Console Application":
                generateCppProject(projectDir, projName, description); break;
            case "C# .NET Console App":
                generateCsharpProject(projectDir, projName, description); break;
            case "Rust CLI Tool":
                generateRustProject(projectDir, projName, description); break;
            case "Kotlin Console App":
                generateKotlinProject(projectDir, projName, description); break;
            case "Java Desktop (Swing)":
                generateSwingProject(projectDir, projName, description); break;
            // ── Mobile Hybrid ──
            case "Cordova Android App":
                generateCordovaProject(projectDir, projName, description); break;
            case "Ionic Angular App":
                generateIonicProject(projectDir, projName, description); break;
            case "Capacitor JS App":
                generateCapacitorProject(projectDir, projName, description); break;
            case "React Native Web":
                generateReactNativeWebProject(projectDir, projName, description); break;
            // ── Desktop Hybrid ──
            case "Electron Desktop App":
                generateElectronProject(projectDir, projName, description); break;
            case "Tauri + React App":
                generateTauriProject(projectDir, projName, description); break;
            // ── Vite-Based Modern Frontend ──
            case "Vite + Vue 3":
                generateViteVueProject(projectDir, projName, description); break;
            case "Vite + React":
                generateViteReactProject(projectDir, projName, description); break;
            case "Vite + TypeScript":
                generateViteTypescriptProject(projectDir, projName, description); break;
            case "Alpine.js SPA":
                generateAlpineJsProject(projectDir, projName, description); break;
            case "HTMX + Express":
                generateHtmxExpressProject(projectDir, projName, description); break;
            case "Stencil.js PWA":
                generateStencilProject(projectDir, projName, description); break;
            case "Web Components (Vanilla)":
                generateWebComponentsProject(projectDir, projName, description); break;
            // ── Fullstack / Backend (Additional) ──
            case "NestJS API":
                generateNestJsProject(projectDir, projName, description); break;
            case "FastAPI Python":
                generateFastApiProject(projectDir, projName, description); break;
            case "Ruby on Rails":
                generateRailsProject(projectDir, projName, description); break;
            case "Laravel PHP":
                generateLaravelProject(projectDir, projName, description); break;
            case "ASP.NET Core Web API":
                generateAspNetCoreProject(projectDir, projName, description); break;
            case "Symfony PHP":
                generateSymfonyProject(projectDir, projName, description); break;
            case "Express + MongoDB API":
                generateExpressMongoProject(projectDir, projName, description); break;
            case "AdonisJS API":
                generateAdonisJsProject(projectDir, projName, description); break;
            // ── More Mobile Hybrid (Android APK) ──
            case "Quasar Vue App":
                generateQuasarProject(projectDir, projName, description); break;
            case "Framework7 App":
                generateFramework7Project(projectDir, projName, description); break;
            case "NativeScript Vue App":
                generateNativeScriptProject(projectDir, projName, description); break;
            case "Onsen UI + React":
                generateOnsenUiProject(projectDir, projName, description); break;
            // ── Modern JS Runtimes ──
            case "Deno Project":
                generateDenoProject(projectDir, projName, description); break;
            case "Bun Project":
                generateBunProject(projectDir, projName, description); break;
            // ── Static Site Generators ──
            case "Hugo Site":
                generateHugoProject(projectDir, projName, description); break;
            case "Eleventy (11ty) Site":
                generateEleventyProject(projectDir, projName, description); break;
            case "Jekyll Site":
                generateJekyllProject(projectDir, projName, description); break;
            // ── Special ──
            case "Flutter Web Project":
                generateFlutterWebProject(projectDir, projName, description); break;
            case "Docker + Compose Project":
            case "Multi-Service Project":
                generateDockerProject(projectDir, projName, description); break;
            default:
                generateHtmlProject(projectDir, projName, description); break;
        }
    }

    // ================================================================
    //  TEMPLATE GENERATORS
    // ================================================================

    private void generateHtmlProject(File dir, String name, String desc) throws IOException {
        String safe = escHtml(name);
        String safeDesc = escHtml(desc);
        writeFile(new File(dir, "index.html"),
            "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n"
            + "  <meta charset=\"UTF-8\">\n  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "  <title>" + safe + "</title>\n  <link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n"
            + "  <header>\n    <h1>" + safe + "</h1>\n    <p>" + safeDesc + "</p>\n  </header>\n"
            + "  <main id=\"app\"></main>\n  <footer><p>&copy; " + java.time.Year.now().getValue() + " " + safe + "</p></footer>\n"
            + "  <script src=\"script.js\"></script>\n</body>\n</html>\n");
        writeFile(new File(dir, "style.css"),
            "* { margin:0; padding:0; box-sizing:border-box; }\n"
            + "body { font-family:'Segoe UI',system-ui,sans-serif; line-height:1.6; color:#333; background:#f4f4f4; }\n"
            + "header { background:linear-gradient(135deg,#6C5CE7,#a29bfe); color:#fff; text-align:center; padding:60px 20px; }\n"
            + "header h1 { font-size:2.5em; margin-bottom:10px; }\n"
            + "main { max-width:900px; margin:40px auto; padding:20px; background:#fff; border-radius:8px; box-shadow:0 2px 10px rgba(0,0,0,0.1); }\n"
            + "footer { text-align:center; padding:20px; color:#666; font-size:0.9em; }\n"
            + ".btn { display:inline-block; padding:10px 20px; background:#6C5CE7; color:#fff; border:none; border-radius:4px; cursor:pointer; }\n"
            + ".btn:hover { background:#5a4bd1; }\n");
        writeFile(new File(dir, "script.js"),
            "// " + name + "\ndocument.addEventListener('DOMContentLoaded', () => {\n"
            + "  const app = document.getElementById('app');\n"
            + "  if (app) app.innerHTML = '<p>Welcome to " + escJs(name) + "!</p>';\n"
            + "  console.log('" + escJs(desc) + "');\n});\n");
        writeFile(new File(dir, "README.md"), "# " + name + "\n\n" + desc + "\n\n## Usage\nOpen `index.html` in a browser.\n");
    }

    private void generateReactProject(File dir, String name, String desc, boolean typescript) throws IOException {
        String ext = typescript ? "tsx" : "jsx";
        String ext2 = typescript ? "ts" : "js";
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"private\": true,\n  \"description\": \"" + escJson(desc) + "\",\n"
            + "  \"scripts\": { \"start\": \"react-scripts start\", \"build\": \"react-scripts build\" },\n"
            + "  \"dependencies\": { \"react\": \"^18.2.0\", \"react-dom\": \"^18.2.0\", \"react-scripts\": \"5.0.1\" },\n"
            + "  \"browserslist\": { \"production\": [\">0.2%\",\"not dead\",\"not op_mini all\"], \"development\": [\"last 1 chrome version\"] }\n}\n");
        writeFile(new File(dir, "public/index.html"),
            "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "  <title>" + name + "</title>\n</head>\n<body>\n  <div id=\"root\"></div>\n</body>\n</html>\n");
        writeFile(new File(dir, "src/index." + ext2),
            "import React from 'react';\nimport ReactDOM from 'react-dom/client';\nimport App from './App." + ext + "';\n"
            + "const root = ReactDOM.createRoot(document.getElementById('root'));\nroot.render(<React.StrictMode><App /></React.StrictMode>);\n");
        writeFile(new File(dir, "src/App." + ext),
            "import React from 'react';\nimport './App.css';\n\nconst App = () => (\n"
            + "  <div className=\"app\">\n    <header className=\"app-header\">\n"
            + "      <h1>" + name + "</h1>\n      <p>" + escHtml(desc) + "</p>\n    </header>\n"
            + "    <main></main>\n  </div>\n);\n\nexport default App;\n");
        writeFile(new File(dir, "src/App.css"),
            ".app { text-align:center; font-family:'Segoe UI',sans-serif; }\n"
            + ".app-header { background:#282c34; min-height:60vh; display:flex; flex-direction:column; align-items:center; justify-content:center; color:white; }\n");
        writeFile(new File(dir, "README.md"), "# " + name + "\n\n" + desc + "\n\n## Setup\n1. `npm install`\n2. `npm start`\n");
    }

    private void generateVueProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"description\": \"" + escJson(desc) + "\",\n"
            + "  \"scripts\": { \"serve\": \"vue-cli-service serve\", \"build\": \"vue-cli-service build\" },\n"
            + "  \"dependencies\": { \"vue\": \"^3.3.0\" }\n}\n");
        writeFile(new File(dir, "src/main.js"),
            "import { createApp } from 'vue';\nimport App from './App.vue';\ncreateApp(App).mount('#app');\n");
        writeFile(new File(dir, "public/index.html"),
            "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\">"
            + "<title>" + name + "</title></head><body><div id=\"app\"></div></body></html>\n");
        writeFile(new File(dir, "src/App.vue"),
            "<template>\n  <div class=\"app\">\n    <h1>" + name + "</h1>\n    <p>" + escHtml(desc) + "</p>\n  </div>\n</template>\n"
            + "<script>\nexport default { name: 'App' }\n</script>\n<style>\nbody { font-family:'Segoe UI',sans-serif; margin:0; }\n"
            + ".app { text-align:center; padding:40px; }\n</style>\n");
        writeFile(new File(dir, "README.md"), "# " + name + "\n\n" + desc + "\n");
    }

    private void generateNextProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"description\": \"" + escJson(desc) + "\",\n  \"scripts\": {\n"
            + "    \"dev\": \"next dev\", \"build\": \"next build\", \"start\": \"next start\"\n  },\n"
            + "  \"dependencies\": { \"next\": \"^14.0.0\", \"react\": \"^18.2.0\", \"react-dom\": \"^18.2.0\" }\n}\n");
        writeFile(new File(dir, "pages/index.js"),
            "export default function Home() {\n  return (\n    <div>\n      <h1>" + name + "</h1>\n      <p>" + escHtml(desc) + "</p>\n    </div>\n  );\n}\n");
        writeFile(new File(dir, "pages/api/hello.js"),
            "export default function handler(req, res) {\n  res.status(200).json({ name: '" + name + "' });\n}\n");
        writeFile(new File(dir, "README.md"), "# " + name + "\n\n" + desc + "\n\n1. `npm install`\n2. `npm run dev`\n");
    }

    private void generateNodeApiProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"description\": \"" + escJson(desc) + "\",\n  \"main\": \"server.js\",\n"
            + "  \"scripts\": { \"start\": \"node server.js\", \"dev\": \"nodemon server.js\" },\n"
            + "  \"dependencies\": { \"express\": \"^4.18.2\", \"cors\": \"^2.8.5\" }\n}\n");
        writeFile(new File(dir, "server.js"),
            "const express = require('express');\nconst cors = require('cors');\n"
            + "const app = express();\napp.use(cors());\napp.use(express.json());\n\n"
            + "app.get('/api/health', (req, res) => res.json({ status: 'ok', project: '" + name + "' }));\n\n"
            + "const PORT = process.env.PORT || 3000;\napp.listen(PORT, () => console.log('" + name + " running on port ' + PORT));\n");
        writeFile(new File(dir, "README.md"),
            "# " + name + "\n\n" + desc + "\n\n## API\n- `GET /api/health`\n\n## Setup\n1. `npm install`\n2. `npm start`\n");
    }

    private void generateFlaskProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "app.py"),
            "from flask import Flask, render_template\n\napp = Flask(__name__)\n\n"
            + "@app.route('/')\ndef home():\n    return render_template('index.html', name='" + name + "', desc=\"" + escJson(desc) + "\")\n\n"
            + "if __name__ == '__main__':\n    app.run(debug=True)\n");
        writeFile(new File(dir, "templates/index.html"),
            "<!DOCTYPE html>\n<html><head><title>{{ name }}</title><link rel=\"stylesheet\" href=\"/static/style.css\"></head>\n"
            + "<body><h1>{{ name }}</h1><p>{{ desc }}</p></body>\n</html>\n");
        writeFile(new File(dir, "static/style.css"),
            "body { font-family:'Segoe UI',sans-serif; margin:40px; text-align:center; background:#f4f4f4; }\n");
        writeFile(new File(dir, "requirements.txt"), "flask\n");
    }

    private void generateDjangoProject(File dir, String name, String desc) throws IOException {
        String pkg = name.toLowerCase().replaceAll("[^a-z0-9]", "");
        writeFile(new File(dir, "manage.py"),
            "#!/usr/bin/env python\nimport os, sys\n\ndef main():\n"
            + "    os.environ.setdefault('DJANGO_SETTINGS_MODULE', '" + pkg + ".settings')\n"
            + "    from django.core.management import execute_from_command_line\n    execute_from_command_line(sys.argv)\n\n"
            + "if __name__ == '__main__':\n    main()\n");
        writeFile(new File(dir, pkg + "/settings.py"),
            "import os\nBASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))\n"
            + "SECRET_KEY = 'dev-only-change-in-production'\nDEBUG = True\nALLOWED_HOSTS = []\n"
            + "INSTALLED_APPS = ['django.contrib.contenttypes', 'django.contrib.staticfiles']\n"
            + "ROOT_URLCONF = '" + pkg + ".urls'\n"
            + "DATABASES = { 'default': { 'ENGINE': 'django.db.backends.sqlite3', 'NAME': os.path.join(BASE_DIR, 'db.sqlite3') } }\n"
            + "STATIC_URL = '/static/'\n");
        writeFile(new File(dir, pkg + "/urls.py"),
            "from django.urls import path\nfrom . import views\nurlpatterns = [path('', views.home)]\n");
        writeFile(new File(dir, pkg + "/views.py"),
            "from django.http import HttpResponse\ndef home(request):\n    return HttpResponse('<h1>" + name + "</h1><p>" + escHtml(desc) + "</p>')\n");
        writeFile(new File(dir, "requirements.txt"), "django\n");
    }

    private void generateSpringBootProject(File dir, String name, String desc) throws IOException {
        String pkg = name.toLowerCase().replaceAll("[^a-z0-9]", "");
        File src = new File(dir, "src/main/java/com/example/" + pkg);
        writeFile(new File(src, "Application.java"),
            "package com.example." + pkg + ";\n\nimport org.springframework.boot.SpringApplication;\n"
            + "import org.springframework.boot.autoconfigure.SpringBootApplication;\n\n"
            + "@SpringBootApplication\npublic class Application {\n"
            + "    public static void main(String[] args) {\n        SpringApplication.run(Application.class, args);\n    }\n}\n");
        writeFile(new File(src, "web/HelloController.java"),
            "package com.example." + pkg + ".web;\n\nimport org.springframework.web.bind.annotation.GetMapping;\n"
            + "import org.springframework.web.bind.annotation.RestController;\n\n@RestController\npublic class HelloController {\n"
            + "    @GetMapping(\"/api/hello\")\n    public String hello() { return \"" + name + " is running!\"; }\n}\n");
        writeFile(new File(dir, "pom.xml"),
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
            + "  xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\"\n"
            + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
            + "  <modelVersion>4.0.0</modelVersion>\n"
            + "  <parent><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-parent</artifactId><version>3.2.0</version></parent>\n"
            + "  <groupId>com.example</groupId><artifactId>" + pkg + "</artifactId><version>1.0.0</version>\n"
            + "  <dependencies><dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency></dependencies>\n"
            + "</project>\n");
        writeFile(new File(dir, "README.md"), "# " + name + "\n\n" + desc + "\n\n## Build\n`mvn spring-boot:run`\n");
    }

    private void generateSwingProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "src/Main.java"),
            "import javax.swing.*;\nimport java.awt.*;\n\n"
            + "public class Main {\n    public static void main(String[] args) {\n"
            + "        SwingUtilities.invokeLater(() -> {\n"
            + "            JFrame frame = new JFrame(\"" + name + "\");\n"
            + "            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);\n"
            + "            frame.setSize(800, 600);\n"
            + "            frame.setLocationRelativeTo(null);\n"
            + "            JLabel label = new JLabel(\"" + name + "\\n" + escHtml(desc) + "\", SwingConstants.CENTER);\n"
            + "            label.setFont(new Font(\"Segoe UI\", Font.BOLD, 24));\n"
            + "            frame.add(label);\n"
            + "            frame.setVisible(true);\n"
            + "        });\n    }\n}\n");
    }

    private void generateJavaProject(File dir, String name, String desc) throws IOException {
        new File(dir, "src").mkdirs();
        writeFile(new File(dir, "src/Main.java"),
            "package " + name.toLowerCase().replaceAll("[^a-z0-9]", "") + ";\n\n"
            + "public class Main {\n    public static void main(String[] args) {\n"
            + "        System.out.println(\"" + name + " started!\");\n"
            + "        System.out.println(\"" + escJson(desc) + "\");\n    }\n}\n");
    }

    private void generatePythonProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "main.py"),
            "\"\"\"" + name + "\n" + desc + "\"\"\"\n\n"
            + "def main():\n    print(f'{name} started!')\n\n"
            + "if __name__ == '__main__':\n    main()\n");
        writeFile(new File(dir, "README.md"), "# " + name + "\n\n" + desc + "\n\n## Run\n`python main.py`\n");
    }

    private void generateJsCliProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"description\": \"" + escJson(desc) + "\",\n"
            + "  \"bin\": { \"" + name.toLowerCase() + "\": \"./cli.js\" },\n"
            + "  \"scripts\": { \"start\": \"node cli.js\" }\n}\n");
        writeFile(new File(dir, "cli.js"),
            "#!/usr/bin/env node\nconsole.log('" + name + " CLI');\nconsole.log('" + escJs(desc) + "');\n"
            + "const args = process.argv.slice(2);\nconsole.log('Args:', args.length ? args : '(none)');\n");
        writeFile(new File(dir, "README.md"), "# " + name + "\n\n" + desc + "\n\n## Usage\n`node cli.js`\n");
    }

    private void generatePhpProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "index.php"),
            "<!DOCTYPE html>\n<html>\n<head><title>" + name + "</title><link rel=\"stylesheet\" href=\"style.css\"></head>\n"
            + "<body>\n  <h1><?= \"" + name + "\" ?></h1>\n  <p><?= \"" + escHtml(desc) + "\" ?></p>\n"
            + "  <p>Today: <?= date('Y-m-d H:i:s') ?></p>\n</body>\n</html>\n");
        writeFile(new File(dir, "config.php"),
            "<?php\ndefine('APP_NAME', '" + name + "');\ndefine('APP_DESC', '" + escJs(desc) + "');\n");
        writeFile(new File(dir, "style.css"),
            "body { font-family:'Segoe UI',sans-serif; margin:40px; text-align:center; background:#f8f9fa; }\n");
    }

    private void generateCppProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "src/main.cpp"),
            "#include <iostream>\nint main() {\n    std::cout << \"" + name + " started!\" << std::endl;\n"
            + "    std::cout << \"" + escJson(desc) + "\" << std::endl;\n    return 0;\n}\n");
        writeFile(new File(dir, "Makefile"),
            "CXX = g++\nCXXFLAGS = -std=c++17 -Wall\nTARGET = " + name.toLowerCase() + "\nSRC = src/main.cpp\n"
            + "all: $(TARGET)\n$(TARGET): $(SRC)\n\t$(CXX) $(CXXFLAGS) -o $(TARGET) $(SRC)\nclean:\n\trm -f $(TARGET)\n");
    }

    private void generateCsharpProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "Program.cs"),
            "using System;\n\nnamespace " + name.replaceAll("[^a-zA-Z0-9]", "") + " {\n"
            + "    class Program {\n        static void Main(string[] args) {\n"
            + "            Console.WriteLine(\"" + name + " started!\");\n        }\n    }\n}\n");
        writeFile(new File(dir, name.toLowerCase() + ".csproj"),
            "<Project Sdk=\"Microsoft.NET.Sdk\">\n  <PropertyGroup>\n"
            + "    <OutputType>Exe</OutputType>\n    <TargetFramework>net8.0</TargetFramework>\n"
            + "  </PropertyGroup>\n</Project>\n");
    }

    private void generateGoProject(File dir, String name, String desc) throws IOException {
        String modName = name.toLowerCase().replaceAll("[^a-z0-9]", "");
        writeFile(new File(dir, "go.mod"), "module " + modName + "\n\ngo 1.21\n");
        writeFile(new File(dir, "main.go"),
            "package main\n\nimport (\"fmt\";\"net/http\")\n\n"
            + "func main() {\n\thttp.HandleFunc(\"/\", func(w http.ResponseWriter, r *http.Request) {\n"
            + "\t\tfmt.Fprintf(w, \"<h1>" + name + "</h1><p>" + escHtml(desc) + "</p>\")\n"
            + "\t})\n\tfmt.Println(\"" + name + " listening on :8080\")\n\thttp.ListenAndServe(\":8080\", nil)\n}\n");
    }

    private void generateRustProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "Cargo.toml"),
            "[package]\nname = \"" + name.toLowerCase().replaceAll("[^a-z0-9_]", "") + "\"\n"
            + "version = \"0.1.0\"\nedition = \"2021\"\n");
        writeFile(new File(dir, "src/main.rs"),
            "fn main() {\n    println!(\"" + name + " started!\");\n    println!(\"" + escJs(desc) + "\");\n}\n");
    }

    private void generateKotlinProject(File dir, String name, String desc) throws IOException {
        new File(dir, "src/main/kotlin").mkdirs();
        writeFile(new File(dir, "src/main/kotlin/Main.kt"),
            "fun main() {\n    println(\"" + name + " started!\")\n}\n");
    }

    private void generateDockerProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "docker-compose.yml"),
            "version: '3.8'\nservices:\n  web:\n    build: .\n    ports:\n      - \"3000:3000\"\n"
            + "    volumes:\n      - .:/app\n    environment:\n      - NODE_ENV=development\n");
        writeFile(new File(dir, "Dockerfile"),
            "FROM node:20-alpine\nWORKDIR /app\nCOPY package*.json ./\nRUN npm install\nCOPY . .\n"
            + "EXPOSE 3000\nCMD [\"npm\", \"start\"]\n");
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"description\": \"" + escJson(desc) + "\",\n"
            + "  \"scripts\": { \"start\": \"node server.js\" },\n"
            + "  \"dependencies\": { \"express\": \"^4.18.2\" }\n}\n");
        writeFile(new File(dir, "server.js"),
            "const express = require('express');\nconst app = express();\n"
            + "app.get('/', (req, res) => res.send('<h1>" + name + "</h1><p>" + escHtml(desc) + "</p>'));\n"
            + "app.listen(3000, () => console.log('Running on port 3000'));\n");
        writeFile(new File(dir, ".dockerignore"), "node_modules\n.git\n*.md\n");
        writeFile(new File(dir, "README.md"), "# " + name + "\n\n" + desc + "\n\n## Run\n`docker-compose up`\n");
    }

    private void generateMernProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"description\": \"" + escJson(desc) + "\",\n"
            + "  \"scripts\": { \"server\": \"cd server && npm start\", \"client\": \"cd client && npm start\",\n"
            + "    \"dev\": \"concurrently \\\"npm run server\\\" \\\"npm run client\\\"\" },\n"
            + "  \"dependencies\": { \"concurrently\": \"^8.2.0\" }\n}\n");
        writeFile(new File(dir, "server/package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "-server\",\n  \"version\": \"1.0.0\",\n"
            + "  \"main\": \"index.js\",\n  \"scripts\": { \"start\": \"node index.js\" },\n"
            + "  \"dependencies\": { \"express\": \"^4.18.2\", \"mongoose\": \"^7.6.0\", \"cors\": \"^2.8.5\" }\n}\n");
        writeFile(new File(dir, "server/index.js"),
            "const express = require('express');\nconst cors = require('cors');\n"
            + "const app = express();\napp.use(cors());\napp.use(express.json());\n\n"
            + "app.get('/api/health', (req, res) => res.json({ status: 'ok' }));\n\n"
            + "const PORT = process.env.PORT || 5000;\napp.listen(PORT, () => console.log('Server running on port ' + PORT));\n");
        writeFile(new File(dir, "client/package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "-client\",\n  \"version\": \"1.0.0\",\n"
            + "  \"scripts\": { \"start\": \"react-scripts start\", \"build\": \"react-scripts build\" },\n"
            + "  \"dependencies\": { \"react\": \"^18.2.0\", \"react-dom\": \"^18.2.0\", \"react-scripts\": \"5.0.1\", \"axios\": \"^1.6.0\" }\n}\n");
        writeFile(new File(dir, "client/src/App.js"),
            "import React, { useState, useEffect } from 'react';\nimport axios from 'axios';\n\n"
            + "function App() {\n  const [msg, setMsg] = useState('');\n"
            + "  useEffect(() => {\n    axios.get('/api/health').then(r => setMsg(r.data.status)).catch(e => setMsg('Error'));\n  }, []);\n\n"
            + "  return (\n    <div>\n      <h1>" + name + "</h1>\n      <p>" + escHtml(desc) + "</p>\n      <p>Server: {msg}</p>\n    </div>\n  );\n}\n\nexport default App;\n");
    }

    // ================================================================
    //  NEW WEB & MOBILE-HYBRID TEMPLATE GENERATORS
    // ================================================================

    private void generatePwaProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "index.html"),
            "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n"
            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "  <meta name=\"theme-color\" content=\"#4A90D9\">\n"
            + "  <link rel=\"manifest\" href=\"manifest.json\">\n"
            + "  <title>" + name + "</title>\n  <link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n"
            + "  <h1>" + name + "</h1>\n  <p>" + escHtml(desc) + "</p>\n"
            + "  <script src=\"app.js\"></script>\n</body>\n</html>\n");
        writeFile(new File(dir, "manifest.json"),
            "{\n  \"name\": \"" + name + "\",\n  \"short_name\": \"" + name + "\",\n"
            + "  \"start_url\": \".\",\n  \"display\": \"standalone\",\n"
            + "  \"background_color\": \"#ffffff\",\n  \"theme_color\": \"#4A90D9\",\n"
            + "  \"description\": \"" + escJson(desc) + "\",\n"
            + "  \"icons\": [{ \"src\": \"icon.png\", \"sizes\": \"192x192\", \"type\": \"image/png\" }]\n}\n");
        writeFile(new File(dir, "sw.js"),
            "const CACHE = \"" + name.toLowerCase() + "-v1\";\n"
            + "self.addEventListener('install', e => {\n"
            + "  e.waitUntil(caches.open(CACHE).then(c => c.addAll(['/','index.html','style.css','app.js'])));\n"
            + "});\nself.addEventListener('fetch', e => {\n"
            + "  e.respondWith(caches.match(e.request).then(r => r || fetch(e.request)));\n"
            + "});\n");
        writeFile(new File(dir, "style.css"),
            "body { font-family: system-ui, sans-serif; max-width: 600px; margin: auto; padding: 2rem; }\n");
        writeFile(new File(dir, "app.js"),
            "if ('serviceWorker' in navigator) {\n"
            + "  navigator.serviceWorker.register('sw.js').catch(console.error);\n}\n");
    }

    private void generateAngularProject(File dir, String name, String desc) throws IOException {
        new File(dir, "src/app").mkdirs();
        writeFile(new File(dir, "angular.json"),
            "{\n  \"$schema\": \"./node_modules/@angular/cli/lib/config/schema.json\",\n"
            + "  \"version\": 1,\n  \"projects\": {\n    \"" + name.toLowerCase() + "\": {\n"
            + "      \"projectType\": \"application\",\n      \"root\": \"\",\n"
            + "      \"sourceRoot\": \"src\",\n"
            + "      \"architect\": {\n        \"build\": { \"builder\": \"@angular-devkit/build-angular:application\",\n"
            + "          \"options\": { \"outputPath\": \"dist\", \"index\": \"src/index.html\",\n"
            + "            \"browser\": \"src/main.ts\", \"polyfills\": [\"zone.js\"] },\n"
            + "          \"configurations\": { \"production\": { \"budgets\": [{\"type\":\"initial\",\"maximumWarning\":\"500kb\"}] } } },\n"
            + "        \"serve\": { \"builder\": \"@angular-devkit/build-angular:dev-server\" } } } } }\n");
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"scripts\": { \"start\": \"ng serve\", \"build\": \"ng build\" },\n"
            + "  \"dependencies\": { \"@angular/core\": \"^17.0.0\", \"@angular/platform-browser\": \"^17.0.0\",\n"
            + "    \"@angular/platform-browser-dynamic\": \"^17.0.0\", \"@angular/common\": \"^17.0.0\",\n"
            + "    \"@angular/compiler\": \"^17.0.0\", \"@angular/router\": \"^17.0.0\",\n"
            + "    \"zone.js\": \"^0.14.0\", \"rxjs\": \"^7.8.0\" },\n"
            + "  \"devDependencies\": { \"@angular/cli\": \"^17.0.0\", \"@angular/compiler-cli\": \"^17.0.0\",\n"
            + "    \"typescript\": \"~5.2.0\" }\n}\n");
        writeFile(new File(dir, "tsconfig.json"),
            "{\n  \"compileOnSave\": false,\n  \"compilerOptions\": {\n"
            + "    \"baseUrl\": \"./\",\n    \"outDir\": \"./dist\",\n"
            + "    \"forceConsistentCasingInFileNames\": true,\n    \"strict\": true,\n"
            + "    \"noImplicitOverride\": true,\n    \"noPropertyAccessFromIndexSignature\": true,\n"
            + "    \"noImplicitReturns\": true,\n    \"noFallthroughCasesInSwitch\": true,\n"
            + "    \"sourceMap\": true,\n    \"declaration\": false,\n"
            + "    \"downlevelIteration\": true,\n    \"experimentalDecorators\": true,\n"
            + "    \"moduleResolution\": \"node\",\n    \"importHelpers\": true,\n"
            + "    \"target\": \"ES2022\",\n    \"module\": \"ES2022\",\n"
            + "    \"lib\": [\"ES2022\", \"dom\"]\n  },\n"
            + "  \"angularCompilerOptions\": {\n    \"enableI18nLegacyMessageIdFormat\": false,\n"
            + "    \"strictInjectionParameters\": true,\n"
            + "    \"strictInputAccessModifiers\": true,\n"
            + "    \"strictTemplates\": true\n  }\n}\n");
        writeFile(new File(dir, "src/index.html"),
            "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n"
            + "  <base href=\"/\">\n  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "  <title>" + name + "</title>\n</head>\n<body>\n  <app-root></app-root>\n</body>\n</html>\n");
        writeFile(new File(dir, "src/main.ts"),
            "import { bootstrapApplication } from '@angular/platform-browser-dynamic';\n"
            + "import { AppComponent } from './app/app.component';\n"
            + "bootstrapApplication(AppComponent);\n");
        writeFile(new File(dir, "src/app/app.component.ts"),
            "import { Component } from '@angular/core';\n\n@Component({\n"
            + "  selector: 'app-root',\n  standalone: true,\n"
            + "  template: '<h1>" + name + "</h1><p>" + escHtml(desc) + "</p>'\n})\n"
            + "export class AppComponent {}\n");
    }

    private void generateSvelteProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"scripts\": { \"start\": \"vite\", \"build\": \"vite build\" },\n"
            + "  \"dependencies\": { \"svelte\": \"^4.2.0\" },\n"
            + "  \"devDependencies\": { \"@sveltejs/vite-plugin-svelte\": \"^3.0.0\", \"vite\": \"^5.0.0\" }\n}\n");
        writeFile(new File(dir, "vite.config.js"),
            "import { defineConfig } from 'vite';\nimport { svelte } from '@sveltejs/vite-plugin-svelte';\n"
            + "export default defineConfig({ plugins: [svelte()] });\n");
        writeFile(new File(dir, "src/main.js"),
            "import App from './App.svelte';\nconst app = new App({ target: document.body });\nexport default app;\n");
        writeFile(new File(dir, "src/App.svelte"),
            "<h1>" + name + "</h1>\n<p>" + escHtml(desc) + "</p>\n\n<style>\n  :global(body) { font-family: system-ui, sans-serif; max-width: 600px; margin: auto; padding: 2rem; }\n</style>\n");
        writeFile(new File(dir, "index.html"),
            "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n"
            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "  <title>" + name + "</title>\n</head>\n<body>\n  <script src=\"/src/main.js\" type=\"module\"></script>\n</body>\n</html>\n");
    }

    private void generateSolidJsProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"scripts\": { \"start\": \"vite\", \"build\": \"vite build\" },\n"
            + "  \"dependencies\": { \"solid-js\": \"^1.8.0\" },\n"
            + "  \"devDependencies\": { \"vite\": \"^5.0.0\", \"vite-plugin-solid\": \"^2.8.0\" }\n}\n");
        writeFile(new File(dir, "vite.config.js"),
            "import { defineConfig } from 'vite';\nimport solid from 'vite-plugin-solid';\n"
            + "export default defineConfig({ plugins: [solid()] });\n");
        writeFile(new File(dir, "index.html"),
            "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n"
            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "  <title>" + name + "</title>\n</head>\n<body>\n  <div id=\"root\"></div>\n  <script src=\"/src/index.jsx\" type=\"module\"></script>\n</body>\n</html>\n");
        writeFile(new File(dir, "src/index.jsx"),
            "import { render } from 'solid-js/web';\nimport App from './App';\n"
            + "render(() => <App />, document.getElementById('root'));\n");
        writeFile(new File(dir, "src/App.jsx"),
            "import { createSignal } from 'solid-js';\n\n"
            + "function App() {\n  const [count, setCount] = createSignal(0);\n"
            + "  return (\n    <div>\n      <h1>" + name + "</h1>\n"
            + "      <p>" + escHtml(desc) + "</p>\n"
            + "      <button onClick={() => setCount(c => c+1)}>Count: {count()}</button>\n    </div>\n  );\n}\n\nexport default App;\n");
    }

    private void generatePreactProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"scripts\": { \"start\": \"vite\", \"build\": \"vite build\" },\n"
            + "  \"dependencies\": { \"preact\": \"^10.19.0\" },\n"
            + "  \"devDependencies\": { \"@preact/preset-vite\": \"^2.8.0\", \"vite\": \"^5.0.0\" }\n}\n");
        writeFile(new File(dir, "vite.config.js"),
            "import { defineConfig } from 'vite';\nimport preact from '@preact/preset-vite';\n"
            + "export default defineConfig({ plugins: [preact()] });\n");
        writeFile(new File(dir, "index.html"),
            "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n"
            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "  <title>" + name + "</title>\n</head>\n<body>\n  <div id=\"root\"></div>\n  <script src=\"/src/index.jsx\" type=\"module\"></script>\n</body>\n</html>\n");
        writeFile(new File(dir, "src/index.jsx"),
            "import { render } from 'preact';\nimport App from './App';\n"
            + "render(<App />, document.getElementById('root'));\n");
        writeFile(new File(dir, "src/App.jsx"),
            "import { useState } from 'preact/hooks';\n\n"
            + "function App() {\n  const [count, setCount] = useState(0);\n"
            + "  return (\n    <div>\n      <h1>" + name + "</h1>\n"
            + "      <p>" + escHtml(desc) + "</p>\n"
            + "      <button onClick={() => setCount(c => c+1)}>Count: {count}</button>\n    </div>\n  );\n}\n\nexport default App;\n");
    }

    private void generateLitProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"scripts\": { \"start\": \"web-dev-server\" },\n"
            + "  \"dependencies\": { \"lit\": \"^3.1.0\" },\n"
            + "  \"devDependencies\": { \"@web/dev-server\": \"^0.3.0\" }\n}\n");
        writeFile(new File(dir, "index.html"),
            "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n"
            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "  <title>" + name + "</title>\n"
            + "  <script type=\"module\" src=\"/src/my-app.js\"></script>\n</head>\n<body>\n  <my-app></my-app>\n</body>\n</html>\n");
        writeFile(new File(dir, "src/my-app.js"),
            "import { LitElement, html, css } from 'lit';\n\n"
            + "class MyApp extends LitElement {\n"
            + "  static styles = css` :host { display: block; max-width: 600px; margin: auto; padding: 2rem; }`;\n"
            + "  render() {\n    return html`<h1>" + name + "</h1><p>" + escHtml(desc) + "</p>`;\n  }\n}\n"
            + "customElements.define('my-app', MyApp);\n");
    }

    private void generateQwikProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"scripts\": { \"start\": \"vite\", \"build\": \"vite build\" },\n"
            + "  \"dependencies\": { \"@builder.io/qwik\": \"^1.4.0\" },\n"
            + "  \"devDependencies\": { \"vite\": \"^5.0.0\", \"vite-plugin-qwik\": \"^1.4.0\" }\n}\n");
        writeFile(new File(dir, "vite.config.js"),
            "import { defineConfig } from 'vite';\nimport qwik from 'vite-plugin-qwik';\n"
            + "export default defineConfig({ plugins: [qwik()] });\n");
        writeFile(new File(dir, "index.html"),
            "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n"
            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "  <title>" + name + "</title>\n</head>\n<body>\n  <div id=\"root\"></div>\n  <script src=\"/src/main.jsx\" type=\"module\"></script>\n</body>\n</html>\n");
        writeFile(new File(dir, "src/main.jsx"),
            "import { render } from '@builder.io/qwik';\nimport App from './App';\n"
            + "render(document.getElementById('root'), <App />);\n");
        writeFile(new File(dir, "src/App.jsx"),
            "import { component$ } from '@builder.io/qwik';\n\n"
            + "export default component$(() => {\n"
            + "  return <div><h1>" + name + "</h1><p>" + escHtml(desc) + "</p></div>;\n});\n");
    }

    private void generateNuxtProject(File dir, String name, String desc) throws IOException {
        new File(dir, "pages").mkdirs();
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"scripts\": { \"start\": \"nuxt dev\", \"build\": \"nuxt build\" },\n"
            + "  \"dependencies\": { \"nuxt\": \"^3.8.0\", \"vue\": \"^3.3.0\" },\n"
            + "  \"devDependencies\": { \"@nuxt/devtools\": \"^1.0.0\" }\n}\n");
        writeFile(new File(dir, "nuxt.config.ts"),
            "export default defineNuxtConfig({\n  devtools: { enabled: true }\n});\n");
        writeFile(new File(dir, "app.vue"),
            "<template>\n  <div>\n    <h1>" + name + "</h1>\n    <p>" + escHtml(desc) + "</p>\n  </div>\n</template>\n");
        writeFile(new File(dir, "pages/index.vue"),
            "<template>\n  <div>\n    <h1>Welcome to " + name + "</h1>\n    <NuxtLink to=\"/about\">About</NuxtLink>\n  </div>\n</template>\n");
        writeFile(new File(dir, "pages/about.vue"),
            "<template>\n  <div>\n    <h1>About</h1>\n    <p>" + escHtml(desc) + "</p>\n  </div>\n</template>\n");
    }

    private void generateSvelteKitProject(File dir, String name, String desc) throws IOException {
        new File(dir, "src/routes").mkdirs();
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"scripts\": { \"start\": \"vite dev\", \"build\": \"vite build\", \"preview\": \"vite preview\" },\n"
            + "  \"dependencies\": { \"@sveltejs/kit\": \"^2.0.0\", \"svelte\": \"^4.2.0\" },\n"
            + "  \"devDependencies\": { \"@sveltejs/vite-plugin-svelte\": \"^3.0.0\", \"vite\": \"^5.0.0\" }\n}\n");
        writeFile(new File(dir, "svelte.config.js"),
            "import adapter from '@sveltejs/adapter-auto';\n\n"
            + "export default {\n  kit: { adapter: adapter() }\n};\n");
        writeFile(new File(dir, "vite.config.js"),
            "import { sveltekit } from '@sveltejs/kit/vite';\n"
            + "export default { plugins: [sveltekit()] };\n");
        writeFile(new File(dir, "src/app.html"),
            "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n"
            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "  %sveltekit.head%\n</head>\n<body>\n  <div>%sveltekit.body%</div>\n</body>\n</html>\n");
        writeFile(new File(dir, "src/routes/+page.svelte"),
            "<h1>" + name + "</h1>\n<p>" + escHtml(desc) + "</p>\n\n"
            + "<style>\n  :global(body) { font-family: system-ui, sans-serif; max-width: 600px; margin: auto; padding: 2rem; }\n</style>\n");
    }

    private void generateRemixProject(File dir, String name, String desc) throws IOException {
        new File(dir, "app/routes").mkdirs();
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"scripts\": { \"start\": \"remix dev\", \"build\": \"remix build\" },\n"
            + "  \"dependencies\": { \"@remix-run/node\": \"^2.4.0\", \"@remix-run/react\": \"^2.4.0\",\n"
            + "    \"@remix-run/serve\": \"^2.4.0\", \"react\": \"^18.2.0\", \"react-dom\": \"^18.2.0\" },\n"
            + "  \"devDependencies\": { \"@remix-run/dev\": \"^2.4.0\", \"typescript\": \"^5.2.0\" }\n}\n");
        writeFile(new File(dir, "remix.config.js"),
            "export default {\n  ignoredRouteFiles: ['**/.*'],\n  appDirectory: 'app',\n};\n");
        writeFile(new File(dir, "app/root.tsx"),
            "import { Links, Meta, Outlet, Scripts } from '@remix-run/react';\n\n"
            + "export default function Root() {\n  return (\n    <html lang=\"en\">\n"
            + "      <head><Meta /><Links /></head>\n"
            + "      <body><h1>" + name + "</h1><Outlet /><Scripts /></body>\n"
            + "    </html>\n  );\n}\n");
        writeFile(new File(dir, "app/routes/_index.tsx"),
            "import type { MetaFunction } from '@remix-run/node';\n\n"
            + "export const meta: MetaFunction = () => [{ title: '" + name + "' }];\n\n"
            + "export default function Index() {\n"
            + "  return <p>" + escHtml(desc) + "</p>;\n}\n");
    }

    private void generateAstroProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"scripts\": { \"start\": \"astro dev\", \"build\": \"astro build\", \"preview\": \"astro preview\" },\n"
            + "  \"dependencies\": { \"astro\": \"^4.0.0\" }\n}\n");
        writeFile(new File(dir, "astro.config.mjs"),
            "import { defineConfig } from 'astro/config';\n\nexport default defineConfig({});\n");
        writeFile(new File(dir, "src/pages/index.astro"),
            "---\nconst title = '" + name + "';\nconst desc = '" + escHtml(desc) + "';\n---\n"
            + "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n"
            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "  <title>{title}</title>\n</head>\n<body>\n  <h1>{title}</h1>\n  <p>{desc}</p>\n</body>\n</html>\n");
        writeFile(new File(dir, "src/pages/about.astro"),
            "---\n---\n<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n"
            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "  <title>About</title>\n</head>\n<body>\n  <h1>About " + name + "</h1>\n  <p>" + escHtml(desc) + "</p>\n</body>\n</html>\n");
    }

    private void generateGatsbyProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"scripts\": { \"start\": \"gatsby develop\", \"build\": \"gatsby build\" },\n"
            + "  \"dependencies\": { \"gatsby\": \"^5.12.0\", \"react\": \"^18.2.0\", \"react-dom\": \"^18.2.0\" },\n"
            + "  \"devDependencies\": { \"gatsby-cli\": \"^5.12.0\" }\n}\n");
        writeFile(new File(dir, "gatsby-config.js"),
            "module.exports = {\n  siteMetadata: {\n    title: '" + name + "',\n    description: '" + escJs(desc) + "'\n  },\n  plugins: []\n};\n");
        writeFile(new File(dir, "src/pages/index.js"),
            "import React from 'react';\n\n"
            + "export default function Home() {\n  return (\n    <div>\n"
            + "      <h1>" + name + "</h1>\n      <p>" + escHtml(desc) + "</p>\n    </div>\n  );\n}\n");
        writeFile(new File(dir, "src/pages/about.js"),
            "import React from 'react';\n\n"
            + "export default function About() {\n  return (\n    <div>\n"
            + "      <h1>About</h1>\n      <p>" + escHtml(desc) + "</p>\n    </div>\n  );\n}\n");
    }

    private void generateDocusaurusProject(File dir, String name, String desc) throws IOException {
        new File(dir, "docs").mkdirs();
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"scripts\": { \"start\": \"docusaurus start\", \"build\": \"docusaurus build\" },\n"
            + "  \"dependencies\": { \"@docusaurus/core\": \"^3.0.0\", \"@docusaurus/preset-classic\": \"^3.0.0\",\n"
            + "    \"react\": \"^18.2.0\", \"react-dom\": \"^18.2.0\" }\n}\n");
        writeFile(new File(dir, "docusaurus.config.js"),
            "module.exports = {\n  title: '" + name + "',\n  tagline: '" + escJs(desc) + "',\n"
            + "  presets: [['@docusaurus/preset-classic', { docs: { sidebarPath: false } }]]\n};\n");
        writeFile(new File(dir, "src/pages/index.js"),
            "import React from 'react';\n\n"
            + "export default function Home() {\n  return (\n    <div>\n"
            + "      <h1>" + name + "</h1>\n      <p>" + escHtml(desc) + "</p>\n    </div>\n  );\n}\n");
        writeFile(new File(dir, "docs/intro.md"),
            "# Welcome to " + name + "\n\n" + desc + "\n");
        writeFile(new File(dir, "README.md"), "# " + name + "\n\n" + desc + "\n");
    }

    // ── Mobile Hybrid Templates ──

    private void generateCordovaProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"displayName\": \"" + name + "\",\n"
            + "  \"description\": \"" + escJson(desc) + "\",\n"
            + "  \"scripts\": { \"start\": \"cordova run android\", \"build\": \"cordova build android\" },\n"
            + "  \"dependencies\": { \"cordova\": \"^12.0.0\", \"cordova-android\": \"^10.1.0\" }\n}\n");
        writeFile(new File(dir, "config.xml"),
            "<?xml version='1.0' encoding='utf-8'?>\n"
            + "<widget id=\"com." + name.toLowerCase().replaceAll("[^a-z0-9]", "") + ".app\" version=\"1.0.0\"\n"
            + "        xmlns=\"http://www.w3.org/ns/widgets\" xmlns:cdv=\"http://cordova.apache.org/ns/1.0\">\n"
            + "  <name>" + name + "</name>\n"
            + "  <description>" + escHtml(desc) + "</description>\n"
            + "  <content src=\"index.html\" />\n"
            + "  <access origin=\"*\" />\n"
            + "  <preference name=\"Fullscreen\" value=\"true\" />\n"
            + "  <preference name=\"orientation\" value=\"portrait\" />\n"
            + "</widget>\n");
        writeFile(new File(dir, "www/index.html"),
            "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n"
            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">\n"
            + "  <title>" + name + "</title>\n  <link rel=\"stylesheet\" href=\"css/style.css\">\n"
            + "  <script src=\"cordova.js\"></script>\n</head>\n<body>\n"
            + "  <div id=\"app\">\n    <h1>" + name + "</h1>\n    <p>" + escHtml(desc) + "</p>\n"
            + "    <button id=\"btn\">Tap Me (Cordova)</button>\n  </div>\n"
            + "  <script src=\"js/app.js\"></script>\n</body>\n</html>\n");
        writeFile(new File(dir, "www/css/style.css"),
            "* { margin: 0; padding: 0; box-sizing: border-box; }\n"
            + "body { font-family: system-ui, sans-serif; background: #121212; color: #fff; display: flex; justify-content: center; align-items: center; min-height: 100vh; }\n"
            + "#app { text-align: center; padding: 2rem; }\n"
            + "button { background: #4A90D9; color: #fff; border: none; padding: 12px 24px; border-radius: 8px; font-size: 16px; margin-top: 1rem; }\n");
        writeFile(new File(dir, "www/js/app.js"),
            "document.addEventListener('deviceready', function() {\n"
            + "  document.getElementById('btn').addEventListener('click', function() {\n"
            + "    alert('" + name + " on Cordova is ready!');\n  });\n}, false);\n");
    }

    private void generateIonicProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"description\": \"" + escJson(desc) + "\",\n"
            + "  \"scripts\": { \"start\": \"ionic serve\", \"build\": \"ionic build\", \"android\": \"ionic capacitor run android\" },\n"
            + "  \"dependencies\": { \"@ionic/angular\": \"^7.5.0\", \"@capacitor/core\": \"^5.6.0\" },\n"
            + "  \"devDependencies\": { \"@capacitor/cli\": \"^5.6.0\", \"@angular/cli\": \"^17.0.0\" }\n}\n");
        writeFile(new File(dir, "ionic.config.json"),
            "{\n  \"name\": \"" + name + "\",\n  \"type\": \"angular\",\n  \"integrations\": { \"capacitor\": {} }\n}\n");
        writeFile(new File(dir, "src/app/app.component.ts"),
            "import { Component } from '@angular/core';\n\n@Component({\n  selector: 'app-root',\n  template: '<ion-app><ion-router-outlet></ion-router-outlet></ion-app>'\n})\nexport class AppComponent {}\n");
        writeFile(new File(dir, "src/app/home/home.page.html"),
            "<ion-header><ion-toolbar><ion-title>" + name + "</ion-title></ion-toolbar></ion-header>\n"
            + "<ion-content class=\"ion-padding\">\n  <h1>" + name + "</h1>\n  <p>" + escHtml(desc) + "</p>\n"
            + "  <ion-button expand=\"block\" color=\"primary\">Get Started</ion-button>\n</ion-content>\n");
    }

    private void generateCapacitorProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"description\": \"" + escJson(desc) + "\",\n"
            + "  \"scripts\": {\n"
            + "    \"start\": \"vite\",\n    \"build\": \"vite build\",\n"
            + "    \"android\": \"npx cap run android\",\n    \"sync\": \"npx cap sync\"\n  },\n"
            + "  \"dependencies\": { \"@capacitor/core\": \"^5.6.0\" },\n"
            + "  \"devDependencies\": { \"@capacitor/cli\": \"^5.6.0\", \"vite\": \"^5.0.0\" }\n}\n");
        writeFile(new File(dir, "capacitor.config.json"),
            "{\n  \"appId\": \"com." + name.toLowerCase().replaceAll("[^a-z0-9]", "") + ".app\",\n"
            + "  \"appName\": \"" + name + "\",\n  \"webDir\": \"dist\",\n"
            + "  \"bundledWebRuntime\": false\n}\n");
        writeFile(new File(dir, "index.html"),
            "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n"
            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "  <title>" + name + "</title>\n</head>\n<body>\n  <div id=\"root\">\n"
            + "    <h1>" + name + "</h1>\n    <p>" + escHtml(desc) + "</p>\n  </div>\n  <script src=\"/src/main.js\" type=\"module\"></script>\n</body>\n</html>\n");
        writeFile(new File(dir, "vite.config.js"),
            "import { defineConfig } from 'vite';\nexport default defineConfig({});\n");
        writeFile(new File(dir, "src/main.js"),
            "console.log('" + name + " running on Capacitor JS!');\n");
    }

    private void generateReactNativeWebProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"description\": \"" + escJson(desc) + "\",\n"
            + "  \"scripts\": { \"start\": \"webpack serve\", \"build\": \"webpack build\" },\n"
            + "  \"dependencies\": { \"react\": \"^18.2.0\", \"react-dom\": \"^18.2.0\",\n"
            + "    \"react-native-web\": \"^0.19.0\" },\n"
            + "  \"devDependencies\": { \"webpack\": \"^5.89.0\", \"webpack-cli\": \"^5.1.0\",\n"
            + "    \"webpack-dev-server\": \"^4.15.0\", \"babel-loader\": \"^9.1.0\",\n"
            + "    \"@babel/core\": \"^7.23.0\", \"@babel/preset-env\": \"^7.23.0\",\n"
            + "    \"@babel/preset-react\": \"^7.22.0\", \"html-webpack-plugin\": \"^5.5.0\" }\n}\n");
        writeFile(new File(dir, "webpack.config.js"),
            "const path = require('path');\nconst HtmlPlugin = require('html-webpack-plugin');\n"
            + "module.exports = {\n  mode: 'development',\n  entry: './src/index.js',\n"
            + "  output: { path: path.resolve(__dirname, 'dist'), filename: 'bundle.js' },\n"
            + "  module: { rules: [{ test: /\\.jsx?$/, exclude: /node_modules/, use: 'babel-loader' }] },\n"
            + "  plugins: [new HtmlPlugin({ template: './public/index.html' })],\n"
            + "  devServer: { port: 3000, hot: true }\n};\n");
        writeFile(new File(dir, "public/index.html"),
            "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n"
            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "  <title>" + name + "</title>\n</head>\n<body>\n  <div id=\"root\"></div>\n</body>\n</html>\n");
        writeFile(new File(dir, "src/index.js"),
            "import React from 'react';\nimport { createRoot } from 'react-dom/client';\n"
            + "import App from './App';\ncreateRoot(document.getElementById('root')).render(<App />);\n");
        writeFile(new File(dir, "src/App.js"),
            "import React from 'react';\n"
            + "import { View, Text, StyleSheet } from 'react-native-web';\n\n"
            + "const styles = StyleSheet.create({\n  container: { flex: 1, justifyContent: 'center', alignItems: 'center', padding: 20 },\n  title: { fontSize: 24, fontWeight: 'bold' },\n  desc: { fontSize: 14, color: '#666', marginTop: 8 }\n});\n\n"
            + "export default function App() {\n"
            + "  return (\n    <View style={styles.container}>\n"
            + "      <Text style={styles.title}>" + name + "</Text>\n"
            + "      <Text style={styles.desc}>" + escHtml(desc) + "</Text>\n    </View>\n  );\n}\n");
    }

    // ── Desktop Hybrid Templates ──

    private void generateElectronProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"description\": \"" + escJson(desc) + "\",\n"
            + "  \"main\": \"main.js\",\n"
            + "  \"scripts\": { \"start\": \"electron .\", \"build\": \"electron-builder\" },\n"
            + "  \"dependencies\": { \"electron\": \"^28.0.0\" },\n"
            + "  \"devDependencies\": { \"electron-builder\": \"^24.0.0\" }\n}\n");
        writeFile(new File(dir, "main.js"),
            "const { app, BrowserWindow } = require('electron');\n\n"
            + "function createWindow() {\n  const win = new BrowserWindow({\n"
            + "    width: 1024, height: 768,\n    webPreferences: { nodeIntegration: true }\n"
            + "  });\n  win.loadFile('index.html');\n}\n\napp.whenReady().then(createWindow);\n"
            + "app.on('window-all-closed', () => { if (process.platform !== 'darwin') app.quit(); });\n");
        writeFile(new File(dir, "index.html"),
            "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n"
            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "  <title>" + name + "</title>\n  <link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n"
            + "  <div id=\"app\">\n    <h1>" + name + "</h1>\n    <p>" + escHtml(desc) + "</p>\n"
            + "    <button id=\"infoBtn\">Electron Info</button>\n  </div>\n"
            + "  <script src=\"renderer.js\"></script>\n</body>\n</html>\n");
        writeFile(new File(dir, "renderer.js"),
            "document.getElementById('infoBtn').addEventListener('click', () => {\n"
            + "  const { versions } = require('electron');\n"
            + "  alert('Node: ' + versions.node + '\\nChrome: ' + versions.chrome + '\\nElectron: ' + versions.electron);\n});\n");
        writeFile(new File(dir, "style.css"),
            "body { font-family: system-ui, sans-serif; display: flex; justify-content: center; align-items: center; min-height: 100vh; margin: 0; background: #1a1a2e; color: #eee; }\n"
            + "#app { text-align: center; padding: 2rem; }\n"
            + "button { background: #4A90D9; color: #fff; border: none; padding: 10px 20px; border-radius: 6px; cursor: pointer; }\n");
    }

    private void generateTauriProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"description\": \"" + escJson(desc) + "\",\n"
            + "  \"scripts\": { \"start\": \"vite\", \"build\": \"vite build\", \"tauri\": \"tauri\" },\n"
            + "  \"dependencies\": { \"react\": \"^18.2.0\", \"react-dom\": \"^18.2.0\", \"@tauri-apps/api\": \"^1.5.0\" },\n"
            + "  \"devDependencies\": { \"vite\": \"^5.0.0\", \"@tauri-apps/cli\": \"^1.5.0\" }\n}\n");
        writeFile(new File(dir, "src-tauri/Cargo.toml"),
            "[package]\nname = \"" + name.toLowerCase().replaceAll("[^a-z0-9_]", "") + "\"\n"
            + "version = \"0.1.0\"\nedition = \"2021\"\n"
            + "[build-dependencies]\ntauri-build = { version = \"1.5\", features = [] }\n"
            + "[dependencies]\ntauri = { version = \"1.5\", features = [\"shell-open\"] }\nserde = { version = \"1.0\", features = [\"derive\"] }\nserde_json = \"1.0\"\n");
        writeFile(new File(dir, "index.html"),
            "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n"
            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "  <title>" + name + "</title>\n</head>\n<body>\n  <div id=\"root\">\n"
            + "    <h1>" + name + "</h1>\n    <p>" + escHtml(desc) + "</p>\n  </div>\n"
            + "  <script src=\"/src/main.jsx\" type=\"module\"></script>\n</body>\n</html>\n");
        writeFile(new File(dir, "vite.config.js"),
            "import { defineConfig } from 'vite';\nexport default defineConfig({\n  clearScreen: false, server: { port: 1420, strictPort: true }\n});\n");
        writeFile(new File(dir, "src/main.jsx"),
            "import React from 'react';\nimport { createRoot } from 'react-dom/client';\n"
            + "import App from './App';\ncreateRoot(document.getElementById('root')).render(<App />);\n");
        writeFile(new File(dir, "src/App.jsx"),
            "import React from 'react';\n\n"
            + "function App() {\n  return (\n    <div>\n"
            + "      <h1>" + name + "</h1>\n      <p>" + escHtml(desc) + "</p>\n    </div>\n  );\n}\n\nexport default App;\n");
    }

    // ── Special Templates ──

    private void generateFlutterWebProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "pubspec.yaml"),
            "name: " + name.toLowerCase().replaceAll("[^a-z0-9_]", "") + "\n"
            + "description: " + desc + "\nversion: 1.0.0\n\n"
            + "environment:\n  sdk: '>=3.0.0 <4.0.0'\n\n"
            + "dependencies:\n  flutter:\n    sdk: flutter\n  cupertino_icons: ^1.0.2\n\n"
            + "dev_dependencies:\n  flutter_test:\n    sdk: flutter\n\n"
            + "flutter:\n  uses-material-design: true\n");
        writeFile(new File(dir, "lib/main.dart"),
            "import 'package:flutter/material.dart';\n\n"
            + "void main() => runApp(const MyApp());\n\n"
            + "class MyApp extends StatelessWidget {\n  const MyApp({super.key});\n\n"
            + "  @override\n  Widget build(BuildContext context) {\n    return MaterialApp(\n"
            + "      title: '" + name + "',\n      home: Scaffold(\n"
            + "        appBar: AppBar(title: Text('" + name + "')),\n"
            + "        body: Center(child: Text('" + escJs(desc) + "')),\n"
            + "      ),\n    );\n  }\n}\n");
        writeFile(new File(dir, "web/index.html"),
            "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n"
            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "  <title>" + name + "</title>\n  <script defer src=\"flutter.js\"></script>\n</head>\n<body>\n  <script>\n"
            + "    window.addEventListener('load', function() { _flutter.loader.load(); });\n"
            + "  </script>\n</body>\n</html>\n");
    }

    // ================================================================
    //  NEW TEMPLATE GENERATORS (Vite, fullstack, mobile hybrid, etc.)
    // ================================================================

    private void generateViteVueProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"private\": true,\n  \"description\": \"" + escJson(desc) + "\",\n"
            + "  \"scripts\": { \"dev\": \"vite\", \"build\": \"vite build\", \"preview\": \"vite preview\" },\n"
            + "  \"dependencies\": { \"vue\": \"^3.4.0\" },\n"
            + "  \"devDependencies\": { \"@vitejs/plugin-vue\": \"^5.0.0\", \"vite\": \"^5.0.0\" }\n}\n");
        writeFile(new File(dir, "vite.config.js"),
            "import { defineConfig } from 'vite';\nimport vue from '@vitejs/plugin-vue';\n"
            + "export default defineConfig({ plugins: [vue()] });\n");
        writeFile(new File(dir, "index.html"),
            "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n"
            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "  <title>" + name + "</title>\n</head>\n<body>\n  <div id=\"app\"></div>\n"
            + "  <script type=\"module\" src=\"/src/main.js\"></script>\n</body>\n</html>\n");
        writeFile(new File(dir, "src/main.js"),
            "import { createApp } from 'vue';\nimport App from './App.vue';\ncreateApp(App).mount('#app');\n");
        writeFile(new File(dir, "src/App.vue"),
            "<script setup>\nconst title = '" + name + "';\nconst desc = '" + escJs(desc) + "';\n</script>\n\n"
            + "<template>\n  <div class=\"app\">\n    <h1>{{ title }}</h1>\n    <p>{{ desc }}</p>\n  </div>\n</template>\n\n"
            + "<style scoped>\n.app { font-family: system-ui, sans-serif; text-align: center; padding: 2rem; }\n"
            + "h1 { color: #42b883; }\n</style>\n");
        writeFile(new File(dir, "README.md"), "# " + name + "\n\n" + desc + "\n\n## Dev\n`npm run dev`\n");
    }

    private void generateViteReactProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"private\": true,\n  \"description\": \"" + escJson(desc) + "\",\n"
            + "  \"scripts\": { \"dev\": \"vite\", \"build\": \"vite build\", \"preview\": \"vite preview\" },\n"
            + "  \"dependencies\": { \"react\": \"^18.2.0\", \"react-dom\": \"^18.2.0\" },\n"
            + "  \"devDependencies\": { \"@vitejs/plugin-react\": \"^4.2.0\", \"vite\": \"^5.0.0\" }\n}\n");
        writeFile(new File(dir, "vite.config.js"),
            "import { defineConfig } from 'vite';\nimport react from '@vitejs/plugin-react';\n"
            + "export default defineConfig({ plugins: [react()] });\n");
        writeFile(new File(dir, "index.html"),
            "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n"
            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "  <title>" + name + "</title>\n</head>\n<body>\n  <div id=\"root\"></div>\n"
            + "  <script type=\"module\" src=\"/src/main.jsx\"></script>\n</body>\n</html>\n");
        writeFile(new File(dir, "src/main.jsx"),
            "import React from 'react';\nimport ReactDOM from 'react-dom/client';\n"
            + "import App from './App.jsx';\nReactDOM.createRoot(document.getElementById('root')).render(<React.StrictMode><App /></React.StrictMode>);\n");
        writeFile(new File(dir, "src/App.jsx"),
            "import './App.css';\n\nfunction App() {\n  return (\n    <div className=\"app\">\n"
            + "      <h1>" + name + "</h1>\n      <p>" + escHtml(desc) + "</p>\n    </div>\n  );\n}\n\nexport default App;\n");
        writeFile(new File(dir, "src/App.css"),
            ".app { font-family: system-ui, sans-serif; text-align: center; padding: 2rem; }\n");
        writeFile(new File(dir, "README.md"), "# " + name + "\n\n" + desc + "\n\n## Dev\n`npm run dev`\n");
    }

    private void generateViteTypescriptProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"private\": true,\n  \"description\": \"" + escJson(desc) + "\",\n"
            + "  \"scripts\": { \"dev\": \"vite\", \"build\": \"vite build\", \"preview\": \"vite preview\" },\n"
            + "  \"devDependencies\": { \"typescript\": \"^5.3.0\", \"vite\": \"^5.0.0\" }\n}\n");
        writeFile(new File(dir, "vite.config.js"),
            "import { defineConfig } from 'vite';\nexport default defineConfig({});\n");
        writeFile(new File(dir, "tsconfig.json"),
            "{\n  \"compilerOptions\": {\n    \"target\": \"ES2020\",\n"
            + "    \"module\": \"ESNext\",\n    \"moduleResolution\": \"bundler\",\n"
            + "    \"strict\": true,\n    \"esModuleInterop\": true,\n"
            + "    \"skipLibCheck\": true,\n    \"forceConsistentCasingInFileNames\": true,\n"
            + "    \"outDir\": \"./dist\",\n    \"sourceMap\": true\n  },\n"
            + "  \"include\": [\"src\"]\n}\n");
        writeFile(new File(dir, "index.html"),
            "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n"
            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "  <title>" + name + "</title>\n</head>\n<body>\n  <div id=\"app\"></div>\n"
            + "  <script type=\"module\" src=\"/src/main.ts\"></script>\n</body>\n</html>\n");
        writeFile(new File(dir, "src/main.ts"),
            "const app: HTMLDivElement = document.getElementById('app') as HTMLDivElement;\n"
            + "app.innerHTML = `<h1>" + name + "</h1><p>" + escHtml(desc) + "</p>`;\n");
        writeFile(new File(dir, "README.md"), "# " + name + "\n\n" + desc + "\n\n## Dev\n`npm run dev`\n");
    }

    private void generateAlpineJsProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "index.html"),
            "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n"
            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "  <title>" + name + "</title>\n"
            + "  <script defer src=\"https://cdn.jsdelivr.net/npm/alpinejs@3.x.x/dist/cdn.min.js\"></script>\n"
            + "  <link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n"
            + "  <div x-data=\"{ title: '" + escJs(name) + "', desc: '" + escJs(desc) + "', count: 0 }\" class=\"container\">\n"
            + "    <h1 x-text=\"title\"></h1>\n    <p x-text=\"desc\"></p>\n"
            + "    <button @click=\"count++\" x-text=\"'Clicked ' + count + ' times'\"></button>\n"
            + "  </div>\n  <script src=\"app.js\"></script>\n</body>\n</html>\n");
        writeFile(new File(dir, "style.css"),
            "* { margin:0; padding:0; box-sizing:border-box; }\n"
            + "body { font-family: system-ui, sans-serif; display: flex; justify-content: center; align-items: center; min-height: 100vh; background: #f5f5f5; }\n"
            + ".container { text-align: center; padding: 2rem; background: white; border-radius: 12px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }\n"
            + "h1 { color: #333; margin-bottom: 0.5rem; }\nbutton { margin-top: 1rem; padding: 0.5rem 1.5rem; background: #4f46e5; color: white; border: none; border-radius: 6px; cursor: pointer; }\n");
        writeFile(new File(dir, "app.js"), "// " + name + " - Alpine.js SPA\nconsole.log('Alpine.js ready');\n");
        writeFile(new File(dir, "README.md"), "# " + name + "\n\n" + desc + "\n\n## Usage\nOpen `index.html` in a browser.\n");
    }

    private void generateHtmxExpressProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"description\": \"" + escJson(desc) + "\",\n"
            + "  \"scripts\": { \"start\": \"node server.js\", \"dev\": \"nodemon server.js\" },\n"
            + "  \"dependencies\": { \"express\": \"^4.18.2\", \"cors\": \"^2.8.5\" }\n}\n");
        writeFile(new File(dir, "server.js"),
            "const express = require('express');\nconst path = require('path');\n"
            + "const app = express();\napp.use(express.static('public'));\napp.use(express.json());\n\n"
            + "app.get('/api/hello', (req, res) => {\n"
            + "  res.send('<h2>Hello from " + name + "</h2><p>" + escHtml(desc) + "</p>');\n"
            + "});\n\nconst PORT = process.env.PORT || 3000;\n"
            + "app.listen(PORT, () => console.log('" + name + " on http://localhost:' + PORT));\n");
        writeFile(new File(dir, "public/index.html"),
            "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n"
            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "  <title>" + name + "</title>\n"
            + "  <script src=\"https://unpkg.com/htmx.org@1.9.10\"></script>\n"
            + "  <link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n"
            + "  <div class=\"container\">\n    <h1>" + name + "</h1>\n    <p>" + escHtml(desc) + "</p>\n"
            + "    <button hx-get=\"/api/hello\" hx-target=\"#result\" hx-swap=\"innerHTML\">Load from Server</button>\n"
            + "    <div id=\"result\"></div>\n  </div>\n</body>\n</html>\n");
        writeFile(new File(dir, "public/style.css"),
            "* { margin:0; padding:0; box-sizing:border-box; }\n"
            + "body { font-family: system-ui, sans-serif; background: #f4f4f4; display: flex; justify-content: center; padding-top: 4rem; }\n"
            + ".container { text-align: center; background: white; padding: 2rem; border-radius: 12px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); max-width: 600px; }\n"
            + "button { margin-top: 1rem; padding: 0.6rem 1.5rem; background: #0891b2; color: white; border: none; border-radius: 6px; cursor: pointer; }\n"
            + "#result { margin-top: 1rem; padding: 1rem; background: #f0fdf4; border-radius: 6px; }\n");
        writeFile(new File(dir, "README.md"), "# " + name + "\n\n" + desc + "\n\n## Dev\n`npm install && npm start`\n");
    }

    private void generateStencilProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"description\": \"" + escJson(desc) + "\",\n"
            + "  \"scripts\": { \"start\": \"stencil build --dev --watch --serve\", \"build\": \"stencil build\" },\n"
            + "  \"dependencies\": { \"@stencil/core\": \"^4.7.0\" }\n}\n");
        writeFile(new File(dir, "stencil.config.ts"),
            "import { Config } from '@stencil/core';\n\nexport const config: Config = {\n"
            + "  namespace: '" + name.toLowerCase() + "',\n"
            + "  outputTargets: [{ type: 'www', serviceWorker: null }]\n};\n");
        writeFile(new File(dir, "src/components/my-component/my-component.tsx"),
            "import { Component, Prop, h } from '@stencil/core';\n\n"
            + "@Component({ tag: 'my-component', styleUrl: 'my-component.css', shadow: true })\n"
            + "export class MyComponent {\n  @Prop() name: string = '" + name + "';\n"
            + "  @Prop() desc: string = '" + escJs(desc) + "';\n\n"
            + "  render() {\n    return <div><h1>{this.name}</h1><p>{this.desc}</p></div>;\n  }\n}\n");
        writeFile(new File(dir, "src/components/my-component/my-component.css"),
            ":host { display: block; font-family: system-ui, sans-serif; padding: 1rem; }\n"
            + "h1 { color: #646cff; }\n");
        writeFile(new File(dir, "src/index.html"),
            "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n"
            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "  <title>" + name + "</title>\n  <script type=\"module\" src=\"/build/" + name.toLowerCase() + ".esm.js\"></script>\n"
            + "</head>\n<body>\n  <my-component></my-component>\n</body>\n</html>\n");
        writeFile(new File(dir, "README.md"), "# " + name + "\n\n" + desc + "\n\n## Dev\n`npm install && npm start`\n");
    }

    private void generateWebComponentsProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "index.html"),
            "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n"
            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "  <title>" + name + "</title>\n  <script src=\"src/my-app.js\" type=\"module\"></script>\n"
            + "  <link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n  <my-app></my-app>\n</body>\n</html>\n");
        writeFile(new File(dir, "src/my-app.js"),
            "class MyApp extends HTMLElement {\n  constructor() {\n    super();\n"
            + "    this.attachShadow({ mode: 'open' });\n  }\n\n"
            + "  connectedCallback() {\n    this.shadowRoot.innerHTML = `\n"
            + "      <style>\n        :host { display: block; font-family: system-ui, sans-serif; "
            + "text-align: center; padding: 2rem; }\n"
            + "        h1 { color: #646cff; }\n      </style>\n"
            + "      <h1>" + name + "</h1>\n      <p>" + escHtml(desc) + "</p>\n"
            + "      <button id=\"btn\">Click Me</button>\n    `;\n"
            + "    this.shadowRoot.getElementById('btn').addEventListener('click', () => {\n"
            + "      alert('Hello from " + escJs(name) + "');\n"
            + "    });\n  }\n}\n\ncustomElements.define('my-app', MyApp);\n");
        writeFile(new File(dir, "style.css"),
            "body { margin: 0; background: #f4f4f4; min-height: 100vh; display: flex; justify-content: center; align-items: center; }\n");
        writeFile(new File(dir, "README.md"), "# " + name + "\n\n" + desc + "\n\n## Usage\nOpen `index.html` in a browser.\n");
    }

    // ── Fullstack / Backend ──

    private void generateNestJsProject(File dir, String name, String desc) throws IOException {
        String mod = name.toLowerCase().replaceAll("[^a-z0-9]", "");
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + mod + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"description\": \"" + escJson(desc) + "\",\n"
            + "  \"scripts\": { \"start\": \"nest start\", \"dev\": \"nest start --watch\", \"build\": \"nest build\" },\n"
            + "  \"dependencies\": { \"@nestjs/core\": \"^10.3.0\", \"@nestjs/common\": \"^10.3.0\",\n"
            + "    \"@nestjs/platform-express\": \"^10.3.0\", \"reflect-metadata\": \"^0.1.14\",\n"
            + "    \"rxjs\": \"^7.8.0\" },\n"
            + "  \"devDependencies\": { \"@nestjs/cli\": \"^10.3.0\", \"typescript\": \"^5.3.0\" }\n}\n");
        writeFile(new File(dir, "tsconfig.json"),
            "{\n  \"compilerOptions\": {\n    \"module\": \"commonjs\",\n"
            + "    \"declaration\": true,\n    \"removeComments\": true,\n"
            + "    \"emitDecoratorMetadata\": true,\n    \"experimentalDecorators\": true,\n"
            + "    \"target\": \"ES2021\",\n    \"sourceMap\": true,\n"
            + "    \"outDir\": \"./dist\",\n    \"baseUrl\": \"./\",\n"
            + "    \"incremental\": true,\n    \"skipLibCheck\": true,\n"
            + "    \"strictNullChecks\": true,\n    \"noImplicitAny\": true\n  },\n"
            + "  \"include\": [\"src/**/*\"]\n}\n");
        writeFile(new File(dir, "src/main.ts"),
            "import { NestFactory } from '@nestjs/core';\n"
            + "import { AppModule } from './app.module';\n\n"
            + "async function bootstrap() {\n  const app = await NestFactory.create(AppModule);\n"
            + "  await app.listen(3000);\n  console.log('" + name + " running on http://localhost:3000');\n}\nbootstrap();\n");
        writeFile(new File(dir, "src/app.module.ts"),
            "import { Module } from '@nestjs/common';\n"
            + "import { AppController } from './app.controller';\n\n"
            + "@Module({ controllers: [AppController] })\nexport class AppModule {}\n");
        writeFile(new File(dir, "src/app.controller.ts"),
            "import { Controller, Get } from '@nestjs/common';\n\n"
            + "@Controller()\nexport class AppController {\n"
            + "  @Get()\n  getHello(): string { return '" + name + " is running!'; }\n}\n");
        writeFile(new File(dir, "README.md"), "# " + name + "\n\n" + desc + "\n\n## Dev\n`npm run start:dev`\n");
    }

    private void generateFastApiProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "main.py"),
            "from fastapi import FastAPI\nfrom fastapi.middleware.cors import CORSMiddleware\n\n"
            + "app = FastAPI(title=\"" + name + "\", description=\"" + escJson(desc) + "\")\n\n"
            + "app.add_middleware(CORSMiddleware, allow_origins=[\"*\"], allow_credentials=True, allow_methods=[\"*\"], allow_headers=[\"*\"])\n\n"
            + "@app.get(\"/\")\nasync def root():\n    return {\"message\": \"" + name + " is running!\", \"description\": \"" + escJson(desc) + "\"}\n\n"
            + "@app.get(\"/api/health\")\nasync def health():\n    return {\"status\": \"ok\"}\n");
        writeFile(new File(dir, "requirements.txt"), "fastapi\nuvicorn\n");
        writeFile(new File(dir, "README.md"), "# " + name + "\n\n" + desc + "\n\n## Dev\n`uvicorn main:app --reload`\n");
    }

    private void generateRailsProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "Gemfile"),
            "source 'https://rubygems.org'\n\ngem 'rails', '~> 7.1'\n"
            + "gem 'sqlite3', '~> 1.6'\ngem 'puma', '~> 6.0'\n");
        writeFile(new File(dir, "config.ru"),
            "require './config/environment'\n\nrun Rails.application\n");
        writeFile(new File(dir, "config/environment.rb"),
            "require_relative 'application'\nRails.application.initialize!\n");
        writeFile(new File(dir, "config/application.rb"),
            "require 'rails/all'\n\nmodule " + name.replaceAll("[^a-zA-Z]", "") + "\n"
            + "  class Application < Rails::Application\n"
            + "    config.load_defaults 7.1\n    config.api_only = true\n  end\nend\n");
        writeFile(new File(dir, "app/controllers/application_controller.rb"),
            "class ApplicationController < ActionController::API\nend\n");
        writeFile(new File(dir, "app/controllers/home_controller.rb"),
            "class HomeController < ApplicationController\n"
            + "  def index\n    render json: { name: '" + name + "', description: '" + escJs(desc) + "' }\n  end\nend\n");
        writeFile(new File(dir, "config/routes.rb"),
            "Rails.application.routes.draw do\n  get '/', to: 'home#index'\nend\n");
        writeFile(new File(dir, "Rakefile"),
            "require_relative 'config/application'\nRails.application.load_tasks\n");
        writeFile(new File(dir, "README.md"), "# " + name + "\n\n" + desc + "\n\n## Dev\n`rails server`\n");
    }

    private void generateLaravelProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "composer.json"),
            "{\n  \"name\": \"" + name.toLowerCase().replaceAll("[^a-z0-9]", "") + "/app\",\n"
            + "  \"description\": \"" + escJson(desc) + "\",\n"
            + "  \"require\": { \"php\": \">=8.1\", \"laravel/framework\": \"^10.0\" },\n"
            + "  \"autoload\": { \"psr-4\": { \"App\\\\\\\\\": \"app/\" } },\n"
            + "  \"scripts\": { \"serve\": \"php artisan serve\" }\n}\n");
        writeFile(new File(dir, "public/index.php"),
            "<?php\n\nrequire __DIR__.'/../vendor/autoload.php';\n\n"
            + "$app = require_once __DIR__.'/../bootstrap/app.php';\n\n"
            + "$kernel = $app->make(Illuminate\\Contracts\\Http\\Kernel::class);\n\n"
            + "$response = $kernel->handle(\n  $request = Illuminate\\Http\\Request::capture()\n);\n"
            + "$response->send();\n$kernel->terminate($request, $response);\n");
        writeFile(new File(dir, "routes/web.php"),
            "<?php\n\nuse Illuminate\\Support\\Facades\\Route;\n\n"
            + "Route::get('/', function () {\n    return response()->json(['name' => '" + name + "', 'desc' => '" + escJs(desc) + "']);\n});\n");
        writeFile(new File(dir, "artisan"),
            "#!/usr/bin/env php\n<?php\ndefine('LARAVEL_START', microtime(true));\n"
            + "require __DIR__.'/vendor/autoload.php';\n"
            + "$app = require_once __DIR__.'/bootstrap/app.php';\n"
            + "$status = $app->handle(Illuminate\\Contracts\\Console\\Kernel::class)->handle("
            + "$_SERVER['argv'] ?? []);\nexit($status);\n");
        writeFile(new File(dir, "README.md"), "# " + name + "\n\n" + desc + "\n\n## Dev\n`php artisan serve`\n");
    }

    private void generateAspNetCoreProject(File dir, String name, String desc) throws IOException {
        String ns = name.replaceAll("[^a-zA-Z0-9]", "");
        writeFile(new File(dir, "Program.cs"),
            "using Microsoft.AspNetCore.Builder;\nusing Microsoft.Extensions.DependencyInjection;\nusing Microsoft.Extensions.Hosting;\n\n"
            + "var builder = WebApplication.CreateBuilder(args);\nbuilder.Services.AddControllers();\n"
            + "var app = builder.Build();\napp.MapControllers();\n"
            + "app.Run(\"http://localhost:5000\");\n");
        writeFile(new File(dir, "Controllers/HomeController.cs"),
            "using Microsoft.AspNetCore.Mvc;\n\nnamespace " + ns + ".Controllers;\n\n"
            + "[ApiController]\n[Route(\"/\")]\npublic class HomeController : ControllerBase\n{\n"
            + "    [HttpGet]\n    public IActionResult Get()\n    {\n"
            + "        return Ok(new { name = \"" + name + "\", description = \"" + escJson(desc) + "\" });\n    }\n}\n");
        writeFile(new File(dir, "appsettings.json"),
            "{\n  \"Logging\": { \"LogLevel\": { \"Default\": \"Information\" } },\n"
            + "  \"AllowedHosts\": \"*\"\n}\n");
        writeFile(new File(dir, ns + ".csproj"),
            "<Project Sdk=\"Microsoft.NET.Sdk.Web\">\n  <PropertyGroup>\n"
            + "    <TargetFramework>net8.0</TargetFramework>\n  </PropertyGroup>\n</Project>\n");
        writeFile(new File(dir, "README.md"), "# " + name + "\n\n" + desc + "\n\n## Dev\n`dotnet run`\n");
    }

    private void generateSymfonyProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "composer.json"),
            "{\n  \"name\": \"" + name.toLowerCase().replaceAll("[^a-z0-9]", "") + "/app\",\n"
            + "  \"description\": \"" + escJson(desc) + "\",\n"
            + "  \"require\": { \"php\": \">=8.1\", \"symfony/framework-bundle\": \"^6.4\" },\n"
            + "  \"autoload\": { \"psr-4\": { \"App\\\\\\\\\": \"src/\" } },\n"
            + "  \"scripts\": { \"serve\": \"symfony serve\" }\n}\n");
        writeFile(new File(dir, "public/index.php"),
            "<?php\n\nuse App\\Kernel;\n\nrequire_once dirname(__DIR__).'/vendor/autoload_runtime.php';\n\n"
            + "return function (array $context) {\n  return new Kernel($context['APP_ENV'], (bool) $context['APP_DEBUG']);\n};\n");
        writeFile(new File(dir, "src/Kernel.php"),
            "<?php\n\nnamespace App;\n\nuse Symfony\\Bundle\\FrameworkBundle\\Kernel\\MicroKernelTrait;\n"
            + "use Symfony\\Component\\HttpKernel\\Kernel as BaseKernel;\n\n"
            + "class Kernel extends BaseKernel\n{\n    use MicroKernelTrait;\n}\n");
        writeFile(new File(dir, "src/Controller/HomeController.php"),
            "<?php\n\nnamespace App\\Controller;\n\n"
            + "use Symfony\\Component\\HttpFoundation\\JsonResponse;\n"
            + "use Symfony\\Component\\Routing\\Annotation\\Route;\n\n"
            + "class HomeController\n{\n"
            + "    #[Route('/', name: 'home')]\n    public function index(): JsonResponse\n"
            + "    {\n        return new JsonResponse(['name' => '" + name + "', 'desc' => '" + escJs(desc) + "']);\n    }\n}\n");
        writeFile(new File(dir, "config/routes.yaml"),
            "controllers:\n    resource: ../src/Controller/\n    type: attribute\n");
        writeFile(new File(dir, ".env"), "APP_ENV=dev\nAPP_DEBUG=1\n");
        writeFile(new File(dir, "README.md"), "# " + name + "\n\n" + desc + "\n\n## Dev\n`symfony serve`\n");
    }

    private void generateExpressMongoProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"description\": \"" + escJson(desc) + "\",\n"
            + "  \"main\": \"server.js\",\n"
            + "  \"scripts\": { \"start\": \"node server.js\", \"dev\": \"nodemon server.js\" },\n"
            + "  \"dependencies\": { \"express\": \"^4.18.2\", \"mongoose\": \"^8.0.0\", \"cors\": \"^2.8.5\" }\n}\n");
        writeFile(new File(dir, "server.js"),
            "const express = require('express');\nconst mongoose = require('mongoose');\n"
            + "const cors = require('cors');\n\nconst app = express();\napp.use(cors());\napp.use(express.json());\n\n"
            + "mongoose.connect(process.env.MONGO_URI || 'mongodb://localhost:27017/" + name.toLowerCase() + "')\n"
            + "  .then(() => console.log('MongoDB connected'))\n  .catch(err => console.error('MongoDB error:', err));\n\n"
            + "const ItemSchema = new mongoose.Schema({ name: String, createdAt: { type: Date, default: Date.now } });\n"
            + "const Item = mongoose.model('Item', ItemSchema);\n\n"
            + "app.get('/api/items', async (req, res) => {\n  try { const items = await Item.find(); res.json(items); } catch (e) { res.status(500).json({ error: e.message }); }\n});\n"
            + "app.post('/api/items', async (req, res) => {\n  try { const item = await Item.create(req.body); res.status(201).json(item); } catch (e) { res.status(400).json({ error: e.message }); }\n});\n\n"
            + "const PORT = process.env.PORT || 3000;\napp.listen(PORT, () => console.log('" + name + " running on port ' + PORT));\n");
        writeFile(new File(dir, ".env"), "MONGO_URI=mongodb://localhost:27017/" + name.toLowerCase() + "\nPORT=3000\n");
        writeFile(new File(dir, "README.md"), "# " + name + "\n\n" + desc + "\n\n## Dev\n`npm install && npm start`\n");
    }

    private void generateAdonisJsProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"description\": \"" + escJson(desc) + "\",\n"
            + "  \"scripts\": { \"start\": \"node server.js\", \"dev\": \"node ace serve --watch\" },\n"
            + "  \"dependencies\": { \"@adonisjs/core\": \"^6.0.0\", \"@adonisjs/lucid\": \"^20.0.0\" }\n}\n");
        writeFile(new File(dir, "server.js"),
            "const { Ignitor } = require('@adonisjs/core/build/standalone');\n"
            + "new Ignitor().httpServer().start();\n");
        writeFile(new File(dir, ".env"), "PORT=3333\nHOST=0.0.0.0\nNODE_ENV=development\n");
        writeFile(new File(dir, "start/routes.js"),
            "'use strict';\n\nconst Route = use('Route');\n\n"
            + "Route.get('/', () => {\n  return { name: '" + name + "', description: '" + escJs(desc) + "' };\n});\n");
        writeFile(new File(dir, "README.md"), "# " + name + "\n\n" + desc + "\n\n## Dev\n`npm run dev`\n");
    }

    // ── More Mobile Hybrid (Android APK) ──

    private void generateQuasarProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"description\": \"" + escJson(desc) + "\",\n"
            + "  \"scripts\": {\n"
            + "    \"start\": \"quasar dev\",\n    \"build\": \"quasar build\",\n"
            + "    \"android\": \"quasar dev -m capacitor -T android\",\n"
            + "    \"build:android\": \"quasar build -m capacitor -T android\"\n"
            + "  },\n  \"dependencies\": { \"vue\": \"^3.4.0\", \"quasar\": \"^2.14.0\" },\n"
            + "  \"devDependencies\": { \"@quasar/app-vite\": \"^2.0.0\", \"@capacitor/core\": \"^5.6.0\", "
            + "\"@capacitor/android\": \"^5.6.0\", \"@capacitor/cli\": \"^5.6.0\" }\n}\n");
        writeFile(new File(dir, "quasar.config.js"),
            "module.exports = function (ctx) {\n  return {\n    boot: [],\n    css: ['app.scss'],\n    extras: ['roboto-font', 'material-icons'],\n    build: { target: { browser: ['last 2 versions'], ios: '15', android: '12' } },\n    devServer: { port: 9000 },\n    framework: { config: {}, plugins: [] },\n    animations: [],\n    ssr: { pwa: false }\n  };\n};\n");
        writeFile(new File(dir, "src/App.vue"),
            "<template>\n  <div id=\"q-app\">\n    <q-layout view=\"hHh Lpr lFf\">\n      <q-header elevated><q-toolbar><q-toolbar-title>" + name + "</q-toolbar-title></q-toolbar></q-header>\n"
            + "      <q-page-container><q-page class=\"q-pa-md\"><h1>" + name + "</h1><p>" + escHtml(desc) + "</p></q-page></q-page-container>\n"
            + "    </q-layout>\n  </div>\n</template>\n<script>\nexport default { name: 'App' };\n</script>\n");
        writeFile(new File(dir, "capacitor.config.json"),
            "{\n  \"appId\": \"com." + name.toLowerCase().replaceAll("[^a-z0-9]", "") + ".app\",\n"
            + "  \"appName\": \"" + name + "\",\n  \"webDir\": \"dist/spa\",\n  \"bundledWebRuntime\": false\n}\n");
        writeFile(new File(dir, "README.md"), "# " + name + "\n\n" + desc + "\n\n## Android\n`npm run build:android`\n");
    }

    private void generateFramework7Project(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"description\": \"" + escJson(desc) + "\",\n"
            + "  \"scripts\": { \"start\": \"vite\", \"build\": \"vite build\", \"android\": \"npx cap run android\" },\n"
            + "  \"dependencies\": { \"framework7\": \"^8.3.0\", \"framework7-react\": \"^8.3.0\", "
            + "\"react\": \"^18.2.0\", \"react-dom\": \"^18.2.0\" },\n"
            + "  \"devDependencies\": { \"vite\": \"^5.0.0\", \"@capacitor/core\": \"^5.6.0\", "
            + "\"@capacitor/android\": \"^5.6.0\", \"@capacitor/cli\": \"^5.6.0\", \"@vitejs/plugin-react\": \"^4.2.0\" }\n}\n");
        writeFile(new File(dir, "vite.config.js"),
            "import { defineConfig } from 'vite';\nimport react from '@vitejs/plugin-react';\n"
            + "export default defineConfig({ plugins: [react()] });\n");
        writeFile(new File(dir, "capacitor.config.json"),
            "{\n  \"appId\": \"com." + name.toLowerCase().replaceAll("[^a-z0-9]", "") + ".app\",\n"
            + "  \"appName\": \"" + name + "\",\n  \"webDir\": \"dist\",\n"
            + "  \"bundledWebRuntime\": false\n}\n");
        writeFile(new File(dir, "index.html"),
            "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n"
            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">\n"
            + "  <title>" + name + "</title>\n</head>\n<body>\n  <div id=\"app\"></div>\n  <script type=\"module\" src=\"/src/main.jsx\"></script>\n</body>\n</html>\n");
        writeFile(new File(dir, "src/main.jsx"),
            "import React from 'react';\nimport { createRoot } from 'react-dom/client';\n"
            + "import App from './App.jsx';\ncreateRoot(document.getElementById('app')).render(<App />);\n");
        writeFile(new File(dir, "src/App.jsx"),
            "import React from 'react';\nimport { App as F7App, View, Page, Navbar, Block } from 'framework7-react';\n\n"
            + "export default function App() {\n  return (\n    <F7App>\n      <View main>\n        <Page>\n"
            + "          <Navbar title=\"" + name + "\" />\n"
            + "          <Block><h1>" + name + "</h1><p>" + escHtml(desc) + "</p></Block>\n"
            + "        </Page>\n      </View>\n    </F7App>\n  );\n}\n");
        writeFile(new File(dir, "README.md"), "# " + name + "\n\n" + desc + "\n\n## Android\n`npm run android`\n");
    }

    private void generateNativeScriptProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"description\": \"" + escJson(desc) + "\",\n"
            + "  \"scripts\": { \"start\": \"ns run\", \"android\": \"ns run android\", \"ios\": \"ns run ios\" },\n"
            + "  \"dependencies\": { \"@nativescript/core\": \"^8.6.0\", \"@nativescript/theme\": \"^3.1.0\" },\n"
            + "  \"devDependencies\": { \"@nativescript/types\": \"^8.6.0\", \"typescript\": \"^5.3.0\" }\n}\n");
        writeFile(new File(dir, "app/app.ts"),
            "import { Application } from '@nativescript/core';\n\n"
            + "Application.run({ moduleName: 'app-root' });\n");
        writeFile(new File(dir, "app/app-root.xml"),
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<Page xmlns=\"http://schemas.nativescript.org/tns.xsd\" navigatingTo=\"onNavigatingTo\">\n"
            + "  <ActionBar title=\"" + name + "\" />\n"
            + "  <StackLayout class=\"p-20\">\n"
            + "    <Label text=\"" + escHtml(name) + "\" class=\"h1 text-center\" />\n"
            + "    <Label text=\"" + escHtml(desc) + "\" class=\"body text-center\" />\n"
            + "  </StackLayout>\n</Page>\n");
        writeFile(new File(dir, "app/app-root.ts"),
            "import { EventData, Page } from '@nativescript/core';\n\n"
            + "export function onNavigatingTo(args: EventData) {\n  const page = args.object as Page;\n}\n");
        writeFile(new File(dir, "tsconfig.json"),
            "{\n  \"compilerOptions\": { \"target\": \"ES2020\", \"module\": \"ESNext\",\n"
            + "    \"moduleResolution\": \"bundler\", \"strict\": true,\n"
            + "    \"esModuleInterop\": true, \"experimentalDecorators\": true,\n"
            + "    \"skipLibCheck\": true, \"forceConsistentCasingInFileNames\": true,\n"
            + "    \"paths\": { \"~/*\": [\"./app/*\"] } },\n"
            + "  \"include\": [\"app/**/*\"]\n}\n");
        writeFile(new File(dir, "nativescript.config.ts"),
            "import { NativeScriptConfig } from '@nativescript/core';\n\n"
            + "export default { id: 'com." + name.toLowerCase().replaceAll("[^a-z0-9]", "") + ".app',\n"
            + "  appPath: 'app', appResourcesPath: 'App_Resources',\n"
            + "  android: { v8Flags: '--expose_gc' } } as NativeScriptConfig;\n");
        writeFile(new File(dir, "README.md"), "# " + name + "\n\n" + desc + "\n\n## Android\n`npm run android`\n");
    }

    private void generateOnsenUiProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"description\": \"" + escJson(desc) + "\",\n"
            + "  \"scripts\": { \"start\": \"vite\", \"build\": \"vite build\", \"android\": \"npx cap run android\" },\n"
            + "  \"dependencies\": { \"onsenui\": \"^2.12.0\", \"react\": \"^18.2.0\", \"react-dom\": \"^18.2.0\", "
            + "\"react-onsenui\": \"^1.12.0\" },\n"
            + "  \"devDependencies\": { \"@vitejs/plugin-react\": \"^4.2.0\", \"vite\": \"^5.0.0\", "
            + "\"@capacitor/core\": \"^5.6.0\", \"@capacitor/android\": \"^5.6.0\", \"@capacitor/cli\": \"^5.6.0\" }\n}\n");
        writeFile(new File(dir, "capacitor.config.json"),
            "{\n  \"appId\": \"com." + name.toLowerCase().replaceAll("[^a-z0-9]", "") + ".app\",\n"
            + "  \"appName\": \"" + name + "\",\n  \"webDir\": \"dist\",\n"
            + "  \"bundledWebRuntime\": false\n}\n");
        writeFile(new File(dir, "index.html"),
            "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n"
            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">\n"
            + "  <title>" + name + "</title>\n  <link rel=\"stylesheet\" href=\"https://unpkg.com/onsenui/css/onsenui.css\">\n"
            + "  <link rel=\"stylesheet\" href=\"https://unpkg.com/onsenui/css/onsen-css-components.min.css\">\n"
            + "</head>\n<body>\n  <div id=\"root\"></div>\n  <script type=\"module\" src=\"/src/main.jsx\"></script>\n</body>\n</html>\n");
        writeFile(new File(dir, "src/main.jsx"),
            "import React from 'react';\nimport { createRoot } from 'react-dom/client';\n"
            + "import App from './App.jsx';\ncreateRoot(document.getElementById('root')).render(<App />);\n");
        writeFile(new File(dir, "src/App.jsx"),
            "import React from 'react';\n"
            + "import { Page, Toolbar, Button } from 'react-onsenui';\n\n"
            + "export default function App() {\n  return (\n"
            + "    <Page renderToolbar={() => <Toolbar><div class='center'>" + name + "</div></Toolbar>}>\n"
            + "      <div style={{ padding: '2rem', textAlign: 'center' }}>\n"
            + "        <h1>" + name + "</h1>\n        <p>" + escHtml(desc) + "</p>\n"
            + "        <Button onClick={() => alert('Hello!')}>Tap Me</Button>\n"
            + "      </div>\n    </Page>\n  );\n}\n");
        writeFile(new File(dir, "vite.config.js"),
            "import { defineConfig } from 'vite';\nimport react from '@vitejs/plugin-react';\n"
            + "export default defineConfig({ plugins: [react()] });\n");
        writeFile(new File(dir, "README.md"), "# " + name + "\n\n" + desc + "\n\n## Android\n`npm run android`\n");
    }

    // ── Modern JS Runtimes ──

    private void generateDenoProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "deno.json"),
            "{\n  \"tasks\": { \"start\": \"deno run --allow-net --allow-read main.ts\" },\n"
            + "  \"imports\": { \"std/\": \"https://deno.land/std@0.220.0/\" }\n}\n");
        writeFile(new File(dir, "main.ts"),
            "import { serve } from 'std/http/server.ts';\n\n"
            + "const handler = (req: Request): Response => {\n"
            + "  const body = JSON.stringify({ name: '" + name + "', description: '" + escJs(desc) + "' });\n"
            + "  return new Response(body, { headers: { 'content-type': 'application/json' } });\n"
            + "};\n\nconsole.log('" + name + " running on http://localhost:8000');\n"
            + "await serve(handler, { port: 8000 });\n");
        writeFile(new File(dir, "README.md"), "# " + name + "\n\n" + desc + "\n\n## Dev\n`deno task start`\n");
    }

    private void generateBunProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"description\": \"" + escJson(desc) + "\",\n"
            + "  \"scripts\": { \"start\": \"bun run index.ts\", \"dev\": \"bun --watch index.ts\" },\n"
            + "  \"dependencies\": { \"hono\": \"^4.0.0\" }\n}\n");
        writeFile(new File(dir, "index.ts"),
            "import { Hono } from 'hono';\n\n"
            + "const app = new Hono();\n\n"
            + "app.get('/', (c) => c.json({ name: '" + name + "', description: '" + escJs(desc) + "' }));\n\n"
            + "export default app;\n");
        writeFile(new File(dir, "tsconfig.json"),
            "{\n  \"compilerOptions\": {\n"
            + "    \"target\": \"ESNext\",\n    \"module\": \"ESNext\",\n"
            + "    \"moduleResolution\": \"bundler\",\n    \"strict\": true,\n"
            + "    \"jsx\": \"react-jsx\",\n    \"jsxImportSource\": \"hono/jsx\"\n  }\n}\n");
        writeFile(new File(dir, "README.md"), "# " + name + "\n\n" + desc + "\n\n## Dev\n`bun run dev`\n");
    }

    // ── Static Site Generators ──

    private void generateHugoProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "hugo.toml"),
            "baseURL = 'https://example.org/'\nlanguageCode = 'en-us'\ntitle = '" + name + "'\n");
        writeFile(new File(dir, "content/_index.md"),
            "---\ntitle: '" + name + "'\n---\n\n" + desc + "\n");
        writeFile(new File(dir, "layouts/index.html"),
            "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n"
            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "  <title>{{ .Title }}</title>\n</head>\n<body>\n  <h1>{{ .Title }}</h1>\n  <p>{{ .Description }}</p>\n"
            + "  <div>{{ .Content }}</div>\n</body>\n</html>\n");
        writeFile(new File(dir, "archetypes/default.md"),
            "---\ntitle: '{{ replace .File.ContentBaseName \"-\" \" \" | title }}'\ndate: {{ .Date }}\n---\n");
        writeFile(new File(dir, "README.md"), "# " + name + "\n\n" + desc + "\n\n## Dev\n`hugo server`\n");
    }

    private void generateEleventyProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "package.json"),
            "{\n  \"name\": \"" + name.toLowerCase() + "\",\n  \"version\": \"1.0.0\",\n"
            + "  \"description\": \"" + escJson(desc) + "\",\n"
            + "  \"scripts\": { \"start\": \"eleventy --serve\", \"build\": \"eleventy\" },\n"
            + "  \"dependencies\": { \"@11ty/eleventy\": \"^2.0.0\" }\n}\n");
        writeFile(new File(dir, ".eleventy.js"),
            "module.exports = function (eleventyConfig) {\n"
            + "  return { dir: { input: 'src', output: '_site' } };\n};\n");
        writeFile(new File(dir, "src/index.html"),
            "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n"
            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "  <title>" + name + "</title>\n</head>\n<body>\n  <h1>" + name + "</h1>\n  <p>" + escHtml(desc) + "</p>\n</body>\n</html>\n");
        writeFile(new File(dir, "src/about.md"),
            "# About\n\n" + desc + "\n");
        writeFile(new File(dir, "README.md"), "# " + name + "\n\n" + desc + "\n\n## Dev\n`npm start`\n");
    }

    private void generateJekyllProject(File dir, String name, String desc) throws IOException {
        writeFile(new File(dir, "_config.yml"),
            "title: " + name + "\ndescription: " + desc + "\nbaseurl: ''\n");
        writeFile(new File(dir, "index.md"),
            "---\nlayout: default\ntitle: " + name + "\n---\n\n" + desc + "\n");
        writeFile(new File(dir, "_layouts/default.html"),
            "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n"
            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "  <title>{{ page.title }}</title>\n</head>\n<body>\n  <h1>{{ page.title }}</h1>\n"
            + "  <p>{{ site.description }}</p>\n  <div>{{ content }}</div>\n</body>\n</html>\n");
        writeFile(new File(dir, "Gemfile"),
            "source 'https://rubygems.org'\ngem 'jekyll', '~> 4.3'\n");
        writeFile(new File(dir, "README.md"), "# " + name + "\n\n" + desc + "\n\n## Dev\n`jekyll serve`\n");
    }

    // ================================================================
    //  FILE WRITING HELPERS
    // ================================================================

    private void writeFile(File f, String content) throws IOException {
        f.getParentFile().mkdirs();
        java.nio.file.Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    private String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String escJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("'", "\\'")
                .replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String escJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private String lastAiDevInstructions = "";
    private File lastAiDevBackupDir = null;

    @FXML
    private void onDevelopProjectWithAi() {
        if (projectRoot == null) {
            showError("Open a project first to develop it with AI.");
            return;
        }
        TextInputDialog input = new TextInputDialog();
        input.setTitle("Develop Project with AI");
        input.setHeaderText("Describe improvements for: " + projectRoot.getName());
        input.setContentText("Instructions:");
        input.initOwner(rootPane.getScene().getWindow());
        input.getEditor().setPromptText("e.g., Add dark mode, fix responsive layout, add authentication...");

        Optional<String> result = input.showAndWait();
        if (!result.isPresent() || result.get().trim().isEmpty()) return;
        final String instructions = result.get().trim();
        lastAiDevInstructions = instructions;

        // Read AI config
        String[] prov = {"gemini"};
        String[] gKey = {""};
        String[] gMod = {"gemini-2.0-flash"};
        String[] oEp = {"https://api.groq.com/openai/v1/chat/completions"};
        String[] oKey = {""};
        String[] oMod = {"llama-3.3-70b-versatile"};
        String[] ollEp = {"http://localhost:11434/api/chat"};
        String[] ollMod = {"llama3.2"};
        File cfgFile = new File(System.getProperty("user.home") + "/.webide/ai.properties");
        if (cfgFile.exists()) {
            java.util.Properties p = new java.util.Properties();
            try (java.io.FileInputStream fis = new java.io.FileInputStream(cfgFile)) {
                p.load(fis);
                prov[0] = p.getProperty("provider", "gemini");
                gKey[0] = p.getProperty("gemini.key", "");
                gMod[0] = p.getProperty("gemini.model", "gemini-2.0-flash");
                String tmp = p.getProperty("openai.endpoint", "");
                if (!tmp.isEmpty()) oEp[0] = tmp;
                oKey[0] = p.getProperty("openai.key", "");
                oMod[0] = p.getProperty("openai.model", "llama-3.3-70b-versatile");
                tmp = p.getProperty("ollama.endpoint", "");
                if (!tmp.isEmpty()) ollEp[0] = tmp;
                ollMod[0] = p.getProperty("ollama.model", "llama3.2");
            } catch (Exception ignored) {}
        }

        final String fProv = prov[0];
        final String fGKey = gKey[0];
        final String fGMod = gMod[0];
        final String fOEp = oEp[0];
        final String fOKey = oKey[0];
        final String fOMod = oMod[0];
        final String fOllEp = ollEp[0];
        final String fOllMod = ollMod[0];

        boolean hasKey = (fProv.equals("gemini") && !fGKey.isEmpty())
            || (fProv.equals("openai") && !fOKey.isEmpty())
            || fProv.equals("ollama");
        if (!hasKey) {
            showError("AI is not configured. Open the AI panel and set up your API key first.");
            return;
        }

        // Collect source files
        java.util.List<File> sourceFiles = new java.util.ArrayList<>();
        collectSourceFiles(projectRoot, sourceFiles);
        if (sourceFiles.isEmpty()) {
            showError("No source files found in the project.");
            return;
        }

        // Create backup
        final File backupDir = createDevelopmentBackup(sourceFiles);
        lastAiDevBackupDir = backupDir;

        // Save prompt
        saveAiPrompt("develop_" + projectRoot.getName(), instructions);

        // Clear old dev entries from generator log and start fresh section
        appendGeneratorLog("");
        appendGeneratorLog("========================================");
        appendGeneratorLog("=== AI DEVELOPMENT STARTED ===");
        appendGeneratorLog("Project: " + projectRoot.getName());
        appendGeneratorLog("Instructions: " + instructions);
        appendGeneratorLog("Files to process: " + sourceFiles.size());
        appendGeneratorLog("Backup: " + (backupDir != null ? backupDir.getAbsolutePath() : "NONE"));
        appendGeneratorLog("========================================");

        Platform.runLater(() -> {
            statusLabel.setText("AI Development: analyzing " + sourceFiles.size() + " files...");
            viewLogBtn.setVisible(true);
            onViewGenerationLog();
        });

        // Build project context string
        final StringBuilder projectCtx = new StringBuilder();
        projectCtx.append("Project: ").append(projectRoot.getName()).append("\n");
        projectCtx.append("Structure:\n");
        buildProjectTreeString(projectRoot, projectCtx, "  ");

        new Thread(() -> {
            int total = sourceFiles.size();
            int succeeded = 0;
            int failed = 0;
            int unchanged = 0;

            for (int idx = 0; idx < total; idx++) {
                File file = sourceFiles.get(idx);
                final String relPath = getRelativePath(projectRoot, file);

                if (relPath.startsWith("..") || relPath.contains(".webide_backups")
                    || relPath.contains(".eagle-project")) {
                    continue;
                }

                final int pct = (idx * 100) / total;
                Platform.runLater(() -> statusLabel.setText("AI: [" + pct + "%] " + relPath));

                try {
                    String fileContent = new String(java.nio.file.Files.readAllBytes(file.toPath()),
                        java.nio.charset.StandardCharsets.UTF_8);
                    String ext = relPath.contains(".") ? relPath.substring(relPath.lastIndexOf('.') + 1) : "txt";

                    String prompt = "You are an expert developer improving a project.\n\n"
                        + "## Project Context\n" + projectCtx.toString() + "\n\n"
                        + "## User Instructions\n" + instructions + "\n\n"
                        + "## Current File: " + relPath + "\n```" + ext + "\n" + fileContent + "\n```\n\n"
                        + "Output ONLY the improved file content inside a code block with the same extension tag, e.g. ```" + ext + "\n...\n```. "
                        + "No explanations outside the code block. If the file does NOT need changes, output an empty response.";

                    String response = callAiApi(fProv, fGKey, fGMod, fOEp, fOKey, fOMod, fOllEp, fOllMod, prompt);
                    String newContent = extractCodeBlock(response, ext);

                    if (newContent != null && !newContent.isEmpty() && !newContent.equals(fileContent)) {
                        // Backup already done, write new content
                        java.nio.file.Files.write(file.toPath(), newContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        succeeded++;
                        appendGeneratorLog("✅ " + relPath + " — updated");
                    } else if (newContent != null && !newContent.isEmpty() && newContent.equals(fileContent)) {
                        unchanged++;
                        appendGeneratorLog("🔵 " + relPath + " — unchanged");
                    } else {
                        unchanged++;
                        appendGeneratorLog("➖ " + relPath + " — skipped (no changes needed)");
                    }
                } catch (Exception ex) {
                    failed++;
                    appendGeneratorLog("❌ " + relPath + " — ERROR: " + ex.getMessage());
                }
            }

            final int finalSucceeded = succeeded;
            final int finalFailed = failed;
            final int finalUnchanged = unchanged;
            appendGeneratorLog("========================================");
            appendGeneratorLog("=== AI DEVELOPMENT COMPLETE ===");
            appendGeneratorLog("✅ Updated: " + finalSucceeded + " | ➖ Unchanged: " + finalUnchanged + " | ❌ Failed: " + finalFailed);
            appendGeneratorLog("Total files: " + total + " | Backup: " + (backupDir != null ? backupDir.getAbsolutePath() : "NONE"));
            appendGeneratorLog("========================================");

            Platform.runLater(() -> {
                refreshTree();
                statusLabel.setText("AI Development complete: " + finalSucceeded + " updated, " + finalFailed + " failed, " + finalUnchanged + " unchanged");

                // Show summary alert
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                alert.setTitle("AI Development Complete");
                alert.setHeaderText("Results for: " + instructions);
                alert.setContentText("✅ Updated: " + finalSucceeded + "\n➖ Unchanged: " + finalUnchanged + "\n❌ Failed: " + finalFailed
                    + "\n\nBackup: " + (backupDir != null ? backupDir.getAbsolutePath() : "NONE"));
                alert.initOwner(rootPane.getScene().getWindow());
                ThemeManager.getInstance().applyTheme(alert.getDialogPane().getScene());
                alert.show();
            });
        }).start();
    }

    private File createDevelopmentBackup(java.util.List<File> sourceFiles) {
        try {
            String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            File backupDir = new File(System.getProperty("user.home") + "/.webide/backups/"
                + projectRoot.getName() + "_" + ts);
            backupDir.mkdirs();
            for (File f : sourceFiles) {
                String relPath = projectRoot.toURI().relativize(f.toURI()).getPath();
                File dest = new File(backupDir, relPath);
                dest.getParentFile().mkdirs();
                java.nio.file.Files.copy(f.toPath(), dest.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return backupDir;
        } catch (Exception ex) {
            appendGeneratorLog("WARN: Backup failed: " + ex.getMessage());
            return null;
        }
    }

    private void buildProjectTreeString(File dir, StringBuilder sb, String indent) {
        File[] files = dir.listFiles();
        if (files == null) return;
        java.util.Arrays.sort(files, (a, b) -> {
            if (a.isDirectory() && !b.isDirectory()) return -1;
            if (!a.isDirectory() && b.isDirectory()) return 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });
        for (File f : files) {
            String name = f.getName();
            if (name.startsWith(".") || name.equals("node_modules") || name.equals(".git")
                || name.equals("build") || name.equals("dist") || name.equals("__pycache__")
                || name.equals("target")) continue;
            if (f.isDirectory()) {
                sb.append(indent).append("📁 ").append(name).append("/\n");
                buildProjectTreeString(f, sb, indent + "  ");
            } else {
                sb.append(indent).append("📄 ").append(name).append("\n");
            }
        }
    }

    private void saveAiPrompt(String source, String promptText) {
        try {
            File dir = new File(System.getProperty("user.home") + "/.webide");
            dir.mkdirs();
            File promptsFile = new File(dir, "saved_prompts.json");
            String ts = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            String entry = "{\n"
                + "  \"source\": \"" + escJson(source) + "\",\n"
                + "  \"timestamp\": \"" + ts + "\",\n"
                + "  \"project\": \"" + escJson(projectRoot != null ? projectRoot.getName() : "") + "\",\n"
                + "  \"prompt\": \"" + escJson(promptText) + "\"\n}";

            String existing = "";
            if (promptsFile.exists()) {
                existing = new String(java.nio.file.Files.readAllBytes(promptsFile.toPath()),
                    java.nio.charset.StandardCharsets.UTF_8).trim();
            }
            String output;
            if (existing.isEmpty() || existing.equals("[]")) {
                output = "[\n" + entry + "\n]";
            } else if (existing.startsWith("[")) {
                output = existing.substring(0, 1) + "\n" + entry + ",\n" + existing.substring(1);
            } else {
                output = "[\n" + entry + "\n]";
            }
            java.nio.file.Files.write(promptsFile.toPath(), output.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception ignored) {}
    }

    private String getRelativePath(File root, File file) {
        try {
            return root.toURI().relativize(file.toURI()).getPath();
        } catch (Exception e) {
            return file.getName();
        }
    }

    private void collectSourceFiles(File dir, List<File> results) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            String name = f.getName();
            if (f.isDirectory()) {
                if (!name.startsWith(".") && !name.equals("node_modules") && !name.equals(".git") && !name.equals("build") && !name.equals("dist") && !name.equals("__pycache__") && !name.equals("target")) {
                    collectSourceFiles(f, results);
                }
            } else {
                String lower = name.toLowerCase();
                if (lower.endsWith(".html") || lower.endsWith(".css") || lower.endsWith(".js")
                    || lower.endsWith(".java") || lower.endsWith(".py") || lower.endsWith(".json")
                    || lower.endsWith(".xml") || lower.endsWith(".properties") || lower.endsWith(".txt")
                    || lower.endsWith(".md") || lower.endsWith(".yaml") || lower.endsWith(".yml")
                    || lower.endsWith(".ts") || lower.endsWith(".jsx") || lower.endsWith(".tsx")
                    || lower.endsWith(".vue") || lower.endsWith(".php") || lower.endsWith(".rb")
                    || lower.endsWith(".c") || lower.endsWith(".cpp") || lower.endsWith(".h")
                    || lower.endsWith(".sh") || lower.endsWith(".bat") || lower.endsWith(".sql")) {
                    results.add(f);
                }
            }
        }
    }

    private synchronized com.eagle.editor.AiInlineProvider getAiInlineProvider() {
        if (aiInlineProvider != null) return aiInlineProvider;
        String[] prov = {"gemini"};
        String[] gKey = {""};
        String[] gMod = {"gemini-2.0-flash"};
        String[] oEp = {"https://api.groq.com/openai/v1/chat/completions"};
        String[] oKey = {""};
        String[] oMod = {"llama-3.3-70b-versatile"};
        String[] ollEp = {"http://localhost:11434/api/chat"};
        String[] ollMod = {"llama3.2"};
        File cfgFile = new File(System.getProperty("user.home") + "/.webide/ai.properties");
        if (cfgFile.exists()) {
            java.util.Properties p = new java.util.Properties();
            try (java.io.FileInputStream fis = new java.io.FileInputStream(cfgFile)) {
                p.load(fis);
                prov[0] = p.getProperty("provider", "gemini");
                gKey[0] = p.getProperty("gemini.key", "");
                gMod[0] = p.getProperty("gemini.model", "gemini-2.0-flash");
                String tmp = p.getProperty("openai.endpoint", "");
                if (!tmp.isEmpty()) oEp[0] = tmp;
                oKey[0] = p.getProperty("openai.key", "");
                oMod[0] = p.getProperty("openai.model", "llama-3.3-70b-versatile");
                tmp = p.getProperty("ollama.endpoint", "");
                if (!tmp.isEmpty()) ollEp[0] = tmp;
                ollMod[0] = p.getProperty("ollama.model", "llama3.2");
            } catch (Exception ignored) {}
        }
        boolean hasKey = (prov[0].equals("gemini") && !gKey[0].isEmpty())
            || (prov[0].equals("openai") && !oKey[0].isEmpty())
            || prov[0].equals("ollama");
        if (hasKey) {
            AiProvider ap = new EngineAiProvider(prov[0], gKey[0], gMod[0], oEp[0], oKey[0], oMod[0], ollEp[0], ollMod[0]);
            aiInlineProvider = new com.eagle.editor.AiInlineProvider(ap);
        }
        return aiInlineProvider;
    }

    private String callAiApi(String provider, String gKey, String gModel, String oEp, String oKey, String oModel, String ollEp, String ollModel, String prompt) throws Exception {
        switch (provider) {
            case "openai":
                return callOpenAiApi(oEp, oKey, oModel, prompt);
            case "ollama":
                return callOllamaApi(ollEp, ollModel, prompt);
            default:
                return callGeminiApi(gKey, gModel, prompt);
        }
    }

    private String callGeminiApi(String key, String model, String prompt) throws Exception {
        String urlStr = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + key;
        com.google.gson.JsonObject body = new com.google.gson.JsonObject();
        com.google.gson.JsonArray contents = new com.google.gson.JsonArray();
        com.google.gson.JsonObject part = new com.google.gson.JsonObject();
        part.addProperty("text", prompt);
        com.google.gson.JsonArray parts = new com.google.gson.JsonArray();
        parts.add(part);
        com.google.gson.JsonObject content = new com.google.gson.JsonObject();
        content.add("parts", parts);
        content.addProperty("role", "user");
        contents.add(content);
        body.add("contents", contents);

        String resp = doHttpPost(urlStr, body, null);
        com.google.gson.JsonObject json = new com.google.gson.Gson().fromJson(resp, com.google.gson.JsonObject.class);
        if (json.has("candidates")) {
            com.google.gson.JsonArray candidates = json.getAsJsonArray("candidates");
            if (candidates.size() > 0) {
                com.google.gson.JsonObject candidate = candidates.get(0).getAsJsonObject();
                if (candidate.has("content")) {
                    com.google.gson.JsonArray p = candidate.getAsJsonObject("content").getAsJsonArray("parts");
                    if (p.size() > 0) return p.get(0).getAsJsonObject().get("text").getAsString();
                }
            }
        }
        throw new Exception("Unexpected Gemini response");
    }

    private String callOpenAiApi(String endpoint, String key, String model, String prompt) throws Exception {
        com.google.gson.JsonObject body = new com.google.gson.JsonObject();
        body.addProperty("model", model);
        body.addProperty("stream", false);
        com.google.gson.JsonArray msgs = new com.google.gson.JsonArray();
        com.google.gson.JsonObject m = new com.google.gson.JsonObject();
        m.addProperty("role", "user");
        m.addProperty("content", prompt);
        msgs.add(m);
        body.add("messages", msgs);

        String resp = doHttpPost(endpoint, body, key);
        com.google.gson.JsonObject json = new com.google.gson.Gson().fromJson(resp, com.google.gson.JsonObject.class);
        if (json.has("choices")) {
            com.google.gson.JsonArray choices = json.getAsJsonArray("choices");
            if (choices.size() > 0) {
                com.google.gson.JsonObject choice = choices.get(0).getAsJsonObject();
                if (choice.has("message"))
                    return choice.getAsJsonObject("message").get("content").getAsString();
            }
        }
        throw new Exception("Unexpected OpenAI response");
    }

    private String callOllamaApi(String endpoint, String model, String prompt) throws Exception {
        com.google.gson.JsonObject body = new com.google.gson.JsonObject();
        body.addProperty("model", model);
        body.addProperty("stream", false);
        com.google.gson.JsonArray msgs = new com.google.gson.JsonArray();
        com.google.gson.JsonObject m = new com.google.gson.JsonObject();
        m.addProperty("role", "user");
        m.addProperty("content", prompt);
        msgs.add(m);
        body.add("messages", msgs);

        try {
            String resp = doHttpPost(endpoint, body, null);
            com.google.gson.JsonObject json = new com.google.gson.Gson().fromJson(resp, com.google.gson.JsonObject.class);
            if (json.has("message"))
                return json.getAsJsonObject("message").get("content").getAsString();
            throw new Exception("Unexpected Ollama response");
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("404") && endpoint.contains("/api/chat")) {
                String genEndpoint = endpoint.replace("/api/chat", "/api/generate");
                body.addProperty("prompt", prompt);
                body.remove("messages");
                body.remove("stream");
                String resp = doHttpPost(genEndpoint, body, null);
                com.google.gson.JsonObject json = new com.google.gson.Gson().fromJson(resp, com.google.gson.JsonObject.class);
                if (json.has("response")) return json.get("response").getAsString();
                throw new Exception("Unexpected Ollama generate response");
            }
            throw e;
        }
    }

    private String doHttpPost(String urlStr, com.google.gson.JsonObject body, String bearerToken) throws Exception {
        byte[] postData = new com.google.gson.Gson().toJson(body).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        if (bearerToken != null && !bearerToken.isEmpty())
            conn.setRequestProperty("Authorization", "Bearer " + bearerToken);
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(120000);
        try (java.io.OutputStream os = conn.getOutputStream()) { os.write(postData); }

        int code = conn.getResponseCode();
        if (code != 200) {
            try (java.io.InputStream es = conn.getErrorStream()) {
                String err = es == null ? "" : new String(readAllBytes2(es), java.nio.charset.StandardCharsets.UTF_8);
                throw new Exception("API returned " + code + ": " + err);
            }
        }
        try (java.io.InputStream is = conn.getInputStream()) {
            return new String(readAllBytes2(is), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    private byte[] readAllBytes2(java.io.InputStream in) throws java.io.IOException {
        java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
        byte[] tmp = new byte[8192];
        int n;
        while ((n = in.read(tmp)) != -1) buf.write(tmp, 0, n);
        return buf.toByteArray();
    }

    private String extractCodeBlock(String response, String expectedExt) {
        // Try matching ```ext ... ```
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
            "```(" + java.util.regex.Pattern.quote(expectedExt) + ")\\s*\\n([\\s\\S]*?)```",
            java.util.regex.Pattern.MULTILINE | java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher m = p.matcher(response);
        if (m.find()) return m.group(2).trim();

        // Try any code block
        p = java.util.regex.Pattern.compile("```\\w*\\s*\\n([\\s\\S]*?)```", java.util.regex.Pattern.MULTILINE);
        m = p.matcher(response);
        if (m.find()) return m.group(1).trim();

        return response.trim();
    }

    private void listProjectFiles(File dir, StringBuilder sb, int depth) {
        if (depth > 3) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                if (!f.getName().startsWith(".") && !f.getName().equals("node_modules")) {
                    for (int i = 0; i < depth; i++) sb.append("  ");
                    sb.append("[DIR] ").append(f.getName()).append("/\n");
                    listProjectFiles(f, sb, depth + 1);
                }
            } else {
                for (int i = 0; i < depth; i++) sb.append("  ");
                sb.append("[FILE] ").append(f.getName()).append("\n");
            }
        }
    }

    @FXML
    private void onShowDatabaseViewer() {
        DatabaseViewerDialog.show();
    }

    @FXML
    private void onAddDesktopRunFiles() {
        if (projectRoot == null) {
            showError("Open a project first.");
            return;
        }
        //String[] choices = {"Electron", "Tauri", "NW.js", "Neutralino.js", "Clean Desktop Files"};
        String[] choices = {"Electron", "Clean Desktop Files"};
        ChoiceDialog<String> d = new ChoiceDialog<>("Electron", choices);
        d.setTitle("Add Desktop Run Config");
        d.setHeaderText("Choose a desktop framework to add config files for:");
        d.setContentText("Framework:");
        ThemeManager.getInstance().applyTheme(d.getDialogPane().getScene());
        d.showAndWait().ifPresent(choice -> {
            switch (choice) {
                case "Electron": addElectronFiles(); break;
                case "Tauri": addTauriFiles(); break;
                case "NW.js": addNwjsFiles(); break;
                case "Neutralino.js": addNeutralinoFiles(); break;
                case "Clean Desktop Files": cleanDesktopRunFiles(); break;
            }
        });
    }

    @FXML
    private void addElectronFiles() {
        try {
            File pkg = new File(projectRoot, "package.json");
            String pkgContent = "{\n  \"name\": \"desktop-app\",\n  \"version\": \"1.0.0\",\n  \"main\": \"main.js\",\n  \"scripts\": {\n    \"start\": \"electron .\",\n    \"build\": \"electron-builder\"\n  },\n  \"devDependencies\": {\n    \"electron\": \"^28.0.0\",\n    \"electron-builder\": \"^24.0.0\"\n  }\n}\n";
            if (pkg.exists()) {
                String existing = readFileContent(pkg);
                if (!existing.contains("\"electron\"")) {
                    appendToFile(pkg, ",\n  \"main\": \"main.js\",\n  \"devDependencies\": {\n    \"electron\": \"^28.0.0\",\n    \"electron-builder\": \"^24.0.0\"\n  }\n");
                }
            } else {
                writeFileContent(pkg, pkgContent);
            }
            File mainJs = new File(projectRoot, "main.js");
            if (!mainJs.exists()) {
                writeFileContent(mainJs, "const { app, BrowserWindow } = require('electron');\n\nfunction createWindow() {\n  const win = new BrowserWindow({\n    width: 1024, height: 768,\n    webPreferences: { nodeIntegration: true }\n  });\n  win.loadFile('index.html');\n}\n\napp.whenReady().then(createWindow);\napp.on('window-all-closed', () => { if (process.platform !== 'darwin') app.quit(); });\n");
            }
            writeFileContent(new File(projectRoot, ".electron-builder.yml"), "appId: com.app.desktop\nproductName: DesktopApp\ndirectories:\n  output: dist\nfiles:\n  - \"**/*\"\nwin:\n  target: nsis\nmac:\n  target: dmg\nlinux:\n  target: AppImage\n");
            showInfo("Electron files created:\n- main.js (entry point)\n- .electron-builder.yml (build config)\n- package.json updated with electron deps\n\nRun: npm install && npm start");
        } catch (Exception e) {
            showError("Failed to add Electron files: " + e.getMessage());
        }
    }

    @FXML
    private void addTauriFiles() {
        try {
            // Check Rust/Cargo first
            if (!isToolAvailable("cargo")) {
                showToolMissingDialog("cargo", "https://rustup.rs/",
                    "cargo is required to build the Rust backend of a Tauri app.\nInstall Rust from https://rustup.rs/ then restart the IDE.");
                return;
            }
            File tauriDir = new File(projectRoot, "src-tauri");
            if (!tauriDir.exists()) tauriDir.mkdirs();
            File cargoToml = new File(tauriDir, "Cargo.toml");
            if (!cargoToml.exists()) {
                writeFileContent(cargoToml, "[package]\nname = \"desktop-app\"\nversion = \"0.1.0\"\nedition = \"2021\"\n\n[build-dependencies]\ntauri-build = { version = \"2\", features = [] }\n\n[dependencies]\ntauri = { version = \"2\", features = [] }\ntauri-plugin-shell = \"2\"\nserde = { version = \"1\", features = [\"derive\"] }\nserde_json = \"1\"\n\n[features]\ndefault = [\"custom-protocol\"]\ncustom-protocol = [\"tauri/custom-protocol\"]\n");
            }
            File tauriConf = new File(tauriDir, "tauri.conf.json");
            if (!tauriConf.exists()) {
                writeFileContent(tauriConf, "{\n  \"$schema\": \"https://raw.githubusercontent.com/nicheai/tsconfig/main/tauri.schema.json\",\n  \"productName\": \"DesktopApp\",\n  \"version\": \"0.1.0\",\n  \"identifier\": \"com.app.desktop\",\n  \"build\": {\n    \"frontendDist\": \"../\",\n    \"devUrl\": \"http://localhost:5173\",\n    \"beforeDevCommand\": \"npm run dev\",\n    \"beforeBuildCommand\": \"npm run build\"\n  },\n  \"app\": {\n    \"windows\": [{ \"title\": \"DesktopApp\", \"width\": 1024, \"height\": 768 }]\n  }\n}\n");
            }
            File mainRs = new File(tauriDir, "src");
            if (!mainRs.exists()) mainRs.mkdirs();
            File libRs = new File(mainRs, "lib.rs");
            if (!libRs.exists()) {
                writeFileContent(libRs, "#[cfg_attr(mobile, tauri::mobile_entry_point)]\npub fn run() {\n    tauri::Builder::default()\n        .run(tauri::generate_context!())\n        .expect(\"error while running tauri application\");\n}\n");
            }
            File mainRs2 = new File(mainRs, "main.rs");
            if (!mainRs2.exists()) {
                writeFileContent(mainRs2, "#![cfg_attr(not(debug_assertions), windows_subsystem = \"windows\")]\n\nfn main() {\n    desktop_app_lib::run();\n}\n");
            }
            writeFileContent(new File(tauriDir, "build.rs"), "fn main() {\n    tauri_build::build();\n}\n");
            File pkg = new File(projectRoot, "package.json");
            if (pkg.exists()) {
                String existing = readFileContent(pkg);
                if (!existing.contains("\"@tauri-apps/cli\"")) {
                    appendToFile(pkg, ",\n  \"scripts\": {\n    \"tauri\": \"tauri\"\n  },\n  \"devDependencies\": {\n    \"@tauri-apps/cli\": \"^2.0.0\"\n  }\n");
                }
            } else {
                writeFileContent(pkg, "{\n  \"name\": \"desktop-app\",\n  \"version\": \"1.0.0\",\n  \"scripts\": {\n    \"tauri\": \"tauri\"\n  },\n  \"devDependencies\": {\n    \"@tauri-apps/cli\": \"^2.0.0\"\n  }\n}\n");
            }
            // Auto-run npm install in terminal
            if (terminalPanel != null) {
                terminalPanel.setWorkingDir(projectRoot);
                terminalPanel.newTerminal();
                terminalPanel.runCommand("npm install @tauri-apps/cli");
                statusLabel.setText("Installing Tauri CLI...");
            }
            detectProjectTechnologies();
            showInfo("Tauri files created in src-tauri/:\n- Cargo.toml, tauri.conf.json\n- src/main.rs, src/lib.rs, build.rs\n- package.json updated with @tauri-apps/cli\n\nnpm install running in terminal...\nAfter install, Run: npx tauri dev");
        } catch (Exception e) {
            showError("Failed to add Tauri files: " + e.getMessage());
        }
    }

    @FXML
    private void addNwjsFiles() {
        try {
            File pkg = new File(projectRoot, "package.json");
            String pkgContent = "{\n  \"name\": \"desktop-app\",\n  \"version\": \"1.0.0\",\n  \"main\": \"index.html\",\n  \"scripts\": {\n    \"start\": \"nw .\",\n    \"build\": \"nwbuild --platforms win,osx,linux .\"\n  },\n  \"devDependencies\": {\n    \"nw\": \"^0.85.0\",\n    \"nw-builder\": \"^4.4.0\"\n  }\n}\n";
            if (pkg.exists()) {
                String existing = readFileContent(pkg);
                if (!existing.contains("\"nw\"")) {
                    appendToFile(pkg, ",\n  \"main\": \"index.html\",\n  \"devDependencies\": {\n    \"nw\": \"^0.85.0\",\n    \"nw-builder\": \"^4.4.0\"\n  }\n");
                }
            } else {
                writeFileContent(pkg, pkgContent);
            }
            showInfo("NW.js files created:\n- package.json updated with nw deps\n\nRun: npm install && npm start");
        } catch (Exception e) {
            showError("Failed to add NW.js files: " + e.getMessage());
        }
    }

    @FXML
    private void addNeutralinoFiles() {
        try {
            File pkg = new File(projectRoot, "package.json");
            if (pkg.exists()) {
                String existing = readFileContent(pkg);
                if (!existing.contains("\"@neutralinojs/neu\"")) {
                    appendToFile(pkg, ",\n  \"scripts\": {\n    \"start\": \"neu run\",\n    \"build\": \"neu build\"\n  },\n  \"devDependencies\": {\n    \"@neutralinojs/neu\": \"^11.0.0\"\n  }\n");
                }
            } else {
                writeFileContent(pkg, "{\n  \"name\": \"desktop-app\",\n  \"version\": \"1.0.0\",\n  \"scripts\": {\n    \"start\": \"neu run\",\n    \"build\": \"neu build\"\n  },\n  \"devDependencies\": {\n    \"@neutralinojs/neu\": \"^11.0.0\"\n  }\n}\n");
            }
            File neutralinoConf = new File(projectRoot, "neutralino.config.json");
            if (!neutralinoConf.exists()) {
                writeFileContent(neutralinoConf, "{\n  \"applicationId\": \"com.app.desktop\",\n  \"version\": \"1.0.0\",\n  \"defaultMode\": \"window\",\n  \"port\": 0,\n  \"documentRoot\": \"./\",\n  \"url\": \"/\",\n  \"enableServer\": true,\n  \"enableNativeAPI\": true,\n  \"nativeAllowList\": [\"app.*\", \"os.*\", \"window.*\"],\n  \"modes\": {\n    \"window\": {\n      \"title\": \"DesktopApp\",\n      \"width\": 1024,\n      \"height\": 768\n    }\n  },\n  \"cli\": {\n    \"binaryName\": \"desktop-app\",\n    \"resourcesPath\": \"/\"\n  }\n}\n");
            }
            File resourcesDir = new File(projectRoot, "resources");
            if (!resourcesDir.exists()) resourcesDir.mkdirs();
            File jsDir = new File(resourcesDir, "js");
            if (!jsDir.exists()) jsDir.mkdirs();
            File mainJs = new File(jsDir, "main.js");
            if (!mainJs.exists()) {
                writeFileContent(mainJs, "// Neutralino.js app\nif (Neutralino) {\n    Neutralino.init();\n    Neutralino.events.on('windowClose', () => {\n        Neutralino.app.exit();\n    });\n}\nconsole.log('Neutralino app started');\n");
            }
            // Auto-run npm install in terminal
            if (terminalPanel != null) {
                terminalPanel.setWorkingDir(projectRoot);
                terminalPanel.newTerminal();
                terminalPanel.runCommand("npm install @neutralinojs/neu");
                statusLabel.setText("Installing Neutralino.js CLI...");
            }
            detectProjectTechnologies();
            showInfo("Neutralino.js files created:\n- neutralino.config.json\n- resources/js/main.js\n- package.json updated\n\nnpm install running in terminal...\nAfter install, Run: npm start");
        } catch (Exception e) {
            showError("Failed to add Neutralino.js files: " + e.getMessage());
        }
    }

    @FXML
    private void cleanDesktopRunFiles() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "This will remove all desktop framework config files:\n"
            + "- main.js (Electron / NW.js)\n- .electron-builder.yml (Electron)\n"
            + "- src-tauri/ (Tauri)\n- neutralino.config.json (Neutralino.js)\n"
            + "- resources/ (Neutralino.js)\n\nContinue?");
        confirm.setTitle("Clean Desktop Files");
        confirm.setHeaderText(null);
        ThemeManager.getInstance().applyTheme(confirm.getDialogPane().getScene());
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        int removed = 0;
        String[][] targets = {
            {"main.js", "file"}, {".electron-builder.yml", "file"},
            {"src-tauri", "dir"}, {"neutralino.config.json", "file"}, {"resources", "dir"}
        };
        for (String[] t : targets) {
            File f = new File(projectRoot, t[0]);
            if (f.exists()) {
                if ("dir".equals(t[1])) deleteRecursively(f);
                else f.delete();
                removed++;
            }
        }
        showInfo("Cleanup complete. Removed " + removed + " items.\n\nNote: package.json was not modified. Remove framework dependencies manually if needed.");
    }

    @FXML
    private void cleanAndroidRunFiles() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "This will remove all Android framework config files:\n"
            + "- capacitor.config.json (Capacitor)\n- config.xml (Cordova)\n"
            + "- App.js, index.js, app.json (React Native)\n"
            + "- metro.config.js, babel.config.js (React Native)\n"
            + "- app/ (NativeScript)\n- App_Resources/ (NativeScript)\n\nContinue?");
        confirm.setTitle("Clean Android Files");
        confirm.setHeaderText(null);
        ThemeManager.getInstance().applyTheme(confirm.getDialogPane().getScene());
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        int removed = 0;
        String[][] targets = {
            {"capacitor.config.json", "file"}, {"config.xml", "file"},
            {"App.js", "file"}, {"index.js", "file"}, {"app.json", "file"},
            {"metro.config.js", "file"}, {"babel.config.js", "file"},
            {"app", "dir"}, {"App_Resources", "dir"}
        };
        for (String[] t : targets) {
            File f = new File(projectRoot, t[0]);
            if (f.exists()) {
                if ("dir".equals(t[1])) deleteRecursively(f);
                else f.delete();
                removed++;
            }
        }
        showInfo("Cleanup complete. Removed " + removed + " items.\n\nNote: package.json was not modified. Remove framework dependencies manually if needed.");
    }

    private void deleteRecursively(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) deleteRecursively(c);
            }
        }
        f.delete();
    }

    @FXML
    private void onAddAndroidRunFiles() {
        if (projectRoot == null) {
            showError("Open a project first.");
            return;
        }
        String[] choices = {
            "Capacitor", "Cordova", "React Native", "NativeScript",
            "---", "Android SDK Tools", "Create/Launch Emulator",
            "List Connected Devices", "Run on Device", "Build APK", "Clean Android Files"
        };
        ChoiceDialog<String> d = new ChoiceDialog<>("Capacitor", choices);
        d.setTitle("Android Run Config & Tools");
        d.setHeaderText("Choose a framework to add config, or run a tool:");
        d.setContentText("Option:");
        ThemeManager.getInstance().applyTheme(d.getDialogPane().getScene());
        d.showAndWait().ifPresent(choice -> {
            switch (choice) {
                case "Capacitor": addCapacitorFiles(); break;
                case "Cordova": addCordovaFiles(); break;
                case "React Native": addReactNativeFiles(); break;
                case "NativeScript": addNativeScriptFiles(); break;
                case "Android SDK Tools": showAndroidSdkTools(); break;
                case "Create/Launch Emulator": createOrLaunchEmulator(); break;
                case "List Connected Devices": listAndroidDevices(); break;
                case "Run on Device": runAndroidOnDevice(); break;
                case "Build APK": onBuildToApk(); break;
                case "Clean Android Files": cleanAndroidRunFiles(); break;
            }
        });
    }

    @FXML
    private boolean checkAdb() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"where", "adb"});
            return p.waitFor() == 0;
        } catch (Exception e) { return false; }
    }

    private String findAndroidHome() {
        String home = System.getenv("ANDROID_HOME");
        if (home == null || home.isEmpty()) home = System.getenv("ANDROID_SDK_ROOT");
        if (home == null || home.isEmpty()) {
            String local = System.getProperty("user.home") + "/AppData/Local/Android/Sdk";
            if (new File(local).exists()) return local;
        }
        return home;
    }

    @FXML
    private void showAndroidSdkTools() {
        StringBuilder msg = new StringBuilder();
        String ah = findAndroidHome();
        if (ah != null && !ah.isEmpty()) {
            msg.append("ANDROID_HOME: ").append(ah).append("\n");
            File adb = new File(ah + "/platform-tools/adb.exe");
            msg.append("adb: ").append(adb.exists() ? "✓ Found" : "✗ Missing").append("\n");
            File emu = new File(ah + "/emulator/emulator.exe");
            msg.append("emulator: ").append(emu.exists() ? "✓ Found" : "✗ Missing").append("\n");
            File sdkman = new File(ah + "/cmdline-tools/latest/bin/sdkmanager.bat");
            msg.append("sdkmanager: ").append(sdkman.exists() ? "✓ Found" : "✗ Missing").append("\n");
            if (!adb.exists()) {
                msg.append("\nTo install platform-tools:\n");
                msg.append("1. Download from: https://developer.android.com/studio/releases/platform-tools\n");
                msg.append("2. Extract to: ").append(ah).append("/platform-tools/\n");
            }
            if (!emu.exists()) {
                msg.append("\nTo install emulator:\n");
                if (new File(ah + "/cmdline-tools/latest/bin/sdkmanager.bat").exists()) {
                    msg.append("Run: sdkmanager \"emulator\" \"system-images;android-33;google_apis;x86_64\"\n");
                } else {
                    msg.append("Install Android Studio and create an AVD from the AVD Manager.\n");
                }
            }
        } else {
            msg.append("ANDROID_HOME not found.\n\n");
            msg.append("To set up Android SDK:\n");
            msg.append("1. Install Android Studio from: https://developer.android.com/studio\n");
            msg.append("2. Set environment variable ANDROID_HOME to your SDK path\n");
            msg.append("   (e.g., C:\\Users\\YourUser\\AppData\\Local\\Android\\Sdk)\n");
            msg.append("3. Restart the IDE\n");
        }
        showInfo(msg.toString());
    }

    @FXML
    private void createOrLaunchEmulator() {
        String ah = findAndroidHome();
        if (ah == null || ah.isEmpty()) {
            showError("ANDROID_HOME not set. Use 'Android SDK Tools' first.");
            return;
        }
        // List existing AVDs
        java.util.List<String> avds = new java.util.ArrayList<>();
        File avdDir = new File(System.getProperty("user.home") + "/.android/avd");
        if (avdDir.exists()) {
            for (File f : avdDir.listFiles()) {
                if (f.getName().endsWith(".ini")) {
                    avds.add(f.getName().replace(".ini", ""));
                }
            }
        }
        StringBuilder msg = new StringBuilder();
        if (avds.isEmpty()) {
            msg.append("No emulators found.\n\nTo create one:\n");
            msg.append("Option 1: Use Android Studio AVD Manager\n");
            msg.append("  Tools → AVD Manager → Create Virtual Device\n\n");
            msg.append("Option 2: Command line\n");
            if (new File(ah + "/cmdline-tools/latest/bin/avdmanager.bat").exists()) {
                msg.append("  avdmanager create avd -n MyDevice -k \"system-images;android-33;google_apis;x86_64\"\n");
            } else {
                msg.append("  Install cmdline-tools first via sdkmanager\n");
            }
            showInfo(msg.toString());
        } else {
            msg.append("Available emulators:\n");
            for (String a : avds) msg.append("  • ").append(a).append("\n");
            msg.append("\nLaunch one from terminal:\n");
            msg.append("  ").append(ah).append("/emulator/emulator -avd <name>\n");
            msg.append("\nOr click OK to launch the first one now.");
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, msg.toString());
            confirm.setTitle("Create/Launch Emulator");
            confirm.setHeaderText(null);
            ThemeManager.getInstance().applyTheme(confirm.getDialogPane().getScene());
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK && !avds.isEmpty()) {
                String avdName = avds.get(0);
                if (terminalPanel != null) {
                    terminalPanel.setWorkingDir(projectRoot);
                    terminalPanel.newTerminal();
                    terminalPanel.runCommand("\"" + ah + "/emulator/emulator\" -avd " + avdName + " -no-snapshot-load");
                }
            }
        }
    }

    @FXML
    private void listAndroidDevices() {
        if (!checkAdb()) {
            showError("adb not found in PATH. Set ANDROID_HOME and add platform-tools to PATH.");
            return;
        }
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"adb", "devices", "-l"});
            java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()));
            StringBuilder out = new StringBuilder();
            String line;
            int count = 0;
            while ((line = r.readLine()) != null) {
                out.append(line).append("\n");
                if (line.contains("device ") && !line.contains("offline")) count++;
            }
            p.waitFor();
            String msg = out.toString();
            if (count == 0) msg += "\nNo devices connected. Connect a device via USB (with USB debugging enabled)\n"
                + "or launch an emulator from 'Create/Launch Emulator'.";
            showInfo(msg);
        } catch (Exception e) {
            showError("Failed to list devices: " + e.getMessage());
        }
    }

    @FXML
    private void runAndroidOnDevice() {
        if (!checkAdb()) {
            showError("adb not found. Set ANDROID_HOME first.");
            return;
        }
        // Detect which framework config exists
        String cmd = null;
        if (new File(projectRoot, "capacitor.config.json").exists()) {
            cmd = "npx cap run android";
        } else if (new File(projectRoot, "config.xml").exists()) {
            cmd = "npx cordova run android";
        } else if (new File(projectRoot, "app/app.js").exists() || new File(projectRoot, "app/app-root.xml").exists()) {
            cmd = "ns run android";
        } else if (new File(projectRoot, "package.json").exists()) {
            String pkg = readFileContent(new File(projectRoot, "package.json"));
            if (pkg.contains("react-native")) cmd = "npx react-native run-android";
        }
        if (cmd == null) {
            showError("No Android framework detected.\nAdd config files first (Capacitor, Cordova, React Native, or NativeScript).");
            return;
        }
        if (terminalPanel != null) {
            terminalPanel.setWorkingDir(projectRoot);
            terminalPanel.newTerminal();
            terminalPanel.runCommand(cmd);
            statusLabel.setText("Running on Android device...");
        }
    }

    @FXML
    private void addCapacitorFiles() {
        try {
            File capConfig = new File(projectRoot, "capacitor.config.json");
            if (!capConfig.exists()) {
                writeFileContent(capConfig, "{\n  \"appId\": \"com.app.android\",\n  \"appName\": \"AndroidApp\",\n  \"webDir\": \".\",\n  \"bundledWebRuntime\": false,\n  \"server\": {\n    \"androidScheme\": \"https\"\n  }\n}\n");
            }
            showInfo("Capacitor config created: capacitor.config.json\n\nRun:\nnpm install @capacitor/core @capacitor/android\nnpx cap init AndroidApp com.app.android\nnpx cap add android\nnpx cap sync\nnpx cap run android");
        } catch (Exception e) {
            showError("Failed to add Capacitor files: " + e.getMessage());
        }
    }

    @FXML
    private void addCordovaFiles() {
        try {
            File configXml = new File(projectRoot, "config.xml");
            if (!configXml.exists()) {
                writeFileContent(configXml, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<widget id=\"com.app.android\" version=\"1.0.0\" xmlns=\"http://www.w3.org/ns/widgets\">\n    <name>AndroidApp</name>\n    <description>A Cordova app</description>\n    <content src=\"index.html\" />\n    <access origin=\"*\" />\n    <allow-intent href=\"http://*/*\" />\n    <allow-intent href=\"https://*/*\" />\n    <platform name=\"android\">\n        <allow-intent href=\"market:*\" />\n    </platform>\n</widget>\n");
            }
            showInfo("Cordova config created: config.xml\n\nRun:\nnpm install -g cordova\ncordova create . com.app.android AndroidApp\ncordova platform add android\ncordova run android");
        } catch (Exception e) {
            showError("Failed to add Cordova files: " + e.getMessage());
        }
    }

    @FXML
    private void addReactNativeFiles() {
        try {
            File pkg = new File(projectRoot, "package.json");
            String pkgContent = "{\n  \"name\": \"AndroidApp\",\n  \"version\": \"1.0.0\",\n  \"private\": true,\n  \"scripts\": {\n    \"start\": \"react-native start\",\n    \"android\": \"react-native run-android\",\n    \"build\": \"cd android && ./gradlew assembleRelease\"\n  },\n  \"dependencies\": {\n    \"react\": \"^18.2.0\",\n    \"react-native\": \"^0.73.0\"\n  },\n  \"devDependencies\": {\n    \"@react-native/metro-config\": \"^0.73.0\"\n  }\n}\n";
            if (pkg.exists()) {
                String existing = readFileContent(pkg);
                if (!existing.contains("\"react-native\"")) {
                    appendToFile(pkg, ",\n  \"scripts\": {\n    \"start\": \"react-native start\",\n    \"android\": \"react-native run-android\"\n  },\n  \"dependencies\": {\n    \"react\": \"^18.2.0\",\n    \"react-native\": \"^0.73.0\"\n  }\n");
                }
            } else {
                writeFileContent(pkg, pkgContent);
            }
            File appJs = new File(projectRoot, "App.js");
            if (!appJs.exists()) {
                writeFileContent(appJs, "import React from 'react';\nimport { View, Text, StyleSheet } from 'react-native';\n\nconst App = () => (\n  <View style={styles.container}>\n    <Text style={styles.title}>AndroidApp</Text>\n    <Text style={styles.subtitle}>Welcome to React Native!</Text>\n  </View>\n);\n\nconst styles = StyleSheet.create({\n  container: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: '#f5f5f5' },\n  title: { fontSize: 24, fontWeight: 'bold', color: '#333' },\n  subtitle: { fontSize: 16, color: '#666', marginTop: 8 },\n});\n\nexport default App;\n");
            }
            File indexJs = new File(projectRoot, "index.js");
            if (!indexJs.exists()) {
                writeFileContent(indexJs, "import { AppRegistry } from 'react-native';\nimport App from './App';\nimport { name as appName } from './app.json';\nAppRegistry.registerComponent(appName, () => App);\n");
            }
            File appJson = new File(projectRoot, "app.json");
            if (!appJson.exists()) {
                writeFileContent(appJson, "{\n  \"name\": \"AndroidApp\",\n  \"displayName\": \"AndroidApp\"\n}\n");
            }
            writeFileContent(new File(projectRoot, "metro.config.js"),
                "const { getDefaultConfig, mergeConfig } = require('@react-native/metro-config');\nconst config = {};\nmodule.exports = mergeConfig(getDefaultConfig(__dirname), config);\n");
            writeFileContent(new File(projectRoot, "babel.config.js"),
                "module.exports = {\n  presets: ['module:@react-native/babel-preset'],\n};\n");
            showInfo("React Native files created:\n- package.json with react-native deps\n- App.js, index.js, app.json\n- metro.config.js, babel.config.js\n\nRun: npm install && npx react-native run-android");
        } catch (Exception e) {
            showError("Failed to add React Native files: " + e.getMessage());
        }
    }

    @FXML
    private void addNativeScriptFiles() {
        try {
            File pkg = new File(projectRoot, "package.json");
            String pkgContent = "{\n  \"name\": \"AndroidApp\",\n  \"version\": \"1.0.0\",\n  \"private\": true,\n  \"scripts\": {\n    \"start\": \"ns run android\",\n    \"build\": \"ns build android\"\n  },\n  \"dependencies\": {\n    \"@nativescript/core\": \"^8.6.0\"\n  },\n  \"devDependencies\": {\n    \"@nativescript/android\": \"^8.6.0\"\n  }\n}\n";
            if (pkg.exists()) {
                String existing = readFileContent(pkg);
                if (!existing.contains("\"@nativescript/core\"")) {
                    appendToFile(pkg, ",\n  \"scripts\": {\n    \"start\": \"ns run android\",\n    \"build\": \"ns build android\"\n  },\n  \"dependencies\": {\n    \"@nativescript/core\": \"^8.6.0\"\n  }\n");
                }
            } else {
                writeFileContent(pkg, pkgContent);
            }
            File appDir = new File(projectRoot, "app");
            if (!appDir.exists()) appDir.mkdirs();
            File appJs = new File(appDir, "app.js");
            if (!appJs.exists()) {
                writeFileContent(appJs, "const Application = require('@nativescript/core').Application;\nApplication.run({ moduleName: 'app-root' });\n");
            }
            File rootXml = new File(appDir, "app-root.xml");
            if (!rootXml.exists()) {
                writeFileContent(rootXml, "<Page xmlns=\"http://schemas.nativescript.org/tns.xsd\" navigatingTo=\"onNavigatingTo\">\n  <StackLayout class=\"p-16\">\n    <Label text=\"AndroidApp\" class=\"h1 text-center\" />\n    <Label text=\"Welcome to NativeScript!\" class=\"body text-center\" />\n  </StackLayout>\n</Page>\n");
            }
            File appCss = new File(appDir, "app.css");
            if (!appCss.exists()) {
                writeFileContent(appCss, ".h1 { font-size: 24; font-weight: bold; margin: 16; }\n.body { font-size: 16; color: #666; }\n.p-16 { padding: 16; }\n.text-center { text-align: center; }\n");
            }
            File appResources = new File(projectRoot, "App_Resources");
            if (!appResources.exists()) appResources.mkdirs();
            File androidDir = new File(appResources, "Android");
            if (!androidDir.exists()) androidDir.mkdirs();
            writeFileContent(new File(androidDir, "AndroidManifest.xml"),
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" package=\"com.app.android\">\n  <uses-permission android:name=\"android.permission.INTERNET\" />\n  <application android:name=\"com.tns.NativeScriptApplication\" android:label=\"AndroidApp\" />\n</manifest>\n");
            showInfo("NativeScript files created:\n- package.json with @nativescript/core deps\n- app/app.js, app/app-root.xml, app/app.css\n- App_Resources/Android/AndroidManifest.xml\n\nRun: npm install && ns run android");
        } catch (Exception e) {
            showError("Failed to add NativeScript files: " + e.getMessage());
        }
    }

    private void appendToFile(File f, String content) throws Exception {
        String existing = readFileContent(f);
        int brace = existing.lastIndexOf("}");
        if (brace >= 0) {
            String before = existing.substring(0, brace).trim();
            String sep = before.endsWith(",") ? "" : ",";
            // Remove leading comma from content if appendToFile already adds one
            String cleaned = content.startsWith(",") ? content.substring(1) : content;
            try (java.io.PrintWriter w = new java.io.PrintWriter(f, "UTF-8")) {
                w.print(existing.substring(0, brace) + sep + cleaned + "\n}");
            }
        }
    }

    @FXML
    private void onBuildToApk() {
        if (projectRoot == null) {
            showError("No project is open. Open a project first.");
            return;
        }

        try {
            // ── Cordova / Capacitor direct build ──
            File configXml = new File(projectRoot, "config.xml");
            File capConfig = new File(projectRoot, "capacitor.config.json");
            if (configXml.exists()) {
                // Run cordova build directly
                if (terminalPanel != null) {
                    terminalPanel.setWorkingDir(projectRoot);
                    terminalPanel.newTerminal();
                    final String cmd = "npx cordova build android";
                    terminalPanel.runCommand(cmd);
                    statusLabel.setText("Building Cordova APK...");
                }
                return;
            }
            if (capConfig.exists()) {
                if (terminalPanel != null) {
                    terminalPanel.setWorkingDir(projectRoot);
                    terminalPanel.newTerminal();
                    final String cmd = "npx cap sync && npx cap run android";
                    terminalPanel.runCommand(cmd);
                    statusLabel.setText("Building Capacitor APK...");
                }
                return;
            }

            boolean hasPackageJson = new File(projectRoot, "package.json").exists();
            boolean hasIndexHtml = new File(projectRoot, "index.html").exists();
            boolean hasIndexJs = new File(projectRoot, "index.js").exists();
            ProjectType projectType = ProjectsStore.getProjectType(projectRoot);

            boolean isDroidScript = hasIndexJs
                && (projectType == ProjectType.ANDROID_JS
                    || (!hasIndexHtml && !hasPackageJson));

            String fxml = null;
            String dialogTitle = "Build to Android APK";

            if (hasPackageJson && isDroidScript && hasIndexHtml) {
                // Ambiguous: show choice dialog
                String choice = showBuildChoiceDialog();
                if (choice == null) return;
                switch (choice) {
                    case "html": fxml = "/com/eagle/fxml/CreateHtmlApk.fxml"; dialogTitle = "HTML to Android APK"; break;
                    case "webapp": fxml = "/com/eagle/fxml/WebAppToApk.fxml"; dialogTitle = "Web App to Android APK"; break;
                    case "js": fxml = "/com/eagle/fxml/JsToApk.fxml"; dialogTitle = "JS to Android APK"; break;
                }
            } else if (hasPackageJson) {
                fxml = "/com/eagle/fxml/WebAppToApk.fxml";
                dialogTitle = "Web App to Android APK";
            } else if (isDroidScript) {
                fxml = "/com/eagle/fxml/JsToApk.fxml";
                dialogTitle = "JS to Android APK";
            } else {
                fxml = "/com/eagle/fxml/CreateHtmlApk.fxml";
                dialogTitle = "HTML to Android APK";
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();

            switch (fxml) {
                case "/com/eagle/fxml/WebAppToApk.fxml":
                    ((WebAppToApkController) loader.getController()).preselectProjectFolder(projectRoot);
                    break;
                case "/com/eagle/fxml/JsToApk.fxml":
                    JsToApkController jc = loader.getController();
                    File indexJs = new File(projectRoot, "index.js");
                    if (indexJs.exists()) {
                        jc.preselectFile(indexJs);
                    } else {
                        File active = tabManager.getActiveFile();
                        if (active != null && active.getName().endsWith(".js")) {
                            jc.preselectFile(active);
                        }
                    }
                    break;
                case "/com/eagle/fxml/CreateHtmlApk.fxml":
                    CreateHtmlApkController hc = loader.getController();
                    File indexHtml = new File(projectRoot, "index.html");
                    if (indexHtml.exists()) {
                        hc.preselectHtmlFile(indexHtml);
                    } else {
                        File active = tabManager.getActiveFile();
                        if (active != null && (active.getName().endsWith(".html") || active.getName().endsWith(".htm"))) {
                            hc.preselectHtmlFile(active);
                        }
                    }
                    break;
            }

            Stage dialogStage = new Stage();
            dialogStage.setTitle(dialogTitle);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(rootPane.getScene().getWindow());
            Scene scene = new Scene(root);
            ThemeManager.getInstance().applyTheme(scene);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
        } catch (Exception e) {
            showError("Failed to open APK builder: " + e.getMessage());
        }
    }

    /** Shows a choice dialog for ambiguous projects (has HTML + JS + package.json). Returns "html", "webapp", "js", or null. */
    private String showBuildChoiceDialog() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Choose Build Type");
        alert.setHeaderText("This project has multiple web types.");
        alert.setContentText("How would you like to build it?");

        ButtonType htmlBtn = new ButtonType("HTML → APK");
        ButtonType webAppBtn = new ButtonType("Web App → APK (npm build)");
        ButtonType jsBtn = new ButtonType("JS → APK (DroidScript)");
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(htmlBtn, webAppBtn, jsBtn, cancelBtn);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == htmlBtn) return "html";
            if (result.get() == webAppBtn) return "webapp";
            if (result.get() == jsBtn) return "js";
        }
        return null;
    }

    @FXML
    private void onDeviceFileExplorer() {
        if (!checkAdb()) return;
        try {
            String ah = findAndroidHome();
            final String adbCmd;
            String adbPath = ah + File.separator + "platform-tools" + File.separator + "adb";
            if (new File(adbPath).exists()) {
                adbCmd = adbPath;
            } else {
                adbCmd = "adb";
            }
            ProcessBuilder pb = new ProcessBuilder(adbCmd, "devices");
            Process p = pb.start();
            java.io.InputStream is = p.getInputStream();
            java.util.Scanner sc = new java.util.Scanner(is, "UTF-8").useDelimiter("\\A");
            String deviceList = sc.hasNext() ? sc.next() : "";
            sc.close();
            TextInputDialog tid = new TextInputDialog();
            tid.setTitle("Device File Explorer");
            tid.setHeaderText("Connected Devices:\n" + deviceList + "\n\nEnter device serial to browse files:");
            ThemeManager.getInstance().applyTheme(tid.getDialogPane().getScene());
            tid.showAndWait().ifPresent(serial -> {
                TextInputDialog pathDlg = new TextInputDialog("/sdcard");
                pathDlg.setTitle("Device File Explorer");
                pathDlg.setHeaderText("Enter path to list files on " + serial + ":");
                ThemeManager.getInstance().applyTheme(pathDlg.getDialogPane().getScene());
                pathDlg.showAndWait().ifPresent(path -> {
                    try {
                        ProcessBuilder pb2 = new ProcessBuilder(adbCmd, "-s", serial, "shell", "ls", "-la", path);
                        Process p2 = pb2.start();
                        java.util.Scanner sc2 = new java.util.Scanner(p2.getInputStream(), "UTF-8").useDelimiter("\\A");
                        String listing = sc2.hasNext() ? sc2.next() : "(empty)";
                        sc2.close();
                        Stage stage = new Stage();
                        stage.setTitle("Device: " + serial + "  Path: " + path);
                        TextArea ta = new TextArea(listing);
                        ta.setEditable(false);
                        ta.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
                        Scene scene = new Scene(new BorderPane(ta), 700, 500);
                        ThemeManager.getInstance().applyTheme(scene);
                        stage.setScene(scene);
                        stage.initOwner(rootPane.getScene().getWindow());
                        stage.show();
                    } catch (Exception e) {
                        showError("Failed to list device files: " + e.getMessage());
                    }
                });
            });
        } catch (Exception e) {
            showError("Failed to open Device File Explorer: " + e.getMessage());
        }
    }

    @FXML
    private void onGenerateSignedBundle() {
        if (!checkAdb()) return;
        TextInputDialog dlg = new TextInputDialog("my-release-key");
        dlg.setTitle("Generate Signed Bundle / APK");
        dlg.setHeaderText("Android App Signing");
        dlg.setContentText("Key alias (default: my-release-key):");
        ThemeManager.getInstance().applyTheme(dlg.getDialogPane().getScene());
        dlg.showAndWait().ifPresent(alias -> {
            try {
                String ah = findAndroidHome();
                String keytool = ah + File.separator + "jre" + File.separator + "bin" + File.separator + "keytool";
                if (!new File(keytool).exists()) keytool = "keytool";
                ProcessBuilder pb = new ProcessBuilder(keytool, "-genkey", "-v",
                    "-keystore", "my-release-key.keystore",
                    "-alias", alias,
                    "-keyalg", "RSA",
                    "-keysize", "2048",
                    "-validity", "10000");
                pb.directory(projectRoot);
                Process p = pb.start();
                java.io.OutputStream os = p.getOutputStream();
                os.write("password\npassword\n\n\n\n\n\nyes\n".getBytes("UTF-8"));
                os.flush();
                os.close();
                p.waitFor();
                showInfo("Keystore generated: " + projectRoot + "/my-release-key.keystore\n\n"
                    + "Password: password\nAlias: " + alias + "\n\n"
                    + "To sign your APK:\n"
                    + "jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore my-release-key.keystore app-release-unsigned.apk " + alias);
            } catch (Exception e) {
                showError("Failed to generate signed bundle: " + e.getMessage());
            }
        });
    }

    @FXML
    private void onBuildDesktopApp() {
        if (projectRoot == null) {
            showError("No project is open.");
            return;
        }
        try {
            File pkgJson = new File(projectRoot, "package.json");
            if (!pkgJson.exists()) {
                showError("No package.json found. Desktop builds require a Node.js project.");
                return;
            }
            String content = readFileContent(pkgJson);
            if (content.contains("\"electron\"")) {
                if (terminalPanel != null) {
                    terminalPanel.setWorkingDir(projectRoot);
                    terminalPanel.newTerminal();
                    terminalPanel.runCommand("npx electron-builder --win --mac --linux");
                    statusLabel.setText("Building Electron app...");
                }
            } else if (content.contains("\"@tauri-apps/cli\"")) {
                if (terminalPanel != null) {
                    terminalPanel.setWorkingDir(projectRoot);
                    terminalPanel.newTerminal();
                    terminalPanel.runCommand("npx tauri build");
                    statusLabel.setText("Building Tauri app...");
                }
            } else if (new File(projectRoot, "neutralino.config.json").exists()) {
                if (terminalPanel != null) {
                    terminalPanel.setWorkingDir(projectRoot);
                    terminalPanel.newTerminal();
                    terminalPanel.runCommand("npx @neutralinojs/neu build");
                    statusLabel.setText("Building Neutralino.js app...");
                }
            } else if (content.contains("\"nw\"")) {
                if (terminalPanel != null) {
                    terminalPanel.setWorkingDir(projectRoot);
                    terminalPanel.newTerminal();
                    terminalPanel.runCommand("npx nwbuild --platforms=win64,osx64,linux64");
                    statusLabel.setText("Building NW.js app...");
                }
            } else {
                showError("No supported desktop framework detected. Add Electron, Tauri, NW.js, or Neutralino.js first.");
            }
        } catch (Exception e) {
            showError("Failed to build desktop app: " + e.getMessage());
        }
    }

    @FXML
    private void onPackageDesktopApp() {
        if (projectRoot == null) {
            showError("No project is open.");
            return;
        }
        try {
            File pkgJson = new File(projectRoot, "package.json");
            if (!pkgJson.exists()) {
                showError("No package.json found.");
                return;
            }
            String content = readFileContent(pkgJson);
            if (content.contains("\"electron\"")) {
                if (terminalPanel != null) {
                    terminalPanel.setWorkingDir(projectRoot);
                    terminalPanel.newTerminal();
                    terminalPanel.runCommand("npx electron-builder --publish=never");
                    statusLabel.setText("Packaging Electron app...");
                }
            } else if (content.contains("\"@tauri-apps/cli\"")) {
                if (terminalPanel != null) {
                    terminalPanel.setWorkingDir(projectRoot);
                    terminalPanel.newTerminal();
                    terminalPanel.runCommand("npx tauri build --bundles msi,nsis,appimage,dmg");
                    statusLabel.setText("Packaging Tauri installers...");
                }
            } else {
                showError("Supported desktop frameworks: Electron, Tauri");
            }
        } catch (Exception e) {
            showError("Failed to package desktop app: " + e.getMessage());
        }
    }

    // ---- Snippets ----
    private void insertSnippet(String text) {
        CodeEditor ed = tabManager.getActiveEditor();
        if (ed == null) {
            statusLabel.setText("Open a file to insert snippet");
            return;
        }
        int pos = ed.getCaretPosition();
        ed.insertText(pos, text);
        statusLabel.setText("Snippet inserted");
    }

    @FXML
    private void onSnippetHtmlBoiler() { insertSnippet("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n    <meta charset=\"UTF-8\">\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n    <title>Document</title>\n</head>\n<body>\n    \n</body>\n</html>"); }

    @FXML
    private void onSnippetCssReset() { insertSnippet("*, *::before, *::after {\n    box-sizing: border-box;\n    margin: 0;\n    padding: 0;\n}\n\nhtml { font-size: 16px; }\nbody { font-family: sans-serif; line-height: 1.6; color: #333; }"); }

    @FXML
    private void onSnippetConsoleLog() { insertSnippet("console.log({CURSOR});"); }

    @FXML
    private void onSnippetFunction() { insertSnippet("function name({CURSOR}) {\n    \n}"); }

    @FXML
    private void onSnippetReactComp() { insertSnippet("import React from 'react';\n\nconst Component = (props) => {\n    return (\n        <div>\n            {CURSOR}\n        </div>\n    );\n};\n\nexport default Component;"); }

    @FXML
    private void onSnippetForLoop() { insertSnippet("for (let i = 0; i < {CURSOR}; i++) {\n    \n}"); }

    @FXML
    private void onSnippetIfStmt() { insertSnippet("if ({CURSOR}) {\n    \n}"); }

    // ---- Tools extra ----
    @FXML
    private void onMinifyCode() {
        CodeEditor ed = tabManager.getActiveEditor();
        if (ed == null) return;
        String text = ed.getText();
        if (text == null || text.isEmpty()) return;
        String min = text.replaceAll("\\s+", " ").replaceAll("\\s*\\{\\s*", "{").replaceAll("\\s*\\}\\s*", "}").replaceAll("\\s*;\\s*", ";").trim();
        ed.setText(min);
        statusLabel.setText("Code minified (basic)");
    }

    // ---- Plugins ----
    @FXML
    private void onNewPlugin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/eagle/fxml/PluginCreatorDialog.fxml"));
            Parent root = loader.load();
            PluginCreatorController ctrl = loader.getController();
            Stage stage = new Stage();
            stage.setTitle("New Plugin");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(rootPane.getScene().getWindow());
            Scene scene = new Scene(root);
            ThemeManager.getInstance().applyTheme(scene);
            stage.setScene(scene);
            stage.setMinWidth(780);
            stage.setMinHeight(600);
            stage.setResizable(true);
            ctrl.setStage(stage);
            stage.showAndWait();
            // Reload plugins if a new one was built
            setupPlugins();
        } catch (IOException e) {
            showError("Could not open plugin creator: " + e.getMessage());
        }
    }

    @FXML
    private void onPluginManager() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/eagle/fxml/PluginManagerDialog.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Plugin Manager");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(rootPane.getScene().getWindow());
            Scene scene = new Scene(root);
            ThemeManager.getInstance().applyTheme(scene);
            stage.setScene(scene);
            stage.showAndWait();
            // Reload toolbar items after plugin changes
            setupPlugins();
        } catch (IOException e) {
            showError("Could not open plugin manager: " + e.getMessage());
        }
    }

    @FXML
    private void onExtensionsMarketplace() {
        ExtensionsMarketplaceDialog dialog = new ExtensionsMarketplaceDialog();
        dialog.show();
        setupPlugins();
    }

    @FXML
    private void onInstallPlugin() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Install Plugin from JAR");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Plugin JAR", "*.jar"));
        File jar = chooser.showOpenDialog(rootPane.getScene().getWindow());
        if (jar == null) return;
        File target = new File(PluginManager.getInstance().getPluginsDir(), jar.getName());
        try {
            java.nio.file.Files.copy(jar.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Plugin copied.\nRestart to load it.");
            a.setHeaderText(null);
            ThemeManager.getInstance().applyTheme(a.getDialogPane().getScene());
            a.showAndWait();
        } catch (IOException e) {
            showError("Install failed: " + e.getMessage());
        }
    }

    @FXML
    private void onBrowsePlugins() {
        try {
            java.awt.Desktop.getDesktop().open(PluginManager.getInstance().getPluginsDir());
        } catch (Exception e) {
            showError("Could not open plugins folder: " + e.getMessage());
        }
    }

    private void setupPlugins() {
        // Register builtin plugins once
        PluginManager pm = PluginManager.getInstance();
        // Only register builtins on first call
        if (!pm.isLoaded("hello-world") && !pm.isLoaded("color-picker")) {
            //pm.registerBuiltin(new com.eagle.plugin.builtin.NotePadPlugin());
            //pm.registerBuiltin(new com.eagle.plugin.builtin.ThreeDViewerPlugin());
            //pm.loadAll();
        }

        // --- Clean up stale plugin contributions ---
        // Remove old plugin-contributed toolbar items (identified by "plugin-item" property)
        javafx.scene.layout.HBox toolbar = (javafx.scene.layout.HBox) rootPane.lookup(".toolbar-container");
        if (toolbar != null) {
            toolbar.getChildren().removeIf(n -> Boolean.TRUE.equals(n.getProperties().get("plugin-item")));
        }

        // Remove old plugin-created menus by finding and removing menus with "plugin-menu" property
        javafx.scene.control.MenuBar menuBar = (javafx.scene.control.MenuBar) rootPane.lookup(".menu-bar");
        if (menuBar != null) {
            menuBar.getMenus().removeIf(m -> Boolean.TRUE.equals(m.getProperties().get("plugin-menu")));
        }

        // Remove old plugin panel container
        javafx.scene.Node oldPanelContainer = rootPane.lookup("#pluginPanelsContainer");
        if (oldPanelContainer != null) {
            javafx.scene.Parent parent = oldPanelContainer.getParent();
            if (parent instanceof javafx.scene.layout.Pane) {
                ((javafx.scene.layout.Pane) parent).getChildren().remove(oldPanelContainer);
            }
        }

        // --- Create new top-level menus ---
        // Collect all new menu registrations first (so menu items can reference them)
        java.util.List<String> createdMenuTitles = new java.util.ArrayList<>();
        if (menuBar != null) {
            for (PluginContext ctx : pm.getActiveContexts()) {
                for (PluginContext.NewMenuRegistration reg : ctx.getNewMenus()) {
                    javafx.scene.control.Menu newMenu = new javafx.scene.control.Menu(reg.title);
                    newMenu.getProperties().put("plugin-menu", Boolean.TRUE);
                    int pos = reg.position >= 0 ? reg.position : menuBar.getMenus().size();
                    pos = Math.min(pos, menuBar.getMenus().size());
                    menuBar.getMenus().add(pos, newMenu);
                    createdMenuTitles.add(reg.title);
                }
            }
        }

        // --- Add plugin menu items to existing or newly-created menus ---
        if (menuBar != null) {
            for (PluginContext ctx : pm.getActiveContexts()) {
                for (PluginContext.MenuItemRegistration reg : ctx.getMenuItems()) {
                    javafx.scene.control.Menu targetMenu = null;
                    for (javafx.scene.control.Menu m : menuBar.getMenus()) {
                        if (m.getText().equals(reg.parentMenu)) {
                            targetMenu = m;
                            break;
                        }
                    }
                    if (targetMenu != null) {
                        javafx.scene.control.MenuItem item = new javafx.scene.control.MenuItem(reg.label);
                        item.setOnAction(reg.handler);
                        item.getProperties().put("plugin-item", Boolean.TRUE);
                        targetMenu.getItems().add(item);
                    }
                }
            }
        }

        // --- Add plugin toolbar items (existing behavior, enhanced) ---
        javafx.scene.Node refNode = null;
        if (toolbar != null) {
            // Find reference point: first separator
            for (javafx.scene.Node n : toolbar.getChildren()) {
                if (n instanceof javafx.scene.control.Separator) {
                    refNode = n;
                    break;
                }
            }
        }
        for (PluginContext ctx : pm.getActiveContexts()) {
            for (PluginContext.ToolbarItemRegistration reg : ctx.getToolbarItems()) {
                reg.node.getProperties().put("plugin-item", Boolean.TRUE);
                if (toolbar != null) {
                    int idx = refNode != null ? toolbar.getChildren().indexOf(refNode) + 1 : toolbar.getChildren().size();
                    toolbar.getChildren().add(idx, reg.node);
                }
            }
        }

        // --- Add new toolbar sections (with label and separators) ---
        if (toolbar != null) {
            for (PluginContext ctx : pm.getActiveContexts()) {
                for (PluginContext.ToolbarSectionRegistration reg : ctx.getToolbarSections()) {
                    javafx.scene.control.Separator sep = new javafx.scene.control.Separator(javafx.geometry.Orientation.VERTICAL);
                    sep.getProperties().put("plugin-item", Boolean.TRUE);
                    toolbar.getChildren().add(sep);

                    javafx.scene.control.Label sectionLabel = new javafx.scene.control.Label(reg.sectionName);
                    sectionLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-muted; -fx-padding: 0 4 0 2;");
                    sectionLabel.getProperties().put("plugin-item", Boolean.TRUE);
                    toolbar.getChildren().add(sectionLabel);

                    for (javafx.scene.Node btn : reg.buttons) {
                        btn.getProperties().put("plugin-item", Boolean.TRUE);
                        toolbar.getChildren().add(btn);
                    }
                }
            }
        }

        // --- Add plugin commands to the existing command palette infrastructure ---
        commandsFromPlugins.clear();
        for (PluginContext ctx : pm.getActiveContexts()) {
            for (PluginContext.CommandRegistration cmd : ctx.getCommands()) {
                commandsFromPlugins.add(new CommandPaletteController.CommandItem(
                    cmd.name, cmd.category, "plugin", cmd.action));
            }
        }

        // --- Add plugin panels as a new right-side tab pane ---
        java.util.List<PluginContext.PanelRegistration> allPanels = new java.util.ArrayList<>();
        for (PluginContext ctx : pm.getActiveContexts()) {
            allPanels.addAll(ctx.getPanels());
        }
        if (!allPanels.isEmpty()) {
            javafx.scene.control.TabPane panelTabs = new javafx.scene.control.TabPane();
            panelTabs.setId("pluginPanelsContainer");
            panelTabs.setPrefWidth(260);
            panelTabs.setMinWidth(200);
            panelTabs.setStyle("-fx-tab-min-width: 80;");
            for (PluginContext.PanelRegistration reg : allPanels) {
                javafx.scene.control.Tab tab = new javafx.scene.control.Tab(reg.title);
                tab.setContent(reg.content);
                tab.setClosable(false);
                panelTabs.getTabs().add(tab);
            }
            // Insert after preview container but before any toggleable panels
            if (mainSplit != null) {
                int insertAt = mainSplit.getItems().indexOf(previewContainer) + 1;
                if (insertAt <= 0) insertAt = mainSplit.getItems().size();
                mainSplit.getItems().add(insertAt, panelTabs);
                // Adjust divider positions to fit 4 items
                if (mainSplit.getItems().size() >= 4) {
                    mainSplit.setDividerPositions(0.18, 0.48, 0.68);
                }
            }
        }
    }

    private final java.util.List<CommandPaletteController.CommandItem> commandsFromPlugins = new java.util.ArrayList<>();

    // ---- Window ----
    @FXML
    private void onMinimize() {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.setIconified(true);
    }

    // ---- Help extra ----
    @FXML
    private void onKeyboardShortcuts() {
        Alert d = new Alert(Alert.AlertType.INFORMATION,
            "Common Shortcuts:\n\n" +
            "Ctrl+N  - New File\n" +
            "Ctrl+O  - Open Folder\n" +
            "Ctrl+S  - Save\n" +
            "Ctrl+Shift+S - Save All\n" +
            "Ctrl+F  - Find/Replace\n" +
            "Ctrl+G  - Go to Line\n" +
            "Ctrl+Z  - Undo\n" +
            "Ctrl+Y  - Redo\n" +
            "Ctrl+P  - Toggle Preview\n" +
            "Ctrl+B  - Toggle Sidebar\n" +
            "F11     - Full Screen\n" +
            "F1      - Documentation"
        );
        d.setTitle("Keyboard Shortcuts");
        d.setHeaderText("Eagle IDE Shortcuts");
        ThemeManager.getInstance().applyTheme(d.getDialogPane().getScene());
        d.showAndWait();
    }

    @FXML
    private void onDocumentation() {
        try {
            java.awt.Desktop.getDesktop().browse(
                new java.net.URI("https://github.com/user/eagle-ide/wiki"));
        } catch (Exception e) {
            statusLabel.setText("Could not open documentation");
        }
    }

    @FXML
    private void onCheckUpdates() {
        statusLabel.setText("Checking for updates... (not yet implemented)");
    }

    @FXML
    private void onReportIssue() {
        try {
            java.awt.Desktop.getDesktop().browse(
                new java.net.URI("https://github.com/user/eagle-ide/issues"));
        } catch (Exception e) {
            statusLabel.setText("Could not open issue tracker");
        }
    }

    // ================================================================
    //   HELPERS
    // ================================================================

    private boolean deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File c : children) deleteRecursive(c);
            }
        }
        return file.delete();
    }

    private String readFileContent(File file) {
        try { return new String(java.nio.file.Files.readAllBytes(file.toPath()),
            java.nio.charset.StandardCharsets.UTF_8); } catch (Exception e) { return ""; }
    }

    private void writeFileContent(File file, String content) {
        try { java.nio.file.Files.write(file.toPath(), content.getBytes(
            java.nio.charset.StandardCharsets.UTF_8)); } catch (Exception ignored) {}
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(null);
        ThemeManager.getInstance().applyTheme(alert.getDialogPane().getScene());
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setHeaderText(null);
        ThemeManager.getInstance().applyTheme(alert.getDialogPane().getScene());
        alert.showAndWait();
    }

    // ================================================================
    //   ADVANCED TREE CELL RENDERER
    // ================================================================

    private static final java.util.Map<String, String> FILE_TYPE_COLORS = new java.util.HashMap<>();
    static {
        FILE_TYPE_COLORS.put("js", "#f7df1e");
        FILE_TYPE_COLORS.put("mjs", "#f7df1e");
        FILE_TYPE_COLORS.put("ts", "#3178c6");
        FILE_TYPE_COLORS.put("jsx", "#61dafb");
        FILE_TYPE_COLORS.put("tsx", "#3178c6");
        FILE_TYPE_COLORS.put("html", "#e34c26");
        FILE_TYPE_COLORS.put("htm", "#e34c26");
        FILE_TYPE_COLORS.put("css", "#563d7c");
        FILE_TYPE_COLORS.put("scss", "#c6538c");
        FILE_TYPE_COLORS.put("sass", "#c6538c");
        FILE_TYPE_COLORS.put("less", "#1d365d");
        FILE_TYPE_COLORS.put("json", "#292929");
        FILE_TYPE_COLORS.put("xml", "#0060ac");
        FILE_TYPE_COLORS.put("md", "#083fa1");
        FILE_TYPE_COLORS.put("java", "#b07219");
        FILE_TYPE_COLORS.put("py", "#3572A5");
        FILE_TYPE_COLORS.put("php", "#4F5D95");
        FILE_TYPE_COLORS.put("sql", "#e38c00");
        FILE_TYPE_COLORS.put("sh", "#4d5a5e");
        FILE_TYPE_COLORS.put("bash", "#4d5a5e");
        FILE_TYPE_COLORS.put("bat", "#c1f12d");
        FILE_TYPE_COLORS.put("cmd", "#c1f12d");
        FILE_TYPE_COLORS.put("yml", "#cb171e");
        FILE_TYPE_COLORS.put("yaml", "#cb171e");
        FILE_TYPE_COLORS.put("vue", "#42b883");
        FILE_TYPE_COLORS.put("svelte", "#ff3e00");
        FILE_TYPE_COLORS.put("svg", "#ffb13b");
        FILE_TYPE_COLORS.put("png", "#a4c639");
        FILE_TYPE_COLORS.put("jpg", "#a4c639");
        FILE_TYPE_COLORS.put("jpeg", "#a4c639");
        FILE_TYPE_COLORS.put("gif", "#a4c639");
        FILE_TYPE_COLORS.put("ico", "#a4c639");
        FILE_TYPE_COLORS.put("pdf", "#b30b00");
        FILE_TYPE_COLORS.put("txt", "#4477aa");
        FILE_TYPE_COLORS.put("env", "#fca121");
        FILE_TYPE_COLORS.put("gitignore", "#e64a19");
        FILE_TYPE_COLORS.put("dockerfile", "#0db7ed");
        FILE_TYPE_COLORS.put("zip", "#665544");
        FILE_TYPE_COLORS.put("tar", "#665544");
        FILE_TYPE_COLORS.put("gz", "#665544");
        FILE_TYPE_COLORS.put("rar", "#665544");
    }

    @FXML
    private void onCompareFiles() {
        File current = tabManager != null ? tabManager.getActiveFile() : null;
        com.eagle.tools.FileDiffTool.show(rootPane.getScene().getWindow(), current);
    }

    private void scanDirectoryPaths(File dir, java.util.Map<String, Boolean> snapshot) {
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isHidden()) continue;
                if (ProjectMeta.isMarkerFile(child)) continue;
                snapshot.put(child.getAbsolutePath(), child.isDirectory());
                if (child.isDirectory()) {
                    scanDirectoryPaths(child, snapshot);
                }
            }
        }
    }

    private void startFileWatcher() {
        Thread watcherThread = new Thread(() -> {
            java.util.Map<String, Boolean> currentTreeSnapshot = new java.util.HashMap<>();
            File projectsRoot = ProjectsStore.getProjectsRoot();
            if (projectsRoot.exists()) {
                scanDirectoryPaths(projectsRoot, currentTreeSnapshot);
            }

            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }

                // 1. Check tree changes (additions/deletions/renames)
                java.util.Map<String, Boolean> newTreeSnapshot = new java.util.HashMap<>();
                if (projectsRoot.exists()) {
                    scanDirectoryPaths(projectsRoot, newTreeSnapshot);
                }

                if (!newTreeSnapshot.equals(currentTreeSnapshot)) {
                    currentTreeSnapshot = newTreeSnapshot;
                    Platform.runLater(this::refreshTree);
                }

                // 2. Check open files for external modifications / deletions
                if (tabManager != null) {
                    java.util.List<File> openFiles = new java.util.ArrayList<>(tabManager.allOpenFiles());
                    for (File f : openFiles) {
                        if (!f.exists()) {
                            Platform.runLater(() -> tabManager.forceCloseForDeletedFile(f));
                        } else {
                            Long lastKnownTime = tabManager.getFileLastModifiedTime(f);
                            if (lastKnownTime != null) {
                                long diskTime = f.lastModified();
                                if (diskTime > lastKnownTime) {
                                    Platform.runLater(() -> tabManager.reloadFileFromDisk(f));
                                }
                            }
                        }
                    }
                }
            }
        });
        watcherThread.setDaemon(true);
        watcherThread.setName("ProjectFileWatcher");
        watcherThread.start();
    }

    private static String getFileTypeColor(File f) {
        String ext = "";
        String name = f.getName().toLowerCase();
        int dot = name.lastIndexOf('.');
        if (dot >= 0) ext = name.substring(dot + 1);
        return FILE_TYPE_COLORS.getOrDefault(ext, "#8a8a8a");
    }

    private static class AdvancedTreeCell extends TreeCell<FileTreeItem> {
        @Override
        protected void updateItem(FileTreeItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setStyle("");
                return;
            }

            File file = item.getFile();
            File root = ProjectsStore.getProjectsRoot();
            boolean isProjectRoot = file.isDirectory()
                && file.getParentFile() != null
                && file.getParentFile().equals(root);

            // Alternating row background (subtle)
            int rowIdx = getIndex();
            String rowBg = (rowIdx % 2 == 0) ? "transparent" : "rgba(128,128,128,0.04)";

            // Left stripe (file-type color)
            javafx.scene.layout.Region stripe = new javafx.scene.layout.Region();
            stripe.setPrefWidth(3);
            String fileColor = getFileTypeColor(file);
            stripe.setStyle("-fx-background-color: " + fileColor + ";"
                + "-fx-background-radius: 0 2 2 0;");

            // Icon
            javafx.scene.image.ImageView iconView = null;
            if (isProjectRoot) {
                ProjectType type = ProjectsStore.getProjectType(file);
                iconView = type == ProjectType.VISUAL
                    ? IconManager.imageView(IconManager.ADVANCED_TOOL, 16)
                    : IconManager.imageView(IconManager.PACKAGE, 16);
            } else if (file.isDirectory()) {
                boolean expanded = getTreeItem() != null && getTreeItem().isExpanded();
                iconView = IconManager.imageView(expanded ? IconManager.OPEN_FOLDER : IconManager.FOLDER, 16);
            } else {
                iconView = FileIconUtil.iconViewFor(file, 16);
            }

            // File name
            Label nameLabel = new Label(file.getName());
            nameLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: " + (isProjectRoot ? "bold" : "normal") + ";"
                + "-fx-text-fill: -text-primary;");

            // Extension badge
            javafx.scene.layout.HBox leftContent = new javafx.scene.layout.HBox(5);
            leftContent.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            leftContent.getChildren().add(stripe);
            if (iconView != null) leftContent.getChildren().add(iconView);
            leftContent.getChildren().add(nameLabel);

            if (!file.isDirectory()) {
                String ext = getExtension(file.getName());
                if (!ext.isEmpty()) {
                    Label extLabel = new Label("." + ext);
                    extLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-muted; -fx-opacity: 0.6;");
                    leftContent.getChildren().add(extLabel);
                }
            }

            // Inline action buttons (right side)
            javafx.scene.layout.HBox actionBtns = new javafx.scene.layout.HBox(0);
            actionBtns.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            actionBtns.setStyle("-fx-opacity: 0;");

            Button renameBtn = new Button();
            renameBtn.setGraphic(IconManager.imageView(IconManager.RENAME, 12));
            renameBtn.setStyle("-fx-background: transparent; -fx-padding: 2; -fx-min-width: 18; -fx-min-height: 18; -fx-cursor: hand;");
            renameBtn.setTooltip(new javafx.scene.control.Tooltip("Rename"));
            TreeItem<FileTreeItem> tiRef = getTreeItem();
            renameBtn.setOnAction(e -> {
                EditorController inst = EditorController.getInstance();
                if (inst != null) inst.renameItem(getTreeItem());
            });
            actionBtns.getChildren().add(renameBtn);

            Button deleteBtn = new Button();
            deleteBtn.setGraphic(IconManager.imageView(IconManager.DELETE, 12));
            deleteBtn.setStyle("-fx-background: transparent; -fx-padding: 2; -fx-min-width: 18; -fx-min-height: 18; -fx-cursor: hand;");
            deleteBtn.setTooltip(new javafx.scene.control.Tooltip("Delete"));
            deleteBtn.setOnAction(e -> {
                EditorController inst = EditorController.getInstance();
                if (inst != null) inst.deleteItem(getTreeItem());
            });
            actionBtns.getChildren().add(deleteBtn);

            if (file.isDirectory()) {
                Button newFileBtn = new Button();
                newFileBtn.setGraphic(IconManager.imageView(IconManager.NEW_FILE, 12));
                newFileBtn.setStyle("-fx-background: transparent; -fx-padding: 2; -fx-min-width: 18; -fx-min-height: 18; -fx-cursor: hand;");
                newFileBtn.setTooltip(new javafx.scene.control.Tooltip("New File"));
                newFileBtn.setOnAction(e -> {
                    EditorController inst = EditorController.getInstance();
                    if (inst != null) inst.createNewFileIn(file);
                });
                actionBtns.getChildren().add(newFileBtn);
            }

            setOnMouseEntered(e -> actionBtns.setStyle("-fx-opacity: 1;"));
            setOnMouseExited(e -> actionBtns.setStyle("-fx-opacity: 0;"));

            // Status indicators (right side)
            javafx.scene.layout.HBox badges = new javafx.scene.layout.HBox(4);
            badges.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            TabManager tm = EditorController.getInstance() != null ? EditorController.getInstance().tabManager : null;
            if (tm != null && tm.isOpen(file)) {
                boolean dirty = tm.isDirty(tm.tabFor(file));
                Label statusDot = new Label();
                statusDot.setStyle(dirty
                    ? "-fx-background-color: #f1c40f; -fx-min-width: 7; -fx-min-height: 7; -fx-background-radius: 4;"
                    : "-fx-background-color: -accent; -fx-min-width: 5; -fx-min-height: 5; -fx-background-radius: 3;");
                badges.getChildren().add(statusDot);
            }

            javafx.scene.layout.HBox content = new javafx.scene.layout.HBox(6);
            content.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            content.getChildren().add(leftContent);
            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            content.getChildren().add(spacer);
            content.getChildren().add(actionBtns);
            if (!badges.getChildren().isEmpty()) {
                content.getChildren().add(badges);
            }

            setGraphic(content);
            setText(null);
            setPrefHeight(28);
            setStyle("-fx-background-color: " + rowBg + ";"
                + "-fx-padding: 1 2 1 0;");

            // Tooltip
            StringBuilder tip = new StringBuilder(file.getName());
            if (file.isFile()) {
                tip.append("\nSize: ").append(formatFileSize(file.length()));
                tip.append("\nModified: ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(file.lastModified())));
            } else {
                tip.append("\nDirectory");
            }
            javafx.scene.control.Tooltip ttip = new javafx.scene.control.Tooltip(tip.toString());
            ttip.setStyle("-fx-font-size: 11px; -fx-padding: 6 10;");
            setTooltip(ttip);
        }

        private static String getExtension(String name) {
            int dot = name.lastIndexOf('.');
            if (dot < 0 || dot == name.length() - 1) return "";
            return name.substring(dot + 1);
        }

        private String formatFileSize(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}