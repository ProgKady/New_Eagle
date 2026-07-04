package com.eagle.tools;

import com.eagle.util.ThemeManager;
import com.google.gson.*;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.*;

public class FigmaToHtml {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void show(Window owner) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.setTitle("Figma to HTML Converter");

        Label desc = new Label("Paste Figma JSON, drag a .json/.fig file, paste a Figma URL, or load from file.");
        desc.setWrapText(true);

        TextArea input = new TextArea();
        input.setPromptText("{\n  \"document\": {\n    \"children\": [{\n      \"type\": \"FRAME\",\n      ...\n    }]\n  }\n}");
        input.setPrefRowCount(8);
        input.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px;");

        // Drag-drop support
        Label dropZone = new Label("📁 Drop Figma JSON file here");
        dropZone.setStyle("-fx-background-color: -bg-tertiary; -fx-border-color: -accent; -fx-border-style: dashed; -fx-border-radius: 8; -fx-padding: 12; -fx-alignment: center; -fx-font-size: 12px;");
        dropZone.setAlignment(Pos.CENTER);
        dropZone.setMaxWidth(Double.MAX_VALUE);
        dropZone.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) e.acceptTransferModes(TransferMode.COPY);
        });
        dropZone.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasFiles()) {
                File f = db.getFiles().get(0);
                try {
                    String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                    // Pretty-print JSON
                    JsonElement el = new JsonParser().parse(content);
                    input.setText(GSON.toJson(el));
                } catch (Exception ex) {
                    input.setText("// Error reading file: " + ex.getMessage());
                }
            }
        });

        TextArea output = new TextArea();
        output.setEditable(false);
        output.setPrefRowCount(8);
        output.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px;");

        Label status = new Label("Ready");
        status.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-muted;");

        SplitPane split = new SplitPane(input, output);
        split.setDividerPositions(0.45);
        VBox.setVgrow(split, Priority.ALWAYS);

        Button convertBtn = new Button("Convert to HTML");
        convertBtn.setStyle("-fx-background-color: #4a90d9; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        Button copyBtn = new Button("Copy HTML");
        copyBtn.setStyle("-fx-background-color: -bg-tertiary; -fx-text-fill: -text-primary; -fx-cursor: hand;");
        copyBtn.setOnAction(ev -> {
            String html = output.getText();
            if (html != null && !html.isEmpty()) {
                javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
                cc.putString(html);
                javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
                status.setText("Copied to clipboard");
            }
        });

        Button exportBtn = new Button("Export HTML File");
        exportBtn.setStyle("-fx-background-color: -bg-tertiary; -fx-text-fill: -text-primary; -fx-cursor: hand;");
        exportBtn.setOnAction(ev -> {
            String html = output.getText();
            if (html == null || html.isEmpty()) { status.setText("Nothing to export"); return; }
            FileChooser fc = new FileChooser();
            fc.setTitle("Save HTML File");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML", "*.html"));
            File f = fc.showSaveDialog(stage);
            if (f != null) {
                try {
                    Files.write(f.toPath(), html.getBytes(StandardCharsets.UTF_8));
                    status.setText("Exported to " + f.getName());
                } catch (Exception ex) {
                    status.setText("Export failed: " + ex.getMessage());
                }
            }
        });

        Button loadFileBtn = new Button("Load JSON File");
        loadFileBtn.setStyle("-fx-background-color: -bg-tertiary; -fx-text-fill: -text-primary; -fx-cursor: hand;");
        loadFileBtn.setOnAction(ev -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Open Figma JSON");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json", "*.fig"));
            File f = fc.showOpenDialog(stage);
            if (f != null) {
                try {
                    String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                    JsonElement el = new JsonParser().parse(content);
                    input.setText(GSON.toJson(el));
                    status.setText("Loaded " + f.getName());
                } catch (Exception ex) {
                    status.setText("Error: " + ex.getMessage());
                }
            }
        });

        Button loadUrlBtn = new Button("Load from URL");
        loadUrlBtn.setStyle("-fx-background-color: -bg-tertiary; -fx-text-fill: -text-primary; -fx-cursor: hand;");
        loadUrlBtn.setOnAction(ev -> {
            TextInputDialog td = new TextInputDialog("https://api.figma.com/v1/files/YOUR_FILE_KEY");
            td.setTitle("Figma File URL");
            td.setHeaderText("Enter Figma file URL or API endpoint:");
            td.showAndWait().ifPresent(url -> {
                status.setText("Loading Figma file...");
                new Thread(() -> {
                    try {
                        StringBuilder sb = new StringBuilder();
                        URL u = new URL(url);
                        try (BufferedReader br = new BufferedReader(new InputStreamReader(u.openStream(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = br.readLine()) != null) sb.append(line).append("\n");
                        }
                        JsonElement el = new JsonParser().parse(sb.toString());
                        String pretty = GSON.toJson(el);
                        javafx.application.Platform.runLater(() -> {
                            input.setText(pretty);
                            status.setText("Loaded Figma file. Click Convert.");
                        });
                    } catch (Exception e) {
                        javafx.application.Platform.runLater(() -> status.setText("Load failed: " + e.getMessage()));
                    }
                }).start();
            });
        });

        Button previewBtn = new Button("Preview HTML");
        previewBtn.setStyle("-fx-background-color: -accent; -fx-text-fill: white; -fx-cursor: hand;");
        previewBtn.setOnAction(ev -> {
            String html = output.getText();
            if (html == null || html.isEmpty()) { status.setText("Nothing to preview"); return; }
            // Open in default browser
            try {
                File tmp = File.createTempFile("figma_preview_", ".html");
                tmp.deleteOnExit();
                Files.write(tmp.toPath(), html.getBytes(StandardCharsets.UTF_8));
                java.awt.Desktop.getDesktop().browse(tmp.toURI());
                status.setText("Preview opened in browser");
            } catch (Exception ex) {
                status.setText("Preview failed: " + ex.getMessage());
            }
        });

        HBox topBar = new HBox(6, convertBtn, copyBtn, exportBtn, previewBtn, loadFileBtn, loadUrlBtn);
        //topBar.setWrapWidth(true);
        

        convertBtn.setOnAction(e -> {
            String json = input.getText();
            if (json == null || json.trim().isEmpty()) {
                output.setText("Please paste Figma JSON first.");
                return;
            }
            status.setText("Converting...");
            new Thread(() -> {
                try {
                    String html = convertFigJson(json);
                    javafx.application.Platform.runLater(() -> {
                        output.setText(html);
                        status.setText("Conversion complete — " + html.split("\n").length + " lines");
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        output.setText("Error: " + ex.getMessage());
                        status.setText("Conversion failed");
                    });
                }
            }).start();
        });

        VBox root = new VBox(6, desc, dropZone, topBar, split, status);
        root.setPadding(new Insets(8));

        Scene scene = new Scene(root, 850, 620);
        ThemeManager.getInstance().applyTheme(scene);
        stage.setScene(scene);
        stage.show();
    }

    static String convertFigJson(String json) {
        JsonObject root = new JsonParser().parse(json).getAsJsonObject();
        JsonObject document = root;
        if (root.has("document")) {
            document = root.getAsJsonObject("document");
        }
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("<title>Figma Export</title>\n");
        html.append("<style>\n");
        html.append("* { margin: 0; padding: 0; box-sizing: border-box; }\n");
        html.append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f5f5f5; display: flex; min-height: 100vh; }\n");
        writeCssForNode(document, html, 0, true);
        html.append("</style>\n</head>\n<body>\n");
        if (document.has("children")) {
            JsonArray children = document.getAsJsonArray("children");
            for (int i = 0; i < children.size(); i++) {
                renderNode(children.get(i).getAsJsonObject(), html, 1);
            }
        }
        html.append("</body>\n</html>");
        return html.toString();
    }

    private static void renderNode(JsonObject node, StringBuilder html, int depth) {
        String type = getString(node, "type", "FRAME");
        String name = getString(node, "name", "");
        String indent = repeat("  ", depth);

        switch (type) {
            case "FRAME":
            case "GROUP":
            case "COMPONENT":
            case "INSTANCE":
                renderFrame(node, html, depth, name);
                break;
            case "TEXT":
                renderText(node, html, depth);
                break;
            case "RECTANGLE":
                renderRectangle(node, html, depth);
                break;
            case "ELLIPSE":
                renderEllipse(node, html, depth);
                break;
            case "LINE":
                renderLine(node, html, depth);
                break;
            case "VECTOR":
            case "BOOLEAN_OPERATION":
            case "STAR":
            case "REGULAR_POLYGON":
                html.append(indent).append("<!-- ").append(type).append(": ").append(escapeHtml(name)).append(" -->\n");
                renderFrame(node, html, depth, name);
                break;
            default:
                html.append(indent).append("<!-- Unknown node type: ").append(type).append(" -->\n");
        }
    }

    private static void renderFrame(JsonObject node, StringBuilder html, int depth, String name) {
        String indent = repeat("  ", depth);
        String cls = "frame-" + sanitizeClass(name);
        boolean hasChildren = node.has("children") && node.getAsJsonArray("children").size() > 0;
        String layoutMode = getString(node, "layoutMode", "NONE");
        String flexDir = "NONE".equals(layoutMode) ? "" :
            ("HORIZONTAL".equals(layoutMode) ? "row" : "column");

        html.append(indent).append("<div class=\"").append(cls).append("\">\n");
        if (hasChildren) {
            JsonArray children = node.getAsJsonArray("children");
            for (int i = 0; i < children.size(); i++) {
                renderNode(children.get(i).getAsJsonObject(), html, depth + 1);
            }
        } else if (name != null && !name.isEmpty()) {
            html.append(indent).append("  <!-- ").append(escapeHtml(name)).append(" -->\n");
        }
        html.append(indent).append("</div>\n");
    }

    private static void renderText(JsonObject node, StringBuilder html, int depth) {
        String indent = repeat("  ", depth);
        String characters = getString(node, "characters", "");
        String cls = "text-" + sanitizeClass(getString(node, "name", "text"));

        if (characters.isEmpty()) {
            html.append(indent).append("<!-- Empty text node: ").append(escapeHtml(getString(node, "name", ""))).append(" -->\n");
            return;
        }

        JsonObject style = node.has("style") ? node.getAsJsonObject("style") : null;
        boolean isHeading = style != null && "bold".equals(getString(style, "fontWeight", ""))
            && characters.length() < 80;
        String tag = isHeading ? "h2" : "p";

        html.append(indent).append("<").append(tag).append(" class=\"").append(cls).append("\">");
        html.append(escapeHtml(characters));
        html.append("</").append(tag).append(">\n");
    }

    private static void renderRectangle(JsonObject node, StringBuilder html, int depth) {
        String indent = repeat("  ", depth);
        String name = getString(node, "name", "rect");
        String cls = "rect-" + sanitizeClass(name);
        JsonObject box = node.has("absoluteBoundingBox") ? node.getAsJsonObject("absoluteBoundingBox") : null;

        boolean hasImageFill = false;
        if (node.has("fills")) {
            JsonArray fills = node.getAsJsonArray("fills");
            for (int i = 0; i < fills.size(); i++) {
                JsonObject fill = fills.get(i).getAsJsonObject();
                if ("IMAGE".equals(getString(fill, "type", ""))) {
                    hasImageFill = true;
                    break;
                }
            }
        }

        if (hasImageFill) {
            html.append(indent).append("<div class=\"").append(cls).append("\" role=\"img\" aria-label=\"").append(escapeHtml(name)).append("\"></div>\n");
        } else if (name.toLowerCase().contains("divider") || name.toLowerCase().contains("separator")) {
            html.append(indent).append("<hr class=\"").append(cls).append("\">\n");
        } else if (BoxContainsText(name)) {
            html.append(indent).append("<div class=\"").append(cls).append("\">").append(escapeHtml(name)).append("</div>\n");
        } else {
            html.append(indent).append("<div class=\"").append(cls).append("\"></div>\n");
        }
    }

    private static void renderEllipse(JsonObject node, StringBuilder html, int depth) {
        String indent = repeat("  ", depth);
        String cls = "ellipse-" + sanitizeClass(getString(node, "name", "ellipse"));
        html.append(indent).append("<div class=\"").append(cls).append("\"></div>\n");
    }

    private static void renderLine(JsonObject node, StringBuilder html, int depth) {
        String indent = repeat("  ", depth);
        String cls = "line-" + sanitizeClass(getString(node, "name", "line"));
        html.append(indent).append("<hr class=\"").append(cls).append("\">\n");
    }

    private static void writeCssForNode(JsonObject node, StringBuilder css, int depth, boolean isTop) {
        if (!node.has("children") && !"CANVAS".equals(getString(node, "type", ""))) {
            writeCssForSingle(node, css);
            return;
        }
        if (node.has("children")) {
            JsonArray children = node.getAsJsonArray("children");
            for (int i = 0; i < children.size(); i++) {
                writeCssForSingle(children.get(i).getAsJsonObject(), css);
                if (children.get(i).getAsJsonObject().has("children")) {
                    writeCssForNode(children.get(i).getAsJsonObject(), css, depth + 1, false);
                }
            }
        }
    }

    private static void writeCssForSingle(JsonObject node, StringBuilder css) {
        String type = getString(node, "type", "FRAME");
        String name = getString(node, "name", "");
        JsonObject box = node.has("absoluteBoundingBox") ? node.getAsJsonObject("absoluteBoundingBox") : null;
        if (box == null) return;

        double x = getDouble(box, "x", 0);
        double y = getDouble(box, "y", 0);
        double w = getDouble(box, "width", 0);
        double h = getDouble(box, "height", 0);

        String cls;
        if ("FRAME".equals(type) || "GROUP".equals(type) || "COMPONENT".equals(type) || "INSTANCE".equals(type)) {
            cls = "frame-" + sanitizeClass(name);
        } else if ("TEXT".equals(type)) {
            cls = "text-" + sanitizeClass(name.isEmpty() ? "text" : name);
        } else if ("RECTANGLE".equals(type)) {
            cls = "rect-" + sanitizeClass(name.isEmpty() ? "rect" : name);
        } else if ("ELLIPSE".equals(type)) {
            cls = "ellipse-" + sanitizeClass(name.isEmpty() ? "ellipse" : name);
        } else if ("LINE".equals(type)) {
            cls = "line-" + sanitizeClass(name.isEmpty() ? "line" : name);
        } else {
            cls = sanitizeClass(type + "-" + name);
        }

        css.append(".").append(cls).append(" {\n");

        // Position
        css.append("  position: absolute;\n  left: ").append(x).append("px;\n  top: ").append(y).append("px;\n");
        if (w > 0) css.append("  width: ").append(w).append("px;\n");
        if (h > 0) css.append("  height: ").append(h).append("px;\n");

        // Flex layouts
        String layoutMode = getString(node, "layoutMode", "NONE");
        if ("HORIZONTAL".equals(layoutMode)) {
            css.append("  display: flex;\n  flex-direction: row;\n");
            String align = getString(node, "primaryAxisAlignItems", "MIN");
            css.append("  align-items: ").append(flexAlign(align)).append(";\n");
            String cross = getString(node, "counterAxisAlignItems", "MIN");
            css.append("  justify-content: ").append(flexAlign(cross)).append(";\n");
            double gap = getDouble(node, "itemSpacing", 0);
            if (gap > 0) css.append("  gap: ").append(gap).append("px;\n");
        } else if ("VERTICAL".equals(layoutMode)) {
            css.append("  display: flex;\n  flex-direction: column;\n");
            String align = getString(node, "primaryAxisAlignItems", "MIN");
            css.append("  align-items: ").append(flexAlign(align)).append(";\n");
            String cross = getString(node, "counterAxisAlignItems", "MIN");
            css.append("  justify-content: ").append(flexAlign(cross)).append(";\n");
            double gap = getDouble(node, "itemSpacing", 0);
            if (gap > 0) css.append("  gap: ").append(gap).append("px;\n");
        }

        // Padding
        double pl = getDouble(node, "paddingLeft", 0);
        double pr = getDouble(node, "paddingRight", 0);
        double pt = getDouble(node, "paddingTop", 0);
        double pb = getDouble(node, "paddingBottom", 0);
        if (pl > 0 || pr > 0 || pt > 0 || pb > 0) {
            css.append("  padding: ").append(pt).append("px ").append(pr).append("px ").append(pb).append("px ").append(pl).append("px;\n");
        }

        // Corner radius
        double cr = getDouble(node, "cornerRadius", 0);
        if (cr > 0) css.append("  border-radius: ").append(cr).append("px;\n");

        // Opacity
        double opacity = getDouble(node, "opacity", 1);
        if (opacity < 1) css.append("  opacity: ").append(opacity).append(";\n");

        // Fills
        if (node.has("fills")) {
            JsonArray fills = node.getAsJsonArray("fills");
            for (int i = 0; i < fills.size(); i++) {
                JsonObject fill = fills.get(i).getAsJsonObject();
                if (!getBoolean(fill, "visible", true)) continue;
                String fillType = getString(fill, "type", "");
                if ("SOLID".equals(fillType) && fill.has("color")) {
                    JsonObject color = fill.getAsJsonObject("color");
                    String rgba = colorToRgba(color, fill);
                    css.append("  background: ").append(rgba).append(";\n");
                    break;
                } else if ("GRADIENT_LINEAR".equals(fillType)) {
                    String grad = linearGradient(fill);
                    if (grad != null) css.append("  background: ").append(grad).append(";\n");
                    break;
                } else if ("GRADIENT_RADIAL".equals(fillType)) {
                    String grad = radialGradient(fill);
                    if (grad != null) css.append("  background: ").append(grad).append(";\n");
                    break;
                } else if ("IMAGE".equals(fillType)) {
                    css.append("  background: #e0e0e0 url('image-fill-").append(sanitizeClass(name)).append("') center/cover no-repeat;\n");
                    css.append("  background-blend-mode: multiply;\n");
                    break;
                }
            }
        }

        // Strokes
        if (node.has("strokes")) {
            JsonArray strokes = node.getAsJsonArray("strokes");
            if (strokes.size() > 0) {
                JsonObject stroke = strokes.get(0).getAsJsonObject();
                if (stroke.has("color")) {
                    JsonObject color = stroke.getAsJsonObject("color");
                    String rgba = colorToRgba(color, stroke);
                    double weight = getDouble(node, "strokeWeight", 1);
                    css.append("  border: ").append(weight).append("px solid ").append(rgba).append(";\n");
                }
            }
        }

        // Effects
        if (node.has("effects")) {
            JsonArray effects = node.getAsJsonArray("effects");
            for (int i = 0; i < effects.size(); i++) {
                JsonObject eff = effects.get(i).getAsJsonObject();
                if (!getBoolean(eff, "visible", true)) continue;
                String effType = getString(eff, "type", "");
                if ("DROP_SHADOW".equals(effType)) {
                    double dx = getDouble(eff, "offsetX", 0);
                    double dy = getDouble(eff, "offsetY", 0);
                    double radius = getDouble(eff, "radius", 4);
                    JsonObject eColor = eff.has("color") ? eff.getAsJsonObject("color") : null;
                    String shadowColor = eColor != null ? colorToRgba(eColor, eff) : "rgba(0,0,0,0.25)";
                    css.append("  box-shadow: ").append(dx).append("px ").append(dy).append("px ").append(radius).append("px ").append(shadowColor).append(";\n");
                } else if ("INNER_SHADOW".equals(effType)) {
                    double dx = getDouble(eff, "offsetX", 0);
                    double dy = getDouble(eff, "offsetY", 0);
                    double radius = getDouble(eff, "radius", 4);
                    css.append("  box-shadow: inset ").append(dx).append("px ").append(dy).append("px ").append(radius).append("px rgba(0,0,0,0.3);\n");
                } else if ("LAYER_BLUR".equals(effType)) {
                    double radius = getDouble(eff, "radius", 4);
                    css.append("  filter: blur(").append(radius).append("px);\n");
                } else if ("BACKGROUND_BLUR".equals(effType)) {
                    double radius = getDouble(eff, "radius", 4);
                    css.append("  backdrop-filter: blur(").append(radius).append("px);\n");
                }
            }
        }

        // Text styles
        if ("TEXT".equals(type) && node.has("style")) {
            JsonObject style = node.getAsJsonObject("style");
            String ff = getString(style, "fontFamily", null);
            if (ff != null && !ff.isEmpty()) css.append("  font-family: '").append(ff).append("', sans-serif;\n");
            double fs = getDouble(style, "fontSize", 16);
            css.append("  font-size: ").append(fs).append("px;\n");
            String fw = getString(style, "fontWeight", "400");
            css.append("  font-weight: ").append(fw).append(";\n");
            double lh = getDouble(style, "lineHeightPx", fs * 1.4);
            css.append("  line-height: ").append(lh).append("px;\n");
            double ls = getDouble(style, "letterSpacing", 0);
            if (ls != 0) css.append("  letter-spacing: ").append(ls).append("px;\n");
            String ta = getString(style, "textAlignHorizontal", "LEFT").toLowerCase();
            css.append("  text-align: ").append(ta).append(";\n");
            // Text color
            if (node.has("fills")) {
                JsonArray fills = node.getAsJsonArray("fills");
                for (int i = 0; i < fills.size(); i++) {
                    JsonObject fill = fills.get(i).getAsJsonObject();
                    if ("SOLID".equals(getString(fill, "type", "")) && fill.has("color")) {
                        css.append("  color: ").append(colorToRgba(fill.getAsJsonObject("color"), fill)).append(";\n");
                        break;
                    }
                }
            }
        }

        css.append("}\n\n");
    }

    // ── Helpers ──

    private static String flexAlign(String figmaAlign) {
        switch (figmaAlign) {
            case "MIN": return "flex-start";
            case "MAX": return "flex-end";
            case "CENTER": return "center";
            case "SPACE_BETWEEN": return "space-between";
            default: return "flex-start";
        }
    }

    private static String colorToRgba(JsonObject color, JsonObject parent) {
        double r = getDouble(color, "r", 0) * 255;
        double g = getDouble(color, "g", 0) * 255;
        double b = getDouble(color, "b", 0) * 255;
        double a = parent.has("opacity") ? getDouble(parent, "opacity", 1) : 1;
        return String.format("rgba(%.0f, %.0f, %.0f, %.2f)", r, g, b, a);
    }

    private static String linearGradient(JsonObject fill) {
        if (!fill.has("gradientStops")) return null;
        JsonArray stops = fill.getAsJsonArray("gradientStops");
        StringBuilder sb = new StringBuilder("linear-gradient(");
        String dir = getString(fill, "gradientAngle", "180deg");
        sb.append(dir);
        for (int i = 0; i < stops.size(); i++) {
            JsonObject stop = stops.get(i).getAsJsonObject();
            double pos = getDouble(stop, "position", 0) * 100;
            JsonObject col = stop.getAsJsonObject("color");
            String rgba = colorToRgba(col, fill);
            sb.append(", ").append(rgba).append(" ").append(String.format("%.0f", pos)).append("%");
        }
        sb.append(")");
        return sb.toString();
    }

    private static String radialGradient(JsonObject fill) {
        if (!fill.has("gradientStops")) return null;
        JsonArray stops = fill.getAsJsonArray("gradientStops");
        StringBuilder sb = new StringBuilder("radial-gradient(circle");
        for (int i = 0; i < stops.size(); i++) {
            JsonObject stop = stops.get(i).getAsJsonObject();
            double pos = getDouble(stop, "position", 0) * 100;
            JsonObject col = stop.getAsJsonObject("color");
            String rgba = colorToRgba(col, fill);
            sb.append(", ").append(rgba).append(" ").append(String.format("%.0f", pos)).append("%");
        }
        sb.append(")");
        return sb.toString();
    }

    private static boolean BoxContainsText(String name) {
        if (name == null || name.isEmpty()) return false;
        return name.matches(".*[a-zA-Z]{3,}.*");
    }

    private static String sanitizeClass(String s) {
        if (s == null || s.isEmpty()) return "node";
        return s.replaceAll("[^a-zA-Z0-9_-]", "-").replaceAll("-+", "-").replaceAll("^-|-$", "").toLowerCase();
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String getString(JsonObject obj, String key, String def) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) return obj.get(key).getAsString();
        return def;
    }

    private static double getDouble(JsonObject obj, String key, double def) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) return obj.get(key).getAsDouble();
        return def;
    }

    private static boolean getBoolean(JsonObject obj, String key, boolean def) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) return obj.get(key).getAsBoolean();
        return def;
    }

    private static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }
}
