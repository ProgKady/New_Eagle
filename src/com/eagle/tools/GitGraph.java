package com.eagle.tools;

import com.eagle.util.ThemeManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.stage.*;
import java.io.*;
import java.util.*;

public class GitGraph {

    public static void show(File projectRoot, Window owner) {
        if (projectRoot == null) return;
        File gitDir = new File(projectRoot, ".git");
        if (!gitDir.exists()) {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Not a Git repository: " + projectRoot.getName());
            a.initOwner(owner); a.showAndWait();
            return;
        }

        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.setTitle("Git Graph: " + projectRoot.getName());

        VBox container = new VBox(8);
        container.setPadding(new Insets(15));
        Label loading = new Label("Loading git log...");
        container.getChildren().add(loading);

        Scene scene = new Scene(new ScrollPane(container), 700, 500);
        ThemeManager.getInstance().applyTheme(scene);
        stage.setScene(scene);
        stage.show();

        new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder("git", "log", "--all", "--oneline", "--graph",
                    "--pretty=format:%H|%d|%s|%an|%ar", "-100");
                pb.directory(projectRoot);
                Process proc = pb.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                List<String[]> commits = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    commits.add(parseLogLine(line));
                }
                proc.waitFor();

                Platform.runLater(() -> {
                    container.getChildren().clear();
                    if (commits.isEmpty()) {
                        container.getChildren().add(new Label("No commits found."));
                        return;
                    }
                    for (String[] parts : commits) {
                        HBox row = new HBox(8);
                        row.setPadding(new Insets(3, 0, 3, 0));
                        // Graph part (first column - the ASCII graph chars)
                        Label graphLabel = new Label(parts[0]);
                        graphLabel.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px; -fx-text-fill: #888;");

                        // Hash
                        Label hashLabel = new Label(parts[1].substring(0, 7));
                        hashLabel.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px; -fx-text-fill: #d08770;");

                        // Branch/tag info
                        Label refLabel = new Label(parts[2]);
                        refLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #b48ead;");

                        // Message
                        Label msgLabel = new Label(parts[3]);
                        msgLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
                        HBox.setHgrow(msgLabel, Priority.ALWAYS);

                        // Author
                        Label authorLabel = new Label(parts[4]);
                        authorLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");

                        // Date
                        Label dateLabel = new Label(parts[5]);
                        dateLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");

                        row.getChildren().addAll(graphLabel, hashLabel, refLabel, msgLabel, authorLabel, dateLabel);
                        container.getChildren().add(row);
                    }

                    Label total = new Label("Showing " + commits.size() + " commits");
                    total.setStyle("-fx-font-size: 10px; -fx-text-fill: #888; -fx-padding: 8 0 0 0;");
                    container.getChildren().add(total);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    container.getChildren().clear();
                    container.getChildren().add(new Label("Error: " + ex.getMessage()));
                });
            }
        }).start();
    }

    private static String[] parseLogLine(String line) {
        // Extract graph prefix (everything before first alphanumeric hash)
        String graph = "";
        String rest = line;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (Character.isLetterOrDigit(c) && c != '*') {
                graph = line.substring(0, i);
                rest = line.substring(i);
                break;
            }
        }
        String[] parts = rest.split("\\|", 5);
        String[] result = new String[6];
        result[0] = graph;
        result[1] = parts.length > 0 ? parts[0] : "";
        result[2] = parts.length > 1 ? parts[1] : "";
        result[3] = parts.length > 2 ? parts[2] : "";
        result[4] = parts.length > 3 ? parts[3] : "";
        result[5] = parts.length > 4 ? parts[4] : "";
        return result;
    }
}
