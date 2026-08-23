package su.hitori.api.configuration.serializer;

import org.json.JSONArray;
import org.json.JSONObject;
import su.hitori.api.configuration.Field;
import su.hitori.api.configuration.SectionScheme;
import su.hitori.api.configuration.exception.InternalException;
import su.hitori.api.util.JSONUtil;
import su.hitori.api.util.NameFormatter;
import su.hitori.api.util.SafeUtil;
import su.hitori.api.util.UnsafeUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class JSONSerializer implements Serializer {

    public static JSONSerializer INSTANCE = new JSONSerializer();

    private JSONSerializer() {}

    @Override
    public void write(SectionScheme.Node rootSchemeNode, Map<String, Object> rawData, Map<String, String> comments, OutputStream output) {
        JSONObject body = new JSONObject();

        assert rootSchemeNode.section() != null;
        writeSection("", rootSchemeNode, rawData, body);

        try (OutputStreamWriter writer = new OutputStreamWriter(output, StandardCharsets.UTF_8)) {
            body.write(writer, 2, 0);
        }
        catch (IOException exception) {
            throw new IllegalArgumentException(exception);
        }
    }

    private static void writeSection(String absolutePath, SectionScheme.Node node, Map<String, Object> rawData,  JSONObject body) {
        String pathPrefix;
        if(absolutePath.isEmpty()) pathPrefix = absolutePath;
        else pathPrefix = absolutePath + '.';

        assert node.section() != null;
        for (Map.Entry<String, SectionScheme.Node> entry : node.section().entrySet()) {
            String nodeName = entry.getKey();
            SectionScheme.Node embeddedNode = entry.getValue();

            String snakeCaseName = NameFormatter.fromAnyCase(nodeName).toSnake();

            Object resultValue = switch (embeddedNode.nodeType()) {
                case SECTION -> {
                    JSONObject embeddedNodeBody = new JSONObject();
                    writeSection(pathPrefix + nodeName, embeddedNode, rawData, embeddedNodeBody);
                    yield embeddedNodeBody;
                }
                case PRIMITIVE -> {
                    Field<?> field = embeddedNode.field();
                    assert field != null;

                    Object rawValue = rawData.get(pathPrefix + nodeName);
                    if(rawValue != null) {
                        if(rawValue instanceof Enum<?> asEnum) yield asEnum.name();

                        yield rawValue;
                    }

                    yield field.defaultValue();
                }
                case LIST -> {
                    Field<?> field = embeddedNode.field();
                    assert field != null;

                    Object rawValue = rawData.get(pathPrefix + nodeName);

                    JSONArray resultList = new JSONArray();
                    List<Object> rawList = UnsafeUtil.cast(rawValue == null ? field.defaultValue() : rawValue);
                    assert rawList != null;

                    for (Object object : rawList) {
                        resultList.put(switch (object) {
                            case SectionScheme _ -> throw new UnsupportedOperationException("Serializing lists of sections is not supported yet, sorry for the inconvenience.");
                            case Enum<?> asEnum -> asEnum.name();
                            default -> object;
                        });
                    }

                    yield resultList;
                }
            };

            body.put(snakeCaseName, resultValue);
        }
    }

    @Override
    public void read(Logger logger, SectionScheme.Node rootSchemeNode, InputStream input, Map<String, Object> results) {
        JSONObject body;
        try (InputStreamReader reader = new InputStreamReader(input)) {
            body = JSONUtil.read(reader);
        }
        catch (IOException exception) {
            throw new InternalException(exception);
        }

        if(body.isEmpty()) return;

        readSection(logger, "", body, rootSchemeNode, results);
    }

    private static void readSection(Logger logger, String absolutePath, JSONObject sectionBody, SectionScheme.Node node, Map<String, Object> results) {
        assert node.section() != null;

        String pathPrefix;
        if(absolutePath.isEmpty()) pathPrefix = absolutePath;
        else pathPrefix = absolutePath + '.';

        for (String snakeCaseName : sectionBody.keySet()) {
            Object value = sectionBody.get(snakeCaseName);

            String convertedName = NameFormatter.fromAnyCase(snakeCaseName).toCamel();

            var correspondingNode = node.section().get(convertedName);
            if(correspondingNode == null) {
                logger.warning(String.format(
                        "skipping node %s%s because it doesn't exists in the scheme",
                        pathPrefix,
                        snakeCaseName
                ));
                continue;
            }

            switch (correspondingNode.nodeType()) {
                case SECTION -> {
                    JSONObject embeddedSection = UnsafeUtil.cast(value);
                    if(embeddedSection == null) {
                        logger.warning(String.format(
                                "node type mismatch: %s%s defined as %s in scheme, but the attempt to cast it was failed.",
                                pathPrefix,
                                snakeCaseName,
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
                case LIST -> {
                    Field<?> field = correspondingNode.field();
                    assert field != null;

                    JSONArray rawList = UnsafeUtil.cast(value);
                    assert rawList != null;

                    List<Object> list = new ArrayList<>();

                    assert field.listType() != null;
                    if(field.listType().isEnum()) for (Object object : rawList) {
                        //noinspection DataFlowIssue
                        list.add(SafeUtil.enumValueOf(UnsafeUtil.cast(field.listType()), UnsafeUtil.cast(object)));
                    }
                    else if(SectionScheme.class.isAssignableFrom(field.listType())) throw new UnsupportedOperationException("Deserializing lists of sections is not supported yet, sorry for the inconvenience.");
                    else list.addAll(rawList.toList());

                    results.put(pathPrefix + convertedName, list);
                }
            }
        }
    }

}
