package com.eagle.tools;

import com.eagle.util.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class FileDiffTool {

    public static class DiffEntry {
        public enum Type { UNCHANGED, INSERT, DELETE }
        public final Type type;
        public final String line;

        public DiffEntry(Type type, String line) {
            this.type = type;
            this.line = line;
        }

        public String prefix() {
            switch (type) {
                case INSERT: return "+ ";
                case DELETE: return "- ";
                default: return "  ";
            }
        }
    }

    public static void show(Window owner, File initialFile) {
        Stage stage = new Stage();
        stage.setTitle("Compare Files");
        stage.initOwner(owner);
        stage.initModality(Modality.NONE); // Allow checking other files while comparing

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(60);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        ColumnConstraints col3 = new ColumnConstraints();
        grid.getColumnConstraints().addAll(col1, col2, col3);

        Label labelA = new Label("File A:");
        TextField fieldA = new TextField();
        if (initialFile != null) {
            fieldA.setText(initialFile.getAbsolutePath());
        }
        Button browseA = new Button("Browse...");
        browseA.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select File A");
            File f = chooser.showOpenDialog(stage);
            if (f != null) fieldA.setText(f.getAbsolutePath());
        });

        grid.add(labelA, 0, 0);
        grid.add(fieldA, 1, 0);
        grid.add(browseA, 2, 0);

        Label labelB = new Label("File B:");
        TextField fieldB = new TextField();
        Button browseB = new Button("Browse...");
        browseB.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select File B");
            File f = chooser.showOpenDialog(stage);
            if (f != null) fieldB.setText(f.getAbsolutePath());
        });

        grid.add(labelB, 0, 1);
        grid.add(fieldB, 1, 1);
        grid.add(browseB, 2, 1);

        Button swapBtn = new Button("⇅ Swap");
        swapBtn.setOnAction(e -> {
            String temp = fieldA.getText();
            fieldA.setText(fieldB.getText());
            fieldB.setText(temp);
        });

        Button compareBtn = new Button("Compare");
        compareBtn.setDefaultButton(true);
        compareBtn.setStyle("-fx-font-weight: bold;");

        HBox actions = new HBox(10, swapBtn, compareBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(0, 15, 10, 15));

        ListView<DiffEntry> diffView = new ListView<>();
        VBox.setVgrow(diffView, Priority.ALWAYS);
        diffView.setPlaceholder(new Label("Select files and click Compare to see differences."));

        diffView.setCellFactory(lv -> new ListCell<DiffEntry>() {
            @Override
            protected void updateItem(DiffEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle(null);
                } else {
                    setText(item.prefix() + item.line);
                    boolean isDark = ThemeManager.getInstance().isDark();
                    if (item.type == DiffEntry.Type.INSERT) {
                        setStyle("-fx-text-fill: #2ecc71; -fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 13px; -fx-background-color: " + (isDark ? "#1b4d3e" : "#e8f8f5") + ";");
                    } else if (item.type == DiffEntry.Type.DELETE) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 13px; -fx-background-color: " + (isDark ? "#5c2525" : "#fdf2f2") + ";");
                    } else {
                        setStyle("-fx-text-fill: " + (isDark ? "#abb2bf" : "#333333") + "; -fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 13px;");
                    }
                }
            }
        });

        Label statusLabel = new Label("Ready");
        statusLabel.setPadding(new Insets(10, 15, 10, 15));
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.setStyle("-fx-background-color: rgba(0,0,0,0.05);");

        compareBtn.setOnAction(e -> {
            File fileA = new File(fieldA.getText().trim());
            File fileB = new File(fieldB.getText().trim());

            if (!fileA.isFile()) {
                showError(stage, "Invalid File A", "File A does not exist or is not a valid file.");
                return;
            }
            if (!fileB.isFile()) {
                showError(stage, "Invalid File B", "File B does not exist or is not a valid file.");
                return;
            }

            try {
                List<String> linesA = Files.readAllLines(fileA.toPath(), StandardCharsets.UTF_8);
                List<String> linesB = Files.readAllLines(fileB.toPath(), StandardCharsets.UTF_8);

                if ((long) linesA.size() * linesB.size() > 5_000_000) {
                    Alert warn = new Alert(Alert.AlertType.CONFIRMATION);
                    warn.setTitle("Files are large");
                    warn.setHeaderText("Performance Warning");
                    warn.setContentText("The selected files are quite large. Comparing them might take a few seconds and freeze the UI. Do you want to continue?");
                    warn.initOwner(stage);
                    ThemeManager.getInstance().applyTheme(warn.getDialogPane().getScene());
                    if (warn.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
                        return;
                    }
                }

                List<DiffEntry> diff = computeDiff(linesA, linesB);
                diffView.getItems().setAll(diff);

                long additions = diff.stream().filter(d -> d.type == DiffEntry.Type.INSERT).count();
                long deletions = diff.stream().filter(d -> d.type == DiffEntry.Type.DELETE).count();

                if (additions == 0 && deletions == 0) {
                    statusLabel.setText("Files are identical");
                } else {
                    statusLabel.setText(additions + " additions, " + deletions + " deletions");
                }

            } catch (IOException ex) {
                showError(stage, "Error Reading Files", "Failed to read files: " + ex.getMessage());
            }
        });

        VBox layout = new VBox(grid, actions, diffView, statusLabel);
        Scene scene = new Scene(layout, 650, 500);
        stage.setScene(scene);

        ThemeManager.getInstance().applyTheme(scene);
        stage.show();
    }

    private static void showError(Window owner, String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg);
        a.setTitle(title);
        a.setHeaderText(null);
        a.initOwner(owner);
        ThemeManager.getInstance().applyTheme(a.getDialogPane().getScene());
        a.showAndWait();
    }

    private static List<DiffEntry> computeDiff(List<String> left, List<String> right) {
        int n = left.size();
        int m = right.size();
        int[][] dp = new int[n + 1][m + 1];

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                if (left.get(i - 1).equals(right.get(j - 1))) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        List<DiffEntry> diff = new ArrayList<>();
        int i = n, j = m;
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && left.get(i - 1).equals(right.get(j - 1))) {
                diff.add(0, new DiffEntry(DiffEntry.Type.UNCHANGED, left.get(i - 1)));
                i--;
                j--;
            } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
                diff.add(0, new DiffEntry(DiffEntry.Type.INSERT, right.get(j - 1)));
                j--;
            } else {
                diff.add(0, new DiffEntry(DiffEntry.Type.DELETE, left.get(i - 1)));
                i--;
            }
        }
        return diff;
    }
}
