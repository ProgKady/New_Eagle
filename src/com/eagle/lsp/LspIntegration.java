package com.eagle.lsp;

import com.eagle.editor.CodeEditor;
import com.eagle.editor.CodeLinter;
import com.eagle.editor.CompletionProvider;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Bridges LSP client results into CodeEditor's existing UI infrastructure.
 * Converts LSP completions → CompletionProvider.Suggestion format
 * Converts LSP diagnostics → CodeLinter.Problem format
 * Handles go-to-definition, find-references, rename.
 */
public class LspIntegration {

    private static final Gson GSON = new Gson();

    private final CodeEditor editor;
    private final LspLanguageServer server;
    private LspClient client;
    private String currentUri;
    private String currentLanguageId;
    private int docVersion;

    public LspIntegration(CodeEditor editor, LspLanguageServer server) {
        this.editor = editor;
        this.server = server;
    }

    /**
     * Open a file in the LSP server. Call when a file is loaded in the editor.
     */
    public void openDocument(File file) {
        String ext = extensionOf(file.getName());
        currentLanguageId = server.getLanguageId(ext);
        client = server.getClient(currentLanguageId);
        if (client == null) return;

        currentUri = file.toURI().toString();
        docVersion = 1;
        String text = editor.getText();
        // Set up diagnostics callback
        client.setDiagnosticsCallback((uri, diags) -> {
            if (!uri.equals(currentUri)) return;
            Platform.runLater(() -> {
                List<CodeLinter.Problem> problems = new ArrayList<>();
                for (LspClient.LspDiagnostic d : diags) {
                    int startOffset = lineColToOffset(text, d.startLine, d.startChar);
                    int endOffset = lineColToOffset(text, d.endLine, d.endChar);
                    CodeLinter.Problem.Severity sev;
                    switch (d.severity) {
                        case 1: sev = CodeLinter.Problem.Severity.ERROR; break;
                        case 2: sev = CodeLinter.Problem.Severity.WARNING; break;
                        default: sev = CodeLinter.Problem.Severity.INFO; break;
                    }
                    problems.add(new CodeLinter.Problem(d.startLine, startOffset, endOffset,
                        d.message, sev));
                }
                editor.mergeLspDiagnostics(problems);
            });
        });

        client.didOpen(currentUri, currentLanguageId, text);
    }

    /**
     * Notify LSP server that the document has changed.
     */
    public void documentChanged(String text) {
        if (client == null) return;
        docVersion++;
        client.didChange(currentUri, text, docVersion);
    }

