package processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import task.Deadline;
import task.Task;
import task.TaskList;
import task.Todo;

/**
 * Tests command processing for duplicate tasks.
 */
public class ParserTest {
    @TempDir
    private Path temporaryDirectory;

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
