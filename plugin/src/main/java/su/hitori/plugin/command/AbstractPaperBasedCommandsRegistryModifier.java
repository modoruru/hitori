package su.hitori.plugin.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.minecraft.server.MinecraftServer;
import su.hitori.plugin.CorePlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractPaperBasedCommandsRegistryModifier extends CommandRegistryModifier {

    protected final Set<String> toUnregister;
    protected final CommandDispatcher<CommandSourceStack> dispatcher;

    public AbstractPaperBasedCommandsRegistryModifier(CorePlugin corePlugin) {
        this.toUnregister = new HashSet<>();
        this.dispatcher = new CommandDispatcher<>();

        corePlugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, this::event);
    }

    private void event(ReloadableRegistrarEvent<Commands> event) {
        CommandDispatcher<net.minecraft.commands.CommandSourceStack> vanillaDispatcher = MinecraftServer.getServer().getCommands().getDispatcher();
        for (String commandToUnregister : toUnregister) {
            removeBrigadierCommands(vanillaDispatcher.getRoot(), commandToUnregister, true);
        }

        for (CommandNode<CommandSourceStack> child : dispatcher.getRoot().getChildren()) {
            event.registrar().register((LiteralCommandNode<CommandSourceStack>) child);
        }
    }

    @Override
    public void applyModificationsInBatch(List<LiteralCommandNode<CommandSourceStack>> toRegister, Set<String> toUnregister, boolean performReload) {
        for (LiteralCommandNode<CommandSourceStack> node : toRegister) {
            dispatcher.getRoot().addChild(node);
        }
        for (String commandToUnregister : toUnregister) {
            removeBrigadierCommands(dispatcher.getRoot(), commandToUnregister, true);
        }

        this.toUnregister.addAll(toUnregister);

        reload();
    }

    protected abstract void reload();

}
