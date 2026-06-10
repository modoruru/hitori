package su.hitori.plugin.module;

import dev.jorel.commandapi.CommandAPICommand;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;
import su.hitori.api.logging.LoggerFactory;
import su.hitori.api.module.Module;
import su.hitori.api.module.ModuleDescriptor;
import su.hitori.api.module.enable.EnableContext;
import su.hitori.api.util.LoggerUtil;
import su.hitori.api.util.Task;
import su.hitori.plugin.CorePlugin;
import su.hitori.plugin.module.compatibility.CompatibilityLayerImpl;
import su.hitori.plugin.module.enable.CommandsRegistrarImpl;
import su.hitori.plugin.module.enable.ListenersRegistrarImpl;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/*
Work pipeline explanation

# Default module load behavior
1. load
2. call setupCompatibility
3. call enable
4. call enable hooks module have created during setupCompatibility
5. call third-party hooks which waits that module to enable

# Module reload behavior with injected modules
1. load
2. call setupCompatibility
3. call Module#enable(EnableContext)
4. load, call setupCompatibility and call enable for every injected modules, and call enable hooks of injected module except originally reloaded module
5. call third-party hooks for reloaded module
 */
public final class ModuleDescriptorImpl implements ModuleDescriptor {

    private static final Logger logger = LoggerFactory.instance().create();
    private final CorePlugin corePlugin;
    private final ModuleRepositoryImpl moduleRepository;

    private @Nullable Key key;
    private @Nullable ExtendedMeta extendedMeta;
    private @Nullable File currentJar;
    private @Nullable ModuleClassLoader classLoader;

    private @Nullable Module moduleInstance;
    private @Nullable ListenersRegistrarImpl listenersRegistrar;
    private @Nullable CommandsRegistrarImpl commandsRegistrar;
    private @Nullable CompatibilityLayerImpl compatibilityLayer;

    private @Nullable EnableContext lastEnableContext;
    private boolean enabling;
    private boolean enabled;
    private boolean loaded; // is jar loaded or not
    private boolean enabledOnce;
    private boolean compatibilitySetUp;

    public ModuleDescriptorImpl(CorePlugin corePlugin, ModuleRepositoryImpl moduleRepository) {
        this.corePlugin = corePlugin;
        this.moduleRepository = moduleRepository;
    }

    public @Nullable ClassLoader classLoader() {
        return classLoader;
    }

    @Nullable ExtendedMeta getExtendedMeta() {
        return extendedMeta;
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
            moduleInstance.setupCompatibility(compatibilityLayer);
        }
        catch (Throwable exception) {
            logger.severe("Module caused an exception in setupCompatibility - cancelled enabling. Exception presented below.");
            logger.warning(LoggerUtil.exceptionToString(exception));
            return false;
        }

