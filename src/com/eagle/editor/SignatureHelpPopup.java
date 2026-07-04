package com.eagle.editor;

import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

public class SignatureHelpPopup {

    private final Popup popup = new Popup();
    private final VBox root = new VBox(4);
    private final Label label = new Label();
    private int currentSignatureIndex = 0;
    private CompletionProvider.Signature[] signatures;

    public SignatureHelpPopup() {
        root.setMaxWidth(500);
        root.setStyle("-fx-background-color: #1e2030; -fx-border-color: #3d415c; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 8, 0, 0, 2);");
        label.setWrapText(true);
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: #cdd6f4; -fx-font-family: Consolas, monospace;");
        root.getChildren().add(label);
        popup.getContent().add(root);
        popup.setAutoHide(true);
    }

    public void show(CompletionProvider.Signature[] sigs, Point2D screenPos) {
        this.signatures = sigs;
        this.currentSignatureIndex = 0;
        updateText();
        popup.show(label, screenPos.getX(), screenPos.getY() + 20);
    }

    public void hide() {
        popup.hide();
    }

    public boolean isShowing() {
        return popup.isShowing();
    }

    public void nextSignature() {
        if (signatures != null && signatures.length > 1) {
            currentSignatureIndex = (currentSignatureIndex + 1) % signatures.length;
            updateText();
        }
    }

    public void prevSignature() {
        if (signatures != null && signatures.length > 1) {
            currentSignatureIndex = (currentSignatureIndex - 1 + signatures.length) % signatures.length;
            updateText();
        }
    }

    private void updateText() {
        if (signatures == null || currentSignatureIndex >= signatures.length) return;
        CompletionProvider.Signature sig = signatures[currentSignatureIndex];
        StringBuilder sb = new StringBuilder();
        if (signatures.length > 1) {
            sb.append("⮕ ").append(currentSignatureIndex + 1).append("/").append(signatures.length).append("\n");
        }
        sb.append(sig.label);
        if (sig.returnType != null && !sig.returnType.isEmpty() && !"void".equals(sig.returnType)) {
            sb.append(" → ").append(sig.returnType);
        }
        if (sig.params != null && sig.params.length > 0) {
            sb.append("\n──────────\n");
            for (int i = 0; i < sig.params.length; i++) {
                sb.append("  ").append(sig.params[i]).append("\n");
            }
        }
        if (sig.description != null && !sig.description.isEmpty()) {
            sb.append("\n").append(sig.description);
        }
        label.setText(sb.toString().trim());
    }
}
