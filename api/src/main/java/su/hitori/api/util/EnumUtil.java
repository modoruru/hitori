package su.hitori.api.util;

public final class EnumUtil {

    private EnumUtil() {}

    public static <E extends Enum<E>> E safeValueOf(Class<E> clazz, String name) {
        try {
            return Enum.valueOf(clazz, name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
