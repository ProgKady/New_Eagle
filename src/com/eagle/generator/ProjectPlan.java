package com.eagle.generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProjectPlan {

    private final String projectName;
    private final String projectType;
    private final String description;
    private String techStack;
    private String architecture;
    private String designPatterns;
    private String buildTool;
    private String packageManager;
    private String database;
    private final List<String> filePaths;
    private final Map<String, String> dependencies;
    private final Map<String, List<String>> folderTree;

    public ProjectPlan(String projectName, String projectType, String description) {
        this.projectName = projectName;
        this.projectType = projectType;
        this.description = description;
        this.techStack = "";
        this.architecture = "";
        this.designPatterns = "";
        this.buildTool = "";
        this.packageManager = "";
        this.database = "";
        this.filePaths = new ArrayList<>();
        this.dependencies = new LinkedHashMap<>();
        this.folderTree = new LinkedHashMap<>();
    }

    public String getProjectName() { return projectName; }
    public String getProjectType() { return projectType; }
    public String getDescription() { return description; }
    public String getTechStack() { return techStack; }
    public void setTechStack(String s) { this.techStack = s; }
    public String getArchitecture() { return architecture; }
    public void setArchitecture(String s) { this.architecture = s; }
    public String getDesignPatterns() { return designPatterns; }
    public void setDesignPatterns(String s) { this.designPatterns = s; }
    public String getBuildTool() { return buildTool; }
    public void setBuildTool(String s) { this.buildTool = s; }
    public String getPackageManager() { return packageManager; }
    public void setPackageManager(String s) { this.packageManager = s; }
    public String getDatabase() { return database; }
    public void setDatabase(String s) { this.database = s; }

    public List<String> getFilePaths() {
        return Collections.unmodifiableList(filePaths);
    }

    public void addFilePath(String path) {
        if (!filePaths.contains(path)) {
            filePaths.add(path);
        }
    }

    public void addFilePaths(List<String> paths) {
        for (String p : paths) {
            addFilePath(p);
        }
    }

    public Map<String, String> getDependencies() {
        return Collections.unmodifiableMap(dependencies);
    }

    public void addDependency(String name, String version) {
        dependencies.put(name, version);
    }

    public Map<String, List<String>> getFolderTree() {
        return Collections.unmodifiableMap(folderTree);
    }

    public void addToFolder(String folder, String filePath) {
        List<String> files = folderTree.get(folder);
        if (files == null) {
            files = new ArrayList<>();
            folderTree.put(folder, files);
        }
        if (!files.contains(filePath)) {
            files.add(filePath);
        }
    }

    public void buildFolderTree() {
        folderTree.clear();
        for (String path : filePaths) {
            int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
            String folder = (lastSlash >= 0) ? path.substring(0, lastSlash) : ".";
            addToFolder(folder, path);
        }
    }
}
