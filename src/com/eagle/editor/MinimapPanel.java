package com.eagle.editor;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public class MinimapPanel extends StackPane {

    private final Canvas canvas;
    private CodeEditor editor;
    private static final int MINIMAP_WIDTH = 64;
    private static final double LINE_HEIGHT = 3;
    private static final double CHAR_WIDTH = 1.2;
    private int totalLines;
    private int firstVisibleLine;
    private int visibleLineCount;
    public MinimapPanel() {
        setPadding(new Insets(0));
        setPrefWidth(MINIMAP_WIDTH);
        setMinWidth(MINIMAP_WIDTH);
        setMaxWidth(MINIMAP_WIDTH);
        getStyleClass().add("minimap");

        canvas = new Canvas(MINIMAP_WIDTH - 4, 0);
        canvas.setManaged(false);
        canvas.setMouseTransparent(false);
        getChildren().add(canvas);

        canvas.setOnMousePressed(this::onMousePressed);
        canvas.setOnMouseDragged(this::onMouseDragged);
        canvas.setOnMouseReleased(e -> {});

        new AnimationTimer() {
            long last = 0;
            @Override
            public void handle(long now) {
                if (now - last > 100_000_000) {
                    update();
                    last = now;
                }
            }
        }.start();
    }

    public void setEditor(CodeEditor ed) {
        this.editor = ed;
    }

    public void update() {
        if (editor == null) return;
        String text = editor.getText();
        if (text == null || text.isEmpty()) {
            canvas.setHeight(0);
            return;
        }
        String[] lines = text.split("\n", -1);
        totalLines = lines.length;
        double h = Math.max(200, totalLines * LINE_HEIGHT);
        canvas.setWidth(MINIMAP_WIDTH - 4);
        canvas.setHeight(h);
        setPrefHeight(h);
        setMinHeight(h);

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        boolean inBlockComment = false;
        for (int i = 0; i < lines.length; i++) {
            double y = i * LINE_HEIGHT;
            String line = lines[i];
            if (line.contains("/*")) inBlockComment = true;

            if (line.trim().isEmpty()) {
                gc.setFill(Color.gray(0.15));
                gc.fillRect(2, y, canvas.getWidth() - 4, LINE_HEIGHT - 0.3);
            } else {
                renderLine(gc, line, y, inBlockComment);
            }

            if (line.contains("*/") && !line.trim().startsWith("/*")) inBlockComment = false;
        }

        // Viewport indicator
        if (totalLines > 0) {
            double viewY = firstVisibleLine * LINE_HEIGHT;
            double viewH = visibleLineCount * LINE_HEIGHT;
            gc.setFill(Color.rgb(255, 255, 255, 0.1));
            gc.fillRect(0, viewY, canvas.getWidth(), viewH);
            gc.setStroke(Color.rgb(255, 255, 255, 0.25));
            gc.setLineWidth(1);
            gc.strokeRect(1, viewY, canvas.getWidth() - 2, viewH);
        }
    }

    private void renderLine(GraphicsContext gc, String line, double y, boolean inBlockComment) {
        int len = Math.min(line.length(), (int)(canvas.getWidth() / CHAR_WIDTH));
        for (int j = 0; j < len; j++) {
            char ch = line.charAt(j);
            Color c = charColor(ch, inBlockComment);
            gc.setFill(c);
            gc.fillRect(2 + j * CHAR_WIDTH, y, CHAR_WIDTH, LINE_HEIGHT - 0.3);
        }
    }

    private Color charColor(char ch, boolean inBlockComment) {
        if (inBlockComment) return Color.rgb(70, 140, 80);
        switch (ch) {
            case ' ': case '\t': return Color.gray(0.15);
            case '{': case '}': return Color.rgb(255, 200, 50);
            case '(': case ')': return Color.rgb(130, 170, 255);
            case '[': case ']': return Color.rgb(130, 170, 255);
            case '<': case '>': return Color.rgb(220, 180, 100);
            case '+': case '-': case '*': case '/': case '%':
            case '=': case '!': return Color.rgb(250, 150, 80);
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9': return Color.rgb(255, 200, 50);
            case '"': case '\'': case '`': return Color.rgb(220, 180, 100);
            case '.': case ',': case ';': case ':': return Color.rgb(180, 190, 210);
            default: return Color.rgb(180, 190, 210);
        }
    }

    private void onMousePressed(MouseEvent e) {
        if (editor == null || totalLines == 0) return;
        scrollToY(e.getY());
    }

    private void onMouseDragged(MouseEvent e) {
        if (editor == null || totalLines == 0) return;
        scrollToY(e.getY());
    }

    private void scrollToY(double y) {
        int line = (int)(y / LINE_HEIGHT);
        if (line >= 0 && line < totalLines) {
            editor.moveTo(line, 0);
            editor.requestFocus();
        }
    }

    public void setViewport(int firstLine, int lineCount) {
        this.firstVisibleLine = firstLine;
        this.visibleLineCount = lineCount;
    }
}
