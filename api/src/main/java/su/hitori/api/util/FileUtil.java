package su.hitori.api.util;

import su.hitori.api.Pair;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Utils for files
 */
public final class FileUtil {

    private FileUtil() {}

    /**
     * Simply deletes file, or, if folder is present, recursively deletes contents of folder than deleting folder itself.
     * @param file file to delete
     */
    public static void deleteRecursively(File file) {
        if(!file.exists()) return;

        if(file.isFile()) {
            file.delete();
            return;
        }

        File[] files = file.listFiles();
        assert files != null;

        for (File file1 : files) {
            deleteRecursively(file1);
        }
        file.delete();
    }

    /**
     * Writes text to a file.
     * @param file a file to write to
     * @param text text to write to file
     */
    public static void writeTextToFile(File file, String text) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(text);
            writer.flush();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns name and extension of file separated of each other.
     * @param file file to get name and extension of
     * @return pair containing name of file, and extension (or empty string if file doesn't have extension)
     */
    public static Pair<String, String> getNameAndExtension(File file) {
        String name = file.getName();
        int index = name.lastIndexOf('.');
        String extension = index == -1 ? "" : name.substring(index + 1);
        return new Pair<>(name.substring(0, index), extension);
    }

}
