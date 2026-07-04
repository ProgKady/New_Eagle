package com.eagle.editor;

public class ExtensionInfo {
    public String id;
    public String name;
    public String description;
    public String version;
    public String author;
    public String downloadUrl;
    public String iconUrl;
    public long size; // bytes
    public String category;
    public int downloads;

    public ExtensionInfo() {}

    public ExtensionInfo(String id, String name, String description, String version, String author, String downloadUrl) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.version = version;
        this.author = author;
        this.downloadUrl = downloadUrl;
    }
}
