package su.hitori.plugin.module.exception;

import org.jspecify.annotations.Nullable;
import su.hitori.plugin.module.ExtendedMeta;

import java.io.IOException;

public final class MetaReadError extends Exception {

    public static final MetaReadError
            OLD_FORMAT_ERROR = new MetaReadError(Type.OLD_FORMAT, null, null, null),
            MISSING_MODULE_JSON = new MetaReadError(Type.MISSING_MODULE_JSON, null, null, null);

    public final Type type;
    public final @Nullable String missingField;
    public final @Nullable IOException ioException;
    public final @Nullable String formatMessage;

    private MetaReadError(Type type, @Nullable String missingField, @Nullable IOException ioException, @Nullable String formatMessage) {
        this.type = type;
        this.missingField = missingField;
        this.ioException = ioException;
        this.formatMessage = formatMessage;
    }

    public static MetaReadError missingField(String missingField) {
        return new MetaReadError(Type.MISSING_FIELD, missingField, null, null);
    }

    public static MetaReadError io(IOException ioException) {
        return new MetaReadError(Type.IO, null, ioException, null);
    }

    public static MetaReadError format(String formatMessage) {
        return new MetaReadError(Type.FORMAT, null, null, formatMessage);
    }

    public enum Type {
        OLD_FORMAT,
        MISSING_FIELD,
        MISSING_MODULE_JSON,
        IO,
        FORMAT
    }

}
