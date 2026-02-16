package su.hitori.api.module.enable;

import dev.jorel.commandapi.CommandAPICommand;

import java.util.Collection;

/**
 * Module specific commands registrar used in enable phase of module
 */
public interface CommandsRegistrar {

    /**
     * Registers from this module a collection of commands
     * @param commands collection containing commands to register
     * @return that CommandsRegistrar
     */
    CommandsRegistrar register(Collection<CommandAPICommand> commands);

    /**
     * Register from this module an array of commands
     * @param commands array containing commands to register
     * @return that CommandsRegistrar
     */
    CommandsRegistrar register(CommandAPICommand... commands);

}
