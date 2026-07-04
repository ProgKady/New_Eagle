package com.eagle.controller;

import com.jfoenix.controls.JFXTextField;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.FileChooser;

public class PdfToApkController extends AbstractApkBuilder {

    @FXML private JFXTextField pdffile;

    @Override protected String getHeaderTitle() { return "PDF to APK"; }
    @Override protected String getHeaderSubtitle() { return "Turn any PDF document into a native Android app"; }
    @Override protected String getSourcePrompt() { return "Select PDF File"; }
    @Override protected String getExtraPrompt() { return null; }
    @Override protected String getSourceExtension() { return "*.pdf"; }
    @Override protected String getExtraExtension() { return null; }
    @Override protected int getTaskCount() { return 10; }
    @Override protected String getSourceInput() { return pdffile.getText().trim(); }
    @Override protected String getExtraInput() { return null; }

    @FXML void browse1act(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fc.setTitle("Select PDF File");
        File f = fc.showOpenDialog(pdffile.getScene().getWindow());
        if (f != null) onBrowseSource(f);
    }

    @Override protected void onBrowseSource(File f) {
        pdffile.setText(f.getAbsolutePath());
        create.setDisable(false);
    }

    @Override protected void onBrowseExtra(File f) {}

    @Override protected boolean validateInputs(String apkName, String apkPkg, String apkIcon, String sourceInput, String extraInput) {
        if (sourceInput.isEmpty()) {
            showAlert("Missing file", "Please select a PDF file.");
            return false;
        }
        return true;
    }

    @Override protected void generateManifest(String pkgPath, StringBuilder xml) {
        xml.append("<uses-permission android:name=\"android.permission.INTERNET\"/>")
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
            .append("        s.setAllowFileAccessFromFileURLs(true);\n")
            .append("        s.setAllowUniversalAccessFromFileURLs(true);\n")
            .append("        s.setAllowContentAccess(true);\n")
            .append("        s.setDomStorageEnabled(true);\n")
            .append("        s.setLoadWithOverviewMode(true);\n")
            .append("        s.setUseWideViewPort(true);\n")
            .append("        s.setBuiltInZoomControls(true);\n")
            .append("        s.setDisplayZoomControls(false);\n")
            .append("        wv.setWebViewClient(new WebViewClient());\n")
            .append("        wv.loadUrl(\"file:///android_asset/_main_.html\");\n")
            .append("        setContentView(wv);\n")
            .append("    }\n")
            .append("}");
    }

