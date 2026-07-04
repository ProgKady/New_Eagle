package com.eagle.editor;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BreadcrumbBar extends HBox {

    private File projectRoot;
    private File currentFile;
    private Runnable onSegmentClick;

    private static final Pattern[] CONTEXT_PATTERNS = {
        Pattern.compile("(?:public|private|protected|static|abstract|final|native|synchronized)\\s+class\\s+(\\w+)"),
        Pattern.compile("(?:public|private|protected|static)?\\s*(?:function\\s+)?(\\w+)\\s*\\([^)]*\\)\\s*\\{"),
        Pattern.compile("(?:export\\s+)?(?:default\\s+)?(?:function\\s+|const\\s+\\w+\\s*=\\s*(?:async\\s+)?\\([^)]*\\)\\s*=>|class\\s+)"),
        Pattern.compile("def\\s+(\\w+)\\s*\\("),
        Pattern.compile("(?:public|private|protected)?\\s*(?:static\\s+)?(?:void|int|String|boolean|double|float|long|char|var|val|fun)\\s+(\\w+)\\s*\\("),
        Pattern.compile("(?:sub|function)\\s+(\\w+)"),
        Pattern.compile("^(\\w+)\\s*[:=]"),
    };

    private static final Pattern FUNCTION_PATTERN = Pattern.compile(
        "(?:public|private|protected|static|abstract|final|native|synchronized|export|async|def|fun|function|sub|val|var|let|const)\\s+" +
        "(?:function\\s+)?(\\w+)\\s*\\("
    );
    private static final Pattern CLASS_PATTERN = Pattern.compile(
        "(?:public|private|protected|static|abstract|final|sealed|open|data)\\s+(?:class|interface|enum|object|trait|struct)\\s+(\\w+)"
    );

    public BreadcrumbBar() {
        getStyleClass().add("breadcrumb-bar");
        setPadding(new Insets(2, 8, 2, 8));
        setSpacing(4);
    }

    public void setProjectRoot(File root) {
        this.projectRoot = root;
        update();
    }

    public void setCurrentFile(File file) {
        this.currentFile = file;
        update();
    }

    public void setOnSegmentClick(Runnable r) {
        this.onSegmentClick = r;
    }

    public void update() {
        getChildren().clear();

        List<String> segments = new ArrayList<>();

        if (projectRoot != null) {
            String projName = projectRoot.getName();
            segments.add(projName);

            if (currentFile != null) {
                String relPath = currentFile.getAbsolutePath()
                    .replace(projectRoot.getAbsolutePath(), "")
                    .replace("\\", "/")
                    .replaceAll("^/", "");
                if (!relPath.isEmpty()) {
                    String[] parts = relPath.split("/");
                    for (int i = 0; i < parts.length - 1; i++) {
                        segments.add(parts[i]);
                    }
                    String fileName = parts[parts.length - 1];
                    String ctx = detectContext(currentFile);
                    if (ctx != null) {
                        segments.add(fileName);
                        segments.add(ctx);
                    } else {
                        segments.add(fileName);
                    }
                }
            }
        } else if (currentFile != null) {
            segments.add(currentFile.getName());
            String ctx = detectContext(currentFile);
            if (ctx != null) {
                segments.add(ctx);
            }
        }

        for (int i = 0; i < segments.size(); i++) {
            if (i > 0) {
                Label sep = new Label("›");
                sep.getStyleClass().add("breadcrumb-separator");
                getChildren().add(sep);
            }
            Hyperlink link = new Hyperlink(segments.get(i));
            link.getStyleClass().add("breadcrumb-segment");
            link.setVisited(false);
            link.setFocusTraversable(false);
            final int idx = i;
            link.setOnAction(e -> {
                if (onSegmentClick != null) onSegmentClick.run();
            });
            getChildren().add(link);
        }

        setVisible(true);
        setManaged(true);
    }

    private String detectContext(File file) {
        if (file == null || !file.exists()) return null;
        try {
            String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
            int caretLine = 0;
            return detectContextInText(content, caretLine);
        } catch (Exception e) {
            return null;
        }
    }

    private String detectContextInText(String text, int caretLine) {
        String[] lines = text.split("\n", -1);
        int targetLine = Math.min(caretLine, lines.length - 1);
        String lastFunc = null;
        String lastClass = null;

        for (int i = 0; i <= targetLine && i < lines.length; i++) {
            String line = lines[i].trim();

            Matcher cm = CLASS_PATTERN.matcher(line);
            if (cm.find()) {
                lastClass = cm.group(1);
                lastFunc = null;
                continue;
            }

            if (!line.contains("return") && !line.contains("if ") && !line.contains("else ") && !line.contains("for ") && !line.contains("while ")) {
                Matcher fm = FUNCTION_PATTERN.matcher(line);
                if (fm.find()) {
                    lastFunc = fm.group(1);
                }
            }
        }

        if (lastFunc != null) return lastFunc;
        if (lastClass != null) return lastClass;
        return null;
    }
}
