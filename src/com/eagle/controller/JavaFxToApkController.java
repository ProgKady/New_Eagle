package com.eagle.controller;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.eagle.icons.IconManager;
import org.controlsfx.control.Notifications;

public class JavaFxToApkController extends AbstractApkBuilder {

    enum BuildMode { GLUON, WEBVIEW }

    @FXML private TextField jarPathField;
    @FXML private Label modeInfoLabel;
    @FXML private TextField graalVmField;
    @FXML private TextField gradlePathField;
    @FXML private RadioButton gluonMode;
    @FXML private RadioButton webviewMode;
    @FXML private ToggleGroup modeGroup;

    private BuildMode buildMode = BuildMode.GLUON;

    @Override protected String getHeaderTitle() { return "JavaFX to Android APK"; }
    @Override protected String getHeaderSubtitle() { return "Convert JavaFX JAR into a native Android app"; }
    @Override protected String getSourcePrompt() { return "Select JavaFX JAR"; }
    @Override protected String getExtraPrompt() { return null; }
    @Override protected String getSourceExtension() { return "*.jar"; }
    @Override protected String getExtraExtension() { return null; }
    @Override protected int getTaskCount() { return buildMode == BuildMode.GLUON ? 6 : 11; }
    @Override protected String getSourceInput() { return jarPathField.getText().trim(); }
    @Override protected String getExtraInput() { return null; }

