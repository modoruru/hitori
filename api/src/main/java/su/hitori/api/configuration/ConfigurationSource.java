package su.hitori.api.configuration;

import org.jspecify.annotations.Nullable;
import su.hitori.api.configuration.serializer.Serializer;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * ConfigurationSource is a class that holds constant parameters for serializing and deserializing {@link HitoriConfiguration}.<br>
 * This is useful for "reloading" (saving and reading) configuration from where it was originally supposed to be.
 *
 * @see HitoriConfiguration#hasConfigurationSource()
 * @see HitoriConfiguration#readFromSource()
 * @see HitoriConfiguration#writeToSource()
 */
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

    /**
     * Creates ConfigurationSource based on the {@link InputStream} for reading and {@link OutputStream} for writing.
     * @param serializer serializer for the serialization/deserialization
     * @param inputStreamCreator creator of the {@link InputStream}, called on {@link HitoriConfiguration#readFromSource(ConfigurationSource)} call
     * @param outputStreamCreator creator of the {@link OutputStream}, called on {@link HitoriConfiguration#writeToSource(ConfigurationSource)} call
     * @return created ConfigurationSource
     */
    public static ConfigurationSource virtual(Serializer serializer, Supplier<InputStream> inputStreamCreator, Supplier<OutputStream> outputStreamCreator) {
        return new ConfigurationSource(serializer, null, inputStreamCreator, outputStreamCreator);
    }

    /**
     * Creates ConfigurationSource based on the file.
     * @param serializer serializer for the serialization/deserialization
     * @param file base file
     * @return created ConfigurationSource
     */
    public static ConfigurationSource file(Serializer serializer, File file) {
        return new ConfigurationSource(serializer, file, null, null);
    }


    /**
     * Creates ConfigurationSource based on the file.
     * @param serializer serializer for the serialization/deserialization
     * @param path path of the base file
     * @return created ConfigurationSource
     */
    public static ConfigurationSource file(Serializer serializer, Path path) {
        return file(serializer, path.toFile());
    }

}
