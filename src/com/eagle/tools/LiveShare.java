package com.eagle.tools;

import com.eagle.util.ThemeManager;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.*;

public class LiveShare {

    private static ServerSocket server;
    private static Thread serverThread;
    private static volatile boolean running = false;
    private static File sharedRoot;

    private static final Map<String, String> MIME_TYPES = new HashMap<>();
    static {
        MIME_TYPES.put("html", "text/html; charset=utf-8");
        MIME_TYPES.put("htm", "text/html; charset=utf-8");
        MIME_TYPES.put("css", "text/css; charset=utf-8");
        MIME_TYPES.put("js", "application/javascript; charset=utf-8");
        MIME_TYPES.put("json", "application/json; charset=utf-8");
        MIME_TYPES.put("xml", "application/xml; charset=utf-8");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("gif", "image/gif");
        MIME_TYPES.put("svg", "image/svg+xml");
        MIME_TYPES.put("ico", "image/x-icon");
        MIME_TYPES.put("pdf", "application/pdf");
        MIME_TYPES.put("zip", "application/zip");
        MIME_TYPES.put("txt", "text/plain; charset=utf-8");
        MIME_TYPES.put("md", "text/markdown; charset=utf-8");
        MIME_TYPES.put("java", "text/plain; charset=utf-8");
        MIME_TYPES.put("py", "text/plain; charset=utf-8");
        MIME_TYPES.put("ts", "text/plain; charset=utf-8");
        MIME_TYPES.put("tsx", "text/plain; charset=utf-8");
        MIME_TYPES.put("jsx", "text/plain; charset=utf-8");
        MIME_TYPES.put("yaml", "text/plain; charset=utf-8");
        MIME_TYPES.put("yml", "text/plain; charset=utf-8");
        MIME_TYPES.put("sh", "text/plain; charset=utf-8");
        MIME_TYPES.put("bat", "text/plain; charset=utf-8");
        MIME_TYPES.put("ps1", "text/plain; charset=utf-8");
        MIME_TYPES.put("properties", "text/plain; charset=utf-8");
        MIME_TYPES.put("gradle", "text/plain; charset=utf-8");
        MIME_TYPES.put("toml", "text/plain; charset=utf-8");
        MIME_TYPES.put("conf", "text/plain; charset=utf-8");
        MIME_TYPES.put("sql", "text/plain; charset=utf-8");
        MIME_TYPES.put("csv", "text/csv; charset=utf-8");
        MIME_TYPES.put("woff2", "font/woff2");
        MIME_TYPES.put("woff", "font/woff");
        MIME_TYPES.put("ttf", "font/ttf");
        MIME_TYPES.put("otf", "font/otf");
        MIME_TYPES.put("wasm", "application/wasm");
        MIME_TYPES.put("map", "application/json");
        MIME_TYPES.put("kt", "text/plain; charset=utf-8");
        MIME_TYPES.put("go", "text/plain; charset=utf-8");
        MIME_TYPES.put("rs", "text/plain; charset=utf-8");
        MIME_TYPES.put("dart", "text/plain; charset=utf-8");
        MIME_TYPES.put("swift", "text/plain; charset=utf-8");
        MIME_TYPES.put("rb", "text/plain; charset=utf-8");
        MIME_TYPES.put("php", "text/plain; charset=utf-8");
        MIME_TYPES.put("cpp", "text/plain; charset=utf-8");
        MIME_TYPES.put("c", "text/plain; charset=utf-8");
        MIME_TYPES.put("h", "text/plain; charset=utf-8");
        MIME_TYPES.put("vue", "text/plain; charset=utf-8");
        MIME_TYPES.put("svelte", "text/plain; charset=utf-8");
        MIME_TYPES.put("astro", "text/plain; charset=utf-8");
    }

    public static void show(Window owner) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.setTitle("Live Share — HTTP File Server & Tools");

        Label status = new Label("Server: Stopped");
        status.setStyle("-fx-text-fill: #888; -fx-font-size: 13px; -fx-font-weight: bold;");

        TextField portField = new TextField("8080");
        portField.setPrefColumnCount(6);

        TextField rootField = new TextField(System.getProperty("user.dir"));
        rootField.setPrefColumnCount(30);

