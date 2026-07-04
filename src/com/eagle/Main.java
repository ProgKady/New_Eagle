package com.eagle;

import com.eagle.controller.EditorController;
import com.eagle.model.ProjectMeta;
import com.eagle.model.ProjectType;
import com.eagle.plugin.PluginManager;
import com.eagle.util.ProjectsStore;
import com.eagle.util.ThemeManager;
import java.io.File;
import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class Main extends Application {

    private static Stage primaryStage;
    private static String[] startupArgs;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;

        String filePath = getFilePathFromArgs(startupArgs);
        if (filePath != null) {
            File target = new File(filePath);
            if (target.exists()) {
                openTarget(target);
                return;
            }
        }

        showWelcomeScreen();
    }

    private static String getFilePathFromArgs(String[] args) {
        if (args != null && args.length > 0) {
            String arg = args[0].trim();
            if (!arg.startsWith("-") && !arg.startsWith("--")) {
                return arg;
            }
        }
        return null;
    }

    private void openTarget(File target) throws IOException {
        if (target.isDirectory()) {
            openProjectDir(target);
        } else {
            File parent = target.getParentFile();
            if (parent != null && parent.isDirectory()) {
                openProjectDir(parent);
                EditorController controller = getEditorController();
                if (controller != null) {
                    controller.openFileFromExternal(target);
                }
            } else {
                showWelcomeScreen();
            }
        }
    }

    public static void openProjectDir(File dir) throws IOException {
        if (dir == null || !dir.isDirectory()) {
            showWelcomeScreen();
            return;
        }

        File marker = new File(dir, ".eagle-project");
        if (!marker.exists()) {
            ProjectMeta.write(dir, ProjectType.CODE);
        }

        ProjectType type = ProjectMeta.read(dir);
        if (type == ProjectType.VISUAL) {
            javafx.application.Platform.runLater(() -> {
                Alert a = new Alert(Alert.AlertType.INFORMATION,
                    "Visual Builder is currently under development.\nStay tuned for future updates!");
                a.setTitle("Under Development");
                a.setHeaderText(null);
                a.showAndWait();
            });
            EditorController.openProject(dir);
        } else {
            EditorController.openProject(dir);
        }
    }

    private EditorController getEditorController() {
        if (primaryStage.getScene() != null) {
            Object ctrl = primaryStage.getScene().getUserData();
            if (ctrl instanceof EditorController) return (EditorController) ctrl;
        }
        return null;
    }

    public static void showWelcomeScreen() throws IOException {
        if (!ProjectsStore.isRootSet()) {
            ensureProjectsRoot();
        }

        // Initialize external plugins so welcome screen cards appear immediately
        PluginManager.getInstance().loadAll();

        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/com/eagle/fxml/Welcome.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1200, 800);
        ThemeManager.getInstance().applyTheme(scene);

        primaryStage.setTitle("Eagle - Welcome");
        primaryStage.setMaximized(true);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(800);
        primaryStage.show();
    }

    private static void ensureProjectsRoot() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Welcome! Select a folder to store your projects");
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File dir = chooser.showDialog(primaryStage);
        File fulldir= new File (dir+"\\EagleProjects");
        fulldir.mkdir();
        if (fulldir != null && fulldir.isDirectory()) {
            ProjectsStore.setProjectsRoot(fulldir);
        } else {
            ProjectsStore.setProjectsRoot(new File(System.getProperty("user.home"), "EagleProjects"));
            Alert alert = new Alert(Alert.AlertType.INFORMATION, System.getProperty("user.home")+"\\EagleProjects");
            alert.setHeaderText("We have set the default path to: ");
            ThemeManager.getInstance().applyTheme(alert.getDialogPane().getScene());
            alert.showAndWait();
            
        }
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        startupArgs = args;
        launch(args);
    }
}
