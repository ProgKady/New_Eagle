package com.eagle.editor;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

public class GitPanel extends VBox {

    private final TextArea outputArea = new TextArea();
    private final TextArea commitArea = new TextArea();
    private final TextField branchField = new TextField();
    private final Label branchIcon = new Label("\u25cf");
    private final Label aheadLabel = new Label("");
    private final Label stagedCount = new Label("0 staged");
    private final Label modifiedCount = new Label("0 modified");
    private final Label conflictsLabel = new Label();
    private final ListView<String> fileStatusList = new ListView<>();
    private final ObservableList<String> fileStatusItems = FXCollections.observableArrayList();
    private final ComboBox<String> stashCombo = new ComboBox<>();
    private final ObservableList<String> stashItems = FXCollections.observableArrayList();
    private final CheckBox amendCheck = new CheckBox("Amend");
    private final CheckBox signOffCheck = new CheckBox("Sign-off");
    private final Button commitBtn = new Button("Commit");
    private File projectDir;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public GitPanel() {
        getStyleClass().add("git-panel");
        setPrefHeight(200);
        setMinHeight(80);

        buildUI();
    }

    private void buildUI() {
        // ── Branch & status bar ──
        branchIcon.setStyle("-fx-font-size: 14px; -fx-text-fill: #2ea043;");
        branchIcon.setTooltip(new Tooltip("Current branch"));
        branchField.setEditable(false);
        branchField.setPrefColumnCount(18);
        branchField.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        aheadLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-muted;");
        stagedCount.setStyle("-fx-font-size: 10px; -fx-text-fill: #2ea043;");
        modifiedCount.setStyle("-fx-font-size: 10px; -fx-text-fill: #e3b341;");
        conflictsLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #da3633;");
        conflictsLabel.setVisible(false);

        HBox branchBar = new HBox(6, branchIcon, branchField, aheadLabel, stagedCount, modifiedCount, conflictsLabel);
        branchBar.setAlignment(Pos.CENTER_LEFT);
        branchBar.setPadding(new Insets(4, 8, 4, 8));
        branchBar.setStyle("-fx-background-color: -bg-secondary; -fx-border-color: -border; -fx-border-width: 0 0 1 0;");
        HBox.setHgrow(branchField, Priority.NEVER);

        // ── File status list ──
        fileStatusList.setItems(fileStatusItems);
        fileStatusList.setPrefHeight(120);
        fileStatusList.setPlaceholder(new Label("No changes"));
        fileStatusList.setCellFactory(list -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    char status = item.length() > 2 ? item.charAt(0) : ' ';
                    String path = item.length() > 3 ? item.substring(4) : item;
                    String icon;
                    String color;
                    switch (status) {
                        case 'M': icon = "\u270e"; color = "#e3b341"; break;
                        case 'A': icon = "\u2795"; color = "#2ea043"; break;
                        case 'D': icon = "\u2716"; color = "#da3633"; break;
                        case 'R': icon = "\u21c5"; color = "#58a6ff"; break;
                        case 'C': icon = "\u00a9"; color = "#58a6ff"; break;
                        case 'U': icon = "\u26a0"; color = "#da3633"; break;
                        case '?': icon = "\u2753"; color = "#8b949e"; break;
                        default:  icon = "\u25cf"; color = "#8b949e"; break;
                    }
                    if (item.length() > 2 && item.charAt(1) != ' ') {
                        if (item.charAt(1) == 'U') { icon = "\u26a0"; color = "#da3633"; }
                    }
                    Label iconLbl = new Label(icon);
                    iconLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
                    Label pathLbl = new Label(path);
                    pathLbl.setStyle("-fx-font-size: 11px;");
                    HBox cell = new HBox(6, iconLbl, pathLbl);
                    cell.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(cell);
                    setText(null);
                }
            }
        });
        fileStatusList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String sel = fileStatusList.getSelectionModel().getSelectedItem();
                if (sel != null && sel.length() > 4) {
                    showDiff(sel.substring(4));
                }
            }
        });

        // ── Commit area ──
        commitArea.setPromptText("Commit message...");
        commitArea.setPrefRowCount(3);
        commitArea.setPrefHeight(60);
        commitArea.setWrapText(true);
        commitArea.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER && (e.isControlDown() || e.isMetaDown())) {
                doCommit();
                e.consume();
            }
        });

        HBox commitOpts = new HBox(10, amendCheck, signOffCheck);
        commitOpts.setAlignment(Pos.CENTER_LEFT);

        commitBtn.setOnAction(e -> doCommit());

        VBox commitBox = new VBox(4, commitArea, commitOpts, commitBtn);
        commitBox.setPadding(new Insets(4, 8, 4, 8));

        TitledPane commitPane = new TitledPane("Commit", commitBox);
        commitPane.setCollapsible(true);
        commitPane.setExpanded(true);

        // ── Action buttons ──
        Button pushBtn = createBtn("Push", "Push to remote", e -> doPush());
        Button pullBtn = createBtn("Pull", "Pull from remote", e -> doPull());
        Button fetchBtn = createBtn("Fetch", "Fetch from remote", e -> doFetch());
        Button branchBtn = createBtn("Branch", "Manage branches", e -> doBranch());
        Button stashBtn = createBtn("Stash", "Stash changes", e -> doStash());
        Button stashPopBtn = createBtn("Pop", "Pop stash", e -> doStashPop());
        Button diffBtn = createBtn("Diff", "Show diff of selected file", e -> {
            String sel = fileStatusList.getSelectionModel().getSelectedItem();
            showDiff(sel != null && sel.length() > 4 ? sel.substring(4) : "");
        });
        Button logBtn = createBtn("Log", "Show recent commits", e -> doLog());
        Button cherryBtn = createBtn("Cherry", "Cherry-pick a commit", e -> doCherryPick());
        Button resetBtn = createBtn("Reset", "Reset changes", e -> doReset());
        Button initBtn = createBtn("Init", "Init repository", e -> showInitDialog());
        Button cloneBtn = createBtn("Clone", "Clone repository", e -> showCloneDialog());

        FlowPane actionBar = new FlowPane(4, 4, pushBtn, pullBtn, fetchBtn, branchBtn, stashBtn, stashPopBtn,
            diffBtn, logBtn, cherryBtn, resetBtn, initBtn, cloneBtn);
        actionBar.setPadding(new Insets(4, 8, 4, 8));
        actionBar.setStyle("-fx-background-color: -bg-tertiary;");

        // ── Stash list ──
        stashCombo.setItems(stashItems);
        stashCombo.setPromptText("Stash list...");
        stashCombo.setPrefWidth(300);
        Button stashDropBtn = createBtn("Drop", "Drop selected stash", e -> doStashDrop());

        HBox stashBar = new HBox(4, new Label("Stash:"), stashCombo, stashDropBtn);
        stashBar.setAlignment(Pos.CENTER_LEFT);
        stashBar.setPadding(new Insets(2, 8, 2, 8));
        stashBar.setVisible(false);
        stashBar.setManaged(false);
        stashBar.setId("stashBar");

        // ── Output area ──
        outputArea.setEditable(false);
        outputArea.setFont(Font.font("Consolas", 12));
        outputArea.setStyle("-fx-text-fill: #e0e0e0; -fx-control-inner-background: #1e1e1e;");
        outputArea.setPrefRowCount(8);

        TitledPane outputPane = new TitledPane("Output", outputArea);
        outputPane.setCollapsible(true);
        outputPane.setExpanded(true);

        VBox.setVgrow(outputArea, Priority.ALWAYS);

        getChildren().addAll(branchBar, fileStatusList, commitPane, actionBar, stashBar, outputPane);
    }

    private Button createBtn(String text, String tooltip, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(text);
        if (tooltip != null) btn.setTooltip(new Tooltip(tooltip));
        btn.setOnAction(handler);
        btn.setStyle("-fx-font-size: 10px; -fx-padding: 2 6;");
        return btn;
    }

    // ── Public API ──

    public void setProjectDir(File dir) {
        this.projectDir = dir;
        refreshAll();
    }

    public void refreshAll() {
        refreshBranch();
        refreshStatus();
        refreshStashList();
    }

    public void refreshBranch() {
        if (projectDir == null) return;
        runGit("rev-parse --abbrev-ref HEAD", false, (exit, out) -> {
            if (exit == 0 && !out.trim().isEmpty()) {
                Platform.runLater(() -> branchField.setText(out.trim()));
            }
        });
        runGit("rev-list --count --left-right HEAD...@{upstream}", false, (exit, out) -> {
            if (exit == 0 && out.contains("\t")) {
                String[] parts = out.trim().split("\\s+");
                String ahead = parts.length > 0 ? parts[0] : "0";
                String behind = parts.length > 1 ? parts[1] : "0";
                Platform.runLater(() -> aheadLabel.setText("\u2191" + ahead + " \u2193" + behind));
            } else {
                Platform.runLater(() -> aheadLabel.setText(""));
            }
        });
    }

    public void refreshStatus() {
        if (projectDir == null) return;
        runGit("status --porcelain -b", false, (exit, out) -> {
            if (exit != 0) return;
            String[] lines = out.split("\n");
            List<String> files = new ArrayList<>();
            int stagedTotal = 0;
            int modifiedTotal = 0;
            int conflictTotal = 0;
            for (String line : lines) {
                if (line.startsWith("##")) continue;
                if (line.trim().isEmpty()) continue;
                char staged = line.length() > 0 ? line.charAt(0) : ' ';
                char unstaged = line.length() > 1 ? line.charAt(1) : ' ';
                char display = staged != ' ' ? staged : (unstaged != ' ' ? unstaged : '?');
                if (staged != ' ' && staged != '?' && staged != '!') stagedTotal++;
                else if (unstaged != ' ' && unstaged != '?' && unstaged != '!') modifiedTotal++;
                else if (staged == '?' || unstaged == '?') modifiedTotal++;
                if (staged == 'U' || unstaged == 'U' || staged == 'D' || unstaged == 'D') {
                    conflictTotal++;
                    display = 'U';
                }
                files.add(display + " | " + line.substring(3));
            }
            final int sTotal = stagedTotal;
            final int mTotal = modifiedTotal;
            final int cTotal = conflictTotal;
            final List<String> fList = files;
            Platform.runLater(() -> {
                fileStatusItems.setAll(fList);
                stagedCount.setText(sTotal + " staged");
                modifiedCount.setText(mTotal + " modified");
                if (cTotal > 0) {
                    conflictsLabel.setText(cTotal + " conflict(s)");
                    conflictsLabel.setVisible(true);
                } else {
                    conflictsLabel.setVisible(false);
                }
            });
        });
    }

    public void refreshStashList() {
        if (projectDir == null) return;
        runGit("stash list", false, (exit, out) -> {
            if (exit != 0 || out.trim().isEmpty()) {
                Platform.runLater(() -> {
                    stashItems.clear();
                    stashCombo.setVisible(false);
                    stashCombo.setManaged(false);
                });
                return;
            }
            String[] lines = out.split("\n");
            final List<String> stashes = new ArrayList<>();
            for (String l : lines) {
                if (!l.trim().isEmpty()) stashes.add(l.trim());
            }
            Platform.runLater(() -> {
                stashItems.setAll(stashes);
                stashCombo.setVisible(true);
                stashCombo.setManaged(true);
            });
        });
    }

    // ── Git Execution ──

    private void runGit(String args) {
        runGit(args, true, null);
    }

    private void runGit(String args, boolean showOutput, final GitCallback callback) {
        if (projectDir == null) {
            if (showOutput) appendOutput("[error] No project open.\n");
            return;
        }
        final String displayCmd = "git " + args;
        if (showOutput) appendOutput("$ " + displayCmd + "\n");
        executor.submit(() -> {
            try {
                java.util.List<String> cmd = new java.util.ArrayList<>();
                cmd.add("git");
                java.util.Collections.addAll(cmd, args.split(" "));
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.directory(projectDir);
                pb.redirectErrorStream(true);
                Process p = pb.start();
                StringBuilder sb = new StringBuilder();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line).append("\n");
                }
                int exit = p.waitFor();
                String out = sb.toString();
                if (showOutput && !out.isEmpty()) {
                    Platform.runLater(() -> appendOutput(out));
                }
                if (showOutput && exit != 0) {
                    Platform.runLater(() -> appendOutput("[exit code: " + exit + "]\n"));
                }
                if (callback != null) {
                    callback.onComplete(exit, out);
                }
            } catch (Exception e) {
                if (showOutput) Platform.runLater(() -> appendOutput("[error] " + e.getMessage() + "\n"));
                if (callback != null) callback.onComplete(-1, e.getMessage());
            }
        });
    }

    // ── Actions ──

    private void doCommit() {
        String msg = commitArea.getText().trim();
        if (msg.isEmpty()) {
            appendOutput("[error] Enter a commit message first.\n");
            return;
        }
        String safeMsg = msg.replace("\"", "\\\"");
        if (amendCheck.isSelected()) {
            runGit("commit --amend -m \"" + safeMsg + "\"");
        } else {
            runGit("add -A");
            if (signOffCheck.isSelected()) {
                runGit("commit -s -m \"" + safeMsg + "\"");
            } else {
                runGit("commit -m \"" + safeMsg + "\"");
            }
        }
        commitArea.clear();
        amendCheck.setSelected(false);
        refreshStatus();
        refreshBranch();
    }

    private void doPush() {
        runGit("push", true, null);
        refreshBranch();
    }

    private void doPull() {
        runGit("pull --ff-only", true, null);
        refreshAll();
    }

    private void doFetch() {
        runGit("fetch --all --prune", true, null);
        refreshBranch();
    }

    private void doBranch() {
        if (projectDir == null) return;
        runGit("branch -a", true, null);
    }

    private void doStash() {
        if (projectDir == null) return;
        String msg = commitArea.getText().trim();
        if (msg.isEmpty()) {
            runGit("stash push -m \"WIP\"");
        } else {
            String safeMsg = msg.replace("\"", "\\\"");
            runGit("stash push -m \"" + safeMsg + "\"");
        }
        refreshAll();
    }

    private void doStashPop() {
        runGit("stash pop", true, null);
        refreshAll();
    }

    private void doStashDrop() {
        String sel = stashCombo.getSelectionModel().getSelectedItem();
        if (sel == null) {
            appendOutput("[error] Select a stash entry first.\n");
            return;
        }
        String ref = sel.split(":")[0];
        runGit("stash drop " + ref, true, null);
        refreshStashList();
    }

    private void showDiff(String path) {
        if (path.isEmpty()) {
            appendOutput("[error] Select a file first.\n");
            return;
        }
        runGit("diff -- \"" + path.replace("\"", "\\\"") + "\"", true, null);
    }

    private void doLog() {
        runGit("log --oneline --graph --all -20", true, null);
    }

    private void doCherryPick() {
        TextInputDialog d = new TextInputDialog();
        d.setTitle("Cherry-pick Commit");
        d.setHeaderText("Enter commit hash to cherry-pick:");
        d.setContentText("Commit:");
        d.showAndWait().ifPresent(hash -> {
            if (!hash.trim().isEmpty()) {
                runGit("cherry-pick " + hash.trim(), true, null);
                refreshAll();
            }
        });
    }

    private void doReset() {
        String[] choices = {"--soft (keep changes staged)", "--mixed (keep changes unstaged)", "--hard (discard all changes)"};
        ChoiceDialog<String> d = new ChoiceDialog<>(choices[1], choices);
        d.setTitle("Reset");
        d.setHeaderText("Choose reset mode:");
        d.setContentText("Mode:");
        d.showAndWait().ifPresent(choice -> {
            final String mode;
            if (choice.startsWith("--soft")) {
                mode = "--soft";
            } else if (choice.startsWith("--hard")) {
                mode = "--hard";
            } else {
                mode = "--mixed";
            }
            if ("--hard".equals(mode)) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "This will discard ALL uncommitted changes. Continue?");
                confirm.showAndWait().ifPresent(r -> {
                    if (r == ButtonType.OK) {
                        runGit("reset " + mode, true, null);
                        refreshAll();
                    }
                });
            } else {
                runGit("reset " + mode, true, null);
                refreshAll();
            }
        });
    }

    private void showCloneDialog() {
        TextInputDialog d = new TextInputDialog("https://github.com/user/repo.git");
        d.setTitle("Clone Repository");
        d.setHeaderText("Enter the Git repository URL:");
        d.setContentText("URL:");
        d.showAndWait().ifPresent(url -> {
            if (url.trim().isEmpty()) return;
            File parent = projectDir != null ? projectDir.getParentFile() : new File(".");
            String dirName = url.substring(url.lastIndexOf('/') + 1).replace(".git", "");
            final File target = new File(parent, dirName);
            appendOutput("Cloning " + url + " into " + target.getAbsolutePath() + "...\n");
            executor.submit(() -> {
                try {
                    Process p = new ProcessBuilder("git", "clone", url.trim(), target.getAbsolutePath())
                            .redirectErrorStream(true).start();
                    try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                        String l;
                        while ((l = r.readLine()) != null) { final String ll = l; Platform.runLater(() -> appendOutput(ll + "\n")); }
                    }
                    int exit = p.waitFor();
                    Platform.runLater(() -> {
                        appendOutput("Clone " + (exit == 0 ? "complete." : "failed.") + "\n");
                        if (exit == 0 && target.exists()) {
                            projectDir = target;
                            refreshAll();
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> appendOutput("[error] Clone failed: " + e.getMessage() + "\n"));
                }
            });
        });
    }

    private void showInitDialog() {
        TextInputDialog d = new TextInputDialog("https://github.com/user/repo.git");
        d.setTitle("Init + Set Remote");
        d.setHeaderText("Initialize git repo and set remote origin.\nEnter remote URL (or leave empty to init only):");
        d.setContentText("Remote URL:");
        d.showAndWait().ifPresent(url -> {
            appendOutput("Initializing git repository...\n");
            executor.submit(() -> {
                try {
                    Process p = new ProcessBuilder("git", "init").directory(projectDir).redirectErrorStream(true).start();
                    try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                        String l; while ((l = r.readLine()) != null) { final String ll = l; Platform.runLater(() -> appendOutput(ll + "\n")); }
                    }
                    p.waitFor();
                    if (!url.trim().isEmpty()) {
                        Platform.runLater(() -> appendOutput("Setting remote origin...\n"));
                        Process p2 = new ProcessBuilder("git", "remote", "add", "origin", url.trim())
                                .directory(projectDir).redirectErrorStream(true).start();
                        try (BufferedReader r = new BufferedReader(new InputStreamReader(p2.getInputStream()))) {
                            String l; while ((l = r.readLine()) != null) { final String ll = l; Platform.runLater(() -> appendOutput(ll + "\n")); }
                        }
                        p2.waitFor();
                    }
                    Platform.runLater(() -> { appendOutput("Done.\n"); refreshAll(); });
                } catch (Exception e) {
                    Platform.runLater(() -> appendOutput("[error] " + e.getMessage() + "\n"));
                }
            });
        });
    }

    private void appendOutput(String text) {
        outputArea.appendText(text);
    }

    // ── Callback Interface ──

    private interface GitCallback {
        void onComplete(int exitCode, String output);
    }
}
