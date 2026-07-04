package com.eagle.generator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class BatchFileGenerator {

    private final AiProvider ai;
    private final ProgressMonitor monitor;
    private final ExecutorService executor;
    private final int batchSize;

    private static final String GENERATE_PROMPT =
        "You are a senior software engineer. Generate production-quality code for the missing files.\n\n"
        + "Project: %s\nType: %s\nDescription: %s\n\n"
        + "Tech Stack: %s\nArchitecture: %s\n\n"
        + "Already generated files:\n%s\n\n"
        + "Current project structure:\n%s\n\n"
        + "%s\n\n"
        + "IMPORTANT: For each file, use this exact format:\n"
        + "```<language>\nFile: path/to/file.ext\n<file content>\n```\n\n"
        + "Generate ALL these files in a single response. Make them complete and functional.\n"
        + "Files to generate:\n%s";

    public BatchFileGenerator(AiProvider ai, ProgressMonitor monitor, int batchSize) {
        this.ai = ai;
        this.monitor = monitor;
        this.executor = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() / 2));
        this.batchSize = batchSize;
    }

    public void shutdown() {
        executor.shutdown();
    }

    public int generate(ProjectContext context) throws Exception {
        List<String> pending = context.getMissingFiles();
        if (pending.isEmpty()) return 0;

        int totalGenerated = 0;
        int totalPlanned = context.plannedFileCount();
        int alreadyDone = context.getTotalGenerated();

        List<List<String>> batches = createBatches(pending, batchSize);

        for (int batchIdx = 0; batchIdx < batches.size(); batchIdx++) {
            if (monitor.isCancelled()) {
                monitor.onLog("[BatchFileGenerator] Cancelled by user");
                return totalGenerated;
            }

            List<String> batch = batches.get(batchIdx);
            monitor.onLog(String.format(
                "[BatchFileGenerator] Batch %d/%d: %d files",
                batchIdx + 1, batches.size(), batch.size()
            ));

            List<Callable<Integer>> tasks = new ArrayList<>();
            for (String filePath : batch) {
                tasks.add(() -> generateSingleFile(context, filePath));
            }

            List<Future<Integer>> futures = executor.invokeAll(tasks);

            int batchGenerated = 0;
            for (Future<Integer> f : futures) {
                batchGenerated += f.get();
            }

            totalGenerated += batchGenerated;
            alreadyDone += batchGenerated;

            double pct = 30.0 + ((double) alreadyDone / totalPlanned) * 40.0;
            monitor.onPhase("Generating", Math.min(pct, 70.0));

            if (batchGenerated < batch.size()) {
                monitor.onLog(String.format(
                    "[BatchFileGenerator] Batch %d: %d/%d succeeded, requesting retry...",
                    batchIdx + 1, batchGenerated, batch.size()
                ));
                retryFailedFiles(context, batch);
            }
        }

        return totalGenerated;
    }

    public int generateFiles(ProjectContext context, List<String> filePaths) throws Exception {
        if (filePaths.isEmpty()) return 0;

        int count = 0;
        List<List<String>> batches = createBatches(filePaths, batchSize);

        for (int i = 0; i < batches.size(); i++) {
            if (monitor.isCancelled()) return count;

            List<String> batch = batches.get(i);
            monitor.onLog(String.format(
                "[BatchFileGenerator] Generating batch %d/%d: %s",
                i + 1, batches.size(), batch
            ));

            List<String> missing = new ArrayList<>();
            for (String fp : batch) {
                if (!context.isGenerated(fp) && !context.isFailed(fp)) {
                    missing.add(fp);
                }
            }

            if (missing.isEmpty()) continue;

            generateBatch(context, missing);

            for (String fp : missing) {
                if (context.isGenerated(fp)) count++;
            }

            double pct = 30.0 + ((double) context.getTotalGenerated() / context.plannedFileCount()) * 40.0;
            monitor.onPhase("Generating", Math.min(pct, 70.0));
        }

        return count;
    }

    private void generateBatch(ProjectContext context, List<String> filePaths) throws Exception {
        StringBuilder fileList = new StringBuilder();
        Set<String> extensions = new HashSet<>();
        for (String fp : filePaths) {
            fileList.append("- ").append(fp).append("\n");
            extensions.add(ResponseParser.extractLanguage(fp));
        }

        StringBuilder existingFiles = new StringBuilder();
        for (String fp : context.getGeneratedFiles()) {
            existingFiles.append("- ").append(fp).append("\n");
        }

        StringBuilder structure = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : context.getPlan().getFolderTree().entrySet()) {
            structure.append(entry.getKey()).append("/\n");
            for (String f : entry.getValue()) {
                String marker = context.isGenerated(f) ? " [GENERATED]" : " [PENDING]";
                structure.append("  ").append(f).append(marker).append("\n");
            }
        }

        String prompt = String.format(GENERATE_PROMPT,
            escFmt(context.getPlan().getProjectName()),
            escFmt(context.getPlan().getProjectType()),
            escFmt(context.getPlan().getDescription()),
            escFmt(context.getPlan().getTechStack()),
            escFmt(context.getPlan().getArchitecture()),
            existingFiles.toString(),
            structure.toString(),
            "Make files complete, functional, with all imports and dependencies.",
            fileList.toString()
        );

        String response = ai.call(
            "You are a senior software engineer. Generate complete, production-ready files.",
            prompt
        );

        Map<String, String> extracted = ResponseParser.extractFiles(response);

        if (extracted.isEmpty()) {
            retryWithDirectPrompt(context, filePaths, response);
            return;
        }

        for (String fp : filePaths) {
            boolean matched = false;
            String normalizedTarget = ResponseParser.normalizePath(fp);

            for (Map.Entry<String, String> entry : extracted.entrySet()) {
                String extractedPath = ResponseParser.normalizePath(entry.getKey());
                if (extractedPath.equals(normalizedTarget)
                    || extractedPath.endsWith("/" + normalizedTarget)
                    || extractedPath.endsWith("\\" + normalizedTarget)) {
                    writeFile(context, fp, entry.getValue());
                    matched = true;
                    break;
                }
            }

            if (!matched) {
                monitor.onLog("[BatchFileGenerator] No match found for: " + fp);
            }
        }
    }

    private void retryWithDirectPrompt(ProjectContext context, List<String> filePaths, String previousResponse) throws Exception {
        if (monitor.isCancelled()) return;

        monitor.onLog("[BatchFileGenerator] Retrying with direct prompts...");

        for (String fp : filePaths) {
            if (context.isGenerated(fp) || context.isFailed(fp)) continue;

            String directPrompt = String.format(
                "Generate ONLY this single file.\nPath: %s\n\n"
                + "Project: %s\nType: %s\nDescription: %s\n\n"
                + "Respond with:\n```%s\nFile: %s\n<content>\n```",
                fp,
                context.getPlan().getProjectName(),
                context.getPlan().getProjectType(),
                context.getPlan().getDescription(),
                ResponseParser.extractLanguage(fp),
                fp
            );

            try {
                String resp = ai.call(
                    "Generate exactly one file. Use the exact file path in the response.",
                    directPrompt
                );

                Map<String, String> extracted = ResponseParser.extractFiles(resp);
                boolean written = false;

                for (Map.Entry<String, String> entry : extracted.entrySet()) {
                    String ep = ResponseParser.normalizePath(entry.getKey());
                    String nfp = ResponseParser.normalizePath(fp);
                    if (ep.equals(nfp) || ep.endsWith("/" + nfp) || ep.endsWith("\\" + nfp)) {
                        writeFile(context, fp, entry.getValue());
                        written = true;
                        break;
                    }
                }

                if (!written) {
                    monitor.onLog("[BatchFileGenerator] Failed direct retry for: " + fp);
                    context.markFailed(fp);
                }
            } catch (Exception e) {
                monitor.onLog("[BatchFileGenerator] Error generating " + fp + ": " + e.getMessage());
                context.markFailed(fp);
            }
        }
    }

    private void retryFailedFiles(ProjectContext context, List<String> batch) throws Exception {
        List<String> failed = new ArrayList<>();
        for (String fp : batch) {
            if (!context.isGenerated(fp) && !context.isFailed(fp)) {
                failed.add(fp);
            }
        }

        if (failed.isEmpty()) return;

        monitor.onLog("[BatchFileGenerator] Retrying " + failed.size() + " failed files...");

        for (String fp : failed) {
            if (context.isGenerated(fp)) continue;

            String directPrompt = String.format(
                "Generate this exact file:\nPath: %s\n\n"
                + "Project context: %s - %s\n\n"
                + "Use format: ```\nFile: %s\n<content>\n```",
                fp, context.getPlan().getProjectName(), context.getPlan().getDescription(), fp
            );

            try {
                String resp = ai.call(
                    "Generate one file. Include path in File: comment.",
                    directPrompt
                );
                Map<String, String> extracted = ResponseParser.extractFiles(resp);
                boolean written = false;

                for (Map.Entry<String, String> entry : extracted.entrySet()) {
                    if (ResponseParser.normalizePath(entry.getKey()).equals(ResponseParser.normalizePath(fp))) {
                        writeFile(context, fp, entry.getValue());
                        written = true;
                        break;
                    }
                }

                if (!written) {
                    context.markFailed(fp);
                }
            } catch (Exception e) {
                monitor.onLog("[BatchFileGenerator] Retry error for " + fp + ": " + e.getMessage());
                context.markFailed(fp);
            }
        }
    }

    private int generateSingleFile(ProjectContext context, String filePath) throws Exception {
        if (context.isGenerated(filePath)) return 0;

        String prompt = String.format(
            "Generate this file:\nPath: %s\n\nProject: %s (%s)\n%s\n\n"
            + "Language: %s\n\n"
            + "Respond with:\n```%s\nFile: %s\n<full implementation>\n```",
            filePath,
            context.getPlan().getProjectName(),
            context.getPlan().getProjectType(),
            context.getPlan().getDescription(),
            ResponseParser.extractLanguage(filePath),
            ResponseParser.extractLanguage(filePath),
            filePath
        );

        try {
            String response = ai.call(
                "Generate exactly one file with full implementation.",
                prompt
            );

            Map<String, String> extracted = ResponseParser.extractFiles(response);
            for (Map.Entry<String, String> entry : extracted.entrySet()) {
                String ep = ResponseParser.normalizePath(entry.getKey());
                String nfp = ResponseParser.normalizePath(filePath);
                if (ep.equals(nfp) || ep.endsWith("/" + nfp) || ep.endsWith("\\" + nfp)) {
                    writeFile(context, filePath, entry.getValue());
                    return 1;
                }
            }

            context.markFailed(filePath);
            return 0;
        } catch (Exception e) {
            context.markFailed(filePath);
            return 0;
        }
    }

    private void writeFile(ProjectContext context, String path, String content) {
        File target = new File(context.getProjectDir(), path);
        File parent = target.getParentFile();

        if (!parent.exists()) {
            parent.mkdirs();
        }

        try (FileWriter writer = new FileWriter(target)) {
            writer.write(content);
            context.markGenerated(path);
            context.storeContent(path, content);
            monitor.onLog("[BatchFileGenerator] Created: " + path);
        } catch (IOException e) {
            monitor.onLog("[BatchFileGenerator] Error writing " + path + ": " + e.getMessage());
            context.markFailed(path);
        }
    }

    private List<List<String>> createBatches(List<String> items, int size) {
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < items.size(); i += size) {
            batches.add(items.subList(i, Math.min(i + size, items.size())));
        }
        return batches;
    }

    private static String escFmt(String s) {
        return s.replace("%", "%%");
    }
}
