package su.hitori.plugin;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;
import su.hitori.api.Hitori;
import su.hitori.api.HitoriHolder;
import su.hitori.api.HitoriRegistryAccess;
import su.hitori.api.ServerCoreInfo;
import su.hitori.api.command.CommandsRegistryModifier;
import su.hitori.api.configuration.ConfigurationSource;
import su.hitori.api.configuration.HitoriConfiguration;
import su.hitori.api.configuration.serializer.YAMLSerializer;
import su.hitori.api.logging.LoggerFactory;
import su.hitori.api.module.ModuleRepository;
import su.hitori.api.registry.MappedRegistry;
import su.hitori.api.registry.Registry;
import su.hitori.api.registry.RegistryKey;
import su.hitori.api.util.Messages;
import su.hitori.api.util.UnsafeUtil;
import su.hitori.plugin.command.AbstractPaperBasedCommandsRegistryModifier;
import su.hitori.plugin.command.FoliaCommandsRegistryModifier;
import su.hitori.plugin.command.PaperCommandsRegistryModifier;
import su.hitori.plugin.container.ContainerListener;
import su.hitori.plugin.module.ModuleRepositoryImpl;
import su.hitori.plugin.util.MessagesImpl;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class CorePlugin extends JavaPlugin implements Hitori, HitoriRegistryAccess {

    private final LoggerFactory loggerFactory;
    private final ModuleRepositoryImpl moduleRepository;
    private final ServerCoreInfo serverCoreInfo;

    private final Registry<HitoriConfiguration<?>> configurationRegistry;

    private @Nullable AbstractPaperBasedCommandsRegistryModifier commandRegistryModifier;

    CorePlugin(LoggerFactory loggerFactory, ModuleRepositoryImpl moduleRepository) {
        this.loggerFactory = loggerFactory;
        this.moduleRepository = moduleRepository;
        this.serverCoreInfo = new ServerCoreInfoImpl();

        this.configurationRegistry = new MappedRegistry<>(HitoriRegistryAccess.CONFIGURATION);

        moduleRepository.corePlugin(this);
    }

    @Override
    public void onEnable() {
        if(serverCoreInfo.isFolia()) commandRegistryModifier = new FoliaCommandsRegistryModifier(this);
        else commandRegistryModifier = new PaperCommandsRegistryModifier(this);

        HitoriConfiguration<CoreConfiguration> coreConfiguration = HitoriConfiguration.create(
                Key.key("hitori", "core"),
                new CoreConfiguration(),
                ConfigurationSource.file(YAMLSerializer.INSTANCE, getDataPath().resolve("config/config.yml"))
        );

        configurationRegistry.register(coreConfiguration.key(), coreConfiguration);

        ServicesManager servicesManager = Bukkit.getServicesManager();
        servicesManager.register(
                Messages.class,
                new MessagesImpl(coreConfiguration.access()),
                this,
                ServicePriority.Highest
        );

        HitoriHolder.set(this);

        Runnable reloadCommands = commandRegistryModifier.scheduleReload();
        commandRegistryModifier.applyModificationsInBatch(
                List.of(HitoriCommand.boostrap(this)),
                Set.of()
        );

        loadOtherAPIImplementations();

        moduleRepository.enableAll();
        reloadCommands.run();
    }

    private void loadOtherAPIImplementations() {
        Bukkit.getPluginManager().registerEvents(new ContainerListener(), this);
    }

    @Override
    public void onDisable() {
        moduleRepository.disableAll();
    }

    @Override
    public Plugin plugin() {
        return this;
    }

    public LoggerFactory loggerFactory() {
        return loggerFactory;
    }

    @Override
    public ModuleRepository moduleRepository() {
        return moduleRepository;
    }

    @Override
    public ServerCoreInfo serverCoreInfo() {
        return serverCoreInfo;
    }

    public Registry<HitoriConfiguration<?>> configurationRegistry() {
        return configurationRegistry;
    }

    @Override
    public HitoriRegistryAccess registryAccess() {
        return this;
    }

    public AbstractPaperBasedCommandsRegistryModifier commandRegistryModifier() {
        assert commandRegistryModifier != null;
        return commandRegistryModifier;
    }

    @Override
    public <E extends Keyed> Optional<Registry<E>> access(RegistryKey<E> key) throws IllegalAccessError {
        if(key.equals(HitoriRegistryAccess.CONFIGURATION))
            return Optional.of(configurationRegistry).map(UnsafeUtil::cast);

        return Optional.empty();
    }

}
