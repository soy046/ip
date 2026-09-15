package processor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import task.Deadline;
import task.Event;
import task.Task;
import task.TaskList;
import task.Todo;
import ui.Strings;

/**
 * Verifies persistence compatibility and unchanged task state after I/O failures.
 */
public class PersistenceTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    public void processCommand_saveFailure_restoresEveryAffectedTask() throws IOException {
        Path blockedParent = temporaryDirectory.resolve("blocked");
        Files.writeString(blockedParent, "original bytes");
        Parser parser = new Parser(blockedParent.resolve("tasks.txt").toString());
        for (boolean wasDone : new boolean[] {false, true}) {
            TaskList tasks = new TaskList();
            Task first = new Todo("first");
            Task second = new Todo("second");
            tasks.add(first);
            tasks.add(second);
            if (wasDone) {
                first.mark();
            }
            for (String input : new String[] {"todo third", "delete 1", "delete 2", "mark 1", "unmark 1"}) {
                assertEquals(Strings.SAVE_FAILURE, parser.processCommand(input, tasks));
                assertEquals(2, tasks.size());
                assertSame(first, tasks.get(0));
                assertSame(second, tasks.get(1));
                assertEquals(wasDone, first.isDone());
                assertEquals("original bytes", Files.readString(blockedParent));
            }
            parser.processCommand("todo FIRST", tasks);
            assertTrue(parser.processCommand("new", tasks).endsWith("The existing task was kept."));
            assertSame(first, tasks.get(0));
            assertEquals(wasDone, first.isDone());
            assertEquals("Sir, what do you mean by old", parser.processCommand("old", tasks));
        }
    }

    @Test
    public void loadData_readOrCloseFailure_keepsCallerUnchanged() {
        for (boolean failsOnClose : new boolean[] {false, true}) {
            TaskList tasks = new TaskList();
            Task original = new Todo("original");
            tasks.add(original);
            IOException failure = new IOException("read or close failed");
            BufferedReader reader = new BufferedReader(new StringReader("[T][ ] staged\n")) {
                private boolean hasRead;

                @Override
                public String readLine() throws IOException {
                    if (hasRead && !failsOnClose) {
                        throw failure;
                    }
                    hasRead = true;
                    return super.readLine();
                }

                @Override
                public void close() throws IOException {
                    super.close();
                    if (failsOnClose) {
                        throw failure;
                    }
                }
            };
            assertSame(failure, assertThrows(IOException.class, () -> Storage.loadData(tasks, () -> reader)));
            assertEquals(1, tasks.size());
            assertSame(original, tasks.get(0));
        }
    }

    @Test
    public void loadData_missingFileOrDirectory_reportsDifferentFailures() {
        TaskList tasks = new TaskList();
        assertThrows(NoSuchFileException.class, () ->
                Storage.loadData(temporaryDirectory.resolve("missing.txt").toString(), tasks));
        IOException failure = assertThrows(IOException.class, () ->
                Storage.loadData(temporaryDirectory.toString(), tasks));
        assertFalse(failure instanceof NoSuchFileException);
        assertEquals(0, tasks.size());
    }

    @Test
    public void saveNewData_existingRawContent_preservesBytesAndAppends() throws IOException {
        Path target = temporaryDirectory.resolve("tasks.txt");
        String original = "[T][ ] café\r\nmalformed row\n[T][X] café\n";
        Files.write(target, original.getBytes(Charset.defaultCharset()));
        Storage.saveNewData(target.toString(), "[T][ ] new");
        assertArrayEquals((original + "[T][ ] new" + System.lineSeparator()).getBytes(Charset.defaultCharset()),
                Files.readAllBytes(target));
    }

    @Test
    public void modifyData_allDateTimeForms_roundTripsWithStatusAndDuplicates() throws IOException {
        Path target = temporaryDirectory.resolve("tasks.txt");
        TaskList tasks = new TaskList();
        Event.DateTimeValue[] values = {
            new Event.DateTimeValue(LocalDate.of(2026, 9, 20), null),
            new Event.DateTimeValue(null, LocalTime.of(10, 30)),
            new Event.DateTimeValue(LocalDate.of(2026, 9, 20), LocalTime.of(10, 30))
        };
        for (Event.DateTimeValue from : values) {
            Task deadline = new Deadline("duplicate", from.date(), from.time());
            deadline.mark();
            tasks.add(deadline);
            for (Event.DateTimeValue to : values) {
                tasks.add(new Event("duplicate", from, to));
            }
        }
        Storage.modifyData(target.toString(), tasks);
        TaskList loaded = new TaskList();
        Storage.loadData(target.toString(), loaded);
        assertEquals(tasks.size(), loaded.size());
        for (int i = 0; i < tasks.size(); i++) {
            assertEquals(tasks.get(i).toString(), loaded.get(i).toString());
        }
    }

    @Test
    public void loadData_smartSavedValuesAndMalformedRows_preservesCompatibility() throws IOException {
        Path target = temporaryDirectory.resolve("tasks.txt");
        Files.writeString(target, "[D][ ] date (by: Feb 29 2025)\n[D][ ] time (by: 24:00)\n"
                + "[D][ ] both (by: Sep 20 2026 24:00)\n"
                + "[?][ ] bad\n[T][?] bad\n[T]bad\n[D][ ] bad (by: nonsense)\n"
                + "[E][ ] bad (from: 10:00 to: bad)\n[T][ ] last\n");
        TaskList tasks = new TaskList();
        Storage.loadData(target.toString(), tasks);
        assertEquals(4, tasks.size());
        assertEquals("[D][ ] date (by: Feb 28 2025)", tasks.get(0).toString());
        assertEquals("[D][ ] time (by: 00:00)", tasks.get(1).toString());
        assertEquals("[D][ ] both (by: Sep 20 2026 00:00)", tasks.get(2).toString());
        assertEquals("[T][ ] last", tasks.get(3).toString());
    }

    @Test
    public void processCommand_successfulChanges_persistBeforeConfirmation() throws IOException {
        Path target = temporaryDirectory.resolve("tasks.txt");
        Parser parser = new Parser(target.toString());
        TaskList tasks = new TaskList();
        parser.processCommand("todo first", tasks);
        parser.processCommand("todo second", tasks);
        assertTrue(parser.processCommand("mark +1", tasks).startsWith(Strings.MARK));
        assertTrue(Files.readString(target).startsWith("[T][X] first"));
        assertTrue(parser.processCommand("unmark 1", tasks).startsWith(Strings.UNMARK));
        assertTrue(Files.readString(target).startsWith("[T][ ] first"));
        assertTrue(parser.processCommand("delete 1", tasks).startsWith("Noted."));
        assertEquals("[T][ ] second" + System.lineSeparator(), Files.readString(target));
    }
}
