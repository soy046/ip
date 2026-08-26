package task;

/**
 * Represents a task entered by the user and whether it has been completed.
 */
public class Task {
    private boolean isDone;
    private final String name;

    /**
     * Creates a task.
     *
     * @param name the name of the task.
     */
    public Task(String name) {
        this.name = name;
        this.isDone = false;
    }

    /** Marks the task as done. */
    public void mark() {
        this.isDone = true;
    }

    /** Marks the task as not done. */
    public void unMark() {
        this.isDone = false;
    }

    /**
     * Returns the task's string representation.
     *
     * @return the task's string representation.
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
