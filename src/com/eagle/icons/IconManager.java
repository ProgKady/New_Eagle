package com.eagle.icons;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

/**
 * IconManager — loads SVG icons from Material Design Outlined set,
 * renders them to Images via IconRenderer, and caches results.
 *
 * Set SVG_DIR system property or change SVG_BASE to point to your
 * Material Design icons folder.
 */
public class IconManager {

    private static final double DEFAULT_SIZE = 18.0;
    private static final Color DEFAULT_COLOR = Color.web("#555555");
    private static final Color DARK_COLOR = Color.web("#cccccc");

    // Load SVGs from classpath (/icons/ folder in src/icons/)
    private static final String SVG_DIR = "/icons/";
    // Fallback filesystem path (for development when SVGs aren't on classpath)
    private static final String SVG_FS_DIR;
    static {
        String base = System.getProperty("user.dir");
        File f = new File(base, "src/icons");
        if (!f.isDirectory()) {
            f = new File(base, "../src/icons");
        }
        SVG_FS_DIR = f.getAbsolutePath() + File.separator;
    }

    // Cache: icon name → SVG path data (the d="" attribute)
    private static final Map<String, String> svgPathCache = new ConcurrentHashMap<>();
    // Rendered image cache: key → Image
    private static final Map<String, Image> imageCache = new ConcurrentHashMap<>();

