package com.eagle.controller;

import com.jfoenix.controls.JFXTextField;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.FileChooser;

public class MusicApkController extends AbstractApkBuilder {

    @FXML private JFXTextField zipfile;

    @Override protected String getHeaderTitle() { return "Music Player APK"; }
    @Override protected String getHeaderSubtitle() { return "Create a native Android music player from your MP3 files"; }
    @Override protected String getSourcePrompt() { return "Select ZIP with MP3 files"; }
    @Override protected String getExtraPrompt() { return null; }
    @Override protected String getSourceExtension() { return "*.zip"; }
    @Override protected String getExtraExtension() { return null; }
    @Override protected int getTaskCount() { return 10; }
    @Override protected String getSourceInput() { return zipfile.getText().trim(); }
    @Override protected String getExtraInput() { return null; }

    @FXML void browse1act(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Zip Files", "*.zip"));
        fc.setTitle("Select ZIP with MP3 files");
        File f = fc.showOpenDialog(zipfile.getScene().getWindow());
        if (f != null) onBrowseSource(f);
    }

    @Override protected void onBrowseSource(File f) {
        zipfile.setText(f.getAbsolutePath());
        create.setDisable(false);
    }

    @Override protected void onBrowseExtra(File f) {}

    @Override protected boolean validateInputs(String apkName, String apkPkg, String apkIcon, String sourceInput, String extraInput) {
        if (sourceInput.isEmpty()) {
            showAlert("Missing file", "Please select a ZIP file containing MP3 files.");
            return false;
        }
        return true;
    }

    @Override protected void generateManifest(String pkgPath, StringBuilder xml) {
        xml.append("<uses-permission android:name=\"android.permission.INTERNET\"/>")
            .append("<application android:label=\"").append(apkname.getText().trim()).append("\"")
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
            .append("        wv.setWebViewClient(new WebViewClient());\n")
            .append("        wv.loadUrl(\"file:///android_asset/_main_.html\");\n")
            .append("        setContentView(wv);\n")
            .append("    }\n")
            .append("}");
    }

    @Override protected void copyAssets(File workDir, String sourceInput, String extraInput) throws Exception {
        // Extract ZIP to assets
        extractZip(sourceInput, workDir + "\\assets");

        // Scan for MP3 files and generate music.json
        File assetsDir = new File(workDir, "assets");
        List<String> songList = new ArrayList<>();
        scanMp3Files(assetsDir, "", songList);

        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < songList.size(); i++) {
            if (i > 0) json.append(",");
            String name = songList.get(i);
            String display = name.replaceAll("(?i)\\.mp3$", "").replace("_", " ").replace("-", " ").trim();
            json.append("{\"name\":\"").append(escapeJson(display)).append("\",")
                .append("\"file\":\"").append(escapeJson(name)).append("\",")
                .append("\"duration\":0}");
        }
        json.append("]");
        Files.write(Paths.get(workDir + "\\assets\\music.json"), json.toString().getBytes(StandardCharsets.UTF_8));

