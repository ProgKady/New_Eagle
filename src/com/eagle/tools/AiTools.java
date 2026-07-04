package com.eagle.tools;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

public class AiTools {

    /** Language for AI responses: "Arabic" or "English". */
    public static String responseLanguage = "English";

    private static String provider, openaiEndpoint, openaiKey, openaiModel;
    private static String ollamaEndpoint, ollamaModel;
    private static String geminiKey, geminiModel;
    private static String localGgufPath = "";
    private static String llamaCliPath = "llama-cli";

    private static void loadConfig() {
        try {
            File f = new File(System.getProperty("user.home") + "/.webide/ai.properties");
            if (!f.exists()) return;
            Properties p = new Properties();
            try (FileInputStream in = new FileInputStream(f)) { p.load(in); }
            provider = p.getProperty("provider", "gemini");
            geminiKey = p.getProperty("gemini.key", "");
            geminiModel = p.getProperty("gemini.model", "gemini-2.0-flash");
            openaiEndpoint = p.getProperty("openai.endpoint", "https://api.groq.com/openai/v1/chat/completions");
            openaiKey = p.getProperty("openai.key", "");
            openaiModel = p.getProperty("openai.model", "llama-3.3-70b-versatile");
            ollamaEndpoint = p.getProperty("ollama.endpoint", "http://localhost:11434/api/chat");
            ollamaModel = p.getProperty("ollama.model", "llama3.2");
            localGgufPath = p.getProperty("localgguf.path", "");
            llamaCliPath = p.getProperty("llama.cli.path", "llama-cli");
        } catch (Exception ignored) {}
    }

    private static String callAI(String systemPrompt, String userText) throws Exception {
        loadConfig();
        String langInstr = responseLanguage != null && responseLanguage.equals("Arabic")
            ? "\n\nIMPORTANT: Respond in Arabic language only."
            : "\n\nIMPORTANT: Respond in English language only.";
        String prompt = systemPrompt + langInstr + "\n\n" + userText;
        if ("localgguf".equals(provider)) return callLocalGguf(prompt);
        else if ("ollama".equals(provider)) return callOllama(prompt);
        else if ("openai".equals(provider)) return callOpenAI(prompt);
        else return callGemini(prompt);
    }