    @FXML
    void browse1act(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select JavaFX JAR");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Java Archive", "*.jar"));
        File f = fc.showOpenDialog(jarPathField.getScene().getWindow());
        if (f != null) {
            jarPathField.setText(f.getAbsolutePath());
            onBrowseSource(f);
        }
    }

    @Override protected void onBrowseSource(File f) {
        if (apkname.getText().isEmpty()) {
            String name = f.getName();
            if (name.toLowerCase().endsWith(".jar")) {
                name = name.substring(0, name.length() - 4);
            }
            apkname.setText(name);
        }
        create.setDisable(false);
    }

    @Override protected void onBrowseExtra(File f) {}

    @Override protected boolean validateInputs(String apkName, String apkPkg, String apkIcon, String sourceInput, String extraInput) {
        if (sourceInput.isEmpty()) {
            showAlert("Missing JAR", "Please select a JavaFX JAR file.");
            return false;
        }
        File jarFile = new File(sourceInput);
        if (!jarFile.exists() || !jarFile.getName().toLowerCase().endsWith(".jar")) {
            showAlert("Invalid file", "Please select a valid .jar file.");
            return false;
        }
        if (buildMode == BuildMode.GLUON) {
            File graalVm = findGraalVm();
            if (graalVm == null || !new File(graalVm, "bin/java.exe").exists()) {
                showAlert("GraalVM Required",
                    "Gluon Native mode requires GraalVM JDK 21.\n"
                    + "Place GraalVM in the 'tools/graalvm/' folder next to the app,\n"
                    + "or enter the path in the GraalVM Path field above,\n"
                    + "or set the GRAALVM_HOME environment variable.");
                return false;
            }
        }
        return true;
    }

    @Override
    @FXML
    void createact(ActionEvent event) {
        if (buildMode == BuildMode.WEBVIEW) {
            super.createact(event);
            return;
        }

        String apkName = apkname.getText().trim();
        String apkPkg = packagename.getText().trim();
        String apkIcon = pathlabel.getText().trim();
        String sourceInput = getSourceInput();

        if (apkName.isEmpty() || apkPkg.isEmpty() || apkIcon.isEmpty()) {
            showAlert("Missing fields", "Please fill in APK Name, Package Name and select an icon.");
            return;
        }
        if (!validateInputs(apkName, apkPkg, apkIcon, sourceInput, null)) return;

        Stage dfdf = (Stage) packagename.getScene().getWindow();
        apkConfig.setAppName(apkName);
        apkConfig.setPackageName("com.kadysoft." + apkPkg);
        apkConfig.setIconFile(new File(apkIcon));
        apkConfig.setSourceInput(sourceInput);

        ApkCustomizerDialog customizer = new ApkCustomizerDialog(dfdf, apkConfig);
        customizer.show();
        dfdf.setIconified(true);

        java.util.List<String> themeSheets = apkname.getScene() != null
            ? new java.util.ArrayList<>(apkname.getScene().getStylesheets()) : null;
        progressDialog = new TaskProgressDialog("Gluon Build: " + apkName);
        if (themeSheets != null) progressDialog.setThemeStylesheets(themeSheets);
        progressDialog.addTask("Detecting GraalVM");
        progressDialog.addTask("Creating Gradle project");
        progressDialog.addTask("Searching for Gradle");
        progressDialog.addTask("Running GluonFX build");
        progressDialog.addTask("Copying APK");
        progressDialog.addTask("Cleanup");
        progressDialog.show();
        progressDialog.log("Starting Gluon native build pipeline...");

        Task<Void> buildTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                buildWithGluon(apkName, apkPkg, sourceInput);
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
                        public void run() { progressDialog.close(); dfdf.setIconified(false); dfdf.close(); }
                    }, 3000);
                });
            }
        };

        Thread thread = new Thread(buildTask);
        thread.setDaemon(true);
        thread.start();
    }

    // --- WebView mode (old behavior) ---

    @Override
    protected void generateManifest(String pkgPath, StringBuilder xml) {
        xml.append("<uses-permission android:name=\"android.permission.INTERNET\"/>")
            .append("<application android:label=\"").append(apkname.getText().trim()).append("\"")
            .append(" android:icon=\"@drawable/icon\" android:allowBackup=\"true\"")
            .append(" android:hardwareAccelerated=\"true\"")
            .append(" android:usesCleartextTraffic=\"true\">")
            .append("<activity android:name=\"").append(pkgPath).append(".MainActivity\"")
            .append(" android:exported=\"true\"")
            .append(" android:configChanges=\"keyboard|keyboardHidden|orientation|screenSize\">")
            .append("<intent-filter><action android:name=\"android.intent.action.MAIN\"/>")
            .append("<category android:name=\"android.intent.category.LAUNCHER\"/>")
            .append("</intent-filter></activity></application>");
    }

    @Override
    protected void generateJavaSource(String pkgPath, StringBuilder java) {
        java.append("import android.app.Activity;\n")
            .append("import android.os.Bundle;\n")
            .append("import android.webkit.WebView;\n")
            .append("import android.webkit.WebSettings;\n")
            .append("import android.webkit.WebViewClient;\n")
            .append("public class MainActivity extends Activity {\n")
            .append("    @Override\n")
            .append("    protected void onCreate(Bundle savedInstanceState) {\n")
            .append("        super.onCreate(savedInstanceState);\n")
            .append("        WebView wv = new WebView(this);\n")
            .append("        WebSettings s = wv.getSettings();\n")
            .append("        s.setJavaScriptEnabled(true);\n")
            .append("        s.setAllowFileAccess(true);\n")
            .append("        s.setAllowContentAccess(true);\n")
            .append("        s.setDomStorageEnabled(true);\n")
            .append("        s.setLoadWithOverviewMode(true);\n")
            .append("        s.setUseWideViewPort(true);\n")
            .append("        s.setAllowFileAccessFromFileURLs(true);\n")
            .append("        s.setAllowUniversalAccessFromFileURLs(true);\n")
            .append("        wv.setWebViewClient(new WebViewClient());\n")
            .append("        wv.loadUrl(\"file:///android_asset/_main_.html\");\n")
            .append("        setContentView(wv);\n")
            .append("    }\n")
            .append("}");
    }

    private void buildWithWebView(File workDir, String sourceInput, String apkName) throws Exception {
        File jarFile = new File(sourceInput);
        if (!jarFile.exists()) {
            throw new IOException("JAR file not found: " + sourceInput);
        }

        File assetsDir = new File(workDir, "assets");
        assetsDir.mkdirs();
        File jarContentDir = new File(assetsDir, "jar_content");
        jarContentDir.mkdirs();

        String mainClass = "";
        String implVersion = "";
        List<String> classFiles = new ArrayList<>();
        List<String> fxmlFiles = new ArrayList<>();
        List<String> cssFiles = new ArrayList<>();
        List<String> imageFiles = new ArrayList<>();
        List<String> otherFiles = new ArrayList<>();
        long totalJarSize = jarFile.length();

        try (JarInputStream jis = new JarInputStream(new FileInputStream(jarFile))) {
            Manifest mf = jis.getManifest();
            if (mf != null) {
                java.util.jar.Attributes attrs = mf.getMainAttributes();
                if (attrs != null) {
                    mainClass = attrs.getValue("Main-Class");
                    if (mainClass == null) mainClass = "";
                    implVersion = attrs.getValue("Implementation-Version");
                    if (implVersion == null) implVersion = "";
                }
            }
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                String name = entry.getName();
                if (entry.isDirectory()) {
                    new File(jarContentDir, name).mkdirs();
                    continue;
                }
                File outFile = new File(jarContentDir, name);
                outFile.getParentFile().mkdirs();
                Files.copy(jis, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                String lower = name.toLowerCase();
                if (name.endsWith(".class")) {
                    classFiles.add(name);
                } else if (lower.endsWith(".fxml")) {
                    fxmlFiles.add(name);
                } else if (lower.endsWith(".css")) {
                    cssFiles.add(name);
                } else if (lower.matches(".*\\.(png|jpg|jpeg|gif|bmp|svg|ico|webp)$")) {
                    imageFiles.add(name);
                } else if (!name.startsWith("META-INF/") && !name.equals("MANIFEST.MF")) {
                    otherFiles.add(name);
                }
            }
        }

        String jarName = jarFile.getName();
        String appName = apkname.getText().trim();
        if (appName.isEmpty()) appName = jarName.replaceAll("(?i)\\.jar$", "");

        String html = generateMainHtml(appName, jarName, mainClass, implVersion,
            totalJarSize, classFiles, fxmlFiles, cssFiles, imageFiles, otherFiles);
        Files.write(new File(assetsDir, "_main_.html").toPath(), html.getBytes(StandardCharsets.UTF_8));

        progressDialog.logSuccess("JAR extracted (" + totalSizeString(totalJarSize) + ")");
        progressDialog.logSuccess("Found: " + classFiles.size() + " classes, "
            + fxmlFiles.size() + " FXML, " + cssFiles.size() + " CSS, "
            + imageFiles.size() + " images");
    }

    @Override
    protected void copyAssets(File workDir, String sourceInput, String extraInput) throws Exception {
        buildWithWebView(workDir, sourceInput, apkname.getText().trim());
    }

    // --- Gluon/GraalVM mode ---

    private void buildWithGluon(String apkName, String apkPkg, String sourceInput) throws Exception {
        String finalApkPath = System.getProperty("user.home") + "\\Desktop\\" + apkName + ".apk";

        progressDialog.setTaskStatus(0, TaskProgressDialog.TaskStatus.RUNNING);
        progressDialog.log("Detecting GraalVM...");
        File graalVm = findGraalVm();
        if (graalVm == null || !new File(graalVm, "bin/java.exe").exists()) {
            throw new IOException("GraalVM not found.\n\n"
                + "Gluon Native mode requires GraalVM JDK 21.\n"
                + "1. Download from: https://github.com/graalvm/graalvm-ce-builds/releases\n"
                + "2. Extract and place in the 'tools/graalvm/' folder next to the app\n"
                + "3. Or set the GRAALVM_HOME environment variable\n"
                + "4. Or enter the path in the GraalVM Path field above");
        }
        progressDialog.logSuccess("GraalVM: " + graalVm.getAbsolutePath());

        progressDialog.log("Checking Android SDK...");
        String androidHome = null;
        // 1. Check bundled in tools/android-sdk/ first
        for (File base : getAppBaseDirs()) {
            File bundledSdk = new File(base, "tools\\android-sdk");
            if (new File(bundledSdk, "platforms").exists()) {
                androidHome = bundledSdk.getAbsolutePath();
                break;
            }
        }
        // 2. Environment variables
        if (androidHome == null) {
            String env = System.getenv("ANDROID_HOME");
            if (env != null && !env.isEmpty()) androidHome = env;
        }
        if (androidHome == null) {
            String env = System.getenv("ANDROID_SDK_ROOT");
            if (env != null && !env.isEmpty()) androidHome = env;
        }
        // 3. Common paths
        if (androidHome == null) {
            String[] sdkPaths = {
                System.getProperty("user.home") + "\\AppData\\Local\\Android\\Sdk",
                "C:\\Android\\Sdk",
                "C:\\Android\\android-sdk",
            };
            for (String p : sdkPaths) {
                if (new File(p + "\\platforms").exists()) {
                    androidHome = p; break;
                }
            }
        }
        if (androidHome != null && !androidHome.isEmpty()) {
            progressDialog.logSuccess("Android SDK: " + androidHome);
        } else {
            progressDialog.log("Android SDK not detected. Gluon will use its bundled SDK.");
        }
        // Check Android NDK (bundled in tools/android-ndk/ or with Android SDK)
        String androidNdkHome = null;
        // 1. Bundled in tools/android-ndk/
        for (File base : getAppBaseDirs()) {
            File bundledNdk = new File(base, "tools\\android-ndk");
            if (new File(bundledNdk, "toolchains").exists()) {
                androidNdkHome = bundledNdk.getAbsolutePath();
                break;
            }
        }
        // 2. With Android SDK
        if (androidNdkHome == null && androidHome != null) {
            File ndkBundle = new File(androidHome, "ndk-bundle");
            if (ndkBundle.exists()) {
                androidNdkHome = ndkBundle.getAbsolutePath();
            } else {
                File ndkDir = new File(androidHome, "ndk");
                if (ndkDir.exists()) {
                    File[] ndkVersions = ndkDir.listFiles(File::isDirectory);
                    if (ndkVersions != null && ndkVersions.length > 0) {
                        androidNdkHome = ndkVersions[0].getAbsolutePath();
                    }
                }
            }
        }
        if (androidNdkHome != null) {
            progressDialog.logSuccess("Android NDK: " + androidNdkHome);
        } else {
            progressDialog.log("Android NDK not found. Install in tools/android-ndk/ or via sdkmanager \"ndk-bundle\"");
        }
        progressDialog.setTaskStatus(0, TaskProgressDialog.TaskStatus.COMPLETED);

        progressDialog.setTaskStatus(1, TaskProgressDialog.TaskStatus.RUNNING);
        progressDialog.log("Creating Gradle project...");

        File projectDir = new File(System.getProperty("java.io.tmpdir")
            + "\\gluon_build_" + System.currentTimeMillis());
        projectDir.mkdirs();

        File jarFile = new File(sourceInput);
        String mainClass = "";
        try (JarInputStream jis = new JarInputStream(new FileInputStream(jarFile))) {
            Manifest mf = jis.getManifest();
            if (mf != null) {
                java.util.jar.Attributes attrs = mf.getMainAttributes();
                if (attrs != null) {
                    mainClass = attrs.getValue("Main-Class");
                    if (mainClass == null) mainClass = "";
                }
            }
        }
        if (mainClass.isEmpty()) {
            throw new IOException("No Main-Class found in JAR manifest.\n"
                + "The JAR must have a valid META-INF/MANIFEST.MF with a Main-Class entry.");
        }

        generateGradleProject(projectDir, apkName, apkPkg, mainClass, jarFile, graalVm, androidHome);
        progressDialog.setTaskStatus(1, TaskProgressDialog.TaskStatus.COMPLETED);
        progressDialog.logSuccess("Gradle project created");

        progressDialog.setTaskStatus(2, TaskProgressDialog.TaskStatus.RUNNING);
        progressDialog.log("Preparing Gradle...");

        String gradleCmd = findGradle(projectDir);
        progressDialog.logSuccess("Using: " + gradleCmd);
        progressDialog.setTaskStatus(2, TaskProgressDialog.TaskStatus.COMPLETED);

        progressDialog.setTaskStatus(3, TaskProgressDialog.TaskStatus.RUNNING);
        progressDialog.log("Running Gradle build (GluonFX plugin)...");
        progressDialog.log("First run downloads dependencies — may take several minutes.");

        Map<String, String> env = new HashMap<>();
        String graalHome = graalVm.getAbsolutePath();
        env.put("JAVA_HOME", graalHome);
        env.put("GRAALVM_HOME", graalHome);
        if (androidHome != null) env.put("ANDROID_HOME", androidHome);
        if (androidNdkHome != null) env.put("ANDROID_NDK_HOME", androidNdkHome);
        env.put("PATH", graalHome + "\\bin;" + System.getenv("PATH"));

        // Try nativePackage first (GluonFX v1.0.29+), fall back to nativeBuild
        boolean built = false;
        String[] taskCandidates = {
            "build nativePackage",
            "build nativeBuild",
            "nativePackage",
            "nativeBuild"
        };
        for (String task : taskCandidates) {
            try {
                progressDialog.log("> gradle " + task);
                runProcessInDir(gradleCmd + " " + task + " --no-daemon --console=plain --stacktrace", 900_000, projectDir, env);
                built = true;
                break;
            } catch (Exception e) {
                String msg = e.getMessage();
                if (msg != null && msg.contains("can't compile to aarch64-linux-android")) {
                    String wslDistro = getWslDistro();
                    if (wslDistro != null) {
                        progressDialog.log("Cross-compile not supported on Windows.");
                        progressDialog.log("WSL distribution detected: " + wslDistro);
                        progressDialog.log("Retrying build via WSL...");
                        try {
                            runViaWsl(wslDistro, task + " --no-daemon --console=plain --stacktrace", projectDir, env);
                            built = true;
                            break;
                        } catch (Exception wslErr) {
                            throw new IOException("Build failed both on Windows and via WSL.\n"
                                + "Windows: " + e.getMessage() + "\n"
                                + "WSL: " + wslErr.getMessage() + "\n"
                                + "Troubleshoot:\n"
                                + "  1. Ensure GraalVM/Android SDK are accessible from WSL\n"
                                + "  2. Run: wsl --install -d Ubuntu\n"
                                + "  3. See: https://docs.gluonhq.com/#platforms_android_development\n"
                                + "Project retained at: " + projectDir);
                        }
                    }
                    throw new IOException("GluonFX cannot cross-compile from Windows to Android.\n"
                        + "Please install WSL2 with Ubuntu and run the build from Linux.\n"
                        + "See: https://docs.gluonhq.com/#platforms_android_development\n"
                        + "Project retained at: " + projectDir);
                }
                progressDialog.log("Task '" + task + "' failed, trying next...");
            }
        }
        if (!built) {
            throw new IOException("GluonFX build failed with all task variants.\n"
                + "Check the build logs above for details.\n"
                + "Common issues:\n"
                + "  1. GraalVM JDK version mismatch (need 11+)\n"
                + "  2. Missing native-image component (run: gu install native-image)\n"
                + "  3. Android SDK/NDK not found (install NDK via SDK Manager)\n"
                + "  4. On Windows, Android builds require WSL2 with Ubuntu\n"
                + "Project retained at: " + projectDir);
        }
        progressDialog.setTaskStatus(3, TaskProgressDialog.TaskStatus.COMPLETED);
        progressDialog.logSuccess("Gradle build complete");

        progressDialog.setTaskStatus(4, TaskProgressDialog.TaskStatus.RUNNING);
        progressDialog.log("Locating output APK...");
        File apkFile = findGluonApk(projectDir);
        if (apkFile != null && apkFile.exists()) {
            Files.copy(apkFile.toPath(), Paths.get(finalApkPath), StandardCopyOption.REPLACE_EXISTING);
            progressDialog.logSuccess("APK saved: " + finalApkPath);
        } else {
            progressDialog.log("APK not found in expected location. Searching deeper...");
            // Second pass: list entire build dir
            StringBuilder sb = new StringBuilder();
            listDir(new File(projectDir, "build"), sb, 0);
            progressDialog.log("Build directory contents:\n" + sb.toString());
            throw new IOException("Gluon build completed but APK not found.\n"
                + "Project retained at: " + projectDir);
        }
        progressDialog.setTaskStatus(4, TaskProgressDialog.TaskStatus.COMPLETED);

        progressDialog.setTaskStatus(5, TaskProgressDialog.TaskStatus.RUNNING);
        progressDialog.log("Cleaning up...");
        deleteDirectory(projectDir);
        progressDialog.setTaskStatus(5, TaskProgressDialog.TaskStatus.COMPLETED);
        progressDialog.logSuccess("Gluon native build complete!");
    }

    private void listDir(File dir, StringBuilder sb, int depth) {
        if (depth > 4 || dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            for (int i = 0; i < depth; i++) sb.append("  ");
            sb.append(f.isDirectory() ? "[DIR] " : "[FILE] ").append(f.getName());
            if (!f.isDirectory()) sb.append(" (").append(f.length()).append(" bytes)");
            sb.append("\n");
            if (f.isDirectory()) listDir(f, sb, depth + 1);
        }
    }

    private File findGraalVm() {
        // 1. Check bundled tools/graalvm/ (relative to app's own folder)
        File bundled = getBundledGraalVm();
        if (bundled != null) {
            graalVmField.setText(bundled.getAbsolutePath());
            return bundled;
        }
        // 2. User-entered path
        String userPath = graalVmField.getText().trim();
        if (!userPath.isEmpty()) {
            File f = new File(userPath);
            if (new File(f, "bin/java.exe").exists()) return f;
            if (new File(f, "bin/javac.exe").exists()) return f;
        }
        // 3. Environment variables
        String home = System.getenv("GRAALVM_HOME");
        if (home != null) {
            File f = new File(home);
            if (new File(f, "bin/java.exe").exists()) return f;
        }
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && isGraalVm(new File(javaHome))) return new File(javaHome);
        // 4. Common install paths
        String[] candidates = {
            "C:\\Program Files\\Java\\graalvm",
            "C:\\Program Files\\Java\\graalvm-ce-java11-22.3.0",
            "C:\\Program Files\\Java\\graalvm-ce-java17-22.3.0",
            "C:\\Program Files\\GraalVM",
            System.getProperty("user.home") + "\\tools\\graalvm",
            System.getProperty("user.home") + "\\Downloads\\graalvm",
        };
        for (String c : candidates) {
            File f = new File(c);
            if (isGraalVm(f)) return f;
        }
        // 5. Current JDK
        if (isGraalVm(new File(System.getProperty("java.home")))) {
            return new File(System.getProperty("java.home"));
        }
        return null;
    }

    private File getBundledGraalVm() {
        for (File dir : getAppBaseDirs()) {
            File gv = new File(dir, "tools/graalvm");
            if (isGraalVm(gv)) return gv.getAbsoluteFile();
        }
        return null;
    }

    private List<File> getAppBaseDirs() {
        List<File> dirs = new ArrayList<>();
        dirs.add(new File(System.getProperty("user.dir")));
        try {
            URL url = JavaFxToApkController.class.getProtectionDomain()
                .getCodeSource().getLocation();
            File loc = new File(url.toURI());
            if (loc.isFile()) {
                dirs.add(loc.getParentFile()); // dist/
                dirs.add(loc.getParentFile().getParentFile()); // project root
            } else {
                dirs.add(loc); // build/classes/
                dirs.add(loc.getParentFile().getParentFile()); // project root
            }
        } catch (Exception ignored) {}
        return dirs;
    }

    private boolean isGraalVm(File dir) {
        if (dir == null || !dir.exists()) return false;
        File javaExe = new File(dir, "bin/java.exe");
        if (!javaExe.exists()) return false;
        // Check for GraalVM native-image tool (distinctive feature)
        if (new File(dir, "bin/native-image.cmd").exists()
            || new File(dir, "bin/native-image").exists()) return true;
        // Check by running java -version
        try {
            Process p = new ProcessBuilder(javaExe.getAbsolutePath(), "-version")
                .redirectErrorStream(true).start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String output = r.readLine();
                p.waitFor();
                return output != null && output.toLowerCase().contains("graal");
            }
        } catch (Exception e) {
            return false;
        }
    }

    private String findGradle(File projectDir) throws IOException {
        // 1. User-entered path
        String userGradle = gradlePathField.getText().trim();
        if (!userGradle.isEmpty()) {
            File f = new File(userGradle);
            if (isGradleDir(f)) return quotePath(f.getAbsolutePath() + "\\bin\\gradle");
        }
        // 2. Bundled tools/gradle/ (relative to app)
        File bundled = getBundledGradle();
        if (bundled != null) {
            gradlePathField.setText(bundled.getAbsolutePath());
            return quotePath(bundled.getAbsolutePath() + "\\bin\\gradle");
        }
        // 3. GRADLE_HOME
        String gh = System.getenv("GRADLE_HOME");
        if (gh != null && isGradleDir(new File(gh))) return quotePath(gh + "\\bin\\gradle");
        // 4. System PATH
        try {
            ProcessBuilder pb = new ProcessBuilder("gradle", "--version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            if (p.waitFor() == 0) return "gradle";
        } catch (Exception e) { }
        // 5. Try gradlew
        File gradlew = new File(projectDir, "gradlew.bat");
        if (gradlew.exists()) return quotePath(gradlew.getAbsolutePath());
        // 6. Generate wrapper
        generateGradleWrapper(projectDir);
        if (gradlew.exists()) return quotePath(gradlew.getAbsolutePath());
        throw new IOException("Gradle not found.\n"
            + "Place Gradle in tools/gradle/ or set GRADLE_HOME.\n"
            + "Download: https://gradle.org/releases/");
    }

    private String quotePath(String p) {
        return p.contains(" ") ? "\"" + p + "\"" : p;
    }

    private boolean isGradleDir(File dir) {
        if (dir == null || !dir.exists()) return false;
        return new File(dir, "bin/gradle.bat").exists()
            || new File(dir, "bin/gradle").exists();
    }

    private File getBundledGradle() {
        for (File base : getAppBaseDirs()) {
            File gd = new File(base, "tools/gradle");
            if (isGradleDir(gd)) return gd.getAbsoluteFile();
        }
        return null;
    }

    private void generateGradleWrapper(File projectDir) throws IOException {
        File wrapperDir = new File(projectDir, "gradle/wrapper");
        wrapperDir.mkdirs();

        String props = "distributionBase=GRADLE_USER_HOME\n"
            + "distributionPath=wrapper/dists\n"
            + "distributionUrl=https\\://services.gradle.org/distributions/gradle-8.4-bin.zip\n"
            + "networkTimeout=30000\n"
            + "validateDistributionUrl=true\n"
            + "zipStoreBase=GRADLE_USER_HOME\n"
            + "zipStorePath=wrapper/dists\n";
        Files.write(new File(wrapperDir, "gradle-wrapper.properties").toPath(),
            props.getBytes(StandardCharsets.UTF_8));

        // Detect GraalVM Java for the wrapper
        String javaBin = "java";
        File gv = findGraalVm();
        if (gv != null) {
            File gvJava = new File(gv, "bin/java.exe");
            if (gvJava.exists()) javaBin = "\"" + gvJava.getAbsolutePath() + "\"";
        }

        String gradlewBat =
            "@rem Gradle wrapper startup script\n"
            + "@echo off\n"
            + "set DIRNAME=%~dp0\n"
            + "if \"%DIRNAME%\"==\"\" set DIRNAME=.\n"
            + "set APP_HOME=%DIRNAME%\n"
            + "set WRAPPER_JAR=%APP_HOME%gradle\\wrapper\\gradle-wrapper.jar\n"
            + "if not exist \"%WRAPPER_JAR%\" (\n"
            + "    echo Downloading Gradle wrapper...\n"
            + "    powershell -Command \"$wc = New-Object System.Net.WebClient; try { $wc.DownloadFile('https://services.gradle.org/distributions/gradle-8.4-wrapper.jar', '%WRAPPER_JAR%') } catch { }\" >nul 2>&1\n"
            + "    if not exist \"%WRAPPER_JAR%\" (\n"
            + "        powershell -Command \"$wc = New-Object System.Net.WebClient; try { $wc.DownloadFile('https://github.com/gradle/gradle/raw/v8.4.0/gradle/wrapper/gradle-wrapper.jar', '%WRAPPER_JAR%') } catch { }\" >nul 2>&1\n"
            + "    )\n"
            + "    if not exist \"%WRAPPER_JAR%\" (\n"
            + "        echo WARNING: Could not download Gradle wrapper.\n"
            + "        echo Download manually from: https://gradle.org/releases/\n"
            + "        echo Place gradle-wrapper.jar in: %WRAPPER_JAR%\n"
            + "        echo.\n"
            + "        echo Press any key after placing the file, or Ctrl+C to abort...\n"
            + "        pause >nul\n"
            + "    )\n"
            + ")\n"
            + "if exist \"%WRAPPER_JAR%\" (\n"
            + "    " + javaBin + " -classpath \"%WRAPPER_JAR%\" org.gradle.wrapper.GradleWrapperMain --no-daemon %*\n"
            + ") else (\n"
            + "    echo ERROR: gradle-wrapper.jar not found.\n"
            + "    echo Download it from https://gradle.org/releases/ and place in:\n"
            + "    echo %WRAPPER_JAR%\n"
            + "    exit /b 1\n"
            + ")\n";
        Files.write(new File(projectDir, "gradlew.bat").toPath(),
            gradlewBat.getBytes(StandardCharsets.UTF_8));

        // Also generate Linux gradlew shell script (for WSL)
        String gradlewSh =
            "#!/bin/bash\n"
            + "# Gradle wrapper for POSIX\n"
            + "APP_HOME=$(cd \"${0%[/\\\\]*}\" > /dev/null && pwd -P) || exit\n"
            + "CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar\n"
            + "exec \"$JAVA_HOME/bin/java\" -classpath \"$CLASSPATH\" org.gradle.wrapper.GradleWrapperMain \"$@\"\n";
        Files.write(new File(projectDir, "gradlew").toPath(),
            gradlewSh.getBytes(StandardCharsets.UTF_8));
    }

    private void generateGradleProject(File projectDir, String apkName,
                                        String apkPkg, String mainClass,
                                        File jarFile, File graalVm,
                                        String androidHome) throws IOException {
        // settings.gradle
        String settings =
            "pluginManagement {\n"
            + "    repositories {\n"
            + "        gradlePluginPortal()\n"
            + "        mavenCentral()\n"
            + "    }\n"
            + "}\n"
            + "rootProject.name = '" + apkName.replace("'", "\\'") + "'\n";
        Files.write(new File(projectDir, "settings.gradle").toPath(),
            settings.getBytes(StandardCharsets.UTF_8));

        // gradle.properties
        StringBuilder gradleProps = new StringBuilder();
        gradleProps.append("org.gradle.jvmargs=-Xmx4g\n")
            .append("org.gradle.daemon=false\n");
        if (androidHome != null && !androidHome.isEmpty()) {
            gradleProps.append("android.sdk.dir=").append(androidHome.replace("\\", "\\\\")).append("\n");
        }
        // Auto-detect Android NDK (check bundled android-ndk in tools/ first)
        String ndkHome = null;
        for (File base : getAppBaseDirs()) {
            File bundledNdk = new File(base, "tools\\android-ndk");
            if (new File(bundledNdk, "toolchains").exists()) {
                ndkHome = bundledNdk.getAbsolutePath();
                break;
            }
        }
        if (ndkHome == null && androidHome != null) {
            File ndkBundle = new File(androidHome, "ndk-bundle");
            if (ndkBundle.exists()) {
                ndkHome = ndkBundle.getAbsolutePath();
            } else {
                File ndkDir = new File(androidHome, "ndk");
                if (ndkDir.exists()) {
                    File[] ndkVersions = ndkDir.listFiles(File::isDirectory);
                    if (ndkVersions != null && ndkVersions.length > 0) {
                        ndkHome = ndkVersions[0].getAbsolutePath();
                    }
                }
            }
        }
        if (ndkHome != null) {
            gradleProps.append("android.ndk.dir=").append(ndkHome.replace("\\", "\\\\")).append("\n");
        }
        Files.write(new File(projectDir, "gradle.properties").toPath(),
            gradleProps.toString().getBytes(StandardCharsets.UTF_8));

        // libs dir + copy JAR
        new File(projectDir, "libs").mkdirs();
        Files.copy(jarFile.toPath(),
            new File(projectDir, "libs\\app.jar").toPath(),
            StandardCopyOption.REPLACE_EXISTING);

        // build.gradle
        String pluginVer = "1.0.23";
        //String pluginVer = "1.0.29";
        String buildGradle =
            "plugins {\n"
            + "    id 'application'\n"
            + "    id 'com.gluonhq.gluonfx-gradle-plugin' version '" + pluginVer + "'\n"
            + "}\n"
            + "application {\n"
            + "    mainClass = '" + mainClass + "'\n"
            + "}\n"
            + "gluonfx {\n"
            + "    target = 'android'\n"
            + "    graalvmHome = '" + graalVm.getAbsolutePath().replace("\\", "\\\\") + "'\n"
            + "    release {\n"
            + "        appLabel = '" + apkName.replace("'", "\\'") + "'\n"
            + "        versionCode = '1'\n"
            + "        versionName = '1.0'\n"
            + "    }\n"
            + "}\n"
            + "repositories {\n"
            + "    mavenCentral()\n"
            + "}\n"
            + "dependencies {\n"
            + "    implementation fileTree(dir: 'libs', include: ['*.jar'])\n"
            + "}\n";
        Files.write(new File(projectDir, "build.gradle").toPath(),
            buildGradle.getBytes(StandardCharsets.UTF_8));

        progressDialog.log("Gradle project written to: " + projectDir);
    }

    private File findGluonApk(File projectDir) {
        File buildDir = new File(projectDir, "build");
        if (!buildDir.exists()) return null;
        Set<File> apks = new HashSet<>();
        findFiles(buildDir, ".apk", apks);
        // Prefer aarch64 release APK
        for (File apk : apks) {
            String path = apk.getAbsolutePath().toLowerCase();
            if (path.contains("aarch64") || path.contains("arm64")) return apk;
        }
        if (!apks.isEmpty()) return apks.iterator().next();
        return null;
    }

    private void findFiles(File dir, String ext, Set<File> results) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) findFiles(f, ext, results);
            else if (f.getName().toLowerCase().endsWith(ext)) results.add(f);
        }
    }

    // --- Shared (HTML generation, utilities) ---

    private String generateMainHtml(String appName, String jarName, String mainClass,
                                     String implVersion, long totalSize,
                                     List<String> classes, List<String> fxmls,
                                     List<String> css, List<String> images,
                                     List<String> other) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"/>")
          .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0,maximum-scale=1.0\"/>")
          .append("<title>").append(escHtml(appName)).append("</title>")
          .append("<style>")
          .append("*{margin:0;padding:0;box-sizing:border-box;}")
          .append("body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;")
          .append("background:#1a1b22;color:#e8e9ed;padding:0;}")
          // Header
          .append(".header{background:linear-gradient(135deg,#6c5ce7,#a29bfe);padding:28px 20px 22px;text-align:center;}")
          .append(".header h1{font-size:22px;font-weight:700;margin:0 0 4px;}")
          .append(".header p{font-size:13px;opacity:.85;margin:0;}")
          .append(".header .badge{display:inline-block;background:rgba(255,255,255,.2);border-radius:20px;")
          .append("padding:4px 14px;font-size:11px;margin-top:8px;}")
          // Stats bar
          .append(".stats{display:flex;flex-wrap:wrap;gap:6px;padding:12px 16px;background:#262834;}")
          .append(".stat{flex:1;min-width:80px;text-align:center;padding:8px 4px;background:#2a2c38;border-radius:10px;}")
          .append(".stat .num{font-size:18px;font-weight:700;color:#6c5ce7;}")
          .append(".stat .lbl{font-size:10px;color:#9a9cad;margin-top:2px;}")
          // Section
          .append(".section{padding:14px 16px 4px;}")
          .append(".section h2{font-size:13px;font-weight:600;color:#6c5ce7;text-transform:uppercase;letter-spacing:1px;margin-bottom:8px;}")
          // File list
          .append(".file-list{display:grid;grid-template-columns:1fr;gap:4px;padding:0 16px 12px;}")
          // File card
          .append(".file{display:flex;align-items:center;gap:10px;background:#20212b;border:1px solid #33354450;")
          .append("border-radius:10px;padding:8px 12px;transition:background .15s;}")
          .append(".file:active{background:#32344a;}")
          .append(".file .icon{font-size:22px;width:28px;text-align:center;}")
          .append(".file .name{font-size:13px;font-weight:500;flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;}")
          .append(".file .size{font-size:10px;color:#6b6d80;white-space:nowrap;}")
          // Meta card
          .append(".meta{padding:0 16px 12px;}")
          .append(".meta-item{display:flex;justify-content:space-between;padding:6px 0;border-bottom:1px solid #33354430;}")
          .append(".meta-item:last-child{border:none;}")
          .append(".meta-item .k{font-size:12px;color:#9a9cad;}")
          .append(".meta-item .v{font-size:12px;font-weight:500;max-width:60%;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;}")
          // Footer
          .append(".footer{text-align:center;padding:16px;font-size:11px;color:#6b6d80;}")
          // Tab bar
          .append(".tabs{display:flex;gap:0;background:#262834;padding:0 8px;border-bottom:1px solid #33354450;}")
          .append(".tab{padding:10px 14px;font-size:12px;font-weight:600;color:#9a9cad;cursor:pointer;border-bottom:2px solid transparent;}")
          .append(".tab.active{color:#6c5ce7;border-bottom-color:#6c5ce7;}")
          .append(".tab-panel{display:none;}")
          .append(".tab-panel.active{display:block;}")
          // Icons
          .append(".ico-class{color:#c678dd;}.ico-fxml{color:#61afef;}.ico-css{color:#98c379;}.ico-img{color:#d19a66;}")
          .append(".ico-other{color:#abb2bf;}")
          // Search
          .append("#searchBox{width:calc(100% - 32px);margin:8px 16px;padding:8px 12px;border-radius:8px;")
          .append("background:#20212b;border:1px solid #33354450;color:#e8e9ed;font-size:13px;outline:none;}")
          .append("#searchBox:focus{border-color:#6c5ce7;}")
          .append("</style></head><body>")
          // Header
          .append("<div class=\"header\">")
          .append("<h1>").append(escHtml(appName)).append("</h1>")
          .append("<p>").append(escHtml(jarName)).append("</p>");
        if (!mainClass.isEmpty()) {
            sb.append("<div class=\"badge\">").append(escHtml(mainClass)).append("</div>");
        }
        sb.append("</div>");

        // Stats
        sb.append("<div class=\"stats\">")
          .append("<div class=\"stat\"><div class=\"num\">").append(classes.size()).append("</div><div class=\"lbl\">Classes</div></div>")
          .append("<div class=\"stat\"><div class=\"num\">").append(fxmls.size()).append("</div><div class=\"lbl\">Screens</div></div>")
          .append("<div class=\"stat\"><div class=\"num\">").append(images.size()).append("</div><div class=\"lbl\">Images</div></div>")
          .append("<div class=\"stat\"><div class=\"num\">").append(totalSizeString(totalSize)).append("</div><div class=\"lbl\">Total</div></div>")
          .append("</div>");

        // Tabs
        sb.append("<input id=\"searchBox\" type=\"text\" placeholder=\"Search files...\" oninput=\"filterFiles(this.value)\"/>");
        sb.append("<div class=\"tabs\" id=\"fileTabs\">");
        sb.append("<div class=\"tab active\" data-tab=\"all\" onclick=\"switchTab('all')\">All</div>");
        if (!fxmls.isEmpty()) sb.append("<div class=\"tab\" data-tab=\"fxml\" onclick=\"switchTab('fxml')\">FXML</div>");
        if (!css.isEmpty()) sb.append("<div class=\"tab\" data-tab=\"css\" onclick=\"switchTab('css')\">CSS</div>");
        if (!images.isEmpty()) sb.append("<div class=\"tab\" data-tab=\"images\" onclick=\"switchTab('images')\">Images</div>");
        if (!classes.isEmpty()) sb.append("<div class=\"tab\" data-tab=\"classes\" onclick=\"switchTab('classes')\">Classes</div>");
        if (!other.isEmpty()) sb.append("<div class=\"tab\" data-tab=\"other\" onclick=\"switchTab('other')\">Other</div>");
        sb.append("</div>");

        // All files panel
        sb.append("<div id=\"panel-all\" class=\"tab-panel active\">").append(buildFileList(classes, fxmls, css, images, other, "all")).append("</div>");
        if (!fxmls.isEmpty()) sb.append("<div id=\"panel-fxml\" class=\"tab-panel\">").append(buildFileList(classes, fxmls, css, images, other, "fxml")).append("</div>");
        if (!css.isEmpty()) sb.append("<div id=\"panel-css\" class=\"tab-panel\">").append(buildFileList(classes, fxmls, css, images, other, "css")).append("</div>");
        if (!images.isEmpty()) sb.append("<div id=\"panel-images\" class=\"tab-panel\">").append(buildFileList(classes, fxmls, css, images, other, "images")).append("</div>");
        if (!classes.isEmpty()) sb.append("<div id=\"panel-classes\" class=\"tab-panel\">").append(buildFileList(classes, fxmls, css, images, other, "classes")).append("</div>");
        if (!other.isEmpty()) sb.append("<div id=\"panel-other\" class=\"tab-panel\">").append(buildFileList(classes, fxmls, css, images, other, "other")).append("</div>");

        // Metadata
        sb.append("<div class=\"section\"><h2>App Info</h2></div>")
          .append("<div class=\"meta\">")
          .append("<div class=\"meta-item\"><span class=\"k\">Name</span><span class=\"v\">").append(escHtml(appName)).append("</span></div>")
          .append("<div class=\"meta-item\"><span class=\"k\">JAR</span><span class=\"v\">").append(escHtml(jarName)).append("</span></div>");
        if (!mainClass.isEmpty()) sb.append("<div class=\"meta-item\"><span class=\"k\">Main Class</span><span class=\"v\">").append(escHtml(mainClass)).append("</span></div>");
        if (!implVersion.isEmpty()) sb.append("<div class=\"meta-item\"><span class=\"k\">Version</span><span class=\"v\">").append(escHtml(implVersion)).append("</span></div>");
        sb.append("<div class=\"meta-item\"><span class=\"k\">Total Files</span><span class=\"v\">").append(classes.size() + fxmls.size() + css.size() + images.size() + other.size()).append("</span></div>")
          .append("</div>");

        // Footer
        sb.append("<div class=\"footer\">Generated by WebIDE \u2022 JavaFX APK Converter</div>");

        // JavaScript
        sb.append("<script>")
          .append("var currentTab='all';")
          .append("function switchTab(t){currentTab=t;")
          .append("document.querySelectorAll('.tab-panel').forEach(function(p){p.classList.remove('active');});")
          .append("var panel=document.getElementById('panel-'+t);if(panel)panel.classList.add('active');")
          .append("document.querySelectorAll('.tab').forEach(function(tb){tb.classList.remove('active');});")
          .append("var tb=document.querySelector('.tab[data-tab=\"'+t+'\"]');if(tb)tb.classList.add('active');")
          .append("filterFiles(document.getElementById('searchBox').value);}")
          .append("function filterFiles(q){q=q.toLowerCase();")
          .append("document.querySelectorAll('#panel-'+currentTab+' .file').forEach(function(f){")
          .append("f.style.display=f.querySelector('.name').textContent.toLowerCase().indexOf(q)>=0?'flex':'none';});}")
          .append("</script>")
          .append("</body></html>");
        return sb.toString();
    }

    private String buildFileList(List<String> classes, List<String> fxmls,
                                  List<String> css, List<String> images,
                                  List<String> other, String type) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"file-list\" id=\"list-").append(type).append("\">");

        List<FileEntry> entries = new ArrayList<>();
        if (type.equals("all") || type.equals("classes")) {
            for (String f : classes) entries.add(new FileEntry(f, "class"));
        }
        if (type.equals("all") || type.equals("fxml")) {
            for (String f : fxmls) entries.add(new FileEntry(f, "fxml"));
        }
        if (type.equals("all") || type.equals("css")) {
            for (String f : css) entries.add(new FileEntry(f, "css"));
        }
        if (type.equals("all") || type.equals("images")) {
            for (String f : images) entries.add(new FileEntry(f, "img"));
        }
        if (type.equals("all") || type.equals("other")) {
            for (String f : other) entries.add(new FileEntry(f, "other"));
        }

        // Sort by name
        Collections.sort(entries, (a, b) -> a.name.compareToIgnoreCase(b.name));

        for (FileEntry fe : entries) {
            String icon;
            String iconClass;
            switch (fe.type) {
                case "class": icon = "[C]"; iconClass = "ico-class"; break;
                case "fxml":  icon = "[F]"; iconClass = "ico-fxml"; break;
                case "css":   icon = "[S]"; iconClass = "ico-css"; break;
                case "img":   icon = "[I]"; iconClass = "ico-img"; break;
                default:      icon = "[O]"; iconClass = "ico-other"; break;
            }

            String fileName = fe.name;
            if (fileName.contains("/")) fileName = fileName.substring(fileName.lastIndexOf('/') + 1);

            sb.append("<div class=\"file\" data-ext=\"").append(fe.type).append("\">")
              .append("<div class=\"icon ").append(iconClass).append("\">").append(icon).append("</div>")
              .append("<div class=\"name\">").append(escHtml(fileName)).append("</div>")
              .append("<div class=\"size\">").append(escHtml(fe.name)).append("</div>")
              .append("</div>");
        }

        if (entries.isEmpty()) {
            sb.append("<div style=\"text-align:center;padding:20px;color:#6b6d80;font-size:13px;\">No files in this category</div>");
        }

        sb.append("</div>");
        return sb.toString();
    }

    private static class FileEntry {
        String name, type;
        FileEntry(String n, String t) { name = n; type = t; }
    }

    private static String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String totalSizeString(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        super.initialize(location, resources);
        packagename.setText("javafxapp");

        // Mode toggle
        gluonMode.setSelected(true);
        buildMode = BuildMode.GLUON;
        modeGroup.selectedToggleProperty().addListener((obs, old, nw) -> {
            buildMode = (nw == gluonMode) ? BuildMode.GLUON : BuildMode.WEBVIEW;
            updateModeInfo();
        });
        updateModeInfo();

        // Auto-detect GraalVM
        File gv = findGraalVm();
        if (gv != null && new File(gv, "bin/java.exe").exists()) {
            graalVmField.setText(gv.getAbsolutePath());
        } else {
            graalVmField.setText("");
        }
    }

    private void updateModeInfo() {
        if (buildMode == BuildMode.GLUON) {
            modeInfoLabel.setText("Gluon Native compiles JavaFX directly for Android using GraalVM.\n"
                + "Produces a real native Android app with your JavaFX UI.\n"
                + "Requires GraalVM JDK 11+ and an internet connection.");
        } else {
            modeInfoLabel.setText("WebView Shell creates a basic APK that shows a file explorer.\n"
                + "The JavaFX app is bundled but NOT actually executed.\n"
                + "Use this as a fallback if GraalVM is not available.");
        }
    }

    // --- WSL (Windows Subsystem for Linux) support ---

    private boolean isWslAvailable() {
        try {
            Process p = new ProcessBuilder("wsl.exe", "--status")
                .redirectErrorStream(true).start();
            return p.waitFor(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            return false;
        }
    }

    private String getWslDistro() {
        if (!isWslAvailable()) return null;
        try {
            Process p = new ProcessBuilder("wsl.exe", "-l", "-q")
                .redirectErrorStream(true).start();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.toLowerCase().contains("windows subsystem")
                        && !line.contains("�")) {
                        return line;
                    }
                }
            }
            p.waitFor();
        } catch (Exception e) {}
        return null;
    }

    private String windowsToWslPath(String winPath) {
        if (winPath == null || winPath.isEmpty()) return "";
        String n = winPath.replace("\\", "/");
        if (n.length() >= 2 && n.charAt(1) == ':') {
            char drive = Character.toLowerCase(n.charAt(0));
            n = "/mnt/" + drive + n.substring(2);
        }
        return n;
    }

    private void rewritePathsForWsl(File projectDir, Map<String, String> env) throws IOException {
        File gradleProps = new File(projectDir, "gradle.properties");
        if (gradleProps.exists()) {
            String content = new String(Files.readAllBytes(gradleProps.toPath()), StandardCharsets.UTF_8);
            String androidHome = env.get("ANDROID_HOME");
            if (androidHome != null && !androidHome.isEmpty()) {
                content = content.replaceAll("(?m)^android\\.sdk\\.dir=.*$",
                    "android.sdk.dir=" + windowsToWslPath(androidHome));
            }
            String ndkHome = env.get("ANDROID_NDK_HOME");
            if (ndkHome != null && !ndkHome.isEmpty()) {
                content = content.replaceAll("(?m)^android\\.ndk\\.dir=.*$",
                    "android.ndk.dir=" + windowsToWslPath(ndkHome));
            }
            Files.write(gradleProps.toPath(), content.getBytes(StandardCharsets.UTF_8));
        }
        File buildGradle = new File(projectDir, "build.gradle");
        if (buildGradle.exists()) {
            String content = new String(Files.readAllBytes(buildGradle.toPath()), StandardCharsets.UTF_8);
            String graalHome = env.get("GRAALVM_HOME");
            if (graalHome != null && !graalHome.isEmpty()) {
                content = content.replaceAll("(?m)^\\s*graalvmHome\\s*=.*$",
                    "    graalvmHome = '" + windowsToWslPath(graalHome) + "'");
            }
            Files.write(buildGradle.toPath(), content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void runViaWsl(String distro, String gradleTask, File projectDir,
                            Map<String, String> windowsEnv) throws Exception {
        rewritePathsForWsl(projectDir, windowsEnv);

        String wslDir = windowsToWslPath(projectDir.getAbsolutePath());
        String javaHome = windowsToWslPath(windowsEnv.getOrDefault("JAVA_HOME", ""));
        String graalHome = windowsToWslPath(windowsEnv.getOrDefault("GRAALVM_HOME", ""));
        String androidHome = windowsToWslPath(windowsEnv.getOrDefault("ANDROID_HOME", ""));
        String ndkHome = windowsToWslPath(windowsEnv.getOrDefault("ANDROID_NDK_HOME", ""));

        StringBuilder script = new StringBuilder();
        script.append("#!/bin/bash\nset -e\n");
        script.append("cd '").append(wslDir).append("'\n");
        if (!javaHome.isEmpty()) script.append("export JAVA_HOME='").append(javaHome).append("'\n");
        if (!graalHome.isEmpty()) script.append("export GRAALVM_HOME='").append(graalHome).append("'\n");
        if (!androidHome.isEmpty()) script.append("export ANDROID_HOME='").append(androidHome).append("'\n");
        if (!ndkHome.isEmpty()) script.append("export ANDROID_NDK_HOME='").append(ndkHome).append("'\n");
        if (!javaHome.isEmpty()) script.append("export PATH=\"$JAVA_HOME/bin:$PATH\"\n");
        script.append("chmod +x gradlew\n");
        script.append("./gradlew ").append(gradleTask).append("\n");

        File scriptFile = new File(projectDir, "_wsl_build.sh");
        Files.write(scriptFile.toPath(), script.toString().getBytes(StandardCharsets.UTF_8));

        progressDialog.log("> Building via WSL (" + distro + ")...");

        ProcessBuilder pb = new ProcessBuilder(
            "wsl.exe", "-d", distro, "--", "bash", "-c",
            "cd '" + wslDir + "' && bash _wsl_build.sh"
        );
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
            throw new IOException("WSL build failed (exit code " + exitCode + ")");
        }
        progressDialog.logSuccess("WSL build completed");
    }
}
