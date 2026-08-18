import java.util.Scanner;

/**
 * Runs the chatbot
 */
public class Tuesday {
    /**
     * Processes a valid mark, unmark, todo, deadline, or event command.
     *
     * @param input the command entered by the user
     * @param tasks the task array
     * @param taskCount the number of tasks currently stored
     * @return true if a new task was added, otherwise false
     * @throws TuesdayExceptions.NoDescriptionnException if a todo has no description
     */
    public static boolean commandProcess(String input, Task[] tasks, int taskCount)
            throws TuesdayExceptions.NoDescriptionnException, TuesdayExceptions.UnknownCommandException {
        Command command = Parser.getCommand(input);

        if (command == Command.UNKNOWN) {
            throw new TuesdayExceptions.UnknownCommandException(input);
        }
        // process mark command
        if (command == Command.MARK || command == Command.UNMARK) {
            if (!Parser.isAvailableMark(input, taskCount)) {
                return false;
            }

            Scanner scanner = new Scanner(input);
            scanner.next();
            int target = scanner.nextInt();
            Task task = tasks[target - 1];

            System.out.println(Strings.horizontalLine);
            if (command == Command.MARK) {
                System.out.println(Strings.mark);
                task.mark();
            } else {
                System.out.println(Strings.unmark);
                task.unMark();
            }
            System.out.println("  " + task);
            System.out.println(Strings.horizontalLine);
            return false;
        }

        if (!Parser.isAvailableTaskCommand(input) || taskCount >= tasks.length) {
            return false;
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

        tasks[taskCount] = task;
        System.out.println(Strings.horizontalLine);
        System.out.println("Got it. I've added this task:");
        System.out.println("  " + task);
        System.out.println("Now you have " + (taskCount + 1) + " tasks in the list.");
        System.out.println(Strings.horizontalLine);
        return true;
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
        Task[] taskArray = new Task[100];
        int taskCount = 0;

        // greetings part
        System.out.println(Strings.horizontalLine);
        System.out.println(Strings.banner);
        System.out.println(Strings.greeting);
        System.out.println(Strings.horizontalLine);

        // task adding part and tasks listing part
        input = sc.nextLine();
        while (Parser.getCommand(input) != Command.BYE) {
            Command command = Parser.getCommand(input);
            // task listing part
            if (command == Command.LIST) {
                System.out.println(Strings.horizontalLine);
                System.out.println(Strings.showList);
                for (int i = 1; i < taskCount + 1; i++) {
                    System.out.println(i + "." + taskArray[i - 1].toString());
                }
                System.out.println(Strings.horizontalLine);
                input = sc.nextLine();
                continue;
            // process mark, unmark, todo, deadline, and event commands
            } else if (command == Command.MARK || command == Command.UNMARK
                    || command == Command.TODO || command == Command.DEADLINE
                    || command == Command.EVENT || command == Command.UNKNOWN) {
                try {
                    if (commandProcess(input, taskArray, taskCount)) {
                        taskCount++;
                    }
                } catch (TuesdayExceptions.NoDescriptionnException e) {
                    System.out.println(Strings.horizontalLine);
                    System.out.println("please add description, sir!");
                    System.out.println(Strings.horizontalLine);
                } catch (TuesdayExceptions.UnknownCommandException e) {
                    System.out.println(Strings.horizontalLine);
                    System.out.println("Sir, what do you mean by " + e.getMessage());
                    System.out.println(Strings.horizontalLine);
                }
                input = sc.nextLine();
                continue;
            }
        }

        // farewell part
        System.out.println(Strings.horizontalLine);
        System.out.println(Strings.farewell);
        System.out.println(Strings.horizontalLine);
    }
}
