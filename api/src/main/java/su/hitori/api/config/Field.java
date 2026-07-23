package su.hitori.api.config;

import org.jspecify.annotations.Nullable;
import su.hitori.api.config.exception.InternalException;
import su.hitori.api.util.UnsafeUtil;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

public final class Field<T> {

    final Class<T> type;
    final Class<?> @Nullable [] genericTypes;
    final T defaultValue;

    private HitoriConfiguration.@Nullable Context context;
    private @Nullable FieldInfo fieldInfo;

    private Field(Class<T> type, Class<?> @Nullable [] genericTypes, T defaultValue) {
        this.type = type;
        this.genericTypes = genericTypes;
        this.defaultValue = defaultValue;
    }

    public Class<T> type() {
        return type;
    }

    public T defaultValue() {
        return defaultValue;
    }

    void assignContextAndInfo(HitoriConfiguration.Context context, FieldInfo fieldInfo) {
        this.context = context;
        this.fieldInfo = fieldInfo;
    }

    private static IllegalStateException noContext() {
        return new IllegalStateException("Context is not initialized, or, method is called outside of HitoriConfiguration#access");
    }

    public T get() {
        if(context == null || fieldInfo == null) throw noContext();

        Object rawValue = context.get(fieldInfo);
        if(rawValue == null) return defaultValue;

        if(!type.isInstance(rawValue)) throw InternalException.formatted(
                "Type mismatch: value present in config is not an instance of %s, it is actually instance of %s",
                type.getName(),
                rawValue.getClass().getName()
        );

        return UnsafeUtil.cast(rawValue);
    }

    /**
     *
     * @param value new value to set to the config, or null to reset the field to the default value
     * @return previously assigned value, or null if it was the default
     */
    public @Nullable T set(@Nullable T value) {
        if(context == null || fieldInfo == null) throw noContext();

        Object rawValue = context.get(fieldInfo);
        if(rawValue == null && value == null) return null;

        if(rawValue != null) {
            if(!type.isInstance(rawValue)) throw InternalException.formatted(
                    "Type mismatch: value present in config is not an instance of %s, it is actually instance of %s",
                    type.getName(),
                    rawValue.getClass().getName()
            );

            context.set(fieldInfo, null);
            return UnsafeUtil.cast(rawValue);
        }

        context.set(fieldInfo, value);
        return null;
    }


    // guesses type based on defaultValue without generics
    public static <T> Field<T> create(T defaultValue) {
        Class<T> clazz = UnsafeUtil.cast(defaultValue.getClass());
        if(clazz.getTypeParameters().length != 0) throw new IllegalArgumentException("Use Field#createWithGenerics on classes with generic types!");
        return new Field<>(clazz, null, defaultValue);
    }

    public static <T> Field<T> createWithGenerics(T defaultValue, Class<?>... genericTypes) {
        Class<T> clazz = UnsafeUtil.cast(defaultValue.getClass());

        TypeVariable<Class<T>>[] types = clazz.getTypeParameters();
        if(types.length != genericTypes.length) {
            // todo: config option in hitori to disable this check
            throw new IllegalStateException(String.format(
                    "Either you passed wrong number of generic types (you passed: %s, class declares: %s), or this runtime drops info about generics.",
                    genericTypes.length,
                    types.length
            ));
        }

        // verify generic types
        int length = types.length;
        for (int i = 0; i < length; i++) {
            for (Type bound : types[i].getBounds()) {
                if(!(bound instanceof Class<?> boundClass) || boundClass.isAssignableFrom(genericTypes[i])) continue;

                throw new IllegalArgumentException(String.format(
                        "Class %s could not be assigned as %s's generic type (index %s) which bounds is %s",
                        genericTypes[i].getName(),
                        clazz.getName(),
                        i,
                        boundClass.getName()
                ));
            }
        }

        return new Field<>(clazz, genericTypes, defaultValue);
    }

    record FieldInfo(String name, @Nullable FieldInfo parentField) {}

}
