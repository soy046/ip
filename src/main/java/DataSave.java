import java.io.FileWriter;
import java.io.IOException;
import java.io.File;

/**
 *  This is a class with the method for saving data.
 */
public class DataSave {

    public static final String FILE_PATH = "data/Tuesday.txt";

    /**
     *  A method for Tuesday to save the newly entered data.
     * @param filePath the path of the file which saves data.
     * @param data newly added data
     * @throws IOException resolve the situation when the file does not exist.
     */
    public static void saveNewData(String filePath, String data) throws IOException {
        File file = new File(filePath);
        File parent = file.getParentFile();

        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Unable to create data directory.");
        }

        try (FileWriter fw = new FileWriter(file, true)) {
            fw.write(data);
            fw.write(System.lineSeparator());
        }
    }
}
