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

public class AndroidBuildController implements Initializable {

    @FXML private JFXTextField appName;
    @FXML private JFXTextField packageName;
    @FXML private JFXTextField description;
    @FXML private JFXTextField sourceDir;
    @FXML private ImageView imgview;
    @FXML private Label pathlabel;
    @FXML private JFXButton create;

    private File selectedIcon;
    private TextArea logArea;
    private Stage progressStage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
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
        appName.textProperty().addListener((o, a, b) -> validate());
        packageName.textProperty().addListener((o, a, b) -> validate());
        sourceDir.textProperty().addListener((o, a, b) -> validate());
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
        progressStage = buildProgressStage("Building Android App...");
        new Thread(() -> {
            try {
                log("Generating Capacitor wrapper files...");
                generateCapacitor(name, pkg, desc, srcDir);
                log("Installing npm dependencies...");
                runProcess(srcDir, "npm", "install");
                log("Syncing Capacitor Android platform...");
                runProcess(srcDir, "npx", "cap", "sync", "android");
                log("Building Android APK with Gradle...");
                File androidDir = new File(srcDir, "android");
                if (!androidDir.isDirectory()) {
                    log("Android platform not found; running npx cap add android...");
                    runProcess(srcDir, "npx", "cap", "add", "android");
                }
                File androidDir2 = new File(srcDir, "android");
                if (androidDir2.isDirectory()) {
                    String gradlew = System.getProperty("os.name").toLowerCase().contains("win")
                        ? "gradlew.bat" : "./gradlew";
                    runProcess(androidDir2, gradlew, "assembleDebug");
                }
                Platform.runLater(() -> {
                    if (progressStage != null) progressStage.close();
                    String apkPath = srcDir + "/android/app/build/outputs/apk/debug/app-debug.apk";
                    File apkFile = new File(apkPath);
                    if (apkFile.exists()) {
                        showAlert("Build Complete", "APK built successfully!\n\nLocation: " + apkFile.getAbsolutePath());
                    } else {
                        showAlert("Build Complete", "Android project created.\nAPK should be at:\n" + apkPath);
                    }
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
        return tool;
    }

    private void runProcess(File workDir, String... cmd) throws Exception {
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

    private void generateCapacitor(String name, String pkg, String desc, File srcDir) throws Exception {
        String appId = pkg.isEmpty() ? "com." + name.toLowerCase().replaceAll("[^a-z0-9]", "") + ".app" : pkg;
        File capConf = new File(srcDir, "capacitor.config.json");
        Files.write(capConf.toPath(),
            ("{\n  \"appId\": \"" + appId + "\",\n"
            + "  \"appName\": \"" + name + "\",\n"
            + "  \"webDir\": \"dist\",\n"
            + "  \"bundledWebRuntime\": false,\n"
            + "  \"server\": { \"androidScheme\": \"https\" }\n}\n").getBytes("UTF-8"));
        File pkgFile = new File(srcDir, "package.json");
        if (pkgFile.exists()) {
            String json = new String(Files.readAllBytes(pkgFile.toPath()), "UTF-8");
            if (!json.contains("\"@capacitor/core\"")) {
                json = json.replace("\"dependencies\"", "\"devDependencies\"");
                int idx = json.lastIndexOf("}");
                json = json.substring(0, idx) + ",\n  \"@capacitor/core\": \"^5.6.0\"\n" + json.substring(idx);
                json = json.replaceFirst("\"devDependencies\"", "\"dependencies\"");
            }
            if (!json.contains("\"@capacitor/cli\"")) {
                json = json.replace("\"devDependencies\"", "\"dependencies\"");
                int idx = json.lastIndexOf("}");
                json = json.substring(0, idx) + ",\n  \"@capacitor/cli\": \"^5.6.0\",\n  \"@capacitor/android\": \"^5.6.0\"\n" + json.substring(idx);
                json = json.replaceFirst("\"dependencies\"", "\"devDependencies\"");
            }
            if (!json.contains("\"android\"")) {
                json = json.replace("\"scripts\": {", "\"scripts\": {\n    \"android\": \"npx cap sync && npx cap run android\",");
            }
            if (!json.contains("\"description\"")) {
                json = json.substring(0, json.lastIndexOf("}")) + ",\n  \"description\": \""
                    + (desc.isEmpty() ? name + " Android App" : desc) + "\"\n" + json.substring(json.lastIndexOf("}"));
            }
            Files.write(pkgFile.toPath(), json.getBytes("UTF-8"));
        } else {
            String json = "{\n"
                + "  \"name\": \"" + name.toLowerCase().replaceAll("\\s+", "-") + "\",\n"
                + "  \"version\": \"1.0.0\",\n"
                + "  \"private\": true,\n"
                + "  \"description\": \"" + (desc.isEmpty() ? name + " Android App" : desc) + "\",\n"
                + "  \"scripts\": { \"android\": \"npx cap sync && npx cap run android\" },\n"
                + "  \"dependencies\": { \"@capacitor/core\": \"^5.6.0\" },\n"
                + "  \"devDependencies\": {\n"
                + "    \"@capacitor/cli\": \"^5.6.0\",\n"
                + "    \"@capacitor/android\": \"^5.6.0\"\n  }\n}\n";
            Files.write(pkgFile.toPath(), json.getBytes("UTF-8"));
        }
        if (pathlabel.getText() != null && !pathlabel.getText().isEmpty()) {
            File iconFile = new File(pathlabel.getText());
            if (iconFile.exists()) {
                Files.copy(iconFile.toPath(), new File(srcDir, "icon.png").toPath(), StandardCopyOption.REPLACE_EXISTING);
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
