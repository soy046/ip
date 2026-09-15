package processor;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

import exceptions.TuesdayExceptions;
import task.Deadline;
import task.Event;
import task.Task;
import task.TaskList;
import task.Todo;
import ui.Strings;


/**
 * Provides methods for recognising and validating chatbot commands.
 */
public class Parser {
    private static final int MAX_TASKS = 100;

    private final String storageFilePath;
    private PendingDuplicate pendingDuplicate;

    /**
     * Creates a parser that uses Tuesday's default save file.
     */
    public Parser() {
        this(Storage.FILE_PATH);
    }

    /**
     * Creates a parser that uses a specified save file.
     *
     * <p>This constructor has package access so processor tests can use an isolated temporary file.</p>
     *
     * @param storageFilePath the save file used when commands change tasks
     */
    Parser(String storageFilePath) {
        this.storageFilePath = storageFilePath;
    }

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
            case "new" -> Command.NEW;
            case "old" -> Command.OLD;
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
     * @param tasks the tasks currently stored
     * @return true if the command refers to an existing task
     * @throws TuesdayExceptions.MarkTaskNumberOutOfRangeException if the task number is outside the list
     */
    public static boolean isAvailableMark(String input, TaskList tasks)
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
        if (target <= 0 || target > tasks.size()) {
            throw new TuesdayExceptions.MarkTaskNumberOutOfRangeException(String.valueOf(target));
        }
        return !scanner.hasNext();
    }

    /**
     * Checks whether a delete command refers to an existing task.
     *
     * @param input the complete line entered by the user
     * @param tasks the tasks currently stored
     * @return true if the command refers to an existing task
     * @throws TuesdayExceptions.DeleteTaskNumberOutOfRangeException if the task number is outside the list
     */
    public static boolean isAvailableDelete(String input, TaskList tasks)
            throws TuesdayExceptions.DeleteTaskNumberOutOfRangeException {
        Scanner scanner = new Scanner(input);

        if (!scanner.hasNext() || getCommand(scanner.next()) != Command.DELETE
                || !scanner.hasNextInt()) {
            return false;
        }

        int target = scanner.nextInt();
        if (target <= 0 || target > tasks.size()) {
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
     * Processes a command and returns the corresponding response.
     *
     * @param input the command entered by the user
     * @param tasks the task list
     * @return the response to display to the user
     */
    public static String commandProcess(String input, TaskList tasks) {
        return new Parser().processCommand(input, tasks);
    }

    /**
     * Processes a command as part of this parser's conversation session.
     *
     * <p>A parser instance remembers a proposed duplicate task until the user chooses {@code new} or {@code old}.</p>
     *
     * @param input the command entered by the user
     * @param tasks the task list
     * @return the response to display to the user
     */
    public String processCommand(String input, TaskList tasks) {
        if (pendingDuplicate != null) {
            return processDuplicateResolution(input, tasks);
        }

        Command command = getCommand(input);

        try {
            return switch (command) {
                case BYE -> Strings.FAREWELL;
                case LIST -> processListCommand(tasks);
                case FIND -> processFindCommand(input, tasks);
                case DELETE -> processDeleteCommand(input, tasks);
                case MARK, UNMARK -> processTaskStatusCommand(input, tasks, command);
                case TODO, DEADLINE, EVENT -> processTaskCreationCommand(input, tasks, command);
                case NEW, OLD, UNKNOWN -> throw new TuesdayExceptions.UnknownCommandException(input);
            };
        } catch (TuesdayExceptions.NoDescriptionnException e) {
            return "please add description, sir!";
        } catch (TuesdayExceptions.DeadlineMissingByDateException e) {
            return "please add a deadline date, sir!";
        } catch (TuesdayExceptions.EventMissingTimeException e) {
            return "please add both starting and ending times, sir!";
        } catch (TuesdayExceptions.MarkTaskNumberOutOfRangeException e) {
            return "Sir, that mark number is out of range.";
        } catch (TuesdayExceptions.DeleteTaskNumberOutOfRangeException e) {
            return "Sir, that delete number is out of range.";
        } catch (TuesdayExceptions.UnknownCommandException e) {
            return "Sir, what do you mean by " + e.getMessage();
        } catch (TuesdayExceptions.TaskNumberOutRangeException e) {
            return "Sir, this will cost too much time";
        }
    }

    /**
     * Processes the user's answer to a pending duplicate prompt.
     */
    private String processDuplicateResolution(String input, TaskList tasks) {
        String trimmedInput = input == null ? "" : input.trim();
        if (getCommand(trimmedInput) == Command.BYE) {
            pendingDuplicate = null;
            return Strings.FAREWELL;
        }
        if ("old".equals(trimmedInput)) {
            return keepExistingTask(tasks);
        }
        if ("new".equals(trimmedInput)) {
            return keepProposedTask(tasks);
        }
        return Strings.DUPLICATE_CHOICE_INSTRUCTION;
    }

    /**
     * Discards the proposed task and reports the unchanged list size.
     */
    private String keepExistingTask(TaskList tasks) {
        Task existingTask = pendingDuplicate.existingTask();
        pendingDuplicate = null;
        return "Okay. I've kept this task:\n"
                + "  " + existingTask + "\n"
                + "You still have " + tasks.size() + " tasks in the list.";
    }

    /**
     * Replaces the first matching task with the proposed task and saves the list.
     */
    private String keepProposedTask(TaskList tasks) {
        PendingDuplicate duplicate = pendingDuplicate;
        pendingDuplicate = null;

        tasks.replace(duplicate.existingTaskIndex(), duplicate.proposedTask());
        String response = "Got it. I've replaced this task:\n"
                + "  " + duplicate.existingTask() + "\n"
                + "with:\n"
                + "  " + duplicate.proposedTask() + "\n"
                + "You still have " + tasks.size() + " tasks in the list.";
        try {
            Storage.modifyData(storageFilePath, tasks);
        } catch (IOException e) {
            tasks.replace(duplicate.existingTaskIndex(), duplicate.existingTask());
            return "Failed to save data! Unable to create the file, Sir!\n"
                    + "The existing task was kept.";
        }
        return response;
    }

    /**
     * Formats all tasks for the list command.
     */
    private static String processListCommand(TaskList tasks) {
        StringBuilder taskOutput = new StringBuilder(Strings.SHOW_LIST);
        for (int i = 1; i <= tasks.size(); i++) {
            taskOutput.append("\n").append(i).append(".").append(tasks.get(i - 1));
        }
        return taskOutput.toString();
    }

    /**
     * Finds and formats tasks whose descriptions contain the requested keyword.
     */
    private static String processFindCommand(String input, TaskList tasks) {
        String keyword = input.substring("find".length()).trim();
        if (keyword.isEmpty()) {
            return "Please provide a keyword to find, Sir!";
        }

        StringBuilder taskOutput = new StringBuilder("Here are the matching tasks in your list:");
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).matchesDescription(keyword)) {
                taskOutput.append("\n").append(i + 1).append(".").append(tasks.get(i));
            }
        }
        return taskOutput.toString();
    }

    /**
     * Deletes the task selected by a valid delete command and saves the updated list.
     */
    private String processDeleteCommand(String input, TaskList tasks)
            throws TuesdayExceptions.DeleteTaskNumberOutOfRangeException,
            TuesdayExceptions.UnknownCommandException {
        if (!isAvailableDelete(input, tasks)) {
            throw new TuesdayExceptions.UnknownCommandException(input);
        }

        Scanner scanner = new Scanner(input);
        scanner.next();
        int target = scanner.nextInt();
        Task removedTask = tasks.remove(target - 1);
        String response = "Noted. I've removed this task:\n"
                + "  " + removedTask + "\n"
                + "Now you have " + tasks.size() + " tasks in the list.";
        try {
            Storage.modifyData(storageFilePath, tasks);
        } catch (IOException e) {
            response = "Failed to save data! Unable to create the save file, Sir!\n" + response;
        }
        return response;
    }

    /**
     * Marks or unmarks the selected task and saves its new status.
     */
    private String processTaskStatusCommand(String input, TaskList tasks, Command command)
            throws TuesdayExceptions.MarkTaskNumberOutOfRangeException,
            TuesdayExceptions.UnknownCommandException {
        assert command == Command.MARK || command == Command.UNMARK
                : "A task status command should always be mark or unmark";

        if (!isAvailableMark(input, tasks)) {
            throw new TuesdayExceptions.UnknownCommandException(input);
        }

        Scanner scanner = new Scanner(input);
        scanner.next();
        int target = scanner.nextInt();
        Task task = tasks.get(target - 1);

        String response;
        if (command == Command.MARK) {
            task.mark();
            response = Strings.MARK + "\n  " + task;
        } else {
            task.unMark();
            response = Strings.UNMARK + "\n  " + task;
        }

        try {
            Storage.modifyData(storageFilePath, tasks);
        } catch (IOException e) {
            response = "Failed to save data! Unable to create the file, Sir!\n" + response;
        }
        return response;
    }

    /**
     * Creates a task from a valid task-creation command and saves it.
     */
    private String processTaskCreationCommand(String input, TaskList tasks, Command command)
            throws TuesdayExceptions.NoDescriptionnException,
            TuesdayExceptions.DeadlineMissingByDateException,
            TuesdayExceptions.EventMissingTimeException,
            TuesdayExceptions.TaskNumberOutRangeException,
            TuesdayExceptions.UnknownCommandException {
        if (!isAvailableTaskCommand(input)) {
            throw new TuesdayExceptions.UnknownCommandException(input);
        }

        if (tasks.size() >= MAX_TASKS) {
            throw new TuesdayExceptions.TaskNumberOutRangeException("");
        }

        Task task = createTask(input, command);
        int duplicateIndex = tasks.indexOfDuplicate(task);
        if (duplicateIndex >= 0) {
            Task existingTask = tasks.get(duplicateIndex);
            pendingDuplicate = new PendingDuplicate(task, existingTask, duplicateIndex);
            return "I found an existing task with the same description:\n"
                    + "  " + (duplicateIndex + 1) + "." + existingTask + "\n"
                    + "Would you like to keep the new task or the old task?\n"
                    + "Please reply with \"new\" or \"old\".";
        }

        String response = "Got it. I've added this task:\n"
                + "  " + task + "\n"
                + "Now you have " + (tasks.size() + 1) + " tasks in the list.";
        try {
            Storage.saveNewData(storageFilePath, task.toString());
        } catch (IOException e) {
            response = "Failed to save data! Unable to create the file, Sir\n" + response;
        }
        tasks.add(task);
        return response;
    }

    /**
     * Constructs the task type selected by a task-creation command.
     */
    private static Task createTask(String input, Command command) {
        return switch (command) {
            case TODO -> createTodo(input);
            case DEADLINE -> createDeadline(input);
            case EVENT -> createEvent(input);
            default -> throw new IllegalArgumentException("Command does not create a task: " + command);
        };
    }

    /**
     * Constructs a todo from its command input.
     */
    private static Todo createTodo(String input) {
        String description = input.trim().substring("todo".length()).trim();
        return new Todo(description);
    }

    /**
     * Constructs a deadline from its command input.
     */
    private static Deadline createDeadline(String input) {
        String details = input.trim().substring("deadline".length()).trim();
        int byIndex = details.indexOf("/by");
        String description = details.substring(0, byIndex).trim();
        Event.DateTimeValue by = parseDateTime(details.substring(byIndex + 3).trim());
        assert by != null : "A validated deadline should always contain a valid date or time";
        return new Deadline(description, by.date(), by.time());
    }

    /**
     * Constructs an event from its command input.
     */
    private static Event createEvent(String input) {
        String details = input.trim().substring("event".length()).trim();
        int fromIndex = details.indexOf("/from");
        int toIndex = details.indexOf("/to");
        String description = details.substring(0, fromIndex).trim();
        Event.DateTimeValue from = parseDateTime(details.substring(fromIndex + 5, toIndex).trim());
        Event.DateTimeValue to = parseDateTime(details.substring(toIndex + 3).trim());
        assert from != null && to != null
                : "A validated event should always contain valid start and end times";
        return new Event(description, from, to);
    }

    /**
     * Stores the proposed task and the first existing task with the same description.
     */
    private record PendingDuplicate(Task proposedTask, Task existingTask, int existingTaskIndex) {
    }
}
