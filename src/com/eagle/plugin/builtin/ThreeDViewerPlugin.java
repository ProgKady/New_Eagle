package com.eagle.plugin.builtin;

import com.eagle.plugin.Plugin;
import com.eagle.plugin.PluginContext;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ThreeDViewerPlugin implements Plugin {

    @Override
    public String getId() { return "threedviewer"; }

    @Override
    public String getName() { return "3D Viewer"; }

    @Override
    public String getVersion() { return "1.0.0"; }

    @Override
    public String getAuthor() { return "Ahmed Elkady"; }

    @Override
    public String getDescription() { return "View, test, and control 3D objects (glTF/OBJ/STL) with Three.js"; }

    @Override
    public void init(PluginContext ctx) {
        ctx.registerCommand("Open 3D Viewer", "Tools", () -> openViewer());

        Button btn = new Button("3D");
        btn.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #8b5cf6;");
        btn.setTooltip(new Tooltip("3D Object Viewer — load & control 3D models"));
        btn.setOnAction(e -> openViewer());
        ctx.registerToolbarItem("3D Viewer", btn);

        ctx.registerMenuItem("Tools", "3D Viewer", e -> openViewer());
    }

    @Override
    public void shutdown() {}

    private void openViewer() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("ThreeDViewerDialog.fxml")
            );
            Parent root = loader.load();
            ThreeDViewerController ctrl = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("3D Object Viewer");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setMinWidth(700);
            stage.setMinHeight(500);
            Scene scene = new Scene(root, 900, 620);
            com.eagle.util.ThemeManager.getInstance().applyTheme(scene);
            stage.setScene(scene);
            ctrl.setStage(stage);
            stage.showAndWait();
        } catch (Exception e) {
            System.err.println("Failed to load ThreeDViewer FXML: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
