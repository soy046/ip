import java.util.Scanner;

/**
 * Provides methods for recognising and validating chatbot commands.
 */
public class Parser {
    /**
     * Identifies the task-creation command at the start of an input line.
     *
     * @param input the complete line entered by the user
     * @return the matching command, or UNKNOWN when it is not recognised
     */
    public static Command getCommand(String input) {
        if (input == null || input.trim().isEmpty()) {
            return Command.UNKNOWN;
        }

        String trimmedInput = input.trim();
        String command = trimmedInput.split("\\s+", 2)[0];

        return switch (command) {
        case "todo" -> Command.TODO;
        case "deadline" -> Command.DEADLINE;
        case "event" -> Command.EVENT;
        case "list" -> Command.LIST;
        case "mark" -> Command.MARK;
        case "unmark" -> Command.UNMARK;
        case "bye" -> Command.BYE;
        default -> Command.UNKNOWN;
        };
    }

    /**
     * Checks whether the input is a valid task-creation command.
     *
     * @param input the complete line entered by the user
     * @return true if the input is a valid todo, deadline, or event command
     */
    public static boolean isAvailableTaskCommand(String input)
            throws TuesdayExceptions.NoDescriptionnException,
            TuesdayExceptions.DeadlineMissingByDateException,
            TuesdayExceptions.EventMissingTimeException {
        return switch (getCommand(input)) {
        case TODO -> hasDescription(input, "todo");
        case DEADLINE -> isValidDeadline(input);
        case EVENT -> isValidEvent(input);
        default -> false;
        };
    }

    /**
     * Checks whether the input is a valid mark or unmark command.
     *
     * @param input the complete line entered by the user
     * @param taskCount the number of tasks currently stored
     * @return true if the command refers to an existing task
     */
    public static boolean isAvailableMark(String input, int taskCount) {
        Scanner scanner = new Scanner(input);

        if (!scanner.hasNext()) {
            return false;
        }

        Command command = getCommand(scanner.next());
        if (command != Command.MARK && command != Command.UNMARK) {
            return false;
        }

        if (!scanner.hasNextInt()) {
            return false;
        }

        int target = scanner.nextInt();
        return !scanner.hasNext() && target > 0 && target <= taskCount;
    }

    /**
     * Checks that a todo command has text after the command word.
     */
    private static boolean hasDescription(String input, String command) throws TuesdayExceptions.NoDescriptionnException {
        if (input.trim().length() <= command.length()) {
            throw new TuesdayExceptions.NoDescriptionnException("todo");
        }
        return true;
    }

    /**
     * Checks that a deadline has exactly one non-empty /by section.
     */
    private static boolean isValidDeadline(String input)
            throws TuesdayExceptions.NoDescriptionnException,
            TuesdayExceptions.DeadlineMissingByDateException {
        String details = input.trim().substring("deadline".length()).trim();
        int byIndex = details.indexOf("/by");

        if (details.isEmpty() || (byIndex >= 0 && details.substring(0, byIndex).trim().isEmpty())) {
            throw new TuesdayExceptions.NoDescriptionnException("deadline");
        }

        if (byIndex <= 0 || details.substring(byIndex + 3).trim().isEmpty()) {
            throw new TuesdayExceptions.DeadlineMissingByDateException("");
        }
        return countOccurrences(details, "/by") == 1
                && !details.substring(byIndex + 3).contains("/");
    }

    /**
     * Checks that an event has exactly one /from section and one /to section.
     */
    private static boolean isValidEvent(String input)
            throws TuesdayExceptions.NoDescriptionnException,
            TuesdayExceptions.EventMissingTimeException {
        String details = input.trim().substring("event".length()).trim();
        int fromIndex = details.indexOf("/from");
        int toIndex = details.indexOf("/to");

        if (details.isEmpty() || fromIndex == 0) {
            throw new TuesdayExceptions.NoDescriptionnException("event");
        }

        if (fromIndex < 0 || toIndex < 0 || toIndex <= fromIndex
                || countOccurrences(details, "/from") != 1
                || countOccurrences(details, "/to") != 1
                || details.substring(fromIndex + 5, toIndex).trim().isEmpty()
                || details.substring(toIndex + 3).trim().isEmpty()) {
            throw new TuesdayExceptions.EventMissingTimeException("event");
        }

        return !details.substring(fromIndex + 5, toIndex).contains("/")
                && !details.substring(toIndex + 3).contains("/");
    }

    /**
     * Counts how many times a marker appears in a string.
     */
    private static int countOccurrences(String text, String marker) {
        int count = 0;
        int index = 0;

        while ((index = text.indexOf(marker, index)) >= 0) {
            count++;
            index += marker.length();
        }

        return count;
    }
}
