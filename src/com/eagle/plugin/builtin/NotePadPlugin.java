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

/**
 * NotePad Plugin — example of a plugin that uses FXML + Controller
 * 
 * خلاصة الكلام:
 *   - FXML ملف جوا الـ JAR
 *   - Controller كلاس جوا الـ JAR
 *   - التحميل عن طريق getClass().getResource("NotePadDialog.fxml")
 *   - الـ ClassLoader بتاع الـ JAR هو اللي بيجيبهم
 */
public class NotePadPlugin implements Plugin {

    @Override
    public String getId() { return "notepad"; }

    @Override
    public String getName() { return "NotePad"; }

    @Override
    public String getVersion() { return "1.0.0"; }

    @Override
    public String getAuthor() { return "Eagle IDE"; }

    @Override
    public String getDescription() { return "Demo: plugin with FXML + Controller UI."; }

    @Override
    public void init(PluginContext ctx) {
        // Command Palette
        ctx.registerCommand("Open NotePad", "Tools", () -> openNotePad());

        // Toolbar
        Button btn = new Button("N");
        btn.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #00b894;");
        btn.setTooltip(new Tooltip("NotePad Plugin (FXML demo)"));
        btn.setOnAction(e -> openNotePad());
        ctx.registerToolbarItem("NotePad", btn);

        // Menu
        ctx.registerMenuItem("Tools", "NotePad", e -> openNotePad());
    }

    @Override
    public void shutdown() {}

    private void openNotePad() {
        try {
            // load FXML من نفس الـ package بتاع البلاجن
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("NotePadDialog.fxml")
            );
            Parent root = loader.load();
            NotePadController ctrl = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("NotePad - Plugin Demo");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setMinWidth(500);
            stage.setMinHeight(400);
            Scene scene = new Scene(root, 550, 450);
            com.eagle.util.ThemeManager.getInstance().applyTheme(scene);
            stage.setScene(scene);
            ctrl.setStage(stage);
            stage.showAndWait();
        } catch (Exception e) {
            System.err.println("Failed to load NotePad FXML: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
