package com.eagle.plugin;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

public class PluginManager {

    private static PluginManager instance;
    private final Map<String, PluginWrapper> plugins = new java.util.LinkedHashMap<>();
    private final List<Plugin> builtins = new ArrayList<>();
    private final File pluginsDir;

    static class PluginWrapper {
        final Plugin plugin;
        final PluginInfo info;
        final PluginContext context;
        boolean loaded;
        File jarFile; // non-null for external JAR plugins

        PluginWrapper(Plugin plugin, PluginInfo info, PluginContext context) {
            this.plugin = plugin;
            this.info = info;
            this.context = context;
        }
    }

    private PluginManager() {
        String appDir = System.getProperty("user.dir");
        pluginsDir = new File(appDir, "plugins");
        if (!pluginsDir.exists()) pluginsDir.mkdirs();
    }

    public static synchronized PluginManager getInstance() {
        if (instance == null) instance = new PluginManager();
        return instance;
    }

    public void registerBuiltin(Plugin plugin) {
        builtins.add(plugin);
    }

    public void loadAll() {
        Preferences prefs = Preferences.userNodeForPackage(PluginManager.class);

        // Load builtins first
        for (Plugin p : builtins) {
            boolean enabled = prefs.getBoolean("plugin_" + p.getId(), true);
            PluginInfo info = new PluginInfo(p.getId(), p.getName(), p.getVersion(), p.getAuthor(), p.getDescription(), enabled);
            PluginContext ctx = new PluginContext();
            plugins.put(p.getId(), new PluginWrapper(p, info, ctx));
        }

        // Scan plugins/ directory for JAR/PLUGIN/ZIP files
        File[] jars = pluginsDir.listFiles((d, n) -> n.endsWith(".jar") || n.endsWith(".plugin") || n.endsWith(".zip"));
        if (jars != null) {
            for (File jar : jars) {
                loadExternalPlugin(jar);
            }
        }

        // Init all enabled plugins
        for (PluginWrapper w : plugins.values()) {
            if (w.info.isEnabled()) {
                activate(w);
            }
        }
    }

    private void loadExternalPlugin(File jar) {
        try {
            URLClassLoader loader = new URLClassLoader(new URL[]{jar.toURI().toURL()}, getClass().getClassLoader());
            java.util.jar.JarFile jf = new java.util.jar.JarFile(jar);
            java.util.jar.Manifest mf = jf.getManifest();
            if (mf == null) { jf.close(); return; }
            String mainClass = mf.getMainAttributes().getValue("Plugin-Class");
            jf.close();
            if (mainClass == null) return;

            Class<?> cls = loader.loadClass(mainClass);
            if (!Plugin.class.isAssignableFrom(cls)) return;

            Plugin plugin = (Plugin) cls.newInstance();
            String id = plugin.getId();
            // Skip if already loaded (e.g. from a previous loadAll call)
            if (plugins.containsKey(id)) return;
            Preferences prefs = Preferences.userNodeForPackage(PluginManager.class);
            boolean enabled = prefs.getBoolean("plugin_" + id, true);
            PluginInfo info = new PluginInfo(id, plugin.getName(), plugin.getVersion(), plugin.getAuthor(), plugin.getDescription(), enabled);
            PluginContext ctx = new PluginContext();
            PluginWrapper pw = new PluginWrapper(plugin, info, ctx);
            pw.jarFile = jar;
            plugins.put(id, pw);
        } catch (Exception e) {
            System.err.println("Failed to load plugin: " + jar.getName() + " - " + e.getMessage());
        }
    }

    private void activate(PluginWrapper w) {
        if (w.loaded) return;
        try {
            w.plugin.init(w.context);
            w.loaded = true;
        } catch (Exception e) {
            System.err.println("Failed to init plugin " + w.info.getName() + ": " + e.getMessage());
        }
    }

    public void enablePlugin(String id) {
        PluginWrapper w = plugins.get(id);
        if (w == null) return;
        w.info.setEnabled(true);
        Preferences.userNodeForPackage(PluginManager.class).putBoolean("plugin_" + id, true);
        if (!w.loaded) activate(w);
    }

    public void disablePlugin(String id) {
        PluginWrapper w = plugins.get(id);
        if (w == null) return;
        w.info.setEnabled(false);
        Preferences.userNodeForPackage(PluginManager.class).putBoolean("plugin_" + id, false);
        if (w.loaded) {
            try { w.plugin.shutdown(); } catch (Exception ignored) { }
            w.loaded = false;
        }
    }

