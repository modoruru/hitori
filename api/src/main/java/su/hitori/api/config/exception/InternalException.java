package su.hitori.api.config.exception;

public final class InternalException extends IllegalStateException {

    public InternalException() {
        super();
    }

    public InternalException(String message) {
        super(message);
    }

    public static InternalException formatted(String format, Object... args) {
        return new InternalException(String.format(format, args));
    }

}
