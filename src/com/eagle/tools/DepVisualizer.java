package com.eagle.tools;

import com.eagle.util.ThemeManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import javafx.stage.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

public class DepVisualizer {

    private static final Color[] TYPE_COLORS = {
        Color.web("#e8f0fe"), // js
        Color.web("#e8fae8"), // ts
        Color.web("#fef3e8"), // java
        Color.web("#fce8f0"), // py
        Color.web("#e8fcfc"), // other
    };
    private static final Color[] STROKE_COLORS = {
        Color.web("#4a90d9"),
        Color.web("#2d8659"),
        Color.web("#d97706"),
        Color.web("#c0392b"),
        Color.web("#666666"),
    };

    public static void show(File projectRoot, Window owner) {
        if (projectRoot == null) return;
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.setTitle("Dependency Visualizer: " + projectRoot.getName());

        Pane canvas = new Pane();
        canvas.setPrefSize(2000, 2000);
        canvas.setStyle("-fx-background-color: #111820;");

        Pane edgeLayer = new Pane();
        Pane nodeLayer = new Pane();
        canvas.getChildren().addAll(edgeLayer, nodeLayer);

        Scale zoomTransform = new Scale(1, 1, 0, 0);
        canvas.getTransforms().add(zoomTransform);

        ScrollPane sp = new ScrollPane(canvas);
        sp.setPrefHeight(500);
        sp.setPannable(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // Top controls
        TextField searchField = new TextField();
        searchField.setPromptText("Search nodes...");
        searchField.setPrefColumnCount(20);

        Button analyzeBtn = new Button("Analyze");
        analyzeBtn.setStyle("-fx-background-color: -accent; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");

        Button resetBtn = new Button("Reset Layout");
        resetBtn.setStyle("-fx-cursor: hand;");

        Slider zoomSlider = new Slider(0.1, 2.0, 1.0);
        zoomSlider.setPrefWidth(120);
        zoomSlider.setShowTickLabels(false);

        Label zoomLabel = new Label("100%");
        zoomLabel.setStyle("-fx-font-size: 11px;");

        Label infoLabel = new Label("Click Analyze to scan dependencies.");
        infoLabel.setStyle("-fx-font-size: 11px;");

        ComboBox<String> fileFilter = new ComboBox<>();
        fileFilter.getItems().addAll("All", ".js", ".ts", ".java", ".py");
        fileFilter.setValue("All");

        HBox topBar = new HBox(6, new Label("Search:"), searchField,
            fileFilter, analyzeBtn, resetBtn,
            new Label("Zoom:"), zoomSlider, zoomLabel, infoLabel);
        topBar.setPadding(new Insets(6));
        topBar.setStyle("-fx-background-color: -bg-primary; -fx-border-color: -border; -fx-border-width: 0 0 1 0;");

        // Legend
        HBox legend = new HBox(10);
        legend.setPadding(new Insets(4, 8, 4, 8));
        legend.setStyle("-fx-background-color: -bg-secondary; -fx-border-color: -border; -fx-border-width: 0 0 1 0;");
        String[] labels = {"JS", "TS", "Java", "Python", "Other"};
        for (int i = 0; i < labels.length; i++) {
            Rectangle swatch = new Rectangle(10, 10);
            swatch.setFill(TYPE_COLORS[i]);
            swatch.setStroke(STROKE_COLORS[i]);
            Label l = new Label(labels[i], swatch);
            l.setStyle("-fx-font-size: 10px;");
            legend.getChildren().add(l);
        }

        VBox root = new VBox(0, topBar, legend, sp);
        VBox.setVgrow(sp, Priority.ALWAYS);

        Scene scene = new Scene(root, 1000, 700);
        ThemeManager.getInstance().applyTheme(scene);
        stage.setScene(scene);
        stage.show();

        // Zoom controls
        zoomSlider.valueProperty().addListener((obs, old, val) -> {
            zoomTransform.setX(val.doubleValue());
            zoomTransform.setY(val.doubleValue());
            zoomLabel.setText(String.format("%.0f%%", val.doubleValue() * 100));
        });

        canvas.addEventFilter(ScrollEvent.SCROLL, ev -> {
            if (ev.isControlDown()) {
                double delta = ev.getDeltaY() > 0 ? 0.1 : -0.1;
                double newVal = Math.max(0.1, Math.min(2.0, zoomSlider.getValue() + delta));
                zoomSlider.setValue(newVal);
                ev.consume();
            }
        });

        // State
        final List<FileNode>[] graphNodes = new List[]{new ArrayList<>()};
        final List<Edge>[] graphEdges = new List[]{new ArrayList<>()};
        final Timeline[] simTimer = new Timeline[]{null};
        final Random rnd = new Random(42);

        analyzeBtn.setOnAction(ev -> {
            if (simTimer[0] != null) { simTimer[0].stop(); simTimer[0] = null; }
            edgeLayer.getChildren().clear();
            nodeLayer.getChildren().clear();
            graphNodes[0] = scanFiles(projectRoot, fileFilter.getValue());
            if (graphNodes[0].isEmpty()) {
                Text t = new Text(30, 30, "No files found");
                t.setFill(Color.LIGHTGRAY);
                nodeLayer.getChildren().add(t);
                infoLabel.setText("No files found");
                return;
            }
            // Build name->node map for edge resolution
            Map<String, FileNode> nameMap = new HashMap<>();
            for (FileNode n : graphNodes[0]) nameMap.put(n.name.toLowerCase(), n);

            graphEdges[0] = new ArrayList<>();
            Set<String> edgeSet = new HashSet<>();
            for (FileNode n : graphNodes[0]) {
                for (String dep : n.deps) {
                    String shortName = dep.contains("/") ? dep.substring(dep.lastIndexOf('/') + 1) : dep;
                    shortName = shortName.replaceAll("[\"';\\.\\s]", "");
                    FileNode target = nameMap.get(shortName.toLowerCase());
                    if (target != null && target != n) {
                        String key = n.name + "->" + target.name;
                        if (edgeSet.add(key)) {
                            graphEdges[0].add(new Edge(n, target));
                        }
                    }
                }
            }

            // Initial random positions (avoid overlap)
            double cw = canvas.getPrefWidth() - 100;
            double ch = canvas.getPrefHeight() - 100;
            for (int i = 0; i < graphNodes[0].size(); i++) {
                FileNode n = graphNodes[0].get(i);
                n.x = 100 + rnd.nextDouble() * cw;
                n.y = 100 + rnd.nextDouble() * ch;
                n.vx = 0; n.vy = 0;
            }

            // Physics simulation
            simTimer[0] = new Timeline(new KeyFrame(javafx.util.Duration.millis(30), tick -> {
                simulate(graphNodes[0], graphEdges[0]);
                render(edgeLayer, nodeLayer, graphNodes[0], graphEdges[0], searchField.getText());
            }));
            simTimer[0].setCycleCount(200);
            simTimer[0].play();

            infoLabel.setText(graphNodes[0].size() + " files, " + graphEdges[0].size() + " edges");
            stage.setTitle("Dependency Visualizer: " + projectRoot.getName()
                + " (" + graphNodes[0].size() + " files)");
        });

        resetBtn.setOnAction(ev -> {
            for (FileNode n : graphNodes[0]) {
                n.x = 100 + rnd.nextDouble() * (canvas.getPrefWidth() - 100);
                n.y = 100 + rnd.nextDouble() * (canvas.getPrefHeight() - 100);
                n.vx = 0; n.vy = 0;
            }
            if (simTimer[0] != null) { simTimer[0].stop(); }
            simTimer[0] = new Timeline(new KeyFrame(javafx.util.Duration.millis(30), tick -> {
                simulate(graphNodes[0], graphEdges[0]);
                render(edgeLayer, nodeLayer, graphNodes[0], graphEdges[0], searchField.getText());
            }));
            simTimer[0].setCycleCount(150);
            simTimer[0].play();
        });

        searchField.textProperty().addListener((obs, old, val) -> {
            render(edgeLayer, nodeLayer, graphNodes[0], graphEdges[0], val);
        });
    }

    // Force-directed layout
    private static void simulate(List<FileNode> nodes, List<Edge> edges) {
        if (nodes.isEmpty()) return;
        double repulsion = 80000;
        double attraction = 0.005;
        double damping = 0.85;
        double minDist = 30;
        double centerForce = 0.002;

        double cx = 800, cy = 800;

        // Repulsion (Barnes-Hut simplified)
        int len = nodes.size();
        double[] fx = new double[len];
        double[] fy = new double[len];

        for (int i = 0; i < len; i++) {
            FileNode a = nodes.get(i);
            for (int j = i + 1; j < len; j++) {
                FileNode b = nodes.get(j);
                double dx = a.x - b.x;
                double dy = a.y - b.y;
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist < minDist) dist = minDist;
                double force = repulsion / (dist * dist);
                double fdx = force * dx / dist;
                double fdy = force * dy / dist;
                fx[i] += fdx;
                fy[i] += fdy;
                fx[j] -= fdx;
                fy[j] -= fdy;
            }
        }

        // Attraction along edges
        for (Edge e : edges) {
            double dx = e.target.x - e.source.x;
            double dy = e.target.y - e.source.y;
            double dist = Math.sqrt(dx * dx + dy * dy);
            double force = attraction * dist;
            double fdx = force * dx / Math.max(dist, 1);
            double fdy = force * dy / Math.max(dist, 1);
            int si = nodes.indexOf(e.source);
            int ti = nodes.indexOf(e.target);
            if (si >= 0 && ti >= 0) {
                fx[si] += fdx;
                fy[si] += fdy;
                fx[ti] -= fdx;
                fy[ti] -= fdy;
            }
        }

        // Center gravity and apply
        for (int i = 0; i < len; i++) {
            FileNode n = nodes.get(i);
            // Center gravity
            fx[i] += (cx - n.x) * centerForce;
            fy[i] += (cy - n.y) * centerForce;

            n.vx = (n.vx + fx[i]) * damping;
            n.vy = (n.vy + fy[i]) * damping;

            // Clamp velocity
            double speed = Math.sqrt(n.vx * n.vx + n.vy * n.vy);
            if (speed > 15) { n.vx = n.vx / speed * 15; n.vy = n.vy / speed * 15; }

            n.x += n.vx;
            n.y += n.vy;

            // Keep in bounds
            n.x = Math.max(30, Math.min(1970, n.x));
            n.y = Math.max(30, Math.min(1970, n.y));
        }
    }

