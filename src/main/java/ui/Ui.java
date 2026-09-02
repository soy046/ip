package ui;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import processor.Command;
import processor.DataSave;
import processor.Parser;
import task.TaskList;

/**
 * Handles text user interaction for Tuesday.
 */
public class Ui extends Application {
    /**
     * Prints a message surrounded by the chatbot's divider lines.
     *
     * @param message the message to display
     */
    public static void tuesdayPrint(String message) {
        System.out.println(Strings.HORIZONTAL_LINE);
        System.out.println(message);
        System.out.println(Strings.HORIZONTAL_LINE);
    }

    /**
     * Starts the interactive chatbot session.
     */
    public void run() {
        Scanner scanner = new Scanner(System.in);
        TaskList taskList = new TaskList();
        int taskCount;

        try {
            taskCount = Parser.loadData(DataSave.FILE_PATH, taskList);
        } catch (FileNotFoundException e) {
            taskCount = 0;
        }

        tuesdayPrint(Strings.BANNER + "\n" + Strings.GREETING);

        String input = scanner.nextLine();
        while (true) {
            taskCount += Parser.commandProcess(input, taskList, taskCount);
            if (Parser.getCommand(input) == Command.BYE) {
                break;
            }
            input = scanner.nextLine();
        }
    }

    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Ui.class.getResource("/view/MainWindow.fxml"));
            AnchorPane ap = fxmlLoader.load();
            Scene scene = new Scene(ap);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
