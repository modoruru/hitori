package su.hitori.api.config;

import org.jspecify.annotations.Nullable;
import su.hitori.api.config.exception.IllegalSchemeException;
import su.hitori.api.config.exception.InternalException;
import su.hitori.api.util.UnsafeUtil;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ConfigurationScheme {

    private static final Class<?>[] PRIMITIVES_CLASSES = new Class[]{
            byte.class, Byte.class,
            short.class, Short.class,
            int.class, Integer.class,
            long.class, Long.class,
            float.class, Float.class,
            double.class, Double.class,
            boolean.class, Boolean.class,
            char.class, Character.class
    };

    @Nullable Node root;

    public ConfigurationScheme() {
        // todo: scheme verification
    }

    private static NodeType determineType(Class<?> clazz) {
        if(clazz.isArray() || Collection.class.isAssignableFrom(clazz)) return NodeType.LIST;
        if(clazz.isAssignableFrom(String.class)) return NodeType.PRIMITIVE; // string is very popular config node so why not

        for (Class<?> primitiveClass : PRIMITIVES_CLASSES) {
            if(primitiveClass.isAssignableFrom(clazz)) return NodeType.PRIMITIVE;
        }

        return NodeType.SECTION;
    }

    void compileAndAssignContext(HitoriConfiguration.Context contextToAssign) {
        root = compileSectionNode("", null, null, getClass(), this, contextToAssign);
    }

    Node compileSectionNode(String absoluteNodeName, @Nullable String nodeName, @Nullable Field<?> nodeField, Class<?> clazz, Object instance, HitoriConfiguration.Context contextToAssign) {
        if((nodeName == null) != (nodeField == null)) throw new InternalException();

        Map<String, Node> result = new HashMap<>();

        for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
            if(!field.getType().isAssignableFrom(Field.class)) continue;

            // todo: move to constructor for verification
            int modifiers = field.getModifiers();
            if(!Modifier.isFinal(modifiers)) throw new IllegalSchemeException("Fields should be final! Non-final field: " + field.getName());
            if(!Modifier.isPublic(modifiers)) throw new IllegalSchemeException("Fields should be public! Non-public field: " + field.getName());

            String fieldName = field.getName();

            Field<?> configField = null;
            try {
                Object rawConfigField = field.get(instance);
                if(rawConfigField == null) throw new IllegalSchemeException(new NullPointerException(), "Null field is present: " + field.getName());

                configField = UnsafeUtil.cast(rawConfigField);
            }
            catch (Exception _) {}

            assert configField != null; // because we verify access at the constructor

            configField.assignContextAndInfo(contextToAssign, new Field.FieldInfo(fieldName, absoluteNodeName + '.' + fieldName));

            Class<?> configFieldType = configField.type();
            NodeType nodeType = determineType(configFieldType);

            Node node = switch (nodeType) {
                case PRIMITIVE -> new Node(nodeType, configField, null, null);
                case SECTION -> compileSectionNode(absoluteNodeName + '.' + fieldName, fieldName, configField, configFieldType, configField.defaultValue(), contextToAssign);
                case LIST -> null;
            };
            assert node != null;

            System.out.printf("[compile] Inserted %s node to %s\n", fieldName, nodeName == null ? "root" : nodeName);
            result.put(fieldName, node);
        }

        return new Node(NodeType.SECTION, nodeField, null, result);
    }

    public enum NodeType {
        PRIMITIVE, LIST, SECTION
    }

    public record Node(NodeType entryType, su.hitori.api.config.@Nullable Field<?> field, @Nullable List<Node> list, @Nullable Map<String, Node> section) {

    }

}
