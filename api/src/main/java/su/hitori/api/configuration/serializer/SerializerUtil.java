package su.hitori.api.configuration.serializer;

import java.util.function.Function;

public final class SerializerUtil {

    private SerializerUtil() {}

    public static <N extends Number> N parseNumber(Object value, Function<Number, N> numberValueFunction, Function<String, N> parseFunction) {
        switch (value) {
            case Number number -> {
                return numberValueFunction.apply(number);
            }
            case String string -> {
                return parseFunction.apply(string);
            }
            default -> {}
        }

        return null;
    }

}
