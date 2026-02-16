package su.hitori.api.module.compatibility;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
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
     */
    CompatibilityLayer require(@NotNull Key key);

    /**
     * Adds a hook to other module enabling. If module with specified key enables, the hook will be called.
     * @param key key of module to add hook to
     * @param runnable hook itself
     * @return that CompatibilityLayer
     */
    CompatibilityLayer addEnableHook(@NotNull Key key, @NotNull Runnable runnable);

}
