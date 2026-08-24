package su.hitori.plugin.module.enable;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.key.Key;
import su.hitori.api.ServerCoreInfo;
import su.hitori.api.module.enable.CommandsRegistrar;

import java.util.*;
import java.util.logging.Logger;

public final class CommandsRegistrarImpl implements CommandsRegistrar {

    private final Key moduleKey;
    private final Logger logger;
    private final ServerCoreInfo serverCoreInfo;

    public final List<LiteralCommandNode<CommandSourceStack>> commands = new ArrayList<>();
    public boolean frozen = false;

    public CommandsRegistrarImpl(Key moduleKey, Logger logger, ServerCoreInfo serverCoreInfo) {
        this.moduleKey = moduleKey;
        this.logger = logger;
        this.serverCoreInfo = serverCoreInfo;
    }

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
