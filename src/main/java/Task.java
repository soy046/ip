public class Task {
    private boolean isDone;
    private String name;

    /**
     * Constructor for a task
     *
     * @param name
     */
    public Task(String name) {
        this.name = name;
        this.isDone = false;
    }

    @Override
    public String toString() {
        if (isDone) {
            return "[X] " + this.name;
        } else {
            return "[ ] " + this.name;
        }
    }
}
