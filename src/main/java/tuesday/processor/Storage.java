package tuesday.processor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import tuesday.task.DateTimeFormat;
import tuesday.task.Deadline;
import tuesday.task.Event;
import tuesday.task.Task;
import tuesday.task.TaskList;
import tuesday.task.Todo;

/**
 * Loads and saves Tuesday's task data.
 */
public final class Storage {

    /** Stores the path used for the task save file. */
    public static final String FILE_PATH = "data" + File.separator + "Tuesday.txt";

    private Storage() {
    }

    /**
     * Loads valid saved tasks from a file into a task list.
     *
     * @param filePath the path of the saved task file.
     * @param tasks the list to which loaded tasks are added.
     * @throws NoSuchFileException if the save file does not exist.
     * @throws IOException if opening, reading, or closing the save file fails.
     */
    public static void loadData(String filePath, TaskList tasks) throws IOException {
        loadData(tasks, () -> Files.newBufferedReader(Path.of(filePath), Charset.defaultCharset()));
    }

    /**
     * Stages loaded tasks until reading and closing succeed, leaving the caller unchanged on failure.
     */
    static void loadData(TaskList tasks, ReaderSource source) throws IOException {
        ArrayList<Task> loadedTasks = new ArrayList<>();
        try (BufferedReader reader = source.open()) {
            String line;
            while ((line = reader.readLine()) != null) {
                Task task = toTask(line);
                if (task != null) {
                    loadedTasks.add(task);
                }
            }
        }
        for (Task task : loadedTasks) {
            tasks.add(task);
        }
    }

    /**
     * Appends serialized task data to the save file.
     *
     * @param filePath the path of the saved task file.
     * @param data the serialized task data to append.
     * @throws IOException if the data cannot be saved.
     */
    public static void saveNewData(String filePath, String data) throws IOException {
        Path file = Path.of(filePath);
        byte[] existing;
        try {
            existing = Files.readAllBytes(file);
        } catch (NoSuchFileException exception) {
            existing = new byte[0];
        }
        byte[] addition = (data + System.lineSeparator()).getBytes(Charset.defaultCharset());
        byte[] combined = Arrays.copyOf(existing, existing.length + addition.length);
        System.arraycopy(addition, 0, combined, existing.length, addition.length);
        new AtomicFileWriter().write(file, combined);
    }

    /**
     * Replaces the save file contents with the current tasks.
     *
     * @param filePath the path of the saved task file.
     * @param tasks the tasks to save.
     * @throws IOException if the data cannot be saved.
     */
    public static void modifyData(String filePath, TaskList tasks) throws IOException {
        String data = tasks.stream()
                .map(task -> task.toString() + System.lineSeparator())
                .collect(Collectors.joining());

        new AtomicFileWriter().write(Path.of(filePath), data.getBytes(Charset.defaultCharset()));
    }

    /**
     * Parses a date-time value stored in the task file's display format.
     */
    private static Event.DateTimeValue parseSavedDateTime(String value) {
        try {
            if (!value.contains(":")) {
                return new Event.DateTimeValue(LocalDate.parse(value, DateTimeFormat.DATE_FORMATTER), null);
            }
            int separator = value.lastIndexOf(' ');
            if (separator < 0) {
                return new Event.DateTimeValue(null, LocalTime.parse(value, DateTimeFormat.TIME_FORMATTER));
            }
            if (separator == 0 || separator == value.length() - 1) {
                return null;
            }
            return new Event.DateTimeValue(
                    LocalDate.parse(value.substring(0, separator), DateTimeFormat.DATE_FORMATTER),
                    LocalTime.parse(value.substring(separator + 1), DateTimeFormat.TIME_FORMATTER));
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    /**
     * Converts saved task data back into its corresponding task object.
     */
    private static Task toTask(String taskData) {
        if (taskData == null) {
            return null;
        }

        String data = taskData.trim();
        if (!hasValidSavedTaskStructure(data)) {
            return null;
        }

        char status = data.charAt(4);
        if (!hasValidSavedTaskStatus(status)) {
            return null;
        }

        Task task = parseSavedTask(data.charAt(1), data.substring(7));
        if (task == null) {
            return null;
        }

        if (status == 'X') {
            task.mark();
        }
        return task;
    }

    /**
     * Checks that saved task data has the expected type and status brackets.
     */
    private static boolean hasValidSavedTaskStructure(String data) {
        return data.length() >= 8
                && data.charAt(0) == '['
                && data.charAt(2) == ']'
                && data.charAt(3) == '['
                && data.charAt(5) == ']'
                && data.charAt(6) == ' ';
    }

    /**
     * Checks whether a saved task status represents a completed or incomplete task.
     */
    private static boolean hasValidSavedTaskStatus(char status) {
        return status == 'X' || status == ' ';
    }

    /**
     * Parses saved task details according to their task type.
     */
    private static Task parseSavedTask(char type, String details) {
        return switch (type) {
            case 'T' -> parseSavedTodo(details);
            case 'D' -> parseSavedDeadline(details);
            case 'E' -> parseSavedEvent(details);
            default -> null;
        };
    }

    /**
     * Creates a todo from its saved description.
     */
    private static Todo parseSavedTodo(String details) {
        return new Todo(details);
    }

    /**
     * Parses the description and date-time of a saved deadline.
     */
    private static Deadline parseSavedDeadline(String details) {
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
        return new Deadline(description, deadline.date(), deadline.time());
    }

    /**
     * Parses the description, start, and end of a saved event.
     */
    private static Event parseSavedEvent(String details) {
        String fromMarker = " (from: ";
        String toMarker = " to: ";
        int fromIndex = details.indexOf(fromMarker);
        int toIndex = details.lastIndexOf(toMarker);

        if (fromIndex < 0 || toIndex < fromIndex + fromMarker.length() || !details.endsWith(")")) {
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
        return new Event(description, start, end);
    }

    /**
     * Opens a reader, allowing tests to reproduce failures during reading and closing.
     */
    @FunctionalInterface
    interface ReaderSource {
        /**
         * Opens the input whose ownership transfers to the loader.
         *
         * @return the reader to consume and close.
         * @throws IOException if opening fails.
         */
        BufferedReader open() throws IOException;
    }
}
