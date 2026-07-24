package su.hitori.api.configuration;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.jspecify.annotations.Nullable;
import su.hitori.api.configuration.serializer.Serializer;
import su.hitori.api.configuration.serializer.YAMLSerializer;
import su.hitori.api.logging.LoggerFactory;
import su.hitori.api.util.UnsafeUtil;

import java.io.DataInput;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

public final class HitoriConfiguration<RootScheme extends SectionScheme> implements Keyed {

    private final Key key;
    private final RootScheme rootSectionScheme;

    private final Context context;

    private HitoriConfiguration(Key key, RootScheme rootSectionScheme) {
        this.key = key;
        this.rootSectionScheme = rootSectionScheme;

        this.context = new Context();

        rootSectionScheme.root = SectionScheme.compileSectionNode(context, "", rootSectionScheme.getClass(), rootSectionScheme);
    }

    private static void cleanupSectionRecursively(Map<String, Object> rawSection) {
        for (Object value : rawSection.values()) {
            if(value instanceof Map<?, ?> childMap)
                cleanupSectionRecursively(UnsafeUtil.cast(childMap));
        }
        rawSection.clear();
    }

    private static IllegalStateException notLoaded() {
        return new IllegalStateException("Configuration is not loaded yet!");
    }

    /**
     * Writes defaults to the all fields of the instance.
     */
    public void defaults() {
        synchronized (context.lock) {
            if(context.rawData == null) {
                context.rawData = new HashMap<>();
                return;
            }

            cleanupSectionRecursively(context.rawData);
        }
    }

    public void readYaml(InputStream input) {
        read(YAMLSerializer.INSTANCE, input);
    }

    /**
     * Reads values from {@link DataInput} using provided {@link Serializer}. Overwrites currently initialized data.
     */
    public void read(Serializer serializer, InputStream input) {
        synchronized (context.lock) {
            if(context.rawData != null) context.rawData.clear();
            else context.rawData = new HashMap<>();

            serializer.read(LoggerFactory.instance().create(Serializer.class), rootSectionScheme.root, input, context.rawData);
        }
    }

    public void write(Serializer serializer, OutputStream output) {
        if(context.rawData == null) throw notLoaded();

        synchronized (context.lock) {
            serializer.write(rootSectionScheme.root, context.rawData, output);
        }
    }

    public void writeAsync(Serializer serializer, OutputStream output, Executor executor) {
        if(context.rawData == null) throw notLoaded();

        synchronized (context.lock) {
            Map<String, Object> copy = new HashMap<>(context.rawData);
            executor.execute(() -> serializer.write(rootSectionScheme.root, copy, output));
        }
    }

    public RootScheme access() {
        if(context.rawData == null) throw notLoaded();
        return rootSectionScheme;
    }

    public static <C extends SectionScheme> HitoriConfiguration<C> create(Key key, C rootSectionScheme) {
        return new HitoriConfiguration<>(key, rootSectionScheme);
    }

    @Override
    public Key key() {
        return key;
    }

    /**
     * Shared context data
     */
    static class Context {

        final Object lock = new Object(); // for safe context changing
        @Nullable Map<String, Object> rawData; // currently loaded data; null if no data is loaded

        @Nullable Object get(Field.Info info) {
            synchronized (lock) {
                assert rawData != null;
                System.out.printf("[context/get] %s\n", info.absolutePath());
                return rawData.get(info.absolutePath());
            }
        }

        void set(Field.Info info, @Nullable Object value) {
            synchronized (lock) {
                assert rawData != null;

                if(value == null) rawData.remove(info.absolutePath());
                else rawData.put(info.absolutePath(), value);
            }
        }

    }

}
