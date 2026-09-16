package tuesday.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tuesday.exceptions.TuesdayExceptions;
import tuesday.task.Task;
import tuesday.task.TaskList;
import tuesday.task.Todo;

/**
 * Preserves command validation responses, error precedence, and task state.
 */
public class ValidationTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    public void processCommand_missingOrRepeatedEventMarkers_preservesTimeErrorAndState() throws Exception {
        String[] inputs = {
            "event work",
            "event work /to 11:00",
            "event work /from 10:00 /to",
            "event work /from/to 11:00",
            "event work /from /to",
            "event work /from 10:00 /to 11:00 /to 12:00"
        };
        Path saveFile = temporaryDirectory.resolve("tasks.txt");
        CommandProcessor commandProcessor = new CommandProcessor(saveFile.toString());
        TaskList tasks = new TaskList();
        Task existingTask = new Todo("existing");
        tasks.add(existingTask);

        for (String input : inputs) {
            assertEquals("please add both starting and ending times, sir!",
                    commandProcessor.processCommand(input, tasks), input);
            assertThrows(TuesdayExceptions.EventMissingTimeException.class, () ->
                    UserInputParser.isAvailableTaskCommand(input), input);
        }
        assertEquals(1, tasks.size());
        assertSame(existingTask, tasks.get(0));
        assertFalse(Files.exists(saveFile));
    }

    @Test
    public void processCommand_invalidInputs_preservesResponseAndState() throws IOException {
        String[][] cases = {
            {"todo", "please add description, sir!"},
            {"deadline", "please add description, sir!"},
            {"deadline /by 2026-09-20", "please add description, sir!"},
            {"deadline work", "please add a deadline date, sir!"},
            {"deadline work /by", "please add a deadline date, sir!"},
            {"deadline work /by nonsense", "please add a deadline date, sir!"},
            {"event", "please add description, sir!"},
            {"event /from 10:00 /to 11:00", "please add description, sir!"},
            {"event work /from 10:00", "please add both starting and ending times, sir!"},
            {"event work /to 11:00 /from 10:00", "please add both starting and ending times, sir!"},
            {"event work /from /to 11:00", "please add both starting and ending times, sir!"},
            {"event work /from 10:00 /from 10:30 /to 11:00",
                "please add both starting and ending times, sir!"}
        };
        Path file = temporaryDirectory.resolve("tasks.txt");
        Files.writeString(file, "original bytes");
        TaskList tasks = new TaskList();
        Task existing = new Todo("existing");
        tasks.add(existing);
        CommandProcessor commandProcessor = new CommandProcessor(file.toString());
        for (String[] example : cases) {
            assertEquals(example[1], commandProcessor.processCommand(example[0], tasks), example[0]);
        }
        for (String input : new String[] {null, "", "  ", "old", "new", " unknown ",
                "event work /from nonsense /to 11:00"}) {
            assertEquals("Sir, what do you mean by " + input, commandProcessor.processCommand(input, tasks));
        }
        assertEquals(1, tasks.size());
        assertSame(existing, tasks.get(0));
        assertEquals("[T][ ] existing", existing.toString());
        assertEquals("original bytes", Files.readString(file));
    }

    @Test
    public void processCommand_invalidIndices_preservesErrorPrecedence() {
        TaskList tasks = new TaskList();
        tasks.add(new Todo("existing"));
        CommandProcessor commandProcessor = new CommandProcessor(temporaryDirectory.resolve("tasks.txt").toString());
        for (String command : new String[] {"mark", "unmark", "delete"}) {
            String range = "Sir, that " + (command.equals("delete") ? "delete" : "mark")
                    + " number is out of range.";
            for (String argument : new String[] {"0", "-1", "2", "-2147483648", "2147483647", "0 extra"}) {
                assertEquals(range, commandProcessor.processCommand(command + " " + argument, tasks));
            }
            for (String argument : new String[] {"", "abc", "2147483648", "-2147483649", "1 extra"}) {
                String input = command + " " + argument;
                assertEquals("Sir, what do you mean by " + input, commandProcessor.processCommand(input, tasks));
            }
            assertEquals(range, commandProcessor.processCommand(command + " 1", new TaskList()));
        }
        assertEquals(1, tasks.size());
        assertEquals("[T][ ] existing", tasks.get(0).toString());
    }

    @Test
    public void processCommand_fullList_validatesBeforeCapacity() {
        TaskList tasks = new TaskList();
        for (int i = 0; i < 100; i++) {
            tasks.add(new Todo("task " + i));
        }
        CommandProcessor commandProcessor = new CommandProcessor(temporaryDirectory.resolve("tasks.txt").toString());
        assertEquals("please add description, sir!", commandProcessor.processCommand("todo", tasks));
        assertEquals("Sir, this will cost too much time", commandProcessor.processCommand("todo task 0", tasks));
        assertEquals("Sir, what do you mean by old", commandProcessor.processCommand("old", tasks));
    }

    @Test
    public void processCommand_capacityBoundary_acceptsLastSlotOnly() throws IOException {
        TaskList tasks = new TaskList();
        for (int i = 0; i < 99; i++) {
            tasks.add(new Todo("task " + i));
        }
        Path file = temporaryDirectory.resolve("tasks.txt");
        Storage.modifyData(file.toString(), tasks);
        CommandProcessor commandProcessor = new CommandProcessor(file.toString());
        assertTrue(commandProcessor.processCommand("todo last slot", tasks).startsWith("Got it. I've added"));
        String saved = Files.readString(file);
        assertEquals(100, tasks.size());
        assertEquals("Sir, this will cost too much time", commandProcessor.processCommand("todo overflow", tasks));
        assertEquals(saved, Files.readString(file));
    }
}
