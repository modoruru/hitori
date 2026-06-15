package su.hitori.api;

import org.bukkit.plugin.Plugin;
import su.hitori.api.module.ModuleRepository;

/**
 * hitori framework api
 */
public interface Hitori {

    /**
     * Returns an instance of Hitori in JVM.
     * @return hitori instance.
     */
    @SuppressWarnings("DataFlowIssue")
    static Hitori instance() {
        return HitoriHolder.instance;
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
