package com.eagle.controller;

import com.jfoenix.controls.JFXTextField;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class WebsiteToApkController extends AbstractApkBuilder {

    @FXML private JFXTextField urlfield;

    @Override protected String getHeaderTitle() { return "Website to APK"; }
    @Override protected String getHeaderSubtitle() { return "Wrap any website into a native Android application"; }
    @Override protected String getSourcePrompt() { return null; }
    @Override protected String getExtraPrompt() { return null; }
    @Override protected String getSourceExtension() { return null; }
    @Override protected String getExtraExtension() { return null; }
    @Override protected int getTaskCount() { return 10; }
    @Override protected String getSourceInput() { return urlfield.getText().trim(); }
    @Override protected String getExtraInput() { return null; }

    @Override protected void onBrowseSource(File f) {}
    @Override protected void onBrowseExtra(File f) {}

    @Override protected boolean validateInputs(String apkName, String apkPkg, String apkIcon, String sourceInput, String extraInput) {
        if (sourceInput.isEmpty()) {
            showAlert("Missing URL", "Please enter a website URL.");
            return false;
        }
        if (!sourceInput.startsWith("http://") && !sourceInput.startsWith("https://")) {
            showAlert("Invalid URL", "URL must start with http:// or https://");
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

    @Override protected void generateJavaSource(String pkgPath, StringBuilder java) {
        String url = urlfield.getText().trim();
        java.append("import android.app.Activity;\n")
            .append("import android.os.Bundle;\n")
            .append("import android.webkit.WebView;\n")
            .append("import android.webkit.WebSettings;\n")
            .append("import android.webkit.WebViewClient;\n")
            .append("public class MainActivity extends Activity {\n")
            .append("    private WebView wv;\n")
            .append("    @Override\n")
            .append("    protected void onCreate(Bundle savedInstanceState) {\n")
            .append("        super.onCreate(savedInstanceState);\n")
            .append("        wv = new WebView(this);\n")
            .append("        WebSettings s = wv.getSettings();\n")
            .append("        s.setJavaScriptEnabled(true);\n")
            .append("        s.setDomStorageEnabled(true);\n")
            .append("        s.setLoadWithOverviewMode(true);\n")
            .append("        s.setUseWideViewPort(true);\n")
            .append("        s.setBuiltInZoomControls(true);\n")
            .append("        s.setDisplayZoomControls(false);\n")
            .append("        wv.setWebViewClient(new WebViewClient());\n")
            .append("        wv.loadUrl(\"").append(url).append("\");\n")
            .append("        setContentView(wv);\n")
            .append("    }\n")
            .append("    @Override\n")
            .append("    public void onBackPressed() {\n")
            .append("        if (wv.canGoBack()) wv.goBack();\n")
            .append("        else super.onBackPressed();\n")
            .append("    }\n")
            .append("}");
    }

    @Override protected void copyAssets(File workDir, String sourceInput, String extraInput) throws Exception {
        String splash = "<!DOCTYPE html><html><head><meta charset=\"utf-8\">"
            + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
            + "<style>*{margin:0;padding:0}body{background:#6c5ce7;display:flex;align-items:center;justify-content:center;height:100vh;color:#fff;font-family:sans-serif;font-size:18px}</style>"
            + "</head><body><div>Loading...</div></body></html>";
        Files.write(Paths.get(workDir + "\\assets\\_main_.html"), splash.getBytes(StandardCharsets.UTF_8));
    }

    @Override public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        super.initialize(location, resources);
        urlfield.textProperty().addListener((obs, old, val) -> {
            if (val != null && !val.trim().isEmpty()) create.setDisable(false);
            else create.setDisable(true);
        });
    }
}
