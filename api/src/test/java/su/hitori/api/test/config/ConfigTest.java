package su.hitori.api.test.config;

import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;
import su.hitori.api.config.HitoriConfiguration;

public final class ConfigTest {

    @Test
    public void testScheme() {
        HitoriConfiguration<ExampleConfiguration> exampleConfig = HitoriConfiguration.create(
                Key.key("hitori", "example"),
                new ExampleConfiguration()
        );
        exampleConfig.defaults();

        ExampleConfiguration access = exampleConfig.access();
        System.out.printf("[check] string value: \"%s\"\n", access.string.get());
        System.out.printf("[check] section.join value: \"%s\"\n", access.section.get().join.get());
    }

}
