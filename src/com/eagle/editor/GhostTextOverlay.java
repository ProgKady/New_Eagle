package com.eagle.editor;

import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import org.fxmisc.richtext.CodeArea;

public class GhostTextOverlay {

    private final Label ghostLabel = new Label();
    private final CodeArea codeArea;
    private final StackPane parent;
    private String ghostText = "";
    private int ghostOffset = 0;
    private Runnable onAccept;

    public GhostTextOverlay(CodeArea codeArea, StackPane parent) {
        this.codeArea = codeArea;
        this.parent = parent;
        ghostLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.25); -fx-font-family: Consolas, monospace; -fx-font-size: 13.5px; -fx-background-color: transparent; -fx-padding: 0;");
        ghostLabel.setMouseTransparent(true);
        ghostLabel.setVisible(false);
        parent.getChildren().add(ghostLabel);

        codeArea.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.TAB && isShowing()) {
                accept();
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE && isShowing()) {
                hide();
            }
        });

        codeArea.addEventFilter(KeyEvent.KEY_TYPED, e -> {
            if (isShowing() && !e.getCharacter().isEmpty()) {
                String ch = e.getCharacter();
                if (ghostText.startsWith(ch)) {
                    ghostText = ghostText.substring(ch.length());
                    ghostOffset += ch.length();
                    if (ghostText.isEmpty()) { hide(); return; }
                    updatePosition();
                } else {
                    hide();
                }
            }
        });
    }

    public void show(String completionText) {
        if (completionText == null || completionText.isEmpty()) return;
        int caret = codeArea.getCaretPosition();
        if (caret < 0) return;
        int len = codeArea.getLength();
        String after = "";
        if (caret < len) {
            after = codeArea.getText(caret, Math.min(caret + completionText.length(), len));
        }
        if (!completionText.startsWith(after)) return;

        ghostText = completionText.substring(after.length());
        ghostOffset = caret + after.length();
        if (ghostText.isEmpty()) return;

        ghostLabel.setText(ghostText);
        ghostLabel.setVisible(true);
        updatePosition();
    }

    public void showCustom(String text, int position) {
        ghostText = text;
        ghostOffset = position;
        ghostLabel.setText(ghostText);
        ghostLabel.setVisible(true);
        updatePosition();
    }

    private void updatePosition() {
        try {
            int len = codeArea.getLength();
            int start = ghostOffset;
            int end = start + 1;
            if (start >= len) {
                start = Math.max(0, len - 1);
                end = len;
                codeArea.getCharacterBoundsOnScreen(start, end).ifPresent(b -> {
                    Point2D local = parent.screenToLocal(b.getMaxX(), b.getMinY());
                    if (local != null) {
                        ghostLabel.setLayoutX(local.getX());
                        ghostLabel.setLayoutY(local.getY() - 1);
                    }
                });
            } else {
                codeArea.getCharacterBoundsOnScreen(start, end).ifPresent(b -> {
                    Point2D local = parent.screenToLocal(b.getMinX(), b.getMinY());
                    if (local != null) {
                        ghostLabel.setLayoutX(local.getX());
                        ghostLabel.setLayoutY(local.getY() - 1);
                    }
                });
            }
        } catch (Exception ignored) {}
    }

    public void accept() {
        if (!isShowing()) return;
        int end = Math.min(ghostOffset + ghostText.length(), codeArea.getLength());
        codeArea.insertText(ghostOffset, ghostText);
        ghostText = "";
        ghostLabel.setVisible(false);
        if (onAccept != null) onAccept.run();
    }

    public void hide() {
        ghostText = "";
        ghostLabel.setVisible(false);
    }

    public boolean isShowing() {
        return ghostLabel.isVisible() && !ghostText.isEmpty();
    }

    public void setOnAccept(Runnable r) { this.onAccept = r; }
    public void setFontSize(double size) {
        ghostLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.25); -fx-font-family: Consolas, monospace; -fx-font-size: " + size + "px; -fx-background-color: transparent; -fx-padding: 0;");
    }
}
