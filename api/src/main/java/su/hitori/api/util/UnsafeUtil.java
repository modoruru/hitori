package su.hitori.api.util;

@SuppressWarnings("unchecked")
public final class UnsafeUtil {

    private UnsafeUtil() {}

    // created for blaming in all cast issues
    public static <E> E cast(Object object) {
        try {
            return (E) object;
        }
        catch (Throwable ex) {
            return null;
        }
    }

}
