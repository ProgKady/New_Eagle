package com.eagle.editor;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import com.eagle.icons.IconManager;
import org.fxmisc.richtext.CodeArea;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Find/Replace overlay bar for CodeEditor. Highlights matches and supports
 * next/previous navigation, regex search, and single/replace-all.
 */
public class FindReplacePanel extends HBox {

    private final CodeArea codeArea;
    private final TextField findField = new TextField();
    private final TextField replaceField = new TextField();
    private final Label countLabel = new Label("0/0");
    private final CheckBox regexCheck = new CheckBox(".*");
    private final Label errorLabel = new Label();

    private final List<int[]> matches = new ArrayList<>(); // [start, end]
    private int currentIndex = -1;

    public FindReplacePanel(CodeArea codeArea) {
        this.codeArea = codeArea;
        getStyleClass().add("find-replace-panel");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(6);
        setPadding(new Insets(6, 10, 6, 10));

        findField.setPromptText("Find...");
        findField.setPrefWidth(160);
        replaceField.setPromptText("Replace...");
        replaceField.setPrefWidth(160);

        regexCheck.setTooltip(new Tooltip("Enable regex search"));
        regexCheck.setStyle("-fx-font-size: 11px; -fx-font-family: monospace;");
        regexCheck.setOnAction(e -> search(findField.getText()));

        errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 10px;");
        errorLabel.setVisible(false);

        Button prevBtn = new Button("Prev");
        Button nextBtn = new Button("Next");
        Button replaceBtn = new Button("Replace");
        Button replaceAllBtn = new Button("Replace All");
        Button closeBtn = new Button("Close");

        findField.textProperty().addListener((obs, old, val) -> search(val));
        findField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) goToNext();
            if (e.getCode() == KeyCode.ESCAPE) hidePanel();
        });

        prevBtn.setOnAction(e -> goToPrevious());
        nextBtn.setOnAction(e -> goToNext());
        replaceBtn.setOnAction(e -> replaceCurrent());
        replaceAllBtn.setOnAction(e -> replaceAll());
        closeBtn.setOnAction(e -> hidePanel());

        getChildren().addAll(findField, regexCheck, prevBtn, nextBtn, countLabel, errorLabel,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                replaceField, replaceBtn, replaceAllBtn, closeBtn);

        setVisible(false);
        setManaged(false);
    }

    public void showPanel() {
        setVisible(true);
        setManaged(true);
        findField.requestFocus();
        findField.selectAll();
        if (!findField.getText().isEmpty()) search(findField.getText());
    }

    public void hidePanel() {
        setVisible(false);
        setManaged(false);
        codeArea.requestFocus();
    }

    private void search(String query) {
        matches.clear();
        currentIndex = -1;
        errorLabel.setVisible(false);
        if (query == null || query.isEmpty()) {
            countLabel.setText("0/0");
            return;
        }
        String text = codeArea.getText();

        if (regexCheck.isSelected()) {
            try {
                Pattern pattern = Pattern.compile(query);
                java.util.regex.Matcher m = pattern.matcher(text);
                while (m.find()) {
                    matches.add(new int[]{m.start(), m.end()});
                }
            } catch (PatternSyntaxException e) {
                errorLabel.setText("Invalid regex: " + e.getMessage());
                errorLabel.setVisible(true);
                countLabel.setText("0/0");
                return;
            }
        } else {
            String lowerText = text.toLowerCase();
            String lowerQuery = query.toLowerCase();
            int idx = 0;
            while ((idx = lowerText.indexOf(lowerQuery, idx)) != -1) {
                matches.add(new int[]{idx, idx + query.length()});
                idx += query.length();
            }
        }
        countLabel.setText((matches.isEmpty() ? 0 : 1) + "/" + matches.size());
        if (!matches.isEmpty()) {
            currentIndex = 0;
            selectMatch(currentIndex);
        }
    }

    private void selectMatch(int index) {
        if (index < 0 || index >= matches.size()) return;
        int[] m = matches.get(index);
        codeArea.selectRange(m[0], m[1]);
        codeArea.requestFollowCaret();
        countLabel.setText((index + 1) + "/" + matches.size());
    }

    private void goToNext() {
        if (matches.isEmpty()) return;
        currentIndex = (currentIndex + 1) % matches.size();
        selectMatch(currentIndex);
    }

    private void goToPrevious() {
        if (matches.isEmpty()) return;
        currentIndex = (currentIndex - 1 + matches.size()) % matches.size();
        selectMatch(currentIndex);
    }

    private void replaceCurrent() {
        if (currentIndex < 0 || currentIndex >= matches.size()) return;
        int[] m = matches.get(currentIndex);
        String replacement = replaceField.getText();
        if (regexCheck.isSelected()) {
            String query = findField.getText();
            try {
                Pattern p = Pattern.compile(query);
                String matched = codeArea.getText().substring(m[0], m[1]);
                String processed = matched.replaceAll(query, replacement);
                codeArea.replaceText(m[0], m[1], processed);
            } catch (Exception e) {
                codeArea.replaceText(m[0], m[1], replacement);
            }
        } else {
            codeArea.replaceText(m[0], m[1], replacement);
        }
        search(findField.getText());
    }

    private void replaceAll() {
        String query = findField.getText();
        String replacement = replaceField.getText();
        if (query == null || query.isEmpty()) return;
        String text = codeArea.getText();
        String result;
        if (regexCheck.isSelected()) {
            try {
                result = text.replaceAll(query, replacement);
            } catch (Exception e) {
                errorLabel.setText("Regex error: " + e.getMessage());
                errorLabel.setVisible(true);
                return;
            }
        } else {
            result = text.replace(query, replacement);
        }
        codeArea.replaceText(result);
        search(findField.getText());
    }
}
