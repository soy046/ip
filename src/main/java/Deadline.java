import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Deadline extends Task{
    public static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMM dd yyyy", Locale.ENGLISH);
    public static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");
    private final LocalDate deadlineDate;
    private final LocalTime deadlineTime;

    public Deadline(String name, LocalDate deadlineDate, LocalTime deadlineTime) {
        super(name);
        this.deadlineDate = deadlineDate;
        this.deadlineTime = deadlineTime;
    }

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
