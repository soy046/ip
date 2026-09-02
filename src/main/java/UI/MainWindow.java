package ui;

import java.io.FileNotFoundException;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
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

    private Image tuesdayImage = new Image(this.getClass().getResourceAsStream("images/Tuesday.png"));
    private Image userImage = new Image(this.getClass().getResourceAsStream("images/TontStark.png"));
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
        String response = input;
        dialogContainer.getChildren().addAll(
                DialogBox.getUserDialog(input, userImage),
                DialogBox.getTuesdayDialog(response, userImage)
        );
        userInput.clear();
    }


}
