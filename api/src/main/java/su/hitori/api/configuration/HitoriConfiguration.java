package su.hitori.api.configuration;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.jspecify.annotations.Nullable;
import su.hitori.api.configuration.exception.InternalException;
import su.hitori.api.configuration.listener.ChangeCause;
import su.hitori.api.configuration.listener.FieldListener;
import su.hitori.api.configuration.listener.RegisteredFieldListener;
import su.hitori.api.configuration.serializer.Serializer;
import su.hitori.api.configuration.serializer.YAMLSerializer;
import su.hitori.api.module.enable.ConfigurationsRegistrar;
import su.hitori.api.logging.LoggerFactory;
import su.hitori.api.module.ModuleDescriptor;
import su.hitori.api.util.LoggerUtil;
import su.hitori.api.util.UnsafeUtil;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Instance of the configuration
 * @param <RootScheme> scheme for the configuration
 */
public final class HitoriConfiguration<RootScheme extends SectionScheme> implements Keyed {

    private static final Logger LOGGER = LoggerFactory.instance().create();

    private final Key key;
    private final RootScheme rootSectionScheme;
    private final @Nullable ConfigurationSource configurationSource;

    private final Executor executor;
    private final Context context;

    private HitoriConfiguration(Key key, RootScheme rootSectionScheme, @Nullable ConfigurationSource configurationSource) {
        this.key = key;
        this.rootSectionScheme = rootSectionScheme;
        this.configurationSource = configurationSource;

        this.executor = Executors.newCachedThreadPool();
        this.context = new Context(this);

        rootSectionScheme.root = SectionScheme.compileSectionNode(context, "", rootSectionScheme.getClass(), rootSectionScheme);

        if(configurationSource != null) readFromSource();
    }

    @Override
    public Key key() {
        return key;
    }

    /**
     * @return configuration scheme that is available for reading and writing fields
     * @throws IllegalStateException if configuration hasn't been read at least once
     */
    public RootScheme access() {
        if(context.rawData == null) throw notLoaded();
        return rootSectionScheme;
    }

    /**
     * @return root node of the {@link SectionScheme}
     */
    public RootScheme.Node rootNode() {
        assert rootSectionScheme.root != null;
        return rootSectionScheme.root;
    }

    /**
     * @return whether this configuration has default {@link ConfigurationSource} or not.
     */
    public boolean hasConfigurationSource() {
        return configurationSource != null;
    }

    /**
     * Reads configuration from the {@link ConfigurationSource}
     * @throws IllegalStateException if {@link HitoriConfiguration#hasConfigurationSource()} is false
     */
    public void readFromSource() {
        if(configurationSource == null) throw new IllegalStateException("This HitoriConfiguration doesn't have a default configurationSource.");
        readFromSource(configurationSource);
    }

    /**
     * Reads configuration from the {@link ConfigurationSource}
     * @param configurationSource configuration source to read from
     */
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

    /**
     * Writes configuration to the {@link ConfigurationSource}
     * @throws IllegalStateException if {@link HitoriConfiguration#hasConfigurationSource()} is false
     */
    public void writeToSource() {
        if(configurationSource == null) throw new IllegalStateException("This HitoriConfiguration doesn't has default configurationSource.");
        writeToSource(configurationSource);
    }

    /**
     * Writes configuration to the {@link ConfigurationSource}
     * @param configurationSource configuration source to write to
     */
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

            if(anyListeners()) compareDataAndCallListeners(Map.copyOf(context.rawData), Map.of());

