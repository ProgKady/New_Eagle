package com.eagle.tools;

import com.eagle.util.ThemeManager;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.web.WebView;
import javafx.stage.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class MarkdownPreview {

    public static void show(File mdFile, Window owner) {
        if (mdFile == null || !mdFile.exists()) return;
        try {
            String content = new String(Files.readAllBytes(mdFile.toPath()), StandardCharsets.UTF_8);
            showHtml(mdFile.getName(), renderToHtml(content), owner);
        } catch (Exception e) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Failed to read: " + e.getMessage());
            a.initOwner(owner); a.showAndWait();
        }
    }

    public static void showHtml(String title, String html, Window owner) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.setTitle("Markdown: " + title);

        WebView web = new WebView();
        web.getEngine().loadContent(html);

        Scene scene = new Scene(web, 700, 500);
        ThemeManager.getInstance().applyTheme(scene);
        stage.setScene(scene);
        stage.show();
    }

    public static String renderToHtml(String md) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='utf-8'>")
            .append("<style>")
            .append("body{font-family:'Segoe UI',sans-serif;max-width:800px;margin:20px auto;padding:0 20px;color:#333;line-height:1.6}")
            .append("h1,h2,h3{border-bottom:1px solid #eee;padding-bottom:6px}")
            .append("code{background:#f4f4f4;padding:2px 5px;border-radius:3px;font-size:0.9em}")
            .append("pre code{display:block;padding:12px;overflow-x:auto}")
            .append("blockquote{border-left:4px solid #ddd;margin:0;padding:0 15px;color:#666}")
            .append("table{border-collapse:collapse;width:100%}")
            .append("th,td{border:1px solid #ddd;padding:8px;text-align:left}")
            .append("th{background:#f4f4f4}")
            .append("img{max-width:100%}")
            .append("</style></head><body>");

        String[] lines = md.split("\n", -1);
        boolean inCode = false;
        boolean inList = false;

        for (String line : lines) {
            if (line.startsWith("```")) {
                if (inCode) { html.append("</code></pre>"); inCode = false; }
                else { html.append("<pre><code>"); inCode = true; }
                continue;
            }
            if (inCode) {
                html.append(escapeHtml(line)).append("\n");
                continue;
            }
            if (line.startsWith("# ")) html.append("<h1>").append(escapeHtml(line.substring(2))).append("</h1>");
            else if (line.startsWith("## ")) html.append("<h2>").append(escapeHtml(line.substring(3))).append("</h2>");
            else if (line.startsWith("### ")) html.append("<h3>").append(escapeHtml(line.substring(4))).append("</h3>");
            else if (line.startsWith("> ")) html.append("<blockquote>").append(escapeHtml(line.substring(2))).append("</blockquote>");
            else if (line.startsWith("- ") || line.startsWith("* ")) {
                if (!inList) { html.append("<ul>"); inList = true; }
                html.append("<li>").append(escapeHtml(line.substring(2))).append("</li>");
            } else {
                if (inList) { html.append("</ul>"); inList = false; }
                if (line.trim().isEmpty()) html.append("<br>");
                else html.append("<p>").append(escapeHtml(line)).append("</p>");
            }
        }
        if (inCode) html.append("</code></pre>");
        if (inList) html.append("</ul>");
        html.append("</body></html>");
        return html.toString();
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
