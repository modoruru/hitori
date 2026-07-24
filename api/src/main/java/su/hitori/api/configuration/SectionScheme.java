package su.hitori.api.configuration;

import org.jspecify.annotations.Nullable;
import su.hitori.api.util.UnsafeUtil;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class SectionScheme {

    @Nullable Node root;

    public SectionScheme() {
        // todo: scheme verification
    }

    @SuppressWarnings("DataFlowIssue")
    static Node compileSectionNode(HitoriConfiguration.Context context, String absolutePath, Class<? extends SectionScheme> schemeClass, SectionScheme scheme) {
        String pathPrefix;
        if(absolutePath.isEmpty()) pathPrefix = absolutePath;
        else pathPrefix = absolutePath + '.';

        Map<String, Node> results = new HashMap<>();

        for (java.lang.reflect.Field internalField : schemeClass.getDeclaredFields()) {
            int modifiers = internalField.getModifiers();
            if(Modifier.isStatic(modifiers) || !Modifier.isFinal(modifiers)) continue;

            Class<?> internalFieldType = internalField.getType();
            Object internalFieldValue;
            try {
                internalFieldValue = internalField.get(scheme);
            }
            catch (IllegalAccessException _) {
                continue;
            }

            String internalFieldName = internalField.getName();

            Node node;
            if(SectionScheme.class.isAssignableFrom(internalFieldType)) {
                SectionScheme asSectionScheme = UnsafeUtil.cast(internalFieldValue);
                node = asSectionScheme.root = compileSectionNode(context, pathPrefix + internalFieldName, UnsafeUtil.cast(internalFieldType), asSectionScheme);
            }
            else if(!internalFieldType.isAssignableFrom(Field.class)) continue;
            else {
                Field<?> field = UnsafeUtil.cast(internalFieldValue);
                field.assignContextAndInfo(context, new Field.Info(internalFieldName, pathPrefix + internalFieldName));

                node = new Node(
                        field.type.isAssignableFrom(List.class)
                                ? NodeType.LIST
                                : NodeType.PRIMITIVE,
                        field,
                        null
                );
            }

            System.out.printf("[compile] Added node %s with type %s\n", internalFieldName, node.nodeType.name());
            results.put(internalFieldName, node);
        }

        return new Node(NodeType.SECTION, null, results);
    }

    public enum NodeType {
        PRIMITIVE, LIST, SECTION
    }

    public record Node(NodeType nodeType, @Nullable Field<?> field, @Nullable Map<String, Node> section) {

    }

}
