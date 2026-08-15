package su.hitori.api.registry;

import net.kyori.adventure.key.Keyed;

import java.util.Optional;

/**
 * Class holding the Registries
 */
public interface RegistryAccess {

    /**
     * Returns a registry instance
     * @throws IllegalAccessError if this registryAccess do not have access to registry under such key
     */
    <E extends Keyed> Optional<Registry<E>> access(RegistryKey<E> key) throws IllegalAccessError;

    default <E extends Keyed> Registry<E> accessOrThrow(RegistryKey<E> key) {
        return access(key).orElseThrow();
    }

}
