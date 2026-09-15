package tuesday.processor;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Objects;
import java.util.Scanner;

import tuesday.exceptions.TuesdayExceptions;
import tuesday.task.Deadline;
import tuesday.task.Event;
import tuesday.task.Task;
import tuesday.task.TaskList;
import tuesday.task.Todo;
import tuesday.ui.Strings;

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
     * @param storageFilePath the save file used when commands change tasks.
     */
    public Parser(String storageFilePath) {
        this.storageFilePath = storageFilePath;
    }

    /**
     * Identifies the task-creation command at the start of an input line.
     *
     * @param input the complete line entered by the user.
     * @return the matching command, or UNKNOWN when it is not recognized.
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
     * @param input the complete line entered by the user.
     * @return true if the input is a valid todo, deadline, or event command.
     * @throws TuesdayExceptions.NoDescriptionException if the description is missing.
     * @throws TuesdayExceptions.DeadlineMissingByDateException if the deadline is missing or invalid.
     * @throws TuesdayExceptions.EventMissingTimeException if an event time section is missing.
     */
    public static boolean isAvailableTaskCommand(String input)
            throws TuesdayExceptions.NoDescriptionException,
            TuesdayExceptions.DeadlineMissingByDateException,
            TuesdayExceptions.EventMissingTimeException {
        ValidationError error = parseTaskCreation(input, getCommand(input)).error();
        if (error == null) {
            return true;
        }
        return switch (error.code()) {
            case MISSING_DESCRIPTION -> throw new TuesdayExceptions.NoDescriptionException(error.detail());
            case MISSING_DEADLINE -> throw new TuesdayExceptions.DeadlineMissingByDateException(error.detail());
            case MISSING_EVENT_TIME -> throw new TuesdayExceptions.EventMissingTimeException(error.detail());
            default -> false;
        };
    }

    /**
     * Checks whether the input is a valid mark or unmark command.
     *
     * @param input the complete line entered by the user.
     * @param tasks the tasks currently stored.
     * @return true if the command refers to an existing task.
     * @throws TuesdayExceptions.MarkTaskNumberOutOfRangeException if the task number is outside the list.
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
     * @param input the complete line entered by the user.
     * @param tasks the tasks currently stored.
     * @return true if the command refers to an existing task.
     * @throws TuesdayExceptions.DeleteTaskNumberOutOfRangeException if the task number is outside the list.
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
     * Parses a task once, returning either the task or its validation error.
     */
    private static TaskValidation parseTaskCreation(String input, Command command) {
        return switch (command) {
            case TODO -> parseTodo(input);
            case DEADLINE -> parseDeadline(input);
            case EVENT -> parseEvent(input);
            default -> TaskValidation.invalid(ValidationCode.UNKNOWN_COMMAND, input);
        };
    }

    /**
     * Parses a todo or reports its missing description.
     */
    private static TaskValidation parseTodo(String input) {
        String description = input.trim().substring("todo".length()).trim();
        if (description.isEmpty()) {
            return TaskValidation.invalid(ValidationCode.MISSING_DESCRIPTION, "todo");
        }
        return new TaskValidation(new Todo(description), null);
    }

    /**
     * Parses a deadline while preserving the precedence of description, date-time, and syntax errors.
     */
    private static TaskValidation parseDeadline(String input) {
        String details = input.trim().substring("deadline".length()).trim();
        int byIndex = details.indexOf("/by");

        if (details.isEmpty() || (byIndex >= 0 && details.substring(0, byIndex).trim().isEmpty())) {
            return TaskValidation.invalid(ValidationCode.MISSING_DESCRIPTION, "deadline");
        }

        if (byIndex <= 0 || details.substring(byIndex + 3).trim().isEmpty()) {
            return TaskValidation.invalid(ValidationCode.MISSING_DEADLINE, "");
        }
        String deadline = details.substring(byIndex + 3).trim();
        Event.DateTimeValue by = parseDateTime(deadline);
        if (by == null) {
            return TaskValidation.invalid(ValidationCode.MISSING_DEADLINE, deadline);
        }
        if (countOccurrences(details, "/by") != 1 || deadline.contains("/")) {
            return TaskValidation.invalid(ValidationCode.UNKNOWN_COMMAND, input);
        }
        String description = details.substring(0, byIndex).trim();
        return new TaskValidation(new Deadline(description, by.date(), by.time()), null);
    }

    /**
     * Parses an event while preserving the precedence of description, marker, and date-time errors.
     */
    private static TaskValidation parseEvent(String input) {
        String details = input.trim().substring("event".length()).trim();
        int fromIndex = details.indexOf("/from");
        int toIndex = details.indexOf("/to");

        if (details.isEmpty() || fromIndex == 0) {
            return TaskValidation.invalid(ValidationCode.MISSING_DESCRIPTION, "event");
        }

        if (!hasValidEventMarkers(details, fromIndex, toIndex)) {
            return TaskValidation.invalid(ValidationCode.MISSING_EVENT_TIME, "event");
        }
        if (hasEmptyEventTimes(details, fromIndex, toIndex)) {
            return TaskValidation.invalid(ValidationCode.MISSING_EVENT_TIME, "event");
        }

        String startTime = details.substring(fromIndex + 5, toIndex).trim();
        String endTime = details.substring(toIndex + 3).trim();
        if (startTime.contains("/") || endTime.contains("/")) {
            return TaskValidation.invalid(ValidationCode.UNKNOWN_COMMAND, input);
        }
        Event.DateTimeValue from = parseDateTime(startTime);
        Event.DateTimeValue to = parseDateTime(endTime);
        if (from == null || to == null) {
            return TaskValidation.invalid(ValidationCode.UNKNOWN_COMMAND, input);
        }
        String description = details.substring(0, fromIndex).trim();
        return new TaskValidation(new Event(description, from, to), null);
    }

    /**
     * Checks that each event marker appears once and that the start marker ends before the end marker.
     */
    private static boolean hasValidEventMarkers(String details, int fromIndex, int toIndex) {
        return fromIndex >= 0 && toIndex >= fromIndex + "/from".length()
                && countOccurrences(details, "/from") == 1
                && countOccurrences(details, "/to") == 1;
    }

    /**
     * Checks for empty time sections after marker presence, order, and uniqueness have been validated.
     */
    private static boolean hasEmptyEventTimes(String details, int fromIndex, int toIndex) {
        return details.substring(fromIndex + "/from".length(), toIndex).trim().isEmpty()
                || details.substring(toIndex + "/to".length()).trim().isEmpty();
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
     * @param value an ISO date, ISO time, or ISO date followed by a time.
     * @return the parsed date-time components, or null if invalid.
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
     * Processes a command as part of this parser's conversation session.
     *
     * <p>A parser instance remembers a proposed duplicate task until the user chooses {@code new} or {@code old}.</p>
     *
     * @param input the command entered by the user.
     * @param tasks the task list.
     * @return the response to display to the user.
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
        String response = formatTaskReplaced(duplicate.existingTask(), duplicate.proposedTask(), tasks.size());
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
     * Formats a replacement confirmation with the unchanged number of tasks.
     */
    private static String formatTaskReplaced(Task existingTask, Task proposedTask, int taskCount) {
        return "Got it. I've replaced this task:\n"
                + "  " + existingTask + "\n"
                + "with:\n"
                + "  " + proposedTask + "\n"
                + "You still have " + taskCount + " tasks in the list.";
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
        String response = formatTaskDeleted(removedTask, tasks.size());
        try {
            Storage.modifyData(storageFilePath, tasks);
        } catch (IOException e) {
            tasks.add(validation.index(), removedTask);
            return Strings.SAVE_FAILURE;
        }
        return response;
    }

    /**
     * Formats a deletion confirmation with the number of remaining tasks.
     */
    private static String formatTaskDeleted(Task removedTask, int taskCount) {
        return "Noted. I've removed this task:\n"
                + "  " + removedTask + "\n"
                + "Now you have " + taskCount + " tasks in the list.";
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
        boolean isDone = command == Command.MARK;
        setTaskCompletion(task, isDone);
        String response = formatStatusChanged(task, isDone);

        if (!saveTaskStatus(task, wasDone, tasks)) {
            return Strings.SAVE_FAILURE;
        }
        return response;
    }

    /**
     * Applies a completion state for both requested changes and failed-save recovery.
     */
    private static void setTaskCompletion(Task task, boolean isDone) {
        if (isDone) {
            task.mark();
        } else {
            task.unmark();
        }
    }

    /**
     * Saves the changed status, restoring the previous state if saving fails.
     *
     * @return True if saving succeeds, or false after restoring the previous status.
     */
    private boolean saveTaskStatus(Task task, boolean wasDone, TaskList tasks) {
        try {
            Storage.modifyData(storageFilePath, tasks);
        } catch (IOException e) {
            setTaskCompletion(task, wasDone);
            return false;
        }
        return true;
    }

    /**
     * Formats the confirmation for marking or unmarking a task.
     */
    private static String formatStatusChanged(Task task, boolean isDone) {
        return (isDone ? Strings.MARK : Strings.UNMARK) + "\n  " + task;
    }

    /**
     * Creates a task from a valid task-creation command and saves it.
     */
    private String processTaskCreationCommand(String input, TaskList tasks, Command command) {
        TaskValidation validation = parseTaskCreation(input, command);
        if (validation.error() != null) {
            return formatValidationError(validation.error(), input);
        }

        if (tasks.size() >= MAX_TASKS) {
            return "Sir, this will cost too much time";
        }

        Task task = validation.task();
        int duplicateIndex = tasks.indexOfDuplicate(task);
        if (duplicateIndex >= 0) {
            return promptForDuplicate(task, tasks.get(duplicateIndex), duplicateIndex);
        }

        return addTaskAndSave(task, tasks);
    }

    /**
     * Remembers a proposed duplicate and prompts using the existing task's one-based list number.
     */
    private String promptForDuplicate(Task proposedTask, Task existingTask, int existingTaskIndex) {
        pendingDuplicate = new PendingDuplicate(proposedTask, existingTask, existingTaskIndex);
        return "I found an existing task with the same description:\n"
                + "  " + (existingTaskIndex + 1) + "." + existingTask + "\n"
                + "Would you like to keep the new task or the old task?\n"
                + "Please reply with \"new\" or \"old\".";
    }

    /**
     * Saves a new task before adding it to memory, leaving the list unchanged if saving fails.
     */
    private String addTaskAndSave(Task task, TaskList tasks) {
        String response = formatTaskAdded(task, tasks.size() + 1);
        try {
            Storage.saveNewData(storageFilePath, task.toString());
        } catch (IOException e) {
            return Strings.SAVE_FAILURE;
        }
        tasks.add(task);
        return response;
    }

    /**
     * Formats an addition confirmation with the number of tasks after insertion.
     */
    private static String formatTaskAdded(Task task, int taskCount) {
        return "Got it. I've added this task:\n"
                + "  " + task + "\n"
                + "Now you have " + taskCount + " tasks in the list.";
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
     * Holds either a parsed task or its error so successful validation does not require parsing again.
     */
    private record TaskValidation(Task task, ValidationError error) {
        /**
         * Creates a failed validation result without a task.
         */
        private static TaskValidation invalid(ValidationCode code, String detail) {
            return new TaskValidation(null, new ValidationError(code, detail));
        }
    }

    /**
     * Holds a valid zero-based index, or -1 and a non-null error on failure.
     */
    private record IndexValidation(int index, ValidationError error) {
    }
}
