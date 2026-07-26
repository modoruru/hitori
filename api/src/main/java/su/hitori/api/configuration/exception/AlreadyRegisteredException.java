package su.hitori.api.configuration.exception;

public class AlreadyRegisteredException extends IllegalArgumentException {

    public AlreadyRegisteredException(String message) {
        super(message);
    }

}
