package com.eagle.tools;

import com.eagle.util.ThemeManager;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.*;

public class ColorPickerTool {

    public static String show(Window owner) {
        return show(owner, "#ffffff");
    }

    public static String show(Window owner, String initial) {
        javafx.scene.control.ColorPicker picker = new javafx.scene.control.ColorPicker();
        try { picker.setValue(Color.web(initial)); } catch (Exception e) { picker.setValue(Color.WHITE); }

        Rectangle preview = new Rectangle(60, 30);
        preview.setArcWidth(6); preview.setArcHeight(6);
        preview.fillProperty().bind(picker.valueProperty());

        TextField hexField = new TextField(initial);
        hexField.setPrefColumnCount(9);
        hexField.textProperty().addListener((o, ov, nv) -> {
            try { picker.setValue(Color.web(nv)); } catch (Exception ignored) {}
        });
        picker.setOnAction(e -> {
            Color c = picker.getValue();
            hexField.setText(formatHex(c));
        });

        HBox top = new HBox(10, new Label("Color:"), picker, preview, new Label("Hex:"), hexField);
        top.setPadding(new Insets(15));

        HBox recent = new HBox(6);
        recent.setPadding(new Insets(0, 15, 15, 15));
        String[] defaults = {"#ff0000", "#00ff00", "#0000ff", "#ffff00", "#ff00ff", "#00ffff", "#000000", "#ffffff", "#888888"};
        for (String c : defaults) {
            Rectangle r = new Rectangle(24, 24);
            r.setFill(Color.web(c));
            r.setArcWidth(4); r.setArcHeight(4);
            r.setStyle("-fx-cursor: hand; -fx-border-color: #ccc; -fx-border-radius: 4;");
            r.setOnMouseClicked(e -> { picker.setValue(Color.web(c)); hexField.setText(c); });
            recent.getChildren().add(r);
        }

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Color Picker");
        dialog.initOwner(owner);
        VBox root = new VBox(10, top, new Label("Recent colors:"), recent);
        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().getButtonTypes().addAll(new ButtonType("Select", ButtonBar.ButtonData.OK_DONE), ButtonType.CANCEL);
        dialog.setResultConverter(btn -> btn.getButtonData() == ButtonBar.ButtonData.OK_DONE ? hexField.getText() : null);
        dialog.getDialogPane().sceneProperty().addListener((obs, oldS, newS) -> {
            if (newS != null) ThemeManager.getInstance().applyTheme(newS);
        });

        return dialog.showAndWait().orElse(null);
    }

    private static String formatHex(Color c) {
        return String.format("#%02x%02x%02x",
            (int)(c.getRed() * 255), (int)(c.getGreen() * 255), (int)(c.getBlue() * 255));
    }
}
