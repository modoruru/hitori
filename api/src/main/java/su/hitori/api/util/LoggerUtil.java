package su.hitori.api.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class LoggerUtil {

    private LoggerUtil() {
    }

    public static String exceptionToString(Throwable throwable) {
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        throwable.printStackTrace(printWriter);
        return writer.toString();
    }

}
