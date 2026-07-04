package com.eagle.controller;

import javafx.geometry.Insets;
import javafx.scene.control.IndexRange;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * Lightweight code editor: plain TextArea + synced line-number gutter.
 * No external libraries required (pure JavaFX 8).
 */
public class CodeEditor extends BorderPane {

    private final TextArea textArea = new TextArea();
    private final VBox lineNumbers = new VBox();
    private final ScrollPane gutterScroll = new ScrollPane();

    public CodeEditor() {
        getStyleClass().add("code-editor-root");

        textArea.getStyleClass().add("code-area");
        textArea.setWrapText(false);

        lineNumbers.getStyleClass().add("lineno");
        lineNumbers.setFillWidth(true);

        gutterScroll.setContent(lineNumbers);
        gutterScroll.setFitToWidth(true);
        gutterScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        gutterScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        gutterScroll.setPannable(false);
        gutterScroll.setPrefWidth(46);
        gutterScroll.getStyleClass().add("lineno-scroll");
        gutterScroll.setStyle("-fx-background-color: transparent;");

        setLeft(gutterScroll);
        setCenter(textArea);

        updateLineNumbers();

        textArea.textProperty().addListener((obs, old, val) -> updateLineNumbers());

        // Sync gutter scroll with text area scroll (approximate, via skin lookup)
        textArea.skinProperty().addListener((obs, oldSkin, newSkin) -> attachScrollSync());
    }

    private void attachScrollSync() {
        ScrollPane internalScroll = (ScrollPane) textArea.lookup(".scroll-pane");
        if (internalScroll != null) {
            internalScroll.vvalueProperty().addListener((obs, old, val) ->
                    gutterScroll.setVvalue(val.doubleValue()));
        }
    }

    private void updateLineNumbers() {
        int lines = countLines(textArea.getText());
        lineNumbers.getChildren().clear();
        for (int i = 1; i <= lines; i++) {
            Text t = new Text(String.valueOf(i));
            t.setStyle("-fx-fill: -text-muted;");
            VBox wrap = new VBox(t);
            wrap.setPadding(new Insets(0, 8, 0, 8));
            wrap.setMinHeight(Font.getDefault().getSize() * 1.65);
            lineNumbers.getChildren().add(wrap);
        }
    }

    private int countLines(String text) {
        if (text == null || text.isEmpty()) return 1;
        int count = 1;
        for (char c : text.toCharArray()) {
            if (c == '\n') count++;
        }
        return count;
    }

    public TextArea getTextArea() {
        return textArea;
    }

    public String getText() {
        return textArea.getText();
    }

    public void setText(String text) {
        textArea.setText(text);
    }

    public javafx.beans.property.StringProperty textProperty() {
        return textArea.textProperty();
    }

    public void insertTab() {
        IndexRange range = textArea.getSelection();
        textArea.replaceText(range, "    ");
    }
}
