package com.eagle.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public class SplitEditorPane extends BorderPane {

    private final SplitPane splitPane = new SplitPane();
    private final List<CodeEditor> editors = new ArrayList<>();
    private final List<Consumer<CodeEditor>> onEditorAdded = new ArrayList<>();

    public SplitEditorPane(CodeEditor initial) {
        getStyleClass().add("split-editor-pane");

        HBox toolbar = new HBox(4);
        toolbar.setStyle("-fx-padding: 2 8; -fx-background-color: -bg-secondary;");
        Button splitH = new Button("||");
        splitH.setTooltip(new Tooltip("Split horizontally"));
        splitH.setStyle("-fx-font-size: 10px;");
        splitH.setOnAction(e -> splitHorizontal(initial));

        Button splitV = new Button("=");
        splitV.setTooltip(new Tooltip("Split vertically"));
        splitV.setStyle("-fx-font-size: 10px;");
        splitV.setOnAction(e -> splitVertical(initial));

        Button unsplit = new Button("X");
        unsplit.setTooltip(new Tooltip("Close split"));
        unsplit.setStyle("-fx-font-size: 10px;");
        unsplit.setOnAction(e -> closeSplit(initial));

        toolbar.getChildren().addAll(splitH, splitV, unsplit);
        setTop(toolbar);

        editors.add(initial);
        splitPane.getItems().add(initial);
        setCenter(splitPane);
    }

    private void splitHorizontal(CodeEditor source) {
        int idx = splitPane.getItems().indexOf(source);
        if (idx < 0) return;
        CodeEditor clone = createClone(source);
        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.getItems().add(idx + 1, clone);
        editors.add(clone);
        splitPane.setDividerPositions(0.5);
    }

    private void splitVertical(CodeEditor source) {
        int idx = splitPane.getItems().indexOf(source);
        if (idx < 0) return;
        CodeEditor clone = createClone(source);
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.getItems().add(idx + 1, clone);
        editors.add(clone);
        splitPane.setDividerPositions(0.5);
    }

    private void closeSplit(CodeEditor source) {
        if (editors.size() <= 1) return;
        int idx = splitPane.getItems().indexOf(source);
        if (idx >= 0) {
            splitPane.getItems().remove(idx);
            editors.remove(source);
        }
        if (editors.size() == 1) {
            splitPane.setOrientation(Orientation.HORIZONTAL);
        }
    }

    private CodeEditor createClone(CodeEditor source) {
        CodeEditor clone = new CodeEditor(source.getLanguage());
        clone.applySettings();
        clone.setText(source.getText());
        return clone;
    }

    public List<CodeEditor> getEditors() {
        return editors;
    }

    public CodeEditor getActiveEditor() {
        return editors.isEmpty() ? null : editors.get(0);
    }

    public void setText(String text) {
        for (CodeEditor ed : editors) {
            ed.setText(text);
        }
    }

    public void setFontSize(double size) {
        for (CodeEditor ed : editors) {
            ed.setFontSize(size);
        }
    }
}
