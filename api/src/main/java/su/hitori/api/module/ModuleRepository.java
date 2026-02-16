package su.hitori.api.module;

import net.kyori.adventure.key.Key;
import su.hitori.api.util.UnsafeUtil;

import java.util.Optional;
import java.util.Set;

/**
 * Module repository containing all registered in this session Modules
 */
public interface ModuleRepository {

    /**
     * Returns a module descriptor based on module key
     * @param key module key
     * @return module descriptor or empty if such module does not exist
     */
    Optional<ModuleDescriptor> getModule(Key key);

    /**
     * Returns a module cast to some type.
     * @param key module key
     * @return cast module under this key or null, if there's no such module, or if module type is wrong
     * @param <M> module type class
     */
    default <M extends Module> Optional<M> getUnsafe(Key key) {
        return getModule(key)
                .map(ModuleDescriptor::getInstance)
                .map(UnsafeUtil::cast);
    }

    /**
     * Checks if module under specified key exists in session
     * @param key module key
     * @return does module exists
     */
    boolean isModuleExists(Key key);

    /**
     * Keys of all modules registered in this session
     * @return set containing all keys
     */
    Set<Key> keySet();

}
