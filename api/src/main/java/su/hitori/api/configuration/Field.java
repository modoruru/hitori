package su.hitori.api.configuration;

import org.jspecify.annotations.Nullable;
import su.hitori.api.configuration.exception.AlreadyRegisteredException;
import su.hitori.api.configuration.exception.InternalException;
import su.hitori.api.configuration.listener.FieldListener;
import su.hitori.api.configuration.listener.RegisteredFieldListener;
import su.hitori.api.module.ModuleDescriptor;
import su.hitori.api.util.UnsafeUtil;

import java.util.List;

/**
 * Holds primitives and lists in the {@link SectionScheme}. <br>
 * Also used to get and set data in a configuration instance.
 * @param <T> type of object to hold
 */
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

    /**
     * @return the type of value held by the field
     */
    public Class<T> type() {
        return type;
    }

    /**
     * @return default value of the field
     */
    public T defaultValue() {
        return defaultValue;
    }

    void assignContextAndInfo(HitoriConfiguration.Context context, Info info) {
        this.context = context;
        this.info = info;
    }

    /**
     * @return actual value of the field
     * @throws IllegalStateException if method is called outside the {@link HitoriConfiguration#access}
     */
    public T get() {
        checkContext();
        assert context != null && info != null;

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
     * @param value new value to set to the configuration instance, or null to reset the field to the default value
     * @return the previously assigned value, or null if it was the default value.
     * @throws IllegalStateException if method is called outside the {@link HitoriConfiguration#access}
     */
    public @Nullable T set(@Nullable T value) {
        checkContext();
        assert context != null && info != null;

        Object rawCurrentValue = context.get(info);
        if(rawCurrentValue == null && value == null) return null;

        if(rawCurrentValue != null) {
            if(!type.isInstance(rawCurrentValue)) throw InternalException.formatted(
                    "Type mismatch: value present in config is not an instance of %s, it is actually instance of %s",
                    type.getName(),
                    rawCurrentValue.getClass().getName()
            );

            context.set(info, null);
            return UnsafeUtil.cast(rawCurrentValue);
        }

        context.set(info, value);
        return null;
    }

    // guesses type based on defaultValue without generics

    /**
     * Creates field for holding primitive type.
     * @param defaultValue default value for the field
     * @return created field
     * @param <T> type of the primitive
     */
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

    /**
     * Adds a listener which is called on every field value change.
     * @param descriptor descriptor of the module that registers listener
     * @param listener listener itself
     */
    public RegisteredFieldListener listen(ModuleDescriptor descriptor, FieldListener<T> listener) {
        checkContext();
        assert context != null && info != null;

        RegisteredFieldListener registeredListener = context.addListener(info, descriptor, listener);
        if(registeredListener == null) throw new AlreadyRegisteredException("Field is already being listened by this module!");

        return registeredListener;
    }

    /**
     * Creates field for holding list of the primitives, another lists or the {@link SectionScheme}
     * @param defaultValue list with default entries
     * @return created field
     * @param <T> type of the list
     */
    public static <T> Field<List<T>> createList(T defaultValue, Class<?> listType) {
        throw new UnsupportedOperationException(); // todo
    }

    private void checkContext() {
        if(context == null || info == null || context.rawData == null)
            throw new IllegalStateException("Context is not initialized, or, method is called outside of HitoriConfiguration#access");
    }

    record Info(String name, String absolutePath) {}

}
