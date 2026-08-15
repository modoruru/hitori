package su.hitori.api.util;

import org.jetbrains.annotations.Nullable;

@Deprecated(forRemoval = true)
public final class EnumUtil {

    private EnumUtil() {}

    /**
     * @deprecated use {@link SafeUtil#enumValueOf(Class, String)}
     */
    @Deprecated(forRemoval = true)
    public static <E extends Enum<E>> @Nullable E safeValueOf(Class<E> clazz, String name) {
        return SafeUtil.enumValueOf(clazz, name);
    }

}
