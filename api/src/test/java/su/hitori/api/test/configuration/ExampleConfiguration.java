package su.hitori.api.test.configuration;

import su.hitori.api.configuration.Field;
import su.hitori.api.configuration.SectionScheme;

import java.util.List;

public final class ExampleConfiguration extends SectionScheme {

    public final Field<String> string = Field.create("string");
    public final Field<Integer> integer = Field.create(42);
    public final Field<Boolean> _boolean = Field.create(true);
    public final Messages section = new Messages();
    public final Field<List<String>> strings = Field.createList(List.of(
            "string 1", "string 2", "string 3"
    ), String.class);

    public static final class Messages extends SectionScheme {
        public final Field<String> join = Field.create("%s joined");
        public final Field<String> quit = Field.create("%s quit");
    }

}
