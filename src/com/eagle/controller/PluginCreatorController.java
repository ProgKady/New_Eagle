package com.eagle.controller;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.geometry.Insets;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class PluginCreatorController {

    private static final int STEPS = 5;
    private int currentStep = 0;
    private Stage stage;

    // Step indicator labels
    @FXML private Label step1Label, step2Label, step3Label, step4Label, step5Label;
    @FXML private Label step1Num, step2Num, step3Num, step4Num, step5Num;

    // Step title
    @FXML private Label stepTitle;

    // Content panes for each step
    @FXML private VBox step1Content;
    @FXML private VBox step2Content;
    @FXML private VBox step3Content;
    @FXML private VBox step4Content;
    @FXML private VBox step5Content;

    // Step 1 fields
    @FXML private TextField pluginNameField;
    @FXML private TextField pluginIdField;
    @FXML private TextField versionField;
    @FXML private TextField authorField;
    @FXML private TextArea descArea;

    // Step 2 fields
    @FXML private CheckBox chkFxml;
    @FXML private CheckBox chkCommand;
    @FXML private TextField cmdNameField;
    @FXML private TextField cmdCategoryField;
    @FXML private CheckBox chkToolbar;
    @FXML private TextField toolBtnTextField;
    @FXML private TextField toolTooltipField;
    @FXML private CheckBox chkMenu;
    @FXML private TextField menuParentField;
    @FXML private TextField menuTextField;
    @FXML private CheckBox chkNewMenu;
    @FXML private TextField newMenuTitleField;
    @FXML private TextField newMenuPosField;
    @FXML private CheckBox chkToolbarSection;
    @FXML private TextField toolSectionNameField;
    @FXML private TextField toolBtnLabelsField;
    @FXML private CheckBox chkPanel;
    @FXML private TextField panelTitleField;
    @FXML private CheckBox chkApkTool;
    @FXML private TextField apkToolNameField;
    @FXML private TextField apkToolDescField;
    @FXML private TextField apkToolExtField;

    // Step 3 fields (FXML + Resources)
    @FXML private TextField mainFxmlField;
    @FXML private TextField mainCtrlField;
    @FXML private TextField mainPkgField;
    @FXML private VBox subDialogsContainer;
    @FXML private TextField libPathField;
    @FXML private ListView<String> libsListView;

    // Step 4
    @FXML private TextArea codePreview;

    // Step 5
    @FXML private TextField jdkPathField;
    @FXML private TextArea buildLog;
    @FXML private Button buildBtn;
    @FXML private Hyperlink openFolderLink;

    // Navigation
    @FXML private Button backBtn;
    @FXML private Button nextBtn;
    @FXML private Button cancelBtn;

    private File outputJar;
    private File buildDir;

    // Sub-dialog entries (each row has: name, fxml path, controller path)
    private final List<SubDialogEntry> subDialogs = new ArrayList<>();
    // Library JARs
    private final List<File> libs = new ArrayList<>();

    private static class SubDialogEntry {
        HBox row;
        TextField nameField;
        TextField fxmlField;
        TextField ctrlField;
        TextField pkgField;

        SubDialogEntry(HBox row, TextField nameField, TextField fxmlField, TextField ctrlField, TextField pkgField) {
            this.row = row;
            this.nameField = nameField;
            this.fxmlField = fxmlField;
            this.ctrlField = ctrlField;
            this.pkgField = pkgField;
        }
    }

    @FXML
    void initialize() {
        pluginNameField.textProperty().addListener((o, ov, nv) -> {
            if (nv != null && !nv.isEmpty()) {
                pluginIdField.setText(nv.toLowerCase()
                    .replaceAll("[^a-z0-9\\s-]", "")
                    .trim().replaceAll("\\s+", "-"));
            }
        });
        chkCommand.selectedProperty().addListener((o, ov, nv) -> {
            cmdNameField.setDisable(!nv);
            cmdCategoryField.setDisable(!nv);
        });
        chkToolbar.selectedProperty().addListener((o, ov, nv) -> {
            toolBtnTextField.setDisable(!nv);
            toolTooltipField.setDisable(!nv);
        });
        chkMenu.selectedProperty().addListener((o, ov, nv) -> {
            menuParentField.setDisable(!nv);
            menuTextField.setDisable(!nv);
        });
        chkApkTool.selectedProperty().addListener((o, ov, nv) -> {
            apkToolNameField.setDisable(!nv);
            apkToolDescField.setDisable(!nv);
            apkToolExtField.setDisable(!nv);
        });
        chkNewMenu.selectedProperty().addListener((o, ov, nv) -> {
            newMenuTitleField.setDisable(!nv);
            newMenuPosField.setDisable(!nv);
        });
        chkNewMenu.setSelected(false);
        newMenuTitleField.setDisable(true);
        newMenuPosField.setDisable(true);
        chkToolbarSection.selectedProperty().addListener((o, ov, nv) -> {
            toolSectionNameField.setDisable(!nv);
            toolBtnLabelsField.setDisable(!nv);
        });
        chkToolbarSection.setSelected(false);
        toolSectionNameField.setDisable(true);
        toolBtnLabelsField.setDisable(true);
        chkPanel.selectedProperty().addListener((o, ov, nv) -> {
            panelTitleField.setDisable(!nv);
        });
        chkPanel.setSelected(false);
        panelTitleField.setDisable(true);
        showStep(0);
        detectJdk();
    }

    void setStage(Stage stage) { this.stage = stage; }

    // ================================================================
    //  NAVIGATION
    // ================================================================

    @FXML
    private void onCancel() {
        if (stage != null) stage.close();
    }

    @FXML
    private void onBack() {
        if (currentStep <= 0) return;
        int prev = currentStep - 1;
        // If FXML is not checked, skip step 2 (FXML & Resources) when going back
        if (prev == 2 && !chkFxml.isSelected()) {
            prev = 1;
        }
        showStep(prev);
    }

    @FXML
    private void onNext() {
        if (currentStep == 0 && !validateStep1()) return;
        if (currentStep == 1) {
            if (!validateStep2()) return;
            if (!chkFxml.isSelected()) {
                // No FXML — generate code now, skip FXML step
                generateCode();
                showStep(3);
                return;
            }
            // FXML checked — go to FXML step first
            showStep(2);
            return;
        }
        if (currentStep == 2) {
            if (!validateStep3()) return;
            generateCode();
        }
        if (currentStep < STEPS - 1) showStep(currentStep + 1);
    }

    private void showStep(int step) {
        currentStep = step;
        step1Content.setVisible(step == 0); step1Content.setManaged(step == 0);
        step2Content.setVisible(step == 1); step2Content.setManaged(step == 1);
        step3Content.setVisible(step == 2); step3Content.setManaged(step == 2);
        step4Content.setVisible(step == 3); step4Content.setManaged(step == 3);
        step5Content.setVisible(step == 4); step5Content.setManaged(step == 4);

        backBtn.setDisable(step == 0);
        nextBtn.setVisible(step < STEPS - 1);
        if (step == STEPS - 2) {
            nextBtn.setText("Build & Deploy →");
        } else if (step == STEPS - 3) {
            nextBtn.setText("Generate Code →");
        } else {
            nextBtn.setText("Next →");
        }
        buildBtn.setVisible(step == STEPS - 1);

        String[] titles = {
            "Basic Information",
            "Plugin Capabilities",
            "FXML & Resources",
            "Review Generated Code",
            "Build & Deploy"
        };
        if (step >= 0 && step < titles.length) {
            stepTitle.setText("Step " + (step + 1) + " of " + STEPS + ": " + titles[step]);
        }

        updateStepIndicators(step);
    }

    private void updateStepIndicators(int active) {
        Label[][] steps = {
            { step1Num, step1Label },
            { step2Num, step2Label },
            { step3Num, step3Label },
            { step4Num, step4Label },
            { step5Num, step5Label }
        };
        for (int i = 0; i < STEPS; i++) {
            String cls;
            if (i < active)      cls = "step-completed";
            else if (i == active) cls = "step-active";
            else                  cls = "step-pending";
            steps[i][0].getStyleClass().removeAll("step-completed", "step-active", "step-pending");
            steps[i][0].getStyleClass().add(cls);
            steps[i][1].getStyleClass().removeAll("step-completed", "step-active", "step-pending");
            steps[i][1].getStyleClass().add(cls);
            steps[i][0].setText(i < active ? "\u2713" : String.valueOf(i + 1));
        }
    }

    // ================================================================
    //  STEP 3 — FXML SUB-DIALOG MANAGEMENT
    // ================================================================

    @FXML
    private void onBrowseMainFxml() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Main FXML File");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("FXML", "*.fxml"));
        File f = fc.showOpenDialog(stage);
        if (f != null) {
            mainFxmlField.setText(f.getAbsolutePath());
            // Auto-suggest package based on path
            autoSuggestPackage(f);
        }
    }

    @FXML
    private void onBrowseMainCtrl() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Main Controller Java File");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Java", "*.java"));
        File f = fc.showOpenDialog(stage);
        if (f != null) mainCtrlField.setText(f.getAbsolutePath());
    }

    private void autoSuggestPackage(File fxmlFile) {
        String path = fxmlFile.getAbsolutePath().replace('\\', '/');
        // Try to extract package from src/ or from fxml directory
        int srcIdx = path.indexOf("/src/");
        if (srcIdx >= 0) {
            String sub = path.substring(srcIdx + 5);
            int lastSlash = sub.lastIndexOf('/');
            if (lastSlash > 0) {
                String pkg = sub.substring(0, lastSlash).replace('/', '.');
                if (mainPkgField != null && mainPkgField.getText().isEmpty()) {
                    mainPkgField.setText(pkg);
                }
            }
        }
    }

    @FXML
    private void onAddSubDialog() {
        int idx = subDialogs.size() + 1;
        TextField nameField = new TextField("SubDialog" + idx);
        nameField.setPromptText("Dialog name");
        nameField.setPrefColumnCount(10);

        TextField fxmlField = new TextField();
        fxmlField.setPromptText("Path to .fxml");
        fxmlField.setPrefColumnCount(20);
        Button browseFxml = new Button("...");
        browseFxml.setPrefWidth(30);
        browseFxml.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Sub-Dialog FXML");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("FXML", "*.fxml"));
            File f = fc.showOpenDialog(stage);
            if (f != null) fxmlField.setText(f.getAbsolutePath());
        });

        TextField ctrlField = new TextField();
        ctrlField.setPromptText("Path to Controller .java");
        ctrlField.setPrefColumnCount(20);
        Button browseCtrl = new Button("...");
        browseCtrl.setPrefWidth(30);
        browseCtrl.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Sub-Dialog Controller");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Java", "*.java"));
            File f = fc.showOpenDialog(stage);
            if (f != null) ctrlField.setText(f.getAbsolutePath());
        });

        TextField pkgField = new TextField();
        pkgField.setPromptText("Package");
        pkgField.setPrefColumnCount(15);

        Button removeBtn = new Button("X");
        removeBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-cursor: hand;");

        HBox row = new HBox(4, nameField, fxmlField, browseFxml, ctrlField, browseCtrl, pkgField, removeBtn);
        row.setPadding(new Insets(2, 0, 2, 0));

        SubDialogEntry entry = new SubDialogEntry(row, nameField, fxmlField, ctrlField, pkgField);
        subDialogs.add(entry);

        removeBtn.setOnAction(e -> {
            subDialogs.remove(entry);
            subDialogsContainer.getChildren().remove(row);
        });

        subDialogsContainer.getChildren().add(row);
    }

    @FXML
    private void onBrowseLib() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Library JAR");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JAR", "*.jar"));
        File f = fc.showOpenDialog(stage);
        if (f != null) libPathField.setText(f.getAbsolutePath());
    }

    @FXML
    private void onAddLib() {
        String path = libPathField.getText().trim();
        if (path.isEmpty()) return;
        File f = new File(path);
        if (!f.exists()) return;
        if (!libs.contains(f)) {
            libs.add(f);
            libsListView.getItems().add(f.getName() + "  (" + f.getAbsolutePath() + ")");
        }
        libPathField.clear();
    }

    @FXML
    private void onRemoveLib() {
        int sel = libsListView.getSelectionModel().getSelectedIndex();
        if (sel >= 0 && sel < libs.size()) {
            libs.remove(sel);
            libsListView.getItems().remove(sel);
        }
    }

    // ================================================================
    //  VALIDATION
    // ================================================================

    private boolean validateStep1() {
        if (pluginNameField.getText() == null || pluginNameField.getText().trim().isEmpty()) {
            showAlert("Please enter a Plugin Name.");
            return false;
        }
        if (pluginIdField.getText() == null || pluginIdField.getText().trim().isEmpty()) {
            showAlert("Please enter a Plugin ID.");
            return false;
        }
        if (authorField.getText() == null || authorField.getText().trim().isEmpty()) {
            showAlert("Please enter an Author name.");
            return false;
        }
        return true;
    }

    private boolean validateStep2() {
        if (!chkCommand.isSelected() && !chkToolbar.isSelected() && !chkMenu.isSelected()
            && !chkApkTool.isSelected() && !chkFxml.isSelected()
            && !chkNewMenu.isSelected() && !chkToolbarSection.isSelected() && !chkPanel.isSelected()) {
            showAlert("Select at least one plugin capability.");
            return false;
        }
        return true;
    }

    private boolean validateStep3() {
        if (!chkFxml.isSelected()) return true;
        if (mainFxmlField.getText() == null || mainFxmlField.getText().trim().isEmpty()) {
            showAlert("Select the main FXML file.");
            return false;
        }
        if (mainCtrlField.getText() == null || mainCtrlField.getText().trim().isEmpty()) {
            showAlert("Select the main Controller .java file.");
            return false;
        }
        return true;
    }

    // ================================================================
    //  CODE GENERATION
    // ================================================================

    private void generateCode() {
        String name = pluginNameField.getText().trim();
        String id = pluginIdField.getText().trim();
        String version = versionField.getText().trim();
        if (version.isEmpty()) version = "1.0.0";
        String author = authorField.getText().trim();
        String desc = descArea.getText() == null ? "" : descArea.getText().trim();

        String pkg = "com.eagle.plugin.custom";
        String clsName = toClassName(name);

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import com.eagle.plugin.Plugin;\n");
        sb.append("import com.eagle.plugin.PluginContext;\n");

        if (chkFxml.isSelected()) {
            sb.append("import javafx.fxml.FXMLLoader;\n");
            sb.append("import javafx.scene.Parent;\n");
            sb.append("import javafx.scene.Scene;\n");
            sb.append("import javafx.stage.Modality;\n");
            sb.append("import javafx.stage.Stage;\n");
        }

        if (chkApkTool.isSelected()) {
            sb.append("import com.eagle.controller.EditorController;\n");
            sb.append("import com.eagle.editor.CodeEditor;\n");
            sb.append("import com.eagle.util.SimpleApkBuilder;\n");
            sb.append("import java.io.File;\n");
            sb.append("import java.nio.file.Files;\n");
        }

        if (chkPanel.isSelected()) {
            sb.append("import javafx.scene.layout.VBox;\n");
        }
        sb.append("import javafx.scene.control.Alert;\n");
        sb.append("import javafx.scene.control.Button;\n");
        sb.append("import javafx.scene.control.Tooltip;\n");
        sb.append("\n");
        sb.append("public class ").append(clsName).append(" implements Plugin {\n\n");

        // getId
        sb.append("    @Override\n");
        sb.append("    public String getId() { return \"").append(id).append("\"; }\n\n");

        // getName
        sb.append("    @Override\n");
        sb.append("    public String getName() { return \"").append(escape(name)).append("\"; }\n\n");

        // getVersion
        sb.append("    @Override\n");
        sb.append("    public String getVersion() { return \"").append(escape(version)).append("\"; }\n\n");

        // getAuthor
        sb.append("    @Override\n");
        sb.append("    public String getAuthor() { return \"").append(escape(author)).append("\"; }\n\n");

        // getDescription
        sb.append("    @Override\n");
        sb.append("    public String getDescription() { return \"").append(escape(desc.isEmpty() ? name : desc)).append("\"; }\n\n");

        // init()
        sb.append("    @Override\n");
        sb.append("    public void init(PluginContext ctx) {\n");

        if (chkFxml.isSelected()) {
            sb.append("        // FXML UI — opens main window\n");
            sb.append("        Runnable openUi = () -> openMainWindow();\n");
        }

        // -- Command
        if (chkCommand.isSelected()) {
            String cmdName = cmdNameField.getText().trim();
            if (cmdName.isEmpty()) cmdName = name;
            String category = cmdCategoryField.getText().trim();
            if (category.isEmpty()) category = "Tools";
            sb.append("        // Command Palette\n");
            sb.append("        ctx.registerCommand(\"").append(escape(cmdName)).append("\", \"").append(escape(category)).append("\", () -> {\n");
            if (chkFxml.isSelected()) {
                sb.append("            openUi.run();\n");
            } else {
                sb.append("            showInfo(\"Hello from ").append(escape(name)).append("!\");\n");
            }
            sb.append("        });\n\n");
        }

        // -- Toolbar Button
        if (chkToolbar.isSelected()) {
            String btnText = toolBtnTextField.getText().trim();
            if (btnText.isEmpty()) btnText = id.substring(0, Math.min(3, id.length())).toUpperCase();
            String tooltip = toolTooltipField.getText().trim();
            if (tooltip.isEmpty()) tooltip = name;
            sb.append("        // Toolbar Button\n");
            sb.append("        Button btn = new Button(\"").append(escape(btnText)).append("\");\n");
            sb.append("        btn.setStyle(\"-fx-font-weight: bold; -fx-font-size: 13px;\");\n");
            sb.append("        btn.setTooltip(new Tooltip(\"").append(escape(tooltip)).append("\"));\n");
            sb.append("        btn.setOnAction(e -> ");
            if (chkFxml.isSelected()) {
                sb.append("openUi.run()");
            } else {
                sb.append("showInfo(\"").append(escape(name)).append(" toolbar clicked!\")");
            }
            sb.append(");\n");
            sb.append("        ctx.registerToolbarItem(\"").append(escape(tooltip)).append("\", btn);\n\n");
        }

        // -- Menu Item
        if (chkMenu.isSelected()) {
            String parent = menuParentField.getText().trim();
            if (parent.isEmpty()) parent = "Tools";
            String menuText = menuTextField.getText().trim();
            if (menuText.isEmpty()) menuText = name + "...";
            sb.append("        // Menu Item\n");
            sb.append("        ctx.registerMenuItem(\"").append(escape(parent)).append("\", \"").append(escape(menuText)).append("\", e -> {\n");
            if (chkFxml.isSelected()) {
                sb.append("            openUi.run();\n");
            } else {
                sb.append("            showInfo(\"").append(escape(name)).append(" menu clicked!\");\n");
            }
            sb.append("        });\n");
        }

        // -- APK Tool
        if (chkApkTool.isSelected()) {
            String apkName = apkToolNameField.getText().trim();
            if (apkName.isEmpty()) apkName = name + " to APK";
            String apkDesc = apkToolDescField.getText().trim();
            if (apkDesc.isEmpty()) apkDesc = "Convert to APK using " + name;
            String apkExt = apkToolExtField.getText().trim();
            if (apkExt.isEmpty()) apkExt = "*.*";
            sb.append("        // APK Tool (Welcome Card)\n");
            sb.append("        ctx.registerApkTool(\"").append(escape(apkName)).append("\", \"")
              .append(escape(apkDesc)).append("\", () -> {\n");
            sb.append("            SimpleApkBuilder.showBuildDialog(null, \"").append(escape(apkName))
              .append("\", \"").append(escape(apkExt)).append("\", config -> {\n");
            sb.append("                File assetsDir = new File(config.workDir, \"assets\");\n");
            sb.append("                assetsDir.mkdirs();\n");
            sb.append("                Files.copy(config.sourceFile.toPath(),\n");
            sb.append("                    new File(assetsDir, config.sourceFile.getName()).toPath(),\n");
            sb.append("                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);\n");
            sb.append("                // Build pipeline runs automatically after this callback\n");
            sb.append("            });\n");
            sb.append("        });\n");
        }

        // -- New Top-Level Menu
        if (chkNewMenu.isSelected()) {
            String menuTitle = newMenuTitleField.getText().trim();
            if (menuTitle.isEmpty()) menuTitle = name;
            int menuPos = -1;
            try { menuPos = Integer.parseInt(newMenuPosField.getText().trim()); } catch (Exception ignored) { }
            sb.append("        // New Top-Level Menu\n");
            sb.append("        ctx.registerNewMenu(\"").append(escape(menuTitle)).append("\", ").append(menuPos).append(");\n\n");
        }

        // -- New Toolbar Section
        if (chkToolbarSection.isSelected()) {
            String sectionName = toolSectionNameField.getText().trim();
            if (sectionName.isEmpty()) sectionName = name;
            String[] btnLabels = toolBtnLabelsField.getText().trim().split("\\s*,\\s*");
            if (btnLabels.length == 0 || (btnLabels.length == 1 && btnLabels[0].isEmpty())) {
                btnLabels = new String[]{name};
            }
            sb.append("        // New Toolbar Section\n");
            sb.append("        {\n");
            int bi = 0;
            for (String lbl : btnLabels) {
                if (lbl.trim().isEmpty()) continue;
                String varName = "toolBtn" + (bi++);
                String shortLbl = lbl.trim().length() > 4 ? lbl.trim().substring(0, 4).toUpperCase() : lbl.trim().toUpperCase();
                sb.append("            Button ").append(varName).append(" = new Button(\"").append(escape(lbl.trim())).append("\");\n");
                sb.append("            ").append(varName).append(".setTooltip(new Tooltip(\"").append(escape(lbl.trim())).append("\"));\n");
                sb.append("            ").append(varName).append(".setOnAction(e -> ");
                if (chkFxml.isSelected()) {
                    sb.append("openUi.run()");
                } else {
                    sb.append("showInfo(\"").append(escape(lbl.trim())).append(" clicked!\")");
                }
                sb.append(");\n");
            }
            sb.append("            ctx.registerToolbarSection(\"").append(escape(sectionName)).append("\"");
            for (int i = 0; i < bi; i++) {
                sb.append(", toolBtn").append(i);
            }
            sb.append(");\n");
            sb.append("        }\n\n");
        }

        // -- Plugin Side Panel
        if (chkPanel.isSelected()) {
            String panelTitle = panelTitleField.getText().trim();
            if (panelTitle.isEmpty()) panelTitle = name;
            sb.append("        // Plugin Side Panel\n");
            sb.append("        VBox panel = new VBox(8);\n");
            sb.append("        panel.setPadding(new javafx.geometry.Insets(10));\n");
            sb.append("        panel.setStyle(\"-fx-background-color: -bg-secondary;\");\n");
            sb.append("        Label panelTitle = new Label(\"").append(escape(panelTitle)).append("\");\n");
            sb.append("        panelTitle.setStyle(\"-fx-font-weight: bold; -fx-font-size: 14px;\");\n");
            sb.append("        panel.getChildren().add(panelTitle);\n");
            if (chkFxml.isSelected()) {
                sb.append("        Button openMainBtn = new Button(\"Open ").append(escape(name)).append("\");\n");
                sb.append("        openMainBtn.setOnAction(e -> openUi.run());\n");
                sb.append("        panel.getChildren().add(openMainBtn);\n");
            } else {
                sb.append("        Label info = new Label(\"").append(escape(name)).append(" panel content.\");\n");
                sb.append("        panel.getChildren().add(info);\n");
            }
            sb.append("        ctx.registerPanel(\"").append(escape(panelTitle)).append("\", panel);\n\n");
        }

        sb.append("    }\n\n");

        // shutdown()
        sb.append("    @Override\n");
        sb.append("    public void shutdown() {}\n\n");

        // FXML UI methods
        if (chkFxml.isSelected()) {
            String mainPkg = mainPkgField.getText().trim();
            if (mainPkg.isEmpty()) mainPkg = pkg;
            String mainFxmlName = new File(mainFxmlField.getText().trim()).getName();

            // Main window opener
            sb.append("    private void openMainWindow() {\n");
            sb.append("        try {\n");
            sb.append("            FXMLLoader loader = new FXMLLoader(\n");
            sb.append("                getClass().getResource(\"").append(mainFxmlName).append("\")\n");
            sb.append("            );\n");
            sb.append("            Parent root = loader.load();\n");
            sb.append("            Object ctrl = loader.getController();\n");
            sb.append("            Stage stage = new Stage();\n");
            sb.append("            stage.setTitle(\"").append(escape(name)).append("\");\n");
            sb.append("            stage.initModality(Modality.APPLICATION_MODAL);\n");
            sb.append("            Scene scene = new Scene(root, 600, 450);\n");
            sb.append("            com.eagle.util.ThemeManager.getInstance().applyTheme(scene);\n");
            sb.append("            stage.setScene(scene);\n");
            // Try to call setStage(Stage) on controller
            sb.append("            try { ctrl.getClass().getMethod(\"setStage\", Stage.class).invoke(ctrl, stage); } catch (Exception ignored) {}\n");
            sb.append("            stage.showAndWait();\n");
            sb.append("        } catch (Exception e) {\n");
            sb.append("            e.printStackTrace();\n");
            sb.append("            showInfo(\"Failed to open UI: \" + e.getMessage());\n");
            sb.append("        }\n");
            sb.append("    }\n\n");

            // Sub-dialog openers
            for (SubDialogEntry entry : subDialogs) {
                String subName = entry.nameField.getText().trim();
                String subFxml = new File(entry.fxmlField.getText().trim()).getName();
                sb.append("    private void open").append(toClassName(subName)).append("() {\n");
                sb.append("        try {\n");
                sb.append("            FXMLLoader loader = new FXMLLoader(\n");
                sb.append("                getClass().getResource(\"").append(subFxml).append("\")\n");
                sb.append("            );\n");
                sb.append("            Parent root = loader.load();\n");
                sb.append("            Object ctrl = loader.getController();\n");
                sb.append("            Stage stage = new Stage();\n");
                sb.append("            stage.setTitle(\"").append(escape(subName)).append("\");\n");
                sb.append("            stage.initModality(Modality.APPLICATION_MODAL);\n");
                sb.append("            Scene scene = new Scene(root, 500, 400);\n");
                sb.append("            com.eagle.util.ThemeManager.getInstance().applyTheme(scene);\n");
                sb.append("            stage.setScene(scene);\n");
                sb.append("            try { ctrl.getClass().getMethod(\"setStage\", Stage.class).invoke(ctrl, stage); } catch (Exception ignored) {}\n");
                sb.append("            stage.showAndWait();\n");
                sb.append("        } catch (Exception e) {\n");
                sb.append("            e.printStackTrace();\n");
                sb.append("            showInfo(\"Failed to open ").append(escape(subName)).append(": \" + e.getMessage());\n");
                sb.append("        }\n");
                sb.append("    }\n\n");
            }
        }

        // showInfo helper
        sb.append("    private void showInfo(String msg) {\n");
        sb.append("        javafx.application.Platform.runLater(() -> {\n");
        sb.append("            Alert a = new Alert(Alert.AlertType.INFORMATION);\n");
        sb.append("            a.setTitle(getName());\n");
        sb.append("            a.setHeaderText(null);\n");
        sb.append("            a.setContentText(msg);\n");
        sb.append("            a.showAndWait();\n");
        sb.append("        });\n");
        sb.append("    }\n");
        sb.append("}\n");

        codePreview.setText(sb.toString());
    }

    // ================================================================
    //  BUILD & DEPLOY
    // ================================================================

    @FXML
    private void onBrowseJdk() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select javac.exe");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Executable", "javac.exe"));
        File f = fc.showOpenDialog(stage);
        if (f != null) jdkPathField.setText(f.getAbsolutePath());
    }

    @FXML
    private void onBuild() {
        buildLog.clear();
        buildBtn.setDisable(true);
        buildLog.setText("Building plugin...\n");

        try {
            String jdkPath = jdkPathField.getText().trim();
            if (jdkPath.isEmpty() || !new File(jdkPath).exists()) {
                appendLog("ERROR: javac not found. Set the JDK path.");
                buildBtn.setDisable(false);
                return;
            }

            String id = pluginIdField.getText().trim();
            String name = pluginNameField.getText().trim();
            String pkg = "com.eagle.plugin.custom";

            // Extract class name from the source code
            String source = codePreview.getText();
            String actualClass = toClassName(name);
            java.util.regex.Matcher cm = java.util.regex.Pattern.compile("public\\s+class\\s+(\\w+)").matcher(source);
            if (cm.find()) actualClass = cm.group(1);

            buildDir = new File(System.getProperty("java.io.tmpdir"), "eagle-plugin-builder/" + id);
            deleteDir(buildDir);
            File srcDir = new File(buildDir, "src/" + pkg.replace('.', '/'));
            srcDir.mkdirs();
            File metaInfDir = new File(buildDir, "META-INF");
            metaInfDir.mkdirs();

            // --- Write Plugin Java source ---
            File sourceFile = new File(srcDir, actualClass + ".java");
            try (PrintWriter pw = new PrintWriter(sourceFile, "UTF-8")) {
                pw.print(source);
            }
            appendLog("\u2713 Plugin source written: " + sourceFile.getName());

            // --- Copy FXML files and Controller .java files ---
            if (chkFxml.isSelected()) {
                copyResourceToSrc(mainFxmlField.getText().trim(), srcDir, buildLog);
                copyResourceToSrc(mainCtrlField.getText().trim(), srcDir, buildLog);
                for (SubDialogEntry entry : subDialogs) {
                    copyResourceToSrc(entry.fxmlField.getText().trim(), srcDir, buildLog);
                    copyResourceToSrc(entry.ctrlField.getText().trim(), srcDir, buildLog);
                }
            }

            // --- Copy library JARs ---
            File libsDir = new File(buildDir, "libs");
            for (File lib : libs) {
                File target = new File(libsDir, lib.getName());
                target.getParentFile().mkdirs();
                copyFile(lib, target);
                appendLog("\u2713 Library copied: " + lib.getName());
            }

            // --- Write MANIFEST.MF ---
            String classPath = ".";
            if (!libs.isEmpty()) {
                StringBuilder cp = new StringBuilder(".");
                for (File lib : libs) cp.append(" libs/").append(lib.getName());
                classPath = cp.toString();
            }

            File manifestFile = new File(metaInfDir, "MANIFEST.MF");
            try (PrintWriter pw = new PrintWriter(manifestFile, "UTF-8")) {
                pw.println("Manifest-Version: 1.0");
                pw.println("Plugin-Class: " + pkg + "." + actualClass);
                pw.println("Class-Path: " + classPath);
            }
            appendLog("\u2713 Manifest written");

            // --- Build classpath ---
            String appDir = System.getProperty("user.dir");
            String classpath = appDir + "/build/classes";
            File distLib = new File(appDir, "dist/lib");
            if (distLib.exists()) {
                File[] jars = distLib.listFiles((d, n) -> n.endsWith(".jar"));
                if (jars != null) {
                    for (File j : jars) classpath += ";" + j.getAbsolutePath();
                }
            }
            // Add libs to classpath for compilation
            for (File lib : libs) classpath += ";" + lib.getAbsolutePath();

            String jfxrt = findJfxrt();
            if (jfxrt != null) classpath += ";" + jfxrt;

            // --- Compile ---
            File classesDir = new File(buildDir, "classes");
            classesDir.mkdirs();

            // Collect all .java files to compile
            List<File> javaFiles = new ArrayList<>();
            collectJavaFiles(srcDir, javaFiles);

            if (javaFiles.isEmpty()) {
                appendLog("ERROR: No Java source files found to compile.");
                buildBtn.setDisable(false);
                return;
            }

            appendLog("\nCompiling " + javaFiles.size() + " source files...");
            // Build the javac command
            List<String> cmdList = new ArrayList<>();
            cmdList.add(jdkPath);
            cmdList.add("-encoding");
            cmdList.add("UTF-8");
            cmdList.add("-cp");
            cmdList.add(classpath);
            cmdList.add("-d");
            cmdList.add(classesDir.getAbsolutePath());
            for (File jf : javaFiles) cmdList.add(jf.getAbsolutePath());

            int exitCode = runProcess(cmdList.toArray(new String[0]));

            if (exitCode != 0) {
                appendLog("ERROR: Compilation failed. Check the log above.");
                buildBtn.setDisable(false);
                return;
            }
            appendLog("\u2713 Compilation successful");

            // --- Create JAR ---
            appendLog("\nPackaging JAR...");
            String jarPath = findJar(jdkPath);
            if (jarPath == null) {
                appendLog("ERROR: jar.exe not found.");
                buildBtn.setDisable(false);
                return;
            }

            outputJar = new File(buildDir, id + ".jar");

            // Build jar command: jar cfm <jar> <manifest> -C <classes> . (include libs + fxml)
            // We'll include additional files by adding them individually
            List<String> jarCmdList = new ArrayList<>();
            jarCmdList.add(jarPath);
            jarCmdList.add("cfm");
            jarCmdList.add(outputJar.getAbsolutePath());
            jarCmdList.add(manifestFile.getAbsolutePath());

            // Add compiled classes
            jarCmdList.add("-C");
            jarCmdList.add(classesDir.getAbsolutePath());
            jarCmdList.add(".");

            // Add FXML files (copied to src dir)
            if (chkFxml.isSelected()) {
                jarCmdList.add("-C");
                jarCmdList.add(srcDir.getAbsolutePath());
                jarCmdList.add(".");
            }

            // Add libs
            if (libsDir.exists()) {
                jarCmdList.add("-C");
                jarCmdList.add(buildDir.getAbsolutePath());
                jarCmdList.add("libs");
            }

            exitCode = runProcess(jarCmdList.toArray(new String[0]));
            if (exitCode != 0) {
                appendLog("ERROR: Failed to create JAR.");
                buildBtn.setDisable(false);
                return;
            }
            appendLog("\u2713 JAR created: " + outputJar.getAbsolutePath());

            // --- Deploy to plugins/ ---
            File pluginsDir = new File(appDir, "plugins");
            if (!pluginsDir.exists()) pluginsDir.mkdirs();
            File targetJar = new File(pluginsDir, outputJar.getName());
            copyFile(outputJar, targetJar);
            appendLog("\n\u2705 PLUGIN DEPLOYED SUCCESSFULLY!");
            appendLog("   Location: " + targetJar.getAbsolutePath());

            // Copy libs alongside plugin JAR
            if (!libs.isEmpty()) {
                File pluginsLibsDir = new File(pluginsDir, "libs");
                pluginsLibsDir.mkdirs();
                for (File lib : libs) {
                    copyFile(lib, new File(pluginsLibsDir, lib.getName()));
                }
                appendLog("   Libraries copied to: " + new File(pluginsDir, "libs").getAbsolutePath());
            }

            appendLog("\nRestart the application or go to");
            appendLog("Tools \u2192 Plugin Manager to enable it.");

            openFolderLink.setVisible(true);

        } catch (Exception e) {
            appendLog("ERROR: " + e.getMessage());
            e.printStackTrace();
        } finally {
            buildBtn.setDisable(false);
        }
    }

    @FXML
    private void onOpenFolder() {
        if (outputJar != null && outputJar.getParentFile().exists()) {
            try {
                java.awt.Desktop.getDesktop().open(outputJar.getParentFile());
            } catch (Exception ignored) { }
        }
    }

    @FXML
    private void onOpenPluginsFolder() {
        File pluginsDir = new File(System.getProperty("user.dir"), "plugins");
        if (pluginsDir.exists()) {
            try {
                java.awt.Desktop.getDesktop().open(pluginsDir);
            } catch (Exception ignored) { }
        }
    }

    // ================================================================
    //  HELPERS
    // ================================================================

    private void appendLog(String line) {
        buildLog.appendText(line + "\n");
    }

    private int runProcess(String... cmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                appendLog("  " + line);
            }
        }
        return p.waitFor();
    }

    private void detectJdk() {
        String javaHome = System.getProperty("java.home");
        String[] candidates = {
            javaHome + "/bin/javac.exe",
            javaHome + "/../bin/javac.exe",
            javaHome + "/../../bin/javac.exe"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists()) { jdkPathField.setText(f.getAbsolutePath()); return; }
        }
        String javahome = System.getenv("JAVA_HOME");
        if (javahome != null) {
            File f = new File(javahome + "/bin/javac.exe");
            if (f.exists()) { jdkPathField.setText(f.getAbsolutePath()); return; }
        }
    }

    private String findJfxrt() {
        String javaHome = System.getProperty("java.home");
        String[] candidates = {
            javaHome + "/lib/ext/jfxrt.jar",
            javaHome + "/jre/lib/ext/jfxrt.jar",
            javaHome + "/../jre/lib/ext/jfxrt.jar",
            javaHome + "/../../jre/lib/ext/jfxrt.jar"
        };
        for (String c : candidates) {
            if (new File(c).exists()) return c;
        }
        return null;
    }

    private String findJar(String jdkPath) {
        if (jdkPath != null && !jdkPath.isEmpty()) {
            String jar = jdkPath.replace("javac.exe", "jar.exe").replace("javac", "jar");
            if (new File(jar).exists()) return jar;
        }
        String javaHome = System.getProperty("java.home");
        String[] candidates = {
            javaHome + "/bin/jar.exe",
            javaHome + "/../bin/jar.exe"
        };
        for (String c : candidates) {
            if (new File(c).exists()) return c;
        }
        return null;
    }

    /**
     * Copy a resource (FXML or .java file) into the build's src directory
     * so it ends up in the JAR at the correct path.
     */
    private void copyResourceToSrc(String path, File srcDir, TextArea log) {
        if (path == null || path.trim().isEmpty()) return;
        File file = new File(path.trim());
        if (!file.exists()) {
            appendLog("  Warning: file not found: " + path);
            return;
        }
        try {
            File target = new File(srcDir, file.getName());
            copyFile(file, target);
            appendLog("\u2713 Copied: " + file.getName());
        } catch (IOException e) {
            appendLog("  Warning: failed to copy " + file.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Recursively collect .java files from a directory.
     */
    private void collectJavaFiles(File dir, List<File> result) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                collectJavaFiles(f, result);
            } else if (f.getName().endsWith(".java")) {
                result.add(f);
            }
        }
    }

    private void copyFile(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
    }

    private void deleteDir(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) for (File f : files) {
            if (f.isDirectory()) deleteDir(f);
            else f.delete();
        }
        dir.delete();
    }

    private String toClassName(String name) {
        String cleaned = name.replaceAll("[^a-zA-Z0-9\\s]", "");
        String[] parts = cleaned.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1).toLowerCase());
        }
        String result = sb.toString();
        return result.isEmpty() ? "MyPlugin" : result;
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("Validation");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
