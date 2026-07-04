package com.eagle.tools;

import com.eagle.util.ThemeManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import java.sql.*;
import java.util.*;

public class SqlRunner {

    private static Connection activeConn;

    public static void show(Window owner) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.setTitle("SQL Runner");

        TextField urlField = new TextField("jdbc:mysql://localhost:3306/mydb");
        urlField.setPromptText("jdbc:mysql://host:port/db");
        TextField userField = new TextField("root");
        TextField passField = new PasswordField();

        HBox connRow = new HBox(6,
            new Label("URL:"), urlField,
            new Label("User:"), userField,
            new Label("Pass:"), passField,
            new Button("Connect"));
        connRow.setPadding(new Insets(8));
        HBox.setHgrow(urlField, Priority.ALWAYS);

        TextArea sqlArea = new TextArea();
        sqlArea.setPromptText("SELECT * FROM users");
        sqlArea.setPrefRowCount(4);
        sqlArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12px;");

        Label statusLabel = new Label("Not connected");
        statusLabel.setStyle("-fx-text-fill: #888;");

        TableView<Map<String, String>> resultTable = new TableView<>();
        resultTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TextArea errorArea = new TextArea();
        errorArea.setEditable(false);
        errorArea.setPrefRowCount(3);
        errorArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px; -fx-text-fill: red;");

        TabPane tabs = new TabPane();
        Tab resultTab = new Tab("Results", resultTable);
        Tab errorTab = new Tab("Errors", errorArea);
        tabs.getTabs().addAll(resultTab, errorTab);

        HBox actions = new HBox(8,
            new Button("Run (Ctrl+Enter)"),
            new Button("Clear"),
            new Button("Disconnect"));
        actions.setPadding(new Insets(8));

        Button connectBtn = (Button) connRow.getChildren().get(6);
        Button runBtn = (Button) actions.getChildren().get(0);
        Button clearBtn = (Button) actions.getChildren().get(1);
        Button disconnectBtn = (Button) actions.getChildren().get(2);

        connectBtn.setOnAction(e -> {
            try {
                if (activeConn != null) try { activeConn.close(); } catch (Exception ignored) {}
                activeConn = DriverManager.getConnection(urlField.getText(), userField.getText(), passField.getText());
                statusLabel.setText("Connected: " + urlField.getText());
                statusLabel.setStyle("-fx-text-fill: green;");
                sqlArea.requestFocus();
            } catch (Exception ex) {
                statusLabel.setText("Failed: " + ex.getMessage());
                statusLabel.setStyle("-fx-text-fill: red;");
                errorArea.setText(ex.toString());
                tabs.getSelectionModel().select(errorTab);
            }
        });

        runBtn.setOnAction(e -> runQuery(sqlArea.getText(), resultTable, errorArea, tabs, statusLabel));
        sqlArea.setOnKeyPressed(e -> {
            if (e.isControlDown() && e.getCode().toString().equals("ENTER")) {
                runQuery(sqlArea.getText(), resultTable, errorArea, tabs, statusLabel);
            }
        });

        clearBtn.setOnAction(e -> { sqlArea.clear(); resultTable.getColumns().clear(); resultTable.setItems(FXCollections.observableArrayList()); errorArea.clear(); });
        disconnectBtn.setOnAction(e -> {
            try { if (activeConn != null) activeConn.close(); } catch (Exception ignored) {}
            activeConn = null;
            statusLabel.setText("Disconnected");
            statusLabel.setStyle("-fx-text-fill: #888;");
        });

        VBox root = new VBox(4, connRow, new Label("SQL:"), sqlArea, actions, statusLabel, tabs);
        root.setPadding(new Insets(8));
        VBox.setVgrow(tabs, Priority.ALWAYS);

        Scene scene = new Scene(root, 700, 550);
        ThemeManager.getInstance().applyTheme(scene);
        stage.setScene(scene);
        stage.show();
    }

    private static void runQuery(String sql, TableView<Map<String, String>> table, TextArea errorArea, TabPane tabs, Label status) {
        if (activeConn == null) {
            status.setText("Not connected. Click Connect first.");
            status.setStyle("-fx-text-fill: red;");
            return;
        }
        new Thread(() -> {
            try {
                String lower = sql.trim().toLowerCase();
                boolean isQuery = lower.startsWith("select") || lower.startsWith("show") || lower.startsWith("describe") || lower.startsWith("explain");
                Statement stmt = activeConn.createStatement();
                if (isQuery) {
                    ResultSet rs = stmt.executeQuery(sql);
                    ResultSetMetaData meta = rs.getMetaData();
                    int cols = meta.getColumnCount();
                    List<String> colNames = new ArrayList<>();
                    for (int i = 1; i <= cols; i++) colNames.add(meta.getColumnName(i));

                    List<Map<String, String>> rows = new ArrayList<>();
                    while (rs.next()) {
                        Map<String, String> row = new LinkedHashMap<>();
                        for (int i = 1; i <= cols; i++) row.put(meta.getColumnName(i), rs.getString(i));
                        rows.add(row);
                    }

                    Platform.runLater(() -> {
                        table.getColumns().clear();
                        for (String col : colNames) {
                            TableColumn<Map<String, String>, String> tc = new TableColumn<>(col);
                            tc.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get(col)));
                            table.getColumns().add(tc);
                        }
                        table.setItems(FXCollections.observableArrayList(rows));
                        status.setText("Query returned " + rows.size() + " rows");
                        status.setStyle("-fx-text-fill: green;");
                        tabs.getSelectionModel().select(0);
                    });
                    rs.close();
                } else {
                    int affected = stmt.executeUpdate(sql);
                    Platform.runLater(() -> {
                        status.setText(affected + " rows affected");
                        status.setStyle("-fx-text-fill: green;");
                        table.getColumns().clear();
                        table.setItems(FXCollections.observableArrayList());
                    });
                }
                stmt.close();
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    errorArea.setText(ex.toString());
                    status.setText("Error: " + ex.getMessage());
                    status.setStyle("-fx-text-fill: red;");
                    tabs.getSelectionModel().select(1);
                });
            }
        }).start();
    }
}
