package com.eagle.controller;

import com.jfoenix.controls.JFXTextField;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class QuizApkController extends AbstractApkBuilder {

    @FXML private JFXTextField zipfile;
    @FXML private TextArea quizdata;

    @Override protected String getHeaderTitle() { return "Quiz APK"; }
    @Override protected String getHeaderSubtitle() { return "Create a native Android quiz app with questions and answers"; }
    @Override protected String getSourcePrompt() { return null; }
    @Override protected String getExtraPrompt() { return null; }
    @Override protected String getSourceExtension() { return null; }
    @Override protected String getExtraExtension() { return null; }
    @Override protected int getTaskCount() { return 10; }
    @Override protected String getSourceInput() { return quizdata.getText().trim(); }
    @Override protected String getExtraInput() { return null; }

    @Override protected void onBrowseSource(File f) {}
    @Override protected void onBrowseExtra(File f) {}

    @Override protected boolean validateInputs(String apkName, String apkPkg, String apkIcon, String sourceInput, String extraInput) {
        if (sourceInput.isEmpty()) {
            showAlert("Missing data", "Please enter quiz questions and answers.");
            return false;
        }
        if (!sourceInput.contains("---")) {
            showAlert("Invalid format", "Use --- (three dashes) to separate each question block.");
            return false;
        }
        return true;
    }

    @Override protected void generateManifest(String pkgPath, StringBuilder xml) {
        xml.append("<application android:label=\"").append(apkname.getText().trim()).append("\"")
            .append(" android:icon=\"@drawable/icon\" android:allowBackup=\"true\"")
            .append(" android:hardwareAccelerated=\"true\">")
            .append("<activity android:name=\"").append(pkgPath).append(".MainActivity\"")
            .append(" android:exported=\"true\"")
            .append(" android:configChanges=\"keyboard|keyboardHidden|orientation|screenSize\">")
            .append("<intent-filter><action android:name=\"android.intent.action.MAIN\"/>")
            .append("<category android:name=\"android.intent.category.LAUNCHER\"/>")
            .append("</intent-filter></activity></application>");
    }

    @Override protected void generateJavaSource(String pkgPath, StringBuilder java) {
        java.append("import android.app.Activity;")
            .append("import android.os.Bundle;")
            .append("import android.webkit.WebView;")
            .append("import android.webkit.WebSettings;")
            .append("import android.webkit.WebViewClient;")
            .append("public class MainActivity extends Activity {")
            .append("    @Override")
            .append("    protected void onCreate(Bundle savedInstanceState) {")
            .append("        super.onCreate(savedInstanceState);")
            .append("        WebView wv = new WebView(this);")
            .append("        WebSettings s = wv.getSettings();")
            .append("        s.setJavaScriptEnabled(true);")
            .append("        s.setAllowFileAccess(true);")
            .append("        s.setAllowFileAccessFromFileURLs(true);")
            .append("        s.setAllowUniversalAccessFromFileURLs(true);")
            .append("        s.setDomStorageEnabled(true);")
            .append("        s.setLoadWithOverviewMode(true);")
            .append("        s.setUseWideViewPort(true);")
            .append("        wv.setWebViewClient(new WebViewClient());")
            .append("        wv.loadUrl(\"file:///android_asset/_main_.html\");")
            .append("        setContentView(wv);")
            .append("    }")
            .append("}");
    }

    @Override protected void copyAssets(File workDir, String sourceInput, String extraInput) throws Exception {
        java.util.List<String> blocks = java.util.Arrays.asList(sourceInput.split("---\\s*"));
        StringBuilder json = new StringBuilder("[");
        int qCount = 0;
        for (int i = 0; i < blocks.size(); i++) {
            String block = blocks.get(i).trim();
            if (block.isEmpty()) continue;
            String[] lines = block.split("\\n");
            if (lines.length < 3) continue;
            String question = lines[0].trim();
            String correct = lines[lines.length - 1].trim().toUpperCase();
            StringBuilder opts = new StringBuilder("[");
            for (int j = 1; j < lines.length - 1; j++) {
                if (j > 1) opts.append(",");
                opts.append("\"").append(escapeJson(lines[j].trim())).append("\"");
            }
            opts.append("]");
            if (qCount > 0) json.append(",");
            json.append("{\"q\":\"").append(escapeJson(question)).append("\",")
                .append("\"o\":").append(opts).append(",")
                .append("\"a\":\"").append(correct).append("\"}");
            qCount++;
        }
        json.append("]");

        String html = "<!DOCTYPE html>\n" +
"<html lang=\"ar\" dir=\"rtl\">\n" +
"<head>\n" +
"    <meta charset=\"utf-8\">\n" +
"    <meta name=\"viewport\" content=\"width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no\">\n" +
"    <title>كويز حديث</title>\n" +
"    <style>\n" +
"        *{margin:0;padding:0;box-sizing:border-box}\n" +
"        body{font-family:'Segoe UI',Tahoma,sans-serif;background:#0f0f1a;color:#eee;min-height:100vh;transition:background .3s}\n" +
"        .light body,.light{background:#f4f4f9;color:#222}\n" +
"        \n" +
"        #topBar{background:#1a1a2e;padding:12px 16px;display:flex;align-items:center;gap:8px;border-bottom:1px solid #2a2a4a;position:sticky;top:0;z-index:50}\n" +
"        #topBar h1{font-size:18px;flex:1;font-weight:600}\n" +
"        .badge{padding:5px 12px;border-radius:9999px;font-size:13px;font-weight:600;background:#16213e}\n" +
"        #timerBadge{min-width:70px;text-align:center;background:#e74c3c;color:#fff}\n" +
"        #scoreBadge{background:#2ecc71;color:#fff}\n" +
"        #themeBtn{background:#16213e;border:none;color:#eee;width:36px;height:36px;border-radius:50%;cursor:pointer;font-size:16px}\n" +
"\n" +
"        #progressStrip{height:4px;background:#16213e;width:100%}\n" +
"        #progressFill{height:100%;background:linear-gradient(90deg,#e94560,#2ecc71);transition:width .4s}\n" +
"\n" +
"        #questionArea{padding:20px 20px 12px}\n" +
"        #qCounter{font-size:13px;color:#888;margin-bottom:8px}\n" +
"        #questionText{font-size:18px;line-height:1.55;margin-bottom:16px;font-weight:500}\n" +
"        \n" +
"        #qTimer{font-size:15px;font-weight:700;color:#e74c3c;margin-bottom:12px;text-align:center}\n" +
"\n" +
"        #optionsArea{padding:0 20px 20px;display:flex;flex-direction:column;gap:10px}\n" +
"        .optBtn{width:100%;padding:14px 16px;background:#16213e;border:2px solid #2a2a4a;border-radius:12px;color:#eee;font-size:15px;cursor:pointer;transition:all .2s;text-align:left}\n" +
"        .optBtn:hover{border-color:#6c5ce7}\n" +
"        .optBtn .lbl{display:inline-flex;align-items:center;justify-content:center;width:28px;height:28px;border-radius:50%;background:#0f3460;margin-right:12px;font-weight:700}\n" +
"\n" +
"        .optBtn.correct{border-color:#2ecc71!important;background:rgba(46,204,113,0.15)!important}\n" +
"        .optBtn.wrong{border-color:#e74c3c!important;background:rgba(231,76,60,0.15)!important}\n" +
"        .optBtn .lbl.correct{background:#2ecc71;color:#fff}\n" +
"        .optBtn .lbl.wrong{background:#e74c3c;color:#fff}\n" +
"        .optBtn.disabled{pointer-events:none;opacity:0.7}\n" +
"\n" +
"        #fb{display:none;padding:12px 20px;margin:0 20px 16px;border-radius:8px;font-weight:600}\n" +
"        #fb.ok{background:rgba(46,204,113,0.2);color:#2ecc71}\n" +
"        #fb.nope{background:rgba(231,76,60,0.2);color:#e74c3c}\n" +
"\n" +
"        .resultScreen{display:none;padding:32px 20px;text-align:center;background:#0f0f1a;min-height:100vh}\n" +
"        .resultScreen .big{font-size:62px;font-weight:700;color:#e94560}\n" +
"        .resultScreen .pct{font-size:15px;color:#888;margin:8px 0}\n" +
"        .resultScreen .stats{display:flex;justify-content:center;gap:30px;margin:20px 0;flex-wrap:wrap}\n" +
"        .resultScreen .wrongs{margin-top:20px;text-align:right}\n" +
"        .witem{padding:12px;background:#1a1a2e;border-radius:10px;margin-bottom:10px;font-size:14px}\n" +
"\n" +
"        .btn{background:linear-gradient(90deg,#e94560,#ff6b6b);color:#fff;padding:14px 32px;border:none;border-radius:9999px;font-weight:700;cursor:pointer;margin:6px}\n" +
"        .btn2{background:#16213e;color:#eee;padding:12px 26px;border:none;border-radius:9999px;cursor:pointer;margin:6px}\n" +
"        \n" +
"        .modal{display:none;position:fixed;inset:0;background:rgba(0,0,0,0.9);z-index:100;align-items:center;justify-content:center}\n" +
"        .modal-content{background:#1a1a2e;padding:30px;border-radius:16px;width:90%;max-width:380px}\n" +
"    </style>\n" +
"</head>\n" +
"<body>\n" +
"    <div id=\"topBar\">\n" +
"        <h1>كويز</h1>\n" +
"        <span class=\"badge\" id=\"timerBadge\">00:00</span>\n" +
"        <span class=\"badge\" id=\"scoreBadge\">0/0</span>\n" +
"        <button id=\"themeBtn\" onclick=\"toggleTheme()\">🌙</button>\n" +
"    </div>\n" +
"\n" +
"    <div id=\"progressStrip\"><div id=\"progressFill\" style=\"width:0%\"></div></div>\n" +
"\n" +
"    <div id=\"questionArea\">\n" +
"        <div id=\"qCounter\"></div>\n" +
"        <div id=\"qTimer\">30</div>\n" +
"        <div id=\"questionText\"></div>\n" +
"    </div>\n" +
"\n" +
"    <div id=\"optionsArea\"></div>\n" +
"    <div id=\"fb\"></div>\n" +
"\n" +
"    <div class=\"resultScreen\" id=\"resultScreen\">\n" +
"        <div class=\"big\" id=\"finalScore\">0/0</div>\n" +
"        <div class=\"pct\" id=\"finalPct\">0%</div>\n" +
"        <div class=\"msg\" id=\"finalMsg\"></div>\n" +
"        \n" +
"        <div class=\"stats\">\n" +
"            <div><strong id=\"timeStat\">0:00</strong><br>الوقت الكلي</div>\n" +
"            <div><strong id=\"accuracyStat\">100%</strong><br>الدقة</div>\n" +
"        </div>\n" +
"\n" +
"        <div class=\"wrongs\" id=\"wrongs\"></div>\n" +
"        \n" +
"        <button class=\"btn\" onclick=\"restartQuiz()\">حاول مرة أخرى</button>\n" +
"        <button class=\"btn2\" onclick=\"toggleReview()\">مراجعة الأخطاء</button>\n" +
"        <button class=\"btn2\" onclick=\"shareResults()\">مشاركة النتيجة</button>\n" +
"    </div>\n" +
"\n" +
"    <!-- Modal -->\n" +
"    <div class=\"modal\" id=\"settingsModal\">\n" +
"        <div class=\"modal-content\">\n" +
"            <h2>⚙️ إعدادات الكويز</h2>\n" +
"            <p>مدة كل سؤال (ثانية):</p>\n" +
"            <input type=\"number\" id=\"qTime\" value=\"30\" style=\"width:80px;padding:8px;font-size:16px\">\n" +
"            <br><br>\n" +
"            <label><input type=\"checkbox\" id=\"shuffleQ\" checked> خلط الأسئلة</label><br><br>\n" +
"            <label><input type=\"checkbox\" id=\"shuffleO\" checked> خلط الخيارات</label><br><br>\n" +
"            <button class=\"btn\" onclick=\"startQuiz()\">ابدأ الكويز</button>\n" +
"        </div>\n" +
"    </div>\n" +
"\n" +
"    <script>\n" +
"        let QUIZ_DATA = "+json.toString()+"\n" +
"\n" +
"        let currentQuestion = 0, score = 0, totalTimer = 0, qTimer = 30;\n" +
"        let timerInterval, qTimerInterval, results = {}, wrongs = [], shuffledQuestions = [];\n" +
"        let isLight = false;\n" +
"\n" +
"        function shuffle(arr) { return arr.sort(() => Math.random() - 0.5); }\n" +
"\n" +
"        function startQuiz() {\n" +
"            qTimer = parseInt(document.getElementById('qTime').value) || 30;\n" +
"            document.getElementById('settingsModal').style.display = 'none';\n" +
"\n" +
"            shuffledQuestions = document.getElementById('shuffleQ').checked ? shuffle([...QUIZ_DATA]) : [...QUIZ_DATA];\n" +
"            \n" +
"            currentQuestion = 0; score = 0; totalTimer = 0; wrongs = []; results = {};\n" +
"            \n" +
"            document.getElementById('resultScreen').style.display = 'none';\n" +
"            document.getElementById('questionArea').style.display = 'block';\n" +
"            document.getElementById('optionsArea').style.display = 'flex';\n" +
"            document.getElementById('progressStrip').style.display = 'block';\n" +
"\n" +
"            clearInterval(timerInterval);\n" +
"            timerInterval = setInterval(() => {\n" +
"                totalTimer++;\n" +
"                const m = Math.floor(totalTimer/60), s = totalTimer%60;\n" +
"                document.getElementById('timerBadge').textContent = `${m}:${s<10?'0':''}${s}`;\n" +
"            }, 1000);\n" +
"\n" +
"            showQuestion();\n" +
"        }\n" +
"\n" +
"        function showQuestion() {\n" +
"            if (currentQuestion >= shuffledQuestions.length) return showResult();\n" +
"\n" +
"            const q = shuffledQuestions[currentQuestion];\n" +
"            document.getElementById('questionText').textContent = q.q;\n" +
"            document.getElementById('qCounter').textContent = `سؤال ${currentQuestion+1} من ${shuffledQuestions.length}`;\n" +
"            document.getElementById('progressFill').style.width = `${((currentQuestion+1)/shuffledQuestions.length)*100}%`;\n" +
"\n" +
"            document.getElementById('fb').style.display = 'none';\n" +
"\n" +
"            // Question Timer\n" +
"            let timeLeft = qTimer;\n" +
"            document.getElementById('qTimer').textContent = timeLeft;\n" +
"            document.getElementById('qTimer').style.color = timeLeft <= 10 ? '#e74c3c' : '#ff9800';\n" +
"\n" +
"            clearInterval(qTimerInterval);\n" +
"            qTimerInterval = setInterval(() => {\n" +
"                timeLeft--;\n" +
"                document.getElementById('qTimer').textContent = timeLeft;\n" +
"                if (timeLeft <= 10) document.getElementById('qTimer').style.color = '#e74c3c';\n" +
"                if (timeLeft <= 0) {\n" +
"                    clearInterval(qTimerInterval);\n" +
"                    handleTimeout();\n" +
"                }\n" +
"            }, 1000);\n" +
"\n" +
"            const container = document.getElementById('optionsArea');\n" +
"            container.innerHTML = '';\n" +
"\n" +
"            let options = [...q.o];\n" +
"            if (document.getElementById('shuffleO').checked) options = shuffle(options);\n" +
"\n" +
"            const labels = ['A','B','C','D'];\n" +
"            options.forEach((text, i) => {\n" +
"                const btn = document.createElement('button');\n" +
"                btn.className = 'optBtn';\n" +
"                btn.innerHTML = `<span class=\"lbl\">${labels[i]}</span>${text}`;\n" +
"                btn.onclick = () => selectAnswer(i, options, q.a, text, q.q);\n" +
"                container.appendChild(btn);\n" +
"            });\n" +
"        }\n" +
"\n" +
"        function handleTimeout() {\n" +
"            const q = shuffledQuestions[currentQuestion];\n" +
"            const correctLetter = q.a.toUpperCase();\n" +
"            const correctIndex = q.o.indexOf(q.o[correctLetter.charCodeAt(0)-65]);\n" +
"            \n" +
"            wrongs.push({\n" +
"                q: q.q,\n" +
"                correct: `${correctLetter}. ${q.o[correctIndex]}`,\n" +
"                your: \"انتهى الوقت\"\n" +
"            });\n" +
"\n" +
"            const fb = document.getElementById('fb');\n" +
"            fb.className = 'nope';\n" +
"            fb.textContent = `⏰ انتهى الوقت! الصحيح: ${correctLetter}. ${q.o[correctIndex]}`;\n" +
"            fb.style.display = 'block';\n" +
"\n" +
"            setTimeout(() => {\n" +
"                currentQuestion++;\n" +
"                showQuestion();\n" +
"            }, 1800);\n" +
"        }\n" +
"\n" +
"        function selectAnswer(selectedIdx, currentOpts, correctLetter, selectedText, qText) {\n" +
"            clearInterval(qTimerInterval);\n" +
"            const correctIdx = currentOpts.indexOf(shuffledQuestions[currentQuestion].o[correctLetter.charCodeAt(0)-65]);\n" +
"            const buttons = document.querySelectorAll('.optBtn');\n" +
"            \n" +
"            buttons.forEach(b => b.classList.add('disabled'));\n" +
"            buttons[selectedIdx].classList.add(selectedIdx === correctIdx ? 'correct' : 'wrong');\n" +
"            if (selectedIdx !== correctIdx) buttons[correctIdx].classList.add('correct');\n" +
"\n" +
"            const fb = document.getElementById('fb');\n" +
"            if (selectedIdx === correctIdx) {\n" +
"                score++;\n" +
"                results[currentQuestion] = true;\n" +
"                fb.className = 'ok'; fb.textContent = '✅ إجابة صحيحة!';\n" +
"            } else {\n" +
"                fb.className = 'nope';\n" +
"                fb.textContent = `❌ الصحيح: ${correctLetter}. ${currentOpts[correctIdx]}`;\n" +
"                wrongs.push({\n" +
"                    q: qText,\n" +
"                    correct: `${correctLetter}. ${currentOpts[correctIdx]}`,\n" +
"                    your: `${String.fromCharCode(65+selectedIdx)}. ${selectedText}`\n" +
"                });\n" +
"            }\n" +
"\n" +
"            fb.style.display = 'block';\n" +
"            document.getElementById('scoreBadge').textContent = `${score}/${shuffledQuestions.length}`;\n" +
"\n" +
"            setTimeout(() => {\n" +
"                currentQuestion++;\n" +
"                showQuestion();\n" +
"            }, 1650);\n" +
"        }\n" +
"\n" +
"        function showResult() {\n" +
"            clearInterval(timerInterval);\n" +
"            clearInterval(qTimerInterval);\n" +
"            \n" +
"            const total = shuffledQuestions.length;\n" +
"            const pct = Math.round(score / total * 100);\n" +
"            \n" +
"            document.getElementById('questionArea').style.display = 'none';\n" +
"            document.getElementById('optionsArea').style.display = 'none';\n" +
"            document.getElementById('progressStrip').style.display = 'none';\n" +
"            document.getElementById('resultScreen').style.display = 'block';\n" +
"\n" +
"            document.getElementById('finalScore').textContent = `${score}/${total}`;\n" +
"            document.getElementById('finalPct').textContent = `${pct}%`;\n" +
"            \n" +
"            const m = Math.floor(totalTimer/60), s = totalTimer%60;\n" +
"            document.getElementById('timeStat').textContent = `${m}:${s<10?'0':''}${s}`;\n" +
"            document.getElementById('accuracyStat').textContent = `${pct}%`;\n" +
"\n" +
"            let msg = pct >= 85 ? '🎉 أداء رائع!' : pct >= 70 ? '👍 ممتاز' : pct >= 50 ? '✅ جيد' : '💪 حاول مرة أخرى';\n" +
"            document.getElementById('finalMsg').textContent = msg;\n" +
"\n" +
"            // مراجعة الأخطاء\n" +
"            const ws = document.getElementById('wrongs');\n" +
"            ws.innerHTML = wrongs.length ? '<h3 style=\"color:#e74c3c;margin-bottom:12px\">الأسئلة الخاطئة</h3>' : '<p style=\"color:#888\">لم يكن هناك أخطاء!</p>';\n" +
"            \n" +
"            wrongs.forEach(w => {\n" +
"                const div = document.createElement('div');\n" +
"                div.className = 'witem';\n" +
"                div.innerHTML = `<b>${w.q}</b><br><span style=\"color:#2ecc71\">✓ ${w.correct}</span><br><span style=\"color:#e74c3c\">✗ ${w.your}</span>`;\n" +
"                ws.appendChild(div);\n" +
"            });\n" +
"        }\n" +
"\n" +
"        function toggleReview() {\n" +
"            const ws = document.getElementById('wrongs');\n" +
"            ws.style.display = (ws.style.display === 'none' || ws.style.display === '') ? 'block' : 'none';\n" +
"        }\n" +
"\n" +
"        function shareResults() {\n" +
"            const pct = document.getElementById('finalPct').textContent;\n" +
"            const text = `🎯 حصلت على ${pct} في الكويز!\\nالوقت: ${document.getElementById('timeStat').textContent}`;\n" +
"            \n" +
"            if (navigator.share) {\n" +
"                navigator.share({ title: 'نتيجة الكويز', text });\n" +
"            } else {\n" +
"                navigator.clipboard.writeText(text).then(() => alert('✅ تم نسخ النتيجة!'));\n" +
"            }\n" +
"        }\n" +
"\n" +
"        function restartQuiz() {\n" +
"            document.getElementById('settingsModal').style.display = 'flex';\n" +
"        }\n" +
"\n" +
"        function toggleTheme() {\n" +
"            isLight = !isLight;\n" +
"            document.body.classList.toggle('light', isLight);\n" +
"            document.getElementById('themeBtn').textContent = isLight ? '☀️' : '🌙';\n" +
"        }\n" +
"\n" +
"        window.onload = () => {\n" +
"            if (!QUIZ_DATA || QUIZ_DATA.length === 0) {\n" +
"                alert(\"يرجى إضافة الأسئلة داخل QUIZ_DATA\");\n" +
"            } else {\n" +
"                document.getElementById('settingsModal').style.display = 'flex';\n" +
"            }\n" +
"        };\n" +
"    </script>\n" +
"</body>\n" +
"</html>";
        Files.write(Paths.get(workDir + "\\assets\\_main_.html"), html.getBytes(StandardCharsets.UTF_8));
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    @Override public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        super.initialize(location, resources);
        quizdata.textProperty().addListener((obs, old, val) -> {
            if (val != null && !val.trim().isEmpty()) create.setDisable(false);
            else create.setDisable(true);
        });
    }
}
