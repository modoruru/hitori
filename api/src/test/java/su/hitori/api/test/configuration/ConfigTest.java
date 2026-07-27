package su.hitori.api.test.configuration;

import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import su.hitori.api.configuration.HitoriConfiguration;
import su.hitori.api.configuration.serializer.YAMLSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

public final class ConfigTest {

    @Test
    public void testScheme() {
        HitoriConfiguration<ExampleConfiguration> exampleConfig = HitoriConfiguration.create(
                Key.key("hitori", "example"),
                new ExampleConfiguration(),
                null
        );
        exampleConfig.defaults();

        ExampleConfiguration access = exampleConfig.access();
        Assertions.assertEquals("string", access.string.get());
        Assertions.assertEquals("%s joined", access.section.join.get());
    }

    @Test
    public void testWrite(@TempDir Path tempDir) throws IOException {
        HitoriConfiguration<ExampleConfiguration> exampleConfig = HitoriConfiguration.create(
                Key.key("hitori", "example"),
                new ExampleConfiguration(),
                null
        );
        exampleConfig.defaults();

        try (FileOutputStream fos = new FileOutputStream(tempDir.resolve("example.yml").toFile())) {
            exampleConfig.write(YAMLSerializer.INSTANCE, fos);
            fos.flush();
        }
    }

    @Test
    public void testRead(@TempDir Path tempDir) throws IOException {
        HitoriConfiguration<ExampleConfiguration> exampleConfig = HitoriConfiguration.create(
                Key.key("hitori", "example"),
                new ExampleConfiguration(),
                null
        );
        exampleConfig.defaults();

        ExampleConfiguration access = exampleConfig.access();
        Assertions.assertEquals("string", access.string.get());
        Assertions.assertEquals("%s joined", access.section.join.get());

        access.section.join.set("%s joined the server");

        File file = tempDir.resolve("example.yml").toFile();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            exampleConfig.write(YAMLSerializer.INSTANCE, fos);
            fos.flush();
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            exampleConfig.read(YAMLSerializer.INSTANCE, fis);
        }

        Assertions.assertEquals("string", access.string.get());
        Assertions.assertEquals("%s joined the server", access.section.join.get());
    }

}