        String html = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no\">"
            + "<style>:root{--bg:#0f0f1a;--surface:#1a1a2e;--card:#16213e;--accent:#e94560;--accent2:#6c5ce7;--text:#eee;--text2:#888;--border:#2a2a4a;--shadow:rgba(233,69,96,0.3)}"
            + ".light{--bg:#f4f4f9;--surface:#fff;--card:#eaeaf2;--accent:#6c5ce7;--accent2:#e94560;--text:#222;--text2:#888;--border:#ddd;--shadow:rgba(108,92,231,0.2)}"
            + "*{margin:0;padding:0;box-sizing:border-box}body{font-family:sans-serif;background:var(--bg);color:var(--text);min-height:100vh;display:flex;flex-direction:column;transition:background .3s,color .3s;overflow:hidden;height:100vh}"
            + "#header{padding:16px 20px;text-align:center;background:linear-gradient(135deg,var(--surface),var(--card));border-bottom:1px solid var(--border);position:relative;flex-shrink:0}"
            + "#header h1{font-size:20px;background:linear-gradient(135deg,var(--accent),var(--accent2));-webkit-background-clip:text;-webkit-text-fill-color:transparent}"
            + "#header p{font-size:11px;color:var(--text2);margin-top:2px;-webkit-text-fill-color:var(--text2)}"
            + "#themeBtn{position:absolute;top:12px;right:14px;background:var(--card);border:none;color:var(--text);width:34px;height:34px;border-radius:50%;cursor:pointer;font-size:16px;display:flex;align-items:center;justify-content:center}"
            + "#playerSection{padding:12px 16px 4px;flex-shrink:0}"
            + "#albumArt{width:140px;height:140px;margin:0 auto 8px;border-radius:20px;background:linear-gradient(135deg,var(--accent),var(--accent2));display:flex;align-items:center;justify-content:center;font-size:56px;box-shadow:0 6px 28px var(--shadow);transition:.3s}"
            + "#albumArt.pulse{animation:pulse 1.8s ease-in-out infinite}"
            + "@keyframes pulse{0%,100%{transform:scale(1)}50%{transform:scale(1.05)}}"
            + "#songInfo{text-align:center;padding:0 4px}#songInfo h2{font-size:15px;margin-bottom:2px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}"
            + "#songInfo p{font-size:11px;color:var(--text2)}"
            + "#progressWrap{padding:8px 16px 0}#progressBarBg{width:100%;height:4px;background:var(--card);border-radius:2px;cursor:pointer;position:relative}"
            + "#progressBarFill{height:100%;background:linear-gradient(90deg,var(--accent),var(--accent2));border-radius:2px;width:0%;transition:width .2s}"
            + "#timeRow{display:flex;justify-content:space-between;padding:2px 16px 0;font-size:10px;color:var(--text2)}"
            + "#controls{display:flex;justify-content:center;align-items:center;gap:18px;padding:8px 16px;flex-shrink:0}"
            + "#controls button{background:var(--card);border:none;color:var(--text);width:38px;height:38px;border-radius:50%;cursor:pointer;font-size:15px;transition:.2s;display:flex;align-items:center;justify-content:center}"
            + "#controls button:hover{background:var(--accent);color:#fff}#controls #playBtn{width:52px;height:52px;font-size:22px;background:var(--accent);color:#fff;box-shadow:0 4px 16px var(--shadow)}"
            + "#modeBtn{font-size:12px!important}#modeBtn.active{background:var(--accent2)!important;color:#fff}"
            + "#volumeWrap{display:flex;align-items:center;gap:6px;padding:0 16px;flex-shrink:0}"
            + "#volumeWrap input{flex:1;height:3px;-webkit-appearance:none;background:var(--card);border-radius:2px;outline:none}"
            + "#volumeWrap input::-webkit-slider-thumb{-webkit-appearance:none;width:14px;height:14px;border-radius:50%;background:var(--accent);cursor:pointer}"
            + "#volumeIcon{font-size:14px;color:var(--text2);min-width:20px}"
            + "#playlist{flex:1;overflow-y:auto;padding:0 12px 12px;margin-top:2px}"
            + "#playlist h3{font-size:11px;color:var(--text2);padding:6px 4px;font-weight:bold;text-transform:uppercase;letter-spacing:1px}"
            + ".song{padding:8px 12px;margin:2px 0;background:var(--card);border-radius:8px;cursor:pointer;display:flex;align-items:center;gap:10px;transition:.2s;border:1px solid transparent}"
            + ".song:hover{background:var(--surface);border-color:var(--border)}.song.active{background:var(--surface);border-color:var(--accent)}"
            + ".song .num{font-size:11px;color:var(--text2);width:20px;text-align:center;flex-shrink:0}"
            + ".song .info{flex:1;min-width:0}.song .info .name{font-size:13px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}"
            + ".song .info .dur{font-size:10px;color:var(--text2);margin-top:1px}"
            + ".song .eq{display:none;gap:2px;align-items:flex-end;height:16px;flex-shrink:0;margin-left:auto}"
            + ".song.active .eq{display:flex}.eq span{width:3px;background:var(--accent);border-radius:2px;animation:eq .6s ease-in-out infinite alternate}"
            + ".eq span:nth-child(1){height:6px;animation-delay:0s}.eq span:nth-child(2){height:12px;animation-delay:.2s}.eq span:nth-child(3){height:8px;animation-delay:.4s}"
            + "@keyframes eq{0%{height:3px}100%{height:var(--h)}}"
            + "</style></head><body>"
            + "<div id=\"header\"><h1>Music Player</h1><p id=\"songCount\">0 songs</p>"
            + "<button id=\"themeBtn\" onclick=\"toggleTheme()\">\u263e</button></div>"
            + "<div id=\"playerSection\"><div id=\"albumArt\">\u266b</div></div>"
            + "<div id=\"songInfo\"><h2 id=\"songTitle\">No song</h2><p id=\"artistName\">Tap a song to play</p></div>"
            + "<div id=\"progressWrap\"><div id=\"progressBarBg\"><div id=\"progressBarFill\"></div></div></div>"
            + "<div id=\"timeRow\"><span id=\"curTime\">0:00</span><span id=\"totTime\">0:00</span></div>"
            + "<div id=\"controls\">"
            + "<button onclick=\"prevSong()\" title=\"Previous\">\u23ee</button>"
            + "<button id=\"playBtn\" onclick=\"togglePlay()\">\u25b6</button>"
            + "<button onclick=\"nextSong()\" title=\"Next\">\u23ed</button>"
            + "<button id=\"modeBtn\" onclick=\"toggleMode()\" title=\"Repeat\">\u21bb</button>"
            + "</div>"
            + "<div id=\"volumeWrap\"><span id=\"volumeIcon\">\ud83d\udd0a</span><input id=\"volumeSlider\" type=\"range\" min=\"0\" max=\"1\" step=\"0.05\" value=\"0.8\"></div>"
            + "<div id=\"playlist\"><h3>\u266b Playlist</h3></div>"
            + "<script>var songs=" + json.toString() + ",current=-1,audio=null,mode='all';"
            + "function formatTime(s){s=Math.floor(s||0);var m=Math.floor(s/60);return m+':'+(s%60<10?'0':'')+(s%60)}"
            + "function loadSongs(){var list=document.getElementById('playlist');list.innerHTML='<h3>\u266b Playlist ('+songs.length+')</h3>';"
            + "document.getElementById('songCount').textContent=songs.length+' songs';"
            + "songs.forEach(function(s,i){var d=document.createElement('div');d.className='song';"
            + "d.innerHTML='<span class=\"num\">'+(i+1)+'</span>'"
            + "+'<div class=\"info\"><div class=\"name\">'+s.name+'</div><div class=\"dur\">'+formatTime(s.duration||0)+'</div></div>'"
            + "'<div class=\"eq\"><span></span><span></span><span></span></div>';"
            + "d.onclick=function(){playSong(i)};list.appendChild(d)})}"
            + "function playSong(i){if(i<0||i>=songs.length)return;current=i;if(audio){audio.pause();audio.remove()}"
            + "audio=new Audio(songs[i].file);audio.volume=parseFloat(document.getElementById('volumeSlider').value);"
            + "audio.play();updateUI();audio.ontimeupdate=updateProgress;"
            + "audio.onended=function(){if(mode=='one'){playSong(current)}else if(mode=='shuffle'){"
            + "var n;do{n=Math.floor(Math.random()*songs.length)}while(n===current&&songs.length>1);playSong(n)}else{nextSong()}};"
            + "window.audio=audio}"
            + "function updateUI(){document.querySelectorAll('.song').forEach(function(e,i){e.classList.toggle('active',i===current)});"
            + "var s=songs[current];if(s){document.getElementById('songTitle').textContent=s.name;"
            + "document.getElementById('artistName').textContent='Now Playing';"
            + "document.getElementById('albumArt').classList.add('pulse');"
            + "document.getElementById('playBtn').textContent='\u23f8'}else{"
            + "document.getElementById('songTitle').textContent='No song';"
            + "document.getElementById('artistName').textContent='Tap a song';"
            + "document.getElementById('albumArt').classList.remove('pulse');"
            + "document.getElementById('playBtn').textContent='\u25b6'}}"
            + "function updateProgress(){if(!audio||!audio.duration)return;var c=audio.currentTime,d=audio.duration;"
            + "document.getElementById('progressBarFill').style.width=(c/d*100)+'%';"
            + "document.getElementById('curTime').textContent=formatTime(c);"
            + "document.getElementById('totTime').textContent=formatTime(d)}"
            + "function togglePlay(){if(!audio)return;if(audio.paused)audio.play();else audio.pause();updateUI()}"
            + "function nextSong(){if(songs.length===0)return;if(current<songs.length-1)playSong(current+1);else playSong(0)}"
            + "function prevSong(){if(songs.length===0)return;if(current>0)playSong(current-1);else playSong(songs.length-1)}"
            + "function toggleMode(){var modes=['all','one','shuffle'];var labels=['\u21bb','\u2460','\u267b'];"
            + "var idx=modes.indexOf(mode);mode=modes[(idx+1)%3];var btn=document.getElementById('modeBtn');"
            + "btn.textContent=labels[(idx+1)%3];if(mode!=='all')btn.classList.add('active');else btn.classList.remove('active')}"
            + "function toggleTheme(){document.body.classList.toggle('light')}"
            + "document.getElementById('progressBarBg').onclick=function(e){if(!audio)return;"
            + "var rect=this.getBoundingClientRect();var pct=(e.clientX-rect.left)/rect.width;"
            + "audio.currentTime=pct*audio.duration};"
            + "document.getElementById('volumeSlider').oninput=function(){if(audio)audio.volume=parseFloat(this.value)};"
            + "loadSongs();updateUI();"
            + "</script></body></html>";
        Files.write(Paths.get(workDir + "\\assets\\_main_.html"), html.getBytes(StandardCharsets.UTF_8));
    }

    private void scanMp3Files(File dir, String prefix, List<String> result) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            String name = prefix.isEmpty() ? f.getName() : prefix + "/" + f.getName();
            if (f.isDirectory()) {
                scanMp3Files(f, name, result);
            } else if (f.getName().toLowerCase().endsWith(".mp3")) {
                result.add(name);
            }
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private void extractZip(String zipPath, String outputDir) throws Exception {
        java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(
            new java.io.BufferedInputStream(new java.io.FileInputStream(zipPath)));
        byte[] buffer = new byte[8192];
        java.util.zip.ZipEntry entry;
        File outputFolder = new File(outputDir);
        outputFolder.mkdirs();
        while ((entry = zis.getNextEntry()) != null) {
            File newFile = new File(outputFolder, entry.getName());
            if (entry.isDirectory()) { newFile.mkdirs(); }
            else {
                newFile.getParentFile().mkdirs();
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(newFile)) {
                    int len; while ((len = zis.read(buffer)) > 0) fos.write(buffer, 0, len);
                }
            }
            zis.closeEntry();
        }
        zis.close();
    }
}
