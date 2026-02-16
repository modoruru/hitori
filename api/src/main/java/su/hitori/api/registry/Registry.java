package su.hitori.api.registry;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.NotNullByDefault;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Registry of keyed elements
 * @param <E> elements type
 */
@NotNullByDefault
public interface Registry<E extends Keyed> {

    /**
     * Returns the key of this registry.
     * @return registryKey
     */
    RegistryKey<E> key();

    /**
     * Registers new element under specified key
     * @param key key of element
     * @param element element to register
     * @return if module successfully registered, false otherwise
     */
    boolean register(Key key, E element);

    /**
     * Does registry have an element registered under specified key
     * @param key key to check
     * @return does registry have such element
     */
    boolean hasKey(Key key);

    /**
     * All elements registered in this registry
     * @return a collection containing all the elements
     */
    Collection<E> elements();

    /**
     * Keys of all elements registered in this registry.
     * @return a set containing all the keys
     */
    Set<Key> keys();

    /**
     * Returns an element from this registry.
     * @param key key of the element
     * @return element or null if element under this key is not registered
     */
    @Nullable E get(Key key);

    /**
     * Returns an optional element from this registry
     * @param key key of the element
     * @return element or empty if element under this key is not registered
     */
    Optional<E> getOptional(Key key);

    /**
     * Applies consumer if element under such key registered in registry.
     * @param key key of the element
     * @param consumer consumer to apply
     */
    default void consume(Key key, Consumer<E> consumer) {
        getOptional(key).ifPresent(consumer);
    }

    /**
     * Returns a stream of all registered elements
     * @return stream containing all the elements
     */
    Stream<E> stream();

}
