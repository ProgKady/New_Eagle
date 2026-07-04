package com.eagle.editor;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import org.fxmisc.richtext.CodeArea;

public class IndentGuideOverlay {

    private final Canvas canvas;
    private final CodeArea codeArea;
    private final Pane parent;
    private int tabSize = 4;
    private Color lineColor = Color.rgb(255, 255, 255, 0.08);
    private boolean visible = true;
    private boolean pendingRedraw = false;
    private double scrollY = 0;
    private double charWidth = 8.1;
    private double lineHeight = 20.0;

    public IndentGuideOverlay(CodeArea codeArea, Pane parent) {
        this.codeArea = codeArea;
        this.parent = parent;
        this.canvas = new Canvas();
        canvas.setMouseTransparent(true);
        parent.getChildren().add(canvas);

        canvas.widthProperty().bind(parent.widthProperty());
        canvas.heightProperty().bind(parent.heightProperty());

        codeArea.textProperty().addListener((o, a, b) -> scheduleRedraw());
        codeArea.caretPositionProperty().addListener((o, a, b) -> scheduleRedraw());

        try {
            codeArea.estimatedScrollYProperty().addListener((o, old, val) -> {
                scrollY = val.doubleValue();
                scheduleRedraw();
            });
        } catch (Exception ignored) {}
    }

    private void scheduleRedraw() {
        if (pendingRedraw) return;
        pendingRedraw = true;
        Platform.runLater(() -> {
            pendingRedraw = false;
            redraw();
        });
    }

    public void redraw() {
        if (!visible) { canvas.setVisible(false); return; }
        canvas.setVisible(true);
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        int totalLines = codeArea.getParagraphs().size();
        if (totalLines == 0) return;

        double indentWidth = tabSize * charWidth;
        double gutterWidth = 45;

        int firstLine = Math.max(0, (int)(scrollY / lineHeight) - 1);
        int visibleLines = (int)(h / lineHeight) + 2;
        int lastLine = Math.min(totalLines, firstLine + visibleLines);

        gc.setStroke(lineColor);
        gc.setLineWidth(1);
        gc.setLineDashes(1, 3);

        for (int i = firstLine; i < lastLine; i++) {
            String line;
            try { line = codeArea.getParagraph(i).getText(); } catch (Exception e) { continue; }
            int indent = countIndent(line);
            if (indent < tabSize) continue;

            double y = i * lineHeight - scrollY;
            int levels = indent / tabSize;
            for (int lv = 1; lv <= levels; lv++) {
                double x = gutterWidth + lv * indentWidth - 4;
                if (x < w) {
                    gc.strokeLine(x, y, x, y + lineHeight);
                }
            }
        }
        gc.setLineDashes(null);
    }

    private int countIndent(String line) {
        int count = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == ' ') count++;
            else if (c == '\t') count += tabSize;
            else break;
        }
        return count;
    }

    public void setTabSize(int size) { this.tabSize = size; redraw(); }
    public void setVisible(boolean v) { this.visible = v; if (v) redraw(); else canvas.setVisible(false); }
    public void setLineColor(Color c) { this.lineColor = c; redraw(); }
}
