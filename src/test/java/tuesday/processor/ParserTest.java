package tuesday.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tuesday.task.Deadline;
import tuesday.task.Task;
import tuesday.task.TaskList;
import tuesday.task.Todo;

/**
 * Tests command processing for duplicate tasks.
 */
public class ParserTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    public void processCommand_markAndUnmark_preservesResponsesAndSavedStatus() throws IOException {
        Path saveFile = temporaryDirectory.resolve("Tuesday.txt");
        Parser parser = new Parser(saveFile.toString());
        TaskList tasks = new TaskList();
        Task task = new Todo("read book");
        tasks.add(task);

        assertEquals("Nice! I've marked this task as done:\n  [T][X] read book",
                parser.processCommand("mark 1", tasks));
        assertEquals("[T][X] read book" + System.lineSeparator(), Files.readString(saveFile));
        assertSame(task, tasks.get(0));
        assertTrue(task.isDone());

        assertEquals("OK, I've marked this task as not done yet:\n  [T][ ] read book",
                parser.processCommand("unmark 1", tasks));
        assertEquals("[T][ ] read book" + System.lineSeparator(), Files.readString(saveFile));
        assertSame(task, tasks.get(0));
        assertFalse(task.isDone());
    }

    @Test
    public void processCommand_deleteMiddleTask_preservesResponseAndRemainingOrder() throws IOException {
        Path saveFile = temporaryDirectory.resolve("Tuesday.txt");
        Parser parser = new Parser(saveFile.toString());
        TaskList tasks = new TaskList();
        Task firstTask = new Todo("first");
        Task lastTask = new Todo("last");
        tasks.add(firstTask);
        tasks.add(new Todo("middle"));
        tasks.add(lastTask);

        assertEquals("Noted. I've removed this task:\n  [T][ ] middle\nNow you have 2 tasks in the list.",
                parser.processCommand("delete 2", tasks));
        assertEquals(2, tasks.size());
        assertSame(firstTask, tasks.get(0));
        assertSame(lastTask, tasks.get(1));
        assertEquals("[T][ ] first" + System.lineSeparator() + "[T][ ] last" + System.lineSeparator(),
                Files.readString(saveFile));
    }

    @Test
    public void processCommand_deadlines_preservesParsedValuesAndSavedFormat() throws Exception {
        String[] inputs = {"2026-09-20", "10:30", "2026-09-20 10:30"};
        String[] displays = {"Sep 20 2026", "10:30", "Sep 20 2026 10:30"};
        Path saveFile = temporaryDirectory.resolve("Tuesday.txt");
        Parser parser = new Parser(saveFile.toString());
        TaskList tasks = new TaskList();
        StringBuilder expectedSave = new StringBuilder();

        for (int i = 0; i < inputs.length; i++) {
            String expected = "[D][ ] work " + i + " (by: " + displays[i] + ")";
            assertTaskAdded(parser, tasks, "deadline work " + i + " /by " + inputs[i], expected);
            expectedSave.append(expected).append(System.lineSeparator());
        }
        assertSavedTasks(saveFile, tasks, expectedSave.toString());
    }

    @Test
    public void processCommand_events_preservesParsedValuesAndSavedFormat() throws Exception {
        String[] inputs = {"2026-09-20", "10:30", "2026-09-20 10:30"};
        String[] displays = {"Sep 20 2026", "10:30", "Sep 20 2026 10:30"};
        Path saveFile = temporaryDirectory.resolve("Tuesday.txt");
        Parser parser = new Parser(saveFile.toString());
        TaskList tasks = new TaskList();
        StringBuilder expectedSave = new StringBuilder();

        for (int i = 0; i < inputs.length; i++) {
            for (int j = 0; j < inputs.length; j++) {
                String description = "meeting " + i + " " + j;
                String command = "event " + description + " /from " + inputs[i] + " /to " + inputs[j];
                String expected = "[E][ ] " + description + " (from: " + displays[i] + " to: " + displays[j] + ")";
                assertTaskAdded(parser, tasks, command, expected);
                expectedSave.append(expected).append(System.lineSeparator());
            }
        }
        assertSavedTasks(saveFile, tasks, expectedSave.toString());
    }

    /**
     * Checks that validation and command processing agree on the newly added task's display value.
     */
    private static void assertTaskAdded(Parser parser, TaskList tasks, String command, String expected)
            throws Exception {
        assertTrue(Parser.isAvailableTaskCommand(command));
        assertTrue(parser.processCommand(command, tasks).contains(expected));
        assertEquals(expected, tasks.get(tasks.size() - 1).toString());
    }

    /**
     * Checks the complete saved text and reloads it to verify task order and display values.
     */
    private static void assertSavedTasks(Path saveFile, TaskList tasks, String expectedSave) throws IOException {
        assertEquals(expectedSave, Files.readString(saveFile));
        TaskList loadedTasks = new TaskList();
        Storage.loadData(saveFile.toString(), loadedTasks);
        assertEquals(tasks.size(), loadedTasks.size());
        for (int i = 0; i < tasks.size(); i++) {
            assertEquals(tasks.get(i).toString(), loadedTasks.get(i).toString());
        }
    }

    @Test
    public void processCommand_duplicateThenNew_replacesAndSavesTask() throws IOException {
        Path saveFile = temporaryDirectory.resolve("Tuesday.txt");
        Parser parser = new Parser(saveFile.toString());
        TaskList tasks = new TaskList();
        Task existingTask = new Deadline("submit report", LocalDate.of(2026, 9, 10), null);
        existingTask.mark();
        tasks.add(existingTask);

        String prompt = parser.processCommand("todo Submit   Report", tasks);

        assertEquals("I found an existing task with the same description:\n"
                + "  1.[D][X] submit report (by: Sep 10 2026)\n"
                + "Would you like to keep the new task or the old task?\n"
                + "Please reply with \"new\" or \"old\".", prompt);
        assertSame(existingTask, tasks.get(0));
        assertFalse(Files.exists(saveFile));

        String invalidResponse = parser.processCommand("list", tasks);
        assertEquals("Please reply with \"new\" to keep the new task or \"old\" to keep the existing task.",
                invalidResponse);

        String replacementResponse = parser.processCommand("new", tasks);
        assertEquals("Got it. I've replaced this task:\n"
                + "  [D][X] submit report (by: Sep 10 2026)\n"
                + "with:\n"
                + "  [T][ ] Submit   Report\n"
                + "You still have 1 tasks in the list.", replacementResponse);
        assertEquals(1, tasks.size());
        assertInstanceOf(Todo.class, tasks.get(0));
        assertEquals("[T][ ] Submit   Report", tasks.get(0).toString());
        assertEquals("[T][ ] Submit   Report" + System.lineSeparator(), Files.readString(saveFile));
    }

    @Test
    public void processCommand_duplicateThenOld_keepsExistingTaskWithoutSaving() {
        Path saveFile = temporaryDirectory.resolve("Tuesday.txt");
        Parser parser = new Parser(saveFile.toString());
        TaskList tasks = new TaskList();
        Task existingTask = new Todo("read book");
        tasks.add(existingTask);

        parser.processCommand("deadline READ BOOK /by 2026-09-20", tasks);
        String response = parser.processCommand("old", tasks);

        assertEquals("Okay. I've kept this task:\n"
                + "  [T][ ] read book\n"
                + "You still have 1 tasks in the list.", response);
        assertSame(existingTask, tasks.get(0));
        assertFalse(Files.exists(saveFile));
    }

    @Test
    public void processCommand_duplicateThenBye_discardsPendingTask() {
        Parser parser = new Parser(temporaryDirectory.resolve("Tuesday.txt").toString());
        TaskList tasks = new TaskList();
        tasks.add(new Todo("read book"));

        parser.processCommand("todo READ BOOK", tasks);

        assertEquals("Bye. Hope to see you again soon!", parser.processCommand("bye", tasks));
        assertEquals("Sir, what do you mean by old", parser.processCommand("old", tasks));
        assertEquals(1, tasks.size());
    }

    @Test
    public void processCommand_fullListAndDuplicate_reportsCapacityErrorFirst() {
        Parser parser = new Parser(temporaryDirectory.resolve("Tuesday.txt").toString());
        TaskList tasks = new TaskList();
        for (int i = 0; i < 100; i++) {
            tasks.add(new Todo("task " + i));
        }

        assertEquals("Sir, this will cost too much time", parser.processCommand("todo task 0", tasks));
        assertEquals("Sir, what do you mean by old", parser.processCommand("old", tasks));
    }

    @Test
    public void processCommand_uniqueTask_addsAndSavesNormally() throws IOException {
        Path saveFile = temporaryDirectory.resolve("Tuesday.txt");
        Parser parser = new Parser(saveFile.toString());
        TaskList tasks = new TaskList();

        String response = parser.processCommand("todo read textbook", tasks);

        assertEquals("Got it. I've added this task:\n"
                + "  [T][ ] read textbook\n"
                + "Now you have 1 tasks in the list.", response);
        assertEquals("[T][ ] read textbook" + System.lineSeparator(), Files.readString(saveFile));
    }
}
