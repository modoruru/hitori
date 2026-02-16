package su.hitori.plugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import su.hitori.api.Hitori;
import su.hitori.api.ServerCoreInfo;
import su.hitori.api.logging.LoggerFactory;
import su.hitori.api.module.ModuleRepository;
import su.hitori.plugin.container.ContainerListener;
import su.hitori.plugin.logging.LoggerFactoryImpl;
import su.hitori.plugin.module.ModuleRepositoryImpl;

public final class CorePlugin extends JavaPlugin implements Hitori {

    private final ModuleRepositoryImpl moduleRepository = new ModuleRepositoryImpl(this);
    private final LoggerFactory loggerFactory = new LoggerFactoryImpl();
    private final ServerCoreInfo serverCoreInfo = new ServerCoreInfoImpl();

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();

        ServicesManager servicesManager = Bukkit.getServicesManager();
        servicesManager.register(
                Hitori.class,
                this,
                this,
                ServicePriority.Highest
        );
        servicesManager.register(
                LoggerFactory.class,
                loggerFactory,
                this,
                ServicePriority.Highest
        );

        new HitoriCommand(this).register(this);

        loadOtherAPIImplementations();

        moduleRepository.load(getDataFolder());
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

    @Override
    public ModuleRepository moduleRepository() {
        return moduleRepository;
    }

    @Override
    public ServerCoreInfo serverCoreInfo() {
        return serverCoreInfo;
    }

}
