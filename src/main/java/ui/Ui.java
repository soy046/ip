package ui;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

/**
 * Creates the Tuesday application window.
 */
public class Ui extends Application {
    /**
     * Opens the application at its preferred size and sets the minimum usable window size.
     * Displays the Tuesday name and icon in the window title bar.
     *
     * @param stage The primary application window supplied by JavaFX.
     */
    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Ui.class.getResource("/view/MainWindow.fxml"));
            AnchorPane root = fxmlLoader.load();
            Scene scene = new Scene(root);

            stage.setScene(scene);
            stage.setTitle("Tuesday");
            stage.getIcons().add(new Image(Ui.class.getResource("/images/Tuesday.png").toExternalForm()));
            stage.show();
            configureMinimumWindowSize(stage, scene, root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds the actual window borders to the minimum usable content size.
     * This must run after showing the stage, when its decoration sizes are available.
     *
     * @param stage The displayed application window.
     * @param scene The scene containing the application content.
     * @param root The root pane defining the minimum content dimensions.
     */
    private void configureMinimumWindowSize(Stage stage, Scene scene, AnchorPane root) {
        double decorationWidth = Math.max(0, stage.getWidth() - scene.getWidth());
        double decorationHeight = Math.max(0, stage.getHeight() - scene.getHeight());

        stage.setMinWidth(root.getMinWidth() + decorationWidth);
        stage.setMinHeight(root.getMinHeight() + decorationHeight);
    }
}
