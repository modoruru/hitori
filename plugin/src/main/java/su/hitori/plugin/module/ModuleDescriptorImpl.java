package su.hitori.plugin.module;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jspecify.annotations.Nullable;
import su.hitori.api.HitoriRegistryAccess;
import su.hitori.api.Version;
import su.hitori.api.command.CommandsModificationInfo;
import su.hitori.api.configuration.HitoriConfiguration;
import su.hitori.api.logging.LoggerFactory;
import su.hitori.api.module.Module;
import su.hitori.api.module.ModuleBootstrap;
import su.hitori.api.module.ModuleDescriptor;
import su.hitori.api.module.enable.EnableContext;
import su.hitori.api.registry.MappedRegistry;
import su.hitori.api.util.LoggerUtil;
import su.hitori.api.util.Task;
import su.hitori.plugin.CorePlugin;
import su.hitori.plugin.module.compatibility.CompatibilityLayerImpl;
import su.hitori.plugin.module.dependency.ModuleDependency;
import su.hitori.plugin.module.enable.CommandsRegistrarImpl;
import su.hitori.plugin.module.enable.ConfigurationsRegistrarImpl;
import su.hitori.plugin.module.enable.ListenersRegistrarImpl;
import su.hitori.plugin.module.exception.DependencyFailError;
import su.hitori.plugin.module.exception.MetaReadError;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public final class ModuleDescriptorImpl implements ModuleDescriptor {

    private static final Logger logger = LoggerFactory.instance().create(ModuleDescriptor.class);
    private @Nullable CorePlugin corePlugin;
    private final ModuleRepositoryImpl moduleRepository;

    private @Nullable Key key;
    private @Nullable ExtendedMeta extendedMeta;
    private @Nullable File currentJar;
    private @Nullable ModuleClassLoader classLoader;

    private @Nullable Module moduleInstance;
    private @Nullable ListenersRegistrarImpl listenersRegistrar;
    private @Nullable CommandsRegistrarImpl commandsRegistrar;
    private @Nullable ConfigurationsRegistrarImpl configurationsRegistrar;
    private @Nullable CompatibilityLayerImpl compatibilityLayer;

    private @Nullable EnableContext lastEnableContext;
    private @Nullable CommandsModificationInfo commandsModificationInfo;
    private final Set<ModuleDescriptorImpl> injected = new HashSet<>();
    private boolean bootstrapClassloaderSkip = true;
    private boolean enabling;
    private boolean enabled;
    private boolean loaded; // is jar loaded or not
    private boolean enabledOnce;
    private boolean compatibilitySetUp, compatibilitySetupFailed;

    public ModuleDescriptorImpl(ModuleRepositoryImpl moduleRepository) {
        this.moduleRepository = moduleRepository;
    }

    @Override
    public Key key() {
        assert key != null;
        return key;
    }

    public @Nullable ExtendedMeta extendedMeta() {
        return extendedMeta;
    }

    public @Nullable ClassLoader classLoader() {
        return classLoader;
    }

    @Nullable ModuleClassLoader getClassLoader() {
        return classLoader;
    }

    @Nullable CompatibilityLayerImpl getCompatibilityLayer() {
        return compatibilityLayer;
    }

    @Override
    public Module getInstance() {
        assert moduleInstance != null;
        return moduleInstance;
    }

    @Override
    public boolean isEnabling() {
        return enabling;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    boolean setupCompatibility() {
        if(!loaded || enabled || compatibilitySetUp) return true;
        try {
            assert moduleInstance != null;
            assert compatibilityLayer != null;
            moduleInstance.setupCompatibility(compatibilityLayer);
        }
        catch (Throwable exception) {
            logger.severe("Module caused an exception in setupCompatibility - cancelled enabling. Exception presented below.");
            logger.warning(LoggerUtil.exceptionToString(exception));
            compatibilitySetupFailed = true;
            return false;
        }

        return compatibilitySetUp = true;
    }

    boolean enable() {
        if(!loaded || enabled || enabling || compatibilitySetupFailed) return false;
        try {
            assert key != null;
            logger.info("Enabling module \"" + key.asString() + "\"");
            if(!compatibilitySetUp && !setupCompatibility()) return false;
            enabling = true;

            assert extendedMeta != null;

            // Incompatibility logging
            Set<String> notFoundModules = new HashSet<>();
            Set<String> incompatibleVersionsEntries = new HashSet<>();

            for (Map.Entry<Key, ModuleDependency> entry : extendedMeta.modulesDependencies().entrySet()) {
                Key requiredModule = entry.getKey();

                ModuleDescriptorImpl descriptor = moduleRepository.descriptors.get(requiredModule);
                if(descriptor == null || !descriptor.loaded || descriptor.compatibilitySetupFailed) {
                    if(entry.getValue().type == ModuleDependency.Type.HARD)
                        notFoundModules.add(requiredModule.asString());
                    continue;
                }

                assert descriptor.extendedMeta != null;
                Version presentVersion = descriptor.extendedMeta.moduleMeta().version();

                if(!entry.getValue().compatible(presentVersion))
                    incompatibleVersionsEntries.add(String.format("%s (required: %s, present: %s)", requiredModule.key(), entry.getValue(), presentVersion));
            }

            if(!notFoundModules.isEmpty() || !incompatibleVersionsEntries.isEmpty()) {
                StringBuilder builder = new StringBuilder("Failed to satisfy dependencies for ")
                        .append(key.asString())
                        .append(" module (enabling cancelled):");

                if(!notFoundModules.isEmpty()) {
                    builder.append("\n  Missing or unloaded dependencies:");
                    for (String entry : notFoundModules) {
                        builder.append("\n    - ").append(entry);
                    }
                }

                if(!incompatibleVersionsEntries.isEmpty()) {
                    builder.append("\n  Incompatible dependency versions:");
                    for (String entry : incompatibleVersionsEntries) {
                        builder.append("\n    - ").append(entry);
                    }
                }

                logger.warning(builder.toString());
                enabling = false;
                return false;
            }

            assert listenersRegistrar != null && commandsRegistrar != null && configurationsRegistrar != null;
            listenersRegistrar.frozen = false;
            commandsRegistrar.frozen = false;
            configurationsRegistrar.frozen = false;

            EnableContext context = lastEnableContext = new EnableContext(listenersRegistrar, commandsRegistrar, configurationsRegistrar, enabledOnce, new CompletableFuture<>());
            try {
                assert moduleInstance != null;
                moduleInstance.enable(context);
            }
            catch (Throwable exception) {
                logger.severe("Module caused an exception while enabling - disabling it. Exception presented below.");
                logger.warning(LoggerUtil.exceptionToString(exception));
                enabling = false;
                Task.async(this::disable, 0L); // To consider: Should we even call disable logic if enabling was failed? Some modules wouldn't handle properly such scenarios.
                return false;
            }

            listenersRegistrar.frozen = true;
            commandsRegistrar.frozen = true;

            for (Listener listener : listenersRegistrar.listeners) {
                assert corePlugin != null;
                Bukkit.getPluginManager().registerEvents(listener, corePlugin);
            }

            assert corePlugin != null;
            commandsModificationInfo = corePlugin.commandRegistryModifier().applyModificationsInBatch(
                    commandsRegistrar.commands,
                    Set.of()
            );

            for (Map.Entry<Key, HitoriConfiguration<?>> entry : configurationsRegistrar.configurations.entrySet()) {
                configurationsRegistrar.registry.register(entry.getKey(), entry.getValue());
            }

            enabling = false;
            enabled = true;

            if(!enabledOnce) enabledOnce = true;
        }
        catch (Throwable exception) {
            logger.warning(LoggerUtil.exceptionToString(exception));
            return false;
        }

        return true;
    }

    void disable() {
        if(!loaded || !enabled) return;
        try {
            assert key != null;
            logger.info("Disabling module \"" + key.asString() + "\"");
            disableInternal();
            enabled = false;
            compatibilitySetUp = false;
        }
        catch (Throwable exception) {
            logger.warning(LoggerUtil.exceptionToString(exception));
        }
    }

    private void disableInternal() {
        try {
            assert moduleInstance != null;
            moduleInstance.disable();
        }
        catch (Throwable exception) {
            logger.severe("Module caused an exception while disabling.");
            logger.warning(LoggerUtil.exceptionToString(exception));
        }

        assert listenersRegistrar != null && commandsRegistrar != null && corePlugin != null;
        for (Listener listener : listenersRegistrar.listeners) {
            HandlerList.unregisterAll(listener);
        }

        if(commandsModificationInfo != null) {
            corePlugin.commandRegistryModifier().undoBatch(commandsModificationInfo);
            commandsModificationInfo = null;
        }

        listenersRegistrar.listeners.clear();
        commandsRegistrar.commands.clear();
    }

    @Override
    public File getFolder() {
        assert currentJar != null && key != null;
        return new File(currentJar.getParentFile(), key.asString().replace(':', '_'));
    }

    public Optional<List<Key>> getReloadAffectedModules() {
        if(!enabled) return Optional.empty();

        assert classLoader != null;
        Set<ModuleDescriptorImpl> injected = classLoader.getInjectedModules();
        if(injected.isEmpty()) return Optional.empty();

        return Optional.of(
                injected.stream()
                        .map(Keyed::key)
                        .toList()
        );
    }

    public Optional<ModuleBootstrap> createModuleBoostrap() {
        assert extendedMeta != null && classLoader != null;
        if(extendedMeta.bootstrapClass() == null) return Optional.empty();

        try {
            Class<?> mainClass = classLoader.loadClass(extendedMeta.bootstrapClass());
            Constructor<?> constructor = mainClass.getConstructor();
            Object instance = constructor.newInstance();
            if(!(instance instanceof ModuleBootstrap moduleBootstrap)) throw new IllegalStateException("Created instance is not an instance of ModuleBootstrap");
            return Optional.of(moduleBootstrap);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    void corePlugin(CorePlugin corePlugin) {
        this.corePlugin = corePlugin;
    }

    void initializeJar(File jar) throws MetaReadError {
        ExtendedMeta meta = ExtendedMeta.readMetaFromJar(jar);
        Key newKey = meta.moduleMeta().key();

        boolean first = key == null;

        injected.clear();
        if(!first) {
            if(!newKey.equals(key))
                throw new IllegalStateException("Different keys in jars! Current: " + key.asString() + ", Present: " + newKey.asString());

            assert classLoader != null;
            injected.addAll(classLoader.getInjectedModules());

            try {
                classLoader.close();
            }
            catch (Throwable exception) {
                logger.warning(LoggerUtil.exceptionToString(exception));
            }
            loaded = false;
        }
        else if(moduleRepository.isModuleExists(newKey)) {
            throw new IllegalStateException("Module under this key already registered");
        }

        key = newKey;
        extendedMeta = meta;
        currentJar = jar;
        try {
            classLoader = new ModuleClassLoader(
                    new URL[]{jar.toURI().toURL()},
                    CorePlugin.class.getClassLoader(),
                    extendedMeta,
                    this,
                    moduleRepository
            );
        }
        catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        if(bootstrapClassloaderSkip)
            bootstrapClassloaderSkip = false;
    }

    public void reload(File jar, boolean autoEnable, boolean reloadInjected, Set<ModuleDescriptorImpl> skipReloadIfInjected) throws MetaReadError, DependencyFailError {
        if(enabled) disable();

        logger.info("Loading module from " + jar.getName());

        if(!bootstrapClassloaderSkip)
            initializeJar(jar);

        assert corePlugin != null && extendedMeta != null && key != null;
        if(!extendedMeta.hitoriDependency().compatible(corePlugin.version()))
            throw new DependencyFailError(String.format(
                    "%s requires hitori version %s, but version %s is installed.",
                    key.asString(),
                    extendedMeta.hitoriDependency(),
                    corePlugin.version()
            ));

        if(!extendedMeta.javaDependency().compatible(corePlugin.javaVersionFeature()))
            throw new DependencyFailError(String.format(
                    "%s requires java version %s, but version %s is installed.",
                    key.asString(),
                    extendedMeta.javaDependency(),
                    corePlugin.javaVersionFeature()
            ));

        injected.removeAll(skipReloadIfInjected);

        assert classLoader != null;
        moduleInstance = classLoader.create();
        listenersRegistrar = new ListenersRegistrarImpl();
        commandsRegistrar = new CommandsRegistrarImpl();
        if(configurationsRegistrar != null) {
            for (Key key : configurationsRegistrar.configurations.keySet()) {
                ((MappedRegistry<HitoriConfiguration<?>>) configurationsRegistrar.registry).remove(key);
            }

            for (HitoriConfiguration<?> configuration : configurationsRegistrar.registry.elements()) {
                configuration.unregisterAllFieldListeners(this);
            }

            configurationsRegistrar.configurations.clear();
        }
        configurationsRegistrar = new ConfigurationsRegistrarImpl(corePlugin.access(HitoriRegistryAccess.CONFIGURATION).orElseThrow());
        compatibilityLayer = new CompatibilityLayerImpl(extendedMeta.modulesDependencies(), moduleRepository);

        loaded = true;
        compatibilitySetupFailed = false;

        if(injected.isEmpty() || !reloadInjected) {
            if(autoEnable && enable()) {
                callOutcomingHooks(null);
                callIncomingHooks();
            }
            return;
        }

        for (ModuleDescriptorImpl descriptor : injected) {
            assert descriptor.getJar() != null;
            descriptor.reload(descriptor.getJar(), false, true, injected);
        }

        if(!autoEnable && !enable()) return;

        for (ModuleDescriptorImpl descriptor : injected) {
            if(!descriptor.enable()) return;
        }

        assert this.key != null;

        for (ModuleDescriptorImpl descriptor : injected) {
            descriptor.callOutcomingHooks(key);
            descriptor.callIncomingHooks();
        }

        callOutcomingHooks(null);
        callIncomingHooks();
    }

    // todo: change how the enable hooks is called as CompletableFuture for finishing is not a very good option here.
    // comment on todo: we don't have another options
    void callIncomingHooks() {
        assert key != null;
        moduleRepository.callEnableHooks(key);
        assert lastEnableContext != null;
        lastEnableContext.enableHooksFuture().complete(null);
    }

    void callOutcomingHooks(@Nullable Key ignore) {
        assert compatibilityLayer != null;

        for (Map.Entry<Key, Runnable> entry : compatibilityLayer.enableHooks.entrySet()) {
            Key key = entry.getKey();
            if(ignore != null && key.compareTo(ignore) == 0 || compatibilityLayer.triggered.contains(key)) continue;

            moduleRepository.getModule(key).ifPresent(hookedDescriptor -> {
                if(hookedDescriptor.isEnabled()) {
                    try {
                        compatibilityLayer.triggered.add(key);
                        entry.getValue().run();
                    }
                    catch (Throwable exception) {
                        logger.warning(LoggerUtil.exceptionToString(exception));
                    }
                }
            });
        }
    }

    public @Nullable File getJar() {
        return currentJar;
    }

}
