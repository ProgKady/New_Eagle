package com.eagle.tools;

import com.eagle.util.ThemeManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ApiTester {

    public static void show(Window owner) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.setTitle("API Tester");

        ComboBox<String> methodBox = new ComboBox<>();
        methodBox.getItems().addAll("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS");
        methodBox.setValue("GET");

        TextField urlField = new TextField("https://api.github.com/repos/tabler/tabler-icons");
        urlField.setPromptText("https://api.example.com/endpoint");

        TextArea headersArea = new TextArea();
        headersArea.setPromptText("Header-Name: Value\nContent-Type: application/json");
        headersArea.setPrefRowCount(4);

        TextArea bodyArea = new TextArea();
        bodyArea.setPromptText("Request body (for POST/PUT)...");
        bodyArea.setPrefRowCount(6);

        TextArea responseArea = new TextArea();
        responseArea.setEditable(false);
        responseArea.setPrefRowCount(12);
        responseArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px;");

        Label statusLabel = new Label();
        Button sendBtn = new Button("Send Request");
        sendBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");

        TabPane tabs = new TabPane();
        Tab headersTab = new Tab("Headers", headersArea);
        Tab bodyTab = new Tab("Body", bodyArea);
        tabs.getTabs().addAll(headersTab, bodyTab);

        sendBtn.setOnAction(e -> {
            responseArea.clear();
            statusLabel.setText("Sending...");
            new Thread(() -> {
                try {
                    String method = methodBox.getValue();
                    String urlStr = urlField.getText().trim();
                    if (urlStr.isEmpty()) { Platform.runLater(() -> statusLabel.setText("Enter a URL")); return; }

                    HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                    conn.setRequestMethod(method);
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(30000);

                    // Parse custom headers
                    for (String h : headersArea.getText().split("\n")) {
                        h = h.trim();
                        if (h.contains(":")) {
                            int colon = h.indexOf(':');
                            conn.setRequestProperty(h.substring(0, colon).trim(), h.substring(colon + 1).trim());
                        }
                    }

                    if (method.equals("POST") || method.equals("PUT") || method.equals("PATCH")) {
                        String body = bodyArea.getText();
                        if (!body.isEmpty()) {
                            conn.setDoOutput(true);
                            try (OutputStream os = conn.getOutputStream()) {
                                os.write(body.getBytes(StandardCharsets.UTF_8));
                            }
                        }
                    }

                    int code = conn.getResponseCode();
                    StringBuilder resp = new StringBuilder();
                    try (InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream()) {
                        if (is != null) {
                            Scanner s = new Scanner(is, "UTF-8");
                            while (s.hasNextLine()) resp.append(s.nextLine()).append("\n");
                        }
                    }
                    String headers = "";
                    for (Map.Entry<String, List<String>> h : conn.getHeaderFields().entrySet()) {
                        if (h.getKey() != null) headers += h.getKey() + ": " + String.join(", ", h.getValue()) + "\n";
                    }
                    final String finalResp = resp.toString();
                    final String finalHeaders = headers;
                    final int finalCode = code;
                    javafx.application.Platform.runLater(() -> {
                        // Format JSON if possible
                        String display = finalResp;
                        if (finalResp.trim().startsWith("{") || finalResp.trim().startsWith("[")) {
                            try {
                                com.google.gson.Gson gson = new com.google.gson.Gson();
                                Object obj = gson.fromJson(finalResp, Object.class);
                                display = new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(obj);
                            } catch (Exception ignored) {}
                        }
                        responseArea.setText(display);
                        statusLabel.setText("Status: " + finalCode + "  |  Size: " + finalResp.length() + " bytes  |  Headers: " + finalHeaders.split("\n").length + " headers");
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        responseArea.setText("Error: " + ex.toString());
                        statusLabel.setText("Failed");
                    });
                }
            }).start();
        });

        HBox methodUrl = new HBox(6, methodBox, urlField);
        HBox.setHgrow(urlField, Priority.ALWAYS);

        VBox root = new VBox(8,
            new Label("API Tester - Test HTTP endpoints"),
            methodUrl,
            tabs,
            sendBtn,
            statusLabel,
            new Label("Response:"),
            responseArea);
        root.setPadding(new Insets(12));

        Scene scene = new Scene(root, 650, 600);
        ThemeManager.getInstance().applyTheme(scene);
        stage.setScene(scene);
        stage.show();
    }
}
