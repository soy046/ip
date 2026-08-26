package task;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Represents a task with a start and end date-time.
 */
public class Event extends Task {
    /** Formats dates in the task display format. */
    public static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMM dd yyyy", Locale.ENGLISH);
    /** Formats times in the task display format. */
    public static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeValue start;
    private final DateTimeValue end;

    /**
     * Creates an event task.
     *
     * @param name the task description.
     * @param start the event start date-time.
     * @param end the event end date-time.
     */
    public Event(String name, DateTimeValue start, DateTimeValue end) {
        super(name);
        this.start = start;
        this.end = end;
    }

    /**
     * Returns the task in its saved display format.
     *
     * @return the formatted event task.
     */
    @Override
    public String toString() {
        return "[E]" + super.toString() + " (from: "
                + format(start) + " to: " + format(end) + ")";
    }

    private static String format(DateTimeValue value) {
        String result = "";
        if (value.date() != null) {
            result = value.date().format(DATE_FORMATTER);
        }
        if (value.time() != null) {
            if (!result.isEmpty()) {
                result += " ";
            }
            result += value.time().format(TIME_FORMATTER);
        }
        return result;
    }

    /**
     * Stores the optional date and time components of one event boundary.
     */
    public record DateTimeValue(LocalDate date, LocalTime time) {
    }
}
