package su.hitori.api;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import su.hitori.api.module.ModuleRepository;

import java.util.Optional;

/**
 * hitori framework api
 */
public interface Hitori {

    /**
     * Returns an instance of Hitori in JVM.
     * @return hitori instance.
     */
    static Hitori instance() {
        return Optional.ofNullable(Bukkit.getServer().getServicesManager().getRegistration(Hitori.class))
                .map(RegisteredServiceProvider::getProvider)
                .orElse(null);
    }

    /**
     * Returns instance of hitori plugin.
     */
    Plugin plugin();

    /**
     * Instance of ModuleRepository
     */
    ModuleRepository moduleRepository();

    ServerCoreInfo serverCoreInfo();

}
