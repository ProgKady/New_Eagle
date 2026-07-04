package com.eagle.generator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProjectAuditor {

    private final AiProvider ai;
    private final ProgressMonitor monitor;

    private static final Pattern IMPORT_PATTERN = Pattern.compile(
        "(?:import|require|from)\\s+['\"]([^'\"]+)['\"]"
    );

    private static final Pattern LOCAL_IMPORT = Pattern.compile(
        "['\"](\\.[^'\"]+)['\"]"
    );

    public ProjectAuditor(AiProvider ai, ProgressMonitor monitor) {
        this.ai = ai;
        this.monitor = monitor;
    }

    public List<ProjectIssue> audit(ProjectContext context) {
        List<ProjectIssue> issues = new ArrayList<>();
        monitor.onPhase("Reviewing", 75);
        monitor.onLog("[ProjectAuditor] Auditing generated project...");

        for (String path : context.getGeneratedFiles()) {
            String content = context.getContent(path);
            if (content == null) {
                File f = new File(context.getProjectDir(), path);
                if (f.exists()) {
                    try {
                        byte[] bytes = Files.readAllBytes(f.toPath());
                        content = new String(bytes, "UTF-8");
                        context.storeContent(path, content);
                    } catch (IOException e) {
                        continue;
                    }
                } else {
                    continue;
                }
            }

            checkMissingImports(context, path, content, issues);
            checkBrokenReferences(context, path, content, issues);
        }

        monitor.onLog("[ProjectAuditor] Found " + issues.size() + " issues");
        monitor.onPhase("Reviewing", 80);

        return issues;
    }

    public List<ProjectIssue> auditDirectory(ProjectContext context) {
        List<ProjectIssue> issues = new ArrayList<>();
        File dir = context.getProjectDir();

        if (!dir.exists()) return issues;

        auditDirectoryRecursive(dir, dir, context, issues);

        return issues;
    }

    private void auditDirectoryRecursive(File base, File dir, ProjectContext context, List<ProjectIssue> issues) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.isDirectory()) {
                if (!f.getName().startsWith(".") && !f.getName().equals("node_modules")
                    && !f.getName().equals("target") && !f.getName().equals("build")
                    && !f.getName().equals("dist") && !f.getName().equals(".git")) {
                    auditDirectoryRecursive(base, f, context, issues);
                }
            } else {
                String relPath = base.toURI().relativize(f.toURI()).getPath();
                if (!context.isGenerated(relPath)) {
                    try {
                        byte[] bytes = Files.readAllBytes(f.toPath());
                        String content = new String(bytes, "UTF-8");
                        context.storeContent(relPath, content);
                        checkMissingImports(context, relPath, content, issues);
                    } catch (IOException e) {
                        // skip
                    }
                }
            }
        }
    }

    private void checkMissingImports(ProjectContext context, String path, String content, List<ProjectIssue> issues) {
        Set<String> projectFiles = new HashSet<>();
        for (String p : context.getGeneratedFiles()) {
            String name = filenameWithoutExt(p);
            projectFiles.add(name);
            projectFiles.add(name.toLowerCase());
        }

        Matcher matcher = IMPORT_PATTERN.matcher(content);
        while (matcher.find()) {
            String imp = matcher.group(1);
            if (imp.startsWith(".")) {
                String resolved = resolveRelativeImport(path, imp);
                if (resolved != null) {
                    if (!context.isGenerated(resolved)) {
                        File f = new File(context.getProjectDir(), resolved);
                        if (!f.exists()) {
                            issues.add(new ProjectIssue(
                                path, "Missing import", imp + " -> " + resolved
                            ));
                        }
                    }
                }
            }
        }
    }

    private void checkBrokenReferences(ProjectContext context, String path, String content, List<ProjectIssue> issues) {
        if (path.endsWith(".java")) {
            Pattern classRef = Pattern.compile("\\b([A-Z][a-zA-Z0-9]+)\\b");
            Set<String> keywords = new HashSet<>();
            keywords.add("String"); keywords.add("Integer"); keywords.add("Boolean");
            keywords.add("Long"); keywords.add("Double"); keywords.add("Float");
            keywords.add("List"); keywords.add("Map"); keywords.add("Set");
            keywords.add("Object"); keywords.add("Class"); keywords.add("Thread");
            keywords.add("Runnable"); keywords.add("Exception"); keywords.add("RuntimeException");
            keywords.add("System"); keywords.add("Math"); keywords.add("Arrays");
            keywords.add("Collections"); keywords.add("Optional"); keywords.add("Stream");
            keywords.add("Void"); keywords.add("Number"); keywords.add("Character");
            keywords.add("Byte"); keywords.add("Short"); keywords.add("File");
            keywords.add("Path"); keywords.add("Paths"); keywords.add("Files");
            keywords.add("InputStream"); keywords.add("OutputStream"); keywords.add("Reader");
            keywords.add("Writer"); keywords.add("BufferedReader"); keywords.add("IOException");
            keywords.add("ArrayList"); keywords.add("HashMap"); keywords.add("HashSet");
            keywords.add("LinkedList"); keywords.add("TreeMap"); keywords.add("TreeSet");
            keywords.add("StringBuilder"); keywords.add("StringBuffer");
            keywords.add("Iterator"); keywords.add("Iterable"); keywords.add("Comparable");
            keywords.add("Comparator"); keywords.add("Serializable");
            keywords.add("Override"); keywords.add("Deprecated"); keywords.add("SuppressWarnings");
            keywords.add("FunctionalInterface");
        }
    }

    public void fixIssues(ProjectContext context, List<ProjectIssue> issues) throws Exception {
        if (issues.isEmpty()) return;

        monitor.onLog("[ProjectAuditor] Fixing " + issues.size() + " issues...");

        List<ProjectIssue> importIssues = new ArrayList<>();
        for (ProjectIssue issue : issues) {
            if (issue.getType().equals("Missing import")) {
                importIssues.add(issue);
            }
        }

        if (!importIssues.isEmpty()) {
            fixMissingImports(context, importIssues);
        }

        monitor.onLog("[ProjectAuditor] Fix phase complete");
        monitor.onPhase("Fixing", 85);
    }

    private void fixMissingImports(ProjectContext context, List<ProjectIssue> issues) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("Fix the following missing import issues:\n\n");
        for (ProjectIssue issue : issues) {
            sb.append("- File: ").append(issue.getFile()).append("\n");
            sb.append("  Missing: ").append(issue.getDetail()).append("\n");
        }
        sb.append("\nFor each file, provide the corrected version.\n");

        String response = ai.call(
            "You are a senior software engineer. Fix missing imports in the generated project.",
            sb.toString()
        );

        monitor.onLog("[ProjectAuditor] Fix response received");
    }

    public boolean detectMissingFiles(ProjectContext context) throws Exception {
        List<String> missing = context.getMissingFiles();
        if (missing.isEmpty()) return true;

        monitor.onLog("[MissingFileDetector] " + missing.size() + " files still missing");

        StringBuilder sb = new StringBuilder();
        sb.append("Analyze this project. Check if any critical files are missing.\n\n");
        sb.append("Project: ").append(context.getPlan().getProjectName()).append("\n");
        sb.append("Type: ").append(context.getPlan().getProjectType()).append("\n\n");
        sb.append("Planned files:\n");
        for (String p : context.getPlan().getFilePaths()) {
            String status = context.isGenerated(p) ? "[OK]" : "[MISSING]";
            sb.append("  ").append(status).append(" ").append(p).append("\n");
        }
        sb.append("\nGenerated files:\n");
        for (String p : context.getGeneratedFiles()) {
            sb.append("  ").append(p).append("\n");
        }
        sb.append("\nAre there any essential files missing? Consider:\n");
        sb.append("- Package manager files (package.json, pom.xml, etc.)\n");
        sb.append("- Entry point / main files\n");
        sb.append("- Configuration files\n");
        sb.append("- Build files\n");
        sb.append("- README, .gitignore, LICENSE\n");
        sb.append("- Missing files from the planned list\n\n");
        sb.append("Return a JSON array of missing file paths: [\"path1\", \"path2\", ...]");

        String response = ai.call(
            "You are a senior software architect. Identify missing files.",
            sb.toString()
        );

        List<String> newFiles = new ArrayList<>();
        String json = ResponseParser.extractJsonString(response);
        if (json.startsWith("[")) {
            try {
                com.google.gson.JsonArray arr = com.google.gson.JsonParser.parseString(json).getAsJsonArray();
                for (int i = 0; i < arr.size(); i++) {
                    String p = arr.get(i).getAsString();
                    if (!context.isGenerated(p) && !context.isFailed(p)) {
                        newFiles.add(p);
                        context.getPlan().addFilePath(p);
                    }
                }
            } catch (Exception e) {
                // fallback to line parsing
                for (String line : response.split("\\n")) {
                    line = line.trim().replaceAll("^[-*\\d.]+\\s*", "");
                    if (line.contains("/") || line.contains("\\")) {
                        if (!context.isGenerated(line) && !context.isFailed(line)) {
                            newFiles.add(line);
                            context.getPlan().addFilePath(line);
                        }
                    }
                }
            }
        }

        monitor.onLog("[MissingFileDetector] Added " + newFiles.size() + " missing files");
        return newFiles.isEmpty();
    }

    private static String filenameWithoutExt(String path) {
        String name = path;
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) name = name.substring(slash + 1);
        int dot = name.lastIndexOf('.');
        if (dot > 0) name = name.substring(0, dot);
        return name;
    }

    private static String resolveRelativeImport(String currentFile, String importPath) {
        String dir = currentFile;
        int slash = Math.max(dir.lastIndexOf('/'), dir.lastIndexOf('\\'));
        if (slash >= 0) {
            dir = dir.substring(0, slash + 1);
        } else {
            dir = "";
        }

        String resolved = importPath.replace("\\", "/");
        while (resolved.startsWith("../")) {
            resolved = resolved.substring(3);
            int idx = Math.max(dir.lastIndexOf('/', dir.length() - 2), dir.lastIndexOf('\\', dir.length() - 2));
            if (idx >= 0) {
                dir = dir.substring(0, idx + 1);
            } else {
                dir = "";
            }
        }
        while (resolved.startsWith("./")) {
            resolved = resolved.substring(2);
        }

        String result = dir + resolved;

        if (!result.contains(".")) {
            String[] exts = { ".js", ".jsx", ".ts", ".tsx", ".java", ".py", ".php" };
            for (String ext : exts) {
                String candidate = result + ext;
                if (new File(candidate).exists() || candidate.contains(".")) {
                    return candidate;
                }
            }
        }

        return result;
    }

    public static class ProjectIssue {
        private final String file;
        private final String type;
        private final String detail;

        public ProjectIssue(String file, String type, String detail) {
            this.file = file;
            this.type = type;
            this.detail = detail;
        }

        public String getFile() { return file; }
        public String getType() { return type; }
        public String getDetail() { return detail; }

        @Override
        public String toString() {
            return "[" + type + "] " + file + ": " + detail;
        }
    }
}
