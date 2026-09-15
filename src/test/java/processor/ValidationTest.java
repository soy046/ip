package processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import exceptions.TuesdayExceptions;
import task.Task;
import task.TaskList;
import task.Todo;

/**
 * Preserves validation responses, public contracts, and parsing boundaries.
 */
public class ValidationTest {
    @TempDir
    private Path temporaryDirectory;

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
        Parser parser = new Parser(file.toString());
        for (String[] example : cases) {
            assertEquals(example[1], parser.processCommand(example[0], tasks), example[0]);
        }
        for (String input : new String[] {null, "", "  ", "old", "new", " unknown ",
            "event work /from nonsense /to 11:00"}) {
            assertEquals("Sir, what do you mean by " + input, parser.processCommand(input, tasks));
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
        Parser parser = new Parser(temporaryDirectory.resolve("tasks.txt").toString());
        for (String command : new String[] {"mark", "unmark", "delete"}) {
            String range = "Sir, that " + (command.equals("delete") ? "delete" : "mark")
                    + " number is out of range.";
            for (String argument : new String[] {"0", "-1", "2", "-2147483648", "2147483647", "0 extra"}) {
                assertEquals(range, parser.processCommand(command + " " + argument, tasks));
            }
            for (String argument : new String[] {"", "abc", "2147483648", "-2147483649", "1 extra"}) {
                String input = command + " " + argument;
                assertEquals("Sir, what do you mean by " + input, parser.processCommand(input, tasks));
            }
            assertEquals(range, parser.processCommand(command + " 1", new TaskList()));
        }
        assertEquals(1, tasks.size());
        assertEquals("[T][ ] existing", tasks.get(0).toString());
    }

    @Test
    public void validators_legacyCalls_preservesExceptionsAndPayloads() throws Exception {
        TaskList tasks = new TaskList();
        tasks.add(new Todo("existing"));
        assertTrue(Parser.isAvailableMark("mark +1", tasks));
        assertTrue(Parser.isAvailableMark("unmark 1", tasks));
        assertTrue(Parser.isAvailableMark("\u2003mark\u20031", tasks));
        assertTrue(Parser.isAvailableDelete("\u2003delete\u20031", tasks));
        assertTrue(Parser.isAvailableDelete("delete 1", tasks));
        assertFalse(Parser.isAvailableMark("delete 1", tasks));
        assertFalse(Parser.isAvailableDelete("mark 1", tasks));
        assertFalse(Parser.isAvailableMark("mark 1 extra", tasks));
        assertFalse(Parser.isAvailableTaskCommand(null));
        assertFalse(Parser.isAvailableTaskCommand("list"));
        assertThrows(NullPointerException.class, () -> Parser.isAvailableMark(null, tasks));
        assertThrows(NullPointerException.class, () -> Parser.isAvailableDelete(null, tasks));
        assertEquals("0", assertThrows(TuesdayExceptions.MarkTaskNumberOutOfRangeException.class, () ->
                Parser.isAvailableMark("mark 0 extra", tasks)).getMessage());
        assertEquals("2", assertThrows(TuesdayExceptions.DeleteTaskNumberOutOfRangeException.class, () ->
                Parser.isAvailableDelete("delete 2", tasks)).getMessage());
        for (String command : new String[] {"todo", "deadline", "event"}) {
            assertEquals(command, assertThrows(TuesdayExceptions.NoDescriptionnException.class, () ->
                    Parser.isAvailableTaskCommand(command)).getMessage());
        }
        assertEquals("", assertThrows(TuesdayExceptions.DeadlineMissingByDateException.class, () ->
                Parser.isAvailableTaskCommand("deadline work")).getMessage());
        assertEquals("bad", assertThrows(TuesdayExceptions.DeadlineMissingByDateException.class, () ->
                Parser.isAvailableTaskCommand("deadline work /by bad")).getMessage());
        assertEquals("event", assertThrows(TuesdayExceptions.EventMissingTimeException.class, () ->
                Parser.isAvailableTaskCommand("event work /from 10:00")).getMessage());
    }

    @Test
    public void parseDateTime_calendarAndSyntaxBoundaries_preservesAcceptedValues() {
        for (String value : new String[] {"0000-02-29", "2000-02-29", "2024-02-29", "23:59", "00:00",
            "2026-09-20 10:00"}) {
            assertNotNull(Parser.parseDateTime(value), value);
        }
        for (String value : new String[] {"1900-02-29", "2025-02-29", "2026-00-10", "2026-13-10",
            "2026-01-00", "2026-04-31", "24:00", "12:60", "1:00", "2026-9-20", " 10:00",
            "2026-09-20  10:00", "2026-09-20T10:00", ""}) {
            assertNull(Parser.parseDateTime(value), value);
        }
    }

    @Test
    public void processCommand_fullList_validatesBeforeCapacity() {
        TaskList tasks = new TaskList();
        for (int i = 0; i < 100; i++) {
            tasks.add(new Todo("task " + i));
        }
        Parser parser = new Parser(temporaryDirectory.resolve("tasks.txt").toString());
        assertEquals("please add description, sir!", parser.processCommand("todo", tasks));
        assertEquals("Sir, this will cost too much time", parser.processCommand("todo task 0", tasks));
        assertEquals("Sir, what do you mean by old", parser.processCommand("old", tasks));
    }

    @Test
    public void processCommand_capacityBoundary_acceptsLastSlotOnly() throws IOException {
        TaskList tasks = new TaskList();
        for (int i = 0; i < 99; i++) {
            tasks.add(new Todo("task " + i));
        }
        Path file = temporaryDirectory.resolve("tasks.txt");
        Storage.modifyData(file.toString(), tasks);
        Parser parser = new Parser(file.toString());
        assertTrue(parser.processCommand("todo last slot", tasks).startsWith("Got it. I've added"));
        String saved = Files.readString(file);
        assertEquals(100, tasks.size());
        assertEquals("Sir, this will cost too much time", parser.processCommand("todo overflow", tasks));
        assertEquals(saved, Files.readString(file));
    }
}
