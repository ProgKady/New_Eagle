package com.eagle.util;

import com.eagle.model.ProjectMeta;
import com.eagle.model.ProjectType;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class ProjectsStore {

    private static final String PREF_KEY = "projects_root";
    private static final String ROOT_SET_KEY = "projects_root_set";
    private static final Preferences prefs = Preferences.userNodeForPackage(ProjectsStore.class);

    public static boolean isRootSet() {
        return prefs.getBoolean(ROOT_SET_KEY, false);
    }

    public static File getProjectsRoot() {
        String path = prefs.get(PREF_KEY, null);
        if (path != null) {
            File f = new File(path);
            if (f.isDirectory()) return f;
        }
        File defaultDir = new File(System.getProperty("user.home"), "EagleProjects");
        if (!defaultDir.exists()) defaultDir.mkdirs();
        setProjectsRoot(defaultDir);
        return defaultDir;
    }

    public static void setProjectsRoot(File dir) {
        if (dir != null && dir.isDirectory()) {
            prefs.put(PREF_KEY, dir.getAbsolutePath());
            prefs.putBoolean(ROOT_SET_KEY, true);
        }
    }

    public static List<File> scanProjects() {
        List<File> result = new ArrayList<>();
        File root = getProjectsRoot();
        File[] dirs = root.listFiles();
        if (dirs != null) {
            for (File d : dirs) {
                if (d.isDirectory() && !d.isHidden() && !d.getName().startsWith(".")) {
                    File marker = new File(d, ".eagle-project");
                    if (marker.exists()) {
                        result.add(d);
                    }
                }
            }
        }
        return result;
    }

    public static List<File> scanAllDirectories() {
        List<File> result = new ArrayList<>();
        File root = getProjectsRoot();
        File[] dirs = root.listFiles();
        if (dirs != null) {
            for (File d : dirs) {
                if (d.isDirectory() && !d.isHidden() && !d.getName().startsWith(".")) {
                    result.add(d);
                }
            }
        }
        return result;
    }

    public static File createProject(String name, ProjectType type) {
        File root = getProjectsRoot();
        File projectDir = new File(root, name);
        if (projectDir.exists()) return null;
        if (projectDir.mkdirs()) {
            ProjectMeta.write(projectDir, type);
            return projectDir;
        }
        return null;
    }

    public static boolean deleteProject(File projectDir) {
        if (projectDir == null || !projectDir.isDirectory()) return false;
        deleteRecursive(projectDir);
        return !projectDir.exists();
    }

    public static boolean renameProject(File projectDir, String newName) {
        if (projectDir == null || !projectDir.isDirectory()) return false;
        File parent = projectDir.getParentFile();
        File renamed = new File(parent, newName);
        return projectDir.renameTo(renamed);
    }

    public static File duplicateProject(File projectDir, String newName) {
        if (projectDir == null || !projectDir.isDirectory()) return null;
        File parent = projectDir.getParentFile();
        File copy = new File(parent, newName);
        if (copy.exists()) return null;
        try {
            copyDir(projectDir, copy);
            return copy;
        } catch (Exception e) {
            return null;
        }
    }

    public static ProjectType getProjectType(File projectDir) {
        return ProjectMeta.read(projectDir);
    }

    private static void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File c : children) deleteRecursive(c);
            }
        }
        file.delete();
    }

    private static void copyDir(File src, File dst) throws Exception {
        if (src.isDirectory()) {
            if (!dst.exists()) dst.mkdirs();
            String[] children = src.list();
            if (children != null) {
                for (String child : children) {
                    copyDir(new File(src, child), new File(dst, child));
                }
            }
        } else {
            java.nio.file.Files.copy(src.toPath(), dst.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