        return compatibilitySetUp = true;
    }

    void enable() {
        enable(true, null);
    }

    void enable(boolean callIncomingHooks, Key ignoreOurHookFor) {
        if(!loaded || enabled || enabling) return;
        try {
            assert key != null;
            logger.info("Enabling module \"" + key.asString() + "\"");
            if(!compatibilitySetUp && !setupCompatibility()) return;
            enabling = true;

            Set<String> notFound = new HashSet<>();
            assert compatibilityLayer != null;
            for (Key requiredModule : compatibilityLayer.required) {
                if(!requiredModule.equals(key) && moduleRepository.getModule(requiredModule).isEmpty())
                    notFound.add(requiredModule.asString());
            }

            if(!notFound.isEmpty()) {
                logger.warning("Module \"" + key.asString() + "\" requires unknown modules: " + String.join(", ", notFound) + ". Enabling cancelled.");
                enabling = false;
                return;
            }

            assert listenersRegistrar != null && commandsRegistrar != null;
            listenersRegistrar.frozen = false;
            commandsRegistrar.frozen = false;

            EnableContext context = lastEnableContext = new EnableContext(listenersRegistrar, commandsRegistrar, enabledOnce, new CompletableFuture<>());
            try {
                assert moduleInstance != null;
                moduleInstance.enable(context);
            }
            catch (Throwable exception) {
                logger.severe("Module caused an exception while enabling - disabling it. Exception presented below.");
                logger.warning(LoggerUtil.exceptionToString(exception));
                enabling = false;
                Task.async(this::disable, 0L);
                return;
            }

            listenersRegistrar.frozen = true;
            commandsRegistrar.frozen = true;

            for (Listener listener : listenersRegistrar.listeners) {
                Bukkit.getPluginManager().registerEvents(listener, corePlugin);
            }

            Task.runGlobally(() -> {
                for (CommandAPICommand command : commandsRegistrar.commands) {
                    command.register(corePlugin);
                }
            }, 1L);

            for (Map.Entry<Key, Runnable> entry : compatibilityLayer.enableHooks.entrySet()) {
                Key key = entry.getKey();
                if(ignoreOurHookFor != null && key.compareTo(ignoreOurHookFor) == 0) continue;

                moduleRepository.getModule(key).ifPresent(descriptor -> {
                    if(descriptor.isEnabled()) {
                        try {
                            entry.getValue().run();
                        }
                        catch (Throwable exception) {
                            logger.warning(LoggerUtil.exceptionToString(exception));
                        }
                    }
                });
            }

            if(callIncomingHooks) {
                moduleRepository.callEnableHooks(key);
                context.enableHooksFuture().complete(null);
            }

            enabling = false;
            enabled = true;

            if(!enabledOnce) enabledOnce = true;
        }
        catch (Throwable exception) {
            logger.warning(LoggerUtil.exceptionToString(exception));
        }
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

        assert listenersRegistrar != null && commandsRegistrar != null;
        for (Listener listener : listenersRegistrar.listeners) {
            HandlerList.unregisterAll(listener);
        }

        // Unregister command aliases
        Set<String> toUnregister = new HashSet<>();
        for (CommandAPICommand command : commandsRegistrar.commands) {
            toUnregister.add(command.getName());
            toUnregister.addAll(Arrays.asList(command.getAliases()));
        }

        Task.ensureSync(() -> {
            try {
                Method method = Class.forName("dev.jorel.commandapi.CommandAPI").getDeclaredMethod("unregister", String.class, boolean.class);
                for (String command : toUnregister) {
                    method.invoke(null, command, true);
                }
            }
            catch (Exception _) {
                // ignore stacktrace
            }
        });

        listenersRegistrar.listeners.clear();
        commandsRegistrar.commands.clear();
    }

    @Override
    public File getFolder() {
        assert key != null;
        return new File(corePlugin.getDataFolder(), key.asString().replace(':', '_'));
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

    public void reload(File jar, boolean autoEnable, Set<ModuleDescriptorImpl> skipReloadIfInjected) {
        if(enabled) disable();

        logger.info("Loading module from " + jar.getName());

        ExtendedMeta meta;
        Key newKey;
        try {
            meta = ExtendedMeta.readMetaFromJar(jar);
            newKey = meta.key();
        }
        catch (Throwable ex) {
            throw new RuntimeException("Error while parsing jar meta ", ex);
        }

        boolean first = key == null;

        final Set<ModuleDescriptorImpl> injected = new HashSet<>();
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

        injected.removeAll(skipReloadIfInjected);

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

        moduleInstance = classLoader.create();
        listenersRegistrar = new ListenersRegistrarImpl();
        commandsRegistrar = new CommandsRegistrarImpl();
        compatibilityLayer = new CompatibilityLayerImpl();

        loaded = true;

        // FUCK COMMAND API - POOREST SHIT IN THE WORLD
        if(injected.isEmpty()) {
            if(autoEnable) enable();
            return;
        }

        for (ModuleDescriptorImpl descriptor : injected) {
            descriptor.reload(descriptor.getJar(), false, injected);
        }

        enable(false, null);

        for (ModuleDescriptorImpl descriptor : injected) {
            descriptor.enable(true, key);
        }

        moduleRepository.callEnableHooks(key);
        assert lastEnableContext != null;
        lastEnableContext.enableHooksFuture().complete(null);
    }

    public @Nullable File getJar() {
        return currentJar;
    }

    @Override
    public Key key() {
        assert key != null;
        return key;
    }

}
