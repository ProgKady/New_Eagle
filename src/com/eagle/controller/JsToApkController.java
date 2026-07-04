package com.eagle.controller;

import com.jfoenix.controls.JFXTextField;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.FileChooser;

public class JsToApkController extends AbstractApkBuilder {

    @FXML private JFXTextField jsfile;

    @Override protected String getHeaderTitle() { return "JS to Android APK"; }
    @Override protected String getHeaderSubtitle() { return "Convert your JavaScript code into a native Android app"; }
    @Override protected String getSourcePrompt() { return "Select JS File"; }
    @Override protected String getExtraPrompt() { return null; }
    @Override protected String getSourceExtension() { return "*.js"; }
    @Override protected String getExtraExtension() { return null; }
    @Override protected int getTaskCount() { return 10; }
    @Override protected String getSourceInput() { return jsfile.getText().trim(); }
    @Override protected String getExtraInput() { return null; }

    private String loadResource(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) return "";
            Scanner s = new Scanner(is, "UTF-8").useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        } catch (Exception e) { return ""; }
    }

    @FXML void browse1act(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JavaScript Files", "*.js"));
        fc.setTitle("Select JavaScript File");
        File f = fc.showOpenDialog(jsfile.getScene().getWindow());
        if (f != null) onBrowseSource(f);
    }

    @Override protected void onBrowseSource(File f) {
        jsfile.setText(f.getAbsolutePath());
        create.setDisable(false);
    }

    public void preselectFile(File f) {
        onBrowseSource(f);
    }

    @Override protected void onBrowseExtra(File f) {}

    @Override protected boolean validateInputs(String apkName, String apkPkg, String apkIcon, String sourceInput, String extraInput) {
        if (sourceInput.isEmpty()) {
            showAlert("Missing file", "Please select a JavaScript file.");
            return false;
        }
        return true;
    }

    @Override protected void generateManifest(String pkgPath, StringBuilder xml) {
        xml.append("<uses-permission android:name=\"android.permission.INTERNET\"/>")
            .append("<uses-permission android:name=\"android.permission.ACCESS_NETWORK_STATE\"/>")
            .append("<uses-permission android:name=\"android.permission.VIBRATE\"/>")
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
        java.append("import android.app.Activity;\n")
            .append("import android.content.Context;\n")
            .append("import android.content.res.AssetManager;\n")
            .append("import android.os.Build;\n")
            .append("import android.os.Bundle;\n")
            .append("import android.os.Handler;\n")
            .append("import android.os.VibrationEffect;\n")
            .append("import android.os.Vibrator;\n")
            .append("import android.webkit.JavascriptInterface;\n")
            .append("import android.webkit.JsPromptResult;\n")
            .append("import android.webkit.WebChromeClient;\n")
            .append("import android.webkit.WebResourceRequest;\n")
            .append("import android.webkit.WebResourceResponse;\n")
            .append("import android.webkit.WebView;\n")
            .append("import android.webkit.WebSettings;\n")
            .append("import android.webkit.WebViewClient;\n")
            .append("import java.io.ByteArrayOutputStream;\n")
            .append("import java.io.InputStream;\n")
            .append("public class MainActivity extends Activity {\n")
            .append("    private WebView _wv;\n")
            .append("    private Handler _h = new Handler();\n")
            .append("    @Override\n")
            .append("    protected void onCreate(Bundle savedInstanceState) {\n")
            .append("        super.onCreate(savedInstanceState);\n")
            .append("        _wv = new WebView(this);\n")
            .append("        WebSettings s = _wv.getSettings();\n")
            .append("        s.setJavaScriptEnabled(true);\n")
            .append("        s.setAllowFileAccess(true);\n")
            .append("        s.setAllowFileAccessFromFileURLs(true);\n")
            .append("        s.setAllowUniversalAccessFromFileURLs(true);\n")
            .append("        s.setAllowContentAccess(true);\n")
            .append("        s.setDomStorageEnabled(true);\n")
            .append("        s.setLoadWithOverviewMode(true);\n")
            .append("        s.setUseWideViewPort(true);\n")
            .append("        _wv.addJavascriptInterface(new DsBridge(), \"_native\");\n")
            .append("        _wv.setWebChromeClient(new WebChromeClient() {\n")
            .append("            public boolean onJsPrompt(WebView v, String url, String msg, String def, JsPromptResult r) {\n")
            .append("                String ret = handlePrompt(msg, def);\n")
            .append("                if (ret != null) { r.confirm(ret); return true; }\n")
            .append("                return super.onJsPrompt(v, url, msg, def, r);\n")
            .append("            }\n")
            .append("        });\n")
            .append("        _wv.setWebViewClient(new WebViewClient() {\n")
            .append("            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest req) {\n")
            .append("                String url = req.getUrl().toString();\n")
            .append("                if (url.startsWith(\"ds:\")) {\n")
            .append("                    String path = url.substring(3);\n")
            .append("                    if (path.startsWith(\"/\")) path = path.substring(1);\n")
            .append("                    try {\n")
            .append("                        String mime = \"text/javascript\";\n")
            .append("                        if (path.endsWith(\".css\")) mime = \"text/css\";\n")
            .append("                        else if (path.endsWith(\".html\")) mime = \"text/html\";\n")
            .append("                        else if (path.endsWith(\".png\")) mime = \"image/png\";\n")
            .append("                        else if (path.endsWith(\".jpg\")||path.endsWith(\".jpeg\")) mime = \"image/jpeg\";\n")
            .append("                        InputStream in = getAssets().open(path);\n")
            .append("                        ByteArrayOutputStream buf = new ByteArrayOutputStream();\n")
            .append("                        byte[] tmp = new byte[4096]; int n;\n")
            .append("                        while ((n = in.read(tmp)) != -1) buf.write(tmp, 0, n);\n")
            .append("                        in.close();\n")
            .append("                        return new WebResourceResponse(mime, \"UTF-8\", new java.io.ByteArrayInputStream(buf.toByteArray()));\n")
            .append("                    } catch (Exception e) { return null; }\n")
            .append("                }\n")
            .append("                return null;\n")
            .append("            }\n")
            .append("        });\n")
            .append("        _wv.loadUrl(\"file:///android_asset/_main_.html\");\n")
            .append("        setContentView(_wv);\n")
            .append("    }\n")
            .append("    private String handlePrompt(String id, String cmd) {\n")
            .append("        if (cmd == null || cmd.isEmpty()) return null;\n")
            .append("        if (cmd.equals(\"_Start\")) return \"true\";\n")
            .append("        if (cmd.equals(\"_DoEvents\")) return \"\";\n")
            .append("        int paren = cmd.indexOf('(');\n")
            .append("        if (paren < 0) return null;\n")
            .append("        String fullMethod = cmd.substring(0, paren);\n")
            .append("        String args = cmd.substring(paren + 1);\n")
            .append("        if (args.endsWith(\")\")) args = args.substring(0, args.length() - 1);\n")
            .append("        String[] parts = args.split(\"\\f\", -1);\n")
            .append("        int dot = fullMethod.indexOf('.');\n")
            .append("        if (dot < 0) return null;\n")
            .append("        String type = fullMethod.substring(0, dot);\n")
            .append("        String method = fullMethod.substring(dot + 1);\n")
            .append("        if (!type.equals(\"App\")) return null;\n")
            .append("        return handleAppMethod(method, parts);\n")
            .append("    }\n")
            .append("    private String getArg(String[] a, int i) {\n")
            .append("        int off = (a.length > 1 && a[0].isEmpty()) ? 1 : 0;\n")
            .append("        int idx = i + off;\n")
            .append("        return (idx >= 0 && idx < a.length) ? a[idx] : \"\";\n")
            .append("    }\n")
            .append("    private String handleAppMethod(String method, String[] args) {\n")
            .append("        try {\n")
            .append("            switch (method) {\n")
            .append("                case \"GetVersion\": return \"1.0\";\n")
            .append("                case \"GetDSVersion\": return \"2.75\";\n")
            .append("                case \"GetDSBuild\": return \"275\";\n")
            .append("                case \"GetOSVersion\": return String.valueOf(android.os.Build.VERSION.SDK_INT);\n")
            .append("                case \"GetModel\": return android.os.Build.MODEL;\n")
            .append("                case \"GetPackageName\": return getPackageName();\n")
            .append("                case \"GetAppName\": return getString(android.R.string.unknownName);\n")
            .append("                case \"GetScreenWidth\": return String.valueOf(getResources().getDisplayMetrics().widthPixels);\n")
            .append("                case \"GetScreenHeight\": return String.valueOf(getResources().getDisplayMetrics().heightPixels);\n")
            .append("                case \"GetScreenDensity\": return String.valueOf(getResources().getDisplayMetrics().density);\n")
            .append("                case \"GetDeviceId\": return android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);\n")
            .append("                case \"GetType\": return \"Android\";\n")
            .append("                case \"GetDebug\": return \"0\";\n")
            .append("                case \"IsDebugEnabled\": return \"false\";\n")
            .append("                case \"IsDebugging\": return \"false\";\n")
            .append("                case \"IsAPK\": return \"true\";\n")
            .append("                case \"IsPremium\": return \"true\";\n")
            .append("                case \"IsEngine\": return \"false\";\n")
            .append("                case \"IsTablet\": return String.valueOf((getResources().getConfiguration().screenLayout & android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK) >= android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE);\n")
            .append("                case \"HasExternalStorage\": return \"true\";\n")
            .append("                case \"IsConnected\": return \"true\";\n")
            .append("                case \"GetOrientation\": return getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE ? \"Landscape\" : \"Portrait\";\n")
            .append("                case \"GetLanguage\": return java.util.Locale.getDefault().getDisplayLanguage();\n")
            .append("                case \"GetLanguageCode\": return java.util.Locale.getDefault().getLanguage();\n")
            .append("                case \"GetCountry\": return java.util.Locale.getDefault().getDisplayCountry();\n")
            .append("                case \"GetCountryCode\": return java.util.Locale.getDefault().getCountry();\n")
            .append("                case \"GetIPAddress\": return \"127.0.0.1\";\n")
            .append("                case \"GetBatteryLevel\": return \"100\";\n")
            .append("                case \"IsCharging\": return \"true\";\n")
            .append("                case \"GetPrivateFolder\": return getFilesDir().getAbsolutePath();\n")
            .append("                case \"GetPublicFolder\": return \"/sdcard\";\n")
            .append("                case \"GetTempFolder\": return getCacheDir().getAbsolutePath();\n")
            .append("                case \"GetExternalFolder\": return \"/sdcard\";\n")
            .append("                case \"GetInternalFolder\": return getFilesDir().getAbsolutePath();\n")
            .append("                case \"GetDisplayWidth\": return String.valueOf(getResources().getDisplayMetrics().widthPixels);\n")
            .append("                case \"GetDisplayHeight\": return String.valueOf(getResources().getDisplayMetrics().heightPixels);\n")
            .append("                case \"GetDefaultOrientation\": return \"Portrait\";\n")
            .append("                case \"GetRotation\": return String.valueOf(getWindowManager().getDefaultDisplay().getRotation());\n")
            .append("                case \"GetAppPath\": return getPackageCodePath();\n")
            .append("                case \"GetPath\": return getFilesDir().getAbsolutePath();\n")
            .append("                case \"GetName\": return getPackageName();\n")
            .append("                case \"GetFreeSpace\": return String.valueOf(new java.io.File(\"/\").getFreeSpace());\n")
            .append("                case \"GetKeyboardHeight\": return \"0\";\n")
            .append("                case \"IsKeyboardShown\": return \"false\";\n")
            .append("                case \"HasSoftNav\": return \"false\";\n")
            .append("                case \"IsPortrait\": return String.valueOf(getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT);\n")
            .append("                case \"IsAppInstalled\": return \"false\";\n")
            .append("                case \"FileExists\": return String.valueOf(new java.io.File(getArg(args,0)).exists());\n")
            .append("                case \"FolderExists\": return String.valueOf(new java.io.File(getArg(args,0)).isDirectory());\n")
            .append("                case \"IsFolder\": return String.valueOf(new java.io.File(getArg(args,0)).isDirectory());\n")
            .append("                case \"ReadFile\": return readFileContent(getArg(args,0));\n")
            .append("                case \"GetFileSize\": return String.valueOf(new java.io.File(getArg(args,0)).length());\n")
            .append("                case \"GetFileDate\": return String.valueOf(new java.io.File(getArg(args,0)).lastModified());\n")
            .append("                case \"ShowPopup\":\n")
            .append("                    showToast(getArg(args,0));\n")
            .append("                    return \"\";\n")
            .append("                case \"Vibrate\":\n")
            .append("                    vibrate(getArg(args,0));\n")
            .append("                    return \"\";\n")
            .append("                case \"Exit\":\n")
            .append("                    _h.post(() -> android.os.Process.killProcess(android.os.Process.myPid()));\n")
            .append("                    return \"\";\n")
            .append("                case \"SetTitle\":\n")
            .append("                    final String t = getArg(args,0);\n")
            .append("                    if (!t.isEmpty()) _h.post(() -> { if (getActionBar() != null) getActionBar().setTitle(t); });\n")
            .append("                    return \"\";\n")
            .append("                default: return null;\n")
            .append("            }\n")
            .append("        } catch (Exception e) { return null; }\n")
            .append("    }\n")
            .append("    private void showToast(String msg) {\n")
            .append("        _h.post(() -> android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show());\n")
            .append("    }\n")
            .append("    private void vibrate(String pattern) {\n")
            .append("        try {\n")
            .append("            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);\n")
            .append("            if (v != null) {\n")
            .append("                long ms = Long.parseLong(pattern);\n")
            .append("                if (Build.VERSION.SDK_INT >= 26)\n")
            .append("                    v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));\n")
            .append("                else v.vibrate(ms);\n")
            .append("            }\n")
            .append("        } catch (Exception e) {}\n")
            .append("    }\n")
            .append("    private String readFileContent(String path) {\n")
            .append("        try {\n")
            .append("            java.io.File f = new java.io.File(path);\n")
            .append("            if (!f.exists()) {\n")
            .append("                InputStream in = getAssets().open(path);\n")
            .append("                ByteArrayOutputStream buf = new ByteArrayOutputStream();\n")
            .append("                byte[] tmp = new byte[4096]; int n;\n")
            .append("                while ((n = in.read(tmp)) != -1) buf.write(tmp, 0, n);\n")
            .append("                in.close();\n")
            .append("                return buf.toString(\"UTF-8\");\n")
            .append("            }\n")
            .append("            return new String(java.nio.file.Files.readAllBytes(f.toPath()), \"UTF-8\");\n")
            .append("        } catch (Exception e) { return \"\"; }\n")
            .append("    }\n")
            .append("    public class DsBridge {\n")
            .append("        @JavascriptInterface\n")
            .append("        public void Execute(final String js) {\n")
            .append("            _h.post(new Runnable() {\n")
            .append("                public void run() { _wv.evaluateJavascript(js, null); }\n")
            .append("            });\n")
            .append("        }\n")
            .append("        @JavascriptInterface\n")
            .append("        public void ShowPopup(final String msg) {\n")
            .append("            _h.post(new Runnable() {\n")
            .append("                public void run() {\n")
            .append("                    android.widget.Toast.makeText(_wv.getContext(), msg, android.widget.Toast.LENGTH_SHORT).show();\n")
            .append("                }\n")
            .append("            });\n")
            .append("        }\n")
            .append("        @JavascriptInterface\n")
            .append("        public String GetType() { return \"Android\"; }\n")
            .append("        @JavascriptInterface\n")
            .append("        public String GetDebug() { return \"0\"; }\n")
            .append("        @JavascriptInterface\n")
            .append("        public String GetVersion() { return \"1.0\"; }\n")
            .append("        @JavascriptInterface\n")
            .append("        public String GetPackageName() { return getPackageName(); }\n")
            .append("        @JavascriptInterface\n")
            .append("        public String GetDeviceId() {\n")
            .append("            return android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);\n")
            .append("        }\n")
            .append("        @JavascriptInterface\n")
            .append("        public String GetScreenWidth() { return String.valueOf(getResources().getDisplayMetrics().widthPixels); }\n")
            .append("        @JavascriptInterface\n")
            .append("        public String GetScreenHeight() { return String.valueOf(getResources().getDisplayMetrics().heightPixels); }\n")
            .append("        @JavascriptInterface\n")
            .append("        public void Exit() {\n")
            .append("            _h.post(() -> android.os.Process.killProcess(android.os.Process.myPid()));\n")
            .append("        }\n")
            .append("        @JavascriptInterface\n")
            .append("        public void Vibrate(final String ms) {\n")
            .append("            try {\n")
            .append("                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);\n")
            .append("                if (v != null) {\n")
            .append("                    long t = Long.parseLong(ms);\n")
            .append("                    if (Build.VERSION.SDK_INT >= 26)\n")
            .append("                        v.vibrate(VibrationEffect.createOneShot(t, VibrationEffect.DEFAULT_AMPLITUDE));\n")
            .append("                    else v.vibrate(t);\n")
            .append("                }\n")
            .append("            } catch (Exception e) {}\n")
            .append("        }\n")
            .append("    }\n")
            .append("}");
    }

    @Override protected void copyAssets(File workDir, String sourceInput, String extraInput) throws Exception {
        // Copy user's JS file as index.js (main entry)
        Files.copy(Paths.get(sourceInput), Paths.get(workDir + "\\assets\\index.js"), StandardCopyOption.REPLACE_EXISTING);

        // Collect js libs from tools/ skipping app.js (dsbridge.js replaces it)
        File toolsDir = new File(System.getProperty("user.dir") + "\\tools");
        StringBuilder libScripts = new StringBuilder();
        if (toolsDir.exists() && toolsDir.isDirectory()) {
            File[] jsLibs = toolsDir.listFiles((dir, name) -> name.endsWith(".js") && !name.equals("app.js"));
            if (jsLibs != null) {
                for (File js : jsLibs) {
                    if (js.length() > 100000) continue; // skip large files (app.js is 160KB)
                    Files.copy(js.toPath(), Paths.get(workDir + "\\assets\\" + js.getName()), StandardCopyOption.REPLACE_EXISTING);
                    libScripts.append("<script src=\"").append(js.getName()).append("\"></script>\n");
                }
            }
        }

        // Load dsbridge.js from classpath resources
        String dsbridge = loadResource("/com/eagle/resources/dsbridge.js");

        // Generate main HTML: dsbridge.js inline, then libs, then user's index.js
        String wrapperHtml = "<!DOCTYPE html><html><head><meta charset=\"utf-8\">"
            + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no\">"
            + "<style>*{margin:0;padding:0;box-sizing:border-box}body{font-family:sans-serif;background:#f5f5f5;color:#222}"
            + "#loading{position:fixed;top:0;left:0;right:0;bottom:0;display:flex;align-items:center;justify-content:center;background:#f5f5f5;z-index:9999;font-size:16px;color:#888;flex-direction:column;gap:12px}"
            + "#loading .spinner{width:36px;height:36px;border:3px solid #ddd;border-top-color:#6c5ce7;border-radius:50%;animation:spin .8s linear infinite}"
            + "@keyframes spin{to{transform:rotate(360deg)}}"
            + "#error{display:none;position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);background:#fff;padding:24px;border-radius:12px;box-shadow:0 4px 24px rgba(0,0,0,0.12);text-align:center;max-width:85%;z-index:10000}"
            + "#error h3{color:#e74c3c;margin-bottom:6px;font-size:16px}#error p{color:#666;font-size:13px;margin-bottom:10px;word-break:break-all}"
            + "#error button{background:#6c5ce7;color:#fff;border:none;padding:8px 20px;border-radius:8px;cursor:pointer;font-size:13px}"
            + "</style></head><body>"
            + "<div id=\"loading\"><div class=\"spinner\"></div><span>Loading...</span></div>"
            + "<div id=\"error\"><h3>Script Error</h3><p id=\"errMsg\"></p><button onclick=\"this.parentElement.style.display='none'\">OK</button></div>"
            + "<div id=\"app\"></div>"
            + "<script>" + dsbridge + "</script>"
            + libScripts.toString()
            + "<script src=\"index.js\"></script>"
            + "<script>document.getElementById('loading').style.display='none';"
            + "window.onerror=function(m,u,l){var el=document.getElementById('errMsg');"
            + "el.textContent=m+(u?' at '+u:'')+(l?' line '+l:'');document.getElementById('error').style.display='block';return true}</script>"
            + "</body></html>";
        Files.write(Paths.get(workDir + "\\assets\\_main_.html"), wrapperHtml.getBytes(StandardCharsets.UTF_8));
    }
}
