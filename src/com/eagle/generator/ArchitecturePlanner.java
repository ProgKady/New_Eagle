package com.eagle.generator;

import java.util.List;

public class ArchitecturePlanner {

    private final AiProvider ai;
    private final ProgressMonitor monitor;

    private static final String PLAN_PROMPT =
        "You are a senior software architect. Create a complete project plan for the following project.\n\n"
        + "You MUST respond with ONLY a valid JSON array of file objects. No markdown, no explanation, no code blocks.\n\n"
        + "Each object in the array MUST have these fields:\n"
        + "  \"path\": \"relative/file/path.ext\"\n"
        + "  \"description\": \"brief description of this file\"\n"
        + "  \"language\": \"programming language\"\n"
        + "  \"type\": \"source|config|asset|test|doc|script|docker\"\n\n"
        + "Rules:\n"
        + "- Generate ALL files the project needs (unlimited number)\n"
        + "- Include source code, configs, tests, docs, build files, CI/CD, Docker\n"
        + "- Use standard project structures for the given type\n"
        + "- Include package.json, pom.xml, build.gradle, Cargo.toml, go.mod, requirements.txt, etc. as appropriate\n"
        + "- Include .gitignore, README.md, LICENSE\n"
        + "- Include Dockerfile, docker-compose.yml, .github/workflows/ if appropriate\n"
        + "- Use proper folder depth (src/main/java/com/example/... etc)\n"
        + "- Paths use forward slashes\n\n"
        + "Project Name: %s\n"
        + "Project Type: %s\n"
        + "Description: %s";

    private static final String TECH_PROMPT =
        "Based on this project description, recommend technologies, frameworks, database, build tools, "
        + "and design patterns. Keep it brief.\n\nProject: %s\nType: %s\nDescription: %s";

    public ArchitecturePlanner(AiProvider ai, ProgressMonitor monitor) {
        this.ai = ai;
        this.monitor = monitor;
    }

    public ProjectPlan createPlan(String name, String type, String description) throws Exception {
        ProjectPlan plan = new ProjectPlan(name, type, description);

        monitor.onPhase("Planning", 5);
        monitor.onLog("[ArchitecturePlanner] Analyzing project...");

        String safeName = escFmt(name);
        String safeType = escFmt(type);
        String safeDesc = escFmt(description);

        try {
            String techResponse = ai.call(
                "You are a senior software architect. Recommend technologies briefly.",
                String.format(TECH_PROMPT, safeName, safeType, safeDesc)
            );
            plan.setTechStack(techResponse);
            monitor.onLog("[ArchitecturePlanner] Tech analysis: " + (techResponse != null ? techResponse.substring(0, Math.min(100, techResponse.length())) : "null"));
        } catch (Exception e) {
            monitor.onLog("[ArchitecturePlanner] Tech analysis failed: " + e.getMessage());
            plan.setTechStack("Standard " + type + " stack");
        }

        monitor.onLog("[ArchitecturePlanner] Generating file structure...");
        monitor.onPhase("Planning", 15);

        String planResponse;
        try {
            planResponse = ai.call(
                "You are a senior software architect. Generate a complete file list as JSON array only.",
                String.format(PLAN_PROMPT, safeName, safeType, safeDesc)
            );
        } catch (Exception e) {
            monitor.onLog("[ArchitecturePlanner] Plan AI call failed: " + e.getMessage());
            return plan;
        }

        monitor.onPhase("Planning", 25);
        if (planResponse == null || planResponse.trim().isEmpty()) {
            monitor.onLog("[ArchitecturePlanner] AI returned empty response!");
            return plan;
        }
        monitor.onLog("[ArchitecturePlanner] AI response length: " + planResponse.length());

        String json = ResponseParser.extractJsonString(planResponse);
        monitor.onLog("[ArchitecturePlanner] Extracted JSON (" + json.length() + " chars): "
            + json.substring(0, Math.min(200, json.length())));

        List<java.util.Map<String, String>> files = ResponseParser.parseJsonFileList(json);
        monitor.onLog("[ArchitecturePlanner] Parsed " + files.size() + " file entries from JSON");

        if (files.isEmpty()) {
            List<String> paths = ResponseParser.extractFilePaths(planResponse);
            monitor.onLog("[ArchitecturePlanner] extractFilePaths gave " + paths.size() + " paths");
            plan.addFilePaths(paths);
        } else {
            for (java.util.Map<String, String> f : files) {
                String path = f.get("path");
                if (path != null && !path.isEmpty()) {
                    plan.addFilePath(path);
                    String desc = f.get("description");
                    if (desc == null) desc = f.get("type");
                    if (desc == null) desc = "";
                    plan.addToFolder(parentFolder(path), path);
                }
            }
        }

        plan.buildFolderTree();

        monitor.onLog("[ArchitecturePlanner] Plan created: " + plan.getFilePaths().size() + " files");
        monitor.onPhase("Planning", 30);

        return plan;
    }

    public boolean refinePlan(ProjectContext context) throws Exception {
        ProjectPlan plan = context.getPlan();
        int remaining = context.remainingFileCount();

        if (remaining == 0) {
            return true;
        }

        monitor.onLog("[ArchitecturePlanner] Refining plan - " + remaining + " files remaining");

        StringBuilder sb = new StringBuilder();
        sb.append("We already generated:\n");
        for (String p : context.getGeneratedFiles()) {
            sb.append("- ").append(p).append("\n");
        }
        sb.append("\nStill missing:\n");
        for (String p : context.getMissingFiles()) {
            sb.append("- ").append(p).append("\n");
        }
        sb.append("\nGenerate ONLY the missing files. Return JSON array of file objects.");

        String response = ai.call(
            "You are a senior software architect. Generate only missing files as JSON array.",
            sb.toString()
        );

        String json = ResponseParser.extractJsonString(response);
        List<java.util.Map<String, String>> files = ResponseParser.parseJsonFileList(json);

        if (!files.isEmpty()) {
            for (java.util.Map<String, String> f : files) {
                String path = f.get("path");
                if (path != null && !context.isGenerated(path) && !context.isFailed(path)) {
                    plan.addFilePath(path);
                }
            }
            plan.buildFolderTree();
        }

        return context.remainingFileCount() == 0;
    }

    private static String parentFolder(String path) {
        int idx = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return (idx >= 0) ? path.substring(0, idx) : ".";
    }

    private static String escFmt(String s) {
        return s.replace("%", "%%");
    }
}
