package com.eagle.tools;

import com.eagle.util.ThemeManager;
import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.*;

public class ExportFlutter {

    private static final Pattern TAG_PATTERN = Pattern.compile("<(\\w+)([^>]*)>([\\s\\S]*?)</\\1>", Pattern.DOTALL);
    private static final Pattern SELF_CLOSING = Pattern.compile("<(\\w+)([^>]*?)/>", Pattern.DOTALL);
    private static final Pattern STYLE_ATTR = Pattern.compile("style\\s*=\\s*\"([^\"]*)\"");
    private static final Pattern CLASS_ATTR = Pattern.compile("class\\s*=\\s*\"([^\"]*)\"");
    private static final Pattern ID_ATTR = Pattern.compile("id\\s*=\\s*\"([^\"]*)\"");

    public static void show(Window owner) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.setTitle("Export to Flutter");

        Label desc = new Label("Paste HTML to convert to Flutter widgets. Supports: div, span, h1-h6, p, a, img, ul/ol, table, form, input, button, section, header, footer, nav, aside, video, iframe, svg, select, textarea, label, blockquote, pre, code, figure, figcaption");
        desc.setWrapText(true);

        TextArea htmlInput = new TextArea();
        htmlInput.setPromptText("<div style=\"padding:20px;background:#f0f0f0;\">\n  <h1>Hello</h1>\n  <p>World</p>\n</div>");
        htmlInput.setPrefRowCount(8);
        htmlInput.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px;");

        TextArea dartOutput = new TextArea();
        dartOutput.setEditable(false);
        dartOutput.setPrefRowCount(8);
        dartOutput.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px;");

        Label status = new Label("Ready");
        status.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-muted;");

        SplitPane split = new SplitPane(htmlInput, dartOutput);
        split.setDividerPositions(0.5);
        VBox.setVgrow(split, Priority.ALWAYS);

