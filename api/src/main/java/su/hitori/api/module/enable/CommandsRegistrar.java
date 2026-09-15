package su.hitori.api.module.enable;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;

import java.util.Collection;

/**
 * Module specific commands registrar used in enable phase of module
 */
public interface CommandsRegistrar {

    /**
     * Register from this module an array of commands
     * @param commands array containing commands to register
     * @return this CommandsRegistrar
     */
    CommandsRegistrar register(LiteralCommandNode<CommandSourceStack>... commands);

    /**
     * Registers a collection of commands assigned to this module
     * @param commands collection containing commands to register
     * @return this CommandsRegistrar
     */
    CommandsRegistrar registerCollection(Collection<LiteralCommandNode<CommandSourceStack>> commands);

}
