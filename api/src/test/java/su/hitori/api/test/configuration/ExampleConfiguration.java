package su.hitori.api.test.configuration;

import su.hitori.api.configuration.Field;
import su.hitori.api.configuration.SectionScheme;

public final class ExampleConfiguration extends SectionScheme {

    public final Field<String> string = Field.create("string");
    public final Field<Integer> integer = Field.create(42);
    public final Field<Boolean> _boolean = Field.create(true);
    public final Messages section = new Messages();

    public static final class Messages extends SectionScheme {
        public final Field<String> join = Field.create("%s joined");
        public final Field<String> quit = Field.create("%s quit");
    }

}
