package su.hitori.api.configuration;

import org.jspecify.annotations.Nullable;
import su.hitori.api.configuration.exception.InternalException;
import su.hitori.api.util.UnsafeUtil;

import java.util.List;

public final class Field<T> {

    public static final Class<?>[] PRIMITIVES_CLASSES = new Class[]{
            byte.class, Byte.class,
            short.class, Short.class,
            int.class, Integer.class,
            long.class, Long.class,
            float.class, Float.class,
            double.class, Double.class,
            boolean.class, Boolean.class,
            char.class, Character.class,

            String.class,
            Enum.class
    };

    final Class<T> type;
    final @Nullable Class<?> listType;
    final T defaultValue;

    private HitoriConfiguration.@Nullable Context context;
    private @Nullable Info info;

    private Field(Class<T> type, @Nullable Class<?> listType, T defaultValue) {
        this.type = type;
        this.listType = listType;
        this.defaultValue = defaultValue;
    }

    public Class<T> type() {
        return type;
    }

    public T defaultValue() {
        return defaultValue;
    }

    private static IllegalStateException noContext() {
        return new IllegalStateException("Context is not initialized, or, method is called outside of HitoriConfiguration#access");
    }

    void assignContextAndInfo(HitoriConfiguration.Context context, Info info) {
        this.context = context;
        this.info = info;
    }

    public T get() {
        if(context == null || info == null) throw noContext();

        Object rawValue = context.get(info);
        if(rawValue == null) return defaultValue;

        if(!type.isInstance(rawValue)) throw InternalException.formatted(
                "Type mismatch: value present in config is not an instance of %s, it is actually instance of %s",
                type.getName(),
                rawValue.getClass().getName()
        );

        T returnValue = UnsafeUtil.cast(rawValue);
        assert returnValue != null;
        return returnValue;
    }

    /**
     *
     * @param value new value to set to the config, or null to reset the field to the default value
     * @return previously assigned value, or null if it was the default
     */
    public @Nullable T set(@Nullable T value) {
        if(context == null || info == null) throw noContext();

        Object rawValue = context.get(info);
        if(rawValue == null && value == null) return null;

        if(rawValue != null) {
            if(!type.isInstance(rawValue)) throw InternalException.formatted(
                    "Type mismatch: value present in config is not an instance of %s, it is actually instance of %s",
                    type.getName(),
                    rawValue.getClass().getName()
            );

            context.set(info, null);
            return UnsafeUtil.cast(rawValue);
        }

        context.set(info, value);
        return null;
    }

    // guesses type based on defaultValue without generics
    public static <T> Field<T> create(T defaultValue) {
        Class<T> clazz = UnsafeUtil.cast(defaultValue.getClass());
        assert clazz != null;

        if(clazz.getTypeParameters().length != 0) throw new IllegalArgumentException("Use Field#createList for lists!");

        for (Class<?> primitiveClass : PRIMITIVES_CLASSES) {
            if(primitiveClass.isAssignableFrom(clazz))
                return new Field<>(clazz, null, defaultValue);
        }

        if(clazz.isArray() || clazz.isAssignableFrom(List.class))
            return new Field<>(clazz, null, defaultValue);

        throw new IllegalStateException("Field type should be a primitive, or, a java.util.List");
    }

    public static <T> Field<List<T>> createList(T defaultValue, Class<?> listType) {
        throw new UnsupportedOperationException(); // todo
    }

    record Info(String name, String absolutePath) {}

}
