package su.hitori.api.util;

import org.jspecify.annotations.Nullable;

@SuppressWarnings("unchecked")
public final class UnsafeUtil {

    private UnsafeUtil() {}

    // created for blaming in all cast issues

    /**
     * Tries to cast passed argument to E type. On fail, returns null.
     */
    @SuppressWarnings("DataFlowIssue") // intellij idea gone crazy
    public static <E> @Nullable E cast(Object object) {
        try {
            return (E) object;
        }
        catch (Throwable _) {
            return null;
        }
    }

}
