package processor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import task.TaskList;

/**
 * This is a class with the method for saving data.
 */
public class DataSave {

    /** Stores the path used for the task save file. */
    public static final String FILE_PATH = "data" + File.separator + "Tuesday.txt";

    /**
     * A method for Tuesday to save the newly entered data.
     *
     * @param filePath the path of the file which saves data.
     * @param data     newly added data
     * @throws IOException indicate that the file cannot be created.
     */
    public static void saveNewData(String filePath, String data) throws IOException {
        File file = new File(filePath);
        File parent = file.getParentFile();

        // create the directory if not exists
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Unable to create data directory.");
        }

        // try with resource to close the writer automatically
        try (FileWriter fw = new FileWriter(file, true)) {
            fw.write(data);
            fw.write(System.lineSeparator());
        }
    }

    /**
     * This method is used for modifying the data saved
     *
     * @param filePath  the location of the file saving data
     * @param tasks     an ArrayList which stores the data temporarily
     * @param taskCount the number of tasks in the ArrayList
     * @throws IOException indicate that the file cannot be created at the certain location
     */
    public static void modifyData(String filePath, TaskList tasks, int taskCount) throws IOException {
        File file = new File(filePath);
        File parent = file.getParentFile();

        // create the directory if not exists
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Unable to create data directory.");
        }

        String data = "";
        for (int i = 0; i < taskCount; i++) {
            data = data + tasks.get(i).toString() + System.lineSeparator();
        }

        // try with resource to close the writer automatically
        try (FileWriter fw = new FileWriter(file, false)) {
            fw.write(data);
        }
    }
}
