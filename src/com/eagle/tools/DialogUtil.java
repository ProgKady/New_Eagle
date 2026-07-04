package com.eagle.tools;

import com.eagle.util.ThemeManager;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public class DialogUtil {

    private static Window ownerWindow;

    public static void setOwnerWindow(Window w) { ownerWindow = w; }

    public static Window getOwnerWindow() { return ownerWindow; }

    public static void applyTheme(Dialog<?> dlg) {
        dlg.setOnShown(e -> {
            Scene s = dlg.getDialogPane().getScene();
            if (s != null) ThemeManager.getInstance().applyTheme(s);
        });
    }

    public static Alert alert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg);
        a.setTitle(title);
        a.setHeaderText(null);
        if (ownerWindow != null) a.initOwner(ownerWindow);
        applyTheme(a);
        return a;
    }

    public static void showInfo(String title, String msg) {
        alert(Alert.AlertType.INFORMATION, title, msg).showAndWait();
    }

    public static void showError(String title, String msg) {
        alert(Alert.AlertType.ERROR, title, msg).showAndWait();
    }

    public static TextInputDialog textInput(String title, String header, String defaultValue) {
        TextInputDialog dlg = new TextInputDialog(defaultValue);
        dlg.setTitle(title);
        dlg.setHeaderText(header);
        if (ownerWindow != null) dlg.initOwner(ownerWindow);
        applyTheme(dlg);
        return dlg;
    }

    public static <T> ChoiceDialog<T> choice(String title, String header, T defaultValue, T... items) {
        ChoiceDialog<T> dlg = new ChoiceDialog<>(defaultValue, items);
        dlg.setTitle(title);
        dlg.setHeaderText(header);
        if (ownerWindow != null) dlg.initOwner(ownerWindow);
        applyTheme(dlg);
        return dlg;
    }

    public static Dialog<String> resultDialog(String title) {
        Dialog<String> dlg = new Dialog<>();
        dlg.setTitle(title);
        dlg.setHeaderText(null);
        if (ownerWindow != null) dlg.initOwner(ownerWindow);
        applyTheme(dlg);
        return dlg;
    }

    public static Dialog<Void> progressDialog(String title, String header) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle(title);
        dlg.setHeaderText(header);
        if (ownerWindow != null) dlg.initOwner(ownerWindow);
        applyTheme(dlg);
        return dlg;
    }

    public static void showResult(String title, String text) {
        Dialog<String> dlg = resultDialog(title);
        TextArea area = new TextArea(text);
        area.setEditable(false);
        area.setPrefRowCount(10);
        area.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        Button copyBtn = new Button("Copy to Clipboard");
        copyBtn.setOnAction(e -> {
            javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
            cc.putString(text);
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
        });
        VBox vb = new VBox(8, area, copyBtn);
        dlg.getDialogPane().setContent(vb);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dlg.showAndWait();
    }

    public static void showResultLarge(String title, String text) {
        Dialog<String> dlg = resultDialog(title);
        dlg.setResizable(true);
        dlg.getDialogPane().setPrefSize(700, 500);
        TextArea area = new TextArea(text);
        area.setEditable(false);
        area.setWrapText(true);
        area.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        Button copyBtn = new Button("Copy to Clipboard");
        copyBtn.setOnAction(e -> {
            javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
            cc.putString(text);
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
        });
        VBox vb = new VBox(8, area, copyBtn);
        dlg.getDialogPane().setContent(vb);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dlg.showAndWait();
    }
}
