package su.hitori.api.util;

@SuppressWarnings("unchecked")
public final class UnsafeUtil {

    private UnsafeUtil() {}

    // created for blaming in all cast issues

    /**
     * Tries to cast passed argument to E type. On fail, returns null.
     */
    public static <E> E cast(Object object) {
        try {
            return (E) object;
        }
        catch (Throwable _) {
            return null;
        }
    }

}
