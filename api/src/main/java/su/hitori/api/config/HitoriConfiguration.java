package su.hitori.api.config;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.jspecify.annotations.Nullable;
import su.hitori.api.util.UnsafeUtil;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class HitoriConfiguration<C extends ConfigurationScheme> implements Keyed {

    private final Key key;
    private final C scheme;

    private final Context context;
    private CompletableFuture<C> schemeFuture;

    private HitoriConfiguration(Key key, C scheme) {
        this.key = key;
        this.scheme = scheme;

        this.context = new Context();
        this.schemeFuture = new CompletableFuture<C>();

        scheme.compileAndAssignContext(context);
    }

    private void postContextOpen() {
        schemeFuture.complete(scheme);
        schemeFuture = new CompletableFuture<>();
    }

    /**
     * Writes defaults to the all fields of the instance.
     */
    public synchronized void defaults() {
        synchronized (context.lock) {
            if(context.rawData == null) {
                context.rawData = new HashMap<>();
                postContextOpen();
                return;
            }

            cleanupSectionRecursively(context.rawData);
            postContextOpen();
        }
    }

    private void cleanupSectionRecursively(Map<String, Object> map) {
        for (Object value : map.values()) {
            if(value instanceof Map<?, ?> childMap)
                cleanupSectionRecursively(UnsafeUtil.cast(childMap));
        }
        map.clear();
    }

    public C access() {
        if(context.rawData == null) throw new IllegalStateException("Configuration is not loaded yet!");
        return scheme;
    }

    public void access(Consumer<C> access) {
        access.accept(access());
    }

    public CompletableFuture<C> accessInFuture() {
        return schemeFuture;
    }

    public static <C extends ConfigurationScheme> HitoriConfiguration<C> create(Key key, C scheme) {
        return new HitoriConfiguration<>(key, scheme);
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

        @Nullable Object get(Field.FieldInfo fieldInfo) {
            synchronized (lock) {
                assert rawData != null;

                assert fieldInfo.absolutePath() != null;
                String[] path = fieldInfo.absolutePath().split("\\.");
                Map<String, Object> node = rawData;
                for (int i = 0, length = path.length; i < length; i++) {
                    Object rawValue = node.get(path[i]);
                    if(rawValue == null) return null;

                    if(i != length - 1) {
                        node = UnsafeUtil.cast(rawValue);
                        continue;
                    }

                    return rawValue;
                }

                return null;
            }
        }

        void set(Field.FieldInfo fieldInfo, @Nullable Object value) {
            synchronized (lock) {
                assert rawData != null;

                assert fieldInfo.absolutePath() != null;
                String[] path = fieldInfo.absolutePath().split("\\.");
                Map<String, Object> node = rawData;
                for (int i = 0, length = path.length - 1; i < length; i++) {
                    String pathPartName = path[i];
                    Object rawValue = node.get(pathPartName);
                    if(rawValue == null && value == null) return; // value is null - default and the section is null already - it holds defaults by our definitions

                    if(rawValue == null) node.put(pathPartName, node = new HashMap<>());
                    else node = UnsafeUtil.cast(rawValue);
                }

                if(value == null) node.remove(fieldInfo.name());
                else node.put(fieldInfo.name(), value);
            }
        }

    }

}
