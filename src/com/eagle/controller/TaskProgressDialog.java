package com.eagle.controller;

import com.eagle.icons.IconManager;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

public class TaskProgressDialog {

    private final Stage stage;
    private final VBox root;
    private final VBox taskList;
    private final Label titleLabel;
    private final ProgressBar overallProgress;
    private final Label progressText;
    private final TextArea logArea;
    private final Label elapsedLabel;
    private final List<TaskEntry> tasks = new ArrayList<>();
    private final Timeline elapsedTimer = new Timeline();
    private long startTime;

    private static class TaskEntry {
        final String name;
        final HBox row;
        final Label badge;
        final Label statusIcon;
        final Label nameLabel;
        final ProgressBar progress;
        final Label percentLabel;
        TaskStatus status = TaskStatus.PENDING;

        TaskEntry(String name, HBox row, Label badge, Label statusIcon, Label nameLabel,
                  ProgressBar progress, Label percentLabel) {
            this.name = name;
            this.row = row;
            this.badge = badge;
            this.statusIcon = statusIcon;
            this.nameLabel = nameLabel;
            this.progress = progress;
            this.percentLabel = percentLabel;
        }
    }

    public enum TaskStatus {
        PENDING("[ ]", "#6b6d80"),
        RUNNING("[>]", "#6c5ce7"),
        COMPLETED("[OK]", "#2ecc71"),
        FAILED("[!!]", "#e74c3c"),
        SKIPPED("[--]", "#f39c12");

        final String symbol;
        final String color;
        TaskStatus(String symbol, String color) {
            this.symbol = symbol;
            this.color = color;
        }
    }

    public TaskProgressDialog(String title) {
        this(title, 560, 580);
    }

