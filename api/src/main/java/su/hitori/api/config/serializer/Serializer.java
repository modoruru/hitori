package su.hitori.api.config.serializer;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.Map;

public interface Serializer {

    void write(Map<String, Object> rawData, DataOutput output);

    Map<String, Object> read(DataInput input);

}
