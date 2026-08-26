package processor;

import exceptions.TuesdayExceptions;
import task.Deadline;
import task.Event;
import task.Task;
import task.TaskList;
import task.Todo;
import ui.Strings;
import ui.UI;

import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;
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
            case "find" -> Command.FIND;
            case "mark" -> Command.MARK;
            case "unmark" -> Command.UNMARK;
            case "delete" -> Command.DELETE;
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
        case TODO -> hasDescription(input);
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
    public static boolean isAvailableMark(String input, int taskCount)
            throws TuesdayExceptions.MarkTaskNumberOutOfRangeException {
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
        if (target <= 0 || target > taskCount) {
            throw new TuesdayExceptions.MarkTaskNumberOutOfRangeException(String.valueOf(target));
        }
        return !scanner.hasNext();
    }

    /**
     * Checks whether a delete command refers to an existing task.
     *
     * @param input the complete line entered by the user
     * @param taskCount the number of tasks currently stored
     * @return true if the command refers to an existing task
     */
    public static boolean isAvailableDelete(String input, int taskCount)
            throws TuesdayExceptions.DeleteTaskNumberOutOfRangeException {
        Scanner scanner = new Scanner(input);

        if (!scanner.hasNext() || getCommand(scanner.next()) != Command.DELETE
                || !scanner.hasNextInt()) {
            return false;
        }

        int target = scanner.nextInt();
        if (target <= 0 || target > taskCount) {
            throw new TuesdayExceptions.DeleteTaskNumberOutOfRangeException(String.valueOf(target));
        }
        return !scanner.hasNext();
    }

    /**
     * Checks that a todo command has text after the command word.
     */
    private static boolean hasDescription(String input) throws TuesdayExceptions.NoDescriptionnException {
        if (input.trim().length() <= "todo".length()) {
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
        String deadline = details.substring(byIndex + 3).trim();
        if (parseDateTime(deadline) == null) {
            throw new TuesdayExceptions.DeadlineMissingByDateException(deadline);
        }
        return countOccurrences(details, "/by") == 1
                && !deadline.contains("/");
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

        String startTime = details.substring(fromIndex + 5, toIndex).trim();
        String endTime = details.substring(toIndex + 3).trim();
        return !startTime.contains("/") && !endTime.contains("/")
                && parseDateTime(startTime) != null && parseDateTime(endTime) != null;
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

    /**
     * Checks whether a value is an ISO date.
     */
    private static boolean isDate(String value) {
        if (!value.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return false;
        }
        try {
            LocalDate.parse(value);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Checks whether a value is an ISO time.
     */
    private static boolean isTime(String value) {
        if (!value.matches("\\d{2}:\\d{2}")) {
            return false;
        }
        try {
            LocalTime.parse(value);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Parses an input date-time value. It accepts a date, a time, or both.
     *
     * @param value an ISO date, ISO time, or ISO date followed by a time
     * @return the parsed date-time components, or null if invalid
     */
    public static Event.DateTimeValue parseDateTime(String value) {
        if (isDate(value)) {
            return new Event.DateTimeValue(LocalDate.parse(value), null);
        }
        if (isTime(value)) {
            return new Event.DateTimeValue(null, LocalTime.parse(value));
        }

        String[] parts = value.split(" ", -1);
        if (parts.length == 2 && isDate(parts[0]) && isTime(parts[1])) {
            return new Event.DateTimeValue(
                    LocalDate.parse(parts[0]), LocalTime.parse(parts[1]));
        }
        return null;
    }

    /**
     * Parses a date-time value stored in the task file's display format.
     */
    private static Event.DateTimeValue parseSavedDateTime(String value) {
        try {
            return new Event.DateTimeValue(
                    LocalDate.parse(value, Event.DATE_FORMATTER), null);
        } catch (DateTimeParseException dateException) {
            try {
                return new Event.DateTimeValue(
                        null, LocalTime.parse(value, Event.TIME_FORMATTER));
            } catch (DateTimeParseException timeException) {
                int separator = value.lastIndexOf(' ');
                if (separator <= 0) {
                    return null;
                }
                try {
                    return new Event.DateTimeValue(
                            LocalDate.parse(value.substring(0, separator), Event.DATE_FORMATTER),
                            LocalTime.parse(value.substring(separator + 1), Event.TIME_FORMATTER));
                } catch (DateTimeParseException combinedException) {
                    return null;
                }
            }
        }
    }

    /**
     * Loads saved tasks from a file into an ArrayList. And will skip the line which is not a valid Task
     *
     * @param filePath the path of the saved task file
     * @param tasks the list to which loaded tasks are added
     * @return the number of valid tasks loaded
     * @throws FileNotFoundException if the save file cannot be found
     */
    public static int loadData(String filePath, TaskList tasks)
            throws FileNotFoundException {
        int taskCount = 0;

        try (Scanner scanner = new Scanner(new File(filePath))) {
            while (scanner.hasNextLine()) {
                Task task = toTask(scanner.nextLine());
                if (task != null) {
                    tasks.add(task);
                    taskCount++;
                }
            }
        }

        return taskCount;
    }

    /**
     * Converts a saved task string back into its corresponding task object.
     *
     * @param taskData the saved string representation of a task
     * @return the reconstructed task, or null if the saved text is invalid
     */
    private static Task toTask(String taskData) {
        if (taskData == null || taskData.trim().length() < 8) {
            return null;
        }

        String data = taskData.trim();
        if (data.charAt(0) != '[' || data.charAt(2) != ']'
                || data.charAt(3) != '[' || data.charAt(5) != ']'
                || data.charAt(6) != ' ') {
            return null;
        }

        char type = data.charAt(1);
        char status = data.charAt(4);
        boolean isDone;

        if (status == 'X') {
            isDone = true;
        } else if (status == ' ') {
            isDone = false;
        } else {
            return null;
        }

        String details = data.substring(7);
        Task task;

        if (type == 'T') {
            task = new Todo(details);
        } else if (type == 'D') {
            String marker = " (by: ";
            int markerIndex = details.lastIndexOf(marker);
            if (markerIndex < 0 || !details.endsWith(")")) {
                return null;
            }

            String description = details.substring(0, markerIndex);
            String deadlineText = details.substring(markerIndex + marker.length(), details.length() - 1);
            if (description.isEmpty() || deadlineText.isEmpty()) {
                return null;
            }
            Event.DateTimeValue deadline = parseSavedDateTime(deadlineText);
            if (deadline == null) {
                return null;
            }
            task = new Deadline(description, deadline.date(), deadline.time());
        } else if (type == 'E') {
            String fromMarker = " (from: ";
            String toMarker = " to: ";
            int fromIndex = details.indexOf(fromMarker);
            int toIndex = details.lastIndexOf(toMarker);

            if (fromIndex < 0 || toIndex <= fromIndex || !details.endsWith(")")) {
                return null;
            }

            String description = details.substring(0, fromIndex);
            String startTimeText = details.substring(fromIndex + fromMarker.length(), toIndex);
            String endTimeText = details.substring(toIndex + toMarker.length(), details.length() - 1);
            if (description.isEmpty() || startTimeText.isEmpty() || endTimeText.isEmpty()) {
                return null;
            }
            Event.DateTimeValue start = parseSavedDateTime(startTimeText);
            Event.DateTimeValue end = parseSavedDateTime(endTimeText);
            if (start == null || end == null) {
                return null;
            }
            task = new Event(description, start, end);
        } else {
            return null;
        }

        if (isDone) {
            task.mark();
        }
        return task;
    }

    /**
     * Processes a valid mark, unmark, delete, todo, deadline, or event command.
     *
     * @param input the command entered by the user
     * @param tasks the task list
     * @param taskCount the number of tasks currently stored
     * @return 1 if a task was added, -1 if a task was deleted, otherwise 0
     * @throws TuesdayExceptions.NoDescriptionnException if a todo has no description
     */
    public static int commandProcess(String input, TaskList tasks, int taskCount)
            throws TuesdayExceptions.NoDescriptionnException, TuesdayExceptions.UnknownCommandException,
            TuesdayExceptions.TaskNumberOutRangeException, TuesdayExceptions.DeadlineMissingByDateException,
            TuesdayExceptions.EventMissingTimeException,
            TuesdayExceptions.MarkTaskNumberOutOfRangeException,
            TuesdayExceptions.DeleteTaskNumberOutOfRangeException {
        Command command = getCommand(input);

        if (command == Command.UNKNOWN) {
            throw new TuesdayExceptions.UnknownCommandException(input);
        }

        if (command == Command.DELETE) {
            if (!isAvailableDelete(input, taskCount)) {
                throw new TuesdayExceptions.UnknownCommandException(input);
            }

            Scanner scanner = new Scanner(input);
            scanner.next();
            int target = scanner.nextInt();
            Task removedTask = tasks.remove(target - 1);
            try {
                DataSave.modifyData(DataSave.FILE_PATH, tasks, taskCount - 1);
            } catch (IOException e) {
                UI.tuesdayPrint("Failed to save data! Unable to create the save file, Sir!");
            }
            UI.tuesdayPrint("Noted. I've removed this task:\n"
                    + "  " + removedTask + "\n"
                    + "Now you have " + (taskCount - 1) + " tasks in the list.");
            return -1;
        }

        if (command == Command.MARK || command == Command.UNMARK) {
            if (!isAvailableMark(input, taskCount)) {
                throw new TuesdayExceptions.UnknownCommandException(input);
            }

            Scanner scanner = new Scanner(input);
            scanner.next();
            int target = scanner.nextInt();
            Task task = tasks.get(target - 1);

            if (command == Command.MARK) {
                task.mark();
                UI.tuesdayPrint(Strings.mark + "\n  " + task);
            } else {
                task.unMark();
                UI.tuesdayPrint(Strings.unmark + "\n  " + task);
            }

            try {
                DataSave.modifyData(DataSave.FILE_PATH, tasks, taskCount);
            } catch (IOException e) {
                UI.tuesdayPrint("Failed to save data! Unable to create the file, Sir!");
            }
            return 0;
        }

        if (!isAvailableTaskCommand(input)) {
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
            Event.DateTimeValue by = parseDateTime(details.substring(byIndex + 3).trim());
            task = new Deadline(description, by.date(), by.time());
        } else {
            String details = trimmedInput.substring("event".length()).trim();
            int fromIndex = details.indexOf("/from");
            int toIndex = details.indexOf("/to");
            String description = details.substring(0, fromIndex).trim();
            Event.DateTimeValue from = parseDateTime(details.substring(fromIndex + 5, toIndex).trim());
            Event.DateTimeValue to = parseDateTime(details.substring(toIndex + 3).trim());
            task = new Event(description, from, to);
        }

        try {
            DataSave.saveNewData(DataSave.FILE_PATH, task.toString());
        } catch (IOException e) {
            UI.tuesdayPrint("Failed to save data! Unable to create the file, Sir");
        }
        tasks.add(task);
        UI.tuesdayPrint("Got it. I've added this task:\n"
                + "  " + task + "\n"
                + "Now you have " + (taskCount + 1) + " tasks in the list.");
        return 1;
    }
}
