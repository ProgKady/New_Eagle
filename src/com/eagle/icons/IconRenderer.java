package com.eagle.icons;

import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Scale;
import java.util.HashMap;
import java.util.Map;

public class IconRenderer {

    private static final Map<String, Image> cache = new HashMap<>();

    public static Image render(String svgPathData, int size) {
        String key = svgPathData + "@" + size;
        Image cached = cache.get(key);
        if (cached != null) return cached;

        try {
            SVGPath path = new SVGPath();
            path.setContent(svgPathData);

            double viewSize = 24.0;
            double scale = size / viewSize;

            path.setScaleX(scale);
            path.setScaleY(scale);
            path.setFill(Color.web("#555555"));
            path.setStroke(Color.web("#555555"));

            WritableImage img = new WritableImage(size, size);
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            path.snapshot(params, img);

            cache.put(key, img);
            return img;
        } catch (Exception e) {
            return null;
        }
    }

    public static Image render(String svgPathData, int size, Color fill) {
        String key = svgPathData + "@" + size + "@" + fill.hashCode();
        Image cached = cache.get(key);
        if (cached != null) return cached;

        try {
            SVGPath path = new SVGPath();
            path.setContent(svgPathData);

            double viewSize = 24.0;
            double scale = size / viewSize;

            path.setScaleX(scale);
            path.setScaleY(scale);
            path.setFill(fill);
            path.setStroke(fill);

            WritableImage img = new WritableImage(size, size);
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            path.snapshot(params, img);

            cache.put(key, img);
            return img;
        } catch (Exception e) {
            return null;
        }
    }

    public static void clearCache() {
        cache.clear();
    }
}