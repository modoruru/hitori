package su.hitori.api.configuration;

import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;
import su.hitori.api.configuration.exception.AlreadyRegisteredException;
import su.hitori.api.configuration.exception.InternalException;
import su.hitori.api.configuration.listener.FieldListener;
import su.hitori.api.configuration.listener.RegisteredFieldListener;
import su.hitori.api.module.ModuleDescriptor;
import su.hitori.api.util.UnsafeUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    final Map<Key, RegisteredFieldListener> listeners;

    private HitoriConfiguration.@Nullable Context context;
    private SectionScheme.@Nullable Node node;
    private @Nullable String absolutePath;

    private Field(Class<T> type, @Nullable Class<?> listType, T defaultValue) {
        this.type = type;
        this.listType = listType;
        this.defaultValue = defaultValue;
        this.listeners = new HashMap<>();
    }

    void rebuildAbsolutePath() {
        assert node != null;

        SectionScheme.SectionNode parentNode = node.parentNode;
        if(parentNode == null) {
            absolutePath = node.name;
            return;
        }

        List<String> parts = new ArrayList<>();
        parts.add(node.name);
        addPart(parts, parentNode.asSection);

        StringBuilder builder = new StringBuilder();
        for (int i = parts.size() - 1; i >= 0; i--) {
            builder.append(parts.get(i));
            if(i > 0) builder.append('.');
        }
        currentFullPath = builder.toString();
    }

    private static void addPart(List<String> parts, SectionScheme.SectionNode sectionNode) {
        if(sectionNode.section.index >= 0) {
            parts.add(String.format("[%s]", sectionNode.section.index));
            SectionScheme.ListNode node = (SectionScheme.ListNode) sectionNode.parentNode;
            assert node != null && node.field.name != null;
            parts.add(node.field.name);

            if(node.field.parentNode != null && node.field.parentNode.asSection != null)
                addPart(parts, node.field.parentNode.asSection);
            return;
        }

        if(sectionNode.parentNode != null) {
            addPart();
        }
    }

    /**
     * @return the type of value held by the field
     */
    public Class<T> type() {
        return type;
    }

    public @Nullable Class<?> listType() {
        return listType;
    }

    /**
     * @return default value of the field
     */
    public T defaultValue() {
        return defaultValue;
    }

    void assignContextAndInfo(HitoriConfiguration.Context context, SectionScheme.Node node) {
        this.context = context;
        this.node = node;
        rebuildAbsolutePath();
    }

    private void checkType(Object rawValue) {
        if((type == Float.class && rawValue instanceof Double)) return;

        if(!type.isInstance(rawValue)) throw InternalException.formatted(
                "Type mismatch: value present in config is not an instance of %s, it is actually instance of %s",
                type.getName(),
                rawValue.getClass().getName()
        );
    }

    private T cast(Object rawValue) {
        if(type == Float.class && rawValue instanceof Double asDouble) return type.cast(asDouble.floatValue());

        T returnValue = UnsafeUtil.cast(rawValue);
        assert returnValue != null;
        return returnValue;
    }

    /**
     * @return actual value of the field
     * @throws IllegalStateException if method is called outside the {@link HitoriConfiguration#access}
     */
    public T get() {
        checkContext();
        assert context != null && parentNode != null;

        Object rawValue = context.get(this);
        if(rawValue == null) {
            if(List.class == type) {
                List<?> defaultValues = UnsafeUtil.cast(defaultValue);
                assert defaultValues != null;

                T returnValue = UnsafeUtil.cast(new ArrayList<>(defaultValues));
                assert returnValue != null;
                return returnValue;
            }
            return defaultValue;
        }

        checkType(rawValue);

        // if T is a list, return copy of it
        if(List.class == type) {
            List<?> valueAsList = UnsafeUtil.cast(rawValue);
            assert valueAsList != null;

            rawValue = new ArrayList<>(valueAsList);
        }

        return cast(rawValue);
    }

    /**
     * @param value new value to set to the configuration instance, or null to reset the field to the default value
     * @return the previously assigned value, or null if it was the default value.
     * @throws IllegalStateException if method is called outside the {@link HitoriConfiguration#access}
     */
    public @Nullable T set(@Nullable T value) {
        checkContext();
        assert context != null && parentNode != null;

        Object rawCurrentValue = context.get(this);
        if(rawCurrentValue == null && value == null) return null;

        if(rawCurrentValue != null && value == null) {
            checkType(rawCurrentValue);

            context.set(this, null);
            return cast(rawCurrentValue);
        }

        if(List.class == type) {
            List<?> cast = UnsafeUtil.cast(value);
            assert cast != null;
            context.set(this, List.copyOf(cast));
        }
        else context.set(this, value);
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

        throw new IllegalStateException("Field type should be a primitive");
    }

    /**
     * Adds a listener which is called on every field value change.
     * @param descriptor descriptor of the module that registers listener
     * @param listener listener itself
     */
    public RegisteredFieldListener listen(ModuleDescriptor descriptor, FieldListener<T> listener) {
        checkContext();
        assert context != null && parentNode != null;

        RegisteredFieldListener registeredListener = listeners.get(descriptor.key());
        if(registeredListener != null) throw new AlreadyRegisteredException("Field is already being listened by this module!");

        registeredListener = new RegisteredFieldListener(descriptor, listener, () -> listeners.remove(descriptor.key()));
        listeners.put(descriptor.key(), registeredListener);

        return registeredListener;
    }

    /**
     * Creates field for holding list of the primitives, another lists or the {@link SectionScheme}
     * @param defaultValue list with default entries
     * @return created field
     * @param <T> type of the list, either primitive or {@link SectionScheme}
     */
    public static <T> Field<List<T>> createList(List<T> defaultValue, Class<T> listElementsType) {
        Class<List<T>> clazz = UnsafeUtil.cast(defaultValue.getClass());
        assert clazz != null;

        if(listElementsType == List.class) throw new IllegalArgumentException("Embedded list are not allowed at the time.");
        if(SectionScheme.class.isAssignableFrom(listElementsType)) {
            // Verify that elements are of the exact same class
            for (T section : defaultValue) {
                if (section.getClass() != listElementsType) throw new IllegalArgumentException(String.format(
                        "All elements must be of the same class as provided listElementsType. Expected: %s, Actual: %s",
                        listElementsType.getName(),
                        section.getClass().getName()
                ));
            }
            return new Field<>(clazz, listElementsType, List.copyOf(defaultValue));
        }

        for (Class<?> primitiveClass : PRIMITIVES_CLASSES) {
            if(primitiveClass.isAssignableFrom(listElementsType))
                return new Field<>(clazz, listElementsType, defaultValue);
        }

        throw new IllegalStateException("List elements type should be a primitive or an extension from SectionScheme");
    }

    private void checkContext() {
        if(context == null || parentNode == null || context.rawData == null)
            throw new IllegalStateException("Context is not initialized, or, method is called outside of HitoriConfiguration#access");
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

}
