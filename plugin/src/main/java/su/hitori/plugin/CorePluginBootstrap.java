package su.hitori.plugin;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import org.bukkit.plugin.java.JavaPlugin;
import su.hitori.api.logging.LoggerFactory;
import su.hitori.api.logging.LoggerFactoryHolder;
import su.hitori.plugin.logging.LoggerFactoryImpl;
import su.hitori.plugin.module.ModuleRepositoryImpl;

@SuppressWarnings({"UnstableApiUsage", "unused"})
public final class CorePluginBootstrap implements PluginBootstrap {

    private final LoggerFactory loggerFactory;
    private final ModuleRepositoryImpl moduleRepository;

    public CorePluginBootstrap() {
        this.loggerFactory = new LoggerFactoryImpl();
        LoggerFactoryHolder.set(loggerFactory);

        this.moduleRepository = new ModuleRepositoryImpl();
    }

    @Override
    public void bootstrap(BootstrapContext context) {
        moduleRepository.bootstrap(context.getDataDirectory().toFile(), context);
    }

    @Override
    public JavaPlugin createPlugin(PluginProviderContext context) {
        return new CorePlugin(loggerFactory, moduleRepository);
    }

}