    public TaskProgressDialog(String title, double width, double height) {
        root = new VBox();
        root.getStyleClass().add("tp-root");

        // ===== HEADER =====
        VBox header = new VBox();
        header.getStyleClass().add("tp-header");

        HBox titleBar = new HBox(10);
        titleBar.setAlignment(Pos.CENTER_LEFT);

        VBox titleGroup = new VBox(2);
        titleLabel = new Label(title);
        titleLabel.getStyleClass().add("title");

        elapsedLabel = new Label("00:00");
        elapsedLabel.getStyleClass().add("elapsed");

        titleGroup.getChildren().addAll(titleLabel, elapsedLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("close-btn");
        closeBtn.setOnAction(e -> close());

        titleBar.getChildren().addAll(titleGroup, spacer, closeBtn);

        Label subtitle = new Label("Building your APK - track progress below");
        subtitle.getStyleClass().add("subtitle");

        header.getChildren().addAll(titleBar, subtitle);

        // ===== TASK LIST =====
        taskList = new VBox();
        taskList.getStyleClass().add("tp-tasklist");

        ScrollPane scrollPane = new ScrollPane(taskList);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-border: none; -fx-background-color: transparent;");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPrefHeight(200);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // ===== OVERALL PROGRESS =====
        VBox overallSection = new VBox(8);
        overallSection.getStyleClass().add("tp-overall-card");

        HBox overallHeader = new HBox(10);
        overallHeader.setAlignment(Pos.CENTER_LEFT);
        Label overallLabel = new Label("OVERALL PROGRESS");
        overallLabel.getStyleClass().add("tp-overall-label");

        Region sp2 = new Region();
        HBox.setHgrow(sp2, Priority.ALWAYS);

        progressText = new Label("0%");
        progressText.getStyleClass().add("tp-overall-value");
        overallHeader.getChildren().addAll(overallLabel, sp2, progressText);

        overallProgress = new ProgressBar(0);
        overallProgress.getStyleClass().add("tp-overall-bar");

        overallSection.getChildren().addAll(overallHeader, overallProgress);

        // ===== LOG =====
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(90);
        logArea.setWrapText(true);
        logArea.getStyleClass().add("tp-log-area");

        VBox logSection = new VBox(6);
        logSection.getStyleClass().add("tp-log-card");

        HBox logHeader = new HBox(10);
        logHeader.setAlignment(Pos.CENTER_LEFT);
        Label logLabel = new Label("BUILD LOG");
        logLabel.getStyleClass().add("tp-log-label");

        Region sp3 = new Region();
        HBox.setHgrow(sp3, Priority.ALWAYS);

        Button clearLog = new Button("Clear");
        clearLog.getStyleClass().add("tp-log-clear");
        clearLog.setOnAction(e -> logArea.clear());

        logHeader.getChildren().addAll(logLabel, sp3, clearLog);
        logSection.getChildren().addAll(logHeader, logArea);

        // ===== ASSEMBLE =====
        root.getChildren().addAll(header, scrollPane, overallSection, logSection);

        Scene scene = new Scene(root, width, height);
        scene.setFill(Color.TRANSPARENT);

        // Load dialog CSS using same path as FXML would
        String cssPath = getClass().getResource("/com/eagle/css/apk-dialog.css").toExternalForm();
        if (cssPath != null) {
            scene.getStylesheets().add(cssPath);
        }

        stage = new Stage();
        //stage.initStyle(StageStyle.TRANSPARENT);
        stage.setScene(scene);
        stage.centerOnScreen();

        // Drag support
        DragContext dc = new DragContext();
        header.setOnMousePressed(e -> { dc.x = e.getSceneX(); dc.y = e.getSceneY(); });
        header.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - dc.x);
            stage.setY(e.getScreenY() - dc.y);
        });

        // Elapsed timer
        elapsedTimer.setCycleCount(Timeline.INDEFINITE);
        elapsedTimer.getKeyFrames().add(new KeyFrame(Duration.seconds(1), e -> {
            long elapsed = System.currentTimeMillis() - startTime;
            long mins = (elapsed / 1000) / 60;
            long secs = (elapsed / 1000) % 60;
            elapsedLabel.setText(String.format("%02d:%02d", mins, secs));
        }));
    }

    public void addTask(String name) {
        int index = tasks.size() + 1;

        HBox row = new HBox();
        row.getStyleClass().add("tp-task-row");

        Label badge = new Label(String.valueOf(index));
        badge.getStyleClass().addAll("tp-badge", "pending");

        Label icon = new Label(TaskStatus.PENDING.symbol);
        icon.setStyle("-fx-text-fill: " + TaskStatus.PENDING.color + ";");
        icon.getStyleClass().add("tp-status-icon");
        icon.setMinWidth(20);

        Label lbl = new Label(name);
        lbl.getStyleClass().add("tp-task-name");
        HBox.setHgrow(lbl, Priority.ALWAYS);

        ProgressBar pb = new ProgressBar(0);
        pb.getStyleClass().add("tp-progress-bar");

        Label pct = new Label("0%");
        pct.getStyleClass().add("tp-percent");

        row.getChildren().addAll(badge, icon, lbl, pb, pct);
        taskList.getChildren().add(row);
        tasks.add(new TaskEntry(name, row, badge, icon, lbl, pb, pct));
    }

    public void setTaskStatus(int index, TaskStatus status) {
        if (index < 0 || index >= tasks.size()) return;
        TaskEntry t = tasks.get(index);
        t.status = status;
        Platform.runLater(() -> {
            t.statusIcon.setText(status.symbol);
            t.statusIcon.setStyle("-fx-text-fill: " + status.color + ";");

            t.badge.getStyleClass().removeAll("pending", "running", "completed", "failed", "skipped");
            t.nameLabel.getStyleClass().removeAll("running", "completed", "failed", "skipped");
            t.row.getStyleClass().removeAll("running", "completed", "failed", "skipped");

            switch (status) {
                case RUNNING:
                    t.badge.getStyleClass().add("running");
                    t.nameLabel.getStyleClass().add("running");
                    t.row.getStyleClass().add("running");
                    t.progress.setStyle("-fx-accent: -accent;");
                    t.percentLabel.setStyle("-fx-text-fill: -accent;");
                    break;
                case COMPLETED:
                    t.badge.getStyleClass().add("completed");
                    t.nameLabel.getStyleClass().add("completed");
                    t.row.getStyleClass().add("completed");
                    t.progress.setStyle("-fx-accent: -success;");
                    t.progress.setProgress(1.0);
                    t.percentLabel.setText("100%");
                    t.percentLabel.setStyle("-fx-text-fill: -success;");
                    break;
                case FAILED:
                    t.badge.getStyleClass().add("failed");
                    t.nameLabel.getStyleClass().add("failed");
                    t.row.getStyleClass().add("failed");
                    t.progress.setStyle("-fx-accent: -danger;");
                    t.percentLabel.setStyle("-fx-text-fill: -danger;");
                    break;
                case SKIPPED:
                    t.badge.getStyleClass().add("skipped");
                    t.nameLabel.getStyleClass().add("skipped");
                    t.row.getStyleClass().add("skipped");
                    t.progress.setStyle("-fx-accent: -warning;");
                    t.percentLabel.setStyle("-fx-text-fill: -warning;");
                    break;
            }
        });
    }

    public void setTaskProgress(int index, double progress) {
        if (index < 0 || index >= tasks.size()) return;
        TaskEntry t = tasks.get(index);
        double p = clamp(progress);
        Platform.runLater(() -> {
            t.progress.setProgress(p);
            t.percentLabel.setText(String.format("%.0f%%", p * 100));
        });
    }

    public void setOverallProgress(double progress) {
        double p = clamp(progress);
        Platform.runLater(() -> {
            overallProgress.setProgress(p);
            progressText.setText(String.format("%.0f%%", p * 100));
        });
    }

    public void setThemeStylesheets(java.util.List<String> stylesheets) {
        javafx.application.Platform.runLater(() -> {
            javafx.collections.ObservableList<String> sheets = stage.getScene().getStylesheets();
            if (stylesheets != null) {
                // Insert theme stylesheets at beginning (before apk-dialog.css)
                String dialogCss = getClass().getResource("/com/eagle/css/apk-dialog.css").toExternalForm();
                sheets.remove(dialogCss);
                sheets.addAll(0, stylesheets);
                sheets.add(dialogCss);
            }
        });
    }

    public void log(String message) {
        Platform.runLater(() -> {
            logArea.appendText("> " + message + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    public void logError(String message) {
        Platform.runLater(() -> {
            logArea.appendText("> " + message + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    public void logSuccess(String message) {
        Platform.runLater(() -> {
            logArea.appendText("> " + message + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    public void show() {
        Platform.runLater(() -> {
            startTime = System.currentTimeMillis();
            elapsedTimer.play();
            stage.show();
            FadeTransition ft = new FadeTransition(Duration.millis(300), root);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        });
    }

    public void close() {
        elapsedTimer.stop();
        Platform.runLater(() -> {
            FadeTransition ft = new FadeTransition(Duration.millis(200), root);
            ft.setFromValue(1);
            ft.setToValue(0);
            ft.setOnFinished(e -> stage.close());
            ft.play();
        });
    }

    public Stage getStage() { return stage; }
    public int getTaskCount() { return tasks.size(); }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }

    private static class DragContext { double x, y; }
}
