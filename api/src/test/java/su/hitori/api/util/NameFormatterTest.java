package su.hitori.api.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static su.hitori.api.util.NameFormatter.fromAnyCase;
import static su.hitori.api.util.NameFormatter.fromPascalCase;

public final class NameFormatterTest {

    @Test
    public void testIntendedUseCases() {
        Assertions.assertEquals("nameFormatter", fromAnyCase("NameFormatter").toCamel());
        Assertions.assertEquals("ConfigMessages", fromAnyCase("config_messages").toPascal());
        Assertions.assertEquals("name-formatter-test", fromAnyCase("name_formatter_test").toKebab());
    }

    @Test
    public void testNonIntendedUseCases() {
        Assertions.assertEquals("_default", fromAnyCase("_default").toCamel());
        Assertions.assertEquals("VeryCoolNMSImplementation_1.21.6", fromPascalCase("VeryCoolNMSImplementation_1.21.6").toPascal());
        Assertions.assertEquals("entity_nms", fromPascalCase("EntityNMS").toSnake());
        Assertions.assertEquals("nms-entity", fromPascalCase("NMSEntity").toKebab());
    }

}
