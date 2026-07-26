package su.hitori.api.configuration.serializer;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import su.hitori.api.configuration.Field;
import su.hitori.api.configuration.SectionScheme;
import su.hitori.api.util.NameFormatter;
import su.hitori.api.util.UnsafeUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public final class YAMLSerializer implements Serializer {

    public static final YAMLSerializer INSTANCE = new YAMLSerializer();

    private final Yaml yaml;

    private YAMLSerializer() {
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setPrettyFlow(true);
        this.yaml = new Yaml(dumperOptions);
    }

    @Override
    public void write(SectionScheme.Node rootSchemeNode, Map<String, Object> rawData, OutputStream output) {
        // build map for proper saving
        Map<String, Object> map = new HashMap<>();

        assert rootSchemeNode.section() != null;
        writeSection("", rootSchemeNode, rawData, map);

        try (OutputStreamWriter writer = new OutputStreamWriter(output, StandardCharsets.UTF_8)) {
            yaml.dump(map, writer);
        }
        catch (IOException exception) {
            throw new IllegalArgumentException(exception);
        }
    }

    private void writeSection(String absolutePath, SectionScheme.Node node, Map<String, Object> rawData, Map<String, Object> results) {
        String pathPrefix;
        if(absolutePath.isEmpty()) pathPrefix = absolutePath;
        else pathPrefix = absolutePath + '.';

        assert node.section() != null;
        for (Map.Entry<String, SectionScheme.Node> entry : node.section().entrySet()) {
            String nodeName = entry.getKey();
            SectionScheme.Node embeddedNode = entry.getValue();

            String yamlName = NameFormatter.fromAnyCase(nodeName).toSnake();

            Object resultValue = switch (embeddedNode.nodeType()) {
                case SECTION -> {
                    Map<String, Object> embeddedNodeResults = new HashMap<>();
                    writeSection(pathPrefix + nodeName, embeddedNode, rawData, embeddedNodeResults);
                    yield embeddedNodeResults;
                }
                case PRIMITIVE -> {
                    Field<?> field = embeddedNode.field();
                    assert field != null;

                    Object rawValue = rawData.get(pathPrefix + nodeName);
                    if(rawValue != null) {
                        if(field.type().isEnum())
                            //noinspection DataFlowIssue
                            yield Enum.valueOf(UnsafeUtil.cast(field.type()), rawValue.toString());

                        yield rawValue;
                    }

                    yield field.defaultValue();
                }
                case LIST -> throw new UnsupportedOperationException(); // todo
            };

            results.put(yamlName, resultValue);
        }
    }

    @Override
    public void read(Logger logger, SectionScheme.Node rootSchemeNode, InputStream input, Map<String, Object> results) {
        Map<String, Object> map = yaml.load(input);
        if(map.isEmpty()) return;

        readSection(logger, "", map, rootSchemeNode, results);
    }

    private void readSection(Logger logger, String absolutePath, Map<String, Object> section, SectionScheme.Node node, Map<String, Object> results) {
        assert node.section() != null;

        String pathPrefix;
        if(absolutePath.isEmpty()) pathPrefix = absolutePath;
        else pathPrefix = absolutePath + '.';

        for (Map.Entry<String, Object> entry : section.entrySet()) {
            String yamlName = entry.getKey();
            Object value = entry.getValue();

            String convertedName = NameFormatter.fromAnyCase(yamlName).toCamel();

            var correspondingNode = node.section().get(convertedName);
            if(correspondingNode == null) {
                logger.warning(String.format(
                        "skipping node %s%s because it doesn't exists in the scheme",
                        pathPrefix,
                        yamlName
                ));
                continue;
            }

            switch (correspondingNode.nodeType()) {
                case SECTION -> {
                    Map<String, Object> embeddedSection = UnsafeUtil.cast(value);
                    if(embeddedSection == null) {
                        logger.warning(String.format(
                                "node type mismatch: %s%s defined as %s in scheme, but the attempt to cast it was failed.",
                                pathPrefix,
                                yamlName,
                                correspondingNode.nodeType().name()
                        ));
                        continue;
                    }

                    assert correspondingNode.section() != null;
                    readSection(logger, pathPrefix + convertedName, embeddedSection, correspondingNode, results);
                }
                case PRIMITIVE -> {
                    if(value instanceof Enum<?> enumValue) {
                        results.put(pathPrefix + convertedName, enumValue.name());
                        continue;
                    }

                    results.put(pathPrefix + convertedName, value);
                }
                case LIST -> throw new UnsupportedOperationException(); // todo
            }
        }
    }

}
