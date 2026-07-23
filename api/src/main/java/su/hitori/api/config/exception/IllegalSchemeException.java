package su.hitori.api.config.exception;

public final class IllegalSchemeException extends IllegalStateException {

    public IllegalSchemeException(Throwable cause, String format, Object... args) {
        super(String.format(format, args), cause);
    }

    public IllegalSchemeException(String format, Object... args) {
        super(String.format(format, args));
    }

}
