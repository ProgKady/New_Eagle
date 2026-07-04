package com.eagle.lsp;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JSON-RPC 2.0 / LSP client that communicates with a language server over stdio.
 */
public class LspClient {

    private static final Gson GSON = new Gson();

    private final AtomicInteger msgId = new AtomicInteger(1);
    private final ConcurrentHashMap<Integer, CompletableFuture<JsonObject>> pending = new ConcurrentHashMap<>();

    private Process serverProcess;
    private BufferedReader reader;
    private OutputStreamWriter writer;
    private Thread readerThread;
    private volatile boolean running;

    private volatile ServerCapabilities capabilities;

    public static class ServerCapabilities {
        public boolean completionProvider;
        public boolean hoverProvider;
        public boolean definitionProvider;
        public boolean referencesProvider;
        public boolean renameProvider;
        public boolean documentSymbolProvider;
        public boolean diagnosticsProvider;
        public boolean signatureHelpProvider;
    }

    /**
     * Start a language server process.
     * @param command   the command to start the server (e.g. "typescript-language-server --stdio")
     * @param rootUri   workspace root URI string (e.g. "file:///path/to/project")
     */
    public CompletableFuture<Boolean> start(String[] command, String rootUri) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false);
        serverProcess = pb.start();
        writer = new OutputStreamWriter(serverProcess.getOutputStream(), StandardCharsets.UTF_8);
        reader = new BufferedReader(new InputStreamReader(serverProcess.getInputStream(), StandardCharsets.UTF_8));

        running = true;

        readerThread = new Thread(this::readLoop, "lsp-reader");
        readerThread.setDaemon(true);
        readerThread.start();

        // Send initialize request
        JsonObject params = new JsonObject();
        String pidStr = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
        try { params.addProperty("processId", Integer.parseInt(pidStr)); } catch (Exception e) { params.addProperty("processId", (Number) null); }
        params.addProperty("rootUri", rootUri);

        JsonObject capabilities = new JsonObject();
        JsonObject textDoc = new JsonObject();
        textDoc.addProperty("synchronization", 1);
        textDoc.addProperty("completion", 1);
        textDoc.addProperty("hover", 1);
        textDoc.addProperty("definition", 1);
        textDoc.addProperty("references", 1);
        textDoc.addProperty("rename", 1);
        textDoc.addProperty("documentSymbol", 1);
        textDoc.addProperty("signatureHelp", 1);
        capabilities.add("textDocument", textDoc);

        JsonObject workspace = new JsonObject();
        workspace.addProperty("applyEdit", false);
        capabilities.add("workspace", workspace);

        params.add("capabilities", capabilities);

        return sendRequest("initialize", params).thenApply(result -> {
            parseCapabilities(result);
            sendNotification("initialized", new JsonObject());
            return true;
        });
    }

    private void parseCapabilities(JsonObject result) {
        ServerCapabilities caps = new ServerCapabilities();
        if (result != null && result.has("capabilities")) {
            JsonObject c = result.getAsJsonObject("capabilities");
            caps.completionProvider = c.has("completionProvider");
            caps.hoverProvider = c.has("hoverProvider");
            caps.definitionProvider = c.has("definitionProvider");
            caps.referencesProvider = c.has("referencesProvider");
            caps.renameProvider = c.has("renameProvider");
            caps.documentSymbolProvider = c.has("documentSymbolProvider");
            caps.diagnosticsProvider = c.has("textDocumentSync");
            caps.signatureHelpProvider = c.has("signatureHelpProvider");
        }
        this.capabilities = caps;
    }

    public ServerCapabilities getCapabilities() {
        return capabilities;
    }

    // ---- Document notifications ----

    public void didOpen(String uri, String languageId, String text) {
        JsonObject textDoc = new JsonObject();
        textDoc.addProperty("uri", uri);
        textDoc.addProperty("languageId", languageId);
        textDoc.addProperty("version", 1);
        textDoc.addProperty("text", text);
        JsonObject params = new JsonObject();
        params.add("textDocument", textDoc);
        sendNotification("textDocument/didOpen", params);
    }

    public void didChange(String uri, String text, int version) {
        JsonObject textDoc = new JsonObject();
        textDoc.addProperty("uri", uri);
        textDoc.addProperty("version", version);

        JsonObject change = new JsonObject();
        change.addProperty("text", text);
        JsonArray changes = new JsonArray();
        changes.add(change);

        JsonObject params = new JsonObject();
        params.add("textDocument", textDoc);
        params.add("contentChanges", changes);
        sendNotification("textDocument/didChange", params);
    }

    public void didClose(String uri) {
        JsonObject textDoc = new JsonObject();
        textDoc.addProperty("uri", uri);
        JsonObject params = new JsonObject();
        params.add("textDocument", textDoc);
        sendNotification("textDocument/didClose", params);
    }

    // ---- LSP requests ----

    public CompletableFuture<JsonObject> completion(String uri, int line, int column) {
        JsonObject params = positionParams(uri, line, column);
        return sendRequest("textDocument/completion", params);
    }

    public CompletableFuture<JsonObject> hover(String uri, int line, int column) {
        JsonObject params = positionParams(uri, line, column);
        return sendRequest("textDocument/hover", params);
    }

    public CompletableFuture<JsonObject> definition(String uri, int line, int column) {
        JsonObject params = positionParams(uri, line, column);
        return sendRequest("textDocument/definition", params);
    }

    public CompletableFuture<JsonObject> references(String uri, int line, int column, boolean includeDeclaration) {
        JsonObject params = positionParams(uri, line, column);
        JsonObject context = new JsonObject();
        context.addProperty("includeDeclaration", includeDeclaration);
        params.add("context", context);
        return sendRequest("textDocument/references", params);
    }

    public CompletableFuture<JsonObject> rename(String uri, int line, int column, String newName) {
        JsonObject params = positionParams(uri, line, column);
        params.addProperty("newName", newName);
        return sendRequest("textDocument/rename", params);
    }

    public CompletableFuture<JsonObject> documentSymbol(String uri) {
        JsonObject params = new JsonObject();
        JsonObject textDoc = new JsonObject();
        textDoc.addProperty("uri", uri);
        params.add("textDocument", textDoc);
        return sendRequest("textDocument/documentSymbol", params);
    }

    public CompletableFuture<JsonObject> signatureHelp(String uri, int line, int column) {
        JsonObject params = positionParams(uri, line, column);
        return sendRequest("textDocument/signatureHelp", params);
    }

    // ---- Shutdown ----

    public void shutdown() {
        try {
            sendRequest("shutdown", new JsonObject()).get();
        } catch (Exception ignored) {}
        try { sendNotification("exit", new JsonObject()); } catch (Exception ignored) {}
        running = false;
        running = false;
        if (readerThread != null) readerThread.interrupt();
        try { if (serverProcess != null) serverProcess.destroy(); } catch (Exception ignored) {}
    }

    // ---- Internal JSON-RPC ----

    private JsonObject positionParams(String uri, int line, int column) {
        JsonObject textDoc = new JsonObject();
        textDoc.addProperty("uri", uri);
        JsonObject pos = new JsonObject();
        pos.addProperty("line", line);
        pos.addProperty("character", column);
        JsonObject params = new JsonObject();
        params.add("textDocument", textDoc);
        params.add("position", pos);
        return params;
    }

    private CompletableFuture<JsonObject> sendRequest(String method, JsonObject params) {
        int id = msgId.getAndIncrement();
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        pending.put(id, future);

        JsonObject msg = new JsonObject();
        msg.addProperty("jsonrpc", "2.0");
        msg.addProperty("id", id);
        msg.addProperty("method", method);
        msg.add("params", params);
        writeMessage(msg);

        return future;
    }

    private void sendNotification(String method, JsonObject params) {
        JsonObject msg = new JsonObject();
        msg.addProperty("jsonrpc", "2.0");
        msg.addProperty("method", method);
        msg.add("params", params);
        writeMessage(msg);
    }

    private synchronized void writeMessage(JsonObject msg) {
        if (writer == null) return;
        try {
            String json = GSON.toJson(msg);
            String header = "Content-Length: " + json.getBytes(StandardCharsets.UTF_8).length + "\r\n\r\n";
            writer.write(header);
            writer.write(json);
            writer.flush();
        } catch (IOException e) {
            System.err.println("LSP write error: " + e.getMessage());
        }
    }

    private void readLoop() {
        try {
            while (running) {
                String line = reader.readLine();
                if (line == null) break;
                if (line.startsWith("Content-Length: ")) {
                    int len = Integer.parseInt(line.substring(16).trim());
                    // Skip remaining headers
                    while ((line = reader.readLine()) != null && !line.isEmpty()) {}
                    if (line == null) break;
                    char[] buf = new char[len];
                    int off = 0;
                    while (off < len) {
                        int n = reader.read(buf, off, len - off);
                        if (n < 0) break;
                        off += n;
                    }
                    String jsonStr = new String(buf, 0, off);
                    JsonObject msg = JsonParser.parseString(jsonStr).getAsJsonObject();
                    handleMessage(msg);
                }
            }
        } catch (IOException e) {
            if (running) System.err.println("LSP read error: " + e.getMessage());
        }
        running = false;
    }

    private void handleMessage(JsonObject msg) {
        if (msg.has("id")) {
            int id = msg.get("id").getAsInt();
            CompletableFuture<JsonObject> future = pending.remove(id);
            if (future != null) {
                if (msg.has("result") && !msg.get("result").isJsonNull()) {
                    future.complete(msg.getAsJsonObject("result"));
                } else if (msg.has("error")) {
                    future.completeExceptionally(
                        new RuntimeException("LSP error: " + msg.getAsJsonObject("error")));
                } else {
                    future.complete(new JsonObject());
                }
            }
        } else if (msg.has("method")) {
            String method = msg.get("method").getAsString();
            JsonObject params = msg.has("params") ? msg.getAsJsonObject("params") : new JsonObject();
            handleNotification(method, params);
        }
    }

    private void handleNotification(String method, JsonObject params) {
        if ("textDocument/publishDiagnostics".equals(method)) {
            if (diagnosticsCallback != null) {
                String uri = params.get("uri").getAsString();
                JsonArray diags = params.getAsJsonArray("diagnostics");
                List<LspDiagnostic> list = new ArrayList<>();
                for (int i = 0; i < diags.size(); i++) {
                    JsonObject d = diags.get(i).getAsJsonObject();
                    LspDiagnostic diag = new LspDiagnostic();
                    JsonObject range = d.getAsJsonObject("range");
                    JsonObject start = range.getAsJsonObject("start");
                    JsonObject end = range.getAsJsonObject("end");
                    diag.startLine = start.get("line").getAsInt();
                    diag.startChar = start.get("character").getAsInt();
                    diag.endLine = end.get("line").getAsInt();
                    diag.endChar = end.get("character").getAsInt();
                    diag.severity = d.has("severity") ? d.get("severity").getAsInt() : 1;
                    diag.message = d.get("message").getAsString();
                    diag.source = d.has("source") ? d.get("source").getAsString() : "";
                    list.add(diag);
                }
                diagnosticsCallback.accept(uri, list);
            }
        }
    }

    // ---- Callbacks ----

    private DiagnosticsCallback diagnosticsCallback;

    public void setDiagnosticsCallback(DiagnosticsCallback cb) {
        this.diagnosticsCallback = cb;
    }

    @FunctionalInterface
    public interface DiagnosticsCallback {
        void accept(String uri, List<LspDiagnostic> diagnostics);
    }

    public static class LspDiagnostic {
        public int startLine, startChar, endLine, endChar;
        public int severity; // 1=error, 2=warning, 3=info, 4=hint
        public String message;
        public String source;
    }
}