    // ── Mapping: our icon constant → Material SVG filename (without .svg) ──
    private static final Map<String, String> ICON_MAP = new HashMap<>();
    static {
        ICON_MAP.put("folder", "folder");
        ICON_MAP.put("file", "description");
        ICON_MAP.put("file-default", "description");
        ICON_MAP.put("file-html", "code");
        ICON_MAP.put("file-css", "css");
        ICON_MAP.put("file-js", "javascript");
        ICON_MAP.put("file-json", "data_object");
        ICON_MAP.put("file-md", "article");
        ICON_MAP.put("file-py", "python");
        ICON_MAP.put("file-java", "java");
        ICON_MAP.put("file-xml", "code");
        ICON_MAP.put("file-img", "image");
        ICON_MAP.put("file-video", "videocam");
        ICON_MAP.put("file-audio", "audiotrack");
        ICON_MAP.put("file-pdf", "picture_as_pdf");
        ICON_MAP.put("file-zip", "folder_zip");
        ICON_MAP.put("file-sql", "table_chart");
        ICON_MAP.put("file-sh", "terminal");
        ICON_MAP.put("file-git", "code_off");
        ICON_MAP.put("file-text", "article");
        ICON_MAP.put("file-code", "code");

        ICON_MAP.put("save", "save");
        ICON_MAP.put("save-as", "save_as");
        ICON_MAP.put("settings", "settings");
        ICON_MAP.put("terminal", "terminal");
        ICON_MAP.put("git", "call_split");
        ICON_MAP.put("bug", "bug_report");
        ICON_MAP.put("search", "search");
        ICON_MAP.put("play", "play_arrow");
        ICON_MAP.put("robot", "smart_toy");
        ICON_MAP.put("database", "storage");
        ICON_MAP.put("package", "inventory_2");
        ICON_MAP.put("android", "android");
        ICON_MAP.put("cloud", "cloud_upload");
        ICON_MAP.put("home", "home");
        ICON_MAP.put("refresh", "refresh");
        ICON_MAP.put("plus", "add");
        ICON_MAP.put("close", "close");
        ICON_MAP.put("export", "file_upload");
        ICON_MAP.put("download", "download");
        ICON_MAP.put("undo", "undo");
        ICON_MAP.put("redo", "redo");
        ICON_MAP.put("cut", "content_cut");
        ICON_MAP.put("copy", "content_copy");
        ICON_MAP.put("paste", "content_paste");
        ICON_MAP.put("new-file", "note_add");
        ICON_MAP.put("new-folder", "create_new_folder");
        ICON_MAP.put("menu", "menu");
        ICON_MAP.put("edit", "edit");
        ICON_MAP.put("delete", "delete");
        ICON_MAP.put("rename", "drive_file_rename_outline");
        ICON_MAP.put("duplicate", "file_copy");
        ICON_MAP.put("zip", "folder_zip");
        ICON_MAP.put("lightbulb", "lightbulb");
        ICON_MAP.put("note", "note");
        ICON_MAP.put("bold", "format_bold");
        ICON_MAP.put("italic", "format_italic");
        ICON_MAP.put("underline", "format_underline");
        ICON_MAP.put("open-folder", "folder_open");
        ICON_MAP.put("new-window", "open_in_new");
        ICON_MAP.put("show-in-folder", "folder_open");
        ICON_MAP.put("project-info", "info");
        ICON_MAP.put("archive", "archive");
        ICON_MAP.put("pause", "pause");
        ICON_MAP.put("stop", "stop");
        ICON_MAP.put("icon-bug", "bug_report");
        ICON_MAP.put("icon-robot", "smart_toy");
        ICON_MAP.put("icon-search", "search");
        ICON_MAP.put("icon-plus", "add");
        ICON_MAP.put("icon-close", "close");
        ICON_MAP.put("icon-edit", "edit");
        ICON_MAP.put("icon-delete", "delete");
        ICON_MAP.put("icon-download", "download");
        ICON_MAP.put("icon-upload", "upload");
        ICON_MAP.put("icon-export", "file_upload");
        ICON_MAP.put("icon-home", "home");
        ICON_MAP.put("icon-database", "storage");
        ICON_MAP.put("icon-cloud", "cloud_upload");
        ICON_MAP.put("icon-lightbulb", "lightbulb");
        ICON_MAP.put("icon-note", "note");
        ICON_MAP.put("icon-menu", "menu");
        ICON_MAP.put("icon-warning", "warning");
        ICON_MAP.put("icon-info", "info");
        ICON_MAP.put("icon-theme-toggle", "dark_mode");
        ICON_MAP.put("icon-android", "android");

        // Severity
        ICON_MAP.put("severity-error", "error");
        ICON_MAP.put("severity-warning", "warning");
        ICON_MAP.put("severity-info", "info");
        ICON_MAP.put("severity-hint", "lightbulb");

        // Task status
        ICON_MAP.put("task-pending", "radio_button_unchecked");
        ICON_MAP.put("task-running", "sync");
        ICON_MAP.put("task-completed", "check_circle");
        ICON_MAP.put("task-failed", "cancel");
        ICON_MAP.put("task-skipped", "remove_circle_outline");

        // Builder components
        ICON_MAP.put("comp-text", "text_fields");
        ICON_MAP.put("comp-button", "smart_button");
        ICON_MAP.put("comp-form", "checklist");
        ICON_MAP.put("comp-media", "image");
        ICON_MAP.put("comp-layout", "view_module");
        ICON_MAP.put("comp-data", "table_chart");
        ICON_MAP.put("comp-feedback", "announcement");
        ICON_MAP.put("comp-indicator", "info");
        ICON_MAP.put("comp-advanced", "tune");

        // Code completion
        ICON_MAP.put("compl-tag", "code");
        ICON_MAP.put("compl-keyword", "key");
        ICON_MAP.put("compl-property", "settings");
        ICON_MAP.put("compl-snippet", "content_paste");
        ICON_MAP.put("compl-function", "functions");
        ICON_MAP.put("compl-method", "functions");
        ICON_MAP.put("compl-variable", "data_object");
        ICON_MAP.put("compl-type", "category");
        ICON_MAP.put("compl-operator", "calculate");
        ICON_MAP.put("compl-builtin", "build");
        ICON_MAP.put("compl-value", "palette");

        // Media
        ICON_MAP.put("media-play", "play_arrow");
        ICON_MAP.put("media-pause", "pause");
        ICON_MAP.put("media-stop", "stop");
        ICON_MAP.put("media-prev", "skip_previous");
        ICON_MAP.put("media-next", "skip_next");

        // Toolbar
        ICON_MAP.put("toolbar-save", "save");
        ICON_MAP.put("toolbar-undo", "undo");
        ICON_MAP.put("toolbar-find", "search");
        ICON_MAP.put("toolbar-preview", "visibility");
        ICON_MAP.put("toolbar-ai", "smart_toy");
        ICON_MAP.put("toolbar-terminal", "terminal");
        ICON_MAP.put("toolbar-git", "call_split");
        ICON_MAP.put("toolbar-debug", "bug_report");
        ICON_MAP.put("toolbar-quick-apk", "android");
        ICON_MAP.put("toolbar-settings", "settings");
        ICON_MAP.put("toolbar-browse", "folder_open");
        ICON_MAP.put("toolbar-new-proj", "create_new_folder");
        ICON_MAP.put("toolbar-refresh", "refresh");
        ICON_MAP.put("toolbar-new-file", "note_add");
        ICON_MAP.put("toolbar-new-folder", "create_new_folder");
        ICON_MAP.put("toolbar-redo", "redo");
        ICON_MAP.put("toolbar-copy", "content_copy");
        ICON_MAP.put("toolbar-paste", "content_paste");
        ICON_MAP.put("toolbar-align-left", "format_align_left");
        ICON_MAP.put("toolbar-align-center", "format_align_center");
        ICON_MAP.put("toolbar-align-right", "format_align_right");

        // Sidebar
        ICON_MAP.put("sidebar-explorer", "folder");
        ICON_MAP.put("sidebar-preview", "visibility");
        ICON_MAP.put("sidebar-close", "close");

        // Status
        ICON_MAP.put("status-ready", "check_circle");
        ICON_MAP.put("status-build-ok", "check_circle");
        ICON_MAP.put("status-build-fail", "cancel");

        // Welcome
        ICON_MAP.put("welcome-recent", "history");
        ICON_MAP.put("welcome-new-proj", "create_new_folder");
        ICON_MAP.put("welcome-open-proj", "folder_open");
        ICON_MAP.put("welcome-template", "dashboard");

        // Toggle
        ICON_MAP.put("toggle-on", "toggle_on");
        ICON_MAP.put("toggle-off", "toggle_off");
    }

