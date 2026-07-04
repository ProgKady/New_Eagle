package com.eagle.model;

/**
 * Distinguishes the two project kinds the IDE supports:
 * - CODE: a normal file-tree + text-editor project (HTML/CSS/JS/etc.)
 * - VISUAL: a drag-and-drop "builder" project, stored as a JSON layout
 *   alongside the generated HTML/CSS output.
 */
public enum ProjectType {
    CODE("Code Project", "Write HTML, CSS, JS and more by hand with a full code editor."),
    VISUAL("Drag & Drop Project", "Build pages visually by dragging components onto a canvas."),
    ANDROID_JS("Android JS Project", "Build Android apps using DroidScript JavaScript framework with native API access.");

    private final String displayName;
    private final String description;

    ProjectType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
