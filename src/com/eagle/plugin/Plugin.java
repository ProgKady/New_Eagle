package com.eagle.plugin;

public interface Plugin {
    String getId();
    String getName();
    String getVersion();
    String getAuthor();
    String getDescription();
    void init(PluginContext context);
    void shutdown();
}
