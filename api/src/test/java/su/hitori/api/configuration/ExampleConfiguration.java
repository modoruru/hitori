package su.hitori.api.configuration;

import java.util.List;

@Comment("Comment on the root node")
public final class ExampleConfiguration extends SectionScheme {

    public final Field<String> string = Field.create("string");
    public final Field<Integer> integer = Field.create(42);
    public final Field<Boolean> _boolean = Field.create(true);
    public final Messages section = new Messages();
    public final Field<List<String>> strings = Field.createList(List.of(
            "string 1", "string 2", "string 3"
    ), String.class);
    public final Field<ExampleEnum> coolValue = Field.create(ExampleConfiguration.ExampleEnum.SECOND_COOL_VALUE);

    @Comment("Example comment for field field_with_comment")
    public final Field<String> fieldWithComment = Field.create("Field with comment");

    public static final class Messages extends SectionScheme {
        public final Field<String> join = Field.create("%s joined");
        public final Field<String> quit = Field.create("%s quit");
    }

    public enum ExampleEnum {
        VERY_COOL_VALUE,
        SECOND_COOL_VALUE
    }

}
