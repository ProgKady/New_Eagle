package com.eagle.tools;

import com.eagle.util.ThemeManager;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Text;
import javafx.stage.*;
import java.util.*;
import java.util.regex.*;

public class ErDiagram {

    public static void show(Window owner) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.setTitle("ER Diagram");

        TextArea sqlArea = new TextArea();
        sqlArea.setPromptText("Paste CREATE TABLE statements...\n\n" +
            "CREATE TABLE users (\n  id INT PRIMARY KEY,\n  name VARCHAR(100),\n  email VARCHAR(255)\n);");
        sqlArea.setPrefRowCount(10);
        sqlArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12px;");

        Pane canvas = new Pane();
        canvas.setPrefSize(600, 400);
        canvas.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #ddd;");

        ScrollPane canvasScroll = new ScrollPane(canvas);
        canvasScroll.setPrefHeight(400);

        Button generateBtn = new Button("Generate Diagram");
        generateBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");

        generateBtn.setOnAction(e -> {
            canvas.getChildren().clear();
            List<TableInfo> tables = parseSql(sqlArea.getText());
            if (tables.isEmpty()) {
                Text t = new Text(50, 50, "No CREATE TABLE statements found.\nPaste SQL and click Generate.");
                canvas.getChildren().add(t);
                return;
            }
            drawDiagram(canvas, tables);
        });

        VBox root = new VBox(8,
            new Label("ER Diagram Generator - Paste SQL CREATE TABLE statements:"),
            sqlArea,
            generateBtn,
            canvasScroll);
        root.setPadding(new Insets(12));
        VBox.setVgrow(sqlArea, Priority.ALWAYS);

        Scene scene = new Scene(root, 700, 650);
        ThemeManager.getInstance().applyTheme(scene);
        stage.setScene(scene);
        stage.show();
    }

    static class TableInfo {
        String name;
        List<ColumnInfo> columns = new ArrayList<>();
        TableInfo(String name) { this.name = name; }
    }

    static class ColumnInfo {
        String name, type;
        boolean isPrimary, isForeignKey;
        String fkRef;
        ColumnInfo(String name, String type) { this.name = name; this.type = type; }
    }

    private static List<TableInfo> parseSql(String sql) {
        List<TableInfo> tables = new ArrayList<>();
        Pattern tablePattern = Pattern.compile("CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(?:`?(\\w+)`?)\\s*\\(([\\s\\S]*?)\\);",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Matcher m = tablePattern.matcher(sql);
        while (m.find()) {
            String tableName = m.group(1);
            String body = m.group(2);
            TableInfo table = new TableInfo(tableName);
            String[] lines = body.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("--") || line.startsWith("//")) continue;
                if (line.toUpperCase().startsWith("PRIMARY KEY")) {
                    Matcher pk = Pattern.compile("\\(\\s*`?(\\w+)`?\\s*\\)").matcher(line);
                    if (pk.find()) {
                        String pkCol = pk.group(1);
                        for (ColumnInfo col : table.columns) {
                            if (col.name.equalsIgnoreCase(pkCol)) col.isPrimary = true;
                        }
                    }
                    continue;
                }
                if (line.toUpperCase().contains("FOREIGN KEY")) continue;
                String[] parts = line.split("\\s+", 3);
                if (parts.length >= 2) {
                    String colName = parts[0].replace("`", "");
                    String colType = parts[1].toUpperCase();
                    // Remove trailing comma
                    if (colName.endsWith(",")) colName = colName.substring(0, colName.length()-1);
                    ColumnInfo col = new ColumnInfo(colName, colType);
                    if (line.toUpperCase().contains("PRIMARY KEY") || line.toUpperCase().contains("AUTO_INCREMENT"))
                        col.isPrimary = true;
                    table.columns.add(col);
                }
            }
            tables.add(table);
        }
        return tables;
    }

    private static void drawDiagram(Pane canvas, List<TableInfo> tables) {
        double startX = 40, startY = 30;
        double tableW = 180;
        double rowH = 22;
        int cols = Math.min(3, Math.max(1, (int)Math.ceil(Math.sqrt(tables.size()))));

        for (int i = 0; i < tables.size(); i++) {
            TableInfo tbl = tables.get(i);
            int col = i % cols;
            int row = i / cols;
            double x = startX + col * (tableW + 40);
            double y = startY + row * (tbl.columns.size() * rowH + 60);

            // Table header
            Rectangle header = new Rectangle(x, y, tableW, 28);
            header.setFill(Color.web("#4CAF50"));
            header.setStroke(Color.web("#388E3C"));
            header.setArcWidth(6); header.setArcHeight(6);
            Text title = new Text(x + 10, y + 19, tbl.name);
            title.setFill(Color.WHITE);
            title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

            canvas.getChildren().addAll(header, title);

            // Columns
            for (int j = 0; j < tbl.columns.size(); j++) {
                ColumnInfo colInfo = tbl.columns.get(j);
                double cy = y + 28 + j * rowH;
                Rectangle colRect = new Rectangle(x, cy, tableW, rowH);
                colRect.setFill(j % 2 == 0 ? Color.web("#f5f5f5") : Color.WHITE);
                colRect.setStroke(Color.web("#ddd"));
                canvas.getChildren().add(colRect);

                String prefix = colInfo.isPrimary ? "PK " : (colInfo.isForeignKey ? "FK " : "   ");
                Text colText = new Text(x + 6, cy + 15, prefix + colInfo.name + " : " + colInfo.type);
                colText.setStyle(colInfo.isPrimary ? "-fx-font-weight: bold; -fx-font-size: 10px;" : "-fx-font-size: 10px;");
                canvas.getChildren().add(colText);
            }

            // Border line after header
            Line sep = new Line(x, y + 28, x + tableW, y + 28);
            sep.setStroke(Color.web("#388E3C"));
            sep.setStrokeWidth(1.5);
            canvas.getChildren().add(sep);
        }

        // Adjust canvas size
        double maxX = 0, maxY = 0;
        for (javafx.scene.Node node : canvas.getChildren()) {
            if (node instanceof Rectangle) {
                Rectangle r = (Rectangle) node;
                maxX = Math.max(maxX, r.getX() + r.getWidth());
                maxY = Math.max(maxY, r.getY() + r.getHeight());
            }
            if (node instanceof Text) {
                Text t = (Text) node;
                maxX = Math.max(maxX, t.getX() + 200);
                maxY = Math.max(maxY, t.getY() + 30);
            }
        }
        canvas.setPrefSize(Math.max(600, maxX + 40), Math.max(400, maxY + 40));
    }
}
