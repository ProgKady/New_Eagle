package com.eagle.util;

import com.eagle.model.ProjectMeta;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Exports a project folder to a single .zip archive, recreating the relative
 * directory structure. Skips the hidden ".eagle-project" marker file so the
 * exported archive is a clean, portable copy of the actual web project.
 */
public class ZipExporter {

    public static void exportDirectory(File sourceDir, File targetZip) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(targetZip);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            Path basePath = sourceDir.toPath();
            addDirectoryToZip(sourceDir, basePath, zos);
        }
    }

    private static void addDirectoryToZip(File dir, Path basePath, ZipOutputStream zos) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isHidden()) continue;
            if (ProjectMeta.isMarkerFile(file)) continue;

            if (file.isDirectory()) {
                addDirectoryToZip(file, basePath, zos);
            } else {
                String relativePath = basePath.relativize(file.toPath()).toString().replace(File.separatorChar, '/');
                ZipEntry entry = new ZipEntry(relativePath);
                zos.putNextEntry(entry);
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                }
                zos.closeEntry();
            }
        }
    }
}
