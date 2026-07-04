package com.eagle.lsp;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Manages LSP server instances per language/project.
 * Detects available language servers and starts them on demand.
 */
public class LspLanguageServer {

    private static LspLanguageServer instance;

    private final Map<String, LspClient> activeClients = new HashMap<>();
    private final Map<String, ServerConfig> configs = new HashMap<>();
    private String workspaceRoot = "";

    public static LspLanguageServer getInstance() {
        if (instance == null) instance = new LspLanguageServer();
        return instance;
    }

    public static class ServerConfig {
        public String[] command;
        public String languageId;
        public String[] extensions;

        public ServerConfig(String[] command, String languageId, String[] extensions) {
            this.command = command;
            this.languageId = languageId;
            this.extensions = extensions;
        }
    }

    private LspLanguageServer() {
        // Auto-detect available language servers
        detectServers();
    }

    public void setWorkspaceRoot(String path) {
        this.workspaceRoot = path != null ? new File(path).toURI().toString() : "";
    }

    private void detectServers() {
        // TypeScript / JavaScript
        if (isToolAvailable("typescript-language-server")) {
            configs.put("javascript", new ServerConfig(
                new String[]{"typescript-language-server", "--stdio"},
                "javascript", new String[]{".js", ".jsx", ".mjs", ".cjs"}));
            configs.put("typescript", new ServerConfig(
                new String[]{"typescript-language-server", "--stdio"},
                "typescript", new String[]{".ts", ".tsx"}));
        }

        // Python
        if (isToolAvailable("pyright-langserver") || isToolAvailable("pyright")) {
            String cmd = isToolAvailable("pyright-langserver") ? "pyright-langserver" : "pyright";
            configs.put("python", new ServerConfig(
                new String[]{cmd, "--stdio"},
                "python", new String[]{".py"}));
        } else if (isToolAvailable("pylsp")) {
            configs.put("python", new ServerConfig(
                new String[]{"pylsp"},
                "python", new String[]{".py"}));
        }

        // Java
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null) {
            File jdtls = new File(javaHome + "/bin/jdtls");
            if (jdtls.exists()) {
                configs.put("java", new ServerConfig(
                    new String[]{jdtls.getAbsolutePath()},
                    "java", new String[]{".java"}));
            }
        }

        // HTML
        if (isToolAvailable("vscode-html-language-server") || isToolAvailable("html-languageserver")) {
            String cmd = isToolAvailable("vscode-html-language-server")
                ? "vscode-html-language-server" : "html-languageserver";
            configs.put("html", new ServerConfig(
                new String[]{cmd, "--stdio"},
                "html", new String[]{".html", ".htm"}));
        }

        // CSS
        if (isToolAvailable("vscode-css-language-server") || isToolAvailable("css-languageserver")) {
            String cmd = isToolAvailable("vscode-css-language-server")
                ? "vscode-css-language-server" : "css-languageserver";
            configs.put("css", new ServerConfig(
                new String[]{cmd, "--stdio"},
                "css", new String[]{".css", ".scss", ".less", ".sass"}));
        }

        // JSON
        if (isToolAvailable("vscode-json-language-server") || isToolAvailable("json-languageserver")) {
            String cmd = isToolAvailable("vscode-json-language-server")
                ? "vscode-json-language-server" : "json-languageserver";
            configs.put("json", new ServerConfig(
                new String[]{cmd, "--stdio"},
                "json", new String[]{".json"}));
        }

        // Go
        if (isToolAvailable("gopls")) {
            configs.put("go", new ServerConfig(
                new String[]{"gopls"},
                "go", new String[]{".go"}));
        }

        // Rust
        if (isToolAvailable("rust-analyzer")) {
            configs.put("rust", new ServerConfig(
                new String[]{"rust-analyzer"},
                "rust", new String[]{".rs"}));
        }

        // PHP
        if (isToolAvailable("php") && isToolAvailable("php-language-server")) {
            configs.put("php", new ServerConfig(
                new String[]{"php-language-server"},
                "php", new String[]{".php"}));
        }

        // SQL
        if (isToolAvailable("sql-language-server")) {
            configs.put("sql", new ServerConfig(
                new String[]{"sql-language-server"},
                "sql", new String[]{".sql"}));
        }

        // YAML
        if (isToolAvailable("yaml-language-server")) {
            configs.put("yaml", new ServerConfig(
                new String[]{"yaml-language-server", "--stdio"},
                "yaml", new String[]{".yaml", ".yml"}));
        }

        // Dockerfile
        if (isToolAvailable("docker-langserver")) {
            configs.put("dockerfile", new ServerConfig(
                new String[]{"docker-langserver"},
                "dockerfile", new String[]{"Dockerfile"}));
        }

        // Kotlin
        if (isToolAvailable("kotlin-language-server")) {
            configs.put("kotlin", new ServerConfig(
                new String[]{"kotlin-language-server"},
                "kotlin", new String[]{".kt", ".kts"}));
        }

        // C/C++
        if (isToolAvailable("clangd")) {
            configs.put("c", new ServerConfig(
                new String[]{"clangd"},
                "c", new String[]{".c", ".h"}));
            configs.put("cpp", new ServerConfig(
                new String[]{"clangd"},
                "cpp", new String[]{".cpp", ".hpp", ".cc", ".cxx"}));
        }
    }

    /**
     * Get the language ID for a file extension.
     */
    public String getLanguageId(String ext) {
        switch (ext.toLowerCase()) {
            case ".js": case ".jsx": case ".mjs": case ".cjs": return "javascript";
            case ".ts": case ".tsx": return "typescript";
            case ".py": return "python";
            case ".java": return "java";
            case ".html": case ".htm": return "html";
            case ".css": case ".scss": case ".less": case ".sass": return "css";
            case ".json": return "json";
            case ".go": return "go";
            case ".rs": return "rust";
            case ".php": return "php";
            case ".sql": return "sql";
            case ".yaml": case ".yml": return "yaml";
            case ".kt": case ".kts": return "kotlin";
            case ".c": case ".h": return "c";
            case ".cpp": case ".hpp": case ".cc": case ".cxx": return "cpp";
            case ".xml": return "xml";
            case ".md": return "markdown";
            default: return "";
        }
    }

    /**
     * Start or get the LSP client for a given language.
     */
    public LspClient getClient(String languageId) {
        if (languageId == null || languageId.isEmpty()) return null;
        LspClient existing = activeClients.get(languageId);
        if (existing != null) return existing;

        ServerConfig config = configs.get(languageId);
        if (config == null) return null;

        LspClient client = new LspClient();
        try {
            Future<Boolean> init = client.start(config.command, workspaceRoot);
            init.get(10, TimeUnit.SECONDS);
            activeClients.put(languageId, client);
            return client;
        } catch (Exception e) {
            System.err.println("Failed to start " + languageId + " LSP server: " + e.getMessage());
            client.shutdown();
            return null;
        }
    }

    /**
     * Check if an extension has a configured language server.
     */
    public boolean hasServerFor(String ext) {
        String langId = getLanguageId(ext);
        return !langId.isEmpty() && configs.containsKey(langId);
    }

    /**
     * Shut down all running servers.
     */
    public void shutdownAll() {
        for (LspClient client : activeClients.values()) {
            client.shutdown();
        }
        activeClients.clear();
    }

    private boolean isToolAvailable(String tool) {
        try {
            ProcessBuilder pb;
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                pb = new ProcessBuilder("where", tool);
            } else {
                pb = new ProcessBuilder("which", tool);
            }
            Process p = pb.start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