    /**
     * Get LSP completions as CompletionProvider.Suggestion list.
     */
    public List<CompletionProvider.Suggestion> getCompletions(int line, int column, String prefix) {
        if (client == null || client.getCapabilities() == null
            || !client.getCapabilities().completionProvider) return java.util.Collections.emptyList();

        try {
            JsonObject result = client.completion(currentUri, line, column).get();
            if (result == null) return java.util.Collections.emptyList();

            List<CompletionProvider.Suggestion> suggestions = new ArrayList<>();
            JsonArray items;
            if (result.has("items")) {
                items = result.getAsJsonArray("items");
            } else if (result.has("isIncomplete")) {
                items = result.getAsJsonArray("items");
            } else {
                return java.util.Collections.emptyList();
            }

            for (int i = 0; i < items.size(); i++) {
                JsonObject item = items.get(i).getAsJsonObject();
                String label = item.get("label").getAsString();
                String insertText = label;
                if (item.has("insertText")) insertText = item.get("insertText").getAsString();
                if (item.has("textEdit")) {
                    JsonObject te = item.getAsJsonObject("textEdit");
                    if (te.has("newText")) insertText = te.get("newText").getAsString();
                }
                String detail = item.has("detail") ? item.get("detail").getAsString() : "";
                if (prefix.isEmpty() || label.toLowerCase().startsWith(prefix.toLowerCase())) {
                    suggestions.add(new CompletionProvider.Suggestion(
                        label, insertText, "lsp"));
                }
            }
            return suggestions;
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Get hover text from LSP.
     */
    public String getHoverText(int line, int column) {
        if (client == null || client.getCapabilities() == null
            || !client.getCapabilities().hoverProvider) return null;
        try {
            JsonObject result = client.hover(currentUri, line, column).get();
            if (result == null) return null;
            if (result.has("contents")) {
                JsonElement contents = result.get("contents");
                StringBuilder sb = new StringBuilder();
                if (contents.isJsonArray()) {
                    for (JsonElement e : contents.getAsJsonArray()) {
                        if (e.isJsonObject()) {
                            JsonObject part = e.getAsJsonObject();
                            if (part.has("language") && part.has("value")) {
                                sb.append("```").append(part.get("language").getAsString())
                                  .append("\n").append(part.get("value").getAsString()).append("\n```\n");
                            } else if (part.has("value")) {
                                sb.append(part.get("value").getAsString()).append("\n");
                            }
                        } else {
                            sb.append(e.getAsString()).append("\n");
                        }
                    }
                } else if (contents.isJsonObject()) {
                    JsonObject part = contents.getAsJsonObject();
                    if (part.has("value")) sb.append(part.get("value").getAsString());
                } else {
                    sb.append(contents.getAsString());
                }
                return sb.toString().trim();
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Go to definition — returns file URI and position.
     */
    public DefinitionResult goToDefinition(int line, int column) {
        if (client == null || client.getCapabilities() == null
            || !client.getCapabilities().definitionProvider) return null;
        try {
            JsonObject result = client.definition(currentUri, line, column).get();
            if (result == null) return null;
            if (result.has("uri")) {
                // Single location
                return locationFromResult(result);
            } else if (result.has("range")) {
                return locationFromResult(result);
            } else if (result.isJsonArray()) {
                JsonArray arr = result.getAsJsonArray();
                if (arr.size() > 0) {
                    return locationFromResult(arr.get(0).getAsJsonObject());
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Find references.
     */
    public List<LocationResult> findReferences(int line, int column) {
        List<LocationResult> results = new ArrayList<>();
        if (client == null || client.getCapabilities() == null
            || !client.getCapabilities().referencesProvider) return results;
        try {
            JsonObject result = client.references(currentUri, line, column, false).get();
            if (result == null) return results;
            if (result.isJsonArray()) {
                for (JsonElement e : result.getAsJsonArray()) {
                    results.add(referenceFromResult(e.getAsJsonObject()));
                }
            }
        } catch (Exception ignored) {}
        return results;
    }

    /**
     * Rename symbol.
     */
    public RenameResult renameSymbol(int line, int column, String newName) {
        if (client == null || client.getCapabilities() == null
            || !client.getCapabilities().renameProvider) return null;
        try {
            JsonObject result = client.rename(currentUri, line, column, newName).get();
            return RenameResult.fromLspResult(result);
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Get document symbols for outline view.
     */
    public List<SymbolResult> getDocumentSymbols() {
        List<SymbolResult> results = new ArrayList<>();
        if (client == null || client.getCapabilities() == null
            || !client.getCapabilities().documentSymbolProvider) return results;
        try {
            JsonObject result = client.documentSymbol(currentUri).get();
            if (result == null) return results;
            if (result.isJsonArray()) {
                JsonArray arr = result.getAsJsonArray();
                for (int i = 0; i < arr.size(); i++) {
                    results.add(symbolFromResult(arr.get(i).getAsJsonObject()));
                }
            }
        } catch (Exception ignored) {}
        return results;
    }

    public boolean isActive() { return client != null; }

    // ---- Helpers ----

    private LocationResult referenceFromResult(JsonObject obj) {
        LocationResult r = new LocationResult();
        if (obj.has("uri")) r.uri = obj.get("uri").getAsString();
        if (obj.has("range")) {
            JsonObject range = obj.getAsJsonObject("range");
            JsonObject start = range.getAsJsonObject("start");
            JsonObject end = range.getAsJsonObject("end");
            r.startLine = start.get("line").getAsInt();
            r.startChar = start.get("character").getAsInt();
            r.endLine = end.get("line").getAsInt();
            r.endChar = end.get("character").getAsInt();
        }
        return r;
    }

    private DefinitionResult locationFromResult(JsonObject obj) {
        DefinitionResult r = new DefinitionResult();
        if (obj.has("uri")) r.uri = obj.get("uri").getAsString();
        if (obj.has("range")) {
            JsonObject range = obj.getAsJsonObject("range");
            JsonObject start = range.getAsJsonObject("start");
            r.startLine = start.get("line").getAsInt();
            r.startChar = start.get("character").getAsInt();
        }
        if (obj.has("targetUri")) { // for LocationLink
            r.uri = obj.get("targetUri").getAsString();
            JsonObject range = obj.getAsJsonObject("targetRange");
            JsonObject start = range.getAsJsonObject("start");
            r.startLine = start.get("line").getAsInt();
            r.startChar = start.get("character").getAsInt();
        }
        return r;
    }

    private SymbolResult symbolFromResult(JsonObject obj) {
        SymbolResult r = new SymbolResult();
        r.name = obj.get("name").getAsString();
        r.kind = obj.has("kind") ? obj.get("kind").getAsInt() : 0;
        if (obj.has("location")) {
            JsonObject loc = obj.getAsJsonObject("location");
            r.uri = loc.get("uri").getAsString();
            JsonObject range = loc.getAsJsonObject("range");
            JsonObject start = range.getAsJsonObject("start");
            r.startLine = start.get("line").getAsInt();
        }
        if (obj.has("children")) {
            JsonArray children = obj.getAsJsonArray("children");
            for (int i = 0; i < children.size(); i++) {
                r.children.add(symbolFromResult(children.get(i).getAsJsonObject()));
            }
        }
        return r;
    }

    private int lineColToOffset(String text, int line, int col) {
        if (text == null) return 0;
        int curLine = 0, offset = 0;
        while (curLine < line && offset < text.length()) {
            int nl = text.indexOf('\n', offset);
            if (nl < 0) break;
            offset = nl + 1;
            curLine++;
        }
        return Math.min(offset + col, text.length());
    }

    private String extensionOf(String name) {
        int idx = name.lastIndexOf('.');
        return idx >= 0 ? name.substring(idx).toLowerCase() : "";
    }

    // ---- Result types ----

    public static class DefinitionResult {
        public String uri;
        public int startLine, startChar;
    }

    public static class LocationResult {
        public String uri;
        public int startLine, startChar, endLine, endChar;
    }

    public static class RenameResult {
        public List<RenameChange> changes = new ArrayList<>();

        static RenameResult fromLspResult(JsonObject result) {
            if (result == null) return null;
            RenameResult rr = new RenameResult();
            if (result.has("changes")) {
                JsonObject changes = result.getAsJsonObject("changes");
                for (java.util.Map.Entry<String, JsonElement> entry : changes.entrySet()) {
                    String uri = entry.getKey();
                    JsonArray edits = entry.getValue().getAsJsonArray();
                    for (int i = 0; i < edits.size(); i++) {
                        JsonObject edit = edits.get(i).getAsJsonObject();
                        RenameChange rc = new RenameChange();
                        rc.uri = uri;
                        JsonObject range = edit.getAsJsonObject("range");
                        rc.startLine = range.getAsJsonObject("start").get("line").getAsInt();
                        rc.startChar = range.getAsJsonObject("start").get("character").getAsInt();
                        rc.endLine = range.getAsJsonObject("end").get("line").getAsInt();
                        rc.endChar = range.getAsJsonObject("end").get("character").getAsInt();
                        rc.newText = edit.get("newText").getAsString();
                        rr.changes.add(rc);
                    }
                }
            }
            if (result.has("documentChanges")) {
                JsonArray docChanges = result.getAsJsonArray("documentChanges");
                for (int i = 0; i < docChanges.size(); i++) {
                    JsonObject dc = docChanges.get(i).getAsJsonObject();
                    // TextDocumentEdit
                    if (dc.has("edits")) {
                        JsonArray edits = dc.getAsJsonArray("edits");
                        for (int j = 0; j < edits.size(); j++) {
                            JsonObject edit = edits.get(j).getAsJsonObject();
                            RenameChange rc = new RenameChange();
                            if (edit.has("textDocument")) {
                                rc.uri = edit.getAsJsonObject("textDocument").get("uri").getAsString();
                            }
                            JsonObject range = edit.getAsJsonObject("range");
                            rc.startLine = range.getAsJsonObject("start").get("line").getAsInt();
                            rc.startChar = range.getAsJsonObject("start").get("character").getAsInt();
                            rc.endLine = range.getAsJsonObject("end").get("line").getAsInt();
                            rc.endChar = range.getAsJsonObject("end").get("character").getAsInt();
                            rc.newText = edit.get("newText").getAsString();
                            rr.changes.add(rc);
                        }
                    }
                }
            }
            return rr;
        }
    }

    public static class RenameChange {
        public String uri;
        public int startLine, startChar, endLine, endChar;
        public String newText;
    }

    public static class SymbolResult {
        public String name;
        public int kind;
        public String uri;
        public int startLine;
        public List<SymbolResult> children = new ArrayList<>();
    }
}