        Button convertBtn = new Button("Convert to Flutter");
        convertBtn.setStyle("-fx-background-color: #02569b; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        Button copyBtn = new Button("Copy Code");
        copyBtn.setStyle("-fx-background-color: -bg-tertiary; -fx-text-fill: -text-primary; -fx-cursor: hand;");
        copyBtn.setOnAction(ev -> {
            String code = dartOutput.getText();
            if (code != null && !code.isEmpty()) {
                ClipboardContent cc = new ClipboardContent();
                cc.putString(code);
                Clipboard.getSystemClipboard().setContent(cc);
                status.setText("Copied");
            }
        });

        Button exportBtn = new Button("Export .dart File");
        exportBtn.setStyle("-fx-background-color: -bg-tertiary; -fx-text-fill: -text-primary; -fx-cursor: hand;");
        exportBtn.setOnAction(ev -> {
            String code = dartOutput.getText();
            if (code == null || code.isEmpty()) { status.setText("Nothing to export"); return; }
            FileChooser fc = new FileChooser();
            fc.setTitle("Save Dart File");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Dart", "*.dart"));
            File f = fc.showSaveDialog(stage);
            if (f != null) {
                try {
                    java.nio.file.Files.write(f.toPath(), code.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    status.setText("Exported to " + f.getName());
                } catch (Exception ex) {
                    status.setText("Export failed: " + ex.getMessage());
                }
            }
        });

        // Options
        CheckBox statefulCheck = new CheckBox("Generate StatefulWidget");
        CheckBox responsiveCheck = new CheckBox("Responsive (MediaQuery)");
        CheckBox darkModeCheck = new CheckBox("Dark mode support");
        HBox options = new HBox(12, statefulCheck, responsiveCheck, darkModeCheck);

        HBox topBar = new HBox(6, convertBtn, copyBtn, exportBtn);

        convertBtn.setOnAction(e -> {
            String html = htmlInput.getText();
            if (html == null || html.trim().isEmpty()) {
                dartOutput.setText("// Paste HTML code first.");
                return;
            }
            try {
                String flutter = convertHtmlToFlutter(html, statefulCheck.isSelected(), responsiveCheck.isSelected(), darkModeCheck.isSelected());
                dartOutput.setText(flutter);
                status.setText("Converted — " + flutter.split("\n").length + " lines");
            } catch (Exception ex) {
                dartOutput.setText("// Error: " + ex.getMessage());
                status.setText("Conversion failed");
            }
        });

        VBox root = new VBox(6, desc, options, topBar, split, status);
        root.setPadding(new Insets(8));

        Scene scene = new Scene(root, 800, 580);
        ThemeManager.getInstance().applyTheme(scene);
        stage.setScene(scene);
        stage.show();
    }

    static String convertHtmlToFlutter(String html, boolean stateful, boolean responsive, boolean darkMode) {
        html = html.replaceAll("<!--.*?-->", "");
        html = html.replaceAll("\\s+", " ").trim();

        StringBuilder d = new StringBuilder();
        d.append("import 'package:flutter/material.dart';\n\n");

        if (stateful) {
            d.append("class ConvertedWidget extends StatefulWidget {\n");
            d.append("  const ConvertedWidget({super.key});\n\n");
            d.append("  @override\n");
            d.append("  State<ConvertedWidget> createState() => _ConvertedWidgetState();\n");
            d.append("}\n\n");
            d.append("class _ConvertedWidgetState extends State<ConvertedWidget> {\n");
            d.append("  @override\n");
            d.append("  Widget build(BuildContext context) {\n");
        } else {
            d.append("class ConvertedWidget extends StatelessWidget {\n");
            d.append("  const ConvertedWidget({super.key});\n\n");
            d.append("  @override\n");
            d.append("  Widget build(BuildContext context) {\n");
        }

        if (responsive) {
            d.append("    final screenWidth = MediaQuery.of(context).size.width;\n");
            d.append("    final isMobile = screenWidth < 600;\n");
        }
        if (darkMode) {
            d.append("    final isDark = Theme.of(context).brightness == Brightness.dark;\n");
        }

        d.append("    return ");
        convertNode(html, d, 0, true);
        d.append(";\n");
        d.append("  }\n");
        d.append("}\n");
        return d.toString();
    }

    private static int convertNode(String html, StringBuilder d, int depth, boolean isRoot) {
        String indent = repeat("  ", depth + 3);
        String in4 = repeat("  ", depth + 4);
        html = html.trim();
        if (html.isEmpty()) {
            d.append("SizedBox.shrink()");
            return 0;
        }

        Matcher selfM = SELF_CLOSING.matcher(html);
        if (selfM.find() && selfM.start() == 0) {
            String tag = selfM.group(1).toLowerCase();
            String attrs = selfM.group(2);
            String remaining = html.substring(selfM.end()).trim();
            convertSelfClosing(tag, attrs, d, depth);
            if (!remaining.isEmpty()) {
                d.append("\n").append(indent);
                convertNode(remaining, d, depth, false);
            }
            return 0;
        }

        Matcher tagM = TAG_PATTERN.matcher(html);
        if (tagM.find() && tagM.start() == 0) {
            String tag = tagM.group(1).toLowerCase();
            String attrs = tagM.group(2);
            String inner = tagM.group(3).trim();
            String remaining = html.substring(tagM.end()).trim();
            String styles = parseStyles(attrs);
            String cls = parseAttr(attrs, CLASS_ATTR);
            String id = parseAttr(attrs, ID_ATTR);

            switch (tag) {
                case "html":
                case "body":
                case "div":
                case "section":
                case "header":
                case "footer":
                case "main":
                case "article":
                case "aside":
                case "nav":
                    List<String> children = splitChildren(inner);
                    if (children.isEmpty()) {
                        d.append("const SizedBox.shrink()");
                    } else if (children.size() == 1) {
                        writeContainerOpen(d, depth, styles, cls, id);
                        d.append("\n").append(in4).append("child: ");
                        convertNode(children.get(0), d, depth + 1, false);
                        d.append(",");
                        d.append("\n").append(indent).append(")");
                    } else {
                        writeContainerOpen(d, depth, styles, cls, id);
                        d.append("\n").append(in4).append("child: Column(\n").append(in4).append("  children: [\n");
                        for (String child : children) {
                            d.append(in4).append("    ");
                            convertNode(child, d, depth + 2, false);
                            d.append(",\n");
                        }
                        d.append(in4).append("  ],\n").append(in4).append("),\n").append(indent).append(")");
                    }
                    if (!remaining.isEmpty()) d.append("\n").append(indent);
                    break;

                case "span":
                    d.append("Text(\n").append(in4).append("'").append(escapeDart(inner)).append("',\n").append(in4).append("style: TextStyle(fontSize: 14, color: Colors.black87),\n").append(indent).append(")");
                    break;

                case "h1": case "h2": case "h3": case "h4": case "h5": case "h6":
                    int level = Integer.parseInt(tag.substring(1));
                    double size = 32 - (level - 1) * 4;
                    d.append("Text(\n").append(in4).append("'").append(escapeDart(inner)).append("',\n");
                    d.append(in4).append("style: TextStyle(\n").append(in4).append("  fontSize: ").append(String.format("%.0f", size)).append(",\n");
                    d.append(in4).append("  fontWeight: FontWeight.bold,\n");
                    d.append(in4).append("  color: Colors.black87,\n");
                    d.append(in4).append("),\n").append(indent).append(")");
                    break;

                case "p":
                    d.append("Text(\n").append(in4).append("'").append(escapeDart(inner)).append("',\n");
                    d.append(in4).append("style: const TextStyle(fontSize: 16, color: Colors.black87),\n").append(indent).append(")");
                    break;

                case "a":
                    String href = parseAttr(attrs, Pattern.compile("href\\s*=\\s*\"([^\"]*)\""));
                    d.append("GestureDetector(\n").append(in4).append("onTap: () => {/* ").append(escapeDart(href)).append(" */},\n");
                    d.append(in4).append("child: Text(\n").append(in4).append("  '").append(escapeDart(inner)).append("',\n");
                    d.append(in4).append("  style: const TextStyle(color: Colors.blue, decoration: TextDecoration.underline),\n");
                    d.append(in4).append("),\n").append(indent).append(")");
                    break;

                case "img":
                    String src = parseAttr(attrs, Pattern.compile("src\\s*=\\s*\"([^\"]*)\""));
                    String alt = parseAttr(attrs, Pattern.compile("alt\\s*=\\s*\"([^\"]*)\""));
                    d.append("Image.network(\n").append(in4).append("'").append(escapeDart(src.isEmpty() ? "https://via.placeholder.com/200" : src)).append("',\n");
                    d.append(in4).append("alt: '").append(escapeDart(alt)).append("',\n");
                    d.append(in4).append("fit: BoxFit.cover,\n").append(indent).append(")");
                    break;

                case "ul":
                case "ol":
                    List<String> items = extractListItems(inner);
                    d.append("Column(\n").append(in4).append("children: [\n");
                    for (String item : items) {
                        d.append(in4).append("  Row(\n").append(in4).append("    children: [\n");
                        d.append(in4).append("      Text('" + ("ul".equals(tag) ? "•" : "1.") + " '),\n");
                        d.append(in4).append("      ");
                        convertNode(item, d, depth + 3, false);
                        d.append(",\n");
                        d.append(in4).append("    ],\n").append(in4).append("  ),\n");
                    }
                    d.append(in4).append("],\n").append(indent).append(")");
                    break;

                case "li":
                    d.append("Text('").append(escapeDart(inner)).append("')");
                    break;

                case "button":
                    d.append("ElevatedButton(\n").append(in4).append("onPressed: () {},\n");
                    d.append(in4).append("child: Text('").append(escapeDart(inner.isEmpty() ? "Button" : inner)).append("'),\n").append(indent).append(")");
                    break;

                case "input": {
                    String type = parseAttr(attrs, Pattern.compile("type\\s*=\\s*\"([^\"]*)\""));
                    if ("checkbox".equals(type) || "radio".equals(type)) {
                        d.append("Checkbox(\n").append(in4).append("value: false,\n");
                        d.append(in4).append("onChanged: (v) {},\n").append(indent).append(")");
                    } else {
                        d.append("TextField(\n").append(in4).append("decoration: InputDecoration(\n");
                        d.append(in4).append("  hintText: '").append(escapeDart(parseAttr(attrs, Pattern.compile("placeholder\\s*=\\s*\"([^\"]*)\"")))).append("',\n");
                        d.append(in4).append("  border: const OutlineInputBorder(),\n");
                        d.append(in4).append("),\n").append(indent).append(")");
                    }
                    break;
                }

                case "textarea":
                    d.append("TextField(\n").append(in4).append("maxLines: 5,\n");
                    d.append(in4).append("decoration: InputDecoration(\n");
                    d.append(in4).append("  hintText: '").append(escapeDart(parseAttr(attrs, Pattern.compile("placeholder\\s*=\\s*\"([^\"]*)\"")))).append("',\n");
                    d.append(in4).append("  border: const OutlineInputBorder(),\n");
                    d.append(in4).append("),\n").append(indent).append(")");
                    break;

                case "select":
                    d.append("DropdownButtonFormField(\n").append(in4).append("items: [],\n");
                    d.append(in4).append("onChanged: (v) {},\n").append(indent).append(")");
                    break;

                case "label":
                    d.append("Text('").append(escapeDart(inner)).append("',\n");
                    d.append(in4).append("style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500),\n").append(indent).append(")");
                    break;

                case "table":
                    convertTable(inner, d, depth);
                    break;

                case "br":
                    d.append("const SizedBox(height: 16)");
                    break;

                case "hr":
                    d.append("const Divider()");
                    break;

                case "strong": case "b":
                    d.append("Text(\n").append(in4).append("'").append(escapeDart(inner)).append("',\n");
                    d.append(in4).append("style: const TextStyle(fontWeight: FontWeight.bold),\n").append(indent).append(")");
                    break;

                case "em": case "i":
                    d.append("Text(\n").append(in4).append("'").append(escapeDart(inner)).append("',\n");
                    d.append(in4).append("style: const TextStyle(fontStyle: FontStyle.italic),\n").append(indent).append(")");
                    break;

                case "u":
                    d.append("Text(\n").append(in4).append("'").append(escapeDart(inner)).append("',\n");
                    d.append(in4).append("style: const TextStyle(decoration: TextDecoration.underline),\n").append(indent).append(")");
                    break;

                case "del": case "s":
                    d.append("Text(\n").append(in4).append("'").append(escapeDart(inner)).append("',\n");
                    d.append(in4).append("style: const TextStyle(decoration: TextDecoration.lineThrough),\n").append(indent).append(")");
                    break;

                case "blockquote":
                    d.append("Container(\n").append(in4).append("padding: const EdgeInsets.all(12),\n");
                    d.append(in4).append("decoration: BoxDecoration(\n");
                    d.append(in4).append("  border: const Border(left: BorderSide(color: Colors.grey, width: 3)),\n");
                    d.append(in4).append("  color: Colors.grey.shade100,\n");
                    d.append(in4).append("),\n");
                    d.append(in4).append("child: Text('").append(escapeDart(inner)).append("',\n");
                    d.append(in4).append("  style: const TextStyle(fontStyle: FontStyle.italic),\n");
                    d.append(in4).append("),\n").append(indent).append(")");
                    break;

                case "pre":
                    d.append("Container(\n").append(in4).append("padding: const EdgeInsets.all(10),\n");
                    d.append(in4).append("decoration: BoxDecoration(\n");
                    d.append(in4).append("  color: Colors.grey.shade900,\n");
                    d.append(in4).append("  borderRadius: BorderRadius.circular(6),\n");
                    d.append(in4).append("),\n");
                    d.append(in4).append("child: SelectableText('").append(escapeDart(inner)).append("',\n");
                    d.append(in4).append("  style: const TextStyle(fontFamily: 'monospace', color: Colors.white, fontSize: 13),\n");
                    d.append(in4).append("),\n").append(indent).append(")");
                    break;

                case "code":
                    d.append("Container(\n").append(in4).append("padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),\n");
                    d.append(in4).append("decoration: BoxDecoration(\n");
                    d.append(in4).append("  color: Colors.grey.shade200,\n");
                    d.append(in4).append("  borderRadius: BorderRadius.circular(4),\n");
                    d.append(in4).append("),\n");
                    d.append(in4).append("child: Text('").append(escapeDart(inner)).append("',\n");
                    d.append(in4).append("  style: const TextStyle(fontFamily: 'monospace', fontSize: 13),\n");
                    d.append(in4).append("),\n").append(indent).append(")");
                    break;

                case "figure":
                case "figcaption":
                    d.append("Text('").append(escapeDart(inner)).append("',\n");
                    d.append(in4).append("style: const TextStyle(fontSize: 12, color: Colors.grey),\n").append(indent).append(")");
                    break;

                case "video":
                case "iframe":
                    String videoSrc = parseAttr(attrs, Pattern.compile("src\\s*=\\s*\"([^\"]*)\""));
                    d.append("Container(\n").append(in4).append("height: 200,\n");
                    d.append(in4).append("decoration: BoxDecoration(\n");
                    d.append(in4).append("  color: Colors.black,\n");
                    d.append(in4).append("  borderRadius: BorderRadius.circular(8),\n");
                    d.append(in4).append("),\n");
                    d.append(in4).append("child: Center(\n");
                    d.append(in4).append("  child: Text('Video: ").append(escapeDart(videoSrc)).append("',\n");
                    d.append(in4).append("    style: const TextStyle(color: Colors.white),\n");
                    d.append(in4).append("  ),\n");
                    d.append(in4).append("),\n").append(indent).append(")");
                    break;

                case "form":
                    d.append("Form(\n").append(in4).append("child: Column(\n").append(in4).append("  children: [\n");
                    List<String> formChildren = splitChildren(inner);
                    for (String child : formChildren) {
                        d.append(in4).append("    ");
                        convertNode(child, d, depth + 3, false);
                        d.append(",\n");
                    }
                    d.append(in4).append("  ],\n").append(in4).append("),\n").append(indent).append(")");
                    break;

                case "canvas":
                case "svg":
                    d.append("CustomPaint(\n").append(in4).append("size: Size(200, 200),\n");
                    d.append(in4).append("painter: _CanvasPainter(),\n").append(indent).append(")");
                    break;

                default:
                    d.append("Text('<!-- ").append(tag).append(" ").append(escapeDart(inner)).append(" -->')");
            }

            if (!remaining.isEmpty()) {
                if (d.charAt(d.length() - 1) != '\n') d.append("\n").append(indent);
                convertNode(remaining, d, depth, false);
            }
            return 0;
        }

        String text = html.trim();
        if (!text.isEmpty()) {
            d.append("Text('").append(escapeDart(text)).append("')");
        }
        return 0;
    }

    private static void convertSelfClosing(String tag, String attrs, StringBuilder d, int depth) {
        String indent = repeat("  ", depth + 3);
        tag = tag.toLowerCase();
        switch (tag) {
            case "br":
                d.append("const SizedBox(height: 16)");
                break;
            case "hr":
                d.append("const Divider()");
                break;
            case "img":
                String src = parseAttr(attrs, Pattern.compile("src\\s*=\\s*\"([^\"]*)\""));
                d.append("Image.network(\n").append(indent).append("  '").append(escapeDart(src.isEmpty() ? "https://via.placeholder.com/200" : src)).append("',\n");
                d.append(indent).append("  fit: BoxFit.cover,\n").append(indent).append(")");
                break;
            case "input":
                d.append("TextField(\n").append(indent).append("  decoration: InputDecoration(\n");
                d.append(indent).append("    hintText: '").append(escapeDart(parseAttr(attrs, Pattern.compile("placeholder\\s*=\\s*\"([^\"]*)\"")))).append("',\n");
                d.append(indent).append("    border: const OutlineInputBorder(),\n").append(indent).append("  ),\n").append(indent).append(")");
                break;
            case "link":
                d.append("const SizedBox.shrink()");
                break;
            default:
                d.append("const SizedBox.shrink()");
        }
    }

    private static void writeContainerOpen(StringBuilder d, int depth, String styles, String cls, String id) {
        String indent = repeat("  ", depth + 3);
        d.append("Container(");
        if (id != null && !id.isEmpty()) {
            d.append("\n").append(indent).append("// id: ").append(id);
        }
        if (cls != null && !cls.isEmpty()) {
            d.append("\n").append(indent).append("// class: ").append(cls);
        }
        if (styles != null && !styles.isEmpty()) {
            Map<String, String> styleMap = parseStyleMap(styles);
            if (styleMap.containsKey("padding") || styleMap.containsKey("margin")
                || styleMap.containsKey("background") || styleMap.containsKey("width")) {
                d.append("\n").append(indent).append("decoration: BoxDecoration(\n");
                if (styleMap.containsKey("background")) {
                    String bg = styleMap.get("background");
                    d.append(indent).append("  color: ").append(cssColorToFlutter(bg)).append(",\n");
                }
                if (styleMap.containsKey("border-radius")) {
                    d.append(indent).append("  borderRadius: BorderRadius.circular(").append(styleMap.get("border-radius").replace("px", "")).append("),\n");
                }
                if (styleMap.containsKey("box-shadow")) {
                    d.append(indent).append("  boxShadow: const [BoxShadow(blurRadius: 4)],\n");
                }
                d.append(indent).append("),\n");
            }
            String p = styleMap.get("padding");
            if (p != null) {
                List<Double> pads = parseCssPadding(p);
                if (pads.size() == 1) {
                    d.append(indent).append("padding: const EdgeInsets.all(").append(String.format("%.0f", pads.get(0))).append("),\n");
                } else if (pads.size() == 2) {
                    d.append(indent).append("padding: const EdgeInsets.symmetric(horizontal: ").append(String.format("%.0f", pads.get(1))).append(", vertical: ").append(String.format("%.0f", pads.get(0))).append("),\n");
                } else if (pads.size() == 4) {
                    d.append(indent).append("padding: const EdgeInsets.only(left: ").append(String.format("%.0f", pads.get(3))).append(", top: ").append(String.format("%.0f", pads.get(0))).append(", right: ").append(String.format("%.0f", pads.get(1))).append(", bottom: ").append(String.format("%.0f", pads.get(2))).append("),\n");
                }
            }
            String m = styleMap.get("margin");
            if (m != null) {
                d.append(indent).append("margin: const EdgeInsets.all(8),\n");
            }
            String w = styleMap.get("width");
            if (w != null) {
                d.append(indent).append("width: ").append(cssSizeToDouble(w)).append(",\n");
            }
            String h = styleMap.get("height");
            if (h != null) {
                d.append(indent).append("height: ").append(cssSizeToDouble(h)).append(",\n");
            }
            String ta = styleMap.get("text-align");
            if (ta != null) {
                d.append(indent).append("alignment: Alignment.").append(textAlignFlutter(ta)).append(",\n");
            }
        }
    }

    private static void convertTable(String inner, StringBuilder d, int depth) {
        String indent = repeat("  ", depth + 3);
        String in4 = repeat("  ", depth + 4);
        d.append("Table(\n").append(in4).append("border: TableBorder.all(),\n");
        d.append(in4).append("children: [\n");
        Matcher trM = Pattern.compile("<tr[^>]*>([\\s\\S]*?)</tr>", Pattern.DOTALL).matcher(inner);
        while (trM.find()) {
            String row = trM.group(1);
            d.append(in4).append("  TableRow(\n").append(in4).append("    children: [\n");
            Matcher tdM = Pattern.compile("<t[dh][^>]*>([\\s\\S]*?)</t[dh]>", Pattern.DOTALL).matcher(row);
            while (tdM.find()) {
                String cell = tdM.group(1).trim();
                d.append(in4).append("      ");
                convertNode(cell, d, depth + 4, false);
                d.append(",\n");
            }
            d.append(in4).append("    ],\n").append(in4).append("  ),\n");
        }
        d.append(in4).append("],\n").append(indent).append(")");
    }

    // ── HTML Parsing Helpers ──

    private static List<String> splitChildren(String inner) {
        List<String> children = new ArrayList<>();
        int i = 0;
        while (i < inner.length()) {
            if (inner.charAt(i) == '<') {
                int end = findTagEnd(inner, i);
                if (end > i) {
                    String tag = inner.substring(i, end + 1).trim();
                    if (!tag.isEmpty()) children.add(tag);
                    i = end + 1;
                } else {
                    i++;
                }
            } else {
                int next = inner.indexOf('<', i);
                if (next < 0) {
                    String text = inner.substring(i).trim();
                    if (!text.isEmpty()) children.add(text);
                    break;
                } else {
                    String text = inner.substring(i, next).trim();
                    if (!text.isEmpty()) children.add(text);
                    i = next;
                }
            }
        }
        return children;
    }

    private static int findTagEnd(String s, int start) {
        if (s.charAt(start) != '<') return -1;
        int close = s.indexOf("/>", start);
        if (close > start && !s.substring(start + 1, close).contains(">")) return close + 1;
        int gt = s.indexOf('>', start);
        if (gt < 0) return -1;
        String tagName = s.substring(start + 1, gt).split("\\s+", 2)[0].trim();
        if (tagName.isEmpty()) return -1;
        String endTag = "</" + tagName + ">";
        int end = s.indexOf(endTag, gt + 1);
        return end >= 0 ? end + endTag.length() - 1 : gt;
    }

    private static List<String> extractListItems(String inner) {
        List<String> items = new ArrayList<>();
        Matcher liM = Pattern.compile("<li[^>]*>([\\s\\S]*?)</li>", Pattern.DOTALL).matcher(inner);
        while (liM.find()) {
            items.add(liM.group(1).trim());
        }
        return items;
    }

    private static String stripTags(String html) {
        return html.replaceAll("<[^>]+>", "").trim();
    }

    private static String parseAttr(String attrs, Pattern pattern) {
        Matcher m = pattern.matcher(attrs);
        return m.find() ? m.group(1) : "";
    }

    private static String parseStyles(String attrs) {
        Matcher m = STYLE_ATTR.matcher(attrs);
        return m.find() ? m.group(1) : "";
    }

    private static Map<String, String> parseStyleMap(String styles) {
        Map<String, String> map = new LinkedHashMap<>();
        if (styles == null || styles.isEmpty()) return map;
        for (String part : styles.split(";")) {
            part = part.trim();
            if (part.isEmpty()) continue;
            String[] kv = part.split(":", 2);
            if (kv.length == 2) {
                map.put(kv[0].trim().toLowerCase(), kv[1].trim());
            }
        }
        return map;
    }

    private static List<Double> parseCssPadding(String padding) {
        List<Double> vals = new ArrayList<>();
        for (String s : padding.split("\\s+")) {
            try {
                vals.add(Double.parseDouble(s.replace("px", "").trim()));
            } catch (NumberFormatException ignored) {}
        }
        return vals;
    }

    private static double cssSizeToDouble(String size) {
        try {
            return Double.parseDouble(size.replace("px", "").replace("%", "").trim());
        } catch (NumberFormatException e) {
            return 200;
        }
    }

    private static String cssColorToFlutter(String cssColor) {
        cssColor = cssColor.trim().toLowerCase();
        if (cssColor.startsWith("#")) {
            String hex = cssColor.substring(1);
            if (hex.length() == 3) hex = "" + hex.charAt(0) + hex.charAt(0) + hex.charAt(1) + hex.charAt(1) + hex.charAt(2) + hex.charAt(2);
            return "const Color(0xFF" + hex + ")";
        } else if (cssColor.startsWith("rgb")) {
            return "Colors.blueGrey.shade100";
        }
        switch (cssColor) {
            case "red": return "Colors.red";
            case "blue": return "Colors.blue";
            case "green": return "Colors.green";
            case "black": return "Colors.black";
            case "white": return "Colors.white";
            case "gray": case "grey": return "Colors.grey";
            case "transparent": return "Colors.transparent";
            case "yellow": return "Colors.yellow";
            case "orange": return "Colors.orange";
            case "purple": return "Colors.purple";
            case "pink": return "Colors.pink";
            case "teal": return "Colors.teal";
            case "cyan": return "Colors.cyan";
            case "indigo": return "Colors.indigo";
            case "amber": return "Colors.amber";
            case "lime": return "Colors.lime";
            default: return "Colors.blueGrey.shade50";
        }
    }

    private static String textAlignFlutter(String align) {
        switch (align) {
            case "left": return "centerLeft";
            case "right": return "centerRight";
            case "center": return "center";
            default: return "topLeft";
        }
    }

    private static String escapeDart(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "");
    }

    private static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }
}
