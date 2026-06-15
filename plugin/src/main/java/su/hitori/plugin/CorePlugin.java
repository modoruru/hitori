package su.hitori.plugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import su.hitori.api.Hitori;
import su.hitori.api.HitoriHolder;
import su.hitori.api.ServerCoreInfo;
import su.hitori.api.logging.LoggerFactory;
import su.hitori.api.module.ModuleRepository;
import su.hitori.api.util.Messages;
import su.hitori.plugin.container.ContainerListener;
import su.hitori.plugin.logging.LoggerFactoryImpl;
import su.hitori.plugin.module.ModuleRepositoryImpl;
import su.hitori.plugin.util.MessagesImpl;

import java.nio.file.Path;

public final class CorePlugin extends JavaPlugin implements Hitori {

    private final LoggerFactory loggerFactory;
    private final ModuleRepositoryImpl moduleRepository;
    private final ServerCoreInfo serverCoreInfo = new ServerCoreInfoImpl();

    CorePlugin(LoggerFactory loggerFactory, ModuleRepositoryImpl moduleRepository) {
        this.loggerFactory = loggerFactory;
        this.moduleRepository = moduleRepository;
        moduleRepository.corePlugin(this);
    }

    @Override
    public void onEnable() {
        Path configPath = getDataPath().resolve("config/").resolve("config.yml");
        configPath.toFile().getParentFile().mkdirs();
        HitoriConfiguration configuration = new HitoriConfiguration(configPath);
        configuration.reload();

        ServicesManager servicesManager = Bukkit.getServicesManager();
        servicesManager.register(
                Messages.class,
                new MessagesImpl(configuration),
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

}
