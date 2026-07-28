package su.hitori.api.test.configuration;

import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import su.hitori.api.configuration.HitoriConfiguration;
import su.hitori.api.configuration.serializer.JSONSerializer;
import su.hitori.api.configuration.serializer.Serializer;
import su.hitori.api.configuration.serializer.YAMLSerializer;

import java.io.*;
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
    public void testReadYAML(@TempDir Path tempDir) throws IOException {
        testRead(tempDir, YAMLSerializer.INSTANCE, "yml");
    }

    @Test
    public void testReadJSON(@TempDir Path tempDir) throws IOException {
        testRead(tempDir, JSONSerializer.INSTANCE, "json");
    }

    private void testRead(Path tempDir, Serializer serializer, String extension) throws IOException {
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

        File file = tempDir.resolve("example." + extension).toFile();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            exampleConfig.write(serializer, fos);
            fos.flush();
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            exampleConfig.read(serializer, fis);
        }

        Assertions.assertEquals("string", access.string.get());
        Assertions.assertEquals("%s joined the server", access.section.join.get());
    }

}