    private static String callOpenAI(String prompt) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("model", openaiModel);
        body.addProperty("stream", false);
        JsonArray msgs = new JsonArray();
        JsonObject m = new JsonObject();
        m.addProperty("role", "user"); m.addProperty("content", prompt);
        msgs.add(m); body.add("messages", msgs);
        String resp = doRequest(openaiEndpoint, body, openaiKey);
        JsonObject json = new Gson().fromJson(resp, JsonObject.class);
        if (json.has("choices")) {
            JsonArray choices = json.getAsJsonArray("choices");
            if (choices.size() > 0) {
                JsonObject choice = choices.get(0).getAsJsonObject();
                if (choice.has("message"))
                    return choice.getAsJsonObject("message").get("content").getAsString();
            }
        }
        if (json.has("error"))
            throw new IOException(json.getAsJsonObject("error").get("message").getAsString());
        return resp;
    }

    private static String callGemini(String prompt) throws Exception {
        String urlStr = "https://generativelanguage.googleapis.com/v1beta/models/"
            + geminiModel + ":generateContent?key=" + geminiKey;
        JsonObject body = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);
        JsonArray parts = new JsonArray(); parts.add(part);
        JsonObject content = new JsonObject();
        content.add("parts", parts); content.addProperty("role", "user");
        contents.add(content); body.add("contents", contents);
        String resp = doRequest(urlStr, body, null);
        JsonObject json = new Gson().fromJson(resp, JsonObject.class);
        if (json.has("candidates")) {
            JsonArray candidates = json.getAsJsonArray("candidates");
            if (candidates.size() > 0) {
                JsonObject candidate = candidates.get(0).getAsJsonObject();
                if (candidate.has("content")) {
                    JsonArray partss = candidate.getAsJsonObject("content").getAsJsonArray("parts");
                    if (partss.size() > 0)
                        return partss.get(0).getAsJsonObject().get("text").getAsString();
                }
                if (candidate.has("finishReason")) {
                    String reason = candidate.get("finishReason").getAsString();
                    if (!reason.equals("STOP")) return "[Blocked: " + reason + "]";
                }
            }
        }
        if (json.has("error"))
            throw new IOException(json.getAsJsonObject("error").get("message").getAsString());
        return resp;
    }

    private static String callOllama(String prompt) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("model", ollamaModel);
        body.addProperty("stream", false);
        body.addProperty("keep_alive", "5m");
        JsonObject options = new JsonObject();
        options.addProperty("num_predict", 4096);
        body.add("options", options);
        JsonArray msgs = new JsonArray();
        JsonObject m = new JsonObject();
        m.addProperty("role", "user"); m.addProperty("content", prompt);
        msgs.add(m); body.add("messages", msgs);
        String resp = doRequest(ollamaEndpoint, body, null);
        JsonObject json = new Gson().fromJson(resp, JsonObject.class);
        if (json.has("message"))
            return json.getAsJsonObject("message").get("content").getAsString();
        if (json.has("response"))
            return json.get("response").getAsString();
        if (json.has("error"))
            throw new IOException(json.get("error").getAsString());
        return resp;
    }

    private static String callLocalGguf(String prompt) throws Exception {
        if (localGgufPath.isEmpty() || !new File(localGgufPath).exists()) {
            throw new IOException("GGUF model file not found: " + localGgufPath);
        }
        File tempPrompt = File.createTempFile("gguf_prompt_", ".txt");
        try (OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(tempPrompt), StandardCharsets.UTF_8)) {
            fw.write(prompt);
        }
        java.util.ArrayList<String> cmd = new java.util.ArrayList<>();
        cmd.add(llamaCliPath);
        cmd.add("-m"); cmd.add(localGgufPath);
        cmd.add("-c"); cmd.add("4096");
        cmd.add("--temp"); cmd.add("0.7");
        cmd.add("-n"); cmd.add("-1");
        cmd.add("-f"); cmd.add(tempPrompt.getAbsolutePath());
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        StringBuilder out = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) out.append(line).append("\n");
        }
        p.waitFor(120, java.util.concurrent.TimeUnit.SECONDS);
        tempPrompt.delete();
        String result = out.toString().trim();
        if (result.isEmpty()) throw new IOException("No output from llama-cli");
        return result;
    }

    private static String doRequest(String urlStr, JsonObject body, String bearerToken) throws Exception {
        byte[] postData = new Gson().toJson(body).getBytes(StandardCharsets.UTF_8);
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        if (bearerToken != null && !bearerToken.isEmpty())
            conn.setRequestProperty("Authorization", "Bearer " + bearerToken);
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(120000);
        try (OutputStream os = conn.getOutputStream()) { os.write(postData); }
        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line; while ((line = br.readLine()) != null) sb.append(line).append("\n");
        }
        return sb.toString().trim();
    }

    private static void showProgressAndCall(String title, String systemPrompt, String userText) {
        loadConfig();
        if ("gemini".equals(provider) && geminiKey.isEmpty()) {
            DialogUtil.showError("No API Key", "Set your Gemini API key in Settings \u2192 AI tab");
            return;
        }
        if ("openai".equals(provider) && openaiKey.isEmpty()) {
            DialogUtil.showError("No API Key", "Set your OpenAI API key in Settings \u2192 AI tab");
            return;
        }

        Dialog<Void> dlg = DialogUtil.progressDialog(title, "Thinking...");
        dlg.setResizable(true);
        dlg.getDialogPane().setPrefSize(500, 300);

        Label statusLabel = new Label("Initializing...");
        statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #888;");
        ProgressIndicator pi = new ProgressIndicator();
        VBox progBox = new VBox(12, pi, statusLabel);
        progBox.setStyle("-fx-alignment: center; -fx-padding: 20;");
        dlg.getDialogPane().setContent(progBox);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dlg.show();

        new Thread(() -> {
            try {
                Platform.runLater(() -> statusLabel.setText("Connecting to AI provider..."));
                String result = callAI(systemPrompt, userText);
                Platform.runLater(() -> {
                    dlg.close();
                    showAiResult(title, result);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    dlg.close();
                    DialogUtil.showError("AI Error", e.getMessage());
                });
            }
        }).start();
    }

    private static void showAiResult(String title, String text) {
        Dialog<String> dlg = new Dialog<>();
        dlg.setTitle(title);
        dlg.setHeaderText(null);
        dlg.setResizable(true);
        dlg.getDialogPane().setPrefSize(900, 650);
        if (DialogUtil.getOwnerWindow() != null) dlg.initOwner(DialogUtil.getOwnerWindow());
        DialogUtil.applyTheme(dlg);
        TextArea area = new TextArea(text);
        area.setEditable(false);
        area.setWrapText(true);
        area.setStyle("-fx-font-family: 'Segoe UI', Tahoma, sans-serif; -fx-font-size: 13px; -fx-line-spacing: 4;");
        Button copyBtn = new Button("Copy to Clipboard");
        copyBtn.setOnAction(e -> {
            javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
            cc.putString(text);
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
        });
        VBox vb = new VBox(8, area, copyBtn);
        VBox.setVgrow(area, Priority.ALWAYS);
        dlg.getDialogPane().setContent(vb);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dlg.showAndWait();
    }

    public static void explainCode(String code, String language) {
        String sys = "You are an expert programmer. Explain the following " + language
            + " code in detail, describing what each part does, its purpose, and any notable patterns.";
        showProgressAndCall("AI: Explain Code", sys, code);
    }

    public static void fixCode(String code, String language) {
        String sys = "You are an expert debugger. Review the following " + language
            + " code, identify bugs, issues, or improvements, and provide the corrected version. Explain each fix.";
        showProgressAndCall("AI: Fix Code", sys, code);
    }

    public static void generateCode(String description, String language) {
        String sys = "You are an expert " + language + " developer. Generate clean, well-structured "
            + language + " code based on the following description. Include comments explaining the logic.";
        showProgressAndCall("AI: Generate Code", sys, description);
    }

    public static void refactorCode(String code, String language) {
        String sys = "You are an expert in code refactoring. Review the following " + language
            + " code and suggest improvements for readability, performance, and maintainability. "
            + "Provide the refactored version with explanations.";
        showProgressAndCall("AI: Refactor Code", sys, code);
    }

    public static void generateUnitTests(String code, String language) {
        String sys = "You are an expert in testing. Generate comprehensive unit tests for the following "
            + language + " code. Include edge cases, use a standard testing framework for this language.";
        showProgressAndCall("AI: Generate Tests", sys, code);
    }

    public static void generateDocumentation(String code, String language) {
        String sys = "You are a technical writer. Generate comprehensive documentation for the following "
            + language + " code. Include description, parameters, return values, examples, and notes.";
        showProgressAndCall("AI: Generate Docs", sys, code);
    }

    public static void explainError(String errorText, String code) {
        String sys = "You are an expert debugger. Explain the following error in detail, what caused it, "
            + "and how to fix it. Consider the code context provided.";
        showProgressAndCall("AI: Explain Error", sys, "Error:\n" + errorText + "\n\nCode:\n" + code);
    }

    public static void aiCodeReview(String code, String language) {
        String sys = "You are an expert code reviewer. Perform a thorough code review of the following "
            + language + " code. Check for: bugs, security issues, performance problems, code style violations, "
            + "and architectural concerns. Provide actionable recommendations.";
        showProgressAndCall("AI: Code Review", sys, code);
    }

    public static void aiCommitMessage(String diffText) {
        String sys = "You are a git expert. Generate a concise, descriptive git commit message for the "
            + "following diff. Follow conventional commit format (type: description).";
        showProgressAndCall("AI: Commit Message", sys, diffText);
    }

    public static void aiRenameSymbol(String code, String oldName, String newName, String language) {
        String sys = "You are an expert " + language + " developer. Rename the symbol '" + oldName
            + "' to '" + newName + "' in the following code. Consider all usages, ensure correctness, "
            + "and return the complete updated code.";
        showProgressAndCall("AI: Rename Symbol", sys, code);
    }
}
