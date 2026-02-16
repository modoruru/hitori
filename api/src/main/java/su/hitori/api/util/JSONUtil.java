package su.hitori.api.util;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;

/**
 * JSON utils for {@code org.json} library
 */
public final class JSONUtil {

    private JSONUtil() {
    }

    /**
     * Reads JSONObject from reader.
     * @param reader reader to read from
     * @return read JSONObject
     */
    public static JSONObject read(Reader reader) {
        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            StringBuilder builder = new StringBuilder();

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                builder.append(line).append('\n');
            }

            return new JSONObject(builder.toString());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads JSONObject from file
     * @param file file to read from
     * @return read JSONObject
     */
    public static JSONObject readFile(File file) {
        if(!file.exists()) return new JSONObject();

        try (FileReader reader = new FileReader(file)) {
            return read(reader);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
