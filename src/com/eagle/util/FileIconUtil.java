package com.eagle.util;

import com.eagle.icons.IconManager;
import javafx.scene.image.ImageView;
import java.io.File;

public class FileIconUtil {

    public static String iconPathFor(File file) {
        if (file.isDirectory()) return "folder";
        return IconManager.fileTypeIconKey(file.getName());
    }

    public static ImageView iconViewFor(File file) {
        return IconManager.fileTypeImageView(file.getName());
    }

    public static ImageView iconViewFor(File file, int size) {
        return IconManager.fileTypeImageView(file.getName(), size);
    }

    public static boolean isEditable(File file) {
        if (file.isDirectory()) return false;
        String name = file.getName().toLowerCase();
        String[] textExt = {
            ".html", ".htm", ".css", ".js", ".mjs", ".json", ".md", ".txt", ".xml",
            ".java", ".ts", ".yml", ".yaml", ".php", ".py", ".sql", ".scss", ".sass",
            ".less", ".sh", ".bash", ".zsh", ".bat", ".cmd", ".jsx", ".tsx",
            ".gitignore", ".gitattributes", ".gitmodules", ".env", ".dockerfile",
            ".vue", ".svelte", ".svg",
            ".fxml", ".properties", ".dat",
        };
        for (String ext : textExt) {
            if (name.endsWith(ext)) return true;
        }
        return false;
    }
}