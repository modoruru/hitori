package su.hitori.plugin.module.compatibility;

import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;
import su.hitori.api.module.compatibility.CompatibilityLayer;
import su.hitori.plugin.module.ExtendedMeta;
import su.hitori.plugin.module.ModuleDescriptorImpl;
import su.hitori.plugin.module.ModuleRepositoryImpl;
import su.hitori.plugin.module.dependency.ModuleDependency;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class CompatibilityLayerImpl implements CompatibilityLayer {

    public final Map<Key, ModuleDependency> modulesDependencies;
    public final ModuleRepositoryImpl moduleRepository;

    public final Map<Key, Runnable> enableHooks;
    public final Set<Key> triggered;

    public boolean frozen;

    public CompatibilityLayerImpl(Map<Key, ModuleDependency> modulesDependencies, ModuleRepositoryImpl moduleRepository) {
        this.modulesDependencies = modulesDependencies;
        this.moduleRepository = moduleRepository;

        this.enableHooks = new HashMap<>();
        this.triggered = new HashSet<>();
    }

    @Override
    public CompatibilityLayer require(Key key) {
        return this;
    }

    @Override
    public CompatibilityLayer addEnableHook(Key key, Runnable runnable) {
        if(frozen) return this;

        ModuleDependency dependency = modulesDependencies.get(key);
        if(dependency == null) throw new IllegalArgumentException("Module \"" + key.asString() + "\" is not specified as dependency.");

        Boolean compatible = compatible(key, dependency);
        if(compatible == null) {
            if(dependency.type == ModuleDependency.Type.HARD)
                throw new IllegalStateException("Module \"" + key.asString() + "\" is specified as a dependency, but is not present currently.");
            return this;
        }
        if(!compatible) throw new IllegalStateException("Module \"" + key.asString() + "\" is specified as a dependency but is not compatible with currently installed version.");

        enableHooks.put(key, runnable);
        return this;
    }

    private @Nullable Boolean compatible(Key key, ModuleDependency dependency) {
        ModuleDescriptorImpl descriptor = moduleRepository.descriptors.get(key);
        if(descriptor == null) return null;

        ExtendedMeta extendedMeta = descriptor.extendedMeta();
        if(extendedMeta == null) return null;

        return dependency.compatible(extendedMeta.moduleMeta().version());
    }

    @Override
    public @Nullable Boolean compatible(Key key) {
        ModuleDependency dependency = modulesDependencies.get(key);
        if(dependency == null) return null;
        return compatible(key, dependency);
    }

}
