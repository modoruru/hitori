package su.hitori.api.registry;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

/**
 * Basic implementation of registry based on Map
 * @param <E> elements type
 */
@NotNullByDefault
public class MappedRegistry<E extends Keyed> implements Registry<E> {

    private final RegistryKey<E> key;
    private final Map<Key, E> map;

    public MappedRegistry(RegistryKey<E> key) {
        this.key = key;
        this.map = new HashMap<>();
    }

    public void clear() {
        map.clear();
    }

    public void remove(Key key) {
        map.remove(key);
    }

    @Override
    public boolean register(Key key, E element) {
        if(map.containsKey(key)) return false;
        map.put(key, element);
        return true;
    }

    @Override
    public boolean hasKey(Key key) {
        return map.containsKey(key);
    }

    @Override
    public Collection<E> elements() {
        return List.copyOf(map.values());
    }

    @Override
    public Set<Key> keys() {
        return Set.copyOf(map.keySet());
    }

    @Override
    public @Nullable E get(Key key) {
        return map.get(key);
    }

    @Override
    public Optional<E> getOptional(Key key) {
        return Optional.ofNullable(map.get(key));
    }

    @Override
    public Stream<E> stream() {
        return map.values().stream();
    }

    @Override
    public RegistryKey<E> key() {
        return key;
    }

}
