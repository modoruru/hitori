package su.hitori.api.registry;

import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A class holding Registry's
 */
public interface RegistryAccess {

    /**
     * Returns a registry instance
     * @throws IllegalAccessError if this registryAccess do not have access to registry under such key
     */
    <E extends Keyed> Optional<Registry<@NotNull E>> access(RegistryKey<E> key) throws IllegalAccessError;

}
