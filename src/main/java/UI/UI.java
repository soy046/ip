package ui;

import java.io.FileNotFoundException;
import java.util.Scanner;

import exceptions.TuesdayExceptions;
import processor.Command;
import processor.DataSave;
import processor.Parser;
import task.TaskList;

/**
 * Handles user interaction for Tuesday.
 */
public class UI {
    /**
     * Prints a message surrounded by the chatbot's divider lines.
     *
     * @param message the message to display
     */
    public static void tuesdayPrint(String message) {
        System.out.println(Strings.horizontalLine);
        System.out.println(message);
        System.out.println(Strings.horizontalLine);
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

        tuesdayPrint(Strings.banner + "\n" + Strings.greeting);

        String input = scanner.nextLine();
        while (Parser.getCommand(input) != Command.BYE) {
            Command command = Parser.getCommand(input);

            if (command == Command.LIST) {
                StringBuilder taskOutput = new StringBuilder(Strings.showList);
                for (int i = 1; i <= taskCount; i++) {
                    taskOutput.append("\n").append(i).append(".").append(taskList.get(i - 1));
                }
                tuesdayPrint(taskOutput.toString());
            } else if (command == Command.MARK || command == Command.UNMARK
                    || command == Command.DELETE || command == Command.TODO
                    || command == Command.DEADLINE || command == Command.EVENT
                    || command == Command.UNKNOWN) {
                try {
                    taskCount += Parser.commandProcess(input, taskList, taskCount);
                } catch (TuesdayExceptions.NoDescriptionnException e) {
                    tuesdayPrint("please add description, sir!");
                } catch (TuesdayExceptions.DeadlineMissingByDateException e) {
                    tuesdayPrint("please add a deadline date, sir!");
                } catch (TuesdayExceptions.EventMissingTimeException e) {
                    tuesdayPrint("please add both starting and ending times, sir!");
                } catch (TuesdayExceptions.MarkTaskNumberOutOfRangeException e) {
                    tuesdayPrint("Sir, that mark number is out of range.");
                } catch (TuesdayExceptions.DeleteTaskNumberOutOfRangeException e) {
                    tuesdayPrint("Sir, that delete number is out of range.");
                } catch (TuesdayExceptions.UnknownCommandException e) {
                    tuesdayPrint("Sir, what do you mean by " + e.getMessage());
                } catch (TuesdayExceptions.TaskNumberOutRangeException e) {
                    tuesdayPrint("Sir, this will cost too much time");
                }
            }

            input = scanner.nextLine();
        }

        tuesdayPrint(Strings.farewell);
    }
}
