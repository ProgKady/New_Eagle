package com.eagle.editor;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.fxmisc.richtext.CodeArea;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InlayHintOverlay {

    private final List<Label> hints = new ArrayList<>();
    private final CodeArea codeArea;
    private final StackPane parent;
    private boolean visible = true;
    private boolean pendingUpdate = false;

    public InlayHintOverlay(CodeArea codeArea, StackPane parent) {
        this.codeArea = codeArea;
        this.parent = parent;
    }

    public void update() {
        if (pendingUpdate) return;
        pendingUpdate = true;
        Platform.runLater(() -> {
            pendingUpdate = false;
            doUpdate();
        });
    }

    private void doUpdate() {
        clear();

        String text = codeArea.getText();
        if (text == null || text.isEmpty() || text.length() > 50000) return;

        int caret = codeArea.getCaretPosition();
        int start = Math.max(0, caret - 200);
        int end = Math.min(text.length(), caret + 200);
        String context = text.substring(start, end);

        Matcher eqMatcher = Pattern.compile("=\\s*([^;,\\)\\]]+)").matcher(context);
        while (eqMatcher.find()) {
            int hintPos = start + eqMatcher.start(1);
            addHint(hintPos, ": ");
        }
    }

    private void addHint(int charPosition, String hintText) {
        if (hintText == null || hintText.isEmpty()) return;
        try {
            codeArea.getCharacterBoundsOnScreen(charPosition, Math.min(charPosition + 1, codeArea.getLength())).ifPresent(b -> {
                Label lbl = new Label(hintText);
                lbl.setStyle("-fx-text-fill: rgba(200,200,200,0.3); -fx-font-family: Consolas, monospace; -fx-font-size: 10px; -fx-background-color: transparent; -fx-padding: 0; -fx-font-style: italic;");
                lbl.setMouseTransparent(true);
                Point2D local = parent.screenToLocal(b.getMinX() - 12, b.getMinY());
                if (local != null) {
                    lbl.setLayoutX(local.getX());
                    lbl.setLayoutY(local.getY() - 1);
                }
                lbl.setVisible(visible);
                parent.getChildren().add(lbl);
                hints.add(lbl);
            });
        } catch (Exception ignored) {}
    }

    private void clear() {
        for (Label lbl : hints) {
            parent.getChildren().remove(lbl);
        }
        hints.clear();
    }

    public void setVisible(boolean v) {
        this.visible = v;
        for (Label lbl : hints) lbl.setVisible(v);
    }
}