    @Override protected void copyAssets(File workDir, String sourceInput, String extraInput) throws Exception {
        Files.copy(Paths.get(sourceInput), Paths.get(workDir + "\\assets\\document.pdf"), StandardCopyOption.REPLACE_EXISTING);

        String pdfPath = "file:///android_asset/document.pdf";
        String html = "<!DOCTYPE html>\n" +
"<html lang=\"ar\" dir=\"rtl\">\n" +
"<head>\n" +
"    <meta charset=\"utf-8\">\n" +
"    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">\n" +
"    <title>PDF Pro Viewer - Ultimate</title>\n" +
"    <script src=\"https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.min.js\"></script>\n" +
"    <style>\n" +
"        :root {\n" +
"            --bg: #0a0a14;\n" +
"            --surface: #161625;\n" +
"            --card: #1e1e2f;\n" +
"            --accent: #00e6ff;\n" +
"            --accent2: #ff3366;\n" +
"            --text: #f0f0f5;\n" +
"            --text2: #b0b0c0;\n" +
"            --border: #2a2a3a;\n" +
"        }\n" +
"        .light {\n" +
"            --bg: #f5f7fa;\n" +
"            --surface: #ffffff;\n" +
"            --card: #edf0f5;\n" +
"            --accent: #0066ff;\n" +
"            --accent2: #e91e63;\n" +
"            --text: #1a1a2e;\n" +
"            --text2: #555;\n" +
"            --border: #d0d4e0;\n" +
"        }\n" +
"        * { margin:0; padding:0; box-sizing:border-box; }\n" +
"        body {\n" +
"            font-family: 'Segoe UI', Tahoma, sans-serif;\n" +
"            background: var(--bg);\n" +
"            color: var(--text);\n" +
"            height: 100vh;\n" +
"            overflow: hidden;\n" +
"            transition: 0.4s;\n" +
"        }\n" +
"        #topbar {\n" +
"            position: fixed;\n" +
"            top: 0; left: 0; right: 0;\n" +
"            z-index: 1000;\n" +
"            background: rgba(22,22,37,0.97);\n" +
"            backdrop-filter: blur(20px);\n" +
"            border-bottom: 1px solid var(--border);\n" +
"            padding: 8px 10px;\n" +
"            display: flex;\n" +
"            align-items: center;\n" +
"            gap: 6px;\n" +
"            flex-wrap: wrap;\n" +
"            min-height: 62px;\n" +
"        }\n" +
"        button, select {\n" +
"            background: var(--card);\n" +
"            border: none;\n" +
"            color: var(--text);\n" +
"            padding: 8px 12px;\n" +
"            border-radius: 10px;\n" +
"            cursor: pointer;\n" +
"            font-size: 14px;\n" +
"            transition: all 0.25s;\n" +
"        }\n" +
"        button:hover, select:hover { background: var(--accent); color: #000; transform: scale(1.05); }\n" +
"        .active { background: var(--accent)!important; color: #000; }\n" +
"\n" +
"        #sidebar {\n" +
"            position: fixed;\n" +
"            top: 62px; left: 0; bottom: 0;\n" +
"            width: 280px;\n" +
"            background: var(--surface);\n" +
"            border-left: 1px solid var(--border);\n" +
"            transform: translateX(-100%);\n" +
"            transition: transform 0.4s ease;\n" +
"            z-index: 950;\n" +
"            overflow-y: auto;\n" +
"            padding: 15px;\n" +
"        }\n" +
"        #sidebar.open { transform: translateX(0); }\n" +
"\n" +
"        #viewer {\n" +
"            position: fixed;\n" +
"            top: 62px; left: 0; right: 0; bottom: 0;\n" +
"            overflow: auto;\n" +
"            padding: 20px;\n" +
"            background: var(--bg);\n" +
"            display: flex;\n" +
"            flex-direction: column;\n" +
"            align-items: center;\n" +
"            gap: 20px;\n" +
"        }\n" +
"        canvas {\n" +
"            box-shadow: 0 15px 40px rgba(0,0,0,0.7);\n" +
"            border-radius: 12px;\n" +
"            max-width: 100%;\n" +
"            transition: transform 0.3s;\n" +
"        }\n" +
"        #thumbPanel {\n" +
"            position: fixed;\n" +
"            top: 62px; right: 0; bottom: 0;\n" +
"            width: 210px;\n" +
"            background: var(--surface);\n" +
"            border-right: 1px solid var(--border);\n" +
"            overflow-y: auto;\n" +
"            padding: 12px;\n" +
"            transform: translateX(100%);\n" +
"            transition: 0.4s;\n" +
"            z-index: 900;\n" +
"        }\n" +
"        #thumbPanel.open { transform: translateX(0); }\n" +
"\n" +
"        #searchBar {\n" +
"            position: fixed;\n" +
"            top: 62px; left: 0; right: 0;\n" +
"            background: var(--surface);\n" +
"            padding: 12px;\n" +
"            display: none;\n" +
"            align-items: center;\n" +
"            gap: 10px;\n" +
"            z-index: 960;\n" +
"            border-bottom: 1px solid var(--border);\n" +
"        }\n" +
"        #loader {\n" +
"            position: fixed;\n" +
"            inset: 0;\n" +
"            background: rgba(10,10,20,0.98);\n" +
"            display: flex;\n" +
"            flex-direction: column;\n" +
"            align-items: center;\n" +
"            justify-content: center;\n" +
"            z-index: 2000;\n" +
"        }\n" +
"        .spinner {\n" +
"            width: 60px; height: 60px;\n" +
"            border: 5px solid var(--card);\n" +
"            border-top-color: var(--accent);\n" +
"            border-radius: 50%;\n" +
"            animation: spin 1s linear infinite;\n" +
"        }\n" +
"        .progress { width: 280px; height: 6px; background: var(--card); margin-top: 20px; border-radius: 10px; overflow: hidden; }\n" +
"        .progress-bar { height: 100%; width: 0%; background: var(--accent); transition: width 0.3s; }\n" +
"        @keyframes spin { to { transform: rotate(360deg); } }\n" +
"    </style>\n" +
"</head>\n" +
"<body>\n" +
"\n" +
"    <div id=\"loader\">\n" +
"        <div class=\"spinner\"></div>\n" +
"        <p id=\"loaderMsg\" style=\"margin-top:25px;font-size:18px;color:var(--text2)\">جاري تحميل المستند...</p>\n" +
"        <div class=\"progress\"><div class=\"progress-bar\" id=\"progressBar\"></div></div>\n" +
"    </div>\n" +
"\n" +
"    <!-- Top Toolbar -->\n" +
"    <div id=\"topbar\">\n" +
"        <button onclick=\"toggleSidebar()\" title=\"القائمة الجانبية\">☰</button>\n" +
"        <input type=\"file\" id=\"fileInput\" accept=\".pdf\" style=\"display:none\" onchange=\"loadLocalFile(event)\">\n" +
"        <button onclick=\"document.getElementById('fileInput').click()\" title=\"فتح ملف من الجهاز\">📂</button>\n" +
"\n" +
"        <button onclick=\"prevPage()\" title=\"السابق\">◀</button>\n" +
"        <input id=\"pageInput\" type=\"number\" value=\"1\" style=\"width:75px;text-align:center\" onchange=\"goToPage(this.value)\">\n" +
"        <span id=\"totalPages\">/ 0</span>\n" +
"        <button onclick=\"nextPage()\" title=\"التالي\">▶</button>\n" +
"\n" +
"        <button onclick=\"toggleThumbs()\" title=\"مصغرات\">🖼️</button>\n" +
"        <button onclick=\"toggleSearch()\" title=\"بحث\">🔍</button>\n" +
"        \n" +
"        <button onclick=\"zoomOut()\" title=\"تصغير\">−</button>\n" +
"        <input type=\"range\" id=\"zoomSlider\" min=\"30\" max=\"500\" value=\"150\" oninput=\"setZoom(this.value)\" style=\"width:140px\">\n" +
"        <span id=\"zoomLabel\">100%</span>\n" +
"        <button onclick=\"zoomIn()\" title=\"تكبير\">+</button>\n" +
"\n" +
"        <button onclick=\"rotatePage()\" title=\"تدوير\">⟳</button>\n" +
"        <button onclick=\"fitPage()\" title=\"ملائمة الصفحة\">↔</button>\n" +
"        <button onclick=\"toggleViewMode()\" id=\"viewModeBtn\" title=\"وضع العرض\">📄</button>\n" +
"        \n" +
"        <button onclick=\"toggleFullscreen()\" title=\"ملء الشاشة\">⛶</button>\n" +
"        <button onclick=\"printPDF()\" title=\"طباعة\">🖨️</button>\n" +
"        <button onclick=\"downloadPDF()\" title=\"تحميل\">⬇️</button>\n" +
"        <button onclick=\"copyCurrentPageText()\" title=\"نسخ النص\">📋</button>\n" +
"\n" +
"        <button id=\"themeBtn\" onclick=\"toggleTheme()\" style=\"margin-right:auto\" title=\"تغيير الثيم\">☀️</button>\n" +
"    </div>\n" +
"\n" +
"    <!-- Sidebar -->\n" +
"    <div id=\"sidebar\">\n" +
"        <h3 style=\"margin-bottom:15px;color:var(--accent)\">PDF Pro Ultimate</h3>\n" +
"        <button onclick=\"toggleThumbs()\" style=\"width:100%;text-align:right;margin:6px 0\">📑 المصغرات</button>\n" +
"        <button onclick=\"loadOutline()\" style=\"width:100%;text-align:right;margin:6px 0\">📖 المحتوى (Outline)</button>\n" +
"        <button onclick=\"toggleSearch()\" style=\"width:100%;text-align:right;margin:6px 0\">🔎 بحث متقدم</button>\n" +
"        <button onclick=\"toggleNightMode()\" style=\"width:100%;text-align:right;margin:6px 0\">🌙 وضع الليل</button>\n" +
"        <hr style=\"margin:15px 0;border-color:var(--border)\">\n" +
"        <small style=\"color:var(--text2)\">أدوات إضافية</small>\n" +
"        <div style=\"margin-top:10px;font-size:13px;line-height:1.8;color:var(--text2)\">\n" +
"            Ctrl + F → بحث<br>\n" +
"            Ctrl + R → تدوير<br>\n" +
"            Ctrl + O → فتح ملف<br>\n" +
"            + / - → زوم\n" +
"        </div>\n" +
"    </div>\n" +
"\n" +
"    <!-- Search -->\n" +
"    <div id=\"searchBar\">\n" +
"        <input id=\"searchInput\" placeholder=\"ابحث في المستند...\" style=\"flex:1;padding:10px\">\n" +
"        <button onclick=\"searchAllPages()\">بحث</button>\n" +
"        <span id=\"searchCount\" style=\"white-space:nowrap\"></span>\n" +
"        <button onclick=\"prevMatch()\">↑</button>\n" +
"        <button onclick=\"nextMatch()\">↓</button>\n" +
"        <button onclick=\"toggleSearch()\">✕</button>\n" +
"    </div>\n" +
"\n" +
"    <!-- Thumbnails -->\n" +
"    <div id=\"thumbPanel\"></div>\n" +
"\n" +
"    <!-- Viewer -->\n" +
"    <div id=\"viewer\"></div>\n" +
"\n" +
"    <script>\n" +
"        pdfjsLib.GlobalWorkerOptions.workerSrc = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.worker.min.js';\n" +
"\n" +
"        let pdfDoc = null;\n" +
"        let pageNum = 1;\n" +
"        let scale = 1.5;\n" +
"        let rotation = 0;\n" +
"        let viewMode = 'single';\n" +
"        let darkMode = true;\n" +
"        let currentFileName = \"document.pdf\";\n" +
"        let searchResults = [];\n" +
"        let searchIndex = -1;\n" +
"\n" +
"        const viewer = document.getElementById('viewer');\n" +
"\n" +
"        // رابط اختباري عام\n" +
"        const demoPDF = \""+pdfPath+"\";\n" +
"\n" +
"        async function loadPDF(url, name = \"document.pdf\") {\n" +
"            document.getElementById('loader').style.display = 'flex';\n" +
"            document.getElementById('progressBar').style.width = '0%';\n" +
"            \n" +
"            try {\n" +
"                const loadingTask = pdfjsLib.getDocument(url);\n" +
"                loadingTask.onProgress = function(progress) {\n" +
"                    const percent = Math.round((progress.loaded / progress.total) * 100) || 50;\n" +
"                    document.getElementById('progressBar').style.width = percent + '%';\n" +
"                };\n" +
"                \n" +
"                pdfDoc = await loadingTask.promise;\n" +
"                currentFileName = name;\n" +
"                \n" +
"                document.getElementById('totalPages').textContent = `/ ${pdfDoc.numPages}`;\n" +
"                document.getElementById('pageInput').max = pdfDoc.numPages;\n" +
"                \n" +
"                renderPage(1);\n" +
"                renderThumbnails();\n" +
"                document.getElementById('loader').style.display = 'none';\n" +
"            } catch (err) {\n" +
"                document.getElementById('loaderMsg').innerHTML = `خطأ في التحميل:<br>${err.message}`;\n" +
"            }\n" +
"        }\n" +
"\n" +
"        async function renderPage(num) {\n" +
"            if (!pdfDoc) return;\n" +
"            pageNum = num;\n" +
"            document.getElementById('pageInput').value = num;\n" +
"\n" +
"            const page = await pdfDoc.getPage(num);\n" +
"            const viewport = page.getViewport({ scale: scale, rotation: rotation });\n" +
"\n" +
"            const canvas = document.createElement('canvas');\n" +
"            canvas.height = viewport.height;\n" +
"            canvas.width = viewport.width;\n" +
"\n" +
"            await page.render({\n" +
"                canvasContext: canvas.getContext('2d'),\n" +
"                viewport: viewport\n" +
"            }).promise;\n" +
"\n" +
"            viewer.innerHTML = '';\n" +
"            viewer.appendChild(canvas);\n" +
"        }\n" +
"\n" +
"        function goToPage(n) {\n" +
"            n = parseInt(n);\n" +
"            if (n < 1) n = 1;\n" +
"            if (n > pdfDoc.numPages) n = pdfDoc.numPages;\n" +
"            renderPage(n);\n" +
"        }\n" +
"\n" +
"        function nextPage() { if (pageNum < pdfDoc.numPages) renderPage(pageNum + 1); }\n" +
"        function prevPage() { if (pageNum > 1) renderPage(pageNum - 1); }\n" +
"\n" +
"        function zoomIn() { scale = Math.min(scale + 0.25, 5); renderPage(pageNum); updateZoom(); }\n" +
"        function zoomOut() { scale = Math.max(scale - 0.25, 0.4); renderPage(pageNum); updateZoom(); }\n" +
"        function setZoom(val) { scale = parseFloat(val) / 100; renderPage(pageNum); updateZoom(); }\n" +
"        function updateZoom() {\n" +
"            document.getElementById('zoomLabel').textContent = Math.round(scale * 100) + '%';\n" +
"            document.getElementById('zoomSlider').value = Math.round(scale * 100);\n" +
"        }\n" +
"        function fitPage() { scale = 1.5; updateZoom(); renderPage(pageNum); }\n" +
"\n" +
"        function rotatePage() {\n" +
"            rotation = (rotation + 90) % 360;\n" +
"            renderPage(pageNum);\n" +
"        }\n" +
"\n" +
"        function toggleThumbs() {\n" +
"            document.getElementById('thumbPanel').classList.toggle('open');\n" +
"        }\n" +
"\n" +
"        async function renderThumbnails() {\n" +
"            const panel = document.getElementById('thumbPanel');\n" +
"            panel.innerHTML = '<h4 style=\"margin:10px 0\">مصغرات</h4>';\n" +
"            for (let i = 1; i <= pdfDoc.numPages; i++) {\n" +
"                const page = await pdfDoc.getPage(i);\n" +
"                const viewport = page.getViewport({ scale: 0.22 });\n" +
"                const canvas = document.createElement('canvas');\n" +
"                canvas.height = viewport.height;\n" +
"                canvas.width = viewport.width;\n" +
"                await page.render({ canvasContext: canvas.getContext('2d'), viewport }).promise;\n" +
"                canvas.onclick = () => renderPage(i);\n" +
"                if (i === pageNum) canvas.style.border = `3px solid var(--accent)`;\n" +
"                panel.appendChild(canvas);\n" +
"            }\n" +
"        }\n" +
"\n" +
"        function toggleSidebar() { document.getElementById('sidebar').classList.toggle('open'); }\n" +
"        function toggleSearch() {\n" +
"            const bar = document.getElementById('searchBar');\n" +
"            bar.style.display = (bar.style.display === 'flex') ? 'none' : 'flex';\n" +
"            if (bar.style.display === 'flex') document.getElementById('searchInput').focus();\n" +
"        }\n" +
"\n" +
"        async function searchAllPages() {\n" +
"            const query = document.getElementById('searchInput').value.trim().toLowerCase();\n" +
"            if (!query || !pdfDoc) return;\n" +
"            searchResults = [];\n" +
"            for (let i = 1; i <= pdfDoc.numPages; i++) {\n" +
"                const page = await pdfDoc.getPage(i);\n" +
"                const tc = await page.getTextContent();\n" +
"                const text = tc.items.map(item => item.str.toLowerCase()).join(' ');\n" +
"                if (text.includes(query)) searchResults.push(i);\n" +
"            }\n" +
"            document.getElementById('searchCount').textContent = `${searchResults.length} نتيجة`;\n" +
"            if (searchResults.length > 0) renderPage(searchResults[0]);\n" +
"        }\n" +
"\n" +
"        function prevMatch() { if (searchResults.length > 0) { searchIndex = (searchIndex - 1 + searchResults.length) % searchResults.length; renderPage(searchResults[searchIndex]); } }\n" +
"        function nextMatch() { if (searchResults.length > 0) { searchIndex = (searchIndex + 1) % searchResults.length; renderPage(searchResults[searchIndex]); } }\n" +
"\n" +
"        function toggleTheme() {\n" +
"            darkMode = !darkMode;\n" +
"            document.body.classList.toggle('light');\n" +
"            document.getElementById('themeBtn').textContent = darkMode ? '☀️' : '🌙';\n" +
"        }\n" +
"\n" +
"        function toggleNightMode() {\n" +
"            document.body.style.filter = document.body.style.filter ? '' : 'invert(0.9) hue-rotate(180deg)';\n" +
"        }\n" +
"\n" +
"        function toggleFullscreen() {\n" +
"            if (!document.fullscreenElement) document.documentElement.requestFullscreen();\n" +
"            else document.exitFullscreen();\n" +
"        }\n" +
"\n" +
"        function printPDF() { window.print(); }\n" +
"\n" +
"        function downloadPDF() {\n" +
"            const a = document.createElement('a');\n" +
"            a.href = demoPDF; // أو رابط الملف المرفوع\n" +
"            a.download = currentFileName;\n" +
"            a.click();\n" +
"        }\n" +
"\n" +
"        async function copyCurrentPageText() {\n" +
"            if (!pdfDoc) return;\n" +
"            const page = await pdfDoc.getPage(pageNum);\n" +
"            const tc = await page.getTextContent();\n" +
"            const text = tc.items.map(i => i.str).join('\\n');\n" +
"            navigator.clipboard.writeText(text).then(() => alert(\"تم نسخ نص الصفحة\"));\n" +
"        }\n" +
"\n" +
"        function loadLocalFile(e) {\n" +
"            const file = e.target.files[0];\n" +
"            if (!file) return;\n" +
"            const reader = new FileReader();\n" +
"            reader.onload = async function(ev) {\n" +
"                const typed = new Uint8Array(ev.target.result);\n" +
"                const loadingTask = pdfjsLib.getDocument(typed);\n" +
"                pdfDoc = await loadingTask.promise;\n" +
"                currentFileName = file.name;\n" +
"                document.getElementById('totalPages').textContent = `/ ${pdfDoc.numPages}`;\n" +
"                renderPage(1);\n" +
"                renderThumbnails();\n" +
"            };\n" +
"            reader.readAsArrayBuffer(file);\n" +
"        }\n" +
"\n" +
"        function loadOutline() {\n" +
"            alert(\"المؤشرات (Outline) سيتم تحميلها إذا كانت موجودة في الملف - قيد التطوير المتقدم\");\n" +
"        }\n" +
"\n" +
"        function toggleViewMode() {\n" +
"            viewMode = viewMode === 'single' ? 'continuous' : 'single';\n" +
"            document.getElementById('viewModeBtn').textContent = viewMode === 'single' ? '📄' : '📑';\n" +
"            alert('تم تبديل الوضع إلى: ' + (viewMode === 'single' ? 'صفحة واحدة' : 'تمرير مستمر'));\n" +
"        }\n" +
"\n" +
"        // Keyboard Shortcuts\n" +
"        document.addEventListener('keydown', e => {\n" +
"            if (e.key === 'ArrowRight') nextPage();\n" +
"            if (e.key === 'ArrowLeft') prevPage();\n" +
"            if ((e.key === 'f' || e.key === 'F') && e.ctrlKey) { e.preventDefault(); toggleSearch(); }\n" +
"            if (e.key === 'r' && e.ctrlKey) { e.preventDefault(); rotatePage(); }\n" +
"            if (e.key === '+' || e.key === '=') zoomIn();\n" +
"            if (e.key === '-') zoomOut();\n" +
"            if (e.key.toLowerCase() === 'o' && e.ctrlKey) { e.preventDefault(); document.getElementById('fileInput').click(); }\n" +
"        });\n" +
"\n" +
"        // Drag & Drop\n" +
"        document.addEventListener('dragover', e => e.preventDefault());\n" +
"        document.addEventListener('drop', e => {\n" +
"            e.preventDefault();\n" +
"            const file = e.dataTransfer.files[0];\n" +
"            if (file && file.type === 'application/pdf') {\n" +
"                const reader = new FileReader();\n" +
"                reader.onload = async ev => {\n" +
"                    const typed = new Uint8Array(ev.target.result);\n" +
"                    pdfDoc = await pdfjsLib.getDocument(typed).promise;\n" +
"                    currentFileName = file.name;\n" +
"                    document.getElementById('totalPages').textContent = `/ ${pdfDoc.numPages}`;\n" +
"                    renderPage(1);\n" +
"                    renderThumbnails();\n" +
"                };\n" +
"                reader.readAsArrayBuffer(file);\n" +
"            }\n" +
"        });\n" +
"\n" +
"        // بدء التشغيل\n" +
"        window.onload = () => loadPDF(demoPDF, \""+pdfPath+"\");\n" +
"    </script>\n" +
"</body>\n" +
"</html>";
        Files.write(Paths.get(workDir + "\\assets\\_main_.html"), html.getBytes(StandardCharsets.UTF_8));
    }
}
