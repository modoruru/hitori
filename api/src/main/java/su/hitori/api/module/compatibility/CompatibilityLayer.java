package su.hitori.api.module.compatibility;

import net.kyori.adventure.key.Key;
import su.hitori.api.module.Module;

/**
 * Class used to describe compatibility with other modules.
 * <p>
 * Once {@link Module#setupCompatibility(CompatibilityLayer)} is called, instance of this class is not mutable.
 * All operations will be just ignored.
 */
public interface CompatibilityLayer {

    /**
     * Marks module as required
     * If it doesn't exist, module will not be loaded
     * @return that CompatibilityLayer
     * @deprecated Method now does nothing, see <a href="https://github.com/modoruru/hitori/wiki/Migrating#from-1xx-to-200">Migrating from 1.x.x to 2.0.0</a>
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    CompatibilityLayer require(Key key);

    /**
     * Adds a hook to other module enabling. If module with specified key enables, the hook will be called.
     * @param key key of module to add hook to
     * @param runnable hook itself
     * @return that CompatibilityLayer
     */
    CompatibilityLayer addEnableHook(Key key, Runnable runnable);

}
