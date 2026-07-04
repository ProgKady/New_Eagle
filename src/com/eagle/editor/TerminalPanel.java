package com.eagle.editor;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

import java.io.*;

public class TerminalPanel extends VBox {

    private final TabPane tabPane = new TabPane();
    private int terminalCounter = 0;
    private File workingDir;

    public TerminalPanel() {
        getStyleClass().add("terminal-panel");
        setPrefHeight(200);
        setMinHeight(80);

        Button newBtn = new Button("+ New Terminal");
        newBtn.setStyle("-fx-font-size: 10px;");
        newBtn.setOnAction(e -> newTerminal());

        HBox header = new HBox(8, newBtn);
        header.setPadding(new Insets(4, 8, 2, 8));
        header.setStyle("-fx-background-color: -bg-tertiary;");

        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);

        getChildren().addAll(header, tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
    }

    public void setWorkingDir(File dir) {
        this.workingDir = dir;
    }

    public void newTerminal() {
        terminalCounter++;
        Tab tab = new Tab("Terminal " + terminalCounter);
        TerminalSession session = new TerminalSession(workingDir);
        tab.setContent(session);
        tab.setOnClosed(e -> session.destroy());
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    public void runCommand(String cmd) {
        runCommand(cmd, null);
    }

    public void runCommand(String cmd, String extraPathEntry) {
        Tab selected = tabPane.getSelectionModel().getSelectedItem();
        TerminalSession session;
        if (selected == null || !(selected.getContent() instanceof TerminalSession)) {
            newTerminal();
            selected = tabPane.getSelectionModel().getSelectedItem();
            if (selected == null) return;
        }
        session = (TerminalSession) selected.getContent();
        if (extraPathEntry != null && !extraPathEntry.isEmpty()) {
            session.runCommand(cmd, workingDir, extraPathEntry);
        } else {
            session.runCommand(cmd);
        }
    }

    /** Returns the text content of the currently active terminal tab, or empty string. */
    public String getActiveTerminalOutput() {
        Tab selected = tabPane.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getContent() instanceof TerminalSession) {
            return ((TerminalSession) selected.getContent()).getOutput();
        }
        return "";
    }

    private static class TerminalSession extends VBox {
        private final TextArea outputArea = new TextArea();
        private final TextField inputField = new TextField();
        private final File sessionDir;
        private Process process;
        private Thread readerThread;
        private Thread errorThread;

        TerminalSession(File dir) {
            this.sessionDir = dir;
            outputArea.setEditable(false);
            outputArea.setFont(Font.font("Consolas", 13));
            outputArea.setStyle("-fx-text-fill: #e0e0e0; -fx-control-inner-background: #1e1e1e;");
            outputArea.setPrefRowCount(12);

            inputField.setFont(Font.font("Consolas", 13));
            inputField.setPromptText("Type a command and press Enter...");
            inputField.setStyle("-fx-text-fill: #e0e0e0; -fx-control-inner-background: #2d2d2d;");

            Label infoLabel = new Label("Working dir: " + (dir != null ? dir.getAbsolutePath() : "N/A"));
            infoLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-muted;");

            inputField.setOnAction(e -> executeCommand());

            setSpacing(2);
            setPadding(new Insets(4));
            getChildren().addAll(infoLabel, outputArea, inputField);
            VBox.setVgrow(outputArea, Priority.ALWAYS);

            appendOutput("Terminal ready" + (dir != null ? " in " + dir.getAbsolutePath() : "") + "\n");
        }

        private void executeCommand() {
            String cmd = inputField.getText().trim();
            if (cmd.isEmpty()) return;
            appendOutput("> " + cmd + "\n");
            inputField.clear();
            runCommand(cmd, sessionDir);
        }

        void runCommand(String cmd) {
            runCommand(cmd, sessionDir, null);
        }

        void runCommand(String cmd, File dir) {
            runCommand(cmd, dir, null);
        }

        void runCommand(String cmd, File dir, String extraPathEntry) {
            if (cmd == null || cmd.trim().isEmpty()) return;
            appendOutput("> " + cmd + "\n");
            try {
                String shell = System.getenv("COMSPEC");
                if (shell == null) shell = "cmd.exe";
                ProcessBuilder pb = new ProcessBuilder(shell, "/c", cmd)
                        .directory(dir)
                        .redirectErrorStream(true);
                if (extraPathEntry != null && !extraPathEntry.isEmpty()) {
                    String oldPath = pb.environment().get("PATH");
                    String newPath = extraPathEntry + ";" + (oldPath != null ? oldPath : "");
                    pb.environment().put("PATH", newPath);
                }
                process = pb.start();

                readerThread = new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            final String l = line;
                            Platform.runLater(() -> appendOutput(l + "\n"));
                        }
                    } catch (IOException ignored) {}
                });
                readerThread.setDaemon(true);
                readerThread.start();

            } catch (IOException ex) {
                appendOutput("[error] " + ex.getMessage() + "\n");
            }
        }

        private void appendOutput(String text) {
            outputArea.appendText(text);
        }

        void destroy() {
            if (process != null) process.destroyForcibly();
        }

        boolean isRunning() {
            return process != null && process.isAlive();
        }

        String getOutput() {
            return outputArea.getText();
        }
    }

    public void cancelCurrentCommand() {
        Tab selected = tabPane.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getContent() instanceof TerminalSession) {
            ((TerminalSession) selected.getContent()).destroy();
        }
    }

    public boolean isCommandRunning() {
        Tab selected = tabPane.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getContent() instanceof TerminalSession) {
            return ((TerminalSession) selected.getContent()).isRunning();
        }
        return false;
    }
}
