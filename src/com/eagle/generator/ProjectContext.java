package com.eagle.generator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProjectContext {

    private final File projectDir;
    private final ProjectPlan plan;
    private final Set<String> generatedFiles;
    private final Set<String> failedFiles;
    private final Set<String> fixedFiles;
    private final Map<String, String> fileContents;
    private final Map<String, String> fileExtensions;
    private final StringBuilder conversationMemory;
    private int totalGenerated;
    private int totalFailed;
    private int totalFixed;

    public ProjectContext(File projectDir, ProjectPlan plan) {
        this.projectDir = projectDir;
        this.plan = plan;
        this.generatedFiles = new LinkedHashSet<>();
        this.failedFiles = new LinkedHashSet<>();
        this.fixedFiles = new LinkedHashSet<>();
        this.fileContents = new LinkedHashMap<>();
        this.fileExtensions = new LinkedHashMap<>();
        this.conversationMemory = new StringBuilder();
        this.totalGenerated = 0;
        this.totalFailed = 0;
        this.totalFixed = 0;
    }

    public File getProjectDir() { return projectDir; }
    public ProjectPlan getPlan() { return plan; }

    public Set<String> getGeneratedFiles() {
        return Collections.unmodifiableSet(generatedFiles);
    }

    public Set<String> getFailedFiles() {
        return Collections.unmodifiableSet(failedFiles);
    }

    public Set<String> getFixedFiles() {
        return Collections.unmodifiableSet(fixedFiles);
    }

    public boolean isGenerated(String path) {
        return generatedFiles.contains(normalize(path));
    }

    public boolean isFailed(String path) {
        return failedFiles.contains(normalize(path));
    }

    public void markGenerated(String path) {
        String n = normalize(path);
        generatedFiles.add(n);
        failedFiles.remove(n);
        totalGenerated++;
    }

    public void markFailed(String path) {
        String n = normalize(path);
        failedFiles.add(n);
        totalFailed++;
    }

    public void markFixed(String path) {
        String n = normalize(path);
        fixedFiles.add(n);
        failedFiles.remove(n);
        totalFixed++;
    }

    public void storeContent(String path, String content) {
        fileContents.put(normalize(path), content);
    }

    public String getContent(String path) {
        return fileContents.get(normalize(path));
    }

    public void storeExtension(String path, String ext) {
        fileExtensions.put(normalize(path), ext);
    }

    public String getExtension(String path) {
        return fileExtensions.get(normalize(path));
    }

    public int getTotalGenerated() { return totalGenerated; }
    public int getTotalFailed() { return totalFailed; }
    public int getTotalFixed() { return totalFixed; }

    public int plannedFileCount() {
        return plan.getFilePaths().size();
    }

    public int remainingFileCount() {
        int remaining = 0;
        for (String path : plan.getFilePaths()) {
            if (!generatedFiles.contains(path) && !failedFiles.contains(path)) {
                remaining++;
            }
        }
        return remaining;
    }

    public List<String> getMissingFiles() {
        List<String> missing = new ArrayList<>();
        for (String path : plan.getFilePaths()) {
            if (!generatedFiles.contains(path) && !failedFiles.contains(path)) {
                missing.add(path);
            }
        }
        return missing;
    }

    public void appendConversation(String text) {
        conversationMemory.append(text).append("\n");
    }

    public String getConversationMemory() {
        return conversationMemory.toString();
    }

    public int getConversationLength() {
        return conversationMemory.length();
    }

    public void resetConversation() {
        conversationMemory.setLength(0);
    }

    private static String normalize(String path) {
        return path.replace('\\', '/').trim();
    }
}
