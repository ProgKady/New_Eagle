package com.eagle.model;

public class RecentProject {
    private final String name;
    private final String path;

    public RecentProject(String name, String path) {
        this.name = name;
        this.path = path;
    }

    public String getName() { return name; }
    public String getPath() { return path; }

    @Override
    public String toString() {
        return name + "   —   " + path;
    }
}
