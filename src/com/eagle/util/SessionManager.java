package com.eagle.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Persists the last open project and its open file list so the IDE can
 * restore the previous session on next launch (or after a crash). Paths are
 * stored relative to the project root so the session is portable if the
 * project folder is moved together with its contents.
 */
public class SessionManager {

    private static final Preferences prefs = Preferences.userNodeForPackage(SessionManager.class);
    private static final String KEY_LAST_PROJECT = "lastProjectPath";
    private static final String KEY_OPEN_FILES_PREFIX = "openFiles::";

    public static void saveLastSession(File projectDir) {
        prefs.put(KEY_LAST_PROJECT, projectDir.getAbsolutePath());
    }

    public static File getLastProject() {
        String path = prefs.get(KEY_LAST_PROJECT, null);
        if (path == null) return null;
        File dir = new File(path);
        return dir.exists() && dir.isDirectory() ? dir : null;
    }

    public static void clearLastSession() {
        // Intentionally keep the last project recorded for "resume on launch";
        // only the dirty/crash markers would be cleared here if we tracked them.
    }

    /** Records the currently open files (by path relative to the project root) for this project. */
    public static void recordOpenFiles(File projectDir, java.util.Collection<File> openFiles) {
        List<String> relPaths = new ArrayList<>();
        String rootPath = projectDir.getAbsolutePath();
        for (File f : openFiles) {
            String abs = f.getAbsolutePath();
            if (abs.startsWith(rootPath)) {
                String rel = abs.substring(rootPath.length());
                if (rel.startsWith(File.separator)) rel = rel.substring(1);
                relPaths.add(rel);
            }
        }
        prefs.put(KEY_OPEN_FILES_PREFIX + rootPath, String.join("\n", relPaths));
    }

    public static List<String> getLastOpenFiles(File projectDir) {
        String stored = prefs.get(KEY_OPEN_FILES_PREFIX + projectDir.getAbsolutePath(), "");
        List<String> result = new ArrayList<>();
        if (stored.trim().isEmpty()) return result;
        for (String line : stored.split("\n")) {
            if (!line.trim().isEmpty()) result.add(line.trim());
        }
        return result;
    }
}
