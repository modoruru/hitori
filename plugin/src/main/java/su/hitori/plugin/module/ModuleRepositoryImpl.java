package su.hitori.plugin.module;

import net.kyori.adventure.key.Key;
import su.hitori.api.module.ModuleDescriptor;
import su.hitori.api.module.ModuleRepository;
import su.hitori.api.util.Pipeline;
import su.hitori.plugin.CorePlugin;

import java.io.File;
import java.util.Optional;
import java.util.Set;

public final class ModuleRepositoryImpl implements ModuleRepository {

    private final CorePlugin corePlugin;
    private final Pipeline<ModuleDescriptorImpl> descriptors = new Pipeline<>();

    public ModuleRepositoryImpl(CorePlugin corePlugin) {
        this.corePlugin = corePlugin;
    }

    Class<?> loadModuleSpecificClass(ModuleDescriptorImpl requestSource, String name, boolean resolve) {
        // maybe replace with a faster logic
        Class<?> clazz = null;
        for (ModuleDescriptorImpl descriptor : descriptors) {
            if(name.startsWith(descriptor.getExtendedMeta().packageName())) {
                try {
                    clazz = descriptor.getClassLoader().loadClass(requestSource, name, resolve);
                } catch (ClassNotFoundException _) {}
            }
        }
        return clazz;
    }

    void callEnableHooks(Key enabled) {
        for (ModuleDescriptorImpl descriptor : descriptors) {
            assert descriptor != null;
            if(!descriptor.isEnabled() || descriptor.key().equals(enabled)) continue;

            Runnable runnable = descriptor.getCompatibilityLayer().enableHooks.get(enabled);
            if(runnable == null) continue;

            try {
                runnable.run();
            }
            catch (Throwable ex) {
                ex.printStackTrace();
            }
        }
    }

    public void load(File folder) {
        File[] files = folder.listFiles();
        if(files == null) return;

        for (File file : files) {
            loadSingle(file);
        }

        enableAll();
    }

    void loadSingle(File moduleJarFile) {
        if(!moduleJarFile.isFile() || !moduleJarFile.getName().endsWith(".jar")) return;

        for (ModuleDescriptorImpl descriptor : descriptors) {
            if(descriptor.getJar().equals(moduleJarFile))
                throw new IllegalArgumentException("This jar already loaded as module!");
        }

        ModuleDescriptorImpl descriptor;
        try {
            descriptor = new ModuleDescriptorImpl(corePlugin, this);
            descriptor.reload(moduleJarFile, false, Set.of());
        }
        catch (Throwable ex) {
            ex.printStackTrace();
            return;
        }

        descriptors.addLast(descriptor.key(), descriptor);
    }

    void enableAll() {
        descriptors.forEach(ModuleDescriptorImpl::setupCompatibility);

        descriptors.sort((first, second) -> {
            if(first.getCompatibilityLayer().required.contains(second.key())) return 1;
            else if (second.getCompatibilityLayer().required.contains(first.key())) return -1;
            return 0;
        });

        descriptors.forEach(ModuleDescriptorImpl::enable);
    }

    public void disableAll() {
        descriptors.forEach(ModuleDescriptorImpl::disable);
    }

    @Override
    public Optional<ModuleDescriptor> getModule(Key key) {
        return Optional.ofNullable(descriptors.get(key));
    }

    @Override
    public boolean isModuleExists(Key key) {
        return descriptors.get(key) != null;
    }

    @Override
    public Set<Key> keySet() {
        return Set.copyOf(descriptors.order());
    }

}
