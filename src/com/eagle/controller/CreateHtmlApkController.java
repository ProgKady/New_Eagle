package com.eagle.controller;

import com.eagle.model.ApkConfig;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import com.eagle.util.ThemeManager;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;

public class CreateHtmlApkController implements Initializable {

    @FXML private ResourceBundle resources;
    @FXML private URL location;
    @FXML private JFXTextField zipfile;
    @FXML private Label pathlabel;
    @FXML private JFXTextField apkname;
    @FXML private JFXButton browse2;
    @FXML private JFXTextField packagename;
    @FXML private JFXButton browse1;
    @FXML private JFXTextField htmlfile;
    @FXML private ImageView imgview;
    @FXML private JFXButton create;

    private TaskProgressDialog progressDialog;
    private File selectedIcon;
    private ApkConfig apkConfig = new ApkConfig();

    private static final String APP_DIR = getAppDir();
    private static String BUNDLED_TOOLS = APP_DIR + "\\tools";
    private static String SDK_PATH = detectSdkPath();
    private static String BUILD_TOOLS = detectBuildToolsDir();
    private static String PLATFORM_JAR = detectPlatformJar();
    private static String ADMOB_JARS_CP = detectAdmobJars(";");
    private static String ADMOB_JARS_D8 = detectAdmobJars(" ");
    private static String KEYSTORE_PATH = APP_DIR + "\\tools\\eagle.keystore";
    private static final String KEYSTORE_ALIAS = "eagle";
    private static final String KEYSTORE_PASS = "eagle710";
    private static final String KEYSTORE_DNAME = "CN=WebIDE, OU=Development, O=EagleSoft, L=Cairo, C=EG";

