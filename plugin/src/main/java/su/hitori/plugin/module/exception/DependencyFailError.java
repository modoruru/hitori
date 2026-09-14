package su.hitori.plugin.module.exception;

public class DependencyFailError extends Exception {

    public final String error;

    public DependencyFailError(String error) {
        super(error);
        this.error = error;
    }

}
