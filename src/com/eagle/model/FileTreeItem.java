package com.eagle.model;

import java.io.File;

/**
 * Wraps a File for display in the project TreeView.
 */
public class FileTreeItem {
    private final File file;

    public FileTreeItem(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public boolean isDirectory() {
        return file.isDirectory();
    }

    @Override
    public String toString() {
        return file.getName();
    }
}
