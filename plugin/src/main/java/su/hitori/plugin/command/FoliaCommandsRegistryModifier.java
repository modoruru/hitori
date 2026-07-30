package su.hitori.plugin.command;

import io.papermc.paper.command.brigadier.PaperCommands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventRunner;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.help.SimpleHelpMap;
import org.bukkit.plugin.Plugin;
import su.hitori.plugin.CorePlugin;

public final class FoliaCommandsRegistryModifier extends AbstractPaperBasedCommandsRegistryModifier {

    public FoliaCommandsRegistryModifier(CorePlugin corePlugin) {
        super(corePlugin);
    }

    @Override
    public void reload() {
        PaperCommands.INSTANCE.setValid();
        LifecycleEventRunner.INSTANCE.callReloadableRegistrarEvent(
                LifecycleEvents.COMMANDS,
                PaperCommands.INSTANCE,
                Plugin.class,
                ReloadableRegistrarEvent.Cause.RELOAD
        );

        SimpleHelpMap helpMap = (SimpleHelpMap) Bukkit.getServer().getHelpMap();
        helpMap.clear();
        helpMap.initializeGeneralTopics();
        helpMap.initializeCommands();
        ((CraftServer) Bukkit.getServer()).syncCommands();
    }

}
