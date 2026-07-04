package com.eagle.editor;

import com.eagle.util.ThemeManager;
import java.io.File;
import java.sql.*;
import java.util.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class DatabaseViewerDialog {

    private static Connection conn;
    private static String dbType = "SQLite";

    public static void show() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Database Viewer");

        TabPane tabs = new TabPane();
        Tab browseTab = new Tab("Browse");
        Tab queryTab = new Tab("SQL Query");

        // --- Browse tab ---
        ListView<String> tableList = new ListView<>();
        tableList.setPrefWidth(200);

        TableView<List<String>> dataTable = new TableView<>();
        dataTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Label statusLabel = new Label("Open or connect to a database to begin.");
        statusLabel.setStyle("-fx-padding: 8;");

        // Connection controls
        ChoiceBox<String> dbTypeChoice = new ChoiceBox<>();
        dbTypeChoice.getItems().addAll("SQLite (.db/.sqlite)", "MySQL", "PostgreSQL", "MariaDB");
        dbTypeChoice.setValue("SQLite (.db/.sqlite)");

        TextField hostField = new TextField("localhost");
        hostField.setPromptText("Host");
        hostField.setPrefColumnCount(12);
        TextField portField = new TextField();
        portField.setPromptText("Port");
        portField.setPrefColumnCount(5);
        TextField dbNameField = new TextField();
        dbNameField.setPromptText("Database");
        dbNameField.setPrefColumnCount(10);
        TextField userField = new TextField();
        userField.setPromptText("User");
        userField.setPrefColumnCount(8);
        PasswordField passField = new PasswordField();
        passField.setPromptText("Password");
        passField.setPrefColumnCount(8);

        HBox connFields = new HBox(4, new Label("Host:"), hostField, new Label("Port:"), portField,
                new Label("DB:"), dbNameField, new Label("User:"), userField, new Label("Pass:"), passField);
        connFields.setVisible(false);

        dbTypeChoice.setOnAction(e -> {
            String sel = dbTypeChoice.getValue();
            boolean network = sel.startsWith("MySQL") || sel.startsWith("PostgreSQL") || sel.startsWith("MariaDB");
            connFields.setVisible(network);
            if (sel.equals("MySQL")) portField.setText("3306");
            else if (sel.equals("PostgreSQL")) portField.setText("5432");
            else if (sel.equals("MariaDB")) portField.setText("3307");
            else portField.setText("");
        });

        Button openBtn = new Button("Open / Connect");
        openBtn.setOnAction(e -> {
            try {
                String sel = dbTypeChoice.getValue();
                if (conn != null) { conn.close(); conn = null; }
                if (sel.startsWith("SQLite")) {
                    FileChooser fc = new FileChooser();
                    fc.setTitle("Open SQLite Database");
                    fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite Database", "*.db", "*.sqlite", "*.sqlite3"));
                    File f = fc.showOpenDialog(stage);
                    if (f == null) return;
                    Class.forName("org.sqlite.JDBC");
                    conn = DriverManager.getConnection("jdbc:sqlite:" + f.getAbsolutePath());
                    dbType = "SQLite";
                    statusLabel.setText("Connected to: " + f.getName());
                } else {
                    String host = hostField.getText().trim();
                    String port = portField.getText().trim();
                    String dbName = dbNameField.getText().trim();
                    String user = userField.getText().trim();
                    String pass = passField.getText();
                    if (host.isEmpty() || port.isEmpty() || dbName.isEmpty()) {
                        statusLabel.setText("Please fill in host, port and database name.");
                        return;
                    }
                    String url;
                    if (sel.startsWith("MySQL")) {
                        Class.forName("com.mysql.cj.jdbc.Driver");
                        url = "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false&allowPublicKeyRetrieval=true";
                        dbType = "MySQL";
                    } else if (sel.startsWith("MariaDB")) {
                        Class.forName("org.mariadb.jdbc.Driver");
                        url = "jdbc:mariadb://" + host + ":" + port + "/" + dbName;
                        dbType = "MariaDB";
                    } else {
                        Class.forName("org.postgresql.Driver");
                        url = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
                        dbType = "PostgreSQL";
                    }
                    conn = DriverManager.getConnection(url, user, pass);
                    statusLabel.setText("Connected to " + sel + " @ " + host + ":" + port + "/" + dbName);
                }
                refreshTableList(tableList);
                dataTable.getColumns().clear();
                dataTable.getItems().clear();
            } catch (Exception ex) {
                statusLabel.setText("Error: " + ex.getMessage());
            }
        });

        tableList.getSelectionModel().selectedItemProperty().addListener((obs, old, tbl) -> {
            if (tbl == null || conn == null) return;
            loadTableData(dataTable, tbl, statusLabel);
        });

        VBox left = new VBox(4, new Label("Tables:"), tableList);
        left.setPadding(new Insets(8));
        VBox.setVgrow(tableList, Priority.ALWAYS);

        // CRUD toolbar
        Button insertBtn = new Button("+ Insert Row");
        insertBtn.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-cursor: hand;");
        Button editBtn = new Button("Edit Selected");
        editBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-cursor: hand;");
        Button deleteBtn = new Button("- Delete Selected");
        deleteBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-cursor: hand;");
        Button refreshBtn = new Button("Refresh");
        refreshBtn.setStyle("-fx-cursor: hand;");

        insertBtn.setOnAction(e -> {
            String tbl = tableList.getSelectionModel().getSelectedItem();
            if (tbl == null) { statusLabel.setText("Select a table first."); return; }
            showInsertDialog(stage, tbl, dataTable, statusLabel);
        });
        editBtn.setOnAction(e -> {
            String tbl = tableList.getSelectionModel().getSelectedItem();
            if (tbl == null) { statusLabel.setText("Select a table first."); return; }
            List<String> row = dataTable.getSelectionModel().getSelectedItem();
            if (row == null) { statusLabel.setText("Select a row to edit."); return; }
            showEditDialog(stage, tbl, row, dataTable, statusLabel);
        });
        deleteBtn.setOnAction(e -> {
            String tbl = tableList.getSelectionModel().getSelectedItem();
            if (tbl == null) { statusLabel.setText("Select a table first."); return; }
            List<String> row = dataTable.getSelectionModel().getSelectedItem();
            if (row == null) { statusLabel.setText("Select a row to delete."); return; }
            showDeleteConfirm(stage, tbl, row, dataTable, statusLabel);
        });
        refreshBtn.setOnAction(e -> {
            String tbl = tableList.getSelectionModel().getSelectedItem();
            if (tbl != null) loadTableData(dataTable, tbl, statusLabel);
        });

        HBox crudBar = new HBox(6, insertBtn, editBtn, deleteBtn, refreshBtn);
        crudBar.setPadding(new Insets(4, 0, 4, 0));

        VBox right = new VBox(4, new Label("Data:"), crudBar, dataTable);
        right.setPadding(new Insets(8));
        VBox.setVgrow(dataTable, Priority.ALWAYS);

        SplitPane split = new SplitPane(left, right);
        split.setDividerPositions(0.25);
        VBox.setVgrow(split, Priority.ALWAYS);

        HBox top = new HBox(8, dbTypeChoice, openBtn, connFields, statusLabel);
        top.setPadding(new Insets(8));
        HBox.setHgrow(statusLabel, Priority.ALWAYS);

        VBox browseContent = new VBox(top, split);
        VBox.setVgrow(split, Priority.ALWAYS);
        browseTab.setContent(browseContent);

        // --- SQL Query tab ---
        TextArea queryInput = new TextArea();
        queryInput.setPromptText("Enter SQL query...");
        queryInput.setPrefRowCount(4);
        queryInput.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12px;");

        TableView<List<String>> queryResult = new TableView<>();
        queryResult.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Label queryStatus = new Label("Run a query to see results.");
        queryStatus.setStyle("-fx-padding: 4;");

        Button runBtn = new Button("Run Query");
        runBtn.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        runBtn.setOnAction(e -> {
            if (conn == null) { queryStatus.setText("Connect to a database first."); return; }
            String sql = queryInput.getText().trim();
            if (sql.isEmpty()) { queryStatus.setText("Enter a query."); return; }
            try {
                String upper = sql.toUpperCase().trim();
                boolean isQuery = upper.startsWith("SELECT") || upper.startsWith("PRAGMA") || upper.startsWith("EXPLAIN") || upper.startsWith("SHOW") || upper.startsWith("DESCRIBE");
                Statement stmt = conn.createStatement();
                if (isQuery) {
                    ResultSet rs = stmt.executeQuery(sql);
                    ResultSetMetaData meta = rs.getMetaData();
                    int cc = meta.getColumnCount();
                    queryResult.getColumns().clear();
                    for (int i = 1; i <= cc; i++) {
                        final int ci = i;
                        TableColumn<List<String>, String> col = new TableColumn<>(meta.getColumnName(i));
                        col.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().get(ci - 1)));
                        queryResult.getColumns().add(col);
                    }
                    queryResult.getItems().clear();
                    int rowCount = 0;
                    while (rs.next()) {
                        List<String> row = new ArrayList<>();
                        for (int i = 1; i <= cc; i++) row.add(rs.getString(i) != null ? rs.getString(i) : "");
                        queryResult.getItems().add(row);
                        rowCount++;
                    }
                    rs.close();
                    queryStatus.setText("Query returned " + rowCount + " rows (" + cc + " columns)");
                } else {
                    int affected = stmt.executeUpdate(sql);
                    queryStatus.setText("Statement executed. " + affected + " rows affected.");
                    queryResult.getColumns().clear();
                    queryResult.getItems().clear();
                    refreshTableList(tableList);
                }
                stmt.close();
            } catch (Exception ex) {
                queryStatus.setText("Error: " + ex.getMessage());
            }
        });

        VBox queryContent = new VBox(4, new Label("SQL:"), queryInput,
                new HBox(6, runBtn, queryStatus), queryResult);
        queryContent.setPadding(new Insets(8));
        VBox.setVgrow(queryResult, Priority.ALWAYS);
        queryTab.setContent(queryContent);

        tabs.getTabs().addAll(browseTab, queryTab);

        Button closeBtn = new Button("Close");
        closeBtn.setOnAction(e -> stage.close());
        HBox bottom = new HBox(closeBtn);
        bottom.setPadding(new Insets(8));
        bottom.setStyle("-fx-alignment: center-right;");

        VBox root = new VBox(tabs, bottom);
        root.setPrefSize(900, 550);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        stage.setOnCloseRequest(ev -> {
            try { if (conn != null) conn.close(); } catch (Exception ignored) {}
        });

        Scene scene = new Scene(root);
        ThemeManager.getInstance().applyTheme(scene);
        stage.setScene(scene);
        stage.showAndWait();
    }

    private static void refreshTableList(ListView<String> tableList) {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            String[] types = {"TABLE", "VIEW"};
            ResultSet rs = meta.getTables(null, null, "%", types);
            tableList.getItems().clear();
            while (rs.next()) tableList.getItems().add(rs.getString("TABLE_NAME"));
            rs.close();
        } catch (Exception ignored) {}
    }

    private static void loadTableData(TableView<List<String>> dataTable, String tbl, Label statusLabel) {
        try {
            String q;
            if (dbType.equals("PostgreSQL")) q = "SELECT * FROM \"" + tbl + "\" LIMIT 200";
            else q = "SELECT * FROM \"" + tbl + "\" LIMIT 200";
            Statement stmt = conn.createStatement();
            ResultSet data = stmt.executeQuery(q);
            ResultSetMetaData colMeta = data.getMetaData();
            int colCount = colMeta.getColumnCount();
            dataTable.getColumns().clear();
            for (int i = 1; i <= colCount; i++) {
                final int ci = i;
                TableColumn<List<String>, String> col = new TableColumn<>(colMeta.getColumnName(i));
                col.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().get(ci - 1)));
                dataTable.getColumns().add(col);
            }
            dataTable.getItems().clear();
            while (data.next()) {
                List<String> row = new ArrayList<>();
                for (int i = 1; i <= colCount; i++) row.add(data.getString(i) != null ? data.getString(i) : "");
                dataTable.getItems().add(row);
            }
            data.close();
            stmt.close();
            statusLabel.setText("Table: " + tbl + " — " + dataTable.getItems().size() + " rows (" + colCount + " cols)");
        } catch (SQLException ex) {
            statusLabel.setText("Error: " + ex.getMessage());
        }
    }

    private static String quoteId(String id) {
        if (dbType.equals("PostgreSQL")) return "\"" + id + "\"";
        return "`" + id + "`";
    }

    private static String quoteVal(String val) {
        if (val == null || val.isEmpty()) return "NULL";
        return "'" + val.replace("'", "''") + "'";
    }

    private static List<String> getPrimaryKeyColumns(String tbl) {
        List<String> pks = new ArrayList<>();
        try {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getPrimaryKeys(null, null, tbl);
            while (rs.next()) pks.add(rs.getString("COLUMN_NAME"));
            rs.close();
        } catch (Exception ignored) {}
        return pks;
    }

    private static void showInsertDialog(Stage owner, String tbl, TableView<List<String>> dataTable, Label statusLabel) {
        Stage dlg = new Stage();
        dlg.initOwner(owner);
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle("Insert Row into " + tbl);
        try {
            String q = "SELECT * FROM " + quoteId(tbl) + " LIMIT 0";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(q);
            ResultSetMetaData meta = rs.getMetaData();
            int cc = meta.getColumnCount();
            VBox form = new VBox(6);
            form.setPadding(new Insets(12));
            List<TextField> fields = new ArrayList<>();
            for (int i = 1; i <= cc; i++) {
                HBox row = new HBox(6, new Label(meta.getColumnName(i) + ":"));
                TextField tf = new TextField();
                tf.setPrefColumnCount(30);
                if (meta.isAutoIncrement(i)) {
                    tf.setDisable(true);
                    tf.setPromptText("auto-increment");
                }
                row.getChildren().add(tf);
                row.setStyle("-fx-alignment: center-left;");
                form.getChildren().add(row);
                fields.add(tf);
            }
            rs.close();
            stmt.close();
            Button saveBtn = new Button("Insert");
            saveBtn.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-cursor: hand;");
            saveBtn.setOnAction(ev -> {
                try {
                    StringBuilder cols = new StringBuilder();
                    StringBuilder vals = new StringBuilder();
                    List<String> colNames = new ArrayList<>();
                    for (int i = 0; i < fields.size(); i++) {
                        if (fields.get(i).isDisabled()) continue;
                        if (cols.length() > 0) { cols.append(", "); vals.append(", "); }
                        String colName = ((Label)((HBox)form.getChildren().get(i)).getChildren().get(0)).getText().replace(":", "");
                        cols.append(quoteId(colName));
                        vals.append(quoteVal(fields.get(i).getText()));
                        colNames.add(colName);
                    }
                    String sql = "INSERT INTO " + quoteId(tbl) + " (" + cols + ") VALUES (" + vals + ")";
                    Statement st = conn.createStatement();
                    st.executeUpdate(sql);
                    st.close();
                    loadTableData(dataTable, tbl, statusLabel);
                    dlg.close();
                } catch (Exception ex) {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Insert Error: " + ex.getMessage());
                    a.showAndWait();
                }
            });
            form.getChildren().add(new HBox(saveBtn));
            Scene s = new Scene(form, 420, 40 + cc * 35);
            ThemeManager.getInstance().applyTheme(s);
            dlg.setScene(s);
            dlg.showAndWait();
        } catch (Exception ex) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage());
            a.showAndWait();
        }
    }

    private static void showEditDialog(Stage owner, String tbl, List<String> row, TableView<List<String>> dataTable, Label statusLabel) {
        Stage dlg = new Stage();
        dlg.initOwner(owner);
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle("Edit Row in " + tbl);
        try {
            String q = "SELECT * FROM " + quoteId(tbl) + " LIMIT 0";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(q);
            ResultSetMetaData meta = rs.getMetaData();
            int cc = meta.getColumnCount();
            VBox form = new VBox(6);
            form.setPadding(new Insets(12));
            List<TextField> fields = new ArrayList<>();
            List<String> colNames = new ArrayList<>();
            for (int i = 1; i <= cc; i++) {
                HBox hb = new HBox(6, new Label(meta.getColumnName(i) + ":"));
                TextField tf = new TextField();
                tf.setPrefColumnCount(30);
                if (i - 1 < row.size()) tf.setText(row.get(i - 1));
                hb.getChildren().add(tf);
                hb.setStyle("-fx-alignment: center-left;");
                form.getChildren().add(hb);
                fields.add(tf);
                colNames.add(meta.getColumnName(i));
            }
            rs.close();
            stmt.close();

            List<String> pkCols = getPrimaryKeyColumns(tbl);
            Button saveBtn = new Button("Update");
            saveBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-cursor: hand;");
            saveBtn.setOnAction(ev -> {
                try {
                    StringBuilder setClause = new StringBuilder();
                    StringBuilder whereClause = new StringBuilder();
                    for (int i = 0; i < fields.size(); i++) {
                        String cn = colNames.get(i);
                        String newVal = fields.get(i).getText();
                        String oldVal = i < row.size() ? row.get(i) : "";
                        if (pkCols.contains(cn)) {
                            if (whereClause.length() > 0) whereClause.append(" AND ");
                            whereClause.append(quoteId(cn)).append(" = ").append(quoteVal(oldVal));
                        } else {
                            if (setClause.length() > 0) setClause.append(", ");
                            setClause.append(quoteId(cn)).append(" = ").append(quoteVal(newVal));
                        }
                    }
                    if (whereClause.length() == 0) {
                        for (int i = 0; i < fields.size(); i++) {
                            if (whereClause.length() > 0) whereClause.append(" AND ");
                            whereClause.append(quoteId(colNames.get(i))).append(" = ").append(quoteVal(row.get(i)));
                        }
                    }
                    String sql = "UPDATE " + quoteId(tbl) + " SET " + setClause + " WHERE " + whereClause;
                    Statement st = conn.createStatement();
                    int affected = st.executeUpdate(sql);
                    st.close();
                    loadTableData(dataTable, tbl, statusLabel);
                    dlg.close();
                    if (affected == 0) {
                        Alert a = new Alert(Alert.AlertType.WARNING, "No rows were updated. The row may have been deleted already.");
                        a.showAndWait();
                    }
                } catch (Exception ex) {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Update Error: " + ex.getMessage());
                    a.showAndWait();
                }
            });
            form.getChildren().add(new HBox(saveBtn));
            Scene s = new Scene(form, 420, 40 + cc * 35);
            ThemeManager.getInstance().applyTheme(s);
            dlg.setScene(s);
            dlg.showAndWait();
        } catch (Exception ex) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage());
            a.showAndWait();
        }
    }

    private static void showDeleteConfirm(Stage owner, String tbl, List<String> row, TableView<List<String>> dataTable, Label statusLabel) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.initOwner(owner);
        confirm.setTitle("Delete Row");
        confirm.setHeaderText("Delete selected row from " + tbl + "?");
        confirm.setContentText("This action cannot be undone.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (!result.isPresent() || result.get() != ButtonType.OK) return;
        try {
            List<String> pkCols = getPrimaryKeyColumns(tbl);
            StringBuilder whereClause = new StringBuilder();

            String q = "SELECT * FROM " + quoteId(tbl) + " LIMIT 0";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(q);
            ResultSetMetaData meta = rs.getMetaData();
            List<String> colNames = new ArrayList<>();
            for (int i = 1; i <= meta.getColumnCount(); i++) colNames.add(meta.getColumnName(i));
            rs.close();
            stmt.close();

            if (!pkCols.isEmpty()) {
                for (String pk : pkCols) {
                    int idx = colNames.indexOf(pk);
                    if (idx >= 0 && idx < row.size()) {
                        if (whereClause.length() > 0) whereClause.append(" AND ");
                        whereClause.append(quoteId(pk)).append(" = ").append(quoteVal(row.get(idx)));
                    }
                }
            }
            if (whereClause.length() == 0) {
                for (int i = 0; i < colNames.size() && i < row.size(); i++) {
                    if (whereClause.length() > 0) whereClause.append(" AND ");
                    whereClause.append(quoteId(colNames.get(i))).append(" = ").append(quoteVal(row.get(i)));
                }
            }
            String sql = "DELETE FROM " + quoteId(tbl) + " WHERE " + whereClause;
            Statement st = conn.createStatement();
            int affected = st.executeUpdate(sql);
            st.close();
            loadTableData(dataTable, tbl, statusLabel);
            if (affected == 0) {
                Alert a = new Alert(Alert.AlertType.WARNING, "No rows were deleted.");
                a.showAndWait();
            }
        } catch (Exception ex) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Delete Error: " + ex.getMessage());
            a.showAndWait();
        }
    }
}
