package su.hitori.plugin.module.compatibility;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import su.hitori.api.module.compatibility.CompatibilityLayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class CompatibilityLayerImpl implements CompatibilityLayer {

    public final Set<Key> required = new HashSet<>();
    public final Map<Key, Runnable> enableHooks = new HashMap<>();
    public boolean frozen;

    @Override
    public CompatibilityLayer require(@NotNull Key key) {
        if(frozen) return this;
        required.add(key);
        return this;
    }

    @Override
    public CompatibilityLayer addEnableHook(@NotNull Key key, @NotNull Runnable runnable) {
        if(frozen) return this;
        enableHooks.put(key, runnable);
        return this;
    }

}
