package com.eagle.controller;

import com.jfoenix.controls.JFXTextField;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.DirectoryChooser;

public class WebAppToApkController extends AbstractApkBuilder {

    @FXML private JFXTextField projectFolder;

    @Override protected String getHeaderTitle() { return "Web App to Android APK"; }
    @Override protected String getHeaderSubtitle() { return "Build React, Vue, Angular & any web project into a native Android app"; }
    @Override protected String getSourcePrompt() { return "Select Project Folder"; }
    @Override protected String getExtraPrompt() { return null; }
    @Override protected String getSourceExtension() { return null; }
    @Override protected String getExtraExtension() { return null; }
    @Override protected int getTaskCount() { return 10; }
    @Override protected String getSourceInput() { return projectFolder.getText().trim(); }
    @Override protected String getExtraInput() { return null; }

    @FXML
    void browse1act(ActionEvent event) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select Web Project Folder");
        File f = dc.showDialog(projectFolder.getScene().getWindow());
        if (f != null) onBrowseSource(f);
    }

    @Override protected void onBrowseSource(File f) {
        projectFolder.setText(f.getAbsolutePath());
        if (apkname.getText().isEmpty()) {
            apkname.setText(f.getName());
        }
        create.setDisable(false);
    }

    public void preselectProjectFolder(File f) {
        onBrowseSource(f);
    }

    @Override protected void onBrowseExtra(File f) {}

    @Override protected boolean validateInputs(String apkName, String apkPkg, String apkIcon, String sourceInput, String extraInput) {
        if (sourceInput.isEmpty()) {
            showAlert("Missing folder", "Please select a web project folder.");
            return false;
        }
        File projectDir = new File(sourceInput);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            showAlert("Invalid folder", "Selected path is not a valid directory.");
            return false;
        }
        File pkgJson = new File(projectDir, "package.json");
        if (!pkgJson.exists()) {
            showAlert("No package.json", "The selected folder does not contain a package.json file.\n\nThis tool is for Node.js web projects (React, Vue, Angular, etc.).");
            return false;
        }
        return true;
    }

    @Override protected void generateManifest(String pkgPath, StringBuilder xml) {
        xml.append("<uses-permission android:name=\"android.permission.INTERNET\"/>")
            .append("<uses-permission android:name=\"android.permission.ACCESS_NETWORK_STATE\"/>")
            .append("<application android:label=\"").append(apkname.getText().trim()).append("\"")
            .append(" android:icon=\"@drawable/icon\" android:allowBackup=\"true\"")
            .append(" android:hardwareAccelerated=\"true\" android:usesCleartextTraffic=\"true\">")
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
            .append("import android.webkit.WebResourceRequest;\n")
            .append("import android.webkit.WebResourceResponse;\n")
            .append("import java.io.InputStream;\n")
            .append("import java.io.ByteArrayOutputStream;\n")
            .append("import java.io.ByteArrayInputStream;\n")
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
            .append("        wv.setWebViewClient(new WebViewClient() {\n")
            .append("            @Override\n")
            .append("            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest req) {\n")
            .append("                String url = req.getUrl().getPath();\n")
            .append("                if (url.startsWith(\"/\")) url = url.substring(1);\n")
            .append("                try {\n")
            .append("                    String mime = \"text/html\";\n")
            .append("                    if (url.endsWith(\".css\")) mime = \"text/css\";\n")
            .append("                    else if (url.endsWith(\".js\")) mime = \"text/javascript\";\n")
            .append("                    else if (url.endsWith(\".png\")) mime = \"image/png\";\n")
            .append("                    else if (url.endsWith(\".jpg\")||url.endsWith(\".jpeg\")) mime = \"image/jpeg\";\n")
            .append("                    else if (url.endsWith(\".svg\")) mime = \"image/svg+xml\";\n")
            .append("                    else if (url.endsWith(\".json\")) mime = \"application/json\";\n")
            .append("                    else if (url.endsWith(\".woff2\")) mime = \"font/woff2\";\n")
            .append("                    else if (url.endsWith(\".woff\")) mime = \"font/woff\";\n")
            .append("                    else if (url.endsWith(\".ttf\")) mime = \"font/ttf\";\n")
            .append("                    else if (url.endsWith(\".ico\")) mime = \"image/x-icon\";\n")
            .append("                    InputStream in = getAssets().open(url);\n")
            .append("                    ByteArrayOutputStream buf = new ByteArrayOutputStream();\n")
            .append("                    byte[] tmp = new byte[4096]; int n;\n")
            .append("                    while ((n = in.read(tmp)) != -1) buf.write(tmp, 0, n);\n")
            .append("                    in.close();\n")
            .append("                    return new WebResourceResponse(mime, \"UTF-8\", new ByteArrayInputStream(buf.toByteArray()));\n")
            .append("                } catch (Exception e) {\n")
            .append("                    try {\n")
            .append("                        InputStream in = getAssets().open(\"index.html\");\n")
            .append("                        ByteArrayOutputStream buf = new ByteArrayOutputStream();\n")
            .append("                        byte[] tmp = new byte[4096]; int n;\n")
            .append("                        while ((n = in.read(tmp)) != -1) buf.write(tmp, 0, n);\n")
            .append("                        in.close();\n")
            .append("                        return new WebResourceResponse(\"text/html\", \"UTF-8\", new ByteArrayInputStream(buf.toByteArray()));\n")
            .append("                    } catch (Exception e2) { return null; }\n")
            .append("                }\n")
            .append("            }\n")
            .append("        });\n")
            .append("        wv.loadUrl(\"file:///android_asset/index.html\");\n")
            .append("        setContentView(wv);\n")
            .append("    }\n")
            .append("}");
    }

    @Override
    protected void copyAssets(File workDir, String sourceInput, String extraInput) throws Exception {
        File projectDir = new File(sourceInput);

        File pkgJson = new File(projectDir, "package.json");
        if (!pkgJson.exists()) {
            throw new IOException("Selected folder does not contain package.json");
        }

        File outputDir = findBuildOutput(projectDir);

        if (outputDir == null || !new File(outputDir, "index.html").exists()) {
            if (outputDir != null) {
                deleteDirectory(outputDir);
            }
            outputDir = null;

            // Ensure node_modules/.bin exists (missing .bin symlinks = build will fail)
            File nodeModules = new File(projectDir, "node_modules");
            File binDir = new File(nodeModules, ".bin");
            boolean needsInstall = false;
            if (!nodeModules.exists()) {
                needsInstall = true;
            } else if (!binDir.exists() || binDir.list().length == 0) {
                progressDialog.log(".bin directory missing — will reinstall");
                needsInstall = true;
            }
            // Also check if the actual build binary exists
            if (!needsInstall && nodeModules.exists()) {
                String[] buildBins = {"vue-cli-service", "react-scripts", "ng", "next", "nuxt", "svelte-kit"};
                boolean foundAny = false;
                for (String name : buildBins) {
                    if (new File(binDir, name + ".cmd").exists()
                        || new File(binDir, name).exists()) {
                        foundAny = true; break;
                    }
                }
                if (!foundAny) {
                    progressDialog.log("No framework binary found — will install dependencies");
                    needsInstall = true;
                }
            }

            if (needsInstall) {
                progressDialog.log("Installing npm dependencies...");
                try {
                    runProcessInDir("npm install", 300_000, projectDir);
                    progressDialog.logSuccess("npm install complete");
                } catch (Exception e) {
                    progressDialog.log("npm install had issues: " + e.getMessage());
                }
            } else {
                progressDialog.log("node_modules ready, skipping npm install");
            }

            ensureNodeSassCompatible(projectDir);

            progressDialog.log("Running npm run build...");
            java.util.Map<String, String> env = new java.util.HashMap<>();
            env.put("NODE_OPTIONS", "--openssl-legacy-provider");
            env.put("SASS_BINARY_SITE", "https://github.com/sass/node-sass/releases/download");

            String buildCmd = readBuildCommand(new File(projectDir, "package.json"));
            // On Windows, Unix export syntax ("export VAR=value && cmd") fails.
            // Since we already set those env vars, strip the export prefix and run directly.
            if (buildCmd != null && !buildCmd.isEmpty()
                && buildCmd.matches("(?i)^export\\s+\\w+=[^&]+&&\\s*.*")) {
                String stripped = buildCmd.replaceAll("(?i)^export\\s+\\w+=[^&]+&&\\s*", "").trim();
                progressDialog.log("Using direct build command (export stripped): " + stripped);
                try {
                    runProcessInDir(stripped, 300_000, projectDir, env);
                    progressDialog.logSuccess("npm build complete");
                    buildCmd = null; // mark as done
                } catch (Exception e) {
                    progressDialog.log("npm run build failed, trying npx: " + stripped);
                }
            }
            if (buildCmd != null) {
                try {
                    runProcessInDir("npm run build", 300_000, projectDir, env);
                    progressDialog.logSuccess("npm build complete");
                    buildCmd = null;
                } catch (Exception e) {
                    // Build failed — try reading the build script from package.json
                    // and run via npx which properly resolves local .bin
                    buildCmd = readBuildCommand(new File(projectDir, "package.json"));
                    // Strip Unix export syntax in npx fallback too
                    if (buildCmd != null) {
                        buildCmd = buildCmd.replaceAll("(?i)^export\\s+\\w+=[^&]+&&\\s*", "").trim();
                    }
                    if (buildCmd != null && !buildCmd.isEmpty()) {
                    progressDialog.log("npm run build failed, trying npx: " + buildCmd);
                    try {
                        runProcessInDir("npx --yes " + buildCmd, 300_000, projectDir, env);
                        progressDialog.logSuccess("npx build complete");
                    } catch (Exception e2) {
                        throw new IOException("Build still fails.\n"
                            + "The project's build script: " + buildCmd + "\n"
                            + "Try running 'npm install && npm run build' manually.\n"
                            + "Error: " + e2.getMessage());
                    }
                } else {
                    throw new IOException("Build failed.\n"
                        + "No build script found in package.json.\n"
                        + "Error: " + e.getMessage());
                }
            }

            }

            outputDir = findBuildOutput(projectDir);
        }

        if (!outputDir.exists() || !outputDir.isDirectory()) {
            throw new IOException("Build output folder not found.\n"
                + "Checked for: build/, dist/, out/, _site/, output/\n"
                + "Please ensure 'npm run build' succeeds in the project folder.");
        }

        File assetsDir = new File(workDir, "assets");
        copyDirectory(outputDir, assetsDir);

        File indexHtml = new File(assetsDir, "index.html");
        if (!indexHtml.exists()) {
            throw new IOException("No index.html found in build output.\n"
                + "Build output was found at: " + outputDir.getAbsolutePath());
        }

        progressDialog.logSuccess("Built project assets copied to APK");
    }

    private File findBuildOutput(File projectDir) {
        String[] candidates = {"build", "dist", "out", "_site", "output", ".next", ".nuxt", ".svelte-kit"};
        for (String c : candidates) {
            File f = new File(projectDir, c);
            if (f.exists() && f.isDirectory()) return f;
        }
        return null;
    }

    @Override
    protected void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDirectory(f);
                else f.delete();
            }
        }
        dir.delete();
    }

    private void ensureNodeSassCompatible(File projectDir) {
        File nodeSassDir = new File(new File(projectDir, "node_modules"), "node-sass");
        if (!nodeSassDir.isDirectory()) return;

        try {
            ProcessBuilder pb = new ProcessBuilder("node", "-e",
                "try{require('node-sass');process.exit(0)}catch(e){process.exit(1)}");
            pb.directory(projectDir);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            if (p.waitFor(15, TimeUnit.SECONDS) && p.exitValue() == 0) {
                progressDialog.log("node-sass OK");
                return;
            }
        } catch (Exception e) {
            progressDialog.log("node-sass check failed: " + e.getMessage());
        }

        progressDialog.log("node-sass incompatible — replacing with sass...");
        try {
            runProcessInDir("npm uninstall node-sass --no-save", 120_000, projectDir);
        } catch (Exception e) {
            progressDialog.log("uninstall node-sass note: " + e.getMessage());
        }
        try {
            runProcessInDir("npm install sass --no-save", 120_000, projectDir);
            progressDialog.log("sass installed");
        } catch (Exception e) {
            progressDialog.log("Could not install sass: " + e.getMessage());
        }
    }

    private String readBuildCommand(File pkgJson) {
        if (pkgJson == null || !pkgJson.exists()) return null;
        try {
            String content = new String(java.nio.file.Files.readAllBytes(pkgJson.toPath()),
                java.nio.charset.StandardCharsets.UTF_8);
            // Look for "build": "..." in scripts
            int idx = content.indexOf("\"build\"");
            if (idx < 0) return null;
            idx = content.indexOf(':', idx);
            if (idx < 0) return null;
            int start = content.indexOf('"', idx + 1);
            if (start < 0) return null;
            int end = start + 1;
            while (end < content.length() && content.charAt(end) != '"') {
                if (content.charAt(end) == '\\') end++;
                end++;
            }
            return content.substring(start + 1, end);
        } catch (Exception e) {
            return null;
        }
    }

    private void copyDirectory(File src, File dest) throws IOException {
        File[] files = src.listFiles();
        if (files == null) return;
        dest.mkdirs();
        for (File f : files) {
            File target = new File(dest, f.getName());
            if (f.isDirectory()) {
                copyDirectory(f, target);
            } else {
                Files.copy(f.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
