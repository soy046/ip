import java.util.Scanner;

/**
 * Provides methods for recognising and validating chatbot commands.
 */
public class Parser {
    /**
     * Identifies the task-creation command at the start of an input line.
     *
     * @param input the complete line entered by the user
     * @return "todo", "deadline", or "event" for a matching command;
     *         otherwise an empty string
     */
    public static String getTaskCommand(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }

        String trimmedInput = input.trim();
        String command = trimmedInput.split("\\s+", 2)[0];

        return switch (command) {
        case "todo", "deadline", "event" -> command;
        default -> "";
        };
    }

    /**
     * Checks whether the input is a valid task-creation command.
     *
     * @param input the complete line entered by the user
     * @return true if the input is a valid todo, deadline, or event command
     */
    public static boolean isAvailableTaskCommand(String input) {
        return switch (getTaskCommand(input)) {
        case "todo" -> hasDescription(input, "todo");
        case "deadline" -> isValidDeadline(input);
        case "event" -> isValidEvent(input);
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

        String command = scanner.next();
        if (!command.equals("mark") && !command.equals("unmark")) {
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
    private static boolean hasDescription(String input, String command) {
        return input.trim().length() > command.length();
    }

    /**
     * Checks that a deadline has exactly one non-empty /by section.
     */
    private static boolean isValidDeadline(String input) {
        String details = input.trim().substring("deadline".length()).trim();
        int byIndex = details.indexOf("/by");

        return byIndex > 0
                && countOccurrences(details, "/by") == 1
                && !details.substring(byIndex + 3).trim().isEmpty()
                && !details.substring(byIndex + 3).contains("/");
    }

    /**
     * Checks that an event has exactly one /from section and one /to section.
     */
    private static boolean isValidEvent(String input) {
        String details = input.trim().substring("event".length()).trim();
        int fromIndex = details.indexOf("/from");
        int toIndex = details.indexOf("/to");

        return fromIndex > 0
                && toIndex > fromIndex
                && countOccurrences(details, "/from") == 1
                && countOccurrences(details, "/to") == 1
                && !details.substring(fromIndex + 5, toIndex).trim().isEmpty()
                && !details.substring(fromIndex + 5, toIndex).contains("/")
                && !details.substring(toIndex + 3).trim().isEmpty()
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
