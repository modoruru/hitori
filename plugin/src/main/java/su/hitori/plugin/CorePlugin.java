package su.hitori.plugin;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import su.hitori.api.Hitori;
import su.hitori.api.HitoriHolder;
import su.hitori.api.HitoriRegistryAccess;
import su.hitori.api.ServerCoreInfo;
import su.hitori.api.configuration.HitoriConfiguration;
import su.hitori.api.logging.LoggerFactory;
import su.hitori.api.module.ModuleRepository;
import su.hitori.api.registry.MappedRegistry;
import su.hitori.api.registry.Registry;
import su.hitori.api.registry.RegistryKey;
import su.hitori.api.util.Messages;
import su.hitori.plugin.container.ContainerListener;
import su.hitori.plugin.module.ModuleRepositoryImpl;
import su.hitori.plugin.util.MessagesImpl;

import java.util.Optional;

public final class CorePlugin extends JavaPlugin implements Hitori, HitoriRegistryAccess {

    private final LoggerFactory loggerFactory;
    private final ModuleRepositoryImpl moduleRepository;
    private final ServerCoreInfo serverCoreInfo;

    private final Registry<HitoriConfiguration<?>> configurationRegistry;

    CorePlugin(LoggerFactory loggerFactory, ModuleRepositoryImpl moduleRepository) {
        this.loggerFactory = loggerFactory;
        this.moduleRepository = moduleRepository;
        this.serverCoreInfo = new ServerCoreInfoImpl();

        this.configurationRegistry = new MappedRegistry<>(HitoriRegistryAccess.CONFIGURATION);

        moduleRepository.corePlugin(this);
    }

    @Override
    public void onEnable() {
        HitoriConfiguration<CoreConfiguration> coreConfiguration = HitoriConfiguration.create(
                Key.key("hitori", "core"),
                new CoreConfiguration()
        );

        ServicesManager servicesManager = Bukkit.getServicesManager();
        servicesManager.register(
                Messages.class,
                new MessagesImpl(coreConfiguration.access()),
                this,
                ServicePriority.Highest
        );

        HitoriHolder.set(this);

        new HitoriCommand(this).register(this);

        loadOtherAPIImplementations();

        moduleRepository.enableAll();
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

    @Override
    public HitoriRegistryAccess registryAccess() {
        return this;
    }

    @Override
    public <E extends Keyed> Optional<Registry<E>> access(RegistryKey<E> key) throws IllegalAccessError {

        if(key.equals(HitoriRegistryAccess.CONFIGURATION)) {

        }

        return Optional.empty();
    }

}
