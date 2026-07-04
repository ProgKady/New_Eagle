package com.eagle.tools;

import com.eagle.util.ThemeManager;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.*;
import java.io.File;

public class ImagePreview {

    public static void show(File imageFile, Window owner) {
        if (imageFile == null || !imageFile.exists()) return;
        try {
            Image img = new Image(imageFile.toURI().toString());
            ImageView iv = new ImageView(img);
            iv.setPreserveRatio(true);
            double w = img.getWidth();
            double h = img.getHeight();
            double maxW = 800, maxH = 600;
            if (w > maxW) { iv.setFitWidth(maxW); }
            if (h > maxH) { iv.setFitHeight(maxH); }

            ScrollPane scroll = new ScrollPane(iv);
            scroll.setPrefSize(maxW + 40, maxH + 40);

            Label info = new Label(String.format("%s  |  %.0f x %.0f px  |  %d KB",
                imageFile.getName(), w, h, imageFile.length() / 1024));

            BorderPane root = new BorderPane();
            root.setCenter(scroll);
            root.setBottom(info);
            BorderPane.setMargin(info, new Insets(8));
            BorderPane.setMargin(scroll, new Insets(8));

            Stage stage = new Stage();
            stage.initOwner(owner);
            stage.setTitle("Image: " + imageFile.getName());
            Scene scene = new Scene(root);
            ThemeManager.getInstance().applyTheme(scene);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Cannot open image: " + e.getMessage());
            a.initOwner(owner); a.showAndWait();
        }
    }

    public static Tooltip createTooltip(String imagePath) {
        try {
            File f = new File(imagePath);
            if (!f.exists()) return null;
            Image img = new Image(f.toURI().toString(), 200, 200, true, true);
            ImageView iv = new ImageView(img);
            Tooltip tp = new Tooltip();
            tp.setGraphic(iv);
            tp.setStyle("-fx-background: transparent; -fx-padding: 4;");
            return tp;
        } catch (Exception e) {
            return null;
        }
    }
}
