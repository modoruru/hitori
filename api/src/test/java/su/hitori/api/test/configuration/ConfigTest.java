package su.hitori.api.test.configuration;

import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import su.hitori.api.configuration.HitoriConfiguration;
import su.hitori.api.configuration.serializer.YAMLSerializer;
import su.hitori.api.logging.LoggerFactory;
import su.hitori.api.logging.LoggerFactoryHolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;

public final class ConfigTest {

    private void setupLogging() {
        LoggerFactoryHolder.set(new LoggerFactory() {
            @Override
            public Logger create() {
                return create("unknown");
            }

            @Override
            public Logger create(Class<?> clazz) {
                return create(clazz.getSimpleName());
            }

            @Override
            public Logger create(String name) {
                return Logger.getLogger(name);
            }
        });
    }

    @Test
    public void testScheme() {
        HitoriConfiguration<ExampleConfiguration> exampleConfig = HitoriConfiguration.create(
                Key.key("hitori", "example"),
                new ExampleConfiguration()
        );
        exampleConfig.defaults();

        ExampleConfiguration access = exampleConfig.access();
        System.out.printf("[check] string value: \"%s\"\n", access.string.get());
        System.out.printf("[check] section.join value: \"%s\"\n", access.section.join.get());

        Assertions.assertEquals("string", access.string.get());
        Assertions.assertEquals("%s joined", access.section.join.get());
    }

    @Test
    public void testWrite(@TempDir Path tempDir) throws IOException {
        HitoriConfiguration<ExampleConfiguration> exampleConfig = HitoriConfiguration.create(
                Key.key("hitori", "example"),
                new ExampleConfiguration()
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
                new ExampleConfiguration()
        );
        exampleConfig.defaults();

        ExampleConfiguration access = exampleConfig.access();
        System.out.println("Before read checks");
        Assertions.assertEquals("string", access.string.get());
        Assertions.assertEquals("%s joined", access.section.join.get());

        access.section.join.set("%s joined the server");

        File file = tempDir.resolve("example.yml").toFile();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            exampleConfig.write(YAMLSerializer.INSTANCE, fos);
            fos.flush();
        }

        // Setup logging for read first
        setupLogging();
        try (FileInputStream fis = new FileInputStream(file)) {
            exampleConfig.read(YAMLSerializer.INSTANCE, fis);
        }

        System.out.println("After read checks");
        Assertions.assertEquals("string", access.string.get());
        Assertions.assertEquals("%s joined the server", access.section.join.get());
    }

}
