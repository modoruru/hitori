package su.hitori.plugin.module;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.provider.classloader.ConfiguredPluginClassLoader;
import io.papermc.paper.plugin.provider.classloader.PluginClassLoaderGroup;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import su.hitori.api.logging.LoggerFactory;
import su.hitori.api.module.ModuleBootstrap;
import su.hitori.api.module.ModuleDescriptor;
import su.hitori.api.module.ModuleRepository;
import su.hitori.api.util.LoggerUtil;
import su.hitori.api.util.Pipeline;
import su.hitori.plugin.CorePlugin;

import java.io.File;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

public final class ModuleRepositoryImpl implements ModuleRepository {

    private @Nullable CorePlugin corePlugin;
    private final Logger logger = LoggerFactory.instance().create();
    private final Pipeline<ModuleDescriptorImpl> descriptors = new Pipeline<>();

    public void corePlugin(CorePlugin corePlugin) {
        if(this.corePlugin == null && corePlugin != null) {
            this.corePlugin = corePlugin;
        }
    }

    @Nullable Class<?> loadModuleSpecificClass(ModuleDescriptorImpl requestSource, String name, boolean resolve) {
        // maybe replace with a faster logic
        Class<?> clazz = null;
        for (ModuleDescriptorImpl descriptor : descriptors) {
            assert descriptor.getExtendedMeta() != null && descriptor.getClassLoader() != null;
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
            assert descriptor != null && descriptor.getCompatibilityLayer() != null;
            if(!descriptor.isEnabled() || descriptor.key().equals(enabled)) continue;

            Runnable runnable = descriptor.getCompatibilityLayer().enableHooks.get(enabled);
            if(runnable == null) continue;

            try {
                runnable.run();
            }
            catch (Throwable exception) {
                logger.warning(LoggerUtil.exceptionToString(exception));
            }
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    private void enablePaperAccessHook() {
        // this code is a temporary solution and should probably be replaced with a more robust
        assert corePlugin != null;
        if(!(corePlugin.getClass().getClassLoader() instanceof ConfiguredPluginClassLoader paperPluginClassLoader)) {
            logger.warning("failed to initialize paper access hook: class loader is not an instance of PaperPluginClassLoader");
            return;
        }

        PluginClassLoaderGroup group = paperPluginClassLoader.getGroup();
        if(group == null) {
            logger.warning("failed to initialize paper access hook: plugin class loader group is null.");
            return;
        }

        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            if(plugin == corePlugin.plugin() || plugin.getName().equalsIgnoreCase("commandapi")) continue;
            if(!(plugin.getClass().getClassLoader() instanceof ConfiguredPluginClassLoader pluginClassLoader)) {
                logger.warning(plugin.getName() + " class loader is not an instance of ConfiguredPluginClassLoader, it's actually: " + plugin.getClass().getClassLoader().getClass().getSimpleName());
                continue;
            }

            group.add(pluginClassLoader);
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public void bootstrap(File folder, BootstrapContext context) {
        File[] files = folder.listFiles();
        if(files == null) return;

        for (File file : files) {
            loadSingle(file);
        }

        descriptors.forEach(descriptor -> {
            Optional<ModuleBootstrap> optBootstrap = descriptor.createModuleBoostrap();
            if(optBootstrap.isEmpty()) return;
            optBootstrap.get().bootstrap(context);
        });
    }

    void loadSingle(File moduleJarFile) {
        if(!moduleJarFile.isFile() || !moduleJarFile.getName().endsWith(".jar")) return;

        for (ModuleDescriptorImpl descriptor : descriptors) {
            assert descriptor.getJar() != null;
            if(descriptor.getJar().equals(moduleJarFile))
                throw new IllegalArgumentException("This jar already loaded as module!");
        }

        ModuleDescriptorImpl descriptor;
        try {
            descriptor = new ModuleDescriptorImpl(this);
            descriptor.initializeJar(moduleJarFile);
        }
        catch (Throwable exception) {
            logger.warning(LoggerUtil.exceptionToString(exception));
            return;
        }

        descriptors.addLast(descriptor.key(), descriptor);
    }

    public void enableAll() {
        enablePaperAccessHook();

        descriptors.forEach(descriptor -> {
            descriptor.reload(descriptor.getJar(), false, Set.of());
            descriptor.corePlugin(corePlugin);
        });
        descriptors.forEach(ModuleDescriptorImpl::setupCompatibility);

        descriptors.sort((first, second) -> {
            assert first.getCompatibilityLayer() != null && second.getCompatibilityLayer() != null;
            if(first.getCompatibilityLayer().required.contains(second.key())) return 1;
            else if (second.getCompatibilityLayer().required.contains(first.key())) return -1;
            return 0;
        });

        descriptors.forEach(ModuleDescriptorImpl::enable);

        for (ModuleDescriptorImpl descriptor : descriptors) {
            descriptor.callOutcomingHooks(null);
            descriptor.callIncomingHooks();
        }
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
