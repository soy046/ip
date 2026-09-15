package processor;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Objects;
import java.util.Scanner;

import exceptions.TuesdayExceptions;
import task.Deadline;
import task.Event;
import task.Task;
import task.TaskList;
import task.Todo;
import ui.Strings;


/**
 * Provides methods for recognizing and validating chatbot commands.
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
     * The same path should be used when loading the task list.
     *
     * @param storageFilePath the save file used when commands change tasks
     */
    public Parser(String storageFilePath) {
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
     * @throws TuesdayExceptions.NoDescriptionnException if the description is missing
     * @throws TuesdayExceptions.DeadlineMissingByDateException if the deadline is missing or invalid
     * @throws TuesdayExceptions.EventMissingTimeException if an event time section is missing
     */
    public static boolean isAvailableTaskCommand(String input)
            throws TuesdayExceptions.NoDescriptionnException,
            TuesdayExceptions.DeadlineMissingByDateException,
            TuesdayExceptions.EventMissingTimeException {
        ValidationError error = validateTaskCreation(input, getCommand(input));
        if (error == null) {
            return true;
        }
        return switch (error.code()) {
            case MISSING_DESCRIPTION -> throw new TuesdayExceptions.NoDescriptionnException(error.detail());
            case MISSING_DEADLINE -> throw new TuesdayExceptions.DeadlineMissingByDateException(error.detail());
            case MISSING_EVENT_TIME -> throw new TuesdayExceptions.EventMissingTimeException(error.detail());
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
        Objects.requireNonNull(input);
        ValidationError error = validateTaskIndex(input, tasks, Command.MARK).error();
        if (error != null && error.code() == ValidationCode.MARK_OUT_OF_RANGE) {
            throw new TuesdayExceptions.MarkTaskNumberOutOfRangeException(error.detail());
        }
        return error == null;
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
        Objects.requireNonNull(input);
        ValidationError error = validateTaskIndex(input, tasks, Command.DELETE).error();
        if (error != null && error.code() == ValidationCode.DELETE_OUT_OF_RANGE) {
            throw new TuesdayExceptions.DeleteTaskNumberOutOfRangeException(error.detail());
        }
        return error == null;
    }

    /**
     * Returns a validated zero-based index or an error before any task is accessed.
     */
    private static IndexValidation validateTaskIndex(String input, TaskList tasks, Command command) {
        ValidationError unknown = new ValidationError(ValidationCode.UNKNOWN_COMMAND, input);
        try (Scanner scanner = new Scanner(input)) {
            if (!scanner.hasNext()) {
                return new IndexValidation(-1, unknown);
            }
            Command actualCommand = getCommand(scanner.next());
            boolean isMatchingCommand = command == Command.DELETE
                    ? actualCommand == Command.DELETE
                    : actualCommand == Command.MARK || actualCommand == Command.UNMARK;
            if (!isMatchingCommand || !scanner.hasNextInt()) {
                return new IndexValidation(-1, unknown);
            }
            int target = scanner.nextInt();
            if (target <= 0 || target > tasks.size()) {
                ValidationCode code = command == Command.DELETE
                        ? ValidationCode.DELETE_OUT_OF_RANGE : ValidationCode.MARK_OUT_OF_RANGE;
                return new IndexValidation(-1, new ValidationError(code, String.valueOf(target)));
            }
            if (scanner.hasNext()) {
                return new IndexValidation(-1, unknown);
            }
            return new IndexValidation(target - 1, null);
        }
    }

    /**
     * Returns a creation error, or null when all required fields are valid.
     */
    private static ValidationError validateTaskCreation(String input, Command command) {
        return switch (command) {
            case TODO -> validateTodo(input);
            case DEADLINE -> validateDeadline(input);
            case EVENT -> validateEvent(input);
            default -> new ValidationError(ValidationCode.UNKNOWN_COMMAND, input);
        };
    }

    /**
     * Returns a missing-description error, or null for a valid todo.
     */
    private static ValidationError validateTodo(String input) {
        if (input.trim().length() <= "todo".length()) {
            return new ValidationError(ValidationCode.MISSING_DESCRIPTION, "todo");
        }
        return null;
    }

    /**
     * Returns a deadline validation error, or null when its required description and value are valid.
     */
    private static ValidationError validateDeadline(String input) {
        String details = input.trim().substring("deadline".length()).trim();
        int byIndex = details.indexOf("/by");

        if (details.isEmpty() || (byIndex >= 0 && details.substring(0, byIndex).trim().isEmpty())) {
            return new ValidationError(ValidationCode.MISSING_DESCRIPTION, "deadline");
        }

        if (byIndex <= 0 || details.substring(byIndex + 3).trim().isEmpty()) {
            return new ValidationError(ValidationCode.MISSING_DEADLINE, "");
        }
        String deadline = details.substring(byIndex + 3).trim();
        if (parseDateTime(deadline) == null) {
            return new ValidationError(ValidationCode.MISSING_DEADLINE, deadline);
        }
        if (countOccurrences(details, "/by") != 1 || deadline.contains("/")) {
            return new ValidationError(ValidationCode.UNKNOWN_COMMAND, input);
        }
        return null;
    }

    /**
     * Returns an event validation error, or null when its description and both time sections are valid.
     */
    private static ValidationError validateEvent(String input) {
        String details = input.trim().substring("event".length()).trim();
        int fromIndex = details.indexOf("/from");
        int toIndex = details.indexOf("/to");

        if (details.isEmpty() || fromIndex == 0) {
            return new ValidationError(ValidationCode.MISSING_DESCRIPTION, "event");
        }

        if (fromIndex < 0 || toIndex < 0 || toIndex <= fromIndex
                || countOccurrences(details, "/from") != 1
                || countOccurrences(details, "/to") != 1
                || details.substring(fromIndex + 5, toIndex).trim().isEmpty()
                || details.substring(toIndex + 3).trim().isEmpty()) {
            return new ValidationError(ValidationCode.MISSING_EVENT_TIME, "event");
        }

        String startTime = details.substring(fromIndex + 5, toIndex).trim();
        String endTime = details.substring(toIndex + 3).trim();
        if (startTime.contains("/") || endTime.contains("/")
                || parseDateTime(startTime) == null || parseDateTime(endTime) == null) {
            return new ValidationError(ValidationCode.UNKNOWN_COMMAND, input);
        }
        return null;
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
        int year = Integer.parseInt(value.substring(0, 4));
        int month = Integer.parseInt(value.substring(5, 7));
        int day = Integer.parseInt(value.substring(8, 10));
        return month >= 1 && month <= 12 && YearMonth.of(year, month).isValidDay(day);
    }

    /**
     * Checks whether a value is an ISO time.
     */
    private static boolean isTime(String value) {
        if (!value.matches("\\d{2}:\\d{2}")) {
            return false;
        }
        int hour = Integer.parseInt(value.substring(0, 2));
        int minute = Integer.parseInt(value.substring(3, 5));
        return hour < 24 && minute < 60;
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

        return switch (command) {
            case BYE -> Strings.FAREWELL;
            case LIST -> processListCommand(tasks);
            case FIND -> processFindCommand(input, tasks);
            case DELETE -> processDeleteCommand(input, tasks);
            case MARK, UNMARK -> processTaskStatusCommand(input, tasks, command);
            case TODO, DEADLINE, EVENT -> processTaskCreationCommand(input, tasks, command);
            case NEW, OLD, UNKNOWN -> "Sir, what do you mean by " + input;
        };
    }

    /**
     * Converts an expected validation failure into its existing user-facing response.
     */
    private static String formatValidationError(ValidationError error, String originalInput) {
        return switch (error.code()) {
            case MISSING_DESCRIPTION -> "please add description, sir!";
            case MISSING_DEADLINE -> "please add a deadline date, sir!";
            case MISSING_EVENT_TIME -> "please add both starting and ending times, sir!";
            case MARK_OUT_OF_RANGE -> "Sir, that mark number is out of range.";
            case DELETE_OUT_OF_RANGE -> "Sir, that delete number is out of range.";
            case UNKNOWN_COMMAND -> "Sir, what do you mean by " + originalInput;
        };
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
    private String processDeleteCommand(String input, TaskList tasks) {
        IndexValidation validation = validateTaskIndex(input, tasks, Command.DELETE);
        if (validation.error() != null) {
            return formatValidationError(validation.error(), input);
        }

        Task removedTask = tasks.remove(validation.index());
        String response = "Noted. I've removed this task:\n"
                + "  " + removedTask + "\n"
                + "Now you have " + tasks.size() + " tasks in the list.";
        try {
            Storage.modifyData(storageFilePath, tasks);
        } catch (IOException e) {
            tasks.add(validation.index(), removedTask);
            return Strings.SAVE_FAILURE;
        }
        return response;
    }

    /**
     * Marks or unmarks the selected task and saves its new status.
     */
    private String processTaskStatusCommand(String input, TaskList tasks, Command command) {
        assert command == Command.MARK || command == Command.UNMARK
                : "A task status command should always be mark or unmark";

        IndexValidation validation = validateTaskIndex(input, tasks, command);
        if (validation.error() != null) {
            return formatValidationError(validation.error(), input);
        }

        Task task = tasks.get(validation.index());
        boolean wasDone = task.isDone();

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
            if (wasDone) {
                task.mark();
            } else {
                task.unMark();
            }
            return Strings.SAVE_FAILURE;
        }
        return response;
    }

    /**
     * Creates a task from a valid task-creation command and saves it.
     */
    private String processTaskCreationCommand(String input, TaskList tasks, Command command) {
        ValidationError error = validateTaskCreation(input, command);
        if (error != null) {
            return formatValidationError(error, input);
        }

        if (tasks.size() >= MAX_TASKS) {
            return "Sir, this will cost too much time";
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
            return Strings.SAVE_FAILURE;
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

    /**
     * Identifies expected input failures without throwing during command processing.
     */
    private enum ValidationCode {
        UNKNOWN_COMMAND,
        MISSING_DESCRIPTION,
        MISSING_DEADLINE,
        MISSING_EVENT_TIME,
        MARK_OUT_OF_RANGE,
        DELETE_OUT_OF_RANGE
    }

    /**
     * Keeps the failure category and payload used by the public exception-based adapters.
     */
    private record ValidationError(ValidationCode code, String detail) {
    }

    /**
     * Holds a valid zero-based index, or -1 and a non-null error on failure.
     */
    private record IndexValidation(int index, ValidationError error) {
    }
}