    private static void render(Pane edgeLayer, Pane nodeLayer,
                               List<FileNode> nodes, List<Edge> edges, String filter) {
        edgeLayer.getChildren().clear();
        nodeLayer.getChildren().clear();

        String lcFilter = filter.toLowerCase().trim();

        // Determine which nodes match
        Set<FileNode> matched = new HashSet<>();
        Set<FileNode> connected = new HashSet<>();
        if (!lcFilter.isEmpty()) {
            for (FileNode n : nodes) {
                if (n.name.toLowerCase().contains(lcFilter)) {
                    matched.add(n);
                }
            }
            // Also show nodes connected to matched
            for (Edge e : edges) {
                if (matched.contains(e.source) || matched.contains(e.target)) {
                    connected.add(e.source);
                    connected.add(e.target);
                }
            }
        }

        boolean hasFilter = !lcFilter.isEmpty();

        // Draw edges
        for (Edge e : edges) {
            boolean visible = !hasFilter || matched.contains(e.source) || matched.contains(e.target)
                || connected.contains(e.source);
            double alpha = visible ? 0.3 : 0.04;

            Line line = new Line(e.source.x, e.source.y, e.target.x, e.target.y);
            line.setStroke(Color.web("#a0a0a0", alpha));
            line.setStrokeWidth(1.5);

            // Arrow head
            Polygon arrow = new Polygon();
            double dx = e.target.x - e.source.x;
            double dy = e.target.y - e.source.y;
            double len = Math.sqrt(dx * dx + dy * dy);
            if (len > 10) {
                double ux = dx / len;
                double uy = dy / len;
                double arrowSize = 8;
                double ax = e.target.x - ux * 12;
                double ay = e.target.y - uy * 12;
                double perpX = -uy;
                double perpY = ux;
                arrow.getPoints().addAll(
                    ax + ux * arrowSize, ay + uy * arrowSize,
                    ax + perpX * arrowSize * 0.5 - ux * arrowSize * 0.3,
                    ay + perpY * arrowSize * 0.5 - uy * arrowSize * 0.3,
                    ax - perpX * arrowSize * 0.5 - ux * arrowSize * 0.3,
                    ay - perpY * arrowSize * 0.5 - uy * arrowSize * 0.3
                );
                arrow.setFill(Color.web("#a0a0a0", alpha));
                arrow.setStroke(null);
            }

            if (visible && alpha > 0.1) {
                line.setStrokeWidth(2);
                line.setStroke(Color.web("#a0a0a0", 0.5));
            }
            edgeLayer.getChildren().add(line);
            if (arrow.getPoints().size() > 0) edgeLayer.getChildren().add(arrow);
        }

        // Draw nodes
        for (FileNode n : nodes) {
            boolean match = lcFilter.isEmpty() || matched.contains(n) || connected.contains(n);
            double alpha = hasFilter ? (match ? 1.0 : 0.12) : 1.0;

            int typeIdx = getTypeIndex(n.name);
            Color fill = TYPE_COLORS[typeIdx];
            Color stroke = STROKE_COLORS[typeIdx];

            double bw = Math.min(180, Math.max(120, n.name.length() * 7 + 10));
            double bh = 32;

            // Glow for matched
            if (match && !lcFilter.isEmpty()) {
                Rectangle glow = new Rectangle(n.x - 3, n.y - 3, bw + 6, bh + 6);
                glow.setFill(null);
                glow.setStroke(Color.web("#ffdd57", 0.6));
                glow.setStrokeWidth(2);
                glow.setArcWidth(10); glow.setArcHeight(10);
                nodeLayer.getChildren().add(glow);
            }

            Rectangle rect = new Rectangle(n.x, n.y, bw, bh);
            rect.setFill(Color.web(toHex(fill), alpha));
            rect.setStroke(Color.web(toHex(stroke), alpha));
            rect.setArcWidth(6); rect.setArcHeight(6);
            rect.setCursor(Cursor.HAND);

            Text label = new Text(n.x + 6, n.y + 20, n.name);
            label.setFill(Color.web("#ffffff", alpha));
            label.setStyle("-fx-font-size: 11px; -fx-font-family: 'Consolas';");
            if (hasFilter && !match) {
                label.setOpacity(0.12);
            }

            // Tooltip
            StringBuilder ttText = new StringBuilder("File: ").append(n.name)
                .append("\nPath: ").append(n.path)
                .append("\nDependencies: ").append(n.deps.size());
            if (!n.deps.isEmpty()) {
                ttText.append("\n---\n");
                for (String d : n.deps) ttText.append("  ").append(d).append("\n");
            }
            String tip = ttText.toString();
            Tooltip tooltip = new Tooltip(tip);
            tooltip.setStyle("-fx-font-size: 10px; -fx-background-color: #222; -fx-text-fill: #ddd; -fx-padding: 6;");
            Tooltip.install(rect, tooltip);

            // Click to show detail dialog
            rect.setOnMouseClicked(ev -> showNodeDetail(n, ev));

            // Drag support
            final Point2D[] dragDelta = {new Point2D(0, 0)};
            rect.setOnMousePressed(ev -> {
                dragDelta[0] = new Point2D(ev.getSceneX() - n.x, ev.getSceneY() - n.y);
                rect.toFront();
            });
            rect.setOnMouseDragged(ev -> {
                n.x = ev.getSceneX() - dragDelta[0].getX();
                n.y = ev.getSceneY() - dragDelta[0].getY();
                render(edgeLayer, nodeLayer, nodes, edges, "");
            });

            nodeLayer.getChildren().addAll(rect, label);
        }
    }

