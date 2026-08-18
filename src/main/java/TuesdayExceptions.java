/**
 *  A class with all the exceptions for Tuesday nested
 */
public class TuesdayExceptions {
    public static class NoDescriptionnException extends Exception {
        public NoDescriptionnException(String message) {
            super(message);
        }
    }

    public static class UnknownCommandException extends Exception {
        public UnknownCommandException(String message) {
            super(message);
        }
    }

    public static class TaskNumberOutRangeException extends Exception {
        public TaskNumberOutRangeException(String message) {
            super(message);
        }
    }

    public static class DeadlineMissingByDateException extends Exception {
        public DeadlineMissingByDateException(String message) {
            super(message);
        }
    }

    public static class EventMissingTimeException extends Exception {
        public EventMissingTimeException(String message) {
            super(message);
        }
    }
}
