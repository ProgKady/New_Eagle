package com.eagle.tools;

import com.eagle.util.ThemeManager;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import java.util.*;

public class ErDesigner {

    private static final List<TableDef> tables = new ArrayList<>();
    private static Pane canvas;

    public static void show(Window owner) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.setTitle("ER Database Designer");

        canvas = new Pane();
        canvas.setPrefSize(800, 500);
        canvas.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ddd;");

        TextField nameField = new TextField();
        nameField.setPromptText("Table name");
        Button addBtn = new Button("Add Table");
        addBtn.setOnAction(e -> addTable(nameField.getText()));
        HBox tb = new HBox(6, new Label("Table:"), nameField, addBtn);
        tb.setPadding(new Insets(6));

        ScrollPane sp = new ScrollPane(canvas);
        sp.setPrefHeight(400);

        Button exportBtn = new Button("Export SQL");
        exportBtn.setOnAction(e -> exportSql());
        Button clearBtn = new Button("Clear All");
        clearBtn.setOnAction(e -> { tables.clear(); canvas.getChildren().clear(); });

        VBox root = new VBox(6, tb, sp, new HBox(6, exportBtn, clearBtn));
        root.setPadding(new Insets(8));

        Scene scene = new Scene(root, 900, 600);
        ThemeManager.getInstance().applyTheme(scene);
        stage.setScene(scene);
        stage.show();
    }

    private static void addTable(String name) {
        if (name == null || name.trim().isEmpty()) return;
        TableDef t = new TableDef(name.trim());
        tables.add(t);
        draw();
    }

    private static void draw() {
        canvas.getChildren().clear();
        double x = 30, y = 30;
        for (TableDef t : tables) {
            VBox box = new VBox(2);
            box.setStyle("-fx-background-color: white; -fx-border-color: #4a90d9; -fx-border-width: 2; -fx-padding: 6; -fx-background-radius: 4;");
            Label title = new Label(t.name);
            title.setStyle("-fx-font-weight: bold; -fx-text-fill: #4a90d9; -fx-font-size: 13px;");
            box.getChildren().add(title);
            box.getChildren().add(new Separator());
            for (ColumnDef c : t.columns) {
                Label cl = new Label((c.pk ? "PK " : "") + c.name + "  " + c.type);
                cl.setStyle("-fx-font-size: 11px; -fx-font-family: 'Consolas';");
                box.getChildren().add(cl);
            }
            Button addCol = new Button("+Col");
            addCol.setStyle("-fx-font-size: 9px; -fx-padding: 1 4; -fx-background-color: #e8f0fe;");
            final String tn = t.name;
            addCol.setOnAction(e -> addColumnDialog(tn));
            box.getChildren().add(addCol);
            box.setLayoutX(x);
            box.setLayoutY(y);
            canvas.getChildren().add(box);
            x += 220;
            if (x > 700) { x = 30; y += 200; }
        }
    }

    private static void addColumnDialog(String tableName) {
        TableDef t = tables.stream().filter(tb -> tb.name.equals(tableName)).findFirst().orElse(null);
        if (t == null) return;
        Dialog<List<String>> d = new Dialog<>();
        d.setTitle("Add Column");
        d.initOwner(canvas.getScene().getWindow());
        TextField nameF = new TextField(); nameF.setPromptText("column_name");
        ComboBox<String> typeF = new ComboBox<>();
        typeF.getItems().addAll("INT", "VARCHAR(255)", "TEXT", "BOOLEAN", "FLOAT", "DATE", "BIGINT");
        typeF.setValue("VARCHAR(255)");
        CheckBox pkF = new CheckBox("Primary Key");
        VBox v = new VBox(6, new Label("Name:"), nameF, new Label("Type:"), typeF, pkF);
        v.setPadding(new Insets(10));
        d.getDialogPane().setContent(v);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.getDialogPane().sceneProperty().addListener((obs, o, n) -> { if (n != null) ThemeManager.getInstance().applyTheme(n); });
        d.setResultConverter(btn -> btn == ButtonType.OK ? Arrays.asList(nameF.getText(), typeF.getValue(), pkF.isSelected() ? "1" : "0") : null);
        d.showAndWait().ifPresent(res -> {
            ColumnDef c = new ColumnDef(res.get(0), res.get(1));
            c.pk = "1".equals(res.get(2));
            t.columns.add(c);
            draw();
        });
    }

    private static void exportSql() {
        StringBuilder sb = new StringBuilder();
        for (TableDef t : tables) {
            sb.append("CREATE TABLE ").append(t.name).append(" (\n");
            for (int i = 0; i < t.columns.size(); i++) {
                ColumnDef c = t.columns.get(i);
                sb.append("  ").append(c.name).append(" ").append(c.type);
                if (c.pk) sb.append(" PRIMARY KEY");
                if (i < t.columns.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append(");\n\n");
        }
        TextArea ta = new TextArea(sb.toString());
        ta.setEditable(false);
        ta.setPrefSize(500, 300);
        Stage s = new Stage();
        s.initOwner(canvas.getScene().getWindow());
        s.setTitle("Generated SQL");
        Scene sc = new Scene(new VBox(ta), 500, 300);
        ThemeManager.getInstance().applyTheme(sc);
        s.setScene(sc);
        s.show();
    }

    static class TableDef {
        String name;
        List<ColumnDef> columns = new ArrayList<>();
        TableDef(String n) { name = n; }
    }
    static class ColumnDef {
        String name, type;
        boolean pk;
        ColumnDef(String n, String t) { name = n; type = t; }
    }
}