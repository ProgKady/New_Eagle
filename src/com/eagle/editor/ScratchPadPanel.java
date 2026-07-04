package com.eagle.editor;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ScratchPadPanel extends VBox {

    private final TextArea textArea = new TextArea();
    private static final File SAVE_FILE = new File(System.getProperty("user.home") + "/.webide/scratchpad.txt");

    public ScratchPadPanel() {
        setStyle("-fx-background-color: -bg-secondary;");
        Label header = new Label("Scratch Pad");
        header.setStyle("-fx-font-weight: bold; -fx-padding: 8; -fx-font-size: 13px;");

        Button saveBtn = new Button("Save");
        saveBtn.setOnAction(e -> save());

        Button clearBtn = new Button("Clear");
        clearBtn.setOnAction(e -> textArea.clear());

        HBox topBar = new HBox(8, header, saveBtn, clearBtn);
        topBar.setPadding(new Insets(4, 8, 4, 8));
        topBar.setStyle("-fx-border-color: -border-color; -fx-border-width: 0 0 1 0;");

        textArea.setPromptText("Write notes here...");
        textArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        VBox.setVgrow(textArea, Priority.ALWAYS);

        load();

        getChildren().addAll(topBar, textArea);
    }

    private void load() {
        try {
            if (SAVE_FILE.exists()) {
                textArea.setText(new String(Files.readAllBytes(SAVE_FILE.toPath()), StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {}
    }

    private void save() {
        try {
            SAVE_FILE.getParentFile().mkdirs();
            Files.write(SAVE_FILE.toPath(), textArea.getText().getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {}
    }
}