            cleanupSectionRecursively(context.rawData);
        }
    }

    private static void cleanupSectionRecursively(Map<String, Object> rawSection) {
        for (Object value : rawSection.values()) {
            if(value instanceof Map<?, ?> childMap)
                cleanupSectionRecursively(UnsafeUtil.cast(childMap));
        }
        rawSection.clear();
    }

    /**
     * Writes configuration to the {@link OutputStream}
     * @param serializer serializer to use
     * @param output output stream to write to
     * @throws IllegalStateException if configuration hasn't been read at least once
     */
    public void write(Serializer serializer, OutputStream output) {
        if(context.rawData == null) throw notLoaded();

        synchronized (context.lock) {
            serializer.write(rootSectionScheme.root, context.rawData, output);
        }
    }

    /**
     * Writes configuration to the {@link OutputStream} asynchronously.
     * @param serializer serializer to use
     * @param output output stream to write to
     * @param executor executor to run write task
     * @throws IllegalStateException if configuration hasn't been read at least once
     */
    public void writeAsync(Serializer serializer, OutputStream output, Executor executor) {
        if(context.rawData == null) throw notLoaded();

        synchronized (context.lock) {
            Map<String, Object> copy = new HashMap<>(context.rawData);
            executor.execute(() -> serializer.write(rootSectionScheme.root, copy, output));
        }
    }

    /**
     * Reads configuration using {@link YAMLSerializer} serializer
     * @param input input stream to read from
     */
    public void readYaml(InputStream input) {
        read(YAMLSerializer.INSTANCE, input);
    }

    /**
     * Reads configuration from the file
     * @param serializer serializer to use
     * @param file file to read from
     */
    public void readFile(Serializer serializer, File file) {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            read(serializer, inputStream);
        }
        catch (Exception exception) {
            throw new InternalException(exception);
        }
    }

    /**
     * Reads configuration from the {@link InputStream}
     * @param serializer serializer to use
     * @param input input stream to read from
     */
    public void read(Serializer serializer, InputStream input) {
        synchronized (context.lock) {
            boolean anyListeners = anyListeners();
            Map<String, Object> originalData = anyListeners ? new HashMap<>() : null;

            if(context.rawData != null) {
                if(anyListeners) originalData.putAll(context.rawData);
                context.rawData.clear();
            }
            else context.rawData = new HashMap<>();

            serializer.read(LoggerFactory.instance().create(Serializer.class), rootSectionScheme.root, input, context.rawData);

            // last blocking and huge operation
            if(anyListeners) compareDataAndCallListeners(originalData, Map.copyOf(context.rawData));
        }
    }

    /**
     * Modules can register {@link FieldListener}s by invoking {@link Field#listen(ModuleDescriptor, FieldListener)}.
     * Field invokes {@link HitoriConfiguration}'s internal class {@link Context} which is actually stores all the registered listeners.<br>
     * This method iterates over all registered listeners and removes any registered by the passed {@link ModuleDescriptor}.
     * @throws IllegalStateException if the configuration has not been loaded at least once
     */
    public void unregisterAllFieldListeners(ModuleDescriptor descriptor) {
        if(context.rawData == null) throw notLoaded();

        for (Map<Key, RegisteredFieldListener> map : context.fieldListeners.values()) {
            map.keySet().removeIf(key -> key.equals(descriptor.key()));
        }
    }

    private boolean anyListeners() {
        return context.fieldListeners.values()
                .stream()
                .anyMatch(
                        map -> map.values()
                        .stream()
                        .anyMatch(registeredFieldListener -> registeredFieldListener.registrar().isEnabled())
                );
    }

    private boolean anyListeners(String fieldAbsolutePath) {
        Map<Key, RegisteredFieldListener> map = context.fieldListeners.get(fieldAbsolutePath);
        if(map == null || map.isEmpty()) return false;
        return map.values()
                .stream()
                .anyMatch(registeredFieldListener -> registeredFieldListener.registrar().isEnabled());
    }

    private @Nullable Field<?> resolveField(String absolutePath) {
        SectionScheme.Node node = rootSectionScheme.root;
        assert node != null && node.section() != null;

        String[] parts = absolutePath.split("\\.");
        for (int i = 0, length = parts.length - 1; i < length; i++) {
            assert node.section() != null;
            node = node.section().get(parts[i]);
        }

        assert node.section() != null;
        return Optional.ofNullable(node.section().get(parts[parts.length - 1]))
                .map(SectionScheme.Node::field)
                .orElse(null);
    }

    private void compareDataAndCallListeners(Map<String, Object> oldValues, Map<String, Object> newValues) {
        executor.execute(() -> {
            for (Map.Entry<String, Map<Key, RegisteredFieldListener>> entry : context.fieldListeners.entrySet()) {
                String fieldAbsolutePath = entry.getKey();

                Object oldValue = oldValues.get(fieldAbsolutePath);
                Object newValue = newValues.get(fieldAbsolutePath);

                compareFieldValuesAndCallListeners(fieldAbsolutePath, oldValue, newValue, entry.getValue().values());
            }
        });
    }

    private void compareFieldValuesAndCallListeners(String fieldAbsolutePath, @Nullable Object oldValue, @Nullable Object newValue, Collection<RegisteredFieldListener> listeners) {
        if(listeners.isEmpty()) return;

        if(oldValue == null && newValue == null) return;
        if(oldValue != null && newValue != null && (oldValue == newValue || oldValue.equals(newValue))) return;

        Field<?> field = resolveField(fieldAbsolutePath);
        if(field == null) throw InternalException.formatted("Can't find %s field", fieldAbsolutePath);

        listeners.removeIf(registeredFieldListener -> {
            if(registeredFieldListener.registrar().isEnabled()) return true;

            executor.execute(() -> {
                try {
                    registeredFieldListener.listener().handle(
                            UnsafeUtil.cast(oldValue == null ? field.defaultValue : oldValue),
                            UnsafeUtil.cast(newValue == null ? field.defaultValue : newValue),
                            ChangeCause.CONFIG_READ
                    );
                }
                catch (Throwable throwable) {
                    LOGGER.warning(String.format(
                            "Handler for the %s/%s field from the %s module has failed:\n%s",
                            key.asString(),
                            fieldAbsolutePath,
                            registeredFieldListener.registrar().key().asString(),
                            LoggerUtil.exceptionToString(throwable)
                    ));
                }
            });

            return false;
        });
    }

    private static IllegalStateException notLoaded() {
        return new IllegalStateException("Configuration is not loaded yet!");
    }

    /**
     * Create an instance of the configuration
     * @param key key of the configuration (used in registration, especially in the {@link ConfigurationsRegistrar}
     * @param rootSectionScheme scheme of the configuration
     * @param configurationSource configuration source. if not null, {@link HitoriConfiguration#readFromSource()} would be called immediately after the creation
     * @return created configuration instance
     * @param <C> type of the scheme
     */
    public static <C extends SectionScheme> HitoriConfiguration<C> create(Key key, C rootSectionScheme, @Nullable ConfigurationSource configurationSource) {
        return new HitoriConfiguration<>(key, rootSectionScheme, configurationSource);
    }

    /**
     * Shared context data
     */
    static class Context {

        private final HitoriConfiguration<?> configuration;
        final Object lock = new Object(); // for safe context changing
        final Map<String, Map<Key, RegisteredFieldListener>> fieldListeners = new HashMap<>();
        @Nullable Map<String, Object> rawData; // currently loaded data; null if no data is loaded

        private Context(HitoriConfiguration<?> configuration) {
            this.configuration = configuration;
        }

        @Nullable RegisteredFieldListener addListener(Field.Info info, ModuleDescriptor descriptor, FieldListener<?> fieldListener) {
            Map<Key, RegisteredFieldListener> fieldListeners = this.fieldListeners.computeIfAbsent(info.absolutePath(), _ -> new HashMap<>());
            RegisteredFieldListener registeredFieldListener = fieldListeners.get(descriptor.key());
            if(registeredFieldListener == null) return null;

            registeredFieldListener = new RegisteredFieldListener(descriptor, fieldListener);
            fieldListeners.put(descriptor.key(), registeredFieldListener);
            return registeredFieldListener;
        }

        @Nullable Object get(Field.Info info) {
            synchronized (lock) {
                assert rawData != null;
                return rawData.get(info.absolutePath());
            }
        }

        void set(Field.Info info, @Nullable Object value) {
            synchronized (lock) {
                assert rawData != null;
                String absolutePath = info.absolutePath();

                Object oldValue = value == null
                        ? rawData.remove(absolutePath)
                        : rawData.put(absolutePath, value);

                if(!configuration.anyListeners(absolutePath)) return;
                configuration.compareFieldValuesAndCallListeners(absolutePath, oldValue, value, fieldListeners.get(absolutePath).values());
            }
        }

    }

}
