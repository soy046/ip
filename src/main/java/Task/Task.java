package task;

import java.util.Locale;

/**
 * Represents a task entered by the user and whether it has been completed.
 */
public class Task {
    private boolean isDone;
    private final String name;

    /**
     * Constructor for a task
     *
     * @param name： the name of the task
     */
    public Task(String name) {
        this.name = name;
        this.isDone = false;
    }

    /**
     *  Mark the task as done
     */
    public void mark() {
        this.isDone = true;
    }

    /**
     *  Unmark the task as not done
     */
    public void unMark() {
        this.isDone = false;
    }

    /**
     * Checks whether the task description contains a keyword.
     *
     * @param keyword the keyword to search for.
     * @return true if the description contains the keyword, ignoring case.
     */
    public boolean matchesDescription(String keyword) {
        return this.name.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    /**
     * Give the Task object string representation for both cases
     *
     * @return Task object's string representation
     */
    @Override
    public String toString() {
        if (isDone) {
            return "[X] " + this.name;
        } else {
            return "[ ] " + this.name;
        }
    }
}
