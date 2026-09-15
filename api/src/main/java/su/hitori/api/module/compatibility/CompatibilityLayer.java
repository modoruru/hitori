package su.hitori.api.module.compatibility;

import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;
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
     * Adds a hook that triggers when another module is enabled.
     * @param key key of the module to add hook to
     * @param runnable hook itself
     * @throws IllegalStateException if the module is specified as a dependency but the module version is incompatible. if dependency is of hard type, it'll also throw if the module is not present at all
     * @throws IllegalArgumentException if the requested module is not specified as a dependency in hitori.module.json
     * @see CompatibilityLayer#compatible(Key)
     * @return that CompatibilityLayer
     */
    CompatibilityLayer addEnableHook(Key key, Runnable runnable);


    /**
     * Checks if the module specified as a dependency is compatible with the currently installed one. Can be useful to check soft dependencies before calling {@link CompatibilityLayer#addEnableHook(Key, Runnable)}.
     * @param key key of the module to check
     * @throws IllegalArgumentException if the requested module is not specified as a dependency in hitori.module.json
     * @return null, if module is not present at all
     */
    @Nullable Boolean compatible(Key key);

}
