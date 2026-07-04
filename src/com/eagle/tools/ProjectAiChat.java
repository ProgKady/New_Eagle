package com.eagle.tools;

import com.eagle.editor.AiPanel;
import com.eagle.util.ThemeManager;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;

public class ProjectAiChat {

    public static void show(File projectRoot, AiPanel aiPanel, Window owner) {
        if (projectRoot == null) return;
        if (aiPanel == null) return;

        // Build project context with tech stack
        final StringBuilder ctx = new StringBuilder();
        ctx.append("Project: ").append(projectRoot.getName()).append("\n");
        ctx.append("Path: ").append(projectRoot.getAbsolutePath()).append("\n");

        // Tech stack detection
        String techStack = detectTechStack(projectRoot);
        ctx.append("Tech Stack: ").append(techStack).append("\n");

        // File summary
        int[] counts = countFiles(projectRoot);
        ctx.append("Files: ").append(counts[0]).append(" | Dirs: ").append(counts[1]).append("\n");

        ctx.append("\n--- Project Structure ---\n");
        collectFilesForContext(projectRoot, ctx, "");

        TextArea contextArea = new TextArea(ctx.toString());
        contextArea.setEditable(true);
        contextArea.setPrefRowCount(10);
        contextArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 10px;");

        // Status label below context
        Label statusLabel = new Label("Ready. Ask a question about the project above.");
        statusLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-muted;");

        TextField questionField = new TextField();
        questionField.setPromptText("Ask about the project... (e.g., 'Explain the architecture', 'Find bugs', 'Add feature')");

        CheckBox savePromptCheck = new CheckBox("Save this prompt for later");
        savePromptCheck.setSelected(false);
        savePromptCheck.setStyle("-fx-font-size: 10px;");

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("AI Chat With Project");
        dialog.setHeaderText("Ask AI about: " + projectRoot.getName());
        dialog.initOwner(owner);

        VBox root = new VBox(8,
            new Label("Project Context (editable):"),
            contextArea,
            statusLabel,
            new Label("Your Question:"),
            questionField,
            savePromptCheck);
        root.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().getButtonTypes().addAll(
            new ButtonType("Send to AI Chat", ButtonBar.ButtonData.OK_DONE), ButtonType.CANCEL);
        dialog.setResultConverter(btn -> btn.getButtonData() == ButtonBar.ButtonData.OK_DONE ? questionField.getText() : null);
        dialog.getDialogPane().sceneProperty().addListener((obs, oldS, newS) -> {
            if (newS != null) ThemeManager.getInstance().applyTheme(newS);
        });

        questionField.textProperty().addListener((obs, old, val) -> {
            statusLabel.setText(val != null && !val.trim().isEmpty()
                ? "Question ready (" + val.length() + " chars)" : "Ready.");
        });

        dialog.showAndWait().ifPresent(question -> {
            if (question == null || question.trim().isEmpty()) return;
            String q = question.trim();

            String prompt = "You are an expert developer analyzing a project.\n\n"
                + "## Project Context\n" + ctx.toString() + "\n\n"
                + "## User Question\n" + q + "\n\n"
                + "Please provide a detailed, helpful response based on the project files. "
                + "Reference specific files and line numbers where relevant.";

            aiPanel.sendPrompt(prompt);

            if (savePromptCheck.isSelected()) {
                savePrompt(projectRoot, q, techStack);
            }
        });
    }

    private static String detectTechStack(File projectRoot) {
        StringBuilder sb = new StringBuilder();
        File[] files = projectRoot.listFiles();
        if (files == null) return "Unknown";
        for (File f : files) {
            String name = f.getName();
            if (name.equals("package.json")) { sb.append("Node.js/JavaScript "); }
            if (name.equals("requirements.txt") || name.equals("setup.py") || name.equals("Pipfile")) { sb.append("Python "); }
            if (name.equals("pom.xml")) { sb.append("Java/Maven "); }
            if (name.equals("build.gradle") || name.equals("build.gradle.kts")) { sb.append("Java/Gradle "); }
            if (name.equals("Cargo.toml")) { sb.append("Rust "); }
            if (name.equals("go.mod")) { sb.append("Go "); }
            if (name.equals("composer.json")) { sb.append("PHP "); }
            if (name.equals("Gemfile")) { sb.append("Ruby "); }
            if (name.endsWith(".csproj") || name.equals("*.sln")) { sb.append("C#/.NET "); }
            if (name.equals("Dockerfile") || name.equals("docker-compose.yml")) { sb.append("Docker "); }
            // Framework detection
            File[] allFiles = projectRoot.listFiles();
            if (allFiles != null) {
                for (File sf : allFiles) {
                    if (sf.isDirectory()) {
                        String dn = sf.getName();
                        if (dn.equals("src")) {
                            File[] srcFiles = sf.listFiles();
                            if (srcFiles != null) {
                                for (File ssf : srcFiles) {
                                    if (ssf.getName().equals("App.vue") || ssf.getName().endsWith(".vue")) { sb.append("Vue.js "); }
                                    if (ssf.getName().equals("App.jsx") || ssf.getName().equals("App.tsx")) { sb.append("React "); }
                                }
                            }
                        }
                    }
                }
            }
        }
        String result = sb.toString().trim();
        return result.isEmpty() ? "Unknown" : result;
    }

