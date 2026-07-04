package com.eagle.util;

import com.eagle.model.RecentProject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Persists a small list of recently opened project folders between runs.
 */
public class RecentProjectsStore {

    private static final String KEY_PREFIX = "recent_";
    private static final String COUNT_KEY = "recent_count";
    private static final int MAX_RECENT = 8;

    private static final Preferences prefs = Preferences.userNodeForPackage(RecentProjectsStore.class);

    public static List<RecentProject> load() {
        List<RecentProject> result = new ArrayList<>();
        int count = prefs.getInt(COUNT_KEY, 0);
        for (int i = 0; i < count; i++) {
            String path = prefs.get(KEY_PREFIX + i, null);
            if (path != null) {
                File f = new File(path);
                if (f.exists() && f.isDirectory()) {
                    result.add(new RecentProject(f.getName(), path));
                }
            }
        }
        return result;
    }

    public static void addRecent(File projectDir) {
        List<RecentProject> current = load();
        current.removeIf(p -> p.getPath().equals(projectDir.getAbsolutePath()));
        current.add(0, new RecentProject(projectDir.getName(), projectDir.getAbsolutePath()));
        if (current.size() > MAX_RECENT) {
            current = current.subList(0, MAX_RECENT);
        }
        save(current);
    }

    public static void removeRecent(String path) {
        List<RecentProject> current = load();
        current.removeIf(p -> p.getPath().equals(path));
        save(current);
    }

    private static void save(List<RecentProject> list) {
        prefs.putInt(COUNT_KEY, list.size());
        for (int i = 0; i < list.size(); i++) {
            prefs.put(KEY_PREFIX + i, list.get(i).getPath());
        }
    }
}
