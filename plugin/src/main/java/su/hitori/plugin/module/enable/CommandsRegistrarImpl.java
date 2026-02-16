package su.hitori.plugin.module.enable;

import dev.jorel.commandapi.CommandAPICommand;
import su.hitori.api.module.enable.CommandsRegistrar;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class CommandsRegistrarImpl implements CommandsRegistrar {

    public final Set<CommandAPICommand> commands = new HashSet<>();
    public boolean frozen = false;

    @Override
    public CommandsRegistrar register(Collection<CommandAPICommand> commands) {
        if(frozen) return this;
        this.commands.addAll(commands);
        return this;
    }

    @Override
    public CommandsRegistrar register(CommandAPICommand... commands) {
        if(frozen) return this;
        Collections.addAll(this.commands, commands);
        return this;
    }

}
