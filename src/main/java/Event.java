import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Event extends Task{
    public static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMM dd yyyy", Locale.ENGLISH);
    public static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeValue start;
    private final DateTimeValue end;

    public Event(String name, DateTimeValue start, DateTimeValue end) {
        super(name);
        this.start = start;
        this.end = end;
    }

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
