package ui;

import java.io.FileNotFoundException;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import processor.Command;
import processor.Parser;
import processor.Storage;
import task.TaskList;

/**
 * Controller for the main GUI
 */
public class MainWindow extends AnchorPane {
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private VBox dialogContainer;
    @FXML
    private TextField userInput;
    @FXML
    private Button sendButton;

    private Image tuesdayImage = new Image(this.getClass().getResourceAsStream("/images/Tuesday.png"));
    private Image userImage = new Image(this.getClass().getResourceAsStream("/images/TonyStark.png"));
    private TaskList tasks = new TaskList();
    private Parser parser = new Parser();

    /**
     *  initializes the MainWindow
     */
    @FXML
    public void initialize() {
        scrollPane.vvalueProperty().bind(dialogContainer.heightProperty());
        try {
            Storage.loadData(Storage.FILE_PATH, this.tasks);
        } catch (FileNotFoundException e) {
            // A missing save file is expected when the application runs for the first time.
        }
    }

    /**
     * handles user input
     */
    @FXML
    private void handleUserInput() {
        String input = userInput.getText();
        String response = parser.processCommand(input, tasks);
        dialogContainer.getChildren().addAll(
                DialogBox.getUserDialog(input, userImage),
                DialogBox.getTuesdayDialog(response, tuesdayImage)
        );
        userInput.clear();

        if (Parser.getCommand(input) == Command.BYE) {
            PauseTransition pause = new PauseTransition(Duration.seconds(2));
            pause.setOnFinished(event -> Platform.exit());
            pause.play();
        }
    }


}
