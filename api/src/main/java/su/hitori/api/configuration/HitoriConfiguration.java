package su.hitori.api.configuration;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.jspecify.annotations.Nullable;
import su.hitori.api.configuration.exception.InternalException;
import su.hitori.api.configuration.serializer.Serializer;
import su.hitori.api.configuration.serializer.YAMLSerializer;
import su.hitori.api.logging.LoggerFactory;
import su.hitori.api.util.UnsafeUtil;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

public final class HitoriConfiguration<RootScheme extends SectionScheme> implements Keyed {

    private final Key key;
    private final RootScheme rootSectionScheme;
    private final @Nullable ConfigurationSource configurationSource;

    private final Context context;

    private HitoriConfiguration(Key key, RootScheme rootSectionScheme, @Nullable ConfigurationSource configurationSource) {
        this.key = key;
        this.rootSectionScheme = rootSectionScheme;
        this.configurationSource = configurationSource;

        this.context = new Context();

        rootSectionScheme.root = SectionScheme.compileSectionNode(context, "", rootSectionScheme.getClass(), rootSectionScheme);

        if(configurationSource != null) readFromSource();
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

    public SectionScheme.Node rootNode() {
        assert rootSectionScheme.root != null;
        return rootSectionScheme.root;
    }

    public boolean hasConfigurationSource() {
        return configurationSource != null;
    }

    public void readFromSource() {
        if(configurationSource == null) throw new IllegalStateException("This HitoriConfiguration doesn't has default configurationSource.");
        readFromSource(configurationSource);
    }

    public void readFromSource(ConfigurationSource configurationSource) {
        synchronized (context.lock) {
            try {
                InputStream inputStream;
                if(configurationSource.file != null) inputStream = new FileInputStream(configurationSource.file);
                else {
                    assert configurationSource.inputStreamCreator != null;
                    inputStream = configurationSource.inputStreamCreator.get();
                }

                try (inputStream) {
                    read(configurationSource.serializer, inputStream);
                }
            }
            catch (Exception exception) {
                throw new InternalException(exception);
            }
        }
    }

    public void writeToSource() {
        if(configurationSource == null) throw new IllegalStateException("This HitoriConfiguration doesn't has default configurationSource.");
        writeToSource(configurationSource);
    }

    public void writeToSource(ConfigurationSource configurationSource) {
        synchronized (context.lock) {
            try {
                OutputStream outputStream;
                if(configurationSource.file != null) outputStream = new FileOutputStream(configurationSource.file);
                else {
                    assert configurationSource.outputStreamCreator != null;
                    outputStream = configurationSource.outputStreamCreator.get();
                }

                try (outputStream) {
                    write(configurationSource.serializer, outputStream);
                }
            }
            catch (Exception exception) {
                throw new InternalException(exception);
            }
        }
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

    public void readFile(Serializer serializer, File file) {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            read(serializer, inputStream);
        }
        catch (Exception exception) {
            throw new InternalException(exception);
        }
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

    public static <C extends SectionScheme> HitoriConfiguration<C> create(Key key, C rootSectionScheme, @Nullable ConfigurationSource configurationSource) {
        return new HitoriConfiguration<>(key, rootSectionScheme, configurationSource);
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
