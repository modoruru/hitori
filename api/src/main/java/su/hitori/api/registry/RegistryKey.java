package su.hitori.api.registry;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;

/**
 * A key for registry
 * @param key the string based key
 * @param clazz class of stored elements
 * @param <E> type of stored elements
 */
public record RegistryKey<E extends Keyed>(Key key, Class<E> clazz) {

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof RegistryKey<?> anotherKey)) return false;
        return key.equals(anotherKey.key) && clazz.isAssignableFrom(anotherKey.clazz);
    }

}