    /** Load SVG path data from classpath /icons/, cache it */
    private static String loadSvgPath(String iconKey) {
        String cached = svgPathCache.get(iconKey);
        if (cached != null) return cached;

        String matName = ICON_MAP.get(iconKey);
        if (matName == null) matName = iconKey;

        // Try loading from classpath
        String pathData = tryLoadSvg(matName);
        if (pathData == null && !matName.equals(iconKey)) {
            pathData = tryLoadSvg(iconKey);
        }
        if (pathData != null) {
            svgPathCache.put(iconKey, pathData);
        }
        return pathData;
    }

    private static String tryLoadSvg(String name) {
        // Try classpath first (works when SVGs are in JAR or build output)
        String resPath = SVG_DIR + name + ".svg";
        try (InputStream is = IconManager.class.getResourceAsStream(resPath)) {
            if (is != null) {
                return extractPathFromSvg(is);
            }
        } catch (Exception e) {
            // Fall through to filesystem
        }
        // Fallback: try filesystem (development mode)
        File fsFile = new File(SVG_FS_DIR + name + ".svg");
        if (fsFile.isFile()) {
            try (InputStream is = new FileInputStream(fsFile)) {
                return extractPathFromSvg(is);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private static String extractPathFromSvg(InputStream is) throws IOException {
        byte[] bytes = readAllBytes(is);
        String content = new String(bytes, "UTF-8");
        int dIdx = content.indexOf("d=\"");
        if (dIdx < 0) return null;
        dIdx += 3;
        int end = content.indexOf('"', dIdx);
        if (end < 0) return null;
        return content.substring(dIdx, end);
    }

    private static byte[] readAllBytes(InputStream is) throws IOException {
        byte[] buf = new byte[8192];
        int pos = 0;
        int read;
        while ((read = is.read(buf, pos, buf.length - pos)) >= 0) {
            pos += read;
            if (pos == buf.length) {
                byte[] n = new byte[buf.length * 2];
                System.arraycopy(buf, 0, n, 0, buf.length);
                buf = n;
            }
        }
        byte[] result = new byte[pos];
        System.arraycopy(buf, 0, result, 0, pos);
        return result;
    }

    /** Render an SVG icon to Image */
    private static Image renderIcon(String iconKey, int size, boolean dark) {
        String pathData = loadSvgPath(iconKey);
        if (pathData == null) return null;

        Color color = dark ? DARK_COLOR : DEFAULT_COLOR;
        return IconRenderer.render(pathData, size, color);
    }

    private static ImageView makeView(String name) {
        return makeView(name, DEFAULT_SIZE);
    }

    private static ImageView makeView(String name, double size) {
        boolean dark = isDarkTheme();
        Image img = renderIcon(name, (int) size, dark);
        if (img == null) return null;
        ImageView iv = new ImageView(img);
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setPreserveRatio(true);
        return iv;
    }

    private static boolean isDarkTheme() {
        try {
            return com.eagle.util.ThemeManager.getInstance().isDark();
        } catch (Exception e) {
            return false;
        }
    }

    // ── Public API ──

    public static ImageView fileTypeImageView(String fileName) {
        return fileTypeImageView(fileName, 16);
    }

    public static ImageView fileTypeImageView(String fileName, int size) {
        String key = fileTypeIconKey(fileName);
        return imageView(key, size);
    }

    public static String fileTypeIconKey(String fileName) {
        if (fileName == null) return "file-default";
        String lower = fileName.toLowerCase();
        if (fileName.indexOf('.') < 0) return "folder";
        String ext = lower.substring(lower.lastIndexOf('.'));
        switch (ext) {
            case ".html": case ".htm": return "file-html";
            case ".css": case ".scss": case ".sass": case ".less": return "file-css";
            case ".js": case ".mjs": case ".cjs": case ".ts": case ".go": case ".swift": return "file-js";
            case ".jsx": case ".tsx": case ".rs": case ".cpp": case ".c": case ".h": case ".hpp": case ".cs": return "file-code";
            case ".json": return "file-json";
            case ".md": case ".markdown": return "file-md";
            case ".php": case ".yml": case ".yaml": case ".xml": return "file-xml";
            case ".py": case ".rb": return "file-py";
            case ".sql": return "file-sql";
            case ".sh": case ".bash": case ".bat": case ".cmd": return "file-sh";
            case ".gitignore": case ".gitattributes": case ".gitmodules": return "file-git";
            case ".env": case ".txt": return "file-text";
            case ".vue": case ".svelte": return "file-html";
            case ".svg": case ".png": case ".jpg": case ".jpeg": case ".gif": case ".webp": case ".ico": return "file-img";
            case ".mp4": case ".webm": case ".mov": case ".avi": return "file-video";
            case ".mp3": case ".wav": case ".aac": case ".ogg": return "file-audio";
            case ".pdf": return "file-pdf";
            case ".zip": case ".tar": case ".gz": case ".rar": case ".7z": return "file-zip";
            case ".java": case ".kt": case ".kts": return "file-java";
            default: return "file-default";
        }
    }

    public static ImageView severityImageView(String severity, int size) {
        String key;
        switch (severity.toLowerCase()) {
            case "error": key = "severity-error"; break;
            case "warning": key = "severity-warning"; break;
            case "hint": key = "severity-hint"; break;
            default: key = "severity-info";
        }
        return imageView(key, size);
    }

    public static ImageView taskStatusImageView(String status, int size) {
        String key;
        switch (status.toLowerCase()) {
            case "pending": key = "task-pending"; break;
            case "running": key = "task-running"; break;
            case "completed": key = "task-completed"; break;
            case "failed": key = "task-failed"; break;
            default: key = "task-skipped";
        }
        return imageView(key, size);
    }

    /** Generic: render any icon key to ImageView */
    public static ImageView imageView(String iconKey, int size) {
        String name = iconKey;
        if (name != null && name.endsWith(".png")) {
            name = name.substring(0, name.length() - 4);
        }
        return makeView(name, size);
    }

    // ── Backward-compatible convenience methods (same signatures) ──
    public static Node icon(String pathData) { return makeView("folder"); }
    public static Node icon(String pathData, double size) { return makeView("folder", size); }
    public static Node icon(String pathData, double size, javafx.scene.paint.Paint fill) { return makeView("folder", size); }
    public static Node iconDark(String pathData, double size) { return makeView("folder", size); }

    public static Node folderIcon() { return makeView("folder"); }
    public static Node fileIcon() { return makeView("file"); }
    public static Node saveIcon() { return makeView("save"); }
    public static Node settingsIcon() { return makeView("settings"); }
    public static Node terminalIcon() { return makeView("terminal"); }
    public static Node gitIcon() { return makeView("git"); }
    public static Node bugIcon() { return makeView("bug"); }
    public static Node searchIcon() { return makeView("search"); }
    public static Node playIcon() { return makeView("play"); }
    public static Node robotIcon() { return makeView("robot"); }
    public static Node fileCodeIcon() { return makeView("file-code"); }
    public static Node databaseIcon() { return makeView("database"); }
    public static Node packageIcon() { return makeView("package"); }
    public static Node androidIcon() { return makeView("android"); }
    public static Node cloudUploadIcon() { return makeView("cloud"); }
    public static Node deployIcon() { return makeView("cloud"); }
    public static Node homeIcon() { return makeView("home"); }
    public static Node zipIcon() { return makeView("zip"); }
    public static Node refreshIcon() { return makeView("refresh"); }
    public static Node plusIcon() { return makeView("plus"); }
    public static Node closeIcon() { return makeView("close"); }
    public static Node exportIcon() { return makeView("export"); }
    public static Node downloadIcon() { return makeView("download"); }
    public static Node menuIcon() { return makeView("menu"); }
    public static Node undoIcon() { return makeView("undo"); }
    public static Node redoIcon() { return makeView("redo"); }
    public static Node cutIcon() { return makeView("cut"); }
    public static Node copyIcon() { return makeView("copy"); }
    public static Node pasteIcon() { return makeView("paste"); }
    public static Node newFileIcon() { return makeView("new-file"); }
    public static Node newFolderIcon() { return makeView("new-folder"); }

    // ── Keep string constants for backward compat (ComponentType references them) ──
    public static final String FOLDER = "folder";
    public static final String FILE = "file";
    public static final String SAVE = "save";
    public static final String SETTINGS = "settings";
    public static final String TERMINAL = "terminal";
    public static final String GIT_BRANCH = "git";
    public static final String BUG = "bug";
    public static final String SEARCH = "search";
    public static final String PLAY = "play";
    public static final String ROBOT = "robot";
    public static final String FILE_CODE = "file-code";
    public static final String UNDO = "undo";
    public static final String REDO = "redo";
    public static final String CUT = "cut";
    public static final String COPY = "copy";
    public static final String PASTE = "paste";
    public static final String PLUS = "plus";
    public static final String CLOSE = "close";
    public static final String DATABASE = "database";
    public static final String PACKAGE = "package";
    public static final String ANDROID = "android";
    public static final String CLOUD_UPLOAD = "cloud";
    public static final String HOME = "home";
    public static final String REFRESH = "refresh";
    public static final String EXPORT = "export";
    public static final String DOWNLOAD = "download";
    public static final String EDIT = "edit";
    public static final String DELETE = "delete";
    public static final String RENAME = "rename";
    public static final String DUPLICATE = "duplicate";
    public static final String NEW_FILE = "new-file";
    public static final String NEW_FOLDER = "new-folder";
    public static final String SAVE_AS = "save-as";
    public static final String LIGHTBULB = "lightbulb";
    public static final String NOTE = "note";
    public static final String MENU = "menu";
    public static final String ZIP = "zip";
    public static final String BOLD = "bold";
    public static final String ITALIC = "italic";
    public static final String UNDERLINE = "underline";
    public static final String OPEN_FOLDER = "open-folder";
    public static final String NEW_WINDOW = "new-window";
    public static final String SHOW_IN_FOLDER = "show-in-folder";
    public static final String PROJECT_INFO = "project-info";
    public static final String ARCHIVE = "archive";
    public static final String SEVERITY_ERROR = "severity-error";
    public static final String SEVERITY_WARN = "severity-warning";
    public static final String SEVERITY_INFO = "severity-info";
    public static final String SEVERITY_HINT = "severity-hint";
    public static final String TEXT_TOOL = "comp-text";
    public static final String BUTTON_TOOL = "comp-button";
    public static final String FORM_TOOL = "comp-form";
    public static final String MEDIA_TOOL = "comp-media";
    public static final String LAYOUT_TOOL = "comp-layout";
    public static final String DATA_TOOL = "comp-data";
    public static final String FEEDBACK_TOOL = "comp-feedback";
    public static final String INDICATOR_TOOL = "comp-indicator";
    public static final String ADVANCED_TOOL = "comp-advanced";
    public static final String TAG = "compl-tag";
    public static final String KEYWORD = "compl-keyword";
    public static final String PROPERTY = "compl-property";
    public static final String SNIPPET = "compl-snippet";
    public static final String FUNCTION = "compl-function";
    public static final String METHOD = "compl-method";
    public static final String VARIABLE = "compl-variable";
    public static final String TYPE = "compl-type";
    public static final String OPERATOR = "compl-operator";
    public static final String BUILTIN = "compl-builtin";
    public static final String VALUE = "compl-value";
    public static final String PAUSE = "pause";
    public static final String MEDIA_STOP = "stop";
    public static final String TASK_PENDING = "task-pending";
    public static final String TASK_RUNNING = "task-running";
    public static final String TASK_DONE = "task-completed";
    public static final String TASK_FAILED = "task-failed";
    public static final String TASK_SKIPPED = "task-skipped";
    public static final String FILE_HTML = "file-html";
    public static final String FILE_CSS = "file-css";
    public static final String FILE_JS = "file-js";
    public static final String FILE_JSON = "file-json";
    public static final String FILE_MD = "file-md";
    public static final String FILE_PY = "file-py";
    public static final String FILE_JAVA = "file-java";
    public static final String FILE_XML = "file-xml";
    public static final String FILE_IMG = "file-img";
    public static final String FILE_VIDEO = "file-video";
    public static final String FILE_AUDIO = "file-audio";
    public static final String FILE_PDF = "file-pdf";
    public static final String FILE_ZIP = "file-zip";
    public static final String FILE_SQL = "file-sql";
    public static final String FILE_SH = "file-sh";
    public static final String FILE_GIT = "file-git";
    public static final String FILE_TEXT = "file-text";
    public static final String FILE_DEFAULT = "file-default";
}