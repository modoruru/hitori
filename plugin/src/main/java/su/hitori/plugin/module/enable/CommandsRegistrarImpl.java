package su.hitori.plugin.module.enable;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import su.hitori.api.module.enable.CommandsRegistrar;

import java.util.*;

public final class CommandsRegistrarImpl implements CommandsRegistrar {

    public final List<LiteralCommandNode<CommandSourceStack>> commands = new ArrayList<>();
    public boolean frozen = false;

    @SafeVarargs
    @Override
    public final CommandsRegistrar register(LiteralCommandNode<CommandSourceStack>... commands) {
        if(frozen) return this;
        Collections.addAll(this.commands, commands);
        return this;
    }

    @Override
    public CommandsRegistrar registerCollection(Collection<LiteralCommandNode<CommandSourceStack>> commands) {
        if(frozen) return this;
        this.commands.addAll(commands);
        return this;
    }

}
