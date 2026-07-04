package com.eagle.util;

import java.io.*;
import java.util.*;

public class ToolsConfig {

    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.webide";
    private static final String CONFIG_FILE = CONFIG_DIR + "/tools.properties";

    private static Properties cached;

    // ── Category keys ──
    public static final String[] NODE_KEYS = {"node", "npm", "npx", "yarn", "pnpm"};
    public static final String[] MOBILE_KEYS = {"cordova", "android.sdk", "ionic", "capacitor"};
    public static final String[] JAVA_KEYS = {"java", "javac", "mvn", "gradle"};
    public static final String[] PYTHON_KEYS = {"python", "pip"};
    public static final String[] RUST_KEYS = {"cargo", "rustup"};
    public static final String[] GO_KEYS = {"go"};
    public static final String[] PHP_KEYS = {"php", "composer"};
    public static final String[] FLUTTER_KEYS = {"flutter", "dart"};
    public static final String[] DOTNET_KEYS = {"dotnet"};
    public static final String[] OTHER_KEYS = {"deno", "bun", "hugo", "jekyll", "rails", "quasar", "nativescript", "ns"};

    private static Properties load() {
        if (cached != null) return cached;
        cached = new Properties();
        File f = new File(CONFIG_FILE);
        if (f.exists()) {
            try (FileInputStream in = new FileInputStream(f)) {
                cached.load(in);
            } catch (Exception ignored) {}
        }
        return cached;
    }

    public static void save(Properties props) {
        cached = props;
        new File(CONFIG_DIR).mkdirs();
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "Webide Tool Paths");
        } catch (Exception ignored) {}
    }

    public static String getToolPath(String toolName) {
        Properties p = load();
        String val = p.getProperty(toolName + ".path");
        if (val != null && !val.trim().isEmpty()) {
            val = val.trim();
            File f = new File(val);
            if (f.exists()) {
                if (f.isFile()) return val;
                if (f.isDirectory()) {
                    String[] exts = {".exe", ".cmd", ".bat", ""};
                    for (String ext : exts) {
                        File exe = new File(f, toolName + ext);
                        if (exe.exists() && exe.isFile()) return exe.getAbsolutePath();
                    }
                }
            }
        }
        return null;
    }

    public static Map<String, String> getAllPaths() {
        Properties p = load();
        Map<String, String> map = new LinkedHashMap<>();
        for (String key : p.stringPropertyNames()) {
            if (key.endsWith(".path")) {
                map.put(key.replace(".path", ""), p.getProperty(key, ""));
            }
        }
        return map;
    }

    public static String getDocUrl(String toolName) {
        switch (toolName) {
            case "node": case "npm": case "npx": case "yarn": case "pnpm":
                return "https://nodejs.org";
            case "cordova":
                return "https://cordova.apache.org";
            case "android.sdk":
                return "https://developer.android.com/studio";
            case "ionic":
                return "https://ionicframework.com";
            case "capacitor":
                return "https://capacitorjs.com";
            case "java": case "javac":
                return "https://adoptium.net";
            case "mvn":
                return "https://maven.apache.org/download.cgi";
            case "gradle":
                return "https://gradle.org/install";
            case "python": case "pip":
                return "https://www.python.org/downloads";
            case "cargo": case "rustup":
                return "https://rustup.rs";
            case "go":
                return "https://go.dev/dl";
            case "php": case "composer":
                return "https://windows.php.net/download";
            case "flutter": case "dart":
                return "https://flutter.dev";
            case "dotnet":
                return "https://dotnet.microsoft.com/download";
            case "deno":
                return "https://deno.land";
            case "bun":
                return "https://bun.sh";
            case "hugo":
                return "https://gohugo.io";
            case "jekyll":
                return "https://jekyllrb.com";
            case "rails":
                return "https://rubyonrails.org";
            case "quasar":
                return "https://quasar.dev";
            case "nativescript": case "ns":
                return "https://nativescript.org";
            default:
                return "https://www.google.com/search?q=install+" + toolName;
        }
    }
}
