package com.eagle.plugin;

public class PluginInfo {
    private final String id;
    private final String name;
    private final String version;
    private final String author;
    private final String description;
    private boolean enabled;

    public PluginInfo(String id, String name, String version, String author, String description, boolean enabled) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.author = author;
        this.description = description;
        this.enabled = enabled;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getAuthor() { return author; }
    public String getDescription() { return description; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
