package su.hitori.api.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * IO utils
 */
public final class IOUtil {

    private IOUtil() {}

    /**
     * Reads InputStream to byte array
     * @param is input stream to read from
     * @return read byte[]
     * @throws IOException if an I/O error occurs while reading or flushing
     */
    public static byte[] readInputStream(InputStream is) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            if(is == null) throw new RuntimeException();

            int read;
            byte[] buffer = new byte[1024];
            while ((read = is.read(buffer, 0, buffer.length)) != -1) {
                baos.write(buffer, 0, read);
            }

            baos.flush();
            return baos.toByteArray();
        }
        catch (Throwable e) {
            throw new IOException(e);
        }
    }

}
