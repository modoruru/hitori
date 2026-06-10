package su.hitori.api.util;

import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

public final class SafeUtil {

    private SafeUtil() {
    }

    public static <E extends Enum<E>> @Nullable E enumValueOf(Class<E> clazz, String name) {
        return wrapTry(() -> Enum.valueOf(clazz, name));
    }

    public static @Nullable Integer parseInt(String string) {
        return wrapParse(Integer::parseInt, string);
    }

    public static @Nullable Byte parseByte(String string) {
        return wrapParse(Byte::parseByte, string);
    }

    public static @Nullable Short parseShort(String string) {
        return wrapParse(Short::parseShort, string);
    }

    public static @Nullable Long parseLong(String string) {
        return wrapParse(Long::parseLong, string);
    }

    public static @Nullable Float parseFloat(String string) {
        return wrapParse(Float::parseFloat, string);
    }

    public static @Nullable Double parseDouble(String string) {
        return wrapParse(Double::parseDouble, string);
    }

    public static @Nullable Boolean parseBoolean(String string) {
        return wrapParse(Boolean::parseBoolean, string);
    }

    private static <E> @Nullable E wrapParse(Function<String, E> parseFunction, String string) {
        try {
            return parseFunction.apply(string);
        }
        catch (Throwable _) {
            return null;
        }
    }

    private static <E> @Nullable E wrapTry(Supplier<E> supplier) {
        try {
            return supplier.get();
        }
        catch (Throwable _) {
            return null;
        }
    }


}
