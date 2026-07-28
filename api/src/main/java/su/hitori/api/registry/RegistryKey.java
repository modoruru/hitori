package su.hitori.api.registry;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;

import java.util.Objects;

/**
 * A key for registry
 * @param key the string based key
 * @param clazz class of stored elements
 * @param <E> type of stored elements
 */
public record RegistryKey<E extends Keyed>(Key key, Class<E> clazz) {

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof RegistryKey<?>(Key key1, Class<?> clazz1))) return false;
        return key.equals(key1) && (clazz.isAssignableFrom(clazz1) || clazz1.isAssignableFrom(clazz));
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, clazz);
    }

}
