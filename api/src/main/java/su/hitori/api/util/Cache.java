package su.hitori.api.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public final class Cache<K, V> {

    private final Map<K, ValueWrapper<V>> map;

    private final long retainTime;
    private final ScheduledExecutorService scheduledExecutorService;
    private final BiConsumer<K, V> removalListener;
    private final boolean callListenerOnlyOnAutoRemoval;

    private Cache(Map<K, V> initialValues, long retainTime, ScheduledExecutorService scheduledExecutorService, BiConsumer<K, V> removalListener, boolean callListenerOnlyOnAutoRemoval) {
        if(retainTime < 1 || retainTime > 60_000L) throw new IllegalArgumentException("retainTime can't be below 1 and more than 60000 (2ms to 1min)");

        this.map = new HashMap<>();
        this.retainTime = retainTime;
        this.scheduledExecutorService = scheduledExecutorService;
        this.removalListener = removalListener;
        this.callListenerOnlyOnAutoRemoval = callListenerOnlyOnAutoRemoval;

        for (Map.Entry<K, V> entry : initialValues.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public static <K, V> Builder<K, V> builder(@NotNull ScheduledExecutorService scheduledExecutorService) {
        return new Builder<>(scheduledExecutorService);
    }

    private void scheduleRemoveTask(K key, ValueWrapper<V> wrapper) {
        if(wrapper.removeTask != null && !wrapper.removeTask.isDone())
            wrapper.removeTask.cancel(true);

        wrapper.removeTask = scheduledExecutorService.schedule(() -> {
            internalRemove(key, true);
            return wrapper.value;
        }, retainTime, TimeUnit.MILLISECONDS);
    }

    public V put(@NotNull K key, @NotNull V value) {
        ValueWrapper<V> wrapper = new ValueWrapper<>(value);
        scheduleRemoveTask(key, wrapper);

        ValueWrapper<V> previouslyAssociated = map.put(key, wrapper);
        if(previouslyAssociated == null) return null;

        previouslyAssociated.removeTask.cancel(true);
        return previouslyAssociated.value;
    }

    public V get(@NotNull K key) {
        ValueWrapper<V> wrapper = map.get(key);
        if(wrapper == null) return null;
        scheduleRemoveTask(key, wrapper);
        return wrapper.value;
    }

    public @Nullable V remove(@NotNull K key) {
        return internalRemove(key, false);
    }

    private V internalRemove(K key, boolean autoRemove) {
        ValueWrapper<V> wrapper = map.get(key);
        if(wrapper == null) return null;
        if(!autoRemove)
            wrapper.removeTask.cancel(true);

        if(removalListener != null && !callListenerOnlyOnAutoRemoval && autoRemove)
            removalListener.accept(key, wrapper.value);

        return wrapper.value;
    }

    public Map<K, V> asMap() {
        Map<K, V> result = new HashMap<>();
        for (Map.Entry<K, ValueWrapper<V>> entry : map.entrySet()) {
            result.put(entry.getKey(), entry.getValue().value);
        }
        return result;
    }

    public void clear() {
        map.clear();
    }

    private static final class ValueWrapper<V> {

        final V value;
        ScheduledFuture<V> removeTask;

        ValueWrapper(V value) {
            this.value = value;
        }

    }

    public static final class Builder<K, V> {

        private final ScheduledExecutorService scheduledExecutorService;
        private long retainTime;
        private Map<K, V> initialValues;
        private BiConsumer<K, V> removalListener;
        private boolean callListenerOnlyOnAutoRemoval;

        Builder(ScheduledExecutorService scheduledExecutorService) {
            this.scheduledExecutorService = scheduledExecutorService;
        }

        public Builder<K, V> withRetainTime(long retainTime) {
            this.retainTime = retainTime;
            return this;
        }


        public Builder<K, V> withInitialValues(Map<K, V> initialValues) {
            this.initialValues = initialValues;
            return this;
        }

        public Builder<K, V> withRemovalListener(BiConsumer<K, V> removalListener, boolean callListenerOnlyOnAutoRemoval) {
            this.removalListener = removalListener;
            this.callListenerOnlyOnAutoRemoval = callListenerOnlyOnAutoRemoval;
            return this;
        }

        public Cache<K, V> build() {
            return new Cache<>(
                    initialValues == null ? Map.of() : initialValues,
                    retainTime,
                    scheduledExecutorService,
                    removalListener,
                    callListenerOnlyOnAutoRemoval
            );
        }

    }

}
