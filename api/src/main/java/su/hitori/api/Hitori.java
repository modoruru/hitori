package su.hitori.api;

import org.bukkit.plugin.Plugin;
import su.hitori.api.command.CommandsModificationInfo;
import su.hitori.api.command.CommandsRegistryModifier;
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

    HitoriRegistryAccess registryAccess();

    /**
     * Internal hitori tool for registering and unregistering commands.
     * <p>
     * DISCLAIMER:<br>
     * For registering module commands use {@link su.hitori.api.module.enable.EnableContext#commands()} and {@link su.hitori.api.module.enable.CommandsRegistrar#register}
     * <p>
     * Every time reload happens, {@link CommandsRegistryModifier} re-applies all modifications done using {@link CommandsRegistryModifier#applyModificationsInBatch}.
     * They can be undone using {@link CommandsRegistryModifier#undoBatch(CommandsModificationInfo)}
     */
     CommandsRegistryModifier commandRegistryModifier();

}
