package ui;

import java.io.FileNotFoundException;
import java.util.Scanner;

import processor.Command;
import processor.DataSave;
import processor.Parser;
import task.TaskList;

/**
 * Handles user interaction for Tuesday.
 */
public class Ui {
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
}
