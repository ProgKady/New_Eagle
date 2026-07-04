package com.eagle.editor;

import com.eagle.plugin.PluginManager;
import com.eagle.util.ThemeManager;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ExtensionsMarketplaceDialog {

    private static final String DEFAULT_REGISTRY = "https://raw.githubusercontent.com/ProgKady/Eagle-IDE-Extensions/main/extensions.json";
    private static final File EXTENSIONS_DIR = PluginManager.getInstance().getPluginsDir();

    private final Stage stage = new Stage();
    private final TableView<ExtensionInfo> table = new TableView<>();
    private final ObservableList<ExtensionInfo> items = FXCollections.observableArrayList();
    private final Label statusLabel = new Label("Connect to the internet to browse extensions.");
    private final Button installBtn = new Button("Install");
    private final TextField registryField;

    public ExtensionsMarketplaceDialog() {
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Extensions Marketplace");

        registryField = new TextField(DEFAULT_REGISTRY);
        registryField.setDisable(true);
        registryField.setOpacity(1);
        registryField.setVisible(false);
        registryField.setPrefColumnCount(50);

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(e -> fetchExtensions());

        HBox registryBar = new HBox(8,  registryField, refreshBtn);
        registryBar.setPadding(new Insets(8));

        TableColumn<ExtensionInfo, String> nameCol = new TableColumn<>("Extension");
        nameCol.setPrefWidth(180);
        nameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().name));

        TableColumn<ExtensionInfo, String> descCol = new TableColumn<>("Description");
        descCol.setPrefWidth(350);
        descCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().description));

        TableColumn<ExtensionInfo, String> authorCol = new TableColumn<>("Author");
        authorCol.setPrefWidth(120);
        authorCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().author));

        TableColumn<ExtensionInfo, String> verCol = new TableColumn<>("Version");
        verCol.setPrefWidth(70);
        verCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().version));

        TableColumn<ExtensionInfo, String> dlCol = new TableColumn<>("Downloads");
        dlCol.setPrefWidth(80);
        dlCol.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().downloads)));

        table.getColumns().addAll(nameCol, descCol, authorCol, verCol, dlCol);
        table.setItems(items);
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            installBtn.setDisable(sel == null || isInstalled(sel.id));
        });
        table.setRowFactory(tv -> {
            TableRow<ExtensionInfo> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    installExtension(row.getItem());
                }
            });
            return row;
        });

        VBox.setVgrow(table, Priority.ALWAYS);

        installBtn.setDisable(true);
        installBtn.setOnAction(e -> {
            ExtensionInfo sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) installExtension(sel);
        });

        Button closeBtn = new Button("Close");
        closeBtn.setOnAction(e -> stage.close());

        HBox btnBar = new HBox(8, installBtn, closeBtn);
        btnBar.setAlignment(Pos.CENTER_RIGHT);
        btnBar.setPadding(new Insets(8));

        statusLabel.setPadding(new Insets(4, 8, 0, 8));

        VBox root = new VBox(registryBar, table, statusLabel, btnBar);
        root.setPrefSize(850, 500);

        Scene scene = new Scene(root);
        ThemeManager.getInstance().applyTheme(scene);
        stage.setScene(scene);
        
        Platform.runLater(()-> {
    fetchExtensions();
    });
        
    }

    public void show() {
        stage.showAndWait(); 
    }

    private void fetchExtensions() {
        final String registryUrl;
        String url = registryField.getText().trim();
        if (url.isEmpty()) registryUrl = DEFAULT_REGISTRY;
        else registryUrl = url;
        statusLabel.setText("Fetching extensions...");

        new Thread(() -> {
            try {
                String json = readUrl(registryUrl);
                List<ExtensionInfo> list = parseExtensions(json);
                Platform.runLater(() -> {
                    items.setAll(list);
                    statusLabel.setText(list.size() + " extensions found");
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Error: " + e.getMessage()));
            }
        }).start();
    }

    private String readUrl(String urlStr) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("User-Agent", "EagleIDE");
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    private List<ExtensionInfo> parseExtensions(String json) {
        List<ExtensionInfo> list = new ArrayList<>();
        try {
            com.google.gson.JsonArray arr = new com.google.gson.JsonParser().parse(json).getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                com.google.gson.JsonObject obj = arr.get(i).getAsJsonObject();
                ExtensionInfo info = new ExtensionInfo();
                info.id = getStr(obj, "id");
                info.name = getStr(obj, "name");
                info.description = getStr(obj, "description");
                info.version = getStr(obj, "version");
                info.author = getStr(obj, "author");
                info.downloadUrl = getStr(obj, "download_url");
                info.iconUrl = getStr(obj, "icon_url");
                info.category = getStr(obj, "category");
                info.downloads = getInt(obj, "downloads");
                list.add(info);
            }
        } catch (Exception e) {
            System.err.println("Parse error: " + e.getMessage());
        }
        return list;
    }

    private String getStr(com.google.gson.JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
    }

    private int getInt(com.google.gson.JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsInt() : 0;
    }

    private boolean isInstalled(String id) {
        return new File(EXTENSIONS_DIR, id + ".jar").exists();
    }

    private void installExtension(ExtensionInfo info) {
        if (info.downloadUrl == null || info.downloadUrl.isEmpty()) {
            statusLabel.setText("No download URL for " + info.name);
            return;
        }
        statusLabel.setText("Downloading " + info.name + "...");
        installBtn.setDisable(true);

        new Thread(() -> {
            try {
                File jarFile = new File(EXTENSIONS_DIR, info.id + ".jar");

                HttpURLConnection conn = (HttpURLConnection) new URL(info.downloadUrl).openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(30000);
                conn.setRequestProperty("User-Agent", "EagleIDE");

                if (info.downloadUrl.endsWith(".zip")) {
                    File dir = new File(EXTENSIONS_DIR, info.id);
                    dir.mkdirs();
                    try (ZipInputStream zis = new ZipInputStream(conn.getInputStream())) {
                        ZipEntry entry;
                        byte[] buf = new byte[8192];
                        while ((entry = zis.getNextEntry()) != null) {
                            File out = new File(dir, entry.getName());
                            if (entry.isDirectory()) {
                                out.mkdirs();
                            } else {
                                out.getParentFile().mkdirs();
                                try (FileOutputStream fos = new FileOutputStream(out)) {
                                    int n;
                                    while ((n = zis.read(buf)) > 0) fos.write(buf, 0, n);
                                }
                            }
                            zis.closeEntry();
                        }
                    }
                } else {
                    EXTENSIONS_DIR.mkdirs();
                    try (InputStream is = conn.getInputStream();
                         FileOutputStream fos = new FileOutputStream(jarFile)) {
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = is.read(buf)) > 0) fos.write(buf, 0, n);
                    }
                }
                conn.disconnect();

                PluginManager.getInstance().loadAll();

                Platform.runLater(() -> {
                    statusLabel.setText("✓ Installed " + info.name + " v" + info.version);
                    table.refresh();
                    installBtn.setDisable(isInstalled(info.id));
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("✗ Install failed: " + e.getMessage());
                    installBtn.setDisable(false);
                });
            }
        }).start();
    }
}
