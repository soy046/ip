import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Deadline extends Task{
    public static final DateTimeFormatter DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("MMM dd yyyy", Locale.ENGLISH);
    private final LocalDate deadline;

    public Deadline(String name, LocalDate deadline) {
        super(name);
        this.deadline = deadline;
    }

    @Override
    public String toString() {
        return "[D]" + super.toString() + " (by: "
                + this.deadline.format(DISPLAY_FORMATTER) + ")";
    }
}
