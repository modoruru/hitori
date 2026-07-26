package su.hitori.api.configuration;

import org.jspecify.annotations.Nullable;
import su.hitori.api.util.UnsafeUtil;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scheme for the configuration.
 * <p>
 * Each configuration consists of Nodes, which come in three types:
 * <ul>
 *     <li>primitive - stores some value</li>
 *     <li>section - stores nodes based on keys, works exactly as {@link Map}</li>
 *     <li>list - stores sorted array of the nodes, works exactly as {@link List}</li>
 * </ul>
 * SectionScheme in our API stands for section node type.
 *
 * <h2>Declaring primitive and list nodes in the section</h2>
 * Every primitive should be declared using {@link Field} object.<br>
 * For primitives {@link Field}'s are created using {@link Field#create(Object)} method.<br>
 * For lists, it's the {@link Field#createList(Object, Class)} method.
 * <p>
 * An example of declaring fields shown below:
 * <pre>{@code
 * public class ExampleConfig extends SectionScheme {
 *     public final Field<String> exampleString = Field.create("Example string value!");
 *     public final Field<List<String> exampleList = Field.createList(List.of("string 1", "string 2"), String.class);
 * }
 * }</pre>
 * <h2>Declaring section nodes in another section</h2>
 * Section are simply declared using an instance of one's.
 * An example:
 * <pre>{@code
 * public class ExampleConfig extends SectionScheme {
 *     public final Field<String> exampleString = Field.create("Example string value!");
 *     public final Field<List<String> exampleList = Field.createList(List.of("string 1", "string 2"), String.class);
 *     public final ExampleSection exampleSection = new ExampleSection();
 *
 *     public class ExampleSection extends SectionScheme {
 *         public final Field<String> exampleStringInSection = Field.create("Example string in the section!");
 *     }
 * }
 */
public abstract class SectionScheme {

    @Nullable Node root;

    public SectionScheme() {
        // todo: scheme verification
    }

    /**
     * Collects data of the scheme via {@link java.lang.reflect}.
     * @param context context that will be assigned to the {@link Field}s
     * @param absolutePath absolute path of the node to compile
     * @param schemeClass class of the scheme
     * @param scheme scheme instance
     * @return compiled node
     */
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

            results.put(internalFieldName, node);
        }

        return new Node(NodeType.SECTION, null, results);
    }

    /**
     * Node type for the {@link Node}.
     */
    public enum NodeType {
        PRIMITIVE, LIST, SECTION
    }

    /**
     * Node for the {@link SectionScheme}
     * @param nodeType
     * @param field field declaring this node, null if {@link Node#nodeType()} is a section.
     * @param section section declaring this node, null if {@link Node#nodeType()} is not a section.
     */
    public record Node(NodeType nodeType, @Nullable Field<?> field, @Nullable Map<String, Node> section) {

    }

}
