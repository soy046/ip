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
import processor.DataSave;
import processor.Parser;
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
    private int taskCount = 0;

    /**
     *  initializes the MainWindow
     */
    @FXML
    public void initialize() {
        scrollPane.vvalueProperty().bind(dialogContainer.heightProperty());
        try {
            this.taskCount = Parser.loadData(DataSave.FILE_PATH, this.tasks);
        } catch (FileNotFoundException e) {
            this.taskCount = 0;
        }
    }

    /**
     * handles user input
     */
    @FXML
    private void handleUserInput() {
        String input = userInput.getText();
        String response = Parser.commandProcess(input, tasks, taskCount);
        taskCount = tasks.size();
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
