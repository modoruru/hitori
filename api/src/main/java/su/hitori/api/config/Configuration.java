package su.hitori.api.config;

import net.elytrium.serializer.NameStyle;
import net.elytrium.serializer.SerializerConfig;
import net.elytrium.serializer.language.object.YamlSerializable;

import java.nio.file.Path;

/**
 * A class holding configuration based on Elytrium library.
 * For use reference check: <a href="https://github.com/Elytrium/java-serializer">Elytrium/java-serializer</a>
 */
public abstract class Configuration extends YamlSerializable {

    public Configuration(Path path) {
        super(path, new SerializerConfig.Builder()
                .setFieldNameStyle(NameStyle.CAMEL_CASE)
                .setNodeNameStyle(NameStyle.SNAKE_CASE)
                .build()
        );
    }

}
