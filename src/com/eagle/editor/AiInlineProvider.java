package com.eagle.editor;

import com.eagle.generator.AiProvider;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class AiInlineProvider {

    private final AiProvider provider;
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private final int contextLines = 30;

    public AiInlineProvider(AiProvider provider) {
        this.provider = provider;
    }

    public boolean isAvailable() {
        return provider != null;
    }

    public String getProviderName() {
        return provider != null ? provider.getProviderName() : "none";
    }

    /**
     * Fetch an inline completion suggestion asynchronously.
     * Returns null if busy or no suggestion.
     */
    public CompletableFuture<String> getInlineCompletion(
            String codeBefore, String codeAfter, String language, int cursorLine) {
        if (!busy.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                String prompt = buildPrompt(codeBefore, codeAfter, language, cursorLine);
                String response = provider.call(
                    "You are an AI code completion engine. "
                    + "Output ONLY the code that should appear next at the cursor. "
                    + "No explanation. No markdown. No backticks. "
                    + "Output what the developer would type next — a short single-line or multi-line completion.",
                    prompt);
                if (response == null) return null;
                response = response.trim();
                if (response.isEmpty()) return null;
                // Strip markdown code fences if present
                if (response.contains("```")) {
                    response = response.replaceAll("(?s)```[a-zA-Z]*\\n?", "").trim();
                }
                // Limit to reasonable length
                if (response.length() > 500) {
                    response = response.substring(0, 500);
                }
                // Remove leading whitespace that would duplicate existing indentation
                return response;
            } catch (Exception e) {
                return null;
            } finally {
                busy.set(false);
            }
        });
    }

    private String buildPrompt(String codeBefore, String codeAfter, String language, int cursorLine) {
        StringBuilder sb = new StringBuilder();
        sb.append("Continue the following ").append(language).append(" code at the cursor (marked by <CURSOR>):\n\n");

        // Take context before cursor (last contextLines lines)
        String[] lines = codeBefore.split("\n", -1);
        int startLine = Math.max(0, lines.length - contextLines);
        for (int i = startLine; i < lines.length; i++) {
            sb.append(lines[i]).append("\n");
        }
        sb.append("<CURSOR>\n");

        // Take a few lines after cursor
        String[] afterLines = codeAfter.split("\n", -1);
        int afterCount = Math.min(5, afterLines.length);
        for (int i = 0; i < afterCount; i++) {
            sb.append(afterLines[i]).append("\n");
        }

        sb.append("\n---\nComplete at <CURSOR>. Output ONLY the completion code, NOTHING else.");
        return sb.toString();
    }
}
