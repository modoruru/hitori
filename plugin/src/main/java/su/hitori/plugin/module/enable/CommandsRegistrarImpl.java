package su.hitori.plugin.module.enable;

import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.jorel.commandapi.CommandAPICommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.key.Key;
import su.hitori.api.ServerCoreInfo;
import su.hitori.api.module.enable.CommandsRegistrar;

import java.util.*;
import java.util.logging.Logger;

@SuppressWarnings("removal") // "Overrides method that is deprecated and marked for removal" and what, I should remove implementation because of this? screw it
public final class CommandsRegistrarImpl implements CommandsRegistrar {

    private final Key moduleKey;
    private final Logger logger;
    private final ServerCoreInfo serverCoreInfo;

    public final Set<CommandAPICommand> oldCommands = new HashSet<>();
    public final List<LiteralCommandNode<CommandSourceStack>> commands = new ArrayList<>();
    public boolean frozen = false;

    public CommandsRegistrarImpl(Key moduleKey, Logger logger, ServerCoreInfo serverCoreInfo) {
        this.moduleKey = moduleKey;
        this.logger = logger;
        this.serverCoreInfo = serverCoreInfo;
    }

    private void printWarningOrThrowIfFolia() {
        final String separator = "========================================";
        logger.warning(separator);
        logger.warning(String.format("Module %s uses old command registration API.", moduleKey.asString()));
        logger.warning("It would be removed in the future releases and the module will BREAK.");
        logger.warning("Notify authors of this module about this warning.");
        logger.warning(separator);

        if(serverCoreInfo.isFolia()) {
            logger.severe(separator);
            logger.severe("Folia doesn't works with the old command registration API! An exception will be thrown.");
            logger.severe(separator);
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public CommandsRegistrar register(Collection<CommandAPICommand> commands) {
        printWarningOrThrowIfFolia();
        if(frozen) return this;
        this.oldCommands.addAll(commands);
        return this;
    }

    @Override
    public CommandsRegistrar register(CommandAPICommand... commands) {
        printWarningOrThrowIfFolia();
        if(frozen) return this;
        Collections.addAll(this.oldCommands, commands);
        return this;
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
