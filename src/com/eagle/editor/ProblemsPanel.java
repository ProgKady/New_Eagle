package com.eagle.editor;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import org.fxmisc.richtext.CodeArea;

import java.util.List;

public class ProblemsPanel extends VBox {

    private final ListView<ProblemItem> problemList = new ListView<>();
    private final Label summaryLabel = new Label("0 problems");
    private CodeArea linkedCodeArea;

    public ProblemsPanel() {
        getStyleClass().add("problems-panel");
        setPrefHeight(120);
        setMinHeight(30);

        summaryLabel.getStyleClass().add("problems-summary");
        problemList.getStyleClass().add("problems-list");
        problemList.setCellFactory(lv -> new ListCell<ProblemItem>() {
            @Override
            protected void updateItem(ProblemItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("[" + item.severity + "] Line " + item.line + ": " + item.message);
                    String prefix = item.severity.equals("ERROR") ? "[ERR]" : item.severity.equals("WARNING") ? "[WARN]" : "[INFO]";
                    setText(prefix + "  " + getText());
                    if (item.severity.equals("ERROR")) setStyle("-fx-text-fill: #e74c3c;");
                    else if (item.severity.equals("WARNING")) setStyle("-fx-text-fill: #f1c40f;");
                    else setStyle("-fx-text-fill: #3498db;");
                }
            }
        });
        problemList.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && linkedCodeArea != null) {
                ProblemItem item = problemList.getSelectionModel().getSelectedItem();
                if (item != null && item.line > 0) {
                    int pos = 0;
                    for (int i = 0; i < item.line - 1; i++) {
                        String p = linkedCodeArea.getParagraph(i).getText();
                        pos += p.length() + 1;
                    }
                    linkedCodeArea.moveTo(pos);
                    linkedCodeArea.requestFollowCaret();
                    linkedCodeArea.requestFocus();
                }
            }
        });

        getChildren().addAll(summaryLabel, problemList);
    }

    public void linkTo(CodeArea codeArea) {
        this.linkedCodeArea = codeArea;
    }

    public void setProblems(List<CodeLinter.Problem> problems) {
        ObservableList<ProblemItem> items = FXCollections.observableArrayList();
        int errors = 0, warnings = 0;
        for (CodeLinter.Problem p : problems) {
            items.add(new ProblemItem(p.line, p.message, p.severity.name()));
            if (p.severity == CodeLinter.Problem.Severity.ERROR) errors++;
            else if (p.severity == CodeLinter.Problem.Severity.WARNING) warnings++;
        }
        problemList.setItems(items);
        StringBuilder sb = new StringBuilder();
        if (errors > 0) sb.append(errors).append(" error(s)");
        if (warnings > 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(warnings).append(" warning(s)");
        }
        if (sb.length() == 0) sb.append("No problems");
        summaryLabel.setText(sb.toString());
    }

    private static class ProblemItem {
        final int line;
        final String message;
        final String severity;

        ProblemItem(int line, String message, String severity) {
            this.line = line;
            this.message = message;
            this.severity = severity;
        }
    }
}