    private static int getTypeIndex(String name) {
        if (name.endsWith(".js") || name.endsWith(".jsx")) return 0;
        if (name.endsWith(".ts") || name.endsWith(".tsx")) return 1;
        if (name.endsWith(".java")) return 2;
        if (name.endsWith(".py")) return 3;
        return 4;
    }

    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x",
            (int)(c.getRed() * 255),
            (int)(c.getGreen() * 255),
            (int)(c.getBlue() * 255));
    }

    private static void showNodeDetail(FileNode node, MouseEvent ev) {
        if (ev.getButton() != MouseButton.PRIMARY) return;
        Stage dStage = new Stage();
        dStage.setTitle("Node: " + node.name);

        VBox dRoot = new VBox(6);
        dRoot.setPadding(new Insets(10));

        Label nameLbl = new Label("File: " + node.name);
        nameLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        Label pathLbl = new Label("Path: " + node.path);
        pathLbl.setStyle("-fx-font-size: 11px;");
        TextArea depsArea = new TextArea();
        depsArea.setEditable(false);
        depsArea.setPrefRowCount(8);
        StringBuilder sb = new StringBuilder();
        if (node.deps.isEmpty()) {
            sb.append("(no dependencies)");
        } else {
            for (String d : node.deps) sb.append(d).append("\n");
        }
        depsArea.setText(sb.toString());

        Button openBtn = new Button("Open File");
        openBtn.setStyle("-fx-background-color: -accent; -fx-text-fill: white; -fx-cursor: hand;");
        openBtn.setOnAction(e -> {
            try {
                java.awt.Desktop.getDesktop().open(new File(node.path));
            } catch (Exception ignored) {}
        });

        dRoot.getChildren().addAll(nameLbl, pathLbl, new Label("Imports:"), depsArea, openBtn);

        Scene dScene = new Scene(dRoot, 400, 300);
        ThemeManager.getInstance().applyTheme(dScene);
        dStage.setScene(dScene);
        dStage.show();
    }

    // Scanning
    static class FileNode {
        String name, path;
        List<String> deps = new ArrayList<>();
        double x, y, vx, vy;
        FileNode(String n, String p) { name = n; path = p; }
    }

    static class Edge {
        FileNode source, target;
        Edge(FileNode s, FileNode t) { source = s; target = t; }
    }

    private static List<FileNode> scanFiles(File dir, String typeFilter) {
        List<FileNode> nodes = new ArrayList<>();
        scan(dir, nodes, typeFilter);
        return nodes;
    }

    private static void scan(File dir, List<FileNode> nodes, String typeFilter) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                String n = f.getName();
                if (!n.startsWith(".") && !"node_modules".equals(n) && !"build".equals(n)
                    && !"dist".equals(n) && !"target".equals(n) && !"__pycache__".equals(n)) {
                    scan(f, nodes, typeFilter);
                }
            } else {
                String name = f.getName();
                if (!typeFilter.equals("All") && !name.endsWith(typeFilter)) continue;
                if (name.endsWith(".js") || name.endsWith(".ts") || name.endsWith(".jsx")
                    || name.endsWith(".tsx") || name.endsWith(".java") || name.endsWith(".py")) {
                    try {
                        byte[] bytes = Files.readAllBytes(f.toPath());
                        // Skip binary files
                        boolean binary = false;
                        int checkLen = Math.min(bytes.length, 1024);
                        for (int i = 0; i < checkLen; i++) {
                            if (bytes[i] == 0) { binary = true; break; }
                        }
                        if (binary) continue;

                        String content = new String(bytes);
                        FileNode node = new FileNode(name, f.getAbsolutePath());
                        extractDeps(content, name, node);
                        if (!node.deps.isEmpty() || true) { // include all
                            nodes.add(node);
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    private static void extractDeps(String content, String filename, FileNode node) {
        // Java imports
        Pattern javaImport = Pattern.compile("^import\\s+(static\\s+)?([a-zA-Z0-9_.]+);", Pattern.MULTILINE);
        Matcher m = javaImport.matcher(content);
        while (m.find()) {
            String imp = m.group(2);
            if (!imp.startsWith("java.") && !imp.startsWith("javax.") && !imp.startsWith("sun.")) {
                String shortName = imp.contains(".") ? imp.substring(imp.lastIndexOf('.') + 1) : imp;
                node.deps.add(shortName + ".java");
            }
        }

        // JS/TS imports
        Pattern jsImport = Pattern.compile(
            "(?:import\\s+(?:\\{[^}]+\\}|\\*\\s+as\\s+\\w+|\\w+)\\s+from\\s+|require\\s*\\()['\"]([^'\"]+)['\"]",
            Pattern.MULTILINE);
        m = jsImport.matcher(content);
        while (m.find()) {
            String imp = m.group(1);
            String shortName = imp.contains("/") ? imp.substring(imp.lastIndexOf('/') + 1) : imp;
            // Remove .js, .ts etc
            shortName = shortName.replaceAll("\\.(js|ts|jsx|tsx)$", "");
            node.deps.add(shortName + (filename.endsWith(".ts") ? ".ts" : ".js"));
        }

        // Python imports
        Pattern pyImport = Pattern.compile(
            "^(?:from\\s+(\\S+)\\s+import|import\\s+(\\S+))", Pattern.MULTILINE);
        m = pyImport.matcher(content);
        while (m.find()) {
            String imp = m.group(1) != null ? m.group(1) : m.group(2);
            String shortName = imp.contains(".") ? imp.substring(0, imp.indexOf('.')) : imp;
            node.deps.add(shortName + ".py");
        }
    }
}
