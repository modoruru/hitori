package su.hitori.api.test.config;

import su.hitori.api.config.ConfigurationScheme;
import su.hitori.api.config.Field;

public final class ExampleConfiguration extends ConfigurationScheme {

    public final Field<String> string = Field.create("string");
    public final Field<Integer> integer = Field.create(42);
    public final Field<Boolean> _boolean = Field.create(true);
    public final Field<Messages> section = Field.create(new Messages());

    public static final class Messages {
        public final Field<String> join = Field.create("%s joined");
        public final Field<String> quit = Field.create("%s quit");
    }

}
