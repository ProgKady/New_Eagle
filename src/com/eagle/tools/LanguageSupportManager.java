package com.eagle.tools;

import java.io.*;
import java.util.*;

public class LanguageSupportManager {

    public static class LangConfig {
        public final String name;
        public final String runtimeCheck;
        public final String runtimeUrl;
        public final String linterCheck;
        public final String linterInstall;
        public final String formatterCheck;
        public final String formatterInstall;
        public boolean runtimeFound;
        public boolean linterFound;
        public boolean formatterFound;
        public boolean enabled;
        public String runtimePath = "";
        public String linterPath = "";
        public String formatterPath = "";

        public LangConfig(String name, String runtimeCheck, String runtimeUrl,
                          String linterCheck, String linterInstall,
                          String formatterCheck, String formatterInstall) {
            this.name = name;
            this.runtimeCheck = runtimeCheck;
            this.runtimeUrl = runtimeUrl;
            this.linterCheck = linterCheck;
            this.linterInstall = linterInstall;
            this.formatterCheck = formatterCheck;
            this.formatterInstall = formatterInstall;
        }
    }

    private static final Map<String, LangConfig> LANGUAGES = new LinkedHashMap<>();
    private static final File CONFIG_DIR = new File(System.getProperty("user.home") + "/.webide");
    private static final File CONFIG_FILE = new File(CONFIG_DIR, "languages.properties");

    static {
        register("Python", "python --version", "https://python.org/downloads/",
            "pylint --version", "pip install pylint",
            "black --version", "pip install black");
        register("Node.js", "node --version", "https://nodejs.org/",
            "eslint --version", "npm install -g eslint",
            "prettier --version", "npm install -g prettier");
        register("Java", "java -version", "https://adoptium.net/",
            "checkstyle --version", "Download from https://checkstyle.org/",
            "", "");
        register("C/C++", "gcc --version", "https://gcc.gnu.org/",
            "cppcheck --version", "Install cppcheck via package manager",
            "clang-format --version", "Install clang-format via package manager");
        register("Go", "go version", "https://go.dev/dl/",
            "golint --version", "go install golang.org/x/lint/golint@latest",
            "gofmt --version", "gofmt is built into Go");
        register("Rust", "rustc --version", "https://rustup.rs/",
            "clippy-driver --version", "rustup component add clippy",
            "rustfmt --version", "rustup component add rustfmt");
        register("Dart/Flutter", "dart --version", "https://dart.dev/get-dart",
            "dartanalyzer --version", "Built into Dart SDK",
            "dartfmt --version", "Built into Dart SDK");
        register("PHP", "php --version", "https://php.net/downloads.php",
            "phpcs --version", "composer global require squizlabs/php_codesniffer",
            "php-cs-fixer --version", "composer global require friendsofphp/php-cs-fixer");
        register("Ruby", "ruby --version", "https://ruby-lang.org/en/downloads/",
            "rubocop --version", "gem install rubocop",
            "rufo --version", "gem install rufo");
        register("TypeScript", "tsc --version", "https://typescriptlang.org/download",
            "tslint --version", "npm install -g tslint",
            "prettier --version", "npm install -g prettier");
        register("SQL", "", "",
            "", "",
            "", "");
        register("Kotlin", "kotlin -version", "https://kotlinlang.org/",
            "ktlint --version", "Install ktlint via package manager",
            "", "");
        register("Swift", "swift --version", "https://swift.org/download/",
            "swiftlint --version", "brew install swiftlint",
            "swiftformat --version", "brew install swiftformat");
    }

    private static void register(String name, String runtimeCheck, String runtimeUrl,
                                 String linterCheck, String linterInstall,
                                 String formatterCheck, String formatterInstall) {
        LANGUAGES.put(name, new LangConfig(name, runtimeCheck, runtimeUrl,
            linterCheck, linterInstall, formatterCheck, formatterInstall));
    }

    public static Collection<LangConfig> getAll() { return LANGUAGES.values(); }

    public static LangConfig get(String name) { return LANGUAGES.get(name); }

    public static void detectAll() {
        for (LangConfig c : LANGUAGES.values()) {
            detectRuntime(c);
            detectLinter(c);
            detectFormatter(c);
        }
    }

    public static void detectRuntime(LangConfig c) {
        if (c.runtimeCheck.isEmpty()) { c.runtimeFound = true; return; }
        try {
            ProcessBuilder pb = new ProcessBuilder(c.runtimeCheck.split(" "));
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
            c.runtimeFound = p.exitValue() == 0;
            if (c.runtimeFound) {
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String first = br.readLine();
                if (first != null) c.runtimePath = first.trim();
            }
        } catch (Exception e) {
            c.runtimeFound = false;
        }
    }

    public static void detectLinter(LangConfig c) {
        if (c.linterCheck.isEmpty()) { c.linterFound = true; return; }
        try {
            ProcessBuilder pb = new ProcessBuilder(c.linterCheck.split(" "));
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
            c.linterFound = p.exitValue() == 0;
            if (c.linterFound) {
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String first = br.readLine();
                if (first != null) c.linterPath = first.trim();
            }
        } catch (Exception e) {
            c.linterFound = false;
        }
    }

    public static void detectFormatter(LangConfig c) {
        if (c.formatterCheck.isEmpty()) { c.formatterFound = true; return; }
        try {
            ProcessBuilder pb = new ProcessBuilder(c.formatterCheck.split(" "));
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
            c.formatterFound = p.exitValue() == 0;
            if (c.formatterFound) {
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String first = br.readLine();
                if (first != null) c.formatterPath = first.trim();
            }
        } catch (Exception e) {
            c.formatterFound = false;
        }
    }

    public static void loadSettings() {
        if (!CONFIG_FILE.exists()) return;
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(CONFIG_FILE)) { p.load(in); } catch (Exception ignored) { return; }
        for (LangConfig c : LANGUAGES.values()) {
            c.enabled = Boolean.parseBoolean(p.getProperty(c.name + ".enabled", "true"));
            c.linterPath = p.getProperty(c.name + ".linterPath", "");
            c.formatterPath = p.getProperty(c.name + ".formatterPath", "");
        }
    }

    public static void saveSettings() {
        CONFIG_DIR.mkdirs();
        Properties p = new Properties();
        for (LangConfig c : LANGUAGES.values()) {
            p.setProperty(c.name + ".enabled", String.valueOf(c.enabled));
            p.setProperty(c.name + ".linterPath", c.linterPath);
            p.setProperty(c.name + ".formatterPath", c.formatterPath);
        }
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            p.store(out, "Language Support Settings");
        } catch (Exception ignored) {}
    }

    public static void installLanguage(String langName) {
        LangConfig c = get(langName);
        if (c == null) return;
        if (!c.runtimeFound && !c.runtimeUrl.isEmpty()) {
            openUrl(c.runtimeUrl);
        }
    }

    private static void openUrl(String url) {
        try {
            if (java.awt.Desktop.isDesktopSupported())
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception ignored) {}
    }
}
