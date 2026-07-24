package su.hitori.api.configuration;

import org.jspecify.annotations.Nullable;
import su.hitori.api.configuration.serializer.Serializer;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.function.Supplier;

public final class ConfigurationSource {

    final Serializer serializer;
    final @Nullable File file;
    final @Nullable Supplier<InputStream> inputStreamCreator;
    final @Nullable Supplier<OutputStream> outputStreamCreator;

    private ConfigurationSource(Serializer serializer, @Nullable File file, @Nullable Supplier<InputStream> inputStreamCreator, @Nullable Supplier<OutputStream> outputStreamCreator) {
        this.serializer = serializer;
        this.file = file;
        this.inputStreamCreator = inputStreamCreator;
        this.outputStreamCreator = outputStreamCreator;
    }

    public static ConfigurationSource virtual(Serializer serializer, Supplier<InputStream> inputStreamCreator, Supplier<OutputStream> outputStreamCreator) {
        return new ConfigurationSource(serializer, null, inputStreamCreator, outputStreamCreator);
    }

    public static ConfigurationSource file(Serializer serializer, File file) {
        return new ConfigurationSource(serializer, file, null, null);
    }

    public static ConfigurationSource file(Serializer serializer, Path path) {
        return file(serializer, path.toFile());
    }

}
