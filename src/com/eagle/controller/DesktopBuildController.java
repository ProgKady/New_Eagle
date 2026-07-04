package com.eagle.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class DesktopBuildController implements Initializable {

    @FXML private JFXTextField appName;
    @FXML private JFXTextField packageName;
    @FXML private JFXTextField description;
    @FXML private JFXTextField sourceDir;
    @FXML private ImageView imgview;
    @FXML private Label pathlabel;
    @FXML private JFXButton create;
    @FXML private JFXButton electronBtn;
    @FXML private JFXButton tauriBtn;

    private File selectedIcon;
    private boolean useTauri;
    private TextArea logArea;
    private Stage progressStage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setDefaultIcon();
        useTauri = false;
        appName.textProperty().addListener((o, a, b) -> validate());
        packageName.textProperty().addListener((o, a, b) -> validate());
        sourceDir.textProperty().addListener((o, a, b) -> validate());
    }

    private void setDefaultIcon() {
        try {
            InputStream is = getClass().getResourceAsStream("/com/eagle/icons/image.png");
            if (is != null) {
                byte[] iconBytes = readAllBytes(is);
                is.close();
                imgview.setImage(new Image(new java.io.ByteArrayInputStream(iconBytes)));
                File tempIcon = File.createTempFile("eagle_default_icon_", ".png");
                tempIcon.deleteOnExit();
                Files.write(tempIcon.toPath(), iconBytes);
                pathlabel.setText(tempIcon.getAbsolutePath());
            }
        } catch (Exception e) { }
    }

    @FXML void imgviewact(MouseEvent event) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png"));
        fc.setTitle("Select App Icon");
        File f = fc.showOpenDialog(imgview.getScene().getWindow());
        if (f != null) {
            selectedIcon = f;
            imgview.setImage(new Image(f.toURI().toString()));
            pathlabel.setText(f.getAbsolutePath());
            pathlabel.setVisible(true);
        }
    }

    @FXML void browseSource(ActionEvent event) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select Web Project Folder");
        File f = dc.showDialog(sourceDir.getScene().getWindow());
        if (f != null) {
            sourceDir.setText(f.getAbsolutePath());
            if (appName.getText().trim().isEmpty()) appName.setText(f.getName());
            if (packageName.getText().trim().isEmpty()) {
                packageName.setText("com." + f.getName().toLowerCase().replaceAll("[^a-z0-9]", "") + ".app");
            }
            validate();
        }
    }

    @FXML void selectElectron(ActionEvent event) {
        useTauri = false;
        electronBtn.setStyle("-fx-background-color: -accent; -fx-background-radius: 8; -fx-padding: 8 20;");
        tauriBtn.setStyle("-fx-background-color: #6b6d80; -fx-background-radius: 8; -fx-padding: 8 26;");
    }

    @FXML void selectTauri(ActionEvent event) {
        useTauri = true;
        tauriBtn.setStyle("-fx-background-color: -accent; -fx-background-radius: 8; -fx-padding: 8 26;");
        electronBtn.setStyle("-fx-background-color: #6b6d80; -fx-background-radius: 8; -fx-padding: 8 20;");
    }

    public void preselectSource(File dir) {
        sourceDir.setText(dir.getAbsolutePath());
        appName.setText(dir.getName());
        packageName.setText("com." + dir.getName().toLowerCase().replaceAll("[^a-z0-9]", "") + ".app");
        validate();
    }

    private void validate() {
        boolean ok = !sourceDir.getText().isEmpty()
            && !appName.getText().trim().isEmpty()
            && !packageName.getText().trim().isEmpty();
        create.setDisable(!ok);
    }

    @FXML void createact(ActionEvent event) {
        String name = appName.getText().trim();
        String pkg = packageName.getText().trim();
        String desc = description.getText().trim();
        String src = sourceDir.getText().trim();
        File srcDir = new File(src);
        if (!srcDir.isDirectory()) {
            showAlert("Invalid Source", "Selected source is not a valid directory.");
            return;
        }
        // Create progress stage on JavaFX thread, then launch build in background
        progressStage = buildProgressStage(useTauri ? "Building Tauri Desktop App..." : "Building Electron Desktop App...");
        new Thread(() -> {
            try {
                log("Generating wrapper files...");
                if (useTauri) {
                    generateTauri(name, pkg, desc, srcDir);
                } else {
                    generateElectron(name, pkg, desc, srcDir);
                }
                log("Installing npm dependencies...");
                runProcess(srcDir, "npm", "install");
                if (useTauri) {
                    log("Building Tauri app (this may take a while)...");
                    runProcess(srcDir, "npx", "tauri", "build");
                } else {
                    log("Building Electron app (this may take a while)...");
                    runProcess(srcDir, "npx", "electron-builder");
                }
                Platform.runLater(() -> {
                    if (progressStage != null) progressStage.close();
                    String outputDir = srcDir + (useTauri ? "/src-tauri/target/release" : "/dist");
                    showAlert("Build Complete",
                        "Desktop app built successfully!\n\nOutput: " + new File(outputDir).getAbsolutePath()
                        + "\n\n" + (useTauri
                            ? "Installer will be in src-tauri/target/release/bundle/"
                            : "Installer will be in dist/"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (progressStage != null) progressStage.close();
                    showAlert("Build Failed", e.getMessage());
                });
            }
        }).start();
    }

    private Stage buildProgressStage(String title) {
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(create.getScene().getWindow());
        stage.setWidth(640);
        stage.setHeight(480);
        VBox root = new VBox(8);
        root.setStyle("-fx-padding: 14; -fx-background: #1e1e2e;");
        ProgressBar bar = new ProgressBar(-1);
        bar.setPrefWidth(Double.MAX_VALUE);
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setStyle("-fx-control-inner-background: #111; -fx-text-fill: #0f0; -fx-font-family: monospace; -fx-font-size: 12px;");
        root.getChildren().addAll(bar, logArea);
        Scene sc = new Scene(root);
        com.eagle.util.ThemeManager.getInstance().applyTheme(sc);
        stage.setScene(sc);
        stage.show();
        return stage;
    }

    private String resolveTool(String tool) {
        String path = com.eagle.util.ToolsConfig.getToolPath(tool);
        if (path != null) return path;
        // Check in node's directory (user likely configured node.path not npm.path)
        String[] dirsToCheck = {
            com.eagle.util.ToolsConfig.getToolPath("node"),
            com.eagle.util.ToolsConfig.getToolPath("npm"),
            com.eagle.util.ToolsConfig.getToolPath("npx")
        };
        for (String p : dirsToCheck) {
            if (p == null) continue;
            File dir = new File(p).isFile() ? new File(p).getParentFile() : new File(p);
            if (dir.isDirectory()) {
                File s = new File(dir, tool + ".exe");
                if (s.exists()) return s.getAbsolutePath();
                s = new File(dir, tool + ".cmd");
                if (s.exists()) return s.getAbsolutePath();
                s = new File(dir, tool);
                if (s.exists()) return s.getAbsolutePath();
            }
        }
        return tool; // fallback to PATH
    }

    private void runProcess(File workDir, String... cmd) throws Exception {
        // Resolve first command if it's npm/npx
        String[] resolved = cmd.clone();
        if (cmd.length > 0) {
            String t = cmd[0].toLowerCase();
            if (t.equals("npm") || t.equals("npx") || t.equals("node")) {
                resolved[0] = resolveTool(cmd[0]);
            }
        }
        log("> " + String.join(" ", resolved));
        ProcessBuilder pb = new ProcessBuilder(resolved);
        pb.directory(workDir);
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        try (java.io.BufferedReader r = new java.io.BufferedReader(
                new java.io.InputStreamReader(proc.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                log(line);
            }
        }
        int code = proc.waitFor();
        if (code != 0) {
            throw new Exception("Process exited with code " + code + " (see log for details)");
        }
    }

    private void log(String msg) {
        Platform.runLater(() -> {
            if (logArea != null) logArea.appendText(msg + "\n");
        });
    }

    private void generateElectron(String name, String pkg, String desc, File srcDir) throws Exception {
        File mainJs = new File(srcDir, "main.js");
        if (!mainJs.exists()) {
            String mainJsContent =
                "const { app, BrowserWindow } = require('electron');\n"
                + "const path = require('path');\n\n"
                + "function createWindow() {\n"
                + "  const win = new BrowserWindow({\n"
                + "    width: 1200, height: 800,\n"
                + "    webPreferences: { preload: path.join(__dirname, 'preload.js') }\n"
                + "  });\n"
                + "  win.loadFile(path.join(__dirname, 'index.html'));\n"
                + "}\n\n"
                + "app.whenReady().then(createWindow);\n"
                + "app.on('window-all-closed', () => { if (process.platform !== 'darwin') app.quit(); });\n"
                + "app.on('activate', () => { if (BrowserWindow.getAllWindows().length === 0) createWindow(); });\n";
            Files.write(mainJs.toPath(), mainJsContent.getBytes("UTF-8"));
        }
        File preloadJs = new File(srcDir, "preload.js");
        if (!preloadJs.exists()) {
            Files.write(preloadJs.toPath(),
                ("// Preload script\nconst { contextBridge } = require('electron');\n"
                + "contextBridge.exposeInMainWorld('appInfo', { name: '" + name + "', version: '1.0.0' });\n")
                .getBytes("UTF-8"));
        }
        File pkgFile = new File(srcDir, "package.json");
        String json;
        if (pkgFile.exists()) {
            json = new String(Files.readAllBytes(pkgFile.toPath()), "UTF-8");
        } else {
            json = "{\n  \"name\": \"" + name.toLowerCase().replaceAll("\\s+", "-") + "\",\n"
                + "  \"version\": \"1.0.0\",\n  \"private\": true\n}\n";
        }
        if (!json.contains("\"electron\"")) {
            json = json.replace("\"dependencies\": {", "\"devDependencies\": {");
            int idx = json.lastIndexOf("}");
            json = json.substring(0, idx) + ",\n  \"electron\": \"^28.0.0\"\n" + json.substring(idx);
            json = json.replaceFirst("\"devDependencies\":", "\"dependencies\":");
        }
        if (!json.contains("\"main\"")) {
            json = json.substring(0, json.lastIndexOf("}")) + ",\n  \"main\": \"main.js\"\n" + json.substring(json.lastIndexOf("}"));
        }
        if (!json.contains("electron .")) {
            json = json.replace("\"scripts\": {", "\"scripts\": {\n    \"electron\": \"electron .\",");
        }
        if (!json.contains("\"electron-builder\"")) {
            int idx2 = json.lastIndexOf("}");
            json = json.substring(0, idx2) + ",\n  \"electron-builder\": \"^24.0.0\"\n" + json.substring(idx2);
        }
        if (!json.contains("electron-builder")) {
            json = json.replace("\"scripts\": {", "\"scripts\": {\n    \"build\": \"electron-builder\",");
        }
        if (!json.contains("\"author\"")) {
            json = json.substring(0, json.lastIndexOf("}")) + ",\n  \"author\": \"" + name + "\",\n"
                + "  \"description\": \"" + (desc.isEmpty() ? name + " Desktop App" : desc) + "\"\n"
                + json.substring(json.lastIndexOf("}"));
        }
        if (!json.contains("\"appId\"")) {
            json = json.substring(0, json.lastIndexOf("}")) + ",\n  \"build\": {\n"
                + "    \"appId\": \"" + pkg + "\",\n"
                + "    \"productName\": \"" + name + "\",\n"
                + "    \"directories\": { \"output\": \"dist\" },\n"
                + "    \"win\": { \"target\": \"nsis\" },\n"
                + "    \"mac\": { \"target\": \"dmg\" },\n"
                + "    \"linux\": { \"target\": \"AppImage\" }\n"
                + "  }\n" + json.substring(json.lastIndexOf("}"));
        }
        Files.write(pkgFile.toPath(), json.getBytes("UTF-8"));
        if (pathlabel.getText() != null && !pathlabel.getText().isEmpty()) {
            File iconFile = new File(pathlabel.getText());
            if (iconFile.exists()) {
                Files.copy(iconFile.toPath(), new File(srcDir, "icon.png").toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void generateTauri(String name, String pkg, String desc, File srcDir) throws Exception {
        File srcTauri = new File(srcDir, "src-tauri");
        srcTauri.mkdirs();
        new File(srcTauri, "src").mkdirs();
        new File(srcTauri, "icons").mkdirs();
        File cargo = new File(srcTauri, "Cargo.toml");
        if (!cargo.exists()) {
            Files.write(cargo.toPath(),
                ("[package]\nname = \"" + name.toLowerCase().replaceAll("\\s+", "_") + "\"\n"
                + "version = \"0.1.0\"\nedition = \"2021\"\n"
                + "description = \"" + (desc.isEmpty() ? name + " Desktop App" : desc) + "\"\n\n"
                + "[build-dependencies]\ntauri-build = { version = \"1.5\", features = [] }\n\n"
                + "[dependencies]\ntauri = { version = \"1.6\", features = [\"shell-open\"] }\n"
                + "serde = { version = \"1.0\", features = [\"derive\"] }\n"
                + "serde_json = \"1.0\"\n\n"
                + "[features]\ndefault = [\"custom-protocol\"]\n"
                + "custom-protocol = [\"tauri/custom-protocol\"]\n").getBytes("UTF-8"));
        }
        String devPath = "http://localhost:5173";
        File pjFile = new File(srcDir, "package.json");
        if (pjFile.exists()) {
            String pj = new String(Files.readAllBytes(pjFile.toPath()), "UTF-8");
            if (pj.contains("next") || pj.contains("nuxt")) devPath = "http://localhost:3000";
            else if (pj.contains("angular")) devPath = "http://localhost:4200";
        }
        File conf = new File(srcTauri, "tauri.conf.json");
        if (!conf.exists()) {
            Files.write(conf.toPath(),
                ("{\n  \"build\": {\n    \"beforeBuildCommand\": \"npm run build\",\n"
                + "    \"beforeDevCommand\": \"npm run dev\",\n"
                + "    \"devPath\": \"" + devPath + "\",\n"
                + "    \"distDir\": \"../dist\"\n  },\n"
                + "  \"package\": { \"productName\": \"" + name + "\", \"version\": \"0.1.0\" },\n"
                + "  \"tauri\": {\n"
                + "    \"allowlist\": { \"all\": false, \"shell\": { \"open\": true } },\n"
                + "    \"bundle\": {\n"
                + "      \"identifier\": \"" + pkg + "\",\n"
                + "      \"icon\": [\"icons/icon.png\"]\n    }\n  }\n}\n").getBytes("UTF-8"));
        }
        File mainRs = new File(srcTauri, "src/main.rs");
        if (!mainRs.exists()) {
            Files.write(mainRs.toPath(),
                ("#![cfg_attr(not(debug_assertions), windows_subsystem = \"windows\")]\n\n"
                + "fn main() {\n  tauri::Builder::default()\n"
                + "    .run(tauri::generate_context!())\n"
                + "    .expect(\"error while running tauri\");\n}\n").getBytes("UTF-8"));
        }
        File buildRs = new File(srcTauri, "build.rs");
        if (!buildRs.exists()) {
            Files.write(buildRs.toPath(), "fn main() {\n  tauri_build::build()\n}\n".getBytes("UTF-8"));
        }
        if (pjFile.exists()) {
            String json = new String(Files.readAllBytes(pjFile.toPath()), "UTF-8");
            if (!json.contains("\"@tauri-apps/cli\"")) {
                json = json.replace("\"devDependencies\"", "\"dependencies\"");
                int idx = json.lastIndexOf("}");
                json = json.substring(0, idx) + ",\n  \"@tauri-apps/cli\": \"^1.5.0\"\n" + json.substring(idx);
                json = json.replaceFirst("\"dependencies\"", "\"devDependencies\"");
                Files.write(pjFile.toPath(), json.getBytes("UTF-8"));
            }
        }
        if (pathlabel.getText() != null && !pathlabel.getText().isEmpty()) {
            File iconFile = new File(pathlabel.getText());
            if (iconFile.exists()) {
                Files.copy(iconFile.toPath(), new File(srcTauri, "icons/icon.png").toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void showAlert(String title, String msg) {
        if (Platform.isFxApplicationThread()) {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle(title);
            a.setHeaderText(null);
            a.setContentText(msg);
            a.showAndWait();
        } else {
            Platform.runLater(() -> {
                Alert a = new Alert(Alert.AlertType.INFORMATION);
                a.setTitle(title);
                a.setHeaderText(null);
                a.setContentText(msg);
                a.showAndWait();
            });
        }
    }

    private byte[] readAllBytes(InputStream is) throws java.io.IOException {
        java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
        byte[] tmp = new byte[4096];
        int n;
        while ((n = is.read(tmp)) != -1) buf.write(tmp, 0, n);
        return buf.toByteArray();
    }
}
