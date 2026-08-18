import java.util.ArrayList;
import java.util.Scanner;

/**
 * Runs the chatbot
 */
public class Tuesday {
    /**
     *  a method for Tuesday to print
     * @param message a string about what Tuesday say
     */
    public static void tuesdayPrint(String message) {
        System.out.println(Strings.horizontalLine);
        System.out.println(message);
        System.out.println(Strings.horizontalLine);
    }

    /**
     * Processes a valid mark, unmark, delete, todo, deadline, or event command.
     *
     * @param input the command entered by the user
     * @param tasks the task array
     * @param taskCount the number of tasks currently stored
     * @return 1 if a task was added, -1 if a task was deleted, otherwise 0
     * @throws TuesdayExceptions.NoDescriptionnException if a todo has no description
     */
    public static int commandProcess(String input, ArrayList<Task> tasks, int taskCount)
            throws TuesdayExceptions.NoDescriptionnException, TuesdayExceptions.UnknownCommandException,
            TuesdayExceptions.TaskNumberOutRangeException, TuesdayExceptions.DeadlineMissingByDateException,
            TuesdayExceptions.EventMissingTimeException {
        Command command = Parser.getCommand(input);

        if (command == Command.UNKNOWN) {
            throw new TuesdayExceptions.UnknownCommandException(input);
        }

        if (command == Command.DELETE) {
            if (!Parser.isAvailableDelete(input, taskCount)) {
                throw new TuesdayExceptions.UnknownCommandException(input);
            }

            Scanner scanner = new Scanner(input);
            scanner.next();
            int target = scanner.nextInt();
            Task removedTask = tasks.remove(target - 1);

            tuesdayPrint("Noted. I've removed this task:\n"
                    + "  " + removedTask + "\n"
                    + "Now you have " + (taskCount - 1) + " tasks in the list.");
            return -1;
        }

        // process mark command
        if (command == Command.MARK || command == Command.UNMARK) {
            if (!Parser.isAvailableMark(input, taskCount)) {
                throw new TuesdayExceptions.UnknownCommandException(input);
            }

            Scanner scanner = new Scanner(input);
            scanner.next();
            int target = scanner.nextInt();
            Task task = tasks.get(target - 1);

            if (command == Command.MARK) {
                task.mark();
                tuesdayPrint(Strings.mark + "\n  " + task);
            } else {
                task.unMark();
                tuesdayPrint(Strings.unmark + "\n  " + task);
            }
            return 0;
        }

        if (!Parser.isAvailableTaskCommand(input)) {
            throw new TuesdayExceptions.UnknownCommandException(input);
        }

        if (taskCount >= 100) {
            throw new TuesdayExceptions.TaskNumberOutRangeException("");
        }

        String trimmedInput = input.trim();
        Task task;

        if (command == Command.TODO) {
            String description = trimmedInput.substring("todo".length()).trim();
            task = new Todo(description);
        } else if (command == Command.DEADLINE) {
            String details = trimmedInput.substring("deadline".length()).trim();
            int byIndex = details.indexOf("/by");
            String description = details.substring(0, byIndex).trim();
            String by = details.substring(byIndex + 3).trim();
            task = new Deadline(description, by);
        } else {
            String details = trimmedInput.substring("event".length()).trim();
            int fromIndex = details.indexOf("/from");
            int toIndex = details.indexOf("/to");
            String description = details.substring(0, fromIndex).trim();
            String from = details.substring(fromIndex + 5, toIndex).trim();
            String to = details.substring(toIndex + 3).trim();
            task = new Event(description, from, to);
        }

        tasks.add(task);
        tuesdayPrint("Got it. I've added this task:\n"
                + "  " + task + "\n"
                + "Now you have " + (taskCount + 1) + " tasks in the list.");
        return 1;
    }
    /**
     * Starts the chatbot, store tasks and process the user input
     *
     * @param args
     */
    public static void main(String[] args) {
        // initialize the input, scanner and the task array with the index for input
        Scanner sc = new Scanner(System.in);
        String input = "";
        ArrayList<Task> taskArray = new ArrayList<>();
        int taskCount = 0;

        // greetings part
        tuesdayPrint(Strings.banner + "\n" + Strings.greeting);

        // task adding part and tasks listing part
        input = sc.nextLine();
        while (Parser.getCommand(input) != Command.BYE) {
            Command command = Parser.getCommand(input);
            // task listing part
            if (command == Command.LIST) {
                StringBuilder taskList = new StringBuilder(Strings.showList);
                for (int i = 1; i < taskCount + 1; i++) {
                    taskList.append("\n").append(i).append(".").append(taskArray.get(i - 1));
                }
                tuesdayPrint(taskList.toString());
                input = sc.nextLine();
            // process mark, unmark, todo, deadline, and event commands
            } else if (command == Command.MARK || command == Command.UNMARK
                    || command == Command.DELETE || command == Command.TODO || command == Command.DEADLINE
                    || command == Command.EVENT || command == Command.UNKNOWN) {
                try {
                    taskCount += commandProcess(input, taskArray, taskCount);
                } catch (TuesdayExceptions.NoDescriptionnException e) {
                    tuesdayPrint("please add description, sir!");
                } catch (TuesdayExceptions.DeadlineMissingByDateException e) {
                    tuesdayPrint("please add a deadline date, sir!");
                } catch (TuesdayExceptions.EventMissingTimeException e) {
                    tuesdayPrint("please add both starting and ending times, sir!");
                } catch (TuesdayExceptions.UnknownCommandException e) {
                    tuesdayPrint("Sir, what do you mean by " + e.getMessage());
                } catch (TuesdayExceptions.TaskNumberOutRangeException e) {
                    tuesdayPrint("Sir, this will cost too much time");
                }
                input = sc.nextLine();
            }
        }

        // farewell part
        tuesdayPrint(Strings.farewell);
    }
}
