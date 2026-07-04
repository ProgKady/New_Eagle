package com.eagle.util;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class SimpleApkBuilder {

    public static class ApkBuildConfig {
        public File sourceFile;
        public File iconFile;
        public String apkName = "MyApp";
        public String packageName = "com.example.app";
        public File workDir;
        public List<String> log = new ArrayList<>();
    }

    @FunctionalInterface
    public interface BuildCallback {
        void build(ApkBuildConfig config) throws Exception;
    }

    public static void showBuildDialog(Window owner, String title,
                                        String extensionFilter,
                                        BuildCallback callback) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle(title);

        ApkBuildConfig config = new ApkBuildConfig();
        TextField sourceField = new TextField();
        sourceField.setPromptText("Select source file...");
        sourceField.setPrefColumnCount(30);
        Button browseBtn = new Button("...");
        browseBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Source File");
            if (extensionFilter != null && !extensionFilter.isEmpty() && !extensionFilter.equals("*.*")) {
                fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Source Files", extensionFilter));
            }
            File f = fc.showOpenDialog(dialog);
            if (f != null) {
                sourceField.setText(f.getAbsolutePath());
            }
        });

        HBox sourceRow = new HBox(8, sourceField, browseBtn);
        HBox.setHgrow(sourceField, Priority.ALWAYS);

        TextField apkNameField = new TextField("MyApp");
        apkNameField.setPromptText("APK file name");

        TextField pkgField = new TextField("com.example.app");
        pkgField.setPromptText("e.g. com.example.app");

        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(8);

        Button buildBtn = new Button("Build APK");
        buildBtn.setDefaultButton(true);
        ProgressBar progress = new ProgressBar(0);
        progress.setVisible(false);

        buildBtn.setOnAction(ev -> {
            String src = sourceField.getText().trim();
            if (src.isEmpty()) {
                showAlert("Please select a source file.", dialog);
                return;
            }
            config.sourceFile = new File(src);
            config.apkName = apkNameField.getText().trim();
            if (config.apkName.isEmpty()) config.apkName = "MyApp";
            config.packageName = pkgField.getText().trim();
            if (config.packageName.isEmpty()) config.packageName = "com.example.app";

            buildBtn.setDisable(true);
            progress.setVisible(true);
            logArea.clear();

            new Thread(() -> {
                try {
                    config.workDir = new File(
                        System.getProperty("java.io.tmpdir") + "\\apk_build_" + System.currentTimeMillis());
                    config.workDir.mkdirs();

                    // Let the plugin prepare assets
                    callback.build(config);

                    // Run the full build pipeline
                    buildApk(config, logArea);
                    Platform.runLater(() -> {
                        logArea.appendText("\nBuild complete!\n");
                        buildBtn.setDisable(false);
                        progress.setVisible(false);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        logArea.appendText("\nError: " + ex.getMessage() + "\n");
                        buildBtn.setDisable(false);
                        progress.setVisible(false);
                    });
                }
            }).start();
        });

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(16));
        grid.add(new Label("Source File:"), 0, 0);
        grid.add(sourceRow, 1, 0);
        grid.add(new Label("APK Name:"), 0, 1);
        grid.add(apkNameField, 1, 1);
        grid.add(new Label("Package:"), 0, 2);
        grid.add(pkgField, 1, 2);
        GridPane.setColumnSpan(sourceRow, 2);
        GridPane.setColumnSpan(apkNameField, 2);
        GridPane.setColumnSpan(pkgField, 2);

        VBox root = new VBox(12, grid, buildBtn, progress, logArea);
        root.setPadding(new Insets(8));
        Scene scene = new Scene(root, 520, 400);

        try {
            scene.getStylesheets().add(
                SimpleApkBuilder.class.getResource("/com/eagle/css/base.css").toExternalForm());
        } catch (Exception ignored) {}

        dialog.setScene(scene);
        dialog.showAndWait();
    }

    public static void buildApk(ApkBuildConfig config, TextArea logArea) throws Exception {
        File workDir = config.workDir;
        String apkName = config.apkName;
        String apkPkg = config.packageName;
        String pkgPath = "com.kadysoft." + apkPkg;
        String pkgDir = "com/kadysoft/" + apkPkg;

        new File(workDir, "res\\values").mkdirs();
        new File(workDir, "res\\drawable").mkdirs();
        new File(workDir, "res\\drawable-xhdpi").mkdirs();
        new File(workDir, "src\\" + pkgDir).mkdirs();
        new File(workDir, "obj\\" + pkgDir).mkdirs();
        new File(workDir, "assets").mkdirs();

        String buildTools = detectBuildTools();
        String platformJar = detectPlatformJar();
        String keystorePath = detectKeystore();
        String appDir = detectAppDir();
        String jdkHome = detectJdkHome();

        log(logArea, "Build tools: " + buildTools);
        log(logArea, "Platform JAR: " + platformJar);

        // Validate tools
        if (!new File(buildTools + "\\aapt2.exe").exists())
            throw new IOException("aapt2.exe not found in " + buildTools);
        if (!new File(platformJar).exists())
            throw new IOException("android.jar not found at " + platformJar);

        // AndroidManifest.xml
        String manifest = "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>"
            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\""
            + " package=\"" + pkgPath + "\""
            + " android:installLocation=\"auto\""
            + " android:versionCode=\"100\" android:versionName=\"1.00\">"
            + "<uses-sdk android:minSdkVersion=\"21\"/>"
            + "<application android:icon=\"@drawable/icon\" android:label=\"" + apkName + "\">"
            + "<activity android:name=\".MainActivity\" android:configChanges=\"orientation|screenSize|keyboardHidden\">"
            + "<intent-filter><action android:name=\"android.intent.action.MAIN\"/>"
            + "<category android:name=\"android.intent.category.LAUNCHER\"/></intent-filter>"
            + "</activity></application></manifest>";
        Files.write(Paths.get(workDir + "\\AndroidManifest.xml"),
            manifest.getBytes(StandardCharsets.UTF_8));
        log(logArea, "AndroidManifest.xml created");

        // MainActivity.java (WebView loader)
        String javaSource = "package " + pkgPath + ";\n"
            + "import android.app.Activity;\n"
            + "import android.os.Bundle;\n"
            + "import android.webkit.WebView;\n"
            + "import android.webkit.WebViewClient;\n"
            + "public class MainActivity extends Activity {\n"
            + "    private WebView wv;\n"
            + "    @Override\n"
            + "    protected void onCreate(Bundle savedInstanceState) {\n"
            + "        super.onCreate(savedInstanceState);\n"
            + "        wv = new WebView(this);\n"
            + "        wv.setWebViewClient(new WebViewClient());\n"
            + "        wv.getSettings().setJavaScriptEnabled(true);\n"
            + "        wv.getSettings().setDomStorageEnabled(true);\n"
            + "        wv.getSettings().setAllowFileAccess(true);\n"
            + "        setContentView(wv);\n"
            + "        wv.loadUrl(\"file:///android_asset/index.html\");\n"
            + "    }\n"
            + "    @Override\n"
            + "    public void onBackPressed() {\n"
            + "        if (wv.canGoBack()) wv.goBack();\n"
            + "        else super.onBackPressed();\n"
            + "    }\n"
            + "}";
        Files.write(Paths.get(workDir + "\\src\\" + pkgDir + "\\MainActivity.java"),
            javaSource.getBytes(StandardCharsets.UTF_8));
        log(logArea, "MainActivity.java created");

        // Resources
        String strings = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources><string name=\"app_name\">" + apkName + "</string></resources>";
        Files.write(Paths.get(workDir + "\\res\\values\\strings.xml"),
            strings.getBytes(StandardCharsets.UTF_8));
        String styles = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>"
            + "<style name=\"AppTheme\" parent=\"android:Theme.Material.Light.NoActionBar\">"
            + "<item name=\"android:windowFullscreen\">true</item>"
            + "</style></resources>";
        Files.write(Paths.get(workDir + "\\res\\values\\styles.xml"),
            styles.getBytes(StandardCharsets.UTF_8));
        log(logArea, "Resources created");

        // Copy web project files to assets
        if (config.sourceFile != null && config.sourceFile.exists()) {
            File assetsDir = new File(workDir, "assets");
            assetsDir.mkdirs();
            copyFileOrDir(config.sourceFile.getParentFile(), assetsDir);
            log(logArea, "Project files copied to assets");
        }

        // Icon
        File iconFile = config.iconFile;
        if (iconFile != null && iconFile.exists()) {
            Files.copy(iconFile.toPath(), Paths.get(workDir + "\\res\\drawable\\icon.png"),
                StandardCopyOption.REPLACE_EXISTING);
            Files.copy(iconFile.toPath(), Paths.get(workDir + "\\res\\drawable-xhdpi\\icon.png"),
                StandardCopyOption.REPLACE_EXISTING);
        } else {
            // Use default icon from app resources
            InputStream defaultIcon = SimpleApkBuilder.class.getResourceAsStream("/com/eagle/icons/image.png");
            if (defaultIcon != null) {
                Files.copy(defaultIcon, Paths.get(workDir + "\\res\\drawable\\icon.png"),
                    StandardCopyOption.REPLACE_EXISTING);
                Files.copy(SimpleApkBuilder.class.getResourceAsStream("/com/eagle/icons/image.png"),
                    Paths.get(workDir + "\\res\\drawable-xhdpi\\icon.png"),
                    StandardCopyOption.REPLACE_EXISTING);
            }
        }
        log(logArea, "Icon copied");

        // Compile Java
        log(logArea, "Compiling Java source...");
        runProcess(buildTools, platformJar, jdkHome, workDir, pkgDir, logArea);

        // Compile resources with aapt2
        log(logArea, "Compiling resources (aapt2)...");
        exec("\"" + buildTools + "\\aapt2.exe\" compile --dir \"" + workDir + "\\res\" -o \""
            + workDir + "\\res.flata\"", logArea);
        exec("\"" + buildTools + "\\aapt2.exe\" link --auto-add-overlay -o \"" + workDir
            + "\\app.ap_\" -I \"" + platformJar + "\" --manifest \"" + workDir
            + "\\AndroidManifest.xml\" -R \"" + workDir + "\\res.flata\"", logArea);
        log(logArea, "Resources compiled");

        // Build APK
        log(logArea, "Assembling APK...");
        buildApkPackage(workDir, workDir + "\\unsigned.apk", logArea);
        log(logArea, "APK assembled");

        // Sign
        log(logArea, "Signing APK...");
        ensureKeystore(buildTools, keystorePath, jdkHome, logArea);
        exec("\"" + jdkHome + "\\bin\\jarsigner.exe\" -keystore \"" + keystorePath
            + "\" -storepass eagle710 -keypass eagle710 -digestalg SHA1 -sigalg SHA1withRSA"
            + " -signedjar \"" + workDir + "\\signed.apk\" \"" + workDir + "\\unsigned.apk\" eagle",
            logArea);
        log(logArea, "APK signed");

        // Zipalign
        log(logArea, "Zipaligning...");
        String finalApk = System.getProperty("user.home") + "\\Desktop\\" + apkName + ".apk";
        exec("\"" + buildTools + "\\zipalign.exe\" -v 4 \"" + workDir + "\\signed.apk\" \""
            + finalApk + "\"", logArea);
        log(logArea, "APK saved to: " + finalApk);

        // Cleanup
        deleteDirectory(workDir);
        log(logArea, "Build complete!");
    }

    // ----------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------

    private static void log(TextArea logArea, String msg) {
        Platform.runLater(() -> logArea.appendText(msg + "\n"));
    }

    private static void showAlert(String msg, Stage owner) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.initOwner(owner);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private static void exec(String command, TextArea logArea) throws Exception {
        File batFile = File.createTempFile("eagle_", ".bat");
        batFile.deleteOnExit();
        Files.write(batFile.toPath(), Arrays.asList("@echo off", command), StandardCharsets.UTF_8);
        ProcessBuilder pb = new ProcessBuilder(batFile.getAbsolutePath());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                final String l = line;
                Platform.runLater(() -> logArea.appendText("  " + l + "\n"));
            }
        }
        int exitCode = p.waitFor();
        if (exitCode != 0) {
            throw new IOException("Command failed (code " + exitCode + "): " + command);
        }
    }

    private static void runProcess(String buildTools, String platformJar,
                                    String jdkHome, File workDir,
                                    String pkgDir, TextArea logArea) throws Exception {
        String srcFile = workDir + "\\src\\" + pkgDir + "\\MainActivity.java";
        String objDir = workDir + "\\obj";
        exec("\"" + jdkHome + "\\bin\\javac.exe\" -cp \"" + platformJar + "\" -d \""
            + objDir + "\" \"" + srcFile + "\"", logArea);

        // Convert to DEX
        File d8Jar = findD8Jar(buildTools);
        StringBuilder classFiles = new StringBuilder();
        appendClassFiles(new File(objDir), classFiles);
        if (classFiles.length() == 0) {
            throw new IOException("No .class files found in " + objDir);
        }
        exec("\"" + jdkHome + "\\bin\\java.exe\" -cp \"" + d8Jar
            + "\" com.android.tools.r8.D8 --lib \"" + platformJar
            + "\" --min-api 21 --output \"" + workDir + "\"" + classFiles, logArea);
    }

    private static void buildApkPackage(File workDir, String outputPath,
                                         TextArea logArea) throws IOException {
        try (java.util.zip.ZipOutputStream zos =
                new java.util.zip.ZipOutputStream(new FileOutputStream(outputPath))) {
            File ap_ = new File(workDir, "app.ap_");
            if (ap_.exists()) {
                try (java.util.zip.ZipInputStream zis =
                        new java.util.zip.ZipInputStream(new FileInputStream(ap_))) {
                    java.util.zip.ZipEntry e;
                    while ((e = zis.getNextEntry()) != null) {
                        zos.putNextEntry(new java.util.zip.ZipEntry(e.getName()));
                        copyStream(zis, zos);
                        zos.closeEntry();
                    }
                }
            }
            File dex = new File(workDir, "classes.dex");
            if (dex.exists()) {
                zos.putNextEntry(new java.util.zip.ZipEntry("classes.dex"));
                Files.copy(dex.toPath(), zos);
                zos.closeEntry();
            }
            File assets = new File(workDir, "assets");
            if (assets.exists() && assets.isDirectory()) {
                zipDir(assets, assets.getAbsolutePath().length() + 1, zos);
            }
        }
        log(logArea, "APK package created");
    }

    private static void copyFileOrDir(File src, File dst) throws IOException {
        if (src.isDirectory()) {
            String[] children = src.list();
            if (children != null) {
                for (String child : children) {
                    if (child.equals("node_modules") || child.startsWith(".")) continue;
                    copyFileOrDir(new File(src, child), new File(dst, child));
                }
            }
        } else {
            dst.getParentFile().mkdirs();
            Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void zipDir(File dir, int baseLen,
                                java.util.zip.ZipOutputStream zos) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            String entryName = f.getAbsolutePath().substring(baseLen).replace("\\", "/");
            if (f.isDirectory()) {
                zos.putNextEntry(new java.util.zip.ZipEntry(entryName + "/"));
                zos.closeEntry();
                zipDir(f, baseLen, zos);
            } else {
                zos.putNextEntry(new java.util.zip.ZipEntry("assets/" + entryName));
                Files.copy(f.toPath(), zos);
                zos.closeEntry();
            }
        }
    }

    private static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
    }

    private static void appendClassFiles(File dir, StringBuilder sb) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) appendClassFiles(f, sb);
            else if (f.getName().endsWith(".class")) {
                sb.append(" \"").append(f.getAbsolutePath()).append("\"");
            }
        }
    }

    private static void ensureKeystore(String buildTools, String keystorePath,
                                        String jdkHome, TextArea logArea) throws Exception {
        File ks = new File(keystorePath);
        if (!ks.exists()) {
            ks.getParentFile().mkdirs();
            exec("\"" + jdkHome + "\\bin\\keytool.exe\" -genkeypair -keystore \""
                + keystorePath + "\" -alias eagle -keypass eagle710 -storepass eagle710"
                + " -dname \"CN=WebIDE, OU=Development, O=EagleSoft, L=Cairo, C=EG\""
                + " -validity 3650 -keyalg RSA -keysize 2048", logArea);
        }
    }

    private static File findD8Jar(String buildTools) {
        File f = new File(buildTools + "\\lib\\d8.jar");
        if (f.exists()) return f;
        return new File(buildTools + "\\d8.jar");
    }

    private static void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDirectory(f);
                else f.delete();
            }
        }
        dir.delete();
    }

    // ----------------------------------------------------------------
    // Environment detection (mirrors AbstractApkBuilder logic)
    // ----------------------------------------------------------------

    private static String detectAppDir() {
        String userDir = System.getProperty("user.dir");
        if (new File(userDir + "\\tools\\aapt2.exe").exists()
            || new File(userDir + "\\tools\\android.jar").exists()) {
            return userDir;
        }
        try {
            File jarPath = new File(SimpleApkBuilder.class
                .getProtectionDomain().getCodeSource().getLocation().toURI());
            String parent = jarPath.getParentFile().getAbsolutePath();
            if (parent.endsWith("classes") || parent.endsWith("build\\classes")) {
                File projectRoot = new File(parent).getParentFile().getParentFile();
                if (new File(projectRoot, "tools\\aapt2.exe").exists()
                    || new File(projectRoot, "tools\\android.jar").exists()) {
                    return projectRoot.getAbsolutePath();
                }
            }
            return parent;
        } catch (Exception e) {
            return userDir;
        }
    }

    private static String detectSdkPath() {
        String bundled = detectAppDir() + "\\tools";
        String[] bundledCandidates = { bundled, System.getProperty("user.dir") + "\\tools" };
        for (String b : bundledCandidates) {
            if (new File(b + "\\aapt2.exe").exists()) return b;
        }
        String home = System.getProperty("user.home");
        String[] sdkCandidates = {
            home + "\\AppData\\Local\\Android\\Sdk", "C:\\Android\\Sdk",
            "C:\\Android\\android-sdk", home + "\\Android\\Sdk"
        };
        for (String c : sdkCandidates) {
            if (new File(c + "\\platforms").exists()) return c;
        }
        return bundled;
    }

    private static String detectBuildTools() {
        String bundled = detectAppDir() + "\\tools";
        if (new File(bundled + "\\aapt2.exe").exists()) return bundled;
        String bt = detectSdkPath() + "\\build-tools";
        File dir = new File(bt);
        if (!dir.exists()) return bundled;
        File[] vers = dir.listFiles(File::isDirectory);
        if (vers == null || vers.length == 0) return bundled;
        String[] names = new String[vers.length];
        for (int i = 0; i < vers.length; i++) names[i] = vers[i].getName();
        Arrays.sort(names, Collections.reverseOrder());
        return bt + "\\" + names[0];
    }

    private static String detectPlatformJar() {
        String bundled = detectAppDir() + "\\tools";
        if (new File(bundled + "\\android.jar").exists()) return bundled + "\\android.jar";
        String plat = detectSdkPath() + "\\platforms";
        File dir = new File(plat);
        if (!dir.exists()) return bundled + "\\android.jar";
        File[] vers = dir.listFiles(File::isDirectory);
        if (vers == null || vers.length == 0) return bundled + "\\android.jar";
        String[] names = new String[vers.length];
        for (int i = 0; i < vers.length; i++) names[i] = vers[i].getName();
        Arrays.sort(names, Collections.reverseOrder());
        return plat + "\\" + names[0] + "\\android.jar";
    }

    private static String detectKeystore() {
        return detectAppDir() + "\\tools\\eagle.keystore";
    }

    private static String detectJdkHome() {
        String javaHome = System.getProperty("java.home");
        if (javaHome.endsWith("jre"))
            javaHome = javaHome.substring(0, javaHome.length() - 4) + "jdk";
        if (!new File(javaHome + "\\bin\\javac.exe").exists()) {
            String[] paths = {
                "C:\\Program Files\\Java\\jdk1.8.0_491",
                "C:\\Program Files\\Java\\jdk1.8.0_161",
                "C:\\Program Files\\Java\\latest",
                "C:\\Program Files\\Java\\jdk-17",
                "C:\\Program Files\\Java\\jdk-21"
            };
            for (String p : paths) {
                if (new File(p + "\\bin\\javac.exe").exists()) return p;
            }
        }
        return javaHome;
    }
}
