package com.eagle.plugin.builtin;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;

/**
 * Controller for NotePadDialog.fxml
 * داخل الـ JAR جنب الـ FXML وبيتحملوا مع بعض
 */
public class NotePadController {

    @FXML private TextArea noteArea;
    @FXML private Label statusLabel;

    private Stage stage;

    void setStage(Stage stage) { this.stage = stage; }

    @FXML
    private void onClear() {
        noteArea.clear();
        statusLabel.setText("Cleared");
    }

    @FXML
    private void onCopy() {
        String text = noteArea.getSelectedText();
        if (text == null || text.isEmpty()) text = noteArea.getText();
        ClipboardContent cc = new ClipboardContent();
        cc.putString(text);
        Clipboard.getSystemClipboard().setContent(cc);
        statusLabel.setText("Copied " + text.length() + " chars");
    }

    @FXML
    private void onClose() {
        if (stage != null) stage.close();
    }
}