        Button browseBtn = new Button("Browse");
        browseBtn.setStyle("-fx-cursor: hand;");
        browseBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select shared folder");
            File dir = dc.showDialog(stage);
            if (dir != null) rootField.setText(dir.getAbsolutePath());
        });

        TextArea log = new TextArea();
        log.setEditable(false);
        log.setPrefRowCount(8);
        log.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px;");

        Button startBtn = new Button("Start Server");
        startBtn.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 18;");
        Button stopBtn = new Button("Stop");
        stopBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 18;");
        stopBtn.setDisable(true);

        Label urlLabel = new Label();
        urlLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -accent; -fx-cursor: hand;");
        urlLabel.setOnMouseClicked(e -> {
            String url = urlLabel.getText();
            if (!url.isEmpty() && !url.startsWith("Server")) {
                try { java.awt.Desktop.getDesktop().browse(new URI(url)); }
                catch (Exception ignored) {}
            }
        });

        // File comparison tab
        TabPane tabs = new TabPane();
        Tab serverTab = new Tab("Server");
        Tab diffTab = new Tab("File Compare");

        // Diff panel
        VBox diffPanel = new VBox(6);
        diffPanel.setPadding(new Insets(8));
        Label diffDesc = new Label("Compare two files or directories:");
        TextField file1Field = new TextField();
        file1Field.setPromptText("First file path");
        TextField file2Field = new TextField();
        file2Field.setPromptText("Second file path");
        Button browse1Btn = new Button("Browse");
        Button browse2Btn = new Button("Browse");
        HBox row1 = new HBox(6, file1Field, browse1Btn);
        HBox row2 = new HBox(6, file2Field, browse2Btn);
        HBox.setHgrow(file1Field, Priority.ALWAYS);
        HBox.setHgrow(file2Field, Priority.ALWAYS);
        browse1Btn.setOnAction(ev -> {
            FileChooser fc = new FileChooser();
            File f = fc.showOpenDialog(stage);
            if (f != null) file1Field.setText(f.getAbsolutePath());
        });
        browse2Btn.setOnAction(ev -> {
            FileChooser fc = new FileChooser();
            File f = fc.showOpenDialog(stage);
            if (f != null) file2Field.setText(f.getAbsolutePath());
        });
        TextArea diffResult = new TextArea();
        diffResult.setEditable(false);
        diffResult.setPrefRowCount(12);
        diffResult.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px;");
        Button compareBtn = new Button("Compare");
        compareBtn.setStyle("-fx-background-color: -accent; -fx-text-fill: white; -fx-cursor: hand;");
        compareBtn.setOnAction(ev -> {
            String p1 = file1Field.getText().trim();
            String p2 = file2Field.getText().trim();
            if (p1.isEmpty() || p2.isEmpty()) { diffResult.setText("Please specify both files/directories."); return; }
            try {
                String diff = compareFiles(new File(p1), new File(p2));
                diffResult.setText(diff);
            } catch (Exception ex) {
                diffResult.setText("Error: " + ex.getMessage());
            }
        });
        diffPanel.getChildren().addAll(diffDesc, row1, row2, compareBtn, diffResult);
        diffTab.setContent(diffPanel);

        // Server log content
        VBox serverContent = new VBox(6);
        serverContent.setPadding(new Insets(8));
        HBox portRow = new HBox(6, new Label("Port:"), portField, new Label("Root:"), rootField, browseBtn);
        HBox btnRow = new HBox(6, startBtn, stopBtn, urlLabel);
        serverContent.getChildren().addAll(status, portRow, btnRow, new Label("Server Log:"), log);
        serverTab.setContent(serverContent);

        tabs.getTabs().addAll(serverTab, diffTab);

        startBtn.setOnAction(e -> {
            if (running) return;
            try {
                int port = Integer.parseInt(portField.getText().trim());
                String rootPath = rootField.getText().trim();
                sharedRoot = new File(rootPath);
                if (!sharedRoot.isDirectory()) {
                    log.appendText("Error: Not a valid directory: " + rootPath + "\n");
                    return;
                }

                server = new ServerSocket(port);
                running = true;
                status.setText("Server: Running on port " + port);
                status.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 13px; -fx-font-weight: bold;");
                log.appendText("[" + timestamp() + "] Server started\n");
                log.appendText("[" + timestamp() + "] Sharing: " + sharedRoot.getAbsolutePath() + "\n");
                log.appendText("[" + timestamp() + "] Files: " + countFiles(sharedRoot) + " files\n");

                String localIp = InetAddress.getLocalHost().getHostAddress();
                String shareUrl = "http://" + localIp + ":" + port;
                urlLabel.setText(shareUrl);
                ClipboardContent cc = new ClipboardContent();
                cc.putString(shareUrl);
                Clipboard.getSystemClipboard().setContent(cc);
                log.appendText("[" + timestamp() + "] URL copied to clipboard\n");
                log.appendText("[" + timestamp() + "] LAN: " + shareUrl + "\n");
                log.appendText("[" + timestamp() + "] Local: http://localhost:" + port + "\n");

                startBtn.setDisable(true);
                stopBtn.setDisable(false);
                portField.setDisable(true);
                rootField.setDisable(true);
                browseBtn.setDisable(true);

                serverThread = new Thread(LiveShare::serverLoop);
                serverThread.setDaemon(true);
                serverThread.start();
            } catch (Exception ex) {
                log.appendText("[" + timestamp() + "] Error: " + ex.getMessage() + "\n");
            }
        });

        stopBtn.setOnAction(e -> {
            stopServer(log);
            startBtn.setDisable(false);
            stopBtn.setDisable(true);
            portField.setDisable(false);
            rootField.setDisable(false);
            browseBtn.setDisable(false);
            urlLabel.setText("");
        });

        VBox root = new VBox(tabs);
        root.setPadding(new Insets(4));

        Scene scene = new Scene(root, 600, 500);
        ThemeManager.getInstance().applyTheme(scene);
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> stopServer(null));
        stage.show();
    }

    private static String compareFiles(File f1, File f2) throws Exception {
        if (f1.isDirectory() && f2.isDirectory()) {
            return compareDirectories(f1, f2);
        }
        if (!f1.exists() || !f2.exists()) {
            return "Error: Both files must exist.";
        }
        if (f1.isFile() && f2.isFile()) {
            String c1 = new String(Files.readAllBytes(f1.toPath()), StandardCharsets.UTF_8);
            String c2 = new String(Files.readAllBytes(f2.toPath()), StandardCharsets.UTF_8);
            if (c1.equals(c2)) {
                return "Files are identical.\n\n" + f1.getName() + ": " + f1.length() + " bytes\n" + f2.getName() + ": " + f2.length() + " bytes";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("=== DIFF: ").append(f1.getName()).append(" vs ").append(f2.getName()).append(" ===\n\n");
            String[] lines1 = c1.split("\n", -1);
            String[] lines2 = c2.split("\n", -1);
            int max = Math.max(lines1.length, lines2.length);
            int diffCount = 0;
            for (int i = 0; i < max; i++) {
                String l1 = i < lines1.length ? lines1[i] : "";
                String l2 = i < lines2.length ? lines2[i] : "";
                if (!l1.equals(l2)) {
                    diffCount++;
                    if (diffCount <= 100) {
                        String lineNum = String.format("%4d", i + 1);
                        if (!l1.isEmpty()) sb.append("- ").append(lineNum).append(": ").append(l1).append("\n");
                        if (!l2.isEmpty()) sb.append("+ ").append(lineNum).append(": ").append(l2).append("\n");
                    }
                }
            }
            if (diffCount > 100) {
                sb.append("\n... and ").append(diffCount - 100).append(" more differences\n");
            }
            sb.append("\nTotal differences: ").append(diffCount);
            return sb.toString();
        }
        return "Error: Both paths must be either files or directories.";
    }

    private static String compareDirectories(File d1, File d2) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== DIRECTORY COMPARE ===\n\n");
        Set<String> files1 = new TreeSet<>();
        Set<String> files2 = new TreeSet<>();
        listFiles(d1, "", files1);
        listFiles(d2, "", files2);

        Set<String> onlyIn1 = new TreeSet<>(files1);
        onlyIn1.removeAll(files2);
        Set<String> onlyIn2 = new TreeSet<>(files2);
        onlyIn2.removeAll(files1);
        Set<String> common = new TreeSet<>(files1);
        common.retainAll(files2);

        if (!onlyIn1.isEmpty()) {
            sb.append("Only in ").append(d1.getName()).append(":\n");
            for (String f : onlyIn1) sb.append("  ").append(f).append("\n");
            sb.append("\n");
        }
        if (!onlyIn2.isEmpty()) {
            sb.append("Only in ").append(d2.getName()).append(":\n");
            for (String f : onlyIn2) sb.append("  ").append(f).append("\n");
            sb.append("\n");
        }
        sb.append("Common files: ").append(common.size()).append("\n");
        sb.append("Differences in common files:\n");
        int diffCount = 0;
        for (String path : common) {
            try {
                File f1 = new File(d1, path);
                File f2 = new File(d2, path);
                if (f1.length() != f2.length()) {
                    sb.append("  * ").append(path).append(" (size: ").append(f1.length()).append(" vs ").append(f2.length()).append(")\n");
                    diffCount++;
                } else {
                    String c1 = new String(Files.readAllBytes(f1.toPath()), StandardCharsets.UTF_8);
                    String c2 = new String(Files.readAllBytes(f2.toPath()), StandardCharsets.UTF_8);
                    if (!c1.equals(c2)) {
                        sb.append("  * ").append(path).append(" (content differs)\n");
                        diffCount++;
                    }
                }
            } catch (Exception ignored) {}
        }
        if (diffCount == 0) sb.append("  (none — all common files are identical)\n");
        sb.append("\nSummary:\n");
        sb.append("  ").append(d1.getName()).append(": ").append(files1.size()).append(" files\n");
        sb.append("  ").append(d2.getName()).append(": ").append(files2.size()).append(" files\n");
        sb.append("  Common: ").append(common.size()).append(" files\n");
        sb.append("  Different: ").append(diffCount).append(" files\n");
        return sb.toString();
    }

    private static void listFiles(File dir, String prefix, Set<String> results) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            String name = f.getName();
            if (name.startsWith(".")) continue;
            String rel = prefix.isEmpty() ? name : prefix + "/" + name;
            if (f.isDirectory()) {
                listFiles(f, rel, results);
            } else {
                results.add(rel);
            }
        }
    }

    private static int countFiles(File dir) {
        int count = 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;
        for (File f : files) {
            if (f.isDirectory()) count += countFiles(f);
            else count++;
        }
        return count;
    }

    private static void stopServer(TextArea log) {
        running = false;
        try { if (server != null) server.close(); } catch (Exception ignored) {}
        if (log != null) log.appendText("[" + timestamp() + "] Server stopped\n");
    }

    private static void serverLoop() {
        try {
            while (running && !server.isClosed()) {
                Socket client = server.accept();
                handleClient(client);
            }
        } catch (Exception ignored) {}
    }

    private static void handleClient(Socket client) {
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
            OutputStream out = client.getOutputStream();
            String requestLine = r.readLine();
            if (requestLine == null) { client.close(); return; }

            String line;
            while ((line = r.readLine()) != null && !line.isEmpty()) {}

            String[] parts = requestLine.split("\\s+");
            if (parts.length < 2) { client.close(); return; }
            String method = parts[0];
            String path = URLDecoder.decode(parts[1], "UTF-8");

            if (path.contains("..") || path.contains("~")) {
                sendError(out, 403, "Forbidden");
                client.close();
                return;
            }

            File file = new File(sharedRoot, path.startsWith("/") ? path.substring(1) : path);
            file = file.getCanonicalFile();

            if (!file.getAbsolutePath().startsWith(sharedRoot.getAbsolutePath())) {
                sendError(out, 403, "Forbidden");
                client.close();
                return;
            }

            if (file.isDirectory()) {
                if (!path.endsWith("/")) {
                    sendRedirect(out, path + "/");
                } else {
                    File index = new File(file, "index.html");
                    if (index.exists()) {
                        sendFile(out, index);
                    } else {
                        sendDirectoryListing(out, file, path);
                    }
                }
            } else if (file.exists()) {
                sendFile(out, file);
            } else {
                sendError(out, 404, "Not Found");
            }

            client.close();
        } catch (Exception ignored) {}
    }

    private static void sendFile(OutputStream out, File file) throws IOException {
        String mime = getMimeType(file.getName());
        byte[] data = Files.readAllBytes(file.toPath());
        String header = "HTTP/1.1 200 OK\r\n"
            + "Content-Type: " + mime + "\r\n"
            + "Content-Length: " + data.length + "\r\n"
            + "Accept-Ranges: bytes\r\n"
            + "Cache-Control: no-cache\r\n"
            + "Connection: close\r\n"
            + "\r\n";
        out.write(header.getBytes(StandardCharsets.UTF_8));
        out.write(data);
        out.flush();
    }

    private static void sendDirectoryListing(OutputStream out, File dir, String path) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        buf.write("<!DOCTYPE html>\n<html>\n<head>\n".getBytes());
        buf.write("<meta charset=\"UTF-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n".getBytes());
        buf.write(("<title>Index of " + path + "</title>\n").getBytes());
        buf.write("<style>\n".getBytes());
        buf.write("* { margin: 0; padding: 0; box-sizing: border-box; }\n".getBytes());
        buf.write("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #1a1a2e; color: #e0e0e0; padding: 24px; }\n".getBytes());
        buf.write("h1 { color: #fff; margin-bottom: 16px; font-size: 20px; }\n".getBytes());
        buf.write("a { color: #64b5f6; text-decoration: none; display: block; padding: 6px 10px; border-radius: 4px; }\n".getBytes());
        buf.write("a:hover { background: rgba(100,181,246,0.1); }\n".getBytes());
        buf.write("tr:hover { background: rgba(255,255,255,0.03); }\n".getBytes());
        buf.write("</style>\n</head>\n<body>\n".getBytes());

        buf.write(("<h1>Index of " + path + "</h1>\n").getBytes());
        buf.write("<table>\n".getBytes());

        if (!path.equals("/")) {
            String parent = path.substring(0, path.lastIndexOf('/', path.length() - 2) + 1);
            buf.write(("<tr><td><a href=\"" + parent + "\">[..]</a></td><td></td></tr>\n").getBytes());
        }

        File[] files = dir.listFiles();
        if (files != null) {
            Arrays.sort(files, (a, b) -> {
                if (a.isDirectory() && !b.isDirectory()) return -1;
                if (!a.isDirectory() && b.isDirectory()) return 1;
                return a.getName().compareToIgnoreCase(b.getName());
            });
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            for (File f : files) {
                String name = f.getName();
                if (name.startsWith(".")) continue;
                String fp = path + name;
                if (f.isDirectory()) fp += "/";
                String size = f.isDirectory() ? "-" : formatSize(f.length());
                String date = sdf.format(new Date(f.lastModified()));
                buf.write(("<tr><td><a href=\"" + fp + "\"> " + name + "</a></td><td>" + size + "</td><td>" + date + "</td></tr>\n").getBytes());
            }
        }

        buf.write("</table>\n</body>\n</html>".getBytes());
        byte[] data = buf.toByteArray();
        String header = "HTTP/1.1 200 OK\r\n"
            + "Content-Type: text/html; charset=utf-8\r\n"
            + "Content-Length: " + data.length + "\r\n"
            + "Connection: close\r\n"
            + "\r\n";
        out.write(header.getBytes(StandardCharsets.UTF_8));
        out.write(data);
        out.flush();
    }

    private static void sendError(OutputStream out, int code, String msg) throws IOException {
        String body = "<!DOCTYPE html><html><body><h1>" + code + " " + msg + "</h1></body></html>";
        String header = "HTTP/1.1 " + code + " " + msg + "\r\n"
            + "Content-Type: text/html\r\n"
            + "Content-Length: " + body.getBytes(StandardCharsets.UTF_8).length + "\r\n"
            + "Connection: close\r\n"
            + "\r\n";
        out.write(header.getBytes(StandardCharsets.UTF_8));
        out.write(body.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private static void sendRedirect(OutputStream out, String location) throws IOException {
        String header = "HTTP/1.1 301 Moved Permanently\r\n"
            + "Location: " + location + "\r\n"
            + "Content-Length: 0\r\n"
            + "Connection: close\r\n"
            + "\r\n";
        out.write(header.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private static String getMimeType(String name) {
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";
        return MIME_TYPES.getOrDefault(ext, "application/octet-stream");
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private static String timestamp() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date());
    }
}
