package com.eagle.generator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AiProjectEngine {

    private final AiProvider ai;
    private final ProgressMonitor monitor;
    private final ArchitecturePlanner planner;
    private final BatchFileGenerator generator;
    private final ProjectAuditor auditor;

    private static final int BATCH_SIZE = 5;
    private static final int MAX_FIX_ITERATIONS = 3;

    public AiProjectEngine(AiProvider ai, ProgressMonitor monitor) {
        this.ai = ai;
        this.monitor = monitor;
        this.planner = new ArchitecturePlanner(ai, monitor);
        this.generator = new BatchFileGenerator(ai, monitor, BATCH_SIZE);
        this.auditor = new ProjectAuditor(ai, monitor);
    }

    public GenerationResult generate(String projectName, String projectType,
                                      String description, File projectDir) {
        GenerationResult result = new GenerationResult();
        result.startTime = System.currentTimeMillis();

        try {
            if (!projectDir.exists()) {
                projectDir.mkdirs();
            }

            monitor.onLog("=== GENERATION STARTED ===");
            monitor.onLog("Project: " + projectName);
            monitor.onLog("Type: " + projectType);
            monitor.onLog("Directory: " + projectDir.getAbsolutePath());
            monitor.onPhase("Planning", 0);

            ProjectPlan plan = planner.createPlan(projectName, projectType, description);
            monitor.onLog("[Planner] Architecture plan created with " + plan.getFilePaths().size() + " files");

            ProjectContext context = new ProjectContext(projectDir, plan);
            context.appendConversation("Architecture planned: " + plan.getFilePaths().size() + " files\n");

            monitor.onPhase("Generating", 30);
            int generated = generator.generate(context);
            result.filesGenerated = generated;
            monitor.onLog("[Generator] Generated " + generated + " files");

            context.appendConversation("Generated " + generated + " files\n");

            int fixIteration = 0;
            boolean allComplete = false;

            while (!allComplete && fixIteration < MAX_FIX_ITERATIONS) {
                fixIteration++;

                monitor.onPhase("Reviewing", 75);

                List<ProjectAuditor.ProjectIssue> issues = auditor.audit(context);
                result.issuesFound += issues.size();

                if (!issues.isEmpty()) {
                    monitor.onLog("[Auditor] Found " + issues.size() + " issues (iteration " + fixIteration + ")");
                    auditor.fixIssues(context, issues);
                    result.issuesFixed += issues.size();
                }

                monitor.onPhase("Scanning", 85);

                boolean missingDetected = !auditor.detectMissingFiles(context);
                if (missingDetected) {
                    monitor.onLog("[Auditor] " + context.remainingFileCount() + " missing files detected");
                    context.appendConversation("Missing files detected: " + context.remainingFileCount() + "\n");

                    generator.generateFiles(context, context.getMissingFiles());
                }

                allComplete = (context.remainingFileCount() == 0 && !missingDetected);

                if (fixIteration == 1) {
                    planner.refinePlan(context);
                    if (!context.getMissingFiles().isEmpty()) {
                        generator.generateFiles(context, context.getMissingFiles());
                    }
                }
            }

            if (context.remainingFileCount() > 0) {
                monitor.onLog("[Engine] " + context.remainingFileCount() + " files could not be generated");
                result.filesMissing = context.remainingFileCount();
            }

            monitor.onPhase("Completed", 100);
            monitor.onLog("=== GENERATION COMPLETE ===");
            monitor.onLog("Generated: " + context.getTotalGenerated()
                + " | Failed: " + context.getTotalFailed()
                + " | Fixed: " + context.getTotalFixed()
                + " | Issues: " + result.issuesFound);

            result.endTime = System.currentTimeMillis();
            result.success = true;
            result.context = context;
            result.filePaths = new ArrayList<>(context.getGeneratedFiles());

            generator.shutdown();

        } catch (Exception e) {
            monitor.onError("Engine error: " + e.getMessage());
            result.success = false;
            result.errorMessage = e.getMessage();
            generator.shutdown();
        }

        monitor.onComplete(result.filePaths != null ? result.filePaths.size() : 0,
            result.filePaths != null ? result.filePaths : new ArrayList<String>());

        return result;
    }

    public static class GenerationResult {
        private boolean success;
        private String errorMessage;
        private int filesGenerated;
        private int filesMissing;
        private int issuesFound;
        private int issuesFixed;
        private long startTime;
        private long endTime;
        private ProjectContext context;
        private List<String> filePaths;

        private GenerationResult() {
            this.success = false;
            this.errorMessage = "";
            this.filesGenerated = 0;
            this.filesMissing = 0;
            this.issuesFound = 0;
            this.issuesFixed = 0;
            this.startTime = 0;
            this.endTime = 0;
            this.context = null;
            this.filePaths = new ArrayList<>();
        }

        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public int getFilesGenerated() { return filesGenerated; }
        public int getFilesMissing() { return filesMissing; }
        public int getIssuesFound() { return issuesFound; }
        public int getIssuesFixed() { return issuesFixed; }
        public long getDuration() { return endTime - startTime; }
        public ProjectContext getContext() { return context; }
        public List<String> getFilePaths() { return filePaths; }
    }
}