    public List<PluginInfo> getAllPlugins() {
        List<PluginInfo> result = new ArrayList<>();
        for (PluginWrapper w : plugins.values()) result.add(w.info);
        return result;
    }

    public PluginInfo getPluginInfo(String id) {
        PluginWrapper w = plugins.get(id);
        return w != null ? w.info : null;
    }

    public boolean isLoaded(String id) {
        PluginWrapper w = plugins.get(id);
        return w != null && w.loaded;
    }

    public List<PluginContext> getActiveContexts() {
        List<PluginContext> result = new ArrayList<>();
        for (PluginWrapper w : plugins.values()) {
            if (w.loaded) result.add(w.context);
        }
        return result;
    }

    public File getPluginsDir() { return pluginsDir; }

    public boolean isBuiltin(String id) {
        PluginWrapper w = plugins.get(id);
        return w != null && w.jarFile == null;
    }

    public File getPluginJarFile(String id) {
        PluginWrapper w = plugins.get(id);
        return w != null ? w.jarFile : null;
    }

    public File getBuiltinSourceFile(String id) {
        PluginWrapper w = plugins.get(id);
        if (w == null || w.jarFile != null) return null;
        String simpleName = w.plugin.getClass().getSimpleName();
        String appDir = System.getProperty("user.dir");
        File src = new File(appDir, "src/com/eagle/plugin/builtin/" + simpleName + ".java");
        if (src.exists()) return src;
        src = new File(appDir, "src/com/eagle/plugin/external/" + simpleName + ".java");
        if (src.exists()) return src;
        return null;
    }

    public List<File> getBuiltinPluginFiles(String id) {
        PluginWrapper w = plugins.get(id);
        if (w == null || w.jarFile != null) return java.util.Collections.emptyList();
        String simpleName = w.plugin.getClass().getSimpleName();
        String prefix = simpleName;
        // If class ends with "Plugin", also match without the suffix
        // e.g., "NotePadPlugin" → match "NotePad" prefix too → finds NotePadDialog.fxml, NotePadController.java
        if (prefix.endsWith("Plugin") && prefix.length() > 6) {
            prefix = prefix.substring(0, prefix.length() - 6);
        }
        String appDir = System.getProperty("user.dir");
        List<File> result = new ArrayList<>();

        String lowerPrefix = prefix.toLowerCase();

        File builtinDir = new File(appDir, "src/com/eagle/plugin/builtin");
        if (builtinDir.exists()) {
            scanRelatedFiles(builtinDir, lowerPrefix, result);
        }
        File externalDir = new File(appDir, "src/com/eagle/plugin/external");
        if (externalDir.exists()) {
            scanRelatedFiles(externalDir, lowerPrefix, result);
        }
        return result;
    }

    private void scanRelatedFiles(File dir, String lowerPrefix, List<File> result) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isFile() && f.getName().toLowerCase().startsWith(lowerPrefix)) {
                result.add(f);
            }
        }
    }

    public File getBuiltinPluginDir(String id) {
        PluginWrapper w = plugins.get(id);
        if (w == null || w.jarFile != null) return null;
        String simpleName = w.plugin.getClass().getSimpleName();
        String appDir = System.getProperty("user.dir");
        File dir = new File(appDir, "src/com/eagle/plugin/builtin");
        if (dir.exists()) {
            File f = new File(dir, simpleName + ".java");
            if (f.exists()) return dir;
        }
        dir = new File(appDir, "src/com/eagle/plugin/external");
        if (dir.exists()) {
            File f = new File(dir, simpleName + ".java");
            if (f.exists()) return dir;
        }
        return null;
    }

    public boolean deletePlugin(String id) {
        PluginWrapper w = plugins.get(id);
        if (w == null) return false;
        if (w.jarFile != null) {
            // External JAR - delete the file
            if (w.jarFile.delete()) {
                plugins.remove(id);
                return true;
            }
            return false;
        }
        // Built-in - just disable
        disablePlugin(id);
        return true;
    }

    public void removePluginFromList(String id) {
        PluginWrapper w = plugins.get(id);
        if (w != null && w.jarFile != null) {
            plugins.remove(id);
        }
    }
}
