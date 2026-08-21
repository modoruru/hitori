package su.hitori.api.configuration.serializer;

import su.hitori.api.configuration.SectionScheme;
import su.hitori.api.configuration.HitoriConfiguration;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Serializer for the {@link HitoriConfiguration}. Serializes and deserializes configuration values in its own way.
 */
public interface Serializer {

    /**
     * Serializes configuration data
     * @param rootSchemeNode root configuration node
     * @param rawData "absolutePath to value" map containing all configuration data
     * @param comments "absolutePath to comment" map containing comments for specific fields
     * @param output where to write serialized data
     */
    void write(SectionScheme.Node rootSchemeNode, Map<String, Object> rawData, Map<String, String> comments, OutputStream output);

    /**
     * Deserializes configuration data
     * @param logger logger for deserialization (mostly for warnings and errors)
     * @param rootSchemeNode root configuration node
     * @param input where to read the serialized data from
     * @param results "absolutePath to value" for writing results
     */
    void read(Logger logger, SectionScheme.Node rootSchemeNode, InputStream input, Map<String, Object> results);

}