    private static int[] countFiles(File dir) {
        int files = 0;
        int dirs = 0;
        File[] list = dir.listFiles();
        if (list == null) return new int[]{0, 0};
        for (File f : list) {
            String n = f.getName();
            if (n.startsWith(".") || n.equals("node_modules") || n.equals("build")
                || n.equals("dist") || n.equals("__pycache__") || n.equals(".git")
                || n.equals("target")) continue;
            if (f.isDirectory()) {
                dirs++;
                int[] sub = countFiles(f);
                files += sub[0];
                dirs += sub[1];
            } else {
                files++;
            }
        }
        return new int[]{files, dirs};
    }

    private static void collectFilesForContext(File dir, StringBuilder sb, String indent) {
        File[] files = dir.listFiles();
        if (files == null) return;
        java.util.Arrays.sort(files, (a, b) -> {
            if (a.isDirectory() && !b.isDirectory()) return -1;
            if (!a.isDirectory() && b.isDirectory()) return 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });
        for (File f : files) {
            if (f.isDirectory()) {
                String n = f.getName();
                if (!n.startsWith(".") && !n.equals("node_modules") && !n.equals("build")
                    && !n.equals("dist") && !n.equals("__pycache__") && !n.equals(".git")
                    && !n.equals("target")) {
                    sb.append(indent).append("📁 ").append(n).append("/\n");
                    collectFilesForContext(f, sb, indent + "  ");
                }
            } else {
                String name = f.getName();
                sb.append(indent).append("📄 ").append(name);
                sb.append(" (").append(formatSize(f.length())).append(")");
                // Include content for small text files
                if (f.length() < 10000) {
                    try {
                        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";
                        if (ext.matches("html|css|js|java|py|json|xml|txt|md|properties|yml|yaml|ts|jsx|tsx|vue|php|rb|sql|c|cpp|h|sh|bat|kt|go|rs")) {
                            String content = new String(java.nio.file.Files.readAllBytes(f.toPath()),
                                java.nio.charset.StandardCharsets.UTF_8);
                            sb.append("\n").append(indent).append("  ```").append(ext).append("\n")
                              .append(indent).append("  ").append(content.replace("\n", "\n" + indent + "  "))
                              .append("\n").append(indent).append("  ```");
                        }
                    } catch (Exception ignored) {}
                }
                sb.append("\n");
            }
        }
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + "KB";
        return (bytes / (1024 * 1024)) + "MB";
    }

    private static void savePrompt(File projectRoot, String question, String techStack) {
        try {
            File dir = new File(System.getProperty("user.home") + "/.webide");
            dir.mkdirs();
            File promptsFile = new File(dir, "saved_prompts.json");
            String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            String entry = "{\n"
                + "  \"source\": \"chat_about_project\",\n"
                + "  \"timestamp\": \"" + ts + "\",\n"
                + "  \"project\": \"" + escJson(projectRoot.getName()) + "\",\n"
                + "  \"techStack\": \"" + escJson(techStack) + "\",\n"
                + "  \"prompt\": \"" + escJson(question) + "\"\n}";

            String existing = "";
            if (promptsFile.exists()) {
                existing = new String(java.nio.file.Files.readAllBytes(promptsFile.toPath()),
                    java.nio.charset.StandardCharsets.UTF_8).trim();
            }
            String output;
            if (existing.isEmpty() || existing.equals("[]")) {
                output = "[\n" + entry + "\n]";
            } else if (existing.startsWith("[")) {
                output = existing.substring(0, 1) + "\n" + entry + ",\n" + existing.substring(1);
            } else {
                output = "[\n" + entry + "\n]";
            }
            java.nio.file.Files.write(promptsFile.toPath(), output.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception ignored) {}
    }

    private static String escJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
