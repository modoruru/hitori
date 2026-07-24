package su.hitori.api.configuration.serializer;

import su.hitori.api.configuration.SectionScheme;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.logging.Logger;

public interface Serializer {

    void write(SectionScheme.Node node, Map<String, Object> rawData, OutputStream output);

    void read(Logger logger, SectionScheme.Node rootSchemeNode, InputStream input, Map<String, Object> results);

}
