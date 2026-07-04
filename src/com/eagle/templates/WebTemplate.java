package com.eagle.templates;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A ready-made starter template: a named bundle of files (relative path -> content)
 * that gets written into a new project folder.
 */
public class WebTemplate {
    private final String id;
    private final String name;
    private final String description;
    private final String icon;
    private final Map<String, String> files = new LinkedHashMap<>();

    public WebTemplate(String id, String name, String description, String icon) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
    }

    public WebTemplate addFile(String relativePath, String content) {
        files.put(relativePath, content);
        return this;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getIcon() { return icon; }
    public Map<String, String> getFiles() { return files; }

    @Override
    public String toString() {
        return icon + "  " + name;
    }
}
