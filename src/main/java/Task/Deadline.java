package task;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Represents a task with an optional deadline date and time.
 */
public class Deadline extends Task {
    /** Formats dates in the task display format. */
    public static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMM dd yyyy", Locale.ENGLISH);
    /** Formats times in the task display format. */
    public static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");
    private final LocalDate deadlineDate;
    private final LocalTime deadlineTime;

    /**
     * Creates a deadline task.
     *
     * @param name the task description.
     * @param deadlineDate the optional deadline date.
     * @param deadlineTime the optional deadline time.
     */
    public Deadline(String name, LocalDate deadlineDate, LocalTime deadlineTime) {
        super(name);
        this.deadlineDate = deadlineDate;
        this.deadlineTime = deadlineTime;
    }

    /**
     * Returns the task in its saved display format.
     *
     * @return the formatted deadline task.
     */
    @Override
    public String toString() {
        String deadline = "";
        if (deadlineDate != null) {
            deadline = deadlineDate.format(DATE_FORMATTER);
        }
        if (deadlineTime != null) {
            if (!deadline.isEmpty()) {
                deadline += " ";
            }
            deadline += deadlineTime.format(TIME_FORMATTER);
        }
        return "[D]" + super.toString() + " (by: " + deadline + ")";
    }
}