    private static String getAppDir() {

        String userDir = System.getProperty("user.dir");
        if (new File(userDir + "\\tools\\aapt2.exe").exists()
            || new File(userDir + "\\tools\\android.jar").exists()) {
            return userDir;
        }

        try {
            File jarPath = new File(CreateHtmlApkController.class
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

        String[] bundledCandidates = {
            BUNDLED_TOOLS,
            System.getProperty("user.dir") + "\\tools",
        };
        for (String b : bundledCandidates) {
            if (new File(b + "\\android.jar").exists()
                || new File(b + "\\aapt2.exe").exists()) {
                return b;
            }
        }
        String home = System.getProperty("user.home");
        String[] sdkCandidates = {
            home + "\\AppData\\Local\\Android\\Sdk",
            "C:\\Android\\Sdk",
            "C:\\Android\\android-sdk",
            home + "\\Android\\Sdk",
        };
        for (String c : sdkCandidates) {
            if (new File(c + "\\platforms").exists()) return c;
        }
        return BUNDLED_TOOLS;
    }

    private static String detectBuildToolsDir() {

        if (new File(BUNDLED_TOOLS + "\\aapt2.exe").exists()) return BUNDLED_TOOLS;

        String bt = SDK_PATH + "\\build-tools";
        File dir = new File(bt);
        if (!dir.exists()) return BUNDLED_TOOLS;
        File[] vers = dir.listFiles(File::isDirectory);
        if (vers == null || vers.length == 0) return BUNDLED_TOOLS;
        String[] names = new String[vers.length];
        for (int i = 0; i < vers.length; i++) names[i] = vers[i].getName();
        Arrays.sort(names, Collections.reverseOrder());
        return bt + "\\" + names[0];
    }

    private static String detectPlatformJar() {

        if (new File(BUNDLED_TOOLS + "\\android.jar").exists()) return BUNDLED_TOOLS + "\\android.jar";

        String plat = SDK_PATH + "\\platforms";
        File dir = new File(plat);
        if (!dir.exists()) return BUNDLED_TOOLS + "\\android.jar";
        File[] vers = dir.listFiles(File::isDirectory);
        if (vers == null || vers.length == 0) return BUNDLED_TOOLS + "\\android.jar";
        String[] names = new String[vers.length];
        for (int i = 0; i < vers.length; i++) names[i] = vers[i].getName();
        Arrays.sort(names, Collections.reverseOrder());
        return plat + "\\" + names[0] + "\\android.jar";
    }

    private static final List<String> COMMON_PERMISSIONS = Arrays.asList(
        "android.permission.INTERNET",
        "android.permission.ACCESS_NETWORK_STATE",
        "android.permission.ACCESS_WIFI_STATE",
        "android.permission.CAMERA",
        "android.permission.RECORD_AUDIO",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.READ_CONTACTS",
        "android.permission.READ_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.SEND_SMS",
        "android.permission.CALL_PHONE",
        "android.permission.BLUETOOTH",
        "android.permission.NFC",
        "android.permission.VIBRATE",
        "android.permission.WAKE_LOCK",
        "android.permission.FOREGROUND_SERVICE",
        "android.permission.READ_PHONE_STATE",
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.WRITE_SETTINGS",
        "android.permission.REQUEST_INSTALL_PACKAGES"
    );

    @FXML
    void imgviewact(MouseEvent event) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png"));
        fc.setTitle("Select App Icon");
        File f = fc.showOpenDialog(imgview.getScene().getWindow());
        if (f != null) {
            selectedIcon = f;
            imgview.setImage(new Image(f.toURI().toString()));
            pathlabel.setText(f.getAbsolutePath());
            pathlabel.setVisible(true);
            browse1.setDisable(false);
        }
    }

    @FXML
    void browse1act(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML Files", "*.html"));
        fc.setTitle("Select HTML File");
        File f = fc.showOpenDialog(htmlfile.getScene().getWindow());
        if (f != null) {
            htmlfile.setText(f.getAbsolutePath());
            browse2.setDisable(false);
        }
    }

    public void preselectHtmlFile(File f) {
        htmlfile.setText(f.getAbsolutePath());
        browse2.setDisable(false);
    }

    @FXML
    void browse2act(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Zip Files", "*.zip"));
        fc.setTitle("Select Resources Zip");
        File f = fc.showOpenDialog(zipfile.getScene().getWindow());
        if (f != null) {
            zipfile.setText(f.getAbsolutePath());
            create.setDisable(false);
        }
    }

    @FXML
    void previewAct(ActionEvent event) {
        String path = htmlfile.getText().trim();
        if (path.isEmpty()) {
            showAlert("No file", "Please select an HTML file first.");
            return;
        }
        File f = new File(path);
        if (!f.exists()) {
            showAlert("Not found", "HTML file does not exist.");
            return;
        }
        Platform.runLater(() -> {
            Stage previewStage = new Stage();
            previewStage.initModality(Modality.NONE);
            previewStage.setTitle("Preview: " + f.getName());
            WebView wv = new WebView();
            wv.getEngine().load(f.toURI().toString());
            Scene s = new Scene(wv, 800, 600);
            previewStage.setScene(s);
            previewStage.show();
        });
    }

    @FXML
    void settingsAct(ActionEvent event) {
        showSettingsDialog();
    }

    private void showSettingsDialog() {
        Platform.runLater(() -> {
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Build Tools Settings");

            VBox root = new VBox(12);
            root.setStyle("-fx-padding: 20;");
            root.getStyleClass().add("dialog-pane");

            Label title = new Label("Android SDK & Build Tools");
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

            TextField sdkField = new TextField(SDK_PATH);
            sdkField.setStyle("-fx-background-radius: 8; -fx-padding: 8;");
            Label sdkLabel = new Label("SDK Path:");
            sdkLabel.setStyle("-fx-font-size: 12px;");

            TextField btField = new TextField(BUILD_TOOLS);
            btField.setStyle("-fx-background-radius: 8; -fx-padding: 8;");
            Label btLabel = new Label("Build Tools Path:");
            btLabel.setStyle("-fx-font-size: 12px;");

            TextField platField = new TextField(PLATFORM_JAR);
            platField.setStyle("-fx-background-radius: 8; -fx-padding: 8;");
            Label platLabel = new Label("android.jar Path:");
            platLabel.setStyle("-fx-font-size: 12px;");

            TextField ksField = new TextField(KEYSTORE_PATH);
            ksField.setStyle("-fx-background-radius: 8; -fx-padding: 8;");
            Label ksLabel = new Label("Keystore Path:");
            ksLabel.setStyle("-fx-font-size: 12px;");

            HBox buttons = new HBox(10);
            buttons.setAlignment(Pos.CENTER_RIGHT);
            Button saveBtn = new Button("Save");
            saveBtn.getStyleClass().add("btn-primary");
            saveBtn.setOnAction(e -> {
                SDK_PATH = sdkField.getText();
                BUILD_TOOLS = btField.getText();
                PLATFORM_JAR = platField.getText();
                KEYSTORE_PATH = ksField.getText();
                stage.close();
            });
            Button cancelBtn = new Button("Cancel");
            cancelBtn.setOnAction(e -> stage.close());
            buttons.getChildren().addAll(cancelBtn, saveBtn);

            root.getChildren().addAll(title, sdkLabel, sdkField, btLabel, btField, platLabel, platField, ksLabel, ksField, buttons);
            Scene s = new Scene(root, 480, 400);
            s.getStylesheets().add(getClass().getResource("/com/eagle/css/base.css").toExternalForm());
            ThemeManager.getInstance().applyTheme(s);
            stage.setScene(s);
            stage.show();
        });
    }

    @FXML
    void createact(ActionEvent event) {
        String apkName = apkname.getText().trim();
        String apkPkg = packagename.getText().trim();
        String apkIcon = pathlabel.getText().trim();
        String apkSource = htmlfile.getText().trim();
        String apkResource = zipfile.getText().trim();
        
        Stage dfdf=(Stage) packagename.getScene().getWindow();

        if (apkName.isEmpty() || apkPkg.isEmpty() || apkIcon.isEmpty()
                || apkSource.isEmpty() || apkResource.isEmpty()) {
            showAlert("Missing fields", "Please fill in all fields before creating the APK.");
            return;
        }

        StringBuilder missing = new StringBuilder();
        if (!new File(BUILD_TOOLS + "\\aapt2.exe").exists())
            missing.append("\n\u2022 aapt2.exe  (\"").append(BUILD_TOOLS).append("\")");
        if (!new File(PLATFORM_JAR).exists())
            missing.append("\n\u2022 android.jar  (\"").append(PLATFORM_JAR).append("\")");
        if (!findD8Jar().exists())
            missing.append("\n\u2022 d8.jar  (\"").append(BUILD_TOOLS).append("\\lib\\ or ").append(BUILD_TOOLS).append("\\\")");
        if (!new File(BUILD_TOOLS + "\\zipalign.exe").exists())
            missing.append("\n\u2022 zipalign.exe  (\"").append(BUILD_TOOLS).append("\")");
        if (missing.length() > 0) {
            showAlert("Missing Build Tools",
                "Put these files in a folder named \"tools\" next to the app:\n"
                + missing
                + "\n\nOr set correct paths in Settings (gear icon).");
            return;
        }

        // Fill config from UI fields
        apkConfig.setAppName(apkName);
        apkConfig.setPackageName("com.kadysoft." + apkPkg);
        apkConfig.setIconFile(new File(apkIcon));
        apkConfig.setSourceInput(apkSource);
        apkConfig.setExtraInput(apkResource);

        // Show customizer dialog before build
        ApkCustomizerDialog customizer = new ApkCustomizerDialog(dfdf, apkConfig);
        customizer.show();

        dfdf.setIconified(true);

        Task<Void> buildTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                buildApk(apkName, apkPkg, apkSource, apkResource);
                return null;
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    progressDialog.setOverallProgress(0);
                    progressDialog.logError("Build failed: " + getException().getMessage());
                    Notifications.create()
                        .title("Build Failed")
                        .text("Error: " + getException().getMessage())
                        .position(Pos.CENTER)
                        .hideAfter(Duration.seconds(8))
                        .showError();
                });
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    progressDialog.setOverallProgress(1.0);
                    progressDialog.logSuccess("APK created successfully!");
                    Notifications.create()
                        .title("Success!")
                        .text("APK saved to your desktop!")
                        .position(Pos.CENTER)
                        .hideAfter(Duration.seconds(7))
                        .showInformation();
                    new Timer().schedule(new TimerTask() {
                        public void run() { progressDialog.close(); }
                    }, 3000);
                });
            }
        };

        Thread thread = new Thread(buildTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void buildApk(String apkName, String apkPkg,
                          String apkSource, String apkResource) throws Exception {
        // Create working directory
        File workDir = new File(System.getProperty("java.io.tmpdir") + "\\webide_build_" + System.currentTimeMillis());
        workDir.mkdirs();

        File resDir = new File(workDir, "res\\values");
        resDir.mkdirs();
        new File(workDir, "res\\drawable").mkdirs();
        new File(workDir, "res\\drawable-xhdpi").mkdirs();
        File srcDir = new File(workDir, "src");
        srcDir.mkdirs();
        File assetsDir = new File(workDir, "assets");
        assetsDir.mkdirs();
        File objDir = new File(workDir, "obj");
        objDir.mkdirs();

        String pkgPath = "com." + "kadysoft." + apkPkg;
        String pkgDir = "com/" + "kadysoft/" + apkPkg;
        new File(srcDir, pkgDir).mkdirs();
        new File(objDir, pkgDir).mkdirs();

        String finalApkPath = System.getProperty("user.home") + "\\Desktop\\" + apkName + ".apk";

        // Initialize dialog
        java.util.List<String> themeSheets = apkname.getScene() != null
            ? new java.util.ArrayList<>(apkname.getScene().getStylesheets()) : null;
        CountDownLatch dialogReady = new CountDownLatch(1);
        Platform.runLater(() -> {
            progressDialog = new TaskProgressDialog("Building APK: " + apkName);
            if (themeSheets != null) progressDialog.setThemeStylesheets(themeSheets);
            progressDialog.addTask("Preparing project structure");
            progressDialog.addTask("Generating AndroidManifest");
            progressDialog.addTask("Writing Java source (WebView)");
            progressDialog.addTask("Generating resources");
            progressDialog.addTask("Copying assets (HTML + ZIP)");
            progressDialog.addTask("Compiling Java to DEX");
            progressDialog.addTask("Compiling resources (aapt2)");
            progressDialog.addTask("Building APK");
            progressDialog.addTask("Signing APK");
            progressDialog.addTask("Zipalign & finalize");
            progressDialog.show();
            dialogReady.countDown();
        });
        dialogReady.await();

        progressDialog.log("Starting build pipeline...");

        // 1. Prepare project
        progressDialog.setTaskStatus(0, TaskProgressDialog.TaskStatus.RUNNING);
        progressDialog.log("Creating project structure...");
        Thread.sleep(200);
        progressDialog.setTaskStatus(0, TaskProgressDialog.TaskStatus.COMPLETED);
        progressDialog.setTaskProgress(0, 1.0);

        // 2. AndroidManifest (from apkConfig)
        progressDialog.setTaskStatus(1, TaskProgressDialog.TaskStatus.RUNNING);
        progressDialog.log("Generating AndroidManifest.xml...");
        StringBuilder manifest = new StringBuilder();
        String verName = apkConfig.getVersionName();
        int verCode = apkConfig.getVersionCode();
        manifest.append("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>")
            .append("<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"")
            .append(" package=\"").append(pkgPath).append("\"")
            .append(" android:installLocation=\"auto\"")
            .append(" android:versionCode=\"").append(verCode).append("\" android:versionName=\"").append(verName).append("\">")
            .append("<uses-sdk android:minSdkVersion=\"21\"/>");
        if (apkConfig.isInternetPermission())
            manifest.append("<uses-permission android:name=\"android.permission.INTERNET\"/>");
        if (apkConfig.isStoragePermission()) {
            manifest.append("<uses-permission android:name=\"android.permission.WRITE_EXTERNAL_STORAGE\"/>");
            manifest.append("<uses-permission android:name=\"android.permission.READ_EXTERNAL_STORAGE\"/>");
        }
        if (apkConfig.isCameraPermission())
            manifest.append("<uses-permission android:name=\"android.permission.CAMERA\"/>");
        if (apkConfig.isLocationPermission())
            manifest.append("<uses-permission android:name=\"android.permission.ACCESS_FINE_LOCATION\"/>");
        if (apkConfig.isMicrophonePermission())
            manifest.append("<uses-permission android:name=\"android.permission.RECORD_AUDIO\"/>");
        if (apkConfig.isContactsPermission())
            manifest.append("<uses-permission android:name=\"android.permission.READ_CONTACTS\"/>");
        if (apkConfig.isSmsPermission())
            manifest.append("<uses-permission android:name=\"android.permission.SEND_SMS\"/>");
        if (apkConfig.isBluetoothPermission())
            manifest.append("<uses-permission android:name=\"android.permission.BLUETOOTH\"/>");
        manifest.append("<uses-permission android:name=\"android.permission.ACCESS_NETWORK_STATE\"/>");
        if (apkConfig.isEnablePush())
            manifest.append("<uses-permission android:name=\"android.permission.POST_NOTIFICATIONS\"/>");
        manifest.append("<application android:label=\"").append(apkName).append("\"")
            .append(" android:icon=\"@drawable/icon\"")
            .append(" android:allowBackup=\"true\"")
            .append(" android:hardwareAccelerated=\"true\"")
            .append(" android:usesCleartextTraffic=\"true\">");
        String appClose = "</application>";
        StringBuilder extras = new StringBuilder();
        if (apkConfig.isEnableSplash()) {
            extras.append("<activity android:name=\"").append(pkgPath).append(".SplashActivity\"")
                .append(" android:exported=\"true\"")
                .append(" android:theme=\"@style/SplashTheme\">")
                .append("<intent-filter><action android:name=\"android.intent.action.MAIN\"/>")
                .append("<category android:name=\"android.intent.category.LAUNCHER\"/>")
                .append("</intent-filter></activity>");
        }
        extras.append("<activity android:name=\"").append(pkgPath).append(".MainActivity\"")
            .append(" android:exported=\"true\"")
            .append(" android:configChanges=\"keyboard|keyboardHidden|orientation|screenSize\">");
        if (!apkConfig.isEnableSplash()) {
            extras.append("<intent-filter><action android:name=\"android.intent.action.MAIN\"/>")
                .append("<category android:name=\"android.intent.category.LAUNCHER\"/>")
                .append("</intent-filter>");
        }
        extras.append("</activity>");
        if (apkConfig.isEnablePush()) {
            extras.append("<service android:name=\"").append(pkgPath).append(".NotificationService\"")
                .append(" android:exported=\"false\"/>");
            extras.append("<meta-data android:name=\"com.google.firebase.messaging.default_notification_channel_id\"")
                .append(" android:value=\"default_channel\"/>");
        }
        if (apkConfig.isEnableAds()) {
            extras.append("<meta-data android:name=\"com.google.android.gms.ads.APPLICATION_ID\"")
                .append(" android:value=\"").append(apkConfig.getAdmobAppId()).append("\"/>");
        }
        manifest.append(extras).append(appClose).append("</manifest>");
        Files.write(Paths.get(workDir + "\\AndroidManifest.xml"), manifest.toString().getBytes(StandardCharsets.UTF_8));
        progressDialog.setTaskStatus(1, TaskProgressDialog.TaskStatus.COMPLETED);
        progressDialog.setTaskProgress(1, 1.0);
        progressDialog.logSuccess("AndroidManifest.xml created");

        // 3. Java source (with ads & notifications from config)
        progressDialog.setTaskStatus(2, TaskProgressDialog.TaskStatus.RUNNING);
        progressDialog.log("Writing Java source (WebView Activity)...");
        StringBuilder java = new StringBuilder();
        java.append("package ").append(pkgPath).append(";\n")
            .append("import android.app.Activity;\n")
            .append("import android.os.Bundle;\n")
            .append("import android.webkit.WebView;\n")
            .append("import android.webkit.WebSettings;\n")
            .append("import android.webkit.WebViewClient;\n")
            .append("import android.widget.LinearLayout;\n");
        if (apkConfig.isEnablePush())
            java.append("import android.content.Intent;\n");
        java.append("public class MainActivity extends Activity {\n")
            .append("    @Override\n")
            .append("    protected void onCreate(Bundle savedInstanceState) {\n")
            .append("        super.onCreate(savedInstanceState);\n")
            .append("        LinearLayout root = new LinearLayout(this);\n")
            .append("        root.setOrientation(LinearLayout.VERTICAL);\n")
            .append("        WebView wv = new WebView(this);\n")
            .append("        WebSettings s = wv.getSettings();\n")
            .append("        s.setJavaScriptEnabled(true);\n")
            .append("        s.setAllowFileAccess(true);\n")
            .append("        s.setAllowContentAccess(true);\n")
            .append("        s.setDomStorageEnabled(true);\n")
            .append("        s.setLoadWithOverviewMode(true);\n")
            .append("        s.setUseWideViewPort(true);\n")
            .append("        wv.setWebViewClient(new WebViewClient());\n")
            .append("        wv.loadUrl(\"file:///android_asset/_main_.html\");\n")
            .append("        root.addView(wv, new LinearLayout.LayoutParams(\n")
            .append("            LinearLayout.LayoutParams.MATCH_PARENT,\n")
            .append("            LinearLayout.LayoutParams.MATCH_PARENT, 1f));\n");
        if (apkConfig.isEnableAds()) {
            java.append("        // Ad banner\n")
                .append("        LinearLayout adBar = new LinearLayout(this);\n")
                .append("        adBar.setOrientation(LinearLayout.VERTICAL);\n")
                .append("        adBar.setLayoutParams(new LinearLayout.LayoutParams(\n")
                .append("            LinearLayout.LayoutParams.MATCH_PARENT,\n")
                .append("            (int) (getResources().getDisplayMetrics().density * 60)));\n")
                .append("        android.webkit.WebView adView = new android.webkit.WebView(this);\n")
                .append("        adView.setLayoutParams(new LinearLayout.LayoutParams(\n")
                .append("            LinearLayout.LayoutParams.MATCH_PARENT,\n")
                .append("            LinearLayout.LayoutParams.MATCH_PARENT));\n")
                .append("        adView.getSettings().setJavaScriptEnabled(true);\n")
                .append("        adView.loadUrl(\"file:///android_asset/ad.html\");\n")
                .append("        adBar.addView(adView);\n")
                .append("        root.addView(adBar);\n");
        }
        java.append("        setContentView(root);\n");
        if (apkConfig.isEnablePush()) {
            java.append("        // Start notification service\n")
                .append("        Intent nIntent = new Intent(this, ").append(pkgPath).append(".NotificationService.class);\n")
                .append("        nIntent.putExtra(\"title\", \"Welcome!\");\n")
                .append("        nIntent.putExtra(\"body\", \"Notifications are enabled.\");\n")
                .append("        startService(nIntent);\n");
        }
        java.append("    }\n").append("}");
        Files.write(Paths.get(srcDir + "\\" + pkgDir + "\\MainActivity.java"), java.toString().getBytes(StandardCharsets.UTF_8));
        // Generate SplashActivity if enabled
        if (apkConfig.isEnableSplash()) {
            String bgColor = apkConfig.getSplashBgColor();
            boolean isImageSplash = bgColor != null && bgColor.startsWith("IMAGE:");
            String splashImageAsset = null;
            if (isImageSplash) {
                splashImageAsset = "splash_bg.png";
                String imgPath = bgColor.substring(6);
                try {
                    Files.copy(new File(imgPath).toPath(),
                        new File(assetsDir, splashImageAsset).toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
                    bgColor = null;
                } catch (Exception e) {
                    bgColor = "#6C5CE7";
                    isImageSplash = false;
                }
            }
            String splashActivity = "package " + pkgPath + ";\n"
                + "import android.app.Activity;\n"
                + "import android.content.Intent;\n"
                + "import android.os.Bundle;\n"
                + "import android.os.Handler;\n"
                + "import android.widget.TextView;\n"
                + "import android.widget.ImageView;\n"
                + "import android.widget.RelativeLayout;\n"
                + "import android.graphics.Color;\n"
                + "import android.graphics.drawable.BitmapDrawable;\n"
                + "import android.graphics.BitmapFactory;\n"
                + "import android.view.Gravity;\n"
                + "import java.io.InputStream;\n"
                + "public class SplashActivity extends Activity {\n"
                + "    @Override\n"
                + "    protected void onCreate(Bundle savedInstanceState) {\n"
                + "        super.onCreate(savedInstanceState);\n"
                + "        RelativeLayout layout = new RelativeLayout(this);\n";
            if (isImageSplash) {
                splashActivity += "        try {\n"
                    + "            InputStream is = getAssets().open(\"" + splashImageAsset + "\");\n"
                    + "            layout.setBackground(new BitmapDrawable(getResources(), BitmapFactory.decodeStream(is)));\n"
                    + "            is.close();\n"
                    + "        } catch (Exception e) {\n"
                    + "            layout.setBackgroundColor(Color.parseColor(\"#6C5CE7\"));\n"
                    + "        }\n";
            } else {
                splashActivity += "        layout.setBackgroundColor(Color.parseColor(\"" + (bgColor != null ? bgColor : "#6C5CE7") + "\"));\n";
            }
            splashActivity +=
                "        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(\n"
                + "            RelativeLayout.LayoutParams.WRAP_CONTENT,\n"
                + "            RelativeLayout.LayoutParams.WRAP_CONTENT);\n"
                + "        lp.addRule(RelativeLayout.CENTER_IN_PARENT);\n"
                + "        TextView tv = new TextView(this);\n"
                + "        tv.setText(\"" + escapeJavaString(apkConfig.getSplashTitle()) + "\");\n"
                + "        tv.setTextColor(Color.parseColor(\"" + apkConfig.getSplashTextColor() + "\"));\n"
                + "        tv.setTextSize(24);\n"
                + "        tv.setGravity(Gravity.CENTER);\n"
                + "        layout.addView(tv, lp);\n"
                + "        setContentView(layout);\n"
                + "        new Handler().postDelayed(() -> {\n"
                + "            startActivity(new Intent(SplashActivity.this, MainActivity.class));\n"
                + "            finish();\n"
                + "        }, 2000);\n"
                + "    }\n"
                + "}";
            Files.write(Paths.get(srcDir + "\\" + pkgDir + "\\SplashActivity.java"), splashActivity.getBytes(StandardCharsets.UTF_8));
        }

        // Generate NotificationService if push enabled
        if (apkConfig.isEnablePush()) {
            String notifService = "package " + pkgPath + ";\n"
                + "import android.app.NotificationChannel;\n"
                + "import android.app.NotificationManager;\n"
                + "import android.app.Service;\n"
                + "import android.content.Intent;\n"
                + "import android.os.Build;\n"
                + "import android.os.IBinder;\n"
                + "import androidx.core.app.NotificationCompat;\n"
                + "public class NotificationService extends Service {\n"
                + "    public static final String CHANNEL_ID = \"default_channel\";\n"
                + "    @Override\n"
                + "    public void onCreate() {\n"
                + "        super.onCreate();\n"
                + "        createNotificationChannel();\n"
                + "    }\n"
                + "    @Override\n"
                + "    public int onStartCommand(Intent intent, int flags, int startId) {\n"
                + "        String title = intent != null ? intent.getStringExtra(\"title\") : \"Notification\";\n"
                + "        String body = intent != null ? intent.getStringExtra(\"body\") : \"\";\n"
                + "        showNotification(title, body);\n"
                + "        return START_NOT_STICKY;\n"
                + "    }\n"
                + "    @Override\n"
                + "    public IBinder onBind(Intent intent) { return null; }\n"
                + "    private void createNotificationChannel() {\n"
                + "        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {\n"
                + "            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, \"Default\", NotificationManager.IMPORTANCE_DEFAULT);\n"
                + "            NotificationManager nm = getSystemService(NotificationManager.class);\n"
                + "            nm.createNotificationChannel(ch);\n"
                + "        }\n"
                + "    }\n"
                + "    private void showNotification(String title, String body) {\n"
                + "        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)\n"
                + "            .setSmallIcon(android.R.drawable.ic_dialog_info)\n"
                + "            .setContentTitle(title)\n"
                + "            .setContentText(body)\n"
                + "            .setAutoCancel(true)\n"
                + "            .setPriority(NotificationCompat.PRIORITY_DEFAULT);\n"
                + "        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);\n"
                + "        nm.notify((int) System.currentTimeMillis(), b.build());\n"
                + "    }\n"
                + "}";
            Files.write(Paths.get(srcDir + "\\" + pkgDir + "\\NotificationService.java"), notifService.getBytes(StandardCharsets.UTF_8));
        }

        // Generate ad HTML if ads enabled (uses adsbygoogle.js in WebView - real Google ads, no native SDK)
        if (apkConfig.isEnableAds()) {
            String adUnitId = apkConfig.getAdmobBannerId();
            String pubId = adUnitId;
            String slotId = "";
            if (adUnitId.contains("/")) {
                int slash = adUnitId.indexOf("/");
                pubId = adUnitId.substring(0, slash);
                slotId = adUnitId.substring(slash + 1);
            }
            pubId = pubId.replace("-app", "");
            String adHtml = "<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<style>body{margin:0;padding:0;text-align:center;background:transparent}"
                + ".ad-container{min-height:50px;display:flex;align-items:center;justify-content:center}"
                + "ins.adsbygoogle{display:block!important}</style></head><body>"
                + "<div class=\"ad-container\">"
                + "<ins class=\"adsbygoogle\" style=\"display:block\" data-ad-client=\""
                + escapeJavaString(pubId) + "\" data-ad-slot=\"" + escapeJavaString(slotId)
                + "\" data-ad-format=\"auto\" data-full-width-responsive=\"true\"></ins></div>"
                + "<script async src=\"https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client="
                + escapeJavaString(pubId) + "\"></script>"
                + "<script>(adsbygoogle=window.adsbygoogle||[]).push({});</script></body></html>";
            Files.write(Paths.get(assetsDir + "\\ad.html"), adHtml.getBytes(StandardCharsets.UTF_8));
        }

        progressDialog.setTaskStatus(2, TaskProgressDialog.TaskStatus.COMPLETED);
        progressDialog.setTaskProgress(2, 1.0);
        progressDialog.logSuccess("Java source created");

        // 3. Resources (colors, styles)
        progressDialog.setTaskStatus(3, TaskProgressDialog.TaskStatus.RUNNING);
        progressDialog.log("Generating resources (strings, colors, styles)...");
        String strings = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources><string name=\"app_name\">" + apkName + "</string></resources>";
        Files.write(Paths.get(resDir + "\\strings.xml"), strings.getBytes(StandardCharsets.UTF_8));

        String splashBgColor = apkConfig.getSplashBgColor();
        if (splashBgColor != null && splashBgColor.startsWith("IMAGE:")) {
            splashBgColor = "#6C5CE7";
        }
        String colors = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "  <color name=\"primary\">" + apkConfig.getPrimaryColor() + "</color>\n"
            + "  <color name=\"primary_dark\">" + apkConfig.getStatusBarColor() + "</color>\n"
            + "  <color name=\"secondary\">" + apkConfig.getSecondaryColor() + "</color>\n"
            + "  <color name=\"splash_bg\">" + (splashBgColor != null ? splashBgColor : "#6C5CE7") + "</color>\n"
            + "  <color name=\"splash_text\">" + (apkConfig.getSplashTextColor() != null ? apkConfig.getSplashTextColor() : "#FFFFFF") + "</color>\n"
            + "</resources>";
        Files.write(Paths.get(resDir + "\\colors.xml"), colors.getBytes(StandardCharsets.UTF_8));

        String styles;
        if (apkConfig.isEnableSplash()) {
            styles = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<resources>"
                + "<style name=\"AppTheme\" parent=\"android:Theme.Material.Light.NoActionBar\">"
                + "<item name=\"android:windowFullscreen\">true</item>"
                + "</style>"
                + "<style name=\"SplashTheme\" parent=\"android:Theme.Material.Light.NoActionBar\">"
                + "<item name=\"android:windowBackground\">@color/splash_bg</item>"
                + "<item name=\"android:windowFullscreen\">true</item>"
                + "</style></resources>";
        } else {
            styles = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<resources>"
                + "<style name=\"AppTheme\" parent=\"android:Theme.Material.Light.NoActionBar\">"
                + "<item name=\"android:windowFullscreen\">true</item>"
                + "</style></resources>";
        }
        Files.write(Paths.get(resDir + "\\styles.xml"), styles.getBytes(StandardCharsets.UTF_8));
        progressDialog.setTaskStatus(3, TaskProgressDialog.TaskStatus.COMPLETED);
        progressDialog.setTaskProgress(3, 1.0);

        progressDialog.setTaskStatus(4, TaskProgressDialog.TaskStatus.RUNNING);
        progressDialog.log("Copying icon & assets...");
        Files.copy(Paths.get(pathlabel.getText()), Paths.get(workDir + "\\res\\drawable\\icon.png"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get(pathlabel.getText()), Paths.get(workDir + "\\res\\drawable-xhdpi\\icon.png"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get(apkSource), Paths.get(workDir + "\\assets\\_main_.html"), StandardCopyOption.REPLACE_EXISTING);
        extractZip(apkResource, workDir + "\\assets");
        progressDialog.setTaskStatus(4, TaskProgressDialog.TaskStatus.COMPLETED);
        progressDialog.setTaskProgress(4, 1.0);
        progressDialog.logSuccess("Assets copied");


        progressDialog.setTaskStatus(5, TaskProgressDialog.TaskStatus.RUNNING);
        progressDialog.log("Compiling Java source...");
        File srcPkgDir = new File(srcDir + "\\" + pkgDir);
        File[] javaFiles = srcPkgDir.listFiles((d, name) -> name.endsWith(".java"));
        if (javaFiles == null || javaFiles.length == 0) {
            throw new IOException("No .java files found in " + srcPkgDir);
        }
        StringBuilder javaFilesArg = new StringBuilder();
        for (File jf : javaFiles) {
            javaFilesArg.append(" \"").append(jf.getAbsolutePath()).append("\"");
        }
        runProcess("\"" + getJdkJavac() + "\" -J-Xmx512m -cp \"" + PLATFORM_JAR + ADMOB_JARS_CP + "\" -d \"" + objDir + "\"" + javaFilesArg.toString(), 60_000);
        progressDialog.log("Converting to DEX...");

        StringBuilder classFiles = new StringBuilder();
        appendClassFiles(objDir, classFiles);
        if (classFiles.length() == 0) {
            throw new IOException("No .class files found in " + objDir);
        }
        runProcess("\"" + getJdkJava() + "\" -Xmx512m -cp \"" + findD8Jar() + "\" com.android.tools.r8.D8"
            + " --lib \"" + PLATFORM_JAR + "\" --min-api 21 --output \"" + workDir + "\""
            + classFiles.toString() + ADMOB_JARS_D8, 120_000);
        progressDialog.setTaskStatus(5, TaskProgressDialog.TaskStatus.COMPLETED);
        progressDialog.setTaskProgress(5, 1.0);
        progressDialog.logSuccess("DEX generated");

        progressDialog.setTaskStatus(6, TaskProgressDialog.TaskStatus.RUNNING);
        progressDialog.log("Compiling resources (aapt2)...");
        runProcess("\"" + BUILD_TOOLS + "\\aapt2.exe\" compile --dir \"" + workDir + "\\res\" -o \"" + workDir + "\\res.flata\"", 60_000);
        progressDialog.log("Linking resources...");
        runProcess("\"" + BUILD_TOOLS + "\\aapt2.exe\" link --auto-add-overlay -o \"" + workDir + "\\app.ap_\" -I \"" + PLATFORM_JAR + "\" --manifest \"" + workDir + "\\AndroidManifest.xml\" -R \"" + workDir + "\\res.flata\"", 60_000);
        progressDialog.setTaskStatus(6, TaskProgressDialog.TaskStatus.COMPLETED);
        progressDialog.setTaskProgress(6, 1.0);
        progressDialog.logSuccess("Resources compiled & linked");

        progressDialog.setTaskStatus(7, TaskProgressDialog.TaskStatus.RUNNING);
        progressDialog.log("Assembling APK...");
        buildApkPackage(workDir, workDir + "\\unsigned.apk");
        progressDialog.setTaskStatus(7, TaskProgressDialog.TaskStatus.COMPLETED);
        progressDialog.setTaskProgress(7, 1.0);
        progressDialog.logSuccess("APK assembled");

        progressDialog.setTaskStatus(8, TaskProgressDialog.TaskStatus.RUNNING);
        progressDialog.log("Signing APK...");
        ensureKeystore();
        runProcess("\"" + getJdkJarsigner() + "\" -keystore \"" + KEYSTORE_PATH + "\" -storepass " + KEYSTORE_PASS
            + " -keypass " + KEYSTORE_PASS + " -digestalg SHA1 -sigalg SHA1withRSA"
            + " -signedjar \"" + workDir + "\\signed.apk\" \"" + workDir + "\\unsigned.apk\" " + KEYSTORE_ALIAS, 60_000);
        progressDialog.setTaskStatus(8, TaskProgressDialog.TaskStatus.COMPLETED);
        progressDialog.setTaskProgress(8, 1.0);
        progressDialog.logSuccess("APK signed");

        progressDialog.setTaskStatus(9, TaskProgressDialog.TaskStatus.RUNNING);
        progressDialog.log("Zipaligning...");
        runProcess("\"" + BUILD_TOOLS + "\\zipalign.exe\" -v 4 \"" + workDir + "\\signed.apk\" \"" + finalApkPath + "\"", 60_000);
        progressDialog.setTaskStatus(9, TaskProgressDialog.TaskStatus.COMPLETED);
        progressDialog.setTaskProgress(9, 1.0);
        progressDialog.logSuccess("APK saved: " + finalApkPath);

        progressDialog.log("Cleaning up...");
        deleteDirectory(workDir);
        progressDialog.logSuccess("Build complete!");
    }

    private static String escapeJavaString(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    private void buildApkPackage(File workDir, String outputPath) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputPath))) {
            // Add the resource APK contents
            File ap_ = new File(workDir, "app.ap_");
            if (ap_.exists()) {
                try (ZipInputStream zis = new ZipInputStream(new FileInputStream(ap_))) {
                    ZipEntry e;
                    while ((e = zis.getNextEntry()) != null) {
                        zos.putNextEntry(new ZipEntry(e.getName()));
                        copyStream(zis, zos);
                        zos.closeEntry();
                    }
                }
            }

            File dex = new File(workDir, "classes.dex");
            if (dex.exists()) {
                zos.putNextEntry(new ZipEntry("classes.dex"));
                Files.copy(dex.toPath(), zos);
                zos.closeEntry();
            }

            File assets = new File(workDir, "assets");
            if (assets.exists() && assets.isDirectory()) {
                zipDirectory(assets, assets.getAbsolutePath().length() + 1, zos);
            }
        }
    }

    private void zipDirectory(File dir, int baseLen, ZipOutputStream zos) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            String entryName = f.getAbsolutePath().substring(baseLen).replace("\\", "/");
            if (f.isDirectory()) {
                zos.putNextEntry(new ZipEntry(entryName + "/"));
                zos.closeEntry();
                zipDirectory(f, baseLen, zos);
            } else {
                zos.putNextEntry(new ZipEntry("assets/" + entryName));
                Files.copy(f.toPath(), zos);
                zos.closeEntry();
            }
        }
    }

    private void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
    }

    private void extractZip(String zipPath, String outputDir) throws IOException {
        byte[] buffer = new byte[8192];
        File outputFolder = new File(outputDir);
        outputFolder.mkdirs();
        String destDirPath = outputFolder.getCanonicalPath();

        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipPath)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(outputFolder, entry.getName());
                String destFilePath = newFile.getCanonicalPath();
                if (!destFilePath.startsWith(destDirPath + File.separator)) {
                    continue;
                }
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    newFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) fos.write(buffer, 0, len);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private void runProcess(String command, long timeoutMs) throws Exception {

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
                progressDialog.log(line);
            }
        }
        int exitCode = p.waitFor();
        if (exitCode != 0) {
            throw new IOException("Command failed (code " + exitCode + ")");
        }
    }

    private void ensureKeystore() throws Exception {
        File ks = new File(KEYSTORE_PATH);
        if (!ks.exists()) {
            ks.getParentFile().mkdirs();
            runProcess("\"" + getJdkKeytool() + "\" -genkeypair -keystore \"" + KEYSTORE_PATH
                + "\" -alias " + KEYSTORE_ALIAS + " -keypass " + KEYSTORE_PASS
                + " -storepass " + KEYSTORE_PASS
                + " -dname \"" + KEYSTORE_DNAME + "\" -validity 3650 -keyalg RSA -keysize 2048", 30_000);
            progressDialog.logSuccess("Keystore created at: " + KEYSTORE_PATH);
        }
    }

    private String getJdkHome() {
        String javaHome = System.getProperty("java.home");
        if (javaHome.endsWith("jre")) javaHome = javaHome.substring(0, javaHome.length() - 4) + "jdk";
        if (!new File(javaHome + "\\bin\\javac.exe").exists()) {
            // Try common JDK paths
            String[] jdkPaths = {
                "C:\\Program Files\\Java\\jdk1.8.0_491",
                "C:\\Program Files\\Java\\jdk1.8.0_161",
                "C:\\Program Files\\Java\\latest",
                "C:\\Program Files\\Java\\jdk-17",
                "C:\\Program Files\\Java\\jdk-21"
            };
            for (String p : jdkPaths) {
                if (new File(p + "\\bin\\javac.exe").exists()) return p;
            }
        }
        return javaHome;
    }

    private String getJdkJavac() { return getJdkHome() + "\\bin\\javac.exe"; }
    private String getJdkJava() { return getJdkHome() + "\\bin\\java.exe"; }
    private String getJdkJarsigner() { return getJdkHome() + "\\bin\\jarsigner.exe"; }
    private String getJdkKeytool() { return getJdkHome() + "\\bin\\keytool.exe"; }

    private File findD8Jar() {
        File f = new File(BUILD_TOOLS + "\\lib\\d8.jar");
        if (f.exists()) return f;
        return new File(BUILD_TOOLS + "\\d8.jar");
    }

    private static String detectAdmobJars(String sep) {
        String[] bases = {BUNDLED_TOOLS, System.getProperty("user.dir") + "\\tools"};
        try {
            File jarPath = new File(CreateHtmlApkController.class
                .getProtectionDomain().getCodeSource().getLocation().toURI());
            String parent = jarPath.getParentFile().getAbsolutePath();
            bases = new String[]{BUNDLED_TOOLS, System.getProperty("user.dir") + "\\tools",
                parent + "\\..\\tools"};
        } catch (Exception ignored) {}
        for (String base : bases) {
            String s = "";
            for (String j : new String[]{"play-services-ads.jar", "play-services-ads-lite.jar", "play-services-ads-base.jar", "play-services-basement.jar", "play-services-tasks.jar"}) {
                File f = new File(base + "\\" + j);
                if (f.exists()) {
                    if (sep.equals(" ")) s += " \"" + f.getAbsolutePath() + "\"";
                    else s += ";" + f.getAbsolutePath();
                }
            }
            if (!s.isEmpty()) return s;
        }
        return "";
    }

    private void appendClassFiles(File dir, StringBuilder sb) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                appendClassFiles(f, sb);
            } else if (f.getName().endsWith(".class")) {
                sb.append(" \"").append(f.getAbsolutePath()).append("\"");
            }
        }
    }

    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDirectory(f);
                else f.delete();
            }
        }
        dir.delete();
    }

    private void showAlert(String title, String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.WARNING);
            a.setTitle(title);
            a.setHeaderText(null);
            a.setContentText(msg);
            a.showAndWait();
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        java.io.InputStream is = getClass().getResourceAsStream("/com/eagle/icons/image.png");
        if (is != null) {
            imgview.setImage(new Image(is));
        }
    }
}
