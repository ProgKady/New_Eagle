package com.eagle.generator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ResponseParser {

    private static final Pattern CODE_BLOCK = Pattern.compile(
        "```(\\w*)\\s*\n?(.*?)```",
        Pattern.DOTALL
    );

    private static final Pattern FILE_PATH = Pattern.compile(
        "(?:^|\\n)\\s*(?://|#|--|\\/\\*)?\\s*(?:File\\s*[:.]?\\s*|Path\\s*[:.]?\\s*|file\\s*[:.]?\\s*)(.+?)\\s*[:\\n]",
        Pattern.MULTILINE
    );

    private static final String[] COMMENT_PREFIXES = { "//", "#", "--", "<!--", "/*", "*" };

    private ResponseParser() {}

    public static Map<String, String> extractFiles(String response) {
        Map<String, String> files = new LinkedHashMap<>();
        Matcher matcher = CODE_BLOCK.matcher(response);
        while (matcher.find()) {
            String lang = matcher.group(1).trim().toLowerCase();
            String content = matcher.group(2);
            String path = extractPathFromBlock(content, lang);
            if (path != null) {
                files.put(path, cleanContent(content));
            }
        }
        return files;
    }

    public static List<String> extractFilePaths(String response) {
        List<String> paths = new ArrayList<>();
        String normalized = response.replace("\r\n", "\n");

        Matcher fm = FILE_PATH.matcher(normalized);
        while (fm.find()) {
            String path = fm.group(1).trim();
            path = stripTrailingPunctuation(path);
            if (isValidPath(path) && !paths.contains(path)) {
                paths.add(path);
            }
        }

        Matcher cm = CODE_BLOCK.matcher(normalized);
        while (cm.find()) {
            String content = cm.group(2);
            String firstLine = content.split("\\n", 2)[0].trim();
            if (firstLine.startsWith("File:") || firstLine.toLowerCase().startsWith("file:")) {
                String path = firstLine.substring(5).trim();
                path = stripTrailingPunctuation(path);
                if (isValidPath(path) && !paths.contains(path)) {
                    paths.add(path);
                }
            }
        }

        return paths;
    }

    public static List<Map<String, String>> parseJsonFileList(String response) {
        List<Map<String, String>> result = new ArrayList<>();
        String json = extractJsonString(response);
        try {
            com.google.gson.JsonElement el = com.google.gson.JsonParser.parseString(json);
            com.google.gson.JsonArray arr;
            if (el.isJsonArray()) {
                arr = el.getAsJsonArray();
            } else {
                return result;
            }
            for (int i = 0; i < arr.size(); i++) {
                com.google.gson.JsonObject obj = arr.get(i).getAsJsonObject();
                Map<String, String> map = new LinkedHashMap<>();
                if (obj.has("path")) {
                    map.put("path", obj.get("path").getAsString());
                }
                if (obj.has("description")) {
                    map.put("description", obj.get("description").getAsString());
                }
                if (obj.has("type")) {
                    map.put("type", obj.get("type").getAsString());
                }
                if (obj.has("language")) {
                    map.put("language", obj.get("language").getAsString());
                }
                if (!map.isEmpty()) {
                    result.add(map);
                }
            }
        } catch (Exception e) {
            // Fallback: try regex
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "\"path\"\\s*:\\s*\"([^\"]+)\"").matcher(response);
            while (m.find()) {
                Map<String, String> map = new LinkedHashMap<>();
                map.put("path", m.group(1));
                result.add(map);
            }
        }
        return result;
    }

    public static String extractJsonString(String response) {
        if (response == null || response.isEmpty()) return "[]";
        int start = response.indexOf('[');
        if (start < 0) {
            start = response.indexOf('{');
        }
        if (start < 0) return response;
        int end = -1;
        int depth = 0;
        boolean inString = false;
        for (int i = start; i < response.length(); i++) {
            char c = response.charAt(i);
            if (c == '"' && (i == 0 || response.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            if (!inString) {
                if (c == '[' || c == '{') depth++;
                if (c == ']' || c == '}') {
                    depth--;
                    if (depth == 0) {
                        end = i + 1;
                        break;
                    }
                }
            }
        }
        if (end > start) {
            return response.substring(start, end);
        }
        return response;
    }

    private static String extractPathFromBlock(String content, String lang) {
        String[] lines = content.split("\\n");
        String first = lines[0].trim();

        if (first.startsWith("File:") || first.toLowerCase().startsWith("file:")) {
            return stripTrailingPunctuation(first.substring(5).trim());
        }
        if (first.startsWith("//") || first.startsWith("#") || first.startsWith("--")) {
            for (String prefix : COMMENT_PREFIXES) {
                if (first.startsWith(prefix)) {
                    String after = first.substring(prefix.length()).trim();
                    if (after.startsWith("File:") || after.toLowerCase().startsWith("file:")) {
                        return stripTrailingPunctuation(after.substring(5).trim());
                    }
                    if (isValidPath(after)) {
                        return stripTrailingPunctuation(after);
                    }
                }
            }
        }
        if (isValidPath(first) && first.contains(".")) {
            return stripTrailingPunctuation(first);
        }
        if (lang != null && !lang.isEmpty() && lang.contains("/")) {
            return lang;
        }
        return null;
    }

    private static String cleanContent(String content) {
        String[] lines = content.split("\\n", 2);
        String first = lines[0].trim();
        if (first.startsWith("File:") || first.toLowerCase().startsWith("file:")
            || first.startsWith("//") || first.startsWith("#") || first.startsWith("--")) {
            return (lines.length > 1) ? lines[1] : "";
        }
        return content;
    }

    private static boolean isValidPath(String s) {
        if (s == null || s.isEmpty()) return false;
        return s.contains("/") || s.contains("\\") || s.contains(".");
    }

    private static String stripTrailingPunctuation(String s) {
        if (s == null || s.isEmpty()) return s;
        while (!s.isEmpty() && ".,;:!?)]}>-".indexOf(s.charAt(s.length() - 1)) >= 0) {
            s = s.substring(0, s.length() - 1).trim();
        }
        return s.trim();
    }

    public static String normalizePath(String path) {
        return path.replace('\\', '/').trim();
    }

    public static String extractLanguage(String path) {
        if (path == null || path.isEmpty()) return "";
        int dot = path.lastIndexOf('.');
        if (dot < 0) return "";
        String ext = path.substring(dot + 1).toLowerCase();
        switch (ext) {
            case "java": return "java";
            case "js": return "javascript";
            case "jsx": return "jsx";
            case "ts": return "typescript";
            case "tsx": return "tsx";
            case "py": return "python";
            case "html": return "html";
            case "css": return "css";
            case "scss": case "sass": return "scss";
            case "json": return "json";
            case "xml": return "xml";
            case "yaml": case "yml": return "yaml";
            case "md": return "markdown";
            case "sql": return "sql";
            case "sh": case "bash": return "bash";
            case "bat": case "cmd": return "bat";
            case "kt": case "kts": return "kotlin";
            case "go": return "go";
            case "rs": return "rust";
            case "php": return "php";
            case "rb": return "ruby";
            case "c": return "c";
            case "cpp": case "cc": case "cxx": return "cpp";
            case "cs": return "csharp";
            case "swift": return "swift";
            case "dart": return "dart";
            case "vue": return "vue";
            case "svelte": return "svelte";
            case "tf": return "terraform";
            case "dockerfile": case "Dockerfile": return "dockerfile";
            case "gradle": return "gradle";
            case "toml": return "toml";
            default: return ext;
        }
    }
}
