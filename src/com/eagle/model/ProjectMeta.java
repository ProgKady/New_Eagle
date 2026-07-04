package com.eagle.model;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Reads/writes a hidden ".eagle" marker file inside a project folder so the
 * IDE remembers whether the project is a CODE project or a VISUAL (drag&drop) one.
 */
public class ProjectMeta {

    private static final String MARKER_FILE = ".eagle-project";

    public static void write(File projectDir, ProjectType type) {
        try {
            File marker = new File(projectDir, MARKER_FILE);
            Files.write(marker.toPath(), ("type=" + type.name() + "\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) { }
    }

    public static ProjectType read(File projectDir) {
        File marker = new File(projectDir, MARKER_FILE);
        if (!marker.exists()) return ProjectType.CODE; // default/back-compat
        try {
            String content = new String(Files.readAllBytes(marker.toPath()), StandardCharsets.UTF_8);
            for (String line : content.split("\n")) {
                if (line.startsWith("type=")) {
                    String val = line.substring(5).trim();
                    try {
                        return ProjectType.valueOf(val);
                    } catch (IllegalArgumentException ignored) { }
                }
            }
        } catch (IOException ignored) { }
        return ProjectType.CODE;
    }

    public static boolean isMarkerFile(File file) {
        return file.getName().equals(MARKER_FILE);
    }
}
